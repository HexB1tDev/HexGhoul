package com.hex.kagune.control.village;

import com.hex.kagune.control.npc.EtoHand;
import com.hex.kagune.control.npc.EtoRules;

/**
 * 1.3.78: hand in hand with a villager on a date (pure): where it walks, glued at its partner's right side as Eto does ({@link EtoHand}),
 * but at a full-size body's reach: both hands turned out {@link EtoHand#ARM} from their sides so they meet, the villager's hanging from a
 * slim or a wide arm, its partner's from a man's arm or a woman's (drawn at Eto's size, the one female body's).
 */
public final class HumanHand {
    private HumanHand() {
    }

    /** How far out from its middle a hand hangs: a wide arm's (0.375) or a slim one's (0.34), turned out ARM, the arm 0.75 long. */
    public static double hand(boolean slim) {
        return (slim ? 0.34 : 0.375) + 0.75 * Math.sin(EtoHand.ARM);
    }

    /** The holder's hand: a woman's as the female body draws it (Eto's size), a man's a wide arm's. */
    public static double holder(boolean woman) {
        return woman ? hand(true) * EtoRules.SIZE : hand(false);
    }

    /** How far apart the two stand so their hands meet. */
    public static double out(boolean villagerSlim, boolean holderWoman) {
        return hand(villagerSlim) + holder(holderWoman);
    }

    /** The offset {x, z} from the holder (facing {@code bodyYaw} degrees) to where the villager stands, {@code out} to their right. */
    public static double[] offset(float bodyYaw, double out) {
        double yaw = Math.toRadians(bodyYaw);
        return new double[] {-Math.cos(yaw) * out, -Math.sin(yaw) * out};
    }
}
