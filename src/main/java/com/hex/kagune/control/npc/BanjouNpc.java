package com.hex.kagune.control.npc;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 4.1.54: Banjou, the Gas Masks' leader, placed in the cottage of their hideout (one of him in a world), back in it a day after he is
 * killed. Everything else is every companion's: the mind, the talk, the courtship ({@link EtoNpc}, {@link Persona#BANJOU}); his one small
 * blue wing slaps and nothing more ({@code NpcWings}).
 */
public class BanjouNpc extends EtoNpc {
    public BanjouNpc(EntityType<? extends EtoNpc> type, Level level) {
        super(type, level);
    }

    @Override
    public Persona persona() {
        return Persona.BANJOU;
    }
}
