package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlEntities;
import com.hex.kagune.control.npc.GhoulNpc;
import com.hex.kagune.control.npc.NpcIdentity;
import com.hex.kagune.control.npc.NpcRules;
import com.hex.kagune.data.KaguneProfile;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 7.0.2: where ghoul NPCs come from: the overworld at night, any biome. Every {@link NpcRules#SPAWN_EVERY} ticks each player out in
 * it has a {@link NpcRules#SPAWN_CHANCE} chance of one turning up {@link NpcRules#SPAWN_NEAREST} to {@link NpcRules#SPAWN_FARTHEST}
 * blocks away, on the surface (never in water or lava, never inside anything), no other player close to the spot, and never near
 * another ghoul NPC ({@link NpcRules#spaced}). The stronger the player who meets it, the stronger it is likely to be.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class GhoulNpcSpawner {
    private GhoulNpcSpawner() {
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.m_46472_() != Level.f_46428_) {
            return;
        }
        if (level.m_46467_() % NpcRules.SPAWN_EVERY != 0L || !NpcRules.night(level.m_46468_())) {
            return;
        }
        for (ServerPlayer player : new ArrayList<>(level.m_6907_())) {
            if (!player.m_5833_() && player.m_217043_().m_188500_() < NpcRules.SPAWN_CHANCE) {
                near(level, player);
            }
        }
    }

    /** One near {@code player}, if a place for it is found on the first few tries (null if not). */
    static GhoulNpc near(ServerLevel level, ServerPlayer player) {
        NpcIdentity who = NpcIdentity.roll(KaguneProfile.isUnlocked(player) ? KaguneProfile.mastery(player) : 0, () -> player.m_217043_().m_188500_());
        for (int attempt = 0; attempt < 4; attempt++) {
            double[] offset = NpcRules.around(player.m_217043_().m_188500_(), player.m_217043_().m_188500_());
            double x = player.m_20185_() + offset[0];
            double z = player.m_20189_() + offset[1];
            if (!clear(level, x, z)) {
                continue;
            }
            GhoulNpc npc = place(level, x, z, who);
            if (npc != null) {
                return npc;
            }
        }
        return null;
    }

    /** True if no player is close to {@code x, z} and the ghoul NPCs there leave room for another. */
    static boolean clear(ServerLevel level, double x, double z) {
        for (ServerPlayer player : level.m_6907_()) {
            double dx = player.m_20185_() - x;
            double dz = player.m_20189_() - z;
            if (dx * dx + dz * dz < NpcRules.CLEAR_OF_PLAYERS * NpcRules.CLEAR_OF_PLAYERS) {
                return false;
            }
        }
        double nearest = Double.MAX_VALUE;
        int crowd = 0;
        double y = level.m_6924_(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)Math.floor(x), (int)Math.floor(z));
        AABB area = new AABB(x - NpcRules.CROWD, y - NpcRules.CROWD, z - NpcRules.CROWD, x + NpcRules.CROWD, y + NpcRules.CROWD, z + NpcRules.CROWD);
        List<GhoulNpc> others = level.m_6443_(GhoulNpc.class, area, npc -> npc.m_6084_());
        for (GhoulNpc other : others) {
            double dx = other.m_20185_() - x;
            double dz = other.m_20189_() - z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            nearest = Math.min(nearest, distance);
            if (distance <= NpcRules.CROWD) {
                crowd++;
            }
        }
        return NpcRules.spaced(nearest, crowd);
    }

    /** {@code who} on the surface at {@code x, z}: on solid ground, out of any fluid, with room to stand (null if not). */
    public static GhoulNpc place(ServerLevel level, double x, double z, NpcIdentity who) {
        int bx = (int)Math.floor(x);
        int bz = (int)Math.floor(z);
        int y = level.m_6924_(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
        BlockState ground = level.m_8055_(BlockPos.m_274561_(bx + 0.5, y - 1, bz + 0.5));
        if (ground.m_60795_() || !ground.m_60819_().m_76178_()) {
            return null;
        }
        GhoulNpc npc = ControlEntities.GHOUL.get().m_20615_(level);
        if (npc == null) {
            return null;
        }
        npc.m_7678_(bx + 0.5, y, bz + 0.5, (float)(npc.m_217043_().m_188500_() * 360.0), 0.0F);
        if (!level.m_45786_(npc)) {
            return null;
        }
        npc.identify(who);
        GhoulNpcBrain.dress(npc);
        level.m_7967_(npc);
        return npc;
    }
}
