# Consumer status and typography fixes

Local acceptance for [issue1](https://github.com/canardlapin/meridianfx/issues/1),
[issue2](https://github.com/canardlapin/meridianfx/issues/2) and
[issue3](https://github.com/canardlapin/meridianfx/issues/3).
Mote: `bd-01M50A36JQ1CSEP2NFBBSV8TT4`.
Base: `37a48d55891409a861aa517a508459ff9f07fb50`; source digest `71e9f05e353f8cc9e937cd625ce2177e81ddf0ed045c2e5be3bd0cf8a36d5b8d`.
At acceptance the changes were local and uncommitted. Subsequent delivery is recorded in Git history; upstream issue closure is separate.

## Reproduced before changing themes

[Baseline observations](baseline/observed.txt) reproduce 11.8999996px standalone
headers versus12px registered headers in all four variants. Numeric weights
500 and600 resolve to System Regular; normal/lighter resolve to Regular and
bold/bolder to Bold from the default upright regular parent.
[Baseline probe source](BaselineProbe.java), [command/log](baseline.log) and its
metadata retain the experiment before theme changes.

Inspection confirmed no neutral/progress roles and only ViewHeader in workbench.
The reporter's exact inactive-selection collision is their consumer evidence;
this run did not reconstruct the ephys4s application.

## Accepted changes

| Report | Result |
|---|---|
| Missing status vocabulary/component | Dedicated neutral/progress roles in both palettes; StatusIndicator supplies five semantic classes, visible label plus dot, bound accessible text and caller-owned domain meaning. New statuses cannot alias selection/focus colors. |
| Native weight mismatch | Theming guide maps historical500→400 and600→700. Generated browser aliases expose400/700; hash-locked original HTML/PNGs remain unchanged. Every emitted weight keyword has a checked macOS upright face profile. |
| Standalone sizing/dead rule | Compiled metadata header size and density height use literal metrics. Both installation paths produce12px Regular (18px at registered150% text scale). The superseded bold header rule is removed. |

[Public component contract](../../THEMING.md#status-indicators) and
[native typography contract](../../THEMING.md#native-typography-and-direct-installation)
document installation, translation and limitations. No new runtime dependency,
bundled font, scientific palette or provider pin is introduced.

## Verification

- 30 Python tests pass, including independent decimal contrast and status/selection separation.
- All284 required contrast checks pass. New-status text covers seven contexts; dot checks explicitly cover canvas/shell/raised. Minimum new-status text contrast is4.621:1.
- Native contract:12 variants,192 status assertions,12 scene snapshots. Standalone and registered light/dark × comfortable/compact plus registered150% text scale; computed paints, dot dimensions, typography/faces, class changes, null-status rejection, bound accessible text and wrapping pass.
- Existing native ThemeSmoke:4 variants,12 scene snapshots,249 assertions; lifecycle/menu/dialog checks pass.
- Formatting, compile and theme/workbench packaging pass. Generator checks, independent materializations, independent Sass recompilation/byte comparison, receipt verification and foundation classpath checks pass.
- Impeccable detector reports no findings; native assertions/captures remain the substantive evidence.
- Representative captures visually inspected: [light compact standalone](native-01/step-1.png) and [dark comfortable enlarged](native-01/step-10.png). Both show readable labels and dots without clipping. These are JavaFX scenes, not OS decorations.

[Verification receipt](verification.json) records commands, exits, artifacts and output
hashes. [Source manifest](source-manifest.json) and [source bundle](candidate-source.zip)
bind the candidate to its base; `artifacts/` retains packaged theme/workbench JARs.
Raw logs include normal Prism diagnostics on stderr; no failed checks are hidden.

Reproduce with README commands and
`tools/sbt 'sampler/runMain meridian.sampler.ConsumerContractProbe /absolute/new/output'`.
The historical baseline probe requires the base theme resources.

## Limits

This is a partial C1/C4 slice, not completion of C4 or v0.1. Only macOS arm64,
JDK25.0.1, JavaFX27+30/Metal ran here. Linux/Windows, assistive technology and the
consuming ephys4s application's mapping remain separate checks. Relative weights
are checked from the default regular upright parent; host font/parent overrides
need their own validation. Progress is static; updates are not automatically announced.

Acceptance did not include commit, push, hosted CI or upstream issue closure. Subsequent delivery is recorded in Git history. Meridian's ScalaDock
pin remains `628c46fce92c4d31b0b08459ac0f18777b5ba527`; S6/provider acceptance is unchanged.
