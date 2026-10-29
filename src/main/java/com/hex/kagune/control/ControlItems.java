package com.hex.kagune.control;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 7.0.1: the expansion's items, registered on the mod bus ({@link KaguneControlBootstrap}). Ghoul Meat ({@link GhoulMeat}): what a
 * ghoul leaves when it dies, eaten for good (raw meat, edible even on a full reserve); in the creative Food and Drinks tab.
 */
public final class ControlItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "hexkagune");
    public static final RegistryObject<Item> GHOUL_MEAT = ITEMS.register(GhoulMeat.ID, () -> new Item(new Item.Properties()
        .m_41489_(new FoodProperties.Builder().m_38760_(GhoulMeat.NUTRITION).m_38758_(GhoulMeat.SATURATION).m_38757_().m_38765_().m_38767_())));

    private ControlItems() {
    }

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    /** True for a stack of Ghoul Meat. */
    public static boolean ghoulMeat(ItemStack stack) {
        return stack != null && !stack.m_41619_() && stack.m_41720_() == GHOUL_MEAT.get();
    }

    @Mod.EventBusSubscriber(modid = "hexkagune", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Tabs {
        private Tabs() {
        }

        @SubscribeEvent
        public static void tabs(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey() == CreativeModeTabs.f_256839_) {
                event.accept(GHOUL_MEAT);
            }
        }
    }
}
