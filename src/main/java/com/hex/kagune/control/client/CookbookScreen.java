package com.hex.kagune.control.client;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.kitchen.Cookbook;
import com.hex.kagune.control.kitchen.Dish;
import com.hex.kagune.control.kitchen.Dishes;
import com.hex.kagune.control.kitchen.Menu;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Home Living (1.3.41): the ghoul cookbook open ({@link Cookbook}): an open book in the spirit of Alex's Caves' guide, its pages turned
 * by the arrows in its corners (or the arrow keys, or the wheel), the house in the left corner back to the contents, a silk ribbon per
 * chapter over its top edge. The cover (its picture, its title, the contents), each chapter (its picture, a few words, its dishes as
 * icons to pick), each dish (its picture on the table, its recipe in slots with the real items, what it fills, what it gives, what is
 * in it) and the kitchen's own effects at the back. It opens where it was last left.
 */
public final class CookbookScreen extends Screen {
    private static final ResourceLocation BOOK = Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", "textures/gui/cookbook.png"));
    private static final ResourceLocation BACKDROP = Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", "textures/gui/cookbook/backdrop.png"));
    private static final ResourceLocation ICONS = Objects.requireNonNull(ResourceLocation.m_214293_("minecraft", "textures/gui/icons.png"));
    static final int TEX_W = 512;
    static final int TEX_H = 256;
    static final int W = 300;
    static final int H = 196;
    static final int LEFT = 12;
    static final int RIGHT = 152;
    static final int TOP = 10;
    static final int PAGE_W = 136;
    static final int PAGE_H = 176;
    static final int MARGIN = 10;
    static final int INK = 0xFF40281E;
    static final int TITLE = 0xFF7A1418;
    static final int FAINT = 0xFF8A6A50;
    static final int GHOUL = 0xFF8C0E1C;
    /** Sprites in the book's texture: x, y, w, h. */
    static final int[] NEXT = {304, 0, 18, 12};
    static final int[] NEXT_HOVER = {304, 12, 18, 12};
    static final int[] PREV = {324, 0, 18, 12};
    static final int[] PREV_HOVER = {324, 12, 18, 12};
    static final int[] HOME = {344, 0, 14, 12};
    static final int[] HOME_HOVER = {344, 12, 14, 12};
    static final int[] FRAME = {304, 28, 104, 104};
    static final int[] RULE = {304, 136, 110, 5};
    static final int[] SLOT = {304, 144, 18, 18};
    static final int RIBBON_X = 412;
    static final int RIBBON_W = 12;
    static final int RIBBON_H = 30;

    /** Where it was left (this game's run). */
    private static int last;

    private int at = last;

    public CookbookScreen() {
        super(Component.m_237115_("item.hexkagune." + Cookbook.ID));
    }

    private int x0() {
        return (this.f_96543_ - W) / 2;
    }

    private int y0() {
        return (this.f_96544_ - H) / 2 + 6;
    }

    private void turn(int to) {
        this.at = Cookbook.clamp(to);
        last = this.at;
    }

    @Override
    public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.m_280273_(g);
        Font font = Minecraft.m_91087_().f_91062_;
        int x = this.x0();
        int y = this.y0();
        Cookbook.Spread spread = Cookbook.spread(this.at);
        this.ribbons(g, x, y, mouseX, mouseY, spread);
        g.m_280411_(BOOK, x, y, W, H, 0, 0, W, H, TEX_W, TEX_H);
        Hover hover = new Hover();
        switch (spread.kind()) {
            case COVER -> this.cover(g, font, x, y, mouseX, mouseY);
            case CHAPTER -> this.chapter(g, font, x, y, spread.chapter(), mouseX, mouseY, hover);
            case DISH -> this.dish(g, font, x, y, spread.dish(), mouseX, mouseY, hover);
            case EFFECTS -> this.effects(g, font, x, y);
        }
        this.corners(g, font, x, y, mouseX, mouseY);
        if (hover.stack != null) {
            g.m_280153_(font, hover.stack, mouseX, mouseY);
        }
    }

    /** What the mouse is over that wants a tooltip. */
    private static final class Hover {
        ItemStack stack;
    }

    // ---------------------------------------------------------------------------------------------------- the pages

    private void cover(GuiGraphics g, Font font, int x, int y, int mouseX, int mouseY) {
        int px = x + LEFT;
        this.picture(g, px + (PAGE_W - FRAME[2]) / 2, y + TOP + 8, "cover");
        this.centred(g, font, Component.m_237115_("book.hexkagune.cookbook.title").getString(), px + PAGE_W / 2, y + TOP + 120, TITLE);
        int ty = y + TOP + 134;
        for (String line : Cookbook.wrap(Component.m_237115_("book.hexkagune.cookbook.subtitle").getString(), PAGE_W - 2 * MARGIN, font::m_92895_)) {
            this.centred(g, font, line, px + PAGE_W / 2, ty, FAINT);
            ty += 10;
        }
        int rx = x + RIGHT + MARGIN;
        int ry = y + TOP + 12;
        this.centred(g, font, Component.m_237115_("book.hexkagune.cookbook.contents").getString(), x + RIGHT + PAGE_W / 2, ry, TITLE);
        this.rule(g, x + RIGHT + (PAGE_W - RULE[2]) / 2, ry + 12);
        ry += 24;
        for (String chapter : Menu.CHAPTERS) {
            this.contentsLine(g, font, rx, ry, Component.m_237115_("book.hexkagune.cookbook.chapter." + chapter).getString(),
                Cookbook.chapterAt(chapter), mouseX, mouseY);
            ry += 14;
        }
        this.contentsLine(g, font, rx, ry + 4, Component.m_237115_("book.hexkagune.cookbook.effects").getString(), Cookbook.effectsAt(), mouseX, mouseY);
    }

    private void contentsLine(GuiGraphics g, Font font, int x, int y, String name, int spread, int mouseX, int mouseY) {
        boolean over = mouseX >= x && mouseX < x + PAGE_W - 2 * MARGIN && mouseY >= y - 2 && mouseY < y + 10;
        String page = String.valueOf(Cookbook.pageNumber(spread));
        int width = PAGE_W - 2 * MARGIN;
        g.m_280056_(font, name, x, y, over ? TITLE : INK, false);
        g.m_280056_(font, page, x + width - font.m_92895_(page), y, over ? TITLE : FAINT, false);
        int dotsFrom = x + font.m_92895_(name) + 3;
        int dotsTo = x + width - font.m_92895_(page) - 3;
        for (int dx = dotsFrom; dx < dotsTo; dx += 3) {
            g.m_280509_(dx, y + 7, dx + 1, y + 8, FAINT);
        }
    }

    private void chapter(GuiGraphics g, Font font, int x, int y, String chapter, int mouseX, int mouseY, Hover hover) {
        int px = x + LEFT;
        this.picture(g, px + (PAGE_W - FRAME[2]) / 2, y + TOP + 4, chapter);
        this.centred(g, font, Component.m_237115_("book.hexkagune.cookbook.chapter." + chapter).getString(), px + PAGE_W / 2, y + TOP + 114, TITLE);
        int ty = y + TOP + 128;
        for (String line : Cookbook.wrap(Component.m_237115_("book.hexkagune.cookbook.chapter." + chapter + ".intro").getString(),
                PAGE_W - 2 * MARGIN, font::m_92895_)) {
            this.centred(g, font, line, px + PAGE_W / 2, ty, INK);
            ty += 10;
        }
        int rx = x + RIGHT + MARGIN;
        this.centred(g, font, Component.m_237115_("book.hexkagune.cookbook.in_chapter").getString(), x + RIGHT + PAGE_W / 2, y + TOP + 10, TITLE);
        this.rule(g, x + RIGHT + (PAGE_W - RULE[2]) / 2, y + TOP + 22);
        List<Dish> dishes = Dishes.chapter(chapter);
        String named = null;
        for (int i = 0; i < dishes.size(); i++) {
            int sx = rx + 2 + (i % 5) * 22;
            int sy = y + TOP + 32 + (i / 5) * 22;
            boolean over = mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18;
            this.slot(g, sx, sy, this.stack(dishes.get(i).id(), 1), over);
            if (over) {
                named = Component.m_237115_("item.hexkagune." + dishes.get(i).id()).getString();
            }
        }
        String foot = named != null ? named : Component.m_237115_("book.hexkagune.cookbook.pick").getString();
        this.centred(g, font, foot, x + RIGHT + PAGE_W / 2, y + TOP + PAGE_H - 30, named != null ? TITLE : FAINT);
    }

    private void dish(GuiGraphics g, Font font, int x, int y, Dish dish, int mouseX, int mouseY, Hover hover) {
        int px = x + LEFT;
        // Its picture: the kitchen table, the dish on it, four times its size.
        int fx = px + (PAGE_W - FRAME[2]) / 2;
        int fy = y + TOP + 6;
        this.picture(g, fx, fy, null);
        ItemStack own = this.stack(dish.id(), 1);
        g.m_280168_().m_85836_();
        g.m_280168_().m_252880_(fx + FRAME[2] / 2.0F - 32.0F, fy + 22.0F, 0.0F);
        g.m_280168_().m_85841_(4.0F, 4.0F, 1.0F);
        g.m_280480_(own, 0, 0);
        g.m_280168_().m_85849_();
        // Its recipe: the ingredients in slots, an arrow, what it makes.
        int ry = fy + FRAME[3] + 6;
        if (dish.recipe() == null) {
            for (String line : Cookbook.wrap(Component.m_237115_("book.hexkagune.cookbook.hunted").getString(), PAGE_W - 2 * MARGIN, font::m_92895_)) {
                this.centred(g, font, line, px + PAGE_W / 2, ry + 4, INK);
                ry += 10;
            }
        } else if (dish.recipe().cooking()) {
            int sx = px + MARGIN + 14;
            this.slotWithHover(g, sx, ry + 6, this.stack(dish.recipe().ingredients()[0], 1), mouseX, mouseY, hover);
            g.m_280056_(font, "->", sx + 24, ry + 11, INK, false);
            this.slotWithHover(g, sx + 40, ry + 6, own, mouseX, mouseY, hover);
            g.m_280056_(font, Component.m_237115_("book.hexkagune.cookbook.seared").getString(), sx + 62, ry + 11, FAINT, false);
        } else {
            String[] parts = dish.recipe().ingredients();
            int perRow = 5;
            int rows = (parts.length + perRow - 1) / perRow;
            for (int i = 0; i < parts.length; i++) {
                this.slotWithHover(g, px + MARGIN + (i % perRow) * 18, ry + (i / perRow) * 18, this.stack(parts[i], 1), mouseX, mouseY, hover);
            }
            int ax = px + MARGIN + perRow * 18 + 2;
            int ay = ry + (rows * 18) / 2 - 9;
            g.m_280056_(font, "->", ax, ay + 5, INK, false);
            this.slotWithHover(g, ax + 12, ay, this.stack(dish.id(), dish.makes()), mouseX, mouseY, hover);
        }
        // The right page: its name, what it is, what it fills, what is in it, what it gives.
        int rx = x + RIGHT + MARGIN;
        int ty = y + TOP + 8;
        for (String line : Cookbook.wrap(Component.m_237115_("item.hexkagune." + dish.id()).getString(), PAGE_W - 2 * MARGIN, font::m_92895_)) {
            this.centred(g, font, line, x + RIGHT + PAGE_W / 2, ty, TITLE);
            ty += 10;
        }
        this.rule(g, x + RIGHT + (PAGE_W - RULE[2]) / 2, ty + 1);
        ty += 10;
        String about = Component.m_237115_("item.hexkagune." + dish.id() + ".about").getString().replaceAll("§.", "");
        for (String line : Cookbook.wrap(about, PAGE_W - 2 * MARGIN, font::m_92895_)) {
            g.m_280056_(font, line, rx, ty, INK, false);
            ty += 10;
        }
        ty += 4;
        g.m_280056_(font, Component.m_237115_("book.hexkagune.cookbook.fills").getString(), rx, ty, FAINT, false);
        int shanks = (dish.nutrition() + 1) / 2;
        int sx = rx + font.m_92895_(Component.m_237115_("book.hexkagune.cookbook.fills").getString()) + 4;
        for (int i = 0; i < shanks; i++) {
            boolean half = i == shanks - 1 && dish.nutrition() % 2 == 1;
            g.m_280411_(ICONS, sx + i * 8, ty - 1, 9, 9, 16, 27, 9, 9, 256, 256);
            g.m_280411_(ICONS, sx + i * 8, ty - 1, 9, 9, half ? 61 : 52, 27, 9, 9, 256, 256);
        }
        ty += 13;
        if (dish.ghoulMeat() > 0) {
            String text = dish.ghoulMeat() == 1 ? Component.m_237115_("book.hexkagune.cookbook.ghoul_meat").getString()
                : Component.m_237110_("book.hexkagune.cookbook.ghoul_meat_pieces", dish.ghoulMeat()).getString();
            g.m_280056_(font, text, rx, ty, GHOUL, false);
            ty += 10;
        }
        if (dish.humanMeat()) {
            g.m_280056_(font, Component.m_237115_("book.hexkagune.cookbook.human_meat").getString(), rx, ty, TITLE, false);
            ty += 10;
        }
        ty += 3;
        for (Dish.Effect effect : dish.effects()) {
            this.effectIcon(g, effect.key(), rx, ty - 2, 12);
            String name = Component.m_237115_((Dishes.kitchenEffect(effect.key()) ? "effect.hexkagune." : "effect.minecraft.") + effect.key()).getString()
                + Dishes.level(effect.amplifier());
            g.m_280056_(font, name, rx + 15, ty, INK, false);
            String time = Dishes.time(effect.ticks());
            g.m_280056_(font, time, x + RIGHT + PAGE_W - MARGIN - font.m_92895_(time), ty, FAINT, false);
            ty += 13;
        }
    }

    /** The kitchen's own effects, two a page, each its icon, its name and what it does, one under the other. */
    private void effects(GuiGraphics g, Font font, int x, int y) {
        this.centred(g, font, Component.m_237115_("book.hexkagune.cookbook.effects").getString(), x + LEFT + PAGE_W / 2, y + TOP + 8, TITLE);
        this.rule(g, x + LEFT + (PAGE_W - RULE[2]) / 2, y + TOP + 20);
        Menu.Kitchen[] all = Menu.EFFECTS;
        int ty = y + TOP + 28;
        for (int i = 0; i < all.length; i++) {
            if (i == (all.length + 1) / 2) {
                ty = y + TOP + 8;
            }
            int px = (i < (all.length + 1) / 2 ? x + LEFT : x + RIGHT) + MARGIN;
            this.effectIcon(g, all[i].key(), px, ty, 18);
            g.m_280056_(font, Component.m_237115_("effect.hexkagune." + all[i].key()).getString(), px + 22, ty + 5, TITLE, false);
            ty += 22;
            for (String line : Cookbook.wrap(Component.m_237115_("effect.hexkagune." + all[i].key() + ".description").getString(),
                    PAGE_W - 2 * MARGIN, font::m_92895_)) {
                g.m_280056_(font, line, px, ty, INK, false);
                ty += 10;
            }
            ty += 8;
        }
    }

    // ---------------------------------------------------------------------------------------------------- the book's parts

    private void corners(GuiGraphics g, Font font, int x, int y, int mouseX, int mouseY) {
        int bottom = y + TOP + PAGE_H - 16;
        if (this.at > 0) {
            int px = x + LEFT + 8;
            this.sprite(g, this.over(mouseX, mouseY, px, bottom, PREV) ? PREV_HOVER : PREV, px, bottom);
            int hx = x + LEFT + 32;
            this.sprite(g, this.over(mouseX, mouseY, hx, bottom, HOME) ? HOME_HOVER : HOME, hx, bottom);
        }
        if (this.at < Cookbook.spreads().size() - 1) {
            int nx = x + RIGHT + PAGE_W - 8 - NEXT[2];
            this.sprite(g, this.over(mouseX, mouseY, nx, bottom, NEXT) ? NEXT_HOVER : NEXT, nx, bottom);
        }
        if (this.at > 0) {
            String left = String.valueOf(Cookbook.pageNumber(this.at));
            String right = String.valueOf(Cookbook.pageNumber(this.at) + 1);
            this.centred(g, font, left, x + LEFT + PAGE_W / 2, bottom + 2, FAINT);
            this.centred(g, font, right, x + RIGHT + PAGE_W / 2, bottom + 2, FAINT);
        }
    }

    /** The chapters' ribbons over the top edge of the right page: the open chapter's longer. */
    private void ribbons(GuiGraphics g, int x, int y, int mouseX, int mouseY, Cookbook.Spread spread) {
        for (int i = 0; i < Menu.CHAPTERS.length + 1; i++) {
            int rx = this.ribbonX(x, i);
            boolean open = i < Menu.CHAPTERS.length ? Menu.CHAPTERS[i].equals(spread.chapter()) : spread.kind() == Cookbook.Kind.EFFECTS;
            boolean over = mouseX >= rx && mouseX < rx + RIBBON_W && mouseY >= y - 14 && mouseY < y + 4;
            int lift = open ? 14 : over ? 11 : 8;
            g.m_280411_(BOOK, rx, y - lift, RIBBON_W, RIBBON_H, RIBBON_X + i * (RIBBON_W + 1), 0, RIBBON_W, RIBBON_H, TEX_W, TEX_H);
        }
    }

    private int ribbonX(int x, int i) {
        return x + RIGHT + 14 + i * (RIBBON_W + 3);
    }

    private void picture(GuiGraphics g, int fx, int fy, String name) {
        ResourceLocation picture = name == null ? BACKDROP
            : Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", "textures/gui/cookbook/" + name + ".png"));
        g.m_280411_(picture, fx + 4, fy + 4, FRAME[2] - 8, FRAME[3] - 8, 0, 0, 48, 48, 48, 48);
        this.sprite(g, FRAME, fx, fy);
    }

    private void rule(GuiGraphics g, int x, int y) {
        this.sprite(g, RULE, x, y);
    }

    private void slot(GuiGraphics g, int x, int y, ItemStack stack, boolean over) {
        this.sprite(g, SLOT, x, y);
        if (over) {
            g.m_280509_(x + 1, y + 1, x + 17, y + 17, 0x40FFFFFF);
        }
        if (stack != null) {
            g.m_280480_(stack, x + 1, y + 1);
            g.m_280370_(Minecraft.m_91087_().f_91062_, stack, x + 1, y + 1);
        }
    }

    private void slotWithHover(GuiGraphics g, int x, int y, ItemStack stack, int mouseX, int mouseY, Hover hover) {
        boolean over = mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
        this.slot(g, x, y, stack, over);
        if (over && stack != null) {
            hover.stack = stack;
        }
    }

    /** An effect's own icon (18x18), drawn {@code size} square. */
    private void effectIcon(GuiGraphics g, String key, int x, int y, int size) {
        String space = Dishes.kitchenEffect(key) ? "hexkagune" : "minecraft";
        ResourceLocation icon = ResourceLocation.m_214293_(space, "textures/mob_effect/" + key + ".png");
        if (icon != null) {
            g.m_280411_(icon, x, y, size, size, 0, 0, 18, 18, 18, 18);
        }
    }

    private ItemStack stack(String id, int count) {
        Item item = id.indexOf(':') >= 0 ? ControlItems.vanilla(id) : ControlItems.dishItem(id);
        return item == null ? null : new ItemStack(item, count);
    }

    private void sprite(GuiGraphics g, int[] sprite, int x, int y) {
        g.m_280411_(BOOK, x, y, sprite[2], sprite[3], sprite[0], sprite[1], sprite[2], sprite[3], TEX_W, TEX_H);
    }

    private boolean over(int mouseX, int mouseY, int x, int y, int[] sprite) {
        return mouseX >= x && mouseX < x + sprite[2] && mouseY >= y && mouseY < y + sprite[3];
    }

    private void centred(GuiGraphics g, Font font, String text, int cx, int y, int colour) {
        g.m_280056_(font, text, cx - font.m_92895_(text) / 2, y, colour, false);
    }

    // ---------------------------------------------------------------------------------------------------- turning the pages

    @Override
    public boolean m_6375_(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.m_6375_(mouseX, mouseY, button);
        }
        int mx = (int)mouseX;
        int my = (int)mouseY;
        int x = this.x0();
        int y = this.y0();
        int bottom = y + TOP + PAGE_H - 16;
        if (this.at > 0 && this.over(mx, my, x + LEFT + 8, bottom, PREV)) {
            this.turn(this.at - 1);
            return true;
        }
        if (this.at > 0 && this.over(mx, my, x + LEFT + 32, bottom, HOME)) {
            this.turn(0);
            return true;
        }
        if (this.at < Cookbook.spreads().size() - 1 && this.over(mx, my, x + RIGHT + PAGE_W - 8 - NEXT[2], bottom, NEXT)) {
            this.turn(this.at + 1);
            return true;
        }
        for (int i = 0; i < Menu.CHAPTERS.length + 1; i++) {
            int rx = this.ribbonX(x, i);
            if (mx >= rx && mx < rx + RIBBON_W && my >= y - 14 && my < y + 4) {
                this.turn(i < Menu.CHAPTERS.length ? Cookbook.chapterAt(Menu.CHAPTERS[i]) : Cookbook.effectsAt());
                return true;
            }
        }
        Cookbook.Spread spread = Cookbook.spread(this.at);
        if (spread.kind() == Cookbook.Kind.COVER) {
            int rx = x + RIGHT + MARGIN;
            int ry = y + TOP + 36;
            for (int i = 0; i < Menu.CHAPTERS.length + 1; i++) {
                int ly = ry + i * 14 + (i == Menu.CHAPTERS.length ? 4 : 0);
                if (mx >= rx && mx < rx + PAGE_W - 2 * MARGIN && my >= ly - 2 && my < ly + 10) {
                    this.turn(i < Menu.CHAPTERS.length ? Cookbook.chapterAt(Menu.CHAPTERS[i]) : Cookbook.effectsAt());
                    return true;
                }
            }
        }
        if (spread.kind() == Cookbook.Kind.CHAPTER) {
            List<Dish> dishes = Dishes.chapter(spread.chapter());
            int rx = x + RIGHT + MARGIN;
            for (int i = 0; i < dishes.size(); i++) {
                int sx = rx + 2 + (i % 5) * 22;
                int sy = y + TOP + 32 + (i / 5) * 22;
                if (mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18) {
                    this.turn(Cookbook.dishAt(dishes.get(i).id()));
                    return true;
                }
            }
        }
        return super.m_6375_(mouseX, mouseY, button);
    }

    @Override
    public boolean m_6050_(double mouseX, double mouseY, double delta) {
        this.turn(this.at + (delta < 0 ? 1 : -1));
        return true;
    }

    @Override
    public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case 262, 267 -> this.turn(this.at + 1); // Right, Page Down
            case 263, 266 -> this.turn(this.at - 1); // Left, Page Up
            case 268 -> this.turn(0); // Home
            default -> {
                return super.m_7933_(keyCode, scanCode, modifiers);
            }
        }
        return true;
    }
}
