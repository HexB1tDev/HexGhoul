package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.client.ui.GuiCanvas;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** 7.0.1: the quest journal (the Quests key, U by default), drawn by {@link QuestJournalView}; tabs and quests picked by click. */
public final class QuestScreen extends Screen {
    private final long openedAt = System.currentTimeMillis();
    private Map<String, AyatoMenuView.Rect> hits = Map.of();
    private float scale = 1.0F;
    private float originX;
    private float originY;
    private QuestJournalView.Tab tab = QuestJournalView.Tab.PATH;
    private String picked;

    public QuestScreen() {
        super(Component.m_237113_("HexKagune Quests"));
        // Tonight's hunt first, when there is one.
        if (!QuestClient.log().hunts().isEmpty()) {
            this.tab = QuestJournalView.Tab.HUNTS;
        }
    }

    private QuestJournalView.Model model() {
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        KagunePath path = player == null ? KagunePath.NONE : PathClient.get(player);
        long time = minecraft.f_91073_ == null ? 0L : minecraft.f_91073_.m_46467_();
        return new QuestJournalView.Model(path, KaguneClientState.self().mastery(), QuestClient.log(), time, this.tab, this.picked);
    }

    @Override
    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.m_280273_(graphics);
        graphics.m_280024_(0, 0, this.f_96543_, this.f_96544_, 0xE8080506, 0xF0180608);
        this.scale = Math.max(0.25F, Math.min(1.0F, Math.min((this.f_96543_ - 12.0F) / QuestJournalView.UI_WIDTH,
            (this.f_96544_ - 12.0F) / QuestJournalView.UI_HEIGHT)));
        this.originX = (this.f_96543_ - QuestJournalView.UI_WIDTH * this.scale) * 0.5F;
        this.originY = (this.f_96544_ - QuestJournalView.UI_HEIGHT * this.scale) * 0.5F;
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        GuiCanvas canvas = new GuiCanvas(graphics, minecraft.f_91062_, player == null ? null : player.m_108560_());
        canvas.push();
        canvas.translate(this.originX, this.originY);
        canvas.scale(this.scale);
        this.hits = QuestJournalView.draw(canvas, this.model(), (mouseX - this.originX) / this.scale, (mouseY - this.originY) / this.scale,
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
            if (!entry.getValue().contains(x, y)) {
                continue;
            }
            String key = entry.getKey();
            if (key.startsWith("tab:")) {
                this.tab = QuestJournalView.Tab.valueOf(key.substring(4));
                this.picked = null;
            } else if (key.startsWith("quest:")) {
                this.picked = key.substring(6);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
        if (ControlKeys.QUESTS.m_90832_(keyCode, scanCode)) {
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
