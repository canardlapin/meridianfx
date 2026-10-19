# ListView and TreeView selection contrast — issue 4

Local repair for [issue 4](https://github.com/canardlapin/meridianfx/issues/4),
tracked independently as `bd-01M5B470S9F5MSCX8V9GNYT4KN` (related to C2).
Base: `9a676597b891d6d9f6b3105d3f0e67b50fb0e46f`. At verification the candidate was
uncommitted; [verification.json](verification.json) binds its source and resources
to SHA-256 hashes. Subsequent delivery is recorded in Git history. This does not
accept C2, B3 or any release gate.

## Repair and measured result

Selected rows retain the inset rounded fill and gain a 2 px outline. The outline
uses the existing active/inactive selection-marker roles; colors, typography,
row geometry and the separate outer focus-visible ring are unchanged. Hover
preserves all three fill layers and their insets. Clearing selection removes
the cue. The matching inner and outer radii avoid the former square marker notch.

The native probe measures actual rendered pixels inside the outline, computed
selection fills and unselected cell backgrounds. The following results hold for
both controls, both densities, with and without hover:

| Appearance | View focus | Cue | Against cell | Against selection fill |
| --- | --- | --- | --- | --- |
| Light | Inactive | `#7B8595` | 3.729:1 | 3.066:1 |
| Light | Active | `#584DC2` | 6.451:1 | 5.518:1 |
| Dark | Inactive | `#6D798D` | 3.991:1 | 3.037:1 |
| Dark | Active | `#BCB7FF` | 9.501:1 | 7.701:1 |

The minimum is **3.037:1**, checked without rounding against 3:1.
The pale fill remains 1.17–1.31:1; the outline now carries selection information.
Eight mandatory recipes add sixteen light/dark `essential` pairs to
`tokens/generated/contrast.json`, covering each control/state against both
adjacent surfaces. Missing recipes and substitution of the pale fill fail tests.

## Reproduction and verification

The same final probe produces:

- [Reported revision](reported-revision/native/observed.tsv): all 32 states fail,
  exit 1, using untouched theme resources from
  `cf5bbff69203556ee8b31ea566502d502e766386`. The reported four fill ratios reproduce.
- [Repaired resources](fixed/native/observed.tsv): all 32 states pass, exit 0.
  Each state also checks selection clearing and restoration, real scene focus
  ownership, and visible fill/background pixels.
- [Existing focus regression](focus-regression/native/observed.txt): all 48 cases
  pass, including TableView and removal of focus-visible state, exit 0.
- [Python tests](python.log): all 32 tests pass. Independent decimal contrast
  references, omitted recipe rejection, pale-fill substitution rejection,
  deterministic generation, stale resources and scientific palette isolation
  are included.
- Token generation/checks, independent Sass recompile, resource hash verification
  and `git diff --check` pass. All 300 required token pairs pass.

Representative inspected native captures:
[before, light compact](baseline-native/native/light-COMPACT-ListView-2.png),
[after, light compact](fixed/native/light-COMPACT-ListView-2.png),
[after, dark compact](fixed/native/dark-COMPACT-TreeView-0.png).
All probe-owned windows close on completion.

With the cached dependency report from `foundationReport`:

```sh
python3 tools/verify_selection.py /absolute/new/selection-evidence
python3 tools/verify_selection.py /absolute/new/focus-evidence --probe DataFocusProbe
python3 -m unittest discover -s tools/tests -v
python3 tools/generate_tokens.py --check
python3 tools/check_tokens.py
python3 tools/build_themes.py --check
python3 tools/build_themes.py --verify
```

The focused runner compiles the theme and selected Java fixture with `javac
--release 25`; it neither resolves nor compiles ScalaDock. It accepts an explicit
`--java-home`, `--classpath-report` and alternate `--resources` root. The receipts
retain exact command arguments, exits, dependency hashes and resource hashes.
To reproduce the reported revision, export its
`modules/theme/src/main/resources/` tree with Git into a separate directory,
then supply that directory with `--resources`; expect exit 1 and 32 violations.
The ordinary sbt route is `tools/sbt 'sampler/runMain
meridian.sampler.SelectionContrastProbe /absolute/new/output'`.

## Limits and retained adverse evidence

This is macOS arm64, JDK 25.0.1, JavaFX 27+30, Metal scene-rendering evidence.
Focused and hover CSS states are controlled explicitly; scene focus ownership
is checked separately. No OS keyboard/pointer, screen-reader, cross-platform or
general WCAG conformance claim follows from this bounded contrast regression.

The first [sandbox attempt](baseline/1.log) could not initialize the native
display/cache and timed out after 90 seconds (parent exit 1); it is not a theme
verdict. [The first native baseline](baseline-native/receipt.json) completed all
32 cases and failed as expected. Its sample was at the anti-aliased outer edge;
the final probe samples one pixel inward, and was rerun unchanged on both the
reported revision and repaired resources. All adverse logs are retained.

Free disk was 19 GiB, below the workspace threshold for a full sbt build. Only
the focused Java compile, native probes and Python/Sass checks above ran. No
full build, hosted CI, packaged-consumer qualification, push, publication or
GitHub issue closure is established by these verification runs.
