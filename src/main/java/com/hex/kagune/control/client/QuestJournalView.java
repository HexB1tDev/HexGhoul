package com.hex.kagune.control.client;

import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.client.ui.UiCanvas;
import com.hex.kagune.control.quest.Gate;
import com.hex.kagune.control.quest.Hunts;
import com.hex.kagune.control.quest.Objective;
import com.hex.kagune.control.quest.Quest;
import com.hex.kagune.control.quest.QuestBook;
import com.hex.kagune.control.quest.QuestLog;
import com.hex.kagune.control.quest.Reward;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 7.0.1: the quest journal (the quests key), drawn like the kagune menus (the same organic board, 960 x 520 scaled to the window):
 * four tabs (the path's own quests, the ghoul's way, tonight's hunts, what is done or closed), the quests in the tab on the left,
 * and the one picked on the right: its story, every objective's progress, what it pays and unlocks, its rival way, what it waits
 * for. At the bottom of the path's tab, what is still locked and which quests open it.
 */
public final class QuestJournalView {
    /**
     * 4.1.34 (the owner: "Where does the menu have to fill the entire screen? Why not just like a cute like thing that still allows you to see
     * the background? Like why not just a cute box?"): a compact box, the world seen all round it.
     */
    public static final int UI_WIDTH = 640;
    public static final int UI_HEIGHT = 380;
    /** 4.1.34: the box's columns: the list's left and width, the quest's left and width, the content's top. */
    static final int LIST_X = 12;
    static final int LIST_WIDTH = 212;
    static final int DETAIL_X = 244;
    static final int DETAIL_WIDTH = 384;
    static final int CONTENT_Y = 78;

    static final int TITLE = 0xFFF4DADA;
    static final int SUBTITLE = 0xFFB83A44;
    static final int HEADER = 0xFFE8323E;
    static final int VALUE = 0xFFF0D6D6;
    static final int MUTED = 0xFF946268;
    static final int FAINT = 0xFF64444A;
    static final int OPEN = 0xFFF0B060;
    static final int DONE = 0xFF7FD08A;
    static final int CLOSED = 0xFF6A5A5E;
    static final int BAR_BACK = 0xFF34101A;
    static final int BAR = 0xFFD01E2E;
    static final int BAR_DONE = 0xFF3FA052;
    static final int ROW = 0xFF1C0A0E;
    static final int ROW_HOVER = 0xFF34101A;
    static final int ROW_PICKED = 0xFF4A0E18;
    /** Where a detail row's value starts after its label. */
    static final int LABEL_WIDTH = 90;

    /** The tabs: the path's quests, any ghoul's, the hunts on, 1.3.29: the companions (romance), what is over. */
    public enum Tab {
        PATH("PATH"), GHOUL("GHOUL'S WAY"), HUNTS("HUNTS"), ROMANCE("ROMANCE"), DONE("DONE");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    /**
     * What the journal shows: whose it is, the log, the game time (a hunt's clock), the tab and the quest picked (null: the first); 1.3.29:
     * how they stand with each companion (the Romance tab; "romance:KEY" is picked there).
     */
    public record Model(KagunePath path, int mastery, QuestLog log, long time, Tab tab, String picked,
                        List<com.hex.kagune.control.npc.RomanceCard> romance) {
        public Model(KagunePath path, int mastery, QuestLog log, long time, Tab tab, String picked) {
            this(path, mastery, log, time, tab, picked, List.of());
        }
    }

    private QuestJournalView() {
    }

    /** The quests listed in {@code tab}, in the book's order (open first, then the locked; done and closed in their own tab). */
    public static List<Quest> listed(Model model, Tab tab) {
        List<Quest> open = new ArrayList<>();
        List<Quest> locked = new ArrayList<>();
        for (Quest quest : model.log().quests()) {
            if (!quest.on(model.path())) {
                continue;
            }
            QuestLog.State state = model.log().state(quest, model.path(), model.mastery());
            if (!model.log().shown(quest, state)) {
                continue; // 1.3.29: not given yet (a teacher's, the ghouls'), a feat not done, a retired one: not listed
            }
            boolean over = state == QuestLog.State.DONE || state == QuestLog.State.CLOSED;
            boolean fits = switch (tab) {
                case PATH -> !over && quest.line() == Quest.Line.PATH;
                case GHOUL -> !over && (quest.line() == Quest.Line.GHOUL || quest.line() == Quest.Line.FAVOUR);
                case HUNTS -> !over && quest.line() == Quest.Line.HUNT;
                case ROMANCE -> false;
                case DONE -> over;
            };
            if (fits) {
                (state == QuestLog.State.OPEN || over ? open : locked).add(quest);
            }
        }
        open.addAll(locked);
        return open;
    }

    /** The quest shown on the right: the one picked if it is in the tab, else the tab's first. */
    public static Quest picked(Model model) {
        List<Quest> list = listed(model, model.tab());
        for (Quest quest : list) {
            if (quest.id().equals(model.picked())) {
                return quest;
            }
        }
        return list.isEmpty() ? null : list.get(0);
    }

    /** Draws the journal and returns its clickable areas: "tab:NAME", "quest:ID". */
    public static Map<String, UkakuMenuView.Rect> draw(UiCanvas c, Model model, double mouseX, double mouseY, double time) {
        Map<String, UkakuMenuView.Rect> hits = new LinkedHashMap<>();
        // 4.1.34 (the owner: "the background and all, they're also super ugly"; "why not just a cute box?"): a clean compact plate.
        c.batch(() -> {
            QuestStyle.backdrop(c, 0, 0, UI_WIDTH, UI_HEIGHT, time);
            for (int i = 0; i < 40; i++) {
                float t = i / 39.0F;
                int alpha = Math.round(190 * (1.0F - Math.abs(t - 0.5F) * 2.0F));
                int x0 = 12 + Math.round((UI_WIDTH - 24) * i / 40.0F);
                c.fill(x0, 46, x0 + (UI_WIDTH - 24) / 40 + 1, 47, alpha << 24 | 0xE8323E);
            }
            for (int i = 0; i < 24; i++) {
                float t = i / 23.0F;
                int alpha = Math.round(170 * (1.0F - Math.abs(t - 0.5F) * 1.6F));
                int y0 = CONTENT_Y + Math.round((UI_HEIGHT - CONTENT_Y - 14) * i / 24.0F);
                c.fill(DETAIL_X - 11, y0, DETAIL_X - 10, y0 + (UI_HEIGHT - CONTENT_Y - 14) / 24 + 1, Math.max(0, alpha) << 24 | 0xA0202C);
            }
            QuestStyle.polygonDiamond(c, DETAIL_X - 10.5F, CONTENT_Y + (UI_HEIGHT - CONTENT_Y - 14) / 2.0F, 3.5F, QuestStyle.RED);
        });
        header(c, model, time);
        tabs(c, model, hits, mouseX, mouseY, LIST_X, 54, time);
        if (model.tab() == Tab.ROMANCE) {
            romance(c, model, hits, mouseX, mouseY, LIST_X, CONTENT_Y);
            return hits;
        }
        Quest picked = picked(model);
        list(c, model, hits, mouseX, mouseY, picked, LIST_X, CONTENT_Y, time);
        if (picked == null) {
            String empty = model.tab() == Tab.HUNTS ? "No hunt on. Ask a ghoul at night if there's prey out; a hunt ends at dawn."
                : model.tab() == Tab.GHOUL ? "Nothing given yet. Teachers give their lessons when asked; the ghouls of the night, when talked to."
                : model.log().finished() ? "Every quest is finished, on every kagune." : "Nothing here yet.";
            int rowY = CONTENT_Y + 4;
            for (String row : UkakuMenuView.wrap(c, empty, DETAIL_WIDTH)) {
                c.text(row, DETAIL_X, rowY, MUTED, false);
                rowY += 12;
            }
        } else {
            detail(c, model, picked, DETAIL_X, CONTENT_Y, DETAIL_WIDTH, time);
        }
        if (model.tab() == Tab.PATH) {
            roadmap(c, model, hits, mouseX, mouseY, picked, DETAIL_X, UI_HEIGHT - 74, DETAIL_WIDTH, time);
        }
        return hits;
    }

    /**
     * 4.1.34: the title and the path and mastery on the left; on the right a bar to the next quest that opens with mastery (what the next
     * mastery is for); the mutation points and the side taken as chips on the tabs' row.
     */
    private static void header(UiCanvas c, Model model, double time) {
        QuestLog log = model.log();
        c.push();
        c.translate(16, 11);
        c.scale(1.4F);
        c.text("QUESTS", 0, 0, TITLE, true);
        c.pop();
        c.text(model.path().displayName().toUpperCase(java.util.Locale.ROOT) + "  ·  MASTERY " + model.mastery(), 16, 29, SUBTITLE, false);
        Quest next = null;
        int from = 0;
        for (Quest quest : QuestBook.all()) {
            if (quest.on(model.path()) && quest.line() == Quest.Line.PATH && quest.mastery() > model.mastery()
                && log.state(quest, model.path(), model.mastery()) == QuestLog.State.LOCKED && (next == null || quest.mastery() < next.mastery())) {
                next = quest;
            }
            if (quest.on(model.path()) && quest.line() == Quest.Line.PATH && quest.mastery() <= model.mastery()) {
                from = Math.max(from, quest.mastery());
            }
        }
        int barWidth = 250;
        int barX = UI_WIDTH - 16 - barWidth;
        int barY = 16;
        float share = next == null ? 1.0F : Math.max(0.0F, Math.min(1.0F, (model.mastery() - from) / (float)Math.max(1, next.mastery() - from)));
        Quest shown = next;
        c.batch(() -> {
            UkakuMenuView.capsule(c, barX, barY, barWidth, 3, BAR_BACK);
            UkakuMenuView.capsule(c, barX, barY, Math.max(6, Math.round(barWidth * share)), 3, shown == null ? BAR_DONE : BAR);
            int glint = barX + (int)((time * 90.0) % Math.max(1, Math.round(barWidth * share)));
            c.fill(glint, barY - 1, glint + 5, barY + 2, 0x60FFFFFF);
        });
        String goal = log.finished() ? "Every quest finished, on every kagune"
            : shown == null ? "Every quest of your path is open to you"
            : model.mastery() + " / " + shown.mastery() + "  ·  next: " + shown.title();
        goal = c.fit(goal, barWidth);
        c.text(goal, barX + barWidth - c.width(goal), barY + 8, shown == null ? DONE : MUTED, false);
        int chipsY = 56;
        int chipX = UI_WIDTH - 12;
        int anteiku = log.side(QuestBook.ANTEIKU);
        int pack = log.side(QuestBook.PACK);
        if (anteiku > 0 || pack > 0) {
            String side = anteiku >= pack ? "ANTEIKU" : "THE PACK";
            chipX -= c.width(side) + 12;
            QuestStyle.chip(c, chipX, chipsY, side, 0xFF1E0A0E, QuestStyle.RED_DARK, VALUE);
            chipX -= 4;
        }
        if (log.points() > 0) {
            String points = log.points() + " PT" + (log.points() == 1 ? "" : "S");
            chipX -= c.width(points) + 12;
            QuestStyle.chip(c, chipX, chipsY, points, 0xFF2A1A08, QuestStyle.GOLD_DARK, QuestStyle.GOLD);
        }
    }

    /** 4.1.34: the tabs, each a pill with its count in a badge; the one open lit with a bar under it, the one under the mouse lifted. */
    private static void tabs(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, int x, int y, double time) {
        QuestLog log = model.log();
        int tabX = x;
        for (Tab tab : Tab.values()) {
            int count = tab == Tab.ROMANCE ? (int)model.romance().stream().filter(card -> card.stage().together()).count()
                : (int)listed(model, tab).stream().filter(q -> tab == Tab.DONE || log.state(q, model.path(), model.mastery()) == QuestLog.State.OPEN).count();
            int labelWidth = c.width(tab.label);
            int badge = count > 0 ? c.width(String.valueOf(count)) + 8 : 0;
            int width = labelWidth + 14 + (badge > 0 ? badge + 4 : 0);
            UkakuMenuView.Rect rect = new UkakuMenuView.Rect(tabX, y, width, 16);
            boolean picked = tab == model.tab();
            boolean hover = rect.contains(mouseX, mouseY);
            int lift = hover && !picked ? -1 : 0;
            QuestStyle.box(c, rect.x(), rect.y() + lift, rect.width(), rect.height(), picked ? ROW_PICKED : hover ? ROW_HOVER : ROW,
                picked ? QuestStyle.RED : hover ? QuestStyle.RED_DARK : QuestStyle.TILE_EDGE);
            c.text(tab.label, tabX + 7, y + 4 + lift, picked ? VALUE : hover ? 0xFFD8A8AC : MUTED, false);
            if (badge > 0) {
                boolean urgent = tab == Tab.HUNTS || tab == Tab.PATH;
                int bx = tabX + 10 + labelWidth;
                QuestStyle.box(c, bx, y + 2 + lift, badge, 12, urgent ? QuestStyle.RED_DARK : QuestStyle.GREY_DARK, urgent ? QuestStyle.RED : QuestStyle.GREY);
                c.text(String.valueOf(count), bx + 4, y + 4 + lift, VALUE, false);
            }
            if (picked) {
                float pulse = 0.6F + 0.4F * (float)Math.sin(time * 3.0);
                c.fill(rect.x() + 4, rect.y() + rect.height() + 2, rect.x() + rect.width() - 4, rect.y() + rect.height() + 4,
                    QuestStyle.mix(QuestStyle.RED_DARK, QuestStyle.RED, pulse));
            }
            hits.put("tab:" + tab.name(), rect);
            tabX += width + 4;
        }
    }

    /**
     * 4.1.34: the quests of the tab as cards: each its icon (where it comes from, how it stands), its title, a chip saying where it is, a bar
     * along its foot; the one under the mouse lifted toward it, the one picked lit with a bar down its side; nearly done ones glowing.
     * They slide in as the journal opens.
     */
    private static void list(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, Quest picked, int x, int y,
                             double time) {
        int rowY = y;
        int index = 0;
        int width = LIST_WIDTH;
        int height = 32;
        for (Quest quest : listed(model, model.tab())) {
            if (rowY + height > UI_HEIGHT - 12) {
                c.text("... and more below", x + 8, rowY, FAINT, false);
                break;
            }
            QuestLog.State state = model.log().state(quest, model.path(), model.mastery());
            float in = (float)Math.max(0.0, Math.min(1.0, (time - index * 0.05) / 0.25));
            int slide = Math.round((1.0F - in * in * (3.0F - 2.0F * in)) * -24.0F);
            UkakuMenuView.Rect rect = new UkakuMenuView.Rect(x, rowY, width, height);
            boolean hover = rect.contains(mouseX, mouseY);
            boolean chosen = quest == picked;
            int cx = x + slide + (hover && !chosen ? 3 : 0);
            float share = state == QuestLog.State.DONE ? 1.0F : share(model.log(), quest);
            boolean near = state == QuestLog.State.OPEN && share >= 0.8F;
            int edge = chosen ? QuestStyle.RED : hover ? QuestStyle.RED_DARK : near
                ? QuestStyle.mix(QuestStyle.TILE_EDGE, QuestStyle.GOLD, 0.4F + 0.4F * (float)Math.sin(time * 4.0)) : QuestStyle.TILE_EDGE;
            int fill = chosen ? ROW_PICKED : hover ? ROW_HOVER : ROW;
            int rx = cx;
            int ry = rowY;
            c.batch(() -> {
                QuestStyle.box(c, rx, ry, width, height, fill, edge);
                if (chosen) {
                    c.fill(rx, ry + 3, rx + 3, ry + height - 3, QuestStyle.RED);
                }
                QuestStyle.box(c, rx + 5, ry + 5, 22, 22, QuestStyle.TILE, QuestStyle.dark(state));
                QuestStyle.glyph(c, quest.line(), state, rx + 7, ry + 7, 18, QuestStyle.TILE, time);
            });
            c.text(c.fit(quest.title(), width - 40), cx + 33, rowY + 4, state == QuestLog.State.LOCKED ? MUTED : VALUE, false);
            String chip = stateText(model, quest, state);
            int chipFill = QuestStyle.mix(QuestStyle.dark(state), 0xFF000000, 0.35F);
            QuestStyle.chip(c, cx + 33, rowY + 15, c.fit(chip, width - 90), chipFill, QuestStyle.dark(state), QuestStyle.colour(state));
            if (state == QuestLog.State.OPEN || state == QuestLog.State.DONE) {
                String percent = Math.round(share * 100.0F) + "%";
                c.text(percent, cx + width - 6 - c.width(percent), rowY + 18, state == QuestLog.State.DONE ? DONE : MUTED, false);
            }
            int barY = rowY + height - 3;
            c.batch(() -> {
                c.fill(rx + 33, barY, rx + width - 6, barY + 1, BAR_BACK);
                if (share > 0.0F) {
                    c.fill(rx + 33, barY, rx + 33 + Math.max(2, Math.round((width - 39) * share)), barY + 1,
                        state == QuestLog.State.DONE ? BAR_DONE : BAR);
                }
            });
            hits.put("quest:" + quest.id(), rect);
            rowY += height + 4;
            index++;
        }
    }

    /**
     * 4.1.34: the quest picked: its title large, where it comes from (a chip), its story set off as a quote; its objectives as a checklist
     * (each a box, ticked when met, its count, a bar of segments for a few or a smooth one for many); what it pays and unlocks as badges;
     * its rival way and side; what it waits for, or a stamp when it is done or closed.
     */
    private static void detail(UiCanvas c, Model model, Quest quest, int x, int y, int width, double time) {
        QuestLog log = model.log();
        QuestLog.State state = log.state(quest, model.path(), model.mastery());
        c.push();
        c.translate(x, y);
        c.scale(1.25F);
        c.text(c.fit(quest.title(), Math.round((width - 110) / 1.25F)), 0, 0, QuestStyle.WHITE, true);
        c.pop();
        String line = switch (quest.line()) {
            case PATH -> model.path().displayName() + " path";
            case GHOUL -> "Any ghoul";
            case FAVOUR -> "A favour";
            case HUNT -> "Hunt" + huntClock(model, quest);
        };
        int lineWidth = c.width(c.fit(line, 110)) + 12;
        QuestStyle.chip(c, x + width - lineWidth, y, c.fit(line, 110), 0xFF1E0A0E, QuestStyle.RED_DARK, MUTED);
        int rowY = y + 16;
        String from = QuestBook.from(quest);
        if (!from.isEmpty()) {
            c.text(from, x, rowY, OPEN, false);
            rowY += 13;
        }
        List<String> story = UkakuMenuView.wrap(c, quest.story(), width - 14);
        int storyTop = rowY;
        for (String row : story) {
            c.text(row, x + 10, rowY + 2, 0xFFDCAEB2, false);
            rowY += 12;
        }
        int storyBottom = rowY + 2;
        c.fill(x, storyTop, x + 2, storyBottom, QuestStyle.RED_DARK);
        rowY += 10;
        rowY = section(c, "OBJECTIVES", x, rowY, width);
        for (int i = 0; i < quest.objectives().size(); i++) {
            Objective objective = quest.objectives().get(i);
            int have = state == QuestLog.State.DONE ? objective.count() : log.progress(quest, i);
            boolean met = have >= objective.count();
            int boxY = rowY;
            int ry = rowY;
            c.batch(() -> {
                QuestStyle.box(c, x, boxY, 11, 11, met ? QuestStyle.GREEN_DARK : QuestStyle.TILE, met ? QuestStyle.GREEN : QuestStyle.RED_DARK);
                if (met) {
                    QuestStyle.check(c, x, boxY, 11, QuestStyle.GREEN);
                }
            });
            c.text(c.fit(objective.text(), width - 70), x + 17, ry + 2, met ? DONE : VALUE, false);
            String count = Math.min(have, objective.count()) + " / " + objective.count();
            c.text(count, x + width - c.width(count), ry + 2, met ? DONE : MUTED, false);
            int barX = x + 17;
            int barY = ry + 14;
            int barWidth = width - 17;
            int total = objective.count();
            int got = Math.min(have, total);
            c.batch(() -> {
                if (total <= 12) {
                    int gap = 2;
                    float each = (barWidth - gap * (total - 1)) / (float)total;
                    for (int k = 0; k < total; k++) {
                        int sx = barX + Math.round(k * (each + gap));
                        c.fill(sx, barY, sx + Math.max(1, Math.round(each)), barY + 3, k < got ? (met ? BAR_DONE : BAR) : BAR_BACK);
                    }
                } else {
                    c.fill(barX, barY, barX + barWidth, barY + 3, BAR_BACK);
                    if (got > 0) {
                        c.fill(barX, barY, barX + Math.max(2, Math.round(barWidth * got / (float)total)), barY + 3, met ? BAR_DONE : BAR);
                    }
                }
            });
            rowY += 21;
        }
        rowY += 2;
        rowY = section(c, "REWARDS", x, rowY, width);
        int chipX = x;
        for (String[] reward : rewards(model, quest)) {
            char kind = reward[0].charAt(0);
            int ink = kind == 'k' ? QuestStyle.GOLD : kind == 'p' ? QuestStyle.GOLD : VALUE;
            int fill = kind == 'k' ? 0xFF2A1A08 : 0xFF240A10;
            int edge = kind == 'k' ? QuestStyle.GOLD_DARK : QuestStyle.RED_DARK;
            int w = c.width(reward[1]) + 26;
            if (chipX + w > x + width) {
                chipX = x;
                rowY += 18;
            }
            int bx = chipX;
            int by = rowY;
            c.batch(() -> {
                QuestStyle.box(c, bx, by, w, 15, fill, edge);
                QuestStyle.rewardGlyph(c, kind, bx + 5, by + 2, ink, fill);
            });
            c.text(reward[1], chipX + 19, rowY + 4, ink, false);
            chipX += w + 5;
        }
        rowY += 22;
        if (quest.exclusive() != null) {
            StringBuilder rivals = new StringBuilder();
            for (Quest other : QuestBook.all()) {
                if (other != quest && quest.exclusive().equals(other.exclusive())) {
                    rivals.append(rivals.length() == 0 ? "" : ", ").append(other.title());
                }
            }
            QuestStyle.chip(c, x, rowY, "RIVAL WAY", 0xFF2A1A08, QuestStyle.GOLD_DARK, QuestStyle.GOLD);
            c.text(c.fit(rivals + ": finish one, the other closes", width - 80), x + 76, rowY + 3, MUTED, false);
            rowY += 17;
        }
        if (quest.reward().side() != null) {
            QuestStyle.chip(c, x, rowY, "SIDE", 0xFF1E0A0E, QuestStyle.RED_DARK, VALUE);
            c.text(c.fit(quest.reward().side().equals(QuestBook.ANTEIKU) ? "Anteiku: ghouls who keep the peace will remember"
                : "The Pack: ghouls who want a war will remember", width - 80), x + 76, rowY + 3, MUTED, false);
            rowY += 17;
        }
        rowY += 4;
        if (state == QuestLog.State.LOCKED) {
            StringBuilder needs = new StringBuilder();
            if (model.mastery() < quest.mastery()) {
                needs.append("mastery ").append(quest.mastery());
            }
            for (String before : quest.after()) {
                Quest prior = QuestBook.get(before);
                if (prior != null && log.state(prior, model.path(), model.mastery()) != QuestLog.State.DONE) {
                    needs.append(needs.length() == 0 ? "" : ", then ").append(prior.title());
                }
            }
            int ly = rowY;
            c.batch(() -> QuestStyle.lock(c, x, ly, 12, QuestStyle.GOLD, INNER));
            c.text("Waits for " + (needs.length() == 0 ? "someone to give it" : needs.toString()), x + 17, rowY + 3, FAINT, false);
        } else if (state == QuestLog.State.CLOSED) {
            stamp(c, x + width - c.width("CLOSED") * 2 - 22, rowY - 4, "CLOSED", QuestStyle.GREY, QuestStyle.GREY_DARK);
            c.text("You took the other way.", x, rowY + 3, CLOSED, false);
        } else if (state == QuestLog.State.DONE) {
            stamp(c, x + width - c.width("COMPLETED") * 2 - 22, rowY - 4, "COMPLETED", QuestStyle.GREEN, QuestStyle.GREEN_DARK);
        }
    }

    /** The detail panel's inner colour (behind glyphs drawn on it). */
    static final int INNER = 0xFF0E0507;

    /** 4.1.34: a section's heading and a thin rule after it; the next row's y. */
    private static int section(UiCanvas c, String title, int x, int y, int width) {
        c.text(title, x, y, HEADER, false);
        int after = x + c.width(title) + 8;
        c.fill(after, y + 4, x + width, y + 5, QuestStyle.RED_DARK);
        return y + 15;
    }

    /** 4.1.34: a stamp across the corner: a framed word. */
    private static void stamp(UiCanvas c, int x, int y, String word, int ink, int dark) {
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
    }

    /** 4.1.34: what {@code quest} pays as badges: {kind (m mastery, p points, k unlock, d meat), text}. */
    static List<String[]> rewards(Model model, Quest quest) {
        List<String[]> out = new ArrayList<>();
        Reward reward = quest.reward();
        int mastery = reward.mastery();
        if (reward.points() > 0 && model.path() == KagunePath.RINKAKU) {
            mastery += reward.points() * Reward.RINKAKU_POINT_MASTERY;
        } else if (reward.points() > 0) {
            out.add(new String[] {"p", "+" + reward.points() + " mutation point" + (reward.points() == 1 ? "" : "s")});
        }
        if (mastery > 0) {
            out.add(0, new String[] {"m", "+" + mastery + " mastery"});
        }
        if (reward.ghoulMeat() > 0) {
            out.add(new String[] {"d", reward.ghoulMeat() + " Ghoul Meat"});
        }
        for (Gate gate : reward.gates()) {
            out.add(new String[] {"k", "Unlocks " + gate.displayName()});
        }
        if (quest.line() == Quest.Line.HUNT) {
            for (QuestLog.Hunt hunt : model.log().hunts()) {
                if (hunt.quest().id().equals(quest.id())) {
                    out.clear();
                    out.add(new String[] {"d", Hunts.payText(hunt.spec())});
                }
            }
        }
        if (out.isEmpty()) {
            out.add(new String[] {"k", "Its unlock"});
        }
        return out;
    }

    /**
     * 4.1.34: the path's road: every quest of the path in the order mastery opens them, a node each (done a green tick, open a red beating
     * dot, locked a padlock, closed a cross), joined by a line lit as far as you have come; rival ways stacked as a fork. A node under the
     * mouse names its quest; a click picks it.
     */
    private static void roadmap(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, Quest picked, int x,
                                int y, int width, double time) {
        List<Quest> road = new ArrayList<>();
        for (Quest quest : QuestBook.all()) {
            if (quest.on(model.path()) && quest.line() == Quest.Line.PATH && quest.path() != null) {
                road.add(quest);
            }
        }
        road.sort(java.util.Comparator.comparingInt(Quest::mastery));
        y = section(c, "YOUR ROAD", x, y, width);
        if (road.isEmpty()) {
            c.text("Choose a kagune to set out on its road.", x, y + 4, MUTED, false);
            return;
        }
        // Columns: one per step; rivals (one exclusive) share a column, stacked.
        List<List<Quest>> columns = new ArrayList<>();
        Map<String, List<Quest>> rivals = new LinkedHashMap<>();
        for (Quest quest : road) {
            if (quest.exclusive() != null && rivals.containsKey(quest.exclusive())) {
                rivals.get(quest.exclusive()).add(quest);
                continue;
            }
            List<Quest> column = new ArrayList<>();
            column.add(quest);
            columns.add(column);
            if (quest.exclusive() != null) {
                rivals.put(quest.exclusive(), column);
            }
        }
        int n = columns.size();
        int step = n <= 1 ? 0 : Math.min(64, (width - 20) / (n - 1));
        int midY = y + 20;
        String hover = null;
        int hoverX = 0;
        int hoverY = 0;
        int reached = -1;
        for (int i = 0; i < n; i++) {
            for (Quest quest : columns.get(i)) {
                if (model.log().state(quest, model.path(), model.mastery()) != QuestLog.State.LOCKED) {
                    reached = i;
                }
            }
        }
        int lit = reached;
        int last = n - 1;
        c.batch(() -> {
            c.fill(x + 10, midY - 1, x + 10 + step * last, midY + 1, BAR_BACK);
            if (lit > 0) {
                c.fill(x + 10, midY - 1, x + 10 + step * lit, midY + 1, BAR);
            }
        });
        for (int i = 0; i < n; i++) {
            List<Quest> column = columns.get(i);
            int cx = x + 10 + step * i;
            for (int k = 0; k < column.size(); k++) {
                Quest quest = column.get(k);
                int cy = column.size() == 1 ? midY : midY + (k == 0 ? -9 : 9);
                if (column.size() > 1 && k > 1) {
                    cy = midY + 9 * k;
                }
                QuestLog.State state = model.log().state(quest, model.path(), model.mastery());
                UkakuMenuView.Rect rect = new UkakuMenuView.Rect(cx - 8, cy - 8, 16, 16);
                boolean over = rect.contains(mouseX, mouseY);
                boolean chosen = quest == picked;
                int ny = cy;
                float beat = state == QuestLog.State.OPEN ? 0.5F + 0.5F * (float)Math.sin(time * 5.0 + i) : 0.0F;
                c.batch(() -> {
                    if (column.size() > 1) {
                        QuestStyle.line(c, cx, midY, cx, ny, 2.0F, BAR_BACK);
                    }
                    if (chosen || over) {
                        QuestStyle.circle(c, cx, ny, 9, chosen ? QuestStyle.RED : QuestStyle.RED_DARK);
                    }
                    QuestStyle.ring(c, cx, ny, 7, 2, QuestStyle.mix(QuestStyle.dark(state), QuestStyle.colour(state), 0.5F + 0.5F * beat), QuestStyle.TILE);
                    QuestStyle.glyph(c, Quest.Line.PATH, state, cx - 4, ny - 4, 9, QuestStyle.TILE, time);
                });
                hits.put("quest:" + quest.id(), rect);
                if (over) {
                    hover = quest.title() + "  \u00b7  " + stateText(model, quest, state);
                    hoverX = cx;
                    hoverY = ny;
                }
            }
            String label = column.get(0).mastery() > 0 ? String.valueOf(column.get(0).mastery()) : "-";
            c.centered(label, cx, midY + (column.size() > 1 ? 19 : 11), FAINT, false);
        }
        if (hover != null) {
            int w = c.width(hover) + 12;
            int tx = Math.max(x, Math.min(x + width - w, hoverX - w / 2));
            int ty = hoverY - 26;
            String text = hover;
            c.batch(() -> QuestStyle.box(c, tx, ty, w, 15, 0xF0100406, QuestStyle.RED));
            c.text(text, tx + 6, ty + 4, VALUE, false);
        }
    }

    /**
     * 1.3.29: the Romance tab: each companion on the left (who, the stage), the one picked on the right: the stage, the regard, the errands,
     * days together, the home, the children, and what to do next.
     */
    private static void romance(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, int x, int y) {
        List<com.hex.kagune.control.npc.RomanceCard> cards = model.romance();
        if (cards.isEmpty()) {
            c.text("Nothing known yet.", DETAIL_X, y + 4, MUTED, false);
            return;
        }
        com.hex.kagune.control.npc.RomanceCard picked = cards.get(0);
        for (com.hex.kagune.control.npc.RomanceCard card : cards) {
            if (("romance:" + card.key()).equals(model.picked())) {
                picked = card;
            }
        }
        int rowY = y;
        for (com.hex.kagune.control.npc.RomanceCard card : cards) {
            UkakuMenuView.Rect rect = new UkakuMenuView.Rect(x, rowY, LIST_WIDTH, 28);
            boolean hover = rect.contains(mouseX, mouseY);
            c.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), card == picked ? ROW_PICKED : hover ? ROW_HOVER : ROW);
            c.text(card.persona().name(), x + 8, rowY + 4, card.met() ? VALUE : MUTED, false);
            c.text(c.fit(card.stageText(), 140), x + 8, rowY + 16, card.stage().together() ? DONE : card.met() ? OPEN : FAINT, false);
            if (card.met()) {
                float share = card.regardShare();
                int barX = x + 160;
                int barY = rowY + 11;
                c.batch(() -> {
                    UkakuMenuView.capsule(c, barX, barY, 44, 3, BAR_BACK);
                    UkakuMenuView.capsule(c, barX, barY, Math.max(3, Math.round(44 * share)), 3, card.regard() < 0 ? BAR : BAR_DONE);
                });
            }
            hits.put("romance:" + card.key(), rect);
            rowY += 31;
        }
        int dx = DETAIL_X;
        int width = DETAIL_WIDTH;
        int dy = y;
        com.hex.kagune.control.npc.RomanceCard card = picked;
        c.text(card.persona().name().toUpperCase(java.util.Locale.ROOT), dx, dy, HEADER, false);
        String stage = card.stageText();
        c.text(stage, dx + width - c.width(stage), dy, card.stage().together() ? DONE : MUTED, false);
        dy += 20;
        if (!card.met()) {
            c.text(card.next(), dx, dy, VALUE, false);
            return;
        }
        c.text("REGARD", dx, dy, HEADER, false);
        String regard = card.regard() + " (" + com.hex.kagune.control.npc.NpcBonds.band(card.regard()).label() + ")";
        c.text(regard, dx + LABEL_WIDTH, dy, VALUE, false);
        int barY = dy + 12;
        float share = card.regardShare();
        int barX = dx;
        c.batch(() -> {
            UkakuMenuView.capsule(c, barX, barY, width, 3, BAR_BACK);
            UkakuMenuView.capsule(c, barX, barY, Math.max(3, Math.round(width * share)), 3, card.regard() < 0 ? BAR : BAR_DONE);
        });
        dy += 24;
        if (!card.stage().together() && card.likes()) {
            c.text("COURTING", dx, dy, HEADER, false);
            c.text(c.fit(card.errands(), width - LABEL_WIDTH), dx + LABEL_WIDTH, dy, VALUE, false);
            dy += 16;
        }
        if (card.stage().together()) {
            c.text("TOGETHER", dx, dy, HEADER, false);
            c.text(card.days() + (card.days() == 1L ? " day" : " days") + (card.stage() == com.hex.kagune.control.npc.Romance.Stage.MARRIED ? " married" : " dating"),
                dx + LABEL_WIDTH, dy, VALUE, false);
            dy += 16;
        }
        if (card.stage() == com.hex.kagune.control.npc.Romance.Stage.MARRIED) {
            c.text("HOME", dx, dy, HEADER, false);
            c.text(card.home() ? "Set: " + card.persona().she() + " keeps house there by day" : "Not set yet", dx + LABEL_WIDTH, dy, card.home() ? DONE : MUTED, false);
            dy += 16;
            c.text("CHILDREN", dx, dy, HEADER, false);
            c.text(card.children() + (card.expecting() ? ", one on the way" : ""), dx + LABEL_WIDTH, dy, VALUE, false);
            dy += 16;
        }
        dy += 6;
        c.text("NEXT", dx, dy, HEADER, false);
        dy += 14;
        for (String row : UkakuMenuView.wrap(c, card.next(), width)) {
            c.text(row, dx, dy, 0xFFDCAEB2, false);
            dy += 12;
        }
    }

    /** The path's gates still shut, and the quests that open each. */
    static String stateText(Model model, Quest quest, QuestLog.State state) {
        return switch (state) {
            case OPEN -> {
                int met = 0;
                for (int i = 0; i < quest.objectives().size(); i++) {
                    met += model.log().progress(quest, i) >= quest.objectives().get(i).count() ? 1 : 0;
                }
                yield quest.line() == Quest.Line.HUNT ? "on" + huntClock(model, quest) : met + " of " + quest.objectives().size() + " done";
            }
            case DONE -> "done";
            case CLOSED -> "closed: you took the other way";
            default -> com.hex.kagune.control.npc.EtoRules.lessonFlag(quest.id()) != 0
                && !model.log().bond(com.hex.kagune.control.npc.EtoRules.KEY).has(com.hex.kagune.control.npc.EtoRules.lessonFlag(quest.id()))
                ? "Eto teaches it at the Library" : model.mastery() < quest.mastery() ? "at mastery " + quest.mastery() : "after another quest";
        };
    }

    /** How far along {@code quest} is, all objectives together (0 to 1). */
    static float share(QuestLog log, Quest quest) {
        float total = 0.0F;
        for (int i = 0; i < quest.objectives().size(); i++) {
            total += Math.min(1.0F, log.progress(quest, i) / (float)quest.objectives().get(i).count());
        }
        return quest.objectives().isEmpty() ? 0.0F : total / quest.objectives().size();
    }

    /** ", until dawn (about N min)" for a hunt on. */
    static String huntClock(Model model, Quest quest) {
        for (QuestLog.Hunt hunt : model.log().hunts()) {
            if (hunt.quest().id().equals(quest.id())) {
                long left = Math.max(0L, hunt.endsAt() - model.time());
                return ", until dawn (about " + Math.max(1L, Math.round(left / 1200.0)) + " min)";
            }
        }
        return "";
    }

    static String pays(Model model, Quest quest) {
        Reward reward = quest.reward();
        List<String> parts = new ArrayList<>();
        int mastery = reward.mastery();
        if (reward.points() > 0 && model.path() == KagunePath.RINKAKU) {
            mastery += reward.points() * Reward.RINKAKU_POINT_MASTERY;
        } else if (reward.points() > 0) {
            parts.add(reward.points() + " mutation point" + (reward.points() == 1 ? "" : "s"));
        }
        if (mastery > 0) {
            parts.add(0, mastery + " mastery");
        }
        if (reward.ghoulMeat() > 0) {
            parts.add(reward.ghoulMeat() + " Ghoul Meat");
        }
        if (quest.line() == Quest.Line.HUNT) {
            for (QuestLog.Hunt hunt : model.log().hunts()) {
                if (hunt.quest().id().equals(quest.id())) {
                    return Hunts.payText(hunt.spec());
                }
            }
        }
        return parts.isEmpty() ? "its unlock" : String.join(", ", parts);
    }
}
