#!/usr/bin/env python3
"""4.1.48: an isometric picture of one structure template as the game gets it (data/hexkagune/structures/<name>.nbt), on a stretch of
grass at its floor's level, through city_render's renderer (seen from the south-east, lit from above).

    python3 tools/structure_render.py <name> out.png [--scale N] [--turn 0..3] [--roof Y] [--cut south|east] [--people]

--turn turns it a quarter at a time as the game does before it is drawn (turn 2 shows its north and west sides); --roof Y leaves out
everything from its layer Y up (to see inside); --cut south leaves out its southern half (east: its eastern half); --people marks where its
people stand (and draws each as a two-block column in its colour).
"""
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import city_render as C  # noqa: E402
import library_structure as L  # noqa: E402

DATA = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "data" / "hexkagune" / "structures"


def load(name):
    root = L.read(DATA / (name + ".nbt"))
    root = root.value if isinstance(root, L.Tag) else root
    w, h, d = (C._v(v) for v in C._v(root["size"]))
    names, pal = ["minecraft:air"], []
    for p in C._v(root["palette"]):
        n = C._v(p["Name"])
        if n == "minecraft:jigsaw":
            n = "minecraft:dirt_path"  # the anchor becomes the floor once placed
        if n not in names:
            names.append(n)
        pal.append(names.index(n))
    grid = np.zeros((w, h, d), dtype=np.uint16)
    for b in C._v(root["blocks"]):
        x, y, z = (C._v(v) for v in C._v(b["pos"]))
        grid[x, y, z] = pal[C._v(b["state"])]
    people = []
    for e in C._v(root["entities"]):
        x, y, z = (C._v(v) for v in C._v(e["blockPos"]))
        people.append((C._v(C._v(e["nbt"])["id"]), x, y, z))
    return grid, names, people


def main():
    args = sys.argv[1:]
    name, out = args[0], args[1]
    scale = int(args[args.index("--scale") + 1]) if "--scale" in args else 3
    turn = int(args[args.index("--turn") + 1]) if "--turn" in args else 0
    grid, names, people = load(name)
    w, h, d = grid.shape
    if "--people" in args:
        # Each person a column two blocks tall (its feet and head), in a colour of its own kind.
        names.append("minecraft:yellow_concrete")
        mark = len(names) - 1
        for _, x, y, z in people:
            grid[x, y, z] = mark
            grid[x, y + 1, z] = mark
    grid = C.turned(grid, turn)
    if "--roof" in args:
        grid = grid.copy()
        grid[:, int(args[args.index("--roof") + 1]):, :] = 0
    if "--cut" in args:
        grid = grid.copy()
        side = args[args.index("--cut") + 1]
        if side == "south":
            grid[:, :, grid.shape[2] // 2:] = 0
        else:
            grid[grid.shape[0] // 2:, :, :] = 0
    names = list(names)
    framed = C.on_grass(grid, names, margin=8, ground=0)
    # on_grass lays grass at layer 0 under everything: the template's own floor (layer 0) stays where it has one.
    C.render(framed, names, out, scale=scale)
    print(f"wrote {out}: {name} {w}x{h}x{d}, turn {turn}, {len(people)} people")


if __name__ == "__main__":
    main()
