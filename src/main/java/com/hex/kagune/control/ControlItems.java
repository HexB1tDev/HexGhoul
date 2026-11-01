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

    /** 1.3.25: the quinques: each case (shut, one to a stack) and its weapon (out). */
    public static final RegistryObject<Item> QUINQUE_CASE_BIKAKU = ITEMS.register(com.hex.kagune.control.quinque.Quinque.BLADE.caseId,
        () -> new Item(new Item.Properties().m_41487_(1)));
    public static final RegistryObject<Item> QUINQUE_BIKAKU_BLADE = ITEMS.register(com.hex.kagune.control.quinque.Quinque.BLADE.weaponId,
        () -> new SwordItem(Tiers.NETHERITE, com.hex.kagune.control.quinque.Quinque.BLADE.damage, com.hex.kagune.control.quinque.Quinque.BLADE.speed,
            new Item.Properties()));
    public static final RegistryObject<Item> QUINQUE_CASE_KOUKAKU = ITEMS.register(com.hex.kagune.control.quinque.Quinque.SPEAR.caseId,
        () -> new Item(new Item.Properties().m_41487_(1)));
    public static final RegistryObject<Item> QUINQUE_KOUKAKU_SPEAR = ITEMS.register(com.hex.kagune.control.quinque.Quinque.SPEAR.weaponId,
        () -> new SwordItem(Tiers.NETHERITE, com.hex.kagune.control.quinque.Quinque.SPEAR.damage, com.hex.kagune.control.quinque.Quinque.SPEAR.speed,
            new Item.Properties()));

    private ControlItems() {
    }

    /** 1.3.25: a quinque's case item. */
    public static Item caseOf(com.hex.kagune.control.quinque.Quinque quinque) {
        return (quinque == com.hex.kagune.control.quinque.Quinque.BLADE ? QUINQUE_CASE_BIKAKU : QUINQUE_CASE_KOUKAKU).get();
    }

    /** 1.3.25: a quinque's weapon item. */
    public static Item weaponOf(com.hex.kagune.control.quinque.Quinque quinque) {
        return (quinque == com.hex.kagune.control.quinque.Quinque.BLADE ? QUINQUE_BIKAKU_BLADE : QUINQUE_KOUKAKU_SPEAR).get();
    }

    /** 1.3.25: the quinque {@code stack} is the case of, or null. */
    public static com.hex.kagune.control.quinque.Quinque quinqueCase(ItemStack stack) {
        if (stack == null || stack.m_41619_()) {
            return null;
        }
        Item item = stack.m_41720_();
        return item == QUINQUE_CASE_BIKAKU.get() ? com.hex.kagune.control.quinque.Quinque.BLADE
            : item == QUINQUE_CASE_KOUKAKU.get() ? com.hex.kagune.control.quinque.Quinque.SPEAR : null;
    }

    /** 1.3.25: the quinque {@code stack} is the weapon of (open), or null. */
    public static com.hex.kagune.control.quinque.Quinque quinqueWeapon(ItemStack stack) {
        if (stack == null || stack.m_41619_()) {
            return null;
        }
        Item item = stack.m_41720_();
        return item == QUINQUE_BIKAKU_BLADE.get() ? com.hex.kagune.control.quinque.Quinque.BLADE
            : item == QUINQUE_KOUKAKU_SPEAR.get() ? com.hex.kagune.control.quinque.Quinque.SPEAR : null;
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
                event.accept(QUINQUE_CASE_BIKAKU);
                event.accept(QUINQUE_BIKAKU_BLADE);
                event.accept(QUINQUE_CASE_KOUKAKU);
                event.accept(QUINQUE_KOUKAKU_SPEAR);
            }
        }
    }
}
