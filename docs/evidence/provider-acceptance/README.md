# B0 — accepted ScalaDock provider on JavaFX 27

Accepted locally on 2026-10-19 (America/Toronto; final run ended
2026-10-20T00:12:25Z). The executable B0 consumer gate passes. This enables B1;
the production Meridian–ScalaDock appearance adapter is still separate work.

## Frozen inputs and provider review

- Accepted provider: **`4a7a61bb397a68d5048f809a6ea1c30ce8b45223`**, merged
  [ScalaDock PR #3](https://github.com/canardlapin/scaladock/pull/3).
- Git tree: `9a123756fa1707cade0149a627dcaaa1f9617faf`, identical to tested PR head
  `3e84bf30be7b2e5f4e2ec1a0f75b32ae1a340535`.
- Provider S6 independently accepted source
  `87f900e9bdb1cce08872b363d4fe20a04c458b29`; the subsequent focus-recovery fix
  `39fccfeaf920ec9e6522351d9ef12c5d1bd7f98b` received its own narrow review.
  Retained verbatim copies from the pinned provider checkout:
  [S6 review](provider-review/independent-review.md),
  [CI fix and review](provider-review/ci-followup.md), and its
  [manifest](provider-review/ci-followup/verification.json).
  Paths inside those historical copies refer to the provider repository.
- [Merged-head CI](https://github.com/canardlapin/scaladock/actions/runs/39344445990)
  completed successfully on all four jobs: library floor, current runtime,
  macOS build and Ubuntu build. See [run](merged-ci.json) and
  [jobs](merged-ci-jobs.json). Provider floor remains JDK 22 / JavaFX 24.0.1;
  Meridian remains JDK 25 / JavaFX 27. The provider follow-up retains 174 tests
  per hosted lane; these are provider results, not Meridian test counts.
- Consumer base at verification: `1ac736b28b5f955fcf042daefde18db9eda6e776`.
  The then-uncommitted B0 changes are identified by the 28-file source map in
  [verification.json](verification.json), digest
  `f4fc1bd6b168e1e6615747f20e63d303692490066e15b09d68677e3c51d24100`.
  Runtime JAR digests, executing provider class hashes and JavaFX module
  identities are retained; no consumer commit is invented.

## Default resolution experiment

The same `scaladock/providerResolutionReport` task observed:

| Configured SHA | Observed SHA | sbt staging checkout |
| --- | --- | --- |
| `628c46fce92c4d31b0b08459ac0f18777b5ba527` | same | `e5369b89f9d6f9ffec6b/scaladock` |
| `4a7a61bb397a68d5048f809a6ea1c30ce8b45223` | same | `ae58878124cf8bf90b01/scaladock` |

Both use the remote full-SHA Git URI, have empty local-override and tracked-diff
fields, and resolve outside `../scaladock`. The new checkout was fetched by
sbt. [Baseline](baseline-resolution.tsv), [accepted](accepted-resolution.tsv)
and command logs preserve this experiment. Executing `Dock.class` and
`LayoutState.class` hashes match the new checkout's compiled classes. Explicit
development overrides remain available but cannot satisfy this acceptance gate.

## Consumer checks

The probe lives in `meridian.scaladock`, using only public ScalaDock APIs. It
forwards Meridian preferences explicitly as fixture wiring. Four panes retain
their identities and edited text across presentation changes, popout and
save/dock-back/load; exactly four are built and exactly four disposed.

| Phase | Scene scheme | Requested / rendered header | Reduced motion | Floating |
| --- | --- | --- | --- | --- |
| Dark comfortable | DARK | 32 / 32 | false | no |
| Light compact | LIGHT | 28 / 28 | true | no |
| Dark compact | DARK | 28 / 28 | true | yes |
| Light comfortable, text scale 1.25, restored | LIGHT | 40 / 40 | false | yes |

Additional assertions cover custom popup palette, dismissal before reopening
after presentation changes, F6/Shift+F6, Ctrl+Tab/Ctrl+Shift+Tab, maximize/restore,
four Shift+F10 menus, content-consumed key ownership, System motion policy,
reference-counted external stylesheet leases, unchanged layout during
presentation updates, and idempotent disposal closing owned popups/windows.
Ten owned-scene PNGs and [native result](native-final/result.json) are retained.
Rendered header bounds, not just requested settings, are checked.

The final commands and real exits are in the `.log.meta.json` sidecars:

```sh
tools/sbt 'scaladock/Test/scalafmtCheck' 'scalafmtSbtCheck' \
  'scaladock/providerResolutionReport' \
  'scaladock/Test/runMain meridian.scaladock.ProviderAcceptanceProbe docs/evidence/provider-acceptance/native-final' \
  'scaladock/writeFoundationReport'
python3 tools/generate_tokens.py --check
python3 tools/check_tokens.py
python3 tools/build_themes.py --check
python3 tools/verify_foundation.py
python3 -m unittest discover -s tools/tests -v
python3 tools/verify_provider.py --capture docs/evidence/provider-acceptance/native-final \
  --log docs/evidence/provider-acceptance/consumer-final.log
python3 tools/verify_provider.py --check docs/evidence/provider-acceptance/verification.json
```

All final commands exited 0: scoped native compilation/run, formatting, generated
outputs, 300 required token contrasts, immutable foundation inputs and **35
Python tests**. Use a new capture directory when rerunning; the probe refuses
to overwrite evidence. Copy the fresh target provider report to
`accepted-resolution.tsv` before issuing a new verification receipt. `--check`
only validates retained hashes; it does not rerun JavaFX.
Run that historical hash check at the B0 acceptance commit; B1 subsequently
changes the appearance service and adapter package documentation. The B1 receipt
identifies the later source state without rewriting this B0 evidence.

## Adverse runs and limits

`consumer-01.log` and `consumer-02.log` retain two exit-1 fixture failures.
The first assumed TextField-targeted Ctrl+Tab would select the next pane; the
second incorrectly assumed it would preserve the same pane. TextField traversal
was not a valid oracle for the dock's bubbling-key contract. The final fixture
uses an explicitly consuming content handler followed by an unconsuming pane
root, asserting both ownership and actual navigation. `consumer-03.log` passed;
`consumer-final.log` reran after formatting and also passed. No provider source
was changed to obtain these results.

This is a macOS/aarch64, JDK 25.0.1 / JavaFX 27+30 native consumer run. Key events
are dispatched through JavaFX, and captures are scene snapshots; neither is OS
input automation or full screen-reader qualification. The independently reviewed
upstream macOS AXEnabled=false limitation remains. B0 checks motion policy
forwarding; detailed animation/drag continuity remains provider S5 evidence.
Scala 3.7.4 emits its known deprecated `sun.misc.Unsafe` warning on JDK 25.

Disk availability was 14 GiB, below the repository's full-build threshold. Only
the scoped consumer and its dependencies were compiled; no full aggregate build
is claimed. B1, docked-workspace acceptance B2/B3, application pilot D0/D5, and
packaged/clean-consumer qualification D6 remain separate gates. This record
describes the B0 verification state before its subsequent commit and push;
it does not claim artifact publication or completion of those later gates.
