package com.hex.kagune.control.client;

import com.hex.kagune.control.RinkakuForms;
import com.hex.kagune.control.RinkakuUnlocks;
import com.hex.kagune.control.client.ui.UiCanvas;
import com.hex.kagune.control.king.KingForm;
import com.hex.kagune.control.quest.Gate;
import com.hex.kagune.control.quest.Gates;
import com.hex.kagune.control.quest.QuestLog;
import com.hex.kagune.control.ukaku.UkakuMutation;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.MutationNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 6.0.0: the Rinkaku path's kagune menu (the menu key): his organ and mastery, his forms to pick (the half kakujas), his RC and what K
 * does, the kakugan placement, and (1.0.41) its one passive, You Thought You'd Get Rid of Me That Easily?, opened by its quest. 1.0.10:
 * the final kakuja, War or Peace, after the Dragon.
 *
 * <p>4.1.58 (the owner: "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as interactable as it is,
 * Dialog, mastery menus ALL OF THEM"): the quest menu's box ({@link QuestFrame}), as the Ukaku menu (4.1.56). Tabs FORMS and FINAL KAKUJA
 * (its badge: a choice waiting). FORMS: the five forms and the passive as quest cards on the left, his RC and the kakugan under them; the
 * one picked on the right as a quest is shown (its story, the form itself in a framed tile, its limbs, speed and control, its moves as a
 * checklist, and a WEAR THIS FORM button), the road of every step at the foot (a step picks its form). FINAL KAKUJA: War and Peace as
 * cards, the one picked with its emblem, its card's rows and a button to the choice; K's keys under them. Once one is chosen, its mark is
 * on the plate (War's veins, Peace's feathers) and its name is the menu's title.
 */
public final class RinkakuMenuView {
    /** The old board's virtual size (the screens that still scale by it). */
    public static final int UI_WIDTH = UkakuMenuView.UI_WIDTH;
    public static final int UI_HEIGHT = UkakuMenuView.UI_HEIGHT;
    /** 4.1.58: the box's size: the Ukaku menu's. */
    public static final int BOX_WIDTH = UkakuMenuView.BOX_WIDTH;
    public static final int BOX_HEIGHT = UkakuMenuView.BOX_HEIGHT;
    static final int DETAIL_WIDTH = UkakuMenuView.DETAIL_WIDTH;
    static final int ROAD_Y = UkakuMenuView.ROAD_Y;
    static final int TILE_WIDTH = UkakuMenuView.TILE_WIDTH;
    static final int TILE_HEIGHT = UkakuMenuView.TILE_HEIGHT;
    /** The half kakujas' colour (the original's purple kakuja buttons), for their chips' words. */
    static final int HALF = 0xFFC08AE8;

    /** 4.1.58: the menu's tabs. */
    public enum Tab {
        FORMS("FORMS"), KING("FINAL KAKUJA");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    /**
     * What the menu shows; {@code pending} is a mutation clicked once (the old tree's; none are drawn now). 4.1.58: the tab open and what is
     * picked in it ("pick:ID" a form, "pick:passive", "step:N" a step of the road; "pick:war", "pick:peace"; null: the form worn, the
     * kakuja chosen or War).
     */
    public record Model(int mastery, boolean active, KaguneForm form, float energy, float maxEnergy, long mutations, MutationNode pending,
                        QuestLocks quests, King king, Tab tab, String picked) {
        public Model {
            quests = quests == null ? QuestLocks.ALL : quests;
            king = king == null ? King.ASLEEP : king;
            tab = tab == null ? Tab.FORMS : tab;
        }

        public Model(int mastery, boolean active, KaguneForm form, float energy, float maxEnergy, long mutations, MutationNode pending) {
            this(mastery, active, form, energy, maxEnergy, mutations, pending, QuestLocks.ALL, King.ASLEEP);
        }

        public Model(int mastery, boolean active, KaguneForm form, float energy, float maxEnergy, long mutations, MutationNode pending,
                     QuestLocks quests) {
            this(mastery, active, form, energy, maxEnergy, mutations, pending, quests, King.ASLEEP);
        }

        public Model(int mastery, boolean active, KaguneForm form, float energy, float maxEnergy, long mutations, MutationNode pending,
                     QuestLocks quests, King king) {
            this(mastery, active, form, energy, maxEnergy, mutations, pending, quests, king, Tab.FORMS, null);
        }
    }

    /**
     * 1.0.5: the final kakuja: has the Dragon been through him ({@code awake}), what he chose, what he wears. 1.0.10: chosen on the
     * cards ({@code KingChoiceScreen}), never here; {@code pending} stays for the old saves of the menu's model.
     */
    public record King(boolean awake, KingForm chosen, KingForm worn, KingForm pending) {
        public static final King ASLEEP = new King(false, KingForm.NONE, KingForm.NONE, KingForm.NONE);
    }

    private RinkakuMenuView() {
    }

    /**
     * Draws the box at its own size and returns its clickable areas: "tab:FORMS", "tab:KING", "pick:ID" (a form looked at), "pick:passive",
     * "step:N", "form:ID" (WEAR THIS FORM, only for a form that may be worn and is not), "eyes", "pick:war", "pick:peace", "king:ID" (to the
     * choice, only while it may be made).
     */
    public static Map<String, UkakuMenuView.Rect> draw(UiCanvas c, Model model, double mouseX, double mouseY, double time) {
        Map<String, UkakuMenuView.Rect> hits = new LinkedHashMap<>();
        KingForm chosen = model.king().chosen();
        // 1.0.10: once the final kakuja is chosen, its mark is on the board (4.1.58: its emblem, War's one eye or Peace's cross blade, where
        // the quest menu keeps its honeycomb: the header's empty middle, never over anything to read).
        boolean marked = chosen == KingForm.WAR || chosen == KingForm.PEACE;
        QuestFrame.plate(c, BOX_WIDTH, BOX_HEIGHT, time, 292, 6, marked ? 0 : 170, marked ? 0 : 38);
        if (marked) {
            // Its own emblem, small (its card's: War's one eye, Peace's turning cross blade).
            c.push();
            c.translate(MARK_X + MARK_WIDTH / 2.0F, MARK_Y + MARK_HEIGHT / 2.0F);
            if (chosen == KingForm.WAR) {
                c.scale(0.4F);
                KingChoiceView.warEye(c, 0, 0, time);
            } else {
                c.scale(0.5F);
                KingChoiceView.peaceCross(c, 0, 0, false, time);
            }
            c.pop();
        }
        header(c, model, time);
        QuestFrame.tabs(c, List.of(new QuestFrame.Tab(Tab.FORMS.name(), Tab.FORMS.label, 0, false),
            new QuestFrame.Tab(Tab.KING.name(), Tab.KING.label, choosable(model) ? 1 : 0, true)), model.tab().name(), hits, mouseX, mouseY,
            QuestFrame.LIST_X, QuestFrame.TABS_Y, time);
        QuestFrame.Tip tip = model.tab() == Tab.KING ? kingTab(c, model, hits, mouseX, mouseY, time) : formsTab(c, model, hits, mouseX, mouseY, time);
        QuestFrame.tooltip(c, tip, 8, BOX_WIDTH - 8);
        return hits;
    }

    /** Where the final kakuja's emblem is drawn: the header's empty middle (between the title and the bar). */
    static final int MARK_X = 286;
    static final int MARK_Y = 4;
    static final int MARK_WIDTH = 186;
    static final int MARK_HEIGHT = 40;

    /** True while War or Peace may be chosen: the Dragon has been through him and nothing is chosen yet. */
    static boolean choosable(Model model) {
        return model.king().awake() && model.king().chosen() == KingForm.NONE;
    }

    // ------------------------------------------------------------------ the header

    /** The title (the final kakuja's once chosen), the matrix and mastery; the bar to the next step; his RC and the kagune out or in. */
    private static void header(UiCanvas c, Model model, double time) {
        int mastery = model.mastery();
        KingForm chosen = model.king().chosen();
        String title = chosen == KingForm.WAR ? "WAR  ·  THE ONE-EYED KING" : chosen == KingForm.PEACE ? "PEACE  ·  RINKAKU :RE" : "RINKAKU";
        List<int[]> steps = new ArrayList<>(steps().keySet().stream().map(m -> new int[] {m}).toList());
        int from = 0;
        int next = -1;
        for (int[] step : steps) {
            if (step[0] <= mastery) {
                from = Math.max(from, step[0]);
            } else if (next < 0) {
                next = step[0];
            }
        }
        float share = next < 0 ? 1.0F : (mastery - from) / (float)Math.max(1, next - from);
        String goal = next < 0 ? "Every step of the Rinkaku reached" : mastery + " / " + next + "  ·  next: " + steps().get(next);
        QuestFrame.header(c, BOX_WIDTH, title, "RC CELL / CENTIPEDE MATRIX  ·  MASTERY " + mastery, share, next < 0, goal, time);
        int right = BOX_WIDTH - 12;
        boolean out = model.active();
        right = QuestFrame.chipLeft(c, right, 56, out ? "KAGUNE OUT  ·  R RETRACTS" : "KAGUNE IN  ·  R MANIFESTS",
            out ? 0xFF2A0A10 : 0xFF181012, out ? QuestStyle.RED_DARK : QuestStyle.GREY_DARK, out ? 0xFFFF8A7A : QuestJournalView.MUTED);
        QuestFrame.chipLeft(c, right, 56, "RC " + Math.round(model.energy()) + " / " + Math.round(model.maxEnergy()), QuestFrame.RED_FILL,
            QuestStyle.RED_DARK, QuestFrame.VALUE);
    }

    /** Every step of the Rinkaku by its mastery: its forms (the tails, the half kakujas) and its moves, in words. */
    public static TreeMap<Integer, String> steps() {
        TreeMap<Integer, List<String>> parts = new TreeMap<>();
        for (KaguneForm form : RinkakuForms.forms()) {
            parts.computeIfAbsent(form.requiredMastery(), k -> new ArrayList<>()).add(form.displayName() + (RinkakuForms.kakuja(form) ? " (half kakuja)" : ""));
        }
        String[][] moves = {{"Grapple (J)"}, {"Thy Death (Z)"}, {"Grab (G), Throw (N)"}, {"Crush (H)"}, {"Lift (V)"}, {"Slam (B)"}};
        int[] needs = {RinkakuUnlocks.GRAPPLE, RinkakuUnlocks.RAPID_STRIKE, RinkakuUnlocks.GRAB, RinkakuUnlocks.CRUSH, RinkakuUnlocks.LIFT,
            RinkakuUnlocks.SLAM};
        for (int i = 0; i < needs.length; i++) {
            parts.computeIfAbsent(needs[i], k -> new ArrayList<>()).add(moves[i][0]);
        }
        TreeMap<Integer, String> out = new TreeMap<>();
        parts.forEach((m, words) -> out.put(m, String.join(", ", words)));
        return out;
    }

    // ------------------------------------------------------------------ the forms' tab

    private static QuestFrame.Tip formsTab(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, double time) {
        String picked = picked(model);
        int y = QuestFrame.CONTENT_Y;
        int index = 0;
        for (KaguneForm form : RinkakuForms.forms()) {
            QuestFrame.card(c, formCard(model, form), QuestFrame.LIST_X, y, QuestFrame.LIST_WIDTH, picked.equals("pick:" + form.id()), hits, mouseX,
                mouseY, time, index++);
            y += QuestFrame.CARD_HEIGHT + QuestFrame.CARD_GAP;
        }
        QuestFrame.card(c, passiveCard(model), QuestFrame.LIST_X, y, QuestFrame.LIST_WIDTH, picked.equals("pick:passive"), hits, mouseX, mouseY,
            time, index);
        y += QuestFrame.CARD_HEIGHT + QuestFrame.CARD_GAP;
        y = rc(c, model, QuestFrame.LIST_X, y + 6, QuestFrame.LIST_WIDTH);
        int ky = QuestFrame.section(c, "KAKUGAN", QuestFrame.LIST_X, y + 4, QuestFrame.LIST_WIDTH);
        QuestFrame.button(c, "eyes", "ADJUST KAKUGAN", QuestFrame.LIST_X, ky, QuestFrame.LIST_WIDTH, 16, true, false, hits, mouseX, mouseY, time);
        if (picked.equals("pick:passive")) {
            passiveDetail(c, model, QuestFrame.DETAIL_X, QuestFrame.CONTENT_Y, DETAIL_WIDTH);
        } else {
            formDetail(c, model, pickedForm(model), hits, mouseX, mouseY, QuestFrame.DETAIL_X, QuestFrame.CONTENT_Y, DETAIL_WIDTH, time);
        }
        return road(c, model, picked, hits, mouseX, mouseY, QuestFrame.DETAIL_X, ROAD_Y, DETAIL_WIDTH, time);
    }

    /** What is picked on the forms' tab: the pick as given ("pick:ID", "pick:passive"), a step's form ("step:N"), else the form worn. */
    static String picked(Model model) {
        String picked = model.picked();
        if (picked != null) {
            if (picked.equals("pick:passive")) {
                return picked;
            }
            if (picked.startsWith("pick:") && KaguneForm.fromId(picked.substring(5)) != null && RinkakuForms.forms().length > 0
                && java.util.Arrays.asList(RinkakuForms.forms()).contains(KaguneForm.fromId(picked.substring(5)))) {
                return picked;
            }
            if (picked.startsWith("step:")) {
                try {
                    return "pick:" + formAt(Integer.parseInt(picked.substring(5))).id();
                } catch (NumberFormatException wrong) {
                    // the form worn
                }
            }
        }
        KaguneForm worn = model.form();
        return "pick:" + (worn != null && java.util.Arrays.asList(RinkakuForms.forms()).contains(worn) ? worn : RinkakuForms.forms()[0]).id();
    }

    /** The form shown on the right (the passive's pick shows the worn one). */
    static KaguneForm pickedForm(Model model) {
        String picked = picked(model);
        KaguneForm form = picked.startsWith("pick:") ? KaguneForm.fromId(picked.substring(5)) : null;
        return form == null || !java.util.Arrays.asList(RinkakuForms.forms()).contains(form) ? RinkakuForms.forms()[0] : form;
    }

    /** The form a step at {@code mastery} belongs to: the last whose mastery it has reached. */
    static KaguneForm formAt(int mastery) {
        KaguneForm at = RinkakuForms.forms()[0];
        for (KaguneForm form : RinkakuForms.forms()) {
            if (mastery >= form.requiredMastery()) {
                at = form;
            }
        }
        return at;
    }

    /** How a form stands: worn (open), had (done), waiting for its quest (closed), or ahead (locked). */
    static QuestLog.State state(Model model, KaguneForm form) {
        if (!form.isUnlocked(model.mastery())) {
            return QuestLog.State.LOCKED;
        }
        if (!model.quests().passes(Gates.rinkaku(form))) {
            return QuestLog.State.CLOSED;
        }
        return form == model.form() ? QuestLog.State.OPEN : QuestLog.State.DONE;
    }

    /** True if {@code form} may be put on from here: reached, its quest done, not worn now. */
    static boolean wearable(Model model, KaguneForm form) {
        return state(model, form) == QuestLog.State.DONE;
    }

    /**
     * A form as a quest card: its glyph (its tails, the centipede legs), its name, a chip (WORN, READY, NEEDS ITS QUEST, "at mastery 500"; a
     * half kakuja's says so), the bar to it and how far along for the next one.
     */
    static QuestFrame.Card formCard(Model model, KaguneForm form) {
        int mastery = model.mastery();
        QuestLog.State state = state(model, form);
        KaguneForm[] all = RinkakuForms.forms();
        int at = java.util.Arrays.asList(all).indexOf(form);
        int from = at <= 0 ? 0 : all[at - 1].requiredMastery();
        boolean reached = state != QuestLog.State.LOCKED;
        boolean next = !reached && mastery >= from;
        float share = reached ? 1.0F : next ? Math.max(0.0F, Math.min(1.0F, (mastery - from) / (float)Math.max(1, form.requiredMastery() - from))) : 0.0F;
        boolean half = RinkakuForms.kakuja(form);
        String chip = switch (state) {
            case OPEN -> half ? "WORN  ·  HALF KAKUJA" : "WORN";
            case DONE -> half ? "READY  ·  HALF KAKUJA" : "READY";
            case CLOSED -> "NEEDS ITS QUEST";
            default -> "at mastery " + form.requiredMastery();
        };
        return new QuestFrame.Card("pick:" + form.id(), form.displayName(), !reached || state == QuestLog.State.CLOSED, chip, QuestStyle.colour(state),
            QuestStyle.dark(state), next ? Math.round(share * 100.0F) + "%" : "", share, state == QuestLog.State.DONE ? QuestJournalView.BAR_DONE
            : QuestJournalView.BAR, next && share >= 0.8F, (cc, x, y, size, back, t) -> formGlyph(cc, form, state, x, y, size, back, t));
    }

    /** The passive as a card: had (its quest done) or waiting for its quest. */
    static QuestFrame.Card passiveCard(Model model) {
        boolean owned = model.quests().passes(Gate.RINKAKU_NOT_THAT_EASILY);
        int ink = owned ? QuestStyle.GREEN : QuestStyle.colour(QuestLog.State.LOCKED);
        int dark = owned ? QuestStyle.GREEN_DARK : QuestStyle.GREY_DARK;
        return new QuestFrame.Card("pick:passive", PASSIVE_NAME, !owned, owned ? "PASSIVE  ·  YOURS" : "PASSIVE  ·  NEEDS ITS QUEST", ink, dark,
            "", -1.0F, 0, false, (cc, x, y, size, back, t) -> {
                UkakuMenuView.mutationGlyph(cc, UkakuMutation.NOT_THAT_EASILY, owned ? ink : QuestStyle.mix(ink, back, 0.45F), x, y, size, back, t);
                if (!owned) {
                    QuestStyle.lock(cc, x + size / 2, y + size / 2, size / 2, QuestStyle.mix(QuestStyle.GOLD, back, 0.2F), back);
                }
            });
    }

    /** His RC: how much, a bar, what happens at none. */
    private static int rc(UiCanvas c, Model model, int x, int y, int width) {
        float share = model.maxEnergy() <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, model.energy() / model.maxEnergy()));
        y = QuestFrame.section(c, "RC", Math.round(model.energy()) + " / " + Math.round(model.maxEnergy()), x, y, width);
        int barY = y + 3;
        c.batch(() -> {
            UkakuMenuView.capsule(c, x, barY, width, 3, QuestJournalView.BAR_BACK);
            UkakuMenuView.capsule(c, x, barY, Math.max(6, Math.round(width * share)), 3, QuestJournalView.BAR);
        });
        c.text("At 0 the kagune goes in for 10 s", x, y + 10, QuestFrame.MUTED, false);
        return y + 22;
    }

    /**
     * A form, as the journal shows a quest: its name large, its mastery a chip; where he stands with it; its story; the form itself in a
     * framed tile beside its limbs, speed and control, and its moves as a checklist; WEAR THIS FORM while it may be put on, a stamp when worn,
     * else what it waits for.
     */
    private static void formDetail(UiCanvas c, Model model, KaguneForm form, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY,
                                   int x, int y, int width, double time) {
        int mastery = model.mastery();
        QuestLog.State state = state(model, form);
        c.push();
        c.translate(x, y);
        c.scale(1.25F);
        c.text(c.fit(form.displayName(), Math.round((width - 110) / 1.25F)), 0, 0, QuestStyle.WHITE, true);
        c.pop();
        String chip = form.requiredMastery() == 0 ? "From the start" : "Mastery " + form.requiredMastery();
        QuestStyle.chip(c, x + width - c.width(chip) - 12, y, chip, 0xFF1E0A0E, QuestStyle.RED_DARK, QuestFrame.MUTED);
        int rowY = y + 16;
        Gate gate = Gates.rinkaku(form);
        String standing = switch (state) {
            case OPEN -> "Worn now" + (model.active() ? ", out" : "");
            case DONE -> "Yours: wear it below";
            case CLOSED -> "Opens with " + QuestFrame.opener(gate);
            default -> "Ahead of you: at mastery " + form.requiredMastery();
        };
        c.text(c.fit(standing, width), x, rowY, state == QuestLog.State.OPEN ? QuestJournalView.OPEN : state == QuestLog.State.DONE
            ? QuestJournalView.DONE : QuestFrame.MUTED, false);
        rowY += 13;
        rowY = QuestFrame.story(c, story(form), x, rowY, width) + 8;
        preview(c, model, form, state, x, rowY, TILE_WIDTH, TILE_HEIGHT, time);
        int rx = x + TILE_WIDTH + 12;
        int rw = width - TILE_WIDTH - 12;
        int ink = state == QuestLog.State.LOCKED ? QuestFrame.MUTED : QuestFrame.VALUE;
        int ry = QuestFrame.section(c, "THE ORGAN", rx, rowY, rw);
        QuestFrame.row(c, "Limbs", limbs(form), rx, ry, 58, rw, ink);
        int speed = RinkakuForms.speed(form);
        QuestFrame.row(c, "Speed", speed < 0 ? "his own" : "Speed " + UkakuMenuView.roman(speed + 1) + ", kagune out", rx, ry + 12, 58, rw, ink);
        QuestFrame.row(c, "Control", RinkakuForms.autonomous(form) ? "its legs fight on their own" : RinkakuForms.kakuja(form) ? "yours; K tap brings it"
            : "yours", rx, ry + 24, 58, rw, ink);
        ry += 42;
        List<int[]> moves = new ArrayList<>();
        KaguneForm[] all = RinkakuForms.forms();
        int at = java.util.Arrays.asList(all).indexOf(form);
        int until = at + 1 < all.length ? all[at + 1].requiredMastery() : Integer.MAX_VALUE;
        TreeMap<Integer, String> steps = steps();
        int got = 0;
        for (Map.Entry<Integer, String> step : steps.entrySet()) {
            if (step.getKey() >= form.requiredMastery() && step.getKey() < until) {
                moves.add(new int[] {step.getKey()});
                got += mastery >= step.getKey() ? 1 : 0;
            }
        }
        ry = QuestFrame.section(c, "STEPS", got + " / " + moves.size(), rx, ry, rw);
        int nextStep = steps.ceilingKey(mastery + 1) == null ? -1 : steps.ceilingKey(mastery + 1);
        for (int[] move : moves) {
            QuestFrame.checkRow(c, mastery >= move[0], move[0] != nextStep && mastery < move[0], steps.get(move[0]), String.valueOf(move[0]), rx, ry, rw);
            ry += 14;
        }
        rowY = Math.max(rowY + TILE_HEIGHT, ry) + 8;
        if (state == QuestLog.State.OPEN) {
            QuestFrame.stamp(c, x, rowY, "WORN", QuestStyle.RED, QuestStyle.RED_DARK);
        } else {
            String label = switch (state) {
                case DONE -> "WEAR THIS FORM";
                case CLOSED -> "NEEDS ITS QUEST";
                default -> "AT MASTERY " + form.requiredMastery();
            };
            QuestFrame.button(c, "form:" + form.id(), label, x, rowY, 200, 22, state == QuestLog.State.DONE, false, hits, mouseX, mouseY, time);
        }
    }

    /** A form's limbs in words. */
    static String limbs(KaguneForm form) {
        if (form.hasCentipedeLegs()) {
            int tentacles = form.limbCount() - 2;
            return tentacles > 0 ? "2 centipede legs, " + tentacles + " tentacles" : "2 centipede legs";
        }
        return form.limbCount() + (form.limbCount() == 1 ? " tail" : " tails");
    }

    /** What each form is, told as a quest's story. */
    static String story(KaguneForm form) {
        if (RinkakuForms.autonomous(form)) {
            return "Your first half kakuja: two centipede legs out of your back that hunt and fight on their own while you run. A tap of K brings "
                + "out the strongest you have.";
        }
        if (RinkakuForms.kakuja(form)) {
            return "Your second half kakuja: the centipede legs and four tentacles together, all yours to command. Right click dashes (S held: "
                + "back).";
        }
        return switch (form.limbCount()) {
            case 1 -> "One tail out of your lower back, raw and hungry. It strikes, grapples and throws what it takes.";
            case 2 -> "A second tail: two to strike with, to grab and to throw with, to crush what they hold.";
            default -> "Four tails: the full Rinkaku, crushing, lifting and slamming with all of them at once.";
        };
    }

    /** The form itself in a framed tile (its edge beating while worn): its tails, its centipede legs; veiled with a padlock when ahead. */
    private static void preview(UiCanvas c, Model model, KaguneForm form, QuestLog.State state, int x, int y, int width, int height, double time) {
        int edge = state == QuestLog.State.OPEN ? QuestStyle.mix(QuestStyle.RED_DARK, QuestStyle.RED, 0.5F + 0.5F * (float)Math.sin(time * 2.2))
            : state == QuestLog.State.DONE ? QuestStyle.GREEN_DARK : QuestStyle.TILE_EDGE;
        c.batch(() -> {
            QuestStyle.box(c, x, y, width, height, QuestStyle.TILE, edge);
            c.gradient(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF12060A, 0xFF070305);
        });
        PathChoiceLayout.Box box = new PathChoiceLayout.Box(x + 8, y + 6, width - 16, height - 30);
        boolean locked = state == QuestLog.State.LOCKED || state == QuestLog.State.CLOSED;
        int tails = form.hasCentipedeLegs() ? Math.max(0, form.limbCount() - 2) : form.limbCount();
        List<PathSilhouette.Mark> marks = PathSilhouette.rinkakuForm(box, time, locked ? 0.0F : 1.0F, tails, form.hasCentipedeLegs(), 0x0C080E);
        c.batch(() -> {
            for (PathSilhouette.Mark mark : marks) {
                UkakuMenuView.draw(c, mark);
            }
            if (locked) {
                c.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xA0080306);
                QuestStyle.lock(c, x + width / 2 - 13, y + (height - 26) / 2 - 12, 26, QuestStyle.mix(QuestStyle.GOLD, QuestStyle.TILE, 0.2F), 0xFF0A0405);
            }
        });
        c.centered(limbs(form), x + width / 2, y + height - 15, locked ? QuestFrame.FAINT : 0xFFB0767A, false);
    }

    /** The passive, as a quest: its name, what it does, what it changes, and its quest (or a stamp once it is his). */
    private static void passiveDetail(UiCanvas c, Model model, int x, int y, int width) {
        Gate gate = Gate.RINKAKU_NOT_THAT_EASILY;
        boolean owned = model.quests().passes(gate);
        c.push();
        c.translate(x, y);
        c.scale(1.25F);
        c.text(c.fit(PASSIVE_NAME, Math.round((width - 80) / 1.25F)), 0, 0, QuestStyle.WHITE, true);
        c.pop();
        QuestStyle.chip(c, x + width - c.width("Passive") - 12, y, "Passive", 0xFF1E0A0E, QuestStyle.RED_DARK, QuestFrame.MUTED);
        int rowY = y + 16;
        c.text(c.fit(owned ? "Yours for good: its quest is done" : "Opens with " + QuestFrame.opener(gate), width), x, rowY,
            owned ? QuestJournalView.DONE : QuestJournalView.OPEN, false);
        rowY += 13;
        rowY = QuestFrame.story(c, String.join(" ", PASSIVE_TEXT), x, rowY, width) + 10;
        rowY = QuestFrame.section(c, "WITH IT", "now, then with it", x, rowY, width);
        String[][] rows = {{"A killing blow", "you die", "you're blown apart"}, {"Then", "you respawn", "all forget you; you knit back"},
            {"How often", "never", "once every 5 minutes"}};
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
            QuestFrame.stamp(c, x, rowY, "YOURS", QuestStyle.GREEN, QuestStyle.GREEN_DARK);
        } else {
            int ly = rowY;
            c.batch(() -> QuestStyle.lock(c, x, ly, 12, QuestStyle.GOLD, QuestJournalView.INNER));
            c.text(c.fit("Waits for " + QuestFrame.opener(gate), width - 20), x + 17, rowY + 3, QuestFrame.FAINT, false);
        }
    }

    /** Every step of the Rinkaku on the quest road: reached ticked, the next beating, the rest locked; the picked form's steps ringed. */
    private static QuestFrame.Tip road(UiCanvas c, Model model, String picked, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY,
                                       int x, int y, int width, double time) {
        int mastery = model.mastery();
        y = QuestFrame.section(c, "YOUR ROAD", "a step picks its form", x, y, width);
        TreeMap<Integer, String> steps = steps();
        Integer next = steps.ceilingKey(mastery + 1);
        KaguneForm shown = picked.equals("pick:passive") ? null : pickedForm(model);
        List<QuestFrame.Node> nodes = new ArrayList<>();
        int reached = -1;
        int i = 0;
        for (Map.Entry<Integer, String> step : steps.entrySet()) {
            boolean got = mastery >= step.getKey();
            reached = got ? i : reached;
            QuestLog.State state = got ? QuestLog.State.DONE : next != null && step.getKey().equals(next) ? QuestLog.State.OPEN : QuestLog.State.LOCKED;
            nodes.add(new QuestFrame.Node("step:" + step.getKey(), String.valueOf(step.getKey()), step.getValue() + "  ·  "
                + (got ? "reached" : "at mastery " + step.getKey()), state, shown != null && formAt(step.getKey()) == shown));
            i++;
        }
        return QuestFrame.road(c, nodes, reached, hits, mouseX, mouseY, x, y, width, 64, time);
    }

    // ------------------------------------------------------------------ the final kakuja's tab

    private static QuestFrame.Tip kingTab(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, double time) {
        KingForm picked = pickedKing(model);
        int y = QuestFrame.CONTENT_Y;
        int index = 0;
        for (KingForm form : new KingForm[] {KingForm.WAR, KingForm.PEACE}) {
            QuestFrame.card(c, kingCard(model, form), QuestFrame.LIST_X, y, QuestFrame.LIST_WIDTH, form == picked, hits, mouseX, mouseY, time, index++);
            y += QuestFrame.CARD_HEIGHT + QuestFrame.CARD_GAP;
        }
        int ky = QuestFrame.section(c, "K", QuestFrame.LIST_X, y + 6, QuestFrame.LIST_WIDTH);
        KingForm chosen = model.king().chosen();
        QuestFrame.row(c, "K tap", "Strongest half kakuja", QuestFrame.LIST_X, ky, 52, QuestFrame.LIST_WIDTH, QuestFrame.VALUE);
        QuestFrame.row(c, "K hold", chosen == KingForm.NONE ? "Final kakuja (after the Dragon)" : chosen.displayName() + ", the final kakuja",
            QuestFrame.LIST_X, ky + 12, 52, QuestFrame.LIST_WIDTH, chosen == KingForm.NONE ? QuestFrame.MUTED : QuestFrame.VALUE);
        QuestFrame.row(c, "Half", "Centipede (750), Full Kakuja (1000)", QuestFrame.LIST_X, ky + 24, 52, QuestFrame.LIST_WIDTH, QuestFrame.VALUE);
        c.text(c.fit("Full Kakuja, right click: dash (S held: back)", QuestFrame.LIST_WIDTH), QuestFrame.LIST_X, ky + 40, QuestFrame.MUTED, false);
        kingDetail(c, model, picked, hits, mouseX, mouseY, QuestFrame.DETAIL_X, QuestFrame.CONTENT_Y, DETAIL_WIDTH, time);
        return kingRoad(c, model, hits, mouseX, mouseY, QuestFrame.DETAIL_X, ROAD_Y, DETAIL_WIDTH, time);
    }

    /** The final kakuja shown: the one picked, else the one chosen, else War. */
    static KingForm pickedKing(Model model) {
        if ("pick:peace".equals(model.picked())) {
            return KingForm.PEACE;
        }
        if ("pick:war".equals(model.picked())) {
            return KingForm.WAR;
        }
        return model.king().chosen() == KingForm.PEACE ? KingForm.PEACE : KingForm.WAR;
    }

    /** How War or Peace stands for him: worn, chosen, to choose, forsaken, sealed, or waiting for the Dragon. */
    enum KingState {
        WORN, CHOSEN, CHOOSE, FORSAKEN, SEALED, DRAGON
    }

    static KingState kingState(Model model, KingForm form) {
        King king = model.king();
        if (!king.awake()) {
            return KingState.DRAGON;
        }
        if (king.chosen() == form) {
            return king.worn() == form ? KingState.WORN : KingState.CHOSEN;
        }
        if (!form.built()) {
            return KingState.SEALED;
        }
        return king.chosen() == KingForm.NONE ? KingState.CHOOSE : KingState.FORSAKEN;
    }

    /** War or Peace as a quest card: its mark, its name, a chip; one waiting to be chosen glows gold. */
    static QuestFrame.Card kingCard(Model model, KingForm form) {
        KingState state = kingState(model, form);
        int ink = switch (state) {
            case WORN -> QuestStyle.RED;
            case CHOSEN -> QuestStyle.GREEN;
            case CHOOSE -> QuestStyle.GOLD;
            default -> QuestStyle.colour(QuestLog.State.LOCKED);
        };
        int dark = switch (state) {
            case WORN -> QuestStyle.RED_DARK;
            case CHOSEN -> QuestStyle.GREEN_DARK;
            case CHOOSE -> QuestStyle.GOLD_DARK;
            default -> QuestStyle.GREY_DARK;
        };
        String chip = switch (state) {
            case WORN -> "WORN  ·  HOLD K";
            case CHOSEN -> "YOURS  ·  HOLD K";
            case CHOOSE -> "CHOOSE  ·  ONCE";
            case FORSAKEN -> "FORSAKEN";
            case SEALED -> "SEALED";
            case DRAGON -> "AFTER THE DRAGON";
        };
        boolean dim = state == KingState.FORSAKEN || state == KingState.SEALED || state == KingState.DRAGON;
        String title = form == KingForm.WAR ? "War  ·  the one-eyed king" : "Peace  ·  Rinkaku :re";
        return new QuestFrame.Card("pick:" + form.id(), title, dim, chip, ink, dark, "", -1.0F, 0, state == KingState.CHOOSE,
            (cc, x, y, size, back, t) -> {
                kingGlyph(cc, form, dim ? QuestStyle.mix(ink, back, 0.45F) : ink, x, y, size, back, t);
                if (state == KingState.DRAGON || state == KingState.SEALED) {
                    QuestStyle.lock(cc, x + size / 2, y + size / 2, size / 2, QuestStyle.mix(QuestStyle.GOLD, back, 0.2F), back);
                }
            });
    }

    /** War's glyph an eye (one white point in the black), Peace's a cross blade. */
    static void kingGlyph(UiCanvas c, KingForm form, int ink, int x, int y, int size, int back, double time) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        if (form == KingForm.WAR) {
            QuestStyle.circle(c, cx, cy, Math.round(size * 0.42F), ink);
            QuestStyle.circle(c, cx, cy, Math.round(size * 0.3F), 0xFF050203);
            QuestStyle.circle(c, cx, cy, Math.max(1, Math.round(size * 0.1F)), 0xFFFFFFFF);
            return;
        }
        double turn = Math.toRadians(30.0) + time * 0.3;
        for (int k = 0; k < 4; k++) {
            double a = turn + k * Math.PI / 2.0;
            float length = size * (k % 2 == 0 ? 0.46F : 0.28F);
            QuestStyle.line(c, cx, cy, cx + (float)Math.cos(a) * length, cy + (float)Math.sin(a) * length, Math.max(1.5F, size * 0.14F), ink);
        }
        QuestStyle.circle(c, cx, cy, Math.max(1, Math.round(size * 0.12F)), 0xFFFF2234);
    }

    /**
     * War or Peace, as a quest: its name, its title, where he stands with it, its story; its emblem in a framed tile beside its card's rows;
     * a button to the choice while it may be made, a stamp once it is his, else what it waits for.
     */
    private static void kingDetail(UiCanvas c, Model model, KingForm form, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY,
                                   int x, int y, int width, double time) {
        KingState state = kingState(model, form);
        c.push();
        c.translate(x, y);
        c.scale(1.25F);
        c.text(form == KingForm.WAR ? "War" : "Peace", 0, 0, QuestStyle.WHITE, true);
        c.pop();
        String chip = form == KingForm.WAR ? "The one-eyed king" : "Rinkaku :re";
        QuestStyle.chip(c, x + width - c.width(chip) - 12, y, chip, 0xFF1E0A0E, QuestStyle.RED_DARK, QuestFrame.MUTED);
        int rowY = y + 16;
        String standing = switch (state) {
            case WORN -> "Yours, worn now: hold K to take it off";
            case CHOSEN -> "Yours for good: hold K to let it out";
            case CHOOSE -> "Yours to choose, once and for good";
            case FORSAKEN -> "Forsaken: you chose " + model.king().chosen().displayName();
            case SEALED -> "Sealed: not ready yet. It is coming";
            case DRAGON -> "Waits for the Dragon (Kakuja V2) to go through you";
        };
        c.text(c.fit(standing, width), x, rowY, state == KingState.WORN || state == KingState.CHOSEN ? QuestJournalView.DONE
            : state == KingState.CHOOSE ? QuestJournalView.OPEN : QuestFrame.MUTED, false);
        rowY += 13;
        String story = form == KingForm.WAR ? "The black king: a torn cape, one eye burning white, six tails that hunt and a Black Sun. He takes "
            + "what is in his way." : "The white king: armour like porcelain, kagune wings, six cross blades on his arms. Graceful, and long in "
            + "his blows.";
        rowY = QuestFrame.story(c, story, x, rowY, width) + 8;
        boolean dim = state == KingState.FORSAKEN || state == KingState.SEALED || state == KingState.DRAGON;
        int tileY = rowY;
        int edge = state == KingState.WORN ? QuestStyle.mix(QuestStyle.RED_DARK, QuestStyle.RED, 0.5F + 0.5F * (float)Math.sin(time * 2.2))
            : state == KingState.CHOSEN ? QuestStyle.GREEN_DARK : state == KingState.CHOOSE ? QuestStyle.GOLD_DARK : QuestStyle.TILE_EDGE;
        c.batch(() -> {
            QuestStyle.box(c, x, tileY, TILE_WIDTH, TILE_HEIGHT, QuestStyle.TILE, edge);
            c.gradient(x + 2, tileY + 2, x + TILE_WIDTH - 2, tileY + TILE_HEIGHT - 2, 0xFF12060A, 0xFF070305);
        });
        if (form == KingForm.WAR) {
            KingChoiceView.warEye(c, x + TILE_WIDTH / 2, tileY + TILE_HEIGHT / 2 - 6, time);
        } else {
            KingChoiceView.peaceCross(c, x + TILE_WIDTH / 2, tileY + TILE_HEIGHT / 2 - 6, false, time);
        }
        if (dim) {
            c.batch(() -> {
                c.fill(x + 2, tileY + 2, x + TILE_WIDTH - 2, tileY + TILE_HEIGHT - 2, 0x90080306);
                if (state != KingState.FORSAKEN) {
                    QuestStyle.lock(c, x + TILE_WIDTH / 2 - 13, tileY + TILE_HEIGHT / 2 - 18, 26, QuestStyle.mix(QuestStyle.GOLD, QuestStyle.TILE, 0.2F),
                        0xFF0A0405);
                }
            });
        }
        c.centered(form == KingForm.WAR ? "one eye" : "the cross blade", x + TILE_WIDTH / 2, tileY + TILE_HEIGHT - 15, dim ? QuestFrame.FAINT
            : 0xFFB0767A, false);
        int rx = x + TILE_WIDTH + 12;
        int rw = width - TILE_WIDTH - 12;
        int ry = QuestFrame.section(c, "THE KAKUJA", rx, rowY, rw);
        for (String[] row : form == KingForm.WAR ? KingChoiceView.WAR_ROWS : KingChoiceView.PEACE_ROWS) {
            c.text(row[0], rx, ry, QuestFrame.MUTED, false);
            for (String line : UkakuMenuView.wrap(c, row[1], rw - 70)) {
                c.text(line, rx + 70, ry, dim ? QuestFrame.MUTED : QuestFrame.VALUE, false);
                ry += 12;
            }
            ry += 1;
        }
        rowY = Math.max(rowY + TILE_HEIGHT, ry) + 8;
        if (state == KingState.WORN || state == KingState.CHOSEN) {
            QuestFrame.stamp(c, x, rowY, "YOURS", QuestStyle.GREEN, QuestStyle.GREEN_DARK);
        } else {
            String label = switch (state) {
                case CHOOSE -> "CHOOSE ON ITS CARDS";
                case FORSAKEN -> "FORSAKEN";
                case SEALED -> "SEALED";
                default -> "AFTER THE DRAGON";
            };
            QuestFrame.button(c, "king:" + form.id(), label, x, rowY, 200, 22, state == KingState.CHOOSE, true, hits, mouseX, mouseY, time);
            if (state == KingState.CHOOSE) {
                c.text("once, and never again", x + 212, rowY + 8, QuestStyle.GOLD, false);
            }
        }
    }

    /** The road to the final kakuja: the Full Kakuja at 1000, the Dragon through him, War or Peace chosen. */
    private static QuestFrame.Tip kingRoad(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, int x,
                                           int y, int width, double time) {
        y = QuestFrame.section(c, "TO THE FINAL KAKUJA", x, y, width);
        King king = model.king();
        boolean full = RinkakuForms.finalKakuja().isUnlocked(model.mastery());
        boolean dragon = king.awake();
        boolean chose = king.chosen() != KingForm.NONE;
        boolean[] done = {full, dragon, chose};
        String[][] words = {{"1000", "The Full Kakuja, at mastery 1000"}, {"Dragon", "The Dragon (Kakuja V2) through you"},
            {"Choose", "War or Peace, once and for good"}};
        List<QuestFrame.Node> nodes = new ArrayList<>();
        int reached = -1;
        boolean nextSet = false;
        for (int i = 0; i < 3; i++) {
            QuestLog.State state = done[i] ? QuestLog.State.DONE : !nextSet ? QuestLog.State.OPEN : QuestLog.State.LOCKED;
            if (!done[i]) {
                nextSet = true;
            } else {
                reached = i;
            }
            nodes.add(new QuestFrame.Node("road:" + i, words[i][0], words[i][1] + "  ·  " + (done[i] ? "done" : "ahead"), state, false));
        }
        return QuestFrame.road(c, nodes, reached, hits, mouseX, mouseY, x, y, width, 180, time);
    }

    // ------------------------------------------------------------------ glyphs

    /** A form's glyph in its tile: its tails (one, two, four) curling up, the centipede legs as two thick segmented arcs. */
    static void formGlyph(UiCanvas c, KaguneForm form, QuestLog.State state, int x, int y, int size, int back, double time) {
        int ink = QuestStyle.colour(state);
        int dim = state == QuestLog.State.LOCKED || state == QuestLog.State.CLOSED ? QuestStyle.mix(ink, back, 0.5F) : ink;
        float s = size;
        float sway = state == QuestLog.State.OPEN ? (float)Math.sin(time * 2.4) * 0.05F : 0.0F;
        float rx = x + s * 0.5F;
        float ry = y + s * 0.92F;
        int tails = form.hasCentipedeLegs() ? Math.max(0, form.limbCount() - 2) : form.limbCount();
        if (form.hasCentipedeLegs()) {
            for (int side = -1; side <= 1; side += 2) {
                float a = (float)Math.toRadians(side < 0 ? -118.0 : -62.0) + sway * side;
                UkakuMenuView.tentacle(c, rx, ry, a, s * 0.78F, -side * 0.5F, s * 0.2F, dim);
                // its segments: a notch across it every few steps
                float px = rx;
                float py = ry;
                for (int k = 1; k <= 3; k++) {
                    float t = k / 4.0F;
                    float aa = a - side * 0.5F * t * t;
                    px += (float)Math.cos(aa) * s * 0.19F;
                    py += (float)Math.sin(aa) * s * 0.19F;
                    QuestStyle.line(c, px - (float)Math.sin(aa) * s * 0.1F, py + (float)Math.cos(aa) * s * 0.1F, px + (float)Math.sin(aa) * s * 0.1F,
                        py - (float)Math.cos(aa) * s * 0.1F, 1.0F, back);
                }
            }
        }
        float[] angles = tails <= 1 ? new float[] {-60.0F} : tails == 2 ? new float[] {-125.0F, -55.0F}
            : new float[] {-150.0F, -112.0F, -68.0F, -30.0F};
        for (int i = 0; i < tails; i++) {
            float a = (float)Math.toRadians(angles[i]) + sway * (i % 2 == 0 ? 1 : -1);
            UkakuMenuView.tentacle(c, rx, ry - (form.hasCentipedeLegs() ? s * 0.1F : 0.0F), a, s * (form.hasCentipedeLegs() ? 0.5F : 0.74F),
                (angles[i] < -90.0F ? -1 : 1) * 0.8F, s * (form.hasCentipedeLegs() ? 0.12F : tails == 1 ? 0.22F : 0.17F), dim);
        }
        if (state == QuestLog.State.LOCKED || state == QuestLog.State.CLOSED) {
            QuestStyle.lock(c, x + size / 2, y + size / 2, size / 2, QuestStyle.mix(QuestStyle.GOLD, back, 0.2F), back);
        }
    }

    /** 1.0.41: the passive's name and what it does, as the menu shows them. */
    static final String PASSIVE_NAME = "You Thought You'd Get Rid of Me That Easily?";
    static final String[] PASSIVE_TEXT = {"Every 5 min, death blows you apart and", "everything forgets you; you knit back",
        "together, slowly, and stand up whole."};
}
