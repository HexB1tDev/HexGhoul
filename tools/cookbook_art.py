#!/usr/bin/env python3
"""Home Living (1.3.41): the ghoul cookbook's art (python3 tools/cookbook_art.py [out-dir]).

The book Touka trades for two cocoa beans, drawn as an open book in the spirit of Alex's Caves' own guide: two parchment pages on a
dark red leather cover, stained and worn at the edges, a shadow down the gutter; arrows in the bottom corners to turn the pages; a
silk ribbon per chapter down the right edge; a frame for each picture. And a picture for the cover and each chapter (48x48, drawn
twice the size on the page): a covered dish under a cloche, the butcher's board, a bread basket, a steaming pot, the grill, a cake on
its stand, a cup of the café's coffee.

Writes src/main/resources/assets/hexkagune/textures/gui/cookbook.png (the book and its sprites, 512x256) and
textures/gui/cookbook/<chapter>.png (the pictures), or into out-dir with a preview of the spread.
"""
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import kitchen_art as art  # noqa: E402
from kitchen_art import Icon  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
GUI = ROOT / "src" / "main" / "resources" / "assets" / "hexkagune" / "textures" / "gui"

# The spread (its place in the texture is 0,0): the cover all round, two pages inside.
W, H = 300, 196
PAGE_W, PAGE_H = 136, 176
LEFT_X, RIGHT_X, PAGE_Y = 12, 152, 10
# The sprites, to the right of the spread: (x, y, w, h).
SPRITES = {
    "next": (304, 0, 18, 12), "next_hover": (304, 12, 18, 12),
    "prev": (324, 0, 18, 12), "prev_hover": (324, 12, 18, 12),
    "frame": (304, 28, 104, 104),
    "ribbon": (412, 0, 12, 30),
    "rule": (304, 136, 110, 5),
    "slot": (304, 144, 18, 18),
    "home": (344, 0, 14, 12), "home_hover": (344, 12, 14, 12),
}
RIBBONS = [(196, 40, 52), (66, 120, 70), (200, 140, 50), (120, 60, 150), (220, 110, 150), (90, 60, 40), (60, 100, 170)]

LEATHER = (92, 20, 26)
LEATHER_HI = (128, 36, 40)
LEATHER_LO = (58, 10, 16)
PAPER = (238, 224, 196)
PAPER_LO = (214, 194, 160)
PAPER_EDGE = (190, 164, 124)
INK = (64, 40, 30)


def parchment(img, x0, y0, w, h, rng, left):
    """A page: parchment, darker toward its edges, a few stains, the gutter's shadow on its inner side."""
    px = img.load()
    for y in range(h):
        for x in range(w):
            edge = min(x, w - 1 - x, y, h - 1 - y)
            shade = max(0.0, 1.0 - edge / 10.0) * 0.22
            gutter = (w - 1 - x) if left else x
            shade += max(0.0, 1.0 - gutter / 14.0) * 0.28
            n = rng.random() * 0.05
            c = tuple(int(PAPER[i] * (1.0 - shade - n) + PAPER_LO[i] * (shade * 0.3)) for i in range(3))
            px[x0 + x, y0 + y] = c + (255,)
    for _ in range(5):
        cx, cy, r = rng.randint(8, w - 8), rng.randint(8, h - 8), rng.randint(3, 9)
        for y in range(-r, r + 1):
            for x in range(-r, r + 1):
                if x * x + y * y <= r * r and rng.random() < 0.55:
                    p = px[x0 + cx + x, y0 + cy + y]
                    px[x0 + cx + x, y0 + cy + y] = (max(0, p[0] - 10), max(0, p[1] - 14), max(0, p[2] - 20), 255)
    # Its outer edge, the pages beneath showing as thin lines.
    for y in range(h):
        for k in range(2):
            x = (x0 + k) if left else (x0 + w - 1 - k)
            px[x, y0 + y] = PAPER_EDGE + (255,)


def spread(img, rng):
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, W - 1, H - 1], radius=8, fill=LEATHER)
    d.rounded_rectangle([2, 2, W - 3, H - 3], radius=7, outline=LEATHER_HI)
    d.rounded_rectangle([0, 0, W - 1, H - 1], radius=8, outline=LEATHER_LO)
    px = img.load()
    for y in range(H):
        for x in range(W):
            p = px[x, y]
            if p[3] and rng.random() < 0.25:
                px[x, y] = (max(0, p[0] - 8), max(0, p[1] - 4), max(0, p[2] - 4), 255)
    # Stitching round the cover.
    for x in range(10, W - 10, 4):
        px[x, 4] = (170, 120, 80, 255)
        px[x, H - 5] = (170, 120, 80, 255)
    parchment(img, LEFT_X, PAGE_Y, PAGE_W, PAGE_H, rng, True)
    parchment(img, RIGHT_X, PAGE_Y, PAGE_W, PAGE_H, rng, False)
    # The spine between the pages.
    d.rectangle([LEFT_X + PAGE_W, PAGE_Y - 2, RIGHT_X - 1, PAGE_Y + PAGE_H + 1], fill=LEATHER_LO)


def arrow(img, x0, y0, right, hover):
    d = ImageDraw.Draw(img)
    body = (150, 40, 44) if hover else (110, 26, 30)
    hi = (220, 120, 110) if hover else (170, 70, 66)
    if right:
        pts = [(x0 + 1, y0 + 3), (x0 + 10, y0 + 3), (x0 + 10, y0), (x0 + 17, y0 + 6), (x0 + 10, y0 + 11), (x0 + 10, y0 + 8), (x0 + 1, y0 + 8)]
    else:
        pts = [(x0 + 16, y0 + 3), (x0 + 7, y0 + 3), (x0 + 7, y0), (x0, y0 + 6), (x0 + 7, y0 + 11), (x0 + 7, y0 + 8), (x0 + 16, y0 + 8)]
    d.polygon(pts, fill=body, outline=(40, 10, 12))
    d.line([(x0 + 2, y0 + 4), (x0 + 14, y0 + 4)] if right else [(x0 + 3, y0 + 4), (x0 + 15, y0 + 4)], fill=hi)


def home(img, x0, y0, hover):
    d = ImageDraw.Draw(img)
    body = (150, 40, 44) if hover else (110, 26, 30)
    d.polygon([(x0, y0 + 6), (x0 + 7, y0), (x0 + 13, y0 + 6)], fill=body, outline=(40, 10, 12))
    d.rectangle([x0 + 2, y0 + 6, x0 + 11, y0 + 11], fill=body, outline=(40, 10, 12))
    d.rectangle([x0 + 6, y0 + 8, x0 + 7, y0 + 11], fill=(40, 10, 12))


def frame(img, x0, y0, w, h):
    """A picture's frame: a thin dark line, a gold one inside it, little corner flourishes."""
    d = ImageDraw.Draw(img)
    d.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], outline=INK)
    d.rectangle([x0 + 2, y0 + 2, x0 + w - 3, y0 + h - 3], outline=(176, 130, 60))
    for (cx, cy) in ((x0, y0), (x0 + w - 5, y0), (x0, y0 + h - 5), (x0 + w - 5, y0 + h - 5)):
        d.rectangle([cx, cy, cx + 4, cy + 4], fill=INK)
        d.point((cx + 2, cy + 2), fill=(220, 170, 80))


def ribbon(img, x0, y0, w, h, colour):
    """A chapter's silk ribbon, lit down one side, a notch cut in its end."""
    d = ImageDraw.Draw(img)
    hi = tuple(min(255, c + 50) for c in colour)
    lo = tuple(max(0, c - 40) for c in colour)
    d.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], fill=colour)
    d.line([(x0 + 1, y0), (x0 + 1, y0 + h - 4)], fill=hi)
    d.line([(x0 + w - 2, y0), (x0 + w - 2, y0 + h - 4)], fill=lo)
    d.polygon([(x0, y0 + h - 1), (x0 + w // 2, y0 + h - 6), (x0 + w - 1, y0 + h - 1)], fill=(0, 0, 0, 0))


def rule(img, x0, y0, w):
    """A divider: a line with a little diamond in its middle."""
    d = ImageDraw.Draw(img)
    d.line([(x0, y0 + 2), (x0 + w - 1, y0 + 2)], fill=(150, 110, 80))
    m = x0 + w // 2
    d.polygon([(m - 3, y0 + 2), (m, y0 - 1 + 0), (m + 3, y0 + 2), (m, y0 + 4)], fill=(130, 30, 34))


def slot(img, x0, y0):
    d = ImageDraw.Draw(img)
    d.rectangle([x0, y0, x0 + 17, y0 + 17], fill=(206, 186, 150), outline=(150, 112, 80))
    d.line([(x0 + 1, y0 + 1), (x0 + 16, y0 + 1)], fill=(176, 150, 112))


def book():
    rng = random.Random(1341)
    img = Image.new("RGBA", (512, 256), (0, 0, 0, 0))
    spread(img, rng)
    arrow(img, *SPRITES["next"][:2], True, False)
    arrow(img, *SPRITES["next_hover"][:2], True, True)
    arrow(img, *SPRITES["prev"][:2], False, False)
    arrow(img, *SPRITES["prev_hover"][:2], False, True)
    home(img, *SPRITES["home"][:2], False)
    home(img, *SPRITES["home_hover"][:2], True)
    frame(img, *SPRITES["frame"])
    for k, colour in enumerate(RIBBONS):
        ribbon(img, SPRITES["ribbon"][0] + k * (SPRITES["ribbon"][2] + 1), SPRITES["ribbon"][1], SPRITES["ribbon"][2], SPRITES["ribbon"][3], colour)
    rule(img, SPRITES["rule"][0], SPRITES["rule"][1], SPRITES["rule"][2])
    slot(img, *SPRITES["slot"][:2])
    return img


# ---------------------------------------------------------------------------------------------------- the pictures (48x48)

def scaled(icon, factor, size=48, at=(0, 0)):
    """A 16x16 icon blown up {factor} times onto a {size} canvas at {at}."""
    out = Icon(size)
    big = icon.img.resize((16 * factor, 16 * factor), Image.NEAREST)
    out.img.alpha_composite(big, at)
    return out


def table(i, top=36):
    """The kitchen behind (tiles on the wall, a little light from the upper left) and the wooden tabletop the picture stands on."""
    for y in range(top):
        for x in range(48):
            tile = (x % 8 == 0) or (y % 8 == 0)
            light = max(0.0, 1.0 - (x + y) / 70.0) * 18
            base = (86, 60, 52) if tile else (120, 86, 72)
            i.px(x, y, tuple(min(255, int(c + light)) for c in base))
    i.rect(0, top, 47, 47, (120, 80, 46))
    i.rect(0, top, 47, top, (160, 116, 70))
    for y in range(top + 3, 48, 4):
        for x in range(0, 48):
            if (x * 7 + y * 3) % 11 == 0:
                i.px(x, y, (100, 64, 36))


def cover():
    """A dish under a silver cloche, a wisp of red steam escaping."""
    i = Icon(48)
    table(i, 38)
    plate = Icon(48)
    plate.ellipse(24, 37, 21, 4, art.PLATE, bottom=art.PLATE_LO)
    plate.outline()
    i.paste(plate)
    cloche = Icon(48)
    cloche.ellipse(24, 30, 17, 13, (196, 198, 210), top=(236, 238, 246))
    cloche.rect(7, 30, 41, 35, (196, 198, 210))
    cloche.shade(lambda x, y: y > 24 and x > 33 and y < 36, (170, 172, 186))
    cloche.ellipse(24, 16.5, 3, 2, (150, 152, 166), top=(220, 222, 232))
    cloche.rect(6, 35, 42, 36, (140, 142, 156))
    for (x, y) in [(12, 26), (13, 22), (14, 19), (13, 16)]:
        cloche.px(x, y, (240, 244, 252))
    cloche.outline()
    i.paste(cloche)
    for (x, y) in [(38, 30), (39, 27), (38, 24), (39, 21), (40, 18), (39, 15)]:
        i.px(x + 3, y - 4, art.GLOW, 220)
        i.px(x + 4, y - 4, art.BLOOD_HI, 140)
    return i


def basics():
    """The butcher's board: a hunk of meat, a cleaver bitten into the wood, a bottle of blood."""
    i = Icon(48)
    table(i)
    board = Icon(48)
    board.rect(4, 30, 43, 37, (186, 140, 90))
    board.rect(4, 30, 43, 30, (210, 168, 116))
    board.rect(4, 37, 43, 37, (140, 100, 60))
    board.outline()
    i.paste(board)
    i.paste(scaled(art.human_meat(), 2, at=(6, 6)))
    cleaver = Icon(48)
    cleaver.rect(32, 14, 41, 22, (200, 204, 214))
    cleaver.rect(32, 14, 41, 14, (240, 242, 248))
    cleaver.rect(32, 22, 41, 22, (150, 154, 166))
    cleaver.rect(38, 23, 39, 30, art.STICK)
    cleaver.px(39, 16, (90, 90, 100))
    cleaver.outline()
    i.paste(cleaver)
    i.paste(scaled(art.blood_bottle(), 1, at=(27, 18)))
    return i


def breads():
    """A wicker basket of bread, a sandwich leaning on it."""
    i = Icon(48)
    table(i)
    basket = Icon(48)
    basket.polygon([(5, 22), (43, 22), (39, 40), (9, 40)], (180, 130, 70))
    for y in range(23, 40, 3):
        for x in range(6, 43):
            if basket.has(x, y):
                basket.px(x, y, (140, 96, 50))
    for x in range(8, 42, 4):
        for y in range(23, 40):
            if basket.has(x, y) and (y // 3) % 2 == 0:
                basket.px(x, y, (206, 160, 96))
    basket.outline()
    loaves = Icon(48)
    for (cx, cy, rx) in ((15, 20, 9), (30, 18, 10), (24, 15, 7)):
        loaves.ellipse(cx, cy, rx, 5, art.BREAD, top=art.BREAD_HI)
        for k in range(-rx + 3, rx - 2, 4):
            loaves.px(cx + k, cy - 2, art.BREAD_LO)
            loaves.px(cx + k + 1, cy - 3, art.BREAD_LO)
    loaves.outline()
    i.paste(loaves)
    i.paste(basket)
    i.paste(scaled(art.seared_sandwich(), 1, at=(31, 27)))
    return i


def bowls():
    """A pot on the stove, steam curling up, a ladle in it."""
    i = Icon(48)
    table(i, 40)
    pot = Icon(48)
    pot.ellipse(24, 22, 17, 4, (70, 70, 80))
    pot.rect(7, 22, 41, 38, (90, 90, 104))
    pot.ellipse(24, 38, 17, 3, (70, 70, 80))
    pot.shade(lambda x, y: x > 32 and y > 22, (66, 66, 78))
    pot.rect(9, 24, 11, 34, (120, 120, 134))
    pot.ellipse(24, 22, 15, 3, art.STEW, top=art.STEW_HI)
    pot.rect(3, 25, 6, 27, (70, 70, 80))
    pot.rect(42, 25, 45, 27, (70, 70, 80))
    pot.outline()
    i.paste(pot)
    ladle = Icon(48)
    ladle.line([(30 + k, 20 - k) for k in range(12)], (176, 176, 190))
    ladle.line([(31 + k, 20 - k) for k in range(12)], (210, 210, 222))
    ladle.outline()
    i.paste(ladle)
    for (x, y) in [(18, 15), (17, 12), (18, 9), (19, 6), (24, 14), (25, 11), (24, 8), (23, 5)]:
        i.px(x, y, (240, 240, 248), 170)
    for (x, y) in [(14, 21), (20, 22), (27, 21)]:
        i.px(x, y, art.SEARED_HI)
    return i


def plates():
    """The grill: bars over embers, two skewers and a steak laid across it."""
    i = Icon(48)
    table(i, 40)
    grill = Icon(48)
    grill.rect(4, 28, 43, 38, (50, 46, 50))
    for x in range(6, 42, 3):
        grill.px(x, 35, (230, 90, 30))
        grill.px(x + 1, 36, (250, 160, 40))
    grill.rect(4, 26, 43, 27, (90, 90, 100))
    grill.rect(6, 38, 7, 44, (50, 46, 50))
    grill.rect(40, 38, 41, 44, (50, 46, 50))
    grill.outline()
    i.paste(grill)
    for (x, y) in ((2, 14), (9, 15)):
        i.paste(scaled(art.meat_skewer(), 1, at=(x, y)))
    steak = Icon(48)
    steak.ellipse(32, 24, 9, 4, art.SEARED, top=art.SEARED_HI, bottom=art.SEARED_LO)
    for k in range(4):
        steak.line([(26 + 4 * k, 26), (28 + 4 * k, 22)], art.SEARED_LO)
    steak.outline()
    i.paste(steak)
    for (x, y) in [(30, 15), (31, 12), (30, 9), (36, 14), (37, 11), (12, 12), (13, 9)]:
        i.px(x, y, (220, 220, 228), 150)
    return i


def sweets():
    """A whole cake on its stand, berries on top, a slice already served in front."""
    i = Icon(48)
    table(i, 42)
    stand = Icon(48)
    stand.ellipse(22, 36, 18, 3, art.PLATE, bottom=art.PLATE_LO)
    stand.rect(20, 37, 24, 41, art.PLATE_LO)
    stand.ellipse(22, 42, 8, 2, art.PLATE)
    stand.outline()
    i.paste(stand)
    cake = Icon(48)
    cake.rect(7, 19, 37, 34, art.CAKE)
    cake.rect(7, 26, 37, 28, art.GHOUL)
    cake.rect(7, 34, 37, 34, art.CAKE_LO)
    cake.shade(lambda x, y: x > 30, art.CAKE_LO)
    cake.ellipse(22, 19, 15, 4, art.FROSTING, top=(206, 40, 60))
    for x in range(9, 36, 5):
        cake.rect(x, 19, x + 1, 22 + (x % 3), art.FROSTING)
    cake.outline()
    i.paste(cake)
    for (x, y) in [(13, 17), (22, 16), (31, 17)]:
        i.paste(art.piece(lambda l, x=x, y=y: (l.px(x, y, (200, 20, 40)), l.px(x, y - 1, (90, 140, 60))), 48))
    i.paste(scaled(art.kakuja_cake(), 1, at=(31, 28)))
    return i


def drinks():
    """The café's coffee: a cup on its saucer, the leaf on the cup, steam; a sugar cube beside it."""
    i = Icon(48)
    table(i, 40)
    i.paste(scaled(art.anteiku_blend(), 2, at=(6, 6)))
    i.paste(scaled(art.blood_sugar(), 1, at=(34, 26)))
    return i


def backdrop():
    """The kitchen's wall and table alone: the backdrop a dish is shown on."""
    i = Icon(48)
    table(i, 34)
    return i


def item_icon():
    """The book as an item: dark red leather, a gold fork and knife crossed on its cover, its pages' edge showing."""
    i = Icon(16)
    i.rect(3, 1, 13, 14, LEATHER)
    i.rect(3, 1, 3, 14, LEATHER_LO)
    i.rect(4, 1, 13, 1, LEATHER_HI)
    i.rect(12, 2, 13, 14, PAPER)
    i.rect(13, 2, 13, 14, PAPER_LO)
    gold = (220, 170, 70)
    for k in range(7):
        i.px(5 + k, 4 + k, gold)
        i.px(11 - k, 4 + k, (200, 204, 214) if k < 4 else gold)
    i.px(5, 3, gold)
    i.px(6, 3, gold)
    i.rect(4, 13, 11, 13, (200, 140, 60))
    i.outline()
    return i


PICTURES = {"cover": cover, "basics": basics, "breads": breads, "bowls": bowls, "plates": plates, "sweets": sweets, "drinks": drinks,
            "backdrop": backdrop}


def preview(directory, img, pictures):
    """The spread at 3x with the cover picture and a chapter in place, to look at."""
    view = img.crop((0, 0, W, H)).copy()
    view.alpha_composite(pictures["cover"].img.resize((96, 96), Image.NEAREST), (LEFT_X + 20, PAGE_Y + 30))
    view.alpha_composite(pictures["bowls"].img.resize((96, 96), Image.NEAREST), (RIGHT_X + 20, PAGE_Y + 30))
    big = view.resize((W * 3, H * 3), Image.NEAREST)
    big.save(directory / "spread.png")
    sheet = Image.new("RGBA", (len(pictures) * 52 * 3, 52 * 3), (238, 224, 196, 255))
    for k, (name, picture) in enumerate(pictures.items()):
        sheet.alpha_composite(picture.img.resize((144, 144), Image.NEAREST), (k * 156 + 6, 6))
    sheet.save(directory / "pictures.png")


def main():
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else None
    img = book()
    pictures = {name: draw() for name, draw in PICTURES.items()}
    if out:
        out.mkdir(parents=True, exist_ok=True)
        img.save(out / "cookbook.png")
        item_icon().img.resize((128, 128), Image.NEAREST).save(out / "item.png")
        preview(out, img, pictures)
        print("preview in", out)
        return
    (GUI / "cookbook").mkdir(parents=True, exist_ok=True)
    img.save(GUI / "cookbook.png")
    for name, picture in pictures.items():
        picture.save(GUI / "cookbook" / f"{name}.png")
    item_icon().save(GUI.parent / "item" / "ghoul_cookbook.png")
    print("cookbook art written")


if __name__ == "__main__":
    main()
