package com.hex.kagune.control.npc;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 1.3.14: Ken: the one-eyed king. Rinkaku; quiet, polite, a little eerie; teaches the kakujas and the final forms through what was done to him ({@link Teaching}). Spawned by command for now. Everything else is every companion's: the
 * mind, the talk, the dating and the marriage ({@link EtoNpc}, {@link Persona}).
 */
public class KenNpc extends EtoNpc {
    public KenNpc(EntityType<? extends EtoNpc> type, Level level) {
        super(type, level);
    }

    @Override
    public Persona persona() {
        return Persona.KEN;
    }
}
