package com.hex.kagune.control.npc;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 1.3.14: Touka: the rabbit. Ukaku; strict, serious, quick to pout; teaches the wings ({@link Teaching}). Spawned by command for now (her cafe is coming). Everything else is every companion's: the
 * mind, the talk, the dating and the marriage ({@link EtoNpc}, {@link Persona}).
 */
public class ToukaNpc extends EtoNpc {
    public ToukaNpc(EntityType<? extends EtoNpc> type, Level level) {
        super(type, level);
    }

    @Override
    public Persona persona() {
        return Persona.TOUKA;
    }
}
