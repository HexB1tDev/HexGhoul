#!/bin/bash
# Pale's base form off-game (6.1.0): the posed body with her steam, flesh and moves (tools/PalePreview.java), in the art
# previewer's rasterizer. Run `python3 build.py` once first. Usage: tools/pale-preview.sh <out-dir> [sheet ...]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLASSES="$ROOT/build/pale-art-classes"
rm -rf "$CLASSES"
mkdir -p "$CLASSES"
javac -nowarn -encoding UTF-8 --release 17 -cp "$ROOT/build/classes:$ROOT/build/stubs:$ROOT/build/base.jar" -d "$CLASSES" \
  "$ROOT"/src/main/java/com/hex/kagune/control/*.java "$ROOT"/src/main/java/com/hex/kagune/control/pale/*.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/ukaku/*.java "$ROOT"/src/main/java/com/hex/kagune/control/kakugan/*.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/wound/*.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/RigPose.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/IdlePose.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/PaleIdle.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/BodyFrame.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/PaleMotions.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/PaleOwnLeg.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/PaleOwnArm.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/Remains.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/WatchPose.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/ArmPose.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/PaleStrikePoses.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/PaleFleshPose.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/OwlPose.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/OwlMotions.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/OwlArms.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/OwlLancePose.java \
  "$ROOT"/tools/PoseRender.java "$ROOT"/tools/WingPreview.java "$ROOT"/tools/ArtPreview.java "$ROOT"/tools/PalePreview.java
java -Djava.awt.headless=true -cp "$CLASSES:$ROOT/build/classes:$ROOT/build/base.jar" PalePreview "$@"
