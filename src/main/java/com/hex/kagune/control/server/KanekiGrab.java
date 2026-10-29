package com.hex.kagune.control.server;

import com.hex.kagune.control.ClashRules;
import com.hex.kagune.control.GrabRules;
import com.hex.kagune.control.Safety;
import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.gameplay.TendrilMode;
import com.hex.kagune.network.KaguneNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.0.42: Kaneki's grab (G), slower and breakable ({@link GrabRules}). The original's tendril took 9 ticks to reach and started
 * dragging its catch in from its 3rd: no one could do a thing about it. Now it takes twice as long ({@link #slow}, just after the
 * original sets it out), reels in only over its last ticks ({@link #reelFrom}, the original's {@code tickReachEntity} asks), and
 * the one it reaches for can break it by swinging back at him: a kagune M1 (any path's, {@link KaguneClash}) or a player's bare
 * swing, facing him ({@link #broken}, asked first thing every tick of the reach). Broken, the tendril goes back, the wind of a clash
 * blows out in front of its catch, who is thrown clear, and he waits a moment before he can act again.
 *
 * <p>1.0.43: a parry, not a button to mash ({@link GrabRules#inWindow}): only the first swing at him since it set out is judged
 * (one swung another way is let be), and only one in its last ticks breaks it. Its window opening is heard and seen (a bell and a glint in front of the one it reaches for).
 */
public final class KanekiGrab {
    /**
     * Each grab on its way, by its Kaneki: when it set out, whether its one chance to be broken is gone, and the last swing looked at
     * (one swung away from him is let be: not an answer to the grab, not a chance spent).
     */
    private record Judged(long began, boolean spent, long seen) {
    }

    private static final java.util.Map<java.util.UUID, Judged> JUDGED = new java.util.HashMap<>();

    private KanekiGrab() {
    }

    /** Just after the original set a grab out on {@code player}'s tendril this tick: the reach made slower, and every client told. */
    public static void slow(ServerPlayer player) {
        try {
            KaguneRuntime.State state = KaguneRuntime.get(player.m_20148_());
            long now = player.m_9236_().m_46467_();
            if (state != null && state.mode == TendrilMode.REACH_ENTITY && state.modeStarted == now) {
                state.modeDuration = GrabRules.reach(state.modeDuration);
                KaguneNetwork.broadcast(player);
            }
        } catch (RuntimeException failure) {
            Safety.caught(failure, "slowing Kaneki's grab");
        }
    }

    /** The tick of the reach that reeling in starts on (the original's {@code tickReachEntity} compares the reach's age with this). */
    public static long reelFrom(KaguneRuntime.State state) {
        return GrabRules.reelFrom(state.modeDuration);
    }

    /**
     * First thing every tick of {@code player}'s grab on its way: true if the one it reaches for has swung back at him since it set
     * out (the grab is then broken here, and the original's tick must do nothing more).
     */
    public static boolean broken(ServerPlayer player, KaguneRuntime.State state, long now) {
        try {
            Entity caught = player.m_284548_().m_6815_(state.actionTargetId);
            if (!(caught instanceof LivingEntity body) || body == player || !body.m_6084_()) {
                return false;
            }
            java.util.UUID id = player.m_20148_();
            Judged judged = JUDGED.get(id);
            if (judged == null || judged.began() != state.modeStarted) {
                if (JUDGED.size() > 64) {
                    JUDGED.values().removeIf(old -> now > old.began() + 200L);
                }
                judged = new Judged(state.modeStarted, false, Long.MIN_VALUE);
                JUDGED.put(id, judged);
            }
            if (now - state.modeStarted == GrabRules.windowOpens(state.modeDuration)) {
                cue(player, body);
            }
            if (judged.spent()) {
                return false;
            }
            long swung = swing(body, state.modeStarted, now);
            if (swung == Long.MIN_VALUE || swung == judged.seen()) {
                return false;
            }
            // Its first swing at him since it set out: in the window it breaks it; too early, and that was its one chance.
            Vec3 look = body.m_20154_();
            if (!ClashRules.faces(look.f_82479_, look.f_82481_, player.m_20185_() - body.m_20185_(), player.m_20189_() - body.m_20189_())) {
                JUDGED.put(id, new Judged(state.modeStarted, false, swung));
                return false;
            }
            if (!GrabRules.inWindow(state.modeStarted, state.modeDuration, swung)) {
                JUDGED.put(id, new Judged(state.modeStarted, true, swung));
                return false;
            }
            JUDGED.remove(id);
            state.pendingGrabSlot = -1;
            KaguneServerController.returnToHoldingOrRetract(player, state, now);
            state.inputCooldownUntil = Math.max(state.inputCooldownUntil, now + GrabRules.STAGGER);
            Vec3 chest = body.m_20182_().m_82520_(0.0, body.m_20206_() * 0.6, 0.0);
            Vec3 toward = player.m_20182_().m_82520_(0.0, player.m_20206_() * 0.6, 0.0).m_82546_(chest);
            Vec3 way = toward.m_82556_() < 1.0E-6 ? body.m_20154_() : toward.m_82541_();
            // Where the blow met the tendril: an arm's length in front of the one it reached for.
            KaguneClash.burst(body, chest.m_82549_(way.m_82490_(Math.min(1.2, toward.m_82553_() * 0.5))), way, 0.9F, 1.1F);
            double dx = -way.f_82479_;
            double dz = -way.f_82481_;
            double flat = Math.sqrt(dx * dx + dz * dz);
            if (flat > 1.0E-4) {
                body.m_20256_(new Vec3(dx / flat * GrabRules.PUSH, GrabRules.LIFT, dz / flat * GrabRules.PUSH));
                body.f_19864_ = true;
                body.f_19789_ = 0.0F;
            }
            KaguneNetwork.broadcast(player);
            return true;
        } catch (RuntimeException failure) {
            Safety.caught(failure, "breaking Kaneki's grab");
            return false;
        }
    }

    /** 1.0.43: the window to break it opening: a bell, and a glint an arm's length in front of the one it reaches for, toward him. */
    private static void cue(ServerPlayer player, LivingEntity body) {
        Vec3 chest = body.m_20182_().m_82520_(0.0, body.m_20206_() * 0.6, 0.0);
        Vec3 toward = player.m_20182_().m_82520_(0.0, player.m_20206_() * 0.6, 0.0).m_82546_(chest);
        Vec3 way = toward.m_82556_() < 1.0E-6 ? body.m_20154_() : toward.m_82541_();
        Vec3 at = chest.m_82549_(way.m_82490_(Math.min(1.2, toward.m_82553_() * 0.5)));
        float[] data = {(float)at.f_82479_, (float)at.f_82480_, (float)at.f_82481_, (float)way.f_82479_, (float)way.f_82480_, (float)way.f_82481_, 0.7F,
            0.0F, 0.0F};
        com.hex.kagune.control.network.ControlNetwork.broadcastAround(body, new com.hex.kagune.control.network.S2CKingFx(body.m_20148_(),
            com.hex.kagune.control.network.S2CKingFx.MARK, new int[] {com.hex.kagune.control.king.KingMarkFx.GLINT, 0}, data));
        AyatoFire.sound(player.m_284548_(), at, "block.note_block.bell", 0.9F, 1.8F);
    }

    /**
     * The tick {@code body}'s first swing since {@code began} set out, as far as is seen by {@code now} (a kagune M1, or a player's
     * bare swing), or {@link Long#MIN_VALUE} if none yet. (Asked every tick: the first one seen is the first one swung.)
     */
    private static long swing(LivingEntity body, long began, long now) {
        long kagune = KaguneClash.lastSwing(body);
        if (GrabRules.counts(began, kagune, now)) {
            return kagune;
        }
        if (body instanceof ServerPlayer && body.f_20911_ && body.f_20913_ <= GrabRules.FRESH_SWING) {
            long bare = now - Math.max(0, body.f_20913_);
            if (GrabRules.counts(began, bare, now)) {
                return bare;
            }
        }
        return Long.MIN_VALUE;
    }
}
