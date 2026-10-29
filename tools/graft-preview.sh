#!/bin/bash
# Graft DNA off-game (7.0.0): the game's own rig and meshes on the posed body (tools/PoseRender.java) in the wing previewer's
# rasterizer. Run `python3 build.py` once first (or at least its patch and stubs steps).
# Usage: tools/graft-preview.sh <out-dir> [sheet ...]   (sheets: stages details kakuja moves deploy npc npc-skins npc-flips npc-fight ...)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLASSES="$ROOT/build/graft-preview-classes"
rm -rf "$CLASSES"
mkdir -p "$CLASSES"
javac -nowarn -encoding UTF-8 --release 17 -proc:none -cp "$ROOT/build/stubs:$ROOT/build/base.jar" -sourcepath "$ROOT/src/main/java:$ROOT/tools" \
  -d "$CLASSES" "$ROOT"/tools/GraftPreview.java "$ROOT"/tools/ArtPreview.java "$ROOT"/tools/PoseRender.java "$ROOT"/tools/WingPreview.java
java -Djava.awt.headless=true -Dgraft.root="$ROOT" -cp "$CLASSES:$ROOT/build/stubs:$ROOT/build/base.jar" GraftPreview "$@"
