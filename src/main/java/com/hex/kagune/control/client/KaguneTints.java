package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.KaguneColours;
import com.hex.kagune.control.KaguneTint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;

/**
 * 1.3.35: the colour a body's kagune is drawn in on this client: a player's own (their profile's, rolled by KaguneColours and sent to
 * everyone with the original's state), a wild ghoul's from the seed it was born with (KaguneTint.wild); anyone else's the original
 * crimson (drawn as it always was).
 */
public final class KaguneTints {
    private KaguneTints() {
    }

    /** The kagune colour of the entity with id {@code id} in this client's world (the crimson if none, or not known). */
    public static int of(int id) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (id < 0 || minecraft.f_91073_ == null) {
            return KaguneColours.DEFAULT;
        }
        return of(minecraft.f_91073_.m_6815_(id));
    }

    /** {@code entity}'s kagune colour. */
    public static int of(Entity entity) {
        try {
            if (entity instanceof AbstractClientPlayer player) {
                return KaguneClientState.get(player).color() & 0xFFFFFF;
            }
            if (entity instanceof com.hex.kagune.control.npc.GhoulNpc npc) {
                GhoulNpcClient.View view = GhoulNpcClient.get(npc.m_19879_());
                return view == null ? KaguneColours.DEFAULT : KaguneTint.wild(view.identity().seed());
            }
        } catch (RuntimeException failure) {
            // (unreadable: drawn as it always was)
        }
        return KaguneColours.DEFAULT;
    }
}
