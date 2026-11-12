package com.hex.kagune.control.npc;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 4.1.49: Roma, the Clowns' leader, placed in the middle of their circus tent (one of her in a world), back in it a day after she is
 * killed. Everything else is every companion's: the mind, the talk, the courtship ({@link EtoNpc}, {@link Persona#ROMA}); her legs fight
 * Kick Master's way and her four purple tendrils their own ({@code NpcTendrils}).
 */
public class RomaNpc extends EtoNpc {
    public RomaNpc(EntityType<? extends EtoNpc> type, Level level) {
        super(type, level);
    }

    @Override
    public Persona persona() {
        return Persona.ROMA;
    }
}
