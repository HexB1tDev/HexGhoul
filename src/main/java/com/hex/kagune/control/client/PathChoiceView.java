package com.hex.kagune.control.client;

import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.client.PathChoiceLayout.Box;
import com.hex.kagune.control.client.ui.UiCanvas;
import java.util.List;

/**
 * 4.1.64: the path choice screen's drawing, pure ({@link UiCanvas}), so it can be rendered outside the game and checked; the screen keeps
 * the picking and the hold. As the quest menu (the owner: "new Menus: Every single menu in the game is to mimic and look like the quest
 * menu, and be as interactable as it is, Dialog, mastery menus ALL OF THEM"): its board over the world, the title in its type, a rule under
 * the header, every path a quest card (its own colour at its edge, lifted under the mouse, lit and beating once picked), the confirm held
 * as the quest menu's button fills, the stages along each card a little road with its tooltip.
 */
public final class PathChoiceView {
    static final String TITLE = "YOUR RC CELLS ARE WAKING UP";
    static final String SUBTITLE = "What tears out of your back decides how you fight.";
    static final String SUBTITLE_SHORT = "Your kagune decides how you fight.";
    static final String PERMANENT = "This choice is permanent. Choose with your gut.";
    static final String HINT = "Arrows: choose   Hold Enter: awaken   Esc: decide later";
    /** The board round everything, this far in from the screen's edge. */
    public static final int INSET = 4;
    /** The cards' own colour (the silhouettes are blended against it). */
    static final int CARD = 0x1C0A0E;
    static final int BONE = 0xE8DCCB;

    /** What the screen shows this frame: the path picked, the mouse, the time, and how far the hold has got (below 0: none). */
    public record State(KagunePath selected, double mouseX, double mouseY, double time, float hold) {
    }

    private PathChoiceView() {
    }

    /** The board as big as the screen (less {@link #INSET}). */
    public static Box board(PathChoiceLayout layout) {
        return new Box(INSET, INSET, layout.width - 2 * INSET, layout.height - 2 * INSET);
    }

    /** Where the rule under the header goes: just under its lines, never on a card. */
    public static int ruleY(PathChoiceLayout layout) {
        int under = layout.subtitleY + layout.subtitleLines * PathChoiceLayout.LINE + 2;
        int top = Integer.MAX_VALUE;
        for (Box card : layout.cards) {
            top = Math.min(top, card.y());
        }
        return Math.min(under, top - 4);
    }

    public static void draw(UiCanvas c, PathChoiceLayout layout, List<PathCards.Card> cards, State state) {
        int width = layout.width;
        Box board = board(layout);
        int ruleY = ruleY(layout);
        // The honeycomb only where the header has room for it: right of the title, never under its words.
        int titleRight = width / 2 + Math.round(c.width(TITLE) * 1.5F / 2.0F) + 10;
        int hx = Math.max(titleRight, board.right() - 190);
        int hw = Math.max(0, board.right() - 12 - hx);
        QuestStyle.backdrop(c, board.x(), board.y(), board.width(), board.height(), state.time(), hx, board.y() + 4, hw < 40 ? 0 : hw,
            Math.max(10, ruleY - board.y() - 6));
        c.push();
        c.translate(width / 2.0F, layout.titleY);
        c.scale(1.5F);
        c.centered(TITLE, 0, 0, QuestJournalView.TITLE, true);
        c.pop();
        String subtitle = c.width(SUBTITLE) <= width - 2 * PathChoiceLayout.MARGIN ? SUBTITLE : SUBTITLE_SHORT;
        c.centered(subtitle, width / 2, layout.subtitleY, QuestJournalView.SUBTITLE);
        if (layout.subtitleLines > 1) {
            c.centered(c.fit(PERMANENT, width - 2 * PathChoiceLayout.MARGIN), width / 2, layout.subtitleY + PathChoiceLayout.LINE, QuestFrame.MUTED);
        }
        c.batch(() -> {
            for (int i = 0; i < 40; i++) {
                float t = i / 39.0F;
                int alpha = Math.round(190 * (1.0F - Math.abs(t - 0.5F) * 2.0F));
                int x0 = board.x() + 12 + Math.round((board.width() - 24) * i / 40.0F);
                c.fill(x0, ruleY, x0 + (board.width() - 24) / 40 + 1, ruleY + 1, alpha << 24 | 0xE8323E);
            }
        });
        String[] tip = {null};
        int[] tipAt = {0, 0};
        for (int i = 0; i < cards.size() && i < layout.cards.size(); i++) {
            card(c, layout, cards.get(i), layout.cards.get(i), state, tip, tipAt);
        }
        buttons(c, layout, state);
        if (layout.hintY >= 0) {
            c.centered(c.fit(HINT, width - 2 * PathChoiceLayout.MARGIN), width / 2, layout.hintY, QuestFrame.FAINT);
        }
        if (tip[0] != null) {
            int tipWidth = c.width(tip[0]) + 8;
            int x = Math.max(board.x() + 2, Math.min(board.right() - tipWidth - 2, tipAt[0] - tipWidth / 2));
            int y = Math.max(board.y() + 2, tipAt[1] - 16);
            c.batch(() -> QuestStyle.box(c, x, y, tipWidth, 13, 0xFF1A0A0E, QuestStyle.RED));
            c.text(tip[0], x + 4, y + 3, QuestFrame.VALUE, false);
        }
    }

    /** One path as a quest card: its colour at its edge, lifted under the mouse; picked, lit, raised and its bar beating. */
    private static void card(UiCanvas c, PathChoiceLayout layout, PathCards.Card card, Box slot, State state, String[] tip, int[] tipAt) {
        boolean hovered = slot.contains(state.mouseX(), state.mouseY());
        boolean chosen = state.selected() == card.path();
        int lift = chosen ? 2 : hovered ? 1 : 0;
        Box box = new Box(slot.x(), slot.y() - lift, slot.width(), slot.height());
        int accent = 0xFF000000 | card.accent();
        int edge = chosen ? accent : hovered ? QuestStyle.mix(QuestStyle.TILE_EDGE, accent, 0.7F) : QuestStyle.mix(QuestStyle.TILE_EDGE, accent, 0.3F);
        c.batch(() -> {
            if (chosen) {
                c.fill(box.x() - 3, box.y() - 3, box.right() + 3, box.bottom() + 3, 0x40000000 | card.accent());
            }
            QuestStyle.box(c, box.x(), box.y(), box.width(), box.height(), 0xFF000000 | CARD, edge);
            if (chosen) {
                float pulse = 0.55F + 0.45F * (float)Math.sin(state.time() * 4.0);
                c.fill(box.x() + 4, box.bottom() - 3, box.right() - 4, box.bottom() - 2, QuestStyle.mix(0xFF000000 | CARD, accent, pulse));
                c.fill(box.x() + 1, box.y() + 3, box.x() + 3, box.bottom() - 3, accent);
            }
        });
        PathChoiceLayout.Card inner = layout.card(box);
        float intensity = chosen ? 1.0F : hovered ? 0.7F : 0.35F;
        List<PathSilhouette.Mark> marks = card.silhouette().marks(inner.silhouette(), state.time(), intensity, chosen, CARD);
        c.batch(() -> {
            for (PathSilhouette.Mark mark : marks) {
                UkakuMenuView.draw(c, mark);
            }
        });
        c.text(c.fit(card.name(), inner.textWidth()), inner.textX(), inner.nameY(), chosen || hovered ? accent
            : 0xFF000000 | PathSilhouette.blend(CARD, card.accent(), 0.8F), true);
        c.text(c.fit(card.type(), inner.textWidth()), inner.textX(), inner.typeY(), QuestFrame.MUTED, false);
        int bullets = Math.min(inner.maxBullets(), card.bullets().size());
        for (int i = 0; i < bullets; i++) {
            c.text(c.fit("• " + card.bullets().get(i), inner.textWidth()), inner.textX(), inner.bulletsY() + i * PathChoiceLayout.LINE,
                0xFF000000 | PathSilhouette.blend(CARD, BONE, chosen ? 1.0F : 0.8F), false);
        }
        if (inner.tagsY() >= 0) {
            c.text(c.fit(card.tags(), inner.textWidth()), inner.textX(), inner.tagsY(), 0xFF000000 | PathSilhouette.blend(CARD, card.accent(), 0.75F),
                false);
        }
        if (inner.stripY() >= 0) {
            strip(c, card, inner, state, tip, tipAt);
        }
    }

    /** The path's stages along the foot of its card, a little road: a node each, its mastery under it; the one under the mouse named. */
    private static void strip(UiCanvas c, PathCards.Card card, PathChoiceLayout.Card inner, State state, String[] tip, int[] tipAt) {
        List<String> names = card.stageNames();
        List<Integer> thresholds = card.stageMastery();
        int left = inner.textX() + 6;
        int right = inner.textX() + inner.textWidth() - 6;
        int y = inner.stripY() + 2;
        int accent = 0xFF000000 | card.accent();
        c.batch(() -> c.fill(left, y, right, y + 1, 0xFF000000 | PathSilhouette.blend(CARD, card.accent(), 0.4F)));
        for (int i = 0; i < names.size(); i++) {
            int x = names.size() == 1 ? left : left + (right - left) * i / (names.size() - 1);
            boolean over = Math.abs(state.mouseX() - x) <= 4 && state.mouseY() >= y - 4 && state.mouseY() <= y + 12;
            int node = over ? accent : 0xFF000000 | PathSilhouette.blend(CARD, card.accent(), 0.75F);
            int r = over ? 2 : 1;
            c.batch(() -> {
                c.fill(x - r - 1, y - r - 1, x + r + 2, y + r + 2, 0xFF000000 | CARD);
                c.fill(x - r, y - r, x + r + 1, y + r + 1, node);
            });
            c.push();
            c.translate(x, y + 5);
            c.scale(0.7F);
            c.centered(Integer.toString(thresholds.get(i)), 0, 0, over ? QuestFrame.VALUE : QuestFrame.MUTED);
            c.pop();
            if (over) {
                tip[0] = names.get(i) + " (mastery " + thresholds.get(i) + ")";
                tipAt[0] = x;
                tipAt[1] = y;
            }
        }
    }

    /** Awaken (held: it fills in the path's colour) as the quest menu's button, and Decide later. */
    private static void buttons(UiCanvas c, PathChoiceLayout layout, State state) {
        Box confirm = layout.confirm;
        boolean enabled = state.selected().chosen();
        int accent = enabled ? 0xFF000000 | PathCards.of(state.selected()).accent() : QuestStyle.GREY_DARK;
        boolean hovered = confirm.contains(state.mouseX(), state.mouseY());
        boolean holding = state.hold() >= 0.0F;
        int lift = enabled && hovered && !holding ? -1 : 0;
        int edge = !enabled ? QuestStyle.GREY_DARK : hovered || holding ? accent : QuestStyle.mix(QuestStyle.TILE_EDGE, accent, 0.6F);
        int fill = !enabled ? 0xFF140A0C : hovered ? QuestJournalView.ROW_PICKED : QuestJournalView.ROW;
        c.batch(() -> {
            QuestStyle.box(c, confirm.x(), confirm.y() + lift, confirm.width(), confirm.height(), fill, edge);
            if (holding) {
                int filled = Math.round((confirm.width() - 2) * Math.min(1.0F, state.hold()));
                c.fill(confirm.x() + 1, confirm.y() + 1, confirm.x() + 1 + filled, confirm.bottom() - 1, QuestStyle.mix(QuestJournalView.ROW, accent, 0.6F));
            }
            if (enabled && hovered) {
                c.fill(confirm.x() + 4, confirm.bottom() + 1, confirm.right() - 4, confirm.bottom() + 2, accent);
            }
        });
        String label = !enabled ? "Choose a path" : holding ? "Awakening..." : "Hold to awaken: " + PathCards.of(state.selected()).awaken();
        c.centered(c.fit(label, confirm.width() - 8), confirm.centerX(), confirm.y() + 6 + lift, !enabled ? QuestStyle.GREY : hovered || holding
            ? QuestStyle.WHITE : QuestFrame.VALUE);
        Box later = layout.later;
        boolean overLater = later.contains(state.mouseX(), state.mouseY());
        c.centered("Decide later", later.centerX(), later.y() + 2, overLater ? QuestFrame.VALUE : QuestFrame.MUTED);
        if (overLater) {
            int half = c.width("Decide later") / 2;
            c.fill(later.centerX() - half, later.y() + 11, later.centerX() + half, later.y() + 12, QuestStyle.RED);
        }
    }
}
