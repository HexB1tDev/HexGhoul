package com.hex.kagune.control.server;

import com.hex.kagune.control.family.ChildKaguneRules;
import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.family.Genome;
import com.hex.kagune.control.family.KaguneGenes;
import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.graft.GraftRules;
import com.hex.kagune.control.graft.GraftStage;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CGhoulNpc;
import com.hex.kagune.control.network.S2CPaleFx;
import com.hex.kagune.control.network.S2CUkakuFx;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.NpcIdentity;
import com.hex.kagune.control.npc.NpcTemper;
import com.hex.kagune.control.ukaku.WingForm;
import com.hex.kagune.control.ukaku.WingRig;
import com.hex.kagune.control.wound.WoundBook;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.3.43: a ghoul child's kagune in a fight, on the server ({@link ChildKaguneRules}): the tails' string (the transplant's lashes, every
 * beat landing on the tick its clients draw it, as a wild ghoul's: {@code GhoulNpcCombat}), the wing's or wings' fire shots (from the
 * tip of the wing it aims, flown a tick at a time, burning what they hit), all at its size, and with tails and a wing both: the tails up
 * close, the wing further off. Its clients are told as a wild ghoul's are ({@link S2CGhoulNpc}, under the child's id), and draw it from
 * what the child inherited. Its blows land only on what it fights and on monsters about it: never a player, a companion or a child.
 */
final class ChildKagune {
    /** Shot ids clear of the players' and the wild ghouls' ({@code GhoulNpcCombat} counts up from 1 << 30). */
    private static int nextBolt = 3 << 29;

    /** Its kagune in a fight: out since when, its lash (move, start, aim), its string, its volley, its shots in flight. */
    static final class Fight {
        boolean out;
        long outAt = Long.MIN_VALUE;
        GraftMove move = GraftMove.NONE;
        long moveAt = Long.MIN_VALUE;
        Vec3 point = Vec3.f_82478_;
        double yaw;
        GraftMove last;
        long lastEnds = Long.MIN_VALUE;
        int strung;
        long readyAt = Long.MIN_VALUE;
        final List<Set<Integer>> hits = new ArrayList<>();
        int prey;
        long volleyAt = Long.MIN_VALUE;
        long shotAt = Long.MIN_VALUE;
        int shotSide = 1;
        int shots;
        long nextShot = Long.MIN_VALUE;
        final List<Bolt> bolts = new ArrayList<>();
        boolean dirty = true;
    }

    /** A fire shot in flight. */
    static final class Bolt {
        final int id = nextBolt++;
        Vec3 position;
        Vec3 velocity;
        int life;
        float damage;
        final Set<Integer> hit = new HashSet<>();
    }

    private ChildKagune() {
    }

    /** True if {@code genome} gives it a kagune it fights with here (tails or wings: Eto's Owl kagune is not yet a child's). */
    static boolean has(Genome genome) {
        return genome != null && genome.ghoul() && (KaguneGenes.tails(genome.kagune()) || KaguneGenes.wings(genome.kagune()) > 0);
    }

    /**
     * A tick of its fight against {@code threat} with its kagune (its kagune grows out first). Its lash carried on, or the next thing it
     * does chosen ({@link ChildKaguneRules#use}): a lash, a shot, or closing in.
     */
    static void fight(ChildNpc npc, ChildBrain.Mind mind, Children.Child child, Genome genome, LivingEntity threat, ServerLevel level, long now) {
        Fight f = mind.kagune;
        ChildStage stage = mind.stage == null ? child.stage(now) : mind.stage;
        double scale = ChildKaguneRules.scale(stage);
        int bits = genome.kagune();
        float damage = ChildKaguneRules.damage(stage, child.skill, Children.might(level, child));
        if (!f.out) {
            f.out = true;
            f.outAt = now;
            f.readyAt = Math.max(f.readyAt, now + 10L); // the kagune tears out first
            f.dirty = true;
            sound(level, npc.m_20182_(), HexKaguneSounds.KAGUNE_STRAIN.get(), 0.6F, 1.35F);
        }
        if (f.prey != threat.m_19879_()) {
            f.prey = threat.m_19879_();
            f.dirty = true;
        }
        npc.m_21563_().m_24960_(threat, 30.0F, 30.0F);
        int limbs = GraftStage.TRANSPLANT.limbs(ChildKaguneRules.tailMastery(stage));
        if (f.move != GraftMove.NONE) {
            lashing(npc, f, limbs, scale, damage, threat, level, now);
            return;
        }
        double distance = Math.sqrt(threat.m_20280_(npc));
        boolean sees = npc.m_142582_(threat);
        double reach = GraftCombat.reach(GraftMove.LASH_1, limbs) * scale;
        ChildKaguneRules.Use use = ChildKaguneRules.use(bits, distance, sees, reach);
        if (use != ChildKaguneRules.Use.WINGS && f.volleyAt != Long.MIN_VALUE) {
            f.volleyAt = Long.MIN_VALUE; // the wing comes down
            f.shots = 0;
            f.dirty = true;
        }
        switch (use) {
            case TAILS -> {
                npc.m_21573_().m_26573_();
                if (now < f.readyAt) {
                    return;
                }
                boolean chained = f.strung > 0 && f.last != null && now <= f.lastEnds + GraftRules.CHAIN_WINDOW;
                GraftMove move = chained ? GraftMove.nextStrike(f.last, GraftStage.TRANSPLANT) : GraftMove.firstStrike(GraftStage.TRANSPLANT);
                begin(npc, f, threat, move, level, now);
                f.strung = chained ? f.strung + 1 : 1;
                if (f.strung >= ChildKaguneRules.string(child.skill) || move == GraftMove.LASH_5) {
                    f.strung = 0;
                    f.readyAt = now + move.ticks() + ChildKaguneRules.pause(npc.m_217043_().m_188500_());
                }
            }
            case WINGS -> shoot(npc, f, bits, genome.seed(), stage, child.skill, damage, threat, distance, level, now);
            default -> npc.m_21573_().m_5624_(threat, 1.25);
        }
    }

    /** Every tick, fighting or not: its shots fly on; its kagune drawn back in once the fight is over (or it can't fight). */
    static void tick(ChildNpc npc, ChildBrain.Mind mind, boolean fighting, ServerLevel level, long now) {
        Fight f = mind.kagune;
        for (Iterator<Bolt> it = f.bolts.iterator(); it.hasNext(); ) {
            if (!step(npc, mind, it.next(), level, now)) {
                it.remove();
            }
        }
        if (!fighting && (f.out || f.move != GraftMove.NONE || f.volleyAt != Long.MIN_VALUE)) {
            f.out = false;
            f.outAt = now;
            f.move = GraftMove.NONE;
            f.volleyAt = Long.MIN_VALUE;
            f.strung = 0;
            f.shots = 0;
            f.prey = 0;
            f.dirty = true;
        }
        if (f.dirty) {
            f.dirty = false;
            Children.Child child = Children.get(level, npc.childId());
            if (child != null) {
                ControlNetwork.broadcastAround(npc, message(npc, mind, child));
            }
        }
    }

    /** Its shots put out (it died, or is gone): every client stops drawing them. */
    static void fizzleAll(ChildNpc npc, ChildBrain.Mind mind) {
        for (Bolt bolt : mind.kagune.bolts) {
            fx(npc, S2CUkakuFx.FIZZLE, bolt.id, bolt.position, bolt.velocity, 1.0F, 0);
        }
        mind.kagune.bolts.clear();
    }

    /**
     * What its clients are told of its kagune, as a wild ghoul's ({@link S2CGhoulNpc} under its id): who it is as far as the drawing goes
     * (tails or wings, a seed), out or not and since when, its lash and its aim, its prey, its volley and its last shot.
     */
    static S2CGhoulNpc message(ChildNpc npc, ChildBrain.Mind mind, Children.Child child) {
        Genome genome = child.genome();
        NpcIdentity.Kind kind = KaguneGenes.tails(genome.kagune()) ? NpcIdentity.Kind.RINKAKU : NpcIdentity.Kind.UKAKU;
        long who = new NpcIdentity(kind, NpcTemper.byOrdinal(0), 0, 0, 0, (int)(genome.seed() & 0xFFFF)).pack();
        if (mind == null) {
            return new S2CGhoulNpc(npc.m_19879_(), who, false, Long.MIN_VALUE, GraftMove.NONE.ordinal(), Long.MIN_VALUE, 0.0, 0.0, 0.0, 0, Long.MIN_VALUE,
                Long.MIN_VALUE, 1, -1, Long.MIN_VALUE);
        }
        Fight f = mind.kagune;
        return new S2CGhoulNpc(npc.m_19879_(), who, f.out, f.outAt, f.move.ordinal(), f.moveAt, f.point.f_82479_, f.point.f_82480_, f.point.f_82481_, f.prey,
            f.volleyAt, f.shotAt, f.shotSide, -1, Long.MIN_VALUE);
    }

    // ---------------------------------------------------------------- the tails

    private static void begin(ChildNpc npc, Fight f, LivingEntity prey, GraftMove move, ServerLevel level, long now) {
        f.move = move;
        f.moveAt = now;
        f.point = GhoulNpcBrain.centre(prey);
        Vec3 facing = f.point.m_82546_(npc.m_20182_());
        f.yaw = Math.toDegrees(Math.atan2(-facing.f_82479_, facing.f_82481_));
        f.hits.clear();
        f.dirty = true;
        sound(level, npc.m_20182_(), HexKaguneSounds.KAGUNE_WHIP.get(), 0.5F, 1.15F + 0.1F * (float)npc.m_217043_().m_188500_());
    }

    private static void lashing(ChildNpc npc, Fight f, int limbs, double scale, float damage, LivingEntity threat, ServerLevel level, long now) {
        GraftMove move = f.move;
        long age = now - f.moveAt;
        npc.m_21573_().m_26573_(); // planted while the tails strike
        List<GraftMove.Beat> beats = move.beats(limbs);
        for (int i = 0; i < beats.size(); i++) {
            beat(npc, f, beats.get(i), i, age, scale, damage, threat, level);
        }
        if (age >= move.ticks()) {
            f.last = move;
            f.lastEnds = now;
            f.move = GraftMove.NONE;
            f.dirty = true;
        }
    }

    /** One beat at {@code age} ticks into its lash, at its size: it lands on the tick its clients show it landing. */
    private static void beat(ChildNpc npc, Fight f, GraftMove.Beat beat, int index, long age, double scale, float damage, LivingEntity threat,
                             ServerLevel level) {
        boolean sweeping = beat.kind() == GraftMove.Kind.SWEEP;
        if (sweeping ? age < beat.land() || age > beat.end() : age != beat.land()) {
            return;
        }
        double reach = beat.reach() * scale;
        Vec3 chest = npc.m_20182_().m_82520_(0.0, 1.35 * scale, 0.0);
        Vec3 toward = f.point.m_82546_(chest);
        Vec3 dir = toward.m_82556_() < 1.0E-6 ? flat(npc.m_20154_()) : toward.m_82541_();
        while (f.hits.size() <= index) {
            f.hits.add(new HashSet<>());
        }
        Set<Integer> hit = f.hits.get(index);
        switch (beat.kind()) {
            case SWEEP -> {
                double span = Math.max(1, beat.end() - beat.land());
                double before = age == beat.land() ? beat.from()
                    : beat.from() + (beat.to() - beat.from()) * GraftCombat.smooth((age - 1 - beat.land()) / span);
                double after = beat.from() + (beat.to() - beat.from()) * GraftCombat.smooth((age - beat.land()) / span);
                double low = Math.min(before, after) - 12.0;
                double high = Math.max(before, after) + 12.0;
                double sign = Math.signum(beat.to() - beat.from());
                for (LivingEntity body : around(npc, threat, reach + 1.0)) {
                    Vec3 to = flat(GhoulNpcBrain.centre(body).m_82546_(npc.m_20182_()));
                    double bearing = bearing(npc, f, body);
                    double dy = body.m_20186_() + body.m_20206_() * 0.5 - (npc.m_20186_() + beat.height() * scale);
                    if (bearing < low || bearing > high || Math.abs(dy) > 1.8 + body.m_20206_() * 0.5 || !hit.add(body.m_19879_())) {
                        continue;
                    }
                    Vec3 across = new Vec3(-to.f_82481_ * sign, 0.0, to.f_82479_ * sign);
                    strike(npc, body, beat, across.m_82549_(to.m_82490_(0.3)), damage, WoundBook.Blow.SLASH, S2CPaleFx.IMPACT);
                }
            }
            case THRUST, UPPER -> {
                for (LivingEntity body : around(npc, threat, reach + 1.0)) {
                    Vec3 centre = GhoulNpcBrain.centre(body);
                    double along = centre.m_82546_(chest).m_82526_(dir);
                    Vec3 closest = chest.m_82549_(dir.m_82490_(Math.max(0.0, Math.min(reach, along))));
                    if (along < 0.3 || closest.m_82554_(centre) > body.m_20205_() * 0.5 + 0.7 || !hit.add(body.m_19879_())) {
                        continue;
                    }
                    boolean upper = beat.kind() == GraftMove.Kind.UPPER;
                    strike(npc, body, beat, upper ? new Vec3(dir.f_82479_ * 0.25, 0.0, dir.f_82481_ * 0.25) : dir, damage, WoundBook.Blow.STAB,
                        S2CPaleFx.IMPALED);
                    if (upper) {
                        body.m_20256_(body.m_20184_().m_82520_(0.0, 0.35, 0.0));
                        body.f_19864_ = true;
                    }
                }
            }
            case SLAM -> {
                Vec3 at = toward.m_82553_() > reach ? chest.m_82549_(dir.m_82490_(reach)) : f.point;
                BlockHitResult ground = level.m_45547_(new ClipContext(at.m_82520_(0.0, 1.5, 0.0), at.m_82520_(0.0, -4.0, 0.0), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, npc));
                Vec3 down = ground.m_6662_() == HitResult.Type.BLOCK ? ground.m_82450_() : new Vec3(at.f_82479_, npc.m_20186_(), at.f_82481_);
                for (LivingEntity body : around(npc, threat, reach + 2.5)) {
                    Vec3 centre = GhoulNpcBrain.centre(body);
                    double distance = Math.hypot(centre.f_82479_ - down.f_82479_, centre.f_82481_ - down.f_82481_) - body.m_20205_() * 0.5;
                    if (distance > 2.2 || Math.abs(body.m_20186_() - down.f_82480_) > 2.5 || !hit.add(body.m_19879_())) {
                        continue;
                    }
                    strike(npc, body, beat, flat(centre.m_82546_(down)).m_82490_(0.6), damage * (distance <= 1.4 ? 1.0F : 0.5F), WoundBook.Blow.SLAM,
                        S2CPaleFx.CRUSHED);
                }
                sound(level, down, HexKaguneSounds.KAGUNE_SLAM.get(), 0.6F, 1.1F);
            }
            default -> {
            }
        }
    }

    /** One blow of a beat on {@code body}: hurt (a mob's attack), wounded where it was struck, thrown along {@code push}. */
    private static void strike(ChildNpc npc, LivingEntity body, GraftMove.Beat beat, Vec3 push, float damage, WoundBook.Blow blow, int fx) {
        Vec3 centre = GhoulNpcBrain.centre(body);
        Vec3 into = push.m_82556_() < 1.0E-6 ? flat(centre.m_82546_(npc.m_20182_())) : push.m_82541_();
        WoundHits.expect(body, blow, centre.m_82546_(into.m_82490_(0.3)), into, (npc.m_217043_().m_188500_() - 0.5) * 1.4);
        body.f_19802_ = 0; // every beat its own blow
        if (!body.m_6469_(npc.m_269291_().m_269333_(npc), damage * beat.power())) {
            return;
        }
        double heavy = beat.has(GraftMove.HEAVY) ? 1.3 : 1.0;
        Vec3 shove = new Vec3(push.f_82479_, 0.0, push.f_82481_).m_82490_(0.35 * heavy);
        double up = beat.has(GraftMove.KNOCK_UP) ? 0.4 * heavy : 0.12;
        body.m_20256_(body.m_20184_().m_82490_(0.3).m_82549_(shove).m_82520_(0.0, up, 0.0));
        body.f_19864_ = true;
        PaleBlitz.fxAround(body, npc.m_19879_(), fx, centre, into, fx == S2CPaleFx.IMPALED ? body.m_20206_() : 0.8F, fx == S2CPaleFx.IMPACT ? S2CPaleFx.BONE : 0);
        sound((ServerLevel)npc.m_9236_(), centre, HexKaguneSounds.FLESH_IMPACT.get(), 0.6F, 1.0F + 0.2F * (float)npc.m_217043_().m_188500_());
    }

    /** The bearing of {@code body} from it, degrees from the way the lash faces (plus to its right). */
    private static double bearing(ChildNpc npc, Fight f, LivingEntity body) {
        Vec3 to = GhoulNpcBrain.centre(body).m_82546_(npc.m_20182_());
        double rel = Math.toDegrees(Math.atan2(-to.f_82479_, to.f_82481_)) - f.yaw;
        return ((rel % 360.0) + 540.0) % 360.0 - 180.0;
    }

    /** What its blows may land on within {@code range}: what it fights, and monsters (never a player, a companion, a child), in sight. */
    private static List<LivingEntity> around(ChildNpc npc, LivingEntity threat, double range) {
        return npc.m_9236_().m_6443_(LivingEntity.class, npc.m_20191_().m_82400_(range),
            living -> living != npc && living.m_6084_() && living.m_20280_(npc) <= range * range && fair(living, threat) && npc.m_142582_(living));
    }

    /** True if its kagune may hurt {@code living}: what it fights, or a monster; never a player, a companion, a child. */
    static boolean fair(LivingEntity living, LivingEntity threat) {
        if (living instanceof net.minecraft.world.entity.player.Player || living instanceof com.hex.kagune.control.npc.EtoNpc || living instanceof ChildNpc) {
            return false;
        }
        return living == threat || EtoBrain.hostile(living);
    }

    // ---------------------------------------------------------------- the wing(s)

    private static void shoot(ChildNpc npc, Fight f, int bits, long seed, ChildStage stage, int skill, float damage, LivingEntity prey, double distance,
                              ServerLevel level, long now) {
        // Its distance: back off when its prey comes in, closer when it slips away, on its feet while it shoots.
        if (distance > ChildKaguneRules.SHOOT_FARTHEST) {
            npc.m_21573_().m_5624_(prey, 1.15);
        } else if (distance < ChildKaguneRules.SHOOT_NEAREST && !KaguneGenes.tails(bits)) {
            Vec3 away = flat(npc.m_20182_().m_82546_(prey.m_20182_()));
            Vec3 to = npc.m_20182_().m_82549_(away.m_82490_(4.0));
            npc.m_21573_().m_26519_(to.f_82479_, npc.m_20186_(), to.f_82481_, 1.25);
        } else {
            npc.m_21573_().m_26573_();
        }
        if (now < f.readyAt) {
            return;
        }
        if (f.volleyAt == Long.MIN_VALUE) {
            // The wing curls forward to aim, and the first shot leaves a moment after.
            f.volleyAt = now;
            f.nextShot = now + 6L;
            f.shots = 0;
            f.dirty = true;
            return;
        }
        if (now < f.nextShot) {
            return;
        }
        int side = ChildKaguneRules.side(bits, seed, f.shotSide);
        fire(npc, stage, skill, prey, level, side, damage, f);
        f.shotAt = now;
        f.shotSide = side;
        f.shots++;
        f.nextShot = now + ChildKaguneRules.SHOT_EVERY;
        f.dirty = true;
        if (f.shots >= ChildKaguneRules.string(skill)) {
            f.shots = 0;
            f.volleyAt = Long.MIN_VALUE;
            f.readyAt = now + ChildKaguneRules.pause(npc.m_217043_().m_188500_());
        }
    }

    /** A shot from the tip of the wing on {@code side} at {@code prey}'s middle. */
    private static void fire(ChildNpc npc, ChildStage stage, int skill, LivingEntity prey, ServerLevel level, int side, float damage, Fight f) {
        Vec3 aim = GhoulNpcBrain.centre(prey);
        Vec3 from = tip(npc, stage, skill, side, aim);
        Vec3 way = aim.m_82546_(from);
        way = way.m_82556_() < 1.0E-6 ? npc.m_20154_() : way.m_82541_();
        Bolt bolt = new Bolt();
        bolt.position = from;
        bolt.velocity = way.m_82490_(ChildKaguneRules.SHOT_SPEED);
        bolt.life = (int)Math.ceil(ChildKaguneRules.SHOT_RANGE / ChildKaguneRules.SHOT_SPEED);
        bolt.damage = damage;
        f.bolts.add(bolt);
        fx(npc, S2CUkakuFx.BOLT, bolt.id, from, bolt.velocity, 0.7F, bolt.life);
        UkakuFire.sound(level, from, "entity.blaze.shoot", 0.4F, 1.5F + 0.1F * (float)npc.m_217043_().m_188500_());
    }

    /** Where its wing's shot leaves: the wing's tip as it curls to aim ({@link WingRig#aimTip}), at its size. */
    static Vec3 tip(ChildNpc npc, ChildStage stage, int skill, int side, Vec3 aim) {
        double scale = ChildKaguneRules.scale(stage);
        double yaw = Math.toRadians(npc.f_20883_);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        Vec3 toward = aim.m_82546_(npc.m_20182_().m_82520_(0.0, WingRig.ROOT_Y * scale, 0.0));
        double[] rig = {toward.f_82479_ * rightX + toward.f_82481_ * rightZ, toward.f_82480_, toward.f_82479_ * forwardX + toward.f_82481_ * forwardZ};
        double length = Math.sqrt(rig[0] * rig[0] + rig[1] * rig[1] + rig[2] * rig[2]);
        double[] dir = length < 1.0E-6 ? WingRig.FORWARD : new double[] {rig[0] / length, rig[1] / length, rig[2] / length};
        double[] local = WingRig.aimTip(ChildKaguneRules.wingMastery(stage), WingForm.TWIN, side, dir, false);
        return new Vec3(npc.m_20185_() + (rightX * local[0] + forwardX * local[2]) * scale, npc.m_20186_() + local[1] * scale,
            npc.m_20189_() + (rightZ * local[0] + forwardZ * local[2]) * scale);
    }

    private static boolean step(ChildNpc npc, ChildBrain.Mind mind, Bolt bolt, ServerLevel level, long now) {
        Vec3 from = bolt.position;
        Vec3 to = from.m_82549_(bolt.velocity);
        BlockHitResult block = level.m_45547_(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc));
        boolean wall = block.m_6662_() == HitResult.Type.BLOCK;
        Vec3 end = wall ? block.m_82450_() : to;
        LivingEntity threat = mind.target != 0 && level.m_6815_(mind.target) instanceof LivingEntity living ? living : null;
        AABB search = new AABB(from.f_82479_, from.f_82480_, from.f_82481_, end.f_82479_, end.f_82480_, end.f_82481_).m_82400_(1.4);
        List<LivingEntity> near = level.m_6443_(LivingEntity.class, search, living -> living != npc && living.m_6084_() && !bolt.hit.contains(living.m_19879_())
            && fair(living, threat));
        int samples = Math.max(1, (int)Math.ceil(from.m_82554_(end) / 0.25));
        for (int sample = 0; sample <= samples; sample++) {
            Vec3 point = from.m_82549_(end.m_82546_(from).m_82490_(sample / (double)samples));
            for (LivingEntity body : near) {
                if (!body.m_20191_().m_82400_(0.3).m_82390_(point)) {
                    continue;
                }
                bolt.hit.add(body.m_19879_());
                burn(npc, body, bolt);
                fx(npc, S2CUkakuFx.IMPACT, bolt.id, point, bolt.velocity, 0.7F, 0);
                return false;
            }
        }
        if (wall) {
            fx(npc, S2CUkakuFx.IMPACT, bolt.id, end, bolt.velocity, 0.0F, 0);
            return false;
        }
        bolt.position = to;
        if (--bolt.life <= 0) {
            fx(npc, S2CUkakuFx.FIZZLE, bolt.id, to, bolt.velocity, 0.7F, 0);
            return false;
        }
        return true;
    }

    /** A shot's hit: hurt (a mob's attack), set alight a moment, pushed a little the way it flew. */
    private static void burn(ChildNpc npc, LivingEntity body, Bolt bolt) {
        Vec3 centre = GhoulNpcBrain.centre(body);
        Vec3 into = bolt.velocity.m_82541_();
        WoundHits.expect(body, WoundBook.Blow.BURN, centre.m_82546_(into.m_82490_(0.3)), into, 0.0);
        body.f_19802_ = 0;
        if (!body.m_6469_(npc.m_269291_().m_269333_(npc), bolt.damage)) {
            return;
        }
        body.m_20254_(1);
        if (bolt.velocity.m_165925_() > 1.0E-6) {
            body.m_147240_(0.15, -bolt.velocity.f_82479_, -bolt.velocity.f_82481_);
        }
        UkakuFire.sound((ServerLevel)npc.m_9236_(), centre, "entity.generic.burn", 0.4F, 1.3F);
    }

    private static void fx(ChildNpc npc, int kind, int id, Vec3 at, Vec3 velocity, float size, int extra) {
        ControlNetwork.broadcastAround(npc, new S2CUkakuFx(kind, npc.m_19879_(), id, at.f_82479_, at.f_82480_, at.f_82481_, (float)velocity.f_82479_,
            (float)velocity.f_82480_, (float)velocity.f_82481_, size, extra));
    }

    private static Vec3 flat(Vec3 direction) {
        Vec3 flat = new Vec3(direction.f_82479_, 0.0, direction.f_82481_);
        return flat.m_82556_() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.m_82541_();
    }

    private static void sound(ServerLevel level, Vec3 at, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level.m_6263_(null, at.f_82479_, at.f_82480_, at.f_82481_, sound, SoundSource.HOSTILE, volume, pitch);
    }
}
