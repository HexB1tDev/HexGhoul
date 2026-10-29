package com.hex.kagune.control.server;

import com.hex.kagune.control.ukaku.UkakuRules;
import com.hex.kagune.control.ukaku.WingForm;
import com.hex.kagune.control.ukaku.WingRig;
import com.hex.kagune.control.network.S2CUkakuFx;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.gameplay.TendrilMode;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Wing Vortex (Z, the original's Rapid Strike key: {@code KaguneServerController.startRapidStrike} is a bytecode
 * trampoline to here). Ukaku coils and one of his wings (a random one; one wing alone uses itself) twirls up, then shoots
 * forward as a spinning vortex of RC fire, still growing out of his shoulder blade: its point bores ahead, and everything
 * the whole twisting length of it touches is caught once: hit hard, set on fire and thrown along it. When the point reaches
 * the end of its range (or a wall) the vortex ends: the wing detaches from his back, unwinds and fades where it is, and a new
 * wing grows back out of the shoulder blade in its place ({@link UkakuRules#vortexWing}). Simulated here, not as an entity;
 * every client is told when it shoots out, what it hits and where it ends ({@link S2CUkakuFx}), and draws it from his
 * shoulder blade to its point ({@code ukaku/WingVortex}).
 *
 * <p>The final kakuja has no wings but throws the same fire cyclone from its fire side: the three fire tendrils whip forward
 * and it leaves the left of his back (Fire Dance still makes it bigger). The first kakuja's Z is Rushing Death instead.
 *
 * <p>5.0.9: it is a fire move only. Crystal wings (the V mode) can't throw it; thaw them first.
 */
public final class UkakuVortex {
    /** A throw waiting for the end of the wind-up. */
    private record Throw(int targetId, long at) {
    }

    private static final class Vortex {
        final int id = nextId++;
        /** The wing it is: +1 right, -1 left (its root stays on that shoulder blade). */
        int side;
        /** Its point. */
        Vec3 position;
        Vec3 velocity;
        int life;
        float radius;
        float damage;
        int burnSeconds;
        float size;
        boolean landed;
        final Set<Integer> hit = new HashSet<>();
    }

    private static final Map<UUID, Throw> THROWS = new HashMap<>();
    private static final Map<UUID, List<Vortex>> VORTICES = new HashMap<>();
    /** Vortex ids, apart from the fire's bolts and the crystal's spikes (the clients fly them all in one table). */
    private static int nextId = 0x20000000;
    /** How far the vortex looks for a target to aim at. */
    private static final double AIM_SLACK = 4.0;

    private UkakuVortex() {
    }

    /**
     * Replaces {@code KaguneServerController.startRapidStrike}: on the Ukaku path the key throws a Wing Vortex; on Rinkaku's it is
     * Rapid Strike, from its mastery (6.0.4, {@code RinkakuUnlocks}).
     */
    public static void startRapidStrike(ServerPlayer player, Entity hint) {
        if (PaleServer.isPale(player) || GraftServer.isGraft(player)) {
            return; // 6.1.0: on Pale's path Z is Big Punch (C2SPaleAction); 7.0.0: Graft's Z is his own (C2SGraftAction)
        }
        if (!UkakuServer.isUkaku(player)) {
            if (!RinkakuAutonomy.refuse(player) && UkakuGates.unlocked(player, "Rapid Strike", com.hex.kagune.control.RinkakuUnlocks.RAPID_STRIKE)) {
                KaguneServerController.startRapidStrikeLegacy(player, hint);
            }
            return;
        }
        start(player, hint);
    }

    /** Starts a throw if a wing can go now. Returns true if it started. */
    static boolean start(ServerPlayer player, Entity hint) {
        long now = player.m_9236_().m_46467_();
        TendrilMode mode = KaguneRuntime.get(player.m_20148_()).mode;
        if (!KaguneProfile.isActive(player) || !player.m_6084_() || mode == TendrilMode.MANIFEST || mode == TendrilMode.DISMISS) {
            return false;
        }
        int mastery = KaguneProfile.mastery(player);
        WingForm form = UkakuServer.form(player);
        if (form.kakuja() && !form.finalKakuja()) {
            // In the first kakuja, Z is Rushing Death.
            return UkakuRush.start(player);
        }
        if (!canThrow(form)) {
            // Canon: the One-Winged Kagune is one crystallized blade; he cuts with it and never throws it. The kakuja has no wings.
            player.m_5661_(Component.m_237115_(form == WingForm.ONE_WING ? "message.hexkagune.ukaku.one_wing_blade"
                : "message.hexkagune.ukaku.kakuja_no_wings"), true);
            return false;
        }
        if (crystallized(player, form)) {
            // 5.0.9: the vortex is RC fire; crystal wings have none to throw (thaw them with V first).
            player.m_5661_(Component.m_237115_("message.hexkagune.ukaku.vortex_crystal"), true);
            return false;
        }
        if (mastery < UkakuRules.VORTEX_MASTERY) {
            UkakuServer.locked(player, "The Wing Vortex", UkakuRules.VORTEX_MASTERY);
            return false;
        }
        UkakuServer.UkakuState state = UkakuServer.state(player);
        if (now < state.nextVortexAt || THROWS.containsKey(player.m_20148_()) || state.gatling) {
            return false; // the last wing is still growing back, or both are busy firing the Gatling
        }
        if (!KaguneServerController.useEnergy(player, UkakuRules.vortexCost(mastery), true)) {
            return false;
        }
        double range = UkakuRules.VORTEX_RANGE + AIM_SLACK;
        LivingEntity target = KaguneServerController.validHintedLiving(player, hint, range, false);
        if (target == null) {
            target = KaguneServerController.traceLiving(player, range, false);
        }
        // The final kakuja's cyclone always leaves its fire side (the left).
        int side = form.finalKakuja() ? -1 : UkakuRules.vortexSide(form.rightWing(), UkakuServer.wingGone(player, 1, now),
            UkakuServer.wingGone(player, -1, now), player.m_217043_().m_188501_());
        long launch = now + UkakuRules.VORTEX_WINDUP;
        state.vortexSide = side;
        state.vortexAt = launch;
        state.vortexRegrowAt = launch + UkakuRules.vortexFlight(mastery) + UkakuRules.VORTEX_FADE_TICKS;
        state.nextVortexAt = now + (Cooldowns.off(player) ? UkakuRules.vortexBusy(mastery) : UkakuRules.vortexCooldown(mastery));
        THROWS.put(player.m_20148_(), new Throw(target == null ? 0 : target.m_19879_(), launch));
        UkakuFire.sound(player.m_284548_(), shoulder(player, side), "item.trident.riptide_1", 0.6F, KaguneServerController.vary(player, 1.5F, 0.05F));
        UkakuServer.broadcast(player);
        return true;
    }

    /** Throws the wing at the end of the wind-up and flies the vortices. Called every server tick for Ukaku players. */
    static void tick(ServerPlayer player, UkakuServer.UkakuState state, long now) {
        UUID id = player.m_20148_();
        Throw pending = THROWS.get(id);
        if (pending != null && now >= pending.at()) {
            THROWS.remove(id);
            WingForm form = UkakuServer.form(player);
            if (KaguneProfile.isActive(player) && player.m_6084_() && canThrow(form) && !crystallized(player, form)) {
                launch(player, state, pending);
            } else {
                // The wings went in (or crystallized) before the throw: nothing is thrown, the wing stays.
                state.vortexAt = Long.MIN_VALUE;
                state.vortexRegrowAt = Long.MIN_VALUE;
                state.dirty = true;
            }
        }
        List<Vortex> vortices = VORTICES.get(id);
        if (vortices != null) {
            for (Iterator<Vortex> iterator = vortices.iterator(); iterator.hasNext(); ) {
                if (!step(player, state, iterator.next(), now)) {
                    iterator.remove();
                }
            }
            if (vortices.isEmpty()) {
                VORTICES.remove(id);
            }
        }
    }

    /** True for a form that throws a Wing Vortex: the wings, and the final kakuja's fire tendrils. */
    static boolean canThrow(WingForm form) {
        return form.wings() || form.finalKakuja();
    }

    /** True while his wings are crystal (the V mode): the vortex is a fire move, the fire wings' alone (5.0.9). */
    static boolean crystallized(ServerPlayer player, WingForm form) {
        return !form.kakuja() && UkakuServer.crystalMode(player);
    }

    /** True between Z and the wing leaving (the wind-up). */
    static boolean throwing(ServerPlayer player) {
        return THROWS.containsKey(player.m_20148_());
    }

    static void forget(ServerPlayer player) {
        THROWS.remove(player.m_20148_());
        VORTICES.remove(player.m_20148_());
    }

    /** The wing tears off the shoulder blade and flies at the target (or where he looks). */
    private static void launch(ServerPlayer player, UkakuServer.UkakuState state, Throw pending) {
        ServerLevel level = player.m_284548_();
        int mastery = KaguneProfile.mastery(player);
        double range = UkakuRules.VORTEX_RANGE;
        Vec3 eye = player.m_146892_();
        Vec3 aim = null;
        if (pending.targetId() > 0 && level.m_6815_(pending.targetId()) instanceof LivingEntity living && living.m_6084_()
            && living.m_20280_(player) <= (range + AIM_SLACK) * (range + AIM_SLACK)) {
            aim = KaguneServerController.center(living);
        }
        if (aim == null) {
            Vec3 end = eye.m_82549_(player.m_20154_().m_82490_(range));
            BlockHitResult block = level.m_45547_(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            aim = block.m_6662_() == HitResult.Type.BLOCK ? block.m_82450_() : end;
        }
        Vec3 from = clearOfBlocks(player, eye, shoulder(player, state.vortexSide));
        Vec3 heading = aim.m_82546_(from);
        heading = heading.m_82556_() < 1.0E-6 ? player.m_20154_() : heading.m_82541_();
        Vortex vortex = new Vortex();
        vortex.side = state.vortexSide;
        vortex.position = from;
        vortex.velocity = heading.m_82490_(UkakuRules.vortexSpeed(mastery));
        vortex.life = UkakuRules.vortexFlight(mastery);
        vortex.radius = UkakuRules.vortexRadius(mastery);
        vortex.damage = UkakuRules.vortexDamage(mastery);
        vortex.burnSeconds = UkakuRules.vortexBurnSeconds(mastery);
        // The thrown wing is as big as the wing was: small wings make a small vortex.
        vortex.size = (float)Math.max(0.6, Math.min(1.3, UkakuRules.wingLength(mastery, UkakuServer.form(player)) / 3.2));
        if (UkakuServer.hasMutation(player, com.hex.kagune.control.ukaku.UkakuMutation.FIRE_DANCE)) {
            // Fire Dance: a far bigger fire cyclone that hits much harder.
            vortex.radius *= UkakuRules.FIRE_DANCE_RADIUS;
            vortex.damage *= UkakuRules.FIRE_DANCE_DAMAGE;
            vortex.size *= UkakuRules.FIRE_DANCE_SIZE;
        }
        VORTICES.computeIfAbsent(player.m_20148_(), ignored -> new ArrayList<>()).add(vortex);
        UkakuFire.fx(player, S2CUkakuFx.VORTEX, vortex.id, from, vortex.velocity, vortex.size, vortex.life);
        UkakuFire.sound(level, from, "item.trident.riptide_3", 1.0F, KaguneServerController.vary(player, 1.1F, 0.05F));
        UkakuFire.sound(level, from, "entity.blaze.shoot", 0.9F, KaguneServerController.vary(player, 0.6F, 0.05F));
    }

    /**
     * One tick: the point bores on, and everything along the whole vortex, from his shoulder blade to its point, is caught.
     * Returns false once it has ended (its range, a wall, or he can't hold it: dead, wings in, pulled far from it).
     */
    private static boolean step(ServerPlayer player, UkakuServer.UkakuState state, Vortex vortex, long now) {
        ServerLevel level = player.m_284548_();
        Vec3 from = vortex.position;
        Vec3 to = from.m_82549_(vortex.velocity);
        BlockHitResult block = level.m_45547_(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        boolean wall = block.m_6662_() == HitResult.Type.BLOCK;
        Vec3 end = wall ? block.m_82450_() : to;
        Vec3 root = root(player, vortex.side);
        boolean held = player.m_6084_() && KaguneProfile.isActive(player) && canThrow(UkakuServer.form(player))
            && root.m_82557_(end) <= (UkakuRules.VORTEX_RANGE + 8.0) * (UkakuRules.VORTEX_RANGE + 8.0);
        if (held) {
            catchAlong(player, vortex, root, end);
        }
        if (!held || wall || --vortex.life <= 0) {
            vortex.position = end;
            finish(player, state, vortex, now);
            return false;
        }
        vortex.position = to;
        return true;
    }

    /** Everything within the vortex's radius of its line from {@code root} to {@code point}, once each. */
    private static void catchAlong(ServerPlayer player, Vortex vortex, Vec3 root, Vec3 point) {
        ServerLevel level = player.m_284548_();
        double length = root.m_82554_(point);
        Vec3 middle = root.m_82549_(point).m_82490_(0.5);
        Vec3 shift = middle.m_82546_(player.m_20191_().m_82399_());
        AABB search = player.m_20191_().m_82386_(shift.f_82479_, shift.f_82480_, shift.f_82481_).m_82400_(length * 0.5 + vortex.radius + 1.0);
        List<LivingEntity> candidates = level.m_6443_(LivingEntity.class, search,
            entity -> entity != player && entity.m_6084_() && !vortex.hit.contains(entity.m_19879_())
                && KaguneServerController.validSwipeTarget(player, entity));
        if (candidates.isEmpty()) {
            return;
        }
        Vec3 heading = length < 1.0E-6 ? vortex.velocity.m_82541_() : point.m_82546_(root).m_82490_(1.0 / length);
        int samples = Math.max(1, (int)Math.ceil(length / 0.4));
        for (int sample = 0; sample <= samples; sample++) {
            double along = length * sample / samples;
            // Narrow where it leaves his back, full width along it.
            double reach = vortex.radius * (along < 1.2 ? 0.3 + 0.7 * along / 1.2 : 1.0);
            Vec3 at = root.m_82549_(heading.m_82490_(along));
            for (LivingEntity target : candidates) {
                if (vortex.hit.contains(target.m_19879_()) || !target.m_20191_().m_82400_(reach).m_82390_(at)) {
                    continue;
                }
                vortex.hit.add(target.m_19879_());
                strike(player, vortex, target, heading);
            }
        }
    }

    /** Caught in the vortex: a heavy hit, set on fire, thrown hard along it ({@code heading}) and up. */
    private static void strike(ServerPlayer player, Vortex vortex, LivingEntity target, Vec3 heading) {
        target.f_19802_ = 0;
        Vec3 into = heading.m_82556_() > 1.0E-6 ? heading.m_82541_() : new Vec3(0.0, 0.0, 1.0);
        WoundHits.expect(target, com.hex.kagune.control.wound.WoundBook.Blow.SLASH, KaguneServerController.center(target).m_82546_(into.m_82490_(0.3)),
            into, 0.0);
        if (!target.m_6469_(player.m_269291_().m_269075_(player), vortex.damage)) {
            return;
        }
        target.m_20254_(vortex.burnSeconds);
        double k = UkakuRules.VORTEX_KNOCKBACK;
        target.m_20256_(new Vec3(heading.f_82479_ * k, Math.max(UkakuRules.VORTEX_LIFT, heading.f_82480_ * k * 0.5 + UkakuRules.VORTEX_LIFT),
            heading.f_82481_ * k));
        target.f_19864_ = true;
        Vec3 at = KaguneServerController.center(target);
        UkakuFire.fx(player, S2CUkakuFx.VORTEX_HIT, vortex.id, at, heading, vortex.size, 0);
        UkakuFire.sound(player.m_284548_(), at, "entity.generic.burn", 0.8F, KaguneServerController.vary(player, 1.0F, 0.1F));
        KaguneServerController.playAt(player, target, HexKaguneSounds.FLESH_IMPACT, 0.9F, KaguneServerController.vary(player, 0.9F, 0.06F));
        ControlServer.onKaguneHit(player, 1);
        if (!vortex.landed) {
            vortex.landed = true;
            KaguneProfile.addMastery(player, "Swipe", 1);
            KaguneNetwork.broadcast(player);
        }
    }

    /** The vortex ends: the wing detaches from his back, unwinds and fades, and the new one starts to grow once it has faded. */
    private static void finish(ServerPlayer player, UkakuServer.UkakuState state, Vortex vortex, long now) {
        UkakuFire.fx(player, S2CUkakuFx.VORTEX_END, vortex.id, vortex.position, vortex.velocity, vortex.size, UkakuRules.VORTEX_FADE_TICKS);
        UkakuFire.sound(player.m_284548_(), vortex.position, "block.fire.extinguish", 0.6F, KaguneServerController.vary(player, 0.8F, 0.1F));
        long regrow = now + UkakuRules.VORTEX_FADE_TICKS;
        if (regrow < state.vortexRegrowAt) {
            state.vortexRegrowAt = regrow;
            state.dirty = true;
        }
    }

    /** Where the vortex grows from: the shoulder blade of wing {@code side} (world), as the wing renderer places it. */
    private static Vec3 root(ServerPlayer player, int side) {
        double yaw = Math.toRadians(player.f_20883_);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double x = WingRig.ROOT_X * side;
        double z = WingRig.ROOT_Z;
        double y = WingRig.ROOT_Y - (player.m_6047_() ? WingRig.CROUCH_DROP : 0.0);
        return new Vec3(player.m_20185_() + rightX * x + forwardX * z, player.m_20186_() + y, player.m_20189_() + rightZ * x + forwardZ * z);
    }

    /** Where the vortex's point starts: off wing {@code side}'s shoulder, a little forward and out (world). */
    private static Vec3 shoulder(ServerPlayer player, int side) {
        double yaw = Math.toRadians(player.f_20883_);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double x = (WingRig.ROOT_X + 0.35) * side;
        double z = WingRig.ROOT_Z + 0.75;
        double y = WingRig.ROOT_Y + 0.15 - (player.m_6047_() ? WingRig.CROUCH_DROP : 0.0);
        return new Vec3(player.m_20185_() + rightX * x + forwardX * z, player.m_20186_() + y, player.m_20189_() + rightZ * x + forwardZ * z);
    }

    /** A launch point inside a wall would bury what leaves it there: pull it back toward the eye instead. */
    static Vec3 clearOfBlocks(ServerPlayer player, Vec3 eye, Vec3 launch) {
        BlockHitResult hit = player.m_9236_().m_45547_(new ClipContext(eye, launch, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.m_6662_() != HitResult.Type.BLOCK) {
            return launch;
        }
        Vec3 stop = hit.m_82450_();
        return new Vec3(eye.f_82479_ + (stop.f_82479_ - eye.f_82479_) * 0.8, eye.f_82480_ + (stop.f_82480_ - eye.f_82480_) * 0.8,
            eye.f_82481_ + (stop.f_82481_ - eye.f_82481_) * 0.8);
    }
}
