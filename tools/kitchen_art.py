"""Home Living (1.3.40): the ghoul kitchen's pixel art (imported by tools/kitchen.py, which says what each dish is).

Every icon is 16x16, drawn from a few shapes (meat, bread, a bowl seen a little from above with its rim, a plate, a bottle, a mug, a
pie in its tin, a slice of cake...) in one palette, outlined in a dark rim as vanilla's food is, lit from the upper left. Pieces that
overlap are drawn on their own layer and outlined there ({@code piece}), so each keeps its rim. Raw human meat is a marbled hunk with a
strip of pale skin still on it and a bone end; ghoul meat (its own icon already) is deep crimson with black veins; anything made with
ghoul meat shows those veins and a faint red glow, so a ghoul can tell at a glance. The four kitchen effects get 18x18 icons.
"""
from PIL import Image

# ---------------------------------------------------------------------------------------------------- the palette (r, g, b)
OUT = (40, 22, 20)
BREAD = (196, 140, 72)
BREAD_HI = (226, 178, 104)
BREAD_LO = (150, 98, 46)
CRUMB = (240, 214, 160)
TOAST = (232, 196, 140)
RAW = (204, 74, 82)
RAW_HI = (232, 120, 122)
RAW_LO = (150, 40, 54)
SKIN = (236, 190, 160)
FAT = (242, 220, 204)
SEARED = (150, 72, 44)
SEARED_HI = (190, 104, 62)
SEARED_LO = (100, 44, 30)
GHOUL = (126, 14, 30)
GHOUL_HI = (176, 32, 48)
GHOUL_VEIN = (24, 6, 10)
GLOW = (255, 64, 72)
BLOOD = (150, 10, 24)
BLOOD_HI = (200, 30, 44)
BOWL = (138, 92, 54)
BOWL_HI = (170, 120, 74)
BOWL_LO = (96, 62, 36)
PLATE = (222, 222, 230)
PLATE_LO = (176, 176, 188)
STEW = (128, 40, 30)
STEW_HI = (168, 62, 44)
BROTH = (196, 120, 60)
NOODLE = (240, 214, 140)
EGG_WHITE = (246, 244, 236)
YOLK = (250, 196, 50)
GREEN = (90, 160, 60)
GREEN_LO = (54, 110, 40)
CARROT = (238, 128, 32)
BEET = (150, 26, 52)
MUSH_RED = (200, 40, 40)
MUSH_BROWN = (150, 110, 80)
RICE = (236, 232, 214)
RICE_LO = (196, 190, 168)
INK = (30, 26, 40)
INK_HI = (64, 58, 80)
KELP = (40, 70, 40)
KELP_HI = (64, 100, 54)
STICK = (120, 84, 46)
STICK_HI = (160, 116, 66)
MUG = (236, 230, 220)
MUG_LO = (190, 182, 170)
COFFEE = (74, 42, 24)
COFFEE_HI = (120, 74, 44)
CREAM = (230, 210, 180)
CAKE = (250, 238, 230)
CAKE_LO = (220, 200, 194)
FROSTING = (176, 20, 40)
CRUST = (200, 136, 64)
CRUST_HI = (230, 176, 100)
CRUST_LO = (150, 92, 40)
FILL = (140, 30, 40)
POTATO = (214, 170, 92)
POTATO_LO = (150, 100, 50)
PUMPKIN = (226, 140, 40)
PUMPKIN_HI = (246, 180, 80)
BERRY = (170, 26, 50)
GLOW_BERRY = (250, 196, 70)
HONEY = (240, 166, 36)
HONEY_HI = (255, 210, 100)
BONE = (236, 228, 210)
COCOA = (92, 52, 30)
COCOA_HI = (130, 80, 48)
GLASS = (200, 226, 236)
GLASS_HI = (240, 250, 255)
CORK = (160, 110, 64)
SLIME = (110, 190, 90)
SNOW = (236, 244, 252)
GOLD = (250, 206, 60)
GOLD_LO = (200, 150, 30)
CURRY = (204, 140, 40)
CURRY_HI = (230, 176, 70)
SUGAR = (250, 250, 250)
SUGAR_LO = (214, 214, 224)
SAUSAGE = (90, 24, 26)
SAUSAGE_HI = (130, 44, 40)
FRIED = (214, 150, 60)
FRIED_HI = (240, 190, 100)
FRIED_LO = (160, 100, 40)
MOCHI = (250, 236, 240)
MOCHI_LO = (226, 206, 214)


class Icon:
    def __init__(self, size=16):
        self.size = size
        self.img = Image.new("RGBA", (size, size), (0, 0, 0, 0))

    def px(self, x, y, c, a=255):
        if 0 <= x < self.size and 0 <= y < self.size:
            self.img.putpixel((int(x), int(y)), tuple(c) + (a,))

    def get(self, x, y):
        if 0 <= x < self.size and 0 <= y < self.size:
            return self.img.getpixel((int(x), int(y)))
        return (0, 0, 0, 0)

    def has(self, x, y):
        return self.get(x, y)[3] > 0

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.px(x, y, c)

    def ellipse(self, cx, cy, rx, ry, c, top=None, bottom=None):
        """A filled ellipse; {top}/{bottom} shade its upper/lower part."""
        for y in range(self.size):
            for x in range(self.size):
                dx = (x + 0.5 - cx) / rx
                dy = (y + 0.5 - cy) / ry
                if dx * dx + dy * dy <= 1.0:
                    col = c
                    if top is not None and dy < -0.45:
                        col = top
                    if bottom is not None and dy > 0.5:
                        col = bottom
                    self.px(x, y, col)

    def polygon(self, pts, c):
        """Every pixel whose centre is inside the polygon {pts}."""
        n = len(pts)
        for y in range(self.size):
            for x in range(self.size):
                px, py = x + 0.5, y + 0.5
                inside = False
                for k in range(n):
                    (x0, y0), (x1, y1) = pts[k], pts[(k + 1) % n]
                    if (y0 > py) != (y1 > py) and px < x0 + (py - y0) * (x1 - x0) / (y1 - y0):
                        inside = not inside
                if inside:
                    self.px(x, y, c)

    def shade(self, test, c):
        """Recolour the drawn pixels where test(x, y) holds."""
        for y in range(self.size):
            for x in range(self.size):
                if self.has(x, y) and test(x, y):
                    self.px(x, y, c)

    def line(self, pts, c):
        for (x, y) in pts:
            self.px(x, y, c)

    def outline(self, c=OUT):
        """A one-pixel dark rim round everything drawn (outside it)."""
        filled = {(x, y) for y in range(self.size) for x in range(self.size) if self.has(x, y)}
        for (x, y) in list(filled):
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (x + dx, y + dy)
                if n not in filled and 0 <= n[0] < self.size and 0 <= n[1] < self.size:
                    self.px(n[0], n[1], c)

    def paste(self, other):
        self.img.alpha_composite(other.img)

    def save(self, path):
        self.img.save(path)


def piece(draw, size=16):
    """One piece on its own layer, outlined there (so it keeps its rim where it overlaps another)."""
    i = Icon(size)
    draw(i)
    i.outline()
    return i


def layered(*draws):
    """An icon made of pieces, each outlined on its own, the later ones in front."""
    i = Icon()
    for d in draws:
        i.paste(piece(d))
    return i


def glow(i, points):
    for x, y in points:
        i.px(x, y, GLOW)


def veins(i, points):
    for x, y in points:
        i.px(x, y, GHOUL_VEIN)


# ---------------------------------------------------------------------------------------------------- shared shapes

HUNK = [(1.5, 8.5), (3.5, 4.5), (8.0, 3.2), (12.5, 4.2), (14.5, 7.5), (13.0, 11.8), (8.0, 13.0), (3.0, 12.2)]
RIND = [(3, 6), (4, 5), (5, 5), (6, 4), (7, 4), (8, 4), (9, 4), (10, 4), (11, 5), (12, 5), (13, 6), (2, 7)]


def hunk(i, base, lo, rind, marbling, highlights):
    i.polygon(HUNK, base)
    i.shade(lambda x, y: y > 10 or x + y > 22, lo)
    i.line(RIND, rind)
    i.line(marbling, FAT if base == RAW else (206, 150, 110))
    i.line(highlights, RAW_HI if base == RAW else SEARED_HI)


def bone_end(c=FAT, hi=(255, 246, 236)):
    def draw(l):
        l.rect(12, 9, 14, 10, c)
        l.px(15, 8, c)
        l.px(15, 11, c)
        l.px(14, 9, hi)
    return draw


def bowl(i, top=9, inside=None, inside_hi=None):
    """A bowl seen from a little above: its body below row {top}, its rim an oval ring there, {inside} filling the ring."""
    for y in range(i.size):
        for x in range(i.size):
            dx = (x + 0.5 - 8) / 6.6
            dy = (y + 0.5 - top) / 5.2
            if dy > 0 and dx * dx + dy * dy <= 1.0:
                i.px(x, y, BOWL_LO if dy > 0.7 or dx > 0.62 else BOWL)
    i.rect(6, top + 5, 9, top + 5, BOWL_LO)
    i.ellipse(8, top, 6.6, 2.2, BOWL_HI)
    if inside is not None:
        i.ellipse(8, top, 5.3, 1.4, inside, top=inside_hi)


def plate(i, y=12):
    i.rect(1, y, 14, y, PLATE)
    i.rect(2, y + 1, 13, y + 1, PLATE_LO)


def round_plate(i, cy=10.5):
    i.ellipse(8, cy, 7.4, 3.6, PLATE, bottom=PLATE_LO)
    i.ellipse(8, cy - 0.3, 5.6, 2.4, (236, 236, 242))


def stick(i, x0=2, y0=13, n=13):
    for k in range(n):
        i.px(x0 + k, y0 - k, STICK if k % 3 else STICK_HI)


def board(l):
    l.rect(1, 12, 14, 13, STICK_HI)
    l.rect(1, 13, 14, 13, STICK)
    l.rect(2, 14, 3, 14, STICK)
    l.rect(12, 14, 13, 14, STICK)


# ---------------------------------------------------------------------------------------------------- the basics

def human_meat():
    i = Icon()
    hunk(i, RAW, RAW_LO, SKIN, [(4, 7), (5, 7), (6, 6), (9, 6), (10, 6), (5, 10), (6, 10), (7, 9), (11, 9), (12, 8), (9, 11)],
         [(5, 8), (8, 8), (10, 8), (7, 11)])
    i.outline()
    i.paste(piece(bone_end()))
    return i


def cooked_human_meat():
    i = Icon()
    hunk(i, SEARED, SEARED_LO, (206, 140, 84), [], [(6, 6), (9, 6), (12, 8)])
    for k in range(3):
        for t in range(4):
            i.px(4 + 3 * k + t, 10 - t, (70, 30, 20))
    i.outline()
    i.paste(piece(bone_end((226, 206, 180), (246, 232, 214))))
    return i


def cooked_ghoul_meat():
    i = Icon()
    i.polygon(HUNK, GHOUL)
    i.shade(lambda x, y: y > 10 or x + y > 22, (80, 8, 20))
    i.line(RIND, (60, 20, 24))
    for k in range(3):
        for t in range(4):
            i.px(4 + 3 * k + t, 10 - t, GHOUL_VEIN)
    i.line([(6, 6), (9, 5), (12, 7)], GHOUL_HI)
    glow(i, [(3, 8), (11, 9)])
    i.outline()
    return i


def human_mince():
    i = Icon()
    i.ellipse(8, 10, 6.5, 3.6, RAW, top=RAW_HI, bottom=RAW_LO)
    i.ellipse(8, 7.5, 4.0, 2.6, RAW, top=RAW_HI)
    for (x, y) in [(4, 9), (6, 10), (9, 8), (11, 10), (7, 7), (10, 11), (5, 11), (8, 9), (12, 9), (6, 8)]:
        i.px(x, y, RAW_LO if (x + y) % 2 else FAT)
    i.outline()
    return i


def bottle(liquid, hi, label=None, cork=CORK, ghoul=False):
    def glass(l):
        l.ellipse(8, 10.5, 4.6, 4.2, GLASS)
        l.rect(7, 3, 8, 6, GLASS)
        l.ellipse(8, 11.2, 3.8, 3.2, liquid, top=hi)
        l.px(6, 9, GLASS_HI)
        l.px(5, 10, GLASS_HI)
        if label is not None:
            l.rect(6, 11, 10, 12, label)
        if ghoul:
            veins(l, [(9, 12), (10, 10)])
            glow(l, [(7, 13)])
    def stopper(l):
        l.rect(6, 1, 9, 2, cork)
        l.px(6, 1, tuple(min(255, v + 30) for v in cork))
    return layered(glass, stopper)


def blood_bottle():
    return bottle(BLOOD, BLOOD_HI)


def blood_sugar():
    i = Icon()
    for (x, y) in ((2, 8), (8, 8), (5, 3)):
        def cube(l, x=x, y=y):
            l.rect(x, y, x + 5, y + 5, SUGAR)
            l.rect(x, y + 4, x + 5, y + 5, SUGAR_LO)
            l.rect(x + 1, y + 1, x + 2, y + 2, (255, 255, 255))
            l.px(x + 3, y + 3, BLOOD_HI)
            l.px(x + 4, y + 2, (220, 120, 130))
        i.paste(piece(cube))
    return i


def rc_broth():
    i = Icon()
    bowl(i, 9, (120, 16, 30), (170, 40, 52))
    i.line([(5, 9), (9, 8), (11, 9)], GHOUL_VEIN)
    glow(i, [(7, 9), (10, 9)])
    i.outline()
    for (x, y) in [(6, 6), (7, 5), (6, 4), (10, 6), (11, 5), (10, 4)]:
        i.px(x, y, (236, 210, 214), 140)
    return i


# ---------------------------------------------------------------------------------------------------- breads and sandwiches

def sandwich(filling, extra=None, ghoul=False):
    i = Icon()
    i.rect(2, 4, 13, 6, BREAD)
    i.rect(2, 4, 13, 4, BREAD_HI)
    for x in (4, 8, 11):
        i.px(x, 5, CRUMB)
    i.rect(1, 7, 14, 9, filling[0])
    i.rect(1, 7, 14, 7, filling[1])
    i.rect(1, 9, 14, 9, filling[2])
    if extra:
        for (x0, x1, c) in extra:
            i.rect(x0, 7, x1, 7, c)
    if ghoul:
        veins(i, [(3, 8), (7, 8), (11, 8)])
        glow(i, [(5, 8), (13, 8)])
    i.rect(2, 10, 13, 12, BREAD)
    i.rect(2, 12, 13, 12, BREAD_LO)
    i.outline()
    return i


def seared_sandwich():
    return sandwich((SEARED, SEARED_HI, SEARED_LO), [(2, 4, GREEN), (10, 12, GREEN)])


def ghoul_sandwich():
    return sandwich((GHOUL, GHOUL_HI, GHOUL), [(9, 11, MUSH_RED)], ghoul=True)


def club_sandwich():
    """A triangle cut, stacked high, a pick through it."""
    i = Icon()
    i.polygon([(1.5, 14.5), (14.5, 14.5), (14.5, 3.5)], TOAST)
    bands = {7: GREEN, 8: SEARED_HI, 9: SEARED, 11: YOLK, 12: BEET}
    for y, c in bands.items():
        for x in range(16):
            if i.has(x, y) and x < 14:
                i.px(x, y, c)
    i.shade(lambda x, y: y == 14, BREAD_LO)
    i.shade(lambda x, y: x == 14, BREAD)
    i.outline()
    i.paste(piece(lambda l: (l.rect(12, 1, 12, 6, BONE), l.rect(11, 0, 13, 1, (220, 40, 50)))))
    return i


def blood_toast():
    i = Icon()
    i.polygon([(2.5, 5.0), (4.0, 2.5), (12.0, 2.5), (13.5, 5.0), (13.5, 13.5), (2.5, 13.5)], BREAD_LO)
    i.polygon([(3.5, 5.0), (4.6, 3.5), (11.4, 3.5), (12.5, 5.0), (12.5, 12.5), (3.5, 12.5)], TOAST)
    i.polygon([(4.5, 6.0), (5.4, 5.0), (10.6, 5.0), (11.5, 6.0), (11.5, 11.0), (8.0, 11.5), (4.5, 10.5)], BLOOD)
    i.line([(5, 6), (6, 6), (10, 7)], BLOOD_HI)
    i.line([(6, 9), (9, 8)], SUGAR)
    i.px(7, 12, BLOOD)
    i.outline()
    return i


def meat_bun():
    i = Icon()
    plate(i, 13)
    i.ellipse(8, 9.5, 6.4, 4.4, MOCHI, top=(255, 252, 250), bottom=MOCHI_LO)
    for (x, y) in [(6, 6), (7, 5), (8, 5), (9, 5), (10, 6), (8, 7), (7, 6), (9, 6)]:
        i.px(x, y, (214, 200, 196))
    i.px(8, 5, RAW)
    i.outline()
    return i


def burger(patty, patty_hi, patty_lo, ghoul=False, double=False):
    i = Icon()
    i.ellipse(8, 4.5, 6.5, 3.2, BREAD, top=BREAD_HI)
    for x in (5, 8, 11):
        i.px(x, 3, CRUMB)
    i.rect(2, 7, 13, 7, GREEN)
    i.rect(3, 7, 5, 7, BEET)
    rows = [8, 9] if not double else [8, 9, 11, 12]
    for y in rows:
        i.rect(1, y, 14, y, patty)
    i.rect(1, 8, 14, 8, patty_hi)
    if double:
        i.rect(2, 10, 13, 10, YOLK)
        i.rect(1, 12, 14, 12, patty_lo)
        base = 13
    else:
        i.rect(1, 9, 14, 9, patty_lo)
        base = 10
    i.rect(2, base, 13, base + 2, BREAD)
    i.rect(2, base + 2, 13, base + 2, BREAD_LO)
    if ghoul:
        veins(i, [(4, 9), (9, 9), (12, 12)])
        glow(i, [(6, 12), (13, 9)])
    i.outline()
    return i


def anteiku_burger():
    return burger(SEARED, SEARED_HI, SEARED_LO)


def kakuja_burger():
    return burger(GHOUL, GHOUL_HI, (80, 8, 20), ghoul=True, double=True)


def sausage_roll():
    i = Icon()
    i.polygon([(1.5, 9.0), (4.0, 5.0), (13.5, 3.5), (14.5, 7.0), (12.0, 11.5), (2.5, 12.5)], FRIED)
    i.shade(lambda x, y: y >= 10 or x + y > 20, FRIED_LO)
    i.line([(5, 6), (7, 5), (9, 5), (11, 4)], FRIED_HI)
    for x in (5, 8, 11):
        i.line([(x, 7), (x + 1, 8)], FRIED_LO)
    i.outline()
    i.paste(piece(lambda l: (l.ellipse(2.8, 10.3, 2.2, 2.0, SAUSAGE, top=SAUSAGE_HI), l.px(2, 10, (60, 14, 16)))))
    return i


def flatbread_wrap():
    i = Icon()
    i.polygon([(3.0, 14.5), (1.5, 6.5), (6.0, 2.0), (13.5, 3.0), (14.5, 9.0), (8.0, 14.5)], (232, 208, 160))
    i.shade(lambda x, y: x + y > 19, (204, 176, 120))
    i.polygon([(3.5, 6.5), (6.5, 3.5), (12.5, 4.0), (12.0, 7.0), (5.0, 9.0)], SEARED)
    i.line([(5, 5), (8, 4), (11, 5)], GREEN)
    i.line([(6, 6), (10, 6)], BERRY)
    i.line([(4, 7), (9, 7)], SEARED_HI)
    for (x, y) in [(5, 11), (8, 10), (11, 9)]:
        i.px(x, y, (196, 150, 90))
    i.outline()
    return i


# ---------------------------------------------------------------------------------------------------- bowls

def stew(liquid, hi, chunks, top=9, steam=False):
    i = Icon()
    bowl(i, top, liquid, hi)
    for (x, y, c) in chunks:
        i.px(x, y, c)
    i.outline()
    if steam:
        for (x, y) in [(6, 5), (7, 4), (6, 3), (10, 5), (11, 4), (10, 3)]:
            i.px(x, y, (236, 236, 244), 140)
    return i


def mound(rice, rice_lo, bits, inside=None):
    i = Icon()
    bowl(i, 10, inside or rice_lo)
    i.ellipse(8, 9.2, 5.0, 3.0, rice, top=tuple(min(255, c + 30) for c in rice), bottom=rice_lo)
    for (x, y) in [(5, 9), (7, 8), (9, 9), (10, 7), (7, 10)]:
        i.px(x, y, rice_lo)
    for (x, y, c) in bits:
        i.px(x, y, c)
    i.outline()
    return i


def red_stew():
    return stew(STEW, STEW_HI, [(5, 9, SEARED), (6, 9, SEARED_HI), (9, 9, CARROT), (11, 8, MUSH_BROWN), (7, 10, POTATO)], steam=True)


def crimson_risotto():
    return mound((214, 120, 110), (176, 80, 76), [(5, 8, RAW_LO), (10, 9, RAW), (8, 7, MUSH_RED), (11, 8, BEET)])


def black_risotto():
    i = mound(INK_HI, INK, [(5, 8, GHOUL), (9, 8, GHOUL_HI), (11, 9, MUSH_BROWN), (7, 7, (240, 240, 240))])
    glow(i, [(6, 9), (10, 8)])
    return i


def meatballs():
    i = Icon()
    bowl(i, 10, (170, 30, 30), (200, 50, 40))
    i.outline()
    for cx, cy in ((5.5, 9), (10.5, 9), (8, 7.5)):
        i.paste(piece(lambda l, cx=cx, cy=cy: l.ellipse(cx, cy, 2.2, 2.2, SEARED, top=SEARED_HI, bottom=SEARED_LO)))
    i.px(9, 6, GREEN)
    return i


def blood_soup():
    return stew(BEET, (190, 50, 90), [(5, 9, (240, 230, 230)), (6, 9, (240, 230, 230)), (10, 8, BLOOD), (11, 9, GREEN)])


def ramen(broth, broth_hi, meat, ghoul=False, egg=True):
    i = Icon()
    bowl(i, 9, broth, broth_hi)
    for x in range(3, 13):
        if x % 2:
            i.px(x, 9, NOODLE)
    if egg:
        i.ellipse(10.5, 8.3, 1.5, 1.2, EGG_WHITE)
        i.px(10, 8, YOLK)
    i.rect(4, 8, 5, 8, meat)
    i.px(7, 8, KELP)
    if ghoul:
        glow(i, [(4, 9), (12, 9)])
        i.px(5, 8, GHOUL_VEIN)
    i.outline()
    def sticks(l):
        for k in range(6):
            l.px(9 + k // 2, 6 - k, STICK)
            l.px(11 + k // 2, 6 - k, STICK_HI)
    i.paste(piece(sticks))
    return i


def midnight_ramen():
    return ramen(BROTH, (220, 150, 80), SEARED)


def rc_ramen():
    return ramen((130, 20, 30), (176, 44, 56), GHOUL, ghoul=True)


def crimson_curry():
    i = Icon()
    bowl(i, 9, CURRY, CURRY_HI)
    i.ellipse(5.5, 8.6, 2.6, 1.2, RICE)
    for (x, y, c) in [(8, 9, SEARED), (9, 8, SEARED_HI), (11, 9, CARROT), (10, 9, POTATO), (12, 8, BLOOD_HI)]:
        i.px(x, y, c)
    i.outline()
    return i


def centipede_soba():
    i = Icon()
    bowl(i, 9, (90, 60, 40), (120, 84, 56))
    for x in range(3, 13):
        i.px(x, 8 + (x % 2), (150, 120, 90))
    i.outline()
    def centipede(l):
        for k, x in enumerate(range(5, 12)):
            l.px(x, 6 + (k % 2), GHOUL)
        l.px(12, 6, GHOUL_HI)
        for x in range(5, 12, 2):
            l.px(x, 5 + ((x // 2) % 2) * 3, GHOUL_VEIN)
    i.paste(piece(centipede))
    glow(i, [(12, 6)])
    return i


def red_hunger_stew():
    i = stew((110, 14, 24), (150, 30, 40), [(5, 9, GHOUL), (6, 9, GHOUL_VEIN), (9, 9, SEARED), (11, 8, BEET), (8, 10, POTATO)])
    glow(i, [(4, 8), (12, 9)])
    return i


def tide_broth():
    i = stew((70, 120, 110), (110, 160, 150), [(5, 9, KELP_HI), (6, 8, KELP), (9, 9, (220, 180, 160)), (10, 9, (220, 180, 160)), (11, 8, RAW)])
    return i


def pumpkin_soup():
    return stew(PUMPKIN, PUMPKIN_HI, [(5, 9, RAW_LO), (6, 9, RAW), (9, 8, CARROT), (10, 9, (240, 240, 230)), (11, 9, (240, 240, 230))], steam=True)


def forest_stew():
    return stew((110, 50, 40), (150, 74, 56), [(5, 9, MUSH_BROWN), (6, 8, MUSH_BROWN), (9, 9, MUSH_RED), (10, 8, (240, 240, 240)), (11, 9, BLOOD_HI)])


def crimson_pudding():
    i = Icon()
    bowl(i, 11, (240, 230, 220))
    i.outline()
    def flan(l):
        l.polygon([(4.5, 11.0), (5.5, 5.0), (10.5, 5.0), (11.5, 11.0)], (250, 222, 160))
        l.rect(5, 5, 10, 6, BLOOD)
        l.px(5, 7, BLOOD)
        l.px(9, 7, BLOOD)
        l.px(10, 8, BLOOD_HI)
        l.px(6, 8, (255, 240, 200))
    i.paste(piece(flan))
    return i


# ---------------------------------------------------------------------------------------------------- the grill and the plate

def skewer(pieces, ghoul=False):
    i = Icon()
    stick(i)
    i.outline()
    for k, (x, y) in enumerate([(4, 9), (7, 6), (10, 3)]):
        c, hi, lo = pieces[k]
        def chunk(l, x=x, y=y, c=c, hi=hi, lo=lo):
            l.rect(x, y, x + 2, y + 2, c)
            l.px(x, y, hi)
            l.px(x + 2, y + 2, lo)
            if ghoul and c == GHOUL:
                l.px(x + 1, y + 1, GHOUL_VEIN)
        i.paste(piece(chunk))
    if ghoul:
        glow(i, [(12, 3), (5, 10)])
    return i


def meat_skewer():
    m = (SEARED, SEARED_HI, SEARED_LO)
    return skewer([m, (BEET, (190, 50, 80), BEET), m])


def kagune_kebab():
    g = (GHOUL, GHOUL_HI, GHOUL_VEIN)
    return skewer([g, (MUSH_RED, (230, 90, 90), (150, 20, 20)), g], ghoul=True)


def steak_dinner():
    i = Icon()
    round_plate(i)
    i.outline()
    i.paste(piece(lambda l: (l.ellipse(6.5, 9.0, 4.4, 2.8, SEARED, top=SEARED_HI, bottom=SEARED_LO),
                             l.line([(4, 9), (5, 8), (7, 9), (8, 8)], SEARED_LO))))
    i.paste(piece(lambda l: (l.ellipse(11.5, 9.5, 2.2, 1.8, POTATO, top=(236, 200, 120)), l.px(11, 9, (250, 236, 180)))))
    i.paste(piece(lambda l: (l.line([(10, 12), (11, 12), (12, 12), (13, 11)], CARROT), l.px(14, 11, GREEN))))
    return i


def glazed_ribs():
    """A rack of ribs on a plate, the glaze shining, the bones' ends out at the bottom."""
    i = Icon()
    round_plate(i, 11.0)
    i.outline()
    def rack(l):
        l.polygon([(2.5, 8.5), (3.5, 4.5), (13.5, 4.5), (13.5, 9.5), (2.5, 10.5)], SEARED)
        l.shade(lambda x, y: y >= 9, SEARED_LO)
        for x in (4, 7, 10):
            l.line([(x, 5), (x + 1, 6), (x + 1, 8)], (120, 50, 30))
        l.line([(4, 5), (6, 5), (9, 5), (12, 5)], HONEY_HI)
        l.line([(5, 6), (8, 6), (11, 7)], HONEY)
    i.paste(piece(rack))
    def bones(l):
        for x in (4, 7, 10, 13):
            l.rect(x, 10, x, 12, BONE)
    i.paste(piece(bones))
    return i


def blood_sausage():
    i = Icon()
    for (cx, cy) in ((5.0, 6.0), (11.0, 10.5)):
        def link(l, cx=cx, cy=cy):
            l.ellipse(cx, cy, 4.2, 2.4, SAUSAGE, top=SAUSAGE_HI)
            l.px(int(cx) - 1, int(cy) - 1, (170, 70, 70))
            l.px(int(cx) + 2, int(cy), (60, 14, 16))
        i.paste(piece(link))
    i.paste(piece(lambda l: l.line([(8, 8), (9, 8), (8, 9)], (200, 180, 150))))
    return i


def human_jerky():
    i = Icon()
    for k, (x0, y0) in enumerate(((2, 3), (6, 5), (10, 4))):
        def strip(l, x0=x0, y0=y0, k=k):
            l.polygon([(x0 + 0.5, y0 + 0.5), (x0 + 3.5, y0 + 0.0), (x0 + 4.0, y0 + 9.0), (x0 + 1.0, y0 + 9.5)], (110, 40, 34))
            l.line([(x0 + 1, y0 + 2), (x0 + 2, y0 + 4), (x0 + 1, y0 + 7)], (150, 70, 54))
            l.px(x0 + 2, y0 + 1, SUGAR)
        i.paste(piece(strip))
    return i


def tartare(base, hi, vein, glow_points=None, bits=()):
    i = Icon()
    round_plate(i)
    i.outline()
    def heap(l):
        l.ellipse(8, 8.5, 4.6, 3.0, base, top=hi)
        for (x, y) in [(5, 8), (7, 10), (10, 8), (11, 9), (8, 9)]:
            l.px(x, y, vein)
        for (x, y, c) in bits:
            l.px(x, y, c)
    i.paste(piece(heap))
    i.paste(piece(lambda l: (l.ellipse(8.2, 7.2, 1.5, 1.3, YOLK), l.px(8, 6, (255, 236, 150)))))
    if glow_points:
        glow(i, glow_points)
    return i


def human_tartare():
    return tartare(RAW, RAW_HI, RAW_LO, bits=[(4, 9, BEET), (12, 8, GREEN)])


def kakuhou_tartare():
    return tartare(GHOUL, GHOUL_HI, GHOUL_VEIN, glow_points=[(4, 9), (11, 7)], bits=[(12, 8, MUSH_RED)])


def akai_sushi():
    """Two pieces of a roll, seen from a little above: the kelp round each, the rice, the red middle."""
    i = Icon()
    i.paste(piece(board))
    for (cx, cy) in ((5.2, 6.2), (10.6, 9.0)):
        def roll(l, cx=cx, cy=cy):
            l.rect(int(cx - 3), int(cy), int(cx + 2), int(cy + 2), KELP)
            l.ellipse(cx, cy, 3.3, 2.0, RICE)
            l.ellipse(cx, cy, 1.6, 0.9, RAW)
            l.px(int(cx) - 2, int(cy) + 2, KELP_HI)
        i.paste(piece(roll))
    return i


def kuro_sushi():
    """Two nigiri: rice blocks with ghoul meat draped over, a band of kelp round each."""
    i = Icon()
    i.paste(piece(board))
    for (x0, y0) in ((1, 6), (8, 8)):
        def nigiri(l, x0=x0, y0=y0):
            l.rect(x0, y0 + 2, x0 + 6, y0 + 4, RICE)
            l.rect(x0, y0 + 4, x0 + 6, y0 + 4, RICE_LO)
            l.polygon([(x0 - 0.5, y0 + 2.5), (x0 + 1.0, y0 + 0.5), (x0 + 7.5, y0 + 0.5), (x0 + 7.5, y0 + 2.5)], GHOUL)
            l.line([(x0 + 2, y0 + 1), (x0 + 5, y0 + 1)], GHOUL_HI)
            l.px(x0 + 3, y0 + 2, GHOUL_VEIN)
            l.rect(x0 + 3, y0, x0 + 3, y0 + 4, INK)
        i.paste(piece(nigiri))
    glow(i, [(6, 7), (13, 9)])
    return i


def dumplings():
    i = Icon()
    i.paste(piece(lambda l: plate(l, 13)))
    for (cx, cy) in ((8, 7.0), (4.6, 10.2), (11.4, 10.2)):
        def one(l, cx=cx, cy=cy):
            l.ellipse(cx, cy, 3.3, 2.5, (238, 232, 214), top=(252, 250, 244), bottom=(200, 190, 166))
            for dx in (-1, 0, 1):
                l.px(int(cx) + dx, int(cy) - 2, (214, 206, 184) if dx else (252, 250, 244))
            l.px(int(cx) + 1, int(cy), (226, 216, 190))
        i.paste(piece(one))
    return i


def spring_rolls():
    i = Icon()
    for (x0, y0) in ((2, 4), (4, 9)):
        def roll(l, x0=x0, y0=y0):
            l.rect(x0, y0, x0 + 9, y0 + 2, FRIED)
            l.rect(x0, y0, x0 + 9, y0, FRIED_HI)
            l.rect(x0, y0 + 2, x0 + 9, y0 + 2, FRIED_LO)
            for x in range(x0 + 2, x0 + 9, 3):
                l.px(x, y0 + 1, FRIED_LO)
            l.ellipse(x0 + 10.5, y0 + 1.5, 1.6, 1.6, (240, 214, 150))
            l.px(x0 + 10, y0 + 1, CARROT)
            l.px(x0 + 11, y0 + 2, RAW)
        i.paste(piece(roll))
    return i


def croquettes():
    i = Icon()
    i.paste(piece(lambda l: plate(l, 13)))
    for (cx, cy) in ((5.0, 7.0), (10.8, 7.4), (8.0, 10.6)):
        def one(l, cx=cx, cy=cy):
            l.ellipse(cx, cy, 3.4, 2.2, FRIED, top=FRIED_HI, bottom=FRIED_LO)
            l.px(int(cx) - 1, int(cy) - 1, (250, 214, 140))
            l.px(int(cx) + 1, int(cy), FRIED_LO)
        i.paste(piece(one))
    return i


def stuffed_potato():
    i = Icon()
    i.ellipse(8, 9.5, 6.6, 4.0, POTATO_LO, bottom=(110, 70, 36))
    i.ellipse(8, 8.6, 5.2, 2.6, (250, 236, 180))
    i.ellipse(8, 8.2, 3.6, 1.8, RAW_LO, top=RAW)
    i.line([(6, 8), (9, 7), (10, 8)], MUSH_BROWN)
    i.px(7, 7, GREEN)
    i.outline()
    return i


def omelette(fill, fill2, ghoul=False):
    i = Icon()
    round_plate(i)
    i.outline()
    def egg(l):
        l.polygon([(2.5, 10.0), (4.0, 7.0), (8.0, 5.8), (12.0, 7.0), (13.5, 10.0)], YOLK)
        l.line([(4, 7), (5, 7), (8, 6), (11, 7)], (255, 222, 110))
        l.line([(3, 9), (13, 9)], (226, 160, 40))
        l.px(5, 8, fill)
        l.px(9, 8, fill2)
        l.px(11, 8, fill)
        if ghoul:
            l.px(7, 8, GHOUL_VEIN)
    i.paste(piece(egg))
    if ghoul:
        glow(i, [(6, 8)])
    return i


def sunday_omelette():
    return omelette(RAW_LO, CARROT)


def owls_omelette():
    return omelette(GHOUL, MUSH_BROWN, ghoul=True)


def morning_plate():
    i = Icon()
    round_plate(i)
    i.outline()
    i.paste(piece(lambda l: (l.ellipse(5.0, 9.0, 3.0, 2.2, EGG_WHITE), l.ellipse(5.0, 8.8, 1.2, 1.0, YOLK))))
    i.paste(piece(lambda l: (l.line([(8, 7), (9, 8), (10, 7), (11, 8), (12, 7), (13, 8)], SEARED),
                             l.line([(8, 8), (9, 9), (10, 8), (11, 9), (12, 8), (13, 9)], SEARED_HI))))
    i.paste(piece(lambda l: (l.rect(6, 4, 10, 6, TOAST), l.rect(6, 4, 10, 4, BREAD), l.px(10, 6, BREAD_LO))))
    return i


def meatloaf():
    i = Icon()
    i.paste(piece(lambda l: plate(l, 13)))
    for (x0, y0) in ((2, 5), (7, 6)):
        def slice_(l, x0=x0, y0=y0):
            l.rect(x0, y0, x0 + 6, y0 + 6, SEARED)
            l.rect(x0, y0, x0 + 6, y0 + 1, (170, 30, 30))
            l.rect(x0, y0 + 6, x0 + 6, y0 + 6, SEARED_LO)
            for (x, y) in [(1, 3), (4, 2), (3, 5), (5, 4)]:
                l.px(x0 + x, y0 + y, SEARED_HI)
            l.px(x0 + 2, y0 + 4, (240, 230, 200))
        i.paste(piece(slice_))
    return i


def roast_haunch():
    i = Icon()
    round_plate(i, 11.5)
    i.outline()
    def roast(l):
        l.ellipse(7, 8.0, 5.4, 4.0, SEARED, top=SEARED_HI, bottom=SEARED_LO)
        l.line([(4, 6), (6, 5), (9, 5)], (210, 130, 80))
        l.line([(4, 9), (6, 10), (9, 10)], SEARED_LO)
    i.paste(piece(roast))
    i.paste(piece(lambda l: (l.rect(11, 4, 13, 5, BONE), l.px(14, 3, BONE), l.px(14, 6, BONE), l.px(12, 4, (255, 250, 240)))))
    i.paste(piece(lambda l: (l.ellipse(12.5, 10.5, 1.6, 1.5, (200, 40, 40)), l.px(12, 10, (240, 120, 110)))))
    i.paste(piece(lambda l: (l.px(2, 11, BERRY), l.px(3, 12, BERRY), l.px(1, 12, BERRY))))
    return i


def pie(fill, top=CRUST, top_hi=CRUST_HI, ghoul=False, slits=True):
    i = Icon()
    i.ellipse(8, 10.5, 7.0, 3.6, (150, 150, 160), bottom=(110, 110, 124))
    i.ellipse(8, 8.5, 7.0, 3.6, CRUST_LO)
    i.ellipse(8, 8.3, 5.9, 2.8, top, top=top_hi)
    if slits:
        for (x0, y0) in [(5, 8), (8, 7), (10, 9)]:
            i.px(x0, y0, fill)
            i.px(x0 + 1, y0, fill)
    if ghoul:
        i.px(6, 8, GHOUL_VEIN)
        glow(i, [(9, 7), (11, 9)])
    i.outline()
    return i


def meat_pie():
    return pie(FILL)


def ghouls_pie():
    return pie(GHOUL, ghoul=True)


def blood_pumpkin_pie():
    i = pie(PUMPKIN, top=PUMPKIN, top_hi=PUMPKIN_HI, slits=False)
    i.line([(5, 8), (7, 7), (9, 8), (11, 7)], BLOOD)
    i.px(8, 9, BLOOD_HI)
    return i


def berry_tart():
    i = Icon()
    i.ellipse(8, 10.0, 6.4, 3.4, CRUST_LO)
    i.ellipse(8, 8.8, 6.0, 3.0, CRUST, top=CRUST_HI)
    i.ellipse(8, 8.6, 4.4, 2.0, BLOOD)
    i.outline()
    for (x, y) in ((5, 8), (8, 7), (10, 9), (7, 9), (11, 7)):
        i.paste(piece(lambda l, x=x, y=y: (l.px(x, y, BERRY), l.px(x, y - 1, (220, 80, 100)))))
    return i


def cake_slice(ghoul=True, filling=FILL, frosting=FROSTING, cake=CAKE, cake_lo=CAKE_LO):
    i = Icon()
    front = [(1.5, 8.0), (13.5, 3.5), (13.5, 13.5), (1.5, 13.5)]
    i.polygon(front, cake)
    for y in range(16):
        for x in range(16):
            if i.has(x, y):
                top = 8.0 - (x - 1.5) * 4.5 / 12.0
                if y + 0.5 < top + 1.6:
                    i.px(x, y, frosting)
                elif y in (9, 10):
                    i.px(x, y, GHOUL if ghoul else filling)
                elif y == 13:
                    i.px(x, y, cake_lo)
    i.rect(13, 4, 14, 13, cake_lo)
    i.rect(13, 4, 14, 5, tuple(max(0, v - 46) for v in frosting))
    i.rect(13, 9, 14, 10, GHOUL_VEIN if ghoul else tuple(max(0, v - 30) for v in filling))
    for (x, y) in [(5, 8), (9, 7), (9, 8)]:
        i.px(x, y, frosting)
    if ghoul:
        veins(i, [(4, 9), (10, 10)])
        glow(i, [(7, 10)])
    i.outline()
    i.paste(piece(lambda l: (l.px(11, 2, (200, 20, 40)), l.px(11, 1, (90, 140, 60)))))
    return i


def kakuja_cake():
    return cake_slice(True)


def velvet_cake():
    return cake_slice(False, filling=CAKE, frosting=(250, 246, 240), cake=(176, 30, 44), cake_lo=(130, 16, 30))


def blood_cookies():
    i = Icon()
    for (cx, cy) in ((5.5, 6.0), (10.5, 10.0)):
        def cookie(l, cx=cx, cy=cy):
            l.ellipse(cx, cy, 4.3, 4.0, (196, 130, 70), top=(222, 160, 96), bottom=(150, 92, 46))
            for (dx, dy) in ((-2, -1), (1, -2), (0, 1), (2, 1), (-1, 2)):
                l.px(int(cx) + dx, int(cy) + dy, BLOOD if (dx + dy) % 2 else COCOA)
        i.paste(piece(cookie))
    return i


def blood_jelly():
    i = Icon()
    plate(i, 13)
    i.polygon([(3.5, 12.5), (4.5, 5.5), (11.5, 5.5), (12.5, 12.5)], (190, 20, 40))
    i.polygon([(4.5, 7.0), (5.0, 5.5), (11.0, 5.5), (11.5, 7.0)], (230, 70, 90))
    i.line([(5, 8), (5, 9), (5, 10)], (240, 120, 130))
    i.px(10, 9, (130, 10, 24))
    i.outline()
    return i


def ghoul_mochi():
    i = Icon()
    i.paste(piece(lambda l: plate(l, 13)))
    for (cx, cy) in ((5.0, 9.5), (11.0, 9.5), (8.0, 6.5)):
        def one(l, cx=cx, cy=cy):
            l.ellipse(cx, cy, 3.2, 2.8, MOCHI, top=(255, 250, 252), bottom=MOCHI_LO)
            l.px(int(cx), int(cy) + 1, GHOUL)
            l.px(int(cx) - 1, int(cy) + 1, (200, 120, 130))
        i.paste(piece(one))
    glow(i, [(8, 7)])
    return i


def truffles():
    i = Icon()
    for (cx, cy) in ((4.5, 10.0), (11.5, 10.0), (8.0, 6.0)):
        def one(l, cx=cx, cy=cy):
            l.ellipse(cx, cy, 3.0, 2.8, COCOA, top=COCOA_HI)
            l.px(int(cx) - 1, int(cy) - 1, (170, 120, 90))
            l.px(int(cx), int(cy) - 2, BLOOD_HI)
        i.paste(piece(one))
    return i


def golden_heart():
    i = Icon()
    for (cx, cy) in ((5.5, 6.5), (10.5, 6.5)):
        i.ellipse(cx, cy, 3.4, 3.4, GOLD, top=(255, 240, 150))
    for y in range(7, 15):
        w = 7 - (y - 7)
        i.rect(8 - w, y, 7 + w, y, GOLD)
    i.shade(lambda x, y: y >= 11 or x >= 12, GOLD_LO)
    i.line([(6, 7), (7, 8), (9, 8), (10, 9), (8, 10), (6, 10)], GHOUL)
    veins(i, [(7, 9), (9, 9)])
    i.px(4, 5, (255, 255, 230))
    glow(i, [(8, 9)])
    i.outline()
    return i


def glow_sorbet():
    i = Icon()
    def glass_cup(l):
        l.polygon([(3.5, 8.0), (12.5, 8.0), (10.5, 12.5), (5.5, 12.5)], GLASS)
        l.rect(7, 13, 8, 13, GLASS)
        l.rect(5, 14, 10, 14, GLASS)
        l.px(5, 9, GLASS_HI)
    i.paste(piece(glass_cup))
    def scoops(l):
        l.ellipse(6.0, 6.8, 2.8, 2.4, (250, 220, 120), top=(255, 244, 180))
        l.ellipse(10.0, 6.8, 2.8, 2.4, (240, 90, 100), top=(255, 150, 160))
        l.ellipse(8.0, 4.2, 2.6, 2.2, (250, 220, 120), top=(255, 244, 180))
    i.paste(piece(scoops))
    i.px(8, 2, GLOW_BERRY)
    i.px(10, 6, BLOOD)
    return i


# ---------------------------------------------------------------------------------------------------- drinks

def mug(liquid, hi, foam=None, mark=None, steam=True):
    i = Icon()
    i.rect(3, 5, 11, 13, MUG)
    i.rect(3, 13, 11, 13, MUG_LO)
    i.rect(12, 7, 13, 7, MUG)
    i.rect(13, 8, 13, 10, MUG)
    i.rect(12, 11, 13, 11, MUG)
    i.rect(4, 5, 10, 6, liquid)
    i.rect(5, 5, 7, 5, hi)
    if foam:
        for (x, y) in foam:
            i.px(x, y, CREAM)
    if mark:
        mark(i)
    i.outline()
    if steam:
        for (x, y) in [(5, 3), (6, 2), (5, 1), (8, 2), (9, 1), (8, 0)]:
            i.px(x, y, (236, 236, 244), 150)
    return i


def anteiku_blend():
    def leaf(i):
        i.px(7, 9, (90, 60, 40))
        i.px(8, 10, (90, 60, 40))
        i.px(6, 10, (120, 160, 80))
        i.px(7, 10, (120, 160, 80))
    return mug(COFFEE, COFFEE_HI, foam=[(8, 6)], mark=leaf)


def blood_latte():
    def heart(i):
        i.px(7, 9, BLOOD_HI)
        i.px(9, 9, BLOOD_HI)
        i.rect(7, 10, 9, 10, BLOOD_HI)
        i.px(8, 11, BLOOD_HI)
    return mug((170, 90, 80), (200, 130, 120), foam=[(6, 5), (7, 6), (8, 5), (9, 6)], mark=heart)


def crimson_tea():
    i = Icon()
    def cup(l):
        l.ellipse(8, 11.5, 5.5, 2.2, PLATE, bottom=PLATE_LO)
        l.polygon([(3.5, 6.5), (12.5, 6.5), (11.0, 11.5), (5.0, 11.5)], (250, 250, 252))
        l.ellipse(8, 6.8, 4.4, 1.2, (190, 40, 50))
        l.px(5, 8, (226, 226, 236))
        l.px(9, 9, BERRY)
        l.px(7, 9, BERRY)
    i.paste(piece(cup))
    i.paste(piece(lambda l: (l.rect(13, 7, 13, 9, (250, 250, 252)), l.px(12, 7, (250, 250, 252)), l.px(12, 9, (250, 250, 252)))))
    for (x, y) in [(6, 4), (7, 3), (6, 2), (9, 4), (10, 3)]:
        i.px(x, y, (236, 236, 244), 150)
    return i


def rc_tonic():
    return bottle((200, 30, 60), (250, 90, 110), label=GLOW_BERRY, cork=GHOUL, ghoul=True)


def kakuja_shot():
    i = Icon()
    def glass_(l):
        l.polygon([(4.5, 4.5), (11.5, 4.5), (10.5, 13.5), (5.5, 13.5)], GLASS)
        l.polygon([(5.0, 7.0), (11.0, 7.0), (10.2, 12.5), (5.8, 12.5)], (150, 10, 24))
        l.rect(5, 7, 10, 7, (230, 120, 40))
        l.px(6, 5, GLASS_HI)
        l.px(6, 6, GLASS_HI)
        l.px(8, 10, GHOUL_VEIN)
        glow(l, [(9, 9)])
    i.paste(piece(glass_))
    i.paste(piece(lambda l: (l.px(11, 3, (250, 180, 40)), l.px(12, 2, (250, 120, 20)), l.px(10, 2, (250, 220, 120)))))
    return i


# ---------------------------------------------------------------------------------------------------- the effects' icons (18x18)

def effect_icon(kind):
    i = Icon(18)
    if kind == "refreshed":         # a drop of water, a glint in it
        i.polygon([(9.0, 2.0), (13.2, 9.5), (4.8, 9.5)], (90, 180, 240))
        i.ellipse(9, 11, 4.5, 4.5, (90, 180, 240), top=(170, 220, 255))
        i.px(7, 10, (240, 250, 255))
        i.px(7, 11, (240, 250, 255))
    elif kind == "replenished":     # a heart, full
        for (cx, cy) in ((6.5, 7.5), (11.5, 7.5)):
            i.ellipse(cx, cy, 3.4, 3.4, (230, 60, 80), top=(255, 140, 150))
        for y in range(8, 15):
            w = 7 - (y - 8)
            i.rect(9 - w, y, 8 + w, y, (230, 60, 80))
        i.px(6, 6, (255, 220, 225))
    elif kind == "content":         # a smiling face
        i.ellipse(9, 9, 6.5, 6.5, (250, 210, 80), top=(255, 236, 140), bottom=(220, 170, 50))
        i.px(6, 7, OUT)
        i.px(11, 7, OUT)
        for x in range(6, 12):
            i.px(x, 12 if 7 <= x <= 10 else 11, OUT)
    else:                           # clear sight: an open eye
        i.ellipse(9, 9, 7, 4, (245, 245, 250))
        i.ellipse(9, 9, 3, 3, (60, 150, 220), top=(120, 200, 250))
        i.ellipse(9, 9, 1.3, 1.3, OUT)
        i.px(10, 8, (255, 255, 255))
    i.outline()
    return i
