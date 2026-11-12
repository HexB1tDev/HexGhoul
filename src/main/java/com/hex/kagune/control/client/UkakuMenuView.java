package com.hex.kagune.control.client;

import com.hex.kagune.control.ComboStyle;
import com.hex.kagune.control.UkakuStage;
import com.hex.kagune.control.client.ui.UiCanvas;
import com.hex.kagune.control.kakugan.KakuganEyes;
import com.hex.kagune.control.quest.Gate;
import com.hex.kagune.control.quest.Gates;
import com.hex.kagune.control.quest.QuestBook;
import com.hex.kagune.control.quest.QuestLog;
import com.hex.kagune.control.ukaku.UkakuMutation;
import com.hex.kagune.control.ukaku.UkakuRules;
import com.hex.kagune.control.ukaku.UkakuUnlock;
import com.hex.kagune.control.ukaku.WingForm;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Ukaku path's kagune menu (the menu key), drawn on any {@link UiCanvas}: his transformation (one wing, two wings growing to full size
 * at 500, the first kakuja at 750 and the final at 1000), a live wing preview, his combat readout, the kakugan placement, and his mutations
 * (a point at each milestone, spent on whichever he wants).
 *
 * <p>4.1.56 (the owner: "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as interactable as it is,
 * Dialog, mastery menus ALL OF THEM"; of the quest menu: "why not just a cute box?"): the quest menu's box ({@link QuestFrame}), the world
 * seen round it. The header as the journal's (the title, the matrix and mastery in red, a bar to the next unlock); two tabs, ORGAN and
 * MUTATIONS (its badge the points to spend). ORGAN: the five stages as quest cards on the left (picked by a click), the combat readout and
 * the kakugan under them; on the right the stage picked as a quest: its story, its wings in a framed tile, the organ's rows, its abilities
 * as a checklist, and the road of every unlock (a click on a step picks its stage). MUTATIONS: the mutations as cards, the one picked shown
 * on the right with what it costs and an UNLOCK button, so a stray click spends nothing; the points and where the next comes from.
 */
public final class UkakuMenuView {
    /** The old board's virtual size (the other kagune menus and the talk screen still use it). */
    public static final int UI_WIDTH = 960;
    public static final int UI_HEIGHT = 520;
    /** 4.1.56: the box's size (the screen scales it to fit, the world seen round it). */
    public static final int BOX_WIDTH = 740;
    public static final int BOX_HEIGHT = 440;
    /** The detail column's width, and where the road starts. */
    static final int DETAIL_WIDTH = BOX_WIDTH - QuestFrame.DETAIL_X - 12;
    static final int ROAD_Y = BOX_HEIGHT - 64;
    /** The preview tile's size. */
    static final int TILE_WIDTH = 190;
    static final int TILE_HEIGHT = 150;

    /** 4.1.56: the menu's tabs. */
    public enum Tab {
        ORGAN("ORGAN"), MUTATIONS("MUTATIONS");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    /**
     * What the menu shows; 4.1.56: the tab open and what is picked in it ("stage:NAME" or "unlock:NAME" on the organ's tab, "pick:N" on the
     * mutations'; null: the stage he is at, or the mutation most worth a look).
     */
    public record Model(int mastery, boolean active, WingForm form, KakuganEyes eyes, int mutations, QuestLocks quests, Tab tab, String picked) {
        public Model {
            tab = tab == null ? Tab.ORGAN : tab;
            quests = quests == null ? QuestLocks.ALL : quests;
        }

        public Model(int mastery, boolean active, WingForm form, KakuganEyes eyes) {
            this(mastery, active, form, eyes, 0);
        }

        public Model(int mastery, boolean active, WingForm form, KakuganEyes eyes, int mutations) {
            this(mastery, active, form, eyes, mutations, QuestLocks.ALL);
        }

        public Model(int mastery, boolean active, WingForm form, KakuganEyes eyes, int mutations, QuestLocks quests) {
            this(mastery, active, form, eyes, mutations, quests, Tab.ORGAN, null);
        }
    }

    /** A clickable area on the board (virtual coordinates). */
    public record Rect(int x, int y, int width, int height) {
        public boolean contains(double px, double py) {
            return px >= this.x && px <= this.x + this.width && py >= this.y && py <= this.y + this.height;
        }
    }

    private UkakuMenuView() {
    }

    /**
     * Draws the box at its own size (the caller scales and places the canvas) and returns its clickable areas by name: "tab:ORGAN",
     * "tab:MUTATIONS", "stage:NAME", "unlock:NAME", "eyes" (the kakugan screen), "pick:N" (a mutation looked at), "mutation:N" (its UNLOCK
     * button, only while it can be unlocked), "milestone:N". {@code mouseX}/{@code mouseY} are in the box's coordinates.
     */
    public static Map<String, Rect> draw(UiCanvas c, Model model, double mouseX, double mouseY, double time) {
        Map<String, Rect> hits = new LinkedHashMap<>();
        // The quest menu's plate, its honeycomb in the header's empty middle (between the title and the bar).
        QuestFrame.plate(c, BOX_WIDTH, BOX_HEIGHT, time, 292, 6, 170, 38);
        header(c, model, time);
        int points = points(model);
        QuestFrame.tabs(c, List.of(new QuestFrame.Tab(Tab.ORGAN.name(), Tab.ORGAN.label, 0, false),
            new QuestFrame.Tab(Tab.MUTATIONS.name(), Tab.MUTATIONS.label, points, true)), model.tab().name(), hits, mouseX, mouseY,
            QuestFrame.LIST_X, QuestFrame.TABS_Y, time);
        QuestFrame.Tip tip = model.tab() == Tab.MUTATIONS ? mutationsTab(c, model, hits, mouseX, mouseY, time)
            : organTab(c, model, hits, mouseX, mouseY, time);
        QuestFrame.tooltip(c, tip, 8, BOX_WIDTH - 8);
        return hits;
    }

    /** Mutation points to spend (the milestones' and the quests'). */
    static int points(Model model) {
        return UkakuMutation.available(model.mastery(), model.mutations(), model.quests().points());
    }

    // ------------------------------------------------------------------ the header

    /**
     * The title, the matrix and mastery in red; top right a bar from the last unlock reached to the next ("760 / 800 · next: Rushing Death
     * (Z), Thorn Fan"); on the tabs' row the kagune out or in (R) and the points to spend as chips.
     */
    private static void header(UiCanvas c, Model model, double time) {
        int mastery = model.mastery();
        UkakuUnlock next = UkakuUnlock.next(mastery);
        int from = 0;
        for (UkakuUnlock unlock : UkakuUnlock.values()) {
            if (unlock.reached(mastery)) {
                from = Math.max(from, unlock.mastery());
            }
        }
        float share = next == null ? 1.0F : (mastery - from) / (float)Math.max(1, next.mastery() - from);
        String goal = next == null ? "Every step of the Ukaku reached" : next.comingSoon() && mastery >= next.mastery()
            ? "Next: " + next.text() + " (coming soon)" : mastery + " / " + next.mastery() + "  ·  next: " + next.text();
        QuestFrame.header(c, BOX_WIDTH, "UKAKU", "RC CELL / BLACK RABBIT MATRIX  ·  MASTERY " + mastery, share, next == null, goal, time);
        int right = BOX_WIDTH - 12;
        boolean out = model.active();
        right = QuestFrame.chipLeft(c, right, 56, out ? "KAGUNE OUT  ·  R RETRACTS" : "KAGUNE IN  ·  R MANIFESTS",
            out ? 0xFF2A0A10 : 0xFF181012, out ? QuestStyle.RED_DARK : QuestStyle.GREY_DARK, out ? 0xFFFF8A7A : QuestJournalView.MUTED);
        int points = points(model);
        if (points > 0) {
            QuestFrame.chipLeft(c, right, 56, points + " PT" + (points == 1 ? "" : "S") + " TO SPEND", QuestFrame.GOLD_FILL, QuestStyle.GOLD_DARK,
                QuestStyle.GOLD);
        }
    }

    // ------------------------------------------------------------------ the organ's tab

    private static QuestFrame.Tip organTab(UiCanvas c, Model model, Map<String, Rect> hits, double mouseX, double mouseY, double time) {
        UkakuStage picked = pickedStage(model);
        int y = QuestFrame.CONTENT_Y;
        int index = 0;
        for (UkakuStage stage : UkakuStage.values()) {
            QuestFrame.card(c, stageCard(model, stage), QuestFrame.LIST_X, y, QuestFrame.LIST_WIDTH, stage == picked, hits, mouseX, mouseY, time,
                index++);
            y += QuestFrame.CARD_HEIGHT + QuestFrame.CARD_GAP;
        }
        y = combat(c, model, QuestFrame.LIST_X, y + 6, QuestFrame.LIST_WIDTH);
        kakugan(c, model, hits, mouseX, mouseY, QuestFrame.LIST_X, y + 6, QuestFrame.LIST_WIDTH, time);
        stageDetail(c, model, picked, QuestFrame.DETAIL_X, QuestFrame.CONTENT_Y, DETAIL_WIDTH, time);
        return unlockRoad(c, model, picked, hits, mouseX, mouseY, QuestFrame.DETAIL_X, ROAD_Y, DETAIL_WIDTH, time);
    }

    /** The stage shown on the right: the one picked (or the stage of the step picked on the road), else the one he is at. */
    static UkakuStage pickedStage(Model model) {
        String picked = model.picked();
        if (picked != null) {
            try {
                if (picked.startsWith("stage:")) {
                    return UkakuStage.valueOf(picked.substring(6));
                }
                if (picked.startsWith("unlock:")) {
                    return stageOf(UkakuUnlock.valueOf(picked.substring(7)));
                }
            } catch (IllegalArgumentException unknown) {
                // an old pick: the stage he is at
            }
        }
        return UkakuStage.forMastery(model.mastery());
    }

    /** The stage a step of the road belongs to (the last whose mastery it has reached). */
    static UkakuStage stageOf(UkakuUnlock unlock) {
        UkakuStage of = UkakuStage.ONE_WING;
        for (UkakuStage stage : UkakuStage.values()) {
            if (unlock.mastery() >= stage.requiredMastery()) {
                of = stage;
            }
        }
        return of;
    }

    /** The steps of the road that come with {@code stage} (from its mastery up to the next stage's). */
    static List<UkakuUnlock> abilities(UkakuStage stage) {
        UkakuStage[] all = UkakuStage.values();
        int until = stage.ordinal() + 1 < all.length ? all[stage.ordinal() + 1].requiredMastery() : Integer.MAX_VALUE;
        List<UkakuUnlock> out = new ArrayList<>();
        for (UkakuUnlock unlock : UkakuUnlock.values()) {
            if (unlock.mastery() >= stage.requiredMastery() && unlock.mastery() < until) {
                out.add(unlock);
            }
        }
        return out;
    }

    /** How a stage stands: the one he is at open (red), those behind him done (green), those ahead locked. */
    static QuestLog.State state(Model model, UkakuStage stage) {
        if (stage.comingSoon() || model.mastery() < stage.requiredMastery()) {
            return QuestLog.State.LOCKED;
        }
        return stage == UkakuStage.forMastery(model.mastery()) ? QuestLog.State.OPEN : QuestLog.State.DONE;
    }

    /**
     * A stage as a quest card: its glyph (wings, tentacles), its name, a chip (CURRENT, REACHED, "TAP K"/"HOLD K" for the kakuja, "at mastery
     * 750" ahead), the bar to it along its foot and how far along for the next one (glowing gold once it is near).
     */
    static QuestFrame.Card stageCard(Model model, UkakuStage stage) {
        int mastery = model.mastery();
        QuestLog.State state = state(model, stage);
        boolean reached = state != QuestLog.State.LOCKED;
        UkakuStage[] all = UkakuStage.values();
        int from = stage.ordinal() == 0 ? 0 : all[stage.ordinal() - 1].requiredMastery();
        boolean next = !reached && mastery >= from;
        float share = reached ? 1.0F : next ? Math.max(0.0F, Math.min(1.0F, (mastery - from) / (float)Math.max(1, stage.requiredMastery() - from))) : 0.0F;
        String chip = stage.comingSoon() ? "COMING SOON" : !reached ? "at mastery " + stage.requiredMastery() : stage == UkakuStage.KAKUJA_I ? "TAP K"
            : stage == UkakuStage.KAKUJA_II ? "HOLD K" : state == QuestLog.State.OPEN ? "CURRENT" : "REACHED";
        String right = next ? Math.round(share * 100.0F) + "%" : state == QuestLog.State.OPEN && (stage == UkakuStage.KAKUJA_I
            || stage == UkakuStage.KAKUJA_II) ? "CURRENT" : "";
        return new QuestFrame.Card("stage:" + stage.name(), stage.displayName(), !reached, chip, QuestStyle.colour(state), QuestStyle.dark(state),
            right, share, state == QuestLog.State.DONE ? QuestJournalView.BAR_DONE : QuestJournalView.BAR, next && share >= 0.8F,
            (cc, x, y, size, back, t) -> stageGlyph(cc, stage, state, x, y, size, back, t));
    }

    /** The combat readout, as the organ's rows: his combo, his blows, the vortex and the crystal, the cost and his speed. */
    private static int combat(UiCanvas c, Model model, int x, int y, int width) {
        int mastery = model.mastery();
        WingForm form = model.form().shown(mastery);
        y = QuestFrame.section(c, "COMBAT READOUT", x, y, width);
        int label = 66;
        boolean vortex = mastery >= UkakuRules.VORTEX_MASTERY;
        boolean crystal = mastery >= UkakuRules.CRYSTAL_MASTERY;
        boolean agile = UkakuMutation.AGILITY_BOOST.in(model.mutations());
        QuestFrame.row(c, "Combo", comboName(UkakuRules.style(mastery, form, false)), x, y, label, width, QuestFrame.VALUE);
        QuestFrame.row(c, "Blows", String.format("%.1f damage", UkakuRules.meleeDamage(mastery)), x, y + 12, label, width, QuestFrame.VALUE);
        QuestFrame.row(c, "Vortex", vortex ? String.format("%.1f + fire, %.0f blocks", UkakuRules.vortexDamage(mastery), UkakuRules.VORTEX_RANGE)
            : "at mastery " + UkakuRules.VORTEX_MASTERY, x, y + 24, label, width, vortex ? QuestFrame.VALUE : QuestFrame.FAINT);
        QuestFrame.row(c, "Crystal", crystal ? String.format("%.0f blocks, bleed", UkakuRules.crystalRange(mastery))
            : "at mastery " + UkakuRules.CRYSTAL_MASTERY, x, y + 36, label, width, crystal ? QuestFrame.VALUE : QuestFrame.FAINT);
        QuestFrame.row(c, "RC cost", String.format("x%.2f", UkakuRules.rcCostMultiplier(mastery)), x, y + 48, label, width, QuestFrame.VALUE);
        QuestFrame.row(c, "Speed", "Speed " + roman(UkakuRules.speedAmplifier(mastery) + (agile ? UkakuRules.AGILITY_SPEED : 0) + 1) + ", Jump "
            + roman(UkakuRules.jumpAmplifier(mastery) + (agile ? UkakuRules.AGILITY_JUMP : 0) + 1), x, y + 60, label, width, QuestFrame.VALUE);
        return y + 72;
    }

    /** The kakugan: where the eyes sit, an eye in a tile, and the button to move them ("eyes": the kakugan screen). */
    private static void kakugan(UiCanvas c, Model model, Map<String, Rect> hits, double mouseX, double mouseY, int x, int y, int width, double time) {
        y = QuestFrame.section(c, "KAKUGAN", x, y, width);
        KakuganEyes eyes = model.eyes();
        int ty = y;
        c.batch(() -> {
            QuestStyle.box(c, x, ty - 2, 16, 14, QuestStyle.TILE, QuestStyle.RED_DARK);
            QuestStyle.glyph(c, com.hex.kagune.control.quest.Quest.Line.GHOUL, QuestLog.State.OPEN, x + 1, ty - 2, 14, QuestStyle.TILE, time);
        });
        String where = "Column " + (eyes.rightX() + 1) + " / " + (eyes.leftX() + 1) + ", row " + (eyes.rightY() + 1)
            + (!eyes.oneEyed() ? "" : eyes.ghoul() == KakuganEyes.RIGHT ? ", right only" : ", left only");
        c.text(c.fit(where, width - 22), x + 22, y + 1, QuestFrame.MUTED, false);
        QuestFrame.button(c, "eyes", "ADJUST KAKUGAN", x, y + 17, width, 16, true, false, hits, mouseX, mouseY, time);
    }

    /**
     * The stage picked, as the journal shows a quest: its name large, its mastery a chip; where he stands with it (gold), its story; its wings
     * in a framed tile beside the organ's rows and its abilities as a checklist; what it waits for when it is ahead.
     */
    private static void stageDetail(UiCanvas c, Model model, UkakuStage stage, int x, int y, int width, double time) {
        int mastery = model.mastery();
        QuestLog.State state = state(model, stage);
        boolean reached = state != QuestLog.State.LOCKED;
        c.push();
        c.translate(x, y);
        c.scale(1.25F);
        c.text(c.fit(stage.displayName(), Math.round((width - 110) / 1.25F)), 0, 0, QuestStyle.WHITE, true);
        c.pop();
        String chip = stage.requiredMastery() == 0 ? "From the start" : "Mastery " + stage.requiredMastery();
        int chipWidth = c.width(chip) + 12;
        QuestStyle.chip(c, x + width - chipWidth, y, chip, 0xFF1E0A0E, QuestStyle.RED_DARK, QuestFrame.MUTED);
        int rowY = y + 16;
        c.text(c.fit(standing(model, stage), width), x, rowY, state == QuestLog.State.DONE ? QuestJournalView.DONE : state == QuestLog.State.OPEN
            ? QuestJournalView.OPEN : QuestFrame.MUTED, false);
        rowY += 13;
        rowY = QuestFrame.story(c, story(stage), x, rowY, width) + 8;
        preview(c, model, stage, x, rowY, TILE_WIDTH, TILE_HEIGHT, time);
        int rx = x + TILE_WIDTH + 12;
        int rw = width - TILE_WIDTH - 12;
        int ry = QuestFrame.section(c, "THE ORGAN", rx, rowY, rw);
        WingForm form = stageForm(model, stage);
        int shownAt = stageMastery(model, stage);
        QuestFrame.row(c, "Kagune", stage == UkakuStage.forMastery(mastery) && !form.kakuja() ? wingsText(mastery, form) : stageWings(stage), rx, ry, 52,
            rw, reached ? QuestFrame.VALUE : QuestFrame.MUTED);
        QuestFrame.row(c, "Form", form.displayName(shownAt), rx, ry + 12, 52, rw, reached ? QuestFrame.VALUE : QuestFrame.MUTED);
        QuestFrame.row(c, form.kakuja() ? "Needles" : "Length", form.kakuja() ? String.format("fly %.0f blocks", UkakuRules.NEEDLE_RANGE)
            : String.format("%.1f blocks", UkakuRules.wingLength(shownAt, form)), rx, ry + 24, 52, rw, reached ? QuestFrame.VALUE : QuestFrame.MUTED);
        ry += 42;
        List<UkakuUnlock> abilities = abilities(stage);
        int got = 0;
        for (UkakuUnlock unlock : abilities) {
            got += unlock.reached(mastery) ? 1 : 0;
        }
        ry = QuestFrame.section(c, "ABILITIES", got + " / " + abilities.size(), rx, ry, rw);
        UkakuUnlock next = UkakuUnlock.next(mastery);
        for (UkakuUnlock unlock : abilities) {
            QuestFrame.checkRow(c, unlock.reached(mastery), unlock != next, unlock.text(), String.valueOf(unlock.mastery()), rx, ry, rw);
            ry += 14;
        }
        rowY = Math.max(rowY + TILE_HEIGHT, ry) + 6;
        if (!reached) {
            int ly = rowY;
            c.batch(() -> QuestStyle.lock(c, x, ly, 12, QuestStyle.GOLD, QuestJournalView.INNER));
            String waits = stage.comingSoon() ? "Not in the game yet" : "Waits for mastery " + stage.requiredMastery() + "  ("
                + (stage.requiredMastery() - mastery) + " to go)";
            c.text(waits, x + 17, rowY + 3, QuestFrame.FAINT, false);
        }
    }

    /** Where he stands with {@code stage}, in a line under its name. */
    static String standing(Model model, UkakuStage stage) {
        QuestLog.State state = state(model, stage);
        if (state == QuestLog.State.LOCKED) {
            return stage.comingSoon() ? "Coming soon" : "Ahead of you: at mastery " + stage.requiredMastery();
        }
        if (stage == UkakuStage.KAKUJA_I) {
            return (state == QuestLog.State.OPEN ? "Yours now: " : "Yours: ") + "tap K to transform, tap it again to come back";
        }
        if (stage == UkakuStage.KAKUJA_II) {
            return "Yours now: hold K to transform";
        }
        return state == QuestLog.State.OPEN ? "Yours now" + (model.active() ? ", out" : "") : "Behind you: reached";
    }

    /** What each stage is, told as a quest's story. */
    static String story(UkakuStage stage) {
        return switch (stage) {
            case ONE_WING -> "A new Ukaku: one small wing out of your left shoulder blade, Touka's side. It burns and it is quick: fire shots, the "
                + "Rabbit Hop, the Flare.";
            case TWIN_WINGS -> "The right wing comes in. Two small wings that grow with every bit of mastery until they reach their full size at "
                + UkakuStage.FULL_SIZE_MASTERY + ".";
            case FULL_WINGS -> "Your wings at their full size, in the full :re shape. The Black Rabbit and the Crystal Bloom are yours.";
            case KAKUJA_I -> "Not wings at all: six armored tentacles grow from your shoulder blades, and armor grows over your body.";
            case KAKUJA_II -> "Three of the six stay needles, the other three burn into fire tendrils, under a half mask.";
        };
    }

    /** The wings a stage has, in a few words. */
    static String stageWings(UkakuStage stage) {
        return switch (stage) {
            case ONE_WING -> "1 small wing";
            case TWIN_WINGS -> "2, growing until " + UkakuStage.FULL_SIZE_MASTERY;
            case FULL_WINGS -> "2, full size";
            case KAKUJA_I -> "6 armored tentacles";
            case KAKUJA_II -> "3 needles, 3 fire tendrils";
        };
    }

    /** The form a stage is drawn and described in: the one out now for the stage he is at, else the stage's own. */
    static WingForm stageForm(Model model, UkakuStage stage) {
        if (stage == UkakuStage.forMastery(model.mastery()) && !stage.comingSoon()) {
            WingForm out = model.form().shown(model.mastery());
            if (out.kakuja() == (stage == UkakuStage.KAKUJA_I || stage == UkakuStage.KAKUJA_II)) {
                return out;
            }
        }
        return switch (stage) {
            case ONE_WING -> WingForm.RABBIT;
            case TWIN_WINGS, FULL_WINGS -> WingForm.TWIN;
            case KAKUJA_I -> WingForm.KAKUJA;
            case KAKUJA_II -> WingForm.FINAL_KAKUJA;
        };
    }

    /** The mastery a stage is shown at: his own for the stage he is at, else the stage's. */
    static int stageMastery(Model model, UkakuStage stage) {
        return stage == UkakuStage.forMastery(model.mastery()) ? model.mastery() : stage.requiredMastery();
    }

    /**
     * The stage's wings in a framed tile, as a quest's icon is framed (its edge beating while it is the stage he is at): a few stars in the
     * dark, the wings as big as the stage has them (one before the twin wings), the kakuja's tentacles ({@link PathSilhouette#ukakuKakuja});
     * veiled with a padlock when ahead.
     */
    private static void preview(UiCanvas c, Model model, UkakuStage stage, int x, int y, int width, int height, double time) {
        QuestLog.State state = state(model, stage);
        WingForm form = stageForm(model, stage);
        int shownAt = stageMastery(model, stage);
        int edge = state == QuestLog.State.OPEN ? QuestStyle.mix(QuestStyle.RED_DARK, QuestStyle.RED, 0.5F + 0.5F * (float)Math.sin(time * 2.2))
            : state == QuestLog.State.DONE ? QuestStyle.GREEN_DARK : QuestStyle.TILE_EDGE;
        c.batch(() -> {
            QuestStyle.box(c, x, y, width, height, QuestStyle.TILE, edge);
            c.gradient(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF12060A, 0xFF070305);
            for (int i = 0; i < 16; i++) {
                int sx = x + 10 + (int)(hash(i * 3 + 1) * (width - 20));
                int sy = y + 8 + (int)(hash(i * 3 + 2) * (height - 40));
                int glow = 0x50 + (int)(0x60 * (0.5 + 0.5 * Math.sin(time * 1.7 + i)));
                c.fill(sx, sy, sx + 1, sy + 1, glow << 24 | 0xB8C4FF);
            }
        });
        float size = form.kakuja() ? 1.0F
            : (UkakuRules.wingLength(shownAt, form) - UkakuRules.ONE_WING_LENGTH) / (UkakuRules.FULL_WINGS_LENGTH - UkakuRules.ONE_WING_LENGTH);
        float scale = 0.5F + 0.5F * Math.max(0.0F, Math.min(1.0F, size));
        int inner = height - 26;
        int boxWidth = Math.round((width - 12) * scale);
        int boxHeight = Math.round(inner * scale);
        int boxX = x + (width - boxWidth) / 2;
        int boxY = y + 4 + (inner - boxHeight);
        PathChoiceLayout.Box box = new PathChoiceLayout.Box(boxX, boxY, boxWidth, boxHeight);
        boolean oneWing = form == WingForm.RABBIT || form == WingForm.ONE_WING;
        List<PathSilhouette.Mark> marks = new ArrayList<>();
        float intensity = state == QuestLog.State.LOCKED ? 0.0F : 1.0F;
        for (PathSilhouette.Mark mark : form.kakuja() ? PathSilhouette.ukakuKakuja(box, time, intensity, form.finalKakuja(), 0x0C080E)
            : PathSilhouette.ukaku(box, time, intensity, false, 0x0C080E)) {
            // Seen from the front, his left wing is on the right: drop the other one when only one is out.
            if (!oneWing || centreX(mark) >= box.centerX() - 3) {
                marks.add(mark);
            }
        }
        c.batch(() -> {
            for (PathSilhouette.Mark mark : marks) {
                draw(c, mark);
            }
            if (state == QuestLog.State.LOCKED) {
                c.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xA0080306);
                QuestStyle.lock(c, x + width / 2 - 13, y + (height - 26) / 2 - 12, 26, QuestStyle.mix(QuestStyle.GOLD, QuestStyle.TILE, 0.2F),
                    0xFF0A0405);
            }
        });
        String caption = form.finalKakuja() ? "final kakuja: needles and fire" : form.kakuja() ? "first kakuja: six armored tentacles"
            : !form.rightWing() ? "one small wing" : shownAt < UkakuStage.FULL_SIZE_MASTERY
            ? "two wings, " + Math.round(UkakuStage.growth(shownAt) * 100.0F) + "% grown" : "full :re wings";
        c.centered(caption, x + width / 2, y + height - 15, state == QuestLog.State.LOCKED ? QuestFrame.FAINT : 0xFFB0767A, false);
    }

    /**
     * Every step of the Ukaku's road ({@link UkakuUnlock}) at its mastery: reached ticked, the next beating, the rest locked; the picked
     * stage's steps ringed. Under the mouse a step names what it brings; a click on it picks its stage.
     */
    private static QuestFrame.Tip unlockRoad(UiCanvas c, Model model, UkakuStage picked, Map<String, Rect> hits, double mouseX, double mouseY, int x,
                                             int y, int width, double time) {
        int mastery = model.mastery();
        y = QuestFrame.section(c, "YOUR ROAD", "a step picks its stage", x, y, width);
        UkakuUnlock next = UkakuUnlock.next(mastery);
        List<QuestFrame.Node> nodes = new ArrayList<>();
        for (UkakuUnlock unlock : UkakuUnlock.values()) {
            QuestLog.State state = unlock.reached(mastery) ? QuestLog.State.DONE : unlock == next ? QuestLog.State.OPEN : QuestLog.State.LOCKED;
            nodes.add(new QuestFrame.Node("unlock:" + unlock.name(), String.valueOf(unlock.mastery()), unlock.text() + "  ·  "
                + (unlock.reached(mastery) ? "reached" : unlock.comingSoon() ? "coming soon" : "at mastery " + unlock.mastery()), state,
                stageOf(unlock) == picked));
        }
        return QuestFrame.road(c, nodes, UkakuUnlock.reachedCount(mastery) - 1, hits, mouseX, mouseY, x, y, width, 64, time);
    }

    // ------------------------------------------------------------------ the mutations' tab

    private static QuestFrame.Tip mutationsTab(UiCanvas c, Model model, Map<String, Rect> hits, double mouseX, double mouseY, double time) {
        UkakuMutation picked = pickedMutation(model);
        int y = QuestFrame.CONTENT_Y;
        int index = 0;
        for (UkakuMutation mutation : UkakuMutation.values()) {
            QuestFrame.card(c, mutationCard(model, mutation), QuestFrame.LIST_X, y, QuestFrame.LIST_WIDTH, mutation == picked, hits, mouseX, mouseY,
                time, index++);
            y += QuestFrame.CARD_HEIGHT + QuestFrame.CARD_GAP;
        }
        pointsPanel(c, model, QuestFrame.LIST_X, y + 6, QuestFrame.LIST_WIDTH);
        mutationDetail(c, model, picked, hits, mouseX, mouseY, QuestFrame.DETAIL_X, QuestFrame.CONTENT_Y, DETAIL_WIDTH, time);
        return milestoneRoad(c, model, hits, mouseX, mouseY, QuestFrame.DETAIL_X, ROAD_Y, DETAIL_WIDTH, time);
    }

    /** How a mutation stands: had, ready to unlock (a point and its quest), waiting for its quest, waiting for a point. */
    enum Standing {
        OWNED, READY, QUEST, POINT
    }

    static Standing standing(Model model, UkakuMutation mutation) {
        if (mutation.in(model.mutations())) {
            return Standing.OWNED;
        }
        if (!model.quests().passes(Gates.ukaku(mutation))) {
            return Standing.QUEST;
        }
        return UkakuMutation.canUnlock(model.mastery(), model.mutations(), mutation, model.quests().points()) ? Standing.READY : Standing.POINT;
    }

    /** The mutation shown on the right: the one picked, else the first ready to unlock, else the first not had, else the first. */
    static UkakuMutation pickedMutation(Model model) {
        String picked = model.picked();
        if (picked != null && picked.startsWith("pick:")) {
            try {
                UkakuMutation mutation = UkakuMutation.byOrdinal(Integer.parseInt(picked.substring(5)));
                if (mutation != null) {
                    return mutation;
                }
            } catch (NumberFormatException unknown) {
                // fall through to the default
            }
        }
        for (Standing wanted : new Standing[] {Standing.READY, Standing.POINT, Standing.QUEST}) {
            for (UkakuMutation mutation : UkakuMutation.values()) {
                if (standing(model, mutation) == wanted) {
                    return mutation;
                }
            }
        }
        return UkakuMutation.values()[0];
    }

    /** A mutation as a quest card: its glyph, its name, a chip (UNLOCKED, READY, NEEDS A POINT, NEEDS ITS QUEST); a ready one glows gold. */
    static QuestFrame.Card mutationCard(Model model, UkakuMutation mutation) {
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
        return new QuestFrame.Card("pick:" + mutation.ordinal(), mutation.displayName(), locked, chip, ink, dark, "", -1.0F, 0,
            standing == Standing.READY, (cc, x, y, size, back, t) -> {
                mutationGlyph(cc, mutation, locked ? QuestStyle.mix(ink, back, 0.45F) : ink, x, y, size, back, t);
                if (locked) {
                    QuestStyle.lock(cc, x + size / 2, y + size / 2, size / 2, QuestStyle.mix(QuestStyle.GOLD, back, 0.2F), back);
                }
            });
    }

    /** The points: how many to spend (large), where they came from, how many are spent, when the next comes. */
    private static void pointsPanel(UiCanvas c, Model model, int x, int y, int width) {
        y = QuestFrame.section(c, "POINTS", x, y, width);
        int points = points(model);
        String count = String.valueOf(points);
        c.push();
        c.translate(x, y);
        c.scale(2.0F);
        c.text(count, 0, 0, points > 0 ? QuestStyle.GOLD : QuestFrame.MUTED, true);
        c.pop();
        c.text(points == 1 ? "point to spend" : "points to spend", x + c.width(count) * 2 + 8, y + 5, points > 0 ? QuestStyle.GOLD : QuestFrame.MUTED,
            false);
        y += 22;
        int next = UkakuMutation.nextMilestone(model.mastery());
        QuestFrame.row(c, "Mastery", UkakuMutation.earned(model.mastery()) + " earned at its milestones", x, y, 66, width, QuestFrame.VALUE);
        QuestFrame.row(c, "Quests", model.quests().points() + " earned", x, y + 12, 66, width, QuestFrame.VALUE);
        QuestFrame.row(c, "Spent", String.valueOf(UkakuMutation.unlocked(model.mutations())), x, y + 24, 66, width, QuestFrame.VALUE);
        QuestFrame.row(c, "Next", next > 0 ? "at mastery " + next : "every milestone reached", x, y + 36, 66, width, next > 0 ? QuestFrame.VALUE
            : QuestJournalView.DONE);
    }

    /**
     * The mutation picked, as the journal shows a quest: its name large, a chip; where it stands (green, gold or muted), what it does as its
     * story; what it costs as reward badges; then the UNLOCK button ("mutation:N", the only click that spends a point) while it can be
     * unlocked, a stamp once it is had, or a grey button saying what it waits for (no click).
     */
    private static void mutationDetail(UiCanvas c, Model model, UkakuMutation mutation, Map<String, Rect> hits, double mouseX, double mouseY, int x,
                                       int y, int width, double time) {
        Standing standing = standing(model, mutation);
        c.push();
        c.translate(x, y);
        c.scale(1.25F);
        c.text(c.fit(mutation.displayName(), Math.round((width - 90) / 1.25F)), 0, 0, QuestStyle.WHITE, true);
        c.pop();
        int chipWidth = c.width("Mutation") + 12;
        QuestStyle.chip(c, x + width - chipWidth, y, "Mutation", 0xFF1E0A0E, QuestStyle.RED_DARK, QuestFrame.MUTED);
        int rowY = y + 16;
        Gate gate = Gates.ukaku(mutation);
        int next = UkakuMutation.nextMilestone(model.mastery());
        String line = switch (standing) {
            case OWNED -> "Unlocked: yours for good";
            case READY -> "A point to spend: unlock it below";
            case QUEST -> "Opens with its quest: " + QuestBook.openersText(gate);
            case POINT -> next > 0 ? "Needs a point: the next comes at mastery " + next : "Needs a point: a quest can still pay one";
        };
        int ink = standing == Standing.OWNED ? QuestJournalView.DONE : standing == Standing.READY ? QuestJournalView.OPEN : QuestFrame.MUTED;
        c.text(c.fit(line, width), x, rowY, ink, false);
        rowY += 13;
        rowY = QuestFrame.story(c, mutation.description(), x, rowY, width) + 10;
        rowY = QuestFrame.section(c, "COST", x, rowY, width);
        int bx = x + QuestFrame.badge(c, 'p', "1 mutation point", true, x, rowY) + 5;
        if (gate != null) {
            String quest = QuestBook.openersText(gate);
            QuestFrame.badge(c, 'k', c.fit("Its quest: " + (quest.isEmpty() ? "given in the world" : quest), width - (bx - x) - 26), true, bx, rowY);
        }
        rowY += 24;
        rowY = QuestFrame.section(c, "WITH IT", standing == Standing.OWNED ? "yours now" : "now, then with it", x, rowY, width);
        int after = standing == Standing.OWNED ? QuestJournalView.DONE : standing == Standing.READY ? QuestStyle.GOLD : QuestFrame.VALUE;
        for (String[] effect : effects(model, mutation)) {
            c.text(effect[0], x, rowY, QuestFrame.MUTED, false);
            c.text(c.fit(effect[1], 86), x + 136, rowY, QuestFrame.FAINT, false);
            int ax = x + 226;
            int ay = rowY + 4;
            c.batch(() -> {
                QuestStyle.line(c, ax, ay, ax + 12, ay, 1.6F, after);
                QuestStyle.line(c, ax + 8, ay - 3, ax + 12, ay, 1.6F, after);
                QuestStyle.line(c, ax + 8, ay + 3, ax + 12, ay, 1.6F, after);
            });
            c.text(c.fit(effect[2], width - 246), x + 246, rowY, after, false);
            rowY += 13;
        }
        rowY += 8;
        if (standing == Standing.OWNED) {
            QuestFrame.stamp(c, x, rowY, "UNLOCKED", QuestStyle.GREEN, QuestStyle.GREEN_DARK);
            return;
        }
        String label = switch (standing) {
            case READY -> "UNLOCK  ·  SPEND 1 POINT";
            case QUEST -> "NEEDS ITS QUEST";
            default -> "NEEDS A POINT";
        };
        QuestFrame.button(c, "mutation:" + mutation.ordinal(), label, x, rowY, 200, 22, standing == Standing.READY, true, hits, mouseX, mouseY, time);
        int have = points(model);
        c.text(have + (have == 1 ? " point" : " points") + " to spend", x + 212, rowY + 8, have > 0 ? QuestStyle.GOLD : QuestFrame.FAINT, false);
    }

    /**
     * What a mutation changes, in the rules' own numbers: {what, without it, with it}. Agility Boost's speed and jump (kagune out), Fire
     * Dash's hop, Fire Dance's cyclone, Bursting Shards' blasts, the death You Thought You'd Get Rid of Me turns aside.
     */
    static List<String[]> effects(Model model, UkakuMutation mutation) {
        int mastery = model.mastery();
        List<String[]> out = new ArrayList<>();
        switch (mutation) {
            case AGILITY_BOOST -> {
                int speed = UkakuRules.speedAmplifier(mastery) + 1;
                int jump = UkakuRules.jumpAmplifier(mastery) + 1;
                out.add(new String[] {"Speed, kagune out", "Speed " + roman(speed), "Speed " + roman(speed + UkakuRules.AGILITY_SPEED)});
                out.add(new String[] {"Jump, kagune out", "Jump " + roman(jump), "Jump " + roman(jump + UkakuRules.AGILITY_JUMP)});
            }
            case FIRE_DASH -> {
                out.add(new String[] {"Rabbit Hop, pushed back", "0.95", String.format("%.2f", UkakuRules.FIRE_DASH_BACK)});
                out.add(new String[] {"Rabbit Hop, lift", "0.50", String.format("%.2f", UkakuRules.FIRE_DASH_LIFT)});
                out.add(new String[] {"W + right click", "the same hop", "a dash forward through it all"});
            }
            case FIRE_DANCE -> {
                boolean vortex = mastery >= UkakuRules.VORTEX_MASTERY;
                float damage = UkakuRules.vortexDamage(Math.max(mastery, UkakuRules.VORTEX_MASTERY));
                out.add(new String[] {"Vortex damage", String.format("%.1f", damage), String.format("%.1f", damage * UkakuRules.FIRE_DANCE_DAMAGE)});
                out.add(new String[] {"Its reach round you", "x1", String.format("x%.1f", UkakuRules.FIRE_DANCE_RADIUS)});
                out.add(new String[] {"The cyclone", "as it is", String.format("x%.1f the size", UkakuRules.FIRE_DANCE_SIZE)});
                if (!vortex) {
                    out.add(new String[] {"The Wing Vortex", "at mastery " + UkakuRules.VORTEX_MASTERY, "at mastery " + UkakuRules.VORTEX_MASTERY});
                }
            }
            case BURSTING_SHARDS -> {
                out.add(new String[] {"Fire shards", "hit", String.format("explode, %.1f blocks", UkakuRules.SHARD_EXPLOSION_RADIUS)});
                out.add(new String[] {"Their blast", "none", Math.round(UkakuRules.SHARD_EXPLOSION_SHARE * 100.0F) + "% of a shard's damage"});
                out.add(new String[] {"Spikes and needles", "burst", "explode as they burst"});
            }
            case NOT_THAT_EASILY -> {
                out.add(new String[] {"A killing blow", "you die", "you're blown apart"});
                out.add(new String[] {"Then", "you respawn", "all forget you; you knit back"});
                out.add(new String[] {"How often", "never", "once every 5 minutes"});
            }
        }
        return out;
    }

    /** The milestones that pay a point, on a road: reached ticked, the next beating, the rest locked. */
    private static QuestFrame.Tip milestoneRoad(UiCanvas c, Model model, Map<String, Rect> hits, double mouseX, double mouseY, int x, int y,
                                                int width, double time) {
        int mastery = model.mastery();
        y = QuestFrame.section(c, "POINTS ON YOUR ROAD", "a point at each", x, y, width);
        List<QuestFrame.Node> nodes = new ArrayList<>();
        int next = UkakuMutation.nextMilestone(mastery);
        int reached = -1;
        for (int i = 0; i < UkakuMutation.MILESTONES.length; i++) {
            int milestone = UkakuMutation.MILESTONES[i];
            boolean got = mastery >= milestone;
            reached = got ? i : reached;
            nodes.add(new QuestFrame.Node("milestone:" + milestone, String.valueOf(milestone), "Mastery " + milestone + ": a mutation point  ·  "
                + (got ? "earned" : "ahead"), got ? QuestLog.State.DONE : milestone == next ? QuestLog.State.OPEN : QuestLog.State.LOCKED, false));
        }
        return QuestFrame.road(c, nodes, reached, hits, mouseX, mouseY, x, y, width, 112, time);
    }

    // ------------------------------------------------------------------ glyphs

    /**
     * A stage's glyph in its tile: one wing, two small, two large, the six tentacles, needles and flames; in the stage's colour, dim with a
     * padlock when ahead.
     */
    static void stageGlyph(UiCanvas c, UkakuStage stage, QuestLog.State state, int x, int y, int size, int back, double time) {
        int ink = QuestStyle.colour(state);
        int dim = state == QuestLog.State.LOCKED ? QuestStyle.mix(ink, back, 0.5F) : ink;
        float s = size;
        float sway = state == QuestLog.State.OPEN ? (float)Math.sin(time * 2.4) * 0.04F : 0.0F;
        switch (stage) {
            case ONE_WING -> {
                // One wing out (the left: on the right, seen from the front), the other still only a faint ghost of the pair to come.
                wing(c, x + s * 0.45F, y + s * 0.82F, s * 0.5F, -1, 0.0F, QuestStyle.mix(dim, back, 0.78F));
                wing(c, x + s * 0.55F, y + s * 0.82F, s * 0.5F, 1, sway, dim);
            }
            case TWIN_WINGS -> {
                wing(c, x + s * 0.45F, y + s * 0.82F, s * 0.5F, -1, -sway, dim);
                wing(c, x + s * 0.55F, y + s * 0.82F, s * 0.5F, 1, sway, dim);
            }
            case FULL_WINGS -> {
                wing(c, x + s * 0.46F, y + s * 0.88F, s * 0.7F, -1, -sway, dim);
                wing(c, x + s * 0.54F, y + s * 0.88F, s * 0.7F, 1, sway, dim);
            }
            case KAKUJA_I -> {
                // Six thin tentacles from one root, fanned over the top, each tip curling outward.
                for (int i = 0; i < 6; i++) {
                    int side = i < 3 ? -1 : 1;
                    float a = (float)Math.toRadians(-144.0 + i * 21.6) + sway * side;
                    tentacle(c, x + s * 0.5F, y + s * 0.88F, a, s * 0.58F, side * 0.6F, s * 0.1F, dim);
                }
            }
            default -> {
                for (int i = 0; i < 3; i++) {
                    float a = (float)Math.toRadians(-125.0 + i * 35.0);
                    QuestStyle.line(c, x + s * 0.5F, y + s * 0.88F, x + s * 0.5F + (float)Math.cos(a) * s * 0.62F,
                        y + s * 0.88F + (float)Math.sin(a) * s * 0.62F, Math.max(1.2F, s * 0.08F), dim);
                }
                for (int side = -1; side <= 1; side += 2) {
                    tentacle(c, x + s * 0.5F, y + s * 0.88F, (float)Math.toRadians(side < 0 ? -170.0 : -10.0) + sway, s * 0.5F, side * 0.9F, s * 0.14F,
                        QuestStyle.mix(dim, QuestStyle.GOLD, state == QuestLog.State.LOCKED ? 0.0F : 0.45F));
                }
                tentacle(c, x + s * 0.5F, y + s * 0.88F, (float)Math.toRadians(-90.0) + sway, s * 0.34F, 1.2F, s * 0.12F,
                    QuestStyle.mix(dim, QuestStyle.GOLD, state == QuestLog.State.LOCKED ? 0.0F : 0.45F));
            }
        }
        if (state == QuestLog.State.LOCKED) {
            QuestStyle.lock(c, x + size / 2, y + size / 2, size / 2, QuestStyle.mix(QuestStyle.GOLD, back, 0.2F), back);
        }
    }

    /**
     * A wing as rays fanned up and out from its root ({@code side} 1 to the right, -1 to the left), {@code k} long: thick at the root,
     * thin at the tip, the longest sweeping up and out (a small glyph reads as spikes, not as a filled shape).
     */
    static void wing(UiCanvas c, float rootX, float rootY, float k, int side, float sway, int colour) {
        float[][] rays = {{0.3F, 0.78F}, {0.68F, 1.0F}, {1.04F, 0.9F}, {1.38F, 0.66F}};
        float thick = Math.max(1.4F, k * 0.16F);
        for (float[] ray : rays) {
            double a = -Math.PI / 2.0 + side * (ray[0] + sway);
            float midX = rootX + (float)Math.cos(a) * k * ray[1] * 0.5F;
            float midY = rootY + (float)Math.sin(a) * k * ray[1] * 0.5F;
            float tipX = rootX + (float)Math.cos(a) * k * ray[1];
            float tipY = rootY + (float)Math.sin(a) * k * ray[1];
            QuestStyle.line(c, rootX, rootY, midX, midY, thick, colour);
            QuestStyle.line(c, midX, midY, tipX, tipY, Math.max(1.0F, thick * 0.6F), colour);
        }
        QuestStyle.circle(c, Math.round(rootX), Math.round(rootY), Math.max(1, Math.round(thick * 0.6F)), colour);
    }

    /** A tentacle: a curving line thinning from {@code root} wide to its tip, {@code length} long, bending by {@code bend}. */
    static void tentacle(UiCanvas c, float x, float y, float angle, float length, float bend, float root, int colour) {
        float px = x;
        float py = y;
        for (int i = 1; i <= 8; i++) {
            float t = i / 8.0F;
            float a = angle + bend * t * t;
            float nx = px + (float)Math.cos(a) * length / 8.0F;
            float ny = py + (float)Math.sin(a) * length / 8.0F;
            QuestStyle.line(c, px, py, nx, ny, Math.max(1.0F, root * (1.0F - 0.75F * t)), colour);
            px = nx;
            py = ny;
        }
    }

    /**
     * A mutation's glyph: Agility Boost two chevrons rising, Fire Dash an arrow with its streaks, Fire Dance a cyclone, Bursting Shards a
     * burst, You Thought You'd Get Rid of Me a heart cracked across.
     */
    static void mutationGlyph(UiCanvas c, UkakuMutation mutation, int ink, int x, int y, int size, int back, double time) {
        float s = size;
        float w = Math.max(1.5F, s * 0.13F);
        switch (mutation) {
            case AGILITY_BOOST -> {
                for (int k = 0; k < 2; k++) {
                    float cy = y + s * (0.42F + k * 0.32F);
                    QuestStyle.line(c, x + s * 0.16F, cy + s * 0.14F, x + s * 0.5F, cy - s * 0.16F, w, ink);
                    QuestStyle.line(c, x + s * 0.84F, cy + s * 0.14F, x + s * 0.5F, cy - s * 0.16F, w, ink);
                }
            }
            case FIRE_DASH -> {
                QuestStyle.line(c, x + s * 0.5F, y + s * 0.2F, x + s * 0.86F, y + s * 0.5F, w, ink);
                QuestStyle.line(c, x + s * 0.86F, y + s * 0.5F, x + s * 0.5F, y + s * 0.8F, w, ink);
                for (int k = 0; k < 3; k++) {
                    float sy = y + s * (0.3F + k * 0.2F);
                    float from = x + s * (k == 1 ? 0.06F : 0.18F);
                    QuestStyle.line(c, from, sy, x + s * 0.56F, sy, Math.max(1.0F, w * 0.7F), ink);
                }
            }
            case FIRE_DANCE -> {
                float px = x + s * 0.5F;
                float py = y + s * 0.5F;
                double spin = time * 1.5;
                for (int i = 1; i <= 24; i++) {
                    float t = i / 24.0F;
                    double a = spin + t * Math.PI * 3.4;
                    float r = s * 0.44F * t;
                    float nx = x + s * 0.5F + (float)Math.cos(a) * r;
                    float ny = y + s * 0.5F + (float)Math.sin(a) * r * 0.8F;
                    QuestStyle.line(c, px, py, nx, ny, Math.max(1.2F, w * (0.5F + 0.6F * t)), ink);
                    px = nx;
                    py = ny;
                }
            }
            case BURSTING_SHARDS -> {
                float cx = x + s * 0.5F;
                float cy = y + s * 0.5F;
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2.0 * i / 8 + Math.PI / 8.0;
                    float r0 = s * 0.18F;
                    float r1 = s * (i % 2 == 0 ? 0.48F : 0.34F);
                    QuestStyle.line(c, cx + (float)Math.cos(a) * r0, cy + (float)Math.sin(a) * r0, cx + (float)Math.cos(a) * r1,
                        cy + (float)Math.sin(a) * r1, Math.max(1.2F, w * 0.85F), ink);
                }
                QuestStyle.circle(c, Math.round(cx), Math.round(cy), Math.max(1, Math.round(s * 0.12F)), ink);
            }
            default -> {
                int r = Math.max(2, Math.round(s * 0.22F));
                int cy = Math.round(y + s * 0.4F);
                QuestStyle.circle(c, Math.round(x + s * 0.5F) - r + 1, cy, r, ink);
                QuestStyle.circle(c, Math.round(x + s * 0.5F) + r - 1, cy, r, ink);
                c.polygon(new float[] {x + s * 0.08F, x + s * 0.92F, x + s * 0.5F}, new float[] {cy + 1, cy + 1, y + s * 0.92F}, ink);
                QuestStyle.line(c, x + s * 0.48F, y + s * 0.22F, x + s * 0.4F, y + s * 0.46F, Math.max(1.0F, w * 0.6F), back);
                QuestStyle.line(c, x + s * 0.4F, y + s * 0.46F, x + s * 0.58F, y + s * 0.6F, Math.max(1.0F, w * 0.6F), back);
                QuestStyle.line(c, x + s * 0.58F, y + s * 0.6F, x + s * 0.5F, y + s * 0.86F, Math.max(1.0F, w * 0.6F), back);
            }
        }
    }

    // ------------------------------------------------------------------ text

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

    static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI"; // 4.1.56: Agility Boost's speed reaches VI and VII
            case 7 -> "VII";
            case 8 -> "VIII";
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
            case UKAKU_WINGS -> "Wing slams + wing guard"; // 4.1.2
            case UKAKU_LEFT_WING -> "Left-wing slams + wing guard";
            case UKAKU_FIRE -> "Fire shots + enders"; // 4.1.56: every form named
            case UKAKU_CRYSTAL -> "Crystal spikes + enders";
            case UKAKU_KAKUJA -> "Needle volleys + enders";
            case UKAKU_FINAL_KAKUJA -> "Needles and fire, side by side";
            default -> style.name().toLowerCase();
        };
    }

    // ------------------------------------------------------------------ drawing (as the Rinkaku menu draws)

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
