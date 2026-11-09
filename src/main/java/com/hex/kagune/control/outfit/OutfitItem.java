package com.hex.kagune.control.outfit;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;

/**
 * 4.0.0 (The Great Exploration): an outfit piece or a mask as a real, wearable item: iron's protection and wear (the owner's own mod gives
 * them iron's, nothing more), worn in its slot by a player or an NPC. Its look is the owner's model, drawn on the wearer by
 * {@code OutfitLayer} (third person) and on a player's own arms by {@code OutfitFirstPerson}; vanilla's armour layer is given a clear
 * texture ({@link #getArmorTexture}) so nothing of iron armour shows over it.
 */
public final class OutfitItem extends ArmorItem {
    /** Vanilla's armour layer draws this (all clear): the model is drawn by the mod's own layer. */
    public static final String CLEAR = "hexkagune:textures/models/armor/outfit_clear.png";

    private final Outfits.Piece piece;

    public OutfitItem(Outfits.Piece piece, net.minecraft.world.item.Item.Properties properties) {
        super(ArmorMaterials.IRON, type(piece.slot()), properties);
        this.piece = piece;
    }

    /** What it is (its model, its slot). */
    public Outfits.Piece piece() {
        return this.piece;
    }

    /** Forge's armour texture (IForgeItem): clear, so vanilla's armour model draws nothing; the owner's model is drawn instead. */
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return CLEAR;
    }

    private static ArmorItem.Type type(int slot) {
        return switch (slot) {
            case OutfitModels.CHESTPLATE -> ArmorItem.Type.CHESTPLATE;
            case OutfitModels.LEGGINGS -> ArmorItem.Type.LEGGINGS;
            case OutfitModels.BOOTS -> ArmorItem.Type.BOOTS;
            default -> ArmorItem.Type.HELMET;
        };
    }

    /** The equipment slot a piece worn in {@code slot} takes. */
    public static EquipmentSlot equipment(int slot) {
        return switch (slot) {
            case OutfitModels.CHESTPLATE -> EquipmentSlot.CHEST;
            case OutfitModels.LEGGINGS -> EquipmentSlot.LEGS;
            case OutfitModels.BOOTS -> EquipmentSlot.FEET;
            default -> EquipmentSlot.HEAD;
        };
    }
}
