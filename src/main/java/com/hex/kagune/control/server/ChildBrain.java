package com.hex.kagune.control.server;

import com.hex.kagune.control.family.ChildLines;
import com.hex.kagune.control.family.ChildMoment;
import com.hex.kagune.control.family.ChildRules;
import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CChild;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.FinalSight;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.FightRules;
import com.hex.kagune.control.npc.UniqueNpc;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
        /** 1.3.24: when it last got at what it fights (close to it): given up only after a long while without. */
        long reachedAt = Long.MIN_VALUE;
        long lastHit = Long.MIN_VALUE;
        long nextLine;
        long parentAway = Long.MIN_VALUE;
        boolean dirty = true;
        long company;
        /** 1.3.32: whether the hand holding its own had Shift down last tick (a tap of it lets go, as with a companion). */
        boolean holdShift = true;
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
        /** 1.3.21: when it was last hurt (it wakes), the bed it lies in (or goes to), a nap's end, who sent it to bed (to tuck it in). */
        long hurtAt = Long.MIN_VALUE;
        BlockPos bed;
        long napUntil = Long.MIN_VALUE;
        UUID tuckBy;
        /** 1.3.21: the food it is given (eaten through its FEED moment). */
        net.minecraft.world.item.ItemStack food;
        /** 1.3.29: when a parent came back after a long while away (it missed them), or never. */
        long reunited = Long.MIN_VALUE;
        /** 1.3.43: its kagune in a fight (ChildKagune): out or not, its lash, its volley, its shots in flight. */
        final ChildKagune.Fight kagune = new ChildKagune.Fight();
        /** 1.3.43: it fought with its kagune this tick (else the kagune goes back in). */
        boolean kaguneFought;
        /**
         * 1.3.49 (ParentDuty): a word or a shout of its own or about it (a remark in a fight, tag: each on its tick) and when that queue is
         * free; until when it holds back from fighting (a parent stopped it); when a parent last came after it; the parent walking it home (a
         * UUID) and until when; since when it has been out of its home; when a parent last had a word about its kagune. 1.3.53: a talk it
         * stays for is its {@code conversation} (the last one it was in, {@code lastTalk}).
         */
        final java.util.ArrayDeque<ParentDuty.Said> talk = new java.util.ArrayDeque<>();
        long talkFree = Long.MIN_VALUE;
        long calmedUntil = Long.MIN_VALUE;
        long fetchedAt = Long.MIN_VALUE / 2;
        Conversation conversation;
        Conversation lastTalk;
        UUID escort;
        long escortUntil = Long.MIN_VALUE;
        long outsideSince = Long.MIN_VALUE;
        long remarkedAt = Long.MIN_VALUE / 2;
        /**
         * 1.3.51 (SiblingLife): when it last had a moment with a brother or sister; the one it plays tag with (an entity id, 0 none),
         * whether it is it (chasing), until when, when it was last tagged; the little one it is going after (an entity id) and until when,
         * whether it got to it; when it last had a word about a sibling's kagune.
         */
        long siblingAt = Long.MIN_VALUE / 2;
        int tagWith;
        boolean chasing;
        long tagUntil = Long.MIN_VALUE;
        long taggedAt = Long.MIN_VALUE / 2;
        int minding;
        long mindingUntil = Long.MIN_VALUE;
        boolean mindingReached;
        /** 1.3.53: its word with the little one it went after (they walk home once it is said). */
        Conversation mindingTalk;
        /**
         * 1.3.58 (SiblingLife): hide and seek: with whom (an entity id; 0 none), whether it is the one who seeks, the seeker's part (0
         * counting, 1 looking, 2 over) and since when, when the game began, whether the seeker found it; the hider's spot (null: not chosen
         * yet), since when it has been on its way there, whether it is hidden, when it last giggled; the seeker's next place to look, since
         * when, when it last said so.
         */
        int hideWith;
        boolean seeker;
        int hidePart;
        long hideAt;
        long hideBegan;
        boolean hideFound;
        Vec3 hideSpot;
        long hideSetOff;
        boolean hidden;
        long hintAt = Long.MIN_VALUE / 2;
        Vec3 seekTo;
        long seekToAt;
        long seekSaidAt = Long.MIN_VALUE / 2;
        /** 1.3.57: what it does with its body as it says a line in a talk (TalkCues), since when. */
        com.hex.kagune.control.family.ChildCue cue = com.hex.kagune.control.family.ChildCue.NONE;
        long cueAt = Long.MIN_VALUE;
        long siblingRemarkAt = Long.MIN_VALUE / 2;
        /** 1.3.59: when it last played with a baby brother or sister. */
        long babyAt = Long.MIN_VALUE / 2;
        /** 1.3.65: a parent in War (or the Dragon) it hides from (null: none), until when, when it last said something about it. */
        UUID dreadOf;
        long dreadUntil = Long.MIN_VALUE;
        long dreadSaidAt = Long.MIN_VALUE / 2;
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
        HalfBloodServer.tick(npc, now); // 2.2.1: a dash under way
        if (now % 100L == 7L && !child.genome().kaguneKnown()) {
            Children.genome(level, child); // 1.3.43: a child from before: its kagune worked out from its parents
            if (mind != null) {
                mind.dirty = true;
            }
        }
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
        // 1.3.21: its family's home is its home (unless it follows someone, or was told to wait elsewhere): it roams all of it.
        Households.Home house = Households.forChild(level, child);
        if (house != null && npc.following() == null && !npc.waiting() && mind.carrier == null && !house.at(home)) {
            npc.homeAt(house.pos());
            home = house.pos();
            UniqueNpcs.claim(level, key, npc.m_20148_(), home);
        }
        boolean atHouse = house != null && house.at(home);
        if (npc.following() == null && mind.carrier == null) {
            // 2.0.7 (the owner: never teleported home for being far; out past its home by day, home by night): its leash wider by day (by its
            // age), its home's at night; far out, it walks back. Put home only stuck inside blocks, or out at night getting no nearer (unseen).
            boolean night = HomeLife.night(level.m_46468_(), true);
            int leash = atHouse ? com.hex.kagune.control.family.HomeRules.leash(house.radius() - 2, night,
                com.hex.kagune.control.family.HomeRules.roam(child.stage(now))) : 8;
            npc.m_21446_(home, leash);
            Vec3 homeAt = new Vec3(home.m_123341_() + 0.5, home.m_123342_() - 1.0, home.m_123343_() + 0.5);
            boolean away = atHouse && !npc.waiting() && npc.m_20238_(homeAt) > (leash + 4.0) * (leash + 4.0);
            if (npc.carriedBy == 0 && Homeward.bugged(npc, level, homeAt, away, now)) {
                Homeward.put(npc, homeAt);
            }
        }
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
                npc.sendHome(); // 2.0.7: it walks home (never put there)
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
            // 1.3.21: as strong as its parents: their might makes its health. 1.3.47: their health too (ChildRules.grownHealth).
            float most = ChildRules.health(stage, Children.might(level, child), Children.vitality(level, child));
            com.hex.kagune.control.family.Genome born = Children.genome(level, child);
            boolean half = born != null && born.half();
            if (half) {
                // 2.2.1: a ghoul's and a human's child: far harder to kill, and quicker.
                most *= com.hex.kagune.control.family.HalfBlood.HEALTH;
            }
            npc.m_21051_(net.minecraft.world.entity.ai.attributes.Attributes.f_22279_).m_22100_(0.3 * (half ? com.hex.kagune.control.family.HalfBlood.SPEED : 1.0));
            float was = Math.max(1.0F, npc.m_21233_());
            float share = Math.max(0.0F, Math.min(1.0F, npc.m_21223_() / was));
            npc.m_21051_(net.minecraft.world.entity.ai.attributes.Attributes.f_22276_).m_22100_(most);
            // Grown into a new stage: its new health whole; its first tick: as hurt as it was (1.3.47: the same share of its new most).
            npc.m_21153_(first ? Math.max(1.0F, share * most) : most);
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
        // 1.3.32 (the owner: there was no letting go but talking to it): hand in hand with a player, a tap of Shift lets go.
        if (carrier instanceof ServerPlayer holder && mind.ride == ChildRules.RIDE_HAND) {
            boolean shift = holder.m_6047_();
            if (shift && !mind.holdShift) {
                putDown(npc, mind);
                carrier = null;
                holder.m_5661_(Component.m_237113_("\u00a7b" + child.name + "\u00a77 lets go of your hand."), true);
            }
            mind.holdShift = shift;
        } else {
            mind.holdShift = true; // a new hold waits for Shift to be let up before a tap counts
        }
        // 1.3.53: a talk it is in first: it stays for it (Conversation), unless something it would fight comes at it.
        boolean talking = Conversation.going(npc, level, now);
        if (talking && mind.carrier == null) {
            LivingEntity danger = threat(npc, mind, child, level, now);
            if (danger != null && ChildRules.fights(stage, child.skill) && now >= mind.calmedUntil) {
                Conversation.end(npc, com.hex.kagune.control.family.Talks.End.FIGHT, level, now);
                talking = false;
            }
        }
        if (!talking) {
            moments(npc, mind, child, level, now, stage);
        }
        // 1.3.32 (the owner: after the airplane the child hovered in midair): a moment that ended just now may have set it down (the
        // airplane does); it was then carried again this same tick from the carrier looked up before, its gravity off for good.
        if (mind.carrier == null) {
            carrier = null;
        }
        if (carrier == null && npc.carriedBy == 0) {
            npc.m_20242_(false); // never left floating (nor saved floating: a world saved mid-carry comes back with its gravity on)
            EtoBrain.solid(npc, null);
        }
        if (mind.cue == com.hex.kagune.control.family.ChildCue.COWER && now >= mind.dreadUntil) {
            mind.cue = com.hex.kagune.control.family.ChildCue.NONE; // 1.3.65: no longer hiding: it stands up straight again
            mind.dreadOf = null;
            mind.dirty = true;
        }
        if (carrier != null) {
            carry(npc, carrier, mind.ride);
            mind.company = now;
        } else if (now < mind.dreadUntil && cower(npc, mind, child, level, now)) {
            // 1.3.65: a parent in War near: it hides behind the other parent (or keeps off), peeking out at them.
            mind.company = now;
        } else if (talking && Conversation.hold(npc, level, now)) {
            // 1.3.53: in a talk: standing, turned to whoever speaks (or walking up to the one it talks to first); its fight off meanwhile.
            if (mind.turned || mind.target != 0) {
                mind.turned = false;
                mind.target = 0;
                mind.dirty = true;
            }
        } else if (busyStanding(mind, now)) {
            // 1.3.20: in a moment with a parent: standing to it, facing them (or walking up to them first).
            stand(npc, mind, level, now);
        } else if (ChildTalk.talker(npc, level) != null && !npc.m_5803_()) {
            // 1.3.39 (the owner: talked to, they walked away): it stands still and looks at whoever is talking to it.
            ServerPlayer talker = ChildTalk.talker(npc, level);
            npc.m_21573_().m_26573_();
            npc.m_20256_(new Vec3(0.0, npc.m_20184_().f_82480_, 0.0));
            npc.m_21563_().m_24960_(talker, 30.0F, 30.0F);
        } else if (stage.sits()) {
            // A baby sits where it is put.
            npc.m_21573_().m_26573_();
            npc.m_20256_(new Vec3(0.0, npc.m_20184_().f_82480_, 0.0));
        } else if (ParentDuty.led(npc, mind, level, now)) {
            // 1.3.49: its parent (or a big brother or sister) walks it home at their side.
            if (mind.turned || mind.target != 0) {
                mind.turned = false;
                mind.target = 0;
                mind.dirty = true;
            }
        } else {
            LivingEntity threat = threat(npc, mind, child, level, now);
            if (threat != null && ChildRules.fights(stage, child.skill) && now >= mind.calmedUntil) {
                fight(npc, mind, child, threat, level, now);
            } else {
                // 1.3.24: the fight over, for a human child too (its target was only ever cleared for a ghoul child that had turned: a
                // human child that fought once never had a moment, a bedtime or a word again).
                if (mind.turned || mind.target != 0) {
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
                        say(parent, npc, child, ChildLines.scared(stage, ChildLines.call(stage, isMother(child, parent)), npc.m_217043_().m_188503_(100)));
                    }
                } else if (leader != null && leader.m_9236_() == level) {
                    follow(npc, leader);
                    mind.company = now;
                } else if (SiblingLife.tick(npc, mind, child, level, now)) {
                    // 1.3.51: playing tag with a brother or sister, or walking a little one home.
                    mind.company = now;
                } else if (atHouse && !npc.waiting()) {
                    toBed(npc, mind, child, house, level, now, stage);
                }
            }
        }
        lines(npc, mind, child, level, now, stage);
        if (mind.dirty) {
            mind.dirty = false;
            ControlNetwork.broadcastAround(npc, message(npc, mind, child));
        }
        // 1.3.47: it heals on its own a while after it was last hurt (a ghoul child fast); its most worked out again once its parents are known.
        if (now % 20L == 3L) {
            float most = npc.m_21233_();
            if (npc.m_21223_() < most && npc.m_6084_() && (mind.hurtAt == Long.MIN_VALUE || now - mind.hurtAt >= ChildRules.HEAL_AFTER)) {
                npc.m_5634_(ChildRules.healing(most, child.genome().ghoul()));
            }
            if (child.vitality < 0.0F && now % 200L == 3L && mind.stage != null && Children.vitality(level, child) > 0.0F) {
                mind.stage = null; // its most worked out again next tick, now its parents' health is known
            }
        }
        // 1.3.49: what it and its parents say, each on its tick; how long it has been out of its home (a parent misses it).
        ParentDuty.talk(npc, mind, level, now);
        if (now % 20L == 13L) {
            Households.Home family = Households.forChild(level, child);
            boolean out = family != null && npc.m_20238_(new Vec3(family.pos().m_123341_() + 0.5, family.pos().m_123342_(),
                family.pos().m_123343_() + 0.5)) > (double)family.radius() * family.radius();
            if (!out) {
                mind.outsideSince = Long.MIN_VALUE;
            } else if (mind.outsideSince == Long.MIN_VALUE) {
                mind.outsideSince = now;
            }
        }
        // 1.3.43: its kagune: its shots fly on; out of a fight, it goes back in.
        ChildKagune.tick(npc, mind, mind.kaguneFought, level, now);
        mind.kaguneFought = false;
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
            : ride == ChildRules.RIDE_HAND ? 0.0 : ChildRules.CARRY_AHEAD) * scale;
        double up = (ride == ChildRules.RIDE_SHOULDERS ? ChildRules.SHOULDERS_UP : ride == ChildRules.RIDE_SPIN ? ChildRules.SPIN_UP
            : ride == ChildRules.RIDE_HAND ? 0.0 : ChildRules.CARRY_UP) * scale;
        // 1.3.21: hand in hand, at their left on its own feet.
        double left = ride == ChildRules.RIDE_HAND ? ChildRules.hand(npc.mind == null || npc.mind.stage == null ? ChildStage.CHILD : npc.mind.stage, scale, false)
            .side() : 0.0;
        Vec3 at = carrier.m_20182_().m_82520_(-Math.sin(yaw) * ahead + Math.cos(yaw) * left, up, Math.cos(yaw) * ahead + Math.sin(yaw) * left);
        npc.m_21573_().m_26573_();
        npc.m_20242_(true);
        // 1.3.39 (the owner: held by the hand they suffocated in blocks): no block in its way while it is held, nor choking it.
        npc.f_19794_ = true;
        npc.carriedBy = carrier.m_19879_();
        npc.m_6021_(at.f_82479_, at.f_82480_, at.f_82481_);
        npc.m_20256_(Vec3.f_82478_);
        npc.f_19789_ = 0.0F;
        float facing = carrier.f_20883_ + (ride == ChildRules.RIDE_SHOULDERS || ride == ChildRules.RIDE_HAND ? 0.0F : 180.0F);
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
        HomeLife.wake(npc); // 1.3.21: picked up, it wakes
        if (npc.mind != null) {
            npc.mind.carrier = carrier.m_20148_();
            npc.mind.ride = ride;
            npc.mind.dirty = true;
        }
    }

    static void putDown(ChildNpc npc, Mind mind) {
        LivingEntity held = mind.carrier != null && npc.m_9236_() instanceof ServerLevel at ? carrierOf(at, mind.carrier) : null;
        if (mind.carrier != null) {
            mind.carrier = null;
            mind.ride = ChildRules.RIDE_ARMS;
            mind.dirty = true;
            npc.settle();
            // 1.3.21: set down away from its family's home, it waits right there (never whisked back home); at home, it lives in it.
            if (npc.m_9236_() instanceof ServerLevel level) {
                Households.Home house = Households.forChild(level, Children.get(level, npc.childId()));
                boolean inside = house != null && npc.m_20238_(new Vec3(house.x() + 0.5, house.y() - 1.0, house.z() + 0.5)) <= house.radius() * house.radius();
                npc.waiting(house != null && !inside);
            }
        }
        npc.m_20242_(false);
        npc.carriedBy = 0;
        // 1.3.39: solid again; set down inside a wall, it is put on the feet of whoever held it instead.
        EtoBrain.solid(npc, held);
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
        if (Conversation.in(npc) != null) {
            return false; // 1.3.53: not in the middle of a talk (a player who wants it ends the talk first: Conversation)
        }
        if (mind.hideWith != 0) {
            // 1.3.58: in the middle of hide and seek: a player who wants it ends the game; anyone else waits.
            if (!(partner instanceof ServerPlayer)) {
                return false;
            }
            SiblingLife.stopHide(npc);
        }
        if (npc.m_5803_() != (moment == ChildMoment.TUCK)) {
            return false; // asleep, only tucked in; tucked in, only asleep
        }
        if (moment == ChildMoment.FEED && (mind.food == null || mind.food.m_41619_())) {
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
        if (moment == ChildMoment.TUCK && Math.sqrt(npc.m_20280_(partner)) > TUCK_NEAR) {
            // 1.3.21: asleep, it can't walk to them: it waits for them to come up to its bed (HomeLife ticks it while it sleeps: sleeping()).
            mind.pending = moment;
            mind.pendingWith = partner.m_20148_();
            mind.pendingUntil = now + 300L;
            return true;
        }
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
            if (child != null && !child.parent(Pregnancies.player(player.m_20148_()))) {
                // 2.0.5: with someone who is not its parent (a wave, a high five): its words for a stranger.
                say(player, npc, child, com.hex.kagune.control.family.StrangerLines.moment(moment, mind.stage, npc.m_217043_().m_188503_(1000)));
            } else if (child != null) {
                say(player, npc, child, ChildLines.moment(moment, mind.stage, ChildLines.call(mind.stage, isMother(child, player)),
                    npc.m_217043_().m_188503_(1000)));
            }
        }
        if (moment == ChildMoment.FEED && mind.food != null) {
            npc.m_21008_(net.minecraft.world.InteractionHand.MAIN_HAND, mind.food);
        }
        if (moment == ChildMoment.FLOWER) {
            // A flower in its hand to show them; given a little later (moments()).
            String[] flowers = {"minecraft:poppy", "minecraft:dandelion", "minecraft:cornflower", "minecraft:oxeye_daisy", "minecraft:azure_bluet",
                "minecraft:allium", "minecraft:pink_tulip", "minecraft:lily_of_the_valley"};
            npc.m_21008_(net.minecraft.world.InteractionHand.MAIN_HAND, EtoBrain.stack(flowers[npc.m_217043_().m_188503_(flowers.length)]));
        }
    }

    /** 1.3.21: how close to a sleeping child its parent must come for its tuck-in (blocks, to where it lies). */
    static final double TUCK_NEAR = 2.6;

    /** 1.3.21: a tuck-in waiting on its parent to come up to its bed (its parent walks up: an NPC parent, ParentCare). */
    static boolean tuckPending(ChildNpc npc) {
        return npc.mind != null && npc.mind.pending == ChildMoment.TUCK;
    }

    /**
     * 1.3.21: each tick it lies asleep (its mind is still: its body can't move): a tuck-in waiting on its parent begun once they are close,
     * given up if they are gone, too far, or too long.
     */
    static void sleeping(ChildNpc npc, ServerLevel level, long now) {
        Mind mind = npc.mind;
        if (mind == null) {
            return;
        }
        if (mind.pending == ChildMoment.TUCK) {
            LivingEntity partner = mind.pendingWith == null ? null : carrierOf(level, mind.pendingWith);
            if (partner == null || !partner.m_6084_() || now > mind.pendingUntil || partner.m_20280_(npc) > 24.0 * 24.0) {
                mind.pending = ChildMoment.NONE;
                mind.pendingWith = null;
            } else if (Math.sqrt(npc.m_20280_(partner)) <= TUCK_NEAR) {
                begin(npc, mind, ChildMoment.TUCK, partner, now);
            }
        }
        // Its own tick (where this is sent) is still while it sleeps: a tuck-in begun now is shown from here.
        Children.Child child = mind.dirty ? Children.get(level, npc.childId()) : null;
        if (child != null) {
            mind.dirty = false;
            ControlNetwork.broadcastAround(npc, message(npc, mind, child));
        }
    }

    /** It got up: a tuck-in still waiting is dropped (only for one asleep). */
    static void woke(ChildNpc npc) {
        if (tuckPending(npc)) {
            npc.mind.pending = ChildMoment.NONE;
            npc.mind.pendingWith = null;
        }
    }

    /**
     * 1.3.53: a talk begins: whatever moment it was having (or walking up to) is over, as if its partner had gone off (set back on its
     * feet from a spin, its flower kept, its food finished).
     */
    static void stopMoment(ChildNpc npc) {
        Mind mind = npc.mind;
        if (mind == null) {
            return;
        }
        if (mind.pending != ChildMoment.NONE) {
            mind.pending = ChildMoment.NONE;
            mind.pendingWith = null;
        }
        if (mind.moment != ChildMoment.NONE) {
            if (mind.moment == ChildMoment.SPIN) {
                putDown(npc, mind);
            }
            if (mind.moment == ChildMoment.FLOWER) {
                gift(npc, null);
            }
            if (mind.moment == ChildMoment.FEED) {
                fed(npc, mind);
            }
            mind.moment = ChildMoment.NONE;
            mind.dirty = true;
        }
    }

    /** 1.3.53: true while it stays for a talk (its strolling waits: ChildNpc); 1.3.58: or plays hide and seek. */
    public static boolean held(ChildNpc npc) {
        return Conversation.in(npc) != null || npc.mind != null && npc.mind.hideWith != 0;
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
        if (mind.pending == ChildMoment.TUCK && !npc.m_5803_()) {
            woke(npc); // awake again before its parent came: no tuck-in now
        }
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
            if (was == ChildMoment.FEED) {
                fed(npc, mind);
            }
            if (was == ChildMoment.SPIN) {
                putDown(npc, mind); // set back down on its feet
            }
            return;
        }
        if (mind.moment == ChildMoment.FEED) {
            long age = now - mind.momentAt;
            if (age >= 8L && age <= 48L && age % 4L == 0L) {
                UkakuFire.sound(level, npc.m_20182_().m_82520_(0.0, npc.m_20206_() * 0.8, 0.0), "entity.generic.eat", 0.5F,
                    1.3F + 0.2F * npc.m_217043_().m_188501_());
            }
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
                if (mind.moment == ChildMoment.FEED) {
                    fed(npc, mind); // it finishes it anyway
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

    // ---------------------------------------------------------------- 1.3.21: home life

    /** FEED's end: the food eaten (its hand empty again), a little health back. */
    private static void fed(ChildNpc npc, Mind mind) {
        npc.m_21008_(net.minecraft.world.InteractionHand.MAIN_HAND, EtoBrain.stack(null));
        mind.food = null;
        npc.m_5634_(4.0F);
    }

    /**
     * {@code food} given to it by {@code partner} (a parent): eaten through its FEED moment if it can eat it (a ghoul child only a ghoul's
     * food, a human child only a human's), true; false if it won't (said so) or can't now.
     */
    public static boolean feed(ChildNpc npc, LivingEntity partner, net.minecraft.world.item.ItemStack food, long now) {
        Mind mind = npc.mind;
        if (mind == null || food == null || food.m_41619_()) {
            return false;
        }
        mind.food = food;
        if (!start(npc, ChildMoment.FEED, partner, now)) {
            mind.food = null;
            return false;
        }
        return true;
    }

    /** True if a child (a ghoul, or not) can eat {@code food}. */
    public static boolean eats(boolean ghoul, net.minecraft.world.item.ItemStack food) {
        if (food == null || food.m_41619_()) {
            return false;
        }
        boolean meat = com.hex.kagune.control.ControlItems.ghoulMeat(food);
        return ghoul ? meat : !meat && food.m_41614_();
    }

    /**
     * Its night at home: to its own bed (a single bed; never the double), and lying down in it; tucked in by whoever sent it to bed (or an NPC
     * parent near). Only from its bedtime (or a parent's "Bedtime"), never a baby on its own (it can't walk: a parent puts it to bed).
     */
    private static void toBed(ChildNpc npc, Mind mind, Children.Child child, Households.Home house, ServerLevel level, long now, ChildStage stage) {
        boolean asked = mind.tuckBy != null;
        if (stage.sits() || mind.target != 0 || busy(mind, now) || !HomeLife.night(level.m_46468_(), true) && now >= mind.napUntil && !asked) {
            if (!HomeLife.night(level.m_46468_(), true)) {
                mind.tuckBy = null;
            }
            return;
        }
        BlockPos bed = HomeLife.childBed(level, house, child, now);
        if (bed == null || !HomeLife.free(level, bed)) {
            mind.bed = null;
            return;
        }
        mind.bed = bed;
        HomeLife.toBed(npc, bed, 0.8);
        if (npc.m_5803_()) {
            asleep(npc, mind, child, level, now);
        }
    }

    /** It just lay down: tucked in by whoever sent it (near), else by an NPC parent near; its line to them. */
    private static void asleep(ChildNpc npc, Mind mind, Children.Child child, ServerLevel level, long now) {
        LivingEntity by = mind.tuckBy == null ? null : player(level, mind.tuckBy);
        mind.tuckBy = null;
        if (by == null || by.m_20280_(npc) > 12.0 * 12.0) {
            by = null;
            for (EtoNpc parent : EtoBrain.live()) {
                if (parent.m_9236_() == level && parent.mind != null && !parent.m_5803_() && parent.m_20280_(npc) < 12.0 * 12.0
                    && Households.parentOf(level, parent, npc)) {
                    by = parent;
                    break;
                }
            }
        }
        if (by != null && start(npc, ChildMoment.TUCK, by, now)) {
            if (by instanceof EtoNpc parent && parent.mind != null) {
                parent.mind.momentChild = npc.m_20148_();
            } else if (by instanceof ServerPlayer player && mind.pending == ChildMoment.TUCK && mind.stage != null && !mind.stage.sits()) {
                say(player, npc, child, ChildLines.tuckMe(mind.stage, ChildLines.call(mind.stage, isMother(child, player)), npc.m_217043_().m_188503_(100)));
            }
        }
    }

    /** "Bedtime." from a parent: off to its bed now (a baby carried or near is laid in its bed); false if it has no bed at home. */
    public static boolean bedtime(ChildNpc npc, ServerPlayer parent, long now) {
        Mind mind = npc.mind;
        ServerLevel level = (ServerLevel)npc.m_9236_();
        Children.Child child = Children.get(level, npc.childId());
        Households.Home house = Households.forChild(level, child);
        if (mind == null || child == null || house == null || mind.stage == null) {
            return false;
        }
        BlockPos bed = HomeLife.childBed(level, house, child, now);
        if (bed == null || !HomeLife.free(level, bed)) {
            return false;
        }
        npc.follow(null);
        npc.waiting(false);
        npc.homeAt(house.pos());
        mind.tuckBy = parent.m_20148_();
        mind.napUntil = HomeLife.night(level.m_46468_(), true) ? Long.MIN_VALUE : now + 2400L;
        if (mind.stage.sits()) {
            // A baby can't walk: laid in its bed (from their arms, or where it sat).
            putDown(npc, mind);
            npc.m_6021_(bed.m_123341_() + 0.5, bed.m_123342_() + 0.6, bed.m_123343_() + 0.5);
            mind.bed = bed;
            npc.m_5802_(bed);
            asleep(npc, mind, child, level, now);
        } else {
            putDown(npc, mind);
        }
        return true;
    }

    /** "Go home." from a parent: it stops following or waiting elsewhere and goes back to its family's home (walking). */
    public static boolean goHome(ChildNpc npc, ServerLevel level) {
        Children.Child child = Children.get(level, npc.childId());
        Households.Home house = Households.forChild(level, child);
        if (house == null) {
            // 2.3.8: no family home: back where it belongs (where it was born or first put), never left following.
            if (npc.origin() == null) {
                return false;
            }
            npc.waiting(false);
            npc.sendHome();
            if (npc.mind != null) {
                putDown(npc, npc.mind);
            }
            return true;
        }
        npc.follow(null);
        npc.waiting(false);
        npc.homeAt(house.pos());
        if (npc.mind != null) {
            putDown(npc, npc.mind);
        }
        return true;
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
    /**
     * What threatens it: what it already fights, kept till it is dead or gone far off (1.3.24: {@link FightRules}); else a hostile mob
     * after it or one of its parents, or within five blocks of it (it was three).
     */
    private static LivingEntity threat(ChildNpc npc, Mind mind, Children.Child child, ServerLevel level, long now) {
        LivingEntity current = mind.target != 0 && level.m_6815_(mind.target) instanceof LivingEntity living ? living : null;
        if (current != null) {
            if (current.m_20280_(npc) <= FightRules.REACHED * FightRules.REACHED) {
                mind.reachedAt = now;
            }
            if (current.m_6084_() && current.m_9236_() == level && FightRules.keeps(Math.sqrt(current.m_20280_(npc)), -1.0,
                FightRules.since(now, mind.reachedAt), true)) {
                return current;
            }
        }
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity body : level.m_6443_(LivingEntity.class, npc.m_20191_().m_82400_(ChildRules.GUARD), entity -> entity.m_6084_()
            && EtoBrain.hostile(entity))) {
            LivingEntity target = body instanceof Mob mob ? mob.m_5448_() : null;
            boolean family = target == npc || target instanceof ServerPlayer player && child.parent(Pregnancies.player(player.m_20148_()))
                || target != null && SiblingLife.afterSibling(target, child, level); // 1.3.51: after a brother or sister: theirs to fight too
            double distance = body.m_20280_(npc);
            if ((family || FightRules.notices(Math.sqrt(distance))) && distance < bestDistance) {
                best = body;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static void fight(ChildNpc npc, Mind mind, Children.Child child, LivingEntity threat, ServerLevel level, long now) {
        boolean ghoul = child.genome().ghoul();
        if (!mind.turned || mind.target != threat.m_19879_()) {
            if (mind.target != threat.m_19879_()) {
                mind.reachedAt = now; // 1.3.24: a fresh fight
            }
            mind.turned = ghoul;
            mind.target = threat.m_19879_();
            mind.dirty = true;
            ServerPlayer parent = nearestParent(npc, child, level, 16.0);
            if (parent != null) {
                say(parent, npc, child, ChildLines.fight(mind.stage, npc.m_217043_().m_188503_(100)));
            }
        }
        // 1.3.43: a ghoul child with a kagune of its own fights with it (its tails, its wing or wings), as it was born to.
        com.hex.kagune.control.family.Genome genome = ghoul ? Children.genome(level, child) : null;
        // 2.2.1: a ghoul's and a human's child dashes as the White Reaper does: into reach, or out of the way.
        if (genome != null && genome.half()) {
            HalfBloodServer.fight(npc, threat, level, now);
        }
        if (ChildKagune.has(genome)) {
            mind.kaguneFought = true;
            ChildKagune.fight(npc, mind, child, genome, threat, level, now);
            return;
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
            com.hex.kagune.control.family.Genome born = Children.genome(level, child);
            float half = born != null && born.half() ? com.hex.kagune.control.family.HalfBlood.DAMAGE : 1.0F; // 2.2.1
            threat.m_6469_(npc.m_269291_().m_269333_(npc), ChildRules.damage(mind.stage, child.skill, ghoul, Children.might(level, child)) * half);
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
        if (mind.conversation != null || mind.hideWith != 0 || now < mind.dreadUntil) {
            return; // 1.3.53: nothing of its own to say in the middle of a talk (a greeting waits till it is over); 1.3.58: nor hiding (1.3.65: from War)
        }
        int seed = npc.m_217043_().m_188503_(1000);
        String call = ChildLines.call(stage, isMother(child, parent));
        if (mind.parentAway != Long.MIN_VALUE && now - mind.parentAway >= 6000L) {
            mind.reunited = now; // 1.3.29: away a long while: it missed them
        }
        if (mind.parentAway != Long.MIN_VALUE && now - mind.parentAway >= 1200L) {
            say(parent, npc, child, ChildLines.greet(stage, call, seed));
            mind.nextLine = now + 600L + npc.m_217043_().m_188503_(900);
            welcome(npc, mind, parent, now); // 1.3.20: it runs to them
        }
        mind.parentAway = Long.MIN_VALUE;
        // 1.3.29: now and then it comes up to them with something (a scene: SceneServer), and waits for their answer.
        if (mind.target == 0 && now % 20L == 9L && !busy(mind, now) && SceneServer.childTick(npc, child, parent, now)) {
            mind.nextLine = Math.max(mind.nextLine, now + 1200L);
            return;
        }
        if (now >= mind.nextLine && mind.target == 0) {
            say(parent, npc, child, ChildLines.idle(stage, call, seed));
            mind.nextLine = now + 900L + npc.m_217043_().m_188503_(1500);
        }
    }

    /** 1.3.30: its words to {@code player}, in a bubble over its head ({@code npc}) that only they see (never the chat). */
    // ---------------------------------------------------------------- 1.3.65: hiding from War

    /**
     * {@code from}, its parent, is in War (or the Dragon) near it: it hides from them a while ({@link FinalSight#HIDES}, kept up as long as
     * they are near). As it starts, whatever it was doing stops: a talk, a moment, a game of hide and seek or tag, minding a little one.
     */
    static void dread(ChildNpc npc, ServerPlayer from, ServerLevel level, long now) {
        dread(npc, from, level, now, FinalSight.HIDES);
    }

    /** 2.0.5: the same, for {@code ticks} (a stranger's kagune frightens it for a while: it is not renewed while in sight). */
    static void dread(ChildNpc npc, ServerPlayer from, ServerLevel level, long now, long ticks) {
        Mind mind = npc.mind;
        if (mind == null || mind.carrier != null) {
            return; // in someone's arms: they have it
        }
        boolean fresh = now >= mind.dreadUntil;
        mind.dreadOf = from.m_20148_();
        mind.dreadUntil = Math.max(mind.dreadUntil, now + ticks);
        if (!fresh) {
            return;
        }
        mind.dreadSaidAt = now; // its first word is the one it says on seeing it (Sightings)
        if (Conversation.in(npc) != null) {
            Conversation.end(npc, com.hex.kagune.control.family.Talks.End.INTERRUPTED, level, now);
        }
        stopMoment(npc);
        SiblingLife.stopHide(npc);
        mind.tagWith = 0;
        mind.minding = 0;
    }

    /**
     * Hiding from the parent in War: behind the other parent (a player, or its companion parent: whichever is nearer, within 24 and not in
     * War themselves), on the far side from them, or with no one to hide behind, {@link FinalSight#KEEPS_OFF} off; crouched in on itself
     * ({@code ChildCue.COWER}), its eyes on them; now and then a word ({@code FearLines.hiding}). A baby can't go anywhere: it sits and
     * whimpers. False once they are gone (it stops hiding).
     */
    private static boolean cower(ChildNpc npc, Mind mind, Children.Child child, ServerLevel level, long now) {
        ServerPlayer from = mind.dreadOf == null ? null : level.m_7654_().m_6846_().m_11259_(mind.dreadOf);
        if (from == null || from.m_9236_() != level || !from.m_6084_() || from.m_20280_(npc) > 24.0 * 24.0) {
            mind.dreadUntil = Long.MIN_VALUE;
            return false;
        }
        if (Conversation.in(npc) != null) {
            Conversation.end(npc, com.hex.kagune.control.family.Talks.End.INTERRUPTED, level, now);
        }
        ChildStage stage = child.stage(now);
        // 2.4.4 (the owner: "kids shake but dont run away, adult kids are scared"): a child still growing stays where it is, shaking,
        // crouched in on itself, its eyes on them; only a grown one backs away behind someone (or off).
        boolean grown = stage == ChildStage.ADULT;
        LivingEntity shelter = stage.sits() || !grown ? null : shelter(npc, child, from, level);
        Vec3 spot = null;
        if (shelter != null) {
            double[] at = FinalSight.behind(shelter.m_20185_(), shelter.m_20189_(), from.m_20185_(), from.m_20189_(),
                FinalSight.BEHIND + shelter.m_20205_() * 0.5);
            spot = new Vec3(at[0], shelter.m_20186_(), at[1]);
        } else if (grown && from.m_20280_(npc) < FinalSight.KEEPS_OFF * FinalSight.KEEPS_OFF) {
            double[] at = com.hex.kagune.control.npc.HumanRules.fleeStep(npc.m_20185_(), npc.m_20189_(), from.m_20185_(), from.m_20189_(), 0.0, 4.0);
            spot = new Vec3(at[0], npc.m_20186_(), at[1]);
        }
        double dx = spot == null ? 0.0 : spot.f_82479_ - npc.m_20185_();
        double dz = spot == null ? 0.0 : spot.f_82481_ - npc.m_20189_();
        if (spot != null && dx * dx + dz * dz > FinalSight.THERE * FinalSight.THERE) {
            if (now % 5L == 0L || npc.m_21573_().m_26571_()) {
                npc.m_21573_().m_26519_(spot.f_82479_, spot.f_82480_, spot.f_82481_, 1.35);
            }
        } else {
            npc.m_21573_().m_26573_();
            npc.m_20256_(new Vec3(0.0, npc.m_20184_().f_82480_, 0.0));
        }
        npc.m_21563_().m_24960_(from, 30.0F, 30.0F);
        if (mind.turned || mind.target != 0) {
            mind.turned = false;
            mind.target = 0;
            mind.dirty = true;
        }
        if (mind.cue != com.hex.kagune.control.family.ChildCue.COWER || !mind.cue.running(now - mind.cueAt)) {
            mind.cue = com.hex.kagune.control.family.ChildCue.COWER;
            mind.cueAt = now;
            mind.dirty = true;
        }
        if (now - mind.dreadSaidAt >= 160L + npc.m_217043_().m_188503_(100)) {
            mind.dreadSaidAt = now;
            String call = ChildLines.call(stage, isMother(child, from));
            String other = shelter instanceof ServerPlayer them ? ChildLines.call(stage, isMother(child, them))
                : shelter instanceof EtoNpc companion ? ChildLines.call(stage, companion.persona().female()) : "";
            // 2.0.5: hiding from a stranger (not its parent in War): its words for them.
            String line = child.parent(Pregnancies.player(from.m_20148_()))
                ? com.hex.kagune.control.npc.FearLines.hiding(stage, call, other, npc.m_217043_().m_188503_(100))
                : com.hex.kagune.control.family.StrangerLines.hiding(stage, other, npc.m_217043_().m_188503_(100));
            for (ServerPlayer player : level.m_6907_()) {
                if (player.m_20280_(npc) <= 16.0 * 16.0 && SceneServer.sees(npc, player, 16.0)) {
                    ControlNetwork.sendTo(player, new com.hex.kagune.control.network.S2CNpcSay(npc.m_19879_(), line));
                }
            }
        }
        return true;
    }

    /** The other parent it hides behind from {@code from}: a player or its companion parent, the nearer, within 24, alive, not in War. */
    private static LivingEntity shelter(ChildNpc npc, Children.Child child, ServerPlayer from, ServerLevel level) {
        String fromKey = Pregnancies.player(from.m_20148_());
        LivingEntity best = null;
        double bestDistance = 24.0 * 24.0;
        for (String parent : new String[] {child.mother, child.father}) {
            if (parent == null || parent.equals(fromKey)) {
                continue;
            }
            ServerPlayer player = Pregnancies.playerOf(level, parent);
            if (player != null && player != from && player.m_9236_() == level && player.m_6084_() && !Sightings.seen(player).terror()
                && player.m_20280_(npc) < bestDistance) {
                best = player;
                bestDistance = player.m_20280_(npc);
            }
        }
        for (EtoNpc companion : level.m_6443_(EtoNpc.class, npc.m_20191_().m_82400_(24.0), body -> body.m_6084_() && body.mind != null)) {
            if (child.parent(Pregnancies.npc(companion.persona().key())) && companion.m_20280_(npc) < bestDistance) {
                best = companion;
                bestDistance = companion.m_20280_(npc);
            }
        }
        return best;
    }

    static void say(ServerPlayer player, ChildNpc npc, Children.Child child, String line) {
        // 1.3.31: only when it can see them, near enough (never through a wall).
        if (line != null && !line.isEmpty() && npc != null && SceneServer.sees(npc, player, 16.0)) {
            com.hex.kagune.control.network.ControlNetwork.sendTo(player, new com.hex.kagune.control.network.S2CNpcSay(npc.m_19879_(), line));
        }
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
            (mind == null ? ChildMoment.NONE : mind.moment).ordinal(), mind == null ? Long.MIN_VALUE : mind.momentAt, partner == null ? 0 : partner.m_19879_(),
            (mind == null ? com.hex.kagune.control.family.ChildCue.NONE : mind.cue).ordinal(), mind == null ? Long.MIN_VALUE : mind.cueAt,
            Children.motherSize(child)); // 2.3.0
    }

    /** A player starts seeing a child: they are told how to draw it. */
    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ChildNpc npc && event.getEntity() instanceof ServerPlayer viewer && npc.m_9236_() instanceof ServerLevel level) {
            Children.Child child = Children.get(level, npc.childId());
            if (child != null) {
                ControlNetwork.sendTo(viewer, message(npc, npc.mind, child));
                ControlNetwork.sendTo(viewer, ChildKagune.message(npc, npc.mind, child)); // 1.3.43: its kagune, as a wild ghoul's
                if (com.hex.kagune.control.family.KaguneGenes.wings(child.genome().kagune()) > 0) {
                    ControlNetwork.sendTo(viewer, ChildKagune.wings(npc, npc.mind)); // 1.3.48: its wings' crystal
                }
                if (com.hex.kagune.control.family.KaguneGenes.owl(child.genome().kagune()) && npc.mind != null) {
                    ChildFlesh.tracked(viewer, npc, npc.mind.kagune.flesh); // 1.3.50: Eto's child's flesh, and its arm if it is out
                    ChildArm.tracked(viewer, npc);
                }
            }
        }
    }

    /**
     * A parent's blow (a swing, a kagune's sweep) never lands on their own child. 1.3.21: and nothing a companion does ever touches a child
     * (the owner: a mother's attack caught her child by accident and killed it): not Eto's fists or flesh, Touka's spikes, Ken's blades or his
     * flesh creatures, whatever they were aiming at.
     */
    @SubscribeEvent
    public static void attacked(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ChildNpc npc) || event.getSource() == null || !(npc.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        net.minecraft.world.entity.Entity by = event.getSource().m_7639_();
        net.minecraft.world.entity.Entity direct = event.getSource().m_7640_();
        if (companion(by) || companion(direct)) {
            event.setCanceled(true);
            return;
        }
        if (by instanceof ServerPlayer player) {
            Children.Child child = Children.get(level, npc.childId());
            if (child != null && child.parent(Pregnancies.player(player.m_20148_()))) {
                event.setCanceled(true);
            }
        }
    }

    /** 1.3.21: a companion, or something of one's (its flesh creatures): never a child's harm. */
    static boolean companion(net.minecraft.world.entity.Entity body) {
        if (body instanceof EtoNpc) {
            return true;
        }
        return body instanceof com.hex.kagune.control.npc.FleshThrall thrall && FleshThralls.owner(thrall) instanceof EtoNpc;
    }

    /** It died: the world remembers (its body comes back home a day later), its parents are told. */
    @SubscribeEvent
    public static void died(LivingDeathEvent event) {
        if (event.getEntity() instanceof ChildNpc npc && npc.m_9236_() instanceof ServerLevel level) {
            Children.Child child = Children.get(level, npc.childId());
            if (child != null) {
                tellParents(level, child, "§c" + child.name + " is gone. They won't be coming back.");
            }
            // 1.3.21: the owner: "when they die, that's it". Forgotten by the world: never remade.
            HomeLife.wake(npc);
            if (npc.mind != null) {
                ChildKagune.fizzleAll(npc, npc.mind); // 1.3.43: its shots in flight put out
            }
            if (npc.mind != null && npc.mind.food != null) {
                npc.mind.food = null;
            }
            UniqueNpcs.perished(level, npc.uniqueKey());
        }
    }
}
