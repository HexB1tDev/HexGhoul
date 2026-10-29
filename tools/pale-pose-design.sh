#!/bin/bash
# Pale's speed blitz choreography (6.1.0): solves the arms of every strike key onto the fist and elbow targets in
# tools/PalePoseDesign.java and writes src/main/java/.../client/animation/PaleStrikePoses.java. Run `python3 build.py` once first.
# Usage: tools/pale-pose-design.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLASSES="$ROOT/build/pale-design-classes"
rm -rf "$CLASSES"
mkdir -p "$CLASSES"
javac -nowarn -encoding UTF-8 --release 17 -cp "$ROOT/build/classes:$ROOT/build/stubs:$ROOT/build/base.jar" -d "$CLASSES" \
  "$ROOT"/src/main/java/com/hex/kagune/control/pale/*.java "$ROOT"/src/main/java/com/hex/kagune/control/ukaku/BladeCarry.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/RigPose.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/BodyFrame.java "$ROOT"/tools/PalePoseDesign.java
java -cp "$CLASSES:$ROOT/build/classes" PalePoseDesign "$ROOT/src/main/java/com/hex/kagune/control/client/animation/PaleStrikePoses.java"
