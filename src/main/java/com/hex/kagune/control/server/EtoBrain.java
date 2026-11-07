package com.hex.kagune.control.server;

import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CEto;
import com.hex.kagune.control.network.S2CNpcAlert;
import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.EtoHand;
import com.hex.kagune.control.npc.EtoLines;
import com.hex.kagune.control.npc.EtoMood;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.npc.EtoTalk;
import com.hex.kagune.control.npc.NpcBonds;
import com.hex.kagune.control.npc.NpcMemory;
import com.hex.kagune.control.npc.NpcRules;
import com.hex.kagune.control.npc.Romance;
import com.hex.kagune.control.npc.UniqueNpc;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.quest.QuestLog;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 1.3.0: what Eto does, decided on the server every tick ({@link EtoNpc}'s own AI step). She keeps to her home (restricted to
 * {@link EtoRules#HOME_RADIUS}, put back if she ends up past {@link EtoRules#LOST}); hostiles that come within reach of her, a player who
 * hits her or is her enemy, she fights ({@link EtoRules#choose}: the Speed Blitz, the Big Punch, the Speed Kick, flips) as far as her
 * leash, then goes home and heals. Calm, she does her goofy gestures now and then, and with a player she is dating she sometimes asks
 * for a kiss herself ({@link EtoRules#kiss}): she walks up, leans in with her eyes shut, and waits for a tap of shift (a peck), a hold (she
 * blushes and stays), or nothing (she is cross, and ignores them a while). Every client tracking her is told what it needs to draw her.
 *
 * <p>1.3.2: there is only one of her ({@link UniqueNpcs}: another body is sent away on its first tick). Her partner (one player, for good)
 * is never fought, whatever they do to her (she sulks instead), is protected when she sees them hurt, is clung to, greeted when they come
 * home, gone looking for and talked to, and warned privately ({@code S2CNpcAlert}) when she wants a kiss, a hug or them. A kiss held long
 * enough, she pulls them in. Her idle hands follow her mood ({@link EtoMood}), with a book, a quill, a brush in them. In a fight she gets
 * serious, then maniac ({@link EtoRules.Phase}): faster, harder to hurt. 1.3.4: her fight is her Owl DNA kit, a Pale player's own moves
 * ({@link EtoOwl}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class EtoBrain {
    /** Her state on the server. */
    public static final class Mind {
        /** What she fights (an entity id, 0 none), when she last fought and was hurt. */
        int target;
        long foughtAt = Long.MIN_VALUE;
        long hurtAt = Long.MIN_VALUE;
        /** 1.3.24: when she last got at what she fights (close to it, or a blow landed on it): given up only after a long while without. */
        long reachedAt = Long.MIN_VALUE;
        /** Players who hurt her, until when she holds it against them. */
        final Map<UUID, Long> grudges = new HashMap<>();
        /** 1.3.4: her Owl DNA kit's moves ({@link EtoOwl}). */
        final EtoOwl.Fight owl = new EtoOwl.Fight();
        /** 1.3.19: Ken's side of Peace ({@link KenPeace}). */
        final KenPeace.State peace = new KenPeace.State();
        /** 1.3.19: Touka's wings ({@link NpcWings}). */
        final NpcWings.State wings = new NpcWings.State();
        /** 1.3.49: a child of hers she is going after (ParentDuty), or null. */
        ParentDuty.Duty duty;
        /** 1.3.53: a talk she stays for (her children's, hers with them: Conversation), or null. */
        Conversation conversation;
        EtoGesture gesture = EtoGesture.NONE;
        long gestureAt = Long.MIN_VALUE;
        long nextGestureAt;
        /** Who she is walking up to (a kiss, a hug, to cling), and what for: null none. */
        UUID toward;
        /** 3.2.2: that walk is one she set off on by herself (not asked for): she lets it go if someone else gets to them first. */
        boolean towardOwn;
        /** 3.2.2: until when someone else has her partner (checked every 5 ticks): nothing of her own till then. */
        long busyFor = Long.MIN_VALUE;
        EtoGesture towardFor = EtoGesture.NONE;
        long towardSince;
        /** The kiss she is waiting on: its stage, since when, the shift key's hold. */
        EtoRules.Kiss kiss;
        long kissAt;
        int held;
        /** 2.2.4: the kiss as her mood has it (MoodKiss), and whether a teasing one has dodged its first try. */
        com.hex.kagune.control.npc.MoodKiss.Style kissStyle = com.hex.kagune.control.npc.MoodKiss.Style.SWEET;
        boolean dodged;
        /** 2.3.9: the kiss is one she came asking for herself (not one they asked for): turning it down is no offence. */
        boolean asked;
        /** Per player: when she next asks for a kiss of her own, and until when she ignores them. */
        final Map<UUID, Long> nextAsk = new HashMap<>();
        final Map<UUID, Long> ignoring = new HashMap<>();
        /** Who she is talking to (she stands still and faces them), until when. */
        UUID talking;
        long talkingUntil;
        boolean turned;
        boolean dirty = true;
        // 1.3.2
        /** Her partner as the world remembers it (refreshed every second), and the line over her head. */
        UUID partner;
        Romance.Stage stage = Romance.Stage.NONE;
        String tag = "";
        /** Who she holds on to now (an entity id; 0 none), for every client. */
        int with;
        /** How seriously she takes the fight. */
        EtoRules.Phase phase = EtoRules.Phase.CALM;
        /** Per player: smiling since a kiss or a hug, until; cross after a hit, until; close since; next she goes looking for them. */
        final Map<UUID, Long> giddy = new HashMap<>();
        final Map<UUID, Long> sulk = new HashMap<>();
        /** 2.2.0: what she holds against each partner (how many offences, the last when): {@link com.hex.kagune.control.npc.Tolerance}. */
        final Map<UUID, long[]> offences = new HashMap<>();
        /** 2.2.3: once she's over it, she comes to make it up ({@code make_up} after a sulk, {@code jealous_over} after jealousy): by player. */
        final Map<UUID, String> makeUp = new HashMap<>();
        /** 2.2.0: when she last brushed a blow off (so a flurry isn't a flurry of words). */
        long shrugAt = Long.MIN_VALUE / 2;
        final Map<UUID, Long> nearSince = new HashMap<>();
        long nextSeek;
        long nextNightAsk;
        /** 1.3.12: when she last started something herself; what she last remarked on (the rain, dusk and dawn by day, a wound). */
        long initiativeAt;
        boolean rained;
        long duskDay = -1L;
        long dawnDay = -1L;
        long hurtSaid;
        long killSaid;
        /** Her partner last near her (game time), and whether they are near now: back after a while, she greets them. */
        long partnerNearAt = Long.MIN_VALUE;
        boolean partnerNear;
        /** The gesture her hands were last dressed for. */
        EtoGesture props = EtoGesture.NONE;
        /** 2.4.9: the food she carries from the stove (an item id: in her hand as she walks and as she holds it out), and what was shown. */
        String carry;
        String propsCarry;
        long protectedAt = Long.MIN_VALUE;
        /** 1.3.3: when a player was last about her (away from home a day without one, she goes back). */
        long company = Long.MIN_VALUE;
        /** 1.3.3: whose hand she holds (she walks at their side), and whether their shift was down last tick (a tap lets go). */
        UUID holding;
        boolean holdShift;
        /** 1.3.15: the child she holds in her arms (null none), until when, and when she may pick one up again ({@link ParentCare}). */
        UUID cradling;
        long cradleUntil;
        long nextCradle;
        /** 1.3.20: the child it is having a moment with (a hug, a toss...), and when it may start another. */
        UUID momentChild;
        long nextMoment;
        /** 1.3.21: the bed it lies in (or is on its way to), at home; null: none tonight. */
        BlockPos bed;
        /** 1.3.37: half woken in the night by someone's hand: until this tick their talk doesn't get her up (HomeLife). */
        long drowsyUntil = Long.MIN_VALUE;
        /** 1.3.14: the partner she is coming to for the night they asked for a baby (null none), and when she may next try. */
        UUID babyNight;
        long nextBabyNight;
        /** 1.3.29: the chore she is doing at home ({@link HomeChores}; null none), when she may start the next, the last, when she last said so. */
        HomeChores.Job chore;
        long nextChore;
        com.hex.kagune.control.family.Chores.Kind lastChore;
        long choreSaid = Long.MIN_VALUE;
        long noSweepUntil;
        /** 1.3.31: her partner came back after a while (when): she greets them once she sees them; MIN_VALUE: no one to greet. */
        long welcomeFrom = Long.MIN_VALUE;
        /** 1.3.31: when she last said the stove had nothing to burn. */
        long noFuelSaid = Long.MIN_VALUE / 2;
        /** 1.3.29: her walk at home last checked: where she was, when, how many checks in a row she got nowhere. */
        double stuckX;
        double stuckZ;
        long stuckAt;
        int stuck;
    }

    /** 1.3.32: true if any player is near enough to see her (within 32 blocks): she is never moved before anyone's eyes. */
    static boolean watched(EtoNpc npc, ServerLevel level) {
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_20280_(npc) < 32.0 * 32.0) {
                return true;
            }
        }
        return false;
    }

    /** 1.3.29: true while a chore at home has her (vanilla's strolling waits: it would walk her off it). */
    public static boolean busy(EtoNpc npc) {
        // 1.3.39: nor wandering off while someone talks to her. 1.3.53: nor in a talk with her children.
        return HomeChores.busy(npc.mind) || npc.mind != null && (npc.mind.talking != null || Conversation.in(npc) != null);
    }

    /**
     * 1.3.29: "Go home, please.": she lets go of whatever she was doing with them, follows no one, and her home is where she belongs again
     * ({@code at}): she walks there, or is simply there once she is far enough off (and the talk is over).
     */
    static void sendHome(EtoNpc npc, ServerLevel level, BlockPos at, boolean shared, long now) {
        Mind mind = npc.mind;
        if (mind == null) {
            return;
        }
        letGo(npc, mind, level, now, false);
        endKiss(npc, mind, null, now);
        mind.toward = null;
        mind.towardFor = EtoGesture.NONE;
        HomeChores.stop(npc, mind, level);
        if (shared) {
            npc.homeAt(at);
        } else {
            npc.sendHome();
        }
        mind.company = now;
        npc.m_21573_().m_26519_(at.m_123341_() + 0.5, at.m_123342_() - 1.0, at.m_123343_() + 0.5, 0.9);
    }

    /** Every Eto ticking on this server (one, unless another has yet to be sent away). */
    private static final Set<EtoNpc> LIVE = Collections.newSetFromMap(new WeakHashMap<>());

    /** 1.3.21: every companion's body ticking now (a copy). */
    static java.util.List<EtoNpc> live() {
        return new java.util.ArrayList<>(LIVE);
    }

    private EtoBrain() {
    }

    public static void tick(EtoNpc npc) {
        if (!(npc.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        long now = level.m_46467_();
        Mind mind = npc.mind;
        if (mind == null) {
            // 1.3.2: only one of her lives; any other body leaves on its first tick.
            BlockPos first = npc.home() != null ? npc.home() : npc.settle();
            if (!UniqueNpcs.claim(level, npc.persona().key(), npc.m_20148_(), first)) {
                // 3.0.4 (the owner: "I see the library, but Eto is not in it"): a world has three libraries, and whichever was loaded first
                // (a player passing within range, unseen) took her; the one they then found sent its own body away. Until someone has met
                // her, a body of hers waits where it was made, and the one a player comes to is her.
                UniqueNpc slot = UniqueNpcs.get(level, npc.persona().key());
                if (slot.met || slot.partner != null || metByAnyone(level, npc.persona().key())) {
                    npc.m_146870_();
                    return;
                }
                if (!someoneAbout(npc, level)) {
                    npc.m_21573_().m_26573_();
                    return;
                }
                UniqueNpcs.adopt(level, npc.persona().key(), npc, first);
            }
            mind = new Mind();
            npc.mind = mind;
            mind.nextGestureAt = now + 100L + npc.m_217043_().m_188503_(200);
            mind.nextSeek = now + 1200L;
            mind.nextChore = now + 200L + npc.m_217043_().m_188503_(400);
            UniqueNpc known = UniqueNpcs.get(level, npc.persona().key());
            mind.company = known.company != Long.MIN_VALUE ? known.company : now;
            npc.origin();
            npc.m_21530_();
            // 1.3.3: right-handed always (a left-handed one held the pocket book in the hand that writes).
            npc.m_21559_(false);
            LIVE.add(npc);
        }
        BlockPos home = npc.home();
        if (home == null) {
            home = npc.settle();
        }
        if (now % 200L == 7L && !UniqueNpcs.claim(level, npc.persona().key(), npc.m_20148_(), home)) {
            npc.m_146870_();
            return;
        }
        if (now % 20L == 13L && someoneAbout(npc, level)) {
            mind.company = now;
        }
        if (now % 200L == 7L) {
            // 1.3.3: away from her own floor a day with no one about (waiting, following, lost), she goes home.
            BlockPos origin = npc.origin();
            boolean away = origin != null && Math.pow(origin.m_123341_() - home.m_123341_(), 2) + Math.pow(origin.m_123342_() - home.m_123342_(), 2)
                + Math.pow(origin.m_123343_() - home.m_123343_(), 2) > 6.0 * 6.0;
            UniqueNpcs.report(level, npc.persona().key(), origin, away, mind.company);
            if (away && mind.target == 0 && now - mind.company >= UniqueNpc.GOES_HOME) {
                letGo(npc, mind, level, now, false);
                npc.sendHome(); // 2.0.7: she walks home (never put there)
                home = npc.home();
                mind.company = now;
                UniqueNpcs.report(level, npc.persona().key(), origin, false, now);
                ServerPlayer partner = player(level, mind.partner);
                if (partner != null) {
                    partner.m_5661_(Component.m_237113_(npc.persona().colour() + npc.displayName() + " got tired of waiting and set off home to "
                        + npc.persona().home() + "."), false);
                }
            }
        }
        if (now % 20L == 3L) {
            partner(npc, mind, level);
        }
        // 2.1.2: courtship chapters counted in time and place (a walk, a sit, somewhere high, dawn, the dead of night).
        CourtshipServer.tick(npc, level, now);
        // 3.0.9: following a player, her home goes with her; she walks to keep up, regardless of distance.
        // 1.3.3: holding someone's hand, the same, at their side.
        UUID leaderId = mind.holding != null ? mind.holding : npc.following();
        if (leaderId != null) {
            ServerPlayer leader = player(level, leaderId);
            if (now % 20L == 0L || leader != null && mind.target == 0) {
                home = npc.settle();
            }
            if (leader != null && mind.target == 0) {
                Vec3 at = leader.m_20182_();
                double far = npc.m_20238_(at);
                if (far > 9.0 && now % 10L == 0L && mind.toward == null && mind.kiss == null && mind.holding == null) {
                    npc.m_21573_().m_5624_(leader, 1.1);
                }
            }
        }
        double[] feet = npc.homeFeet();
        Vec3 homeAt = new Vec3(feet[0], feet[1], feet[2]);
        // 1.3.21: at the home shared with their spouse, she roams all of it, walks back on her own when she strays (her restriction), and is
        // brought back only from far beyond it.
        Households.Home house = npc.following() == null ? Households.forCompanion(level, npc) : null;
        // 2.0.7 (the owner: "NPCS should not be teleported home unless they die or bug inside a wall ... go outside the max radius, just with
        // the feeling that by night time they have to be home"): at a shared home her leash reaches farther by day and only her home at
        // night; far out, she walks back (MoveTowardsRestrictionGoal), never put there for being far. Put home only stuck inside blocks, or
        // out when she should be home and getting no nearer for minutes with nobody to see (Homeward).
        boolean night = HomeLife.night(level.m_46468_(), false);
        int leash = house != null ? com.hex.kagune.control.family.HomeRules.leash(house.radius(), night, com.hex.kagune.control.family.HomeRules.roam(null))
            : mind.partnerNear ? EtoRules.LEASH : EtoRules.HOME_RADIUS;
        boolean away = mind.target == 0 && mind.talking == null && mind.holding == null && npc.following() == null
            && npc.m_20238_(homeAt) > (house != null ? (leash + 4.0) * (leash + 4.0) : EtoRules.LOST * EtoRules.LOST);
        boolean recoveryBusy = mind.target != 0 || mind.owl.busy() || EtoOwl.carried(mind.owl) || mind.holding != null
            || npc.following() != null || mind.talking != null || mind.with != 0;
        if (Homeward.bugged(npc, level, homeAt, away, recoveryBusy, now)) {
            Homeward.put(npc, homeAt);
            mind.target = 0;
        }
        LivingEntity target = target(npc, mind, level, homeAt, now);
        // 1.3.19: his final kakuja's kit (Ken's Peace), every tick; it goes back into him once the fight is long over.
        KenPeace.tick(npc, mind, target, level, now);
        // 1.3.19: her wings' spikes in flight, and the wings going back in once the fight is long over (Touka's).
        NpcWings.tick(npc, mind, level, now);
        // Her partner about, she may wander as far as her leash to be near them.
        // 1.3.24: fighting, as far as the chase goes (it keeps after what it fights till it is dead or gone).
        int chase = (int)com.hex.kagune.control.npc.FightRules.CHASE;
        npc.m_21446_(home, house != null ? Math.max(leash, target != null ? chase : 0) : target != null ? chase : leash);
        // 1.3.29: her chores at home stop for anything else (a fight, a talk, a kiss, a hand, a child, the night, home no more); walking
        // about at home, she never stays stuck.
        if (mind.chore != null && (house == null || target != null || HomeLife.night(level.m_46468_(), false) || mind.toward != null || mind.kiss != null
            || mind.holding != null || mind.talking != null || mind.cradling != null || mind.momentChild != null || mind.with != 0 || npc.m_5803_())) {
            HomeChores.stop(npc, mind, level);
        }
        if (house != null && target == null && mind.chore == null && mind.toward == null && mind.kiss == null && mind.holding == null && mind.talking == null) {
            HomeChores.unstick(npc, mind, house, now);
        }
        boolean turned = target != null;
        if (turned != mind.turned) {
            mind.turned = turned;
            mind.dirty = true;
        }
        if (target != null) {
            endKiss(npc, mind, null, now);
            letGo(npc, mind, level, now, false);
            Conversation.end(npc, com.hex.kagune.control.family.Talks.End.FIGHT, level, now); // 1.3.53: a fight ends any talk
            ParentCare.drop(npc, mind, level, now);
            if (mind.gesture != EtoGesture.NONE && mind.gesture != EtoGesture.LAUGH) {
                gesture(mind, EtoGesture.NONE, now);
            }
            fight(npc, mind, target, level, now);
        } else if (mind.owl.busy()) {
            // What it was fighting is gone: the move under way plays out (a kick lands on nothing, Witness This drops out).
            EtoOwl.tick(npc, mind, null, level, now, mind.phase);
        } else {
            if (mind.phase != EtoRules.Phase.CALM && now - mind.foughtAt > 60L) {
                mind.phase = EtoRules.Phase.CALM;
            }
            // 1.3.21: at night at home, to her bed (the double bed's free half); else her calm. 1.3.53: a talk she is in before either:
            // she stays for it (standing, turned to whoever speaks; or coming over to it first).
            if (Conversation.going(npc, level, now) && Conversation.hold(npc, level, now)) {
                mind.company = now;
            } else if (!toBed(npc, mind, house, level, now)) {
                calm(npc, mind, level, now);
            }
            heal(npc, mind, now);
        }
        // 1.3.6: her Watch This tentacles hunt on their own, whatever she is doing, until their ten seconds are up.
        EtoWatch.tick(npc, mind, level, now);
        // 1.3.7: and her Mutated Arm, out until it is back in her.
        EtoArm.tick(npc, mind, level, now);
        // 1.3.8: the Owl's burst, and shedding it once the fight is long over.
        EtoOwlForm.tick(npc, mind, level, now);
        // 2.4.8: a finger cracked is heard (the pack's finger_crack, at the moment the pose snaps).
        if (mind.gesture == EtoGesture.CRACK && now - mind.gestureAt == KenPullOut.CRACK_AT) {
            double yaw = Math.toRadians(npc.f_20883_);
            UkakuFire.sound(level, npc.m_20182_().m_82520_(-Math.sin(yaw) * 0.5, 1.3, Math.cos(yaw) * 0.5), "hexkagune:finger_crack", 0.8F, 1.0F);
        }
        // 1.3.30: a child in her arms is held the whole time (its cradle, PET, never runs out under it: it ran out and was begun again,
        // the arms dropping and coming back).
        boolean cradles = mind.cradling != null && mind.gesture == EtoGesture.PET;
        if (mind.gesture.over(now - mind.gestureAt) && mind.gesture != EtoGesture.NONE && !mind.gesture.held() && !cradles) {
            gesture(mind, EtoGesture.NONE, now);
        }
        // 3.2.3: a kiss's, a blush's or a hand's pose with no kiss or hand behind it any more: let go of after a moment (it never was).
        if (mind.gesture.stranded(now - mind.gestureAt, mind.kiss != null || mind.toward != null, mind.holding != null)) {
            gesture(mind, EtoGesture.NONE, now);
        }
        if (mind.with != 0 && mind.gesture != EtoGesture.PULL && mind.gesture != EtoGesture.CLING && mind.gesture != EtoGesture.HUG
            && mind.gesture != EtoGesture.HAND && mind.gesture != EtoGesture.STEAL && mind.kiss == null) {
            mind.with = 0;
            mind.dirty = true;
        }
        props(npc, mind);
        sync(npc, mind);
    }

    /** Her partner, as the world remembers it: the line over her head follows. */
    private static void partner(EtoNpc npc, Mind mind, ServerLevel level) {
        UniqueNpc unique = UniqueNpcs.get(level, npc.persona().key());
        UUID partner = unique.stage.together() ? unique.partner : null;
        String tag = unique.tag();
        mind.partner = partner;
        mind.stage = partner == null ? Romance.Stage.NONE : unique.stage;
        if (!tag.equals(mind.tag)) {
            mind.tag = tag;
            mind.dirty = true;
        }
    }

    /** True if {@code player} is her partner. */
    static boolean partnerOf(Mind mind, Entity player) {
        return mind != null && mind.partner != null && player != null && mind.partner.equals(player.m_20148_());
    }

    // ---------------------------------------------------------------- how she feels

    /** How she feels about {@code player} now ({@link EtoMood#of}, from what has happened). */
    public static EtoMood mood(EtoNpc npc, ServerPlayer player, long now) {
        Mind mind = npc.mind;
        ServerLevel level = (ServerLevel)npc.m_9236_();
        UniqueNpc unique = UniqueNpcs.get(level, npc.persona().key());
        UUID id = player.m_20148_();
        NpcMemory memory = NpcMemoryServer.of(player);
        long seen = memory.get(npc.persona().key(), NpcMemory.SEEN, Long.MIN_VALUE);
        long daysAway = seen == Long.MIN_VALUE ? 0L : Math.max(0L, (now - seen) / 24000L);
        boolean sulking = mind != null && (until(mind.ignoring, id) > now || until(mind.sulk, id) > now);
        boolean jealous = memory.get(npc.persona().key(), NpcMemory.JEALOUS, Long.MIN_VALUE) > now;
        boolean giddy = mind != null && until(mind.giddy, id) > now;
        Long since = mind == null ? null : mind.nearSince.get(id);
        boolean near = since != null && now - since > 600L;
        double roll = Math.floorMod((id.hashCode() * 31L + now / 3000L) * 0x9E3779B1L, 1000L) / 1000.0;
        return EtoMood.of(unique.stageOf(id), unique.claimedFrom(id), QuestServer.bond(player, npc.persona().key()).regard(), level.m_46468_(), daysAway, sulking,
            jealous, giddy, near, roll);
    }

    private static long until(Map<UUID, Long> map, UUID id) {
        Long value = map.get(id);
        return value == null ? Long.MIN_VALUE : value;
    }

    // ---------------------------------------------------------------- who she fights

    private static LivingEntity target(EtoNpc npc, Mind mind, ServerLevel level, Vec3 home, long now) {
        LivingEntity current = mind.target == 0 ? null : level.m_6815_(mind.target) instanceof LivingEntity living ? living : null;
        if (current != null && current.m_20280_(npc) <= com.hex.kagune.control.npc.FightRules.REACHED * com.hex.kagune.control.npc.FightRules.REACHED) {
            mind.reachedAt = now;
        }
        // 1.3.24: what it fights it keeps till it is dead, or gone far off, or chased too far from home, or out of its reach a long while.
        if (current != null && (!current.m_6084_() || current.m_9236_() != level
            || !com.hex.kagune.control.npc.FightRules.keeps(Math.sqrt(current.m_20280_(npc)), Math.sqrt(current.m_20238_(home)),
                com.hex.kagune.control.npc.FightRules.since(now, mind.reachedAt), false)
            || current instanceof ServerPlayer player && (player.m_7500_() || player.m_5833_() || partnerOf(mind, player)))) {
            current = null;
        }
        if (current == null && now % 5L == 0L) {
            current = seek(npc, mind, level, home, now);
            if (current != null) {
                mind.reachedAt = now; // a fresh fight: its time to get at it starts now
            }
        }
        int id = current == null ? 0 : current.m_19879_();
        if (id != mind.target) {
            if (id == 0 && mind.target != 0) {
                if (mind.phase == EtoRules.Phase.MANIAC) {
                    bark(npc, level, npc.persona().script().won(npc.m_217043_().m_188503_(100)));
                }
            }
            mind.target = id;
            mind.dirty = true;
        }
        return current;
    }

    /**
     * 1.3.19: whether {@code body} is a foe of this companion's (what its final kakuja's blades, shields and flesh go for): what it fights,
     * a monster, what hurt it or is out for it; a player only if fought or held a grudge against, never its partner; never a child, another
     * companion, a villager or a beast minding its own business.
     */
    public static boolean foe(EtoNpc npc, LivingEntity body) {
        Mind mind = npc.mind;
        if (body == npc || !body.m_6084_() || mind == null) {
            return false;
        }
        if (body instanceof EtoNpc || body instanceof com.hex.kagune.control.npc.ChildNpc) {
            return false;
        }
        if (body instanceof ServerPlayer player) {
            return !player.m_7500_() && !player.m_5833_() && !partnerOf(mind, player) && !spared(player, npc)
                && (mind.target == player.m_19879_() || mind.grudges.containsKey(player.m_20148_()));
        }
        if (com.hex.kagune.control.server.FleshThralls.ownedBy(body, npc)) {
            return false;
        }
        return body.m_19879_() == mind.target || body instanceof Enemy || npc.m_21188_() == body || body instanceof net.minecraft.world.entity.Mob mob
            && mob.m_5448_() == npc;
    }

    /**
     * 1.3.21: her night at the home she shares with her spouse: to the double bed (its free half; theirs is the other), lying down in it.
     * Not while she is with someone (a hand, a kiss, a hug, a child in her arms or a moment with one, a talk). True while it has her.
     */
    private static boolean toBed(EtoNpc npc, Mind mind, Households.Home house, ServerLevel level, long now) {
        if (house == null || !HomeLife.night(level.m_46468_(), false) || mind.holding != null || mind.kiss != null || mind.toward != null
            || mind.cradling != null || mind.momentChild != null || mind.talking != null || mind.with != 0) {
            mind.bed = null;
            return false;
        }
        if (comesForBabyNight(npc, mind, level, now)) {
            mind.bed = null;
            return false; // (her walk to them goes on in her calm: closeIn)
        }
        if (mind.bed == null || !HomeLife.free(level, mind.bed)) {
            mind.bed = HomeLife.spouseBed(level, house, now);
        }
        if (mind.bed == null) {
            return false;
        }
        if (mind.gesture != EtoGesture.NONE) {
            gesture(mind, EtoGesture.NONE, now);
        }
        HomeLife.toBed(npc, mind.bed, 0.75);
        if (npc.m_5803_()) {
            ServerPlayer partner = player(level, mind.partner);
            if (partner != null && partner.m_20280_(npc) < 16.0 * 16.0) {
                String line = switch (npc.persona().key()) {
                    case "touka" -> "*gets into bed and pulls the covers up* ...You coming or what?";
                    case "ken" -> "*lies down on his side of the bed* I kept your side warm.";
                    default -> "*curls up on her side of the bed* Come to bed, love. Don't make me wait.";
                };
                say(npc, partner, line);
                babyNightInBed(npc, mind, partner, now);
            }
            mind.dirty = true;
        }
        return true;
    }

    /**
     * 2.0.4 (the owner: after a first child, "you can request another but the cutscene is not triggering anymore whenever near the bed at
     * night"): at the home they share, she goes to bed at night instead of her calm, so the baby night ({@code together}) never came. Now,
     * lying in the shared bed, the night they asked for follows when they come to bed: within {@link #BED_NEAR} of her, or lying down
     * themselves close by (the double bed's other half).
     */
    private static void babyNightInBed(EtoNpc npc, Mind mind, ServerPlayer partner, long now) {
        if (now % 20L != 7L || now < mind.nextBabyNight) {
            return;
        }
        double d2 = partner.m_20280_(npc);
        if (d2 > BED_NEAR * BED_NEAR && !(partner.m_5803_() && d2 < BED_LYING * BED_LYING)) {
            return;
        }
        EtoTalk.Context c = context(npc, partner);
        if (babyNightDue(c)) {
            mind.nextBabyNight = now + 1200L;
            mind.babyNight = null;
            EtoServer.night(partner, npc, c);
        }
    }

    /**
     * 2.5.7 (the owner: after the first child, "the spouse says 'tonight it is' but when night comes she never initiates the action"): at
     * the home they share she went straight to bed at nightfall and only waited there (her coming to them, {@link #together}, is her calm's,
     * which the bed took the place of; before bedtime her chores held her). Now, the night they asked for, she comes to them first, out of
     * bed if she was in it, as she does away from home: the night follows when she takes their arm. Already beside her in bed (or lying
     * down by it), it follows as before ({@link #babyNightInBed}). True if she set off.
     */
    private static boolean comesForBabyNight(EtoNpc npc, Mind mind, ServerLevel level, long now) {
        if (now % 20L != 11L || now < mind.nextBabyNight || mind.partner == null) {
            return false;
        }
        ServerPlayer partner = player(level, mind.partner);
        if (partner == null || !partner.m_6084_() || partner.m_9236_() != level || partner.m_5833_()) {
            return false;
        }
        double d2 = partner.m_20280_(npc);
        if (d2 > 32.0 * 32.0 || !reachable(npc, partner) || Sightings.finalOf(partner).terror() || Engaged.busy(partner, npc.m_19879_(), now)
            || npc.m_5803_() && d2 <= BED_NEAR * BED_NEAR || partner.m_5803_() && d2 < BED_LYING * BED_LYING) {
            return false;
        }
        EtoTalk.Context c = context(npc, partner);
        if (!babyNightDue(c)) {
            return false;
        }
        mind.nextBabyNight = now + 1200L;
        mind.babyNight = partner.m_20148_();
        HomeLife.wake(npc);
        say(npc, partner, npc.persona().script().babyNight(c));
        alert(npc, partner, Romance.Want.NIGHT, "");
        own(npc, mind, partner, EtoGesture.CLING, now);
        return true;
    }

    /** The night they asked for a baby is due: dark, not had tonight, married, a woman and a man, nothing on the way yet. */
    static boolean babyNightDue(EtoTalk.Context c) {
        return c.night() && !c.nightUsed() && c.married() && c.mixed() && c.family().wantsBaby() && !c.pregnant();
    }

    /** 2.0.4: how near the bed they come for the baby night (blocks from her), and how near lying down in it. */
    static final double BED_NEAR = 3.0;
    static final double BED_LYING = 6.0;

    /**
     * 1.3.24: a hostile mob to take on: a monster; a neutral one (an enderman, a zombified piglin, a spider by day) only once it is after
     * someone (never one minding its own business: a whole swarm would come).
     */
    static boolean hostile(LivingEntity living) {
        if (!(living instanceof Enemy)) {
            return false;
        }
        return !(living instanceof net.minecraft.world.entity.NeutralMob) || living instanceof net.minecraft.world.entity.Mob mob && mob.m_5448_() != null;
    }

    /** 1.3.21: the player this companion is married to (null: none). */
    public static UUID spouse(EtoNpc npc) {
        Mind mind = npc.mind;
        return mind != null && mind.stage == Romance.Stage.MARRIED ? mind.partner : null;
    }

    /** Something to fight: a hostile within reach of her, a player she holds a grudge against or who is her enemy, near her home. */
    private static LivingEntity seek(EtoNpc npc, Mind mind, ServerLevel level, Vec3 home, long now) {
        mind.grudges.values().removeIf(until -> until < now);
        AABB around = new AABB(home.f_82479_ - EtoRules.LEASH, home.f_82480_ - 3.0, home.f_82481_ - EtoRules.LEASH, home.f_82479_ + EtoRules.LEASH,
            home.f_82480_ + 4.0, home.f_82481_ + EtoRules.LEASH);
        LivingEntity best = null;
        double nearest = Double.MAX_VALUE;
        for (LivingEntity living : level.m_6443_(LivingEntity.class, around, living -> living instanceof ServerPlayer && living.m_6084_())) {
            double distance = living.m_20280_(npc);
            ServerPlayer player = (ServerPlayer)living;
            // 1.3.2: her partner, never; whatever they did.
            boolean fights = !player.m_7500_() && !player.m_5833_() && !partnerOf(mind, player) && (mind.grudges.containsKey(player.m_20148_())
                || QuestServer.bond(player, npc.persona().key()).regard() <= NpcBonds.ENEMY);
            if (fights && distance < nearest) {
                nearest = distance;
                best = living;
            }
        }
        // 1.3.24: a hostile mob within five blocks of her (it was three: she let them come right up to her).
        double notice = com.hex.kagune.control.npc.FightRules.NOTICE;
        for (LivingEntity living : level.m_6443_(LivingEntity.class, npc.m_20191_().m_82400_(notice), living -> living != npc && living.m_6084_()
            && hostile(living))) {
            double distance = living.m_20280_(npc);
            if (com.hex.kagune.control.npc.FightRules.notices(Math.sqrt(distance)) && distance < nearest) {
                nearest = distance;
                best = living;
            }
        }
        // 1.3.15: anything hostile near one of her children (or after one) is hers to deal with.
        if (best == null) {
            best = ParentCare.threat(npc, level);
        }
        // 1.3.31: at the home she shares with her spouse, anything hostile on its ground is too (in her sight, close by the house, or
        // after one of hers).
        if (best == null && npc.following() == null) {
            Households.Home house = Households.forCompanion(level, npc);
            if (house != null) {
                best = guard(npc, mind, level, house);
            }
        }
        return best;
    }

    /** 1.3.31: the hostile on her home's ground she should see to (the nearest to her), or null. */
    static LivingEntity guard(EtoNpc npc, Mind mind, ServerLevel level, Households.Home house) {
        BlockPos c = house.pos();
        int r = house.radius();
        AABB ground = new AABB(c.m_123341_() - r, c.m_123342_() - 5, c.m_123343_() - r, c.m_123341_() + r + 1, c.m_123342_() + 7, c.m_123343_() + r + 1);
        ServerPlayer partner = player(level, mind.partner);
        LivingEntity best = null;
        double nearest = Double.MAX_VALUE;
        // 1.3.37: and a CCG investigator anywhere near the house: a ghoul's home has no worse a threat (the owner: "if it senses a mob or
        // a CCG agent nearby, it will get up to fight").
        for (LivingEntity body : level.m_6443_(LivingEntity.class, ground, body -> body.m_6084_()
            && (body instanceof Enemy && hostile(body) || body instanceof com.hex.kagune.control.npc.CcgAgent))) {
            double toHouse = body.m_20238_(new Vec3(c.m_123341_() + 0.5, c.m_123342_(), c.m_123343_() + 0.5));
            boolean after = body instanceof net.minecraft.world.entity.Mob mob && mob.m_5448_() != null
                && (mob.m_5448_() == partner || mob.m_5448_() instanceof com.hex.kagune.control.npc.ChildNpc || mob.m_5448_() == npc);
            if (!(after || toHouse < 10.0 * 10.0 || npc.m_142582_(body))) {
                continue;
            }
            double distance = body.m_20280_(npc);
            if (distance < nearest) {
                nearest = distance;
                best = body;
            }
        }
        return best;
    }

    /**
     * She was hurt: whoever did it is fought, and a player who did it loses her regard. 1.3.2: her partner is never fought back: she
     * is hurt, says so, and sulks.
     */
    @SubscribeEvent
    public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        // 1.3.24: a blow she landed on what she fights (her spikes, her kagune from afar): she is getting at it, she keeps at it.
        Entity by = event.getSource() == null ? null : event.getSource().m_7639_();
        if (by instanceof EtoNpc fighter && fighter.mind != null && fighter.mind.target == event.getEntity().m_19879_()
            && fighter.m_9236_() instanceof ServerLevel at) {
            fighter.mind.reachedAt = at.m_46467_();
        }
        if (by instanceof ServerPlayer hitter && !(event.getEntity() instanceof EtoNpc) && hitter.m_9236_() instanceof ServerLevel at) {
            STRUCK.put(hitter.m_20148_(), at.m_46467_()); // 2.2.0: in the middle of a fight (a blow on her then is an accident)
        }
        if (event.getEntity() instanceof ServerPlayer victim) {
            protect(victim, event.getSource());
            return;
        }
        if (event.getEntity() instanceof com.hex.kagune.control.npc.ChildNpc child) {
            childHurt(child, event.getSource());
            return;
        }
        if (!(event.getEntity() instanceof EtoNpc npc)) {
            return;
        }
        DamageSource source = event.getSource();
        Mind mind = npc.mind;
        if (mind == null || !(npc.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        long now = level.m_46467_();
        mind.hurtAt = now;
        if (KenPeace.wears(npc)) {
            // 1.3.19: in Peace: its shields take what they meet, and it takes off a share of the rest (nothing while it comes over him).
            event.setAmount(KenPeace.hurt(npc, source.m_7639_(), source.m_7640_(), event.getAmount()));
        }
        Entity cause = source.m_7639_();
        if (!(cause instanceof LivingEntity attacker) || attacker == npc) {
            return;
        }
        if (attacker instanceof ServerPlayer player) {
            QuestLog.Bond bond = QuestServer.bond(player, npc.persona().key());
            if (partnerOf(mind, player)) {
                // 2.2.0: tolerance: a first blow is an accident (more so mid-fight); only more of them, close together, put her out.
                boolean fighting = now - STRUCK.getOrDefault(player.m_20148_(), Long.MIN_VALUE / 2) <= FIGHTING;
                com.hex.kagune.control.npc.Tolerance.Reaction reaction = com.hex.kagune.control.npc.Tolerance.hit(offences(mind, player, now), fighting);
                offend(mind, player, now);
                int seed = npc.m_217043_().m_188503_(100);
                QuestServer.bond(player, npc.persona().key(), bond.regard(bond.regard() + reaction.regard));
                if (reaction == com.hex.kagune.control.npc.Tolerance.Reaction.SHRUG) {
                    if (until(mind.sulk, player.m_20148_()) < now && now - mind.shrugAt > 40L) {
                        mind.shrugAt = now;
                        say(npc, player, com.hex.kagune.control.npc.Tolerance.shrug(npc.persona().key(), seed));
                    }
                } else {
                    if (until(mind.sulk, player.m_20148_()) < now) {
                        say(npc, player, npc.persona().script().partnerHit(seed, mind.stage == Romance.Stage.MARRIED));
                        gesture(mind, EtoGesture.POUT, now);
                    }
                    mind.sulk.put(player.m_20148_(), Math.max(until(mind.sulk, player.m_20148_()), now + reaction.sulk));
                    mind.makeUp.put(player.m_20148_(), "make_up"); // 2.2.3
                }
                endKiss(npc, mind, null, now);
                return;
            }
            QuestServer.bond(player, npc.persona().key(), bond.regard(bond.regard() + EtoRules.BLOW));
            mind.grudges.put(player.m_20148_(), now + 400L);
            if (player.m_20148_().equals(mind.talking)) {
                mind.talking = null;
            }
        }
        if (mind.target == 0) {
            mind.target = attacker.m_19879_();
            mind.reachedAt = now; // 1.3.24: a fresh fight
            mind.dirty = true;
        }
    }

    /**
     * 1.3.4: nothing hurts her through the Speed Kick's lunge and dash (as a Pale player's). 1.3.14: nor does anything the one whose hand
     * she holds does (or the one she clings to, hugs, kisses): their kagune out, their fists, their shots, whatever they swing.
     */
    @SubscribeEvent
    public static void attacked(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (!(event.getEntity() instanceof EtoNpc npc) || npc.mind == null) {
            return;
        }
        if (EtoOwl.untouchable(npc.mind.owl, npc.m_9236_().m_46467_())) {
            event.setCanceled(true);
            return;
        }
        DamageSource source = event.getSource();
        if (source != null && (source.m_7639_() instanceof ServerPlayer by && spared(by, npc)
            || source.m_7640_() instanceof ServerPlayer direct && spared(direct, npc))) {
            event.setCanceled(true);
        }
    }

    /**
     * 1.3.14: true if {@code body} is a companion holding {@code player}'s hand (or clinging to them, hugging them, kissing them): nothing
     * they do may touch her. The original kagune's own target checks ask this too (KagunePatch), so its tails and swipes pass her by.
     */
    public static boolean spared(ServerPlayer player, LivingEntity body) {
        try {
            if (player == null || !(body instanceof EtoNpc npc) || npc.mind == null) {
                return false;
            }
            Mind mind = npc.mind;
            return player.m_20148_().equals(mind.holding) || mind.with != 0 && mind.with == player.m_19879_();
        } catch (RuntimeException failure) {
            return false;
        }
    }

    static String capital(String word) {
        return word == null || word.isEmpty() ? "" : Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    /** 1.3.2: her partner (or the one she walks with) hurt where she can see who did it: she goes for them. */
    private static void protect(ServerPlayer victim, DamageSource source) {
        if (!(source.m_7639_() instanceof LivingEntity attacker) || attacker == victim || attacker instanceof EtoNpc) {
            return;
        }
        for (EtoNpc npc : LIVE) {
            Mind mind = npc.mind;
            if (mind == null || npc.m_213877_() || npc.m_9236_() != victim.m_9236_() || npc.m_20280_(victim) > 16.0 * 16.0) {
                continue;
            }
            boolean hers = partnerOf(mind, victim) || victim.m_20148_().equals(npc.following());
            if (!hers || !npc.m_142582_(attacker) || attacker instanceof ServerPlayer player && partnerOf(mind, player)) {
                continue;
            }
            long now = npc.m_9236_().m_46467_();
            mind.target = attacker.m_19879_();
            mind.reachedAt = now; // 1.3.24: a fresh fight
            mind.dirty = true;
            endKiss(npc, mind, null, now);
            if (now - mind.protectedAt > 200L) {
                mind.protectedAt = now;
                bark(npc, (ServerLevel)npc.m_9236_(), npc.persona().script().protect(context(npc, victim)));
            }
        }
    }

    /** 1.3.15: one of her children hurt: whoever did it is hers, now, wherever she can see them (a parent's partner never hurts it). */
    private static void childHurt(com.hex.kagune.control.npc.ChildNpc child, DamageSource source) {
        if (child.mind != null && child.m_9236_() instanceof ServerLevel hurtIn) {
            child.mind.hurtAt = hurtIn.m_46467_(); // 1.3.21: it wakes
        }
        if (!(source.m_7639_() instanceof LivingEntity attacker) || attacker instanceof EtoNpc || !(child.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        for (EtoNpc npc : LIVE) {
            Mind mind = npc.mind;
            if (mind == null || npc.m_213877_() || npc.m_9236_() != level || npc.m_20280_(child) > ParentCare.LOOKS * ParentCare.LOOKS
                || !ParentCare.parentOf(npc, child, level) || attacker instanceof ServerPlayer player && partnerOf(mind, player)) {
                continue;
            }
            long now = level.m_46467_();
            mind.target = attacker.m_19879_();
            mind.reachedAt = now; // 1.3.24: a fresh fight
            mind.dirty = true;
            if (attacker instanceof ServerPlayer player) {
                mind.grudges.put(player.m_20148_(), now + 1200L);
            }
            if (now - mind.protectedAt > 200L) {
                mind.protectedAt = now;
                bark(npc, level, npc.persona().female() ? "Get AWAY from my child!" : "Touch my child again. I dare you.");
            }
        }
    }

    // ---------------------------------------------------------------- her fight

    private static void fight(EtoNpc npc, Mind mind, LivingEntity target, ServerLevel level, long now) {
        mind.foughtAt = now;
        mind.talking = null;
        float health = npc.m_21223_() / Math.max(1.0F, npc.m_21233_());
        EtoRules.Phase phase = EtoRules.Phase.of(health);
        if (phase.ordinal() > mind.phase.ordinal()) {
            int seed = npc.m_217043_().m_188503_(100);
            bark(npc, level, phase == EtoRules.Phase.MANIAC ? npc.persona().script().maniac(seed) : npc.persona().script().serious(seed));
            if (phase == EtoRules.Phase.MANIAC) {
                gesture(mind, EtoGesture.LAUGH, now);
            }
        }
        mind.phase = phase;
        if (now % 40L == 0L) {
            if (phase.speed >= 0) {
                npc.m_7292_(new MobEffectInstance(MobEffects.f_19596_, 60, phase.speed, false, false, false));
            }
            if (phase.resistance >= 0) {
                npc.m_7292_(new MobEffectInstance(MobEffects.f_19606_, 60, phase.resistance, false, false, false));
            }
        }
        npc.m_21563_().m_24960_(target, 30.0F, 30.0F);
        if (NpcWings.fight(npc, mind, target, level, now)) {
            return; // 1.3.19: Touka's crystal spikes
        }
        if (KenPeace.fight(npc, mind, target, level, now, phase)) {
            return; // 1.3.19: Ken in Peace
        }
        EtoOwl.tick(npc, mind, target, level, now, phase);
    }

    // ---------------------------------------------------------------- calm

    private static void calm(EtoNpc npc, Mind mind, ServerLevel level, long now) {
        if (mind.holding == null) {
            solid(npc, player(level, mind.partner)); // (a hold dropped some other way)
        }
        // 1.3.39 (the owner: talked to, they walked away): her talk holds her as long as it is open on their screen, not 30 seconds.
        ServerPlayer open = player(level, mind.talking);
        if (open != null && EtoServer.talkingWith(open, npc) && open.m_20280_(npc) <= 10.0 * 10.0) {
            mind.talkingUntil = Math.max(mind.talkingUntil, now + 40L);
        }
        if (mind.talking != null && now > mind.talkingUntil) {
            mind.talking = null;
        }
        ServerPlayer partner = player(level, mind.partner);
        if (partner != null && (partner.m_9236_() != level || partner.m_20280_(npc) > 24.0 * 24.0 || !partner.m_6084_())) {
            partner = null;
        }
        nearness(npc, mind, level, partner, now);
        if (mind.holding != null) {
            holdHands(npc, mind, level, now);
            return;
        }
        if (mind.toward != null || mind.kiss != null) {
            closeIn(npc, mind, level, now);
            return;
        }
        if (mind.gesture == EtoGesture.CLING && mind.with != 0) {
            // 1.3.3: wound round their arm: she stays at their side while she clings; walked off, she lets go.
            if (level.m_6815_(mind.with) instanceof ServerPlayer held && held.m_6084_() && held.m_20280_(npc) < 2.4 * 2.4) {
                stayBeside(npc, held, CLING_OUT, now);
                face(npc, held);
                npc.m_21563_().m_24960_(held, 30.0F, 30.0F);
                return;
            }
            gesture(mind, EtoGesture.NONE, now);
            mind.with = 0;
        }
        ServerPlayer talker = player(level, mind.talking);
        if (talker != null) {
            npc.m_21573_().m_26573_();
            face(npc, talker);
            npc.m_21563_().m_24960_(talker, 30.0F, 30.0F);
            return;
        }
        // 2.0.8 (the owner: "while an NPC is doing a task make sure it doesnt cancel it out to do another"): a chore under way comes before
        // her own errands (a straying child, a moment with her spouse, a friend's word, a kiss she'd ask for); they wait till it is done.
        if (mind.chore != null && mind.cradling == null && !HomeLife.night(level.m_46468_(), false)) {
            Households.Home keeping = npc.following() == null ? Households.forCompanion(level, npc) : null;
            if (keeping != null && HomeChores.tick(npc, mind, keeping, level, partner, now)) {
                return;
            }
        }
        // 1.3.49: a child of hers strayed too far, out at night, or on a rampage: she goes after it before anything else of her own.
        if (ParentDuty.tick(npc, mind, level, now)) {
            return;
        }
        if (partner != null && together(npc, mind, level, partner, now)) {
            return;
        }
        // 1.3.29: a friend near (not her partner): now and then she comes up to them with something (SceneServer).
        if (now % 100L == 61L && mind.target == 0 && mind.holding == null) {
            for (ServerPlayer player : level.m_6907_()) {
                if (player != partner && player.m_6084_() && !player.m_5833_() && player.m_20280_(npc) <= 10.0 * 10.0 && npc.m_142582_(player)
                    && SceneServer.friendTick(npc, player, now)) {
                    return;
                }
            }
        }
        // 1.3.29: at the home she shares with her spouse, by day, she keeps house (HomeChores). 1.3.31 (the owner: she only ever held the
        // child and talked): the house comes before an idle cuddle: a chore due, she puts the child down and gets on with it.
        Households.Home house = npc.following() == null ? Households.forCompanion(level, npc) : null;
        if (house != null && !HomeLife.night(level.m_46468_(), false)) {
            if (mind.cradling != null && mind.chore == null && now >= mind.nextChore && mind.momentChild == null) {
                ParentCare.drop(npc, mind, level, now);
            }
            if (mind.cradling == null && HomeChores.tick(npc, mind, house, level, partner, now)) {
                return;
            }
        }
        // 1.3.15: her children, while they are still children: she keeps close, and holds the little ones now and then.
        if (ParentCare.tend(npc, mind, level, partner, now)) {
            return;
        }
        if (now % 100L == 37L) {
            askForKiss(npc, mind, level, now);
            if (mind.toward != null) {
                return;
            }
        }
        if (now >= mind.nextGestureAt && mind.gesture == EtoGesture.NONE) {
            EtoMood mood = partner != null ? mood(npc, partner, now)
                : EtoMood.of(Romance.Stage.NONE, false, NpcBonds.FRIENDLY, level.m_46468_(), 0L, false, false, false, false, npc.m_217043_().m_188500_());
            EtoGesture[] idle = npc.persona().idles(mood);
            EtoGesture[] any = npc.persona().anyIdle();
            // Her mood's first, mostly; now and then anything at all (1.3.14: each companion's own).
            EtoGesture pick = npc.m_217043_().m_188500_() < 0.8 ? idle[npc.m_217043_().m_188503_(idle.length)]
                : any[npc.m_217043_().m_188503_(any.length)];
            gesture(mind, pick, now);
            if (pick.still()) {
                npc.m_21573_().m_26573_();
            }
            mind.nextGestureAt = now + pick.ticks + 120L + npc.m_217043_().m_188503_(240);
        } else if (mind.gesture.still() && mind.gesture != EtoGesture.NONE) {
            npc.m_21573_().m_26573_();
        }
    }

    /** Who has been near her, since when; her partner gone a while and back is greeted; when she last saw each of them is kept. */
    private static void nearness(EtoNpc npc, Mind mind, ServerLevel level, ServerPlayer partner, long now) {
        if (now % 20L != 11L) {
            return;
        }
        for (ServerPlayer player : level.m_6907_()) {
            UUID id = player.m_20148_();
            boolean near = player.m_6084_() && !player.m_5833_() && player.m_20280_(npc) <= 8.0 * 8.0;
            if (near) {
                mind.nearSince.putIfAbsent(id, now);
                if (now % 600L == 11L) {
                    NpcMemoryServer.set(player, npc.persona().key(), NpcMemory.SEEN, now);
                }
            } else {
                mind.nearSince.remove(id);
            }
        }
        boolean near = partner != null && partner.m_20280_(npc) <= 16.0 * 16.0;
        if (near && !mind.partnerNear) {
            if (mind.partnerNearAt != Long.MIN_VALUE && now - mind.partnerNearAt >= 6000L) {
                // 1.3.31: back after a while: she greets them once she actually sees them come in (never through a wall, never from
                // outside the house), within the next minute.
                mind.welcomeFrom = now;
            }
            NpcMemoryServer.set(partner, npc.persona().key(), NpcMemory.SEEN, now);
        }
        if (mind.welcomeFrom != Long.MIN_VALUE && (now - mind.welcomeFrom > 1200L || now < mind.welcomeFrom)) {
            mind.welcomeFrom = Long.MIN_VALUE;
        }
        if (mind.welcomeFrom != Long.MIN_VALUE && partner != null && mind.target == 0 && mind.kiss == null && mind.holding == null
            && SceneServer.sees(npc, partner, 12.0) && !Engaged.busy(partner, npc.m_19879_(), now)) { // 3.2.2: her welcome waits its turn
            mind.welcomeFrom = Long.MIN_VALUE;
            HomeChores.stop(npc, mind, level);
            EtoTalk.Context c = context(npc, partner);
            // 1.3.31: and if she cooked while they were out, she hands them the meal.
            String served = HomeKitchen.serve(npc, partner, level);
            say(npc, partner, served != null ? served : npc.persona().script().welcome(c));
            alert(npc, partner, Romance.Want.MISSED, "");
            if (reachable(npc, partner)) {
                own(npc, mind, partner, EtoGesture.HUG, now);
            }
        }
        mind.partnerNear = near;
        if (near) {
            mind.partnerNearAt = now;
        }
    }

    /**
     * Her partner is about: she keeps close (closer in a clingy mood), clings to them when they stand still near her, goes looking for
     * them with something to say, and late at night, in the mood, asks them to bed. True if that took her this tick.
     */
    private static boolean together(EtoNpc npc, Mind mind, ServerLevel level, ServerPlayer partner, long now) {
        EtoMood mood = mood(npc, partner, now);
        double distance = Math.sqrt(partner.m_20280_(npc));
        boolean clingy = mood == EtoMood.CLINGY || mood == EtoMood.AFFECTIONATE || mood == EtoMood.LONELY || mood == EtoMood.GIDDY;
        boolean reach = reachable(npc, partner);
        // 1.3.30 (the owner: at home the spouse never stopped following them, dropping everything): at the home they share it keeps to
        // its own day (its chores) and never trails them about; in the middle of a chore it starts nothing with them (they can still
        // talk to it, ask for a kiss, a hug, its hand).
        boolean home = npc.following() == null && Households.forCompanion(level, npc) != null;
        if (home && mind.chore != null) {
            return false;
        }
        // 1.3.31: nothing started with them that she can't see (through a wall, round a corner, from outside the house).
        if (!npc.m_142582_(partner)) {
            return false;
        }
        // 2.4.4 (the owner: "spouses don't like the War form"): nothing sweet started with them while they wear it (or the Dragon has
        // them); she keeps a step back from it.
        if (Sightings.finalOf(partner).terror()) {
            if (distance < 4.0 && now % 10L == 3L) {
                double[] away = com.hex.kagune.control.npc.HumanRules.fleeStep(npc.m_20185_(), npc.m_20189_(), partner.m_20185_(), partner.m_20189_(), 0.0, 3.0);
                npc.m_21573_().m_26519_(away[0], npc.m_20186_(), away[1], 1.0);
            }
            return false;
        }
        if (clingy && reach && distance > 3.5 && now % 15L == 0L && !mind.gesture.still() && mind.chore == null && !home) {
            npc.m_21573_().m_5624_(partner, 1.0);
        }
        if (!reach) {
            return false;
        }
        // 3.2.2: someone else has them now (or just let go): whatever she'd start by herself waits its turn.
        if (now % 5L == 0L && Engaged.busy(partner, npc.m_19879_(), now)) {
            mind.busyFor = now + 5L;
        }
        if (now < mind.busyFor) {
            return false;
        }
        // 1.3.14: they asked for a baby: once it is dark she comes to them herself, and the night (and the child) follows.
        if (now % 40L == 17L && now >= mind.nextBabyNight && distance < 16.0) {
            EtoTalk.Context c = context(npc, partner);
            if (babyNightDue(c)) {
                mind.nextBabyNight = now + 1200L;
                mind.babyNight = partner.m_20148_();
                say(npc, partner, npc.persona().script().babyNight(c));
                alert(npc, partner, Romance.Want.NIGHT, "");
                own(npc, mind, partner, EtoGesture.CLING, now);
                return true;
            }
        }
        if (mood == EtoMood.AFFECTIONATE && now >= mind.nextNightAsk && distance < 12.0) {
            mind.nextNightAsk = now + 24000L;
            EtoTalk.Context c = context(npc, partner);
            if (!c.nightUsed()) {
                say(npc, partner, npc.persona().script().approach(c));
                alert(npc, partner, Romance.Want.NIGHT, "");
                own(npc, mind, partner, EtoGesture.CLING, now);
                return true;
            }
        }
        // 2.2.3: over a sulk or a jealous spell: she comes to make it up herself (her script's make_up, jealous_over).
        if (now % 20L == 9L && distance < 14.0 && mind.holding == null && mind.kiss == null && mind.target == 0 && makeUp(npc, mind, partner, level, now)) {
            return true;
        }
        // 1.3.29: now and then she comes up to them with something to say (a scene: SceneServer) and waits for their answer.
        if (now % 20L == 13L && distance < 14.0 && mind.holding == null && mind.kiss == null && mind.target == 0
            && SceneServer.partnerTick(npc, partner, now)) {
            mind.nextSeek = Math.max(mind.nextSeek, now + 2400L);
            mind.initiativeAt = Math.max(mind.initiativeAt, now - com.hex.kagune.control.npc.EtoInitiative.EVERY / 2);
            return true;
        }
        // 1.3.12: unasked, she does something with them: their hand, their arm, a hug, a lean, a song, a page, a dance.
        if (now % 20L == 5L && mind.gesture != EtoGesture.CLING) {
            com.hex.kagune.control.npc.EtoInitiative.Act act = com.hex.kagune.control.npc.EtoInitiative.pick(mood, distance, mind.holding != null,
                now - mind.initiativeAt, npc.m_217043_().m_188500_() / Math.max(0.05, npc.persona().forward()), npc.m_217043_().m_188500_());
            if (act != com.hex.kagune.control.npc.EtoInitiative.Act.NONE) {
                mind.initiativeAt = now;
                // 1.3.29: each her own way (Touka: a kiss on the cheek, a kiss stolen; their own words as it lands).
                EtoGesture does = npc.persona().unasked(act.gesture);
                if (does == act.gesture) {
                    say(npc, partner, npc.persona().script().initiative(act, npc.m_217043_().m_188503_(1000)));
                }
                // 2.4.1 (the owner: "Spouse sometimes appear in the alert box 'Wants to talk to you' ... they DON'T want to talk to you"):
                // no alert for what she does by herself; it's seen.
                own(npc, mind, partner, does, now);
                return true;
            }
        }
        moments(npc, mind, level, partner, distance, now);
        long every = mood.seeks();
        if (every > 0L && now >= mind.nextSeek && distance < 16.0) {
            mind.nextSeek = now + every + npc.m_217043_().m_188503_((int)Math.max(1L, every / 2L));
            EtoTalk.Context c = context(npc, partner);
            // 2.4.1: a word as she comes over, and a wave: no "wants to talk" (she doesn't), and no grabbing hold of them every time.
            say(npc, partner, npc.persona().script().approach(c));
            own(npc, mind, partner, EtoGesture.WAVE, now);
            mind.initiativeAt = now;
            return true;
        }
        Long since = mind.nearSince.get(partner.m_20148_());
        // 2.4.1 (the owner: "they're not constantly running up to you kissing you and grabbing you ... its too unrealistic"): once in a
        // while at most (it was a 1 in 100 chance every tick she stood near them).
        if (clingy && distance < 4.0 && since != null && now - since > 600L && mind.gesture == EtoGesture.NONE
            && now - mind.initiativeAt >= com.hex.kagune.control.npc.EtoInitiative.EVERY && npc.m_217043_().m_188500_() < 0.0015) {
            mind.initiativeAt = now;
            // 1.3.29: Touka's way: a kiss on the cheek, now and then their face turned to hers and a kiss stolen.
            EtoGesture does = npc.persona().unasked(npc.m_217043_().m_188500_() < 0.3 ? EtoGesture.DANCE : EtoGesture.CLING);
            own(npc, mind, partner, does == EtoGesture.DANCE ? EtoGesture.CLING : does, now);
            return true;
        }
        return false;
    }

    /** 1.3.12: what she remarks on, unasked, with her partner near: the rain starting, dusk and dawn (once a day each), their wound. */
    private static void moments(EtoNpc npc, Mind mind, ServerLevel level, ServerPlayer partner, double distance, long now) {
        if (now % 40L != 9L || distance > 16.0) {
            return;
        }
        int seed = npc.m_217043_().m_188503_(1000);
        boolean raining = level.m_46471_();
        if (raining && !mind.rained) {
            say(npc, partner, npc.persona().script().moment(com.hex.kagune.control.npc.EtoInitiative.Moment.RAIN, true, seed));
        }
        mind.rained = raining;
        long time = Math.floorMod(level.m_46468_(), 24000L);
        long day = Math.floorDiv(level.m_46468_(), 24000L);
        if (time >= 12000L && time < 13000L && mind.duskDay != day) {
            mind.duskDay = day;
            say(npc, partner, npc.persona().script().moment(com.hex.kagune.control.npc.EtoInitiative.Moment.DUSK, true, seed));
        }
        if (time >= 23000L && mind.dawnDay != day) {
            mind.dawnDay = day;
            say(npc, partner, npc.persona().script().moment(com.hex.kagune.control.npc.EtoInitiative.Moment.DAWN, true, seed));
        }
        if (partner.m_21223_() < partner.m_21233_() * 0.4F && now - mind.hurtSaid > 1200L && distance < 12.0) {
            mind.hurtSaid = now;
            say(npc, partner, npc.persona().script().moment(com.hex.kagune.control.npc.EtoInitiative.Moment.HURT, true, seed));
            if (mind.gesture == EtoGesture.NONE && mind.holding == null) {
                own(npc, mind, partner, EtoGesture.HUG, now);
            }
        }
    }

    /** 1.3.12: something killed near her by a player she can see: she has a word for it (her partner's kill, warmer), once a minute. */
    @SubscribeEvent
    public static void killedNear(LivingDeathEvent event) {
        if (!(event.getSource().m_7639_() instanceof ServerPlayer killer) || event.getEntity() instanceof EtoNpc || !(killer.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        long now = level.m_46467_();
        for (EtoNpc npc : LIVE) {
            Mind mind = npc.mind;
            if (mind == null || npc.m_9236_() != level || npc.m_20280_(event.getEntity()) > 16.0 * 16.0 || mind.target != 0 || now - mind.killSaid < 1200L
                || !npc.m_142582_(event.getEntity())) {
                continue;
            }
            mind.killSaid = now;
            say(npc, killer, npc.persona().script().moment(com.hex.kagune.control.npc.EtoInitiative.Moment.KILL, partnerOf(mind, killer),
                npc.m_217043_().m_188503_(1000)));
        }
    }

    /** True if she can go to {@code player} without leaving home too far behind (or she follows them anyway). */
    private static boolean reachable(EtoNpc npc, ServerPlayer player) {
        if (player.m_20148_().equals(npc.following())) {
            return true;
        }
        double[] feet = npc.homeFeet();
        double dx = player.m_20185_() - feet[0];
        double dz = player.m_20189_() - feet[2];
        return dx * dx + dz * dz <= (EtoRules.LEASH + 1.0) * (EtoRules.LEASH + 1.0);
    }

    /**
     * 2.4.9 (the owner: "SOMETIMES asking for a kiss afterwards (not always and depends on their mood)"): having just handed {@code
     * partner} the food she made, whether she wants a kiss for it: never while she sulks or they're ignoring her, else by her mood's
     * readiness (Kitchen.kissAfterMeal; none at all jealous, sulking or wary) and her own way.
     */
    static boolean kissForMeal(EtoNpc npc, Mind mind, ServerPlayer partner, long now) {
        if (partner == null || mind.ignoring.containsKey(partner.m_20148_()) || until(mind.sulk, partner.m_20148_()) > now
            || mind.kiss != null || mind.target != 0 || Engaged.busy(partner, npc.m_19879_(), now)) { // 3.2.2: one at a time
            return false;
        }
        double chance = com.hex.kagune.control.family.Kitchen.kissAfterMeal(
            com.hex.kagune.control.npc.MoodActs.kissing(mood(npc, partner, now)), npc.persona().kissing());
        return npc.m_217043_().m_188500_() < chance;
    }

    /** 2.4.9: she asks {@code partner} for a kiss now (the same as when she wants one by herself: the alert, the walk up, theirs to turn down). */
    static void askKiss(EtoNpc npc, Mind mind, ServerPlayer partner, long now) {
        mind.nextAsk.put(partner.m_20148_(), now + EtoRules.KISS_ASK_EVERY);
        alert(npc, partner, Romance.Want.KISS, "");
        own(npc, mind, partner, EtoGesture.KISS, now);
        mind.asked = true;
    }

    /** Now and then, a player she is dating near her, and her twenty minutes since the last up: she wants a kiss. */
    private static void askForKiss(EtoNpc npc, Mind mind, ServerLevel level, long now) {
        mind.ignoring.values().removeIf(until -> until < now);
        ServerPlayer player = player(level, mind.partner);
        if (player == null || player.m_20280_(npc) > EtoRules.KISS_ASK_NEAR * EtoRules.KISS_ASK_NEAR || !player.m_6084_() || player.m_5833_()
            || !reachable(npc, player) || !npc.m_142582_(player)
            || mind.ignoring.containsKey(player.m_20148_()) || until(mind.sulk, player.m_20148_()) > now
            || now < mind.nextAsk.getOrDefault(player.m_20148_(), Long.MIN_VALUE) || Engaged.busy(player, npc.m_19879_(), now)) {
            return;
        }
        // 2.2.3: how readily by her mood (never when jealous; most when lonely or affectionate; little when sleepy or starving).
        double chance = EtoRules.KISS_ASK_CHANCE * com.hex.kagune.control.npc.MoodActs.kissing(mood(npc, player, now)) * npc.persona().kissing();
        if (npc.m_217043_().m_188500_() < chance) {
            mind.nextAsk.put(player.m_20148_(), now + EtoRules.KISS_ASK_EVERY);
            // 1.3.14: Touka and Ken, more often than not, just walk up and give a quick one themselves.
            if (npc.persona().pecks() && npc.m_217043_().m_188500_() < 0.65) {
                own(npc, mind, player, EtoGesture.PECK, now);
                return;
            }
            alert(npc, player, Romance.Want.KISS, "");
            say(npc, player, npc.persona().script().kissHint(npc.m_217043_().m_188503_(1000)));
            own(npc, mind, player, EtoGesture.KISS, now);
            mind.asked = true; // 2.3.9: hers to ask; theirs to turn down (walk off) without hurting her
        }
    }

    /**
     * 2.2.3: jealous of {@code partner} now: remembered, so she comes to make it up once it's passed; no longer cross or jealous with one
     * remembered: she comes up to them with it (true), if they are free and in sight.
     */
    private static boolean makeUp(EtoNpc npc, Mind mind, ServerPlayer partner, ServerLevel level, long now) {
        EtoMood mood = mood(npc, partner, now);
        UUID id = partner.m_20148_();
        if (mood == EtoMood.JEALOUS) {
            mind.makeUp.put(id, "jealous_over");
            return false;
        }
        String scene = mind.makeUp.get(id);
        if (scene == null || mood == EtoMood.SULKY) {
            return false;
        }
        // 3.3.7: one of her ways to come and make up (make_up, make_up_2...), not always the same.
        com.hex.kagune.control.npc.scene.Scene made = SceneServer.variant(npc.persona().key(), scene, npc.m_217043_().m_188503_(1000));
        if (made == null) {
            mind.makeUp.remove(id);
            return false;
        }
        if (SceneServer.approachMoment(npc, partner, made, now)) {
            mind.makeUp.remove(id);
            return true;
        }
        return false;
    }

    /** 2.2.0: when each player last struck something that wasn't a companion (mid-fight, a blow on her is an accident). */
    private static final Map<UUID, Long> STRUCK = WorldTimes.map();
    /** 2.2.0: struck something else this recently (ticks): fighting. */
    static final long FIGHTING = 60L;

    /** 2.2.0: how many offences of {@code player}'s she still holds at {@code now}. */
    static int offences(Mind mind, ServerPlayer player, long now) {
        long[] held = mind.offences.get(player.m_20148_());
        return held == null ? 0 : com.hex.kagune.control.npc.Tolerance.offences((int)held[0], held[1], now);
    }

    /** 2.2.0: one more offence of {@code player}'s, now. */
    private static void offend(Mind mind, ServerPlayer player, long now) {
        int before = offences(mind, player, now);
        mind.offences.put(player.m_20148_(), new long[] {before + 1L, now});
        if (STRUCK.size() > 256) {
            STRUCK.clear();
        }
    }

    /**
     * 2.2.0: {@code player} says sorry: all forgiven (true), or (hurt again and again) forgiven but she wants a moment first (false: a short
     * sulk in place of what she held).
     */
    static boolean apologised(EtoNpc npc, ServerPlayer player, long now) {
        Mind mind = npc.mind;
        if (mind == null) {
            return true;
        }
        com.hex.kagune.control.npc.Tolerance.Apology answer = com.hex.kagune.control.npc.Tolerance.apology(offences(mind, player, now));
        forgive(npc, player);
        mind.offences.remove(player.m_20148_());
        if (answer == com.hex.kagune.control.npc.Tolerance.Apology.A_MOMENT) {
            mind.sulk.put(player.m_20148_(), now + com.hex.kagune.control.npc.Tolerance.MOMENT);
            return false;
        }
        return true;
    }

    /** True if she is ignoring {@code player} (they left her hanging). */
    public static boolean ignoring(EtoNpc npc, ServerPlayer player, long now) {
        Mind mind = npc.mind;
        Long until = mind == null ? null : mind.ignoring.get(player.m_20148_());
        return until != null && until > now;
    }

    /** 1.3.2: she forgives {@code player}: no longer ignored, sulked at, held a grudge against or fought. */
    static void forgive(EtoNpc npc, ServerPlayer player) {
        Mind mind = npc.mind;
        if (mind == null) {
            return;
        }
        UUID id = player.m_20148_();
        mind.ignoring.remove(id);
        mind.sulk.remove(id);
        mind.grudges.remove(id);
        if (mind.target == player.m_19879_()) {
            mind.target = 0;
            mind.dirty = true;
        }
    }

    /**
     * 2.2.7: married to {@code player} by command ({@code /kagune <who> marry}): nothing held against them (no sulk, grudge, offence,
     * being ignored, make-up owed), giddy with them, the ring's gesture and hearts.
     */
    static void married(EtoNpc npc, ServerPlayer player, long now) {
        Mind mind = npc.mind;
        if (mind == null) {
            return;
        }
        forgive(npc, player);
        mind.offences.remove(player.m_20148_());
        mind.makeUp.remove(player.m_20148_());
        giddy(npc, player, now);
        gesture(mind, EtoGesture.RING, now);
        if (npc.m_9236_() instanceof ServerLevel level) {
            partner(npc, mind, level);
            hearts(level, npc, player, 12);
        }
        mind.dirty = true;
    }

    /** 1.3.2: something sweet happened with {@code player}: she smiles about it a while. */
    static void giddy(EtoNpc npc, ServerPlayer player, long now) {
        if (npc.mind != null) {
            npc.mind.giddy.put(player.m_20148_(), now + 3600L);
        }
    }

    /** She walks up to {@code player} for {@code what} (a kiss, a hug, to cling, to wave). */
    public static void walkUp(EtoNpc npc, Mind mind, ServerPlayer player, EtoGesture what, long now) {
        if (what == EtoGesture.CLING && !npc.persona().clings()) {
            what = EtoGesture.HUG; // 1.3.29: one who never winds round an arm (Touka) holds them properly instead
        }
        endKiss(npc, mind, null, now);
        mind.asked = false;
        if (npc.m_9236_() instanceof ServerLevel level) {
            ParentCare.drop(npc, mind, level, now);
        }
        mind.toward = player.m_20148_();
        mind.towardFor = what;
        mind.towardSince = now;
        mind.towardOwn = false;
        mind.talking = null;
        gesture(mind, EtoGesture.NONE, now);
    }

    /** 3.2.2: she walks up to {@code player} for {@code what} by herself (one at a time: {@link Engaged}; she checked before setting off). */
    private static void own(EtoNpc npc, Mind mind, ServerPlayer player, EtoGesture what, long now) {
        walkUp(npc, mind, player, what, now);
        mind.towardOwn = true;
    }

    /** Walking up to them, then the kiss, the hug, the clinging itself. */
    private static void closeIn(EtoNpc npc, Mind mind, ServerLevel level, long now) {
        ServerPlayer player = player(level, mind.toward);
        if (player == null || !player.m_6084_()) {
            endKiss(npc, mind, null, now);
            return;
        }
        // 3.2.2: on her way over by herself and someone else has them now (they asked for it, or got there first): another time.
        if (mind.towardOwn && mind.kiss == null && mind.with != player.m_19879_() && now % 5L == 0L && Engaged.taken(player, npc.m_19879_(), now)) {
            endKiss(npc, mind, null, now);
            return;
        }
        double distance = Math.sqrt(player.m_20280_(npc));
        face(npc, player);
        npc.m_21563_().m_24960_(player, 30.0F, 30.0F);
        if (mind.kiss == null) {
            boolean side = mind.towardFor == EtoGesture.CLING || mind.towardFor == EtoGesture.HAND;
            if (side) {
                // 1.3.3: to their side, close enough to take their arm (or their hand), not a grab at the air from a step away.
                Vec3 spot = beside(player, mind.towardFor == EtoGesture.HAND ? EtoHand.out(com.hex.kagune.control.Genders.female(player)) : CLING_OUT);
                double off = Math.sqrt(Math.pow(spot.f_82479_ - npc.m_20185_(), 2) + Math.pow(spot.f_82481_ - npc.m_20189_(), 2));
                if (off > 0.45 || distance > 1.25) {
                    if (now - mind.towardSince > 240L || distance > EtoRules.LEASH + 12.0) {
                        endKiss(npc, mind, null, now);
                        return;
                    }
                    if (now % 4L == 0L) {
                        npc.m_21573_().m_26519_(spot.f_82479_, spot.f_82480_, spot.f_82481_, 1.0);
                    }
                    return;
                }
            } else if (distance > EtoRules.KISS_NEAR) {
                if (now - mind.towardSince > 200L || distance > EtoRules.LEASH + 12.0) {
                    endKiss(npc, mind, null, now);
                    return;
                }
                npc.m_21573_().m_5624_(player, 1.0);
                return;
            }
            npc.m_21573_().m_26573_();
            int seed = npc.m_217043_().m_188503_(100);
            switch (mind.towardFor) {
                case HUG -> {
                    gesture(mind, EtoGesture.HUG, now);
                    mind.with = player.m_19879_();
                    hearts(level, npc, player, 6);
                    say(npc, player, npc.persona().script().hugged(seed));
                    giddy(npc, player, now);
                    mind.toward = null;
                    if (player.m_20148_().equals(mind.babyNight)) {
                        // 1.3.29: one who never clings (Touka) comes for the night they asked for with a hug instead.
                        mind.babyNight = null;
                        EtoServer.night(player, npc, context(npc, player));
                    }
                    return;
                }
                case STEAL -> {
                    // 1.3.29 (Touka): their face turned to hers (their view drawn to her while it lasts), a kiss, then a complaint.
                    gesture(mind, EtoGesture.STEAL, now);
                    mind.with = player.m_19879_();
                    hearts(level, npc, player, 6);
                    say(npc, player, npc.persona().script().stolen(seed));
                    NpcMemoryServer.add(player, npc.persona().key(), NpcMemory.KISSES, 1L);
                    giddy(npc, player, now);
                    RomanceServer.romanced(player, npc.persona().key());
                    mind.toward = null;
                    mind.dirty = true;
                    return;
                }
                case CLING -> {
                    gesture(mind, EtoGesture.CLING, now);
                    mind.with = player.m_19879_();
                    hearts(level, npc, player, 3);
                    giddy(npc, player, now);
                    mind.toward = null;
                    if (player.m_20148_().equals(mind.babyNight)) {
                        // 1.3.14: the night they asked for: the dark, and in the morning, maybe, a child on the way.
                        mind.babyNight = null;
                        EtoServer.night(player, npc, context(npc, player));
                    }
                    return;
                }
                case PECK -> {
                    // 1.3.14: a quick kiss of her own: in, out, a word, pink.
                    gesture(mind, EtoGesture.PECK, now);
                    mind.with = 0;
                    hearts(level, npc, player, 5);
                    say(npc, player, npc.persona().script().peck(seed));
                    NpcMemoryServer.add(player, npc.persona().key(), NpcMemory.KISSES, 1L);
                    giddy(npc, player, now);
                    RomanceServer.romanced(player, npc.persona().key());
                    mind.toward = null;
                    mind.dirty = true;
                    return;
                }
                case HAND -> {
                    // 1.3.3: her hand in theirs; she walks at their side until a tap of shift lets go.
                    mind.toward = null;
                    mind.holding = player.m_20148_();
                    mind.holdShift = player.m_6047_();
                    mind.with = player.m_19879_();
                    gesture(mind, EtoGesture.HAND, now);
                    hearts(level, npc, player, 4);
                    giddy(npc, player, now);
                    say(npc, player, npc.persona().script().handTaken(seed));
                    player.m_5661_(Component.m_237113_("\u00a7d\u2764 Holding hands with " + npc.displayName() + " \u00a77(tap Shift to let go)"), true);
                    return;
                }
                case KISS -> {
                    mind.kiss = EtoRules.Kiss.WAIT;
                    mind.kissAt = now;
                    mind.held = 0;
                    mind.with = player.m_19879_();
                    // 2.2.4: the kiss her mood gives (angry, possessive, giddy, tender, desperate, sleepy, hungry, teasing, or sweet).
                    mind.kissStyle = com.hex.kagune.control.npc.MoodKiss.of(mood(npc, player, now));
                    mind.dodged = false;
                    mind.dirty = true;
                    gesture(mind, EtoGesture.KISS, now);
                    String waiting = mind.kissStyle == com.hex.kagune.control.npc.MoodKiss.Style.SWEET
                        ? npc.persona().script().kissWaiting(npc.displayName())
                        : com.hex.kagune.control.npc.MoodKiss.waiting(mind.kissStyle, npc.displayName(), npc.persona().female() ? "her" : "his");
                    player.m_5661_(Component.m_237113_(npc.persona().colour() + "\u2764 " + waiting + " \u00a77(tap or hold Shift)"), true);
                    return;
                }
                default -> {
                    gesture(mind, mind.towardFor == EtoGesture.NONE ? EtoGesture.WAVE : mind.towardFor, now);
                    mind.toward = null;
                    return;
                }
            }
        }
        npc.m_21573_().m_26573_();
        if (now < mind.kissAt) {
            return; // 2.2.4: a teasing one, back from her dodge, about to lean in again
        }
        if (now == mind.kissAt && mind.dodged && mind.kiss == EtoRules.Kiss.WAIT) {
            gesture(mind, EtoGesture.KISS, now);
        }
        // 2.3.9 (the owner: "sometimes your spouse gets stuck in the kiss-asking animation when you don't really want one ... you press
        // shift on her and ... nothing happened"): never left leaning in for long whatever happens (twice her wait at most), and one she
        // came asking for herself let go without a grudge.
        if (mind.kiss == EtoRules.Kiss.WAIT && now - mind.kissAt > 2L * com.hex.kagune.control.npc.MoodKiss.waits(mind.kissStyle) + 40L) {
            letGoOfKiss(npc, mind, player, now);
            return;
        }
        boolean shift = player.m_6047_();
        int released = !shift && mind.held > 0 ? mind.held : 0;
        mind.held = shift ? mind.held + 1 : 0;
        EtoRules.Kiss before = mind.kiss;
        // 2.2.4: how long she waits is her mood's (the angry won't wait; the sleepy forget to stop).
        long waited = (now - mind.kissAt) * EtoRules.KISS_WAIT / Math.max(1, com.hex.kagune.control.npc.MoodKiss.waits(mind.kissStyle));
        EtoRules.Kiss after = EtoRules.kiss(before, waited, shift, mind.held, released, distance);
        if (after == EtoRules.Kiss.KISSED && before == EtoRules.Kiss.WAIT && com.hex.kagune.control.npc.MoodKiss.dodges(mind.kissStyle) && !mind.dodged) {
            // 2.2.4: teasing: the first try dodged, laughing; she leans in again in a moment.
            mind.dodged = true;
            mind.held = 0;
            mind.kissAt = now + 12L;
            gesture(mind, EtoGesture.HOP, now);
            say(npc, player, com.hex.kagune.control.npc.MoodKiss.dodged(npc.persona().key(), npc.m_217043_().m_188503_(100)));
            return;
        }
        if (after == before) {
            if (before == EtoRules.Kiss.BLUSH && now % 15L == 0L) {
                hearts(level, npc, player, 1);
            }
            if (before == EtoRules.Kiss.PULL) {
                pullIn(npc, player, level, now);
            }
            return;
        }
        int seed = npc.m_217043_().m_188503_(100);
        switch (after) {
            case BLUSH -> {
                mind.kiss = EtoRules.Kiss.BLUSH;
                mind.kissAt = now;
                gesture(mind, EtoGesture.BLUSH, now);
                hearts(level, npc, player, 3);
            }
            case PULL -> {
                // Held long enough: her arms round their head, and she pulls them in.
                mind.kiss = EtoRules.Kiss.PULL;
                gesture(mind, EtoGesture.PULL, now);
                hearts(level, npc, player, 8);
                pullIn(npc, player, level, now);
            }
            case KISSED -> {
                com.hex.kagune.control.npc.MoodKiss.Style style = mind.kissStyle;
                String moody = com.hex.kagune.control.npc.MoodKiss.kissed(npc.persona().key(), style, before == EtoRules.Kiss.BLUSH,
                    before == EtoRules.Kiss.PULL, seed);
                String line = moody != null ? moody : before == EtoRules.Kiss.PULL ? npc.persona().script().pulled(seed)
                    : before == EtoRules.Kiss.BLUSH ? npc.persona().script().held(seed) : npc.persona().script().kissed(seed, mind.stage == Romance.Stage.MARRIED);
                say(npc, player, line);
                NpcMemoryServer.add(player, npc.persona().key(), NpcMemory.KISSES, 1L);
                giddy(npc, player, now);
                RomanceServer.romanced(player, npc.persona().key());
                if (before == EtoRules.Kiss.PULL) {
                    hearts(level, npc, player, (int)Math.round(14 * com.hex.kagune.control.npc.MoodKiss.hearts(style)));
                }
                // 2.2.4: an angry kiss ends the sulk; a possessive one the jealousy.
                if (style == com.hex.kagune.control.npc.MoodKiss.Style.ANGRY) {
                    forgive(npc, player);
                    mind.makeUp.remove(player.m_20148_());
                }
                if (style == com.hex.kagune.control.npc.MoodKiss.Style.POSSESSIVE) {
                    NpcMemoryServer.of(player).forget(npc.persona().key(), NpcMemory.JEALOUS);
                    NpcMemoryServer.save(player);
                    mind.makeUp.remove(player.m_20148_());
                }
                endKiss(npc, mind, player, now);
                EtoGesture then = com.hex.kagune.control.npc.MoodKiss.after(style);
                if (then != null) {
                    gesture(mind, then, now); // what she does once it's over: arms crossed, clinging, holding on, leaning, licking her lips, off
                    if (then == EtoGesture.HUG || then == EtoGesture.CLING || then == EtoGesture.LEAN) {
                        mind.with = player.m_19879_();
                    }
                }
                mind.kissStyle = com.hex.kagune.control.npc.MoodKiss.Style.SWEET;
            }
            default -> {
                if (mind.asked) {
                    letGoOfKiss(npc, mind, player, now); // 2.3.9: hers to ask; not wanted now is no offence
                    return;
                }
                // Left hanging. 2.2.0: the first time shrugged off; only more of them put her out, and not for long.
                mind.kiss = null;
                mind.toward = null;
                com.hex.kagune.control.npc.Tolerance.Reaction reaction = com.hex.kagune.control.npc.Tolerance.missed(offences(mind, player, now));
                offend(mind, player, now);
                if (reaction == com.hex.kagune.control.npc.Tolerance.Reaction.SHRUG) {
                    gesture(mind, EtoGesture.POUT, now);
                    say(npc, player, com.hex.kagune.control.npc.Tolerance.shrugKiss(npc.persona().key(), seed));
                } else {
                    mind.ignoring.put(player.m_20148_(), now + reaction.sulk);
                    mind.makeUp.put(player.m_20148_(), "make_up"); // 2.2.3
                    gesture(mind, EtoGesture.ANGRY, now);
                    say(npc, player, npc.persona().script().hanging(seed));
                    player.m_5661_(Component.m_237113_(npc.displayName() + " is put out with you. Say sorry (talk to " + npc.persona().her() + "), or give "
                        + npc.persona().her() + " a minute."), true);
                }
            }
        }
    }

    /**
     * 2.3.9: a kiss she leaned in for that isn't happening (they walked off, or it ran on too long): she straightens up and lets it go with
     * a shrug of her own (Tolerance's), no grudge, no sulk.
     */
    static void letGoOfKiss(EtoNpc npc, Mind mind, ServerPlayer player, long now) {
        mind.kiss = null;
        mind.toward = null;
        mind.held = 0;
        mind.asked = false;
        mind.with = 0;
        mind.kissStyle = com.hex.kagune.control.npc.MoodKiss.Style.SWEET;
        gesture(mind, EtoGesture.POUT, now);
        say(npc, player, com.hex.kagune.control.npc.Tolerance.shrugKiss(npc.persona().key(), npc.m_217043_().m_188503_(100)));
        mind.dirty = true;
    }

    /** How far to their right she stands to cling to their arm, and to hold their hand. */
    static final double CLING_OUT = 0.55;
    static final double HAND_OUT = EtoHand.OUT;
    private static final EtoHand HAND = new EtoHand();

    /** The spot {@code out} blocks to {@code player}'s right, level with them. */
    private static Vec3 beside(ServerPlayer player, double out) {
        double yaw = Math.toRadians(player.m_146908_());
        return new Vec3(player.m_20185_() - Math.cos(yaw) * out, player.m_20186_(), player.m_20189_() - Math.sin(yaw) * out);
    }

    /** She keeps to the spot {@code out} to {@code player}'s right by walking, including when left behind or on a different floor. */
    private static void stayBeside(EtoNpc npc, ServerPlayer player, double out, long now) {
        Vec3 spot = beside(player, out);
        double dx = spot.f_82479_ - npc.m_20185_();
        double dz = spot.f_82481_ - npc.m_20189_();
        double off = Math.sqrt(dx * dx + dz * dz);
        if (off > 0.3 || Math.abs(spot.f_82480_ - npc.m_20186_()) > 0.5) {
            if (now % 3L == 0L) {
                npc.m_21573_().m_26519_(spot.f_82479_, spot.f_82480_, spot.f_82481_, Math.min(1.7, 0.9 + off * 0.35));
            }
        } else {
            npc.m_21573_().m_26573_();
        }
    }

    /** 1.3.3: walking hand in hand: at their side, facing the way they face; a tap of shift (or them gone) lets go. */
    private static void holdHands(EtoNpc npc, Mind mind, ServerLevel level, long now) {
        ServerPlayer player = player(level, mind.holding);
        if (player == null || !player.m_6084_() || player.m_5833_() || player.m_20280_(npc) > 12.0 * 12.0) {
            letGo(npc, mind, level, now, false);
            return;
        }
        boolean shift = player.m_6047_();
        if (shift && !mind.holdShift) {
            letGo(npc, mind, level, now, true);
            return;
        }
        mind.holdShift = shift;
        if (mind.gesture != EtoGesture.HAND) {
            gesture(mind, EtoGesture.HAND, now);
        }
        mind.with = player.m_19879_();
        // 1.3.4: hand in hand, as one: kept at their side, every tick, facing the way their body faces (every client draws her glued
        // there too, EtoRenderer); only where there is no room beside them does she walk to catch up.
        float yaw = player.f_20883_;
        boolean woman = com.hex.kagune.control.Genders.female(player);
        Vec3 spot = HAND.spot(player.m_20185_(), player.m_20186_(), player.m_20189_(), yaw, woman);
        // 1.3.39 (the owner: hand in hand they suffocated in blocks: "just disable collision with blocks altogether while holding hands"):
        // always at their side, through whatever is there, never pushed by it nor choked in it, nor falling.
        npc.f_19794_ = true;
        npc.m_20242_(true);
        npc.m_21573_().m_26573_();
        npc.m_6021_(spot.f_82479_, spot.f_82480_, spot.f_82481_);
        npc.m_20256_(player.m_20184_());
        npc.f_19789_ = 0.0F;
        npc.m_146922_(yaw);
        npc.f_20883_ = yaw;
        npc.f_20885_ = yaw;
        if (now % 80L == 0L) {
            hearts(level, npc, player, 1);
        }
    }

    /**
     * 1.3.39: back to a body that collides and falls, after a hand let go (or a hold dropped any other way); if it was let go inside a
     * wall, set down on the feet of the one who held it ({@code by}), never left choking in it.
     */
    static void solid(net.minecraft.world.entity.Mob body, LivingEntity by) {
        if (!body.f_19794_) {
            return;
        }
        body.f_19794_ = false;
        body.m_20242_(false);
        if (body.m_9236_() instanceof ServerLevel world && !world.m_45756_(body, body.m_20191_()) && by != null && by.m_9236_() == world) {
            body.m_6021_(by.m_20185_(), by.m_20186_(), by.m_20189_());
        }
    }

    /** She lets go of their hand ({@code said}: they let go, and she says something about it). */
    private static void letGo(EtoNpc npc, Mind mind, ServerLevel level, long now, boolean said) {
        if (mind.holding == null) {
            return;
        }
        ServerPlayer player = player(level, mind.holding);
        mind.holding = null;
        mind.with = 0;
        solid(npc, player);
        if (mind.gesture == EtoGesture.HAND) {
            gesture(mind, EtoGesture.NONE, now);
        }
        npc.m_21573_().m_26573_();
        if (npc.following() == null) {
            npc.settle();
        }
        if (said && player != null) {
            say(npc, player, npc.persona().script().handLet(npc.m_217043_().m_188503_(100)));
        }
        mind.dirty = true;
    }

    /** True if a player is about her (company: no reason to go home). */
    /** 3.0.4: true if a player here has met companion {@code key} (one who met her before the world kept it). */
    private static boolean metByAnyone(ServerLevel level, String key) {
        for (ServerPlayer player : level.m_6907_()) {
            if (QuestServer.bond(player, key).has(NpcBonds.MET)) {
                UniqueNpcs.met(level, key);
                return true;
            }
        }
        return false;
    }

    private static boolean someoneAbout(EtoNpc npc, ServerLevel level) {
        for (ServerPlayer player : level.m_6907_()) {
            if (!player.m_5833_() && player.m_20280_(npc) <= 24.0 * 24.0) {
                return true;
            }
        }
        return false;
    }

    /** Pulled in: they are drawn to just in front of her face (their view is turned to hers on their client). */
    private static void pullIn(EtoNpc npc, ServerPlayer player, ServerLevel level, long now) {
        double yaw = Math.toRadians(npc.f_20883_);
        Vec3 spot = new Vec3(npc.m_20185_() - Math.sin(yaw) * 0.6, player.m_20186_(), npc.m_20189_() + Math.cos(yaw) * 0.6);
        Vec3 gap = spot.m_82546_(player.m_20182_());
        double flat = Math.sqrt(gap.f_82479_ * gap.f_82479_ + gap.f_82481_ * gap.f_82481_);
        if (flat > 0.12) {
            double pull = Math.min(0.18, flat * 0.35);
            player.m_20256_(new Vec3(gap.f_82479_ / flat * pull, player.m_20184_().f_82480_, gap.f_82481_ / flat * pull));
            player.f_19864_ = true;
        }
        if (now % 10L == 0L) {
            hearts(level, npc, player, 2);
        }
    }

    /** The kiss (or the walk up) over: kissed ({@code kissed} not null: hearts), or simply dropped. */
    private static void endKiss(EtoNpc npc, Mind mind, ServerPlayer kissed, long now) {
        if (mind.toward == null && mind.kiss == null) {
            return;
        }
        mind.toward = null;
        mind.kiss = null;
        mind.held = 0;
        if (kissed != null && npc.m_9236_() instanceof ServerLevel level) {
            gesture(mind, EtoGesture.KISSED, now);
            hearts(level, npc, kissed, 8);
        } else if (mind.gesture == EtoGesture.KISS || mind.gesture == EtoGesture.BLUSH || mind.gesture == EtoGesture.PULL) {
            gesture(mind, EtoGesture.NONE, now);
        }
        mind.with = 0;
        mind.dirty = true;
    }

    /** Hearts between her face and theirs. */
    static void hearts(ServerLevel level, EtoNpc npc, LivingEntity other, int count) {
        Vec3 a = npc.m_20182_();
        Vec3 b = other.m_20182_();
        level.m_8767_(ParticleTypes.f_123750_, (a.f_82479_ + b.f_82479_) * 0.5, a.f_82480_ + 1.6 * npc.persona().size(), (a.f_82481_ + b.f_82481_) * 0.5, count, 0.3,
            0.25, 0.3, 0.02);
    }

    /** {@code player} talks to her: she stands and faces them a while. */
    static void listen(EtoNpc npc, ServerPlayer player, long now) {
        Mind mind = npc.mind;
        if (mind == null) {
            return;
        }
        mind.talking = player == null ? null : player.m_20148_();
        mind.talkingUntil = now + 600L;
    }

    /** Out of a fight a while: she knits back up. */
    private static void heal(EtoNpc npc, Mind mind, long now) {
        long calmSince = Math.max(mind.foughtAt, mind.hurtAt);
        if (now % 20L == 0L && npc.m_21223_() < npc.m_21233_() && (calmSince == Long.MIN_VALUE || now - calmSince >= EtoRules.CALM_TICKS)) {
            npc.m_5634_(EtoRules.HEAL_PER_SECOND * 2.0F);
        }
    }

    // ---------------------------------------------------------------- what's in her hands

    /** Her hands dressed for her gesture: the book, the quill and pocket book, the brush; emptied after. */
    private static void props(EtoNpc npc, Mind mind) {
        EtoGesture now = mind.target != 0 ? EtoGesture.NONE : mind.gesture;
        // 2.4.9: the food she carries, in her hand as she walks with it and as she holds it out.
        String carry = mind.target == 0 && (now == EtoGesture.NONE || now == EtoGesture.SERVE) ? mind.carry : null;
        if (now == mind.props && java.util.Objects.equals(carry, mind.propsCarry)) {
            return;
        }
        mind.props = now;
        mind.propsCarry = carry;
        ItemStack food = carry == null ? null : HomeKitchen.stack(carry, 1);
        npc.m_21008_(InteractionHand.MAIN_HAND, food != null ? food : stack(now.mainHand));
        npc.m_21008_(InteractionHand.OFF_HAND, stack(now.offHand));
    }

    static ItemStack stack(String id) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.m_214293_("minecraft", id == null ? "air" : id.substring(id.indexOf(':') + 1)));
        if (item == null) {
            item = ForgeRegistries.ITEMS.getValue(ResourceLocation.m_214293_("minecraft", "air"));
        }
        return new ItemStack(item, id == null ? 0 : 1);
    }

    /** She died: nothing of hers is dropped from her hands, the world remembers it, and her shards go out. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void died(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof EtoNpc npc) || !(npc.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        npc.m_21008_(InteractionHand.MAIN_HAND, stack(null));
        npc.m_21008_(InteractionHand.OFF_HAND, stack(null));
        UniqueNpcs.died(level, npc.persona().key(), npc.m_20148_());
        // 1.3.6: her tentacles go with her, and whatever her flesh held is let go.
        EtoWatch.forget(npc);
        EtoArm.forget(npc);
        EtoOwlForm.forget(npc);
        KenPeace.forget(npc);
        NpcWings.forget(npc, npc.mind);
        if (npc.mind != null) {
            EtoFlesh.release(level, npc.mind.owl);
            HomeChores.stop(npc, npc.mind, level); // 1.3.29: a chest's lid she had up, shut
        }
        LIVE.remove(npc);
    }

    // ---------------------------------------------------------------- helpers

    /** The player {@code id} in {@code level}, or null. */
    static ServerPlayer player(ServerLevel level, UUID id) {
        if (id == null) {
            return null;
        }
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_20148_().equals(id)) {
                return player;
            }
        }
        return null;
    }

    /** What the talk would say to {@code player} now (for the lines she says unasked). */
    static EtoTalk.Context context(EtoNpc npc, ServerPlayer player) {
        return EtoServer.context(player, npc);
    }

    /**
     * Her words to {@code player} alone: 1.3.30, in a bubble over her head that only they see (the chat was spammed with them); 1.3.31,
     * only when she can see them, near enough to be heard (never through a wall, never from across the world).
     */
    static void say(EtoNpc npc, ServerPlayer player, String line) {
        if (line != null && !line.isEmpty() && SceneServer.sees(npc, player, 16.0)) {
            ControlNetwork.sendTo(player, new com.hex.kagune.control.network.S2CNpcSay(npc.m_19879_(), line));
        }
    }

    /** Her words to every player near her. */
    static void bark(EtoNpc npc, ServerLevel level, String line) {
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_20280_(npc) <= 24.0 * 24.0) {
                say(npc, player, line);
            }
        }
    }

    /** A private word to her partner: a banner and a chime only they get. */
    static void alert(EtoNpc npc, ServerPlayer player, Romance.Want want, String text) {
        ControlNetwork.sendTo(player, new S2CNpcAlert(npc.displayName(), want.ordinal(), text));
    }

    /** 1.3.11: who last showed her what (by player), and when: she reacts once in a while, not at every flicker. */
    private static final Map<UUID, Long> SEEN = WorldTimes.map();

    /**
     * 1.3.11: {@code player} shows her what they are ({@link com.hex.kagune.control.npc.Display}): a word by who they are to her and where
     * she is (her Library), and what she does: amused, annoyed among her books, impressed, worried for her partner as it gets worse.
     */
    static void saw(EtoNpc npc, ServerPlayer player, com.hex.kagune.control.npc.Display shown, com.hex.kagune.control.npc.FinalSight sight,
        ServerLevel level, long now) {
        Mind mind = npc.mind;
        if (mind == null || mind.target == player.m_19879_() || shown == com.hex.kagune.control.npc.Display.NONE) {
            return;
        }
        Long last = SEEN.get(player.m_20148_());
        if (last != null && now - last < 600L && shown != com.hex.kagune.control.npc.Display.DREAD) {
            return;
        }
        SEEN.put(player.m_20148_(), now);
        EtoTalk.Context c = EtoServer.context(player, npc);
        com.hex.kagune.control.npc.EtoVoice voice = com.hex.kagune.control.npc.EtoVoice.of(c);
        BlockPos origin = npc.origin();
        boolean library = origin != null && npc.m_20238_(new Vec3(origin.m_123341_() + 0.5, origin.m_123342_(), origin.m_123343_() + 0.5)) < 28.0 * 28.0;
        // 1.3.65: the worst sights told apart, each companion its own way (Eto fascinated by War, Touka sick with worry, Ken who has been
        // there; Peace in awe).
        boolean worst = shown == com.hex.kagune.control.npc.Display.DREAD && sight != null && sight.any();
        com.hex.kagune.control.npc.Script script = npc.persona().script();
        String line = worst ? script.dread(sight, voice, library, npc.m_217043_().m_188503_(1000)) : script.saw(shown, voice, library, npc.m_217043_().m_188503_(1000));
        if (!line.isEmpty()) {
            say(npc, player, line);
        }
        boolean ours = voice.together();
        EtoGesture gesture;
        switch (shown) {
            case EYES -> gesture = ours ? EtoGesture.BLUSH : EtoGesture.LOOK;
            case KAGUNE -> gesture = ours ? EtoGesture.GIGGLE : library ? EtoGesture.ANGRY : EtoGesture.LAUGH;
            case KAKUJA -> gesture = ours ? EtoGesture.CLING : EtoGesture.LOOK;
            default -> gesture = worst ? script.dreadGesture(sight, ours) : ours ? EtoGesture.HUG : EtoGesture.LOOK;
        }
        if (mind.target == 0 && mind.kiss == null && mind.holding == null) {
            face(npc, player);
            gesture(mind, gesture, now);
        }
        // 2.5.2 (the owner: no hearts as the kagune or a kakuja comes out): her partner's kagune makes her giggle; no hearts puffed
        // between them (with her behind them, they rose behind their back as it tore out).
    }

    static void gesture(Mind mind, EtoGesture gesture, long now) {
        mind.gesture = gesture;
        mind.gestureAt = now;
        mind.dirty = true;
    }

    static void face(EtoNpc npc, Entity other) {
        Vec3 to = other.m_20182_().m_82546_(npc.m_20182_());
        if (to.f_82479_ * to.f_82479_ + to.f_82481_ * to.f_82481_ < 1.0E-4) {
            return;
        }
        float yaw = (float)Math.toDegrees(Math.atan2(-to.f_82479_, to.f_82481_));
        npc.m_146922_(yaw);
        npc.f_20883_ = yaw;
        npc.f_20885_ = yaw;
    }

    private static void sync(EtoNpc npc, Mind mind) {
        if (mind.owl.dirty) {
            mind.owl.dirty = false;
            mind.dirty = true;
        }
        if (!mind.dirty) {
            return;
        }
        mind.dirty = false;
        ControlNetwork.broadcastAround(npc, message(npc, mind));
    }

    static S2CEto message(EtoNpc npc, Mind mind) {
        if (mind == null) {
            return new S2CEto(npc.m_19879_(), 0, Long.MIN_VALUE, 0, 0, Long.MIN_VALUE, 0, 0.0F, 0.0F, 0.0, 0.0, 0.0, false, 0, "");
        }
        EtoOwl.Fight f = mind.owl;
        return new S2CEto(npc.m_19879_(), mind.gesture.ordinal(), mind.gestureAt, f.move.ordinal(), f.detail, f.at, f.target, f.charge, f.yaw,
            f.point.f_82479_, f.point.f_82480_, f.point.f_82481_, mind.turned, mind.with, mind.tag,
            mind.kissStyle == null ? 0 : mind.kissStyle.ordinal()); // 2.2.4
    }

    /** A player starts seeing her: they are told what she is doing. */
    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof EtoNpc npc && event.getEntity() instanceof ServerPlayer player) {
            ControlNetwork.sendTo(player, message(npc, npc.mind));
            EtoArm.tracked(player, npc);
            EtoOwlForm.tracked(player, npc);
            NpcWings.tracked(player, npc);
        }
    }
}
