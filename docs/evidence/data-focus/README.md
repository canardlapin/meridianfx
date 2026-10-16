# Data-view focus-visible border — issue5

Local acceptance for [MeridianFX issue5](https://github.com/canardlapin/meridianfx/issues/5).
Mote: `bd-01M5151J7F8843FY5H8X4E21FH`.
Base `cf5bbff69203556ee8b31ea566502d502e766386`; local source digest `7dadefce0ae0b9d4b4fc6510a1486ad5101429e03597257c243dff718d71dd30`.
At acceptance this was uncommitted local source. Subsequent delivery is recorded in Git history; upstream issue closure remains separate.

## Result

The existing TableView rule now also targets ListView and TreeView. In all four
themes, focused+focus-visible renders the semantic focus-ring color at2px on all
four sides. Ordinary focused, unfocused and cleared-focus-visible states retain
the decorative1px border. No selection, token, scientific palette or dependency
change is included; related issue4 remains separate.

[Passing native observations](native-02/observed.txt) cover4variants ×3controls
×4states =48cases:12ring states and36ordinary/unfocused/cleared states.
The probe checks the real Scene focus owner, all four border colors and widths,
and actual focus-visible CSS state. It retains native window/node focus flags.

The focused/focus-visible pseudo-classes are explicitly controlled. This isolates
the CSS rendering contract from macOS activation, which is not guaranteed for an
unattended native window. These results are **not OS keyboard/pointer input tests**
or screen-reader/WCAG qualification. Synthetic fixtures only.

Representative inspected scenes: [light compact list](native-02/variant-1-ListView.png)
and [dark compact tree](native-02/variant-3-TreeView.png). The target view has a clear
2px ring while the other views retain their decorative borders. These are owned
JavaFX scene captures, not OS screenshots.

## Reproduction and verification

The [baseline](baseline-02/observed.txt) measured the unmodified theme: list/tree
keep1px even in focus-visible state, while table gets2px.
[Baseline probe source](BaselineProbe.java) preserves that harness. Use the baseline
mode with base resources to reproduce; it intentionally expects the old defect.

```sh
tools/sbt 'sampler/runMain meridian.sampler.DataFocusProbe /absolute/new/output'
python3 -m unittest discover -s tools/tests -v
python3 tools/build_themes.py --check
python3 tools/build_themes.py --verify
```

Final [native log](native-02.log):48states/12captures pass; all owned windows close.
Existing ThemeSmoke also passes249assertions/12captures. Formatting, compile and
theme packaging passed. All30Python tests pass; independent Sass recompile/byte
comparison, token generator check, foundation classpaths and whitespace pass.

The first baseline attempt incorrectly assumed an active OS window and failed
the native isFocused assertion. The next baseline measured all48states with real
scene ownership and explicit CSS states. The first corrected-theme run then
exposed a show-time default-focus race; the final fixture resolves CSS/layout and
requests the intended owner at observation time, retaining the ownership check.
Both adverse logs remain; neither failure was a theme verdict or hidden.

[Verification receipt](verification.json) retains exact commands/exits, observations
and output hashes. [Source manifest](source-manifest.json) and
[source bundle](candidate-source.zip) bind the candidate to the base.
The packaged JAR in `artifacts/` was checked byte-for-byte against all four compiled
CSS resources. [Native scope and contract](../../THEMING.md#geometry-and-interaction)
is documented publicly.

Only macOSarm64/JDK25.0.1/JavaFX27+30/Metal ran here. Linux/Windows, OS input,
assistive technology, related selection contrast and full component acceptance
remain separate gates. Source and provider pins are unchanged apart from the
declared Meridian focus recipe/resources/fixture/documentation.
