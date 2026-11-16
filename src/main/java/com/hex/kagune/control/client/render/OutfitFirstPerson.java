package com.hex.kagune.control.client.render;

import com.hex.kagune.control.outfit.OutfitItem;
import com.hex.kagune.control.outfit.OutfitModels;
import com.hex.kagune.control.outfit.Outfits;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

/**
 * 4.0.0 (The Great Exploration): in first person, an outfit's sleeve on the player's own arm (the owner: "appropriate first-person and
 * third-person rendering"): just before vanilla draws the bare arm ({@link RenderArmEvent}), that arm's cubes of the chestplate worn (the
 * Formal Overcoat's sleeve, cuff and three bands; the Crimson Coat's) are drawn where vanilla puts the arm.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class OutfitFirstPerson {
    private OutfitFirstPerson() {
    }

    @SubscribeEvent
    public static void arm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        if (player == null || player.m_20145_()) {
            return;
        }
        try {
            ItemStack chest = player.m_6844_(EquipmentSlot.CHEST);
            if (!(chest.m_41720_() instanceof OutfitItem item)) {
                return;
            }
            Outfits.Piece piece = item.piece();
            boolean right = ((Enum<?>)event.getArm()).ordinal() == 1;
            OutfitModels.Model model = OutfitModels.get(piece.model());
            if (model == null) {
                return;
            }
            boolean slim = "slim".equals(player.m_108564_());
            List<OutfitModels.Cube> cubes = model.on(OutfitLayer.arm(right ? OutfitModels.RIGHT_ARM : OutfitModels.LEFT_ARM, slim), piece.slot());
            if (cubes.isEmpty()) {
                return;
            }
            PoseStack pose = event.getPoseStack();
            pose.m_85836_();
            // Where vanilla's model puts the arm it is about to draw: at its shoulder, swayed out a little (as KingFirstPerson places War's).
            pose.m_252880_((right ? -KingFirstPerson.SHOULDER_X : KingFirstPerson.SHOULDER_X) / 16.0F,
                (slim ? KingFirstPerson.SLIM_Y : KingFirstPerson.SHOULDER_Y) / 16.0F, 0.0F);
            pose.m_252781_(new Quaternionf().rotateAxis(right ? KingFirstPerson.SWAY : -KingFirstPerson.SWAY, 0.0F, 0.0F, 1.0F));
            OutfitLayer.emit(event.getMultiBufferSource().m_6299_(OutfitLayer.type(piece.model())), pose, cubes, event.getPackedLight());
            pose.m_85849_();
        } catch (RuntimeException failure) {
            PlayerLayers.failed("an outfit's sleeve (first person)", failure);
        }
    }
}
