package com.hex.kagune.control.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 2.4.3 (the owner: "Better Npcs swimming, they get stuck in water super easily for a ton of time, they should be as fast as a player is in
 * water"): every NPC of the mod's in water (companions, children, villagers, wild ghouls, investigators, thralls) swims as a player does:
 * on its way somewhere it goes at a player's swimming pace ({@link #PACE}) the way it faces, its head kept up out of the water, and at a
 * bank (a block in front to climb, room over it) it pulls itself up out ({@link #CLIMB}) instead of nosing at it.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class NpcSwim {
    private NpcSwim() {
    }

    /** Blocks a tick it swims (about a player's: 0.22, the water's drag taking a fifth of it, near 3.5 blocks a second). */
    static final double PACE = 0.22;
    /** How far up it pulls itself onto a bank (a jump's). */
    static final double CLIMB = 0.42;
    /** How fast it rises while its head is under. */
    static final double RISE = 0.06;

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof PathfinderMob mob) || !(mob.m_9236_() instanceof ServerLevel level) || !ours(mob) || !mob.m_20069_()
            || !mob.m_6084_()) {
            return;
        }
        try {
            swim(mob, level);
        } catch (RuntimeException failure) {
            com.hex.kagune.control.Safety.caught(failure, "an NPC swimming");
        }
    }

    static boolean ours(PathfinderMob mob) {
        return mob instanceof com.hex.kagune.control.npc.EtoNpc || mob instanceof com.hex.kagune.control.npc.ChildNpc
            || mob instanceof com.hex.kagune.control.npc.HumanNpc || mob instanceof com.hex.kagune.control.npc.GhoulNpc
            || mob instanceof com.hex.kagune.control.npc.CcgAgent || mob instanceof com.hex.kagune.control.npc.FleshThrall;
    }

    static void swim(PathfinderMob mob, ServerLevel level) {
        Vec3 motion = mob.m_20184_();
        double yaw = Math.toRadians(mob.m_146908_());
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        boolean going = !mob.m_21573_().m_26571_();
        double x = motion.f_82479_;
        double y = motion.f_82480_;
        double z = motion.f_82481_;
        if (going) {
            x = fx * PACE;
            z = fz * PACE;
        } else {
            // Nowhere it is going (no path through water): it makes for the nearest land rather than bobbing in place.
            double[] shore = SHORE.get(mob.m_19879_());
            if (shore == null || mob.f_19797_ % 20 == 0) {
                shore = shore(mob, level);
                SHORE.put(mob.m_19879_(), shore);
                if (SHORE.size() > 512) {
                    SHORE.clear();
                }
            }
            if (shore != null) {
                fx = shore[0];
                fz = shore[1];
                x = fx * PACE;
                z = fz * PACE;
                mob.m_146922_((float)Math.toDegrees(Math.atan2(-fx, fz)));
                going = true;
            }
        }
        double feet = mob.m_20186_();
        double eye = feet + mob.m_20206_() * 0.85;
        if (wet(level, BlockPos.m_274561_(mob.m_20185_(), eye, mob.m_20189_()))) {
            y = Math.max(y, RISE);
        }
        // A bank ahead: a block it would bump into at its feet, with room over it: up and out.
        BlockPos ahead = BlockPos.m_274561_(mob.m_20185_() + fx * 0.75, feet + 0.4, mob.m_20189_() + fz * 0.75);
        BlockPos over = BlockPos.m_274561_(mob.m_20185_() + fx * 0.75, feet + 1.4, mob.m_20189_() + fz * 0.75);
        if (going && solid(level, ahead) && !solid(level, over)) {
            y = CLIMB;
        }
        mob.m_20256_(new Vec3(x, y, z));
    }

    /** Each swimmer's way to the shore (a unit {x, z}), looked again every second. */
    private static final java.util.Map<Integer, double[]> SHORE = new java.util.HashMap<>();

    /** The way (unit {x, z}) to the nearest dry ground it can stand on within 8 blocks, or null if there is none. */
    static double[] shore(PathfinderMob mob, ServerLevel level) {
        double best = Double.MAX_VALUE;
        double[] way = null;
        for (int ring = 1; ring <= 8; ring++) {
            for (int k = 0; k < 16; k++) {
                double a = k * Math.PI / 8.0;
                double dx = Math.cos(a) * ring;
                double dz = Math.sin(a) * ring;
                for (int up = 0; up <= 1; up++) {
                    BlockPos stand = BlockPos.m_274561_(mob.m_20185_() + dx, mob.m_20186_() + up, mob.m_20189_() + dz);
                    BlockPos floor = BlockPos.m_274561_(mob.m_20185_() + dx, mob.m_20186_() + up - 1.0, mob.m_20189_() + dz);
                    if (!wet(level, stand) && !solid(level, stand) && solid(level, floor) && ring < best) {
                        best = ring;
                        double l = Math.sqrt(dx * dx + dz * dz);
                        way = new double[] {dx / l, dz / l};
                    }
                }
            }
            if (way != null) {
                return way;
            }
        }
        return null;
    }

    private static boolean wet(ServerLevel level, BlockPos pos) {
        return !level.m_8055_(pos).m_60819_().m_76178_();
    }

    private static boolean solid(ServerLevel level, BlockPos pos) {
        return !level.m_8055_(pos).m_60812_(level, pos).m_83281_();
    }
}
