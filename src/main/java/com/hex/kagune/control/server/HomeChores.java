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
 * Each chore is a short round of a few blocks of its kind; on the way, if it gets nowhere for a few seconds it sets off again, and if it
 * is hopelessly stuck it is put beside what it was going to; a block it can't get to is given up. Anything else (a fight, a talk, a kiss,
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

        Job(Chores.Kind kind, List<BlockPos> round, long now) {
            this.kind = kind;
            this.round = round;
            this.walkSince = now;
            this.fromAt = now;
        }

        BlockPos at() {
            return this.index < this.round.size() ? this.round.get(this.index) : null;
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
            if (!still(level, at, job.kind)) {
                next(job, now);
                return true;
            }
            if (close(npc, at, job.index == job.box ? Chores.Kind.TIDY : job.kind)) {
                work(npc, mind, job, level, at, now);
            } else {
                walk(npc, mind, job, house, level, at, now);
            }
            return true;
        }
        // At it: facing it, hands busy, until the gesture is done (or something else took its hands: then on to the next).
        npc.m_21573_().m_26573_();
        face(npc, at);
        long age = now - job.workSince;
        if (age % 10L == 0L && job.doing == job.kind.gesture) {
            particles(level, job.kind, at);
        }
        if (mind.gesture != job.doing) {
            shut(level, job);
            next(job, now);
        } else if (age >= job.doing.ticks - 4L) {
            job.did |= effect(npc, job, level, at, house, now);
            shut(level, job);
            next(job, now);
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
        Chores.Kind kind = Chores.pick(counts, mind.lastChore, !items.isEmpty(), npc.m_217043_().m_188500_());
        if (kind == null) {
            return null;
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
        }
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

    /** True if {@code at} is still something this chore is for (a swept spot always is). */
    private static boolean still(ServerLevel level, BlockPos at, Chores.Kind kind) {
        if (kind == Chores.Kind.SWEEP) {
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
     * On its way: set on its path again every half second; no nearer in a few seconds, it sets off again; stuck that way a few times, it
     * is put beside the block (inside the home); no way there at all, or too long on the way, the block is given up.
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
                    BlockPos stand = besideBlock(level, at, house);
                    if (stand != null && !crowded(npc, level)) {
                        npc.m_6021_(stand.m_123341_() + 0.5, stand.m_123342_(), stand.m_123343_() + 0.5);
                    } else {
                        next(job, now);
                        return;
                    }
                    job.repaths = 0;
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

    /** True if a player stands right by it (pressed in a corner by someone is not stuck: nobody is moved through them). */
    private static boolean crowded(EtoNpc npc, ServerLevel level) {
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_20280_(npc) < 2.5 * 2.5) {
                return true;
            }
        }
        return false;
    }

    /** A spot to stand beside {@code at} (one of its four sides, the same height or one off), inside the home; null if none. */
    private static BlockPos besideBlock(ServerLevel level, BlockPos at, Households.Home house) {
        int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int dy = 0; dy >= -1; dy--) {
            for (int[] side : sides) {
                BlockPos spot = off(at, side[0], dy, side[1]);
                if (standable(level, spot) && Math.abs(spot.m_123341_() - house.x()) <= house.radius() && Math.abs(spot.m_123343_() - house.z()) <= house.radius()) {
                    return spot;
                }
            }
        }
        return standable(level, at) ? at : null;
    }

    // ---------------------------------------------------------------- the work

    /** It has got there: it stops, faces it, its hands at it (a chest's lid up). */
    private static void work(EtoNpc npc, EtoBrain.Mind mind, Job job, ServerLevel level, BlockPos at, long now) {
        npc.m_21573_().m_26573_();
        face(npc, at);
        job.doing = job.index == job.box ? EtoGesture.TIDY : job.kind.gesture;
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
     * dropped, whatever walks it picks a new one); stuck that way a few times over, it is put back on the home's spot (unless someone
     * stands right by it).
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
        if (mind.stuck >= Chores.REPATHS + 1 && npc.m_9236_() instanceof ServerLevel level && !crowded(npc, level)) {
            mind.stuck = 0;
            double[] feet = npc.homeFeet();
            npc.m_6021_(feet[0], feet[1], feet[2]);
        }
    }
}
