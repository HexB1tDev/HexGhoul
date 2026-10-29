package com.hex.kagune.control.client.animation;

import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.pale.OwlActs;

/**
 * 6.2.0: one player's two giant arms between the Owl's moves and her idle ({@link OwlMotions#swing}, {@link OwlPose#swing}): a move
 * starts from the arms exactly as they were drawn the frame before (out of her idle, or out of the move it cut off), and when it is
 * over they ease back into her idle from where it left them. The drawing keeps one of these for each player in the Owl.
 */
public final class OwlArms {
    /** After a move, how long (ticks) the arms take to ease back into her idle. */
    static final double OUT = 7.0;

    private PaleMove move = PaleMove.NONE;
    /** Whether the arms were the move's, the frame before (keyed for it, or held by the creature's act). */
    private boolean armed;
    private long startedAt = Long.MIN_VALUE;
    private int detail;
    private OwlPose.Swing startRight;
    private OwlPose.Swing startLeft;
    private OwlPose.Swing shownRight;
    private OwlPose.Swing shownLeft;
    private OwlPose.Swing lastRight;
    private OwlPose.Swing lastLeft;
    private double endedAt = Double.NaN;
    /** When the arms were last drawn (what was shown longer ago than a couple of ticks is no start for anything). */
    private double drawnAt = Double.NaN;

    /**
     * Both arms ({right, left}) this frame at {@code time} (ticks, never set back): {@code move} (begun at {@code startedAt}, its
     * detail, {@code age} ticks in, its charge) if it keys them, off her torso as {@code body} poses it (the move thrown {@code turn}
     * radians, + to her right, from her body's facing), else her idle ({@code idleRight}, {@code idleLeft}), eased into from a move
     * just over.
     */
    public OwlPose.Swing[] swing(PaleMove move, long startedAt, int detail, double age, float charge, double time, OwlPose.Swing idleRight,
                                 OwlPose.Swing idleLeft, RigPose body, float turn) {
        return this.swing(move, startedAt, detail, age, charge, time, idleRight, idleLeft, body, turn, null);
    }

    /** 6.2.4: the same through the creature's act ({@code act}, null: none), the kagune held, driven in or spread as it has them. */
    public OwlPose.Swing[] swing(PaleMove move, long startedAt, int detail, double age, float charge, double time, OwlPose.Swing idleRight,
                                 OwlPose.Swing idleLeft, RigPose body, float turn, OwlActs.Act act) {
        boolean keyed = OwlMotions.armed(move);
        boolean armed = keyed || act != null;
        if (!(Math.abs(time - this.drawnAt) <= 2.0)) {
            // Not drawn lately (out of sight, another world): nothing shown to start from or ease out of.
            this.shownRight = null;
            this.shownLeft = null;
            this.lastRight = null;
            this.lastLeft = null;
        }
        this.drawnAt = time;
        if (armed && (move != this.move || startedAt != this.startedAt || detail != this.detail)) {
            this.startRight = this.shownRight == null ? idleRight : this.shownRight;
            this.startLeft = this.shownLeft == null ? idleLeft : this.shownLeft;
            this.endedAt = Double.NaN;
        }
        if (!armed && this.armed) {
            this.endedAt = time;
            this.lastRight = this.shownRight;
            this.lastLeft = this.shownLeft;
        }
        this.move = move;
        this.armed = armed;
        this.startedAt = startedAt;
        this.detail = detail;
        OwlPose.Swing right;
        OwlPose.Swing left;
        if (armed) {
            right = keyed ? OwlMotions.swing(move, 1, age, detail, charge, this.startRight, body, turn) : idleRight;
            left = keyed ? OwlMotions.swing(move, -1, age, detail, charge, this.startLeft, body, turn) : idleLeft;
            right = OwlPose.acted(right, this.startRight, age, act, 1);
            left = OwlPose.acted(left, this.startLeft, age, act, -1);
        } else if (!Double.isNaN(this.endedAt) && time - this.endedAt < OUT && this.lastRight != null && this.lastLeft != null) {
            double u = OwlPose.smooth((time - this.endedAt) / OUT);
            right = OwlMotions.blend(this.lastRight, idleRight, u, 1);
            left = OwlMotions.blend(this.lastLeft, idleLeft, u, -1);
        } else {
            right = idleRight;
            left = idleLeft;
        }
        right = OwlMotions.carried(right, 1);
        left = OwlMotions.carried(left, -1);
        this.shownRight = right;
        this.shownLeft = left;
        return new OwlPose.Swing[] {right, left};
    }
}
