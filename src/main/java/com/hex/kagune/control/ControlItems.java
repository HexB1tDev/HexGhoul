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
 * netherite-tier swords, so they sweep, wear and take a sword's enchantments), in the Combat tab. Home Living (1.3.40): the ghoul
 * kitchen ({@link com.hex.kagune.control.kitchen.Menu}): human meat and every dish made with it or with ghoul meat, each a
 * {@link com.hex.kagune.control.kitchen.DishItem}, in the Food and Drinks tab after Ghoul Meat; and the kitchen's four effects.
 */
public final class ControlItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "hexkagune");
    public static final RegistryObject<Item> GHOUL_MEAT = ITEMS.register(GhoulMeat.ID, () -> new Item(new Item.Properties()
        .m_41489_(new FoodProperties.Builder().m_38760_(GhoulMeat.NUTRITION).m_38758_(GhoulMeat.SATURATION).m_38757_().m_38765_().m_38767_())));

    /** 1.3.41: Touka's cookbook (one to a stack), read on a right click ({@code CookbookClient}). */
    public static final RegistryObject<Item> COOKBOOK = ITEMS.register(com.hex.kagune.control.kitchen.Cookbook.ID,
        () -> new Item(new Item.Properties().m_41487_(1)));

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

    /** Home Living (1.3.40): the kitchen's dishes, by their item path, in the cookbook's order. */
    private static final java.util.Map<String, RegistryObject<Item>> DISHES = new java.util.LinkedHashMap<>();
    /** And its own effects, by key. */
    private static final DeferredRegister<net.minecraft.world.effect.MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,
        "hexkagune");
    private static final java.util.Map<String, RegistryObject<net.minecraft.world.effect.MobEffect>> KITCHEN_EFFECTS = new java.util.HashMap<>();

    static {
        for (com.hex.kagune.control.kitchen.Dish dish : com.hex.kagune.control.kitchen.Menu.DISHES) {
            DISHES.put(dish.id(), ITEMS.register(dish.id(), () -> new com.hex.kagune.control.kitchen.DishItem(dish, dishProperties(dish))));
        }
        for (com.hex.kagune.control.kitchen.Menu.Kitchen effect : com.hex.kagune.control.kitchen.Menu.EFFECTS) {
            KITCHEN_EFFECTS.put(effect.key(), EFFECTS.register(effect.key(), () -> new com.hex.kagune.control.kitchen.KitchenEffect(effect.colour())));
        }
    }

    private ControlItems() {
    }

    /**
     * A dish's item properties: its food (meat if there is meat in it; a drink or a snack on a full stomach too), its stack, and for a
     * bottled one the bottle given back when it is cooked with (as vanilla's honey bottle).
     */
    private static Item.Properties dishProperties(com.hex.kagune.control.kitchen.Dish dish) {
        FoodProperties.Builder food = new FoodProperties.Builder().m_38760_(dish.nutrition()).m_38758_(dish.saturation());
        if ((dish.humanMeat() || dish.ghoulMeat() > 0) && dish.kind() != com.hex.kagune.control.kitchen.Dish.Kind.DRINK) {
            food = food.m_38757_();
        }
        if (dish.alwaysEdible()) {
            food = food.m_38765_();
        }
        Item.Properties properties = new Item.Properties().m_41489_(food.m_38767_()).m_41487_(dish.stack());
        if (dish.kind() == com.hex.kagune.control.kitchen.Dish.Kind.DRINK && dish.container() != null) {
            Item bottle = vanilla(dish.container());
            if (bottle != null) {
                properties = properties.m_41495_(bottle);
            }
        }
        return properties;
    }

    /** A vanilla (or any) item by its id ("minecraft:bowl"), or null. */
    public static Item vanilla(String id) {
        int colon = id.indexOf(':');
        net.minecraft.resources.ResourceLocation key = net.minecraft.resources.ResourceLocation.m_214293_(id.substring(0, colon), id.substring(colon + 1));
        return key == null ? null : ForgeRegistries.ITEMS.getValue(key);
    }

    /** Home Living (1.3.40): the dish {@code stack} is, or null. */
    public static com.hex.kagune.control.kitchen.Dish dish(ItemStack stack) {
        return stack != null && !stack.m_41619_() && stack.m_41720_() instanceof com.hex.kagune.control.kitchen.DishItem item ? item.dish() : null;
    }

    /** A dish's item by its path ("human_meat"), or null. */
    public static Item dishItem(String id) {
        RegistryObject<Item> item = DISHES.get(id);
        return item == null ? null : item.get();
    }

    /** One of the kitchen's own effects by key, or null. */
    public static net.minecraft.world.effect.MobEffect kitchenEffect(String key) {
        RegistryObject<net.minecraft.world.effect.MobEffect> effect = KITCHEN_EFFECTS.get(key);
        return effect == null ? null : effect.get();
    }

    /** An effect a dish gives, by key: the kitchen's own, or vanilla's by its path ("strength"); null if there is no such effect. */
    public static net.minecraft.world.effect.MobEffect effect(String key) {
        net.minecraft.world.effect.MobEffect own = kitchenEffect(key);
        if (own != null) {
            return own;
        }
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.m_214293_("minecraft", key);
        return id == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(id);
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

    /** 1.3.41: true for Touka's cookbook. */
    public static boolean cookbook(ItemStack stack) {
        return stack != null && !stack.m_41619_() && stack.m_41720_() == COOKBOOK.get();
    }

    /** True for the Gold Ring. */
    public static boolean ring(ItemStack stack) {
        return stack != null && !stack.m_41619_() && stack.m_41720_() == GOLD_RING.get();
    }

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        EFFECTS.register(modBus);
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
                // 1.3.41: the cookbook; 1.3.40: the kitchen, in its order.
                event.accept(COOKBOOK);
                for (RegistryObject<Item> dish : DISHES.values()) {
                    event.accept(dish);
                }
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
