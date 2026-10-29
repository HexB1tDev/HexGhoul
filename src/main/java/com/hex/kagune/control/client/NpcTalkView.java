package com.hex.kagune.control.client;

import com.hex.kagune.control.client.ui.UiCanvas;
import com.hex.kagune.control.npc.NpcBonds;
import com.hex.kagune.control.npc.NpcTalk;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 7.0.2: a conversation with a ghoul NPC, drawn in the house style (the organic board of the kagune menus and the journal), low on
 * the screen so the ghoul stays in sight above it: its face, its name, its temper and how it stands with the player (a bar from
 * enemy to friend), what it says, and the answers, numbered (a click or the number key picks one).
 */
public final class NpcTalkView {
    public static final int UI_WIDTH = UkakuMenuView.UI_WIDTH;
    public static final int UI_HEIGHT = UkakuMenuView.UI_HEIGHT;
    /** The board (an oval) and the content laid inside it, a rectangle whose corners stay within the oval. */
    static final int BOX_WIDTH = 940;
    static final int BOX_HEIGHT = 300;
    static final int CONTENT_WIDTH = 700;
    static final int CONTENT_HEIGHT = 200;
    static final int FACE = 64;
    static final int ENEMY = 0xFFD01E2E;
    static final int FRIEND = 0xFF3FA052;

    /** What is shown: who is talking, how it stands with the player, what it says, the answers. */
    public record Model(String name, String temper, int regard, boolean ally, String line, List<NpcTalk.Choice> choices) {
    }

    private NpcTalkView() {
    }

    /** Draws the conversation and returns its clickable areas: "choice:N". */
    public static Map<String, UkakuMenuView.Rect> draw(UiCanvas c, Model model, double mouseX, double mouseY, double time) {
        Map<String, UkakuMenuView.Rect> hits = new LinkedHashMap<>();
        int centerX = UI_WIDTH / 2;
        int centerY = UI_HEIGHT - 8 - BOX_HEIGHT / 2;
        int left = centerX - CONTENT_WIDTH / 2;
        int right = centerX + CONTENT_WIDTH / 2;
        int top = centerY - CONTENT_HEIGHT / 2;
        int bottom = centerY + CONTENT_HEIGHT / 2;
        c.batch(() -> {
            UkakuMenuView.organicPanel(c, centerX, centerY, BOX_WIDTH / 2, BOX_HEIGHT / 2, GraftMenuView.PANEL_FILL, GraftMenuView.PANEL_EDGE);
            UkakuMenuView.organicPanel(c, centerX, centerY, BOX_WIDTH / 2 - 5, BOX_HEIGHT / 2 - 5, GraftMenuView.INNER_FILL, GraftMenuView.INNER_EDGE);
            UkakuMenuView.drawVein(c, left - 30, top + 96, centerX, top + 84, right + 30, top + 96, GraftMenuView.VEIN, 1);
        });
        // The face, framed in flesh.
        int faceX = left + 4;
        int faceY = top + 4;
        c.fill(faceX - 4, faceY - 4, faceX + FACE + 4, faceY + FACE + 4, 0xFF4A0E18);
        c.fill(faceX - 2, faceY - 2, faceX + FACE + 2, faceY + FACE + 2, 0xFF120408);
        c.face(faceX, faceY, FACE);
        c.hat(faceX, faceY, FACE, null);

        // Who, what, and how it stands with them.
        int textX = faceX + FACE + 20;
        int textWidth = right - textX;
        c.text(model.name(), textX, top + 2, QuestJournalView.TITLE, true);
        NpcBonds.Band band = NpcBonds.band(model.regard());
        String standing = model.ally() ? "WALKS WITH YOU" : band.label().toUpperCase(java.util.Locale.ROOT);
        c.text(model.temper().toUpperCase(java.util.Locale.ROOT) + "  /  " + standing, textX, top + 17, QuestJournalView.SUBTITLE, false);
        regard(c, textX, top + 32, textWidth, model.regard());

        // What it says.
        int y = top + 46;
        List<String> lines = wrap(c, model.line(), textWidth);
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            c.text(lines.get(i), textX, y, QuestJournalView.VALUE, false);
            y += 12;
        }

        // The answers: one column, or two when there are many.
        List<NpcTalk.Choice> choices = model.choices();
        boolean two = choices.size() > 4;
        int rows = two ? (choices.size() + 1) / 2 : choices.size();
        int columnWidth = two ? (CONTENT_WIDTH - 8) / 2 : CONTENT_WIDTH;
        int choicesTop = bottom - rows * 19;
        for (int i = 0; i < choices.size(); i++) {
            int column = two ? i / rows : 0;
            int row = two ? i % rows : i;
            int x = left + column * (columnWidth + 8);
            int rowY = choicesTop + row * 19;
            UkakuMenuView.Rect rect = new UkakuMenuView.Rect(x, rowY, columnWidth, 17);
            boolean hover = rect.contains(mouseX, mouseY);
            NpcTalk.Choice choice = choices.get(i);
            boolean hostile = choice == NpcTalk.Choice.INSULT || choice == NpcTalk.Choice.PART;
            c.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), hover ? QuestJournalView.ROW_PICKED : QuestJournalView.ROW);
            c.text(String.valueOf(i + 1), x + 7, rowY + 5, hover ? QuestJournalView.HEADER : QuestJournalView.MUTED, false);
            int colour = hover ? QuestJournalView.VALUE : hostile ? 0xFFB06070 : choice == NpcTalk.Choice.LEAVE ? QuestJournalView.MUTED : 0xFFD8B8B8;
            c.text(c.fit(choice.text(), columnWidth - 30), x + 22, rowY + 5, colour, false);
            hits.put("choice:" + i, rect);
        }
        return hits;
    }

    /** The regard as a bar from the least to the most, filled from the middle, with the enemy, friendly and friend marks. */
    static void regard(UiCanvas c, int x, int y, int width, int regard) {
        c.fill(x, y, x + width, y + 4, QuestJournalView.BAR_BACK);
        int middle = x + width / 2;
        int at = x + (int)Math.round((regard - NpcBonds.LEAST) / (double)(NpcBonds.MOST - NpcBonds.LEAST) * width);
        if (at != middle) {
            c.fill(Math.min(middle, at), y, Math.max(middle, at), y + 4, regard < 0 ? ENEMY : FRIEND);
        }
        for (int mark : new int[] {NpcBonds.ENEMY, 0, NpcBonds.FRIENDLY, NpcBonds.FRIEND}) {
            int mx = x + (int)Math.round((mark - NpcBonds.LEAST) / (double)(NpcBonds.MOST - NpcBonds.LEAST) * width);
            c.fill(mx, y - 2, mx + 1, y + 6, mark == 0 ? QuestJournalView.FAINT : QuestJournalView.MUTED);
        }
    }

    /** {@code text} broken into lines no wider than {@code width}. */
    static List<String> wrap(UiCanvas c, String text, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String tried = line.length() == 0 ? word : line + " " + word;
            if (c.width(tried) > width && line.length() > 0) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(tried);
            }
        }
        if (line.length() > 0) {
            out.add(line.toString());
        }
        return out;
    }
}
