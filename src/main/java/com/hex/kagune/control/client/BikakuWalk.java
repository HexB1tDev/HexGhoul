package com.hex.kagune.control.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.6.2 (the owner: "Being able to climb anything, like Dr. Octopus pretty much."): in The Walker his walking tentacles climb any wall he
 * walks into: going on into it he goes up it (a ladder's pace); holding sneak he hangs on it; letting go he slides down it slowly. His own
 * client moves him (as a ladder does); the server takes the fall off him (BikakuServer).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class BikakuWalk {
    /** Up a wall, blocks a tick (a ladder's); down one he holds onto but doesn't climb. */
    static final double CLIMB = 0.2;
    static final double SLIDE = -0.07;
    /** How far before his feet (from his middle) and at what heights a wall is felt. */
    static final double FEEL = 0.45;
    static final double[] HEIGHTS = {0.2, 1.2};

    private BikakuWalk() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        ClientLevel level = minecraft.f_91073_;
        if (player == null || level == null || !BikakuClient.walking(player, 1.0F) || player.m_20159_() || player.m_21255_()) {
            return;
        }
        Input input = player.f_108618_;
        if (input == null) {
            return;
        }
        double yaw = Math.toRadians(player.m_146908_());
        if (!wallAhead(level, player.m_20185_(), player.m_20186_(), player.m_20189_(), -Math.sin(yaw), Math.cos(yaw))) {
            return;
        }
        Vec3 motion = player.m_20184_();
        double up = input.f_108567_ > 0.1F ? CLIMB : input.f_108573_ ? Math.max(motion.f_82480_, 0.0) : Math.max(motion.f_82480_, SLIDE);
        player.m_20256_(new Vec3(motion.f_82479_, up, motion.f_82481_));
    }

    /** A wall right before his feet or his chest, facing ({@code dx}, {@code dz}). */
    public static boolean wallAhead(ClientLevel level, double x, double y, double z, double dx, double dz) {
        for (double height : HEIGHTS) {
            if (solid(level, x + dx * FEEL, y + height, z + dz * FEEL)) {
                return true;
            }
        }
        return false;
    }

    /** True when the block at that point has something to stand on or grip. */
    public static boolean solid(ClientLevel level, double x, double y, double z) {
        BlockPos pos = BlockPos.m_274561_(x, y, z);
        return !level.m_8055_(pos).m_60812_(level, pos).m_83281_();
    }

    /** {@link #solid} at the block (x, y, z). */
    public static boolean solid(ClientLevel level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        // An unloaded chunk reads as air on the client: nothing to stand on there.
        return !level.m_8055_(pos).m_60812_(level, pos).m_83281_();
    }
}
