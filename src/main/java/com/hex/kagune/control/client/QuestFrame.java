package com.hex.kagune.control.client;

import com.hex.kagune.control.client.ui.UiCanvas;
import com.hex.kagune.control.quest.Quest;
import com.hex.kagune.control.quest.QuestLog;
import java.util.List;
import java.util.Map;

/**
 * 4.1.56 (the owner: "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as interactable as it is,
 * Dialog, mastery menus ALL OF THEM"): the quest menu's pieces ({@link QuestJournalView}'s look), for every menu moved onto it: the plate
 * with its header rule and the divider between its columns, the header (title, subtitle, a bar to what comes next), tabs with badges,
 * cards as the quest list's, a section's heading, a story, a checklist row, a button, a road of nodes, a stamp and a tooltip. Pure: drawn
 * on a {@link UiCanvas}, in the box's own coordinates (its top left at 0, 0).
 */
final class QuestFrame {
    private QuestFrame() {
    }

    /** The box's columns, as the journal's: the list's left and width, the detail's left, the content's top, the header rule, the tabs. */
    static final int LIST_X = QuestJournalView.LIST_X;
    static final int LIST_WIDTH = QuestJournalView.LIST_WIDTH;
    static final int DETAIL_X = QuestJournalView.DETAIL_X;
    static final int CONTENT_Y = QuestJournalView.CONTENT_Y;
    static final int RULE_Y = 46;
    static final int TABS_Y = 54;
    /** A card's height and the gap under it (the quest list's). */
    static final int CARD_HEIGHT = 32;
    static final int CARD_GAP = 4;
    /** The header's bar to what comes next. */
    static final int BAR_WIDTH = 250;

    static final int VALUE = QuestJournalView.VALUE;
    static final int MUTED = QuestJournalView.MUTED;
    static final int FAINT = QuestJournalView.FAINT;
    static final int STORY = 0xFFDCAEB2;
    static final int GOLD_FILL = 0xFF2A1A08;
    static final int RED_FILL = 0xFF240A10;
    static final int TIP_FILL = 0xF0100406;

    /** A tab: its key ("tab:KEY" when clicked), its label, a count in a badge (0: none), the badge red (else grey). */
    record Tab(String key, String label, int badge, boolean urgent) {
    }

    /** A card's icon, drawn in its tile ({@code size} square at {@code x, y} on {@code back}). */
    @FunctionalInterface
    interface Icon {
        void draw(UiCanvas c, int x, int y, int size, int back, double time);
    }

    /**
     * A card as the quest list's: its key (its click), its title (dim when out of reach), a chip ({@code ink} on {@code dark}), a word at
     * its right, the bar along its foot ({@code share} of it in {@code bar}; below 0 no bar), a glow when it wants a look, its icon.
     */
    record Card(String key, String title, boolean dim, String chip, int ink, int dark, String right, float share, int bar, boolean glow, Icon icon) {
    }

    /** A road's node: its key, the word under it, its tooltip, how it stands, and whether it is lit (part of what is picked). */
    record Node(String key, String label, String tip, QuestLog.State state, boolean lit) {
    }

    /** A tooltip waiting to be drawn last (over everything): its words and the point it hangs over. */
    record Tip(String text, int x, int y) {
    }

    /**
     * The plate ({@link QuestStyle#backdrop}: cut corners, bright brackets, its honeycomb in {@code hx, hy, hw, hh}), the red rule under the
     * header fading out at both ends, and the faint divider between the list and the detail with its diamond.
     */
    static void plate(UiCanvas c, int width, int height, double time, int hx, int hy, int hw, int hh) {
        c.batch(() -> {
            QuestStyle.backdrop(c, 0, 0, width, height, time, hx, hy, hw, hh);
            for (int i = 0; i < 40; i++) {
                float t = i / 39.0F;
                int alpha = Math.round(190 * (1.0F - Math.abs(t - 0.5F) * 2.0F));
                int x0 = 12 + Math.round((width - 24) * i / 40.0F);
                c.fill(x0, RULE_Y, x0 + (width - 24) / 40 + 1, RULE_Y + 1, alpha << 24 | 0xE8323E);
            }
            int span = height - CONTENT_Y - 14;
            for (int i = 0; i < 24; i++) {
                float t = i / 23.0F;
                int alpha = Math.round(170 * (1.0F - Math.abs(t - 0.5F) * 1.6F));
                int y0 = CONTENT_Y + Math.round(span * i / 24.0F);
                c.fill(DETAIL_X - 11, y0, DETAIL_X - 10, y0 + span / 24 + 1, Math.max(0, alpha) << 24 | 0xA0202C);
            }
            QuestStyle.polygonDiamond(c, DETAIL_X - 10.5F, CONTENT_Y + span / 2.0F, 3.5F, QuestStyle.RED);
        });
    }

    /**
     * The header as the journal's: the title large, the subtitle in red under it; at the top right a bar ({@code share} full, green when
     * {@code full}) with what it leads to under it, right-aligned.
     */
    static void header(UiCanvas c, int width, String title, String subtitle, float share, boolean full, String goal, double time) {
        c.push();
        c.translate(16, 11);
        c.scale(1.4F);
        c.text(title, 0, 0, QuestJournalView.TITLE, true);
        c.pop();
        c.text(subtitle, 16, 29, QuestJournalView.SUBTITLE, false);
        int barX = width - 16 - BAR_WIDTH;
        int barY = 16;
        float s = Math.max(0.0F, Math.min(1.0F, share));
        c.batch(() -> {
            UkakuMenuView.capsule(c, barX, barY, BAR_WIDTH, 3, QuestJournalView.BAR_BACK);
            UkakuMenuView.capsule(c, barX, barY, Math.max(6, Math.round(BAR_WIDTH * s)), 3, full ? QuestJournalView.BAR_DONE : QuestJournalView.BAR);
            int glint = barX + (int)((time * 90.0) % Math.max(1, Math.round(BAR_WIDTH * s)));
            c.fill(glint, barY - 1, glint + 5, barY + 2, 0x60FFFFFF);
        });
        String fitted = c.fit(goal, BAR_WIDTH);
        c.text(fitted, barX + BAR_WIDTH - c.width(fitted), barY + 8, full ? QuestJournalView.DONE : MUTED, false);
    }

    /** A chip whose right edge is at {@code right}; the next chip's right edge returned (a gap left). */
    static int chipLeft(UiCanvas c, int right, int y, String text, int fill, int edge, int ink) {
        int x = right - (c.width(text) + 12);
        QuestStyle.chip(c, x, y, text, fill, edge, ink);
        return x - 4;
    }

    /** The tabs as the journal's: pills with a badge, the open one lit with a beating bar under it, the one under the mouse lifted. */
    static void tabs(UiCanvas c, List<Tab> tabs, String open, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, int x, int y,
                     double time) {
        int tabX = x;
        for (Tab tab : tabs) {
            int labelWidth = c.width(tab.label());
            int badge = tab.badge() > 0 ? c.width(String.valueOf(tab.badge())) + 8 : 0;
            int width = labelWidth + 14 + (badge > 0 ? badge + 4 : 0);
            UkakuMenuView.Rect rect = new UkakuMenuView.Rect(tabX, y, width, 16);
            boolean picked = tab.key().equals(open);
            boolean hover = rect.contains(mouseX, mouseY);
            int lift = hover && !picked ? -1 : 0;
            QuestStyle.box(c, rect.x(), rect.y() + lift, rect.width(), rect.height(), picked ? QuestJournalView.ROW_PICKED : hover
                ? QuestJournalView.ROW_HOVER : QuestJournalView.ROW, picked ? QuestStyle.RED : hover ? QuestStyle.RED_DARK : QuestStyle.TILE_EDGE);
            c.text(tab.label(), tabX + 7, y + 4 + lift, picked ? VALUE : hover ? 0xFFD8A8AC : MUTED, false);
            if (badge > 0) {
                int bx = tabX + 10 + labelWidth;
                QuestStyle.box(c, bx, y + 2 + lift, badge, 12, tab.urgent() ? QuestStyle.RED_DARK : QuestStyle.GREY_DARK,
                    tab.urgent() ? QuestStyle.RED : QuestStyle.GREY);
                c.text(String.valueOf(tab.badge()), bx + 4, y + 4 + lift, VALUE, false);
            }
            if (picked) {
                float pulse = 0.6F + 0.4F * (float)Math.sin(time * 3.0);
                c.fill(rect.x() + 4, rect.y() + rect.height() + 2, rect.x() + rect.width() - 4, rect.y() + rect.height() + 4,
                    QuestStyle.mix(QuestStyle.RED_DARK, QuestStyle.RED, pulse));
            }
            hits.put("tab:" + tab.key(), rect);
            tabX += width + 4;
        }
    }

    /**
     * A card as the quest list's ({@code index}: its place, as they slide in one after another): its plate, a bar down its side when picked,
     * its icon in a tile, its title, its chip, the word at its right, the bar along its foot; lifted toward the mouse, glowing gold when it
     * wants a look. Its rect is put in {@code hits} under its key.
     */
    static UkakuMenuView.Rect card(UiCanvas c, Card card, int x, int y, int width, boolean chosen, Map<String, UkakuMenuView.Rect> hits,
                                   double mouseX, double mouseY, double time, int index) {
        int height = CARD_HEIGHT;
        float in = (float)Math.max(0.0, Math.min(1.0, (time - index * 0.05) / 0.25));
        int slide = Math.round((1.0F - in * in * (3.0F - 2.0F * in)) * -24.0F);
        UkakuMenuView.Rect rect = new UkakuMenuView.Rect(x, y, width, height);
        boolean hover = rect.contains(mouseX, mouseY);
        int cx = x + slide + (hover && !chosen ? 3 : 0);
        int edge = chosen ? QuestStyle.RED : hover ? QuestStyle.RED_DARK : card.glow()
            ? QuestStyle.mix(QuestStyle.TILE_EDGE, QuestStyle.GOLD, 0.4F + 0.4F * (float)Math.sin(time * 4.0)) : QuestStyle.TILE_EDGE;
        int fill = chosen ? QuestJournalView.ROW_PICKED : hover ? QuestJournalView.ROW_HOVER : QuestJournalView.ROW;
        c.batch(() -> {
            QuestStyle.box(c, cx, y, width, height, fill, edge);
            if (chosen) {
                c.fill(cx, y + 3, cx + 3, y + height - 3, QuestStyle.RED);
            }
            QuestStyle.box(c, cx + 5, y + 5, 22, 22, QuestStyle.TILE, card.dark());
            if (card.icon() != null) {
                card.icon().draw(c, cx + 7, y + 7, 18, QuestStyle.TILE, time);
            }
        });
        int rightWidth = card.right() == null || card.right().isEmpty() ? 0 : c.width(card.right()) + 8;
        c.text(c.fit(card.title(), width - 40), cx + 33, y + 4, card.dim() ? MUTED : VALUE, false);
        if (card.chip() != null && !card.chip().isEmpty()) {
            int chipFill = QuestStyle.mix(card.dark(), 0xFF000000, 0.35F);
            QuestStyle.chip(c, cx + 33, y + 15, c.fit(card.chip(), width - 52 - rightWidth), chipFill, card.dark(), card.ink());
        }
        if (rightWidth > 0) {
            c.text(card.right(), cx + width - 6 - c.width(card.right()), y + 18, card.ink(), false);
        }
        if (card.share() >= 0.0F) {
            int barY = y + height - 3;
            c.batch(() -> {
                c.fill(cx + 33, barY, cx + width - 6, barY + 1, QuestJournalView.BAR_BACK);
                if (card.share() > 0.0F) {
                    c.fill(cx + 33, barY, cx + 33 + Math.max(2, Math.round((width - 39) * Math.min(1.0F, card.share()))), barY + 1, card.bar());
                }
            });
        }
        hits.put(card.key(), rect);
        return rect;
    }

    /** A section's heading and a thin rule after it, as the journal's; the next row's y. */
    static int section(UiCanvas c, String title, int x, int y, int width) {
        return section(c, title, null, x, y, width);
    }

    /** A section's heading, a hint at its right end (faint), the rule between them; the next row's y. */
    static int section(UiCanvas c, String title, String hint, int x, int y, int width) {
        c.text(title, x, y, QuestJournalView.HEADER, false);
        int after = x + c.width(title) + 8;
        int end = x + width;
        if (hint != null && !hint.isEmpty()) {
            int hintWidth = c.width(hint);
            c.text(hint, end - hintWidth, y, FAINT, false);
            end -= hintWidth + 8;
        }
        if (end > after) {
            c.fill(after, y + 4, end, y + 5, QuestStyle.RED_DARK);
        }
        return y + 15;
    }

    /** Words set off as a quest's story: a red bar down their side; the next row's y. */
    static int story(UiCanvas c, String text, int x, int y, int width) {
        int top = y;
        int rowY = y;
        for (String row : UkakuMenuView.wrap(c, text, width - 14)) {
            c.text(row, x + 10, rowY + 2, STORY, false);
            rowY += 12;
        }
        int bottom = rowY + 2;
        c.batch(() -> c.fill(x, top, x + 2, bottom, QuestStyle.RED_DARK));
        return bottom;
    }

    /** A checklist row as a quest's objective: a box (ticked when {@code met}), its words, a word at the right end. */
    static void checkRow(UiCanvas c, boolean met, boolean dim, String text, String right, int x, int y, int width) {
        c.batch(() -> {
            QuestStyle.box(c, x, y, 11, 11, met ? QuestStyle.GREEN_DARK : QuestStyle.TILE, met ? QuestStyle.GREEN : dim ? QuestStyle.TILE_EDGE
                : QuestStyle.RED_DARK);
            if (met) {
                QuestStyle.check(c, x, y, 11, QuestStyle.GREEN);
            }
        });
        int rightWidth = right == null ? 0 : c.width(right) + 8;
        c.text(c.fit(text, width - 17 - rightWidth), x + 17, y + 2, met ? QuestJournalView.DONE : dim ? MUTED : VALUE, false);
        if (right != null) {
            c.text(right, x + width - c.width(right), y + 2, met ? QuestJournalView.DONE : FAINT, false);
        }
    }

    /** A label and its value on one row (the label muted, the value fitted after {@code labelWidth}). */
    static void row(UiCanvas c, String label, String value, int x, int y, int labelWidth, int width, int ink) {
        c.text(label, x, y, MUTED, false);
        c.text(c.fit(value, width - labelWidth), x + labelWidth, y, ink, false);
    }

    /**
     * A button in the menu's style: a plate with an edge (gold for spending, else red) that lights and lifts under the mouse, a bar under it
     * beating while the mouse is on it. Disabled: grey, and not put in {@code hits} (a click on it does nothing).
     */
    static UkakuMenuView.Rect button(UiCanvas c, String key, String label, int x, int y, int width, int height, boolean enabled, boolean gold,
                                     Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, double time) {
        UkakuMenuView.Rect rect = new UkakuMenuView.Rect(x, y, width, height);
        boolean hover = enabled && rect.contains(mouseX, mouseY);
        int lift = hover ? -1 : 0;
        int bright = gold ? QuestStyle.GOLD : QuestStyle.RED;
        int dark = gold ? QuestStyle.GOLD_DARK : QuestStyle.RED_DARK;
        int edge = !enabled ? QuestStyle.GREY_DARK : hover ? bright : QuestStyle.mix(dark, bright, 0.15F + 0.15F * (float)Math.sin(time * 2.5));
        int fill = !enabled ? 0xFF140A0C : hover ? (gold ? 0xFF3E2810 : QuestJournalView.ROW_PICKED) : gold ? GOLD_FILL : QuestJournalView.ROW;
        c.batch(() -> {
            QuestStyle.box(c, x, y + lift, width, height, fill, edge);
            if (hover) {
                float pulse = 0.6F + 0.4F * (float)Math.sin(time * 5.0);
                c.fill(x + 4, y + height + 1, x + width - 4, y + height + 3, QuestStyle.mix(dark, bright, pulse));
            }
        });
        int ink = !enabled ? QuestStyle.GREY : gold ? QuestStyle.GOLD : hover ? QuestStyle.WHITE : VALUE;
        String text = c.fit(label, width - 8);
        c.text(text, x + (width - c.width(text)) / 2, y + (height - 8) / 2 + 1 + lift, ink, false);
        if (enabled) {
            hits.put(key, rect);
        }
        return rect;
    }

    /**
     * A road as the journal's: the nodes spaced along a line lit as far as {@code reached} (an index; -1 none), each a ring with the path's
     * glyph (done a tick, open a beating tendril, locked a padlock), lit ones ringed; their word under them; at most {@code maxStep} apart.
     * Each is put in {@code hits};
     * the one under the mouse is returned as a tooltip to draw last (null if none).
     */
    static Tip road(UiCanvas c, List<Node> nodes, int reached, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, int x, int y,
                    int width, int maxStep, double time) {
        int n = nodes.size();
        if (n == 0) {
            return null;
        }
        int step = n <= 1 ? 0 : Math.min(maxStep, (width - 20) / (n - 1));
        int midY = y + 13;
        int last = n - 1;
        c.batch(() -> {
            c.fill(x + 10, midY - 1, x + 10 + step * last, midY + 1, QuestJournalView.BAR_BACK);
            if (reached > 0) {
                c.fill(x + 10, midY - 1, x + 10 + step * Math.min(last, reached), midY + 1, QuestJournalView.BAR);
            }
        });
        Tip tip = null;
        for (int i = 0; i < n; i++) {
            Node node = nodes.get(i);
            int cx = x + 10 + step * i;
            UkakuMenuView.Rect rect = new UkakuMenuView.Rect(cx - 8, midY - 8, 16, 16);
            boolean over = rect.contains(mouseX, mouseY);
            QuestLog.State state = node.state();
            float beat = state == QuestLog.State.OPEN ? 0.5F + 0.5F * (float)Math.sin(time * 5.0 + i) : 0.0F;
            int index = i;
            c.batch(() -> {
                if (node.lit() || over) {
                    QuestStyle.circle(c, cx, midY, 9, over ? QuestStyle.RED : QuestStyle.mix(QuestStyle.RED_DARK, QuestStyle.RED,
                        0.3F + 0.3F * (float)Math.sin(time * 3.0 + index)));
                }
                QuestStyle.ring(c, cx, midY, 7, 2, QuestStyle.mix(QuestStyle.dark(state), QuestStyle.colour(state), 0.5F + 0.5F * beat), QuestStyle.TILE);
                QuestStyle.glyph(c, Quest.Line.PATH, state, cx - 4, midY - 4, 9, QuestStyle.TILE, time);
            });
            hits.put(node.key(), rect);
            if (node.label() != null) {
                c.centered(node.label(), cx, midY + 11, node.lit() ? MUTED : FAINT, false);
            }
            if (over) {
                tip = new Tip(node.tip(), cx, midY - 8);
            }
        }
        return tip;
    }

    /** A tooltip over the point {@code tip} hangs from, kept between {@code left} and {@code right}. */
    static void tooltip(UiCanvas c, Tip tip, int left, int right) {
        if (tip == null || tip.text() == null || tip.text().isEmpty()) {
            return;
        }
        int w = c.width(tip.text()) + 12;
        int tx = Math.max(left, Math.min(right - w, tip.x() - w / 2));
        int ty = tip.y() - 19;
        c.batch(() -> QuestStyle.box(c, tx, ty, w, 15, TIP_FILL, QuestStyle.RED));
        c.text(tip.text(), tx + 6, ty + 4, VALUE, false);
    }

    /** A stamp as the journal's: a framed word, twice the size, its top left at {@code x, y}; its width returned. */
    static int stamp(UiCanvas c, int x, int y, String word, int ink, int dark) {
        int w = c.width(word) * 2 + 20;
        c.batch(() -> {
            QuestStyle.box(c, x, y, w, 24, dark, ink);
            QuestStyle.box(c, x + 2, y + 2, w - 4, 20, QuestStyle.mix(dark, 0xFF000000, 0.4F), dark);
        });
        c.push();
        c.translate(x + 10, y + 5);
        c.scale(2.0F);
        c.text(word, 0, 0, ink, false);
        c.pop();
        return w;
    }

    /** The width {@link #stamp} takes for {@code word}. */
    static int stampWidth(UiCanvas c, String word) {
        return c.width(word) * 2 + 20;
    }

    /**
     * 4.1.59: what opens {@code gate}, in words: its quests by title; else the teacher's lesson that opens it ("Noro's trial, The Walker":
     * a lesson is no quest of the book, and "its quest:" with nothing after it was all the menus said); else "its quest".
     */
    static String opener(com.hex.kagune.control.quest.Gate gate) {
        if (gate == null) {
            return "its quest";
        }
        // 4.1.60: a quest by its title in quotes ("the quest \"Four Thorns\""), never mistaken for the stage or form it opens.
        java.util.List<com.hex.kagune.control.quest.Quest> quests = new java.util.ArrayList<>(com.hex.kagune.control.quest.QuestBook.openers(gate));
        quests.removeIf(quest -> com.hex.kagune.control.quest.QuestBook.giver(quest) == com.hex.kagune.control.quest.QuestBook.Giver.RETIRED);
        if (!quests.isEmpty()) {
            StringBuilder text = new StringBuilder("the quest ");
            for (int i = 0; i < quests.size(); i++) {
                if (i > 0) {
                    text.append(i == quests.size() - 1 ? " or " : ", ");
                }
                text.append('"').append(quests.get(i).title()).append('"');
            }
            return text.toString();
        }
        for (com.hex.kagune.control.npc.Teaching.Lesson lesson : com.hex.kagune.control.npc.Teaching.ALL) {
            if (lesson.gate() == gate && !lesson.teacher().isEmpty()) {
                String teacher = Character.toUpperCase(lesson.teacher().charAt(0)) + lesson.teacher().substring(1);
                return teacher + "'s " + (lesson.kind() == com.hex.kagune.control.npc.Teaching.Kind.TRIAL ? "trial" : "lesson") + " \"" + lesson.name() + "\"";
            }
        }
        return "its quest";
    }

    /** A reward chip as the journal's: a glyph ({@link QuestStyle#rewardGlyph}: m, k, p, d), its words; its width returned. */
    static int badge(UiCanvas c, char kind, String text, boolean gold, int x, int y) {
        int ink = gold ? QuestStyle.GOLD : VALUE;
        int fill = gold ? GOLD_FILL : RED_FILL;
        int edge = gold ? QuestStyle.GOLD_DARK : QuestStyle.RED_DARK;
        int w = c.width(text) + 26;
        c.batch(() -> {
            QuestStyle.box(c, x, y, w, 15, fill, edge);
            QuestStyle.rewardGlyph(c, kind, x + 5, y + 2, ink, fill);
        });
        c.text(text, x + 19, y + 4, ink, false);
        return w;
    }
}
