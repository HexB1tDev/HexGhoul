#!/usr/bin/env python3
"""4.1.57: the clearing template, src/main/resources/data/hexkagune/structures/clearing.nbt: a cube of air SitePrep.CLEAR (16) on a side,
every block of it written (a template's air is placed as air), laid in tiles over a place's site before the place itself
(ControlStructures.clearing): whatever the world had over the floor there, hill, trunk or leaves, is gone before the place is built.

    python3 tools/clearing_structure.py
"""
import io
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from companion_structures import write_block, write_template  # noqa: E402

SIZE = 16
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/data/hexkagune/structures/clearing.nbt"


def main():
    blocks = io.BytesIO()
    count = 0
    for y in range(SIZE):
        for z in range(SIZE):
            for x in range(SIZE):
                write_block(blocks, 0, (x, y, z), None)
                count += 1
    write_template(OUT, (SIZE, SIZE, SIZE), [("minecraft:air", {})], blocks.getvalue(), count, [])
    print(f"wrote {OUT} ({count} blocks of air, {OUT.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
