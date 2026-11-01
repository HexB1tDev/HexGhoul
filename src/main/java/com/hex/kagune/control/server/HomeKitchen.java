package com.hex.kagune.control.server;

import com.hex.kagune.control.family.Chores;
import com.hex.kagune.control.family.Kitchen;
import com.hex.kagune.control.kitchen.Dish;
import com.hex.kagune.control.kitchen.Dishes;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.data.KaguneProfile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 1.3.31: the spouse's real help at home ({@link Kitchen}'s rules): it cooks the raw food it finds in the home's chests at the stove (a
 * furnace or a smoker burning fuel from the chests, or a campfire), keeping a meal for its spouse (for a ghoul, the raw cuts set aside:
 * cooked food won't stay down) and putting the rest away; it smelts the ores, sand and cobblestone left in the chests; it picks the ripe
 * crops and leaves the plants to grow again, putting what they give away. Everything is taken out of the chests and put back in one go
 * (nothing is ever lost or made twice: what can't be put away is never taken). The meal is kept in its saved data and handed over when its
 * spouse comes home ({@link #serve}).
 *
 * <p>1.3.42: and for a ghoul spouse it cooks the cookbook's dishes ({@link #dinner}, {@link #cookDish}; which one is {@link Kitchen#choose}):
 * when the chests hold all a recipe needs (or what it needs first: the mince, the bottled blood, a seared cut), it takes it all out, cooks
 * at the stove (a table, or by the chest, when nothing needs searing) with the same stirring as ever, and keeps the dish as their meal or
 * puts it in the chest; a few a day, a while apart.
 */
final class HomeKitchen {
    /** Its saved data's keys: the meal kept for its spouse; whether its spouse was a ghoul when last seen (1 yes). */
    static final String MEAL = "HexMeal";
    static final String GHOUL = "HexMealGhoul";
    /**
     * 1.3.42: the cookbook's dishes: the last one it made; whether that took the last of something another recipe needs (1 yes); the
     * day it counts and how many it made that day; when it made the last one; the last one put in the chest with its spouse away (told
     * when they come home).
     */
    static final String DISH_LAST = "HexDishLast";
    static final String DISH_EMPTIED = "HexDishEmptied";
    static final String DISH_DAY = "HexDishDay";
    static final String DISH_COUNT = "HexDishCount";
    static final String DISH_AT = "HexDishAt";
    static final String DISH_NEWS = "HexDishNews";

    /** 1.3.42: a dish of the cookbook it could make now: what and how, the chest it fetches from first, where it cooks. */
    record Dinner(Kitchen.Cooking cooking, BlockPos chest, BlockPos at) {
    }

    /** 1.3.42: a dish made: how, how many it kept for its spouse, how many went in the chest. */
    record Made(Kitchen.Cooking cooking, int kept, int stored) {
    }

    /** What it could do about the house now: the chest to fetch from and the station to cook at (or null); the same for smelting. */
    record Plan(BlockPos cookChest, BlockPos cookAt, BlockPos smeltChest, BlockPos smeltAt, boolean noFuel) {
        static final Plan NONE = new Plan(null, null, null, null, false);
    }

    private HomeKitchen() {
    }

    // ---------------------------------------------------------------- what it could do

    /** True if its spouse is a ghoul (as last seen: kept for when they're away), refreshed while they're about. */
    static boolean ghoulSpouse(EtoNpc npc, ServerPlayer partner) {
        CompoundTag tag = npc.getPersistentData();
        if (partner != null) {
            boolean ghoul = KaguneProfile.isUnlocked(partner);
            tag.m_128405_(GHOUL, ghoul ? 1 : 0);
            return ghoul;
        }
        return tag.m_128451_(GHOUL) == 1;
    }

    /** The home's chests that really hold things (plain chests, barrels), loaded. */
    static List<BlockPos> chests(ServerLevel level, HomeChores.Found found) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos at : found.blocks().getOrDefault(Chores.Kind.TIDY, List.of())) {
            if (level.m_46805_(at) && Chores.sortable(String.valueOf(level.m_8055_(at).m_60734_())) && level.m_7702_(at) instanceof Container) {
                out.add(at);
            }
        }
        return out;
    }

    /** What it could cook or smelt now, from what the chests hold and the stations the home has. */
    static Plan plan(ServerLevel level, HomeChores.Found found, List<BlockPos> chests, boolean ghoul, int mealRoom) {
        List<BlockPos> stations = found.blocks().getOrDefault(Chores.Kind.COOK, List.of());
        BlockPos cookChest = null;
        BlockPos cookAt = null;
        BlockPos smeltChest = null;
        BlockPos smeltAt = null;
        boolean noFuel = false;
        int fuel = fuel(level, chests);
        if (ghoul) {
            // A ghoul's meal: raw cuts set aside (prepared at a table, or at the stove, or by the chest itself).
            BlockPos flesh = mealRoom > 0 ? holding(level, chests, id -> Kitchen.flesh(id)) : null;
            if (flesh != null) {
                cookChest = flesh;
                List<BlockPos> tables = found.blocks().getOrDefault(Chores.Kind.WIPE, List.of());
                cookAt = !tables.isEmpty() ? nearest(tables, flesh) : !stations.isEmpty() ? nearest(stations, flesh) : flesh;
            }
        } else {
            BlockPos raw = holding(level, chests, id -> Kitchen.cooked(id) != null);
            if (raw != null) {
                BlockPos campfire = nearestWhere(level, stations, raw, text -> Kitchen.station(text, false) && Kitchen.free(text));
                BlockPos stove = nearestWhere(level, stations, raw, text -> Kitchen.station(text, false));
                BlockPos at = campfire != null ? campfire : fuel >= Kitchen.PER_ITEM ? stove : null;
                if (at != null) {
                    cookChest = raw;
                    cookAt = at;
                } else if (stove != null) {
                    noFuel = true;
                }
            }
        }
        BlockPos ore = fuel >= Kitchen.PER_ITEM ? holding(level, chests, id -> Kitchen.smelted(id) != null) : null;
        if (ore != null) {
            BlockPos furnace = nearestWhere(level, stations, ore, text -> Kitchen.station(text, true));
            if (furnace != null) {
                smeltChest = ore;
                smeltAt = furnace;
            }
        } else if (fuel < Kitchen.PER_ITEM && holding(level, chests, id -> Kitchen.smelted(id) != null) != null
            && nearestWhere(level, stations, chests.isEmpty() ? null : chests.get(0), text -> Kitchen.station(text, true)) != null) {
            noFuel = true;
        }
        return new Plan(cookChest, cookAt, smeltChest, smeltAt, noFuel);
    }

    /** The ripe crops of the home (the nearest first, up to {@code most}). */
    static List<BlockPos> ripe(ServerLevel level, HomeChores.Found found, int most) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos at : found.blocks().getOrDefault(Chores.Kind.WATER, List.of())) {
            if (out.size() >= most) {
                break;
            }
            if (level.m_46805_(at)) {
                BlockState state = level.m_8055_(at);
                if (Kitchen.ripe(state.toString())) {
                    out.add(at);
                }
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- doing it

    /**
     * At the stove (or the table, for a ghoul's meal): a batch of raw food from the chests cooked (fuel taken from them for a furnace or a
     * smoker), a meal kept for its spouse, the rest put away. True if anything was made.
     */
    static boolean cook(EtoNpc npc, ServerLevel level, List<BlockPos> chests, BlockPos at, boolean ghoul) {
        List<Chores.Pile> meal = meal(npc);
        if (ghoul) {
            int room = Kitchen.MEAL - Kitchen.portions(meal);
            List<ItemStack> cuts = take(level, chests, id -> Kitchen.flesh(id), room);
            if (cuts.isEmpty()) {
                return false;
            }
            for (ItemStack cut : cuts) {
                int[] took = new int[1];
                meal = Kitchen.add(meal, id(cut), cut.m_41613_(), took);
            }
            meal(npc, meal);
            return true;
        }
        String station = String.valueOf(level.m_8055_(at).m_60734_());
        boolean free = Kitchen.free(station);
        int fuel = free ? Integer.MAX_VALUE : fuel(level, chests);
        int most = Math.min(Kitchen.BATCH, fuel / Kitchen.PER_ITEM);
        if (most <= 0) {
            return false;
        }
        List<ItemStack> raw = take(level, chests, id -> Kitchen.cooked(id) != null, most);
        if (!free) {
            raw = trim(npc, level, chests, raw, fuel(level, chests) / Kitchen.PER_ITEM);
        }
        if (raw.isEmpty()) {
            return false;
        }
        int count = 0;
        List<ItemStack> made = new ArrayList<>();
        for (ItemStack stack : raw) {
            count += stack.m_41613_();
            ItemStack out = stack(Kitchen.cooked(id(stack)), stack.m_41613_());
            if (out == null) {
                giveBack(npc, level, chests, List.of(stack));
                continue;
            }
            made.add(out);
        }
        if (!free) {
            burn(level, chests, count * Kitchen.PER_ITEM);
        }
        // A meal kept for its spouse first, the rest put away.
        List<ItemStack> rest = new ArrayList<>();
        for (ItemStack out : made) {
            int[] took = new int[1];
            meal = Kitchen.add(meal, id(out), out.m_41613_(), took);
            if (took[0] < out.m_41613_()) {
                ItemStack left = out.m_41777_();
                left.m_41764_(out.m_41613_() - took[0]);
                rest.add(left);
            }
        }
        meal(npc, meal);
        giveBack(npc, level, chests, rest);
        smoke(level, at);
        return !made.isEmpty();
    }

    /** At the furnace: a batch of one kind of ore (or sand, cobblestone, logs) from the chests smelted with their fuel, put away. */
    static boolean smelt(EtoNpc npc, ServerLevel level, List<BlockPos> chests, BlockPos at) {
        int fuel = fuel(level, chests);
        int most = Math.min(Kitchen.BATCH * 2, fuel / Kitchen.PER_ITEM);
        if (most <= 0) {
            return false;
        }
        String[] kind = new String[1];
        List<ItemStack> ore = take(level, chests, id -> Kitchen.smelted(id) != null && (kind[0] == null || kind[0].equals(id)), most, kind);
        if (ore.isEmpty()) {
            return false;
        }
        // What is left to burn once these are out (a log smelted is no longer there to burn): any more than it covers goes back.
        ore = trim(npc, level, chests, ore, fuel(level, chests) / Kitchen.PER_ITEM);
        int count = 0;
        for (ItemStack stack : ore) {
            count += stack.m_41613_();
        }
        if (count <= 0) {
            return false;
        }
        ItemStack out = stack(Kitchen.smelted(kind[0]), count);
        if (out == null) {
            giveBack(npc, level, chests, ore);
            return false;
        }
        burn(level, chests, count * Kitchen.PER_ITEM);
        List<ItemStack> outs = new ArrayList<>();
        int max = Math.max(1, out.m_41741_());
        while (count > 0) {
            ItemStack part = out.m_41777_();
            part.m_41764_(Math.min(max, count));
            count -= part.m_41613_();
            outs.add(part);
        }
        giveBack(npc, level, chests, outs);
        smoke(level, at);
        return true;
    }

    /** A ripe crop picked: what it gives put away (none is picked if it can't be), the plant left to grow again. True if picked. */
    static boolean harvest(EtoNpc npc, ServerLevel level, List<BlockPos> chests, BlockPos crop, double roll, double roll2) {
        if (!level.m_46805_(crop)) {
            return false;
        }
        BlockState state = level.m_8055_(crop);
        if (!Kitchen.ripe(state.toString())) {
            return false;
        }
        List<ItemStack> gives = new ArrayList<>();
        for (Chores.Pile pile : Kitchen.harvest(String.valueOf(state.m_60734_()), roll, roll2)) {
            ItemStack stack = stack(pile.key(), pile.count());
            if (stack != null) {
                gives.add(stack);
            }
        }
        if (gives.isEmpty() || !fits(level, chests, gives)) {
            return false;
        }
        giveBack(npc, level, chests, gives);
        level.m_7731_(crop, state.m_60734_().m_49966_(), 3);
        level.m_8767_(ParticleTypes.f_123748_, crop.m_123341_() + 0.5, crop.m_123342_() + 0.6, crop.m_123343_() + 0.5, 4, 0.3, 0.2, 0.3, 0.0);
        return true;
    }

    /**
     * Its spouse is home: the meal it kept handed to them (into their inventory, or at their feet if it's full), and what it says as it
     * does; null if there was none. 1.3.42: a dish of the cookbook in it is what it talks about; with no meal kept, a dish it put in
     * the chest while they were out is.
     */
    static String serve(EtoNpc npc, ServerPlayer partner, ServerLevel level) {
        String news = npc.getPersistentData().m_128461_(DISH_NEWS);
        npc.getPersistentData().m_128359_(DISH_NEWS, "");
        String key = npc.persona().key();
        int seed = npc.m_217043_().m_188503_(1000);
        String main = hand(npc, partner);
        if (main == null) {
            return news.isEmpty() || Dishes.byItem(news) == null ? null : Kitchen.madeWhileOut(key, Kitchen.name(news), seed);
        }
        if (Dishes.byItem(main) != null) {
            return Kitchen.servedDish(key, Kitchen.name(main), !news.isEmpty(), seed);
        }
        return Kitchen.served(key, ghoulSpouse(npc, partner), Kitchen.name(main), seed);
    }

    /**
     * The meal it kept handed to its spouse (into their inventory, or at their feet if it's full); what the main thing in it was (a dish
     * of the cookbook before anything else, else the most of one thing), or null if it kept none.
     */
    static String hand(EtoNpc npc, ServerPlayer partner) {
        List<Chores.Pile> meal = meal(npc);
        if (meal.isEmpty()) {
            return null;
        }
        Chores.Pile main = meal.get(0);
        for (Chores.Pile pile : meal) {
            boolean dish = Dishes.byItem(pile.key()) != null;
            boolean mainDish = Dishes.byItem(main.key()) != null;
            if (dish && !mainDish || dish == mainDish && pile.count() > main.count()) {
                main = pile;
            }
        }
        for (Chores.Pile pile : meal) {
            for (ItemStack stack : stacks(pile.key(), pile.count())) {
                if (!partner.m_36356_(stack)) {
                    partner.m_36176_(stack, false);
                }
            }
        }
        meal(npc, List.of());
        return main.key();
    }

    // ---------------------------------------------------------------- 1.3.42: the cookbook's dishes

    /** What the chests hold, item by item (plain stacks: nothing named or enchanted, as {@link #take} takes). */
    static Map<String, Integer> stock(ServerLevel level, List<BlockPos> chests) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (BlockPos at : chests) {
            if (level.m_7702_(at) instanceof Container box) {
                for (int i = 0; i < box.m_6643_(); i++) {
                    ItemStack stack = box.m_8020_(i);
                    if (!stack.m_41619_() && stack.m_41783_() == null) {
                        out.merge(id(stack), stack.m_41613_(), Integer::sum);
                    }
                }
            }
        }
        return out;
    }

    /** How many things it could sear at {@code at}: any number at a campfire, what the chests' fuel covers at a furnace or smoker, else none. */
    static int searable(ServerLevel level, BlockPos at, List<BlockPos> chests) {
        if (at == null || !level.m_46805_(at)) {
            return 0;
        }
        String text = String.valueOf(level.m_8055_(at).m_60734_());
        if (!Kitchen.station(text, false)) {
            return 0;
        }
        return Kitchen.free(text) ? Integer.MAX_VALUE : fuel(level, chests) / Kitchen.PER_ITEM;
    }

    /** True if it may make another of the cookbook's dishes now (a few a day, a while apart: {@link Kitchen#dishDue}). */
    static boolean dishDue(EtoNpc npc, ServerLevel level, long now) {
        CompoundTag tag = npc.getPersistentData();
        long day = Math.floorDiv(level.m_46468_(), 24000L);
        int today = tag.m_128454_(DISH_DAY) == day ? tag.m_128451_(DISH_COUNT) : 0;
        long at = tag.m_128454_(DISH_AT);
        return Kitchen.dishDue(today, at == 0L ? Long.MIN_VALUE : at, now);
    }

    /**
     * A dish of the cookbook it could make now for its spouse (a ghoul: a human would be sick on ghoul meat, and it won't feed them a
     * person), or null: which ({@link Kitchen#choose}), the chest it fetches from, and where it cooks: at the stove if something needs
     * searing (a campfire, or a furnace or smoker the chests have fuel for), else at the stove anyway, or a table, or by the chest.
     */
    static Dinner dinner(EtoNpc npc, ServerLevel level, HomeChores.Found found, List<BlockPos> chests, boolean ghoul, long now) {
        if (!ghoul || chests.isEmpty() || !dishDue(npc, level, now)) {
            return null;
        }
        List<BlockPos> stations = found.blocks().getOrDefault(Chores.Kind.COOK, List.of());
        BlockPos from = chests.get(0);
        BlockPos campfire = nearestWhere(level, stations, from, text -> Kitchen.station(text, false) && Kitchen.free(text));
        BlockPos stove = nearestWhere(level, stations, from, text -> Kitchen.station(text, false));
        BlockPos sears = campfire != null ? campfire : stove;
        int searable = sears == null ? 0 : searable(level, sears, chests);
        Map<String, Integer> stock = stock(level, chests);
        CompoundTag tag = npc.getPersistentData();
        String last = tag.m_128461_(DISH_LAST).isEmpty() ? null : tag.m_128461_(DISH_LAST);
        boolean emptiedLast = tag.m_128451_(DISH_EMPTIED) == 1;
        if (Kitchen.passes(stock, searable, ghoul, last, emptiedLast)) {
            // Every dish it could make would take the last of something again: this meal's turn passes; the next one may.
            tag.m_128405_(DISH_EMPTIED, 0);
            tag.m_128356_(DISH_AT, now);
            return null;
        }
        Kitchen.Cooking cooking = Kitchen.choose(stock, searable, ghoul, last, emptiedLast, npc.m_217043_().m_188500_());
        if (cooking == null) {
            return null;
        }
        BlockPos chest = holding(level, chests, cooking.uses()::containsKey);
        if (chest == null) {
            chest = from;
        }
        BlockPos at;
        if (cooking.seared() > 0) {
            at = sears;
        } else {
            List<BlockPos> tables = found.blocks().getOrDefault(Chores.Kind.WIPE, List.of());
            at = stove != null ? nearestWhere(level, stations, chest, text -> Kitchen.station(text, false)) : !tables.isEmpty() ? nearest(tables, chest) : chest;
        }
        return at == null ? null : new Dinner(cooking, chest, at);
    }

    /** The cookbook's dishes it could make from the chests now (their names), for {@code /kagune home}; never more than {@code most}. */
    static List<String> possible(ServerLevel level, HomeChores.Found found, List<BlockPos> chests, boolean ghoul, int most) {
        List<String> out = new ArrayList<>();
        if (!ghoul || chests.isEmpty()) {
            return out;
        }
        List<BlockPos> stations = found.blocks().getOrDefault(Chores.Kind.COOK, List.of());
        BlockPos campfire = nearestWhere(level, stations, chests.get(0), text -> Kitchen.station(text, false) && Kitchen.free(text));
        BlockPos stove = campfire != null ? campfire : nearestWhere(level, stations, chests.get(0), text -> Kitchen.station(text, false));
        int searable = stove == null ? 0 : searable(level, stove, chests);
        Map<String, Integer> stock = stock(level, chests);
        for (Dish dish : com.hex.kagune.control.kitchen.Menu.DISHES) {
            if (out.size() < most && Kitchen.cooks(dish, true) && Kitchen.cooking(dish, stock, searable) != null) {
                out.add(Dishes.name(dish.id()));
            }
        }
        return out;
    }

    /** How many of the cookbook's dishes it has made today. */
    static int dishesToday(EtoNpc npc, ServerLevel level) {
        CompoundTag tag = npc.getPersistentData();
        return tag.m_128454_(DISH_DAY) == Math.floorDiv(level.m_46468_(), 24000L) ? tag.m_128451_(DISH_COUNT) : 0;
    }

    /**
     * At the stove (or the table, or the chest): the dish {@code dishId} made for real from what the chests hold now (if they no longer
     * hold enough for it, another is chosen, or none): every ingredient taken out at once (any short, all of it put back), the fuel for
     * what was seared burnt, the buckets and bottles put back, the dish kept as its spouse's meal as far as there is room ({@link
     * Kitchen#MEAL}), the rest and any prep left over put in the chest. Null if nothing was made.
     */
    static Made cookDish(EtoNpc npc, ServerLevel level, List<BlockPos> chests, BlockPos at, boolean ghoul, String dishId, ServerPlayer partner, long now) {
        if (!ghoul || chests.isEmpty()) {
            return null;
        }
        CompoundTag tag = npc.getPersistentData();
        Map<String, Integer> stock = stock(level, chests);
        int searable = searable(level, at, chests);
        boolean emptiedLast = tag.m_128451_(DISH_EMPTIED) == 1;
        Kitchen.Cooking cooking = Kitchen.cooking(Dishes.byId(dishId), stock, searable);
        if (cooking == null || emptiedLast && cooking.empties(stock)) {
            String last = tag.m_128461_(DISH_LAST);
            cooking = Kitchen.choose(stock, searable, ghoul, last.isEmpty() ? null : last, emptiedLast, npc.m_217043_().m_188500_());
        }
        if (cooking == null) {
            return null;
        }
        // Everything out of the chests at once: if anything is short after all, it all goes back and nothing is made.
        List<ItemStack> taken = new ArrayList<>();
        boolean short_ = false;
        for (Map.Entry<String, Integer> use : cooking.uses().entrySet()) {
            String want = use.getKey();
            List<ItemStack> got = take(level, chests, want::equals, use.getValue());
            int n = 0;
            for (ItemStack stack : got) {
                n += stack.m_41613_();
            }
            taken.addAll(got);
            if (n < use.getValue()) {
                short_ = true;
                break;
            }
        }
        boolean free = cooking.seared() == 0 || Kitchen.free(String.valueOf(level.m_8055_(at).m_60734_()));
        if (!short_ && !free && fuel(level, chests) < cooking.seared() * Kitchen.PER_ITEM) {
            short_ = true; // what it took was the fuel (sticks in a kebab): not enough left to burn
        }
        if (short_) {
            giveBack(npc, level, chests, taken);
            return null;
        }
        if (!free) {
            burn(level, chests, cooking.seared() * Kitchen.PER_ITEM);
        }
        // The dish kept as its spouse's meal first, as far as there's room; the rest, the prep left over, the buckets and bottles away.
        List<Chores.Pile> meal = meal(npc);
        int kept = 0;
        int stored = 0;
        List<ItemStack> away = new ArrayList<>();
        for (Map.Entry<String, Integer> out : cooking.yields().entrySet()) {
            int count = out.getValue();
            if (out.getKey().equals(cooking.item())) {
                int[] took = new int[1];
                meal = Kitchen.add(meal, out.getKey(), count, took);
                kept = took[0];
                count -= kept;
                stored = count;
            }
            away.addAll(stacks(out.getKey(), count));
        }
        meal(npc, meal);
        giveBack(npc, level, chests, away);
        // What it remembers: what it made, whether that took the last of something, how many today, when.
        long day = Math.floorDiv(level.m_46468_(), 24000L);
        int today = tag.m_128454_(DISH_DAY) == day ? tag.m_128451_(DISH_COUNT) : 0;
        tag.m_128359_(DISH_LAST, cooking.dish().id());
        tag.m_128405_(DISH_EMPTIED, cooking.empties(stock) ? 1 : 0);
        tag.m_128356_(DISH_DAY, day);
        tag.m_128405_(DISH_COUNT, today + 1);
        tag.m_128356_(DISH_AT, now);
        boolean there = partner != null && SceneServer.sees(npc, partner, 12.0);
        if (stored > 0 && !there) {
            tag.m_128359_(DISH_NEWS, cooking.item());
        }
        if (Kitchen.station(String.valueOf(level.m_8055_(at).m_60734_()), false)) {
            smoke(level, at);
        } else {
            level.m_8767_(ParticleTypes.f_123748_, at.m_123341_() + 0.5, at.m_123342_() + 1.1, at.m_123343_() + 0.5, 5, 0.3, 0.1, 0.3, 0.0);
        }
        return new Made(cooking, kept, stored);
    }

    /** The meal it keeps for its spouse now. */
    static List<Chores.Pile> meal(EtoNpc npc) {
        return Kitchen.load(npc.getPersistentData().m_128461_(MEAL));
    }

    private static void meal(EtoNpc npc, List<Chores.Pile> meal) {
        npc.getPersistentData().m_128359_(MEAL, Kitchen.save(meal));
    }

    // ---------------------------------------------------------------- the chests

    static String id(ItemStack stack) {
        return String.valueOf(ForgeRegistries.ITEMS.getKey(stack.m_41720_()));
    }

    /** A stack of {@code count} of the item named {@code id} ("minecraft:...", "hexkagune:..."), or null if there's no such item. */
    static ItemStack stack(String id, int count) {
        if (id == null || count <= 0) {
            return null;
        }
        int colon = id.indexOf(':');
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.m_214293_(colon < 0 ? "minecraft" : id.substring(0, colon), id.substring(colon + 1)));
        if (item == null || String.valueOf(ForgeRegistries.ITEMS.getKey(item)).equals("minecraft:air")) {
            return null;
        }
        return new ItemStack(item, count);
    }

    /** The first chest holding something {@code wanted}, or null. */
    static BlockPos holding(ServerLevel level, List<BlockPos> chests, Predicate<String> wanted) {
        for (BlockPos at : chests) {
            if (level.m_7702_(at) instanceof Container box) {
                for (int i = 0; i < box.m_6643_(); i++) {
                    ItemStack stack = box.m_8020_(i);
                    if (!stack.m_41619_() && stack.m_41783_() == null && wanted.test(id(stack))) {
                        return at;
                    }
                }
            }
        }
        return null;
    }

    /** How much fuel the chests hold, in halves ({@link Kitchen#fuel}). */
    static int fuel(ServerLevel level, List<BlockPos> chests) {
        long total = 0L;
        for (BlockPos at : chests) {
            if (level.m_7702_(at) instanceof Container box) {
                for (int i = 0; i < box.m_6643_(); i++) {
                    ItemStack stack = box.m_8020_(i);
                    if (!stack.m_41619_()) {
                        total += (long)Kitchen.fuel(id(stack)) * stack.m_41613_();
                    }
                }
            }
        }
        return (int)Math.min(Integer.MAX_VALUE, total);
    }

    /** {@code count} of the item named {@code id} as whole stacks (none past the item's own limit), or none if there's no such item. */
    static List<ItemStack> stacks(String id, int count) {
        List<ItemStack> out = new ArrayList<>();
        ItemStack one = stack(id, 1);
        if (one == null) {
            return out;
        }
        int max = Math.max(1, one.m_41741_());
        for (int left = count; left > 0; left -= max) {
            ItemStack part = one.m_41777_();
            part.m_41764_(Math.min(max, left));
            out.add(part);
        }
        return out;
    }

    /** {@code halves} of fuel taken out of the chests (the cheapest that covers it: sticks and planks before coal), never more than held. */
    static void burn(ServerLevel level, List<BlockPos> chests, int halves) {
        int left = halves;
        for (int pass = 0; pass < 2 && left > 0; pass++) {
            for (BlockPos at : chests) {
                if (!(level.m_7702_(at) instanceof Container box)) {
                    continue;
                }
                for (int i = 0; i < box.m_6643_() && left > 0; i++) {
                    ItemStack stack = box.m_8020_(i);
                    int each = stack.m_41619_() ? 0 : Kitchen.fuel(id(stack));
                    // First pass: small fuel only (sticks, wood); then anything.
                    if (each <= 0 || pass == 0 && each > 3) {
                        continue;
                    }
                    int use = Math.min(stack.m_41613_(), (left + each - 1) / each);
                    left -= use * each;
                    ItemStack after = stack.m_41777_();
                    after.m_41764_(stack.m_41613_() - use);
                    box.m_6836_(i, after.m_41613_() <= 0 ? ItemStack.f_41583_ : after);
                    box.m_6596_();
                }
            }
        }
    }

    /** Up to {@code most} items {@code wanted} (plain stacks, nothing named or enchanted) taken out of the chests. */
    static List<ItemStack> take(ServerLevel level, List<BlockPos> chests, Predicate<String> wanted, int most) {
        return take(level, chests, wanted, most, new String[1]);
    }

    /** The same; {@code kind}[0] set to the first item's name taken (the predicate may read it to keep to one kind). */
    static List<ItemStack> take(ServerLevel level, List<BlockPos> chests, Predicate<String> wanted, int most, String[] kind) {
        List<ItemStack> out = new ArrayList<>();
        int left = most;
        for (BlockPos at : chests) {
            if (left <= 0 || !(level.m_7702_(at) instanceof Container box)) {
                continue;
            }
            for (int i = 0; i < box.m_6643_() && left > 0; i++) {
                ItemStack stack = box.m_8020_(i);
                if (stack.m_41619_() || stack.m_41783_() != null) {
                    continue;
                }
                String id = id(stack);
                if (!wanted.test(id)) {
                    continue;
                }
                if (kind[0] == null) {
                    kind[0] = id;
                }
                int use = Math.min(left, stack.m_41613_());
                ItemStack taken = stack.m_41777_();
                taken.m_41764_(use);
                out.add(taken);
                ItemStack after = stack.m_41777_();
                after.m_41764_(stack.m_41613_() - use);
                box.m_6836_(i, after.m_41613_() <= 0 ? ItemStack.f_41583_ : after);
                box.m_6596_();
                left -= use;
            }
        }
        return out;
    }

    /** {@code taken} cut down to {@code most} items in all, the rest put back in the chests; what is kept. */
    static List<ItemStack> trim(EtoNpc npc, ServerLevel level, List<BlockPos> chests, List<ItemStack> taken, int most) {
        List<ItemStack> kept = new ArrayList<>();
        List<ItemStack> back = new ArrayList<>();
        int left = Math.max(0, most);
        for (ItemStack stack : taken) {
            int keep = Math.min(left, stack.m_41613_());
            left -= keep;
            if (keep > 0) {
                ItemStack k = stack.m_41777_();
                k.m_41764_(keep);
                kept.add(k);
            }
            if (keep < stack.m_41613_()) {
                ItemStack b = stack.m_41777_();
                b.m_41764_(stack.m_41613_() - keep);
                back.add(b);
            }
        }
        giveBack(npc, level, chests, back);
        return kept;
    }

    /** True if every one of {@code stacks} would go into the chests (each whole). */
    static boolean fits(ServerLevel level, List<BlockPos> chests, List<ItemStack> stacks) {
        int empty = 0;
        for (BlockPos at : chests) {
            if (level.m_7702_(at) instanceof Container box) {
                for (int i = 0; i < box.m_6643_(); i++) {
                    if (box.m_8020_(i).m_41619_()) {
                        empty++;
                    }
                }
            }
        }
        return empty >= stacks.size();
    }

    /** {@code stacks} put into the chests (onto the same, then into empty slots); what can't go in is dropped at {@code npc}'s feet. */
    static void giveBack(EtoNpc npc, ServerLevel level, List<BlockPos> chests, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            boolean in = false;
            for (BlockPos at : chests) {
                if (level.m_7702_(at) instanceof Container box && HomeChores.insert(box, stack)) {
                    box.m_6596_();
                    in = true;
                    break;
                }
            }
            if (!in && npc != null) {
                npc.m_19983_(stack);
            }
        }
    }

    private static void smoke(ServerLevel level, BlockPos at) {
        level.m_8767_(ParticleTypes.f_123762_, at.m_123341_() + 0.5, at.m_123342_() + 1.1, at.m_123343_() + 0.5, 6, 0.2, 0.1, 0.2, 0.01);
        level.m_8767_(ParticleTypes.f_175834_, at.m_123341_() + 0.5, at.m_123342_() + 0.5, at.m_123343_() + 0.5, 3, 0.25, 0.1, 0.25, 0.0);
    }

    static BlockPos nearest(List<BlockPos> all, BlockPos from) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos at : all) {
            double d = from == null ? 0.0 : Math.pow(at.m_123341_() - from.m_123341_(), 2) + Math.pow(at.m_123342_() - from.m_123342_(), 2)
                + Math.pow(at.m_123343_() - from.m_123343_(), 2);
            if (d < bestD) {
                bestD = d;
                best = at;
            }
        }
        return best;
    }

    private static BlockPos nearestWhere(ServerLevel level, List<BlockPos> all, BlockPos from, Predicate<String> fits) {
        List<BlockPos> some = new ArrayList<>();
        for (BlockPos at : all) {
            if (level.m_46805_(at) && fits.test(String.valueOf(level.m_8055_(at).m_60734_()))) {
                some.add(at);
            }
        }
        return some.isEmpty() ? null : nearest(some, from);
    }
}
