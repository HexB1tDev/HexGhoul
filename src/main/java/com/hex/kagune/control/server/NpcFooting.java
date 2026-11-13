package com.hex.kagune.control.server;

import com.hex.kagune.control.Footing;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 4.1.73 (the owner: "Make sure NPCS dont spawn in the ground at any point, especially official NPCS."): every NPC of the mod's put down
 * where it really stands ({@link Footing}: the top of the floor's collision there, with room for its whole body), and any found in the
 * blocks stood out of them. The official NPCs coming back at their homes, sent home, put home stuck or stalled; a child sent home; an ally
 * ghoul brought up behind its player: each set on its floor ({@link #feet}), never at the whole block its home was kept as. And every
 * NPC of the mod's (companions, children, humans, ghouls, investigators, thralls) found in the blocks round it ({@link Footing#INSIDE}
 * deep, {@link Footing#SEEN} checks running, half a second apart) is stood on the nearest floor with room ({@link #guard}): one made into
 * a block built since, left in a wall a door shut on, put down where the ground rose. Never one held, carried or riding (no block stops
 * those), asleep, or sat at its desk (an investigator on its chair's step).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class NpcFooting {
    /** How often (ticks) each NPC is looked at. */
    static final int EVERY = 10;
    /** Each NPC found in the blocks: how many checks running. */
    private static final Map<Entity, Integer> STUCK = new WeakHashMap<>();

    private NpcFooting() {
    }

    /**
     * The height {@code body} stands at put down near ({@code x}, {@code y}, {@code z}): its floor's collision top there with room for it
     * ({@link Footing#stand}); {@code y} itself if there is none near.
     */
    public static double feet(ServerLevel level, Mob body, double x, double y, double z) {
        int bx = (int)Math.floor(x);
        int bz = (int)Math.floor(z);
        AABB box = body.m_20191_();
        double dx = x - body.m_20185_();
        double dz = z - body.m_20189_();
        double from = body.m_20186_();
        double feet = Footing.stand(y, new Footing.Column() {
            @Override
            public double top(int cy) {
                BlockPos pos = new BlockPos(bx, cy, bz);
                VoxelShape shape = level.m_8055_(pos).m_60812_(level, pos);
                if (shape.m_83281_()) {
                    return Double.NaN;
                }
                double top = Double.NaN;
                for (AABB part : shape.m_83299_()) {
                    top = Double.isNaN(top) ? part.f_82292_ : Math.max(top, part.f_82292_);
                }
                return top;
            }

            @Override
            public boolean free(double at) {
                return level.m_45756_(body, box.m_82386_(dx, at - from, dz));
            }
        });
        return Double.isNaN(feet) ? y : feet;
    }

    /** {@code body} (in the world) put at ({@code x}, {@code y}, {@code z}), stood on its floor there ({@link #feet}). */
    public static void put(ServerLevel level, Mob body, double x, double y, double z) {
        body.m_21573_().m_26573_();
        body.m_6021_(x, feet(level, body, x, y, z), z);
    }

    /** Each NPC of the mod's, every {@link #EVERY} ticks: found in the blocks {@link Footing#SEEN} checks running, stood out of them. */
    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof PathfinderMob mob) || !(mob.m_9236_() instanceof ServerLevel level) || !NpcSwim.ours(mob)
            || (mob.f_19797_ + mob.m_19879_()) % EVERY != 0) {
            return;
        }
        try {
            guard(mob, level);
        } catch (RuntimeException failure) {
            com.hex.kagune.control.Safety.caught(failure, "an NPC stood out of the blocks");
        }
    }

    static void guard(PathfinderMob mob, ServerLevel level) {
        if (!mob.m_6084_() || mob.f_19794_ || mob.m_20159_() || mob.m_5803_()
            || mob instanceof com.hex.kagune.control.npc.CcgAgent agent && agent.seated()) {
            STUCK.remove(mob);
            return;
        }
        boolean inside = level.m_186434_(mob, mob.m_20191_().m_82400_(-Footing.INSIDE)).iterator().hasNext();
        if (!inside) {
            STUCK.remove(mob);
            return;
        }
        int times = STUCK.merge(mob, 1, Integer::sum);
        if (!Footing.stuck(times)) {
            return;
        }
        STUCK.remove(mob);
        double y = mob.m_20186_();
        double feet = feet(level, mob, mob.m_20185_(), y, mob.m_20189_());
        if (feet != y) {
            mob.m_21573_().m_26573_();
            mob.m_6021_(mob.m_20185_(), feet, mob.m_20189_());
            mob.m_20256_(Vec3.f_82478_);
            mob.f_19789_ = 0.0F;
        }
    }
}
