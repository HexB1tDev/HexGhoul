#!/usr/bin/env python3
"""1.3.25: the quinques' textures and models (python3 tools/quinque_models.py [preview-dir]).

Each quinque (Bikaku blade, Koukaku spear) is drawn in five frames, chosen by the item property "hexkagune:deploy" (0 its case shut
.. 1 its weapon out, QuinqueClient):

  <kind>_case_closed   the attaché case, upright, its handle up (held by it, hanging at the side)
  <kind>_case_open     split along its seam on its glowing kakuhou core
  <kind>_half          the handle become a grip, the case folded down round it into a guard, the weapon a third out
  <kind>_nearly        two thirds out, the guard closing in
  <kind>_full          the quinque

The case item and the weapon item both use all five (a case shutting runs them backwards); a still case is shut, a still weapon out.
The weapons are built standing (along y) and every element turned 45 degrees about z, so they lie on a vanilla sword sprite's diagonal
(grip low left, point high right) and are held as a sword is (vanilla's handheld transforms), only longer: the blade about 1.4 swords,
the spear nearly two (its butt behind the hand).

Textures: one 32x32 sheet per kind, patches of 4x4 pixels (2x2 UV), each face drawn from one patch.
"""
import json
import math
import random
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEXTURES = ROOT / "src" / "main" / "resources" / "assets" / "hexkagune" / "textures" / "item"
MODELS = ROOT / "src" / "main" / "resources" / "assets" / "hexkagune" / "models" / "item"

# Patches: (column, row) on the 8x8 grid of 4px squares.
CASE, TRIM, HANDLE, LATCH, EMBLEM, CORE = (0, 0), (1, 0), (2, 0), (3, 0), (4, 0), (5, 0)
BLADE, EDGE, JOINT, WRAP, STEEL, SHAFT = (0, 1), (1, 1), (2, 1), (3, 1), (4, 1), (5, 1)

KINDS = {
    "bikaku": {
        "case": (206, 209, 214), "trim": (64, 66, 72), "handle": (24, 24, 27), "latch": (150, 153, 160),
        "emblem": (176, 18, 30), "core": (255, 48, 58),
        "blade": (98, 10, 18), "edge": (236, 52, 60), "joint": (44, 4, 9), "wrap": (30, 28, 30), "steel": (184, 187, 194),
        "shaft": (40, 38, 42),
    },
    "koukaku": {
        "case": (206, 209, 214), "trim": (64, 66, 72), "handle": (24, 24, 27), "latch": (150, 153, 160),
        "emblem": (16, 150, 168), "core": (90, 250, 255),
        "blade": (62, 66, 76), "edge": (208, 214, 224), "joint": (32, 35, 42), "wrap": (30, 28, 30), "steel": (184, 187, 194),
        "shaft": (226, 226, 230),
    },
}


def clamp(v):
    return max(0, min(255, int(round(v))))


def texture(kind, colours):
    rng = random.Random(kind)
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))

    def patch(cell, base, style="noise"):
        cx, cy = cell
        for y in range(4):
            for x in range(4):
                k = 1.0 + rng.uniform(-0.05, 0.05)
                if style == "plate" and (x == 0 or y == 0):
                    k *= 1.08  # a lit edge
                if style == "plate" and (x == 3 or y == 3):
                    k *= 0.82  # a shadowed edge
                if style == "glow":
                    k = 1.0 if 1 <= x <= 2 and 1 <= y <= 2 else 0.78
                if style == "wrap" and (x + y) % 2 == 0:
                    k *= 1.35  # the grip's criss-cross
                if style == "vein" and (x == 1 or (x == 2 and y % 2 == 0)):
                    k *= 0.7  # a darker vein along the kagune
                img.putpixel((cx * 4 + x, cy * 4 + y), tuple(clamp(c * k) for c in base) + (255,))

    patch(CASE, colours["case"], "plate")
    patch(TRIM, colours["trim"])
    patch(HANDLE, colours["handle"])
    patch(LATCH, colours["latch"], "plate")
    patch(EMBLEM, colours["emblem"], "plate")
    patch(CORE, colours["core"], "glow")
    patch(BLADE, colours["blade"], "vein")
    patch(EDGE, colours["edge"])
    patch(JOINT, colours["joint"])
    patch(WRAP, colours["wrap"], "wrap")
    patch(STEEL, colours["steel"], "plate")
    patch(SHAFT, colours["shaft"])
    return img


def uv(cell):
    cx, cy = cell
    return [cx * 2, cy * 2, cx * 2 + 2, cy * 2 + 2]


def box(frm, to, cell, front=None, turned=True):
    """An element from {frm} to {to}, every face drawn from {cell} ({front}: its north and south faces from another)."""
    faces = {}
    for face in ("north", "south", "east", "west", "up", "down"):
        faces[face] = {"uv": uv(front if front and face in ("north", "south") else cell), "texture": "#q"}
    element = {"from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to], "faces": faces}
    if turned:
        element["rotation"] = {"angle": -45, "axis": "z", "origin": [8, 8, 8]}
    return element


# ---------------------------------------------------------------------------------------------------- the case

def case_elements(opened):
    """The attaché case standing on its foot, handle up; {opened}: split along its seam over its glowing core."""
    lift = 1.1 if opened else 0.0
    els = [
        box((1, 2, 6.5), (15, 6.8, 9.5), CASE, turned=False),                       # the lower half
        box((1, 6.8 + lift, 6.5), (15, 12 + lift, 9.5), CASE, turned=False),        # the upper half (the lid)
        box((0.85, 6.55, 6.35), (15.15, 7.05, 9.65), TRIM, turned=False),           # the seam's band (lower lip)
        box((0.85, 6.8 + lift, 6.35), (15.15, 7.25 + lift, 9.65), TRIM, turned=False),
        box((0.85, 1.8, 6.35), (15.15, 2.3, 9.65), TRIM, turned=False),             # the foot rim
        box((3.2, 10.4 + lift, 9.5), (4.6, 11.4 + lift, 9.85), LATCH, turned=False),  # the latches
        box((11.4, 10.4 + lift, 9.5), (12.8, 11.4 + lift, 9.85), LATCH, turned=False),
        box((7, 8.0 + lift, 9.5), (9, 10.0 + lift, 9.7), EMBLEM, turned=False),     # the emblem, front and back
        box((7, 8.0 + lift, 6.3), (9, 10.0 + lift, 6.5), EMBLEM, turned=False),
        box((5.6, 12 + lift, 7.5), (6.6, 13.4 + lift, 8.5), HANDLE, turned=False),  # the handle
        box((9.4, 12 + lift, 7.5), (10.4, 13.4 + lift, 8.5), HANDLE, turned=False),
        box((5.2, 13.4 + lift, 7.35), (10.8, 14.5 + lift, 8.65), HANDLE, turned=False),
    ]
    if opened:
        els.append(box((2.5, 6.8, 7.0), (13.5, 6.8 + lift, 9.0), CORE, turned=False))  # the kakuhou core in the gap
    return els


# ---------------------------------------------------------------------------------------------------- the weapons (standing)

GUARD_TOP = 10.0


def stretch(y, f, low):
    """A standing weapon's height {y} as it runs out ({f} 0..1): above the guard the blade, below {low} a spear's butt."""
    if y > GUARD_TOP:
        return GUARD_TOP + (y - GUARD_TOP) * f
    if y < low:
        return low + (y - low) * f
    return y


def guard(f, cell_core, wide):
    """The case folded down round the grip into a guard: wider and taller the less it has closed ({f})."""
    spread = 1.0 + (1.0 - f) * 0.9
    half = wide / 2.0 * spread
    depth = 1.5 + (1.0 - f) * 0.6
    top = GUARD_TOP
    bottom = 8.0 - (1.0 - f) * 1.5
    return [
        box((8 - half, bottom, 8 - depth), (8 + half, top, 8 + depth), CASE),
        box((8 - half - 0.15, bottom + (top - bottom) * 0.42, 8 - depth - 0.15), (8 + half + 0.15, bottom + (top - bottom) * 0.58, 8 + depth + 0.15), TRIM),
        box((8 - 1.6, bottom + 0.4, 8 + depth), (8 + 1.6, top - 0.4, 8 + depth + 0.2), cell_core),
        box((8 - 1.6, bottom + 0.4, 8 - depth - 0.2), (8 + 1.6, top - 0.4, 8 - depth), cell_core),
    ]


def blade_elements(f):
    """The Bikaku blade: a black grip, the guard, and four kagune segments running out to a point, a bright edge down one side."""
    s = lambda y: stretch(y, f, 0.0)
    els = [
        box((7, 0.4, 7), (9, 2, 9), STEEL),                 # the pommel
        box((7.25, 2, 7.25), (8.75, 8.2, 8.75), WRAP),      # the grip (the case's handle)
    ]
    els += guard(f, CORE, 6.0)
    segments = [(6.5, 10, 16, 7.4), (6.65, 16, 21.5, 7.45), (6.85, 21.5, 26.5, 7.5), (7.15, 26.5, 30, 7.55)]
    for x0, y0, y1, z0 in segments:
        if s(y1) - s(y0) < 0.05:
            continue
        els.append(box((x0, s(y0), z0), (16 - x0, s(y1), 16 - z0), BLADE))
        els.append(box((16 - x0 - 0.05, s(y0), z0 + 0.15), (16 - x0 + 0.35, s(y1), 16 - z0 - 0.15), EDGE))  # its edge
        els.append(box((x0 - 0.12, s(y0), z0 - 0.08), (16 - x0 + 0.12, s(y0) + 0.35 * f + 0.05, 16 - z0 + 0.08), JOINT))  # the joint
    if f > 0.9:
        els.append(box((7.55, s(30), 7.62), (8.45, s(31.6), 8.38), EDGE))  # the point
    return els


def spear_elements(f):
    """The Koukaku spear: a white shaft (black grip where the hand is), the guard as a collar, a broad plated head with fins."""
    low = 1.0
    s = lambda y: stretch(y, f, low)
    els = [
        box((7, s(-12), 7), (9, s(-10.5), 9), STEEL),           # the butt cap
        box((7.4, s(-10.5), 7.4), (8.6, low, 8.6), SHAFT),      # the shaft behind the hand
        box((7.3, low, 7.3), (8.7, 8.2, 8.7), WRAP),            # the grip
        box((7.4, 8.2, 7.4), (8.6, GUARD_TOP, 8.6), SHAFT),
    ]
    els += guard(f, CORE, 5.0)
    shaft_top = 18.0
    if s(shaft_top) - GUARD_TOP > 0.05:
        els.append(box((7.4, GUARD_TOP, 7.4), (8.6, s(shaft_top), 8.6), SHAFT))      # the shaft past the guard
        els.append(box((6.4, s(shaft_top), 6.8), (9.6, s(19.5), 9.2), STEEL))         # the head's socket
    head = [(5.4, 19.5, 23.5), (6.0, 23.5, 27.3), (6.7, 27.3, 30.0)]
    for x0, y0, y1 in head:
        if s(y1) - s(y0) < 0.05:
            continue
        els.append(box((x0, s(y0), 7.35), (16 - x0, s(y1), 8.65), BLADE))
        els.append(box((x0 - 0.05, s(y0), 7.5), (x0 + 0.3, s(y1), 8.5), EDGE))       # both edges, bright
        els.append(box((16 - x0 - 0.3, s(y0), 7.5), (16 - x0 + 0.05, s(y1), 8.5), EDGE))
    if f > 0.4:
        els.append(box((7.55, s(19.5), 7.15), (8.45, s(30.5), 8.85), STEEL))          # the spine
        els.append(box((7.8, s(20.5), 8.85), (8.2, s(29.0), 8.95), CORE))             # a glowing seam down it
        els.append(box((4.4, s(19.5), 7.6), (5.4, s(21.8), 8.4), JOINT))              # the fins
        els.append(box((10.6, s(19.5), 7.6), (11.6, s(21.8), 8.4), JOINT))
    if f > 0.9:
        els.append(box((7.3, s(30.0), 7.5), (8.7, s(31.8), 8.5), EDGE))               # the point
    return els


# ---------------------------------------------------------------------------------------------------- how each is held

HANDHELD = {
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
}


def weapon_display(span, first_scale, first_up):
    """Handheld, with the GUI, the ground and an item frame fitted to its length ({span}: its standing extent, low and high), and in
    the first person a little smaller and lower than a sword (it is longer: it still runs out of the view, as a lance would)."""
    lo, hi = span
    # Its diagonal runs from (8 + (lo-8)/sqrt2) to (8 + (hi-8)/sqrt2) on both axes; fitted into the slot (14 of 16), centred.
    a = 8 + (lo - 8) / math.sqrt(2)
    b = 8 + (hi - 8) / math.sqrt(2)
    scale = min(1.0, 14.0 / (b - a))
    centre = (a + b) / 2.0
    shift = round((8 - centre) * scale, 3)
    d = dict(HANDHELD)
    d["firstperson_righthand"] = {"rotation": [0, -90, 25], "translation": [1.13, first_up, 1.13], "scale": [first_scale] * 3}
    d["firstperson_lefthand"] = {"rotation": [0, 90, -25], "translation": [1.13, first_up, 1.13], "scale": [first_scale] * 3}
    d["gui"] = {"rotation": [0, 0, 0], "translation": [shift, shift, 0], "scale": [round(scale, 3)] * 3}
    d["ground"] = {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [round(scale * 0.6, 3)] * 3}
    d["fixed"] = {"rotation": [0, 180, 0], "translation": [-shift, shift, 0], "scale": [round(scale, 3)] * 3}
    return d


CASE_DISPLAY = {
    # Held by its handle, hanging at the side, its broad face along the arm (it turns 90 degrees from a sword's).
    # Solved against a player's arm (its holding pose) so the handle sits where a sword's grip is held and the case hangs straight.
    "thirdperson_righthand": {"rotation": [165, 90, -90], "translation": [0, -0.51, -2.47], "scale": [0.62, 0.62, 0.62]},
    "thirdperson_lefthand": {"rotation": [165, -90, 90], "translation": [0, -0.51, -2.47], "scale": [0.62, 0.62, 0.62]},
    # Held up in the lower corner, handle on top, turned a little toward the eye (chosen from previews of the first person view).
    "firstperson_righthand": {"rotation": [8, -25, 0], "translation": [-3, 2, -3], "scale": [0.55, 0.55, 0.55]},
    "firstperson_lefthand": {"rotation": [8, 25, 0], "translation": [-3, 2, -3], "scale": [0.55, 0.55, 0.55]},
    "gui": {"rotation": [15, -30, 0], "translation": [0, -0.5, 0], "scale": [0.9, 0.9, 0.9]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
}

FRAMES = [("case_open", 0.2), ("half", 0.45), ("nearly", 0.7), ("full", 0.95)]


def model(kind, elements, display):
    return {"credit": "HexKagune 1.3.25: tools/quinque_models.py", "texture_size": [32, 32],
            "textures": {"q": f"hexkagune:item/quinque_{kind}", "particle": f"hexkagune:item/quinque_{kind}"},
            "elements": elements, "display": display, "gui_light": "front"}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=1) + "\n")


def main():
    TEXTURES.mkdir(parents=True, exist_ok=True)
    out = {}
    for kind, colours in KINDS.items():
        texture(kind, colours).save(TEXTURES / f"quinque_{kind}.png")
        build = blade_elements if kind == "bikaku" else spear_elements
        span = (0.4, 31.6) if kind == "bikaku" else (-12.0, 31.8)
        held = weapon_display(span, 0.55, 2.4) if kind == "bikaku" else weapon_display(span, 0.5, 2.0)
        frames = {
            "case_closed": model(kind, case_elements(False), CASE_DISPLAY),
            "case_open": model(kind, case_elements(True), CASE_DISPLAY),
            "half": model(kind, build(0.33), held),
            "nearly": model(kind, build(0.66), held),
            "full": model(kind, build(1.0), held),
        }
        for name, data in frames.items():
            write(MODELS / "quinque" / f"{kind}_{name}.json", data)
        out[kind] = frames
        overrides = [{"predicate": {"hexkagune:deploy": v}, "model": f"hexkagune:item/quinque/{kind}_{name}"} for name, v in FRAMES]
        weapon = "quinque_bikaku_blade" if kind == "bikaku" else "quinque_koukaku_spear"
        for item in (f"quinque_case_{kind}", weapon):
            write(MODELS / f"{item}.json", {"parent": f"hexkagune:item/quinque/{kind}_case_closed", "overrides": overrides})
        print(f"{kind}: texture and {len(frames)} frames; {sum(len(f['elements']) for f in frames.values())} elements")
    return out


if __name__ == "__main__":
    main()
