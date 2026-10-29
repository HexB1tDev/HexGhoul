package com.hex.kagune.control.client.fx;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.client.render.BloodStains;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

/**
 * Blood from Ayato's crystal spikes, ported from Hex God of Time ({@code client/Blood}, {@code HexParticles.Drip}; its
 * assets, free to use): seen rather than counted.
 *
 * <p>A body bleeding from the spikes bleeds from each spike still in it (from the body itself once they have burst): drops
 * fall from the wounds, a still body pools where it stands and a running one leaves a trail of small pools. A spike going
 * in or bursting throws a spurt: a sheet of blood flung the way it went, gobs arcing out, a fine spray, pools spattered
 * along where it lands. Drops are Photon motes ({@link RcFx}) with Hex God of Time's blood sprites; each knows where it
 * will come down (worked out once, when it is thrown), and about half of them leave a mark there ({@link BloodStains}).
 *
 * <p>Hex God of Time already draws its own blood and stains for the original's {@code hexkagune:bleeding} when it is
 * installed, so then the steady bleeding is left to it and only the spurts (which it doesn't know about) are drawn here:
 * blood is never doubled.
 */
public final class AyatoBlood {
    private static final ResourceLocation[] DROPS = {
        KaguneMod.id("textures/particle/blood_0.png"), KaguneMod.id("textures/particle/blood_1.png"),
        KaguneMod.id("textures/particle/blood_2.png"), KaguneMod.id("textures/particle/blood_3.png")};
    /** A drop's weight and drag (Hex God of Time's Drip: gravity 0.75 of vanilla's 0.04, friction 0.98). */
    static final double GRAVITY = 0.03;
    static final double DRAG = 0.98;
    /** The longest a drop falls before it is gone, whatever is under it (ticks). */
    static final int LONGEST = 60;
    /** Share of the drops that come down which leave a mark. */
    private static final float MARKS = 0.45F;
    /** How long a drop's mark and a small pool lie, and a puddle, after blood last ran into it (ticks). */
    private static final int LIFE = 600;
    private static final int PUDDLE_LIFE = 1200;
    private static final Random RANDOM = new Random();

    private record Landing(long at, double x, double y, double z, double vx, double vz, float size) {
    }

    private static final List<Landing> LANDINGS = new ArrayList<>();
    private static final Map<Integer, Vec3> LAST = new HashMap<>();
    private static final Set<Integer> SEEN = new HashSet<>();
    private static Boolean foreign;

    private AyatoBlood() {
    }

    /** True when Hex God of Time is installed: it draws the original's bleeding itself. */
    public static boolean foreign() {
        if (foreign == null) {
            ModList mods = ModList.get();
            foreign = mods != null && mods.isLoaded("hexgodofstories");
        }
        return foreign;
    }

    /**
     * One tick of a body bleeding from the spikes: {@code stacks} as the server counts them, {@code wounds} where the
     * spikes still in it are. Left to Hex God of Time when it is installed.
     */
    static void bleed(ClientLevel level, LivingEntity body, int stacks, List<Vec3> wounds) {
        if (foreign()) {
            return;
        }
        int id = body.m_19879_();
        SEEN.add(id);
        // Shown like Hex God of Time shows the original's bleeding: two stacks to start, up to five.
        int shown = Math.max(1, Math.min(5, 1 + (stacks + 1) / 2));
        for (int i = 0; i < shown; i++) {
            if (RANDOM.nextInt(2) != 0) {
                continue;
            }
            Vec3 at = wounds.isEmpty()
                ? body.m_20182_().m_82520_((RANDOM.nextDouble() - 0.5) * body.m_20205_(), body.m_20206_() * (0.35 + RANDOM.nextDouble() * 0.4),
                    (RANDOM.nextDouble() - 0.5) * body.m_20205_())
                : wounds.get(RANDOM.nextInt(wounds.size()));
            drop(level, body, at, new Vec3((RANDOM.nextDouble() - 0.5) * 0.03, -0.04, (RANDOM.nextDouble() - 0.5) * 0.03), true);
        }
        Vec3 previous = LAST.put(id, body.m_20182_());
        if (previous == null) {
            return;
        }
        boolean moving = previous.m_82557_(body.m_20182_()) > 0.0016;
        // A still body pools where it stands, the one puddle widening; a running one leaves a trail of small ones behind it.
        int odds = moving ? Math.max(2, 7 - shown) : 10;
        if (RANDOM.nextInt(odds) != 0) {
            return;
        }
        if (moving) {
            pool(level, body, null, RANDOM.nextDouble() * 0.12 + 0.1, LIFE, 10);
        } else {
            pool(level, body, null, RANDOM.nextDouble() * 0.2 + 0.22, PUDDLE_LIFE, 30);
        }
    }

    /**
     * A spike going in or bursting out: a sheet of blood flung along {@code way}, the fastest drops furthest; gobs arcing up
     * out of the wound; a fine spray every way; pools spattered along where it is thrown and under the body.
     */
    public static void spurt(ClientLevel level, Vec3 at, Vec3 way, float power, Entity wounded) {
        // Whose collisions the drops use: the wounded body, or (a body we don't know) this client's own player.
        Entity body = wounded != null ? wounded : net.minecraft.client.Minecraft.m_91087_().f_91074_;
        if (way.m_82556_() < 1.0E-6 || body == null) {
            return;
        }
        Vec3 swing = way.m_82541_();
        float strength = Math.max(0.3F, Math.min(2.5F, power));
        Vec3 across = swing.m_82537_(new Vec3(0.0, 1.0, 0.0));
        if (across.m_82556_() < 1.0E-4) {
            across = new Vec3(1.0, 0.0, 0.0);
        }
        across = across.m_82541_();
        for (int i = RcFx.count(34 * strength); i > 0; i--) {
            Vec3 from = at.m_82549_(across.m_82490_((RANDOM.nextDouble() - 0.5) * 0.4)).m_82549_(swing.m_82490_(RANDOM.nextDouble() * 0.1));
            double speed = (0.1 + RANDOM.nextDouble() * 0.36) * (0.65 + 0.35 * strength);
            drop(level, body, from, swing.m_82490_(speed).m_82520_((RANDOM.nextDouble() - 0.5) * 0.1, 0.03 + RANDOM.nextDouble() * 0.12,
                (RANDOM.nextDouble() - 0.5) * 0.1), true);
        }
        for (int i = RcFx.count(8 * strength); i > 0; i--) {
            drop(level, body, at.m_82549_(across.m_82490_((RANDOM.nextDouble() - 0.5) * 0.25)), swing.m_82490_(0.05 + RANDOM.nextDouble() * 0.15)
                .m_82520_((RANDOM.nextDouble() - 0.5) * 0.08, 0.1 + RANDOM.nextDouble() * 0.16, (RANDOM.nextDouble() - 0.5) * 0.08), true);
        }
        for (int i = RcFx.count(12 * strength); i > 0; i--) {
            drop(level, body, at, new Vec3((RANDOM.nextDouble() - 0.5) * 0.25, (RANDOM.nextDouble() - 0.3) * 0.18, (RANDOM.nextDouble() - 0.5) * 0.25),
                false);
        }
        if (wounded == null) {
            return;
        }
        for (int i = 0; i < Math.round(3 * strength) + 1; i++) {
            pool(level, body, at.m_82549_(swing.m_82490_(0.3 + RANDOM.nextDouble() * 1.8 * strength)).m_82520_((RANDOM.nextDouble() - 0.5) * 0.7, 0.0,
                (RANDOM.nextDouble() - 0.5) * 0.7), RANDOM.nextDouble() * 0.25 + 0.14, LIFE, 18);
        }
        pool(level, body, null, RANDOM.nextDouble() * 0.25 + 0.25, PUDDLE_LIFE, 40);
    }

    /** Each client tick: the drops that have come down leave their marks, the stains spread and dry. */
    static void tick(ClientLevel level) {
        long now = level.m_46467_();
        for (Iterator<Landing> iterator = LANDINGS.iterator(); iterator.hasNext(); ) {
            Landing landing = iterator.next();
            if (now >= landing.at()) {
                iterator.remove();
                BloodStains.mark(level, new Vec3(landing.x(), landing.y(), landing.z()), landing.vx(), landing.vz(),
                    landing.size() * (1.3 + RANDOM.nextFloat() * 0.9), LIFE);
            }
        }
        LAST.keySet().retainAll(SEEN);
        SEEN.clear();
        BloodStains.tick(level);
    }

    static void clear() {
        LANDINGS.clear();
        LAST.clear();
        SEEN.clear();
        BloodStains.clear();
    }

    /**
     * One drop thrown from {@code at} at {@code velocity}: falls until it comes down on whatever is below (worked out here, so
     * it never falls through the floor), and with {@code mayMark} about half the drops that come down leave a mark.
     */
    private static void drop(ClientLevel level, Entity body, Vec3 at, Vec3 velocity, boolean mayMark) {
        double ground = ground(level, body, at.f_82479_, at.f_82480_ + 0.05, at.f_82481_);
        double[] fall = fall(at, velocity, ground);
        boolean landed = fall[0] > 0.0;
        if (landed && mayMark && RANDOM.nextFloat() < MARKS && LANDINGS.size() < 512) {
            // Where it comes down may be on other ground than under where it left: look again there.
            double there = ground(level, body, fall[1], at.f_82480_ + 0.05, fall[3]);
            if (Math.abs(there - ground) > 0.25) {
                fall = fall(at, velocity, there);
                landed = fall[0] > 0.0;
                ground = there;
            }
            if (landed) {
                long now = level.m_46467_();
                LANDINGS.add(new Landing(now + (long)fall[4], fall[1], ground, fall[3], fall[5], fall[6], 0.042F + RANDOM.nextFloat() * 0.034F));
            }
        }
        int life = (int)Math.max(2, Math.min(LONGEST, fall[4]));
        float shade = 0.84F + RANDOM.nextFloat() * 0.16F;
        int red = 0xFF000000 | Math.round(235 * shade) << 16 | Math.round(6 * shade) << 8 | Math.round(9 * shade);
        float size = 0.085F + RANDOM.nextFloat() * 0.068F;
        RcFx.at(DROPS[RANDOM.nextInt(DROPS.length)], false, life, at).velocity(velocity).drag((float)DRAG).fall((float)GRAVITY).bloom(1)
            .colors(red, red & 0x00FFFFFF | 0xD0000000).size(size, size * 0.9F).spawn();
    }

    /**
     * A drop falling from {@code at} at {@code velocity} down to {@code ground}: {came down 1 or not 0, x, y, z where it ends,
     * ticks, vx, vz as it lands}.
     */
    static double[] fall(Vec3 at, Vec3 velocity, double ground) {
        double x = at.f_82479_;
        double y = at.f_82480_;
        double z = at.f_82481_;
        double vx = velocity.f_82479_;
        double vy = velocity.f_82480_;
        double vz = velocity.f_82481_;
        for (int tick = 1; tick <= LONGEST; tick++) {
            vy = (vy - GRAVITY) * DRAG;
            vx *= DRAG;
            vz *= DRAG;
            x += vx;
            y += vy;
            z += vz;
            if (y <= ground) {
                return new double[] {1.0, x, ground, z, tick, vx, vz};
            }
        }
        return new double[] {0.0, x, y, z, LONGEST, vx, vz};
    }

    /** The top of whatever is under {@code x, y, z} within six blocks (far below if nothing). */
    private static double ground(ClientLevel level, Entity body, double x, double y, double z) {
        Vec3 from = new Vec3(x, y, z);
        BlockHitResult hit = level.m_45547_(new ClipContext(from, from.m_82520_(0.0, -6.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, body));
        return hit.m_6662_() == HitResult.Type.BLOCK ? hit.m_82450_().f_82480_ : y - 64.0;
    }

    /**
     * Blood run onto the ground under {@code over} (anywhere under the body when null): into the puddle already there, which
     * widens, or a new one spreading out over about {@code spread} ticks.
     */
    private static void pool(ClientLevel level, Entity body, Vec3 over, double size, int life, int spread) {
        Vec3 from = over == null
            ? body.m_20182_().m_82520_((RANDOM.nextDouble() - 0.5) * body.m_20205_() * 0.8, 0.1, (RANDOM.nextDouble() - 0.5) * body.m_20205_() * 0.8)
            : new Vec3(over.f_82479_, Math.min(over.f_82480_, body.m_20186_() + 0.1), over.f_82481_);
        BlockHitResult hit = level.m_45547_(new ClipContext(from, from.m_82520_(0.0, -3.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, body));
        if (hit.m_6662_() != HitResult.Type.BLOCK || hit.m_82450_().f_82480_ > from.f_82480_ - 1.0E-3) {
            return; // nothing below within reach, or the blood would start inside a wall
        }
        BloodStains.pool(level, hit.m_82450_(), size, life, spread);
    }
}
