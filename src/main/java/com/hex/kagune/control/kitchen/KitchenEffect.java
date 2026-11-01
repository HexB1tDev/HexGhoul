package com.hex.kagune.control.kitchen;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Home Living (1.3.40): one of the kitchen's own effects (Refreshed, Replenished, Content, Clear Sight): a good one, in its colour. It
 * does nothing by itself; the server reads who has it every tick and does what it says ({@code KitchenServer}), and the client clears
 * the fog for Clear Sight ({@code KitchenClient}).
 */
public final class KitchenEffect extends MobEffect {
    public KitchenEffect(int colour) {
        super(MobEffectCategory.BENEFICIAL, colour);
    }
}
