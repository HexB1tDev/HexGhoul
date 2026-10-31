package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlEntities;
import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.family.Genome;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.NpcMemory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 1.3.10: every child in the world (kept with it, on the overworld), by its id: its name, what it inherited ({@link Genome}), its parents
 * (keys and names, as {@code Pregnancies} has them), when it was born, how far it was taught to fight and when its last lesson was. Its
 * body is a {@link ChildNpc} that knows only its id; the world remakes that body at its home a day after it dies ({@code UniqueNpcs}, its
 * key {@code "child:<id>"}). Born of a pregnancy ({@link #born}), at its mother's feet.
 */
public final class Children {
    static final String NAME = "hexkagune_children";

    public static final class Child {
        public String id = "";
        public String name = "";
        public String genome = "";
        public String mother = "";
        public String motherName = "";
        public String father = "";
        public String fatherName = "";
        public long born;
        public int skill;
        public long lesson = Long.MIN_VALUE;

        public Genome genome() {
            Genome g = Genome.load(this.genome);
            return g == null ? Genome.of(new Genome.Parent(Genome.HAIR, Genome.EYES, Genome.SKIN, false, false),
                new Genome.Parent(Genome.HAIR, Genome.EYES, Genome.SKIN, false, false), new java.util.Random(this.born)) : g;
        }

        public ChildStage stage(long now) {
            return ChildStage.of(ChildStage.days(this.born, now));
        }

        /** Whether {@code key} is one of its parents. */
        public boolean parent(String key) {
            return key != null && (key.equals(this.mother) || key.equals(this.father));
        }

        String save() {
            return String.join("\t", this.id, this.name, this.genome, this.mother, this.motherName, this.father, this.fatherName, Long.toString(this.born),
                Integer.toString(this.skill), Long.toString(this.lesson));
        }

        static Child load(String line) {
            String[] p = line.split("\t", -1);
            if (p.length != 10) {
                return null;
            }
            Child c = new Child();
            try {
                c.id = p[0];
                c.name = p[1];
                c.genome = p[2];
                c.mother = p[3];
                c.motherName = p[4];
                c.father = p[5];
                c.fatherName = p[6];
                c.born = Long.parseLong(p[7]);
                c.skill = Integer.parseInt(p[8]);
                c.lesson = Long.parseLong(p[9]);
            } catch (NumberFormatException bad) {
                return null;
            }
            return c;
        }
    }

    static final class Data extends SavedData {
        final Map<String, Child> all;

        Data(Map<String, Child> all) {
            this.all = all;
        }

        static Data load(CompoundTag tag) {
            Map<String, Child> all = new LinkedHashMap<>();
            for (String line : tag.m_128461_("children").split("\n")) {
                Child c = line.isEmpty() ? null : Child.load(line);
                if (c != null) {
                    all.put(c.id, c);
                }
            }
            return new Data(all);
        }

        @Override
        public CompoundTag m_7176_(CompoundTag tag) {
            StringBuilder out = new StringBuilder();
            for (Child c : this.all.values()) {
                out.append(c.save()).append('\n');
            }
            tag.m_128359_("children", out.toString());
            return tag;
        }
    }

    private Children() {
    }

    /** Called once as the mod starts: births make children, and a dead child's body is remade knowing who it is. */
    public static void init() {
        Pregnancies.onBirth(Children::born);
        UniqueNpcs.kinds("child:", ControlEntities.CHILD, (body, key) -> {
            if (body instanceof ChildNpc child) {
                child.setChild(key.substring("child:".length()));
            }
        });
    }

    private static Data data(ServerLevel level) {
        return UniqueNpcs.overworld(level).m_8895_().m_164861_(Data::load, () -> new Data(new LinkedHashMap<>()), NAME);
    }

    public static Child get(ServerLevel level, String id) {
        return id == null || id.isEmpty() ? null : data(level).all.get(id);
    }

    public static void changed(ServerLevel level) {
        data(level).m_77762_();
    }

    /** Every child {@code key} is a parent of. */
    public static List<Child> of(ServerLevel level, String key) {
        List<Child> out = new ArrayList<>();
        for (Child c : data(level).all.values()) {
            if (c.parent(key)) {
                out.add(c);
            }
        }
        return out;
    }

    /** A pregnancy's end: the child, named, its body at its mother's feet, at home there. */
    static void born(ServerLevel level, LivingEntity mother, Pregnancies.Carried carried, Genome genome) {
        Child child = new Child();
        child.id = Long.toString(level.m_46467_(), 36) + Integer.toString(java.util.concurrent.ThreadLocalRandom.current().nextInt(1296), 36);
        child.name = ChildNames.pick(genome);
        child.genome = genome.save();
        child.mother = carried.mother;
        child.motherName = carried.motherName;
        child.father = carried.father;
        child.fatherName = carried.fatherName;
        child.born = level.m_46467_();
        data(level).all.put(child.id, child);
        changed(level);
        ChildNpc body = ControlEntities.CHILD.get().m_20615_(level);
        if (body != null) {
            body.setChild(child.id);
            body.m_7678_(mother.m_20185_(), mother.m_20186_(), mother.m_20189_(), mother.m_146908_(), 0.0F);
            BlockPos home = body.settle();
            UniqueNpcs.claim(level, "child:" + child.id, body.m_20148_(), home);
            level.m_7967_(body);
        }
        // Their parents remember they have one more.
        for (String parent : new String[] {child.mother, child.father}) {
            ServerPlayer player = Pregnancies.playerOf(level, parent);
            String other = parent.equals(child.mother) ? child.father : child.mother;
            if (player != null && other.startsWith("npc:")) {
                NpcMemoryServer.add(player, other.substring(4), NpcMemory.CHILDREN, 1L);
            }
        }
    }

    /** 1.3.10, for testing: a child {@code ticks} older. */
    public static void age(ServerLevel level, Child child, long ticks) {
        child.born -= ticks;
        changed(level);
    }
}
