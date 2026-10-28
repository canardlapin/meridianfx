# Exact native CI JDK installation

Owner issue #10; downstream Mote ephys4s-provider-meridianfx-ci-jdk-input.
PR #9 head328eea1 initially failed run40763462427/job123081224534 before
compilation: actions/setup-java@v5 rejects25.0.4.1+1 as non-SemVer. The accepted
Temurin runtime exists, but four numeric version components cannot pass that
action's version parser. No native acceptance probe ran in the failed job.

The workflow now downloads the official exact Temurin25.0.4.1+1 archive for its
actual macOS architecture and checks a hard-coded SHA256 from the release's
asset metadata. It extracts the checksum-verified archive and validates the
actual executable's java.runtime.version25.0.4.1+1-LTS, Eclipse Adoptium vendor
and matching host architecture before exporting JAVA_HOME/PATH. It does not
replace the accepted runtime with floating major25 or relabel another patch.

Official release: https://github.com/adoptium/temurin25-binaries/releases/tag/jdk-25.0.4.1%2B1

- macOS aarch64 archive SHA256:61979887f7506a24a57439ff99adb8b3a7fc89977d9cfe3b8984f58a981b7b9d
- macOS x64 archive SHA256:e6229d9504f7922053ab31821b9e6bee8761daf7b026a3476d1a027563009880

verify-native-ci-jdk.py passes with the previously qualified local x64 runtime
and refuses an actual JDK21 executable. Hosted acceptance requires the repaired
workflow to pass the unchanged formatting, B0, B1, auxiliary, minimized-selection
and header-minimum probes on the actual exact installed runtime. The original
failed run and official metadata are retained by the ephys4s publication packet.
Other CI platforms and whole-workbench qualification remain separate gates.
The pre-existing unavailable local owner Mote publisher remains untouched;
the GitHub issue supplies current owner tracking.
