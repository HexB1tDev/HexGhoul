package com.hex.kagune.control.client;

import com.hex.kagune.control.bikaku.WalkerLegs;
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
 * 3.7.1 (the owner: "the walker should lift the player way higher up (like 3-4 blocks up)  and adapt better to literally any terrain"): his
 * legs carry him {@link WalkerLegs#HEIGHT} blocks over the ground round him ({@link WalkerLegs#ride}: up onto whatever is there, let down
 * under a roof), going at a walking pace as on the ground; he climbs only walls ({@link WalkerLegs#climbable}).
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
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer player = minecraft.f_91074_;
        ClientLevel level = minecraft.f_91073_;
        if (player == null || level == null || !BikakuClient.walking(player, 1.0F) || player.m_20159_() || player.m_21255_()) {
            return;
        }
        double ride = WalkerLegs.ride(new double[] {player.m_20185_(), player.m_20186_(), player.m_20189_()}, (x, y, z) -> solid(level, x, y, z));
        if (event.phase == TickEvent.Phase.START) {
            // 3.7.1: carried at its height, he goes along at a walking pace as on the ground (Minecraft's own air pace is a crawl).
            if (!Double.isNaN(ride) && Math.abs(ride - player.m_20186_()) < 1.0) {
                player.m_6853_(true);
            }
            return;
        }
        Input input = player.f_108618_;
        if (input == null) {
            return;
        }
        double yaw = Math.toRadians(player.m_146908_());
        boolean wall = wallAhead(level, player.m_20185_(), player.m_20186_(), player.m_20189_(), -Math.sin(yaw), Math.cos(yaw));
        Vec3 motion = player.m_20184_();
        double up;
        if (wall && input.f_108567_ > 0.1F) {
            up = CLIMB;
        } else if (wall && input.f_108573_) {
            up = Math.max(motion.f_82480_, 0.0);
        } else if (!Double.isNaN(ride)) {
            up = WalkerLegs.lift(player.m_20186_(), ride);
        } else if (wall) {
            up = Math.max(motion.f_82480_, SLIDE);
        } else {
            return; // nothing in the legs' reach below: he falls (the server takes the fall off him)
        }
        player.m_20256_(new Vec3(motion.f_82479_, up, motion.f_82481_));
        player.f_19789_ = 0.0F;
    }

    /** A wall right before his feet or his chest, facing ({@code dx}, {@code dz}); 3.7.1: a wall, not the underside of a roof. */
    public static boolean wallAhead(ClientLevel level, double x, double y, double z, double dx, double dz) {
        for (double height : HEIGHTS) {
            int bx = (int)Math.floor(x + dx * FEEL);
            int by = (int)Math.floor(y + height);
            int bz = (int)Math.floor(z + dz * FEEL);
            if (WalkerLegs.climbable((a, b, c) -> solid(level, a, b, c), bx, by, bz)) {
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
