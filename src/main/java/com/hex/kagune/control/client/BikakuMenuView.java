package com.hex.kagune.control.client;

import com.hex.kagune.control.bikaku.BikakuRules;
import com.hex.kagune.control.client.ui.UiCanvas;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 3.4.9: the Bikaku path's kagune menu (the menu key), drawn like the other paths' (the same organic board, 960 x 520 scaled to the
 * window) in the worm's crimson-purple over black: the kagune, mastery, RC, what its bites do, the kakugan placement, its moves now and
 * the plan's moves still to come with the mastery each will need.
 */
public final class BikakuMenuView {
    public static final int UI_WIDTH = UkakuMenuView.UI_WIDTH;
    public static final int UI_HEIGHT = UkakuMenuView.UI_HEIGHT;

    static final int PANEL_FILL = 0xF30E0308;
    static final int PANEL_EDGE = 0xFF5A1030;
    static final int INNER_FILL = 0xF2070205;
    static final int INNER_EDGE = 0xFF2A0A18;
    static final int VEIN = 0xFF781236;
    static final int VEIN_BRIGHT = 0xFF480A20;
    static final int TITLE = 0xFFF4DAE2;
    static final int SUBTITLE = 0xFFC03A60;
    static final int HEADER = 0xFFE8386A;
    static final int LABEL = 0xFF9A5A70;
    static final int VALUE = 0xFFF0D6DE;
    static final int TRAIT = 0xFFDCAEBE;
    static final int MUTED = 0xFF946274;
    static final int FAINT = 0xFF64444E;
    static final int BAR_BACK = 0xFF34101E;
    static final int BAR = 0xFFC8204E;
    static final int RC = 0xFFA81E3A;
    static final int BUTTON = 0xFF34101E;
    static final int BUTTON_HOVER = 0xFF581630;
    static final int SOON = 0xFFB08A50;

    /**
     * What the menu shows. 3.6.3: his mutations ({@code BikakuMutation} bits) and how long until Grand Devouring can feed again (ticks; 0
     * ready).
     */
    public record Model(int mastery, boolean active, float energy, float maxEnergy, int form, int mutations, long devourWait) {
        /** As before 3.6.0 (the one worm). */
        public Model(int mastery, boolean active, float energy, float maxEnergy) {
            this(mastery, active, energy, maxEnergy, 0);
        }

        /** As before 3.6.3 (no mutations). */
        public Model(int mastery, boolean active, float energy, float maxEnergy, int form) {
            this(mastery, active, energy, maxEnergy, form, 0, 0L);
        }
    }

    /** The plan's moves still to come (the owner's spec), each with the mastery it is meant to need (3.5.0: the built ones are moves). */
    static final Object[][] PLANNED = {
        {"Devouring Maw", BikakuRules.MAW}, {"Predatory Drag", BikakuRules.DRAG}, {"Ravenous Pursuit", BikakuRules.PURSUIT},
        {"Carrion Eruption", BikakuRules.ERUPTION}, {"Monstrous Regeneration", BikakuRules.REGENERATION}};

    /** The planned moves not built yet. */
    static java.util.List<Object[]> coming() {
        java.util.List<Object[]> out = new java.util.ArrayList<>();
        for (Object[] planned : PLANNED) {
            boolean built = false;
            for (com.hex.kagune.control.bikaku.BikakuMove move : com.hex.kagune.control.bikaku.BikakuMove.values()) {
                built |= move.displayName().equals(planned[0]);
            }
            if (!built) {
                out.add(planned);
            }
        }
        return out;
    }

    private BikakuMenuView() {
    }

    /** Draws the board and returns its clickable areas: "eyes". */
    public static Map<String, UkakuMenuView.Rect> draw(UiCanvas c, Model model, double mouseX, double mouseY, double time) {
        Map<String, UkakuMenuView.Rect> hits = new LinkedHashMap<>();
        int panelWidth = 930;
        int panelHeight = 500;
        int centerX = UI_WIDTH / 2;
        int centerY = UI_HEIGHT / 2;
        int left = centerX - panelWidth / 2;
        int top = centerY - panelHeight / 2;
        c.batch(() -> {
            UkakuMenuView.organicPanel(c, centerX, centerY, panelWidth / 2, panelHeight / 2, PANEL_FILL, PANEL_EDGE);
            UkakuMenuView.organicPanel(c, centerX, centerY, panelWidth / 2 - 5, panelHeight / 2 - 5, INNER_FILL, INNER_EDGE);
            UkakuMenuView.drawVein(c, left + 262, top + 44, left + 280, centerY, left + 250, top + panelHeight - 45, VEIN, 3);
            UkakuMenuView.drawVein(c, left + 535, top + 42, left + 520, centerY, left + 550, top + panelHeight - 42, VEIN, 3);
            UkakuMenuView.drawVein(c, left + 18, top + 74, centerX, top + 32, left + panelWidth - 18, top + 74, VEIN_BRIGHT, 2);
        });
        c.centered("B I K A K U", centerX, top + 18, TITLE);
        c.centered("THE WORM KAGUNE  /  A MOUTH OF FANGS", centerX, top + 34, SUBTITLE);
        c.centered("M1 bites (a string of three)  •  its moves on your kagune keys, opened by mastery  •  each bite that lands heals you", centerX,
            top + 51, 0xFF94506A);

        identity(c, model, hits, mouseX, mouseY, left + 24, top + 68);
        middle(c, model, hits, mouseX, mouseY, left + 295, top + 76, time);
        moves(c, model, hits, mouseX, mouseY, left + 566, top + 70);

        c.centered(model.active() ? "R  RETRACT" : "ALT + R  MANIFEST", centerX, top + panelHeight - 16, model.active() ? 0xFFFF4A70 : 0xFFB0405A);

        String tip = tooltip(hits, mouseX, mouseY);
        if (tip != null) {
            int width = c.width(tip) + 8;
            int x = (int)Math.min(UI_WIDTH - width - 4, mouseX + 10);
            int y = (int)Math.max(4, mouseY - 16);
            c.fill(x - 1, y - 1, x + width + 1, y + 13, PANEL_EDGE);
            c.fill(x, y, x + width, y + 12, 0xF00E0308);
            c.text(tip, x + 4, y + 2, VALUE, false);
        }
        return hits;
    }

    private static void identity(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, int x, int y) {
        int mastery = model.mastery();
        com.hex.kagune.control.bikaku.BikakuForm current = com.hex.kagune.control.bikaku.BikakuForm.byOrdinal(model.form());
        c.text("CURRENT KAGUNE", x, y, HEADER, false);
        row(c, "Type", "Bikaku", x, y + 25);
        row(c, "Kagune", current.walks() ? "3 worms, 2 legs, one root" // 3.6.2: The Walker's walking tentacles
            : current.worms() == 1 ? "One worm, lower back" : current.worms() + " worms, one root", x, y + 43);
        row(c, "Length", "About 8 blocks", x, y + 61);
        row(c, "Mouth", "Six fangs, three rings", x, y + 79);
        row(c, "Colour", "Your kagune colour", x, y + 97);

        c.text("MASTERY", x, y + 122, HEADER, false);
        c.text(mastery + " / 1000", x + 126, y + 122, VALUE, false);
        c.batch(() -> {
            UkakuMenuView.capsule(c, x, y + 140, 205, 8, BAR_BACK);
            UkakuMenuView.capsule(c, x, y + 140, Math.max(8, Math.round(205.0F * Math.min(1000, mastery) / 1000.0F)), 8, BAR);
        });
        c.text("Every bite that lands grows it", x, y + 154, MUTED, false);

        // 3.6.0: its forms (each opens with mastery; every move made by all its worms).
        c.text("FORM", x, y + 180, HEADER, false);
        for (com.hex.kagune.control.bikaku.BikakuForm form : com.hex.kagune.control.bikaku.BikakuForm.values()) {
            int by = y + 196 + form.ordinal() * 22;
            boolean open = form.unlocked(mastery);
            boolean picked = form == current;
            UkakuMenuView.Rect rect = new UkakuMenuView.Rect(x, by, 205, 18);
            boolean hover = open && rect.contains(mouseX, mouseY);
            c.fill(x - 1, by - 1, x + 206, by + 19, picked ? VEIN_BRIGHT : PANEL_EDGE);
            c.fill(x, by, x + 205, by + 18, picked ? BUTTON_HOVER : hover ? BUTTON_HOVER : BUTTON);
            c.text(form.displayName().toUpperCase(java.util.Locale.ROOT), x + 8, by + 5, open ? (picked ? TITLE : VALUE) : FAINT, false);
            c.text(open ? (picked ? "ON" : form.walks() ? "walks" : form.worms() + (form.worms() == 1 ? " worm" : " worms")) // 3.6.2
                : !form.built() ? "soon" : Integer.toString(form.mastery()),
                x + 150, by + 5, open ? TRAIT : FAINT, false);
            if (open) {
                hits.put("form" + form.ordinal(), rect);
            }
        }
    }

    private static void middle(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, int x, int y, double time) {
        c.text("RC", x, y, HEADER, false);
        float share = model.maxEnergy() <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, model.energy() / model.maxEnergy()));
        c.text(Math.round(model.energy()) + " / " + Math.round(model.maxEnergy()), x + 126, y, VALUE, false);
        // 3.6.6: Potential Unlock: purple (the owner: "Should be Purple, the bar, my bad").
        boolean purple = com.hex.kagune.control.bikaku.BikakuMutation.POTENTIAL_UNLOCK.in(model.mutations());
        int rc = purple ? com.hex.kagune.control.KaguneTint.recolour(RC, com.hex.kagune.control.bikaku.BikakuRules.POTENTIAL_PURPLE) : RC;
        c.batch(() -> {
            UkakuMenuView.capsule(c, x, y + 19, 205, 8, BAR_BACK);
            UkakuMenuView.capsule(c, x, y + 19, Math.max(8, Math.round(205.0F * share)), 8, rc);
        });
        c.text("One bar: RC, healing and food. Eat to fill it", x, y + 33, MUTED, false);

        c.text("PREDATORY FEEDING", x, y + 58, HEADER, false);
        row(c, "A bite", "+" + half(BikakuRules.BITE_HEAL) + " heart", x, y + 76);
        row(c, "A kill", "+" + half(BikakuRules.KILL_HEAL) + " hearts more", x, y + 92);
        c.text("Bites make them bleed; the third stuns", x, y + 108, MUTED, false);

        // 3.5.4: the kakuja.
        c.text("KAKUJA  (K, mastery " + BikakuRules.KAKUJA + ")", x, y + 222, HEADER, false);
        c.text(Math.round(BikakuRules.KAKUJA_COST) + " RC, then it drains; " + BikakuRules.KAKUJA_COOLDOWN / 20 + " s to come back", x, y + 238, TRAIT, false);
        c.text("Rooted, " + Math.round((1.0F - BikakuRules.KAKUJA_TAKEN) * 100) + "% less from every blow,", x, y + 252, MUTED, false);
        c.text("regenerating; its tentacles bite " + Math.round(BikakuRules.KAKUJA_REACH) + " blocks out,", x, y + 266, MUTED, false);
        c.text("and bite what comes at it on their own", x, y + 280, MUTED, false);

        // 3.5.3: its passives while the worm is out.
        c.text("WHILE THE WORM IS OUT", x, y + 134, HEADER, false);
        c.text("Abnormal Regeneration: you heal far", x, y + 152, TRAIT, false);
        c.text("sooner and faster", x, y + 166, TRAIT, false);
        c.text("Pain Suppression: hard to knock back,", x, y + 184, TRAIT, false);
        c.text("stuns last half as long", x, y + 198, TRAIT, false);

        button(c, hits, "eyes", "ADJUST KAKUGAN", x, y + 296, 220, mouseX, mouseY, time);
    }

    /** Its moves now, and the plan's still to come; 3.6.3: then its mutations. */
    private static void moves(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, int x, int y) {
        c.text("MOVES", x, y, HEADER, false);
        int rowY = y + 20;
        String[][] now = {{"M1", "Snap · Lunge · Maul (stuns)"}};
        for (String[] move : now) {
            c.text(move[0], x, rowY, LABEL, false);
            c.text(move[1], x + 40, rowY, VALUE, false);
            rowY += 16;
        }
        // 3.5.0: the moves on its keys (the original's kagune keys), each with its mastery and cooldown; 3.5.7: then the kakuja's own.
        for (boolean kakuja : new boolean[] {false, true}) {
            if (kakuja) {
                rowY += 8;
                c.text("IN THE KAKUJA", x, rowY, HEADER, false);
                rowY += 18;
            }
            for (com.hex.kagune.control.bikaku.BikakuMove move : com.hex.kagune.control.bikaku.BikakuMove.values()) {
                if (move == com.hex.kagune.control.bikaku.BikakuMove.NONE || move.kakuja() != kakuja) {
                    continue;
                }
                boolean open = move.unlocked(model.mastery());
                c.text(move.slot().name(), x, rowY, open ? LABEL : FAINT, false);
                c.text(move.displayName(), x + 40, rowY, open ? VALUE : FAINT, false);
                c.text(open ? (move.cooldown() / 20) + " s" : Integer.toString(move.mastery()), x + 240, rowY, open ? TRAIT : FAINT, false);
                rowY += 16;
            }
        }
        c.text("Keys are your kagune keys (Controls)", x, rowY + 6, MUTED, false);
        c.text("Hold M1 or a thrown move's key to throw it further:", x, rowY + 20, MUTED, false);
        c.text("up to " + Math.round(BikakuRules.HELD_REACH) + " blocks after " + BikakuRules.CHARGE_FULL / 20 + " s (it is back slower)", x, rowY + 32, MUTED, false);
        rowY += 26;

        java.util.List<Object[]> coming = coming();
        int trackY = rowY + 50;
        if (!coming.isEmpty()) {
            c.text("COMING IN LATER UPDATES", x, rowY + 32, HEADER, false);
            for (Object[] move : coming) {
                int needs = (Integer)move[1];
                c.text(needs + "", x, trackY, FAINT, false);
                c.text((String)move[0], x + 40, trackY, model.mastery() >= needs ? SOON : MUTED, false);
                trackY += 15;
            }
        }
        mutations(c, model, hits, mouseX, mouseY, x, trackY - 2);
    }

    /**
     * 3.6.3: its mutations, each unlocked by a quest (the owner: "these mutations are NOT unlocked through mastery they're gonna be unlocked
     * through an NPC's quest"): one he has says it is on (and Grand Devouring how long until it can feed again), one he hasn't that it is
     * locked, one still to come that it is coming.
     */
    private static void mutations(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, int x, int y) {
        c.text("MUTATIONS", x, y, HEADER, false);
        c.text("each unlocked by a quest", x + 86, y, MUTED, false);
        int width = 300;
        int height = 38;
        for (com.hex.kagune.control.bikaku.BikakuMutation mutation : com.hex.kagune.control.bikaku.BikakuMutation.values()) {
            int top = y + 15 + mutation.ordinal() * (height + 5);
            boolean owned = mutation.in(model.mutations());
            c.fill(x, top, x + width, top + height, owned ? 0xFFC8204E : PANEL_EDGE);
            c.fill(x + 1, top + 1, x + width - 1, top + height - 1, owned ? 0xFF3A0C1C : BUTTON);
            String tag = !mutation.built() ? "SOON" : owned ? devourTag(mutation, model.devourWait()) : "LOCKED";
            int tagWidth = c.width(tag);
            c.text(c.fit(mutation.displayName().toUpperCase(java.util.Locale.ROOT), width - 26 - tagWidth), x + 7, top + 5,
                owned ? TITLE : mutation.built() ? VALUE : FAINT, false);
            c.text(tag, x + width - 7 - tagWidth, top + 5, !mutation.built() ? SOON : owned ? 0xFFFF4A70 : MUTED, false);
            int line = 0;
            for (String row : UkakuMenuView.wrap(c, mutation.description(), width - 14)) {
                if (line < 2) {
                    c.text(row, x + 7, top + 16 + line * 10, owned ? TRAIT : mutation.built() ? MUTED : FAINT, false);
                }
                line++;
            }
        }
    }

    /** 3.6.3: a mutation he has: on, and Grand Devouring's wait until it can feed again. */
    static String devourTag(com.hex.kagune.control.bikaku.BikakuMutation mutation, long wait) {
        return mutation == com.hex.kagune.control.bikaku.BikakuMutation.GRAND_DEVOURING && wait > 0L ? "ON · " + (wait + 19L) / 20L + " s" : "ON";
    }

    private static String half(float health) {
        float hearts = health / 2.0F;
        return hearts == Math.round(hearts) ? Integer.toString(Math.round(hearts)) : Float.toString(hearts);
    }

    private static void button(UiCanvas c, Map<String, UkakuMenuView.Rect> hits, String key, String label, int x, int y, int width, double mouseX,
                               double mouseY, double time) {
        UkakuMenuView.Rect rect = new UkakuMenuView.Rect(x, y, width, 20);
        boolean hover = rect.contains(mouseX, mouseY);
        int pulse = hover ? 0 : (int)(10 * (0.5 + 0.5 * Math.sin(time * 2.4)));
        c.fill(x - 1, y - 1, x + width + 1, y + 21, PANEL_EDGE);
        c.fill(x, y, x + width, y + 20, hover ? BUTTON_HOVER : BUTTON + (pulse << 16));
        c.centered(label, x + width / 2, y + 6, hover ? TITLE : VALUE);
        hits.put(key, rect);
    }

    private static void row(UiCanvas c, String label, String value, int x, int y) {
        c.text(label, x, y, LABEL, false);
        c.text(value, x + 90, y, VALUE, false);
    }

    private static String tooltip(Map<String, UkakuMenuView.Rect> hits, double x, double y) {
        UkakuMenuView.Rect eyes = hits.get("eyes");
        if (eyes != null && eyes.contains(x, y)) {
            return "Place the kakugan on your skin's eyes";
        }
        return null;
    }
}
