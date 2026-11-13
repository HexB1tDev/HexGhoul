package com.hex.kagune.control.client;

import com.hex.kagune.control.client.ui.GuiCanvas;
import com.hex.kagune.control.kitchen.Cookbook;
import com.hex.kagune.control.kitchen.Dish;
import com.hex.kagune.control.kitchen.Dishes;
import com.hex.kagune.control.kitchen.Menu;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Home Living (1.3.41): the ghoul cookbook open ({@link Cookbook}). 4.1.66: as the quest menu, drawn by {@link CookbookView}: the box alone
 * over the world (no overlay), a tab per chapter and one for the kitchen's effects, the chapter's dishes as slots, the dish picked as a page.
 * Arrows (or the wheel) go dish to dish, chapter to chapter; Home to the first. It opens where it was last left.
 */
public final class CookbookScreen extends Screen {
    /** Where it was left (this game's run). */
    private static String lastTab = Menu.CHAPTERS[0];
    private static String lastPicked;

    private final long openedAt = System.currentTimeMillis();
    private Map<String, UkakuMenuView.Rect> hits = Map.of();
    private float scale = 1.0F;
    private float originX;
    private float originY;
    private String tab = lastTab;
    private String picked = lastPicked;

    public CookbookScreen() {
        super(Component.m_237115_("item.hexkagune." + Cookbook.ID));
    }

    private void show(String tab, String picked) {
        this.tab = tab;
        this.picked = picked;
        lastTab = tab;
        lastPicked = picked;
    }

    @Override
    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.scale = Math.max(0.25F, Math.min(1.0F, Math.min((this.f_96543_ - 24.0F) / CookbookView.BOX_WIDTH, (this.f_96544_ - 24.0F) / CookbookView.BOX_HEIGHT)));
        this.originX = (this.f_96543_ - CookbookView.BOX_WIDTH * this.scale) * 0.5F;
        this.originY = (this.f_96544_ - CookbookView.BOX_HEIGHT * this.scale) * 0.5F;
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        GuiCanvas canvas = new GuiCanvas(graphics, minecraft.f_91062_, player == null ? null : player.m_108560_());
        canvas.push();
        canvas.translate(this.originX, this.originY);
        canvas.scale(this.scale);
        this.hits = CookbookView.draw(canvas, new CookbookView.Model(this.tab, this.picked), (mouseX - this.originX) / this.scale,
            (mouseY - this.originY) / this.scale, (System.currentTimeMillis() - this.openedAt) / 1000.0);
        canvas.pop();
    }

    @Override
    public boolean m_6375_(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.m_6375_(mouseX, mouseY, button);
        }
        double x = (mouseX - this.originX) / this.scale;
        double y = (mouseY - this.originY) / this.scale;
        for (Map.Entry<String, UkakuMenuView.Rect> entry : this.hits.entrySet()) {
            if (!entry.getValue().contains(x, y)) {
                continue;
            }
            String key = entry.getKey();
            if (key.startsWith("tab:")) {
                this.show(key.substring(4), null);
            } else if (key.startsWith("pick:")) {
                this.show(this.tab, key);
            } else if (key.startsWith("goto:")) {
                // A dish that gives the effect shown: its chapter, it picked.
                Dish dish = Dishes.byId(key.substring(5));
                if (dish != null) {
                    this.show(dish.chapter(), "pick:d:" + dish.id());
                }
            }
            return true;
        }
        return super.m_6375_(mouseX, mouseY, button);
    }

    /** One dish on (or back), into the next chapter (or the previous) past its last (or first); the effects one by one. */
    private void step(int by) {
        if (CookbookView.EFFECTS.equals(this.tab)) {
            Menu.Kitchen[] all = Menu.EFFECTS;
            int at = 0;
            for (int i = 0; i < all.length; i++) {
                if (("pick:e:" + all[i].key()).equals(this.picked)) {
                    at = i;
                }
            }
            this.show(this.tab, "pick:e:" + all[Math.floorMod(at + by, all.length)].key());
            return;
        }
        List<String> chapters = List.of(Menu.CHAPTERS);
        int chapter = Math.max(0, chapters.indexOf(this.tab));
        List<Dish> dishes = Dishes.chapter(this.tab);
        Dish shown = CookbookView.pickedDish(new CookbookView.Model(this.tab, this.picked));
        int at = shown == null ? 0 : dishes.indexOf(shown) + by;
        if (at >= dishes.size() && chapter + 1 < chapters.size()) {
            this.show(chapters.get(chapter + 1), null);
        } else if (at < 0 && chapter > 0) {
            List<Dish> before = Dishes.chapter(chapters.get(chapter - 1));
            this.show(chapters.get(chapter - 1), before.isEmpty() ? null : "pick:d:" + before.get(before.size() - 1).id());
        } else if (at >= 0 && at < dishes.size()) {
            this.show(this.tab, "pick:d:" + dishes.get(at).id());
        }
    }

    @Override
    public boolean m_6050_(double mouseX, double mouseY, double delta) {
        this.step(delta < 0 ? 1 : -1);
        return true;
    }

    @Override
    public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case 262, 267 -> this.step(1); // Right, Page Down
            case 263, 266 -> this.step(-1); // Left, Page Up
            case 268 -> this.show(Menu.CHAPTERS[0], null); // Home
            default -> {
                return super.m_7933_(keyCode, scanCode, modifiers);
            }
        }
        return true;
    }

    @Override
    public boolean m_7043_() {
        return false;
    }
}
