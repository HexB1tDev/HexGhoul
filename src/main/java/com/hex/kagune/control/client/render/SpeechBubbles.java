package com.hex.kagune.control.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
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
        boolean thought = com.hex.kagune.control.npc.BubbleShape.thought(lines);
        String[] text = new String[lines.size()];
        for (int i = 0; i < text.length; i++) {
            text[i] = com.hex.kagune.control.npc.BubbleShape.ink(lines.get(i), thought);
        }
        render(body, text, thought, base, age, pose, buffers);
    }

    /** A sleeper's "z Z z" cloud, {@code clock} its own time (it drifts in and out). */
    public static void snore(Entity body, double clock, PoseStack pose, MultiBufferSource buffers) {
        double phase = (clock + (body.m_19879_() & 63) * 7.0) % 120.0;
        if (phase > 70.0) {
            return;
        }
        render(body, new String[] {com.hex.kagune.control.npc.BubbleShape.zees(phase)}, true, 0.15, phase, pose, buffers);
    }

    private static void render(Entity body, String[] text, boolean thought, double base, double age, PoseStack pose, MultiBufferSource buffers) {
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
        com.hex.kagune.control.npc.BubbleShape shape = com.hex.kagune.control.npc.BubbleShape.of(widest, text.length, thought);
        float pop = com.hex.kagune.control.npc.BubbleShape.pop(age);
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
            float x = -font.m_92895_(text[i]) / 2.0F;
            float y = shape.lineY(i);
            font.m_271703_(text[i], x, y, 0xFF000000, false, matrix, buffers, Font.DisplayMode.f_181350_, 0, FULL_BRIGHT);
        }
    }
}
