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
 * ghoul leaves when it dies, eaten for good (raw meat, edible even on a full reserve); in the creative Food and Drinks tab. 1.3.2: the
 * Gold Ring (crafted from gold and a diamond), offered to the NPC one is dating to marry her.
 */
public final class ControlItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "hexkagune");
    public static final RegistryObject<Item> GHOUL_MEAT = ITEMS.register(GhoulMeat.ID, () -> new Item(new Item.Properties()
        .m_41489_(new FoodProperties.Builder().m_38760_(GhoulMeat.NUTRITION).m_38758_(GhoulMeat.SATURATION).m_38757_().m_38765_().m_38767_())));

    /** 1.3.2: the ring a proposal is made with (one to a stack). */
    public static final RegistryObject<Item> GOLD_RING = ITEMS.register("gold_ring", () -> new Item(new Item.Properties().m_41487_(1)));

    private ControlItems() {
    }

    /** True for the Gold Ring. */
    public static boolean ring(ItemStack stack) {
        return stack != null && !stack.m_41619_() && stack.m_41720_() == GOLD_RING.get();
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
