import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import meridian.theme.MeridianAppearance;
import meridian.theme.MeridianPreferences;
import meridian.theme.MeridianPreferences.Appearance;
import meridian.theme.MeridianPreferences.Motion;
import meridian.theme.metrics.MeridianMetrics.DensityMode;

/** Independent Java consumer: only the packaged theme, AtlantaFX and named JavaFX modules. */
public final class JavaCaller {
    public static void main(String[] args) throws Exception {
        CountDownLatch complete = new CountDownLatch(1);
        Throwable[] failure = new Throwable[1];
        Platform.startup(() -> Platform.runLater(() -> {
            Platform.setImplicitExit(false);
            try (var appearance = MeridianAppearance.install(
                    new MeridianPreferences(Appearance.LIGHT,DensityMode.COMPACT,1,Motion.REDUCED))) {
                Button button = new Button("Native Java API");
                VBox root = new VBox(button);
                Scene scene = new Scene(root,400,160);
                try (var registration = appearance.register(scene)) {
                    if (registration!=appearance.register(scene)) throw new AssertionError("Duplicate registration");
                    root.applyCss(); root.layout();
                    if (button.getFont().getSize()!=14 || button.minHeight(-1)<27.95) throw new AssertionError("Default metrics");
                    appearance.setPreferences(new MeridianPreferences(Appearance.DARK,DensityMode.COMPACT,2,Motion.REDUCED));
                    root.applyCss(); root.layout();
                    if (button.getFont().getSize()!=28 || button.minHeight(-1)<55.95) throw new AssertionError("Enlarged metrics font="+button.getFont().getSize()+" min="+button.minHeight(-1));
                    if (!appearance.theme().isDarkMode() || !scene.getPreferences().isReducedMotion()) throw new AssertionError("Preferences");
                    if (appearance.theme().getManifest().getModules().size()<40) throw new AssertionError("Missing packaged manifest");
                }
                try { Class.forName("scala.Predef"); throw new AssertionError("Scala runtime unexpectedly present"); }
                catch (ClassNotFoundException expected) { /* Independent Java classpath. */ }
                try { Class.forName("scaladock.fx.Dock"); throw new AssertionError("Dock unexpectedly present"); }
                catch (ClassNotFoundException expected) { /* Provider-free theme. */ }
                System.out.println("MERIDIAN_JAVA_CALLER_OK packagedTheme=true scala=false scaladock=false windows=0");
            } catch (Throwable error) { failure[0]=error; }
            finally { complete.countDown(); }
        }));
        try {
            if (!complete.await(30,TimeUnit.SECONDS)) throw new AssertionError("FX caller timeout");
            if (failure[0]!=null) throw new IllegalStateException("Java caller failed",failure[0]);
        } finally { Platform.exit(); }
    }
}
