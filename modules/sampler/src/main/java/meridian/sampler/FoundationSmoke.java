package meridian.sampler;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import javax.imageio.ImageIO;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import meridian.theme.FoundationTheme;
import meridian.workbench.ViewHeader;

/** Bounded real-window foundation probe; this is not the finished Meridian design. */
public final class FoundationSmoke {
    private static volatile Throwable failure;

    private FoundationSmoke() {}

    public static void main(String[] args) {
        Application.launch(SmokeWindow.class, args);
        if (failure != null) {
            throw new IllegalStateException("Native foundation smoke failed", failure);
        }
    }

    public static final class SmokeWindow extends Application {
        @Override
        public void start(Stage stage) {
            try {
                if (getParameters().getRaw().size() != 1) {
                    throw new IllegalArgumentException("Pass a new output directory.");
                }
                Path output = Path.of(getParameters().getRaw().getFirst()).toAbsolutePath();
                if (Files.exists(output)) {
                    throw new IllegalArgumentException("Output already exists: " + output);
                }
                Files.createDirectories(output);
                FoundationTheme.apply();
                ViewHeader header = new ViewHeader("MeridianFX foundation",
                        "Pinned native runtime · AtlantaFX Primer Light baseline");
                TextField field = new TextField("Native controls are ready");
                Button action = new Button("Continue");
                VBox content = new VBox(12, header, field, action);
                content.setStyle("-fx-padding: 28; -fx-font-size: 14px;");
                Scene scene = new Scene(content, 620, 250);
                stage.setTitle("MeridianFX — foundation smoke");
                stage.setScene(scene);
                stage.show();
                Platform.runLater(() -> {
                    try {
                        content.applyCss();
                        content.layout();
                        WritableImage snapshot = scene.snapshot(null);
                        writePng(snapshot, output.resolve("foundation.png"));
                        Properties report = new Properties();
                        report.setProperty("java.version", System.getProperty("java.version"));
                        report.setProperty("java.home", System.getProperty("java.home"));
                        report.setProperty("javafx.version", System.getProperty("javafx.version", "unknown"));
                        report.setProperty("javafx.runtime.version", System.getProperty("javafx.runtime.version", "unknown"));
                        report.setProperty("os.name", System.getProperty("os.name"));
                        report.setProperty("os.version", System.getProperty("os.version"));
                        report.setProperty("os.arch", System.getProperty("os.arch"));
                        report.setProperty("theme", "AtlantaFX 3.0.0 PrimerLight (foundation only)");
                        report.setProperty("stylesheet", FoundationTheme.stylesheet());
                        report.setProperty("font", field.getFont().toString());
                        report.setProperty("window.outputScaleX", Double.toString(stage.getOutputScaleX()));
                        report.setProperty("window.outputScaleY", Double.toString(stage.getOutputScaleY()));
                        report.setProperty("scene.width", Double.toString(scene.getWidth()));
                        report.setProperty("scene.height", Double.toString(scene.getHeight()));
                        report.setProperty("renderer", "automatic; actual pipeline recorded in launch log");
                        report.setProperty("capture", "JavaFX Scene.snapshot from a shown native Stage");
                        try (var stream = Files.newOutputStream(output.resolve("runtime.properties"))) {
                            report.store(stream, "MeridianFX native foundation smoke");
                        }
                        System.out.println("MERIDIAN_FOUNDATION_SMOKE_OK " + output);
                    } catch (Throwable error) {
                        failure = error;
                    } finally {
                        stage.close();
                        Platform.exit();
                    }
                });
            } catch (Throwable error) {
                failure = error;
                stage.close();
                Platform.exit();
            }
        }
    }

    private static void writePng(WritableImage image, Path output) throws IOException {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        BufferedImage png = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var pixels = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                png.setRGB(x, y, pixels.getArgb(x, y));
            }
        }
        if (!ImageIO.write(png, "png", output.toFile())) {
            throw new IOException("No PNG image writer");
        }
    }
}
