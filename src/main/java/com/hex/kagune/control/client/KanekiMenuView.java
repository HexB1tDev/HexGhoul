package com.hex.kagune.control.client;

import com.hex.kagune.control.KanekiForms;
import com.hex.kagune.control.KanekiUnlocks;
import com.hex.kagune.control.client.ui.UiCanvas;
import com.hex.kagune.control.king.KingForm;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.MutationNode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 6.0.0: the Kaneki path's kagune menu (the menu key), drawn like Ayato's ({@link AyatoMenuView}: the same organic board, 960 x
 * 520 scaled to the window) in Kaneki's crimson over black: his organ and mastery, his forms to pick (the half kakujas in violet),
 * his RC and what K does, the kakugan placement, and (1.0.41, in place of the mutation tree, which the path no longer has) its one
 * passive, You Thought You'd Get Rid of Me That Easily?, opened by its quest. 1.0.10: once the final kakuja is chosen the whole
 * board turns to it ({@link KingTheme}): pure black with red veins for War, white with feathers for Peace.
 */
public final class KanekiMenuView {
    public static final int UI_WIDTH = AyatoMenuView.UI_WIDTH;
    public static final int UI_HEIGHT = AyatoMenuView.UI_HEIGHT;

    /** What the menu shows; {@code pending} is a mutation clicked once (a second click takes it). */
    public record Model(int mastery, boolean active, KaguneForm form, float energy, float maxEnergy, long mutations, MutationNode pending,
                        QuestLocks quests, King king) {
        public Model(int mastery, boolean active, KaguneForm form, float energy, float maxEnergy, long mutations, MutationNode pending) {
            this(mastery, active, form, energy, maxEnergy, mutations, pending, QuestLocks.ALL, King.ASLEEP);
        }

        public Model(int mastery, boolean active, KaguneForm form, float energy, float maxEnergy, long mutations, MutationNode pending,
                     QuestLocks quests) {
            this(mastery, active, form, energy, maxEnergy, mutations, pending, quests, King.ASLEEP);
        }
    }

    /**
     * 1.0.5: the final kakuja: has the Dragon been through him ({@code awake}), what he chose, what he wears. 1.0.10: chosen on the
     * cards ({@code KingChoiceScreen}), never here; {@code pending} stays for the old saves of the menu's model.
     */
    public record King(boolean awake, KingForm chosen, KingForm worn, KingForm pending) {
        public static final King ASLEEP = new King(false, KingForm.NONE, KingForm.NONE, KingForm.NONE);
    }

    private KanekiMenuView() {
    }

    /** Draws the board and returns its clickable areas: "eyes", "form:ID", "passive", "king:ID". */
    public static Map<String, AyatoMenuView.Rect> draw(UiCanvas c, Model model, double mouseX, double mouseY, double time) {
        KingTheme t = KingTheme.of(model.king().chosen());
        Map<String, AyatoMenuView.Rect> hits = new LinkedHashMap<>();
        int panelWidth = 930;
        int panelHeight = 500;
        int centerX = UI_WIDTH / 2;
        int centerY = UI_HEIGHT / 2;
        int left = centerX - panelWidth / 2;
        int top = centerY - panelHeight / 2;
        c.batch(() -> {
            AyatoMenuView.organicPanel(c, centerX, centerY, panelWidth / 2, panelHeight / 2, t.panelFill(), t.panelEdge());
            AyatoMenuView.organicPanel(c, centerX, centerY, panelWidth / 2 - 5, panelHeight / 2 - 5, t.innerFill(), t.innerEdge());
            if (t.feathers()) {
                KingTheme.feathers(c, left + 30, top + 18, panelWidth - 60, panelHeight - 36, t.vein(), t.veinBright(), time);
            }
            if (t.veins()) {
                KingTheme.veins(c, left + 20, top + 14, panelWidth - 40, panelHeight - 28, t.vein(), t.veinBright(), time);
            }
            AyatoMenuView.drawVein(c, left + 262, top + 44, left + 280, centerY, left + 250, top + panelHeight - 45, t.vein(), t.feathers() ? 1 : 3);
            AyatoMenuView.drawVein(c, left + 535, top + 42, left + 520, centerY, left + 550, top + panelHeight - 42, t.vein(), t.feathers() ? 1 : 3);
            AyatoMenuView.drawVein(c, left + 18, top + 74, centerX, top + 32, left + panelWidth - 18, top + 74, t.veinBright(), 2);
        });
        KingForm chosen = model.king().chosen();
        c.centered(chosen == KingForm.WAR ? "W A R   ·   T H E   O N E - E Y E D   K I N G" : chosen == KingForm.PEACE ? "P E A C E   ·   K A N E K I   : R E"
            : "R I N K A K U   E V O L U T I O N", centerX, top + 18, t.title(), !t.feathers());
        c.centered("RC CELL / CENTIPEDE MATRIX", centerX, top + 34, t.subtitle(), !t.feathers());
        c.centered("M1 tails  •  J grapple  •  G grab  •  H crush  •  K half kakuja  •  hold K final", centerX, top + 51, t.hint(), !t.feathers());

        identity(c, t, model, hits, mouseX, mouseY, left + 24, top + 68);
        middle(c, t, model, hits, mouseX, mouseY, left + 295, top + 76, time);
        mutations(c, t, model, hits, mouseX, mouseY, left + 566, top + 70);

        c.centered(model.active() ? "R  RETRACT" : "R  MANIFEST", centerX, top + panelHeight - 16, model.active() ? t.on() : t.off(), !t.feathers());

        String tip = tooltip(hits, mouseX, mouseY, model);
        if (tip != null) {
            int width = c.width(tip) + 8;
            int x = (int)Math.min(UI_WIDTH - width - 4, mouseX + 10);
            int y = (int)Math.max(4, mouseY - 16);
            c.fill(x - 1, y - 1, x + width + 1, y + 13, t.panelEdge());
            c.fill(x, y, x + width, y + 12, t.tipFill());
            c.text(tip, x + 4, y + 2, t.value(), false);
        }
        return hits;
    }

    private static void identity(UiCanvas c, KingTheme t, Model model, Map<String, AyatoMenuView.Rect> hits, double mouseX, double mouseY, int x, int y) {
        int mastery = model.mastery();
        KaguneForm form = model.form();
        c.text("CURRENT ORGAN", x, y, t.header(), false);
        row(c, t, "Type", "Rinkaku", x, y + 25);
        row(c, t, "Silhouette", "Tails from the lower back", x, y + 43);
        row(c, t, "Form", form.displayName(), x, y + 61);
        row(c, t, "Limbs", Integer.toString(form.limbCount()), x, y + 79);

        c.text("MASTERY", x, y + 109, t.header(), false);
        c.text(mastery + " / 1000", x + 126, y + 109, t.value(), false);
        c.batch(() -> {
            AyatoMenuView.capsule(c, x, y + 128, 205, 8, t.barBack());
            AyatoMenuView.capsule(c, x, y + 128, Math.max(8, Math.round(205.0F * Math.min(1000, mastery) / 1000.0F)), 8, t.bar());
        });
        c.text(nextForm(mastery), x, y + 142, t.muted(), false);

        c.text("FORMS", x, y + 164, t.header(), false);
        int rowY = y + 182;
        for (KaguneForm option : KanekiForms.forms()) {
            // 7.0.1: a form past its mastery may still wait for its quest.
            boolean quest = option.isUnlocked(mastery) && !model.quests().passes(com.hex.kagune.control.quest.Gates.kaneki(option));
            boolean unlocked = option.isUnlocked(mastery) && !quest;
            boolean selected = option == form;
            AyatoMenuView.Rect rect = new AyatoMenuView.Rect(x, rowY - 3, 220, 16);
            boolean hover = rect.contains(mouseX, mouseY);
            int fill = t == KingTheme.KANEKI ? KanekiForms.buttonColor(selected ? t.bar() : hover && unlocked ? t.buttonHover() : t.button(), option, selected,
                unlocked) : selected ? t.bar() : hover && unlocked ? t.buttonHover() : t.button();
            c.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), fill);
            int label = t == KingTheme.KANEKI ? KanekiForms.labelColor(unlocked ? t.value() : t.faint(), option, unlocked)
                : selected && t.feathers() ? 0xFFFFFFFF : unlocked ? t.value() : t.faint();
            // 1.0.10: Kaneki's two kakujas are his half kakujas now; the final kakuja is War or Peace.
            String name = KanekiForms.kakuja(option) ? option.displayName() + " (half)" : option.displayName();
            c.text(name, x + 6, rowY + 1, label, false);
            c.text(unlocked ? (selected ? "worn" : "") : quest ? "quest" : option.requiredMastery() + "", x + 186 - (quest ? 6 : 0), rowY + 1,
                unlocked ? (selected && t.feathers() ? 0xFFFFFFFF : t.trait()) : quest ? t.quest() : t.faint(), false);
            hits.put("form:" + option.id(), rect);
            rowY += 19;
        }
        // 1.0.5: the final kakuja, after the Dragon (1.0.10: chosen once on its cards; K held puts it on).
        c.text("FINAL KAKUJA", x, rowY + 5, t.header(), false);
        c.text("hold K", x + 220 - c.width("hold K"), rowY + 5, t.label(), false);
        rowY += 23;
        King king = model.king();
        for (KingForm option : new KingForm[] {KingForm.WAR, KingForm.PEACE}) {
            boolean mine = king.chosen() == option;
            boolean open = king.awake() && king.chosen() == KingForm.NONE && option.built();
            boolean worn = king.worn() == option;
            AyatoMenuView.Rect rect = new AyatoMenuView.Rect(x, rowY - 3, 220, 16);
            boolean hover = rect.contains(mouseX, mouseY);
            int fill = worn ? t.bar() : mine ? t.buttonHover() : hover && open ? t.buttonHover() : t.button();
            c.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), fill);
            c.text(option.displayName(), x + 6, rowY + 1, worn && t.feathers() ? 0xFFFFFFFF : mine || open ? t.value() : t.faint(), false);
            String state;
            int colour;
            if (!king.awake()) {
                state = "dragon";
                colour = t.quest();
            } else if (mine) {
                state = worn ? "worn" : "yours";
                colour = worn && t.feathers() ? 0xFFFFFFFF : t.trait();
            } else if (!option.built()) {
                state = "sealed";
                colour = t.faint();
            } else if (open) {
                state = "choose";
                colour = t.available();
            } else {
                state = "forsaken";
                colour = t.faint();
            }
            c.text(state, x + 220 - 6 - c.width(state), rowY + 1, colour, false);
            hits.put("king:" + option.id(), rect);
            rowY += 19;
        }
    }

    private static void middle(UiCanvas c, KingTheme t, Model model, Map<String, AyatoMenuView.Rect> hits, double mouseX, double mouseY, int x, int y,
                               double time) {
        c.text("RC", x, y, t.header(), false);
        float share = model.maxEnergy() <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, model.energy() / model.maxEnergy()));
        c.text(Math.round(model.energy()) + " / " + Math.round(model.maxEnergy()), x + 126, y, t.value(), false);
        c.batch(() -> {
            AyatoMenuView.capsule(c, x, y + 19, 205, 8, t.barBack());
            AyatoMenuView.capsule(c, x, y + 19, Math.max(8, Math.round(205.0F * share)), 8, t.rc());
        });
        c.text("At 0 the kagune goes in for 10 s", x, y + 33, t.muted(), false);

        // 1.0.10: K: a tap for the strongest half kakuja, a hold for the final kakuja.
        c.text("K", x, y + 60, t.header(), false);
        row(c, t, "K tap", "Strongest half kakuja", x, y + 80);
        row(c, t, "K hold", model.king().chosen() == KingForm.NONE ? "Final kakuja (after the Dragon)" : model.king().chosen().displayName(), x, y + 98);
        c.text("Half kakujas: Centipede (750), Full Kakuja (1000)", x, y + 122, t.trait(), false);
        c.text("Full Kakuja, sprint: vanish into the wind 5 s", x, y + 138, t.muted(), false);
        c.text("(attacking ends it; 30 s cooldown)", x, y + 152, t.faint(), false);
        c.text("Right click: dash (S held: back)", x, y + 168, t.muted(), false);

        button(c, t, hits, "eyes", "ADJUST KAKUGAN", x, y + 200, 220, mouseX, mouseY, time);

        // 6.0.4: his tendril moves unlock with mastery (KanekiUnlocks), as Ayato's do.
        c.text("UNLOCKS", x, y + 236, t.header(), false);
        int mastery = model.mastery();
        String[][] moves = {{"J", "Grapple"}, {"Z", "Rapid Strike"}, {"G  N", "Grab  ·  Throw"}, {"H", "Crush"}, {"V", "Lift"}, {"B", "Slam"}};
        int[] needs = {KanekiUnlocks.GRAPPLE, KanekiUnlocks.RAPID_STRIKE, KanekiUnlocks.GRAB, KanekiUnlocks.CRUSH, KanekiUnlocks.LIFT, KanekiUnlocks.SLAM};
        for (int i = 0; i < moves.length; i++) {
            boolean open = KanekiUnlocks.unlocked(mastery, needs[i]);
            int rowY = y + 254 + i * 16;
            c.text(moves[i][0], x, rowY, open ? t.label() : t.faint(), false);
            c.text(moves[i][1], x + 40, rowY, open ? t.value() : t.faint(), false);
            c.text(open ? "ready" : Integer.toString(needs[i]), x + 186, rowY, open ? t.trait() : t.faint(), false);
        }
    }

    /**
     * 1.0.41: no mutations on the Kaneki path any more: in their place its one passive, You Thought You'd Get Rid of Me That
     * Easily?, which its quest alone gives.
     */
    private static void mutations(UiCanvas c, KingTheme t, Model model, Map<String, AyatoMenuView.Rect> hits, double mouseX, double mouseY, int x, int y) {
        c.text("PASSIVE", x, y, t.header(), false);
        com.hex.kagune.control.quest.Gate gate = com.hex.kagune.control.quest.Gate.KANEKI_NOT_THAT_EASILY;
        boolean owned = model.quests().passes(gate);
        int rowY = y + 20;
        AyatoMenuView.Rect rect = new AyatoMenuView.Rect(x, rowY - 3, 340, 15);
        if (rect.contains(mouseX, mouseY)) {
            c.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), t.button());
        }
        c.fill(x + 4, rowY + 1, x + 10, rowY + 7, owned ? t.owned() : t.locked());
        c.text(PASSIVE_NAME, x + 16, rowY, owned ? t.value() : t.faint(), false);
        hits.put("passive", rect);
        rowY += 18;
        for (String line : PASSIVE_TEXT) {
            c.text(line, x + 16, rowY, owned ? t.trait() : t.faint(), false);
            rowY += 13;
        }
        rowY += 6;
        c.text(owned ? "yours" : "quest: " + com.hex.kagune.control.quest.QuestBook.openersText(gate), x + 16, rowY, owned ? t.owned() : t.label(), false);
    }

    /** 1.0.41: the passive's name and what it does, as the menu shows them. */
    static final String PASSIVE_NAME = "You Thought You'd Get Rid of Me That Easily?";
    static final String[] PASSIVE_TEXT = {"Every 5 min, death blows you apart and", "everything forgets you; you knit back",
        "together, slowly, and stand up whole."};

    private static void button(UiCanvas c, KingTheme t, Map<String, AyatoMenuView.Rect> hits, String key, String label, int x, int y, int width,
                               double mouseX, double mouseY, double time) {
        AyatoMenuView.Rect rect = new AyatoMenuView.Rect(x, y, width, 20);
        boolean hover = rect.contains(mouseX, mouseY);
        int pulse = hover || t.feathers() ? 0 : (int)(10 * (0.5 + 0.5 * Math.sin(time * 2.4)));
        c.fill(x - 1, y - 1, x + width + 1, y + 21, t.panelEdge());
        c.fill(x, y, x + width, y + 20, hover ? t.buttonHover() : t.button() + (pulse << 16));
        c.centered(label, x + width / 2, y + 6, hover ? t.title() : t.value(), !t.feathers());
        hits.put(key, rect);
    }

    private static void row(UiCanvas c, KingTheme t, String label, String value, int x, int y) {
        c.text(label, x, y, t.label(), false);
        c.text(value, x + 90, y, t.value(), false);
    }

    private static String nextForm(int mastery) {
        for (KaguneForm form : KanekiForms.forms()) {
            if (!form.isUnlocked(mastery)) {
                return "Next: " + form.displayName() + " at " + form.requiredMastery();
            }
        }
        return "Every form is yours";
    }

    private static String tooltip(Map<String, AyatoMenuView.Rect> hits, double x, double y, Model model) {
        for (Map.Entry<String, AyatoMenuView.Rect> entry : hits.entrySet()) {
            if (!entry.getValue().contains(x, y)) {
                continue;
            }
            String key = entry.getKey();
            if (key.equals("passive")) {
                return "Furuta's revive: done its quest, the Kaneki path's for good";
            }
            if (key.startsWith("form:")) {
                KaguneForm form = KaguneForm.fromId(key.substring(5));
                if (form == null) {
                    return null;
                }
                if (KanekiForms.kakuja(form)) {
                    return form.isUnlocked(model.mastery()) ? form.displayName() + ": a half kakuja (K tap brings out the strongest)"
                        : "A half kakuja: unlocks at mastery " + form.requiredMastery();
                }
                return form.isUnlocked(model.mastery()) ? form.displayName() + ": " + form.limbCount() + " limbs" : "Unlocks at mastery " + form.requiredMastery();
            }
            if (key.equals("eyes")) {
                return "Place the kakugan on your skin's eyes";
            }
            if (key.startsWith("king:")) {
                KingForm form = KingForm.byId(key.substring(5));
                King king = model.king();
                if (!king.awake()) {
                    return "The final kakuja: after the Dragon (Kakuja V2)";
                }
                if (king.chosen() == KingForm.NONE) {
                    return form.built() ? "Click to choose War or Peace, once and for good" : form.displayName() + ": not yet. It is coming";
                }
                return king.chosen() == form ? form.displayName() + ": hold K to let it out, and again to take it off" : "You chose " + king.chosen().displayName();
            }
        }
        return null;
    }
}
