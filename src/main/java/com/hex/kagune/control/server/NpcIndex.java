package com.hex.kagune.control.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 4.1.100 (the lag patch, part 2): each world's own list of the mod's NPCs (wild ghouls, investigators, townspeople, companions and
 * officials, children, flesh creatures), kept as each one joins its world. "The gang mates within 160 blocks" was a search of every
 * entity in every chunk of a 320-block box; now it is a walk over this short list ({@link #of}: the same answer, those whose box meets
 * the asked box and pass the filter). A body unloaded, dead and gone or moved to another world is dropped from the list as it is next
 * walked; the list holds its bodies weakly, so nothing is ever kept alive by it.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class NpcIndex {
    private static final Map<Level, Set<LivingEntity>> LEVELS = new WeakHashMap<>();

    private NpcIndex() {
    }

    /** True for the mod's NPCs (the kinds the list holds). */
    public static boolean ours(Entity entity) {
        return entity instanceof com.hex.kagune.control.npc.GhoulNpc || entity instanceof com.hex.kagune.control.npc.CcgAgent
            || entity instanceof com.hex.kagune.control.npc.HumanNpc || entity instanceof com.hex.kagune.control.npc.EtoNpc
            || entity instanceof com.hex.kagune.control.npc.ChildNpc || entity instanceof com.hex.kagune.control.npc.FleshThrall;
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && ours(event.getEntity())) {
            add(level, (LivingEntity)event.getEntity());
        }
    }

    static synchronized void add(Level level, LivingEntity body) {
        LEVELS.computeIfAbsent(level, ignored -> Collections.newSetFromMap(new WeakHashMap<>())).add(body);
    }

    /**
     * The mod's NPCs of {@code type} in {@code level} whose bounding box meets {@code box} and that pass {@code filter}: what
     * {@code level.getEntitiesOfClass(type, box, filter)} finds of them, from the list. On a client, the game's own search.
     */
    public static <T extends Entity> List<T> of(Level level, Class<T> type, AABB box, Predicate<? super T> filter) {
        if (!(level instanceof ServerLevel)) {
            return level.m_6443_(type, box, filter);
        }
        List<T> found = new ArrayList<>();
        synchronized (NpcIndex.class) {
            Set<LivingEntity> bodies = LEVELS.get(level);
            if (bodies == null) {
                return found;
            }
            for (Iterator<LivingEntity> it = bodies.iterator(); it.hasNext(); ) {
                LivingEntity body = it.next();
                if (body == null) {
                    continue;
                }
                if (body.m_213877_() || body.m_9236_() != level) {
                    it.remove(); // unloaded, gone, or moved to another world (its copy there joined that world's list)
                    continue;
                }
                if (type.isInstance(body) && body.m_20191_().m_82381_(box)) {
                    found.add(type.cast(body));
                }
            }
        }
        // The filter outside the lock: it may look at anything (and is the asker's own code).
        found.removeIf(body -> !filter.test(body));
        return found;
    }

    /** How many bodies the list holds for {@code level} (dropping the gone ones); for /kagune lag's readers and the tests. */
    public static int count(Level level) {
        return of(level, LivingEntity.class, new AABB(-3.0E7, -2048.0, -3.0E7, 3.0E7, 4096.0, 3.0E7), body -> true).size();
    }
}
