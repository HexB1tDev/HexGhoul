"""Regenerate the kakuja model assets: python3 tools/kakuja/generate.py

Writes awakening/assets/geo/<form>.geo.json and awakening/assets/textures/kakuja/<form>.png (+ _glowmask)
for every form built so far. The outputs are committed; the build does not run this script."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import textures  # noqa: E402
import war  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(HERE))
ASSETS = os.path.join(ROOT, "awakening", "assets")


def main():
    os.makedirs(os.path.join(ASSETS, "geo"), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, "textures", "kakuja"), exist_ok=True)
    model, tendrils, cape = war.build()
    model.write(os.path.join(ASSETS, "geo", "war.geo.json"))
    textures.write(war.SWATCHES, war.MATERIALS, os.path.join(ASSETS, "textures", "kakuja", "war.png"),
                   os.path.join(ASSETS, "textures", "kakuja", "war_glowmask.png"), seed=11)
    print(f"war: {len(model.bones)} bones, {model.cube_count()} cubes, {len(tendrils)} tendrils, {len(cape)} cape strips")


if __name__ == "__main__":
    main()
