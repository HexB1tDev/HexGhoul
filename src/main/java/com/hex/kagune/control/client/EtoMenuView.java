package com.hex.kagune.control.client;

import com.hex.kagune.control.client.ui.UiCanvas;
import com.hex.kagune.control.eto.EtoMutation;
import com.hex.kagune.control.eto.EtoRules;
import com.hex.kagune.control.eto.EtoStrike;
import com.hex.kagune.control.eto.EtoUnlock;
import com.hex.kagune.control.kakugan.KakuganEyes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Eto path's kagune menu (the menu key; 6.1.0), drawn on any {@link UiCanvas}: the same 960 x 520 board and organic style
 * as the Kaneki and Ayato menus, in her colours (pale flesh and bandage white over deep ink, the one red eye). It shows her
 * organ and stage, her mastery and the track of what it brings, a live preview of the pale flesh, her combat readout, her one
 * kakugan (and the button to place it), her mutations, and her kakuja, the Owl (6.2.0: her only one; the sealed V2 is gone).
 */
public final class EtoMenuView {
    public static final int UI_WIDTH = 960;
    public static final int UI_HEIGHT = 520;

    // Eto's palette: bandage and bone over a deep ink-plum, flesh pinks, and the kakugan's red for what matters.
    static final int PANEL_FILL = 0xF3100C12;
    static final int PANEL_EDGE = 0xFF5A4A4C;
    static final int INNER_FILL = 0xF20A080C;
    static final int INNER_EDGE = 0xFF2C2228;
    static final int VEIN = 0xFF6A4E4E;
    static final int VEIN_BRIGHT = 0xFF463536;
    static final int TITLE = 0xFFF6EEE6;
    static final int SUBTITLE = 0xFFB89A8E;
    static final int HEADER = 0xFFEAC2B0;
    static final int LABEL = 0xFF9A8580;
    static final int VALUE = 0xFFF2E8E0;
    static final int TRAIT = 0xFFE2D4CA;
    static final int MUTED = 0xFF9A8A86;
    static final int FAINT = 0xFF6A5C5C;
    static final int BAR_BACK = 0xFF2E2426;
    static final int BAR = 0xFFE4C8B8;
    static final int EYE = 0xFFE8283C;
    static final int PREVIEW_FILL = 0xC6141012;
    static final int PREVIEW_EDGE = 0xFF4E4040;
    static final int PREVIEW_INNER = 0xE20A080A;
    static final int PREVIEW_INNER_EDGE = 0xFF2A2224;
    static final int SEALED = 0xFF8A7A96;

    /**
     * What the menu shows (6.2.0: her mutations, a bitmask of {@link EtoMutation}, and how many ticks until her death that does
     * not take is ready again, 0 when it is).
     */
    public record Model(int mastery, boolean active, KakuganEyes eyes, int mutations, long reviveIn) {
        public Model(int mastery, boolean active, KakuganEyes eyes) {
            this(mastery, active, eyes, 0, 0L);
        }
    }

    /** A clickable area on the board (virtual 960 x 520 coordinates). */
    public record Rect(int x, int y, int width, int height) {
        public boolean contains(double px, double py) {
            return px >= this.x && px <= this.x + this.width && py >= this.y && py <= this.y + this.height;
        }
    }

    private EtoMenuView() {
    }

    /**
     * Draws the board at virtual size (the caller scales the canvas) and returns its clickable areas by name ("eyes",
     * "kakuja:V1", "mutation:N"). {@code mouseX}/{@code mouseY} are in board coordinates, for hovering and the tooltip.
     */
    public static Map<String, Rect> draw(UiCanvas c, Model model, double mouseX, double mouseY, double time) {
        Map<String, Rect> hits = new LinkedHashMap<>();
        int panelWidth = 930;
        int panelHeight = 500;
        int centerX = UI_WIDTH / 2;
        int centerY = UI_HEIGHT / 2;
        int left = centerX - panelWidth / 2;
        int top = centerY - panelHeight / 2;
        c.batch(() -> {
            AyatoMenuView.organicPanel(c, centerX, centerY, panelWidth / 2, panelHeight / 2, PANEL_FILL, PANEL_EDGE);
            AyatoMenuView.organicPanel(c, centerX, centerY, panelWidth / 2 - 5, panelHeight / 2 - 5, INNER_FILL, INNER_EDGE);
            AyatoMenuView.drawVein(c, left + 262, top + 44, left + 280, centerY, left + 250, top + panelHeight - 45, VEIN, 3);
            AyatoMenuView.drawVein(c, left + 535, top + 42, left + 520, centerY, left + 550, top + panelHeight - 42, VEIN, 3);
            AyatoMenuView.drawVein(c, left + 18, top + 74, centerX, top + 32, left + panelWidth - 18, top + 74, VEIN_BRIGHT, 2);
        });
        c.centered("O N E - E Y E D   O W L", centerX, top + 18, TITLE);
        c.centered("RC CELL / PALE FLESH MATRIX", centerX, top + 34, SUBTITLE);
        c.centered("Ctrl+R form  •  M1 blitz  •  RMB flips  •  Z punch  •  J flesh  •  V kicks  •  B watch this  •  N arm  •  K owl", centerX, top + 51,
            0xFFA48C84);

        identity(c, model, left + 24, top + 68);
        preview(c, model, left + 295, top + 76, 220, 190, time);
        stats(c, model, hits, left + 295, top + 285);
        mutations(c, model, hits, left + 566, top + 66);
        kakuja(c, model, hits, left + 566, top + 300, time);

        c.centered(model.active() ? "R  RETRACT" : "ALT + R  BASE FORM", centerX, top + panelHeight - 16, model.active() ? EYE : 0xFFB49486);

        String tip = tooltip(hits, mouseX, mouseY, model);
        if (tip != null) {
            int width = c.width(tip) + 8;
            int x = (int)Math.min(UI_WIDTH - width - 4, mouseX + 10);
            int y = (int)Math.max(4, mouseY - 16);
            c.fill(x - 1, y - 1, x + width + 1, y + 13, PANEL_EDGE);
            c.fill(x, y, x + width, y + 12, 0xF0120E12);
            c.text(tip, x + 4, y + 2, VALUE, false);
        }
        return hits;
    }

    // ------------------------------------------------------------------ sections

    private static void identity(UiCanvas c, Model model, int x, int y) {
        int mastery = model.mastery();
        c.text("CURRENT ORGAN", x, y, HEADER, false);
        row(c, "Ghoul", "Natural one-eyed", x, y + 25);
        row(c, "Kagune", "Pale flesh, upper back", x, y + 43);
        row(c, "Stage", EtoUnlock.stageName(mastery), x, y + 61);
        row(c, "Kakugan", eyeText(model.eyes()) + " only", x, y + 79);
        row(c, "Form", model.active() ? "Base form (out)" : "Base form (Ctrl+R)", x, y + 97);
        row(c, "Blitz", EtoStrike.count(mastery) + " of " + EtoStrike.values().length + " strikes", x, y + 115);

        c.text("MASTERY", x, y + 151, HEADER, false);
        c.text(mastery + " / 1000", x + 126, y + 151, VALUE, false);
        c.batch(() -> {
            AyatoMenuView.capsule(c, x, y + 171, 205, 8, BAR_BACK);
            AyatoMenuView.capsule(c, x, y + 171, Math.max(8, Math.round(205.0F * Math.min(1000, mastery) / 1000.0F)), 8, BAR);
        });
        c.text(nextMilestone(mastery), x, y + 185, MUTED, false);

        c.text("UNLOCKS", x, y + 202, HEADER, false);
        int rowY = y + 223;
        for (EtoUnlock unlock : track(mastery)) {
            boolean reached = unlock.reached(mastery);
            int colour = reached ? TRAIT : MUTED;
            c.text(Integer.toString(unlock.mastery()), x + 5, rowY, MUTED, false);
            c.text(c.fit(unlock.text(), 200), x + 34, rowY, colour, false);
            rowY += 16;
        }
    }

    /** The live preview: the pale flesh rearing out of her back, breathing, steam off her shoulders. */
    private static void preview(UiCanvas c, Model model, int x, int y, int width, int height, double time) {
        c.batch(() -> {
            AyatoMenuView.organicPanel(c, x + width / 2, y + height / 2, width / 2, height / 2, PREVIEW_FILL, PREVIEW_EDGE);
            AyatoMenuView.organicPanel(c, x + width / 2, y + height / 2, width / 2 - 5, height / 2 - 5, PREVIEW_INNER, PREVIEW_INNER_EDGE);
        });
        c.centered("LIVE RC RESPONSE", x + width / 2, y + 10, HEADER);
        PathChoiceLayout.Box box = new PathChoiceLayout.Box(x + 12, y + 22, width - 24, 124);
        List<PathSilhouette.Mark> marks = PathSilhouette.eto(box, time, 1.0F, model.active(), 0x0B090B);
        c.batch(() -> {
            for (PathSilhouette.Mark mark : marks) {
                AyatoMenuView.draw(c, mark);
            }
        });
        c.centered(model.active() ? "base form / the flesh stirs under her skin" : "sheathed / Ctrl+R to wake it", x + width / 2, y + height - 40,
            0xFF8E7670);
        c.centered("kakuja: the Owl (K)", x + width / 2, y + height - 26, SEALED);
    }

    private static void stats(UiCanvas c, Model model, Map<String, Rect> hits, int x, int y) {
        int mastery = model.mastery();
        c.text("COMBAT READOUT", x, y, HEADER, false);
        c.text(String.format("Fist            %.1f a strike", EtoRules.fist(mastery)), x, y + 23, TRAIT, false);
        c.text(String.format("Big Punch      %.0f to %.0f, %.0f blocks", EtoRules.punchDamage(mastery, 0.0F), EtoRules.punchDamage(mastery, 1.0F),
            EtoRules.punchRange(1.0F)), x, y + 40, TRAIT, false);
        c.text(mastery >= EtoRules.SWIPE_MASTERY ? String.format("Flesh reach    %.0f blocks", EtoRules.SWIPE_REACH)
            : "Flesh reach    at mastery " + EtoRules.SWIPE_MASTERY, x, y + 57, TRAIT, false);
        c.text(mastery >= EtoRules.HAMMER_MASTERY ? String.format("Flesh Hammer  %.1f to %.1f blocks wide", EtoRules.hammerRadius(0.0F),
            EtoRules.hammerRadius(1.0F)) : "Flesh Hammer  at mastery " + EtoRules.HAMMER_MASTERY, x, y + 74, TRAIT, false);
        c.text("Smashes weak blocks; they grow back in 30 s", x, y + 91, MUTED, false);

        c.text("KAKUGAN", x, y + 132, HEADER, false);
        c.text("One eye only: the " + eyeText(model.eyes()).toLowerCase(java.util.Locale.ROOT), x, y + 146, MUTED, false);
        Rect button = new Rect(x, y + 160, 214, 18);
        c.fill(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), 0xFF8A3A3A);
        c.fill(button.x() + 1, button.y() + 1, button.x() + button.width() - 1, button.y() + button.height() - 1, 0xFF3A1E20);
        c.centered("ADJUST KAKUGAN", button.x() + button.width() / 2, button.y() + 5, 0xFFF2DED8);
        hits.put("eyes", button);
    }

    /**
     * Her kakuja, the Owl, as one wide slot (6.2.0: her only one; the sealed V2 is gone): its eye open once her mastery reaches it,
     * what it is, the mastery it needs and its key.
     */
    private static void kakuja(UiCanvas c, Model model, Map<String, Rect> hits, int x, int y, double time) {
        c.text("KAKUJA", x, y, HEADER, false);
        int slotWidth = 335;
        int slotHeight = 96;
        int sx = x;
        int sy = y + 16;
        Rect slot = new Rect(sx, sy, slotWidth, slotHeight);
        float pulse = (float)(0.5 + 0.5 * Math.sin(time * 1.6));
        int edge = PathSilhouette.blend(0x3A2E40, 0x7A6488, 0.35F + 0.35F * pulse) | 0xFF000000;
        c.fill(sx, sy, sx + slotWidth, sy + slotHeight, edge);
        c.fill(sx + 2, sy + 2, sx + slotWidth - 2, sy + slotHeight - 2, 0xFF120E16);
        // The Owl's eye: the lids closed over a thread of red until it is hers, then apart over a red kakugan.
        int ex = sx + 62;
        int ey = sy + 46;
        boolean open = model.mastery() >= EtoRules.KAKUJA_V1_MASTERY;
        c.batch(() -> {
            for (int k = -26; k <= 26; k++) {
                int lid = (int)Math.round(Math.sqrt(Math.max(0.0, 1.0 - (k / 26.0) * (k / 26.0))) * (open ? 14.0 : 6.0));
                c.fill(ex + k, ey - lid, ex + k + 1, ey - lid + 2, 0xFF5E5068);
                c.fill(ex + k, ey + lid - 1, ex + k + 1, ey + lid + 1, 0xFF3E3448);
                if (open && lid > 2) {
                    c.fill(ex + k, ey - lid + 2, ex + k + 1, ey + lid - 1, Math.abs(k) < 7 ? 0xFF1A0508 : 0xFFB0182A);
                }
            }
            int glow = (int)(80 + 120 * pulse);
            c.fill(ex - 16, ey, ex + 16, ey + 1, glow << 24 | 0xE8283C);
        });
        int tx = sx + 124;
        c.text("THE OWL", tx, sy + 12, open ? TRAIT : SEALED, false);
        c.text(open ? "K: grow it  /  K again: turn back" : "sealed until mastery " + EtoRules.KAKUJA_V1_MASTERY, tx, sy + 28, open ? 0xFFD8C8E2 : FAINT,
            false);
        c.text("Giant blades on red muscle", tx, sy + 46, MUTED, false);
        c.text("An owl's mask, one burning eye", tx, sy + 60, MUTED, false);
        c.text("mastery " + EtoRules.KAKUJA_V1_MASTERY, tx, sy + 76, open ? TRAIT : FAINT, false);
        hits.put("kakuja:V1", slot);
    }

    /**
     * 6.2.0: her mutations, as the other paths have theirs: each mastery milestone earns a point, spent here by clicking the mutation
     * she wants ("mutation:N", N its ordinal); one she has shows whether it is ready. They have the top of the right column (the
     * keys are on the line under the title).
     */
    private static void mutations(UiCanvas c, Model model, Map<String, Rect> hits, int x, int y) {
        int points = EtoMutation.available(model.mastery(), model.mutations());
        int next = EtoMutation.nextMilestone(model.mastery());
        c.text("MUTATIONS", x, y, HEADER, false);
        String status = points > 0 ? points + (points == 1 ? " point to spend: click it" : " points to spend: click one")
            : next > 0 ? "a point at mastery " + next : "every point spent";
        c.text(c.fit(status, 240), x + 86, y, points > 0 ? 0xFFF0B060 : MUTED, false);
        // 6.2.0: four of them now, a little shorter each, so they all fit above her kakuja.
        int width = 335;
        int height = 44;
        for (EtoMutation mutation : EtoMutation.values()) {
            int top = y + 16 + mutation.ordinal() * (height + 6);
            boolean owned = mutation.in(model.mutations());
            boolean ready = EtoMutation.canUnlock(model.mastery(), model.mutations(), mutation);
            int edge = owned ? 0xFFC8402A : ready ? 0xFFF0A040 : 0xFF3A2430;
            int fill = owned ? 0xFF3A1A16 : ready ? 0xFF2E2016 : 0xFF1E161C;
            c.fill(x, top, x + width, top + height, edge);
            c.fill(x + 2, top + 2, x + width - 2, top + height - 2, fill);
            boolean waits = mutation == EtoMutation.NOT_THAT_EASILY;
            String tag = owned ? (waits ? reviveTag(model.reviveIn()) : "ON") : ready ? "CLICK TO UNLOCK" : "NEEDS A POINT";
            int tagWidth = c.width(tag);
            c.text(c.fit(mutation.displayName(), width - 26 - tagWidth), x + 8, top + 6, owned ? 0xFFFFD6C8 : ready ? 0xFFF7E3C8 : 0xFF8F7A80, false);
            c.text(tag, x + width - 8 - tagWidth, top + 6, owned ? (waits && model.reviveIn() > 0 ? 0xFFB08A80 : 0xFFE86A4A) : ready ? 0xFFF0B060
                : 0xFF6E5A62, false);
            int line = 0;
            for (String row : AyatoMenuView.wrap(c, mutation.description(), width - 16)) {
                if (line < 2) {
                    c.text(row, x + 8, top + 18 + line * 11, owned ? 0xFFD8B8B0 : 0xFF9C868E, false);
                }
                line++;
            }
            hits.put("mutation:" + mutation.ordinal(), new Rect(x, top, width, height));
            if (owned && mutation == EtoMutation.WITNESS_THIS) {
                // 6.2.2: its switch, Instant Big Punch: Z tapped is the full charge at once (and so the combo).
                boolean on = (model.mutations() & EtoMutation.INSTANT_PUNCH) != 0;
                String label = "INSTANT BIG PUNCH: " + (on ? "ON" : "OFF");
                int bw = c.width(label) + 10;
                int bx = x + width - 8 - tagWidth - 6 - bw;
                c.fill(bx, top + 3, bx + bw, top + 16, on ? 0xFFE86A4A : 0xFF6E5A62);
                c.fill(bx + 1, top + 4, bx + bw - 1, top + 15, on ? 0xFF5A2018 : 0xFF241A20);
                c.text(label, bx + 5, top + 5, on ? 0xFFFFD6C8 : 0xFFB8A0A8, false);
                hits.put("instant", new Rect(bx, top + 3, bw, 13));
            }
        }
    }

    /** Ready, or how long until it is (m:ss). */
    static String reviveTag(long ticksLeft) {
        if (ticksLeft <= 0L) {
            return "READY";
        }
        long seconds = (ticksLeft + 19L) / 20L;
        return String.format(java.util.Locale.ROOT, "REGROWING %d:%02d", seconds / 60L, seconds % 60L);
    }

    // ------------------------------------------------------------------ text

    private static void row(UiCanvas c, String label, String value, int x, int y) {
        c.text(label, x, y, LABEL, false);
        c.text(c.fit(value, 140), x + 76, y, VALUE, false);
    }

    /** Which eye turns: her chosen one, or (never both on her path) the right. */
    static String eyeText(KakuganEyes eyes) {
        return eyes != null && eyes.ghoul() == KakuganEyes.LEFT ? "Left eye" : "Right eye";
    }

    static String nextMilestone(int mastery) {
        EtoUnlock next = EtoUnlock.next(mastery);
        if (next == null) {
            return "EVOLUTION COMPLETE";
        }
        return "NEXT UNLOCK  " + next.mastery() + "  (" + (next.mastery() - mastery) + " mastery)";
    }

    /** Rows of the unlock track shown: the last few steps reached, then the next few, {@link #TRACK_ROWS} in all. */
    static final int TRACK_ROWS = 7;

    static List<EtoUnlock> track(int mastery) {
        EtoUnlock[] all = EtoUnlock.values();
        EtoUnlock next = EtoUnlock.next(mastery);
        int reached = next == null ? all.length : next.ordinal();
        int start = Math.max(0, Math.min(all.length - TRACK_ROWS, reached - TRACK_ROWS / 2));
        List<EtoUnlock> rows = new ArrayList<>();
        for (int i = start; i < Math.min(all.length, start + TRACK_ROWS); i++) {
            rows.add(all[i]);
        }
        return rows;
    }

    private static String tooltip(Map<String, Rect> hits, double x, double y, Model model) {
        Rect instant = hits.get("instant");
        if (instant != null && instant.contains(x, y)) {
            return (model.mutations() & EtoMutation.INSTANT_PUNCH) != 0
                ? "Instant Big Punch: on. Z tapped is the full-charge punch at once, straight into the combo (click: off)"
                : "Instant Big Punch: off. Z held to charge as usual (click: on, a tap is the full punch and the combo)";
        }
        for (Map.Entry<String, Rect> entry : hits.entrySet()) {
            if (!entry.getValue().contains(x, y)) {
                continue;
            }
            String key = entry.getKey();
            if (key.equals("kakuja:V1")) {
                return model.mastery() >= EtoRules.KAKUJA_V1_MASTERY ? "The Owl (K): M1 claws, kagune, bite  J tap whirl, hold spikes  N grab  V lunge, hold slam  Z fist  RMB pounce"
                    : "The Owl: unlocks at mastery " + EtoRules.KAKUJA_V1_MASTERY;
            }
            if (key.startsWith("mutation:")) {
                EtoMutation mutation = EtoMutation.byOrdinal(Integer.parseInt(key.substring(9)));
                if (mutation == null) {
                    return null;
                }
                if (mutation.in(model.mutations())) {
                    if (mutation == EtoMutation.UP_WE_GO) {
                        return mutation.displayName() + ": on (right click: your flips soar)";
                    }
                    if (mutation == EtoMutation.TRUE_ANONYMITY) {
                        return mutation.displayName() + ": on (V held: the spiked club grows on past its full size)";
                    }
                    if (mutation == EtoMutation.WITNESS_THIS) {
                        return mutation.displayName() + ": on (Z held to the full charge: the combo follows the punch)";
                    }
                    return model.reviveIn() > 0 ? mutation.displayName() + ": regrowing, ready in " + reviveTag(model.reviveIn()).substring(10)
                        : mutation.displayName() + ": ready";
                }
                int next = EtoMutation.nextMilestone(model.mastery());
                return EtoMutation.canUnlock(model.mastery(), model.mutations(), mutation) ? "Click to unlock " + mutation.displayName()
                    : next > 0 ? "A mutation point comes at mastery " + next : "Every mutation point is spent";
            }
            if (key.equals("eyes")) {
                return "Move your kakugan onto your skin's eyes (Owl DNA turns one eye only)";
            }
        }
        return null;
    }
}
