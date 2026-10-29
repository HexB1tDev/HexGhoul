package com.hex.kagune.control.client;

import com.hex.kagune.control.ComboStage;
import com.hex.kagune.control.ComboStyle;
import com.hex.kagune.control.ControlMath;
import com.hex.kagune.control.UrgeType;
import com.hex.kagune.control.network.S2CControlState;

/** Client copy of a player's server-authoritative control state. Purely visual. */
public record ControlView(
    float control,
    float hunger,
    float frenzy,
    UrgeType urge,
    long urgeStarted,
    int urgeDuration,
    int urgeTargetId,
    float urgeStrength,
    boolean urgeRestrained,
    ComboStyle comboStyle,
    int comboStage,
    long comboStarted,
    int comboDuration,
    int comboLimbMask,
    float comboDeviation,
    boolean comboInvoluntary
) {
    /** Fully controlled, calm, fed: used for players we have no data for (e.g. not yet synced). */
    public static final ControlView NEUTRAL = new ControlView(1.0F, 0.0F, 0.0F, UrgeType.NONE, 0L, 0, 0, 0.0F, false,
        ComboStyle.WHIP, -1, 0L, 0, 0, 0.0F, false);
    /** A few ticks after a swing's end where the arms are still settling. */
    public static final int COMBO_SETTLE_TICKS = 4;

    static ControlView of(S2CControlState message) {
        return new ControlView(
            ControlMath.clamp01(message.control()),
            ControlMath.clamp01(message.hunger()),
            ControlMath.clamp01(message.frenzy()),
            UrgeType.byOrdinal(message.urgeType()),
            message.urgeStarted(),
            message.urgeDuration(),
            message.urgeTargetId(),
            message.urgeStrength(),
            message.urgeRestrained(),
            ComboStyle.byOrdinal(message.comboStyle()),
            message.comboStage(),
            message.comboStarted(),
            message.comboDuration(),
            message.comboLimbMask(),
            message.comboDeviation(),
            message.comboInvoluntary());
    }

    public float struggle() {
        return ControlMath.struggle(this.control, this.hunger, this.frenzy);
    }

    public float instinct() {
        return ControlMath.instinct(this.control, this.hunger, this.frenzy);
    }

    public float bloodlust() {
        return ControlMath.bloodlust(this.control, this.hunger, this.frenzy);
    }

    public boolean hasCombo() {
        return this.comboStage >= 0 && this.comboDuration > 0;
    }

    /**
     * True only while a combo swing is actually playing at {@code worldTime}. The server keeps reporting the last stage after it
     * ends (so a chain can continue), which makes {@link #hasCombo()} alone stay true forever after the first swing.
     */
    public boolean comboPlaying(double worldTime) {
        return this.hasCombo() && worldTime >= this.comboStarted && worldTime - this.comboStarted < this.comboDuration + COMBO_SETTLE_TICKS;
    }

    public ComboStage stage() {
        return this.comboStyle.stage(Math.max(0, this.comboStage));
    }

    /** True while this combo data describes the swipe the main kagune state is showing. */
    public boolean describesSwipe(long modeStarted) {
        return this.hasCombo() && this.comboStarted == modeStarted;
    }

    public boolean comboUsesLimb(int limb) {
        return limb >= 0 && limb < 31 && (this.comboLimbMask & 1 << limb) != 0;
    }

    /** Urge age in ticks at {@code worldTime}, or -1 if no urge is active then. */
    public float urgeAge(double worldTime) {
        if (this.urge == UrgeType.NONE) {
            return -1.0F;
        }
        double age = worldTime - this.urgeStarted;
        return age < 0.0 || age > this.urgeDuration + 6.0 ? -1.0F : (float)age;
    }
}
