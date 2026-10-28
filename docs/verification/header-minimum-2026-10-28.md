# Application dock-header minimum

Owner-side ticket for ephys4s-provider-meridianfx-header-minimum; consumer task
ephys4s-f-ui-v2-dock-fidelity. Base canonical bcbe642aeba4b560ccee6432741c2e5fa26fbc82,
incremental parent e33fa6c46f375dcf5f91212b43da30b82f39a795; exact ScalaDock
4a7a61bb397a68d5048f809a6ea1c30ce8b45223.

The two-argument binding owns header metrics and resets any direct host request
on appearance changes. A compact host cannot retain an accepted 32 logical-pixel
header/restore target while preserving the rest of its compact preferences.
The baseline HeaderMinimumProbe manually requests 32 and changes preferences;
requested height becomes 28, effective/rendered height 29. The actual ephys4s
workspace reproduces the same 29-pixel geometry with canonical and parent builds.

The public three-argument bind retains a finite nonnegative application minimum.
It combines that minimum with the generated text-scaled density minimum;
ScalaDock still raises the effective height for text/controls. The existing
bind delegates with zero and preserves its behavior. No consumer CSS or density
change is required. SCALADOCK.md specifies units, scaling and close/rebind policy.

Acceptance is exercised by HeaderMinimumProbe: 36 minimum/theme/density/scale
combinations (0/32/48, light/dark, compact/comfortable, 1/1.5/2), actual rendered
header heights, 14px-scaled title floors, unchanged pane/layout identity and
unrelated host settings; invalid -1/NaN/+Inf/-Inf fail without mutation; repeated
close/rebind and final close leave no subscriptions or scene registrations and
balance pane creation/disposal. The existing B0, B1, auxiliary typography and
minimized-selection probes remain required. Native CI runs the new regression.

Exact commands, retained adverse runs and consumer evidence are in ephys4s
`docs/verification/native-dock-fidelity-2026-10-28`. Local qualification targets
macOS x86_64, JDK25.0.4.1/OpenJFX27; other desktop platforms and hosted CI remain
unexecuted. This is engineering qualification, not scientific/performance evidence.
Canonical publication, integration and exact downstream repin/recheck remain open.

The local owner Mote store cannot currently admit updates: its pre-existing
unadmitted operation 20261019T222347.497518Z-p37812-c0000-r2424-ha6cd28.json is
rejected by the shared-authority requirement. Its journal was preserved. This
owner-side ticket specification and the root Mote retain tracking until that
publisher is available; this file does not claim an accepted owner Mote op.
