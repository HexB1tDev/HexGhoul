**Built in 3.7.3** (`src/main/java/com/hex/kagune/control/client/animation/Reattach.java`, `bikaku/PotentialRules.java`): this folder is the
prototype the preview was made from, kept for the record (its `Part` enum is now `PotentialRules.Part`).

# Potential Unlock, tearing-off and strands: the prototype the preview was made from (not built into the mod yet)

- `Reattach.java` (package `com.hex.kagune.control.client.animation`, pure): a part torn off whole (arm at the shoulder, leg at the hip, the
  head at the neck) made of Pale's revive pieces (`Remains`), thrown (`thrown`), lying, hauled back (`place`), its strands (`strands`,
  `strandWidth`) and the tissue (`tissue`); timeline `REACH_AT` 14, `GRIP_AT` 40, `HAUL_AT` 48, `HOME_AT` 88, `WHOLE` 100 ticks.
- `PotentialPreview.java` (default package, with `tools/PoseRender.java`, `WingPreview.java`, `ArtPreview.java`, `PalePreview.java`):
  the sheet `docs/handoff/potential-reattach-preview.png`, sent to the owner before building.

Compile and run (after a `python3 tools/quick_check.py`):

    P=docs/handoff/potential-prototype; mkdir -p build/potential
    javac -nowarn -encoding UTF-8 --release 17 -cp build/quick/classes:build/quick/stubs:build/quick/base.jar -d build/potential \
      tools/PoseRender.java tools/WingPreview.java tools/ArtPreview.java tools/PalePreview.java $P/Reattach.java $P/PotentialPreview.java
    java -Djava.awt.headless=true -cp build/potential:build/quick/classes:build/quick/base.jar PotentialPreview <out-dir>

To build it: move `Reattach.java` into `src/main/java/com/hex/kagune/control/client/animation/`, the sheet into a tool, then the server
(`server/BikakuPotential`), the sync and the drawing as `docs/handoff/next-ai-3.6.6.md` plans; wait for the owner's word on the preview.
