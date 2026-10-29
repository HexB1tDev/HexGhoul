package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.client.KaguneKeys;
import com.hex.kagune.control.client.ui.GuiCanvas;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.MutationNode;
import com.hex.kagune.network.C2SKaguneForm;
import com.hex.kagune.network.C2SKaguneMutation;
import com.hex.kagune.network.KaguneNetwork;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * 6.0.0: the Rinkaku path's kagune menu (the menu key), drawn by {@link RinkakuMenuView}: forms picked and mutations taken with the
 * original's own requests (the server checks them), and the kakugan placement.
 */
public final class RinkakuMenuScreen extends Screen {
    private final long openedAt = System.currentTimeMillis();
    private Map<String, UkakuMenuView.Rect> hits = Map.of();
    private float scale = 1.0F;
    private float originX;
    private float originY;
    private MutationNode pending;
    private long pendingUntil;

    public RinkakuMenuScreen() {
        super(Component.m_237113_("HexKagune Rinkaku"));
    }

    @Override
    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.m_280273_(graphics);
        // 1.0.10: Peace's board sits on a pale veil, War's and Rinkaku's on the dark.
        boolean pale = KingClient.chosen() == com.hex.kagune.control.king.KingForm.PEACE;
        graphics.m_280024_(0, 0, this.f_96543_, this.f_96544_, pale ? 0xC8C8CCD4 : 0xE8080506, pale ? 0xD8E2E4EA : 0xF0180608);
        this.scale = Math.max(0.25F, Math.min(1.0F, Math.min((this.f_96543_ - 12.0F) / RinkakuMenuView.UI_WIDTH,
            (this.f_96544_ - 12.0F) / RinkakuMenuView.UI_HEIGHT)));
        this.originX = (this.f_96543_ - RinkakuMenuView.UI_WIDTH * this.scale) * 0.5F;
        this.originY = (this.f_96544_ - RinkakuMenuView.UI_HEIGHT * this.scale) * 0.5F;
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        if (this.pending != null && System.currentTimeMillis() > this.pendingUntil) {
            this.pending = null;
        }
        GuiCanvas canvas = new GuiCanvas(graphics, minecraft.f_91062_, player == null ? null : player.m_108560_());
        KaguneClientState.View state = KaguneClientState.self();
        com.hex.kagune.control.king.KingForm worn = KingClient.wearing(KingClient.chosen()) ? KingClient.chosen()
            : com.hex.kagune.control.king.KingForm.NONE;
        RinkakuMenuView.Model model = new RinkakuMenuView.Model(state.mastery(), state.active(), state.form(), state.energy(), KaguneProfile.MAX_ENERGY,
            state.mutationMask(), this.pending, QuestLocks.live(), new RinkakuMenuView.King(KingClient.awake(), KingClient.chosen(), worn,
            com.hex.kagune.control.king.KingForm.NONE));
        canvas.push();
        canvas.translate(this.originX, this.originY);
        canvas.scale(this.scale);
        this.hits = RinkakuMenuView.draw(canvas, model, (mouseX - this.originX) / this.scale, (mouseY - this.originY) / this.scale,
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
            if (key.startsWith("form:")) {
                KaguneForm form = KaguneForm.fromId(key.substring(5));
                // 7.0.1: a form waiting for its quest is not asked for (the server would take it back).
                if (form != null && QuestClient.open(com.hex.kagune.control.quest.Gates.rinkaku(form))) {
                    KaguneNetwork.CHANNEL.sendToServer(new C2SKaguneForm(form));
                }
                return true;
            }
            if (key.startsWith("king:")) {
                // 1.0.10: the final kakuja is chosen on its two cards, once and for good.
                KingClient.openChoice();
                return true;
            }
            if (key.startsWith("mutation:")) {
                MutationNode node = MutationNode.find(key.substring(9));
                if (node == null || !QuestClient.open(com.hex.kagune.control.quest.Gates.rinkaku(node))) {
                    return true; // (7.0.1: or it waits for its quest)
                }
                if (node == this.pending) {
                    KaguneNetwork.CHANNEL.sendToServer(new C2SKaguneMutation(node));
                    this.pending = null;
                } else {
                    this.pending = node;
                    this.pendingUntil = System.currentTimeMillis() + 3000L;
                }
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
