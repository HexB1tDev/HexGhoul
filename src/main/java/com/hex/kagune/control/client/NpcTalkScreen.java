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
 *
 * <p>1.3.3: each page's words are typed out with a voice (a soft blip every few letters, pitched by who is talking, a little different
 * each time); a click, a number key, Space or Enter while they come shows them all at once, and the answers come in after. The bond
 * meter glides from where it was to where it is now, and shows what changed.
 */
public final class NpcTalkScreen extends Screen {
    /** How fast the meter's heart glides (share of the way a second, about). */
    static final double GLIDE = 5.0;

    private final long openedAt = System.currentTimeMillis();
    final int npc;
    private final ResourceLocation skin;
    private NpcTalkView.Model model;
    private Map<String, UkakuMenuView.Rect> hits = Map.of();
    private float scale = 1.0F;
    private float originX;
    private float originY;
    private boolean sent;
    /** This page: when it came, whether its words were skipped to the end, how many letters have been voiced. */
    private long pageAt = System.currentTimeMillis();
    private boolean skipped;
    private int voiced;
    /** The meter: as shown now, the last change and when it came. */
    private float shown;
    private long drawnAt = System.currentTimeMillis();
    private int delta;
    private long deltaAt = Long.MIN_VALUE;

    public NpcTalkScreen(int npc, ResourceLocation skin, NpcTalkView.Model model) {
        super(Component.m_237113_(model.name()));
        this.npc = npc;
        this.skin = skin;
        this.model = model;
        // It opens with the heart gliding from the middle to where they stand.
        this.shown = 0.0F;
    }

    /** The next page from the server. */
    void page(NpcTalkView.Model model) {
        if (model.regard() != this.model.regard()) {
            this.delta = model.regard() - this.model.regard();
            this.deltaAt = System.currentTimeMillis();
        }
        this.model = model;
        this.sent = false;
        this.pageAt = System.currentTimeMillis();
        this.skipped = false;
        this.voiced = 0;
    }

    private double typed() {
        return this.skipped ? Double.POSITIVE_INFINITY : (System.currentTimeMillis() - this.pageAt) / 1000.0;
    }

    private boolean talking() {
        String line = this.model.line() == null ? "" : this.model.line();
        return NpcTalkView.revealed(line, this.typed()) < line.length();
    }

    @Override
    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        double step = Math.min(0.1, (now - this.drawnAt) / 1000.0);
        this.drawnAt = now;
        this.shown += (float)((this.model.regard() - this.shown) * Math.min(1.0, step * GLIDE));
        if (Math.abs(this.model.regard() - this.shown) < 0.05F) {
            this.shown = this.model.regard();
        }
        this.voice();
        this.scale = Math.max(0.25F, Math.min(1.0F, Math.min((this.f_96543_ - 12.0F) / NpcTalkView.UI_WIDTH, (this.f_96544_ - 6.0F) / NpcTalkView.UI_HEIGHT)));
        this.originX = (this.f_96543_ - NpcTalkView.UI_WIDTH * this.scale) * 0.5F;
        this.originY = this.f_96544_ - NpcTalkView.UI_HEIGHT * this.scale;
        GuiCanvas canvas = new GuiCanvas(graphics, Minecraft.m_91087_().f_91062_, this.skin);
        canvas.push();
        canvas.translate(this.originX, this.originY);
        canvas.scale(this.scale);
        NpcTalkView.Motion motion = new NpcTalkView.Motion((now - this.openedAt) / 1000.0, this.typed(), this.shown, this.delta,
            this.deltaAt == Long.MIN_VALUE ? 10.0 : (now - this.deltaAt) / 1000.0);
        this.hits = NpcTalkView.draw(canvas, this.model, (mouseX - this.originX) / this.scale, (mouseY - this.originY) / this.scale,
            (now - this.openedAt) / 1000.0, motion);
        canvas.pop();
    }

    /** A soft blip for every few letters as they come: the one talking's voice (its pitch from its name, a little different each time). */
    private void voice() {
        String line = this.model.line() == null ? "" : this.model.line();
        int out = NpcTalkView.revealed(line, this.typed());
        if (this.skipped) {
            this.voiced = out;
            return;
        }
        while (this.voiced < out) {
            char at = line.charAt(this.voiced);
            if (Character.isLetter(at) && this.voiced % 3 == 0) {
                float base = 1.25F + Math.floorMod(this.model.name().hashCode(), 7) * 0.06F;
                float wobble = (Math.floorMod(at * 31 + this.voiced * 7, 9) - 4) * 0.025F;
                CutsceneOverlay.play("minecraft", "block.note_block.bit", 0.09F, base + wobble);
            }
            this.voiced++;
        }
    }

    /** True (and the words all shown) if they were still coming. */
    private boolean skip() {
        if (!this.talking()) {
            return false;
        }
        this.skipped = true;
        return true;
    }

    @Override
    public boolean m_6375_(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (this.skip()) {
            return true;
        }
        double x = (mouseX - this.originX) / this.scale;
        double y = (mouseY - this.originY) / this.scale;
        for (Map.Entry<String, UkakuMenuView.Rect> entry : this.hits.entrySet()) {
            if (entry.getValue().contains(x, y) && entry.getKey().startsWith("choice:")) {
                this.pick(Integer.parseInt(entry.getKey().substring(7)));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
        // Space or Enter shows the words at once; the number keys (1 to 9) pick an answer (the first press, while they come, shows them).
        if (keyCode == 32 || keyCode == 257 || keyCode == 335) {
            this.skip();
            return true;
        }
        if (keyCode >= 49 && keyCode <= 57) {
            if (!this.skip()) {
                this.pick(keyCode - 49);
            }
            return true;
        }
        return super.m_7933_(keyCode, scanCode, modifiers);
    }

    private void pick(int index) {
        if (this.sent || index < 0 || index >= this.model.choices().size() || !this.hits.containsKey("choice:" + index)) {
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
