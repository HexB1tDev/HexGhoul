#!/bin/bash
# Eto's speed blitz choreography (6.1.0): solves the arms of every strike key onto the fist and elbow targets in
# tools/EtoPoseDesign.java and writes src/main/java/.../client/animation/EtoStrikePoses.java. Run `python3 build.py` once first.
# Usage: tools/eto-pose-design.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLASSES="$ROOT/build/eto-design-classes"
rm -rf "$CLASSES"
mkdir -p "$CLASSES"
javac -nowarn -encoding UTF-8 --release 17 -cp "$ROOT/build/classes:$ROOT/build/stubs:$ROOT/build/base.jar" -d "$CLASSES" \
  "$ROOT"/src/main/java/com/hex/kagune/control/eto/*.java "$ROOT"/src/main/java/com/hex/kagune/control/ayato/BladeCarry.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/RigPose.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/BodyFrame.java "$ROOT"/tools/EtoPoseDesign.java
java -cp "$CLASSES:$ROOT/build/classes" EtoPoseDesign "$ROOT/src/main/java/com/hex/kagune/control/client/animation/EtoStrikePoses.java"
