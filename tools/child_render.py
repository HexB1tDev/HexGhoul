#!/usr/bin/env python3
"""1.3.19: the children as the game draws them, off-game: every son's and daughter's skin at every stage (baby, toddler, child, teen).

    python3 tools/child_render.py <out-dir> [--kakugan]

Reads the skins tools/child_skins.py made (textures/entity/child/skins) and family/ChildSkins.java (which are girls', which slim) and
draws each on vanilla's player model the way ChildRenderer poses it: a baby and a toddler on a little one's proportions (a full-size
head on a body, arms and legs half as big), older ones on a player's; each stage at its own size (ChildStage.draw); a baby sitting
(legs out in front, arms forward, dropped so it sits on the ground); wide or slim arms as its skin is painted; both layers of the skin
(the outer one a little bigger, as vanilla inflates it). A three-quarter view, orthographic, lit by face as Minecraft lights entities.
With --kakugan the eyes are drawn turned (both eyes' overlays laid on). Writes sons.png and daughters.png.
"""
import math
import os
import re
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
SKINS = ROOT / "src" / "main" / "resources" / "assets" / "hexkagune" / "textures" / "entity" / "child" / "skins"
JAVA = ROOT / "src" / "main" / "java" / "com" / "hex" / "kagune" / "control" / "family" / "ChildSkins.java"

# ChildStage: name, draw size, young (a little one's proportions), sits.
STAGES = (("baby", 0.6, True, True), ("toddler", 0.8, True, False), ("child", 0.7, False, False), ("teen", 0.88, False, False))

SS = 2           # supersampling
UNIT = 7.4       # screen pixels per model pixel at draw size 1 (before supersampling)
YAW = math.radians(28.0)
PITCH = math.radians(-12.0)
LIGHT = np.array([0.35, -0.85, -0.6])
LIGHT = LIGHT / np.linalg.norm(LIGHT)


def skins():
    out = []
    for m in re.finditer(r'new Skin\("(\w+)", (true|false), (true|false), 0x[0-9A-F]{6}\), // (.+?), (\d+)x(\d+)', JAVA.read_text()):
        out.append({"key": m.group(1), "girl": m.group(2) == "true", "slim": m.group(3) == "true", "source": m.group(4)})
    return out


# ------------------------------------------------------------------ the model (vanilla PlayerModel, y down, the feet at 24)

def rot(x, y, z):
    """Quaternionf.rotationZYX(z, y, x) as a matrix: Rz Ry Rx."""
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return rz @ ry @ rx


class Part:
    def __init__(self, pivot, rotation=(0.0, 0.0, 0.0), scale=1.0):
        self.pivot = np.array(pivot, dtype=float)
        self.r = rot(*rotation)
        self.scale = scale

    def to_model(self, p):
        return self.pivot + self.r @ (np.array(p, dtype=float) * self.scale)


def boxes(slim, young, sits):
    """Every cuboid: (part, min corner, size, texture origin, inflate) for both layers."""
    arm_w = 3 if slim else 4
    if young:
        head = Part((0, 12, 0), scale=1.0)
        body = Part((0, 12, 0), scale=0.5)
        ra = (-2.5, 13, 0)
        la = (2.5, 13, 0)
        rl = (-0.95, 18, 0.05)
        ll = (0.95, 18, 0.05)
        limb = 0.5
    else:
        head = Part((0, 0, 0))
        body = Part((0, 0, 0))
        ra = (-5, 2, 0)
        la = (5, 2, 0)
        rl = (-1.9, 12, 0.1)
        ll = (1.9, 12, 0.1)
        limb = 1.0
    arm_out = 0.07
    arm_x = -0.5 if sits else 0.0
    right_arm = Part(ra, (arm_x, 0.0, arm_out), limb)
    left_arm = Part(la, (arm_x, 0.0, -arm_out), limb)
    if sits:
        right_leg = Part(rl, (-1.4137167, 0.31415927, 0.07853982), limb)
        left_leg = Part(ll, (-1.4137167, -0.31415927, -0.07853982), limb)
    else:
        right_leg = Part(rl, scale=limb)
        left_leg = Part(ll, scale=limb)
    rx = -3 if not slim else -2
    out = [
        (head, (-4, -8, -4), (8, 8, 8), (0, 0), 0.0), (head, (-4, -8, -4), (8, 8, 8), (32, 0), 0.5),
        (body, (-4, 0, -2), (8, 12, 4), (16, 16), 0.0), (body, (-4, 0, -2), (8, 12, 4), (16, 32), 0.25),
        (right_arm, (rx, -2, -2), (arm_w, 12, 4), (40, 16), 0.0), (right_arm, (rx, -2, -2), (arm_w, 12, 4), (40, 32), 0.25),
        (left_arm, (-1, -2, -2), (arm_w, 12, 4), (32, 48), 0.0), (left_arm, (-1, -2, -2), (arm_w, 12, 4), (48, 48), 0.25),
        (right_leg, (-2, 0, -2), (4, 12, 4), (0, 16), 0.0), (right_leg, (-2, 0, -2), (4, 12, 4), (0, 32), 0.25),
        (left_leg, (-2, 0, -2), (4, 12, 4), (16, 48), 0.0), (left_leg, (-2, 0, -2), (4, 12, 4), (0, 48), 0.25),
    ]
    return out


def faces(part, lo, size, uv, inflate):
    """The six faces: (origin, along u, along v, texture rect (u, v, w, h) in 64-units, outward normal), in model space."""
    x0, y0, z0 = (lo[i] - inflate for i in range(3))
    w, h, d = size
    x1, y1, z1 = x0 + w + 2 * inflate, y0 + h + 2 * inflate, z0 + d + 2 * inflate
    u, v = uv
    P = lambda x, y, z: part.to_model((x, y, z))
    out = [
        # front (z min): u along +x, v down
        (P(x0, y0, z0), P(x1, y0, z0), P(x0, y1, z0), (u + d, v + d, w, h)),
        # back (z max): u from x max to x min
        (P(x1, y0, z1), P(x0, y0, z1), P(x1, y1, z1), (u + 2 * d + w, v + d, w, h)),
        # right side (x min): u from z max (back) to z min (front)
        (P(x0, y0, z1), P(x0, y0, z0), P(x0, y1, z1), (u, v + d, d, h)),
        # left side (x max): u from z min to z max
        (P(x1, y0, z0), P(x1, y0, z1), P(x1, y1, z0), (u + d + w, v + d, d, h)),
        # top (y min): u along +x, v from z max (back) to z min (front)
        (P(x0, y0, z1), P(x1, y0, z1), P(x0, y0, z0), (u + d, v, w, d)),
        # bottom (y max): u along +x, v from z min to z max
        (P(x0, y1, z0), P(x1, y1, z0), P(x0, y1, z1), (u + d + w, v, w, d)),
    ]
    centre = part.to_model((x0 + (x1 - x0) / 2, y0 + (y1 - y0) / 2, z0 + (z1 - z0) / 2))
    return out, centre


# ------------------------------------------------------------------ drawing

VIEW = None


def view_matrix():
    cy, sy = math.cos(YAW), math.sin(YAW)
    cp, sp = math.cos(PITCH), math.sin(PITCH)
    ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    rx = np.array([[1, 0, 0], [0, cp, -sp], [0, sp, cp]])
    return rx @ ry


def render(tex, slim, young, sits, draw, width, height, ground):
    """One child, as an RGBA image width x height (supersampled), its feet on {@code ground} (pixels from the top)."""
    V = view_matrix()
    k = UNIT * draw * SS
    img = np.zeros((height, width, 4), dtype=np.float32)
    zbuf = np.full((height, width), np.inf, dtype=np.float32)
    tw = tex.shape[1]
    s = tw / 64.0
    drop = (0.75 * draw * (0.5 if young else 1.0)) * 16.0 / draw if sits else 0.0  # model pixels the baby is set down by
    cx = width / 2.0

    def screen(p):
        q = V @ np.array([p[0], p[1] - 24.0 + drop, p[2]])
        return np.array([cx + q[0] * k, ground + q[1] * k, q[2] * k])

    for layer in (0, 1):
        for part, lo, size, uv, inflate in boxes(slim, young, sits):
            if (inflate > 0) != (layer == 1):
                continue
            fs, centre = faces(part, lo, size, uv, inflate)
            for p0, pu, pv, rect in fs:
                a, bu, bv = screen(p0), screen(pu), screen(pv)
                eu, ev = bu - a, bv - a
                # (u across, v down in a y-down space: their cross is the face's inward normal.)
                n_out = -np.cross(pu - p0, pv - p0)
                n_out = n_out / (np.linalg.norm(n_out) + 1e-12)
                # Faces turned away from the camera (it looks down +z) are not drawn.
                if (V @ n_out)[2] > -1e-6:
                    continue
                if abs(eu[0] * ev[1] - eu[1] * ev[0]) < 1e-9:
                    continue
                light = 0.5 + 0.5 * max(0.0, float(np.dot(n_out, LIGHT)))
                xs = [a[0], bu[0], bv[0], bu[0] + ev[0]]
                ys = [a[1], bu[1], bv[1], bu[1] + ev[1]]
                x_lo, x_hi = max(0, int(math.floor(min(xs)))), min(width - 1, int(math.ceil(max(xs))))
                y_lo, y_hi = max(0, int(math.floor(min(ys)))), min(height - 1, int(math.ceil(max(ys))))
                if x_lo > x_hi or y_lo > y_hi:
                    continue
                gx, gy = np.meshgrid(np.arange(x_lo, x_hi + 1) + 0.5, np.arange(y_lo, y_hi + 1) + 0.5)
                det = eu[0] * ev[1] - eu[1] * ev[0]
                dx, dy = gx - a[0], gy - a[1]
                fa = (dx * ev[1] - dy * ev[0]) / det
                fb = (eu[0] * dy - eu[1] * dx) / det
                inside = (fa >= 0) & (fa < 1) & (fb >= 0) & (fb < 1)
                if not inside.any():
                    continue
                depth = a[2] + fa * eu[2] + fb * ev[2]
                tu = ((rect[0] + np.clip(fa, 0, 0.9999) * rect[2]) * s).astype(int)
                tv = ((rect[1] + np.clip(fb, 0, 0.9999) * rect[3]) * s).astype(int)
                tu = np.clip(tu, 0, tw - 1)
                tv = np.clip(tv, 0, tex.shape[0] - 1)
                texel = tex[tv, tu]
                alpha = texel[..., 3] / 255.0
                sub_z = zbuf[y_lo:y_hi + 1, x_lo:x_hi + 1]
                draw_mask = inside & (alpha > 0.02) & (depth < sub_z)
                if not draw_mask.any():
                    continue
                rgb = texel[..., :3] * light
                sub = img[y_lo:y_hi + 1, x_lo:x_hi + 1]
                if layer == 0:
                    sub[draw_mask, :3] = rgb[draw_mask]
                    sub[draw_mask, 3] = 255.0
                    sub_z[draw_mask] = depth[draw_mask]
                else:
                    al = alpha[draw_mask][:, None]
                    sub[draw_mask, :3] = rgb[draw_mask] * al + sub[draw_mask, :3] * (1 - al)
                    sub[draw_mask, 3] = np.maximum(sub[draw_mask, 3], alpha[draw_mask] * 255.0)
                    opaque = draw_mask & (alpha > 0.5)
                    sub_z[opaque] = depth[opaque]
    return img


def figure(path, slim, stage, kakugan, cell_w, cell_h, ground):
    tex = Image.open(path).convert("RGBA")
    if kakugan:
        for side in ("r", "l"):
            over = path.with_name(path.stem + "_k" + side + ".png")
            if over.exists():
                tex.alpha_composite(Image.open(over).convert("RGBA"))
    arr = np.asarray(tex).astype(np.float32)
    name, draw, young, sits = stage
    img = render(arr, slim, young, sits, draw, cell_w * SS, cell_h * SS, ground * SS)
    out = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA")
    return out.resize((cell_w, cell_h), Image.LANCZOS)


def sheet(kids, out_path, title, kakugan):
    cell_w, cell_h, ground = 108, 246, 226
    per_row = 4  # skins per row, each four stages wide
    block_w = cell_w * 4 + 16
    rows = (len(kids) + per_row - 1) // per_row
    head = 54
    W = block_w * per_row + 16
    H = head + rows * (cell_h + 26) + 10
    canvas = Image.new("RGBA", (W, H), (34, 36, 42, 255))
    d = ImageDraw.Draw(canvas)
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 13)
        big = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 22)
    except OSError:
        font = big = ImageFont.load_default()
    d.text((16, 12), title, fill=(240, 240, 245, 255), font=big)
    labels = " · ".join(s[0] for s in STAGES)
    d.text((W - 16 - d.textlength("each: " + labels, font=font), 20), "each: " + labels, fill=(170, 175, 190, 255), font=font)
    for i, kid in enumerate(kids):
        bx = 8 + (i % per_row) * block_w
        by = head + (i // per_row) * (cell_h + 26)
        d.rounded_rectangle((bx + 2, by, bx + block_w - 6, by + cell_h + 20), radius=10, fill=(44, 47, 55, 255))
        # the ground line and soft shadows
        for j, stage in enumerate(STAGES):
            x = bx + 8 + j * cell_w
            sw = int(cell_w * 0.42 * stage[1])
            d.ellipse((x + cell_w // 2 - sw // 2, by + ground - 4, x + cell_w // 2 + sw // 2, by + ground + 4), fill=(28, 30, 35, 255))
        for j, stage in enumerate(STAGES):
            x = bx + 8 + j * cell_w
            fig = figure(SKINS / (kid["key"] + ".png"), kid["slim"], stage, kakugan, cell_w, cell_h, ground)
            canvas.alpha_composite(fig, (x, by))
        label = "%s  %s%s" % (kid["key"], kid["source"].replace(".png", ""), "  (slim)" if kid["slim"] else "")
        d.text((bx + 12, by + cell_h + 2), label, fill=(225, 228, 235, 255), font=font)
    canvas.save(out_path)


def main():
    out = Path(sys.argv[1])
    kakugan = "--kakugan" in sys.argv
    out.mkdir(parents=True, exist_ok=True)
    all_kids = skins()
    suffix = "-kakugan" if kakugan else ""
    sons = [k for k in all_kids if not k["girl"]]
    daughters = [k for k in all_kids if k["girl"]]
    sheet(sons, out / ("sons%s.png" % suffix), "Sons (%d skins) at every stage" % len(sons), kakugan)
    sheet(daughters, out / ("daughters%s.png" % suffix), "Daughters (%d skins) at every stage" % len(daughters), kakugan)


if __name__ == "__main__":
    main()
