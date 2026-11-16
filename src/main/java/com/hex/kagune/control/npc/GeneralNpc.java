package com.hex.kagune.control.npc;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 4.1.5: the CCG's General, placed with the CCG building (one of him in a world), back in it a day after he is killed. Everything else
 * is every companion's: the mind, the talk, the courtship ({@link EtoNpc}, {@link Persona#GENERAL}).
 */
public class GeneralNpc extends EtoNpc {
    public GeneralNpc(EntityType<? extends EtoNpc> type, Level level) {
        super(type, level);
    }

    @Override
    public Persona persona() {
        return Persona.GENERAL;
    }
}
