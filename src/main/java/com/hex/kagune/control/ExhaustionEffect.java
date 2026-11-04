package com.hex.kagune.control;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 2.1.1 (the owner: "Final form (Peace or War) now have a 5 minute cooldown, reflected as the new custom effect Exaustion, with a custom 2d
 * image as well"): Exhaustion, the burnt-out rest after a final kakuja (War or Peace) is let go. It does nothing by itself: the rest is
 * kept on the player ({@code KingRules.RESTS}) and the server keeps this effect showing it while it lasts ({@code KingServer}), so milk, death
 * or a relog can't end it early. Its icon is {@code textures/mob_effect/exhaustion.png} ({@code tools/exhaustion_icon.py}).
 */
public final class ExhaustionEffect extends MobEffect {
    /** Its colour (a burnt, dark red-grey). */
    public static final int COLOUR = 0x5A2A34;

    public ExhaustionEffect() {
        super(MobEffectCategory.HARMFUL, COLOUR);
    }
}
