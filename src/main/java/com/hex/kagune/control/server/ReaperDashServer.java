package com.hex.kagune.control.server;

import com.hex.kagune.control.ReaperDash;
import com.hex.kagune.control.network.S2CUkakuFx;
import com.hex.kagune.data.KaguneProfile;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 2.1.7: the White Reaper's dash for Ukaku's final kakuja ({@link ReaperDash}): the sprint key tapped twice (the client says which way it
 * was moving) spends one of three charges and throws them that way, fast and low, as he does; one comes back every five seconds. The action
 * bar says how many are left ("Dash 2/3") as one is spent and as each comes back.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class ReaperDashServer {
    private ReaperDashServer() {
    }

    /** A player's charges, and the dash under way (until when, which way). */
    private static final class State {
        final ReaperDash.Charges charges = new ReaperDash.Charges();
        long until = Long.MIN_VALUE;
        Vec3 way = Vec3.f_82478_;
        int shown = ReaperDash.CHARGES;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    /** True while {@code player} has the dashes: Ukaku, the kagune out, in the final kakuja. */
    static boolean allowed(ServerPlayer player) {
        return player.m_6084_() && UkakuServer.isUkaku(player) && KaguneProfile.isActive(player) && UkakuServer.form(player).finalKakuja();
    }

    /** The double tap, {@code tenths} the way they were moving (tenths of a degree of world yaw: {@link ReaperDash#encode}). */
    static void dash(ServerPlayer player, int tenths) {
        if (!allowed(player)) {
            return;
        }
        State state = STATES.computeIfAbsent(player.m_20148_(), id -> new State());
        long now = player.m_9236_().m_46467_();
        if (Cooldowns.off(player)) {
            state.charges.fill();
        }
        if (now < state.until) {
            return; // one at a time
        }
        if (!state.charges.spend(now)) {
            player.m_5661_(Component.m_237113_("§7" + ReaperDash.bar(0)), true);
            return;
        }
        double[] way = ReaperDash.decode(tenths);
        state.way = new Vec3(way[0], 0.0, way[1]);
        state.until = now + ReaperDash.TICKS;
        push(player, state);
        int ready = state.charges.ready(now);
        state.shown = ready;
        player.m_5661_(Component.m_237113_("§f" + ReaperDash.bar(ready)), true);
        Vec3 at = player.m_20182_();
        UkakuFire.fx(player, S2CUkakuFx.GUST, 0, at, state.way.m_82490_(ReaperDash.SPEED), 1.0F, 0);
        UkakuFire.sound(player.m_284548_(), at, "hexkagune:sfx.dash", 0.9F, 1.1F);
    }

    private static void push(ServerPlayer player, State state) {
        Vec3 motion = player.m_20184_();
        player.m_20256_(new Vec3(state.way.f_82479_ * ReaperDash.SPEED, Math.min(motion.f_82480_, player.m_20096_() ? 0.08 : 0.02),
            state.way.f_82481_ * ReaperDash.SPEED));
        player.f_19864_ = true;
        player.f_19789_ = 0.0F;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        State state = STATES.get(player.m_20148_());
        if (state == null) {
            return;
        }
        long now = player.m_9236_().m_46467_();
        if (now < state.until && player.m_6084_()) {
            push(player, state);
        }
        int ready = state.charges.ready(now);
        if (ready != state.shown) {
            // One came back: said, while the dashes are still his.
            state.shown = ready;
            if (allowed(player)) {
                player.m_5661_(Component.m_237113_("§f" + ReaperDash.bar(ready)), true);
            }
        }
        if (ready >= ReaperDash.CHARGES && now >= state.until && !allowed(player)) {
            STATES.remove(player.m_20148_());
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().m_20148_());
    }
}
