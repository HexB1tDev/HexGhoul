package com.hex.kagune.control.kitchen;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

/**
 * Home Living (1.3.40): a dish of the ghoul kitchen as an item ({@link Dish}): food as vanilla's is (its nutrition and saturation on its
 * properties), drunk if it is a drink, eaten twice as fast if it is a snack. What it does once swallowed (its effects, its bowl or bottle
 * given back, the ghoul meat in it) is the server's ({@code KitchenServer}); its tooltip the client's ({@code KitchenClient}).
 */
public final class DishItem extends Item {
    private final Dish dish;

    public DishItem(Dish dish, Item.Properties properties) {
        super(properties);
        this.dish = dish;
    }

    public Dish dish() {
        return this.dish;
    }

    @Override
    public UseAnim m_6164_(ItemStack stack) {
        return this.dish.kind() == Dish.Kind.DRINK ? UseAnim.DRINK : UseAnim.EAT;
    }

    @Override
    public int m_8105_(ItemStack stack) {
        return this.dish.useTicks();
    }
}
