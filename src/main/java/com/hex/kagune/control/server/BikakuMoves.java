package com.hex.kagune.control.server;

import com.hex.kagune.control.bikaku.BikakuMove;
import com.hex.kagune.control.bikaku.BikakuRules;
import com.hex.kagune.control.bikaku.MawMotion;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 3.5.0: the Bikaku worm's moves on the server (its keys: {@link BikakuMove}): each checked (mastery alone opens them: the owner's "a
 * custom NPC" will teach them later), paid for, aimed, and run beat by beat on the motion's own timing ({@link MawMotion}), so what the
 * clients draw and what the server does stay together. Devouring Maw and Predatory Drag hold what they catch at the worm's mouth, where
 * {@link MawMotion#mawHeld} and {@link MawMotion#dragHeld} put it, moving it there every tick.
 */
final class BikakuMoves {
    private BikakuMoves() {
    }

    /** True while a move is still being made (the worm is busy). */
    static boolean busy(BikakuServer.State state, long now) {
        return state.move != BikakuMove.NONE;
    }

    /** A move key: the move on it, if it is open, off cooldown and paid for. */
    static void key(ServerPlayer player, BikakuServer.State state, BikakuMove.Slot slot, Entity hint, long now) {
        key(player, state, slot, hint, now, BikakuMove.of(slot, state.kakuja).baseReach());
    }

    /** 3.5.8: as above, a thrown move thrown as far as {@code reach} (held for). */
    static void key(ServerPlayer player, BikakuServer.State state, BikakuMove.Slot slot, Entity hint, long now, double reach) {
        BikakuMove move = BikakuMove.of(slot, state.kakuja); // 3.5.7: in the kakuja, its own moves on the same keys
        if (move == BikakuMove.NONE) {
            if (state.kakuja) {
                BikakuServer.notice(player, state, Component.m_237115_("message.hexkagune.bikaku.kakuja_none"));
            } else {
                BikakuServer.notYet(player);
            }
            return;
        }
        if (!move.unlocked(KaguneProfile.mastery(player))) {
            BikakuServer.notice(player, state, Component.m_237110_("message.hexkagune.bikaku.locked", move.displayName(), move.mastery()));
            return;
        }
        Long ready = state.readyAt.get(move);
        if (ready != null && now < ready && now >= ready - 100000L) {
            BikakuServer.notice(player, state, Component.m_237110_("message.hexkagune.bikaku.cooldown", move.displayName(), (ready - now + 19) / 20));
            return;
        }
        if (!KaguneServerController.useEnergy(player, move.cost(), true)) {
            return;
        }
        if (move == BikakuMove.UNDYING) {
            undying(player, state, move, now);
            return;
        }
        if (!move.occupies()) {
            regenerate(player, state, move, now);
            return;
        }
        start(player, state, move, hint, now, reach);
    }

    /** 3.5.7: Undying Abomination begins (its tentacles keep doing what they do). */
    private static void undying(ServerPlayer player, BikakuServer.State state, BikakuMove move, long now) {
        state.undyingAt = now;
        state.undyingSlowUntil = Long.MIN_VALUE;
        state.readyAt.put(move, now + Cooldowns.of(player, (long)move.cooldown()));
        UkakuFire.sound(player.m_284548_(), player.m_20182_(), "hexkagune:sfx.transform.organic_armor_shift", 1.0F, 0.75F);
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_RESHAPE, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
        BikakuServer.broadcast(player, state);
    }

    /** 3.5.3: Monstrous Regeneration begins (the worm keeps doing what it does). */
    private static void regenerate(ServerPlayer player, BikakuServer.State state, BikakuMove move, long now) {
        state.regenAt = now;
        state.readyAt.put(move, now + Cooldowns.of(player, (long)move.cooldown()));
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_RESHAPE, 1.0F, KaguneServerController.vary(player, 0.7F, 0.05F));
        BikakuServer.broadcast(player, state);
    }

    private static void start(ServerPlayer player, BikakuServer.State state, BikakuMove move, Entity hint, long now, double held) {
        if (move.kakuja()) {
            state.moveReach = BikakuRules.KAKUJA_REACH;
            state.moveSlower = 1.0;
            startKakuja(player, state, move, hint, now);
            return;
        }
        // 3.5.8: as far as it was held for, every beat slower for it.
        double reach = Math.max(move.baseReach(), held);
        state.moveReach = reach;
        state.moveSlower = BikakuRules.slower(reach, move.baseReach());
        java.util.Arrays.fill(state.moveRoutes, null);
        state.calmWorms();
        int worms = BikakuServer.worms(player, state);
        if (worms >= 2) {
            formStart(player, state, move, hint, now, reach, worms); // 3.6.1: every worm of the form makes it
            return;
        }
        LivingEntity target;
        switch (move) {
            case DRAG -> {
                // 3.5.9: along the ground round blocks: the hinted body if there is a way to it, else as before.
                target = aimGround(player, hint, reach);
                if (hint instanceof LivingEntity living && living != target && living.m_6084_() && KaguneServerController.validSwipeTarget(player, living)
                    && flat(KaguneServerController.center(living).m_82546_(player.m_20182_())) <= reach + living.m_20205_()
                    && groundRoute(player, state, living, reach) != null) {
                    target = living;
                }
                state.moveRoutes[0] = target == null ? null : bending(groundRoute(player, state, target, reach));
            }
            case ERUPTION -> target = aimGround(player, hint, reach);
            case PURSUIT -> target = BikakuServer.aimFrom(player, hint, BikakuServer.shoulder(player), reach);
            default -> {
                // 3.6.0: from its first worm's shoulder (in its lane, in a form of several).
                BikakuServer.Aim aim = BikakuServer.aimRouted(player, hint, BikakuServer.shoulder(player, state, 0), reach);
                target = aim.body();
                state.moveRoutes[0] = bending(aim.route());
            }
        }
        state.move = move;
        state.moveAt = now;
        java.util.Arrays.fill(state.targets, 0);
        state.targets[0] = target == null ? 0 : target.m_19879_();
        state.movePoint = target != null ? KaguneServerController.center(target) : switch (move) {
            case DRAG -> groundAhead(player, reach * 0.8);
            case ERUPTION -> groundAhead(player, reach * 0.7);
            // 3.5.9: at nothing, thrown along the look as far as the first block (not through it).
            default -> BikakuRoutes.clipped(player, BikakuServer.shoulder(player, state, 0), player.m_146892_().m_82549_(player.m_20154_().m_82490_(reach * 0.85)));
        };
        if (move == BikakuMove.PURSUIT) {
            pursuitTargets(player, state, target);
            // 3.5.9: each worm's way round blocks from its own lane.
            for (int i = 0; i < BikakuMove.PURSUIT_WORMS; i++) {
                if (state.targets[i] != 0 && player.m_284548_().m_6815_(state.targets[i]) instanceof LivingEntity body) {
                    state.moveRoutes[i] = bending(BikakuRoutes.route(player, BikakuServer.world(player, MawMotion.pursuitShoulder(i)),
                        KaguneServerController.center(body), reach + body.m_20205_()));
                }
            }
        }
        state.eruptVictim = 0;
        state.heldId = 0;
        state.caught = false;
        state.heldAt = null;
        state.done = 0;
        state.readyAt.put(move, now + Cooldowns.of(player, (long)move.cooldown()));
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.95F, KaguneServerController.vary(player, 0.7F, 0.05F));
        BikakuServer.broadcast(player, state);
    }

    /** Each tick of the move it is making: its beats, what it holds, its end. */
    static void tick(ServerPlayer player, BikakuServer.State state, long now) {
        long real = now - state.moveAt;
        BikakuMove move = state.move;
        if (real < 0) {
            finish(player, state);
            return;
        }
        if (state.moveWorms >= 2 && !move.kakuja()) {
            formTick(player, state, move, real, now);
            return;
        }
        // 3.5.8: a move thrown further runs every beat slower ({@code age} on its own beats; what it holds moved to the next real tick's).
        double k = Math.max(1.0, state.moveSlower);
        long age = (long)Math.floor(real / k);
        double next = (real + 1) / k;
        switch (move) {
            case MAW, DRAG -> grabTick(player, state, move, age, next, now);
            case PURSUIT -> pursuitTick(player, state, age, now);
            case ERUPTION -> eruptionTick(player, state, age, now);
            case JAWS -> jawsTick(player, state, age, now);
            case HUNGER -> hungerTick(player, state, age, now);
            case CATACLYSM -> cataclysmTick(player, state, age, now);
            default -> {
            }
        }
        if (age >= (state.caught ? move.ticks() : move.missedTicks())) {
            finish(player, state);
        }
    }

    /** True this tick while one of its moves' mouths is shutting on something (its wounds are the teeth's punctures). */
    static boolean biting(BikakuServer.State state, long now) {
        if (state.moveWorms >= 2 && !state.move.kakuja()) {
            return formBiting(state, now);
        }
        long age = (long)Math.floor((now - state.moveAt) / Math.max(1.0, state.moveSlower)); // 3.5.8: on its own beats
        if (state.move == BikakuMove.PURSUIT) {
            for (int i = 0; i < BikakuMove.PURSUIT_WORMS; i++) {
                if (age == BikakuMove.pursuitLands(i)) {
                    return true;
                }
            }
            return false;
        }
        // 3.5.7: the kakuja's bites.
        if (state.move == BikakuMove.JAWS) {
            for (int i = 0; i < com.hex.kagune.control.bikaku.MawKakuja.tentacles(); i++) {
                if (BikakuMove.jawsBites(i, age)) {
                    return true;
                }
            }
            return false;
        }
        if (state.move == BikakuMove.HUNGER) {
            for (int bite : BikakuMove.HUNGER_BITES) {
                if (age == bite) {
                    return true;
                }
            }
            return age == BikakuMove.HUNGER.land() || age == BikakuMove.HUNGER_CRUSH;
        }
        if (state.move == BikakuMove.CATACLYSM) {
            return false;
        }
        for (int i = 0; i < state.autoAt.length; i++) {
            if (state.autoAt[i] != Long.MIN_VALUE && now - state.autoAt[i] == BikakuRules.Bite.TENTACLE.land()) {
                return true;
            }
        }
        return state.move != BikakuMove.NONE && age == state.move.land();
    }

    /** A grab's beats: its bite, what it holds, the fling or the slam. */
    private static void grabTick(ServerPlayer player, BikakuServer.State state, BikakuMove move, long age, double next, long now) {
        if (state.done == 0 && age >= move.land()) {
            state.done = 1;
            land(player, state, move, now);
        }
        if (state.heldId != 0) {
            long until = move == BikakuMove.MAW ? BikakuMove.MAW_RELEASE : BikakuMove.DRAG_SLAM;
            if (age < until) {
                double[] at = move == BikakuMove.MAW ? MawMotion.mawHeld(state.heldAt, next) : MawMotion.dragHeld(state.heldAt, next);
                if (state.moveRoutes[0] != null) {
                    // 3.5.9: pulled back the way it went, round the blocks.
                    double[][] route = BikakuRoutes.rig(player, state.moveRoutes[0]);
                    int worms = BikakuServer.worms(player, state);
                    at = MawMotion.along(at, move == BikakuMove.MAW ? MawMotion.wormShoulder(worms, 0) : MawMotion.wormPoint(worms, 0, MawMotion.groundAnchor()),
                        route[route.length - 1], route);
                }
                hold(player, state, at);
            }
        }
        if (state.done == 1 && state.caught) {
            if (move == BikakuMove.MAW && age >= BikakuMove.MAW_RELEASE) {
                state.done = 2;
                fling(player, state, now);
            } else if (move == BikakuMove.DRAG && age >= BikakuMove.DRAG_SLAM) {
                state.done = 2;
                slam(player, state, now);
            }
        }
    }

    // ---------------------------------------------------------------- 3.5.1: Ravenous Pursuit

    /** Its three worms' bodies: the aimed one first, then the nearest others in front in reach; fewer, and they share (all on one). */
    private static void pursuitTargets(ServerPlayer player, BikakuServer.State state, LivingEntity first) {
        java.util.List<LivingEntity> picked = new java.util.ArrayList<>();
        if (first != null) {
            picked.add(first);
        }
        Vec3 eye = player.m_146892_();
        Vec3 look = player.m_20154_();
        double reach = state.moveReach;
        java.util.List<LivingEntity> near = new java.util.ArrayList<>(player.m_284548_().m_6443_(LivingEntity.class, player.m_20191_().m_82400_(reach + 1.0),
            living -> fair(player, living) && KaguneServerController.center(living).m_82554_(eye) <= reach + living.m_20205_()));
        near.sort(java.util.Comparator.comparingDouble(body -> body.m_20280_(player)));
        for (LivingEntity body : near) {
            if (picked.size() >= BikakuMove.PURSUIT_WORMS) {
                break;
            }
            Vec3 to = KaguneServerController.center(body).m_82546_(eye);
            if (picked.contains(body) || to.m_82556_() < 1.0E-6 || to.m_82541_().m_82526_(look) < 0.2) {
                continue;
            }
            picked.add(body);
        }
        // Each worm after the body on its own side: the one over his right shoulder the rightmost, over his left the leftmost, the one
        // rearing behind his head the one between (two bodies: it takes the aimed one; one body: all three on it).
        java.util.List<LivingEntity> bySide = new java.util.ArrayList<>(picked);
        bySide.sort(java.util.Comparator.comparingDouble(body -> -BikakuServer.rig(player, KaguneServerController.center(body))[0]));
        LivingEntity[] worms = new LivingEntity[BikakuMove.PURSUIT_WORMS];
        if (bySide.size() >= 3) {
            worms[0] = bySide.get(0);
            worms[1] = bySide.get(bySide.size() - 1);
            worms[2] = bySide.get(1);
        } else if (bySide.size() == 2) {
            worms[0] = bySide.get(0);
            worms[1] = bySide.get(1);
            worms[2] = picked.get(0);
        } else if (bySide.size() == 1) {
            java.util.Arrays.fill(worms, bySide.get(0));
        }
        for (int i = 0; i < BikakuMove.PURSUIT_WORMS; i++) {
            state.targets[i] = worms[i] == null ? 0 : worms[i].m_19879_();
        }
    }

    private static void pursuitTick(ServerPlayer player, BikakuServer.State state, long age, long now) {
        for (int i = 0; i < BikakuMove.PURSUIT_WORMS; i++) {
            int bit = 1 << i;
            if ((state.done & bit) != 0 || age < BikakuMove.pursuitLands(i)) {
                continue;
            }
            state.done |= bit;
            Vec3 eye = player.m_146892_();
            LivingEntity body = null;
            if (state.targets[i] != 0 && player.m_284548_().m_6815_(state.targets[i]) instanceof LivingEntity target && target.m_6084_()
                && (state.moveRoutes[i] != null ? BikakuRoutes.atEnd(player, state.moveRoutes[i], KaguneServerController.center(target),
                    BikakuRules.MOUTH_RADIUS + 1.0 + target.m_20205_()) : fair(player, target)
                    && KaguneServerController.center(target).m_82554_(eye) <= state.moveReach + target.m_20205_())) {
                body = target;
            }
            if (body == null) {
                Vec3 to = state.movePoint.m_82546_(eye);
                double d = to.m_82553_();
                Vec3 mouth = d <= state.moveReach || d < 1.0E-6 ? state.movePoint : eye.m_82549_(to.m_82490_(state.moveReach / d));
                body = nearest(player, mouth);
            }
            if (body != null) {
                hurt(player, body, BikakuRules.PURSUIT_POWER, 3, 0, BikakuRules.BITE_HEAL, now, false);
            } else {
                KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.6F, KaguneServerController.vary(player, 1.0F, 0.1F));
            }
        }
    }

    // ---------------------------------------------------------------- 3.5.1: Carrion Eruption

    private static void eruptionTick(ServerPlayer player, BikakuServer.State state, long age, long now) {
        if (state.done == 0 && age >= BikakuMove.ERUPT_RISE) {
            // It has come under its mark: where it bursts up is fixed (where its mark stands now), and every client is told.
            state.done = 1;
            if (state.targets[0] != 0 && player.m_284548_().m_6815_(state.targets[0]) instanceof LivingEntity target && target.m_6084_()
                && flat(target.m_20182_().m_82546_(player.m_20182_())) <= state.moveReach + 2.0) {
                state.movePoint = KaguneServerController.center(target);
                state.eruptVictim = target.m_19879_();
            }
            state.targets[0] = 0;
            KaguneServerController.playAt(player, state.movePoint, HexKaguneSounds.KAGUNE_ANCHOR, 1.0F, KaguneServerController.vary(player, 0.7F, 0.05F));
            BikakuServer.broadcast(player, state);
        }
        if (state.done == 1 && age >= BikakuMove.ERUPTION.land()) {
            state.done = 2;
            double[] spot = MawMotion.eruptSpot(BikakuServer.rig(player, state.movePoint), state.moveReach);
            Vec3 at = BikakuServer.world(player, spot);
            double r = BikakuRules.ERUPTION_RADIUS;
            AABB area = new AABB(at.f_82479_ - r, at.f_82480_ - 0.6, at.f_82481_ - r, at.f_82479_ + r, at.f_82480_ + 3.2, at.f_82481_ + r);
            boolean any = false;
            for (LivingEntity body : player.m_284548_().m_6443_(LivingEntity.class, area, living -> living != player && living.m_6084_()
                && KaguneServerController.validSwipeTarget(player, living))) {
                if (flat(body.m_20182_().m_82546_(at)) > r + body.m_20205_() * 0.5) {
                    continue;
                }
                boolean first = !any;
                if (hurt(player, body, BikakuRules.ERUPTION_POWER, 3, 0, first ? BikakuRules.BITE_HEAL : 0.0F, now, true)) {
                    any = true;
                    Vec3 out = body.m_20182_().m_82546_(at);
                    Vec3 aside = flat(out) < 1.0E-3 ? Vec3.f_82478_ : new Vec3(out.f_82479_, 0.0, out.f_82481_).m_82541_().m_82490_(0.2);
                    body.m_20256_(body.m_20184_().m_82490_(0.2).m_82549_(aside).m_82520_(0.0, BikakuRules.ERUPTION_LAUNCH, 0.0));
                    body.f_19864_ = true;
                }
            }
            KaguneServerController.playAt(player, at, HexKaguneSounds.BONE_CRUSH, 1.0F, KaguneServerController.vary(player, 0.7F, 0.05F));
        }
    }

    private static void finish(ServerPlayer player, BikakuServer.State state) {
        java.util.Arrays.fill(state.moveRoutes, null); // 3.5.9
        release(player, state, false);
        state.calmWorms(); // 3.6.1
        state.move = BikakuMove.NONE;
        state.moveAt = Long.MIN_VALUE;
        java.util.Arrays.fill(state.targets, 0);
        state.caught = false;
        state.heldAt = null;
        state.done = 0;
        BikakuServer.broadcast(player, state);
    }

    /** Lets go of what it holds ({@code thrown}: it keeps the speed it was thrown at; else it is stopped where it is). */
    static void release(ServerPlayer player, BikakuServer.State state, boolean thrown) {
        for (int i = 0; i < state.held.length; i++) {
            letWormGo(player, state, i, thrown); // 3.6.1: every worm's catch
        }
        if (state.heldId != 0 && !thrown) {
            Entity entity = player.m_9236_().m_6815_(state.heldId);
            if (entity != null) {
                entity.m_20256_(Vec3.f_82478_);
                entity.f_19864_ = true;
                entity.f_19789_ = 0.0F;
            }
        }
        state.heldId = 0;
    }

    // ---------------------------------------------------------------- 3.6.1: every worm of the form making the move

    /**
     * 3.6.1 (the owner: "to do enhanced versions of themselves whenever they're, like, in different transformations ... you can send all
     * three of them ... the thing that sends the worm underground and then up can be like more than one"): a worm move in a form of
     * several: each worm goes after its own body (the aimed one first, then the nearest others in front in reach; fewer bodies, and they all
     * go after the first), from its own lane, its part {@link BikakuMove#stagger} ticks after the worm before.
     */
    private static void formStart(ServerPlayer player, BikakuServer.State state, BikakuMove move, Entity hint, long now, double reach, int worms) {
        state.move = move;
        state.moveAt = now;
        state.moveWorms = worms;
        java.util.Arrays.fill(state.targets, 0);
        LivingEntity first = switch (move) {
            case DRAG, ERUPTION -> {
                LivingEntity ground = aimGround(player, hint, reach);
                if (move == BikakuMove.DRAG && hint instanceof LivingEntity living && living != ground && living.m_6084_()
                    && KaguneServerController.validSwipeTarget(player, living)
                    && flat(KaguneServerController.center(living).m_82546_(player.m_20182_())) <= reach + living.m_20205_()
                    && wormGroundRoute(player, worms, 0, living, reach) != null) {
                    ground = living;
                }
                yield ground;
            }
            default -> BikakuServer.aimRouted(player, hint, BikakuServer.shoulder(player, state, 0), reach).body();
        };
        LivingEntity[] bodies = wormBodies(player, move, first, reach, worms);
        Vec3 ahead = move == BikakuMove.DRAG ? groundAhead(player, reach * 0.8) : move == BikakuMove.ERUPTION ? groundAhead(player, reach * 0.7)
            : BikakuRoutes.clipped(player, BikakuServer.shoulder(player, state, 0), player.m_146892_().m_82549_(player.m_20154_().m_82490_(reach * 0.85)));
        for (int i = 0; i < worms; i++) {
            LivingEntity body = bodies[i];
            state.targets[i] = body == null ? 0 : body.m_19879_();
            state.points[i] = body != null ? KaguneServerController.center(body) : ahead;
            if (body != null) {
                state.moveRoutes[i] = switch (move) {
                    case DRAG -> bending(wormGroundRoute(player, worms, i, body, reach));
                    case ERUPTION -> null;
                    default -> bending(BikakuRoutes.route(player, BikakuServer.shoulder(player, state, i), KaguneServerController.center(body),
                        reach + body.m_20205_()));
                };
            }
        }
        // Worm 0's part, for the single fields.
        state.movePoint = state.points[0];
        state.eruptVictim = 0;
        state.heldId = 0;
        state.caught = false;
        state.heldAt = null;
        state.done = 0;
        state.readyAt.put(move, now + Cooldowns.of(player, (long)move.cooldown()));
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 1.0F, KaguneServerController.vary(player, 0.62F, 0.05F));
        BikakuServer.broadcast(player, state);
    }

    /**
     * The bodies a form's worms go after: the first (the aimed one), then the nearest fair others in front in reach (by side for Ravenous
     * Pursuit: the rightmost to the worm out to his right, the leftmost to the one to his left, between to the one behind his head); fewer, and
     * the rest go after the first (all of them on one body when there is only one).
     */
    private static LivingEntity[] wormBodies(ServerPlayer player, BikakuMove move, LivingEntity first, double reach, int worms) {
        java.util.List<LivingEntity> picked = new java.util.ArrayList<>();
        if (first != null) {
            picked.add(first);
        }
        Vec3 eye = player.m_146892_();
        Vec3 look = player.m_20154_();
        boolean ground = move == BikakuMove.DRAG || move == BikakuMove.ERUPTION;
        java.util.List<LivingEntity> near = new java.util.ArrayList<>(player.m_284548_().m_6443_(LivingEntity.class, player.m_20191_().m_82400_(reach + 1.0),
            living -> fair(player, living) && (ground ? flat(KaguneServerController.center(living).m_82546_(player.m_20182_())) <= reach + living.m_20205_()
                && Math.abs(living.m_20186_() - player.m_20186_()) < 4.0 : KaguneServerController.center(living).m_82554_(eye) <= reach + living.m_20205_())));
        near.sort(java.util.Comparator.comparingDouble(body -> body.m_20280_(player)));
        for (LivingEntity body : near) {
            if (picked.size() >= worms) {
                break;
            }
            Vec3 to = KaguneServerController.center(body).m_82546_(eye);
            if (picked.contains(body) || to.m_82556_() < 1.0E-6 || to.m_82541_().m_82526_(look) < 0.2) {
                continue;
            }
            picked.add(body);
        }
        LivingEntity[] out = new LivingEntity[worms];
        if (picked.isEmpty()) {
            return out;
        }
        if (move == BikakuMove.PURSUIT && picked.size() >= 2) {
            java.util.List<LivingEntity> bySide = new java.util.ArrayList<>(picked);
            bySide.sort(java.util.Comparator.comparingDouble(body -> -BikakuServer.rig(player, KaguneServerController.center(body))[0]));
            out[0] = bySide.get(0);
            out[1] = bySide.get(bySide.size() - 1);
            if (worms > 2) {
                out[2] = bySide.size() >= 3 ? bySide.get(1) : picked.get(0);
            }
            return out;
        }
        for (int i = 0; i < worms; i++) {
            out[i] = i < picked.size() ? picked.get(i) : picked.get(0);
        }
        return out;
    }

    /** Worm {@code i}'s way along the ground round blocks, from before its own right foot (in its lane) to {@code body}'s feet. */
    private static double[][] wormGroundRoute(ServerPlayer player, int worms, int i, LivingEntity body, double reach) {
        return BikakuRoutes.route(player, BikakuServer.world(player, MawMotion.wormPoint(worms, i, MawMotion.groundAnchor())),
            body.m_20182_().m_82520_(0.0, 0.4, 0.0), reach + body.m_20205_() + 2.0);
    }

    /** Each tick of a form's worm move: every worm's part on its own beats; the move ends when the last worm's has. */
    private static void formTick(ServerPlayer player, BikakuServer.State state, BikakuMove move, long real, long now) {
        double k = Math.max(1.0, state.moveSlower);
        boolean going = false;
        for (int i = 0; i < state.moveWorms; i++) {
            long mine = real - (long)i * move.stagger();
            if (mine < 0) {
                going = true;
                continue;
            }
            long age = (long)Math.floor(mine / k);
            double next = (mine + 1) / k;
            long end;
            switch (move) {
                case MAW, DRAG -> {
                    wormGrab(player, state, move, i, age, next, now);
                    end = state.caughts[i] ? move.ticks() : move.missedTicks();
                }
                case PURSUIT -> {
                    wormChain(player, state, i, age, now);
                    end = BikakuMove.chainLands(BikakuMove.CHAIN_BITES - 1) + 3 + BikakuMove.CHAIN_AFTER;
                }
                case ERUPTION -> {
                    wormErupt(player, state, i, age, now);
                    end = move.ticks();
                }
                default -> end = 0;
            }
            if (age < end) {
                going = true;
            } else if (state.held[i] != 0) {
                letWormGo(player, state, i, false);
            }
        }
        if (!going) {
            finish(player, state);
        }
    }

    /** True this tick while one of a form's worms is shutting its mouth on something (its wound is a bite's). */
    private static boolean formBiting(BikakuServer.State state, long now) {
        double k = Math.max(1.0, state.moveSlower);
        for (int i = 0; i < state.moveWorms; i++) {
            long mine = now - state.moveAt - (long)i * state.move.stagger();
            if (mine < 0) {
                continue;
            }
            long age = (long)Math.floor(mine / k);
            if (state.move == BikakuMove.PURSUIT) {
                for (int j = 0; j < BikakuMove.CHAIN_BITES; j++) {
                    if (age == BikakuMove.chainLands(j)) {
                        return true;
                    }
                }
            } else if (age == state.move.land()) {
                return true;
            }
        }
        return false;
    }

    /** Worm {@code i}'s Devouring Maw or Predatory Drag: its bite, what it holds (in its own lane), the fling or the slam. */
    private static void wormGrab(ServerPlayer player, BikakuServer.State state, BikakuMove move, int i, long age, double next, long now) {
        if (state.dones[i] == 0 && age >= move.land()) {
            state.dones[i] = 1;
            landWorm(player, state, move, i, now);
        }
        if (state.held[i] != 0 && state.heldAts[i] != null) {
            long until = move == BikakuMove.MAW ? BikakuMove.MAW_RELEASE : BikakuMove.DRAG_SLAM;
            if (age < until) {
                int worms = state.moveWorms;
                // Held where its own worm holds it (worked out in its own lane, as every client draws it), along its way round blocks.
                double[] own = MawMotion.wormUnpoint(worms, i, state.heldAts[i]);
                double[] at = MawMotion.wormPoint(worms, i, move == BikakuMove.MAW ? MawMotion.mawHeld(own, next) : MawMotion.dragHeld(own, next));
                if (state.moveRoutes[i] != null) {
                    double[][] route = BikakuRoutes.rig(player, state.moveRoutes[i]);
                    at = MawMotion.along(at, move == BikakuMove.MAW ? MawMotion.wormShoulder(worms, i) : MawMotion.wormPoint(worms, i, MawMotion.groundAnchor()),
                        route[route.length - 1], route);
                }
                holdWorm(player, state, i, at);
            }
        }
        if (state.dones[i] == 1 && state.caughts[i]) {
            if (move == BikakuMove.MAW && age >= BikakuMove.MAW_RELEASE) {
                state.dones[i] = 2;
                flingWorm(player, state, i);
            } else if (move == BikakuMove.DRAG && age >= BikakuMove.DRAG_SLAM) {
                state.dones[i] = 2;
                slamWorm(player, state, i, now);
            }
        }
    }

    /** Worm {@code i}'s mouth shuts: on its body if it is still in reach (round blocks, along its way), else on whatever is at its mouth. */
    private static void landWorm(ServerPlayer player, BikakuServer.State state, BikakuMove move, int i, long now) {
        LivingEntity body = state.targets[i] != 0 && player.m_284548_().m_6815_(state.targets[i]) instanceof LivingEntity target && target.m_6084_()
            && KaguneServerController.validSwipeTarget(player, target) ? target : null;
        Vec3 mouth = state.points[i] != null ? state.points[i] : player.m_146892_();
        if (body != null) {
            Vec3 centre = KaguneServerController.center(body);
            boolean reached;
            if (state.moveRoutes[i] != null) {
                reached = BikakuRoutes.atEnd(player, state.moveRoutes[i], centre, BikakuRules.MOUTH_RADIUS + 1.6 + body.m_20205_());
            } else if (move == BikakuMove.DRAG) {
                reached = fair(player, body) && flat(centre.m_82546_(player.m_20182_())) <= state.moveReach + body.m_20205_();
            } else {
                reached = fair(player, body) && centre.m_82554_(BikakuServer.shoulder(player, state, i)) <= state.moveReach + body.m_20205_();
            }
            body = reached ? body : null;
        } else {
            body = nearest(player, mouth);
        }
        if (body == null) {
            KaguneServerController.playAt(player, mouth, HexKaguneSounds.KAGUNE_WHIP, 0.6F, KaguneServerController.vary(player, 0.6F, 0.05F));
            syncWorm0(state);
            BikakuServer.broadcast(player, state);
            return;
        }
        boolean maw = move == BikakuMove.MAW;
        int heldFor = maw ? BikakuMove.MAW_RELEASE - move.land() + BikakuRules.GRAB_STUN_AFTER : BikakuMove.DRAG_SLAM - move.land() + 4;
        boolean bit = hurt(player, body, maw ? BikakuRules.MAW_POWER : BikakuRules.DRAG_BITE_POWER, maw ? 4 : 2, heldFor,
            maw ? BikakuRules.MAW_HEAL : BikakuRules.BITE_HEAL, now, maw);
        // Only one worm holds a body: the others bite it and draw back (on one body, it is bitten once by each worm).
        boolean holdingAlready = false;
        for (int j = 0; j < state.moveWorms; j++) {
            holdingAlready |= j != i && state.held[j] == body.m_19879_();
        }
        if (bit && body.m_6084_() && !holdingAlready) {
            state.caughts[i] = true;
            state.held[i] = body.m_19879_();
            state.heldAts[i] = BikakuServer.rig(player, KaguneServerController.center(body));
            KaguneServerController.playAt(player, body, HexKaguneSounds.KAGUNE_GRAB, 0.9F, KaguneServerController.vary(player, 0.85F, 0.05F));
        }
        syncWorm0(state);
        BikakuServer.broadcast(player, state);
    }

    /** What worm {@code i} holds kept at its mouth, moved toward {@code middle} (the body's frame), or let go if it is gone. */
    private static void holdWorm(ServerPlayer player, BikakuServer.State state, int i, double[] middle) {
        Entity entity = player.m_9236_().m_6815_(state.held[i]);
        if (!(entity instanceof LivingEntity body) || !body.m_6084_() || body.m_213877_() || body.m_20280_(player) > 40.0 * 40.0) {
            state.held[i] = 0;
            syncWorm0(state);
            BikakuServer.broadcast(player, state);
            return;
        }
        Vec3 at = BikakuServer.world(player, middle);
        GraftCombat.carry(body, at.m_82520_(0.0, -body.m_20206_() * 0.52, 0.0));
    }

    /** Worm {@code i} of Devouring Maw lets go with a throw to its own side: the first out to his right, the second to his left, the third ahead and up. */
    private static void flingWorm(ServerPlayer player, BikakuServer.State state, int i) {
        Entity entity = state.held[i] != 0 ? player.m_9236_().m_6815_(state.held[i]) : null;
        state.held[i] = 0;
        if (entity instanceof LivingEntity body && body.m_6084_()) {
            double yaw = Math.toRadians(player.f_20883_);
            Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
            Vec3 ahead = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
            Vec3 throwWay = i == 1 ? right.m_82490_(-1.25).m_82549_(ahead.m_82490_(0.2)).m_82520_(0.0, 0.5, 0.0)
                : i == 2 ? ahead.m_82490_(1.1).m_82520_(0.0, 0.75, 0.0) : right.m_82490_(1.25).m_82549_(ahead.m_82490_(0.2)).m_82520_(0.0, 0.5, 0.0);
            body.m_20256_(throwWay);
            body.f_19864_ = true;
            KaguneServerController.playAt(player, body, HexKaguneSounds.KAGUNE_WHIP, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
        }
        syncWorm0(state);
        BikakuServer.broadcast(player, state);
    }

    /** Worm {@code i}'s Predatory Drag slams what it holds down, hurts and stuns it, lets go. */
    private static void slamWorm(ServerPlayer player, BikakuServer.State state, int i, long now) {
        Entity entity = state.held[i] != 0 ? player.m_9236_().m_6815_(state.held[i]) : null;
        state.held[i] = 0;
        if (entity instanceof LivingEntity body && body.m_6084_()) {
            state.slamming = true;
            hurt(player, body, BikakuRules.DRAG_SLAM_POWER, 4, BikakuRules.SLAM_STUN, BikakuRules.BITE_HEAL, now, true);
            state.slamming = false;
            body.m_20256_(new Vec3(0.0, 0.12, 0.0));
            body.f_19864_ = true;
            KaguneServerController.playAt(player, body, HexKaguneSounds.KAGUNE_SLAM, 1.0F, KaguneServerController.vary(player, 0.85F, 0.05F));
        }
        syncWorm0(state);
        BikakuServer.broadcast(player, state);
    }

    /** Lets go of what worm {@code i} holds ({@code thrown}: it keeps its speed; else it is stopped where it is). */
    private static void letWormGo(ServerPlayer player, BikakuServer.State state, int i, boolean thrown) {
        if (state.held[i] != 0 && !thrown) {
            Entity entity = player.m_9236_().m_6815_(state.held[i]);
            if (entity != null) {
                entity.m_20256_(Vec3.f_82478_);
                entity.f_19864_ = true;
                entity.f_19789_ = 0.0F;
            }
        }
        state.held[i] = 0;
    }

    /** Worm {@code i}'s chain of Ravenous Pursuit bites at its body (each once, on its own beats). */
    private static void wormChain(ServerPlayer player, BikakuServer.State state, int i, long age, long now) {
        for (int j = 0; j < BikakuMove.CHAIN_BITES; j++) {
            int bit = 1 << j;
            if ((state.dones[i] & bit) != 0 || age < BikakuMove.chainLands(j)) {
                continue;
            }
            state.dones[i] |= bit;
            LivingEntity body = state.targets[i] != 0 && player.m_284548_().m_6815_(state.targets[i]) instanceof LivingEntity target && target.m_6084_()
                && KaguneServerController.validSwipeTarget(player, target) ? target : null;
            if (body != null) {
                Vec3 centre = KaguneServerController.center(body);
                boolean reached = state.moveRoutes[i] != null
                    ? BikakuRoutes.atEnd(player, state.moveRoutes[i], centre, BikakuRules.MOUTH_RADIUS + 1.6 + body.m_20205_())
                    : fair(player, body) && centre.m_82554_(player.m_146892_()) <= state.moveReach + body.m_20205_() + 1.0;
                body = reached ? body : null;
            }
            if (body != null) {
                hurt(player, body, BikakuRules.PURSUIT_POWER, 3, 0, BikakuRules.BITE_HEAL, now, false);
            } else {
                KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.6F, KaguneServerController.vary(player, 1.0F, 0.1F));
            }
        }
    }

    /** Worm {@code i}'s Carrion Eruption: under its own body (its mark fixed as it comes under it), bursting up and throwing it. */
    private static void wormErupt(ServerPlayer player, BikakuServer.State state, int i, long age, long now) {
        if ((state.dones[i] & 1) == 0 && age >= BikakuMove.ERUPT_RISE) {
            state.dones[i] |= 1;
            if (state.targets[i] != 0 && player.m_284548_().m_6815_(state.targets[i]) instanceof LivingEntity target && target.m_6084_()
                && flat(target.m_20182_().m_82546_(player.m_20182_())) <= state.moveReach + 2.0) {
                state.points[i] = KaguneServerController.center(target);
                state.victims[i] = target.m_19879_();
            }
            state.targets[i] = 0;
            KaguneServerController.playAt(player, state.points[i], HexKaguneSounds.KAGUNE_ANCHOR, 1.0F, KaguneServerController.vary(player, 0.7F, 0.05F));
            syncWorm0(state);
            BikakuServer.broadcast(player, state);
        }
        if ((state.dones[i] & 2) == 0 && age >= BikakuMove.ERUPTION.land()) {
            state.dones[i] |= 2;
            double[] spot = MawMotion.eruptSpot(BikakuServer.rig(player, state.points[i]), state.moveReach);
            Vec3 at = BikakuServer.world(player, spot);
            double r = BikakuRules.ERUPTION_RADIUS;
            AABB area = new AABB(at.f_82479_ - r, at.f_82480_ - 0.6, at.f_82481_ - r, at.f_82479_ + r, at.f_82480_ + 3.2, at.f_82481_ + r);
            boolean any = false;
            for (LivingEntity body : player.m_284548_().m_6443_(LivingEntity.class, area, living -> living != player && living.m_6084_()
                && KaguneServerController.validSwipeTarget(player, living))) {
                if (flat(body.m_20182_().m_82546_(at)) > r + body.m_20205_() * 0.5) {
                    continue;
                }
                boolean first = !any;
                if (hurt(player, body, BikakuRules.ERUPTION_POWER, 3, 0, first ? BikakuRules.BITE_HEAL : 0.0F, now, true)) {
                    any = true;
                    Vec3 out = body.m_20182_().m_82546_(at);
                    Vec3 aside = flat(out) < 1.0E-3 ? Vec3.f_82478_ : new Vec3(out.f_82479_, 0.0, out.f_82481_).m_82541_().m_82490_(0.2);
                    body.m_20256_(body.m_20184_().m_82490_(0.2).m_82549_(aside).m_82520_(0.0, BikakuRules.ERUPTION_LAUNCH, 0.0));
                    body.f_19864_ = true;
                }
            }
            KaguneServerController.playAt(player, at, HexKaguneSounds.BONE_CRUSH, 1.0F, KaguneServerController.vary(player, 0.7F, 0.05F));
        }
    }

    /** Worm 0's part copied into the single fields (the clients' and the wounds' as before). */
    private static void syncWorm0(BikakuServer.State state) {
        state.heldId = state.held[0];
        state.caught = state.caughts[0];
        state.heldAt = state.heldAts[0];
        state.movePoint = state.points[0] != null ? state.points[0] : state.movePoint;
        state.eruptVictim = state.victims[0];
    }

    // ---------------------------------------------------------------- 3.5.7: the kakuja's own moves

    /** One of the kakuja's moves begins: its bodies chosen, its point, its sound. */
    private static void startKakuja(ServerPlayer player, BikakuServer.State state, BikakuMove move, Entity hint, long now) {
        Vec3 centre = BikakuServer.kakujaCentre(player);
        state.move = move;
        state.moveAt = now;
        java.util.Arrays.fill(state.targets, 0);
        state.eruptVictim = 0;
        state.heldId = 0;
        state.caught = false;
        state.heldAt = null;
        state.done = 0;
        state.struck.clear();
        LivingEntity aimed = BikakuServer.aimFrom(player, hint, centre, BikakuRules.KAKUJA_REACH);
        switch (move) {
            case JAWS -> {
                jawsTargets(player, state, aimed, centre);
                state.movePoint = aimed != null ? KaguneServerController.center(aimed) : centre;
                UkakuFire.sound(player.m_284548_(), centre, "hexkagune:sfx.writhe", 1.2F, 0.7F);
            }
            case HUNGER -> {
                state.targets[0] = aimed == null ? 0 : aimed.m_19879_();
                state.movePoint = aimed != null ? KaguneServerController.center(aimed)
                    : player.m_146892_().m_82549_(player.m_20154_().m_82490_(BikakuRules.KAKUJA_REACH * 0.7));
                UkakuFire.sound(player.m_284548_(), centre, "hexkagune:sfx.writhe", 1.2F, 0.6F);
            }
            default -> { // Flesh Cataclysm: drawing in
                state.movePoint = centre;
                UkakuFire.sound(player.m_284548_(), centre, "hexkagune:sfx.transform.body_warp", 1.3F, 0.7F);
            }
        }
        state.readyAt.put(move, now + Cooldowns.of(player, (long)move.cooldown()));
        BikakuServer.broadcast(player, state);
    }

    /** True if {@code body} is in the kakuja's reach (from its middle) and fair game. */
    private static boolean inKakujaReach(ServerPlayer player, LivingEntity body, Vec3 centre, double reach) {
        return body != null && fair(player, body) && KaguneServerController.center(body).m_82554_(centre) <= reach + body.m_20205_();
    }

    /**
     * Thousand Jaws' bodies: one for each mouthed tentacle, the body round it nearest the way that tentacle faces out of the mass (the aimed
     * one first, each body taken once while there are more; fewer, and they share).
     */
    private static void jawsTargets(ServerPlayer player, BikakuServer.State state, LivingEntity aimed, Vec3 centre) {
        java.util.List<LivingEntity> bodies = new java.util.ArrayList<>(player.m_284548_().m_6443_(LivingEntity.class,
            box(centre, BikakuRules.KAKUJA_REACH + 1.0), living -> inKakujaReach(player, living, centre, BikakuRules.KAKUJA_REACH)));
        java.util.Set<Integer> taken = new java.util.HashSet<>();
        for (int i = 0; i < com.hex.kagune.control.bikaku.MawKakuja.tentacles(); i++) {
            if (!com.hex.kagune.control.bikaku.MawKakuja.mouthed(i)) {
                continue;
            }
            LivingEntity best = null;
            double bestScore = -9.0;
            for (LivingEntity body : bodies) {
                double score = facing(player, i, body) + (body == aimed ? 0.6 : 0.0) - (taken.contains(body.m_19879_()) ? 2.0 : 0.0);
                if (score > bestScore) {
                    bestScore = score;
                    best = body;
                }
            }
            state.targets[i] = best == null ? 0 : best.m_19879_();
            if (best != null) {
                taken.add(best.m_19879_());
            }
        }
    }

    /** How much tentacle {@code i} faces {@code body} (the level way out of the mass toward it, -1 .. 1). */
    private static double facing(ServerPlayer player, int i, LivingEntity body) {
        double[] at = BikakuServer.rig(player, KaguneServerController.center(body));
        double[] way = com.hex.kagune.control.bikaku.MawKakuja.sweepWay(i);
        double flat = Math.hypot(at[0], at[2]);
        return flat < 1.0E-6 ? 0.0 : (at[0] * way[0] + at[2] * way[2]) / flat;
    }

    private static void jawsTick(ServerPlayer player, BikakuServer.State state, long age, long now) {
        Vec3 centre = BikakuServer.kakujaCentre(player);
        boolean told = false;
        for (int i = 0; i < com.hex.kagune.control.bikaku.MawKakuja.tentacles(); i++) {
            if (!com.hex.kagune.control.bikaku.MawKakuja.mouthed(i)) {
                continue;
            }
            for (int start : BikakuMove.jawsStarts(i)) {
                if (age == start) {
                    // Each bite: its body still there, or the next one its way.
                    LivingEntity had = state.targets[i] != 0 && player.m_284548_().m_6815_(state.targets[i]) instanceof LivingEntity body ? body : null;
                    if (!inKakujaReach(player, had, centre, BikakuRules.KAKUJA_REACH)) {
                        int before = state.targets[i];
                        LivingEntity next = nearestFacing(player, i, centre, BikakuRules.KAKUJA_REACH);
                        state.targets[i] = next == null ? 0 : next.m_19879_();
                        told |= before != state.targets[i];
                    }
                }
            }
            if (BikakuMove.jawsBites(i, age)) {
                LivingEntity body = state.targets[i] != 0 && player.m_284548_().m_6815_(state.targets[i]) instanceof LivingEntity target ? target : null;
                if (inKakujaReach(player, body, centre, BikakuRules.KAKUJA_REACH)) {
                    hurt(player, body, BikakuRules.JAWS_POWER, 2, 0, BikakuRules.JAWS_HEAL, now, false);
                } else {
                    KaguneServerController.playAt(player, centre, HexKaguneSounds.KAGUNE_WHIP, 0.5F, KaguneServerController.vary(player, 0.9F, 0.1F));
                }
            }
        }
        // Its grins snap at whatever is close to it: a big bite, a stun, pulled in against it.
        for (int snap : BikakuMove.JAWS_SNAPS) {
            if (age == snap) {
                AABB near = box(centre, BikakuRules.GRIN_REACH + 1.0);
                for (LivingEntity body : player.m_284548_().m_6443_(LivingEntity.class, near, living -> inKakujaReach(player, living, centre,
                    BikakuRules.GRIN_REACH))) {
                    if (hurt(player, body, BikakuRules.GRIN_POWER, 3, BikakuRules.GRIN_STUN, BikakuRules.BITE_HEAL, now, true) && body.m_6084_()) {
                        Vec3 in = centre.m_82546_(body.m_20182_());
                        Vec3 pull = new Vec3(in.f_82479_, 0.0, in.f_82481_);
                        body.m_20256_(pull.m_82556_() < 1.0E-6 ? Vec3.f_82478_ : pull.m_82541_().m_82490_(0.35));
                        body.f_19864_ = true;
                    }
                }
                UkakuFire.sound(player.m_284548_(), centre, "hexkagune:sfx.dragon_bite", 1.1F, 0.8F);
            }
        }
        if (told) {
            BikakuServer.broadcast(player, state);
        }
    }

    /** The fair body in reach that tentacle {@code i} faces most (or null). */
    private static LivingEntity nearestFacing(ServerPlayer player, int i, Vec3 centre, double reach) {
        LivingEntity best = null;
        double bestScore = -9.0;
        for (LivingEntity body : player.m_284548_().m_6443_(LivingEntity.class, box(centre, reach + 1.0),
            living -> inKakujaReach(player, living, centre, reach))) {
            double score = facing(player, i, body);
            if (score > bestScore) {
                bestScore = score;
                best = body;
            }
        }
        return best;
    }

    private static void hungerTick(ServerPlayer player, BikakuServer.State state, long age, long now) {
        Vec3 centre = BikakuServer.kakujaCentre(player);
        int land = BikakuMove.HUNGER.land();
        if (state.done == 0 && age >= land) {
            state.done = 1;
            LivingEntity body = state.targets[0] != 0 && player.m_284548_().m_6815_(state.targets[0]) instanceof LivingEntity target ? target : null;
            if (!inKakujaReach(player, body, centre, BikakuRules.KAKUJA_REACH)) {
                body = nearest(player, state.movePoint);
            }
            int held = BikakuMove.HUNGER_RELEASE - land + 4;
            if (body != null && hurt(player, body, BikakuRules.HUNGER_POWER, 3, held, BikakuRules.HUNGER_HEAL, now, false)) {
                state.caught = true;
                state.targets[0] = body.m_19879_();
                if (body.m_6084_()) {
                    state.heldId = body.m_19879_();
                    state.heldAt = BikakuServer.rig(player, KaguneServerController.center(body));
                    KaguneServerController.playAt(player, body, HexKaguneSounds.KAGUNE_GRAB, 1.0F, KaguneServerController.vary(player, 0.75F, 0.05F));
                }
            } else {
                KaguneServerController.playAt(player, state.movePoint, HexKaguneSounds.KAGUNE_WHIP, 0.7F, KaguneServerController.vary(player, 0.6F, 0.05F));
            }
            BikakuServer.broadcast(player, state);
        }
        if (!state.caught) {
            return;
        }
        if (state.heldId != 0 && state.heldAt != null && age < BikakuMove.HUNGER_RELEASE) {
            hold(player, state, com.hex.kagune.control.bikaku.KakujaActs.hungerHeld(state.heldAt, age + 1));
        }
        Entity entity = state.heldId != 0 ? player.m_9236_().m_6815_(state.heldId) : null;
        LivingEntity held = entity instanceof LivingEntity living && living.m_6084_() ? living : null;
        for (int bite : BikakuMove.HUNGER_BITES) {
            if (age == bite && held != null) {
                hurt(player, held, BikakuRules.HUNGER_POWER, 3, 0, BikakuRules.HUNGER_HEAL, now, false);
            }
        }
        if (age == BikakuMove.HUNGER_CRUSH) {
            if (held != null) {
                hurt(player, held, BikakuRules.HUNGER_CRUSH_POWER, 5, BikakuRules.HUNGER_STUN, BikakuRules.HUNGER_CRUSH_HEAL, now, true);
            }
            UkakuFire.sound(player.m_284548_(), BikakuServer.world(player, com.hex.kagune.control.bikaku.KakujaActs.MAW_GRIP), "hexkagune:sfx.dragon_bite",
                1.4F, 0.65F);
        }
        if (age == BikakuMove.HUNGER_RELEASE && state.heldId != 0) {
            // Let go: spat out before it.
            state.heldId = 0;
            if (held != null) {
                double yaw = Math.toRadians(player.f_20883_);
                Vec3 ahead = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
                held.m_20256_(ahead.m_82490_(0.9).m_82520_(0.0, 0.45, 0.0));
                held.f_19864_ = true;
            }
            BikakuServer.broadcast(player, state);
        }
    }

    private static void cataclysmTick(ServerPlayer player, BikakuServer.State state, long age, long now) {
        Vec3 centre = BikakuServer.kakujaCentre(player);
        Vec3 feet = player.m_20182_();
        int burst = BikakuMove.CATACLYSM.land();
        double reach = BikakuRules.KAKUJA_REACH * 1.15;
        if (age == burst) {
            // It bursts out: every mouth roars; what is close is hurled away.
            UkakuFire.sound(player.m_284548_(), centre, "hexkagune:sfx.dragon_roar", 2.0F, 0.6F);
            UkakuFire.sound(player.m_284548_(), centre, "hexkagune:kagune_burst", 1.4F, 0.6F);
            for (LivingEntity body : player.m_284548_().m_6443_(LivingEntity.class, box(centre, BikakuRules.CATA_NEAR + 1.0),
                living -> inKakujaReach(player, living, centre, BikakuRules.CATA_NEAR))) {
                state.struck.add(body.m_19879_());
                if (hurt(player, body, BikakuRules.CATA_NEAR_POWER, 2, BikakuRules.CATA_STUN, 0.0F, now, true)) {
                    throwOut(body, feet, BikakuRules.CATA_THROW_NEAR, 0.55);
                }
            }
        }
        if (age > burst && age <= BikakuMove.CATA_SWEPT) {
            // Its tentacles sweep round it: whatever one passes is struck hard, once.
            double time = player.m_9236_().m_46467_();
            java.util.List<double[]> heads = new java.util.ArrayList<>();
            for (int i = 0; i < com.hex.kagune.control.bikaku.MawKakuja.tentacles(); i++) {
                heads.add(com.hex.kagune.control.bikaku.KakujaActs.cataclysmHead(i, age, time));
            }
            for (LivingEntity body : player.m_284548_().m_6443_(LivingEntity.class, box(centre, reach + 1.0),
                living -> !state.struck.contains(living.m_19879_()) && inKakujaReach(player, living, centre, reach))) {
                double[] at = BikakuServer.rig(player, KaguneServerController.center(body));
                double angle = Math.atan2(at[2], at[0]);
                double far = Math.hypot(at[0], at[2]);
                for (double[] head : heads) {
                    double headAngle = Math.atan2(head[2], head[0]);
                    double off = Math.abs(Math.atan2(Math.sin(angle - headAngle), Math.cos(angle - headAngle)));
                    if (off <= BikakuRules.CATA_ARC && far <= Math.hypot(head[0], head[2]) + 1.5 && at[1] < head[1] + 3.5) {
                        state.struck.add(body.m_19879_());
                        if (hurt(player, body, BikakuRules.CATA_POWER, 3, BikakuRules.CATA_STUN, 0.0F, now, true)) {
                            throwOut(body, feet, BikakuRules.CATA_THROW, 0.35);
                        }
                        break;
                    }
                }
            }
        }
    }

    /** 3.5.9: Predatory Drag's way along the ground round blocks, from before his right foot to {@code body}'s feet (null: none in reach). */
    private static double[][] groundRoute(ServerPlayer player, BikakuServer.State state, LivingEntity body, double reach) {
        return BikakuRoutes.route(player, BikakuServer.world(player, MawMotion.wormPoint(BikakuServer.worms(player, state), 0, MawMotion.groundAnchor())),
            body.m_20182_().m_82520_(0.0, 0.4, 0.0), reach + body.m_20205_() + 2.0);
    }

    /** {@code route} if it bends round something, else null (a straight line). */
    private static double[][] bending(double[][] route) {
        return BikakuRoutes.bends(route) ? route : null;
    }

    /** A box {@code r} out each way round {@code at}. */
    private static AABB box(Vec3 at, double r) {
        return new AABB(at.f_82479_ - r, at.f_82480_ - r, at.f_82481_ - r, at.f_82479_ + r, at.f_82480_ + r, at.f_82481_ + r);
    }

    /** {@code body} thrown out from {@code from} (level, {@code out} hard) and up. */
    private static void throwOut(LivingEntity body, Vec3 from, double out, double up) {
        if (!body.m_6084_()) {
            return;
        }
        Vec3 away = body.m_20182_().m_82546_(from);
        Vec3 flat = new Vec3(away.f_82479_, 0.0, away.f_82481_);
        flat = flat.m_82556_() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : flat.m_82541_();
        body.m_20256_(flat.m_82490_(out).m_82520_(0.0, up, 0.0));
        body.f_19864_ = true;
    }

    // ---------------------------------------------------------------- 3.5.7: Autonomous Predation

    /**
     * Each tick in the kakuja: its mouthed tentacles bite hostile bodies near it on their own, each at its own pace and never several at
     * once ({@link BikakuRules#AUTO_EVERY}, {@link BikakuRules#AUTO_GAP}); never one a move of its own is using.
     */
    static void autoTick(ServerPlayer player, BikakuServer.State state, long now) {
        Vec3 centre = BikakuServer.kakujaCentre(player);
        int lands = BikakuRules.Bite.TENTACLE.land();
        boolean told = false;
        double[] point = state.move.kakuja() ? BikakuServer.rig(player, state.movePoint) : null;
        java.util.List<LivingEntity> hostiles = null;
        for (int i = 0; i < com.hex.kagune.control.bikaku.MawKakuja.tentacles() && i < state.autoAt.length; i++) {
            if (!com.hex.kagune.control.bikaku.MawKakuja.mouthed(i)) {
                continue;
            }
            long since = state.autoAt[i] == Long.MIN_VALUE ? Long.MAX_VALUE : now - state.autoAt[i];
            if (since < com.hex.kagune.control.bikaku.KakujaActs.autoTicks()) {
                if (!state.autoShut[i] && since >= lands) {
                    state.autoShut[i] = true;
                    LivingEntity body = state.autoTarget[i] != 0 && player.m_284548_().m_6815_(state.autoTarget[i]) instanceof LivingEntity target ? target : null;
                    if (inKakujaReach(player, body, centre, BikakuRules.AUTO_REACH)) {
                        hurt(player, body, BikakuRules.AUTO_POWER, 2, 0, BikakuRules.AUTO_HEAL, now, false);
                    }
                }
                continue;
            }
            if (now < state.autoReadyAt[i] || now < state.nextAutoAt || now % 5L != i % 5L
                || com.hex.kagune.control.bikaku.KakujaActs.uses(state.move, i, point)) {
                continue;
            }
            if (hostiles == null) {
                hostiles = new java.util.ArrayList<>(player.m_284548_().m_6443_(LivingEntity.class, box(centre, BikakuRules.AUTO_REACH
                    + 1.0), living -> hostile(player, living) && inKakujaReach(player, living, centre, BikakuRules.AUTO_REACH)));
            }
            LivingEntity best = null;
            double bestScore = 0.0;
            for (LivingEntity body : hostiles) {
                double score = facing(player, i, body);
                if (best == null || score > bestScore) {
                    best = body;
                    bestScore = score;
                }
            }
            if (best == null || bestScore < -0.2) {
                continue;
            }
            state.autoAt[i] = now;
            state.autoTarget[i] = best.m_19879_();
            state.autoShut[i] = false;
            state.autoReadyAt[i] = now + BikakuRules.AUTO_EVERY + 7L * i;
            state.nextAutoAt = now + BikakuRules.AUTO_GAP;
            KaguneServerController.playAt(player, centre, HexKaguneSounds.KAGUNE_WHIP, 0.7F, KaguneServerController.vary(player, 0.75F, 0.08F));
            told = true;
        }
        if (told) {
            BikakuServer.broadcast(player, state);
        }
    }

    /** 3.5.7: a body that has come for him (as the Owl's partner judges it): a monster that isn't merely neutral, one hunting him, or what last hurt him. */
    static boolean hostile(ServerPlayer player, LivingEntity body) {
        if (body == player || !body.m_6084_()) {
            return false;
        }
        if (player.m_21188_() == body) {
            return true;
        }
        if (body instanceof net.minecraft.world.entity.Mob mob && mob.m_5448_() == player) {
            return true;
        }
        return body instanceof net.minecraft.world.entity.monster.Enemy
            && (!(body instanceof net.minecraft.world.entity.NeutralMob) || body instanceof net.minecraft.world.entity.Mob mob && mob.m_5448_() != null);
    }

    // ---------------------------------------------------------------- the bite and the catch

    /** The mouth shuts: on its target if it is still in reach and in sight, else on whatever is at the mouth; caught, it holds on. */
    private static void land(ServerPlayer player, BikakuServer.State state, BikakuMove move, long now) {
        LivingEntity body = null;
        Vec3 mouth;
        if (move == BikakuMove.DRAG) {
            Vec3 feet = player.m_20182_();
            if (state.targets[0] != 0 && player.m_284548_().m_6815_(state.targets[0]) instanceof LivingEntity target && fair(player, target)
                && flat(KaguneServerController.center(target).m_82546_(feet)) <= state.moveReach + target.m_20205_()) {
                body = target;
            }
            double[] reach = MawMotion.dragReach(BikakuServer.rig(player, state.movePoint), state.moveReach);
            mouth = BikakuServer.world(player, reach);
        } else {
            Vec3 from = BikakuServer.shoulder(player, state, 0);
            if (state.targets[0] != 0 && player.m_284548_().m_6815_(state.targets[0]) instanceof LivingEntity target && fair(player, target)
                && KaguneServerController.center(target).m_82554_(from) <= state.moveReach + target.m_20205_()) {
                body = target;
            }
            mouth = BikakuServer.mouth(from, state.movePoint, state.moveReach);
        }
        if (state.moveRoutes[0] != null) {
            // 3.5.9: round blocks: what it went after, if it is still where its way ends (else nothing).
            double[][] route = state.moveRoutes[0];
            body = state.targets[0] != 0 && player.m_284548_().m_6815_(state.targets[0]) instanceof LivingEntity target && target.m_6084_()
                && KaguneServerController.validSwipeTarget(player, target) && BikakuRoutes.atEnd(player, route, KaguneServerController.center(target),
                BikakuRules.MOUTH_RADIUS + 1.6 + target.m_20205_()) ? target : null;
            double[] end = route[route.length - 1];
            mouth = new Vec3(end[0], end[1], end[2]);
        } else if (body == null) {
            body = nearest(player, mouth);
        }
        if (body == null) {
            KaguneServerController.playAt(player, mouth, HexKaguneSounds.KAGUNE_WHIP, 0.6F, KaguneServerController.vary(player, 0.6F, 0.05F));
            BikakuServer.broadcast(player, state);
            return;
        }
        boolean maw = move == BikakuMove.MAW;
        int held = maw ? BikakuMove.MAW_RELEASE - move.land() + BikakuRules.GRAB_STUN_AFTER : BikakuMove.DRAG_SLAM - move.land() + 4;
        boolean bit = hurt(player, body, maw ? BikakuRules.MAW_POWER : BikakuRules.DRAG_BITE_POWER, maw ? 4 : 2, held,
            maw ? BikakuRules.MAW_HEAL : BikakuRules.BITE_HEAL, now, maw);
        state.caught = true;
        state.targets[0] = body.m_19879_();
        if (bit && body.m_6084_()) {
            state.heldId = body.m_19879_();
            state.heldAt = BikakuServer.rig(player, KaguneServerController.center(body));
            KaguneServerController.playAt(player, body, HexKaguneSounds.KAGUNE_GRAB, 0.9F, KaguneServerController.vary(player, 0.85F, 0.05F));
        } else {
            state.caught = bit; // killed by the bite: nothing left to hold, the worm draws back as from a catch
        }
        BikakuServer.broadcast(player, state);
    }

    /** What it holds is kept at the mouth: moved toward {@code middle} (the body's frame) each tick, or let go if it is gone. */
    private static void hold(ServerPlayer player, BikakuServer.State state, double[] middle) {
        Entity entity = player.m_9236_().m_6815_(state.heldId);
        if (!(entity instanceof LivingEntity body) || !body.m_6084_() || body.m_213877_() || body.m_20280_(player) > 24.0 * 24.0) {
            state.heldId = 0;
            BikakuServer.broadcast(player, state);
            return;
        }
        Vec3 at = BikakuServer.world(player, middle);
        GraftCombat.carry(body, at.m_82520_(0.0, -body.m_20206_() * 0.52, 0.0));
    }

    /** Devouring Maw lets go with a powerful sideways throw: out to his right and up. */
    private static void fling(ServerPlayer player, BikakuServer.State state, long now) {
        Entity entity = state.heldId != 0 ? player.m_9236_().m_6815_(state.heldId) : null;
        state.heldId = 0;
        if (entity instanceof LivingEntity body && body.m_6084_()) {
            double yaw = Math.toRadians(player.f_20883_);
            Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
            Vec3 ahead = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
            body.m_20256_(right.m_82490_(1.25).m_82549_(ahead.m_82490_(0.2)).m_82520_(0.0, 0.5, 0.0));
            body.f_19864_ = true;
            KaguneServerController.playAt(player, body, HexKaguneSounds.KAGUNE_WHIP, 1.0F, KaguneServerController.vary(player, 0.6F, 0.05F));
        }
        BikakuServer.broadcast(player, state);
    }

    /** Predatory Drag's slam: driven down onto the ground before him, hurt, stunned, let go. */
    private static void slam(ServerPlayer player, BikakuServer.State state, long now) {
        Entity entity = state.heldId != 0 ? player.m_9236_().m_6815_(state.heldId) : null;
        state.heldId = 0;
        if (entity instanceof LivingEntity body && body.m_6084_()) {
            state.slamming = true;
            hurt(player, body, BikakuRules.DRAG_SLAM_POWER, 4, BikakuRules.SLAM_STUN, BikakuRules.BITE_HEAL, now, true);
            state.slamming = false;
            body.m_20256_(new Vec3(0.0, 0.12, 0.0));
            body.f_19864_ = true;
            KaguneServerController.playAt(player, body, HexKaguneSounds.KAGUNE_SLAM, 1.0F, KaguneServerController.vary(player, 0.85F, 0.05F));
        }
        BikakuServer.broadcast(player, state);
    }

    /**
     * One of the worm's blows on {@code body}: its damage by mastery ({@code power} its share; a player takes {@link BikakuRules#PVP_SHARE}),
     * bleeding, a stun, Predatory Feeding ({@code heal}, more on a kill), mastery. True if it landed.
     */
    static boolean hurt(ServerPlayer player, LivingEntity body, float power, int bleed, int stun, float heal, long now, boolean heavy) {
        float damage = BikakuRules.damage(KaguneProfile.mastery(player)) * power;
        if (body instanceof net.minecraft.world.entity.player.Player) {
            damage *= BikakuRules.PVP_SHARE;
        }
        body.f_19802_ = 0;
        if (!body.m_6469_(player.m_269291_().m_269075_(player), damage)) {
            return false;
        }
        UkakuCrystalCombat.graftBleed(body, player, bleed, now);
        if (stun > 0) {
            PaleStun.stun(body, now, stun);
        }
        KaguneServerController.playAt(player, KaguneServerController.center(body), heavy ? HexKaguneSounds.BONE_CRUSH : HexKaguneSounds.FLESH_IMPACT,
            0.9F, KaguneServerController.vary(player, heavy ? 0.85F : 1.0F, 0.08F));
        float healed = heal + (body.m_6084_() ? 0.0F : BikakuRules.KILL_HEAL);
        if (healed > 0.0F && player.m_21223_() < player.m_21233_()) {
            GhoulRegen.healOwn(player, healed); // 3.5.2: a ghoul's own heal (half a heart was taken for food's and cancelled)
            KaguneNetwork.sendTo(player, player);
        }
        KaguneProfile.addMastery(player, "Bikaku", 1);
        ControlServer.onKaguneHit(player, 1);
        return true;
    }

    // ---------------------------------------------------------------- aim

    /** A body the worm may bite. */
    static boolean fair(ServerPlayer player, LivingEntity body) {
        return body != player && body.m_6084_() && KaguneServerController.validSwipeTarget(player, body) && KaguneServerController.clearLineTo(player, body);
    }

    /** The nearest fair body within {@link BikakuRules#MOUTH_RADIUS} of {@code mouth}, or null. */
    static LivingEntity nearest(ServerPlayer player, Vec3 mouth) {
        double r = BikakuRules.MOUTH_RADIUS + 1.0;
        AABB area = new AABB(mouth.f_82479_ - r, mouth.f_82480_ - r, mouth.f_82481_ - r, mouth.f_82479_ + r, mouth.f_82480_ + r, mouth.f_82481_ + r);
        LivingEntity best = null;
        double bestDistance = BikakuRules.MOUTH_RADIUS * BikakuRules.MOUTH_RADIUS;
        for (LivingEntity near : player.m_284548_().m_6443_(LivingEntity.class, area, living -> fair(player, living))) {
            double d = KaguneServerController.center(near).m_82557_(mouth);
            if (d <= bestDistance) {
                bestDistance = d;
                best = near;
            }
        }
        return best;
    }

    /** A move along the ground's target (Predatory Drag, Carrion Eruption): the hinted body if fair and in reach, else the nearest in front. */
    static LivingEntity aimGround(ServerPlayer player, Entity hint, double reach) {
        Vec3 feet = player.m_20182_();
        if (hint instanceof LivingEntity living && fair(player, living) && flat(KaguneServerController.center(living).m_82546_(feet)) <= reach + living.m_20205_()
            && Math.abs(living.m_20186_() - player.m_20186_()) < 4.0) {
            return living;
        }
        Vec3 look = player.m_20154_();
        Vec3 ahead = new Vec3(look.f_82479_, 0.0, look.f_82481_);
        ahead = ahead.m_82556_() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : ahead.m_82541_();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        AABB area = player.m_20191_().m_82400_(reach + 1.0);
        for (LivingEntity body : player.m_284548_().m_6443_(LivingEntity.class, area, living -> fair(player, living))) {
            Vec3 to = KaguneServerController.center(body).m_82546_(feet);
            double distance = flat(to);
            if (distance > reach + body.m_20205_() * 0.5 || distance < 1.0E-3 || Math.abs(body.m_20186_() - player.m_20186_()) >= 4.0) {
                continue;
            }
            double facing = (to.f_82479_ * ahead.f_82479_ + to.f_82481_ * ahead.f_82481_) / distance;
            if (facing < 0.8) {
                continue;
            }
            double score = distance * (2.0 - facing);
            if (score < bestScore) {
                bestScore = score;
                best = body;
            }
        }
        return best;
    }

    /** A move along the ground at nothing: along the ground ahead, {@code reach} out. */
    static Vec3 groundAhead(ServerPlayer player, double reach) {
        Vec3 look = player.m_20154_();
        Vec3 ahead = new Vec3(look.f_82479_, 0.0, look.f_82481_);
        ahead = ahead.m_82556_() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : ahead.m_82541_();
        return player.m_20182_().m_82549_(ahead.m_82490_(reach)).m_82520_(0.0, 0.99, 0.0);
    }

    private static double flat(Vec3 v) {
        return Math.sqrt(v.f_82479_ * v.f_82479_ + v.f_82481_ * v.f_82481_);
    }
}
