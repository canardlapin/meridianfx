# A1 foundation acceptance

Accepted locally on 2026-10-14 for Mote issue `bd-01M4Y1ABS0W5YJ245C1XEQ5CYW`.

Source-tree digest: `534c6e1003a639975bcc2ad83f52776782bf9dbe2e0e8c06720ff76eaba415b2`.
The 85 input hashes are in [source-manifest.json](source-manifest.json).
MeridianFX is not yet Git-backed; no Git commit or remote publication is claimed.

## Acceptance criteria

| A1 obligation | Evidence | Result |
|---|---|---|
| Four module boundaries with Java-friendly theme/workbench APIs | [build.sbt](../../../build.sbt), compiled FoundationTheme and ViewHeader, [module dependency reports](dependencies/theme/runtime-classpath.txt) | Pass; theme/workbench carry no ScalaDock, ScalaFX or Scala runtime |
| Exact build/runtime and upstream style pins | [availability receipt](artifact-availability.json), [upstream lock](../../../upstream/lock.json), original MIT notice and 65 locked upstream files | Pass: Scala 3.7.4, sbt 1.12.14, JDK25, JavaFX27, AtlantaFX3.0.0 |
| Coherent platform runtime and explicit provider resolution | [verification.json](verification.json), per-module dependency reports, cloned provider HEAD `628c46fce92c4d31b0b08459ac0f18777b5ba527` | Pass; all four runtime classpaths use JavaFX27; no sibling checkout selected |
| Clean build and bounded native smoke; commands and exits retained | [build-01.log](build-01.log), [final acceptance log](acceptance.log), their JSON sidecars, [native capture](native-final/foundation.png), [runtime properties](native-final/runtime.properties) | Pass; actual native Stage shown and closed, Java APIs exercised |
| Licenses and font/icon provenance; original references preserved | [NOTICE](../../../NOTICE), [LICENSE](../../../LICENSE), [upstream MIT license](../../../upstream/atlantafx/LICENSE), verifier reference checks | Pass; three supplied reference hashes unchanged, system fonts, no bundled font/icon asset |

## Executed checks

- `tools/sbt scalafmtAll clean compile foundationReport`: exit 0, clean module compilation and default full-SHA source resolution.
- `tools/sbt scalafmtSbtCheck scalafmtCheckAll compile foundationReport 'sampler/runMain meridian.sampler.FoundationSmoke docs/evidence/a1/native-final'`: exit 0 for final sources and named-module launcher. The final run recompiled the workbench/sampler Java APIs.
- `python3 tools/verify_foundation.py --classpath --capture docs/evidence/a1/native-final`: exit 0; 65 upstream hashes, three reference hashes, four coherent runtime classpaths and a nonempty native PNG.
- Explicit JDK22 through `MERIDIAN_JAVA_HOME`: expected exit 1 before sbt starts; [version guard receipt](jdk-version-guard.json). The wrapper does not silently substitute another JDK.

Formatting checks include Scala source and sbt build files. Java sources were compiled with
`--release 25 -Xlint:all`. Final logs contain no compiler, JavaFX configuration,
or CSS parser warning. Prism's verbose stderr is labelled `[error]` by sbt but
records successful native-library/shader initialization; the child actually exits 0.

## Native observation

- macOS 14.3, aarch64; Homebrew OpenJDK 25.0.1.
- JavaFX runtime 27+30, Metal `com.sun.prism.mtl.MTLPipeline`.
- Window output scale 2.0; scene snapshot 620×250 logical pixels.
- Native System Regular 14 px; AtlantaFX 3.0.0 PrimerLight baseline.
- Final PNG SHA-256: `516973b394d73ef47052fb765eea194e9886ec77cfc870eb021141f78d41a372`.
- The final capture is byte-identical to the inspected named-module capture from native-02.
- Window closes and the log records QuantumRenderer shutdown.

The probe verifies the build/runtime/control baseline. Authored Meridian themes and token
generation are A2/A3. Dock gestures, multi-window appearance, scientific invariants,
Linux/Windows, accessibility and exact-artifact consumers retain their later acceptance gates.

## Preserved earlier findings

The first smoke exited 0, but its relative output path was under the sampler module and
JavaFX reported an unnamed-module configuration warning. The first path-based verifier
therefore failed with a missing root-relative runtime.properties file. [native-01.log](native-01.log)
and [the preserved first capture](native-attempt-01/foundation.png) retain that evidence.

The launch now uses the project root as working directory and named JavaFX modules.
native-02 and the final run show the corrected output path, no configuration warning,
and successful Metal initialization. native-02 also records the harmless sbt notification
that its formatting step changed build whitespace; the final invocation loaded the
formatted build afresh.

## Next action

A2 (`bd-01M4Y1ABTGV06BWQJAX3ZKXFZX`): author semantic tokens and deterministic native,
browser and typed-metric generation with explicit contrast contracts. ScalaDock's S1
provider issue remains independently ready. Application pilot selection remains D0.
