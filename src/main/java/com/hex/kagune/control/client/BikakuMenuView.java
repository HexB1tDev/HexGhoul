package com.hex.kagune.control.client;

import com.hex.kagune.control.bikaku.BikakuForm;
import com.hex.kagune.control.bikaku.BikakuMove;
import com.hex.kagune.control.bikaku.BikakuMutation;
import com.hex.kagune.control.bikaku.BikakuRules;
import com.hex.kagune.control.client.ui.UiCanvas;
import com.hex.kagune.control.quest.Gate;
import com.hex.kagune.control.quest.QuestLog;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 3.4.9: the Bikaku path's kagune menu (the menu key): the worm, mastery, its one bar (energy, healing, food), what its bites do, the
 * kakugan placement, its forms (3.6.0), its moves and its kakuja's (3.5.x), its mutations opened by quests (3.6.3).
 *
 * <p>4.1.59 (the owner: "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as interactable as it is,
 * Dialog, mastery menus ALL OF THEM"): the quest menu's box ({@link QuestFrame}), as the Ukaku and Rinkaku menus. Tabs FORMS, MOVES,
 * KAKUJA and MUTATIONS. Each a column of quest cards; the one picked shown on the right as a quest (its story, what it is in numbers, what
 * it waits for); a form taken only by its USE THIS FORM button; the road of every step at the foot. What the server asks of each (its
 * mastery and its quest: BikakuServer's gates) is what the menu says.
 */
public final class BikakuMenuView {
    /** The old board's virtual size (the screens that still scale by it). */
    public static final int UI_WIDTH = UkakuMenuView.UI_WIDTH;
    public static final int UI_HEIGHT = UkakuMenuView.UI_HEIGHT;
    /** 4.1.59: the box's size: the other mastery menus'. */
    public static final int BOX_WIDTH = UkakuMenuView.BOX_WIDTH;
    public static final int BOX_HEIGHT = UkakuMenuView.BOX_HEIGHT;
    static final int DETAIL_WIDTH = UkakuMenuView.DETAIL_WIDTH;
    static final int ROAD_Y = UkakuMenuView.ROAD_Y;
    static final int TILE_WIDTH = UkakuMenuView.TILE_WIDTH;
    static final int TILE_HEIGHT = UkakuMenuView.TILE_HEIGHT;
    /** The bar's colour once Potential Unlock is had (3.6.7: the owner: "Should be Purple, the bar, my bad"). */
    static final int PURPLE = 0xFF000000 | BikakuRules.POTENTIAL_PURPLE;

    /** 4.1.59: the menu's tabs. */
    public enum Tab {
        FORMS("FORMS"), MOVES("MOVES"), KAKUJA("KAKUJA"), MUTATIONS("MUTATIONS");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    /**
     * What the menu shows. 3.6.3: his mutations ({@code BikakuMutation} bits) and how long until Grand Devouring can feed again (ticks; 0
     * ready). 4.1.59: the quests' gates, the tab open and what is picked ("pick:f:NAME" a form, "pick:m:NAME" a move or "pick:m:M1",
     * "pick:k:NAME" the kakuja's or "pick:k:KAKUJA", "pick:u:NAME" a mutation, "step:N" a step of the road; null: the tab's first).
     */
    public record Model(int mastery, boolean active, float energy, float maxEnergy, int form, int mutations, long devourWait, QuestLocks quests, Tab tab,
                        String picked) {
        public Model {
            quests = quests == null ? QuestLocks.ALL : quests;
            tab = tab == null ? Tab.FORMS : tab;
        }

        /** As before 3.6.0 (the one worm). */
        public Model(int mastery, boolean active, float energy, float maxEnergy) {
            this(mastery, active, energy, maxEnergy, 0);
        }

        /** As before 3.6.3 (no mutations). */
        public Model(int mastery, boolean active, float energy, float maxEnergy, int form) {
            this(mastery, active, energy, maxEnergy, form, 0, 0L);
        }

        /** As before 4.1.59. */
        public Model(int mastery, boolean active, float energy, float maxEnergy, int form, int mutations, long devourWait) {
            this(mastery, active, energy, maxEnergy, form, mutations, devourWait, QuestLocks.ALL, Tab.FORMS, null);
        }
    }

    /** The plan's moves still to come (the owner's spec), each with the mastery it is meant to need (3.5.0: the built ones are moves). */
    static final Object[][] PLANNED = {
        {"Devouring Maw", BikakuRules.MAW}, {"Predatory Drag", BikakuRules.DRAG}, {"Ravenous Pursuit", BikakuRules.PURSUIT},
        {"Carrion Eruption", BikakuRules.ERUPTION}, {"Monstrous Regeneration", BikakuRules.REGENERATION}};

    /** The planned moves not built yet. */
    static List<Object[]> coming() {
        List<Object[]> out = new ArrayList<>();
        for (Object[] planned : PLANNED) {
            boolean built = false;
            for (BikakuMove move : BikakuMove.values()) {
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

    /**
     * Draws the box at its own size and returns its clickable areas: "tab:NAME", "pick:...", "step:N", "eyes", "formN" (USE THIS FORM, N
     * the form's ordinal: only for a form that may be taken and is not on).
     */
    public static Map<String, UkakuMenuView.Rect> draw(UiCanvas c, Model model, double mouseX, double mouseY, double time) {
        Map<String, UkakuMenuView.Rect> hits = new LinkedHashMap<>();
        QuestFrame.plate(c, BOX_WIDTH, BOX_HEIGHT, time, 292, 6, 170, 38);
        header(c, model, time);
        List<QuestFrame.Tab> tabs = new ArrayList<>();
        for (Tab tab : Tab.values()) {
            tabs.add(new QuestFrame.Tab(tab.name(), tab.label, 0, false));
        }
        QuestFrame.tabs(c, tabs, model.tab().name(), hits, mouseX, mouseY, QuestFrame.LIST_X, QuestFrame.TABS_Y, time);
        QuestFrame.Tip tip = switch (model.tab()) {
            case MOVES -> movesTab(c, model, hits, mouseX, mouseY, time, false);
            case KAKUJA -> movesTab(c, model, hits, mouseX, mouseY, time, true);
            case MUTATIONS -> mutationsTab(c, model, hits, mouseX, mouseY, time);
            default -> formsTab(c, model, hits, mouseX, mouseY, time);
        };
        QuestFrame.tooltip(c, tip, 8, BOX_WIDTH - 8);
        return hits;
    }

    // ------------------------------------------------------------------ the gates (as BikakuServer asks them)

    /** The quest a form waits for: more than one worm the Worms', The Walker its own too. */
    static boolean passes(Model model, BikakuForm form) {
        boolean worms = form.worms() <= 1 || model.quests().passes(Gate.BIKAKU_WORMS);
        return worms && (!form.walks() || model.quests().passes(Gate.BIKAKU_WALKER));
    }

    /** The gate a form waits for first (null: none). */
    static Gate gate(Model model, BikakuForm form) {
        if (form.worms() > 1 && !model.quests().passes(Gate.BIKAKU_WORMS)) {
            return Gate.BIKAKU_WORMS;
        }
        return form.walks() ? Gate.BIKAKU_WALKER : form.worms() > 1 ? Gate.BIKAKU_WORMS : null;
    }

    /** The gate a move waits for (BikakuServer: Pursuit, Eruption and Regeneration the Worms', the kakuja's its own; null: none). */
    static Gate gate(BikakuMove move) {
        return switch (move) {
            case PURSUIT, ERUPTION, REGENERATION -> Gate.BIKAKU_WORMS;
            case JAWS, HUNGER, CATACLYSM, UNDYING -> Gate.BIKAKU_KAKUJA;
            default -> null;
        };
    }

    // ------------------------------------------------------------------ the header

    /** The title, the worm and mastery; the bar to the next step; his one bar and the kagune out or in. */
    private static void header(UiCanvas c, Model model, double time) {
        int mastery = model.mastery();
        TreeMap<Integer, String> steps = steps();
        Integer next = steps.ceilingKey(mastery + 1);
        Integer from = steps.floorKey(mastery);
        int start = from == null ? 0 : from;
        float share = next == null ? 1.0F : (mastery - start) / (float)Math.max(1, next - start);
        String goal = next == null ? "Every step of the Bikaku reached" : mastery + " / " + next + "  ·  next: " + steps.get(next);
        QuestFrame.header(c, BOX_WIDTH, "BIKAKU", "THE WORM KAGUNE  ·  MASTERY " + mastery, share, next == null, goal, time);
        int right = BOX_WIDTH - 12;
        boolean out = model.active();
        right = QuestFrame.chipLeft(c, right, 56, out ? "KAGUNE OUT  ·  R RETRACTS" : "KAGUNE IN  ·  ALT + R MANIFESTS",
            out ? 0xFF2A0A10 : 0xFF181012, out ? QuestStyle.RED_DARK : QuestStyle.GREY_DARK, out ? 0xFFFF8A7A : QuestJournalView.MUTED);
        boolean purple = BikakuMutation.POTENTIAL_UNLOCK.in(model.mutations());
        QuestFrame.chipLeft(c, right, 56, "ENERGY " + Math.round(model.energy()) + " / " + Math.round(model.maxEnergy()), purple ? 0xFF1E0A2E
            : QuestFrame.RED_FILL, purple ? QuestStyle.mix(PURPLE, 0xFF000000, 0.4F) : QuestStyle.RED_DARK, purple ? 0xFFD8B8FF : QuestFrame.VALUE);
    }

    /** Every step of the Bikaku by its mastery: its forms, its moves, its kakuja, in words. */
    public static TreeMap<Integer, String> steps() {
        TreeMap<Integer, List<String>> parts = new TreeMap<>();
        for (BikakuForm form : BikakuForm.values()) {
            parts.computeIfAbsent(form.mastery(), k -> new ArrayList<>()).add(form.displayName() + (form.walks() ? " (half kakuja, tap K)" : ""));
        }
        for (BikakuMove move : BikakuMove.values()) {
            if (move != BikakuMove.NONE && !move.kakuja()) {
                parts.computeIfAbsent(move.mastery(), k -> new ArrayList<>()).add(move.displayName() + " (" + move.slot().name() + ")");
            }
        }
        parts.computeIfAbsent(BikakuRules.KAKUJA, k -> new ArrayList<>()).add("the Kakuja (hold K)");
        TreeMap<Integer, String> out = new TreeMap<>();
        parts.forEach((m, words) -> out.put(m, String.join(", ", words)));
        return out;
    }

    // ------------------------------------------------------------------ the forms' tab

    private static QuestFrame.Tip formsTab(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, double time) {
        BikakuForm picked = pickedForm(model);
        int y = QuestFrame.CONTENT_Y;
        int index = 0;
        for (BikakuForm form : BikakuForm.values()) {
            QuestFrame.card(c, formCard(model, form), QuestFrame.LIST_X, y, QuestFrame.LIST_WIDTH, form == picked, hits, mouseX, mouseY, time, index++);
            y += QuestFrame.CARD_HEIGHT + QuestFrame.CARD_GAP;
        }
        y = energy(c, model, QuestFrame.LIST_X, y + 6, QuestFrame.LIST_WIDTH);
        y = QuestFrame.section(c, "PREDATORY FEEDING", QuestFrame.LIST_X, y + 4, QuestFrame.LIST_WIDTH);
        QuestFrame.row(c, "A bite", "+" + half(BikakuRules.BITE_HEAL) + " heart", QuestFrame.LIST_X, y, 66, QuestFrame.LIST_WIDTH, QuestFrame.VALUE);
        QuestFrame.row(c, "A kill", "+" + half(BikakuRules.KILL_HEAL) + " hearts more", QuestFrame.LIST_X, y + 12, 66, QuestFrame.LIST_WIDTH,
            QuestFrame.VALUE);
        c.text("Bites make them bleed; the third stuns", QuestFrame.LIST_X, y + 26, QuestFrame.MUTED, false);
        int ky = QuestFrame.section(c, "KAKUGAN", QuestFrame.LIST_X, y + 42, QuestFrame.LIST_WIDTH);
        QuestFrame.button(c, "eyes", "ADJUST KAKUGAN", QuestFrame.LIST_X, ky, QuestFrame.LIST_WIDTH, 16, true, false, hits, mouseX, mouseY, time);
        formDetail(c, model, picked, hits, mouseX, mouseY, QuestFrame.DETAIL_X, QuestFrame.CONTENT_Y, DETAIL_WIDTH, time);
        return road(c, model, hits, mouseX, mouseY, QuestFrame.DETAIL_X, ROAD_Y, DETAIL_WIDTH, time, picked.mastery());
    }

    /** The form shown: the one picked (or the form a step of the road is in), else the one on. */
    static BikakuForm pickedForm(Model model) {
        String picked = model.picked();
        if (picked != null) {
            for (BikakuForm form : BikakuForm.values()) {
                if (picked.equals("pick:f:" + form.name())) {
                    return form;
                }
            }
            if (picked.startsWith("step:")) {
                try {
                    return formAt(Integer.parseInt(picked.substring(5)));
                } catch (NumberFormatException wrong) {
                    // the one on
                }
            }
        }
        return BikakuForm.byOrdinal(model.form());
    }

    /** The form a step at {@code mastery} belongs to (the last whose mastery it has reached). */
    static BikakuForm formAt(int mastery) {
        BikakuForm at = BikakuForm.ONE;
        for (BikakuForm form : BikakuForm.values()) {
            if (mastery >= form.mastery()) {
                at = form;
            }
        }
        return at;
    }

    /** How a form stands: on (open), had (done), waiting for its quest (closed), ahead (locked). */
    static QuestLog.State state(Model model, BikakuForm form) {
        if (!form.built() || !form.unlocked(model.mastery())) {
            return QuestLog.State.LOCKED;
        }
        if (!passes(model, form)) {
            return QuestLog.State.CLOSED;
        }
        return form == BikakuForm.byOrdinal(model.form()) ? QuestLog.State.OPEN : QuestLog.State.DONE;
    }

    /** True if {@code form} may be taken from here: had, its quest done, picked in the menu (The Walker is K tapped), not on. */
    static boolean takeable(Model model, BikakuForm form) {
        return state(model, form) == QuestLog.State.DONE && form.picked();
    }

    /** A form as a quest card: its worms (The Walker's legs), its name, a chip (ON, READY, TAP K, NEEDS ITS QUEST, "at mastery 500"). */
    static QuestFrame.Card formCard(Model model, BikakuForm form) {
        int mastery = model.mastery();
        QuestLog.State state = state(model, form);
        BikakuForm[] all = BikakuForm.values();
        int from = form.ordinal() == 0 ? 0 : all[form.ordinal() - 1].mastery();
        boolean reached = state != QuestLog.State.LOCKED;
        boolean next = !reached && mastery >= from;
        float share = reached ? 1.0F : next ? Math.max(0.0F, Math.min(1.0F, (mastery - from) / (float)Math.max(1, form.mastery() - from))) : 0.0F;
        String chip = switch (state) {
            case OPEN -> form.walks() ? "ON  ·  HALF KAKUJA" : "ON";
            case DONE -> form.walks() ? "TAP K  ·  HALF KAKUJA" : "READY";
            case CLOSED -> "NEEDS ITS QUEST";
            default -> form.built() ? "at mastery " + form.mastery() : "SOON";
        };
        return new QuestFrame.Card("pick:f:" + form.name(), form.displayName(), !reached || state == QuestLog.State.CLOSED, chip, QuestStyle.colour(state),
            QuestStyle.dark(state), next ? Math.round(share * 100.0F) + "%" : "", share, state == QuestLog.State.DONE ? QuestJournalView.BAR_DONE
            : QuestJournalView.BAR, next && share >= 0.8F, (cc, x, y, size, back, t) -> formGlyph(cc, form, state, x, y, size, back, t));
    }

    /** The one bar: energy, healing and food (purple with Potential Unlock). */
    private static int energy(UiCanvas c, Model model, int x, int y, int width) {
        float share = model.maxEnergy() <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, model.energy() / model.maxEnergy()));
        boolean purple = BikakuMutation.POTENTIAL_UNLOCK.in(model.mutations());
        y = QuestFrame.section(c, "ENERGY", Math.round(model.energy()) + " / " + Math.round(model.maxEnergy()), x, y, width);
        int barY = y + 3;
        c.batch(() -> {
            UkakuMenuView.capsule(c, x, barY, width, 3, QuestJournalView.BAR_BACK);
            UkakuMenuView.capsule(c, x, barY, Math.max(6, Math.round(width * share)), 3, purple ? PURPLE : QuestJournalView.BAR);
        });
        c.text(c.fit("One bar: energy, healing and food. Eat to fill it", width), x, y + 10, QuestFrame.MUTED, false);
        return y + 22;
    }

    /** A form, as the journal shows a quest: its story, the worms in a framed tile, what it is, while the worm is out; USE THIS FORM. */
    private static void formDetail(UiCanvas c, Model model, BikakuForm form, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY,
                                   int x, int y, int width, double time) {
        QuestLog.State state = state(model, form);
        title(c, form.displayName(), form.mastery() == 0 ? "From the start" : "Mastery " + form.mastery(), x, y, width);
        int rowY = y + 16;
        Gate gate = gate(model, form);
        String standing = switch (state) {
            case OPEN -> form.walks() ? "On now: tap K to come down" : "On now" + (model.active() ? ", out" : "");
            case DONE -> form.walks() ? "Yours: tap K to rise on it" : "Yours: take it below";
            case CLOSED -> "Opens with " + QuestFrame.opener(gate);
            default -> form.built() ? "Ahead of you: at mastery " + form.mastery() : "Coming";
        };
        c.text(c.fit(standing, width), x, rowY, state == QuestLog.State.OPEN ? QuestJournalView.OPEN : state == QuestLog.State.DONE ? QuestJournalView.DONE
            : QuestFrame.MUTED, false);
        rowY += 13;
        rowY = QuestFrame.story(c, story(form), x, rowY, width) + 8;
        boolean locked = state == QuestLog.State.LOCKED || state == QuestLog.State.CLOSED;
        int tileY = rowY;
        tile(c, state, x, tileY, time);
        PathChoiceLayout.Box box = new PathChoiceLayout.Box(x + 8, tileY + 6, TILE_WIDTH - 16, TILE_HEIGHT - 30);
        List<PathSilhouette.Mark> marks = PathSilhouette.bikakuForm(box, time, locked ? 0.0F : 1.0F, state == QuestLog.State.OPEN, form.worms(),
            form.walks(), 0x0C080E);
        c.batch(() -> {
            for (PathSilhouette.Mark mark : marks) {
                UkakuMenuView.draw(c, mark);
            }
        });
        veil(c, locked, x, tileY);
        c.centered(worms(form), x + TILE_WIDTH / 2, tileY + TILE_HEIGHT - 15, locked ? QuestFrame.FAINT : 0xFFB0767A, false);
        int rx = x + TILE_WIDTH + 12;
        int rw = width - TILE_WIDTH - 12;
        int ink = locked ? QuestFrame.MUTED : QuestFrame.VALUE;
        int ry = QuestFrame.section(c, "THE KAGUNE", rx, rowY, rw);
        QuestFrame.row(c, "Worms", worms(form), rx, ry, 58, rw, ink);
        QuestFrame.row(c, "Length", "About 8 blocks", rx, ry + 12, 58, rw, ink);
        QuestFrame.row(c, "Mouth", "Six fangs, three rings", rx, ry + 24, 58, rw, ink);
        ry += 42;
        ry = QuestFrame.section(c, "WHILE THE WORM IS OUT", rx, ry, rw);
        for (String line : UkakuMenuView.wrap(c, "Abnormal Regeneration: you heal far sooner and faster. Pain Suppression: hard to knock back, stuns"
            + " last half as long.", rw)) {
            c.text(line, rx, ry, QuestFrame.STORY, false);
            ry += 12;
        }
        rowY = Math.max(tileY + TILE_HEIGHT, ry) + 8;
        if (state == QuestLog.State.OPEN && form.picked()) {
            QuestFrame.stamp(c, x, rowY, "ON", QuestStyle.RED, QuestStyle.RED_DARK);
        } else {
            String label = switch (state) {
                case OPEN, DONE -> form.picked() ? "USE THIS FORM" : "TAP K";
                case CLOSED -> "NEEDS ITS QUEST";
                default -> form.built() ? "AT MASTERY " + form.mastery() : "SOON";
            };
            QuestFrame.button(c, "form" + form.ordinal(), label, x, rowY, 200, 22, takeable(model, form), false, hits, mouseX, mouseY, time);
            if (form.walks() && !locked) {
                c.text(c.fit(Math.round(BikakuRules.WALKER_COST) + " energy, then half its drain; carried " + com.hex.kagune.control.bikaku.WalkerLegs.HEIGHT
                    + " blocks up", width - 212), x + 212, rowY + 8, QuestFrame.MUTED, false);
            }
        }
    }

    /** A form's worms in words. */
    static String worms(BikakuForm form) {
        return form.walks() ? "3 worms, 2 legs, one root" : form.worms() == 1 ? "One worm, lower back" : form.worms() + " worms, one root";
    }

    /** What each form is, told as a quest's story. */
    static String story(BikakuForm form) {
        if (form.walks()) {
            return "Your half kakuja: three worms, and two walking tentacles out of the same root that carry you up off the ground and up walls; "
                + "your own legs hang still. Tap K.";
        }
        return switch (form.worms()) {
            case 1 -> "One worm out of your lower back, a mouth of fangs at its end. It bites, and every bite that lands heals you.";
            case 2 -> "Two worms out of the one root, each in its own lane: two mouths, and every move made by both.";
            default -> "Three worms out of the one root: every move made by all three, every bite three times over.";
        };
    }

    // ------------------------------------------------------------------ the moves' and the kakuja's tabs

    private static QuestFrame.Tip movesTab(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, double time,
                                           boolean kakuja) {
        String picked = pickedMove(model, kakuja);
        int y = QuestFrame.CONTENT_Y;
        int index = 0;
        List<String> keys = new ArrayList<>();
        keys.add(kakuja ? "KAKUJA" : "M1");
        for (BikakuMove move : BikakuMove.values()) {
            if (move != BikakuMove.NONE && move.kakuja() == kakuja) {
                keys.add(move.name());
            }
        }
        String prefix = kakuja ? "pick:k:" : "pick:m:";
        for (String key : keys) {
            QuestFrame.card(c, moveCard(model, key, prefix), QuestFrame.LIST_X, y, QuestFrame.LIST_WIDTH, picked.equals(key), hits, mouseX, mouseY, time,
                index++);
            y += QuestFrame.CARD_HEIGHT + QuestFrame.CARD_GAP;
        }
        int hy = QuestFrame.section(c, kakuja ? "THE KAKUJA" : "THE KEYS", QuestFrame.LIST_X, y + 6, QuestFrame.LIST_WIDTH);
        String note = kakuja ? "Hold K at mastery " + BikakuRules.KAKUJA + ": its moves take the worm's keys while it lasts."
            : "Your kagune keys (Controls). Hold M1 or a thrown move's key to throw it further: up to " + Math.round(BikakuRules.HELD_REACH)
            + " blocks after " + BikakuRules.CHARGE_FULL / 20 + " s (it is back slower).";
        for (String line : UkakuMenuView.wrap(c, note, QuestFrame.LIST_WIDTH)) {
            c.text(line, QuestFrame.LIST_X, hy, QuestFrame.MUTED, false);
            hy += 12;
        }
        moveDetail(c, model, picked, kakuja, QuestFrame.DETAIL_X, QuestFrame.CONTENT_Y, DETAIL_WIDTH);
        if (kakuja) {
            return null;
        }
        int at = picked.equals("M1") ? 0 : BikakuMove.valueOf(picked).mastery();
        return road(c, model, hits, mouseX, mouseY, QuestFrame.DETAIL_X, ROAD_Y, DETAIL_WIDTH, time, at);
    }

    /** The move shown on a tab ({@code kakuja}: the kakuja's): the one picked, else its first ("M1", "KAKUJA"). */
    static String pickedMove(Model model, boolean kakuja) {
        String prefix = kakuja ? "pick:k:" : "pick:m:";
        String picked = model.picked();
        if (picked != null && picked.startsWith(prefix)) {
            String name = picked.substring(prefix.length());
            if (name.equals(kakuja ? "KAKUJA" : "M1")) {
                return name;
            }
            for (BikakuMove move : BikakuMove.values()) {
                if (move != BikakuMove.NONE && move.kakuja() == kakuja && move.name().equals(name)) {
                    return name;
                }
            }
        }
        if (!kakuja && picked != null && picked.startsWith("step:")) {
            try {
                int at = Integer.parseInt(picked.substring(5));
                for (BikakuMove move : BikakuMove.values()) {
                    if (move != BikakuMove.NONE && !move.kakuja() && move.mastery() == at) {
                        return move.name();
                    }
                }
            } catch (NumberFormatException wrong) {
                // the first
            }
        }
        return kakuja ? "KAKUJA" : "M1";
    }

    /** How a move stands: had (done), waiting for its quest (closed), ahead (locked). The bites and the kakuja itself too. */
    static QuestLog.State moveState(Model model, String key) {
        if (key.equals("M1")) {
            return QuestLog.State.DONE;
        }
        if (key.equals("KAKUJA")) {
            return model.mastery() < BikakuRules.KAKUJA ? QuestLog.State.LOCKED : model.quests().passes(Gate.BIKAKU_KAKUJA) ? QuestLog.State.DONE
                : QuestLog.State.CLOSED;
        }
        BikakuMove move = BikakuMove.valueOf(key);
        if (!move.unlocked(model.mastery())) {
            return QuestLog.State.LOCKED;
        }
        return model.quests().passes(gate(move)) ? QuestLog.State.DONE : QuestLog.State.CLOSED;
    }

    /** A move (the bites, the kakuja) as a quest card: its glyph, its name, a chip (its key and cooldown, NEEDS ITS QUEST, "at mastery"). */
    static QuestFrame.Card moveCard(Model model, String key, String prefix) {
        QuestLog.State state = moveState(model, key);
        String name;
        String ready;
        int needs;
        if (key.equals("M1")) {
            name = "Bites: Snap, Lunge, Maul";
            ready = "M1  ·  A STRING OF THREE";
            needs = 0;
        } else if (key.equals("KAKUJA")) {
            name = "The Kakuja";
            ready = "HOLD K  ·  " + BikakuRules.KAKUJA_COOLDOWN / 20 + " S";
            needs = BikakuRules.KAKUJA;
        } else {
            BikakuMove move = BikakuMove.valueOf(key);
            name = move.displayName();
            ready = move.slot().name() + "  ·  " + move.cooldown() / 20 + " S";
            needs = move.mastery();
        }
        String chip = switch (state) {
            case DONE -> ready;
            case CLOSED -> "NEEDS ITS QUEST";
            default -> "at mastery " + needs;
        };
        int ink = state == QuestLog.State.DONE ? QuestStyle.GREEN : QuestStyle.colour(state);
        boolean dim = state != QuestLog.State.DONE;
        return new QuestFrame.Card(prefix + key, name, dim, chip, ink, QuestStyle.dark(state), "", -1.0F, 0, false, (cc, x, y, size, back, t) -> {
            moveGlyph(cc, key, dim ? QuestStyle.mix(ink, back, 0.45F) : ink, x, y, size, back, t);
            if (dim) {
                QuestStyle.lock(cc, x + size / 2, y + size / 2, size / 2, QuestStyle.mix(QuestStyle.GOLD, back, 0.2F), back);
            }
        });
    }

    /** A move, as the journal shows a quest: its story, then what it is in numbers in two columns; what it waits for. */
    private static void moveDetail(UiCanvas c, Model model, String key, boolean kakuja, int x, int y, int width) {
        QuestLog.State state = moveState(model, key);
        String name;
        String chip;
        String story;
        List<String[]> rows = new ArrayList<>();
        Gate gate;
        int needs;
        if (key.equals("M1")) {
            name = "Bites: Snap, Lunge, Maul";
            chip = "Key M1";
            story = "A string of three bites. Every bite that lands heals you, they make what they bite bleed, and the third, the Maul, stuns.";
            rows.add(new String[] {"Key", "M1 (left click)"});
            rows.add(new String[] {"A bite heals", "+" + half(BikakuRules.BITE_HEAL) + " heart"});
            rows.add(new String[] {"A kill heals", "+" + half(BikakuRules.KILL_HEAL) + " hearts more"});
            rows.add(new String[] {"Held", "thrown up to " + Math.round(BikakuRules.HELD_REACH) + " blocks"});
            gate = null;
            needs = 0;
        } else if (key.equals("KAKUJA")) {
            name = "The Kakuja";
            chip = "Hold K";
            story = "The whole worm becomes a rooted mass of mouths and tentacles. It regenerates, takes far less from every blow, and its tentacles "
                + "bite what comes at it on their own.";
            rows.add(new String[] {"Key", "K held"});
            rows.add(new String[] {"Costs", Math.round(BikakuRules.KAKUJA_COST) + " energy, then it drains"});
            rows.add(new String[] {"Comes back", "after " + BikakuRules.KAKUJA_COOLDOWN / 20 + " s"});
            rows.add(new String[] {"Takes", Math.round((1.0F - BikakuRules.KAKUJA_TAKEN) * 100) + "% less from every blow"});
            rows.add(new String[] {"Bites", Math.round(BikakuRules.KAKUJA_REACH) + " blocks out round it"});
            rows.add(new String[] {"Stands", "rooted, regenerating"});
            gate = Gate.BIKAKU_KAKUJA;
            needs = BikakuRules.KAKUJA;
        } else {
            BikakuMove move = BikakuMove.valueOf(key);
            name = move.displayName();
            chip = "Key " + move.slot().name();
            story = story(move);
            rows.add(new String[] {"Key", move.slot().name() + (move.held() ? " (hold to throw it further)" : "")});
            rows.add(new String[] {"Cooldown", move.cooldown() / 20 + " s"});
            rows.add(new String[] {"Energy", move.cost() <= 0.0F ? "none: its cooldown is its price" : Math.round(move.cost()) + ""});
            if (move.baseReach() > 0.0) {
                rows.add(new String[] {"Reach", Math.round(move.baseReach()) + " blocks"});
            }
            rows.add(new String[] {"Lasts", String.format(java.util.Locale.ROOT, "%.1f s", move.ticks() / 20.0)});
            gate = gate(move);
            needs = move.mastery();
        }
        title(c, name, chip, x, y, width);
        int rowY = y + 16;
        String standing = switch (state) {
            case DONE -> "Yours";
            case CLOSED -> "Opens with " + QuestFrame.opener(gate);
            default -> "Ahead of you: at mastery " + needs;
        };
        c.text(c.fit(standing, width), x, rowY, state == QuestLog.State.DONE ? QuestJournalView.DONE : QuestFrame.MUTED, false);
        rowY += 13;
        rowY = QuestFrame.story(c, story, x, rowY, width) + 10;
        rowY = QuestFrame.section(c, kakuja ? (key.equals("KAKUJA") ? "THE KAKUJA" : "IN THE KAKUJA") : "THE MOVE", x, rowY, width);
        int half = (width - 16) / 2;
        for (int i = 0; i < rows.size(); i++) {
            int col = i % 2;
            int cy = rowY + (i / 2) * 13;
            QuestFrame.row(c, rows.get(i)[0], rows.get(i)[1], x + col * (half + 16), cy, 82, half, state == QuestLog.State.DONE ? QuestFrame.VALUE
                : QuestFrame.MUTED);
        }
        rowY += ((rows.size() + 1) / 2) * 13 + 10;
        if (state != QuestLog.State.DONE) {
            int ly = rowY;
            c.batch(() -> QuestStyle.lock(c, x, ly, 12, QuestStyle.GOLD, QuestJournalView.INNER));
            c.text(c.fit(state == QuestLog.State.CLOSED ? "Waits for " + QuestFrame.opener(gate) : "Waits for mastery " + needs + "  ("
                + (needs - model.mastery()) + " to go)", width - 20), x + 17, rowY + 3, QuestFrame.FAINT, false);
        }
    }

    /** What each move does, told as a quest's story (its own words, short). */
    static String story(BikakuMove move) {
        return switch (move) {
            case MAW -> "Thrown far out at what you aim at, its mouth opened wide and snapped shut round it: pulled up before you, shaken, then flung "
                + "away. Bleed, a brief stun, a little healing.";
            case DRAG -> "Along the ground to what you aim at; it bites on and drags it back across the ground to you, lifts it high and slams it "
                + "down. A grab and a slam: stun, bleed.";
            case PURSUIT -> "The worm splits into three thinner mouths out of the same root, each going after its own body in fast, snaking "
                + "lunges; against one, all three bite it in turn.";
            case ERUPTION -> "Driven into the ground, it tunnels under the surface and bursts up under what you aim at, jaws wide, biting it and "
                + "throwing it into the air.";
            case REGENERATION -> "For a while your body regenerates far faster than any ghoul's, strands of kagune knitting it back. Not "
                + "invincible; it bites and moves on meanwhile.";
            case JAWS -> "Every mouthed tentacle thrown out at the bodies round it, each biting again and again on its own rhythm; the mouths of "
                + "its body snap at whatever is close.";
            case HUNGER -> "Three mouths and a coiling tentacle go after one body; held, it is bitten from every side, then pulled into the great "
                + "maw for a crushing last bite. Every bite heals you.";
            case CATACLYSM -> "The whole mass draws in on itself, then bursts out, tentacles flung out straight and swept round it while every "
                + "mouth roars: what is close is hurled away.";
            case UNDYING -> "For a while it regenerates at an extraordinary rate and takes still less from every blow; a heavy blow slows it a "
                + "moment, never stops it.";
            default -> "";
        };
    }

    // ------------------------------------------------------------------ the mutations' tab

    private static QuestFrame.Tip mutationsTab(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, double time) {
        BikakuMutation picked = BikakuMutation.values()[0];
        if (model.picked() != null) {
            for (BikakuMutation mutation : BikakuMutation.values()) {
                if (model.picked().equals("pick:u:" + mutation.name())) {
                    picked = mutation;
                }
            }
        }
        int y = QuestFrame.CONTENT_Y;
        int index = 0;
        for (BikakuMutation mutation : BikakuMutation.values()) {
            QuestFrame.card(c, mutationCard(model, mutation), QuestFrame.LIST_X, y, QuestFrame.LIST_WIDTH, mutation == picked, hits, mouseX, mouseY, time,
                index++);
            y += QuestFrame.CARD_HEIGHT + QuestFrame.CARD_GAP;
        }
        int ny = QuestFrame.section(c, "HOW THEY OPEN", QuestFrame.LIST_X, y + 6, QuestFrame.LIST_WIDTH);
        for (String line : UkakuMenuView.wrap(c, "Not by mastery: each is opened by its quest, and is yours for good, on whenever it can be.",
            QuestFrame.LIST_WIDTH)) {
            c.text(line, QuestFrame.LIST_X, ny, QuestFrame.MUTED, false);
            ny += 12;
        }
        mutationDetail(c, model, picked, QuestFrame.DETAIL_X, QuestFrame.CONTENT_Y, DETAIL_WIDTH);
        return null;
    }

    /** The gate that opens a mutation. */
    static Gate gate(BikakuMutation mutation) {
        return mutation == BikakuMutation.GRAND_DEVOURING ? Gate.BIKAKU_DEVOURING : Gate.BIKAKU_POTENTIAL;
    }

    /** A mutation as a card: ON (Grand Devouring's wait), NEEDS ITS QUEST, SOON. */
    static QuestFrame.Card mutationCard(Model model, BikakuMutation mutation) {
        boolean owned = mutation.in(model.mutations());
        int ink = owned ? QuestStyle.GREEN : QuestStyle.colour(QuestLog.State.LOCKED);
        int dark = owned ? QuestStyle.GREEN_DARK : QuestStyle.GREY_DARK;
        String chip = !mutation.built() ? "SOON" : owned ? devourTag(mutation, model.devourWait()) : "NEEDS ITS QUEST";
        return new QuestFrame.Card("pick:u:" + mutation.name(), mutation.displayName(), !owned, chip, ink, dark, "", -1.0F, 0, false,
            (cc, x, y, size, back, t) -> {
                mutationGlyph(cc, mutation, owned ? ink : QuestStyle.mix(ink, back, 0.45F), x, y, size, back, t);
                if (!owned) {
                    QuestStyle.lock(cc, x + size / 2, y + size / 2, size / 2, QuestStyle.mix(QuestStyle.GOLD, back, 0.2F), back);
                }
            });
    }

    /** A mutation, as a quest: what it does, what it changes, and its quest (or ON once it is his). */
    private static void mutationDetail(UiCanvas c, Model model, BikakuMutation mutation, int x, int y, int width) {
        boolean owned = mutation.in(model.mutations());
        Gate gate = gate(mutation);
        title(c, mutation.displayName(), "Mutation", x, y, width);
        int rowY = y + 16;
        String standing = owned ? (mutation == BikakuMutation.GRAND_DEVOURING && model.devourWait() > 0L ? "Yours: it can feed again in "
            + (model.devourWait() + 19L) / 20L + " s" : "Yours, on whenever it can be") : "Opens with " + QuestFrame.opener(gate);
        c.text(c.fit(standing, width), x, rowY, owned ? QuestJournalView.DONE : QuestJournalView.OPEN, false);
        rowY += 13;
        rowY = QuestFrame.story(c, mutation.description(), x, rowY, width) + 10;
        rowY = QuestFrame.section(c, "WITH IT", owned ? "yours now" : "now, then with it", x, rowY, width);
        String[][] rows = mutation == BikakuMutation.GRAND_DEVOURING
            ? new String[][] {{"A body at 1 heart", "you bite it", "a worm swallows it whole"}, {"Your bar", "as it is", "refilled"},
                {"How often", "never", "once a minute"}}
            : new String[][] {{"Energy", "x1", "x3"}, {"Your bar", "red", "purple"}, {"Torn apart", "you die", "you pull yourself back together"}};
        int after = owned ? QuestJournalView.DONE : QuestFrame.VALUE;
        for (String[] r : rows) {
            c.text(r[0], x, rowY, QuestFrame.MUTED, false);
            c.text(r[1], x + 136, rowY, QuestFrame.FAINT, false);
            int ax = x + 226;
            int ay = rowY + 4;
            c.batch(() -> {
                QuestStyle.line(c, ax, ay, ax + 12, ay, 1.6F, after);
                QuestStyle.line(c, ax + 8, ay - 3, ax + 12, ay, 1.6F, after);
                QuestStyle.line(c, ax + 8, ay + 3, ax + 12, ay, 1.6F, after);
            });
            c.text(c.fit(r[2], width - 246), x + 246, rowY, after, false);
            rowY += 13;
        }
        rowY += 10;
        if (owned) {
            QuestFrame.stamp(c, x, rowY, "ON", QuestStyle.GREEN, QuestStyle.GREEN_DARK);
        } else {
            int ly = rowY;
            c.batch(() -> QuestStyle.lock(c, x, ly, 12, QuestStyle.GOLD, QuestJournalView.INNER));
            c.text(c.fit("Waits for " + QuestFrame.opener(gate), width - 20), x + 17, rowY + 3, QuestFrame.FAINT, false);
        }
    }

    /** 3.6.3: a mutation he has: on, and Grand Devouring's wait until it can feed again. */
    static String devourTag(BikakuMutation mutation, long wait) {
        return mutation == BikakuMutation.GRAND_DEVOURING && wait > 0L ? "ON · " + (wait + 19L) / 20L + " s" : "ON";
    }

    // ------------------------------------------------------------------ shared pieces

    /** A detail's name large and a chip at its right. */
    private static void title(UiCanvas c, String name, String chip, int x, int y, int width) {
        c.push();
        c.translate(x, y);
        c.scale(1.25F);
        c.text(c.fit(name, Math.round((width - c.width(chip) - 30) / 1.25F)), 0, 0, QuestStyle.WHITE, true);
        c.pop();
        QuestStyle.chip(c, x + width - c.width(chip) - 12, y, chip, 0xFF1E0A0E, QuestStyle.RED_DARK, QuestFrame.MUTED);
    }

    /** A framed tile, its edge by how it stands. */
    private static void tile(UiCanvas c, QuestLog.State state, int x, int y, double time) {
        int edge = state == QuestLog.State.OPEN ? QuestStyle.mix(QuestStyle.RED_DARK, QuestStyle.RED, 0.5F + 0.5F * (float)Math.sin(time * 2.2))
            : state == QuestLog.State.DONE ? QuestStyle.GREEN_DARK : QuestStyle.TILE_EDGE;
        c.batch(() -> {
            QuestStyle.box(c, x, y, TILE_WIDTH, TILE_HEIGHT, QuestStyle.TILE, edge);
            c.gradient(x + 2, y + 2, x + TILE_WIDTH - 2, y + TILE_HEIGHT - 2, 0xFF12060A, 0xFF070305);
        });
    }

    /** A tile veiled with a padlock (ahead, or waiting for its quest). */
    private static void veil(UiCanvas c, boolean locked, int x, int y) {
        if (!locked) {
            return;
        }
        c.batch(() -> {
            c.fill(x + 2, y + 2, x + TILE_WIDTH - 2, y + TILE_HEIGHT - 2, 0xA0080306);
            QuestStyle.lock(c, x + TILE_WIDTH / 2 - 13, y + (TILE_HEIGHT - 26) / 2 - 12, 26, QuestStyle.mix(QuestStyle.GOLD, QuestStyle.TILE, 0.2F),
                0xFF0A0405);
        });
    }

    /** Every step of the Bikaku on the quest road (forms and moves): reached ticked, the next beating; the picked one's ringed. */
    private static QuestFrame.Tip road(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, int x, int y,
                                       int width, double time, int lit) {
        int mastery = model.mastery();
        y = QuestFrame.section(c, "YOUR ROAD", "a step opens what it brings", x, y, width);
        TreeMap<Integer, String> steps = steps();
        Integer next = steps.ceilingKey(mastery + 1);
        List<QuestFrame.Node> nodes = new ArrayList<>();
        int reached = -1;
        int i = 0;
        for (Map.Entry<Integer, String> step : steps.entrySet()) {
            boolean got = mastery >= step.getKey();
            reached = got ? i : reached;
            QuestLog.State state = got ? QuestLog.State.DONE : next != null && step.getKey().equals(next) ? QuestLog.State.OPEN : QuestLog.State.LOCKED;
            nodes.add(new QuestFrame.Node("step:" + step.getKey(), String.valueOf(step.getKey()), step.getValue() + "  ·  "
                + (got ? "reached" : "at mastery " + step.getKey()), state, step.getKey() == lit));
            i++;
        }
        return QuestFrame.road(c, nodes, reached, hits, mouseX, mouseY, x, y, width, 64, time);
    }

    private static String half(float health) {
        float hearts = health / 2.0F;
        return hearts == Math.round(hearts) ? Integer.toString(Math.round(hearts)) : Float.toString(hearts);
    }

    // ------------------------------------------------------------------ glyphs

    /** A worm: a thick coiling line up from the foot, its mouth a ring at the end. */
    static void worm(UiCanvas c, float x0, float y0, float lean, float s, int ink, int back) {
        float px = x0;
        float py = y0;
        for (int k = 1; k <= 8; k++) {
            float t = k / 8.0F;
            float nx = x0 + lean * s * 0.3F * t + (float)Math.sin(t * 6.0) * s * 0.08F;
            float ny = y0 - t * s * 0.66F;
            QuestStyle.line(c, px, py, nx, ny, Math.max(1.2F, s * (0.12F + 0.03F * t)), ink);
            px = nx;
            py = ny;
        }
        QuestStyle.circle(c, Math.round(px), Math.round(py), Math.max(2, Math.round(s * 0.15F)), ink);
        QuestStyle.circle(c, Math.round(px), Math.round(py), Math.max(1, Math.round(s * 0.07F)), back);
    }

    /** A form's glyph: its worms rising from one root (The Walker's two legs planted under them). */
    static void formGlyph(UiCanvas c, BikakuForm form, QuestLog.State state, int x, int y, int size, int back, double time) {
        int ink = QuestStyle.colour(state);
        int dim = state == QuestLog.State.LOCKED || state == QuestLog.State.CLOSED ? QuestStyle.mix(ink, back, 0.5F) : ink;
        float s = size;
        float rootX = x + s * 0.5F;
        float rootY = y + s * (form.walks() ? 0.66F : 0.92F);
        float[] leans = form.worms() <= 1 ? new float[] {0.4F} : form.worms() == 2 ? new float[] {-0.9F, 0.9F} : new float[] {-1.1F, 0.1F, 1.1F};
        for (float lean : leans) {
            worm(c, rootX, rootY, lean, form.walks() ? s * 0.8F : s, dim, back);
        }
        if (form.walks()) {
            QuestStyle.line(c, rootX, rootY, x + s * 0.12F, y + s * 0.98F, Math.max(1.4F, s * 0.1F), dim);
            QuestStyle.line(c, rootX, rootY, x + s * 0.88F, y + s * 0.98F, Math.max(1.4F, s * 0.1F), dim);
        }
        if (state == QuestLog.State.LOCKED || state == QuestLog.State.CLOSED) {
            QuestStyle.lock(c, x + size / 2, y + size / 2, size / 2, QuestStyle.mix(QuestStyle.GOLD, back, 0.2F), back);
        }
    }

    /** A move's glyph: its bites three marks, the Maw a mouth of fangs, the Drag an arrow back, the Pursuit three worms, and so on. */
    static void moveGlyph(UiCanvas c, String key, int ink, int x, int y, int size, int back, double time) {
        float s = size;
        int cx = x + size / 2;
        int cy = y + size / 2;
        switch (key) {
            case "M1" -> {
                for (int k = 0; k < 3; k++) {
                    float bx = x + s * (0.2F + k * 0.3F);
                    QuestStyle.line(c, bx - s * 0.08F, y + s * 0.25F, bx + s * 0.08F, y + s * 0.75F, Math.max(1.5F, s * 0.12F), ink);
                }
            }
            case "MAW", "HUNGER" -> {
                QuestStyle.circle(c, cx, cy, Math.round(s * 0.44F), ink);
                QuestStyle.circle(c, cx, cy, Math.round(s * 0.26F), back);
                for (int k = 0; k < 6; k++) {
                    double a = Math.PI * 2.0 * k / 6;
                    QuestStyle.line(c, cx + (float)Math.cos(a) * s * 0.28F, cy + (float)Math.sin(a) * s * 0.28F, cx + (float)Math.cos(a) * s * 0.12F,
                        cy + (float)Math.sin(a) * s * 0.12F, 1.2F, ink);
                }
            }
            case "DRAG" -> {
                QuestStyle.line(c, x + s * 0.12F, y + s * 0.8F, x + s * 0.9F, y + s * 0.8F, Math.max(1.4F, s * 0.1F), ink);
                QuestStyle.line(c, x + s * 0.12F, y + s * 0.8F, x + s * 0.34F, y + s * 0.62F, Math.max(1.4F, s * 0.1F), ink);
                worm(c, x + s * 0.7F, y + s * 0.78F, -1.0F, s * 0.8F, ink, back);
            }
            case "PURSUIT", "JAWS" -> {
                for (float lean : new float[] {-1.0F, 0.0F, 1.0F}) {
                    worm(c, cx, y + s * 0.95F, lean, s * 0.9F, ink, back);
                }
            }
            case "ERUPTION" -> {
                QuestStyle.line(c, x + s * 0.05F, y + s * 0.72F, x + s * 0.95F, y + s * 0.72F, Math.max(1.2F, s * 0.08F), ink);
                worm(c, cx, y + s * 0.72F, 0.2F, s * 0.9F, ink, back);
                QuestStyle.line(c, cx - s * 0.3F, y + s * 0.92F, cx - s * 0.16F, y + s * 0.78F, 1.2F, ink);
                QuestStyle.line(c, cx + s * 0.3F, y + s * 0.92F, cx + s * 0.16F, y + s * 0.78F, 1.2F, ink);
            }
            case "REGENERATION", "UNDYING" -> {
                QuestStyle.line(c, cx, y + s * 0.15F, cx, y + s * 0.85F, Math.max(2.0F, s * 0.22F), ink);
                QuestStyle.line(c, x + s * 0.15F, cy, x + s * 0.85F, cy, Math.max(2.0F, s * 0.22F), ink);
            }
            case "CATACLYSM" -> {
                for (int k = 0; k < 8; k++) {
                    double a = Math.PI * 2.0 * k / 8;
                    QuestStyle.line(c, cx + (float)Math.cos(a) * s * 0.16F, cy + (float)Math.sin(a) * s * 0.16F, cx + (float)Math.cos(a) * s * 0.46F,
                        cy + (float)Math.sin(a) * s * 0.46F, Math.max(1.2F, s * 0.1F), ink);
                }
                QuestStyle.circle(c, cx, cy, Math.max(1, Math.round(s * 0.12F)), ink);
            }
            default -> {
                // The kakuja itself: a mass with three mouths.
                QuestStyle.circle(c, cx, Math.round(y + s * 0.62F), Math.round(s * 0.34F), ink);
                for (int k = 0; k < 3; k++) {
                    int mx = Math.round(x + s * (0.25F + k * 0.25F));
                    int my = Math.round(y + s * (k == 1 ? 0.26F : 0.4F));
                    QuestStyle.circle(c, mx, my, Math.max(2, Math.round(s * 0.13F)), ink);
                    QuestStyle.circle(c, mx, my, Math.max(1, Math.round(s * 0.06F)), back);
                }
            }
        }
    }

    /** A mutation's glyph: Grand Devouring a great mouth over a small body, Potential Unlock three bars rising. */
    static void mutationGlyph(UiCanvas c, BikakuMutation mutation, int ink, int x, int y, int size, int back, double time) {
        float s = size;
        if (mutation == BikakuMutation.GRAND_DEVOURING) {
            QuestStyle.circle(c, x + size / 2, y + size / 2, Math.round(s * 0.46F), ink);
            QuestStyle.circle(c, x + size / 2, y + size / 2, Math.round(s * 0.3F), back);
            QuestStyle.circle(c, x + size / 2, y + size / 2, Math.max(1, Math.round(s * 0.12F)), ink);
            return;
        }
        for (int k = 0; k < 3; k++) {
            float bx = x + s * (0.12F + k * 0.3F);
            float top = y + s * (0.62F - k * 0.22F);
            c.fill(Math.round(bx), Math.round(top), Math.round(bx + s * 0.2F), Math.round(y + s * 0.9F), ink);
        }
    }
}
