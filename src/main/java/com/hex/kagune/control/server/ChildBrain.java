package com.hex.kagune.control.server;

import com.hex.kagune.control.family.ChildLines;
import com.hex.kagune.control.family.ChildMoment;
import com.hex.kagune.control.family.ChildRules;
import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CChild;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.UniqueNpc;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.10: a child's mind (server): one body of it in the world ({@code UniqueNpcs}); growing through its stages (its box and health with
 * them, its parents told); carried in a parent's arms; a baby sitting where it is put; following who told it to, or waiting at home; a
 * child or teen that was taught fighting what threatens its family (its kakugan showing, if it has one), an untaught one running to a
 * parent; its lines now and then, and a greeting when a parent comes back. Every client near is told what it needs ({@link S2CChild}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class ChildBrain {
    public static final class Mind {
        ChildStage stage;
        UUID carrier;
        int target;
        boolean turned;
        long lastHit = Long.MIN_VALUE;
        long nextLine;
        long parentAway = Long.MIN_VALUE;
        boolean dirty = true;
        long company;
        /** 1.3.20: how it is held (ChildRules.RIDE_*). */
        int ride;
        /** 1.3.20: its moment with a parent: what, since when, with whom (a player or its NPC parent); one it walks up to them for first. */
        ChildMoment moment = ChildMoment.NONE;
        long momentAt = Long.MIN_VALUE;
        UUID partner;
        ChildMoment pending = ChildMoment.NONE;
        UUID pendingWith;
        long pendingUntil = Long.MIN_VALUE;
        /** When it may next start a moment of its own. */
        long nextOwn = Long.MIN_VALUE;
    }

    private ChildBrain() {
    }

    public static void tick(ChildNpc npc) {
        if (!(npc.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        long now = level.m_46467_();
        Children.Child child = Children.get(level, npc.childId());
        if (child == null) {
            npc.m_146870_(); // a body of no one's child
            return;
        }
        String key = npc.uniqueKey();
        Mind mind = npc.mind;
        if (mind == null) {
            BlockPos first = npc.home() != null ? npc.home() : npc.settle();
            if (!UniqueNpcs.claim(level, key, npc.m_20148_(), first)) {
                npc.m_146870_();
                return;
            }
            mind = new Mind();
            npc.mind = mind;
            mind.nextLine = now + 400L + npc.m_217043_().m_188503_(600);
            mind.company = now;
        }
        BlockPos home = npc.home() == null ? npc.settle() : npc.home();
        if (now % 200L == 11L) {
            if (!UniqueNpcs.claim(level, key, npc.m_20148_(), home)) {
                npc.m_146870_();
                return;
            }
            BlockPos origin = npc.origin();
            boolean away = origin != null && Math.pow(origin.m_123341_() - home.m_123341_(), 2) + Math.pow(origin.m_123342_() - home.m_123342_(), 2)
                + Math.pow(origin.m_123343_() - home.m_123343_(), 2) > 36.0;
            UniqueNpcs.report(level, key, origin, away, mind.company);
            if (away && mind.carrier == null && now - mind.company >= UniqueNpc.GOES_HOME) {
                npc.goHome();
                mind.company = now;
            }
        }
        // Grown.
        ChildStage stage = child.stage(now);
        if (stage != mind.stage) {
            boolean first = mind.stage == null;
            mind.stage = stage;
            npc.stage = stage;
            npc.m_6210_();
            npc.m_21051_(net.minecraft.world.entity.ai.attributes.Attributes.f_22276_).m_22100_(stage.health);
            if (npc.m_21223_() > stage.health) {
                npc.m_21153_(stage.health);
            }
            if (!stage.carried()) {
                putDown(npc, mind);
            }
            if (!first) {
                tellParents(level, child, "§b" + ChildLines.grown(stage, child.name));
            }
            mind.dirty = true;
        }
        LivingEntity carrier = mind.carrier == null ? null : carrierOf(level, mind.carrier);
        if (mind.carrier != null && (carrier == null || !carrier.m_6084_() || carrier.m_9236_() != level || carrier.m_20280_(npc) > 16.0 * 16.0)) {
            putDown(npc, mind);
            carrier = null;
        }
        moments(npc, mind, child, level, now, stage);
        if (carrier != null) {
            carry(npc, carrier, mind.ride);
            mind.company = now;
        } else if (busyStanding(mind, now)) {
            // 1.3.20: in a moment with a parent: standing to it, facing them (or walking up to them first).
            stand(npc, mind, level, now);
        } else if (stage.sits()) {
            // A baby sits where it is put.
            npc.m_21573_().m_26573_();
            npc.m_20256_(new Vec3(0.0, npc.m_20184_().f_82480_, 0.0));
        } else {
            LivingEntity threat = threat(npc, child, level);
            if (threat != null && ChildRules.fights(stage, child.skill)) {
                fight(npc, mind, child, threat, level, now);
            } else {
                if (mind.turned) {
                    mind.turned = false;
                    mind.target = 0;
                    mind.dirty = true;
                }
                ServerPlayer leader = npc.following() == null ? null : player(level, npc.following());
                if (threat != null) {
                    // Untaught (or too little): it runs to a parent.
                    ServerPlayer parent = nearestParent(npc, child, level, 24.0);
                    if (parent != null) {
                        npc.m_21573_().m_5624_(parent, 1.3);
                    }
                    if (now % 60L == 0L && parent != null) {
                        say(parent, child, ChildLines.scared(stage, ChildLines.call(stage, isMother(child, parent)), npc.m_217043_().m_188503_(100)));
                    }
                } else if (leader != null && leader.m_9236_() == level) {
                    follow(npc, leader);
                    mind.company = now;
                }
            }
        }
        lines(npc, mind, child, level, now, stage);
        if (mind.dirty) {
            mind.dirty = false;
            ControlNetwork.broadcastAround(npc, message(npc, mind, child));
        }
    }

    // ---------------------------------------------------------------- carried

    /**
     * Held in the carrier's arms: sitting on their forearms at their chest ({@link ChildRules#CARRY_UP}, its hips there), facing them, a
     * little in front ({@link ChildRules#CARRY_AHEAD}). 1.3.15: no gravity while held (it fell between ticks, and was drawn by their feet),
     * and it neither pushes nor is pushed ({@code ChildNpc.carriedBy}); any living carrier (a player, or its NPC parent).
     */
    private static void carry(ChildNpc npc, LivingEntity carrier, int ride) {
        double scale = ChildNpc.carrySize(carrier);
        double yaw = Math.toRadians(carrier.f_20883_);
        // 1.3.20: up on their shoulders (facing their way), or held out ahead at arm's length to be spun.
        double ahead = (ride == ChildRules.RIDE_SHOULDERS ? -ChildRules.SHOULDERS_BACK : ride == ChildRules.RIDE_SPIN ? ChildRules.SPIN_AHEAD
            : ChildRules.CARRY_AHEAD) * scale;
        double up = (ride == ChildRules.RIDE_SHOULDERS ? ChildRules.SHOULDERS_UP : ride == ChildRules.RIDE_SPIN ? ChildRules.SPIN_UP : ChildRules.CARRY_UP)
            * scale;
        Vec3 at = carrier.m_20182_().m_82520_(-Math.sin(yaw) * ahead, up, Math.cos(yaw) * ahead);
        npc.m_21573_().m_26573_();
        npc.m_20242_(true);
        npc.carriedBy = carrier.m_19879_();
        npc.m_6021_(at.f_82479_, at.f_82480_, at.f_82481_);
        npc.m_20256_(Vec3.f_82478_);
        npc.f_19789_ = 0.0F;
        float facing = carrier.f_20883_ + (ride == ChildRules.RIDE_SHOULDERS ? 0.0F : 180.0F);
        npc.m_146922_(facing);
        npc.f_20883_ = facing;
        npc.f_20885_ = facing;
    }

    /** The one {@code id} names: a player, or (1.3.15) any living body in the world (its NPC parent). */
    static LivingEntity carrierOf(ServerLevel level, UUID id) {
        ServerPlayer player = player(level, id);
        if (player != null) {
            return player;
        }
        return level.m_8791_(id) instanceof LivingEntity living ? living : null;
    }

    static void pickUp(ChildNpc npc, LivingEntity carrier) {
        pickUp(npc, carrier, ChildRules.RIDE_ARMS);
    }

    /** 1.3.20: held by {@code carrier} as {@code ride} has it (in their arms, on their shoulders, held out to spin). */
    static void pickUp(ChildNpc npc, LivingEntity carrier, int ride) {
        if (npc.mind != null) {
            npc.mind.carrier = carrier.m_20148_();
            npc.mind.ride = ride;
            npc.mind.dirty = true;
        }
    }

    static void putDown(ChildNpc npc, Mind mind) {
        if (mind.carrier != null) {
            mind.carrier = null;
            mind.ride = ChildRules.RIDE_ARMS;
            mind.dirty = true;
            npc.settle();
        }
        npc.m_20242_(false);
        npc.carriedBy = 0;
    }

    static boolean carried(ChildNpc npc) {
        return npc.mind != null && npc.mind.carrier != null;
    }

    // ---------------------------------------------------------------- 1.3.20: moments with a parent

    /**
     * {@code moment} with {@code partner} (a parent: a player, or its NPC parent): begun now if it can be (held already, or done where it
     * is), else it walks up to them first ({@link #stand}). A moment done in their arms picks it up (TOSS) or lifts it out to spin (SPIN).
     * False if it can't have that moment now (its stage, a fight, a moment already under way).
     */
    public static boolean start(ChildNpc npc, ChildMoment moment, LivingEntity partner, long now) {
        Mind mind = npc.mind;
        if (mind == null || mind.stage == null || !moment.fits(mind.stage) || mind.target != 0 || busy(mind, now) || !partner.m_6084_()) {
            return false;
        }
        boolean holds = partner.m_20148_().equals(mind.carrier);
        if (moment == ChildMoment.TOSS) {
            if (mind.carrier != null && !holds) {
                return false;
            }
            pickUp(npc, partner, ChildRules.RIDE_ARMS);
        } else if (moment == ChildMoment.SPIN) {
            if (mind.carrier != null && !holds) {
                return false;
            }
            pickUp(npc, partner, ChildRules.RIDE_SPIN);
        } else if (mind.carrier != null && moment != ChildMoment.PAT) {
            if (!holds) {
                return false; // in someone else's arms
            }
            putDown(npc, mind); // held, it can be patted; for the rest it is set down on its feet first
        }
        // A baby can't walk: it stays where it sits (turned to them) and they come to it.
        if (moment.stand > 0.0 && mind.carrier == null && !mind.stage.sits()
            && Math.sqrt(npc.m_20280_(partner)) > moment.stand * ChildNpc.carrySize(partner) + 0.9) {
            mind.pending = moment;
            mind.pendingWith = partner.m_20148_();
            mind.pendingUntil = now + 120L;
            return true;
        }
        begin(npc, mind, moment, partner, now);
        return true;
    }

    private static void begin(ChildNpc npc, Mind mind, ChildMoment moment, LivingEntity partner, long now) {
        mind.moment = moment;
        mind.momentAt = now;
        mind.partner = partner.m_20148_();
        mind.pending = ChildMoment.NONE;
        mind.pendingWith = null;
        mind.dirty = true;
        if (partner instanceof ServerPlayer player && npc.m_9236_() instanceof ServerLevel level) {
            Children.Child child = Children.get(level, npc.childId());
            if (child != null) {
                say(player, child, ChildLines.moment(moment, mind.stage, ChildLines.call(mind.stage, isMother(child, player)),
                    npc.m_217043_().m_188503_(1000)));
            }
        }
        if (moment == ChildMoment.FLOWER) {
            // A flower in its hand to show them; given a little later (moments()).
            String[] flowers = {"minecraft:poppy", "minecraft:dandelion", "minecraft:cornflower", "minecraft:oxeye_daisy", "minecraft:azure_bluet",
                "minecraft:allium", "minecraft:pink_tulip", "minecraft:lily_of_the_valley"};
            npc.m_21008_(net.minecraft.world.InteractionHand.MAIN_HAND, EtoBrain.stack(flowers[npc.m_217043_().m_188503_(flowers.length)]));
        }
    }

    /** True while it is in a moment, or walking up to a parent for one. */
    static boolean busy(Mind mind, long now) {
        return mind.moment != ChildMoment.NONE && now - mind.momentAt < mind.moment.ticks || mind.pending != ChildMoment.NONE;
    }

    /** True while a moment (or the walk up to one) has it on its feet facing its parent (a baby: sitting, turned to them). */
    private static boolean busyStanding(Mind mind, long now) {
        return busy(mind, now) && (mind.pending != ChildMoment.NONE || mind.moment.stand > 0.0 || mind.stage != null && mind.stage.sits());
    }

    /** The child having a moment with {@code partner} now (an entity id), or null. */
    static boolean withPartner(ChildNpc npc, LivingEntity partner, long now) {
        Mind mind = npc.mind;
        return mind != null && busy(mind, now) && (partner.m_20148_().equals(mind.partner) || partner.m_20148_().equals(mind.pendingWith));
    }

    /** Each tick: the walk up to a moment, the moment's end; and now and then one of its own, near a parent. */
    private static void moments(ChildNpc npc, Mind mind, Children.Child child, ServerLevel level, long now, ChildStage stage) {
        if (mind.pending != ChildMoment.NONE) {
            LivingEntity partner = mind.pendingWith == null ? null : carrierOf(level, mind.pendingWith);
            if (partner == null || !partner.m_6084_() || now > mind.pendingUntil || mind.target != 0 || partner.m_20280_(npc) > 24.0 * 24.0) {
                mind.pending = ChildMoment.NONE;
                mind.pendingWith = null;
            } else if (Math.sqrt(npc.m_20280_(partner)) <= mind.pending.stand * ChildNpc.carrySize(partner) + 0.9) {
                begin(npc, mind, mind.pending, partner, now);
            }
            return;
        }
        if (mind.moment != ChildMoment.NONE && now - mind.momentAt >= mind.moment.ticks) {
            ChildMoment was = mind.moment;
            mind.moment = ChildMoment.NONE;
            mind.dirty = true;
            if (was == ChildMoment.FLOWER && !npc.m_21205_().m_41619_()) {
                gift(npc, mind.partner == null ? null : carrierOf(level, mind.partner));
            }
            if (was == ChildMoment.SPIN) {
                putDown(npc, mind); // set back down on its feet
            }
            return;
        }
        if (mind.moment == ChildMoment.FLOWER && now - mind.momentAt >= 30L && !npc.m_21205_().m_41619_()) {
            LivingEntity to = mind.partner == null ? null : carrierOf(level, mind.partner);
            gift(npc, to);
        }
        if (mind.moment != ChildMoment.NONE) {
            LivingEntity partner = mind.partner == null ? null : carrierOf(level, mind.partner);
            if (partner == null || !partner.m_6084_() || partner.m_20280_(npc) > 6.0 * 6.0 || mind.target != 0) {
                if (mind.moment == ChildMoment.SPIN) {
                    putDown(npc, mind);
                }
                if (mind.moment == ChildMoment.FLOWER) {
                    gift(npc, null); // they went off: it keeps its flower to itself (its hand empty again)
                }
                mind.moment = ChildMoment.NONE;
                mind.dirty = true;
            }
            return;
        }
        // Of its own, now and then, a parent (a player) near and nothing else going on: a greeting hug, arms up, a wave, a clap, a flower.
        if (now < mind.nextOwn || mind.carrier != null || mind.target != 0 || now % 20L != 7L) {
            return;
        }
        ServerPlayer parent = nearestParent(npc, child, level, 6.0);
        if (parent == null) {
            return;
        }
        if (npc.m_217043_().m_188500_() < 0.12) {
            ChildMoment[] own = ChildMoment.OWN;
            ChildMoment pick = own[npc.m_217043_().m_188503_(own.length)];
            if (pick.fits(stage) && start(npc, pick, parent, now)) {
                mind.nextOwn = now + 1200L + npc.m_217043_().m_188503_(1800);
            }
        }
    }

    /** 1.3.20: back from far off: it runs to them and hugs them (a baby claps). */
    static void welcome(ChildNpc npc, Mind mind, ServerPlayer parent, long now) {
        if (mind.stage == null || busy(mind, now) || mind.carrier != null) {
            return;
        }
        if (ChildMoment.HUG.fits(mind.stage)) {
            start(npc, ChildMoment.HUG, parent, now);
        } else if (ChildMoment.CLAP.fits(mind.stage)) {
            start(npc, ChildMoment.CLAP, parent, now);
        }
        mind.nextOwn = Math.max(mind.nextOwn, now + 600L);
    }

    /** In a moment that has it on its feet: it walks up to its parent, then stands its moment's distance off them, facing them. */
    private static void stand(ChildNpc npc, Mind mind, ServerLevel level, long now) {
        UUID with = mind.pending != ChildMoment.NONE ? mind.pendingWith : mind.partner;
        LivingEntity partner = with == null ? null : carrierOf(level, with);
        if (partner == null) {
            return;
        }
        if (mind.pending != ChildMoment.NONE) {
            npc.m_21573_().m_5624_(partner, 1.2);
            return;
        }
        npc.m_21573_().m_26573_();
        if (mind.stage != null && mind.stage.sits() || mind.moment.stand <= 0.0) {
            // Where it sits (a baby), or a moment done where it stands: only turned to them.
            Vec3 to = partner.m_20182_().m_82546_(npc.m_20182_());
            if (to.f_82479_ * to.f_82479_ + to.f_82481_ * to.f_82481_ > 1.0E-4) {
                float face = (float)Math.toDegrees(Math.atan2(-to.f_82479_, to.f_82481_));
                npc.m_146922_(face);
                npc.f_20883_ = face;
                npc.f_20885_ = face;
            }
            npc.m_20256_(new Vec3(0.0, npc.m_20184_().f_82480_, 0.0));
            return;
        }
        double want = mind.moment.stand * ChildNpc.carrySize(partner);
        Vec3 from = partner.m_20182_();
        Vec3 way = npc.m_20182_().m_82546_(from);
        way = new Vec3(way.f_82479_, 0.0, way.f_82481_);
        if (way.m_82556_() < 1.0E-4) {
            double yaw = Math.toRadians(partner.f_20883_);
            way = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        }
        way = way.m_82541_();
        Vec3 at = from.m_82549_(way.m_82490_(want));
        Vec3 step = at.m_82546_(npc.m_20182_());
        if (Math.abs(step.f_82479_) + Math.abs(step.f_82481_) > 0.02) {
            // Eased toward its spot over a few ticks (a short step each, only into open air).
            Vec3 next = npc.m_20182_().m_82520_(step.f_82479_ * 0.5, 0.0, step.f_82481_ * 0.5);
            if (level.m_45756_(npc, npc.m_20191_().m_82386_(next.f_82479_ - npc.m_20185_(), 0.0, next.f_82481_ - npc.m_20189_()))) {
                npc.m_6021_(next.f_82479_, npc.m_20186_(), next.f_82481_);
            }
        }
        float yaw = (float)Math.toDegrees(Math.atan2(way.f_82479_, -way.f_82481_));
        npc.m_146922_(yaw);
        npc.f_20883_ = yaw;
        npc.f_20885_ = yaw;
        npc.m_20256_(new Vec3(0.0, npc.m_20184_().f_82480_, 0.0));
    }

    /** FLOWER: the flower in its hand given into theirs (dropped at their feet if they have no room); its hand empty after. */
    private static void gift(ChildNpc npc, LivingEntity partner) {
        net.minecraft.world.item.ItemStack stack = npc.m_21205_();
        npc.m_21008_(net.minecraft.world.InteractionHand.MAIN_HAND, EtoBrain.stack(null));
        if (!(partner instanceof ServerPlayer player) || stack.m_41619_()) {
            return;
        }
        if (!player.m_36356_(stack)) {
            player.m_36176_(stack, false);
        }
    }

    // ---------------------------------------------------------------- following

    private static void follow(ChildNpc npc, ServerPlayer leader) {
        double far = npc.m_20280_(leader);
        if (far > ChildRules.FOLLOW_TELEPORT * ChildRules.FOLLOW_TELEPORT) {
            Vec3 at = leader.m_20182_();
            npc.m_6021_(at.f_82479_, at.f_82480_, at.f_82481_);
            npc.m_21573_().m_26573_();
        } else if (far > ChildRules.FOLLOW_FROM * ChildRules.FOLLOW_FROM) {
            npc.m_21573_().m_5624_(leader, 1.15);
        } else if (far < ChildRules.FOLLOW_STOP * ChildRules.FOLLOW_STOP) {
            npc.m_21573_().m_26573_();
        }
        // Where it is told to wait is home; following, home is wherever it is.
        npc.m_21446_(new BlockPos((int)Math.floor(npc.m_20185_()), (int)Math.floor(npc.m_20186_()), (int)Math.floor(npc.m_20189_())), 6);
    }

    // ---------------------------------------------------------------- fighting

    /** Something hostile after it or its family, close: a monster with one of them as its target, or right on top of them. */
    private static LivingEntity threat(ChildNpc npc, Children.Child child, ServerLevel level) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity body : level.m_6443_(LivingEntity.class, npc.m_20191_().m_82400_(ChildRules.GUARD), entity -> entity instanceof Enemy && entity.m_6084_())) {
            LivingEntity target = body instanceof Mob mob ? mob.m_5448_() : null;
            boolean family = target == npc || target instanceof ServerPlayer player && child.parent(Pregnancies.player(player.m_20148_()));
            double distance = body.m_20280_(npc);
            if ((family || distance < 3.0 * 3.0) && distance < bestDistance) {
                best = body;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static void fight(ChildNpc npc, Mind mind, Children.Child child, LivingEntity threat, ServerLevel level, long now) {
        boolean ghoul = child.genome().ghoul();
        if (!mind.turned || mind.target != threat.m_19879_()) {
            mind.turned = ghoul;
            mind.target = threat.m_19879_();
            mind.dirty = true;
            ServerPlayer parent = nearestParent(npc, child, level, 16.0);
            if (parent != null) {
                say(parent, child, ChildLines.fight(mind.stage, npc.m_217043_().m_188503_(100)));
            }
        }
        npc.m_21563_().m_24960_(threat, 30.0F, 30.0F);
        double reach = ChildRules.REACH + threat.m_20205_() * 0.5;
        if (npc.m_20280_(threat) > reach * reach) {
            npc.m_21573_().m_5624_(threat, 1.25);
            return;
        }
        npc.m_21573_().m_26573_();
        if (mind.lastHit == Long.MIN_VALUE || now - mind.lastHit >= ChildRules.cooldown(child.skill)) {
            mind.lastHit = now;
            npc.m_21011_(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            threat.m_6469_(npc.m_269291_().m_269333_(npc), ChildRules.damage(mind.stage, child.skill, ghoul));
        }
    }

    // ---------------------------------------------------------------- what it says

    private static void lines(ChildNpc npc, Mind mind, Children.Child child, ServerLevel level, long now, ChildStage stage) {
        ServerPlayer parent = nearestParent(npc, child, level, 8.0);
        if (parent == null) {
            if (mind.parentAway == Long.MIN_VALUE) {
                mind.parentAway = now;
            }
            return;
        }
        mind.company = now;
        int seed = npc.m_217043_().m_188503_(1000);
        String call = ChildLines.call(stage, isMother(child, parent));
        if (mind.parentAway != Long.MIN_VALUE && now - mind.parentAway >= 1200L) {
            say(parent, child, ChildLines.greet(stage, call, seed));
            mind.nextLine = now + 600L + npc.m_217043_().m_188503_(900);
            welcome(npc, mind, parent, now); // 1.3.20: it runs to them
        }
        mind.parentAway = Long.MIN_VALUE;
        if (now >= mind.nextLine && mind.target == 0) {
            say(parent, child, ChildLines.idle(stage, call, seed));
            mind.nextLine = now + 900L + npc.m_217043_().m_188503_(1500);
        }
    }

    static void say(ServerPlayer player, Children.Child child, String line) {
        player.m_5661_(Component.m_237113_("§b" + child.name + "§r: " + line), false);
    }

    private static void tellParents(ServerLevel level, Children.Child child, String text) {
        for (String parent : new String[] {child.mother, child.father}) {
            ServerPlayer player = Pregnancies.playerOf(level, parent);
            if (player != null) {
                player.m_5661_(Component.m_237113_(text), false);
            }
        }
    }

    static boolean isMother(Children.Child child, ServerPlayer player) {
        return child.mother.equals(Pregnancies.player(player.m_20148_()));
    }

    static ServerPlayer nearestParent(ChildNpc npc, Children.Child child, ServerLevel level, double range) {
        ServerPlayer best = null;
        for (String parent : new String[] {child.mother, child.father}) {
            ServerPlayer player = Pregnancies.playerOf(level, parent);
            if (player != null && player.m_9236_() == level && player.m_20280_(npc) <= range * range && (best == null || player.m_20280_(npc) < best.m_20280_(npc))) {
                best = player;
            }
        }
        return best;
    }

    static ServerPlayer player(ServerLevel level, UUID id) {
        return id == null || level.m_7654_() == null ? null : level.m_7654_().m_6846_().m_11259_(id);
    }

    static S2CChild message(ChildNpc npc, Mind mind, Children.Child child) {
        LivingEntity carrier = mind == null || mind.carrier == null ? null : carrierOf((ServerLevel)npc.m_9236_(), mind.carrier);
        LivingEntity partner = mind == null || mind.partner == null || mind.moment == ChildMoment.NONE ? null
            : carrierOf((ServerLevel)npc.m_9236_(), mind.partner);
        return new S2CChild(npc.m_19879_(), child.name, child.genome, (mind == null || mind.stage == null ? child.stage(npc.m_9236_().m_46467_()) : mind.stage)
            .ordinal(), carrier == null ? 0 : carrier.m_19879_(), mind != null && mind.turned, mind == null ? 0 : mind.ride,
            (mind == null ? ChildMoment.NONE : mind.moment).ordinal(), mind == null ? Long.MIN_VALUE : mind.momentAt, partner == null ? 0 : partner.m_19879_());
    }

    /** A player starts seeing a child: they are told how to draw it. */
    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ChildNpc npc && event.getEntity() instanceof ServerPlayer viewer && npc.m_9236_() instanceof ServerLevel level) {
            Children.Child child = Children.get(level, npc.childId());
            if (child != null) {
                ControlNetwork.sendTo(viewer, message(npc, npc.mind, child));
            }
        }
    }

    /** A parent's blow (a swing, a kagune's sweep) never lands on their own child. */
    @SubscribeEvent
    public static void attacked(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (event.getEntity() instanceof ChildNpc npc && event.getSource() != null && event.getSource().m_7639_() instanceof ServerPlayer player
            && npc.m_9236_() instanceof ServerLevel level) {
            Children.Child child = Children.get(level, npc.childId());
            if (child != null && child.parent(Pregnancies.player(player.m_20148_()))) {
                event.setCanceled(true);
            }
        }
    }

    /** It died: the world remembers (its body comes back home a day later), its parents are told. */
    @SubscribeEvent
    public static void died(LivingDeathEvent event) {
        if (event.getEntity() instanceof ChildNpc npc && npc.m_9236_() instanceof ServerLevel level) {
            UniqueNpcs.died(level, npc.uniqueKey(), npc.m_20148_());
            Children.Child child = Children.get(level, npc.childId());
            if (child != null) {
                tellParents(level, child, "§c" + child.name + " was hurt badly. They'll be back home tomorrow.");
            }
        }
    }
}
