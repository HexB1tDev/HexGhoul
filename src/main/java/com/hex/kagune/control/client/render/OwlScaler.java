package com.hex.kagune.control.client.render;

import com.hex.kagune.control.client.PaleClient;
import com.hex.kagune.control.client.OwlClient;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.pale.OwlRules;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 6.2.0: in the Owl her arms (and their sleeves) are drawn thicker ({@link RigPose#BULK_ARMS} at the full bulk), as HexHaki's haki
 * physique draws them, by the model's own scale, put back as it was after (her arms are pushed out by the pose, her chest drawn
 * broader by {@link PaleFleshRenderer}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class OwlScaler {
    /** The players drawn thicker this frame, and each part's scale before (x, z for the arms, then the sleeves). */
    private static final Map<AbstractClientPlayer, float[]> SCALED = new IdentityHashMap<>();

    private OwlScaler() {
    }

    @SubscribeEvent
    public static void pre(RenderPlayerEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player) || !(event.getRenderer().m_7200_() instanceof HumanoidModel<?> model)) {
            return;
        }
        double clock = PaleClient.clock(event.getPartialTick());
        OwlClient.Owl owl = OwlClient.get(player, clock);
        if (owl == null) {
            return;
        }
        float k = 1.0F + RigPose.BULK_ARMS * (float)OwlRules.bulk(owl.grown(clock));
        ModelPart[] parts = parts(model);
        float[] was = new float[parts.length * 2];
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] != null) {
                was[i * 2] = parts[i].f_233553_;
                was[i * 2 + 1] = parts[i].f_233555_;
                parts[i].f_233553_ = was[i * 2] * k;
                parts[i].f_233555_ = was[i * 2 + 1] * k;
            }
        }
        SCALED.put(player, was);
    }

    @SubscribeEvent
    public static void post(RenderPlayerEvent.Post event) {
        float[] was = event.getEntity() instanceof AbstractClientPlayer player ? SCALED.remove(player) : null;
        if (was == null || !(event.getRenderer().m_7200_() instanceof HumanoidModel<?> model)) {
            return;
        }
        ModelPart[] parts = parts(model);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] != null) {
                parts[i].f_233553_ = was[i * 2];
                parts[i].f_233555_ = was[i * 2 + 1];
            }
        }
    }

    /** Her arms and (a player's model) their sleeves. */
    private static ModelPart[] parts(HumanoidModel<?> model) {
        if (model instanceof PlayerModel<?> player) {
            return new ModelPart[] {model.f_102811_, model.f_102812_, player.f_103375_, player.f_103374_};
        }
        return new ModelPart[] {model.f_102811_, model.f_102812_, null, null};
    }
}
