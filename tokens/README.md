# Meridian UI tokens

[meridian.json](meridian.json) is the authoritative A2 source for raw sRGB colors,
semantic roles, primitive component-state contrast recipes, upstream aliases,
typography, density, geometry and motion. Edit that file, then regenerate.
Resources carry its SHA-256; timestamps and machine-specific paths are excluded.

```sh
python3 tools/generate_tokens.py
python3 tools/generate_tokens.py --check
python3 tools/check_tokens.py
python3 -m unittest discover -s tools/tests -p test_tokens.py -v
```

Python 3.10 or later suffices; only the standard library is used.
`--check` never writes. It fails on missing, edited or stale expected outputs and
unexpected files in `tokens/generated`. The checker materializes two independent
passes in a temporary directory, compares every byte and injects edited/missing
files. Regression tests also prove stale-source and obsolete-file rejection.

## Schema and state contracts

The version is `meridian.tokens.v1`. The executable schema is
[validate()](../tools/generate_tokens.py): exact top-level fields, semantic roles,
metric fields and 15 public ScalaDock aliases; raw references and hex validity;
required recipes with fixed text/essential classifications; finite metrics,
typography floors and scoped exemptions. Duplicate JSON keys, non-finite JSON
and unreferenced raw colors are rejected. Additional recipes can extend the
inventory; new roles/metric fields require an explicit schema/generator change.

| Layer | Meaning |
|---|---|
| `raw` | Authored `#RRGGBB` or `#RRGGBBAA` sRGB |
| `roles.dark`, `roles.light` | Identical inventories of semantic references into raw colors |
| `recipes` | Named primitive/state, foreground/background roles, optional top-to-bottom backdrop layers, contrast class and exemption rationale |
| `mappings` | AtlantaFX, ScalaDock and browser names bound to semantic roles |
| `typography`, `geometry`, `densities`, `motion` | Logical pixels and integer milliseconds |
| `contrast_policy`, `exclusions`, `provenance` | Thresholds, scoped exceptions, scientific boundary and reference identity |

Each appearance includes dedicated neutral/progress status roles; contrast recipes cover text, placeholders,
boundaries, focus, action states, selection, inactive selection, invalid/read-only
fields, menus, dock tabs, indicators, statuses and composited drop markers.
Only disabled button/field/menu foregrounds use the unavailable-control exception.
Meaningful data in inactive or disabled-selected rows remains tested.
Separators, shadows and supplementary drop tint are decorative; essential drop
and selection markers remain tested.

Neutral/progress labels and dots are independently checked on canvas, shell and raised surfaces; their colors must not alias selection/focus. See [status and native weight contracts](../docs/THEMING.md#status-indicators). Generated browser weights are normal400/strong700; the locked browser study retains historical500/600 values with an explicit native translation in that guide.\n\nOriginal references are hash-locked and unchanged. New pressed neutral surfaces
are dark `#2B2F39` and light `#E4E8EF`, calibrated to preserve essential-boundary
contrast. Existing text, control borders, action and focus colors are retained.
Decorative borders must not serve as the sole essential input boundary.

## Generated outputs

| Output | Purpose |
|---|---|
| `modules/theme/src/main/resources/meridian/theme/tokens-{dark,light}.css` | Root looked-up colors and 19 initial AtlantaFX aliases |
| `modules/scaladock/src/main/resources/meridian/scaladock/tokens-{dark,light}.css` | Self-contained dock palette using all 15 public names |
| `modules/theme/src/main/java/meridian/theme/metrics/MeridianMetrics.java` | Java-callable immutable metric records |
| `modules/theme/src/main/resources/meridian/theme/tokens.json` | Resolved semantic palette, recipes and metrics |
| `tokens/generated/_palette-{dark,light}.scss` | Semantic/AtlantaFX maps for the A3 theme build |
| `design-study/generated/tokens.css` | Browser aliases and metrics; original HTML is unchanged |
| `tokens/generated/contrast.json` | Layer stacks, effective RGBA, ratios, thresholds and exemptions |
| `tokens/generated/native-expected.tsv` | Native semantic/alias expectations |
| `tokens/generated/manifest.json` | Source digest and hashes of the other generated outputs |

These are token layers; complete user-agent themes are A3. JavaFX numeric values
use `MeridianMetrics` and generated literal component CSS, not numeric looked-up
colors. Generated Java must not be hand-edited.

ScalaDock's accent uses the focus/marker color, rather than the filled-action
accent, for essential-marker contrast. Its selected bar uses the neutral inactive
marker; outlines/shadows are decorative. B0/B1 and provider tasks must validate
actual component anatomy and interactions. These initial mappings do not establish
dock gesture, detachment, persistence or accessibility acceptance.

## Metrics

General text is 14 px, table text 13 px and metadata 12 px. Density changes spacing,
not font size. Comfortable/compact control minima are 32/28 px, table minima 40/32 px
and headers 32/28 px. `controlHeightFor`, `tableRowHeightFor` and `headerHeightFor`
allow measured content to increase these sizes; callers include their layout
insets in that measurement. `scaledForText` accepts finite factors at least 1,
expands typography/targets and keeps padding as a density choice. A visible 16 px
icon has an independent 32/28 px target; a 1 px divider has 12 px resize hit reach.
Radii are 6/8/12 px. Hover/overlay/panel durations are 110/150/180 ms;
`MOTION.reduced()` and the browser preference rule set them to zero. Native
preference observation/propagation belongs to A3.

## Contrast and scientific boundaries

The design thresholds follow W3C's [ordinary-text criterion](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html)
(4.5:1) and [essential non-text criterion](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html)
(3:1). Unrounded ratios are compared; large-label relaxation is not used.
Alpha is source-over composited on the explicit opaque backdrop before sRGB
relative luminance. Drop fill/scrim/slot recipes declare their layer order.
Excluded pairs retain their computed ratios and rationales.

This proves declared solid-color primitive recipes, not font rasterization,
final composed theme states, screen-reader support or full native accessibility.
A3 and later fixtures must test rendered contexts; each new context/background
needs a pair before making a contrast claim. The smallest passing ratios are close
to their thresholds, so surfaces cannot change independently of the report.

UI tokens do not generate scientific LUTs, chart/data-series palettes,
thresholding, normalization or export colors. Reference curves are illustrative,
without scientific colormap qualification.

[Acceptance evidence](../docs/evidence/a2/README.md) retains independent Decimal
references, rejection cases and actual JavaFX RGBA observations.
[Token CI](../.github/workflows/tokens.yml) runs the generator check, deterministic
materialization and regressions. Its checkout pin is the verified
[v5.0.0 commit](https://github.com/actions/checkout/commit/08c6903cd8c0fde910a37f88322edcfb5dd907a8).
CI is configured locally; no hosted execution/publication is claimed.
