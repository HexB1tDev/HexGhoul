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
 *
 * <p>1.3.3: alive. The board rises into place; the name is large, and the mood (any temper after " - ") sits beside it as a tag of its
 * own colour. How they stand is a bond meter of five bands, enemy to friend, a heart on it that glides to where they now stand, and
 * what an answer did to it floats up off the heart ("+3"). What is said is typed out ({@link #revealed}: quick, but pausing at a comma,
 * longer at the end of a sentence; the screen gives it a voice), and only once it is said do the answers come in, one after another.
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
    /** The bond meter's width, and each band's colour (enemy, cold, stranger, friendly, friend). */
    static final int METER = 210;
    static final int[] BANDS = {0xFFD01E2E, 0xFF8E3A4C, 0xFF7C7482, 0xFF56A86A, 0xFFE0607E};
    /** How quickly the words come (characters a second), and the pauses after a comma and a sentence's end (seconds). */
    static final double PER_SECOND = 52.0;
    static final double COMMA = 0.11;
    static final double STOP = 0.3;
    /** How long the answers take to come in (each after the one before), and the board to rise. */
    static final double STAGGER = 0.055;
    static final double FADE = 0.2;
    static final double RISE = 0.28;

    /** What is shown: who is talking, how it stands with the player, what it says, the answers. */
    public record Model(String name, String temper, int regard, boolean ally, String line, List<NpcTalk.Choice> choices, List<String> labels) {
        public Model(String name, String temper, int regard, boolean ally, String line, List<NpcTalk.Choice> choices) {
            this(name, temper, regard, ally, line, choices, choices.stream().map(NpcTalk.Choice::text).toList());
        }

        /** The words shown for answer {@code i}. */
        public String label(int i) {
            return i < this.labels.size() ? this.labels.get(i) : this.choices.get(i).text();
        }
    }

    /**
     * 1.3.3: where the screen's motion is: seconds since it opened, seconds the words have been coming ({@code typed}; infinite when
     * skipped), the regard as the meter shows it now (gliding to the real one), and what changed it last and how long ago.
     */
    public record Motion(double opened, double typed, float regardShown, int delta, double deltaAge) {
        /** Everything already in place (the previews). */
        public static Motion still(int regard) {
            return new Motion(10.0, Double.POSITIVE_INFINITY, regard, 0, 10.0);
        }
    }

    private NpcTalkView() {
    }

    /** Draws the conversation, all of it in place, and returns its clickable areas: "choice:N". */
    public static Map<String, UkakuMenuView.Rect> draw(UiCanvas c, Model model, double mouseX, double mouseY, double time) {
        return draw(c, model, mouseX, mouseY, time, Motion.still(model.regard()));
    }

    /** Draws the conversation as it is now ({@code m}), and returns its clickable areas: "choice:N" (only the answers already in). */
    public static Map<String, UkakuMenuView.Rect> draw(UiCanvas c, Model model, double mouseX, double mouseY, double time, Motion m) {
        Map<String, UkakuMenuView.Rect> hits = new LinkedHashMap<>();
        int rise = (int)Math.round((1.0 - ease(m.opened() / RISE)) * 36.0);
        int centerX = UI_WIDTH / 2;
        int centerY = UI_HEIGHT - 8 - BOX_HEIGHT / 2 + rise;
        int left = centerX - CONTENT_WIDTH / 2;
        int right = centerX + CONTENT_WIDTH / 2;
        int top = centerY - CONTENT_HEIGHT / 2;
        int bottom = centerY + CONTENT_HEIGHT / 2;
        c.batch(() -> {
            UkakuMenuView.organicPanel(c, centerX, centerY, BOX_WIDTH / 2, BOX_HEIGHT / 2, GraftMenuView.PANEL_FILL, GraftMenuView.PANEL_EDGE);
            UkakuMenuView.organicPanel(c, centerX, centerY, BOX_WIDTH / 2 - 5, BOX_HEIGHT / 2 - 5, GraftMenuView.INNER_FILL, GraftMenuView.INNER_EDGE);
        });
        String text = model.line() == null ? "" : model.line();
        int shown = revealed(text, m.typed());
        boolean talking = shown < text.length();

        // The face, framed in flesh; the frame breathes while it talks.
        int faceX = left + 4;
        int faceY = top + 4;
        int glow = talking ? (int)Math.round(40 + 40 * Math.sin(time * 9.0)) : 0;
        c.fill(faceX - 4, faceY - 4, faceX + FACE + 4, faceY + FACE + 4, mix(0xFF4A0E18, 0xFFB8304A, glow / 255.0F));
        c.fill(faceX - 2, faceY - 2, faceX + FACE + 2, faceY + FACE + 2, 0xFF120408);
        c.face(faceX, faceY, FACE);
        c.hat(faceX, faceY, FACE, null);

        // The name, large; the mood beside it; who they are to each other under it.
        int textX = faceX + FACE + 20;
        int textWidth = right - textX;
        String[] temper = temper(model.temper());
        c.push();
        c.translate(textX, top);
        c.scale(1.5F);
        c.text(model.name(), 0, 0, QuestJournalView.TITLE, true);
        c.pop();
        int nameRight = textX + (int)Math.ceil(c.width(model.name()) * 1.5);
        if (!temper[1].isEmpty()) {
            mood(c, temper[1], nameRight + 8, top + 2, time);
        }
        NpcBonds.Band band = NpcBonds.band(model.regard());
        String standing = model.ally() ? "WALKS WITH YOU" : band.label().toUpperCase(java.util.Locale.ROOT);
        c.text(temper[0].toUpperCase(java.util.Locale.ROOT), textX, top + 18, QuestJournalView.SUBTITLE, false);
        meter(c, right - METER, top + 1, m, standing, time);

        // A vein under the heading.
        c.fill(textX, top + 31, right, top + 32, 0x55B83A44);
        c.fill(textX, top + 31, textX + (int)((right - textX) * Math.min(1.0, m.opened() / 0.5)), top + 32, 0xAAB83A44);

        // The answers: one column, or two when there are many; in only once the words are all said, one after another.
        List<NpcTalk.Choice> choices = model.choices();
        boolean two = choices.size() > 4;
        int rows = two ? (choices.size() + 1) / 2 : choices.size();
        int columnWidth = two ? (CONTENT_WIDTH - 8) / 2 : CONTENT_WIDTH;
        int lowest = bottom - rows * 19;

        // What it says, typed out: as many lines as fit above the answers.
        int y = top + 40;
        List<String> lines = wrap(c, text, textWidth);
        int fit = Math.max(3, (lowest - 4 - y) / 12);
        // 1.3.4: the answers in the middle of the room left under the words (not down against the board's edge).
        int saidTo = y + Math.min(fit, lines.size()) * 12;
        int choicesTop = Math.max(saidTo + 10, Math.min(lowest, saidTo + (bottom - saidTo - rows * 19) / 2));
        int left0 = shown;
        for (int i = 0; i < Math.min(fit, lines.size()) && left0 > 0; i++) {
            String line = lines.get(i);
            String part = line.substring(0, Math.min(line.length(), left0));
            left0 -= line.length() + 1;
            if (i == fit - 1 && lines.size() > fit && !talking) {
                part = c.fit(part + " ...", textWidth);
            }
            c.text(part, textX, y, QuestJournalView.VALUE, false);
            if (talking && left0 <= 0) {
                // The caret where the words are coming from.
                if ((int)(time * 4.0) % 2 == 0) {
                    int caret = textX + c.width(part) + 1;
                    c.fill(caret, y, caret + 4, y + 8, 0xAAE8B8C0);
                }
            }
            y += 12;
        }
        if (talking) {
            String hint = "click to skip  »";
            c.text(hint, right - c.width(hint), bottom - 10, (int)(140 + 80 * Math.sin(time * 3.0)) << 24 | 0x946268, false);
            return hits;
        }
        double since = m.typed() == Double.POSITIVE_INFINITY ? 10.0 : m.typed() - duration(text);
        for (int i = 0; i < choices.size(); i++) {
            float in = (float)ease((since - i * STAGGER) / FADE);
            if (in <= 0.01F) {
                continue;
            }
            int column = two ? i / rows : 0;
            int row = two ? i % rows : i;
            int slide = Math.round((1.0F - in) * 14.0F);
            int x = left + column * (columnWidth + 8) + slide;
            int rowY = choicesTop + row * 19;
            UkakuMenuView.Rect rect = new UkakuMenuView.Rect(x, rowY, columnWidth - slide, 17);
            boolean hover = in > 0.95F && rect.contains(mouseX, mouseY);
            NpcTalk.Choice choice = choices.get(i);
            boolean hostile = choice == NpcTalk.Choice.INSULT || choice == NpcTalk.Choice.PART;
            int alpha = Math.round(255 * in);
            c.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), fade(hover ? QuestJournalView.ROW_PICKED : QuestJournalView.ROW, in));
            // An accent down the left: red for a cruel word, grey for leaving, the flesh's own for the rest; brighter under the pointer.
            int accent = hostile ? 0xFFB06070 : choice == NpcTalk.Choice.LEAVE || choice == NpcTalk.Choice.BACK ? 0xFF64444A : 0xFFB83A44;
            c.fill(rect.x(), rect.y(), rect.x() + (hover ? 3 : 2), rect.y() + rect.height(), fade(hover ? 0xFFF0B8C4 : accent, in));
            c.text(String.valueOf(i + 1), x + 8, rowY + 5, alpha << 24 | (hover ? QuestJournalView.HEADER : QuestJournalView.MUTED) & 0xFFFFFF, false);
            int colour = hover ? QuestJournalView.VALUE : hostile ? 0xFFB06070 : choice == NpcTalk.Choice.LEAVE ? QuestJournalView.MUTED : 0xFFD8B8B8;
            c.text(c.fit(model.label(i), columnWidth - 34 - slide), x + 22 + (hover ? 2 : 0), rowY + 5, alpha << 24 | colour & 0xFFFFFF, false);
            if (in > 0.95F) {
                hits.put("choice:" + i, rect);
            }
        }
        return hits;
    }

    /** "Your date - giddy": the title, and the mood ("" for none). */
    static String[] temper(String temper) {
        String all = temper == null ? "" : temper;
        int dash = all.indexOf(" - ");
        return dash < 0 ? new String[] {all, ""} : new String[] {all.substring(0, dash), all.substring(dash + 3)};
    }

    /** The mood as a tag: a rounded little plate of its own colour, its word in it, breathing gently. */
    static void mood(UiCanvas c, String mood, int x, int y, double time) {
        int colour = moodColour(mood);
        int width = c.width(mood) + 12;
        int pulse = (int)Math.round(20 * (1.0 + Math.sin(time * 2.4)));
        c.fill(x + 1, y, x + width - 1, y + 12, (0x50 + pulse) << 24 | colour & 0xFFFFFF);
        c.fill(x, y + 1, x + width, y + 11, (0x50 + pulse) << 24 | colour & 0xFFFFFF);
        c.fill(x + 1, y + 11, x + width - 1, y + 12, colour);
        c.text(mood, x + 6, y + 2, 0xFFFFFFFF, true);
    }

    static int moodColour(String mood) {
        return switch (mood) {
            case "giddy" -> 0xFFE070A0;
            case "happy" -> 0xFF62B87A;
            case "sulky" -> 0xFF8E6AC0;
            case "jealous" -> 0xFF4E9E58;
            case "lonely" -> 0xFF5C7EC4;
            case "clingy" -> 0xFFE58AB4;
            case "sleepy" -> 0xFF7E8EB0;
            case "hungry" -> 0xFFC02838;
            case "affectionate" -> 0xFFE03C6C;
            case "bored" -> 0xFF8A8A8A;
            case "cold" -> 0xFF6A8AA6;
            case "wary" -> 0xFFB08040;
            default -> 0xFFB83A44;
        };
    }

    /**
     * The bond meter: "BOND" and five bands from enemy to friend, each in its colour (bright up to where they stand, dim past it), a
     * heart where they stand, gliding there, and the last change floating up off it.
     */
    static void meter(UiCanvas c, int x, int y, Motion m, String standing, double time) {
        float regard = m.regardShown();
        int current = NpcBonds.band(Math.round(regard)).ordinal();
        c.text("BOND", x, y, QuestJournalView.MUTED, false);
        c.text(standing, x + c.width("BOND") + 8, y, BANDS[current], false);
        int barY = y + 13;
        int[] edges = {NpcBonds.LEAST, NpcBonds.ENEMY, 0, NpcBonds.FRIENDLY, NpcBonds.FRIEND, NpcBonds.MOST};
        int at = x + Math.round((regard - NpcBonds.LEAST) / (float)(NpcBonds.MOST - NpcBonds.LEAST) * METER);
        c.fill(x - 1, barY - 1, x + METER + 1, barY + 7, 0xFF12060A);
        for (int band = 0; band < 5; band++) {
            int from = x + (edges[band] - NpcBonds.LEAST) * METER / (NpcBonds.MOST - NpcBonds.LEAST);
            int to = x + (edges[band + 1] - NpcBonds.LEAST) * METER / (NpcBonds.MOST - NpcBonds.LEAST);
            int gapFrom = from + (band == 0 ? 0 : 1);
            c.fill(gapFrom, barY, to, barY + 6, fade(BANDS[band], 0.16F));
            int lit = Math.min(to, at);
            if (lit > gapFrom) {
                // The band they stand in bright; the ones behind them, dim: the way they came.
                c.fill(gapFrom, barY, lit, barY + 6, band == current ? BANDS[band] : fade(BANDS[band], 0.45F));
                if (band == current) {
                    c.fill(gapFrom, barY, lit, barY + 2, fade(0xFFFFFFFF, 0.22F));
                }
            }
        }
        // The heart where they stand, twice size on the bar, glowing as it breathes.
        int glow = (int)Math.round(50 + 45 * Math.sin(time * 3.2));
        c.fill(at - 10, barY - 5, at + 10, barY + 11, glow << 24 | BANDS[current] & 0xFFFFFF);
        c.push();
        c.translate(at - 7, barY - 3);
        c.scale(2.0F);
        heart(c, 0, 0, 0xFFFFE6EE, current == 0 ? 0xFF9A1020 : 0xFFE0405E);
        c.pop();
        // What changed, floating up off it.
        if (m.delta() != 0 && m.deltaAge() < 1.8) {
            float life = (float)(m.deltaAge() / 1.8);
            int alpha = Math.max(8, Math.round(255 * (1.0F - life * life)));
            String delta = (m.delta() > 0 ? "+" : "") + m.delta();
            int colour = m.delta() > 0 ? 0x7CE08E : 0xF05060;
            c.text(delta, at - c.width(delta) / 2, barY - 16 - Math.round(life * 12.0F), alpha << 24 | colour, true);
        }
    }

    /** A little heart, 7 by 6: its outline and its fill. */
    static void heart(UiCanvas c, int x, int y, int light, int body) {
        String[] rows = {".XX.XX.", "XXXXXXX", "XXXXXXX", ".XXXXX.", "..XXX..", "...X..."};
        for (int r = 0; r < rows.length; r++) {
            for (int col = 0; col < 7; col++) {
                if (rows[r].charAt(col) == 'X') {
                    c.fill(x + col, y + r, x + col + 1, y + r + 1, r == 1 && (col == 1 || col == 2) ? light : body);
                }
            }
        }
    }

    // ---------------------------------------------------------------- the words coming

    /** How long (seconds) the pause after character {@code i} of {@code text} is, on top of its own time. */
    static double pause(String text, int i) {
        char at = text.charAt(i);
        boolean last = i + 1 >= text.length();
        char next = last ? ' ' : text.charAt(i + 1);
        if (at == ',' || at == ';' || at == ':' || at == '—') {
            return COMMA;
        }
        if ((at == '.' || at == '!' || at == '?') && next == ' ') {
            return STOP;
        }
        if (at == '.' && next == '.') {
            return 0.06;
        }
        return 0.0;
    }

    /** How many characters of {@code text} are out {@code seconds} after it began (all of it, once that is long enough). */
    public static int revealed(String text, double seconds) {
        if (text == null || text.isEmpty() || seconds == Double.POSITIVE_INFINITY) {
            return text == null ? 0 : text.length();
        }
        double clock = 0.0;
        for (int i = 0; i < text.length(); i++) {
            clock += 1.0 / PER_SECOND;
            if (clock > seconds) {
                return i;
            }
            clock += pause(text, i);
        }
        return text.length();
    }

    /** How long {@code text} takes to come out (seconds). */
    public static double duration(String text) {
        double clock = 0.0;
        for (int i = 0; text != null && i < text.length(); i++) {
            clock += 1.0 / PER_SECOND + pause(text, i);
        }
        return clock;
    }

    // ---------------------------------------------------------------- helpers

    static double ease(double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        return c * c * (3.0 - 2.0 * c);
    }

    /** {@code argb} with its alpha times {@code share}. */
    static int fade(int argb, float share) {
        int alpha = Math.max(0, Math.min(255, Math.round(((argb >>> 24) & 255) * share)));
        return alpha << 24 | argb & 0xFFFFFF;
    }

    static int mix(int a, int b, float t) {
        float k = Math.max(0.0F, Math.min(1.0F, t));
        int r = Math.round(((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k);
        int g = Math.round(((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k);
        int bl = Math.round((a & 255) * (1 - k) + (b & 255) * k);
        return 0xFF000000 | r << 16 | g << 8 | bl;
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
