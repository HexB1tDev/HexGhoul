#!/usr/bin/env python3
"""3.6.5: the inside of Grand Devouring's mouth as a GeckoLib model (the owner: "the inside of the enormous fleshy cavity, including the
thick organic walls, multiple rows of teeth, moving tissue, and the throat extending deeper into darkness"; "The fleshy walls should
pulsate, contract, and move around the victim"; "witness the jaws slowly closing around them, gradually blocking the outside world").

Writes, under <out>/assets/hexkagune/:
  geo/devour_cavity.geo.json            the model (model pixels, 16 to a block): the mouth at y 0, the cavity up to WALL_TOP, the throat on
                                        up into a dark cap; a hole RADIUS px wide (radius) at the canonical size (scaled to the victim's
                                        cavity in game)
  animations/devour_cavity.animation.json  "animation.hexkagune.devour_cavity.pulse": the walls breathing, each ring a beat after the one
                                        below; the throat working upward
  textures/geo/devour_cavity.png        light flesh (tinted the devourer's kagune colour in game), bone teeth (drawn untinted), a throat
                                        into darkness

Bones (the renderer and the model's code find them by name):
  root
    wall0..wall5     rings of flesh panels round the cavity (pulse), each with toothN (its ring of teeth, drawn untinted)
    throat0..throat2 rings narrowing on up, then cap
    jaw0..jaw7       turned round the mouth; each holds hingeN at the rim (code turns it: open hangs it out and down, shut lays it
                     across the hole) and that a fangN at its tip (drawn untinted)

Pure: python3 and PIL. Usage (writes into the mod's resources): python3 tools/devour_cavity.py src/main/resources
Preview off-game: tools/CavityPreview.java (see its header).
"""
import json
import math
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

RADIUS = 16.0        # the cavity's radius at the canonical size (px)
RINGS = 6            # rings of wall
RING_H = 6.0         # each ring's height (px)
WALL_TOP = RINGS * RING_H
PANELS = 16          # panels round a ring
THICK = 3.0          # a panel's thickness
TEETH = 10           # teeth round a ring
THROAT = [(13.0, 6.0), (9.5, 6.0), (6.0, 6.0)]  # (radius, height) of the throat's rings, on up from WALL_TOP
JAWS = 8
TEX = 128


def face(u, v, w, h):
    return {"uv": [u, v], "uv_size": [w, h]}


def cube(origin, size, pivot, rotation, uv):
    return {"origin": [round(x, 4) for x in origin], "size": [round(x, 4) for x in size], "pivot": [round(x, 4) for x in pivot],
            "rotation": [round(x, 4) for x in rotation], "uv": uv}


def panel_uv(band_v, band_h):
    """Inner face (north, facing the middle): the flesh band; the rest a darker edge."""
    return {"north": face(0, band_v, 32, band_h), "south": face(96, 0, 8, 8), "east": face(32, band_v, 4, band_h),
            "west": face(32, band_v, 4, band_h), "up": face(36, band_v, 16, 3), "down": face(36, band_v + 3, 16, 3)}


def tooth_uv():
    return {k: face(64, 0, 6, 16) for k in ("north", "south", "east", "west")} | {"up": face(70, 0, 4, 4), "down": face(70, 4, 4, 4)}


def model():
    bones = [{"name": "root", "pivot": [0.0, 0.0, 0.0]}]
    rng = random.Random(7)
    for k in range(RINGS):
        y0 = k * RING_H
        mid = y0 + RING_H / 2
        width = 2 * math.pi * RADIUS / PANELS * 1.3
        offset = 360.0 / PANELS / 2 if k % 2 else 0.0
        cubes = []
        for p in range(PANELS):
            a = p * 360.0 / PANELS + offset
            # Each panel a little in or out of line with the next: folds of flesh, not a pipe; overlapping, so nothing shows through.
            sink = rng.uniform(-0.5, 0.5)
            cubes.append(cube([-width / 2, y0 - 1.0, RADIUS + sink], [width, RING_H + 2.0, THICK], [0, mid, 0], [0, a, 0],
                              panel_uv(k * 16, 16)))
        bones.append({"name": f"wall{k}", "parent": "root", "pivot": [0.0, mid, 0.0], "cubes": cubes})
        ring_teeth = []
        length = 5.5 - 0.6 * k  # bigger near the mouth
        for t in range(TEETH):
            a = t * 360.0 / TEETH + (18.0 if k % 2 else 0.0) + rng.uniform(-6, 6)
            w = 2.2 - 0.18 * k
            # A fang out of the wall toward the middle: a thick root, then a thin tip set a little lower (curving down).
            root = length * 0.45
            ring_teeth.append(cube([-w / 2, mid - w / 2, RADIUS - root + 0.4], [w, w, root], [0, mid, 0], [0, a, 0], tooth_uv()))
            tip = w * 0.45
            ring_teeth.append(cube([-tip / 2, mid - w * 0.35 - tip / 2, RADIUS - length + 0.4], [tip, tip, length - root + 0.2], [0, mid, 0],
                                   [0, a, 0], tooth_uv()))
        bones.append({"name": f"tooth{k}", "parent": f"wall{k}", "pivot": [0.0, mid, 0.0], "cubes": ring_teeth})
    # The throat, narrowing on up into the dark.
    y = WALL_TOP
    for i, (radius, height) in enumerate(THROAT):
        mid = y + height / 2
        n = 10
        width = 2 * math.pi * radius / n * 1.35
        cubes = [cube([-width / 2, y - 1.0, radius], [width, height + 2.0, THICK], [0, mid, 0], [0, p * 360.0 / n + 9.0 * i, 0],
                      panel_uv(96 + i * 10, 10)) for p in range(n)]
        bones.append({"name": f"throat{i}", "parent": "root", "pivot": [0.0, mid, 0.0], "cubes": cubes})
        y += height
    cap = THROAT[-1][0] + THICK
    bones.append({"name": "cap", "parent": "root", "pivot": [0.0, y, 0.0], "cubes": [
        cube([-cap, y, -cap], [2 * cap, 1.0, 2 * cap], [0, y, 0], [0, 0, 0],
             {k: face(112, 112, 16, 16) for k in ("north", "south", "east", "west", "up", "down")})]})
    # The jaws: a flap of lip per eighth of the rim, hinged at the rim; code turns each hinge.
    for j in range(JAWS):
        a = j * 360.0 / JAWS + 22.5
        bones.append({"name": f"jaw{j}", "parent": "root", "pivot": [0.0, 0.0, 0.0], "rotation": [0.0, a, 0.0]})
        width = 2 * math.pi * RADIUS / JAWS * 1.1
        flap = {"north": face(80, 16, 16, 4), "south": face(80, 16, 16, 4), "east": face(80, 16, 4, 4), "west": face(80, 16, 4, 4),
                "up": face(80, 20, 16, 24), "down": face(96, 20, 16, 24)}
        bones.append({"name": f"hinge{j}", "parent": f"jaw{j}", "pivot": [0.0, 0.0, RADIUS], "cubes": [
            cube([-width / 2, -1.5, 0.6], [width, 1.5, RADIUS + THICK - 0.6], [0, 0, RADIUS], [0, 0, 0], flap)]})
        bones.append({"name": f"fang{j}", "parent": f"hinge{j}", "pivot": [0.0, 0.0, 2.0], "cubes": [
            cube([-0.9, 0.0, 1.2], [1.8, 4.5, 1.8], [0, 0, 2.0], [-12.0, 0, 0], tooth_uv())]})
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.hexkagune.devour_cavity", "texture_width": TEX, "texture_height": TEX,
                        "visible_bounds_width": 4, "visible_bounds_height": 6, "visible_bounds_offset": [0, 2, 0]},
        "bones": bones}]}


def animation():
    length = 2.4
    steps = 8
    bones = {}
    for k in range(RINGS + len(THROAT)):
        name = f"wall{k}" if k < RINGS else f"throat{k - RINGS}"
        amount = 0.07 if k < RINGS else 0.12
        scale = {}
        rotation = {}
        for s in range(steps + 1):
            t = length * s / steps
            phase = 2 * math.pi * s / steps - 0.75 * k
            side = 1.0 + amount * math.sin(phase)
            tall = 1.0 - 0.4 * amount * math.sin(phase)
            key = f"{t:.4f}".rstrip("0").rstrip(".") if s else "0.0"
            scale[key] = {"vector": [round(side, 4), round(tall, 4), round(side, 4)], "easing": "catmullrom"}
            rotation[key] = {"vector": [0.0, round(4.0 * math.sin(phase * 0.5 + k), 3), 0.0], "easing": "catmullrom"}
        bones[name] = {"scale": scale, "rotation": rotation}
    return {"format_version": "1.8.0", "animations": {"animation.hexkagune.devour_cavity.pulse": {
        "loop": True, "animation_length": length, "bones": bones}}}


def texture():
    rng = random.Random(11)
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)
    # Wall bands (light flesh, tinted in game), darker the deeper they are; folds across, veins, wet glints.
    for k in range(RINGS):
        v0 = k * 16
        base = 0.95 - 0.09 * k
        for y in range(16):
            fold = 0.82 + 0.18 * math.sin(y / 16 * math.pi * 2 + k)
            for x in range(52):
                n = 0.9 + 0.1 * rng.random()
                lum = base * fold * n
                d.point((x, v0 + y), (int(255 * lum), int(225 * lum), int(225 * lum), 255))
        for _ in range(5):
            x = rng.randrange(0, 32)
            y = v0 + rng.randrange(0, 16)
            pts = [(x, y)]
            for _ in range(6):
                x += rng.choice((-1, 0, 1, 1))
                y += rng.choice((-1, 0, 1))
                pts.append((max(0, min(31, x)), max(v0, min(v0 + 15, y))))
            d.line(pts, fill=(int(150 * base), int(60 * base), int(70 * base), 255))
        for _ in range(7):
            x, y = rng.randrange(0, 32), v0 + rng.randrange(0, 16)
            d.point((x, y), (255, 250, 250, 255))
    # Teeth: bone, darker at the root.
    for y in range(16):
        g = 0.62 + 0.38 * (1 - y / 15)
        for x in range(64, 74):
            d.point((x, y), (int(244 * g), int(236 * g), int(220 * g), 255))
    # Lip flaps.
    for y in range(16, 44):
        for x in range(80, 112):
            lum = 0.85 * (0.85 + 0.15 * math.sin(x / 3.0)) * (0.9 + 0.1 * rng.random())
            d.point((x, y), (int(255 * lum), int(205 * lum), int(205 * lum), 255))
    # Outer faces: dark flesh.
    d.rectangle((96, 0, 103, 7), fill=(70, 30, 34, 255))
    # The throat: into darkness.
    for i in range(3):
        v0 = 96 + i * 10
        for y in range(10):
            lum = (0.32 - 0.1 * i) * (1 - y / 30)
            for x in range(52):
                q = lum * (0.85 + 0.15 * rng.random())
                d.point((x, v0 + y), (int(255 * q), int(150 * q), int(150 * q), 255))
    d.rectangle((112, 112, 127, 127), fill=(6, 2, 3, 255))
    return img.filter(ImageFilter.SMOOTH)


def main():
    out = Path(sys.argv[1]) / "assets" / "hexkagune"
    (out / "geo").mkdir(parents=True, exist_ok=True)
    (out / "animations").mkdir(parents=True, exist_ok=True)
    (out / "textures" / "geo").mkdir(parents=True, exist_ok=True)
    (out / "geo" / "devour_cavity.geo.json").write_text(json.dumps(model(), indent=1) + "\n")
    (out / "animations" / "devour_cavity.animation.json").write_text(json.dumps(animation(), indent=1) + "\n")
    texture().save(out / "textures" / "geo" / "devour_cavity.png")
    print(out)


if __name__ == "__main__":
    main()
