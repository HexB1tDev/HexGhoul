package com.hex.kagune.control.client.fx;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.client.render.KaguneWorldRenderer;
import com.hex.kagune.control.KakujaBurst;
import com.hex.kagune.control.KanekiForms;
import com.hex.kagune.control.ayato.CentipedeTail;
import com.hex.kagune.control.ayato.WingRig;
import com.hex.kagune.control.client.AyatoClient;
import com.hex.kagune.control.client.KakuganClient;
import com.hex.kagune.control.client.animation.BodyAnimations;
import com.hex.kagune.control.kakugan.CentipedeMask;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.TendrilMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Kaneki's kakuja bursting out of his body and shattering (5.0.0), on every client for every Kaneki player it sees. Each client
 * watches the synced state ({@link #observe}): when a player goes into a kakuja (K, or the kagune coming out in one) his legs
 * tear out of his back one after the other and overshoot before they settle, his tentacles erupt after them, his manifestation
 * blood and tear sound go with each, and the mask creeps over his face from the lens. When he leaves it (K again, putting the
 * kagune away, or running out of energy) every leg, tentacle and the mask break into pieces that fly off him (from what was drawn
 * the frame before), with a low glass break; the form he goes back to grows its tentacles out again. Nothing here is sent over
 * the network: every client sees the same state change and plays it itself.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class KakujaFx {
    private static final RenderType TYPE = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    /** 1.2.2: the user's own pack's shattering (Sfx). */
    private static final SoundEvent GLASS = SoundEvent.m_262824_(ResourceLocation.m_214293_("hexkagune", "sfx.shatter"));
    private static final int FULL_BRIGHT = 0xF000F0;
    /** A snapshot older than this (ticks) is not shattered (the part wasn't drawn lately). */
    private static final long FRESH = 6L;
    /** The most pieces' quads flying at once, all players together; the oldest pieces go first. */
    private static final int MAX_QUADS = 24000;
    private static final double SEEN_SQR = 64.0 * 64.0;
    private static final java.util.Random RANDOM = new java.util.Random();
    private static final Map<UUID, Track> TRACKS = new HashMap<>();
    private static final List<KakujaBurst.Shard> SHARDS = new ArrayList<>();
    private static KaguneClientState.View lastView;
    private static Track lastTrack;
    private static ClientLevel trackedLevel;

    /** What one client knows of one player's kakuja: whether he is in one, when it burst or broke, what was drawn last. */
    public static final class Track {
        KaguneClientState.View view;
        boolean seen;
        boolean alive;
        boolean kakuja;
        KaguneForm form;
        /** When the current kakuja burst out, or NaN. */
        double burstAt = Double.NaN;
        /** Whether this burst brought the legs out (not when the final kakuja grows out of the first). */
        boolean legsBurst;
        /** When the mask started to grow, or NaN (already on, or none). */
        double maskAt = Double.NaN;
        /** When the kakuja broke, or NaN. */
        double shatteredAt = Double.NaN;
        /** How many limbs' blood and tear have been played for the current burst. */
        int torn;
        final LegShot[] legs = new LegShot[6];
        final TubeShot[] tubes = new TubeShot[6];
        long legsDrawnAt = Long.MIN_VALUE;

        /** Age of the current burst at {@code time}, or NaN. */
        public double burstAge(double time) {
            return Double.isNaN(this.burstAt) ? Double.NaN : time - this.burstAt;
        }

        /** The time the current burst started (NaN for none): a renderer that sees it change starts its legs afresh. */
        public double burstAt() {
            return this.burstAt;
        }

        /** True while the legs of the current burst are still tearing out or settling. */
        public boolean legsBursting(double time, int legs) {
            return this.legsBurst && KakujaBurst.legsBursting(this.burstAge(time), legs);
        }

        /** True when the kakuja has broken but the server is still putting the kagune away: nothing of it may be drawn. */
        public boolean broken() {
            return !Double.isNaN(this.shatteredAt) && this.view != null && this.view.active() && KanekiForms.kakuja(this.view.form());
        }
    }

    /** Ayato's kakuja (5.0.5): when it burst out, when the final one's fire lit, and what was drawn last (rig quads and where). */
    private static final class AyatoTrack {
        boolean seen;
        boolean in;
        boolean fin;
        double burstAt = Double.NaN;
        double fireAt = Double.NaN;
        List<WingRig.Quad> quads;
        double x;
        double y;
        double z;
        double yaw;
        long drawnAt = Long.MIN_VALUE;
    }

    private static final Map<UUID, AyatoTrack> AYATO = new HashMap<>();
    /** Where his tentacles leave his back (rig space), what the burst grows out from. */
    private static final double[] AYATO_ROOT = {0.0, 1.3, -0.25};
    /** How far the burst's front has to run to reach every tip (blocks), and how wide its hot edge is. */
    private static final double AYATO_REACH = 7.0;
    private static final double AYATO_EDGE = 0.7;
    private static final double AYATO_STEEL_TICKS = 22.0;
    private static final double AYATO_FIRE_DELAY = 10.0;
    private static final double AYATO_FIRE_TICKS = 20.0;
    private static final int STEEL_HOT = 0xFFF2E6;
    private static final int FIRE_HOT = 0xFFF0A0;

    /** Ayato's kakuja coming out, lighting or going: watched like Kaneki's ({@link #observe}), from the synced form. */
    public static void observeAyato(AbstractClientPlayer player) {
        AyatoTrack track = AYATO.computeIfAbsent(player.m_36316_().getId(), id -> new AyatoTrack());
        KaguneClientState.View view = KaguneClientState.get(player);
        boolean in = view.active() && player.m_6084_() && AyatoClient.kakuja(player);
        boolean fin = in && AyatoClient.finalKakuja(player);
        if (!track.seen) {
            track.seen = true;
            track.in = in;
            track.fin = fin;
            return;
        }
        double now = player.m_9236_().m_46467_();
        if (in && !track.in) {
            track.burstAt = now;
            track.fireAt = fin ? now + AYATO_FIRE_DELAY : Double.NaN;
            AyatoClient.spawnManifestBlood(player, true);
        } else if (fin && !track.fin) {
            // The first kakuja's left side burns into the final one's fire.
            track.fireAt = now;
        } else if (!in && track.in) {
            shatterAyato(player, track, now);
        }
        track.in = in;
        track.fin = fin;
    }

    /** The kakuja's quads as drawn this frame (rig space, the feet at {@code x, y, z} in the world, turned by {@code yaw}). */
    public static void ayatoDrawn(AbstractClientPlayer player, List<WingRig.Quad> quads, double x, double y, double z, double yaw, long tick) {
        AyatoTrack track = AYATO.get(player.m_36316_().getId());
        if (track != null) {
            track.quads = quads;
            track.x = x;
            track.y = y;
            track.z = z;
            track.yaw = yaw;
            track.drawnAt = tick;
        }
    }

    /**
     * The kakuja bursting out: only what the front has reached from his back is drawn. The steel tentacles forge out white-hot
     * at their growing edge; the final kakuja's fire (his left) ignites a beat later, flaring yellow-white as it catches.
     */
    public static List<WingRig.Quad> ayatoBurst(AbstractClientPlayer player, List<WingRig.Quad> quads, double time) {
        AyatoTrack track = AYATO.get(player.m_36316_().getId());
        if (track == null) {
            return quads;
        }
        double steel = Double.isNaN(track.burstAt) ? Double.POSITIVE_INFINITY : front(time - track.burstAt, AYATO_STEEL_TICKS);
        double fire = Double.isNaN(track.fireAt) ? steel : front(time - track.fireAt, AYATO_FIRE_TICKS);
        if (steel >= AYATO_REACH + AYATO_EDGE && fire >= AYATO_REACH + AYATO_EDGE) {
            return quads;
        }
        boolean fin = track.fin;
        List<WingRig.Quad> out = new ArrayList<>(quads.size());
        for (WingRig.Quad quad : quads) {
            float[] xyz = quad.xyz();
            double cx = (xyz[0] + xyz[3] + xyz[6] + xyz[9]) * 0.25;
            double cy = (xyz[1] + xyz[4] + xyz[7] + xyz[10]) * 0.25;
            double cz = (xyz[2] + xyz[5] + xyz[8] + xyz[11]) * 0.25;
            boolean burning = fin && cx < 0.0;
            double reach = burning ? fire : steel;
            double d = Math.sqrt((cx - AYATO_ROOT[0]) * (cx - AYATO_ROOT[0]) + (cy - AYATO_ROOT[1]) * (cy - AYATO_ROOT[1])
                + (cz - AYATO_ROOT[2]) * (cz - AYATO_ROOT[2]));
            double grown = Math.max(0.0, Math.min(1.0, (reach - d) / AYATO_EDGE));
            if (grown <= 0.0) {
                continue;
            }
            if (grown >= 1.0) {
                out.add(quad);
                continue;
            }
            float k = (float)(0.3 + 0.7 * grown);
            float[] moved = new float[12];
            for (int c = 0; c < 4; c++) {
                moved[c * 3] = (float)(cx + (xyz[c * 3] - cx) * k);
                moved[c * 3 + 1] = (float)(cy + (xyz[c * 3 + 1] - cy) * k);
                moved[c * 3 + 2] = (float)(cz + (xyz[c * 3 + 2] - cz) * k);
            }
            int hot = burning ? FIRE_HOT : STEEL_HOT;
            int[] argb = quad.argb();
            int[] tinted = new int[4];
            for (int c = 0; c < 4; c++) {
                double t = 1.0 - grown;
                int a = burning ? Math.max(argb[c] >>> 24, 0xC0) : argb[c] >>> 24;
                tinted[c] = a << 24 | mix(argb[c] & 0xFFFFFF, hot, t);
            }
            out.add(new WingRig.Quad(moved, quad.nx(), quad.ny(), quad.nz(), tinted));
        }
        return out;
    }

    private static double front(double age, double ticks) {
        double t = Math.max(0.0, Math.min(1.0, age / ticks));
        return (AYATO_REACH + AYATO_EDGE) * (1.0 - (1.0 - t) * (1.0 - t) * (1.0 - t));
    }

    private static int mix(int rgb, int hot, double t) {
        int r = (int)Math.round(((rgb >> 16) & 0xFF) + (((hot >> 16) & 0xFF) - ((rgb >> 16) & 0xFF)) * t);
        int g = (int)Math.round(((rgb >> 8) & 0xFF) + (((hot >> 8) & 0xFF) - ((rgb >> 8) & 0xFF)) * t);
        int b = (int)Math.round((rgb & 0xFF) + ((hot & 0xFF) - (rgb & 0xFF)) * t);
        return r << 16 | g << 8 | b;
    }

    /** Ayato's kakuja breaking the way Kaneki's does: every tentacle, steel and fire, into flying pieces, with the glass. */
    private static void shatterAyato(AbstractClientPlayer player, AyatoTrack track, double now) {
        track.burstAt = Double.NaN;
        track.fireAt = Double.NaN;
        List<WingRig.Quad> drawn = track.quads;
        track.quads = null;
        Minecraft minecraft = Minecraft.m_91087_();
        if (drawn == null || (long)now - track.drawnAt > FRESH || player.m_20280_(minecraft.f_91074_) > SEEN_SQR) {
            return;
        }
        double forwardX = -Math.sin(track.yaw);
        double forwardZ = Math.cos(track.yaw);
        double rightX = -Math.cos(track.yaw);
        double rightZ = -Math.sin(track.yaw);
        List<KakujaBurst.Quad> quads = new ArrayList<>(drawn.size());
        for (WingRig.Quad quad : drawn) {
            float[] xyz = quad.xyz();
            double[][] world = new double[4][];
            for (int c = 0; c < 4; c++) {
                double lx = xyz[c * 3];
                double lz = xyz[c * 3 + 2];
                world[c] = new double[] {track.x + rightX * lx + forwardX * lz, track.y + xyz[c * 3 + 1], track.z + rightZ * lx + forwardZ * lz};
            }
            double[] n = {rightX * quad.nx() + forwardX * quad.nz(), quad.ny(), rightZ * quad.nx() + forwardZ * quad.nz()};
            int[] c = quad.argb();
            quads.add(new KakujaBurst.Quad(world[0], world[1], world[2], world[3], n, c[0], c[1], c[2], c[3], true));
        }
        double feetY = player.m_20186_();
        double[] body = {player.m_20185_(), feetY + player.m_20206_() * 0.55, player.m_20189_()};
        double floor = player.m_20096_() ? feetY + 0.03 : feetY - 2.5;
        add(KakujaBurst.shatter(quads, 0.5, body, floor, 1.0, player.m_19879_() * 0x9E3779B97F4A7C15L + (long)now));
        glass(player);
    }

    private record LegShot(double[][] world, double[] dorsal, int side, CentipedeTail.Look look, long tick) {
    }

    private record TubeShot(Vec3[] points, double radius, int colour, int vein, long tick) {
    }

    private KakujaFx() {
    }

    // -------------------------------------------------------------------------------------------------------- the watching

    /**
     * Looks at {@code player}'s synced state and plays what changed: a burst into a kakuja, the final kakuja growing out of the
     * first, or the kakuja breaking. Cheap when nothing changed. Called by {@code AyatoClient.tendrilView} (the original renderer
     * asks for every player every frame) and by our renderers.
     */
    public static Track observe(Player player) {
        KaguneClientState.View view = KaguneClientState.get(player);
        Track track = TRACKS.computeIfAbsent(player.m_36316_().getId(), id -> new Track());
        lastView = view;
        lastTrack = track;
        boolean alive = player.m_6084_();
        if (view == track.view && alive == track.alive) {
            return track;
        }
        track.view = view;
        track.alive = alive;
        if (AyatoClient.isAyato(player)) {
            track.seen = false;
            return track;
        }
        boolean in = view.active() && alive && KanekiForms.kakuja(view.form()) && view.mode() != TendrilMode.DISMISS;
        KaguneForm before = track.form;
        track.form = view.form();
        if (!track.seen) {
            // First sight of this player (joined, came into view): whatever he is in is already out.
            track.seen = true;
            track.kakuja = in;
            return track;
        }
        double now = player.m_9236_().m_46467_();
        if (in && !track.kakuja) {
            burst(track, view, now, true, true);
            if (view.mode() != TendrilMode.MANIFEST && player instanceof AbstractClientPlayer client) {
                BodyAnimations.playManifestPain(client, (int)KakujaBurst.burstEnd(2), view.form().limbCount());
            }
        } else if (in && before != view.form()) {
            // The final kakuja out of the first: the legs stay, the tentacles burst out.
            burst(track, view, now, before == null || !before.hasCentipedeLegs(), before == null || !before.hasKanekiMask());
        } else if (!in && track.kakuja && player instanceof AbstractClientPlayer client) {
            shatter(client, track, before, now);
        }
        track.kakuja = in;
        return track;
    }

    private static void burst(Track track, KaguneClientState.View view, double now, boolean legs, boolean mask) {
        // Straight out in a kakuja (the kagune coming out): the burst runs on the manifestation's own clock.
        track.burstAt = view.mode() == TendrilMode.MANIFEST ? Math.min(now, view.modeStarted()) : now;
        track.legsBurst = legs;
        track.maskAt = mask && view.form().hasKanekiMask() ? track.burstAt : Double.NaN;
        track.shatteredAt = Double.NaN;
        track.torn = 0;
    }

    /** The limb deployment the original renderer asks for (trampoline target of {@code KaguneWorldRenderer.limbDeployment}). */
    public static float limbDeployment(KaguneClientState.View view, int limb, double frame) {
        float legacy = KaguneWorldRenderer.limbDeploymentLegacy(view, limb, frame);
        Track track = view == lastView ? lastTrack : null;
        if (track == null || !track.seen) {
            return legacy;
        }
        KaguneForm form = view.form();
        if (KanekiForms.kakuja(form)) {
            if (track.broken()) {
                return 0.0F;
            }
            double age = track.burstAge(frame);
            // A manifestation straight into a kakuja runs on the burst's timing all the way (the original's is slower).
            if (Double.isNaN(age) || view.mode() == TendrilMode.DISMISS
                || age > KakujaBurst.burstEnd(2) + 12.0 && view.mode() != TendrilMode.MANIFEST) {
                return legacy;
            }
            if (form.limb(limb).segmented()) {
                // The legs come out on their own timing (CentipedeRenderer).
                return track.legsBurst ? 1.0F : legacy;
            }
            return KakujaBurst.tentacleOut(age, tentacleOrder(form, limb));
        }
        if (!Double.isNaN(track.shatteredAt) && view.active() && view.mode() != TendrilMode.MANIFEST && view.mode() != TendrilMode.DISMISS) {
            double age = frame - track.shatteredAt;
            if (!KakujaBurst.regrown(age, form.limbCount())) {
                return Math.min(legacy, KakujaBurst.regrowOut(age, limb));
            }
        }
        return legacy;
    }

    /** Which of the form's tentacles (not counting legs) {@code limb} is. */
    static int tentacleOrder(KaguneForm form, int limb) {
        int order = 0;
        for (int i = 0; i < limb && i < form.limbCount(); i++) {
            if (!form.limb(i).segmented()) {
                order++;
            }
        }
        return order;
    }

    /** Which of the form's legs {@code limb} is. */
    public static int legOrder(KaguneForm form, int limb) {
        int order = 0;
        for (int i = 0; i < limb && i < form.limbCount(); i++) {
            if (form.limb(i).segmented()) {
                order++;
            }
        }
        return order;
    }

    /**
     * How long the mask has been growing at {@code time} (ticks; infinite once it is on), or NaN while the kakuja is broken and
     * the mask must not be drawn.
     */
    public static double maskAge(Player player, double time) {
        Track track = observe(player);
        if (track.broken()) {
            return Double.NaN;
        }
        return Double.isNaN(track.maskAt) ? Double.POSITIVE_INFINITY : time - track.maskAt;
    }

    // ------------------------------------------------------------------------------------------------- what was drawn last

    /** A centipede leg was drawn this frame: kept (in the world) in case it breaks. */
    public static void leg(Track track, int limb, double[][] world, double[] dorsal, int side, CentipedeTail.Look look, long tick) {
        if (limb >= 0 && limb < track.legs.length) {
            track.legs[limb] = new LegShot(world, dorsal, side, look, tick);
            track.legsDrawnAt = tick;
        }
    }

    /** One of the original's tubes was drawn ({@code CentipedeRenderer.emitTube}): kept if it is a kakuja's tentacle. */
    public static void tube(KaguneClientState.View view, int limb, Vec3[] points, long tick) {
        Track track = view == lastView ? lastTrack : null;
        if (track == null || limb < 0 || limb >= track.tubes.length || !KanekiForms.kakuja(view.form()) || view.form().limb(limb).segmented()) {
            return;
        }
        KaguneForm.LimbLayout layout = view.form().limb(limb);
        track.tubes[limb] = new TubeShot(points, view.limbThickness(layout), view.color(), view.veinColor(), tick);
    }

    // ------------------------------------------------------------------------------------------------------- the breaking

    private static void shatter(AbstractClientPlayer player, Track track, KaguneForm was, double now) {
        track.shatteredAt = now;
        track.burstAt = Double.NaN;
        track.maskAt = Double.NaN;
        Minecraft minecraft = Minecraft.m_91087_();
        long tick = (long)now;
        double feetY = player.m_20186_();
        double[] body = {player.m_20185_(), feetY + player.m_20206_() * 0.55, player.m_20189_()};
        double floor = player.m_20096_() ? feetY + 0.03 : feetY - 2.5;
        long seed = player.m_19879_() * 0x9E3779B97F4A7C15L + tick;
        boolean near = player.m_20280_(minecraft.f_91074_) <= SEEN_SQR;
        if (near) {
            List<KakujaBurst.Quad> legs = new ArrayList<>();
            List<KakujaBurst.Quad> tubes = new ArrayList<>();
            List<KakujaBurst.Quad> mask = new ArrayList<>();
            for (LegShot shot : track.legs) {
                if (shot != null && tick - shot.tick() <= FRESH) {
                    legQuads(shot, legs);
                }
            }
            for (TubeShot shot : track.tubes) {
                if (shot != null && tick - shot.tick() <= FRESH) {
                    tubeQuads(shot, tubes);
                }
            }
            boolean firstPerson = player == minecraft.f_91074_ && minecraft.f_91066_.m_92176_().m_90612_();
            if (!firstPerson && was != null && was.hasKanekiMask() && tick - track.legsDrawnAt <= FRESH) {
                maskQuads(player, mask);
            }
            add(KakujaBurst.shatter(legs, 0.55, body, floor, 1.0, seed));
            add(KakujaBurst.shatter(tubes, 0.45, body, floor, 1.1, seed + 1L));
            add(KakujaBurst.shatter(mask, 0.085, body, floor, 0.55, seed + 2L));
            glass(player);
        }
        java.util.Arrays.fill(track.legs, null);
        java.util.Arrays.fill(track.tubes, null);
    }

    private static void add(List<KakujaBurst.Shard> shards) {
        SHARDS.addAll(shards);
        int quads = 0;
        for (KakujaBurst.Shard shard : SHARDS) {
            quads += shard.quads();
        }
        Iterator<KakujaBurst.Shard> oldest = SHARDS.iterator();
        while (quads > MAX_QUADS && oldest.hasNext()) {
            quads -= oldest.next().quads();
            oldest.remove();
        }
    }

    /** The leg as it was last drawn, at lower detail: the plates, flesh and claws that break off. */
    private static void legQuads(LegShot shot, List<KakujaBurst.Quad> out) {
        CentipedeTail.Look look = shot.look();
        CentipedeTail.Look coarse = new CentipedeTail.Look(look.width(), look.plates(), false, 1.0, look.seed(), look.jaw(), look.flare(),
            look.ripple(), look.phase());
        CentipedeTail.tail((a, b, c, d, n, ca, cb, cc, cd) -> out.add(new KakujaBurst.Quad(a, b, c, d, n, ca, cb, cc, cd,
            n[0] == 0.0 && n[1] == 1.0 && n[2] == 0.0)), CentipedeTail.through(shot.world(), 40, shot.side(), shot.dorsal()), coarse);
    }

    /** A rinkaku tentacle as it was last drawn: a six-sided tube in its colour, tapering to the tip, its veins darker. */
    private static void tubeQuads(TubeShot shot, List<KakujaBurst.Quad> out) {
        Vec3[] points = shot.points();
        int count = points.length;
        if (count < 2) {
            return;
        }
        int sides = 6;
        double[][][] rings = new double[count][sides][];
        double[][][] radial = new double[count][sides][];
        double[] previousNormal = null;
        for (int i = 0; i < count; i++) {
            Vec3 before = points[Math.max(0, i - 1)];
            Vec3 after = points[Math.min(count - 1, i + 1)];
            double[] tangent = unit(after.f_82479_ - before.f_82479_, after.f_82480_ - before.f_82480_, after.f_82481_ - before.f_82481_);
            double[] carried = previousNormal == null ? null : reject(previousNormal, tangent);
            double[] normal = carried == null ? perpendicular(tangent) : unit(carried[0], carried[1], carried[2]);
            double[] binormal = cross(tangent, normal);
            previousNormal = normal;
            double along = i / (double)(count - 1);
            double radius = Math.max(0.02, shot.radius() * (1.0 - 0.66 * Math.pow(along, 1.6)) * (0.55 + 0.45 * Math.min(1.0, along / 0.15)));
            for (int s = 0; s < sides; s++) {
                double angle = Math.PI * 2.0 * s / sides;
                double[] r = {normal[0] * Math.cos(angle) + binormal[0] * Math.sin(angle), normal[1] * Math.cos(angle) + binormal[1] * Math.sin(angle),
                    normal[2] * Math.cos(angle) + binormal[2] * Math.sin(angle)};
                radial[i][s] = r;
                rings[i][s] = new double[] {points[i].f_82479_ + r[0] * radius, points[i].f_82480_ + r[1] * radius, points[i].f_82481_ + r[2] * radius};
            }
        }
        for (int i = 0; i < count - 1; i++) {
            for (int s = 0; s < sides; s++) {
                int next = (s + 1) % sides;
                double[] n = unit(radial[i][s][0] + radial[i][next][0], radial[i][s][1] + radial[i][next][1], radial[i][s][2] + radial[i][next][2]);
                double shade = 0.62 + 0.38 * Math.max(0.0, n[1] * 0.5 + 0.5);
                int base = (i + s) % 5 == 0 ? shot.vein() : shot.colour();
                int colour = 0xFF000000 | scale(base, shade);
                out.add(new KakujaBurst.Quad(rings[i][s], rings[i + 1][s], rings[i + 1][next], rings[i][next], n, colour, colour, colour, colour, false));
            }
        }
    }

    /** The mask on his face as it is now: its quads in the world (the head model's transform, without his animation). */
    private static void maskQuads(AbstractClientPlayer player, List<KakujaBurst.Quad> out) {
        double bodyYaw = player.f_20883_;
        double headYaw = wrap(player.f_20885_ - bodyYaw);
        double pitch = player.m_146909_();
        boolean crouching = player.m_6047_();
        double x = player.m_20185_();
        double y = player.m_20186_();
        double z = player.m_20189_();
        CentipedeMask.build((a, b, c, d, n, ca, cb, cc, cd) -> {
            double[][] corners = {a, b, c, d};
            double[][] world = new double[4][];
            for (int k = 0; k < 4; k++) {
                double[] offset = KakujaBurst.head(corners[k], bodyYaw, headYaw, pitch, crouching);
                world[k] = new double[] {x + offset[0], y + offset[1], z + offset[2]};
            }
            double[] o = KakujaBurst.head(new double[] {0.0, 0.0, 0.0}, bodyYaw, headYaw, pitch, crouching);
            double[] tip = KakujaBurst.head(n, bodyYaw, headYaw, pitch, crouching);
            double[] normal = unit(tip[0] - o[0], tip[1] - o[1], tip[2] - o[2]);
            out.add(new KakujaBurst.Quad(world[0], world[1], world[2], world[3], normal, ca, cb, cc, cd, false));
        }, KakuganClient.eyes(player));
    }

    /** The kakuja breaking: a low glass break, twice over for weight. */
    private static void glass(AbstractClientPlayer player) {
        double y = player.m_20186_() + player.m_20206_() * 0.6;
        Minecraft.m_91087_().m_91106_().m_120367_(new Shatter(player.m_20185_(), y, player.m_20189_(), 1.4F, 0.5F));
        Minecraft.m_91087_().m_91106_().m_120367_(new Shatter(player.m_20185_(), y, player.m_20189_(), 1.0F, 0.62F));
    }

    /** A one-shot glass break where the kakuja broke. */
    private static final class Shatter extends AbstractTickableSoundInstance {
        private Shatter(double x, double y, double z, float volume, float pitch) {
            super(GLASS, SoundSource.PLAYERS, SoundInstance.m_235150_());
            this.f_119578_ = false;
            this.f_119579_ = 0;
            this.f_119573_ = volume;
            this.f_119574_ = pitch;
            this.f_119580_ = SoundInstance.Attenuation.LINEAR;
            this.f_119575_ = x;
            this.f_119576_ = y;
            this.f_119577_ = z;
        }

        @Override
        public void m_7788_() {
        }
    }

    // ------------------------------------------------------------------------------------------------------ tick and draw

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level != trackedLevel) {
            trackedLevel = level;
            TRACKS.clear();
            AYATO.clear();
            SHARDS.clear();
            lastView = null;
            lastTrack = null;
        }
        if (level == null || minecraft.m_91104_()) {
            return;
        }
        double now = level.m_46467_();
        List<UUID> present = new ArrayList<>();
        for (AbstractClientPlayer player : level.m_6907_()) {
            Track track = observe(player);
            tear(player, track, now);
            if (AyatoClient.isAyato(player)) {
                observeAyato(player);
                AyatoTrack ayato = AYATO.get(player.m_36316_().getId());
                if (ayato != null) {
                    ayatoTick(player, ayato, now);
                }
            } else {
                AYATO.remove(player.m_36316_().getId());
            }
            present.add(player.m_36316_().getId());
        }
        // A player out of sight is forgotten at once: when he is seen again, whatever he is in is already out.
        TRACKS.keySet().retainAll(present);
        AYATO.keySet().retainAll(present);
        if (lastTrack != null && !TRACKS.containsValue(lastTrack)) {
            lastView = null;
            lastTrack = null;
        }
        for (Iterator<KakujaBurst.Shard> iterator = SHARDS.iterator(); iterator.hasNext(); ) {
            KakujaBurst.Shard shard = iterator.next();
            shard.step();
            if (shard.dead()) {
                iterator.remove();
            }
        }
    }

    /** A burst's blood and tear sound, one for each limb as it comes out (the original's manifestation runs its own). */
    private static void tear(AbstractClientPlayer player, Track track, double now) {
        if (Double.isNaN(track.burstAt) || track.form == null || !track.kakuja || track.view == null) {
            return;
        }
        boolean manifesting = track.view.mode() == TendrilMode.MANIFEST;
        KaguneForm form = track.form;
        double age = now - track.burstAt;
        int due = 0;
        for (int limb = 0; limb < form.limbCount(); limb++) {
            boolean out = form.limb(limb).segmented()
                ? track.legsBurst && KakujaBurst.legOut(age, legOrder(form, limb)) > 0.0
                : age >= KakujaBurst.tentacleStart(tentacleOrder(form, limb));
            if (out) {
                due++;
            }
        }
        while (track.torn < due && track.torn < 6) {
            if (!manifesting) {
                // A manifestation plays its own blood and tear per limb; a K transformation has only these.
                AyatoClient.spawnManifestBlood(player, track.torn == 0);
            }
            gush(player, track.torn % 2 == 0 ? 1 : -1, track.torn == 0 ? 2.2F : 1.4F);
            track.torn++;
        }
    }

    /** A rig point of {@code player} (x right, y up from the feet, z forward) in the world, and a rig direction. */
    private static Vec3[] onBody(AbstractClientPlayer player, double x, double y, double z, double dx, double dy, double dz) {
        double yaw = Math.toRadians(player.f_20883_);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        return new Vec3[] {new Vec3(player.m_20185_() + rightX * x + forwardX * z, player.m_20186_() + y, player.m_20189_() + rightZ * x + forwardZ * z),
            new Vec3(rightX * dx + forwardX * dz, dy, rightZ * dx + forwardZ * dz)};
    }

    /** A limb tearing out of Kaneki's back on {@code side}: blood gushing back and up out of the wound, scraps of flesh. */
    private static void gush(AbstractClientPlayer player, int side, float power) {
        Vec3[] root = onBody(player, 0.14 * side, 1.1, -0.22, 0.45 * side, 0.55, -1.0);
        if (!RcFx.near(root[0], 48.0) || !(player.m_9236_() instanceof ClientLevel level)) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            Vec3 way = root[1].m_82520_((RANDOM.nextFloat() - 0.5) * 0.6, (RANDOM.nextFloat() - 0.2) * 0.5,
                (RANDOM.nextFloat() - 0.5) * 0.6);
            AyatoBlood.spurt(level, root[0], way, power, player);
        }
        for (int i = 0; i < RcFx.count(14.0F); i++) {
            Vec3 velocity = root[1].m_82490_(0.12 + 0.18 * RANDOM.nextFloat()).m_82520_((RANDOM.nextFloat() - 0.5) * 0.16,
                0.06 * RANDOM.nextFloat(), (RANDOM.nextFloat() - 0.5) * 0.16);
            RcFx.at(RcFx.MOTE, false, 14 + RANDOM.nextInt(10), root[0]).velocity(velocity).drag(0.9F).fall(0.03F)
                .colors(RcFx.CRIMSON, RcFx.clear(0xFF3A0410)).size(0.16F, 0.08F).spawn();
        }
    }

    /**
     * Ayato's kakuja coming out, every tick of it: steel sparks spraying from where his armored tentacles force out of his back,
     * and on the final kakuja fire bursting into flame where his left side ignites, licking out along it.
     */
    private static void ayatoTick(AbstractClientPlayer player, AyatoTrack track, double now) {
        if (!track.in || !RcFx.near(player.m_20182_(), 48.0) || !(player.m_9236_() instanceof ClientLevel level)) {
            return;
        }
        double steel = now - track.burstAt;
        if (!Double.isNaN(track.burstAt) && steel >= 0.0 && steel < AYATO_STEEL_TICKS) {
            for (int side = track.fin ? 1 : -1; side <= 1; side += 2) {
                Vec3[] root = onBody(player, 0.21 * side, 1.24, -0.2, 0.7 * side, 0.45, -0.8);
                for (int i = 0; i < RcFx.count(5.0F); i++) {
                    Vec3 velocity = root[1].m_82490_(0.25 + 0.35 * RANDOM.nextFloat()).m_82520_((RANDOM.nextFloat() - 0.5) * 0.3,
                        (RANDOM.nextFloat() - 0.3) * 0.3, (RANDOM.nextFloat() - 0.5) * 0.3);
                    RcFx.at(RcFx.STREAK, true, 6 + RANDOM.nextInt(6), root[0]).velocity(velocity).drag(0.86F).fall(0.02F)
                        .colors(0xFFF4F8FF, RcFx.clear(0xFFB8C4D8)).size(0.1F, 0.03F).spawn();
                }
                if (steel < 1.0) {
                    RcFx.at(RcFx.RING, true, 8, root[0]).colors(0xFFFFFFFF, RcFx.clear(RcFx.RED)).size(0.2F, 1.6F).spawn();
                }
            }
        }
        double fire = now - track.fireAt;
        if (!Double.isNaN(track.fireAt) && fire >= 0.0 && fire < AYATO_FIRE_TICKS) {
            Vec3[] root = onBody(player, -0.21, 1.24, -0.2, -0.75, 0.5, -0.7);
            double reach = 0.4 + 3.0 * (fire / AYATO_FIRE_TICKS);
            for (int i = 0; i < RcFx.count(9.0F); i++) {
                double along = reach * RANDOM.nextFloat();
                Vec3 at = root[0].m_82549_(root[1].m_82541_().m_82490_(along)).m_82520_((RANDOM.nextFloat() - 0.5) * 0.5,
                    (RANDOM.nextFloat() - 0.5) * 0.5, (RANDOM.nextFloat() - 0.5) * 0.5);
                Vec3 velocity = root[1].m_82490_(0.06).m_82520_(0.0, 0.04 + 0.05 * RANDOM.nextFloat(), 0.0);
                RcFx.at(RcFx.FLAME, true, 10 + RANDOM.nextInt(8), at).velocity(velocity).drag(0.88F).fall(-0.004F).wander(0.01F)
                    .colors(RANDOM.nextFloat() < 0.4F ? RcFx.CORE : RcFx.MOLTEN, RcFx.clear(RcFx.FIRE)).size(0.35F, 0.12F).spawn();
            }
            if (fire < 1.0) {
                RcFx.at(RcFx.RING, true, 10, root[0]).colors(RcFx.CORE, RcFx.clear(RcFx.FIRE)).size(0.3F, 2.4F).spawn();
            }
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || SHARDS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null) {
            return;
        }
        float partial = event.getPartialTick();
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        PoseStack stack = event.getPoseStack();
        Matrix4f matrix = stack.m_85850_().m_252922_();
        Matrix3f normals = stack.m_85850_().m_252943_();
        MultiBufferSource.BufferSource buffers = minecraft.m_91269_().m_110104_();
        VertexConsumer consumer = buffers.m_6299_(TYPE);
        for (KakujaBurst.Shard shard : SHARDS) {
            double fade = shard.fade(partial);
            if (fade <= 0.01) {
                continue;
            }
            double[] at = shard.position(partial);
            double[] rotation = shard.rotation(partial);
            double scale = 0.35 + 0.65 * fade;
            int light = LevelRenderer.m_109541_(level, BlockPos.m_274561_(at[0], at[1] + 0.1, at[2]));
            double cx = at[0] - camera.f_82479_;
            double cy = at[1] - camera.f_82480_;
            double cz = at[2] - camera.f_82481_;
            for (int q = 0; q < shard.quads(); q++) {
                double[] n = shard.normal(q, rotation);
                int lit = shard.glow(q) ? FULL_BRIGHT : light;
                for (int corner = 0; corner < 4; corner++) {
                    double[] p = shard.corner(q, corner, rotation, scale);
                    int colour = shard.colour(q, corner);
                    int alpha = (int)Math.round((colour >>> 24) * fade);
                    consumer.m_252986_(matrix, (float)(cx + p[0]), (float)(cy + p[1]), (float)(cz + p[2]))
                        .m_6122_((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, alpha)
                        .m_7421_(0.5F, 0.5F)
                        .m_86008_(OverlayTexture.f_118083_)
                        .m_85969_(lit)
                        .m_252939_(normals, (float)n[0], (float)n[1], (float)n[2])
                        .m_5752_();
                }
            }
        }
        buffers.m_109912_(TYPE);
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        TRACKS.clear();
        AYATO.clear();
        SHARDS.clear();
        lastView = null;
        lastTrack = null;
    }

    // ------------------------------------------------------------------------------------------------------------- vectors

    private static int scale(int rgb, double factor) {
        int r = (int)Math.min(255, Math.round(((rgb >> 16) & 0xFF) * factor));
        int g = (int)Math.min(255, Math.round(((rgb >> 8) & 0xFF) * factor));
        int b = (int)Math.min(255, Math.round((rgb & 0xFF) * factor));
        return r << 16 | g << 8 | b;
    }

    private static double[] unit(double x, double y, double z) {
        double length = Math.sqrt(x * x + y * y + z * z);
        return length < 1.0E-9 ? new double[] {0.0, 1.0, 0.0} : new double[] {x / length, y / length, z / length};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] reject(double[] v, double[] axis) {
        double dot = v[0] * axis[0] + v[1] * axis[1] + v[2] * axis[2];
        double[] r = {v[0] - axis[0] * dot, v[1] - axis[1] * dot, v[2] - axis[2] * dot};
        double length = Math.sqrt(r[0] * r[0] + r[1] * r[1] + r[2] * r[2]);
        return length < 1.0E-6 ? perpendicular(axis) : r;
    }

    private static double[] perpendicular(double[] t) {
        double[] up = Math.abs(t[1]) < 0.9 ? new double[] {0.0, 1.0, 0.0} : new double[] {1.0, 0.0, 0.0};
        return unit(up[0] - t[0] * (up[0] * t[0] + up[1] * t[1] + up[2] * t[2]), up[1] - t[1] * (up[0] * t[0] + up[1] * t[1] + up[2] * t[2]),
            up[2] - t[2] * (up[0] * t[0] + up[1] * t[1] + up[2] * t[2]));
    }

    private static double wrap(double degrees) {
        return ((degrees % 360.0) + 540.0) % 360.0 - 180.0;
    }
}
