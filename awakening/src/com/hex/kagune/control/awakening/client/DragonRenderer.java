package com.hex.kagune.control.awakening.client;

import com.hex.kagune.control.awakening.DragonRig;
import com.hex.kagune.control.awakening.V3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Draws every Dragon in view, plus shatter debris and shockwaves, after entities each frame. The mesh
 * itself comes from {@link DragonSculptor}; nothing here affects gameplay.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class DragonRenderer {
    private static final ResourceLocation WHITE = new ResourceLocation("minecraft", "textures/block/white_concrete.png");
    private static final RenderType TYPE = RenderType.entityTranslucent(WHITE);

    private DragonRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || (DragonClient.DRAGONS.isEmpty() && DragonClient.FRAGMENTS.isEmpty() && DragonClient.RINGS.isEmpty())) return;
        float partial = event.getPartialTick();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(TYPE);
        PoseStack stack = event.getPoseStack();
        // 1.0.29: the smooth clock everything else of ours is drawn by (the game time hitches once a second).
        double time = com.hex.kagune.control.client.ControlClient.worldTime(partial);
        for (DragonClient.Dragon dragon : DragonClient.DRAGONS.values()) {
            DragonPose pose = dragon.pose(partial);
            if (pose == null) continue;
            pose.floor = floor(level, pose);
            stack.pushPose();
            stack.translate(pose.anchorX - camera.x, pose.anchorY - camera.y, pose.anchorZ - camera.z);
            DragonMesh mesh = new DragonMesh(consumer, stack.last().pose(), stack.last().normal());
            double distance = new V3(pose.anchorX - camera.x, pose.anchorY - camera.y, pose.anchorZ - camera.z).add(pose.centroid()).length();
            int sides = distance > 140.0 ? 12 : 20;
            DragonSculptor.sculpt(mesh, pose, time, sides, segmentLights(level, pose));
            stack.popPose();
        }
        // Debris is positioned relative to the camera in double precision, then drawn untranslated.
        DragonMesh view = new DragonMesh(consumer, stack.last().pose(), stack.last().normal());
        V3 eye = new V3(camera.x, camera.y, camera.z);
        fragments(view, level, partial, eye);
        rings(view, partial, eye);
        buffers.endBatch(TYPE);
    }

    private static int[] segmentLights(ClientLevel level, DragonPose pose) {
        int[] lights = new int[DragonRig.SEGMENTS];
        for (int i = 0; i < DragonRig.SEGMENTS; i++) {
            V3 w = pose.world(pose.segments[i]);
            lights[i] = LevelRenderer.getLightColor(level, BlockPos.containing(w.x(), w.y() + pose.radii[i], w.z()));
        }
        return lights;
    }

    /** 1.0.32: how far a foot looks down for the ground, and up out of it where it starts inside. */
    private static final int FLOOR_DOWN = 10;
    private static final int FLOOR_UP = 3;

    /**
     * 1.0.32: the ground under its feet: the first block down with something to stand on (or a liquid), stepping up out of one it
     * starts inside; leaves and logs do not count (it crawls through trees, its feet on the land under them).
     */
    private static DragonPose.Floor floor(ClientLevel level, DragonPose pose) {
        return (x, y, z) -> {
            int bx = (int) Math.floor(x + pose.anchorX);
            int by = (int) Math.floor(y + pose.anchorY);
            int bz = (int) Math.floor(z + pose.anchorZ);
            if (ground(level, bx, by, bz)) {
                for (int up = 1; up <= FLOOR_UP; up++) {
                    if (!ground(level, bx, by + up, bz)) return by + up - pose.anchorY;
                }
                return Double.NaN;
            }
            for (int down = 1; down <= FLOOR_DOWN; down++) {
                if (ground(level, bx, by - down, bz)) return by - down + 1 - pose.anchorY;
            }
            return Double.NEGATIVE_INFINITY;
        };
    }

    private static boolean ground(ClientLevel level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty()) return true;
        if (state.getBlock() instanceof LeavesBlock || (state.getBlock() instanceof RotatedPillarBlock && state.ignitedByLava())) return false;
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    private static void fragments(DragonMesh mesh, ClientLevel level, float partial, V3 eye) {
        for (DragonClient.Fragment f : DragonClient.FRAGMENTS) {
            double x = f.x + (f.resting ? 0.0 : f.vx * partial);
            double y = f.y + (f.resting ? 0.0 : f.vy * partial);
            double z = f.z + (f.resting ? 0.0 : f.vz * partial);
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(x, y, z));
            double fade = Math.min(1.0, (f.life - f.age) / 15.0);
            int alpha = (int) (255 * fade);
            double angle = f.angle + f.spin * partial;
            V3 axis = new V3(f.ax, f.ay, f.az);
            V3 c = new V3(x, y, z).sub(eye);
            double s = f.size;
            V3 a = rotate(new V3(s, 0.0, -s * 0.4), axis, angle).add(c);
            V3 b = rotate(new V3(-s * 0.6, s * 0.7, s * 0.2), axis, angle).add(c);
            V3 d = rotate(new V3(-s * 0.5, -s * 0.6, s * 0.5), axis, angle).add(c);
            V3 e = rotate(new V3(0.1 * s, 0.1 * s, -s * 0.9), axis, angle).add(c);
            mesh.triangle(a, b, d, f.color, alpha, light);
            mesh.triangle(a, d, e, DragonPalette.mix(f.color, DragonPalette.CRIMSON, 0.3), alpha, light);
            mesh.triangle(a, e, b, f.color, alpha, light);
            mesh.triangle(b, e, d, DragonPalette.mix(f.color, DragonPalette.VEIN, 0.2), alpha, light);
        }
    }

    private static V3 rotate(V3 v, V3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return v.scale(cos).add(axis.cross(v).scale(sin)).add(axis.scale(axis.dot(v) * (1.0 - cos)));
    }

    private static void rings(DragonMesh mesh, float partial, V3 eye) {
        for (DragonClient.Ring ring : DragonClient.RINGS) {
            double k = Math.min(1.0, (ring.age + partial) / ring.life);
            double eased = 1.0 - Math.pow(1.0 - k, 3.0);
            double outer = ring.radius * eased;
            double inner = Math.max(0.0, outer - 1.5 - ring.radius * 0.08);
            int alpha = (int) (220 * (1.0 - k));
            mesh.annulus(new V3(ring.x, ring.y, ring.z).sub(eye), new V3(1.0, 0.0, 0.0), new V3(0.0, 0.0, 1.0), inner, outer, 48,
                    DragonPalette.CRIMSON, DragonPalette.PALE_EYE, 0, alpha, V3.UP, DragonMesh.FULL_BRIGHT);
        }
    }
}
