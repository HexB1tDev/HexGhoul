#!/usr/bin/env python3
"""4.0.0 (The Great Exploration): the owner's outfits and masks as the game draws them, and the masks a wild ghoul may wear.

    python3 tools/outfit_models.py [--preview <dir>]

Reads the owner's canonical geometry (art/outfits/: formal.model.json and crimson.model.json, the 24 masks' masks/<name>/<name>.model.json,
each with its 512 x 512 texture; Minecraft model units, Java Y-down, absolute positions in the player model's frame, each cube on one bone
with its own pivot and rotation and its box-UV offset), fits them to the game's player (fit_mask, fit_outfit: the masks' eyes on the
skin's, the half masks down over the mouth, the coats drawn in, the sleeves again for slim arms), adds the generic masks (GENERIC, drawn
here: a clown's, a gas mask, a skull, a fox, an oni, a cloth wrap, a porcelain face, a bandage wrap, a kabuki face; no character's), and
writes:

  src/main/resources/assets/hexkagune/models/outfits.bin       every model, baked (OutfitModels reads it)
  src/main/resources/assets/hexkagune/textures/outfit/<id>.png  every model's texture (the owner's as they are; the generic ones painted)
  src/main/resources/assets/hexkagune/textures/item/<item>.png  each item's icon: the model's own cubes seen from the front, 32 x 32
  src/main/resources/assets/hexkagune/models/item/<item>.json   each item's model (item/generated)

Box UV is the game's own (ModelPart.Cube: the faces' nets from the cube's offset, fractional sizes as they are), so a texture lands where
the owner's tools put it. Nothing in the owner's files is changed.
"""
import json
import math
import random
import struct
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "art" / "outfits"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "hexkagune"
BIN = ASSETS / "models" / "outfits.bin"
BONES = ["head", "body", "right_arm", "left_arm", "right_leg", "left_leg", "right_arm_slim", "left_arm_slim", "right_leg_coat", "left_leg_coat"]
SLOTS = ["helmet", "chestplate", "leggings", "boots"]
PIVOTS = {"head": (0, 0, 0), "body": (0, 0, 0), "right_arm": (-5, 2, 0), "left_arm": (5, 2, 0), "right_leg": (-1.9, 12, 0),
          "left_leg": (1.9, 12, 0), "right_arm_slim": (-5, 2, 0), "left_arm_slim": (5, 2, 0), "right_leg_coat": (-1.9, 12, 0),
          "left_leg_coat": (1.9, 12, 0)}

# The owner's masks: (their folder, the item's id, its name in game). Kaneki's is "eyepatch" (the build keeps that name out of the jar).
MASKS = [
    ("kaneki", "mask_eyepatch", "Eyepatch Mask"), ("touka", "mask_touka", "Touka's Mask"), ("ayato", "mask_ayato", "Ayato's Mask"),
    ("noro", "mask_noro", "Noro's Mask"), ("yamori", "mask_yamori", "Hockey Mask"), ("eto", "mask_eto", "Eto's Mask"),
    ("tatara", "mask_tatara", "Red Muzzle Mask"), ("bin_brothers", "mask_bin_brothers", "Laughing Mask"),
    ("roma", "mask_roma", "Jester Mask"), ("shachi", "mask_shachi", "Fanged Mask"), ("rize", "mask_rize", "White Muzzle Mask"),
    ("uta", "mask_uta", "Crescent Mask"), ("itori", "mask_itori", "Red Vine Mask"), ("yomo", "mask_yomo", "Black Respirator"),
    ("yoshimura", "mask_dotted", "Dotted Mask"), ("nishiki", "mask_nishiki", "Black Grin Mask"),
    ("tsukiyama", "mask_tsukiyama", "Gourmet's Half Mask"), ("kanae", "mask_kanae", "Rose Filigree Mask"),
    ("donato", "mask_donato", "Priest's Mask"), ("koori", "mask_koori", "Slit Mask"), ("amon", "mask_amon", "Bolted Mask"),
    ("seidou", "mask_seidou", "Purple Grin Mask"), ("akira", "mask_akira", "Violet Mask"), ("arima", "mask_arima", "White Line Mask"),
]
# The outfits: (their file, each slot's item id and name).
OUTFITS = [
    ("formal", [("formal_helmet", "Formal Cap"), ("formal_chestplate", "Formal Overcoat"), ("formal_leggings", "Formal Trousers"),
                ("formal_boots", "Formal Shoes")]),
    ("crimson", [("crimson_helmet", "Crimson Hood"), ("crimson_chestplate", "Crimson Coat"), ("crimson_leggings", "Crimson Coat Tails"),
                 ("crimson_boots", "Crimson Boots")]),
]

# ---------------------------------------------------------------- the generic masks (a wild ghoul's), drawn here

# 4.0.0 (the owner, on the first ones: "very basic and lack details, not only that but the eye position of the masks are very off"):
# each is drawn on a grid of quarter pixels in front of the face, cell by cell (a material, how far forward it stands), its edges shaded,
# its middle standing further forward than its sides (a face's curve), its eye holes on the skin's own eyes (the face's fifth row of
# eight: y -4 to -3, the eyes at x 1 to 3 either side), so a ghoul's kakugan shows through. Each run of like cells is one cube, from its
# face back to just in front of the skin's hat layer (which stands half a pixel off the head).

COLOURS = {"white": (232, 230, 224), "bone": (221, 212, 190), "bone_dark": (176, 164, 136), "black": (24, 24, 28), "void": (12, 10, 12),
           "red": (178, 28, 36), "dark_red": (110, 18, 24), "blue": (60, 110, 200), "grey": (120, 122, 128), "dark_grey": (58, 60, 66),
           "olive": (92, 100, 58), "olive_dark": (62, 68, 40), "lens": (40, 70, 72), "lens_light": (92, 140, 140), "gold": (212, 168, 62),
           "ivory": (236, 226, 196), "cloth": (46, 48, 60), "cloth_dark": (30, 31, 40), "cloth_light": (66, 68, 84), "strap": (34, 30, 28),
           "pink": (214, 120, 140), "rubber": (40, 42, 46), "steel": (150, 154, 160), "porcelain": (244, 240, 234), "crack": (90, 88, 92),
           "brown": (70, 46, 34), "bandage": (226, 218, 198), "bandage_dark": (178, 168, 146), "stain": (128, 32, 30), "chalk": (246, 244, 240)}
CELL = 0.25
GX0, GY0 = -4.75, -11.0  # the grid's top left corner (x, y): 38 columns to x 4.75, 47 rows to y 0.75
COLS, ROWS = 38, 47
BACK = -4.53  # every front cube's back: just in front of the hat layer (z -4.5)
EYE_Y = -3.5  # the skin's eyes' row (y -4 to -3)


def curve(x):
    """How far forward the shell stands at {@code x} (its middle further than its sides): the face's front, in four bands."""
    a = abs(x)
    return -4.86 if a < 1.25 else -4.79 if a < 2.5 else -4.71 if a < 3.5 else -4.62


class Face:
    """A mask's front, cell by cell: a material each (None: nothing; "hole": open), how far each stands forward of the shell."""

    def __init__(self):
        self.mat = [[None] * COLS for _ in range(ROWS)]
        self.lift = [[0.0] * COLS for _ in range(ROWS)]
        self.extra = []

    @staticmethod
    def centre(r, c):
        return GX0 + (c + 0.5) * CELL, GY0 + (r + 0.5) * CELL

    def paint(self, test, mat, lift=None, only=None):
        """Every cell {@code test(x, y)} holds: {@code mat} (and its lift); {@code only}: just cells already of those materials."""
        for r in range(ROWS):
            for c in range(COLS):
                x, y = self.centre(r, c)
                if test(x, y) and (only is None or self.mat[r][c] in only):
                    self.mat[r][c] = mat
                    if lift is not None:
                        self.lift[r][c] = lift(x, y) if callable(lift) else lift

    def ring(self, inside, mat, width=1, lift=None):
        """The cells just around those {@code inside} holds (within {@code width} cells), as {@code mat}: an outline."""
        hit = [[inside(*self.centre(r, c)) for c in range(COLS)] for r in range(ROWS)]
        for r in range(ROWS):
            for c in range(COLS):
                if hit[r][c] or self.mat[r][c] is None:
                    continue
                if any(hit[r + dr][c + dc] for dr in range(-width, width + 1) for dc in range(-width, width + 1)
                       if 0 <= r + dr < ROWS and 0 <= c + dc < COLS):
                    self.mat[r][c] = mat
                    if lift is not None:
                        self.lift[r][c] = lift

    def cubes(self, outline=True):
        """The front as cubes: like cells in a row one cube, rows alike stacked into one; each cell at the edge of the shell darker."""
        key = [[None] * COLS for _ in range(ROWS)]
        for r in range(ROWS):
            for c in range(COLS):
                m = self.mat[r][c]
                if m is None or m == "hole":
                    continue
                x, _ = self.centre(r, c)
                edge = outline and any(not (0 <= r + dr < ROWS and 0 <= c + dc < COLS) or self.mat[r + dr][c + dc] in (None, "hole")
                                       for dr, dc in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                key[r][c] = (m, round(curve(x) - self.lift[r][c], 3), edge)
        runs = []
        for r in range(ROWS):
            c = 0
            while c < COLS:
                k = key[r][c]
                if k is None:
                    c += 1
                    continue
                c1 = c
                while c1 + 1 < COLS and key[r][c1 + 1] == k:
                    c1 += 1
                runs.append([r, r, c, c1, k])
                c = c1 + 1
        # Stack each run on the one just above it, alike and as wide.
        merged = []
        open_runs = {}
        for run in runs:
            r, _, c0, c1, k = run
            above = open_runs.get((r - 1, c0, c1, k))
            if above is not None:
                above[1] = r
                del open_runs[(r - 1, c0, c1, k)]
                open_runs[(r, c0, c1, k)] = above
            else:
                merged.append(run)
                open_runs[(r, c0, c1, k)] = run
        out = []
        for r0, r1, c0, c1, (m, front, edge) in merged:
            x0, y0 = GX0 + c0 * CELL, GY0 + r0 * CELL
            w, h = (c1 - c0 + 1) * CELL, (r1 - r0 + 1) * CELL
            out.append(((x0, y0, front), (w, h, BACK - front), m, None, "edge" if edge else "front"))
        return out + self.extra


def ellipse(cx, cy, rx, ry):
    return lambda x, y: ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0


def both(test):
    """{@code test} and its mirror across the face's middle."""
    return lambda x, y: test(x, y) or test(-x, y)


def blob(cx, cy, rx, ry, p=2.6):
    """A rounded square (a superellipse): a face's outline."""
    return lambda x, y: abs((x - cx) / rx) ** p + abs((y - cy) / ry) ** p <= 1.0


def rect(x0, y0, x1, y1):
    return lambda x, y: x0 <= x <= x1 and y0 <= y <= y1


def line(x0, y0, x1, y1, width):
    """Within {@code width / 2} of the segment."""
    def test(x, y):
        dx, dy = x1 - x0, y1 - y0
        t = max(0.0, min(1.0, ((x - x0) * dx + (y - y0) * dy) / (dx * dx + dy * dy)))
        return math.hypot(x - (x0 + t * dx), y - (y0 + t * dy)) <= width / 2
    return test


def path(points, width):
    tests = [line(*points[i], *points[i + 1], width) for i in range(len(points) - 1)]
    return lambda x, y: any(t(x, y) for t in tests)


def poly(points):
    """Inside the polygon (even-odd)."""
    def test(x, y):
        inside = False
        n = len(points)
        for i in range(n):
            (xa, ya), (xb, yb) = points[i], points[(i + 1) % n]
            if (ya > y) != (yb > y) and x < (xb - xa) * (y - ya) / (yb - ya) + xa:
                inside = not inside
        return inside
    return test


def any_of(*tests):
    return lambda x, y: any(t(x, y) for t in tests)


def straps(y0=-4.3, h=1.1, mat="strap"):
    """The band round the head: a side each (just off the hat layer), and across the back."""
    return [((-4.9, y0, -4.5), (0.2, h, 9.4), mat, None, "side_strap"), ((4.7, y0, -4.5), (0.2, h, 9.4), mat, None, "side_strap"),
            ((-4.9, y0, 4.7), (9.8, h, 0.2), mat, None, "rear_strap")]


def temples(y0, y1, mat, z0=-4.56, z1=-2.6):
    """The mask's sides turning back along the temples (seen from the side, a shell, not a plate), behind the front's edge, under the strap."""
    return [((-4.68, y0, z0), (0.16, y1 - y0, z1 - z0), mat, None, "temple_return"),
            ((4.52, y0, z0), (0.16, y1 - y0, z1 - z0), mat, None, "temple_return")]


FACE = blob(0.0, -4.05, 4.62, 4.32)  # a full mask's outline: the whole face, rounded, a little past the hat layer's edge
EYES = both(ellipse(2.0, EYE_Y, 0.95, 0.55))  # the skin's eyes


def clown():
    f = Face()
    f.paint(FACE, "white")
    f.paint(both(lambda x, y: abs(x - 2.0) / 1.45 + abs(y - EYE_Y) / 1.35 <= 1.0), "black", 0.04)        # the diamonds round the eyes
    f.paint(both(ellipse(2.0, -1.85, 0.32, 0.42)), "blue", 0.04)                                        # a tear under each
    f.paint(both(path([(0.8, -5.35), (1.6, -6.05), (2.5, -6.2), (3.4, -5.7)], 0.38)), "red", 0.08)      # arched brows
    f.paint(both(ellipse(3.35, -2.45, 0.62, 0.55)), "pink", 0.02)                                      # cheeks
    smile = lambda x: -1.05 - 0.085 * x * x
    f.paint(lambda x, y: abs(x) <= 3.3 and abs(y - smile(x)) <= 0.5, "red", 0.1)                       # the smile
    f.paint(lambda x, y: abs(x) <= 2.9 and abs(y - smile(x)) <= 0.14, "void", -0.12)                    # its slit
    f.paint(ellipse(0.0, -2.35, 0.78, 0.72), "red", 0.55)                                              # the nose, a ball
    f.paint(ellipse(0.0, -2.4, 0.5, 0.48), "red", 0.85)
    f.paint(ellipse(-0.22, -2.62, 0.16, 0.16), "white", 0.87)
    f.paint(both(ellipse(3.65, -7.5, 1.05, 0.95)), "red", lambda x, y: 0.25 + 0.12 * ((int((x + 9) * 2) + int((y + 12) * 2)) % 2))  # hair tufts
    f.paint(EYES, "hole")
    f.extra += straps() + temples(-6.8, -1.2, "white")
    return f.cubes()


def gas():
    f = Face()
    f.paint(blob(0.0, -3.9, 4.6, 4.4), "rubber")
    f.paint(rect(-0.32, -5.2, 0.32, -2.5), "rubber", 0.18)                                              # the bridge between the lenses
    f.paint(both(ellipse(2.05, -3.55, 1.28, 1.2)), "steel", 0.36)                                       # lens rims
    f.paint(both(ellipse(2.05, -3.55, 1.0, 0.93)), "lens", 0.16)                                        # the glass
    f.paint(both(lambda x, y: abs((x - 1.75) + (y + 3.85)) < 0.2 and ellipse(2.05, -3.55, 0.9, 0.85)(x, y)), "lens_light", 0.17)
    f.paint(ellipse(0.0, -1.25, 1.45, 1.25), "dark_grey", 0.3)                                        # the filter's seat
    f.paint(ellipse(0.0, -1.25, 1.1, 0.98), "olive", 0.95)                                            # the filter
    f.paint(ellipse(0.0, -1.25, 0.82, 0.72), "olive_dark", 1.18)
    f.paint(lambda x, y: ellipse(0.0, -1.25, 0.7, 0.6)(x, y) and int((y + 12) * 4) % 2 == 0, "black", 1.2)  # its grille
    f.paint(both(ellipse(3.55, -1.5, 0.62, 0.6)), "olive", 0.5)                                       # cheek valves
    f.paint(both(ellipse(3.55, -1.5, 0.3, 0.28)), "black", 0.52)
    f.paint(both(any_of(ellipse(1.25, -6.7, 0.15, 0.15), ellipse(3.0, -6.25, 0.15, 0.15), ellipse(3.85, -3.55, 0.15, 0.15))), "steel", 0.1)
    f.paint(rect(-3.6, -7.35, 3.6, -7.1), "black", 0.05)                                              # a seam
    f.extra += straps(-4.1, 1.2) + straps(-6.9, 0.8) + temples(-6.4, -0.8, "rubber") + [
        ((-0.6, -8.84, -4.6), (1.2, 0.3, 9.2), "strap", None, "head_strap")]
    return f.cubes()


def skull():
    f = Face()
    f.paint(any_of(blob(0.0, -5.0, 4.62, 3.4), blob(0.0, -1.6, 3.2, 1.9, 3.0), blob(0.0, -2.6, 4.0, 1.4, 2.4)), "bone")
    f.paint(both(ellipse(3.6, -2.6, 0.75, 0.9)), "bone_dark", 0.0)                                    # the cheekbones' shadow
    f.paint(both(ellipse(2.0, -3.55, 1.45, 1.2)), "bone_dark", 0.06)                                   # round the sockets
    f.paint(both(ellipse(2.0, -3.55, 1.12, 0.85)), "hole")                                             # the sockets, on the eyes
    f.paint(any_of(both(ellipse(0.28, -2.3, 0.3, 0.33)), poly([(-0.55, -2.25), (0.55, -2.25), (0.0, -1.35)])), "void", -0.1)  # the nose
    f.paint(rect(-2.5, -1.05, 2.5, 0.05), "void", -0.12)                                              # the mouth
    f.paint(lambda x, y: -2.5 <= x <= 2.5 and -1.0 <= y <= 0.0 and (int((x + 8) * 2) % 2 == 0) and abs(y + 0.5) > 0.12, "bone", 0.04)
    f.paint(path([(1.0, -7.95), (1.4, -7.2), (1.1, -6.6), (1.7, -5.9)], 0.18), "bone_dark", 0.0)       # cracks
    f.paint(path([(-3.2, -7.0), (-2.6, -6.5), (-2.9, -5.9)], 0.18), "bone_dark", 0.0)
    f.paint(both(rect(3.4, -6.6, 4.4, -6.35)), "bone_dark", 0.0)
    f.extra += straps() + temples(-7.0, -2.2, "bone")
    return f.cubes()


def fox():
    f = Face()
    f.paint(blob(0.0, -4.0, 4.6, 4.3), "white")
    ear = poly([(-4.35, -7.2), (-3.35, -10.7), (-1.55, -7.75)])
    inner = poly([(-3.75, -7.75), (-3.3, -9.85), (-2.25, -7.95)])
    f.paint(both(ear), "white", 0.05)
    f.paint(both(inner), "red", 0.08)
    f.paint(path([(-0.95, -2.9), (0.0, -3.2), (0.95, -2.9)], 0.01), "white")
    f.paint(poly([(-1.25, -3.1), (1.25, -3.1), (0.7, -0.35), (-0.7, -0.35)]), "white", lambda x, y: 0.35 + 0.65 * max(0.0, (y + 3.1) / 2.75))
    f.paint(ellipse(0.0, -0.75, 0.42, 0.32), "black", 1.05)                                            # the nose's tip
    f.paint(path([(-1.45, -0.15), (-0.7, -0.05), (0.0, -0.3), (0.7, -0.05), (1.45, -0.15)], 0.16), "black", 0.1)
    eye = poly([(1.0, -3.3), (1.9, -3.85), (3.15, -4.1), (2.65, -3.15), (1.5, -3.0)])
    eyes = both(eye)
    f.ring(eyes, "red", 1, 0.08)                                                                       # red round the eyes
    f.paint(both(path([(2.65, -4.0), (3.5, -4.6), (4.1, -4.8)], 0.3)), "red", 0.08)                   # flicked up at the corners
    f.paint(eyes, "hole")
    f.paint(both(ellipse(1.65, -5.2, 0.5, 0.2)), "red", 0.06)                                          # brows
    f.paint(ellipse(0.0, -6.5, 0.32, 0.85), "red", 0.06)                                               # the flame on the brow
    f.paint(both(ellipse(0.62, -7.15, 0.17, 0.17)), "gold", 0.08)
    for a, b in (((2.6, -2.35), (3.95, -2.6)), ((2.65, -1.8), (4.0, -1.75)), ((2.55, -1.25), (3.85, -0.9))):
        f.paint(both(line(*a, *b, 0.18)), "red", 0.06)                                                # whiskers
    f.extra += straps() + temples(-6.8, -1.4, "white")
    return f.cubes()


def oni():
    f = Face()
    f.paint(blob(0.0, -4.0, 4.66, 4.3, 2.3), "red")
    f.paint(both(poly([(0.45, -4.75), (3.75, -5.75), (3.9, -5.05), (0.65, -4.25)])), "dark_red", 0.38)   # brows, knitted
    f.paint(both(ellipse(2.05, -3.45, 1.05, 0.72)), "gold", 0.16)                                      # the eyes' gold
    f.paint(both(poly([(1.15, -3.55), (2.0, -3.95), (2.95, -3.75), (2.6, -3.1), (1.4, -3.05)])), "hole")
    f.paint(ellipse(0.0, -2.25, 1.05, 0.7), "dark_red", 0.5)                                          # the nose
    f.paint(both(ellipse(0.48, -2.0, 0.2, 0.15)), "void", 0.3)
    f.paint(blob(0.0, -0.7, 2.75, 0.68, 2.2), "void", -0.12)                                           # the open mouth
    f.paint(lambda x, y: abs(x) <= 2.2 and -1.35 <= y <= -1.05 and int((x + 8) * 2) % 2 == 0, "ivory", 0.05)  # teeth
    f.paint(both(poly([(1.55, -0.05), (2.15, -0.05), (1.75, -1.5)])), "ivory", 0.32)                   # tusks
    f.paint(both(path([(2.9, -2.3), (3.6, -1.6), (3.7, -0.8)], 0.2)), "dark_red", 0.02)                 # creases
    f.paint(both(path([(0.6, -6.6), (1.5, -6.9), (2.4, -6.7)], 0.2)), "dark_red", 0.02)
    f.paint(ellipse(0.0, -6.0, 0.35, 0.35), "gold", 0.1)
    # The horns: three blocks each, narrowing and leaning out.
    for side in (-1, 1):
        f.extra += [((side * 2.75 - 0.55, -8.6, -4.7), (1.1, 1.3, 1.1), "ivory", (0, 0, side * 12), "horn"),
                    ((side * 3.0 - 0.42, -9.75, -4.6), (0.84, 1.3, 0.84), "ivory", (0, 0, side * 22), "horn"),
                    ((side * 3.35 - 0.28, -10.75, -4.5), (0.56, 1.1, 0.56), "bone_dark", (0, 0, side * 32), "horn")]
    f.extra += straps() + temples(-6.8, -1.2, "red")
    return f.cubes()


def cloth():
    f = Face()
    top = -2.6
    f.paint(rect(-4.62, top, 4.62, 0.5), "cloth", lambda x, y: 0.08 if int((y - top) / 0.75) % 2 else 0.0)  # folds
    f.paint(lambda x, y: -4.62 <= x <= 4.62 and top <= y <= 0.5 and (y - top) % 0.75 < 0.25, "cloth_dark")
    f.paint(path([(-3.0, -1.2), (-2.5, -0.7), (-2.0, -1.2), (-1.5, -0.7), (-1.0, -1.2), (-0.5, -0.7), (0.0, -1.2), (0.5, -0.7),
                  (1.0, -1.2), (1.5, -0.7), (2.0, -1.2), (2.5, -0.7), (3.0, -1.2)], 0.2), "cloth_light", 0.1)  # a grin, stitched
    f.paint(rect(-0.25, top, 0.25, top + 0.5), "cloth", 0.22)                                           # over the nose
    f.extra += [((-4.8, top, -4.55), (0.28, 0.5 - top, 9.35), "cloth", None, "wrap"), ((4.52, top, -4.55), (0.28, 0.5 - top, 9.35), "cloth", None, "wrap"),
                ((-4.8, top, 4.52), (9.6, 0.5 - top, 0.28), "cloth", None, "wrap"),
                ((-0.7, top + 0.2, 4.8), (1.4, 1.2, 0.6), "cloth_dark", None, "knot"),
                ((-0.65, top + 1.2, 4.95), (0.5, 3.4, 0.2), "cloth", (14, 0, 9), "tail"), ((0.15, top + 1.2, 4.95), (0.5, 3.0, 0.2), "cloth", (14, 0, -12), "tail")]
    return f.cubes()


def porcelain():
    f = Face()
    f.paint(blob(0.0, -4.0, 4.5, 4.3, 2.2), "porcelain")
    f.paint(rect(-0.4, -4.6, 0.4, -2.0), "porcelain", 0.14)                                            # the nose's bridge
    f.paint(rect(-0.62, -2.35, 0.62, -1.95), "porcelain", 0.2)
    eye = ellipse(2.0, EYE_Y, 0.95, 0.5)
    f.ring(both(eye), "black", 1, 0.03)                                                                # liner
    for dx, dy in ((0.0, -0.45), (0.7, -0.4), (1.2, -0.25)):
        f.paint(both(line(1.5 + dx, EYE_Y - 0.5, 1.5 + dx + 0.25, EYE_Y - 0.5 + dy - 0.35, 0.16)), "black", 0.04)  # lashes
    f.paint(both(eye), "hole")
    f.paint(both(path([(0.9, -5.25), (2.0, -5.65), (3.1, -5.4)], 0.17)), "brown", 0.04)                 # brows, painted
    f.paint(both(ellipse(3.0, -2.15, 0.7, 0.45)), "pink", 0.02)                                        # blush
    f.paint(any_of(both(ellipse(0.32, -1.2, 0.38, 0.2)), ellipse(0.0, -0.88, 0.55, 0.22)), "red", 0.08)   # lips
    f.paint(rect(-0.55, -1.05, 0.55, -0.98), "dark_red", 0.09)
    # A crack from the brow down through the eye, and a piece broken off at the top right.
    f.paint(path([(-0.6, -7.9), (-1.0, -6.9), (-0.7, -6.2), (-1.5, -5.3), (-1.35, -4.2)], 0.17), "crack", 0.0)
    f.paint(path([(-1.0, -6.9), (-1.9, -6.6), (-2.6, -6.9)], 0.15), "crack", 0.0)
    f.paint(path([(-1.6, -2.9), (-2.1, -2.0), (-1.8, -1.2)], 0.15), "crack", 0.0)
    chip = poly([(2.4, -8.6), (4.8, -8.6), (4.8, -5.9), (3.9, -6.4), (3.4, -7.1), (2.6, -7.4)])
    f.ring(chip, "crack", 1)
    f.paint(chip, None)
    f.extra += straps() + temples(-5.8, -1.6, "porcelain")
    return f.cubes()


def bandage():
    f = Face()
    wrap = lambda x, y: y + (0.25 if x > 1.5 else 0.0) - (0.25 if x < -1.5 else 0.0)  # each turn a little askew
    f.paint(blob(0.0, -4.0, 4.66, 4.4, 4.0), "bandage", lambda x, y: 0.1 * (int(math.floor(wrap(x, y) / 1.0)) % 2))
    f.paint(lambda x, y: blob(0.0, -4.0, 4.66, 4.4, 4.0)(x, y) and wrap(x, y) % 1.0 < 0.25, "bandage_dark")  # the turns' edges
    f.paint(ellipse(2.0, EYE_Y, 0.95, 0.52), "hole")                                                    # one eye left out
    f.ring(ellipse(2.0, EYE_Y, 0.95, 0.52), "bandage_dark", 1)
    f.paint(any_of(ellipse(-2.0, -3.4, 0.85, 0.6), ellipse(-1.7, -2.7, 0.35, 0.5)), "stain", 0.06)      # seeping through over the other
    f.paint(ellipse(-2.0, -3.4, 0.45, 0.3), "dark_red", 0.07)
    f.paint(any_of(ellipse(1.6, -0.9, 0.42, 0.3), ellipse(1.9, -0.55, 0.2, 0.25)), "stain", 0.06)
    f.paint(rect(-3.9, -6.9, -2.6, -6.65), "steel", 0.16)                                             # a safety pin
    f.paint(ellipse(-2.55, -6.78, 0.22, 0.22), "steel", 0.16)
    f.extra += [((-4.8, -8.4, -4.56), (0.28, 8.8, 9.36), "bandage", None, "wrap"), ((4.52, -8.4, -4.56), (0.28, 8.8, 9.36), "bandage", None, "wrap"),
                ((-4.8, -8.4, 4.52), (9.6, 8.8, 0.28), "bandage", None, "wrap")]
    for i in range(8):
        y = -8.25 + i * 1.0
        f.extra += [((-4.88, y + 0.25, -4.5), (0.08, 0.25, 9.3), "bandage_dark", None, "wrap_edge"), ((4.8, y, -4.5), (0.08, 0.25, 9.3), "bandage_dark", None, "wrap_edge"),
                    ((-4.8, y + 0.12, 4.8), (9.6, 0.25, 0.08), "bandage_dark", None, "wrap_edge")]
    f.extra += [((1.2, -6.0, 4.8), (0.9, 4.6, 0.18), "bandage", (10, 0, -8), "loose_end")]
    return f.cubes()


def kabuki():
    f = Face()
    f.paint(blob(0.0, -4.0, 4.55, 4.3, 2.4), "chalk")
    f.paint(both(path([(1.0, -3.95), (2.1, -4.7), (3.2, -5.9), (4.0, -7.0)], 0.42)), "red", 0.06)      # the kumadori, swept up
    f.paint(both(path([(3.0, -3.3), (3.7, -2.6), (4.1, -1.8)], 0.34)), "red", 0.06)
    f.paint(both(path([(1.1, -1.9), (1.8, -1.2), (1.6, -0.4)], 0.26)), "red", 0.06)
    f.paint(both(poly([(0.7, -5.35), (2.9, -6.6), (3.3, -6.1), (0.9, -4.95)])), "black", 0.12)       # brows, raised in anger
    eye = ellipse(2.0, EYE_Y, 0.95, 0.48)
    f.ring(both(eye), "red", 1, 0.05)
    f.paint(both(eye), "hole")
    f.paint(rect(-0.18, -5.9, 0.18, -2.4), "chalk", 0.12)
    f.paint(ellipse(0.0, -6.7, 0.3, 0.3), "red", 0.08)
    f.paint(any_of(both(ellipse(0.35, -1.25, 0.4, 0.2)), ellipse(0.0, -0.95, 0.55, 0.24)), "red", 0.1)  # lips
    f.paint(rect(-0.62, -1.12, 0.62, -1.05), "black", 0.11)
    f.extra += straps() + temples(-6.5, -1.4, "chalk")
    return f.cubes()


GENERIC_MAKERS = [("mask_clown", "Clown Mask", clown), ("mask_gas", "Gas Mask", gas), ("mask_skull", "Skull Mask", skull),
                  ("mask_fox", "Fox Mask", fox), ("mask_oni", "Oni Mask", oni), ("mask_cloth", "Cloth Mask", cloth),
                  ("mask_porcelain", "Porcelain Mask", porcelain), ("mask_bandage", "Bandage Mask", bandage), ("mask_kabuki", "Kabuki Mask", kabuki)]
GENERIC = {item: (label, make()) for item, label, make in GENERIC_MAKERS}


def generic_model(cubes):
    """Cubes on the head bone, packed into an atlas (each net at its own offset), painted by material."""
    size = 128
    while True:
        out, x, y, row = [], 2, 2, 0
        for c in cubes:
            pos, dims, mat = c[0], c[1], c[2]
            rot = c[3] if len(c) > 3 and c[3] else (0, 0, 0)
            w, h, d = dims
            nw = math.ceil(2 * (w + d)) + 2
            nh = math.ceil(d + h) + 2
            if x + nw > size:
                x, y, row = 2, y + row, 0
            out.append({"name": c[4] if len(c) > 4 else mat, "bone": "head", "slot": "helmet", "pos": list(pos), "size": list(dims), "mat": mat,
                        "uv": [x, y], "rot": list(rot), "pivot": [pos[0] + w / 2, pos[1] + h / 2, pos[2] + d / 2] if any(rot) else list(pos)})
            x += nw
            row = max(row, nh)
        if y + row <= size:
            return {"texture_width": size, "texture_height": size, "pivots": PIVOTS, "cubes": out}
        size *= 2


def paint(model, seed):
    """Each cube's net filled with its material's colour, a little grain; a cube at the shell's edge a shade darker."""
    size = model["texture_width"]
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    rng = random.Random(seed)
    for c in model["cubes"]:
        u, v = c["uv"]
        w, h, d = c["size"]
        base = COLOURS[c["mat"]]
        tone = (0.8 if c["name"] == "edge" else 1.0) * rng.uniform(0.96, 1.03)
        x1, y1 = math.ceil(u + 2 * (w + d)), math.ceil(v + d + h)
        for x in range(int(u), min(size, x1 + 1)):
            for y in range(int(v), min(size, y1 + 1)):
                g = rng.uniform(-5, 5)
                px[x, y] = tuple(max(0, min(255, int(ch * tone + g))) for ch in base) + (255,)
    return img


# ---------------------------------------------------------------- fitting the owner's geometry to the game's player

# The owner (after seeing them worn): "Armor is too bulky, if the player is alex model it should adapt to it as well as the male body's
# muscle shape. Masks are... very very lack luster, half masks are covering most of the face instead of being lower, way lower ... the
# eye position of the masks are very off." Their files stay as they are; what is baked is fitted:
#
#  - a full mask's eyes moved onto the skin's eyes (y -4 to -3): the band its eyes are in kept as tall as it is, the brow above stretched
#    down to meet it, the nose and mouth below drawn up under it; the masks with no eyes (Noro's) and Touka's (already there) as they are;
#  - a half mask (over the mouth) drawn down so it starts below the nose's row (y -2.75), not over the eyes;
#  - every mask a little wider and deeper (5%) so it stands clear of the skin's hat layer, never through it;
#  - the outfits' coats, sleeves and trousers drawn in to the body (4.0.1: whatever stands more than 0.27 off a part, a quarter as far,
#    each layer still over the one under it), the Formal Cap out past the hat layer; 4.0.1: their trims, buttons and the Formal shirt and
#    trousers in colour, and details of their own ({@link #details}: belts, badges, a pocket square, epaulettes, laces, straps);
#  - each sleeve again for slim arms (Alex's three pixels: the outer side in by one), drawn instead on a slim-armed wearer.

HALF_MASKS = {"kaneki", "ayato", "nishiki", "shachi", "tatara", "yomo", "rize", "yoshimura"}
HALF_TOP = -2.75
# Each full mask's eyes, where the owner's model has them (the band from their top to their bottom, y).
MASK_EYES = {"akira": (-4.75, -4.125), "amon": (-4.875, -4.375), "arima": (-4.625, -4.125), "bin_brothers": (-5.5, -4.375),
             "donato": (-5.25, -3.5), "eto": (-4.75, -3.5), "itori": (-4.375, -3.25), "kanae": (-4.75, -4.125), "koori": (-4.5, -4.0),
             "roma": (-5.875, -4.125), "tsukiyama": (-5.25, -4.0), "uta": (-5.5, -2.75), "yamori": (-5.75, -3.875), "seidou": (-4.625, -4.125)}
MASK_GIRTH = 1.05
BASES = {"head": ((-4, -8, -4), (4, 0, 4)), "body": ((-4, 0, -2), (4, 12, 2)), "right_arm": ((-8, 0, -2), (-4, 12, 2)),
         "left_arm": ((4, 0, -2), (8, 12, 2)), "right_leg": ((-3.9, 12, -2), (0.1, 24, 2)), "left_leg": ((-0.1, 12, -2), (3.9, 24, 2))}
# 4.0.1 (the owner: "They seem a little too thick ... not so thick in general"): what stands more than CLEAR off a part (just over the
# skin's outer layer, 0.25 off it) keeps SNUG of its distance, each layer at least LAYER over the one under it (or as far as it was).
CLEAR, SNUG, LAYER = 0.27, 0.24, 0.03
CAP_GIRTH = 1.075


def refit(model, fx=None, fy=None, fz=None):
    """Every cube's corners (and pivot) through the maps given (each monotonic along its axis); its texture's net kept as it was."""
    out = []
    for c in model["cubes"]:
        c = dict(c)
        c.setdefault("net", list(c["size"]))
        pos, size, pivot = list(c["pos"]), list(c["size"]), list(c["pivot"])
        for axis, f in enumerate((fx, fy, fz)):
            if f is None:
                continue
            lo, hi = f(pos[axis], c), f(pos[axis] + size[axis], c)
            pos[axis], size[axis] = lo, hi - lo
            pivot[axis] = f(pivot[axis], c)
        c["pos"], c["size"], c["pivot"] = pos, size, pivot
        out.append(c)
    return dict(model, cubes=out)


def piecewise(knots):
    """The map through ({@code from}, {@code to}) knots (sorted by {@code from}), each end's piece carried on past it."""
    def f(v, _c=None):
        i = 0
        while i < len(knots) - 2 and v > knots[i + 1][0]:
            i += 1
        (a0, b0), (a1, b1) = knots[i], knots[i + 1]
        return b0 + (v - a0) * (b1 - b0) / (a1 - a0)
    return f


def fit_mask(folder, model):
    cubes = [c for c in model["cubes"] if "strap" not in c["name"]]
    top = min(c["pos"][1] for c in cubes)
    bottom = max(c["pos"][1] + c["size"][1] for c in cubes)
    if folder in HALF_MASKS:
        model = refit(model, fy=piecewise([(top, HALF_TOP), (bottom, bottom)]))
    elif folder in MASK_EYES:
        e0, e1 = MASK_EYES[folder]
        half = (e1 - e0) / 2
        model = refit(model, fy=piecewise([(top, top), (e0, EYE_Y - half), (e1, EYE_Y + half), (bottom, bottom)]))
    return refit(model, fx=lambda v, c: v * MASK_GIRTH, fz=lambda v, c: v * MASK_GIRTH)


def layers(offsets):
    """The knots a part's side is drawn in through: each distance off it (past CLEAR) its cubes' faces stand at, and where it goes."""
    knots = [(CLEAR, CLEAR)]
    for o in sorted(set(round(o, 3) for o in offsets if o > CLEAR + 1.0E-6)):
        last_o, last_m = knots[-1]
        knots.append((o, max(CLEAR + (o - CLEAR) * SNUG, last_m + min(LAYER, o - last_o))))
    return knots


def through(knots, o):
    """Distance {@code o} off a part, drawn in through {@code knots} (nothing within CLEAR moves)."""
    if o <= knots[0][0]:
        return o
    for (a0, b0), (a1, b1) in zip(knots, knots[1:]):
        if o <= a1:
            return b0 + (o - a0) * (b1 - b0) / (a1 - a0)
    a, b = knots[-1]
    return b + (o - a) * SNUG


def snugger(model):
    """Each part's sides' knots ({@link #layers}), from where every cube of the outfit stands off it (x and z; the head as it is)."""
    out = {}
    for bone, ((x0, _, z0), (x1, _, z1)) in BASES.items():
        for axis, lo, hi in ((0, x0, x1), (2, z0, z1)):
            ups, downs = [], []
            for c in model["cubes"]:
                if c["bone"] != bone:
                    continue
                for v in (c["pos"][axis], c["pos"][axis] + c["size"][axis]):
                    ups.append(v - hi)
                    downs.append(lo - v)
            out[(bone, axis)] = (lo, hi, layers(ups), layers(downs))
    return out


# ---------------------------------------------------------------- 4.0.1: the outfits finished

# The owner: "Can you add more details to it as well to make it as polished"; "you could add like a color or something to the formal
# outfit. For example, to make it look a bit more complex." Their models and textures stay as they are: what is added is cubes of its own
# (each net painted in a free part of the outfit's texture, below the owner's), and a few of their cubes' nets painted another colour
# (its shading kept: each texel's light against its old colour's).

TRIMS = {"navy": (36, 47, 76), "navy_light": (58, 72, 108), "shirt": (236, 238, 242), "silver": (204, 208, 216), "silver_dark": (140, 146, 160),
         "leather": (40, 30, 27), "leather_light": (78, 58, 48), "square": (176, 24, 34), "lace": (88, 88, 98), "shine": (70, 70, 82),
         "trouser": (40, 45, 64), "seam": (108, 108, 120), "stitch": (64, 8, 20), "piping": (36, 18, 24), "strap": (30, 28, 32)}
DETAIL_V = 72  # the owner's nets end above this row of their 512 x 512 texture


def box(name, bone, slot, pos, size, mat, rot=(0, 0, 0), pivot=None):
    return {"name": "detail_" + name, "bone": bone, "slot": slot, "pos": list(pos), "size": list(size), "mat": mat, "rot": list(rot),
            "pivot": list(pivot) if pivot else list(pos)}


def mirrored(c):
    """A limb's cube on the other side (x mirrored about the body's middle)."""
    out = json.loads(json.dumps(c))
    out["bone"] = c["bone"].replace("right_", "left_")
    out["name"] = c["name"].replace("_right_", "_left_")
    out["pos"][0] = -(c["pos"][0] + c["size"][0])
    out["pivot"][0] = -c["pivot"][0]
    out["rot"] = [c["rot"][0], -c["rot"][1], -c["rot"][2]]
    return out


def details(style):
    """The cubes 4.0.1 adds to an outfit (in the owner's frame, fitted with theirs), each with its own net below the owner's."""
    out = []
    pad = (-4.1, -0.65, 0)  # the shoulder pad's pivot (the owner's): the epaulette turns with it
    if style == "formal":
        out += [
            # A white shirt's collar points beside the knot; a navy-welted breast pocket with a red square; a silver badge.
            box("collar_point_r", "body", "chestplate", (-1.55, -0.05, -3.12), (0.7, 0.8, 0.1), "shirt", (0, 0, 25), (-0.85, -0.05, -3.12)),
            box("collar_point_l", "body", "chestplate", (0.85, -0.05, -3.12), (0.7, 0.8, 0.1), "shirt", (0, 0, -25), (0.85, -0.05, -3.12)),
            box("breast_pocket", "body", "chestplate", (2.05, 4.7, -3.04), (1.55, 0.24, 0.1), "navy"),
            box("pocket_square", "body", "chestplate", (2.3, 4.15, -3.02), (0.65, 0.58, 0.08), "square"),
            box("pocket_square_tip", "body", "chestplate", (2.43, 3.93, -3.02), (0.4, 0.24, 0.08), "square"),
            box("badge", "body", "chestplate", (-3.3, 4.35, -3.06), (0.8, 0.85, 0.12), "silver"),
            box("badge_centre", "body", "chestplate", (-3.08, 4.58, -3.12), (0.36, 0.39, 0.06), "navy"),
            box("back_seam", "body", "chestplate", (-0.06, 0.6, 2.76), (0.12, 10.0, 0.06), "seam"),
            # A leather belt round the coat between the second and third buttons, its buckle silver.
            box("belt_front", "body", "chestplate", (-4.47, 9.3, -3.24), (8.94, 0.62, 0.3), "leather"),
            box("belt_side_r", "body", "chestplate", (-4.62, 9.3, -2.94), (0.12, 0.62, 5.82), "leather"),
            box("belt_side_l", "body", "chestplate", (4.5, 9.3, -2.94), (0.12, 0.62, 5.82), "leather"),
            box("belt_back", "body", "chestplate", (-4.62, 9.3, 2.76), (9.24, 0.62, 0.14), "leather"),
            box("buckle", "body", "chestplate", (-0.62, 9.12, -3.34), (1.24, 0.98, 0.1), "silver"),
            box("buckle_tongue", "body", "chestplate", (-0.36, 9.36, -3.4), (0.72, 0.5, 0.06), "leather_light"),
            # Navy epaulettes on the shoulders, a silver button on each; two silver buttons on each cuff.
            box("right_epaulette", "right_arm", "chestplate", (-8.75, -0.98, -0.55), (3.6, 0.15, 1.1), "navy", (0, 0, -4), pad),
            box("right_epaulette_button", "right_arm", "chestplate", (-5.75, -1.06, -0.2), (0.4, 0.08, 0.4), "silver", (0, 0, -4), pad),
            box("right_cuff_button_0", "right_arm", "chestplate", (-9.06, 10.35, -1.05), (0.09, 0.35, 0.35), "silver"),
            box("right_cuff_button_1", "right_arm", "chestplate", (-9.06, 10.35, -0.35), (0.09, 0.35, 0.35), "silver"),
            # Laced, polished shoes.
            box("right_lace_0", "right_leg", "boots", (-3.1, 21.9, -2.95), (2.1, 0.1, 0.1), "lace"),
            box("right_lace_1", "right_leg", "boots", (-3.1, 21.9, -2.7), (2.1, 0.1, 0.1), "lace"),
            box("right_lace_2", "right_leg", "boots", (-3.1, 21.9, -2.45), (2.1, 0.1, 0.1), "lace"),
            box("right_toe_shine", "right_leg", "boots", (-3.7, 22.25, -3.21), (2.6, 0.5, 0.06), "shine"),
            # The cap: a navy band, a silver badge, a short black visor.
            box("cap_band_front", "head", "helmet", (-4.42, -6.6, -4.42), (8.84, 0.5, 0.12), "navy"),
            box("cap_band_back", "head", "helmet", (-4.42, -6.6, 4.3), (8.84, 0.5, 0.12), "navy"),
            box("cap_band_r", "head", "helmet", (-4.42, -6.6, -4.3), (0.12, 0.5, 8.6), "navy"),
            box("cap_band_l", "head", "helmet", (4.3, -6.6, -4.3), (0.12, 0.5, 8.6), "navy"),
            box("cap_badge", "head", "helmet", (-0.5, -7.75, -4.46), (1.0, 1.0, 0.16), "silver"),
            box("cap_badge_centre", "head", "helmet", (-0.22, -7.47, -4.52), (0.44, 0.44, 0.06), "navy"),
            box("cap_visor", "head", "helmet", (-3.4, -6.15, -5.5), (6.8, 0.18, 1.2), "leather", (12, 0, 0), (0.0, -6.15, -4.3)),
        ]
    else:
        out += [
            # A black belt with a silver buckle between the third and fourth clasps; a stitched seam down the back and the hood.
            box("belt_front", "body", "chestplate", (-4.47, 8.15, -3.1), (8.94, 0.6, 0.16), "leather"),
            box("belt_side_r", "body", "chestplate", (-4.62, 8.15, -2.94), (0.12, 0.6, 5.82), "leather"),
            box("belt_side_l", "body", "chestplate", (4.5, 8.15, -2.94), (0.12, 0.6, 5.82), "leather"),
            box("belt_back", "body", "chestplate", (-4.62, 8.15, 2.76), (9.24, 0.6, 0.14), "leather"),
            box("buckle", "body", "chestplate", (-0.55, 7.99, -3.2), (1.1, 0.92, 0.1), "silver"),
            box("buckle_tongue", "body", "chestplate", (-0.32, 8.22, -3.26), (0.64, 0.46, 0.06), "leather_light"),
            box("back_seam", "body", "chestplate", (-0.06, 0.6, 2.76), (0.12, 10.0, 0.06), "stitch"),
            box("hood_back_seam", "head", "helmet", (-0.06, -9.0, 4.97), (0.12, 9.6, 0.06), "stitch"),
            # A strap with a silver buckle round each cuff; one round each boot below the coat.
            box("right_cuff_strap_out", "right_arm", "chestplate", (-9.07, 10.3, -2.78), (0.1, 0.32, 5.56), "strap"),
            box("right_cuff_strap_front", "right_arm", "chestplate", (-8.97, 10.3, -2.78), (5.04, 0.32, 0.1), "strap"),
            box("right_cuff_strap_back", "right_arm", "chestplate", (-8.97, 10.3, 2.68), (5.04, 0.32, 0.1), "strap"),
            box("right_cuff_strap_in", "right_arm", "chestplate", (-3.93, 10.3, -2.78), (0.1, 0.32, 5.56), "strap"),
            box("right_cuff_buckle", "right_arm", "chestplate", (-9.13, 10.22, -0.3), (0.06, 0.48, 0.6), "silver"),
            box("right_boot_strap_out", "right_leg", "boots", (-4.28, 21.5, -2.35), (0.1, 0.28, 4.7), "strap"),
            box("right_boot_strap_front", "right_leg", "boots", (-4.18, 21.5, -2.35), (4.2, 0.28, 0.1), "strap"),
            box("right_boot_strap_back", "right_leg", "boots", (-4.18, 21.5, 2.25), (4.2, 0.28, 0.1), "strap"),
            box("right_boot_buckle", "right_leg", "boots", (-4.34, 21.42, -0.3), (0.06, 0.44, 0.6), "silver"),
        ]
    out += [mirrored(c) for c in out if c["bone"].startswith("right_")]
    # Each net below the owner's, in rows.
    x, y, row = 2, DETAIL_V, 0
    for c in out:
        w, h, d = c["size"]
        nw = math.ceil(2 * (w + d)) + 2
        nh = math.ceil(d + h) + 2
        if x + nw > 510:
            x, y, row = 2, y + row, 0
        c["uv"] = [x, y]
        x += nw
        row = max(row, nh)
    assert y + row <= 512, "no room for the details' nets"
    return out


def named(*parts, bones=None):
    """A test of a cube: its name holds one of {@code parts} (on one of {@code bones}, if given)."""
    return lambda c: any(part in c["name"] for part in parts) and (bones is None or c["bone"] in bones)


LIMBS_AND_BODY = ("body", "right_arm", "left_arm", "right_leg", "left_leg")
# Which of the owner's cubes are painted another colour: (a test, the colour).
RECOLOUR = {
    "formal": [(named("placket_edge", "outer_edge", "asymmetric_overlap", "_hem", bones=LIMBS_AND_BODY), "navy"),
               (named("_cuff", bones=LIMBS_AND_BODY), "navy"),
               (lambda c: c["name"].startswith("front_button"), "silver"),
               (named("shirt_insert"), "shirt"),
               (named("_trouser"), "trouser")],
    "crimson": [(named("placket_edge", "_hem", bones=LIMBS_AND_BODY), "piping")],
}


def finish(style, model, texture):
    """The outfit's texture as the game draws it: the owner's, the recoloured cubes' nets repainted, the details' nets painted."""
    img = texture.convert("RGBA").copy()
    px = img.load()
    size = img.size[0]
    rng = random.Random(11 if style == "formal" else 12)
    for c in model["cubes"]:
        u, v = c["uv"]
        w, h, d = c.get("net", c["size"])
        x1, y1 = min(size - 1, math.ceil(u + 2 * (w + d))), min(img.size[1] - 1, math.ceil(v + d + h))
        if c["name"].startswith("detail_"):
            base = TRIMS[c["mat"]]
            for x in range(int(u), x1 + 1):
                for y in range(int(v), y1 + 1):
                    g = rng.uniform(-4, 4)
                    edge = x in (int(u), x1) or y in (int(v), y1)
                    px[x, y] = tuple(max(0, min(255, int(ch * (0.86 if edge else 1.0) + g))) for ch in base) + (255,)
        elif "paint" in c:
            target = TRIMS[c["paint"]]
            texels = [(x, y) for x in range(int(u), x1 + 1) for y in range(int(v), y1 + 1) if px[x, y][3] > 0]
            if not texels:
                continue
            mean = sum(sum(px[x, y][:3]) for x, y in texels) / (3.0 * len(texels))
            for x, y in texels:
                r, g, b, a = px[x, y]
                shade = ((r + g + b) / 3.0) / max(1.0, mean)
                px[x, y] = tuple(max(0, min(255, int(ch * shade))) for ch in target) + (a,)
    return img


def fit_outfit(style, model):
    model = dict(model, cubes=model["cubes"] + details(style))
    recolour = RECOLOUR[style]
    for c in model["cubes"]:
        for test, mat in recolour:
            if test(c):
                c["paint"] = mat
                break
    sides = snugger(model)

    def along(axis):
        def f(v, c):
            if c["bone"] == "head":
                return v * CAP_GIRTH if style == "formal" else v
            lo, hi, ups, downs = sides[(c["bone"], axis)]
            if v > hi:
                return hi + through(ups, v - hi)
            if v < lo:
                return lo - through(downs, lo - v)
            return v
        return f
    model = refit(model, fx=along(0), fz=along(2))
    if style == "crimson":
        # The hood's shadow over the face in front of the hat layer, so nothing of the skin's shows through it.
        for c in model["cubes"]:
            if c["name"] == "hood_recessed_shadow_lining":
                c["pos"][2] = -4.6
                c["pivot"][2] = -4.6
    slim = []
    for c in model["cubes"]:
        if c["bone"] not in ("right_arm", "left_arm"):
            continue
        sign = -1 if c["bone"] == "right_arm" else 1
        inner, outer = 4.0 * sign, 8.0 * sign

        def squeeze(v, _c=None, inner=inner, outer=outer, sign=sign):
            # Alex's arm is three wide: its inner side where Steve's is, its outer one pixel in.
            if (v - outer) * sign >= 0:
                return v - sign
            if (v - inner) * sign >= 0:
                return inner + (v - inner) * 0.75
            return v
        one = refit({"cubes": [c]}, fx=squeeze)["cubes"][0]
        one["bone"] = c["bone"] + "_slim"
        slim.append(one)
    model["cubes"] = model["cubes"] + slim
    # 4.0.2 (the owner: "the jacket also breaks when walking"): the coat's tails on the legs are a bone of their own, hung from the hip and
    # swinging less than the leg; the trousers, the shoes and the rest stay on the leg.
    for c in model["cubes"]:
        if c["bone"] in ("right_leg", "left_leg") and any(k in c["name"] for k in TAIL_PARTS):
            c["bone"] = c["bone"] + "_coat"
    # 4.0.2 (the owner: "the armor does not allow for animation, make it compatible with animations"): a cube across a joint bendy-lib
    # bends (an elbow, a knee, the middle of the torso) is cut in two there, so its halves bend as the skin's do.
    model["cubes"] = [half for c in model["cubes"] for half in split(c)]
    return model


# The coat's tail pieces on a leg (the owner's names): front and back panels, their hems, gussets and edges, the pockets on them.
TAIL_PARTS = ("coat_", "_hem", "gusset", "inner_edge", "back_seam", "pocket_")
# Where each bone bends (the model's y): an arm's elbow 4 below its pivot, a leg's knee 6, the torso's middle 6 below the neck.
BENDS = {"right_arm": 6.0, "left_arm": 6.0, "right_arm_slim": 6.0, "left_arm_slim": 6.0, "right_leg": 18.0, "left_leg": 18.0, "body": 6.0}


def split(c):
    """{@code c}, or its two halves if it crosses its bone's bend (unturned cubes; a turned one bends as a whole by its corners)."""
    at = BENDS.get(c["bone"])
    y0, h = c["pos"][1], c["size"][1]
    if at is None or any(c["rot"]) or not (y0 + 0.05 < at < y0 + h - 0.05):
        return [c]
    upper, lower = json.loads(json.dumps(c)), json.loads(json.dumps(c))
    upper.setdefault("net", list(c["size"]))
    lower.setdefault("net", list(c["size"]))
    upper["size"][1] = at - y0
    lower["pos"][1] = at
    lower["size"][1] = y0 + h - at
    lower["pivot"] = list(lower["pos"])
    upper["name"] += "_upper"
    lower["name"] += "_lower"
    return [upper, lower]


# ---------------------------------------------------------------- box UV (the game's own) and the icons

def faces(cube, texw, texh):
    """The cube's six faces as the game makes them: ([4 corners], [4 uvs], outward normal), in the model's frame, rotated about its pivot."""
    x0, y0, z0 = cube["pos"]
    w, h, d = cube["size"]
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    u, v = cube["uv"]
    w, h, d = cube.get("net", cube["size"])  # the texture's net: the cube's size as the owner made it (fitted, its texture stays put)
    f4, f5, f6, f7, f8, f9 = u, u + d, u + d + w, u + d + w + w, u + d + w + d, u + d + w + d + w
    f10, f11, f12 = v, v + d, v + d + h
    v0, v1, v2, v3 = (x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)
    v4, v5, v6, v7 = (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)

    def poly(vs, ua, va, ub, vb, n):
        return vs, [(ub / texw, va / texh), (ua / texw, va / texh), (ua / texw, vb / texh), (ub / texw, vb / texh)], n

    out = [poly([v5, v1, v2, v6], f6, f11, f8, f12, (1, 0, 0)),     # east
           poly([v0, v4, v7, v3], f4, f11, f5, f12, (-1, 0, 0)),    # west
           poly([v5, v4, v0, v1], f5, f10, f6, f11, (0, -1, 0)),    # down (y up in the game's frame; the model's -y)
           poly([v2, v3, v7, v6], f6, f11, f7, f10, (0, 1, 0)),     # up
           poly([v1, v0, v3, v2], f5, f11, f6, f12, (0, 0, -1)),    # north (the front)
           poly([v4, v5, v6, v7], f8, f11, f9, f12, (0, 0, 1))]     # south
    rx, ry, rz = (math.radians(a) for a in cube["rot"])
    if rx or ry or rz:
        p = cube["pivot"]

        def turn(q):
            x, y, z = q[0] - p[0], q[1] - p[1], q[2] - p[2]
            # The game's ModelPart: rotationZYX(z, y, x): x first, then y, then z.
            y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
            x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
            x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
            return (x + p[0], y + p[1], z + p[2])

        out = [([turn(q) for q in vs], uvs, turn((n[0] + p[0], n[1] + p[1], n[2] + p[2]))) for vs, uvs, n in out]
        out = [(vs, uvs, (n[0] - p[0], n[1] - p[1], n[2] - p[2])) for vs, uvs, n in out]
    return out


def icon(model, texture, slot, size=32, yaw=-28.0, pitch=0.0):
    """The cubes of {@code slot} seen from the front (a little turned), on a transparent square: an inventory icon."""
    tex = texture.convert("RGBA")
    tw, th = model["texture_width"], model["texture_height"]
    tpx = tex.load()
    # A mask's straps round the head are left out of its icon (seen from the front they stick out sideways).
    cubes = [c for c in model["cubes"] if c.get("slot", "helmet") == slot and "strap" not in c.get("name", "")
             and not c.get("bone", "head").endswith("_slim")]
    pts = []
    a = math.radians(yaw)
    ca, sa = math.cos(a), math.sin(a)
    b = math.radians(pitch)
    cb, sb = math.cos(b), math.sin(b)

    def view(q):
        x, y, z = q
        x, z = x * ca + z * sa, -x * sa + z * ca
        # Seen from a little above: the model's y is down, so its top tips toward the viewer.
        return (x, y * cb - z * sb, y * sb + z * cb)

    polys = []
    for c in cubes:
        for vs, uvs, n in faces(c, tw, th):
            vv = [view(q) for q in vs]
            nn = view(n)
            if nn[2] >= -1e-4:  # facing away (the viewer looks along +z from z = -inf)
                continue
            polys.append((vv, uvs, nn))
            pts.extend(vv)
    if not pts:
        return Image.new("RGBA", (size, size), (0, 0, 0, 0))
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    span = max(max(xs) - min(xs), max(ys) - min(ys))
    big = size * 4
    scale = (big - 8) / span
    cx, cy = (max(xs) + min(xs)) / 2, (max(ys) + min(ys)) / 2
    canvas = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    cpx = canvas.load()
    depth = [[1e9] * big for _ in range(big)]
    light = (0.35, -0.55, -0.75)
    ln = math.sqrt(sum(c * c for c in light))
    for vv, uvs, nn in polys:
        shade = 0.62 + 0.38 * max(0.0, -(nn[0] * light[0] + nn[1] * light[1] + nn[2] * light[2]) / ln)
        steps_u = max(2, int(math.dist(vv[0], vv[1]) * scale) + 2)
        steps_v = max(2, int(math.dist(vv[1], vv[2]) * scale) + 2)
        for i in range(steps_u + 1):
            s = i / steps_u
            for j in range(steps_v + 1):
                t = j / steps_v
                # Bilinear over the quad (corners 0..3 in order) and its uvs.
                p = [(1 - s) * (1 - t) * vv[0][k] + s * (1 - t) * vv[1][k] + s * t * vv[2][k] + (1 - s) * t * vv[3][k] for k in range(3)]
                uv = [(1 - s) * (1 - t) * uvs[0][k] + s * (1 - t) * uvs[1][k] + s * t * uvs[2][k] + (1 - s) * t * uvs[3][k] for k in range(2)]
                X = int((p[0] - cx) * scale + big / 2)
                Y = int((p[1] - cy) * scale + big / 2)
                if not (0 <= X < big and 0 <= Y < big) or p[2] >= depth[Y][X]:
                    continue
                tx = min(tw - 1, max(0, int(uv[0] * tw)))
                ty = min(th - 1, max(0, int(uv[1] * th)))
                col = tpx[tx, ty]
                if col[3] < 16:
                    continue
                depth[Y][X] = p[2]
                cpx[X, Y] = (int(col[0] * shade), int(col[1] * shade), int(col[2] * shade), 255)
    return canvas.resize((size, size), Image.LANCZOS)


# ---------------------------------------------------------------- writing

def bake(models):
    out = bytearray(b"HXOF")
    out += struct.pack(">HH", 2, len(models))
    for mid, model in models:
        name = mid.encode("utf-8")
        out += struct.pack(">H", len(name)) + name
        out += struct.pack(">HHH", model["texture_width"], model["texture_height"], len(model["cubes"]))
        for c in model["cubes"]:
            out += struct.pack(">BB", BONES.index(c["bone"]), SLOTS.index(c.get("slot", "helmet")))
            out += struct.pack(">17f", *c["pos"], *c["size"], *c["pivot"], *c["rot"], *c["uv"], *c.get("net", c["size"]))
    BIN.parent.mkdir(parents=True, exist_ok=True)
    BIN.write_bytes(bytes(out))


def item_model(item):
    path = ASSETS / "models" / "item" / f"{item}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": f"hexkagune:item/{item}"}}, indent=2) + "\n")


def main():
    models = []
    names = {}
    textures = ASSETS / "textures" / "outfit"
    items = ASSETS / "textures" / "item"
    textures.mkdir(parents=True, exist_ok=True)
    items.mkdir(parents=True, exist_ok=True)
    previews = []
    for style, pieces in OUTFITS:
        model = fit_outfit(style, json.loads((ART / f"{style}.model.json").read_text()))
        tex = finish(style, model, Image.open(ART / f"{style}.png"))
        tex.save(textures / f"{style}.png")
        models.append((style, model))
        for slot, (item, label) in zip(SLOTS, pieces):
            names[item] = label
            img = icon(model, tex, slot, yaw=-30.0, pitch=28.0 if slot in ("helmet", "boots") else 0.0)
            img.save(items / f"{item}.png")
            item_model(item)
            previews.append((item, img))
    for folder, item, label in MASKS:
        model = fit_mask(folder, json.loads((ART / "masks" / folder / f"{folder}.model.json").read_text()))
        tex = Image.open(ART / "masks" / folder / f"{folder}.png")
        tex.save(textures / f"{item}.png")
        models.append((item, model))
        names[item] = label
        img = icon(model, tex, "helmet", yaw=0.0)
        img.save(items / f"{item}.png")
        item_model(item)
        previews.append((item, img))
    for i, (item, (label, cubes)) in enumerate(GENERIC.items()):
        model = generic_model(cubes)
        tex = paint(model, i)
        tex.save(textures / f"{item}.png")
        models.append((item, model))
        names[item] = label
        img = icon(model, tex, "helmet", yaw=0.0)
        img.save(items / f"{item}.png")
        item_model(item)
        previews.append((item, img))
    bake(models)
    print(f"{len(models)} models, {sum(len(m['cubes']) for _, m in models)} cubes, {BIN.stat().st_size} bytes")
    print(json.dumps(names))
    if "--preview" in sys.argv:
        out = Path(sys.argv[sys.argv.index("--preview") + 1])
        out.mkdir(parents=True, exist_ok=True)
        cols = 8
        sheet = Image.new("RGBA", (cols * 136, math.ceil(len(previews) / cols) * 136), (40, 42, 48, 255))
        for k, (item, img) in enumerate(previews):
            big = img.resize((128, 128), Image.NEAREST)
            sheet.paste(big, ((k % cols) * 136 + 4, (k // cols) * 136 + 4), big)
        sheet.save(out / "outfit-icons.png")
        for item, (label, cubes) in GENERIC.items():
            model = generic_model(cubes)
            icon(model, paint(model, 0), "helmet", size=160, yaw=-25.0).save(out / f"{item}-big.png")


if __name__ == "__main__":
    main()
