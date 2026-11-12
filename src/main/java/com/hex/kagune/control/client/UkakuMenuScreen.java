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

/**
 * The Ukaku path's kagune menu (opened by the HexKagune menu key on the Ukaku path); drawn by {@link UkakuMenuView}.
 *
 * <p>4.1.56 (the owner: "Every single menu in the game is to mimic and look like the quest menu, and be as interactable as it is"): the quest
 * menu's box with the world seen round it (no overlay), its tabs, and what is picked kept while it is open; a mutation is spent only by its
 * UNLOCK button.
 */
public final class UkakuMenuScreen extends Screen {
    private final long openedAt = System.currentTimeMillis();
    private Map<String, UkakuMenuView.Rect> hits = Map.of();
    private float scale = 1.0F;
    private float originX;
    private float originY;
    private UkakuMenuView.Tab tab = UkakuMenuView.Tab.ORGAN;
    private String picked;

    public UkakuMenuScreen() {
        super(Component.m_237113_("HexKagune Ukaku"));
    }

    @Override
    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 4.1.56: as the quest menu: no overlay over the world, only the box and its shadow round it.
        this.scale = Math.max(0.25F, Math.min(1.0F, Math.min((this.f_96543_ - 24.0F) / UkakuMenuView.BOX_WIDTH,
            (this.f_96544_ - 24.0F) / UkakuMenuView.BOX_HEIGHT)));
        this.originX = (this.f_96543_ - UkakuMenuView.BOX_WIDTH * this.scale) * 0.5F;
        this.originY = (this.f_96544_ - UkakuMenuView.BOX_HEIGHT * this.scale) * 0.5F;
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        GuiCanvas canvas = new GuiCanvas(graphics, minecraft.f_91062_, player == null ? null : player.m_108560_());
        KaguneClientState.View state = KaguneClientState.self();
        WingForm form = player == null ? WingForm.TWIN : UkakuClient.get(player).form();
        int mutations = player == null ? 0 : UkakuClient.get(player).mutations();
        UkakuMenuView.Model model = new UkakuMenuView.Model(state.mastery(), state.active(), form, KakuganClient.selfEyes(), mutations,
            QuestLocks.live(), this.tab, this.picked);
        canvas.push();
        canvas.translate(this.originX, this.originY);
        canvas.scale(this.scale);
        this.hits = UkakuMenuView.draw(canvas, model, (mouseX - this.originX) / this.scale, (mouseY - this.originY) / this.scale,
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
        for (Map.Entry<String, UkakuMenuView.Rect> entry : this.hits.entrySet()) {
            if (!entry.getValue().contains(x, y)) {
                continue;
            }
            String key = entry.getKey();
            if (key.equals("eyes")) {
                Minecraft.m_91087_().m_91152_(new KakuganScreen());
                return true;
            }
            if (key.startsWith("tab:")) {
                this.tab = UkakuMenuView.Tab.valueOf(key.substring(4));
                this.picked = null;
                return true;
            }
            if (key.startsWith("stage:") || key.startsWith("unlock:") || key.startsWith("pick:")) {
                this.picked = key;
                return true;
            }
            if (key.startsWith("mutation:")) {
                // 4.1.56: only the UNLOCK button carries this key (drawn only while a point can be spent on it); the server checks again.
                int ordinal = Integer.parseInt(key.substring(9));
                com.hex.kagune.control.network.ControlNetwork.CHANNEL.sendToServer(
                    new com.hex.kagune.control.network.C2SUkakuAction(com.hex.kagune.control.network.C2SUkakuAction.MUTATE, ordinal));
                return true;
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
