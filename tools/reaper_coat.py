#!/usr/bin/env python3
"""4.1.37: the White Reaper's coat, the one he takes off (client.render.ReaperCoatLayer): the Formal Overcoat's own model and nets
(src/main/resources/assets/hexkagune/textures/outfit/formal.png, left as it is), repainted white as his coat is: every grey and the navy
trim turned to white cloth with its own shading kept (each texel's light carried over), the metal and the dark inside left as they are.

Usage: python3 tools/reaper_coat.py   (writes textures/entity/ccg/reaper_coat.png)
"""
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "src/main/resources/assets/hexkagune/textures/outfit/formal.png"
OUT = ROOT / "src/main/resources/assets/hexkagune/textures/entity/ccg/reaper_coat.png"


def main():
    rgba = np.asarray(Image.open(SOURCE).convert("RGBA")).astype(np.float64)
    rgb = rgba[..., :3]
    alpha = rgba[..., 3]
    light = rgb @ np.array([0.299, 0.587, 0.114])
    top = rgb.max(axis=2)
    low = rgb.min(axis=2)
    saturation = np.where(top > 0, (top - low) / np.maximum(top, 1.0), 0.0)
    out = rgb.copy()
    cloth = (alpha > 0) & (light > 38.0)
    grey = cloth & (saturation < 0.22)
    trim = cloth & ~grey  # the navy trim, the red of the badge: white too, a shade darker
    # White cloth: its light kept as shading round a bright, faintly cool white.
    white = 172.0 + 0.36 * light
    out[grey] = np.stack([white[grey] - 2.0, white[grey], white[grey] + 4.0], axis=-1)
    shade = 158.0 + 0.5 * light
    out[trim] = np.stack([shade[trim] - 2.0, shade[trim], shade[trim] + 3.0], axis=-1)
    out = np.clip(out, 0, 255)
    image = np.dstack([out, alpha]).astype(np.uint8)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(image, "RGBA").save(OUT)
    print("wrote", OUT.relative_to(ROOT))


if __name__ == "__main__":
    main()
