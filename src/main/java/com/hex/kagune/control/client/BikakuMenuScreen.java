package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.client.KaguneKeys;
import com.hex.kagune.control.client.ui.GuiCanvas;
import com.hex.kagune.data.KaguneProfile;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** 3.4.9: the Bikaku path's kagune menu (the menu key), drawn by {@link BikakuMenuView}. */
public final class BikakuMenuScreen extends Screen {
    private final long openedAt = System.currentTimeMillis();
    private Map<String, UkakuMenuView.Rect> hits = Map.of();
    private float scale = 1.0F;
    private float originX;
    private float originY;

    public BikakuMenuScreen() {
        super(Component.m_237113_("HexKagune Bikaku"));
    }

    @Override
    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.m_280273_(graphics);
        graphics.m_280024_(0, 0, this.f_96543_, this.f_96544_, 0xE8080306, 0xF0180610);
        this.scale = Math.max(0.25F, Math.min(1.0F, Math.min((this.f_96543_ - 12.0F) / BikakuMenuView.UI_WIDTH,
            (this.f_96544_ - 12.0F) / BikakuMenuView.UI_HEIGHT)));
        this.originX = (this.f_96543_ - BikakuMenuView.UI_WIDTH * this.scale) * 0.5F;
        this.originY = (this.f_96544_ - BikakuMenuView.UI_HEIGHT * this.scale) * 0.5F;
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        GuiCanvas canvas = new GuiCanvas(graphics, minecraft.f_91062_, player == null ? null : player.m_108560_());
        KaguneClientState.View state = KaguneClientState.self();
        BikakuMenuView.Model model = new BikakuMenuView.Model(state.mastery(), state.active(), state.energy(), KaguneProfile.MAX_ENERGY);
        canvas.push();
        canvas.translate(this.originX, this.originY);
        canvas.scale(this.scale);
        this.hits = BikakuMenuView.draw(canvas, model, (mouseX - this.originX) / this.scale, (mouseY - this.originY) / this.scale,
            (System.currentTimeMillis() - this.openedAt) / 1000.0);
        canvas.pop();
    }

    @Override
    public boolean m_6375_(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        double x = (mouseX - this.originX) / this.scale;
        double y = (mouseY - this.originY) / this.scale;
        UkakuMenuView.Rect eyes = this.hits.get("eyes");
        if (eyes != null && eyes.contains(x, y)) {
            Minecraft.m_91087_().m_91152_(new KakuganScreen());
            return true;
        }
        return false;
    }

    @Override
    public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
        if (KaguneKeys.MENU.m_90832_(keyCode, scanCode)) {
            this.m_7379_();
            return true;
        }
        return super.m_7933_(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean m_7043_() {
        return false;
    }
}
