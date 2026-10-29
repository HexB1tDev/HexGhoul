package com.hex.kagune.control.server;

import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.pale.OwlLances;
import com.hex.kagune.control.pale.OwlRules;
import com.hex.kagune.control.pale.WatchRules;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CPaleFx;
import com.hex.kagune.control.network.S2COwlLance;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 6.2.0, the Owl's Watch This (B in it; {@link OwlLances}, {@link OwlRules}): hunched, she rears up and six lances of red muscle
 * tipped with barbed bone burst out of her back into a crown over her; for exactly ten seconds they hunt on their own. Each goes for
 * something hostile to her (a mob after her, or anything that hurt her lately, as Watch This's tentacles do), favouring its own way
 * round her: it rears up high and dives onto it from above, the spear through it and on into the ground (the hurt, a bleed, blood);
 * pinned there a moment, it is hoisted up over her on the barbs and slammed into the ground on her far side (hurt again, a crater,
 * stunned hard); the lance tears free and hunts on. Then they draw back into her. Every client draws them from {@link S2COwlLance};
 * what they pin and hoist is held where the lance has it each tick.
 */
public final class OwlWatch {
    /** One lance: which, what it is doing since when, at what, and its pin (the chest in her frame, the way she faced) and slam. */
    static final class Lance {
        final int index;
        OwlLances.Phase phase = OwlLances.Phase.NONE;
        long startedAt;
        int targetId;
        int dive;
        float yaw;
        /** Where it is pinned (the world: fixed to the ground there, wherever she goes). */
        Vec3 pin = Vec3.f_82478_;
        /** Its hoist: where its chest began and where it is slammed down, in her frame as she faced ({@code yaw}) when it was torn up. */
        double[] pinned = new double[3];
        double[] slam = new double[3];
        /** Where its point was when a slam or a miss began (the world). */
        Vec3 point = Vec3.f_82478_;

        Lance(int index) {
            this.index = index;
        }
    }

    /** Her lances: when they burst out, and the bodies just slammed (by entity id: free again from that tick). */
    static final class Crown {
        final long out;
        /** When they start back in: ten seconds after the burst, or sooner if the Owl goes. */
        long end;
        final Lance[] lances = new Lance[OwlLances.COUNT];
        final Map<Integer, Long> slammed = new HashMap<>();

        Crown(long out) {
            this.out = out;
            this.end = out + OwlRules.LANCES_ACTIVE;
            for (int i = 0; i < OwlLances.COUNT; i++) {
                this.lances[i] = new Lance(i);
            }
        }
    }

    private static final Map<UUID, Crown> CROWNS = new HashMap<>();

    private OwlWatch() {
    }

    /** B in the Owl: the lances, if she can (her mastery, her RC, not already out, nothing else going on). */
    static void start(ServerPlayer player, PaleServer.PaleState state, long now) {
        if (CROWNS.containsKey(player.m_20148_()) || !PaleCancel.allowedOwl(state, now) || now < state.watchReadyAt
            || PaleServer.locked(player, state, "Watch This", PaleRules.WATCH_MASTERY)) {
            return;
        }
        if (!KaguneServerController.useEnergy(player, WatchRules.COST, true)) {
            return;
        }
        PaleCancel.cut(player, state, now);
        state.queuedAt = Long.MIN_VALUE;
        PaleServer.begin(player, state, PaleMove.OWL_WATCH, 0, 0, 0.0F, null, now);
        Crown crown = new Crown(now + OwlRules.LANCES_ERUPT_AT);
        CROWNS.put(player.m_20148_(), crown);
        for (Lance lance : crown.lances) {
            set(player, lance, OwlLances.Phase.OUT, crown.out);
        }
        state.watchReadyAt = crown.out + OwlRules.LANCES_ACTIVE + OwlRules.LANCES_RETRACT + Cooldowns.of(player, WatchRules.COOLDOWN);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_STRAIN, 0.9F, KaguneServerController.vary(player, 0.6F, 0.05F));
    }

    /** Whether her lances are out (or coming out, or going back in). */
    static boolean out(ServerPlayer player) {
        return CROWNS.containsKey(player.m_20148_());
    }

    /** Each tick of hers: the lances burst out, hunt, dive, pin, hoist and slam; after ten seconds they go back in. */
    static void tick(ServerPlayer player, long now) {
        Crown crown = CROWNS.get(player.m_20148_());
        if (crown == null) {
            return;
        }
        if (now == crown.out) {
            // Torn out of her back all at once: bone cracking, the crown up over her.
            ServerLevel level = player.m_284548_();
            KaguneServerController.playSelf(player, HexKaguneSounds.MANIFEST_RIP, 1.0F, KaguneServerController.vary(player, 0.65F, 0.05F));
            KaguneServerController.playSelf(player, HexKaguneSounds.BONE_CRACK, 1.0F, KaguneServerController.vary(player, 0.75F, 0.05F));
            UkakuFire.sound(level, player.m_20182_(), "entity.slime.squish", 1.2F, KaguneServerController.vary(player, 0.55F, 0.05F));
        }
        long end = crown.end;
        if (now >= end) {
            for (Lance lance : crown.lances) {
                // (What one pins or hoists is let go where it is: nothing holds it any more.)
                if (lance.phase != OwlLances.Phase.IN) {
                    set(player, lance, OwlLances.Phase.IN, end);
                }
            }
            if (now >= end + OwlRules.LANCES_RETRACT) {
                for (Lance lance : crown.lances) {
                    set(player, lance, OwlLances.Phase.NONE, now);
                }
                CROWNS.remove(player.m_20148_());
            }
            return;
        }
        crown.slammed.values().removeIf(free -> now >= free);
        for (Lance lance : crown.lances) {
            step(player, crown, lance, now);
        }
    }

    private static void step(ServerPlayer player, Crown crown, Lance lance, long now) {
        long age = now - lance.startedAt;
        LivingEntity target = lance.targetId == 0 ? null
            : player.m_9236_().m_6815_(lance.targetId) instanceof LivingEntity living && living.m_6084_() ? living : null;
        switch (lance.phase) {
            case OUT -> {
                if (age >= OwlRules.LANCES_ERUPT) {
                    set(player, lance, OwlLances.Phase.IDLE, now);
                }
            }
            case IDLE -> {
                LivingEntity prey = hunt(player, crown, lance, now);
                if (prey != null) {
                    lance.targetId = prey.m_19879_();
                    lance.dive = OwlLances.diveTicks(PaleWatch.chest(prey).m_82554_(PaleWatch.world(player, player.m_146908_(), OwlLances.pivot(lance.index))));
                    set(player, lance, OwlLances.Phase.STRIKE, now);
                }
            }
            case STRIKE -> {
                if (age < OwlLances.strikeTicks(lance.dive)) {
                    return;
                }
                Vec3 pivot = PaleWatch.world(player, player.m_146908_(), OwlLances.pivot(lance.index));
                if (target == null || !PaleBlitz.valid(player, target) || PaleWatch.chest(target).m_82554_(pivot) > OwlRules.LANCE_REACH) {
                    // Gone, or got away: the spear goes into the ground where it was going.
                    Vec3 aim = target == null ? player.m_20182_().m_82549_(player.m_20154_().m_82490_(3.0)) : target.m_20182_();
                    lance.point = aim;
                    set(player, lance, OwlLances.Phase.MISS, now);
                    UkakuFire.sound(player.m_284548_(), aim, "block.gravel.break", 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
                    return;
                }
                pin(player, lance, target, now);
            }
            case PIN -> {
                if (target == null) {
                    lance.point = lance.pin;
                    set(player, lance, OwlLances.Phase.RECOVER, now);
                    return;
                }
                hold(player, target, lance.pin);
                if (age >= OwlRules.LANCE_PIN) {
                    hoist(player, lance, target, now);
                }
            }
            case HOIST -> {
                if (target == null) {
                    lance.point = PaleWatch.world(player, lance.yaw, lance.slam);
                    set(player, lance, OwlLances.Phase.RECOVER, now);
                    return;
                }
                double u = OwlLances.hoistU(age);
                hold(player, target, PaleWatch.world(player, lance.yaw, OwlLances.hoist(u, lance.pinned, lance.slam)));
                if (age >= OwlRules.LANCE_HOIST) {
                    slam(player, crown, lance, target, now);
                }
            }
            case RECOVER -> {
                if (age >= OwlRules.LANCE_RECOVER) {
                    set(player, lance, OwlLances.Phase.IDLE, now);
                }
            }
            case MISS -> {
                if (age >= OwlRules.LANCE_MISS) {
                    set(player, lance, OwlLances.Phase.IDLE, now);
                }
            }
            default -> {
            }
        }
    }

    /**
     * What this lance goes for: something near her (chest to chest, {@link OwlRules#LANCE_RANGE}) hostile to her, in her sight, not
     * another lance's, not just slammed; the nearest, favouring its own way round her.
     */
    static LivingEntity hunt(ServerPlayer player, Crown crown, Lance lance, long now) {
        ServerLevel level = player.m_284548_();
        Vec3 from = PaleWatch.chest(player);
        Vec3 eye = player.m_146892_();
        Map<Integer, Long> grudges = PaleWatch.grudges(player);
        double yaw = Math.toRadians(player.m_146908_());
        Vec3 ahead = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        double bearing = OwlLances.bearing(lance.index);
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        AABB area = player.m_20191_().m_82400_(OwlRules.LANCE_RANGE + 1.0);
        for (LivingEntity candidate : level.m_6443_(LivingEntity.class, area, entity -> PaleBlitz.valid(player, entity))) {
            int id = candidate.m_19879_();
            if (taken(crown, lance, id) || crown.slammed.containsKey(id) || !PaleWatch.hostile(player, candidate, grudges, now)) {
                continue;
            }
            Vec3 to = PaleWatch.chest(candidate).m_82546_(from);
            double distance = to.m_82553_();
            if (distance > OwlRules.LANCE_RANGE || !PaleMoves.seen(level, player, eye, candidate)) {
                continue;
            }
            double off = Math.atan2(to.m_82526_(right), to.m_82526_(ahead)) - bearing;
            off = Math.abs(off - Math.PI * 2.0 * Math.round(off / (Math.PI * 2.0)));
            double score = distance + off * 1.8;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    /** Whether another lance is already diving on, pinning or hoisting {@code id}. */
    private static boolean taken(Crown crown, Lance lance, int id) {
        for (Lance other : crown.lances) {
            if (other != lance && other.targetId == id && (other.phase == OwlLances.Phase.STRIKE || other.phase == OwlLances.Phase.PIN
                || other.phase == OwlLances.Phase.HOIST)) {
                return true;
            }
        }
        return false;
    }

    /** The spear down through it and into the ground: the hurt, a bleed, blood; pinned where it stands. */
    private static void pin(ServerPlayer player, Lance lance, LivingEntity target, long now) {
        ServerLevel level = player.m_284548_();
        Vec3 chest = PaleWatch.chest(target);
        float yaw = player.m_146908_();
        double[] dir = OwlLances.diveDir(PaleWatch.frame(player, yaw, chest));
        Vec3 through = PaleWatch.turn(yaw, dir);
        target.f_19802_ = 0;
        WoundHits.expect(target, com.hex.kagune.control.wound.WoundBook.Blow.STAB, chest.m_82546_(through.m_82490_(target.m_20205_() * 0.5)), through, 0.0);
        float damage = PaleRules.fist(KaguneProfile.mastery(player)) * OwlRules.LANCE_DAMAGE;
        if (!target.m_6469_(player.m_269291_().m_269075_(player), damage)) {
            lance.point = target.m_20182_();
            set(player, lance, OwlLances.Phase.MISS, now);
            return;
        }
        UkakuCrystalCombat.rinkakuBleed(target, player, now);
        PaleStun.stun(target, now, OwlRules.LANCE_PIN + OwlRules.LANCE_HOIST + 4);
        PaleBlitz.fx(player, S2CPaleFx.IMPALED, target.m_19879_(), chest, through, target.m_20206_(), OwlLances.side(lance.index));
        UkakuFire.sound(level, chest, "entity.player.attack.strong", 1.0F, KaguneServerController.vary(player, 0.55F, 0.05F));
        UkakuFire.sound(level, chest, "entity.slime.squish", 1.0F, KaguneServerController.vary(player, 0.65F, 0.05F));
        KaguneServerController.playAt(player, target, HexKaguneSounds.BONE_CRACK, 1.0F, KaguneServerController.vary(player, 0.8F, 0.05F));
        KaguneProfile.addMastery(player, "Swipe", 2);
        KaguneNetwork.broadcast(player);
        PaleServer.landed(player, 1);
        lance.pin = chest;
        set(player, lance, OwlLances.Phase.PIN, now);
        hold(player, target, chest);
    }

    /** Torn up out of the ground with it on the barbs: where it will be slammed down, on her far side, found now. */
    private static void hoist(ServerPlayer player, Lance lance, LivingEntity target, long now) {
        double chestUp = target.m_20206_() * OwlLances.CHEST;
        // From where it is pinned, in her frame as she is now (she may have moved round it meanwhile).
        lance.yaw = player.m_146908_();
        lance.pinned = PaleWatch.frame(player, lance.yaw, lance.pin);
        double[] slam = OwlLances.slamPoint(lance.pinned, chestUp, 0.0);
        Vec3 over = PaleWatch.world(player, lance.yaw, new double[] {slam[0], 1.5, slam[2]});
        Vec3 ground = ground(player.m_284548_(), player, over);
        lance.slam = OwlLances.slamPoint(lance.pinned, chestUp, ground.f_82480_ - player.m_20186_());
        set(player, lance, OwlLances.Phase.HOIST, now);
        UkakuFire.sound(player.m_284548_(), PaleWatch.chest(target), "entity.player.attack.sweep", 1.1F, KaguneServerController.vary(player, 0.45F, 0.05F));
    }

    /** Brought down on her far side: hurt again, a crater, stunned hard, a little bounce; the lance tears free. */
    private static void slam(ServerPlayer player, Crown crown, Lance lance, LivingEntity target, long now) {
        ServerLevel level = player.m_284548_();
        Vec3 chest = PaleWatch.world(player, lance.yaw, lance.slam);
        Vec3 feet = chest.m_82520_(0.0, -target.m_20206_() * OwlLances.CHEST, 0.0);
        hold(player, target, chest);
        target.f_19802_ = 0;
        float damage = PaleRules.fist(KaguneProfile.mastery(player)) * OwlRules.LANCE_SLAM_DAMAGE;
        if (target.m_6469_(player.m_269291_().m_269075_(player), damage)) {
            PaleStun.stun(target, now, com.hex.kagune.control.pale.StunRules.HEAVY_TICKS);
            PaleServer.landed(player, 1);
        }
        Vec3 away = feet.m_82546_(player.m_20182_());
        away = new Vec3(away.f_82479_, 0.0, away.f_82481_);
        away = away.m_82553_() < 1.0E-6 ? Vec3.f_82478_ : away.m_82541_();
        target.m_20256_(away.m_82490_(0.25).m_82520_(0.0, 0.32, 0.0));
        target.f_19864_ = true;
        BlockSmash.crater(player, feet.m_82520_(0.0, -0.4, 0.0), OwlRules.LANCE_CRATER);
        PaleBlitz.fx(player, S2CPaleFx.SHOCKWAVE, 0, feet, away, 1.6F, 0);
        UkakuFire.sound(level, feet, "entity.generic.explode", 0.7F, KaguneServerController.vary(player, 0.8F, 0.05F));
        UkakuFire.sound(level, feet, "entity.player.attack.knockback", 1.2F, KaguneServerController.vary(player, 0.55F, 0.05F));
        KaguneServerController.playAt(player, target, HexKaguneSounds.BONE_CRACK, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
        crown.slammed.put(target.m_19879_(), now + OwlRules.LANCE_SLAMMED_FREE);
        lance.point = feet;
        set(player, lance, OwlLances.Phase.RECOVER, now);
    }

    /** Held on the spear: its chest where the lance has it (its feet below that), nothing else moving it. */
    private static void hold(ServerPlayer player, LivingEntity body, Vec3 chest) {
        CentipedeHunt.place(player, body, chest.m_82520_(0.0, -body.m_20206_() * OwlLances.CHEST, 0.0));
    }

    /** The ground's top under {@code at} (from a block and a half above it down three), or {@code at} itself if there is none. */
    private static Vec3 ground(ServerLevel level, ServerPlayer player, Vec3 at) {
        net.minecraft.world.phys.BlockHitResult hit = level.m_45547_(new net.minecraft.world.level.ClipContext(at.m_82520_(0.0, 1.5, 0.0), at.m_82520_(0.0, -3.5, 0.0),
            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        return hit.m_6662_() == net.minecraft.world.phys.HitResult.Type.BLOCK ? hit.m_82450_() : new Vec3(at.f_82479_, player.m_20186_(), at.f_82481_);
    }

    /** The lance starts something new now (or at {@code at}) and every client is told. */
    private static void set(ServerPlayer player, Lance lance, OwlLances.Phase phase, long at) {
        lance.phase = phase;
        lance.startedAt = at;
        if (phase == OwlLances.Phase.IDLE || phase == OwlLances.Phase.OUT || phase == OwlLances.Phase.IN || phase == OwlLances.Phase.NONE
            || phase == OwlLances.Phase.RECOVER || phase == OwlLances.Phase.MISS) {
            lance.targetId = 0;
        }
        ControlNetwork.broadcast(player, message(player, lance));
    }

    private static S2COwlLance message(ServerPlayer player, Lance lance) {
        return new S2COwlLance(player.m_20148_(), lance.index, lance.phase.ordinal(), lance.startedAt, lance.targetId, lance.dive, lance.point.f_82479_,
            lance.point.f_82480_, lance.point.f_82481_);
    }

    /** The Owl is going: her lances draw back into her from now (what they hold is let go where it is). */
    static void retract(ServerPlayer player, long now) {
        Crown crown = CROWNS.get(player.m_20148_());
        if (crown != null && crown.end > now) {
            crown.end = Math.max(crown.out, now);
        }
    }

    /** Someone has started seeing her: where each of her lances is, so they are drawn at once. */
    static void tracked(ServerPlayer viewer, ServerPlayer subject) {
        Crown crown = CROWNS.get(subject.m_20148_());
        if (crown != null) {
            for (Lance lance : crown.lances) {
                ControlNetwork.sendTo(viewer, message(subject, lance));
            }
        }
    }

    /** Her lances go at once (her form or the Owl put away, death, another world, gone): what they hold is let go where it is. */
    static void forget(ServerPlayer player) {
        Crown crown = CROWNS.remove(player.m_20148_());
        if (crown == null) {
            return;
        }
        long now = player.m_9236_().m_46467_();
        for (Lance lance : crown.lances) {
            set(player, lance, OwlLances.Phase.NONE, now);
        }
    }
}
