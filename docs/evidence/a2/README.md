# A2 token acceptance

Accepted locally on 2026-10-14 for Mote issue `bd-01M4Y1ABTGV06BWQJAX3ZKXFZX`.
The package remains source-only and is not Git-backed; no commit, hosted CI,
release, push or publication is claimed.

Source candidate digest: `8b327c9a9f2c2cec3d3fe5756e762141576de659468458d935a13574d70ae65d` (104 file hashes in
[source-manifest.json](source-manifest.json)).
Authored token source SHA-256:
`3c33e875f6c093d6c8444b2d1d58741ff620cc58ea9ce3e542d835d77fa2a410`.
The candidate includes build/source/generated resources, dependency lock, upstream
styles and original design references; it excludes caches, Mote operations,
acceptance logs/readout and build outputs. The original A1 manifest is historical.

## Acceptance criteria

| Obligation | Evidence | Result |
|---|---|---|
| Raw, semantic, state and metric contracts | [Authoritative source](../../../tokens/meridian.json), [schema and usage guide](../../../tokens/README.md), executable validator | Pass: 42 raw colors, 31 roles per mode, 118 named recipes |
| Deterministic native/dock/browser/Java generation and CI stale checks | [Generated manifest](../../../tokens/generated/manifest.json), [verification](verification.json), [workflow](../../../.github/workflows/tokens.yml) | Pass locally: 12 outputs; byte-identical materialized passes; edited/missing/stale/obsolete outputs rejected; hosted workflow unrun |
| Declared text and essential contrast, including compositing and scoped exceptions | [Full contrast report](../../../tokens/generated/contrast.json), [regression log](python-final.log) | Pass: 224 required pairs, 12 explicit exceptions; unrounded thresholds 4.5/3; independent Decimal references and alpha-layer tests |
| Typography/density floors and growth | [Typed Java metrics](../../../modules/theme/src/main/java/meridian/theme/metrics/MeridianMetrics.java), [native final log](native-final.log) | Pass: general14/table13/metadata12; comfortable32/40 and compact28/32 control/row minima; measured content and text enlargement grow targets |
| Scientific colors excluded | Explicit source exclusions, forbidden chart/LUT mappings and unused-raw rejection tests | Pass; reference curves remain unqualified illustrations |

## Executed checks

All retained command metadata report actual exit 0; raw logs are linked below.
The log sidecars record exact argv, duration, exit and termination reason.

- `python3 -m unittest discover -s tools/tests -v`: [final log](python-final.log),
  [metadata](python-final.log.meta.json); **22 tests passed**. Tests cover independent
  60-digit Decimal contrast references, black/white21, threshold non-rounding,
  single/two-layer alpha, missing opaque backdrop, schema coverage, illegal
  exemptions, duplicate keys, finite metrics/policy, fractional metric precision,
  typography/density floors, failed palette generation and stale-output detection.
- `tools/sbt scalafmtSbtCheck scalafmtCheckAll compile foundationReport
  'sampler/runMain meridian.sampler.TokenProbe tokens/generated/native-expected.tsv docs/evidence/a2/native-final.tsv'`:
  [final log](native-final.log), [metadata](native-final.log.meta.json).
  Final generated Java compiled; Scala/sbt formatting checks passed.
- `python3 tools/check_tokens.py --native-report docs/evidence/a2/native-final.tsv
  --report docs/evidence/a2/verification.json`: [acceptance log](acceptance.log),
  [metadata](acceptance.log.meta.json); determinism, coverage, contrast and all
  **130 actual JavaFX RGBA observations** match within `1e-7`.
- `python3 tools/verify_foundation.py --classpath`:
  [integrity log](foundation-integrity.log), [metadata](foundation-integrity.log.meta.json);
  all 65 upstream hashes, three reference hashes and four isolated JavaFX27
  runtime classpaths pass. Observed dependency reports are retained in
  [dependencies](dependencies/theme/runtime-classpath.txt).
- `python3 tools/generate_tokens.py --check` also passed on final generated files;
  the checker above compares the same expected bytes.

The first native and Python checks passed before two final guard improvements:
fractional metrics now retain double precision, and contrast policies reject
non-finite/boolean values. Earlier logs are preserved; final checks cover the
updated generator and generated Java. No failed implementation check was hidden.

## Native observation and bounds

The final probe uses Homebrew OpenJDK25.0.1, JavaFX27, macOS/aarch64 and the Metal
pipeline. It creates real Scenes and applies generated CSS on the JavaFX thread,
reads semantic/AtlantaFX/dock colors from actual background fills in both modes,
and exercises metric growth/reduced-motion/invalid-input guards.
It opens **zero windows** and exits with QuantumRenderer shutdown.
[Observed channels](native-final.tsv) are color-resolution evidence, not pixels or
a native rendering benchmark. The log has no compiler or CSS warning.
Verbose native-library/vsync stderr appears with sbt's `[error]` prefix but
describes successful initialization; the child and parent exited 0.

Smallest required ordinary-text ratio: **4.585583691486803**
(light warning text on pressed surface).
Smallest essential ratio: **3.0345002972023702**
(light secondary-button border on pressed surface).
Keep these unrounded values: changing a surface or boundary requires rerunning
the report. Decorative/disabled exceptions are recorded for each mode, rather
than silently removed from coverage. Active/inactive meaningful data stays tested.

A2 delivers palette layers, recipes and metric resources. It does not establish
complete AtlantaFX theme behavior, rendered state coverage, dock interactions,
detached-window propagation, screen-reader accessibility, cross-platform
qualification, scientific color validity or artifact publication.
Original reference files are unchanged; generated browser tokens are separate.
No ScalaDock provider source or application source was changed.

## Next action

A3 (`bd-01M4Y1ABW0Q7QDR63Y7VCMTMB2`) compiles the authored AtlantaFX-derived
themes and implements appearance/density binding. Provider S1 remains independently
owned in ScalaDock. B0 still requires its reviewed provider candidate.
