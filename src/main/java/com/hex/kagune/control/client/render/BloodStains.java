package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Blood on the ground from Ukaku's crystal spikes, ported from Hex God of Time ({@code client/BloodStains}, with its
 * textures): puddles that spread, run together and dry, and the marks drops leave where they land.
 *
 * <p>A puddle is fed rather than stacked: blood running into a puddle that is already there widens it, so a body bleeding
 * where it stands grows one puddle under it. Fresh blood shines (the sky's highlights catch more the lower you look across
 * it), then darkens and dulls as it dries, still red, and at the end of its time thins away. A drop's mark lies where the
 * drop came down, stretched the way it was going, or round with a splashed crown if it fell straight; one landing in a
 * puddle runs into it instead.
 *
 * <p>Simpler than the original in two ways, to keep to the render API the mod already uses: a stain only lies where the
 * ground under all of it is the same surface (checked by looking down at its corners as it grows: it stops growing at a
 * ledge), and stains are drawn with the entity translucent type before the wings, each a hair above the last so they
 * never fight.
 */
public final class BloodStains {
    static final int POOL_KINDS = 6;
    static final int MARK_KINDS = 4;
    /** The round, crowned mark of a drop that fell straight down; the others are thrown drops'. */
    static final int FALLEN = 0;
    private static final RenderType[] POOL = new RenderType[POOL_KINDS];
    private static final RenderType[] SHEEN = new RenderType[POOL_KINDS];
    private static final RenderType[] MARK = new RenderType[MARK_KINDS];

    static {
        for (int i = 0; i < POOL_KINDS; i++) {
            POOL[i] = type("textures/blood/pool_" + i + ".png");
            SHEEN[i] = type("textures/blood/pool_" + i + "_sheen.png");
        }
        for (int i = 0; i < MARK_KINDS; i++) {
            MARK[i] = type("textures/blood/spatter_" + i + ".png");
        }
    }

    /** Stains at once; past this the oldest goes. */
    private static final int MOST = 720;
    /** The widest a single puddle grows, half across, in blocks. */
    private static final double WIDEST = 1.35;
    /** How close (as a share of its half-width) blood has to land to run into a puddle rather than start its own. */
    private static final double INTO = 0.6;
    private static final double SEEN = 96.0 * 96.0;
    private static final int MARKS_A_TICK = 28;
    /** Fresh blood's tint and dry blood's: both red. Drops' marks dry faster than a puddle does. */
    private static final float[] FRESH = {0.86F, 0.03F, 0.035F};
    private static final float[] DRY = {0.55F, 0.035F, 0.03F};
    private static final int WET = 240;
    private static final int DRYING = 520;
    private static final int MARK_WET = 80;
    private static final int MARK_DRYING = 240;
    /** The share of its life over which a stain thins away at the end, and its sheen's strongest. */
    private static final float FADE = 0.2F;
    private static final float SHINE = 0.55F;
    /** Height over the ground, and the extra step between one stain and the next (so overlapping stains never fight). */
    private static final double LIFT = 0.004;
    private static final double LAYER = 0.0007;
    private static final int LAYERS = 10;

    private static final class Stain {
        final double x;
        final double y;
        final double z;
        /** The texture's forward (+V) is (sin yaw, cos yaw) across the ground: a thrown drop's mark points the way it went. */
        final float yaw;
        final float stretch;
        final boolean pool;
        final int kind;
        final long born;
        final int layer;
        /** Half-width it is spreading to, half-width drawn this tick and last, how fast it gets there, and the widest it may. */
        double size;
        double shown;
        double before;
        float rate;
        double room;
        long fed;
        int life;
        int light;

        Stain(double x, double y, double z, float yaw, float stretch, boolean pool, int kind, double size, double start, int spread, int life, long now,
              int layer) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.stretch = stretch;
            this.pool = pool;
            this.kind = kind;
            this.size = size;
            this.shown = start;
            this.before = start;
            this.rate = 1.0F - (float)Math.exp(-3.0 / Math.max(1, spread));
            this.born = now;
            this.fed = now;
            this.life = life;
            this.layer = layer;
        }
    }

    private static final List<Stain> STAINS = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static int marked;
    private static long markedTick = Long.MIN_VALUE;
    private static int serial;

    private BloodStains() {
    }

    private static RenderType type(String path) {
        return RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id(path)));
    }

    public static void clear() {
        STAINS.clear();
    }

    /** Blood run onto the ground at {@code at} (on its surface): into the puddle it lands in, which widens, or a new one. */
    public static void pool(ClientLevel level, Vec3 at, double size, int life, int spread) {
        long now = level.m_46467_();
        Stain into = into(at, 0.0);
        if (into != null) {
            // Fed past the room it has, it looks further out on its next tick (and stops at a ledge).
            into.size = Math.min(WIDEST, Math.sqrt(into.size * into.size + size * size * 0.5));
            into.rate = Math.max(into.rate, 1.0F - (float)Math.exp(-3.0 / Math.max(1, spread)));
            into.fed = now;
            into.life = Math.max(into.life, life);
            return;
        }
        double room = room(level, at, Math.min(WIDEST, size * 2.5), 8, 4);
        if (room < 0.06) {
            return;
        }
        add(level, new Stain(at.f_82479_, at.f_82480_, at.f_82481_, RANDOM.nextFloat() * (float)(Math.PI * 2.0), 1.0F, true, RANDOM.nextInt(POOL_KINDS),
            Math.min(room, size), Math.min(room, size) * 0.35, spread, life, now, serial++ % LAYERS)).room = room;
    }

    /**
     * A drop's mark where it came down: stretched along its way ({@code vx}, {@code vz}), the faster the longer, or crowned
     * and round if it fell nearly straight; into a puddle, if it came down in one.
     */
    public static void mark(ClientLevel level, Vec3 at, double vx, double vz, double size, int life) {
        long now = level.m_46467_();
        if (markedTick != now) {
            markedTick = now;
            marked = 0;
        }
        Stain into = into(at, 0.02);
        if (into != null) {
            into.size = Math.min(WIDEST, Math.sqrt(into.size * into.size + size * size * 0.3));
            into.fed = now;
            return;
        }
        if (marked >= MARKS_A_TICK) {
            return;
        }
        double speed = Math.sqrt(vx * vx + vz * vz);
        boolean thrown = speed > 0.04;
        float stretch = thrown ? (float)Math.min(2.8, 1.25 + speed * 9.0) : 1.0F;
        if (room(level, at, size * stretch, 4, 1) < size * stretch * 0.9) {
            return; // on an edge: a mark hanging off it would float
        }
        marked++;
        float yaw = thrown ? (float)Math.atan2(vx, vz) : RANDOM.nextFloat() * (float)(Math.PI * 2.0);
        int kind = thrown ? 1 + RANDOM.nextInt(MARK_KINDS - 1) : FALLEN;
        add(level, new Stain(at.f_82479_, at.f_82480_, at.f_82481_, yaw, stretch, false, kind, size, size, 1, life, now, serial++ % LAYERS)).room = size;
    }

    /** The fresh puddle this lands in, on the same surface, well inside it; or none. */
    private static Stain into(Vec3 at, double margin) {
        Stain best = null;
        double nearest = Double.MAX_VALUE;
        for (Stain stain : STAINS) {
            if (!stain.pool || Math.abs(stain.y - at.f_82480_) > 0.03 || stain.size >= Math.min(WIDEST, stain.room)) {
                continue;
            }
            double dx = stain.x - at.f_82479_;
            double dz = stain.z - at.f_82481_;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < Math.max(stain.shown, stain.size * 0.5) * INTO + margin && d < nearest) {
                nearest = d;
                best = stain;
            }
        }
        return best;
    }

    /**
     * How wide (half across) a stain at {@code at} can lie before it hangs off the surface it is on: the ground is looked at
     * in {@code samples} directions out to {@code wanted}, shrinking (up to {@code attempts} times) until all of it is the
     * same surface.
     */
    private static double room(ClientLevel level, Vec3 at, double wanted, int samples, int attempts) {
        double half = wanted;
        for (int attempt = 0; attempt < attempts && half >= 0.05; attempt++) {
            boolean held = true;
            for (int k = 0; k < samples && held; k++) {
                double angle = Math.PI * 2.0 * k / samples + Math.PI / 4.0;
                held = surface(level, at.f_82479_ + Math.cos(angle) * half, at.f_82480_, at.f_82481_ + Math.sin(angle) * half);
            }
            if (held) {
                return half;
            }
            half *= 0.6;
        }
        return 0.0;
    }

    /** True if the ground at {@code x, z} is the surface at height {@code y} (nothing solid over it, nothing missing under it). */
    private static boolean surface(ClientLevel level, double x, double y, double z) {
        LocalPlayer viewer = Minecraft.m_91087_().f_91074_;
        if (viewer == null) {
            return false;
        }
        Vec3 from = new Vec3(x, y + 0.3, z);
        BlockHitResult hit = level.m_45547_(new ClipContext(from, new Vec3(x, y - 0.3, z), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));
        return hit.m_6662_() == HitResult.Type.BLOCK && Math.abs(hit.m_82450_().f_82480_ - y) < 0.03;
    }

    private static Stain add(ClientLevel level, Stain stain) {
        if (STAINS.size() >= MOST) {
            STAINS.remove(0);
        }
        stain.light = LevelRenderer.m_109541_(level, BlockPos.m_274561_(stain.x, stain.y + 0.1, stain.z));
        STAINS.add(stain);
        return stain;
    }

    /** Each client tick: spreading (never past the room it has), ageing, the light on each. */
    public static void tick(ClientLevel level) {
        if (STAINS.isEmpty()) {
            return;
        }
        long now = level.m_46467_();
        STAINS.removeIf(stain -> now - stain.fed > stain.life);
        for (int i = 0; i < STAINS.size(); i++) {
            Stain stain = STAINS.get(i);
            stain.before = stain.shown;
            if (stain.pool && stain.size > stain.room && (now + i) % 4 == 0) {
                // Fed past the room it was given: look again further out (it may lie on a wide floor), or stop there.
                stain.room = Math.max(stain.room, room(level, new Vec3(stain.x, stain.y, stain.z), Math.min(WIDEST, stain.size), 8, 3));
                stain.size = Math.min(stain.size, stain.room);
            }
            stain.shown += (stain.size - stain.shown) * stain.rate;
            if (Math.abs(stain.size - stain.shown) < 1.0E-3) {
                stain.shown = stain.size;
            }
            if ((now + i) % 5 == 0) {
                stain.light = LevelRenderer.m_109541_(level, BlockPos.m_274561_(stain.x, stain.y + 0.1, stain.z));
            }
        }
    }

    /**
     * Draws every stain (camera-relative): the puddles, then the drops' marks, then the shine over every wet puddle. Called
     * by the wing renderer before the wings, so translucent wings over blood still show it.
     */
    public static void render(MultiBufferSource.BufferSource buffers, Matrix4f matrix, Matrix3f normals, Vec3 camera, float partial, long now) {
        if (STAINS.isEmpty()) {
            return;
        }
        for (int kind = 0; kind < POOL_KINDS; kind++) {
            draw(buffers, POOL[kind], true, kind, false, camera, partial, now, matrix, normals);
        }
        for (int kind = 0; kind < MARK_KINDS; kind++) {
            draw(buffers, MARK[kind], false, kind, false, camera, partial, now, matrix, normals);
        }
        for (int kind = 0; kind < POOL_KINDS; kind++) {
            draw(buffers, SHEEN[kind], true, kind, true, camera, partial, now, matrix, normals);
        }
    }

    private static void draw(MultiBufferSource.BufferSource buffers, RenderType type, boolean pools, int kind, boolean sheen, Vec3 eye, float partial,
                             long now, Matrix4f matrix, Matrix3f normals) {
        VertexConsumer out = null;
        for (Stain stain : STAINS) {
            if (stain.pool != pools || stain.kind != kind) {
                continue;
            }
            double dx = stain.x - eye.f_82479_;
            double dy = stain.y - eye.f_82480_;
            double dz = stain.z - eye.f_82481_;
            double away = dx * dx + dy * dy + dz * dz;
            if (away > SEEN) {
                continue;
            }
            float age = (float)(now - stain.fed + partial);
            float left = stain.life - age;
            float alpha = clamp(left / (stain.life * FADE)) * clamp((float)(now - stain.born + partial) / 2.0F);
            float wet = wetness(stain, age);
            float r;
            float g;
            float b;
            if (sheen) {
                if (wet <= 0.02F) {
                    continue;
                }
                // The sky in it: catching more the lower the eye looks across it.
                double steep = Math.abs(dy) / Math.sqrt(Math.max(away, 1.0E-6));
                float grazing = (float)(1.0 - steep);
                alpha *= SHINE * wet * (0.22F + 0.78F * grazing * grazing);
                r = 1.0F;
                g = 1.0F;
                b = 1.0F;
            } else {
                r = DRY[0] + (FRESH[0] - DRY[0]) * wet;
                g = DRY[1] + (FRESH[1] - DRY[1]) * wet;
                b = DRY[2] + (FRESH[2] - DRY[2]) * wet;
            }
            if (alpha <= 0.01F) {
                continue;
            }
            if (out == null) {
                out = buffers.m_6299_(type);
            }
            double half = stain.before + (stain.shown - stain.before) * partial;
            quad(out, stain, half, eye, matrix, normals, r, g, b, alpha, sheen);
        }
        if (out != null) {
            buffers.m_109912_(type);
        }
    }

    /** 1 while fresh, down to 0 as it dries: a puddle stays wet longer, and blood running into it wets it again. */
    private static float wetness(Stain stain, float age) {
        int wet = stain.pool ? WET : MARK_WET;
        int drying = stain.pool ? DRYING : MARK_DRYING;
        return 1.0F - clamp((age - wet) / drying);
    }

    /** The stain: its texture turned and stretched across the ground, a hair above it (each layer a little higher). */
    private static void quad(VertexConsumer out, Stain stain, double half, Vec3 eye, Matrix4f matrix, Matrix3f normals, float r, float g, float b,
                             float alpha, boolean sheen) {
        if (half <= 1.0E-3) {
            return;
        }
        double length = half * stain.stretch;
        double fx = Math.sin(stain.yaw);
        double fz = Math.cos(stain.yaw);
        double rx = fz;
        double rz = -fx;
        float y = (float)(stain.y - eye.f_82480_ + LIFT + LAYER * stain.layer + (sheen ? LAYER * 0.5 : 0.0));
        int a8 = Math.max(0, Math.min(255, (int)(alpha * 255.0F)));
        int r8 = (int)(r * 255.0F);
        int g8 = (int)(g * 255.0F);
        int b8 = (int)(b * 255.0F);
        double[][] corners = {{-1.0, -1.0}, {-1.0, 1.0}, {1.0, 1.0}, {1.0, -1.0}};
        for (double[] corner : corners) {
            double side = corner[0] * half;
            double along = corner[1] * length;
            float x = (float)(stain.x + rx * side + fx * along - eye.f_82479_);
            float z = (float)(stain.z + rz * side + fz * along - eye.f_82481_);
            out.m_252986_(matrix, x, y, z)
                .m_6122_(r8, g8, b8, a8)
                .m_7421_((float)(corner[0] * 0.5 + 0.5), (float)(corner[1] * 0.5 + 0.5))
                .m_86008_(OverlayTexture.f_118083_)
                .m_85969_(stain.light)
                .m_252939_(normals, 0.0F, 1.0F, 0.0F)
                .m_5752_();
        }
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

}
