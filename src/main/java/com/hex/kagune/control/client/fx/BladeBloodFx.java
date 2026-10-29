package com.hex.kagune.control.client.fx;

import com.hex.kagune.control.network.S2CUkakuFx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 5.0.20: blood from the Steel Blade ({@link S2CUkakuFx#BLADE_HIT}). Each cut flings a sheet of blood along the way the blade
 * went, and more is thrown off the blade as it carries through; a kill bursts, and the body gushes as it falls, pooling under it.
 * The blade itself is bloodied ({@link #stain}): fresh and red after a cut, more after a kill, darkening as it dries and wearing
 * off over a minute. Drops, marks and pools are {@link UkakuBlood}'s.
 */
public final class BladeBloodFx {
    /** How much a cut and a kill bloody the blade (0..1), how long until it is dry, and until it has worn off (ticks). */
    static final double CUT = 0.35;
    static final double KILL = 0.75;
    static final double DRIES = 600.0;
    static final double WEARS = 1400.0;

    private record Stain(double amount, long at) {
    }

    private record Gush(int body, Vec3 way, long until, long next) {
    }

    private static final Map<Integer, Stain> STAINS = new HashMap<>();
    private static final List<Gush> GUSHES = new ArrayList<>();

    private BladeBloodFx() {
    }

    /** A cut landed ({@code way}: the blade's way, unit; {@code near}: close enough to the camera to show). */
    static void hit(ClientLevel level, S2CUkakuFx message, Vec3 at, Vec3 way, boolean near) {
        long now = level.m_46467_();
        boolean killed = message.extra() == 1;
        float power = Math.max(0.4F, message.size());
        Entity body = level.m_6815_(message.id());
        if (near && way.m_82556_() > 1.0E-6) {
            Vec3 cut = way.m_82541_();
            // The sheet along the cut, then what the blade throws off as it carries through: further along, rising a little.
            UkakuBlood.spurt(level, at, cut, power, body);
            UkakuBlood.spurt(level, at.m_82549_(cut.m_82490_(0.35)), cut.m_82520_(0.0, 0.3, 0.0), power * 0.55F, body);
            if (killed) {
                // The kill: a burst up and out of the wound, and the body gushing as it goes down.
                UkakuBlood.spurt(level, at, cut.m_82490_(0.5).m_82520_(0.0, 1.0, 0.0), Math.min(2.5F, power * 1.6F), body);
                if (GUSHES.size() < 32) {
                    GUSHES.add(new Gush(message.id(), cut, now + 18, now + 3));
                }
            }
        }
        Stain was = STAINS.get(message.shooter());
        double left = was == null ? 0.0 : amount(was, now);
        STAINS.put(message.shooter(), new Stain(Math.min(1.0, left + (killed ? KILL : CUT)), now));
    }

    /** Each client tick: bodies cut down gush in beats as they fall. */
    static void tick(ClientLevel level) {
        long now = level.m_46467_();
        List<Gush> again = new ArrayList<>();
        for (Iterator<Gush> it = GUSHES.iterator(); it.hasNext(); ) {
            Gush gush = it.next();
            if (now >= gush.until()) {
                it.remove();
                continue;
            }
            if (now < gush.next()) {
                continue;
            }
            it.remove();
            Entity body = level.m_6815_(gush.body());
            if (body != null) {
                float left = (float)(gush.until() - now) / 18.0F;
                Vec3 from = body.m_20182_().m_82520_(0.0, body.m_20206_() * (0.25 + 0.35 * left), 0.0);
                UkakuBlood.spurt(level, from, gush.way().m_82520_(0.0, 0.6, 0.0), 0.5F + 0.9F * left, body);
                again.add(new Gush(gush.body(), gush.way(), gush.until(), now + 4));
            }
        }
        GUSHES.addAll(again);
        STAINS.values().removeIf(stain -> now - stain.at() > WEARS);
    }

    static void clear() {
        STAINS.clear();
        GUSHES.clear();
    }

    /** {amount 0..1, dryness 0..1} of blood on {@code player}'s blade at {@code now} (world ticks). */
    public static double[] stain(int player, double now) {
        Stain stain = STAINS.get(player);
        if (stain == null) {
            return new double[] {0.0, 0.0};
        }
        double age = Math.max(0.0, now - stain.at());
        return new double[] {amount(stain, now), Math.min(1.0, age / DRIES)};
    }

    private static double amount(Stain stain, double now) {
        double age = Math.max(0.0, now - stain.at());
        return stain.amount() * Math.max(0.0, 1.0 - age / WEARS);
    }
}
