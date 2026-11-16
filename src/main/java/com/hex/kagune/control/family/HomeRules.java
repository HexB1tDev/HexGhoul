package com.hex.kagune.control.family;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 1.3.21: a family's nights at home (pure): when each goes to bed and gets up, and which beds are whose. A bed head is read off its state as
 * the game writes it ("...[facing=north,occupied=false,part=head]"); two heads side by side facing the same way are one double bed (its
 * spouse's: the other half is the player's), every other bed a single bed (the children's, nearest home first).
 */
public final class HomeRules {
    /** The night (day ticks): children go to bed from {@link #KIDS_BED}, a spouse from {@link #SPOUSE_BED}; everyone is up at {@link #WAKE}. */
    public static final long KIDS_BED = 12600L;
    public static final long SPOUSE_BED = 13400L;
    public static final long WAKE = 23300L;

    private HomeRules() {
    }

    /** A bed head: where (block), facing which way (0 north, 1 east, 2 south, 3 west). */
    public record Head(int x, int y, int z, int facing) {
        long key() {
            return ((long)this.x & 0x3FFFFFFL) << 38 | ((long)this.z & 0x3FFFFFFL) << 12 | (long)this.y & 0xFFFL;
        }
    }

    /** A bed: its head, and the head of its other half if it is a double bed (null: a single bed). */
    public record Bed(Head head, Head pair) {
        public boolean doubled() {
            return this.pair != null;
        }
    }

    /** A home's beds: its double beds and its single beds, nearest its spot first. */
    public record Plan(List<Bed> doubles, List<Bed> singles) {
    }

    /** True at night for a child ({@code child}) or a spouse: from its bedtime until the morning. */
    /**
     * 2.0.7 (the owner: "I want them to be able to go outside the max radius, just with the feeling that by night time they have to be
     * home"): how far past its home's radius one may roam by day: a spouse ({@code null}), a teen or a grown child {@link #ROAM}, a child
     * half that, a toddler and a baby not at all.
     */
    public static int roam(ChildStage stage) {
        if (stage == null || stage.older()) {
            return ROAM;
        }
        return stage == ChildStage.CHILD ? ROAM / 2 : 0;
    }

    /** 2.0.7: by day, farther out. */
    public static final int ROAM = 24;

    /** 2.0.7: its leash now: its home's {@code radius} at night (home by then), that and its {@code roam} by day. */
    public static int leash(int radius, boolean night, int roam) {
        return night ? radius : radius + roam;
    }

    /** 2.0.7: looks (every 10 ticks) its body may be stuck inside blocks before it is put home (3 s). */
    public static final int WALLED = 6;
    /** 2.0.7: how long it may get no nearer home, out when it should be home, before it is put there (unseen): stuck, not just slow. */
    public static final long NO_HEADWAY = 2400L;

    /** 2.0.7: true if it should be put home: no nearer by 2 blocks than {@code best} (when it was, {@code since}) for {@link #NO_HEADWAY}. */
    public static boolean noHeadway(double best, double far, long since, long now) {
        return since != Long.MIN_VALUE && far > best - 2.0 && now - since >= NO_HEADWAY;
    }

    public static boolean night(long dayTime, boolean child) {
        long t = Math.floorMod(dayTime, 24000L);
        return t >= (child ? KIDS_BED : SPOUSE_BED) && t < WAKE;
    }

    /** True if a block (as the game writes it: "Block{minecraft:red_bed}") is a bed. */
    public static boolean bedBlock(String block) {
        return block != null && block.endsWith("_bed}");
    }

    /** Which way a bed state faces (0 north, 1 east, 2 south, 3 west; -1 none). */
    public static int facing(String state) {
        return state.contains("facing=north") ? 0 : state.contains("facing=east") ? 1 : state.contains("facing=south") ? 2 : state.contains("facing=west") ? 3 : -1;
    }

    public static boolean head(String state) {
        return state.contains("part=head");
    }

    public static boolean occupied(String state) {
        return state.contains("occupied=true");
    }

    /** Where the other half of a double bed would be: the head beside {@code head}, to one side ({@code which} -1 or 1) of the way it faces. */
    static Head side(Head head, int which) {
        return head.facing() == 0 || head.facing() == 2 ? new Head(head.x() + which, head.y(), head.z(), head.facing())
            : new Head(head.x(), head.y(), head.z() + which, head.facing());
    }

    /** The beds of {@code heads}: side by side and facing the same way, a double bed (listed once); the rest single; nearest the spot first. */
    public static Plan pair(List<Head> heads, int cx, int cy, int cz) {
        Map<Long, Head> at = new HashMap<>();
        for (Head head : heads) {
            at.put(head.key(), head);
        }
        List<Bed> doubles = new ArrayList<>();
        List<Bed> singles = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        for (Head head : heads) {
            if (used.contains(head.key())) {
                continue;
            }
            used.add(head.key());
            Head partner = null;
            for (int which : new int[] {-1, 1}) {
                Head other = at.get(side(head, which).key());
                if (other != null && other.facing() == head.facing() && !used.contains(other.key())) {
                    partner = other;
                    break;
                }
            }
            if (partner != null) {
                used.add(partner.key());
                doubles.add(new Bed(head, partner));
            } else {
                singles.add(new Bed(head, null));
            }
        }
        Comparator<Bed> nearest = Comparator.comparingDouble(b -> {
            double dx = b.head().x() - cx;
            double dy = b.head().y() - cy;
            double dz = b.head().z() - cz;
            return dx * dx + dy * dy + dz * dz;
        });
        doubles.sort(nearest);
        singles.sort(nearest);
        return new Plan(doubles, singles);
    }

    /** The single bed for the {@code index}th child of a home (eldest first), or null if there are fewer beds; never a double bed. */
    public static Head childBed(Plan plan, int index) {
        return index < 0 || index >= plan.singles().size() ? null : plan.singles().get(index).head();
    }
}
