"""Paint the One-Eyed Owl's glow (textures/entity/owl_halo.png): a soft round halo, white, fading to nothing at its edge.

Drawn adding light (the game's eyes render type ignores alpha: its colour is weighted by its own alpha here), tinted by each halo's
colour. Needs Pillow. Run: python3 tools/owl_halo.py
"""
import math
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/hexkagune/textures/entity/owl_halo.png"
SIZE = 64


def main():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for y in range(SIZE):
        for x in range(SIZE):
            r = math.hypot(x + 0.5 - SIZE / 2, y + 0.5 - SIZE / 2) / (SIZE / 2)
            # A bright heart and a long soft falling off, as light blooms.
            a = max(0.0, 1.0 - r) ** 2.2 * 0.75 + max(0.0, 1.0 - r / 0.35) ** 2 * 0.25
            v = int(round(255 * min(1.0, a)))
            img.putpixel((x, y), (v, v, v, v))
    img.save(OUT)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
