package com.hex.kagune.control.server;

import com.hex.kagune.control.ClashRules;
import com.hex.kagune.control.Safety;
import com.hex.kagune.control.king.KingMarkFx;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CKingFx;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
     * owner looks), what it goes for (0: whatever is in front), whether it is still going, how it is cut off; 1.3.62: how hard it is (the
     * damage it would do).
     */
    private record Swing(LivingEntity who, long began, long until, double reach, Vec3 aim, int target, BooleanSupplier live, Runnable stop,
                         int[] shards, double hit) {
    }

    private static final Map<UUID, Swing> SWINGS = new HashMap<>();
    /** 1.0.42: when each body's last blow set out (kept after it lands: Rinkaku's grab asks whether one was swung since it set out). */
    private static final Map<UUID, Long> LAST = WorldTimes.map();
    /** Blows long gone are let go of this many ticks after they could have landed. */
    private static final long FORGET = 200L;

    private KaguneClash() {
    }

    /**
     * {@code player}'s M1 set out {@code now}, landing within {@code lands} ticks, {@code reach} blocks long; {@code live} true while it
     * is still that blow (not cut off by anything since), {@code stop} cuts it off.
     */
    public static void swing(ServerPlayer player, long now, int lands, double reach, BooleanSupplier live, Runnable stop) {
        swing(player, now, lands, reach, null, 0, ClashRules.HIT, live, stop);
    }

    /** 1.3.62: as above, {@code hit} hard (the damage it would do: the clash it makes is as big as both blows between them). */
    public static void swing(ServerPlayer player, long now, int lands, double reach, double hit, BooleanSupplier live, Runnable stop) {
        swing(player, now, lands, reach, null, 0, hit, live, stop);
    }

    /** 1.0.41: as above for any body: aimed along {@code aim} (null: the way it looks) at {@code target} (an entity id, 0: none in particular). */
    public static void swing(LivingEntity who, long now, int lands, double reach, Vec3 aim, int target, BooleanSupplier live, Runnable stop) {
        swing(who, now, lands, reach, aim, target, ClashRules.HIT, live, stop);
    }

    /** 1.3.62: as above, {@code hit} hard. */
    public static void swing(LivingEntity who, long now, int lands, double reach, Vec3 aim, int target, double hit, BooleanSupplier live,
                             Runnable stop) {
        if (SWINGS.size() > 64) {
            SWINGS.values().removeIf(old -> now > old.until() + FORGET || old.who().m_213877_());
        }
        SWINGS.put(who.m_20148_(), new Swing(who, now, now + Math.max(0, lands), reach, aim, target, live, stop, new int[1], hit));
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
        // 1.3.62: as big as both blows were hard between them.
        double hitMine = mine == null ? ClashRules.HIT : mine.hit();
        float power = ClashRules.impact(hitMine, theirs.hit());
        // 2.0.11: the weaker thrown further; overwhelmed by a big enough gap, flung far, down and stunned.
        apart(striker, other, power, ClashRules.ratio(hitMine, theirs.hit()), theirs.hit(), now);
        apart(other, striker, power, ClashRules.ratio(theirs.hit(), hitMine), hitMine, now);
        wind(striker, other, power);
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

    /**
     * 3.2.4: {@code player}'s blow in a clash on the CCG's scale ({@link com.hex.kagune.control.ClashPower}): how far they have come
     * (mastery, the share of their path's gates open, the ghoul meat in them), their form and mutations ({@code kagune}: a kagune's blow,
     * which they make harder), {@code weight} how heavy this blow is next to an ordinary one.
     */
    public static double strength(ServerPlayer player, double weight, boolean kagune) {
        int mastery = com.hex.kagune.data.KaguneProfile.mastery(player);
        com.hex.kagune.control.KagunePath path = PathServer.path(player);
        int on = 0;
        int open = 0;
        for (com.hex.kagune.control.quest.Gate gate : com.hex.kagune.control.quest.Gate.values()) {
            if (gate.on(path)) {
                on++;
                if (QuestServer.open(player, gate)) {
                    open++;
                }
            }
        }
        double gates = on == 0 ? 0.0 : (double)open / on;
        double factor = kagune ? com.hex.kagune.data.KaguneProfile.swipeDamageLegacy(player) / (6.0 + 0.006 * mastery) : 1.0;
        return com.hex.kagune.control.ClashPower.player(com.hex.kagune.control.ClashPower.progress(mastery, gates, GhoulMeatServer.eaten(player)),
            com.hex.kagune.control.ClashPower.build(factor), weight);
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

    /** 1.3.63: {@code a} and {@code b} meet in a clash of {@code power}: both thrown apart, the wind of it, the ground under it shaking. */
    static void collide(LivingEntity a, LivingEntity b, float power) {
        apart(a, b, power);
        apart(b, a, power);
        wind(a, b, power);
    }

    /**
     * 2.0.11: {@code who} thrown back from {@code from} by the gap between their blows ({@code ratio}: how much harder the other's was): the
     * weaker further. Overwhelmed ({@link ClashRules#overwhelmed}; the owner: "pretty much the same effect entirely of the kick without the
     * animation, send them through blocks"): sent off as the Speed Kick sends a body (its speed, its rise, {@code BlockSmash.kick}'s flight
     * through what it can break and into what is in the way, {@code strongerHit} its crash), down where it lands and stunned till it is up
     * ({@code PaleKnockdown}), every client drawing it tumbling, lying there and getting up. A body flown by an NPC breaks no blocks (the
     * kick's smashing is a player's).
     */
    private static void apart(LivingEntity who, LivingEntity from, float power, double ratio, double strongerHit, long now) {
        if (!ClashRules.overwhelmed(ratio)) {
            push(who, from, ClashRules.push(power, ratio), ClashRules.LIFT);
            return;
        }
        double resist = Math.max(0.0, Math.min(1.0, who.m_21133_(net.minecraft.world.entity.ai.attributes.Attributes.f_22278_)));
        double speed = com.hex.kagune.control.pale.PaleRules.KICK_SPEED * (1.0 - com.hex.kagune.control.pale.PaleRules.KICK_RESIST * resist);
        Vec3 away = push(who, from, 1.0, 0.0);
        Vec3 launch = away.m_82490_(speed).m_82520_(0.0, com.hex.kagune.control.pale.PaleRules.KICK_UP, 0.0);
        if (from instanceof ServerPlayer player) {
            BlockSmash.kick(player, who, away, speed, com.hex.kagune.control.pale.PaleRules.KICK_UP, (float)strongerHit);
        } else {
            who.m_20256_(launch);
            who.f_19864_ = true;
        }
        PaleKnockdown.launched(who, now, com.hex.kagune.control.pale.PaleRules.KICK_DOWN_TICKS, true);
        PaleBlitz.fxAround(who, from.m_19879_(), com.hex.kagune.control.network.S2CPaleFx.KICKED, who.m_20182_(), launch, 1.0F, 0);
        if (who.m_9236_() instanceof ServerLevel level) {
            UkakuFire.sound(level, who.m_20182_(), "entity.player.attack.knockback", 1.4F, 0.55F);
        }
    }

    /** {@code who} sent off from {@code from}: {@code push} blocks a tick across, {@code lift} up. Its launch. */
    private static Vec3 push(LivingEntity who, LivingEntity from, double push, double lift) {
        double dx = who.m_20185_() - from.m_20185_();
        double dz = who.m_20189_() - from.m_20189_();
        double l = Math.sqrt(dx * dx + dz * dz);
        if (l < 1.0E-4) {
            Vec3 look = who.m_20154_();
            dx = -look.f_82479_;
            dz = -look.f_82481_;
            l = Math.max(1.0E-4, Math.sqrt(dx * dx + dz * dz));
        }
        Vec3 launch = new Vec3(dx / l * push, lift, dz / l * push);
        who.m_20256_(launch);
        who.f_19864_ = true;
        who.f_19789_ = 0.0F;
        return launch;
    }

    /** {@code who} thrown back from {@code from}, the harder the bigger the clash ({@code power}). */
    private static void apart(LivingEntity who, LivingEntity from, float power) {
        push(who, from, ClashRules.push(power), ClashRules.LIFT);
    }

    /**
     * The wind of it, between their chests, blown out square to the line they met on; the crack and the gust of it. 1.3.62: as big as
     * {@code power}; hard enough ({@link ClashRules#quakes}), the ground under them shakes (a ring of dust, the view of all near shaken,
     * a deep rumble).
     */
    private static void wind(LivingEntity a, LivingEntity b, float power) {
        Vec3 chestA = a.m_20182_().m_82520_(0.0, a.m_20206_() * 0.6, 0.0);
        Vec3 chestB = b.m_20182_().m_82520_(0.0, b.m_20206_() * 0.6, 0.0);
        Vec3 at = chestA.m_82549_(chestB).m_82490_(0.5);
        Vec3 way = chestB.m_82546_(chestA);
        burst(a, at, way.m_82556_() < 1.0E-6 ? a.m_20154_() : way.m_82541_(), power, ClashRules.volume(power));
        if (ClashRules.quakes(power)) {
            quake(a, new Vec3(at.f_82479_, Math.min(a.m_20186_(), b.m_20186_()) + 0.05, at.f_82481_), power);
        }
    }

    /** 1.3.62: the ground shaking under a clash of {@code power} at {@code ground}: its ring of dust and rumble for every client near. */
    public static void quake(LivingEntity near, Vec3 ground, float power) {
        float[] data = {(float)ground.f_82479_, (float)ground.f_82480_, (float)ground.f_82481_, 0.0F, 1.0F, 0.0F, power - 0.6F, 0.0F, 0.0F};
        ControlNetwork.broadcastAround(near, new S2CKingFx(near.m_20148_(), S2CKingFx.MARK, new int[] {KingMarkFx.GROUND, 0}, data));
        if (near.m_9236_() instanceof ServerLevel level) {
            UkakuFire.sound(level, ground, "hexkagune:sfx.quake", ClashRules.volume(power) * 0.8F, 0.85F);
        }
    }

    /**
     * 1.0.42: the wind of a clash at {@code at}, blown out square to {@code way} (a unit vector), {@code power} strong (1.2 two blows
     * meeting), seen and heard by every client near {@code near}, as loud as {@code volume}.
     */
    public static void burst(LivingEntity near, Vec3 at, Vec3 way, float power, float volume) {
        float[] data = {(float) at.f_82479_, (float) at.f_82480_, (float) at.f_82481_, (float) way.f_82479_, (float) way.f_82480_, (float) way.f_82481_,
            ClashRules.shown(power), 0.0F, 0.0F}; // 2.0.11: drawn bigger than its power
        ControlNetwork.broadcastAround(near, new S2CKingFx(near.m_20148_(), S2CKingFx.MARK, new int[] {KingMarkFx.CLASH, 0}, data));
        if (near.m_9236_() instanceof ServerLevel level) {
            // 1.3.61 (the owner: "use the new sounds I sent as the new clash sounds whenever there's an impact"): their clash is the
            // impact itself (sfx.clash: Clashes.zip's four), with the gust of it; the heavy blow it was layered over is gone.
            // 2.0.11: louder, laid over itself the harder it is (one sound is never louder than full), each a shade apart.
            for (int layer = 0; layer < ClashRules.layers(power); layer++) {
                UkakuFire.sound(level, at, "hexkagune:sfx.clash", volume, ClashRules.pitch(power) * (1.0F - 0.04F * layer));
            }
            UkakuFire.sound(level, at, "entity.ender_dragon.flap", volume * 0.8F, 1.5F);
        }
    }
}
