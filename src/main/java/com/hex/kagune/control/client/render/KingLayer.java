package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.client.ControlClient;
import com.hex.kagune.control.client.KingClient;
import com.hex.kagune.control.king.KingForm;
import com.hex.kagune.control.king.KingRules;
import com.hex.kagune.control.king.KingShed;
import com.hex.kagune.control.king.KingTransform;
import com.hex.kagune.control.king.WarCloth;
import com.hex.kagune.control.king.WarModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 1.0.5: the final kakuja on its wearer ({@link WarModel}): every part drawn on the body part it rides, so it goes with every pose.
 * By night War goes dark: the wearer's own skin drawn again over him in black (the deeper the night, the darker), the kakuja's
 * plates dimmed with it, and its one eye left burning, a white point with a bloom that grows with the distance to the camera so
 * it still shows far off in the dark.
 *
 * <p>Built once a game tick per player and part (the shapes are kept as quads and drawn again for the frames between): the kakuja
 * is many thousands of quads, and moves slowly enough that twenty builds a second read as smooth.
 */
public final class KingLayer extends RenderLayer<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> {
    private static final RenderType SHAPES = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    /** What burns: added onto what is behind it at full brightness. */
    private static final RenderType LIGHT = RenderType.m_110488_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    private static final int FULL_BRIGHT = 0xF000F0;
    /** How dark the night makes him: the black drawn over his skin at the dead of night, and what is left of the plates' light. */
    private static final double NIGHT_SHADE = 0.86;
    private static final double NIGHT_PLATES = 0.32;
    /** How much of the red glow the dead of night takes away (the eye's white keeps all of its own). */
    static final double NIGHT_RED = 0.72;
    /** The eye's bloom: from this far (blocks) it grows with the distance, up to {@link #BLOOM_MAX} times. */
    private static final double BLOOM_NEAR = 5.0;
    private static final double BLOOM_MAX = 18.0;

    /** One player's kakuja as last built: the tick, how grown and how deep the night, and each part's quads. */
    private static final class Built {
        KingForm form = KingForm.NONE;
        long tick = Long.MIN_VALUE;
        int growStep = -1;
        boolean clothed;
        int nightStep = -1;
        int turnStep = -2;
        final List<float[]>[] parts = newParts();
    }

    @SuppressWarnings("unchecked")
    private static List<float[]>[] newParts() {
        List<float[]>[] parts = new List[WarModel.Part.values().length];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = new ArrayList<>();
        }
        return parts;
    }

    private static final Map<AbstractClientPlayer, Built> BUILT = new WeakHashMap<>();
    /**
     * 1.0.28: War built at two ticks, the one now and the next, and every frame drawn between the two, corner by corner (as Peace's
     * kagune is, {@link Living}): his rinkaku writhe every frame, not in steps twenty times a second. Each tick only the next one is
     * built (the one before is the last tick's next), so it costs what building once a tick did.
     */
    private static final class Pair {
        Built was = new Built();
        Built next = new Built();
    }

    private static final Map<AbstractClientPlayer, Pair> PAIRS = new WeakHashMap<>();
    private static final WarModel.Part[] PARTS = WarModel.Part.values();
    /**
     * 1.0.18: one player's Peace kagune (its wings, arms and blades), alive, apart from its armour: built for one moment and for the
     * moment {@code step} ticks on, and every frame drawn between the two, corner by corner (as smooth as building it every frame, at a
     * fraction of the cost). Built again from scratch whenever what it is doing changes.
     */
    private static final class Living {
        long from = Long.MIN_VALUE;
        int step;
        com.hex.kagune.control.king.KingMove move;
        long moveAt;
        int level;
        boolean on;
        long since;
        Built was = new Built();
        Built next = new Built();
    }

    private static final Map<AbstractClientPlayer, Living> LIVING = new WeakHashMap<>();
    /** 1.0.18: past this far from the camera (blocks) Peace's kagune is built every four ticks (still drawn between), not every tick. */
    private static final double LIVING_NEAR = 28.0;

    public KingLayer(RenderLayerParent<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void m_6494_(PoseStack pose, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                        float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        PlayerLayers.ran(player);
        try {
            this.draw(pose, buffers, packedLight, player, partialTick);
        } catch (RuntimeException failure) {
            PlayerLayers.failed("final kakuja", failure);
        }
    }

    private void draw(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float partial) {
        if (player.m_20145_() || !(this.m_117386_() instanceof HumanoidModel<?> model)) {
            return;
        }
        KingClient.View view = KingClient.get(player);
        double time = ControlClient.worldTime(partial);
        // 1.0.11: or turning into the Dragon (TurningModel), before the final kakuja ever was.
        double turning = com.hex.kagune.control.client.TurningClient.progress(player, time);
        if (turning < 0.0 && (view == null || view.form() == KingForm.NONE)) {
            return;
        }
        // 1.0.8: Peace too (its look; the night and the eye are War's).
        boolean war = turning < 0.0 && view.form() == KingForm.WAR;
        double grow = turning >= 0.0 ? 1.0 : view.grow(time);
        if (grow <= 0.003) {
            return;
        }
        double night = !war || player.m_9236_() == null ? 0.0 : KingRules.night(player.m_9236_().m_46468_());
        // 1.0.19: War leaving him: what he wore the moment he took it off, crumbling away piece by piece (KingShed).
        double gone = turning < 0.0 && KingTransform.going(view.form(), view.on(), time - view.since()) ? time - view.since() : -1.0;
        boolean shedding = war && gone >= 0.0;
        // The night on his skin: the body drawn again over itself in black (1.0.19: lifting off him as War leaves).
        double shade = shedding ? 1.0 - Math.min(1.0, gone / (KingTransform.WAR_LEAVE * 0.8)) : 1.0;
        if (night > 0.01 && shade > 0.01) {
            VertexConsumer skin = buffers.m_6299_(RenderType.m_110473_(player.m_108560_()));
            this.m_117386_().m_7695_(pose, skin, light, OverlayTexture.f_118083_, 0.0F, 0.0F, 0.0F, (float)(NIGHT_SHADE * night * grow * shade));
        }
        WarModel.Motion motion = war && !shedding ? warMotion(player, view, time) : WarModel.Motion.STILL;
        // 1.0.11: the one eye right on the kakugan, where the player placed it.
        double[] eye = WarModel.eyeOn(com.hex.kagune.control.client.KakuganClient.eyes(player));
        // 1.0.16: Peace's blades as they move (PeaceMotion). 1.0.18: Peace's kagune is alive, so it is built apart from its armour and
        // again every frame (near the camera; ten times a second farther off); its armour once a tick.
        com.hex.kagune.control.king.PeaceMotion.State peace = com.hex.kagune.control.king.PeaceMotion.State.REST;
        long key = (long)Math.floor(time);
        double at = key;
        double[] growth = turning < 0.0 ? KingTransform.growth(view.form(), view.on(), time - view.since()) : null;
        boolean peaceShown = turning < 0.0 && view.form() == KingForm.PEACE;
        if (peaceShown) {
            peace = peaceState(player, partial, time);
        }
        if (growth != null && peaceShown) {
            // Peace's armour swelling out of him: every frame while it does.
            key = (long)Math.floor(time * 60.0) | Long.MIN_VALUE;
            at = time;
        } else if (growth != null) {
            // War tearing out of him: built twice a tick (all of it is heavy to build every frame).
            key = (long)Math.floor(time * 2.0) | Long.MIN_VALUE;
            at = Math.floor(time * 2.0) / 2.0;
        }
        // 1.0.17: War's cape and skirt are real cloth (WarCloth) while he stands, walks and fights; lying, swimming, gliding or
        // riding they are drawn still. 1.0.19: leaving him, it is built once, still, as it was when he took it off, and crumbles.
        boolean clothed = war && !shedding && clothed(player);
        if (shedding) {
            key = view.since() | Long.MIN_VALUE;
            at = view.since();
        }
        Built built;
        Built from = null;
        float tween = 0.0F;
        if (war && !shedding && growth == null) {
            // 1.0.28: War as he is between two ticks (Pair).
            long now = (long)Math.floor(time);
            Pair pair = PAIRS.computeIfAbsent(player, p -> new Pair());
            if (pair.next.tick == now && pair.was.tick != now) {
                Built old = pair.was;
                pair.was = pair.next;
                pair.next = old;
            }
            buildInto(pair.was, view.form(), now, now, view.grow(now), night, warMotion(player, view, now), turning, eye, peace, null, clothed,
                com.hex.kagune.control.king.PeaceModel.ARMOUR);
            buildInto(pair.next, view.form(), now + 1L, now + 1.0, view.grow(now + 1.0), night, warMotion(player, view, now + 1.0), turning, eye, peace,
                null, clothed, com.hex.kagune.control.king.PeaceModel.ARMOUR);
            built = pair.next;
            from = pair.was;
            tween = (float)Math.max(0.0, Math.min(1.0, time - now));
        } else {
            PAIRS.remove(player);
            built = built(player, turning >= 0.0 ? KingForm.NONE : view.form(), key, at, grow, night, motion, turning, eye, peace, growth, clothed,
                com.hex.kagune.control.king.PeaceModel.ARMOUR);
        }
        float[] blend = new float[20];
        Living living = null;
        float between = 0.0F;
        if (peaceShown) {
            boolean near = player.m_20238_(Minecraft.m_91087_().f_91063_.m_109153_().m_90583_()) < LIVING_NEAR * LIVING_NEAR;
            living = living(player, view, partial, time, near ? 1 : 4);
            between = (float)Math.max(0.0, Math.min(1.0, (time - living.from) / living.step));
        }
        float[] scratch = new float[20];
        ModelPart[] parts = {model.f_102808_, model.f_102810_, model.f_102811_, model.f_102812_, model.f_102813_, model.f_102814_};
        double plates = 1.0 - (1.0 - NIGHT_PLATES) * night;
        VertexConsumer shapes = buffers.m_6299_(SHAPES);
        Matrix4f[] matrices = new Matrix4f[parts.length];
        Matrix3f[] normals = new Matrix3f[parts.length];
        for (int i = 0; i < parts.length; i++) {
            ModelPart part = parts[i];
            if (part == null || !part.f_104207_) {
                continue;
            }
            pose.m_85836_();
            if (i == 0 || i == 2 || i == 3) {
                PlayerLayers.upperBody(pose, player, partial);
            }
            part.m_104299_(pose);
            matrices[i] = new Matrix4f(pose.m_85850_().m_252922_());
            normals[i] = new Matrix3f(pose.m_85850_().m_252943_());
            pose.m_85849_();
            List<float[]> drawn = built.parts[i];
            List<float[]> before = from == null || from.parts[i].size() != drawn.size() ? null : from.parts[i];
            for (int n = 0; n < drawn.size(); n++) {
                float[] q = drawn.get(n);
                if (q[19] != 0.0F || shedding && !KingShed.shed(q, PARTS[i], gone, scratch)) {
                    continue;
                }
                float[] s = shedding ? scratch : before != null ? between(before.get(n), q, tween, blend) : q;
                for (int k = 0; k < 4; k++) {
                    shape(shapes, matrices[i], normals[i], s, k, dim((int)s[15 + k], plates), light);
                }
            }
            if (living != null) {
                List<float[]> a = living.was.parts[i];
                List<float[]> b = living.next.parts[i];
                boolean paired = a.size() == b.size();
                for (int n = 0; n < b.size(); n++) {
                    float[] q = paired ? between(a.get(n), b.get(n), between, scratch) : b.get(n);
                    if (q[19] != 0.0F) {
                        continue;
                    }
                    for (int k = 0; k < 4; k++) {
                        shape(shapes, matrices[i], normals[i], q, k, dim((int)q[15 + k], plates), light);
                    }
                }
            }
        }
        if (clothed) {
            cloth(pose, shapes, player, model, partial, time, grow, plates, light, view);
        }
        // Two types never take vertices in turn (they share one buffer): what burns is drawn after the rest.
        VertexConsumer lights = buffers.m_6299_(LIGHT);
        for (int i = 0; i < parts.length; i++) {
            if (matrices[i] == null) {
                continue;
            }
            List<float[]> drawn = built.parts[i];
            List<float[]> before = from == null || from.parts[i].size() != drawn.size() ? null : from.parts[i];
            for (int n = 0; n < drawn.size(); n++) {
                float[] q = drawn.get(n);
                if (q[19] == 0.0F || shedding && !KingShed.shed(q, PARTS[i], gone, scratch)) {
                    continue;
                }
                float[] s = shedding ? scratch : before != null ? between(before.get(n), q, tween, blend) : q;
                for (int k = 0; k < 4; k++) {
                    shape(lights, matrices[i], normals[i], s, k, premultiplied(nightGlow((int)s[15 + k], night)), FULL_BRIGHT);
                }
            }
            if (living != null) {
                List<float[]> a = living.was.parts[i];
                List<float[]> b = living.next.parts[i];
                boolean paired = a.size() == b.size();
                for (int n = 0; n < b.size(); n++) {
                    if (b.get(n)[19] == 0.0F) {
                        continue;
                    }
                    float[] q = paired ? between(a.get(n), b.get(n), between, scratch) : b.get(n);
                    for (int k = 0; k < 4; k++) {
                        shape(lights, matrices[i], normals[i], q, k, premultiplied(nightGlow((int)q[15 + k], night)), FULL_BRIGHT);
                    }
                }
            }
        }
        if (war && !shedding && matrices[0] != null) {
            bloom(lights, matrices[0], normals[0], player, partial, grow, night, eye);
        }
    }

    /**
     * The eye's bloom: rings of white light round its point, wider the farther the camera is (so it keeps its size on the screen far
     * off), stronger by night.
     */
    private static void bloom(VertexConsumer lights, Matrix4f matrix, Matrix3f normals, AbstractClientPlayer player, float partial, double grow,
                              double night, double[] eye) {
        Vec3 camera = Minecraft.m_91087_().f_91063_.m_109153_().m_90583_();
        Vec3 eyes = player.m_146892_();
        double distance = Math.sqrt(camera.m_82557_(eyes));
        double reach = Math.max(1.0, Math.min(BLOOM_MAX, distance / BLOOM_NEAR));
        double strength = (0.55 + 0.45 * night) * grow;
        double[] at = WarModel.eye(grow, eye);
        double z = at[2] - 0.02 * reach;
        // Radii (head pixels, times the reach) and the light there: a hot point fading out.
        double[] radii = {0.0, 0.4, 0.9, 1.8, 3.2};
        double[] glow = {1.0, 0.65, 0.3, 0.1, 0.0};
        int sides = 20;
        for (int r = 0; r + 1 < radii.length; r++) {
            double inner = radii[r] * reach;
            double outer = radii[r + 1] * reach;
            int in = grey(glow[r] * strength);
            int out = grey(glow[r + 1] * strength);
            for (int s = 0; s < sides; s++) {
                double a0 = Math.PI * 2.0 * s / sides;
                double a1 = Math.PI * 2.0 * (s + 1) / sides;
                float[] q = {(float)(at[0] + Math.cos(a0) * inner), (float)(at[1] + Math.sin(a0) * inner), (float)z,
                    (float)(at[0] + Math.cos(a0) * outer), (float)(at[1] + Math.sin(a0) * outer), (float)z,
                    (float)(at[0] + Math.cos(a1) * outer), (float)(at[1] + Math.sin(a1) * outer), (float)z,
                    (float)(at[0] + Math.cos(a1) * inner), (float)(at[1] + Math.sin(a1) * inner), (float)z, 0.0F, 0.0F, -1.0F};
                int[] colours = {in, out, out, in};
                for (int k = 0; k < 4; k++) {
                    shape(lights, matrix, normals, q, k, colours[k], FULL_BRIGHT);
                }
            }
        }
    }

    /**
     * 1.0.7: where each rinkaku strikes at this tick, in body pixels ({x, y, z, weight}; null: none): out at its prey and back for a
     * lash, a twitch toward it for a spike fired. The body turns with its yaw (the layer draws in the body's own frame).
     */
    private static double[][] aims(AbstractClientPlayer player, double time) {
        long[][] lashes = KingClient.lashes(player);
        if (lashes == null || player.m_9236_() == null) {
            return null;
        }
        double[][] aims = new double[lashes.length][];
        double yaw = Math.toRadians(player.f_20883_);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double pixels = 16.0 / 0.9375;
        boolean any = false;
        for (int i = 0; i < lashes.length; i++) {
            long[] lash = lashes[i];
            if (lash == null) {
                continue;
            }
            boolean spike = lash[2] == 1L;
            double span = spike ? KingClient.SPIKE_TICKS : KingClient.LASH_TICKS;
            double age = time - lash[1];
            if (age < 0.0 || age > span) {
                continue;
            }
            net.minecraft.world.entity.Entity target = player.m_9236_().m_6815_((int)lash[0]);
            if (target == null) {
                continue;
            }
            double dx = target.m_20185_() - player.m_20185_();
            double dy = target.m_20186_() + target.m_20206_() * 0.5 - player.m_20186_();
            double dz = target.m_20189_() - player.m_20189_();
            double forward = -dx * sin + dz * cos;
            double left = dx * cos + dz * sin;
            double out = age < 2.0 ? age / 2.0 : Math.max(0.0, 1.0 - (age - 2.0) / (span - 2.0));
            aims[i] = new double[] {left * pixels, dy * pixels, -forward * pixels, spike ? 0.35 * out : out};
            any = true;
        }
        return any ? aims : null;
    }

    /** 1.0.17: each War's cloth: the simulation, how far it has run (ticks), his body then, his feet then and a substep before. */
    private static final class Clothing {
        final WarCloth cloth = new WarCloth();
        double time = Double.NaN;
        WarCloth.Pose last;
        double[] feet;
        double[] feetBefore;
        long burstFor = Long.MIN_VALUE;
    }

    private static final Map<AbstractClientPlayer, Clothing> CLOTHING = new WeakHashMap<>();

    /** True while War's cloth can hang as itself on {@code player}: not swimming, gliding, lying down or riding. */
    private static boolean clothed(AbstractClientPlayer player) {
        return !player.m_6069_() && !player.m_21255_() && !player.m_5803_() && !player.m_20159_();
    }

    /**
     * 1.0.17: War's cloth this frame. Run in even steps ({@link WarCloth#SUBSTEPS_PER_TICK} a tick) as far as the frame's time, his body
     * moving evenly from where it was at the last step to where it is now; drawn between its last two steps, carried on with his feet
     * to this frame, on both sides, in the model's own space (the layer's root, as the renderer left it).
     */
    private static void cloth(PoseStack pose, VertexConsumer shapes, AbstractClientPlayer player, HumanoidModel<?> model, float partial, double time,
                              double grow, double plates, int light, KingClient.View view) {
        Clothing c = CLOTHING.computeIfAbsent(player, p -> new Clothing());
        double block = 16.0 / 0.9375;
        double crouch = player.m_6047_() ? -0.125 : 0.0; // as the player renderer lowers a crouching player
        double[] feet = {player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial, player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial + crouch,
            player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial};
        float bodyYaw = player.f_20884_ + wrap(player.f_20883_ - player.f_20884_) * partial;
        double yaw = Math.toRadians(bodyYaw);
        double[] left = {Math.cos(yaw), 0.0, Math.sin(yaw)};
        double[] forward = {-Math.sin(yaw), 0.0, Math.cos(yaw)};
        double ground = player.m_20096_() ? feet[1] - crouch : Double.NaN;
        WarCloth.Pose now = new WarCloth.Pose(frame(model.f_102810_, player, partial, false, feet, left, forward, block),
            frame(model.f_102811_, player, partial, true, feet, left, forward, block), frame(model.f_102812_, player, partial, true, feet, left, forward, block),
            frame(model.f_102813_, player, partial, false, feet, left, forward, block), frame(model.f_102814_, player, partial, false, feet, left, forward, block),
            ground, 1.0 / block);
        double sub = 1.0 / WarCloth.SUBSTEPS_PER_TICK;
        boolean fresh = !c.cloth.ready() || Double.isNaN(c.time) || time < c.time - 1.0 || time - c.time > 20.0 || distance(feet, c.feet) > 4.0;
        if (fresh) {
            c.cloth.reset(now);
            c.time = time;
            c.last = now;
            c.feet = feet;
            c.feetBefore = feet;
        } else {
            int n = (int)Math.floor((time - c.time) / sub);
            if (n > 0) {
                double f = Math.min(1.0, n * sub / (time - c.time));
                WarCloth.Pose to = f >= 0.9999 ? now : between(c.last, now, f);
                double[] feetTo = lerp(c.feet, feet, f);
                c.cloth.step(c.last, to, n, WarCloth.Wind.breeze(time));
                c.feetBefore = lerp(c.feet, feetTo, (n - 1.0) / n);
                c.feet = feetTo;
                c.time += n * sub;
                c.last = to;
            }
        }
        // His coming's burst (KingTransform): the cloth thrown wide, once, as it first comes out of him.
        if (view.on() && view.since() != c.burstFor && KingTransform.bursting(KingForm.WAR, time - view.since())) {
            c.burstFor = view.since();
            c.cloth.burst(now.torso().at(0.0, 5.0, 0.0), 9.0);
        }
        double alpha = Math.max(0.0, Math.min(1.0, (time - c.time) / sub));
        double[] lagged = lerp(c.feetBefore, c.feet, alpha);
        double[] shift = {feet[0] - lagged[0], feet[1] - lagged[1], feet[2] - lagged[2]};
        Matrix4f root = new Matrix4f(pose.m_85850_().m_252922_());
        Matrix3f rootNormals = new Matrix3f(pose.m_85850_().m_252943_());
        c.cloth.draw(alpha, shift, grow, (a, b, cc, d, colour) -> {
            double[] ma = toModel(a, feet, left, forward, block);
            double[] mb = toModel(b, feet, left, forward, block);
            double[] mc = toModel(cc, feet, left, forward, block);
            double[] md = toModel(d, feet, left, forward, block);
            double[] n = unit(cross(minus(mc, ma), minus(md, mb)));
            int shade = dim(colour, plates);
            clothFace(shapes, root, rootNormals, ma, mb, mc, md, n, 0.06, shade, light);
            clothFace(shapes, root, rootNormals, ma, md, mc, mb, new double[] {-n[0], -n[1], -n[2]}, 0.06, shade, light);
        });
    }

    /** A posed part of the model (its pixels, as WarCloth reads them) out in the world: the layer's own transform for it, then his place. */
    private static WarCloth.Frame frame(ModelPart part, AbstractClientPlayer player, float partial, boolean upper, double[] feet, double[] left,
                                        double[] forward, double block) {
        PoseStack local = new PoseStack();
        if (upper) {
            PlayerLayers.upperBody(local, player, partial);
        }
        part.m_104299_(local);
        Matrix4f m = new Matrix4f(local.m_85850_().m_252922_());
        return (x, y, z) -> {
            Vector3f v = m.transformPosition(new Vector3f((float)(x / 16.0), (float)(y / 16.0), (float)(z / 16.0)));
            double mx = v.x * 16.0;
            double my = v.y * 16.0;
            double mz = v.z * 16.0;
            double up = 24.016 - my; // the renderer stands the model 1.501 blocks up, in its pixels
            return new double[] {feet[0] + (left[0] * mx - forward[0] * mz) / block, feet[1] + up / block, feet[2] + (left[2] * mx - forward[2] * mz) / block};
        };
    }

    /** A point of the world in the model's own pixels (x his left, y down from the neck, z back). */
    private static double[] toModel(double[] w, double[] feet, double[] left, double[] forward, double block) {
        double dx = w[0] - feet[0];
        double dy = w[1] - feet[1];
        double dz = w[2] - feet[2];
        return new double[] {(dx * left[0] + dz * left[2]) * block, 24.016 - dy * block, -(dx * forward[0] + dz * forward[2]) * block};
    }

    /** One side of a quad of cloth, lifted {@code lift} pixels off its middle along {@code n}. */
    private static void clothFace(VertexConsumer shapes, Matrix4f matrix, Matrix3f normals, double[] a, double[] b, double[] c, double[] d, double[] n, double lift,
                                  int colour, int light) {
        float[] q = new float[15];
        double[][] corners = {a, b, c, d};
        for (int k = 0; k < 4; k++) {
            q[k * 3] = (float)(corners[k][0] + n[0] * lift);
            q[k * 3 + 1] = (float)(corners[k][1] + n[1] * lift);
            q[k * 3 + 2] = (float)(corners[k][2] + n[2] * lift);
        }
        q[12] = (float)n[0];
        q[13] = (float)n[1];
        q[14] = (float)n[2];
        for (int k = 0; k < 4; k++) {
            shape(shapes, matrix, normals, q, k, colour, light);
        }
    }

    /** A body between two: every point of each part {@code f} of the way from where {@code a} has it to where {@code b} does. */
    private static WarCloth.Pose between(WarCloth.Pose a, WarCloth.Pose b, double f) {
        return new WarCloth.Pose(between(a.torso(), b.torso(), f), between(a.rightArm(), b.rightArm(), f), between(a.leftArm(), b.leftArm(), f),
            between(a.rightLeg(), b.rightLeg(), f), between(a.leftLeg(), b.leftLeg(), f), b.ground(), b.scale());
    }

    private static WarCloth.Frame between(WarCloth.Frame a, WarCloth.Frame b, double f) {
        return (x, y, z) -> lerp(a.at(x, y, z), b.at(x, y, z), f);
    }

    private static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    private static double distance(double[] a, double[] b) {
        return b == null ? Double.MAX_VALUE : Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
    }

    private static double[] minus(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] unit(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return l < 1.0E-9 ? new double[] {0.0, -1.0, 0.0} : new double[] {a[0] / l, a[1] / l, a[2] / l};
    }

    private static float wrap(float degrees) {
        float d = degrees % 360.0F;
        if (d >= 180.0F) {
            d -= 360.0F;
        }
        if (d < -180.0F) {
            d += 360.0F;
        }
        return d;
    }

    /**
     * 1.0.18: {@code player}'s living Peace kagune for this frame: built for the step it is in and the next ({@code step} ticks apart),
     * moved on a step when the time comes (the next becoming what it was), all built again when its move or its wearing changes.
     */
    private static Living living(AbstractClientPlayer player, KingClient.View view, float partial, double time, int step) {
        Living living = LIVING.computeIfAbsent(player, p -> new Living());
        long from = (long)Math.floor(time / step) * step;
        boolean same = living.step == step && living.move == view.move() && living.moveAt == view.moveAt() && living.level == view.level()
            && living.on == view.on() && living.since == view.since();
        if (same && living.from == from) {
            return living;
        }
        if (same && living.from + step == from) {
            Built was = living.was;
            living.was = living.next;
            living.next = was;
        } else {
            fill(living.was, player, view, partial, time, from);
        }
        fill(living.next, player, view, partial, time, from + step);
        living.from = from;
        living.step = step;
        living.move = view.move();
        living.moveAt = view.moveAt();
        living.level = view.level();
        living.on = view.on();
        living.since = view.since();
        return living;
    }

    /** {@code built} filled with {@code player}'s Peace kagune as it is at {@code at} (ticks). */
    private static void fill(Built built, AbstractClientPlayer player, KingClient.View view, float partial, double time, double at) {
        com.hex.kagune.control.king.PeaceMotion.State state = alive(player, view, partial, time, peaceState(player, partial, time, at), at);
        double grow = view.grow(at);
        WarModel.Part[] parts = WarModel.Part.values();
        for (int i = 0; i < parts.length; i++) {
            built.parts[i].clear();
            com.hex.kagune.control.king.PeaceModel.build(into(built.parts[i]), parts[i], at, grow, state, false, null,
                com.hex.kagune.control.king.PeaceModel.KAGUNE);
        }
    }

    /** A quad {@code k} of the way from {@code a} to {@code b} (the same quad built at two moments), written into {@code out}. */
    private static float[] between(float[] a, float[] b, float k, float[] out) {
        for (int i = 0; i < 15; i++) {
            out[i] = a[i] + (b[i] - a[i]) * k;
        }
        for (int i = 15; i < 20; i++) {
            out[i] = b[i];
        }
        return out;
    }

    /** A sink keeping each quad as {corners, normal, colours, glow} in {@code quads}. */
    private static com.hex.kagune.control.ukaku.UkakuModel.Sink into(List<float[]> quads) {
        return (a, b, c, d, n, ca, cb, cc, cd) -> {
            if (!Double.isFinite(a[0] + a[1] + a[2] + b[0] + b[1] + b[2] + c[0] + c[1] + c[2] + d[0] + d[1] + d[2])) {
                return; // 1.0.36: nothing not a number ever reaches the buffer
            }
            boolean glow = n[0] == 0.0 && n[1] == 1.0 && n[2] == 0.0;
            quads.add(new float[] {(float)a[0], (float)a[1], (float)a[2], (float)b[0], (float)b[1], (float)b[2], (float)c[0], (float)c[1], (float)c[2],
                (float)d[0], (float)d[1], (float)d[2], (float)n[0], (float)n[1], (float)n[2], ca, cb, cc, cd, glow ? 1.0F : 0.0F});
        };
    }

    /**
     * 1.0.26: every shape {@code player}'s final kakuja was last drawn with (its armour, and Peace's living kagune as at its latest
     * step), each with its body part (0 head, 1 torso, 2 right arm, 3 left arm, 4 right leg, 5 left leg): what its afterimages are made of
     * ({@link KingAfterimages}). The shapes are the layer's own (corners in the part's model pixels, normal, colours, glow): not to be kept.
     */
    public static void quads(AbstractClientPlayer player, java.util.function.ObjIntConsumer<float[]> out) {
        Pair pair = PAIRS.get(player);
        Built built = pair != null ? pair.next : BUILT.get(player);
        Living living = LIVING.get(player);
        for (int i = 0; i < PARTS.length; i++) {
            if (built != null) {
                for (float[] q : built.parts[i]) {
                    out.accept(q, i);
                }
            }
            if (living != null) {
                for (float[] q : living.next.parts[i]) {
                    out.accept(q, i);
                }
            }
        }
    }

    /** 1.0.26: what War's rinkaku are doing at {@code time}: the move's, aimed at what each struck (third person and first person alike). */
    public static WarModel.Motion warMotion(AbstractClientPlayer player, KingClient.View view, double time) {
        com.hex.kagune.control.king.KingMove move = view.moveAt(time);
        double age = Math.floor(time) - view.moveAt();
        if (move == com.hex.kagune.control.king.KingMove.DEFY) {
            // 1.0.28: the cocoon closing round him, held, opening (by its own clock: the move is held).
            return WarModel.Motion.orbed(com.hex.kagune.control.king.WarRules.defyClosed(age, view.level() == com.hex.kagune.control.king.WarRules.DEFY_OPENING));
        }
        double[][] blow = null;
        if (move == com.hex.kagune.control.king.KingMove.STRIKE) {
            // 1.0.35: his M1 thrown with his rinkaku too, at what it is at.
            blow = WarModel.strikeAims(view.level(), time - view.moveAt(), bodyPixels(player, view.target()));
        }
        return WarModel.Motion.of(move, view.level(), move.progress(age, view.level())).aiming(WarModel.overlay(blow, aims(player, Math.floor(time))));
    }

    /** 1.0.35: body {@code id}'s middle in {@code player}'s body pixels (x his left, y up, z back), or null if it is not here. */
    private static double[] bodyPixels(AbstractClientPlayer player, int id) {
        if (id == 0 || player.m_9236_() == null) {
            return null;
        }
        net.minecraft.world.entity.Entity target = player.m_9236_().m_6815_(id);
        if (target == null) {
            return null;
        }
        double yaw = Math.toRadians(player.f_20883_);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double pixels = 16.0 / 0.9375;
        double dx = target.m_20185_() - player.m_20185_();
        double dy = target.m_20186_() + target.m_20206_() * 0.5 - player.m_20186_();
        double dz = target.m_20189_() - player.m_20189_();
        double forward = -dx * sin + dz * cos;
        double left = dx * cos + dz * sin;
        return new double[] {left * pixels, dy * pixels, -forward * pixels};
    }

    /** 1.0.25: each Peace's six arms alive (PeaceArms), per player. */
    private static final Map<AbstractClientPlayer, com.hex.kagune.control.king.PeaceArms> ARMS = new WeakHashMap<>();

    /** 1.0.25: {@code state} with {@code player}'s living arms in it as they are at {@code at} (ticks; between two, between them). */
    public static com.hex.kagune.control.king.PeaceMotion.State alive(AbstractClientPlayer player, float partial, double time,
                                                                       com.hex.kagune.control.king.PeaceMotion.State state, double at) {
        KingClient.View view = KingClient.get(player);
        return view == null ? state : alive(player, view, partial, time, state, at);
    }

    private static com.hex.kagune.control.king.PeaceMotion.State alive(AbstractClientPlayer player, KingClient.View view, float partial, double time,
                                                                        com.hex.kagune.control.king.PeaceMotion.State state, double at) {
        com.hex.kagune.control.king.PeaceArms arms = ARMS.computeIfAbsent(player,
            p -> new com.hex.kagune.control.king.PeaceArms(p.m_20148_().getLeastSignificantBits()));
        com.hex.kagune.control.king.PeaceArms.Source source = new com.hex.kagune.control.king.PeaceArms.Source() {
            @Override
            public com.hex.kagune.control.king.PeaceMotion.State state(long tick) {
                return peaceState(player, partial, time, tick);
            }

            @Override
            public double grow(long tick) {
                return view.grow(tick);
            }
        };
        double[] motion = motion(player);
        double[][][] lines = at == Math.floor(at) ? arms.lines((long)at, source, motion) : arms.lines(at, source, motion);
        return state.withArms(lines);
    }

    /** 1.0.25: how {@code player}'s body moved this tick: {turn (radians, + to his right), blocks to his right, up, ahead}. */
    private static double[] motion(AbstractClientPlayer player) {
        double yaw = Math.toRadians(player.f_20883_);
        double d = player.f_20883_ - player.f_20884_;
        double turn = Math.toRadians(d - 360.0 * Math.floor((d + 180.0) / 360.0));
        double dx = player.m_20185_() - player.f_19790_;
        double dz = player.m_20189_() - player.f_19792_;
        return new double[] {turn, dx * -Math.cos(yaw) + dz * -Math.sin(yaw), player.m_20186_() - player.f_19791_, dx * -Math.sin(yaw) + dz * Math.cos(yaw)};
    }

    /** 1.0.16: how far each Peace's turning leaves its blades behind, and how fast it runs, eased (per player). */
    private static final Map<AbstractClientPlayer, double[]> SWAY = new WeakHashMap<>();

    /**
     * 1.0.16: what {@code player}'s Peace blades are doing this frame: the move (and the one it cut off), the look's pitch, the blades
     * sent out on their own at bodies ({@link com.hex.kagune.control.client.KingFx#peaceAims}), how far its turning leaves them
     * behind and how fast it runs.
     */
    public static com.hex.kagune.control.king.PeaceMotion.State peaceState(AbstractClientPlayer player, float partial, double time) {
        return peaceState(player, partial, time, time);
    }

    /** 1.0.18: as above, as it will be at {@code at} (ticks; its turning, running and rising as they are now). */
    static com.hex.kagune.control.king.PeaceMotion.State peaceState(AbstractClientPlayer player, float partial, double time, double at) {
        KingClient.View view = KingClient.get(player);
        if (view == null) {
            return com.hex.kagune.control.king.PeaceMotion.State.REST;
        }
        com.hex.kagune.control.king.KingMove move = view.moveAt(at);
        double age = move == com.hex.kagune.control.king.KingMove.NONE ? 0.0 : at - view.moveAt();
        if (move == com.hex.kagune.control.king.KingMove.NONE && KingTransform.coming(view.form(), view.on(), at - view.since())) {
            // 1.0.17: coming over him, its kagune unfolding as wings (never sent: made here from when it was put on).
            move = com.hex.kagune.control.king.KingMove.EMERGE;
            age = at - view.since();
        } else if (!view.on()) {
            // 1.0.19: leaving him, its blades folding in and sinking into his back.
            move = com.hex.kagune.control.king.KingMove.RECEDE;
            age = at - view.since();
        }
        double pitch = -Math.toRadians(player.f_19860_ + (player.m_146909_() - player.f_19860_) * partial);
        double[] sway = SWAY.computeIfAbsent(player, p -> new double[] {0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0});
        if (sway[2] != Math.floor(time)) {
            sway[2] = Math.floor(time);
            // Eased a tick at a time ({was, now} kept for each, so a frame between ticks sits between them): 1.0.18: and how fast he rises.
            sway[3] = sway[0];
            sway[4] = sway[1];
            sway[5] = sway[6];
            double d = player.f_20883_ - player.f_20884_;
            double turn = Math.toRadians(d - 360.0 * Math.floor((d + 180.0) / 360.0));
            double speed = Math.hypot(player.m_20185_() - player.f_19790_, player.m_20189_() - player.f_19792_);
            sway[0] += (Math.max(-0.6, Math.min(0.6, turn * 1.2)) - sway[0]) * 0.35;
            sway[1] += (Math.min(1.0, speed / 0.26) - sway[1]) * 0.25;
            sway[6] += (Math.max(-0.6, Math.min(0.6, player.m_20186_() - player.f_19791_)) - sway[6]) * 0.3;
        }
        double k = at == time ? time - Math.floor(time) : 1.0;
        double lag = sway[3] + (sway[0] - sway[3]) * k;
        double run = sway[4] + (sway[1] - sway[4]) * k;
        double rise = sway[5] + (sway[6] - sway[5]) * k;
        double cut = view.moveAt() - view.wasAt();
        return new com.hex.kagune.control.king.PeaceMotion.State(move, view.level(), age, pitch,
            com.hex.kagune.control.client.KingFx.peaceAims(player, at, at == time ? partial : 1.0F), move == com.hex.kagune.control.king.KingMove.NONE ? com.hex.kagune.control.king.KingMove.NONE
                : view.was(), Math.max(0, view.wasLevel()), cut, lag, run, rise);
    }

    private static int grey(double amount) {
        int v = (int)Math.round(255.0 * Math.max(0.0, Math.min(1.0, amount)));
        return 0xFF000000 | v << 16 | v << 8 | v;
    }

    /**
     * The player's kakuja for this tick, built again only when the tick, the growth or the night has moved on; 1.0.11: or how far
     * they have turned into the Dragon ({@code turning}, -1: not turning).
     */
    private static Built built(AbstractClientPlayer player, KingForm form, long tick, double at, double grow, double night, WarModel.Motion motion,
                               double turning, double[] eye, com.hex.kagune.control.king.PeaceMotion.State peace, double[] growth, boolean clothed, int what) {
        return buildInto(BUILT.computeIfAbsent(player, p -> new Built()), form, tick, at, grow, night, motion, turning, eye, peace, growth, clothed, what);
    }

    /** {@link #built} into {@code built} (left as it is if it is already that). */
    private static Built buildInto(Built built, KingForm form, long tick, double at, double grow, double night, WarModel.Motion motion, double turning,
                                   double[] eye, com.hex.kagune.control.king.PeaceMotion.State peace, double[] growth, boolean clothed, int what) {
        int growStep = (int)Math.round(grow * 40.0);
        int nightStep = (int)Math.round(night * 20.0);
        int turnStep = turning < 0.0 ? -1 : (int)Math.round(turning * com.hex.kagune.control.king.TurningModel.PRELUDE_TICKS);
        if (built.form == form && built.tick == tick && built.growStep == growStep && built.nightStep == nightStep && built.turnStep == turnStep
            && built.clothed == clothed) {
            return built;
        }
        built.clothed = clothed;
        built.form = form;
        built.tick = tick;
        built.growStep = growStep;
        built.nightStep = nightStep;
        built.turnStep = turnStep;
        WarModel.Part[] parts = WarModel.Part.values();
        for (int i = 0; i < parts.length; i++) {
            List<float[]> quads = built.parts[i];
            quads.clear();
            com.hex.kagune.control.ukaku.UkakuModel.Sink sink = into(quads);
            if (turning >= 0.0) {
                com.hex.kagune.control.king.TurningModel.build(sink, parts[i], tick, turning, eye);
            } else if (form == KingForm.PEACE) {
                com.hex.kagune.control.king.PeaceModel.build(sink, parts[i], at, grow, peace, false, growth, what);
            } else {
                WarModel.build(com.hex.kagune.control.king.KingGrowth.wrap(sink, parts[i], growth), parts[i], at, grow, night, motion, eye, clothed);
            }
        }
        return built;
    }

    /** By night every burning red goes low and the eye's white is all that shines (white and grey glows are the eye's). */
    static int nightGlow(int argb, double night) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return r == g && g == b ? argb : dim(argb, 1.0 - NIGHT_RED * night);
    }

    /** {@code argb} with its colour dimmed to {@code share}. */
    static int dim(int argb, double share) {
        int r = (int)Math.round(((argb >> 16) & 0xFF) * share);
        int g = (int)Math.round(((argb >> 8) & 0xFF) * share);
        int b = (int)Math.round((argb & 0xFF) * share);
        return argb & 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** An ARGB colour for the additive type: its alpha folded into its colour. */
    private static int premultiplied(int argb) {
        double a = (argb >>> 24) / 255.0;
        int r = (int)Math.round(((argb >> 16) & 0xFF) * a);
        int g = (int)Math.round(((argb >> 8) & 0xFF) * a);
        int b = (int)Math.round((argb & 0xFF) * a);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static void shape(VertexConsumer consumer, Matrix4f matrix, Matrix3f normals, float[] q, int corner, int colour, int light) {
        consumer.m_252986_(matrix, q[corner * 3] / 16.0F, q[corner * 3 + 1] / 16.0F, q[corner * 3 + 2] / 16.0F)
            .m_6122_((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, colour >>> 24)
            .m_7421_(0.5F, 0.5F)
            .m_86008_(OverlayTexture.f_118083_)
            .m_85969_(light)
            .m_252939_(normals, q[12], q[13], q[14])
            .m_5752_();
    }
}
