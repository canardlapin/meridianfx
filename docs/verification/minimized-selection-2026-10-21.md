# Minimized tab presentation

Consumer tracking: ephys4s Mote
`ephys4s-provider-meridianfx-minimized-selection`. The initial provider revision
is 3c3956de1bc3e84a7a54e2b9f90838c3f23a5281, based on canonical
bcbe642aeba4b560ccee6432741c2e5fa26fbc82; ScalaDock is exactly
4a7a61bb397a68d5048f809a6ea1c30ce8b45223.

The real ephys4s workspace exposed an opaque selection marker on a minimized
Points tab after its global theme button received focus. The tab was not
focused, but its group remained active. Computed paints were 0x7b8595ff and
0xffffffff. The adapter's more specific active/inactive selected-tab recipes
overrode ScalaDock's minimized-tab transparency. Moving focus to another dock
group removed the marker; looking only at that state missed the defect.
The old focused recipe also included the content selection marker alongside
the focus ring.

The owning adapter now explicitly styles minimized tabs in active/inactive
group/window states. Unfocused restore tabs remain transparent. Focused restore
tabs retain an inset focus ring on the header surface, with no selected-pane
marker and zero geometry-changing border. Normal selected tabs retain their
existing marker. No consumer stylesheet override is needed.

`MinimizedSelectionProbe` uses an actual JavaFX application, pane factories and
Meridian binding. It checks twelve combinations of light/dark,
compact/comfortable and text scale 1/1.5/2. Each covers active-group/global
focus, active-group/tab focus and another active group, with both active and
inactive window CSS states: 72 computed-paint observations. Actual node focus
is checked; inactive-window CSS simulation is not OS window automation.
Header geometry, native Space restoration, pane layout and magenta scientific
swatches are preserved. Every sample is written before its assertions.

The unchanged adapter fails the first sample with the two opaque paints above.
The fixed adapter passes all 72 observations on macOS 12 x86_64, JDK
25.0.4.1+1 and OpenJFX 27+30. Formatting, provider resolution, B0 and B1 pass;
B1 retains its 100 appearance changes, 20 rebindings and closed-window checks.
Exact logs and sample bytes are retained by ephys4s in
docs/verification/native-tranche-2026-10-21/native-docking:
provider-repro-2.log, provider-repro-2, provider-complete-1.log,
provider-check-1, provider-b0-1 and provider-b1-1.

Run through the pinned JDK/sbt toolchain with a new output directory:

```sh
sbt scalafmtCheckAll scaladock/providerResolutionReport \
  'scaladock/Test/runMain meridian.scaladock.MinimizedSelectionProbe NEW_DIRECTORY'
```

The native-adapter PR workflow now runs and preserves this probe too. Hosted CI
and other supported platforms have not been executed locally. Publication,
canonical integration and exact downstream pin/requalification remain required.

The provider's local Mote store remains unavailable because its pre-existing
fenced operation requires the shared Mote publisher; its journal was preserved.
See auxiliary-typography-2026-10-21.md for the exact operation and retained
diagnostic. This owner-side record and consumer Mote track the work until an
upstream issue can be created with publication authorization.
