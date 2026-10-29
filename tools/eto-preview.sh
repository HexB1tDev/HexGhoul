#!/bin/bash
# Eto's base form off-game (6.1.0): the posed body with her steam, flesh and moves (tools/EtoPreview.java), in the art
# previewer's rasterizer. Run `python3 build.py` once first. Usage: tools/eto-preview.sh <out-dir> [sheet ...]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLASSES="$ROOT/build/eto-art-classes"
rm -rf "$CLASSES"
mkdir -p "$CLASSES"
javac -nowarn -encoding UTF-8 --release 17 -cp "$ROOT/build/classes:$ROOT/build/stubs:$ROOT/build/base.jar" -d "$CLASSES" \
  "$ROOT"/src/main/java/com/hex/kagune/control/*.java "$ROOT"/src/main/java/com/hex/kagune/control/eto/*.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/ayato/*.java "$ROOT"/src/main/java/com/hex/kagune/control/kakugan/*.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/wound/*.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/RigPose.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/IdlePose.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/EtoIdle.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/BodyFrame.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/EtoMotions.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/EtoOwnLeg.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/EtoOwnArm.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/Remains.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/WatchPose.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/ArmPose.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/EtoStrikePoses.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/EtoFleshPose.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/OwlPose.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/OwlMotions.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/OwlArms.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/OwlLancePose.java \
  "$ROOT"/tools/PoseRender.java "$ROOT"/tools/WingPreview.java "$ROOT"/tools/ArtPreview.java "$ROOT"/tools/EtoPreview.java
java -Djava.awt.headless=true -cp "$CLASSES:$ROOT/build/classes:$ROOT/build/base.jar" EtoPreview "$@"
