#!/bin/bash
# Fast look iteration for the kagune visuals: recompiles only the pure wing and kakugan packages (control.ukaku, control.kakugan) over
# the last full build, then renders preview sheets from the exact quads the game draws (tools/WingPreview.java).
# Run `python3 build.py` once first (it produces build/classes).
#
# Usage: tools/preview.sh <out-dir> [sheet ...]
#   sheets: forms actions closeup manifest (default set), part-flame part-flame-close part-flame-3d part-crystal
#           part-crystal-3d part-crystal-views part-onewing part-root kakugan
#   The part-* sheets show reference frames next to the renders; they are read from $REF_DIR (default
#   docs/reference, which is git-ignored: anime and manga frames are not committed).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLASSES="$ROOT/build/preview-classes"
rm -rf "$CLASSES"
mkdir -p "$CLASSES"
javac -nowarn -encoding UTF-8 --release 17 -cp "$ROOT/build/classes" -d "$CLASSES" "$ROOT"/src/main/java/com/hex/kagune/control/ukaku/*.java "$ROOT"/src/main/java/com/hex/kagune/control/kakugan/*.java
java -Djava.awt.headless=true -Dref="${REF_DIR:-$ROOT/docs/reference}" -cp "$CLASSES:$ROOT/build/classes" "$ROOT/tools/WingPreview.java" "$@"
