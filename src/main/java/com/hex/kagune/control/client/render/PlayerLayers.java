package com.hex.kagune.control.client.render;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.client.animation.BodyAnimations;
import com.hex.kagune.control.client.animation.RigPose;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.joml.Quaternionf;

/**
 * The layers this mod draws on players (the wounds, the kakugan, the kakuja's armor and half mask, Rinkaku's mask), kept on
 * whatever renderer draws them, on any device (3.0.1). In 3.0.0 they were only added when the renderers were built
 * ({@code EntityRenderersEvent.AddLayers}), and on some setups (the user's phone launcher among them) none of them showed while
 * the kagune did. Now:
 * <ul>
 *   <li>they are added when the renderers are built, and again to any player renderer that turns up without them (checked
 *       each time a player is drawn), so a renderer built some other way still gets them;</li>
 *   <li>if a player is drawn without the renderer running its layers (a mod drawing players its own way), they are drawn right
 *       after the player instead, on the same body pose, as the vanilla renderer would place them;</li>
 *   <li>one layer failing is logged once and skipped, never taking the others down with it.</li>
 * </ul>
 * Each case is logged once, so a player's log says which path their game took.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class PlayerLayers {
    /** Our layers on each renderer that has them, wounds first. Weak, so renderers dropped on a resource reload go with it. */
    @SuppressWarnings("rawtypes")
    private static final Map<LivingEntityRenderer, List<RenderLayer>> ATTACHED = new WeakHashMap<>();
    /** Players being drawn whose renderer has not run our layers yet this time. */
    private static final Set<UUID> PENDING = new HashSet<>();
    private static final Set<String> REPORTED = new HashSet<>();
    private static Boolean skinLayers3d;

    private PlayerLayers() {
    }

    /** Puts our layers on {@code renderer} once (the wounds first, see {@link WoundLayer}), and hands them back. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static List<RenderLayer> attach(LivingEntityRenderer renderer) {
        List<RenderLayer> existing = ATTACHED.get(renderer);
        if (existing != null) {
            return existing;
        }
        WoundLayer wounds = new WoundLayer(renderer);
        try {
            List layers = ObfuscationReflectionHelper.getPrivateValue(LivingEntityRenderer.class, renderer, "f_115291_");
            layers.add(0, wounds);
        } catch (RuntimeException unreachable) {
            report("wounds-last", "HexKagune could not put the wound layer first (" + unreachable + "); it is drawn last");
            renderer.m_115326_(wounds);
        }
        KakuganLayer kakugan = new KakuganLayer(renderer);
        KakujaArmorLayer armor = new KakujaArmorLayer(renderer);
        RinkakuMaskLayer mask = new RinkakuMaskLayer(renderer);
        renderer.m_115326_(kakugan);
        renderer.m_115326_(armor);
        renderer.m_115326_(mask);
        SteelBladeLayer blade = new SteelBladeLayer(renderer);
        renderer.m_115326_(blade);
        GraftArmorLayer graft = new GraftArmorLayer(renderer);
        renderer.m_115326_(graft);
        KingLayer king = new KingLayer(renderer);
        renderer.m_115326_(king);
        // 1.3.9: a woman's figure (her bust).
        FigureLayer figure = new FigureLayer(renderer);
        renderer.m_115326_(figure);
        List<RenderLayer> ours = List.of(wounds, kakugan, armor, mask, blade, graft, king, figure);
        ATTACHED.put(renderer, ours);
        return ours;
    }

    /**
     * 5.0.13: turns {@code pose} the way PlayerAnimator turns the head and arms with the upper body's bend (its
     * {@code BipedEntityModelMixin} draws them under {@code IBendHelper.rotateMatrixStack}), before a layer places anything
     * on the head or an arm with the part's own {@code translateAndRotate}, which never carries that turn. Without it a
     * forward lean (Ukaku's kakuja hunch, Rinkaku's crouch, running in a kakuja) left the kakugan buried a pixel or more
     * inside the head it was drawn on, so the eyes never glowed in those forms, and the masks sank into the face with it.
     */
    static void upperBody(PoseStack pose, AbstractClientPlayer player, float partialTick) {
        float[] turn = BodyAnimations.upperTurn(player, partialTick);
        if (turn == null) {
            return;
        }
        pose.m_252880_(0.0F, RigPose.UPPER_PIVOT, 0.0F);
        pose.m_252781_(new Quaternionf().rotateAxis(turn[0], turn[1], turn[2], turn[3]));
        pose.m_252880_(0.0F, -RigPose.UPPER_PIVOT, 0.0F);
    }

    /** Called by each of our layers as it starts: this player's renderer is running its layers. */
    static void ran(AbstractClientPlayer player) {
        PENDING.remove(player.m_20148_());
    }

    /** A layer threw: logged once per layer, then that layer is skipped for the frame and the others still draw. */
    static void failed(String layer, RuntimeException failure) {
        StackTraceElement[] trace = failure.getStackTrace();
        StringBuilder where = new StringBuilder();
        for (int i = 0; i < Math.min(4, trace.length); i++) {
            where.append(i == 0 ? " at " : " < ").append(trace[i]);
        }
        report("failed-" + layer, "HexKagune's " + layer + " layer failed and is skipped: " + failure + where);
    }

    /**
     * True when the 3D Skin Layers mod is loaded: it turns the skin's second layer into voxels standing about a pixel off the
     * body, which would bury anything drawn close to the skin (the kakuja's chest plates, the half mask). Those are then pushed
     * out past the voxels.
     */
    public static boolean skinLayers3d() {
        if (skinLayers3d == null) {
            boolean loaded;
            try {
                loaded = ModList.get() != null && ModList.get().isLoaded("skinlayers3d");
            } catch (RuntimeException unknown) {
                loaded = false;
            }
            skinLayers3d = loaded;
            if (loaded) {
                report("skinlayers3d", "HexKagune: 3D Skin Layers is loaded, the kakuja armor and mask stand out past its voxels");
            }
        }
        return skinLayers3d;
    }

    private static void report(String key, String message) {
        if (REPORTED.add(key)) {
            KaguneMod.LOGGER.info(message);
        }
    }

    @SubscribeEvent
    public static void pre(RenderPlayerEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }
        if (vanished(player)) {
            // 6.0.0: vanished in the wind (the kakuja's passive): nothing of him is drawn, not even his armour or what he holds.
            // 6.0.5: for himself too, in third person (until now his own view still showed his armour and what he held, as a
            // potion of invisibility leaves them).
            event.setCanceled(true);
            return;
        }
        LivingEntityRenderer<?, ?> renderer = event.getRenderer();
        if (renderer != null && !ATTACHED.containsKey(renderer)) {
            attach(renderer);
            report("late-" + renderer.getClass().getName(), "HexKagune added its player layers late, to " + renderer.getClass().getName()
                + " (it was not among the renderers built at load)");
        }
        PENDING.add(player.m_20148_());
    }

    /**
     * 6.0.0: true while {@code player} is vanished by the kakuja's passive: invisible with Ukaku's Steel Blade out or in Rinkaku's
     * Full Kakuja (the server gives the invisibility; this tells it from a potion's, which leaves the armour showing).
     */
    public static boolean vanished(AbstractClientPlayer player) {
        if (!player.m_20145_()) {
            return false;
        }
        com.hex.kagune.client.KaguneClientState.View view = com.hex.kagune.client.KaguneClientState.get(player);
        return view.active() && (com.hex.kagune.control.client.UkakuClient.blade(player, com.hex.kagune.control.client.ControlClient.worldTime(0.0F)) > 0.0
            || view.form() == com.hex.kagune.gameplay.KaguneForm.FULL_KAKUJA);
    }

    /** Before {@code KakuganLayer.OuterLayer.post} gives the held-back second layer back, so the kakugan layer still draws it. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void post(RenderPlayerEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player) || !PENDING.remove(player.m_20148_())) {
            return;
        }
        // The player was drawn but its renderer never ran its layers: draw ours on the body as the vanilla renderer places them.
        // Poses the vanilla renderer turns the whole body for (asleep, swimming, gliding, dying) are left out.
        LivingEntityRenderer renderer = event.getRenderer();
        if (renderer == null || player.m_5833_() || player.m_20145_() || !player.m_6084_() || player.m_5803_() || player.m_6069_()
            || player.m_21255_()) {
            return;
        }
        report("after-" + renderer.getClass().getName(), "HexKagune draws its player layers after the player: " + renderer.getClass().getName()
            + " drew the player without running its layers");
        float partial = event.getPartialTick();
        float previous = player.f_20884_;
        float bodyYaw = previous + (((player.f_20883_ - previous) % 360.0F + 540.0F) % 360.0F - 180.0F) * partial;
        PoseStack pose = event.getPoseStack();
        pose.m_85836_();
        // LivingEntityRenderer.render: setupRotations (turn to the body's yaw), flip, the player's 0.9375 scale, model origin.
        pose.m_252781_(Axis.f_252436_.m_252977_(180.0F - bodyYaw));
        pose.m_85841_(-1.0F, -1.0F, 1.0F);
        pose.m_85841_(0.9375F, 0.9375F, 0.9375F);
        pose.m_252880_(0.0F, -1.501F, 0.0F);
        float age = player.f_19797_ + partial;
        for (RenderLayer layer : attach(renderer)) {
            layer.m_6494_(pose, event.getMultiBufferSource(), event.getPackedLight(), player, 0.0F, 0.0F, partial, age, 0.0F, 0.0F);
        }
        pose.m_85849_();
    }

    /** Adds the layers to both player renderers (wide and slim arms) and any other player skin a mod registers, as they are built. */
    @Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        @SuppressWarnings("rawtypes")
        public static void addLayers(EntityRenderersEvent.AddLayers event) {
            StringBuilder skins = new StringBuilder();
            for (String skin : event.getSkins()) {
                LivingEntityRenderer renderer = event.getSkin(skin);
                if (renderer != null) {
                    attach(renderer);
                    skins.append(skins.length() == 0 ? "" : ", ").append(skin);
                }
            }
            KaguneMod.LOGGER.info("HexKagune added its player layers to the " + skins + " player renderers");
        }
    }
}
