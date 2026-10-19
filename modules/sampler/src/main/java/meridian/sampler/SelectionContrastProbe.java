package meridian.sampler;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import meridian.theme.MeridianAppearance;
import meridian.theme.MeridianPreferences;
import meridian.theme.MeridianPreferences.Appearance;
import meridian.theme.MeridianPreferences.Motion;
import meridian.theme.metrics.MeridianMetrics.DensityMode;

/** Rendered selection contract; CSS hover/focus states are controlled, not OS input. */
public final class SelectionContrastProbe {
    private static Throwable failure;
    private SelectionContrastProbe() {}
    public static void main(String[] args) {
        Application.launch(Window.class, args);
        if (failure != null) throw new IllegalStateException("Selection contrast failed", failure);
    }

    public static final class Window extends Application {
        private static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");
        private static final PseudoClass HOVER = PseudoClass.getPseudoClass("hover");
        private final List<String> observations = new ArrayList<>();
        private final List<String> violations = new ArrayList<>();
        private final List<Control> views = new ArrayList<>();
        private Stage stage;
        private VBox root;
        private Button sink;
        private MeridianAppearance appearance;
        private Path output;
        private int phase;

        @Override public void start(Stage primary) {
            stage = primary;
            try {
                require(getParameters().getRaw().size() == 1, "Supply a new output directory");
                output = Path.of(getParameters().getRaw().getFirst()).toAbsolutePath();
                require(!Files.exists(output), "Output already exists");
                Files.createDirectories(output);
                Files.writeString(output.resolve("runtime.txt"), "java=" + System.getProperty("java.version")
                        + " javafx=" + System.getProperty("javafx.runtime.version") + " os="
                        + System.getProperty("os.name") + "/" + System.getProperty("os.arch")
                        + "\nReal scene focusOwner; controlled CSS focused/hover; no OS input injection.\n");
                observations.add("appearance\tdensity\tcontrol\tactive\thover\tfill\tcell\tcue\tfill_vs_cell\tcue_vs_cell\tcue_vs_fill\twindow_focused");
                ListView<String> list = new ListView<>();
                list.getItems().addAll("Selected dataset", "Reference dataset", "Validation dataset");
                list.getSelectionModel().selectFirst();
                TreeItem<String> branch = new TreeItem<>("Selected branch");
                branch.getChildren().addAll(List.of(new TreeItem<>("Reference revision"), new TreeItem<>("Validation revision")));
                branch.setExpanded(true);
                TreeView<String> tree = new TreeView<>(branch);
                tree.getSelectionModel().selectFirst();
                views.addAll(List.of(list, tree));
                for (Control view : views) view.setPrefSize(300, 180);
                sink = new Button("Focus outside selection");
                root = new VBox(16, new HBox(16, list, tree), sink);
                root.setStyle("-fx-padding: 20px;");
                Scene scene = new Scene(root, 656, 270);
                appearance = MeridianAppearance.install(new MeridianPreferences(Appearance.LIGHT, DensityMode.COMFORTABLE, 1, Motion.REDUCED));
                appearance.register(scene);
                stage.setScene(scene); stage.show();
                next();
            } catch (Throwable error) { fail(error); }
        }

        private void next() {
            if (phase == 32) {
                try {
                    Files.write(output.resolve("observed.tsv"), observations);
                    Files.write(output.resolve("violations.txt"), violations);
                    require(violations.isEmpty(), violations.size() + " failed states; see violations.txt");
                    close();
                    System.out.println("MERIDIAN_SELECTION_OK states=32 windowsClosed=true");
                } catch (Throwable error) { fail(error); }
                return;
            }
            int variant = phase / 8, index = phase % 8 / 4, state = phase % 4;
            boolean dark = variant >= 2, active = state >= 2, hover = state % 2 == 1;
            DensityMode density = variant % 2 == 0 ? DensityMode.COMFORTABLE : DensityMode.COMPACT;
            if (phase % 8 == 0)
                appearance.setPreferences(new MeridianPreferences(dark ? Appearance.DARK : Appearance.LIGHT, density, 1, Motion.REDUCED));
            PauseTransition pause = new PauseTransition(Duration.millis(120));
            pause.setOnFinished(event -> {
                try {
                    root.applyCss(); root.layout();
                    Control view = views.get(index);
                    if (active) view.requestFocus(); else sink.requestFocus();
                    require(stage.getScene().getFocusOwner() == (active ? view : sink), "Scene focus owner");
                    for (Control control : views) {
                        control.pseudoClassStateChanged(FOCUSED, control == view && active);
                        for (var cell : control.lookupAll(".cell")) cell.pseudoClassStateChanged(HOVER, false);
                    }
                    IndexedCell<?> selected = cell(view, 0), other = cell(view, 1);
                    selected.pseudoClassStateChanged(HOVER, hover);
                    root.applyCss(); root.layout();
                    require(selected.isSelected() && !other.isSelected(), "Selection model state");
                    var fills = selected.getBackground().getFills();
                    Color fill = (Color) fills.getLast().getFill();
                    Color background = (Color) other.getBackground().getFills().getLast().getFill();
                    var rendered = selected.snapshot(null, null);
                    int y = (int) selected.getHeight() / 2;
                    // Mid-left of the inset pill, away from text and rounded corners.
                    Color cue = rendered.getPixelReader().getColor(5, y);
                    Color interior = rendered.getPixelReader().getColor(8, y);
                    require(near(interior, fill), "Visible selection interior matches computed fill");
                    require(near(other.snapshot(null, null).getPixelReader().getColor(4, y), background), "Unselected cell background");
                    double outside = contrast(cue, background), inside = contrast(cue, fill);
                    String id = (dark ? "dark" : "light") + "-" + density + "-" + view.getClass().getSimpleName() + "-" + state;
                    observations.add(String.format(Locale.ROOT, "%s\t%s\t%s\t%b\t%b\t%s\t%s\t%s\t%.9f\t%.9f\t%.9f\t%b",
                            dark ? "dark" : "light", density, view.getClass().getSimpleName(), active, hover,
                            hex(fill), hex(background), hex(cue), contrast(fill, background), outside, inside, stage.isFocused()));
                    if (outside < 3 || inside < 3) violations.add(id + " cue/cell=" + outside + " cue/fill=" + inside);
                    // Recycling/selection changes must remove the cue from the old row.
                    if (view instanceof ListView<?> list) list.getSelectionModel().clearSelection();
                    else ((TreeView<?>) view).getSelectionModel().clearSelection();
                    selected.pseudoClassStateChanged(HOVER, false);
                    root.applyCss(); root.layout();
                    require(!selected.isSelected(), "Cleared selection");
                    require(near(selected.snapshot(null, null).getPixelReader().getColor(4, y), background), "No lingering cue after clearing selection");
                    if (view instanceof ListView<?> list) list.getSelectionModel().selectFirst();
                    else ((TreeView<?>) view).getSelectionModel().selectFirst();
                    selected.pseudoClassStateChanged(HOVER, hover);
                    root.applyCss(); root.layout();
                    if (!hover) snapshot(id + ".png");
                    phase++; next();
                } catch (Throwable error) { fail(error); }
            });
            pause.play();
        }

        private static IndexedCell<?> cell(Control view, int index) {
            return view.lookupAll(view instanceof ListView<?> ? ".list-cell" : ".tree-cell").stream()
                    .filter(node -> node instanceof IndexedCell<?> cell && cell.getIndex() == index)
                    .map(node -> (IndexedCell<?>) node).findFirst().orElseThrow();
        }
        private void snapshot(String name) throws Exception {
            var image = stage.getScene().snapshot(null);
            BufferedImage buffered = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            int[] pixels = new int[buffered.getWidth() * buffered.getHeight()];
            image.getPixelReader().getPixels(0, 0, buffered.getWidth(), buffered.getHeight(),
                    javafx.scene.image.PixelFormat.getIntArgbInstance(), pixels, 0, buffered.getWidth());
            buffered.setRGB(0, 0, buffered.getWidth(), buffered.getHeight(), pixels, 0, buffered.getWidth());
            ImageIO.write(buffered, "png", output.resolve(name).toFile());
        }
        private void close() {
            if (appearance != null) appearance.close();
            stage.close();
            require(javafx.stage.Window.getWindows().stream().noneMatch(javafx.stage.Window::isShowing), "Owned windows closed");
            Platform.exit();
        }
        private void fail(Throwable error) {
            failure = error; error.printStackTrace();
            try { if (output != null) Files.write(output.resolve("observed.tsv"), observations); }
            catch (Exception writeError) { error.addSuppressed(writeError); }
            close();
        }
        private static String hex(Color color) {
            return String.format(Locale.ROOT, "#%02X%02X%02X", Math.round(color.getRed() * 255), Math.round(color.getGreen() * 255), Math.round(color.getBlue() * 255));
        }
        private static boolean near(Color a, Color b) {
            return Math.abs(a.getRed() - b.getRed()) < .005 && Math.abs(a.getGreen() - b.getGreen()) < .005
                    && Math.abs(a.getBlue() - b.getBlue()) < .005 && a.getOpacity() == 1 && b.getOpacity() == 1;
        }
        private static double luminance(Color color) {
            double[] channels = {color.getRed(), color.getGreen(), color.getBlue()};
            for (int i = 0; i < channels.length; i++)
                channels[i] = channels[i] <= .04045 ? channels[i] / 12.92 : Math.pow((channels[i] + .055) / 1.055, 2.4);
            return channels[0] * .2126 + channels[1] * .7152 + channels[2] * .0722;
        }
        private static double contrast(Color a, Color b) {
            double x = luminance(a), y = luminance(b);
            return (Math.max(x, y) + .05) / (Math.min(x, y) + .05);
        }
        private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    }
}
