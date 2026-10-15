# MeridianFX delivery plan and acceptance criteria

Reviewed and expanded 2026-10-14. Registered in local Mote as an executable acceptance backlog. Planning and registration do not establish native compatibility or release qualification.

## Decision

Proceed with MeridianFX as an AtlantaFX-derived design system. Preserve the study's quiet surfaces, violet action accent, typography-led hierarchy, and distinct selection/focus treatments. Make ScalaDock integration a requirement of the first native milestone.

Add a small **meridian-scaladock** adapter to the proposed module structure. ScalaDock continues to own layout, pane identity, docking gestures, persistence, perspectives, and window lifecycle. Meridian owns appearance and reusable application compositions. Neither project should acquire the other's responsibilities.

The first acceptance specimen is a working docked scientific workspace, not a standalone button gallery.

## Evidence and limits

- MeridianFX was an empty directory at review start, without a build or Git repository.
- Reviewed the two supplied PNGs visually and the HTML source, found in Downloads. Copies are preserved in [design-study](design-study/index.html). No live browser interaction tests were run.
- Reviewed ScalaDock at `628c46fce92c4d31b0b08459ac0f18777b5ba527`, on a clean local `main`. Read its styling contract, theme/window/drag/layout implementations, relevant tests, build, and CI configuration.
- Inspected ScalaDock's committed light and floating-window images as context. They are reference captures, not new captures of the reviewed HEAD.
- The supplied ZIP, JavaFX starter, palette generator, contrast scripts, and test reports were not among the three located assets. The proposal's twelve browser checks and seventy-two color-pair checks remain reported prior results, not independently reproduced evidence.
- No ScalaDock implementation changes, builds, hosted checks, publication, or runtime migrations were performed during this planning review.

### Preserved reference files

| File | SHA-256 |
|---|---|
| `design-study/index.html` | `430a193182a3de720b8b48abb474f09d4f015279aefb34dff80220d4a5abdd7b` |
| `design-study/meridian-light.png` | `663a9c8f45cc81c28777e7069f09adbf550c8c103abbcd564a5ef0a947ed17a1` |
| `design-study/meridian-components.png` | `2c9f73432d5b67b3e1a6c764cc34c173bbf8324521633353b8589ed59acc48f4` |

## Review of the proposal

### Retain

The light preview has a clear work surface, subdued navigation, a readable selected row, and a useful relationship between the dataset list and inspector. The dark component preview establishes a consistent family of surfaces, controls, boundaries, and restrained feedback.

Retain ordinary JavaFX controls, explicit semantic roles, spacing-based density, separate scientific color policy, and native visual evidence as the acceptance authority. Mira is explicitly intended for dense interfaces; Linear's March 2026 design account supports quieter navigation and clearer separation between location and view controls. They are references for design decisions, not implementation dependencies. [Mira](https://ui.shadcn.com/docs/changelog/2025-12-shadcn-create), [Linear](https://linear.app/now/behind-the-latest-design-refresh).

### Amend before implementation

1. **Separate application navigation from dock tabs.** The sidebar and location bar describe the application. Dock tabs describe movable panes. Avoid repeating a project title, page tabs, pane title, and toolbar above every scientific view. Keep the large title/metrics block on overview screens; working views should devote more area to data.
2. **Make the inspector recoverable at narrow widths.** The HTML hides it below 1150 px. A desktop window must instead offer collapse/reopen, a dock strip, or a detachable inspector; the selected object's details must remain reachable.
3. **Honor the stated typography floor.** The study uses 10–11 px for several statuses, table headings, chart labels, and metadata. Use 12 px for meaningful supporting information, 13 px for dense table content, and 14 px for general controls at default scale. Validate actual native font faces and baselines.
4. **Treat 32/28 px controls and 40/32 px rows as default minima.** Text enlargement, localization, multiline editors, and validation can require larger geometry. Compact mode reduces spacing, not essential text.
5. **Specify real interaction states.** Some HTML navigation only changes selection or displays a specimen notice; it does not implement separate views. Define native filtering, no-results, hidden-selection, loading, error, and recovery behavior. A filtered-out selection must be explicitly identified or cleared, rather than leaving an unexplained inspector.
6. **Complete keyboard and overlay behavior.** Command search needs selection/activation rules, no-results feedback, Escape, focus restoration, and ownership relative to a floating window. A focus-visible stylesheet cannot make a custom mouse-driven node keyboard-operable.
7. **Use the browser study as visual authority, not a literal window frame.** Its outer preview controls, margin, rounded app card, and caption belong to the study. Native applications should retain normal OS window behavior.

## Ownership and dependency structure

Arrows mean "depends on":

```text
meridian-theme       -> AtlantaFX base + pinned AtlantaFX styling source
meridian-workbench   -> meridian-theme + JavaFX
meridian-scaladock   -> meridian-theme + scaladock-fx
meridian-sampler     -> all three Meridian modules

scaladock-fx         -> scaladock-core + JavaFX
```

| Module | Responsibility |
|---|---|
| `meridian-theme` | Authoritative tokens, generated resources, light/dark themes, density recipes, semantic classes, Java-callable theme entry points. No required ScalaFX or docking dependency. |
| `meridian-workbench` | Optional navigation, view header, inspector sections, status/empty states, and command composition using existing controls. No docking model or scientific domain model. |
| `meridian-scaladock` | Translate Meridian appearance into ScalaDock's public CSS contract and presentation settings; coordinate appearance changes and their lifecycle. |
| `meridian-sampler` | Native fixtures, interaction probes, screenshot scenarios, and performance workloads; not a runtime dependency. |

Keep ScalaDock usable with its own built-in themes and unrelated hosts. Use public selectors and hooks; do not depend on private node-tree anatomy or reflect into ScalaDock internals.

Use AtlantaFX's existing Theme/ThemeManager facilities for theme application. A Meridian integration binding may translate the chosen appearance to registered docks, windows, and compositions; it should not become a competing global theme manager. Verify the exact pinned API during the foundation milestone. [AtlantaFX theme API](https://mkpaz.github.io/atlantafx/atlantafx.base/atlantafx/base/theme/package-summary.html).

## Runtime baseline

**Recommended first Meridian runtime: JDK 25 + JavaFX 27 + AtlantaFX 3.0.0.** AtlantaFX currently documents these requirements, and JavaFX 27 requires JDK 25. Pin the exact dependency versions and the AtlantaFX source revision used for SASS, including upstream notices and a small, reviewable downstream patch set. [AtlantaFX](https://github.com/mkpaz/atlantafx), [JavaFX 27](https://openjfx.io/highlights/27/).

ScalaDock's current libraries compile against JavaFX 24.0.1 and target JDK 22; its demo uses JavaFX 27. Its FX test configuration still uses JavaFX 24.0.1, even in JDK 25 CI jobs. Demo compilation therefore does not establish ScalaDock interaction correctness on JavaFX 27.

- Preserve ScalaDock's JDK 22 / JavaFX 24 floor for generic integration changes.
- Add a real JavaFX 27 consumer test lane with coherent base/graphics/controls and platform-native dependencies.
- Do not claim Meridian supports JavaFX 24. A compatibility edition is separate future scope only if a named application needs it.
- Use the workspace defaults for a new Scala build: Scala 3.7.4 and sbt 1.12.14. ScalaDock currently uses 3.7.0 / 1.11.7; do not silently align it as part of styling.
- ScalaDock is source-only at the reviewed HEAD. Development must pin a full source SHA or an explicitly produced exact artifact. A sibling checkout override must be opt-in. Publication requires a concrete artifact dependency and clean consumer proof, not a SNAPSHOT or source-composite assumption.

## ScalaDock integration contract

The existing [styling contract](../scaladock/docs/styling.md) already provides public classes, pseudo-classes, `DockTheme.Custom(url)`, theme-change events, and propagation to dock regions and floating windows. Build on these.

### Token and stylesheet mapping

Generate the dock adapter from the same authored token source as the JavaFX user-agent theme. Define its dock values on `.dock`, because drag ghosts live in independent popups.

| Meridian semantic role | ScalaDock hook |
|---|---|
| Work surface | `-dock-base` |
| Supporting chrome | `-dock-surface` |
| Raised surface | `-dock-surface-raised` |
| Decorative divider | `-dock-outline` |
| Supporting / primary text | `-dock-text` / `-dock-text-strong` |
| Hover / pressed feedback | `-dock-hover` / `-dock-pressed` |
| Active group / drop emphasis | `-dock-accent`, plus documented state recipes where focus and action roles differ |
| Persistent inactive selection | `-dock-selected-bar` |
| Drop feedback | `-dock-accent-soft`, `-dock-drop-scrim`, `-dock-slot-fill` |
| Overlay shadow | `-dock-shadow` |

Map every documented token, including legacy aliases, deliberately. Keep essential control boundaries distinct from `-dock-outline`. Check translucent drag fills over their actual surfaces.

The compiled Meridian user-agent stylesheet does not automatically override ScalaDock's author-level `dock.css`. Load the adapter after the dock base stylesheet and test computed styles. A dedicated adapter stylesheet is the intended integration layer; it is different from maintaining a broad pile of standard-control overrides.

Generate geometry values into a typed presentation configuration as well as CSS where needed. JavaFX looked-up colors are not a general substitute for typed layout metrics.

### Required provider work

| Priority | Finding at reviewed HEAD | Planned change and acceptance |
|---|---|---|
| P1 | `DockTheme.Custom(url).isDark` is `None`, so `applyWindowScheme` cannot request matching native decorations for custom themes. | Add explicit optional scheme metadata while preserving existing `Custom(url)` use. Review enum/pattern-match compatibility. Verify main and newly created/existing floating windows. |
| P1 | `Dock.settings` and the settings captured by renderers/drag handling are fixed at construction. `LayoutSettings.headerPx` drives both geometry and hit-testing. | Add a generic, FX-thread presentation-metrics update. Update every live consumer atomically, request layout, and safely cancel an active gesture before changing its geometry. Preserve pane objects, IDs, selections, and user layout. |
| P1 | Custom dock chrome includes `HBox` / `StackPane` nodes with mouse handlers and accessible names. Names do not establish complete accessible actions or traversal. | Establish keyboard/accessible activation for tab/close/header actions and minimized strips; retain existing F6 and Ctrl+Tab behavior. Use native control semantics where practical and test focus visibility. |
| P1 | Main regions and floating windows have explicit theme propagation; the ghost receives a stylesheet at construction. Owner-created menus/tooltips have separate popup lifecycle behavior. | Test changes while overlays are already open. Re-style or close/reopen transient overlays through a documented policy; never leave an old-theme popup or stale-density ghost visible. |
| P2 | Drop-indicator motion uses a custom `AnimationTimer`; other feedback uses transitions. | Add a generic reduced-motion policy for custom animation. OS preference support in JavaFX is not proof that custom timers stop. Retain immediate drag-position feedback. |

Relevant provider paths:

- [Dock.scala](../scaladock/modules/fx/src/main/scala/scaladock/fx/Dock.scala): theme, custom scheme, settings, floating stages, native header.
- [DockRegion.scala](../scaladock/modules/fx/src/main/scala/scaladock/fx/DockRegion.scala): theme sheets, layout, animated drop indicator.
- [views.scala](../scaladock/modules/fx/src/main/scala/scaladock/fx/views.scala): headers, tabs, accessible roles, overflow, dividers.
- [drag.scala](../scaladock/modules/fx/src/main/scala/scaladock/fx/drag.scala): hit-testing and ghost popup.
- [sizing.scala](../scaladock/modules/core/src/main/scala/scaladock/sizing.scala): pure geometry and `LayoutSettings`.
- [dock.css](../scaladock/modules/fx/src/main/resources/scaladock/dock.css): author-level chrome recipes.

These are source-backed integration needs, not reproduced runtime defect reports. Keep provider changes generic; no Meridian tokens or AtlantaFX dependency belong in ScalaDock.

### Appearance and lifecycle invariants

- One application preference value controls appearance, density, text scale, and motion policy. First release assumes a consistent appearance across its windows.
- Apply changes on the JavaFX thread. Registration has an explicit disposal path; repeated theme changes must not accumulate listeners or retain closed stages.
- Main scene, popouts, new/restored windows, drag ghosts, context menus, tooltips, combo/date popups, dialogs, and command palette all receive the current appearance.
- A floating single-pane window keeps ScalaDock's existing single-row title treatment; Meridian must not add a duplicate pane header.
- Appearance preferences stay outside the persisted docking/domain layout schema. Theme changes do not rewrite pane state or create new pane views.
- Density and text scale affect effective geometry consistently. Header targets begin at 32/28 logical px, but expand when measured text and controls require it. Keep resize hit areas generous independently of visible divider width.
- Appearance changes do not alter scientific lookup tables, thresholds, normalization, values, or export policy. Scientific charts must not silently inherit AtlantaFX's general chart palette.

## Registered delivery and acceptance backlog

Mote owns status, blockers and claims. This document owns the design contract and maps it to the live issues below. `rel` edges express containment; dependency edges express required completion. Epics depend on their required children so they do not appear as implementation-ready work.

### Tracker homes and completion boundary

- Meridian umbrella: `bd-01M4Y11R2CYJRMJAMKE5C8QCMX` in `meridianfx/.mote`.
- ScalaDock provider epic: `bd-01M4Y1ACZQ98NNBK5JEV5Q49BE` in `scaladock/.mote`.
- Planning/registration task: `bd-01M4Y1ABNPPBNXBW58WSJPKJWP`; completing this task does not complete any implementation epic.
- Existing workspace runtime discovery: `bd-01M3VMVAWWK0QAFHYT351CSX6P` in `../.mote`; reused as provenance for the pilot runtime decision.
- Scope: **6 epics, 26 implementation/acceptance tasks, and 1 planning task**; 77 blocker edges and 31 containment edges, all within their owning stores.

A cross-store issue ID is a provenance reference, not a Mote blocker edge. B0 is a real local acceptance task, initially blocked pending a provider candidate. Reopen B0 when S6 supplies a reviewed candidate and then run Meridian's consumer probe. D0 separately gates the selected application's runtime readiness; it does not block A1–A3 or generic ScalaDock work.

### Shared acceptance rules

1. Close a task only when its checklist and named evidence pass. Keep review work separate; B3, S6 and D7 require a reviewer distinct from the relevant implementer. Do not infer passing execution from a test file or CI configuration.
2. Every executable receipt names exact repository candidates/artifact hashes, command, actual exit status, fixture inputs, output/log locations, runtime and material limitations. Preserve adverse evidence and distinguish local, hosted, native and software-renderer results.
3. Required checks may not be weakened or re-baselined solely to pass. A failed requirement remains open until repaired or an explicit scope/acceptance decision is recorded. A blocker is not acceptance.
4. No app or provider files are edited through another repository's ownership. Reserve exact paths before work; overlapping tasks share one writer or serialize. ScalaDock's provider sequence is deliberately serialized because its files overlap.
5. Planning, local candidate packaging, and review do not authorize publication, pushing, merging, or unrelated application runtime migration. Follow each repository's existing rules at those boundaries.

### Fixed validation matrix

| Dimension | Required v0.1 evidence |
|---|---|
| Appearance and density | Light/dark × comfortable/compact, on the same deterministic fixtures |
| Runtime | Meridian JDK25/JavaFX27/AtlantaFX3.0.0; ScalaDock retains JDK22/JavaFX24.0.1 plus a real JavaFX27 interaction lane |
| Initial supported platforms | Named macOS and Linux environments; exact OS versions, fonts and renderers recorded. Windows remains unqualified until equivalent native evidence is added. |
| Logical viewport | 1280×800 and 1024×700; recovery and reachability at 800×600 |
| Display/text scaling | Available 1x/2x display scales recorded separately from 100/125/150/200% text scaling; unavailable combinations are explicit gaps |
| Fixtures | Dataset browser, scientific viewport with inspector, settings/analysis form; component state gallery |
| Control states | Default, hover, pressed, disabled, focused, selected, inactive selection, invalid, read-only, editing, empty, loading, error; relevant combinations such as focused+invalid and disabled+selected |
| Dock states | Drag/header insertion/edge drops, cancellation, resize, overflow, minimize/reopen, maximize/restore, popout/dock-back, save/load, perspective changes and close veto |
| Detached UI | Main and restored/new floating windows, drag ghost, menu/tooltip/combo/date/color popups, dialog and command composition; switches while open |
| Contrast | Declared meaningful-text pairs ≥4.5:1; applicable essential non-text/focus pairs ≥3:1; alpha on real backgrounds; documented decorative/disabled exceptions |
| Scientific isolation | Identical source data/LUT/threshold/normalization/state; deterministic exports under a fixed policy; zero appearance-triggered analysis calls |
| Stress workload | 100000 virtualized rows, eight panes, three floats; three 120 s trials after ≥20 s warmup; named machine and instrumentation; proposed p95 UI pulse processing ≤16.7 ms |
| Resource lifecycle | 100 switch/popout/dock-back/close cycles; stage/subscription/pane counts return to defined baselines; retained heap measured separately |
| Artifact use | Clean Java theme-only and Scala docking consumers outside sibling checkouts against exact local candidate artifacts |

The display-scale set is evidence-dependent; a missing required configuration remains an explicit support limitation and is evaluated in D7. No Windows or blanket accessibility claim is inferred from the initial matrix. The existing design-study reported checks are historical claims, not substitutes for this matrix.

### Execution order

```text
Meridian: A1 -> A2 -> A3
Provider: S1 -> S2 -> S3 -> S4 -> S5 -> S6
Provider receipt -> reopen B0 -> B1 -> B2 -> B3
B3 -> C1 / C2 / C3 -> C4 / C5 -> D1 / D2 / D6
B3 + C2 -> D3 -> D4
A1 -> D0 (pilot/runtime decision); D0 + C4 + C5 + D3 -> D5
D1 + D2 + D3 + D4 + D5 + D6 -> D7
All phase children -> phase closure; E0 + E1 + E2 + E3 -> umbrella closure
```

The provider-receipt arrow is a manual cross-store evidence handoff. The actual local blocker edges are listed per issue below and were verified in Mote; this diagram is a readable summary.

### [E0] Establish the pinned native theme foundation

**Mote:** `bd-01M4Y1ABQJSG2HSG059RFK802F` · **Store:** `meridianfx/.mote`

Make the theme reproducibly buildable and usable from Java or Scala.

**Epic acceptance:**

- [x] A1–A3 pass with exact versions and source provenance.
- [x] A clean build launches the initial native themed scene and resolves a coherent JavaFX 27 runtime.
- [x] Generated resources and metrics are deterministic; palette relationships and the minimal control state set pass.

#### [A1] Bootstrap the build, runtime pins and upstream provenance

**Mote:** `bd-01M4Y1ABS0W5YJ245C1XEQ5CYW` · **Priority:** P1 · **Requires:** None

**Outcome:** Establish the actual project/build contract without silently changing provider toolchains.

**Owned scope:** build.sbt; project/build.properties; module skeletons; README.md; upstream lock/provenance and license files

**Acceptance criteria:**

- [ ] Create the four module boundaries from PLAN.md; theme and workbench expose Java-friendly APIs and do not require ScalaFX or ScalaDock; only adapter depends on ScalaDock.
- [ ] Pin Scala 3.7.4, sbt 1.12.14, JDK 25, JavaFX 27 and AtlantaFX 3.0.0; verify availability and exact upstream style-source commit before implementation. If a dependency is unavailable, report it rather than substituting silently.
- [ ] Resolve a single compatible JavaFX base/graphics/controls/native set on the build platform; record the resolved graph and runtime versions. Default provider source resolution uses a full SHA, with any sibling checkout override explicit.
- [ ] A clean native smoke application builds and exits; define documented formatting/compile/test/generation commands and retain raw exit status. Preserve all original reference asset hashes.
- [ ] Record upstream MIT notices and font/icon redistribution provenance. No public repository, commit, push or publication is implied by this task.

**Required evidence:** Clean build/smoke log, dependency tree, runtime manifest, upstream source lock and notices; record actual command names once implemented.

#### [A2] Define semantic tokens and deterministic generation with contrast contracts

**Mote:** `bd-01M4Y1ABTGV06BWQJAX3ZKXFZX` · **Priority:** P1 · **Requires:** `A1` (`bd-01M4Y1ABS0W5YJ245C1XEQ5CYW`)

**Outcome:** Make one authored source govern browser/native palette, geometry and adapter values.

**Owned scope:** tokens; generator tooling; generated theme resources; generated typed metrics; design documentation

**Acceptance criteria:**

- [x] Define raw/semantic/component roles for surfaces, meaningful text, decorative borders, essential control boundaries, action/focus/selection, invalid/disabled states, typography, density and motion.
- [x] Generate native CSS, initial dock-token outputs and typed logical metrics from one source; a second generation is byte-identical and CI detects edited or stale generated outputs.
- [x] Declare every foreground/background test pair and state. Normal meaningful text meets 4.5:1 and essential boundaries/focus information meet 3:1 where applicable; evaluate alpha compositing on actual backgrounds and record decorative/disabled exceptions explicitly.
- [x] Default meaningful text is at least 12 px, table content 13 px and general controls 14 px; comfortable/compact geometry starts at 32/28 control and 40/32 row minima without reducing text.
- [x] Scientific colormaps and data-series policy are explicitly outside UI palette generation; supplied illustrative curves are not validated scientific colormaps.

**Required evidence:** Determinism/staleness tests, pair-specific contrast report including composited values, token coverage/schema checks and generated-source provenance.

#### [A3] Implement the initial AtlantaFX-derived themes and appearance binding

**Mote:** `bd-01M4Y1ABW0Q7QDR63Y7VCMTMB2` · **Priority:** P1 · **Requires:** `A2` (`bd-01M4Y1ABTGV06BWQJAX3ZKXFZX`)

**Outcome:** Provide the minimum real theme needed for the early docked slice.

**Owned scope:** meridian-theme source/resources; initial control fixture; appearance lifecycle tests

**Acceptance criteria:**

- [x] Compile deliberate AtlantaFX-derived user-agent resources for both appearances and densities; preserve a small reviewable upstream delta rather than a Modena-overlay production path.
- [x] Use the pinned AtlantaFX Theme/ThemeManager facilities; appearance, density, text scale and motion have one application preference source with explicit apply/dispose lifecycle.
- [x] Buttons, labels, text fields, basic table, menu and dialog render default/hover/pressed/focused/selected/disabled/invalid states without JavaFX CSS parser warnings.
- [x] Switching appearance reaches registered existing/new scenes without duplicate stylesheet entries or listeners; missing optional appearance preferences have documented defaults.
- [x] A small Java caller compiles and invokes the theme API; runtime theme JAR does not pull in ScalaDock or mandatory ScalaFX.

**Required evidence:** Native smoke captures in four variants, computed-style assertions, lifecycle/disposal tests and Java API compilation.

### [Meridian provider] Qualify generic theme, density and accessibility integration

**Mote:** `bd-01M4Y1ACZQ98NNBK5JEV5Q49BE` · **Store:** `scaladock/.mote`

Supply generic presentation capabilities required by Meridian while preserving ScalaDock's independent architecture and runtime floor.

**Epic acceptance:**

- [ ] S1–S6 pass on an exact reviewed provider revision, including JDK22/JavaFX24 floor and JDK25/JavaFX27 runtime lanes.
- [ ] No AtlantaFX/Meridian dependency, theme-specific colors, scientific policy or new docking/persistence ownership enters ScalaDock.
- [ ] Existing public selectors and existing Custom(url) use remain supported or receive an explicit reviewed compatibility disposition; pane identity/persistence/close-admission behavior is preserved.
- [ ] Native custom schemes, live metrics/hit-tests, popup lifecycle, keyboard accessible actions and motion policy meet task acceptance.
- [ ] Provide a consumer-ready receipt to Meridian epic bd-01M4Y11R2CYJRMJAMKE5C8QCMX and local gate bd-01M4Y1ABZ2J47PJP195GP8JSPS. Provider completion alone does not establish Meridian compatibility.

#### [S1] Add custom theme appearance metadata without raising the library floor

**Mote:** `bd-01M4Y1AD18TGSE7FQBMXQHBPFY` · **Priority:** P1 · **Requires:** None

**Outcome:** Let custom themes specify native light/dark scheme through public generic APIs.

**Owned scope:** modules/fx/src/main/scala/scaladock/fx/Dock.scala; ThemeSuite; docs/styling.md

**Acceptance criteria:**

- [x] Preserve existing built-in Dark/Light and Custom(url) caller behavior; add explicit optional scheme metadata with a documented source/pattern-match compatibility review.
- [x] Known custom schemes reach existing/new/restored floating scenes; the main-scene ownership/application hook is explicit and documented.
- [x] Unknown/custom-default scheme uses a deliberate fallback and never guesses from a URL/name or equals check against a built-in theme.
- [x] JavaFX24 floor still compiles/loads without new-runtime linkage failures; JavaFX27 native decorations follow supported platform behavior.
- [x] Generic tests exercise synthetic theme URLs; no Meridian or AtlantaFX dependency is introduced.

**Required evidence:** ThemeSuite/API compatibility probes, JDK22/FX24 and JDK25/FX27 runs; native scheme evidence for supported platforms.

#### [S2] Make presentation metrics update atomically across layout and drag handling

**Mote:** `bd-01M4Y1AD2TV1YDNQ20NKE2PF6M` · **Priority:** P1 · **Requires:** `S1` (`bd-01M4Y1AD18TGSE7FQBMXQHBPFY`)

**Outcome:** Support live density and enlarged text without stale geometry or pane reconstruction.

**Owned scope:** Dock.scala; DockRegion.scala; views.scala; drag.scala; sizing.scala; geometry/drag/divider/minimize suites

**Acceptance criteria:**

- [x] Introduce a public FX-thread metrics update path; every renderer, minimized strip, geometry computation and drag hit-test consumes the same current metrics snapshot.
- [x] Reject invalid/non-finite/negative metrics according to an explicit documented contract and define effective minima for zero/tiny available space.
- [x] Cancel an armed/active pane or divider drag safely before metrics change; clear ghost/preview/cursors and ensure the next gesture uses the new geometry.
- [x] Switch headers 32→28→32 and enlarged-text metrics with existing/new/restored floats; assert actual header/content bounds agree with model hit-tests.
- [x] Keep pane Node identities, selected IDs, scientific/application state, split ownership and saved-layout schema intact; do not dispose/recreate the dock.
- [x] Honor dynamic text/control preferred minima at the adapter boundary instead of treating density height as an absolute clipping ceiling.

**Required evidence:** Sizing/geometry assertions, DragDropSuite/DividerSuite/MinimizeFxSuite/PopoutSuite regressions and instance/state counters.

#### [S3] Define theme changes for live popups and dispose presentation bindings

**Mote:** `bd-01M4Y1AD4DZVMZNHBXTA4VC80V` · **Priority:** P1 · **Requires:** `S2` (`bd-01M4Y1AD2TV1YDNQ20NKE2PF6M`)

**Outcome:** Prevent stale ghost/menu appearance and retained window/subscription resources.

**Owned scope:** Dock.scala; DockRegion.scala; views.scala; drag.scala; theme/popout/tab-menu fixtures

**Acceptance criteria:**

- [x] Specify and implement propagation or explicit dismissal/reopen policy for already-open ghost, context/overflow menu and tooltip surfaces.
- [x] Apply public custom styles on the independent .dock ghost root and floating/header roots; preserve unrelated owner stylesheets.
- [x] Switch light/dark and density during open overlays and window creation/restore; no visible surface keeps stale theme geometry or unresolved lookup colors.
- [x] Close/dispose all owned transient UI and listeners when its dock/owner closes; repeated bind/switch/close cycles do not add stylesheet duplicates or live subscriptions.
- [x] Document which host-created dialogs/popups require host registration; use public hooks rather than theme-specific scene traversal.

**Required evidence:** Computed-style probes, popup open/change/close cases, stylesheet/listener counts and owner-disposal tests.

#### [S4] Complete keyboard and accessible actions for custom docking chrome

**Mote:** `bd-01M4Y1AD636FK3M3DJNGTB6W9Y` · **Priority:** P1 · **Requires:** `S3` (`bd-01M4Y1AD4DZVMZNHBXTA4VC80V`)

**Outcome:** Make tab/header/minimized actions usable without mouse-only handlers.

**Owned scope:** views.scala; Dock.scala keyboard actions; dock.css; KeyboardSuite/ChromeSuite/TabMenuSuite/MinimizeFxSuite

**Acceptance criteria:**

- [ ] Tabs, close buttons, group actions, overflow and minimized strips expose correct role/name/state and actionable keyboard/assistive semantics.
- [ ] Keyboard users can select/close/reopen/minimize/maximize/pop out/dock back through documented actions or menus; hidden-hover chrome does not make actions unreachable.
- [ ] Preserve F6/Ctrl+Tab routing and let content-consumed keys remain consumed by content; Escape cancels drags and closes appropriate transient UI.
- [ ] Focus indication remains visible on selected/inactive states and returns to a live appropriate target after close/dock/perspective transitions.
- [ ] Retain accessible selection notifications and close veto/async admission; role/name assignment alone is not treated as complete accessibility proof.

**Required evidence:** Automated action/focus/routing assertions and bounded native assistive-technology probes; no regression in existing keyboard/close suites.

#### [S5] Add explicit reduced-motion behavior for custom docking animations

**Mote:** `bd-01M4Y1AD7SHRQSGD38RTJ86517` · **Priority:** P1 · **Requires:** `S4` (`bd-01M4Y1AD636FK3M3DJNGTB6W9Y`)

**Outcome:** Honor motion preferences in custom timers as well as native transitions.

**Owned scope:** DockRegion.scala indicator timer; views.scala tab slide/divider feedback; generic presentation API and tests

**Acceptance criteria:**

- [ ] Provide generic motion policy with runtime changes and a documented host/OS-preference bridge that remains compatible with the library floor.
- [ ] Reduced motion immediately settles indicator glides/tab slides and nonessential fades; custom AnimationTimer execution stops when settled, hidden or disposed.
- [ ] Keep immediate drag-follow and clear drop-target feedback; cancellation leaves no running timer, stale indicator or cursor.
- [ ] Test switching policy mid-animation, during density/theme changes and while closing a floating owner.
- [ ] Default-motion behavior remains intentional and bounded; implementation does not rely solely on JavaFX27 release-note guarantees.

**Required evidence:** Timer/lifecycle assertions, reduced/full-motion native traces and drag-cancellation regressions on floor/27 lanes.

#### [S6] Qualify the provider on both runtime lanes and issue an independent receipt

**Mote:** `bd-01M4Y1AD9D3H9T2PPFKHJYFF3S` · **Priority:** P1 · **Requires:** `S1` (`bd-01M4Y1AD18TGSE7FQBMXQHBPFY`), `S2` (`bd-01M4Y1AD2TV1YDNQ20NKE2PF6M`), `S3` (`bd-01M4Y1AD4DZVMZNHBXTA4VC80V`), `S4` (`bd-01M4Y1AD636FK3M3DJNGTB6W9Y`), `S5` (`bd-01M4Y1AD7SHRQSGD38RTJ86517`)

**Outcome:** Prove generic provider compatibility before Meridian consumes the candidate.

**Owned scope:** build.sbt; CI/runtime test configuration; fx/core tests; docs/styling.md; provider evidence

**Acceptance criteria:**

- [ ] Run required formatting/compile/core/fx regression checks on exact candidate, including JDK22/JavaFX24.0.1 floor and JDK25/JavaFX27. Runtime jars/classifiers are coherent and recorded.
- [ ] Tests actually execute JavaFX27 interaction code; compiling a 27 demo with FX tests still using 24 is insufficient.
- [ ] Cover pane identity, persisted layouts/perspectives, async close admission, custom scheme, metric/hit-test updates, live popups, keyboard and custom motion.
- [ ] Independent reviewer records candidate SHA, API/selector compatibility verdict, commands/exits, native evidence and open limitations.
- [ ] Publish the receipt locally to this issue and reference it from Meridian bd-01M4Y1ABZ2J47PJP195GP8JSPS; leave source publication/runtime upgrades of other applications outside scope.

**Required evidence:** Existing commands: sbt scalafmtCheckAll compile test; floor core/test fx/test; add and document a real 27 test lane before claiming it ran. Keep raw logs and native manifests.

### [E1] Prove Meridian on a real ScalaDock workspace

**Mote:** `bd-01M4Y1ABXGVJ1W4SYHZ5XTN0V3` · **Store:** `meridianfx/.mote`

Make docking compatibility an early visual and behavioral acceptance gate.

**Epic acceptance:**

- [ ] B0 accepts a reviewed exact ScalaDock revision through the declared consumer checks.
- [ ] B1/B2 deliver a functioning docked workspace and detached inspector in all four appearance/density variants.
- [ ] B3 independently accepts the native hierarchy, density, focus and window/overlay consistency before broad catalog expansion.

#### [B0] Accept the ScalaDock provider candidate on JavaFX 27

**Mote:** `bd-01M4Y1ABZ2J47PJP195GP8JSPS` · **Priority:** P1 · **Requires:** `A1` (`bd-01M4Y1ABS0W5YJ245C1XEQ5CYW`) · **Initial state:** blocked on external provider candidate

**Outcome:** Consume and verify the provider contract; do not duplicate provider implementation in this repository.

**Owned scope:** provider pin; meridian-scaladock consumer fixture; docs/evidence/provider-acceptance

**Acceptance criteria:**

- [ ] Read the ScalaDock provider epic bd-01M4Y1ACZQ98NNBK5JEV5Q49BE in ../scaladock; obtain its reviewed full commit, floor/27 evidence and API notes. Keep this issue blocked until an actual candidate is available.
- [ ] Pin that candidate explicitly and execute a JavaFX 27 consumer probe for custom scheme, live metrics, popup policy, keyboard actions, motion and pane identity.
- [ ] Prove default dependency resolution does not silently use ../scaladock; changing the configured pin changes the tested candidate and is recorded.
- [ ] Record exact provider/consumer SHAs or artifact digests, runtime graph, commands/exits and limitations; only a passing consumer receipt unblocks B1.

**Required evidence:** Executable consumer receipt; provider S6 review is required evidence, not a substitute for this consumer run.

#### [B1] Implement the Meridian-to-ScalaDock adapter

**Mote:** `bd-01M4Y1AC0JTZKW8J9BXH2W28NG` · **Priority:** P1 · **Requires:** `A3` (`bd-01M4Y1ABW0Q7QDR63Y7VCMTMB2`), `B0` (`bd-01M4Y1ABZ2J47PJP195GP8JSPS`)

**Outcome:** Translate appearance into public ScalaDock hooks with correct CSS precedence and lifecycle.

**Owned scope:** meridian-scaladock source/resources/tests

**Acceptance criteria:**

- [ ] Map every documented -dock-* token (including legacy aliases) on .dock from generated semantic values; distinguish focus, selection and essential boundaries.
- [ ] Install adapter recipes after dock.css and assert computed paints in main root, float, ghost and menus; no dependency on private ScalaDock node anatomy.
- [ ] Propagate appearance/density/text-scale/motion to existing/new/restored windows atomically on the FX thread, using the generic provider APIs.
- [ ] Dispose subscriptions and registrations deterministically; repeated binds/unbinds and 100 appearance changes leave one stylesheet entry per intended resource and no retained closed windows.
- [ ] Keep scientific content palette and persisted pane/domain state independent of appearance.

**Required evidence:** Computed-style tests, subscription/stylesheet counts, new/restored/popout lifecycle probes and token coverage report.

#### [B2] Build the native dataset and scientific docked workspace

**Mote:** `bd-01M4Y1AC213806BRJE6J4TNPHK` · **Priority:** P1 · **Requires:** `B1` (`bd-01M4Y1AC0JTZKW8J9BXH2W28NG`)

**Outcome:** Create the first complete executable specimen with realistic content and window behavior.

**Owned scope:** meridian-sampler workspace fixture; minimal workbench compositions; deterministic synthetic data

**Acceptance criteria:**

- [ ] Use a real JavaFX table, synthetic scientific viewport, inspector and console in ScalaDock; sidebar/location navigation and movable-pane tabs have distinct roles.
- [ ] Filtering, row selection, no-results, hidden-selection policy, a real validation dialog and context menu behave coherently. Adopt clear-selection on filtered-out rows unless a visible retained-selection explanation is implemented and tested.
- [ ] Inspector can pop out, dock back, minimize, reopen and recover at 800×600 logical viewport; necessary controls remain reachable through scrolling or overflow.
- [ ] Save/load, perspective switch, close veto and drag cancellation preserve pane identities and values; a single-pane float has one title/header treatment.
- [ ] Record fixed synthetic fixture IDs, dimensions and initial selection for repeatable captures; include active, inactive and keyboard-focused states.

**Required evidence:** Four variant native captures plus deterministic interaction script/probes, instance counters, saved-layout and selected-object assertions.

#### [B3] Independently accept the native docked vertical slice

**Mote:** `bd-01M4Y1AC3GPTQ6CPQBYP2SZP7D` · **Priority:** P1 · **Requires:** `B2` (`bd-01M4Y1AC213806BRJE6J4TNPHK`)

**Outcome:** Review the native result before catalog expansion; review must be distinguishable from builder self-checks.

**Owned scope:** docs/evidence/vertical-slice; native captures and review findings

**Acceptance criteria:**

- [ ] A reviewer other than the slice implementer inspects exact candidate captures and reruns key consumer interactions; record reviewer identity and candidate SHA.
- [ ] Accept light/dark × comfortable/compact at 1280×800 and 1024×700, plus 800×600 recovery; check native font baseline, text floors, selected/focused/inactive distinction, and detached inspector.
- [ ] Change density after a drag is armed and verify safe cancellation, subsequent header/edge targets, resize hit area and pane identity. Open menus/dialogs during appearance changes.
- [ ] Every blocking finding has a reproduction and is repaired/rechecked or remains open; do not pass based on browser similarity alone.
- [ ] The accepted native specimens become versioned baselines; preserve the browser references as original design intent.

**Required evidence:** SHA-bound independent review, four variant baseline manifest, interaction logs and closed blocking findings.

### [E2] Complete the native component and workbench system

**Mote:** `bd-01M4Y1AC50DY6493QZPQWMJZY0` · **Store:** `meridianfx/.mote`

Extend the accepted native slice into a coherent reusable control and application composition system.

**Epic acceptance:**

- [ ] C1–C5 cover the full component inventory and three realistic application fixtures.
- [ ] Applicable state combinations are explicit and render coherently in four variants, including edited/invalid/focused, disabled/selected and inactive selection.
- [ ] Optional workbench and command composition work without a docking dependency; overlay appearance and narrow-window recovery meet the contract.

#### [C1] Complete action, form, feedback and typography recipes

**Mote:** `bd-01M4Y1AC6JTSXM8Y0XPTGX188F` · **Priority:** P1 · **Requires:** `B3` (`bd-01M4Y1AC3GPTQ6CPQBYP2SZP7D`)

**Outcome:** Complete ordinary native control recipes from the accepted baseline.

**Owned scope:** meridian-theme action/form/feedback recipes; sampler component fixture

**Acceptance criteria:**

- [ ] Cover buttons, toggle buttons, check/radio controls, text/password/text-area inputs, spinner, slider, progress and validation feedback using standard JavaFX or suitable existing AtlantaFX controls.
- [ ] Verify default/hover/pressed/focus/disabled and applicable invalid/read-only/indeterminate/selected combinations; selected or invalid controls retain visible keyboard focus.
- [ ] Native regular/strong font faces, alignment and icon optical weight are documented and measured; 125/150/200% text scaling grows controls or makes content scrollable.
- [ ] Validation uses persistent label/error text and accessible associations; no meaning depends only on color. Default 12/13/14 px text roles are honored.
- [ ] Compact mode changes spacing without shrinking essential typography; controls allow localization and multiline content.

**Required evidence:** Native computed geometry/paint tests, four variant state fixture, long-label/enlarged-text captures and validation associations.

#### [C2] Complete virtualized data and selection controls

**Mote:** `bd-01M4Y1AC868PT0M45WRQ8ET1H6` · **Priority:** P1 · **Requires:** `B3` (`bd-01M4Y1AC3GPTQ6CPQBYP2SZP7D`)

**Outcome:** Make dense data views usable through selection, editing, overflow and focus transitions.

**Owned scope:** meridian-theme table/tree/list/tab/scrollbar recipes; virtualized fixtures

**Acceptance criteria:**

- [ ] Cover TableView, TreeTableView, TreeView, ListView, TabPane, headers, sorting and scrollbars with realistic cells, icons, numeric alignment and empty/loading/error content.
- [ ] Verify active/inactive selection, selection+focus, disabled+selected, editing+invalid, cell commit/cancel, long labels and horizontal/vertical overflow.
- [ ] Virtualized cell reuse clears stale style/state/validation from previous rows; editable contents and font enlargement can exceed default row minima.
- [ ] Columns preserve stable numeric alignment with the chosen font strategy; scientific values and formatting semantics are unchanged by theme/density.
- [ ] Sampler has deterministic 0/1/many/100000 row cases; no per-cell shadows, idle animations or theme-triggered scientific work.

**Required evidence:** Selection/edit/reuse regression probes, geometry captures, virtualized-node counts and deterministic data assertions.

#### [C3] Complete menus, popups, dialogs and overlay lifecycle

**Mote:** `bd-01M4Y1AC9V1P5G9ZM4VH766YEK` · **Priority:** P1 · **Requires:** `B3` (`bd-01M4Y1AC3GPTQ6CPQBYP2SZP7D`)

**Outcome:** Apply the system consistently outside the main scene graph.

**Owned scope:** theme overlay recipes; workbench overlay binding; popup/dialog fixtures

**Acceptance criteria:**

- [ ] Cover context/menu/menu-button controls, combo/choice/date/color popups, tooltips, alerts and dialogs in main and floating owners.
- [ ] Check appearance changes while overlays are open and creation after a switch; no stale palette, density or fallback Modena/Atlanta default remains unintentionally visible.
- [ ] Popup location and clipping remain correct near window/display edges and enlarged text; overlays stay usable at 800×600.
- [ ] Escape, selection/activation and focus return respect the original owner; a closed owner does not retain an overlay or focus target.
- [ ] Use platform-native file dialogs where applicable and document that OS-controlled surfaces are outside CSS styling.

**Required evidence:** Native open-overlay captures and owner/focus/close-lifecycle assertions in all four variants.

#### [C4] Finish optional workbench compositions and three realistic fixtures

**Mote:** `bd-01M4Y1ACBGF18WJCQ139EAN4VA` · **Priority:** P1 · **Requires:** `C1` (`bd-01M4Y1AC6JTSXM8Y0XPTGX188F`), `C2` (`bd-01M4Y1AC868PT0M45WRQ8ET1H6`), `C3` (`bd-01M4Y1AC9V1P5G9ZM4VH766YEK`)

**Outcome:** Deliver reusable application structure without owning docking or scientific models.

**Owned scope:** meridian-workbench; sampler dataset/viewport/settings fixtures

**Acceptance criteria:**

- [ ] Provide navigation, location/view header, inspector sections and status/empty/loading/error compositions built from existing controls; no duplicate pane/title rows.
- [ ] Dataset browser, scientific viewport+inspector and settings/analysis form fixtures exercise populated, empty, loading, partial/error and recovery states.
- [ ] At 1280×800, 1024×700 and 800×600 all essential actions/details remain reachable; inspector collapse always has a labeled keyboard-accessible reopen action.
- [ ] Long paths/labels, negative/large numbers, missing values and validation messages have explicit layout behavior; no clipped required text under 200% text enlargement.
- [ ] Workbench-only sample compiles without ScalaDock; compositions expose semantic classes and do not retain application-specific scientific data.

**Required evidence:** Standalone workbench consumer compile, deterministic fixture state matrix and native recovery/resize checks.

#### [C5] Implement command search with native keyboard and focus semantics

**Mote:** `bd-01M4Y1ACD68Q4G6XW8YW9BCSBK` · **Priority:** P1 · **Requires:** `C1` (`bd-01M4Y1AC6JTSXM8Y0XPTGX188F`), `C3` (`bd-01M4Y1AC9V1P5G9ZM4VH766YEK`)

**Outcome:** Provide an optional command composition that works across main and floating windows.

**Owned scope:** meridian-workbench command composition; sampler command registry/fixture

**Acceptance criteria:**

- [ ] Command registry separates stable IDs, visible labels, availability and execution; UI uses caller-provided actions rather than owning application architecture.
- [ ] Open shortcut honors platform conventions and content-owned key handling; typing filters, Up/Down select, Enter executes exactly once, Escape dismisses and restores prior valid focus.
- [ ] Empty/no-results/disabled commands and clearing search have visible and accessible feedback; focus remains within the open composition.
- [ ] Command invocation from a float acts on its documented context; closing that owner safely closes/reparents the overlay under an explicit tested policy.
- [ ] All four variants and 200% text scaling remain usable; accessible names/results state and reduced-motion behavior are verified.

**Required evidence:** Keyboard event and action-count tests, no-results/disabled state captures, owner disposal and focus-return probes.

### [E3] Qualify native behavior, scientific isolation and real adoption

**Mote:** `bd-01M4Y1ACEWD2JE1T3TXHNVSWDX` · **Store:** `meridianfx/.mote`

Turn the theme and sampler into an evidence-qualified development release candidate.

**Epic acceptance:**

- [ ] D0–D7 close with exact candidate-bound evidence and a real application pilot.
- [ ] Declared v0.1 support is macOS and Linux at tested versions/scales; Windows is explicitly unqualified unless equivalent native evidence is added.
- [ ] Scientific values/colormaps/export policy remain invariant; behavior/a11y/performance/packaging gates pass without unexplained exceptions.
- [ ] Independent final review confirms documentation and artifacts match evidence; public release remains separately authorized.

#### [D0] Select the real pilot and resolve its runtime migration boundary

**Mote:** `bd-01M4Y1ACGJ8TKMKNM4AS7H1S0W` · **Priority:** P1 · **Requires:** `A1` (`bd-01M4Y1ABS0W5YJ245C1XEQ5CYW`)

**Outcome:** Name and bound one application integration before touching application code.

**Owned scope:** pilot decision in PLAN.md or its successor acceptance record; application-owned Mote issue

**Acceptance criteria:**

- [ ] Select one named real application with an actual scientific viewport and editing workflow; record maintainer/user selection and exact current application/provider revisions.
- [ ] Read existing workspace discovery bd-01M3VMVAWWK0QAFHYT351CSX6P in ../.mote and the chosen application's own runtime evidence. Historical notes are leads, not a current qualification.
- [ ] Record whether the application already qualifies on JDK25/JavaFX27; if migration is needed, create/reuse its application-owned issue and obtain its accepted runtime receipt before marking this gate complete.
- [ ] Keep generic Meridian and ScalaDock work independent of that broader migration; no application/provider toolchain is silently upgraded.
- [ ] Define pilot-owned files, allowed synthetic/approved fixtures, expected workflow, rendering/export invariants and rollback; source changes stay in the application tracker.

**Required evidence:** Named pilot decision and application-owned runtime/scope receipt with full SHAs; this issue is a consumer gate, not authority to migrate unrelated apps.

#### [D1] Establish native visual and interaction regression coverage

**Mote:** `bd-01M4Y1ACJ7SAJ4FQDAWP676DX5` · **Priority:** P1 · **Requires:** `C4` (`bd-01M4Y1ACBGF18WJCQ139EAN4VA`), `C5` (`bd-01M4Y1ACD68Q4G6XW8YW9BCSBK`)

**Outcome:** Capture stable native acceptance evidence and detect state/layout regressions.

**Owned scope:** sampler automation; CI; screenshot manifests and reviewed baselines

**Acceptance criteria:**

- [ ] On named macOS and Linux environments run all four variants at 1280×800 and 1024×700, default text and available 1x/2x display scales; record unavailable scale/platform combinations honestly.
- [ ] Exercise 800×600 recovery and 125/150/200% text enlargement, long/localized labels, row editing, empty/loading/error, active/inactive selection, drag/drop, minimized/maximized/overflow, popouts and overlays.
- [ ] Capture stage dimensions, OS/display scale, native font faces, renderer, JDK/JavaFX/dependency versions, fixture ID and exact candidate SHA with every accepted specimen.
- [ ] Automated baseline comparisons use reviewed platform-specific tolerances and targeted geometry/state assertions; failed images are preserved and never auto-blessed.
- [ ] Linux virtual-display/software runs and native renderer captures are reported separately; Windows support is not claimed from Linux/macOS evidence.
- [ ] Document automation ownership and close only task-owned stages/tooling after runs; native OS-input scenarios run with an explicit bounded execution window.

**Required evidence:** Native capture index, scripted scenario results, raw logs/exit codes, comparison findings and ownership/cleanup receipt.

#### [D2] Verify keyboard, accessible actions, focus and motion

**Mote:** `bd-01M4Y1ACKVCC0J51AFVVNVEDHA` · **Priority:** P1 · **Requires:** `C4` (`bd-01M4Y1ACBGF18WJCQ139EAN4VA`), `C5` (`bd-01M4Y1ACD68Q4G6XW8YW9BCSBK`)

**Outcome:** Qualify operation independently of visual screenshots or contrast-pair checks.

**Owned scope:** native behavior/accessibility tests; manual assistive-technology checklist and findings

**Acceptance criteria:**

- [ ] Complete primary fixture workflows by keyboard, including F6/Ctrl+Tab, menus, validation, tab/group actions, minimize/reopen, command palette, dialogs, close veto and focus restoration across floats.
- [ ] Accessible roles/names/state/action support is checked in native trees and manually with VoiceOver on macOS and the supported Linux assistive stack; record exact environment and unresolved limitations.
- [ ] Visible focus remains distinguishable on selected/invalid controls and in inactive windows; required actions cannot depend solely on hover or mouse drag.
- [ ] OS/app reduced-motion preferences suppress custom indicator glides, fades and tab sliding while preserving immediate pointer feedback; runtime preference changes are tested.
- [ ] Required controls/text remain reachable at 200% text scale; applicable 4.5:1 text and 3:1 essential non-text checks pass on actual generated state colors.
- [ ] Separate UI usability/accessibility findings from any broad compliance claim; unresolved blocking findings prevent completion.

**Required evidence:** Keyboard/focus/action assertions, manual assistive-technology transcript/findings, motion probes and contrast report.

#### [D3] Prove scientific appearance and state isolation

**Mote:** `bd-01M4Y1ACNFSYM16RGVY4EH6F14` · **Priority:** P1 · **Requires:** `B3` (`bd-01M4Y1AC3GPTQ6CPQBYP2SZP7D`), `C2` (`bd-01M4Y1AC868PT0M45WRQ8ET1H6`)

**Outcome:** Show that theme changes cannot change scientific meaning or trigger analysis.

**Owned scope:** sampler scientific fixture; explicit rendering-policy boundary tests; pilot invariant specification

**Acceptance criteria:**

- [ ] Use fixed synthetic values with an explicit LUT, thresholds, normalization, opacity and export policy; do not use general -color-chart-* values for scientific meaning.
- [ ] Across appearance, density, text scale, window move/popout/restore and perspective switches, assert identical source values, IDs, LUT entries, thresholds and saved scientific state.
- [ ] Count analysis calls and require zero recomputations from appearance-only changes; any resize/redraw is distinguished from analysis.
- [ ] Under a fixed export/rendering policy, deterministic exports retain identical data/colors; assert bytes or canonical pixel/data payload as appropriate, excluding explicitly recorded metadata.
- [ ] Test at least one chart and one scientific viewport fixture; document that these probes are bounded evidence rather than general scientific validation.

**Required evidence:** Before/after data and export hashes, LUT/state assertions, analysis-call counters and native viewport captures.

#### [D4] Measure performance and lifecycle under the declared workload

**Mote:** `bd-01M4Y1ACQ4GE1MJ3AEZ7KVPZBF` · **Priority:** P1 · **Requires:** `C4` (`bd-01M4Y1ACBGF18WJCQ139EAN4VA`), `C5` (`bd-01M4Y1ACD68Q4G6XW8YW9BCSBK`), `D3` (`bd-01M4Y1ACNFSYM16RGVY4EH6F14`)

**Outcome:** Verify that the design remains usable at scale and releases owned resources.

**Owned scope:** performance harness; deterministic workload; lifecycle counters; evidence reports

**Acceptance criteria:**

- [ ] Freeze the workload before measurement: 100000 virtualized rows, eight panes, three floats and a fixed synthetic viewport; name CPU/GPU/RAM/OS/refresh/renderer and record baseline/candidate revisions.
- [ ] Document what the timing instrumentation measures. Run three 120-second trials after at least 20 seconds warmup for scrolling, docking and editing; report distributions and raw traces, not a single average.
- [ ] Meet the proposed p95 UI pulse-processing budget of 16.7 ms on the designated 60 Hz reference machine; a failure is reported and repaired or explicitly revises the support/target decision before acceptance, never hidden by changing the workload.
- [ ] Over 100 open/popout/dock-back/close and appearance cycles, stage/subscription/pane lifecycle counts return to their defined baselines; disposed UI has no retained owned handlers. Record retained-heap analysis separately from allocation rate.
- [ ] Scientific recomputation count remains zero for appearance-only operations; preserve raw exits/traces and distinguish native-renderer from software-pipeline results.

**Required evidence:** Reproducible benchmark command, hardware/workload manifest, three trial reports, raw traces and lifecycle counter assertions.

#### [D5] Integrate and qualify the chosen real application

**Mote:** `bd-01M4Y1ACRVH3Z9MDQS9CZDTCE6` · **Priority:** P1 · **Requires:** `D0` (`bd-01M4Y1ACGJ8TKMKNM4AS7H1S0W`), `C4` (`bd-01M4Y1ACBGF18WJCQ139EAN4VA`), `C5` (`bd-01M4Y1ACD68Q4G6XW8YW9BCSBK`), `D3` (`bd-01M4Y1ACNFSYM16RGVY4EH6F14`)

**Outcome:** Demonstrate the design in a real scientific workflow without conflating theme and runtime migration.

**Owned scope:** chosen application's explicitly owned integration paths; consumer receipt in Meridian

**Acceptance criteria:**

- [ ] Implement under the chosen application's own issue/reservations and exact pinned Meridian/ScalaDock candidates; keep changes limited to authorized theme/composition integration.
- [ ] Exercise a real representative viewport plus table/editing/validation and save/restore/popout workflows in all four variants.
- [ ] Run the application's existing rendering and scientific invariant gates on the chosen runtime; record main/float focus, overlays, scientific state/export and zero appearance-triggered analysis.
- [ ] Confirm an application-level rollback to its previous appearance/dependency pins and preserve existing saved-layout/domain data.
- [ ] Record provider/consumer SHA and artifact parity and owner review. A passing sampler alone cannot close this task.

**Required evidence:** Application-owned implementation/review receipt, exact pins, native captures and relevant existing application gate logs.

#### [D6] Qualify artifacts, Java/Scala consumers and adoption documentation

**Mote:** `bd-01M4Y1ACTQ9GW526Q0KBPRGWQJ` · **Priority:** P1 · **Requires:** `C4` (`bd-01M4Y1ACBGF18WJCQ139EAN4VA`), `C5` (`bd-01M4Y1ACD68Q4G6XW8YW9BCSBK`)

**Outcome:** Make the modules independently consumable with honest support and provenance.

**Owned scope:** packaging; clean consumer fixtures; README/API/style docs; notices

**Acceptance criteria:**

- [ ] Produce exact local candidate artifacts and inspect POM/JAR dependency closure; theme-only Java consumer does not acquire ScalaDock/ScalaFX, adapter Scala consumer obtains only its declared closure.
- [ ] Build/run consumers outside sibling checkouts in an isolated local repository; no SNAPSHOT, hidden local-composite dependency or development path is required.
- [ ] Include all generated resources, source/license/font/icon notices; verify CSS URLs resolve from packaged JARs, including popup/float construction and shaded/module-path arrangements actually claimed.
- [ ] Document installation, theme switching, density/text scale, lifetime/disposal, semantic style API, adapter contract, runtime/support matrix and application migration boundaries with compiled examples.
- [ ] State pre-release maturity, tested versions and limitations accurately; producing local candidate artifacts does not publish them.

**Required evidence:** POM/JAR inspection, clean Java and Scala consumer logs, compiled docs examples, license/resource inventory and exact artifact digests.

#### [D7] Independently review the complete v0.1 acceptance evidence

**Mote:** `bd-01M4Y1ACWH92CRN4ND2RQGCTD2` · **Priority:** P1 · **Requires:** `D1` (`bd-01M4Y1ACJ7SAJ4FQDAWP676DX5`), `D2` (`bd-01M4Y1ACKVCC0J51AFVVNVEDHA`), `D3` (`bd-01M4Y1ACNFSYM16RGVY4EH6F14`), `D4` (`bd-01M4Y1ACQ4GE1MJ3AEZ7KVPZBF`), `D5` (`bd-01M4Y1ACRVH3Z9MDQS9CZDTCE6`), `D6` (`bd-01M4Y1ACTQ9GW526Q0KBPRGWQJ`)

**Outcome:** Make the final completion decision from reproducible evidence.

**Owned scope:** docs/evidence/final-acceptance; support/readiness documentation

**Acceptance criteria:**

- [ ] A reviewer distinct from implementation owners reviews the exact candidate and samples independent reruns of native, consumer, scientific-isolation and lifecycle evidence.
- [ ] Every acceptance row in PLAN.md is mapped to a passing receipt or an explicitly non-blocking declared limitation; no required missing platform/run or open major defect is hidden.
- [ ] Reconcile artifact hashes, provider/application pins, local results and any hosted results; distinguish generated/configured checks from executed checks.
- [ ] Readme/support claims match evidence; publication, signing/release credentials and deployment remain outside this acceptance action.
- [ ] Close this gate only on a recorded accepted verdict; otherwise record the exact failing obligation and return it to its owning task.

**Required evidence:** SHA-bound independent verdict and evidence coverage map; this gate unblocks E3 closure, then the delivery umbrella.

## Verification commands and evidence

The following commands already exist in ScalaDock; they are future implementation checks, not checks run in this review:

```sh
# From ../scaladock, before a heavy build:
df -h /System/Volumes/Data

# Focused integration regression coverage:
sbt 'fx/testOnly scaladock.fx.ThemeSuite scaladock.fx.PopoutSuite scaladock.fx.ChromeSuite scaladock.fx.KeyboardSuite scaladock.fx.DragDropSuite scaladock.fx.DividerSuite scaladock.fx.MinimizeFxSuite scaladock.fx.PerspectivesSuite scaladock.fx.CloseAdmissionSuite scaladock.fx.TabMenuSuite'

# Required repository checks once provider changes are ready:
sbt scalafmtCheckAll compile test

# Real native demo:
sbt demo/run

# Existing macOS screenshot rig; output is an evidence directory:
sbt 'demo/runMain scaladock.demo.Shots target/meridian-baseline 01 02 04 05 07 09 11 14'
```

On Linux use the repository's existing `xvfb-run --auto-servernum` wrapper for UI tests. A software-rendered virtual-display pass does not qualify the native renderer.

The screenshot rig currently captures the ScalaDock demo, not Meridian. Adapt its scenario approach in `meridian-sampler`; do not label an unchanged demo capture as Meridian evidence. Add the JavaFX 27 runtime lane and generator/contrast checks when the Meridian build exists, documenting their actual commands then.

Extend existing suites with behavior assertions rather than stylesheet-string comparisons: computed paints, geometry/hit-tests, pane identity, focus owner, window count, overlay appearance, and disposal. Retain the JDK 22 / JavaFX 24 provider-floor gate alongside the new consumer lane.

## Scope, risks, and next action

**In scope:** the coherent native design system, optional workbench compositions, explicit ScalaDock integration, and the evidence needed to claim the declared compatibility.

**Outside the first implementation:** replacement docking architecture, a scientific visualization library, application-wide runtime migrations, custom window-manager behavior, JavaFX 24 Meridian support, a perceptual palette editor, and public release.

**Main risks:** native typography differs from the browser; nested author styles and popup roots override/inhibit intended appearance; density metrics become stale in drag handling; custom chrome lacks full accessible behavior; a runtime upgrade changes scientific rendering. Each has an early gate above.

Before code changes, inspect the relevant repository's live Mote issues/reservations and use package-owned tasks. MeridianFX now has its own local Mote store; generic provider work is registered in ScalaDock's existing store. This document is the design/acceptance map, while Mote owns status, dependencies and claims. The package directory is not yet a Git repository; these are local tracker records, not published or remote-synchronized work.

**Next action:** E0 and A1–A3 are accepted locally (see docs/evidence/a1, a2 and a3). S1 (bd-01M4Y1AD18TGSE7FQBMXQHBPFY) is accepted locally in ScalaDock: explicit custom appearance metadata, both runtime lanes, compatibility disposition and four macOS native window captures; see [provider evidence](../scaladock/docs/evidence/meridian-s1/README.md). S2 (bd-01M4Y1AD2TV1YDNQ20NKE2PF6M) is accepted locally: atomic live metrics, preferred minima, gesture cancellation and identity/state preservation, with 135 tests on each runtime lane and six timed native phases; see [S2 evidence](../scaladock/docs/evidence/meridian-s2/README.md). S3 (bd-01M4Y1AD4DZVMZNHBXTA4VC80V) is accepted locally: explicit popup dismissal/reopen, public root styling and scoped presentation disposal, with 142 tests on each runtime lane and four timed native phases (14 scene captures); see [S3 evidence](../scaladock/docs/evidence/meridian-s3/README.md). Next is provider S4 (bd-01M4Y1AD636FK3M3DJNGTB6W9Y), keyboard and accessible actions for custom docking chrome. B0 remains blocked until a reviewed provider candidate is available; B1 requires B0 and A3. D0 remains independently ready for pilot/runtime selection. The first docked visual gate remains B3: four workspace variants, detached inspector and keyboard-visible focus.
