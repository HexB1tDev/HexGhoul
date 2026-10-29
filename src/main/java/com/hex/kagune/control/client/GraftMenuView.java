package com.hex.kagune.control.client;

import com.hex.kagune.control.client.ui.UiCanvas;
import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.graft.GraftMutation;
import com.hex.kagune.control.graft.GraftRules;
import com.hex.kagune.control.graft.GraftStage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 7.0.0: Graft DNA's kagune menu (the menu key), drawn like the other paths' (the same organic board, 960 x 520 scaled to the
 * window) in dark crimson over black: the organ (Rize's transplanted kakuhou), mastery, the base stages to pick, RC, the kakuja, what
 * his body does, the kakugan placement, every key's move in the stage he is in, and the unlock track.
 */
public final class GraftMenuView {
    public static final int UI_WIDTH = UkakuMenuView.UI_WIDTH;
    public static final int UI_HEIGHT = UkakuMenuView.UI_HEIGHT;

    static final int PANEL_FILL = 0xF3100406;
    static final int PANEL_EDGE = 0xFF5E0C16;
    static final int INNER_FILL = 0xF2080304;
    static final int INNER_EDGE = 0xFF2C0A10;
    static final int VEIN = 0xFF7A0E18;
    static final int VEIN_BRIGHT = 0xFF4A0A10;
    static final int TITLE = 0xFFF4DADA;
    static final int SUBTITLE = 0xFFB83A44;
    static final int HEADER = 0xFFE8323E;
    static final int LABEL = 0xFF9A5A60;
    static final int VALUE = 0xFFF0D6D6;
    static final int TRAIT = 0xFFDCAEB2;
    static final int MUTED = 0xFF946268;
    static final int FAINT = 0xFF64444A;
    static final int BAR_BACK = 0xFF34101A;
    static final int BAR = 0xFFD01E2E;
    static final int RC = 0xFFA81E30;
    static final int BUTTON = 0xFF34101A;
    static final int BUTTON_HOVER = 0xFF581622;
    static final int KAKUJA = 0xFFF2F2F2;

    /** What the menu shows. */
    public record Model(int mastery, boolean active, GraftStage stage, boolean kakuja, float energy, float maxEnergy, int mutations,
                        QuestLocks quests) {
        public Model(int mastery, boolean active, GraftStage stage, boolean kakuja, float energy, float maxEnergy) {
            this(mastery, active, stage, kakuja, energy, maxEnergy, 0);
        }

        public Model(int mastery, boolean active, GraftStage stage, boolean kakuja, float energy, float maxEnergy, int mutations) {
            this(mastery, active, stage, kakuja, energy, maxEnergy, mutations, QuestLocks.ALL);
        }
    }

    private GraftMenuView() {
    }

    /** Draws the board and returns its clickable areas: "eyes", "stage:ID", "mutation:ID". */
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
        c.centered("F U R U T A   D N A", centerX, top + 18, TITLE);
        c.centered("RIZE'S KAKUHOU  /  ARTIFICIAL ONE-EYED GHOUL", centerX, top + 34, SUBTITLE);
        c.centered("M1 strings (a blade in hand: blade + kagune)  •  Ability 1-6  •  right click dash  •  K kakuja  •  hold K next stage", centerX,
            top + 51, 0xFF94505A);

        identity(c, model, hits, mouseX, mouseY, left + 24, top + 68);
        middle(c, model, hits, mouseX, mouseY, left + 295, top + 76, time);
        moves(c, model, left + 566, top + 70);

        c.centered(model.active() ? "R  RETRACT" : "ALT + R  MANIFEST", centerX, top + panelHeight - 16, model.active() ? 0xFFFF4A56 : 0xFFB0404A);

        String tip = tooltip(hits, mouseX, mouseY, model);
        if (tip != null) {
            int width = c.width(tip) + 8;
            int x = (int)Math.min(UI_WIDTH - width - 4, mouseX + 10);
            int y = (int)Math.max(4, mouseY - 16);
            c.fill(x - 1, y - 1, x + width + 1, y + 13, PANEL_EDGE);
            c.fill(x, y, x + width, y + 12, 0xF0100406);
            c.text(tip, x + 4, y + 2, VALUE, false);
        }
        return hits;
    }

    private static void identity(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, int x, int y) {
        int mastery = model.mastery();
        GraftStage shown = model.kakuja() ? GraftStage.KAKUJA : model.stage();
        c.text("CURRENT ORGAN", x, y, HEADER, false);
        row(c, "Type", "Rinkaku (transplanted)", x, y + 25);
        row(c, "Kakuhou", "Rize Kamishiro's", x, y + 43);
        row(c, "Kakugan", "Right eye", x, y + 61);
        row(c, "Stage", shown.displayName(), x, y + 79);
        row(c, "Limbs", Integer.toString(shown.limbs(mastery)), x, y + 97);

        c.text("MASTERY", x, y + 122, HEADER, false);
        c.text(mastery + " / 1000", x + 126, y + 122, VALUE, false);
        c.batch(() -> {
            UkakuMenuView.capsule(c, x, y + 140, 205, 8, BAR_BACK);
            UkakuMenuView.capsule(c, x, y + 140, Math.max(8, Math.round(205.0F * Math.min(1000, mastery) / 1000.0F)), 8, BAR);
        });
        c.text(next(mastery), x, y + 154, MUTED, false);

        c.text("STAGES", x, y + 176, HEADER, false);
        c.text("click to grow into one", x + 70, y + 176, FAINT, false);
        int rowY = y + 194;
        for (GraftStage option : GraftStage.values()) {
            // 7.0.1: a stage past its mastery may still wait for its quest.
            boolean quest = option.unlocked(mastery) && !model.quests().passes(com.hex.kagune.control.quest.Gates.graft(option));
            boolean unlocked = option.unlocked(mastery) && !quest;
            boolean selected = option == shown;
            UkakuMenuView.Rect rect = new UkakuMenuView.Rect(x, rowY - 3, 220, 16);
            boolean hover = rect.contains(mouseX, mouseY) && !option.kakuja();
            int fill = selected ? (option.kakuja() ? 0xFF2A2A2E : BAR) : hover && unlocked ? BUTTON_HOVER : BUTTON;
            c.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), fill);
            int label = !unlocked ? FAINT : option.kakuja() ? KAKUJA : VALUE;
            c.text(option.displayName() + (option.kakuja() ? "  (K)" : ""), x + 6, rowY + 1, label, false);
            c.text(unlocked ? (selected ? "out" : "") : quest ? "quest" : option.mastery() + "", x + 190 - (quest ? 6 : 0), rowY + 1,
                unlocked ? TRAIT : quest ? 0xFFE8A040 : FAINT, false);
            if (!option.kakuja()) {
                hits.put("stage:" + option.id(), rect);
            }
            rowY += 19;
        }
    }

    private static void middle(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, int x, int y, double time) {
        c.text("RC", x, y, HEADER, false);
        float share = model.maxEnergy() <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, model.energy() / model.maxEnergy()));
        c.text(Math.round(model.energy()) + " / " + Math.round(model.maxEnergy()), x + 126, y, VALUE, false);
        c.batch(() -> {
            UkakuMenuView.capsule(c, x, y + 19, 205, 8, BAR_BACK);
            UkakuMenuView.capsule(c, x, y + 19, Math.max(8, Math.round(205.0F * share)), 8, RC);
        });
        c.text("One bar: RC, healing and food. Eat to fill it", x, y + 33, MUTED, false);

        c.text("KAKUJA", x, y + 58, HEADER, false);
        row(c, "K tap", "Transform (" + GraftRules.KAKUJA_MASTERY + ")", x, y + 76);
        row(c, "Price", Math.round(GraftRules.KAKUJA_COST) + " RC, then it drains", x, y + 92);
        c.text("Takes " + Math.round((1.0F - GraftRules.KAKUJA_TAKEN) * 100) + "% less from every blow", x, y + 108, MUTED, false);
        c.text("Strength II, speed, a higher jump", x, y + 122, MUTED, false);

        mutations(c, model, hits, mouseX, mouseY, x, y + 142, time);

        button(c, hits, "eyes", "ADJUST KAKUGAN", x, y + 262, 220, mouseX, mouseY, time);
    }

    /** His mutations: one card each, taken (bright), ready to take with a point (click), or waiting for the next milestone. */
    private static void mutations(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, int x, int y,
                                  double time) {
        int points = GraftMutation.available(model.mastery(), model.mutations(), model.quests().points());
        c.text("MUTATIONS", x, y, HEADER, false);
        int next = GraftMutation.nextMilestone(model.mastery());
        c.text(points > 0 ? points + (points == 1 ? " point to spend" : " points to spend") : next < 0 ? "every point spent" : "next point at " + next,
            x + 92, y, points > 0 ? TRAIT : FAINT, false);
        int rowY = y + 16;
        for (GraftMutation mutation : GraftMutation.values()) {
            boolean owned = mutation.in(model.mutations());
            boolean quest = !owned && !model.quests().passes(com.hex.kagune.control.quest.Gates.graft(mutation));
            boolean ready = !owned && points > 0 && !quest;
            UkakuMenuView.Rect rect = new UkakuMenuView.Rect(x, rowY - 3, 225, 22);
            boolean hover = rect.contains(mouseX, mouseY);
            int pulse = ready && !hover ? (int)(14 * (0.5 + 0.5 * Math.sin(time * 3.0))) : 0;
            int fill = owned ? 0xFF4A0E18 : ready ? (hover ? BUTTON_HOVER : BUTTON + (pulse << 16)) : 0xFF1C0A0E;
            c.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), fill);
            String name = mutation.displayName();
            if (c.width(name) > 212) {
                name = name.substring(0, Math.min(name.length(), 30)) + "...";
            }
            c.text(name, x + 6, rowY + 1, owned ? VALUE : ready ? TRAIT : FAINT, false);
            c.text(owned ? "taken" : ready ? "click to take" : quest ? "needs its quest" : "locked", x + 6, rowY + 10,
                owned ? 0xFFE8323E : ready ? MUTED : quest ? 0xFFE8A040 : FAINT, false);
            hits.put("mutation:" + mutation.id(), rect);
            rowY += 25;
        }
    }

    /** Every key's move in the stage he is in, and what unlocks next. */
    private static void moves(UiCanvas c, Model model, int x, int y) {
        GraftStage stage = model.kakuja() ? GraftStage.KAKUJA : model.stage();
        c.text("MOVES  ·  " + stage.displayName().toUpperCase(java.util.Locale.ROOT), x, y, HEADER, false);
        int rowY = y + 20;
        String[] keys = {"M1", "RMB", "1", "2", "3", "4", "5", "6", "7", "8"};
        GraftMove.Slot[] slots = {GraftMove.Slot.M1, GraftMove.Slot.M2, GraftMove.Slot.Z, GraftMove.Slot.J, GraftMove.Slot.V, GraftMove.Slot.B,
            GraftMove.Slot.N, GraftMove.Slot.H, GraftMove.Slot.G, GraftMove.Slot.C};
        for (int i = 0; i < slots.length; i++) {
            GraftMove move = GraftMove.of(slots[i], stage);
            if (move == GraftMove.NONE) {
                continue;
            }
            boolean open = model.mastery() >= move.mastery();
            c.text(keys[i], x, rowY, open ? LABEL : FAINT, false);
            c.text(move.displayName(), x + 40, rowY, open ? VALUE : FAINT, false);
            c.text(open ? (move.cooldown() > 0 ? (move.cooldown() / 20.0 + " s") : "") : Integer.toString(move.mastery()), x + 290, rowY, open ? TRAIT : FAINT,
                false);
            rowY += 16;
        }
        c.text("A blade or an axe in hand: M1 is your weapon,", x, rowY + 6, MUTED, false);
        c.text("and the kagune pierces what it cut", x, rowY + 20, MUTED, false);

        c.text("UNLOCK TRACK", x, rowY + 46, HEADER, false);
        int trackY = rowY + 64;
        int shown = 0;
        List<GraftMove> track = GraftMove.track();
        for (GraftMove move : track) {
            if (move.mastery() <= model.mastery() || shown >= 6) {
                continue;
            }
            c.text(move.mastery() + "", x, trackY, FAINT, false);
            c.text(move.displayName() + "  (" + move.stage().displayName() + ")", x + 40, trackY, MUTED, false);
            trackY += 15;
            shown++;
        }
        if (shown == 0) {
            c.text("Every move is yours", x, trackY, TRAIT, false);
        }
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

    private static String next(int mastery) {
        for (GraftStage stage : GraftStage.values()) {
            if (!stage.unlocked(mastery)) {
                return "Next: " + stage.displayName() + " at " + stage.mastery();
            }
        }
        return "Every stage is yours";
    }

    private static String tooltip(Map<String, UkakuMenuView.Rect> hits, double x, double y, Model model) {
        for (Map.Entry<String, UkakuMenuView.Rect> entry : hits.entrySet()) {
            if (!entry.getValue().contains(x, y)) {
                continue;
            }
            String key = entry.getKey();
            if (key.startsWith("stage:")) {
                GraftStage stage = GraftStage.fromId(key.substring(6));
                if (stage == null) {
                    return null;
                }
                return stage.unlocked(model.mastery()) ? stage.displayName() + ": " + stage.limbs(model.mastery()) + " limbs"
                    : "Unlocks at mastery " + stage.mastery();
            }
            if (key.equals("eyes")) {
                return "Place the kakugan on your skin's right eye";
            }
            if (key.startsWith("mutation:")) {
                GraftMutation mutation = GraftMutation.fromId(key.substring(9));
                return mutation == null ? null : mutation.description();
            }
        }
        return null;
    }
}
