package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.client.KaguneKeys;
import com.hex.kagune.control.client.ui.GuiCanvas;
import com.hex.kagune.control.graft.GraftStage;
import com.hex.kagune.data.KaguneProfile;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * 7.0.0: Graft DNA's kagune menu (the menu key), drawn by {@link GraftMenuView}: the base stage picked (the server checks the
 * mastery), the kakugan placement, and every stage's moves on their keys.
 *
 * <p>4.1.60 (the owner: "Every single menu in the game is to mimic and look like the quest menu, and be as interactable as it is"): the quest
 * menu's box with the world round it (no overlay), its tabs, what is picked kept while it is open; a stage grown into only by its GROW INTO
 * IT button, a mutation bought only by its UNLOCK button.
 */
public final class GraftMenuScreen extends Screen {
    private final long openedAt = System.currentTimeMillis();
    private Map<String, UkakuMenuView.Rect> hits = Map.of();
    private float scale = 1.0F;
    private float originX;
    private float originY;
    private GraftMenuView.Tab tab = GraftMenuView.Tab.STAGES;
    private String picked;

    public GraftMenuScreen() {
        super(Component.m_237113_("HexKagune Graft"));
    }

    @Override
    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 4.1.60: as the quest menu: no overlay over the world, only the box and its shadow round it.
        this.scale = Math.max(0.25F, Math.min(1.0F, Math.min((this.f_96543_ - 24.0F) / GraftMenuView.BOX_WIDTH,
            (this.f_96544_ - 24.0F) / GraftMenuView.BOX_HEIGHT)));
        this.originX = (this.f_96543_ - GraftMenuView.BOX_WIDTH * this.scale) * 0.5F;
        this.originY = (this.f_96544_ - GraftMenuView.BOX_HEIGHT * this.scale) * 0.5F;
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        GuiCanvas canvas = new GuiCanvas(graphics, minecraft.f_91062_, player == null ? null : player.m_108560_());
        KaguneClientState.View state = KaguneClientState.self();
        GraftClient.View view = GraftClient.get(player);
        GraftMenuView.Model model = new GraftMenuView.Model(state.mastery(), state.active(), view.stage(), view.kakuja(), state.energy(),
            KaguneProfile.MAX_ENERGY, view.mutations(), QuestLocks.live(), this.tab, this.picked);
        canvas.push();
        canvas.translate(this.originX, this.originY);
        canvas.scale(this.scale);
        this.hits = GraftMenuView.draw(canvas, model, (mouseX - this.originX) / this.scale, (mouseY - this.originY) / this.scale,
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
                this.tab = GraftMenuView.Tab.valueOf(key.substring(4));
                this.picked = null;
                return true;
            }
            if (key.startsWith("pick:") || key.startsWith("step:")) {
                this.picked = key; // 4.1.60: a card, a stage's pill or a step looked at; nothing changes in the game
                return true;
            }
            // 4.1.60: only the UNLOCK button carries "mutation:", only GROW INTO IT "stage:" (each drawn only while it may be used).
            if (key.startsWith("mutation:")) {
                com.hex.kagune.control.graft.GraftMutation mutation = com.hex.kagune.control.graft.GraftMutation.fromId(key.substring(9));
                if (mutation != null) {
                    GraftInput.unlockMutation(mutation.ordinal());
                }
                return true;
            }
            if (key.startsWith("stage:")) {
                GraftStage stage = GraftStage.fromId(key.substring(6));
                if (stage != null) {
                    GraftInput.pickStage(stage.ordinal());
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
