package com.hex.kagune.control.server;

import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CGangName;
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
 * talked through with Ayato (Aogiri: "Kill 5 CCG") and Roma (the Clowns: a joke, three Gas Masks, a quinque case) ({@link #talk}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class GangServer {
    private static final String GANG = "HexGang";
    private static final String OWN = "HexGangOwnKills";
    private static final String BANNED = "HexGangBanned";
    private static final String AOGIRI_QUEST = "HexGangAogiriQuest";
    private static final String CCG = "HexGangCcgKills";
    private static final String CLOWN_STEP = "HexGangClownStep";
    private static final String GAS = "HexGangGasKills";
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

    /** {@code player}'s gang and where they stand with each. */
    public static GangPlayer of(ServerPlayer player) {
        CompoundTag k = kept(player);
        return new GangPlayer(k.m_128451_(GANG), k.m_128451_(OWN), k.m_128451_(BANNED), k.m_128471_(AOGIRI_QUEST), k.m_128451_(CCG),
            k.m_128451_(CLOWN_STEP), k.m_128451_(GAS));
    }

    /** {@code player}'s gang kept as {@code g}; a change of gang shown on their name everywhere. */
    static void set(ServerPlayer player, GangPlayer g) {
        int was = of(player).gang();
        CompoundTag k = kept(player);
        k.m_128405_(GANG, g.gang());
        k.m_128405_(OWN, g.ownKills());
        k.m_128405_(BANNED, g.banned());
        k.m_128379_(AOGIRI_QUEST, g.aogiriQuest());
        k.m_128405_(CCG, g.ccgKills());
        k.m_128405_(CLOWN_STEP, g.clownStep());
        k.m_128405_(GAS, g.gasKills());
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
            if (AgentBrain.ccg(dead)) {
                now = now.killedDove();
            }
            if (dead instanceof GhoulNpc ghoul && ghoul.identity() != null) {
                NpcIdentity who = ghoul.identity();
                if (who.gangOf() == com.hex.kagune.control.npc.Gang.GAS_MASKS) {
                    now = now.killedGasMask();
                }
                if (was.member(GangPlayer.of(who))) {
                    now = now.killedOwn();
                }
            }
            // 4.1.52: a member's reputation: up for the CCG and for other gangs killed, down a lot for one of their own.
            int by = 0;
            if (was.gang() != GangPlayer.NONE) {
                NpcIdentity who = dead instanceof GhoulNpc ghoul ? ghoul.identity() : null;
                if (AgentBrain.ccg(dead) || who != null && who.ccg()) {
                    by = GangStanding.KILL_CCG;
                } else if (who != null && (who.aogiri() || who.gang() != 0) && !was.member(GangPlayer.of(who))) {
                    by = GangStanding.KILL_RIVAL;
                } else if (who != null && was.member(GangPlayer.of(who))) {
                    by = -GangStanding.KILL_OWN;
                }
            }
            // 4.1.53: today's daily quest's kills.
            if (was.gang() != GangPlayer.NONE) {
                NpcIdentity who = dead instanceof GhoulNpc ghoul ? ghoul.identity() : null;
                boolean ccg = AgentBrain.ccg(dead) || who != null && who.ccg();
                boolean gas = who != null && who.gangOf() == com.hex.kagune.control.npc.Gang.GAS_MASKS;
                boolean rival = who != null && !ccg && (who.aogiri() || who.gang() != 0) && !was.member(GangPlayer.of(who));
                dailyKill(player, ccg, rival, gas);
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
            if (now.ccgKills() != was.ccgKills()) {
                player.m_5661_(Component.m_237113_("§4Aogiri Tree: §f" + Math.min(now.ccgKills(), GangPlayer.AOGIRI_CCG) + "/" + GangPlayer.AOGIRI_CCG
                    + " CCG" + (now.aogiriDone() ? " §7(go back to Ayato)" : "")), true);
            }
            if (now.gasKills() != was.gasKills()) {
                player.m_5661_(Component.m_237113_("§9The Clowns: §f" + Math.min(now.gasKills(), GangPlayer.CLOWN_GAS) + "/" + GangPlayer.CLOWN_GAS
                    + " Gas Masks" + (now.clownStep() == GangPlayer.CLOWN_CASE ? " §7(now a quinque case, for Roma)" : "")), true);
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

    /** True if {@code player} is in their gang's hideout: Aogiri's mansion, the Clowns' tent (their claimed spots, in the overworld). */
    static boolean inHideout(ServerPlayer player, int gang) {
        if (gang != GangPlayer.AOGIRI && gang != GangPlayer.CLOWNS || player.m_9236_().m_46472_() != net.minecraft.world.level.Level.f_46428_) {
            return false;
        }
        int[] chunk = com.hex.kagune.control.PlaceClaims.claimed(gang == GangPlayer.AOGIRI ? "aogiri_hideout" : "circus");
        if (chunk == null) {
            return false;
        }
        double reach = gang == GangPlayer.AOGIRI ? AOGIRI_HIDEOUT : CLOWN_HIDEOUT;
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
                return;
            }
            long now = player.m_9236_().m_46467_();
            vanish(player, now); // 4.1.53
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

    // ---------------------------------------------------------------- 4.1.53: daily quests

    private static final String DAILY_KIND = "HexDailyKind";
    private static final String DAILY_GOAL = "HexDailyGoal";
    private static final String DAILY_COUNT = "HexDailyCount";
    private static final String DAILY_DAY = "HexDailyDay";
    private static final String DAILY_PAID = "HexDailyPaid";

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

    /** 4.1.53: the gang whose daily quests {@code persona} gives (the owner: Noro for Aogiri; Roma, the Clowns' leader), or none. */
    static int gives(Persona persona) {
        return persona == Persona.NORO ? GangPlayer.AOGIRI : persona == Persona.ROMA ? GangPlayer.CLOWNS : GangPlayer.NONE;
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
        return gang == GangPlayer.AOGIRI ? "Any work for me today?" : "Got a job for me, boss?";
    }

    /** One of today's kills counted for {@code player}'s daily quest (an investigator, one of another gang, a Gas Mask). */
    static void dailyKill(ServerPlayer player, boolean ccg, boolean rival, boolean gas) {
        GangDaily d = daily(player);
        if (!d.today(today(player)) || d.claimed() || d.done() || d.job().brought) {
            return;
        }
        GangDaily.Job job = d.job();
        boolean counts = switch (job) {
            case AOGIRI_CCG, CLOWNS_CCG -> ccg;
            case AOGIRI_RIVALS -> rival;
            case CLOWNS_GAS -> gas;
            default -> false;
        };
        if (!counts) {
            return;
        }
        GangDaily after = d.counted();
        daily(player, after);
        String giver = job.gang == GangPlayer.AOGIRI ? "Noro" : "Roma";
        player.m_5661_(Component.m_237113_(colour(job.gang) + "Daily: §f" + job.text(after.goal()) + " §7(" + after.count() + "/" + after.goal() + ")"
            + (after.done() ? " §a- done, go back to " + giver : "")), true);
    }

    /**
     * Asking {@code persona} for today's job: one given if there is none today (one a day), how it goes, the things it asks for handed
     * over from the hand, or, done, paid (its items and {@link com.hex.kagune.control.npc.GangStanding#DAILY} reputation).
     */
    static String daily(ServerPlayer player, Persona persona, int seed) {
        int gang = gives(persona);
        if (gang == GangPlayer.NONE || !of(player).member(gang)) {
            return persona == Persona.NORO ? "..." : "Jobs are for Clowns, sweetheart. Join first.";
        }
        boolean noro = gang == GangPlayer.AOGIRI;
        long today = today(player);
        GangDaily d = daily(player);
        if (!d.today(today)) {
            GangDaily fresh = GangDaily.give(gang, today, player.m_20148_().getLeastSignificantBits());
            daily(player, fresh);
            player.m_5661_(Component.m_237113_(colour(gang) + "Daily: §f" + fresh.job().text(fresh.goal()) + " §7(0/" + fresh.goal() + ")"), false);
            String what = fresh.job().text(fresh.goal()).toLowerCase(java.util.Locale.ROOT);
            return noro ? pick(seed, "*tilts his head* ...Work. " + capFirst(what) + ". Today.", "Aogiri needs this done. " + capFirst(what) + ".",
                "...You came. Good. " + capFirst(what) + ". Then come back.")
                : pick(seed, "A job! For you! " + capFirst(what) + ". Make it funny.", "Today's act: " + what + ". Break a leg. Theirs, preferably.",
                    "*claps* Ooh, homework! " + capFirst(what) + ". Chop chop.");
        }
        if (d.claimed()) {
            return noro ? pick(seed, "...Done for today. Rest.", "Tomorrow.") : pick(seed, "You already did today's act! Encores are tomorrow.",
                "Shoo. Come back tomorrow, I'll have something awful.");
        }
        if (!d.done() && d.job().brought) {
            net.minecraft.world.item.ItemStack held = player.m_21205_();
            net.minecraft.world.item.Item wanted = com.hex.kagune.control.ControlItems.vanilla(d.job() == GangDaily.Job.AOGIRI_MEAT ? GangDaily.MEAT
                : "minecraft:allium");
            int have = wanted != null && !held.m_41619_() && held.m_41720_() == wanted ? held.m_41613_() : 0;
            GangDaily got = d.brought(have);
            if (got.done()) {
                if (!player.m_7500_()) {
                    held.m_41774_(d.goal());
                }
                d = got;
            } else {
                return noro ? "...Not enough. " + d.goal() + ". In your hand." : "I said " + d.goal() + "! In your hand, where I can see them. I count everything.";
            }
        }
        if (!d.done()) {
            return noro ? d.count() + " of " + d.goal() + ". ...Keep going." : d.count() + " of " + d.goal() + "! The suspense is killing me. Not literally. Yet.";
        }
        daily(player, d.paid());
        StringBuilder paid = new StringBuilder();
        for (GangDaily.Pay pay : d.pay(player.m_20148_().getLeastSignificantBits())) {
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
        reputation(player, com.hex.kagune.control.npc.GangStanding.DAILY);
        player.m_5661_(Component.m_237113_(colour(gang) + "Daily done: §f" + paid + " §7(+" + com.hex.kagune.control.npc.GangStanding.DAILY
            + "% reputation)"), false);
        return noro ? pick(seed, "...Good. *hands it over without a word*", "Aogiri remembers this. Take it.")
            : pick(seed, "*throws confetti made of something* Bravo! Take your prize!", "What a show! Here, a little something. Don't eat the confetti.");
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
        String id = gang == GangPlayer.AOGIRI ? "hexkagune:mask_bin_brothers" : gang == GangPlayer.CLOWNS ? GangDaily.CLOWN_MASK : null;
        net.minecraft.world.item.Item item = id == null ? null : com.hex.kagune.control.ControlItems.vanilla(id);
        kept(player).m_128405_(MASK, gang);
        if (item == null) {
            return;
        }
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item, 1);
        if (!player.m_36356_(stack)) {
            player.m_36176_(stack, false);
        }
        player.m_5661_(Component.m_237113_(colour(gang) + cap(GangPlayer.title(gang)) + ": §fyour mask. §7Wear it with pride (on your head)."), false);
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
            }
            if (hurt instanceof ServerPlayer player && source instanceof LivingEntity other && source != hurt) {
                boost(player, other);
            }
        } catch (RuntimeException | LinkageError failure) {
            Safety.caught(failure, "GangServer.fought");
        }
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

    static String colour(int gang) {
        return gang == GangPlayer.AOGIRI ? "§4" : gang == GangPlayer.CLOWNS ? "§9" : "§7";
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
                event.setDisplayname(Component.m_237113_(GangPlayer.named(gang, event.getDisplayname().getString())));
            }
        } catch (RuntimeException | LinkageError failure) {
            Safety.caught(failure, "GangServer.name");
        }
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

    /** True if {@code persona} is the way into a gang: Ayato (Aogiri), Roma (the Clowns). */
    static boolean leads(Persona persona) {
        return persona == Persona.AYATO || persona == Persona.ROMA;
    }

    /** The gang {@code persona} is the way into. */
    static int gangOf(Persona persona) {
        return persona == Persona.AYATO ? GangPlayer.AOGIRI : persona == Persona.ROMA ? GangPlayer.CLOWNS : GangPlayer.NONE;
    }

    /** 4.1.52: the gang {@code persona} belongs to (Ayato and Noro Aogiri's, Roma the Clowns'), or none. */
    static int memberOf(Persona persona) {
        return persona == Persona.AYATO || persona == Persona.NORO ? GangPlayer.AOGIRI : persona == Persona.ROMA ? GangPlayer.CLOWNS : GangPlayer.NONE;
    }

    /** What asking {@code persona} about their gang is called on the talk screen, by where {@code player} stands. */
    static String ask(ServerPlayer player, Persona persona) {
        GangPlayer g = of(player);
        int gang = gangOf(persona);
        if (g.member(gang)) {
            return gang == GangPlayer.AOGIRI ? "How's Aogiri?" : "How are the Clowns?";
        }
        if (gang == GangPlayer.AOGIRI) {
            return g.aogiriDone() ? "It's done. Five doves." : g.aogiriQuest() ? "About joining Aogiri..." : "I want to join Aogiri.";
        }
        if (g.clownStep() == GangPlayer.CLOWN_CASE && holdsCase(player)) {
            return "Here. A dove's quinque case. (the case in your hand)";
        }
        return g.clownStep() != GangPlayer.CLOWN_NOT_YET ? "About joining the Clowns..." : "I want to join the Clowns.";
    }

    /** The talk about {@code persona}'s gang ({@code key}: "gang", or "gang:joke:n" a joke told): what is said, what it does. */
    static Said talk(ServerPlayer player, Persona persona, String key, int seed) {
        GangPlayer g = of(player);
        int gang = gangOf(persona);
        boolean ayato = gang == GangPlayer.AOGIRI;
        if (g.member(gang)) {
            return new Said(ayato ? pick(seed, "Aogiri's fine. Don't get sloppy.", "You're one of us. Act like it.", "Still breathing? Good. Aogiri needs you.")
                : pick(seed, "My favourite Clown! Go scare someone.", "You wear our face well. ...Kidding, you don't have one. You have mine. In your heart.",
                    "The Clowns are great! Three of them are on fire, but great."), null);
        }
        if (g.banned(gang)) {
            return new Said(ayato ? "You killed our own. You're not Aogiri, and you never will be. Get out of my sight before I finish it."
                : "*the grin stays; the eyes don't* You killed my Clowns. You don't get a mask. You get a head start. Run.", null);
        }
        if (g.gang() != GangPlayer.NONE) {
            return new Said(ayato ? "You run with the Clowns. Aogiri doesn't share. Get lost."
                : "Aogiri? Ew. Trees. The ringmaster doesn't do double acts. Ditch them first.", null);
        }
        if (ayato) {
            if (g.aogiriDone()) {
                set(player, g.join(GangPlayer.AOGIRI));
                joined(player, GangPlayer.AOGIRI);
                return new Said("Five doves. ...Fine. You're Aogiri now. Don't make me regret it. And if you ever turn on us, I'll kill you myself.", null);
            }
            if (g.aogiriQuest()) {
                return new Said(g.ccgKills() + " of five. The doves aren't going to kill themselves. Move.", null);
            }
            set(player, g.startAogiri());
            player.m_5661_(Component.m_237113_("§4Aogiri Tree: §fkill " + GangPlayer.AOGIRI_CCG + " CCG §7(0/" + GangPlayer.AOGIRI_CCG + ")"), false);
            return new Said("Join Aogiri? ...You? Tch. Fine. Prove it. Kill five doves. CCG. I'll know if you lie.", null);
        }
        // Roma's chain: a joke that makes her laugh, three Gas Masks, a quinque case.
        if (g.clownStep() == GangPlayer.CLOWN_NOT_YET) {
            if (key.startsWith("gang:joke:")) {
                int told = Integer.parseInt(key.substring("gang:joke:".length()));
                if (told == FUNNY) {
                    set(player, g.laughed());
                    player.m_5661_(Component.m_237113_("§9The Clowns: §fkill " + GangPlayer.CLOWN_GAS + " Gas Masks §7(0/" + GangPlayer.CLOWN_GAS + ")"),
                        false);
                    return new Said("*bursts out laughing so hard she has to hold onto you* HA! The BRIEFCASE! ...Okay. Okay. You're funny. Next act: three "
                        + "Gas Masks. Kill them. They're no fun at all.", null);
                }
                return new Said(told == 0 ? "*stares at you* ...That's a dad joke. I'm not your dad. Try again some other time."
                    : "Hey! ...That one's about ME. ...It's a little funny. But no. Come back with better material.", null);
            }
            List<String[]> jokes = new ArrayList<>(List.of(new String[] {JOKES[0], "gang:joke:0"}, new String[] {JOKES[1], "gang:joke:1"},
                new String[] {JOKES[2], "gang:joke:2"}));
            java.util.Collections.rotate(jokes, Math.floorMod(seed, jokes.size()));
            return new Said("Join the Clowns? Make me laugh first! Go on. Tell me a joke. A good one. I'll know.", jokes);
        }
        if (g.clownStep() == GangPlayer.CLOWN_GAS_MASKS) {
            return new Said(g.gasKills() + " of three Gas Masks! Chop chop. They're the ones breathing heavily at everyone.", null);
        }
        if (!takeCase(player)) {
            return new Said("Now bring me an investigator's quinque case. The briefcase! I want to put flowers in it.", null);
        }
        set(player, g.join(GangPlayer.CLOWNS));
        joined(player, GangPlayer.CLOWNS);
        return new Said("*opens the case, drops an allium in it, and cackles* A dove's briefcase! Welcome to the Clowns, sweetheart. Nobody touches you now. "
            + "Except me.", null);
    }

    /** The jokes told to Roma; the second is the one that lands (her humour). */
    static final String[] JOKES = {"Why did the scarecrow win an award? He was outstanding in his field.",
        "What's the best part of a dove? The briefcase. It's a lunchbox.", "Why don't ghouls eat clowns? They taste funny."};
    static final int FUNNY = 1;

    private static void joined(ServerPlayer player, int gang) {
        standing(player, GangStanding.joined(today(player))); // 4.1.52: the bar starts at half
        giveMask(player, gang); // 4.1.53
        UkakuFire.sound(player.m_284548_(), player.m_20182_(), "entity.player.levelup", 0.8F, 0.7F);
        player.m_5661_(Component.m_237113_((gang == GangPlayer.AOGIRI ? "§4" : "§9") + "You joined " + GangPlayer.title(gang)
            + ". §7They will never go for you again, and they will fight for you."), false);
    }

    private static String pick(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
