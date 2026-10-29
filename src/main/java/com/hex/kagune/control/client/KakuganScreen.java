package com.hex.kagune.control.client;

import com.hex.kagune.control.client.PathChoiceLayout.Box;
import com.hex.kagune.control.client.ui.GuiCanvas;
import com.hex.kagune.control.kakugan.KakuganEyes;
import com.hex.kagune.control.network.C2SPlaceKakugan;
import com.hex.kagune.control.network.ControlNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Place your kakugan: your own face from your skin (inner layer), with your two eyes to drag onto wherever the eyes
 * are on your skin. Only the eyes move. Mirror keeps them symmetrical; eye height and iris side cover skins whose eyes
 * are one pixel tall or look the other way; "Find my eyes" puts them where the eyes are on your skin (read off it, 3.0.0;
 * a player who never places them gets those anyway). The face is shown with its outer layer, as others see it: where that
 * layer covers an eye, the kakugan is drawn on it too. One eyed (3.1.0) asks which eye becomes the ghoul eye; the other stays
 * human. Done sends the placement to the server (which validates it); Later or Esc keeps what you had (and {@code /kagune eyes}
 * brings this back).
 */
public final class KakuganScreen extends Screen {
    private final long openedAt = System.currentTimeMillis();
    private KakuganEyes eyes;
    private boolean mirror;
    private boolean selectedRight = true;
    private boolean dragging;
    /** The "which eye?" box is open (One eyed). */
    private boolean choosing;
    private int grabColumn;
    private int grabRow;

    public KakuganScreen() {
        super(Component.m_237115_("screen.hexkagune.kakugan"));
        this.eyes = this.oneEyed(KakuganClient.selfEyes());
        this.mirror = this.eyes.mirrored();
    }

    /** 6.1.0: on a path that is one-eyed by nature (Pale's) only one eye ever turns: no way to two, not even by a reset. */
    private boolean bothAllowed() {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        return player == null || PathClient.get(player) != com.hex.kagune.control.KagunePath.PALE && !this.rightOnly();
    }

    /** 7.0.0: Graft DNA's one kakugan is always the right eye: neither the left nor both can be picked. */
    private boolean rightOnly() {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        return player != null && PathClient.get(player) == com.hex.kagune.control.KagunePath.GRAFT;
    }

    /** {@code eyes} as this path allows them: a one-eyed path keeps one eye (the right, if both were set). */
    private KakuganEyes oneEyed(KakuganEyes eyes) {
        if (this.rightOnly()) {
            return eyes.withGhoul(KakuganEyes.RIGHT);
        }
        return this.bothAllowed() || eyes.oneEyed() ? eyes : eyes.withGhoul(KakuganEyes.RIGHT);
    }

    private KakuganScreenLayout layout() {
        return KakuganScreenLayout.of(this.f_96543_, this.f_96544_);
    }

    @Override
    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        KakuganScreenLayout layout = this.layout();
        if (this.dragging) {
            int[] cell = layout.cellAt(Math.max(layout.face.x(), Math.min(layout.face.right() - 1, mouseX)),
                Math.max(layout.face.y(), Math.min(layout.face.bottom() - 1, mouseY)));
            if (cell != null) {
                this.move(cell[0] - this.grabColumn, cell[1] - this.grabRow);
            }
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        GuiCanvas canvas = new GuiCanvas(graphics, minecraft.f_91062_, player == null ? null : player.m_108560_());
        int seed = player == null ? 0 : player.m_36316_().getId().hashCode();
        double time = (System.currentTimeMillis() - this.openedAt) / 1000.0;
        KakuganView.draw(canvas, layout, new KakuganView.State(this.eyes, seed, this.mirror, this.selectedRight, this.hovered(layout, mouseX, mouseY),
            this.dragging, time, KakuganClient.selfCovered(), KakuganClient.selfGlass(), KakuganClient.selfDetected() != null, this.choosing,
            this.bothAllowed()));
    }

    /** What is under the mouse: an eye ("right", "left") or a control; only the box's buttons while it is open. */
    private String hovered(KakuganScreenLayout layout, double x, double y) {
        if (this.choosing) {
            return layout.choiceRight.contains(x, y) ? "pick-right" : layout.choiceLeft.contains(x, y) ? "pick-left"
                : layout.choiceBoth.contains(x, y) && this.bothAllowed() ? "pick-both" : null;
        }
        if (layout.eye(this.eyes, true).contains(x, y)) {
            return "right";
        }
        if (layout.eye(this.eyes, false).contains(x, y)) {
            return "left";
        }
        String[] names = {"mirror", "height", "iris", "one", "outer", "reset", "done", "later"};
        Box[] boxes = {layout.mirror, layout.eyeHeight, layout.iris, layout.oneEye, layout.outer, layout.reset, layout.done, layout.later};
        for (int i = 0; i < boxes.length; i++) {
            if (boxes[i].contains(x, y)) {
                return names[i];
            }
        }
        return null;
    }

    /** Moves the selected eye so its top-left pixel is at ({@code column}, {@code row}). */
    private void move(int column, int row) {
        this.eyes = this.selectedRight ? this.eyes.withRight(column, row, this.mirror) : this.eyes.withLeft(column, row, this.mirror);
    }

    @Override
    public boolean m_6375_(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        KakuganScreenLayout layout = this.layout();
        String target = this.hovered(layout, mouseX, mouseY);
        if (this.choosing) {
            // A pick sets the ghoul eye; a click anywhere else just closes the box.
            switch (target == null ? "" : target) {
                case "pick-right" -> this.eyes = this.eyes.withGhoul(KakuganEyes.RIGHT);
                case "pick-left" -> this.eyes = this.oneEyed(this.eyes.withGhoul(KakuganEyes.LEFT));
                case "pick-both" -> this.eyes = this.oneEyed(this.eyes.withGhoul(KakuganEyes.BOTH));
                default -> {
                }
            }
            this.choosing = false;
            return true;
        }
        if ("right".equals(target) || "left".equals(target)) {
            this.selectedRight = "right".equals(target);
            int[] cell = layout.cellAt(mouseX, mouseY);
            this.grabColumn = cell == null ? 0 : cell[0] - this.eyes.column(this.selectedRight);
            this.grabRow = cell == null ? 0 : cell[1] - this.eyes.row(this.selectedRight);
            this.dragging = true;
            return true;
        }
        if (target == null) {
            int[] cell = layout.cellAt(mouseX, mouseY);
            if (cell == null) {
                return false;
            }
            // A click elsewhere on the face brings the nearer eye there.
            double right = Math.abs(cell[0] - (this.eyes.rightX() + 0.5)) + Math.abs(cell[1] - this.eyes.rightY());
            double left = Math.abs(cell[0] - (this.eyes.leftX() + 0.5)) + Math.abs(cell[1] - this.eyes.leftY());
            this.selectedRight = right <= left;
            this.move(cell[0] - (this.selectedRight ? 0 : 1), cell[1]);
            return true;
        }
        switch (target) {
            case "mirror" -> {
                this.mirror = !this.mirror;
                if (this.mirror) {
                    this.move(this.eyes.column(this.selectedRight), this.eyes.row(this.selectedRight));
                }
            }
            case "height" -> this.eyes = this.eyes.withHeight(this.eyes.height() == 2 ? 1 : 2);
            case "iris" -> this.eyes = this.eyes.withIrisInside(!this.eyes.irisInside());
            case "one" -> this.choosing = true;
            case "outer" -> {
                KakuganEyes found = KakuganClient.selfDetected();
                if (found != null) {
                    // Found where the eyes are; which of them turns stays the player's choice.
                    this.eyes = found.withGhoul(this.eyes.ghoul());
                    this.mirror = found.mirrored();
                }
            }
            case "reset" -> {
                // Back to where the eyes start; which of them turns is kept on a one-eyed path.
                this.eyes = this.bothAllowed() ? KakuganEyes.DEFAULT : KakuganEyes.DEFAULT.withGhoul(this.eyes.oneEyed() ? this.eyes.ghoul() : KakuganEyes.RIGHT);
                this.mirror = true;
            }
            case "done" -> this.done();
            case "later" -> Minecraft.m_91087_().m_91152_(null);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean m_6348_(double mouseX, double mouseY, int button) {
        if (button == 0 && this.dragging) {
            this.dragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
        if (this.choosing) {
            // Esc (or any key) closes the box first; Enter keeps what it shows.
            this.choosing = false;
            return true;
        }
        int column = this.eyes.column(this.selectedRight);
        int row = this.eyes.row(this.selectedRight);
        switch (keyCode) {
            case 263 -> this.move(column - 1, row); // Left
            case 262 -> this.move(column + 1, row); // Right
            case 265 -> this.move(column, row - 1); // Up
            case 264 -> this.move(column, row + 1); // Down
            case 258 -> this.selectedRight = !this.selectedRight; // Tab
            case 257, 335 -> this.done(); // Enter
            default -> {
                return super.m_7933_(keyCode, scanCode, modifiers); // Esc: later
            }
        }
        return true;
    }

    private void done() {
        ControlNetwork.CHANNEL.sendToServer(new C2SPlaceKakugan(this.eyes));
        Minecraft.m_91087_().m_91152_(null);
    }

    @Override
    public boolean m_7043_() {
        return false;
    }
}
