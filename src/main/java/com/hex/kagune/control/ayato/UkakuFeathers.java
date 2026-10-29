package com.hex.kagune.control.ayato;

import static com.hex.kagune.control.ayato.UkakuModel.*;

/**
 * What is left of the first 3D pass of the twin wings (feathered fans): the palette the other parts share and the crystal
 * chips the One-Winged Kagune sheds. 5.0.13: the fan itself went with the Wing Block's shell, its last user.
 */
final class UkakuFeathers {
    // Crystallized palette (anime :re shield frames and the √A wings).
    static final int CRYSTAL_ROOT = 0xE8142C;
    static final int CRYSTAL_BODY = 0x3B34B8;
    static final int RIM = 0xFF1F3A;
    static final int RIM_TIP = 0xFF6AA8;
    static final int GLOW = 0xF59BD6;
    // RC gas puffs and the shard flash.
    static final int PUFF_CORE = 0xFF8A9A;
    static final int PUFF_EDGE = 0xE8243E;
    static final int FLASH = 0xFFF6FA;

    /** One feather of the fan: angle from outward toward up (degrees), length, half-width, root, depth layer, curl. */
    record Feather(double angle, double length, double width, double rootU, double rootV, double depth, double bend, boolean covert,
                   int index) {
    }

    private UkakuFeathers() {
    }

    /** Crystal chips shed by a dissolving feather, drifting off and fading. */
    static void chips(Sink sink, Frame frame, Feather f, Params p) {
        double progress = 1.0 - clamp01(p.deploy());
        double fade = (1.0 - progress) * clamp01(p.alpha());
        if (fade <= 0.01) {
            return;
        }
        double a = Math.toRadians(f.angle());
        for (int chip = 0; chip < 3; chip++) {
            double t = 0.45 + 0.24 * chip;
            double reach = f.length() * t;
            double drift = progress * (0.25 + 0.15 * chip);
            double cx = f.rootU() + Math.cos(a) * reach - Math.sin(a) * drift * 0.6;
            double cy = f.rootV() + Math.sin(a) * reach + Math.cos(a) * drift * 0.6 + drift * 0.3;
            double cz = f.depth() + drift * 0.8;
            double r = 0.035 * (1.0 - 0.5 * progress);
            int color = argb(mixInt(CRYSTAL_ROOT, CRYSTAL_BODY, 0.4 + 0.2 * chip), 0.9 * fade);
            int edge = argb(RIM, 0.9 * fade);
            double spin = progress * 6.0 + chip;
            double ux = Math.cos(spin) * r;
            double uy = Math.sin(spin) * r;
            glowQuad(sink, frame, new double[] {cx - ux, cy - uy, cz}, new double[] {cx - uy * 0.5, cy + ux * 0.5, cz},
                new double[] {cx + ux, cy + uy, cz}, new double[] {cx + uy * 0.5, cy - ux * 0.5, cz}, edge, color, edge, color);
            glowQuad(sink, frame, new double[] {cx - ux, cy - uy, cz}, new double[] {cx, cy, cz + r * 0.8},
                new double[] {cx + ux, cy + uy, cz}, new double[] {cx, cy, cz - r * 0.8}, edge, color, edge, color);
        }
    }
}
