package com.hex.kagune.control.server;

import com.hex.kagune.control.ThyDeath;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CThyDeath;
import com.hex.kagune.data.KaguneProfile;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 2.4.5: Rinkaku's Thy Death on the server ({@link ThyDeath}). Z pressed (the original's Rapid Strike key, through {@code UkakuVortex})
 * or held (the client's pings) keeps it going; let go, or {@link ThyDeath#HELD} ticks without a ping, it ends. Meanwhile each tail out,
 * staggered, picks its own foe (a different one from the others where it can) and lunges; every client is told ({@link S2CThyDeath}) and
 * draws the lunge; the blow lands as its point goes through. Its foes: monsters, anything after the player or that hurt them, investigators,
 * wild ghouls not their own. Never a companion, a child, a villager, an animal, or another player who hasn't hurt them.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class ThyDeathServer {
    private ThyDeathServer() {
    }

    private static final int MOST = 8;

    private static final class State {
        long heldUntil;
        final long[] next = new long[MOST];
        final int[] target = new int[MOST];
        final long[] start = new long[MOST];
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    /** Z: {@code phase} 2 pressed, 1 still held, 0 let go. */
    static void input(ServerPlayer player, int phase) {
        long now = player.m_9236_().m_46467_();
        if (phase == 0) {
            stop(player);
            return;
        }
        if (!allowed(player)) {
            return;
        }
        State state = STATES.get(player.m_20148_());
        if (state == null) {
            state = new State();
            for (int i = 0; i < MOST; i++) {
                state.next[i] = ThyDeath.first(now, i);
            }
            STATES.put(player.m_20148_(), state);
        }
        state.heldUntil = now + ThyDeath.HELD;
    }

    /** True if {@code player} can use it now: a Rinkaku ghoul with the kagune out, alive. */
    static boolean allowed(ServerPlayer player) {
        return player.m_6084_() && KaguneProfile.isActive(player) && PathServer.path(player) == com.hex.kagune.control.KagunePath.RINKAKU
            && !UkakuServer.isUkaku(player) && !PaleServer.isPale(player) && !GraftServer.isGraft(player);
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !(player.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        State state = STATES.get(player.m_20148_());
        if (state == null) {
            return;
        }
        long now = level.m_46467_();
        if (now > state.heldUntil || !allowed(player)) {
            stop(player);
            return;
        }
        try {
            int mastery = KaguneProfile.mastery(player);
            int limbs = Math.min(MOST, Math.max(1, KaguneProfile.manifestedLimbs(player)));
            double range = ThyDeath.range(mastery);
            for (int i = 0; i < limbs; i++) {
                if (state.target[i] != 0 && now - state.start[i] == ThyDeath.HIT_AT) {
                    hit(player, level, state.target[i], ThyDeath.damage(mastery), range);
                }
                if (now < state.next[i]) {
                    continue;
                }
                LivingEntity foe = pick(player, level, range, state, i, limbs);
                if (foe == null) {
                    state.target[i] = 0;
                    state.next[i] = now + 5L;
                    continue;
                }
                state.target[i] = foe.m_19879_();
                state.start[i] = now;
                state.next[i] = now + ThyDeath.STRIKE + ThyDeath.REST;
                ControlNetwork.broadcastAround(player, new S2CThyDeath(player.m_19879_(), i, foe.m_19879_(), now));
            }
        } catch (RuntimeException failure) {
            com.hex.kagune.control.Safety.caught(failure, "Thy Death");
            stop(player);
        }
    }

    /** It ends: every tail back (told to every client). */
    static void stop(ServerPlayer player) {
        if (STATES.remove(player.m_20148_()) != null) {
            ControlNetwork.broadcastAround(player, new S2CThyDeath(player.m_19879_(), 0, 0, player.m_9236_().m_46467_()));
        }
    }

    private static void hit(ServerPlayer player, ServerLevel level, int id, float damage, double range) {
        if (level.m_6815_(id) instanceof LivingEntity foe && foe.m_6084_() && foe.m_20280_(player) <= (range + 2.0) * (range + 2.0)) {
            foe.m_6469_(player.m_269291_().m_269333_(player), damage);
            if (level.m_46467_() % 4L == 0L) {
                KaguneProfile.addMastery(player, "Swipe", 1);
            }
        }
    }

    /** Tail {@code limb}'s foe: the nearest one no other tail is lunging at, else the nearest. */
    private static LivingEntity pick(ServerPlayer player, ServerLevel level, double range, State state, int limb, int limbs) {
        LivingEntity free = null;
        LivingEntity any = null;
        double freeBest = Double.MAX_VALUE;
        double anyBest = Double.MAX_VALUE;
        for (LivingEntity body : level.m_6443_(LivingEntity.class, player.m_20191_().m_82400_(range), e -> foe(player, e))) {
            double d = body.m_20280_(player);
            if (d > range * range) {
                continue;
            }
            boolean taken = false;
            for (int j = 0; j < limbs; j++) {
                taken |= j != limb && state.target[j] == body.m_19879_();
            }
            if (!taken && d < freeBest) {
                freeBest = d;
                free = body;
            }
            if (d < anyBest) {
                anyBest = d;
                any = body;
            }
        }
        return free != null ? free : any;
    }

    /** True if {@code body} is something Thy Death's tails go for. */
    static boolean foe(ServerPlayer player, LivingEntity body) {
        if (body == player || !body.m_6084_() || body.m_5833_() || body instanceof com.hex.kagune.control.npc.EtoNpc
            || body instanceof com.hex.kagune.control.npc.ChildNpc) {
            return false;
        }
        boolean hurtThem = player.m_21188_() == body;
        if (body instanceof Player other) {
            return hurtThem && !other.m_7500_();
        }
        if (body instanceof com.hex.kagune.control.npc.HumanNpc) {
            return hurtThem;
        }
        if (body instanceof com.hex.kagune.control.npc.GhoulNpc ghoul) {
            return ghoul.mind == null || !player.m_20148_().equals(ghoul.mind.owner);
        }
        if (body instanceof com.hex.kagune.control.npc.CcgAgent) {
            return true;
        }
        return hurtThem || body instanceof net.minecraft.world.entity.monster.Enemy
            || body instanceof net.minecraft.world.entity.Mob mob && mob.m_5448_() == player;
    }
}
