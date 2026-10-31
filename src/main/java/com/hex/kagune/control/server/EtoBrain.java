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
        /** Players who hurt her, until when she holds it against them. */
        final Map<UUID, Long> grudges = new HashMap<>();
        /** 1.3.4: her Owl DNA kit's moves ({@link EtoOwl}). */
        final EtoOwl.Fight owl = new EtoOwl.Fight();
        EtoGesture gesture = EtoGesture.NONE;
        long gestureAt = Long.MIN_VALUE;
        long nextGestureAt;
        /** Who she is walking up to (a kiss, a hug, to cling), and what for: null none. */
        UUID toward;
        EtoGesture towardFor = EtoGesture.NONE;
        long towardSince;
        /** The kiss she is waiting on: its stage, since when, the shift key's hold. */
        EtoRules.Kiss kiss;
        long kissAt;
        int held;
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
        final Map<UUID, Long> nearSince = new HashMap<>();
        long nextSeek;
        long nextNightAsk;
        /** Her partner last near her (game time), and whether they are near now: back after a while, she greets them. */
        long partnerNearAt = Long.MIN_VALUE;
        boolean partnerNear;
        /** The gesture her hands were last dressed for. */
        EtoGesture props = EtoGesture.NONE;
        long protectedAt = Long.MIN_VALUE;
        /** 1.3.3: when a player was last about her (away from home a day without one, she goes back). */
        long company = Long.MIN_VALUE;
        /** 1.3.3: whose hand she holds (she walks at their side), and whether their shift was down last tick (a tap lets go). */
        UUID holding;
        boolean holdShift;
    }

    /** Every Eto ticking on this server (one, unless another has yet to be sent away). */
    private static final Set<EtoNpc> LIVE = Collections.newSetFromMap(new WeakHashMap<>());

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
            if (!UniqueNpcs.claim(level, EtoRules.KEY, npc.m_20148_(), first)) {
                npc.m_146870_();
                return;
            }
            mind = new Mind();
            npc.mind = mind;
            mind.nextGestureAt = now + 100L + npc.m_217043_().m_188503_(200);
            mind.nextSeek = now + 1200L;
            UniqueNpc known = UniqueNpcs.get(level, EtoRules.KEY);
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
        if (now % 200L == 7L && !UniqueNpcs.claim(level, EtoRules.KEY, npc.m_20148_(), home)) {
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
            UniqueNpcs.report(level, EtoRules.KEY, origin, away, mind.company);
            if (away && mind.target == 0 && now - mind.company >= UniqueNpc.GOES_HOME) {
                letGo(npc, mind, level, now, false);
                npc.goHome();
                home = npc.home();
                mind.company = now;
                UniqueNpcs.report(level, EtoRules.KEY, origin, false, now);
                ServerPlayer partner = player(level, mind.partner);
                if (partner != null) {
                    partner.m_5661_(Component.m_237113_("\u00a7d" + EtoRules.NAME + " got tired of waiting and went home to the Library."), false);
                }
            }
        }
        if (now % 20L == 3L) {
            partner(npc, mind, level);
        }
        // 1.3.1: following a player, her home goes with her (never pulled back to the Library); she keeps up, or is put by them if far behind.
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
                if (far > 24.0 * 24.0) {
                    npc.m_6021_(at.f_82479_, at.f_82480_, at.f_82481_);
                    npc.m_21573_().m_26573_();
                    home = npc.settle();
                } else if (far > 9.0 && now % 10L == 0L && mind.toward == null && mind.kiss == null && mind.holding == null) {
                    npc.m_21573_().m_5624_(leader, 1.1);
                }
            }
        }
        double[] feet = npc.homeFeet();
        Vec3 homeAt = new Vec3(feet[0], feet[1], feet[2]);
        if (npc.m_20238_(homeAt) > EtoRules.LOST * EtoRules.LOST && !EtoOwl.carried(mind.owl)) {
            npc.m_6021_(homeAt.f_82479_, homeAt.f_82480_, homeAt.f_82481_);
            npc.m_21573_().m_26573_();
            mind.target = 0;
        }
        LivingEntity target = target(npc, mind, level, homeAt, now);
        // Her partner about, she may wander as far as her leash to be near them.
        npc.m_21446_(home, target != null || mind.partnerNear ? EtoRules.LEASH : EtoRules.HOME_RADIUS);
        boolean turned = target != null;
        if (turned != mind.turned) {
            mind.turned = turned;
            mind.dirty = true;
        }
        if (target != null) {
            endKiss(npc, mind, null, now);
            letGo(npc, mind, level, now, false);
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
            calm(npc, mind, level, now);
            heal(npc, mind, now);
        }
        // 1.3.6: her Watch This tentacles hunt on their own, whatever she is doing, until their ten seconds are up.
        EtoWatch.tick(npc, mind, level, now);
        // 1.3.7: and her Mutated Arm, out until it is back in her.
        EtoArm.tick(npc, mind, level, now);
        // 1.3.8: the Owl's burst, and shedding it once the fight is long over.
        EtoOwlForm.tick(npc, mind, level, now);
        if (mind.gesture.over(now - mind.gestureAt) && mind.gesture != EtoGesture.NONE && !mind.gesture.held()) {
            gesture(mind, EtoGesture.NONE, now);
        }
        if (mind.with != 0 && mind.gesture != EtoGesture.PULL && mind.gesture != EtoGesture.CLING && mind.gesture != EtoGesture.HUG
            && mind.gesture != EtoGesture.HAND && mind.kiss == null) {
            mind.with = 0;
            mind.dirty = true;
        }
        props(npc, mind);
        sync(npc, mind);
    }

    /** Her partner, as the world remembers it: the line over her head follows. */
    private static void partner(EtoNpc npc, Mind mind, ServerLevel level) {
        UniqueNpc unique = UniqueNpcs.get(level, EtoRules.KEY);
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
        UniqueNpc unique = UniqueNpcs.get(level, EtoRules.KEY);
        UUID id = player.m_20148_();
        NpcMemory memory = NpcMemoryServer.of(player);
        long seen = memory.get(EtoRules.KEY, NpcMemory.SEEN, Long.MIN_VALUE);
        long daysAway = seen == Long.MIN_VALUE ? 0L : Math.max(0L, (now - seen) / 24000L);
        boolean sulking = mind != null && (until(mind.ignoring, id) > now || until(mind.sulk, id) > now);
        boolean jealous = memory.get(EtoRules.KEY, NpcMemory.JEALOUS, Long.MIN_VALUE) > now;
        boolean giddy = mind != null && until(mind.giddy, id) > now;
        Long since = mind == null ? null : mind.nearSince.get(id);
        boolean near = since != null && now - since > 600L;
        double roll = Math.floorMod((id.hashCode() * 31L + now / 3000L) * 0x9E3779B1L, 1000L) / 1000.0;
        return EtoMood.of(unique.stageOf(id), unique.claimedFrom(id), QuestServer.bond(player, EtoRules.KEY).regard(), level.m_46468_(), daysAway, sulking,
            jealous, giddy, near, roll);
    }

    private static long until(Map<UUID, Long> map, UUID id) {
        Long value = map.get(id);
        return value == null ? Long.MIN_VALUE : value;
    }

    // ---------------------------------------------------------------- who she fights

    private static LivingEntity target(EtoNpc npc, Mind mind, ServerLevel level, Vec3 home, long now) {
        LivingEntity current = mind.target == 0 ? null : level.m_6815_(mind.target) instanceof LivingEntity living ? living : null;
        if (current != null && (!current.m_6084_() || current.m_20238_(home) > (EtoRules.LEASH + 2.0) * (EtoRules.LEASH + 2.0)
            || current instanceof ServerPlayer player && (player.m_7500_() || player.m_5833_() || partnerOf(mind, player)))) {
            current = null;
        }
        if (current == null && now % 5L == 0L) {
            current = seek(npc, mind, level, home, now);
        }
        int id = current == null ? 0 : current.m_19879_();
        if (id != mind.target) {
            if (id == 0 && mind.target != 0) {
                if (mind.phase == EtoRules.Phase.MANIAC) {
                    bark(npc, level, EtoLines.won(npc.m_217043_().m_188503_(100)));
                }
            }
            mind.target = id;
            mind.dirty = true;
        }
        return current;
    }

    /** Something to fight: a hostile within reach of her, a player she holds a grudge against or who is her enemy, near her home. */
    private static LivingEntity seek(EtoNpc npc, Mind mind, ServerLevel level, Vec3 home, long now) {
        mind.grudges.values().removeIf(until -> until < now);
        AABB around = new AABB(home.f_82479_ - EtoRules.LEASH, home.f_82480_ - 3.0, home.f_82481_ - EtoRules.LEASH, home.f_82479_ + EtoRules.LEASH,
            home.f_82480_ + 4.0, home.f_82481_ + EtoRules.LEASH);
        LivingEntity best = null;
        double nearest = Double.MAX_VALUE;
        for (LivingEntity living : level.m_6443_(LivingEntity.class, around, living -> living != npc && living.m_6084_())) {
            double distance = living.m_20280_(npc);
            boolean fights;
            if (living instanceof ServerPlayer player) {
                // 1.3.2: her partner, never; whatever they did.
                fights = !player.m_7500_() && !player.m_5833_() && !partnerOf(mind, player) && (mind.grudges.containsKey(player.m_20148_())
                    || QuestServer.bond(player, EtoRules.KEY).regard() <= NpcBonds.ENEMY);
            } else {
                fights = living instanceof Enemy && distance <= (EtoRules.STRIKE_REACH + 0.5) * (EtoRules.STRIKE_REACH + 0.5);
            }
            if (fights && distance < nearest) {
                nearest = distance;
                best = living;
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
        if (event.getEntity() instanceof ServerPlayer victim) {
            protect(victim, event.getSource());
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
        Entity cause = source.m_7639_();
        if (!(cause instanceof LivingEntity attacker) || attacker == npc) {
            return;
        }
        if (attacker instanceof ServerPlayer player) {
            QuestLog.Bond bond = QuestServer.bond(player, EtoRules.KEY);
            if (partnerOf(mind, player)) {
                QuestServer.bond(player, EtoRules.KEY, bond.regard(bond.regard() + Romance.PARTNER_BLOW));
                if (until(mind.sulk, player.m_20148_()) < now) {
                    say(player, EtoLines.partnerHit(npc.m_217043_().m_188503_(100), mind.stage == Romance.Stage.MARRIED));
                    gesture(mind, EtoGesture.POUT, now);
                }
                mind.sulk.put(player.m_20148_(), now + Romance.PARTNER_SULK);
                endKiss(npc, mind, null, now);
                return;
            }
            QuestServer.bond(player, EtoRules.KEY, bond.regard(bond.regard() + EtoRules.BLOW));
            mind.grudges.put(player.m_20148_(), now + 400L);
            if (player.m_20148_().equals(mind.talking)) {
                mind.talking = null;
            }
        }
        if (mind.target == 0) {
            mind.target = attacker.m_19879_();
            mind.dirty = true;
        }
    }

    /** 1.3.4: nothing hurts her through the Speed Kick's lunge and dash (as a Pale player's). */
    @SubscribeEvent
    public static void attacked(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (event.getEntity() instanceof EtoNpc npc && npc.mind != null && EtoOwl.untouchable(npc.mind.owl, npc.m_9236_().m_46467_())) {
            event.setCanceled(true);
        }
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
            mind.dirty = true;
            endKiss(npc, mind, null, now);
            if (now - mind.protectedAt > 200L) {
                mind.protectedAt = now;
                bark(npc, (ServerLevel)npc.m_9236_(), EtoLines.protect(context(npc, victim)));
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
            bark(npc, level, phase == EtoRules.Phase.MANIAC ? EtoLines.maniac(seed) : EtoLines.serious(seed));
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
        EtoOwl.tick(npc, mind, target, level, now, phase);
    }

    // ---------------------------------------------------------------- calm

    private static void calm(EtoNpc npc, Mind mind, ServerLevel level, long now) {
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
        if (partner != null && together(npc, mind, level, partner, now)) {
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
            EtoGesture[] idle = mood.idles();
            // Her mood's first, mostly; now and then anything at all.
            EtoGesture pick = npc.m_217043_().m_188500_() < 0.8 ? idle[npc.m_217043_().m_188503_(idle.length)]
                : EtoGesture.IDLE[npc.m_217043_().m_188503_(EtoGesture.IDLE.length)];
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
                    NpcMemoryServer.set(player, EtoRules.KEY, NpcMemory.SEEN, now);
                }
            } else {
                mind.nearSince.remove(id);
            }
        }
        boolean near = partner != null && partner.m_20280_(npc) <= 16.0 * 16.0;
        if (near && !mind.partnerNear) {
            if (mind.partnerNearAt != Long.MIN_VALUE && now - mind.partnerNearAt >= 6000L && mind.target == 0 && mind.kiss == null) {
                EtoTalk.Context c = context(npc, partner);
                say(partner, EtoLines.welcome(c));
                alert(partner, Romance.Want.MISSED, "");
                if (reachable(npc, partner)) {
                    walkUp(npc, mind, partner, EtoGesture.HUG, now);
                }
            }
            NpcMemoryServer.set(partner, EtoRules.KEY, NpcMemory.SEEN, now);
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
        if (clingy && reach && distance > 3.5 && now % 15L == 0L && !mind.gesture.still()) {
            npc.m_21573_().m_5624_(partner, 1.0);
        }
        if (!reach) {
            return false;
        }
        if (mood == EtoMood.AFFECTIONATE && now >= mind.nextNightAsk && distance < 12.0) {
            mind.nextNightAsk = now + 24000L;
            EtoTalk.Context c = context(npc, partner);
            if (!c.nightUsed()) {
                say(partner, EtoLines.approach(c));
                alert(partner, Romance.Want.NIGHT, "");
                walkUp(npc, mind, partner, EtoGesture.CLING, now);
                return true;
            }
        }
        long every = mood.seeks();
        if (every > 0L && now >= mind.nextSeek && distance < 16.0) {
            mind.nextSeek = now + every + npc.m_217043_().m_188503_((int)Math.max(1L, every / 2L));
            EtoTalk.Context c = context(npc, partner);
            say(partner, EtoLines.approach(c));
            alert(partner, Romance.Want.TALK, "");
            walkUp(npc, mind, partner, clingy ? EtoGesture.CLING : EtoGesture.WAVE, now);
            return true;
        }
        Long since = mind.nearSince.get(partner.m_20148_());
        if (clingy && distance < 4.0 && since != null && now - since > 200L && mind.gesture == EtoGesture.NONE && npc.m_217043_().m_188500_() < 0.01) {
            walkUp(npc, mind, partner, EtoGesture.CLING, now);
            return true;
        }
        return false;
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

    /** Now and then, a player she is dating near her, and her twenty minutes since the last up: she wants a kiss. */
    private static void askForKiss(EtoNpc npc, Mind mind, ServerLevel level, long now) {
        mind.ignoring.values().removeIf(until -> until < now);
        ServerPlayer player = player(level, mind.partner);
        if (player == null || player.m_20280_(npc) > EtoRules.KISS_ASK_NEAR * EtoRules.KISS_ASK_NEAR || !player.m_6084_() || player.m_5833_()
            || !reachable(npc, player)
            || mind.ignoring.containsKey(player.m_20148_()) || until(mind.sulk, player.m_20148_()) > now
            || now < mind.nextAsk.getOrDefault(player.m_20148_(), Long.MIN_VALUE)) {
            return;
        }
        double chance = mood(npc, player, now) == EtoMood.AFFECTIONATE ? EtoRules.KISS_ASK_CHANCE * 2.0 : EtoRules.KISS_ASK_CHANCE;
        if (npc.m_217043_().m_188500_() < chance) {
            mind.nextAsk.put(player.m_20148_(), now + EtoRules.KISS_ASK_EVERY);
            alert(player, Romance.Want.KISS, "");
            walkUp(npc, mind, player, EtoGesture.KISS, now);
        }
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

    /** 1.3.2: something sweet happened with {@code player}: she smiles about it a while. */
    static void giddy(EtoNpc npc, ServerPlayer player, long now) {
        if (npc.mind != null) {
            npc.mind.giddy.put(player.m_20148_(), now + 3600L);
        }
    }

    /** She walks up to {@code player} for {@code what} (a kiss, a hug, to cling, to wave). */
    public static void walkUp(EtoNpc npc, Mind mind, ServerPlayer player, EtoGesture what, long now) {
        endKiss(npc, mind, null, now);
        mind.toward = player.m_20148_();
        mind.towardFor = what;
        mind.towardSince = now;
        mind.talking = null;
        gesture(mind, EtoGesture.NONE, now);
    }

    /** Walking up to them, then the kiss, the hug, the clinging itself. */
    private static void closeIn(EtoNpc npc, Mind mind, ServerLevel level, long now) {
        ServerPlayer player = player(level, mind.toward);
        if (player == null || !player.m_6084_()) {
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
                    say(player, EtoLines.hugged(seed));
                    giddy(npc, player, now);
                    mind.toward = null;
                    return;
                }
                case CLING -> {
                    gesture(mind, EtoGesture.CLING, now);
                    mind.with = player.m_19879_();
                    hearts(level, npc, player, 3);
                    giddy(npc, player, now);
                    mind.toward = null;
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
                    say(player, EtoLines.handTaken(seed));
                    player.m_5661_(Component.m_237113_("\u00a7d\u2764 Holding hands with " + EtoRules.NAME + " \u00a77(tap Shift to let go)"), true);
                    return;
                }
                case KISS -> {
                    mind.kiss = EtoRules.Kiss.WAIT;
                    mind.kissAt = now;
                    mind.held = 0;
                    mind.with = player.m_19879_();
                    gesture(mind, EtoGesture.KISS, now);
                    player.m_5661_(Component.m_237113_("§d❤ " + EtoRules.NAME + " leans in, eyes closed... §7(tap or hold Shift)"), true);
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
        boolean shift = player.m_6047_();
        int released = !shift && mind.held > 0 ? mind.held : 0;
        mind.held = shift ? mind.held + 1 : 0;
        EtoRules.Kiss before = mind.kiss;
        EtoRules.Kiss after = EtoRules.kiss(before, now - mind.kissAt, shift, mind.held, released, distance);
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
                String line = before == EtoRules.Kiss.PULL ? EtoLines.pulled(seed) : before == EtoRules.Kiss.BLUSH ? EtoLines.held(seed)
                    : EtoLines.kissed(seed, mind.stage == Romance.Stage.MARRIED);
                say(player, line);
                NpcMemoryServer.add(player, EtoRules.KEY, NpcMemory.KISSES, 1L);
                giddy(npc, player, now);
                RomanceServer.romanced(player, EtoRules.KEY);
                if (before == EtoRules.Kiss.PULL) {
                    hearts(level, npc, player, 14);
                }
                endKiss(npc, mind, player, now);
            }
            default -> {
                // Left hanging.
                mind.kiss = null;
                mind.toward = null;
                mind.ignoring.put(player.m_20148_(), now + EtoRules.IGNORE_TICKS);
                gesture(mind, EtoGesture.ANGRY, now);
                say(player, EtoLines.hanging(seed));
                player.m_5661_(Component.m_237113_(EtoRules.NAME + " is cross with you. She'll ignore you for a while."), true);
            }
        }
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

    /** She keeps to the spot {@code out} to {@code player}'s right: walking there, or put there if left far behind. */
    private static void stayBeside(EtoNpc npc, ServerPlayer player, double out, long now) {
        Vec3 spot = beside(player, out);
        double dx = spot.f_82479_ - npc.m_20185_();
        double dz = spot.f_82481_ - npc.m_20189_();
        double off = Math.sqrt(dx * dx + dz * dz);
        if (off > 5.0 || Math.abs(spot.f_82480_ - npc.m_20186_()) > 3.0) {
            npc.m_21573_().m_26573_();
            npc.m_6021_(spot.f_82479_, spot.f_82480_, spot.f_82481_);
        } else if (off > 0.3) {
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
        Vec3 move = spot.m_82546_(npc.m_20182_());
        if (level.m_45756_(npc, npc.m_20191_().m_82386_(move.f_82479_, move.f_82480_, move.f_82481_))) {
            npc.m_21573_().m_26573_();
            npc.m_6021_(spot.f_82479_, spot.f_82480_, spot.f_82481_);
            npc.m_20256_(player.m_20184_());
        } else {
            stayBeside(npc, player, EtoHand.out(woman), now);
        }
        npc.m_146922_(yaw);
        npc.f_20883_ = yaw;
        npc.f_20885_ = yaw;
        if (now % 80L == 0L) {
            hearts(level, npc, player, 1);
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
        if (mind.gesture == EtoGesture.HAND) {
            gesture(mind, EtoGesture.NONE, now);
        }
        npc.m_21573_().m_26573_();
        if (npc.following() == null) {
            npc.settle();
        }
        if (said && player != null) {
            say(player, EtoLines.handLet(npc.m_217043_().m_188503_(100)));
        }
        mind.dirty = true;
    }

    /** True if a player is about her (company: no reason to go home). */
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
        level.m_8767_(ParticleTypes.f_123750_, (a.f_82479_ + b.f_82479_) * 0.5, a.f_82480_ + 1.6 * EtoRules.SIZE, (a.f_82481_ + b.f_82481_) * 0.5, count, 0.3,
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
        if (now == mind.props) {
            return;
        }
        mind.props = now;
        npc.m_21008_(InteractionHand.MAIN_HAND, stack(now.mainHand));
        npc.m_21008_(InteractionHand.OFF_HAND, stack(now.offHand));
    }

    private static ItemStack stack(String id) {
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
        UniqueNpcs.died(level, EtoRules.KEY, npc.m_20148_());
        // 1.3.6: her tentacles go with her, and whatever her flesh held is let go.
        EtoWatch.forget(npc);
        EtoArm.forget(npc);
        EtoOwlForm.forget(npc);
        if (npc.mind != null) {
            EtoFlesh.release(level, npc.mind.owl);
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

    /** Her words to {@code player} alone, in their chat. */
    static void say(ServerPlayer player, String line) {
        if (line != null && !line.isEmpty()) {
            player.m_5661_(Component.m_237113_("§d<" + EtoRules.NAME + ">§r " + line), false);
        }
    }

    /** Her words to every player near her. */
    static void bark(EtoNpc npc, ServerLevel level, String line) {
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_20280_(npc) <= 24.0 * 24.0) {
                say(player, line);
            }
        }
    }

    /** A private word to her partner: a banner and a chime only they get. */
    static void alert(ServerPlayer player, Romance.Want want, String text) {
        ControlNetwork.sendTo(player, new S2CNpcAlert(EtoRules.NAME, want.ordinal(), text));
    }

    /** 1.3.11: who last showed her what (by player), and when: she reacts once in a while, not at every flicker. */
    private static final Map<UUID, Long> SEEN = new HashMap<>();

    /**
     * 1.3.11: {@code player} shows her what they are ({@link com.hex.kagune.control.npc.Display}): a word by who they are to her and where
     * she is (her Library), and what she does: amused, annoyed among her books, impressed, worried for her partner as it gets worse.
     */
    static void saw(EtoNpc npc, ServerPlayer player, com.hex.kagune.control.npc.Display shown, ServerLevel level, long now) {
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
        String line = EtoLines.saw(shown, voice, library, npc.m_217043_().m_188503_(1000));
        if (!line.isEmpty()) {
            say(player, line);
        }
        boolean ours = voice.together();
        EtoGesture gesture;
        switch (shown) {
            case EYES -> gesture = ours ? EtoGesture.BLUSH : EtoGesture.LOOK;
            case KAGUNE -> gesture = ours ? EtoGesture.GIGGLE : library ? EtoGesture.ANGRY : EtoGesture.LAUGH;
            case KAKUJA -> gesture = ours ? EtoGesture.CLING : EtoGesture.LOOK;
            default -> gesture = ours ? EtoGesture.HUG : EtoGesture.LOOK;
        }
        if (mind.target == 0 && mind.kiss == null && mind.holding == null) {
            face(npc, player);
            gesture(mind, gesture, now);
        }
        if (ours && shown == com.hex.kagune.control.npc.Display.KAGUNE) {
            hearts(level, npc, player, 3);
        }
    }

    static void gesture(Mind mind, EtoGesture gesture, long now) {
        mind.gesture = gesture;
        mind.gestureAt = now;
        mind.dirty = true;
    }

    private static void face(EtoNpc npc, Entity other) {
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
            f.point.f_82479_, f.point.f_82480_, f.point.f_82481_, mind.turned, mind.with, mind.tag);
    }

    /** A player starts seeing her: they are told what she is doing. */
    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof EtoNpc npc && event.getEntity() instanceof ServerPlayer player) {
            ControlNetwork.sendTo(player, message(npc, npc.mind));
            EtoArm.tracked(player, npc);
            EtoOwlForm.tracked(player, npc);
        }
    }
}
