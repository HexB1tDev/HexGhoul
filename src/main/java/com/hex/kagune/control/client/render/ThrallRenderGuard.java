package com.hex.kagune.control.client.render;

import com.hex.kagune.control.ControlEntities;
import com.hex.kagune.control.Safety;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.FleshThrall;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.2.1: Peace's flesh creatures can never take the game down for want of a renderer. In some setups the game was left with none for
 * them (two players' crash reports, 1.0.37 and 1.2.0: the entity dispatcher's renderer null the moment one came into view), though
 * one is registered for them at startup ({@link FleshThrallRenderer}, and again with the ghoul NPC's). So as one joins this client's
 * world, the renderer is looked for: none, and it is registered once more and every entity renderer made again from the registered
 * ones (as a resource reload does), before the creature is ever drawn; if there is still none, the creature is kept out of this
 * client's world (the server keeps it, and it still fights), never drawn without one. What happened is logged once.
 * 1.3.1: Eto too (the owner's crash report: the same null renderer as the Library came into view). 1.3.14: every body the mod adds
 * (the owner's crash report on 1.3.12: a newborn child, the same null renderer): wild ghouls, the flesh creatures, Eto, Touka, Ken and
 * children; every one of them registered again together, once for each kind found without one. 1.3.27: the CCG's investigators.
 *
 * <p>4.2.3 (the owner, the seventh time: "Bro, I'm at the exact location says Banjo is at. He's not here."; "the CCG, Banjo, and the circus
 * girl, they're not spawning"): the General (4.1.5), Roma (4.1.49) and Banjou (4.1.54) were never added to {@link #registerAll}, so in a
 * game left without the mod's renderers (the owner's: their crash reports since 1.0.37) the mend made none for them and each was kept out
 * of the player's world: on the server, alive at their spot, and never seen. Every kind the mod registers is in it now (and the humans,
 * {@link #ours}); NpcTests fails the build if a kind is ever left out again.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class ThrallRenderGuard {
    /** The kinds a mend has been tried for already (each is tried once). */
    private static final java.util.Set<net.minecraft.world.entity.EntityType<?>> MENDED = new java.util.HashSet<>();

    private ThrallRenderGuard() {
    }

    /** True for a body the mod adds (and draws). */
    static boolean ours(Entity entity) {
        return entity instanceof FleshThrall || entity instanceof EtoNpc || entity instanceof com.hex.kagune.control.npc.ChildNpc
            || entity instanceof com.hex.kagune.control.npc.GhoulNpc || entity instanceof com.hex.kagune.control.npc.CcgAgent
            || entity instanceof com.hex.kagune.control.npc.HumanNpc; // 4.2.3: the humans too
    }

    /** Every renderer the mod has, registered (again). */
    static void registerAll(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ControlEntities.GHOUL.get(), GhoulNpcRenderer::new);
        event.registerEntityRenderer(ControlEntities.THRALL.get(), NoopRenderer::new);
        event.registerEntityRenderer(ControlEntities.ETO.get(), EtoRenderer::new);
        event.registerEntityRenderer(ControlEntities.TOUKA.get(), context -> new EtoRenderer(context, com.hex.kagune.control.npc.Persona.TOUKA));
        event.registerEntityRenderer(ControlEntities.KEN.get(), context -> new EtoRenderer(context, com.hex.kagune.control.npc.Persona.KEN));
        event.registerEntityRenderer(ControlEntities.AYATO.get(), context -> new EtoRenderer(context, com.hex.kagune.control.npc.Persona.AYATO));
        event.registerEntityRenderer(ControlEntities.NORO.get(), context -> new EtoRenderer(context, com.hex.kagune.control.npc.Persona.NORO));
        // 4.2.3: the General, Roma and Banjou, missing here since they came (so kept out of the owner's game), and the humans.
        event.registerEntityRenderer(ControlEntities.GENERAL.get(), context -> new EtoRenderer(context, com.hex.kagune.control.npc.Persona.GENERAL));
        event.registerEntityRenderer(ControlEntities.ROMA.get(), context -> new EtoRenderer(context, com.hex.kagune.control.npc.Persona.ROMA));
        event.registerEntityRenderer(ControlEntities.BANJOU.get(), context -> new EtoRenderer(context, com.hex.kagune.control.npc.Persona.BANJOU));
        event.registerEntityRenderer(ControlEntities.CHILD.get(), ChildRenderer::new);
        event.registerEntityRenderer(ControlEntities.AGENT.get(), AgentRenderer::new);
        event.registerEntityRenderer(ControlEntities.HUMAN.get(), HumanRenderer::new);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void join(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!event.getLevel().f_46443_ || !ours(entity)) {
            return;
        }
        EntityRenderDispatcher dispatcher = Minecraft.m_91087_().m_91290_();
        if (dispatcher.m_114382_(entity) != null) {
            return;
        }
        String who = entity.getClass().getSimpleName();
        if (MENDED.add(entity.m_6095_())) {
            try {
                registerAll(new EntityRenderersEvent.RegisterRenderers());
                dispatcher.m_6213_(Minecraft.m_91087_().m_91098_());
            } catch (Throwable failure) {
                Safety.caught(failure, "making " + who + "'s renderer again");
            }
            boolean fixed = dispatcher.m_114382_(entity) != null;
            Safety.caught(new IllegalStateException(who + " came into this client's world with no renderer: "
                + (fixed ? "every renderer of the mod was registered again and the entity renderers made again" : "still none after making them "
                + "again, so it is kept out of this client's world")), who + "'s renderer");
        }
        if (dispatcher.m_114382_(entity) == null) {
            event.setCanceled(true);
        }
    }
}
