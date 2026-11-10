package com.hex.kagune.control;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.npc.EtoNpc;
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
 * the ghoul NPC, a monster (so it goes as monsters do when no one is near), a player's size; 1.0.34: Peace's flesh creatures; 1.3.0: Eto.
 */
public final class ControlEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "hexkagune");
    public static final RegistryObject<EntityType<GhoulNpc>> GHOUL = ENTITIES.register("ghoul", () -> EntityType.Builder.<GhoulNpc>m_20704_(GhoulNpc::new,
        MobCategory.MONSTER).m_20699_(0.6F, 1.8F).m_20702_(10).m_20717_(3).m_20712_(KaguneMod.id("ghoul").toString()));
    /** 1.0.34: Peace's flesh creatures (Rebirth Instinct): belonging to no wild kind, so the world never spawns or keeps them on its own. */
    public static final RegistryObject<EntityType<FleshThrall>> THRALL = ENTITIES.register("flesh_thrall", () -> EntityType.Builder.<FleshThrall>m_20704_(
        FleshThrall::new, MobCategory.MISC).m_20699_(0.6F, 1.95F).m_20702_(8).m_20717_(2).m_20712_(KaguneMod.id("flesh_thrall").toString()));

    /** 1.3.0: Eto, who lives in the Library: a creature, not a monster, so peaceful never takes her; a player's size. */
    public static final RegistryObject<EntityType<EtoNpc>> ETO = ENTITIES.register("eto", () -> EntityType.Builder.<EtoNpc>m_20704_(EtoNpc::new,
        MobCategory.CREATURE).m_20699_(0.6F * com.hex.kagune.control.npc.EtoRules.SIZE, 1.8F * com.hex.kagune.control.npc.EtoRules.SIZE).m_20702_(10).m_20717_(1).m_20712_(KaguneMod.id("eto").toString()));

    /** 1.3.14: Touka and Ken, companions after Eto: creatures (never despawned), each their persona's size. */
    public static final RegistryObject<EntityType<com.hex.kagune.control.npc.ToukaNpc>> TOUKA = ENTITIES.register("touka",
        () -> EntityType.Builder.<com.hex.kagune.control.npc.ToukaNpc>m_20704_(com.hex.kagune.control.npc.ToukaNpc::new, MobCategory.CREATURE)
            .m_20699_(0.6F * com.hex.kagune.control.npc.Persona.TOUKA.size(), 1.8F * com.hex.kagune.control.npc.Persona.TOUKA.size()).m_20702_(10).m_20717_(1)
            .m_20712_(KaguneMod.id("touka").toString()));
    public static final RegistryObject<EntityType<com.hex.kagune.control.npc.KenNpc>> KEN = ENTITIES.register("ken",
        () -> EntityType.Builder.<com.hex.kagune.control.npc.KenNpc>m_20704_(com.hex.kagune.control.npc.KenNpc::new, MobCategory.CREATURE)
            .m_20699_(0.6F * com.hex.kagune.control.npc.Persona.KEN.size(), 1.8F * com.hex.kagune.control.npc.Persona.KEN.size()).m_20702_(10).m_20717_(1)
            .m_20712_(KaguneMod.id("ken").toString()));

    /** 4.0.0 (The Great Exploration): Ayato and Noro, Aogiri Tree's: creatures (never despawned), each their persona's size. */
    public static final RegistryObject<EntityType<com.hex.kagune.control.npc.AyatoNpc>> AYATO = ENTITIES.register("ayato",
        () -> EntityType.Builder.<com.hex.kagune.control.npc.AyatoNpc>m_20704_(com.hex.kagune.control.npc.AyatoNpc::new, MobCategory.CREATURE)
            .m_20699_(0.6F * com.hex.kagune.control.npc.Persona.AYATO.size(), 1.8F * com.hex.kagune.control.npc.Persona.AYATO.size()).m_20702_(10).m_20717_(1)
            .m_20712_(KaguneMod.id("ayato").toString()));
    /** 4.1.5: the CCG's General: a creature (never despawned), his persona's size. */
    public static final RegistryObject<EntityType<com.hex.kagune.control.npc.GeneralNpc>> GENERAL = ENTITIES.register("general",
        () -> EntityType.Builder.<com.hex.kagune.control.npc.GeneralNpc>m_20704_(com.hex.kagune.control.npc.GeneralNpc::new, MobCategory.CREATURE)
            .m_20699_(0.6F * com.hex.kagune.control.npc.Persona.GENERAL.size(), 1.8F * com.hex.kagune.control.npc.Persona.GENERAL.size()).m_20702_(10).m_20717_(1)
            .m_20712_(KaguneMod.id("general").toString()));
    public static final RegistryObject<EntityType<com.hex.kagune.control.npc.NoroNpc>> NORO = ENTITIES.register("noro",
        () -> EntityType.Builder.<com.hex.kagune.control.npc.NoroNpc>m_20704_(com.hex.kagune.control.npc.NoroNpc::new, MobCategory.CREATURE)
            .m_20699_(0.6F * com.hex.kagune.control.npc.Persona.NORO.size(), 1.8F * com.hex.kagune.control.npc.Persona.NORO.size()).m_20702_(10).m_20717_(1)
            .m_20712_(KaguneMod.id("noro").toString()));

    /**
     * 1.3.10: a child: a creature (never despawned), boxed as a player and shrunk to its stage by the size event ({@code Genders}); its
     * looks made on each client from what it inherited.
     */
    public static final RegistryObject<EntityType<com.hex.kagune.control.npc.ChildNpc>> CHILD = ENTITIES.register("child",
        () -> EntityType.Builder.<com.hex.kagune.control.npc.ChildNpc>m_20704_(com.hex.kagune.control.npc.ChildNpc::new, MobCategory.CREATURE)
            .m_20699_(0.6F, 1.8F).m_20702_(10).m_20717_(1).m_20712_(KaguneMod.id("child").toString()));

    /**
     * 1.3.27: a CCG investigator: a player's size; of no wild kind (the mob caps never count them: they turn up in pairs, AgentSpawner),
     * going as a wanderer does when no one is near.
     */
    public static final RegistryObject<EntityType<com.hex.kagune.control.npc.CcgAgent>> AGENT = ENTITIES.register("ccg_investigator",
        () -> EntityType.Builder.<com.hex.kagune.control.npc.CcgAgent>m_20704_(com.hex.kagune.control.npc.CcgAgent::new, MobCategory.MISC)
            .m_20699_(0.6F, 1.8F).m_20702_(10).m_20717_(2).m_20712_(KaguneMod.id("ccg_investigator").toString()));

    /**
     * Home Living (1.3.40): The Human: a person (a creature, a player's size), made only by command for now (no wild kind spawns it); it
     * runs from ghouls and leaves Human Meat.
     */
    public static final RegistryObject<EntityType<com.hex.kagune.control.npc.HumanNpc>> HUMAN = ENTITIES.register("human",
        () -> EntityType.Builder.<com.hex.kagune.control.npc.HumanNpc>m_20704_(com.hex.kagune.control.npc.HumanNpc::new, MobCategory.CREATURE)
            .m_20699_(0.6F, 1.8F).m_20702_(10).m_20717_(3).m_20712_(KaguneMod.id("human").toString()));

    private ControlEntities() {
    }

    static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        // 1.3.2: one of her in a world, made again at her home some days after she is killed.
        com.hex.kagune.control.server.UniqueNpcs.kind(com.hex.kagune.control.npc.EtoRules.KEY, ETO);
        // 1.3.14: and Touka, and Ken: one each.
        com.hex.kagune.control.server.UniqueNpcs.kind(com.hex.kagune.control.npc.Persona.TOUKA.key(), TOUKA);
        com.hex.kagune.control.server.UniqueNpcs.kind(com.hex.kagune.control.npc.Persona.KEN.key(), KEN);
        // 4.0.0: Ayato and Noro: one each, back at their hideout a day after they are killed.
        com.hex.kagune.control.server.UniqueNpcs.kind(com.hex.kagune.control.npc.Persona.AYATO.key(), AYATO);
        com.hex.kagune.control.server.UniqueNpcs.kind(com.hex.kagune.control.npc.Persona.NORO.key(), NORO);
        // 4.1.5: the General: one, back in the CCG's building a day after he is killed.
        com.hex.kagune.control.server.UniqueNpcs.kind(com.hex.kagune.control.npc.Persona.GENERAL.key(), GENERAL);
        // 1.3.10: births make children; each one's body made again at its home a day after it dies.
        com.hex.kagune.control.server.Children.init();
    }

    @Mod.EventBusSubscriber(modid = "hexkagune", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Attributes {
        private Attributes() {
        }

        @SubscribeEvent
        public static void attributes(EntityAttributeCreationEvent event) {
            event.put(GHOUL.get(), GhoulNpc.attributes().m_22265_());
            event.put(THRALL.get(), FleshThrall.attributes().m_22265_());
            event.put(ETO.get(), EtoNpc.attributes().m_22265_());
            event.put(TOUKA.get(), EtoNpc.attributes(com.hex.kagune.control.npc.Persona.TOUKA).m_22265_());
            event.put(KEN.get(), EtoNpc.attributes(com.hex.kagune.control.npc.Persona.KEN).m_22265_());
            event.put(AYATO.get(), EtoNpc.attributes(com.hex.kagune.control.npc.Persona.AYATO).m_22265_());
            event.put(NORO.get(), EtoNpc.attributes(com.hex.kagune.control.npc.Persona.NORO).m_22265_());
            event.put(GENERAL.get(), EtoNpc.attributes(com.hex.kagune.control.npc.Persona.GENERAL).m_22265_());
            event.put(CHILD.get(), com.hex.kagune.control.npc.ChildNpc.attributes().m_22265_());
            event.put(AGENT.get(), com.hex.kagune.control.npc.CcgAgent.attributes().m_22265_());
            event.put(HUMAN.get(), com.hex.kagune.control.npc.HumanNpc.attributes().m_22265_());
        }
    }
}
