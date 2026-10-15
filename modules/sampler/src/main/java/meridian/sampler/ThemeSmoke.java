package meridian.sampler;

import atlantafx.base.theme.ThemeManager;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.imageio.ImageIO;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import meridian.theme.MeridianAppearance;
import meridian.theme.MeridianPreferences;
import meridian.theme.MeridianPreferences.Appearance;
import meridian.theme.MeridianPreferences.Motion;
import meridian.theme.MeridianTheme;
import meridian.theme.metrics.MeridianMetrics.DensityMode;

/** Bounded native four-variant render and appearance lifecycle checks; synthetic fixtures only. */
public final class ThemeSmoke {
    private static Throwable failure;
    private ThemeSmoke() {}
    public static void main(String[] args) {
        Application.launch(Window.class, args);
        if (failure != null) throw new IllegalStateException("Meridian theme acceptance failed", failure);
    }

    public static final class Window extends Application {
        private MeridianAppearance binding;
        private Stage stage;
        private Path output;
        private Fixture fixture;
        private int variant;
        private atlantafx.base.theme.Theme hostTheme;
        private java.util.Set<String> hostModules;
        private final List<String> checks = new ArrayList<>();
        private final Map<String, Color> palette = new HashMap<>();
        private Dialog<Void> dialog;
        private ContextMenu menu;
        private final List<Stage> extraStages = new ArrayList<>();

        @Override public void start(Stage primary) {
            stage = primary;
            try {
                if (getParameters().getRaw().size() != 1) throw new IllegalArgumentException("Pass a new output directory");
                output = Path.of(getParameters().getRaw().getFirst()).toAbsolutePath();
                if (Files.exists(output)) throw new IllegalArgumentException("Output already exists: " + output);
                Files.createDirectories(output);
                for (String line : Files.readAllLines(Path.of("tokens/generated/native-expected.tsv"))) {
                    String[] parts = line.split("\t");
                    if (parts[0].equals("theme") && parts[2].startsWith("-meridian-")) {
                        palette.put(parts[1] + "." + parts[2].substring(10), Color.web(parts[3]));
                    }
                }
                hostTheme = ThemeManager.instance().getTheme();
                hostModules = ThemeManager.instance().getModules();
                binding = MeridianAppearance.install(new MeridianPreferences(Appearance.LIGHT, DensityMode.COMFORTABLE, 1, Motion.REDUCED));
                fixture = new Fixture();
                fixture.root.setMouseTransparent(true);
                Scene scene = new Scene(fixture.root, 1060, 710);
                binding.register(scene);
                stage.setScene(scene);
                stage.setTitle("MeridianFX — native control specimens");
                stage.show();
                nextVariant();
            } catch (Throwable error) { fail(error); }
        }

        private void nextVariant() {
            if (variant == 4) {
                later(() -> { lifecycle(); finish(); });
                return;
            }
            boolean dark = variant >= 2;
            DensityMode density = variant % 2 == 0 ? DensityMode.COMFORTABLE : DensityMode.COMPACT;
            binding.setPreferences(new MeridianPreferences(dark ? Appearance.DARK : Appearance.LIGHT, density, 1, Motion.REDUCED));
            fixture.mode.setText((dark ? "Dark" : "Light") + " / " + density.name().toLowerCase(java.util.Locale.ROOT));
            fixture.table.requestFocus();
            fixture.hover.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
            fixture.pressed.arm();
            fixture.focus.pseudoClassStateChanged(PseudoClass.getPseudoClass("focus-visible"), true);
            fixture.focus.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);
            later(() -> {
                styles(fixture.root);
                verifyFixture(dark, density);
                writePng(stage.getScene().snapshot(null), output.resolve(name() + ".png"));
                menu = new ContextMenu(new MenuItem("Inspect dataset"), new MenuItem("Export summary"), new MenuItem("Unavailable"));
                menu.getItems().getLast().setDisable(true);
                menu.setAutoHide(false);
                menu.show(fixture.secondary, javafx.geometry.Side.BOTTOM, 0, 6);
                later(() -> captureMenu(dark));
            });
        }

        private void captureMenu(boolean dark) throws IOException {
            styles(menu.getScene().getRoot());
            require(menu.isShowing(), "context menu actually shown");
            Region menuSurface = menu.getScene().getRoot().lookupAll(".context-menu").stream()
                    .filter(n -> n instanceof Region r && r.getBackground() != null && !r.getBackground().getFills().isEmpty())
                    .map(n -> (Region)n).findFirst().orElseThrow();
            color(lastFill(menuSurface), expected(dark, "surface-raised"), "menu.surface");
            Node first = menu.getItems().getFirst().getStyleableNode();
            first.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"),true);
            styles(menu.getScene().getRoot());
            color(lastFill((Region)first),expected(dark,"selection-inactive"),"menu.focused");
            color(((Label)((Parent)first).lookup(".label")).getTextFill(),expected(dark,"text-primary"),"menu.focused.text");
            Node unavailable = menu.getItems().getLast().getStyleableNode();
            color(((Label)((Parent)unavailable).lookup(".label")).getTextFill(),expected(dark,"disabled-foreground"),"menu.disabled.text");
            require(menu.getScene().getPreferences().isReducedMotion(), "menu inherits reduced motion");
            writePng(menu.getScene().getRoot().snapshot(null, null), output.resolve(name() + "-menu.png"));
            menu.hide();
            dialog = new Dialog<>();
            dialog.initOwner(stage);
            dialog.setTitle("Analysis configuration");
            dialog.setHeaderText("Review the configuration");
            dialog.setContentText("Synthetic specimen. Enter a positive window size before running.");
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
            dialog.show();
            binding.register(dialog.getDialogPane().getScene());
            later(() -> {
                styles(dialog.getDialogPane());
                require(dialog.isShowing(), "dialog actually shown");
                color(lastFill(dialog.getDialogPane()), expected(dark, "surface-canvas"), "dialog.surface");
                color(((Label) dialog.getDialogPane().lookup(".header-panel .label")).getTextFill(),
                        expected(dark, "text-primary"), "dialog.header.text");
                Label body = (Label) dialog.getDialogPane().lookup(".content.label");
                var shown = (javafx.scene.text.Text) body.lookup(".text");
                require(shown != null && shown.getText().equals(body.getText()), "dialog.body.not-truncated shown=" + (shown == null ? null : shown.getText()));
                writePng(dialog.getDialogPane().snapshot(null, null), output.resolve(name() + "-dialog.png"));
                var registration = binding.register(dialog.getDialogPane().getScene());
                dialog.close();
                registration.close();
                dialog = null;
                variant++;
                nextVariant();
            });
        }

        private void verifyFixture(boolean dark, DensityMode density) {
            double min = density == DensityMode.COMPACT ? 28 : 32;
            color(lastFill(fixture.root), expected(dark, "surface-canvas"), "root.surface");
            color(fixture.title.getTextFill(), expected(dark, "text-primary"), "label.default");
            near(fixture.title.getFont().getSize(), 25, "title.font");
            color(fixture.secondary.getTextFill(), expected(dark, "text-primary"), "button.default.text");
            color(lastFill(fixture.secondary), expected(dark, "surface-raised"), "button.default.background");
            near(fixture.secondary.getHeight(), min, "button.height");
            near(fixture.secondary.getFont().getSize(), 14, "button.font");
            color(lastFill(fixture.primary), expected(dark, "action-default"), "button.primary");
            color(fixture.primary.getTextFill(), expected(dark, "text-on-action"), "button.primary.text");
            color(lastFill(fixture.hover), expected(dark, "selection-inactive"), "button.hover");
            near(fixture.hover.getOpacity(), 1, "button.hover.opacity");
            color(lastFill(fixture.pressed), expected(dark, "state-pressed"), "button.pressed");
            color(fixture.focus.getBorder().getStrokes().getFirst().getTopStroke(), expected(dark, "focus-ring"), "button.focus-visible");
            color(fixture.disabled.getTextFill(), expected(dark, "disabled-foreground"), "button.disabled.text");
            near(fixture.disabled.getOpacity(), 1, "button.disabled.opacity");
            color(css(fixture.field, "-fx-text-fill"), expected(dark, "text-primary"), "field.default.text");
            color(css(fixture.prompt, "-fx-prompt-text-fill"), expected(dark, "text-placeholder"), "field.placeholder");
            color(fixture.invalid.getBackground().getFills().getFirst().getFill(), expected(dark, "border-invalid"), "field.invalid.boundary");
            color(css(fixture.invalid, "-fx-text-fill"), expected(dark, "text-primary"), "field.invalid.text");
            near(fixture.field.getHeight(), min, "field.height");
            fixture.invalid.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"),true);
            styles(fixture.root);
            color(fixture.invalid.getBackground().getFills().getFirst().getFill(),expected(dark,"border-invalid"),"field.invalid-focused.boundary");
            color(fixture.invalid.getBorder().getStrokes().getFirst().getTopStroke(),expected(dark,"focus-ring"),"field.invalid-focused.focus");
            fixture.invalid.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"),false);
            fixture.prompt.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"),true);
            styles(fixture.root);
            color(css(fixture.prompt,"-fx-prompt-text-fill"),expected(dark,"text-placeholder"),"field.focused.placeholder");
            fixture.prompt.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"),false);
            styles(fixture.root);
            color(lastFill(fixture.readonly), expected(dark, "surface-shell"), "field.readonly");
            color(css(fixture.disabledField, "-fx-text-fill"), expected(dark, "disabled-foreground"), "field.disabled");
            near(((javafx.scene.control.TableCell<?,?>)fixture.table.lookup(".table-cell")).getFont().getSize(), 13, "table.font");
            TableRow<?> row = selectedRow(fixture.table);
            color(lastFill(row), expected(dark, fixture.table.isFocused() ? "selection-active" : "selection-inactive"), "table.selected");
            near(row.getHeight(), density == DensityMode.COMPACT ? 32 : 40, "table.row.height");
            var headers = fixture.table.lookupAll(".column-header").stream()
                    .filter(n -> n instanceof Region r && r.isVisible() && r.getWidth() > 0 && r.getHeight() > 0)
                    .map(n -> (Region)n).toList();
            require(headers.size() >= 2,"visible table column headers present");
            for (Region header : headers) {
                near(((Number)css(header,"-fx-size")).doubleValue(),density == DensityMode.COMPACT ? 28 : 32,"table.header.computed-size");
                near(header.getHeight(),density == DensityMode.COMPACT ? 28 : 32,"table.header.height pref="+header.prefHeight(-1));
            }
            color(row.getBackground().getFills().get(1).getFill(),
                    expected(dark, fixture.table.isFocused() ? "selection-marker" : "selection-inactive-marker"), "table.selection.marker");
            fixture.secondary.requestFocus();
            styles(fixture.root);
            require(!fixture.table.isFocused(),"table actually loses focus");
            color(lastFill(row),expected(dark,"selection-inactive"),"table.inactive-selection");
            color(row.getBackground().getFills().get(1).getFill(),expected(dark,"selection-inactive-marker"),"table.inactive-marker");
            fixture.table.requestFocus();
            styles(fixture.root);
            require(fixture.root.getStylesheets().size() == 1, "one dynamic stylesheet on fixture");
            require(stage.getScene().getStylesheets().isEmpty(), "no production overlay stylesheet");
            require(binding.theme() == MeridianTheme.of(dark,density), "correct Theme instance");
            require(binding.theme().getManifest().getModules().containsKey("meridian"), "AtlantaFX manifest parses");
            require(stage.getScene().getPreferences().getColorScheme() == (dark ? javafx.application.ColorScheme.DARK : javafx.application.ColorScheme.LIGHT),
                    "scene color scheme matches explicit appearance");
        }

        private void lifecycle() throws Exception {
            var originalTheme = ThemeManager.instance().getTheme();
            var hiddenRoot = new VBox(new Button("Hidden scene"));
            var hidden = new Scene(hiddenRoot, 400, 200);
            String authored = "data:text/css,.label%20%7B%20-fx-text-fill:%20red;%20%7D";
            hiddenRoot.getStylesheets().add(authored);
            var one = binding.register(hidden);
            require(one == binding.register(hidden), "duplicate registration returns same handle");
            int count = binding.registeredSceneCount();
            for (int i = 0; i < 16; i++) {
                binding.setPreferences(new MeridianPreferences(i % 2 == 0 ? Appearance.LIGHT : Appearance.DARK,
                        i % 3 == 0 ? DensityMode.COMPACT : DensityMode.COMFORTABLE, 1, Motion.REDUCED));
            }
            require(hiddenRoot.getStylesheets().size() == 2, "repeated switching does not duplicate sheets");
            require(binding.registeredSceneCount() == count, "repeated switching does not duplicate registrations");
            require(hidden.getUserAgentStylesheet().equals(binding.theme().getUserAgentStylesheet()), "hidden scene updated");
            Parent replacement = new VBox(new TextField("Replacement root"));
            hidden.setRoot(replacement);
            require(hiddenRoot.getStylesheets().equals(List.of(authored)), "old root treatment removed and host CSS retained");
            require(replacement.getStylesheets().size() == 1, "replacement root treated once");
            one.close(); one.close();
            require(replacement.getStylesheets().isEmpty() && hidden.getUserAgentStylesheet() == null, "unregister restores scene");
            hidden.setRoot(new VBox());
            binding.setPreferences(new MeridianPreferences(Appearance.LIGHT, DensityMode.COMPACT, 2, Motion.REDUCED));
            require(hidden.getRoot().getStylesheets().isEmpty(), "unregistered root listener detached");
            styles(fixture.root);
            near(fixture.secondary.getFont().getSize(),28,"existing-scene.enlarged.font");
            require(fixture.secondary.getHeight() >= 55.95,"existing-scene.enlarged.target");
            near(fixture.secondary.minHeight(-1),56,"existing-scene.enlarged.minimum");
            near(fixture.title.getFont().getSize(),50,"existing-scene.enlarged.title");
            near(((javafx.scene.control.TableCell<?,?>)fixture.table.lookup(".table-cell")).getFont().getSize(),26,"existing-scene.enlarged.table-font");
            int[] transitions = {0};
            javafx.event.EventHandler<javafx.css.TransitionEvent> transitionListener = event -> {
                if (event.getPropertyName().equals("-fx-background-color")) transitions[0]++;
            };
            fixture.secondary.addEventHandler(javafx.css.TransitionEvent.RUN,transitionListener);
            binding.setPreferences(new MeridianPreferences(Appearance.LIGHT,DensityMode.COMPACT,2,Motion.FULL));
            styles(fixture.root);
            fixture.secondary.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"),true);
            styles(fixture.root);
            require(transitions[0] > 0,"full-motion native CSS transition created");
            binding.setPreferences(new MeridianPreferences(Appearance.LIGHT,DensityMode.COMPACT,2,Motion.REDUCED));
            styles(fixture.root);
            int reducedCount=transitions[0];
            fixture.secondary.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"),false);
            styles(fixture.root);
            require(transitions[0]==reducedCount,"reduced-motion hover creates no CSS transition");
            fixture.secondary.removeEventHandler(javafx.css.TransitionEvent.RUN,transitionListener);
            var enlargedRoot = new VBox(new Button("Enlarged text"));
            var enlargedScene = new Scene(enlargedRoot, 500, 200);
            var reg = binding.register(enlargedScene);
            Stage extra = new Stage(); extraStages.add(extra);
            extra.setScene(enlargedScene); extra.show();
            styles(enlargedRoot);
            Button enlarged = (Button) enlargedRoot.getChildren().getFirst();
            near(enlarged.getFont().getSize(), 28, "enlarged.font");
            near(enlarged.minHeight(-1),56,"enlarged.control.minimum");
            require(enlarged.getHeight() >= 55.95,"enlarged effective control target grows");
            require(enlargedScene.getPreferences().isReducedMotion(), "new scene inherits current motion");
            require(enlargedScene.getUserAgentStylesheet().equals(binding.theme().getUserAgentStylesheet()), "new scene inherits current theme");
            reg.close(); extra.close();
            Scene hostScene = new Scene(new VBox());
            hostScene.getPreferences().setColorScheme(javafx.application.ColorScheme.DARK);
            hostScene.getPreferences().setReducedMotion(true);
            var hostRegistration = binding.register(hostScene,new MeridianAppearance.SceneRestore(javafx.application.ColorScheme.DARK,true));
            require(hostScene.getPreferences().getColorScheme()==javafx.application.ColorScheme.LIGHT,"local host override replaced while bound");
            hostRegistration.close();
            require(hostScene.getPreferences().getColorScheme()==javafx.application.ColorScheme.DARK && hostScene.getPreferences().isReducedMotion(),
                    "explicit host overrides restored");
            Scene boundHost = new Scene(new VBox());
            var hostProperty = new javafx.beans.property.SimpleObjectProperty<>(javafx.application.ColorScheme.DARK);
            boundHost.getPreferences().colorSchemeProperty().bind(hostProperty);
            expectFailure(() -> binding.register(boundHost),IllegalArgumentException.class,"foreign scene preference binding rejected");
            require(boundHost.getPreferences().colorSchemeProperty().isBound(),"foreign binding preserved");
            boundHost.getPreferences().colorSchemeProperty().unbind();
            expectFailure(() -> binding.setPreferences(null), NullPointerException.class, "null preference rejected");
            expectFailure(() -> new MeridianPreferences(Appearance.LIGHT,DensityMode.COMPACT,Double.NaN,Motion.FULL),
                    IllegalArgumentException.class,"invalid scale rejected");
            expectFailure(() -> new MeridianPreferences(Appearance.LIGHT,DensityMode.COMPACT,Double.MAX_VALUE,Motion.FULL),
                    IllegalArgumentException.class,"overflow scale rejected");
            expectFailure(MeridianAppearance::install,IllegalStateException.class,"second binding rejected");
            Throwable[] offThread = new Throwable[1];
            Thread worker = new Thread(() -> {
                try { binding.setPreferences(MeridianPreferences.defaults()); } catch (Throwable error) { offThread[0] = error; }
            });
            worker.start(); worker.join();
            require(offThread[0] instanceof IllegalStateException, "off-thread mutation rejected");
            binding.close(); binding.close();
            require(ThemeManager.instance().getTheme()==hostTheme && ThemeManager.instance().getModules().equals(hostModules),
                    "disposal restores prior host theme and module selection");
            require(fixture.root.getStylesheets().isEmpty(), "disposal removes dynamic fixture treatment");
            require(stage.getScene().getUserAgentStylesheet() == null, "disposal restores previous scene UA");
            require(stage.getScene().getPreferences().getColorScheme()==Platform.getPreferences().getColorScheme(),
                    "disposal restores platform-coalesced scheme");
            require(stage.getScene().getPreferences().isReducedMotion()==Platform.getPreferences().isReducedMotion(),
                    "disposal restores platform-coalesced motion");
            expectFailure(() -> binding.setPreferences(MeridianPreferences.defaults()),IllegalStateException.class,"disposed mutation rejected");
            // Reinstalling proves the shared manager was not disposed and the option was unregistered.
            binding = MeridianAppearance.install(MeridianPreferences.optional(null,null,null,null));
            require(binding.preferences().equals(MeridianPreferences.defaults()), "optional preference defaults");
            require(binding.theme().isDarkMode()==(Platform.getPreferences().getColorScheme()==javafx.application.ColorScheme.DARK),
                    "SYSTEM appearance follows typed platform value");
            require(binding.reducedMotion()==Platform.getPreferences().isReducedMotion(),"SYSTEM motion follows typed platform value");
            binding.register(stage.getScene());
            require(stage.getScene().getRoot().getStylesheets().size() == 1, "reinstallation has one stylesheet");
            require(ThemeManager.instance().getTheme() instanceof MeridianTheme, "shared manager usable after disposal");
            require(originalTheme instanceof MeridianTheme, "lifecycle fixture began with authored theme");
            binding.close();
            binding = null;
        }

        private Color expected(boolean dark, String role) { return palette.get((dark ? "dark." : "light.")+role); }
        private String name() { if (variant >= 4) return "lifecycle"; return (variant >= 2 ? "dark" : "light") + "-" + (variant % 2 == 0 ? "comfortable" : "compact"); }

        private void require(boolean condition, String message) {
            if (!condition) throw new AssertionError(name() + ": " + message);
            checks.add(name()+"\t"+message+"\tpassed");
        }
        private void near(double actual, double expected, String label) {
            require(Math.abs(actual-expected) < 0.05, label+" actual="+actual+" expected="+expected);
        }
        private void color(Object actual, Color expected, String label) {
            require(expected != null && actual instanceof Color && same((Color)actual,expected),
                    label+" actual="+actual+" expected="+expected);
        }
        private void expectFailure(Runnable body, Class<? extends Throwable> type, String label) {
            Throwable thrown = null;
            try { body.run(); } catch (Throwable error) { thrown = error; }
            require(type.isInstance(thrown),label);
        }
        private void later(CheckedRunnable work) {
            Platform.runLater(() -> Platform.runLater(() -> {
                try { work.run(); } catch (Throwable error) { fail(error); }
            }));
        }
        private void finish() throws IOException {
            Files.write(output.resolve("assertions.tsv"),checks);
            Properties props = new Properties();
            for (String key : List.of("java.version","javafx.runtime.version","os.name","os.version","os.arch")) props.setProperty(key,System.getProperty(key,"unknown"));
            props.setProperty("assertions",Integer.toString(checks.size()));
            props.setProperty("window.outputScaleX",Double.toString(stage.getOutputScaleX()));
            props.setProperty("capture","Native shown stages/popup/dialog; forced pseudo-class specimens plus live focus/selection and preference lifecycle");
            try (var stream = Files.newOutputStream(output.resolve("runtime.properties"))) { props.store(stream,"A3 native theme acceptance"); }
            System.out.println("MERIDIAN_THEME_SMOKE_OK variants=4 captures=12 assertions="+checks.size());
            stage.close(); Platform.exit();
        }
        private void fail(Throwable error) {
            failure = error;
            if (menu != null) menu.hide();
            if (dialog != null) dialog.close();
            if (binding != null) binding.close();
            for (Stage extra : extraStages) extra.close();
            stage.close(); Platform.exit();
        }
    }

    @FunctionalInterface private interface CheckedRunnable { void run() throws Exception; }
    private static void styles(Parent root) { root.applyCss(); root.layout(); }
    private static Object lastFill(Region region) { return region.getBackground().getFills().getLast().getFill(); }
    private static boolean same(Color a, Color b) {
        return Math.abs(a.getRed()-b.getRed()) < 1e-7 && Math.abs(a.getGreen()-b.getGreen()) < 1e-7
                && Math.abs(a.getBlue()-b.getBlue()) < 1e-7 && Math.abs(a.getOpacity()-b.getOpacity()) < 1e-7;
    }
    @SuppressWarnings("unchecked")
    private static Object css(Styleable node, String property) {
        for (var entry : node.getCssMetaData()) {
            if (entry.getProperty().equals(property)) return ((CssMetaData<Styleable,Object>)entry).getStyleableProperty(node).getValue();
        }
        throw new AssertionError("Missing CSS metadata: "+property);
    }
    private static TableRow<?> selectedRow(TableView<?> table) {
        return table.lookupAll(".table-row-cell").stream().filter(n -> n instanceof TableRow<?> row && row.isSelected())
                .map(n -> (TableRow<?>)n).findFirst().orElseThrow();
    }

    private static final class Fixture {
        final Label title = label("Small details. One system.","meridian-title");
        final Label mode = label("Light / comfortable","meridian-secondary");
        final Button primary = button("Run analysis","accent");
        final Button secondary = button("Configure");
        final Button hover = button("Hover");
        final Button pressed = button("Pressed");
        final Button focus = button("Keyboard focus");
        final Button disabled = button("Unavailable");
        final TextField field = new TextField("Recall correspondence");
        final TextField prompt = new TextField();
        final TextField invalid = new TextField("-4");
        final TextField readonly = new TextField("Dataset identifier");
        final TextField disabledField = new TextField("Unavailable");
        final TableView<String> table = new TableView<>();
        final VBox root;

        Fixture() {
            disabled.setDisable(true); disabledField.setDisable(true);
            prompt.setPromptText("Enter an analysis name");
            readonly.setEditable(false);
            invalid.pseudoClassStateChanged(PseudoClass.getPseudoClass("invalid"),true);
            GridPane actions = new GridPane(); actions.setHgap(12); actions.setVgap(14);
            actions.add(primary,0,0); actions.add(secondary,1,0); actions.add(hover,2,0);
            actions.add(focus,0,1); actions.add(pressed,1,1); actions.add(disabled,2,1);
            VBox left = new VBox(14,label("Actions","meridian-section"), actions,
                    label("Focus, selection and pressed states remain distinct.","meridian-secondary"),
                    label("The state specimens include forced pseudo-classes.","meridian-metadata","meridian-secondary"));
            VBox inputs = new VBox(10,label("Inputs","meridian-section"),field,prompt,
                    invalid,label("Enter a positive window size.","danger","meridian-metadata"),readonly,disabledField);
            HBox top = new HBox(38,left,inputs); HBox.setHgrow(left,Priority.ALWAYS); HBox.setHgrow(inputs,Priority.ALWAYS);
            left.setMinWidth(510); inputs.setPrefWidth(420);
            TableColumn<String,String> name = new TableColumn<>("Dataset");
            name.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue()));
            name.setPrefWidth(430);
            TableColumn<String,String> state = new TableColumn<>("State");
            state.setCellValueFactory(data -> new ReadOnlyStringWrapper("Ready"));
            state.setPrefWidth(160);
            table.getColumns().add(name); table.getColumns().add(state);
            table.getItems().addAll("Naturalistic memory","Functional alignment","Hippocampal atlas","Repeat-session mapping");
            table.getSelectionModel().select(0);
            table.setPrefHeight(218);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            VBox data = new VBox(12,label("Dataset browser","meridian-section"),table);
            HBox header = new HBox(title,new Region(),mode); HBox.setHgrow(header.getChildren().get(1),Priority.ALWAYS); header.setAlignment(Pos.CENTER_LEFT);
            root = new VBox(24,header,label("Shared geometry. Clear states. Standard JavaFX controls.","meridian-secondary"),top,data,
                    label("Native JavaFX specimen · synthetic data · UI appearance does not define scientific color policy.","meridian-metadata","meridian-secondary"));
            root.setPadding(new Insets(28));
        }
    }
    private static Label label(String text, String... classes) { var x=new Label(text); x.getStyleClass().addAll(classes); return x; }
    private static Button button(String text, String... classes) { var x=new Button(text); x.getStyleClass().addAll(classes); return x; }
    private static void writePng(WritableImage image, Path output) throws IOException {
        int w=(int)image.getWidth(), h=(int)image.getHeight();
        BufferedImage png=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) png.setRGB(x,y,image.getPixelReader().getArgb(x,y));
        if (!ImageIO.write(png,"png",output.toFile())) throw new IOException("No PNG writer");
    }
}
