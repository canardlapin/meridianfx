package meridian.sampler;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import meridian.theme.MeridianAppearance;
import meridian.theme.MeridianPreferences;
import meridian.theme.MeridianPreferences.Appearance;
import meridian.theme.MeridianPreferences.Motion;
import meridian.theme.MeridianTheme;
import meridian.theme.metrics.MeridianMetrics.DensityMode;
import meridian.workbench.StatusIndicator;
import meridian.workbench.StatusIndicator.Status;

/** Native consumer regressions on the qualified System-font platform; synthetic content only. */
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
        private MeridianAppearance binding;
        private String previousUserAgent;
        private final List<String> observed = new ArrayList<>();
        private final Map<String, Color> palette = new HashMap<>();
        private final Map<String, String> faces = Map.of("normal", "System Regular", "bold", "System Bold",
                "bolder", "System Bold", "lighter", "System Regular");
        private int checkedStatuses;

        @Override public void start(Stage primary) {
            stage = primary;
            try {
                if (getParameters().getRaw().size() != 1) throw new IllegalArgumentException("New output directory required");
                output = Path.of(getParameters().getRaw().getFirst()).toAbsolutePath();
                if (Files.exists(output)) throw new IllegalArgumentException("Output exists: " + output);
                Files.createDirectories(output);
                require(System.getProperty("os.name").startsWith("Mac"),
                        "No qualified System face profile for this OS; record and qualify one before adding a lane");
                previousUserAgent = Application.getUserAgentStylesheet();
                observed.add("java=" + System.getProperty("java.version") + " javafx=" + System.getProperty("javafx.runtime.version")
                        + " os=" + System.getProperty("os.name") + "/" + System.getProperty("os.arch"));
                observed.add("System faces=" + Font.getFontNames("System"));
                for (String line : Files.readAllLines(Path.of("tokens/generated/native-expected.tsv"))) {
                    var parts = line.split("\t");
                    if (parts[0].equals("theme") && parts[2].startsWith("-meridian-"))
                        palette.put(parts[1] + "." + parts[2].substring(10), Color.web(parts[3]));
                }
                next();
            } catch (Throwable error) { fail(error); }
        }

        private void next() {
            try {
                if (binding != null) { binding.close(); binding = null; }
                if (step == 12) { finish(); return; }
                boolean registered = step >= 4;
                boolean dark = step % 4 >= 2;
                double scale = step >= 8 ? 1.5 : 1;
                DensityMode density = step % 2 == 0 ? DensityMode.COMFORTABLE : DensityMode.COMPACT;
                var theme = MeridianTheme.of(dark, density);
                Application.setUserAgentStylesheet(theme.getUserAgentStylesheet());
                VBox root = new VBox(12);
                root.setStyle("-fx-padding: 20px;");
                Label title = new Label("Status belongs to the data");
                title.getStyleClass().add("meridian-title");
                root.getChildren().add(title);
                root.getChildren().add(new Label(theme + (registered ? " · registered" : " · standalone") + " · text scale " + scale));

                TableView<String> table = new TableView<>();
                TableColumn<String, String> column = new TableColumn<>("Dataset");
                column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue()));
                column.setPrefWidth(600);
                table.getColumns().add(column); table.getItems().add("Synthetic dataset");
                table.setPrefHeight(110 * scale);
                root.getChildren().add(table);

                List<StatusIndicator> statuses = new ArrayList<>();
                HBox surfaces = new HBox(20);
                for (String surface : List.of("canvas", "shell", "raised")) {
                    VBox panel = new VBox(12);
                    panel.setStyle("-fx-padding: 12px; -fx-background-color: -meridian-surface-" + surface + ";");
                    panel.getChildren().add(new Label(surface));
                    for (Status status : Status.values()) {
                        StatusIndicator indicator = new StatusIndicator(switch (status) {
                            case NEUTRAL -> "Stale · detached";
                            case PROGRESS -> "Running";
                            case SUCCESS -> "Ready";
                            case WARNING -> "Needs review";
                            case DANGER -> "Failed";
                        }, status);
                        statuses.add(indicator); panel.getChildren().add(indicator);
                    }
                    surfaces.getChildren().add(panel);
                }
                root.getChildren().add(surfaces);
                StatusIndicator longStatus = new StatusIndicator(
                        "A long translated status remains readable when the text scale increases — المصدر متاح",
                        Status.NEUTRAL);
                longStatus.setWrapText(true); longStatus.setMaxWidth(600 * scale);
                root.getChildren().add(longStatus);

                List<Label> weights = new ArrayList<>();
                String css;
                try (var stream = ConsumerContractProbe.class.getResourceAsStream(theme.getUserAgentStylesheet())) {
                    require(stream != null, "Theme resource missing");
                    css = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                }
                var matcher = Pattern.compile("-fx-font-weight:\\s*([^;]+);").matcher(css);
                Set<String> emitted = new TreeSet<>();
                while (matcher.find()) emitted.add(matcher.group(1).trim());
                require(!emitted.isEmpty() && faces.keySet().containsAll(emitted), "Undocumented emitted font weight " + emitted);
                HBox faceRow = new HBox(12);
                for (String weight : emitted) {
                    Label label = new Label(weight);
                    label.setStyle("-fx-font-family: 'System'; -fx-font-weight: " + weight + ";");
                    weights.add(label); faceRow.getChildren().add(label);
                }
                root.getChildren().add(faceRow);
                Scene scene = new Scene(root, 760 * scale, 560 * scale);
                if (registered) {
                    binding = MeridianAppearance.install(new MeridianPreferences(dark ? Appearance.DARK : Appearance.LIGHT,
                            density, scale, Motion.REDUCED));
                    binding.register(scene);
                }
                stage.setScene(scene); stage.show();
                PauseTransition pause = new PauseTransition(Duration.millis(150));
                pause.setOnFinished(event -> {
                    try {
                        root.applyCss(); root.layout();
                        var headers = table.lookupAll(".column-header .label");
                        require(!headers.isEmpty(), "No native header");
                        for (var node : headers) {
                            var header = (Label) node;
                            near(header.getFont().getSize(), 12 * scale, "header size");
                            require(header.getFont().getName().equals("System Regular"), "header face " + header.getFont());
                        }
                        near(((javafx.scene.control.TableCell<?, ?>) table.lookup(".table-cell")).getFont().getSize(), 13 * scale, "table cell size");
                        require(title.getFont().getName().equals("System Bold"), "title face");
                        for (Label label : weights) {
                            require(label.getFont().getName().equals(faces.get(label.getText())),
                                    "Undocumented native face for " + label.getText() + ": " + label.getFont());
                            observed.add("step=" + step + " weight=" + label.getText() + " face=" + label.getFont().getName());
                        }
                        for (int i = 0; i < statuses.size(); i++)
                            verifyStatus(statuses.get(i), dark, List.of("canvas", "shell", "raised").get(i / Status.values().length), scale);
                        verifyMutation(statuses.getFirst(), root, dark, scale);
                        require(longStatus.getHeight() >= 12 * scale && longStatus.getHeight() < root.getHeight(), "wrapping status visible");
                        var image = scene.snapshot(null);
                        BufferedImage buffered = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                        int[] pixels = new int[buffered.getWidth() * buffered.getHeight()];
                        image.getPixelReader().getPixels(0, 0, buffered.getWidth(), buffered.getHeight(),
                                javafx.scene.image.PixelFormat.getIntArgbInstance(), pixels, 0, buffered.getWidth());
                        buffered.setRGB(0, 0, buffered.getWidth(), buffered.getHeight(), pixels, 0, buffered.getWidth());
                        ImageIO.write(buffered, "png", output.resolve("step-" + step + ".png").toFile());
                        observed.add("step=" + step + " registered=" + registered + " theme=" + theme
                                + " scale=" + scale + " header=" + ((Label) headers.iterator().next()).getFont());
                        step++; next();
                    } catch (Throwable error) { fail(error); }
                });
                pause.play();
            } catch (Throwable error) { fail(error); }
        }

        private void verifyStatus(StatusIndicator status, boolean dark, String surface, double scale) {
            String mode = dark ? "dark" : "light";
            Color expected = palette.get(mode + ".status-" + status.getStatus().name().toLowerCase(java.util.Locale.ROOT));
            Color actual = (Color) status.getTextFill();
            color(actual, expected, "status label");
            Region dot = (Region) status.getGraphic();
            color((Color) dot.getBackground().getFills().getFirst().getFill(), expected, "status dot");
            Color background = palette.get(mode + ".surface-" + surface);
            require(contrast(actual, background) >= 4.5 && contrast(expected, background) >= 3, "status contrast");
            near(status.getFont().getSize(), 12 * scale, "status text scale");
            require(dot.getWidth() >= 5 * scale && Math.abs(dot.getWidth() - dot.getHeight()) < .01, "round visible dot");
            require(status.getAccessibleRole() == AccessibleRole.TEXT && !status.isFocusTraversable(), "noninteractive status semantics");
            require(status.queryAccessibleAttribute(AccessibleAttribute.TEXT).equals(status.getText()), "accessible label");
            checkedStatuses++;
        }

        private void verifyMutation(StatusIndicator status, VBox root, boolean dark, double scale) {
            status.getStyleClass().add("host-owned");
            for (Status value : Status.values()) status.setStatus(value);
            status.setStatus(Status.PROGRESS);
            status.setStatus(Status.PROGRESS);
            require(status.getStyleClass().contains("host-owned"), "foreign style class");
            require(status.getStyleClass().stream().filter(s -> s.startsWith("status-")).count() == 1, "one status class");
            try { status.setStatus(null); throw new AssertionError("null status accepted"); }
            catch (NullPointerException expected) { require(status.getStatus() == Status.PROGRESS, "rejected mutation is atomic"); }
            var text = new SimpleStringProperty("Updated running state");
            status.textProperty().bind(text); text.set("Running · updated");
            require(status.getAccessibleText().equals(text.get()), "accessible bound text follows updates");
            root.applyCss(); root.layout();
            verifyStatus(status, dark, "canvas", scale);
            status.textProperty().unbind();
        }

        private void finish() throws Exception {
            Files.write(output.resolve("observed.txt"), observed);
            Application.setUserAgentStylesheet(previousUserAgent);
            stage.close(); Platform.exit();
            System.out.println("MERIDIAN_CONSUMER_CONTRACT_OK variants=12 captures=12 statuses=" + checkedStatuses + " windowsClosed=true");
        }
        private void fail(Throwable error) {
            failure = error; error.printStackTrace();
            try {
                Files.write(output.resolve("partial-observed.txt"), observed);
                if (binding != null) binding.close();
                Application.setUserAgentStylesheet(previousUserAgent);
            } catch (Exception ignored) { error.addSuppressed(ignored); }
            stage.close(); Platform.exit();
        }
        private static void near(double actual, double expected, String label) { require(Math.abs(actual - expected) < .01, label + ": " + actual + " != " + expected); }
        private static void color(Color actual, Color expected, String label) { require(actual != null && actual.equals(expected), label + ": " + actual + " != " + expected); }
        private static double contrast(Color a, Color b) { double x = luminance(a), y = luminance(b); return (Math.max(x, y) + .05) / (Math.min(x, y) + .05); }
        private static double luminance(Color color) {
            double[] rgb = {color.getRed(), color.getGreen(), color.getBlue()};
            for (int i = 0; i < rgb.length; i++) rgb[i] = rgb[i] <= .04045 ? rgb[i] / 12.92 : Math.pow((rgb[i] + .055) / 1.055, 2.4);
            return rgb[0] * .2126 + rgb[1] * .7152 + rgb[2] * .0722;
        }
        private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    }
}
