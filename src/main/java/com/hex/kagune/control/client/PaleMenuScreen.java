package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.client.KaguneKeys;
import com.hex.kagune.control.client.ui.GuiCanvas;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** The Pale path's kagune menu (6.1.0; the HexKagune menu key on her path); drawn by {@link PaleMenuView}. */
public final class PaleMenuScreen extends Screen {
    private final long openedAt = System.currentTimeMillis();
    private Map<String, PaleMenuView.Rect> hits = Map.of();
    private float scale = 1.0F;
    private float originX;
    private float originY;

    public PaleMenuScreen() {
        super(Component.m_237113_("HexKagune Owl DNA"));
    }

    @Override
    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.m_280273_(graphics);
        graphics.m_280024_(0, 0, this.f_96543_, this.f_96544_, 0xE8080709, 0xF0120E10);
        this.scale = Math.max(0.25F, Math.min(1.0F, Math.min((this.f_96543_ - 12.0F) / PaleMenuView.UI_WIDTH,
            (this.f_96544_ - 12.0F) / PaleMenuView.UI_HEIGHT)));
        this.originX = (this.f_96543_ - PaleMenuView.UI_WIDTH * this.scale) * 0.5F;
        this.originY = (this.f_96544_ - PaleMenuView.UI_HEIGHT * this.scale) * 0.5F;
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        GuiCanvas canvas = new GuiCanvas(graphics, minecraft.f_91062_, player == null ? null : player.m_108560_());
        KaguneClientState.View state = KaguneClientState.self();
        long now = minecraft.f_91073_ == null ? 0L : minecraft.f_91073_.m_46467_();
        long reviveIn = Math.max(0L, ReviveClient.readyAt() - now);
        PaleMenuView.Model model = new PaleMenuView.Model(state.mastery(), state.active(), KakuganClient.selfEyes(), ReviveClient.mask(),
            reviveIn > com.hex.kagune.control.pale.ReviveRules.COOLDOWN ? 0L : reviveIn);
        canvas.push();
        canvas.translate(this.originX, this.originY);
        canvas.scale(this.scale);
        this.hits = PaleMenuView.draw(canvas, model, (mouseX - this.originX) / this.scale, (mouseY - this.originY) / this.scale,
            (System.currentTimeMillis() - this.openedAt) / 1000.0);
        canvas.pop();
    }

    @Override
    public boolean m_6375_(double mouseX, double mouseY, int button) {
        if (button == 0) {
            double x = (mouseX - this.originX) / this.scale;
            double y = (mouseY - this.originY) / this.scale;
            PaleMenuView.Rect eyes = this.hits.get("eyes");
            if (eyes != null && eyes.contains(x, y)) {
                Minecraft.m_91087_().m_91152_(new KakuganScreen());
                return true;
            }
            PaleMenuView.Rect instant = this.hits.get("instant");
            if (instant != null && instant.contains(x, y)) {
                // 6.2.2: Witness This's Instant Big Punch switch (the server keeps it and sends it back).
                boolean on = (ReviveClient.mask() & com.hex.kagune.control.pale.PaleMutation.INSTANT_PUNCH) != 0;
                com.hex.kagune.control.network.ControlNetwork.CHANNEL.sendToServer(
                    new com.hex.kagune.control.network.C2SPaleAction(com.hex.kagune.control.network.C2SPaleAction.INSTANT_PUNCH, on ? 0 : 1, 0));
                return true;
            }
            for (Map.Entry<String, PaleMenuView.Rect> entry : this.hits.entrySet()) {
                if (entry.getKey().startsWith("mutation:") && entry.getValue().contains(x, y)) {
                    // The server checks there is a point to spend and the mutation isn't had yet.
                    int ordinal = Integer.parseInt(entry.getKey().substring(9));
                    com.hex.kagune.control.network.ControlNetwork.CHANNEL.sendToServer(
                        new com.hex.kagune.control.network.C2SPaleAction(com.hex.kagune.control.network.C2SPaleAction.MUTATION, ordinal, 0));
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
