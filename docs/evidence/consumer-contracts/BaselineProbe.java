package meridian.sampler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import meridian.theme.MeridianAppearance;
import meridian.theme.MeridianPreferences;
import meridian.theme.MeridianPreferences.Appearance;
import meridian.theme.MeridianPreferences.Motion;
import meridian.theme.MeridianTheme;
import meridian.theme.metrics.MeridianMetrics.DensityMode;

/** Native consumer regressions. Owns its stage and records resolved faces and table sizes. */
public final class ConsumerContractProbe {
    private static Throwable failure;
    private ConsumerContractProbe() {}
    public static void main(String[] args) {
        Application.launch(Window.class, args);
        if (failure != null) throw new IllegalStateException("Consumer contract failed", failure);
    }
    public static final class Window extends Application {
        private Path output;
        private Stage stage;
        private int step;
        private boolean baseline;
        private MeridianAppearance binding;
        private final List<String> observed = new ArrayList<>();

        @Override public void start(Stage primary) {
            stage = primary;
            try {
                var args = getParameters().getRaw();
                if (args.size() < 1 || args.size() > 2) throw new IllegalArgumentException("New output directory [baseline]");
                output = Path.of(args.getFirst()).toAbsolutePath();
                if (Files.exists(output)) throw new IllegalArgumentException("Output exists: " + output);
                Files.createDirectories(output);
                baseline = args.size() == 2 && args.get(1).equals("baseline");
                observed.add("java=" + System.getProperty("java.version") + " javafx=" + System.getProperty("javafx.runtime.version")
                        + " os=" + System.getProperty("os.name") + "/" + System.getProperty("os.arch"));
                next();
            } catch (Throwable error) { fail(error); }
        }

        private void next() {
            try {
                if (binding != null) { binding.close(); binding = null; }
                if (step == 8) { finish(); return; }
                boolean registered = step >= 4;
                boolean dark = step % 4 >= 2;
                DensityMode density = step % 2 == 0 ? DensityMode.COMFORTABLE : DensityMode.COMPACT;
                var theme = MeridianTheme.of(dark, density);
                Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
                VBox root = new VBox(12);
                root.setStyle("-fx-padding: 20px;");
                Label title = new Label("Consumer typography and status contract");
                title.getStyleClass().add("meridian-title");
                root.getChildren().add(title);
                TableView<String> table = new TableView<>();
                TableColumn<String, String> column = new TableColumn<>("Dataset");
                column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue()));
                column.setPrefWidth(450);
                table.getColumns().add(column); table.getItems().add("Synthetic dataset");
                table.setPrefHeight(120);
                root.getChildren().add(table);
                List<Label> weights = new ArrayList<>();
                for (String weight : List.of("normal", "bold", "bolder", "lighter", "500", "600")) {
                    Label label = new Label("System weight " + weight);
                    label.setStyle("-fx-font-family: 'System'; -fx-font-weight: " + weight + ";");
                    weights.add(label); root.getChildren().add(label);
                }
                Scene scene = new Scene(root, 680, 520);
                if (registered) {
                    binding = MeridianAppearance.install(new MeridianPreferences(dark ? Appearance.DARK : Appearance.LIGHT,
                            density, 1, Motion.REDUCED));
                    binding.register(scene);
                }
                stage.setScene(scene); stage.show();
                PauseTransition pause = new PauseTransition(Duration.millis(150));
                pause.setOnFinished(event -> {
                    try {
                        root.applyCss(); root.layout();
                        var header = (Label) table.lookup(".column-header .label");
                        if (header == null) throw new AssertionError("No native header");
                        double expected = baseline && !registered ? 11.9 : 12;
                        require(Math.abs(header.getFont().getSize() - expected) < .01, "header " + header.getFont());
                        observed.add("step=" + step + " registered=" + registered + " theme=" + theme + " header=" + header.getFont());
                        for (Label label : weights) observed.add(label.getText() + "=" + label.getFont());
                        step++; next();
                    } catch (Throwable error) { fail(error); }
                });
                pause.play();
            } catch (Throwable error) { fail(error); }
        }

        private void finish() throws Exception {
            Files.write(output.resolve("observed.txt"), observed);
            stage.close(); Platform.exit();
            System.out.println("MERIDIAN_CONSUMER_BASELINE_OK variants=8");
        }
        private void fail(Throwable error) {
            failure = error; error.printStackTrace();
            if (binding != null) binding.close();
            stage.close(); Platform.exit();
        }
        private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    }
}
