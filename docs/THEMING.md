# Applying Meridian themes

Meridian's initial user-agent themes compile the locked AtlantaFX 3.0.0 modules
with authored semantic colors, configuration and a small control-recipe delta.
They cover the A3 native fixture; the exhaustive control and docking gates remain
later work. JDK25 and JavaFX27 are required. No application runtime upgrade is implied.

## Java and Scala callers

Install once on the JavaFX Application Thread, register each application-owned
scene, and close at application shutdown. The following calls are compiled and
executed by the independent [Java consumer](evidence/a3/JavaCaller.java).

```java
import meridian.theme.MeridianAppearance;
import meridian.theme.MeridianPreferences;
import meridian.theme.MeridianPreferences.Appearance;
import meridian.theme.MeridianPreferences.Motion;
import meridian.theme.metrics.MeridianMetrics.DensityMode;

// Inside Application.start(), after creating your Scene:
var appearance = MeridianAppearance.install(
    new MeridianPreferences(Appearance.LIGHT, DensityMode.COMFORTABLE, 1, Motion.SYSTEM));
var registration = appearance.register(scene);
stage.setScene(scene);
stage.show();

// An application preference change, on the FX thread:
appearance.setPreferences(
    new MeridianPreferences(Appearance.DARK, DensityMode.COMPACT, 1.25, Motion.REDUCED));

// When this scene is retired:
registration.close();
// At application shutdown (Application.stop()):
appearance.close();
```

The theme API and metric records are Java classes and do not add Scala, ScalaFX or
ScalaDock to the theme runtime. Scala applications can call the same API directly.

| Missing optional setting | Default |
|---|---|
| Appearance | SYSTEM: typed platform scheme; anything other than DARK resolves to light |
| Density | COMFORTABLE |
| Text scale | 1; at least 1, finite, and small enough to keep CSS dimensions representable |
| Motion | SYSTEM: typed platform reduced-motion preference |

`install()` selects all defaults. `MeridianPreferences.optional(null, null, null, null)`
fills missing values independently. Invalid supplied values are rejected.
The immutable record and read-only preference property are the application source
for appearance, density, text scale and motion. Do not write ThemeManager's theme,
modules or Meridian option independently while the binding owns them.

## Scene and disposal contract

`register(scene)` assumes the scene previously inherited platform scheme/motion.
JavaFX's [scene preference getters](https://openjfx.io/javadoc/27/javafx.graphics/javafx/scene/Scene.Preferences.html)
coalesce null to platform values, so they cannot reveal whether that same value was
an explicit override. Supply a declared restoration policy when the host has local
overrides:

```java
import javafx.application.ColorScheme;

var registration = appearance.register(scene,
    new MeridianAppearance.SceneRestore(ColorScheme.DARK, Boolean.TRUE));
```

Null restoration fields mean platform inheritance. These values describe what to
restore, not the active Meridian preference. Registration rejects foreign bound
scheme/motion properties and preserves them. Repeated registration with the same
scene/policy returns one handle; conflicting restoration policies are rejected.
Closing either the registration or application binding is idempotent.

AtlantaFX's ThemeManager applies the selected global theme and option to shown
windows, including new popup/dialog scenes. Explicit registration also treats
hidden scenes, observes root replacement and cleans the old root. A hidden scene's
stylesheet is updated immediately; JavaFX computes descendant styles and layout
through its normal CSS/layout lifecycle. Register application scenes before showing
them. For unregistered automatic scenes, disposal restores the previously observed
effective scheme/motion; explicit registration is required to preserve inheritance
intent. Automatically encountered scenes with foreign bound preferences retain
their binding and do not receive Meridian's local scene treatment.

Meridian adds one marked dynamic stylesheet per treated root for text scale and
motion. It preserves unrelated root and scene stylesheet entries and prior
scene user-agent resources. Registration closure removes that root observer and
treatment; the application-wide user-agent theme remains global until the whole
binding closes. It cannot opt one scene out of the global application theme.
Scene-local and application author styles still have normal CSS precedence.

Application closure removes Meridian's two platform preference listeners, root
listeners and option registration, cleans its tracked roots, restores prior
scene values/resources and the previous ThemeManager theme/modules, and permits
a later install. Weak references cover automatically discovered scenes/roots;
explicit registrations retain scenes until closed. It does not dispose the shared
ThemeManager or remove another owner's options. Scheme/motion property ownership
must remain with Meridian while registered.

## Geometry and interaction

Comfortable/compact controls begin at 32/28 logical px and table rows at 40/32 px,
with shared general14/table13/metadata12 text. Table headers begin at 32/28 px.
The native live-switch regression requires both computed and laid-out header size
to change; preferred and minimum header geometry are explicit to avoid retaining
the old density. They are preferred/minimum values, not maximum heights.
Measured application layouts and wrapped/custom cells need their later fixtures.

Use `appearance.metrics()` to read the current text-scaled logical minima.
Radii/padding stay deliberate density choices. Native hover transitions use
JavaFX27 CSS and the token duration; REDUCED sets duration to zero. SYSTEM observes
both typed platform scheme and motion properties. No layout animation or idle
decoration is introduced. Label status classes, `accent` buttons and standard
upstream semantic names are retained. Invalid fields support `:invalid` and the
AtlantaFX `:danger` convention; focused invalid controls keep the invalid boundary
and a separate visible focus ring. Disabled data remains readable in tables.

## Reproducible theme build

Ordinary sbt builds use checked-in compiled CSS and verify its input/output hashes
with Python; they do not require Node or Sass. Recompiling themes requires Node18+
and the development-only locked compiler:

```sh
npm ci --prefix theme-build --ignore-scripts --no-audit --no-fund
python3 tools/generate_tokens.py --check
python3 tools/build_themes.py
python3 tools/build_themes.py --check
python3 tools/build_themes.py --verify
python3 -m unittest discover -s tools/tests -v
tools/sbt scalafmtSbtCheck scalafmtCheckAll clean compile foundationReport theme/packageBin
tools/sbt 'sampler/runMain meridian.sampler.ThemeSmoke target/new-theme-smoke'
```

The smoke output directory must be new. It shows and closes native controls,
context menus and dialogs in all four variants and records computed-style and
lifecycle assertions. Some state specimens use forced pseudo-classes; this is
render/state evidence, not an OS keyboard, screen-reader or exhaustive interaction
qualification.

[Compiler source](../tools/build_themes.py) configures upstream SASS modules without
editing them; [recipes](../theme-build/_recipes.scss) contain the bounded Meridian
delta. Dart Sass1.93.2 and all npm dependencies are locked. Only three known
deprecation categories from the pinned upstream syntax are silenced during
compilation; compiler errors and all other warnings remain visible.
Four full user-agent resources and matching AtlantaFX manifests are generated
without absolute paths, timestamps or source maps. Optional module filtering
always retains the required Meridian recipe/token module.
The [generation receipt](../modules/theme/src/main/resources/meridian/theme/themes/generation.json)
hashes source inputs and all eight CSS/manifest outputs. CI recompiles and compares
them, tests staleness/determinism and retains A2's contrast checks; hosted CI has not
run here.

Generic upstream chart defaults are identical across all four variants and remain
unqualified for scientific use. Scientific LUTs, data-series policy, thresholds and
exports remain application-owned. ScalaDock adaptation and provider qualification
are still separate tasks. See [A3 acceptance evidence](evidence/a3/README.md).
