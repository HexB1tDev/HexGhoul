#!/usr/bin/env python3
"""1.3.79: more children's looks (the owner: "grab the current children, and make dozens of other versions with different hair colors and
eye colors? (careful with the hair cause skins usually have hair on the back especially for women, they have hair drawn on their back of the
skin"): each child skin (the game's own processed copies, textures/entity/child/skins/<key>.png: never the owner's files) in VARIANTS other
hair colours and eye colours, written beside it as <key>_vN.png, and src/main/java/com/hex/kagune/control/family/ChildLooks.java: every
skin's looks (its own first) with their hair and eye colours, so a child wears the look of its skin nearest the hair and eyes its genes
gave it (its parents').

    python3 tools/child_variants.py [preview-dir]

The hair: its colours are read off the top and the back of the head (base and hat layers: what is not skin there, the commonest shades).
On the head, every pixel of those colours is hair (the face's front and the hat's front only where it joins the hairline at the face's
top or sides: never a mouth, never inside an eye box). On the body (the torso and its jacket, front, back, sides and top, and the arms'
tops), only what runs down unbroken from the shoulders is hair: long hair drawn down a back or over a chest, never a belt or a coat lower
down. A hair pixel takes the new colour by its own light against the hair's (its shading kept). The eyes: inside each hand-drawn eye box
(blink_skins.BOXES), what is iris (not white, not skin, not a black pupil or lash, not a glint) turns to the new eye colour by its own
light. Skins whose eyes are not boxed keep theirs. A skin can name its hair colours, or rectangles that are or are not hair, below.
"""
import colorsys
import sys
from collections import Counter
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import blink_skins as bs  # noqa: E402
import child_skins as cs  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
SKINS = ROOT / "src/main/resources/assets/hexkagune/textures/entity/child/skins"
JAVA = ROOT / "src/main/java/com/hex/kagune/control/family/ChildLooks.java"

VARIANTS = 4
# Hair colours a child may have (RGB): natural and the bright ones these skins' style has.
HAIRS = [(30, 26, 30), (62, 42, 32), (110, 72, 46), (140, 58, 36), (200, 108, 50), (222, 188, 112), (236, 228, 206), (206, 210, 220),
         (232, 140, 176), (80, 112, 206), (156, 126, 210), (90, 164, 138)]
# Eye colours (never red: that is the kakugan's).
EYES = [(70, 130, 220), (60, 160, 90), (210, 160, 40), (150, 90, 210), (40, 170, 180), (120, 80, 50), (120, 140, 160)]

# Where hair is looked for, in 64-pixel skin units (x0, y0, x1, y1). The head's faces (all of it hair where the colour matches)...
HEAD = [(0, 0, 32, 16), (32, 0, 64, 16)]
# ...but the face's front and the hat's front only where it joins the hairline.
FRONTS = [(8, 8, 16, 16), (40, 8, 48, 16)]
# The body's faces where long hair is drawn, each with the edge it hangs from ("top": runs down from its top row).
BODY = [(20, 20, 28, 32), (32, 20, 40, 32), (16, 20, 20, 32), (28, 20, 32, 32),      # torso front, back, right side, left side
        (20, 36, 28, 48), (32, 36, 40, 48), (16, 36, 20, 48), (28, 36, 32, 48),      # the jacket's
        (20, 16, 28, 20), (20, 32, 28, 36),                                          # torso top, jacket top
        (44, 16, 48, 20), (36, 48, 40, 52), (44, 32, 48, 36), (52, 48, 56, 52)]      # the arms' tops (and sleeves')
BACKS = {(32, 20, 40, 32), (32, 36, 40, 48)}
FRONT_BODY = {(20, 20, 28, 32), (20, 36, 28, 48)}
# Per skin: its hair colours by hand (RGB list), rectangles (64 units) that are hair whatever they look like, or never hair.
HAIR_COLOURS = {}
NOT_HAIR = {
    # A plaid hood with ears on the hat layer (all but its front); the hair is under it.
    "son_07": [(40, 0, 48, 8), (48, 0, 56, 8), (32, 8, 40, 16), (48, 8, 56, 16), (56, 8, 64, 16)],
    # Her maroon skirt's edges are near her pink hair's darkest shade.
    "daughter_13": [(20, 26, 28, 32), (20, 42, 28, 48)],
}
# Skins whose hair can't be recoloured cleanly (looked at on the previews): daughter_02's pale hair comes out striped, daughter_04's
# palest strands stay as white blobs, daughter_07's black hair and black hoodie are one colour, daughter_10's white bows and top are
# read as its hair, son_14's pale hair only tints at its edges (and its eyes are hidden).
NO_VARIANTS = {"daughter_02", "daughter_04", "daughter_07", "daughter_10", "son_14"}
# Skins whose hair is (partly) the skin's own colour, so only their eyes change (daughter_08: its bow was read as its hair; daughter_17's
# light brown and daughter_23's white came out striped or rimmed).
EYES_ONLY = {"daughter_08", "daughter_17", "daughter_23"}
# Girls whose clothes on the body are their hair's colour (daughter_06: a grey-brown sweater, daughter_07: a black hoodie): head only.
BODY_OFF = {"daughter_06", "daughter_07"}


def scaled(rect, s):
    return tuple(int(v * s) for v in rect)


def palette(image, s, tone):
    """The hair's shades: what covers the top of the face's front (its first row and a half, base and hat as seen), not skin, the
    commonest (each 4% or more), up to 10. (The head's top and back were tried first: a hood, a cap, a bow or a laurel was read as
    the hair there.)"""
    face = cs.Face(image)
    found = Counter()
    members = {}
    for y in range(0, max(1, int(1.5 * face.s))):
        for x in range(face.n):
            c = face.seen[y][x]
            if c[3] < 128 or cs.skinlike(c, tone):
                continue
            q = cs.quant(c, 16)
            found[q] += 1
            members.setdefault(q, []).append(c[:3])
    total = sum(found.values())
    if total < face.n // 2:
        return []  # hardly any: the hair is the skin's own colour (or none shows there)
    out = []
    for q, k in found.most_common(10):
        if k >= total * 0.04:
            cs_ = members[q]
            out.append(tuple(int(sum(c[i] for c in cs_) / len(cs_)) for i in range(3)))
    # A bright accessory in the fringe's corners (son_13's green horns) is not the hair: a shade far from the commonest's hue, or vivid
    # against grey hair, is dropped.
    if out:
        main = hsv(out[0])
        kept = [out[0]]
        for c in out[1:]:
            h = hsv(c)
            far = min(abs(h[0] - main[0]), 1.0 - abs(h[0] - main[0])) > 0.11
            if h[1] > 0.35 and (main[1] >= 0.15 and far or main[1] < 0.15 and h[1] > 0.5):
                continue
            kept.append(c)
        out = kept
    return out


def hsv(c):
    return colorsys.rgb_to_hsv(c[0] / 255.0, c[1] / 255.0, c[2] / 255.0)


class Hair:
    """What counts as the hair's colour: near one of its shades, or (coloured hair) of its hues, saturated and lit within its range."""

    def __init__(self, shades, tone):
        self.shades = shades
        self.tone = tone
        coloured = [hsv(h) for h in shades if hsv(h)[1] >= 0.25 and hsv(h)[2] >= 0.1]
        self.hues = [h[0] for h in coloured] if len(coloured) * 2 >= len(shades) else []
        if self.hues:
            self.smin = min(h[1] for h in coloured) * 0.6
            self.vmin = min(h[2] for h in coloured) * 0.5
            self.vmax = min(1.0, max(h[2] for h in coloured) * 1.25 + 0.15)
        # Greyish hair (black, grey, a dull brown-grey): any greyish pixel lit within its range (a lighter grey on the back of the head).
        greys = [hsv(h) for h in shades if hsv(h)[1] < 0.25]
        self.grey = len(greys) * 2 > len(shades)
        if self.grey:
            self.gmin = min(h[2] for h in greys) * 0.5
            self.gmax = min(1.0, max(h[2] for h in greys) * 1.5 + 0.08)

    def __call__(self, c):
        if c[3] < 128 or cs.skinlike(c, self.tone):
            return False
        if any(cs.dist(c, h) < 38 for h in self.shades):
            return True
        h, sat, v = hsv(c)
        if self.grey and sat < 0.3 and self.gmin <= v <= self.gmax:
            return True
        if not self.hues:
            return False
        return sat >= self.smin and self.vmin <= v <= self.vmax and any(min(abs(h - k), 1.0 - abs(h - k)) <= 0.045 for k in self.hues)


def eye_rects(key, s):
    """The eye boxes (blink_skins.BOXES) in the texture's pixels, for the face's front and the hat's front."""
    out = []
    for side in bs.BOXES.get(key, ()):
        for (x0, y0, x1, y1) in side:
            for ox in (8, 40):
                out.append((int((ox + x0) * s), int((8 + y0) * s), int((ox + x1) * s), int((8 + y1) * s)))
    return out


def inside(x, y, rects):
    return any(r[0] <= x < r[2] and r[1] <= y < r[3] for r in rects)


def flood(px, rect, seeds, hair, blocked):
    """The hair-coloured pixels of {@code rect} joined (8 ways) to {@code seeds}."""
    x0, y0, x1, y1 = rect
    done = set()
    stack = [p for p in seeds if hair(px[p[0], p[1]]) and not inside(p[0], p[1], blocked)]
    done.update(stack)
    while stack:
        x, y = stack.pop()
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                q = (x + dx, y + dy)
                if q in done or not (x0 <= q[0] < x1 and y0 <= q[1] < y1):
                    continue
                if inside(q[0], q[1], blocked) or not hair(px[q[0], q[1]]):
                    continue
                done.add(q)
                stack.append(q)
    return done


def hair_mask(key, image, s, tone, shades, girl):
    """The hair's pixels: the head's (as said above) and, on a girl's skin (boys' long hair is rare, and their hoodies and shirts are
    too often their hair's colour), what runs down the body from the shoulders."""
    hair = Hair(shades, tone)
    px = image.load()
    blocked = eye_rects(key, s) + [scaled(r, s) for r in NOT_HAIR.get(key, [])]
    fronts = [scaled(r, s) for r in FRONTS]
    mask = set()
    for rect in HEAD:
        x0, y0, x1, y1 = scaled(rect, s)
        for y in range(y0, y1):
            for x in range(x0, x1):
                if inside(x, y, fronts) or inside(x, y, blocked):
                    continue
                if hair(px[x, y]):
                    mask.add((x, y))
    for rect in fronts:
        x0, y0, x1, y1 = rect
        seeds = [(x, y0) for x in range(x0, x1)] + [(x0, y) for y in range(y0, y1)] + [(x1 - 1, y) for y in range(y0, y1)]
        mask |= flood(px, rect, seeds, hair, blocked)
    if not girl or key in BODY_OFF:
        return mask
    for r in BODY:
        rect = scaled(r, s)
        x0, y0, x1, y1 = rect
        seeds = [(x, y0) for x in range(x0, x1)]
        if r in BACKS:
            # Down a back, a bow or a clip may sit across the hair's top: what is in its top 40% starts it too.
            seeds += [(x, y) for y in range(y0, y0 + int(0.4 * (y1 - y0))) for x in range(x0, x1)]
        if r in FRONT_BODY:
            # Over a chest, hair hangs at the sides: only its outer 2.5 units (a dress's dark lines in the middle are never hair).
            w = int(2.5 * s)
            for side in ((x0, x0 + w), (x1 - w, x1)):
                part = (side[0], y0, side[1], y1)
                mask |= flood(px, part, [(x, y0) for x in range(side[0], side[1])], hair, blocked)
            continue
        mask |= flood(px, rect, seeds, hair, blocked)
    return mask


def iris_mask(key, image, s, tone):
    """The irises' pixels (texture coordinates) inside the eye boxes: coloured, not white, skin, a black pupil or lash, or a glint."""
    px = image.load()
    out = set()
    for (x0, y0, x1, y1) in eye_rects(key, s):
        for y in range(y0, y1):
            for x in range(x0, x1):
                c = px[x, y]
                if c[3] < 128 or cs.dist(c, tone) < 40 or bs.white(c, tone) or cs.lum(c) < 34 or cs.lum(c) > 235:
                    continue
                if cs.sat(c) < 18 and cs.lum(c) > 150:
                    continue  # a pale grey: the white's shading
                out.add((x, y))
    return out


def recolour(c, target, mean_lum, strength=1.0):
    """{@code c} as {@code target} by its own light against {@code mean_lum} (its shading kept)."""
    l = cs.lum(c)
    k = (l + 14.0) / (mean_lum + 14.0)
    tl = max(1.0, cs.lum(target))
    out = []
    for i in range(3):
        v = target[i] * k
        if k > 1.0:  # a highlight: toward white, not past the target's hue
            v = target[i] + (255 - target[i]) * min(1.0, (l - mean_lum) / max(1.0, 255 - mean_lum)) * 0.85
        out.append(int(max(0, min(255, round(v * strength + c[i] * (1 - strength))))))
    return tuple(out) + (c[3],)


def fringe(image, s):
    """The colour over the top of the face's front (for a skin whose hair can't be told from its skin)."""
    face = cs.Face(image)
    return mean([face.seen[y][x][:3] for y in range(max(1, int(1.5 * face.s))) for x in range(face.n) if face.seen[y][x][3] >= 128])


def mean(colours):
    return tuple(int(sum(c[i] for c in colours) / len(colours)) for i in range(3)) if colours else (0, 0, 0)


def choices(seed, pool, avoid, count, gap):
    """{@code count} of {@code pool}, seeded, none within {@code gap} of {@code avoid} or of each other."""
    order = sorted(range(len(pool)), key=lambda i: (i * 2654435761 + seed * 40503) % 1000003)
    out = []
    for i in order:
        c = pool[i]
        if cs.dist(c, avoid) < gap or any(cs.dist(c, o) < gap for o in out):
            continue
        out.append(c)
        if len(out) == count:
            break
    return out


def main():
    preview = Path(sys.argv[1]) if len(sys.argv) > 1 else None
    keys = sorted(p.stem for p in SKINS.glob("*.png") if p.stem.split("_")[-1].isdigit())
    for old in SKINS.glob("*_v[0-9].png"):
        old.unlink()
    rows = []
    sheets = []
    for n, key in enumerate(keys):
        image = Image.open(SKINS / (key + ".png")).convert("RGBA")
        s = image.size[0] // 64
        face = cs.Face(image)
        tone = cs.tone_of(face)
        shades = HAIR_COLOURS.get(key) or palette(image, s, tone)
        girl = key.startswith("daughter")
        hair = hair_mask(key, image, s, tone, shades, girl) if shades and key not in EYES_ONLY else set()
        irises = iris_mask(key, image, s, tone) if key in bs.BOXES else set()
        px = image.load()
        hair_mean = mean([px[p[0], p[1]] for p in hair]) if hair else fringe(image, s)
        eye_mean = mean([px[p[0], p[1]] for p in irises])
        hair_lum = cs.lum(hair_mean + (255,)) if hair else 0
        eye_lum = cs.lum(eye_mean + (255,)) if irises else 0
        looks = [(key, hair_mean, eye_mean if irises else (0, 0, 0))]
        made = []
        if (hair or irises) and key not in NO_VARIANTS:
            # Pale hair that can't be told from the skin: the eyes alone change.
            hairs = choices(n, HAIRS, hair_mean, VARIANTS, 70) if hair else [hair_mean] * VARIANTS
            eyes = choices(n * 7 + 3, EYES, eye_mean, VARIANTS, 60) if irises else []
            for v, new_hair in enumerate(hairs):
                out = image.copy()
                op = out.load()
                for (x, y) in hair:
                    op[x, y] = recolour(px[x, y], new_hair, hair_lum)
                new_eyes = eyes[v % len(eyes)] if eyes else None
                if new_eyes:
                    for (x, y) in irises:
                        op[x, y] = recolour(px[x, y], new_eyes, eye_lum)
                name = f"{key}_v{v + 1}"
                out.save(SKINS / (name + ".png"), optimize=True)
                looks.append((name, new_hair, new_eyes or (eye_mean if irises else (0, 0, 0))))
                made.append(out)
        rows.append((key, looks))
        if preview is not None:
            mark = image.copy()
            mp = mark.load()
            for (x, y) in hair:
                mp[x, y] = (0, 255, 255, 255)
            for (x, y) in irises:
                mp[x, y] = (255, 0, 255, 255)
            sheets.append((key, image, mark, made))
        print(f"{key}: hair {len(hair)} px in {len(shades)} shades, irises {len(irises)} px, {len(looks) - 1} versions")
    write_java(rows)
    if preview is not None:
        write_previews(preview, sheets)


def write_java(rows):
    lines = []
    for key, looks in rows:
        parts = ", ".join(f'new Look("{name}", 0x{h[0]:02X}{h[1]:02X}{h[2]:02X}, 0x{e[0]:02X}{e[1]:02X}{e[2]:02X})' for name, h, e in looks)
        lines.append(f'        Map.entry("{key}", new Look[] {{{parts}}})')
    body = ",\n".join(lines)
    JAVA.write_text(f'''package com.hex.kagune.control.family;

import java.util.Map;

/**
 * 1.3.79: each child skin's looks (pure; written by {{@code tools/child_variants.py}}: do not edit by hand): the skin as it is, and
 * versions of it with other hair and eye colours ({{@code <key>_vN.png}}). A child wears its skin ({{@link ChildSkins#pick}}) in the look
 * whose hair and eyes are nearest the ones its genes gave it, its parents' ({{@link #pick}}). The versions share the skin's overlays
 * (kakugan, shut eyes, blush): the eyes are where they were.
 */
public final class ChildLooks {{
    private ChildLooks() {{
    }}

    /** One look: its texture's key, its hair colour and its eye colour (RGB; 0 for eyes not boxed). */
    public record Look(String texture, int hair, int eyes) {{
    }}

    static final Map<String, Look[]> LOOKS = Map.ofEntries(
{body});

    /** Every look of skin {{@code key}} (its own first); just its own if it has no versions. */
    public static Look[] of(String key) {{
        Look[] looks = LOOKS.get(key);
        return looks != null ? looks : new Look[] {{new Look(key, 0, 0)}};
    }}

    /** The look of skin {{@code key}} nearest hair {{@code hair}} and eyes {{@code eyes}} (RGB): hair counts twice as much as eyes. */
    public static Look pick(String key, int hair, int eyes) {{
        Look best = null;
        double bestScore = Double.MAX_VALUE;
        for (Look look : of(key)) {{
            double score = 2.0 * ChildSkins.distance(look.hair(), hair) + (look.eyes() == 0 ? 0.0 : ChildSkins.distance(look.eyes(), eyes));
            if (score < bestScore) {{
                bestScore = score;
                best = look;
            }}
        }}
        return best;
    }}
}}
''')


def flat(image, back=False):
    """A flat front (or back) view of a 64-unit skin, 16 x 32 units."""
    s = image.size[0] // 64
    fig = Image.new("RGBA", (16 * s, 32 * s), (0, 0, 0, 0))

    def put(x, y, w, h, at):
        fig.alpha_composite(image.crop((x * s, y * s, (x + w) * s, (y + h) * s)), (at[0] * s, at[1] * s))

    if not back:
        put(8, 8, 8, 8, (4, 0)); put(40, 8, 8, 8, (4, 0))
        put(20, 20, 8, 12, (4, 8)); put(20, 36, 8, 12, (4, 8))
        put(44, 20, 4, 12, (0, 8)); put(44, 36, 4, 12, (0, 8))
        put(36, 52, 4, 12, (12, 8)); put(52, 52, 4, 12, (12, 8))
        put(4, 20, 4, 12, (4, 20)); put(4, 36, 4, 12, (4, 20))
        put(20, 52, 4, 12, (8, 20)); put(4, 52, 4, 12, (8, 20))
    else:
        put(24, 8, 8, 8, (4, 0)); put(56, 8, 8, 8, (4, 0))
        put(32, 20, 8, 12, (4, 8)); put(32, 36, 8, 12, (4, 8))
        put(52, 20, 4, 12, (12, 8)); put(52, 36, 4, 12, (12, 8))
        put(44, 52, 4, 12, (0, 8)); put(60, 52, 4, 12, (0, 8))
        put(12, 20, 4, 12, (8, 20)); put(12, 36, 4, 12, (8, 20))
        put(28, 52, 4, 12, (4, 20)); put(12, 52, 4, 12, (4, 20))
    return fig


def write_previews(folder, sheets):
    from PIL import ImageDraw
    folder.mkdir(parents=True, exist_ok=True)
    U = 6  # screen pixels per skin unit
    per = 4
    for i in range(0, len(sheets), per):
        part = sheets[i:i + per]
        cols = 4 + VARIANTS * 2
        sheet = Image.new("RGBA", (cols * (16 * U + 6), len(part) * (32 * U + 18)), (46, 46, 56, 255))
        d = ImageDraw.Draw(sheet)
        for j, (key, image, mark, made) in enumerate(part):
            views = [flat(image), flat(image, True), flat(mark), flat(mark, True)]
            for m in made:
                views += [flat(m), flat(m, True)]
            for k, v in enumerate(views):
                sheet.alpha_composite(v.resize((16 * U, 32 * U), Image.NEAREST), (k * (16 * U + 6), j * (32 * U + 18)))
            d.text((2, j * (32 * U + 18) + 32 * U + 3), key + "   (front, back | hair cyan, irises magenta | versions front/back)", fill=(255, 255, 255, 255))
        sheet.save(folder / f"variants_{i // per:02d}.png")


if __name__ == "__main__":
    main()
