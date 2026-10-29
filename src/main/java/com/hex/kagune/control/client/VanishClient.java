package com.hex.kagune.control.client;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.client.render.PlayerLayers;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/**
 * 6.0.5: the kakuja's vanish ({@code KakujaPassive}) leaves no trail on the ground: no dirt kicked up as he sprints. Vanilla
 * throws it from {@code Entity.baseTick} whenever an entity is sprinting (invisible or not), and Forge has no hook there, so on
 * every client a vanished player's sprint flag is held off over just the stretch of his tick that throws it: from the start of
 * his tick to his movement input ({@code LocalPlayer.aiStep} fires it after {@code baseTick} and before his sprint logic) for
 * the player at the keys, or to the end of his tick for anyone else. Outside that stretch the flag is his as ever: his speed, his
 * sprinting, what the server hears. Not in water, where the flag keeps him swimming (and vanilla throws no dirt anyway).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class VanishClient {
    /** Entity's shared flag for sprinting. */
    static final int SPRINTING = 3;
    private static final Set<UUID> HELD = new HashSet<>();
    private static Method sharedFlag;
    private static boolean unavailable;

    private VanishClient() {
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof AbstractClientPlayer player)) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            if (player.m_20142_() && !player.m_20069_() && PlayerLayers.vanished(player) && flag(player, false)) {
                HELD.add(player.m_20148_());
            }
        } else {
            release(player);
        }
    }

    /** The player at the keys: his sprint flag back before his own sprint logic runs. */
    @SubscribeEvent
    public static void movementInput(MovementInputUpdateEvent event) {
        if (event.getEntity() instanceof AbstractClientPlayer player) {
            release(player);
        }
    }

    private static void release(AbstractClientPlayer player) {
        if (HELD.remove(player.m_20148_())) {
            flag(player, true);
        }
    }

    /** Sets the entity's sprint flag ({@code Entity.setSharedFlag}, protected); false if it can't be reached (then nothing is held). */
    private static boolean flag(Entity entity, boolean sprinting) {
        if (unavailable) {
            return false;
        }
        try {
            if (sharedFlag == null) {
                sharedFlag = ObfuscationReflectionHelper.findMethod(Entity.class, "m_20115_", int.class, boolean.class);
            }
            sharedFlag.invoke(entity, SPRINTING, sprinting);
            return true;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            unavailable = true;
            KaguneMod.LOGGER.info("HexKagune can't hide a vanished player's sprint particles (" + failure + ")");
            return false;
        }
    }
}
