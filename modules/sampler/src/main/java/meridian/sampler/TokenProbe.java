package meridian.sampler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import meridian.theme.metrics.MeridianMetrics;

/** Parses real generated JavaFX resources on the FX thread without opening a window. */
public final class TokenProbe {
    private TokenProbe() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Expected input TSV and new output TSV");
        Path input = Path.of(args[0]).toAbsolutePath();
        Path output = Path.of(args[1]).toAbsolutePath();
        if (Files.exists(output)) throw new IllegalArgumentException("Output already exists: " + output);
        List<String> requested = Files.readAllLines(input);
        CountDownLatch done = new CountDownLatch(1);
        Throwable[] failure = new Throwable[1];
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            try {
                List<String> observed = new ArrayList<>();
                for (String scope : List.of("theme", "dock")) {
                    for (String appearance : List.of("dark", "light")) {
                        VBox root = new VBox();
                        if (scope.equals("dock")) root.getStyleClass().add("dock");
                        Region swatch = new Region();
                        root.getChildren().add(swatch);
                        Scene scene = new Scene(root, 100, 100);
                        String resource = scope.equals("theme")
                                ? "/meridian/theme/tokens-" + appearance + ".css"
                                : "/meridian/scaladock/tokens-" + appearance + ".css";
                        var url = TokenProbe.class.getResource(resource);
                        if (url == null) throw new IllegalStateException("Missing resource: " + resource);
                        scene.getStylesheets().add(url.toExternalForm());
                        for (String line : requested) {
                            String[] fields = line.split("\t");
                            if (!fields[0].equals(scope) || !fields[1].equals(appearance)) continue;
                            swatch.setStyle("-fx-background-color: " + fields[2] + ";");
                            root.applyCss();
                            root.layout();
                            if (swatch.getBackground() == null) throw new IllegalStateException("Unresolved token " + line);
                            Color color = (Color) swatch.getBackground().getFills().getFirst().getFill();
                            observed.add(scope + "\t" + appearance + "\t" + fields[2] + "\t"
                                    + color.getRed() + "\t" + color.getGreen() + "\t"
                                    + color.getBlue() + "\t" + color.getOpacity());
                        }
                    }
                }
                var compact = MeridianMetrics.density(MeridianMetrics.DensityMode.COMPACT);
                if (compact.controlHeightFor(70) != 70) throw new IllegalStateException("Measured height capped");
                if (compact.headerHeightFor(90) != 90) throw new IllegalStateException("Header height capped");
                if (compact.scaledForText(2).controlMinHeight() != 56) throw new IllegalStateException("Text enlargement");
                if (MeridianMetrics.TYPOGRAPHY.scaledForText(2).metadata() != 24) throw new IllegalStateException("Metadata enlargement");
                if (MeridianMetrics.GEOMETRY.iconSize() >= compact.iconTargetMinSize()) throw new IllegalStateException("Icon target not independent");
                if (MeridianMetrics.MOTION.reduced().overlayMs() != 0) throw new IllegalStateException("Reduced motion");
                try {
                    compact.controlHeightFor(Double.NaN);
                    throw new IllegalStateException("Non-finite measurement accepted");
                } catch (IllegalArgumentException expected) {
                    // Expected public API guard.
                }
                try {
                    compact.scaledForText(0.5);
                    throw new IllegalStateException("Typography shrinking accepted");
                } catch (IllegalArgumentException expected) {
                    // Default typography floors remain intact.
                }
                Files.createDirectories(output.getParent());
                Files.write(output, observed);
                System.out.println("MERIDIAN_TOKEN_PROBE_OK colors=" + observed.size() + " metrics=passed windows=0");
            } catch (Throwable error) {
                failure[0] = error;
            } finally {
                done.countDown();
            }
        });
        try {
            if (!done.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("FX token probe timed out");
            if (failure[0] != null) throw new IllegalStateException("Token probe failed", failure[0]);
        } finally {
            Platform.exit();
        }
    }
}
