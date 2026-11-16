package com.hex.kagune.control.client;

import com.hex.kagune.control.bikaku.MawKakuja;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.6.8 (the owner: "IN kakuja form you cant see anything, the camera needs to adapt to the new size."): his own view in the Bikaku
 * kakuja, sized to it. In first person his view is lifted to its crest ({@link MawKakuja#EYES}: a thing its size sees from up there);
 * in third person the camera turns round that same point and sits {@link MawKakuja#VIEW_BACK} blocks behind it instead of 4. Both grow
 * with it as it grows and shrink back as it falls away, and both stop short of blocks, as Minecraft's own camera does. (What is left of
 * it near the lens is not drawn: {@code BikakuRenderer}.)
 *
 * <p>The camera's own setter is not public: it is reached through Forge's {@code ObfuscationReflectionHelper}; if that ever fails the
 * view is simply Minecraft's own (the game never stops for it).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class BikakuCamera {
    /** How far short of a block the view stops (blocks). */
    private static final double MARGIN = 0.3;

    private static java.lang.reflect.Method setPosition;
    private static boolean failed;

    private BikakuCamera() {
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        if (failed) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        if (player == null || minecraft.f_91073_ == null || minecraft.m_91288_() != player || !BikakuClient.isBikaku(player)) {
            return;
        }
        float partial = (float)event.getPartialTick();
        BikakuClient.View view = BikakuClient.get(player);
        MawKakuja.Growth growth = MawKakuja.growth(view.kakuja(), view.kakujaAge(PaleClient.clock(partial)));
        if (MawKakuja.grown(growth) <= 0.002) {
            return;
        }
        try {
            Vec3 at = place(minecraft, player, partial, growth, event.getYaw(), event.getPitch());
            if (at != null) {
                move(event.getCamera(), at);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            failed = true;
            com.hex.kagune.control.Safety.caught(failure, "the Bikaku kakuja's camera");
        }
    }

    /** Where the camera goes this frame: his lifted view (first person), or that far behind it (third person, either way round). */
    private static Vec3 place(Minecraft minecraft, LocalPlayer player, float partial, MawKakuja.Growth growth, float yaw, float pitch) {
        double feetX = player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial;
        double feetY = player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial;
        double feetZ = player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial;
        Vec3 eye = player.m_20299_(partial);
        // His body's frame, as the kakuja is built in (BikakuRenderer.Frame): right and forward from the body's yaw.
        double bodyYaw = player.f_20884_ + wrap(player.f_20883_ - player.f_20884_) * partial;
        double turn = Math.toRadians(bodyYaw);
        double forwardX = -Math.sin(turn);
        double forwardZ = Math.cos(turn);
        double rightX = -Math.cos(turn);
        double rightZ = -Math.sin(turn);
        double[] rig = MawKakuja.eyes(growth, eye.f_82480_ - feetY);
        Vec3 lifted = new Vec3(feetX + rightX * rig[0] + forwardX * rig[2], feetY + rig[1], feetZ + rightZ * rig[0] + forwardZ * rig[2]);
        Vec3 pivot = shortOf(minecraft, player, eye, lifted);
        if (minecraft.f_91066_.m_92176_().m_90612_()) {
            return pivot;
        }
        // Third person: back from it along the camera's own look (its front view already faces him), stopped short of blocks the way
        // Minecraft's own is (eight rays a little apart, so no corner of the view goes into a block).
        double yawR = Math.toRadians(yaw);
        double pitchR = Math.toRadians(pitch);
        Vec3 look = new Vec3(-Math.sin(yawR) * Math.cos(pitchR), -Math.sin(pitchR), Math.cos(yawR) * Math.cos(pitchR));
        double back = MawKakuja.viewBack(growth);
        for (int corner = 0; corner < 8; corner++) {
            Vec3 from = pivot.m_82520_((corner & 1) * 0.2 - 0.1, (corner >> 1 & 1) * 0.2 - 0.1, (corner >> 2 & 1) * 0.2 - 0.1);
            Vec3 to = from.m_82520_(-look.f_82479_ * back, -look.f_82480_ * back, -look.f_82481_ * back);
            BlockHitResult hit = minecraft.f_91073_.m_45547_(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.m_6662_() != HitResult.Type.MISS) {
                back = Math.min(back, hit.m_82450_().m_82554_(from));
            }
        }
        return pivot.m_82520_(-look.f_82479_ * back, -look.f_82480_ * back, -look.f_82481_ * back);
    }

    /** {@code to}, or as near it from {@code from} as the blocks between let the view go. */
    private static Vec3 shortOf(Minecraft minecraft, LocalPlayer player, Vec3 from, Vec3 to) {
        double length = to.m_82554_(from);
        if (length < 1.0E-3) {
            return to;
        }
        BlockHitResult hit = minecraft.f_91073_.m_45547_(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.m_6662_() == HitResult.Type.MISS) {
            return to;
        }
        double reach = Math.max(0.0, hit.m_82450_().m_82554_(from) - MARGIN) / length;
        return from.m_82549_(to.m_82546_(from).m_82490_(reach));
    }

    private static void move(Camera camera, Vec3 at) throws ReflectiveOperationException {
        if (setPosition == null) {
            setPosition = net.minecraftforge.fml.util.ObfuscationReflectionHelper.findMethod(Camera.class, "m_90581_", Vec3.class);
        }
        setPosition.invoke(camera, at);
    }

    private static double wrap(double degrees) {
        double wrapped = degrees % 360.0;
        return wrapped >= 180.0 ? wrapped - 360.0 : wrapped < -180.0 ? wrapped + 360.0 : wrapped;
    }
}
