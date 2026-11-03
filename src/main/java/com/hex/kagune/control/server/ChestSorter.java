package com.hex.kagune.control.server;

import com.hex.kagune.control.family.ChestSort;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 2.0.9: the spouse sorting the home's chests by kind ({@link ChestSort}'s rules): each time it tidies a chest, the home's chests are given
 * their kinds (a chest it gives one is named for it, as an anvil would: "Food", "Blocks", "Whatever"...), and everything in that chest that
 * belongs in another is taken there (each stack whole, where it fits; what doesn't fit stays). A chest the player named anything else is
 * theirs: nothing is taken from it or put in it. A double chest is one chest (both halves named alike). Names are read and written in the
 * chest's saved data (its {@code CustomName}).
 */
final class ChestSorter {
    /** A chest of the home: its halves (one, or a double chest's two), and what its name makes it. */
    private record Unit(List<BlockPos> halves, ChestSort.Box box) {
    }

    private ChestSorter() {
    }

    /** At {@code at} (one of {@code chests}): the home's chests given kinds and named, and what in it belongs elsewhere taken there. True if anything changed. */
    static boolean organize(ServerLevel level, List<BlockPos> chests, BlockPos at) {
        List<Unit> units = units(level, chests);
        Map<ChestSort.Kind, Integer> counts = new EnumMap<>(ChestSort.Kind.class);
        for (Unit unit : units) {
            if (unit.box().theirs()) {
                continue;
            }
            for (BlockPos half : unit.halves()) {
                Container box = container(level, half);
                for (int i = 0; box != null && i < box.m_6643_(); i++) {
                    ItemStack stack = box.m_8020_(i);
                    if (!stack.m_41619_()) {
                        ChestSort.Kind kind = kind(stack);
                        if (kind != unit.box().kind()) {
                            counts.merge(kind, stack.m_41613_(), Integer::sum);
                        }
                    }
                }
            }
        }
        List<ChestSort.Box> boxes = new ArrayList<>();
        for (Unit unit : units) {
            boxes.add(unit.box());
        }
        List<ChestSort.Kind> kinds = ChestSort.assign(boxes, counts);
        boolean changed = false;
        int here = -1;
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            if (kinds.get(i) != null && kinds.get(i) != unit.box().kind()) {
                for (BlockPos half : unit.halves()) {
                    name(level, half, kinds.get(i).label);
                }
                changed = true;
            }
            if (unit.halves().contains(at)) {
                here = i;
            }
        }
        if (here < 0 || units.get(here).box().theirs()) {
            return changed;
        }
        ChestSort.Kind hereKind = kinds.get(here);
        for (BlockPos half : units.get(here).halves()) {
            Container box = container(level, half);
            boolean moved = false;
            for (int i = 0; box != null && i < box.m_6643_(); i++) {
                ItemStack stack = box.m_8020_(i);
                if (stack.m_41619_()) {
                    continue;
                }
                ChestSort.Kind kind = kind(stack);
                int to = kind == hereKind ? -1 : ChestSort.target(kinds, kind);
                if (to < 0 || to == here || units.get(to).box().theirs()) {
                    continue;
                }
                for (BlockPos destination : units.get(to).halves()) {
                    Container into = container(level, destination);
                    if (into != null && HomeChores.insert(into, stack.m_41777_())) {
                        box.m_6836_(i, ItemStack.f_41583_);
                        into.m_6596_();
                        moved = true;
                        break;
                    }
                }
            }
            if (moved) {
                box.m_6596_();
                changed = true;
            }
        }
        return changed;
    }

    /** The home's chests as units: a double chest's halves together, each with what its name makes it. */
    private static List<Unit> units(ServerLevel level, List<BlockPos> chests) {
        List<Unit> out = new ArrayList<>();
        List<BlockPos> taken = new ArrayList<>();
        for (BlockPos at : chests) {
            if (taken.contains(at)) {
                continue;
            }
            List<BlockPos> halves = new ArrayList<>(List.of(at));
            String state = String.valueOf(level.m_8055_(at));
            String half = ChestSort.half(state);
            if (!half.isEmpty()) {
                String facing = facing(state);
                for (BlockPos other : chests) {
                    if (other.equals(at) || taken.contains(other) || other.m_123342_() != at.m_123342_()
                        || Math.abs(other.m_123341_() - at.m_123341_()) + Math.abs(other.m_123343_() - at.m_123343_()) != 1) {
                        continue;
                    }
                    String otherState = String.valueOf(level.m_8055_(other));
                    String otherHalf = ChestSort.half(otherState);
                    if (!otherHalf.isEmpty() && !otherHalf.equals(half) && facing(otherState).equals(facing)) {
                        halves.add(other);
                        break;
                    }
                }
            }
            taken.addAll(halves);
            String name = "";
            for (BlockPos each : halves) {
                if (name.isEmpty()) {
                    name = name(level, each);
                }
            }
            out.add(new Unit(halves, ChestSort.box(name)));
        }
        return out;
    }

    private static String facing(String state) {
        int i = state.indexOf("facing=");
        if (i < 0) {
            return "";
        }
        int end = state.indexOf(',', i);
        int close = state.indexOf(']', i);
        int stop = end < 0 ? close : close < 0 ? end : Math.min(end, close);
        return stop < 0 ? state.substring(i) : state.substring(i, stop);
    }

    private static Container container(ServerLevel level, BlockPos at) {
        return level.m_46805_(at) && level.m_7702_(at) instanceof Container box ? box : null;
    }

    /** Its custom name as words (empty: none). */
    private static String name(ServerLevel level, BlockPos at) {
        BlockEntity entity = level.m_46805_(at) ? level.m_7702_(at) : null;
        if (entity == null) {
            return "";
        }
        CompoundTag tag = entity.m_187480_();
        return tag.m_128441_("CustomName") ? ChestSort.nameOf(tag.m_128461_("CustomName")) : "";
    }

    /** Named {@code label} (as an anvil names it), through its saved data. */
    private static void name(ServerLevel level, BlockPos at, String label) {
        BlockEntity entity = level.m_46805_(at) ? level.m_7702_(at) : null;
        if (entity == null) {
            return;
        }
        CompoundTag tag = entity.m_187480_();
        tag.m_128359_("CustomName", ChestSort.saved(label));
        entity.m_142466_(tag);
        entity.m_6596_();
    }

    /** What kind {@code stack} is: by its id, whether it is eaten (or ghoul meat), whether it places a block. */
    static ChestSort.Kind kind(ItemStack stack) {
        Object id = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
        boolean food = stack.m_41614_() || com.hex.kagune.control.ControlItems.ghoulMeat(stack);
        return ChestSort.kind(String.valueOf(id), food, blockItem(stack.m_41720_()));
    }

    /** True if {@code item} places a block (a BlockItem, any mod's; by its classes' names, so no new game API is touched). */
    private static boolean blockItem(Object item) {
        for (Class<?> type = item.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getSimpleName().equals("BlockItem")) {
                return true;
            }
        }
        return false;
    }
}
