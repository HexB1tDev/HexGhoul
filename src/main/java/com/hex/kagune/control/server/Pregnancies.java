package com.hex.kagune.control.server;

import com.hex.kagune.control.family.FamilyLines;
import com.hex.kagune.control.family.Genome;
import com.hex.kagune.control.family.Pregnancy;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CCutscene;
import com.hex.kagune.control.network.S2CNpcAlert;
import com.hex.kagune.control.network.S2CPregnancy;
import com.hex.kagune.control.npc.Romance;
import com.hex.kagune.control.npc.UniqueBody;
import com.hex.kagune.control.npc.UniqueNpc;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.10: every pregnancy in the world (kept with it, on the overworld), for any woman: a female player ({@code "player:<uuid>"}) or a
 * one-of-a-kind NPC ({@code "npc:<key>"}, Eto's {@code "npc:eto"}), by her partner, since the night she conceived, with what the child
 * will be ({@link Genome}, decided then). Each client that sees her is told (her belly, {@link S2CPregnancy}); her partner is told
 * privately as her time goes on ({@link Pregnancy#ALERTS}); two Minecraft days on, wherever she is, the birth: the cutscene ("9 months
 * later...") for her partner (and her, a player) and the child ({@link #onBirth}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class Pregnancies {
    static final String NAME = "hexkagune_family";

    /** One pregnancy: who carries, by whom (keys and names), since when, what the child will be, which alerts were given (bits). */
    public static final class Carried {
        public String mother = "";
        public String motherName = "";
        public String father = "";
        public String fatherName = "";
        public long conceived;
        public String genome = "";
        public int alerted;

        String save() {
            return String.join("\t", this.mother, this.motherName, this.father, this.fatherName, Long.toString(this.conceived), this.genome,
                Integer.toString(this.alerted));
        }

        static Carried load(String line) {
            String[] p = line.split("\t", -1);
            if (p.length != 7) {
                return null;
            }
            Carried c = new Carried();
            try {
                c.mother = p[0];
                c.motherName = p[1];
                c.father = p[2];
                c.fatherName = p[3];
                c.conceived = Long.parseLong(p[4]);
                c.genome = p[5];
                c.alerted = Integer.parseInt(p[6]);
            } catch (NumberFormatException bad) {
                return null;
            }
            return c;
        }
    }

    /** What is done when a child is born (the child's own systems make it: {@code ChildServer}). */
    public interface Birth {
        void born(ServerLevel level, LivingEntity mother, Carried carried, Genome genome);
    }

    private static final List<Birth> BIRTHS = new ArrayList<>();

    static final class Data extends SavedData {
        final Map<String, Carried> all;

        Data(Map<String, Carried> all) {
            this.all = all;
        }

        static Data load(CompoundTag tag) {
            Map<String, Carried> all = new LinkedHashMap<>();
            for (String line : tag.m_128461_("carried").split("\n")) {
                Carried c = line.isEmpty() ? null : Carried.load(line);
                if (c != null) {
                    all.put(c.mother, c);
                }
            }
            return new Data(all);
        }

        @Override
        public CompoundTag m_7176_(CompoundTag tag) {
            StringBuilder out = new StringBuilder();
            for (Carried c : this.all.values()) {
                out.append(c.save()).append('\n');
            }
            tag.m_128359_("carried", out.toString());
            return tag;
        }
    }

    private Pregnancies() {
    }

    public static void onBirth(Birth birth) {
        BIRTHS.add(birth);
    }

    public static String npc(String key) {
        return "npc:" + key;
    }

    public static String player(UUID id) {
        return "player:" + id;
    }

    /** The key of {@code body} as a mother or father: a player's, or a one-of-a-kind NPC's; null for anyone else. */
    public static String keyOf(Entity body) {
        if (body instanceof Player player) {
            return player(player.m_20148_());
        }
        return body instanceof UniqueBody unique ? npc(unique.uniqueKey()) : null;
    }

    private static Data data(ServerLevel level) {
        return UniqueNpcs.overworld(level).m_8895_().m_164861_(Data::load, () -> new Data(new LinkedHashMap<>()), NAME);
    }

    /** {@code mother}'s pregnancy, or null. */
    public static Carried get(ServerLevel level, String mother) {
        return mother == null ? null : data(level).all.get(mother);
    }

    public static boolean pregnant(ServerLevel level, String mother) {
        return get(level, mother) != null;
    }

    /** How far along {@code mother} is (0..1), or -1 if she isn't carrying. */
    public static double progress(ServerLevel level, String mother) {
        Carried c = get(level, mother);
        return c == null ? -1.0 : Pregnancy.progress(c.conceived, level.m_46467_());
    }

    /** She conceived tonight, by {@code father}: the child decided now. */
    public static void conceive(ServerLevel level, String mother, String motherName, String father, String fatherName, Genome genome) {
        if (pregnant(level, mother)) {
            return;
        }
        Carried c = new Carried();
        c.mother = mother;
        c.motherName = motherName;
        c.father = father;
        c.fatherName = fatherName;
        c.conceived = level.m_46467_();
        c.genome = genome.save();
        data(level).all.put(mother, c);
        data(level).m_77762_();
        sync(level, mother);
    }

    /** 1.3.10, for testing: every pregnancy {@code key} is part of (mother or father) brought to {@code left} ticks before the birth. */
    public static int hurry(ServerLevel level, String key, long left) {
        int hurried = 0;
        for (Carried c : data(level).all.values()) {
            if (c.mother.equals(key) || c.father.equals(key)) {
                c.conceived = level.m_46467_() - Pregnancy.TERM + left;
                c.alerted = (1 << Pregnancy.ALERTS.length) - 1;
                hurried++;
                sync(level, c.mother);
            }
        }
        data(level).m_77762_();
        return hurried;
    }

    /** Where {@code key} is in the world now (a player online; a one-of-a-kind NPC's body, wherever it is loaded), or null. */
    public static LivingEntity body(ServerLevel any, String key) {
        if (key == null) {
            return null;
        }
        if (key.startsWith("player:")) {
            return playerOf(any, key);
        }
        if (key.startsWith("npc:")) {
            UniqueNpc npc = UniqueNpcs.get(any, key.substring(4));
            if (npc.body == null || !npc.alive || any.m_7654_() == null) {
                return null;
            }
            for (ServerLevel level : any.m_7654_().m_129785_()) {
                if (level.m_8791_(npc.body) instanceof LivingEntity body && body.m_6084_()) {
                    return body;
                }
            }
        }
        return null;
    }

    /** The player {@code key} names, if online. */
    public static ServerPlayer playerOf(ServerLevel any, String key) {
        if (key == null || !key.startsWith("player:") || any.m_7654_() == null) {
            return null;
        }
        try {
            return any.m_7654_().m_6846_().m_11259_(UUID.fromString(key.substring(7)));
        } catch (IllegalArgumentException bad) {
            return null;
        }
    }

    /** Every client that sees her told how she is (her belly). */
    static void sync(ServerLevel level, String mother) {
        LivingEntity body = body(level, mother);
        if (body != null) {
            Carried c = get(level, mother);
            ControlNetwork.broadcastAround(body, new S2CPregnancy(body.m_20148_(), c == null ? Long.MIN_VALUE : c.conceived));
        }
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.m_46472_() != Level.f_46428_
            || level.m_46467_() % 20L != 0L) {
            return;
        }
        Data data = data(level);
        if (data.all.isEmpty()) {
            return;
        }
        long now = level.m_46467_();
        for (Carried c : new ArrayList<>(data.all.values())) {
            double along = Pregnancy.progress(c.conceived, now);
            for (int i = 0; i < Pregnancy.ALERTS.length; i++) {
                if ((c.alerted & 1 << i) == 0 && along >= Pregnancy.ALERTS[i]) {
                    c.alerted |= 1 << i;
                    data.m_77762_();
                    String text = FamilyLines.alert(i, (int)(now / 20L));
                    for (String who : new String[] {c.father, c.mother}) {
                        ServerPlayer player = playerOf(level, who);
                        if (player != null) {
                            ControlNetwork.sendTo(player, new S2CNpcAlert(who.equals(c.mother) ? "You" : c.motherName, Romance.Want.TALK.ordinal(),
                                who.equals(c.mother) ? youAlert(i) : text));
                        }
                    }
                }
            }
            if (Pregnancy.due(c.conceived, now)) {
                LivingEntity mother = body(level, c.mother);
                if (mother != null && mother.m_9236_() instanceof ServerLevel there) {
                    birth(there, mother, c);
                }
            }
        }
    }

    /** What a mother who plays is told herself as her time goes on. */
    static String youAlert(int index) {
        return switch (index) {
            case 0 -> "felt the baby kick.";
            case 1 -> "are heavy and tired. The baby is growing.";
            default -> "feel it: it's close now.";
        };
    }

    /** The birth: the pregnancy over, the cutscene, the child. */
    static void birth(ServerLevel level, LivingEntity mother, Carried c) {
        Data data = data(level);
        data.all.remove(c.mother);
        data.m_77762_();
        ControlNetwork.broadcastAround(mother, new S2CPregnancy(mother.m_20148_(), Long.MIN_VALUE));
        Genome genome = Genome.load(c.genome);
        if (genome == null) {
            genome = Genome.of(new Genome.Parent(Genome.HAIR, Genome.EYES, Genome.SKIN, false, false),
                new Genome.Parent(Genome.HAIR, Genome.EYES, Genome.SKIN, false, false), new java.util.Random(c.conceived));
        }
        String child = ChildNames.pick(genome);
        for (String who : new String[] {c.father, c.mother}) {
            ServerPlayer player = playerOf(level, who);
            if (player == null) {
                continue;
            }
            if (player.m_9236_() == mother.m_9236_() && player.m_20280_(mother) <= 96.0 * 96.0) {
                ControlNetwork.sendTo(player, new S2CCutscene(S2CCutscene.BIRTH, FamilyLines.born(c.motherName, genome.gender().female(), child)));
            } else {
                ControlNetwork.sendTo(player, new S2CNpcAlert(c.motherName, Romance.Want.TALK.ordinal(), who.equals(c.mother) ? "gave birth."
                    : "had the baby. " + (genome.gender().female() ? "A girl" : "A boy") + ". Come home."));
            }
        }
        for (Birth birth : BIRTHS) {
            birth.born(level, mother, c, genome);
        }
    }

    /** A player starts seeing a woman who is carrying: they are told. */
    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof LivingEntity body && viewer.m_9236_() instanceof ServerLevel level) {
            Carried c = get(level, keyOf(body));
            if (c != null) {
                ControlNetwork.sendTo(viewer, new S2CPregnancy(body.m_20148_(), c.conceived));
            }
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.m_9236_() instanceof ServerLevel level) {
            Carried c = get(level, player(player.m_20148_()));
            if (c != null) {
                ControlNetwork.sendTo(player, new S2CPregnancy(player.m_20148_(), c.conceived));
            }
        }
    }
}
