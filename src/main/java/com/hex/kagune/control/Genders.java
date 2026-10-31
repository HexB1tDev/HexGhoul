package com.hex.kagune.control;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.9: every player's gender as this side knows it (the server from their profile, {@code GenderServer}; a client from what the
 * server sends, {@code GenderClient}), and what it makes of a woman's body on both sides alike: her box and her eyes
 * {@link FemaleBody#SIZE} as big (Forge's own size event, so she fits and is hit where she is drawn). Both sides must agree, or her
 * own client would move a box the server does not.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class Genders {
    private static final Map<UUID, Gender> KNOWN = new ConcurrentHashMap<>();

    private Genders() {
    }

    public static Gender of(UUID id) {
        return id == null ? Gender.UNSET : KNOWN.getOrDefault(id, Gender.UNSET);
    }

    /** True for a player who picked female (an NPC's body is its own kind's). */
    public static boolean female(Entity entity) {
        return entity instanceof Player && of(entity.m_20148_()).female();
    }

    public static void put(UUID id, Gender gender) {
        if (id == null) {
            return;
        }
        if (gender == null || gender == Gender.UNSET) {
            KNOWN.remove(id);
        } else {
            KNOWN.put(id, gender);
        }
    }

    public static void clear() {
        KNOWN.clear();
    }

    /** A woman's box and eyes, a woman's size (every pose: standing, crouching, swimming...). */
    @SubscribeEvent
    public static void size(EntityEvent.Size event) {
        if (female(event.getEntity())) {
            event.setNewSize(event.getNewSize().m_20388_(FemaleBody.SIZE), false);
            event.setNewEyeHeight(event.getNewEyeHeight() * FemaleBody.SIZE);
        }
    }
}
