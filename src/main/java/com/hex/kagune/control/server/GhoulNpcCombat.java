package com.hex.kagune.control.server;

import com.hex.kagune.control.ukaku.WingForm;
import com.hex.kagune.control.ukaku.WingRig;
import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.graft.GraftRules;
import com.hex.kagune.control.graft.GraftStage;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CUkakuFx;
import com.hex.kagune.control.network.S2CPaleFx;
import com.hex.kagune.control.npc.GhoulNpc;
import com.hex.kagune.control.npc.NpcIdentity;
import com.hex.kagune.control.npc.NpcRules;
import com.hex.kagune.control.wound.WoundBook;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 7.0.2: how a ghoul NPC fights, on the server: only its kagune's M1, reusing what the players' kagunes already have.
 * <ul>
 *   <li>Rinkaku: the transplant's string ({@link GraftMove#LASH_1} on: the rising lash, the backhand, the twin skewer, the gutting
 *       upper, the guillotine), each move's {@link GraftMove.Beat}s on the very ticks every client draws them, a few lashes a string
 *       (more the stronger it is), then it circles and sizes its prey up.</li>
 *   <li>Ukaku: the twin wings' fire shots, as Ukaku's M1: from the tip of the wing it aims, flown here a tick at a time, hitting the
 *       first body in their way and setting it alight; every client draws them as his ({@code S2CUkakuFx}). It keeps its distance
 *       and shoots in volleys, the wings taking turns.</li>
 * </ul>
 * Every blow is a mob's attack (scaled by the difficulty as every mob's is); none breaks blocks. 1.0.41: they move like ghouls (front
 * flips in, back flips clear, a blow coming at them dodged or met with their own: KaguneClash), the rinkaku from the full length of
 * their tails (the lancers) and with Owl's Execution for their heavy, the ukaku from far off (the snipers) and with a feather storm.
 */
final class GhoulNpcCombat {
    /** Shot ids well clear of the players' ({@code UkakuFire} counts up from 1). */
    private static int nextBolt = 1 << 30;

    /** A fire shot in flight. */
    static final class Bolt {
        final int id = nextBolt++;
        Vec3 position;
        Vec3 velocity;
        int life;
        float damage;
        final Set<Integer> hit = new HashSet<>();
    }

    private GhoulNpcCombat() {
    }

    // ---------------------------------------------------------------- rinkaku: the transplant's string

    static void lash(GhoulNpc npc, NpcIdentity who, GhoulNpcBrain.Mind mind, LivingEntity prey, ServerLevel level, long now) {
        int limbs = limbs(who);
        if (mind.move != GraftMove.NONE) {
            GraftMove move = mind.move;
            long age = now - mind.moveAt;
            npc.m_21573_().m_26573_(); // planted while the tails strike
            List<GraftMove.Beat> beats = move.beats(limbs);
            for (int i = 0; i < beats.size(); i++) {
                beat(npc, who, mind, beats.get(i), i, age, level, now);
                if (mind.move != move) {
                    return; // 1.0.41: it met a blow coming back (KaguneClash): cut off
                }
            }
            if (age < move.ticks()) {
                return;
            }
            mind.last = move;
            mind.lastEnds = now;
            mind.move = GraftMove.NONE;
            mind.dirty = true;
        }
        double reach = GraftCombat.reach(GraftMove.LASH_1, limbs);
        // 1.0.41: the skewer's reach: its tails' full length.
        double full = GraftCombat.reach(GraftMove.LASH_3, limbs);
        double distance = Math.sqrt(prey.m_20280_(npc));
        boolean sight = npc.m_142582_(prey);
        if (now < mind.readyAt) {
            circle(npc, mind, prey, preferred(who) + 1.0, now);
            return;
        }
        boolean chained = mind.strung > 0 && mind.last != null && now <= mind.lastEnds + GraftRules.CHAIN_WINDOW;
        // 1.0.41: its heavy, its right click: Owl's Execution, a flurry of lashes and the skewer, on prey close enough.
        if (!chained && sight && distance <= NpcRules.HEAVY_REACH && now >= mind.heavyReadyAt && npc.m_217043_().m_188500_() < 0.5) {
            begin(npc, who, mind, prey, GraftMove.OWLS_EXECUTION, level, now);
            mind.strung = 0;
            mind.heavyReadyAt = now + NpcRules.heavyEvery(who.tier());
            mind.readyAt = now + GraftMove.OWLS_EXECUTION.ticks() + pause(npc);
            sound(level, npc.m_20182_(), HexKaguneSounds.KAGUNE_STRAIN.get(), 0.9F, 0.75F);
            return;
        }
        // 1.0.41: a lancer opens from the full length of its tails, with the skewer.
        if (!chained && sight && NpcRules.lancer(who.seed()) && distance > reach - 0.4 && distance <= full - 0.2) {
            string(npc, who, mind, prey, GraftMove.LASH_3, false, level, now);
            return;
        }
        if (distance > reach - 0.4 || !sight) {
            npc.m_21573_().m_5624_(prey, 1.15);
            return;
        }
        GraftMove move = chained ? GraftMove.nextStrike(mind.last, GraftStage.TRANSPLANT) : GraftMove.firstStrike(GraftStage.TRANSPLANT);
        string(npc, who, mind, prey, move, chained, level, now);
    }

    /** One lash of its string, begun now ({@code chained}: out of the last one's hold). */
    private static void string(GhoulNpc npc, NpcIdentity who, GhoulNpcBrain.Mind mind, LivingEntity prey, GraftMove move, boolean chained, ServerLevel level,
                               long now) {
        begin(npc, who, mind, prey, move, level, now);
        mind.strung = chained ? mind.strung + 1 : 1;
        if (mind.strung >= NpcRules.string(who.tier()) || move == GraftMove.LASH_5) {
            mind.strung = 0;
            mind.readyAt = now + move.ticks() + pause(npc);
        }
    }

    /**
     * {@code move} begun now at {@code prey}: aimed at its middle, every client told; 1.0.41: and on its way (KaguneClash), so a blow
     * of its prey's landing first meets it, and it meets a blow of its prey's still coming.
     */
    private static void begin(GhoulNpc npc, NpcIdentity who, GhoulNpcBrain.Mind mind, LivingEntity prey, GraftMove move, ServerLevel level, long now) {
        mind.move = move;
        mind.moveAt = now;
        mind.point = GhoulNpcBrain.centre(prey);
        Vec3 facing = mind.point.m_82546_(npc.m_20182_());
        mind.yaw = Math.toDegrees(Math.atan2(-facing.f_82479_, facing.f_82481_));
        mind.hits.clear();
        mind.dirty = true;
        sound(level, npc.m_20182_(), HexKaguneSounds.KAGUNE_WHIP.get(), 0.6F, 0.95F + 0.1F * (float)npc.m_217043_().m_188500_());
        int limbs = limbs(who);
        int lands = move.ticks();
        for (GraftMove.Beat beat : move.beats(limbs)) {
            lands = Math.min(lands, beat.land());
        }
        long began = now;
        KaguneClash.swing(npc, now, lands + 1, GraftCombat.reach(move, limbs), flat(facing), prey.m_19879_(),
            () -> mind.move == move && mind.moveAt == began, () -> cut(npc, mind));
    }

    /** 1.0.41: its lash met a blow coming back (KaguneClash): cut off, the string with it, a moment before the next. */
    private static void cut(GhoulNpc npc, GhoulNpcBrain.Mind mind) {
        long now = npc.m_9236_().m_46467_();
        mind.move = GraftMove.NONE;
        mind.last = null;
        mind.lastEnds = now;
        mind.strung = 0;
        mind.readyAt = now + com.hex.kagune.control.ClashRules.STAGGER + 6L;
        mind.dirty = true;
    }

    /** 1.0.41: how many tails it has out. */
    private static int limbs(NpcIdentity who) {
        return GraftStage.TRANSPLANT.limbs(who.lookMastery());
    }

    /** 1.0.41: how far off it likes its prey: a lancer at the full length of its tails, the others at a lash's reach. */
    static double preferred(NpcIdentity who) {
        int limbs = limbs(who);
        return NpcRules.lancer(who.seed()) ? GraftCombat.reach(GraftMove.LASH_3, limbs) - 0.6 : GraftCombat.reach(GraftMove.LASH_1, limbs) - 0.8;
    }

    /** One beat at {@code age} ticks into its move: it lands on the tick every client shows it landing. */
    private static void beat(GhoulNpc npc, NpcIdentity who, GhoulNpcBrain.Mind mind, GraftMove.Beat beat, int index, long age, ServerLevel level,
                             long now) {
        boolean sweeping = beat.kind() == GraftMove.Kind.SWEEP;
        if (sweeping ? age < beat.land() || age > beat.end() : age != beat.land()) {
            return;
        }
        Vec3 chest = npc.m_20182_().m_82520_(0.0, 1.35, 0.0);
        Vec3 toward = mind.point.m_82546_(chest);
        Vec3 dir = toward.m_82556_() < 1.0E-6 ? flat(npc.m_20154_()) : toward.m_82541_();
        Vec3 at = toward.m_82553_() > beat.reach() ? chest.m_82549_(dir.m_82490_(beat.reach())) : mind.point;
        Set<Integer> hit = GhoulNpcBrain.hitSet(mind, index);
        switch (beat.kind()) {
            case SWEEP -> {
                // The tail swept round at its reach from one side of its aim to the other, as every client draws it (and as a
                // player's, GraftCombat.sweep): what it passes through this tick is struck and thrown on round.
                double span = Math.max(1, beat.end() - beat.land());
                double before = age == beat.land() ? beat.from()
                    : beat.from() + (beat.to() - beat.from()) * GraftCombat.smooth((age - 1 - beat.land()) / span);
                double after = beat.from() + (beat.to() - beat.from()) * GraftCombat.smooth((age - beat.land()) / span);
                double low = Math.min(before, after) - 12.0;
                double high = Math.max(before, after) + 12.0;
                double sign = Math.signum(beat.to() - beat.from());
                for (LivingEntity body : around(npc, who, mind, beat.reach() + 1.0, now)) {
                    Vec3 to = flat(GhoulNpcBrain.centre(body).m_82546_(npc.m_20182_()));
                    double bearing = bearing(npc, mind, body);
                    double dy = body.m_20186_() + body.m_20206_() * 0.5 - (npc.m_20186_() + beat.height());
                    if (bearing < low || bearing > high || Math.abs(dy) > 2.2 + body.m_20206_() * 0.5 || hit.contains(body.m_19879_())) {
                        continue;
                    }
                    hit.add(body.m_19879_());
                    Vec3 across = new Vec3(-to.f_82481_ * sign, 0.0, to.f_82479_ * sign);
                    if (strike(npc, who, body, beat, across.m_82549_(to.m_82490_(0.3)), now, WoundBook.Blow.SLASH, S2CPaleFx.IMPACT)) {
                        return;
                    }
                }
            }
            case THRUST, UPPER -> {
                // Run through along the line it shot out on (ripped up from under it, for the upper).
                for (LivingEntity body : around(npc, who, mind, beat.reach() + 1.0, now)) {
                    Vec3 centre = GhoulNpcBrain.centre(body);
                    double along = centre.m_82546_(chest).m_82526_(dir);
                    Vec3 closest = chest.m_82549_(dir.m_82490_(Math.max(0.0, Math.min(beat.reach(), along))));
                    if (along < 0.5 || closest.m_82554_(centre) > body.m_20205_() * 0.5 + 0.8 || !hit.add(body.m_19879_())) {
                        continue;
                    }
                    boolean upper = beat.kind() == GraftMove.Kind.UPPER;
                    if (strike(npc, who, body, beat, upper ? new Vec3(dir.f_82479_ * 0.25, 0.0, dir.f_82481_ * 0.25) : dir, now, WoundBook.Blow.STAB,
                        S2CPaleFx.IMPALED)) {
                        return;
                    }
                    if (upper) {
                        body.m_20256_(body.m_20184_().m_82520_(0.0, 0.45, 0.0));
                        body.f_19864_ = true;
                    }
                }
            }
            case SLAM -> {
                // Brought down on it: everything round where the tails hit the ground is struck, the nearest hardest.
                BlockHitResult ground = level.m_45547_(new ClipContext(at.m_82520_(0.0, 1.5, 0.0), at.m_82520_(0.0, -4.0, 0.0), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, npc));
                Vec3 down = ground.m_6662_() == HitResult.Type.BLOCK ? ground.m_82450_() : new Vec3(at.f_82479_, npc.m_20186_(), at.f_82481_);
                for (LivingEntity body : around(npc, who, mind, beat.reach() + 3.5, now)) {
                    Vec3 centre = GhoulNpcBrain.centre(body);
                    double distance = Math.hypot(centre.f_82479_ - down.f_82479_, centre.f_82481_ - down.f_82481_) - body.m_20205_() * 0.5;
                    if (distance > 3.1 || Math.abs(body.m_20186_() - down.f_82480_) > 3.0 || !hit.add(body.m_19879_())) {
                        continue;
                    }
                    float share = distance <= 2.1 ? 1.0F : 0.5F;
                    if (strike(npc, who, body, new GraftMove.Beat(beat.limb(), beat.kind(), beat.start(), beat.land(), beat.end(), beat.reach(), 0.0, 0.0, 0.0,
                        beat.power() * share, beat.flags()), flat(centre.m_82546_(down)).m_82490_(0.7), now, WoundBook.Blow.SLAM, S2CPaleFx.CRUSHED)) {
                        return;
                    }
                }
                BlockPos below = BlockPos.m_274561_(down.f_82479_, down.f_82480_ - 0.01, down.f_82481_);
                PaleBlitz.fxAround(npc, npc.m_19879_(), S2CPaleFx.SHOCKWAVE, down, flat(dir), 3.0F, Block.m_49956_(level.m_8055_(below)));
                sound(level, down, HexKaguneSounds.KAGUNE_SLAM.get(), 0.9F, 0.8F);
            }
            default -> {
            }
        }
    }

    /**
     * One blow of a beat on {@code body}: hurt (a mob's attack), wounded where it was struck, thrown along {@code push}. 1.0.41: unless
     * it meets a blow of {@code body}'s coming back (KaguneClash): then neither lands, and true.
     */
    private static boolean strike(GhoulNpc npc, NpcIdentity who, LivingEntity body, GraftMove.Beat beat, Vec3 push, long now, WoundBook.Blow blow,
                                  int fx) {
        if (KaguneClash.meets(npc, body, now)) {
            return true;
        }
        Vec3 centre = GhoulNpcBrain.centre(body);
        Vec3 into = push.m_82556_() < 1.0E-6 ? flat(centre.m_82546_(npc.m_20182_())) : push.m_82541_();
        WoundHits.expect(body, blow, centre.m_82546_(into.m_82490_(0.3)), into, (npc.m_217043_().m_188500_() - 0.5) * 1.4);
        body.f_19802_ = 0; // every beat its own blow, as on every path
        if (!body.m_6469_(npc.m_269291_().m_269333_(npc), NpcRules.damage(who.tier()) * beat.power())) {
            return false;
        }
        KaguneClash.landed(npc);
        double heavy = beat.has(GraftMove.HEAVY) ? 1.4 : 1.0;
        Vec3 shove = new Vec3(push.f_82479_, 0.0, push.f_82481_).m_82490_(0.45 * heavy);
        double up = beat.has(GraftMove.KNOCK_UP) ? 0.5 * heavy : 0.15;
        body.m_20256_(body.m_20184_().m_82490_(0.3).m_82549_(shove).m_82520_(0.0, up, 0.0));
        body.f_19864_ = true;
        PaleBlitz.fxAround(body, npc.m_19879_(), fx, centre, into, fx == S2CPaleFx.IMPALED ? body.m_20206_() : 1.0F, fx == S2CPaleFx.IMPACT ? S2CPaleFx.BONE : 0);
        sound((ServerLevel)npc.m_9236_(), centre, HexKaguneSounds.FLESH_IMPACT.get(), 0.8F, 0.9F + 0.2F * (float)npc.m_217043_().m_188500_());
        return false;
    }

    /** The bearing of {@code body} from it, degrees from the way the lash faces (plus to its right), as {@code GraftCombat.bearing}. */
    private static double bearing(GhoulNpc npc, GhoulNpcBrain.Mind mind, LivingEntity body) {
        Vec3 to = GhoulNpcBrain.centre(body).m_82546_(npc.m_20182_());
        double rel = Math.toDegrees(Math.atan2(-to.f_82479_, to.f_82481_)) - mind.yaw;
        return ((rel % 360.0) + 540.0) % 360.0 - 180.0;
    }

    /** Every body its blows may land on within {@code range} of it. */
    private static List<LivingEntity> around(GhoulNpc npc, NpcIdentity who, GhoulNpcBrain.Mind mind, double range, long now) {
        return npc.m_9236_().m_6443_(LivingEntity.class, npc.m_20191_().m_82400_(range),
            living -> living.m_20280_(npc) <= range * range && GhoulNpcBrain.fair(npc, who, mind, living, now) && npc.m_142582_(living));
    }

    // ---------------------------------------------------------------- ukaku: the wings' fire

    static void shoot(GhoulNpc npc, NpcIdentity who, GhoulNpcBrain.Mind mind, LivingEntity prey, ServerLevel level, long now) {
        double distance = Math.sqrt(prey.m_20280_(npc));
        boolean sight = npc.m_142582_(prey);
        // 1.0.41: a sniper keeps far off and shoots far.
        boolean sniper = NpcRules.sniper(who.seed());
        double nearest = sniper ? NpcRules.SNIPE_NEAREST : NpcRules.SHOOT_NEAREST;
        double farthest = sniper ? NpcRules.SNIPE_FARTHEST : NpcRules.SHOOT_FARTHEST;
        double range = sniper ? NpcRules.SNIPE_RANGE : NpcRules.SHOT_RANGE;
        // Its distance: closer when it can't see or reach, back off when its prey comes in, sideways while it shoots.
        if (!sight || distance > farthest) {
            npc.m_21573_().m_5624_(prey, 1.1);
        } else if (distance < nearest) {
            Vec3 away = flat(npc.m_20182_().m_82546_(prey.m_20182_()));
            Vec3 to = npc.m_20182_().m_82549_(away.m_82490_(5.0));
            npc.m_21573_().m_26519_(to.f_82479_, npc.m_20186_(), to.f_82481_, 1.25);
        } else {
            circle(npc, mind, prey, Math.max(nearest + 1.0, Math.min(farthest - 2.0, distance)), now);
        }
        // 1.0.41: its heavy, its right click: the feather storm, a fan of fire from both wings at once.
        if (sight && mind.volleyAt == Long.MIN_VALUE && now >= mind.readyAt && now >= mind.heavyReadyAt && distance >= 4.0 && distance <= 16.0
            && npc.m_217043_().m_188500_() < 0.35) {
            storm(npc, who, mind, prey, level, now);
            return;
        }
        if (now < mind.readyAt || !sight || distance > range) {
            if (mind.volleyAt != Long.MIN_VALUE && (now >= mind.readyAt || !sight)) {
                mind.volleyAt = Long.MIN_VALUE; // lost it: the wings come down
                mind.shots = 0;
                mind.dirty = true;
            }
            return;
        }
        if (mind.volleyAt == Long.MIN_VALUE) {
            // The wings curl forward to aim, and the first shot leaves a moment after.
            mind.volleyAt = now;
            mind.nextShot = now + 5L;
            mind.shots = 0;
            mind.dirty = true;
            return;
        }
        if (now < mind.nextShot) {
            return;
        }
        fire(npc, who, mind, prey, level, mind.side, 0.0, sniper ? NpcRules.SNIPE_SPEED : NpcRules.SHOT_SPEED, range,
            sniper ? NpcRules.SNIPE_DAMAGE : 1.0F);
        mind.shotAt = now;
        mind.shotSide = mind.side;
        mind.side = -mind.side;
        mind.shots++;
        mind.nextShot = now + (sniper ? NpcRules.SNIPE_EVERY : NpcRules.SHOT_EVERY);
        mind.dirty = true;
        if (mind.shots >= (sniper ? Math.max(2, NpcRules.string(who.tier()) - 1) : NpcRules.string(who.tier()))) {
            mind.shots = 0;
            mind.volleyAt = Long.MIN_VALUE;
            mind.readyAt = now + 6L + pause(npc);
        }
    }

    /** 1.0.41: the feather storm: the wings flare and loose a fan of shots at once, from both of them, at {@code prey}. */
    private static void storm(GhoulNpc npc, NpcIdentity who, GhoulNpcBrain.Mind mind, LivingEntity prey, ServerLevel level, long now) {
        for (int i = 0; i < NpcRules.STORM_SHOTS; i++) {
            double spread = NpcRules.STORM_SPREAD * (2.0 * i / (NpcRules.STORM_SHOTS - 1) - 1.0);
            fire(npc, who, mind, prey, level, i % 2 == 0 ? 1 : -1, spread, NpcRules.SHOT_SPEED * 1.1, NpcRules.SHOT_RANGE, 0.8F);
        }
        mind.volleyAt = now;
        mind.shotAt = now;
        mind.shotSide = mind.side;
        mind.shots = 0;
        mind.heavyReadyAt = now + NpcRules.heavyEvery(who.tier());
        mind.readyAt = now + 24L + pause(npc);
        mind.dirty = true;
        UkakuFire.sound(level, npc.m_20182_().m_82520_(0.0, 1.4, 0.0), "entity.ender_dragon.flap", 0.9F, 1.6F);
        UkakuFire.sound(level, npc.m_20182_().m_82520_(0.0, 1.4, 0.0), "entity.blaze.shoot", 1.0F, 0.8F);
    }

    /**
     * A shot from the tip of the wing on {@code side} (1 right, -1 left) at {@code prey}'s middle; 1.0.41: turned {@code spread}
     * degrees off it (a fan's), flying {@code speed} blocks a tick for {@code range} blocks, hurting {@code harder} times a shot.
     */
    private static void fire(GhoulNpc npc, NpcIdentity who, GhoulNpcBrain.Mind mind, LivingEntity prey, ServerLevel level, int side, double spread,
                             double speed, double range, float harder) {
        Vec3 aim = GhoulNpcBrain.centre(prey);
        Vec3 from = tip(npc, who, side, aim);
        Vec3 way = aim.m_82546_(from);
        way = way.m_82556_() < 1.0E-6 ? npc.m_20154_() : way.m_82541_();
        if (spread != 0.0) {
            double turn = Math.toRadians(spread);
            way = new Vec3(way.f_82479_ * Math.cos(turn) - way.f_82481_ * Math.sin(turn), way.f_82480_, way.f_82479_ * Math.sin(turn) + way.f_82481_ * Math.cos(turn));
        }
        Bolt bolt = new Bolt();
        bolt.position = from;
        bolt.velocity = way.m_82490_(speed);
        bolt.life = (int)Math.ceil(range / speed);
        bolt.damage = NpcRules.damage(who.tier()) * harder;
        mind.bolts.add(bolt);
        fx(npc, S2CUkakuFx.BOLT, bolt.id, from, bolt.velocity, 1.0F, bolt.life);
        UkakuFire.sound(level, from, "entity.blaze.shoot", 0.5F, 1.35F + 0.1F * (float)npc.m_217043_().m_188500_());
    }

    /** Where a wing's shot leaves: its tip as the wing curls to aim ({@link WingRig#aimTip}), as Ukaku's. */
    static Vec3 tip(GhoulNpc npc, NpcIdentity who, int side, Vec3 aim) {
        double yaw = Math.toRadians(npc.f_20883_);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        Vec3 toward = aim.m_82546_(npc.m_20182_().m_82520_(0.0, WingRig.ROOT_Y, 0.0));
        double[] rig = {toward.f_82479_ * rightX + toward.f_82481_ * rightZ, toward.f_82480_, toward.f_82479_ * forwardX + toward.f_82481_ * forwardZ};
        double length = Math.sqrt(rig[0] * rig[0] + rig[1] * rig[1] + rig[2] * rig[2]);
        double[] dir = length < 1.0E-6 ? WingRig.FORWARD : new double[] {rig[0] / length, rig[1] / length, rig[2] / length};
        double[] local = WingRig.aimTip(who.lookMastery(), WingForm.TWIN, side, dir, false);
        return new Vec3(npc.m_20185_() + rightX * local[0] + forwardX * local[2], npc.m_20186_() + local[1], npc.m_20189_() + rightZ * local[0] + forwardZ * local[2]);
    }

    /** Every shot of this NPC's a tick on: what it meets is burnt, a wall stops it, and it fizzles at the end of its range. */
    static void fly(GhoulNpc npc, NpcIdentity who, GhoulNpcBrain.Mind mind, ServerLevel level, long now) {
        for (Iterator<Bolt> it = mind.bolts.iterator(); it.hasNext(); ) {
            if (!step(npc, who, mind, it.next(), level, now)) {
                it.remove();
            }
        }
    }

    private static boolean step(GhoulNpc npc, NpcIdentity who, GhoulNpcBrain.Mind mind, Bolt bolt, ServerLevel level, long now) {
        Vec3 from = bolt.position;
        Vec3 to = from.m_82549_(bolt.velocity);
        BlockHitResult block = level.m_45547_(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, npc));
        boolean wall = block.m_6662_() == HitResult.Type.BLOCK;
        Vec3 end = wall ? block.m_82450_() : to;
        // 1.0.42: a kagune M1 on its way meets a shard coming at its owner first, as Ukaku's are (KaguneClash): it bursts there.
        Vec3 met = KaguneClash.swat(npc, level, from, end, now);
        if (met != null) {
            end = met;
            wall = false;
        }
        AABB search = new AABB(from.f_82479_, from.f_82480_, from.f_82481_, end.f_82479_, end.f_82480_, end.f_82481_).m_82400_(NpcRules.SHOT_RADIUS + 1.0);
        List<LivingEntity> near = level.m_6443_(LivingEntity.class, search, living -> living != npc && !bolt.hit.contains(living.m_19879_())
            && GhoulNpcBrain.fair(npc, who, mind, living, now));
        int samples = Math.max(1, (int)Math.ceil(from.m_82554_(end) / 0.25));
        for (int sample = 0; sample <= samples; sample++) {
            Vec3 point = from.m_82549_(end.m_82546_(from).m_82490_(sample / (double)samples));
            for (LivingEntity body : near) {
                if (!body.m_20191_().m_82400_(NpcRules.SHOT_RADIUS).m_82390_(point)) {
                    continue;
                }
                bolt.hit.add(body.m_19879_());
                burn(npc, body, bolt);
                fx(npc, S2CUkakuFx.IMPACT, bolt.id, point, bolt.velocity, 1.0F, 0);
                return false;
            }
        }
        if (met != null || wall) {
            fx(npc, S2CUkakuFx.IMPACT, bolt.id, end, bolt.velocity, 0.0F, 0);
            return false;
        }
        bolt.position = to;
        if (--bolt.life <= 0) {
            fx(npc, S2CUkakuFx.FIZZLE, bolt.id, to, bolt.velocity, 1.0F, 0);
            return false;
        }
        return true;
    }

    /** A shot's hit: hurt (a mob's attack), burning, pushed a little the way it flew. */
    private static void burn(GhoulNpc npc, LivingEntity body, Bolt bolt) {
        Vec3 centre = GhoulNpcBrain.centre(body);
        Vec3 into = bolt.velocity.m_82541_();
        WoundHits.expect(body, WoundBook.Blow.BURN, centre.m_82546_(into.m_82490_(0.3)), into, 0.0);
        body.f_19802_ = 0;
        if (!body.m_6469_(npc.m_269291_().m_269333_(npc), bolt.damage)) {
            return;
        }
        body.m_20254_(NpcRules.SHOT_BURN);
        if (bolt.velocity.m_165925_() > 1.0E-6) {
            body.m_147240_(0.2, -bolt.velocity.f_82479_, -bolt.velocity.f_82481_);
        }
        UkakuFire.sound((ServerLevel)npc.m_9236_(), centre, "entity.generic.burn", 0.5F, 1.2F);
    }

    /** Its shots in flight put out (it died, or left): every client stops drawing them. */
    static void fizzleAll(GhoulNpc npc, GhoulNpcBrain.Mind mind) {
        for (Bolt bolt : mind.bolts) {
            fx(npc, S2CUkakuFx.FIZZLE, bolt.id, bolt.position, bolt.velocity, 1.0F, 0);
        }
        mind.bolts.clear();
    }

    private static void fx(GhoulNpc npc, int kind, int id, Vec3 at, Vec3 velocity, float size, int extra) {
        ControlNetwork.broadcastAround(npc, new S2CUkakuFx(kind, npc.m_19879_(), id, at.f_82479_, at.f_82480_, at.f_82481_, (float)velocity.f_82479_,
            (float)velocity.f_82480_, (float)velocity.f_82481_, size, extra));
    }

    // ---------------------------------------------------------------- 1.0.41: moving like a ghoul

    /** True while it is in a flip (its body turning over): it neither strikes nor shoots. */
    static boolean flipping(GhoulNpc npc, GhoulNpcBrain.Mind mind, long now) {
        if (mind.flipAt == Long.MIN_VALUE) {
            return false;
        }
        long age = now - mind.flipAt;
        return age < NpcRules.FLIP_BUSY || age < NpcRules.FLIP_POSED && !npc.m_20096_();
    }

    /**
     * Its feet, before it strikes or shoots: a blow coming at it answered (met with its own lash, or a flip clear of it), a front flip
     * in on prey too far off, a back flip clear of prey too close. True while it is in a flip (nothing else this tick).
     */
    static boolean maneuver(GhoulNpc npc, NpcIdentity who, GhoulNpcBrain.Mind mind, LivingEntity prey, ServerLevel level, long now) {
        if (mind.flipAt != Long.MIN_VALUE) {
            if (now - mind.flipAt == 1L && mind.flipPush != null) {
                // The push renewed: the ground's grip takes most of the first tick's.
                npc.m_20256_(mind.flipPush);
                npc.f_19864_ = true;
            }
            if (flipping(npc, mind, now)) {
                npc.m_21573_().m_26573_();
                return true;
            }
        }
        boolean rinkaku = who.kind() == NpcIdentity.Kind.RINKAKU;
        double distance = Math.sqrt(prey.m_20280_(npc));
        boolean ready = npc.m_20096_() && now >= mind.flipReadyAt;
        KaguneClash.Threat threat = KaguneClash.threat(npc, now);
        if (threat != null && threat.began() != mind.answered) {
            mind.answered = threat.began();
            double roll = npc.m_217043_().m_188500_();
            int limbs = limbs(who);
            if (rinkaku && threat.who() == prey && mind.move == GraftMove.NONE && distance <= GraftCombat.reach(GraftMove.LASH_3, limbs) + 0.3
                && roll < NpcRules.counter(who.tier())) {
                // Met with its own: the skewer straight back at it (the two meet, KaguneClash).
                begin(npc, who, mind, prey, GraftMove.LASH_3, level, now);
                mind.strung = 1;
                return false;
            }
            if (ready && roll < NpcRules.dodge(who.tier()) + (rinkaku ? 0.0 : 0.15)) {
                flip(npc, who, mind, com.hex.kagune.control.pale.PaleRules.Flip.BACK, flat(npc.m_20182_().m_82546_(threat.who().m_20182_())),
                    NpcRules.FLIP_BACK_SPEED, NpcRules.FLIP_BACK_UP, threat.who(), level, now);
                return true;
            }
        }
        if (!ready || mind.move != GraftMove.NONE || mind.volleyAt != Long.MIN_VALUE || npc.m_217043_().m_188500_() > NpcRules.FLIP_CHANCE) {
            return false;
        }
        boolean sniper = NpcRules.sniper(who.seed());
        double near = rinkaku ? NpcRules.CROWDED : (sniper ? NpcRules.SNIPE_NEAREST : NpcRules.SHOOT_NEAREST) - 1.0;
        double want = rinkaku ? preferred(who) : sniper ? (NpcRules.SNIPE_NEAREST + NpcRules.SNIPE_FARTHEST) / 2.0
            : (NpcRules.SHOOT_NEAREST + NpcRules.SHOOT_FARTHEST) / 2.0;
        if (distance < near) {
            flip(npc, who, mind, com.hex.kagune.control.pale.PaleRules.Flip.BACK, flat(npc.m_20182_().m_82546_(prey.m_20182_())), NpcRules.FLIP_BACK_SPEED,
                NpcRules.FLIP_BACK_UP, prey, level, now);
            return true;
        }
        double most = rinkaku ? NpcRules.CLOSE_IN_MOST : want + 2.0 * NpcRules.CLOSE_IN + 4.0;
        if (distance > want + NpcRules.CLOSE_IN && distance < most && npc.m_142582_(prey)) {
            flip(npc, who, mind, com.hex.kagune.control.pale.PaleRules.Flip.FORWARD, flat(prey.m_20182_().m_82546_(npc.m_20182_())),
                NpcRules.flipSpeed(distance - want), NpcRules.FLIP_FORWARD_UP, prey, level, now);
            return true;
        }
        return false;
    }

    /**
     * A flip ({@code kind}) along {@code way} (level, unit), {@code speed} blocks a tick along and {@code up} up, facing {@code facing}
     * as it goes over; whatever it was making stops, and every client sees it go.
     */
    static void flip(GhoulNpc npc, NpcIdentity who, GhoulNpcBrain.Mind mind, com.hex.kagune.control.pale.PaleRules.Flip kind, Vec3 way, double speed,
                     double up, LivingEntity facing, ServerLevel level, long now) {
        npc.m_21573_().m_26573_();
        Vec3 push = new Vec3(way.f_82479_ * speed, up, way.f_82481_ * speed);
        npc.m_20256_(push);
        npc.f_19864_ = true;
        npc.f_19789_ = 0.0F;
        mind.flipPush = push;
        mind.flip = kind.ordinal();
        mind.flipAt = now;
        mind.flipReadyAt = now + NpcRules.flipEvery(who.tier());
        if (mind.move != GraftMove.NONE) {
            mind.move = GraftMove.NONE;
            mind.strung = 0;
        }
        if (mind.volleyAt != Long.MIN_VALUE) {
            mind.volleyAt = Long.MIN_VALUE;
            mind.shots = 0;
        }
        if (facing != null) {
            Vec3 to = facing.m_20182_().m_82546_(npc.m_20182_());
            float yaw = (float)Math.toDegrees(Math.atan2(-to.f_82479_, to.f_82481_));
            npc.m_146922_(yaw);
            npc.f_20883_ = yaw;
            npc.f_20885_ = yaw;
        }
        mind.dirty = true;
        sound(level, npc.m_20182_(), HexKaguneSounds.KAGUNE_WHIP.get(), 0.5F, 0.85F);
        UkakuFire.sound(level, npc.m_20182_(), "entity.player.attack.sweep", 0.35F, 0.7F);
    }

    // ---------------------------------------------------------------- moving and the rest

    /** Between strings it circles its prey at {@code radius}, now one way, now the other, sizing it up. */
    private static void circle(GhoulNpc npc, GhoulNpcBrain.Mind mind, LivingEntity prey, double radius, long now) {
        if (now >= mind.circleAt) {
            mind.circleAt = now + 30L + (long)(npc.m_217043_().m_188500_() * 30.0);
            mind.circle = npc.m_217043_().m_188500_() < 0.5 ? -1.0 : 1.0;
        }
        if (now % 5L != 0L) {
            return;
        }
        Vec3 from = flat(npc.m_20182_().m_82546_(prey.m_20182_()));
        double angle = Math.atan2(from.f_82481_, from.f_82479_) + mind.circle * 0.6;
        Vec3 to = prey.m_20182_().m_82520_(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
        npc.m_21573_().m_26519_(to.f_82479_, prey.m_20186_(), to.f_82481_, 0.9);
    }

    private static long pause(GhoulNpc npc) {
        return NpcRules.PAUSE_LEAST + (long)(npc.m_217043_().m_188500_() * (NpcRules.PAUSE_MOST - NpcRules.PAUSE_LEAST));
    }

    static Vec3 flat(Vec3 direction) {
        return UkakuFire.horizontal(direction);
    }

    private static void sound(ServerLevel level, Vec3 at, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level.m_6263_(null, at.f_82479_, at.f_82480_, at.f_82481_, sound, SoundSource.HOSTILE, volume, pitch);
    }
}
