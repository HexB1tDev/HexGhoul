package com.hex.kagune.control.client;

import com.hex.kagune.control.client.ui.UiCanvas;
import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.graft.GraftMutation;
import com.hex.kagune.control.graft.GraftRules;
import com.hex.kagune.control.graft.GraftStage;
import com.hex.kagune.control.quest.Gate;
import com.hex.kagune.control.quest.Gates;
import com.hex.kagune.control.quest.QuestLog;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 7.0.0: Graft DNA's kagune menu (the menu key): the organ (Rize's transplanted kakuhou), mastery, the base stages to grow into, RC, the
 * kakuja, the kakugan placement, every key's move in each stage, the unlock track, and the mutations a point at each milestone buys.
 *
 * <p>4.1.60 (the owner: "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as interactable as it is,
 * Dialog, mastery menus ALL OF THEM"): the quest menu's box ({@link QuestFrame}), as the other mastery menus. Tabs STAGES, MOVES and
 * MUTATIONS (its badge: points to spend). A stage is grown into only by its GROW INTO IT button, a mutation bought only by its UNLOCK
 * button; every card shows how it stands and what it waits for (its mastery, its quest or teacher); the road of every step at the foot.
 */
public final class GraftMenuView {
    /** The old board's virtual size (the screens that still scale by it). */
    public static final int UI_WIDTH = UkakuMenuView.UI_WIDTH;
    public static final int UI_HEIGHT = UkakuMenuView.UI_HEIGHT;
    /** 4.1.60: the box's size: the other mastery menus'. */
    public static final int BOX_WIDTH = UkakuMenuView.BOX_WIDTH;
    public static final int BOX_HEIGHT = UkakuMenuView.BOX_HEIGHT;
    static final int DETAIL_WIDTH = UkakuMenuView.DETAIL_WIDTH;
    static final int ROAD_Y = UkakuMenuView.ROAD_Y;
    static final int TILE_WIDTH = UkakuMenuView.TILE_WIDTH;
    static final int TILE_HEIGHT = UkakuMenuView.TILE_HEIGHT;
    /** The keys each slot answers to, as the menu names them. */
    static final GraftMove.Slot[] SLOTS = {GraftMove.Slot.M1, GraftMove.Slot.M2, GraftMove.Slot.Z, GraftMove.Slot.J, GraftMove.Slot.V,
        GraftMove.Slot.B, GraftMove.Slot.N, GraftMove.Slot.H, GraftMove.Slot.G, GraftMove.Slot.C};
    static final String[] KEYS = {"M1", "RMB", "1", "2", "3", "4", "5", "6", "7", "8"};

    /** 4.1.60: the menu's tabs. */
    public enum Tab {
        STAGES("STAGES"), MOVES("MOVES"), MUTATIONS("MUTATIONS");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    /**
     * What the menu shows. 4.1.60: the tab open and what is picked ("pick:s:ID" a stage, "pick:m:NAME" a move, "pick:u:ID" a mutation,
     * "step:N" a step of the road; null: the stage out, its first move, the mutation most worth a look).
     */
    public record Model(int mastery, boolean active, GraftStage stage, boolean kakuja, float energy, float maxEnergy, int mutations,
                        QuestLocks quests, Tab tab, String picked) {
        public Model {
            quests = quests == null ? QuestLocks.ALL : quests;
            tab = tab == null ? Tab.STAGES : tab;
            stage = stage == null ? GraftStage.TRANSPLANT : stage;
        }

        public Model(int mastery, boolean active, GraftStage stage, boolean kakuja, float energy, float maxEnergy) {
            this(mastery, active, stage, kakuja, energy, maxEnergy, 0);
        }

        public Model(int mastery, boolean active, GraftStage stage, boolean kakuja, float energy, float maxEnergy, int mutations) {
            this(mastery, active, stage, kakuja, energy, maxEnergy, mutations, QuestLocks.ALL);
        }

        public Model(int mastery, boolean active, GraftStage stage, boolean kakuja, float energy, float maxEnergy, int mutations,
                     QuestLocks quests) {
            this(mastery, active, stage, kakuja, energy, maxEnergy, mutations, quests, Tab.STAGES, null);
        }

        /** The stage he is in now (the kakuja while it is on). */
        GraftStage shown() {
            return this.kakuja ? GraftStage.KAKUJA : this.stage;
        }
    }

    private GraftMenuView() {
    }

    /**
     * Draws the box at its own size and returns its clickable areas: "tab:NAME", "pick:...", "step:N", "eyes", "stage:ID" (GROW INTO IT,
     * only for a stage he may grow into and is not in), "mutation:ID" (UNLOCK, only while a point may be spent on it).
     */
    public static Map<String, UkakuMenuView.Rect> draw(UiCanvas c, Model model, double mouseX, double mouseY, double time) {
        Map<String, UkakuMenuView.Rect> hits = new LinkedHashMap<>();
        QuestFrame.plate(c, BOX_WIDTH, BOX_HEIGHT, time, 292, 6, 170, 38);
        header(c, model, time);
        int points = points(model);
        QuestFrame.tabs(c, List.of(new QuestFrame.Tab(Tab.STAGES.name(), Tab.STAGES.label, 0, false), new QuestFrame.Tab(Tab.MOVES.name(),
            Tab.MOVES.label, 0, false), new QuestFrame.Tab(Tab.MUTATIONS.name(), Tab.MUTATIONS.label, points, true)), model.tab().name(), hits, mouseX,
            mouseY, QuestFrame.LIST_X, QuestFrame.TABS_Y, time);
        QuestFrame.Tip tip = switch (model.tab()) {
            case MOVES -> movesTab(c, model, hits, mouseX, mouseY, time);
            case MUTATIONS -> mutationsTab(c, model, hits, mouseX, mouseY, time);
            default -> stagesTab(c, model, hits, mouseX, mouseY, time);
        };
        QuestFrame.tooltip(c, tip, 8, BOX_WIDTH - 8);
        return hits;
    }

    /** Mutation points to spend (the milestones' and the quests'). */
    static int points(Model model) {
        return GraftMutation.available(model.mastery(), model.mutations(), model.quests().points());
    }

    // ------------------------------------------------------------------ the header

    private static void header(UiCanvas c, Model model, double time) {
        int mastery = model.mastery();
        TreeMap<Integer, String> steps = steps();
        Integer next = steps.ceilingKey(mastery + 1);
        Integer from = steps.floorKey(mastery);
        int start = from == null ? 0 : from;
        float share = next == null ? 1.0F : (mastery - start) / (float)Math.max(1, next - start);
        String goal = next == null ? "Every step of the graft reached" : mastery + " / " + next + "  ·  next: " + steps.get(next);
        QuestFrame.header(c, BOX_WIDTH, "GRAFT DNA", "RIZE'S KAKUHOU  ·  MASTERY " + mastery, share, next == null, goal, time);
        int right = BOX_WIDTH - 12;
        boolean out = model.active();
        right = QuestFrame.chipLeft(c, right, 56, out ? "KAGUNE OUT  ·  R RETRACTS" : "KAGUNE IN  ·  ALT + R MANIFESTS",
            out ? 0xFF2A0A10 : 0xFF181012, out ? QuestStyle.RED_DARK : QuestStyle.GREY_DARK, out ? 0xFFFF8A7A : QuestJournalView.MUTED);
        right = QuestFrame.chipLeft(c, right, 56, "RC " + Math.round(model.energy()) + " / " + Math.round(model.maxEnergy()), QuestFrame.RED_FILL,
            QuestStyle.RED_DARK, QuestFrame.VALUE);
        int points = points(model);
        if (points > 0) {
            QuestFrame.chipLeft(c, right, 56, points + " PT" + (points == 1 ? "" : "S"), QuestFrame.GOLD_FILL, QuestStyle.GOLD_DARK, QuestStyle.GOLD);
        }
    }

    /** Every step of the graft by its mastery: its stages, the second tail, its keyed moves, in words. */
    public static TreeMap<Integer, String> steps() {
        TreeMap<Integer, List<String>> parts = new TreeMap<>();
        for (GraftStage stage : GraftStage.values()) {
            parts.computeIfAbsent(stage.mastery(), k -> new ArrayList<>()).add(stage.displayName() + (stage.kakuja() ? " (K)" : ""));
        }
        parts.computeIfAbsent(GraftStage.TWIN_MASTERY, k -> new ArrayList<>()).add("the second tail");
        for (GraftMove move : GraftMove.track()) {
            if (move.slot() != GraftMove.Slot.M1 && move.slot() != GraftMove.Slot.NONE) {
                parts.computeIfAbsent(move.mastery(), k -> new ArrayList<>()).add(move.displayName());
            }
        }
        TreeMap<Integer, String> out = new TreeMap<>();
        parts.forEach((m, words) -> out.put(m, String.join(", ", words)));
        return out;
    }

    // ------------------------------------------------------------------ the stages' tab

    private static QuestFrame.Tip stagesTab(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, double time) {
        GraftStage picked = pickedStage(model);
        int y = QuestFrame.CONTENT_Y;
        int index = 0;
        for (GraftStage stage : GraftStage.values()) {
            QuestFrame.card(c, stageCard(model, stage), QuestFrame.LIST_X, y, QuestFrame.LIST_WIDTH, stage == picked, hits, mouseX, mouseY, time, index++);
            y += QuestFrame.CARD_HEIGHT + QuestFrame.CARD_GAP;
        }
        float share = model.maxEnergy() <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, model.energy() / model.maxEnergy()));
        int ry = QuestFrame.section(c, "RC", Math.round(model.energy()) + " / " + Math.round(model.maxEnergy()), QuestFrame.LIST_X, y + 6,
            QuestFrame.LIST_WIDTH);
        int barY = ry + 3;
        c.batch(() -> {
            UkakuMenuView.capsule(c, QuestFrame.LIST_X, barY, QuestFrame.LIST_WIDTH, 3, QuestJournalView.BAR_BACK);
            UkakuMenuView.capsule(c, QuestFrame.LIST_X, barY, Math.max(6, Math.round(QuestFrame.LIST_WIDTH * share)), 3, QuestJournalView.BAR);
        });
        c.text("One bar: RC, healing and food. Eat to fill it", QuestFrame.LIST_X, ry + 10, QuestFrame.MUTED, false);
        int ky = QuestFrame.section(c, "KAKUGAN", QuestFrame.LIST_X, ry + 28, QuestFrame.LIST_WIDTH);
        QuestFrame.button(c, "eyes", "ADJUST KAKUGAN", QuestFrame.LIST_X, ky, QuestFrame.LIST_WIDTH, 16, true, false, hits, mouseX, mouseY, time);
        stageDetail(c, model, picked, hits, mouseX, mouseY, QuestFrame.DETAIL_X, QuestFrame.CONTENT_Y, DETAIL_WIDTH, time);
        return road(c, model, hits, mouseX, mouseY, QuestFrame.DETAIL_X, ROAD_Y, DETAIL_WIDTH, time, picked);
    }

    /** The stage shown: the one picked (or the stage a step of the road is in), else the one he is in. */
    static GraftStage pickedStage(Model model) {
        String picked = model.picked();
        if (picked != null) {
            if (picked.startsWith("pick:s:")) {
                GraftStage stage = GraftStage.fromId(picked.substring(7));
                if (stage != null) {
                    return stage;
                }
            }
            if (picked.startsWith("step:")) {
                try {
                    return stageAt(Integer.parseInt(picked.substring(5)));
                } catch (NumberFormatException wrong) {
                    // the one he is in
                }
            }
        }
        return model.shown();
    }

    /** The stage a step at {@code mastery} belongs to. */
    static GraftStage stageAt(int mastery) {
        GraftStage at = GraftStage.TRANSPLANT;
        for (GraftStage stage : GraftStage.values()) {
            if (mastery >= stage.mastery()) {
                at = stage;
            }
        }
        return at;
    }

    /** How a stage stands: in now (open), had (done), waiting for its quest (closed), ahead (locked). */
    static QuestLog.State state(Model model, GraftStage stage) {
        if (!stage.unlocked(model.mastery())) {
            return QuestLog.State.LOCKED;
        }
        if (!model.quests().passes(Gates.graft(stage))) {
            return QuestLog.State.CLOSED;
        }
        return stage == model.shown() ? QuestLog.State.OPEN : QuestLog.State.DONE;
    }

    /**
     * True if he may grow into {@code stage} from here, as the server takes it (GraftServer.pickStage): had, its quest done, no kakuja (K),
     * not the stage he is in (in the kakuja too: it rests on that stage).
     */
    static boolean growable(Model model, GraftStage stage) {
        return state(model, stage) == QuestLog.State.DONE && !stage.kakuja() && stage != model.stage();
    }

    /** A stage as a quest card: its glyph, its name, a chip (GROWN, READY, TAP K, NEEDS ITS QUEST, "at mastery 450"), the bar to it. */
    static QuestFrame.Card stageCard(Model model, GraftStage stage) {
        int mastery = model.mastery();
        QuestLog.State state = state(model, stage);
        GraftStage[] all = GraftStage.values();
        int from = stage.ordinal() == 0 ? 0 : all[stage.ordinal() - 1].mastery();
        boolean reached = state != QuestLog.State.LOCKED;
        boolean next = !reached && mastery >= from;
        float share = reached ? 1.0F : next ? Math.max(0.0F, Math.min(1.0F, (mastery - from) / (float)Math.max(1, stage.mastery() - from))) : 0.0F;
        String chip = switch (state) {
            case OPEN -> stage.kakuja() ? "ON  ·  TAP K" : "GROWN";
            case DONE -> stage.kakuja() ? "TAP K" : stage == model.stage() ? "GROWN  ·  UNDER THE KAKUJA" : "READY";
            case CLOSED -> "NEEDS ITS QUEST";
            default -> "at mastery " + stage.mastery();
        };
        return new QuestFrame.Card("pick:s:" + stage.id(), stage.displayName(), !reached || state == QuestLog.State.CLOSED, chip, QuestStyle.colour(state),
            QuestStyle.dark(state), next ? Math.round(share * 100.0F) + "%" : "", share, state == QuestLog.State.DONE ? QuestJournalView.BAR_DONE
            : QuestJournalView.BAR, next && share >= 0.8F, (cc, x, y, size, back, t) -> stageGlyph(cc, stage, state, model.mastery(), x, y, size, back, t));
    }

    /** A stage, as the journal shows a quest: its story, the kagune in a framed tile, what it is, its moves; GROW INTO IT. */
    private static void stageDetail(UiCanvas c, Model model, GraftStage stage, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY,
                                    int x, int y, int width, double time) {
        int mastery = model.mastery();
        QuestLog.State state = state(model, stage);
        title(c, stage.displayName(), stage.mastery() == 0 ? "From the start" : "Mastery " + stage.mastery(), x, y, width);
        int rowY = y + 16;
        String standing = switch (state) {
            case OPEN -> stage.kakuja() ? "On now: tap K to come out of it" : "Grown now" + (model.active() ? ", out" : "");
            case DONE -> stage.kakuja() ? "Yours: tap K to transform" : stage == model.stage() ? "Grown; the kakuja is on over it"
                : "Yours: grow into it below";
            case CLOSED -> "Opens with " + QuestFrame.opener(Gates.graft(stage));
            default -> "Ahead of you: at mastery " + stage.mastery();
        };
        c.text(c.fit(standing, width), x, rowY, state == QuestLog.State.OPEN ? QuestJournalView.OPEN : state == QuestLog.State.DONE ? QuestJournalView.DONE
            : QuestFrame.MUTED, false);
        rowY += 13;
        rowY = QuestFrame.story(c, story(stage), x, rowY, width) + 8;
        boolean locked = state == QuestLog.State.LOCKED || state == QuestLog.State.CLOSED;
        int tileY = rowY;
        int edge = state == QuestLog.State.OPEN ? QuestStyle.mix(QuestStyle.RED_DARK, QuestStyle.RED, 0.5F + 0.5F * (float)Math.sin(time * 2.2))
            : state == QuestLog.State.DONE ? QuestStyle.GREEN_DARK : QuestStyle.TILE_EDGE;
        c.batch(() -> {
            QuestStyle.box(c, x, tileY, TILE_WIDTH, TILE_HEIGHT, QuestStyle.TILE, edge);
            c.gradient(x + 2, tileY + 2, x + TILE_WIDTH - 2, tileY + TILE_HEIGHT - 2, 0xFF12060A, 0xFF070305);
        });
        PathChoiceLayout.Box box = new PathChoiceLayout.Box(x + 8, tileY + 6, TILE_WIDTH - 16, TILE_HEIGHT - 30);
        List<PathSilhouette.Mark> marks = PathSilhouette.graftForm(box, time, locked ? 0.0F : 1.0F, stage, Math.max(mastery, stage.mastery()), 0x0C080E);
        c.batch(() -> {
            for (PathSilhouette.Mark mark : marks) {
                UkakuMenuView.draw(c, mark);
            }
            if (locked) {
                c.fill(x + 2, tileY + 2, x + TILE_WIDTH - 2, tileY + TILE_HEIGHT - 2, 0xA0080306);
                QuestStyle.lock(c, x + TILE_WIDTH / 2 - 13, tileY + (TILE_HEIGHT - 26) / 2 - 12, 26, QuestStyle.mix(QuestStyle.GOLD, QuestStyle.TILE, 0.2F),
                    0xFF0A0405);
            }
        });
        int limbs = stage.limbs(Math.max(mastery, stage.mastery()));
        c.centered(caption(stage, limbs), x + TILE_WIDTH / 2, tileY + TILE_HEIGHT - 15, locked ? QuestFrame.FAINT : 0xFFB0767A, false);
        int rx = x + TILE_WIDTH + 12;
        int rw = width - TILE_WIDTH - 12;
        int ink = locked ? QuestFrame.MUTED : QuestFrame.VALUE;
        int at = Math.max(mastery, stage.mastery());
        int ry = QuestFrame.section(c, "THE KAGUNE", rx, rowY, rw);
        QuestFrame.row(c, "Limbs", limbs(stage, at), rx, ry, 58, rw, ink);
        QuestFrame.row(c, "Length", String.format(java.util.Locale.ROOT, "%.1f blocks", stage.length(at)), rx, ry + 12, 58, rw, ink);
        QuestFrame.row(c, "Blows", String.format(java.util.Locale.ROOT, "x%.2f", stage.damage()), rx, ry + 24, 58, rw, ink);
        ry += 42;
        List<GraftMove> moves = moves(stage);
        int got = 0;
        for (GraftMove move : moves) {
            got += !locked && mastery >= move.mastery() ? 1 : 0;
        }
        ry = QuestFrame.section(c, "ITS MOVES", got + " / " + moves.size(), rx, ry, rw);
        for (GraftMove move : moves) {
            // A stage still shut keeps its moves shut, whatever the mastery.
            boolean had = !locked && mastery >= move.mastery();
            QuestFrame.checkRow(c, had, !had, key(move) + "  " + (move.slot() == GraftMove.Slot.M1 ? "a string of " + string(stage).size() + " blows"
                : move.displayName()), String.valueOf(move.mastery()), rx, ry, rw);
            ry += 14;
        }
        rowY = Math.max(tileY + TILE_HEIGHT, ry) + 8;
        if ((state == QuestLog.State.OPEN || state == QuestLog.State.DONE && stage == model.stage()) && !stage.kakuja()) {
            QuestFrame.stamp(c, x, rowY, "GROWN", QuestStyle.RED, QuestStyle.RED_DARK);
        } else {
            String label = switch (state) {
                case OPEN, DONE -> stage.kakuja() ? "TAP K" : "GROW INTO IT";
                case CLOSED -> "NEEDS ITS QUEST";
                default -> "AT MASTERY " + stage.mastery();
            };
            QuestFrame.button(c, "stage:" + stage.id(), label, x, rowY, 200, 22, growable(model, stage), false, hits, mouseX, mouseY, time);
            if (stage.kakuja() && !locked) {
                c.text(c.fit(Math.round(GraftRules.KAKUJA_COST) + " RC, then it drains; " + Math.round((1.0F - GraftRules.KAKUJA_TAKEN) * 100)
                    + "% less from every blow", width - 212), x + 212, rowY + 8, QuestFrame.MUTED, false);
            }
        }
    }

    /** Under a stage's picture: its limbs by what they are. */
    static String caption(GraftStage stage, int limbs) {
        String noun = switch (stage) {
            case TRANSPLANT -> limbs == 1 ? "tail" : "tails";
            case CENTIPEDE -> "centipedes";
            case THORNS -> "thorns";
            case DRAGON -> "crescents";
            case KAKUJA -> "colossi";
        };
        return limbs + " " + noun;
    }

    /** A stage's limbs in words. */
    static String limbs(GraftStage stage, int mastery) {
        int n = stage.limbs(mastery);
        return switch (stage) {
            case TRANSPLANT -> n == 1 ? "1 tail (2 at " + GraftStage.TWIN_MASTERY + ")" : "2 tails";
            case CENTIPEDE -> "3 armored centipedes, one with a mouth";
            case THORNS -> "4 thorned giants";
            case DRAGON -> "a mass of eyes, " + n + " crescents";
            case KAKUJA -> "armor, a head, 2 colossal centipedes";
        };
    }

    /** What each stage is, told as a quest's story. */
    static String story(GraftStage stage) {
        return switch (stage) {
            case TRANSPLANT -> "Rize's transplanted kakuhou: one muscular tail from your lower back, a second at mastery " + GraftStage.TWIN_MASTERY
                + ". Fast and precise.";
            case CENTIPEDE -> "Three armored centipedes out of your back; the first has a toothed mouth that hunts on its own.";
            case THORNS -> "Four enormous thorn-covered tails, far bigger than you are, that catch, run through and hurl.";
            case DRAGON -> "Vast thick crescents full of eyes, stacked one on another into one mass out to your side.";
            case KAKUJA -> "The kakuja (K): living armor over your body, a monstrous head and two colossal centipedes. Never a stage you rest "
                + "in: K turns it on and off.";
        };
    }

    // ------------------------------------------------------------------ the moves' tab

    /** The keyed moves of {@code stage}, in key order (M1's string by its first blow). */
    static List<GraftMove> moves(GraftStage stage) {
        List<GraftMove> out = new ArrayList<>();
        for (GraftMove.Slot slot : SLOTS) {
            GraftMove move = GraftMove.of(slot, stage);
            if (move != GraftMove.NONE) {
                out.add(move);
            }
        }
        return out;
    }

    /** A move's key as the menu names it. */
    static String key(GraftMove move) {
        for (int i = 0; i < SLOTS.length; i++) {
            if (SLOTS[i] == move.slot()) {
                return KEYS[i];
            }
        }
        return "";
    }

    private static QuestFrame.Tip movesTab(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, double time) {
        GraftStage stage = movesStage(model);
        GraftMove picked = pickedMove(model, stage);
        // The stages as pills: which stage's moves are shown.
        List<QuestFrame.Tab> pills = new ArrayList<>();
        String[] names = {"I", "II", "III", "IV", "K"};
        for (GraftStage each : GraftStage.values()) {
            pills.add(new QuestFrame.Tab("s:" + each.id(), names[each.ordinal()], 0, false));
        }
        Map<String, UkakuMenuView.Rect> pillHits = new LinkedHashMap<>();
        QuestFrame.tabs(c, pills, "s:" + stage.id(), pillHits, mouseX, mouseY, QuestFrame.LIST_X, QuestFrame.CONTENT_Y, time);
        pillHits.forEach((k, r) -> hits.put("pick:" + k.substring(4), r)); // "tab:s:ID" -> "pick:s:ID"
        int y = QuestFrame.CONTENT_Y + 24;
        int index = 0;
        for (GraftMove move : moves(stage)) {
            QuestFrame.card(c, moveCard(model, move), QuestFrame.LIST_X, y, QuestFrame.LIST_WIDTH, move == picked, hits, mouseX, mouseY, time, index++);
            y += QuestFrame.CARD_HEIGHT + QuestFrame.CARD_GAP;
        }
        int ny = QuestFrame.section(c, "BLADE AND KAGUNE", QuestFrame.LIST_X, y + 4, QuestFrame.LIST_WIDTH);
        for (String line : UkakuMenuView.wrap(c, "A blade or an axe in hand: M1 is your weapon, and the kagune pierces what it cut.",
            QuestFrame.LIST_WIDTH)) {
            c.text(line, QuestFrame.LIST_X, ny, QuestFrame.MUTED, false);
            ny += 12;
        }
        moveDetail(c, model, picked, QuestFrame.DETAIL_X, QuestFrame.CONTENT_Y, DETAIL_WIDTH, time);
        return road(c, model, hits, mouseX, mouseY, QuestFrame.DETAIL_X, ROAD_Y, DETAIL_WIDTH, time, stage);
    }

    /** The stage whose moves are shown: the one picked ("pick:s:ID", or the stage of a move or step picked), else the one he is in. */
    static GraftStage movesStage(Model model) {
        String picked = model.picked();
        if (picked != null && picked.startsWith("pick:m:")) {
            for (GraftMove move : GraftMove.values()) {
                if (move.name().equals(picked.substring(7))) {
                    return move.stage();
                }
            }
        }
        return pickedStage(model);
    }

    /** The move shown: the one picked if it is the stage's, else the stage's first. */
    static GraftMove pickedMove(Model model, GraftStage stage) {
        List<GraftMove> moves = moves(stage);
        String picked = model.picked();
        if (picked != null && picked.startsWith("pick:m:")) {
            for (GraftMove move : moves) {
                if (move.name().equals(picked.substring(7))) {
                    return move;
                }
            }
        }
        return moves.isEmpty() ? GraftMove.NONE : moves.get(0);
    }

    /** How a move stands: had (done), its stage waiting for its quest (closed), ahead (locked). */
    static QuestLog.State moveState(Model model, GraftMove move) {
        if (model.mastery() < move.mastery()) {
            return QuestLog.State.LOCKED;
        }
        return model.quests().passes(Gates.graft(move.stage())) ? QuestLog.State.DONE : QuestLog.State.CLOSED;
    }

    static QuestFrame.Card moveCard(Model model, GraftMove move) {
        QuestLog.State state = moveState(model, move);
        String chip = switch (state) {
            case DONE -> key(move) + (move.cooldown() > 0 ? "  ·  " + move.cooldown() / 20 + " S"
                : move.slot() == GraftMove.Slot.M1 ? "  ·  " + string(move.stage()).size() + " BLOWS" : "");
            case CLOSED -> "NEEDS ITS QUEST";
            default -> "at mastery " + move.mastery();
        };
        int ink = state == QuestLog.State.DONE ? QuestStyle.GREEN : QuestStyle.colour(state);
        boolean dim = state != QuestLog.State.DONE;
        return new QuestFrame.Card("pick:m:" + move.name(), name(move), dim, chip, ink, QuestStyle.dark(state), "", -1.0F, 0, false,
            (cc, x, y, size, back, t) -> {
                stageGlyph(cc, move.stage(), dim ? QuestLog.State.LOCKED : QuestLog.State.DONE, model.mastery(), x, y, size, back, t);
            });
    }

    /** A move's name as the menu shows it (M1 is the stage's string). */
    static String name(GraftMove move) {
        return move.slot() == GraftMove.Slot.M1 ? move == GraftMove.ACCORD ? "Blade and Kagune" : "The M1 string" : move.displayName();
    }

    /** The blows of {@code stage}'s M1 string, in order (never the sword's). */
    static List<GraftMove> string(GraftStage stage) {
        List<GraftMove> out = new ArrayList<>();
        for (GraftMove move : GraftMove.values()) {
            if (move.slot() == GraftMove.Slot.M1 && move.stage() == stage && move != GraftMove.ACCORD) {
                out.add(move);
            }
        }
        return out;
    }

    /** A move, as the journal shows a quest: its story, then its numbers in two columns; what it waits for. */
    private static void moveDetail(UiCanvas c, Model model, GraftMove move, int x, int y, int width, double time) {
        if (move == GraftMove.NONE) {
            return;
        }
        QuestLog.State state = moveState(model, move);
        title(c, name(move), "Key " + key(move), x, y, width);
        int rowY = y + 16;
        String standing = switch (state) {
            case DONE -> "Yours, in " + move.stage().displayName();
            case CLOSED -> "Opens with " + QuestFrame.opener(Gates.graft(move.stage()));
            default -> "Ahead of you: at mastery " + move.mastery();
        };
        c.text(c.fit(standing, width), x, rowY, state == QuestLog.State.DONE ? QuestJournalView.DONE : QuestFrame.MUTED, false);
        rowY += 13;
        rowY = QuestFrame.story(c, story(move), x, rowY, width) + 10;
        rowY = QuestFrame.section(c, "THE MOVE", x, rowY, width);
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"Key", key(move)});
        rows.add(new String[] {"Stage", move.stage().displayName()});
        rows.add(new String[] {"Cooldown", move.cooldown() > 0 ? move.cooldown() / 20.0 + " s" : "none"});
        rows.add(new String[] {"RC", String.format(java.util.Locale.ROOT, "%.1f", move.cost())});
        rows.add(new String[] {"Lasts", String.format(java.util.Locale.ROOT, "%.1f s", move.ticks() / 20.0)});
        if (!move.hint().isEmpty()) {
            rows.add(new String[] {"Then", move.hint()});
        }
        int half = (width - 16) / 2;
        for (int i = 0; i < rows.size(); i++) {
            QuestFrame.row(c, rows.get(i)[0], rows.get(i)[1], x + (i % 2) * (half + 16), rowY + (i / 2) * 13, 70, half,
                state == QuestLog.State.DONE ? QuestFrame.VALUE : QuestFrame.MUTED);
        }
        rowY += ((rows.size() + 1) / 2) * 13 + 10;
        if (state != QuestLog.State.DONE) {
            int ly = rowY;
            c.batch(() -> QuestStyle.lock(c, x, ly, 12, QuestStyle.GOLD, QuestJournalView.INNER));
            c.text(c.fit(state == QuestLog.State.CLOSED ? "Waits for " + QuestFrame.opener(Gates.graft(move.stage())) : "Waits for mastery " + move.mastery()
                + "  (" + (move.mastery() - model.mastery()) + " to go)", width - 20), x + 17, rowY + 3, QuestFrame.FAINT, false);
            rowY += 20;
        }
        // The kagune that makes it, and (for the string) its blows in order.
        int tileY = rowY;
        boolean locked = state != QuestLog.State.DONE;
        c.batch(() -> {
            QuestStyle.box(c, x, tileY, TILE_WIDTH, TILE_HEIGHT, QuestStyle.TILE, locked ? QuestStyle.TILE_EDGE : QuestStyle.GREEN_DARK);
            c.gradient(x + 2, tileY + 2, x + TILE_WIDTH - 2, tileY + TILE_HEIGHT - 2, 0xFF12060A, 0xFF070305);
        });
        PathChoiceLayout.Box box = new PathChoiceLayout.Box(x + 8, tileY + 6, TILE_WIDTH - 16, TILE_HEIGHT - 30);
        List<PathSilhouette.Mark> marks = PathSilhouette.graftForm(box, time, locked ? 0.0F : 1.0F, move.stage(),
            Math.max(model.mastery(), move.stage().mastery()), 0x0C080E);
        c.batch(() -> {
            for (PathSilhouette.Mark mark : marks) {
                UkakuMenuView.draw(c, mark);
            }
        });
        c.centered(move.stage().displayName(), x + TILE_WIDTH / 2, tileY + TILE_HEIGHT - 15, locked ? QuestFrame.FAINT : 0xFFB0767A, false);
        if (move.slot() == GraftMove.Slot.M1 && move != GraftMove.ACCORD) {
            int rx = x + TILE_WIDTH + 12;
            int rw = width - TILE_WIDTH - 12;
            List<GraftMove> blows = string(move.stage());
            int ry = QuestFrame.section(c, "ITS BLOWS", blows.size() + " in a row", rx, tileY, rw);
            for (int i = 0; i < blows.size(); i++) {
                GraftMove blow = blows.get(i);
                QuestFrame.row(c, String.valueOf(i + 1), blow.displayName() + (i == blows.size() - 1 ? "  (the heaviest)" : ""), rx, ry + i * 13, 18, rw,
                    locked ? QuestFrame.MUTED : QuestFrame.VALUE);
            }
        }
    }

    /** What each keyed move does, told as a quest's story (its own words, short). */
    static String story(GraftMove move) {
        return switch (move) {
            case ACCORD -> "With a sword or an axe in hand, M1 is your own blade, and then the kagune pierces where it cut.";
            case STEP -> "The kagune throws you aside in a quick step: out of the way of a blow, or in on what you hunt.";
            case MOCKINGBIRD -> "Open, the tails drawn low and poised: whoever strikes at you now is countered, hooked and run through from behind.";
            case OWLS_EXECUTION -> "In on it, then cut after cut as you circle it, the tails switching angles; both driven through it at the end.";
            case SCORPION_VAULT -> "Both tails stabbed into the ground behind you: you are thrown high; the key again dives down, the impact stuns.";
            case HUMAN_PUPPET -> "One tail catches it and drags it in, stunned; then your choice: the key again hurls it, M1 or right click.";
            case AMBUSH -> "The side centipedes feint at its front; the mouth comes round under it and snaps up through its guard.";
            case LOCOMOTION -> "The centipedes plant ahead in turn and haul you along where you look; you fight as you go; the key again bites.";
            case HUNGRY_MAW -> "The mouth goes after its prey, round what is in the way; caught, it is dragged along wherever you go.";
            case CARAPACE -> "The centipedes coiled round you: a blow at you springs them out at the attacker, and the mouth bites after.";
            case CRUCIFIXION -> "The upper pair catch it and hold it up; the lower pair run it through, again and again; then it is hurled.";
            case BREACH -> "Thrown forward, the four spread ahead of you, smashing what stands in the way and running through what lives.";
            case DRAGNET -> "The lower pair far out to either side, swept in, dragging what they pass to the middle; the upper pair come down on it.";
            case CATAPULT -> "All four planted, and you are flung high, steered where you look; down, the four driven into the ground round you.";
            case STORM -> "The mass grown wide and wheeling round you, every crescent on its own path, cutting what it passes.";
            case BLOOM -> "Drawn in round you, flung out all round, spikes bursting along it in waves, drawn in again.";
            case CURTAIN -> "Raised into a wall of flesh before you, then folded forward onto what is under it, its ends curled round the sides.";
            case KINGS_GAME -> "The mass after one body, at it from every side while you go where you like; a sword's M1 rushes in on it.";
            case CHECKMATE -> "One colossus at its front, the other round behind it; you charge in: claws, jaws, a colossus, a last blow.";
            case HUNT -> "Hurled after it, one colossus biting as you close, the other striding you on; caught, it is dragged and thrown.";
            case RECOVERY -> "Curled round you, healing fast for much RC; you can move, not strike; the key again stops it.";
            case PERFORMANCE -> "The colossi at whatever you look at from every side, your claws at it as fast as you click; the key again: the end.";
            default -> move.slot() == GraftMove.Slot.M1 ? "The stage's string of blows on M1, each its own, the last the heaviest." : "";
        };
    }

    // ------------------------------------------------------------------ the mutations' tab

    enum Standing {
        OWNED, READY, QUEST, POINT
    }

    static Standing standing(Model model, GraftMutation mutation) {
        if (mutation.in(model.mutations())) {
            return Standing.OWNED;
        }
        if (!model.quests().passes(Gates.graft(mutation))) {
            return Standing.QUEST;
        }
        return GraftMutation.canUnlock(model.mastery(), model.mutations(), mutation, model.quests().points()) ? Standing.READY : Standing.POINT;
    }

    static GraftMutation pickedMutation(Model model) {
        if (model.picked() != null) {
            for (GraftMutation mutation : GraftMutation.values()) {
                if (model.picked().equals("pick:u:" + mutation.id())) {
                    return mutation;
                }
            }
        }
        for (Standing wanted : new Standing[] {Standing.READY, Standing.POINT, Standing.QUEST}) {
            for (GraftMutation mutation : GraftMutation.values()) {
                if (standing(model, mutation) == wanted) {
                    return mutation;
                }
            }
        }
        return GraftMutation.values()[0];
    }

    private static QuestFrame.Tip mutationsTab(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, double time) {
        GraftMutation picked = pickedMutation(model);
        int y = QuestFrame.CONTENT_Y;
        int index = 0;
        for (GraftMutation mutation : GraftMutation.values()) {
            QuestFrame.card(c, mutationCard(model, mutation), QuestFrame.LIST_X, y, QuestFrame.LIST_WIDTH, mutation == picked, hits, mouseX, mouseY, time,
                index++);
            y += QuestFrame.CARD_HEIGHT + QuestFrame.CARD_GAP;
        }
        int py = QuestFrame.section(c, "POINTS", QuestFrame.LIST_X, y + 6, QuestFrame.LIST_WIDTH);
        int points = points(model);
        String count = String.valueOf(points);
        c.push();
        c.translate(QuestFrame.LIST_X, py);
        c.scale(2.0F);
        c.text(count, 0, 0, points > 0 ? QuestStyle.GOLD : QuestFrame.MUTED, true);
        c.pop();
        c.text(points == 1 ? "point to spend" : "points to spend", QuestFrame.LIST_X + c.width(count) * 2 + 8, py + 5, points > 0 ? QuestStyle.GOLD
            : QuestFrame.MUTED, false);
        int next = GraftMutation.nextMilestone(model.mastery());
        QuestFrame.row(c, "Mastery", GraftMutation.earned(model.mastery()) + " earned at its milestones", QuestFrame.LIST_X, py + 22, 66,
            QuestFrame.LIST_WIDTH, QuestFrame.VALUE);
        QuestFrame.row(c, "Quests", model.quests().points() + " earned", QuestFrame.LIST_X, py + 34, 66, QuestFrame.LIST_WIDTH, QuestFrame.VALUE);
        QuestFrame.row(c, "Spent", String.valueOf(GraftMutation.unlocked(model.mutations())), QuestFrame.LIST_X, py + 46, 66, QuestFrame.LIST_WIDTH,
            QuestFrame.VALUE);
        QuestFrame.row(c, "Next", next > 0 ? "at mastery " + next : "every milestone reached", QuestFrame.LIST_X, py + 58, 66, QuestFrame.LIST_WIDTH,
            next > 0 ? QuestFrame.VALUE : QuestJournalView.DONE);
        mutationDetail(c, model, picked, hits, mouseX, mouseY, QuestFrame.DETAIL_X, QuestFrame.CONTENT_Y, DETAIL_WIDTH, time);
        return milestones(c, model, hits, mouseX, mouseY, QuestFrame.DETAIL_X, ROAD_Y, DETAIL_WIDTH, time);
    }

    static QuestFrame.Card mutationCard(Model model, GraftMutation mutation) {
        Standing standing = standing(model, mutation);
        int ink = switch (standing) {
            case OWNED -> QuestStyle.GREEN;
            case READY -> QuestStyle.GOLD;
            default -> QuestStyle.colour(QuestLog.State.LOCKED);
        };
        int dark = switch (standing) {
            case OWNED -> QuestStyle.GREEN_DARK;
            case READY -> QuestStyle.GOLD_DARK;
            default -> QuestStyle.GREY_DARK;
        };
        String chip = switch (standing) {
            case OWNED -> "UNLOCKED";
            case READY -> "READY  ·  1 POINT";
            case QUEST -> "NEEDS ITS QUEST";
            case POINT -> "NEEDS A POINT";
        };
        boolean locked = standing == Standing.QUEST || standing == Standing.POINT;
        return new QuestFrame.Card("pick:u:" + mutation.id(), mutation.displayName(), locked, chip, ink, dark, "", -1.0F, 0, standing == Standing.READY,
            (cc, x, y, size, back, t) -> {
                mutationGlyph(cc, mutation, locked ? QuestStyle.mix(ink, back, 0.45F) : ink, x, y, size, back, t);
                if (locked) {
                    QuestStyle.lock(cc, x + size / 2, y + size / 2, size / 2, QuestStyle.mix(QuestStyle.GOLD, back, 0.2F), back);
                }
            });
    }

    private static void mutationDetail(UiCanvas c, Model model, GraftMutation mutation, Map<String, UkakuMenuView.Rect> hits, double mouseX,
                                       double mouseY, int x, int y, int width, double time) {
        Standing standing = standing(model, mutation);
        title(c, mutation.displayName(), "Mutation", x, y, width);
        int rowY = y + 16;
        Gate gate = Gates.graft(mutation);
        int next = GraftMutation.nextMilestone(model.mastery());
        String line = switch (standing) {
            case OWNED -> "Unlocked: yours for good";
            case READY -> "A point to spend: unlock it below";
            case QUEST -> "Opens with " + QuestFrame.opener(gate);
            case POINT -> next > 0 ? "Needs a point: the next comes at mastery " + next : "Needs a point: a quest can still pay one";
        };
        c.text(c.fit(line, width), x, rowY, standing == Standing.OWNED ? QuestJournalView.DONE : standing == Standing.READY ? QuestJournalView.OPEN
            : QuestFrame.MUTED, false);
        rowY += 13;
        rowY = QuestFrame.story(c, mutation.description(), x, rowY, width) + 10;
        rowY = QuestFrame.section(c, "COST", x, rowY, width);
        int bx = x + QuestFrame.badge(c, 'p', "1 mutation point", true, x, rowY) + 5;
        if (gate != null) {
            QuestFrame.badge(c, 'k', c.fit("Opens with " + QuestFrame.opener(gate), width - (bx - x) - 26), true, bx, rowY);
        }
        rowY += 30;
        if (standing == Standing.OWNED) {
            QuestFrame.stamp(c, x, rowY, "UNLOCKED", QuestStyle.GREEN, QuestStyle.GREEN_DARK);
            return;
        }
        String label = switch (standing) {
            case READY -> "UNLOCK  ·  SPEND 1 POINT";
            case QUEST -> "NEEDS ITS QUEST";
            default -> "NEEDS A POINT";
        };
        QuestFrame.button(c, "mutation:" + mutation.id(), label, x, rowY, 200, 22, standing == Standing.READY, true, hits, mouseX, mouseY, time);
        int have = points(model);
        c.text(have + (have == 1 ? " point" : " points") + " to spend", x + 212, rowY + 8, have > 0 ? QuestStyle.GOLD : QuestFrame.FAINT, false);
    }

    private static QuestFrame.Tip milestones(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, int x, int y,
                                             int width, double time) {
        int mastery = model.mastery();
        y = QuestFrame.section(c, "POINTS ON YOUR ROAD", "a point at each", x, y, width);
        List<QuestFrame.Node> nodes = new ArrayList<>();
        int next = GraftMutation.nextMilestone(mastery);
        int reached = -1;
        for (int i = 0; i < GraftMutation.MILESTONES.length; i++) {
            int milestone = GraftMutation.MILESTONES[i];
            boolean got = mastery >= milestone;
            reached = got ? i : reached;
            nodes.add(new QuestFrame.Node("milestone:" + milestone, String.valueOf(milestone), "Mastery " + milestone + ": a mutation point  ·  "
                + (got ? "earned" : "ahead"), got ? QuestLog.State.DONE : milestone == next ? QuestLog.State.OPEN : QuestLog.State.LOCKED, false));
        }
        return QuestFrame.road(c, nodes, reached, hits, mouseX, mouseY, x, y, width, 140, time);
    }

    // ------------------------------------------------------------------ shared pieces

    private static void title(UiCanvas c, String name, String chip, int x, int y, int width) {
        c.push();
        c.translate(x, y);
        c.scale(1.25F);
        c.text(c.fit(name, Math.round((width - c.width(chip) - 30) / 1.25F)), 0, 0, QuestStyle.WHITE, true);
        c.pop();
        QuestStyle.chip(c, x + width - c.width(chip) - 12, y, chip, 0xFF1E0A0E, QuestStyle.RED_DARK, QuestFrame.MUTED);
    }

    /** Every step of the graft on the quest road: reached ticked, the next beating; the steps of {@code lit}'s stage ringed. */
    private static QuestFrame.Tip road(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, int x, int y,
                                       int width, double time, GraftStage lit) {
        int mastery = model.mastery();
        y = QuestFrame.section(c, "YOUR ROAD", "a step picks its stage", x, y, width);
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
                + (got ? "reached" : "at mastery " + step.getKey()), state, stageAt(step.getKey()) == lit));
            i++;
        }
        return QuestFrame.road(c, nodes, reached, hits, mouseX, mouseY, x, y, width, 64, time);
    }

    // ------------------------------------------------------------------ glyphs

    /** A stage's glyph: its tails (one, two), three centipedes, four thorned giants, crescents, the kakuja's head and two colossi. */
    static void stageGlyph(UiCanvas c, GraftStage stage, QuestLog.State state, int mastery, int x, int y, int size, int back, double time) {
        int ink = QuestStyle.colour(state);
        int dim = state == QuestLog.State.LOCKED || state == QuestLog.State.CLOSED ? QuestStyle.mix(ink, back, 0.5F) : ink;
        float s = size;
        float rx = x + s * 0.5F;
        float ry = y + s * 0.92F;
        switch (stage) {
            case TRANSPLANT -> {
                int tails = stage.limbs(Math.max(mastery, 0));
                UkakuMenuView.tentacle(c, rx, ry, (float)Math.toRadians(-58.0), s * 0.78F, -0.9F, s * 0.2F, dim);
                if (tails > 1) {
                    UkakuMenuView.tentacle(c, rx, ry, (float)Math.toRadians(-122.0), s * 0.78F, 0.9F, s * 0.2F, dim);
                }
            }
            case CENTIPEDE -> {
                for (int k = 0; k < 3; k++) {
                    float a = (float)Math.toRadians(-140.0 + k * 50.0);
                    UkakuMenuView.tentacle(c, rx, ry, a, s * 0.72F, (k - 1) * -0.5F, s * 0.16F, dim);
                }
                QuestStyle.circle(c, Math.round(rx + (float)Math.cos(Math.toRadians(-140.0)) * s * 0.66F),
                    Math.round(ry + (float)Math.sin(Math.toRadians(-140.0)) * s * 0.66F), Math.max(1, Math.round(s * 0.08F)), back);
            }
            case THORNS -> {
                for (int k = 0; k < 4; k++) {
                    float a = (float)Math.toRadians(-160.0 + k * 46.0);
                    float tx = rx + (float)Math.cos(a) * s * 0.8F;
                    float ty = ry + (float)Math.sin(a) * s * 0.8F;
                    QuestStyle.line(c, rx, ry, tx, ty, Math.max(1.6F, s * 0.14F), dim);
                    QuestStyle.line(c, (rx + tx) / 2, (ry + ty) / 2, (rx + tx) / 2 + (float)Math.cos(a + 1.2) * s * 0.14F,
                        (ry + ty) / 2 + (float)Math.sin(a + 1.2) * s * 0.14F, 1.0F, dim);
                }
            }
            case DRAGON -> {
                for (int k = 0; k < 3; k++) {
                    int cy = Math.round(y + s * (0.3F + k * 0.24F));
                    QuestStyle.circle(c, Math.round(rx), cy, Math.round(s * 0.24F), dim);
                    QuestStyle.circle(c, Math.round(rx + s * 0.1F), cy - 1, Math.round(s * 0.18F), back);
                    QuestStyle.circle(c, Math.round(rx - s * 0.08F), cy, 1, dim);
                }
            }
            default -> {
                QuestStyle.circle(c, Math.round(rx), Math.round(y + s * 0.34F), Math.round(s * 0.22F), dim);
                QuestStyle.circle(c, Math.round(rx - s * 0.08F), Math.round(y + s * 0.32F), 1, back);
                for (int side = -1; side <= 1; side += 2) {
                    UkakuMenuView.tentacle(c, rx, y + s * 0.6F, (float)Math.toRadians(side < 0 ? -170.0 : -10.0), s * 0.5F, side * 1.0F, s * 0.16F, dim);
                }
            }
        }
        if (state == QuestLog.State.LOCKED || state == QuestLog.State.CLOSED) {
            QuestStyle.lock(c, x + size / 2, y + size / 2, size / 2, QuestStyle.mix(QuestStyle.GOLD, back, 0.2F), back);
        }
    }

    /** A mutation's glyph: the revive a cracked heart, the appetite a mouth, the overdrive a rising arrow, the blood a drop. */
    static void mutationGlyph(UiCanvas c, GraftMutation mutation, int ink, int x, int y, int size, int back, double time) {
        float s = size;
        switch (mutation) {
            case NOT_THAT_EASILY -> UkakuMenuView.mutationGlyph(c, com.hex.kagune.control.ukaku.UkakuMutation.NOT_THAT_EASILY, ink, x, y, size, back, time);
            case RIZES_APPETITE -> {
                QuestStyle.circle(c, x + size / 2, y + size / 2, Math.round(s * 0.44F), ink);
                QuestStyle.circle(c, x + size / 2, y + size / 2, Math.round(s * 0.26F), back);
                for (int k = 0; k < 6; k++) {
                    double a = Math.PI * 2.0 * k / 6;
                    QuestStyle.line(c, x + s * 0.5F + (float)Math.cos(a) * s * 0.28F, y + s * 0.5F + (float)Math.sin(a) * s * 0.28F,
                        x + s * 0.5F + (float)Math.cos(a) * s * 0.12F, y + s * 0.5F + (float)Math.sin(a) * s * 0.12F, 1.2F, ink);
                }
            }
            case KAKUHOU_OVERDRIVE -> QuestStyle.rewardGlyph(c, 'm', x + size / 2 - 5, y + size / 2 - 6, ink, back);
            default -> QuestStyle.rewardGlyph(c, 'd', x + size / 2 - 5, y + size / 2 - 6, ink, back);
        }
    }
}
