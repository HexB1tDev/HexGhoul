#!/bin/bash
# Ayato's moves off-game: the posed body (exact PlayerAnimator + bendy-lib, tools/PoseRender.java) with the wings
# (WingRig) and the RC fragments, in the wing previewer's translucent rasterizer. Run `python3 build.py` once first.
# Usage: tools/art-preview.sh <out-dir> [sheet ...]   (sheets: aim shot moves crystal spikes vortex gatling kakuja kakuja-aim
#        rebirth-thralls kakuja-kit kakuja-motion kakuja-final kakuja-final-kit kaneki-mask wounds centipede centipede-grab centipede-mask kaneki-kakuja centipede-game kakuja-burst kakugan-lean tendril-guard crystal-thorns wounds-anatomy steel-blade)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLASSES="$ROOT/build/art-classes"
rm -rf "$CLASSES"
mkdir -p "$CLASSES"
javac -nowarn -encoding UTF-8 --release 17 -cp "$ROOT/build/classes:$ROOT/build/stubs:$ROOT/build/base.jar" -d "$CLASSES" \
  "$ROOT"/src/main/java/com/hex/kagune/control/*.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/ayato/*.java "$ROOT"/src/main/java/com/hex/kagune/control/kakugan/*.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/wound/*.java "$ROOT"/src/main/java/com/hex/kagune/control/king/*.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/RigPose.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/IdlePose.java \
  "$ROOT"/src/main/java/com/hex/kagune/control/client/animation/WarPose.java \
  "$ROOT"/tools/PoseRender.java "$ROOT"/tools/WingPreview.java "$ROOT"/tools/ArtPreview.java
java -Djava.awt.headless=true -cp "$CLASSES:$ROOT/build/classes:$ROOT/build/base.jar" ArtPreview "$@"
