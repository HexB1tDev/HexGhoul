package com.hex.kagune.control.client.render;

import com.hex.kagune.control.FemaleBody;
import com.hex.kagune.control.Genders;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.9: a female player is drawn with the one female body ({@link FemaleBody}, Eto's): the whole of her {@link FemaleBody#SIZE} as
 * big about her feet (her box and eyes are that size too, {@code Genders}), her legs wider and deeper (their pivots spread by her body
 * layer, {@code KaguneBodyAnimation}: PlayerAnimator sets them every frame), a wide-armed skin's arms slim; her bust is a layer
 * ({@link FigureLayer}). Shaped last of anything before she is drawn and put back first after, each scale divided back so whatever
 * else shaped the model (the Owl's bulk) is left as it was.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class FemalePlayers {
    /** The players being drawn shaped (and whether their arms were slimmed), with the pose pushed for their size. */
    private static final Map<AbstractClientPlayer, Boolean> SHAPED = new IdentityHashMap<>();

    private FemalePlayers() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void pre(RenderPlayerEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player) || !Genders.female(player) || SHAPED.containsKey(player)
            || !(event.getRenderer().m_7200_() instanceof PlayerModel<?> model)) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        pose.m_85836_();
        pose.m_85841_(FemaleBody.SIZE, FemaleBody.SIZE, FemaleBody.SIZE);
        boolean wide = !"slim".equals(player.m_108564_());
        FemaleFigure.shapeLegs(model);
        if (wide) {
            FemaleFigure.shapeArms(model);
        }
        SHAPED.put(player, wide);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void post(RenderPlayerEvent.Post event) {
        Boolean wide = event.getEntity() instanceof AbstractClientPlayer player ? SHAPED.remove(player) : null;
        if (wide == null) {
            return;
        }
        if (event.getRenderer().m_7200_() instanceof PlayerModel<?> model) {
            FemaleFigure.unshape(model, wide);
        }
        event.getPoseStack().m_85849_();
    }
}
