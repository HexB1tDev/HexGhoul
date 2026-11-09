package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.outfit.OutfitItem;
import com.hex.kagune.control.outfit.OutfitModels;
import com.hex.kagune.control.outfit.Outfits;
import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * 4.0.0 (The Great Exploration): NPCs in the owner's outfits, on the server. An investigator wears the whole Formal Outfit (the White
 * Reaper alone keeps his own look), an Aogiri member the whole Crimson Outfit, a wild ghoul now and then a generic mask. What they wear is
 * real armour (every client sees it as any mob's equipment), so its protection is taken off their own ({@link #armour}): they fight as
 * before. When one falls to a player, each piece it wears has its own small chance of being left ({@link Outfits#DROP_CHANCE}).
 */
public final class OutfitServer {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private OutfitServer() {
    }

    /** Puts every piece of {@code set} on {@code mob} where that slot is not already wearing one of the owner's pieces. */
    public static void dress(Mob mob, List<Outfits.Piece> set) {
        for (Outfits.Piece piece : set) {
            EquipmentSlot slot = OutfitItem.equipment(piece.slot());
            if (!(mob.m_6844_(slot).m_41720_() instanceof OutfitItem)) {
                mob.m_8061_(slot, ControlItems.outfit(piece.id()));
            }
            // Its drops are the mod's own (drop): vanilla's chance kept off.
            mob.m_21409_(slot, 0.0F);
        }
    }

    /** Takes every one of the owner's pieces off {@code mob} (the White Reaper keeps his own look). */
    public static void undress(Mob mob) {
        for (EquipmentSlot slot : SLOTS) {
            if (mob.m_6844_(slot).m_41720_() instanceof OutfitItem) {
                mob.m_8061_(slot, ItemStack.f_41583_);
            }
        }
    }

    /** Puts the mask {@code id} on {@code mob}'s head (if its head is bare or wears another mask). */
    public static void mask(Mob mob, String id) {
        ItemStack head = mob.m_6844_(EquipmentSlot.HEAD);
        if (id != null && (head.m_41619_() || head.m_41720_() instanceof OutfitItem item && item.piece().mask())) {
            if (!(head.m_41720_() instanceof OutfitItem worn) || !worn.piece().id().equals(id)) {
                mob.m_8061_(EquipmentSlot.HEAD, ControlItems.outfit(id));
            }
            mob.m_21409_(EquipmentSlot.HEAD, 0.0F);
        }
    }

    /** The armour the owner's pieces {@code wearer} has on give (iron's: cap or mask 2, coat 6, trousers 5, shoes 2). */
    public static double worn(LivingEntity wearer) {
        double total = 0.0;
        for (EquipmentSlot slot : SLOTS) {
            if (wearer.m_6844_(slot).m_41720_() instanceof OutfitItem item) {
                total += switch (item.piece().slot()) {
                    case OutfitModels.CHESTPLATE -> 6.0;
                    case OutfitModels.LEGGINGS -> 5.0;
                    default -> 2.0;
                };
            }
        }
        return total;
    }

    /** {@code own}, less what {@code wearer}'s pieces already give (never below none): set as its base, its armour stays {@code own}. */
    public static double armour(LivingEntity wearer, double own) {
        return Math.max(0.0, own - worn(wearer));
    }

    /**
     * {@code mob} fell ({@code byPlayer}: to a player's hand): each of the owner's pieces it wears is left with its own chance (a little more
     * with Looting), and taken off it.
     */
    public static void drop(Mob mob, int looting, boolean byPlayer) {
        if (!byPlayer) {
            return;
        }
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = mob.m_6844_(slot);
            if (stack.m_41720_() instanceof OutfitItem && mob.m_217043_().m_188500_() < Outfits.DROP_CHANCE + 0.01 * Math.max(0, looting)) {
                mob.m_19983_(stack.m_41777_());
                mob.m_8061_(slot, ItemStack.f_41583_);
            }
        }
    }
}
