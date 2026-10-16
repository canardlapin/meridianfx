package meridian.sampler;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.css.PseudoClass;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import meridian.theme.MeridianAppearance;
import meridian.theme.MeridianPreferences;
import meridian.theme.MeridianPreferences.Appearance;
import meridian.theme.MeridianPreferences.Motion;
import meridian.theme.metrics.MeridianMetrics.DensityMode;

/** Computed native focus-visible render regression; pseudo-class is explicitly controlled, not OS input. */
public final class DataFocusProbe {
    private static Throwable failure;
    private DataFocusProbe() {}
    public static void main(String[] args) {
        Application.launch(Window.class, args);
        if (failure != null) throw new IllegalStateException("Data view focus contract failed", failure);
    }

    public static final class Window extends Application {
        private static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");
        private static final PseudoClass FOCUS_VISIBLE = PseudoClass.getPseudoClass("focus-visible");
        private final List<String> observations = new ArrayList<>();
        private final Map<String, Color> palette = new HashMap<>();
        private final List<Control> views = new ArrayList<>();
        private Path output;
        private Stage stage;
        private VBox root;
        private Button focusSink;
        private MeridianAppearance appearance;
        private boolean baseline;
        private int phase;
        private int captures;

        @Override public void start(Stage primary) {
            stage = primary;
            try {
                var args = getParameters().getRaw();
                require(args.size() == 1 || args.size() == 2 && args.get(1).equals("baseline"), "New output directory [baseline]");
                output = Path.of(args.getFirst()).toAbsolutePath();
                require(!Files.exists(output), "Output already exists");
                Files.createDirectories(output);
                baseline = args.size() == 2;
                observations.add("java=" + System.getProperty("java.version") + " javafx=" + System.getProperty("javafx.runtime.version")
                        + " os=" + System.getProperty("os.name") + "/" + System.getProperty("os.arch"));
                observations.add("Input scope: real scene focusOwner; controlled focused/focus-visible pseudo-classes; OS activation is recorded, not assumed; no OS input injection");
                for (String line : Files.readAllLines(Path.of("tokens/generated/native-expected.tsv"))) {
                    var parts = line.split("\t");
                    if (parts[0].equals("theme") && parts[2].startsWith("-meridian-"))
                        palette.put(parts[1] + "." + parts[2].substring(10), Color.web(parts[3]));
                }
                ListView<String> list = new ListView<>();
                list.getItems().addAll("Synthetic dataset", "Reference dataset");
                list.getSelectionModel().selectFirst();
                TreeView<String> tree = new TreeView<>(new TreeItem<>("Synthetic workspace"));
                tree.getRoot().getChildren().add(new TreeItem<>("Dataset"));
                tree.getRoot().setExpanded(true);
                tree.getSelectionModel().selectFirst();
                TableView<String> table = new TableView<>();
                TableColumn<String, String> column = new TableColumn<>("Dataset");
                column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue()));
                table.getColumns().add(column); table.getItems().add("Synthetic dataset");
                table.getSelectionModel().selectFirst();
                views.addAll(List.of(list, tree, table));
                HBox row = new HBox(16);
                for (Control view : views) {
                    view.setPrefSize(230, 190);
                    view.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(view, Priority.ALWAYS); row.getChildren().add(view);
                }
                focusSink = new Button("Focus outside the data views");
                root = new VBox(16, new Label("Data view focus · controlled native states"), row, focusSink);
                root.setStyle("-fx-padding: 20px;");
                Scene scene = new Scene(root, 780, 300);
                appearance = MeridianAppearance.install(new MeridianPreferences(Appearance.LIGHT, DensityMode.COMFORTABLE, 1, Motion.REDUCED));
                appearance.register(scene); stage.setScene(scene); stage.show(); stage.requestFocus();
                next();
            } catch (Throwable error) { fail(error); }
        }

        private void next() {
            try {
                if (phase == 48) { finish(); return; }
                int variant = phase / 12, index = phase % 12 / 4, state = phase % 4;
                boolean dark = variant >= 2;
                DensityMode density = variant % 2 == 0 ? DensityMode.COMFORTABLE : DensityMode.COMPACT;
                if (phase % 12 == 0)
                    appearance.setPreferences(new MeridianPreferences(dark ? Appearance.DARK : Appearance.LIGHT, density, 1, Motion.REDUCED));
                Control target = views.get(index);
                if (state == 0) focusSink.requestFocus(); else target.requestFocus();
                PauseTransition pause = new PauseTransition(Duration.millis(150));
                pause.setOnFinished(event -> {
                    try {
                        // macOS need not activate an unattended test window. Verify the real scene
                        // owner independently, and control the rendered CSS states explicitly.
                        for (Control view : views) {
                            view.pseudoClassStateChanged(FOCUS_VISIBLE, false);
                            view.pseudoClassStateChanged(FOCUSED, false);
                        }
                        target.pseudoClassStateChanged(FOCUSED, state != 0);
                        target.pseudoClassStateChanged(FOCUS_VISIBLE, state == 2);
                        root.applyCss(); root.layout();
                        require(stage.getScene().getFocusOwner() == (state == 0 ? focusSink : target), "Scene focus ownership at phase " + phase);
                        require(target.getPseudoClassStates().contains(FOCUS_VISIBLE) == (state == 2), "Visible focus state");
                        boolean ring = state == 2 && (!baseline || index == 2);
                        Color expected = palette.get((dark ? "dark" : "light") + (ring ? ".focus-ring" : ".border-decorative"));
                        var strokes = target.getBorder().getStrokes();
                        require(strokes.size() == 1, "One view border");
                        var stroke = strokes.getFirst();
                        double width = ring ? 2 : 1;
                        require(stroke.getTopStroke().equals(expected) && stroke.getBottomStroke().equals(expected)
                                && stroke.getLeftStroke().equals(expected) && stroke.getRightStroke().equals(expected), "Border color at phase " + phase);
                        for (double value : List.of(stroke.getWidths().getTop(), stroke.getWidths().getRight(),
                                stroke.getWidths().getBottom(), stroke.getWidths().getLeft()))
                            require(Math.abs(value - width) < .01, "Border width at phase " + phase + ": " + value);
                        observations.add("variant=" + variant + " density=" + density + " dark=" + dark + " control="
                                + target.getClass().getSimpleName() + " state=" + state + " focusedCss=" + (state != 0) + " nativeFocused=" + target.isFocused() + " windowFocused=" + stage.isFocused()
                                + " focusVisible=" + (state == 2) + " color=" + expected + " width=" + width);
                        if (state == 2) snapshot("variant-" + variant + "-" + target.getClass().getSimpleName() + ".png");
                        phase++; next();
                    } catch (Throwable error) { fail(error); }
                });
                pause.play();
            } catch (Throwable error) { fail(error); }
        }

        private void snapshot(String name) throws Exception {
            var image = stage.getScene().snapshot(null);
            BufferedImage buffered = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            int[] pixels = new int[buffered.getWidth() * buffered.getHeight()];
            image.getPixelReader().getPixels(0, 0, buffered.getWidth(), buffered.getHeight(),
                    javafx.scene.image.PixelFormat.getIntArgbInstance(), pixels, 0, buffered.getWidth());
            buffered.setRGB(0, 0, buffered.getWidth(), buffered.getHeight(), pixels, 0, buffered.getWidth());
            ImageIO.write(buffered, "png", output.resolve(name).toFile()); captures++;
        }
        private void finish() throws Exception {
            Files.write(output.resolve("observed.txt"), observations);
            appearance.close(); stage.close();
            require(javafx.stage.Window.getWindows().stream().noneMatch(javafx.stage.Window::isShowing), "Owned windows closed");
            Platform.exit();
            System.out.println("MERIDIAN_DATA_FOCUS_OK baseline=" + baseline + " states=" + phase + " captures=" + captures + " windowsClosed=true");
        }
        private void fail(Throwable error) {
            failure = error; error.printStackTrace();
            try { if (output != null) Files.write(output.resolve("partial-observed.txt"), observations); }
            catch (Exception writeError) { error.addSuppressed(writeError); }
            if (appearance != null) appearance.close();
            stage.close(); Platform.exit();
        }
        private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    }
}
