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

1.3.28: an investigator carries its case shut in two more frames of the same model, <kind>_case_carried (wide arms) and
<kind>_case_carried_slim, chosen by the deploy values CARRIED_WIDE / CARRIED_SLIM its property gives for an investigator alone
(QuinqueClient.caseFrame); their hand transform is solved here (carried_display) against its carrying arm (AgentPose.CARRY_X,
CARRY_Z): the handle bar inside its fist, the case hanging plumb below it, latches out, clear of its leg.
"""
import json
import math
import random

import numpy as np
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEXTURES = ROOT / "src" / "main" / "resources" / "assets" / "hexkagune" / "textures" / "item"
MODELS = ROOT / "src" / "main" / "resources" / "assets" / "hexkagune" / "models" / "item"

# Patches: (column, row) on the 8x8 grid of 4px squares.
CASE, TRIM, HANDLE, LATCH, EMBLEM, CORE = (0, 0), (1, 0), (2, 0), (3, 0), (4, 0), (5, 0)
BLADE, EDGE, JOINT, WRAP, STEEL, SHAFT = (0, 1), (1, 1), (2, 1), (3, 1), (4, 1), (5, 1)
FLESH, VEIN, BONE, GOLD = (0, 2), (1, 2), (2, 2), (3, 2)
ENGRAVING = (4, 2)

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
    "dawn": dict(BASE, case=(19, 20, 24), trim=(38, 39, 45), latch=(66, 67, 74), emblem=(99, 78, 39),
                 core=(145, 109, 45), blade=(32, 28, 28), edge=(46, 39, 39), joint=(17, 16, 19),
                 shaft=(31, 26, 24), gold=(157, 122, 55), light=(255, 228, 96)),
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
    patch(BLADE, colours["blade"], "noise" if kind == "dawn" else "vein")
    patch(EDGE, colours["edge"])
    patch(JOINT, colours["joint"])
    patch(WRAP, colours["wrap"], "wrap")
    patch(STEEL, colours["steel"], "plate")
    patch(SHAFT, colours["shaft"])
    patch(FLESH, colours["flesh"], "flesh")
    patch(VEIN, colours["vein"], "vein")
    patch(BONE, colours["bone"], "plate")
    patch(GOLD, colours["gold"], "plate")
    if kind == "dawn":
        for row, pattern in enumerate(("..#...#.", ".#.#....", ".#.#....", "..#...#.")):
            for col, mark in enumerate(pattern):
                color = colours["gold"] if mark == "#" else colours["blade"]
                img.putpixel((16 + col, 8 + row), color + (255,))
        patch(LIGHT, colours["light"], "glow")  # 2.0.30: the light in its parted point (drawn last: nothing else changes)
    return img


def uv(cell):
    cx, cy = cell
    return [cx * 2, cy * 2, cx * 2 + 2, cy * 2 + 2]


def box(frm, to, cell, turned=True, rot=None):
    """An element. {turned}: the standing weapons' -45 degrees about z (the handheld diagonal); {rot}: its own (axis, angle, origin)
    instead (1.3.33: the launcher, built lying along x)."""
    faces = {face: {"uv": uv(cell), "texture": "#q"} for face in ("north", "south", "east", "west", "up", "down")}
    if cell == ENGRAVING:
        for face in ("north", "south", "up", "down"):
            faces[face]["uv"] = [8, 4, 12, 6]
            faces[face]["rotation"] = 90
    element = {"from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to], "faces": faces}
    if rot is not None:
        axis, angle, origin = rot
        element["rotation"] = {"angle": angle, "axis": axis, "origin": [round(v, 3) for v in origin]}
    elif turned:
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
         sym(1.0, 9.5, 1.0, 15.5, WRAP),                   # 1.3.33: the left hand's grip, in front (the right hand behind it)
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
    p = [sym(0.95, -5.1, 0.95, -4.0, STEEL),               # the pommel (kashira)
         sym(0.75, -4.0, 0.75, 6.0, WRAP),                 # the hilt, both hands (1.3.33: a katana's, a third of its blade)
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
         sym(0.95, -4.2, 0.95, 1.0, WRAP), sym(0.95, 1.5, 0.95, 7.5, WRAP),   # both hands' grips
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


# 1.3.33: the launcher is built lying along x (the muzzle at +x, its top at +y, its right side at +z), not standing on the diagonal:
# its elements keep their own small turns (a raked grip, a round barrel, crystals) and its display transforms are its own (solved below
# for its rifle hold). The owner: "comically bad... I was expecting something less squared... should be like a proper gun".
GUN_GRIP = (1.3, 5.0, 8.0)    # the right hand's grip on its pistol grip (the fist's middle), model pixels
GUN_FORE = (11.1, 4.6, 8.0)   # the left hand's, on the fore grip under the hand guard


def launcher_parts():
    """(from, to, patch, rotation or None) lying along x: a skeleton stock, a white receiver (the case become it) with a glowing
    crystal feed on top, a raked pistol grip, a fore grip under a dark vented hand guard, a round barrel and a slotted muzzle brake."""
    g = []

    def a(frm, to, cell, rot=None):
        g.append((frm, to, cell, rot))

    raked = ("z", -22.5, (2.5, 8.0, 8.0))
    # The stock: a pad, a comb, a lower bar and a strut rising to the grip (a thumb hole between), a glowing seam on each side.
    a((-6.5, 6.2, 6.7), (-5.5, 11.6, 9.3), HANDLE)
    a((-5.5, 9.6, 6.9), (1.6, 11.6, 9.1), CASE)
    a((-5.5, 6.6, 7.1), (-2.0, 9.6, 8.9), CASE)
    a((-2.8, 7.4, 7.2), (2.2, 8.6, 8.8), CASE, ("z", 22.5, (-0.3, 8.0, 8.0)))
    a((-5.0, 10.0, 9.1), (0.6, 10.4, 9.25), CORE)
    a((-5.0, 10.0, 6.75), (0.6, 10.4, 6.9), CORE)
    # The receiver: stepped (a broad body, a narrower top), a dark band, a crystal window each side, the charging handle.
    a((1.5, 8.0, 6.5), (10.2, 10.6, 9.5), CASE)
    a((1.5, 10.6, 6.8), (10.2, 11.8, 9.2), CASE)
    a((6.6, 7.8, 6.35), (7.2, 11.9, 9.65), TRIM)
    a((2.8, 8.7, 9.5), (6.2, 10.3, 9.65), CORE)
    a((2.8, 8.7, 6.35), (6.2, 10.3, 6.5), CORE)
    a((8.0, 10.6, 6.2), (9.4, 11.2, 6.8), STEEL)
    a((7.4, 6.6, 7.0), (10.0, 8.0, 9.0), TRIM)
    a((7.7, 6.9, 9.0), (9.7, 7.7, 9.1), CORE)
    # Its crystal feed on top: a glowing tube in three bands, a spine, two crystals jutting from it; the rear sight.
    a((2.2, 11.8, 7.0), (9.8, 13.6, 9.0), CORE)
    for x in (2.0, 5.7, 9.3):
        a((x, 11.7, 6.85), (x + 0.6, 13.8, 9.15), TRIM)
    a((2.6, 13.6, 7.6), (9.3, 14.0, 8.4), TRIM)
    a((3.6, 13.3, 7.4), (5.0, 15.3, 8.6), CORE, ("y", 45, (4.3, 14.3, 8.0)))
    a((6.8, 13.4, 7.6), (7.8, 14.8, 8.4), CORE, ("y", 45, (7.3, 14.1, 8.0)))
    a((1.6, 11.8, 7.6), (2.2, 12.8, 8.4), TRIM)
    # The pistol grip, raked back, and its cap; the trigger guard and the trigger.
    a((1.4, 3.0, 7.0), (3.6, 8.0, 9.0), WRAP, raked)
    a((1.2, 2.4, 6.9), (3.8, 3.0, 9.1), TRIM, raked)
    a((3.8, 6.4, 7.6), (7.2, 6.8, 8.4), TRIM)
    a((6.8, 6.8, 7.6), (7.2, 8.0, 8.4), TRIM)
    a((4.6, 6.8, 7.8), (5.2, 8.0, 8.2), STEEL)
    # The hand guard: dark, a rail on top, a chamfer under it, four glowing vents each side; the fore grip under it.
    a((10.2, 8.2, 6.6), (21.0, 11.2, 9.4), BLADE)
    a((10.4, 11.2, 7.3), (20.8, 11.7, 8.7), TRIM)
    a((10.2, 7.8, 7.0), (21.0, 8.2, 9.0), BLADE)
    for x in (12.4, 14.6, 16.8, 19.0):
        a((x, 9.0, 9.4), (x + 1.2, 10.4, 9.5), CORE)
        a((x, 9.0, 6.5), (x + 1.2, 10.4, 6.6), CORE)
    a((10.2, 3.0, 7.2), (12.0, 7.8, 8.8), HANDLE)
    a((10.0, 2.6, 7.1), (12.2, 3.0, 8.9), TRIM)
    # The barrel (round: a second box turned 45 degrees about it), the gas tube, the front sight, the muzzle brake and its glow.
    a((21.0, 9.1, 7.3), (28.0, 10.5, 8.7), STEEL)
    a((21.0, 9.1, 7.3), (28.0, 10.5, 8.7), STEEL, ("x", 45, (24.5, 9.8, 8.0)))
    a((21.0, 10.6, 7.6), (25.0, 11.2, 8.4), TRIM)
    a((24.2, 11.2, 7.7), (24.8, 12.4, 8.3), TRIM)
    a((27.6, 8.6, 6.8), (31.0, 11.0, 9.2), JOINT)
    for x in (28.2, 29.6):
        a((x, 8.9, 9.2), (x + 0.6, 10.7, 9.3), CORE)
        a((x, 8.9, 6.7), (x + 0.6, 10.7, 6.8), CORE)
    a((31.0, 9.3, 7.5), (31.2, 10.3, 8.5), CORE)
    return g


def lerp_profile(points, x):
    """The radius at {x} along a profile of (x, radius) points (straight between them, held past the ends)."""
    if x <= points[0][0]:
        return points[0][1]
    for (xa, ra), (xb, rb) in zip(points, points[1:]):
        if x <= xb:
            return ra + (rb - ra) * (x - xa) / (xb - xa)
    return points[-1][1]


# 2.0.30 (the owner: "smooth out ... because it's really squared, sort of, just to match ours better"): the spike's radius along the
# lance (the recess at its base, the thicker middle, then the long straight taper to the point), and the guard's taper.
DAWN_SPIKE = [(12.2, 0.60), (13.1, 0.55), (13.6, 0.44), (14.7, 0.44), (15.3, 0.57), (16.5, 0.63), (17.8, 0.55), (19.3, 0.46),
              (21.2, 0.38), (23.5, 0.31), (25.9, 0.24), (28.1, 0.17), (30.0, 0.10), (31.8, 0.04)]
DAWN_TAPER = [(1.50, 2.20), (2.15, 2.10), (3.45, 1.89), (4.75, 1.67), (6.05, 1.46), (7.35, 1.26), (8.675, 1.06), (10.075, 0.87),
              (11.65, 0.71), (12.5, 0.66)]
# 2.0.30: the point opens from here as a shot leaves it (the "fire" frame): its two halves parted, a light between them.
DAWN_SPLIT = 26.1
LIGHT = (5, 2)


def dawn_parts(open_tip=False):
    """A narrow black lance with a hollow flared guard, engraved gold bands, and a hooked knuckle guard. 2.0.30: round where it was
    square: the guard's rings 16-sided (8 once small) in fine steps, the spike, grip and butt 8-sided along smooth profiles, the spike's
    two ridges turning about it along its middle; {open_tip}: its point split in two and parted (22.5 degrees each way) about a light."""
    parts = []

    def add(x0, x1, y0, y1, z0, z1, cell, rotation=None):
        parts.append(((x0, 8 + y0, 8 + z0), (x1, 8 + y1, 8 + z1), cell, rotation))

    def turned(angle):
        return None if angle == 0 else ("x", angle, (8, 8, 8))

    def rod(x0, x1, r, cell):
        """An 8-sided rod along x: the square and the same turned 45 degrees."""
        add(x0, x1, -r, r, -r, r, cell)
        add(x0, x1, -r, r, -r, r, cell, turned(45))

    def ring(x0, x1, radius, cell, wall=0.30):
        """One step of the hollow guard: 16 slats round it (8 once it is small), its open middle left for the shaft."""
        sides = 16 if radius >= 1.2 else 8
        half = radius * math.tan(math.pi / sides) + 0.035
        for sign in (-1, 1):
            low, high = sorted((sign * radius, sign * (radius - wall)))
            if sides == 16:
                for angle in (0, 22.5, -22.5, 45, -45):
                    add(x0, x1, low, high, -half, half, cell, turned(angle))
                for angle in (0, 22.5, -22.5):
                    add(x0, x1, -half, half, low, high, cell, turned(angle))
            else:
                add(x0, x1, low, high, -half, half, cell)
                add(x0, x1, -half, half, low, high, cell)
                for angle in (-45, 45):
                    add(x0, x1, low, high, -half, half, cell, turned(angle))

    # The guard: the rim and its engraved gold bands as before (the long plain stretch between them in two), then its taper in fine
    # steps (0.55 px), each barely smaller than the last.
    for x0, x1, radius, cell in [
        (-2.8, -2.45, 3.04, JOINT), (-2.45, -2.20, 2.96, BLADE),
        (-2.20, -2.03, 2.92, GOLD), (-2.03, -1.14, 2.86, ENGRAVING),
        (-1.14, -0.97, 2.72, GOLD), (-0.97, -0.26, 2.66, BLADE), (-0.26, 0.45, 2.56, BLADE),
        (0.45, 0.60, 2.43, GOLD), (0.60, 1.35, 2.36, ENGRAVING),
        (1.35, 1.50, 2.24, GOLD),
    ]:
        ring(x0, x1, radius, cell)
    steps = 20
    for i in range(steps):
        x0 = 1.50 + (12.5 - 1.50) * i / steps
        x1 = 1.50 + (12.5 - 1.50) * (i + 1) / steps
        ring(x0, x1, lerp_profile(DAWN_TAPER, (x0 + x1) / 2), BLADE)

    # The spike: 8-sided along its profile, in short steps (finer toward the point).
    edges = [12.2]
    while edges[-1] < 31.8 - 1e-6:
        edges.append(min(31.8, edges[-1] + (0.7 if edges[-1] < 24.0 else 0.9)))
    for x0, x1 in zip(edges, edges[1:]):
        r = lerp_profile(DAWN_SPIKE, (x0 + x1) / 2)
        if open_tip and x0 >= DAWN_SPLIT - 1e-6:
            # Its point parted: each half a half-rod, turned up or down about the split.
            add(x0, x1, 0.0, r, -r, r, BLADE, ("z", 22.5, (DAWN_SPLIT, 8, 8)))
            add(x0, x1, -r, 0.0, -r, r, BLADE, ("z", -22.5, (DAWN_SPLIT, 8, 8)))
        else:
            rod(x0, x1, r, BLADE)
    if open_tip:
        # The light gathering where it parts (the lightning ball about to leave it).
        add(DAWN_SPLIT - 0.35, DAWN_SPLIT + 1.1, -0.36, 0.36, -0.36, 0.36, LIGHT)
        add(DAWN_SPLIT - 0.35, DAWN_SPLIT + 1.1, -0.36, 0.36, -0.36, 0.36, LIGHT, turned(45))
    # Its two ridges along the thicker middle, turning about it a little at a time (a shallow twist).
    ridge = [(15.3, 16.9, -45), (16.9, 18.5, -22.5), (18.5, 20.1, 0), (20.1, 21.7, 22.5), (21.7, 23.3, 45)]
    for x0, x1, angle in ridge:
        r = lerp_profile(DAWN_SPIKE, (x0 + x1) / 2)
        for sign in (-1, 1):
            low, high = sorted((sign * (r - 0.05), sign * (r + 0.07)))
            add(x0, x1, -0.11, 0.11, low, high, EDGE, turned(angle))

    # The grip and its butt spike, 8-sided.
    rod(-9.4, 12.5, 0.31, SHAFT)
    for x0, x1, radius in [(-10.2, -9.4, 0.42), (-11.0, -10.2, 0.33), (-12.7, -11.0, 0.23),
                           (-14.0, -12.7, 0.14), (-14.8, -14.0, 0.06)]:
        rod(x0, x1, radius, JOINT)

    # Black hooked guard on the underside, with a raised root and a narrow return.
    add(-5.4, -2.55, -1.80, -1.20, -0.36, 0.36, SHAFT)
    add(-3.05, -2.52, -1.58, -0.28, -0.42, 0.42, JOINT)
    add(-7.45, -5.05, -1.77, -1.26, -0.31, 0.31, JOINT, ("z", -22.5, (-5.2, 6.4, 8)))
    add(-8.85, -6.9, -1.35, -1.03, -0.22, 0.22, SHAFT, ("z", -45, (-7.25, 6.8, 8)))
    add(-9.55, -8.1, -0.63, -0.45, -0.13, 0.13, EDGE, ("z", -22.5, (-8.4, 7.45, 8)))
    return parts


GUN_GRIP_DAWN = (-5.7, 8.0, 8.0)
GUNS = {"launcher", "dawn"}


def gun_telescoped(parts, f, grip=GUN_GRIP):
    """The gun {f} out (0..1): every length drawn in toward its grip along x, the case round the grip shrinking into it."""
    els = []
    gx = grip[0]
    for frm, to, cell, rot in parts:
        a = gx + (frm[0] - gx) * f
        b = gx + (to[0] - gx) * f
        if b - a < 0.05:
            continue
        turn = None
        if rot is not None:
            axis, angle, origin = rot
            turn = (axis, angle, (gx + (origin[0] - gx) * f, origin[1], origin[2]))
        element = box((a, frm[1], frm[2]), (b, to[1], to[2]), cell, rot=turn, turned=False)
        if grip == GUN_GRIP_DAWN and cell in {GOLD, ENGRAVING}:
            # Color only the outer wall of each slat. The open end and interior are black.
            outside = ("up" if frm[1] > 8 else "down") if to[1] - frm[1] < to[2] - frm[2] else ("south" if frm[2] > 8 else "north")
            for side, face in element["faces"].items():
                if side != outside:
                    face["uv"] = uv(JOINT)
                    face.pop("rotation", None)
        els.append(element)
    if f < 1.0:
        k = 1.0 - f
        half = 2.4 + 3.6 * k
        tall = 2.0 + 4.0 * k
        depth = 1.2 + 0.8 * k
        cx, y0 = gx + 2.0, 8.0 - tall * 0.3
        els += [box((cx - half, y0, 8 - depth), (cx + half, y0 + tall, 8 + depth), CASE, turned=False),
                box((cx - half - 0.15, y0 + tall * 0.45, 8 - depth - 0.15), (cx + half + 0.15, y0 + tall * 0.58, 8 + depth + 0.15), TRIM, turned=False),
                box((cx - 1.4, y0 + 0.4, 8 + depth), (cx + 1.4, y0 + tall - 0.4, 8 + depth + 0.2), CORE, turned=False),
                box((cx - 1.4, y0 + 0.4, 8 - depth - 0.2), (cx + 1.4, y0 + tall - 0.4, 8 - depth), CORE, turned=False)]
    return els


WEAPONS = {"bikaku": blade_parts, "koukaku": spear_parts, "katana": katana_parts, "mace": mace_parts, "launcher": launcher_parts, "dawn": dawn_parts}


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
    return min(min(q[0][1], q[1][1]) for q in parts), max(max(q[0][1], q[1][1]) for q in parts)


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


# ---------------------------------------------------------------------------------------------------- 1.3.33: the holds, solved

# The body's frame here is the chest's (RigPose: model pixels, x to its left, y down, z to its back; the chest's twist taken out): the
# right shoulder's pivot at (-5, 2, 0), the left's at (5, 2, 0), each arm turned as ModelPart turns it (Rz Ry Rx) and a straight stick
# (an investigator has no elbows). An arm's turn is written as where it points: aim(pitch, yaw, roll) = Ry(yaw) Rx(pitch) Ry(roll):
# roll about its own length as it hangs, pitch forward (-) or back, yaw round to its left (-) or right (+), the way QuinquePose does.
# Each two-handed quinque's stance names the chest's twist, the right arm's pitch and yaw, and which way the weapon points (the look's
# frame: the target straight ahead at -z); its roll is found (the thumb's side of the fist, the arm's -z, along the grip's way out),
# then its third person transform: the weapon pointing so, its right grip in the middle of the fist (QuinqueTests and AgentTests check
# it through vanilla's chain). The left arm reaches its grip (QuinqueGrip does the same every frame, so it stays on it as it swings).

def rx3(a):
    c, s_ = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s_], [0, s_, c]])


def ry3(a):
    c, s_ = math.cos(a), math.sin(a)
    return np.array([[c, 0, s_], [0, 1, 0], [-s_, 0, c]])


def rz3(a):
    c, s_ = math.cos(a), math.sin(a)
    return np.array([[c, -s_, 0], [s_, c, 0], [0, 0, 1]])


def zyx3(x, y, z):
    return rz3(z) @ ry3(y) @ rx3(x)


def aim3(pitch, yaw, roll):
    return ry3(yaw) @ rx3(pitch) @ ry3(roll)


def angles3(m):
    """ModelPart's three angles (Rz Ry Rx) of a turn, as RigPose.leanRotation reads them."""
    sy = max(-1.0, min(1.0, -m[2][0]))
    ry = math.asin(sy)
    if abs(sy) < 0.999999:
        return math.atan2(m[2][1], m[2][2]), ry, math.atan2(m[1][0], m[0][0])
    return math.atan2(-m[1][2], m[1][1]), ry, 0.0


RIGHT_PIVOT = np.array([-5.0, 2.0, 0.0])
LEFT_PIVOT = np.array([5.0, 2.0, 0.0])
FIST_Y = 9.0
FIST_X = 0.75       # the fist's middle across the arm: a wide arm's 1, a slim one's 0.5 (solved between them)
SLIM_SHIFT = 0.25   # PlayerModel.translateToHand moves a slim arm's pivot 0.5 out for the item (solved between)
LAYER = rx3(math.radians(-90)) @ ry3(math.radians(180))
LAYER_T = np.array([1 / 16, 0.125, -0.625])
TURN45 = rz3(math.radians(-45))


def standing(y):
    """A point on a standing weapon's line (x 8, z 8) at height {y}, where its turned element puts it."""
    return 8.0 + TURN45 @ (np.array([8.0, y, 8.0]) - 8.0)


def unit3(v):
    v = np.array(v, dtype=float)
    return v / np.linalg.norm(v)


# Per two-handed quinque: the chest's twist; the right arm's pitch and yaw; the weapon's way out (tip) and its side (the gun's top, a
# blade's edge) in the look's frame; its scale; the right and left grips (model pixels as drawn).
SIN35, COS35 = math.sin(math.radians(35)), math.cos(math.radians(35))
STANCES = {
    # 1.3.33: the one-handed Bikaku blade, en garde: the arm down and forward, the blade level before it. Held 50 degrees off the line
    # of the forearm (vanilla's sword is square to it: a straight arm out to the side then showed only its hilt, "who grabs a sword by
    # the tip of it", the owner said).
    "bikaku": dict(twist=0.0, pitch=-0.7, yaw=-0.25, roll=0.0, theta=50.0, scale=0.85, right=5.0, left=5.0),
    # A rifle: bladed (the left shoulder toward the target), the stock in the right shoulder, the barrel along the look.
    "launcher": dict(twist=0.85, pitch=-1.62, yaw=-1.12, tip=(0, 0, -1), side=(0, -1, 0), scale=0.62, gun=True,
                     right=GUN_GRIP, left=GUN_FORE),
    "dawn": dict(twist=0.36, pitch=-1.05, yaw=-0.42, tip=(0, -0.02, -1), side=(0, -1, 0), scale=0.74, gun=True,
                 right=GUN_GRIP_DAWN, left=GUN_GRIP_DAWN),
    # The spear low and level at the hip, its point at the chest of what it faces, the left hand in front on the shaft (sliding along
    # it, slide: from .. to, as the spear is driven through it in a thrust).
    "koukaku": dict(twist=1.0, pitch=-1.0, yaw=-1.05, tip=(0, 0.06, -1), side=(1, 0, 0), scale=0.85, right=4.5, left=12.5,
                    slide=(9.0, 17.5)),
    # The katana in chudan: the hands before the navel, the point up at the throat, its edge down.
    "katana": dict(twist=0.2, pitch=-1.15, yaw=-0.75, tip=(0, -SIN35, -COS35), side=(0, COS35, -SIN35), scale=0.85, right=3.4, left=-1.3),
    # The mace raised before the body in both hands, its head up and forward over them (straight arms can swing it from there: a
    # shouldered mace can never come over in front without the wrists).
    "mace": dict(twist=0.25, pitch=-1.15, yaw=-0.75, tip=(0, -0.82, -0.57), side=(0, 0.57, -0.82), scale=0.85, right=3.5, left=-1.5,
                 slide=(-7.5, 1.0)),
}


def grip_point(stance, which):
    v = stance[which]
    return np.array(v, dtype=float) if stance.get("gun") else standing(v)


def stance_frame(stance):
    """Where the weapon's own axes go (columns: its model x, y, z) in the chest's frame."""
    long_axis, edge_axis = TURN45 @ np.array([0, 1.0, 0]), TURN45 @ np.array([1.0, 0, 0])
    if "theta" in stance:
        # One-handed: the blade out of the fist {theta} degrees off the forearm's own line toward the thumb's side (the arm's -z), its
        # edge on the side away from the forearm (a sword held with the wrist cocked a little, not square to the arm as vanilla's).
        th = math.radians(stance["theta"])
        arm = aim3(stance["pitch"], stance["yaw"], stance["roll"])
        blade = arm @ np.array([0.0, math.cos(th), -math.sin(th)])
        edge = arm @ np.array([0.0, math.sin(th), math.cos(th)])
        want = np.column_stack([edge, blade, np.cross(edge, blade)])
        have = np.column_stack([edge_axis, long_axis, np.cross(edge_axis, long_axis)])
        return want @ have.T
    twist = ry3(-stance["twist"])
    tip = unit3(twist @ np.array(stance["tip"], dtype=float))
    side = twist @ np.array(stance["side"], dtype=float)
    side = unit3(side - tip * float(side @ tip))
    if stance.get("gun"):
        # x the muzzle's way, y its top, z its right side (x cross y).
        return np.column_stack([tip, side, np.cross(tip, side)])
    # Standing and turned: its length is model (1, 1, 0), its edge (standing x) model (1, -1, 0).
    want = np.column_stack([side, tip, np.cross(side, tip)])
    have = np.column_stack([edge_axis, long_axis, np.cross(edge_axis, long_axis)])
    return want @ have.T


def right_arm(stance):
    """The right arm's turn in the stance: its pitch and yaw, its roll the one that best lays the grip through its fist."""
    if "roll" in stance:
        return stance["roll"], aim3(stance["pitch"], stance["yaw"], stance["roll"])
    frame = stance_frame(stance)
    through = frame[:, 1] if stance.get("gun") else frame @ (TURN45 @ np.array([0, 1.0, 0]))
    best = None
    for i in range(720):
        roll = -math.pi + i * math.pi / 360
        m = aim3(stance["pitch"], stance["yaw"], roll)
        score = float(-m[:, 2] @ through)
        if best is None or score > best[0]:
            best = (score, roll, m)
    return best[1], best[2]


def solve_hold(kind):
    """The third person transform (rotation XYZ degrees, translation pixels, scale) for {kind}'s stance."""
    st = STANCES[kind]
    roll, arm = right_arm(st)
    frame = stance_frame(st)
    rot = (arm @ LAYER).T @ frame
    b = math.asin(max(-1.0, min(1.0, rot[0, 2])))
    if abs(rot[0, 2]) < 0.9999:
        a = math.atan2(-rot[1, 2], rot[2, 2])
        c = math.atan2(-rot[0, 1], rot[0, 0])
    else:
        # Square on y (gimbal lock): only one of x and z is free; z kept 0, x read off the rest exactly.
        c = 0.0
        a = math.atan2(rot[2, 1], rot[1, 1])
    angles = [round(math.degrees(v), 2) for v in (a, b, c)]
    r = rx3(math.radians(angles[0])) @ ry3(math.radians(angles[1])) @ rz3(math.radians(angles[2]))
    scale = st["scale"]
    item_pivot = RIGHT_PIVOT + np.array([SLIM_SHIFT, 0, 0])
    fist = RIGHT_PIVOT + arm @ np.array([-FIST_X, FIST_Y, 0.0])
    grip = grip_point(st, "right")
    t = LAYER.T @ (arm.T @ (fist - item_pivot) / 16.0) - LAYER_T - r @ (scale * (grip / 16.0 - 0.5))
    translation = [round(float(v) * 16.0, 3) for v in t]
    return roll, arm, angles, translation, scale


def item_point(arm, angles, translation, scale, m):
    """A model point (pixels) in the chest's frame (pixels), held in the right hand."""
    r = rx3(math.radians(angles[0])) @ ry3(math.radians(angles[1])) @ rz3(math.radians(angles[2]))
    local = LAYER @ (LAYER_T + np.array(translation) / 16.0 + r @ (scale * (np.array(m, dtype=float) / 16.0 - 0.5)))
    return RIGHT_PIVOT + np.array([SLIM_SHIFT, 0, 0]) + 16.0 * (arm @ local)


def left_arm(stance, arm, angles, translation, scale):
    """The left arm pointed at its grip (a stick: its fist as near it as a straight arm gets), its roll laying the grip through its
    fist as the right's does; and how far the fist misses (pixels: + short of it)."""
    grip = item_point(arm, angles, translation, scale, grip_point(stance, "left"))
    way = unit3(grip - LEFT_PIVOT)
    frame = stance_frame(stance)
    through = frame[:, 1] if stance.get("gun") else frame @ (TURN45 @ np.array([0, 1.0, 0]))
    fist = np.array([FIST_X, FIST_Y, 0.0])
    best = None
    for i in range(720):
        roll = -math.pi + i * math.pi / 360
        base = ry3(roll)
        d0 = unit3(base @ fist)
        axis = np.cross(d0, way)
        sin = np.linalg.norm(axis)
        cos = float(d0 @ way)
        if sin < 1e-9:
            q = np.eye(3)
        else:
            k = axis / sin
            kx = np.array([[0, -k[2], k[1]], [k[2], 0, -k[0]], [-k[1], k[0], 0]])
            q = np.eye(3) + sin * kx + (1 - cos) * (kx @ kx)
        m = q @ base
        score = float(-m[:, 2] @ through)
        if best is None or score > best[0]:
            best = (score, m)
    m = best[1]
    miss = float(np.linalg.norm(grip - LEFT_PIVOT) - np.linalg.norm(fist))
    return angles3(m), miss


def handheld(third, first):
    t_rot, t_tr, t_sc = third
    f_rot, f_tr, f_sc = first
    return {
        "thirdperson_righthand": {"rotation": t_rot, "translation": t_tr, "scale": [t_sc] * 3},
        "thirdperson_lefthand": {"rotation": [t_rot[0], -t_rot[1], -t_rot[2]], "translation": t_tr, "scale": [t_sc] * 3},
        "firstperson_righthand": {"rotation": f_rot, "translation": f_tr, "scale": [f_sc] * 3},
        "firstperson_lefthand": {"rotation": [f_rot[0], -f_rot[1], -f_rot[2]], "translation": f_tr, "scale": [f_sc] * 3},
    }


# First person (rotation, translation, scale): the blade as a sword; the launcher pointing into the screen from the lower right.
FIRST = {
    "bikaku": ([0, -90, 25], [1.13, 2.4, 1.13], 0.55),
    "koukaku": ([0, -90, 25], [1.13, 1.2, 1.13], 0.45),
    "katana": ([0, -90, 25], [1.13, 2.0, 1.13], 0.5),
    "mace": ([0, -90, 25], [1.13, 1.6, 1.13], 0.5),
    "dawn": ([0, 100, 0], [-2.0, 3.0, -5.0], 0.58),
    "launcher": ([0, 100, 0], [-2.0, 3.0, -5.0], 0.6),
}


def weapon_display(kind, lo_hi):
    roll, arm, angles, translation, scale = solve_hold(kind)
    d = handheld((angles, translation, scale), FIRST[kind])
    if kind == "dawn":
        d["gui"] = {"rotation": [15, -20, 20], "translation": [0, 0, 0], "scale": [0.34] * 3}
        d["ground"] = {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.28] * 3}
        d["fixed"] = d["gui"]
        return d
    if kind in GUNS:
        # Side on in a slot, the muzzle to the right, a little turned to show its top.
        d["gui"] = {"rotation": [15, -25, 0], "translation": [-1.6, -0.6, 0], "scale": [0.4] * 3}
        d["ground"] = {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.3] * 3}
        d["fixed"] = {"rotation": [0, 180, 0], "translation": [1.6, -0.6, 0], "scale": [0.4] * 3}
        return d
    lo, hi = lo_hi
    a = 8 + (lo - 8) / math.sqrt(2)
    b = 8 + (hi - 8) / math.sqrt(2)
    scale = min(1.0, 14.0 / (b - a))
    centre = (a + b) / 2.0
    shift = round((8 - centre) * scale, 3)
    d["gui"] = {"rotation": [0, 0, 0], "translation": [shift, shift, 0], "scale": [round(scale, 3)] * 3}
    d["ground"] = {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [round(scale * 0.6, 3)] * 3}
    d["fixed"] = {"rotation": [0, 180, 0], "translation": [-shift, shift, 0], "scale": [round(scale, 3)] * 3}
    return d


HOLDS_JAVA = ROOT / "src" / "main" / "java" / "com" / "hex" / "kagune" / "control" / "client" / "animation" / "QuinqueHolds.java"
JAVA_KINDS = {"bikaku": "BLADE", "koukaku": "SPEAR", "katana": "KATANA", "mace": "MACE", "launcher": "LAUNCHER", "dawn": "DAWN"}


def write_holds():
    """QuinqueHolds.java: each stance and third person transform as solved here, for QuinquePose and QuinqueGrip (never by hand)."""
    def f(v):
        return f"{float(v):.5f}F"

    def arr(vs):
        return "new float[] {" + ", ".join(f(v) for v in vs) + "}"

    cases = []
    for kind, name in JAVA_KINDS.items():
        st = STANCES[kind]
        roll, arm, angles, translation, scale = solve_hold(kind)
        (lx, ly, lz), miss = left_arm(st, arm, angles, translation, scale)
        ends = st.get("slide")
        slide = (standing(ends[0]), standing(ends[1])) if ends else (grip_point(st, "left"), grip_point(st, "left"))
        print(f"{kind}: roll {roll:.3f}, third {angles} {translation} x{scale}; left arm ({lx:.3f}, {ly:.3f}, {lz:.3f}) misses {miss:+.2f} px")
        cases.append(f"            case {name} -> new Hold({f(st['twist'])}, {f(st['pitch'])}, {f(st['yaw'])}, {f(roll)},\n"
                     f"                {arr((lx, ly, lz))},\n"
                     f"                {arr(angles)}, {arr(translation)}, {f(scale)},\n"
                     f"                {arr(grip_point(st, 'right'))}, {arr(grip_point(st, 'left'))},\n"
                     f"                {arr(slide[0])}, {arr(slide[1])});")
    HOLDS_JAVA.write_text("""package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.quinque.Quinque;

/**
 * 1.3.33: each two-handed quinque's stance and the third person transform its model is drawn with in it, solved together by
 * tools/quinque_models.py (generated: never edit by hand; run the script). The chest's twist; the right arm's pitch, yaw and roll
 * (QuinqueGrip.aim); the left arm reaching its grip (x, y, z: RigPose's, as the stance's hint); the model's thirdperson_righthand
 * rotation (XYZ degrees), translation (pixels) and scale; its right and left grips (model pixels, as drawn); the stretch of it the
 * left hand may slide along (the spear's shaft; the others' grip itself).
 */
public final class QuinqueHolds {
    private QuinqueHolds() {
    }

    public record Hold(float twist, float pitch, float yaw, float roll, float[] left, float[] rotation, float[] translation, float scale,
                       float[] rightGrip, float[] leftGrip, float[] slideFrom, float[] slideTo) {
    }

    /** {@code q}'s hold, or null for the one-handed blade. */
    public static Hold of(Quinque q) {
        return switch (q) {
""" + "\n".join(cases) + """
            default -> null;
        };
    }
}
""")


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

# ---------------------------------------------------------------------------------------------------- 1.3.28: an investigator's grip

# Its carrying arm (AgentPose.CARRY_X / CARRY_Z: the right arm hanging, a touch out from the body) and the deploy values that pick its
# frames (AgentPose.CARRIED_WIDE / CARRIED_SLIM: under the first opening frame's 0.2).
CARRY_X, CARRY_Z = 0.0, 0.1
CARRIED_WIDE, CARRIED_SLIM = 0.05, 0.1
CASE_SCALE = 0.62
GRIP = (8.0, 13.95, 8.0)  # the handle bar's middle (model pixels)


def _t(x, y, z):
    m = np.eye(4); m[:3, 3] = (x, y, z); return m


def _rx(a):
    c, s_ = math.cos(a), math.sin(a); m = np.eye(4); m[1, 1] = c; m[1, 2] = -s_; m[2, 1] = s_; m[2, 2] = c; return m


def _ry(a):
    c, s_ = math.cos(a), math.sin(a); m = np.eye(4); m[0, 0] = c; m[0, 2] = s_; m[2, 0] = -s_; m[2, 2] = c; return m


def _rz(a):
    c, s_ = math.cos(a), math.sin(a); m = np.eye(4); m[0, 0] = c; m[0, 1] = -s_; m[1, 0] = s_; m[1, 1] = c; return m


def arm_frame(slim, for_item):
    """The right arm's frame in the entity's (blocks; x its right, y up, z its back; the feet at 0), as LivingEntityRenderer and the
    arm's ModelPart turn it (Rz Ry Rx). For the item, PlayerModel.translateToHand moves a slim arm's pivot half a pixel in."""
    pivot_x = -5.0 + (0.5 if slim and for_item else 0.0)
    pivot_y = 2.5 if slim else 2.0
    m = np.diag([-1.0, -1.0, 1.0, 1.0]) @ _t(0, -1.501, 0)
    return m @ _t(pivot_x / 16, pivot_y / 16, 0) @ _rz(CARRY_Z) @ _rx(CARRY_X)


def fist(slim):
    """The middle of its fist, a pixel above the end of its arm (entity frame, blocks)."""
    centre_x = -0.5 if slim else -1.0
    return (arm_frame(slim, False) @ np.array([centre_x / 16, 9.0 / 16, 0.0, 1.0]))[:3]


def hand_layer(slim):
    """ItemInHandLayer's right hand: the arm, then Rx(-90) Ry(180) and its offset."""
    return arm_frame(slim, True) @ _rx(math.radians(-90)) @ _ry(math.radians(180)) @ _t(1 / 16, 0.125, -0.625)


def carried_display(slim):
    """The third person transform (rotation XYZ degrees, translation pixels) that hangs the case from its fist: model y up, the
    latches' side (model +z) out to its right, its length (model x) along its stride."""
    layer = hand_layer(slim)
    want = np.array([[0.0, 0.0, 1.0], [0.0, 1.0, 0.0], [-1.0, 0.0, 0.0]])  # columns: where model x, y, z go
    rot = layer[:3, :3].T @ want
    b = math.asin(max(-1.0, min(1.0, rot[0, 2])))
    a = math.atan2(-rot[1, 2], rot[2, 2])
    c = math.atan2(-rot[0, 1], rot[0, 0])
    angles = [round(math.degrees(v), 2) for v in (a, b, c)]
    # Rounded angles, then the translation that puts the grip in the fist with them (so the rounding moves nothing that matters).
    r = _rx(math.radians(angles[0])) @ _ry(math.radians(angles[1])) @ _rz(math.radians(angles[2]))
    grip_local = r @ np.diag([CASE_SCALE] * 3 + [1.0]) @ _t(-0.5, -0.5, -0.5) @ np.array([g / 16 for g in GRIP] + [1.0])
    target = np.linalg.inv(layer) @ np.append(fist(slim), 1.0)
    shift = [round(float(v) * 16, 3) for v in (target[:3] - grip_local[:3])]
    return angles, shift


def carried(slim):
    """CASE_DISPLAY with its third person hand solved for an investigator's carrying arm."""
    angles, shift = carried_display(slim)
    d = dict(CASE_DISPLAY)
    third = {"rotation": angles, "translation": shift, "scale": [CASE_SCALE] * 3}
    d["thirdperson_righthand"] = third
    d["thirdperson_lefthand"] = {"rotation": [angles[0], -angles[1], -angles[2]], "translation": shift, "scale": [CASE_SCALE] * 3}
    return d


FRAMES = [("case_open", 0.2), ("half", 0.45), ("nearly", 0.7), ("full", 0.95)]
ITEMS = {  # kind -> (case item, weapon item)
    "bikaku": ("quinque_case_bikaku", "quinque_bikaku_blade"),
    "koukaku": ("quinque_case_koukaku", "quinque_koukaku_spear"),
    "katana": ("quinque_case_katana", "quinque_katana"),
    "mace": ("quinque_case_mace", "quinque_flesh_mace"),
    "launcher": ("quinque_case_launcher", "quinque_crystal_launcher"),
    "dawn": ("quinque_case_dawn", "quinque_dawn"),
}


def model(kind, elements, display):
    return {"credit": "HexKagune: tools/quinque_models.py", "texture_size": [32, 32],
            "textures": {"q": f"hexkagune:item/quinque_{kind}", "particle": f"hexkagune:item/quinque_{kind}"},
            "elements": elements, "display": display, "gui_light": "front"}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.name in {"dawn_half.json", "dawn_nearly.json", "dawn_full.json", "dawn_fire.json"}:
        elements = data.pop("elements")
        payload = json.dumps(data, indent=1)[:-2] + ',\n "elements": [\n'
        payload += ",\n".join("  " + json.dumps(el, separators=(",", ":")) for el in elements)
        path.write_text(payload + "\n ]\n}\n")
    else:
        path.write_text(json.dumps(data, indent=1) + "\n")


def main():
    TEXTURES.mkdir(parents=True, exist_ok=True)
    for kind, colours in KINDS.items():
        texture(kind, colours).save(TEXTURES / f"quinque_{kind}.png")
        parts = WEAPONS[kind]()
        held = weapon_display(kind, span(parts))
        if kind == "dawn":
            def tele(parts, fraction):
                return gun_telescoped(parts, fraction, GUN_GRIP_DAWN)
        else:
            tele = gun_telescoped if kind in GUNS else telescoped
        frames = {
            "case_closed": model(kind, case_elements(False), CASE_DISPLAY),
            "case_open": model(kind, case_elements(True), CASE_DISPLAY),
            "case_carried": model(kind, case_elements(False), carried(False)),
            "case_carried_slim": model(kind, case_elements(False), carried(True)),
            "half": model(kind, tele(parts, 0.33), held),
            "nearly": model(kind, tele(parts, 0.66), held),
            "full": model(kind, tele(parts, 1.0), held),
        }
        if kind == "dawn":
            # 2.0.30: its point parted about a light as a lightning ball leaves it (chosen as a shot is fired; see the item's overrides).
            frames["fire"] = model(kind, tele(dawn_parts(open_tip=True), 1.0), held)
        for name, data in frames.items():
            for el in data["elements"]:
                for v in el["from"] + el["to"]:
                    assert -16 <= v <= 32, f"{kind} {name}: {v} is out of a model's bounds"
            write(MODELS / "quinque" / f"{kind}_{name}.json", data)
        overrides = [{"predicate": {"hexkagune:deploy": v}, "model": f"hexkagune:item/quinque/{kind}_{name}"} for name, v in FRAMES]
        # 1.3.28: the case alone has an investigator's two carried frames, ahead of the opening ones (the last that matches is drawn).
        carrying = [{"predicate": {"hexkagune:deploy": v}, "model": f"hexkagune:item/quinque/{kind}_{name}"}
                    for name, v in (("case_carried", CARRIED_WIDE), ("case_carried_slim", CARRIED_SLIM))]
        case_item, weapon_item = ITEMS[kind]
        write(MODELS / f"{case_item}.json", {"parent": f"hexkagune:item/quinque/{kind}_case_closed", "overrides": carrying + overrides})
        write(MODELS / f"{weapon_item}.json", {"parent": f"hexkagune:item/quinque/{kind}_case_closed", "overrides": overrides})
        print(f"{kind}: {len(parts)} parts, span {span(parts)}")
    write_holds()
    for slim in (False, True):
        print(f"carried ({'slim' if slim else 'wide'}): rotation {carried_display(slim)[0]}, translation {carried_display(slim)[1]}")


if __name__ == "__main__":
    main()
