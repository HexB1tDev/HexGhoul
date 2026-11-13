#!/bin/bash
# 4.1.78: the quick loop between full builds. Recompiles src/main/java and src/test/java from the working tree into build/fast (the
# patched base JAR made once, the stubs once or with STUBS=1) and runs the named test suites (com.hex.kagune.control.<Suite>):
#
#   tools/fast_tests.sh AgentTests NpcTests        (about 25 s for AgentTests, a minute for NpcTests)
#   STUBS=1 tools/fast_tests.sh AgentTests         (after a change under stubs/)
#   tools/fast_tests.sh                            (compile only: build/fast/classes for the previews in tools/)
#
# Not the full build: it skips the verifier, the archive and server checks. Push only after `python3 build.py` ends EXIT 0 (and then
# check GitHub: CLAUDE.md). Prints "MAIN COMPILE FAILED" and exits 2 if src/main/java does not compile. Read its whole output: a suite
# that crashes (a Minecraft class touched in a test) prints a stack trace and no "FAILED" line.
set -e
R="$(cd "$(dirname "$0")/.." && pwd)"
FB="${HEX_FAST:-$R/build/fast}"
EXP="--add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED --add-exports java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED --add-exports java.base/jdk.internal.org.objectweb.asm.tree.analysis=ALL-UNNAMED --add-exports java.base/jdk.internal.org.objectweb.asm.commons=ALL-UNNAMED"
cd "$R"
mkdir -p "$FB"
if [ ! -f "$FB/base.jar" ]; then
  mkdir -p "$FB/patch-classes"
  java -m jdk.compiler/com.sun.tools.javac.Main $EXP -nowarn -d "$FB/patch-classes" patch/KagunePatch.java patch/KaguneVerify.java \
    patch/KaguneServerCheck.java patch/KaguneGuard.java patch/KaguneRemap.java
  java $EXP -cp "$FB/patch-classes" KagunePatch input/hexkagune-1.0.0.jar "$FB/base.jar" >/dev/null
fi
if [ ! -d "$FB/stubs" ] || [ -n "$STUBS" ]; then
  rm -rf "$FB/stubs"
  javac --release 17 -nowarn -d "$FB/stubs" $(find stubs -name '*.java') 2>&1 | grep -v "^Note:\|JAVA_TOOL" || true
fi
rm -rf "$FB/classes" "$FB/test-classes"
javac --release 17 -Xlint:-options -nowarn -cp "$FB/base.jar:$FB/stubs" -d "$FB/classes" $(find src/main/java -name '*.java') 2>&1 \
  | grep -v "^Note:\|JAVA_TOOL" || true
[ -d "$FB/classes/com" ] || { echo "MAIN COMPILE FAILED"; exit 2; }
javac --release 17 -nowarn -cp "$FB/classes:$FB/base.jar:$FB/stubs" -d "$FB/test-classes" $(find src/test/java -name '*.java') 2>&1 \
  | grep -v "^Note:\|JAVA_TOOL" || true
for suite in "$@"; do
  java -ea -cp "$FB/test-classes:$FB/classes:$FB/base.jar" "com.hex.kagune.control.$suite" 2>&1 | grep -v JAVA_TOOL || true
done
