#!/bin/bash
# Pose sheets rendered with the game's exact PlayerAnimator + bendy-lib transforms (tools/PoseRender.java).
# Needs only a JDK. Usage: tools/pose-preview.sh <out-dir> [sheet ...]   (sheets: idle)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLASSES="$ROOT/build/pose-classes"
rm -rf "$CLASSES"
mkdir -p "$CLASSES"
javac -nowarn -encoding UTF-8 --release 17 -d "$CLASSES" \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/RigPose.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/IdlePose.java \
  "$ROOT"/tools/PoseRender.java "$ROOT"/tools/PosePreview.java "$ROOT"/tools/ArmSolve.java
java -Djava.awt.headless=true -Dnoref="${NOREF:-false}" -Dref="${REF_DIR:-$ROOT/docs/reference}" -cp "$CLASSES" PosePreview "$@"
