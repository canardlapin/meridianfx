# Dock auxiliary typography

The ephys4s workspace exposed an 11 px visible overflow count with MeridianFX
bcbe642aeba4b560ccee6432741c2e5fa26fbc82 / ScalaDock 4a7a61b / OpenJFX 27+30.
ScalaDock's default stylesheet sets overflow labels and maximized-group badges
to 11 px. The adapter previously assigned the Meridian typography scale to
titles, menus and tooltips, leaving these two controls at the provider default.

MeridianDock now assigns both controls the scaled metadata size (12 px at scale
1). The fix is in the owning adapter; ephys4s adds no stylesheet override.
AuxiliaryTypographyProbe creates actual dock overflow and maximized-group
counts, reads rendered label fonts, checks layout identity on appearance
changes and preserves a scientific swatch. It covers the Cartesian product of
light/dark, compact/comfortable and text scales 1, 1.5 and 2.

Run with the repository JDK 25 toolchain:

```sh
sbt 'scaladock/Test/runMain meridian.scaladock.AuxiliaryTypographyProbe NEW_DIRECTORY'
```

All twelve combinations passed on macOS x86_64 / OpenJFX 27+30. The complete
provider B0 acceptance and B1 adapter probes also passed, including 100 appearance
changes, 20 rebindings and zero retained windows/subscriptions/registrations.
Main and test formatting checks passed. The ephys4s native downstream test passed
at 1440x900 and 1024x720, light and dark, using an explicit development override;
its runtime/provider admission remains pending a canonical source pin. Complete
logs are retained by ephys4s under native-tranche-2026-10-21/providers:
meridian-complete-1.log, meridian-aux-3/auxiliary-typography.json and native-9.log.
The new native-adapter workflow repeats these provider probes on macOS for PRs.
That hosted workflow has not been executed locally.

Tracking: ephys4s Mote `ephys4s-provider-meridianfx-auxiliary-type` blocks its
MeridianFX admission. This checkout's Mote store could not accept a local record:
it rejects pre-existing operation
20261019T222347.497518Z-p37812-c0000-r2424-ha6cd28.json with “fenced stores require
the shared Mote publisher.” Its journal was left intact. The consumer preserves
that diagnostic in providers/meridian-tracker-unavailable.log alongside the
native-tranche-2026-10-21 qualification evidence. This document retains the
provider-side reproduction and acceptance pending access to its tracker.

Canonical integration and downstream requalification remain required before
closing the consumer gate. Other platforms remain skipped here.
