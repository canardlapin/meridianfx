# MeridianFX

MeridianFX is a native JavaFX design system for scientific desktop applications:
quiet surfaces, readable dense controls, and consistent light/dark appearance.
It compiles AtlantaFX-derived themes, provides Java layout metrics and application
preference binding, and keeps an optional workbench and ScalaDock adapter separate.

**Maturity: pre-alpha; source-only.** This is pre-release development software.
APIs, semantics and package boundaries may change. No stable support or binary
compatibility promise should be inferred. The initial light/dark pair and
comfortable/compact densities pass a bounded native control fixture on macOS.
The complete component system, docking and application acceptance remain in
[the plan](PLAN.md).

## Smallest runnable example

Install JDK25, sbt and Python3.10+. The wrapper selects JDK25 for its child process;
set `MERIDIAN_JAVA_HOME` if it is not found automatically.

```sh
python3 tools/verify_foundation.py
tools/sbt compile foundationReport
tools/sbt 'sampler/runMain meridian.sampler.ThemeSmoke target/new-theme-smoke'
python3 tools/verify_foundation.py --classpath
```

The smoke shows native controls, menus and dialogs in four variants, writes PNGs,
computed-style and lifecycle assertions, then closes its windows. Its output
directory must be new. This is the initial theme fixture, not a docked application.

The Java API is also usable directly from Scala:

```java
import meridian.theme.MeridianAppearance;

// On the JavaFX Application Thread; keep these handles for the application lifetime:
var appearance = MeridianAppearance.install();
var registration = appearance.register(scene);
stage.setScene(scene);
stage.show();

// When the scene is retired:
registration.close();
// At application shutdown:
appearance.close();
```

Defaults follow the platform's typed scheme/motion preference, use comfortable
density and text scale1. [Theming guide](docs/THEMING.md) covers explicit preferences,
scene restoration, hidden/new scenes, disposal, [status indicators](docs/THEMING.md#status-indicators) and the [native font-weight contract](docs/THEMING.md#native-typography-and-direct-installation). The independent Java consumer
in [A3 evidence](docs/evidence/a3/README.md) uses the packaged theme without Scala,
ScalaFX or ScalaDock.

## Build and dependency boundaries

| Module | Current foundation | Remaining responsibility |
|---|---|---|
| `theme` | Four compiled user-agent themes, semantic tokens, metrics and appearance binding | Exhaustive control/context qualification |
| `workbench` | Java ViewHeader and semantic dot+label StatusIndicator; depends on theme | Navigation, inspectors and command composition |
| `scaladock` | Live appearance adapter, generated public-token palettes and native lifecycle gate | Docked-workspace and packaged-consumer qualification |
| `sampler` | Native foundation, token and theme probes | Docked fixtures and complete interaction scenarios |

Theme and workbench carry no ScalaDock, mandatory ScalaFX or Scala runtime.
The adapter uses Scala3.7.4. Its default provider source dependency is pinned to
`4a7a61bb397a68d5048f809a6ea1c30ce8b45223`; a local override requires
`-Dmeridian.scaladock.path=/absolute/checkout`. A sibling is never selected
automatically. Source-composite development is not published-artifact closure;
the full consumer gate remains D6.

[ScalaDock adapter usage](docs/SCALADOCK.md) explains binding and ownership;
[B1 evidence](docs/evidence/adapter/README.md) records native paint and lifecycle checks.

Runtime pins are JDK25, JavaFX27 and AtlantaFX3.0.0; sbt is 1.12.14.
Native artifacts are selected for macOS/Linux/Windows x64 and available macOS/Linux
ARM64 distributions. Initial qualification targets macOS/Linux; only macOS native
evidence is recorded. Linux and Windows remain unqualified. No consuming
application or ScalaDock runtime floor is upgraded by this work.

## Verification and generation

Ordinary builds verify the checked-in theme's source/resource hashes with Python.
Node/Sass are needed only to regenerate or independently recompile themes.

```sh
tools/sbt scalafmtSbtCheck scalafmtCheckAll clean compile foundationReport theme/packageBin
python3 tools/verify_foundation.py --classpath
python3 tools/generate_tokens.py --check
python3 tools/check_tokens.py
python3 -m unittest discover -s tools/tests -p test_tokens.py -v
```

For consumer status/typography regressions (qualified macOS System face profile):

```sh
tools/sbt 'sampler/runMain meridian.sampler.ConsumerContractProbe target/new-consumer-contract'
```

This checks standalone/registered themes, both densities/schemes and enlarged text.
[Consumer-fix evidence](docs/evidence/consumer-contracts/README.md) records the baseline
and bounded results for upstream issues 1–3.

For the full generator suite and theme compilation:

```sh
npm ci --prefix theme-build --ignore-scripts --no-audit --no-fund
python3 tools/build_themes.py --check
python3 -m unittest discover -s tools/tests -v
```

The [token guide](tokens/README.md) describes the authoritative JSON source and
generated resources; [theme build/API guide](docs/THEMING.md) describes the bounded
SASS delta and lifecycle. The [CI workflow](.github/workflows/tokens.yml) detects
edited/stale resources and independently recompiles them; hosted CI has not run.

`foundationReport` records resolved dependencies and runtime classpaths under each
module's `target/foundation`. The verifier checks upstream/reference hashes, build
pins and dependency isolation. The exact original AtlantaFX styling sources and
MIT license remain in `upstream/atlantafx`, with provenance in
[upstream/lock.json](upstream/lock.json). [NOTICE](NOTICE) records font/icon policy:
system fonts, no new bundled font/icon asset. Scientific colors and exports remain
application-owned.

[PLAN.md](PLAN.md) contains acceptance criteria and Mote IDs. Mote owns status and
claims. Evidence for [A1](docs/evidence/a1/README.md), [A2](docs/evidence/a2/README.md)
and [A3](docs/evidence/a3/README.md) distinguishes local checks from broader
accessibility, scientific, platform and application compatibility. Provider qualification and Meridian adapter acceptance remain separate gates.

## License

[MIT](LICENSE). Third-party materials retain their original notices.
