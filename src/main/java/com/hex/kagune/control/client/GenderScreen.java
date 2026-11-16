package com.hex.kagune.control.client;

import com.hex.kagune.control.Gender;
import com.hex.kagune.control.client.ui.GuiCanvas;
import com.hex.kagune.control.network.C2SGender;
import com.hex.kagune.control.network.ControlNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** 1.3.9: the gender screen ({@link GenderView}), after the eyes (their screen's Next): Male or Female, then Done; Back to the eyes. */
public final class GenderScreen extends Screen {
    private final long openedAt = System.currentTimeMillis();
    private Gender picked = GenderClient.self();

    public GenderScreen() {
        super(Component.m_237113_("Who are you?"));
    }

    private GenderView.Layout layout() {
        return new GenderView.Layout(this.f_96543_, this.f_96544_);
    }

    @Override
    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        GuiCanvas canvas = new GuiCanvas(graphics, minecraft.f_91062_, player == null ? null : player.m_108560_());
        GenderView.draw(canvas, this.layout(), this.picked, this.layout().at(mouseX, mouseY), (System.currentTimeMillis() - this.openedAt) / 1000.0);
    }

    @Override
    public boolean m_6375_(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        String at = this.layout().at(mouseX, mouseY);
        if (at == null) {
            return false;
        }
        switch (at) {
            case "male" -> this.picked = Gender.MALE;
            case "female" -> this.picked = Gender.FEMALE;
            case "done" -> this.done();
            case "back" -> Minecraft.m_91087_().m_91152_(new KakuganScreen());
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case 263 -> this.picked = Gender.MALE; // Left
            case 262 -> this.picked = Gender.FEMALE; // Right
            case 257, 335 -> this.done(); // Enter
            default -> {
                return super.m_7933_(keyCode, scanCode, modifiers);
            }
        }
        return true;
    }

    private void done() {
        if (!this.picked.choice()) {
            return;
        }
        ControlNetwork.CHANNEL.sendToServer(new C2SGender(this.picked.ordinal()));
        Minecraft.m_91087_().m_91152_(null);
    }

    @Override
    public boolean m_7043_() {
        return false;
    }
}
