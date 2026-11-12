package com.hex.kagune.control.server;

import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CGangName;
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
        UkakuFire.sound(player.m_284548_(), player.m_20182_(), "entity.player.levelup", 0.8F, 0.7F);
        player.m_5661_(Component.m_237113_((gang == GangPlayer.AOGIRI ? "§4" : "§9") + "You joined " + GangPlayer.title(gang)
            + ". §7They will never go for you again, and they will fight for you."), false);
    }

    private static String pick(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
