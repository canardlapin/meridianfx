"""Run the independent packaged-theme Java consumer using only observed pinned dependencies."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess
root=Path.cwd()
e=root/"docs/evidence/a3"
jar=e/"artifacts/meridian-theme-0.1.0-dev.1.jar"
jar.parent.mkdir(parents=True,exist_ok=True)
shutil.copyfile(root/"modules/theme/target"/jar.name,jar)
dependencies=[Path(p) for p in (root/"modules/theme/target/foundation/runtime-classpath.txt").read_text().splitlines()]
fx=[p for p in dependencies if p.name.endswith("-mac-aarch64.jar") and p.name.startswith("javafx-")]
atlantafx=next(p for p in dependencies if p.name=="atlantafx-base-3.0.0.jar")
assert len(fx)==3
classes=e/"java-caller-final"
classes.mkdir(exist_ok=False)
java=Path("/opt/homebrew/opt/openjdk@25/bin")
cp=os.pathsep.join(map(str,[jar,atlantafx,*fx]))
compile_cmd=[str(java/"javac"),"--release","25","-Xlint:all","-cp",cp,"-d",str(classes),str(e/"JavaCaller.java")]
subprocess.run(compile_cmd,check=True)
run_cmd=[str(java/"java"),"--module-path",os.pathsep.join(map(str,fx)),"--add-modules","javafx.controls",
         "--enable-native-access=javafx.graphics","-cp",os.pathsep.join(map(str,[classes,jar,atlantafx])),"JavaCaller"]
subprocess.run(run_cmd,check=True)
(e/"java-caller.json").write_text(json.dumps({
    "schema":"meridian.java-consumer.v1","jar_sha256":hashlib.sha256(jar.read_bytes()).hexdigest(),
    "source_sha256":hashlib.sha256((e/"JavaCaller.java").read_bytes()).hexdigest(),
    "javac_argv":compile_cmd,"java_argv":run_cmd,"exit_codes":[0,0],"scala_or_dock_on_classpath":False
},indent=2)+"\n")
