#!/usr/bin/env python3
"""1.3.25-1.3.26: the quinques' textures and models (python3 tools/quinque_models.py).

Five quinques, each drawn in five frames chosen by the item property "hexkagune:deploy" (0 its case shut .. 1 its weapon out,
QuinqueClient):

  <kind>_case_closed   the attaché case, upright, its handle up (held by it, hanging at the side)
  <kind>_case_open     split along its seam on its glowing kakuhou core
  <kind>_half          the weapon a third out of the hand, the case folded down round the grip
  <kind>_nearly        two thirds out, the case nearly gone into it
  <kind>_full          the quinque

The weapons are built standing (along y; the right hand on the grip at y 2..8, where a vanilla sword is held) and every element is
turned 45 degrees about z so they lie on a vanilla sword sprite's diagonal and vanilla's handheld transforms hold them. A frame part way
out is the whole weapon telescoped in toward the hand (every height drawn in toward it), the case shrinking round the grip.

  bikaku     the Bikaku blade: one-handed, four kagune segments, a bright edge
  koukaku    the Koukaku spear: big (three blocks), gripped in the middle with both hands, a broad plated head
  katana     the Bikaku katana: a long two-handed hilt, a tsuba, a slightly curved dark blade with a glowing edge
  mace       the Rinkaku flesh mace: a long two-handed handle and a great lump of flesh for a head, bone spikes through it
  launcher   the Ukaku crystal launcher: a gun, three barrels, a magazine of glowing crystal, a pistol grip and a stock

Textures: one 32x32 sheet per kind, patches of 4x4 pixels (2x2 UV), each face drawn from one patch.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEXTURES = ROOT / "src" / "main" / "resources" / "assets" / "hexkagune" / "textures" / "item"
MODELS = ROOT / "src" / "main" / "resources" / "assets" / "hexkagune" / "models" / "item"

# Patches: (column, row) on the 8x8 grid of 4px squares.
CASE, TRIM, HANDLE, LATCH, EMBLEM, CORE = (0, 0), (1, 0), (2, 0), (3, 0), (4, 0), (5, 0)
BLADE, EDGE, JOINT, WRAP, STEEL, SHAFT = (0, 1), (1, 1), (2, 1), (3, 1), (4, 1), (5, 1)
FLESH, VEIN, BONE, GOLD = (0, 2), (1, 2), (2, 2), (3, 2)

BASE = {"case": (206, 209, 214), "trim": (64, 66, 72), "handle": (24, 24, 27), "latch": (150, 153, 160), "wrap": (30, 28, 30),
        "steel": (184, 187, 194), "flesh": (150, 40, 52), "vein": (88, 14, 26), "bone": (226, 218, 198), "gold": (196, 160, 70)}
KINDS = {
    "bikaku": dict(BASE, emblem=(176, 18, 30), core=(255, 48, 58), blade=(98, 10, 18), edge=(236, 52, 60), joint=(44, 4, 9),
                   shaft=(40, 38, 42)),
    "koukaku": dict(BASE, emblem=(16, 150, 168), core=(90, 250, 255), blade=(62, 66, 76), edge=(208, 214, 224), joint=(32, 35, 42),
                    shaft=(226, 226, 230)),
    "katana": dict(BASE, emblem=(120, 10, 20), core=(255, 70, 70), blade=(34, 30, 36), edge=(255, 60, 66), joint=(18, 16, 20),
                   shaft=(36, 30, 34)),
    "mace": dict(BASE, emblem=(150, 30, 90), core=(255, 70, 120), blade=(150, 40, 52), edge=(226, 218, 198), joint=(88, 14, 26),
                 shaft=(214, 206, 192)),
    "launcher": dict(BASE, emblem=(30, 120, 200), core=(120, 240, 255), blade=(56, 58, 66), edge=(196, 200, 210), joint=(28, 30, 36),
                     shaft=(150, 154, 162)),
}

HAND = 5.0  # the right hand's grip, standing (a vanilla sword's)


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
                    k *= 1.08
                if style == "plate" and (x == 3 or y == 3):
                    k *= 0.82
                if style == "glow":
                    k = 1.0 if 1 <= x <= 2 and 1 <= y <= 2 else 0.78
                if style == "wrap" and (x + y) % 2 == 0:
                    k *= 1.35
                if style == "vein" and (x == 1 or (x == 2 and y % 2 == 0)):
                    k *= 0.7
                if style == "flesh":
                    k *= 1.0 + 0.18 * math.sin(x * 2.1 + y * 1.3) + rng.uniform(-0.08, 0.08)
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
    patch(FLESH, colours["flesh"], "flesh")
    patch(VEIN, colours["vein"], "vein")
    patch(BONE, colours["bone"], "plate")
    patch(GOLD, colours["gold"], "plate")
    return img


def uv(cell):
    cx, cy = cell
    return [cx * 2, cy * 2, cx * 2 + 2, cy * 2 + 2]


def box(frm, to, cell, turned=True):
    faces = {face: {"uv": uv(cell), "texture": "#q"} for face in ("north", "south", "east", "west", "up", "down")}
    element = {"from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to], "faces": faces}
    if turned:
        element["rotation"] = {"angle": -45, "axis": "z", "origin": [8, 8, 8]}
    return element


def part(x0, y0, z0, x1, y1, z1, cell):
    return ((min(x0, x1), y0, min(z0, z1)), (max(x0, x1), y1, max(z0, z1)), cell)


def sym(half_x, y0, half_z, y1, cell, dx=0.0):
    """A part centred on the weapon's line (x 8, z 8), {half_x} / {half_z} out each way, shifted {dx} across."""
    return part(8 - half_x + dx, y0, 8 - half_z, 8 + half_x + dx, y1, 8 + half_z, cell)


# ---------------------------------------------------------------------------------------------------- the case

def case_elements(opened):
    lift = 1.1 if opened else 0.0
    els = [
        box((1, 2, 6.5), (15, 6.8, 9.5), CASE, turned=False),
        box((1, 6.8 + lift, 6.5), (15, 12 + lift, 9.5), CASE, turned=False),
        box((0.85, 6.55, 6.35), (15.15, 7.05, 9.65), TRIM, turned=False),
        box((0.85, 6.8 + lift, 6.35), (15.15, 7.25 + lift, 9.65), TRIM, turned=False),
        box((0.85, 1.8, 6.35), (15.15, 2.3, 9.65), TRIM, turned=False),
        box((3.2, 10.4 + lift, 9.5), (4.6, 11.4 + lift, 9.85), LATCH, turned=False),
        box((11.4, 10.4 + lift, 9.5), (12.8, 11.4 + lift, 9.85), LATCH, turned=False),
        box((7, 8.0 + lift, 9.5), (9, 10.0 + lift, 9.7), EMBLEM, turned=False),
        box((7, 8.0 + lift, 6.3), (9, 10.0 + lift, 6.5), EMBLEM, turned=False),
        box((5.6, 12 + lift, 7.5), (6.6, 13.4 + lift, 8.5), HANDLE, turned=False),
        box((9.4, 12 + lift, 7.5), (10.4, 13.4 + lift, 8.5), HANDLE, turned=False),
        box((5.2, 13.4 + lift, 7.35), (10.8, 14.5 + lift, 8.65), HANDLE, turned=False),
    ]
    if opened:
        els.append(box((2.5, 6.8, 7.0), (13.5, 6.8 + lift, 9.0), CORE, turned=False))
    return els


# ---------------------------------------------------------------------------------------------------- the weapons, standing

def blade_parts():
    p = [sym(1.0, 0.4, 1.0, 2.0, STEEL), sym(0.75, 2.0, 0.75, 8.2, WRAP),
         sym(3.0, 8.2, 1.5, 10.0, CASE), sym(3.15, 8.85, 1.65, 9.35, TRIM), sym(1.6, 8.6, 1.7, 9.6, CORE)]
    for x0, y0, y1, z in [(1.5, 10, 16, 0.6), (1.35, 16, 21.5, 0.55), (1.15, 21.5, 26.5, 0.5), (0.85, 26.5, 30, 0.45)]:
        p.append(sym(x0, y0, z, y1, BLADE))
        p.append(part(8 + x0 - 0.05, y0, 8 - z + 0.15, 8 + x0 + 0.35, y1, 8 + z - 0.15, EDGE))
        p.append(sym(x0 + 0.12, y0, z + 0.08, y0 + 0.4, JOINT))
    p.append(sym(0.45, 30, 0.38, 31.6, EDGE))
    return p


def spear_parts():
    p = [sym(1.25, -16, 1.25, -14.6, STEEL),               # the butt cap
         sym(0.9, -14.6, 0.9, 20.0, SHAFT),                # the shaft, thick
         sym(1.0, -9.5, 1.0, -4.0, WRAP),                  # the left hand's grip
         sym(1.0, 1.0, 1.0, 8.0, WRAP),                    # the right hand's (the middle)
         sym(2.2, 20.0, 1.6, 22.4, CASE), sym(2.35, 20.9, 1.75, 21.5, TRIM), sym(1.3, 20.5, 1.8, 21.9, CORE)]
    for half, y0, y1 in [(3.4, 22.4, 25.8), (2.8, 25.8, 28.8), (1.9, 28.8, 30.8)]:
        p.append(sym(half, y0, 0.8, y1, BLADE))
        p.append(part(8 - half - 0.1, y0, 7.35, 8 - half + 0.35, y1, 8.65, EDGE))
        p.append(part(8 + half - 0.35, y0, 7.35, 8 + half + 0.1, y1, 8.65, EDGE))
    p += [sym(0.8, 30.8, 0.6, 32.0, EDGE),
          sym(0.5, 22.4, 1.0, 31.2, STEEL), part(7.8, 23.0, 9.0, 8.2, 30.0, 9.1, CORE),   # the spine and its seam
          part(3.2, 22.4, 7.6, 4.6, 25.4, 8.4, JOINT), part(11.4, 22.4, 7.6, 12.8, 25.4, 8.4, JOINT)]  # the fins
    return p


def katana_parts():
    p = [sym(0.95, -7.6, 0.95, -6.5, STEEL),               # the pommel (kashira)
         sym(0.75, -6.5, 0.75, 6.0, WRAP),                 # the long hilt, both hands
         sym(2.6, 6.0, 2.6, 6.8, STEEL), sym(1.7, 6.1, 2.7, 6.7, CORE),   # the tsuba and its glow
         sym(0.85, 6.8, 0.75, 8.2, GOLD)]                  # the habaki
    curve = [(8.2, 13.0, 0.0), (13.0, 18.0, 0.2), (18.0, 23.0, 0.45), (23.0, 27.5, 0.75), (27.5, 30.5, 1.05)]
    for y0, y1, dx in curve:
        p.append(sym(1.0, y0, 0.4, y1, BLADE, dx))
        p.append(part(8 + 1.0 + dx - 0.05, y0, 7.65, 8 + 1.0 + dx + 0.3, y1, 8.35, EDGE))   # the edge (its hamon glowing)
    p.append(part(8.8, 30.5, 7.65, 10.2, 32.0, 8.35, EDGE))                                # the point
    return p


def mace_parts():
    p = [sym(1.15, -10.0, 1.15, -8.6, STEEL),              # the pommel
         sym(0.8, -8.6, 0.8, 16.0, SHAFT),                 # the long handle
         sym(0.95, -7.5, 0.95, -1.5, WRAP), sym(0.95, 1.5, 0.95, 7.5, WRAP),  # both hands' grips
         sym(1.9, 16.0, 1.9, 17.6, CASE), sym(1.1, 16.3, 2.0, 17.3, CORE),
         sym(4.0, 17.6, 4.0, 27.0, FLESH)]                 # the head: a great lump of flesh
    p += [part(2.7, 19.0, 5.6, 4.0, 25.2, 10.4, FLESH), part(12.0, 19.6, 5.3, 13.4, 25.6, 10.7, FLESH),
          part(5.6, 19.0, 2.7, 10.4, 25.2, 4.0, FLESH), part(5.3, 19.6, 12.0, 10.7, 25.6, 13.4, FLESH),
          sym(2.6, 27.0, 2.6, 29.4, FLESH)]
    p += [part(3.8, 18.2, 3.8, 4.4, 26.4, 12.2, VEIN), part(11.6, 18.6, 3.8, 12.2, 26.6, 12.2, VEIN),   # veins over it
          sym(4.1, 21.6, 4.1, 22.4, VEIN)]
    p += [part(1.6, 21.6, 7.4, 2.7, 23.0, 8.6, BONE), part(13.4, 22.2, 7.4, 14.5, 23.6, 8.6, BONE),      # bone spikes through it
          part(7.4, 22.0, 1.6, 8.6, 23.4, 2.7, BONE), part(7.4, 21.6, 13.4, 8.6, 23.0, 14.5, BONE),
          sym(0.5, 29.4, 0.5, 31.6, BONE),
          part(4.6, 24.2, 3.8, 5.6, 25.2, 4.0, CORE), part(10.2, 20.4, 12.0, 11.2, 21.4, 12.2, CORE)]       # an eye of red here and there
    return p


DOWN = -1.0  # which way across a standing weapon is down in the hand (checked in the previews)


def launcher_parts():
    d = DOWN
    p = [sym(1.4, -6.0, 1.2, -0.2, STEEL), sym(1.6, -6.6, 1.3, -6.0, TRIM),     # the stock and its butt
         sym(1.8, -0.2, 1.5, 17.0, CASE), sym(1.95, 7.6, 1.65, 8.4, TRIM),     # the body (the case become it)
         part(8 + d * 1.6, 2.2, 7.3, 8 + d * 3.9, 6.6, 8.7, WRAP),            # the pistol grip, below
         part(8 + d * 1.6, 6.6, 7.6, 8 + d * 2.7, 8.0, 8.4, TRIM)]            # the trigger guard
    for dz in (-0.75, 0.75):
        p.append(part(7.2, 17.0, 8 + dz - 0.5, 8.2, 27.0, 8 + dz + 0.5, BLADE))   # the barrels
    p.append(part(7.9, 17.0, 7.5, 8.9, 27.6, 8.5, BLADE))
    p += [sym(2.0, 26.6, 1.7, 28.0, STEEL), sym(1.2, 27.0, 1.2, 28.1, JOINT)]   # the muzzle
    top = 8 - d * 1.8
    p += [part(top, 5.0, 7.0, top - d * 2.4, 13.5, 9.0, CORE),                 # the crystal magazine on top, glowing
          part(top - d * 2.4, 6.5, 7.6, top - d * 3.6, 8.0, 8.4, CORE),
          part(top - d * 2.4, 10.0, 7.7, top - d * 3.2, 12.0, 8.3, CORE)]
    return p


WEAPONS = {"bikaku": blade_parts, "koukaku": spear_parts, "katana": katana_parts, "mace": mace_parts, "launcher": launcher_parts}


def telescoped(parts, f):
    """The weapon {f} out (0..1): every height drawn in toward the hand; the case round the grip, shrinking into it as it comes out."""
    els = []
    for (x0, y0, z0), (x1, y1, z1), cell in parts:
        a = HAND + (y0 - HAND) * f
        b = HAND + (y1 - HAND) * f
        if b - a < 0.05:
            continue
        els.append(box((x0, a, z0), (x1, b, z1), cell))
    if f < 1.0:
        k = 1.0 - f
        half = 2.4 + 3.6 * k
        tall = 2.0 + 4.0 * k
        depth = 1.2 + 0.8 * k
        y0 = HAND + 1.0
        els += [box((8 - half, y0, 8 - depth), (8 + half, y0 + tall, 8 + depth), CASE),
                box((8 - half - 0.15, y0 + tall * 0.45, 8 - depth - 0.15), (8 + half + 0.15, y0 + tall * 0.58, 8 + depth + 0.15), TRIM),
                box((8 - 1.4, y0 + 0.4, 8 + depth), (8 + 1.4, y0 + tall - 0.4, 8 + depth + 0.2), CORE),
                box((8 - 1.4, y0 + 0.4, 8 - depth - 0.2), (8 + 1.4, y0 + tall - 0.4, 8 - depth), CORE)]
    return els


def span(parts):
    return min(min(a[1], b[1]) for a, b, c in parts), max(max(a[1], b[1]) for a, b, c in parts)


# ---------------------------------------------------------------------------------------------------- how each is held

def handheld(third, first):
    t_rot, t_tr, t_sc = third
    f_rot, f_tr, f_sc = first
    return {
        "thirdperson_righthand": {"rotation": t_rot, "translation": t_tr, "scale": [t_sc] * 3},
        "thirdperson_lefthand": {"rotation": [t_rot[0], -t_rot[1], -t_rot[2]], "translation": t_tr, "scale": [t_sc] * 3},
        "firstperson_righthand": {"rotation": f_rot, "translation": f_tr, "scale": [f_sc] * 3},
        "firstperson_lefthand": {"rotation": [f_rot[0], -f_rot[1], -f_rot[2]], "translation": f_tr, "scale": [f_sc] * 3},
    }


# Third person (rotation, translation, scale), then first person. The blade as a sword (vanilla handheld); the two-handed ones and the
# launcher solved on a player's arms in their stance (QuinquePose: the right arm as given there, the left reaching the lower grip): the
# right grip in the right hand, the weapon pointing as its stance has it (the spear and the mace up and forward, the katana in a kamae,
# the launcher level at the hip), the left grip where the left hand reaches (scratchpad fit, within a few hundredths of a block).
HELD = {
    "bikaku": (([0, -90, 55], [0, 4.0, 0.5], 0.85), ([0, -90, 25], [1.13, 2.4, 1.13], 0.55)),
    "koukaku": (([-98, -67, -1], [-1.16, -0.34, -2.32], 0.85), ([0, -90, 25], [1.13, 1.2, 1.13], 0.45)),
    "katana": (([-70, -68, 16], [-1.32, 0.7, -2.5], 0.85), ([0, -90, 25], [1.13, 2.0, 1.13], 0.5)),
    "mace": (([120, -183, -150], [-1.23, -0.6, -2.51], 0.85), ([0, -90, 25], [1.13, 1.6, 1.13], 0.5)),
    "launcher": (([-133, -70, -45], [-0.95, 1.7, -0.25], 0.85), ([0, -90, 25], [1.13, 2.0, 1.13], 0.6)),
}


def weapon_display(kind, lo_hi):
    lo, hi = lo_hi
    a = 8 + (lo - 8) / math.sqrt(2)
    b = 8 + (hi - 8) / math.sqrt(2)
    scale = min(1.0, 14.0 / (b - a))
    centre = (a + b) / 2.0
    shift = round((8 - centre) * scale, 3)
    third, first = HELD[kind]
    d = handheld(third, first)
    d["gui"] = {"rotation": [0, 0, 0], "translation": [shift, shift, 0], "scale": [round(scale, 3)] * 3}
    d["ground"] = {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [round(scale * 0.6, 3)] * 3}
    d["fixed"] = {"rotation": [0, 180, 0], "translation": [-shift, shift, 0], "scale": [round(scale, 3)] * 3}
    return d


CASE_DISPLAY = {
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
ITEMS = {  # kind -> (case item, weapon item)
    "bikaku": ("quinque_case_bikaku", "quinque_bikaku_blade"),
    "koukaku": ("quinque_case_koukaku", "quinque_koukaku_spear"),
    "katana": ("quinque_case_katana", "quinque_katana"),
    "mace": ("quinque_case_mace", "quinque_flesh_mace"),
    "launcher": ("quinque_case_launcher", "quinque_crystal_launcher"),
}


def model(kind, elements, display):
    return {"credit": "HexKagune: tools/quinque_models.py", "texture_size": [32, 32],
            "textures": {"q": f"hexkagune:item/quinque_{kind}", "particle": f"hexkagune:item/quinque_{kind}"},
            "elements": elements, "display": display, "gui_light": "front"}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=1) + "\n")


def main():
    TEXTURES.mkdir(parents=True, exist_ok=True)
    for kind, colours in KINDS.items():
        texture(kind, colours).save(TEXTURES / f"quinque_{kind}.png")
        parts = WEAPONS[kind]()
        held = weapon_display(kind, span(parts))
        frames = {
            "case_closed": model(kind, case_elements(False), CASE_DISPLAY),
            "case_open": model(kind, case_elements(True), CASE_DISPLAY),
            "half": model(kind, telescoped(parts, 0.33), held),
            "nearly": model(kind, telescoped(parts, 0.66), held),
            "full": model(kind, telescoped(parts, 1.0), held),
        }
        for name, data in frames.items():
            for el in data["elements"]:
                for v in el["from"] + el["to"]:
                    assert -16 <= v <= 32, f"{kind} {name}: {v} is out of a model's bounds"
            write(MODELS / "quinque" / f"{kind}_{name}.json", data)
        overrides = [{"predicate": {"hexkagune:deploy": v}, "model": f"hexkagune:item/quinque/{kind}_{name}"} for name, v in FRAMES]
        for item in ITEMS[kind]:
            write(MODELS / f"{item}.json", {"parent": f"hexkagune:item/quinque/{kind}_case_closed", "overrides": overrides})
        print(f"{kind}: {len(parts)} parts, span {span(parts)}")


if __name__ == "__main__":
    main()
