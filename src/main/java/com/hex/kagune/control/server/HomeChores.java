package com.hex.kagune.control.server;

import com.hex.kagune.control.family.Chores;
import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.EtoNpc;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 1.3.29: a spouse keeping house by day at the home it shares with them ({@link Households}; the rules are {@link Chores}). Instead of
 * strolling to the door and back it goes about the house: to a chest (its lid up, bent over it, its things sorted and stacked for real),
 * round the floor with the brush (whatever has lain about on it a while is put into a chest), to the tables with a cloth, out round the
 * house with a bucket for the flowers and the crops (a crop or a sapling it waters may grow a step), to a bookshelf a while, to a window.
 * Each chore is a short round of a few blocks of its kind; on the way, if it gets nowhere for a few seconds it sets off again, and a
 * block it can't get to is given up (1.3.32: it is never put anywhere). Anything else (a fight, a talk, a kiss,
 * its spouse's hand, a child, the night) stops it where it is (a chest's lid shut again). Its home's blocks are looked for every minute.
 */
public final class HomeChores {
    /** How far below and above the home's spot it looks (the home's radius across). */
    static final int BELOW = 5;
    static final int ABOVE = 7;
    /** How long what was found is trusted (ticks). */
    static final long RESCAN = 1200L;
    /** At most this many blocks of each kind are kept (the nearest the home's spot first). */
    static final int KEEP = 96;
    /** How fast it walks about the house. */
    static final double SPEED = 0.7;

    /** A home's blocks for its chores as last found, when; and its furniture (beside which the floor is swept). */
    record Found(long at, Map<Chores.Kind, List<BlockPos>> blocks, List<BlockPos> furniture) {
        int[] counts() {
            int[] out = new int[Chores.Kind.values().length];
            for (Map.Entry<Chores.Kind, List<BlockPos>> e : this.blocks.entrySet()) {
                out[e.getKey().ordinal()] = e.getValue().size();
            }
            out[Chores.Kind.SWEEP.ordinal()] = this.furniture.size();
            return out;
        }
    }

    /** The chore under way: its kind, the blocks of its round and which one it is at, its walk there, what it has done. */
    static final class Job {
        final Chores.Kind kind;
        final List<BlockPos> round;
        int index;
        long walkSince;
        long workSince = Long.MIN_VALUE;
        double fromX;
        double fromZ;
        long fromAt;
        int repaths;
        int fails;
        long nextPath;
        /** A chest whose lid it has up (shut again whatever happens). */
        BlockPos lid;
        /** It put a chest in order, or swept things up off the floor; how many blocks it got to. */
        boolean did;
        int worked;
        /** What its hands do at this block (a sweep ends at a chest: put away there); a new walk's start not yet marked. */
        EtoGesture doing = EtoGesture.NONE;
        boolean fresh = true;
        /** The chest the swept things go to (the round's last block), or -1; whether it went to sweep things up. */
        int box = -1;
        boolean items;

        /** 1.3.31: what each block of the round is expected to be (null: a spot of floor) and what its hands do there. */
        final List<Chores.Kind> expects;
        final List<EtoGesture> doings;
        /** 1.3.31: the home's chests as the chore began, and whether its spouse is a ghoul (their meal kept raw). */
        List<BlockPos> chests = List.of();
        boolean ghoul;
        /** 1.3.42: the cookbook's dish it went to cook (its id), or null for the plain cooking. */
        String dish;

        Job(Chores.Kind kind, List<BlockPos> round, long now) {
            this(kind, round, null, null, now);
        }

        Job(Chores.Kind kind, List<BlockPos> round, List<Chores.Kind> expects, List<EtoGesture> doings, long now) {
            this.kind = kind;
            this.round = round;
            this.walkSince = now;
            this.fromAt = now;
            this.expects = new ArrayList<>();
            this.doings = new ArrayList<>();
            for (int i = 0; i < round.size(); i++) {
                this.expects.add(expects != null && i < expects.size() ? expects.get(i) : kind == Chores.Kind.SWEEP ? null : kind);
                this.doings.add(doings != null && i < doings.size() ? doings.get(i) : kind.gesture);
            }
        }

        BlockPos at() {
            return this.index < this.round.size() ? this.round.get(this.index) : null;
        }

        Chores.Kind expect() {
            return this.index < this.expects.size() ? this.expects.get(this.index) : null;
        }

        boolean last() {
            return this.index == this.round.size() - 1;
        }
    }

    private static final Map<String, Found> FOUND = new HashMap<>();
    /** What each kind of block is for (null: nothing), known once per kind of block. */
    private static final Map<Object, Chores.Kind> KINDS = new IdentityHashMap<>();
    private static final Map<Object, Boolean> PLAIN = new IdentityHashMap<>();
    private static final Object NOTHING = new Object();
    private static final Map<Object, Object> KNOWN = new IdentityHashMap<>();

    private HomeChores() {
    }

    // ---------------------------------------------------------------- a day's chores

    /**
     * Its chores this tick (called from its calm, at its home, by day, free): the one under way carried on, or the next begun once it is
     * time. True while a chore has it (it walks or works; nothing else of its calm is done).
     */
    static boolean tick(EtoNpc npc, EtoBrain.Mind mind, Households.Home house, ServerLevel level, ServerPlayer partner, long now) {
        Job job = mind.chore;
        if (job == null) {
            if (now < mind.nextChore || mind.gesture != EtoGesture.NONE) {
                return false;
            }
            boolean near = partner != null && partner.m_20280_(npc) < 16.0 * 16.0;
            mind.nextChore = now + Chores.gap(near, npc.m_217043_().m_188500_());
            job = begin(npc, mind, house, level, now);
            if (job == null) {
                return false;
            }
            mind.chore = job;
            speak(npc, mind, partner, Chores.start(npc.persona().key(), job.kind, npc.m_217043_().m_188503_(1000)), now);
        }
        BlockPos at = job.at();
        if (at == null) {
            finish(npc, mind, level, partner, now);
            return false;
        }
        if (job.workSince == Long.MIN_VALUE) {
            if (job.index == job.box && !job.did) {
                next(job, now); // nothing was swept up: no chest to go to
                return true;
            }
            if (!still(level, at, job.expect())) {
                next(job, now);
                return true;
            }
            if (close(npc, at, job.expect() == null ? job.kind : job.expect())) {
                work(npc, mind, job, level, at, now);
            } else {
                walk(npc, mind, job, house, level, at, now);
            }
            return true;
        }
        // At it: facing it, hands busy, until the step is done. 2.0.8 (the owner: "longer cooking animations over the furnace, with sizzling
        // sounds, while an NPC is doing a task make sure it doesnt cancel it out to do another ... dont allow them to rotate while cooking"):
        // at the stove the stirring goes round Chores.COOK_LOOPS times, the stove crackling; her body and head stay on it every tick; a
        // gesture or word of her own no longer ends the step (her hands go back to it). Only what stops the chore (a fight, a talk, a kiss,
        // a hand, the night: stop()) ends it early.
        npc.m_21573_().m_26573_();
        face(npc, at);
        long age = now - job.workSince;
        if (age % 10L == 0L && (job.doing == job.kind.gesture || job.expect() == Chores.Kind.COOK)) {
            particles(level, job.expect() == Chores.Kind.COOK ? Chores.Kind.COOK : job.kind, at);
        }
        if (job.doing == EtoGesture.COOK && age % 40L == 0L) {
            String sound = Chores.crackle(String.valueOf(level.m_8055_(at).m_60734_()));
            if (sound != null) {
                level.m_6263_(null, at.m_123341_() + 0.5, at.m_123342_() + 0.6, at.m_123343_() + 0.5,
                    net.minecraft.sounds.SoundEvent.m_262824_(net.minecraft.resources.ResourceLocation.m_214293_("minecraft", sound)),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 0.9F + npc.m_217043_().m_188501_() * 0.2F);
            }
        }
        if (age >= Chores.stepTicks(job.doing)) {
            job.did |= effect(npc, job, level, at, house, now);
            shut(level, job);
            next(job, now);
        } else if (mind.gesture != job.doing || job.doing.over(now - mind.gestureAt)) {
            EtoBrain.gesture(mind, job.doing, now); // back to it (another round of stirring, or after a word of her own)
        }
        return true;
    }

    /** Anything else wants it (a fight, a talk, a kiss...): the chore stops where it is, a chest's lid shut again. */
    static void stop(EtoNpc npc, EtoBrain.Mind mind, ServerLevel level) {
        Job job = mind.chore;
        if (job == null) {
            return;
        }
        shut(level, job);
        if (job.workSince != Long.MIN_VALUE && mind.gesture == job.doing) {
            mind.gesture = EtoGesture.NONE;
            mind.gestureAt = level.m_46467_();
            mind.dirty = true;
        }
        mind.chore = null;
        mind.lastChore = job.kind;
    }

    /**
     * 1.3.31: what {@code npc} makes of the home it shares with its spouse, for {@code /kagune home}: whether it is at it, what it found
     * there (chests, stations, plants, tables...), what the chests hold that it can use, what it is doing, the meal it keeps.
     */
    static List<String> status(EtoNpc npc, ServerLevel level) {
        List<String> out = new ArrayList<>();
        String name = npc.displayName();
        EtoBrain.Mind mind = npc.mind;
        Households.Home house = npc.following() == null ? Households.forCompanion(level, npc) : null;
        if (mind == null || house == null) {
            out.add(name + " isn't at your home" + (npc.following() != null ? " (following someone)." : ". Marry, then tell them \"This is our home now.\" where you live."));
            return out;
        }
        long now = level.m_46467_();
        Found found = found(level, house, now);
        StringBuilder seen = new StringBuilder();
        for (Chores.Kind kind : Chores.Kind.values()) {
            int n = found.blocks().getOrDefault(kind, List.of()).size();
            if (n > 0) {
                seen.append(seen.length() == 0 ? "" : ", ").append(n).append(' ').append(switch (kind) {
                    case TIDY -> "chests";
                    case WIPE -> "tables";
                    case WATER -> "plants";
                    case READ -> "shelves";
                    case LOOK -> "windows";
                    case COOK -> "stoves";
                    default -> kind.name().toLowerCase(java.util.Locale.ROOT);
                });
            }
        }
        out.add(name + " at home (" + house.radius() + " blocks round): " + (seen.length() == 0 ? "found nothing to look after" : seen));
        List<BlockPos> chests = HomeKitchen.chests(level, found);
        ServerPlayer partner = EtoBrain.player(level, EtoBrain.spouse(npc));
        boolean ghoul = HomeKitchen.ghoulSpouse(npc, partner);
        HomeKitchen.Plan plan = chests.isEmpty() ? HomeKitchen.Plan.NONE : HomeKitchen.plan(level, found, chests, ghoul,
            com.hex.kagune.control.family.Kitchen.MEAL - com.hex.kagune.control.family.Kitchen.portions(HomeKitchen.meal(npc)));
        out.add("Can do now: " + (plan.cookAt() != null ? (ghoul ? "set aside raw cuts for you" : "cook") + ", " : "")
            + (plan.smeltAt() != null ? "smelt, " : "") + HomeKitchen.ripe(level, found, 64).size() + " ripe crops to pick"
            + (plan.noFuel() ? "; out of fuel (coal, charcoal, wood in a chest)" : "") + (chests.isEmpty() ? "; no plain chest or barrel to use" : ""));
        // 1.3.42: the cookbook's dishes.
        if (!ghoul) {
            out.add("Cookbook dishes: only for a ghoul spouse (you're human: you get the plain cooking).");
        } else {
            List<String> dishes = HomeKitchen.possible(level, found, chests, true, 6);
            int today = HomeKitchen.dishesToday(npc, level);
            out.add("Cookbook dishes: " + (dishes.isEmpty() ? "none the chests hold everything for" : "could make " + String.join(", ", dishes))
                + " (made today: " + today + " of " + com.hex.kagune.control.family.Kitchen.DISHES_A_DAY + ")");
        }
        Job job = mind.chore;
        out.add("Doing: " + (job == null ? (HomeLife.night(level.m_46468_(), false) ? "resting (night)" : "nothing right now (next chore in "
            + Math.max(0L, (mind.nextChore - now) / 20L) + " s)") : job.kind.name().toLowerCase(java.util.Locale.ROOT) + ", step " + (job.index + 1) + " of "
            + job.round.size()));
        List<Chores.Pile> meal = HomeKitchen.meal(npc);
        if (!meal.isEmpty()) {
            StringBuilder m = new StringBuilder();
            for (Chores.Pile pile : meal) {
                m.append(m.length() == 0 ? "" : ", ").append(pile.count()).append(' ').append(com.hex.kagune.control.family.Kitchen.name(pile.key()));
            }
            out.add("Keeping for you: " + m + " (handed over when you come home)");
        }
        return out;
    }

    /** True if a chore has it now (vanilla's strolling waits meanwhile). */
    static boolean busy(EtoBrain.Mind mind) {
        return mind != null && mind.chore != null;
    }

    private static void finish(EtoNpc npc, EtoBrain.Mind mind, ServerLevel level, ServerPlayer partner, long now) {
        Job job = mind.chore;
        mind.chore = null;
        if (job == null) {
            return;
        }
        shut(level, job);
        mind.lastChore = job.kind;
        if (job.items && !job.did) {
            mind.noSweepUntil = now + 6000L;
        }
        if (job.worked > 0) {
            speak(npc, mind, partner, Chores.done(npc.persona().key(), job.kind, job.did, npc.m_217043_().m_188503_(1000)), now);
        }
    }

    /** The next block of the round (its walk there starts now). */
    private static void next(Job job, long now) {
        job.index++;
        job.workSince = Long.MIN_VALUE;
        job.walkSince = now;
        job.fromAt = now;
        job.repaths = 0;
        job.fails = 0;
        job.nextPath = now;
        job.fresh = true;
        job.doing = EtoGesture.NONE;
    }

    /** Its words, to its spouse alone, near enough to hear and not too often. */
    private static void speak(EtoNpc npc, EtoBrain.Mind mind, ServerPlayer partner, String line, long now) {
        boolean lately = mind.choreSaid != Long.MIN_VALUE && now - mind.choreSaid < 2400L;
        if (partner == null || partner.m_20280_(npc) > 12.0 * 12.0 || lately || npc.m_217043_().m_188500_() > 0.4) {
            return;
        }
        mind.choreSaid = now;
        EtoBrain.say(npc, partner, line);
    }

    // ---------------------------------------------------------------- picking a chore

    /** The next chore: which (weighed by what the home has), and the blocks of its round, nearest first. Null if there is nothing to do. */
    private static Job begin(EtoNpc npc, EtoBrain.Mind mind, Households.Home house, ServerLevel level, long now) {
        Found found = found(level, house, now);
        // Things it could not sweep up last time (out of reach, no chest with room) are left a while.
        List<ItemEntity> items = now >= mind.noSweepUntil ? items(level, house) : List.of();
        int[] counts = found.counts();
        if (level.m_46471_()) {
            counts[Chores.Kind.WATER.ordinal()] = 0; // the rain waters the garden
        }
        // 1.3.31: real help: what the chests hold to cook or smelt, and the ripe crops.
        ServerPlayer partner = EtoBrain.player(level, EtoBrain.spouse(npc));
        boolean ghoul = HomeKitchen.ghoulSpouse(npc, partner);
        List<BlockPos> chests = HomeKitchen.chests(level, found);
        int mealRoom = com.hex.kagune.control.family.Kitchen.MEAL - com.hex.kagune.control.family.Kitchen.portions(HomeKitchen.meal(npc));
        HomeKitchen.Plan plan = chests.isEmpty() ? HomeKitchen.Plan.NONE : HomeKitchen.plan(level, found, chests, ghoul, mealRoom);
        List<BlockPos> ripe = chests.isEmpty() ? List.of() : HomeKitchen.ripe(level, found, 4);
        // 1.3.42: one of the cookbook's dishes, when the chests hold all it needs (a few a day, a while apart).
        HomeKitchen.Dinner dinner = HomeKitchen.dinner(npc, level, found, chests, ghoul, now);
        counts[Chores.Kind.COOK.ordinal()] = plan.cookAt() != null || dinner != null ? 1 : 0;
        counts[Chores.Kind.SMELT.ordinal()] = plan.smeltAt() != null ? 1 : 0;
        counts[Chores.Kind.HARVEST.ordinal()] = ripe.size();
        if (plan.noFuel() && partner != null && now - mind.noFuelSaid > 24000L && SceneServer.sees(npc, partner, 12.0)) {
            mind.noFuelSaid = now;
            EtoBrain.say(npc, partner, com.hex.kagune.control.family.Kitchen.noFuel(npc.persona().key(), npc.m_217043_().m_188503_(1000)));
        }
        Chores.Kind kind = Chores.pick(counts, mind.lastChore, !items.isEmpty(), npc.m_217043_().m_188500_());
        if (kind == null) {
            return null;
        }
        if (kind == Chores.Kind.COOK || kind == Chores.Kind.SMELT) {
            // Fetched from the chest, then done at the stove.
            boolean cook = kind == Chores.Kind.COOK;
            boolean dish = cook && dinner != null;
            BlockPos from = dish ? dinner.chest() : cook ? plan.cookChest() : plan.smeltChest();
            BlockPos at = dish ? dinner.at() : cook ? plan.cookAt() : plan.smeltAt();
            List<BlockPos> steps = new ArrayList<>(from.equals(at) ? List.of(at) : List.of(from, at));
            Chores.Kind atKind = kind(level.m_8055_(at).m_60734_());
            List<Chores.Kind> expects = steps.size() == 1 ? List.of(Chores.Kind.TIDY) : List.of(Chores.Kind.TIDY, atKind);
            List<EtoGesture> doings = steps.size() == 1 ? List.of(cook ? EtoGesture.COOK : EtoGesture.TIDY)
                : List.of(EtoGesture.TIDY, cook ? EtoGesture.COOK : EtoGesture.TIDY);
            Job job = new Job(kind, steps, expects, doings, now);
            job.chests = chests;
            job.ghoul = ghoul;
            job.dish = dish ? dinner.cooking().dish().id() : null;
            return job;
        }
        if (kind == Chores.Kind.HARVEST) {
            List<BlockPos> steps = new ArrayList<>(ripe);
            List<Chores.Kind> expects = new ArrayList<>();
            List<EtoGesture> doings = new ArrayList<>();
            for (int i = 0; i < steps.size(); i++) {
                expects.add(Chores.Kind.WATER);
                doings.add(EtoGesture.TIDY);
            }
            BlockPos box = HomeKitchen.nearest(chests, steps.get(steps.size() - 1));
            if (box != null) {
                steps.add(box);
                expects.add(Chores.Kind.TIDY);
                doings.add(EtoGesture.TIDY);
            }
            Job job = new Job(kind, steps, expects, doings, now);
            job.chests = chests;
            return job;
        }
        List<BlockPos> round = new ArrayList<>();
        if (kind == Chores.Kind.SWEEP) {
            for (ItemEntity item : items) {
                BlockPos spot = BlockPos.m_274561_(item.m_20185_(), item.m_20186_(), item.m_20189_());
                if (round.size() < Chores.ROUND && round.stream().noneMatch(p -> d2(p, spot) < 9.0)) {
                    round.add(spot);
                }
            }
            for (int tries = 0; round.size() < 2 && tries < 12 && !found.furniture().isEmpty(); tries++) {
                BlockPos beside = floorBeside(level, found.furniture().get(npc.m_217043_().m_188503_(found.furniture().size())), npc);
                if (beside != null && round.stream().noneMatch(p -> d2(p, beside) < 9.0)) {
                    round.add(beside);
                }
            }
        } else {
            List<BlockPos> all = found.blocks().getOrDefault(kind, List.of());
            int want = switch (kind) {
                case WATER -> Chores.ROUND;
                case TIDY, WIPE -> 2;
                default -> 1;
            };
            for (int tries = 0; round.size() < want && tries < want * 4 && !all.isEmpty(); tries++) {
                BlockPos pick = all.get(npc.m_217043_().m_188503_(all.size()));
                // A watering covers the plants about it: the next one is a little way off.
                double apart = kind == Chores.Kind.WATER ? 9.0 : 0.5;
                if (round.stream().noneMatch(p -> d2(p, pick) < apart)) {
                    round.add(pick);
                }
            }
        }
        if (round.isEmpty()) {
            return null;
        }
        // Nearest first, then each next one nearest the last.
        List<BlockPos> ordered = new ArrayList<>();
        double x = npc.m_20185_();
        double y = npc.m_20186_();
        double z = npc.m_20189_();
        while (!round.isEmpty()) {
            BlockPos best = round.get(0);
            double bestD = Double.MAX_VALUE;
            for (BlockPos p : round) {
                double d = Math.pow(p.m_123341_() + 0.5 - x, 2) + Math.pow(p.m_123342_() - y, 2) + Math.pow(p.m_123343_() + 0.5 - z, 2);
                if (d < bestD) {
                    bestD = d;
                    best = p;
                }
            }
            round.remove(best);
            ordered.add(best);
            x = best.m_123341_() + 0.5;
            y = best.m_123342_();
            z = best.m_123343_() + 0.5;
        }
        if (kind == Chores.Kind.SWEEP && !items.isEmpty()) {
            // Swept up, the things go into a chest: it goes to that chest after (and puts it in order while it is there).
            BlockPos box = chestFor(level, found, ordered.get(ordered.size() - 1));
            if (box != null) {
                ordered.add(box);
            }
        }
        Job job = new Job(kind, ordered, now);
        job.items = kind == Chores.Kind.SWEEP && !items.isEmpty();
        if (kind == Chores.Kind.SWEEP && !items.isEmpty() && ordered.size() > 1 && kind(level.m_8055_(ordered.get(ordered.size() - 1)).m_60734_()) == Chores.Kind.TIDY) {
            job.box = ordered.size() - 1;
            job.expects.set(job.box, Chores.Kind.TIDY);
            job.doings.set(job.box, EtoGesture.TIDY);
        }
        job.chests = chests;
        return job;
    }

    /** What the home has for chores (looked for again every {@link #RESCAN} ticks; only in loaded ground). */
    static Found found(ServerLevel level, Households.Home house, long now) {
        Found found = FOUND.get(house.save());
        if (found != null && now - found.at() < RESCAN && now >= found.at()) {
            return found;
        }
        Map<Chores.Kind, List<BlockPos>> blocks = new EnumMap<>(Chores.Kind.class);
        List<BlockPos> furniture = new ArrayList<>();
        BlockPos centre = house.pos();
        int r = house.radius();
        // Outward from the spot, so the nearest are kept when there are too many.
        for (int ring = 0; ring <= r; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    for (int dy = -BELOW; dy <= ABOVE; dy++) {
                        BlockPos at = new BlockPos(centre.m_123341_() + dx, centre.m_123342_() + dy, centre.m_123343_() + dz);
                        if (!level.m_46805_(at)) {
                            continue;
                        }
                        BlockState state = level.m_8055_(at);
                        Object block = state.m_60734_();
                        Chores.Kind kind = kind(block);
                        if (kind != null) {
                            List<BlockPos> list = blocks.computeIfAbsent(kind, k -> new ArrayList<>());
                            if (list.size() < KEEP) {
                                list.add(at);
                            }
                            if (kind != Chores.Kind.WATER && kind != Chores.Kind.LOOK && furniture.size() < KEEP) {
                                furniture.add(at);
                            }
                        } else if (furniture.size() < KEEP && (HomeLife.isBed(block) || furnace(block))) {
                            furniture.add(at);
                        }
                    }
                }
            }
        }
        found = new Found(now, blocks, furniture);
        FOUND.put(house.save(), found);
        return found;
    }

    /** The home moved (or was set): its blocks looked for again. */
    static void forget(Households.Home house) {
        FOUND.remove(house.save());
    }

    private static Chores.Kind kind(Object block) {
        Object known = KNOWN.computeIfAbsent(block, b -> {
            Chores.Kind kind = Chores.of(String.valueOf(b));
            return kind == null ? NOTHING : kind;
        });
        return known instanceof Chores.Kind kind ? kind : null;
    }

    private static boolean furnace(Object block) {
        return PLAIN.computeIfAbsent(block, b -> {
            String id = Chores.id(String.valueOf(b));
            return id.endsWith("furnace") || id.endsWith("smoker") || id.endsWith("campfire") || id.endsWith("cauldron");
        });
    }

    /** Things lying about the home's floor a while (never just dropped, never in water: a farm's stream is left alone). */
    private static List<ItemEntity> items(ServerLevel level, Households.Home house) {
        BlockPos c = house.pos();
        int r = house.radius();
        AABB box = new AABB(c.m_123341_() - r, c.m_123342_() - BELOW, c.m_123343_() - r, c.m_123341_() + r + 1, c.m_123342_() + ABOVE, c.m_123343_() + r + 1);
        return level.m_6443_(ItemEntity.class, box, item -> item.m_6084_() && item.f_19797_ >= Chores.SETTLED && !item.m_20069_());
    }

    /** A spot of floor beside {@code furniture} (air to stand in, something under it), or null. */
    private static BlockPos floorBeside(ServerLevel level, BlockPos furniture, EtoNpc npc) {
        for (int tries = 0; tries < 6; tries++) {
            int dx = npc.m_217043_().m_188503_(5) - 2;
            int dz = npc.m_217043_().m_188503_(5) - 2;
            if (dx == 0 && dz == 0) {
                continue;
            }
            for (int dy = 1; dy >= -1; dy--) {
                BlockPos at = off(furniture, dx, dy, dz);
                if (standable(level, at)) {
                    return at;
                }
            }
        }
        return null;
    }

    /** True if a body can stand at {@code at}: it and the block above it air, something under it. */
    private static boolean standable(ServerLevel level, BlockPos at) {
        return level.m_46805_(at) && level.m_8055_(at).m_60795_() && level.m_8055_(off(at, 0, 1, 0)).m_60795_() && !level.m_8055_(off(at, 0, -1, 0)).m_60795_();
    }

    private static BlockPos off(BlockPos at, int dx, int dy, int dz) {
        return new BlockPos(at.m_123341_() + dx, at.m_123342_() + dy, at.m_123343_() + dz);
    }

    private static double d2(BlockPos a, BlockPos b) {
        double dx = a.m_123341_() - b.m_123341_();
        double dy = a.m_123342_() - b.m_123342_();
        double dz = a.m_123343_() - b.m_123343_();
        return dx * dx + dy * dy + dz * dz;
    }

    /** True if {@code at} is still what this step of the chore is for (a spot of floor always is). */
    private static boolean still(ServerLevel level, BlockPos at, Chores.Kind kind) {
        if (kind == null || kind == Chores.Kind.SWEEP) {
            return true;
        }
        if (!level.m_46805_(at)) {
            return false;
        }
        return kind(level.m_8055_(at).m_60734_()) == kind;
    }

    // ---------------------------------------------------------------- the walk there

    private static boolean close(EtoNpc npc, BlockPos at, Chores.Kind kind) {
        double dx = at.m_123341_() + 0.5 - npc.m_20185_();
        double dz = at.m_123343_() + 0.5 - npc.m_20189_();
        double dy = at.m_123342_() - npc.m_20186_();
        return dx * dx + dz * dz <= kind.reach * kind.reach && dy > -2.5 && dy < 2.5;
    }

    /**
     * On its way: set on its path again every half second; no nearer in a few seconds, it sets off again; stuck that way a few times, no
     * way there at all, or too long on the way, the block is given up (1.3.32: never put anywhere).
     */
    private static void walk(EtoNpc npc, EtoBrain.Mind mind, Job job, Households.Home house, ServerLevel level, BlockPos at, long now) {
        if (now - job.walkSince > Chores.WALK || job.fails >= 3) {
            next(job, now);
            return;
        }
        if (job.fresh) {
            job.fresh = false;
            job.fromX = npc.m_20185_();
            job.fromZ = npc.m_20189_();
            job.fromAt = now;
        }
        if (now - job.fromAt >= Chores.STUCK) {
            double moved = Math.hypot(npc.m_20185_() - job.fromX, npc.m_20189_() - job.fromZ);
            if (moved < Chores.PROGRESS) {
                job.repaths++;
                npc.m_21573_().m_26573_();
                job.nextPath = now;
                if (job.repaths >= Chores.REPATHS) {
                    // 1.3.32 (the owner: it should never teleport): no way through, it gives that one up and goes on to the next.
                    next(job, now);
                    return;
                }
            }
            job.fromX = npc.m_20185_();
            job.fromZ = npc.m_20189_();
            job.fromAt = now;
        }
        if (now >= job.nextPath) {
            job.nextPath = now + 10L;
            boolean way = npc.m_21573_().m_26519_(at.m_123341_() + 0.5, at.m_123342_(), at.m_123343_() + 0.5, SPEED);
            if (!way) {
                job.fails++;
            }
        }
    }

    // ---------------------------------------------------------------- the work

    /** It has got there: it stops, faces it, its hands at it (a chest's lid up). */
    private static void work(EtoNpc npc, EtoBrain.Mind mind, Job job, ServerLevel level, BlockPos at, long now) {
        npc.m_21573_().m_26573_();
        face(npc, at);
        job.doing = job.index < job.doings.size() ? job.doings.get(job.index) : job.kind.gesture;
        EtoBrain.gesture(mind, job.doing, now);
        job.workSince = now;
        job.worked++;
        BlockState state = level.m_8055_(at);
        if (Chores.lid(String.valueOf(state.m_60734_()))) {
            level.m_7696_(at, state.m_60734_(), 1, 1);
            job.lid = at;
        }
    }

    /** Its hands on {@code at}: its body and head turned to it. */
    private static void face(EtoNpc npc, BlockPos at) {
        double dx = at.m_123341_() + 0.5 - npc.m_20185_();
        double dz = at.m_123343_() + 0.5 - npc.m_20189_();
        if (dx * dx + dz * dz > 1.0E-4) {
            float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
            npc.m_146922_(yaw);
            npc.f_20883_ = yaw;
            npc.f_20885_ = yaw;
        }
        npc.m_21563_().m_24946_(at.m_123341_() + 0.5, at.m_123342_() + 0.5, at.m_123343_() + 0.5);
    }

    /** A chest's lid it had up, shut. */
    private static void shut(ServerLevel level, Job job) {
        if (job.lid == null) {
            return;
        }
        BlockPos at = job.lid;
        job.lid = null;
        if (level.m_46805_(at)) {
            level.m_7696_(at, level.m_8055_(at).m_60734_(), 1, 0);
        }
    }

    private static void particles(ServerLevel level, Chores.Kind kind, BlockPos at) {
        double x = at.m_123341_() + 0.5;
        double y = at.m_123342_();
        double z = at.m_123343_() + 0.5;
        switch (kind) {
            case SWEEP -> level.m_8767_(ParticleTypes.f_123759_, x, y + 0.1, z, 2, 0.45, 0.02, 0.45, 0.01);
            case WATER -> {
                level.m_8767_(ParticleTypes.f_123769_, x, y + 0.7, z, 8, 0.35, 0.1, 0.35, 0.05);
                level.m_8767_(ParticleTypes.f_123804_, x, y + 1.2, z, 3, 0.25, 0.05, 0.25, 0.0);
            }
            case COOK -> {
                // 1.3.42: smoke and flame only off a stove (a dish made at a table or by the chest is stirred without them).
                if (com.hex.kagune.control.family.Kitchen.station(String.valueOf(level.m_8055_(at).m_60734_()), false)) {
                    level.m_8767_(ParticleTypes.f_123762_, x, y + 1.15, z, 2, 0.15, 0.05, 0.15, 0.01);
                    level.m_8767_(ParticleTypes.f_175834_, x, y + 0.45, z, 1, 0.2, 0.05, 0.2, 0.0);
                }
            }
            default -> {
            }
        }
    }

    /**
     * What the chore does when it is done at {@code at}: a chest put in order; the floor swept (what has lain about there put into a
     * chest); the plants about a watered one helped along; a table left clean. True if it did something real.
     */
    private static boolean effect(EtoNpc npc, Job job, ServerLevel level, BlockPos at, Households.Home house, long now) {
        double x = at.m_123341_() + 0.5;
        double y = at.m_123342_();
        double z = at.m_123343_() + 0.5;
        switch (job.kind) {
            case TIDY -> {
                if (sort(level, at)) {
                    level.m_8767_(ParticleTypes.f_123748_, x, y + 1.0, z, 5, 0.3, 0.2, 0.3, 0.0);
                    return true;
                }
                return false;
            }
            case SWEEP -> {
                if (job.index == job.box) {
                    // The chest the swept things went into: put in order while it is open.
                    sort(level, at);
                    return false;
                }
                return sweep(level, house, at, now);
            }
            case WIPE -> {
                level.m_8767_(ParticleTypes.f_123748_, x, y + 1.05, z, 6, 0.3, 0.05, 0.3, 0.0);
                return false;
            }
            case COOK -> {
                // 1.3.31: the chest it fetched from first, then the stove (or a table, a ghoul's meal): it cooks there.
                if (!job.last()) {
                    return false;
                }
                if (job.dish != null) {
                    // 1.3.42: one of the cookbook's dishes, made for real; with its spouse there, handed over (or said where it is).
                    ServerPlayer partner = EtoBrain.player(level, EtoBrain.spouse(npc));
                    HomeKitchen.Made dish = HomeKitchen.cookDish(npc, level, job.chests, at, job.ghoul, job.dish, partner, now);
                    if (dish == null) {
                        return false;
                    }
                    if (partner != null && SceneServer.sees(npc, partner, 12.0)) {
                        boolean kept = dish.kept() > 0 && HomeKitchen.hand(npc, partner) != null;
                        if (npc.mind != null) {
                            npc.mind.choreSaid = now; // no "There." on top of it as the chore ends
                        }
                        EtoBrain.say(npc, partner, com.hex.kagune.control.family.Kitchen.made(npc.persona().key(),
                            com.hex.kagune.control.kitchen.Dishes.name(dish.cooking().dish().id()), kept, npc.m_217043_().m_188503_(1000)));
                    }
                    return true;
                }
                boolean made = HomeKitchen.cook(npc, level, job.chests, at, job.ghoul);
                if (made) {
                    ServerPlayer partner = EtoBrain.player(level, EtoBrain.spouse(npc));
                    if (partner != null && SceneServer.sees(npc, partner, 12.0)) {
                        String served = HomeKitchen.serve(npc, partner, level); // they're here: handed over warm
                        if (served != null) {
                            EtoBrain.say(npc, partner, served);
                        }
                    }
                }
                return made;
            }
            case SMELT -> {
                return job.last() && HomeKitchen.smelt(npc, level, job.chests, at);
            }
            case HARVEST -> {
                if (job.expect() == Chores.Kind.TIDY) {
                    sort(level, at); // the chest the crops went into, put in order while it is open
                    return false;
                }
                return HomeKitchen.harvest(npc, level, job.chests, at, npc.m_217043_().m_188500_(), npc.m_217043_().m_188500_());
            }
            case WATER -> {
                boolean grew = false;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            BlockPos plant = off(at, dx, dy, dz);
                            if (!level.m_46805_(plant)) {
                                continue;
                            }
                            BlockState state = level.m_8055_(plant);
                            if (Chores.grows(String.valueOf(state.m_60734_())) && npc.m_217043_().m_188500_() < 0.5) {
                                state.m_222972_(level, plant, npc.m_217043_());
                                grew = true;
                            }
                        }
                    }
                }
                return grew;
            }
            default -> {
                return false;
            }
        }
    }

    // ---------------------------------------------------------------- a chest, for real

    /** {@code at}'s things sorted by name and stacked as far as they stack (nothing lost or made). True if anything moved. */
    static boolean sort(ServerLevel level, BlockPos at) {
        if (!level.m_46805_(at) || !Chores.sortable(String.valueOf(level.m_8055_(at).m_60734_()))) {
            return false;
        }
        BlockEntity entity = level.m_7702_(at);
        if (!(entity instanceof Container box)) {
            return false;
        }
        int size = box.m_6643_();
        List<Chores.Pile> piles = new ArrayList<>();
        Map<String, ItemStack> kinds = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ItemStack stack = box.m_8020_(i);
            if (stack.m_41619_()) {
                piles.add(null);
                continue;
            }
            String key = key(stack);
            ItemStack known = kinds.get(key);
            if (known != null && !ItemStack.m_150942_(known, stack)) {
                key = key + "#" + i; // alike to look at, not to stack (a mod's own data): kept apart
            }
            kinds.putIfAbsent(key, stack.m_41777_());
            piles.add(new Chores.Pile(key, stack.m_41613_(), stack.m_41741_()));
        }
        if (Chores.sorted(piles, size)) {
            return false;
        }
        List<Chores.Pile> plan = Chores.sort(piles, size);
        if (plan == null) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (i < plan.size()) {
                ItemStack stack = kinds.get(plan.get(i).key()).m_41777_();
                stack.m_41764_(plan.get(i).count());
                box.m_6836_(i, stack);
            } else {
                box.m_6836_(i, ItemStack.f_41583_);
            }
        }
        box.m_6596_();
        return true;
    }

    /** What a stack is, for the sorting: its item's name, then its data (only stacks equal in both stack together). */
    private static String key(ItemStack stack) {
        Object name = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
        Object tag = stack.m_41783_();
        return String.valueOf(name) + (tag == null ? "" : "|" + tag);
    }

    /** What has lain about the floor near {@code at} put into one of the home's chests (each whole, where it fits). True if any was. */
    private static boolean sweep(ServerLevel level, Households.Home house, BlockPos at, long now) {
        AABB near = new AABB(at.m_123341_() - 2.0, at.m_123342_() - 1.0, at.m_123343_() - 2.0, at.m_123341_() + 3.0, at.m_123342_() + 2.0, at.m_123343_() + 3.0);
        List<ItemEntity> items = level.m_6443_(ItemEntity.class, near, item -> item.m_6084_() && item.f_19797_ >= Chores.SETTLED && !item.m_20069_());
        if (items.isEmpty()) {
            return false;
        }
        Found found = found(level, house, now);
        BlockPos boxAt = chestFor(level, found, at);
        if (boxAt == null || !(level.m_7702_(boxAt) instanceof Container box)) {
            return false;
        }
        boolean any = false;
        for (ItemEntity item : items) {
            ItemStack stack = item.m_32055_();
            if (!stack.m_41619_() && insert(box, stack)) {
                item.m_146870_();
                any = true;
            }
        }
        if (any) {
            box.m_6596_();
            level.m_8767_(ParticleTypes.f_123759_, at.m_123341_() + 0.5, at.m_123342_() + 0.2, at.m_123343_() + 0.5, 6, 0.6, 0.1, 0.6, 0.02);
        }
        return any;
    }

    /** The home's chest nearest {@code from} that is really put in order (a plain chest or a barrel with room to look in), or null. */
    private static BlockPos chestFor(ServerLevel level, Found found, BlockPos from) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos at : found.blocks().getOrDefault(Chores.Kind.TIDY, List.of())) {
            if (!level.m_46805_(at) || !Chores.sortable(String.valueOf(level.m_8055_(at).m_60734_())) || !(level.m_7702_(at) instanceof Container)) {
                continue;
            }
            double d = d2(at, from);
            if (d < bestD) {
                bestD = d;
                best = at;
            }
        }
        return best;
    }

    /** {@code stack} put into {@code box} whole (onto stacks of the same, then into empty slots), or not at all. True if it went in. */
    static boolean insert(Container box, ItemStack stack) {
        int size = box.m_6643_();
        int count = stack.m_41613_();
        int max = Math.max(1, stack.m_41741_());
        int room = 0;
        for (int i = 0; i < size && room < count; i++) {
            ItemStack in = box.m_8020_(i);
            if (in.m_41619_()) {
                room += max;
            } else if (ItemStack.m_150942_(in, stack)) {
                room += Math.max(0, Math.min(max, in.m_41741_()) - in.m_41613_());
            }
        }
        if (room < count) {
            return false;
        }
        int left = count;
        for (int i = 0; i < size && left > 0; i++) {
            ItemStack in = box.m_8020_(i);
            if (!in.m_41619_() && ItemStack.m_150942_(in, stack)) {
                int add = Math.min(left, Math.max(0, Math.min(max, in.m_41741_()) - in.m_41613_()));
                if (add > 0) {
                    ItemStack grown = in.m_41777_();
                    grown.m_41764_(in.m_41613_() + add);
                    box.m_6836_(i, grown);
                    left -= add;
                }
            }
        }
        for (int i = 0; i < size && left > 0; i++) {
            if (box.m_8020_(i).m_41619_()) {
                int put = Math.min(left, max);
                ItemStack part = stack.m_41777_();
                part.m_41764_(put);
                box.m_6836_(i, part);
                left -= put;
            }
        }
        return left == 0;
    }

    // ---------------------------------------------------------------- unstuck, whatever it is doing at home

    /**
     * 1.3.29: walking anywhere at its home (strolling, to bed, back home), no nearer for a few seconds: it sets off again (its path
     * dropped, whatever walks it picks a new one). 1.3.32 (the owner: it jumped from the top floor to the basement): it is never put
     * anywhere; stuck a few times over, it just stands a moment before walking again.
     */
    static void unstick(EtoNpc npc, EtoBrain.Mind mind, Households.Home house, long now) {
        if (npc.m_21573_().m_26571_() || npc.m_5803_()) {
            mind.stuck = 0;
            mind.stuckAt = now;
            mind.stuckX = npc.m_20185_();
            mind.stuckZ = npc.m_20189_();
            return;
        }
        if (now - mind.stuckAt < Chores.STUCK) {
            return;
        }
        double moved = Math.hypot(npc.m_20185_() - mind.stuckX, npc.m_20189_() - mind.stuckZ);
        mind.stuckAt = now;
        mind.stuckX = npc.m_20185_();
        mind.stuckZ = npc.m_20189_();
        if (moved >= Chores.PROGRESS) {
            mind.stuck = 0;
            return;
        }
        mind.stuck++;
        npc.m_21573_().m_26573_();
        if (mind.stuck >= Chores.REPATHS + 1) {
            mind.stuck = 0;
        }
    }
}
