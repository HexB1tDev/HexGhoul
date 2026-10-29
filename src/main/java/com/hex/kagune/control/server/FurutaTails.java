package com.hex.kagune.control.server;

import com.hex.kagune.control.furuta.FurutaMove;
import com.hex.kagune.control.furuta.FurutaRules;
import com.hex.kagune.control.furuta.FurutaStage;
import com.hex.kagune.control.network.S2CEtoFx;
import com.hex.kagune.control.wound.WoundBook;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

/**
 * 7.0.2, the user's rework: the two tails' four (Rize's Kakuhou) on the server, beyond the beats {@link FurutaCombat} runs.
 * Mockingbird's counter: a blow at him in the stance misses (he slips aside), the attacker is hooked where it stands and run through
 * from behind, and his string goes on from its fourth blow. Owl's Execution's rush in and its circle round the prey (held where it
 * stands by the onslaught). Scorpion Vault: thrown up off the tails stabbed into the ground behind him, then, the key again in the
 * air, the dive where he looks, its impact stunning what it lands on. Human Puppet: caught and dragged in, then his choice: the key
 * again hurls it, M1 has the other tail rip up through it, right click hauls him in to it for his claws and a tail.
 */
final class FurutaTails {
    private FurutaTails() {
    }

    // ---------------------------------------------------------------- starting

    /** True if {@code move} can start now (the vault needs ground behind him to stab into; the notice says so if not). */
    static boolean ready(ServerPlayer player, FurutaServer.FurutaState state, FurutaMove move) {
        if (move != FurutaMove.SCORPION_VAULT || player.m_20096_() || plantable(player)) {
            return true;
        }
        FurutaServer.notice(player, state, Component.m_237115_("message.hexkagune.furuta.no_vault"));
        return false;
    }

    /** How far a move looks for what it goes after (the execution rushes in from further than its cuts reach). */
    static double seek(FurutaMove move, double reach) {
        return move == FurutaMove.OWLS_EXECUTION ? FurutaRules.EXECUTION_SEEK : move == FurutaMove.HUNGRY_MAW ? FurutaRules.MAW_RANGE : reach;
    }

    /** The key while a two tails' move can be followed up: the dive out of the vault, the puppet thrown. True if it was that. */
    static boolean key(ServerPlayer player, FurutaServer.FurutaState state, FurutaMove.Slot slot, long now) {
        if (FurutaServer.stage(player) != FurutaStage.TRANSPLANT) {
            return false;
        }
        if (slot == FurutaMove.Slot.V && now < state.vaultUntil && !player.m_20096_() && state.move != FurutaMove.VAULT_DIVE) {
            dive(player, state, now);
            return true;
        }
        if (slot == FurutaMove.Slot.B && puppet(state)) {
            follow(player, state, FurutaMove.PUPPET_THROW, now, FurutaCombat.lookPoint(player, 8.0));
            return true;
        }
        return false;
    }

    /** M1 with a puppet held: the other tail rips up through it. */
    static boolean strike(ServerPlayer player, FurutaServer.FurutaState state, long now) {
        if (!puppet(state)) {
            return false;
        }
        follow(player, state, FurutaMove.PUPPET_STRIKE, now, null);
        return true;
    }

    /** Right click with a puppet held: he hauls himself in to it. */
    static boolean step(ServerPlayer player, FurutaServer.FurutaState state, long now) {
        if (!puppet(state)) {
            return false;
        }
        follow(player, state, FurutaMove.PUPPET_PULL, now, null);
        return true;
    }

    /** True while Human Puppet holds what it caught (its choice open). */
    private static boolean puppet(FurutaServer.FurutaState state) {
        return state.move == FurutaMove.HUMAN_PUPPET && state.heldId != 0;
    }

    /** {@code next} follows on from the puppet: what it holds stays held, every limb's point on it ({@code point}: the throw's aim). */
    private static void follow(ServerPlayer player, FurutaServer.FurutaState state, FurutaMove next, long now, Vec3 point) {
        Entity held = player.m_284548_().m_6815_(state.heldId);
        java.util.Arrays.fill(state.targets, 0);
        state.targets[0] = state.heldId;
        Vec3 at = point != null ? point : held != null ? KaguneServerController.center(held) : state.point;
        FurutaCombat.begin(player, state, next, now, player.m_146908_(), at);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.85F, KaguneServerController.vary(player, 0.9F, 0.08F));
    }

    // ---------------------------------------------------------------- Mockingbird

    /**
     * A blow at him: in the stance, the attacker (near enough to reach) is countered and the blow misses; in the counter's first
     * ticks he has already slipped aside, and blows miss. True if the blow was taken care of here.
     */
    static boolean attacked(ServerPlayer player, FurutaServer.FurutaState state, LivingAttackEvent event, long now) {
        FurutaMove move = state.move;
        if (move != FurutaMove.MOCKINGBIRD && move != FurutaMove.MOCKINGBIRD_COUNTER || event.getSource() == null) {
            return false;
        }
        long age = now - state.startedAt;
        if (!(event.getSource().m_7639_() instanceof LivingEntity attacker) || attacker == player || !attacker.m_6084_()) {
            return false;
        }
        if (move == FurutaMove.MOCKINGBIRD_COUNTER) {
            if (age > FurutaRules.COUNTER_DODGE_TICKS) {
                return false;
            }
            event.setCanceled(true); // already slipped aside
            return true;
        }
        if (age < 1 || age > move.ticks() || attacker.m_20280_(player) > FurutaRules.COUNTER_RANGE * FurutaRules.COUNTER_RANGE
            || !KaguneServerController.validSwipeTarget(player, attacker)) {
            return false;
        }
        event.setCanceled(true);
        counter(player, state, attacker, now);
        return true;
    }

    /** The counter: he slips aside of the blow, one tail hooks the attacker, the other comes round and runs it through from behind. */
    private static void counter(ServerPlayer player, FurutaServer.FurutaState state, LivingEntity attacker, long now) {
        Vec3 to = attacker.m_20182_().m_82546_(player.m_20182_());
        float yaw = (float)Math.toDegrees(Math.atan2(-to.f_82479_, to.f_82481_));
        FurutaCombat.release(player, state, false);
        java.util.Arrays.fill(state.targets, 0);
        state.targets[0] = attacker.m_19879_();
        // Aside, to his right of the blow, fast and low.
        double rad = Math.toRadians(yaw);
        Vec3 right = new Vec3(-Math.cos(rad), 0.0, -Math.sin(rad));
        AyatoFire.push(player, right.m_82490_(FurutaRules.COUNTER_DODGE).m_82520_(0.0, 0.12, 0.0));
        FurutaCombat.begin(player, state, FurutaMove.MOCKINGBIRD_COUNTER, now, yaw, KaguneServerController.center(attacker));
        // It flows on: the next M1 is the string's fourth blow.
        state.lastStrike = FurutaMove.LASH_3;
        state.strikeEndsAt = now + FurutaMove.MOCKINGBIRD_COUNTER.ticks();
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 1.0F, KaguneServerController.vary(player, 1.35F, 0.05F));
        AyatoFire.sound(player.m_284548_(), player.m_20182_(), "entity.player.attack.sweep", 0.7F, KaguneServerController.vary(player, 1.4F, 0.08F));
    }

    /** A stance nobody struck at: its cooldown is cut short (it cost him the time, not the whole wait). */
    static void expired(ServerPlayer player, FurutaServer.FurutaState state, FurutaMove move, long now) {
        if (move == FurutaMove.MOCKINGBIRD) {
            state.readyAt.put(move, Math.min(state.readyAt.getOrDefault(move, now), state.startedAt
                + Cooldowns.of(player, (long)FurutaRules.STANCE_WHIFF_COOLDOWN)));
        }
    }

    /**
     * A hooking beat: on its landing tick the limb closes round what struck at him (or what is at its point), then holds it where it
     * stands, stunned, until it lets go.
     */
    static void hook(ServerPlayer player, FurutaServer.FurutaState state, FurutaMove.Beat beat, int index, long age, long now) {
        if (age == beat.land()) {
            LivingEntity caught = caught(player, state, beat, index);
            if (caught == null) {
                KaguneServerController.playAt(player, FurutaCombat.strikePoint(player, state, beat), HexKaguneSounds.KAGUNE_WHIP, 0.7F,
                    KaguneServerController.vary(player, 1.1F, 0.05F));
                return;
            }
            FurutaCombat.hitSet(state, index).add(caught.m_19879_());
            state.heldId = caught.m_19879_();
            FurutaCombat.strikeBody(player, state, caught, beat.power(), Vec3.f_82478_, beat.flags(), now, WoundBook.Blow.CRUSH);
            EtoBlitz.fx(player, S2CEtoFx.CRUSHED, caught.m_19879_(), KaguneServerController.center(caught), Vec3.f_82478_, caught.m_20206_(), 0);
            KaguneServerController.playAt(player, caught, HexKaguneSounds.KAGUNE_GRAB, 1.0F, KaguneServerController.vary(player, 1.1F, 0.05F));
            FurutaServer.broadcast(player, state);
            return;
        }
        if (age <= beat.land() || age > beat.end() || state.heldId == 0) {
            return;
        }
        if (!(player.m_284548_().m_6815_(state.heldId) instanceof LivingEntity held) || !held.m_6084_()) {
            FurutaCombat.release(player, state, false);
            FurutaServer.broadcast(player, state);
            return;
        }
        if (!EtoStun.stun(held, now, 6)) {
            FurutaCombat.release(player, state, false); // 7.0.2: a player held too long breaks free
            FurutaServer.broadcast(player, state);
            return;
        }
        pin(held);
        if (age == beat.end()) {
            FurutaCombat.release(player, state, false);
            FurutaServer.broadcast(player, state);
        }
    }

    /** A flanking beat: round behind what it goes after and driven through it from its back (toward him). */
    static void flank(ServerPlayer player, FurutaServer.FurutaState state, FurutaMove.Beat beat, int index, long now) {
        LivingEntity caught = caught(player, state, beat, index);
        if (caught == null) {
            KaguneServerController.playAt(player, FurutaCombat.strikePoint(player, state, beat), HexKaguneSounds.KAGUNE_WHIP, 0.8F,
                KaguneServerController.vary(player, 0.9F, 0.05F));
            return;
        }
        FurutaCombat.hitSet(state, index).add(caught.m_19879_());
        Vec3 into = AyatoFire.horizontal(player.m_20182_().m_82546_(caught.m_20182_()));
        // Hooked, it stays where it is; loose, the blow from behind throws it at him.
        Vec3 push = state.heldId == caught.m_19879_() ? Vec3.f_82478_ : into.m_82490_(0.5);
        FurutaCombat.strikeBody(player, state, caught, beat.power(), push, beat.flags(), now, WoundBook.Blow.STAB);
        EtoBlitz.fx(player, S2CEtoFx.IMPALED, caught.m_19879_(), KaguneServerController.center(caught), into, caught.m_20206_(), 0);
        KaguneServerController.playAt(player, caught, HexKaguneSounds.FLESH_IMPACT, 1.0F, KaguneServerController.vary(player, 0.85F, 0.05F));
        KaguneServerController.playAt(player, caught, HexKaguneSounds.BONE_CRACK, 0.8F, KaguneServerController.vary(player, 1.1F, 0.05F));
    }

    /** What a hook or a flank closes on: what the move goes after if it is near the beat's point, else the nearest body there. */
    private static LivingEntity caught(ServerPlayer player, FurutaServer.FurutaState state, FurutaMove.Beat beat, int index) {
        Vec3 at = FurutaCombat.strikePoint(player, state, beat);
        Entity aimed = state.targets[0] == 0 ? null : player.m_284548_().m_6815_(state.targets[0]);
        if (aimed instanceof LivingEntity living && living.m_6084_() && KaguneServerController.center(living).m_82554_(at) <= 2.5) {
            return living;
        }
        return FurutaCombat.nearest(player, at, 1.8, FurutaCombat.hitSet(state, index));
    }

    // ---------------------------------------------------------------- each tick

    /** What a two tails' move does beyond its beats this tick. */
    static void tick(ServerPlayer player, FurutaServer.FurutaState state, FurutaMove move, long age, long now) {
        switch (move) {
            case OWLS_EXECUTION -> execution(player, state, age);
            case SCORPION_VAULT -> {
                if (age == FurutaRules.VAULT_LAUNCH) {
                    launch(player, state, now);
                }
            }
            case VAULT_DIVE -> diving(player, state, age, now);
            case PUPPET_THROW -> hurl(player, state, age, now);
            case PUPPET_STRIKE -> {
                // The rip has thrown it up: let it fly.
                if (age == 4 && state.heldId != 0) {
                    FurutaCombat.release(player, state, true);
                    FurutaServer.broadcast(player, state);
                }
            }
            case PUPPET_PULL -> pull(player, state, age);
            default -> {
            }
        }
    }

    // ---------------------------------------------------------------- Owl's Execution

    /**
     * The execution: rushed in to a stride off it, then carried round it (the cuts landing as he goes, every one from a new side),
     * facing it all the way; what he cuts is held where it stands.
     */
    private static void execution(ServerPlayer player, FurutaServer.FurutaState state, long age) {
        LivingEntity prey = state.targets[0] == 0 ? null
            : player.m_284548_().m_6815_(state.targets[0]) instanceof LivingEntity living && living.m_6084_() ? living : null;
        Vec3 me = player.m_20182_();
        if (prey == null) {
            if (age <= FurutaRules.EXECUTION_RUSH) {
                // Nothing to run at: he still dashes in where he looks, cutting the air.
                AyatoFire.push(player, AyatoFire.horizontal(player.m_20154_()).m_82490_(1.1).m_82520_(0.0, player.m_20184_().f_82480_, 0.0));
            }
            return;
        }
        Vec3 centre = prey.m_20182_();
        Vec3 flat = new Vec3(me.f_82479_ - centre.f_82479_, 0.0, me.f_82481_ - centre.f_82481_);
        double distance = flat.m_82553_();
        state.yaw = (float)Math.toDegrees(Math.atan2(-(centre.f_82479_ - me.f_82479_), centre.f_82481_ - me.f_82481_));
        if (age <= FurutaRules.EXECUTION_RUSH) {
            Vec3 way = distance < 1.0E-6 ? AyatoFire.horizontal(player.m_20154_()) : flat.m_82490_(-1.0 / distance);
            double speed = Math.min(FurutaRules.EXECUTION_SPEED, Math.max(0.0, distance - FurutaRules.EXECUTION_RADIUS) * 0.6);
            AyatoFire.push(player, way.m_82490_(speed).m_82520_(0.0, Math.max(0.0, player.m_20184_().f_82480_), 0.0));
            if (age == FurutaRules.EXECUTION_RUSH) {
                // Round the way he was already going (to his right if he stood still).
                Vec3 v = player.m_20184_();
                state.circleFrom = Math.atan2(flat.f_82479_, flat.f_82481_);
                state.circleWay = flat.f_82479_ * v.f_82481_ - flat.f_82481_ * v.f_82479_ < 0.0 ? -1 : 1;
            }
            return;
        }
        if (age > FurutaRules.EXECUTION_CIRCLE_END + 1) {
            pin(prey);
            return;
        }
        double a = state.circleFrom + state.circleWay * Math.toRadians(FurutaRules.executionTurn(age + 1));
        Vec3 want = centre.m_82520_(Math.sin(a) * FurutaRules.EXECUTION_RADIUS, 0.0, Math.cos(a) * FurutaRules.EXECUTION_RADIUS);
        Vec3 gap = new Vec3(want.f_82479_ - me.f_82479_, 0.0, want.f_82481_ - me.f_82481_);
        double length = gap.m_82553_();
        if (length > FurutaRules.EXECUTION_SPEED) {
            gap = gap.m_82490_(FurutaRules.EXECUTION_SPEED / length);
        }
        AyatoFire.push(player, gap.m_82520_(0.0, player.m_20184_().f_82480_, 0.0));
        state.fallSafe = !player.m_20096_() || state.fallSafe;
        pin(prey);
    }

    // ---------------------------------------------------------------- Scorpion Vault

    /** The two points behind him where the tails go into the ground (feet height): a stride back, one either side. */
    private static Vec3[] plants(ServerPlayer player) {
        Vec3 ahead = AyatoFire.horizontal(player.m_20154_());
        Vec3 right = new Vec3(-ahead.f_82481_, 0.0, ahead.f_82479_);
        Vec3 behind = player.m_20182_().m_82549_(ahead.m_82490_(-1.7));
        return new Vec3[] {behind.m_82549_(right.m_82490_(0.55)), behind.m_82549_(right.m_82490_(-0.55))};
    }

    /** True if there is ground under the points behind him to stab into (within a couple of blocks below). */
    private static boolean plantable(ServerPlayer player) {
        for (Vec3 at : plants(player)) {
            BlockHitResult ground = player.m_284548_().m_45547_(new ClipContext(at.m_82520_(0.0, 1.0, 0.0), at.m_82520_(0.0, -2.5, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (ground.m_6662_() == HitResult.Type.BLOCK) {
                return true;
            }
        }
        return false;
    }

    /** The vault: the tails go into the ground behind him (anything standing there is stabbed) and he is thrown up off them. */
    private static void launch(ServerPlayer player, FurutaServer.FurutaState state, long now) {
        Set<Integer> hit = FurutaCombat.hitSet(state, -1);
        for (Vec3 plant : plants(player)) {
            BlockHitResult ground = player.m_284548_().m_45547_(new ClipContext(plant.m_82520_(0.0, 1.0, 0.0), plant.m_82520_(0.0, -2.5, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 at = ground.m_6662_() == HitResult.Type.BLOCK ? ground.m_82450_() : plant;
            if (ground.m_6662_() == HitResult.Type.BLOCK) {
                BlockSmash.crater(player, at, 0.6);
                FurutaCombat.ground(player, at, 1.2F);
            }
            LivingEntity under = FurutaCombat.nearest(player, at.m_82520_(0.0, 0.6, 0.0), 1.0, hit);
            if (under != null) {
                hit.add(under.m_19879_());
                FurutaCombat.strikeBody(player, state, under, 0.5F, new Vec3(0.0, 0.2, 0.0), FurutaMove.STUN, now, WoundBook.Blow.STAB);
                EtoBlitz.fx(player, S2CEtoFx.IMPALED, under.m_19879_(), KaguneServerController.center(under), new Vec3(0.0, -1.0, 0.0), under.m_20206_(), 0);
            }
        }
        Vec3 ahead = AyatoFire.horizontal(player.m_20154_());
        AyatoFire.push(player, ahead.m_82490_(FurutaRules.VAULT_AHEAD).m_82520_(0.0, FurutaRules.VAULT_UP, 0.0));
        state.fallSafe = true;
        state.vaultUntil = now + FurutaRules.VAULT_WINDOW;
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_SLAM, 0.7F, KaguneServerController.vary(player, 1.3F, 0.05F));
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.9F, KaguneServerController.vary(player, 0.8F, 0.05F));
    }

    /** The dive out of the vault: down where he looks, both tails ahead (its RC paid here: it has no cooldown of its own). */
    private static void dive(ServerPlayer player, FurutaServer.FurutaState state, long now) {
        if (!KaguneServerController.useEnergy(player, FurutaMove.VAULT_DIVE.cost(), true)) {
            return;
        }
        FurutaCombat.release(player, state, false);
        java.util.Arrays.fill(state.targets, 0);
        state.vaultUntil = Long.MIN_VALUE;
        state.fallSafe = true;
        Vec3 dir = way(player);
        FurutaCombat.begin(player, state, FurutaMove.VAULT_DIVE, now, player.m_146908_(), player.m_20182_().m_82549_(dir.m_82490_(8.0)));
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 1.0F, KaguneServerController.vary(player, 0.7F, 0.05F));
    }

    /** Where the dive takes him this tick: where he looks, never flatter than {@link FurutaRules#DIVE_DOWN} down. */
    private static Vec3 way(ServerPlayer player) {
        Vec3 look = player.m_20154_();
        double[] d = FurutaRules.diveDirection(look.f_82479_, look.f_82480_, look.f_82481_);
        return new Vec3(d[0], d[1], d[2]);
    }

    /** Diving: driven down along it until the tails strike something on the way, or he comes down. */
    private static void diving(ServerPlayer player, FurutaServer.FurutaState state, long age, long now) {
        Vec3 dir = way(player);
        state.fallSafe = true;
        if (age >= 1) {
            Vec3 feet = player.m_20182_();
            Vec3 ahead = feet.m_82549_(dir.m_82490_(2.4));
            LivingEntity struck = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity body : FurutaCombat.around(player, 3.6)) {
                Vec3 centre = KaguneServerController.center(body);
                Vec3 onto = centre.m_82546_(feet);
                double along = Math.max(0.0, Math.min(2.4, onto.m_82526_(dir)));
                double off = centre.m_82554_(feet.m_82549_(dir.m_82490_(along)));
                if (off <= body.m_20205_() * 0.5 + 1.0 && along < best) {
                    best = along;
                    struck = body;
                }
            }
            if (struck != null || age > 1 && (player.m_20096_() || player.m_20069_())) {
                impact(player, state, struck, dir, ahead, now);
                return;
            }
        }
        AyatoFire.push(player, dir.m_82490_(FurutaRules.DIVE_SPEED));
    }

    /** The dive's impact: what the tails struck run through and stunned hard, the ground broken, everything round it thrown and stunned. */
    private static void impact(ServerPlayer player, FurutaServer.FurutaState state, LivingEntity struck, Vec3 dir, Vec3 ahead, long now) {
        Set<Integer> hit = FurutaCombat.hitSet(state, -1);
        Vec3 from = struck != null ? struck.m_20182_() : player.m_20182_();
        if (struck != null) {
            hit.add(struck.m_19879_());
            FurutaCombat.strikeBody(player, state, struck, FurutaRules.DIVE_DIRECT, dir.m_82490_(0.4), FurutaMove.STUN | FurutaMove.HEAVY | FurutaMove.BLEED,
                now, WoundBook.Blow.STAB);
            EtoBlitz.fx(player, S2CEtoFx.IMPALED, struck.m_19879_(), KaguneServerController.center(struck), dir, struck.m_20206_(), 0);
        }
        BlockHitResult ground = player.m_284548_().m_45547_(new ClipContext(from.m_82520_(0.0, 0.5, 0.0), from.m_82520_(0.0, -3.0, 0.0),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 at = ground.m_6662_() == HitResult.Type.BLOCK ? ground.m_82450_() : from;
        if (ground.m_6662_() == HitResult.Type.BLOCK) {
            BlockSmash.crater(player, at, 1.3);
        }
        FurutaCombat.ground(player, at, 4.0F);
        for (LivingEntity body : FurutaCombat.around(player, FurutaRules.DIVE_RADIUS + 1.5)) {
            Vec3 centre = KaguneServerController.center(body);
            double distance = Math.hypot(centre.f_82479_ - at.f_82479_, centre.f_82481_ - at.f_82481_);
            if (!hit.add(body.m_19879_())) {
                continue;
            }
            if (distance > FurutaRules.DIVE_RADIUS + body.m_20205_() * 0.5 || Math.abs(body.m_20186_() - at.f_82480_) > 2.5) {
                hit.remove(body.m_19879_());
                continue;
            }
            Vec3 away = AyatoFire.horizontal(centre.m_82546_(at));
            float power = FurutaRules.DIVE_SPLASH * (float)Math.max(0.35, 1.0 - distance / FurutaRules.DIVE_RADIUS);
            FurutaCombat.strikeBody(player, state, body, power, away.m_82490_(0.7), FurutaMove.STUN, now, WoundBook.Blow.SLAM);
        }
        AyatoFire.push(player, new Vec3(0.0, 0.1, 0.0));
        KaguneServerController.playAt(player, at, HexKaguneSounds.KAGUNE_SLAM, 1.0F, KaguneServerController.vary(player, 0.8F, 0.05F));
        AyatoFire.sound(player.m_284548_(), at, "entity.generic.explode", 0.5F, KaguneServerController.vary(player, 1.0F, 0.08F));
        FurutaCombat.end(player, state);
    }

    // ---------------------------------------------------------------- Human Puppet

    /** The throw: hoisted high on the tail, then hurled along his look (it smashes through what is in its way as it flies). */
    private static void hurl(ServerPlayer player, FurutaServer.FurutaState state, long age, long now) {
        if (!(state.heldId != 0 && player.m_284548_().m_6815_(state.heldId) instanceof LivingEntity held) || !held.m_6084_()) {
            return;
        }
        FurutaMove.Beat whip = FurutaMove.PUPPET_THROW.beats(FurutaCombat.limbs(player)).get(0);
        if (age < whip.land()) {
            if (!EtoStun.stun(held, now, 6)) {
                FurutaCombat.release(player, state, false); // 7.0.2: a player held too long breaks free
                FurutaServer.broadcast(player, state);
                return;
            }
            Vec3 where = FurutaCombat.around(player, FurutaRules.heldPoint(FurutaMove.PUPPET_THROW, whip.kind(), age));
            FurutaCombat.carry(held, where.m_82520_(0.0, -held.m_20206_() * 0.5, 0.0));
            return;
        }
        if (age == whip.land()) {
            Vec3 look = player.m_20154_();
            Vec3 flight = AyatoFire.horizontal(look).m_82490_(FurutaRules.THROW_SPEED).m_82520_(0.0, FurutaRules.THROW_LIFT + Math.max(-0.4, look.f_82480_), 0.0);
            FurutaCombat.strikeBody(player, state, held, whip.power(), Vec3.f_82478_, whip.flags(), now, WoundBook.Blow.SLAM);
            FurutaCombat.release(player, state, true);
            BlockSmash.launch(player, held, flight, FurutaCombat.THROWN);
            FurutaServer.broadcast(player, state);
            KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_SLAM, 0.8F, KaguneServerController.vary(player, 1.1F, 0.05F));
        }
    }

    /** The pull: hauled in on the tail to the pinned puppet, facing it, for his claws and the other tail. */
    private static void pull(ServerPlayer player, FurutaServer.FurutaState state, long age) {
        if (!(state.heldId != 0 && player.m_284548_().m_6815_(state.heldId) instanceof LivingEntity held) || !held.m_6084_()) {
            return;
        }
        Vec3 me = player.m_20182_();
        Vec3 gap = new Vec3(held.m_20185_() - me.f_82479_, 0.0, held.m_20189_() - me.f_82481_);
        double distance = gap.m_82553_();
        state.yaw = (float)Math.toDegrees(Math.atan2(-gap.f_82479_, gap.f_82481_));
        if (age <= 6 && distance > FurutaRules.PULL_CLOSE + held.m_20205_() * 0.5) {
            double speed = Math.min(FurutaRules.PULL_SPEED, (distance - FurutaRules.PULL_CLOSE) * 0.7);
            double lift = age == 0 ? 0.18 : player.m_20184_().f_82480_;
            AyatoFire.push(player, gap.m_82490_(speed / Math.max(1.0E-6, distance)).m_82520_(0.0, lift, 0.0));
            state.fallSafe = true;
        }
    }

    /** Held where it stands: its own way along the ground taken from it (it still falls). */
    static void pin(LivingEntity body) {
        Vec3 v = body.m_20184_();
        body.m_20256_(new Vec3(0.0, Math.min(0.0, v.f_82480_), 0.0));
        body.f_19864_ = true;
    }
}
