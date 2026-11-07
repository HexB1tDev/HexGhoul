package com.hex.kagune.control.server;

import com.hex.kagune.control.npc.AgentRules;
import com.hex.kagune.control.npc.ArmRules;
import com.hex.kagune.control.npc.CcgAgent;
import com.hex.kagune.control.quinque.Arm;
import com.hex.kagune.control.wound.WoundBook;
import java.util.Iterator;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 3.0.0 (the owner: "CCG rework, make the ccg moveset way more complex, better ranks get better custom weapons ... tendril whips that can
 * cut and grab, crystal spears, big flesh hammers, big two handed swords made from flesh and bone of kagunes all with long M1 chains and
 * right click abilities that the investigators can use at will"): an investigator fighting with its kagune-made weapon ({@link Arm},
 * {@link ArmRules}), on the server.
 *
 * <p>Its rank decides whether it has one (rolled once, kept in its saved data: {@link #wake}); at peace it carries the case it is kept in.
 * In a fight it tears out of the case (its hand empty: the weapon is drawn by every client from the synced state, {@code CcgAgent.arm}),
 * and the investigator fights on its own feet as the others do (its partner's sides, its parries, flips and dashes: {@link AgentBrain}),
 * with its weapon's long string of blows ({@link #begin}, {@link #land}: each its own reach, arc, weight, step and throw) and, now and then,
 * one of its two moves ({@link #think}, {@link #move}): the whip's Bind and Flay, the spear's Javelin and Piercing Charge, the hammer's
 * Quake and Whirlwind, the greatsword's Leaping Cut and Cleave.
 */
final class AgentArms {
    /** Where its weapon is kept ({@link ArmRules#pack} without "out"; 0: rolled, none). */
    static final String KEY = CcgAgent.KEY + "Arm";

    private AgentArms() {
    }

    // ---------------------------------------------------------------- who has one

    /** Its first tick: its weapon, from its saved data, or rolled now by its rank (and kept); it carries that weapon's case at peace. */
    static void wake(CcgAgent agent, AgentBrain.Mind mind) {
        CompoundTag data = agent.getPersistentData();
        int packed;
        if (data.m_128441_(KEY)) {
            packed = data.m_128451_(KEY);
        } else {
            Arm rolled = ArmRules.roll(mind.rank, agent.m_217043_().m_188500_(), agent.m_217043_().m_188500_());
            packed = ArmRules.pack(rolled, ArmRules.masterwork(mind.rank), false);
            data.m_128405_(KEY, packed);
        }
        mind.arm = ArmRules.arm(packed);
        mind.master = mind.arm != null && ArmRules.masterwork(packed);
        mind.armOut = false;
        if (mind.arm != null && mind.kind != mind.arm.carried) {
            mind.kind = mind.arm.carried;
            AgentBrain.save(agent, mind.look, mind.kind, mind.leader, mind.partner);
            agent.dress(mind.look, mind.kind);
        }
        agent.arm(ArmRules.pack(mind.arm, mind.master, false), Long.MIN_VALUE);
        agent.armTarget(0);
    }

    /** Every tick: wounds its Cleave left bleed. */
    static void tick(CcgAgent agent, AgentBrain.Mind mind, ServerLevel level, long now) {
        if (mind.bleeds.isEmpty() || now % 20L != 0L) {
            return;
        }
        Iterator<long[]> each = mind.bleeds.iterator();
        while (each.hasNext()) {
            long[] bleed = each.next();
            Entity found = level.m_6815_((int)bleed[0]);
            if (now > bleed[1] || !(found instanceof LivingEntity body) || !body.m_6084_()) {
                each.remove();
                continue;
            }
            body.f_19802_ = 0;
            body.m_6469_(agent.m_269291_().m_269333_(agent), Float.intBitsToFloat((int)bleed[2]));
            UkakuFire.sound(level, GhoulNpcBrain.centre(body), "hexkagune:sfx.drip", 0.5F, 0.9F + 0.2F * (float)agent.m_217043_().m_188500_());
        }
    }

    // ---------------------------------------------------------------- out of its case and back

    /** It tears out of its case: its hand empty, every client sees it grow out ({@link ArmRules#UNFOLD} ticks). */
    static void draw(CcgAgent agent, AgentBrain.Mind mind, ServerLevel level, long now) {
        mind.armOut = true;
        mind.armAt = now;
        mind.readyAt = Math.max(mind.readyAt, now + ArmRules.UNFOLD);
        agent.m_21008_(InteractionHand.MAIN_HAND, ItemStack.f_41583_);
        agent.arm(ArmRules.pack(mind.arm, mind.master, true), now);
        Vec3 at = hand(agent);
        UkakuFire.sound(level, at, "hexkagune:sfx.case_open", 1.0F, 0.95F + 0.1F * (float)agent.m_217043_().m_188500_());
        UkakuFire.sound(level, at, mind.arm == Arm.CRYSTAL_SPEAR ? "hexkagune:sfx.crystal_form" : "hexkagune:manifest_rip", 0.7F, 0.9F);
        UkakuFire.sound(level, at, "hexkagune:kagune_reshape", 0.6F, 1.0F);
    }

    /** The fight long over: back into its case (the case in its hand again once it is in). */
    static void sheathe(CcgAgent agent, AgentBrain.Mind mind, ServerLevel level, long now) {
        cancel(agent, mind, now);
        mind.armOut = false;
        mind.armAt = now;
        agent.arm(ArmRules.pack(mind.arm, mind.master, false), now);
        Vec3 at = hand(agent);
        UkakuFire.sound(level, at, "hexkagune:kagune_dismiss", 0.6F, 1.0F);
        UkakuFire.sound(level, at, "hexkagune:sfx.case_close", 1.0F, 0.95F + 0.1F * (float)agent.m_217043_().m_188500_());
    }

    // ---------------------------------------------------------------- a fight

    static void fight(CcgAgent agent, AgentBrain.Mind mind, CcgAgent partner, LivingEntity prey, ServerLevel level, long now) {
        if (!mind.armOut) {
            draw(agent, mind, level, now);
        }
        if (AgentBrain.ago(now, mind.armAt) < ArmRules.UNFOLD) {
            agent.m_21573_().m_26573_(); // still tearing out of its case
            return;
        }
        if (mind.move != null) {
            move(agent, mind, level, now);
            return;
        }
        if (mind.striking && now >= mind.landsAt) {
            if (mind.parrying) {
                AgentBrain.land(agent, mind, level, now);
            } else {
                land(agent, mind, level, now);
            }
        }
        if (AgentBrain.maneuver(agent, mind, prey, level, now)) {
            return;
        }
        if (!mind.striking && think(agent, mind, prey, level, now)) {
            return;
        }
        AgentBrain.close(agent, mind, partner, prey, level, now);
    }

    /** The next blow of its string sets out at {@code prey}: on its way (it can be met, KaguneClash), landing at its own tick. */
    static void begin(CcgAgent agent, AgentBrain.Mind mind, LivingEntity prey, ServerLevel level, long now) {
        Arm arm = mind.arm;
        mind.step = ArmRules.next(arm, mind.step, AgentBrain.ago(now, mind.stepAt));
        mind.stepAt = now;
        Arm.Strike strike = arm.strike(mind.step);
        mind.striking = true;
        mind.parrying = false;
        mind.target = prey.m_19879_();
        mind.landsAt = now + strike.land();
        int token = ++mind.token;
        Vec3 aim = UkakuFire.horizontal(prey.m_20182_().m_82546_(agent.m_20182_()));
        // 3.2.4: on the clash's shared scale: its rank's power, its weapon's heft counted by its root (ClashPower).
        double dealt = com.hex.kagune.control.ClashPower.agent(AgentRules.power(mind.rank, mind.superior), ArmRules.damage(arm, mind.step, mind.master));
        KaguneClash.swing(agent, now, strike.land(), ArmRules.reach(arm, mind.step, mind.master) + 0.5, aim, prey.m_19879_(), dealt,
            () -> mind.token == token && mind.striking, () -> AgentBrain.stopped(agent, mind, token));
        agent.swing(ArmRules.stepCode(mind.step), now);
        boolean last = mind.step == arm.length() - 1;
        UkakuFire.sound(level, hand(agent), swingSound(arm, last), 0.65F, pitch(arm) + 0.12F * (float)agent.m_217043_().m_188500_());
        Vec3 velocity = agent.m_20184_();
        double step = strike.step() * 0.8;
        agent.m_20256_(new Vec3(aim.f_82479_ * step + velocity.f_82479_ * 0.2, velocity.f_82480_, aim.f_82481_ * step + velocity.f_82481_ * 0.2));
    }

    /** The blow lands: unless it was met by one coming back, it cuts (crushes, runs through) every foe in its reach and arc. */
    static void land(CcgAgent agent, AgentBrain.Mind mind, ServerLevel level, long now) {
        Arm arm = mind.arm;
        int step = mind.step;
        Arm.Strike strike = arm.strike(step);
        mind.striking = false;
        mind.parrying = false;
        mind.readyAt = now + ArmRules.recover(arm, step, mind.rank.pace);
        Entity found = level.m_6815_(mind.target);
        LivingEntity prey = found instanceof LivingEntity body && body.m_6084_() ? body : null;
        if (prey != null && KaguneClash.meets(agent, prey, now)) {
            mind.readyAt = now + 10L; // met: both thrown apart, nothing hurt
            return;
        }
        KaguneClash.landed(agent);
        double reach = ArmRules.reach(arm, step, mind.master);
        Vec3 look = prey != null ? UkakuFire.horizontal(prey.m_20182_().m_82546_(agent.m_20182_())) : UkakuFire.horizontal(agent.m_20154_());
        float dealt = ArmRules.damage(arm, step, mind.master) * AgentRules.power(mind.rank, mind.superior);
        boolean any = false;
        for (LivingEntity body : foes(agent, mind, prey, reach + 1.5)) {
            if (inArc(agent, body, look, reach, strike.arc())) {
                any |= hit(agent, mind, body, body == prey ? dealt : dealt * 0.7F, strike.blow(), agent.m_20182_(), strike.push(), strike.lift(), level,
                    now);
            }
        }
        boolean last = step == arm.length() - 1;
        if (any) {
            UkakuFire.sound(level, prey != null ? GhoulNpcBrain.centre(prey) : hand(agent), hitSound(arm, strike.blow()), 0.8F,
                0.9F + 0.15F * (float)agent.m_217043_().m_188500_());
            if (last) {
                UkakuFire.sound(level, hand(agent), "hexkagune:sfx.finisher", 0.7F, arm == Arm.FLESH_HAMMER ? 0.7F : 1.0F);
                mind.clear = agent.m_217043_().m_188500_() < 0.3; // now and then it flips clear after its string
            }
        }
        if (last && arm == Arm.FLESH_HAMMER) {
            KaguneClash.quake(agent, agent.m_20182_().m_82549_(look.m_82490_(1.6)), 1.3F); // its smash shakes the ground
        }
    }

    // ---------------------------------------------------------------- its moves

    /** Now and then (by its rank), one of its two moves, if its foe is at a distance it is used from and in its sight. True if it began one. */
    static boolean think(CcgAgent agent, AgentBrain.Mind mind, LivingEntity prey, ServerLevel level, long now) {
        if (Math.floorMod(now + agent.m_19879_(), ArmRules.THINK_EVERY) != 0L || !agent.m_20096_() || !agent.m_142582_(prey)) {
            return false;
        }
        double distance = Math.sqrt(prey.m_20280_(agent));
        boolean[] ready = {now >= mind.moveReady[0], now >= mind.moveReady[1]};
        Arm.Ability ability = ArmRules.ability(mind.arm, mind.rank, distance, ready, agent.m_217043_().m_188500_(), agent.m_217043_().m_188500_());
        if (ability == null || ability == Arm.Ability.PIERCE
            && !AgentBrain.clear(agent, level, UkakuFire.horizontal(prey.m_20182_().m_82546_(agent.m_20182_())))) {
            return false; // (never a charge into a wall or off a drop)
        }
        start(agent, mind, ability, prey, level, now);
        return true;
    }

    /** {@code ability} begins, at {@code prey}: every client sees it ({@code CcgAgent.swing}); its cooldown starts. */
    static void start(CcgAgent agent, AgentBrain.Mind mind, Arm.Ability ability, LivingEntity prey, ServerLevel level, long now) {
        mind.move = ability;
        mind.moveAt = now;
        mind.moveTarget = prey.m_19879_();
        mind.moveHit = 0;
        mind.moveStruck.clear();
        mind.bound = false;
        mind.moveWay = UkakuFire.horizontal(prey.m_20182_().m_82546_(agent.m_20182_()));
        if (mind.moveWay.m_82556_() < 1.0E-6) {
            mind.moveWay = UkakuFire.horizontal(agent.m_20154_());
        }
        int slot = mind.arm.slot(ability);
        if (slot >= 0) {
            mind.moveReady[slot] = now + ArmRules.cooldown(ability, mind.rank.pace);
        }
        agent.m_21573_().m_26573_();
        agent.swing(ArmRules.abilityCode(ability), now);
        Vec3 at = hand(agent);
        switch (ability) {
            case BIND -> UkakuFire.sound(level, at, "hexkagune:kagune_whip", 0.9F, 0.8F);
            case FLAY -> UkakuFire.sound(level, at, "hexkagune:sfx.writhe", 0.8F, 1.1F);
            case JAVELIN -> UkakuFire.sound(level, at, "hexkagune:sfx.crystal_form", 0.9F, 0.8F);
            case PIERCE -> UkakuFire.sound(level, at, "hexkagune:sfx.windup_clash", 0.7F, 1.2F);
            case QUAKE -> UkakuFire.sound(level, at, "hexkagune:kagune_strain", 0.9F, 0.7F);
            case WHIRL -> UkakuFire.sound(level, at, "hexkagune:sfx.whoosh", 0.9F, 0.7F);
            case LEAP -> UkakuFire.sound(level, at, "hexkagune:sfx.step_heavy", 0.9F, 0.8F);
            case CLEAVE -> UkakuFire.sound(level, at, "hexkagune:sfx.windup_clash", 0.8F, 0.8F);
        }
    }

    /** Its move under way, a tick on: its feet, its blows as they come due, and over at its end. */
    static void move(CcgAgent agent, AgentBrain.Mind mind, ServerLevel level, long now) {
        Arm.Ability ability = mind.move;
        long age = now - mind.moveAt;
        Entity found = level.m_6815_(mind.moveTarget);
        LivingEntity target = found instanceof LivingEntity body && body.m_6084_() ? body : null;
        if (target != null && ability != Arm.Ability.PIERCE && ability != Arm.Ability.LEAP) {
            AgentBrain.face(agent, target);
            agent.m_21563_().m_24960_(target, 30.0F, 30.0F);
            mind.moveWay = way(agent, target, mind.moveWay);
        }
        agent.m_21573_().m_26573_();
        feet(agent, mind, ability, target, age);
        while (mind.move == ability && mind.moveHit < ability.hits.length && age >= ability.hits[mind.moveHit]) {
            int index = mind.moveHit++;
            blow(agent, mind, ability, index, target, level, now);
        }
        if (mind.move == ability && ability == Arm.Ability.PIERCE && age >= ability.hits[0] && age < ability.hits[1]) {
            through(agent, mind, level, now); // its charge goes through whoever is in its way
        }
        if (mind.move == ability && ability == Arm.Ability.BIND && mind.bound && age < ability.hits[1]) {
            drag(agent, mind, target);
        }
        if (mind.move == ability && (age >= ability.total || ability == Arm.Ability.BIND && !mind.bound && age >= ability.hits[0] + 6L)) {
            finish(agent, mind, now);
        }
    }

    /** How it moves through {@code ability}: a hop, a leap, a charge, a slow advance, or planted. */
    private static void feet(CcgAgent agent, AgentBrain.Mind mind, Arm.Ability ability, LivingEntity target, long age) {
        Vec3 velocity = agent.m_20184_();
        Vec3 way = mind.moveWay;
        switch (ability) {
            case PIERCE -> {
                if (age >= ability.hits[0] && age < ability.hits[1]) {
                    double speed = AgentRules.dashSpeed(mind.rank) * 1.25;
                    agent.m_20256_(new Vec3(way.f_82479_ * speed, Math.min(velocity.f_82480_, 0.05), way.f_82481_ * speed));
                    agent.f_19864_ = true;
                } else if (age == ability.hits[1]) {
                    agent.m_20256_(new Vec3(velocity.f_82479_ * 0.2, velocity.f_82480_, velocity.f_82481_ * 0.2));
                    agent.f_19864_ = true;
                }
            }
            case QUAKE -> {
                if (age == 3L && agent.m_20096_()) {
                    double gap = target == null ? 0.0 : Math.sqrt(target.m_20280_(agent));
                    double forward = Math.min(0.35, Math.max(0.0, gap - 1.8) / 12.0);
                    agent.m_20256_(new Vec3(way.f_82479_ * forward, 0.55, way.f_82481_ * forward));
                    agent.f_19864_ = true;
                } else if (age == ability.hits[0] - 3L && !agent.m_20096_()) {
                    agent.m_20256_(new Vec3(velocity.f_82479_ * 0.5, -0.9, velocity.f_82481_ * 0.5)); // brought down hard
                    agent.f_19864_ = true;
                }
            }
            case LEAP -> {
                int up = ability.hits[0];
                int down = ability.hits[1];
                if (age == up) {
                    agent.f_19789_ = 0.0F;
                }
                if (age >= up && age < down) {
                    // At its foe (where they are now), level across, falling on them as the cut comes down.
                    Vec3 rel = target == null ? way.m_82490_(3.0) : target.m_20182_().m_82546_(agent.m_20182_());
                    double gap = Math.sqrt(horizontal(rel));
                    double left = Math.max(1.0, down - age);
                    double speed = Math.min(1.1, Math.max(0.0, gap - 1.2) / left);
                    Vec3 flat = gap < 1.0E-3 ? Vec3.f_82478_ : new Vec3(rel.f_82479_ / gap, 0.0, rel.f_82481_ / gap);
                    double vy = age == up ? 0.62 : age >= down - 2 ? Math.min(velocity.f_82480_, -0.8) : velocity.f_82480_;
                    agent.m_20256_(new Vec3(flat.f_82479_ * speed, vy, flat.f_82481_ * speed));
                    agent.f_19864_ = true;
                    if (flat.m_82556_() > 0.0) {
                        mind.moveWay = flat;
                        face(agent, flat);
                    }
                }
            }
            case WHIRL -> {
                if (age >= 6L && age < ability.total - 6L) {
                    double speed = 0.12;
                    agent.m_20256_(new Vec3(way.f_82479_ * speed, velocity.f_82480_, way.f_82481_ * speed));
                }
            }
            default -> {
                agent.m_20256_(new Vec3(velocity.f_82479_ * 0.5, velocity.f_82480_, velocity.f_82481_ * 0.5));
            }
        }
    }

    /** Its {@code index}th blow of {@code ability} comes due. */
    private static void blow(CcgAgent agent, AgentBrain.Mind mind, Arm.Ability ability, int index, LivingEntity target, ServerLevel level, long now) {
        float dealt = ArmRules.damage(mind.arm, ability, mind.master) * AgentRules.power(mind.rank, mind.superior);
        double edge = mind.master ? 1.12 : 1.0;
        Vec3 from = agent.m_20182_();
        switch (ability) {
            case BIND -> {
                if (index == 0) {
                    // Thrown round them: caught if they are still within its length and in its sight; a sting as it bites in.
                    double length = ability.farthest * edge + 1.5;
                    if (target != null && target.m_20280_(agent) <= length * length && agent.m_142582_(target)) {
                        mind.bound = true;
                        agent.armTarget(target.m_19879_());
                        hit(agent, mind, target, dealt * 0.2F, Arm.Strike.SLASH, from, 0.0, 0.0, level, now);
                        UkakuFire.sound(level, GhoulNpcBrain.centre(target), "hexkagune:kagune_grab", 1.0F, 0.9F);
                        UkakuFire.sound(level, GhoulNpcBrain.centre(target), "hexkagune:sfx.grab", 0.8F, 1.0F);
                    } else {
                        UkakuFire.sound(level, hand(agent), "hexkagune:kagune_whip", 0.6F, 1.3F); // it cracks on empty air
                    }
                } else if (mind.bound && target != null) {
                    // Dragged in and slammed down.
                    Vec3 at = GhoulNpcBrain.centre(target);
                    hit(agent, mind, target, dealt, Arm.Strike.CRUSH, from, 0.15, 0.0, level, now);
                    Vec3 velocity = target.m_20184_();
                    target.m_20256_(new Vec3(velocity.f_82479_ * 0.2, -1.0, velocity.f_82481_ * 0.2));
                    target.f_19864_ = true;
                    KaguneClash.quake(agent, target.m_20182_(), 1.2F);
                    level.m_8767_(ParticleTypes.f_123759_, at.f_82479_, target.m_20186_() + 0.1, at.f_82481_, 10, 0.5, 0.05, 0.5, 0.03);
                    UkakuFire.sound(level, at, "hexkagune:kagune_slam", 1.0F, 0.85F);
                }
            }
            case FLAY -> {
                Vec3 look = mind.moveWay;
                boolean any = false;
                for (LivingEntity body : foes(agent, mind, target, ability.farthest * edge + 1.5)) {
                    if (inArc(agent, body, look, ability.farthest * edge, 120.0)) {
                        any |= hit(agent, mind, body, body == target ? dealt : dealt * 0.75F, Arm.Strike.SLASH, from, 0.12, 0.0, level, now);
                    }
                }
                UkakuFire.sound(level, hand(agent), "hexkagune:kagune_whip", 0.7F, 1.0F + 0.08F * index);
                if (any) {
                    UkakuFire.sound(level, target != null ? GhoulNpcBrain.centre(target) : hand(agent), "hexkagune:sfx.slash_spray", 0.6F, 1.1F);
                }
            }
            case JAVELIN -> {
                // Thrown: a long javelin of crystal (three shards in a line, the head the hardest), at where they will be.
                Vec3 hand = agent.m_146892_().m_82520_(0.0, -0.1, 0.0).m_82549_(side(mind.moveWay).m_82490_(0.3));
                Vec3 aim = target == null ? hand.m_82549_(mind.moveWay.m_82490_(10.0)) : GhoulNpcBrain.centre(target);
                double speed = 3.2;
                if (target != null) {
                    double flight = aim.m_82554_(hand) / speed;
                    Vec3 lead = target.m_20184_();
                    aim = aim.m_82520_(lead.f_82479_ * flight, 0.0, lead.f_82481_ * flight);
                }
                Vec3 way = aim.m_82546_(hand);
                if (way.m_82556_() < 1.0E-6) {
                    way = mind.moveWay;
                }
                way = way.m_82541_();
                for (int i = 0; i < 3; i++) {
                    CrystalShots.fire(agent, level, hand.m_82549_(way.m_82490_(-0.55 * i)), way, speed, 30.0, i == 0 ? dealt : dealt * 0.25F,
                        body -> AgentBrain.foe(agent, body));
                }
                agent.shot(now);
                UkakuFire.sound(level, hand, "hexkagune:shard_launch", 1.0F, 0.8F);
                UkakuFire.sound(level, hand, "hexkagune:sfx.spike_throw", 0.9F, 0.9F);
            }
            case PIERCE -> {
                if (index == 0) {
                    UkakuFire.sound(level, from, "hexkagune:sfx.dash", 0.9F, 0.9F);
                }
            }
            case QUAKE -> {
                // The hammer brought down on the ground: everyone near thrown up, the nearer the harder.
                Vec3 ground = from.m_82549_(mind.moveWay.m_82490_(1.4));
                double radius = ability.farthest * edge;
                for (LivingEntity body : foes(agent, mind, target, radius + 1.0)) {
                    double gap = Math.sqrt(horizontal(body.m_20182_().m_82546_(ground)));
                    if (gap <= radius + body.m_20205_() * 0.5 && Math.abs(body.m_20186_() - agent.m_20186_()) <= 2.5) {
                        double near = 1.0 - 0.4 * Math.min(1.0, gap / radius);
                        hit(agent, mind, body, (float)(dealt * near), Arm.Strike.CRUSH, ground, 0.55 * near, 0.6 * near, level, now);
                    }
                }
                KaguneClash.quake(agent, ground, 1.6F);
                level.m_8767_(ParticleTypes.f_123759_, ground.f_82479_, ground.f_82480_ + 0.1, ground.f_82481_, 24, radius * 0.4, 0.05, radius * 0.4, 0.06);
                level.m_8767_(ParticleTypes.f_123762_, ground.f_82479_, ground.f_82480_ + 0.2, ground.f_82481_, 12, 0.6, 0.1, 0.6, 0.02);
                UkakuFire.sound(level, ground, "hexkagune:kagune_slam", 1.0F, 0.7F);
                UkakuFire.sound(level, ground, "hexkagune:sfx.shockwave", 0.9F, 0.75F);
            }
            case WHIRL -> {
                double radius = ability.farthest * edge;
                for (LivingEntity body : foes(agent, mind, target, radius + 1.5)) {
                    if (inArc(agent, body, mind.moveWay, radius, 360.0)) {
                        hit(agent, mind, body, dealt, Arm.Strike.CRUSH, from, 0.45, 0.15, level, now);
                    }
                }
                UkakuFire.sound(level, hand(agent), "hexkagune:sfx.punch_swing_heavy", 0.9F, 0.7F + 0.08F * index);
            }
            case LEAP -> {
                if (index == 1) {
                    // Down onto them where it lands: a cut and the ground split round it.
                    Vec3 at = from.m_82549_(mind.moveWay.m_82490_(0.8));
                    for (LivingEntity body : foes(agent, mind, target, 4.0)) {
                        double gap = Math.sqrt(horizontal(body.m_20182_().m_82546_(at)));
                        if (gap <= 2.6 + body.m_20205_() * 0.5 && Math.abs(body.m_20186_() - agent.m_20186_()) <= 2.5) {
                            hit(agent, mind, body, body == target ? dealt : dealt * 0.6F, Arm.Strike.SLASH, at, 0.4, 0.25, level, now);
                        }
                    }
                    KaguneClash.quake(agent, at, 1.3F);
                    level.m_8767_(ParticleTypes.f_123759_, at.f_82479_, at.f_82480_ + 0.1, at.f_82481_, 14, 0.8, 0.05, 0.8, 0.04);
                    UkakuFire.sound(level, at, "hexkagune:sfx.blade_heavy", 1.0F, 0.75F);
                    UkakuFire.sound(level, at, "hexkagune:sfx.ground", 0.9F, 0.9F);
                } else {
                    UkakuFire.sound(level, from, "hexkagune:sfx.dash", 0.8F, 0.75F);
                }
            }
            case CLEAVE -> {
                double reach = ability.farthest * edge;
                boolean any = false;
                for (LivingEntity body : foes(agent, mind, target, reach + 1.5)) {
                    if (inArc(agent, body, mind.moveWay, reach, 200.0)) {
                        float cut = body == target ? dealt : dealt * 0.8F;
                        if (hit(agent, mind, body, cut, Arm.Strike.SLASH, from, 0.55, 0.1, level, now)) {
                            any = true;
                            // Left bleeding: a twelfth of the cut a second, four seconds.
                            mind.bleeds.add(new long[] {body.m_19879_(), now + 80L, Float.floatToIntBits(Math.max(0.5F, cut / 12.0F))});
                        }
                    }
                }
                UkakuFire.sound(level, hand(agent), "hexkagune:sfx.blade_heavy", 1.0F, 0.6F);
                if (any) {
                    UkakuFire.sound(level, target != null ? GhoulNpcBrain.centre(target) : hand(agent), "hexkagune:sfx.blood_spray", 0.9F, 0.9F);
                }
            }
        }
    }

    /** The Bind: its foe dragged in to it along the whip, lifted a little off its feet. */
    private static void drag(CcgAgent agent, AgentBrain.Mind mind, LivingEntity target) {
        if (target == null || target.m_20280_(agent) > 14.0 * 14.0) {
            mind.bound = false;
            agent.armTarget(0);
            return;
        }
        Vec3 to = agent.m_20182_().m_82549_(mind.moveWay.m_82490_(1.6 + target.m_20205_() * 0.5)).m_82546_(target.m_20182_());
        double length = Math.sqrt(to.m_82556_());
        Vec3 pull = length < 0.2 ? Vec3.f_82478_ : to.m_82490_(Math.min(0.75, length * 0.3) / length);
        double resist = Math.max(0.0, Math.min(1.0, target.m_21133_(net.minecraft.world.entity.ai.attributes.Attributes.f_22278_))) * 0.5;
        target.m_20256_(new Vec3(pull.f_82479_ * (1.0 - resist), Math.max(target.m_20184_().f_82480_, length > 1.0 ? 0.12 : 0.0), pull.f_82481_ * (1.0 - resist)));
        target.f_19864_ = true;
        target.f_19789_ = 0.0F;
    }

    /** The Piercing Charge, a tick on: whoever it runs into (once each) is run through and thrown along its way. */
    private static void through(CcgAgent agent, AgentBrain.Mind mind, ServerLevel level, long now) {
        float dealt = ArmRules.damage(mind.arm, Arm.Ability.PIERCE, mind.master) * AgentRules.power(mind.rank, mind.superior);
        Vec3 tip = agent.m_20182_().m_82549_(mind.moveWay.m_82490_(1.2));
        for (LivingEntity body : foes(agent, mind, null, 3.0)) {
            if (mind.moveStruck.contains(body.m_19879_())) {
                continue;
            }
            double gap = Math.sqrt(horizontal(body.m_20182_().m_82546_(tip)));
            if (gap <= 1.0 + body.m_20205_() * 0.5 && Math.abs(body.m_20186_() - agent.m_20186_()) <= 2.0) {
                mind.moveStruck.add(body.m_19879_());
                Vec3 ahead = body.m_20182_().m_82546_(mind.moveWay.m_82490_(2.0));
                if (hit(agent, mind, body, dealt, Arm.Strike.STAB, ahead, 0.9, 0.3, level, now)) {
                    UkakuFire.sound(level, GhoulNpcBrain.centre(body), "hexkagune:sfx.stab", 1.0F, 0.85F);
                }
            }
        }
    }

    /** Its move done: the whip let go, back to its string. */
    private static void finish(CcgAgent agent, AgentBrain.Mind mind, long now) {
        mind.move = null;
        mind.bound = false;
        mind.moveStruck.clear();
        agent.armTarget(0);
        agent.swing(-1, now);
        mind.readyAt = Math.max(mind.readyAt, now + 6L);
    }

    /** Broken off (stunned, the fight over, its feet taken): nothing more of it lands. */
    static void cancel(CcgAgent agent, AgentBrain.Mind mind, long now) {
        if (mind.move != null) {
            finish(agent, mind, now);
        }
    }

    // ---------------------------------------------------------------- landing a blow

    /** Its foes within {@code range} of it (and {@code prey}, whoever it is): never a bystander, never another investigator. */
    private static java.util.List<LivingEntity> foes(CcgAgent agent, AgentBrain.Mind mind, LivingEntity prey, double range) {
        return agent.m_9236_().m_6443_(LivingEntity.class, agent.m_20191_().m_82400_(range), body -> body != agent && body.m_6084_()
            && !(body instanceof CcgAgent) && (body == prey || body.m_19879_() == mind.prey || AgentBrain.foe(agent, body)));
    }

    /** True if {@code body} is within {@code reach} of it and inside the {@code arc} (degrees, the whole of it) about {@code look}. */
    static boolean inArc(LivingEntity agent, LivingEntity body, Vec3 look, double reach, double arc) {
        Vec3 to = body.m_20182_().m_82546_(agent.m_20182_());
        if (Math.abs(to.f_82480_) > 2.5) {
            return false;
        }
        double flat = Math.sqrt(horizontal(to));
        double half = body.m_20205_() * 0.5;
        if (flat > reach + half + 0.4) {
            return false;
        }
        if (arc >= 359.0 || flat < 0.8) {
            return true;
        }
        Vec3 dir = UkakuFire.horizontal(to);
        double cos = dir.f_82479_ * look.f_82479_ + dir.f_82481_ * look.f_82481_;
        if (cos >= Math.cos(Math.toRadians(arc * 0.5))) {
            return true;
        }
        // A narrow thrust still takes a body it brushes: in front, and within its half-width (and a little) of the line.
        double across = Math.abs(to.f_82479_ * -look.f_82481_ + to.f_82481_ * look.f_82479_);
        return cos > 0.0 && across <= half + 0.5;
    }

    /**
     * One blow on {@code body}: {@code dealt} (a quinque's bite on a ghoul: harder, and a ghoul player's mending held back), its wound,
     * thrown away from {@code from} ({@code push} along, {@code lift} up; blocks a tick, less as it resists). True if it hurt.
     */
    static boolean hit(CcgAgent agent, AgentBrain.Mind mind, LivingEntity body, float dealt, int blow, Vec3 from, double push, double lift,
                       ServerLevel level, long now) {
        Vec3 away = UkakuFire.horizontal(body.m_20182_().m_82546_(from));
        if (away.m_82556_() < 1.0E-6) {
            away = mind.moveWay.m_82556_() > 1.0E-6 ? mind.moveWay : UkakuFire.horizontal(agent.m_20154_());
        }
        boolean ghoul = QuinqueServer.ghoul(body, level);
        float amount = ghoul ? dealt * mind.kind.ghoul : dealt;
        Vec3 centre = GhoulNpcBrain.centre(body);
        WoundHits.expect(body, wound(blow), centre.m_82546_(away.m_82490_(0.3)), away, 0.0);
        body.f_19802_ = 0;
        if (!body.m_6469_(agent.m_269291_().m_269333_(agent), amount)) {
            return false;
        }
        if (ghoul && body instanceof ServerPlayer player) {
            GhoulRegen.suppress(player, now + mind.kind.suppress);
        }
        if (push > 0.0 || lift > 0.0) {
            double resist = Math.max(0.0, Math.min(1.0, body.m_21133_(net.minecraft.world.entity.ai.attributes.Attributes.f_22278_)));
            Vec3 velocity = body.m_20184_();
            double along = push * (1.0 - resist);
            body.m_20256_(new Vec3(velocity.f_82479_ * 0.3 + away.f_82479_ * along, Math.max(velocity.f_82480_, lift * (1.0 - resist * 0.5)),
                velocity.f_82481_ * 0.3 + away.f_82481_ * along));
            body.f_19864_ = true;
        }
        return true;
    }

    static WoundBook.Blow wound(int blow) {
        return switch (blow) {
            case Arm.Strike.STAB -> WoundBook.Blow.STAB;
            case Arm.Strike.CRUSH -> WoundBook.Blow.CRUSH;
            case Arm.Strike.BLUNT -> WoundBook.Blow.BLUNT;
            default -> WoundBook.Blow.SLASH;
        };
    }

    // ---------------------------------------------------------------- small things

    static String swingSound(Arm arm, boolean last) {
        return switch (arm) {
            case WHIP -> "hexkagune:kagune_whip";
            case CRYSTAL_SPEAR -> last ? "hexkagune:sfx.spike_heavy" : "hexkagune:sfx.blade_arc";
            case FLESH_HAMMER -> "hexkagune:sfx.punch_swing_heavy";
            case BONE_SWORD -> last ? "hexkagune:sfx.blade_heavy" : "hexkagune:sfx.blade_swing";
        };
    }

    static String hitSound(Arm arm, int blow) {
        if (blow == Arm.Strike.CRUSH) {
            return "hexkagune:sfx.crush";
        }
        if (blow == Arm.Strike.STAB) {
            return "hexkagune:sfx.stab";
        }
        if (blow == Arm.Strike.BLUNT) {
            return "hexkagune:sfx.punch_hit_heavy";
        }
        return arm == Arm.WHIP ? "hexkagune:sfx.slash_spray" : "hexkagune:flesh_impact";
    }

    static float pitch(Arm arm) {
        return switch (arm) {
            case WHIP -> 1.0F;
            case CRYSTAL_SPEAR -> 1.1F;
            case FLESH_HAMMER -> 0.65F;
            case BONE_SWORD -> 0.8F;
        };
    }

    /** About where its weapon hand is. */
    private static Vec3 hand(LivingEntity agent) {
        return agent.m_20182_().m_82520_(0.0, agent.m_20206_() * 0.6, 0.0);
    }

    /** Its way to {@code target}, level ({@code otherwise} if right on top of it). */
    private static Vec3 way(LivingEntity agent, LivingEntity target, Vec3 otherwise) {
        Vec3 to = UkakuFire.horizontal(target.m_20182_().m_82546_(agent.m_20182_()));
        return to.m_82556_() < 1.0E-6 ? otherwise : to;
    }

    /** Square to {@code way} (level), to its right. */
    private static Vec3 side(Vec3 way) {
        return new Vec3(-way.f_82481_, 0.0, way.f_82479_);
    }

    private static double horizontal(Vec3 v) {
        return v.f_82479_ * v.f_82479_ + v.f_82481_ * v.f_82481_;
    }

    /** It turns along {@code way} (level, unit). */
    private static void face(CcgAgent agent, Vec3 way) {
        float yaw = (float)Math.toDegrees(Math.atan2(-way.f_82479_, way.f_82481_));
        agent.m_146922_(yaw);
        agent.f_20883_ = yaw;
        agent.f_20885_ = yaw;
    }
}
