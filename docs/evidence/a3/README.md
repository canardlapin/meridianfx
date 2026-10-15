# A3 native theme acceptance

Accepted locally on 2026-10-14 for Mote A3 bd-01M4Y1ABW0Q7QDR63Y7VCMTMB2.
E0 covers A1–A3; later provider, dock, control and application gates remain open.
Source-only and not Git-backed: no Git commit, remote CI, release or publication.

Source candidate: b0fa075adaf1d3704de887df93345f3ce59d7e641be157275abd2a46d7a6da35 (123 hashes in
[source-manifest.json](source-manifest.json)).
Packaged theme JAR SHA-256: 8975ca80efa03b2a3e7c70f5c76a6010dcb8e664e50ad4f9ae1235581d924f3d.
[Verification](verification.json) retains final command argv/exits and capture hashes;
[Java consumer receipt](java-caller.json) binds its source/JAR and exact argv.
A1/A2 manifests describe their historical candidates.

## Acceptance criteria

| A3 obligation | Evidence | Result |
|---|---|---|
| AtlantaFX-derived themes in four variants | [Compiler](../../../tools/build_themes.py), [recipes](../../../theme-build/_recipes.scss), [generation receipt](../../../modules/theme/src/main/resources/meridian/theme/themes/generation.json), unchanged locked upstream | Pass: four full UA sheets, four manifests, one receipt; no production Modena overlay |
| Pinned Theme/ThemeManager and one preference/lifecycle source | [Java API](../../../modules/theme/src/main/java/meridian/theme/MeridianAppearance.java), [guide](../../THEMING.md), native lifecycle rows | Pass: immutable appearance/density/scale/motion, typed defaults, explicit scene restoration, disposal/reinstallation |
| Initial native controls/states without CSS warnings | [Assertions](acceptance-final/assertions.tsv), [final clean log](acceptance-final.log), PNGs below | Pass: **245 assertions**, **12 captures**, default/hover/pressed/focus-visible/selected/inactive/disabled/invalid/read-only plus menu/dialog |
| Existing/new/hidden scenes without duplication | Registration, switching, root-replacement and disposal rows; independent hidden-scene consumer | Pass: one marked sheet/root, idempotent registration, live/hidden/new scene handling, cleanup and foreign-binding rejection |
| Small Java caller and isolated theme runtime | [Java source](JavaCaller.java), [consumer log](java-caller-final.log), [actual JAR](artifacts/meridian-theme-0.1.0-dev.1.jar), retained dependency reports | Pass: isolated javac/runtime using only theme JAR, AtlantaFX and named JavaFX; no Scala/ScalaFX/ScalaDock |

## Executed checks

Every final command exits 0; exact argv, timing and termination reasons are in
verification.json and each log's metadata sidecar.

- [Final clean build/native log](acceptance-final.log): scalafmtSbtCheck,
  scalafmtCheckAll, clean, compile, foundationReport, theme/packageBin and
  sampler/runMain ThemeSmoke. Four Meridian modules compile with Java release25
  and Xlint:all; exact provider pin is used without a sibling override.
- [Python suite](python-final-02.log): **28 tests**, including compilation in
  independent paths, manifest bounds, edited/missing/obsolete source/output
  rejection, upstream integrity and invariant chart defaults. The 22 A2 tests
  retain independent Decimal contrast, alpha/threshold and schema regressions.
- [Recompilation --check](themes-final.log): all nine stored theme outputs match.
  Normal sbt builds verify hashes without requiring Node/Sass.
- [Independent Java consumer](java-caller-final.log): no javac warnings; packaged
  API invocation, hidden-scene enlargement and absence of Scala/dock classes pass.
  Source, argv, JAR and the macOS evidence helper are retained; repetition needs a
  new helper output path.
- [Foundation integrity](foundation-integrity.log): 65 upstream and three original
  reference hashes preserved; four coherent JavaFX27 classpaths and Java module
  dependency isolation.
- [Token integrity](tokens-integrity.log): 12 current deterministic A2 resources,
  224 required contrast pairs and 12 explicit exemptions.

## Native observations

Homebrew OpenJDK25.0.1, JavaFX27+30, macOS14.3/aarch64, Metal, output scale2.0;
[runtime properties](acceptance-final/runtime.properties) bind the receipt.
Main scene snapshots are 1060x710 logical pixels.

| Appearance | Comfortable | Compact |
|---|---|---|
| Light | [Controls](acceptance-final/light-comfortable.png) | [Controls](acceptance-final/light-compact.png) |
| Dark | [Controls](acceptance-final/dark-comfortable.png) | [Controls](acceptance-final/dark-compact.png) |

Each variant also has menu/dialog PNGs, such as
[light menu](acceptance-final/light-compact-menu.png) and
[dark dialog](acceptance-final/dark-compact-dialog.png).
Two bounded visual batches inspected hierarchy, spacing, clipping, boundaries,
focus/selection and overlays. Header sizing and stable menu borders were corrected.
All 12 final PNGs are byte-identical to the inspected second-batch images after
the later hidden-scene scaling fix.

Assertions cover general14/table13/title25 text, 32/28 control/header and 40/32 row
geometry, actual palette paints, focused invalid controls, readable placeholders,
inactive selection, scheme/motion, double-size typography/targets, actual CSS
transition creation and reduced-motion suppression. Lifecycle checks cover
repeated switching, host CSS retention, root replacement, prior theme/modules,
explicit/inherited restoration, invalid/off-thread calls and reinstallation.

Some specimens force public pseudo-classes; selection, focus transfer, preferences,
windows and lifecycle calls are real JavaFX. This is not OS keyboard, screen-reader,
exhaustive control, scientific, performance or cross-platform qualification.

The shown-window final log has no compiler or CSS/parser warning. Successful
Prism stderr appears with sbt's error prefix. The zero-window consumer emitted
macOS CVDisplayLink error -6661; its API checks and exit pass. This diagnostic
is retained and does not establish vsync or renderer performance.

## Preserved earlier findings

Earlier logs/captures remain:

- SASS namespace collision and missing generated-metric load path were corrected.
- A nonexistent TableView font getter was replaced with actual TableCell measurement.
- Popup diagnostics identify transparent root/CSSBridge and painted menu content;
  assertions select painted content through public styleable nodes.
- Live density changes could retain an old preferred header height despite the
  new CSS size. Explicit preferred/minimum geometry and native regression checks
  correct it.
- An enlargement assertion initially measured preferred height; it now measures
  effective minima and laid-out targets.
- Null-coalesced scene getters require declared restoration intent. Registration
  now preserves platform inheritance/explicit overrides and foreign bindings.
- The isolated Java caller exposed a real hidden-scene regression: font28 with
  min-height28 (java-caller-02.log). Literal scaled typed geometry/typography in
  the preference sheet fix it; the same caller now passes with minimum56.

## Remaining gates

These are the initial authored controls; other upstream components retain the
later exhaustive state/context gate. Generic chart defaults are identical across
variants and do not qualify scientific colors. ScalaDock/application source is
unchanged. Next is ScalaDock S1; B0 needs its reviewed provider candidate before
adapter B1. D0 remains independently ready for pilot/runtime selection.
