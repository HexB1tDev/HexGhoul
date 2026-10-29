#!/bin/bash
# Repaints the Furuta DNA kakuja's hide (7.0.0) from FurutaSkin's paint into the mod's textures/entity (FurutaTextures).
# Run `python3 build.py` once first (or at least its patch and stubs steps).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLASSES="$ROOT/build/furuta-texture-classes"
rm -rf "$CLASSES"
mkdir -p "$CLASSES"
javac -nowarn -encoding UTF-8 --release 17 -proc:none -cp "$ROOT/build/stubs:$ROOT/build/base.jar" -sourcepath "$ROOT/src/main/java" -d "$CLASSES" \
  "$ROOT"/tools/FurutaTextures.java
java -Djava.awt.headless=true -cp "$CLASSES" FurutaTextures "$ROOT/src/main/resources/assets/hexkagune/textures/entity"
