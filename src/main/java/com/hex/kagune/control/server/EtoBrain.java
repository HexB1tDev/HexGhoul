package com.hex.kagune.control.server;

import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CEto;
import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.npc.NpcBonds;
import com.hex.kagune.control.npc.NpcRules;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.quest.QuestLog;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.0: what Eto does, decided on the server every tick ({@link EtoNpc}'s own AI step). She keeps to her home (restricted to
 * {@link EtoRules#HOME_RADIUS}, put back if she ends up past {@link EtoRules#LOST}); hostiles that come within reach of her, a player who
 * hits her or is her enemy, she fights ({@link EtoRules#choose}: the Speed Blitz, the Big Punch, the Speed Kick, flips) as far as her
 * leash, then goes home and heals. Calm, she does her goofy gestures now and then, and with a player she is dating she sometimes asks
 * for a kiss herself ({@link EtoRules#kiss}): she walks up, leans in with her eyes shut, and waits for a tap of shift (a peck), a hold (she
 * blushes and stays), or nothing (she is cross, and ignores them a while). Every client tracking her is told what it needs to draw her.
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
        EtoRules.Ready ready = EtoRules.Ready.NOW;
        EtoRules.Move move = EtoRules.Move.NONE;
        long moveAt = Long.MIN_VALUE;
        boolean landed;
        int flip = -1;
        long flipAt = Long.MIN_VALUE;
        Vec3 flipPush;
        EtoGesture gesture = EtoGesture.NONE;
        long gestureAt = Long.MIN_VALUE;
        long nextGestureAt;
        /** Who she is walking up to (a kiss or a hug), and what for: null none. */
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
            mind = new Mind();
            npc.mind = mind;
            mind.nextGestureAt = now + 100L + npc.m_217043_().m_188503_(200);
            npc.m_21530_();
        }
        BlockPos home = npc.home();
        if (home == null) {
            home = npc.settle();
        }
        double[] feet = npc.homeFeet();
        Vec3 homeAt = new Vec3(feet[0], feet[1], feet[2]);
        if (npc.m_20238_(homeAt) > EtoRules.LOST * EtoRules.LOST) {
            npc.m_6021_(homeAt.f_82479_, homeAt.f_82480_, homeAt.f_82481_);
            npc.m_21573_().m_26573_();
            mind.target = 0;
        }
        LivingEntity target = target(npc, mind, level, homeAt, now);
        npc.m_21446_(home, target != null ? EtoRules.LEASH : EtoRules.HOME_RADIUS);
        boolean turned = target != null;
        if (turned != mind.turned) {
            mind.turned = turned;
            mind.dirty = true;
        }
        if (target != null) {
            endKiss(npc, mind, null, now);
            if (mind.gesture != EtoGesture.NONE) {
                gesture(mind, EtoGesture.NONE, now);
            }
            fight(npc, mind, target, level, now);
        } else {
            if (mind.move != EtoRules.Move.NONE && now - mind.moveAt > EtoRules.PUNCH_TICKS) {
                mind.move = EtoRules.Move.NONE;
                mind.dirty = true;
            }
            calm(npc, mind, level, now);
            heal(npc, mind, now);
        }
        if (mind.gesture.over(now - mind.gestureAt) && mind.gesture != EtoGesture.NONE && !mind.gesture.held()) {
            gesture(mind, EtoGesture.NONE, now);
        }
        sync(npc, mind);
    }

    // ---------------------------------------------------------------- who she fights

    private static LivingEntity target(EtoNpc npc, Mind mind, ServerLevel level, Vec3 home, long now) {
        LivingEntity current = mind.target == 0 ? null : level.m_6815_(mind.target) instanceof LivingEntity living ? living : null;
        if (current != null && (!current.m_6084_() || current.m_20238_(home) > (EtoRules.LEASH + 2.0) * (EtoRules.LEASH + 2.0)
            || current instanceof ServerPlayer player && (player.m_7500_() || player.m_5833_()))) {
            current = null;
        }
        if (current == null && now % 5L == 0L) {
            current = seek(npc, mind, level, home, now);
        }
        int id = current == null ? 0 : current.m_19879_();
        if (id != mind.target) {
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
                fights = !player.m_7500_() && !player.m_5833_() && (mind.grudges.containsKey(player.m_20148_())
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

    /** She was hurt: whoever did it is fought, and a player who did it loses her regard. */
    @SubscribeEvent
    public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
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

    // ---------------------------------------------------------------- her fight

    private static void fight(EtoNpc npc, Mind mind, LivingEntity target, ServerLevel level, long now) {
        mind.foughtAt = now;
        mind.talking = null;
        float health = npc.m_21223_() / Math.max(1.0F, npc.m_21233_());
        face(npc, target);
        npc.m_21563_().m_24960_(target, 30.0F, 30.0F);
        if (flipping(npc, mind, now)) {
            return;
        }
        double distance = Math.sqrt(target.m_20280_(npc));
        if (mind.move != EtoRules.Move.NONE && carry(npc, mind, target, level, now, distance, health)) {
            return;
        }
        boolean tough = target.m_21223_() >= 30.0F;
        EtoRules.Move move = EtoRules.choose(distance, now, mind.ready, npc.m_20096_(), npc.m_142582_(target), health, tough,
            npc.m_217043_().m_188500_());
        switch (move) {
            case NONE -> npc.m_21573_().m_5624_(target, health < EtoRules.TRANSFORM_AT ? 1.25 : 1.05);
            case FLIP_FORWARD, FLIP_BACK -> flip(npc, mind, move, target, level, now);
            default -> begin(npc, mind, move, level, now);
        }
    }

    private static void begin(EtoNpc npc, Mind mind, EtoRules.Move move, ServerLevel level, long now) {
        npc.m_21573_().m_26573_();
        mind.move = move;
        mind.moveAt = now;
        mind.landed = false;
        mind.ready = mind.ready.used(move, now);
        mind.dirty = true;
        if (move == EtoRules.Move.PUNCH) {
            UkakuFire.sound(level, npc.m_20182_(), "entity.player.attack.nodamage", 0.8F, 0.7F);
        }
    }

    /** The move she is making, one tick on: true while it holds her (nothing else this tick). */
    private static boolean carry(EtoNpc npc, Mind mind, LivingEntity target, ServerLevel level, long now, double distance, float health) {
        long age = now - mind.moveAt;
        switch (mind.move) {
            case STRIKE -> {
                if (!mind.landed) {
                    mind.landed = true;
                    if (distance <= EtoRules.STRIKE_REACH + 0.6) {
                        hit(npc, target, EtoRules.damage(EtoRules.STRIKE_DAMAGE, health), 0.35, 0.1, level, "entity.player.attack.sweep");
                    }
                }
                if (age >= 6L) {
                    mind.move = EtoRules.Move.NONE;
                }
                return false;
            }
            case BLITZ -> {
                if (age == 1L) {
                    // A blink in front of it, already striking.
                    Vec3 from = target.m_20182_().m_82546_(npc.m_20182_());
                    Vec3 way = new Vec3(from.f_82479_, 0.0, from.f_82481_);
                    if (way.m_82553_() > 1.0E-3) {
                        way = way.m_82541_();
                        Vec3 to = target.m_20182_().m_82546_(way.m_82490_(1.3));
                        Vec3 shift = to.m_82546_(npc.m_20182_());
                        if (level.m_45756_(npc, npc.m_20191_().m_82386_(shift.f_82479_, shift.f_82480_, shift.f_82481_))) {
                            npc.m_6021_(to.f_82479_, to.f_82480_, to.f_82481_);
                        } else {
                            npc.m_20256_(way.m_82490_(1.1).m_82520_(0.0, 0.1, 0.0));
                            npc.f_19864_ = true;
                        }
                    }
                    UkakuFire.sound(level, npc.m_20182_(), "entity.player.attack.sweep", 0.9F, 1.3F);
                }
                if (age >= 2L && !mind.landed && Math.sqrt(target.m_20280_(npc)) <= EtoRules.CLOSE_REACH + 0.5) {
                    mind.landed = true;
                    hit(npc, target, EtoRules.damage(EtoRules.BLITZ_DAMAGE, health), 0.5, 0.15, level, "entity.player.attack.strong");
                }
                if (age >= EtoRules.BLITZ_TICKS) {
                    mind.move = EtoRules.Move.NONE;
                    mind.dirty = true;
                }
                return true;
            }
            case PUNCH -> {
                npc.m_21573_().m_26573_();
                if (age >= EtoRules.PUNCH_WINDUP && !mind.landed) {
                    mind.landed = true;
                    if (distance <= EtoRules.CLOSE_REACH + 0.7) {
                        hit(npc, target, EtoRules.damage(EtoRules.PUNCH_DAMAGE, health), 1.6, 0.5, level, "entity.player.attack.knockback");
                        level.m_8767_(ParticleTypes.f_123797_, target.m_20185_(), target.m_20186_() + target.m_20206_() * 0.5, target.m_20189_(), 6, 0.2, 0.2, 0.2,
                            0.1);
                    }
                }
                if (age >= EtoRules.PUNCH_TICKS) {
                    mind.move = EtoRules.Move.NONE;
                    mind.dirty = true;
                }
                return true;
            }
            case KICK -> {
                npc.m_21573_().m_26573_();
                if (age >= 3L && !mind.landed) {
                    mind.landed = true;
                    if (distance <= EtoRules.CLOSE_REACH + 0.5) {
                        hit(npc, target, EtoRules.damage(EtoRules.KICK_DAMAGE, health), 0.4, 0.95, level, "entity.player.attack.strong");
                    }
                }
                if (age >= EtoRules.KICK_TICKS) {
                    mind.move = EtoRules.Move.NONE;
                    mind.dirty = true;
                }
                return true;
            }
            default -> {
                mind.move = EtoRules.Move.NONE;
                return false;
            }
        }
    }

    /** A blow of hers on {@code body}: {@code damage}, thrown {@code away} from her and {@code up}. */
    private static void hit(EtoNpc npc, LivingEntity body, float damage, double away, double up, ServerLevel level, String sound) {
        if (!body.m_6469_(npc.m_269291_().m_269333_(npc), damage)) {
            return;
        }
        Vec3 from = body.m_20182_().m_82546_(npc.m_20182_());
        Vec3 way = new Vec3(from.f_82479_, 0.0, from.f_82481_);
        way = way.m_82553_() < 1.0E-3 ? npc.m_20154_() : way.m_82541_();
        body.m_20256_(body.m_20184_().m_82490_(0.3).m_82549_(way.m_82490_(away)).m_82520_(0.0, up, 0.0));
        body.f_19864_ = true;
        UkakuFire.sound(level, body.m_20182_(), sound, 0.9F, 1.0F);
    }

    private static boolean flipping(EtoNpc npc, Mind mind, long now) {
        if (mind.flipAt == Long.MIN_VALUE) {
            return false;
        }
        long age = now - mind.flipAt;
        if (age == 1L && mind.flipPush != null) {
            npc.m_20256_(mind.flipPush);
            npc.f_19864_ = true;
        }
        boolean busy = age < NpcRules.FLIP_BUSY || age < NpcRules.FLIP_POSED && !npc.m_20096_();
        if (busy) {
            npc.m_21573_().m_26573_();
        }
        return busy;
    }

    /** A flip, as a ghoul NPC's ({@code GhoulNpcCombat.flip}): in over the gap, or back clear of it. */
    private static void flip(EtoNpc npc, Mind mind, EtoRules.Move move, LivingEntity target, ServerLevel level, long now) {
        boolean forward = move == EtoRules.Move.FLIP_FORWARD;
        Vec3 to = target.m_20182_().m_82546_(npc.m_20182_());
        Vec3 way = new Vec3(to.f_82479_, 0.0, to.f_82481_);
        way = way.m_82553_() < 1.0E-3 ? npc.m_20154_() : way.m_82541_();
        if (!forward) {
            way = way.m_82490_(-1.0);
        }
        double speed = forward ? NpcRules.flipSpeed(Math.sqrt(target.m_20280_(npc)) - EtoRules.CLOSE_REACH) : NpcRules.FLIP_BACK_SPEED;
        Vec3 push = new Vec3(way.f_82479_ * speed, forward ? NpcRules.FLIP_FORWARD_UP : NpcRules.FLIP_BACK_UP, way.f_82481_ * speed);
        npc.m_21573_().m_26573_();
        npc.m_20256_(push);
        npc.f_19864_ = true;
        npc.f_19789_ = 0.0F;
        mind.flipPush = push;
        mind.flip = (forward ? PaleRules.Flip.FORWARD : PaleRules.Flip.BACK).ordinal();
        mind.flipAt = now;
        mind.ready = mind.ready.used(move, now);
        mind.dirty = true;
        UkakuFire.sound(level, npc.m_20182_(), "entity.player.attack.sweep", 0.35F, 0.7F);
    }

    // ---------------------------------------------------------------- calm

    private static void calm(EtoNpc npc, Mind mind, ServerLevel level, long now) {
        if (mind.talking != null && now > mind.talkingUntil) {
            mind.talking = null;
        }
        if (mind.toward != null || mind.kiss != null) {
            closeIn(npc, mind, level, now);
            return;
        }
        ServerPlayer talker = player(level, mind.talking);
        if (talker != null) {
            npc.m_21573_().m_26573_();
            face(npc, talker);
            npc.m_21563_().m_24960_(talker, 30.0F, 30.0F);
            return;
        }
        if (now % 100L == 37L) {
            askForKiss(npc, mind, level, now);
            if (mind.toward != null) {
                return;
            }
        }
        if (now >= mind.nextGestureAt && mind.gesture == EtoGesture.NONE) {
            EtoGesture[] idle = EtoGesture.IDLE;
            EtoGesture pick = idle[npc.m_217043_().m_188503_(idle.length)];
            gesture(mind, pick, now);
            if (pick == EtoGesture.LEAN || pick == EtoGesture.STRETCH) {
                npc.m_21573_().m_26573_();
            }
            mind.nextGestureAt = now + 160L + npc.m_217043_().m_188503_(260);
        }
    }

    /** Now and then, a player she is dating near her, and her twenty minutes since the last up: she wants a kiss. */
    private static void askForKiss(EtoNpc npc, Mind mind, ServerLevel level, long now) {
        mind.ignoring.values().removeIf(until -> until < now);
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_20280_(npc) > EtoRules.KISS_ASK_NEAR * EtoRules.KISS_ASK_NEAR || !player.m_6084_() || player.m_5833_()
                || mind.ignoring.containsKey(player.m_20148_()) || !QuestServer.bond(player, EtoRules.KEY).has(EtoRules.DATING)
                || now < mind.nextAsk.getOrDefault(player.m_20148_(), Long.MIN_VALUE)) {
                continue;
            }
            if (npc.m_217043_().m_188500_() < EtoRules.KISS_ASK_CHANCE) {
                mind.nextAsk.put(player.m_20148_(), now + EtoRules.KISS_ASK_EVERY);
                walkUp(npc, mind, player, EtoGesture.KISS, now);
                return;
            }
        }
    }

    /** True if she is ignoring {@code player} (they left her hanging). */
    public static boolean ignoring(EtoNpc npc, ServerPlayer player, long now) {
        Mind mind = npc.mind;
        Long until = mind == null ? null : mind.ignoring.get(player.m_20148_());
        return until != null && until > now;
    }

    /** She walks up to {@code player} for {@code what} (a kiss or a hug). */
    public static void walkUp(EtoNpc npc, Mind mind, ServerPlayer player, EtoGesture what, long now) {
        endKiss(npc, mind, null, now);
        mind.toward = player.m_20148_();
        mind.towardFor = what;
        mind.towardSince = now;
        mind.talking = null;
        gesture(mind, EtoGesture.NONE, now);
    }

    /** Walking up to them, then the kiss or the hug itself. */
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
            if (distance > EtoRules.KISS_NEAR) {
                if (now - mind.towardSince > 200L || distance > EtoRules.LEASH + 4.0) {
                    endKiss(npc, mind, null, now);
                    return;
                }
                npc.m_21573_().m_5624_(player, 0.9);
                return;
            }
            npc.m_21573_().m_26573_();
            if (mind.towardFor == EtoGesture.HUG) {
                gesture(mind, EtoGesture.HUG, now);
                hearts(level, npc, player, 6);
                mind.toward = null;
                return;
            }
            mind.kiss = EtoRules.Kiss.WAIT;
            mind.kissAt = now;
            mind.held = 0;
            gesture(mind, EtoGesture.KISS, now);
            return;
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
            return;
        }
        switch (after) {
            case BLUSH -> {
                mind.kiss = EtoRules.Kiss.BLUSH;
                mind.kissAt = now;
                gesture(mind, EtoGesture.BLUSH, now);
                hearts(level, npc, player, 3);
            }
            case KISSED -> endKiss(npc, mind, player, now);
            default -> {
                // Left hanging.
                mind.kiss = null;
                mind.toward = null;
                mind.ignoring.put(player.m_20148_(), now + EtoRules.IGNORE_TICKS);
                gesture(mind, EtoGesture.ANGRY, now);
                player.m_5661_(Component.m_237113_(EtoRules.NAME + " is cross with you. She'll ignore you for a while."), true);
            }
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
        } else if (mind.gesture == EtoGesture.KISS || mind.gesture == EtoGesture.BLUSH) {
            gesture(mind, EtoGesture.NONE, now);
        }
    }

    /** Hearts between her face and theirs. */
    static void hearts(ServerLevel level, EtoNpc npc, LivingEntity other, int count) {
        Vec3 a = npc.m_20182_();
        Vec3 b = other.m_20182_();
        level.m_8767_(ParticleTypes.f_123750_, (a.f_82479_ + b.f_82479_) * 0.5, a.f_82480_ + 1.75, (a.f_82481_ + b.f_82481_) * 0.5, count, 0.3, 0.25, 0.3,
            0.02);
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
            npc.m_5634_(EtoRules.HEAL_PER_SECOND);
        }
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
        if (!mind.dirty) {
            return;
        }
        mind.dirty = false;
        ControlNetwork.broadcastAround(npc, message(npc, mind));
    }

    static S2CEto message(EtoNpc npc, Mind mind) {
        if (mind == null) {
            return new S2CEto(npc.m_19879_(), 0, Long.MIN_VALUE, 0, Long.MIN_VALUE, -1, Long.MIN_VALUE, false);
        }
        return new S2CEto(npc.m_19879_(), mind.gesture.ordinal(), mind.gestureAt, mind.move.ordinal(), mind.moveAt, mind.flip, mind.flipAt, mind.turned);
    }

    /** A player starts seeing her: they are told what she is doing. */
    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof EtoNpc npc && event.getEntity() instanceof ServerPlayer player) {
            ControlNetwork.sendTo(player, message(npc, npc.mind));
        }
    }
}
