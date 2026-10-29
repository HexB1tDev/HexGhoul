package com.hex.kagune.control.client;

import com.hex.kagune.control.UkakuStage;
import com.hex.kagune.control.ComboStyle;
import com.hex.kagune.control.client.ui.UiCanvas;
import com.hex.kagune.control.kakugan.KakuganEyes;
import com.hex.kagune.control.ukaku.UkakuRules;
import com.hex.kagune.control.ukaku.UkakuMutation;
import com.hex.kagune.control.ukaku.UkakuUnlock;
import com.hex.kagune.control.ukaku.WingForm;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Ukaku path's kagune menu (the menu key), drawn on any {@link UiCanvas}: the same layout and organic style as the
 * original Rinkaku menu (a 960 x 520 board scaled to the window), in Ukaku's colours (vermilion over dark violet), with
 * his transformation (one wing, two wings growing to full size at 500, the first kakuja at 750 and the final at 1000), a live wing
 * preview, his combat readout, the kakugan placement, and his mutations (a point at each milestone, spent by clicking).
 */
public final class UkakuMenuView {
    public static final int UI_WIDTH = 960;
    public static final int UI_HEIGHT = 520;

    // Ukaku's palette: the Rinkaku menu's crimson shifted to vermilion over dark violet.
    static final int PANEL_FILL = 0xF3120814;
    static final int PANEL_EDGE = 0xFF5C1A22;
    static final int INNER_FILL = 0xF20C0610;
    static final int INNER_EDGE = 0xFF2E0E1E;
    static final int VEIN = 0xFF7A2418;
    static final int VEIN_BRIGHT = 0xFF4E1A16;
    static final int TITLE = 0xFFF8E2D8;
    static final int SUBTITLE = 0xFFC0705A;
    static final int HEADER = 0xFFF0784A;
    static final int LABEL = 0xFF9E6A5C;
    static final int VALUE = 0xFFF2DCD2;
    static final int TRAIT = 0xFFE0BCAE;
    static final int MUTED = 0xFF9A7068;
    static final int FAINT = 0xFF6E5056;
    static final int BAR_BACK = 0xFF3B1A16;
    static final int BAR = 0xFFE0402A;
    static final int PREVIEW_FILL = 0xC61A0A12;
    static final int PREVIEW_EDGE = 0xFF5D2016;
    static final int PREVIEW_INNER = 0xE20C080E;
    static final int PREVIEW_INNER_EDGE = 0xFF2A1416;
    static final int SOON = 0xFF8A6A9E;

    /** What the menu shows. */
    public record Model(int mastery, boolean active, WingForm form, KakuganEyes eyes, int mutations, QuestLocks quests) {
        public Model(int mastery, boolean active, WingForm form, KakuganEyes eyes) {
            this(mastery, active, form, eyes, 0);
        }

        public Model(int mastery, boolean active, WingForm form, KakuganEyes eyes, int mutations) {
            this(mastery, active, form, eyes, mutations, QuestLocks.ALL);
        }

    }

    /** A clickable area on the board (virtual 960 x 520 coordinates). */
    public record Rect(int x, int y, int width, int height) {
        public boolean contains(double px, double py) {
            return px >= this.x && px <= this.x + this.width && py >= this.y && py <= this.y + this.height;
        }
    }

    private UkakuMenuView() {
    }

    /**
     * Draws the board at virtual size (the caller scales the canvas) and returns its clickable areas by name ("eyes",
     * "stage:ONE_WING", ...). {@code mouseX}/{@code mouseY} are in board coordinates, for hovering and the tooltip.
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
            organicPanel(c, centerX, centerY, panelWidth / 2, panelHeight / 2, PANEL_FILL, PANEL_EDGE);
            organicPanel(c, centerX, centerY, panelWidth / 2 - 5, panelHeight / 2 - 5, INNER_FILL, INNER_EDGE);
            drawVein(c, left + 262, top + 44, left + 280, centerY, left + 250, top + panelHeight - 45, VEIN, 3);
            drawVein(c, left + 535, top + 42, left + 520, centerY, left + 550, top + panelHeight - 42, VEIN, 3);
            drawVein(c, left + 18, top + 74, centerX, top + 32, left + panelWidth - 18, top + 74, VEIN_BRIGHT, 2);
        });
        c.centered("U K A K U   E V O L U T I O N", centerX, top + 18, TITLE);
        c.centered("RC CELL / BLACK RABBIT MATRIX", centerX, top + 34, SUBTITLE);
        c.centered("M1 fire  \u2022  Z wing vortex  \u2022  V crystal  \u2022  K kakuja", centerX, top + 51, 0xFF9D6A5E);

        identity(c, model, left + 24, top + 68);
        preview(c, model, hits, left + 295, top + 76, 220, 190, time);
        stats(c, model, hits, left + 295, top + 285);
        mutations(c, hits, left + 566, top + 66, model);
        kakuja(c, hits, left + 24, top + panelHeight - 96);

        c.centered(model.active() ? "R  RETRACT" : "R  MANIFEST", centerX, top + panelHeight - 16, model.active() ? 0xFFFF7A5C : 0xFFB9654A);

        String tip = tooltip(hits, mouseX, mouseY, model);
        if (tip != null) {
            int width = c.width(tip) + 8;
            int x = (int)Math.min(UI_WIDTH - width - 4, mouseX + 10);
            int y = (int)Math.max(4, mouseY - 16);
            c.fill(x - 1, y - 1, x + width + 1, y + 13, 0xFF5C1A22);
            c.fill(x, y, x + width, y + 12, 0xF0140A12);
            c.text(tip, x + 4, y + 2, VALUE, false);
        }
        return hits;
    }

    // ------------------------------------------------------------------ sections

    private static void identity(UiCanvas c, Model model, int x, int y) {
        int mastery = model.mastery();
        UkakuStage stage = UkakuStage.forMastery(mastery);
        WingForm form = model.form().shown(mastery);
        c.text("CURRENT ORGAN", x, y, HEADER, false);
        row(c, "Type", "Ukaku", x, y + 25);
        row(c, "Silhouette", "Wings from the shoulder blades", x, y + 43);
        row(c, "Stage", stage.displayName(), x, y + 61);
        row(c, "Wings", wingsText(mastery, form), x, y + 79);
        row(c, "Form", form.displayName(mastery), x, y + 97);
        row(c, "Length", String.format("%.1f blocks", UkakuRules.wingLength(mastery, form)), x, y + 115);

        c.text("MASTERY", x, y + 151, HEADER, false);
        c.text(mastery + " / 1000", x + 126, y + 151, VALUE, false);
        c.batch(() -> {
            capsule(c, x, y + 171, 205, 8, BAR_BACK);
            capsule(c, x, y + 171, Math.max(8, Math.round(205.0F * Math.min(1000, mastery) / 1000.0F)), 8, BAR);
        });
        c.text(nextMilestone(mastery), x, y + 185, MUTED, false);

        c.text("UNLOCKS", x, y + 202, HEADER, false);
        int rowY = y + 223;
        for (UkakuUnlock unlock : track(mastery)) {
            boolean reached = unlock.reached(mastery);
            int colour = reached ? TRAIT : unlock.comingSoon() ? SOON : MUTED;
            c.text(Integer.toString(unlock.mastery()), x + 5, rowY, reached ? MUTED : colour, false);
            c.text(unlock.text() + (unlock.comingSoon() ? "  (soon)" : ""), x + 34, rowY, colour, false);
            rowY += 16;
        }
    }

    /** The live preview: Ukaku's wings as they are now (one, or two growing to full size), with the stage buttons under them. */
    private static void preview(UiCanvas c, Model model, Map<String, Rect> hits, int x, int y, int width, int height, double time) {
        int mastery = model.mastery();
        WingForm form = model.form().shown(mastery);
        c.batch(() -> {
            organicPanel(c, x + width / 2, y + height / 2, width / 2, height / 2, PREVIEW_FILL, PREVIEW_EDGE);
            organicPanel(c, x + width / 2, y + height / 2, width / 2 - 5, height / 2 - 5, PREVIEW_INNER, PREVIEW_INNER_EDGE);
            // A few stars in the dark, like the starry wings of the later eras.
            for (int i = 0; i < 18; i++) {
                int sx = x + 18 + (int)(hash(i * 3 + 1) * (width - 36));
                int sy = y + 22 + (int)(hash(i * 3 + 2) * (height - 70));
                int glow = 0x50 + (int)(0x60 * (0.5 + 0.5 * Math.sin(time * 1.7 + i)));
                c.fill(sx, sy, sx + 1, sy + 1, glow << 24 | 0xB8C4FF);
            }
        });
        c.centered("LIVE RC RESPONSE", x + width / 2, y + 10, 0xFFCF7A63);
        // The wings, as big as they have grown, and only the left one before the twin wings.
        float size = (UkakuRules.wingLength(mastery, form) - UkakuRules.ONE_WING_LENGTH) / (UkakuRules.FULL_WINGS_LENGTH - UkakuRules.ONE_WING_LENGTH);
        float scale = 0.5F + 0.5F * Math.max(0.0F, Math.min(1.0F, size));
        int boxWidth = Math.round((width - 16) * scale);
        int boxHeight = Math.round(122 * scale);
        int boxX = x + (width - boxWidth) / 2;
        int boxY = y + 22 + (122 - boxHeight);
        PathChoiceLayout.Box box = new PathChoiceLayout.Box(boxX, boxY, boxWidth, boxHeight);
        boolean oneWing = form == WingForm.RABBIT || form == WingForm.ONE_WING;
        List<PathSilhouette.Mark> marks = new ArrayList<>();
        for (PathSilhouette.Mark mark : PathSilhouette.ukaku(box, time, 1.0F, false, 0x0C080E)) {
            // Seen from the front, his left wing is on the right: drop the other one when only one is out.
            if (!oneWing || centreX(mark) >= box.centerX() - 3) {
                marks.add(mark);
            }
        }
        c.batch(() -> {
            for (PathSilhouette.Mark mark : marks) {
                draw(c, mark);
            }
        });
        String caption = form.finalKakuja() ? "final kakuja / needles and fire" : form.kakuja() ? "first kakuja / six armored tentacles" : !form.rightWing() ? (mastery < UkakuStage.TWIN_WINGS.requiredMastery() ? "one small wing / twin wings at 100" : "Rabbit Guise / one wing out")
            : mastery < UkakuStage.FULL_SIZE_MASTERY ? "two wings / growing until mastery 500" : "full :re wings";
        c.centered(caption, x + width / 2, y + height - 46, 0xFF8F6152);

        // The transformation, one button per stage.
        UkakuStage current = UkakuStage.forMastery(mastery);
        int buttonY = y + height - 27;
        int buttonWidth = 40;
        int startX = x + (width - (UkakuStage.values().length * (buttonWidth + 3) - 3)) / 2;
        int index = 0;
        for (UkakuStage stage : UkakuStage.values()) {
            int bx = startX + index * (buttonWidth + 3);
            boolean reached = !stage.comingSoon() && mastery >= stage.requiredMastery();
            int colour = stage == current ? 0xFFC8402A : stage.comingSoon() ? 0xFF241C26 : reached ? 0xFF5D2A20 : 0xFF2E2426;
            c.fill(bx, buttonY, bx + buttonWidth, buttonY + 15, colour);
            if (stage.comingSoon()) {
                c.fill(bx, buttonY, bx + buttonWidth, buttonY + 1, SOON);
            }
            String label = switch (stage) {
                case ONE_WING -> "ONE";
                case TWIN_WINGS -> "TWIN";
                case FULL_WINGS -> "FULL";
                case KAKUJA_I -> "KAK I";
                case KAKUJA_II -> "KAK II";
            };
            c.centered(label, bx + buttonWidth / 2, buttonY + 4, stage.comingSoon() ? SOON : reached ? 0xFFF6DED6 : 0xFF706058);
            hits.put("stage:" + stage.name(), new Rect(bx, buttonY, buttonWidth, 15));
            index++;
        }
    }

    private static void stats(UiCanvas c, Model model, Map<String, Rect> hits, int x, int y) {
        int mastery = model.mastery();
        WingForm form = model.form().shown(mastery);
        c.text("COMBAT READOUT", x, y, HEADER, false);
        c.text("Close combo     " + comboName(UkakuRules.style(mastery, form, false)), x, y + 23, TRAIT, false);
        c.text(String.format("Melee damage   %.1f", UkakuRules.meleeDamage(mastery)), x, y + 40, TRAIT, false);
        c.text(mastery >= UkakuRules.VORTEX_MASTERY ? String.format("Wing vortex     %.1f + fire, %.0f blocks", UkakuRules.vortexDamage(mastery),
            UkakuRules.VORTEX_RANGE) : "Wing vortex     at mastery " + UkakuRules.VORTEX_MASTERY, x, y + 57, TRAIT, false);
        c.text(mastery >= UkakuRules.CRYSTAL_MASTERY ? String.format("Crystal spikes  %.0f blocks, bleed", UkakuRules.crystalRange(mastery))
            : "Crystal spikes  at mastery " + UkakuRules.CRYSTAL_MASTERY, x, y + 74, TRAIT, false);
        boolean agile = UkakuMutation.AGILITY_BOOST.in(model.mutations());
        c.text(String.format("RC cost          x%.2f   \u2022   Speed %s, Jump %s", UkakuRules.rcCostMultiplier(mastery),
            roman(UkakuRules.speedAmplifier(mastery) + (agile ? UkakuRules.AGILITY_SPEED : 0) + 1),
            roman(UkakuRules.jumpAmplifier(mastery) + (agile ? UkakuRules.AGILITY_JUMP : 0) + 1)), x, y + 91, MUTED, false);

        // The kakugan: where the eyes are, and a button to move them.
        c.text("KAKUGAN", x, y + 132, HEADER, false);
        KakuganEyes eyes = model.eyes();
        String where = "Eyes at column " + (eyes.rightX() + 1) + " / " + (eyes.leftX() + 1) + ", row " + (eyes.rightY() + 1)
            + (!eyes.oneEyed() ? "" : eyes.ghoul() == KakuganEyes.RIGHT ? ", right only" : ", left only");
        c.text(where, x, y + 146, MUTED, false);
        Rect button = new Rect(x, y + 160, 214, 18);
        boolean over = false;
        control(c, button, "ADJUST KAKUGAN", true, over);
        hits.put("eyes", button);
    }

    /**
     * Ukaku's four mutations, all of which he can have: each mastery milestone earns a point, spent here by clicking the
     * mutation he wants next ("mutation:N", N its ordinal).
     */
    private static void mutations(UiCanvas c, Map<String, Rect> hits, int x, int y, Model model) {
        int points = UkakuMutation.available(model.mastery(), model.mutations(), model.quests().points());
        int next = UkakuMutation.nextMilestone(model.mastery());
        c.text("MUTATIONS", x, y, HEADER, false);
        String status = points > 0 ? points + (points == 1 ? " point to spend: click one" : " points to spend: click one")
            : next > 0 ? "next point at mastery " + next : "every milestone reached";
        c.text(c.fit(status, 250), x + 86, y, points > 0 ? 0xFFF0B060 : MUTED, false);
        int width = 335;
        UkakuMutation[] all = UkakuMutation.values();
        // 1.0.41: five now (Graft's revive the fifth): the cards a little shorter so all of them fit the board.
        int height = all.length > 4 ? 66 : 82;
        int gap = all.length > 4 ? 5 : 8;
        for (int index = 0; index < all.length; index++) {
            UkakuMutation mutation = all[index];
            int top = y + 18 + index * (height + gap);
            boolean owned = mutation.in(model.mutations());
            boolean quest = !owned && !model.quests().passes(com.hex.kagune.control.quest.Gates.ukaku(mutation));
            boolean ready = !quest && UkakuMutation.canUnlock(model.mastery(), model.mutations(), mutation, model.quests().points());
            int edge = owned ? 0xFFC8402A : ready ? 0xFFF0A040 : 0xFF3A2430;
            int fill = owned ? 0xFF3A1A16 : ready ? 0xFF2E2016 : 0xFF1E161C;
            c.fill(x, top, x + width, top + height, edge);
            c.fill(x + 2, top + 2, x + width - 2, top + height - 2, fill);
            c.text(mutation.displayName(), x + 10, top + 8, owned ? 0xFFFFD6C8 : ready ? 0xFFF7E3C8 : 0xFF8F7A80, false);
            String tag = owned ? "UNLOCKED" : ready ? "CLICK TO UNLOCK" : quest ? "NEEDS ITS QUEST" : "NEEDS A POINT";
            c.text(tag, x + width - 10 - c.width(tag), top + 8, owned ? 0xFFE86A4A : ready ? 0xFFF0B060 : 0xFF6E5A62, false);
            int line = 0;
            for (String row : wrap(c, mutation.description(), width - 20)) {
                c.text(row, x + 10, top + 26 + line * 12, owned ? 0xFFD8B8B0 : 0xFF9C868E, false);
                line++;
            }
            hits.put("mutation:" + mutation.ordinal(), new Rect(x, top, width, height));
        }
        StringBuilder milestones = new StringBuilder("Points at mastery ");
        for (int i = 0; i < UkakuMutation.MILESTONES.length; i++) {
            milestones.append(i == 0 ? "" : ", ").append(UkakuMutation.MILESTONES[i]);
        }
        c.text(milestones.toString(), x, y + 18 + all.length * (height + gap) + 2, FAINT, false);
    }

    /** {@code text} broken into lines no wider than {@code width}. */
    static List<String> wrap(UiCanvas c, String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String trial = line.length() == 0 ? word : line + " " + word;
            if (c.width(trial) > width && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(trial);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    /** The two kakuja forms that come after full size: the first (a tap of K transforms into it), the final one (K held). */
    private static void kakuja(UiCanvas c, Map<String, Rect> hits, int x, int y) {
        c.text("KAKUJA FORMS", x, y, HEADER, false);
        String[] names = {"Kakuja I", "Kakuja II"};
        UkakuStage[] stages = {UkakuStage.KAKUJA_I, UkakuStage.KAKUJA_II};
        for (int i = 0; i < 2; i++) {
            int bx = x + i * 112;
            Rect box = new Rect(bx, y + 16, 104, 34);
            c.fill(bx, y + 16, bx + 104, y + 50, 0xFF3A2436);
            c.fill(bx + 1, y + 17, bx + 103, y + 49, 0xFF1C1420);
            c.centered(names[i], bx + 52, y + 21, 0xFFC9B2D6);
            String how = stages[i].comingSoon() ? "COMING SOON" : i == 0 ? "TAP K" : "HOLD K";
            c.centered(how + "  \u2022  " + stages[i].requiredMastery(), bx + 52, y + 35, SOON);
            hits.put("stage:" + stages[i].name(), box);
        }
    }

    // ------------------------------------------------------------------ text

    private static void row(UiCanvas c, String label, String value, int x, int y) {
        c.text(label, x, y, LABEL, false);
        c.text(c.fit(value, 140), x + 76, y, VALUE, false);
    }

    static String wingsText(int mastery, WingForm form) {
        if (form.finalKakuja()) {
            return "final kakuja: 3 + 3 fire";
        }
        if (form.kakuja()) {
            return "kakuja: 6 tentacles";
        }
        if (mastery < UkakuStage.TWIN_WINGS.requiredMastery()) {
            return "1 small wing";
        }
        int grown = Math.round(UkakuStage.growth(mastery) * 100.0F);
        String size = grown >= 100 ? "full size" : grown + "% grown";
        return (form.rightWing() ? "2" : "1 of 2") + "  (" + size + ")";
    }

    static String nextMilestone(int mastery) {
        UkakuUnlock next = UkakuUnlock.next(mastery);
        if (next == null) {
            return "EVOLUTION COMPLETE";
        }
        if (next.comingSoon() && mastery >= next.mastery()) {
            return "NEXT  FINAL KAKUJA (coming soon)";
        }
        return "NEXT UNLOCK  " + next.mastery() + "  (" + (next.mastery() - mastery) + " mastery)";
    }

    /** Rows of the unlock track shown: the last few steps reached, then the next few, {@link #TRACK_ROWS} in all. */
    static final int TRACK_ROWS = 6;

    static List<UkakuUnlock> track(int mastery) {
        UkakuUnlock[] all = UkakuUnlock.values();
        int start = Math.max(0, Math.min(all.length - TRACK_ROWS, UkakuUnlock.reachedCount(mastery) - TRACK_ROWS / 2));
        List<UkakuUnlock> rows = new ArrayList<>();
        for (int i = start; i < Math.min(all.length, start + TRACK_ROWS); i++) {
            rows.add(all[i]);
        }
        return rows;
    }

    static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> Integer.toString(level);
        };
    }

    static String comboName(ComboStyle style) {
        return switch (style) {
            case UKAKU_KICKS -> "Kicks + shard burst";
            case UKAKU_CRYSTAL_KICKS -> "Kicks (wings crystallized)";
            case UKAKU_BLADES -> "Wing blades + kicks";
            case UKAKU_RABBIT -> "Kicks + one-wing shards";
            case UKAKU_RABBIT_BLADES -> "One-wing blade + kicks";
            case UKAKU_ONE_WING -> "One-Winged Kagune";
            default -> style.name().toLowerCase();
        };
    }

    private static String tooltip(Map<String, Rect> hits, double x, double y, Model model) {
        for (Map.Entry<String, Rect> entry : hits.entrySet()) {
            if (!entry.getValue().contains(x, y)) {
                continue;
            }
            String key = entry.getKey();
            if (key.startsWith("stage:")) {
                UkakuStage stage = UkakuStage.valueOf(key.substring(6));
                return stage.comingSoon() ? stage.displayName() + ": coming soon (mastery " + stage.requiredMastery() + ")"
                    : stage.displayName() + " (mastery " + stage.requiredMastery() + ")" + (model.mastery() >= stage.requiredMastery() ? "" : ": locked");
            }
            if (key.startsWith("mutation:")) {
                UkakuMutation mutation = UkakuMutation.byOrdinal(Integer.parseInt(key.substring(9)));
                if (mutation == null) {
                    return null;
                }
                if (mutation.in(model.mutations())) {
                    return null;
                }
                return UkakuMutation.canUnlock(model.mastery(), model.mutations(), mutation) ? "Click to unlock " + mutation.displayName()
                    : "Earn a point at the next milestone to unlock it";
            }
            if (key.equals("eyes")) {
                return "Move your kakugan onto your skin's eyes";
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ drawing (as the Rinkaku menu draws)

    private static void control(UiCanvas c, Rect r, String label, boolean enabled, boolean active) {
        int edge = active ? 0xFFCF5A3A : enabled ? 0xFF8E3A2A : 0xFF49303A;
        int fill = active ? 0xFF7F2A1A : enabled ? 0xFF42201A : 0xFF241D21;
        c.fill(r.x(), r.y(), r.x() + r.width(), r.y() + r.height(), edge);
        c.fill(r.x() + 1, r.y() + 1, r.x() + r.width() - 1, r.y() + r.height() - 1, fill);
        c.centered(label, r.x() + r.width() / 2, r.y() + 5, enabled ? 0xFFF0D6CE : 0xFF715B63);
    }

    static void organicPanel(UiCanvas c, int centerX, int centerY, int radiusX, int radiusY, int fill, int edge) {
        for (int y = -radiusY; y <= radiusY; y++) {
            double normalized = (double)y / Math.max(1, radiusY);
            double oval = Math.sqrt(Math.max(0.0, 1.0 - normalized * normalized));
            double ripple = 1.0 + 0.018 * Math.sin(y * 0.17) + 0.012 * Math.sin(y * 0.047 + 1.2);
            int half = (int)(radiusX * oval * ripple);
            c.fill(centerX - half, centerY + y, centerX + half + 1, centerY + y + 1, edge);
            int inner = Math.max(0, half - 2);
            c.fill(centerX - inner, centerY + y, centerX + inner + 1, centerY + y + 1, fill);
        }
    }

    static void capsule(UiCanvas c, int x, int y, int length, int radius, int colour) {
        if (length <= radius * 2) {
            fillCircle(c, x + length / 2, y, Math.max(1, length / 2), colour);
            return;
        }
        c.fill(x + radius, y - radius, x + length - radius, y + radius + 1, colour);
        fillCircle(c, x + radius, y, radius, colour);
        fillCircle(c, x + length - radius, y, radius, colour);
    }

    static void drawVein(UiCanvas c, int x0, int y0, int controlX, int controlY, int x1, int y1, int colour, int radius) {
        int lastX = x0;
        int lastY = y0;
        for (int i = 1; i <= 36; i++) {
            float t = i / 36.0F;
            float u = 1.0F - t;
            int x = Math.round(u * u * x0 + 2.0F * u * t * controlX + t * t * x1);
            int y = Math.round(u * u * y0 + 2.0F * u * t * controlY + t * t * y1);
            drawSegment(c, lastX, lastY, x, y, colour, radius);
            lastX = x;
            lastY = y;
        }
    }

    static void drawSegment(UiCanvas c, int x0, int y0, int x1, int y1, int colour, int radius) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= Math.max(1, steps); i++) {
            float t = i / (float)Math.max(1, steps);
            fillCircle(c, Math.round(x0 + (x1 - x0) * t), Math.round(y0 + (y1 - y0) * t), radius, colour);
        }
    }

    static void fillCircle(UiCanvas c, int centerX, int centerY, int radius, int colour) {
        for (int y = -radius; y <= radius; y++) {
            int half = (int)Math.sqrt(Math.max(0, radius * radius - y * y));
            c.fill(centerX - half, centerY + y, centerX + half + 1, centerY + y + 1, colour);
        }
    }

    private static float centreX(PathSilhouette.Mark mark) {
        if (mark instanceof PathSilhouette.Shape shape) {
            float sum = 0.0F;
            for (float x : shape.x()) {
                sum += x;
            }
            return sum / shape.x().length;
        }
        if (mark instanceof PathSilhouette.Stroke stroke) {
            return (stroke.x0() + stroke.x1()) / 2.0F;
        }
        return ((PathSilhouette.Disc)mark).cx();
    }

    /** A silhouette mark as GUI fills (as the starting screen draws them). */
    static void draw(UiCanvas c, PathSilhouette.Mark mark) {
        if (mark instanceof PathSilhouette.Stroke stroke) {
            float dx = stroke.x1() - stroke.x0();
            float dy = stroke.y1() - stroke.y0();
            float thinnest = Math.max(0.6F, Math.min(stroke.t0(), stroke.t1()));
            int steps = Math.max(1, (int)Math.ceil(Math.sqrt(dx * dx + dy * dy) / Math.max(0.75F, thinnest * 0.6F)));
            int colour = 0xFF000000 | stroke.rgb();
            for (int i = 0; i <= steps; i++) {
                float t = i / (float)steps;
                float half = (stroke.t0() + (stroke.t1() - stroke.t0()) * t) / 2.0F;
                float x = stroke.x0() + dx * t;
                float y = stroke.y0() + dy * t;
                c.fill(Math.round(x - half), Math.round(y - half), Math.max(Math.round(x - half) + 1, Math.round(x + half)),
                    Math.max(Math.round(y - half) + 1, Math.round(y + half)), colour);
            }
        } else if (mark instanceof PathSilhouette.Shape shape) {
            c.polygon(shape.x(), shape.y(), shape.argb());
        } else if (mark instanceof PathSilhouette.Disc disc) {
            int colour = 0xFF000000 | disc.rgb();
            int radius = Math.max(1, Math.round(disc.r()));
            for (int dy = -radius; dy < radius; dy++) {
                float row = dy + 0.5F;
                int half = Math.round((float)Math.sqrt(Math.max(0.0F, disc.r() * disc.r() - row * row)));
                int cx = Math.round(disc.cx());
                int cy = Math.round(disc.cy());
                c.fill(cx - Math.max(half, 1), cy + dy, cx + Math.max(half, 1), cy + dy + 1, colour);
            }
        }
    }

    private static double hash(int n) {
        long x = n * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 29;
        return (x >>> 11) / (double)(1L << 53);
    }
}
