package com.hex.kagune.control.client;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.quinque.Quinque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 1.3.25: the quinques drawn turning: each case's and weapon's model chooses its frame by "hexkagune:deploy" ({@link Quinque#shown}):
 * the case shut, splitting open on its red core, the weapon half out, nearly out, out; a case shutting runs it backwards. The stack's
 * {@link Quinque#AT} says when it turned (set by the server); with none it is still (a case shut, a weapon out).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class QuinqueClient {
    private QuinqueClient() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        // On the game's thread: the property table is a plain map.
        event.enqueueWork(QuinqueClient::register);
    }

    static void register() {
        ResourceLocation id = ResourceLocation.m_214293_("hexkagune", Quinque.PROPERTY);
        for (Quinque quinque : Quinque.values()) {
            ItemProperties.m_174570_(ControlItems.caseOf(quinque), id, (stack, level, entity, seed) -> shown(stack, false));
            ItemProperties.m_174570_(ControlItems.weaponOf(quinque), id, (stack, level, entity, seed) -> shown(stack, true));
        }
    }

    /** 1.3.25: what a quinque is and how it is used, under its name (on the game bus). */
    @Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
    public static final class Tooltips {
        private Tooltips() {
        }

        @SubscribeEvent
        public static void tooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            Quinque shut = ControlItems.quinqueCase(stack);
            Quinque open = shut == null ? ControlItems.quinqueWeapon(stack) : null;
            if (shut == null && open == null) {
                return;
            }
            Quinque quinque = shut != null ? shut : open;
            java.util.List<Component> lines = event.getToolTip();
            lines.add(Component.m_237115_("item.hexkagune.quinque." + quinque.type + ".lore"));
            lines.add(Component.m_237115_("item.hexkagune.quinque.ghoul"));
            lines.add(Component.m_237115_(shut != null ? "item.hexkagune.quinque.open" : "item.hexkagune.quinque.close"));
        }
    }

    /** How far out {@code stack} is drawn now. */
    static float shown(ItemStack stack, boolean weapon) {
        try {
            CompoundTag tag = stack.m_41783_();
            Minecraft minecraft = Minecraft.m_91087_();
            if (tag == null || !tag.m_128441_(Quinque.AT) || minecraft.f_91073_ == null) {
                return weapon ? 1.0F : 0.0F;
            }
            return Quinque.shown(weapon, minecraft.f_91073_.m_46467_(), tag.m_128454_(Quinque.AT));
        } catch (RuntimeException failure) {
            return weapon ? 1.0F : 0.0F;
        }
    }
}
