package com.hex.kagune.control.client;

import com.hex.kagune.control.client.ui.UiCanvas;
import com.hex.kagune.control.kitchen.Cookbook;
import com.hex.kagune.control.kitchen.Dish;
import com.hex.kagune.control.kitchen.Dishes;
import com.hex.kagune.control.kitchen.Menu;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 4.1.66: the ghoul cookbook (Home Living, 1.3.41) as the quest menu (the owner: "new Menus: Every single menu in the game is to mimic and
 * look like the quest menu, and be as interactable as it is, Dialog, mastery menus ALL OF THEM"), pure ({@link UiCanvas}) so it renders
 * outside the game: the box, its title; a tab per chapter and one for the kitchen's own effects; a chapter's picture and words, its dishes
 * as slots to click; the dish picked as a page: its picture on the kitchen table, what it is, its recipe in slots with the real items
 * (their tooltips under the mouse), what it fills, what is in it, what it gives. The effects' tab: each effect as a card, what it does, and
 * the dishes that give it (a click goes to the dish).
 */
public final class CookbookView {
    public static final int BOX_WIDTH = UkakuMenuView.BOX_WIDTH;
    public static final int BOX_HEIGHT = UkakuMenuView.BOX_HEIGHT;
    static final int DETAIL_WIDTH = UkakuMenuView.DETAIL_WIDTH;
    static final int TILE_WIDTH = UkakuMenuView.TILE_WIDTH;
    static final int TILE_HEIGHT = UkakuMenuView.TILE_HEIGHT;
    /** The effects' tab. */
    public static final String EFFECTS = "effects";
    static final int SLOT = 38;
    static final int SLOT_GAP = 5;
    static final int PER_ROW = 5;
    static final int GHOUL = 0xFFE8283C;

    /** What the book shows: the tab open (a chapter's id, or {@link #EFFECTS}) and what is picked ("pick:d:ID" a dish, "pick:e:KEY" an effect). */
    public record Model(String tab, String picked) {
        public Model {
            tab = tab == null ? Menu.CHAPTERS[0] : tab;
        }
    }

    private CookbookView() {
    }

    /** The tabs in order: every chapter, then the effects. */
    public static List<String> tabs() {
        List<String> tabs = new ArrayList<>(List.of(Menu.CHAPTERS));
        tabs.add(EFFECTS);
        return tabs;
    }

    /**
     * Draws the box at its own size and returns its clickable areas: "tab:ID", "pick:d:ID" (a dish), "pick:e:KEY" (an effect), "goto:ID" (a
     * dish that gives the effect shown: its chapter, it picked).
     */
    public static Map<String, UkakuMenuView.Rect> draw(UiCanvas c, Model model, double mouseX, double mouseY, double time) {
        Map<String, UkakuMenuView.Rect> hits = new LinkedHashMap<>();
        QuestFrame.plate(c, BOX_WIDTH, BOX_HEIGHT, time, 292, 6, 170, 38);
        int dishes = 0;
        for (String chapter : Menu.CHAPTERS) {
            dishes += Dishes.chapter(chapter).size();
        }
        QuestFrame.header(c, BOX_WIDTH, c.tr("book.hexkagune.cookbook.title"), c.tr("book.hexkagune.cookbook.subtitle").toUpperCase(java.util.Locale.ROOT),
            1.0F, true, dishes + " dishes in " + Menu.CHAPTERS.length + " chapters", time);
        List<QuestFrame.Tab> tabs = new ArrayList<>();
        for (String tab : tabs()) {
            tabs.add(new QuestFrame.Tab(tab, tab.toUpperCase(java.util.Locale.ROOT), 0, false));
        }
        QuestFrame.tabs(c, tabs, model.tab(), hits, mouseX, mouseY, QuestFrame.LIST_X, QuestFrame.TABS_Y, time);
        String[] tip = {null};
        if (EFFECTS.equals(model.tab())) {
            effectsTab(c, model, hits, mouseX, mouseY, time);
        } else {
            chapterTab(c, model, hits, mouseX, mouseY, time, tip);
        }
        if (tip[0] != null) {
            c.itemTooltip(tip[0], (int)mouseX, (int)mouseY);
        }
        return hits;
    }

    // ------------------------------------------------------------------ a chapter

    /** The dish shown: the one picked if it is this chapter's, else its first. */
    static Dish pickedDish(Model model) {
        List<Dish> dishes = Dishes.chapter(model.tab());
        if (model.picked() != null && model.picked().startsWith("pick:d:")) {
            for (Dish dish : dishes) {
                if (model.picked().equals("pick:d:" + dish.id())) {
                    return dish;
                }
            }
        }
        return dishes.isEmpty() ? null : dishes.get(0);
    }

    private static void chapterTab(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, double time,
                                   String[] tip) {
        String chapter = model.tab();
        int x = QuestFrame.LIST_X;
        int y = QuestFrame.CONTENT_Y;
        int w = QuestFrame.LIST_WIDTH;
        // The chapter's picture in a tile, its name and its words beside it.
        c.batch(() -> QuestStyle.box(c, x, y, 66, 66, QuestStyle.TILE, QuestStyle.TILE_EDGE));
        c.texture("hexkagune:textures/gui/cookbook/" + chapter + ".png", x + 1, y + 1, 64, 64, 0, 0, 48, 48, 48, 48);
        int tx = x + 74;
        int ty = y + 2;
        for (String line : Cookbook.wrap(c.tr("book.hexkagune.cookbook.chapter." + chapter), w - 74, c::width)) {
            c.text(line, tx, ty, QuestStyle.WHITE, true);
            ty += 11;
        }
        ty += 2;
        for (String line : Cookbook.wrap(c.tr("book.hexkagune.cookbook.chapter." + chapter + ".intro"), w - 74, c::width)) {
            if (ty + 8 <= y + 66) {
                c.text(line, tx, ty, QuestFrame.MUTED, false);
            }
            ty += 10;
        }
        List<Dish> dishes = Dishes.chapter(chapter);
        Dish picked = pickedDish(model);
        int gy = QuestFrame.section(c, c.tr("book.hexkagune.cookbook.in_chapter").toUpperCase(java.util.Locale.ROOT), String.valueOf(dishes.size()), x, y + 74, w);
        String named = null;
        for (int i = 0; i < dishes.size(); i++) {
            Dish dish = dishes.get(i);
            int sx = x + (i % PER_ROW) * (SLOT + SLOT_GAP);
            int sy = gy + (i / PER_ROW) * (SLOT + SLOT_GAP);
            UkakuMenuView.Rect slot = new UkakuMenuView.Rect(sx, sy, SLOT, SLOT);
            boolean over = slot.contains(mouseX, mouseY);
            boolean chosen = dish == picked;
            int edge = chosen ? QuestStyle.RED : over ? QuestStyle.RED_DARK : QuestStyle.TILE_EDGE;
            int fill = chosen ? QuestJournalView.ROW_PICKED : over ? QuestJournalView.ROW_HOVER : QuestJournalView.ROW;
            c.batch(() -> {
                QuestStyle.box(c, sx, sy, SLOT, SLOT, fill, edge);
                if (chosen) {
                    float pulse = 0.6F + 0.4F * (float)Math.sin(time * 4.0);
                    c.fill(sx + 3, sy + SLOT - 3, sx + SLOT - 3, sy + SLOT - 2, QuestStyle.mix(QuestStyle.RED_DARK, QuestStyle.RED, pulse));
                }
            });
            c.item(dish.id(), 1, sx + 3, sy + 3, 32);
            hits.put("pick:d:" + dish.id(), slot);
            if (over) {
                named = c.tr("item.hexkagune." + dish.id());
            }
        }
        int rows = (dishes.size() + PER_ROW - 1) / PER_ROW;
        int footY = gy + rows * (SLOT + SLOT_GAP) + 2;
        c.text(c.fit(named != null ? named : c.tr("book.hexkagune.cookbook.pick"), w), x, footY, named != null ? QuestFrame.VALUE : QuestFrame.FAINT, false);
        if (picked != null) {
            dishPage(c, picked, hits, mouseX, mouseY, QuestFrame.DETAIL_X, QuestFrame.CONTENT_Y, DETAIL_WIDTH, time, tip);
        }
    }

    /** What kind of dish it is, as its chip says it. */
    static String kind(Dish dish) {
        return switch (dish.kind()) {
            case BOWL -> "A BOWL";
            case DRINK -> "A DRINK";
            case SNACK -> "A SNACK";
            default -> "A PLATE";
        };
    }

    /** A dish, as the journal shows a quest: its words, its picture on the table, its recipe, what it fills, what is in it, what it gives. */
    private static void dishPage(UiCanvas c, Dish dish, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, int x, int y, int width,
                                 double time, String[] tip) {
        title(c, c.tr("item.hexkagune." + dish.id()), kind(dish), x, y, width);
        int rowY = y + 16;
        c.text(c.fit(c.tr("book.hexkagune.cookbook.chapter." + dish.chapter()) + (dish.makes() > 1 ? "  ·  makes " + dish.makes() : ""), width), x, rowY,
            QuestJournalView.DONE, false);
        rowY += 13;
        String about = c.tr("item.hexkagune." + dish.id() + ".about").replaceAll("§.", "");
        rowY = QuestFrame.story(c, about, x, rowY, width) + 8;
        // Its picture: the kitchen table, the dish on it four times its size.
        int tileY = rowY;
        c.batch(() -> QuestStyle.box(c, x, tileY, TILE_WIDTH, TILE_HEIGHT, QuestStyle.TILE, QuestStyle.GREEN_DARK));
        c.texture("hexkagune:textures/gui/cookbook/backdrop.png", x + 2, tileY + 2, TILE_WIDTH - 4, TILE_HEIGHT - 4, 0, 0, 48, 48, 48, 48);
        c.item(dish.id(), 1, x + TILE_WIDTH / 2 - 32, tileY + TILE_HEIGHT / 2 - 40, 64);
        // Beside it: the recipe, what it fills, what is in it.
        int rx = x + TILE_WIDTH + 12;
        int rw = width - TILE_WIDTH - 12;
        int ry = QuestFrame.section(c, "THE RECIPE", rx, tileY, rw);
        if (dish.recipe() == null) {
            for (String line : Cookbook.wrap(c.tr("book.hexkagune.cookbook.hunted"), rw, c::width)) {
                c.text(line, rx, ry, QuestFrame.MUTED, false);
                ry += 10;
            }
            ry += 4;
        } else if (dish.recipe().cooking()) {
            slot(c, dish.recipe().ingredients()[0], 1, rx, ry, mouseX, mouseY, tip);
            c.text("->", rx + 24, ry + 5, QuestFrame.MUTED, false);
            slot(c, dish.id(), 1, rx + 40, ry, mouseX, mouseY, tip);
            c.text(c.fit(c.tr("book.hexkagune.cookbook.seared"), rw - 64), rx + 64, ry + 5, QuestFrame.FAINT, false);
            ry += 24;
        } else {
            String[] parts = dish.recipe().ingredients();
            int perRow = 5;
            int rows = (parts.length + perRow - 1) / perRow;
            for (int i = 0; i < parts.length; i++) {
                slot(c, parts[i], 1, rx + (i % perRow) * 20, ry + (i / perRow) * 20, mouseX, mouseY, tip);
            }
            int ax = rx + Math.min(perRow, parts.length) * 20 + 4;
            int ay = ry + (rows * 20) / 2 - 9;
            c.text("->", ax, ay + 5, QuestFrame.MUTED, false);
            slot(c, dish.id(), dish.makes(), ax + 16, ay, mouseX, mouseY, tip);
            ry += rows * 20 + 6;
        }
        ry = QuestFrame.section(c, c.tr("book.hexkagune.cookbook.fills").toUpperCase(java.util.Locale.ROOT), rx, ry, rw);
        int shanks = (dish.nutrition() + 1) / 2;
        for (int i = 0; i < shanks; i++) {
            shank(c, rx + i * 10, ry - 1, i == shanks - 1 && dish.nutrition() % 2 == 1);
        }
        c.text(dish.nutrition() / 2.0F == dish.nutrition() / 2 ? dish.nutrition() / 2 + " shanks" : dish.nutrition() / 2.0F + " shanks",
            rx + shanks * 10 + 6, ry, QuestFrame.MUTED, false);
        ry += 13;
        if (dish.ghoulMeat() > 0) {
            c.text(c.fit(dish.ghoulMeat() == 1 ? c.tr("book.hexkagune.cookbook.ghoul_meat") : c.tr("book.hexkagune.cookbook.ghoul_meat_pieces",
                dish.ghoulMeat()), rw), rx, ry, GHOUL, false);
            ry += 11;
        }
        if (dish.humanMeat()) {
            c.text(c.fit(c.tr("book.hexkagune.cookbook.human_meat"), rw), rx, ry, QuestStyle.RED, false);
        }
        // Under it: what it gives.
        int gy = QuestFrame.section(c, "WHAT IT GIVES", dish.effects().length == 0 ? "nothing more" : String.valueOf(dish.effects().length), x,
            tileY + TILE_HEIGHT + 8, width);
        int half = (width - 16) / 2;
        for (int i = 0; i < dish.effects().length; i++) {
            Dish.Effect effect = dish.effects()[i];
            int ex = x + (i % 2) * (half + 16);
            int ey = gy + (i / 2) * 16;
            effectIcon(c, effect.key(), ex, ey - 2, 12);
            String name = c.tr((Dishes.kitchenEffect(effect.key()) ? "effect.hexkagune." : "effect.minecraft.") + effect.key()) + Dishes.level(effect.amplifier());
            String length = Dishes.time(effect.ticks());
            c.text(c.fit(name, half - 22 - c.width(length)), ex + 16, ey, QuestFrame.VALUE, false);
            c.text(length, ex + half - c.width(length), ey, QuestFrame.MUTED, false);
        }
    }

    /** A slot with an item in it (its count), lit under the mouse; under the mouse it asks for the item's tooltip. */
    private static void slot(UiCanvas c, String id, int count, int x, int y, double mouseX, double mouseY, String[] tip) {
        boolean over = mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
        c.batch(() -> QuestStyle.box(c, x, y, 18, 18, over ? QuestJournalView.ROW_HOVER : QuestStyle.TILE, over ? QuestStyle.RED : QuestStyle.TILE_EDGE));
        c.item(id, count, x + 1, y + 1, 16);
        if (over) {
            tip[0] = id;
        }
    }

    /** A shank of hunger (half: only its bone end), as the hunger bar draws them. */
    static void shank(UiCanvas c, int x, int y, boolean half) {
        int meat = 0xFFB45A2A;
        int dark = 0xFF6E3014;
        int bone = 0xFFEDE4D4;
        c.batch(() -> {
            c.fill(x, y + 5, x + 3, y + 8, bone);
            c.fill(x + 1, y + 6, x + 2, y + 7, 0xFFC8BCA8);
            if (!half) {
                c.fill(x + 3, y + 1, x + 8, y + 6, dark);
                c.fill(x + 3, y + 2, x + 7, y + 5, meat);
                c.fill(x + 4, y + 2, x + 6, y + 3, 0xFFE08A4A);
            } else {
                c.fill(x + 3, y + 3, x + 5, y + 6, dark);
                c.fill(x + 3, y + 4, x + 4, y + 5, meat);
            }
        });
    }

    /** An effect's own icon (its 18 x 18), {@code size} square. */
    static void effectIcon(UiCanvas c, String key, int x, int y, int size) {
        c.texture((Dishes.kitchenEffect(key) ? "hexkagune" : "minecraft") + ":textures/mob_effect/" + key + ".png", x, y, size, size, 0, 0, 18, 18, 18, 18);
    }

    // ------------------------------------------------------------------ the kitchen's own effects

    static Menu.Kitchen pickedEffect(Model model) {
        for (Menu.Kitchen effect : Menu.EFFECTS) {
            if (model.picked() != null && model.picked().equals("pick:e:" + effect.key())) {
                return effect;
            }
        }
        return Menu.EFFECTS[0];
    }

    /** Every dish that gives {@code key}. */
    static List<Dish> giving(String key) {
        List<Dish> out = new ArrayList<>();
        for (String chapter : Menu.CHAPTERS) {
            for (Dish dish : Dishes.chapter(chapter)) {
                for (Dish.Effect effect : dish.effects()) {
                    if (effect.key().equals(key)) {
                        out.add(dish);
                        break;
                    }
                }
            }
        }
        return out;
    }

    private static void effectsTab(UiCanvas c, Model model, Map<String, UkakuMenuView.Rect> hits, double mouseX, double mouseY, double time) {
        Menu.Kitchen picked = pickedEffect(model);
        int y = QuestFrame.CONTENT_Y;
        int index = 0;
        for (Menu.Kitchen effect : Menu.EFFECTS) {
            int count = giving(effect.key()).size();
            QuestFrame.Card card = new QuestFrame.Card("pick:e:" + effect.key(), c.tr("effect.hexkagune." + effect.key()), false,
                count + (count == 1 ? " DISH" : " DISHES"), QuestStyle.GREEN, QuestStyle.GREEN_DARK, "", -1.0F, 0, false,
                (cc, ix, iy, size, back, t) -> effectIcon(cc, effect.key(), ix, iy, size));
            QuestFrame.card(c, card, QuestFrame.LIST_X, y, QuestFrame.LIST_WIDTH, effect == picked, hits, mouseX, mouseY, time, index++);
            y += QuestFrame.CARD_HEIGHT + QuestFrame.CARD_GAP;
        }
        int ny = QuestFrame.section(c, c.tr("book.hexkagune.cookbook.effects").toUpperCase(java.util.Locale.ROOT), QuestFrame.LIST_X, y + 6, QuestFrame.LIST_WIDTH);
        for (String line : UkakuMenuView.wrap(c, "Effects only cooking gives: a good meal does more than fill you.", QuestFrame.LIST_WIDTH)) {
            c.text(line, QuestFrame.LIST_X, ny, QuestFrame.MUTED, false);
            ny += 12;
        }
        // The effect picked: its icon large, what it does, the dishes that give it (a click goes to the dish).
        int x = QuestFrame.DETAIL_X;
        int width = DETAIL_WIDTH;
        title(c, c.tr("effect.hexkagune." + picked.key()), "EFFECT", x, QuestFrame.CONTENT_Y, width);
        int rowY = QuestFrame.CONTENT_Y + 16;
        c.batch(() -> QuestStyle.box(c, x, QuestFrame.CONTENT_Y + 18, 58, 58, QuestStyle.TILE, 0xFF000000 | picked.colour()));
        effectIcon(c, picked.key(), x + 2, QuestFrame.CONTENT_Y + 20, 54);
        int sx = x + 70;
        int sy = QuestFrame.story(c, c.tr("effect.hexkagune." + picked.key() + ".description"), sx, rowY + 2, width - 70);
        List<Dish> dishes = giving(picked.key());
        int gy = QuestFrame.section(c, "IN THESE DISHES", dishes.size() + (dishes.size() == 1 ? " dish" : " dishes") + ": a click opens it", x,
            Math.max(QuestFrame.CONTENT_Y + 84, sy + 8), width);
        int perRow = width / (SLOT + SLOT_GAP);
        String named = null;
        for (int i = 0; i < dishes.size(); i++) {
            Dish dish = dishes.get(i);
            int dx = x + (i % perRow) * (SLOT + SLOT_GAP);
            int dy = gy + (i / perRow) * (SLOT + SLOT_GAP);
            if (dy + SLOT > BOX_HEIGHT - 22) {
                break;
            }
            UkakuMenuView.Rect slot = new UkakuMenuView.Rect(dx, dy, SLOT, SLOT);
            boolean over = slot.contains(mouseX, mouseY);
            c.batch(() -> QuestStyle.box(c, dx, dy, SLOT, SLOT, over ? QuestJournalView.ROW_HOVER : QuestJournalView.ROW, over ? QuestStyle.RED
                : QuestStyle.TILE_EDGE));
            c.item(dish.id(), 1, dx + 3, dy + 3, 32);
            hits.put("goto:" + dish.id(), slot);
            if (over) {
                named = c.tr("item.hexkagune." + dish.id()) + "  ·  " + c.tr("book.hexkagune.cookbook.chapter." + dish.chapter());
            }
        }
        if (named != null) {
            c.text(c.fit(named, width), x, BOX_HEIGHT - 18, QuestFrame.VALUE, false);
        }
    }

    // ------------------------------------------------------------------ shared

    private static void title(UiCanvas c, String name, String chip, int x, int y, int width) {
        c.push();
        c.translate(x, y);
        c.scale(1.25F);
        c.text(c.fit(name, Math.round((width - c.width(chip) - 30) / 1.25F)), 0, 0, QuestStyle.WHITE, true);
        c.pop();
        QuestStyle.chip(c, x + width - c.width(chip) - 12, y, chip, 0xFF1E0A0E, QuestStyle.RED_DARK, QuestFrame.MUTED);
    }
}
