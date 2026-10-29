"""Swatch textures for the kakuja models: one 16x16 patch per material, plus a glow mask that keeps
only the emissive materials (the crimson cores and veins) and is transparent everywhere else."""
import random

from PIL import Image


def hex_rgb(value):
    return tuple(int(value[i:i + 2], 16) for i in (1, 3, 5))


def clamp(v):
    return max(0, min(255, int(round(v))))


def shade(rgb, k):
    return tuple(clamp(c * k) for c in rgb)


def noise_patch(rng, base, spread=0.08):
    """Flat material with per-pixel brightness noise, like a Minecraft texture."""
    rgb = hex_rgb(base)
    return [[shade(rgb, 1.0 + rng.uniform(-spread, spread)) for _ in range(16)] for _ in range(16)]


def plate_patch(rng, base, rim, spread=0.07):
    """Armour plate: noise with a lighter one-pixel rim and a darker inner bevel."""
    px = noise_patch(rng, base, spread)
    rim_rgb = hex_rgb(rim)
    for i in range(16):
        for j in range(16):
            edge = min(i, j, 15 - i, 15 - j)
            if edge == 0:
                px[i][j] = shade(rim_rgb, 1.0 + rng.uniform(-0.05, 0.05))
            elif edge == 1:
                px[i][j] = shade(px[i][j], 0.82)
    return px


def vein_patch(rng, base, vein, count=3):
    """Dark material split by jagged glowing veins. Returns (pixels, vein mask)."""
    px = noise_patch(rng, base, 0.1)
    mask = [[False] * 16 for _ in range(16)]
    vein_rgb = hex_rgb(vein)
    for _ in range(count):
        x = rng.randint(0, 15)
        for y in range(16):
            x = max(0, min(15, x + rng.choice((-1, 0, 0, 1))))
            px[y][x] = shade(vein_rgb, 1.0 + rng.uniform(-0.1, 0.1))
            mask[y][x] = True
            if rng.random() < 0.25 and x + 1 < 16:
                px[y][x + 1] = shade(vein_rgb, 0.7)
    return px, mask


def stripe_patch(rng, base, stripe, vertical=True):
    """Fibrous material: darker strands running along one axis."""
    px = noise_patch(rng, base, 0.06)
    stripe_rgb = hex_rgb(stripe)
    for k in range(16):
        if rng.random() < 0.35:
            for t in range(16):
                y, x = (t, k) if vertical else (k, t)
                px[y][x] = shade(stripe_rgb, 1.0 + rng.uniform(-0.08, 0.08))
    return px


def ring_patch(rng, base, ring, centre):
    px = noise_patch(rng, centre, 0.05)
    ring_rgb = hex_rgb(ring)
    for i in range(16):
        for j in range(16):
            d = ((i - 7.5) ** 2 + (j - 7.5) ** 2) ** 0.5
            if 4.5 <= d <= 7.5:
                px[i][j] = shade(ring_rgb, 1.0 + rng.uniform(-0.05, 0.05))
            elif d > 7.5:
                px[i][j] = shade(hex_rgb(base), 1.0 + rng.uniform(-0.05, 0.05))
    return px


def write(swatches, materials, path, glow_path, seed):
    """materials: name -> (kind, args, glows). glows: False, True (whole patch) or 'veins'."""
    rng = random.Random(seed)
    image = Image.new("RGBA", (swatches.width, swatches.height), (0, 0, 0, 0))
    glow = Image.new("RGBA", (swatches.width, swatches.height), (0, 0, 0, 0))
    for name in swatches.names:
        kind, args, glows = materials[name]
        mask = None
        if kind == "noise":
            px = noise_patch(rng, *args)
        elif kind == "plate":
            px = plate_patch(rng, *args)
        elif kind == "veins":
            px, mask = vein_patch(rng, *args)
        elif kind == "stripes":
            px = stripe_patch(rng, *args)
        elif kind == "ring":
            px = ring_patch(rng, *args)
        else:
            raise ValueError(kind)
        u0, v0 = swatches.origin(name)
        for y in range(16):
            for x in range(16):
                image.putpixel((u0 + x, v0 + y), px[y][x] + (255,))
                if glows is True or (glows == "veins" and mask is not None and mask[y][x]):
                    glow.putpixel((u0 + x, v0 + y), px[y][x] + (255,))
    image.save(path)
    glow.save(glow_path)
