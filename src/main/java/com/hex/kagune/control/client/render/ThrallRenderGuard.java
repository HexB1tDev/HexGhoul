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
 * children; every one of them registered again together, once for each kind found without one.
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
            || entity instanceof com.hex.kagune.control.npc.GhoulNpc;
    }

    /** Every renderer the mod has, registered (again). */
    static void registerAll(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ControlEntities.GHOUL.get(), GhoulNpcRenderer::new);
        event.registerEntityRenderer(ControlEntities.THRALL.get(), NoopRenderer::new);
        event.registerEntityRenderer(ControlEntities.ETO.get(), EtoRenderer::new);
        event.registerEntityRenderer(ControlEntities.TOUKA.get(), context -> new EtoRenderer(context, com.hex.kagune.control.npc.Persona.TOUKA));
        event.registerEntityRenderer(ControlEntities.KEN.get(), context -> new EtoRenderer(context, com.hex.kagune.control.npc.Persona.KEN));
        event.registerEntityRenderer(ControlEntities.CHILD.get(), ChildRenderer::new);
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
