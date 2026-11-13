package com.hex.kagune.control.server;

import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CGangName;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.GangDaily;
import com.hex.kagune.control.npc.GangPlayer;
import com.hex.kagune.control.npc.GangStanding;
import com.hex.kagune.control.npc.GhoulNpc;
import com.hex.kagune.control.npc.NpcIdentity;
import com.hex.kagune.control.npc.Persona;
import com.hex.kagune.control.Safety;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 4.1.50: players in the gangs ({@link GangPlayer}), on the server. Their gang is kept with them ({@code PlayerPersisted}: through death
 * and logging out); their name shows it to everyone ({@link #name}: the emblem, a space, the name in the gang's colour, in chat and over
 * their head: every client is told each player's gang, {@link S2CGangName}); kills of the CCG, the Gas Masks and their own are counted
 * ({@link #died}); a gang never goes for its own and goes for what goes for them ({@link #stance}, {@link #threatens}); and the ways in are
 * talked through with Ayato (Aogiri: "Kill 5 CCG") and Roma (the Clowns: a joke, three Gas Masks, a quinque case) ({@link #talk}). 4.1.54:
 * and Banjou (the Gas Masks: an answer that takes him seriously, three Clowns, a clown's mask); their traits (the owner: "While wearing the
 * gas Mask, gain night vision. / Gain speed 2 when at low HP.": {@link #gasTraits}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class GangServer {
    private static final String GANG = "HexGang";
    private static final String OWN = "HexGangOwnKills";
    private static final String BANNED = "HexGangBanned";
    private static final String AOGIRI_QUEST = "HexGangAogiriQuest";
    private static final String CCG = "HexGangCcgKills";
    private static final String CLOWN_STEP = "HexGangClownStep";
    /** 4.1.54: the Gas Masks' chain (where it stands, the Clowns killed for it). */
    private static final String GAS_STEP = "HexGangGasStep";
    /** 4.1.70: Ken's way into Anteiku: its step, and the investigators killed for it. */
    private static final String ANTEIKU_STEP = "HexGangAnteikuStep";
    /** 4.1.71: the General's way into the CCG: its step, and the ghouls killed for it. */
    private static final String GENERAL_STEP = "HexGangGeneralStep";
    /**
     * 4.1.79: Aogiri's way in, a step of its own (the spar, the ambush, done). One saved before has none: read from 4.1.50's quest flag and
     * its count of the CCG ({@link GangPlayer#aogiriFrom}). The old counts (the CCG's, the Gas Masks', the Clowns', the doves', the ghouls')
     * are no longer kept: the trials are held live (JoinServer).
     */
    private static final String AOGIRI_STEP = "HexGangAogiriStep";
    /** How far a gang's member is looked for round one of the gang that might defend them (blocks). */
    static final double DEFEND = 24.0;
    /** What a gang is to a player: nothing, theirs, or one that banned them. */
    static final int STRANGER = 0;
    static final int OWN_GANG = 1;
    static final int OUTCAST = 2;

    /** 4.1.50: each player's gang as this side knows it (a client's: as the server told it; read by {@link #name}). */
    public static final Map<UUID, Integer> SEEN = new ConcurrentHashMap<>();

    private GangServer() {
    }

    // ---------------------------------------------------------------- kept with the player

    private static CompoundTag kept(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.m_128441_("PlayerPersisted")) {
            root.m_128365_("PlayerPersisted", new CompoundTag());
        }
        return root.m_128469_("PlayerPersisted");
    }

    /** 4.1.79: what is kept with {@code player} (JoinServer's days and the mask lent sit beside the gang's). */
    static CompoundTag keptOf(ServerPlayer player) {
        return kept(player);
    }

    /** {@code player}'s gang and where they stand with each. */
    public static GangPlayer of(ServerPlayer player) {
        CompoundTag k = kept(player);
        int aogiri = k.m_128441_(AOGIRI_STEP) ? k.m_128451_(AOGIRI_STEP) : GangPlayer.aogiriFrom(k.m_128471_(AOGIRI_QUEST), k.m_128451_(CCG));
        return new GangPlayer(k.m_128451_(GANG), k.m_128451_(OWN), k.m_128451_(BANNED), aogiri, k.m_128451_(CLOWN_STEP), k.m_128451_(GAS_STEP),
            k.m_128451_(ANTEIKU_STEP), k.m_128451_(GENERAL_STEP));
    }

    /** {@code player}'s gang kept as {@code g}; a change of gang shown on their name everywhere. */
    static void set(ServerPlayer player, GangPlayer g) {
        int was = of(player).gang();
        CompoundTag k = kept(player);
        k.m_128405_(GANG, g.gang());
        k.m_128405_(OWN, g.ownKills());
        k.m_128405_(BANNED, g.banned());
        k.m_128405_(AOGIRI_STEP, g.aogiriStep());
        k.m_128405_(CLOWN_STEP, g.clownStep());
        k.m_128405_(GAS_STEP, g.gasStep());
        k.m_128405_(ANTEIKU_STEP, g.anteikuStep());
        k.m_128405_(GENERAL_STEP, g.generalStep());
        if (was != g.gang()) {
            SEEN.put(player.m_20148_(), g.gang());
            player.refreshDisplayName();
            tellEveryone(player, g.gang());
        }
    }

    /** Every player on the server, in every world. */
    private static List<ServerPlayer> everyone(ServerPlayer near) {
        List<ServerPlayer> out = new ArrayList<>();
        net.minecraft.server.MinecraftServer server = near.m_284548_().m_7654_();
        for (net.minecraft.server.level.ServerLevel level : server == null ? List.of(near.m_284548_()) : server.m_129785_()) {
            out.addAll(level.m_6907_());
        }
        return out;
    }

    /** Every player told {@code player}'s gang (their name over their head, in chat). */
    private static void tellEveryone(ServerPlayer player, int gang) {
        for (ServerPlayer other : everyone(player)) {
            ControlNetwork.sendTo(other, new S2CGangName(player.m_20148_(), gang));
        }
    }

    /** True if {@code player} holds an investigator's quinque case (its case, shut). */
    static boolean holdsCase(ServerPlayer player) {
        return com.hex.kagune.control.ControlItems.quinqueCase(player.m_21205_()) != null;
    }

    /** The quinque case in {@code player}'s hand given over (kept in creative): true if there was one. */
    static boolean takeCase(ServerPlayer player) {
        if (!holdsCase(player)) {
            return false;
        }
        if (!player.m_7500_()) {
            player.m_21205_().m_41774_(1);
        }
        return true;
    }

    // ---------------------------------------------------------------- what a gang is to a player

    /** What the gang of {@code who} is to {@code player}: {@link #OWN_GANG}, {@link #OUTCAST} (it banned them), or {@link #STRANGER}. */
    static int stance(ServerPlayer player, NpcIdentity who) {
        int gang = GangPlayer.of(who);
        if (gang == GangPlayer.NONE) {
            return STRANGER;
        }
        GangPlayer g = of(player);
        return g.member(gang) ? OWN_GANG : g.banned(gang) ? OUTCAST : STRANGER;
    }

    /**
     * True if {@code body} is going for one of {@code who}'s gang's players near {@code npc} (a mob after them, or what last hurt them): the
     * gang defends its own (the owner: "they will however help the player and defend him against hostiles").
     */
    static boolean threatens(LivingEntity npc, NpcIdentity who, LivingEntity body) {
        int gang = GangPlayer.of(who);
        if (gang == GangPlayer.NONE || body instanceof Player && !(body instanceof ServerPlayer)) {
            return false;
        }
        for (ServerPlayer player : npc.m_9236_().m_6443_(ServerPlayer.class, npc.m_20191_().m_82400_(DEFEND), p -> p.m_6084_() && !p.m_5833_())) {
            if (player == body || !of(player).member(gang)) {
                continue;
            }
            if (body instanceof ServerPlayer other && of(other).member(gang)) {
                continue; // never one of their own
            }
            if (body instanceof Mob mob && mob.m_5448_() == player || player.m_21188_() == body && body.m_6084_()) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- kills counted

    @SubscribeEvent
    public static void died(LivingDeathEvent event) {
        try {
            if (!(event.getSource().m_7639_() instanceof ServerPlayer player)) {
                return;
            }
            LivingEntity dead = event.getEntity();
            GangPlayer was = of(player);
            GangPlayer now = was;
            boolean investigator = AgentBrain.ccg(dead) || dead instanceof GhoulNpc division && division.identity() != null && division.identity().ccg();
            if (investigator && clownMaskOn(player)) {
                // 4.1.79: the Clowns' job, in a Clown's face: an investigator (of either kind) killed wearing one of their masks.
                now = now.advance(GangPlayer.CLOWNS, GangPlayer.CLOWN_JOB, GangPlayer.CLOWN_CASE);
            } else if (investigator && was.clownStep() == GangPlayer.CLOWN_JOB && !was.member(GangPlayer.CLOWNS)) {
                player.m_5661_(Component.m_237113_("§9Roma: §f\"No face, no show!\" §7(a Clown's mask on your head, for the job)"), true);
            }
            if (dead instanceof com.hex.kagune.control.npc.CcgAgent && was.member(GangPlayer.CCG)) {
                now = now.killedOwn(); // 4.1.71: an investigator is one of a member's own (the division's ghouls are, below)
            }
            if (dead instanceof GhoulNpc ghoul && ghoul.identity() != null && was.member(GangPlayer.of(ghoul.identity()))) {
                now = now.killedOwn();
            }
            // 4.1.52: a member's reputation: up for the CCG and for other gangs killed, down a lot for one of their own.
            int by = 0;
            if (was.member(GangPlayer.CCG)) {
                // 4.1.71: one of the CCG: up for any ghoul (but its own division's), down a lot for one of the CCG.
                NpcIdentity who = dead instanceof GhoulNpc ghoul ? ghoul.identity() : null;
                by = AgentBrain.ccg(dead) ? -GangStanding.KILL_OWN : who != null ? GangStanding.KILL_RIVAL : 0;
            } else if (was.gang() != GangPlayer.NONE) {
                NpcIdentity who = dead instanceof GhoulNpc ghoul ? ghoul.identity() : null;
                if (AgentBrain.ccg(dead) || who != null && who.ccg()) {
                    by = GangStanding.KILL_CCG;
                } else if (who != null && (who.aogiri() || who.gang() != 0) && !was.member(GangPlayer.of(who)) && !was.member(GangPlayer.ANTEIKU)) {
                    by = GangStanding.KILL_RIVAL; // 4.1.70: never for one of Anteiku (it is no gang's rival)
                } else if (who != null && was.member(GangPlayer.of(who))) {
                    by = -GangStanding.KILL_OWN;
                }
            }
            // 4.1.53: today's daily quest's kills. 4.1.78: as tags (the quest log's: the victim's, the player's as they killed; and who
            // the victim was to the gangs), so a job asks for any of them ("investigators at night", "with only your eyes", "in mid-air").
            if (was.gang() != GangPlayer.NONE) {
                NpcIdentity who = dead instanceof GhoulNpc ghoul ? ghoul.identity() : null;
                java.util.Set<String> tags = new java.util.HashSet<>(com.hex.kagune.data.KaguneProfile.isUnlocked(player) ? QuestServer.context(player)
                    : QuestServer.bare(player)); // a human's: nothing of a ghoul's read (or written)
                tags.addAll(QuestServer.victim(dead));
                tags.addAll(dailyTags(was, who, AgentBrain.ccg(dead)));
                dailyKill(player, tags, run(player, player.m_9236_().m_46467_()));
            }
            if (now.equals(was)) {
                if (by != 0) {
                    reputation(player, by);
                }
                return;
            }
            set(player, now);
            if (by != 0 && now.gang() == was.gang()) {
                reputation(player, by);
            }
            if (now.clownStep() != was.clownStep()) {
                player.m_5661_(Component.m_237113_("§9The Clowns: §fthe job's done, in her face. §7(now the dove's quinque case, for Roma)"), false);
            }
            if (now.gang() == GangPlayer.NONE && was.gang() != GangPlayer.NONE) {
                player.m_5661_(Component.m_237113_("§c" + cap(GangPlayer.title(was.gang())) + " has banned you for killing its own. Your tag is gone, "
                    + "and they will kill you on sight."), false);
            } else if (now.ownKills() != was.ownKills()) {
                player.m_5661_(Component.m_237113_("§cYou killed one of your own (" + now.ownKills() + "). Over " + GangPlayer.BAN_OVER
                    + " and you are out, for good."), true);
            }
        } catch (RuntimeException | LinkageError failure) {
            Safety.caught(failure, "GangServer.died");
        }
    }

    private static String cap(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    // ---------------------------------------------------------------- 4.1.52: reputation

    private static final String REP = "HexGangRep";
    private static final String REP_DAY = "HexGangRepDay";
    private static final String WARNED = "HexGangWarned";
    /**
     * How far (blocks, on the level) from its claimed spot's middle a gang's hideout reaches: Aogiri's mansion (272 x 263, moved up to 32
     * off its spot to find its ground), the Clowns' tent (77 x 49).
     */
    static final double AOGIRI_HIDEOUT = 180.0;
    static final double CLOWN_HIDEOUT = 70.0;
    /** 4.1.54: the Gas Masks' hideout (28 x 19, its garden walled): a little past its walls. */
    static final double GAS_HIDEOUT = 40.0;
    /** 4.1.71: the CCG's tower (31 x 46, its forecourt round it). */
    static final double CCG_HIDEOUT = 50.0;
    /** The members their gang has turned on (the bar empty, warned, still in its hideout), as of their last check. */
    private static final java.util.Set<UUID> TURNED = ConcurrentHashMap.newKeySet();
    /** When each member last had a blow on one of their own counted (the world's game time). */
    private static final Map<UUID, Long> STRUCK = WorldTimes.map();

    /** {@code player}'s standing in their gang as kept (a member from before 4.1.52: at half). */
    public static GangStanding standing(ServerPlayer player) {
        CompoundTag k = kept(player);
        if (!k.m_128441_(REP)) {
            return GangStanding.NEW;
        }
        return new GangStanding(k.m_128451_(REP), k.m_128454_(REP_DAY), k.m_128454_(WARNED));
    }

    static void standing(ServerPlayer player, GangStanding s) {
        CompoundTag k = kept(player);
        k.m_128405_(REP, s.reputation());
        k.m_128356_(REP_DAY, s.day());
        k.m_128356_(WARNED, s.warnedAt());
    }

    /** The game day (whole days of the world's clock). */
    static long today(ServerPlayer player) {
        return player.m_9236_().m_46467_() / GangStanding.DAY;
    }

    /** {@code player}'s reputation now (drifted to today), or -1 if they are in no gang. */
    public static int reputation(ServerPlayer player) {
        return of(player).gang() == GangPlayer.NONE ? -1 : standing(player).drifted(today(player)).reputation();
    }

    /** The talk screen's standing for {@code player} talking to one of {@code gang}: reputation | gang << 8, or -1 (not theirs). */
    static int shown(ServerPlayer player, int gang) {
        if (gang == GangPlayer.NONE || !of(player).member(gang)) {
            return -1;
        }
        return reputation(player) | gang << 8;
    }

    /** {@code by} added to a member's reputation (taken, if less than 0), shown over their hotbar. */
    static void reputation(ServerPlayer player, int by) {
        GangPlayer g = of(player);
        if (g.gang() == GangPlayer.NONE || by == 0) {
            return;
        }
        long today = today(player);
        GangStanding before = standing(player).drifted(today);
        GangStanding after = before.plus(by, today);
        standing(player, after);
        if (after.reputation() != before.reputation()) {
            String band = GangStanding.band(after.reputation());
            boolean moved = !band.equals(GangStanding.band(before.reputation()));
            player.m_5661_(Component.m_237113_(colour(g.gang()) + cap(GangPlayer.title(g.gang())) + " §7reputation §f" + after.reputation() + "% "
                + (by > 0 ? "§a+" + by : "§c" + by) + (moved ? " §7(" + band + ")" : "")), true);
        }
    }

    /** 4.1.52: a blow on one of their own (one counted every {@link GangStanding#HIT_REST} ticks): their reputation down a little. */
    static void struckOwn(ServerPlayer player, long now) {
        Long last = STRUCK.get(player.m_20148_());
        if (last != null && now >= last && now - last < GangStanding.HIT_REST) {
            return;
        }
        STRUCK.put(player.m_20148_(), now);
        reputation(player, -GangStanding.HIT_OWN);
    }

    /** True if {@code player}'s gang goes for them now (the bar empty, told to leave its hideout half a minute ago, still in it). */
    public static boolean turned(ServerPlayer player) {
        return TURNED.contains(player.m_20148_());
    }

    /**
     * True if {@code player} is in their gang's hideout: Aogiri's mansion, the Clowns' tent, the Gas Masks' hideout, 4.1.71: the CCG's tower
     * (their claimed spots, in the overworld).
     */
    static boolean inHideout(ServerPlayer player, int gang) {
        if (gang != GangPlayer.AOGIRI && gang != GangPlayer.CLOWNS && gang != GangPlayer.GAS_MASKS && gang != GangPlayer.CCG
            || player.m_9236_().m_46472_() != net.minecraft.world.level.Level.f_46428_) {
            return false;
        }
        int[] chunk = com.hex.kagune.control.PlaceClaims.claimed(gang == GangPlayer.AOGIRI ? "aogiri_hideout" : gang == GangPlayer.CLOWNS ? "circus"
            : gang == GangPlayer.CCG ? "ccg_building" : "gas_hideout");
        if (chunk == null) {
            return false;
        }
        double reach = gang == GangPlayer.AOGIRI ? AOGIRI_HIDEOUT : gang == GangPlayer.CLOWNS ? CLOWN_HIDEOUT : gang == GangPlayer.CCG ? CCG_HIDEOUT
            : GAS_HIDEOUT;
        double dx = player.m_20185_() - ((chunk[0] << 4) + 8);
        double dz = player.m_20189_() - ((chunk[1] << 4) + 8);
        return dx * dx + dz * dz <= reach * reach;
    }

    /**
     * Once a second for each member: the bar drifting toward half over the days; at 0 in the hideout, told to leave, then (the owner: "They
     * attack after a warning") turned on after {@link GangStanding#GRACE}; out of it again, let go (not banned: it can rise again).
     */
    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        try {
            if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.f_19797_ % 20 != 0) {
                return;
            }
            GangPlayer g = of(player);
            UUID id = player.m_20148_();
            if (g.gang() == GangPlayer.NONE) {
                TURNED.remove(id);
                gasTraits(player, g); // 4.1.54: a night vision of the mask's taken back
                return;
            }
            long now = player.m_9236_().m_46467_();
            nag(player, g, now); // 4.1.67
            watch(player); // 4.1.78: a night's watch for a daily
            vanish(player, now); // 4.1.53
            gasTraits(player, g); // 4.1.54
            if (kept(player).m_128451_(MASK) != g.gang()) {
                giveMask(player, g.gang()); // 4.1.53: a member from before it got theirs
            }
            GangStanding was = standing(player);
            GangStanding s = was.drifted(now / GangStanding.DAY);
            boolean inside = inHideout(player, g.gang());
            String who = colour(g.gang()) + cap(GangPlayer.title(g.gang()));
            if (s.empty() && inside) {
                if (s.warnedAt() < 0L) {
                    s = s.warn(now);
                    player.m_5661_(Component.m_237113_(who + ": §cyou are not welcome here anymore. Leave the hideout. You have "
                        + GangStanding.GRACE / 20L + " seconds."), false);
                } else if (!s.turned(now, true)) {
                    player.m_5661_(Component.m_237113_("§cLeave the hideout: §f" + s.secondsLeft(now) + "s"), true);
                } else if (!TURNED.contains(id)) {
                    player.m_5661_(Component.m_237113_(who + " §cturns on you."), false);
                }
            } else if (s.warnedAt() >= 0L) {
                s = s.calm();
                if (s.empty() && !inside) {
                    player.m_5661_(Component.m_237113_("§7You left the hideout. They let you go, for now."), true);
                }
            }
            if (s.turned(now, inside)) {
                TURNED.add(id);
            } else {
                TURNED.remove(id);
            }
            if (!s.equals(was)) {
                standing(player, s);
            }
        } catch (RuntimeException | LinkageError failure) {
            Safety.caught(failure, "GangServer.tick");
        }
    }

    // ---------------------------------------------------------------- 4.1.53: daily quests (4.1.78: the board)

    private static final String DAILY_KIND = "HexDailyKind";
    private static final String DAILY_GOAL = "HexDailyGoal";
    private static final String DAILY_COUNT = "HexDailyCount";
    private static final String DAILY_DAY = "HexDailyDay";
    private static final String DAILY_PAID = "HexDailyPaid";
    /** 4.1.78: days in a row with a daily done, and the last such day. */
    private static final String DAILY_STREAK = "HexDailyStreak";
    private static final String DAILY_LAST = "HexDailyLast";
    /** 4.1.78: within this (blocks) of a place's official NPC's home, a night's watch counts. */
    public static final double WATCH_REACH = 40.0;
    /** 4.1.78: each player's run of kills: {the tick of the last, how many in a row} (a Clown's show). */
    private static final Map<UUID, long[]> RUNS = new java.util.concurrent.ConcurrentHashMap<>();

    /** {@code player}'s daily quest as kept. */
    public static GangDaily daily(ServerPlayer player) {
        CompoundTag k = kept(player);
        if (!k.m_128441_(DAILY_KIND)) {
            return GangDaily.NONE;
        }
        return new GangDaily(k.m_128451_(DAILY_KIND), k.m_128451_(DAILY_GOAL), k.m_128451_(DAILY_COUNT), k.m_128454_(DAILY_DAY), k.m_128471_(DAILY_PAID));
    }

    static void daily(ServerPlayer player, GangDaily d) {
        CompoundTag k = kept(player);
        k.m_128405_(DAILY_KIND, d.kind());
        k.m_128405_(DAILY_GOAL, d.goal());
        k.m_128405_(DAILY_COUNT, d.count());
        k.m_128356_(DAILY_DAY, d.day());
        k.m_128379_(DAILY_PAID, d.claimed());
    }

    /**
     * 4.1.53: the gang whose daily quests {@code persona} gives (the owner: Noro for Aogiri; Roma, the Clowns' leader), or none. 4.1.78:
     * Ken Anteiku's (it had none).
     */
    static int gives(Persona persona) {
        return persona == Persona.NORO ? GangPlayer.AOGIRI : persona == Persona.ROMA ? GangPlayer.CLOWNS : persona == Persona.BANJOU
            ? GangPlayer.GAS_MASKS // 4.1.54: Banjou the Gas Masks' (the owner: "Daily quest giver")
            : persona == Persona.GENERAL ? GangPlayer.CCG // 4.1.71: the General the CCG's ("and now daily quests as well")
            : persona == Persona.KEN ? GangPlayer.ANTEIKU : GangPlayer.NONE;
    }

    /** 4.1.78: who gives {@code gang}'s dailies, by name. */
    static String giver(int gang) {
        return switch (gang) {
            case GangPlayer.AOGIRI -> "Noro";
            case GangPlayer.GAS_MASKS -> "Banjou";
            case GangPlayer.CCG -> "the General";
            case GangPlayer.ANTEIKU -> "Ken";
            default -> "Roma";
        };
    }

    /** What asking {@code persona} for today's job is called (one of their gang only), or null. */
    static String dailyAsk(ServerPlayer player, Persona persona) {
        int gang = gives(persona);
        if (gang == GangPlayer.NONE || !of(player).member(gang)) {
            return null;
        }
        GangDaily d = daily(player);
        if (d.today(today(player)) && !d.claimed()) {
            return d.done() || d.job().brought ? "About today's job..." : "Today's job: how far?";
        }
        return gang == GangPlayer.AOGIRI ? "Any work for me today?" : gang == GangPlayer.GAS_MASKS ? "Anything the boys need today?"
            : gang == GangPlayer.CCG ? "Orders for today, General?" : gang == GangPlayer.ANTEIKU ? "Anything I can do for the café today?"
            : "Got a job for me, boss?";
    }

    /** 4.1.78: true if {@code player} can do {@code job} now: a ghoul for the eyes alone; a watch where the world knows the place. */
    static boolean can(ServerPlayer player, GangDaily.Job job) {
        if (job.ghoulOnly() && !com.hex.kagune.data.KaguneProfile.isUnlocked(player)) {
            return false;
        }
        return job.kind != GangDaily.Kind.WATCH || home(player.m_284548_(), job.place) != null;
    }

    /** 4.1.78: the home of the official NPC {@code key} (the heart of her place), or null if the world has none for her yet. */
    static net.minecraft.core.BlockPos home(net.minecraft.server.level.ServerLevel level, String key) {
        com.hex.kagune.control.npc.UniqueNpc npc = UniqueNpcs.known(level, key);
        return npc == null || !npc.homed ? null : new net.minecraft.core.BlockPos(npc.homeX, npc.homeY, npc.homeZ);
    }

    /**
     * 4.1.78: a kill counted for {@code player}'s daily ({@code tags}: the victim's, theirs as they killed, the gangs': GangDaily.CCG_TAG
     * ...; {@code run}: their kills in a row): a KILL job's if it carries what it asks, a STREAK job's best run.
     */
    static void dailyKill(ServerPlayer player, java.util.Set<String> tags, int run) {
        GangDaily d = daily(player);
        if (!d.today(today(player)) || d.claimed() || d.done()) {
            return;
        }
        GangDaily.Job job = d.job();
        GangDaily after = job.kind == GangDaily.Kind.STREAK ? d.best(run) : job.counts(tags) ? d.counted() : d;
        if (after.equals(d)) {
            return;
        }
        daily(player, after);
        progress(player, after);
    }

    /** 4.1.78: the daily's progress in the action bar ("Daily: Kill 4 investigators (2/4)"; done: who to go back to). */
    private static void progress(ServerPlayer player, GangDaily d) {
        GangDaily.Job job = d.job();
        player.m_5661_(Component.m_237113_(colour(job.gang) + "Daily: §f" + job.text(d.goal()) + " §7(" + d.count() + "/" + d.goal() + ")"
            + (d.done() ? " §a- done, go back to " + giver(job.gang) : "")), true);
    }

    /** 4.1.78: {@code player}'s run of kills in a row, counting this one at {@code now} (each within QuestLog.RAMPAGE_TICKS of the last). */
    static int run(ServerPlayer player, long now) {
        long[] r = RUNS.computeIfAbsent(player.m_20148_(), id -> new long[] {Long.MIN_VALUE / 2, 0L});
        r[1] = now - r[0] <= com.hex.kagune.control.quest.QuestLog.RAMPAGE_TICKS ? r[1] + 1 : 1;
        r[0] = now;
        return (int)r[1];
    }

    /** 4.1.78: a second of a night's watch (every second, a member's): counted near the place, at night, till it is done. */
    static void watch(ServerPlayer player) {
        GangDaily d = daily(player);
        if (!d.today(today(player)) || d.claimed() || d.done() || d.job().kind != GangDaily.Kind.WATCH) {
            return;
        }
        net.minecraft.core.BlockPos home = home(player.m_284548_(), d.job().place);
        if (home == null || player.m_20238_(new net.minecraft.world.phys.Vec3(home.m_123341_() + 0.5, home.m_123342_(), home.m_123343_() + 0.5)) > WATCH_REACH * WATCH_REACH) {
            return;
        }
        if (!QuestServer.night(player)) {
            player.m_5661_(Component.m_237113_(colour(d.job().gang) + "Daily: §7come back at night"), true);
            return;
        }
        GangDaily after = d.counted();
        daily(player, after);
        progress(player, after);
    }

    /**
     * 4.1.53: asking {@code persona} about today's job. 4.1.78: with none taken today, the board: three of the gang's jobs to choose from
     * (GangDaily.offers), the one chosen ({@code key} "daily:take:" its ordinal) taken; with one taken, how it goes, the things it asks for
     * handed over from the hand, or, done, paid (its items, a ghoul's mastery, now and then a mutation point, every third day in a row a
     * little more and every seventh a mutation point, and {@link com.hex.kagune.control.npc.GangStanding#DAILY} reputation). In the giver's
     * own words ({@link #voice}).
     */
    static Said daily(ServerPlayer player, Persona persona, String key, int seed) {
        int gang = gives(persona);
        if (gang == GangPlayer.NONE || !of(player).member(gang)) {
            return new Said(voice(gang == GangPlayer.NONE ? GangPlayer.AOGIRI : gang, "outsider", seed, "", null), null);
        }
        long today = today(player);
        long mine = player.m_20148_().getLeastSignificantBits();
        GangDaily d = daily(player);
        if (!d.today(today)) {
            List<GangDaily.Job> offered = GangDaily.offers(gang, today, mine, job -> can(player, job));
            if (key.startsWith("daily:take:")) {
                GangDaily.Job job = jobOf(key.substring("daily:take:".length()));
                if (job != null && offered.contains(job)) {
                    GangDaily taken = GangDaily.take(job, today, mine);
                    daily(player, taken);
                    progress(player, taken);
                    return new Said(voice(gang, "took", seed, job.asked(taken.goal()), taken), null);
                }
            }
            if (offered.isEmpty()) {
                return new Said(voice(gang, "nothing", seed, "", null), null);
            }
            List<String[]> board = new ArrayList<>();
            for (GangDaily.Job job : offered) {
                board.add(new String[] {job.text(GangDaily.take(job, today, mine).goal()), "daily:take:" + job.ordinal()});
            }
            return new Said(voice(gang, "board", seed, "", null), board);
        }
        if (d.claimed()) {
            return new Said(voice(gang, "rested", seed, "", d), null);
        }
        if (!d.done() && d.job().brought) {
            net.minecraft.world.item.ItemStack held = player.m_21205_();
            boolean wanted = false;
            for (String id : d.job().items) {
                net.minecraft.world.item.Item item = com.hex.kagune.control.ControlItems.vanilla(id);
                wanted |= item != null && !held.m_41619_() && held.m_41720_() == item;
            }
            GangDaily got = d.brought(wanted ? held.m_41613_() : 0);
            if (!got.done()) {
                return new Said(voice(gang, "short", seed, "", d), null);
            }
            if (!player.m_7500_()) {
                held.m_41774_(d.goal());
            }
            d = got;
        }
        if (!d.done()) {
            return new Said(voice(gang, "going", seed, "", d), null);
        }
        payDaily(player, gang, d, mine);
        return new Said(voice(gang, "paid", seed, "", d), null);
    }

    /** 4.1.78: the job saved as {@code ordinal}, or null. */
    static GangDaily.Job jobOf(String ordinal) {
        try {
            int at = Integer.parseInt(ordinal);
            return at > 0 && at < GangDaily.Job.values().length ? GangDaily.Job.values()[at] : null;
        } catch (NumberFormatException bad) {
            return null;
        }
    }

    /**
     * 4.1.78: who a victim was to the gangs, as GangDaily's tags: an investigator (or the CCG's own ghouls), a Gas Mask, a Clown, a ghoul
     * of another gang than {@code was}'s, any ghoul NPC not the CCG's (the General's), one of a gang (the General's).
     */
    static java.util.Set<String> dailyTags(GangPlayer was, NpcIdentity who, boolean agent) {
        java.util.Set<String> tags = new java.util.HashSet<>();
        boolean ccg = agent || who != null && who.ccg();
        if (ccg) {
            tags.add(GangDaily.CCG_TAG);
        }
        if (who != null && who.gangOf() == com.hex.kagune.control.npc.Gang.GAS_MASKS) {
            tags.add(GangDaily.GAS_TAG);
        }
        if (who != null && who.gangOf() == com.hex.kagune.control.npc.Gang.CLOWNS) {
            tags.add(GangDaily.CLOWN_TAG);
        }
        if (who != null && !ccg && (who.aogiri() || who.gang() != 0) && !was.member(GangPlayer.of(who))) {
            tags.add(GangDaily.RIVAL_TAG);
        }
        if (who != null && !who.ccg()) {
            tags.add(GangDaily.GHOUL_TAG); // 4.1.71: the General's jobs
            if (who.aogiri() || who.gang() != 0) {
                tags.add(GangDaily.GANGED_TAG);
            }
        }
        return tags;
    }

    /** 4.1.78: today's daily paid: its items, a ghoul's mastery and points, the streak's bonus, reputation; told in chat. */
    private static void payDaily(ServerPlayer player, int gang, GangDaily d, long mine) {
        daily(player, d.paid());
        CompoundTag k = kept(player);
        int days = GangDaily.streak(k.m_128451_(DAILY_STREAK), k.m_128441_(DAILY_LAST) ? k.m_128454_(DAILY_LAST) : Long.MIN_VALUE / 2, d.day());
        k.m_128405_(DAILY_STREAK, days);
        k.m_128356_(DAILY_LAST, d.day());
        int bonus = GangDaily.bonus(days);
        List<GangDaily.Pay> pays = new ArrayList<>(d.pay(mine));
        if (bonus == GangDaily.EXTRA) {
            for (GangDaily.Pay more : d.pay(mine + 1L)) {
                if (pays.stream().noneMatch(p -> p.item().equals(more.item()))) {
                    pays.add(more);
                    break;
                }
            }
        }
        StringBuilder paid = new StringBuilder();
        for (GangDaily.Pay pay : pays) {
            net.minecraft.world.item.Item item = com.hex.kagune.control.ControlItems.vanilla(pay.item());
            if (item == null) {
                continue;
            }
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item, pay.count());
            paid.append(paid.length() == 0 ? "" : ", ").append(pay.text());
            if (!player.m_36356_(stack)) {
                player.m_36176_(stack, false);
            }
        }
        String ghoul = QuestServer.payGhoul(player, d.mastery(), d.point(mine) || bonus == GangDaily.POINT ? 1 : 0);
        if (!ghoul.isEmpty()) {
            paid.append(paid.length() == 0 ? "" : ", ").append(ghoul);
        }
        reputation(player, com.hex.kagune.control.npc.GangStanding.DAILY);
        player.m_5661_(Component.m_237113_(colour(gang) + "Daily done: §f" + paid + " §7(+" + com.hex.kagune.control.npc.GangStanding.DAILY
            + "% reputation" + (days > 1 ? ", " + days + " days in a row" : "") + ")"), false);
    }

    static String voice(int gang, String moment, int seed, String what, GangDaily d) {
        return com.hex.kagune.control.npc.DailyVoice.voice(gang, moment, seed, what, d);
    }

    private static String lowerFirst(String text) {
        return text.isEmpty() ? text : Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }

    private static String capFirst(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    // ---------------------------------------------------------------- 4.1.53: the gang's mask

    private static final String MASK = "HexGangMask";

    /**
     * 4.1.53 (the owner: "Joining gives the mask"): a new member's gang mask, a wearable head piece (Aogiri's rank and file's Laughing Mask,
     * a Clown's mask): into their inventory (dropped at their feet if it is full), once for each gang they join.
     */
    static void giveMask(ServerPlayer player, int gang) {
        String id = gang == GangPlayer.AOGIRI ? "hexkagune:mask_bin_brothers" : gang == GangPlayer.CLOWNS ? GangDaily.CLOWN_MASK
            : gang == GangPlayer.GAS_MASKS ? GangDaily.GAS_MASK // 4.1.54: the Gas Masks' own
            : gang == GangPlayer.CCG ? GangDaily.ISSUED_CASE : null; // 4.1.71: no mask for the CCG: a new investigator's quinque, in its case
        net.minecraft.world.item.Item item = id == null ? null : com.hex.kagune.control.ControlItems.vanilla(id);
        kept(player).m_128405_(MASK, gang);
        if (item == null) {
            return;
        }
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item, 1);
        if (!player.m_36356_(stack)) {
            player.m_36176_(stack, false);
        }
        player.m_5661_(Component.m_237113_(gang == GangPlayer.CCG ? colour(gang) + "The CCG: §fyour quinque, standard issue. §7(a bikaku blade, in its case)"
            : colour(gang) + cap(GangPlayer.title(gang)) + ": §fyour mask. §7Wear it with pride (on your head)."), false);
    }

    // ---------------------------------------------------------------- 4.1.53: the gangs' traits

    /** How long (ticks) a fight's strength or jump lasts after the last blow (refreshed while the fight goes on). */
    static final int FIGHT_BOOST = 200;
    /** 4.1.53: the Clowns' vanishing: at this share of health or under, for this long (ticks), once every so often (the owner: 5 s, 5 min). */
    public static final float VANISH_AT = 0.3F;
    public static final int VANISH = 100;
    public static final long VANISH_REST = 6000L;
    /** When each Clown last vanished (the world's game time). */
    private static final Map<UUID, Long> VANISHED = WorldTimes.map();

    /**
     * 4.1.53 (the owner): "Aogiri - Wild ghouls no longer attack unless provoked, gain strength when fighting CCG and other gangs" (the
     * strength here, the ghouls in GhoulNpcBrain); the Clowns "Gain Jump boost when fighting CCG". Either way round: a blow given or taken.
     */
    @SubscribeEvent
    public static void fought(LivingHurtEvent event) {
        try {
            LivingEntity hurt = event.getEntity();
            net.minecraft.world.entity.Entity source = event.getSource().m_7639_();
            if (source instanceof ServerPlayer player && source != hurt) {
                boost(player, hurt);
                if (of(player).member(GangPlayer.CCG) && ghoul(hurt)) {
                    // 4.1.71 (the owner, the CCG's traits: "Damage increased when fighting ghouls."): every blow on a ghoul lands harder.
                    event.setAmount(event.getAmount() * GangPlayer.CCG_DAMAGE);
                }
            }
            if (hurt instanceof ServerPlayer player && source instanceof LivingEntity other && source != hurt) {
                boost(player, other);
            }
        } catch (RuntimeException | LinkageError failure) {
            Safety.caught(failure, "GangServer.fought");
        }
    }

    /**
     * 4.1.71: true if {@code body} is a ghoul a CCG member's blows land harder on: a ghoul NPC (not of the CCG's own division), a ghoul player,
     * a ghoul among the named (not the General, a human).
     */
    static boolean ghoul(LivingEntity body) {
        if (body instanceof GhoulNpc npc) {
            return npc.identity() == null || !npc.identity().ccg();
        }
        if (body instanceof ServerPlayer player) {
            return com.hex.kagune.data.KaguneProfile.isUnlocked(player);
        }
        return body instanceof com.hex.kagune.control.npc.EtoNpc named && !named.persona().human();
    }

    /** {@code player} fighting {@code other}: Aogiri's strength against the CCG and the other gangs, the Clowns' jump against the CCG. */
    static void boost(ServerPlayer player, LivingEntity other) {
        GangPlayer g = of(player);
        if (g.gang() == GangPlayer.NONE) {
            return;
        }
        NpcIdentity who = other instanceof GhoulNpc ghoul ? ghoul.identity() : null;
        boolean ccg = AgentBrain.ccg(other) || who != null && who.ccg();
        boolean gang = who != null && !ccg && (who.aogiri() || who.gang() != 0) && !g.member(GangPlayer.of(who));
        if (g.member(GangPlayer.AOGIRI) && (ccg || gang)) {
            player.m_7292_(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.f_19600_, FIGHT_BOOST, 0, false, true, true));
        } else if (g.member(GangPlayer.CLOWNS) && ccg) {
            player.m_7292_(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.f_19603_, FIGHT_BOOST, 1, false, true, true));
        }
    }

    /**
     * 4.1.53 (the owner, of the Clowns: "kagune, player and armor become fully invisible for 5 seconds when at low HP (5 minute cooldown)"):
     * checked once a second; the invisibility is the game's own (every client sees it), and a Clown's hides everything of them
     * ({@code PlayerLayers.hidden}: armour, what they hold, their kagune).
     */
    static void vanish(ServerPlayer player, long now) {
        if (!of(player).member(GangPlayer.CLOWNS) || !player.m_6084_() || player.m_21223_() > player.m_21233_() * VANISH_AT) {
            return;
        }
        Long last = VANISHED.get(player.m_20148_());
        if (last != null && now >= last && now - last < VANISH_REST) {
            return;
        }
        VANISHED.put(player.m_20148_(), now);
        player.m_7292_(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.f_19609_, VANISH, 0, false, false, true));
        player.m_5661_(Component.m_237113_("§9The Clowns: §fnow you see me... §7(5 s; again in 5 minutes)"), true);
    }

    /** 4.1.54: the players given the gas mask's night vision (taken back when the mask comes off, or they leave the gang). */
    private static final java.util.Set<UUID> MASK_SIGHT = ConcurrentHashMap.newKeySet();
    /** 4.1.54: how long each grant of it lasts (ticks; renewed each second): past the game's own flicker in a night vision's last 10 s. */
    static final int MASK_SIGHT_TICKS = 300;
    /** 4.1.54: at this share of health or under, a Gas Mask is quick (the owner: "Gain speed 2 when at low HP"). */
    public static final float GAS_SPEED_AT = 0.3F;

    /**
     * 4.1.54 (the owner, the Gas Masks' "Gang Bonuses": "While wearing the gas Mask, gain night vision. / Gain speed 2 when at low HP."):
     * checked once a second. Night vision while a gas mask (any of the Gas Masks' own: {@code mask_gas}, its variants) is on their head, taken
     * back as soon as it comes off (only ours: a potion's longer one is left alone); Speed II while at {@link #GAS_SPEED_AT} of their health
     * or under.
     */
    static void gasTraits(ServerPlayer player, GangPlayer g) {
        boolean member = g.member(GangPlayer.GAS_MASKS);
        UUID id = player.m_20148_();
        if (member && gasMaskOn(player)) {
            player.m_7292_(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.f_19611_, MASK_SIGHT_TICKS, 0, false, false,
                true));
            MASK_SIGHT.add(id);
        } else if (MASK_SIGHT.remove(id)) {
            net.minecraft.world.effect.MobEffectInstance sight = player.m_21124_(net.minecraft.world.effect.MobEffects.f_19611_);
            if (sight != null && sight.m_19557_() <= MASK_SIGHT_TICKS) {
                player.m_21195_(net.minecraft.world.effect.MobEffects.f_19611_);
            }
        }
        if (member && player.m_6084_() && player.m_21223_() <= player.m_21233_() * GAS_SPEED_AT) {
            player.m_7292_(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.f_19596_, 40, 1, false, false, true));
        }
    }

    /** 4.1.54: true if what is on {@code player}'s head is one of the Gas Masks' masks. */
    static boolean gasMaskOn(ServerPlayer player) {
        net.minecraft.world.item.ItemStack head = player.m_6844_(net.minecraft.world.entity.EquipmentSlot.HEAD);
        return head.m_41720_() instanceof com.hex.kagune.control.outfit.OutfitItem item && item.piece().id().startsWith("mask_gas");
    }

    static String colour(int gang) {
        return gang == GangPlayer.AOGIRI ? "§4" : gang == GangPlayer.CLOWNS ? "§9" : gang == GangPlayer.GAS_MASKS ? "§a" : gang == GangPlayer.ANTEIKU ? "§6"
            : gang == GangPlayer.CCG ? "§b" : "§7";
    }

    // ---------------------------------------------------------------- the name

    /**
     * Their name with their gang on it (the owner: "(symbol) (space) (name)"): asked by the game for chat and the tag over their head; on
     * the server from what is kept with them, on a client from what the server told it.
     */
    @SubscribeEvent
    public static void name(PlayerEvent.NameFormat event) {
        try {
            Player player = event.getEntity();
            Integer gang = player instanceof ServerPlayer server ? Integer.valueOf(of(server).gang()) : SEEN.get(player.m_20148_());
            if (gang != null && gang != GangPlayer.NONE) {
                event.setDisplayname(nameTag(gang, event.getDisplayname().getString()));
            }
        } catch (RuntimeException | LinkageError failure) {
            Safety.caught(failure, "GangServer.name");
        }
    }

    /**
     * 4.1.70: the name as {@link GangPlayer#named} has it; Anteiku's in its own light brown (the owner: "Light brown name tag with a coffee
     * symbol"), none of the sixteen colours a name can be given by its text: the emblem, a space, then the name styled with the colour.
     */
    static Component nameTag(int gang, String name) {
        if (gang == GangPlayer.ANTEIKU) {
            return Component.m_237113_("§f" + GangPlayer.COFFEE + " ").m_7220_(Component.m_237113_(name).m_6270_(
                net.minecraft.network.chat.Style.f_131099_.m_178520_(GangPlayer.ANTEIKU_BROWN)));
        }
        if (gang == GangPlayer.CCG) {
            // 4.1.71 (the owner: "Cyan Blue with a brief cafe symbol"): the quinque case, then the name in the CCG's own cyan blue.
            return Component.m_237113_("§f" + GangPlayer.BRIEFCASE + " ").m_7220_(Component.m_237113_(name).m_6270_(
                net.minecraft.network.chat.Style.f_131099_.m_178520_(GangPlayer.CCG_CYAN)));
        }
        return Component.m_237113_(GangPlayer.named(gang, name));
    }

    /** A player in: they are told every player's gang, and everyone theirs. */
    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            int gang = of(player).gang();
            SEEN.put(player.m_20148_(), gang);
            player.refreshDisplayName();
            tellEveryone(player, gang);
            for (ServerPlayer other : everyone(player)) {
                if (other != player) {
                    ControlNetwork.sendTo(player, new S2CGangName(other.m_20148_(), of(other).gang()));
                }
            }
        } catch (RuntimeException | LinkageError failure) {
            Safety.caught(failure, "GangServer.loggedIn");
        }
    }

    // ---------------------------------------------------------------- the ways in, talked through

    /** What the talk does: the line said, and (the Clowns' joke) the jokes to pick from ({label, key} each); null for none. */
    public record Said(String line, List<String[]> jokes) {
    }

    /**
     * True if {@code persona} is the way into a gang: Ayato (Aogiri), Roma (the Clowns), Banjou (4.1.54: the Gas Masks), Ken (4.1.70:
     * Anteiku; so 4.1.67's rules are his too: marrying him takes a player out of another gang, and married to one in another he asks them out),
     * the General (4.1.71: the CCG; the same rules).
     */
    static boolean leads(Persona persona) {
        return persona == Persona.AYATO || persona == Persona.ROMA || persona == Persona.BANJOU || persona == Persona.KEN || persona == Persona.GENERAL;
    }

    /** The gang {@code persona} is the way into. */
    static int gangOf(Persona persona) {
        return persona == Persona.AYATO ? GangPlayer.AOGIRI : persona == Persona.ROMA ? GangPlayer.CLOWNS : persona == Persona.BANJOU ? GangPlayer.GAS_MASKS
            : persona == Persona.KEN ? GangPlayer.ANTEIKU : persona == Persona.GENERAL ? GangPlayer.CCG : GangPlayer.NONE;
    }

    /**
     * 4.1.52: the gang {@code persona} belongs to (Ayato and Noro Aogiri's, Roma the Clowns', 4.1.54: Banjou the Gas Masks', 4.1.70: Ken and
     * Touka Anteiku's), or none.
     */
    static int memberOf(Persona persona) {
        return persona == Persona.AYATO || persona == Persona.NORO ? GangPlayer.AOGIRI : persona == Persona.ROMA ? GangPlayer.CLOWNS
            : persona == Persona.BANJOU ? GangPlayer.GAS_MASKS : persona == Persona.KEN || persona == Persona.TOUKA ? GangPlayer.ANTEIKU
            : persona == Persona.GENERAL ? GangPlayer.CCG : GangPlayer.NONE;
    }

    /** What asking {@code persona} about their gang is called on the talk screen, by where {@code player} stands. */
    static String ask(ServerPlayer player, Persona persona) {
        GangPlayer g = of(player);
        int gang = gangOf(persona);
        if (g.member(gang)) {
            return gang == GangPlayer.AOGIRI ? "How's Aogiri?" : gang == GangPlayer.GAS_MASKS ? "How are the boys?" : gang == GangPlayer.ANTEIKU
                ? "How's Anteiku?" : gang == GangPlayer.CCG ? "How's the bureau, sir?" : "How are the Clowns?";
        }
        // 4.1.79: by the step their way in is at: its trial, live or not, or what is left to say or bring.
        int step = g.step(gang);
        com.hex.kagune.control.npc.JoinTrial.Kind kind = com.hex.kagune.control.npc.JoinTrial.of(gang, step);
        if (kind != null) {
            return com.hex.kagune.control.npc.JoinVoice.ask(kind);
        }
        return switch (gang) {
            case GangPlayer.AOGIRI -> step == GangPlayer.AOGIRI_DONE ? "It's done. The patrol's down." : "I want to join Aogiri.";
            case GangPlayer.CLOWNS -> step == GangPlayer.CLOWN_CASE ? (holdsCase(player) ? "Here. A dove's quinque case. (the case in your hand)"
                : "About the dove's case...") : step == GangPlayer.CLOWN_JOB ? "About the job..." : "I want to join the Clowns.";
            case GangPlayer.GAS_MASKS -> step == GangPlayer.GAS_DONE ? "The gate held." : "I want to join the Gas Masks.";
            case GangPlayer.ANTEIKU -> step == GangPlayer.ANTEIKU_DONE ? "They're gone. Nobody died." : "I want to join Anteiku.";
            default -> step == GangPlayer.CCG_DONE ? "Target down. My partner's alive, sir." : step == GangPlayer.CCG_EXAM ? "I'm ready for the test."
                : "I want to join the CCG.";
        };
    }

    /**
     * The talk about {@code persona}'s gang ({@code key}: "gang"; "gang:start" a trial begun; "gang:joke:n", "gang:serious:n", "gang:why:n",
     * "gang:serve:n" a first answer; "gang:exam:n" an answer in the academy's test; "gang:quit..." a member's way out): what is said, what it
     * does. 4.1.79: {@code npc} is the leader talked to (Ayato fights them in his spar; the scene at the end is theirs); null where unknown.
     */
    static Said talk(ServerPlayer player, EtoNpc npc, Persona persona, String key, int seed) {
        GangPlayer g = of(player);
        int gang = gangOf(persona);
        if (g.member(gang) && (key.startsWith("gang:quit") || key.equals("gang:stay"))) {
            return quit(player, g, gang, key, seed); // 4.1.67
        }
        if (g.member(gang)) {
            return new Said(memberLine(gang, seed), wayOut(gang));
        }
        if (g.banned(gang)) {
            return new Said(bannedLine(gang), null);
        }
        if (g.gang() != GangPlayer.NONE) {
            return new Said(otherGangLine(gang, g.gang()), null);
        }
        com.hex.kagune.control.npc.JoinTrial.Kind kind = com.hex.kagune.control.npc.JoinTrial.of(gang, g.step(gang));
        if (kind != null) {
            return trial(player, npc, kind, key, seed); // 4.1.79
        }
        return switch (gang) {
            case GangPlayer.AOGIRI -> ayato(player, npc, g, seed);
            case GangPlayer.CLOWNS -> roma(player, npc, g, key, seed);
            case GangPlayer.GAS_MASKS -> banjou(player, npc, g, key, seed); // 4.1.54
            case GangPlayer.ANTEIKU -> ken(player, npc, g, key, seed); // 4.1.70
            default -> general(player, npc, g, key, seed); // 4.1.71
        };
    }

    /**
     * 4.1.79: a step that is a live trial ({@code JoinServer}): put to them with the button that begins it, begun ("gang:start": the leader's
     * word as it starts, or why not now), or asked about while it runs.
     */
    static Said trial(ServerPlayer player, EtoNpc npc, com.hex.kagune.control.npc.JoinTrial.Kind kind, String key, int seed) {
        if (JoinServer.live(player) == kind) {
            return new Said(com.hex.kagune.control.npc.JoinVoice.say(kind, "on", seed), null);
        }
        if (key.equals("gang:start")) {
            return new Said(JoinServer.start(player, npc, kind, seed), null);
        }
        return offer(com.hex.kagune.control.npc.JoinVoice.say(kind, "offer", seed), kind);
    }

    /** {@code line}, and the one button that begins {@code kind}. */
    static Said offer(String line, com.hex.kagune.control.npc.JoinTrial.Kind kind) {
        return new Said(line, List.<String[]>of(new String[] {com.hex.kagune.control.npc.JoinVoice.start(kind), "gang:start"}));
    }

    /** 4.1.79: {@code gang}'s way in moved on to {@code to} by a talk, told in the action bar's words in chat (what it asks now). */
    static GangPlayer moved(ServerPlayer player, GangPlayer g, int gang, int from, int to) {
        GangPlayer after = g.advance(gang, from, to);
        if (!after.equals(g)) {
            set(player, after);
            player.m_5661_(Component.m_237113_(colour(gang) + cap(GangPlayer.title(gang)) + ": §f" + com.hex.kagune.control.npc.JoinTrial.goal(gang, to)), false);
        }
        return after;
    }

    /**
     * 4.1.79: Ayato's way into Aogiri (4.1.50's was "Kill 5 CCG"): asked, he wants a fight first (the spar: JoinServer), then an ambush on
     * a CCG patrol; done, the scene. Ayato is cocky and sore about everything ({@code AyatoScript}).
     */
    static Said ayato(ServerPlayer player, EtoNpc npc, GangPlayer g, int seed) {
        if (g.aogiriStep() == GangPlayer.AOGIRI_DONE) {
            return welcome(player, npc, GangPlayer.AOGIRI, g, seed);
        }
        moved(player, g, GangPlayer.AOGIRI, GangPlayer.AOGIRI_NOT_YET, GangPlayer.AOGIRI_SPAR);
        return trial(player, npc, com.hex.kagune.control.npc.JoinTrial.Kind.SPAR, "gang", seed);
    }

    /**
     * Roma's way into the Clowns: a joke that makes her laugh (4.1.50, kept), then 4.1.79: a show in her ring (JoinServer), a job in her
     * face (an investigator killed in one of the Clowns' masks: she lends one), the dove's case brought; the scene.
     */
    static Said roma(ServerPlayer player, EtoNpc npc, GangPlayer g, String key, int seed) {
        if (g.clownStep() == GangPlayer.CLOWN_NOT_YET) {
            if (key.startsWith("gang:joke:")) {
                int told = number(key, "gang:joke:");
                if (told == FUNNY) {
                    moved(player, g, GangPlayer.CLOWNS, GangPlayer.CLOWN_NOT_YET, GangPlayer.CLOWN_RING);
                    return offer(com.hex.kagune.control.npc.JoinVoice.answered(GangPlayer.CLOWNS)
                        + com.hex.kagune.control.npc.JoinVoice.say(com.hex.kagune.control.npc.JoinTrial.Kind.RING, "offer", seed),
                        com.hex.kagune.control.npc.JoinTrial.Kind.RING);
                }
                return new Said(told == 0 ? "*stares at you* ...That's a dad joke. I'm not your dad. Try again some other time."
                    : "Hey! ...That one's about ME. ...It's a little funny. But no. Come back with better material.", null);
            }
            List<String[]> jokes = new ArrayList<>(List.of(new String[] {JOKES[0], "gang:joke:0"}, new String[] {JOKES[1], "gang:joke:1"},
                new String[] {JOKES[2], "gang:joke:2"}));
            java.util.Collections.rotate(jokes, Math.floorMod(seed, jokes.size()));
            return new Said("Join the Clowns? Make me laugh first! Go on. Tell me a joke. A good one. I'll know.", jokes);
        }
        if (g.clownStep() == GangPlayer.CLOWN_JOB) {
            return new Said(com.hex.kagune.control.npc.JoinVoice.roma(JoinServer.lend(player) ? "job" : "job_on", seed), null);
        }
        if (!takeCase(player)) {
            return new Said(com.hex.kagune.control.npc.JoinVoice.roma("case", seed), null);
        }
        return welcome(player, npc, GangPlayer.CLOWNS, g, seed);
    }

    /**
     * 4.1.54 (the owner: the Gas Masks joined by "Short chain, like Roma"; Banjou "wants to be tough but truly isnt, is aware of how weak he
     * truly is"): Banjou's way in. Something said to him that takes him seriously (of three answers, {@link #SERIOUS}: not mockery, not
     * flattery he knows is false), then 4.1.79: the walk through the gas, the gate held against the Clowns (JoinServer); the scene.
     */
    static Said banjou(ServerPlayer player, EtoNpc npc, GangPlayer g, String key, int seed) {
        if (g.gasStep() == GangPlayer.GAS_DONE) {
            return welcome(player, npc, GangPlayer.GAS_MASKS, g, seed);
        }
        if (key.startsWith("gang:serious:")) {
            int said = number(key, "gang:serious:");
            if (said == SERIOUS) {
                moved(player, g, GangPlayer.GAS_MASKS, GangPlayer.GAS_NOT_YET, GangPlayer.GAS_WALK);
                return offer(com.hex.kagune.control.npc.JoinVoice.answered(GangPlayer.GAS_MASKS)
                    + com.hex.kagune.control.npc.JoinVoice.say(com.hex.kagune.control.npc.JoinTrial.Kind.GAS, "offer", seed),
                    com.hex.kagune.control.npc.JoinTrial.Kind.GAS);
            }
            return new Said(said == 0 ? "*His face goes still.* ...Yeah. I've heard that one. Come back when you mean it."
                : "*He laughs, not unkindly.* Don't. I know what I am. Flattery's how people get my boys killed.", null);
        }
        List<String[]> answers = new ArrayList<>(List.of(new String[] {ANSWERS[0], "gang:serious:0"}, new String[] {ANSWERS[1], "gang:serious:1"},
            new String[] {ANSWERS[2], "gang:serious:2"}));
        java.util.Collections.rotate(answers, Math.floorMod(seed, answers.size()));
        return new Said("Join us? *He straightens up, trying to look like a gang boss.* Why? Tell me why. And don't lie to me. Everyone lies to me.",
            answers);
    }

    /**
     * 4.1.70 (the owner: "Anteikou - Joinable through Ken (quest)"): Ken's way into Anteiku. Why they want in, asked (of three answers,
     * {@code GangWords.KEN_RIGHT} is Anteiku's own: neither strength nor a place to hide), then 4.1.79: a shift at the café (brew and serve),
     * the CCG's raid driven off with nobody killed (JoinServer); the scene. Ken is polite, gentle, a little sad, fond of coffee and books.
     */
    static Said ken(ServerPlayer player, EtoNpc npc, GangPlayer g, String key, int seed) {
        if (g.anteikuStep() == GangPlayer.ANTEIKU_DONE) {
            return welcome(player, npc, GangPlayer.ANTEIKU, g, seed);
        }
        if (key.startsWith("gang:why:")) {
            int said = number(key, "gang:why:");
            if (said == com.hex.kagune.control.npc.GangWords.KEN_RIGHT) {
                moved(player, g, GangPlayer.ANTEIKU, GangPlayer.ANTEIKU_NOT_YET, GangPlayer.ANTEIKU_SHIFT);
                return offer(com.hex.kagune.control.npc.JoinVoice.answered(GangPlayer.ANTEIKU)
                    + com.hex.kagune.control.npc.JoinVoice.say(com.hex.kagune.control.npc.JoinTrial.Kind.SHIFT, "offer", seed),
                    com.hex.kagune.control.npc.JoinTrial.Kind.SHIFT);
            }
            return new Said(said == 0 ? "*He winces.* Then you want the other lot. Anteiku doesn't make anyone stronger. ...It just keeps them human."
                : "*He smiles, but it doesn't reach his eyes.* Everyone here is hiding from something. That can't be the reason, though. Think about "
                + "it, and come back.", null);
        }
        String[] why = com.hex.kagune.control.npc.GangWords.KEN_WHY;
        List<String[]> answers = new ArrayList<>(List.of(new String[] {why[0], "gang:why:0"}, new String[] {why[1], "gang:why:1"},
            new String[] {why[2], "gang:why:2"}));
        java.util.Collections.rotate(answers, Math.floorMod(seed, answers.size()));
        return new Said("Anteiku? *He closes his book on one finger.* It's just a café. ...Mostly. People who join us look after each other. "
            + "Why do you want in? Honestly.", answers);
    }

    /**
     * 4.1.71 (the owner: "CCG Faction: / Join through the general (quest)"): the General's way into the CCG. Why they want to serve (of three
     * answers, {@code GangWords.GENERAL_RIGHT} is the bureau's own), then 4.1.79: the academy's test (three questions drawn from
     * JoinTrial.QUESTIONS; one wrong and it is sat again tomorrow), a ghoul hunted with an assigned partner who comes back alive
     * (JoinServer); then he swears them in. A ghoul may join too (the bureau has a division of them): he says so, flatly.
     */
    static Said general(ServerPlayer player, EtoNpc npc, GangPlayer g, String key, int seed) {
        if (g.generalStep() == GangPlayer.CCG_DONE) {
            return welcome(player, npc, GangPlayer.CCG, g, seed);
        }
        if (g.generalStep() == GangPlayer.CCG_EXAM) {
            return exam(player, g, key, seed);
        }
        if (key.startsWith("gang:serve:")) {
            int said = number(key, "gang:serve:");
            if (said == com.hex.kagune.control.npc.GangWords.GENERAL_RIGHT) {
                moved(player, g, GangPlayer.CCG, GangPlayer.CCG_NOT_YET, GangPlayer.CCG_EXAM);
                return new Said(com.hex.kagune.control.npc.JoinVoice.answered(GangPlayer.CCG)
                    + com.hex.kagune.control.npc.JoinVoice.general("exam", seed), List.<String[]>of(new String[] {"I'm ready for the test.", "gang:exam"}));
            }
            return new Said(said == 0 ? "*He doesn't blink.* Then you want to be a butcher. The bureau doesn't hire butchers. Dismissed."
                : "*Dryly.* It isn't. Ask anyone in this building. Try again when you have a real reason.", null);
        }
        String[] why = com.hex.kagune.control.npc.GangWords.GENERAL_WHY;
        List<String[]> answers = new ArrayList<>(List.of(new String[] {why[0], "gang:serve:0"}, new String[] {why[1], "gang:serve:1"},
            new String[] {why[2], "gang:serve:2"}));
        java.util.Collections.rotate(answers, Math.floorMod(seed, answers.size()));
        boolean ghoul = com.hex.kagune.data.KaguneProfile.isUnlocked(player);
        return new Said((ghoul ? "You're a ghoul. *Flat, not unkind.* We have a division for that. The oath's the same. " : "")
            + "You want to join the CCG. *He sets the pen down.* Why? One answer. Make it the true one.", answers);
    }

    /**
     * 4.1.79: the academy's test: begun (three questions drawn, the first asked), an answer given ("gang:exam:n": right, the next; the third
     * right, passed and the hunt put to them; wrong, failed till tomorrow), or refused today after a fail.
     */
    static Said exam(ServerPlayer player, GangPlayer g, String key, int seed) {
        int[] state = JoinServer.EXAMS.get(player.m_20148_());
        if (key.startsWith("gang:exam:") && state != null) {
            int answer = number(key, "gang:exam:");
            if (!com.hex.kagune.control.npc.JoinTrial.right(answer)) {
                JoinServer.failed(player);
                UkakuFire.sound(player.m_284548_(), player.m_20182_(), "block.candle.extinguish", 1.0F, 0.8F);
                return new Said(com.hex.kagune.control.npc.JoinVoice.general("failed", seed), null);
            }
            state[3]++;
            if (state[3] >= com.hex.kagune.control.npc.JoinTrial.EXAM_ASKED) {
                JoinServer.EXAMS.remove(player.m_20148_());
                moved(player, g, GangPlayer.CCG, GangPlayer.CCG_EXAM, GangPlayer.CCG_HUNT);
                UkakuFire.sound(player.m_284548_(), player.m_20182_(), "entity.player.levelup", 0.7F, 1.2F);
                return offer(com.hex.kagune.control.npc.JoinVoice.general("passed", seed) + " "
                    + com.hex.kagune.control.npc.JoinVoice.say(com.hex.kagune.control.npc.JoinTrial.Kind.HUNT, "offer", seed),
                    com.hex.kagune.control.npc.JoinTrial.Kind.HUNT);
            }
            return question(com.hex.kagune.control.npc.JoinVoice.general("next", seed), state, seed);
        }
        state = JoinServer.sit(player);
        if (state == null) {
            return new Said(com.hex.kagune.control.npc.JoinVoice.general("today", seed), null);
        }
        return question(com.hex.kagune.control.npc.JoinVoice.general("exam", seed), state, seed);
    }

    /** The question the test is at, its three answers turned round by {@code seed} (the right one anywhere). */
    static Said question(String said, int[] state, int seed) {
        com.hex.kagune.control.npc.JoinTrial.Question q = com.hex.kagune.control.npc.JoinTrial.QUESTIONS[state[state[3]]];
        List<String[]> answers = new ArrayList<>(List.of(new String[] {q.answer(0), "gang:exam:0"}, new String[] {q.answer(1), "gang:exam:1"},
            new String[] {q.answer(2), "gang:exam:2"}));
        java.util.Collections.rotate(answers, Math.floorMod(seed + state[3], answers.size()));
        return new Said(said + " Question " + (state[3] + 1) + " of " + com.hex.kagune.control.npc.JoinTrial.EXAM_ASKED + ": " + q.asked(), answers);
    }

    /** The number after {@code prefix} in {@code key} (-1 if there is none). */
    static int number(String key, String prefix) {
        try {
            return Integer.parseInt(key.substring(prefix.length()));
        } catch (RuntimeException bad) {
            return -1;
        }
    }

    /**
     * 4.1.79 (the plan: "a scene with the leader at the end"): they join {@code gang}: its member, its mask (or the CCG's quinque), and the
     * leader's scene: their gesture (Ayato's arms crossed, Roma's cackle, Banjou's bow, Ken serving a cup, the General's nod), the happy
     * sparks round them both, their words ({@code JoinVoice.scene}).
     */
    static Said welcome(ServerPlayer player, EtoNpc npc, int gang, GangPlayer g, int seed) {
        set(player, g.join(gang));
        joined(player, gang);
        net.minecraft.server.level.ServerLevel level = player.m_284548_();
        if (npc != null && npc.mind != null) {
            EtoBrain.gesture(npc.mind, sceneGesture(gang), level.m_46467_());
            level.m_8767_(net.minecraft.core.particles.ParticleTypes.f_123748_, npc.m_20185_(), npc.m_20186_() + 1.2, npc.m_20189_(), 10, 0.5, 0.6, 0.5, 0.0);
        }
        level.m_8767_(net.minecraft.core.particles.ParticleTypes.f_123748_, player.m_20185_(), player.m_20186_() + 1.2, player.m_20189_(), 10, 0.5, 0.6, 0.5,
            0.0);
        return new Said(com.hex.kagune.control.npc.JoinVoice.scene(gang, seed), null);
    }

    /** 4.1.79: each leader's move in their scene. */
    static com.hex.kagune.control.npc.EtoGesture sceneGesture(int gang) {
        return switch (gang) {
            case GangPlayer.AOGIRI -> com.hex.kagune.control.npc.EtoGesture.CROSS;
            case GangPlayer.CLOWNS -> com.hex.kagune.control.npc.EtoGesture.LAUGH;
            case GangPlayer.ANTEIKU -> com.hex.kagune.control.npc.EtoGesture.SERVE;
            default -> com.hex.kagune.control.npc.EtoGesture.BOW;
        };
    }

    /** One of {@code gang}'s, talking to its leader (4.1.50 to 4.1.71's lines). */
    static String memberLine(int gang, int seed) {
        return switch (gang) {
            case GangPlayer.AOGIRI -> pick(seed, "Aogiri's fine. Don't get sloppy.", "You're one of us. Act like it.", "Still breathing? Good. Aogiri needs you.");
            case GangPlayer.CLOWNS -> pick(seed, "My favourite Clown! Go scare someone.",
                "You wear our face well. ...Kidding, you don't have one. You have mine. In your heart.", "The Clowns are great! Three of them are on fire, but great.");
            case GangPlayer.GAS_MASKS -> pick(seed, "The boys are good. Fed, masked, nobody hurt this week. ...That last part's new. Thank you.",
                "You're one of us. I still can't believe you picked us.", "Everyone's fine. I counted. Twice.");
            case GangPlayer.ANTEIKU -> pick(seed, "Quiet today. Touka broke two cups and said they jumped. ...Anteiku's fine.",
                "You're one of us. That means being kind to people who'd never be kind back. ...It's harder than fighting. I know.",
                "Have you eaten? Properly, I mean. Sit down, I'll make you a cup.");
            default -> pick(seed, "The bureau's holding. Fewer funerals this month. Keep it that way.",
                "You're one of mine now. That means you come back alive. That's an order.",
                "Paperwork's piling up and the coffee's terrible. So: normal. Anything to report?");
        };
    }

    /** One {@code gang} banned, talking to its leader. */
    static String bannedLine(int gang) {
        return switch (gang) {
            case GangPlayer.AOGIRI -> "You killed our own. You're not Aogiri, and you never will be. Get out of my sight before I finish it.";
            case GangPlayer.CLOWNS -> "*the grin stays; the eyes don't* You killed my Clowns. You don't get a mask. You get a head start. Run.";
            case GangPlayer.GAS_MASKS -> "You killed my boys. *His voice shakes, and he doesn't back down.* You're not a Gas Mask. You never were. Get out "
                + "before they see you.";
            case GangPlayer.ANTEIKU -> "*He doesn't look up from his book.* ...I can't have you here anymore. I'm sorry. Please go.";
            default -> "You killed investigators. My investigators. *He doesn't raise his voice.* Get out of my building while you can still walk.";
        };
    }

    /** One of {@code theirs} asking {@code gang}'s leader to join. 4.1.79: Roma's for every gang (Anteiku's and the CCG's were the Gas Masks'). */
    static String otherGangLine(int gang, int theirs) {
        String title = GangPlayer.title(theirs);
        return switch (gang) {
            case GangPlayer.AOGIRI -> "You run with " + title + ". Aogiri doesn't share. Get lost.";
            case GangPlayer.CLOWNS -> theirs == GangPlayer.AOGIRI ? "Aogiri? Ew. Trees. The ringmaster doesn't do double acts. Ditch them first."
                : theirs == GangPlayer.GAS_MASKS ? "The Gas Masks? The heavy breathers? Ugh. The ringmaster doesn't do double acts. Ditch them first."
                : theirs == GangPlayer.ANTEIKU ? "Anteiku? The coffee people? Cute. The ringmaster doesn't do double acts. Ditch them first."
                : "A DOVE? In my circus? *She laughs until she sees you're serious.* ...Ditch the bureau first. Then we'll talk.";
            case GangPlayer.GAS_MASKS -> "You're with " + title + ". I can't take you. I'd be stealing, and they'd come for the boys.";
            case GangPlayer.ANTEIKU -> "You're with " + title + ". *He shakes his head, sorry about it.* If I took you, they'd follow you to the café's "
                + "door. I can't do that to everyone here.";
            default -> "You run with " + title + ". The bureau doesn't take double agents. Leave them, then we'll talk.";
        };
    }

    /** 4.1.71: true if {@code player} is one of the CCG: every ghoul but the named ones goes for them (GhoulNpcBrain). */
    public static boolean hunted(ServerPlayer player) {
        return of(player).member(GangPlayer.CCG);
    }

    /**
     * 4.1.71 (the owner: "CCG no longer attacks the player."): what the CCG's investigators are to {@code player} ({@link AgentBrain}): their
     * own ({@link #OWN_GANG}: never gone for, even struck, unless {@link #turned}), an {@link #OUTCAST} (it banned them: on sight), or a
     * {@link #STRANGER}.
     */
    static int ccgStance(ServerPlayer player) {
        GangPlayer g = of(player);
        return g.member(GangPlayer.CCG) ? OWN_GANG : g.banned(GangPlayer.CCG) ? OUTCAST : STRANGER;
    }

    /**
     * 4.1.70 (the owner, Anteiku's traits: "All ghouls and factions are neutral to the player but the CCG."): true if {@code player} is one
     * of Anteiku (GhoulNpcBrain: no ghoul or gang goes for them unless they start it; the CCG's still do).
     */
    public static boolean neutral(ServerPlayer player) {
        return of(player).member(GangPlayer.ANTEIKU);
    }

    /**
     * 4.1.70 (the owner: "Enegy bar increases size and increases maximum capacity."): what being one of Anteiku adds to how many times the
     * energy {@code player}'s bar holds ({@link SharedBar#capacity}): {@link GangPlayer#ANTEIKU_CAPACITY}, or 0.
     */
    public static int capacity(ServerPlayer player) {
        return of(player).member(GangPlayer.ANTEIKU) ? GangPlayer.ANTEIKU_CAPACITY : 0;
    }

    // ---------------------------------------------------------------- 4.1.67: quitting; a marriage to another gang's leader; their demands

    /** 4.1.67: what a member may say to their leader to leave (the owner: "Add a new Quit the gang button to each leader"). */
    static List<String[]> wayOut(int gang) {
        return List.<String[]>of(new String[] {com.hex.kagune.control.npc.GangWords.wantOut(gang), "gang:quit"});
    }

    /**
     * 4.1.67: a member asks their leader out ("gang:quit": asked to be sure), goes ("gang:quit:yes": out, not banned; its way in begins again),
     * or thinks better of it ("gang:stay"). Each leader in their own voice ({@code GangWords}).
     */
    static Said quit(ServerPlayer player, GangPlayer g, int gang, String key, int seed) {
        if (key.equals("gang:quit")) {
            return new Said(com.hex.kagune.control.npc.GangWords.asked(gang), List.of(new String[] {"I'm out. I quit.", "gang:quit:yes"},
                new String[] {"No. I'm staying.", "gang:stay"}));
        }
        if (key.equals("gang:stay")) {
            return new Said(com.hex.kagune.control.npc.GangWords.stay(gang), null);
        }
        set(player, g.quit());
        UkakuFire.sound(player.m_284548_(), player.m_20182_(), "block.candle.extinguish", 1.0F, 0.9F);
        player.m_5661_(Component.m_237113_("§7You left " + GangPlayer.title(gang) + ". They won't come for you, and they won't fight for you anymore."), false);
        return new Said(com.hex.kagune.control.npc.GangWords.gone(gang), null);
    }

    /**
     * 4.1.67 (the owner: "kicked from the gang if the player marries another gang leader, but can still join if the player was already married
     * before joining."): {@code player} has just married {@code spouse}; one of another gang, they are out of it (not banned). Joining a gang
     * when already married is never stopped.
     */
    static void married(ServerPlayer player, Persona spouse) {
        GangPlayer g = of(player);
        int theirs = gangOf(spouse);
        if (!leads(spouse) || g.gang() == GangPlayer.NONE || g.gang() == theirs) {
            return;
        }
        int was = g.gang();
        set(player, g.quit());
        player.m_5661_(Component.m_237113_(colour(was) + cap(GangPlayer.title(was)) + "§7: married to the leader of " + GangPlayer.title(theirs)
            + "? You're out. §8(Not banned: you can come back by its way in.)"), false);
    }

    /**
     * 4.1.67 (the owner: "being married to a gang leader but being part of another gang generates different reactions from the spouse,
     * constantly demanding they quit the gang."): what {@code spouse} says to {@code player} about it (two times in three, by {@code seed}),
     * or null when they are not that spouse or there is nothing to say.
     */
    static String spouseDemand(ServerPlayer player, Persona spouse, int seed) {
        GangPlayer g = of(player);
        if (!leads(spouse) || g.gang() == GangPlayer.NONE || g.gang() == gangOf(spouse) || !com.hex.kagune.control.npc.GangWords.demands(seed)
            || UniqueNpcs.stage(player.m_284548_(), spouse.key(), player) != com.hex.kagune.control.npc.Romance.Stage.MARRIED) {
            return null;
        }
        return com.hex.kagune.control.npc.GangWords.demand(gangOf(spouse), g.gang(), seed / 3);
    }

    /** 4.1.67: when a leader's spouse in another gang was last told to quit it by them, near (game time). */
    private static final java.util.Map<UUID, Long> NAGGED = WorldTimes.map();
    /** 4.1.67: at most this often (ticks), while they are near each other. */
    static final long NAG_EVERY = 20L * 150L;

    /** 4.1.67: near their spouse who leads another gang, every so often the spouse says it again (in chat, by name). */
    static void nag(ServerPlayer player, GangPlayer g, long now) {
        Long last = NAGGED.get(player.m_20148_());
        if (g.gang() == GangPlayer.NONE || last != null && now - last < NAG_EVERY) {
            return;
        }
        for (com.hex.kagune.control.npc.EtoNpc npc : player.m_284548_().m_6443_(com.hex.kagune.control.npc.EtoNpc.class, player.m_20191_().m_82400_(12.0),
            body -> body.m_6084_() && body.mind != null)) {
            Persona spouse = npc.persona();
            if (!leads(spouse) || gangOf(spouse) == g.gang()
                || UniqueNpcs.stage(player.m_284548_(), spouse.key(), player) != com.hex.kagune.control.npc.Romance.Stage.MARRIED) {
                continue;
            }
            NAGGED.put(player.m_20148_(), now);
            player.m_5661_(Component.m_237113_(spouse.colour() + npc.displayName() + "§f: " + com.hex.kagune.control.npc.GangWords.demand(gangOf(spouse), g.gang(), (int)(now / 20L))), false);
            return;
        }
    }

    /** 4.1.54: what may be said to Banjou; the third takes him seriously. */
    static final String[] ANSWERS = {"You? The boss? Of a gang? Seriously?", "Because you're the strongest ghoul in the ward. Everyone says so.",
        "Because they follow you. Not for your strength. Because you stay."};
    static final int SERIOUS = 2;

    /** 4.1.79: true if what is on {@code player}'s head is one of the Clowns' masks (any of their five): the job, in her face. */
    static boolean clownMaskOn(ServerPlayer player) {
        net.minecraft.world.item.ItemStack head = player.m_6844_(net.minecraft.world.entity.EquipmentSlot.HEAD);
        return head.m_41720_() instanceof com.hex.kagune.control.outfit.OutfitItem item && item.piece().id().startsWith("mask_clown");
    }

    /** The jokes told to Roma; the second is the one that lands (her humour). */
    static final String[] JOKES = {"Why did the scarecrow win an award? He was outstanding in his field.",
        "What's the best part of a dove? The briefcase. It's a lunchbox.", "Why don't ghouls eat clowns? They taste funny."};
    static final int FUNNY = 1;

    private static void joined(ServerPlayer player, int gang) {
        standing(player, GangStanding.joined(today(player))); // 4.1.52: the bar starts at half
        if (gang != GangPlayer.CLOWNS || !kept(player).m_128471_(JoinServer.LENT)) {
            giveMask(player, gang); // 4.1.53
        } else {
            kept(player).m_128405_(MASK, gang); // 4.1.79: Roma lent hers for the job: it is theirs now
            kept(player).m_128473_(JoinServer.LENT); // (a way in begun again some day lends another)
        }
        UkakuFire.sound(player.m_284548_(), player.m_20182_(), "entity.player.levelup", 0.8F, 0.7F);
        player.m_5661_(Component.m_237113_(colour(gang) + "You joined " + GangPlayer.title(gang) + (gang == GangPlayer.ANTEIKU
            ? ". §7Ghouls leave you be now, every gang too, unless you start it. The CCG won't. Your RC goes further." // 4.1.70
            : gang == GangPlayer.CCG ? ". §7Investigators never go for you now. Every ghoul but the ones you know by name will, and you hit "
                + "ghouls harder." // 4.1.71
            : ". §7They will never go for you again, and they will fight for you.")), false);
    }

    private static String pick(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
