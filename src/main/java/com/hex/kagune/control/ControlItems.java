package com.hex.kagune.control;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.eventbus.api.IEventBus;
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
            // 4.1.20: a quinque grown as a mesh is drawn by code (client.QuinqueItemRenderer), the rest by their item models.
            WEAPONS.put(q, ITEMS.register(q.weaponId, () -> com.hex.kagune.control.quinque.QuinqueMesh.has(q) ? new MeshQuinque(q)
                : new SwordItem(Tiers.NETHERITE, q.damage, q.speed, new Item.Properties())));
        }
    }

    /**
     * 4.1.20: a quinque grown as a smooth mesh (the owner: "every weapon in the game should look like it's made from a kagune ... I don't want
     * squares"): a sword as the others are, its model's parent builtin/entity, drawn by client.QuinqueItemRenderer, which Forge is given
     * here on a client only (the item's constructor calls this there; the server never runs it, nor loads the renderer).
     */
    static final class MeshQuinque extends SwordItem {
        MeshQuinque(com.hex.kagune.control.quinque.Quinque q) {
            super(Tiers.NETHERITE, q.damage, q.speed, new Item.Properties());
        }

        @Override
        public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.hex.kagune.control.client.QuinqueItemRenderer.offer(consumer));
        }
    }

    /**
     * 3.0.6: the investigators' kagune-made weapons ({@link com.hex.kagune.control.quinque.Arm}) as items: each its own case (shut, one to a
     * stack) and its weapon (out; drawn from its mesh in the hand, ArmItemLayer, not from an item model), each a plain item: its blows and
     * moves are its own (PlayerArms), not a sword's.
     */
    private static final java.util.Map<com.hex.kagune.control.quinque.Arm, RegistryObject<Item>> ARM_CASES =
        new java.util.EnumMap<>(com.hex.kagune.control.quinque.Arm.class);
    private static final java.util.Map<com.hex.kagune.control.quinque.Arm, RegistryObject<Item>> ARM_WEAPONS =
        new java.util.EnumMap<>(com.hex.kagune.control.quinque.Arm.class);

    static {
        for (com.hex.kagune.control.quinque.Arm arm : com.hex.kagune.control.quinque.Arm.values()) {
            ARM_CASES.put(arm, ITEMS.register(arm.caseId(), () -> new Item(new Item.Properties().m_41487_(1))));
            ARM_WEAPONS.put(arm, ITEMS.register(arm.weaponId(), () -> new Item(new Item.Properties().m_41487_(1))));
        }
    }

    /** Home Living (1.3.40): the kitchen's dishes, by their item path, in the cookbook's order. */
    private static final java.util.Map<String, RegistryObject<Item>> DISHES = new java.util.LinkedHashMap<>();
    /** And its own effects, by key. */
    private static final DeferredRegister<net.minecraft.world.effect.MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,
        "hexkagune");
    private static final java.util.Map<String, RegistryObject<net.minecraft.world.effect.MobEffect>> KITCHEN_EFFECTS = new java.util.HashMap<>();
    /** 2.1.1: Exhaustion, the rest after a final kakuja (KingServer). */
    private static final RegistryObject<net.minecraft.world.effect.MobEffect> EXHAUSTION = EFFECTS.register("exhaustion", ExhaustionEffect::new);

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

    /** 2.1.1: the Exhaustion effect (the rest after a final kakuja). */
    public static net.minecraft.world.effect.MobEffect exhaustion() {
        return EXHAUSTION.get();
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

    /** 3.0.6: a kagune-made weapon's case item. */
    public static Item armCaseOf(com.hex.kagune.control.quinque.Arm arm) {
        return ARM_CASES.get(arm).get();
    }

    /** 3.0.6: a kagune-made weapon's weapon item. */
    public static Item armWeaponOf(com.hex.kagune.control.quinque.Arm arm) {
        return ARM_WEAPONS.get(arm).get();
    }

    /** 3.0.6: the kagune-made weapon {@code stack} is the case of, or null. */
    public static com.hex.kagune.control.quinque.Arm armCase(ItemStack stack) {
        return armIn(stack, ARM_CASES);
    }

    /** 3.0.6: the kagune-made weapon {@code stack} is (out of its case), or null. */
    public static com.hex.kagune.control.quinque.Arm armWeapon(ItemStack stack) {
        return armIn(stack, ARM_WEAPONS);
    }

    private static com.hex.kagune.control.quinque.Arm armIn(ItemStack stack,
                                                          java.util.Map<com.hex.kagune.control.quinque.Arm, RegistryObject<Item>> items) {
        if (stack == null || stack.m_41619_()) {
            return null;
        }
        Item item = stack.m_41720_();
        for (java.util.Map.Entry<com.hex.kagune.control.quinque.Arm, RegistryObject<Item>> e : items.entrySet()) {
            if (e.getValue().get() == item) {
                return e.getKey();
            }
        }
        return null;
    }

    /** 3.0.6: the stack's tag: a masterwork (a rank 8 or 9 investigator's: darker, harder, further). */
    public static final String MASTERWORK = "ArmMasterwork";

    /** 3.0.6: true if {@code stack} (an arm's case or weapon) is a masterwork. */
    public static boolean masterwork(ItemStack stack) {
        net.minecraft.nbt.CompoundTag tag = stack == null ? null : stack.m_41783_();
        return tag != null && tag.m_128471_(MASTERWORK);
    }

    /** 3.0.6: {@code arm}'s case, a masterwork or not. */
    public static ItemStack armCaseStack(com.hex.kagune.control.quinque.Arm arm, boolean masterwork) {
        ItemStack stack = new ItemStack(armCaseOf(arm), 1);
        if (masterwork) {
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.m_128379_(MASTERWORK, true);
            stack.m_41751_(tag);
        }
        return stack;
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
        TABS.register(modBus);
    }

    // ---------------------------------------------------------------- 4.0.2: the mod's own creative tabs

    /**
     * 4.0.2 (the owner: "we should have our own category in the creative menu with like the armors and quinques and whatever, everything
     * separated and well organized"): the mod's own tabs, each kind of thing in its own: HexKagune (Ghoul Meat, the ring, the cookbook
     * and the kitchen's dishes), Quinques (each case beside its weapon, then the investigators' kagune-made ones), Outfits (the Formal,
     * then the Crimson, piece by piece) and Masks (the owner's 24 in their sheet's order, then the generic ones).
     */
    private static final DeferredRegister<net.minecraft.world.item.CreativeModeTab> TABS = DeferredRegister.create(
        net.minecraft.core.registries.Registries.f_279569_, "hexkagune");

    public static final RegistryObject<net.minecraft.world.item.CreativeModeTab> TAB_KAGUNE = TABS.register("kagune", () ->
        tab("itemGroup.hexkagune.kagune", () -> new ItemStack(GHOUL_MEAT.get(), 1), (parameters, output) -> {
            output.m_246342_(new ItemStack(GHOUL_MEAT.get(), 1));
            output.m_246342_(new ItemStack(GOLD_RING.get(), 1));
            output.m_246342_(new ItemStack(COOKBOOK.get(), 1));
            for (RegistryObject<Item> dish : DISHES.values()) {
                output.m_246342_(new ItemStack(dish.get(), 1));
            }
        }));
    public static final RegistryObject<net.minecraft.world.item.CreativeModeTab> TAB_QUINQUES = TABS.register("quinques", () ->
        tab("itemGroup.hexkagune.quinques", () -> new ItemStack(WEAPONS.get(com.hex.kagune.control.quinque.Quinque.values()[0]).get(), 1), (parameters, output) -> {
            for (com.hex.kagune.control.quinque.Quinque q : com.hex.kagune.control.quinque.Quinque.values()) {
                output.m_246342_(new ItemStack(CASES.get(q).get(), 1));
                output.m_246342_(new ItemStack(WEAPONS.get(q).get(), 1));
            }
            for (com.hex.kagune.control.quinque.Arm arm : com.hex.kagune.control.quinque.Arm.values()) {
                output.m_246342_(new ItemStack(ARM_CASES.get(arm).get(), 1));
                output.m_246342_(new ItemStack(ARM_WEAPONS.get(arm).get(), 1));
            }
        }));
    public static final RegistryObject<net.minecraft.world.item.CreativeModeTab> TAB_OUTFITS = TABS.register("outfits", () ->
        tab("itemGroup.hexkagune.outfits", () -> outfit("formal_chestplate"), (parameters, output) -> {
            for (com.hex.kagune.control.outfit.Outfits.Piece piece : com.hex.kagune.control.outfit.Outfits.ALL) {
                if (!piece.mask()) {
                    output.m_246342_(outfit(piece.id()));
                }
            }
        }));
    public static final RegistryObject<net.minecraft.world.item.CreativeModeTab> TAB_MASKS = TABS.register("masks", () ->
        tab("itemGroup.hexkagune.masks", () -> outfit("mask_eyepatch"), (parameters, output) -> {
            for (com.hex.kagune.control.outfit.Outfits.Piece piece : com.hex.kagune.control.outfit.Outfits.ALL) {
                if (piece.mask()) {
                    output.m_246342_(outfit(piece.id()));
                }
            }
        }));

    private static net.minecraft.world.item.CreativeModeTab tab(String title, java.util.function.Supplier<ItemStack> icon,
                                                                net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator items) {
        return net.minecraft.world.item.CreativeModeTab.builder().m_257941_(net.minecraft.network.chat.Component.m_237115_(title)).m_257737_(icon)
            .m_257501_(items).m_257652_();
    }

    /**
     * 4.0.0 (The Great Exploration): the owner's outfits and masks as items (each one to a stack, iron's protection): the Formal Outfit,
     * the Crimson Outfit, their 24 masks and the generic masks ({@link com.hex.kagune.control.outfit.Outfits}), in the Combat tab.
     */
    private static final java.util.Map<String, RegistryObject<Item>> OUTFITS = new java.util.LinkedHashMap<>();

    static {
        for (com.hex.kagune.control.outfit.Outfits.Piece piece : com.hex.kagune.control.outfit.Outfits.ALL) {
            OUTFITS.put(piece.id(), ITEMS.register(piece.id(), () -> new com.hex.kagune.control.outfit.OutfitItem(piece, new Item.Properties().m_41487_(1))));
        }
    }

    /** 4.0.0: a fresh stack of the outfit piece or mask {@code id} (empty if there is none). */
    public static ItemStack outfit(String id) {
        RegistryObject<Item> item = id == null ? null : OUTFITS.get(id);
        return item == null ? ItemStack.f_41583_ : new ItemStack(item.get(), 1);
    }

    /** True for a stack of Ghoul Meat. */
    public static boolean ghoulMeat(ItemStack stack) {
        return stack != null && !stack.m_41619_() && stack.m_41720_() == GHOUL_MEAT.get();
    }
}
