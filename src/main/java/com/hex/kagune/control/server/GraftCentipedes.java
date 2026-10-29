package com.hex.kagune.control.server;

import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.graft.GraftRig;
import com.hex.kagune.control.graft.GraftRules;
import com.hex.kagune.control.graft.GraftStage;
import com.hex.kagune.control.network.S2CPaleFx;
import com.hex.kagune.control.wound.WoundBook;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

/**
 * 7.0.2, the user's rework: the centipedes' four (two centipedes and the mouth) on the server, beyond the beats {@link GraftCombat}
 * runs, and the jobs: moves that run alongside whatever else he does ({@link GraftMove#job}), each on its own clock.
 * Hungry Maw: the mouth goes after its prey along a route round what is in the way (the move's point is the bend), snapping at what
 * its head meets; caught, the prey is clamped in its jaws and dragged at his side (a job: he fights on with the centipedes), chewed
 * now and then, until he flings it (the key again) or lets it drop. Centipede Locomotion (a job): the centipedes plant ahead in turn
 * and haul him where he looks, stabbing what they plant on, and it ends in the mouth's lunging bite. The Ambush is its beats alone.
 * Living Carapace: a blow at him in the coils springs them out at the attacker (stunned, thrown), and the mouth can bite after.
 */
final class GraftCentipedes {
    private GraftCentipedes() {
    }

    // ---------------------------------------------------------------- jobs

    /** {@code move} (a job) begins now, holding {@code held} (or nothing); any job before it ends. Every client told. */
    static void beginJob(ServerPlayer player, GraftServer.GraftState state, GraftMove move, LivingEntity held, long now) {
        endJob(player, state, false, false);
        state.job = move;
        state.jobAt = now;
        state.jobHeld = held == null ? 0 : held.m_19879_();
        GraftServer.broadcast(player, state);
    }

    /** The job ends: what it held is let go ({@code thrown}: keep its flight), every client told ({@code tell}). */
    static void endJob(ServerPlayer player, GraftServer.GraftState state, boolean thrown, boolean tell) {
        if (state.job == GraftMove.NONE) {
            return;
        }
        if (state.jobHeld != 0 && !thrown && player.m_284548_().m_6815_(state.jobHeld) instanceof LivingEntity held) {
            held.m_20256_(Vec3.f_82478_);
            held.f_19864_ = true;
            held.f_19789_ = 0.0F;
        }
        state.job = GraftMove.NONE;
        state.jobAt = Long.MIN_VALUE;
        state.jobHeld = 0;
        if (tell) {
            GraftServer.broadcast(player, state);
        }
    }

    /** The job's beat on {@code limb} now, or null. */
    private static GraftMove.Beat jobBeat(GraftServer.GraftState state, int limb, long now, int limbs) {
        return state.job == GraftMove.NONE ? null : GraftRig.beat(state.job, limbs, limb, now - state.jobAt);
    }

    /** True if a move's {@code beat} must give way: its limb holds what the job caught (the mouth dragging its prey). */
    static boolean taken(GraftServer.GraftState state, GraftMove.Beat beat, long now, int limbs) {
        if (state.job == GraftMove.NONE || beat.limb() < 0) {
            return false;
        }
        GraftMove.Beat held = jobBeat(state, beat.limb(), now, limbs);
        return held != null && held.has(GraftMove.CARRY);
    }

    /** A move begins: an ability that needs the mouth makes it let go of what it drags (the strings' bites only give way). */
    static void yield(ServerPlayer player, GraftServer.GraftState state, GraftMove move) {
        if (state.job != GraftMove.MAW_DRAG || move.combo() || move == GraftMove.STEP || move.job()) {
            return;
        }
        for (GraftMove.Beat beat : move.beats(3)) {
            if ((beat.limb() == 0 || beat.limb() == GraftMove.ALL) && !beat.has(GraftMove.CARRY)) {
                endJob(player, state, false, false);
                return;
            }
        }
    }

    /** The job this tick: the crawl's strides, the mouth's drag; when its time is up, the crawl ends in its bite, the drag lets go. */
    static void job(ServerPlayer player, GraftServer.GraftState state, long now) {
        if (state.job == GraftMove.NONE) {
            return;
        }
        if (GraftServer.stage(player) != state.job.stage()) {
            endJob(player, state, false, true);
            return;
        }
        long age = now - state.jobAt;
        switch (state.job) {
            case LOCOMOTION -> {
                state.fallSafe = true;
                for (GraftMove.Beat beat : GraftMove.LOCOMOTION.beats(GraftCombat.limbs(player))) {
                    if (age == beat.land()) {
                        stride(player, state, beat, now);
                    }
                }
                if (age >= GraftMove.LOCOMOTION.ticks()) {
                    bite(player, state, now);
                }
            }
            case MAW_DRAG -> {
                drag(player, state, age, now);
                if (state.job == GraftMove.MAW_DRAG && age >= GraftMove.MAW_DRAG.ticks()) {
                    endJob(player, state, false, true);
                }
            }
            case RECOVERY, PERFORMANCE -> {
                GraftFinal.job(player, state, age, now);
                if (state.job == GraftMove.RECOVERY && age >= GraftMove.RECOVERY.ticks()) {
                    endJob(player, state, false, true);
                }
            }
            case KINGS_GAME -> {
                GraftMasses.game(player, state, age, now);
                if (state.job == GraftMove.KINGS_GAME && age >= GraftMove.KINGS_GAME.ticks()) {
                    endJob(player, state, false, true);
                }
            }
            case CATAPULT_FLIGHT -> {
                GraftThorns.flight(player, state, age, now);
                if (state.job == GraftMove.CATAPULT_FLIGHT && age >= GraftMove.CATAPULT_FLIGHT.ticks()) {
                    endJob(player, state, false, true);
                }
            }
            default -> endJob(player, state, false, true);
        }
    }

    // ---------------------------------------------------------------- the keys

    /** The key while a centipede move can be followed up: the drag flung, the crawl's bite now, the carapace's bite. True if so. */
    static boolean key(ServerPlayer player, GraftServer.GraftState state, GraftMove.Slot slot, long now) {
        if (GraftServer.stage(player) != GraftStage.CENTIPEDE) {
            return false;
        }
        if (slot == GraftMove.Slot.Z && state.job == GraftMove.MAW_DRAG) {
            fling(player, state, now);
            return true;
        }
        if (slot == GraftMove.Slot.J && state.job == GraftMove.LOCOMOTION && now - state.jobAt >= 4) {
            bite(player, state, now);
            return true;
        }
        if (slot == GraftMove.Slot.B && state.move == GraftMove.CARAPACE_SPRING && now - state.startedAt >= 2) {
            carapaceBite(player, state, now);
            return true;
        }
        return false;
    }

    /** M1 after the carapace sprung: the mouth's bite. */
    static boolean strike(ServerPlayer player, GraftServer.GraftState state, long now) {
        if (state.move != GraftMove.CARAPACE_SPRING || now - state.startedAt < 2) {
            return false;
        }
        carapaceBite(player, state, now);
        return true;
    }

    // ---------------------------------------------------------------- each tick of a move

    static void tick(ServerPlayer player, GraftServer.GraftState state, GraftMove move, long age, long now) {
        switch (move) {
            case HUNGRY_MAW -> chase(player, state, age, now);
            case CRAWL_BITE -> {
                if (age == GraftRules.CRAWL_LUNGE_AT) {
                    Vec3 look = UkakuFire.horizontal(player.m_20154_());
                    UkakuFire.push(player, look.m_82490_(GraftRules.CRAWL_LUNGE).m_82520_(0.0, 0.08, 0.0));
                }
            }
            default -> {
            }
        }
    }

    // ---------------------------------------------------------------- Hungry Maw

    private static LivingEntity alive(ServerPlayer player, int id) {
        return id != 0 && player.m_284548_().m_6815_(id) instanceof LivingEntity living && living.m_6084_() ? living : null;
    }

    /** Where the mouth rears from before it goes out (world): over his back, as the rig rears it. */
    private static Vec3 rear(ServerPlayer player) {
        double k = GraftServer.stage(player).length(KaguneProfile.mastery(player)) / 5.0;
        return GraftCombat.around(player, new double[] {0.0, 0.95 + 2.4 * k, -0.17 - 0.4 * k});
    }

    /** True if nothing solid lies between {@code a} and {@code b}. */
    private static boolean clear(ServerPlayer player, Vec3 a, Vec3 b) {
        return player.m_284548_().m_45547_(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).m_6662_()
            == HitResult.Type.MISS;
    }

    /**
     * The bend in the mouth's route from {@code from} to {@code to}: a low arch over the straight line if that is open, else up over
     * what is in the way, else round it to either side; the arch if nothing is open (it will try, and fail to reach).
     */
    static Vec3 route(ServerPlayer player, Vec3 from, Vec3 to) {
        Vec3 mid = from.m_82549_(to).m_82490_(0.5);
        double distance = from.m_82554_(to);
        Vec3 flat = UkakuFire.horizontal(to.m_82546_(from));
        Vec3 side = new Vec3(-flat.f_82481_, 0.0, flat.f_82479_);
        Vec3[] bends = {mid.m_82520_(0.0, 0.5 + 0.2 * distance, 0.0), mid.m_82520_(0.0, 2.5 + 0.2 * distance, 0.0), mid.m_82520_(0.0, 4.5, 0.0),
            mid.m_82549_(side.m_82490_(3.0)).m_82520_(0.0, 1.0, 0.0), mid.m_82549_(side.m_82490_(-3.0)).m_82520_(0.0, 1.0, 0.0)};
        for (Vec3 bend : bends) {
            if (clear(player, from, bend) && clear(player, bend, to)) {
                return bend;
            }
        }
        return bends[0];
    }

    /** The point {@code along} blocks down the route from {@code a} through {@code b} to {@code c}. */
    private static Vec3 along(Vec3 a, Vec3 b, Vec3 c, double along) {
        double first = a.m_82554_(b);
        if (along <= first) {
            return a.m_82549_(b.m_82546_(a).m_82490_(along / Math.max(1.0E-6, first)));
        }
        double second = b.m_82554_(c);
        return b.m_82549_(c.m_82546_(b).m_82490_(Math.min(1.0, (along - first) / Math.max(1.0E-6, second))));
    }

    /**
     * The chase: the route re-found every few ticks (every client told when it bends anew), the mouth out along it as far as it has
     * gone, snapping at what its head meets; at its prey (and the last stretch open), the jaws clamp: the drag begins.
     */
    private static void chase(ServerPlayer player, GraftServer.GraftState state, long age, long now) {
        LivingEntity prey = alive(player, state.targets[0]);
        if (prey == null) {
            return; // nothing to go after: it reaches out where he aimed and comes back
        }
        if (prey.m_20280_(player) > GraftRules.MAW_RANGE * GraftRules.MAW_RANGE) {
            GraftCombat.end(player, state); // out of its reach
            return;
        }
        Vec3 from = rear(player);
        Vec3 to = KaguneServerController.center(prey);
        if (age % 4 == 0) {
            Vec3 bend = route(player, from, to);
            if (bend.m_82554_(state.point) > 0.5) {
                state.point = bend;
                GraftServer.broadcast(player, state);
            }
        }
        double length = GraftServer.stage(player).length(KaguneProfile.mastery(player));
        double back = 2.6 * length / 5.0; // root to the rear, along the limb
        double out = GraftRules.mawReach(age, length) - back;
        double route = from.m_82554_(state.point) + state.point.m_82554_(to);
        Vec3 head = along(from, state.point, to, Math.max(0.0, out));
        Set<Integer> snapped = GraftCombat.hitSet(state, -1);
        if (age % GraftRules.MAW_SNAP == GraftRules.MAW_SNAP - 1) {
            LivingEntity met = GraftCombat.nearest(player, head, 1.3, snapped);
            KaguneServerController.playAt(player, head, HexKaguneSounds.BONE_CRUSH, 0.6F, KaguneServerController.vary(player, 1.3F, 0.1F));
            if (met != null && met != prey) {
                snapped.add(met.m_19879_());
                GraftCombat.strikeBody(player, state, met, 0.3F, Vec3.f_82478_, GraftMove.BLEED | GraftMove.STUN, now, WoundBook.Blow.CRUSH);
                PaleBlitz.fx(player, S2CPaleFx.CRUSHED, met.m_19879_(), KaguneServerController.center(met), Vec3.f_82478_, 0.8F, 0);
            }
        }
        if (out >= route - 0.8 && clear(player, state.point, to)) {
            GraftCombat.strikeBody(player, state, prey, 0.6F, Vec3.f_82478_, GraftMove.BLEED | GraftMove.STUN, now, WoundBook.Blow.CRUSH);
            PaleBlitz.fx(player, S2CPaleFx.CRUSHED, prey.m_19879_(), to, Vec3.f_82478_, prey.m_20206_(), 0);
            KaguneServerController.playAt(player, prey, HexKaguneSounds.KAGUNE_GRAB, 1.0F, KaguneServerController.vary(player, 0.8F, 0.05F));
            KaguneServerController.playAt(player, prey, HexKaguneSounds.BONE_CRUSH, 1.0F, KaguneServerController.vary(player, 0.9F, 0.05F));
            GraftCombat.end(player, state);
            beginJob(player, state, GraftMove.MAW_DRAG, prey, now);
        }
    }

    /** The drag: clamped in the mouth, stunned, carried low at his side wherever he goes, chewed now and then. */
    private static void drag(ServerPlayer player, GraftServer.GraftState state, long age, long now) {
        LivingEntity held = alive(player, state.jobHeld);
        if (held == null || held.m_20280_(player) > 12.0 * 12.0) {
            endJob(player, state, false, true);
            return;
        }
        if (!PaleStun.stun(held, now, 6)) {
            endJob(player, state, false, true); // 7.0.2: a player held too long breaks free
            return;
        }
        Vec3 where = GraftCombat.around(player, GraftRules.DRAG_POINT);
        GraftCombat.carry(held, where.m_82520_(0.0, -held.m_20206_() * 0.5, 0.0));
        if (age > 0 && age % GraftRules.CHEW == 0) {
            GraftCombat.strikeBody(player, state, held, GraftMove.MAW_DRAG.beats(3).get(0).power(), Vec3.f_82478_, GraftMove.BLEED | GraftMove.STUN,
                now, WoundBook.Blow.CRUSH);
            PaleBlitz.fx(player, S2CPaleFx.CRUSHED, held.m_19879_(), KaguneServerController.center(held), Vec3.f_82478_, 0.8F, 0);
            KaguneServerController.playAt(player, held, HexKaguneSounds.BONE_CRUSH, 0.8F, KaguneServerController.vary(player, 1.0F, 0.1F));
            if (!held.m_6084_()) {
                SharedBar.feed(player, 4.0F); // it ate
            }
        }
    }

    /** The key again while it drags: the mouth swings it round and flings it along his look. */
    private static void fling(ServerPlayer player, GraftServer.GraftState state, long now) {
        LivingEntity held = alive(player, state.jobHeld);
        if (held == null) {
            endJob(player, state, false, true);
            return;
        }
        Vec3 look = UkakuFire.horizontal(player.m_20154_());
        GraftCombat.strikeBody(player, state, held, 0.6F, Vec3.f_82478_, GraftMove.STUN | GraftMove.HEAVY, now, WoundBook.Blow.SLAM);
        endJob(player, state, true, true);
        BlockSmash.launch(player, held, look.m_82490_(GraftRules.FLING_SPEED).m_82520_(0.0, GraftRules.FLING_LIFT, 0.0), GraftCombat.THROWN);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 1.0F, KaguneServerController.vary(player, 0.8F, 0.05F));
    }

    // ---------------------------------------------------------------- Centipede Locomotion

    /** A stride: the centipede stabs into the ground ahead at its side (what stands there is stabbed) and hauls him on. */
    private static void stride(ServerPlayer player, GraftServer.GraftState state, GraftMove.Beat beat, long now) {
        Vec3 look = UkakuFire.horizontal(player.m_20154_());
        Vec3 right = new Vec3(-look.f_82481_, 0.0, look.f_82479_);
        int n = GraftCombat.limbs(player);
        int s = GraftRig.side(GraftStage.CENTIPEDE, n, beat.limb());
        double k = GraftStage.CENTIPEDE.length(KaguneProfile.mastery(player)) / 5.0;
        Vec3 plant = player.m_20182_().m_82549_(look.m_82490_(beat.reach())).m_82549_(right.m_82490_(s * GraftRules.CRAWL_SPLAY * k));
        BlockHitResult ground = player.m_284548_().m_45547_(new ClipContext(plant.m_82520_(0.0, 1.0, 0.0), plant.m_82520_(0.0, -2.0, 0.0),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (ground.m_6662_() == HitResult.Type.BLOCK) {
            GraftCombat.ground(player, ground.m_82450_(), 1.0F);
        }
        Set<Integer> hit = GraftCombat.hitSet(state, -10 - beat.start());
        LivingEntity under = GraftCombat.nearest(player, plant.m_82520_(0.0, 0.6, 0.0), 1.0, hit);
        if (under != null) {
            hit.add(under.m_19879_());
            GraftCombat.strikeBody(player, state, under, 0.4F, look.m_82490_(0.3), GraftMove.STUN | GraftMove.SPIKED, now, WoundBook.Blow.STAB);
            PaleBlitz.fx(player, S2CPaleFx.IMPALED, under.m_19879_(), KaguneServerController.center(under), new Vec3(0.0, -1.0, 0.0), under.m_20206_(), 0);
        }
        double lift = player.m_20096_() ? 0.12 : Math.max(-0.2, player.m_20184_().f_82480_);
        UkakuFire.push(player, look.m_82490_(GraftRules.CRAWL_PUSH).m_82520_(0.0, lift, 0.0));
        KaguneServerController.playSelf(player, HexKaguneSounds.CENTIPEDE_CLICK, 0.8F, KaguneServerController.vary(player, 1.0F, 0.1F));
    }

    /** The crawl's end: the mouth lunges and bites what is ahead (whatever else he was doing gives way to it). */
    private static void bite(ServerPlayer player, GraftServer.GraftState state, long now) {
        endJob(player, state, false, false);
        GraftCombat.release(player, state, false);
        LivingEntity target = GraftCombat.aim(player, null, 7.5);
        java.util.Arrays.fill(state.targets, 0);
        Vec3 point = target != null ? KaguneServerController.center(target) : GraftCombat.lookPoint(player, 7.0);
        if (target != null) {
            state.targets[0] = target.m_19879_();
        }
        GraftCombat.begin(player, state, GraftMove.CRAWL_BITE, now, player.m_146908_(), point);
        KaguneServerController.playSelf(player, HexKaguneSounds.CENTIPEDE_CLICK, 1.0F, KaguneServerController.vary(player, 0.7F, 0.05F));
    }

    // ---------------------------------------------------------------- Living Carapace

    /**
     * A blow at him: in the carapace, from an attacker near enough, it is caught on the coils and they spring out at it; in the
     * spring's first ticks, blows miss. True if taken care of here (a far blow is left to the guard, which still catches it).
     */
    static boolean attacked(ServerPlayer player, GraftServer.GraftState state, LivingAttackEvent event, long now) {
        GraftMove move = state.move;
        if (move != GraftMove.CARAPACE && move != GraftMove.CARAPACE_SPRING || event.getSource() == null) {
            return false;
        }
        if (!(event.getSource().m_7639_() instanceof LivingEntity attacker) || attacker == player || !attacker.m_6084_()) {
            return false;
        }
        long age = now - state.startedAt;
        if (move == GraftMove.CARAPACE_SPRING) {
            if (age > 3) {
                return false;
            }
            event.setCanceled(true);
            return true;
        }
        if (!GraftServer.guarding(state, now) || attacker.m_20280_(player) > GraftRules.CARAPACE_RANGE * GraftRules.CARAPACE_RANGE
            || !KaguneServerController.validSwipeTarget(player, attacker) || !KaguneProfile.consumeEnergy(player, 0.6F)) {
            return false;
        }
        event.setCanceled(true);
        Vec3 to = attacker.m_20182_().m_82546_(player.m_20182_());
        float yaw = (float)Math.toDegrees(Math.atan2(-to.f_82479_, to.f_82481_));
        GraftCombat.release(player, state, false);
        java.util.Arrays.fill(state.targets, 0);
        state.targets[0] = attacker.m_19879_();
        GraftCombat.begin(player, state, GraftMove.CARAPACE_SPRING, now, yaw, KaguneServerController.center(attacker));
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_GUARD, 1.0F, KaguneServerController.vary(player, 0.8F, 0.05F));
        KaguneServerController.playSelf(player, HexKaguneSounds.BONE_CRACK, 0.9F, KaguneServerController.vary(player, 1.1F, 0.05F));
        return true;
    }

    /** The mouth's bite after the spring, at what struck at him. */
    private static void carapaceBite(ServerPlayer player, GraftServer.GraftState state, long now) {
        Entity target = state.targets[0] == 0 ? null : player.m_284548_().m_6815_(state.targets[0]);
        Vec3 point = target instanceof LivingEntity living && living.m_6084_() ? KaguneServerController.center(living) : state.point;
        Vec3 to = point.m_82546_(player.m_20182_());
        float yaw = (float)Math.toDegrees(Math.atan2(-to.f_82479_, to.f_82481_));
        GraftCombat.begin(player, state, GraftMove.CARAPACE_BITE, now, yaw, point);
        KaguneServerController.playSelf(player, HexKaguneSounds.CENTIPEDE_CLICK, 0.9F, KaguneServerController.vary(player, 0.8F, 0.05F));
    }
}
