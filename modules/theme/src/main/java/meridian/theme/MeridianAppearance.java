package meridian.theme;

import atlantafx.base.theme.Theme;
import atlantafx.base.theme.ThemeManager;
import atlantafx.base.theme.ThemeOption;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.scene.Parent;
import javafx.scene.Scene;
import meridian.theme.metrics.MeridianMetrics;

/**
 * One application-scoped preference binding using AtlantaFX's ThemeManager.
 * All operations are on the FX thread. Close at application shutdown; register hidden scenes explicitly.
 */
public final class MeridianAppearance implements AutoCloseable {
    private static final ThemeOption.Key<MeridianPreferences> KEY =
            new ThemeOption.Key<>("meridian.preferences", MeridianPreferences.class);
    private static final String MARKER = "/*option:meridian.preferences*/";
    private static MeridianAppearance active;

    private final ThemeManager manager;
    private final Theme previousTheme;
    private final Set<String> previousModules;
    private final ReadOnlyObjectWrapper<MeridianPreferences> preferences;
    private final Map<Scene, SceneState> decorated = new WeakHashMap<>();
    private final Map<Scene, Boolean> excluded = new WeakHashMap<>();
    private final Map<Scene, Registration> registered = new IdentityHashMap<>();
    private final InvalidationListener platformListener = ignored -> apply();
    private final ThemeOption<MeridianPreferences> option;
    private boolean closed;

    private MeridianAppearance(MeridianPreferences initial) {
        manager = ThemeManager.instance();
        if (manager.supports(KEY)) throw new IllegalStateException("Meridian preference option already registered");
        previousTheme = manager.getTheme();
        previousModules = manager.getModules();
        preferences = new ReadOnlyObjectWrapper<>(this, "preferences", initial);
        option = ThemeOption.of(KEY, initial, change -> {
            if (!closed && !excluded.containsKey(change.scene())) applyScene(change.scene());
        });
        manager.register(option);
        Platform.getPreferences().colorSchemeProperty().addListener(platformListener);
        Platform.getPreferences().reducedMotionProperty().addListener(platformListener);
        manager.setModules(Set.of());
        apply();
    }

    public static MeridianAppearance install() { return install(MeridianPreferences.defaults()); }

    public static MeridianAppearance install(MeridianPreferences preferences) {
        requireFx();
        Objects.requireNonNull(preferences, "preferences");
        if (active != null) throw new IllegalStateException("Only one application appearance binding may be installed");
        active = new MeridianAppearance(preferences);
        return active;
    }

    public ReadOnlyObjectProperty<MeridianPreferences> preferencesProperty() { return preferences.getReadOnlyProperty(); }
    public MeridianPreferences preferences() { return preferences.get(); }
    public void setPreferences(MeridianPreferences value) {
        requireOpen();
        Objects.requireNonNull(value, "preferences");
        if (value.equals(preferences.get())) return;
        preferences.set(value);
        apply();
    }

    public MeridianTheme theme() {
        requireOpen();
        var p = preferences();
        boolean dark = switch (p.appearance()) {
            case DARK -> true;
            case LIGHT -> false;
            case SYSTEM -> Platform.getPreferences().getColorScheme() == ColorScheme.DARK;
        };
        return MeridianTheme.of(dark, p.density());
    }

    public boolean reducedMotion() {
        requireOpen();
        return switch (preferences().motion()) {
            case FULL -> false;
            case REDUCED -> true;
            case SYSTEM -> Platform.getPreferences().isReducedMotion();
        };
    }

    public MeridianMetrics.DensityMetrics metrics() {
        requireOpen();
        return MeridianMetrics.density(preferences().density()).scaledForText(preferences().textScale());
    }

    /**
     * Registers a scene that previously inherited platform scheme/motion.
     * Use the overload with explicit restoration values for a host with local overrides.
     */
    public Registration register(Scene scene) { return register(scene, SceneRestore.platform()); }

    /** Restores explicit values or platform inheritance (null); JavaFX getters cannot distinguish those. */
    public record SceneRestore(ColorScheme colorScheme, Boolean reducedMotion) {
        public static SceneRestore platform() { return new SceneRestore(null, null); }
    }

    /** Idempotent for the same scene/restoration policy; attaches one root listener. */
    public Registration register(Scene scene, SceneRestore restore) {
        requireOpen();
        Objects.requireNonNull(scene, "scene");
        Objects.requireNonNull(restore, "restore");
        if (scene.getPreferences().colorSchemeProperty().isBound() || scene.getPreferences().reducedMotionProperty().isBound()) {
            throw new IllegalArgumentException("Scene scheme/motion already belongs to another binding");
        }
        var existing = registered.get(scene);
        if (existing != null) {
            if (!existing.baseline.equals(restore)) throw new IllegalArgumentException("Scene already has a different restoration policy");
            return existing;
        }
        excluded.remove(scene);
        var state = decorated.computeIfAbsent(scene, ignored -> new SceneState(scene));
        state.scheme = restore.colorScheme();
        state.motion = restore.reducedMotion();
        var registration = new Registration(scene, restore);
        registered.put(scene, registration);
        applyScene(scene);
        return registration;
    }

    public int registeredSceneCount() { requireOpen(); return registered.size(); }

    private void apply() {
        if (closed) return;
        requireFx();
        manager.setTheme(theme());
        manager.setOption(KEY, preferences());
        for (Scene scene : List.copyOf(registered.keySet())) applyScene(scene);
    }

    private void applyScene(Scene scene) {
        if (scene.getPreferences().colorSchemeProperty().isBound() || scene.getPreferences().reducedMotionProperty().isBound()) return;
        var state = decorated.computeIfAbsent(scene, ignored -> new SceneState(scene));
        scene.setUserAgentStylesheet(theme().getUserAgentStylesheet());
        scene.getPreferences().setColorScheme(theme().isDarkMode() ? ColorScheme.DARK : ColorScheme.LIGHT);
        scene.getPreferences().setReducedMotion(reducedMotion());
        Parent root = scene.getRoot();
        state.remember(root);
        new ThemeManager.Change<>(theme(), preferences(), scene)
                .applyStylesheet("meridian.preferences", scene, ignored -> dynamicCss());
    }

    private String dynamicCss() {
        var text = MeridianMetrics.TYPOGRAPHY.scaledForText(preferences().textScale());
        var density = metrics();
        int hover = reducedMotion() ? 0 : MeridianMetrics.MOTION.hoverMs();
        double small = density.controlMinHeight() - 4 * preferences().textScale();
        // Literal metrics avoid stale em conversions when an existing hidden scene changes text scale.
        return ".root { -fx-font-size: " + text.general() + "px; }\n"
                + ".meridian-title { -fx-font-size: " + text.title() + "px; }\n"
                + ".meridian-section { -fx-font-size: " + text.section() + "px; }\n"
                + ".meridian-metadata { -fx-font-size: " + text.metadata() + "px; }\n"
                + ".button, .toggle-button, .text-field, .password-field, .menu-button, .combo-box-base"
                + " { -fx-min-height: " + density.controlMinHeight() + "px; }\n"
                // .small is one 4px step below the density floor (28 comfortable / 24 compact), still >= WCAG 2.5.8's 24px.
                + ".button.small, .toggle-button.small, .menu-button.small, .split-menu-button.small { -fx-min-height: "
                + small + "px; -fx-pref-height: " + small + "px; -fx-max-height: " + small + "px; }\n"
                + ".table-view { -fx-font-size: " + text.table() + "px; }\n"
                + ".table-view > .column-header-background .column-header { -fx-size: " + density.headerMinHeight()
                + "px; -fx-min-height: " + density.headerMinHeight() + "px; -fx-pref-height: " + density.headerMinHeight() + "px; }\n"
                + ".table-view .table-row-cell, .table-view .table-row-cell > .table-cell"
                + " { -fx-min-height: " + density.tableRowMinHeight() + "px; }\n"
                + ".button, .text-input { transition-duration: " + hover + "ms; }\n";
    }

    private static void cleanRoot(Parent root) {
        root.getStylesheets().removeIf(url -> url.endsWith(MARKER));
    }

    private void restore(Scene scene, SceneState state) {
        for (var ref : state.roots) {
            var root = ref.get();
            if (root != null) cleanRoot(root);
        }
        scene.setUserAgentStylesheet(state.stylesheet);
        scene.getPreferences().setColorScheme(state.scheme);
        scene.getPreferences().setReducedMotion(state.motion);
    }

    /** Removes only Meridian-owned listeners/options and restores prior host settings. */
    @Override public void close() {
        requireFx();
        if (closed) return;
        closed = true;
        Platform.getPreferences().colorSchemeProperty().removeListener(platformListener);
        Platform.getPreferences().reducedMotionProperty().removeListener(platformListener);
        manager.unregister(option);
        for (var registration : registered.values()) registration.detach();
        registered.clear();
        for (var entry : decorated.entrySet()) restore(entry.getKey(), entry.getValue());
        decorated.clear();
        excluded.clear();
        manager.setModules(previousModules);
        manager.setTheme(previousTheme);
        active = null;
        // ThemeManager is a shared singleton; disposing it would disable later installations.
    }

    private void requireOpen() {
        requireFx();
        if (closed) throw new IllegalStateException("Appearance binding is closed");
    }

    private static void requireFx() {
        if (!Platform.isFxApplicationThread()) throw new IllegalStateException("Call from the JavaFX Application Thread");
    }

    private static final class SceneState {
        private final String stylesheet;
        private ColorScheme scheme;
        private Boolean motion;
        private final List<WeakReference<Parent>> roots = new ArrayList<>();
        SceneState(Scene scene) {
            stylesheet = scene.getUserAgentStylesheet();
            scheme = scene.getPreferences().colorSchemeProperty().get();
            motion = scene.getPreferences().reducedMotionProperty().get();
        }
        void remember(Parent root) {
            roots.removeIf(ref -> ref.get() == null);
            if (roots.stream().noneMatch(ref -> ref.get() == root)) roots.add(new WeakReference<>(root));
        }
    }

    public final class Registration implements AutoCloseable {
        private final Scene scene;
        private final SceneRestore baseline;
        private final ChangeListener<Parent> rootListener;
        private boolean detached;
        private Registration(Scene scene, SceneRestore baseline) {
            this.scene = scene;
            this.baseline = baseline;
            rootListener = (ignored, oldRoot, newRoot) -> {
                cleanRoot(oldRoot);
                applyScene(scene);
            };
            scene.rootProperty().addListener(rootListener);
        }
        private void detach() {
            if (detached) return;
            scene.rootProperty().removeListener(rootListener);
            detached = true;
        }
        @Override public void close() {
            requireFx();
            if (detached) return;
            detach();
            registered.remove(scene);
            var state = decorated.remove(scene);
            if (state != null) restore(scene, state);
            excluded.put(scene, true);
        }
    }
}
