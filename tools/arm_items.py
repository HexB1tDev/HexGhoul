#!/usr/bin/env python3
"""3.0.6: the investigators' kagune-made weapons as items (python3 tools/arm_items.py).

Each Arm (control/quinque/Arm.java) is two items: its case (shut) and its weapon (out of it).

  quinque_case_<key>   the case it is carried in: the case of the regular quinque it is held as (its frames: shut, split open, carried
                       by an investigator's wide or slim arm), the same shapes, recoloured: a black case, crimson trim and seams, its
                       emblem and core in its kagune's colour (so it is never taken for a regular one)
  quinque_<key>        the weapon: in the hand it is drawn from its own mesh (ArmItemLayer, ArmItemLayer.FirstPerson), never from this
                       model, so the model is its inventory icon (textures/item/quinque_<key>.png, tools/ArmIcons.java) and shrunk to
                       nothing in either hand

Writes the case textures (textures/item/quinque_arm_<key>.png), the case frames (models/item/quinque/arm_<key>_*.json) and both items'
models (models/item/quinque_case_<key>.json, models/item/quinque_<key>.json). The icons are tools/ArmIcons.java's.
"""
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEXTURES = ROOT / "src" / "main" / "resources" / "assets" / "hexkagune" / "textures" / "item"
MODELS = ROOT / "src" / "main" / "resources" / "assets" / "hexkagune" / "models" / "item"

# key: (the regular quinque's model it is carried as, its emblem, its core)
ARMS = {
    "tendril_whip": ("mace", (150, 14, 34), (255, 46, 70)),
    "crystal_spear": ("launcher", (176, 20, 40), (255, 96, 84)),
    "flesh_hammer": ("koukaku", (120, 30, 22), (255, 120, 60)),
    "bone_greatsword": ("bikaku", (130, 12, 24), (255, 52, 52)),
}

# The case's patches (column, row of 4x4 squares) and what each becomes.
CASE, TRIM, HANDLE, LATCH, EMBLEM, CORE = (0, 0), (1, 0), (2, 0), (3, 0), (4, 0), (5, 0)
RECOLOUR = {CASE: (44, 38, 42), TRIM: (96, 14, 26), HANDLE: (20, 16, 18), LATCH: (122, 100, 84)}

FRAMES = ["case_closed", "case_open", "case_carried", "case_carried_slim"]
HIDDEN = {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.001, 0.001, 0.001]}


def recolour(image, patch, colour):
    """Every pixel of the 4x4 patch made {@code colour}, keeping its own light and dark (its grain) against the patch's mean."""
    x0, y0 = patch[0] * 4, patch[1] * 4
    pixels = [image.getpixel((x0 + dx, y0 + dy)) for dy in range(4) for dx in range(4)]
    lum = [0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2] for p in pixels if p[3] > 0]
    if not lum:
        return
    mean = max(1.0, sum(lum) / len(lum))
    for dy in range(4):
        for dx in range(4):
            p = image.getpixel((x0 + dx, y0 + dy))
            if p[3] == 0:
                continue
            k = (0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]) / mean
            k = 1.0 + (k - 1.0) * 0.8
            image.putpixel((x0 + dx, y0 + dy), tuple(max(0, min(255, int(c * k))) for c in colour) + (p[3],))


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=1) + "\n")


def main():
    for key, (kind, emblem, core) in ARMS.items():
        sheet = Image.open(TEXTURES / f"quinque_{kind}.png").convert("RGBA")
        for patch, colour in RECOLOUR.items():
            recolour(sheet, patch, colour)
        recolour(sheet, EMBLEM, emblem)
        recolour(sheet, CORE, core)
        sheet.save(TEXTURES / f"quinque_arm_{key}.png")
        texture = f"hexkagune:item/quinque_arm_{key}"
        for frame in FRAMES:
            write(MODELS / "quinque" / f"arm_{key}_{frame}.json",
                  {"parent": f"hexkagune:item/quinque/{kind}_{frame}", "textures": {"q": texture, "particle": texture}})
        # Its case: shut, or carried by an investigator (QuinqueClient.caseFrame), or split open.
        write(MODELS / f"quinque_case_{key}.json", {
            "parent": f"hexkagune:item/quinque/arm_{key}_case_closed",
            "overrides": [
                {"predicate": {"hexkagune:deploy": 0.05}, "model": f"hexkagune:item/quinque/arm_{key}_case_carried"},
                {"predicate": {"hexkagune:deploy": 0.1}, "model": f"hexkagune:item/quinque/arm_{key}_case_carried_slim"},
                {"predicate": {"hexkagune:deploy": 0.2}, "model": f"hexkagune:item/quinque/arm_{key}_case_open"},
            ],
        })
        # Its weapon: the icon; nothing in a hand (its mesh is drawn there).
        write(MODELS / f"quinque_{key}.json", {
            "parent": "minecraft:item/handheld",
            "textures": {"layer0": f"hexkagune:item/quinque_{key}"},
            "display": {"thirdperson_righthand": HIDDEN, "thirdperson_lefthand": HIDDEN, "firstperson_righthand": HIDDEN,
                        "firstperson_lefthand": HIDDEN},
        })
        print(key)


if __name__ == "__main__":
    main()
