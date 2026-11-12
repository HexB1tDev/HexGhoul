package com.hex.kagune.control.client;

import com.hex.kagune.control.client.ui.UiCanvas;
import com.hex.kagune.control.kakugan.KakuganEyes;
import com.hex.kagune.control.pale.ArmRules;
import com.hex.kagune.control.pale.OwlRules;
import com.hex.kagune.control.pale.PaleMutation;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.pale.PaleStrike;
import com.hex.kagune.control.pale.PaleUnlock;
import com.hex.kagune.control.pale.WatchRules;
import com.hex.kagune.control.quest.QuestLog;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The Pale path's kagune menu (the menu key; 6.1.0): Owl DNA's organ and stage, mastery and the track of what it brings, the pale flesh,
 * her moves, her one kakugan, her mutations (6.2.0) and her kakuja, the Owl.
 *
 * <p>4.1.61 (the owner: "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as interactable as it is,
 * Dialog, mastery menus ALL OF THEM"): the quest menu's box ({@link QuestFrame}), as the Ukaku, Rinkaku, Bikaku and Graft menus. Tabs BASE
 * FORM, MOVES, THE OWL and MUTATIONS, each a column of quest cards, the one picked shown on the right as a quest (its story, what it is
 * in numbers, what it waits for), the road of every step at the foot. A mutation is unlocked only by its UNLOCK button, drawn only while
 * the server would spend a point on it (PaleRevive.unlock: {@link PaleMutation#canUnlock}); Witness This's Instant Big Punch switch is
 * its own button.
 */
public final class PaleMenuView {
    /** The old board's virtual size (kept for what still scales by it). */
    public static final int UI_WIDTH = UkakuMenuView.UI_WIDTH;
    public static final int UI_HEIGHT = UkakuMenuView.UI_HEIGHT;
    /** 4.1.61: the box's size: the other mastery menus'. */
    public static final int BOX_WIDTH = UkakuMenuView.BOX_WIDTH;
    public static final int BOX_HEIGHT = UkakuMenuView.BOX_HEIGHT;
    static final int DETAIL_WIDTH = UkakuMenuView.DETAIL_WIDTH;
    static final int ROAD_Y = UkakuMenuView.ROAD_Y;
    static final int TILE_WIDTH = UkakuMenuView.TILE_WIDTH;
    static final int TILE_HEIGHT = UkakuMenuView.TILE_HEIGHT;
    /** Her kakugan's red, for what matters. */
    static final int EYE = 0xFFE8283C;

    /** 4.1.61: the menu's tabs. */
    public enum Tab {
        FORM("BASE FORM"), MOVES("MOVES"), OWL("THE OWL"), MUTATIONS("MUTATIONS");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    /**
     * What the menu shows: her mastery, whether her flesh is out, her eyes, her mutations (a bitmask of {@link PaleMutation}, with
     * {@link PaleMutation#INSTANT_PUNCH}), the ticks until her death that does not take is ready again (0 when it is); 4.1.61, the tab
     * open and what is picked ("pick:s:N" a stage, "pick:m:NAME" a move, "pick:o:NAME" the Owl or one of its moves, "pick:u:ID" a
     * mutation, "step:N" a step of the road; null: the tab's first).
     */
    public record Model(int mastery, boolean active, KakuganEyes eyes, int mutations, long reviveIn, Tab tab, String picked) {
        public Model {
            tab = tab == null ? Tab.FORM : tab;
        }

        public Model(int mastery, boolean active, KakuganEyes eyes) {
            this(mastery, active, eyes, 0, 0L);
        }

        /** As before 4.1.61. */
        public Model(int mastery, boolean active, KakuganEyes eyes, int mutations, long reviveIn) {
            this(mastery, active, eyes, mutations, reviveIn, Tab.FORM, null);
        }
    }

    /** Her base form's stages as it grows ({@link PaleUnlock#stageName}): the mastery each begins at. */
    static final int[] STAGES = {0, 250, 500};

    /**
     * One of her moves: its card's name, the key, the mastery it needs, its cooldown (ticks), its RC, its story; the Owl's own are
     * {@code owl}.
     */
    record Move(String id, String name, String key, int mastery, int cooldown, float cost, boolean owl, String story) {
    }

    static final List<Move> MOVES = List.of(
        new Move("BLITZ", "Speed Blitz", "M1", 0, 0, 0.0F, false,
            "Her fists faster than the eye: a string of strikes that grows with her, jab to liver shot, then the uppercut and the blinking "
                + "palm, then the spinning backfist and the twin palms to end it."),
        new Move("FLIPS", "Flips", "RMB", 0, PaleRules.FLIP_COOLDOWN, PaleRules.FLIP_COST, false,
            "Right click with W, A, S or D: she flips that way, over a blow or out of reach, and lands ready."),
        new Move("PUNCH", "Big Punch!", "Z", 0, PaleRules.PUNCH_COOLDOWN, 0.0F, false,
            "Z held, she winds up; let go, the blitz, the punch to the gut, and whatever it hits is sent flying."),
        new Move("SWIPE", "Flesh Swipe", "J", PaleRules.SWIPE_MASTERY, PaleRules.SWIPE_COOLDOWN, PaleRules.SWIPE_COST, false,
            "J tapped: the pale flesh erupts out of her upper back and sweeps across in front of her."),
        new Move("KICK", "Speed Kick", "V", PaleRules.KICK_MASTERY, PaleRules.KICK_COOLDOWN, PaleRules.KICK_COST, false,
            "V tapped: a kick too fast to see, there and gone."),
        new Move("GRAB", "Flesh Grab", "J held", PaleRules.GRAB_MASTERY, PaleRules.GRAB_COOLDOWN, PaleRules.GRAB_COST, false,
            "J held: the flesh reaches out for what she looks at and holds it while J stays down; let go, it is whipped round and thrown."),
        new Move("HAMMER", "Flesh Hammer", "V held", PaleRules.HAMMER_MASTERY, PaleRules.HAMMER_COOLDOWN, PaleRules.HAMMER_START_COST, false,
            "V held: her leg rises and a club of flesh grows round it; let go, the axe kick slams it into the ground."),
        new Move("WATCH", "Watch This", "B", PaleRules.WATCH_MASTERY, WatchRules.COOLDOWN, WatchRules.COST, false,
            "B: her arms thrown out in a cross, two tentacles burst out of her back and run through what is in front of her."),
        new Move("ARM", "Mutated Arm", "N", ArmRules.MASTERY, ArmRules.COOLDOWN, ArmRules.COST, false,
            "N: the arm swells, grabs and drags; held, it crushes everything round her in a sphere."));

    static final List<Move> OWL = List.of(
        new Move("OWL", "The Owl", "K", PaleRules.KAKUJA_V1_MASTERY, OwlRules.COOLDOWN, OwlRules.COST, true,
            "Her kakuja: a gaunt, hunched thing in a tattered robe, an owl's mask of bone with one burning eye, four limbs off her "
                + "shoulders ending in giant navy blades. K grows it; K again, she turns back."),
        new Move("REND", "Rend", "M1", PaleRules.KAKUJA_V1_MASTERY, 0, OwlRules.REND_COST[0], true,
            "Four cuts of the giant blades, one after another, the last the heaviest."),
        new Move("POUNCE", "Pounce", "RMB", PaleRules.KAKUJA_V1_MASTERY, OwlRules.LEAP_COOLDOWN, OwlRules.LEAP_COST, true,
            "A bound, and as she lands both blades are driven into the ground ahead of her."),
        new Move("OWL_PUNCH", "The Owl's Big Punch", "Z", PaleRules.KAKUJA_V1_MASTERY, PaleRules.PUNCH_COOLDOWN, 0.0F, true,
            "Z held, she sinks under the giant arm; let go, her fist and its elbow together, flying level through whatever is in the way."),
        new Move("OWL_KICK", "Speed Kick, Fissure", "V", PaleRules.KAKUJA_V1_MASTERY, PaleRules.KICK_COOLDOWN, PaleRules.KICK_COST, true,
            "V tapped: the dash, a blade through it, the spinning heel. V held: both blades raised high and brought down, the ground split."),
        new Move("WHIRL", "Whirl, Crystal Spikes", "J", PaleRules.KAKUJA_V1_MASTERY, PaleRules.SWIPE_COOLDOWN, OwlRules.WHIRL_COST, true,
            "J tapped: a whole turn with both giant arms out, reaping all round her. J held: fans of crystal spears off its crest that pin "
                + "what they hit."),
        new Move("LANCES", "Watch This", "B", PaleRules.KAKUJA_V1_MASTERY, WatchRules.COOLDOWN, WatchRules.COST, true,
            "Hunched, then reared up as its lances burst out of her back."),
        new Move("HAND", "Great Hand", "N", PaleRules.KAKUJA_V1_MASTERY, OwlRules.GRAB_COOLDOWN, OwlRules.GRAB_COST, true,
            "N: its great hand reaches for what she looks at and holds it up before its jaws while N stays down; let go, slammed into "
                + "the ground."));

    private PaleMenuView() {
    }

    /**
     * Draws the box at its own size and returns its clickable areas: "tab:NAME", "pick:...", "step:N", "milestone:N", "eyes",
     * "mutation:N" (UNLOCK, N the ordinal: only while a point may be spent on it) and "instant" (Witness This's switch, once it is hers).
     */
    public static Map<String, UkakuMenuView.Rect> draw(UiCanvas c, Model model, double mouseX, double mouseY, double time) {
        Map<String, UkakuMenuView.Rect> hits = new LinkedHashMap<>();
        QuestFrame.plate(c, BOX_WIDTH, BOX_HEIGHT, time, 292, 6, 170, 38);
        header(c, model, time);
        int points = PaleMutation.available(model.mastery(), model.mutations());
        List<QuestFrame.Tab> tabs = new ArrayList<>();
        for (Tab tab : Tab.values()) {
            tabs.add(new QuestFrame.Tab(tab.name(), tab.label, tab == Tab.MUTATIONS ? points : 0, tab == Tab.MUTATIONS && points > 0));
        }
        QuestFrame.tabs(c, tabs, model.tab().name(), hits, mouseX, mouseY, QuestFrame.LIST_X, QuestFrame.TABS_Y, time);
        QuestFrame.Tip tip = switch (model.tab()) {
            case MOVES -> movesTab(c, model, MOVES, "pick:m:", hits, mouseX, mouseY, time);
            case OWL -> movesTab(c, model, OWL, "pick:o:", hits, mouseX, mouseY, time);
            case MUTATIONS -> mutationsTab(c, model, hits, mouseX, mouseY, time);
            default -> formTab(c, model, hits, mouseX, mouseY, time);
        };
        QuestFrame.tooltip(c, tip, 8, BOX_WIDTH - 8);
        return hits;
    }

    // ------------------------------------------------------------------ the header

    private static void header(UiCanvas c, Model model, double time) {
        int mastery = model.mastery();
        TreeMap<Integer, String> steps = steps();
        Integer next = steps.ceilingKey(mastery + 1);
        Integer from = steps.floorKey(mastery);
        int start = from == null ? 0 : from;
        float share = next == null ? 1.0F : (mastery - start) / (float)Math.max(1, next - start);
        String goal = next == null ? "Every step of Owl DNA reached" : mastery + " / " + next + "  ·  next: " + steps.get(next);
        QuestFrame.header(c, BOX_WIDTH, "OWL DNA", "PALE FLESH  ·  " + PaleUnlock.stageName(mastery).toUpperCase(java.util.Locale.ROOT)
            + "  ·  MASTERY " + mastery, share, next == null, goal, time);
        int right = BOX_WIDTH - 12;
        boolean out = model.active();
        right = QuestFrame.chipLeft(c, right, 56, out ? "FLESH OUT  ·  R RETRACTS" : "FLESH IN  ·  CTRL + R", out ? 0xFF2A0A10 : 0xFF181012,
            out ? QuestStyle.RED_DARK : QuestStyle.GREY_DARK, out ? 0xFFFF8A7A : QuestJournalView.MUTED);
        int points = PaleMutation.available(mastery, model.mutations());
        if (points > 0) {
            QuestFrame.chipLeft(c, right, 56, points + (points == 1 ? " PT" : " PTS"), QuestFrame.GOLD_FILL, QuestStyle.GOLD_DARK, QuestStyle.GOLD);
        }
    }

    /** Every step of Owl DNA by its mastery ({@link PaleUnlock}), in words. */
    public static TreeMap<Integer, String> steps() {
        TreeMap<Integer, List<String>> parts = new TreeMap<>();
        for (PaleUnlock unlock : PaleUnlock.values()) {
            parts.computeIfAbsent(unlock.mastery(), k -> new ArrayList<>()).add(unlock.text());
        }
        TreeMap<Integer, String> out = new TreeMap<>();
        parts.forEach((m, words) -> out.put(m, m == 0 ? "From the start: " + words.size() + " moves and the base form" : String.join(", ", words)));
        return out;
    }

    // ------------------------------------------------------------------ the base form's tab

    private static QuestFrame.Tip formTab(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, double time) {
        int picked = pickedStage(model);
        int y = QuestFrame.CONTENT_Y;
        for (int i = 0; i < STAGES.length; i++) {
            QuestFrame.card(c, stageCard(model, i), QuestFrame.LIST_X, y, QuestFrame.LIST_WIDTH, i == picked, hits, mouseX, mouseY, time, i);
            y += QuestFrame.CARD_HEIGHT + QuestFrame.CARD_GAP;
        }
        int ry = QuestFrame.section(c, "THE ORGAN", QuestFrame.LIST_X, y + 6, QuestFrame.LIST_WIDTH);
        int ink = QuestFrame.VALUE;
        QuestFrame.row(c, "Ghoul", "Natural, one-eyed", QuestFrame.LIST_X, ry, 66, QuestFrame.LIST_WIDTH, ink);
        QuestFrame.row(c, "Kagune", "Pale flesh, upper back", QuestFrame.LIST_X, ry + 12, 66, QuestFrame.LIST_WIDTH, ink);
        QuestFrame.row(c, "Form", model.active() ? "Base form, out" : "Base form (Ctrl + R)", QuestFrame.LIST_X, ry + 24, 66, QuestFrame.LIST_WIDTH, ink);
        QuestFrame.row(c, "Blitz", PaleStrike.count(model.mastery()) + " of " + PaleStrike.of(PaleStrike.Style.BLITZ).length + " strikes",
            QuestFrame.LIST_X, ry + 36, 66, QuestFrame.LIST_WIDTH, ink);
        int ky = QuestFrame.section(c, "KAKUGAN", QuestFrame.LIST_X, ry + 54, QuestFrame.LIST_WIDTH);
        c.text(c.fit("One eye only: the " + eyeText(model.eyes()).toLowerCase(java.util.Locale.ROOT), QuestFrame.LIST_WIDTH), QuestFrame.LIST_X, ky,
            QuestFrame.MUTED, false);
        QuestFrame.button(c, "eyes", "ADJUST KAKUGAN", QuestFrame.LIST_X, ky + 14, QuestFrame.LIST_WIDTH, 16, true, false, hits, mouseX, mouseY, time);
        stageDetail(c, model, picked, QuestFrame.DETAIL_X, QuestFrame.CONTENT_Y, DETAIL_WIDTH, time);
        return road(c, model, hits, mouseX, mouseY, QuestFrame.DETAIL_X, ROAD_Y, DETAIL_WIDTH, time, STAGES[picked]);
    }

    /** The stage shown: the one picked (or the one a step of the road falls in), else hers now. */
    static int pickedStage(Model model) {
        String picked = model.picked();
        if (picked != null && picked.startsWith("pick:s:")) {
            try {
                int i = Integer.parseInt(picked.substring(7));
                if (i >= 0 && i < STAGES.length) {
                    return i;
                }
            } catch (NumberFormatException wrong) {
                // hers now
            }
        }
        if (picked != null && picked.startsWith("step:")) {
            try {
                return stageAt(Integer.parseInt(picked.substring(5)));
            } catch (NumberFormatException wrong) {
                // hers now
            }
        }
        return stageAt(model.mastery());
    }

    /** The stage {@code mastery} falls in. */
    static int stageAt(int mastery) {
        int at = 0;
        for (int i = 0; i < STAGES.length; i++) {
            if (mastery >= STAGES[i]) {
                at = i;
            }
        }
        return at;
    }

    static QuestLog.State stageState(Model model, int i) {
        if (model.mastery() < STAGES[i]) {
            return QuestLog.State.LOCKED;
        }
        return stageAt(model.mastery()) == i ? QuestLog.State.OPEN : QuestLog.State.DONE;
    }

    static QuestFrame.Card stageCard(Model model, int i) {
        int mastery = model.mastery();
        QuestLog.State state = stageState(model, i);
        int from = i == 0 ? 0 : STAGES[i - 1];
        boolean reached = state != QuestLog.State.LOCKED;
        boolean next = !reached && mastery >= from;
        float share = reached ? 1.0F : next ? Math.max(0.0F, Math.min(1.0F, (mastery - from) / (float)Math.max(1, STAGES[i] - from))) : 0.0F;
        String chip = switch (state) {
            case OPEN -> "NOW";
            case DONE -> "GROWN PAST";
            default -> "at mastery " + STAGES[i];
        };
        return new QuestFrame.Card("pick:s:" + i, PaleUnlock.stageName(STAGES[i]), !reached, chip, QuestStyle.colour(state), QuestStyle.dark(state),
            next ? Math.round(share * 100.0F) + "%" : "", share, state == QuestLog.State.DONE ? QuestJournalView.BAR_DONE : QuestJournalView.BAR,
            next && share >= 0.8F, (cc, x, y, size, back, t) -> stageGlyph(cc, i, reached ? QuestStyle.colour(state) : QuestStyle.mix(QuestStyle.colour(state),
                back, 0.45F), x, y, size, back, t));
    }

    static String story(int stage) {
        return switch (stage) {
            case 0 -> "An experiment's first: the Owl's cells in a natural one-eyed ghoul. Her fists, her flips and the Big Punch; the pale "
                + "flesh still sleeping under her skin.";
            case 1 -> "The strain takes: the flesh answers her now, sweeping, reaching and throwing; her legs too fast to see.";
            default -> "All of it hers: the hammer of flesh, the tentacles, the mutated arm, the Owl, and a mutation at every milestone.";
        };
    }

    /** A stage, as the journal shows a quest: its story, the flesh in its tile, what it brings, and what she hits with. */
    private static void stageDetail(UiCanvas c, Model model, int stage, int x, int y, int width, double time) {
        int mastery = model.mastery();
        QuestLog.State state = stageState(model, stage);
        title(c, PaleUnlock.stageName(STAGES[stage]), STAGES[stage] == 0 ? "From the start" : "Mastery " + STAGES[stage], x, y, width);
        int rowY = y + 16;
        String standing = switch (state) {
            case OPEN -> "Hers now" + (model.active() ? ", the flesh out" : "");
            case DONE -> "Grown past";
            default -> "Ahead of her: at mastery " + STAGES[stage] + "  (" + (STAGES[stage] - mastery) + " to go)";
        };
        c.text(c.fit(standing, width), x, rowY, state == QuestLog.State.OPEN ? QuestJournalView.OPEN : state == QuestLog.State.DONE ? QuestJournalView.DONE
            : QuestFrame.MUTED, false);
        rowY += 13;
        rowY = QuestFrame.story(c, story(stage), x, rowY, width) + 8;
        int tileY = rowY;
        boolean locked = state == QuestLog.State.LOCKED;
        tile(c, state, x, tileY, time);
        PathChoiceLayout.Box box = new PathChoiceLayout.Box(x + 8, tileY + 6, TILE_WIDTH - 16, TILE_HEIGHT - 30);
        List<PathSilhouette.Mark> marks = PathSilhouette.pale(box, time, locked ? 0.0F : 1.0F, stage > 0, 0x0B090B);
        c.batch(() -> {
            for (PathSilhouette.Mark mark : marks) {
                UkakuMenuView.draw(c, mark);
            }
        });
        veil(c, locked, x, tileY);
        c.centered(stage == 0 ? "the flesh asleep" : stage == 1 ? "the flesh answers" : "the flesh unbound", x + TILE_WIDTH / 2,
            tileY + TILE_HEIGHT - 15, locked ? QuestFrame.FAINT : 0xFFB0767A, false);
        int rx = x + TILE_WIDTH + 12;
        int rw = width - TILE_WIDTH - 12;
        int end = stage + 1 < STAGES.length ? STAGES[stage + 1] : Integer.MAX_VALUE;
        List<PaleUnlock> brings = new ArrayList<>();
        for (PaleUnlock unlock : PaleUnlock.values()) {
            if (unlock.mastery() >= STAGES[stage] && unlock.mastery() < end) {
                brings.add(unlock);
            }
        }
        int got = 0;
        for (PaleUnlock unlock : brings) {
            got += unlock.reached(mastery) ? 1 : 0;
        }
        int ry = QuestFrame.section(c, "WHAT IT BRINGS", got + " / " + brings.size(), rx, tileY, rw);
        for (PaleUnlock unlock : brings) {
            QuestFrame.checkRow(c, unlock.reached(mastery), !unlock.reached(mastery), unlock.text(), String.valueOf(unlock.mastery()), rx, ry, rw);
            ry += 14;
        }
        int cy = QuestFrame.section(c, "HER BLOWS", x, Math.max(tileY + TILE_HEIGHT, ry) + 6, width);
        int half = (width - 16) / 2;
        String[][] rows = {
            {"Fist", String.format(java.util.Locale.ROOT, "%.1f a strike", PaleRules.fist(mastery))},
            {"Big Punch", String.format(java.util.Locale.ROOT, "%.0f to %.0f, %.0f blocks", PaleRules.punchDamage(mastery, 0.0F),
                PaleRules.punchDamage(mastery, 1.0F), PaleRules.punchRange(1.0F))},
            {"Flesh reach", mastery >= PaleRules.SWIPE_MASTERY ? String.format(java.util.Locale.ROOT, "%.0f blocks", PaleRules.SWIPE_REACH)
                : "at mastery " + PaleRules.SWIPE_MASTERY},
            {"Flesh Hammer", mastery >= PaleRules.HAMMER_MASTERY ? String.format(java.util.Locale.ROOT, "%.1f to %.1f blocks wide",
                PaleRules.hammerRadius(0.0F), PaleRules.hammerRadius(1.0F)) : "at mastery " + PaleRules.HAMMER_MASTERY}};
        for (int i = 0; i < rows.length; i++) {
            QuestFrame.row(c, rows[i][0], rows[i][1], x + (i % 2) * (half + 16), cy + (i / 2) * 13, 76, half, QuestFrame.VALUE);
        }
    }

    // ------------------------------------------------------------------ the moves' tabs (hers and the Owl's)

    private static QuestFrame.Tip movesTab(UiCanvas c, Model model, List<Move> moves, String prefix, Map<String, UkakuMenuView.Rect> hits,
                                           double mouseX, double mouseY, double time) {
        Move picked = pickedMove(model, moves, prefix);
        int y = QuestFrame.CONTENT_Y;
        int index = 0;
        for (Move move : moves) {
            QuestFrame.card(c, moveCard(model, move, prefix), QuestFrame.LIST_X, y, QuestFrame.LIST_WIDTH, move == picked, hits, mouseX, mouseY, time,
                index++);
            y += QuestFrame.CARD_HEIGHT + QuestFrame.CARD_GAP;
        }
        moveDetail(c, model, picked, QuestFrame.DETAIL_X, QuestFrame.CONTENT_Y, DETAIL_WIDTH, time);
        return road(c, model, hits, mouseX, mouseY, QuestFrame.DETAIL_X, ROAD_Y, DETAIL_WIDTH, time, picked.mastery());
    }

    /** The move shown: the one picked (or the first a step of the road brings), else the tab's first. */
    static Move pickedMove(Model model, List<Move> moves, String prefix) {
        String picked = model.picked();
        if (picked != null) {
            for (Move move : moves) {
                if (picked.equals(prefix + move.id())) {
                    return move;
                }
            }
            if (picked.startsWith("step:")) {
                for (Move move : moves) {
                    if (picked.equals("step:" + move.mastery())) {
                        return move;
                    }
                }
            }
        }
        return moves.get(0);
    }

    static boolean had(Model model, Move move) {
        return model.mastery() >= move.mastery();
    }

    static QuestFrame.Card moveCard(Model model, Move move, String prefix) {
        boolean had = had(model, move);
        QuestLog.State state = had ? QuestLog.State.DONE : QuestLog.State.LOCKED;
        String chip = had ? move.key() + (move.cooldown() > 0 ? "  ·  " + format(move.cooldown() / 20.0F) + " S" : "") : "at mastery " + move.mastery();
        int ink = had ? QuestStyle.GREEN : QuestStyle.colour(state);
        return new QuestFrame.Card(prefix + move.id(), move.name(), !had, chip, ink, QuestStyle.dark(state), "", -1.0F, 0, false,
            (cc, x, y, size, back, t) -> keyGlyph(cc, move, had ? (move.owl() ? EYE : QuestStyle.GREEN) : QuestStyle.mix(ink, back, 0.45F), x, y, size, back,
                !had));
    }

    /** A move, as the journal shows a quest: its story, its numbers in two columns, what it waits for; the flesh or the Owl in its tile. */
    private static void moveDetail(UiCanvas c, Model model, Move move, int x, int y, int width, double time) {
        boolean had = had(model, move);
        title(c, move.name(), "Key " + move.key(), x, y, width);
        int rowY = y + 16;
        boolean kakuja = move.id().equals("OWL");
        String standing = had ? (kakuja ? "Hers: K grows it, K again turns back" : move.owl() ? "Hers, in the Owl" : "Hers")
            : "Ahead of her: at mastery " + move.mastery() + "  (" + (move.mastery() - model.mastery()) + " to go)";
        c.text(c.fit(standing, width), x, rowY, had ? QuestJournalView.DONE : QuestFrame.MUTED, false);
        rowY += 13;
        rowY = QuestFrame.story(c, move.story(), x, rowY, width) + 10;
        rowY = QuestFrame.section(c, kakuja ? "THE KAKUJA" : "THE MOVE", x, rowY, width);
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"Key", move.key()});
        rows.add(new String[] {"Mastery", String.valueOf(move.mastery())});
        rows.add(new String[] {"Cooldown", move.cooldown() > 0 ? format(move.cooldown() / 20.0F) + " s" : "none"});
        rows.add(new String[] {"RC", move.cost() > 0.0F ? format(move.cost()) + (kakuja ? " to grow it" : "") : "none"});
        rows.addAll(extra(model, move));
        int half = (width - 16) / 2;
        for (int i = 0; i < rows.size(); i++) {
            QuestFrame.row(c, rows.get(i)[0], rows.get(i)[1], x + (i % 2) * (half + 16), rowY + (i / 2) * 13, 70, half,
                had ? QuestFrame.VALUE : QuestFrame.MUTED);
        }
        rowY += ((rows.size() + 1) / 2) * 13 + 8;
        if (!had) {
            int ly = rowY;
            c.batch(() -> QuestStyle.lock(c, x, ly, 12, QuestStyle.GOLD, QuestJournalView.INNER));
            c.text(c.fit("Waits for mastery " + move.mastery() + "  (" + (move.mastery() - model.mastery()) + " to go)", width - 20), x + 17, rowY + 3,
                QuestFrame.FAINT, false);
            rowY += 18;
        }
        if (rowY + TILE_HEIGHT > ROAD_Y - 4) {
            return; // no room for the picture
        }
        int tileY = rowY;
        tile(c, had ? QuestLog.State.DONE : QuestLog.State.LOCKED, x, tileY, time);
        PathChoiceLayout.Box box = new PathChoiceLayout.Box(x + 8, tileY + 6, TILE_WIDTH - 16, TILE_HEIGHT - 30);
        List<PathSilhouette.Mark> marks = move.owl() ? PathSilhouette.owl(box, time, had ? 1.0F : 0.0F, 0x0B090B)
            : PathSilhouette.pale(box, time, had ? 1.0F : 0.0F, move.mastery() >= PaleRules.SWIPE_MASTERY, 0x0B090B);
        c.batch(() -> {
            for (PathSilhouette.Mark mark : marks) {
                UkakuMenuView.draw(c, mark);
            }
        });
        c.centered(move.owl() ? "the Owl" : "her base form", x + TILE_WIDTH / 2, tileY + TILE_HEIGHT - 15, had ? 0xFFB0767A : QuestFrame.FAINT, false);
        int rx = x + TILE_WIDTH + 12;
        int rw = width - TILE_WIDTH - 12;
        if (move.id().equals("BLITZ")) {
            PaleStrike[] strikes = PaleStrike.of(PaleStrike.Style.BLITZ);
            int ry = QuestFrame.section(c, "ITS STRIKES", PaleStrike.count(model.mastery()) + " / " + strikes.length, rx, tileY, rw);
            for (PaleStrike strike : strikes) {
                boolean got = model.mastery() >= strike.mastery;
                QuestFrame.checkRow(c, got, !got, strikeName(strike) + (strike.finisher() ? "  (ends it)" : ""), String.valueOf(strike.mastery), rx, ry, rw);
                ry += 14;
            }
        } else if (kakuja) {
            int ry = QuestFrame.section(c, "IN IT", rx, tileY, rw);
            String[][] in = {{"Hurt", Math.round(OwlRules.TAKEN * 100.0F) + "% of every blow reaches her"}, {"Grows", "in " + format(OwlRules.GROW_TICKS
                / 20.0F) + " s"}, {"Her keys", "its own moves (this tab)"}};
            for (String[] r : in) {
                QuestFrame.row(c, r[0], r[1], rx, ry, 56, rw, had ? QuestFrame.VALUE : QuestFrame.MUTED);
                ry += 13;
            }
        }
    }

    /** What else a move is in numbers. */
    static List<String[]> extra(Model model, Move move) {
        int mastery = Math.max(model.mastery(), move.mastery());
        List<String[]> rows = new ArrayList<>();
        switch (move.id()) {
            case "BLITZ" -> rows.add(new String[] {"Fist", String.format(java.util.Locale.ROOT, "%.1f a strike", PaleRules.fist(mastery))});
            case "PUNCH" -> {
                rows.add(new String[] {"Hits", String.format(java.util.Locale.ROOT, "%.0f to %.0f", PaleRules.punchDamage(mastery, 0.0F),
                    PaleRules.punchDamage(mastery, 1.0F))});
                rows.add(new String[] {"Sends", String.format(java.util.Locale.ROOT, "%.0f blocks", PaleRules.punchRange(1.0F))});
            }
            case "SWIPE" -> rows.add(new String[] {"Reach", String.format(java.util.Locale.ROOT, "%.0f blocks", PaleRules.SWIPE_REACH)});
            case "HAMMER" -> {
                rows.add(new String[] {"Wide", String.format(java.util.Locale.ROOT, "%.1f to %.1f blocks", PaleRules.hammerRadius(0.0F),
                    PaleRules.hammerRadius(1.0F))});
                rows.add(new String[] {"Blocks", "weak ones smashed, back in 30 s"});
            }
            case "ARM" -> rows.add(new String[] {"Sphere", format(ArmRules.SPHERE_COST) + " RC held"});
            case "REND" -> rows.add(new String[] {"Cuts", OwlRules.REND_STAGES + ", the last the heaviest"});
            default -> {
            }
        }
        return rows;
    }

    /** A blitz strike's name. */
    static String strikeName(PaleStrike strike) {
        return switch (strike) {
            case JAB -> "Jab";
            case CROSS -> "Cross";
            case HOOK -> "Hook";
            case LIVER -> "Liver shot";
            case UPPERCUT -> "Uppercut";
            case BLINK_PALM -> "Blinking palm";
            case BACKFIST -> "Spinning backfist";
            case TWIN_PALM -> "Twin palms";
            default -> strike.name().charAt(0) + strike.name().substring(1).toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        };
    }

    // ------------------------------------------------------------------ the mutations' tab

    enum Standing {
        OWNED, READY, POINT
    }

    static Standing standing(Model model, PaleMutation mutation) {
        if (mutation.in(model.mutations())) {
            return Standing.OWNED;
        }
        return PaleMutation.canUnlock(model.mastery(), model.mutations(), mutation) ? Standing.READY : Standing.POINT;
    }

    static PaleMutation pickedMutation(Model model) {
        if (model.picked() != null) {
            for (PaleMutation mutation : PaleMutation.values()) {
                if (model.picked().equals("pick:u:" + mutation.id())) {
                    return mutation;
                }
            }
            if (model.picked().startsWith("milestone:")) {
                for (int i = 0; i < PaleMutation.MILESTONES.length && i < PaleMutation.values().length; i++) {
                    if (model.picked().equals("milestone:" + PaleMutation.MILESTONES[i])) {
                        return PaleMutation.values()[i];
                    }
                }
            }
        }
        for (Standing wanted : new Standing[] {Standing.READY, Standing.POINT}) {
            for (PaleMutation mutation : PaleMutation.values()) {
                if (standing(model, mutation) == wanted) {
                    return mutation;
                }
            }
        }
        return PaleMutation.values()[0];
    }

    private static QuestFrame.Tip mutationsTab(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, double time) {
        PaleMutation picked = pickedMutation(model);
        int y = QuestFrame.CONTENT_Y;
        int index = 0;
        for (PaleMutation mutation : PaleMutation.values()) {
            QuestFrame.card(c, mutationCard(model, mutation), QuestFrame.LIST_X, y, QuestFrame.LIST_WIDTH, mutation == picked, hits, mouseX, mouseY, time,
                index++);
            y += QuestFrame.CARD_HEIGHT + QuestFrame.CARD_GAP;
        }
        int mastery = model.mastery();
        int points = PaleMutation.available(mastery, model.mutations());
        int py = QuestFrame.section(c, "POINTS", QuestFrame.LIST_X, y + 6, QuestFrame.LIST_WIDTH);
        c.push();
        c.translate(QuestFrame.LIST_X, py - 2);
        c.scale(1.6F);
        c.text(String.valueOf(points), 0, 0, points > 0 ? QuestStyle.GOLD : QuestFrame.MUTED, true);
        c.pop();
        c.text(points == 1 ? "point to spend" : "points to spend", QuestFrame.LIST_X + 22, py + 2, points > 0 ? QuestStyle.GOLD : QuestFrame.MUTED, false);
        int next = PaleMutation.nextMilestone(mastery);
        QuestFrame.row(c, "Earned", PaleMutation.earned(mastery) + " of " + PaleMutation.MILESTONES.length + ", one at each milestone", QuestFrame.LIST_X,
            py + 18, 66, QuestFrame.LIST_WIDTH, QuestFrame.VALUE);
        QuestFrame.row(c, "Spent", String.valueOf(PaleMutation.unlocked(model.mutations())), QuestFrame.LIST_X, py + 30, 66, QuestFrame.LIST_WIDTH,
            QuestFrame.VALUE);
        QuestFrame.row(c, "Next", next > 0 ? "at mastery " + next : "every one earned", QuestFrame.LIST_X, py + 42, 66, QuestFrame.LIST_WIDTH,
            QuestFrame.VALUE);
        mutationDetail(c, model, picked, hits, mouseX, mouseY, QuestFrame.DETAIL_X, QuestFrame.CONTENT_Y, DETAIL_WIDTH, time);
        return milestones(c, model, hits, mouseX, mouseY, QuestFrame.DETAIL_X, ROAD_Y, DETAIL_WIDTH, time);
    }

    static QuestFrame.Card mutationCard(Model model, PaleMutation mutation) {
        Standing standing = standing(model, mutation);
        String chip = switch (standing) {
            case OWNED -> mutation == PaleMutation.NOT_THAT_EASILY ? reviveTag(model.reviveIn()) : "ON";
            case READY -> "READY  ·  1 POINT";
            default -> "NEEDS A POINT";
        };
        int ink = standing == Standing.OWNED ? QuestStyle.GREEN : standing == Standing.READY ? QuestStyle.GOLD : QuestStyle.colour(QuestLog.State.LOCKED);
        int dark = standing == Standing.OWNED ? QuestStyle.GREEN_DARK : standing == Standing.READY ? QuestStyle.GOLD_DARK : QuestStyle.GREY_DARK;
        return new QuestFrame.Card("pick:u:" + mutation.id(), mutation.displayName(), standing == Standing.POINT, chip, ink, dark, "", -1.0F, 0,
            standing == Standing.READY, (cc, x, y, size, back, t) -> mutationGlyph(cc, mutation, standing == Standing.POINT ? QuestStyle.mix(ink, back, 0.45F)
                : ink, x, y, size, back, t));
    }

    /** A mutation, as a quest: what it does, its cost and UNLOCK (only while a point may be spent on it), or ON (and its switch). */
    private static void mutationDetail(UiCanvas c, Model model, PaleMutation mutation, Map<String, UkakuMenuView.Rect> hits, double mouseX,
                                       double mouseY, int x, int y, int width, double time) {
        Standing standing = standing(model, mutation);
        title(c, mutation.displayName(), "Mutation", x, y, width);
        int rowY = y + 16;
        int next = PaleMutation.nextMilestone(model.mastery());
        String line = switch (standing) {
            case OWNED -> mutation == PaleMutation.NOT_THAT_EASILY && model.reviveIn() > 0L ? "Hers: regrowing, ready in "
                + reviveTag(model.reviveIn()).substring(10) : "Hers, on";
            case READY -> "A point to spend: unlock it below";
            default -> next > 0 ? "Her next point comes at mastery " + next : "Every point is spent";
        };
        c.text(c.fit(line, width), x, rowY, standing == Standing.OWNED ? QuestJournalView.DONE : standing == Standing.READY ? QuestJournalView.OPEN
            : QuestFrame.MUTED, false);
        rowY += 13;
        rowY = QuestFrame.story(c, mutation.description(), x, rowY, width) + 10;
        if (standing == Standing.OWNED) {
            QuestFrame.stamp(c, x, rowY, "ON", QuestStyle.GREEN, QuestStyle.GREEN_DARK);
            rowY += 40;
            if (mutation == PaleMutation.WITNESS_THIS) {
                boolean on = (model.mutations() & PaleMutation.INSTANT_PUNCH) != 0;
                rowY = QuestFrame.section(c, "INSTANT BIG PUNCH", on ? "on" : "off", x, rowY, width);
                rowY = QuestFrame.story(c, on ? "Z tapped is the full-charge punch at once, straight into the combo." : "Z held to charge as usual; "
                    + "switched on, a tap is the full punch and the combo.", x, rowY, width) + 6;
                QuestFrame.button(c, "instant", on ? "SWITCH IT OFF" : "SWITCH IT ON", x, rowY, 200, 22, true, !on, hits, mouseX, mouseY, time);
            }
            return;
        }
        rowY = QuestFrame.section(c, "COST", x, rowY, width);
        QuestFrame.badge(c, 'p', "1 mutation point", true, x, rowY);
        rowY += 22;
        QuestFrame.button(c, "mutation:" + mutation.ordinal(), standing == Standing.READY ? "UNLOCK  ·  SPEND 1 POINT" : "NEEDS A POINT", x, rowY, 200,
            22, standing == Standing.READY, true, hits, mouseX, mouseY, time);
        int points = PaleMutation.available(model.mastery(), model.mutations());
        c.text(c.fit(points > 0 ? points + (points == 1 ? " point to spend" : " points to spend") : next > 0 ? "a point at mastery " + next
            : "every point spent", width - 212), x + 212, rowY + 8, points > 0 ? QuestStyle.GOLD : QuestFrame.MUTED, false);
    }

    /** The milestones that pay a point, on the road: earned ticked, the next beating. */
    private static QuestFrame.Tip milestones(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, int x, int y,
                                             int width, double time) {
        int mastery = model.mastery();
        y = QuestFrame.section(c, "POINTS ON HER ROAD", "a point at each", x, y, width);
        int next = PaleMutation.nextMilestone(mastery);
        List<QuestFrame.Node> nodes = new ArrayList<>();
        int reached = -1;
        for (int i = 0; i < PaleMutation.MILESTONES.length; i++) {
            int milestone = PaleMutation.MILESTONES[i];
            boolean got = mastery >= milestone;
            reached = got ? i : reached;
            nodes.add(new QuestFrame.Node("milestone:" + milestone, String.valueOf(milestone), "Mastery " + milestone + ": a mutation point  ·  "
                + (got ? "earned" : "at mastery " + milestone), got ? QuestLog.State.DONE : milestone == next ? QuestLog.State.OPEN : QuestLog.State.LOCKED,
                false));
        }
        return QuestFrame.road(c, nodes, reached, hits, mouseX, mouseY, x, y, width, 400, time);
    }

    /** Ready, or how long until it is (m:ss). */
    static String reviveTag(long ticksLeft) {
        if (ticksLeft <= 0L) {
            return "READY";
        }
        long seconds = (ticksLeft + 19L) / 20L;
        return String.format(java.util.Locale.ROOT, "REGROWING %d:%02d", seconds / 60L, seconds % 60L);
    }

    // ------------------------------------------------------------------ shared pieces

    /** Which eye turns: her chosen one, or (never both on her path) the right. */
    static String eyeText(KakuganEyes eyes) {
        return eyes != null && eyes.ghoul() == KakuganEyes.LEFT ? "Left eye" : "Right eye";
    }

    private static String format(float value) {
        return value == Math.round(value) ? Integer.toString(Math.round(value)) : String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static void title(UiCanvas c, String name, String chip, int x, int y, int width) {
        c.push();
        c.translate(x, y);
        c.scale(1.25F);
        c.text(c.fit(name, Math.round((width - c.width(chip) - 30) / 1.25F)), 0, 0, QuestStyle.WHITE, true);
        c.pop();
        QuestStyle.chip(c, x + width - c.width(chip) - 12, y, chip, 0xFF1E0A0E, QuestStyle.RED_DARK, QuestFrame.MUTED);
    }

    private static void tile(UiCanvas c, QuestLog.State state, int x, int y, double time) {
        int edge = state == QuestLog.State.OPEN ? QuestStyle.mix(QuestStyle.RED_DARK, QuestStyle.RED, 0.5F + 0.5F * (float)Math.sin(time * 2.2))
            : state == QuestLog.State.DONE ? QuestStyle.GREEN_DARK : QuestStyle.TILE_EDGE;
        c.batch(() -> {
            QuestStyle.box(c, x, y, TILE_WIDTH, TILE_HEIGHT, QuestStyle.TILE, edge);
            c.gradient(x + 2, y + 2, x + TILE_WIDTH - 2, y + TILE_HEIGHT - 2, 0xFF120A0E, 0xFF070506);
        });
    }

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

    /** Every step of Owl DNA on the quest road: reached ticked, the next beating; {@code lit}'s ringed. */
    private static QuestFrame.Tip road(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, int x, int y,
                                       int width, double time, int lit) {
        int mastery = model.mastery();
        y = QuestFrame.section(c, "HER ROAD", "a step opens what it brings", x, y, width);
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

    // ------------------------------------------------------------------ glyphs

    /** A stage: her one eye, opening wider as she grows (a lid, then the iris, then the eye wide and burning). */
    static void stageGlyph(UiCanvas c, int stage, int ink, int x, int y, int size, int back, double time) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        float open = stage == 0 ? 0.35F : stage == 1 ? 0.65F : 1.0F;
        int w = Math.round(size * 0.42F);
        for (int k = -w; k <= w; k++) {
            int lid = (int)Math.round(Math.sqrt(Math.max(0.0, 1.0 - (k / (double)w) * (k / (double)w))) * size * 0.26F * open);
            c.fill(cx + k, cy - lid - 1, cx + k + 1, cy - lid + 1, ink);
            c.fill(cx + k, cy + lid - 1, cx + k + 1, cy + lid + 1, ink);
        }
        if (stage > 0) {
            QuestStyle.circle(c, cx, cy, Math.max(1, Math.round(size * 0.13F * open)), stage == 2 ? EYE : ink);
        }
    }

    /** A move: its key in a keycap. */
    static void keyGlyph(UiCanvas c, Move move, int ink, int x, int y, int size, int back, boolean dim) {
        String key = move.key().replace(" held", "+");
        int w = Math.max(size - 4, c.width(key) + 4);
        int left = x + (size - w) / 2;
        c.batch(() -> QuestStyle.box(c, left, y + 3, w, size - 6, QuestStyle.mix(back, ink, 0.12F), ink));
        c.centered(key, x + size / 2, y + size / 2 - 4, ink, false);
    }

    static void mutationGlyph(UiCanvas c, PaleMutation mutation, int ink, int x, int y, int size, int back, double time) {
        float s = size;
        switch (mutation) {
            case NOT_THAT_EASILY -> UkakuMenuView.mutationGlyph(c, com.hex.kagune.control.ukaku.UkakuMutation.NOT_THAT_EASILY, ink, x, y, size, back, time);
            case UP_WE_GO -> {
                for (int k = 0; k < 2; k++) {
                    float oy = y + s * (0.62F - k * 0.28F);
                    QuestStyle.line(c, x + s * 0.24F, oy + s * 0.14F, x + s * 0.5F, oy - s * 0.1F, 1.6F, ink);
                    QuestStyle.line(c, x + s * 0.5F, oy - s * 0.1F, x + s * 0.76F, oy + s * 0.14F, 1.6F, ink);
                }
            }
            case TRUE_ANONYMITY -> {
                QuestStyle.circle(c, x + size / 2, y + size / 2, Math.round(s * 0.24F), ink);
                for (int k = 0; k < 8; k++) {
                    double a = Math.PI * 2.0 * k / 8;
                    QuestStyle.line(c, x + s * 0.5F + (float)Math.cos(a) * s * 0.24F, y + s * 0.5F + (float)Math.sin(a) * s * 0.24F,
                        x + s * 0.5F + (float)Math.cos(a) * s * 0.42F, y + s * 0.5F + (float)Math.sin(a) * s * 0.42F, 1.2F, ink);
                }
            }
            default -> {
                // Witness This: a fist and the blow's lines.
                c.fill(x + Math.round(s * 0.3F), y + Math.round(s * 0.36F), x + Math.round(s * 0.62F), y + Math.round(s * 0.66F), ink);
                c.fill(x + Math.round(s * 0.34F), y + Math.round(s * 0.4F), x + Math.round(s * 0.58F), y + Math.round(s * 0.44F), back);
                for (int k = -1; k <= 1; k++) {
                    QuestStyle.line(c, x + s * 0.68F, y + s * (0.5F + k * 0.12F), x + s * 0.86F, y + s * (0.5F + k * 0.18F), 1.2F, ink);
                }
            }
        }
    }
}
