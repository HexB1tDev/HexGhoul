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
 * <p>4.1.55 (the owner: "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as interactable as it is,
 * Dialog, mastery menus ALL OF THEM"): the quest menu's box ({@link QuestStyle}), the face in a tile as a quest's icon, the name as its
 * title, the words as a quest's story, the answers as the quest list's cards under their heading ({@link #card}).
 *
 * <p>1.3.3: alive. The board rises into place; the name is large, and the mood (any temper after " - ") sits beside it as a tag of its
 * own colour. How they stand is a bond meter of five bands, enemy to friend, a heart on it that glides to where they now stand, and
 * what an answer did to it floats up off the heart ("+3"). What is said is typed out ({@link #revealed}: quick, but pausing at a comma,
 * longer at the end of a sentence; the screen gives it a voice), and only once it is said do the answers come in, one after another.
 */
public final class NpcTalkView {
    public static final int UI_WIDTH = UkakuMenuView.UI_WIDTH;
    public static final int UI_HEIGHT = UkakuMenuView.UI_HEIGHT;
    /** The face's size on the board. */
    static final int FACE = 56;
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
    public record Model(String name, String temper, int regard, boolean ally, String line, List<NpcTalk.Choice> choices, List<String> labels,
                        int standing) {
        public Model(String name, String temper, int regard, boolean ally, String line, List<NpcTalk.Choice> choices) {
            this(name, temper, regard, ally, line, choices, choices.stream().map(NpcTalk.Choice::text).toList());
        }

        public Model(String name, String temper, int regard, boolean ally, String line, List<NpcTalk.Choice> choices, List<String> labels) {
            this(name, temper, regard, ally, line, choices, labels, -1);
        }

        /** 4.1.52: true if the page shows the player's reputation in the speaker's gang (one of their own, its leader too). */
        public boolean ranked() {
            return this.standing >= 0;
        }

        /** The words shown for answer {@code i} (4.1.55: the answer's own words when none are given, as the client fills them in). */
        public String label(int i) {
            String label = i < this.labels.size() ? this.labels.get(i) : "";
            return label == null || label.isEmpty() ? this.choices.get(i).text() : label;
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
        String text = model.line() == null ? "" : model.line();
        List<NpcTalk.Choice> choices = model.choices();
        boolean two = choices.size() > 4;
        int rows = two ? (choices.size() + 1) / 2 : choices.size();
        int inner = BOARD_WIDTH - PAD * 2;
        List<String> lines = wrap(c, text, inner - QUOTE_IN - 4);
        // The box's size from everything it will hold (never from what is out yet: it does not jump as the answers come in).
        int quoteLines = Math.max(2, Math.min(MOST_LINES, lines.size()));
        int height = HEADER + quoteLines * LINE + 14 + 15 + rows * (ROW_HEIGHT + ROW_GAP) + 10;
        int rise = (int)Math.round((1.0 - ease(m.opened() / RISE)) * 36.0);
        int x = (UI_WIDTH - BOARD_WIDTH) / 2;
        int y = UI_HEIGHT - 8 - height + rise;
        int shown = revealed(text, m.typed());
        boolean talking = shown < text.length();

        // 4.1.55 (the owner: "Every single menu in the game is to mimic and look like the quest menu, and be as interactable as it is, Dialog
        // ..."): the quest menu's own plate (QuestStyle.backdrop: its cut corners, its bright brackets, its honeycomb), low on the screen so
        // the one talking stays in sight above it; the same red rule fading out at both ends under the header.
        int h = height;
        c.batch(() -> {
            // Its honeycomb in the header's empty middle, between the name and the meters (under the answers it showed between the cards).
            QuestStyle.backdrop(c, x, y, BOARD_WIDTH, h, time, x + 300, y + 8, 150, HEADER - 20);
            for (int i = 0; i < 40; i++) {
                float t = i / 39.0F;
                int alpha = Math.round(190 * (1.0F - Math.abs(t - 0.5F) * 2.0F));
                int x0 = x + 12 + Math.round((BOARD_WIDTH - 24) * i / 40.0F);
                c.fill(x0, y + HEADER - 8, x0 + (BOARD_WIDTH - 24) / 40 + 1, y + HEADER - 7, alpha << 24 | 0xE8323E);
            }
        });

        // The face in a tile as a quest's icon is (its edge lit while it talks), the name large as the menu's title, the mood a chip beside
        // it, who they are to each other under it in red.
        int faceX = x + PAD;
        int faceY = y + 12;
        float speaking = talking ? 0.5F + 0.5F * (float)Math.sin(time * 9.0) : 0.0F;
        c.batch(() -> {
            QuestStyle.box(c, faceX, faceY, FACE + 6, FACE + 6, QuestStyle.TILE, QuestStyle.mix(QuestStyle.RED_DARK, QuestStyle.RED, speaking));
            c.fill(faceX + 2, faceY + 2, faceX + FACE + 4, faceY + FACE + 4, 0xFF120408);
        });
        c.face(faceX + 3, faceY + 3, FACE);
        c.hat(faceX + 3, faceY + 3, FACE, null);
        int textX = faceX + FACE + 18;
        String[] temper = temper(model.temper());
        c.push();
        c.translate(textX, y + 14);
        c.scale(1.4F);
        c.text(model.name(), 0, 0, QuestJournalView.TITLE, true);
        c.pop();
        int nameRight = textX + (int)Math.ceil(c.width(model.name()) * 1.4);
        if (!temper[1].isEmpty()) {
            mood(c, temper[1], nameRight + 8, y + 15, time);
        }
        c.text(temper[0].toUpperCase(java.util.Locale.ROOT), textX, y + 33, QuestJournalView.SUBTITLE, false);
        if (model.ally()) {
            QuestStyle.chip(c, textX, y + 46, "WALKS WITH YOU", 0xFF0E2214, QuestStyle.GREEN_DARK, QuestStyle.GREEN);
        }
        // How they stand: the bond (and, 4.1.52, the player's reputation in their gang) on the right, as the menu's progress is.
        NpcBonds.Band band = NpcBonds.band(model.regard());
        String standing = model.ally() ? "WALKS WITH YOU" : band.label().toUpperCase(java.util.Locale.ROOT);
        int meterX = x + BOARD_WIDTH - PAD - METER;
        meter(c, meterX, y + 14, m, standing, time);
        if (model.ranked()) {
            reputation(c, meterX, y + 40, model.standing() & 0xFF, model.standing() >> 8, time);
        }

        // What they say, set off as a quest's story is: a red bar down its side (bright, beating, while the words come), typed out.
        int quoteX = x + PAD;
        int quoteY = y + HEADER + 2;
        int quoteBottom = quoteY + quoteLines * LINE + 2;
        int bar = talking ? QuestStyle.mix(QuestStyle.RED_DARK, QuestStyle.RED, speaking) : QuestStyle.RED_DARK;
        c.fill(quoteX, quoteY - 2, quoteX + 2, quoteBottom, bar);
        int lineY = quoteY;
        int left0 = shown;
        for (int i = 0; i < Math.min(quoteLines, lines.size()) && left0 > 0; i++) {
            String line = lines.get(i);
            String part = line.substring(0, Math.min(line.length(), left0));
            left0 -= line.length() + 1;
            if (i == quoteLines - 1 && lines.size() > quoteLines && !talking) {
                part = c.fit(part + " ...", inner - QUOTE_IN - 4);
            }
            c.text(part, quoteX + QUOTE_IN, lineY, 0xFFDCAEB2, false);
            if (talking && left0 <= 0 && (int)(time * 4.0) % 2 == 0) {
                // The caret where the words are coming from.
                int caret = quoteX + QUOTE_IN + c.width(part) + 1;
                c.fill(caret, lineY, caret + 4, lineY + 8, 0xAAE8B8C0);
            }
            lineY += LINE;
        }

        // The answers under their heading, as the quest list's cards.
        int answersY = quoteBottom + 12;
        int cardsY = section(c, "ANSWERS", quoteX, answersY, inner, talking ? "" : "click, or press its number");
        if (talking) {
            String hint = "click to skip  \u00bb";
            c.text(hint, x + BOARD_WIDTH - PAD - c.width(hint), cardsY + 2, (int)(140 + 80 * Math.sin(time * 3.0)) << 24 | 0x946268, false);
            return hits;
        }
        int columnWidth = two ? (inner - 8) / 2 : inner;
        double since = m.typed() == Double.POSITIVE_INFINITY ? 10.0 : m.typed() - duration(text);
        for (int i = 0; i < choices.size(); i++) {
            float in = (float)ease((since - i * STAGGER) / FADE);
            if (in <= 0.01F) {
                continue;
            }
            int column = two ? i / rows : 0;
            int row = two ? i % rows : i;
            int slide = Math.round((1.0F - in) * -18.0F);
            int cardX = quoteX + column * (columnWidth + 8);
            int cardY = cardsY + row * (ROW_HEIGHT + ROW_GAP);
            UkakuMenuView.Rect rect = new UkakuMenuView.Rect(cardX, cardY, columnWidth, ROW_HEIGHT);
            boolean hover = in > 0.95F && rect.contains(mouseX, mouseY);
            card(c, model, i, cardX + slide + (hover ? 3 : 0), cardY, columnWidth, hover, in, time);
            if (in > 0.95F) {
                hits.put("choice:" + i, rect);
            }
        }
        return hits;
    }

    /** 4.1.55: the box's width, its padding, the header's height (to the rule), a line of the words, the words' indent, the most lines. */
    static final int BOARD_WIDTH = 680;
    static final int PAD = 16;
    static final int HEADER = 82;
    static final int LINE = 12;
    static final int QUOTE_IN = 10;
    static final int MOST_LINES = 5;
    /** 4.1.55: an answer's card, and the room between two. */
    static final int ROW_HEIGHT = 18;
    static final int ROW_GAP = 3;

    /** 4.1.55: a section's heading and a thin rule after it (the quest menu's), a hint at its right end; the next row's y. */
    static int section(UiCanvas c, String title, int x, int y, int width, String hint) {
        c.text(title, x, y, QuestJournalView.HEADER, false);
        int after = x + c.width(title) + 8;
        int end = hint.isEmpty() ? x + width : x + width - c.width(hint) - 8;
        c.fill(after, y + 4, end, y + 5, QuestStyle.RED_DARK);
        if (!hint.isEmpty()) {
            c.text(hint, x + width - c.width(hint), y, QuestJournalView.FAINT, false);
        }
        return y + 15;
    }

    /**
     * 4.1.55: answer {@code i} as a card of the quest list: a plate (lit and edged red under the pointer, a bar down its side), its number in
     * a key cap, the words, and a small mark at its right end for what kind of answer it is (a word, something sweet, a gift, a lesson, a
     * favour, going with them, leaving, a cruel word). It slides in from the left as the answers come.
     */
    static void card(UiCanvas c, Model model, int i, int x, int y, int width, boolean hover, float in, double time) {
        NpcTalk.Choice choice = model.choices().get(i);
        Kind kind = kind(choice);
        boolean hostile = kind == Kind.CRUEL;
        int edge = hover ? QuestStyle.RED : hostile ? 0xFF5A1A24 : QuestStyle.TILE_EDGE;
        int fill = hover ? QuestJournalView.ROW_PICKED : QuestJournalView.ROW;
        c.batch(() -> {
            QuestStyle.box(c, x, y, width, ROW_HEIGHT, fade(fill, in), fade(edge, in));
            if (hover) {
                c.fill(x, y + 3, x + 3, y + ROW_HEIGHT - 3, QuestStyle.RED);
            }
            QuestStyle.box(c, x + 4, y + 2, 14, 14, fade(QuestStyle.TILE, in), fade(hover ? QuestStyle.RED : QuestStyle.RED_DARK, in));
        });
        int alpha = Math.round(255 * in);
        String number = String.valueOf(i + 1);
        c.text(number, x + 11 - c.width(number) / 2, y + 5, alpha << 24 | (hover ? QuestJournalView.VALUE : QuestJournalView.MUTED) & 0xFFFFFF, false);
        int colour = hover ? QuestJournalView.VALUE : hostile ? 0xFFB06070 : kind == Kind.LEAVE ? QuestJournalView.MUTED : 0xFFD8B8B8;
        c.text(c.fit(model.label(i), width - 46), x + 24, y + 5, alpha << 24 | colour & 0xFFFFFF, false);
        mark(c, kind, x + width - 16, y + 4, fade(hover ? markColour(kind, true) : markColour(kind, false), in), time);
    }

    /** 4.1.55: what kind of answer a choice is (its mark on its card). */
    public enum Kind { WORD, SWEET, GIFT, LESSON, FAVOUR, GO, LEAVE, CRUEL }

    public static Kind kind(NpcTalk.Choice choice) {
        return switch (choice) {
            case KISS, HUG, SWEET, ASK_OUT, PROPOSE, NIGHT, HOME, TEASE -> Kind.SWEET;
            case GIFT, BOOK, FLOWER, PUPPY, LETTER -> Kind.GIFT;
            case LESSON, TRIAL, SURGERY -> Kind.LESSON;
            case FAVOUR, ACCEPT, INVITE -> Kind.FAVOUR;
            case FOLLOW, STAY -> Kind.GO;
            case LEAVE, BACK, DECLINE, PART -> Kind.LEAVE;
            case INSULT -> Kind.CRUEL;
            default -> Kind.WORD;
        };
    }

    static int markColour(Kind kind, boolean lit) {
        int colour = switch (kind) {
            case SWEET -> 0xFFE0607E;
            case GIFT, LESSON, FAVOUR -> QuestStyle.GOLD;
            case GO -> QuestStyle.GREEN;
            case CRUEL -> 0xFFD03A4A;
            case LEAVE -> QuestStyle.GREY;
            default -> 0xFFB86A74;
        };
        return lit ? QuestStyle.mix(colour, 0xFFFFFFFF, 0.25F) : QuestStyle.mix(colour, 0xFF000000, 0.15F);
    }

    /** 4.1.55: an answer's mark, about 11 pixels square at {@code x, y}. */
    static void mark(UiCanvas c, Kind kind, int x, int y, int colour, double time) {
        switch (kind) {
            case SWEET -> heart(c, x + 2, y + 2, QuestStyle.mix(colour, 0xFFFFFFFF, 0.5F), colour);
            case GIFT -> {
                // A little parcel: its box, its ribbon, its bow.
                c.fill(x + 1, y + 4, x + 10, y + 11, colour);
                c.fill(x + 5, y + 4, x + 6, y + 11, 0xFF1C0A0E);
                c.fill(x, y + 3, x + 11, y + 5, colour);
                c.fill(x + 3, y + 1, x + 5, y + 3, colour);
                c.fill(x + 6, y + 1, x + 8, y + 3, colour);
            }
            case LESSON -> QuestStyle.rewardGlyph(c, 'm', x, y, colour, QuestJournalView.ROW);
            case FAVOUR -> QuestStyle.rewardGlyph(c, 'p', x, y, colour, QuestJournalView.ROW);
            case GO -> {
                // On together: a chevron, twice.
                QuestStyle.line(c, x + 1, y + 2, x + 4.5F, y + 5.5F, 1.6F, colour);
                QuestStyle.line(c, x + 4.5F, y + 5.5F, x + 1, y + 9, 1.6F, colour);
                QuestStyle.line(c, x + 6, y + 2, x + 9.5F, y + 5.5F, 1.6F, colour);
                QuestStyle.line(c, x + 9.5F, y + 5.5F, x + 6, y + 9, 1.6F, colour);
            }
            case LEAVE -> {
                // Away: an arrow back to the left.
                QuestStyle.line(c, x + 1, y + 5.5F, x + 10, y + 5.5F, 1.6F, colour);
                QuestStyle.line(c, x + 1, y + 5.5F, x + 4.5F, y + 2, 1.6F, colour);
                QuestStyle.line(c, x + 1, y + 5.5F, x + 4.5F, y + 9, 1.6F, colour);
            }
            case CRUEL -> {
                QuestStyle.line(c, x + 2, y + 2, x + 9, y + 9, 1.8F, colour);
                QuestStyle.line(c, x + 9, y + 2, x + 2, y + 9, 1.8F, colour);
            }
            default -> {
                // A word: a speech bubble, its tail at the bottom left.
                QuestStyle.box(c, x, y + 1, 11, 7, colour, colour);
                c.polygon(new float[] {x + 2, x + 5, x + 2}, new float[] {y + 7.5F, y + 7.5F, y + 10.5F}, colour);
                c.fill(x + 2, y + 4, x + 3, y + 5, 0xFF1C0A0E);
                c.fill(x + 5, y + 4, x + 6, y + 5, 0xFF1C0A0E);
                c.fill(x + 8, y + 4, x + 9, y + 5, 0xFF1C0A0E);
            }
        }
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

    /**
     * 4.1.52: each gang's colour on its meter (none, Aogiri's dark red, the Clowns' blue, the Gas Masks' green; 4.1.70: Anteiku's light brown;
     * 4.1.71: the CCG's cyan blue).
     */
    static final int[] GANG_COLOURS = {0xFF8A8A8A, 0xFFC4283A, 0xFF4F78E8, 0xFF58C050, 0xFF000000 | com.hex.kagune.control.npc.GangPlayer.ANTEIKU_BROWN,
        0xFF000000 | com.hex.kagune.control.npc.GangPlayer.CCG_CYAN};

    /**
     * 4.1.52: the reputation meter: "REPUTATION", the word for where they stand (FEARED bright; UNWANTED red, beating), the share; a bar in
     * the gang's colour filled to it, its marks at each fifth, the feared stretch lined above, a diamond where they stand.
     */
    static void reputation(UiCanvas c, int x, int y, int rep, int gang, double time) {
        int value = Math.max(0, Math.min(com.hex.kagune.control.npc.GangStanding.MOST, rep));
        int colour = GANG_COLOURS[Math.max(0, Math.min(GANG_COLOURS.length - 1, gang))];
        String band = com.hex.kagune.control.npc.GangStanding.band(value).toUpperCase(java.util.Locale.ROOT);
        int beat = (int)Math.round(160 + 95 * Math.sin(time * 6.0));
        int bandColour = value <= 0 ? beat << 24 | 0xF03040 : value >= com.hex.kagune.control.npc.GangStanding.FEARED ? mix(colour, 0xFFFFFFFF, 0.35F) : colour;
        c.text("REPUTATION", x, y, QuestJournalView.MUTED, false);
        c.text(band, x + c.width("REPUTATION") + 8, y, bandColour, false);
        String share = value + "%";
        c.text(share, x + METER - c.width(share), y, QuestJournalView.VALUE, false);
        int barY = y + 13;
        int most = com.hex.kagune.control.npc.GangStanding.MOST;
        int feared = x + com.hex.kagune.control.npc.GangStanding.FEARED * METER / most;
        c.fill(x - 1, barY - 1, x + METER + 1, barY + 7, 0xFF12060A);
        // The track, the feared stretch (80 up) a shade brighter.
        c.fill(x, barY, feared, barY + 6, fade(colour, 0.16F));
        c.fill(feared + 1, barY, x + METER, barY + 6, fade(colour, 0.34F));
        int at = x + value * METER / most;
        if (at > x) {
            int fill = value >= com.hex.kagune.control.npc.GangStanding.FEARED ? mix(colour, 0xFFFFFFFF, 0.2F) : colour;
            c.fill(x, barY, at, barY + 6, fill);
            c.fill(x, barY, at, barY + 2, fade(0xFFFFFFFF, 0.22F));
        }
        for (int mark = 20; mark < 100; mark += 20) {
            int mx = x + mark * METER / most;
            c.fill(mx, barY, mx + 1, barY + 6, 0x90000000);
        }
        // Where they stand: a diamond the heart's size, glowing as it breathes.
        int glow = (int)Math.round(50 + 45 * Math.sin(time * 3.2));
        c.fill(at - 9, barY - 5, at + 10, barY + 11, glow << 24 | colour & 0xFFFFFF);
        c.push();
        c.translate(at - 5, barY - 3);
        c.scale(2.0F);
        diamond(c, 0, 0, 0xFFFFFFFF, value <= 0 ? 0xFFF03040 : mix(colour, 0xFFFFFFFF, 0.35F));
        c.pop();
    }

    /** 4.1.52: a little diamond, 5 by 6 (the reputation meter's marker): its light edge and its fill, outlined dark. */
    static void diamond(UiCanvas c, int x, int y, int light, int body) {
        String[] rows = {"..X..", ".XLX.", "XLXXX", "XXXXX", ".XXX.", "..X.."};
        for (int r = 0; r < rows.length; r++) {
            for (int col = 0; col < 5; col++) {
                char ch = rows[r].charAt(col);
                if (ch != '.') {
                    c.fill(x + col, y + r, x + col + 1, y + r + 1, ch == 'L' ? light : body);
                }
            }
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
