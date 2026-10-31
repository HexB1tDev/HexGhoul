package com.hex.kagune.control.server;

import com.hex.kagune.control.family.HomeRules;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.EtoNpc;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.21: a family's nights at home ({@link Households}): the beds in it, found by looking (every bed head within its reach; two side by
 * side facing the same way are a double bed), the double bed its spouse's, the single beds its children's (one each, the eldest nearest
 * home first; a child never takes the double bed). At night each walks to its own, lies down in it ({@code startSleeping}: drawn lying in
 * it on every client, the bed marked taken), and gets up in the morning; or sooner, hurt, talked to, picked up, or its bed gone.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class HomeLife {
    /** How far below and above the home's spot beds are looked for (its radius across). */
    static final int BELOW = 5;
    static final int ABOVE = 7;
    /** How long what was found is trusted before it is looked for again (ticks). */
    static final long RESCAN = 600L;
    /** How close to its bed it must be to lie down in it (blocks, to the bed's middle). */
    static final double LIE_DOWN = 1.7;

    /** Every bed of a home as last found, when. */
    record Plan(long at, HomeRules.Plan beds) {
    }

    private static final Map<String, Plan> PLANS = new HashMap<>();
    private static final Map<Object, Boolean> BEDS = new IdentityHashMap<>();

    private HomeLife() {
    }

    /** True at night for a child ({@code child}) or a spouse: from its bedtime until the morning. */
    public static boolean night(long dayTime, boolean child) {
        return HomeRules.night(dayTime, child);
    }

    /** The home moved (or set): its beds looked for again. */
    static void forget(Households.Home home) {
        PLANS.remove(home.save());
    }

    // ---------------------------------------------------------------- finding beds

    /** {@code block} is a bed (any bed: its registry name ends in "_bed"), known once per kind of block. */
    static boolean isBed(Object block) {
        return BEDS.computeIfAbsent(block, b -> HomeRules.bedBlock(String.valueOf(b)));
    }

    static BlockPos pos(HomeRules.Head head) {
        return head == null ? null : new BlockPos(head.x(), head.y(), head.z());
    }

    /** The beds of {@code home} (looked for again every {@link #RESCAN} ticks; only in loaded ground). */
    static HomeRules.Plan plan(ServerLevel level, Households.Home home, long now) {
        Plan plan = PLANS.get(home.save());
        if (plan != null && now - plan.at() < RESCAN && now >= plan.at()) {
            return plan.beds();
        }
        List<HomeRules.Head> heads = new ArrayList<>();
        BlockPos centre = home.pos();
        int r = home.radius();
        for (int dy = -BELOW; dy <= ABOVE; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos at = new BlockPos(centre.m_123341_() + dx, centre.m_123342_() + dy, centre.m_123343_() + dz);
                    if (!level.m_46805_(at)) {
                        continue;
                    }
                    BlockState state = level.m_8055_(at);
                    if (!isBed(state.m_60734_())) {
                        continue;
                    }
                    String text = state.toString();
                    int facing = HomeRules.facing(text);
                    if (HomeRules.head(text) && facing >= 0) {
                        heads.add(new HomeRules.Head(at.m_123341_(), at.m_123342_(), at.m_123343_(), facing));
                    }
                }
            }
        }
        HomeRules.Plan beds = HomeRules.pair(heads, centre.m_123341_(), centre.m_123342_(), centre.m_123343_());
        PLANS.put(home.save(), new Plan(now, beds));
        return beds;
    }

    /** True if a bed head is still there at {@code pos} and no one (a player, another) lies in it. */
    static boolean free(ServerLevel level, BlockPos pos) {
        if (!level.m_46805_(pos)) {
            return false;
        }
        BlockState state = level.m_8055_(pos);
        if (!isBed(state.m_60734_())) {
            return false;
        }
        String text = state.toString();
        return HomeRules.head(text) && !HomeRules.occupied(text);
    }

    /** True if {@code pos} is still a bed head. */
    static boolean bedAt(ServerLevel level, BlockPos pos) {
        if (pos == null || !level.m_46805_(pos)) {
            return false;
        }
        BlockState state = level.m_8055_(pos);
        return isBed(state.m_60734_()) && HomeRules.head(state.toString());
    }

    /**
     * The bed a spouse sleeps in: a free half of the home's double bed (the other half is theirs); none, a single bed left over after the
     * children have theirs. Null if there is none.
     */
    static BlockPos spouseBed(ServerLevel level, Households.Home home, long now) {
        HomeRules.Plan plan = plan(level, home, now);
        for (HomeRules.Bed bed : plan.doubles()) {
            if (free(level, pos(bed.head()))) {
                return pos(bed.head());
            }
            if (free(level, pos(bed.pair()))) {
                return pos(bed.pair());
            }
        }
        int kids = children(level, home).size();
        for (int i = kids; i < plan.singles().size(); i++) {
            BlockPos single = pos(plan.singles().get(i).head());
            if (free(level, single)) {
                return single;
            }
        }
        return null;
    }

    /** The single bed a child sleeps in: its own (the children of the home in order of birth, the beds nearest home first), or null. */
    static BlockPos childBed(ServerLevel level, Households.Home home, Children.Child child, long now) {
        HomeRules.Plan plan = plan(level, home, now);
        List<Children.Child> kids = children(level, home);
        int index = -1;
        for (int i = 0; i < kids.size(); i++) {
            if (kids.get(i).id.equals(child.id)) {
                index = i;
            }
        }
        return pos(HomeRules.childBed(plan, index));
    }

    /** The children living in {@code home}, eldest first. */
    static List<Children.Child> children(ServerLevel level, Households.Home home) {
        List<Children.Child> out = new ArrayList<>();
        for (Children.Child child : Children.all(level)) {
            Households.Home theirs = Households.forChild(level, child);
            if (theirs != null && theirs.save().equals(home.save())) {
                out.add(child);
            }
        }
        out.sort(Comparator.comparingLong(c -> c.born));
        return out;
    }

    // ---------------------------------------------------------------- going to bed, getting up

    /** Walks {@code body} to {@code bed} and lies it down there once close; true while it is on its way or lying down now. */
    static boolean toBed(net.minecraft.world.entity.PathfinderMob body, BlockPos bed, double speed) {
        double dx = bed.m_123341_() + 0.5 - body.m_20185_();
        double dy = bed.m_123342_() - body.m_20186_();
        double dz = bed.m_123343_() + 0.5 - body.m_20189_();
        if (dx * dx + dz * dz <= LIE_DOWN * LIE_DOWN && Math.abs(dy) < 2.0) {
            body.m_21573_().m_26573_();
            body.m_5802_(bed);
            return true;
        }
        body.m_21573_().m_26519_(bed.m_123341_() + 0.5, bed.m_123342_(), bed.m_123343_() + 0.5, speed);
        return true;
    }

    /**
     * Each tick of a sleeping companion or child (its own mind is still while it sleeps: its body can't move): it gets up in the morning,
     * hurt, talked to, picked up, its bed gone, or its home moved away.
     */
    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.PathfinderMob body) || !(body.m_9236_() instanceof ServerLevel level)
            || !body.m_5803_()) {
            return;
        }
        try {
            long now = level.m_46467_();
            long day = level.m_46468_();
            if (body instanceof EtoNpc && ((EtoNpc)body).mind == null || body instanceof ChildNpc && ((ChildNpc)body).mind == null) {
                // Asleep as it was saved (the world reloaded): its mind wakes with it, so it gets up now (its bed is its own again tonight).
                wake(body);
                return;
            }
            if (body instanceof EtoNpc npc && npc.mind != null) {
                EtoBrain.Mind mind = npc.mind;
                boolean up = !night(day, false) || now - mind.hurtAt < 3L || mind.talking != null || !bedAt(level, mind.bed)
                    || Households.forCompanion(level, npc) == null;
                if (up) {
                    wake(npc);
                    mind.bed = null;
                }
            } else if (body instanceof ChildNpc child && child.mind != null) {
                ChildBrain.Mind mind = child.mind;
                boolean nap = now < mind.napUntil;
                boolean up = !night(day, true) && !nap || now - mind.hurtAt < 3L || mind.carrier != null || !bedAt(level, mind.bed);
                if (up) {
                    wake(child);
                    ChildBrain.woke(child);
                    mind.bed = null;
                    mind.napUntil = Long.MIN_VALUE;
                } else {
                    ChildBrain.sleeping(child, level, now);
                }
            }
        } catch (RuntimeException failure) {
            com.hex.kagune.control.Safety.caught(failure, "a sleeper's night");
        }
    }

    /** It gets up (stepping off its bed, the bed freed). */
    static void wake(net.minecraft.world.entity.LivingEntity body) {
        if (body.m_5803_()) {
            body.m_5796_();
        }
    }
}
