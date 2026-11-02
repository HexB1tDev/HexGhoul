package com.hex.kagune.control.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.hex.kagune.control.client.NpcSpeech;
import com.hex.kagune.control.npc.BubbleLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * 1.3.38: what an NPC says, drawn as a comic's speech bubble over its head (the owner: "rework the speech bubbles to ACTUAL white speech
 * bubbles or thought clouds, these are horrible"): white, rounded, a dark outline, a tail down to the speaker, the words in black; what it
 * does or thinks ("*stirs*", an action line) in a thought cloud instead, scalloped all round, a trail of little puffs down to its head,
 * the words dark grey and slanted; and over a sleeper, now and then, a small cloud of "z Z z". Turned to the camera as a name tag is,
 * popping in as it starts. The shapes are worked out in {@link com.hex.kagune.control.npc.BubbleShape} (pure); this draws them.
 */
public final class SpeechBubbles {
    private SpeechBubbles() {
    }

    private static final RenderType TYPE = RenderType.m_110473_(Objects.requireNonNull(ResourceLocation.m_214293_("minecraft",
        "textures/block/white_concrete.png")));
    private static final int FULL_BRIGHT = 0xF000F0;
    /** Text pixels to blocks (a name tag's). */
    static final float SCALE = 0.025F;
    /** Not drawn further off than this (blocks). */
    static final double FAR = 32.0;

    /**
     * {@code lines} (as NpcSpeech keeps them: an action's styled grey and slanted) over {@code body}, {@code base} blocks above its head,
     * {@code age} ticks after it was said (for its pop).
     */
    public static void draw(Entity body, List<String> lines, double base, double age, PoseStack pose, MultiBufferSource buffers) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        // 2.0.1: a bubble that fails to draw is logged and skipped, never a crash (2.0.0 crashed the game on every bubble).
        try {
            String[] text = inked(lines);
            boolean thought = com.hex.kagune.control.npc.BubbleShape.thought(lines);
            // 1.3.52: out of the way of the other bubbles up (BubbleLayout), shrinking away at its end as the reply pops up.
            BubbleLayout.Place place = place(body, base);
            double left = NpcSpeech.left(body.m_19879_(), Minecraft.m_91087_().m_91296_());
            render(body, text, thought, base, age, left, place, pose, buffers);
        } catch (RuntimeException | LinkageError failure) {
            com.hex.kagune.control.Safety.caught(failure, "SpeechBubbles.draw");
        }
    }

    private static String[] inked(List<String> lines) {
        boolean thought = com.hex.kagune.control.npc.BubbleShape.thought(lines);
        String[] text = new String[lines.size()];
        for (int i = 0; i < text.length; i++) {
            text[i] = com.hex.kagune.control.npc.BubbleShape.ink(lines.get(i), thought);
        }
        return text;
    }

    /** A sleeper's "z Z z" cloud, {@code clock} its own time (it drifts in and out). */
    public static void snore(Entity body, double clock, PoseStack pose, MultiBufferSource buffers) {
        double phase = (clock + (body.m_19879_() & 63) * 7.0) % 120.0;
        if (phase > 70.0) {
            return;
        }
        try {
            render(body, new String[] {com.hex.kagune.control.npc.BubbleShape.zees(phase)}, true, 0.15, phase, 1.0E9, BubbleLayout.Place.HOME, pose, buffers);
        } catch (RuntimeException | LinkageError failure) {
            com.hex.kagune.control.Safety.caught(failure, "SpeechBubbles.snore");
        }
    }

    // ---------------------------------------------------------------- 1.3.52: keeping them apart

    /** The base each speaker's bubble was last drawn at (blocks over its head: a companion's name tag lifts hers). */
    private static final Map<Integer, Double> BASES = new HashMap<>();
    /** Where each bubble is drawn now (shift, lift in its pixels), eased toward where the layout wants it. */
    private static Map<Integer, float[]> eased = new HashMap<>();
    private static long frameTick = Long.MIN_VALUE;
    private static float framePartial = Float.NaN;
    private static double lastClock = Double.NaN;

    /** Where {@code body}'s bubble goes this frame (the layout worked out once a frame, for every bubble up). */
    static BubbleLayout.Place place(Entity body, double base) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91073_ == null) {
            return BubbleLayout.Place.HOME;
        }
        BASES.put(body.m_19879_(), base);
        long tick = minecraft.f_91073_.m_46467_();
        float partial = minecraft.m_91296_();
        if (tick != frameTick || partial != framePartial) {
            frameTick = tick;
            framePartial = partial;
            try {
                arrange(minecraft, tick, partial);
            } catch (RuntimeException failure) {
                eased = new HashMap<>();
                PlayerLayers.failed("the speech bubbles' layout", failure);
            }
        }
        float[] at = eased.get(body.m_19879_());
        return at == null ? BubbleLayout.Place.HOME : new BubbleLayout.Place(at[0], at[1]);
    }

    /** Every bubble up in this player's sight, as the camera sees it, kept apart ({@link BubbleLayout}); moves eased over a few ticks. */
    private static void arrange(Minecraft minecraft, long tick, float partial) {
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        Font font = minecraft.f_91062_;
        List<BubbleLayout.Seen> seen = new ArrayList<>();
        java.util.Set<Integer> going = new java.util.HashSet<>();
        double reference = Double.NaN;
        for (int id : NpcSpeech.speaking()) {
            if (!(minecraft.f_91073_.m_6815_(id) instanceof LivingEntity body) || body.m_20145_()) {
                continue;
            }
            List<String> lines = NpcSpeech.lines(body);
            if (lines.isEmpty()) {
                continue;
            }
            if (NpcSpeech.left(id, partial) < com.hex.kagune.control.npc.BubbleShape.GOES) {
                going.add(id); // shrinking away as the reply comes: in no one's way, and left where it is
                continue;
            }
            double x = body.f_19790_ + (body.m_20185_() - body.f_19790_) * partial;
            double y = body.f_19791_ + (body.m_20186_() - body.f_19791_) * partial + body.m_20206_() + BASES.getOrDefault(id, 0.3);
            double z = body.f_19792_ + (body.m_20189_() - body.f_19792_) * partial;
            double fx = x - camera.f_82479_;
            double fy = y - camera.f_82480_;
            double fz = z - camera.f_82481_;
            double flat = Math.sqrt(fx * fx + fz * fz);
            double far = Math.sqrt(flat * flat + fy * fy);
            if (far > FAR || far < 1.0E-3) {
                continue;
            }
            // Across the view: to the right as the camera turns that way (facing +z, the right is -x).
            double across = -Math.atan2(fx, fz);
            if (Double.isNaN(reference)) {
                reference = across;
            }
            across = Math.IEEEremainder(across - reference, 2.0 * Math.PI);
            String[] text = inked(lines);
            int widest = 0;
            for (String line : text) {
                widest = Math.max(widest, font.m_92895_(line));
            }
            com.hex.kagune.control.npc.BubbleShape shape = com.hex.kagune.control.npc.BubbleShape.of(widest, text.length,
                com.hex.kagune.control.npc.BubbleShape.thought(lines));
            seen.add(BubbleLayout.Seen.of(id, NpcSpeech.age(id, partial), across, Math.atan2(fy, flat), SCALE / far, shape));
        }
        Map<Integer, BubbleLayout.Place> wanted = BubbleLayout.arrange(seen);
        double clock = tick + partial;
        double dt = Double.isNaN(lastClock) ? 0.0 : Math.max(0.0, Math.min(10.0, clock - lastClock));
        lastClock = clock;
        double ease = 1.0 - Math.exp(-dt / 2.0);
        Map<Integer, float[]> next = new HashMap<>();
        for (Map.Entry<Integer, BubbleLayout.Place> entry : wanted.entrySet()) {
            float[] was = eased.get(entry.getKey());
            BubbleLayout.Place to = entry.getValue();
            next.put(entry.getKey(), was == null ? new float[] {to.shift(), to.lift()}
                : new float[] {(float)(was[0] + (to.shift() - was[0]) * ease), (float)(was[1] + (to.lift() - was[1]) * ease)});
        }
        for (int id : going) {
            float[] was = eased.get(id);
            if (was != null) {
                next.put(id, was);
            }
        }
        eased = next;
        BASES.keySet().retainAll(NpcSpeech.speaking());
    }

    private static void render(Entity body, String[] text, boolean thought, double base, double age, double left, BubbleLayout.Place place,
                               PoseStack pose, MultiBufferSource buffers) {
        Minecraft minecraft = Minecraft.m_91087_();
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        double top = body.m_20206_() + base;
        double dx = camera.f_82479_ - body.m_20185_();
        double dy = camera.f_82480_ - (body.m_20186_() + top);
        double dz = camera.f_82481_ - body.m_20189_();
        double far = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (far > FAR || far < 1.0E-3) {
            return;
        }
        Font font = minecraft.f_91062_;
        int widest = 0;
        for (String line : text) {
            widest = Math.max(widest, font.m_92895_(line));
        }
        com.hex.kagune.control.npc.BubbleShape shape = com.hex.kagune.control.npc.BubbleShape.of(widest, text.length, thought, place.shift(), place.lift());
        float pop = com.hex.kagune.control.npc.BubbleShape.pop(age, left);
        if (pop <= 0.01F) {
            return;
        }
        pose.m_85836_();
        try {
            emit(font, shape, text, top, dx, dy, dz, far, pop, pose, buffers);
        } catch (RuntimeException failure) {
            PlayerLayers.failed("a speech bubble", failure);
        } finally {
            pose.m_85849_();
        }
    }

    private static void emit(Font font, com.hex.kagune.control.npc.BubbleShape shape, String[] text, double top, double dx, double dy, double dz,
                             double far, float pop, PoseStack pose, MultiBufferSource buffers) {
        pose.m_85837_(0.0, top, 0.0);
        // Turned as a name tag is: z away from the camera, x to its left, then flipped so text reads left to right, top down.
        pose.m_252781_(new Quaternionf().rotateAxis((float)Math.atan2(-dx, -dz), 0.0F, 1.0F, 0.0F));
        pose.m_252781_(new Quaternionf().rotateAxis((float)Math.asin(Math.max(-1.0, Math.min(1.0, dy / far))), 1.0F, 0.0F, 0.0F));
        pose.m_85841_(-SCALE * pop, -SCALE * pop, SCALE * pop);
        Matrix4f matrix = pose.m_85850_().m_252922_();
        Matrix3f normals = pose.m_85850_().m_252943_();
        // Lit from straight above (this frame's -y is the world's up): the entity lights give it full brightness, so white is white.
        VertexConsumer out = buffers.m_6299_(TYPE);
        shape.fill((a, b, c, d, argb, depth) -> {
            for (float[] p : new float[][] {a, b, c, d}) {
                out.m_252986_(matrix, p[0], p[1], depth)
                    .m_6122_((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, argb >>> 24)
                    .m_7421_(0.5F, 0.5F)
                    .m_86008_(OverlayTexture.f_118083_)
                    .m_85969_(FULL_BRIGHT)
                    .m_252939_(normals, 0.0F, -1.0F, 0.0F)
                    .m_5752_();
            }
        });
        for (int i = 0; i < text.length; i++) {
            float x = shape.shift - font.m_92895_(text[i]) / 2.0F;
            float y = shape.lineY(i);
            font.m_271703_(text[i], x, y, 0xFF000000, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
        }
    }
}
