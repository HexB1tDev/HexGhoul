package com.hex.kagune.control.server;

import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.Romance;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 1.3.21: a family's home ("This is our home now.", said to a spouse): a spot in a world, with room about it. Their spouse lives there (its
 * home moved there, roaming the whole of it, walking back on its own when it strays), and so do all their children, unless told to follow or
 * to wait somewhere else; at night they find their beds in it ({@code HomeLife}: the double bed for the spouse, single beds for the
 * children). One per player, kept with the world.
 */
public final class Households {
    /** How far about the home its family roams (blocks), how far off it they are walked back, how far off they are simply brought back. */
    public static final int RADIUS = 20;
    public static final double STRAYED = RADIUS + 6.0;
    public static final double LOST = 64.0;
    private static final String NAME = "hexkagune_households";

    /** A family's home: its world, its spot (the block above the floor), how far about it they roam. */
    public record Home(String dimension, int x, int y, int z, int radius) {
        public BlockPos pos() {
            return new BlockPos(this.x, this.y, this.z);
        }

        public boolean in(ServerLevel level) {
            return this.dimension.equals(Households.dimension(level));
        }

        /** True if {@code at} is this home (within a couple of blocks of its spot). */
        public boolean at(BlockPos at) {
            return at != null && Math.abs(at.m_123341_() - this.x) <= 2 && Math.abs(at.m_123342_() - this.y) <= 2 && Math.abs(at.m_123343_() - this.z) <= 2;
        }

        String save() {
            return this.dimension + "," + this.x + "," + this.y + "," + this.z + "," + this.radius;
        }

        static Home load(String text) {
            String[] parts = text.split(",");
            if (parts.length < 5) {
                return null;
            }
            try {
                return new Home(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
            } catch (NumberFormatException bad) {
                return null;
            }
        }
    }

    static final class Data extends SavedData {
        final Map<String, Home> all;

        Data(Map<String, Home> all) {
            this.all = all;
        }

        static Data load(CompoundTag tag) {
            Map<String, Home> all = new LinkedHashMap<>();
            for (String line : tag.m_128461_("homes").split("\n")) {
                int eq = line.indexOf('=');
                if (eq > 0) {
                    Home home = Home.load(line.substring(eq + 1));
                    if (home != null) {
                        all.put(line.substring(0, eq), home);
                    }
                }
            }
            return new Data(all);
        }

        @Override
        public CompoundTag m_7176_(CompoundTag tag) {
            StringBuilder out = new StringBuilder();
            for (Map.Entry<String, Home> e : this.all.entrySet()) {
                out.append(e.getKey()).append('=').append(e.getValue().save()).append('\n');
            }
            tag.m_128359_("homes", out.toString());
            return tag;
        }
    }

    private Households() {
    }

    private static Data data(ServerLevel level) {
        return UniqueNpcs.overworld(level).m_8895_().m_164861_(Data::load, () -> new Data(new LinkedHashMap<>()), NAME);
    }

    static String dimension(ServerLevel level) {
        return level.m_46472_().toString().replace(',', ';'); // ResourceKey[minecraft:dimension / minecraft:overworld]
    }

    /** {@code key}'s home ({@code "player:<uuid>"}), or null. */
    public static Home of(ServerLevel level, String key) {
        return key == null ? null : data(level).all.get(key);
    }

    public static Home of(ServerLevel level, UUID player) {
        return player == null ? null : of(level, Pregnancies.player(player));
    }

    /**
     * Their home is here now ({@code at}: where they stand): theirs, their spouse's (moved there), their children's (each told as it next
     * thinks: {@link #forChild}).
     */
    public static Home set(ServerLevel level, ServerPlayer player, BlockPos at) {
        Home home = new Home(dimension(level), at.m_123341_(), at.m_123342_() + 1, at.m_123343_(), RADIUS);
        data(level).all.put(Pregnancies.player(player.m_20148_()), home);
        data(level).m_77762_();
        HomeLife.forget(home);
        return home;
    }

    /** The home a child lives in: one of its parents' (a player's), in this world; null if neither has one here. */
    public static Home forChild(ServerLevel level, Children.Child child) {
        if (child == null) {
            return null;
        }
        for (String parent : new String[] {child.mother, child.father}) {
            Home home = parent != null && parent.startsWith("player:") ? of(level, parent) : null;
            if (home != null && home.in(level)) {
                return home;
            }
        }
        return null;
    }

    /** The home a companion lives in: the one it shares with its spouse (a player), if its own home is there; else null. */
    public static Home forCompanion(ServerLevel level, EtoNpc npc) {
        UUID partner = npc.mind == null ? null : EtoBrain.spouse(npc);
        Home home = of(level, partner);
        return home != null && home.in(level) && home.at(npc.home()) ? home : null;
    }

    /** True if a child of this home is one of {@code npc}'s (the companion is one of its parents). */
    static boolean parentOf(ServerLevel level, EtoNpc npc, ChildNpc child) {
        Children.Child known = Children.get(level, child.childId());
        return known != null && known.parent(Pregnancies.npc(npc.persona().key()));
    }

    /** True for a married couple (a player and their companion). */
    static boolean married(Romance.Stage stage) {
        return stage == Romance.Stage.MARRIED;
    }
}
