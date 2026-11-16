package com.hex.kagune.control.server;

import com.hex.kagune.control.PlaceClaims;
import com.hex.kagune.control.Safety;
import com.hex.kagune.control.village.LiftRules;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 4.1.51: the CCG tower's lift ({@link LiftRules}). The owner's tower has no stairs, only a lift whose command blocks were never in their
 * file; its cabins' signs say where each goes ("Current Floor: 1 / Dest: 2" ... "Current Floor: 10 / Dest: 1"), and this does it: a
 * player in a cabin who presses its button is taken to the next floor up (from the top, down to the lobby), in the same spot of the
 * cabin there. Only in the world's CCG building (within {@link #NEAR} blocks of its claimed spot, in the overworld): a button elsewhere
 * is a button.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class CcgLift {
    /**
     * How far (blocks, on the level) from the middle of the CCG building's start chunk its lift may be: the tower is moved up to 32 blocks
     * each way to find dry flat ground (ControlStructures: nine spots, 32 apart), and its lift is 9 from its middle.
     */
    static final int NEAR = 72;
    /** When each rider last rode (the world's game time: WorldTimes', forgotten with it). */
    private static final Map<UUID, Long> RODE = WorldTimes.map();

    private CcgLift() {
    }

    @SubscribeEvent
    public static void pressed(PlayerInteractEvent.RightClickBlock event) {
        try {
            if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.m_9236_() instanceof ServerLevel level) || level.m_46472_() != Level.f_46428_) {
                return;
            }
            BlockPos pos = event.getPos();
            if (!button(level, pos.m_123341_(), pos.m_123342_(), pos.m_123343_())) {
                return;
            }
            int[] chunk = PlaceClaims.claimed("ccg_building");
            if (chunk == null) {
                return;
            }
            double fx = (chunk[0] << 4) + 8 - (pos.m_123341_() + 0.5);
            double fz = (chunk[1] << 4) + 8 - (pos.m_123343_() + 0.5);
            if (fx * fx + fz * fz > NEAR * NEAR) {
                return;
            }
            double dx = player.m_20185_() - (pos.m_123341_() + 0.5);
            double dz = player.m_20189_() - (pos.m_123343_() + 0.5);
            if (!LiftRules.aboard(dx, dz) || Math.abs(player.m_20186_() - pos.m_123342_()) > 2.0 || player.m_20159_()) {
                return;
            }
            long now = level.m_46467_();
            Long last = RODE.get(player.m_20148_());
            if (last != null && now - last < LiftRules.REST && now >= last) {
                return;
            }
            int above = 0;
            while (above < LiftRules.MOST && button(level, pos.m_123341_(), pos.m_123342_() + LiftRules.STOREY * (above + 1), pos.m_123343_())) {
                above++;
            }
            int below = 0;
            while (below < LiftRules.MOST && button(level, pos.m_123341_(), pos.m_123342_() - LiftRules.STOREY * (below + 1), pos.m_123343_())) {
                below++;
            }
            int ride = LiftRules.ride(above, below);
            if (ride == 0) {
                return;
            }
            RODE.put(player.m_20148_(), now);
            player.m_6021_(player.m_20185_(), player.m_20186_() + ride, player.m_20189_());
            int floor = LiftRules.floor(ride > 0 ? below + 1 : 0);
            player.m_5661_(Component.m_237113_("§7Lift: §fFloor " + floor + (floor == 1 ? " §7(the lobby)" : "")), true);
        } catch (RuntimeException | LinkageError failure) {
            Safety.caught(failure, "CcgLift.pressed");
        }
    }

    /** True if the block at ({@code x}, {@code y}, {@code z}) is a button (a cabin's). */
    private static boolean button(ServerLevel level, int x, int y, int z) {
        return level.m_8055_(new BlockPos(x, y, z)).m_60734_() instanceof ButtonBlock;
    }
}
