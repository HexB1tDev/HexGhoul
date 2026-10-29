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
    public static final int UI_WIDTH = AyatoMenuView.UI_WIDTH;
    public static final int UI_HEIGHT = AyatoMenuView.UI_HEIGHT;

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

    /** The tabs: the path's quests, any ghoul's, the hunts on, what is over. */
    public enum Tab {
        PATH("YOUR PATH"), GHOUL("THE GHOUL'S WAY"), HUNTS("HUNTS"), DONE("DONE");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    /** What the journal shows: whose it is, the log, the game time (a hunt's clock), the tab and the quest picked (null: the first). */
    public record Model(KagunePath path, int mastery, QuestLog log, long time, Tab tab, String picked) {
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
            boolean over = state == QuestLog.State.DONE || state == QuestLog.State.CLOSED;
            boolean fits = switch (tab) {
                case PATH -> !over && quest.line() == Quest.Line.PATH;
                case GHOUL -> !over && (quest.line() == Quest.Line.GHOUL || quest.line() == Quest.Line.FAVOUR);
                case HUNTS -> !over && quest.line() == Quest.Line.HUNT;
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
    public static Map<String, AyatoMenuView.Rect> draw(UiCanvas c, Model model, double mouseX, double mouseY, double time) {
        Map<String, AyatoMenuView.Rect> hits = new LinkedHashMap<>();
        int panelWidth = 930;
        int panelHeight = 500;
        int centerX = UI_WIDTH / 2;
        int centerY = UI_HEIGHT / 2;
        int left = centerX - panelWidth / 2;
        int top = centerY - panelHeight / 2;
        c.batch(() -> {
            AyatoMenuView.organicPanel(c, centerX, centerY, panelWidth / 2, panelHeight / 2, FurutaMenuView.PANEL_FILL, FurutaMenuView.PANEL_EDGE);
            AyatoMenuView.organicPanel(c, centerX, centerY, panelWidth / 2 - 5, panelHeight / 2 - 5, FurutaMenuView.INNER_FILL, FurutaMenuView.INNER_EDGE);
            AyatoMenuView.drawVein(c, left + 18, top + 74, centerX, top + 32, left + panelWidth - 18, top + 74, FurutaMenuView.VEIN_BRIGHT, 2);
            AyatoMenuView.drawVein(c, left + 342, top + 96, left + 352, centerY, left + 338, top + panelHeight - 30, FurutaMenuView.VEIN, 2);
        });
        QuestLog log = model.log();
        c.centered("Q U E S T S", centerX, top + 18, TITLE);
        StringBuilder sub = new StringBuilder(model.path().displayName().toUpperCase(java.util.Locale.ROOT)).append("  /  MASTERY ").append(model.mastery());
        if (log.points() > 0) {
            sub.append("  /  ").append(log.points()).append(" MUTATION POINT").append(log.points() == 1 ? "" : "S").append(" FROM QUESTS");
        }
        int anteiku = log.side(QuestBook.ANTEIKU);
        int pack = log.side(QuestBook.PACK);
        if (anteiku > 0 || pack > 0) {
            sub.append("  /  ").append(anteiku >= pack ? "ANTEIKU'S SIDE" : "THE PACK'S SIDE");
        }
        c.centered(sub.toString(), centerX, top + 34, SUBTITLE);
        c.centered("Mastery opens a quest; the quest opens the transformation. Some ways close others.", centerX, top + 51, 0xFF94505A);

        // The tabs.
        int tabX = left + 24;
        for (Tab tab : Tab.values()) {
            int count = (int)listed(model, tab).stream().filter(q -> tab == Tab.DONE || log.state(q, model.path(), model.mastery()) == QuestLog.State.OPEN).count();
            String label = tab.label + (count > 0 ? "  " + count : "");
            int width = c.width(label) + 16;
            AyatoMenuView.Rect rect = new AyatoMenuView.Rect(tabX, top + 70, width, 16);
            boolean picked = tab == model.tab();
            boolean hover = rect.contains(mouseX, mouseY);
            c.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), picked ? ROW_PICKED : hover ? ROW_HOVER : ROW);
            c.text(label, tabX + 8, top + 74, picked ? VALUE : tab == Tab.HUNTS && count > 0 ? OPEN : MUTED, false);
            hits.put("tab:" + tab.name(), rect);
            tabX += width + 6;
        }

        Quest picked = picked(model);
        list(c, model, hits, mouseX, mouseY, picked, left + 24, top + 96);
        if (picked == null) {
            c.text(model.tab() == Tab.HUNTS ? "No hunt tonight. Hunts come at dusk, now and then, and end at dawn." : "Nothing here yet.", left + 360,
                top + 100, MUTED, false);
        } else {
            detail(c, model, picked, left + 360, top + 96, 545);
        }
        if (model.tab() == Tab.PATH) {
            gates(c, model, left + 360, top + 372, 545);
        }
        return hits;
    }

    private static void list(UiCanvas c, Model model, Map<String, AyatoMenuView.Rect> hits, double mouseX, double mouseY, Quest picked, int x, int y) {
        int rowY = y;
        for (Quest quest : listed(model, model.tab())) {
            if (rowY > y + 360) {
                break;
            }
            QuestLog.State state = model.log().state(quest, model.path(), model.mastery());
            AyatoMenuView.Rect rect = new AyatoMenuView.Rect(x, rowY, 310, 28);
            boolean hover = rect.contains(mouseX, mouseY);
            c.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), quest == picked ? ROW_PICKED : hover ? ROW_HOVER : ROW);
            int colour = switch (state) {
                case OPEN -> OPEN;
                case DONE -> DONE;
                case CLOSED -> CLOSED;
                default -> FAINT;
            };
            c.text(c.fit(quest.title(), 230), x + 8, rowY + 4, state == QuestLog.State.LOCKED ? MUTED : VALUE, false);
            c.text(stateText(model, quest, state), x + 8, rowY + 16, colour, false);
            float share = state == QuestLog.State.DONE ? 1.0F : share(model.log(), quest);
            int barX = x + 240;
            int barY = rowY + 11;
            c.batch(() -> {
                AyatoMenuView.capsule(c, barX, barY, 60, 3, BAR_BACK);
                if (share > 0.0F) {
                    AyatoMenuView.capsule(c, barX, barY, Math.max(3, Math.round(60 * share)), 3, state == QuestLog.State.DONE ? BAR_DONE : BAR);
                }
            });
            hits.put("quest:" + quest.id(), rect);
            rowY += 31;
        }
    }

    private static void detail(UiCanvas c, Model model, Quest quest, int x, int y, int width) {
        QuestLog log = model.log();
        QuestLog.State state = log.state(quest, model.path(), model.mastery());
        c.text(quest.title().toUpperCase(java.util.Locale.ROOT), x, y, HEADER, false);
        String line = switch (quest.line()) {
            case PATH -> model.path().displayName() + " path";
            case GHOUL -> "Any ghoul";
            case FAVOUR -> "A favour";
            case HUNT -> "Hunt" + huntClock(model, quest);
        };
        c.text(line, x + width - c.width(line), y, MUTED, false);
        int rowY = y + 18;
        for (String row : AyatoMenuView.wrap(c, quest.story(), width)) {
            c.text(row, x, rowY, 0xFFDCAEB2, false);
            rowY += 12;
        }
        rowY += 8;
        c.text("OBJECTIVES", x, rowY, HEADER, false);
        rowY += 16;
        for (int i = 0; i < quest.objectives().size(); i++) {
            Objective objective = quest.objectives().get(i);
            int have = state == QuestLog.State.DONE ? objective.count() : log.progress(quest, i);
            boolean met = have >= objective.count();
            c.text(c.fit(objective.text(), width - 150), x, rowY, met ? DONE : VALUE, false);
            String count = have + " / " + objective.count();
            c.text(count, x + width - c.width(count), rowY, met ? DONE : MUTED, false);
            int barY = rowY + 12;
            float share = Math.min(1.0F, have / (float)objective.count());
            c.batch(() -> {
                AyatoMenuView.capsule(c, x, barY, width, 3, BAR_BACK);
                if (share > 0.0F) {
                    AyatoMenuView.capsule(c, x, barY, Math.max(3, Math.round(width * share)), 3, met ? BAR_DONE : BAR);
                }
            });
            rowY += 24;
        }
        rowY += 4;
        c.text("PAYS", x, rowY, HEADER, false);
        c.text(pays(model, quest), x + LABEL_WIDTH, rowY, VALUE, false);
        rowY += 16;
        if (!quest.reward().gates().isEmpty()) {
            StringBuilder opens = new StringBuilder();
            for (Gate gate : quest.reward().gates()) {
                opens.append(opens.length() == 0 ? "" : ", ").append(gate.displayName());
            }
            c.text("UNLOCKS", x, rowY, HEADER, false);
            c.text(c.fit(opens.toString(), width - LABEL_WIDTH), x + LABEL_WIDTH, rowY, OPEN, false);
            rowY += 16;
        }
        if (quest.exclusive() != null) {
            StringBuilder rivals = new StringBuilder();
            for (Quest other : QuestBook.all()) {
                if (other != quest && quest.exclusive().equals(other.exclusive())) {
                    rivals.append(rivals.length() == 0 ? "" : ", ").append(other.title());
                }
            }
            c.text("RIVAL WAY", x, rowY, HEADER, false);
            c.text(c.fit(rivals + ": whichever you finish first closes the other", width - LABEL_WIDTH), x + LABEL_WIDTH, rowY, MUTED, false);
            rowY += 16;
        }
        if (quest.reward().side() != null) {
            c.text("SIDE", x, rowY, HEADER, false);
            c.text(quest.reward().side().equals(QuestBook.ANTEIKU) ? "Anteiku: ghouls who keep the peace will remember"
                : "The Pack: ghouls who want a war will remember", x + LABEL_WIDTH, rowY, MUTED, false);
            rowY += 16;
        }
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
            c.text("WAITS FOR", x, rowY, HEADER, false);
            c.text(needs.length() == 0 ? "-" : needs.toString(), x + LABEL_WIDTH, rowY, FAINT, false);
        } else if (state == QuestLog.State.CLOSED) {
            c.text("Closed: you took the other way.", x, rowY, CLOSED, false);
        } else if (state == QuestLog.State.DONE) {
            c.text("Done.", x, rowY, DONE, false);
        }
    }

    /** The path's gates still shut, and the quests that open each. */
    private static void gates(UiCanvas c, Model model, int x, int y, int width) {
        List<Gate> shut = new ArrayList<>();
        for (Gate gate : Gate.values()) {
            if (gate.on(model.path()) && !model.log().open(gate)) {
                shut.add(gate);
            }
        }
        c.text("STILL LOCKED", x, y, HEADER, false);
        if (shut.isEmpty()) {
            c.text("Nothing: every gate on your way is open.", x + 100, y, DONE, false);
            return;
        }
        int rowY = y + 14;
        for (Gate gate : shut) {
            if (rowY > y + 74) {
                c.text("...", x, rowY, FAINT, false);
                break;
            }
            String text = gate.displayName() + ": " + QuestBook.openersText(gate);
            c.text(c.fit(text, width), x, rowY, MUTED, false);
            rowY += 12;
        }
    }

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
            case CLOSED -> "closed: the other way";
            default -> model.mastery() < quest.mastery() ? "at mastery " + quest.mastery() : "after another quest";
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
        if (reward.points() > 0 && model.path() == KagunePath.KANEKI) {
            mastery += reward.points() * Reward.KANEKI_POINT_MASTERY;
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
