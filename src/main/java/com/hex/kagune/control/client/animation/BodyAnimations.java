package com.hex.kagune.control.client.animation;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.client.KaguneManifestationEffects;
import com.hex.kagune.client.animation.ManifestPainAnimation;
import com.hex.kagune.control.ControlConfig;
import com.hex.kagune.control.ControlMath;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.client.ControlClient;
import com.hex.kagune.control.client.ControlView;
import com.hex.kagune.control.client.PathClient;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Registers the continuous kagune body layer (which also carries the ambient idle stance), and replaces the original manifestation pain pose with a
 * control-scaled version (bytecode trampolines in {@code KaguneManifestationEffects.playPain/stopPain}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BodyAnimations {
    /** Below the original manifest/landing layer (priority 80), so those keep the final say. */
    public static final int BODY_LAYER_PRIORITY = 60;
    public static final ResourceLocation BODY_LAYER = KaguneMod.id("kagune_body");

    /** bendy-lib loaded (PlayerAnimator only bends with it), and the bend readable; null until first asked. */
    private static Boolean bends;

    private BodyAnimations() {
    }

    /**
     * 5.0.13: the turn PlayerAnimator draws this player's head and arms with this frame ({@link RigPose#upperTurn}), from the
     * body bend of every layer of their animation, exactly as its {@code BipedEntityModelMixin} reads it; null when it draws
     * them unturned (no bendy-lib, no animation playing, no bend). The model's own parts never carry this turn, so the layers
     * that draw on the head or an arm add it themselves ({@code PlayerLayers.upperBody}).
     */
    public static float[] upperTurn(AbstractClientPlayer player, float partialTick) {
        if (!bendyLib() || player == null) {
            return null;
        }
        try {
            AnimationStack stack = PlayerAnimationAccess.getPlayerAnimLayer(player);
            if (stack == null || !stack.isActive()) {
                return null;
            }
            Vec3f bend = stack.get3DTransform("body", TransformType.BEND, partialTick, new Vec3f(0.0F, 0.0F, 0.0F));
            return RigPose.upperTurn(bend.getX(), bend.getY());
        } catch (RuntimeException | LinkageError unreadable) {
            // A PlayerAnimator this was not built against: draw the layers on the unturned parts, as before, and say so once.
            bends = false;
            KaguneMod.LOGGER.info("HexKagune could not read PlayerAnimator's body bend (" + unreadable + "); its head layers ignore leans");
            return null;
        }
    }

    /** bendy-lib is loaded (and the bend readable). */
    private static boolean bendyLib() {
        if (bends == null) {
            try {
                bends = ModList.get() != null && ModList.get().isLoaded("bendylib");
            } catch (RuntimeException unknown) {
                bends = false;
            }
        }
        return bends;
    }

    /**
     * 6.0.2: true when elbows and knees can be drawn bent on this client: bendy-lib is loaded and 3D Skin Layers is not (its voxel
     * sleeves and trousers are straight boxes on the parts, so the body layer keeps the limbs straight with it). The idle stances
     * stand on straight limbs otherwise ({@link IdlePose#pose(IdlePose.Style, float, double, boolean)}).
     */
    public static boolean limbsBend() {
        return bendyLib() && !com.hex.kagune.control.client.render.PlayerLayers.skinLayers3d();
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(BODY_LAYER, BODY_LAYER_PRIORITY, KaguneBodyAnimation::new);
    }

    /** Trampoline target for {@code KaguneManifestationEffects.playPain}. */
    public static void playManifestPain(AbstractClientPlayer player, int duration, int limbs) {
        ModifierLayer<IAnimation> layer = KaguneManifestationEffects.bodyEffectLayer(player);
        KagunePath path = PathClient.get(player);
        if (layer == null || path == KagunePath.AYATO || path == KagunePath.ETO || path == KagunePath.FURUTA) {
            // Ayato's wings unfold without the host doubling over: he is never the kagune's victim. Nor is Furuta (7.0.0).
            return;
        }
        float intensity = 1.0F;
        if (ControlConfig.bodyAnimations()) {
            ControlView view = ControlClient.get(player);
            intensity = ControlMath.manifestPainScale(view.control(), view.hunger());
        }
        layer.setAnimation(new ControlledManifestAnimation(duration, limbs, intensity));
    }

    /** Trampoline target for {@code KaguneManifestationEffects.stopPain}. */
    public static void stopManifestPain(AbstractClientPlayer player) {
        ModifierLayer<IAnimation> layer = KaguneManifestationEffects.bodyEffectLayer(player);
        if (layer == null) {
            return;
        }
        IAnimation current = layer.getAnimation();
        if (current instanceof ControlledManifestAnimation || current instanceof ManifestPainAnimation) {
            layer.setAnimation(null);
        }
    }
}
