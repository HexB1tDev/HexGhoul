#!/usr/bin/env python3
"""4.1.50 (the owner: Aogiri's name "dark red with a custom tree emblem", the Clowns' "blue with a custom clown face", "(symbol) (space)
(name)"; 4.1.54, the Gas Masks: "Toxic logo, green name"): the gangs' emblems as 16 x 16 font glyphs (assets/hexkagune/textures/font/), drawn here pixel by pixel, and the font that
maps them (assets/minecraft/font/default.json: U+E000 the tree, U+E001 the clown, U+E002 the toxic trefoil, U+E003 (4.1.70) Anteiku's coffee cup, private characters added to the game's own font: every
resource pack's default.json is read and joined, so a plain name can carry them). The glyphs keep their own colours (the name after them
is coloured, not the emblem: GangPlayer.named puts white before them).

    python3 tools/gang_emblems.py [--preview out.png]
"""
import json
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "hexkagune"

# The tree: a bare, gnarled tree, its crown of forking branches, a thick trunk, roots spreading (o: outline, f: bark, h: light).
TREE = [
    "................",
    "..o..o....o..o..",
    "..of.fo..of.fo..",
    "o..ff.f..f.ff..o",
    "of..fffhhfff..fo",
    ".fff..fhhf..fff.",
    "...fffffhffff...",
    "......fhhf......",
    "......fhhf......",
    "......fhhf......",
    "......fhhf......",
    ".....ffhhff.....",
    "....ff.hh.ff....",
    "..fff..ff..fff..",
    ".oo...o..o...oo.",
    "................",
]
TREE_COLOURS = {"o": (58, 6, 6), "f": (139, 26, 26), "h": (196, 57, 43)}

# The clown: a white face, blue tufts of hair, black diamond eyes with a blue tear, a red nose, a wide red grin.
CLOWN = [
    "................",
    ".bb..........bb.",
    "bBbb.wwwwww.bbBb",
    "bBBbwwwwwwwwbBBb",
    ".bbwwwwwwwwwwbb.",
    "..wwkwwwwwwkwww.",
    "..wkkkwwwwkkkww.",
    "..wwkwwrrwwkwww.",
    "..wwtwrRRrwtwww.",
    "..wwwwwrrwwwwww.",
    "..wrwwwwwwwwrww.",
    "..wwrrwwwwrrwww.",
    "...wwrrrrrrwww..",
    "....wwwwwwwww...",
    "......wwwww.....",
    "................",
]
CLOWN_COLOURS = {"b": (36, 84, 196), "B": (88, 140, 240), "w": (245, 245, 240), "k": (20, 20, 24), "t": (70, 120, 230), "r": (200, 30, 40),
                 "R": (240, 80, 80)}

# 4.1.54 (the owner: the Gas Masks "Toxic logo, green name"): the biohazard trefoil in toxic green: three horns round round holes, each open
# outward, meeting at a small ring in the middle (g: green, d: its darker edge). Drawn from the true symbol's construction shrunk to 16 px,
# its horns two pixels thick (as heavy as the tree and the clown at a small GUI scale).
TOXIC = [
    "....dgg..ggd....",
    "...ggd....dgg...",
    "..ggd......dgg..",
    "..gg........gg..",
    "..gg........gg..",
    "..ggd......dgg..",
    ".dgggg.gg.ggggd.",
    "dggggggggggggggd",
    "ggd..dg..gd..dgg",
    "gg....dggd....gg",
    "gd....gggg....dg",
    "d.....g..g.....d",
    ".....dg..gd.....",
    "....dggddggd....",
    "...dgd....dgd...",
    "..dd........dd..",
]
TOXIC_COLOURS = {"g": (110, 214, 52), "d": (60, 150, 30)}

# 4.1.70 (the owner: Anteiku's "Light brown name tag with a coffee symbol"): a cup of coffee on its saucer, steam rising: a cream cup with a
# dark brown outline, the coffee's crema in its mouth, the handle on the right (its sides two pixels thick), two wisps of steam two pixels
# thick, curling as they rise and thinning at the top (o: outline, w: cup, h: its lit side, c: coffee, C: crema, s: steam).
COFFEE = [
    ".....s.....s....",
    "....ss....ss....",
    "...ss....ss.....",
    "...ss....ss.....",
    "....ss....ss....",
    "ooooooooooooo...",
    "oCcccccccccCo...",
    "ohwwwwwwwwwwoooo",
    "ohwwwwwwwwwwo.oo",
    "ohwwwwwwwwwwo.oo",
    ".ohwwwwwwwwwooo.",
    ".ohwwwwwwwwo....",
    "..ohwwwwwwo.....",
    "oooooooooooooo..",
    ".oooooooooooo...",
    "................",
]
COFFEE_COLOURS = {"o": (78, 46, 24), "w": (236, 222, 196), "h": (255, 250, 238), "c": (110, 64, 32), "C": (186, 132, 82),
                  "s": (226, 226, 232)}

# 4.1.71 (the owner, the CCG: "Cyan Blue with a brief cafe symbol"; their answer: "A briefcase (Recommended)"): an investigator's quinque
# case, steel with rounded corners, its handle over a gap, two latches on a band of the CCG's cyan blue (o: outline, h: light, m: steel,
# d: its shadow, c: cyan, l: the latches). Judged at 16 and 8 px against two others in the scratchpad: this one.
CASE = [
    "................",
    "................",
    ".....oooooo.....",
    "....ohhhhhho....",
    "....oo....oo....",
    ".oooooooooooooo.",
    "ohhhhhhhhhhhhhho",
    "ommmmmmmmmmmmmmo",
    "ommllmmmmmmllmmo",
    "occllccccccllcco",
    "occcccccccccccco",
    "ommmmmmmmmmmmmmo",
    "ommmmmmmmmmmmmmo",
    "oddddddddddddddo",
    ".oooooooooooooo.",
    "................",
]
CASE_COLOURS = {"o": (28, 36, 46), "h": (214, 224, 234), "m": (150, 164, 180), "d": (98, 112, 128), "c": (56, 182, 240), "l": (240, 240, 244)}

GLYPHS = [("aogiri_tree", TREE, TREE_COLOURS, ""), ("clown_face", CLOWN, CLOWN_COLOURS, ""),
          ("gas_toxic", TOXIC, TOXIC_COLOURS, ""), ("anteiku_coffee", COFFEE, COFFEE_COLOURS, "\ue003"),
          ("ccg_case", CASE, CASE_COLOURS, "\ue004")]
# The name's colour after each (the game's: Aogiri §4, the Clowns §9, the Gas Masks §a; 4.1.70: Anteiku's own light brown, GangPlayer.ANTEIKU_BROWN).
NAME_COLOURS = {"aogiri_tree": (170, 0, 0), "clown_face": (85, 85, 255), "gas_toxic": (85, 255, 85), "anteiku_coffee": (200, 155, 109),
                "ccg_case": (56, 182, 240)}


def image(rows, colours):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, c in enumerate(row):
            if c != ".":
                im.putpixel((x, y), colours[c] + (255,))
    return im


def write():
    (ASSETS / "textures" / "font").mkdir(parents=True, exist_ok=True)
    providers = []
    for name, rows, colours, char in GLYPHS:
        image(rows, colours).save(ASSETS / "textures" / "font" / (name + ".png"))
        providers.append({"type": "bitmap", "file": "hexkagune:font/" + name + ".png", "ascent": 7, "height": 8, "chars": [char]})
    font = ROOT / "src" / "main" / "resources" / "assets" / "minecraft" / "font"
    font.mkdir(parents=True, exist_ok=True)
    (font / "default.json").write_text(json.dumps({"providers": providers}, indent=2, ensure_ascii=True) + "\n")


def preview(out):
    """A mock (not the game's font): each emblem big, then in a name tag and a chat line at the game's sizes, scaled up 4 times."""
    scale = 4
    W, H = 1060, 660
    sheet = Image.new("RGB", (W, H), (58, 68, 80))
    d = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf", 8 * scale)
        small = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 15)
    except OSError:
        font = small = ImageFont.load_default()
    d.text((12, 8), "Gang emblems (4.1.50, 4.1.54, 4.1.70, 4.1.71): 16x16 glyphs, shown 8 px tall as a name's symbol (mock text, not the game's font)", fill=(230, 230, 230), font=small)
    x = 20
    for name, rows, colours, _ in GLYPHS:
        big = image(rows, colours).resize((160, 160), Image.NEAREST)
        sheet.paste(big, (x, 40), big)
        d.text((x, 205), name.replace("_", " "), fill=(230, 230, 230), font=small)
        x += 200
    y = 240
    for name, rows, colours, _ in GLYPHS:
        colour = NAME_COLOURS[name]
        # A name tag: dark translucent box, the emblem, a space, the name.
        glyph = image(rows, colours).resize((8 * scale, 8 * scale), Image.NEAREST)
        label = "Ken"
        tw = d.textlength(label, font=font)
        box_w = int(8 * scale + 4 * scale + tw + 4 * scale)
        d.rectangle((20, y, 20 + box_w, y + 11 * scale), fill=(28, 30, 36))
        sheet.paste(glyph, (20 + 2 * scale, y + int(1.5 * scale)), glyph)
        d.text((20 + 8 * scale + 4 * scale, y), label, fill=colour, font=font)
        # A chat line: <emblem name> hello
        cx = 260
        d.text((cx, y), "<", fill=(255, 255, 255), font=font)
        cx += int(d.textlength("<", font=font))
        sheet.paste(glyph, (cx, y + int(1.5 * scale)), glyph)
        cx += 8 * scale + 4 * scale
        d.text((cx, y), label, fill=colour, font=font)
        cx += int(d.textlength(label, font=font))
        d.text((cx, y), "> hey", fill=(255, 255, 255), font=font)
        y += 80
    sheet.save(out)
    print("wrote", out)


if __name__ == "__main__":
    write()
    if "--preview" in sys.argv:
        preview(sys.argv[sys.argv.index("--preview") + 1])
