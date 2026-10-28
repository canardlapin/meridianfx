# Meridian and ScalaDock

`meridian-scaladock` supplies a live appearance binding for the accepted ScalaDock
provider. This is pre-release development software; the API may change. The
adapter has local JavaFX 27 evidence. Docked-workspace and packaged-consumer
qualification remain separate gates.

Create the binding on the JavaFX application thread, after constructing the dock
and the application appearance service:

```scala
import javafx.scene.Scene
import meridian.theme.MeridianAppearance
import meridian.scaladock.MeridianDock

// `dock` is your scaladock.fx.Dock; pane factories and layout remain app-owned.
val appearance = MeridianAppearance.install()
val scene = new Scene(dock.view, 1000, 700)
val sceneRegistration = appearance.register(scene)
val binding = MeridianDock.bind(dock, appearance)
stage.setScene(scene)
stage.show()

// After the application's close-admission flow succeeds:
binding.close()
sceneRegistration.close()
dock.dispose()
appearance.close()
```

The application's single `MeridianAppearance` controls its visible scenes,
including new/restored floating windows. Register application scenes before
showing them so hidden scenes and root replacements follow preferences too.
Keep the scene registration under application ownership: `register` is
idempotent, so another component must not close the application's registration.
The dock binding owns one listener and holds no window registry. It observes
`presentationProperty()`, which publishes resolved preferences only after the
appearance service has updated its registered scenes.

Change appearance through `appearance.setPreferences(...)`; the existing
[theme guide](THEMING.md) describes light/dark/System, density, text scale and
motion. The adapter updates the provider synchronously on the FX thread, before
the next rendered frame. It preserves pane objects, edits, layout and scientific
palette choices. It owns the dock theme, header minimum, divider thickness and
motion policy; other `LayoutSettings` fields retain the host's values. Effective
headers may exceed the requested minimum to fit text and controls.

Applications that require a larger header or restore-strip target can use
`MeridianDock.bind(dock, appearance, headerMinHeight = 32)`. This is a finite,
nonnegative minimum in logical pixels, retained by that binding across theme,
density, text-scale and motion changes. The binding requests the maximum of this
application minimum and the generated, text-scaled density minimum; ScalaDock
then raises it if text or controls require more space. Zero (the two-argument
bind default) preserves the original generated-density policy. The application
minimum itself is not multiplied by text scale. Close and rebind to change it;
calling `dock.setSettings` directly does not change the binding's minimum.

A dock has one appearance binding. A second live bind is rejected before making
changes. Close and rebind when replacing the appearance owner. `close()` is
idempotent, removes the listeners and leaves the last presentation installed; it
is safe after `Dock.dispose()` too. Close the binding before closing the
appearance service. Temporary stage hiding is not terminal disposal.

The stylesheet sequence is provider `dock.css`, then the adapter stylesheet.
The adapter imports generated mappings and its recipes, and adds generated
semantic lookups on `.dock` so independent drag ghosts resolve them. Dynamic
font sizes are literal pixel values: JavaFX's font-size parser rejects color-like
lookups. All 15 documented provider variables, including `-dock-surface-veil`,
are mapped. Selection uses a neutral marker, keyboard focus uses the focus ring,
and popup/ghost boundaries use the essential control-border role. Decorative
split hairlines keep their separate role. Scientific colors are never assigned
by these recipes.

Theme, density and text-scale changes use the provider's dismissal policy:
open menus close and active drags cancel; reopen or restart with the new
presentation. Motion-only changes leave an active drag intact. The application
can style an external popup or inspector root with `dock.bindStylesheets(root)`
and cancel that lease when the root closes. That lease does not take ownership
of the application's scene registration.

Run the native adapter gate with a new output directory:

```sh
tools/sbt 'scaladock/providerResolutionReport' \
  'scaladock/Test/runMain meridian.scaladock.AdapterProbe /tmp/meridian-adapter-capture'
```

The gate checks computed colors on main and floating layouts, menus and real
drag ghosts; all provider tokens; 100 preference changes; 20 unbind/rebind and
float-close cycles; zero remaining subscriptions/registrations; and collection
of closed windows. It uses synthetic pane content and JavaFX event dispatch,
not OS input automation. See [B1 evidence](evidence/adapter/README.md) and the
[accepted provider](evidence/provider-acceptance/README.md) for exact revisions,
retained adverse runs and the upstream macOS accessibility limitation.
