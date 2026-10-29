package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.client.KaguneKeys;
import com.hex.kagune.control.client.ui.GuiCanvas;
import com.hex.kagune.control.ukaku.WingForm;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** The Ukaku path's kagune menu (opened by the HexKagune menu key on the Ukaku path); drawn by {@link UkakuMenuView}. */
public final class UkakuMenuScreen extends Screen {
    private final long openedAt = System.currentTimeMillis();
    private Map<String, UkakuMenuView.Rect> hits = Map.of();
    private float scale = 1.0F;
    private float originX;
    private float originY;

    public UkakuMenuScreen() {
        super(Component.m_237113_("HexKagune Ukaku"));
    }

    @Override
    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.m_280273_(graphics);
        graphics.m_280024_(0, 0, this.f_96543_, this.f_96544_, 0xE8070A08, 0xF0160A0B);
        this.scale = Math.max(0.25F, Math.min(1.0F, Math.min((this.f_96543_ - 12.0F) / UkakuMenuView.UI_WIDTH,
            (this.f_96544_ - 12.0F) / UkakuMenuView.UI_HEIGHT)));
        this.originX = (this.f_96543_ - UkakuMenuView.UI_WIDTH * this.scale) * 0.5F;
        this.originY = (this.f_96544_ - UkakuMenuView.UI_HEIGHT * this.scale) * 0.5F;
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        GuiCanvas canvas = new GuiCanvas(graphics, minecraft.f_91062_, player == null ? null : player.m_108560_());
        KaguneClientState.View state = KaguneClientState.self();
        WingForm form = player == null ? WingForm.TWIN : UkakuClient.get(player).form();
        int mutations = player == null ? 0 : UkakuClient.get(player).mutations();
        UkakuMenuView.Model model = new UkakuMenuView.Model(state.mastery(), state.active(), form, KakuganClient.selfEyes(), mutations,
            QuestLocks.live());
        canvas.push();
        canvas.translate(this.originX, this.originY);
        canvas.scale(this.scale);
        this.hits = UkakuMenuView.draw(canvas, model, (mouseX - this.originX) / this.scale, (mouseY - this.originY) / this.scale,
            (System.currentTimeMillis() - this.openedAt) / 1000.0);
        canvas.pop();
    }

    @Override
    public boolean m_6375_(double mouseX, double mouseY, int button) {
        if (button == 0) {
            double x = (mouseX - this.originX) / this.scale;
            double y = (mouseY - this.originY) / this.scale;
            UkakuMenuView.Rect eyes = this.hits.get("eyes");
            if (eyes != null && eyes.contains(x, y)) {
                Minecraft.m_91087_().m_91152_(new KakuganScreen());
                return true;
            }
            for (java.util.Map.Entry<String, UkakuMenuView.Rect> entry : this.hits.entrySet()) {
                if (entry.getKey().startsWith("mutation:") && entry.getValue().contains(x, y)) {
                    // The server checks there is a point to spend and the mutation isn't had yet.
                    int ordinal = Integer.parseInt(entry.getKey().substring(9));
                    com.hex.kagune.control.network.ControlNetwork.CHANNEL.sendToServer(
                        new com.hex.kagune.control.network.C2SUkakuAction(com.hex.kagune.control.network.C2SUkakuAction.MUTATE, ordinal));
                    return true;
                }
            }
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
