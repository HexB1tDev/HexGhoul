package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.client.PathCards.Card;
import com.hex.kagune.control.client.ui.GuiCanvas;
import com.hex.kagune.control.network.C2SChoosePath;
import com.hex.kagune.control.network.ControlNetwork;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * The starting screen: choose a kagune path, one card per choosable path ({@link PathCards}; the Ukaku path notes,
 * "Starting GUI"). Pick a card with the mouse or arrow keys, then hold the Awaken button (or Enter) to confirm,
 * because the choice is permanent. The client only asks; the server checks and stores the choice.
 */
public final class PathChoiceScreen extends Screen {
    private static final long HOLD_MILLIS = 900L;

    private final List<Card> cards = PathCards.choosable();
    private final long openedAt = System.currentTimeMillis();
    private KagunePath selected = KagunePath.NONE;
    private long holdStarted = -1L;
    private boolean holdByMouse;
    private int holdKey = -1;
    private boolean sent;

    public PathChoiceScreen() {
        super(Component.m_237115_("screen.hexkagune.path_choice"));
    }

    private PathChoiceLayout layout() {
        return PathChoiceLayout.of(this.f_96543_, this.f_96544_);
    }

    @Override
    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.sent && (PathClient.self().chosen() || !KaguneClientState.self().unlocked())) {
            // Chosen elsewhere (an operator, another window) or the kagune was taken away: nothing left to decide.
            Minecraft.m_91087_().m_91152_(null);
            return;
        }
        PathChoiceLayout layout = this.layout();
        long now = System.currentTimeMillis();
        double time = (now - this.openedAt) / 1000.0;
        // 4.1.64: drawn by PathChoiceView (the quest menu's look), the same for the game and the renders.
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        GuiCanvas canvas = new GuiCanvas(graphics, minecraft.f_91062_, player == null ? null : player.m_108560_());
        float hold = this.holdStarted < 0L ? -1.0F : Math.min(1.0F, (now - this.holdStarted) / (float)HOLD_MILLIS);
        PathChoiceView.draw(canvas, layout, this.cards, new PathChoiceView.State(this.selected, mouseX, mouseY, time, hold));
        if (this.holdStarted >= 0L) {
            if (this.holdByMouse && !layout.confirm.contains(mouseX, mouseY)) {
                this.cancelHold();
            } else if (now - this.holdStarted >= HOLD_MILLIS) {
                this.awaken();
            }
        }
    }

    private void select(KagunePath path) {
        if (this.selected != path) {
            this.cancelHold();
            this.selected = path;
        }
    }

    /** Moves the selection one card left or right; the first press picks the outermost card on that side (as with two cards). */
    private void step(int direction) {
        int index = -1;
        for (int i = 0; i < this.cards.size(); i++) {
            if (this.cards.get(i).path() == this.selected) {
                index = i;
            }
        }
        int last = this.cards.size() - 1;
        int next = index < 0 ? (direction > 0 ? last : 0) : Math.max(0, Math.min(last, index + direction));
        this.select(this.cards.get(next).path());
    }

    private void startHold(boolean mouse, int key) {
        if (this.selected.chosen() && this.holdStarted < 0L && !this.sent) {
            this.holdStarted = System.currentTimeMillis();
            this.holdByMouse = mouse;
            this.holdKey = key;
        }
    }

    private void cancelHold() {
        this.holdStarted = -1L;
        this.holdKey = -1;
    }

    private void awaken() {
        this.sent = true;
        this.cancelHold();
        ControlNetwork.CHANNEL.sendToServer(new C2SChoosePath(this.selected));
        Minecraft.m_91087_().m_91152_(null);
    }

    @Override
    public boolean m_6375_(double mouseX, double mouseY, int button) {
        if (button != 0 || this.sent) {
            return false;
        }
        PathChoiceLayout layout = this.layout();
        int card = layout.cardAt(mouseX, mouseY);
        if (card >= 0 && card < this.cards.size()) {
            this.select(this.cards.get(card).path());
        } else if (layout.confirm.contains(mouseX, mouseY)) {
            this.startHold(true, -1);
        } else if (layout.later.contains(mouseX, mouseY)) {
            Minecraft.m_91087_().m_91152_(null);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean m_6348_(double mouseX, double mouseY, int button) {
        if (button == 0 && this.holdByMouse && this.holdStarted >= 0L) {
            this.cancelHold();
            return true;
        }
        return false;
    }

    @Override
    public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case 263, 65, 265, 87 -> this.step(-1); // Left, A, Up, W
            case 262, 68, 264, 83 -> this.step(1);  // Right, D, Down, S
            case 257, 335, 32 -> this.startHold(false, keyCode); // Enter, keypad Enter, Space
            default -> {
                return super.m_7933_(keyCode, scanCode, modifiers); // Esc: decide later
            }
        }
        return true;
    }

    @Override
    public boolean m_7920_(int keyCode, int scanCode, int modifiers) {
        if (!this.holdByMouse && this.holdStarted >= 0L && keyCode == this.holdKey) {
            this.cancelHold();
            return true;
        }
        return false;
    }
}
