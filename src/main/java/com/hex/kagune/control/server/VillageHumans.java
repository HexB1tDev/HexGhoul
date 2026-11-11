package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlEntities;
import com.hex.kagune.control.Safety;
import com.hex.kagune.control.npc.HumanNpc;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 4.1.36 (the owner: "Humans can now however spawn in normal minecraft villages"): every {@link #EVERY} ticks, each vanilla villager within
 * {@link #REACH} blocks of a player in the overworld that has not been looked at yet is looked at once (kept in its saved data): one in
 * {@link #ODDS} brings a human with it, at its feet, unless {@link #MOST} humans are already within {@link #CROWD} blocks. So a vanilla
 * village (a new one or one of an old world) gets a few humans living in it, and a villager farm never fills with them. A wandering trader
 * brings none. The humans wander and talk as any human out of a town does (they have no town's map), about their village ({@link #HOME}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class VillageHumans {
    public static final int EVERY = 100;
    public static final double REACH = 96.0;
    public static final int ODDS = 2;
    public static final int MOST = 4;
    public static final double CROWD = 48.0;
    /** In a villager's saved data: it has been looked at. */
    public static final String KEY = "HexVillageHumans";
    /**
     * 4.1.38: in a brought human's saved data, the village it came to (1, and its x and z under HOME + "X", "Z": where its villager stood): it wanders about there, never
     * further than {@link #HOME_RANGE} blocks (a homeless random walk took it out of the village for good, and its villagers' young brought
     * more, never despawning: they piled up over the world).
     */
    public static final String HOME = "HexVillageHome";
    public static final double HOME_RANGE = 24.0;

    private VillageHumans() {
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.m_46472_() != Level.f_46428_
            || level.m_46467_() % EVERY != 0L) {
            return;
        }
        try {
            for (ServerPlayer player : new ArrayList<>(level.m_6907_())) {
                List<AbstractVillager> fresh = level.m_6443_(AbstractVillager.class, player.m_20191_().m_82400_(REACH),
                    villager -> villager.m_6084_() && !villager.getPersistentData().m_128441_(KEY));
                for (AbstractVillager villager : fresh) {
                    villager.getPersistentData().m_128405_(KEY, 1);
                    if (!(villager instanceof net.minecraft.world.entity.npc.Villager) || villager.m_217043_().m_188503_(ODDS) != 0) {
                        continue;
                    }
                    if (level.m_6443_(HumanNpc.class, villager.m_20191_().m_82400_(CROWD), human -> human.m_6084_()).size() >= MOST) {
                        continue;
                    }
                    bring(level, villager);
                }
            }
        } catch (RuntimeException | LinkageError failure) {
            Safety.caught(failure, "VillageHumans");
        }
    }

    /** A human at {@code villager}'s feet, facing anywhere; kept (never despawns, as a town's people). */
    static HumanNpc bring(ServerLevel level, AbstractVillager villager) {
        HumanNpc human = ControlEntities.HUMAN.get().m_20615_(level);
        if (human == null) {
            return null;
        }
        human.m_7678_(villager.m_20185_(), villager.m_20186_(), villager.m_20189_(), (float)(villager.m_217043_().m_188500_() * 360.0), 0.0F);
        if (!level.m_45786_(human)) {
            return null;
        }
        human.m_21530_();
        human.getPersistentData().m_128405_(HOME + "X", (int)Math.floor(villager.m_20185_()));
        human.getPersistentData().m_128405_(HOME + "Z", (int)Math.floor(villager.m_20189_()));
        human.getPersistentData().m_128405_(HOME, 1);
        level.m_7967_(human);
        return human;
    }
}
