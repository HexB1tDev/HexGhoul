package com.hex.kagune.control.server;

import com.hex.kagune.control.Safety;
import com.hex.kagune.control.npc.CcgAgent;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.GangPlayer;
import com.hex.kagune.control.npc.GhoulNpc;
import com.hex.kagune.control.npc.JoinTrial;
import com.hex.kagune.control.npc.JoinVoice;
import com.hex.kagune.control.npc.NpcIdentity;
import com.hex.kagune.control.npc.NpcTemper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 4.1.79: the gangs' ways in, their trials held live ({@link JoinTrial}'s rules, {@link JoinVoice}'s words): Ayato's spar, Aogiri's ambush,
 * Roma's ring, Banjou's gas and gate, Anteiku's shift and raid, the CCG's hunt. One a player at a time, begun from the leader's talk
 * ({@link #start}), followed every quarter second ({@link #tick}), won or lost ({@link #end}); a win moves the way in on
 * ({@link JoinTrial#after}). Never saved: logged out, dead or gone off its ground, it is lost, and the leader asked again.
 *
 * <p>The bodies a trial makes (the patrol, the volunteers, the Clowns, the regulars, the raid, the partner and the reported ghoul) carry its
 * mark ({@link #MARK}, whose trial; {@link #ROLE}, what they are in it), go with it ({@link #clear}), and any found with no trial live (a
 * restart, a trial lost while their ground was unloaded) are taken away by their own brains ({@link #orphan}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class JoinServer {
    /** On each body a trial makes: whose trial it is (the player's id, as text), and what it is in it. */
    static final String MARK = "HexJoinTrial";
    static final String ROLE = "HexJoinRole";
    /** A café raider who broke off and runs (counted; nothing lands on him while he goes). */
    static final String BROKE = "HexJoinBroke";
    /** A regular's seat (block), kept on them. */
    static final String SEAT = "HexJoinSeat";
    /** Kept with the player: the day Ken last handed over the makings; the day of a failed academy test; Roma's mask lent. */
    static final String MAKINGS_DAY = "HexJoinMakings";
    static final String EXAM_FAILED = "HexJoinExamFailed";
    static final String LENT = "HexJoinMaskLent";

    static final String PATROL = "patrol";
    static final String VOLUNTEER = "volunteer";
    static final String RAIDER = "raider";
    static final String REGULAR = "regular";
    static final String PARTNER = "partner";
    static final String TARGET = "target";

    /** One trial, live. */
    static final class Live {
        final JoinTrial.Kind kind;
        final UUID player;
        final long began;
        final BlockPos anchor;
        /** The leader's body (Ayato, for the spar), by its id now. */
        int leader;
        /** The ambush's first blow; the gas's start (Long.MIN_VALUE: not yet). */
        long first = Long.MIN_VALUE;
        final List<UUID> marked = new ArrayList<>();
        /** The act on stage, the regular waiting; since when. */
        UUID current;
        long currentAt;
        long nextAt;
        int done;
        int strikes;
        int wave;
        long waveAt;
        int outside;
        UUID partner;
        UUID target;
        int seed;
        /** 4.1.80: the companion whose courtship this is (Ayato's spar, asked for as a chapter: no gang's step), or null. */
        String courting;

        Live(JoinTrial.Kind kind, UUID player, long began, BlockPos anchor) {
            this.kind = kind;
            this.player = player;
            this.began = began;
            this.anchor = anchor;
        }

        /** The spar's fight is on (past its count). */
        boolean fighting(long now) {
            return now >= this.began + JoinTrial.SPAR_COUNTDOWN;
        }
    }

    /** Each player's live trial. */
    static final Map<UUID, Live> LIVE = new ConcurrentHashMap<>();
    /** Whose trial each body made for one is in. */
    static final Map<UUID, UUID> OWNER = new ConcurrentHashMap<>();
    /** The academy's test being sat: the three questions, then how many answered. */
    static final Map<UUID, int[]> EXAMS = new ConcurrentHashMap<>();

    private JoinServer() {
    }

    /** The trial {@code player} has live, or null. */
    static JoinTrial.Kind live(ServerPlayer player) {
        Live live = LIVE.get(player.m_20148_());
        return live == null ? null : live.kind;
    }

    // ---------------------------------------------------------------- begun

    /**
     * {@code kind} begun for {@code player} at {@code leader}'s word: its ground taken, its bodies made. What the leader says (their "start",
     * or why not now: another trial live, a show by day, too hurt for a spar, nowhere to hold it).
     */
    static String start(ServerPlayer player, EtoNpc leader, JoinTrial.Kind kind, int seed) {
        return start(player, leader, kind, seed, null);
    }

    /**
     * 4.1.80: Ayato's spar as a chapter of his courtship (the plan: "Ayato's a spar at the hideout, JoinServer's spar can be reused with no
     * gang step"): the same fight, its end counted on the courtship ({@link #end}), its words his romance's (CourtVoice).
     */
    static String court(ServerPlayer player, EtoNpc companion, int seed) {
        return start(player, companion, JoinTrial.Kind.SPAR, seed, companion.persona().key());
    }

    static String start(ServerPlayer player, EtoNpc leader, JoinTrial.Kind kind, int seed, String courting) {
        UUID id = player.m_20148_();
        Live running = LIVE.get(id);
        if (running != null) {
            return running.kind == kind ? JoinVoice.say(kind, "on", seed) : "*They look you over.* You're in the middle of something else. Finish it first.";
        }
        ServerLevel level = player.m_284548_();
        long now = level.m_46467_();
        BlockPos anchor = anchor(level, leader, kind);
        Live live = new Live(kind, id, now, anchor);
        live.seed = seed;
        live.courting = courting;
        switch (kind) {
            case SPAR -> {
                if (leader == null || leader.mind == null) {
                    return courting != null ? com.hex.kagune.control.npc.CourtVoice.spar("died", seed) : JoinVoice.say(kind, "died", seed);
                }
                if (player.m_21223_() < player.m_21233_() * 0.5F) {
                    return courting != null ? com.hex.kagune.control.npc.CourtVoice.spar("hurt", seed)
                        : "*He looks you up and down.* You're half dead already. Eat something. Then come back and I'll finish the job.";
                }
                live.leader = leader.m_19879_();
                leader.m_21153_(leader.m_21233_()); // a fair fight: he starts it whole
            }
            case AMBUSH -> {
                if (!patrol(level, player, live)) {
                    return "*He scowls at the horizon.* No doves out tonight. Ask me again in a bit.";
                }
            }
            case RING -> {
                if (!JoinTrial.showtime(level.m_46468_())) {
                    return JoinVoice.say(kind, "night", seed);
                }
                live.nextAt = now + 40L;
            }
            case GAS -> {
                // armed: it starts once they are out past the gate (tick)
            }
            case GATE -> live.waveAt = now;
            case SHIFT -> {
                makings(player);
                live.nextAt = now + 60L;
            }
            case CAFE -> {
                if (!cafeRaid(level, player, live)) {
                    return "*He looks out of the window.* ...Nobody's coming yet. Wait with me a little?";
                }
            }
            case HUNT -> {
                if (!hunt(level, player, live)) {
                    return "*He checks a report.* The sighting's gone stale. Come back shortly.";
                }
            }
        }
        LIVE.put(id, live);
        UkakuFire.sound(level, player.m_20182_(), "block.note_block.bell", 0.7F, kind == JoinTrial.Kind.SPAR ? 0.6F : 1.0F);
        bar(player, live, now);
        return courting != null ? com.hex.kagune.control.npc.CourtVoice.spar("start", seed) : JoinVoice.say(kind, "start", seed);
    }

    /**
     * Where {@code kind} is held: the leader's home (where they stand if the world has none for them); the shift and the café's raid at
     * Touka's (behind the bar), else Ken's; the ambush and the hunt round the player.
     */
    static BlockPos anchor(ServerLevel level, EtoNpc leader, JoinTrial.Kind kind) {
        if (kind == JoinTrial.Kind.SHIFT || kind == JoinTrial.Kind.CAFE) {
            BlockPos touka = GangServer.home(level, "touka");
            if (touka != null) {
                return touka;
            }
        }
        if (leader == null) {
            return null;
        }
        BlockPos home = GangServer.home(level, leader.persona().key());
        return home != null ? home : BlockPos.m_274561_(leader.m_20185_(), leader.m_20186_() + 0.01, leader.m_20189_());
    }

    /** 4.1.79: Ken's makings for the shift (the blend's, three cups), once a day. */
    static void makings(ServerPlayer player) {
        CompoundTag kept = GangServer.keptOf(player);
        long today = GangServer.today(player);
        if (kept.m_128441_(MAKINGS_DAY) && kept.m_128454_(MAKINGS_DAY) == today) {
            return;
        }
        kept.m_128356_(MAKINGS_DAY, today);
        for (String[] making : JoinTrial.MAKINGS) {
            net.minecraft.world.item.Item item = com.hex.kagune.control.ControlItems.vanilla(making[0]);
            if (item == null) {
                continue;
            }
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item, Integer.parseInt(making[1]));
            if (!player.m_36356_(stack)) {
                player.m_36176_(stack, false);
            }
        }
        player.m_5661_(Component.m_237113_("§6Anteiku: §fKen hands you the makings §7(3 glass bottles, 6 cocoa beans, 3 blood sugar: a bottle, two "
            + "beans and the sugar make an Anteiku Blend)"), false);
    }

    // ---------------------------------------------------------------- the bodies

    /** {@code body} made for {@code live}'s trial as {@code role}: marked (kept on it), known. */
    static void mark(LivingEntity body, Live live, String role) {
        CompoundTag saved = body.getPersistentData();
        saved.m_128359_(MARK, live.player.toString());
        saved.m_128359_(ROLE, role);
        OWNER.put(body.m_20148_(), live.player);
        live.marked.add(body.m_20148_());
    }

    /** {@code body}'s part in a trial ("" for none). */
    static String role(LivingEntity body) {
        return body.getPersistentData().m_128461_(ROLE);
    }

    /** 4.1.79: true if {@code player} is in a spar with {@code npc} (EtoBrain: fought though a partner; no regard lost for its blows). */
    public static boolean sparring(ServerPlayer player, EtoNpc npc) {
        Live live = LIVE.get(player.m_20148_());
        return live != null && live.kind == JoinTrial.Kind.SPAR && live.leader == npc.m_19879_();
    }

    /** True if {@code body} was made for a trial (one live or not). */
    public static boolean marked(LivingEntity body) {
        return body.getPersistentData().m_128441_(MARK);
    }

    /** True if {@code body} was made for a trial that is no longer live: its brain takes it away. */
    public static boolean orphan(LivingEntity body) {
        return marked(body) && !OWNER.containsKey(body.m_20148_());
    }

    /** {@code player}'s live trial if {@code body} is one of its bodies, else null. */
    static Live trialOf(LivingEntity body) {
        UUID owner = OWNER.get(body.m_20148_());
        return owner == null ? null : LIVE.get(owner);
    }

    /** The CCG's pair on its beat, {@link JoinTrial#AMBUSH_NEAR} to {@link JoinTrial#AMBUSH_FAR} from them. */
    static boolean patrol(ServerLevel level, ServerPlayer player, Live live) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = player.m_217043_().m_188500_() * Math.PI * 2.0;
            double distance = JoinTrial.AMBUSH_NEAR + (JoinTrial.AMBUSH_FAR - JoinTrial.AMBUSH_NEAR) * player.m_217043_().m_188500_();
            CcgAgent leader = AgentSpawner.place(level, player.m_20185_() + Math.cos(angle) * distance, player.m_20189_() + Math.sin(angle) * distance);
            if (leader == null) {
                continue;
            }
            CcgAgent other = AgentSpawner.partner(leader, level, com.hex.kagune.control.npc.AgentRules.regularLook(player.m_217043_().m_188500_()),
                com.hex.kagune.control.npc.AgentRules.kind(player.m_217043_().m_188500_()));
            if (other == null) {
                leader.m_146870_();
                continue;
            }
            mark(leader, live, PATROL);
            mark(other, live, PATROL);
            return true;
        }
        return false;
    }

    /** The café's raid: a pair and one more, {@link JoinTrial#CAFE_NEAR} to {@link JoinTrial#CAFE_FAR} out, come for them. */
    static boolean cafeRaid(ServerLevel level, ServerPlayer player, Live live) {
        BlockPos at = live.anchor != null ? live.anchor : BlockPos.m_274561_(player.m_20185_(), player.m_20186_(), player.m_20189_());
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = player.m_217043_().m_188500_() * Math.PI * 2.0;
            double distance = JoinTrial.CAFE_NEAR + (JoinTrial.CAFE_FAR - JoinTrial.CAFE_NEAR) * player.m_217043_().m_188500_();
            CcgAgent leader = AgentSpawner.place(level, at.m_123341_() + 0.5 + Math.cos(angle) * distance, at.m_123343_() + 0.5 + Math.sin(angle) * distance);
            if (leader == null) {
                continue;
            }
            CcgAgent other = AgentSpawner.partner(leader, level, com.hex.kagune.control.npc.AgentRules.regularLook(player.m_217043_().m_188500_()),
                com.hex.kagune.control.npc.AgentRules.kind(player.m_217043_().m_188500_()));
            if (other == null) {
                leader.m_146870_();
                continue;
            }
            mark(leader, live, RAIDER);
            mark(other, live, RAIDER);
            for (int more = 0; more < JoinTrial.CAFE_RAIDERS - 2; more++) {
                CcgAgent third = single(level, leader.m_20185_() + 2.0, leader.m_20189_() + 2.0, player);
                if (third != null) {
                    mark(third, live, RAIDER);
                }
            }
            return true;
        }
        return false;
    }

    /** One investigator on his own (saved as a leader with no partner, so he never makes one when he wakes), on the ground at x, z. */
    static CcgAgent single(ServerLevel level, double x, double z, ServerPlayer player) {
        CcgAgent agent = AgentSpawner.place(level, x, z);
        if (agent == null) {
            return null;
        }
        saveAlone(agent, player);
        return agent;
    }

    /** {@code agent} saved as a leader with no partner before he first wakes (so he never makes one of his own). */
    static void saveAlone(CcgAgent agent, ServerPlayer player) {
        AgentBrain.save(agent, com.hex.kagune.control.npc.AgentRules.regularLook(player.m_217043_().m_188500_()),
            com.hex.kagune.control.npc.AgentRules.kind(player.m_217043_().m_188500_()), true, null);
    }

    /** The hunt: the partner at their side, the reported ghoul {@link JoinTrial#HUNT_NEAR} to {@link JoinTrial#HUNT_FAR} off. */
    static boolean hunt(ServerLevel level, ServerPlayer player, Live live) {
        int mastery = com.hex.kagune.data.KaguneProfile.isUnlocked(player) ? com.hex.kagune.data.KaguneProfile.mastery(player) : 0;
        GhoulNpc target = null;
        for (int attempt = 0; attempt < 8 && target == null; attempt++) {
            double angle = player.m_217043_().m_188500_() * Math.PI * 2.0;
            double distance = JoinTrial.HUNT_NEAR + (JoinTrial.HUNT_FAR - JoinTrial.HUNT_NEAR) * player.m_217043_().m_188500_();
            NpcIdentity rolled = NpcIdentity.roll(mastery, () -> player.m_217043_().m_188500_());
            NpcIdentity who = new NpcIdentity(rolled.kind(), NpcTemper.FERAL, Math.min(rolled.tier() + 1, JoinTrial.HUNT_TOP_TIER), rolled.skin(),
                rolled.name(), rolled.seed(), rolled.mask(), false);
            target = GhoulNpcSpawner.place(level, player.m_20185_() + Math.cos(angle) * distance, player.m_20189_() + Math.sin(angle) * distance, who);
        }
        if (target == null) {
            return false;
        }
        // Beside them on their own floor (the General's office is up in the tower: the ground there is its roof), else on the ground near.
        CcgAgent partner = null;
        BlockPos feet = BlockPos.m_274561_(player.m_20185_(), player.m_20186_() + 0.01, player.m_20189_());
        BlockPos beside = floor(level, feet, 1.5, 3.5, false, false, player);
        if (beside != null) {
            partner = AgentSpawner.placeAt(level, beside.m_123341_(), beside.m_123342_(), beside.m_123343_());
            if (partner != null) {
                saveAlone(partner, player);
            }
        }
        for (int attempt = 0; attempt < 6 && partner == null; attempt++) {
            double angle = attempt * Math.PI / 3.0;
            partner = single(level, player.m_20185_() + Math.cos(angle) * 2.0, player.m_20189_() + Math.sin(angle) * 2.0, player);
        }
        if (partner == null) {
            target.m_146870_();
            return false;
        }
        mark(target, live, TARGET);
        mark(partner, live, PARTNER);
        live.target = target.m_20148_();
        live.partner = partner.m_20148_();
        return true;
    }

    /** A ghoul of {@code who} standing on the floor at {@code at} for {@code live} as {@code role}, or null. */
    static GhoulNpc ghoulAt(ServerLevel level, BlockPos at, NpcIdentity who, Live live, String role) {
        GhoulNpc npc = GhoulNpcSpawner.placeAt(level, at.m_123341_(), at.m_123342_(), at.m_123343_(), who, 0);
        if (npc != null) {
            mark(npc, live, role);
        }
        return npc;
    }

    /**
     * A spot to stand on the floor round {@code anchor}, {@code near} to {@code far} blocks off, at its height (a block up or down), with a
     * roof over it if {@code indoors} (the tent's, the café's: never out on its roof or the street), in a straight open line to the anchor if
     * {@code clear} (the ring's own floor, not behind the seats). Null if none is found.
     */
    static BlockPos floor(ServerLevel level, BlockPos anchor, double near, double far, boolean indoors, boolean clear, ServerPlayer player) {
        int[] heights = {0, 1, -1, 2, -2};
        for (int attempt = 0; attempt < 32; attempt++) {
            double angle = player.m_217043_().m_188500_() * Math.PI * 2.0;
            double r = near + (far - near) * player.m_217043_().m_188500_();
            int x = (int)Math.floor(anchor.m_123341_() + 0.5 + Math.cos(angle) * r);
            int z = (int)Math.floor(anchor.m_123343_() + 0.5 + Math.sin(angle) * r);
            for (int dy : heights) {
                int y = anchor.m_123342_() + dy;
                if (!standable(level, x, y, z) || indoors && !roofed(level, x, y, z)) {
                    continue;
                }
                if (clear && !GhoulNpcBrain.clearWay(level, anchor.m_123341_() + 0.5, y, anchor.m_123343_() + 0.5, x + 0.5, z + 0.5)) {
                    continue;
                }
                return new BlockPos(x, y, z);
            }
        }
        return null;
    }

    /**
     * 4.1.82: a seat in front of Anteiku's counter ({@code anchor}: Touka's spot behind it): JoinTrial.counter's offsets the way she faced
     * when the cafe was built (her body's, EtoBrain.SIT_YAW), on her floor (or a step up or down), under the roof; one of the nearest rows,
     * by chance. Null if she isn't about to say which way, or there's no room.
     */
    static BlockPos counterSeat(ServerLevel level, BlockPos anchor, ServerPlayer player) {
        Float yaw = null;
        for (EtoNpc one : EtoBrain.live()) {
            if (one.persona().key().equals("touka") && one.m_6084_() && one.m_9236_() == level && one.getPersistentData().m_128441_(EtoBrain.SIT_YAW)) {
                yaw = one.getPersistentData().m_128451_(EtoBrain.SIT_YAW) / 10.0F;
            }
        }
        if (yaw == null || anchor == null) {
            return null;
        }
        List<BlockPos> seats = new ArrayList<>();
        int[] heights = {0, -1, 1};
        for (int[] at : JoinTrial.counter(yaw)) {
            for (int dy : heights) {
                int x = anchor.m_123341_() + at[0];
                int y = anchor.m_123342_() + dy;
                int z = anchor.m_123343_() + at[1];
                if (standable(level, x, y, z) && roofed(level, x, y, z)) {
                    seats.add(new BlockPos(x, y, z));
                    break;
                }
            }
            if (seats.size() >= 6) {
                break; // the nearest rows
            }
        }
        return seats.isEmpty() ? null : seats.get(player.m_217043_().m_188503_(seats.size()));
    }

    /**
     * 4.1.82 (the owner: "giving items to NPCS like coffee should be right clicking them ... sometimes they dont even accept it"): a right
     * click with the cup near the regular waiting (within JoinTrial.SERVE_NEAR), aimed at them or not, serves them; never drunk instead.
     */
    @SubscribeEvent
    public static void cup(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            Live live = LIVE.get(player.m_20148_());
            if (live == null || live.kind != JoinTrial.Kind.SHIFT || live.current == null) {
                return;
            }
            net.minecraft.world.item.ItemStack held = event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? player.m_21205_() : player.m_21206_();
            net.minecraft.world.item.Item cup = com.hex.kagune.control.ControlItems.vanilla(JoinTrial.CUP);
            if (cup == null || held.m_41720_() != cup || !(player.m_284548_().m_8791_(live.current) instanceof GhoulNpc regular)
                || regular.m_20280_(player) > JoinTrial.SERVE_NEAR * JoinTrial.SERVE_NEAR) {
                return;
            }
            event.setCanceled(true);
            serve(player, regular);
        } catch (RuntimeException | LinkageError failure) {
            Safety.caught(failure, "JoinServer.cup");
        }
    }

    /** True if a body can stand at x, y, z: solid ground under it, no fluid, its feet and head in the open. */
    static boolean standable(ServerLevel level, int x, int y, int z) {
        BlockState ground = level.m_8055_(new BlockPos(x, y - 1, z));
        BlockState feet = level.m_8055_(new BlockPos(x, y, z));
        BlockState head = level.m_8055_(new BlockPos(x, y + 1, z));
        return !ground.m_60795_() && ground.m_60819_().m_76178_() && feet.m_60795_() && head.m_60795_();
    }

    /** True if something is over x, y, z within ten blocks (a roof). */
    static boolean roofed(ServerLevel level, int x, int y, int z) {
        for (int up = 2; up <= 10; up++) {
            if (!level.m_8055_(new BlockPos(x, y + up, z)).m_60795_()) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- followed

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        try {
            if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.f_19797_ % 5 != 0) {
                return;
            }
            Live live = LIVE.get(player.m_20148_());
            if (live == null) {
                return;
            }
            ServerLevel level = player.m_284548_();
            long now = level.m_46467_();
            if (!player.m_6084_()) {
                end(player, live, false, "died");
                return;
            }
            if (live.courting != null ? !CourtshipServer.on(player, live.courting, com.hex.kagune.control.npc.Courtship.Task.SPAR) : stepGone(player, live)) {
                end(player, live, false, null); // its step (or chapter) moved on some other way (a command, a quit, a gang joined): over, quietly
                return;
            }
            switch (live.kind) {
                case SPAR -> spar(player, live, level, now);
                case AMBUSH -> ambush(player, live, level, now);
                case RING -> ring(player, live, level, now);
                case GAS -> gas(player, live, level, now);
                case GATE -> gate(player, live, level, now);
                case SHIFT -> shift(player, live, level, now);
                case CAFE -> cafe(player, live, level, now);
                case HUNT -> huntTick(player, live, level, now);
            }
            if (LIVE.get(player.m_20148_()) == live && player.f_19797_ % 20 == 0) {
                bar(player, live, now);
            }
        } catch (RuntimeException | LinkageError failure) {
            Safety.caught(failure, "JoinServer.tick");
        }
    }

    /** True if {@code player}'s way in is no longer at {@code live}'s step (or they are in a gang now). */
    static boolean stepGone(ServerPlayer player, Live live) {
        GangPlayer g = GangServer.of(player);
        return g.step(JoinTrial.gang(live.kind)) != JoinTrial.step(live.kind) || g.gang() != GangPlayer.NONE;
    }

    /** Off {@code live}'s ground ({@code away}) a quarter second more, or back on it: true once off it too long ({@code out} ticks). */
    static boolean strayed(Live live, boolean away, int out) {
        live.outside = away ? live.outside + 5 : 0;
        return live.outside > out;
    }

    static double distance(ServerPlayer player, BlockPos at) {
        if (at == null) {
            return 0.0;
        }
        double dx = player.m_20185_() - (at.m_123341_() + 0.5);
        double dz = player.m_20189_() - (at.m_123343_() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    static void spar(ServerPlayer player, Live live, ServerLevel level, long now) {
        EtoNpc ayato = level.m_6815_(live.leader) instanceof EtoNpc npc && npc.m_6084_() ? npc : null;
        if (ayato == null || ayato.mind == null) {
            end(player, live, false, "died");
            return;
        }
        if (!live.fighting(now)) {
            long left = (live.began + JoinTrial.SPAR_COUNTDOWN - now + 19L) / 20L;
            player.m_5661_(Component.m_237113_("§4" + Math.max(1L, left) + "..."), true);
            return;
        }
        if (strayed(live, distance(player, live.anchor) > JoinTrial.SPAR_RING, JoinTrial.SPAR_OUT)) {
            end(player, live, false, "fled");
            return;
        }
        if (JoinTrial.reached(ayato.m_21223_(), ayato.m_21233_(), JoinTrial.SPAR_WIN)) {
            end(player, live, true, "won");
            return;
        }
        if (JoinTrial.reached(player.m_21223_(), player.m_21233_(), JoinTrial.SPAR_FLOOR)) {
            end(player, live, false, "lost");
            return;
        }
        if (now - live.began > JoinTrial.SPAR_COUNTDOWN + JoinTrial.SPAR_TICKS) {
            end(player, live, false, "draw");
            return;
        }
        if (ayato.mind.target != player.m_19879_()) {
            ayato.mind.target = player.m_19879_(); // he keeps at them (EtoBrain lets a spar's partner be fought)
            ayato.mind.reachedAt = now;
            ayato.mind.dirty = true;
        }
    }

    static void ambush(ServerPlayer player, Live live, ServerLevel level, long now) {
        glow(level, live);
        if (live.marked.isEmpty()) {
            end(player, live, true, "won");
            return;
        }
        if (JoinTrial.calledIn(live.first, now)) {
            end(player, live, false, "called");
            return;
        }
        if (live.first == Long.MIN_VALUE && now - live.began > JoinTrial.AMBUSH_FIND) {
            end(player, live, false, "slow");
        }
    }

    static void ring(ServerPlayer player, Live live, ServerLevel level, long now) {
        if (live.anchor == null || strayed(live, distance(player, live.anchor) > JoinTrial.RING_REACH, JoinTrial.RING_OUT)) {
            end(player, live, false, "fled");
            return;
        }
        if (live.current != null && !alive(level, live.current)) {
            live.current = null; // its death counted the act (died)
        }
        if (live.done >= JoinTrial.RING_ACTS) {
            end(player, live, true, "won");
            return;
        }
        if (live.current != null) {
            grudge(level, live.current, player, now);
            if (now - live.currentAt > JoinTrial.RING_ACT) {
                end(player, live, false, "booed");
            }
            return;
        }
        if (now < live.nextAt) {
            return;
        }
        BlockPos at = floor(level, live.anchor, JoinTrial.RING_IN_NEAR, JoinTrial.RING_IN_FAR, false, true, player);
        if (at == null) {
            at = floor(level, live.anchor, 2.0, JoinTrial.RING_IN_FAR, false, false, player);
        }
        if (at == null) {
            live.nextAt = now + 40L;
            return;
        }
        int mastery = com.hex.kagune.data.KaguneProfile.isUnlocked(player) ? com.hex.kagune.data.KaguneProfile.mastery(player) : 0;
        NpcIdentity rolled = NpcIdentity.roll(mastery, () -> player.m_217043_().m_188500_());
        // A drifter: it hunts no other ghoul (never one of Roma's Clowns about the tent), only the one it is set on (its grudge).
        NpcIdentity who = new NpcIdentity(rolled.kind(), NpcTemper.DRIFTER, Math.min(rolled.tier(), JoinTrial.RING_TOP_TIER), rolled.skin(), rolled.name(),
            rolled.seed(), rolled.mask(), false);
        GhoulNpc act = ghoulAt(level, at, who, live, VOLUNTEER);
        if (act == null) {
            live.nextAt = now + 40L;
            return;
        }
        live.current = act.m_20148_();
        live.currentAt = now;
        level.m_8767_(ParticleTypes.f_123759_, act.m_20185_(), act.m_20186_() + 0.5, act.m_20189_(), 20, 0.4, 0.6, 0.4, 0.02);
        UkakuFire.sound(level, act.m_20182_(), "block.note_block.harp", 1.0F, 1.4F);
        player.m_5661_(Component.m_237113_("§9Roma: §f" + (live.done == 0 ? "Volunteer number one!" : live.done == 1 ? "Volunteer number two! Give it up!"
            : "And our last volunteer of the night!") + " §7(" + (live.done + 1) + "/" + JoinTrial.RING_ACTS + ")"), false);
    }

    static void gas(ServerPlayer player, Live live, ServerLevel level, long now) {
        if (live.anchor == null) {
            end(player, live, false, "died");
            return;
        }
        double from = distance(player, live.anchor);
        if (live.first == Long.MIN_VALUE) {
            if (JoinTrial.gasStarts(from)) {
                live.first = now;
                UkakuFire.sound(level, player.m_20182_(), "block.fire.extinguish", 1.0F, 0.6F);
                player.m_5661_(Component.m_237113_("§aThe Gas Masks: §fthe valves hiss open. §7Walk back to Banjou. Don't stop."), false);
            }
            return;
        }
        if (GangServer.gasMaskOn(player)) {
            end(player, live, false, "masked");
            return;
        }
        if (from <= JoinTrial.GAS_REACHED) {
            player.m_21195_(MobEffects.f_19614_);
            end(player, live, true, "won");
            return;
        }
        if (now - live.first > JoinTrial.GAS_TICKS) {
            end(player, live, false, "slow");
            return;
        }
        if (from <= JoinTrial.GAS_OUTER && player.f_19797_ % 20 == 0) {
            player.m_7292_(new MobEffectInstance(MobEffects.f_19614_, 50, 0, false, true, true));
            player.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 50, 0, false, true, true));
            player.m_7292_(new MobEffectInstance(MobEffects.f_19604_, 120, 0, false, true, true));
        }
        if (from <= JoinTrial.GAS_OUTER) {
            level.m_8767_(ParticleTypes.f_123762_, player.m_20185_(), player.m_20186_() + 1.0, player.m_20189_(), 6, 1.2, 0.8, 1.2, 0.01);
            level.m_8767_(ParticleTypes.f_123748_, player.m_20185_(), player.m_20186_() + 0.4, player.m_20189_(), 2, 1.5, 0.4, 1.5, 0.0);
        }
    }

    static void gate(ServerPlayer player, Live live, ServerLevel level, long now) {
        if (live.anchor == null || strayed(live, distance(player, live.anchor) > JoinTrial.GATE_ZONE, JoinTrial.GATE_OUT)) {
            end(player, live, false, "fled");
            return;
        }
        int standing = standing(level, live);
        if (JoinTrial.nextWave(live.wave, standing, now - live.waveAt)) {
            if (clowns(level, player, live)) {
                live.wave++;
                live.waveAt = now;
                UkakuFire.sound(level, player.m_20182_(), "block.bell.use", 1.0F, 0.7F);
                player.m_5661_(Component.m_237113_("§aBanjou: §f" + (live.wave == 1 ? "Clowns! At the gate!" : "More of them! Hold it!") + " §7(wave " + live.wave
                    + "/" + JoinTrial.GATE_WAVES + ")"), false);
            }
            return;
        }
        if (live.wave >= JoinTrial.GATE_WAVES && standing == 0) {
            end(player, live, true, "won");
            return;
        }
        if (now - live.began > JoinTrial.GATE_TICKS) {
            end(player, live, false, "slow");
            return;
        }
        for (UUID one : live.marked) {
            grudge(level, one, player, now);
        }
        march(level, live, player);
    }

    /**
     * The raid's bodies with nothing to fight yet (the gate's Clowns, the café's investigators), each second, walked on toward {@code player}:
     * they come for the gate, the café, rather than wander (a gang's others keep up with its leader; a pair's partner with its leader).
     */
    static void march(ServerLevel level, Live live, ServerPlayer player) {
        if (player.f_19797_ % 20 != 0) {
            return;
        }
        for (UUID one : live.marked) {
            Entity body = level.m_8791_(one);
            if (body instanceof GhoulNpc ghoul && ghoul.mind != null && ghoul.mind.prey == 0 && ghoul.mind.leavingAt == Long.MIN_VALUE) {
                ghoul.m_21573_().m_26519_(player.m_20185_(), player.m_20186_(), player.m_20189_(), 1.0);
            } else if (body instanceof CcgAgent agent && agent.mind != null && agent.mind.prey == 0
                && !agent.getPersistentData().m_128471_(BROKE)) {
                agent.m_21573_().m_26519_(player.m_20185_(), player.m_20186_(), player.m_20189_(), 1.0);
            }
        }
    }

    /** A wave of Clowns, {@link JoinTrial#GATE_NEAR} to {@link JoinTrial#GATE_FAR} out from Banjou, all of one gang. */
    static boolean clowns(ServerLevel level, ServerPlayer player, Live live) {
        com.hex.kagune.control.npc.Gang gang = com.hex.kagune.control.npc.Gang.CLOWNS;
        int[] faces = gang.faces(JoinTrial.GATE_WAVE, () -> player.m_217043_().m_188500_());
        int number = 1 + player.m_217043_().m_188503_(Integer.MAX_VALUE - 1);
        int mastery = com.hex.kagune.data.KaguneProfile.isUnlocked(player) ? com.hex.kagune.data.KaguneProfile.mastery(player) : 0;
        for (int attempt = 0; attempt < 6; attempt++) {
            double angle = player.m_217043_().m_188500_() * Math.PI * 2.0;
            double distance = JoinTrial.GATE_NEAR + (JoinTrial.GATE_FAR - JoinTrial.GATE_NEAR) * player.m_217043_().m_188500_();
            double x = live.anchor.m_123341_() + 0.5 + Math.cos(angle) * distance;
            double z = live.anchor.m_123343_() + 0.5 + Math.sin(angle) * distance;
            List<GhoulNpc> made = new ArrayList<>();
            for (int i = 0; i < JoinTrial.GATE_WAVE; i++) {
                NpcIdentity who = NpcIdentity.roll(mastery, () -> player.m_217043_().m_188500_()).ganged(gang, faces[i]);
                double side = i * Math.PI * 2.0 / JoinTrial.GATE_WAVE;
                GhoulNpc one = GhoulNpcSpawner.place(level, x + (i == 0 ? 0.0 : Math.cos(side) * 2.5), z + (i == 0 ? 0.0 : Math.sin(side) * 2.5), who, number);
                if (one != null) {
                    made.add(one);
                }
            }
            if (made.isEmpty()) {
                continue;
            }
            for (GhoulNpc one : made) {
                mark(one, live, RAIDER);
            }
            return true;
        }
        return false;
    }

    static void shift(ServerPlayer player, Live live, ServerLevel level, long now) {
        if (live.anchor == null || strayed(live, distance(player, live.anchor) > JoinTrial.SHIFT_ZONE, 200)) {
            end(player, live, false, "fled");
            return;
        }
        if (live.done >= JoinTrial.SHIFT_CUSTOMERS) {
            end(player, live, true, "won");
            return;
        }
        if (JoinTrial.shiftLost(live.strikes)) {
            end(player, live, false, "walked");
            return;
        }
        if (live.current != null) {
            Entity regular = level.m_8791_(live.current);
            if (!(regular instanceof GhoulNpc npc) || !npc.m_6084_()) {
                live.current = null;
                live.strikes++;
                live.nextAt = now + JoinTrial.SHIFT_GAP;
                return;
            }
            if (now - live.currentAt > JoinTrial.SHIFT_PATIENCE) {
                live.strikes++;
                leaves(npc, now);
                live.current = null;
                live.nextAt = now + JoinTrial.SHIFT_GAP;
                UkakuFire.sound(level, npc.m_20182_(), "block.candle.extinguish", 0.9F, 0.7F);
                player.m_5661_(Component.m_237113_("§6Anteiku: §fa regular gave up waiting and left. §7(" + live.strikes + "/" + JoinTrial.SHIFT_STRIKES
                    + " walked out)"), false);
                return;
            }
            if (player.f_19797_ % 10 == 0) {
                mark(player, npc); // 4.1.82: theirs alone to see
            }
            return;
        }
        if (now < live.nextAt) {
            return;
        }
        // 4.1.82 (the owner: "clients are spawning in areas that are not infront of the reception"): in front of the counter, the way
        // Touka faces from behind it; only if she isn't there to say which way that is, anywhere on the cafe's floor round her spot.
        BlockPos seat = counterSeat(level, live.anchor, player);
        if (seat == null) {
            seat = floor(level, live.anchor, JoinTrial.SHIFT_NEAR, JoinTrial.SHIFT_FAR, true, false, player);
        }
        if (seat == null) {
            live.nextAt = now + 40L;
            return;
        }
        NpcIdentity rolled = NpcIdentity.roll(0, () -> player.m_217043_().m_188500_());
        NpcIdentity who = new NpcIdentity(rolled.kind(), NpcTemper.ANTEIKU, 0, rolled.skin(), rolled.name(), rolled.seed(), 0, false);
        GhoulNpc regular = ghoulAt(level, seat, who, live, REGULAR);
        if (regular == null) {
            live.nextAt = now + 40L;
            return;
        }
        CompoundTag saved = regular.getPersistentData();
        saved.m_128405_(SEAT + "X", seat.m_123341_());
        saved.m_128405_(SEAT + "Y", seat.m_123342_());
        saved.m_128405_(SEAT + "Z", seat.m_123343_());
        live.current = regular.m_20148_();
        live.currentAt = now;
        UkakuFire.sound(level, regular.m_20182_(), "block.note_block.chime", 0.8F, 1.2F);
        player.m_5661_(Component.m_237113_("§6Anteiku: §f" + who.displayName() + " sits down. §7(a cup of Anteiku Blend, in your hand, to them: "
            + (live.done + 1) + "/" + JoinTrial.SHIFT_CUSTOMERS + ")"), false);
    }

    static void leaves(GhoulNpc npc, long now) {
        if (npc.mind != null && npc.mind.leavingAt == Long.MIN_VALUE) {
            npc.mind.leavingAt = now;
        }
    }

    /**
     * 4.1.79: a right click on {@code npc} by {@code player} while their shift's regular is waiting: a cup of Anteiku Blend in the hand is
     * served (taken, drunk, thanked; they leave), anything else is asked about. True if the click was the shift's (the talk stays shut).
     */
    static boolean serve(ServerPlayer player, GhoulNpc npc) {
        Live live = LIVE.get(player.m_20148_());
        if (live == null || live.kind != JoinTrial.Kind.SHIFT || live.current == null || !live.current.equals(npc.m_20148_())) {
            return false;
        }
        net.minecraft.world.item.Item cup = com.hex.kagune.control.ControlItems.vanilla(JoinTrial.CUP);
        // 4.1.82 (the owner: "sometimes they dont even accept it"): the cup in either hand.
        net.minecraft.world.item.ItemStack held = cup != null && player.m_21206_().m_41720_() == cup && player.m_21205_().m_41720_() != cup
            ? player.m_21206_() : player.m_21205_();
        if (cup == null || held.m_41619_() || held.m_41720_() != cup) {
            player.m_5661_(Component.m_237113_("§6" + npc.identity().displayName() + ": §f\"An Anteiku Blend, please.\" §7(in your hand)"), true);
            return true;
        }
        if (!player.m_7500_()) {
            held.m_41774_(1);
        }
        ServerLevel level = player.m_284548_();
        long now = level.m_46467_();
        live.done++;
        live.current = null;
        live.nextAt = now + JoinTrial.SHIFT_GAP;
        leaves(npc, now);
        UkakuFire.sound(level, npc.m_20182_(), "entity.experience_orb.pickup", 0.8F, 1.1F);
        level.m_8767_(ParticleTypes.f_123748_, npc.m_20185_(), npc.m_20186_() + 1.8, npc.m_20189_(), 6, 0.3, 0.2, 0.3, 0.0);
        String[] thanks = {"*sips* ...Mm. Thank you. Better than Touka's. Don't tell her.", "*wraps both hands round the cup* ...Warm. Thank you.",
            "*a long, grateful breath over the cup* You're new. You're good at this."};
        player.m_5661_(Component.m_237113_("§6" + npc.identity().displayName() + ": §f" + thanks[Math.floorMod(live.seed + live.done, thanks.length)]
            + " §7(" + live.done + "/" + JoinTrial.SHIFT_CUSTOMERS + ")"), false);
        return true;
    }

    /**
     * 4.1.79: a regular waiting for their cup ({@code npc}, the shift's): kept at their seat, facing whoever serves them (or the counter).
     * True if it is one (GhoulNpcBrain's idle stops there).
     */
    public static boolean held(GhoulNpc npc, ServerLevel level, long now) {
        if (!REGULAR.equals(role(npc))) {
            return false;
        }
        CompoundTag saved = npc.getPersistentData();
        if (!saved.m_128441_(SEAT + "X")) {
            return false;
        }
        double sx = saved.m_128451_(SEAT + "X") + 0.5;
        double sy = saved.m_128451_(SEAT + "Y");
        double sz = saved.m_128451_(SEAT + "Z") + 0.5;
        double dx = npc.m_20185_() - sx;
        double dz = npc.m_20189_() - sz;
        if (dx * dx + dz * dz > 1.0) {
            if (now % 20L == 0L || npc.m_21573_().m_26571_()) {
                npc.m_21573_().m_26519_(sx, sy, sz, 0.6);
            }
            return true;
        }
        npc.m_21573_().m_26573_();
        UUID owner = OWNER.get(npc.m_20148_());
        Entity server = owner == null ? null : level.m_8791_(owner);
        if (server instanceof ServerPlayer player && player.m_20280_(npc) < 12.0 * 12.0) {
            npc.m_21563_().m_24960_(player, 30.0F, 30.0F);
        }
        return true;
    }

    static void cafe(ServerPlayer player, Live live, ServerLevel level, long now) {
        if (strayed(live, live.anchor != null && distance(player, live.anchor) > JoinTrial.CAFE_ZONE, JoinTrial.CAFE_OUT)) {
            end(player, live, false, "fled");
            return;
        }
        for (UUID one : new ArrayList<>(live.marked)) {
            Entity body = level.m_8791_(one);
            if (body instanceof LivingEntity raider && raider.m_6084_()) {
                if (raider.getPersistentData().m_128471_(BROKE) || JoinTrial.breaks(raider.m_21223_(), raider.m_21233_())) {
                    driveOff(player, live, raider, level);
                    continue;
                }
                grudge(level, one, player, now);
            }
        }
        march(level, live, player);
        if (live.marked.isEmpty()) {
            end(player, live, true, "won");
            return;
        }
        if (now - live.began > JoinTrial.CAFE_TICKS) {
            end(player, live, false, "slow");
        }
    }

    /** One of the café's raid beaten till he breaks: he runs (gone in a puff, his line shouted), counted. */
    static void driveOff(ServerPlayer player, Live live, LivingEntity raider, ServerLevel level) {
        live.marked.remove(raider.m_20148_());
        OWNER.remove(raider.m_20148_());
        live.done++;
        level.m_8767_(ParticleTypes.f_123759_, raider.m_20185_(), raider.m_20186_() + 0.8, raider.m_20189_(), 16, 0.4, 0.6, 0.4, 0.03);
        String[] lines = {"Fall back! FALL BACK!", "It's not worth it, retreat!", "We'll come back with more!"};
        player.m_5661_(Component.m_237113_("§bInvestigator: §f\"" + lines[Math.floorMod(live.done, lines.length)] + "\" §7(driven off: "
            + live.done + "/" + JoinTrial.CAFE_RAIDERS + ")"), false);
        raider.m_146870_();
    }

    static void huntTick(ServerPlayer player, Live live, ServerLevel level, long now) {
        glow(level, live);
        Entity partner = live.partner == null ? null : level.m_8791_(live.partner);
        if (!(partner instanceof CcgAgent agent) || !agent.m_6084_()) {
            end(player, live, false, "partner");
            return;
        }
        if (live.target == null) {
            end(player, live, true, "won");
            return;
        }
        Entity target = level.m_8791_(live.target);
        if (target != null && agent.mind != null) {
            agent.mind.grudges.put(live.target, now + 200L); // the partner goes for what they hunt, once he sees it
        }
        if (target != null) {
            grudge(level, live.target, player, now);
        }
        if (now - live.began > JoinTrial.HUNT_TICKS) {
            end(player, live, false, "slow");
        }
    }

    /**
     * 4.1.79: the hunt's partner at peace (AgentBrain.patrol): at the side of the one he was assigned to, catching up when left behind.
     * True if it is one, with them about (else he patrols as any would).
     */
    public static boolean partnerWalk(CcgAgent agent, ServerLevel level, long now) {
        if (!PARTNER.equals(role(agent))) {
            return false;
        }
        UUID owner = OWNER.get(agent.m_20148_());
        Entity body = owner == null ? null : level.m_8791_(owner);
        if (!(body instanceof ServerPlayer player) || !player.m_6084_()) {
            return false;
        }
        double gap = Math.sqrt(agent.m_20280_(player));
        if (gap > JoinTrial.PARTNER_CLOSE && (now % 10L == 0L || agent.m_21573_().m_26571_())) {
            agent.m_21573_().m_26519_(player.m_20185_(), player.m_20186_(), player.m_20189_(), gap > 10.0 ? 1.3 : 1.0);
        } else if (gap <= JoinTrial.PARTNER_CLOSE - 1.0) {
            agent.m_21573_().m_26573_();
            agent.m_21563_().m_24960_(player, 30.0F, 30.0F);
        }
        return true;
    }

    /** 4.1.79: true if {@code agent} is {@code player}'s hunt partner: he never goes for them (their kagune out or not). */
    public static boolean partnered(CcgAgent agent, ServerPlayer player) {
        return PARTNER.equals(role(agent)) && player.m_20148_().equals(OWNER.get(agent.m_20148_()));
    }

    /** The marked bodies of {@code live} glowing (the patrol, the reported ghoul): they can be found. */
    static void glow(ServerLevel level, Live live) {
        if (!(level.m_8791_(live.player) instanceof ServerPlayer owner)) {
            return;
        }
        for (UUID one : live.marked) {
            if (level.m_8791_(one) instanceof LivingEntity body && body.m_6084_() && !PARTNER.equals(role(body))) {
                mark(owner, body);
            }
        }
    }

    /**
     * 4.1.82 (the owner: "Clients should be PER player, only highlight the ones that are OF THE PLAYER DOING THE QUEST"): {@code body}
     * marked for {@code owner} alone: a column of green sparks over its head, sent to them and no one else (the glowing effect it had was
     * every player's to see).
     */
    static void mark(ServerPlayer owner, LivingEntity body) {
        owner.m_284548_().m_8624_(owner, ParticleTypes.f_123748_, true, body.m_20185_(), body.m_20186_() + MARK_HEIGHT, body.m_20189_(), MARK_SPARKS,
            0.12, 0.35, 0.12, 0.0);
    }

    /** 4.1.82: how high over a body's feet its mark is (blocks: over a head), and how many sparks a look. */
    static final double MARK_HEIGHT = 2.3;
    static final int MARK_SPARKS = 4;

    /** 4.1.82: true if {@code body} is one of a trial's and {@code other} a player whose trial it is not (it never goes for them; nor they for it). */
    public static boolean othersTrial(Entity body, Entity other) {
        UUID owner = OWNER.get(body.m_20148_());
        return owner != null && other instanceof ServerPlayer player && !player.m_20148_().equals(owner);
    }

    /** How many of {@code live}'s bodies are up. */
    static int standing(ServerLevel level, Live live) {
        int up = 0;
        for (UUID one : live.marked) {
            if (alive(level, one)) {
                up++;
            }
        }
        return up;
    }

    static boolean alive(ServerLevel level, UUID one) {
        return level.m_8791_(one) instanceof LivingEntity body && body.m_6084_();
    }

    /** {@code one} (a ghoul or an investigator of the trial) set on {@code player} a while. */
    static void grudge(ServerLevel level, UUID one, ServerPlayer player, long now) {
        Entity body = level.m_8791_(one);
        if (body instanceof GhoulNpc ghoul && ghoul.mind != null) {
            ghoul.mind.grudges.put(player.m_20148_(), now + 200L);
        } else if (body instanceof CcgAgent agent && agent.mind != null && !PARTNER.equals(role(agent))) {
            agent.mind.grudges.put(player.m_20148_(), now + 200L);
        }
    }

    // ---------------------------------------------------------------- blows and deaths

    /**
     * 4.1.79: the spar's blows held to it (the owner's plan: "a duel that ends at low health, not death"): Ayato's never take them under
     * {@link JoinTrial#SPAR_FLOOR}, theirs never take him under {@link JoinTrial#SPAR_WIN}; the blow that would, ends it. The café's raid:
     * an investigator a blow leaves low breaks off (counted, and spared). Last of all (what lands, as every other handler leaves it).
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent event) {
        try {
            LivingEntity hurt = event.getEntity();
            Entity by = event.getSource().m_7639_();
            if (hurt instanceof ServerPlayer player && by instanceof EtoNpc) {
                Live live = LIVE.get(player.m_20148_());
                if (live != null && live.kind == JoinTrial.Kind.SPAR && by.m_19879_() == live.leader && live.fighting(player.m_9236_().m_46467_())) {
                    float allowed = JoinTrial.clamp(player.m_21223_(), player.m_21233_(), event.getAmount(), JoinTrial.SPAR_FLOOR);
                    boolean floored = allowed < event.getAmount();
                    event.setAmount(allowed);
                    if (floored) {
                        end(player, live, false, "lost");
                    }
                }
                return;
            }
            if (hurt instanceof EtoNpc npc && by instanceof ServerPlayer player) {
                Live live = LIVE.get(player.m_20148_());
                if (live != null && live.kind == JoinTrial.Kind.SPAR && npc.m_19879_() == live.leader) {
                    if (!live.fighting(player.m_9236_().m_46467_())) {
                        event.setAmount(0.0F); // not yet: "three... two..."
                        return;
                    }
                    float allowed = JoinTrial.clamp(npc.m_21223_(), npc.m_21233_(), event.getAmount(), JoinTrial.SPAR_WIN);
                    boolean beaten = allowed < event.getAmount();
                    event.setAmount(allowed);
                    if (beaten) {
                        end(player, live, true, "won");
                    }
                }
                return;
            }
            Live live = trialOf(hurt);
            if (live != null && live.kind == JoinTrial.Kind.CAFE && RAIDER.equals(role(hurt))
                && JoinTrial.breaks(hurt.m_21223_() - event.getAmount(), hurt.m_21233_())) {
                hurt.getPersistentData().m_128379_(BROKE, true); // he breaks: driven off on the trial's next look (cafe)
            }
            if (live != null && live.kind == JoinTrial.Kind.AMBUSH && live.first == Long.MIN_VALUE && by instanceof ServerPlayer player
                && player.m_20148_().equals(live.player)) {
                live.first = player.m_9236_().m_46467_(); // the first blow: the clock starts
                player.m_5661_(Component.m_237113_("§4Aogiri Tree: §fthe first blow! §7Both down within " + JoinTrial.AMBUSH_CLOCK / 20 + " s."), true);
            }
        } catch (RuntimeException | LinkageError failure) {
            Safety.caught(failure, "JoinServer.hurt");
        }
    }

    /** 4.1.79: a café raider who broke off is spared: nothing lands on him while he goes. */
    @SubscribeEvent
    public static void attacked(LivingAttackEvent event) {
        if (OWNER.containsKey(event.getEntity().m_20148_()) && event.getEntity().getPersistentData().m_128471_(BROKE)) {
            event.setCanceled(true);
            return;
        }
        // 4.1.82 (the owner: "Quests are PER player, currently i'm 90% sure Im taking another player's quest"): a trial's bodies are its
        // player's alone: no other player's blow lands on them (nor kills them for that player). 4.1.86 (the owner: "quests per player.
        // thats the main thing"): and they are told whose it is.
        if (othersTrial(event.getEntity(), event.getSource().m_7639_())) {
            event.setCanceled(true);
            if (event.getSource().m_7639_() instanceof ServerPlayer other) {
                notYours(other, event.getEntity());
            }
        }
    }

    /** 4.1.86: when each player was last told a body was another's (the world's game time). */
    private static final Map<UUID, Long> TOLD = WorldTimes.map();

    /**
     * 4.1.86: {@code other} told, on their action bar, that {@code body} is another player's quest (and whose), at most once each
     * JoinTrial.NOT_YOURS_EVERY. True if {@code body} is one of another's trial (whatever was told).
     */
    public static boolean notYours(ServerPlayer other, Entity body) {
        UUID owner = OWNER.get(body.m_20148_());
        if (owner == null || owner.equals(other.m_20148_())) {
            return false;
        }
        long now = other.m_9236_().m_46467_();
        Long last = TOLD.get(other.m_20148_());
        if (last == null || now - last >= JoinTrial.NOT_YOURS_EVERY || now < last) {
            TOLD.put(other.m_20148_(), now);
            String name = other.m_284548_().m_8791_(owner) instanceof ServerPlayer them ? them.m_36316_().getName() : null;
            other.m_5661_(Component.m_237113_("\u00a77" + JoinTrial.notYours(name)), true);
        }
        return true;
    }

    /** 4.1.79: a trial's body dead (counted, or the trial lost), or a player in a trial dead (lost). */
    @SubscribeEvent
    public static void died(LivingDeathEvent event) {
        try {
            LivingEntity dead = event.getEntity();
            if (dead instanceof ServerPlayer player) {
                Live live = LIVE.get(player.m_20148_());
                if (live != null) {
                    end(player, live, false, "died");
                }
                return;
            }
            Live live = trialOf(dead);
            if (live == null) {
                return;
            }
            UUID id = dead.m_20148_();
            live.marked.remove(id);
            OWNER.remove(id);
            ServerPlayer player = dead.m_9236_() instanceof ServerLevel level && level.m_8791_(live.player) instanceof ServerPlayer p ? p : null;
            if (player == null) {
                return;
            }
            boolean theirs = event.getSource().m_7639_() == player;
            switch (live.kind) {
                case RING -> {
                    if (id.equals(live.current)) {
                        live.done++;
                        live.current = null;
                        live.nextAt = player.m_9236_().m_46467_() + JoinTrial.RING_GAP;
                        UkakuFire.sound(player.m_284548_(), dead.m_20182_(), "entity.player.levelup", 0.6F, 1.6F);
                        player.m_5661_(Component.m_237113_("§9Roma: §f" + (live.done >= JoinTrial.RING_ACTS ? "*standing ovation*" : "*whistles* Next!")
                            + " §7(" + live.done + "/" + JoinTrial.RING_ACTS + ")"), false);
                    }
                }
                case AMBUSH -> player.m_5661_(Component.m_237113_("§4Aogiri Tree: §fa dove down. §7(" + (JoinTrial.AMBUSH_SIZE - live.marked.size()) + "/"
                    + JoinTrial.AMBUSH_SIZE + ")"), true);
                case GATE -> player.m_5661_(Component.m_237113_("§aThe Gas Masks: §fa Clown down."), true);
                case CAFE -> {
                    if (theirs) {
                        end(player, live, false, "killed");
                    } else {
                        live.done++;
                    }
                }
                case SHIFT -> {
                    if (id.equals(live.current)) {
                        live.current = null;
                        live.strikes++;
                    }
                }
                case HUNT -> {
                    if (id.equals(live.partner)) {
                        end(player, live, false, "partner");
                    } else if (id.equals(live.target)) {
                        live.target = null;
                        end(player, live, true, "won");
                    }
                }
                default -> {
                }
            }
        } catch (RuntimeException | LinkageError failure) {
            Safety.caught(failure, "JoinServer.died");
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        try {
            if (event.getEntity() instanceof ServerPlayer player) {
                Live live = LIVE.get(player.m_20148_());
                if (live != null) {
                    end(player, live, false, null);
                }
                EXAMS.remove(player.m_20148_());
            }
        } catch (RuntimeException | LinkageError failure) {
            Safety.caught(failure, "JoinServer.loggedOut");
        }
    }

    @SubscribeEvent
    public static void changedWorld(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Live live = LIVE.get(player.m_20148_());
            if (live != null) {
                end(player, live, false, "fled");
            }
        }
    }

    // ---------------------------------------------------------------- over

    /**
     * {@code live} over for {@code player}: its bodies gone (the regulars walk out, the rest in a puff), Ayato let go; won, the way in moved
     * on and the leader's word on it; lost, the leader's word on why ({@code moment}: JoinVoice's; null, nothing said).
     */
    static void end(ServerPlayer player, Live live, boolean won, String moment) {
        if (LIVE.get(live.player) != live) {
            return; // already over
        }
        LIVE.remove(live.player);
        ServerLevel level = player.m_284548_();
        long now = level.m_46467_();
        clear(level, live, now);
        if (live.kind == JoinTrial.Kind.SPAR && level.m_6815_(live.leader) instanceof EtoNpc ayato && ayato.mind != null) {
            if (ayato.mind.target == player.m_19879_()) {
                ayato.mind.target = 0;
                ayato.mind.dirty = true;
            }
            ayato.mind.grudges.remove(player.m_20148_());
        }
        if (live.courting != null) {
            courted(player, live, won, moment, level, now);
            return;
        }
        int gang = JoinTrial.gang(live.kind);
        if (won) {
            GangPlayer g = GangServer.of(player);
            GangPlayer after = g.advance(gang, JoinTrial.step(live.kind), JoinTrial.after(live.kind));
            if (!after.equals(g)) {
                GangServer.set(player, after);
            }
            UkakuFire.sound(level, player.m_20182_(), "entity.player.levelup", 0.7F, 1.2F);
            level.m_8767_(ParticleTypes.f_123748_, player.m_20185_(), player.m_20186_() + 1.0, player.m_20189_(), 12, 0.5, 0.6, 0.5, 0.0);
        } else if (moment != null) {
            UkakuFire.sound(level, player.m_20182_(), "block.candle.extinguish", 1.0F, 0.8F);
        }
        if (moment != null) {
            player.m_5661_(Component.m_237113_(GangServer.colour(gang) + leader(gang) + ": §f" + JoinVoice.say(live.kind, moment, live.seed)), false);
            String next = JoinTrial.goal(gang, won ? JoinTrial.after(live.kind) : JoinTrial.step(live.kind));
            player.m_5661_(Component.m_237113_(GangServer.colour(gang) + GangPlayer.title(gang) + ": §f" + (won ? next : "try again: " + lowerFirst(next))), true);
        }
    }

    /**
     * 4.1.80: a courtship's spar over: won, the chapter done and its moment brought to them at once (he is right there); lost, his word on
     * why ({@code moment}: CourtVoice's; null, nothing said).
     */
    static void courted(ServerPlayer player, Live live, boolean won, String moment, ServerLevel level, long now) {
        com.hex.kagune.control.npc.Persona persona = com.hex.kagune.control.npc.Persona.of(live.courting);
        if (won && CourtshipServer.count(player, live.courting, com.hex.kagune.control.npc.Courtship.Task.SPAR) != null) {
            UkakuFire.sound(level, player.m_20182_(), "entity.player.levelup", 0.7F, 1.2F);
            level.m_8767_(ParticleTypes.f_123748_, player.m_20185_(), player.m_20186_() + 1.0, player.m_20189_(), 12, 0.5, 0.6, 0.5, 0.0);
            if (level.m_6815_(live.leader) instanceof EtoNpc companion) {
                com.hex.kagune.control.npc.Courtship.Chapter chapter = com.hex.kagune.control.npc.Courtship.chapter(live.courting,
                    EtoServer.courtBond(player, live.courting));
                if (chapter != null) {
                    SceneServer.approachMoment(companion, player, SceneServer.scene(live.courting + "." + chapter.id()), now);
                }
            }
        } else if (moment != null) {
            UkakuFire.sound(level, player.m_20182_(), "block.candle.extinguish", 1.0F, 0.8F);
        }
        if (moment != null) {
            player.m_5661_(Component.m_237113_(persona.colour() + persona.name() + ": §f" + com.hex.kagune.control.npc.CourtVoice.spar(moment, live.seed)),
                false);
        }
    }

    /** {@code live}'s bodies gone: the regulars walk out, the rest in a puff of smoke. */
    static void clear(ServerLevel level, Live live, long now) {
        for (UUID one : new ArrayList<>(live.marked)) {
            OWNER.remove(one);
            Entity body = level.m_8791_(one);
            if (body == null || !body.m_6084_()) {
                continue;
            }
            if (body instanceof GhoulNpc npc && REGULAR.equals(role(npc)) && npc.mind != null) {
                OWNER.put(one, live.player); // kept known while it walks out (not an orphan for those few seconds)
                leaves(npc, now);
                continue;
            }
            level.m_8767_(ParticleTypes.f_123759_, body.m_20185_(), body.m_20186_() + 0.8, body.m_20189_(), 14, 0.4, 0.6, 0.4, 0.03);
            body.m_146870_();
        }
        live.marked.clear();
    }

    /** A regular walked out: forgotten (GhoulNpcBrain's leave takes them away). */
    public static void gone(Entity body) {
        OWNER.remove(body.m_20148_());
    }

    /** The leader of {@code gang}'s way in, by name. */
    static String leader(int gang) {
        return switch (gang) {
            case GangPlayer.AOGIRI -> "Ayato";
            case GangPlayer.CLOWNS -> "Roma";
            case GangPlayer.GAS_MASKS -> "Banjou";
            case GangPlayer.ANTEIKU -> "Ken";
            default -> "The General";
        };
    }

    private static String lowerFirst(String text) {
        return text.isEmpty() ? text : Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }

    /** Each second: where they are in it, in the action bar (time left, what is next, which way). */
    static void bar(ServerPlayer player, Live live, long now) {
        int gang = JoinTrial.gang(live.kind);
        String head = live.courting != null ? com.hex.kagune.control.npc.Persona.of(live.courting).colour() + "❤ " // 4.1.80: his courtship's
            : GangServer.colour(gang) + GangPlayer.title(gang) + ": §f";
        ServerLevel level = player.m_284548_();
        String text = switch (live.kind) {
            case SPAR -> live.fighting(now) ? "spar §7(" + JoinTrial.clock(live.began + JoinTrial.SPAR_COUNTDOWN + JoinTrial.SPAR_TICKS - now) + ")" : "";
            case AMBUSH -> "the patrol, " + toward(player, level, live.marked) + (live.first == Long.MIN_VALUE ? ""
                : " §c" + JoinTrial.clock(live.first + JoinTrial.AMBUSH_CLOCK - now));
            case RING -> live.current == null ? "the next act... §7(" + live.done + "/" + JoinTrial.RING_ACTS + ")"
                : "act " + (live.done + 1) + " §c" + JoinTrial.clock(live.currentAt + JoinTrial.RING_ACT - now);
            case GAS -> live.first == Long.MIN_VALUE ? "go out past the gate §7(" + Math.round(Math.max(0.0, JoinTrial.GAS_START - distance(player, live.anchor)))
                + " blocks more)" : "to Banjou, " + way(player, live.anchor) + " §c" + JoinTrial.clock(live.first + JoinTrial.GAS_TICKS - now);
            case GATE -> "hold the gate §7(wave " + live.wave + "/" + JoinTrial.GATE_WAVES + ", " + standing(level, live) + " up) §c"
                + JoinTrial.clock(live.began + JoinTrial.GATE_TICKS - now);
            case SHIFT -> live.current == null ? "the next regular... §7(" + live.done + "/" + JoinTrial.SHIFT_CUSTOMERS + " served)"
                : "a regular is waiting §c" + JoinTrial.clock(live.currentAt + JoinTrial.SHIFT_PATIENCE - now);
            case CAFE -> "drive them off, kill nobody §7(" + live.done + "/" + JoinTrial.CAFE_RAIDERS + ") §c" + JoinTrial.clock(live.began + JoinTrial.CAFE_TICKS - now);
            case HUNT -> "the reported ghoul, " + toward(player, level, live.target == null ? List.of() : List.of(live.target)) + " §7(partner "
                + partnerHealth(level, live) + ")";
        };
        if (!text.isEmpty()) {
            player.m_5661_(Component.m_237113_(head + text), true);
        }
    }

    static String toward(ServerPlayer player, ServerLevel level, List<UUID> bodies) {
        Entity nearest = null;
        double best = Double.MAX_VALUE;
        for (UUID one : bodies) {
            Entity body = level.m_8791_(one);
            if (body != null && body.m_6084_() && body.m_20280_(player) < best) {
                best = body.m_20280_(player);
                nearest = body;
            }
        }
        return nearest == null ? "out of sight" : JoinTrial.way(nearest.m_20185_() - player.m_20185_(), nearest.m_20189_() - player.m_20189_());
    }

    static String way(ServerPlayer player, BlockPos at) {
        return at == null ? "" : JoinTrial.way(at.m_123341_() + 0.5 - player.m_20185_(), at.m_123343_() + 0.5 - player.m_20189_());
    }

    static String partnerHealth(ServerLevel level, Live live) {
        Entity partner = live.partner == null ? null : level.m_8791_(live.partner);
        return partner instanceof LivingEntity body ? Math.round(100.0F * body.m_21223_() / Math.max(1.0F, body.m_21233_())) + "%" : "?";
    }

    // ---------------------------------------------------------------- the academy's test

    /** The test begun for {@code player} (its three questions drawn), or null if today is a failed one's. */
    static int[] sit(ServerPlayer player) {
        CompoundTag kept = GangServer.keptOf(player);
        long today = GangServer.today(player);
        if (!JoinTrial.maySit(today, kept.m_128441_(EXAM_FAILED) ? kept.m_128454_(EXAM_FAILED) : Long.MIN_VALUE)) {
            return null;
        }
        int[] drawn = JoinTrial.draw(today, player.m_20148_().getLeastSignificantBits());
        int[] state = {drawn[0], drawn[1], drawn[2], 0};
        EXAMS.put(player.m_20148_(), state);
        return state;
    }

    /** The test failed today: kept, so it is sat again tomorrow. */
    static void failed(ServerPlayer player) {
        EXAMS.remove(player.m_20148_());
        GangServer.keptOf(player).m_128356_(EXAM_FAILED, GangServer.today(player));
    }

    /** 4.1.79: Roma's mask lent for the job, once: true if it was lent now. */
    static boolean lend(ServerPlayer player) {
        CompoundTag kept = GangServer.keptOf(player);
        if (kept.m_128471_(LENT)) {
            return false;
        }
        kept.m_128379_(LENT, true);
        net.minecraft.world.item.Item item = com.hex.kagune.control.ControlItems.vanilla(com.hex.kagune.control.npc.GangDaily.CLOWN_MASK);
        if (item != null) {
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item, 1);
            if (!player.m_36356_(stack)) {
                player.m_36176_(stack, false);
            }
        }
        return true;
    }
}
