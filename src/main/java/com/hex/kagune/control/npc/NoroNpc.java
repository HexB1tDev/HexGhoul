package com.hex.kagune.control.npc;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 4.0.0 (The Great Exploration): Noro, Aogiri Tree's, placed with every Aogiri hideout (one of him in a world: the first a player meets;
 * the rest sent away), back at his hideout a day after he is killed. Everything else is every companion's: the mind, the talk, the
 * lessons ({@link EtoNpc}, {@link Persona#NORO}).
 */
public class NoroNpc extends EtoNpc {
    public NoroNpc(EntityType<? extends EtoNpc> type, Level level) {
        super(type, level);
    }

    @Override
    public Persona persona() {
        return Persona.NORO;
    }
}
