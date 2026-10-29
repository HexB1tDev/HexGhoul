package com.hex.kagune.control.server;

import com.hex.kagune.control.pale.ArmRules;
import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.pale.OwlShards;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CPaleFx;
import com.hex.kagune.control.network.S2COwlShard;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 6.2.0, the Owl's spikes (6.2.4: J held in it, on Flesh Grab's mastery; {@link OwlShards}): its head lowered, each burst fires a
 * fan of crystal spikes off its crest at what she looks at; each spear flies on its own (the world's, not hers: they fly on whatever she does
 * next), runs through the first body it meets (the hurt, a bleed, stunned) and carries it along its flight until it strikes a wall
 * (pinned there) or has carried it {@link OwlShards#PIN_CARRY} (pinned to the ground there); one that meets nothing sticks where it
 * hits, and every stuck spear crumbles a while later. Held: a storm, a burst every few ticks, each past the first two costing RC.
 * Every client flies the spears from {@link S2COwlShard} alone.
 */
final class OwlVolley {
    /** One spear: where and when it was thrown and how fast, what it carries (and how far it has), whether it has stuck (until when). */
    static final class Spear {
        final int id;
        final Vec3 from;
        final Vec3 speed;
        final long thrownAt;
        int carried;
        double carriedFor;
        Vec3 last;
        long stuckUntil = Long.MIN_VALUE;

        Spear(int id, Vec3 from, Vec3 speed, long thrownAt) {
            this.id = id;
            this.from = from;
            this.speed = speed;
            this.thrownAt = thrownAt;
            this.last = from;
        }

        Vec3 at(double t) {
            double[] p = OwlShards.at(new double[] {this.from.f_82479_, this.from.f_82480_, this.from.f_82481_},
                new double[] {this.speed.f_82479_, this.speed.f_82480_, this.speed.f_82481_}, t);
            return new Vec3(p[0], p[1], p[2]);
        }
    }

    /** What one spear pins: the body, where its feet are held, until when. */
    record Pin(int body, Vec3 feet, long until) {
    }

    /** One player's volley: the throws made of the move under way, when N was last heard held, her spears, what they pin. */
    static final class Volley {
        int throwsDone;
        long heardAt;
        int next;
        final List<Spear> spears = new ArrayList<>();
        final List<Pin> pins = new ArrayList<>();
    }

    private static final Map<UUID, Volley> VOLLEYS = new HashMap<>();

    private OwlVolley() {
    }

    /** N tapped ({@code held} false) or held in the Owl: the volley begins, if she can (her mastery, her RC, the arm's cooldown). */
    static void start(ServerPlayer player, PaleServer.PaleState state, boolean held, long now) {
        if (state.move == PaleMove.OWL_VOLLEY || !PaleCancel.allowedOwl(state, now) || now < state.grabReadyAt
            || PaleServer.locked(player, state, "Flesh Grab", PaleRules.GRAB_MASTERY)) {
            return;
        }
        if (!KaguneServerController.useEnergy(player, ArmRules.COST, true)) {
            return;
        }
        PaleCancel.cut(player, state, now);
        state.queuedAt = Long.MIN_VALUE;
        PaleServer.begin(player, state, PaleMove.OWL_VOLLEY, held ? 1 : 0, 0, 0.0F, null, now);
        Volley volley = VOLLEYS.computeIfAbsent(player.m_20148_(), ignored -> new Volley());
        volley.throwsDone = 0;
        volley.heardAt = now;
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_STRAIN, 0.8F, KaguneServerController.vary(player, 0.75F, 0.05F));
    }

    /** N held: {@code 2} as the hold starts (the storm begins), {@code 1} still down, {@code 0} let go. */
    static void key(ServerPlayer player, PaleServer.PaleState state, int down, long now) {
        if (state.move != PaleMove.OWL_VOLLEY) {
            if (down == 2) {
                start(player, state, true, now);
            }
            return;
        }
        Volley volley = VOLLEYS.get(player.m_20148_());
        if (down == 0) {
            letGo(player, state, now);
        } else if (volley != null) {
            volley.heardAt = now;
        }
    }

    /** The storm is let go (or she can throw no more): the throw under way is the last, and every client is told when. */
    private static void letGo(ServerPlayer player, PaleServer.PaleState state, long now) {
        if (state.move == PaleMove.OWL_VOLLEY && state.detail == 1 && state.charge <= 0.0F) {
            state.charge = Math.max(1.0F, now - state.startedAt);
            PaleServer.broadcast(player, state);
        }
    }

    /** Her move, each tick: the throws let go as they come (the storm's while it is held and her RC lasts); then she is her own again. */
    static void tickMove(ServerPlayer player, PaleServer.PaleState state, long now) {
        Volley volley = VOLLEYS.computeIfAbsent(player.m_20148_(), ignored -> new Volley());
        long age = now - state.startedAt;
        boolean held = state.detail == 1;
        if (held && state.charge <= 0.0F && now - volley.heardAt > PaleRules.HOLD_TIMEOUT) {
            letGo(player, state, now);
        }
        while (volley.throwsDone < OwlShards.throwsMade(held, state.charge) && OwlShards.throwAt(volley.throwsDone) <= age) {
            int k = volley.throwsDone;
            if (k >= 2 && !KaguneServerController.useEnergy(player, OwlShards.STORM_COST, true)) {
                letGo(player, state, now); // out of RC: the storm ends here
                break;
            }
            hurl(player, volley, k, now);
            volley.throwsDone++;
        }
        if (age >= OwlShards.ends(held, state.charge)) {
            state.grabReadyAt = now + Cooldowns.of(player, ArmRules.COOLDOWN);
            PaleServer.end(player, state);
        }
    }

    /** Throw {@code k}: its fan of spears ripped off the blade and hurled at what she looks at. */
    private static void hurl(ServerPlayer player, Volley volley, int k, long now) {
        ServerLevel level = player.m_284548_();
        Vec3 from = PaleWatch.world(player, player.m_146908_(), OwlShards.release(k));
        Vec3 at = aim(level, player);
        for (int j = 0; j < OwlShards.spears(k); j++) {
            double[] d = OwlShards.direction(new double[] {from.f_82479_, from.f_82480_, from.f_82481_}, new double[] {at.f_82479_, at.f_82480_, at.f_82481_}, k, j);
            Spear spear = new Spear(volley.next++, from, new Vec3(d[0], d[1], d[2]).m_82490_(OwlShards.SPEED), now);
            volley.spears.add(spear);
            ControlNetwork.broadcast(player, new S2COwlShard(player.m_20148_(), spear.id, S2COwlShard.THROWN, now, from.f_82479_, from.f_82480_,
                from.f_82481_, (float)spear.speed.f_82479_, (float)spear.speed.f_82480_, (float)spear.speed.f_82481_, 0));
        }
        UkakuFire.sound(level, from, "entity.player.attack.sweep", 1.0F, KaguneServerController.vary(player, 0.6F, 0.06F));
        KaguneServerController.playSelf(player, HexKaguneSounds.BONE_CRACK, 0.7F, KaguneServerController.vary(player, 1.1F, 0.08F));
    }

    /** What she aims at: the first body or block along where she looks (out to {@link OwlShards#AIM}), or that far along it. */
    static Vec3 aim(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.m_146892_();
        Vec3 look = player.m_20154_();
        Vec3 end = eye.m_82549_(look.m_82490_(OwlShards.AIM));
        HitResult hit = level.m_45547_(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double far = hit.m_6662_() == HitResult.Type.MISS ? OwlShards.AIM : hit.m_82450_().m_82554_(eye);
        Vec3 to = eye.m_82549_(look.m_82490_(far));
        double best = 1.0;
        Vec3 aimed = to;
        AABB around = new AABB(Math.min(eye.f_82479_, to.f_82479_), Math.min(eye.f_82480_, to.f_82480_), Math.min(eye.f_82481_, to.f_82481_),
            Math.max(eye.f_82479_, to.f_82479_), Math.max(eye.f_82480_, to.f_82480_), Math.max(eye.f_82481_, to.f_82481_)).m_82400_(1.0);
        for (LivingEntity body : level.m_6443_(LivingEntity.class, around, entity -> PaleBlitz.valid(player, entity))) {
            double t = enter(body.m_20191_().m_82400_(0.3), eye, to);
            if (t >= 0.0 && t < best) {
                best = t;
                aimed = KaguneServerController.center(body);
            }
        }
        return aimed;
    }

    /** Each tick of hers: every spear flies on, runs through, carries, pins, sticks and crumbles; what they pin is held there. */
    static void tick(ServerPlayer player, PaleServer.PaleState state, long now) {
        Volley volley = VOLLEYS.get(player.m_20148_());
        if (volley == null) {
            return;
        }
        ServerLevel level = player.m_284548_();
        for (Iterator<Spear> it = volley.spears.iterator(); it.hasNext(); ) {
            Spear spear = it.next();
            if (spear.stuckUntil != Long.MIN_VALUE) {
                if (now >= spear.stuckUntil) {
                    it.remove();
                    send(player, spear, S2COwlShard.GONE, now, spear.last, Vec3.f_82478_, 0);
                }
                continue;
            }
            fly(player, level, volley, spear, now);
            if (spear.stuckUntil == Long.MIN_VALUE && now - spear.thrownAt > OwlShards.LIFE && spear.carried == 0) {
                it.remove();
                send(player, spear, S2COwlShard.GONE, now, spear.last, Vec3.f_82478_, 0); // spent: it falls apart in the air
            }
        }
        for (Iterator<Pin> it = volley.pins.iterator(); it.hasNext(); ) {
            Pin pin = it.next();
            LivingEntity body = level.m_6815_(pin.body()) instanceof LivingEntity living && living.m_6084_() ? living : null;
            if (body == null || now >= pin.until()) {
                it.remove();
                continue;
            }
            CentipedeHunt.place(player, body, pin.feet());
        }
        if (volley.spears.isEmpty() && volley.pins.isEmpty() && state.move != PaleMove.OWL_VOLLEY) {
            VOLLEYS.remove(player.m_20148_());
        }
    }

    /** One tick of one spear's flight, from where it was to where it is now. */
    private static void fly(ServerPlayer player, ServerLevel level, Volley volley, Spear spear, long now) {
        Vec3 from = spear.last;
        Vec3 to = spear.at(now - spear.thrownAt);
        Vec3 way = to.m_82546_(from);
        double step = way.m_82553_();
        Vec3 dir = step < 1.0E-6 ? spear.speed.m_82541_() : way.m_82490_(1.0 / step);
        HitResult wall = level.m_45547_(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double wallAt = wall.m_6662_() == HitResult.Type.BLOCK ? wall.m_82450_().m_82554_(from) / Math.max(1.0E-6, step) : 2.0;
        if (spear.carried == 0) {
            // The first body along the way this tick (before any wall) is run through.
            LivingEntity hit = null;
            double hitAt = Math.min(1.0, wallAt);
            AABB around = new AABB(Math.min(from.f_82479_, to.f_82479_), Math.min(from.f_82480_, to.f_82480_), Math.min(from.f_82481_, to.f_82481_),
                Math.max(from.f_82479_, to.f_82479_), Math.max(from.f_82480_, to.f_82480_), Math.max(from.f_82481_, to.f_82481_)).m_82400_(1.0);
            for (LivingEntity body : level.m_6443_(LivingEntity.class, around, entity -> PaleBlitz.valid(player, entity))) {
                double t = enter(body.m_20191_().m_82400_(0.15), from, to);
                if (t >= 0.0 && t <= hitAt) {
                    hitAt = t;
                    hit = body;
                }
            }
            if (hit != null && strike(player, level, spear, hit, dir, now)) {
                spear.carried = hit.m_19879_();
                spear.carriedFor = step * (1.0 - hitAt);
            }
        } else {
            spear.carriedFor += step;
        }
        LivingEntity carried = spear.carried == 0 ? null
            : level.m_6815_(spear.carried) instanceof LivingEntity living && living.m_6084_() ? living : null;
        if (spear.carried != 0 && carried == null) {
            spear.carried = 0; // it died on the spear, or is gone: the spear flies on
        }
        if (wallAt <= 1.0) {
            // Into a wall or the ground: stuck there, and what it carries pinned to it.
            Vec3 point = wall.m_82450_();
            spear.last = point;
            spear.stuckUntil = now + OwlShards.STUCK;
            int pinned = 0;
            if (carried != null) {
                Vec3 centre = point.m_82546_(dir.m_82490_(carried.m_20205_() * 0.5 + 0.25));
                Vec3 feet = centre.m_82520_(0.0, -carried.m_20206_() * 0.5, 0.0);
                pin(player, volley, carried, feet, now);
                pinned = carried.m_19879_();
            }
            send(player, spear, S2COwlShard.STUCK, now, point, dir, pinned);
            UkakuFire.sound(level, point, "block.stone.hit", 1.0F, KaguneServerController.vary(player, 0.7F, 0.08F));
            return;
        }
        spear.last = to;
        if (carried != null) {
            if (spear.carriedFor >= OwlShards.PIN_CARRY) {
                // Carried as far as it goes: driven down through it into the ground there.
                Vec3 ground = ground(level, player, to);
                Vec3 down = new Vec3(dir.f_82479_ * 0.6, -0.8, dir.f_82481_ * 0.6).m_82541_();
                Vec3 point = ground.m_82549_(down.m_82490_(0.3));
                pin(player, volley, carried, ground, now);
                spear.last = point;
                spear.stuckUntil = now + OwlShards.STUCK;
                send(player, spear, S2COwlShard.STUCK, now, point, down, carried.m_19879_());
                UkakuFire.sound(level, ground, "block.gravel.break", 1.2F, KaguneServerController.vary(player, 0.6F, 0.05F));
                return;
            }
            // On the spear: its middle just behind the point, carried along.
            Vec3 centre = to.m_82546_(dir.m_82490_(0.35));
            CentipedeHunt.place(player, carried, centre.m_82520_(0.0, -carried.m_20206_() * 0.5, 0.0));
        }
    }

    /** The spear through {@code body}: the hurt, a bleed, stunned, blood out of its back. False when it does not take it. */
    private static boolean strike(ServerPlayer player, ServerLevel level, Spear spear, LivingEntity body, Vec3 dir, long now) {
        Vec3 centre = KaguneServerController.center(body);
        body.f_19802_ = 0;
        WoundHits.expect(body, com.hex.kagune.control.wound.WoundBook.Blow.STAB, centre.m_82546_(dir.m_82490_(body.m_20205_() * 0.5)), dir, 0.0);
        if (!body.m_6469_(player.m_269291_().m_269075_(player), PaleRules.fist(KaguneProfile.mastery(player)) * OwlShards.DAMAGE)) {
            return false;
        }
        UkakuCrystalCombat.rinkakuBleed(body, player, now);
        PaleStun.stun(body, now, OwlShards.PINNED + 10);
        PaleBlitz.fx(player, S2CPaleFx.IMPALED, body.m_19879_(), centre, dir, body.m_20206_(), 1);
        KaguneServerController.playAt(player, body, HexKaguneSounds.BONE_CRACK, 0.7F, KaguneServerController.vary(player, 1.0F, 0.08F));
        UkakuFire.sound(level, centre, "entity.player.attack.strong", 0.8F, KaguneServerController.vary(player, 0.8F, 0.05F));
        KaguneProfile.addMastery(player, "Swipe", 1);
        KaguneNetwork.broadcast(player);
        PaleServer.landed(player, 1);
        return true;
    }

    /** {@code body} pinned with its feet at {@code feet}: held there (and stunned) {@link OwlShards#PINNED} ticks, or longer if pinned again. */
    private static void pin(ServerPlayer player, Volley volley, LivingEntity body, Vec3 feet, long now) {
        volley.pins.removeIf(pin -> pin.body() == body.m_19879_());
        volley.pins.add(new Pin(body.m_19879_(), feet, now + OwlShards.PINNED));
        PaleStun.stun(body, now, OwlShards.PINNED);
        CentipedeHunt.place(player, body, feet);
    }

    /** The ground's top under {@code at} (from a block above it down six), or straight under it at her feet's height. */
    private static Vec3 ground(ServerLevel level, ServerPlayer player, Vec3 at) {
        HitResult hit = level.m_45547_(new ClipContext(at.m_82520_(0.0, 1.0, 0.0), at.m_82520_(0.0, -6.0, 0.0), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, player));
        return hit.m_6662_() == HitResult.Type.BLOCK ? hit.m_82450_() : new Vec3(at.f_82479_, player.m_20186_(), at.f_82481_);
    }

    /** How far (0..1) along {@code a} to {@code b} the segment enters {@code box}, or -1 if it never does. */
    static double enter(AABB box, Vec3 a, Vec3 b) {
        double[] from = {a.f_82479_, a.f_82480_, a.f_82481_};
        double[] way = {b.f_82479_ - a.f_82479_, b.f_82480_ - a.f_82480_, b.f_82481_ - a.f_82481_};
        double[] min = {box.f_82288_, box.f_82289_, box.f_82290_};
        double[] max = {box.f_82291_, box.f_82292_, box.f_82293_};
        double enter = 0.0;
        double leave = 1.0;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(way[i]) < 1.0E-12) {
                if (from[i] < min[i] || from[i] > max[i]) {
                    return -1.0;
                }
                continue;
            }
            double t0 = (min[i] - from[i]) / way[i];
            double t1 = (max[i] - from[i]) / way[i];
            enter = Math.max(enter, Math.min(t0, t1));
            leave = Math.min(leave, Math.max(t0, t1));
            if (enter > leave) {
                return -1.0;
            }
        }
        return enter;
    }

    private static void send(ServerPlayer player, Spear spear, int kind, long now, Vec3 at, Vec3 dir, int body) {
        ControlNetwork.broadcast(player, new S2COwlShard(player.m_20148_(), spear.id, kind, now, at.f_82479_, at.f_82480_, at.f_82481_, (float)dir.f_82479_,
            (float)dir.f_82480_, (float)dir.f_82481_, body));
    }

    /** Someone has started seeing her: every spear of hers still out, so it is drawn at once (stuck ones where they stuck). */
    static void tracked(ServerPlayer viewer, ServerPlayer subject) {
        Volley volley = VOLLEYS.get(subject.m_20148_());
        if (volley == null) {
            return;
        }
        for (Spear spear : volley.spears) {
            if (spear.stuckUntil == Long.MIN_VALUE) {
                ControlNetwork.sendTo(viewer, new S2COwlShard(subject.m_20148_(), spear.id, S2COwlShard.THROWN, spear.thrownAt, spear.from.f_82479_,
                    spear.from.f_82480_, spear.from.f_82481_, (float)spear.speed.f_82479_, (float)spear.speed.f_82480_, (float)spear.speed.f_82481_, 0));
            } else {
                Vec3 dir = spear.speed.m_82541_();
                ControlNetwork.sendTo(viewer, new S2COwlShard(subject.m_20148_(), spear.id, S2COwlShard.STUCK, subject.m_9236_().m_46467_(), spear.last.f_82479_,
                    spear.last.f_82480_, spear.last.f_82481_, (float)dir.f_82479_, (float)dir.f_82480_, (float)dir.f_82481_, 0));
            }
        }
    }

    /** Everything of hers gone at once (her form put away, death, another world, gone): spears and what they pin let go. */
    static void forget(ServerPlayer player) {
        Volley volley = VOLLEYS.remove(player.m_20148_());
        if (volley != null && !volley.spears.isEmpty()) {
            ControlNetwork.broadcast(player, new S2COwlShard(player.m_20148_(), -1, S2COwlShard.CLEAR, player.m_9236_().m_46467_(), 0.0, 0.0, 0.0, 0.0F, 0.0F,
                0.0F, 0));
        }
    }
}
