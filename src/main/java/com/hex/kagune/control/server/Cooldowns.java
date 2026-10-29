package com.hex.kagune.control.server;

import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 6.2.0: {@code /kagune cooldowns off|on}. With cooldowns off, every ability, kakuja and mutation is ready again the moment it is
 * over: each cooldown site asks {@link #of} for its wait (none while they are off), what a move takes to play out stays (a move
 * never starts over itself), and turning them off ends every running cooldown at once. The switch lives in the kagune profile
 * (kept through death and relogging, cleared by /kagune reset). Energy is not a cooldown: the RC bar is untouched. 1.0.33: in
 * creative they are always off (and the running ones end the moment the game mode changes to it); the bar is free there too
 * ({@link SharedBar}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class Cooldowns {
    /** Profile key: cooldowns are off for this player. */
    static final String KEY = "HexCooldownsOff";

    private Cooldowns() {
    }

    /** Players seen in creative last tick (to end their running cooldowns the tick they switch to it). */
    private static final java.util.Set<java.util.UUID> CREATIVE = new java.util.HashSet<>();

    /** True while cooldowns are off for {@code player}: switched off, or in creative. */
    public static boolean off(ServerPlayer player) {
        return player.m_7500_() || switchedOff(player);
    }

    /** True while {@code player} has switched cooldowns off ({@code /kagune cooldowns off}). */
    static boolean switchedOff(ServerPlayer player) {
        return KaguneProfile.root(player).m_128471_(KEY);
    }

    /** {@code ticks} of cooldown, or none while cooldowns are off for {@code player}. */
    static long of(ServerPlayer player, long ticks) {
        return off(player) ? 0L : ticks;
    }

    /** As above, in ticks as an int. */
    static int of(ServerPlayer player, int ticks) {
        return off(player) ? 0 : ticks;
    }

    /** Cooldowns off (every running one ended now) or back on. */
    static void set(ServerPlayer player, boolean off) {
        KaguneProfile.root(player).m_128379_(KEY, off);
        if (off) {
            clear(player);
        }
    }

    /** Every running cooldown of {@code player}'s, over now (the menus told). */
    static void clear(ServerPlayer player) {
        long now = player.m_9236_().m_46467_();
        EtoServer.clearCooldowns(player);
        FurutaServer.clearCooldowns(player);
        EtoRevive.clearCooldown(player);
        AyatoServer.clearCooldowns(player);
        AyatoRush.forget(player);
        KaguneCombat.clearCooldowns(player);
        KakujaPassive.clearCooldowns(player);
        KingServer.clearCooldowns(player);
        original(player, now);
    }

    /**
     * The original's own cooldowns (Kaneki's crush, his slingshot, the fall arrest): its code sets them, so while cooldowns are off
     * they are ended every tick. Its input lock is not one: it is how long a move takes.
     */
    private static void original(ServerPlayer player, long now) {
        KaguneRuntime.State state = KaguneRuntime.peek(player.m_20148_());
        if (state == null) {
            return;
        }
        state.crushCooldownUntil = Math.min(state.crushCooldownUntil, now);
        state.fallArrestCooldownUntil = Math.min(state.fallArrestCooldownUntil, now);
        state.slingshotCooldownUntil = Math.min(state.slingshotCooldownUntil, now);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!player.m_7500_()) {
            CREATIVE.remove(player.m_20148_());
        } else if (CREATIVE.add(player.m_20148_())) {
            clear(player);
        }
        if (off(player)) {
            original(player, player.m_9236_().m_46467_());
        }
    }
}
