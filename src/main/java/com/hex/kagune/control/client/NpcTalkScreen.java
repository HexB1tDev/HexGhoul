package com.hex.kagune.control.client;

import com.hex.kagune.control.client.ui.GuiCanvas;
import com.hex.kagune.control.network.C2SNpcTalk;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.npc.NpcTalk;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 7.0.2: a conversation with a ghoul NPC ({@link NpcTalkView}), opened by the server when one agrees to talk. An answer is picked
 * by click or its number key and sent ({@code C2SNpcTalk}); the server sends the next page. Closing it says goodbye.
 */
public final class NpcTalkScreen extends Screen {
    private final long openedAt = System.currentTimeMillis();
    final int npc;
    private final ResourceLocation skin;
    private NpcTalkView.Model model;
    private Map<String, AyatoMenuView.Rect> hits = Map.of();
    private float scale = 1.0F;
    private float originX;
    private float originY;
    private boolean sent;

    public NpcTalkScreen(int npc, ResourceLocation skin, NpcTalkView.Model model) {
        super(Component.m_237113_(model.name()));
        this.npc = npc;
        this.skin = skin;
        this.model = model;
    }

    /** The next page from the server. */
    void page(NpcTalkView.Model model) {
        this.model = model;
        this.sent = false;
    }

    @Override
    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.scale = Math.max(0.25F, Math.min(1.0F, Math.min((this.f_96543_ - 12.0F) / NpcTalkView.UI_WIDTH, (this.f_96544_ - 6.0F) / NpcTalkView.UI_HEIGHT)));
        this.originX = (this.f_96543_ - NpcTalkView.UI_WIDTH * this.scale) * 0.5F;
        this.originY = this.f_96544_ - NpcTalkView.UI_HEIGHT * this.scale;
        GuiCanvas canvas = new GuiCanvas(graphics, Minecraft.m_91087_().f_91062_, this.skin);
        canvas.push();
        canvas.translate(this.originX, this.originY);
        canvas.scale(this.scale);
        this.hits = NpcTalkView.draw(canvas, this.model, (mouseX - this.originX) / this.scale, (mouseY - this.originY) / this.scale,
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
        for (Map.Entry<String, AyatoMenuView.Rect> entry : this.hits.entrySet()) {
            if (entry.getValue().contains(x, y) && entry.getKey().startsWith("choice:")) {
                this.pick(Integer.parseInt(entry.getKey().substring(7)));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
        // The number keys (1 to 9) pick an answer.
        if (keyCode >= 49 && keyCode <= 57) {
            this.pick(keyCode - 49);
            return true;
        }
        return super.m_7933_(keyCode, scanCode, modifiers);
    }

    private void pick(int index) {
        if (this.sent || index < 0 || index >= this.model.choices().size()) {
            return;
        }
        NpcTalk.Choice choice = this.model.choices().get(index);
        ControlNetwork.CHANNEL.sendToServer(new C2SNpcTalk(this.npc, choice.ordinal()));
        this.sent = true;
        if (choice == NpcTalk.Choice.LEAVE) {
            Minecraft.m_91087_().m_91152_(null);
        }
    }

    /** Closed (Escape): goodbye, so the ghoul stops waiting on them. */
    @Override
    public void m_7379_() {
        ControlNetwork.CHANNEL.sendToServer(new C2SNpcTalk(this.npc, NpcTalk.Choice.LEAVE.ordinal()));
        super.m_7379_();
    }

    @Override
    public boolean m_7043_() {
        return false;
    }
}
