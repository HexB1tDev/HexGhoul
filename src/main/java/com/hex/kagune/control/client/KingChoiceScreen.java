package com.hex.kagune.control.client;

import com.hex.kagune.control.client.ui.GuiCanvas;
import com.hex.kagune.control.king.KingForm;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 1.0.10: the final kakuja's two cards ({@link KingChoiceView}) after the Dragon: a click picks a card, the button held (or Enter
 * held) chooses it, once and for good; the server checks and keeps it. Esc puts it off: the cards come back on the next login, or
 * from the menu.
 */
public final class KingChoiceScreen extends Screen {
    private static final long HOLD_MILLIS = 1200L;
    private final long openedAt = System.currentTimeMillis();
    private Map<String, UkakuMenuView.Rect> hits = Map.of();
    private float scale = 1.0F;
    private float originX;
    private float originY;
    private KingForm picked = KingForm.NONE;
    private long holdStarted = -1L;
    private boolean sent;

    public KingChoiceScreen() {
        super(Component.m_237113_("What kind of King will you become?"));
    }

    @Override
    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.sent && (!KingClient.awake() || KingClient.chosen() != KingForm.NONE)) {
            Minecraft.m_91087_().m_91152_(null); // chosen elsewhere, or the crown is gone
            return;
        }
        this.scale = Math.max(0.25F, Math.min(1.5F, Math.min((this.f_96543_ - 12.0F) / KingChoiceView.UI_WIDTH, (this.f_96544_ - 12.0F) / KingChoiceView.UI_HEIGHT)));
        this.originX = (this.f_96543_ - KingChoiceView.UI_WIDTH * this.scale) * 0.5F;
        this.originY = (this.f_96544_ - KingChoiceView.UI_HEIGHT * this.scale) * 0.5F;
        long now = System.currentTimeMillis();
        double hold = this.holdStarted < 0L ? 0.0 : (now - this.holdStarted) / (double)HOLD_MILLIS;
        GuiCanvas canvas = new GuiCanvas(graphics, Minecraft.m_91087_().f_91062_, null);
        canvas.push();
        canvas.translate(this.originX, this.originY);
        canvas.scale(this.scale);
        this.hits = KingChoiceView.draw(canvas, new KingChoiceView.Model(this.picked, hold, KingForm.PEACE.built()),
            (mouseX - this.originX) / this.scale, (mouseY - this.originY) / this.scale, (now - this.openedAt) / 1000.0);
        canvas.pop();
        if (this.holdStarted >= 0L && hold >= 1.0 && !this.sent) {
            this.sent = true;
            KingClient.choose(this.picked);
            Minecraft.m_91087_().m_91152_(null);
        }
    }

    private String at(double mouseX, double mouseY) {
        double x = (mouseX - this.originX) / this.scale;
        double y = (mouseY - this.originY) / this.scale;
        for (Map.Entry<String, UkakuMenuView.Rect> entry : this.hits.entrySet()) {
            if (entry.getValue().contains(x, y)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public boolean m_6375_(double mouseX, double mouseY, int button) {
        if (button != 0 || this.sent) {
            return false;
        }
        String key = this.at(mouseX, mouseY);
        if (key == null) {
            return false;
        }
        if (key.startsWith("card:")) {
            KingForm form = KingForm.byId(key.substring(5));
            if (form.built()) {
                this.picked = form;
                this.holdStarted = -1L;
            }
        } else if (key.equals("choose") && this.picked != KingForm.NONE) {
            this.holdStarted = System.currentTimeMillis();
        } else if (key.equals("later")) {
            Minecraft.m_91087_().m_91152_(null);
        }
        return true;
    }

    @Override
    public boolean m_6348_(double mouseX, double mouseY, int button) {
        if (button == 0 && this.holdStarted >= 0L && !this.sent) {
            this.holdStarted = -1L;
            return true;
        }
        return false;
    }

    @Override
    public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case 263, 65 -> this.picked = KingForm.WAR; // Left, A
            case 262, 68 -> this.picked = KingForm.PEACE.built() ? KingForm.PEACE : this.picked; // Right, D
            case 257, 335, 32 -> {
                if (this.picked != KingForm.NONE && this.holdStarted < 0L) {
                    this.holdStarted = System.currentTimeMillis();
                }
            }
            default -> {
                return super.m_7933_(keyCode, scanCode, modifiers); // Esc: decide later
            }
        }
        return true;
    }

    @Override
    public boolean m_7920_(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335 || keyCode == 32) && this.holdStarted >= 0L && !this.sent) {
            this.holdStarted = -1L;
            return true;
        }
        return false;
    }

    @Override
    public boolean m_7043_() {
        return false;
    }
}
