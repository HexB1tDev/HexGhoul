#!/usr/bin/env python3
"""1.0.41: the wild ghouls' faces: 24 player skins (12 boys, 12 girls on slim arms), painted here pixel by pixel in vanilla's 64x64
layout (both layers) so they can be made again exactly. Every one keeps its eyes where the kakugan is drawn (NpcLook): the right eye
on columns 1-2, the left on 5-6 of the face, from row 4 (one or two rows tall), the iris toward the nose, nothing over them.

Usage: python3 tools/ghoul_skins.py [out-dir]   (default: src/main/resources/assets/hexkagune/textures/entity/ghoul)
"""
import os
import random
import sys

from PIL import Image

# ---------------------------------------------------------------------------------------------------- colour helpers


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def shade(c, k):
    return tuple(max(0, min(255, int(round(v * k)))) for v in c)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


SKIN = {
    "pale": rgb(0xF3D9C6), "light": rgb(0xEBC5A8), "fair": rgb(0xE2B593), "medium": rgb(0xC9946C), "tan": rgb(0xA8724C), "dark": rgb(0x6E4630),
}
HAIR = {
    "black": rgb(0x1B1A20), "blueblack": rgb(0x1E2333), "dark": rgb(0x2E211B), "brown": rgb(0x5A3A24), "chestnut": rgb(0x7A4A2C),
    "auburn": rgb(0x8A3A22), "blonde": rgb(0xD9B86A), "ash": rgb(0xB9B4B0), "white": rgb(0xE9E7EE), "purple": rgb(0x4A2E5E),
    "red": rgb(0x9A1E26), "navy": rgb(0x27355E), "pink": rgb(0xD98BA4), "teal": rgb(0x2C6A6E),
}
EYES = {"brown": rgb(0x5A3A22), "dark": rgb(0x2A2226), "grey": rgb(0x5E6670), "hazel": rgb(0x7A6430), "blue": rgb(0x3E5A86), "violet": rgb(0x5E4A86), "amber": rgb(0xA8782A)}
WHITE = (238, 238, 240)

# ---------------------------------------------------------------------------------------------------- the 64x64 layout

# Boxes: (u0, v0, width, height, depth) of the base layer and of the second layer.
WIDE_ARM = 4
SLIM_ARM = 3


def faces(u0, v0, w, h, d):
    """Each face's rectangle on the texture: (u, v, width, height)."""
    return {
        "top": (u0 + d, v0, w, d), "bottom": (u0 + d + w, v0, w, d),
        "right": (u0, v0 + d, d, h), "front": (u0 + d, v0 + d, w, h), "left": (u0 + d + w, v0 + d, d, h), "back": (u0 + 2 * d + w, v0 + d, w, h),
    }


def ring(face, x, w, d):
    """Where column x of a side face is round the box (0 at the back of its right side, running forward, round the front, back)."""
    return {"right": x, "front": d + x, "left": d + w + x, "back": 2 * d + w + x}[face]


# ---------------------------------------------------------------------------------------------------- a ghoul


class Ghoul:
    def __init__(self, spec, seed):
        self.s = spec
        self.rng = random.Random(seed)
        self.girl = spec["girl"]
        self.arm = SLIM_ARM if self.girl else WIDE_ARM
        self.skin = SKIN[spec["skin"]]
        self.hair = HAIR[spec["hair"]]
        self.eye = EYES[spec.get("eyes", "brown")]
        self.tall = spec.get("tall", self.girl)
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.noise = {}

    # -------------------------------------------------------------------------------- painting

    def put(self, x, y, c, alpha=255):
        if c is None:
            return
        if len(c) == 4:
            if c[3] == 0:
                return
            alpha = c[3]
            c = c[:3]
        # A whisper of grain, the same for a texel every time.
        n = self.noise.setdefault((x, y), 1.0 + (self.rng.random() - 0.5) * 0.06)
        self.img.putpixel((x, y), shade(c, n) + (alpha,))

    def box(self, u0, v0, w, h, d, paint):
        for face, (fu, fv, fw, fh) in faces(u0, v0, w, h, d).items():
            for y in range(fh):
                for x in range(fw):
                    self.put(fu + x, fv + y, paint(face, x, y, w, h, d))

    # -------------------------------------------------------------------------------- hair

    def hair_at(self, r, y, length, jag=0):
        """Hair colour at ring column r, row y if the hair reaches row {length} there (else None); streaked lighter and darker."""
        reach = length + (jag and self.jag(r, jag))
        if y >= reach:
            return None
        k = 1.0
        if (r * 7 + y * 3) % 5 == 0:
            k = 1.18
        elif (r * 3 + y) % 4 == 0:
            k = 0.82
        if y == reach - 1:
            k *= 0.86  # the ends a shade darker
        return shade(self.hair, k)

    def jag(self, r, amount):
        key = ("jag", r)
        if key not in self.noise:
            self.noise[key] = self.rng.randint(-amount, amount)
        return self.noise[key]

    # -------------------------------------------------------------------------------- the head

    def head_base(self, face, x, y, w, h, d):
        s = self.s
        if face == "top":
            return shade(self.hair, 1.1 if (x + y) % 3 == 0 else 1.0)
        if face == "bottom":
            return shade(self.skin, 0.9)
        r = ring(face, x, w, d)
        if face == "front":
            return self.face(x, y)
        if face == "back":
            c = self.hair_at(r, y, s["back"])
            return c if c else shade(self.skin, 0.92)
        # The sides: hair down to its length, the ear just behind the cheek.
        c = self.hair_at(r, y, s["side"])
        if c:
            return c
        if face == "right" and x in (4, 5) and y in (3, 4) or face == "left" and x in (2, 3) and y in (3, 4):
            return shade(self.skin, 0.88)
        return shade(self.skin, 0.95)

    def face(self, x, y):
        s = self.s
        fringe = s["fringe"][x]
        if y < fringe:
            return shade(self.hair, 1.08 if (x + y) % 3 == 1 else 0.95)
        eye_rows = (4, 5) if self.tall else (4,)
        # The eyes: white outside, iris toward the nose (KakuganEyes(1, 4, 5, 4)).
        if y in eye_rows and x in (1, 2, 5, 6):
            iris = x in (2, 5)
            if iris:
                return shade(self.eye, 0.7) if (self.tall and y == 4) else self.eye
            return WHITE
        if y == 3 and x in (1, 2, 5, 6):
            if self.girl:
                return (36, 26, 30)  # lashes
            return shade(self.hair, 0.7) if self.s["hair"] not in ("white", "ash", "blonde", "pink") else shade(self.hair, 0.6)
        if self.girl and y == 3 and x in (0, 7):
            return (36, 26, 30)  # the lash's flick
        mouth = 7 if self.tall else 6
        if y == mouth - (1 if self.tall else 0) and x in (3, 4):
            return mix(self.skin, (170, 70, 80), 0.35 if self.girl else 0.18)
        if not self.tall and y == 5 and x in (3, 4):
            return shade(self.skin, 0.93)  # under the nose
        if self.girl and y == (6 if self.tall else 5) and x in (1, 6):
            return mix(self.skin, (230, 120, 130), 0.25)  # blush
        if y == 7:
            return shade(self.skin, 0.94)
        return self.skin

    def head_over(self, face, x, y, w, h, d):
        s = self.s
        if s.get("beanie"):
            return self.beanie(face, x, y, w, d)
        if face == "top":
            return shade(self.hair, 1.15 if (x * 3 + y) % 4 == 0 else 1.02)
        if face == "bottom":
            return None
        r = ring(face, x, w, d)
        if face == "front":
            if s.get("patch") and x in (4, 5, 6, 7) and y in (3, 4, 5):
                return (226, 226, 222) if x in (5, 6) or y == 3 else (200, 200, 196)  # the medical eyepatch
            if s.get("patch") and y == 3:
                return (200, 200, 196)  # its strap
            over = s.get("over_fringe", [min(2, f) for f in s["fringe"]])[x]
            if y < over:
                return shade(self.hair, 1.12 if (x + y) % 2 == 0 else 1.0)
            if x in (0, 7) and y < s.get("locks", 0):
                return shade(self.hair, 0.95 if y % 3 else 1.1)  # locks framing the face
            return None
        if s.get("patch") and face == "left" and y == 3:
            return (200, 200, 196)
        length = s["over_back"] if face == "back" else s["over_side"]
        return self.hair_at(r, y, length, s.get("jag", 0))

    def beanie(self, face, x, y, w, d):
        b = rgb(self.s["beanie"])
        if face == "top":
            return shade(b, 1.05)
        if face == "bottom":
            return None
        if y < 3:
            return shade(b, 0.85 if y == 2 else (1.0 if x % 2 else 0.93))  # ribbed, the cuff darker
        if face != "front" and y < 4:
            return self.hair_at(ring(face, x, w, d), y, 5)
        return None

    # -------------------------------------------------------------------------------- the clothes

    def colour(self, key, default=None):
        v = self.s.get(key, default)
        return rgb(v) if isinstance(v, int) else v

    def body_base(self, face, x, y, w, h, d):
        o = self.s["outfit"]
        under = self.colour("under", 0xE8E8E8)
        top = self.colour("top", under)
        if face == "top":
            return self.skin if o in ("tank", "dress") and x in (2, 3, 4, 5) else top
        if face == "bottom":
            return self.colour("pants")
        if o in ("jacket", "coat", "waiter", "school", "punk", "trench"):
            # Under a jacket: the shirt (seen at the front where it is open).
            c = under
            if o == "school" and face == "front" and x in (3, 4) and 1 <= y <= 7:
                return self.colour("tie", 0x9A1E2A) if y < 7 else shade(self.colour("tie", 0x9A1E2A), 0.8)
            if o == "waiter" and face == "front" and x in (3, 4) and y in (1, 2):
                return rgb(0x1A1A1E)  # the bow
            if face == "front" and y == 0 and x in (3, 4):
                return self.skin  # the open collar
            return shade(c, 0.95 if y % 4 == 3 else 1.0)
        if o == "dress":
            return self.dress(face, x, y)
        if o == "tank":
            if face == "front" and y < 2 and x in (2, 3, 4, 5):
                return self.skin
            return top
        if o == "hoodie":
            if face == "front":
                if y == 0 and x in (3, 4):
                    return self.skin
                if x in (3, 4) and 1 <= y <= 4 and y % 2 == 1:
                    return (235, 235, 230)  # drawstrings
                if 7 <= y <= 9 and 1 <= x <= 6:
                    return shade(top, 0.82 if y == 7 or x in (1, 6) else 0.92)  # the pouch
            if y == 11:
                return shade(top, 0.8)  # the hem
            return top
        if o == "track":
            if face == "front" and x in (3, 4):
                return (220, 220, 220) if x == 3 else shade(top, 0.7)  # the zip
            if face in ("right", "left") and x in (1, 2):
                return (230, 230, 230)  # stripes down the sides
            return top
        if o == "turtle":
            if y <= 1:
                return shade(top, 1.08)  # the roll of the neck
            return shade(top, 0.9 if x % 2 else 1.0)  # ribbed knit
        return top

    def dress(self, face, x, y):
        top = self.colour("top", 0x7A1E2A)
        if face == "front" and y == 0 and x in (2, 3, 4, 5):
            return self.skin
        if y == 6:
            return self.colour("belt", 0x1E1A1A)
        return shade(top, 1.05 if face == "front" and x in (3, 4) and y < 6 else 1.0)

    def body_over(self, face, x, y, w, h, d):
        o = self.s["outfit"]
        s = self.s
        jacket = self.colour("jacket", None)
        hair_back = self.long_hair_back(face, x, y)
        if hair_back:
            return hair_back
        if face == "top" or face == "bottom":
            if o == "hoodie" and face == "top" and y >= 2:
                return shade(self.colour("top"), 0.9)  # the hood's fold behind the neck
            return jacket if jacket and face == "top" else None
        if s.get("scarf") and y <= 1:
            return shade(rgb(s["scarf"]), 1.0 if (x + y) % 2 else 0.85)
        if o == "hoodie" and face == "back" and y <= 2:
            return shade(self.colour("top"), 0.8 if y == 2 else 0.92)  # the hood hanging down
        if not jacket:
            return None
        if o == "waiter":
            if face == "front" and (x in (0, 7) or y >= 2 and x in (1, 6) or y >= 4 and x in (2, 5)):
                return shade(jacket, 1.0 if y != 11 else 0.8)
            if face in ("back", "right", "left") and y >= 1:
                return jacket
            return None
        if face == "front":
            open_cols = (3, 4) if o in ("coat", "trench") else (2, 3, 4, 5)
            if o in ("coat", "trench"):
                if y == 0 and x in (2, 3, 4, 5):
                    return shade(jacket, 1.12)  # the collar turned up
                if x in open_cols and y <= 2:
                    return None  # the shirt at the neck
                if x == 4 and y in (4, 7, 10):
                    return (40, 36, 34)  # buttons
                if o == "trench" and y == 6:
                    return shade(jacket, 0.75)  # the belt
                return shade(jacket, 0.88 if x == 3 else 1.0)
            if x in open_cols:
                return None
            if x in (1, 6) and y <= 3:
                return shade(jacket, 1.15)  # lapels
            if o == "punk" and x in (0, 1) and y in (2, 5, 8):
                return (200, 200, 205)  # studs
            return shade(jacket, 0.82 if y == 11 else 1.0)
        if o == "trench" and y == 6:
            return shade(jacket, 0.75)
        return shade(jacket, 0.9 if face == "back" and x in (3, 4) and y > 4 else 1.0)

    def long_hair_back(self, face, x, y):
        """Long hair down the back (and a ponytail, twin tails), on the body's second layer."""
        s = self.s
        down = s.get("down", 0)
        if not down:
            return None
        if face == "back":
            if s.get("hair_tails") == "pony":
                if x in (3, 4) and y < down:
                    return shade(self.hair, 1.1 if y % 2 else 0.92)
                return None
            if s.get("hair_tails") == "twin":
                if x in (0, 1, 6, 7) and y < down:
                    return shade(self.hair, 1.1 if (x + y) % 2 else 0.92)
                return None
            reach = down - (1 if x in (0, 7) else 0)
            if y < reach:
                return shade(self.hair, 1.1 if (x * 2 + y) % 5 == 0 else (0.86 if y == reach - 1 else 1.0))
        if face in ("right", "left") and s.get("hair_tails") is None and y < min(3, down):
            col = x if face == "right" else 3 - x
            if col <= 1:
                return shade(self.hair, 0.95)
        return None

    # -------------------------------------------------------------------------------- the arms and legs

    def arm_base(self, side):
        def paint(face, x, y, w, h, d):
            o = self.s["outfit"]
            sleeve = self.colour("sleeve", self.s.get("top", self.s.get("under", 0xE8E8E8)))
            short = o in ("tank",) or self.s.get("short_sleeves")
            if face == "bottom" or y >= 10:
                return self.hand(y)
            if face == "top":
                return self.skin if o == "tank" else sleeve
            if short and y >= (0 if o == "tank" else 4):
                return self.skin
            if o == "track" and face == ("right" if side == "right" else "left") and x in (1, 2):
                return (230, 230, 230)
            if o == "hoodie" and y == 9:
                return shade(sleeve, 0.8)  # the cuff
            if o == "waiter" and y == 7:
                return shade(sleeve, 0.9)  # rolled up
            if o == "waiter" and y >= 8:
                return self.skin
            return shade(sleeve, 0.95 if y % 5 == 4 else 1.0)
        return paint

    def hand(self, y):
        if self.s.get("gloves"):
            return rgb(self.s["gloves"])
        return self.skin if y < 11 else shade(self.skin, 0.92)

    def arm_over(self, side):
        def paint(face, x, y, w, h, d):
            jacket = self.colour("jacket", None)
            o = self.s["outfit"]
            if not jacket or o == "waiter":
                return None
            if face == "bottom":
                return None
            if face == "top":
                return jacket
            if y >= 10:
                return None
            if y == 9:
                return shade(jacket, 0.82)  # the cuff
            if o == "punk" and y in (2, 5) and face == ("right" if side == "right" else "left"):
                return (200, 200, 205)
            return jacket
        return paint

    def leg_base(self, side):
        def paint(face, x, y, w, h, d):
            s = self.s
            pants = self.colour("pants")
            shoes = self.colour("shoes", 0x222226)
            shoe_from = 10 if not s.get("boots") else 8
            if face == "bottom":
                return shade(shoes, 0.7)
            if face == "top":
                return pants
            if y >= shoe_from:
                if face == "front" and y == shoe_from and not s.get("boots"):
                    return shade(shoes, 1.25)  # the laces' light
                return shoes if y < 11 else shade(shoes, 0.75)
            legs = s.get("legs")  # bare legs or stockings under a skirt
            if legs and y >= s.get("skirt", 0):
                if legs == "bare":
                    return self.skin
                if legs == "socks":
                    return (236, 236, 236) if y >= 6 else self.skin
                return rgb(legs)
            if s.get("pants_style") == "jeans":
                if face == "front" and x == (2 if side == "right" else 1):
                    return shade(pants, 0.85)  # the seam
                if s.get("ripped") and face == "front" and y in (4, 5) and x in (1, 2):
                    return self.skin if side == "right" else shade(pants, 1.3)
                return shade(pants, 1.06 if y in (5, 6) and face == "front" else 1.0)
            if s.get("pants_style") == "track" and face == ("right" if side == "right" else "left") and x in (1, 2):
                return (230, 230, 230)
            if s.get("pants_style") == "cargo" and face == ("right" if side == "right" else "left") and 4 <= y <= 6:
                return shade(pants, 0.82 if y == 4 or x in (0, 3) else 0.92)
            return pants
        return paint

    def leg_over(self, side):
        def paint(face, x, y, w, h, d):
            s = self.s
            skirt = s.get("skirt", 0)
            if skirt and y < skirt and face != "top" and face != "bottom":
                c = self.colour("skirt_colour", s.get("pants"))
                if s.get("plaid"):
                    return shade(c, 0.8 if (x + y) % 3 == 0 else (1.15 if x % 3 == 1 else 1.0))
                return shade(c, 0.88 if x % 2 else 1.0)  # pleats
            if s["outfit"] in ("coat", "trench") and y < s.get("tails", 7) and face != "top" and face != "bottom":
                jacket = self.colour("jacket")
                inside = face == ("left" if side == "right" else "right")
                if inside:
                    return None
                if face == "front" and x == (3 if side == "right" else 0):
                    return None  # the coat open down the middle
                return shade(jacket, 0.9 if y == s.get("tails", 7) - 1 else 1.0)
            if s.get("boots") and y >= 7 and face not in ("top", "bottom"):
                return shade(self.colour("shoes", 0x222226), 1.1 if y == 7 else 1.0)
            return None
        return paint

    # -------------------------------------------------------------------------------- the whole skin

    def paint(self):
        a = self.arm
        self.box(0, 0, 8, 8, 8, self.head_base)
        self.box(32, 0, 8, 8, 8, self.head_over)
        self.box(16, 16, 8, 12, 4, self.body_base)
        self.box(16, 32, 8, 12, 4, self.body_over)
        self.box(40, 16, a, 12, 4, self.arm_base("right"))
        self.box(40, 32, a, 12, 4, self.arm_over("right"))
        self.box(32, 48, a, 12, 4, self.arm_base("left"))
        self.box(48, 48, a, 12, 4, self.arm_over("left"))
        self.box(0, 16, 4, 12, 4, self.leg_base("right"))
        self.box(0, 32, 4, 12, 4, self.leg_over("right"))
        self.box(16, 48, 4, 12, 4, self.leg_base("left"))
        self.box(0, 48, 4, 12, 4, self.leg_over("left"))
        return self.img


# ---------------------------------------------------------------------------------------------------- the 24

B = False
G = True
GHOULS = [
    # Boys.
    dict(name="boy_01", girl=B, skin="light", hair="white", fringe=[2, 3, 2, 3, 2, 3, 2, 2], side=3, back=5, over_side=4, over_back=6, jag=1,
         eyes="grey", outfit="jacket", jacket=0x1C1C22, under=0x2A2A30, sleeve=0x1C1C22, pants=0x16161A, pants_style="slacks", shoes=0x0E0E10,
         patch=True),
    dict(name="boy_02", girl=B, skin="fair", hair="black", fringe=[2, 3, 2, 2, 3, 2, 3, 2], side=3, back=4, over_side=3, over_back=5, jag=1,
         eyes="dark", outfit="hoodie", top=0x2E3A4E, pants=0x2E3E62, pants_style="jeans", shoes=0xE8E8E8),
    dict(name="boy_03", girl=B, skin="medium", hair="brown", fringe=[2, 2, 1, 1, 1, 2, 2, 2], side=2, back=3, over_side=2, over_back=3,
         eyes="brown", outfit="waiter", jacket=0x1A1A1E, under=0xF2F2F0, sleeve=0xF2F2F0, pants=0x1A1A1E, pants_style="slacks", shoes=0x141414),
    dict(name="boy_04", girl=B, skin="tan", hair="dark", fringe=[1, 1, 1, 1, 1, 1, 1, 1], side=1, back=2, over_side=0, over_back=0,
         eyes="brown", outfit="track", top=0x8E1C24, pants=0x22222A, pants_style="track", shoes=0xEEEEEE),
    dict(name="boy_05", girl=B, skin="pale", hair="purple", fringe=[3, 3, 2, 2, 2, 2, 3, 3], side=4, back=6, over_side=5, over_back=7, jag=1,
         eyes="violet", outfit="coat", jacket=0x2A1E2E, under=0xDAD2D6, sleeve=0x2A1E2E, pants=0x1E1A20, pants_style="slacks", shoes=0x141016,
         tails=7, scarf=0x6A1E2A),
    dict(name="boy_06", girl=B, skin="dark", hair="black", fringe=[1, 1, 1, 1, 1, 1, 1, 1], side=1, back=2, over_side=2, over_back=3, jag=1,
         eyes="dark", outfit="punk", jacket=0x1E1C1C, under=0x6E1E22, sleeve=0x1E1C1C, pants=0x3A4256, pants_style="jeans", ripped=True,
         boots=True, shoes=0x1A1616),
    dict(name="boy_07", girl=B, skin="light", hair="blonde", fringe=[2, 2, 2, 1, 2, 2, 2, 2], side=2, back=3, over_side=3, over_back=4, jag=1,
         eyes="blue", outfit="school", jacket=0x1E2A44, under=0xF0F0EE, sleeve=0x1E2A44, tie=0x8A1A24, pants=0x4A4A52, pants_style="slacks",
         shoes=0x2A1A14),
    dict(name="boy_08", girl=B, skin="fair", hair="red", fringe=[3, 2, 2, 3, 2, 2, 3, 3], side=3, back=4, over_side=4, over_back=5, jag=2,
         eyes="brown", outfit="turtle", top=0x2A2A2A, pants=0x3A3226, pants_style="cargo", shoes=0x2E2620),
    dict(name="boy_09", girl=B, skin="medium", hair="ash", fringe=[2, 2, 2, 2, 2, 2, 2, 2], side=2, back=3, over_side=3, over_back=4,
         eyes="grey", outfit="trench", jacket=0x9A8466, under=0x4A4A50, sleeve=0x9A8466, pants=0x2A2A30, pants_style="slacks", shoes=0x3A2A1E,
         tails=6),
    dict(name="boy_10", girl=B, skin="tan", hair="blueblack", fringe=[2, 3, 3, 2, 2, 3, 3, 2], side=3, back=4, over_side=3, over_back=4, jag=1,
         eyes="dark", outfit="hoodie", top=0x4A4A4E, pants=0x1E1E22, pants_style="cargo", shoes=0x1A1A1A, beanie=0x2A2A2E),
    dict(name="boy_11", girl=B, skin="light", hair="chestnut", fringe=[2, 2, 1, 2, 2, 1, 2, 2], side=2, back=3, over_side=2, over_back=3,
         eyes="hazel", outfit="jacket", jacket=0x5A3A22, under=0xE8E2D6, sleeve=0x5A3A22, pants=0x2E3E62, pants_style="jeans", shoes=0x6A4A2E),
    dict(name="boy_12", girl=B, skin="pale", hair="teal", fringe=[3, 3, 3, 2, 3, 3, 3, 3], side=4, back=5, over_side=4, over_back=6, jag=2,
         eyes="grey", outfit="tank", top=0x1A1A1E, pants=0x2A2A2E, pants_style="track", shoes=0xE8E8E8, gloves=0x1A1A1E),
    # Girls.
    dict(name="girl_01", girl=G, skin="pale", hair="black", fringe=[3, 3, 3, 3, 3, 3, 3, 3], side=8, back=8, over_side=8, over_back=8, locks=7,
         eyes="dark", outfit="waiter", jacket=0x1A1A1E, under=0xF2F2F0, sleeve=0xF2F2F0, pants=0x1A1A1E, skirt=5, skirt_colour=0x1A1A1E,
         legs=0x1A1A20, shoes=0x101012, down=6),
    dict(name="girl_02", girl=G, skin="light", hair="purple", fringe=[3, 2, 2, 3, 2, 2, 3, 3], side=6, back=8, over_side=6, over_back=8, locks=5,
         eyes="violet", outfit="hoodie", top=0x3A2A4A, pants=0x1E1E26, pants_style="jeans", shoes=0xE8E8E8, down=3),
    dict(name="girl_03", girl=G, skin="fair", hair="chestnut", fringe=[2, 2, 2, 2, 2, 2, 2, 2], side=5, back=6, over_side=5, over_back=7,
         eyes="hazel", outfit="school", jacket=0x22283E, under=0xF0F0EE, sleeve=0x22283E, tie=0xA01E2E, pants=0x3A2A3A, skirt=5,
         skirt_colour=0x4A2A36, plaid=True, legs="socks", shoes=0x2A1A14, hair_tails="pony", down=7),
    dict(name="girl_04", girl=G, skin="medium", hair="dark", fringe=[2, 3, 2, 2, 2, 2, 3, 2], side=7, back=8, over_side=7, over_back=8, locks=6,
         eyes="brown", outfit="dress", top=0x7A1E2A, pants=0x7A1E2A, skirt=6, skirt_colour=0x7A1E2A, legs="bare", shoes=0x1A1414,
         short_sleeves=True, down=8),
    dict(name="girl_05", girl=G, skin="pale", hair="white", fringe=[3, 3, 2, 2, 2, 2, 3, 3], side=4, back=5, over_side=4, over_back=5,
         eyes="grey", outfit="coat", jacket=0x1A1A20, under=0x2A2A32, sleeve=0x1A1A20, pants=0x1A1A20, pants_style="slacks", shoes=0x0E0E10,
         tails=8, scarf=0x8A1E26),
    dict(name="girl_06", girl=G, skin="tan", hair="pink", fringe=[2, 2, 2, 2, 2, 2, 2, 2], side=6, back=8, over_side=6, over_back=8,
         eyes="brown", outfit="track", top=0x2A2A32, pants=0x2A2A32, pants_style="track", shoes=0xEEEEEE, hair_tails="twin", down=8),
    dict(name="girl_07", girl=G, skin="dark", hair="black", fringe=[2, 2, 1, 2, 2, 1, 2, 2], side=4, back=5, over_side=5, over_back=6, jag=1,
         eyes="amber", outfit="punk", jacket=0x1E1C1C, under=0x2A2A2E, sleeve=0x1E1C1C, pants=0x1E1E22, skirt=4, skirt_colour=0x1E1E22,
         legs=0x1A1A1E, boots=True, shoes=0x1A1616),
    dict(name="girl_08", girl=G, skin="light", hair="blonde", fringe=[3, 3, 2, 2, 2, 2, 3, 3], side=7, back=8, over_side=7, over_back=8, locks=7,
         eyes="blue", outfit="turtle", top=0xD6CFC2, pants=0x3A3A44, skirt=5, skirt_colour=0x3A3A44, legs=0x262628, shoes=0x2A1E18, down=8),
    dict(name="girl_09", girl=G, skin="fair", hair="auburn", fringe=[2, 3, 3, 3, 3, 3, 3, 2], side=5, back=6, over_side=5, over_back=6,
         eyes="brown", outfit="jacket", jacket=0x2A3A2E, under=0xEDE6D8, sleeve=0x2A3A2E, pants=0x2E3E62, pants_style="jeans", shoes=0x4A3428),
    dict(name="girl_10", girl=G, skin="medium", hair="navy", fringe=[3, 3, 3, 3, 3, 3, 3, 3], side=8, back=8, over_side=8, over_back=8, locks=7,
         eyes="blue", outfit="trench", jacket=0x6E5E4A, under=0x22222A, sleeve=0x6E5E4A, pants=0x22222A, pants_style="slacks", shoes=0x1A1414,
         tails=7, down=8),
    dict(name="girl_11", girl=G, skin="pale", hair="red", fringe=[2, 2, 2, 2, 2, 2, 2, 2], side=6, back=8, over_side=6, over_back=8,
         eyes="grey", outfit="dress", top=0x1A1A20, pants=0x1A1A20, skirt=6, skirt_colour=0x1A1A20, legs=0x1A1A20, shoes=0x101012, down=8,
         hair_tails="pony"),
    dict(name="girl_12", girl=G, skin="light", hair="teal", fringe=[3, 3, 2, 3, 3, 2, 3, 3], side=3, back=4, over_side=4, over_back=5, jag=1,
         eyes="grey", outfit="tank", top=0x2A2A2E, pants=0x4A5A3A, pants_style="cargo", shoes=0x2A2A26, gloves=0x2A2A2E),
]


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(root, "src/main/resources/assets/hexkagune/textures/entity/ghoul")
    os.makedirs(out, exist_ok=True)
    for i, spec in enumerate(GHOULS):
        image = Ghoul(spec, 7919 * (i + 1)).paint()
        image.save(os.path.join(out, spec["name"] + ".png"))
    print("wrote", len(GHOULS), "ghoul skins to", out)


if __name__ == "__main__":
    main()
