package com.hex.kagune.control.server;

import com.hex.kagune.control.ClashRules;
import com.hex.kagune.control.Safety;
import com.hex.kagune.control.king.KingMarkFx;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CKingFx;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.0.40: kagune meeting kagune: every path's M1 says here when it sets out ({@link #swing}) and when it has landed ({@link #landed});
 * the moment one lands on a body whose own M1 is still on its way back at the striker ({@link ClashRules}), the two meet
 * ({@link #meets}): both blows are stopped (their own {@code stop}), neither hurts, the two are thrown apart, and every client near
 * sees and hears the wind of it blow out between them ({@link KingMarkFx#CLASH}). 1.0.41: the wild ghouls' lashes too, and a ghoul
 * can see a blow coming at it ({@link #threat}). 1.0.42: a blow on its way meets Ukaku's shards flying at its owner ({@link #swat}),
 * and a blow swung since Rinkaku's grab set out breaks it ({@link #lastSwing}).
 */
public final class KaguneClash {
    /**
     * A blow on its way: whose, when it set out, the last tick it can land, how far it reaches, the way it is aimed (null: the way its
     * owner looks), what it goes for (0: whatever is in front), whether it is still going, how it is cut off.
     */
    private record Swing(LivingEntity who, long began, long until, double reach, Vec3 aim, int target, BooleanSupplier live, Runnable stop,
                         int[] shards) {
    }

    private static final Map<UUID, Swing> SWINGS = new HashMap<>();
    /** 1.0.42: when each body's last blow set out (kept after it lands: Rinkaku's grab asks whether one was swung since it set out). */
    private static final Map<UUID, Long> LAST = new HashMap<>();
    /** Blows long gone are let go of this many ticks after they could have landed. */
    private static final long FORGET = 200L;

    private KaguneClash() {
    }

    /**
     * {@code player}'s M1 set out {@code now}, landing within {@code lands} ticks, {@code reach} blocks long; {@code live} true while it
     * is still that blow (not cut off by anything since), {@code stop} cuts it off.
     */
    public static void swing(ServerPlayer player, long now, int lands, double reach, BooleanSupplier live, Runnable stop) {
        swing(player, now, lands, reach, null, 0, live, stop);
    }

    /** 1.0.41: as above for any body: aimed along {@code aim} (null: the way it looks) at {@code target} (an entity id, 0: none in particular). */
    public static void swing(LivingEntity who, long now, int lands, double reach, Vec3 aim, int target, BooleanSupplier live, Runnable stop) {
        if (SWINGS.size() > 64) {
            SWINGS.values().removeIf(old -> now > old.until() + FORGET || old.who().m_213877_());
        }
        SWINGS.put(who.m_20148_(), new Swing(who, now, now + Math.max(0, lands), reach, aim, target, live, stop, new int[1]));
        if (LAST.size() > 256) {
            LAST.values().removeIf(at -> now > at + FORGET);
        }
        LAST.put(who.m_20148_(), now);
    }

    /** 1.0.42: the tick {@code who}'s last blow set out, or {@link Long#MIN_VALUE} if none lately. */
    public static long lastSwing(LivingEntity who) {
        Long at = LAST.get(who.m_20148_());
        return at == null ? Long.MIN_VALUE : at;
    }

    /**
     * 1.0.42: a shard of {@code shooter}'s flying from {@code from} to {@code to} this tick, through {@code level}: where a blow on its
     * way meets it ({@link ClashRules#swats}: coming at the one swinging it, in front of him and within reach), or null. Met, it is
     * to burst there and hurt nothing; the wind of it blows out where they met. Each blow meets a few ({@link ClashRules#SHARDS_PER_BLOW}),
     * and 1.0.43: is spent on them, landing on nothing after ({@link #meets}).
     */
    public static Vec3 swat(LivingEntity shooter, ServerLevel level, Vec3 from, Vec3 to, long now) {
        if (SWINGS.isEmpty()) {
            return null;
        }
        Swing best = null;
        double first = 2.0;
        double[] a = {from.f_82479_, from.f_82480_, from.f_82481_};
        double[] b = {to.f_82479_, to.f_82480_, to.f_82481_};
        for (Swing swing : SWINGS.values()) {
            LivingEntity who = swing.who();
            if (who == shooter || who.m_9236_() != level || swing.shards()[0] >= ClashRules.SHARDS_PER_BLOW || !who.m_6084_()
                || !ClashRules.onItsWay(swing.began(), swing.until(), now) || !alive(swing)) {
                continue;
            }
            Vec3 look = swing.aim() != null ? swing.aim() : who.m_20154_();
            double[] chest = {who.m_20185_(), who.m_20186_() + who.m_20206_() * 0.6, who.m_20189_()};
            double t = ClashRules.swats(swing.began(), swing.until(), now, look.f_82479_, look.f_82481_, chest, a, b, swing.reach());
            if (t >= 0.0 && t < first) {
                first = t;
                best = swing;
            }
        }
        if (best == null) {
            return null;
        }
        best.shards()[0]++;
        Vec3 at = from.m_82549_(to.m_82546_(from).m_82490_(first));
        Vec3 way = to.m_82546_(from);
        burst(best.who(), at, way.m_82556_() < 1.0E-6 ? best.who().m_20154_() : way.m_82541_(), 0.6F, 0.8F);
        return at;
    }

    /** {@code who}'s M1 has landed (or missed, or ended): no longer on its way. */
    public static void landed(LivingEntity who) {
        SWINGS.remove(who.m_20148_());
    }

    /**
     * {@code striker}'s M1 lands on {@code body} {@code now}: true if it meets a blow of {@code body}'s coming back at it, both then
     * stopped and thrown apart. The striker's blow must then hurt nothing. 1.0.43: true too for a blow spent on shards on its way
     * ({@link #swat}): it lands on nothing.
     */
    public static boolean meets(LivingEntity striker, Entity body, long now) {
        Swing mine = SWINGS.get(striker.m_20148_());
        if (mine != null && mine.shards()[0] > 0 && ClashRules.spent(mine.began(), mine.until(), now)) {
            return true;
        }
        if (!(body instanceof LivingEntity other) || other == striker || !other.m_6084_() || !striker.m_6084_()) {
            return false;
        }
        Swing theirs = SWINGS.get(other.m_20148_());
        if (theirs == null) {
            return false;
        }
        if (now > theirs.until() || !alive(theirs)) {
            SWINGS.remove(other.m_20148_());
            return false;
        }
        if (!aimedAt(theirs, striker, now)) {
            return false;
        }
        // (Its own is the blow landing now: stopped whatever it says of itself, its landing already under way.)
        SWINGS.remove(striker.m_20148_());
        SWINGS.remove(other.m_20148_());
        stop(mine);
        stop(theirs);
        apart(striker, other);
        apart(other, striker);
        wind(striker, other);
        return true;
    }

    /**
     * 1.0.41: a blow on its way at {@code body} now (the one aimed at it that set out last), or null: what a ghoul watches for to
     * dodge it or meet it with its own.
     */
    public static Threat threat(LivingEntity body, long now) {
        Swing best = null;
        for (Iterator<Swing> it = SWINGS.values().iterator(); it.hasNext(); ) {
            Swing swing = it.next();
            if (now > swing.until() + FORGET) {
                it.remove();
                continue;
            }
            if (swing.who() == body || swing.who().m_9236_() != body.m_9236_() || now > swing.until() || !alive(swing) || !aimedAt(swing, body, now)) {
                continue;
            }
            if (best == null || swing.began() > best.began()) {
                best = swing;
            }
        }
        return best == null ? null : new Threat(best.who(), best.began(), best.until());
    }

    /** 1.0.41: a blow coming: whose, when it set out, the last tick it can land. */
    public record Threat(LivingEntity who, long began, long until) {
    }

    /** {@code swing} is aimed at {@code body}: at it by name, or (aimed at nothing in particular) facing it and reaching it. */
    private static boolean aimedAt(Swing swing, LivingEntity body, long now) {
        LivingEntity who = swing.who();
        if (swing.target() != 0 && swing.target() != body.m_19879_()) {
            return false;
        }
        Vec3 look = swing.aim() != null ? swing.aim() : who.m_20154_();
        double dx = body.m_20185_() - who.m_20185_();
        double dz = body.m_20189_() - who.m_20189_();
        double distance = body.m_20182_().m_82554_(who.m_20182_());
        return ClashRules.meets(swing.began(), swing.until(), now, look.f_82479_, look.f_82481_, dx, dz, distance, swing.reach());
    }

    /** The blow is still the one that set out (asked safely). */
    private static boolean alive(Swing swing) {
        try {
            return !swing.who().m_213877_() && swing.live().getAsBoolean();
        } catch (RuntimeException failure) {
            Safety.caught(failure, "a kagune clash asking after a blow");
            return false;
        }
    }

    /** A blow cut off (one that fails to stop never takes the other's with it). */
    private static void stop(Swing swing) {
        if (swing == null) {
            return;
        }
        try {
            swing.stop().run();
        } catch (RuntimeException failure) {
            Safety.caught(failure, "a kagune clash stopping a blow");
        }
    }

    /** {@code who} thrown back from {@code from}. */
    private static void apart(LivingEntity who, LivingEntity from) {
        double dx = who.m_20185_() - from.m_20185_();
        double dz = who.m_20189_() - from.m_20189_();
        double l = Math.sqrt(dx * dx + dz * dz);
        if (l < 1.0E-4) {
            Vec3 look = who.m_20154_();
            dx = -look.f_82479_;
            dz = -look.f_82481_;
            l = Math.max(1.0E-4, Math.sqrt(dx * dx + dz * dz));
        }
        who.m_20256_(new Vec3(dx / l * ClashRules.PUSH, ClashRules.LIFT, dz / l * ClashRules.PUSH));
        who.f_19864_ = true;
        who.f_19789_ = 0.0F;
    }

    /** The wind of it, between their chests, blown out square to the line they met on; the crack and the gust of it. */
    private static void wind(LivingEntity a, LivingEntity b) {
        Vec3 chestA = a.m_20182_().m_82520_(0.0, a.m_20206_() * 0.6, 0.0);
        Vec3 chestB = b.m_20182_().m_82520_(0.0, b.m_20206_() * 0.6, 0.0);
        Vec3 at = chestA.m_82549_(chestB).m_82490_(0.5);
        Vec3 way = chestB.m_82546_(chestA);
        burst(a, at, way.m_82556_() < 1.0E-6 ? a.m_20154_() : way.m_82541_(), 1.2F, 1.3F);
    }

    /**
     * 1.0.42: the wind of a clash at {@code at}, blown out square to {@code way} (a unit vector), {@code power} strong (1.2 two blows
     * meeting), seen and heard by every client near {@code near}, as loud as {@code volume}.
     */
    public static void burst(LivingEntity near, Vec3 at, Vec3 way, float power, float volume) {
        float[] data = {(float) at.f_82479_, (float) at.f_82480_, (float) at.f_82481_, (float) way.f_82479_, (float) way.f_82480_, (float) way.f_82481_,
            power, 0.0F, 0.0F};
        ControlNetwork.broadcastAround(near, new S2CKingFx(near.m_20148_(), S2CKingFx.MARK, new int[] {KingMarkFx.CLASH, 0}, data));
        if (near.m_9236_() instanceof ServerLevel level) {
            float pitch = 1.25F + (1.2F - power) * 0.4F;
            level.m_6263_(null, at.f_82479_, at.f_82480_, at.f_82481_, HexKaguneSounds.KAGUNE_SLAM.get(), SoundSource.PLAYERS, volume, pitch);
            UkakuFire.sound(level, at, "entity.ender_dragon.flap", volume * 1.08F, 1.5F);
            UkakuFire.sound(level, at, "hexkagune:sfx.clash", volume * 0.92F, 1.0F); // 1.2.8: kagune on kagune: an impact
        }
    }
}
