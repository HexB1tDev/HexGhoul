package com.hex.kagune.control;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
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
 * Gold Ring (crafted from gold and a diamond), offered to the NPC one is dating to marry her. 1.3.25: the CCG's quinques
 * ({@link com.hex.kagune.control.quinque.Quinque}): each an attaché case and the weapon it opens into (a Bikaku blade, a Koukaku spear;
 * netherite-tier swords, so they sweep, wear and take a sword's enchantments), in the Combat tab.
 */
public final class ControlItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "hexkagune");
    public static final RegistryObject<Item> GHOUL_MEAT = ITEMS.register(GhoulMeat.ID, () -> new Item(new Item.Properties()
        .m_41489_(new FoodProperties.Builder().m_38760_(GhoulMeat.NUTRITION).m_38758_(GhoulMeat.SATURATION).m_38757_().m_38765_().m_38767_())));

    /** 1.3.2: the ring a proposal is made with (one to a stack). */
    public static final RegistryObject<Item> GOLD_RING = ITEMS.register("gold_ring", () -> new Item(new Item.Properties().m_41487_(1)));

    /** 1.3.25: the quinques: each case (shut, one to a stack) and its weapon (out). 1.3.26: five of them. */
    private static final java.util.Map<com.hex.kagune.control.quinque.Quinque, RegistryObject<Item>> CASES =
        new java.util.EnumMap<>(com.hex.kagune.control.quinque.Quinque.class);
    private static final java.util.Map<com.hex.kagune.control.quinque.Quinque, RegistryObject<Item>> WEAPONS =
        new java.util.EnumMap<>(com.hex.kagune.control.quinque.Quinque.class);

    static {
        for (com.hex.kagune.control.quinque.Quinque q : com.hex.kagune.control.quinque.Quinque.values()) {
            CASES.put(q, ITEMS.register(q.caseId, () -> new Item(new Item.Properties().m_41487_(1))));
            WEAPONS.put(q, ITEMS.register(q.weaponId, () -> new SwordItem(Tiers.NETHERITE, q.damage, q.speed, new Item.Properties())));
        }
    }

    private ControlItems() {
    }

    /** 1.3.25: a quinque's case item. */
    public static Item caseOf(com.hex.kagune.control.quinque.Quinque quinque) {
        return CASES.get(quinque).get();
    }

    /** 1.3.25: a quinque's weapon item. */
    public static Item weaponOf(com.hex.kagune.control.quinque.Quinque quinque) {
        return WEAPONS.get(quinque).get();
    }

    /** 1.3.25: the quinque {@code stack} is the case of, or null. */
    public static com.hex.kagune.control.quinque.Quinque quinqueCase(ItemStack stack) {
        if (stack == null || stack.m_41619_()) {
            return null;
        }
        Item item = stack.m_41720_();
        for (java.util.Map.Entry<com.hex.kagune.control.quinque.Quinque, RegistryObject<Item>> e : CASES.entrySet()) {
            if (e.getValue().get() == item) {
                return e.getKey();
            }
        }
        return null;
    }

    /** 1.3.25: the quinque {@code stack} is the weapon of (open), or null. */
    public static com.hex.kagune.control.quinque.Quinque quinqueWeapon(ItemStack stack) {
        if (stack == null || stack.m_41619_()) {
            return null;
        }
        Item item = stack.m_41720_();
        for (java.util.Map.Entry<com.hex.kagune.control.quinque.Quinque, RegistryObject<Item>> e : WEAPONS.entrySet()) {
            if (e.getValue().get() == item) {
                return e.getKey();
            }
        }
        return null;
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
            if (event.getTabKey() == CreativeModeTabs.f_256797_) {
                // 1.3.25: the quinques, each case and its weapon.
                for (com.hex.kagune.control.quinque.Quinque q : com.hex.kagune.control.quinque.Quinque.values()) {
                    event.accept(CASES.get(q));
                    event.accept(WEAPONS.get(q));
                }
            }
        }
    }
}
