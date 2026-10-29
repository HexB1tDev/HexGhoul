package com.hex.kagune.control;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.npc.FleshThrall;
import com.hex.kagune.control.npc.GhoulNpc;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 7.0.2: the expansion's entities, registered on the mod bus ({@link KaguneControlBootstrap}) as the original registers its spike:
 * the ghoul NPC, a monster (so it goes as monsters do when no one is near), a player's size; 1.0.34: Peace's flesh creatures.
 */
public final class ControlEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "hexkagune");
    public static final RegistryObject<EntityType<GhoulNpc>> GHOUL = ENTITIES.register("ghoul", () -> EntityType.Builder.<GhoulNpc>m_20704_(GhoulNpc::new,
        MobCategory.MONSTER).m_20699_(0.6F, 1.8F).m_20702_(10).m_20717_(3).m_20712_(KaguneMod.id("ghoul").toString()));
    /** 1.0.34: Peace's flesh creatures (Rebirth Instinct): belonging to no wild kind, so the world never spawns or keeps them on its own. */
    public static final RegistryObject<EntityType<FleshThrall>> THRALL = ENTITIES.register("flesh_thrall", () -> EntityType.Builder.<FleshThrall>m_20704_(
        FleshThrall::new, MobCategory.MISC).m_20699_(0.6F, 1.95F).m_20702_(8).m_20717_(2).m_20712_(KaguneMod.id("flesh_thrall").toString()));

    private ControlEntities() {
    }

    static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }

    @Mod.EventBusSubscriber(modid = "hexkagune", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Attributes {
        private Attributes() {
        }

        @SubscribeEvent
        public static void attributes(EntityAttributeCreationEvent event) {
            event.put(GHOUL.get(), GhoulNpc.attributes().m_22265_());
            event.put(THRALL.get(), FleshThrall.attributes().m_22265_());
        }
    }
}
