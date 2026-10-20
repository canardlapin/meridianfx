# B1 — Meridian–ScalaDock adapter acceptance

The local B1 gate passed on 2026-10-19 (America/Toronto). The final command ended
at 2026-10-20T00:55:38Z. B2 can now build the native docked workspace; B3 still
owns independent workspace acceptance.

## Candidate and implementation

- Provider: `4a7a61bb397a68d5048f809a6ea1c30ce8b45223`, accepted through
  [B0](../provider-acceptance/README.md). [Resolution](provider-resolution.tsv)
  remains the full-SHA remote checkout, with no sibling override or tracked diff.
- Consumer base at verification: `1ac736b28b5f955fcf042daefde18db9eda6e776`.
  Changes were uncommitted then. The 34-file source map in [verification.json](verification.json)
  identifies the tested working tree with SHA-256
  `55c73b2ec153a4b47715753fb96be970e769cae182fa2ee9159c223cd35d02c3`.
- `MeridianDock.bind` uses public provider theme, metrics and motion APIs. It
  rejects duplicate owners, retains unrelated host geometry settings, and owns
  one listener. It has no window registry and does not change pane/domain state.
- `MeridianAppearance.presentationProperty()` publishes an immutable resolved
  presentation after scene and ThemeManager updates finish. Listening to raw
  preferences before those updates had produced popup CSS lookup warnings;
  the completed-presentation notification removes that ordering problem.
- Generated mappings cover every documented `-dock-*` token, including the
  legacy alias. Recipes load after provider CSS, distinguish neutral selection,
  keyboard focus and essential boundaries, and use semantic values on `.dock`
  even in independent popup roots. Font sizes are generated literal pixels.
- Focus rings use inset background layers with zero border width/insets. The
  native regression requires focus to leave effective header geometry unchanged.

See [usage and ownership](../../SCALADOCK.md). The application owns the appearance
service and explicit scene registrations; the adapter releases its listener on
close and leaves the last presentation installed.

## Passing evidence

The [final native result](native-ordered/result.json) records JDK 25.0.1,
JavaFX 27+30, macOS/aarch64, all three named JavaFX modules at version 27,
runtime JAR digests, and executing provider class hashes. The verifier matches
those provider bytes to the resolved checkout's compiled classes.

| Phase | Header minimum / effective | Dock label size | Floating state |
| --- | --- | --- | --- |
| Dark comfortable | 32 / 33 | 14 | main only |
| Light compact | 28 / 29 | 14 | main only |
| Dark compact | 28 / 29 | 14 | new float |
| Light comfortable, scale 1.25 | 40 / 41 | 17.5 | saved/restored float |

Effective metrics include the provider's natural chrome minima. The probe checks
actual header bounds and computed font sizes rather than assuming an exact floor.

The final gate checks:

- Computed main/float surfaces, focused menu text/fill, essential popup and
  ghost boundaries, and independent ghost text. Four real tab drags produce
  the ghost scenes. Fourteen owned-scene captures are retained.
- All 15 documented variables in both palettes, including independent detached
  host-root lookups. [Token coverage](token-coverage.json) links each variable
  to its generated semantic role and value; missing/new tokens and stale colors
  have adverse Python tests.
- Live light/dark, density, text-scale and Full/Reduced/System motion. Motion-only
  changes preserve an active drag; appearance changes dismiss an open menu.
- Exactly one provider and one adapter sheet per intended root, with no stale
  sheets after 100 preference changes, including existing floating roots.
- 20 close/rebind and float-close cycles. The binding has one subscription while
  live and zero after close; scene registrations return to zero. All **22 closed
  floating windows** are collected during the bounded weak-reference check.
- Four pane instances, unchanged edited values and synthetic scientific swatches,
  unchanged persisted layout during appearance updates, preserved save/load data,
  and exactly four terminal disposals.

Formatting and **39 Python tests** pass. Generated token/theme checks and locked
foundation inputs pass. The [native log](ordered-native.log) and
[contract log](contracts.log) have exit-0 sidecars retaining exact arguments and
timestamps. The final run has no CSS parsing, lookup or conversion warnings.

```sh
tools/sbt 'scaladock/scalafmtCheck' 'scaladock/Test/scalafmtCheck' \
  'scaladock/providerResolutionReport' \
  'scaladock/Test/runMain meridian.scaladock.AdapterProbe /tmp/new-b1-capture'
python3 tools/generate_tokens.py --check
python3 tools/build_themes.py --verify
python3 tools/verify_foundation.py
python3 -m unittest discover -s tools/tests -v
python3 tools/verify_adapter.py --capture docs/evidence/adapter/native-ordered \
  --log docs/evidence/adapter/ordered-native.log
python3 tools/verify_adapter.py --check docs/evidence/adapter/verification.json
```

Use a new capture directory for a native rerun. Before issuing a new receipt,
copy the fresh `modules/scaladock/target/provider-resolution.tsv` into this
evidence directory and retain the native log plus its command sidecar. The
`--check` command verifies retained source/evidence hashes; it is not a native run.

## Retained adverse evidence and limits

All earlier logs remain. Runs 01/02 failed on unsupported font-size lookups;
03/04 exposed a fixture that inspected an unpainted popup wrapper instead of
its painted surfaces. Runs 05–07 encountered prematurely hidden menus. Run 08
passed, but that timing-dependent result was not used to waive further checks.
Runs 09/10 exposed focus-dependent geometry; run 10 recorded **33→36 pixels**
and residual inherited border insets. Clearing those insets and painting the
ring without layout changes passed run 11.

`final-native.log` reached the native completion marker but produced CSS lookup
and conversion warnings while changing appearance with a menu open. The receipt
verifier rejected it. The subsequent completed-presentation notification produced
the accepted `ordered-native.log`, with the stricter menu/stale-sheet checks kept.

The final macOS run emitted `CVDisplayLink* error: -6661` diagnostics and Scala
3.7.4's deprecated `sun.misc.Unsafe` warning. Scene rendering, native pulses,
assertions and cleanup completed; display-link/frame-pacing behavior is not
qualified by this receipt. Input is JavaFX event dispatch and focus-paint fixtures
use explicit CSS states; no OS-input, full VoiceOver or new hosted-CI result is
claimed. The provider's previously accepted macOS AXEnabled limitation remains.

Disk space declined from 6.2 to 2.5 GiB during the task. Only small incremental
adapter/dependency compiles and focused native runs were performed, with no full
aggregate build. B2/B3, the application pilot and D6 packaged/clean-consumer gates
remain open. This record describes the B1 verification state before its subsequent
commit and push; it does not claim artifact publication or completion of those gates.
