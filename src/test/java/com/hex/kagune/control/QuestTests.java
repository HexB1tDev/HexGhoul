package com.hex.kagune.control;

import com.hex.kagune.control.quest.Gate;
import com.hex.kagune.control.quest.Hunts;
import com.hex.kagune.control.quest.Objective;
import com.hex.kagune.control.quest.Quest;
import com.hex.kagune.control.quest.QuestBook;
import com.hex.kagune.control.quest.QuestEvent;
import com.hex.kagune.control.quest.QuestLog;
import com.hex.kagune.control.quest.Signal;
import com.hex.kagune.control.quest.Tags;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** 7.0.1: the quests: the book holds together, a log moves through its states, saves round-trip, hunts are drawn and rebuilt. */
public final class QuestTests {
    static int passed;
    static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) {
        book();
        states();
        exclusive();
        oneLife();
        saves();
        migration();
        hunts();
        gates();
        givers();
        taughtAlready();
        romance();
        forGood();
        forGoodLater();
        journal();
        ukakuMenu();
        rinkakuMenu();
        bikakuMenu();
        graftMenu();
        paleMenu();
        kingChoice();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Quest tests: " + passed + " checks passed");
    }

    /**
     * 4.1.34 (the owner: "We also need to better the looks of the quest menu ... It's just really dull, really bad looking, not interactive at
     * all"; "the background and all, they're also super ugly"; "why not just a cute box?"): a compact box (the world seen round it); every tab,
     * card and node of the road something to click, inside the box; the road holds every quest of the path; the organic board, its veins and
     * the full-screen overlay gone.
     */
    static void journal() {
        com.hex.kagune.control.client.ui.UiCanvas none = new com.hex.kagune.control.client.ui.UiCanvas() {
            public void fill(int x0, int y0, int x1, int y1, int argb) {
            }

            public void gradient(int x0, int y0, int x1, int y1, int top, int bottom) {
            }

            public void text(String text, int x, int y, int argb, boolean shadow) {
            }

            public void centered(String text, int centerX, int y, int argb) {
            }

            public int width(String text) {
                return text.length() * 6;
            }

            public void face(int x, int y, int size) {
            }

            public void hat(int x, int y, int size, int[] hole) {
            }

            public void push() {
            }

            public void translate(float x, float y) {
            }

            public void scale(float factor) {
            }

            public void pop() {
            }

            public void batch(Runnable draw) {
                draw.run();
            }
        };
        check(com.hex.kagune.control.client.QuestJournalView.UI_WIDTH <= 680 && com.hex.kagune.control.client.QuestJournalView.UI_HEIGHT <= 400,
            "a compact box, not the whole screen");
        QuestLog log = new QuestLog();
        for (com.hex.kagune.control.client.QuestJournalView.Tab tab : com.hex.kagune.control.client.QuestJournalView.Tab.values()) {
            for (double time : new double[] {0.0, 0.1, 2.0}) {
                java.util.Map<String, com.hex.kagune.control.client.UkakuMenuView.Rect> hits = com.hex.kagune.control.client.QuestJournalView.draw(none,
                    new com.hex.kagune.control.client.QuestJournalView.Model(com.hex.kagune.control.KagunePath.RINKAKU, 300, log, 0L, tab, null), 300, 200, time);
                for (com.hex.kagune.control.client.QuestJournalView.Tab each : com.hex.kagune.control.client.QuestJournalView.Tab.values()) {
                    check(hits.containsKey("tab:" + each.name()), tab + ": the " + each + " tab is there to click");
                }
                for (com.hex.kagune.control.client.UkakuMenuView.Rect rect : hits.values()) {
                    check(rect.x() >= 0 && rect.y() >= 0 && rect.x() + rect.width() <= com.hex.kagune.control.client.QuestJournalView.UI_WIDTH
                        && rect.y() + rect.height() <= com.hex.kagune.control.client.QuestJournalView.UI_HEIGHT, tab + ": everything to click inside the box");
                }
                if (tab == com.hex.kagune.control.client.QuestJournalView.Tab.PATH) {
                    for (Quest quest : QuestBook.all()) {
                        if (quest.line() == Quest.Line.PATH && quest.path() == com.hex.kagune.control.KagunePath.RINKAKU) {
                            check(hits.containsKey("quest:" + quest.id()), "the road: " + quest.id() + " is on it, to click");
                        }
                    }
                }
            }
        }
        try {
            String view = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/client/QuestJournalView.java"));
            String screen = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/client/QuestScreen.java"));
            check(!view.contains("organicPanel") && !view.contains("drawVein") && view.contains("QuestStyle.backdrop("),
                "the organic board and its veins gone: the clean plate");
            check(!screen.contains("this.m_280273_(graphics)") && !screen.contains("graphics.m_280024_(0, 0, this.f_96543_, this.f_96544_"),
                "no full-screen overlay: the world seen round the box");
        } catch (java.io.IOException failure) {
            check(false, "the journal's source: " + failure);
        }
    }

    /** 4.1.58: a canvas that draws nothing and keeps every word drawn with where it lands (the box's coordinates, scaled as drawn). */
    static final class Recorder implements com.hex.kagune.control.client.ui.UiCanvas {
        final java.util.List<int[]> texts = new java.util.ArrayList<>();
        final java.util.List<String> words = new java.util.ArrayList<>();
        private final float[] at = {0.0F, 0.0F, 1.0F};
        private final java.util.Deque<float[]> stack = new java.util.ArrayDeque<>();

        public void fill(int x0, int y0, int x1, int y1, int argb) {
        }

        public void gradient(int x0, int y0, int x1, int y1, int top, int bottom) {
        }

        public void text(String text, int x, int y, int argb, boolean shadow) {
            this.texts.add(new int[] {Math.round(this.at[0] + x * this.at[2]), Math.round(this.at[1] + y * this.at[2]), Math.round(this.width(text) * this.at[2])});
            this.words.add(text);
        }

        public void centered(String text, int centerX, int y, int argb) {
            this.text(text, centerX - this.width(text) / 2, y, argb, false);
        }

        public int width(String text) {
            return text.length() * 6;
        }

        public void face(int x, int y, int size) {
        }

        public void hat(int x, int y, int size, int[] hole) {
        }

        public void push() {
            this.stack.push(this.at.clone());
        }

        public void translate(float x, float y) {
            this.at[0] += x * this.at[2];
            this.at[1] += y * this.at[2];
        }

        public void scale(float factor) {
            this.at[2] *= factor;
        }

        public void pop() {
            System.arraycopy(this.stack.pop(), 0, this.at, 0, 3);
        }

        public void batch(Runnable draw) {
            draw.run();
        }

        void clear() {
            this.texts.clear();
            this.words.clear();
        }
    }

    /** 4.1.58: everything clickable in {@code hits} inside a {@code width} x {@code height} box and apart; every word inside it. */
    static void boxed(java.util.Map<String, com.hex.kagune.control.client.UkakuMenuView.Rect> hits, Recorder canvas, int width, int height, String where) {
        java.util.List<com.hex.kagune.control.client.UkakuMenuView.Rect> rects = new java.util.ArrayList<>(hits.values());
        for (int a = 0; a < rects.size(); a++) {
            com.hex.kagune.control.client.UkakuMenuView.Rect r = rects.get(a);
            check(r.x() >= 0 && r.y() >= 0 && r.x() + r.width() <= width && r.y() + r.height() <= height, where + ": everything to click inside the box");
            for (int b = a + 1; b < rects.size(); b++) {
                com.hex.kagune.control.client.UkakuMenuView.Rect o = rects.get(b);
                check(r.x() + r.width() <= o.x() || o.x() + o.width() <= r.x() || r.y() + r.height() <= o.y() || o.y() + o.height() <= r.y(),
                    where + ": nothing to click over anything else to click");
            }
        }
        for (int i = 0; i < canvas.texts.size(); i++) {
            int[] t = canvas.texts.get(i);
            check(t[0] >= 8 && t[0] + t[2] <= width - 8 && t[1] >= 4 && t[1] + 8 <= height - 4, where + ": the words inside the box: \"" + canvas.words.get(i) + "\"");
        }
    }

    /**
     * 4.1.60 (the owner: "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as interactable as it is,
     * Dialog, mastery menus ALL OF THEM"): the Graft menu is the quest menu's box: its three tabs; every stage, move, mutation and step a card
     * or node to click, and every stage's pill on the moves; everything clickable and every word inside the box, apart; GROW INTO IT drawn
     * only for the stage picked and only while the server would take it (GraftServer.pickStage: its mastery, its quest, never the kakuja,
     * never the stage he is in); UNLOCK only while the server would spend a point on it (GraftServer: not had, a point, its quest); what opens
     * a gate named; the moves of a stage still waiting for its quest never ticked; the screen keeps no overlay.
     */
    static void graftMenu() {
        Recorder canvas = new Recorder();
        int width = com.hex.kagune.control.client.GraftMenuView.BOX_WIDTH;
        int height = com.hex.kagune.control.client.GraftMenuView.BOX_HEIGHT;
        check(width <= 760 && height <= 460, "the Graft menu a compact box, the world seen round it");
        java.util.Set<com.hex.kagune.control.quest.Gate> none = java.util.EnumSet.noneOf(com.hex.kagune.control.quest.Gate.class);
        java.util.Set<com.hex.kagune.control.quest.Gate> some = java.util.EnumSet.of(com.hex.kagune.control.quest.Gate.GRAFT_CENTIPEDE);
        com.hex.kagune.control.client.QuestLocks[] locks = {com.hex.kagune.control.client.QuestLocks.ALL,
            new com.hex.kagune.control.client.QuestLocks(none::contains, 0), new com.hex.kagune.control.client.QuestLocks(some::contains, 2)};
        int[] masks = {0, com.hex.kagune.control.graft.GraftMutation.NOT_THAT_EASILY.bit(), com.hex.kagune.control.graft.GraftMutation.all()};
        java.util.List<String> picks = new java.util.ArrayList<>(java.util.Arrays.asList(null, "junk", "pick:s:nope", "step:450", "step:900", "step:0"));
        for (com.hex.kagune.control.graft.GraftStage stage : com.hex.kagune.control.graft.GraftStage.values()) {
            picks.add("pick:s:" + stage.id());
        }
        for (String move : new String[] {"LASH_1", "RIP_1", "HUNGRY_MAW", "CRUCIFIXION", "CURTAIN", "CHECKMATE", "PERFORMANCE", "STEP"}) {
            picks.add("pick:m:" + move);
        }
        for (com.hex.kagune.control.graft.GraftMutation mutation : com.hex.kagune.control.graft.GraftMutation.values()) {
            picks.add("pick:u:" + mutation.id());
        }
        long seen = 0;
        int grows = 0;
        int unlocks = 0;
        int shut = 0;
        for (com.hex.kagune.control.client.GraftMenuView.Tab tab : com.hex.kagune.control.client.GraftMenuView.Tab.values()) {
            for (int mastery : new int[] {0, 260, 500, 760, 950, 1000}) {
                for (com.hex.kagune.control.graft.GraftStage in : new com.hex.kagune.control.graft.GraftStage[] {com.hex.kagune.control.graft.GraftStage.TRANSPLANT,
                    com.hex.kagune.control.graft.GraftStage.CENTIPEDE, com.hex.kagune.control.graft.GraftStage.DRAGON}) {
                    for (boolean kakuja : new boolean[] {false, true}) {
                        if (kakuja && mastery < com.hex.kagune.control.graft.GraftStage.KAKUJA.mastery()) {
                            continue;
                        }
                        for (com.hex.kagune.control.client.QuestLocks lock : locks) {
                            for (int mask : masks) {
                                for (String pick : picks) {
                                    canvas.clear();
                                    com.hex.kagune.control.client.GraftMenuView.Model model = new com.hex.kagune.control.client.GraftMenuView.Model(mastery,
                                        mastery > 0, in, kakuja, 64.0F, 100.0F, mask, lock, tab, pick);
                                    java.util.Map<String, com.hex.kagune.control.client.UkakuMenuView.Rect> hits =
                                        com.hex.kagune.control.client.GraftMenuView.draw(canvas, model, 300, 200, (mastery + mask) % 7);
                                    String where = "graft " + tab + " at " + mastery + " in " + in + (kakuja ? " (kakuja)" : "") + " (" + pick + ")";
                                    seen += canvas.texts.size();
                                    for (com.hex.kagune.control.client.GraftMenuView.Tab each : com.hex.kagune.control.client.GraftMenuView.Tab.values()) {
                                        check(hits.containsKey("tab:" + each.name()), where + ": the " + each + " tab");
                                    }
                                    boxed(hits, canvas, width, height, where);
                                    for (String word : canvas.words) {
                                        check(!word.endsWith("quest: ") && !word.endsWith("quest:") && !word.endsWith("with ") && !word.endsWith("for ")
                                            && !word.contains("its quest"), where + ": what opens it named: \"" + word + "\"");
                                    }
                                    java.util.List<String> grow = hits.keySet().stream().filter(k -> k.startsWith("stage:")).toList();
                                    check(grow.size() <= 1, where + ": one GROW INTO IT at most");
                                    for (String key : grow) {
                                        com.hex.kagune.control.graft.GraftStage stage = com.hex.kagune.control.graft.GraftStage.fromId(key.substring(6));
                                        grows++;
                                        check(tab == com.hex.kagune.control.client.GraftMenuView.Tab.STAGES && stage != null && !stage.kakuja() && stage != in
                                            && stage.unlocked(mastery) && lock.passes(com.hex.kagune.control.quest.Gates.graft(stage)),
                                            where + ": GROW INTO IT only for a stage the server would take (" + key + ")");
                                    }
                                    java.util.List<String> unlock = hits.keySet().stream().filter(k -> k.startsWith("mutation:")).toList();
                                    check(unlock.size() <= 1, where + ": one UNLOCK at most");
                                    for (String key : unlock) {
                                        com.hex.kagune.control.graft.GraftMutation mutation = com.hex.kagune.control.graft.GraftMutation.fromId(key.substring(9));
                                        unlocks++;
                                        check(tab == com.hex.kagune.control.client.GraftMenuView.Tab.MUTATIONS && mutation != null && !mutation.in(mask)
                                            && com.hex.kagune.control.graft.GraftMutation.canUnlock(mastery, mask, mutation, lock.points())
                                            && lock.passes(com.hex.kagune.control.quest.Gates.graft(mutation)),
                                            where + ": UNLOCK only for a mutation the server would unlock (" + key + ")");
                                    }
                                    switch (tab) {
                                        case STAGES -> {
                                            check(hits.containsKey("eyes"), where + ": the kakugan");
                                            for (com.hex.kagune.control.graft.GraftStage stage : com.hex.kagune.control.graft.GraftStage.values()) {
                                                check(hits.containsKey("pick:s:" + stage.id()), where + ": " + stage + " a card to click");
                                            }
                                            for (Integer step : com.hex.kagune.control.client.GraftMenuView.steps().keySet()) {
                                                check(hits.containsKey("step:" + step), where + ": the step at " + step);
                                            }
                                            // A stage waiting for its quest: none of its moves ticked, whatever the mastery.
                                            if (pick != null && pick.startsWith("pick:s:")) {
                                                com.hex.kagune.control.graft.GraftStage looked = com.hex.kagune.control.graft.GraftStage.fromId(pick.substring(7));
                                                if (looked != null && looked.unlocked(mastery) && !lock.passes(com.hex.kagune.control.quest.Gates.graft(looked))) {
                                                    shut++;
                                                    check(canvas.words.stream().anyMatch(w -> w.equals("0 / " + 6) || w.matches("0 / \\d+")),
                                                        where + ": a stage still shut has none of its moves");
                                                }
                                            }
                                        }
                                        case MOVES -> {
                                            for (com.hex.kagune.control.graft.GraftStage stage : com.hex.kagune.control.graft.GraftStage.values()) {
                                                check(hits.containsKey("pick:s:" + stage.id()), where + ": " + stage + "'s pill to click");
                                            }
                                            check(hits.keySet().stream().filter(k -> k.startsWith("pick:m:")).count() >= 5, where + ": the stage's moves to click");
                                        }
                                        default -> {
                                            for (com.hex.kagune.control.graft.GraftMutation mutation : com.hex.kagune.control.graft.GraftMutation.values()) {
                                                check(hits.containsKey("pick:u:" + mutation.id()), where + ": " + mutation + " a card to click");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        check(seen > 100000 && grows > 100 && unlocks > 50 && shut > 20,
            "the Graft menu's words and its buttons all drawn and checked (" + seen + ", " + grows + ", " + unlocks + ", " + shut + ")");
        // The previews: every stage its own picture, every mark inside its tile, at every size the menu draws and as it breathes.
        java.util.Set<Integer> looks = new java.util.HashSet<>();
        for (com.hex.kagune.control.graft.GraftStage stage : com.hex.kagune.control.graft.GraftStage.values()) {
            for (int[] size : new int[][] {{174, 120}, {120, 80}, {300, 200}}) {
                for (int mastery : new int[] {stage.mastery(), 1000}) {
                    for (double time = 0.0; time < 12.0; time += 0.7) {
                        com.hex.kagune.control.client.PathChoiceLayout.Box box = new com.hex.kagune.control.client.PathChoiceLayout.Box(10, 20, size[0], size[1]);
                        java.util.List<com.hex.kagune.control.client.PathSilhouette.Mark> marks =
                            com.hex.kagune.control.client.PathSilhouette.graftForm(box, time, 1.0F, stage, mastery, 0x0C080E);
                        check(marks.size() > 40, stage + " at " + size[0] + "x" + size[1] + ": drawn (" + marks.size() + " marks)");
                        for (com.hex.kagune.control.client.PathSilhouette.Mark mark : marks) {
                            float[] r = reach(mark);
                            check(r[0] >= box.x() && r[1] >= box.y() && r[2] <= box.x() + box.width() && r[3] <= box.y() + box.height(),
                                stage + " at " + size[0] + "x" + size[1] + ", " + time + " s: every mark inside its tile " + java.util.Arrays.toString(r));
                        }
                        if (time == 0.0 && size[0] == 174 && mastery == 1000) {
                            looks.add(marks.size() * 31 + marks.hashCode());
                        }
                    }
                }
            }
        }
        check(looks.size() == com.hex.kagune.control.graft.GraftStage.values().length, "every stage drawn as itself, none sharing a picture");
        try {
            String screen = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/client/GraftMenuScreen.java"));
            check(!screen.contains("this.m_280273_(graphics)") && !screen.contains("graphics.m_280024_(0, 0, this.f_96543_, this.f_96544_"),
                "no full-screen overlay round the Graft menu");
            check(screen.contains("GraftMenuView.BOX_WIDTH") && screen.contains("this.picked = key;") && screen.contains("key.startsWith(\"stage:\")")
                && screen.contains("key.startsWith(\"mutation:\")"), "the screen sized from the box; a stage grown into and a mutation unlocked only by their buttons");
        } catch (java.io.IOException failure) {
            check(false, "the Graft screen's source: " + failure);
        }
    }

    /**
     * 4.1.61 (the owner: "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as interactable as it is,
     * Dialog, mastery menus ALL OF THEM"): the Pale menu is the quest menu's box: its four tabs; every stage, move, the Owl and its moves,
     * every mutation and step a card or node to click; everything clickable and every word inside the box, apart; UNLOCK drawn only for the
     * mutation picked and only while the server would spend a point on it (PaleRevive.unlock: not had, a point left); Instant Big Punch's
     * switch only once Witness This is hers (PaleRevive.instantPunch); the Owl drawn inside its tile; the screen keeps no overlay.
     */
    static void paleMenu() {
        Recorder canvas = new Recorder();
        int width = com.hex.kagune.control.client.PaleMenuView.BOX_WIDTH;
        int height = com.hex.kagune.control.client.PaleMenuView.BOX_HEIGHT;
        check(width <= 760 && height <= 460, "the Pale menu a compact box, the world seen round it");
        com.hex.kagune.control.kakugan.KakuganEyes eyes = com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT;
        int witness = com.hex.kagune.control.pale.PaleMutation.WITNESS_THIS.bit();
        int[] masks = {0, com.hex.kagune.control.pale.PaleMutation.NOT_THAT_EASILY.bit(), witness, witness | com.hex.kagune.control.pale.PaleMutation.INSTANT_PUNCH,
            com.hex.kagune.control.pale.PaleMutation.all()};
        java.util.List<String> picks = new java.util.ArrayList<>(java.util.Arrays.asList(null, "junk", "pick:s:0", "pick:s:2", "pick:s:9", "step:350",
            "step:750", "milestone:900", "pick:m:BLITZ", "pick:m:HAMMER", "pick:m:ARM", "pick:o:OWL", "pick:o:WHIRL", "pick:o:HAND"));
        for (com.hex.kagune.control.pale.PaleMutation mutation : com.hex.kagune.control.pale.PaleMutation.values()) {
            picks.add("pick:u:" + mutation.id());
        }
        long seen = 0;
        int unlocks = 0;
        int switches = 0;
        for (com.hex.kagune.control.client.PaleMenuView.Tab tab : com.hex.kagune.control.client.PaleMenuView.Tab.values()) {
            for (int mastery : new int[] {0, 120, 320, 520, 720, 820, 910, 1000}) {
                for (int mask : masks) {
                    for (long revive : new long[] {0L, 1300L}) {
                        for (String pick : picks) {
                            canvas.clear();
                            com.hex.kagune.control.client.PaleMenuView.Model model = new com.hex.kagune.control.client.PaleMenuView.Model(mastery,
                                mastery % 2 == 0, eyes, mask, revive, tab, pick);
                            java.util.Map<String, com.hex.kagune.control.client.UkakuMenuView.Rect> hits =
                                com.hex.kagune.control.client.PaleMenuView.draw(canvas, model, 300, 200, 0.5 + (mastery + mask) % 5); // (after the cards slide in)
                            String where = "pale " + tab + " at " + mastery + " (" + Integer.toHexString(mask) + ", " + pick + ")";
                            seen += canvas.texts.size();
                            for (com.hex.kagune.control.client.PaleMenuView.Tab each : com.hex.kagune.control.client.PaleMenuView.Tab.values()) {
                                check(hits.containsKey("tab:" + each.name()), where + ": the " + each + " tab");
                            }
                            boxed(hits, canvas, width, height, where);
                            java.util.List<String> unlock = hits.keySet().stream().filter(k -> k.startsWith("mutation:")).toList();
                            check(unlock.size() <= 1, where + ": one UNLOCK at most");
                            for (String key : unlock) {
                                com.hex.kagune.control.pale.PaleMutation mutation = com.hex.kagune.control.pale.PaleMutation.byOrdinal(Integer.parseInt(key.substring(9)));
                                unlocks++;
                                check(tab == com.hex.kagune.control.client.PaleMenuView.Tab.MUTATIONS && mutation != null
                                    && com.hex.kagune.control.pale.PaleMutation.canUnlock(mastery, mask, mutation),
                                    where + ": UNLOCK only for a mutation the server would unlock (" + key + ")");
                            }
                            if (hits.containsKey("instant")) {
                                switches++;
                                check(tab == com.hex.kagune.control.client.PaleMenuView.Tab.MUTATIONS && com.hex.kagune.control.pale.PaleMutation.WITNESS_THIS.in(mask),
                                    where + ": Instant Big Punch's switch only once Witness This is hers");
                            }
                            switch (tab) {
                                case FORM -> {
                                    check(hits.containsKey("eyes"), where + ": the kakugan");
                                    for (int i = 0; i < 3; i++) {
                                        check(hits.containsKey("pick:s:" + i), where + ": stage " + i + " a card to click");
                                    }
                                    for (Integer step : com.hex.kagune.control.client.PaleMenuView.steps().keySet()) {
                                        check(hits.containsKey("step:" + step), where + ": the step at " + step);
                                    }
                                }
                                case MOVES, OWL -> {
                                    String prefix = tab == com.hex.kagune.control.client.PaleMenuView.Tab.OWL ? "pick:o:" : "pick:m:";
                                    long cards = hits.keySet().stream().filter(k -> k.startsWith(prefix)).count();
                                    check(cards >= 8, where + ": every move a card to click (" + cards + ")");
                                    check(hits.keySet().stream().noneMatch(k -> k.startsWith(tab == com.hex.kagune.control.client.PaleMenuView.Tab.OWL ? "pick:m:"
                                        : "pick:o:")), where + ": only this tab's moves");
                                }
                                default -> {
                                    for (com.hex.kagune.control.pale.PaleMutation mutation : com.hex.kagune.control.pale.PaleMutation.values()) {
                                        check(hits.containsKey("pick:u:" + mutation.id()), where + ": " + mutation + " a card to click");
                                    }
                                    for (int milestone : com.hex.kagune.control.pale.PaleMutation.MILESTONES) {
                                        check(hits.containsKey("milestone:" + milestone), where + ": the milestone at " + milestone);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        check(seen > 50000 && unlocks > 50 && switches > 20, "the Pale menu's words and its buttons all drawn and checked (" + seen + ", " + unlocks + ", "
            + switches + ")");
        // The Owl in its tile: every mark inside, at every size it is drawn and as it moves.
        for (int[] size : new int[][] {{174, 120}, {120, 80}, {300, 200}, {80, 140}}) {
            for (double time = 0.0; time < 10.0; time += 0.9) {
                com.hex.kagune.control.client.PathChoiceLayout.Box box = new com.hex.kagune.control.client.PathChoiceLayout.Box(6, 9, size[0], size[1]);
                java.util.List<com.hex.kagune.control.client.PathSilhouette.Mark> marks = com.hex.kagune.control.client.PathSilhouette.owl(box, time, 1.0F, 0x0B090B);
                check(marks.size() > 60, "the Owl drawn at " + size[0] + "x" + size[1] + " (" + marks.size() + " marks)");
                for (com.hex.kagune.control.client.PathSilhouette.Mark mark : marks) {
                    float[] r = reach(mark);
                    check(r[0] >= box.x() && r[1] >= box.y() && r[2] <= box.x() + box.width() && r[3] <= box.y() + box.height(),
                        "the Owl at " + size[0] + "x" + size[1] + ", " + time + " s: every mark inside its tile " + java.util.Arrays.toString(r));
                }
            }
        }
        // The stages the menu shows are the ones the game names.
        int[] starts = {0, 250, 500};
        for (int i = 0; i < starts.length; i++) {
            check(com.hex.kagune.control.pale.PaleUnlock.stageName(starts[i]).equals(com.hex.kagune.control.pale.PaleUnlock.stageName(starts[i] + 249 - (i == 2 ? 0 : 0)))
                && (i == 0 || !com.hex.kagune.control.pale.PaleUnlock.stageName(starts[i] - 1).equals(com.hex.kagune.control.pale.PaleUnlock.stageName(starts[i]))),
                "the menu's stage " + i + " begins where the game's does (" + starts[i] + ")");
        }
        try {
            String screen = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/client/PaleMenuScreen.java"));
            check(!screen.contains("this.m_280273_(graphics)") && !screen.contains("graphics.m_280024_(0, 0, this.f_96543_, this.f_96544_"),
                "no full-screen overlay round the Pale menu");
            check(screen.contains("PaleMenuView.BOX_WIDTH") && screen.contains("this.picked = key;") && screen.contains("key.startsWith(\"mutation:\")")
                && screen.contains("key.equals(\"instant\")"), "the screen sized from the box; a mutation unlocked and the switch flipped only by their buttons");
        } catch (java.io.IOException failure) {
            check(false, "the Pale screen's source: " + failure);
        }
    }

    /**
     * 4.1.65 (the owner: "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as interactable as it is,
     * Dialog, mastery menus ALL OF THEM"): the king's choice is the quest menu's box: both cards, the button held to choose and Decide later
     * to click, inside the box and apart, every word inside it; the screen sized from the box, no overlay.
     */
    static void kingChoice() {
        Recorder canvas = new Recorder();
        int width = com.hex.kagune.control.client.KingChoiceView.BOX_WIDTH;
        int height = com.hex.kagune.control.client.KingChoiceView.BOX_HEIGHT;
        check(width <= 760 && height <= 460, "the king's choice a compact box, the world seen round it");
        long seen = 0;
        for (com.hex.kagune.control.king.KingForm picked : new com.hex.kagune.control.king.KingForm[] {com.hex.kagune.control.king.KingForm.NONE,
            com.hex.kagune.control.king.KingForm.WAR, com.hex.kagune.control.king.KingForm.PEACE}) {
            for (double hold : new double[] {0.0, 0.4, 1.0}) {
                for (boolean peace : new boolean[] {false, true}) {
                    for (double[] mouse : new double[][] {{-1, -1}, {190, 200}, {550, 200}, {370, 386}, {370, 426}}) {
                        canvas.clear();
                        java.util.Map<String, com.hex.kagune.control.client.UkakuMenuView.Rect> hits = com.hex.kagune.control.client.KingChoiceView.draw(canvas,
                            new com.hex.kagune.control.client.KingChoiceView.Model(picked, hold, peace), mouse[0], mouse[1], 2.0);
                        String where = "king's choice " + picked + " held " + hold + (peace ? "" : " (Peace sealed)");
                        seen += canvas.texts.size();
                        check(hits.containsKey("card:war") && hits.containsKey("card:peace") && hits.containsKey("choose") && hits.containsKey("later"),
                            where + ": both cards, the button and Decide later to click");
                        boxed(hits, canvas, width, height, where);
                    }
                }
            }
        }
        check(seen > 500, "the king's choice's words drawn and checked (" + seen + ")");
        try {
            String screen = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/client/KingChoiceScreen.java"));
            check(screen.contains("KingChoiceView.BOX_WIDTH") && !screen.contains("KingChoiceView.UI_WIDTH") && !screen.contains("m_280273_")
                && !screen.contains("m_280024_"), "the king's choice sized from its box, no overlay over the world");
        } catch (java.io.IOException failure) {
            check(false, "the king's choice screen's source: " + failure);
        }
    }

    /** How far a mark reaches: {left, top, right, bottom}, its thickness included (as the menus draw it, to the pixel). */
    static float[] reach(com.hex.kagune.control.client.PathSilhouette.Mark mark) {
        if (mark instanceof com.hex.kagune.control.client.PathSilhouette.Stroke s) {
            float half = Math.max(Math.max(s.t0(), s.t1()), 1.0F) / 2.0F + 0.5F;
            return new float[] {Math.min(s.x0(), s.x1()) - half, Math.min(s.y0(), s.y1()) - half, Math.max(s.x0(), s.x1()) + half, Math.max(s.y0(), s.y1()) + half};
        }
        if (mark instanceof com.hex.kagune.control.client.PathSilhouette.Disc d) {
            float r = Math.max(1.0F, d.r()) + 0.5F;
            return new float[] {d.cx() - r, d.cy() - r, d.cx() + r, d.cy() + r};
        }
        com.hex.kagune.control.client.PathSilhouette.Shape shape = (com.hex.kagune.control.client.PathSilhouette.Shape)mark;
        float[] r = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (int i = 0; i < shape.x().length; i++) {
            r[0] = Math.min(r[0], shape.x()[i]);
            r[1] = Math.min(r[1], shape.y()[i]);
            r[2] = Math.max(r[2], shape.x()[i]);
            r[3] = Math.max(r[3], shape.y()[i]);
        }
        return r;
    }

    /**
     * 4.1.59 (the owner: "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as interactable as it is,
     * Dialog, mastery menus ALL OF THEM"): the Bikaku menu is the quest menu's box: its four tabs; every form, move, the kakuja's moves and
     * every mutation a card to click; everything clickable and every word inside the box, apart; a form taken only by its USE THIS FORM
     * button, drawn only for the form picked and only while the server would take it (its mastery, its quest, picked in the menu, not on);
     * what opens a gate named (a quest or a teacher's lesson, never "its quest:" and nothing); the screen keeps no overlay.
     */
    static void bikakuMenu() {
        Recorder canvas = new Recorder();
        int width = com.hex.kagune.control.client.BikakuMenuView.BOX_WIDTH;
        int height = com.hex.kagune.control.client.BikakuMenuView.BOX_HEIGHT;
        check(width <= 760 && height <= 460, "the Bikaku menu a compact box, the world seen round it");
        java.util.Set<com.hex.kagune.control.quest.Gate> none = java.util.EnumSet.noneOf(com.hex.kagune.control.quest.Gate.class);
        java.util.Set<com.hex.kagune.control.quest.Gate> worms = java.util.EnumSet.of(com.hex.kagune.control.quest.Gate.BIKAKU_WORMS);
        com.hex.kagune.control.client.QuestLocks[] locks = {com.hex.kagune.control.client.QuestLocks.ALL,
            new com.hex.kagune.control.client.QuestLocks(none::contains, 0), new com.hex.kagune.control.client.QuestLocks(worms::contains, 0)};
        int[] masks = {0, com.hex.kagune.control.bikaku.BikakuMutation.GRAND_DEVOURING.bit(), com.hex.kagune.control.bikaku.BikakuMutation.all()};
        String[] picks = {null, "pick:f:ONE", "pick:f:WALKER", "pick:m:M1", "pick:m:REGENERATION", "pick:k:KAKUJA", "pick:k:UNDYING",
            "pick:u:POTENTIAL_UNLOCK", "step:450", "step:750", "junk"};
        long seen = 0;
        int takes = 0;
        for (com.hex.kagune.control.client.BikakuMenuView.Tab tab : com.hex.kagune.control.client.BikakuMenuView.Tab.values()) {
            for (int mastery : new int[] {0, 300, 600, 800, 1000}) {
                for (com.hex.kagune.control.bikaku.BikakuForm on : com.hex.kagune.control.bikaku.BikakuForm.values()) {
                    for (com.hex.kagune.control.client.QuestLocks lock : locks) {
                        for (int mask : masks) {
                            for (String pick : picks) {
                                for (double time : new double[] {0.0, 3.0}) {
                                    canvas.clear();
                                    com.hex.kagune.control.client.BikakuMenuView.Model model = new com.hex.kagune.control.client.BikakuMenuView.Model(mastery,
                                        mastery > 0, 64.0F, 100.0F, on.ordinal(), mask, 120L, lock, tab, pick);
                                    java.util.Map<String, com.hex.kagune.control.client.UkakuMenuView.Rect> hits =
                                        com.hex.kagune.control.client.BikakuMenuView.draw(canvas, model, 300, 200, time);
                                    String where = "bikaku " + tab + " at " + mastery + " in " + on + " (" + pick + ")";
                                    seen += canvas.texts.size();
                                    for (com.hex.kagune.control.client.BikakuMenuView.Tab each : com.hex.kagune.control.client.BikakuMenuView.Tab.values()) {
                                        check(hits.containsKey("tab:" + each.name()), where + ": the " + each + " tab");
                                    }
                                    boxed(hits, canvas, width, height, where);
                                    for (String word : canvas.words) {
                                        check(!word.endsWith("quest: ") && !word.endsWith("quest:") && !word.endsWith("with "), where + ": what opens it named: \"" + word + "\"");
                                    }
                                    java.util.List<String> take = hits.keySet().stream().filter(k -> k.matches("form\\d+")).toList();
                                    check(take.size() <= 1, where + ": one USE THIS FORM at most");
                                    for (String key : take) {
                                        com.hex.kagune.control.bikaku.BikakuForm form = com.hex.kagune.control.bikaku.BikakuForm.byOrdinal(Integer.parseInt(key.substring(4)));
                                        takes++;
                                        boolean gated = form.worms() > 1 && !lock.passes(com.hex.kagune.control.quest.Gate.BIKAKU_WORMS);
                                        check(tab == com.hex.kagune.control.client.BikakuMenuView.Tab.FORMS && form != on && form.unlocked(mastery) && form.picked()
                                            && !gated, where + ": USE THIS FORM only for a form the server would take");
                                    }
                                    switch (tab) {
                                        case FORMS -> {
                                            check(hits.containsKey("eyes"), where + ": the kakugan");
                                            for (com.hex.kagune.control.bikaku.BikakuForm form : com.hex.kagune.control.bikaku.BikakuForm.values()) {
                                                check(hits.containsKey("pick:f:" + form.name()), where + ": " + form + " a card to click");
                                            }
                                            for (Integer step : com.hex.kagune.control.client.BikakuMenuView.steps().keySet()) {
                                                check(hits.containsKey("step:" + step), where + ": the step at " + step);
                                            }
                                        }
                                        case MOVES, KAKUJA -> {
                                            boolean kakuja = tab == com.hex.kagune.control.client.BikakuMenuView.Tab.KAKUJA;
                                            check(hits.containsKey(kakuja ? "pick:k:KAKUJA" : "pick:m:M1"), where + ": the first card");
                                            for (com.hex.kagune.control.bikaku.BikakuMove move : com.hex.kagune.control.bikaku.BikakuMove.values()) {
                                                if (move != com.hex.kagune.control.bikaku.BikakuMove.NONE && move.kakuja() == kakuja) {
                                                    check(hits.containsKey((kakuja ? "pick:k:" : "pick:m:") + move.name()), where + ": " + move + " a card to click");
                                                }
                                            }
                                        }
                                        default -> {
                                            for (com.hex.kagune.control.bikaku.BikakuMutation mutation : com.hex.kagune.control.bikaku.BikakuMutation.values()) {
                                                check(hits.containsKey("pick:u:" + mutation.name()), where + ": " + mutation + " a card to click");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        check(seen > 100000 && takes > 100, "the Bikaku menu's words and its USE buttons all drawn and checked (" + seen + ", " + takes + ")");
        try {
            String screen = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/client/BikakuMenuScreen.java"));
            check(!screen.contains("this.m_280273_(graphics)") && !screen.contains("graphics.m_280024_(0, 0, this.f_96543_, this.f_96544_"),
                "no full-screen overlay round the Bikaku menu");
            check(screen.contains("BikakuMenuView.BOX_WIDTH") && screen.contains("this.picked = key;") && screen.contains("this.hits.get(\"form\" + form.ordinal())"),
                "the screen sized from the box; a form taken only by its button; a card only looked at");
        } catch (java.io.IOException failure) {
            check(false, "the Bikaku screen's source: " + failure);
        }
    }

    /**
     * 4.1.58 (the owner: "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as interactable as it is,
     * Dialog, mastery menus ALL OF THEM"): the Rinkaku menu is the quest menu's box: both tabs; every form, the passive and every step of the
     * road to click; War and Peace to click; everything clickable and every word inside the box, apart; a form put on only by its WEAR THIS
     * FORM button, drawn only for the form picked and only while it may be worn; the choice of War or Peace opened only by its button, only
     * while it may be made; the screen keeps no overlay.
     */
    static void rinkakuMenu() {
        Recorder canvas = new Recorder();
        int width = com.hex.kagune.control.client.RinkakuMenuView.BOX_WIDTH;
        int height = com.hex.kagune.control.client.RinkakuMenuView.BOX_HEIGHT;
        check(width <= 760 && height <= 460, "the Rinkaku menu a compact box, the world seen round it");
        java.util.Set<com.hex.kagune.control.quest.Gate> none = java.util.EnumSet.noneOf(com.hex.kagune.control.quest.Gate.class);
        com.hex.kagune.control.client.QuestLocks[] locks = {com.hex.kagune.control.client.QuestLocks.ALL,
            new com.hex.kagune.control.client.QuestLocks(none::contains, 0)};
        com.hex.kagune.control.king.KingForm no = com.hex.kagune.control.king.KingForm.NONE;
        com.hex.kagune.control.king.KingForm war = com.hex.kagune.control.king.KingForm.WAR;
        com.hex.kagune.control.king.KingForm peace = com.hex.kagune.control.king.KingForm.PEACE;
        com.hex.kagune.control.client.RinkakuMenuView.King[] kings = {com.hex.kagune.control.client.RinkakuMenuView.King.ASLEEP,
            new com.hex.kagune.control.client.RinkakuMenuView.King(true, no, no, no), new com.hex.kagune.control.client.RinkakuMenuView.King(true, war, war, no),
            new com.hex.kagune.control.client.RinkakuMenuView.King(true, war, no, no), new com.hex.kagune.control.client.RinkakuMenuView.King(true, peace, no, no)};
        String[] picks = {null, "pick:seed", "pick:full", "pick:passive", "step:350", "step:1000", "pick:war", "pick:peace", "junk", "pick:nope"};
        long seen = 0;
        int wears = 0;
        int chooses = 0;
        com.hex.kagune.gameplay.KaguneForm[] forms = com.hex.kagune.control.RinkakuForms.forms();
        for (com.hex.kagune.control.client.RinkakuMenuView.Tab tab : com.hex.kagune.control.client.RinkakuMenuView.Tab.values()) {
            for (int mastery : new int[] {0, 300, 600, 800, 1000}) {
                for (com.hex.kagune.gameplay.KaguneForm worn : forms) {
                    for (com.hex.kagune.control.client.QuestLocks lock : locks) {
                        for (com.hex.kagune.control.client.RinkakuMenuView.King king : kings) {
                            for (String pick : picks) {
                                for (double time : new double[] {0.0, 3.0}) {
                                    canvas.clear();
                                    com.hex.kagune.control.client.RinkakuMenuView.Model model = new com.hex.kagune.control.client.RinkakuMenuView.Model(mastery,
                                        mastery > 0, worn, 50.0F, 100.0F, 0L, null, lock, king, tab, pick);
                                    java.util.Map<String, com.hex.kagune.control.client.UkakuMenuView.Rect> hits =
                                        com.hex.kagune.control.client.RinkakuMenuView.draw(canvas, model, 300, 200, time);
                                    String where = "rinkaku " + tab + " at " + mastery + " in " + worn + " (" + king + ", " + pick + ")";
                                    seen += canvas.texts.size();
                                    check(hits.containsKey("tab:FORMS") && hits.containsKey("tab:KING"), where + ": both tabs there to click");
                                    boxed(hits, canvas, width, height, where);
                                    java.util.List<String> wear = hits.keySet().stream().filter(k -> k.startsWith("form:")).toList();
                                    java.util.List<String> choose = hits.keySet().stream().filter(k -> k.startsWith("king:")).toList();
                                    if (tab == com.hex.kagune.control.client.RinkakuMenuView.Tab.FORMS) {
                                        check(hits.containsKey("eyes") && hits.containsKey("pick:passive") && choose.isEmpty(), where + ": the kakugan, the passive; no choice");
                                        for (com.hex.kagune.gameplay.KaguneForm form : forms) {
                                            check(hits.containsKey("pick:" + form.id()), where + ": " + form + " a card to click");
                                        }
                                        for (Integer step : com.hex.kagune.control.client.RinkakuMenuView.steps().keySet()) {
                                            check(hits.containsKey("step:" + step), where + ": the step at " + step + " on the road");
                                        }
                                        check(wear.size() <= 1, where + ": one WEAR button at most");
                                        for (String key : wear) {
                                            com.hex.kagune.gameplay.KaguneForm form = com.hex.kagune.gameplay.KaguneForm.fromId(key.substring(5));
                                            wears++;
                                            check(form != null && form != worn && form.isUnlocked(mastery)
                                                && lock.passes(com.hex.kagune.control.quest.Gates.rinkaku(form)), where + ": WEAR only for a form that may be worn and is not");
                                        }
                                    } else {
                                        check(hits.containsKey("pick:war") && hits.containsKey("pick:peace") && wear.isEmpty(), where + ": War and Peace to click; nothing to wear");
                                        boolean can = king.awake() && king.chosen() == no;
                                        check(choose.size() == (can ? 1 : 0), where + ": the choice's button only while it may be made");
                                        chooses += choose.size();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        check(seen > 100000 && wears > 100 && chooses > 100, "the Rinkaku menu's words, WEAR and choice buttons all drawn and checked (" + seen + ", " + wears
            + ", " + chooses + ")");
        try {
            String screen = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/client/RinkakuMenuScreen.java"));
            check(!screen.contains("this.m_280273_(graphics)") && !screen.contains("graphics.m_280024_(0, 0, this.f_96543_, this.f_96544_"),
                "no full-screen overlay round the Rinkaku menu");
            check(screen.contains("RinkakuMenuView.BOX_WIDTH") && screen.indexOf("new C2SKaguneForm(form)") > screen.indexOf("key.startsWith(\"form:\")")
                && screen.indexOf("this.picked = key;") > 0, "the screen sized from the box; a form worn only by its button; a card only looked at");
        } catch (java.io.IOException failure) {
            check(false, "the Rinkaku screen's source: " + failure);
        }
    }

    /**
     * 4.1.56 (the owner: "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as interactable as it is,
     * Dialog, mastery menus ALL OF THEM"): the Ukaku menu is the quest menu's box: compact (the world round it), both tabs there to click,
     * every stage a card and every step of the road a node to click, everything clickable and every word inside the box, nothing clickable
     * over anything else; a mutation spent only by its UNLOCK button, drawn only while a point can be spent on the one picked; the screen
     * keeps no overlay and spends only on that button.
     */
    static void ukakuMenu() {
        java.util.List<int[]> texts = new java.util.ArrayList<>();
        java.util.List<String> words = new java.util.ArrayList<>();
        float[] at = {0.0F, 0.0F, 1.0F};
        java.util.Deque<float[]> stack = new java.util.ArrayDeque<>();
        com.hex.kagune.control.client.ui.UiCanvas canvas = new com.hex.kagune.control.client.ui.UiCanvas() {
            public void fill(int x0, int y0, int x1, int y1, int argb) {
            }

            public void gradient(int x0, int y0, int x1, int y1, int top, int bottom) {
            }

            public void text(String text, int x, int y, int argb, boolean shadow) {
                texts.add(new int[] {Math.round(at[0] + x * at[2]), Math.round(at[1] + y * at[2]), Math.round(this.width(text) * at[2])});
                words.add(text);
            }

            public void centered(String text, int centerX, int y, int argb) {
                this.text(text, centerX - this.width(text) / 2, y, argb, false);
            }

            public int width(String text) {
                return text.length() * 6;
            }

            public void face(int x, int y, int size) {
            }

            public void hat(int x, int y, int size, int[] hole) {
            }

            public void push() {
                stack.push(at.clone());
            }

            public void translate(float x, float y) {
                at[0] += x * at[2];
                at[1] += y * at[2];
            }

            public void scale(float factor) {
                at[2] *= factor;
            }

            public void pop() {
                float[] back = stack.pop();
                System.arraycopy(back, 0, at, 0, 3);
            }

            public void batch(Runnable draw) {
                draw.run();
            }
        };
        int width = com.hex.kagune.control.client.UkakuMenuView.BOX_WIDTH;
        int height = com.hex.kagune.control.client.UkakuMenuView.BOX_HEIGHT;
        long[] seen = {0L, 0L};
        check(width <= 760 && height <= 460 && width < com.hex.kagune.control.client.UkakuMenuView.UI_WIDTH,
            "the Ukaku menu a compact box, the world seen round it");
        java.util.Set<com.hex.kagune.control.quest.Gate> none = java.util.EnumSet.noneOf(com.hex.kagune.control.quest.Gate.class);
        com.hex.kagune.control.client.QuestLocks[] locks = {com.hex.kagune.control.client.QuestLocks.ALL,
            new com.hex.kagune.control.client.QuestLocks(none::contains, 1)};
        String[] picks = {null, "stage:ONE_WING", "stage:KAKUJA_II", "unlock:RUSHING_DEATH", "pick:0", "pick:1", "pick:3", "pick:4", "nonsense"};
        int[] masks = {0, com.hex.kagune.control.ukaku.UkakuMutation.FIRE_DASH.bit(), com.hex.kagune.control.ukaku.UkakuMutation.all()};
        for (com.hex.kagune.control.client.UkakuMenuView.Tab tab : com.hex.kagune.control.client.UkakuMenuView.Tab.values()) {
            for (int mastery : new int[] {0, 99, 300, 760, 1000}) {
                for (com.hex.kagune.control.ukaku.WingForm form : com.hex.kagune.control.ukaku.WingForm.values()) {
                    for (int mask : masks) {
                        for (com.hex.kagune.control.client.QuestLocks lock : locks) {
                            for (String pick : picks) {
                                for (double time : new double[] {0.0, 0.1, 3.0}) {
                                    texts.clear();
                                    words.clear();
                                    com.hex.kagune.control.client.UkakuMenuView.Model model = new com.hex.kagune.control.client.UkakuMenuView.Model(mastery,
                                        mastery > 0, form, com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT, mask, lock, tab, pick);
                                    java.util.Map<String, com.hex.kagune.control.client.UkakuMenuView.Rect> hits =
                                        com.hex.kagune.control.client.UkakuMenuView.draw(canvas, model, 300, 200, time);
                                    String where = tab + " at " + mastery + " (" + form + ", " + pick + ", t " + time + ")";
                                    check(hits.containsKey("tab:ORGAN") && hits.containsKey("tab:MUTATIONS"), where + ": both tabs there to click");
                                    java.util.List<com.hex.kagune.control.client.UkakuMenuView.Rect> rects = new java.util.ArrayList<>(hits.values());
                                    for (int a = 0; a < rects.size(); a++) {
                                        com.hex.kagune.control.client.UkakuMenuView.Rect r = rects.get(a);
                                        check(r.x() >= 0 && r.y() >= 0 && r.x() + r.width() <= width && r.y() + r.height() <= height,
                                            where + ": everything to click inside the box");
                                        for (int b = a + 1; b < rects.size(); b++) {
                                            com.hex.kagune.control.client.UkakuMenuView.Rect o = rects.get(b);
                                            check(r.x() + r.width() <= o.x() || o.x() + o.width() <= r.x() || r.y() + r.height() <= o.y()
                                                || o.y() + o.height() <= r.y(), where + ": nothing to click over anything else to click");
                                        }
                                    }
                                    for (int i = 0; i < texts.size(); i++) {
                                        int[] t = texts.get(i);
                                        check(t[0] >= 8 && t[0] + t[2] <= width - 8 && t[1] >= 4 && t[1] + 8 <= height - 4,
                                            where + ": the words inside the box: \"" + words.get(i) + "\"");
                                    }
                                    long buttons = hits.keySet().stream().filter(k -> k.startsWith("mutation:")).count();
                                    seen[0] += texts.size();
                                    seen[1] += buttons;
                                    if (tab == com.hex.kagune.control.client.UkakuMenuView.Tab.ORGAN) {
                                        check(hits.containsKey("eyes"), where + ": the kakugan button");
                                        check(buttons == 0, where + ": nothing on the organ's tab spends a point");
                                        for (com.hex.kagune.control.UkakuStage stage : com.hex.kagune.control.UkakuStage.values()) {
                                            check(hits.containsKey("stage:" + stage.name()), where + ": " + stage + " a card to click");
                                        }
                                        for (com.hex.kagune.control.ukaku.UkakuUnlock unlock : com.hex.kagune.control.ukaku.UkakuUnlock.values()) {
                                            check(hits.containsKey("unlock:" + unlock.name()), where + ": " + unlock + " on the road, to click");
                                        }
                                    } else {
                                        for (com.hex.kagune.control.ukaku.UkakuMutation mutation : com.hex.kagune.control.ukaku.UkakuMutation.values()) {
                                            check(hits.containsKey("pick:" + mutation.ordinal()), where + ": " + mutation + " a card to click");
                                        }
                                        com.hex.kagune.control.ukaku.UkakuMutation shown = null;
                                        for (com.hex.kagune.control.ukaku.UkakuMutation mutation : com.hex.kagune.control.ukaku.UkakuMutation.values()) {
                                            if (pick != null && pick.equals("pick:" + mutation.ordinal())) {
                                                shown = mutation;
                                            }
                                        }
                                        check(buttons <= 1, where + ": one UNLOCK button at most");
                                        if (shown != null) {
                                            boolean can = lock.passes(com.hex.kagune.control.quest.Gates.ukaku(shown))
                                                && com.hex.kagune.control.ukaku.UkakuMutation.canUnlock(mastery, mask, shown, lock.points());
                                            check(hits.containsKey("mutation:" + shown.ordinal()) == can && buttons == (can ? 1 : 0),
                                                where + ": the UNLOCK button only for the one picked, only while a point can be spent on it");
                                        }
                                        for (String key : hits.keySet()) {
                                            if (key.startsWith("mutation:")) {
                                                com.hex.kagune.control.ukaku.UkakuMutation mutation = com.hex.kagune.control.ukaku.UkakuMutation.byOrdinal(
                                                    Integer.parseInt(key.substring(9)));
                                                check(mutation != null && !mutation.in(mask) && lock.passes(com.hex.kagune.control.quest.Gates.ukaku(mutation))
                                                    && com.hex.kagune.control.ukaku.UkakuMutation.canUnlock(mastery, mask, mutation, lock.points()),
                                                    where + ": an UNLOCK button only where a point can be spent");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        check(seen[0] > 100000 && seen[1] > 100, "the menu's words and its UNLOCK buttons were all drawn and checked (" + seen[0] + ", " + seen[1] + ")");
        try {
            String screen = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/client/UkakuMenuScreen.java"));
            String view = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/client/UkakuMenuView.java"));
            check(!screen.contains("this.m_280273_(graphics)") && !screen.contains("graphics.m_280024_(0, 0, this.f_96543_, this.f_96544_"),
                "no full-screen overlay: the world seen round the box");
            check(screen.contains("UkakuMenuView.BOX_WIDTH") && screen.contains("UkakuMenuView.BOX_HEIGHT"), "the screen sized from the box");
            check(screen.indexOf("C2SUkakuAction.MUTATE") > screen.indexOf("key.startsWith(\"mutation:\")")
                && screen.split("C2SUkakuAction.MUTATE", -1).length == 2, "the screen spends a point only on the UNLOCK button's key");
            int draw = view.indexOf("public static Map<String, Rect> draw(");
            check(draw > 0 && !view.substring(draw, view.indexOf("// ------------------------------------------------------------------ drawing (as"))
                .contains("organicPanel(") && view.contains("QuestFrame.plate("), "the organic board gone from the Ukaku menu: the quest menu's plate");
        } catch (java.io.IOException failure) {
            check(false, "the Ukaku menu's source: " + failure);
        }
        com.hex.kagune.control.client.UkakuMenuView.Model at760 = new com.hex.kagune.control.client.UkakuMenuView.Model(760, true,
            com.hex.kagune.control.ukaku.WingForm.TWIN, com.hex.kagune.control.kakugan.KakuganEyes.DEFAULT, 0);
        check(at760.tab() == com.hex.kagune.control.client.UkakuMenuView.Tab.ORGAN && at760.picked() == null, "an old model opens on the organ");
    }

    /**
     * 4.1.33 (the owner: "Quests should only be done once ... once you did all of them /quest complete, all of them should be finished. Even if
     * you pick another kagune, this should still be finished because you finished all of them. You don't need to be typing it again."):
     * finished once, every path's and every ghoul's quest is done for good, at any mastery, kept through a save; nothing opens again.
     */
    /**
     * 4.1.40: a log finished for good counts every quest a later version adds as done; at login every gate is opened for it, so an ability
     * a new quest gates is never out of reach.
     */
    static void forGoodLater() {
        try {
            String server = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/QuestServer.java"));
            int login = server.indexOf("public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {");
            check(login > 0 && server.indexOf("if (cached.log.finished()) {", login) - login < 700
                && server.indexOf("cached.log.grant(gate);", login) - login < 900, "finished for good: every gate opened at login, a later version's too");
        } catch (java.io.IOException failure) {
            check(false, "read the source: " + failure);
        }
    }

    static void forGood() {
        QuestLog log = new QuestLog();
        Quest anteiku = QuestBook.get("anteiku_way");
        if (anteiku != null) {
            log.complete(QuestBook.get("pack_way") != null ? QuestBook.get("pack_way") : anteiku);
        }
        log.finishAll();
        QuestLog back = QuestLog.load(log.save());
        check(back.finished(), "finished for good, through a save");
        for (QuestLog l : new QuestLog[] {log, back}) {
            for (com.hex.kagune.control.KagunePath path : com.hex.kagune.control.KagunePath.values()) {
                for (int mastery : new int[] {0, 300, 750, 1000, 5000}) {
                    for (Quest quest : QuestBook.all()) {
                        if (quest.line() != Quest.Line.PATH && quest.line() != Quest.Line.GHOUL) {
                            continue;
                        }
                        QuestLog.State state = l.state(quest, path, mastery);
                        check(state == QuestLog.State.DONE || state == QuestLog.State.CLOSED,
                            quest.id() + " on " + path + " at mastery " + mastery + ": never open again (" + state + ")");
                    }
                }
            }
        }
        // A quest a later version adds is done as it comes: state() says so without a stored end.
        Quest any = null;
        for (Quest quest : QuestBook.all()) {
            if (quest.line() == Quest.Line.PATH) {
                any = quest;
                break;
            }
        }
        QuestLog fresh = QuestLog.load("v=" + QuestLog.VERSION + ";all=1");
        check(any != null && fresh.state(any, any.path() == null ? com.hex.kagune.control.KagunePath.RINKAKU : any.path(), 0)
            == QuestLog.State.DONE, "any quest, stored or not, is done once all are");
        // The hunts and favours still come, a night at a time.
        Hunts.Spec spec = new Hunts.Spec(0, 4, 3, Hunts.PAY_MEAT, 1);
        Quest hunt = Hunts.quest(spec);
        check(back.state(hunt, com.hex.kagune.control.KagunePath.RINKAKU, 500) != QuestLog.State.DONE, "a hunt is still its own night's work");
        back.reset();
        check(!back.finished(), "a reset (the command) starts them over");
    }

    /** Every quest is sound, every gate has a way in on its own path, and every path has its own line. */
    static void book() {
        Set<String> ids = new HashSet<>();
        for (Quest quest : QuestBook.all()) {
            check(ids.add(quest.id()), quest.id() + ": one quest per id");
            check(!quest.title().isBlank() && !quest.story().isBlank() && !quest.objectives().isEmpty(), quest.id() + ": a title, a story, objectives");
            for (String before : quest.after()) {
                Quest prior = QuestBook.get(before);
                check(prior != null && prior.mastery() <= quest.mastery(), quest.id() + ": comes after a real, earlier quest " + before);
            }
            for (Objective objective : quest.objectives()) {
                check(!objective.text().isBlank() && objective.count() > 0, quest.id() + ": objective has words and a count");
            }
            for (Gate gate : quest.reward().gates()) {
                check(gate.path() == null || gate.path() == quest.path(), quest.id() + ": opens only its own path's gate " + gate.id());
            }
            if (quest.exclusive() != null) {
                long rivals = QuestBook.all().stream().filter(other -> quest.exclusive().equals(other.exclusive())).count();
                check(rivals >= 2, quest.id() + ": an exclusive group has a rival");
            }
        }
        for (Gate gate : Gate.values()) {
            List<Quest> openers = QuestBook.openers(gate);
            // 1.3.14: a gate a companion teaches has no quest opening it: its teacher does.
            boolean taught = com.hex.kagune.control.npc.Teaching.taught(gate);
            check(taught ? openers.isEmpty() : !openers.isEmpty(), gate.id() + ": some quest opens it, or its teacher does (never both)");
            for (Quest opener : openers) {
                check(opener.path() == gate.path() || opener.path() == null, gate.id() + ": opened on its own path by " + opener.id());
                check(gate.mutation() ? gate.mastery() == 0 : opener.mastery() <= gate.mastery(), gate.id() + ": " + opener.id() + " starts before the gate's mastery");
            }
            check(taught || !QuestBook.openersText(gate).isBlank(), gate.id() + ": names its quests");
            check(Gate.fromId(gate.id()) == gate, gate.id() + ": found by id");
        }
        for (KagunePath path : new KagunePath[] {KagunePath.RINKAKU, KagunePath.UKAKU, KagunePath.GRAFT}) {
            long own = QuestBook.all().stream().filter(quest -> quest.path() == path).count();
            check(own >= 6, path + ": a line of its own (" + own + " quests)");
            long gates = java.util.Arrays.stream(Gate.values()).filter(gate -> gate.path() == path).count();
            check(gates >= 4, path + ": at least four gates of its own");
        }
        // Rival ways: most gates of the paths have two quests opening them.
        int two = 0;
        int pathGates = 0;
        for (Gate gate : Gate.values()) {
            if (gate.path() != null && !gate.mutation() && !com.hex.kagune.control.npc.Teaching.taught(gate)) {
                pathGates++;
                two += QuestBook.openers(gate).size() >= 2 ? 1 : 0;
            }
        }
        check(two * 2 >= pathGates, "most path gates opened by quests have two ways in (" + two + " of " + pathGates + ")");
    }

    /** Locked until the path and mastery fit, open while counting, met when every objective is, done once paid. */
    static void states() {
        QuestLog log = new QuestLog();
        Quest gift = QuestBook.get("rizes_gift");
        check(log.state(gift, KagunePath.UKAKU, 1000) == QuestLog.State.LOCKED, "another path's quest is locked");
        check(log.state(gift, KagunePath.RINKAKU, gift.mastery() - 1) == QuestLog.State.LOCKED, "locked below its mastery");
        check(log.state(gift, KagunePath.RINKAKU, gift.mastery()) == QuestLog.State.OPEN, "open at its mastery");
        // Locked quests do not count.
        log.apply(QuestEvent.of(Signal.KILL, 1, Tags.KAGUNE), KagunePath.RINKAKU, 0);
        check(log.progress(gift, 0) == 0, "a locked quest doesn't count");
        for (int i = 0; i < 20; i++) {
            log.apply(QuestEvent.of(Signal.KILL, 1, Tags.KAGUNE, Tags.HOSTILE), KagunePath.RINKAKU, 300);
        }
        check(log.progress(gift, 0) == gift.objectives().get(0).count(), "counts up to its objective and no further");
        check(!log.met(gift), "not met with an objective left");
        log.apply(QuestEvent.of(Signal.EAT, 1, Tags.item("minecraft:bread")), KagunePath.RINKAKU, 300);
        check(log.progress(gift, 1) == 0, "an event without the tag doesn't count");
        for (int i = 0; i < 5; i++) {
            log.apply(QuestEvent.of(Signal.EAT, 1, Tags.FLESH), KagunePath.RINKAKU, 300);
        }
        check(log.met(gift), "met with every objective");
        check(!log.open(Gate.RINKAKU_TWIN), "the gate stays shut until it is paid");
        log.complete(gift);
        check(log.state(gift, KagunePath.RINKAKU, 300) == QuestLog.State.DONE && log.open(Gate.RINKAKU_TWIN), "done, and its gate open");
        int before = log.progress(gift, 0);
        log.apply(QuestEvent.of(Signal.KILL, 1, Tags.KAGUNE), KagunePath.RINKAKU, 300);
        check(log.progress(gift, 0) == before, "a done quest counts no more");
        // A quest that comes after another waits for it (1.3.29: the ghouls' chain, once a ghoul has been talked to).
        log.bond("a-ghoul-of-the-night", QuestLog.Bond.NONE.with(com.hex.kagune.control.npc.NpcBonds.MET, true));
        Quest errands = QuestBook.get("errands");
        check(log.state(errands, KagunePath.RINKAKU, 1000) == QuestLog.State.LOCKED, "waits for the quest it comes after");
        log.complete(QuestBook.get("a_friend"));
        check(log.state(errands, KagunePath.RINKAKU, 1000) == QuestLog.State.OPEN, "open once that one is done");
        // Best-of objectives keep the highest amount.
        Quest rampage = QuestBook.get("rampage");
        log.apply(QuestEvent.of(Signal.RAMPAGE, 3), KagunePath.RINKAKU, 1000);
        log.apply(QuestEvent.of(Signal.RAMPAGE, 2), KagunePath.RINKAKU, 1000);
        check(log.progress(rampage, 0) == 3, "a rampage objective keeps its best streak");
        log.apply(QuestEvent.of(Signal.RAMPAGE, 9), KagunePath.RINKAKU, 1000);
        check(log.progress(rampage, 0) == 5 && log.met(rampage), "and caps at its count");
        // Points and sides.
        log.complete(QuestBook.get("die_hard"));
        check(log.points() == 1, "a quest's mutation point is kept");
    }

    /** The first of two rival ways closes the other for good. */
    static void exclusive() {
        QuestLog log = new QuestLog();
        Quest anteiku = QuestBook.get("anteiku");
        Quest pack = QuestBook.get("pack");
        List<Quest> closed = log.complete(pack);
        check(closed.contains(anteiku), "taking the Pack's way closes Anteiku's");
        check(log.state(anteiku, KagunePath.RINKAKU, 1000) == QuestLog.State.CLOSED, "closed for good");
        check(log.side(QuestBook.PACK) == 1 && log.side(QuestBook.ANTEIKU) == 0, "the side taken is remembered");
        log.apply(QuestEvent.of(Signal.RESTRAIN, 1), KagunePath.RINKAKU, 1000);
        check(log.progress(anteiku, 0) == 0, "a closed quest counts no more");
    }

    static void oneLife() {
        Objective o = Objective.of("x", Signal.KILL, 3).inOneLife();
        check(o.oneLife() && !Objective.of("x", Signal.KILL, 3).oneLife(), "one-life objectives are marked");
        check(o.advance(1, QuestEvent.of(Signal.KILL, 1)) == 2, "advance adds");
        check(!Objective.of("x", Signal.KILL, 1, Tags.EYES).without(Tags.KAKUJA).matches(QuestEvent.of(Signal.KILL, 1, Tags.EYES, Tags.KAKUJA)),
            "a forbidden tag stops it matching");
        check(!Objective.of("x", Signal.KILL, 1).matches(QuestEvent.of(Signal.KILL, 0)), "nothing happened, nothing counts");
    }

    static void saves() {
        QuestLog log = new QuestLog();
        log.migrate(gate -> gate == Gate.EYES_KICK);
        for (int i = 0; i < 7; i++) {
            log.apply(QuestEvent.of(Signal.KILL, 1, Tags.KAGUNE), KagunePath.RINKAKU, 300);
        }
        log.complete(QuestBook.get("pack"));
        log.complete(QuestBook.get("cannibal"));
        Hunts.Spec spec = new Hunts.Spec(0, 4, 3, Hunts.PAY_MEAT, 1);
        log.startHunt(spec, 12345L);
        log.apply(QuestEvent.of(Signal.KILL, 1, Tags.mob("minecraft:witch")), KagunePath.RINKAKU, 300);
        log.nextHuntAt(99L);
        QuestLog back = QuestLog.load(log.save());
        Quest gift = QuestBook.get("rizes_gift");
        check(back.progress(gift, 0) == 7, "progress survives the save");
        check(back.state(QuestBook.get("pack"), KagunePath.RINKAKU, 1000) == QuestLog.State.DONE, "done survives");
        check(back.state(QuestBook.get("anteiku"), KagunePath.RINKAKU, 1000) == QuestLog.State.CLOSED, "closed survives");
        check(back.open(Gate.EYES_KICK) && back.open(Gate.RINKAKU_FOUR), "gates survive");
        check(back.points() == 1 && back.side(QuestBook.PACK) == 1, "points and sides survive");
        check(back.migrated() == QuestLog.VERSION && back.nextHuntAt() == 99L, "the version and the hunt clock survive");
        check(back.hunts().size() == 1 && back.hunts().get(0).endsAt() == 12345L, "the hunt survives");
        check(back.progress(back.hunts().get(0).quest(), 0) == 1, "and its progress");
        check(back.save().equals(log.save()), "a second save is the same string");
        check(QuestLog.load("").migrated() == 0 && QuestLog.load(null).points() == 0, "nothing saved is a new log");
        QuestLog junk = QuestLog.load("v=1;gate=nope,eyes_kick;done=nothing;p.nothing=4;p.rizes_gift=99,x;h.0=bad");
        check(junk.open(Gate.EYES_KICK) && junk.gates().size() == 1, "an unknown gate is dropped");
        check(junk.progress(gift, 0) == gift.objectives().get(0).count() && junk.progress(gift, 1) == 0, "bad numbers are clamped or zero");
        check(junk.hunts().isEmpty(), "a bad hunt is dropped");
    }

    static void migration() {
        QuestLog log = new QuestLog();
        log.migrate(gate -> !gate.mutation() && gate.mastery() <= 500);
        check(log.open(Gate.RINKAKU_TWIN) && log.open(Gate.RINKAKU_FOUR) && !log.open(Gate.RINKAKU_CENTIPEDE), "what was reached stays open");
        log.migrate(gate -> true);
        check(!log.open(Gate.RINKAKU_CENTIPEDE), "migration happens once");
        log.reset();
        check(log.gates().isEmpty() && log.migrated() == QuestLog.VERSION, "a reset forgets everything and never migrates again");
    }

    /** Which gate each form, stage and mutation is behind, and the stages a gate leaves usable. */
    static void gates() {
        check(com.hex.kagune.control.quest.Gates.rinkaku(com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED) == null, "the seed is never gated");
        check(com.hex.kagune.control.quest.Gates.rinkaku(com.hex.kagune.gameplay.KaguneForm.RINKAKU_DUAL) == Gate.RINKAKU_TWIN
            && com.hex.kagune.control.quest.Gates.rinkaku(com.hex.kagune.gameplay.KaguneForm.RINKAKU_QUAD) == Gate.RINKAKU_FOUR
            && com.hex.kagune.control.quest.Gates.rinkaku(com.hex.kagune.gameplay.KaguneForm.CENTIPEDE) == Gate.RINKAKU_CENTIPEDE
            && com.hex.kagune.control.quest.Gates.rinkaku(com.hex.kagune.gameplay.KaguneForm.FULL_KAKUJA) == Gate.RINKAKU_FULL_KAKUJA,
            "every Rinkaku form past the seed has its gate");
        check(com.hex.kagune.control.quest.Gates.graft(com.hex.kagune.control.graft.GraftStage.TRANSPLANT) == null, "the transplant is never gated");
        for (com.hex.kagune.control.graft.GraftStage stage : com.hex.kagune.control.graft.GraftStage.values()) {
            Gate gate = com.hex.kagune.control.quest.Gates.graft(stage);
            check(stage == com.hex.kagune.control.graft.GraftStage.TRANSPLANT || gate != null && gate.path() == KagunePath.GRAFT
                && gate.mastery() == stage.mastery(), stage + ": its gate, at its mastery");
        }
        java.util.function.Predicate<Gate> onlyCentipede = gate -> gate == Gate.GRAFT_CENTIPEDE;
        check(com.hex.kagune.control.quest.Gates.graftUsable(com.hex.kagune.control.graft.GraftStage.DRAGON, 1000, onlyCentipede)
            == com.hex.kagune.control.graft.GraftStage.CENTIPEDE, "a stage whose quest isn't done falls back to the highest that is open");
        check(com.hex.kagune.control.quest.Gates.graftUsable(com.hex.kagune.control.graft.GraftStage.THORNS, 1000, gate -> false)
            == com.hex.kagune.control.graft.GraftStage.TRANSPLANT, "nothing open: the transplant");
        check(com.hex.kagune.control.quest.Gates.graftUsable(com.hex.kagune.control.graft.GraftStage.DRAGON, 1000, gate -> true)
            == com.hex.kagune.control.graft.GraftStage.DRAGON, "everything open: what was picked");
        check(com.hex.kagune.control.quest.Gates.graftNext(com.hex.kagune.control.graft.GraftStage.CENTIPEDE, 1000, onlyCentipede)
            == com.hex.kagune.control.graft.GraftStage.TRANSPLANT, "K held skips the stages waiting for quests");
        check(com.hex.kagune.control.quest.Gates.graft(com.hex.kagune.control.graft.GraftMutation.RIZES_APPETITE) == Gate.GRAFT_APPETITE
            && com.hex.kagune.control.quest.Gates.graft(com.hex.kagune.control.graft.GraftMutation.WASHUU_BLOOD) == null
            && com.hex.kagune.control.quest.Gates.ukaku(com.hex.kagune.control.ukaku.UkakuMutation.BURSTING_SHARDS) == Gate.UKAKU_BURSTING_SHARDS
            && com.hex.kagune.control.quest.Gates.ukaku(com.hex.kagune.control.ukaku.UkakuMutation.NOT_THAT_EASILY) == Gate.UKAKU_NOT_THAT_EASILY
            && com.hex.kagune.control.quest.Gates.rinkaku(com.hex.kagune.gameplay.MutationNode.CENTIPEDE_BODY) == null,
            "the gated mutations (1.0.41: the Rinkaku path has none left)");
        // 1.0.41: Graft's revive on every path that has it, behind the same quest: the same objectives, the same mastery.
        Quest graft = QuestBook.get("not_that_easily");
        for (String id : new String[] {"rinkaku_not_that_easily", "ukaku_not_that_easily"}) {
            Quest copy = QuestBook.get(id);
            check(copy != null && copy.mastery() == graft.mastery() && copy.title().equals(graft.title())
                && copy.objectives().equals(graft.objectives()), id + ": Graft's quest, objective for objective");
        }
        check(QuestBook.get("arrow_eater") == null && QuestBook.openers(Gate.RINKAKU_NOT_THAT_EASILY).size() == 1
            && QuestBook.openers(Gate.UKAKU_NOT_THAT_EASILY).size() == 1, "the Rinkaku path's mutation quest is gone; the revive's quests open it");
        // Quest points add to the milestones' (and never go below zero).
        check(com.hex.kagune.control.graft.GraftMutation.available(0, 0, 2) == 2
            && com.hex.kagune.control.graft.GraftMutation.available(500, 0, 0) == 1
            && com.hex.kagune.control.ukaku.UkakuMutation.available(0, 0, -3) == 0, "quest points count with the milestones'");
    }

    static void hunts() {
        Random random = new Random(42L);
        Set<Integer> templates = new HashSet<>();
        for (int i = 0; i < 400; i++) {
            Hunts.Spec spec = Hunts.draw(random, false);
            templates.add(spec.template());
            Quest quest = Hunts.quest(spec);
            check(quest.line() == Quest.Line.HUNT && quest.objectives().size() == 1 && quest.objectives().get(0).count() == spec.count(),
                "a hunt is a one-objective quest: " + spec.save());
            check(spec.equals(Hunts.Spec.load(spec.save())), "a hunt's spec round-trips: " + spec.save());
            check(Hunts.quest(spec).id().equals(quest.id()), "the same spec is the same hunt");
            check(!Hunts.payText(spec).isBlank(), "a hunt says what it pays");
            check(!quest.title().contains("own") || spec.template() != 7, "no ghoul hunt while no ghouls roam");
        }
        check(templates.size() >= 6, "hunts vary: " + templates.size() + " kinds drawn");
        Random a = new Random(7L);
        Random b = new Random(7L);
        check(Hunts.draw(a, true).equals(Hunts.draw(b, true)), "the same roll, the same hunt");
        QuestLog log = new QuestLog();
        Hunts.Spec spec = new Hunts.Spec(1, 0, 2, Hunts.PAY_MASTERY, 20);
        log.startHunt(spec, 100L);
        check(log.expire(99L).isEmpty() && log.hunts().size() == 1, "on until its time");
        check(log.expire(100L).size() == 1 && log.hunts().isEmpty(), "gone at dawn");
    }

    /** 1.3.29: no quest comes by itself unless the path needs it; a teacher's only from the teacher; feats show once done. */
    static void givers() {
        check(QuestBook.giver(QuestBook.get("rizes_gift")) == QuestBook.Giver.PATH && QuestBook.giver(QuestBook.get("anteiku")) == QuestBook.Giver.PATH,
            "the path's own ways on are the path's");
        for (String id : new String[] {"light_feet", "up_we_go", "witness_this"}) {
            check(QuestBook.giver(QuestBook.get(id)) == QuestBook.Giver.RETIRED, id + " is retired: Eto's lesson teaches the same");
        }
        for (String id : new String[] {"first_meal", "night_stalker", "the_brink", "rampage", "die_hard", "cannibal", "ghoul_eater"}) {
            check(QuestBook.giver(QuestBook.get(id)) == QuestBook.Giver.FEAT, id + " is a feat");
        }
        for (String id : new String[] {"among_our_own", "a_friend", "cold_blood", "errands", "side_by_side"}) {
            check(QuestBook.giver(QuestBook.get(id)) == QuestBook.Giver.GHOULS, id + " is the ghouls'");
        }
        for (com.hex.kagune.control.npc.EtoRules.Lesson lesson : com.hex.kagune.control.npc.EtoRules.Lesson.values()) {
            check(QuestBook.giver(QuestBook.get(lesson.quest)) == QuestBook.Giver.ETO, lesson + "'s quest is Eto's to give");
        }
        check(QuestBook.giver(Hunts.quest(new Hunts.Spec(0, 4, 3, Hunts.PAY_MEAT, 1))) == QuestBook.Giver.ASKED, "a hunt is given when asked for");
        int teachers = 0;
        for (Quest quest : QuestBook.all()) {
            QuestBook.Giver giver = QuestBook.giver(quest);
            check(!QuestBook.from(quest).isEmpty() || giver == QuestBook.Giver.PATH, quest.id() + ": the journal says who it is from");
            if (giver == QuestBook.Giver.TEACHER) {
                teachers++;
                com.hex.kagune.control.npc.Teaching.Lesson lesson = QuestBook.lesson(quest);
                check(lesson != null && !quest.opens(lesson.gate()) && QuestBook.from(quest).contains(com.hex.kagune.control.npc.Persona.of(lesson.teacher()).name()),
                    quest.id() + ": a teacher's practice, after their lesson (which opens the gate, not the quest)");
            }
            if (giver == QuestBook.Giver.PATH) {
                check(new QuestLog().given(quest), quest.id() + ": the path's own quests need no one to give them (the way on is never hidden)");
            }
        }
        check(teachers >= 8 && QuestBook.lesson(QuestBook.get("crystal_rain")) == com.hex.kagune.control.npc.Teaching.byId("touka_gatling")
            && QuestBook.lesson(QuestBook.get("thousand_minus_seven")) == com.hex.kagune.control.npc.Teaching.byId("ken_centipede"),
            "the quests whose gates the companions teach are theirs");
        QuestLog log = new QuestLog();
        Quest feet = QuestBook.get("light_feet");
        check(log.state(feet, KagunePath.RINKAKU, 1000) == QuestLog.State.LOCKED && !log.shown(feet, QuestLog.State.LOCKED),
            "Light on Your Feet never comes by itself any more");
        log.apply(QuestEvent.of(Signal.FLIP, 1), KagunePath.RINKAKU, 1000);
        check(log.progress(feet, 1) == 0, "nor counts");
        Quest kick = QuestBook.get("eto_kick");
        check(log.state(kick, KagunePath.RINKAKU, 1000) == QuestLog.State.LOCKED && !log.shown(kick, QuestLog.State.LOCKED), "Eto's waits for her, unlisted");
        log.bond(com.hex.kagune.control.npc.EtoRules.KEY, QuestLog.Bond.NONE.with(com.hex.kagune.control.npc.EtoRules.TAUGHT_KICK, true));
        check(log.state(kick, KagunePath.RINKAKU, 0) == QuestLog.State.OPEN && log.shown(kick, QuestLog.State.OPEN) && kick.title().equals("Light on Your Feet"),
            "asked of Eto, Light on Your Feet is given (hers)");
        check(QuestBook.openersText(Gate.EYES_KICK).split("Light on Your Feet", -1).length == 2, "the gate's ways name the lesson once (not the retired one)");
        Quest meal = QuestBook.get("first_meal");
        check(log.state(meal, KagunePath.RINKAKU, 0) == QuestLog.State.OPEN && !log.shown(meal, QuestLog.State.OPEN), "a feat counts unlisted");
        log.complete(meal);
        check(log.shown(meal, log.state(meal, KagunePath.RINKAKU, 0)), "and shows once done");
        Quest among = QuestBook.get("among_our_own");
        log.bond("eto", log.bond("eto").with(com.hex.kagune.control.npc.NpcBonds.MET, true));
        check(log.state(among, KagunePath.RINKAKU, 0) == QuestLog.State.LOCKED, "the ghouls' quests wait for a ghoul (a companion met is not one)");
        log.bond("9f3c2a7e-0000-4000-8000-000000000001", QuestLog.Bond.NONE.with(com.hex.kagune.control.npc.NpcBonds.MET, true));
        check(log.state(among, KagunePath.RINKAKU, 0) == QuestLog.State.OPEN, "talked to a ghoul of the night, they give theirs");
        Quest rain = QuestBook.get("crystal_rain");
        QuestLog ukaku = new QuestLog();
        check(ukaku.state(rain, KagunePath.UKAKU, 1000) == QuestLog.State.LOCKED, "a teacher's practice waits for the teacher");
        ukaku.grant(Gate.UKAKU_GATLING);
        check(ukaku.state(rain, KagunePath.UKAKU, 1000) == QuestLog.State.OPEN, "taught, the practice is given");
        QuestLog old = QuestLog.load("v=2;p.light_feet=12,3");
        check(old.state(feet, KagunePath.RINKAKU, 1000) == QuestLog.State.OPEN && old.shown(feet, QuestLog.State.OPEN),
            "a retired quest already begun is kept going (no save loses its work)");
    }

    /** 1.3.29: asked for what they know already, a teacher says so and moves on. */
    static void taughtAlready() {
        java.util.function.Predicate<Gate> open = gate -> gate == Gate.UKAKU_CRYSTAL || gate == Gate.UKAKU_GATLING;
        List<com.hex.kagune.control.npc.Teaching.Lesson> known = com.hex.kagune.control.npc.Teaching.knownElsewhere("touka", KagunePath.UKAKU, open, 0L);
        check(known.size() == 2 && known.get(0).id().equals("touka_crystal"), "the gates open some other way are the ones known");
        long told = com.hex.kagune.control.npc.Teaching.bit(known.get(0));
        check(com.hex.kagune.control.npc.Teaching.knownElsewhere("touka", KagunePath.UKAKU, open, told).size() == 1, "told once, never told again");
        check(com.hex.kagune.control.npc.Teaching.knownElsewhere("touka", KagunePath.RINKAKU, open, 0L).isEmpty(), "only the lessons for their path");
        Set<Long> bits = new HashSet<>();
        for (com.hex.kagune.control.npc.Teaching.Lesson lesson : com.hex.kagune.control.npc.Teaching.ALL) {
            long bit = com.hex.kagune.control.npc.Teaching.bit(lesson);
            check(bit != 0L, lesson.id() + " has a bit");
            bits.add(bit * 31L + lesson.teacher().hashCode());
        }
        check(bits.size() == com.hex.kagune.control.npc.Teaching.ALL.size(), "each lesson its own bit (per teacher)");
        check(com.hex.kagune.control.npc.Teaching.names(List.of("A")).equals("A") && com.hex.kagune.control.npc.Teaching.names(List.of("A", "B")).equals("A and B")
            && com.hex.kagune.control.npc.Teaching.names(List.of("A", "B", "C")).equals("A, B and C"), "names run on as a sentence");
        for (String teacher : new String[] {"eto", "touka", "ken"}) {
            for (int seed = 0; seed < 4; seed++) {
                String line = com.hex.kagune.control.npc.Teaching.alreadyKnown(teacher, List.of("Gatling"), seed);
                check(line.contains("Gatling") && line.toLowerCase(java.util.Locale.ROOT).contains("already"), teacher + " says it is already known");
            }
        }
    }

    /** 1.3.29: the journal's Romance tab: each companion's card, sent with the log. */
    static void romance() {
        List<com.hex.kagune.control.npc.RomanceCard> cards = new ArrayList<>();
        com.hex.kagune.control.npc.Romance.Stage[] stages = com.hex.kagune.control.npc.Romance.Stage.values();
        com.hex.kagune.control.npc.EtoRules.Step[] steps = com.hex.kagune.control.npc.EtoRules.Step.values();
        String[] banned = {"kaneki", "ayato", "furuta", "takatsuki", "aogiri", "yoshimura"};
        Random random = new Random(1329);
        for (int i = 0; i < 400; i++) {
            String key = new String[] {"eto", "touka", "ken"}[i % 3];
            com.hex.kagune.control.npc.RomanceCard card = new com.hex.kagune.control.npc.RomanceCard(key, random.nextInt(5) > 0, stages[random.nextInt(stages.length)],
                random.nextInt(201) - 100, random.nextBoolean(), random.nextInt(4) == 0 ? "Somebody" : null, steps[random.nextInt(steps.length)],
                random.nextInt(11), random.nextInt(9), random.nextInt(4), random.nextBoolean(), random.nextBoolean(), random.nextInt(6));
            cards.add(card);
            String next = card.next();
            String stage = card.stageText();
            check(next != null && !next.isBlank() && stage != null && !stage.isBlank() && !card.errands().isBlank(), key + ": every card says where it stands and what's next");
            check(card.regardShare() >= 0.0F && card.regardShare() <= 1.0F, "the regard bar stays in its box");
            for (String name : banned) {
                check(!(next + stage + card.errands()).toLowerCase(java.util.Locale.ROOT).contains(name), "a card never names " + name);
            }
            if (!card.met()) {
                check(next.contains(card.persona().home()), "not met: where to find them");
            }
            if (card.met() && card.stage() == com.hex.kagune.control.npc.Romance.Stage.MARRIED && !card.home()) {
                check(next.contains("This is our home now."), "married with no home: how to make one");
            }
        }
        List<com.hex.kagune.control.npc.RomanceCard> back = com.hex.kagune.control.npc.RomanceCard.load(com.hex.kagune.control.npc.RomanceCard.save(cards));
        check(back.equals(cards), "the cards come through the packet as they were");
        check(com.hex.kagune.control.npc.RomanceCard.load("bad\nnobody\t1\n").isEmpty() && com.hex.kagune.control.npc.RomanceCard.load(null).isEmpty(),
            "a bad card is left out");
    }
}
