package com.hex.kagune.control;

import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.graft.GraftStage;
import com.hex.kagune.control.npc.NpcBonds;
import com.hex.kagune.control.npc.NpcIdentity;
import com.hex.kagune.control.npc.NpcLook;
import com.hex.kagune.control.npc.NpcNames;
import com.hex.kagune.control.npc.NpcRules;
import com.hex.kagune.control.npc.NpcTalk;
import com.hex.kagune.control.npc.NpcTemper;
import com.hex.kagune.control.quest.Favours;
import com.hex.kagune.control.quest.Quest;
import com.hex.kagune.control.quest.QuestBook;
import com.hex.kagune.control.quest.QuestEvent;
import com.hex.kagune.control.quest.QuestLog;
import com.hex.kagune.control.quest.Signal;
import com.hex.kagune.control.quest.Tags;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** 7.0.2: the ghoul NPCs: who they are, who they go for, when and where they turn up, how hard they hit. */
public final class NpcTests {
    static int passed;
    static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    public static void main(String[] args) {
        village();
        lives();
        romance214();
        approach226();
        errands247();
        identity();
        blades();
        bubbles();
        tempers();
        spawning();
        strength();
        look();
        moves();
        bonds();
        favours();
        talk();
        attraction();
        mansion();
        dryLand();
        ccgBuilding();
        onePerWorld();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Ghoul NPC tests: " + passed + " checks passed");
    }

    /** 1.0.41: its flips, heavies and ways of fighting: the stronger the quicker, a flip never slower or faster than one goes. */
    static void moves() {
        for (int tier = 0; tier + 1 < NpcIdentity.TIERS; tier++) {
            check(NpcRules.flipEvery(tier + 1) < NpcRules.flipEvery(tier) && NpcRules.heavyEvery(tier + 1) < NpcRules.heavyEvery(tier)
                && NpcRules.counter(tier + 1) > NpcRules.counter(tier) && NpcRules.dodge(tier + 1) > NpcRules.dodge(tier), "the stronger, the quicker: " + tier);
        }
        for (int tier = 0; tier < NpcIdentity.TIERS; tier++) {
            check(NpcRules.flipEvery(tier) > NpcRules.FLIP_POSED && NpcRules.heavyEvery(tier) > NpcRules.flipEvery(tier)
                && NpcRules.counter(tier) < 1.0 && NpcRules.dodge(tier) < 1.0, "it is not flipping or heaving every moment: " + tier);
        }
        for (double gap = -2.0; gap < 40.0; gap += 0.25) {
            double speed = NpcRules.flipSpeed(gap);
            check(speed >= NpcRules.FLIP_FORWARD_LEAST && speed <= NpcRules.FLIP_FORWARD_MOST, "a front flip's speed for " + gap + ": " + speed);
        }
        check(NpcRules.flipSpeed(4.0) < NpcRules.flipSpeed(8.0), "a front flip carries it farther the farther off its prey");
        int lancers = 0;
        int snipers = 0;
        for (int seed = 0; seed < 0x10000; seed++) {
            lancers += NpcRules.lancer(seed) ? 1 : 0;
            snipers += NpcRules.sniper(seed) ? 1 : 0;
        }
        check(lancers > 0x10000 / 3 && lancers < 0x10000 * 2 / 3, "about half the rinkaku fight from their tails' full length: " + lancers);
        check(snipers > 0x10000 / 4 && snipers < 0x10000 / 2, "about a third of the ukaku keep far off: " + snipers);
        check(NpcRules.SNIPE_NEAREST < NpcRules.SNIPE_FARTHEST && NpcRules.SNIPE_FARTHEST < NpcRules.SNIPE_RANGE
            && NpcRules.CROWDED < NpcRules.CLOSE_IN && NpcRules.CLOSE_IN < NpcRules.CLOSE_IN_MOST, "the ranges it keeps are in order");
    }

    /** Who one is survives the round trip through its saved number, whatever it is. */
    static void identity() {
        for (NpcIdentity.Kind kind : NpcIdentity.Kind.values()) {
            for (NpcTemper temper : NpcTemper.values()) {
                for (int tier = 0; tier < NpcIdentity.TIERS; tier++) {
                    for (int skin = 0; skin < NpcLook.SKINS.length; skin++) {
                        NpcIdentity who = new NpcIdentity(kind, temper, tier, skin, (skin * 7 + tier) % NpcNames.count(), skin * 9001 + tier * 77);
                        check(NpcIdentity.unpack(who.pack()).equals(who), "round trip: " + who);
                    }
                }
            }
        }
        // 1.0.41: one saved before its own skins (no flag): given one of them, the same every time, in range.
        long old = 1L | 2L << 1 | 1L << 4 | 3L << 6 | 17L << 10 | 4242L << 18;
        NpcIdentity before = NpcIdentity.unpack(old);
        check(before.skin() >= 0 && before.skin() < NpcLook.SKINS.length && before.equals(NpcIdentity.unpack(old)) && before.name() == 17
            && before.seed() == 4242 && NpcIdentity.unpack(before.pack()).equals(before), "an old save's ghoul gets one of the new skins, for good");
        NpcIdentity wild = new NpcIdentity(NpcIdentity.Kind.UKAKU, NpcTemper.ANTEIKU, 9, -1, 999, 0x7FFFFFFF);
        check(wild.tier() == NpcIdentity.TIERS - 1 && wild.skin() == NpcLook.SKINS.length - 1 && wild.name() < NpcNames.count() && wild.seed() <= 0xFFFF,
            "numbers out of range are brought in: " + wild);
        Random random = new Random(7);
        Map<NpcTemper, Integer> seen = new EnumMap<>(NpcTemper.class);
        int ukaku = 0;
        int blades = 0;
        for (int i = 0; i < 20000; i++) {
            NpcIdentity who = NpcIdentity.roll(500, random::nextDouble);
            seen.merge(who.temper(), 1, Integer::sum);
            ukaku += who.kind() == NpcIdentity.Kind.UKAKU ? 1 : 0;
            blades += who.kind() == NpcIdentity.Kind.KOUKAKU ? 1 : 0;
            check(NpcIdentity.unpack(who.pack()).equals(who), "a rolled one round trips");
        }
        int total = 0;
        for (NpcTemper temper : NpcTemper.values()) {
            total += temper.weight();
        }
        for (NpcTemper temper : NpcTemper.values()) {
            double share = seen.getOrDefault(temper, 0) / 20000.0;
            check(Math.abs(share - temper.weight() / (double)total) < 0.02, temper + " turns up by its weight: " + share);
        }
        // 1.3.36: the arm blade (an NPC-only koukaku) a little over a quarter, ukaku and rinkaku about even in the rest.
        check(Math.abs(ukaku / 20000.0 - 0.35) < 0.02 && Math.abs(blades / 20000.0 - 0.28) < 0.02, "rinkaku, ukaku and the arm blade by their odds: "
            + ukaku + ", " + blades);
        check(NpcIdentity.Kind.values()[2] == NpcIdentity.Kind.KOUKAKU && NpcIdentity.Kind.byOrdinal(2) == NpcIdentity.Kind.KOUKAKU
            && NpcIdentity.Kind.byOrdinal(9) == NpcIdentity.Kind.RINKAKU, "the arm blade appended (sent by ordinal)");
        // A ghoul saved before 1.3.36 (bit 36 clear) reads as it was.
        long older = 1L | 2L << 1 | 1L << 4 | 3L << 6 | 17L << 10 | 4242L << 18 | 1L << 35;
        check(NpcIdentity.unpack(older).kind() == NpcIdentity.Kind.UKAKU && NpcIdentity.unpack(older & ~1L).kind() == NpcIdentity.Kind.RINKAKU,
            "an older save keeps its kagune");
        check(NpcTemper.pick(0.0) == NpcTemper.FERAL && NpcTemper.pick(0.999999) == NpcTemper.ANTEIKU && NpcTemper.pick(1.5) == NpcTemper.ANTEIKU,
            "the ends of the roll");
    }

    /** Who each temper goes for, leaves be, or will not touch first. */
    static void tempers() {
        NpcTemper.Attitude hostile = NpcTemper.Attitude.HOSTILE;
        NpcTemper.Attitude wary = NpcTemper.Attitude.WARY;
        NpcTemper.Attitude friendly = NpcTemper.Attitude.FRIENDLY;
        String[] sides = {null, "anteiku", "pack"};
        for (String side : sides) {
            for (boolean ghoul : new boolean[] {false, true}) {
                check(NpcTemper.FERAL.toward(ghoul, side) == hostile, "a feral goes for anyone");
                check(NpcTemper.DRIFTER.toward(ghoul, side) == wary && NpcTemper.SCAVENGER.toward(ghoul, side) == wary, "drifters and scavengers leave people be");
                check(NpcTemper.GOURMET.toward(ghoul, side) == (ghoul ? hostile : wary), "a gourmet goes for ghouls only");
            }
        }
        check(NpcTemper.PACK.toward(false, null) == hostile, "the Pack goes for humans");
        check(NpcTemper.PACK.toward(true, "anteiku") == hostile, "the Pack goes for Anteiku's ghouls");
        check(NpcTemper.PACK.toward(true, null) == wary, "the Pack sizes up a ghoul of no side");
        check(NpcTemper.PACK.toward(true, "pack") == friendly, "the Pack never touches its own");
        check(NpcTemper.ANTEIKU.toward(false, null) == friendly && NpcTemper.ANTEIKU.toward(true, "anteiku") == friendly, "Anteiku is friendly");
        check(NpcTemper.ANTEIKU.toward(true, "pack") == wary, "Anteiku is wary of the Pack");
        for (NpcTemper other : NpcTemper.values()) {
            check(NpcTemper.GOURMET.hunts(other) == (other != NpcTemper.GOURMET), "a gourmet eats every other kind: " + other);
            check(NpcTemper.ANTEIKU.hunts(other) == (other == NpcTemper.FERAL), "Anteiku only puts down ferals: " + other);
            check(!NpcTemper.DRIFTER.hunts(other) && !NpcTemper.SCAVENGER.hunts(other), "drifters and scavengers hunt no one: " + other);
            check(NpcTemper.PACK.hunts(other) == (other == NpcTemper.ANTEIKU), "the Pack hunts Anteiku: " + other);
        }
        check(NpcTemper.SCAVENGER.coward() && !NpcTemper.FERAL.coward(), "only the scavenger runs");
        check(NpcTemper.FERAL.hostile() && NpcTemper.GOURMET.hostile() && NpcTemper.PACK.hostile() && !NpcTemper.ANTEIKU.hostile()
            && !NpcTemper.DRIFTER.hostile() && !NpcTemper.SCAVENGER.hostile(), "which are hostile");
        check(NpcTemper.DRIFTER.grudge() > NpcTemper.SCAVENGER.grudge() && NpcTemper.FERAL.grudge() == Integer.MAX_VALUE, "grudges");
    }

    /** Only at night, a ring round the player, never near another. */
    static void spawning() {
        check(!NpcRules.night(0L) && !NpcRules.night(6000L) && !NpcRules.night(12999L), "not by day");
        check(NpcRules.night(13000L) && NpcRules.night(18000L) && NpcRules.night(22999L) && NpcRules.night(24000L * 5 + 18000L), "at night, any day");
        check(!NpcRules.night(23000L) && !NpcRules.night(23999L), "not at dawn");
        check(!NpcRules.spaced(NpcRules.ALONE - 1.0, 0), "never near another");
        check(NpcRules.spaced(NpcRules.ALONE + 1.0, 1), "one other far off is fine");
        check(!NpcRules.spaced(NpcRules.ALONE + 1.0, 2), "not a third in one stretch");
        check(NpcRules.spaced(Double.MAX_VALUE, 0), "none at all: fine");
        Random random = new Random(3);
        for (int i = 0; i < 2000; i++) {
            double[] at = NpcRules.around(random.nextDouble(), random.nextDouble());
            double distance = Math.hypot(at[0], at[1]);
            check(distance >= NpcRules.SPAWN_NEAREST - 1.0E-9 && distance <= NpcRules.SPAWN_FARTHEST + 1.0E-9, "a ring round the player: " + distance);
            check(distance > NpcRules.CLEAR_OF_PLAYERS, "never on top of them");
        }
        // 1.3.27: rare now: about one every two or three nights near someone out in it (a night's tries times the chance).
        double tries = (NpcRules.NIGHT_TO - NpcRules.NIGHT_FROM) / (double)NpcRules.SPAWN_EVERY;
        double expected = tries * NpcRules.SPAWN_CHANCE;
        check(expected >= 0.25 && expected <= 0.6, "rare: " + expected + " a night before spacing");
    }

    /**
     * 4.0.0: ranks D to SS. The stronger the player, the stronger the ghoul (an S or SS rare even then); each rank tougher, quicker and
     * keener; a D only its basic string; the string hurts but does not shred; an old save's three tiers are C, B and A.
     */
    static void strength() {
        Random random = new Random(11);
        for (int mastery : new int[] {0, 500, 1000}) {
            int[] ranks = new int[NpcIdentity.TIERS];
            for (int i = 0; i < 40000; i++) {
                ranks[NpcRules.tier(mastery, random.nextDouble())]++;
            }
            double top = (ranks[NpcRules.S] + ranks[NpcRules.SS]) / 40000.0;
            check(Math.abs(top - NpcRules.atLeast(mastery, NpcRules.S)) < 0.01, "S or SS at mastery " + mastery + ": " + top);
            check(top < 0.2, "an S or an SS stays rare: " + top);
            check(ranks[NpcRules.SS] < ranks[NpcRules.S], "SS rarer than S at mastery " + mastery);
            check(mastery < 1000 || ranks[NpcRules.D] == 0, "no D for the strongest");
            check(mastery > 0 || ranks[NpcRules.D] > ranks[NpcRules.A], "a beginner meets Ds more than As");
        }
        check(NpcRules.RANKS.length == NpcIdentity.TIERS && NpcRules.rankName(0).equals("D") && NpcRules.rankName(5).equals("SS"), "D to SS");
        for (int tier = 1; tier < NpcIdentity.TIERS; tier++) {
            check(NpcRules.health(tier) > NpcRules.health(tier - 1) && NpcRules.damage(tier) > NpcRules.damage(tier - 1)
                && NpcRules.armor(tier) > NpcRules.armor(tier - 1) && NpcRules.speed(tier) > NpcRules.speed(tier - 1), "rank " + tier + " is tougher");
            check(NpcRules.string(tier) > NpcRules.string(tier - 1), "rank " + tier + " strings longer");
            check(NpcRules.regenEvery(tier) < NpcRules.regenEvery(tier - 1) && NpcRules.sight(tier) > NpcRules.sight(tier - 1),
                "rank " + tier + " knits faster and sees further");
            check(NpcRules.pause(tier, 0.5) < NpcRules.pause(tier - 1, 0.5), "rank " + tier + " pauses less between strings");
        }
        check(NpcRules.health(NpcRules.SS) >= 6.0F * NpcRules.health(NpcRules.D), "an SS is a wall next to a D");
        check(!NpcRules.flips(NpcRules.D) && !NpcRules.dodges(NpcRules.D) && !NpcRules.counters(NpcRules.D) && !NpcRules.heavies(NpcRules.D),
            "a D has only its basic string");
        check(NpcRules.flips(NpcRules.C) && !NpcRules.heavies(NpcRules.C) && NpcRules.heavies(NpcRules.B) && NpcRules.counters(NpcRules.B),
            "flips from C, heavies and clashes from B");
        check(!NpcRules.frenzies(NpcRules.A) && NpcRules.frenzies(NpcRules.S) && NpcRules.frenzies(NpcRules.SS), "an S and an SS frenzy");
        check(NpcRules.health(0) > 20.0F, "a ghoul outlasts a player's health");
        for (int tier = 0; tier < NpcIdentity.TIERS; tier++) {
            GraftStage stage = NpcRules.graftStage(tier);
            int limbs = stage.limbs(NpcRules.tailMastery(tier));
            int want = tier <= NpcRules.C ? 1 : tier <= NpcRules.A ? 2 : tier == NpcRules.S ? 3 : 4;
            check(limbs == want, "rank " + tier + ": " + limbs + " tails");
            // One string and the pause after it: the damage a second it averages (before armour and difficulty).
            GraftMove move = GraftMove.firstStrike(stage);
            double power = 0.0;
            int ticks = 0;
            for (int i = 0; i < NpcRules.string(tier); i++) {
                for (GraftMove.Beat beat : move.beats(limbs)) {
                    power += beat.power();
                }
                ticks += move.ticks();
                move = GraftMove.nextStrike(move, stage);
            }
            ticks += NpcRules.pause(tier, 0.5);
            double perSecond = power * NpcRules.damage(tier) * 20.0 / ticks;
            check(perSecond >= 1.5 && perSecond <= 22.0, "rank " + tier + " lashes for " + perSecond + " a second");
            double shots = NpcRules.string(tier) * NpcRules.damage(tier) * 20.0
                / (NpcRules.string(tier) * NpcRules.SHOT_EVERY + 6 + NpcRules.pause(tier, 0.5));
            check(shots >= 1.0 && shots <= 14.0, "rank " + tier + " shoots for " + shots + " a second (and burns)");
            for (double r : new double[] {0.0, 0.49, 0.5, 0.99}) {
                int meat = NpcRules.meat(tier, r);
                check(meat >= 1 + tier / 2 && meat <= 2 + tier / 2, "rank " + tier + " leaves " + meat + " Ghoul Meat");
            }
        }
        check(NpcRules.wingMastery(NpcRules.SS) < 450, "the wings stay the plain twin wings (no crystal, no embers)");
        // An old save's ghoul (three tiers, no rank flag): its tier one up, no mask; and back the same.
        for (int old = 0; old < 3; old++) {
            long packed = 1L | 2L << 1 | (long)old << 4 | 3L << 6 | 17L << 10 | 4242L << 18 | 1L << 35;
            NpcIdentity before = NpcIdentity.unpack(packed);
            check(before.tier() == old + 1 && before.mask() == 0 && !before.aogiri(), "an old tier " + old + " is " + before.rank() + "-rank now");
            check(NpcIdentity.unpack(before.pack()).equals(before), "and stays so once saved again");
        }
        for (int tier = 0; tier < NpcIdentity.TIERS; tier++) {
            for (int mask = 0; mask <= com.hex.kagune.control.outfit.Outfits.GENERIC_MASKS.size(); mask++) {
                for (boolean aogiri : new boolean[] {false, true}) {
                    NpcIdentity who = new NpcIdentity(NpcIdentity.Kind.KOUKAKU, NpcTemper.PACK, tier, 5, 9, 1234, mask, aogiri);
                    check(NpcIdentity.unpack(who.pack()).equals(who), "rank, mask and faction saved and read back: " + who);
                }
            }
        }
        NpcIdentity masked = new NpcIdentity(NpcIdentity.Kind.UKAKU, NpcTemper.FERAL, NpcRules.S, 1, 2, 3, 2, false);
        check("mask_gas".equals(masked.maskItem()) && masked.tagName().endsWith("S-rank"), "a wild one's mask is generic: " + masked.maskItem());
        NpcIdentity member = masked.member(1, NpcRules.B);
        check(member.aogiri() && "mask_bin_brothers".equals(member.maskItem()) && member.tagName().contains("Aogiri"), "a member's mask is its faction's");
    }

    /**
     * 1.0.41: their own faces, twelve boys and twelve girls, every one's eyes where the kakugan goes (read off the very textures: the
     * white, the iris toward the nose, nothing of the second layer over them; and the game's own reader finds them there); names for
     * every temper, a boy's for a boy and a girl's for a girl.
     */
    static void look() {
        check(NpcLook.SKINS.length == 24 && NpcLook.BOYS == 12, "twelve boys, twelve girls");
        check(!NpcLook.girl(0) && !NpcLook.girl(11) && NpcLook.girl(12) && NpcLook.girl(23) && NpcLook.girl(-1), "the girls after the boys");
        check(NpcLook.texture(4).equals("textures/entity/ghoul/boy_05.png") && NpcLook.texture(-1).endsWith("girl_12.png"), "skin textures");
        java.io.File dir = new java.io.File("src/main/resources/assets/hexkagune/textures/entity/ghoul");
        for (int skin = 0; skin < NpcLook.SKINS.length; skin++) {
            com.hex.kagune.control.kakugan.KakuganEyes eyes = NpcLook.eyes(skin);
            check(eyes.validated().equals(eyes), NpcLook.SKINS[skin] + ": its eyes are valid as they are");
            check(eyes.height() == (NpcLook.girl(skin) ? 2 : 1), NpcLook.SKINS[skin] + ": a girl's eyes two rows tall, a boy's one");
            java.awt.image.BufferedImage image;
            try {
                image = javax.imageio.ImageIO.read(new java.io.File(dir, NpcLook.SKINS[skin] + ".png"));
            } catch (java.io.IOException failure) {
                image = null;
            }
            check(image != null && image.getWidth() == 64 && image.getHeight() == 64, NpcLook.SKINS[skin] + ": a 64 x 64 skin");
            if (image == null) {
                continue;
            }
            boolean patched = skin == NpcLook.PATCHED;
            for (int side = 0; side < 2; side++) {
                int x = side == 0 ? eyes.rightX() : eyes.leftX();
                int irisX = side == 0 ? x + 1 : x;
                int whiteX = side == 0 ? x : x + 1;
                boolean turns = eyes.ghoul() == com.hex.kagune.control.kakugan.KakuganEyes.BOTH
                    || eyes.ghoul() == (side == 0 ? com.hex.kagune.control.kakugan.KakuganEyes.RIGHT : com.hex.kagune.control.kakugan.KakuganEyes.LEFT);
                for (int row = 0; row < eyes.height(); row++) {
                    int y = (side == 0 ? eyes.rightY() : eyes.leftY()) + row;
                    int white = image.getRGB(8 + whiteX, 8 + y);
                    int iris = image.getRGB(8 + irisX, 8 + y);
                    check(brightness(white) > 200 && brightness(iris) < brightness(white) - 60, NpcLook.SKINS[skin] + ": an eye (white, iris to the nose) at "
                        + whiteX + "," + y);
                    boolean covered = (image.getRGB(40 + whiteX, 8 + y) >>> 24) > 0 || (image.getRGB(40 + irisX, 8 + y) >>> 24) > 0;
                    check(covered == (patched && !turns), NpcLook.SKINS[skin] + (turns ? ": nothing over the eye that turns" : ": the patch over the eye that does not"));
                }
            }
            if (!patched) {
                int[] pixels = new int[64 * 64];
                image.getRGB(0, 0, 64, 64, pixels, 0, 64);
                com.hex.kagune.control.kakugan.KakuganEyes found = com.hex.kagune.control.kakugan.KakuganSkin.detect(pixels, 64);
                check(found != null && found.rightX() == eyes.rightX() && found.leftX() == eyes.leftX() && found.rightY() == eyes.rightY(),
                    NpcLook.SKINS[skin] + ": the game's own reader finds the eyes where the kakugan goes (" + found + ")");
            }
        }
        check(NpcLook.eyes(NpcLook.PATCHED).ghoul() == com.hex.kagune.control.kakugan.KakuganEyes.RIGHT, "the boy in the eyepatch: only his right eye turns");
        for (NpcTemper temper : NpcTemper.values()) {
            for (boolean girl : new boolean[] {false, true}) {
                String name = NpcNames.name(3, temper, girl);
                check(name.startsWith(NpcNames.given(3, girl)) && name.length() > NpcNames.given(3, girl).length(), "named: " + name);
            }
        }
        check(NpcNames.name(20, NpcTemper.DRIFTER, false).equals("Ren the Drifter") && NpcNames.name(20, NpcTemper.ANTEIKU, false).equals("Ren of Anteiku")
            && NpcNames.name(11, NpcTemper.PACK, true).equals("Mio of the Pack"), "how the names read");
        java.util.Set<String> given = new java.util.HashSet<>();
        for (boolean girl : new boolean[] {false, true}) {
            for (int i = 0; i < NpcNames.count(); i++) {
                check(given.add(NpcNames.given(i, girl)), "no name twice: " + NpcNames.given(i, girl));
            }
        }
        check(NpcNames.count() <= 256, "names fit in the saved number");
        NpcIdentity boy = new NpcIdentity(NpcIdentity.Kind.RINKAKU, NpcTemper.DRIFTER, 0, 3, 20, 1);
        NpcIdentity girl = new NpcIdentity(NpcIdentity.Kind.RINKAKU, NpcTemper.DRIFTER, 0, 15, 11, 1);
        check(!boy.girl() && boy.displayName().equals("Ren the Drifter") && girl.girl() && girl.displayName().equals("Mio the Drifter")
            && girl.given().equals("Mio"), "a boy goes by a boy's name, a girl by a girl's");
    }

    private static int brightness(int argb) {
        return ((argb >> 16 & 255) + (argb >> 8 & 255) + (argb & 255)) / 3;
    }

    /** Regard: how it reads, what it does to a temper's attitude, a stray blow forgiven, a betrayal not. */
    static void bonds() {
        check(NpcBonds.band(-100) == NpcBonds.Band.ENEMY && NpcBonds.band(NpcBonds.ENEMY) == NpcBonds.Band.ENEMY, "enemy at and below -40");
        check(NpcBonds.band(-1) == NpcBonds.Band.COLD && NpcBonds.band(0) == NpcBonds.Band.STRANGER && NpcBonds.band(NpcBonds.FRIENDLY) == NpcBonds.Band.FRIENDLY
            && NpcBonds.band(NpcBonds.FRIEND) == NpcBonds.Band.FRIEND, "the bands");
        for (NpcTemper.Attitude base : NpcTemper.Attitude.values()) {
            check(NpcBonds.attitude(base, NpcBonds.ENEMY) == NpcTemper.Attitude.HOSTILE, "an enemy is gone for, whatever the temper: " + base);
            check(NpcBonds.attitude(base, 0) == base, "a stranger is as its temper says: " + base);
        }
        check(NpcBonds.attitude(NpcTemper.Attitude.WARY, NpcBonds.FRIENDLY) == NpcTemper.Attitude.FRIENDLY, "a friendly one won't fight first");
        check(NpcBonds.attitude(NpcTemper.Attitude.HOSTILE, NpcBonds.MOST) == NpcTemper.Attitude.HOSTILE, "regard never tames a hunter");
        check(NpcBonds.struck(NpcBonds.FRIEND) == NpcBonds.FRIEND + NpcBonds.BLOW && !NpcBonds.betrayal(NpcBonds.FRIEND), "a friend forgives one stray blow");
        check(NpcBonds.betrayal(NpcBonds.FRIENDLY) && NpcBonds.struck(NpcBonds.FRIENDLY) == NpcBonds.BETRAYED, "the blow that turns a friend is a betrayal");
        check(NpcBonds.struck(0) == NpcBonds.BLOW && !NpcBonds.betrayal(0), "a stranger struck only cools");
        check(NpcBonds.struck(-90) == NpcBonds.LEAST, "never below the least");
        check(NpcBonds.joins(NpcTemper.DRIFTER, null) && NpcBonds.joins(NpcTemper.ANTEIKU, "anteiku") && !NpcBonds.joins(NpcTemper.ANTEIKU, "pack")
            && NpcBonds.joins(NpcTemper.PACK, "pack") && !NpcBonds.joins(NpcTemper.PACK, null) && !NpcBonds.joins(NpcTemper.GOURMET, null)
            && !NpcBonds.joins(NpcTemper.FERAL, null), "who walks with whom");
    }

    /** Every temper but the feral asks something fitting, the same until it is done; favours and bonds keep through a save. */
    static void favours() {
        for (NpcTemper temper : NpcTemper.values()) {
            for (int seed = 0; seed < 200; seed++) {
                NpcIdentity who = new NpcIdentity(NpcIdentity.Kind.RINKAKU, temper, 1, 2, seed % NpcNames.count(), seed * 337);
                Favours.Spec spec = Favours.offer(who, seed % 4);
                if (temper == NpcTemper.FERAL) {
                    check(spec == null, "a feral asks nothing");
                    continue;
                }
                check(spec != null && spec.equals(Favours.offer(who, seed % 4)), temper + ": the same ask until it is done");
                check(Favours.Spec.load(spec.save()).equals(spec), "a favour's spec round trips");
                Quest quest = Favours.quest(spec, "1b4e28ba-2fa1-11d2-883f-0016d3cca427", who, seed % 4);
                check(quest.line() == Quest.Line.FAVOUR && quest.objectives().size() == 1 && quest.title().contains(who.given()),
                    "a favour is a quest of the favour line: " + quest.title());
                check(!quest.id().contains(";") && !quest.id().contains("=") && !quest.id().contains(","), "a favour's id fits the save: " + quest.id());
                String asking = Favours.asking(spec, who);
                check(asking.contains("pays"), "it says what it pays: " + asking);
                boolean violent = quest.objectives().get(0).all().contains(Tags.temper("anteiku"));
                check(!violent || temper == NpcTemper.PACK, "only the Pack asks for Anteiku's blood");
            }
        }
        NpcIdentity ren = new NpcIdentity(NpcIdentity.Kind.UKAKU, NpcTemper.DRIFTER, 1, 4, 36, 4242);
        String giver = "1b4e28ba-2fa1-11d2-883f-0016d3cca427";
        QuestLog log = new QuestLog();
        log.bond(giver, log.bond(giver).regard(NpcBonds.FRIENDLY).with(NpcBonds.MET, true));
        Favours.Spec spec = Favours.offer(ren, 0);
        check(log.startFavour(spec, giver, ren), "a favour taken on");
        check(!log.startFavour(spec, giver, ren), "one favour at a time for the same ghoul");
        for (int i = 0; i < 4; i++) {
            log.startFavour(Favours.offer(new NpcIdentity(NpcIdentity.Kind.UKAKU, NpcTemper.ANTEIKU, 0, 0, i, i * 11), 0), "other-" + i,
                new NpcIdentity(NpcIdentity.Kind.UKAKU, NpcTemper.ANTEIKU, 0, 0, i, i * 11));
        }
        check(log.favours().size() == NpcBonds.MOST_FAVOURS, "no more than three on");
        QuestLog read = QuestLog.load(log.save());
        check(read.favours().size() == log.favours().size() && read.favourFrom(giver) != null
            && read.favourFrom(giver).quest().id().equals(log.favourFrom(giver).quest().id()), "favours keep through a save");
        check(read.bond(giver).regard() == NpcBonds.FRIENDLY && read.bond(giver).has(NpcBonds.MET), "bonds keep through a save");
        Quest favour = read.favourFrom(giver).quest();
        check(read.quests().contains(favour), "a favour counts like any quest");
        com.hex.kagune.control.quest.Objective asked = favour.objectives().get(0);
        for (int i = 0; i < 40; i++) {
            read.apply(new QuestEvent(asked.signal(), 1, new java.util.HashSet<>(asked.all())), com.hex.kagune.control.KagunePath.RINKAKU, 500);
        }
        if (read.met(favour)) {
            read.complete(favour);
            check(read.favourFrom(giver) == null, "a favour done is taken off");
            check(read.bond(giver).regard() == NpcBonds.FRIENDLY + NpcBonds.FAVOUR_DONE && read.bond(giver).favours() == 1, "and the ghoul thinks better of you");
            check(!Favours.offer(ren, 1).equals(spec) || Favours.offer(ren, 1).equals(Favours.offer(ren, 1)), "the next ask is rolled afresh");
        } else {
            check(false, "a favour can be done: " + favour.objectives().get(0).text());
        }
        QuestLog crowded = new QuestLog();
        for (int i = 0; i < NpcBonds.REMEMBERED + 10; i++) {
            crowded.bond("npc-" + i, QuestLog.Bond.NONE.regard(i % 7 == 0 ? NpcBonds.FRIEND : 1));
        }
        check(crowded.bonds().size() == NpcBonds.REMEMBERED, "only so many bonds remembered: " + crowded.bonds().size());
        check(crowded.bond("npc-0").regard() == NpcBonds.FRIEND && crowded.bond("npc-" + (NpcBonds.REMEMBERED + 9)).regard() == 1,
            "the faintest forgotten first, the newest kept");
        check(QuestBook.get("a_friend") != null && QuestBook.get("cold_blood") != null && "trust".equals(QuestBook.get("a_friend").exclusive())
            && "trust".equals(QuestBook.get("cold_blood").exclusive()), "a friend or cold blood: one closes the other");
    }

    /** Every temper talks in its own voice at every standing, offers what fits, and never talks when it would fight. */
    static void talk() {
        String[] sides = {null, "anteiku", "pack"};
        java.util.Set<String> greetings = new java.util.HashSet<>();
        for (NpcTemper temper : NpcTemper.values()) {
            for (String side : sides) {
                for (boolean ghoul : new boolean[] {false, true}) {
                    NpcTemper.Attitude base = temper.toward(ghoul, side);
                    for (int regard : new int[] {-30, 0, 30, 70}) {
                        NpcTemper.Attitude attitude = NpcBonds.attitude(base, regard);
                        boolean talks = NpcTalk.talks(temper, attitude);
                        check(!talks || temper != NpcTemper.FERAL && attitude != NpcTemper.Attitude.HOSTILE, temper + " talks only when it would not fight");
                        if (!talks) {
                            continue;
                        }
                        NpcIdentity who = new NpcIdentity(NpcIdentity.Kind.RINKAKU, temper, 1, 0, 5, regard + 100);
                        Favours.Spec spec = Favours.offer(who, 0);
                        for (boolean ally : new boolean[] {false, true}) {
                            NpcTalk.Context c = new NpcTalk.Context(who, regard, ghoul, side, ally, ally, false, false, true, false, true,
                                spec == null ? null : Favours.asking(spec, who), true);
                            NpcTalk.Page page = NpcTalk.open(c);
                            greetings.add(page.line());
                            check(!page.line().isBlank() && page.choices().contains(NpcTalk.Choice.LEAVE) && page.choices().size() <= 8,
                                temper + " greets and lets you leave");
                            check(page.choices().contains(NpcTalk.Choice.PART) == ally && page.choices().contains(NpcTalk.Choice.INSULT) == !ally,
                                "an ally can be parted with, a stranger insulted");
                            check(!page.choices().contains(NpcTalk.Choice.GIFT) || temper != NpcTemper.ANTEIKU, "Anteiku takes no ghoul's flesh");
                            for (NpcTalk.Choice choice : page.choices()) {
                                NpcTalk.Reply reply = NpcTalk.answer(c, choice);
                                check(choice == NpcTalk.Choice.LEAVE || !reply.line().isBlank(), temper + " answers " + choice);
                                if (reply.effect() == NpcTalk.Effect.ALLY) {
                                    check(regard >= NpcBonds.FRIEND && !ally && NpcBonds.joins(temper, side), "joins only a friend it would walk with");
                                }
                            }
                        }
                    }
                }
            }
        }
        check(greetings.size() >= 25, "many different greetings: " + greetings.size());
        NpcIdentity scav = new NpcIdentity(NpcIdentity.Kind.UKAKU, NpcTemper.SCAVENGER, 0, 0, 0, 1);
        NpcTalk.Context c = new NpcTalk.Context(scav, 0, true, null, false, false, true, true, false, false, true, "Eat raw flesh (4). It pays 20 mastery.",
            true);
        check(NpcTalk.answer(c, NpcTalk.Choice.CHAT).regard() == 0, "one chat a night counts");
        check(NpcTalk.answer(c, NpcTalk.Choice.WHO).regard() == 0, "being told twice who it is counts once");
        check(NpcTalk.answer(c, NpcTalk.Choice.FAVOUR).effect() == NpcTalk.Effect.OFFER, "it offers its favour");
        check(NpcTalk.answer(c, NpcTalk.Choice.ACCEPT).effect() == NpcTalk.Effect.START_FAVOUR, "and it is taken on");
        check(NpcTalk.answer(c, NpcTalk.Choice.INSULT).effect() == NpcTalk.Effect.FLEE, "an insulted scavenger runs");
        check(NpcTalk.answer(c, NpcTalk.Choice.GIFT).effect() == NpcTalk.Effect.NONE, "no gift without meat in hand");
    }

    /**
     * 1.3.36: the arm blade (an NPC-only koukaku): three cuts to a string, chained only close together; each lands before it ends; the
     * thrust reaches furthest and hits hardest; the spiral winds shoulder to wrist before the blade grows; its shape stays on the arm; its
     * poses hold the blade arm and keep the free hand on the forearm while it winds.
     */
    static void blades() {
        check(com.hex.kagune.control.npc.BladeArm.next(-1, 0L) == 0 && com.hex.kagune.control.npc.BladeArm.next(0, 3L) == 1
            && com.hex.kagune.control.npc.BladeArm.next(1, 3L) == 2 && com.hex.kagune.control.npc.BladeArm.next(2, 3L) == 0
            && com.hex.kagune.control.npc.BladeArm.next(1, com.hex.kagune.control.npc.BladeArm.CHAIN + 1) == 0
            && com.hex.kagune.control.npc.BladeArm.next(0, -5L) == 0, "three cuts to a string, chained only close together");
        for (int step = 0; step < com.hex.kagune.control.npc.BladeArm.STEPS; step++) {
            check(com.hex.kagune.control.npc.BladeArm.lands(step) > 1 && com.hex.kagune.control.npc.BladeArm.lands(step)
                < com.hex.kagune.control.npc.BladeArm.ticks(step), "cut " + step + " lands partway through");
        }
        check(com.hex.kagune.control.npc.BladeArm.reach(1, 2) > com.hex.kagune.control.npc.BladeArm.reach(1, 0)
            && com.hex.kagune.control.npc.BladeArm.weight(2) > com.hex.kagune.control.npc.BladeArm.weight(0)
            && com.hex.kagune.control.npc.BladeArm.reach(2) > com.hex.kagune.control.npc.BladeArm.reach(0), "the thrust furthest and hardest; stronger, longer");
        double last = 1.0;
        for (int i = 0; i < com.hex.kagune.control.npc.BladeArm.PLATES; i++) {
            double at = com.hex.kagune.control.npc.BladeArm.plate(i, 0.3);
            check(at <= last + 1.0E-9, "the spiral winds from the shoulder down (" + i + ")");
            last = at;
            check(com.hex.kagune.control.npc.BladeArm.plate(i, 0.0) == 0.0 && com.hex.kagune.control.npc.BladeArm.plate(i, 0.8) == 1.0,
                "plate " + i + " none at first, grown before the blade is done");
        }
        check(com.hex.kagune.control.npc.BladeArm.blade(0.5) == 0.0 && com.hex.kagune.control.npc.BladeArm.blade(1.0) == 1.0
            && com.hex.kagune.control.npc.BladeArm.blade(0.8) > 0.0, "the blade grows once the spiral reaches the wrist");
        int[] quads = {0};
        double[] most = {0.0, -99.0, 0.0, 99.0};
        for (boolean slim : new boolean[] {false, true}) {
            com.hex.kagune.control.npc.BladeArmModel.build(1.0, slim, 0x7A1E5C, (a, b, c, d, n, ca, cb, cc, cd) -> {
                quads[0]++;
                for (double[] p : new double[][] {a, b, c, d}) {
                    most[0] = Math.max(most[0], Math.max(Math.abs(p[0] + 1.0), Math.abs(p[2])));
                    most[1] = Math.max(most[1], p[1]);
                    most[3] = Math.min(most[3], p[1]);
                    check(Double.isFinite(p[0]) && Double.isFinite(p[1]) && Double.isFinite(p[2]) && (ca >>> 24) == 0xFF, "a finite, opaque corner");
                }
            });
        }
        check(quads[0] > 100 && most[0] < 7.0 && most[1] > 24.0 && most[1] < 30.0 && most[3] > -8.0,
            "the blade on the arm: close round it, its point well past the fist (" + quads[0] + " quads, " + most[0] + ", " + most[3] + ".." + most[1] + ")");
        int[] none = {0};
        com.hex.kagune.control.npc.BladeArmModel.build(0.0, false, 0x7A1E5C, (a, b, c, d, n, ca, cb, cc, cd) -> none[0]++);
        check(none[0] == 0, "nothing before it winds out");
        com.hex.kagune.control.client.animation.RigPose guard = com.hex.kagune.control.client.animation.BladeArmPose.guard();
        check(guard.rightArmHold == 1.0F && guard.twist < 0.0F, "its guard: the blade arm held, the right shoulder forward");
        for (int step = 0; step < 3; step++) {
            for (double t = 0.0; t <= 1.0; t += 0.1) {
                com.hex.kagune.control.client.animation.RigPose p = com.hex.kagune.control.client.animation.BladeArmPose.swing(step, t);
                check(Float.isFinite(p.rightArmX) && Float.isFinite(p.rightArmY) && Float.isFinite(p.rightArmZ) && Math.abs(p.lunge) <= 8.0F,
                    "cut " + step + " at " + t + " is a sane pose");
            }
        }
        com.hex.kagune.control.client.animation.RigPose winding = com.hex.kagune.control.client.animation.BladeArmPose.wrap(0.5);
        double[] left = com.hex.kagune.control.client.animation.QuinqueGrip.fist(winding, false, false);
        float[] turn = winding.transform("rightArm", com.hex.kagune.control.client.animation.RigPose.ROTATION, 0, 0, 0);
        float[] pivot = winding.transform("rightArm", com.hex.kagune.control.client.animation.RigPose.POSITION, -5, 2, 0);
        double cx = Math.cos(turn[0]), sx = Math.sin(turn[0]), cy = Math.cos(turn[1]), sy = Math.sin(turn[1]), cz = Math.cos(turn[2]), sz = Math.sin(turn[2]);
        double[] local = {-1.0, 6.5, -0.5};
        // Rz Ry Rx of the forearm point.
        double y1 = local[1] * cx - local[2] * sx;
        double z1 = local[1] * sx + local[2] * cx;
        double x2 = local[0] * cy + z1 * sy;
        double z2 = -local[0] * sy + z1 * cy;
        double[] forearm = {pivot[0] + x2 * cz - y1 * sz, pivot[1] + x2 * sz + y1 * cz, pivot[2] + z2};
        double off = Math.sqrt(Math.pow(left[0] - forearm[0], 2) + Math.pow(left[1] - forearm[1], 2) + Math.pow(left[2] - forearm[2], 2));
        check(off < 3.0, "winding out, its free hand grips the forearm (" + off + " px)");
        com.hex.kagune.control.client.animation.RigPose run = com.hex.kagune.control.client.animation.BladeArmPose.run(0.5, 1.0F);
        check(run.lean > 0.3F && run.rightArmHold == 1.0F, "its sprint: pitched forward, the blade arm swept back");
    }

    /**
     * 1.3.38: the speech bubble and the thought cloud: every quad above the speaker's head but the tail's tip or the trail's last puff;
     * the words inside the box; the outline behind the body, the body behind the words; actions make a cloud, words a bubble; words
     * inked black, thoughts dark grey and slanted; it pops in and settles; a sleeper's z's grow.
     */
    static void bubbles() {
        for (boolean thought : new boolean[] {false, true}) {
            for (int lines = 1; lines <= 4; lines++) {
                for (int width : new int[] {6, 40, 180}) {
                    com.hex.kagune.control.npc.BubbleShape shape = com.hex.kagune.control.npc.BubbleShape.of(width, lines, thought);
                    int[] quads = {0};
                    float[] lowest = {-99.0F};
                    boolean[] layered = {true};
                    shape.fill((a, b, c, d, argb, depth) -> {
                        quads[0]++;
                        for (float[] p : new float[][] {a, b, c, d}) {
                            lowest[0] = Math.max(lowest[0], p[1]);
                        }
                        layered[0] &= depth == com.hex.kagune.control.npc.BubbleShape.OUTLINE_DEPTH
                            ? argb == (thought ? com.hex.kagune.control.npc.BubbleShape.CLOUD_INK : com.hex.kagune.control.npc.BubbleShape.INK)
                            : depth == com.hex.kagune.control.npc.BubbleShape.BODY_DEPTH && (argb >>> 24) == 0xFF;
                    });
                    String what = (thought ? "cloud " : "bubble ") + lines + "x" + width;
                    check(quads[0] > 20 && lowest[0] <= 2.0F && lowest[0] >= -0.1F, what + ": over the head, its point at it (" + lowest[0] + ")");
                    check(layered[0] && com.hex.kagune.control.npc.BubbleShape.OUTLINE_DEPTH > com.hex.kagune.control.npc.BubbleShape.BODY_DEPTH,
                        what + ": outline behind body, body behind the words");
                    check(shape.x1 - shape.x0 >= width + 2 * com.hex.kagune.control.npc.BubbleShape.PAD && shape.lineY(0) > shape.y0
                        && shape.lineY(lines - 1) + 9 <= shape.y1 && shape.y1 < 0.0F, what + ": the words inside the box");
                }
            }
        }
        check(com.hex.kagune.control.npc.BubbleShape.thought(java.util.List.of("§7§o*stirs*")) && !com.hex.kagune.control.npc.BubbleShape.thought(
            java.util.List.of("§fHey.")) && !com.hex.kagune.control.npc.BubbleShape.thought(java.util.List.of()), "an action is a cloud, words a bubble");
        check(com.hex.kagune.control.npc.BubbleShape.ink("§fHey.", false).equals("§0Hey.") && com.hex.kagune.control.npc.BubbleShape.ink("§7§o*sighs*",
            true).equals("§8§o*sighs*"), "words inked black, thoughts dark grey and slanted");
        check(com.hex.kagune.control.npc.BubbleShape.pop(0.0) < 0.7F && com.hex.kagune.control.npc.BubbleShape.pop(2.6) > 1.0F
            && com.hex.kagune.control.npc.BubbleShape.pop(10.0) == 1.0F && com.hex.kagune.control.npc.BubbleShape.pop(1.0E9) == 1.0F, "it pops in and settles");
        check(com.hex.kagune.control.npc.BubbleShape.zees(0).equals("z") && com.hex.kagune.control.npc.BubbleShape.zees(20).equals("z Z")
            && com.hex.kagune.control.npc.BubbleShape.zees(50).equals("z Z z"), "a sleeper's z's grow");
        turns();
        layout();
    }

    /**
     * 1.3.52: a talk's turns: a line answered by someone else comes down as the answer goes up (never both at once), shrinking away; the
     * same speaker's next line takes its place anyway; a lone line stays as long as it takes to read.
     */
    static void turns() {
        String line = "You always get the bigger half and it's not fair.";
        check(com.hex.kagune.control.npc.Speech.turn(4, 5, 70L) == 70 && com.hex.kagune.control.npc.Speech.turn(4, 4, 70L) == 0
            && com.hex.kagune.control.npc.Speech.turn(4, 5, 3L) == 20 && com.hex.kagune.control.npc.Speech.turn(4, 5, 99999L)
            == com.hex.kagune.control.npc.Speech.MOST, "a line is up until someone else answers (at least a second)");
        check(com.hex.kagune.control.npc.Speech.life(line, 0) == com.hex.kagune.control.npc.Speech.ticks(line)
            && com.hex.kagune.control.npc.Speech.life(line, 70) == 70 && com.hex.kagune.control.npc.Speech.life(line, 2) == 10
            && com.hex.kagune.control.npc.Speech.life(line, 9999) == com.hex.kagune.control.npc.Speech.ticks(line), "sent for a while, up that long");
        boolean readable = true;
        for (int n = 0; n <= 120; n++) {
            String text = "x".repeat(n);
            int pause = com.hex.kagune.control.family.Parenting.pause(text);
            readable &= pause < com.hex.kagune.control.npc.Speech.ticks(text) && pause >= 40 && pause <= 140 && pause >= 20 + n;
        }
        check(readable, "each turn up long enough to read (a tick or more a letter), the next before it would have come down anyway");
        check(com.hex.kagune.control.npc.BubbleShape.pop(10.0, 100.0) == 1.0F && com.hex.kagune.control.npc.BubbleShape.pop(10.0, 0.0) == 0.0F
            && com.hex.kagune.control.npc.BubbleShape.pop(10.0, 1.5) > 0.3F && com.hex.kagune.control.npc.BubbleShape.pop(10.0, 1.5) < 1.0F
            && com.hex.kagune.control.npc.BubbleShape.pop(1.0, 100.0) == com.hex.kagune.control.npc.BubbleShape.pop(1.0), "it shrinks away at its end");
        // Moved aside or up: the box goes, its point stays at the head; the tail (a cloud's trail) leans down to it.
        for (boolean thought : new boolean[] {false, true}) {
            com.hex.kagune.control.npc.BubbleShape home = com.hex.kagune.control.npc.BubbleShape.of(120, 2, thought);
            com.hex.kagune.control.npc.BubbleShape same = com.hex.kagune.control.npc.BubbleShape.of(120, 2, thought, 0.0F, 0.0F);
            check(same.home() && same.x0 == home.x0 && same.y1 == home.y1, "unmoved, the same shape as before");
            for (float[] move : new float[][] {{90.0F, 0.0F}, {-150.0F, 0.0F}, {0.0F, 40.0F}, {60.0F, 25.0F}}) {
                com.hex.kagune.control.npc.BubbleShape moved = com.hex.kagune.control.npc.BubbleShape.of(120, 2, thought, move[0], move[1]);
                float[] lowest = {-99.0F, 0.0F};
                int[] below = {0};
                moved.fill((a, b, c, d, argb, depth) -> {
                    for (float[] pt : new float[][] {a, b, c, d}) {
                        if (pt[1] > lowest[0]) {
                            lowest[0] = pt[1];
                            lowest[1] = pt[0];
                        }
                        below[0] += pt[1] > moved.y1 + 2.0F ? 1 : 0;
                    }
                });
                String what = (thought ? "cloud" : "bubble") + " moved " + move[0] + "," + move[1];
                check(Math.abs(moved.x0 - (home.x0 + move[0])) < 0.01F && Math.abs(moved.y1 - (home.y1 - move[1])) < 0.01F, what + ": its box moved");
                check(lowest[0] <= 2.6F && lowest[0] >= -2.0F && Math.abs(lowest[1] + 5.0F) < 4.5F, what + ": its point still at the head (" + lowest[0]
                    + " at " + lowest[1] + ")");
                check(below[0] > 0, what + ": something reaches down from the box to the head");
                check(moved.lineY(0) > moved.y0 && moved.lineY(1) + 9 <= moved.y1, what + ": the words still inside");
            }
        }
    }

    /** 1.3.52: bubbles kept apart: two that would overlap spread apart, each to its own speaker's side; boxed in, the newer goes up. */
    static void layout() {
        double k = 0.025 / 6.0; // radians a pixel six blocks off
        com.hex.kagune.control.npc.BubbleShape wide = com.hex.kagune.control.npc.BubbleShape.of(200, 2, false);
        com.hex.kagune.control.npc.BubbleShape small = com.hex.kagune.control.npc.BubbleShape.of(60, 1, false);
        java.util.function.BiFunction<Integer, Double, com.hex.kagune.control.npc.BubbleLayout.Seen> at = (id, x) -> new com.hex.kagune.control.npc.BubbleLayout.Seen(
            id, 100.0 - id, x, 0.0, k, wide.x0, wide.y0, wide.x1, wide.y1, wide.rim());
        java.util.Map<Integer, com.hex.kagune.control.npc.BubbleLayout.Place> one = com.hex.kagune.control.npc.BubbleLayout.arrange(java.util.List.of(at.apply(1, 0.0)));
        check(one.get(1).equals(com.hex.kagune.control.npc.BubbleLayout.Place.HOME), "alone, over its head");
        java.util.Map<Integer, com.hex.kagune.control.npc.BubbleLayout.Place> apart = com.hex.kagune.control.npc.BubbleLayout.arrange(java.util.List.of(
            at.apply(1, 0.0), at.apply(2, 1.2)));
        check(apart.get(1).equals(com.hex.kagune.control.npc.BubbleLayout.Place.HOME) && apart.get(2).equals(com.hex.kagune.control.npc.BubbleLayout.Place.HOME),
            "far enough apart, both over their own heads");
        // Two speakers a block and a half apart, six blocks off: spread apart sideways, each to its own speaker's side, never stacked.
        double near = 1.5 / 6.0;
        for (double side : new double[] {1.0, -1.0}) {
            java.util.List<com.hex.kagune.control.npc.BubbleLayout.Seen> two = java.util.List.of(at.apply(1, 0.0), at.apply(2, side * near));
            java.util.Map<Integer, com.hex.kagune.control.npc.BubbleLayout.Place> placed = com.hex.kagune.control.npc.BubbleLayout.arrange(two);
            check(placed.get(2).shift() * side > 0.0F && placed.get(1).shift() * side < 0.0F && placed.get(1).lift() == 0.0F && placed.get(2).lift() == 0.0F,
                "spread apart, each to its own side: " + placed);
            check(clear(two, placed), "and clear of each other");
            for (com.hex.kagune.control.npc.BubbleLayout.Seen b : two) {
                double[] box = box(b, placed.get(b.id()));
                check(b.x() > box[0] && b.x() < box[2], "each still over its own speaker, its tail short");
            }
        }
        // Right on top of each other (a little one on its parent's hip): split to either side, the point between them.
        java.util.List<com.hex.kagune.control.npc.BubbleLayout.Seen> stacked = java.util.List.of(at.apply(1, 0.0), new com.hex.kagune.control.npc.BubbleLayout.Seen(
            2, 1.0, 0.0, 0.0004, k * 1.06, small.x0, small.y0, small.x1, small.y1, small.rim()));
        java.util.Map<Integer, com.hex.kagune.control.npc.BubbleLayout.Place> hip = com.hex.kagune.control.npc.BubbleLayout.arrange(stacked);
        check(clear(stacked, hip) && hip.get(1).shift() < 0.0F && hip.get(2).shift() > 0.0F && hip.get(2).lift() <= com.hex.kagune.control.npc.BubbleLayout.TILT,
            "a little one's bubble on its parent's: split either side, " + hip);
        // A cloud (its puffs reach past its box) beside a bubble: what is drawn kept clear, not just the boxes.
        com.hex.kagune.control.npc.BubbleShape cloud = com.hex.kagune.control.npc.BubbleShape.of(190, 2, true);
        java.util.List<com.hex.kagune.control.npc.BubbleLayout.Seen> puffs = java.util.List.of(com.hex.kagune.control.npc.BubbleLayout.Seen.of(1, 30.0, 0.0, 0.0,
            k, cloud), com.hex.kagune.control.npc.BubbleLayout.Seen.of(2, 5.0, 0.0, 0.0004, k * 1.06, small));
        java.util.Map<Integer, com.hex.kagune.control.npc.BubbleLayout.Place> drawn = com.hex.kagune.control.npc.BubbleLayout.arrange(puffs);
        double cloudRight = (cloud.x1 + cloud.rim() + drawn.get(1).shift()) * k;
        double bubbleLeft = 0.0004 + (small.x0 - small.rim() + drawn.get(2).shift()) * k * 1.06;
        check(cloud.rim() > 4.0F && bubbleLeft >= cloudRight, "a cloud's puffs kept clear of the bubble beside it");
        // Three in a row: nobody covers anybody.
        java.util.List<com.hex.kagune.control.npc.BubbleLayout.Seen> three = java.util.List.of(at.apply(1, 0.0), at.apply(2, near), at.apply(3, -near));
        check(clear(three, com.hex.kagune.control.npc.BubbleLayout.arrange(three)), "three talking at once: none covers another");
        // Five crowded together: no room aside for all, so some go up; still none covers another.
        java.util.List<com.hex.kagune.control.npc.BubbleLayout.Seen> crowd = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            crowd.add(at.apply(i + 1, (i - 2) * 0.1));
        }
        java.util.Map<Integer, com.hex.kagune.control.npc.BubbleLayout.Place> lifted = com.hex.kagune.control.npc.BubbleLayout.arrange(crowd);
        boolean up = false;
        for (com.hex.kagune.control.npc.BubbleLayout.Place place : lifted.values()) {
            up |= place.lift() > 0.0F;
        }
        check(clear(crowd, lifted) && up, "a crowd: some go up, none covers another");
        check(com.hex.kagune.control.npc.BubbleLayout.arrange(java.util.List.of(at.apply(2, near), at.apply(1, 0.0))).equals(
            com.hex.kagune.control.npc.BubbleLayout.arrange(java.util.List.of(at.apply(1, 0.0), at.apply(2, near)))), "the same whatever order they come in");
    }

    private static boolean clear(java.util.List<com.hex.kagune.control.npc.BubbleLayout.Seen> seen,
                                 java.util.Map<Integer, com.hex.kagune.control.npc.BubbleLayout.Place> placed) {
        for (com.hex.kagune.control.npc.BubbleLayout.Seen a : seen) {
            for (com.hex.kagune.control.npc.BubbleLayout.Seen b : seen) {
                if (a.id() < b.id()) {
                    double[] p = box(a, placed.get(a.id()));
                    double[] q = box(b, placed.get(b.id()));
                    if (p[0] < q[2] && q[0] < p[2] && p[1] < q[3] && q[1] < p[3]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static double[] box(com.hex.kagune.control.npc.BubbleLayout.Seen b, com.hex.kagune.control.npc.BubbleLayout.Place place) {
        double k = b.perPixel();
        double r = b.rim();
        return new double[] {b.x() + (b.x0() - r + place.shift()) * k, b.y() + (-b.y1() - r + place.lift()) * k, b.x() + (b.x1() + r + place.shift()) * k,
            b.y() + (-b.y0() + r + place.lift()) * k};
    }

    /** 1.3.71: the Brick Village: its map, and its people's sense of where they are once it is placed turned. */
    /**
     * 2.0.35: who each villager falls for (its own for life): most the other sex, a fair share both, some their own; a player who isn't who
     * it falls for is turned down kindly; every line filled, in character, never a banned name.
     */
    static void attraction() {
        java.util.regex.Pattern banned = java.util.regex.Pattern.compile("Kaneki|Ayato|Furuta|Takatsuki|Aogiri|Yoshimura|\\{|\\}|null");
        for (boolean female : new boolean[] {true, false}) {
            int[] counts = new int[3];
            for (long seed = 0; seed < 20000; seed++) {
                counts[com.hex.kagune.control.village.HumanPersona.attraction(seed * 7919L + 13L, female).ordinal()]++;
            }
            int other = counts[(female ? com.hex.kagune.control.village.HumanPersona.Attraction.MEN : com.hex.kagune.control.village.HumanPersona.Attraction.WOMEN).ordinal()];
            int own = counts[(female ? com.hex.kagune.control.village.HumanPersona.Attraction.WOMEN : com.hex.kagune.control.village.HumanPersona.Attraction.MEN).ordinal()];
            int both = counts[com.hex.kagune.control.village.HumanPersona.Attraction.BOTH.ordinal()];
            check(other > 11500 && other < 13300 && both > 3800 && both < 5000 && own > 2700 && own < 3700,
                (female ? "women" : "men") + ": most fall for the other sex, a fair share both, some their own: " + java.util.Arrays.toString(counts));
        }
        long seed = 123456789L;
        check(com.hex.kagune.control.village.HumanPersona.attraction(seed, true) == com.hex.kagune.control.village.HumanPersona.attraction(seed, true)
            && com.hex.kagune.control.village.HumanPersona.taste(seed) == com.hex.kagune.control.village.HumanPersona.taste(seed), "its own for life");
        java.util.Set<com.hex.kagune.control.village.HumanPersona.Taste> tastes = java.util.EnumSet.noneOf(com.hex.kagune.control.village.HumanPersona.Taste.class);
        for (long s = 0; s < 2000; s++) {
            tastes.add(com.hex.kagune.control.village.HumanPersona.taste(s));
        }
        check(tastes.size() == com.hex.kagune.control.village.HumanPersona.Taste.values().length, "every taste turns up");
        var MEN = com.hex.kagune.control.village.HumanPersona.Attraction.MEN;
        var WOMEN = com.hex.kagune.control.village.HumanPersona.Attraction.WOMEN;
        var BOTH = com.hex.kagune.control.village.HumanPersona.Attraction.BOTH;
        check(com.hex.kagune.control.village.HumanPersona.attracted(MEN, false) && !com.hex.kagune.control.village.HumanPersona.attracted(MEN, true)
            && com.hex.kagune.control.village.HumanPersona.attracted(WOMEN, true) && !com.hex.kagune.control.village.HumanPersona.attracted(WOMEN, false)
            && com.hex.kagune.control.village.HumanPersona.attracted(BOTH, true) && com.hex.kagune.control.village.HumanPersona.attracted(BOTH, false)
            && com.hex.kagune.control.village.HumanPersona.attracted(MEN, null) && com.hex.kagune.control.village.HumanPersona.attracted(WOMEN, null),
            "who may date whom (a player who never said may date anyone)");
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : com.hex.kagune.control.village.HumanPersona.Temper.values()) {
            for (int roll = 0; roll < 6; roll++) {
                for (var a : com.hex.kagune.control.village.HumanPersona.Attraction.values()) {
                    for (var t : com.hex.kagune.control.village.HumanPersona.Taste.values()) {
                        String line = com.hex.kagune.control.village.HumanPersona.type(temper, a, t, roll);
                        check(!line.isBlank() && !banned.matcher(line).find() && line.contains(a == MEN ? "men" : a == WOMEN ? "women" : "person")
                            || a == BOTH && line.contains("Anyone"), "its type, in its own words: " + temper + " " + a + " " + t);
                    }
                }
                for (var a : new com.hex.kagune.control.village.HumanPersona.Attraction[] {MEN, WOMEN}) {
                    String no = com.hex.kagune.control.village.HumanPersona.notMyType(temper, a, roll);
                    check(!no.isBlank() && !banned.matcher(no).find() && no.toLowerCase(java.util.Locale.ROOT).contains(a == MEN ? "men" : "women")
                        && !no.toLowerCase(java.util.Locale.ROOT).contains(a == MEN ? "women" : "zzz"), "a kind no that says who it falls for: " + temper + " " + a);
                }
            }
        }
        check(com.hex.kagune.control.village.HumanPersona.trusts(com.hex.kagune.control.village.HumanPersona.Taste.DARING) > 0.0
            && com.hex.kagune.control.village.HumanPersona.trusts(com.hex.kagune.control.village.HumanPersona.Taste.FUNNY) == 0.0,
            "someone who likes a little danger takes a ghoul's confession more readily");
        check(com.hex.kagune.control.village.HumanPersona.NOT_TYPE != com.hex.kagune.control.village.HumanPersona.PARTNER
            && com.hex.kagune.control.village.HumanPersona.NOT_TYPE != com.hex.kagune.control.village.HumanPersona.CONFESSED
            && (com.hex.kagune.control.village.HumanPersona.NOT_TYPE & (com.hex.kagune.control.npc.NpcBonds.MET | com.hex.kagune.control.npc.NpcBonds.BEFRIENDED
            | com.hex.kagune.control.npc.NpcBonds.ASKED | com.hex.kagune.control.npc.NpcBonds.ALLY)) == 0, "its own bond flag");
    }

    /**
     * 2.1.3: every villager is someone (HumanLife, HumanVoice): a woman's or a man's name, a life of its own for life, every temper able to
     * say every part of it cleanly, the page offering what it may be asked as it warms, gifts landing as they should, its village's people
     * each given a different trade.
     */
    static void lives() {
        java.util.regex.Pattern banned = java.util.regex.Pattern.compile("Kaneki|Ayato|Furuta|Takatsuki|Aogiri|Yoshimura|Touka|Hinami|Nishiki|Amon|Juuzou"
            + "|Arima|Rize|Uta\\b|Yomo|Shuu|Tsukiyama|Akira|Mutsuki|Urie|Saiko|Haise|Hide\\b|Hideyoshi|Kimi\\b|Chie\\b|Yoriko|Banjou");
        java.util.Set<String> names = new java.util.HashSet<>();
        for (boolean woman : new boolean[] {true, false}) {
            for (int i = 0; i < com.hex.kagune.control.village.HumanLife.names(woman); i++) {
                String name = com.hex.kagune.control.village.HumanLife.name(woman, i);
                check(names.add(name) && !banned.matcher(name).find() && name.matches("[A-Z][a-z]+")
                    && !name.toLowerCase(java.util.Locale.ROOT).matches(".*(kaneki|ayato|furuta).*"), "a name of its own (none banned from the jar): " + name);
            }
        }
        check(com.hex.kagune.control.village.HumanLife.names(true) >= 40 && com.hex.kagune.control.village.HumanLife.names(false) >= 40,
            "plenty of women's names and men's");
        java.util.Set<String> lives = new java.util.HashSet<>();
        java.util.EnumSet<com.hex.kagune.control.village.HumanLife.Job> jobs = java.util.EnumSet.noneOf(com.hex.kagune.control.village.HumanLife.Job.class);
        java.util.EnumSet<com.hex.kagune.control.village.HumanLife.View> views = java.util.EnumSet.noneOf(com.hex.kagune.control.village.HumanLife.View.class);
        for (long seed = 0; seed < 400; seed++) {
            com.hex.kagune.control.village.HumanLife.Life life = com.hex.kagune.control.village.HumanLife.of(seed * 104729L + 7L, null);
            check(life.equals(com.hex.kagune.control.village.HumanLife.of(seed * 104729L + 7L, null)), "its own for life");
            lives.add(life.job() + life.family() + life.hobby() + life.fear() + life.dream() + life.secret());
            jobs.add(life.job());
            views.add(life.view());
            check(com.hex.kagune.control.village.HumanLife.gift(life, life.loves()) == com.hex.kagune.control.village.HumanLife.Gift.LOVED
                && com.hex.kagune.control.village.HumanLife.gift(life, "minecraft:rotten_flesh") == com.hex.kagune.control.village.HumanLife.Gift.DISLIKED
                && com.hex.kagune.control.village.HumanLife.gift(life, "minecraft:cobblestone") == com.hex.kagune.control.village.HumanLife.Gift.FINE,
                "a gift lands as it should");
        }
        check(lives.size() >= 395, "no two lives alike: " + lives.size() + " of 400");
        check(jobs.size() == com.hex.kagune.control.village.HumanLife.Job.values().length && views.size() == com.hex.kagune.control.village.HumanLife.View.values().length,
            "every trade and every view of ghouls turns up");
        java.util.Set<com.hex.kagune.control.village.HumanLife.Job> village = new java.util.HashSet<>();
        for (int slot = 0; slot < 12; slot++) {
            village.add(com.hex.kagune.control.village.HumanLife.villageJob(320, -1088, slot));
        }
        check(village.size() == 12, "a village's people each have their own trade");
        // Every temper says every part of every life, cleanly.
        boolean clean = true;
        String bad = "";
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : com.hex.kagune.control.village.HumanPersona.Temper.values()) {
            for (int seed = 0; seed < 60; seed++) {
                com.hex.kagune.control.village.HumanLife.Life life = com.hex.kagune.control.village.HumanLife.of(seed * 31L + temper.ordinal(), null);
                java.util.List<String> lines = new java.util.ArrayList<>();
                for (com.hex.kagune.control.village.HumanVoice.Topic topic : com.hex.kagune.control.village.HumanVoice.Topic.values()) {
                    if (topic != com.hex.kagune.control.village.HumanVoice.Topic.TYPE) {
                        lines.add(com.hex.kagune.control.village.HumanVoice.answer(temper, life, topic, seed * 13, seed % 2 == 0 ? null
                            : com.hex.kagune.control.village.HumanVoice.gossip("Hana", "baker", seed)));
                    }
                }
                lines.add(com.hex.kagune.control.village.HumanVoice.chat(temper, life, seed * 7));
                lines.add(com.hex.kagune.control.village.HumanVoice.caught(life, seed * 32) + "Hi.");
                for (com.hex.kagune.control.village.HumanLife.Gift how : com.hex.kagune.control.village.HumanLife.Gift.values()) {
                    lines.add(com.hex.kagune.control.village.HumanVoice.gift(temper, how, life.lovesName(), seed % 3 == 0, seed));
                }
                for (String line : lines) {
                    boolean ok = line != null && !line.isBlank() && !line.contains("{") && !line.contains("null") && line.length() <= 420
                        && !banned.matcher(line).find();
                    if (!ok && bad.isEmpty()) {
                        bad = temper + ": " + line;
                    }
                    clean &= ok;
                }
            }
        }
        check(clean, "every temper says every part of every life cleanly: " + bad);
        // The page: what may be asked, as it warms; what is not yet known first.
        java.util.List<com.hex.kagune.control.village.HumanVoice.Topic> stranger = com.hex.kagune.control.village.HumanVoice.topics(0, false, 0, 3);
        java.util.List<com.hex.kagune.control.village.HumanVoice.Topic> friend = com.hex.kagune.control.village.HumanVoice.topics(90, false, 0, 3);
        check(!stranger.contains(com.hex.kagune.control.village.HumanVoice.Topic.SECRET) && !stranger.contains(com.hex.kagune.control.village.HumanVoice.Topic.FEAR)
            && stranger.contains(com.hex.kagune.control.village.HumanVoice.Topic.WORK) && stranger.size() <= com.hex.kagune.control.village.HumanVoice.ROOM,
            "a stranger can ask what anyone could");
        check(friend.contains(com.hex.kagune.control.village.HumanVoice.Topic.SECRET) || friend.size() == com.hex.kagune.control.village.HumanVoice.ROOM,
            "a close friend can ask more");
        int known = 0;
        for (com.hex.kagune.control.village.HumanVoice.Topic topic : com.hex.kagune.control.village.HumanVoice.Topic.values()) {
            if (topic != com.hex.kagune.control.village.HumanVoice.Topic.SECRET) {
                known |= topic.flag();
            }
        }
        check(com.hex.kagune.control.village.HumanVoice.topics(90, false, known, 5).get(0) == com.hex.kagune.control.village.HumanVoice.Topic.SECRET,
            "what isn't known yet comes first");
        check((com.hex.kagune.control.village.HumanVoice.Topic.WORK.flag() & (com.hex.kagune.control.npc.NpcBonds.MET | com.hex.kagune.control.npc.NpcBonds.ASKED
            | com.hex.kagune.control.village.HumanPersona.CONFESSED | com.hex.kagune.control.village.HumanPersona.PARTNER
            | com.hex.kagune.control.village.HumanPersona.NOT_TYPE)) == 0, "learned flags past the bond's own");
    }

    /** 2.1.4: romance for every temper (HumanRomance): flirting at every warmth, a kind no, a partner's talk, more kisses, hugs and love. */
    static void romance214() {
        java.util.regex.Pattern banned = java.util.regex.Pattern.compile("Kaneki|Ayato|Furuta|Takatsuki|Aogiri|Yoshimura");
        boolean clean = true;
        String bad = "";
        int distinct = 0;
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : com.hex.kagune.control.village.HumanPersona.Temper.values()) {
            java.util.Set<String> said = new java.util.HashSet<>();
            for (int seed = 0; seed < 24; seed++) {
                java.util.List<String> lines = new java.util.ArrayList<>();
                for (int regard : new int[] {0, 30, 70}) {
                    lines.add(com.hex.kagune.control.village.HumanRomance.flirt(temper, regard, true, seed));
                    lines.add(com.hex.kagune.control.village.HumanRomance.flirt(temper, regard, false, seed));
                }
                com.hex.kagune.control.village.HumanLife.Life life = com.hex.kagune.control.village.HumanLife.of(seed * 977L, null);
                for (com.hex.kagune.control.village.HumanRomance.Partner topic : com.hex.kagune.control.village.HumanRomance.Partner.values()) {
                    for (com.hex.kagune.control.village.HumanPersona.Taste taste : com.hex.kagune.control.village.HumanPersona.Taste.values()) {
                        lines.add(com.hex.kagune.control.village.HumanRomance.partner(temper, topic, taste, life.dream(), seed));
                    }
                }
                lines.add(com.hex.kagune.control.village.HumanRomance.kissed(temper, seed));
                lines.add(com.hex.kagune.control.village.HumanRomance.hugged(temper, seed));
                lines.add(com.hex.kagune.control.village.HumanRomance.loved(temper, seed));
                for (String line : lines) {
                    boolean ok = line != null && !line.isBlank() && !line.contains("{") && !line.contains("null") && line.length() <= 420
                        && !banned.matcher(line).find() && !line.contains("like I want");
                    if (!ok && bad.isEmpty()) {
                        bad = temper + ": " + line;
                    }
                    clean &= ok;
                    said.add(line);
                }
            }
            distinct += said.size();
            check(said.size() >= 60, temper + " has plenty to say in love: " + said.size());
        }
        check(clean, "every temper's romance is said cleanly: " + bad);
        check(com.hex.kagune.control.village.HumanRomance.flirted(com.hex.kagune.control.village.HumanPersona.Temper.FLIRTY, true)
            > com.hex.kagune.control.village.HumanRomance.flirted(com.hex.kagune.control.village.HumanPersona.Temper.STOIC, true)
            && com.hex.kagune.control.village.HumanRomance.flirted(com.hex.kagune.control.village.HumanPersona.Temper.KIND, false) == 0,
            "a flirt warms the flirty most, and one that doesn't land warms nobody");
    }

    /** 2.2.6: villagers come over by themselves (HumanApproach): how often by temper and warmth, why, and every temper's words. */
    static void approach226() {
        com.hex.kagune.control.village.HumanPersona.Temper[] tempers = com.hex.kagune.control.village.HumanPersona.Temper.values();
        int warm = com.hex.kagune.control.village.HumanPersona.WARM;
        int close = com.hex.kagune.control.village.HumanPersona.CLOSE;
        java.util.regex.Pattern banned = java.util.regex.Pattern.compile("Kaneki|Ayato|Furuta|Takatsuki|Aogiri|Yoshimura");
        boolean clean = true;
        String bad = "";
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : tempers) {
            long stranger = com.hex.kagune.control.village.HumanApproach.every(temper, warm - 1, false);
            long friend = com.hex.kagune.control.village.HumanApproach.every(temper, warm, false);
            long dear = com.hex.kagune.control.village.HumanApproach.every(temper, close, false);
            long partner = com.hex.kagune.control.village.HumanApproach.every(temper, 0, true);
            check(stranger == 0L && friend > dear && dear > partner && partner >= 1200L && friend <= 30000L,
                temper + ": never to a stranger; more often the closer (" + friend + ", " + dear + ", " + partner + ")");
            java.util.EnumMap<com.hex.kagune.control.village.HumanApproach.Why, Integer> seen = new java.util.EnumMap<>(com.hex.kagune.control.village.HumanApproach.Why.class);
            for (int roll = 0; roll < 2000; roll++) {
                com.hex.kagune.control.village.HumanApproach.Why friendly = com.hex.kagune.control.village.HumanApproach.pick(temper, close, false, false, false, false, roll);
                check(!friendly.partners(), temper + ": a friend never comes for a partner's reasons");
                com.hex.kagune.control.village.HumanApproach.Why loved = com.hex.kagune.control.village.HumanApproach.pick(temper, 0, true, false, roll % 2 == 0, false, roll);
                seen.merge(loved, 1, Integer::sum);
                seen.merge(friendly, 1, Integer::sum);
                check(com.hex.kagune.control.village.HumanApproach.pick(temper, warm, false, false, false, false, roll)
                    != com.hex.kagune.control.village.HumanApproach.Why.GIFT, temper + ": only one close to them brings a gift");
                check(com.hex.kagune.control.village.HumanApproach.pick(temper, 100, true, false, true, true, roll)
                    != com.hex.kagune.control.village.HumanApproach.Why.GIFT, temper + ": one gift a day");
                check(com.hex.kagune.control.village.HumanApproach.pick(temper, warm, false, true, false, false, roll)
                    == com.hex.kagune.control.village.HumanApproach.Why.WORRIED, temper + ": worried when they're hurt");
            }
            for (com.hex.kagune.control.village.HumanApproach.Why why : com.hex.kagune.control.village.HumanApproach.Why.values()) {
                check(why == com.hex.kagune.control.village.HumanApproach.Why.WORRIED || seen.getOrDefault(why, 0) > 0, temper + ": comes over for " + why);
                for (int roll = 0; roll < 4; roll++) {
                    String line = com.hex.kagune.control.village.HumanApproach.line(temper, why, "honey bottles", "The fog's back.", roll);
                    boolean ok = why == com.hex.kagune.control.village.HumanApproach.Why.WALK ? line.isEmpty()
                        : line.length() > 3 && !line.contains("{") && !banned.matcher(line).find() && line.length() <= 300
                        && (why != com.hex.kagune.control.village.HumanApproach.Why.GIFT || line.contains("oney bottles"))
                        && (why != com.hex.kagune.control.village.HumanApproach.Why.NEWS && why != com.hex.kagune.control.village.HumanApproach.Why.GOSSIP
                            || line.endsWith("The fog's back."));
                    if (!ok && bad.isEmpty()) {
                        bad = temper + " " + why + ": " + line;
                    }
                    clean &= ok;
                }
            }
        }
        check(clean, "every temper's words for coming over: " + bad);
        check(com.hex.kagune.control.village.HumanApproach.every(com.hex.kagune.control.village.HumanPersona.Temper.FLIRTY, close, false)
            < com.hex.kagune.control.village.HumanApproach.every(com.hex.kagune.control.village.HumanPersona.Temper.SHY, close, false),
            "the flirty come over more often than the shy");
        // The gift, as said.
        check(com.hex.kagune.control.village.HumanApproach.thing("cookies", 4).equals("cookies")
            && com.hex.kagune.control.village.HumanApproach.thing("iron ingot", 1).equals("an iron ingot")
            && com.hex.kagune.control.village.HumanApproach.thing("lantern", 1).equals("a lantern")
            && com.hex.kagune.control.village.HumanApproach.thing("bread", 2).equals("bread")
            && com.hex.kagune.control.village.HumanApproach.thing("honeycomb", 1).equals("honeycomb")
            && com.hex.kagune.control.village.HumanApproach.line(com.hex.kagune.control.village.HumanPersona.Temper.STOIC,
                com.hex.kagune.control.village.HumanApproach.Why.GIFT, "an iron ingot", "", 0).startsWith("An iron ingot"),
            "a gift said rightly (\"an iron ingot\", \"cookies\", \"bread\")");
        check(com.hex.kagune.control.village.HumanApproach.gives(12) == 4 && com.hex.kagune.control.village.HumanApproach.gives(1) == 1,
            "a third of a ware's lot, at least one");
        // 2.3.4: a human partner's ring and a kiss left hanging, every temper; married past every other flag of a human's bond.
        int topicFlags = 0;
        for (com.hex.kagune.control.village.HumanVoice.Topic topic : com.hex.kagune.control.village.HumanVoice.Topic.values()) {
            topicFlags |= topic.flag();
        }
        check((com.hex.kagune.control.village.HumanPersona.MARRIED & (topicFlags | com.hex.kagune.control.village.HumanPersona.CONFESSED
            | com.hex.kagune.control.village.HumanPersona.PARTNER | com.hex.kagune.control.village.HumanPersona.NOT_TYPE
            | com.hex.kagune.control.village.HumanPersona.LEGACY | 7)) == 0, "married: a flag of its own");
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : tempers) {
            for (int roll = 0; roll < 4; roll++) {
                check(com.hex.kagune.control.village.HumanRomance.proposed(temper, roll).length() > 3
                    && com.hex.kagune.control.village.HumanRomance.hanging(temper, roll).length() > 3, temper + ": a ring's answer, a kiss left hanging");
            }
        }
        // Every trade's wares can be given, and said.
        for (com.hex.kagune.control.village.HumanLife.Job job : com.hex.kagune.control.village.HumanLife.Job.values()) {
            check(!job.wares.isEmpty(), job + ": something to give");
            for (com.hex.kagune.control.village.VillageShop.Offer ware : job.wares) {
                String thing = com.hex.kagune.control.village.HumanApproach.thing(ware.name(), com.hex.kagune.control.village.HumanApproach.gives(ware.count()));
                check(!thing.isBlank() && !thing.startsWith("a a") && !thing.startsWith("a e") && !thing.startsWith("a i"), job + ": " + thing);
            }
        }
    }

    /** 2.4.7: villagers' favours: what they ask, kept with the player, every temper's words. */
    static void errands247() {
        java.util.EnumMap<com.hex.kagune.control.village.HumanErrand.Kind, Integer> kinds = new java.util.EnumMap<>(com.hex.kagune.control.village.HumanErrand.Kind.class);
        for (long roll = 0; roll < 4000; roll++) {
            com.hex.kagune.control.village.HumanErrand.Errand errand = com.hex.kagune.control.village.HumanErrand.roll(roll * 7919L + (roll << 9), "Hana");
            kinds.merge(errand.kind(), 1, Integer::sum);
            check(errand.count() >= 1 && errand.emeralds() >= 2 && errand.emeralds() <= 8, "a favour pays in emeralds: " + errand);
            if (errand.kind() == com.hex.kagune.control.village.HumanErrand.Kind.FETCH) {
                check(errand.item().startsWith("minecraft:") && !errand.name().isEmpty(), "a thing to fetch: " + errand);
            }
            String line = com.hex.kagune.control.village.HumanErrand.save("u-1", "Mai", errand, 1, 500L);
            com.hex.kagune.control.village.HumanErrand.Open back = com.hex.kagune.control.village.HumanErrand.load(line);
            check(back != null && back.errand().equals(errand) && back.done() == 1 && back.until() == 500L && back.giverName().equals("Mai"),
                "kept with the player and back: " + line);
        }
        check(kinds.size() == 3, "fetching, hunting and visiting all come up: " + kinds);
        check(com.hex.kagune.control.village.HumanErrand.roll(5L, null).kind() != com.hex.kagune.control.village.HumanErrand.Kind.VISIT,
            "no visit with no neighbour");
        check(com.hex.kagune.control.village.HumanErrand.load("") == null && com.hex.kagune.control.village.HumanErrand.load("junk") == null, "none, or junk, is none");
        java.util.regex.Pattern banned = java.util.regex.Pattern.compile("Kaneki|Ayato|Furuta|Takatsuki|Aogiri|Yoshimura");
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : com.hex.kagune.control.village.HumanPersona.Temper.values()) {
            for (com.hex.kagune.control.village.HumanErrand.Kind kind : com.hex.kagune.control.village.HumanErrand.Kind.values()) {
                for (int seed = 0; seed < 4; seed++) {
                    com.hex.kagune.control.village.HumanErrand.Errand errand = new com.hex.kagune.control.village.HumanErrand.Errand(kind, "minecraft:coal",
                        "coal", 6, "Hana", 3);
                    String ask = com.hex.kagune.control.village.HumanErrand.ask(temper, errand, seed);
                    check(ask.length() > 10 && !ask.contains("{") && !banned.matcher(ask).find(), temper + " " + kind + ": " + ask);
                }
            }
            check(com.hex.kagune.control.village.HumanErrand.thanks(temper, 1).length() > 3 && com.hex.kagune.control.village.HumanErrand.dropped(temper, 1).length() > 3,
                temper + ": thanks and a let-down");
        }
        check(com.hex.kagune.control.village.HumanErrand.visited("Mai", 2).contains("Mai"), "the neighbour knows who sent them");
        check(com.hex.kagune.control.village.HumanErrand.OFFER_EVERY >= 6000L && com.hex.kagune.control.village.HumanErrand.CHANCE <= 0.05, "rare");
    }

    static void village() {
        // 2.0.13: trading with village humans: seven wares each (2.0.14: a staple, two more foods, two farming goods, two basic goods), never meat or fish; every
        // non-meat ingredient the cookbook needs is sold by someone; what they buy, and the page's words.
        {
            boolean six = true;
            boolean noMeat = true;
            java.util.Set<String> sold = new java.util.HashSet<>();
            java.util.Set<String> staples = new java.util.HashSet<>();
            for (com.hex.kagune.control.village.VillageShop.Offer o : com.hex.kagune.control.village.VillageShop.STAPLES) {
                staples.add(o.item());
            }
            for (long seed = 0; seed < 600; seed++) {
                java.util.List<com.hex.kagune.control.village.VillageShop.Offer> stock = com.hex.kagune.control.village.VillageShop.stock(seed * 7919L);
                com.hex.kagune.control.village.HumanLife.Job job = com.hex.kagune.control.village.HumanLife.of(seed * 7919L, null).job();
                six &= stock.size() == 7 && stock.stream().map(com.hex.kagune.control.village.VillageShop.Offer::item).distinct().count() == 7
                    && stock.get(0).equals(job.wares.get(0)) && stock.equals(com.hex.kagune.control.village.VillageShop.stock(seed * 7919L))
                    && stock.subList(0, com.hex.kagune.control.village.VillageShop.TRADE_OFFERS).stream().allMatch(job.wares::contains)
                    && stock.size() <= com.hex.kagune.control.npc.NpcTalk.Choice.SAYS.length - 1;
                for (com.hex.kagune.control.village.VillageShop.Offer o : stock) {
                    sold.add(o.item());
                    noMeat &= !java.util.regex.Pattern.compile("beef|porkchop|chicken|mutton|rabbit|cod|salmon|tropical_fish|pufferfish|hexkagune:|flesh")
                        .matcher(o.item()).find() && o.count() > 0 && o.price() > 0 && o.count() <= 64;
                }
            }
            check(six, "each sells seven things for life (2.1.3: five of its trade's own, its trade's first first), none twice, one slot left to sell");
            for (com.hex.kagune.control.village.HumanLife.Job job : com.hex.kagune.control.village.HumanLife.Job.values()) {
                check(job.wares.size() >= com.hex.kagune.control.village.VillageShop.TRADE_OFFERS + 1
                    && job.wares.stream().map(com.hex.kagune.control.village.VillageShop.Offer::item).distinct().count() == job.wares.size()
                    && job.wares.stream().allMatch(o -> o.item().matches("minecraft:[a-z_]+")), job + ": enough wares of its own, none twice");
            }
            check(noMeat, "no meat, no fish, sensible lots");
            java.util.Set<String> needed = new java.util.TreeSet<>();
            for (com.hex.kagune.control.kitchen.Dish dish : com.hex.kagune.control.kitchen.Menu.DISHES) {
                if (dish.recipe() != null) {
                    for (String item : dish.recipe().ingredients()) {
                        if (item.startsWith("minecraft:") && !item.equals("minecraft:cod")) {
                            needed.add(item);
                        }
                    }
                }
            }
            needed.removeAll(sold);
            check(needed.isEmpty(), "every non-meat ingredient of the cookbook is sold in some village: missing " + needed);
            com.hex.kagune.control.village.VillageShop.Offer wheat = com.hex.kagune.control.village.VillageShop.buys("minecraft:wheat");
            check(wheat != null && com.hex.kagune.control.village.VillageShop.buys("minecraft:diamond") == null
                && com.hex.kagune.control.village.VillageShop.lots(wheat, 45) == 2
                && com.hex.kagune.control.village.VillageShop.sellLabel(wheat, 45).equals("Sell 40 wheat: 2 emeralds")
                && com.hex.kagune.control.village.VillageShop.STAPLES[0].label().equals("12 wheat: 1 emerald"), "what it buys and how the page says it");
            boolean said = true;
            for (int r = 0; r < 12; r++) {
                for (String line : new String[] {com.hex.kagune.control.village.VillageShop.hello(r), com.hex.kagune.control.village.VillageShop.done(r),
                    com.hex.kagune.control.village.VillageShop.short_(wheat, 0, r), com.hex.kagune.control.village.VillageShop.short_(wheat, 3, r),
                    com.hex.kagune.control.village.VillageShop.sold(wheat, r), com.hex.kagune.control.village.VillageShop.bought(wheat, 2, r),
                    com.hex.kagune.control.village.VillageShop.purse(0), com.hex.kagune.control.village.VillageShop.purse(5)}) {
                    said &= line != null && !line.isBlank() && !line.contains("null");
                }
            }
            check(said, "the trade's words are all there");
        }
        // 1.3.75: the villagers' own skins exist, are painted for a player's body, and every new look maps to one of them.
        boolean skins = com.hex.kagune.control.village.HumanSkins.ALL.length >= 8;
        for (com.hex.kagune.control.village.HumanSkins.Skin s : com.hex.kagune.control.village.HumanSkins.ALL) {
            skins &= new java.io.File("src/main/resources/assets/hexkagune/" + s.path()).isFile();
        }
        for (int roll = -50; roll < 50; roll++) {
            int look = com.hex.kagune.control.npc.HumanRules.pick(roll * 7919);
            skins &= look >= com.hex.kagune.control.npc.HumanRules.CUTE && "hexkagune".equals(com.hex.kagune.control.npc.HumanRules.namespace(look))
                && com.hex.kagune.control.npc.HumanRules.skin(look).startsWith("textures/entity/human/human_");
        }
        skins &= "minecraft".equals(com.hex.kagune.control.npc.HumanRules.namespace(3))
            && com.hex.kagune.control.npc.HumanRules.skin(3).startsWith("textures/entity/player/");
        check(skins, "the villagers' own skins: files there, new looks map to them, old looks keep vanilla's");
        // 1.3.79: children's looks: every version's texture there, its own look first, the nearest look picked by hair and eyes.
        int versions = 0;
        boolean looked = true;
        for (com.hex.kagune.control.family.ChildSkins.Skin s : com.hex.kagune.control.family.ChildSkins.ALL) {
            com.hex.kagune.control.family.ChildLooks.Look[] looks = com.hex.kagune.control.family.ChildLooks.of(s.key());
            looked &= looks.length >= 1 && looks[0].texture().equals(s.key());
            for (com.hex.kagune.control.family.ChildLooks.Look look : looks) {
                looked &= new java.io.File("src/main/resources/assets/hexkagune/textures/entity/child/skins/" + look.texture() + ".png").isFile();
                looked &= com.hex.kagune.control.family.ChildLooks.pick(s.key(), look.hair(), look.eyes()) == look;
            }
            versions += looks.length - 1;
        }
        check(looked && versions >= 100, "children's looks: " + versions + " versions with other hair and eyes, each picked by its own colours");
        // 2.0: a village's people never share a look or a name (16 of them: two in each of its 8 houses), whatever village it is.
        boolean unique = com.hex.kagune.control.village.HumanSkins.ALL.length >= 16;
        java.util.Set<Integer> firsts = new java.util.HashSet<>();
        for (int v = 0; v < 200; v++) {
            int mx = v * 977 - 5000;
            int mz = v * -613 + 300;
            java.util.Set<Integer> looks = new java.util.HashSet<>();
            java.util.Set<Integer> names = new java.util.HashSet<>();
            for (int slot = 0; slot < 16; slot++) {
                int look = com.hex.kagune.control.npc.HumanRules.villageLook(mx, mz, slot);
                unique &= look >= com.hex.kagune.control.npc.HumanRules.CUTE && looks.add(look);
                unique &= names.add(com.hex.kagune.control.npc.HumanRules.villageName(mx, mz, slot, com.hex.kagune.control.village.HumanPersona.names()));
            }
            firsts.add(com.hex.kagune.control.npc.HumanRules.villageLook(mx, mz, 0));
        }
        check(unique && firsts.size() >= 10, "no two of a village's 16 people share a look or a name; villages shuffle differently");
        // 1.3.76: blinks: every skin that blinks has its shut eyes; villagers blink with their child skin's; vanilla's looks have none.
        int blinking = 0;
        boolean shut = com.hex.kagune.control.npc.HumanRules.closed(3) == null;
        for (com.hex.kagune.control.family.ChildSkins.Skin s : com.hex.kagune.control.family.ChildSkins.ALL) {
            if (com.hex.kagune.control.family.ChildBlinks.has(s.key())) {
                blinking++;
                shut &= new java.io.File("src/main/resources/assets/hexkagune/" + com.hex.kagune.control.family.ChildBlinks.closed(s.key())).isFile();
            }
        }
        for (int i = 0; i < com.hex.kagune.control.village.HumanSkins.ALL.length; i++) {
            String path = com.hex.kagune.control.npc.HumanRules.closed(com.hex.kagune.control.npc.HumanRules.CUTE + i);
            shut &= (path != null) == com.hex.kagune.control.family.ChildBlinks.has(com.hex.kagune.control.village.HumanSkins.ALL[i].child());
        }
        check(shut && blinking >= 25, "shut eyes for blinks: " + blinking + " child skins, the villagers' from theirs");
        int[][] streets = com.hex.kagune.control.village.VillageMap.STREETS;
        int[][] route = com.hex.kagune.control.village.VillageMap.ROUTE;
        int[][] homes = com.hex.kagune.control.village.VillageMap.HOMES;
        int perimeter = com.hex.kagune.control.village.VillageMap.PERIMETER;
        check(streets.length > 100 && route.length >= 8 && homes.length >= 6, "a village of streets, a patrol loop and homes ("
            + streets.length + ", " + route.length + ", " + homes.length + ")");
        boolean in = true;
        for (int[][] set : new int[][][] {streets, route, homes}) {
            for (int[] p : set) {
                in &= com.hex.kagune.control.village.VillageRules.inside(p[0], p[2], perimeter) && p.length == 3;
            }
        }
        check(in, "every street spot, patrol spot and home within the perimeter");
        boolean ground = true;
        for (int[] p : route) {
            ground &= p[1] >= -4 && p[1] <= 4;
        }
        check(ground, "the patrol stays near the ground (never a roof)");
        check(homes.length == 13 && perimeter <= 128, "13 homes (26 villagers, 26 looks), the village within 128 of its middle");
        // 2.0.4: the world's own ground cleared over the village, from just over its own highest block, never below its ground.
        boolean clears = com.hex.kagune.control.village.VillageTops.clearFrom(-1, 5) == -1
            && com.hex.kagune.control.village.VillageTops.clearFrom(5, com.hex.kagune.control.village.VillageTops.SIZE) == -1;
        for (int x = 0; x < com.hex.kagune.control.village.VillageTops.SIZE; x += 7) {
            for (int z = 0; z < com.hex.kagune.control.village.VillageTops.SIZE; z += 7) {
                int from = com.hex.kagune.control.village.VillageTops.clearFrom(x, z);
                clears &= from > com.hex.kagune.control.village.VillageTops.GROUND && from > com.hex.kagune.control.village.VillageTops.top(x, z);
            }
        }
        check(clears && com.hex.kagune.control.village.VillageTops.MIDDLE == 102 && com.hex.kagune.control.village.VillageTops.top(128, 128) >= 0,
            "the world's ground cleared over the village, never into it");
        // 2.0.1: only on dry, fairly flat land.
        int n = com.hex.kagune.control.village.VillageGround.SAMPLES.length;
        int[] flat = new int[n];
        java.util.Arrays.fill(flat, 70);
        int[] wet = flat.clone();
        wet[12] = 72;
        int[] hill = flat.clone();
        hill[3] = 70 + com.hex.kagune.control.village.VillageGround.FLAT + 1;
        // 2.0.2: a little water at the rim (sample 0 is a corner, 1 and 2 on the edge): a river or a pond, never the sea.
        int[] rim = flat.clone();
        rim[0] = 72;
        rim[1] = 72;
        int[] wider = rim.clone();
        wider[2] = 72;
        int[] low = flat.clone();
        low[0] = 40;
        check(n == 25 && com.hex.kagune.control.village.VillageGround.suits(flat, flat)
            && !com.hex.kagune.control.village.VillageGround.suits(wet, flat) && !com.hex.kagune.control.village.VillageGround.suits(hill, hill)
            && com.hex.kagune.control.village.VillageGround.suits(rim, low) && !com.hex.kagune.control.village.VillageGround.suits(wider, flat)
            && !com.hex.kagune.control.village.VillageGround.suits(new int[0], new int[0]), "a village only on dry, fairly flat land");
        for (int i = 0; i < route.length; i++) {
            int[] a = route[i];
            int[] b = route[(i + 1) % route.length];
            check(Math.hypot(a[0] - b[0], a[2] - b[2]) < 24.0, "the patrol's steps are short (" + i + ")");
        }
        // Turns: as the game turns a template (CLOCKWISE_90: x, z to -z, x), and a yaw-0 body's yaw with it (+90 a quarter turn).
        check(com.hex.kagune.control.village.VillageRules.turns(0.0F) == 0 && com.hex.kagune.control.village.VillageRules.turns(90.0F) == 1
            && com.hex.kagune.control.village.VillageRules.turns(180.0F) == 2 && com.hex.kagune.control.village.VillageRules.turns(-90.0F) == 3
            && com.hex.kagune.control.village.VillageRules.turns(269.0F) == 3 && com.hex.kagune.control.village.VillageRules.turns(-180.0F) == 2,
            "the village's turn read off a body placed facing south");
        int[] q = com.hex.kagune.control.village.VillageRules.turn(3, 5, 1);
        check(q[0] == -5 && q[1] == 3, "a quarter turn clockwise: x, z to -z, x");
        boolean back = true;
        for (int t = 0; t < 4; t++) {
            int[] r = com.hex.kagune.control.village.VillageRules.unturn(com.hex.kagune.control.village.VillageRules.turn(7, -2, t)[0],
                com.hex.kagune.control.village.VillageRules.turn(7, -2, t)[1], t);
            back &= r[0] == 7 && r[1] == -2;
        }
        check(back, "turned and turned back, the same spot");
        double[] c = com.hex.kagune.control.village.VillageRules.clamp(200.0, -3.0, perimeter, 2.0);
        check(c[0] == perimeter - 2.0 && c[1] == -3.0 && !com.hex.kagune.control.village.VillageRules.inside(perimeter + 1.0, 0.0, perimeter),
            "running, never past the perimeter");
        check(com.hex.kagune.control.village.VillageRules.next(route.length - 1, route.length, true) == 0
            && com.hex.kagune.control.village.VillageRules.next(0, route.length, false) == route.length - 1
            && com.hex.kagune.control.village.VillageRules.nearest(route, route[3][0], route[3][2]) == 3, "the loop walked round, either way");
        // 1.3.72: trading in the streets, and a word in passing.
        java.util.regex.Pattern banned = java.util.regex.Pattern.compile("Kaneki|Ayato|Furuta|Takatsuki|Aogiri|Yoshimura|\\{|\\}|null");
        java.util.Set<String> said = new java.util.HashSet<>();
        boolean trades = true;
        java.util.Set<String> known = java.util.Set.of("minecraft:bread", "minecraft:apple", "minecraft:carrot", "minecraft:baked_potato",
            "minecraft:cookie", "minecraft:pumpkin_pie", "minecraft:cod", "minecraft:sweet_berries", "minecraft:honey_bottle", "minecraft:poppy",
            "minecraft:paper", "minecraft:candle", "minecraft:emerald");
        for (com.hex.kagune.control.village.VillageTalk.Goods goods : com.hex.kagune.control.village.VillageTalk.GOODS) {
            trades &= known.contains(goods.item());
            for (int seed = 0; seed < 600; seed += 7) {
                java.util.List<com.hex.kagune.control.village.VillageTalk.Line> lines = com.hex.kagune.control.village.VillageTalk.trade(goods, seed);
                int hands = 0;
                for (com.hex.kagune.control.village.VillageTalk.Line line : lines) {
                    hands += line.hands() ? 1 : 0;
                    trades &= !line.text().isBlank() && !banned.matcher(line.text()).find();
                    said.add(line.text());
                }
                trades &= lines.size() >= 4 && hands == 1 && lines.get(0).seller() && !lines.get(1).seller() && !lines.get(3).seller()
                    && lines.get(3).hands() && lines.get(0).text().toLowerCase(java.util.Locale.ROOT).contains(goods.name().split(" ")[0]);
            }
        }
        check(trades && known.contains(com.hex.kagune.control.village.VillageTalk.COIN), "every trade: goods 1.20.1 has, the seller first, "
            + "the coin and the goods changing hands once, the goods named, nothing that must stay out");
        for (int seed = 0; seed < 40; seed++) {
            String greet = com.hex.kagune.control.village.VillageTalk.greet(seed);
            String dusk = com.hex.kagune.control.village.VillageTalk.dusk(seed);
            check(!greet.isBlank() && !dusk.isBlank() && !banned.matcher(greet + dusk).find(), "a word in passing, and at dusk");
            said.add(greet);
            said.add(dusk);
        }
        check(said.size() > 100, "plenty of different words in the streets (" + said.size() + ")");
        // 1.3.73: talking with them: a name and a temper each, every word in its temper's way.
        java.util.EnumSet<com.hex.kagune.control.village.HumanPersona.Temper> tempers = java.util.EnumSet.noneOf(com.hex.kagune.control.village.HumanPersona.Temper.class);
        java.util.Set<String> names = new java.util.HashSet<>();
        // 2.0.36: a villager placed before keeps one of the first eight (by its seed); one placed since rolls any of them.
        java.util.EnumSet<com.hex.kagune.control.village.HumanPersona.Temper> rolled = java.util.EnumSet.noneOf(com.hex.kagune.control.village.HumanPersona.Temper.class);
        boolean legacy = true;
        for (long seed = 0; seed < 4000; seed += 13) {
            com.hex.kagune.control.village.HumanPersona.Temper old = com.hex.kagune.control.village.HumanPersona.temper(seed * 7919L);
            legacy &= old.ordinal() < com.hex.kagune.control.village.HumanPersona.LEGACY;
            tempers.add(old);
            rolled.add(com.hex.kagune.control.village.HumanPersona.rolled(seed * 7919L));
            names.add(com.hex.kagune.control.village.HumanPersona.name(seed * 104729L));
        }
        check(legacy && tempers.size() == com.hex.kagune.control.village.HumanPersona.LEGACY && names.size() > 12,
            "every old temper and plenty of names turn up, a villager placed before keeping its own (" + tempers.size() + ", " + names.size() + ")");
        check(rolled.size() == com.hex.kagune.control.village.HumanPersona.Temper.values().length && com.hex.kagune.control.village.HumanPersona.Temper.values().length == 12,
            "2.0.36: a new villager may have any of the twelve tempers (" + rolled.size() + ")");
        for (String name : names) {
            check(!banned.matcher(name).find() && !java.util.Set.of("Eto", "Touka", "Ken").contains(name), "a name of its own: " + name);
        }
        java.util.Set<String> talk = new java.util.HashSet<>();
        boolean spoken = true;
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : com.hex.kagune.control.village.HumanPersona.Temper.values()) {
            spoken &= com.hex.kagune.control.village.HumanPersona.liking(temper) > 0;
            for (int regard : new int[] {-50, 0, 30, 80}) {
                for (int seed = 0; seed < 6; seed++) {
                    for (String line : new String[] {com.hex.kagune.control.village.HumanPersona.hello(temper, regard, seed),
                        com.hex.kagune.control.village.HumanPersona.chat(temper, seed), com.hex.kagune.control.village.HumanPersona.about(temper, seed),
                        com.hex.kagune.control.village.HumanPersona.news(seed), com.hex.kagune.control.village.HumanPersona.compliment(temper, regard, seed),
                        com.hex.kagune.control.village.HumanPersona.bye(temper, regard, seed)}) {
                        spoken &= line != null && !line.isBlank() && !banned.matcher(line).find();
                        talk.add(line);
                    }
                }
            }
        }
        check(spoken, "every temper says something to everything, warm or not, and likes a compliment");
        check(talk.size() >= 120, "plenty of different words when talked to (" + talk.size() + ")");
        check(!com.hex.kagune.control.village.HumanPersona.hello(com.hex.kagune.control.village.HumanPersona.Temper.SHY, 0, 1)
            .equals(com.hex.kagune.control.village.HumanPersona.hello(com.hex.kagune.control.village.HumanPersona.Temper.SHY, 90, 1)),
            "a friend is greeted otherwise than a stranger");
        // 1.3.77: gestures: every temper moves with everything it says, real moves (no props), coded and decoded whole.
        boolean moves = true;
        java.util.Set<Integer> kinds = new java.util.HashSet<>();
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : com.hex.kagune.control.village.HumanPersona.Temper.values()) {
            for (int seed = 0; seed < 12; seed++) {
                java.util.List<Integer> codes = new java.util.ArrayList<>(java.util.List.of(
                    com.hex.kagune.control.village.HumanGestures.hello(temper, false, seed), com.hex.kagune.control.village.HumanGestures.hello(temper, true, seed),
                    com.hex.kagune.control.village.HumanGestures.chat(temper, seed), com.hex.kagune.control.village.HumanGestures.about(temper, seed),
                    com.hex.kagune.control.village.HumanGestures.news(seed), com.hex.kagune.control.village.HumanGestures.compliment(temper, seed),
                    com.hex.kagune.control.village.HumanGestures.yes(temper), com.hex.kagune.control.village.HumanGestures.love(temper, seed),
                    com.hex.kagune.control.village.HumanGestures.bye(temper, seed), com.hex.kagune.control.village.HumanGestures.greet(seed),
                    com.hex.kagune.control.village.HumanGestures.dusk(seed), com.hex.kagune.control.village.HumanGestures.idle(temper, seed)));
                for (com.hex.kagune.control.village.HumanPersona.Answer answer : com.hex.kagune.control.village.HumanPersona.Answer.values()) {
                    codes.add(com.hex.kagune.control.village.HumanGestures.confessed(answer));
                }
                for (int code : codes) {
                    boolean cue = com.hex.kagune.control.village.HumanGestures.cue(code);
                    com.hex.kagune.control.npc.EtoGesture g = com.hex.kagune.control.village.HumanGestures.gesture(code);
                    moves &= code > 0 && com.hex.kagune.control.village.HumanGestures.ticks(code) > 0
                        && (cue ? com.hex.kagune.control.village.HumanGestures.childCue(code) != com.hex.kagune.control.family.ChildCue.NONE
                            : g != com.hex.kagune.control.npc.EtoGesture.NONE && g.mainHand == null && g.offHand == null);
                    int packed = com.hex.kagune.control.village.HumanGestures.pack(code, com.hex.kagune.control.village.HumanGestures.style(temper), seed * 977 + 5);
                    moves &= com.hex.kagune.control.village.HumanGestures.code(packed) == code
                        && com.hex.kagune.control.village.HumanGestures.style(packed) == com.hex.kagune.control.village.HumanGestures.style(temper)
                        && com.hex.kagune.control.village.HumanGestures.count(packed) == seed * 977 + 5;
                    kinds.add(code);
                }
                for (String line : new String[] {com.hex.kagune.control.village.HumanPersona.kissed(temper, seed),
                    com.hex.kagune.control.village.HumanPersona.hugged(temper, seed)}) {
                    moves &= line != null && !line.isBlank() && !banned.matcher(line).find();
                }
            }
        }
        int kiss = com.hex.kagune.control.village.HumanGestures.of(com.hex.kagune.control.npc.EtoGesture.KISS);
        check(moves && kinds.size() >= 25, "villagers move with their words: " + kinds.size() + " different moves, none with a prop");
        check(com.hex.kagune.control.village.HumanGestures.ticks(kiss) == com.hex.kagune.control.village.HumanGestures.HELD
            && com.hex.kagune.control.village.HumanGestures.eyesClosed(kiss)
            && com.hex.kagune.control.village.HumanGestures.blush(com.hex.kagune.control.village.HumanGestures.of(com.hex.kagune.control.npc.EtoGesture.KISSED))
            && com.hex.kagune.control.village.HumanGestures.still(kiss), "a kiss: held a while, eyes shut, standing still; then a blush");
        boolean pink = true;
        for (int i = 0; i < com.hex.kagune.control.village.HumanSkins.ALL.length; i++) {
            String path = com.hex.kagune.control.npc.HumanRules.blush(com.hex.kagune.control.npc.HumanRules.CUTE + i);
            pink &= path == null || new java.io.File("src/main/resources/assets/hexkagune/" + path).isFile();
        }
        check(pink && com.hex.kagune.control.npc.HumanRules.blush(2) == null, "villagers' blushes are there");
        // 1.3.78: dates: hand in hand, standing so the two hands meet; something to say for each moment, by every temper.
        boolean dated = true;
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : com.hex.kagune.control.village.HumanPersona.Temper.values()) {
            for (int seed = 0; seed < 6; seed++) {
                for (String line : new String[] {com.hex.kagune.control.village.HumanPersona.walk(temper, seed),
                    com.hex.kagune.control.village.HumanPersona.stroll(temper, seed), com.hex.kagune.control.village.HumanPersona.letGo(temper, seed),
                    com.hex.kagune.control.village.HumanPersona.edge(temper, seed)}) {
                    dated &= line != null && !line.isBlank() && !banned.matcher(line).find();
                }
            }
        }
        double apart = com.hex.kagune.control.village.HumanHand.out(false, false);
        double[] right = com.hex.kagune.control.village.HumanHand.offset(0.0F, apart);
        check(dated && apart > 1.0 && apart < 1.3 && com.hex.kagune.control.village.HumanHand.out(true, true) < apart
            && Math.abs(right[0] + apart) < 1.0E-9 && Math.abs(right[1]) < 1.0E-9, "a date: hand in hand at its partner's right, a word for each moment");
        // 1.3.74: romance: a ghoul's confession first (may take convincing), then together.
        com.hex.kagune.control.village.HumanPersona.Temper kind = com.hex.kagune.control.village.HumanPersona.Temper.KIND;
        com.hex.kagune.control.village.HumanPersona.Temper nervous = com.hex.kagune.control.village.HumanPersona.Temper.NERVOUS;
        int[] yes = new int[2];
        boolean shape = true;
        for (int i = 0; i < 1000; i++) {
            double roll = i / 1000.0;
            com.hex.kagune.control.village.HumanPersona.Answer first = com.hex.kagune.control.village.HumanPersona.confession(kind, 60, false, roll);
            com.hex.kagune.control.village.HumanPersona.Answer plea = com.hex.kagune.control.village.HumanPersona.confession(nervous, 60, true, roll);
            shape &= first != com.hex.kagune.control.village.HumanPersona.Answer.NO && plea != com.hex.kagune.control.village.HumanPersona.Answer.DOUBT;
            yes[0] += com.hex.kagune.control.village.HumanPersona.confession(kind, 60, false, roll) == com.hex.kagune.control.village.HumanPersona.Answer.YES ? 1 : 0;
            yes[1] += com.hex.kagune.control.village.HumanPersona.confession(nervous, 60, false, roll) == com.hex.kagune.control.village.HumanPersona.Answer.YES ? 1 : 0;
        }
        check(shape, "a first try never a flat no (a doubt: they may plead), a plea never another doubt");
        check(yes[0] > yes[1] * 2 && yes[1] > 0, "the kind believe a ghoul far sooner than the nervous (" + yes[0] + " to " + yes[1] + " in 1000)");
        check(com.hex.kagune.control.village.HumanPersona.confession(nervous, 100, true, 0.8) == com.hex.kagune.control.village.HumanPersona.Answer.YES,
            "close enough and pleading, even the nervous may believe");
        check(com.hex.kagune.control.village.HumanPersona.CONFESSED != com.hex.kagune.control.village.HumanPersona.PARTNER
            && (com.hex.kagune.control.village.HumanPersona.CONFESSED & 15) == 0 && (com.hex.kagune.control.village.HumanPersona.PARTNER & 15) == 0,
            "the romance's flags clear of the bond's own (met, befriended, asked, ally)");
        boolean romance = true;
        for (com.hex.kagune.control.village.HumanPersona.Temper temper : com.hex.kagune.control.village.HumanPersona.Temper.values()) {
            for (int seed = 0; seed < 4; seed++) {
                for (com.hex.kagune.control.village.HumanPersona.Answer answer : com.hex.kagune.control.village.HumanPersona.Answer.values()) {
                    String line = com.hex.kagune.control.village.HumanPersona.confessed(temper, answer, seed);
                    romance &= !line.isBlank() && !banned.matcher(line).find();
                }
                for (String line : new String[] {com.hex.kagune.control.village.HumanPersona.yes(temper, seed),
                    com.hex.kagune.control.village.HumanPersona.love(temper, seed), com.hex.kagune.control.village.HumanPersona.hello(temper, 90, true, seed)}) {
                    romance &= !line.isBlank() && !banned.matcher(line).find();
                }
            }
        }
        check(romance, "every temper has its words for a confession (yes, doubt, no), a yes, love, and a partner's hello");
    }

    /**
     * 4.1.1 (the owner: "bug, make sure NONE of the structures spawns in water or collides with water bodies, the mansion is currently happily
     * resting at the very bottom of the ocean..."): every place on dry land, none in or beside water; the world's one hideout at the first
     * dry offer, the same however it is asked.
     */
    static void dryLand() {
        int margin = com.hex.kagune.control.village.DryLand.MARGIN;
        int step = com.hex.kagune.control.village.DryLand.STEP;
        int fine = com.hex.kagune.control.village.DryLand.BIOME_STEP;
        check(step <= 16 && fine <= 4 && margin >= 8, "the ground looked at every 16 blocks, its biome every 4 (the biomes' own grain), 8 out round it");
        // The columns looked at: all over the footprint and its margin, never further apart than their step, the coarse ones first.
        int[][] sizes = {{1, 1}, {7, 5}, {33, 17}, {77, 130}, {272, 263}, {73, 73}};
        for (int[] size : sizes) {
            for (int every : new int[] {step, fine}) {
                int minX = -37;
                int minZ = 1201;
                int[][] columns = com.hex.kagune.control.village.DryLand.columns(minX, minZ, size[0], size[1], every);
                java.util.TreeSet<Integer> xs = new java.util.TreeSet<>();
                java.util.TreeSet<Integer> zs = new java.util.TreeSet<>();
                java.util.Set<Long> seen = new java.util.HashSet<>();
                boolean inside = true;
                for (int[] c : columns) {
                    xs.add(c[0]);
                    zs.add(c[1]);
                    seen.add(((long)c[0] << 32) ^ (c[1] & 0xffffffffL));
                    inside &= c[0] >= minX - margin && c[0] <= minX + size[0] - 1 + margin && c[1] >= minZ - margin && c[1] <= minZ + size[1] - 1 + margin;
                }
                boolean close = true;
                for (java.util.TreeSet<Integer> line : List.of(xs, zs)) {
                    Integer last = null;
                    for (int at : line) {
                        close &= last == null || at - last <= every;
                        last = at;
                    }
                }
                check(inside && close && seen.size() == columns.length && columns.length == xs.size() * zs.size()
                    && xs.first() == minX - margin && xs.last() == minX + size[0] - 1 + margin && zs.first() == minZ - margin
                    && zs.last() == minZ + size[1] - 1 + margin && columns[0][0] == minX - margin && columns[0][1] == minZ - margin,
                    "every " + every + " blocks over " + size[0] + " x " + size[1] + " and 8 out round it, from a corner");
            }
        }
        // A lake 16 blocks across, or a wet biome's patch (a quart, 4 x 4), anywhere under a place or at its edge: not there. Further
        // off, it may stand.
        int minX = 100;
        int minZ = -50;
        int sizeX = 37;
        int sizeZ = 29;
        boolean lakes = true;
        boolean patches = true;
        int lakeTries = 0;
        for (int lx = minX - margin - 24; lx <= minX + sizeX + margin + 8; lx++) {
            for (int lz = minZ - margin - 24; lz <= minZ + sizeZ + margin + 8; lz++) {
                int ax = lx;
                int az = lz;
                boolean touches = ax + 15 >= minX - margin && ax <= minX + sizeX - 1 + margin && az + 15 >= minZ - margin && az <= minZ + sizeZ - 1 + margin;
                com.hex.kagune.control.village.DryLand.Ground lake = ground((x, z) -> x >= ax && x < ax + 16 && z >= az && z < az + 16, (x, z) -> false);
                lakes &= com.hex.kagune.control.village.DryLand.dry(lake, 70, minX, minZ, sizeX, sizeZ) == !touches;
                // A biome's patch lies on its grain (4 x 4, from a multiple of 4).
                int qx = Math.floorDiv(lx, 4) * 4;
                int qz = Math.floorDiv(lz, 4) * 4;
                boolean near = qx + 3 >= minX - margin && qx <= minX + sizeX - 1 + margin && qz + 3 >= minZ - margin && qz <= minZ + sizeZ - 1 + margin;
                com.hex.kagune.control.village.DryLand.Ground patch = ground((x, z) -> false, (x, z) -> x >= qx && x < qx + 4 && z >= qz && z < qz + 4);
                patches &= com.hex.kagune.control.village.DryLand.dry(patch, 70, minX, minZ, sizeX, sizeZ) == !near;
                lakeTries++;
            }
        }
        check(lakes && lakeTries > 1000, "a lake (16 across) under it or at its edge, anywhere: not there; further off: it may stand");
        check(patches, "a sea, a river, a beach or a swamp under it or at its edge (its biome, on its 4-block grain): not there");
        check(com.hex.kagune.control.village.DryLand.dry(ground((x, z) -> false, (x, z) -> false), 70, minX, minZ, sizeX, sizeZ),
            "all dry: it stands");
        // A river 4 blocks wide (its biome) through it, any way and anywhere: not there.
        boolean rivers = true;
        for (int at = minX - margin; at <= minX + sizeX - 1 + margin; at++) {
            int a = Math.floorDiv(at, 4) * 4;
            rivers &= !com.hex.kagune.control.village.DryLand.dry(ground((x, z) -> false, (x, z) -> x >= a && x < a + 4), 70, minX, minZ, sizeX, sizeZ);
        }
        for (int at = minZ - margin; at <= minZ + sizeZ - 1 + margin; at++) {
            int a = Math.floorDiv(at, 4) * 4;
            rivers &= !com.hex.kagune.control.village.DryLand.dry(ground((x, z) -> false, (x, z) -> z >= a && z < a + 4), 70, minX, minZ, sizeX, sizeZ);
        }
        check(rivers, "a river through it, any way: not there");
        // The sea at once: its first column is wet, nothing more is looked at.
        int[] looks = {0};
        com.hex.kagune.control.village.DryLand.Ground sea = new com.hex.kagune.control.village.DryLand.Ground() {
            @Override
            public int surface(int x, int z) {
                looks[0]++;
                return 63;
            }

            @Override
            public int floor(int x, int z) {
                return 40;
            }

            @Override
            public boolean wet(int x, int y, int z) {
                looks[0]++;
                return true;
            }
        };
        check(!com.hex.kagune.control.village.DryLand.dry(sea, 63, 0, 0, 272, 272) && looks[0] == 1, "the sea is seen at its first column");

        // The game's spread (RandomSpreadStructurePlacement, linear): its legacy random is java.util.Random's own.
        long[] seeds = {0L, 1L, -1L, 42L, 1234567890123L, Long.MIN_VALUE + 7, 8675309L};
        boolean same = true;
        boolean within = true;
        for (long seed : seeds) {
            for (int rx = -6; rx <= 6; rx++) {
                for (int rz = -6; rz <= 6; rz++) {
                    int[] chunk = com.hex.kagune.control.village.DryLand.spreadChunk(seed, rx, rz, 40, 20, com.hex.kagune.control.village.DryLand.SALT);
                    long[] legacy = {((long)rx * 341873128712L + (long)rz * 132897987541L + seed + (long)com.hex.kagune.control.village.DryLand.SALT
                        ^ 0x5DEECE66DL) & ((1L << 48) - 1)};
                    int x = legacyInt(legacy, 20);
                    int z = legacyInt(legacy, 20);
                    same &= chunk[0] == rx * 40 + x && chunk[1] == rz * 40 + z;
                    within &= Math.floorDiv(chunk[0], 40) == rx && Math.floorDiv(chunk[1], 40) == rz && chunk[0] - rx * 40 < 20 && chunk[1] - rz * 40 < 20;
                }
            }
        }
        check(same, "the offers are the game's: its legacy random (seeded by the region and the salt) taken step by step");
        check(within, "each offer in its own region, its separation kept");
        // The hideout's offers: nearest the world's middle first, none too near it; chosen the same way however often it is asked.
        for (long seed : seeds) {
            int[][] offers = com.hex.kagune.control.village.DryLand.hideoutOffers(seed);
            boolean order = offers.length > 40 && offers.length <= 4 * com.hex.kagune.control.village.DryLand.REGIONS * com.hex.kagune.control.village.DryLand.REGIONS;
            long last = -1L;
            for (int[] offer : offers) {
                long d = (long)offer[0] * offer[0] + (long)offer[1] * offer[1];
                order &= d >= last && d >= (long)com.hex.kagune.control.village.DryLand.NEAREST * com.hex.kagune.control.village.DryLand.NEAREST;
                int[] again = com.hex.kagune.control.village.DryLand.spreadChunk(seed, Math.floorDiv(offer[0], com.hex.kagune.control.village.DryLand.SPACING),
                    Math.floorDiv(offer[1], com.hex.kagune.control.village.DryLand.SPACING), com.hex.kagune.control.village.DryLand.SPACING,
                    com.hex.kagune.control.village.DryLand.SEPARATION, com.hex.kagune.control.village.DryLand.SALT);
                order &= again[0] == offer[0] && again[1] == offer[1];
                last = d;
            }
            check(order && java.util.Arrays.deepEquals(offers, com.hex.kagune.control.village.DryLand.hideoutOffers(seed)),
                "seed " + seed + ": the hideout's offers are the game's own start chunks, nearest first, none too near the middle");
        }
        int[][] offers = {{40, 3}, {41, 50}, {-60, 2}, {7, -80}, {90, 90}};
        java.util.Set<Integer> wet = java.util.Set.of(0);
        java.util.Set<Integer> steep = java.util.Set.of(1, 3);
        java.util.Set<Integer> badBiome = java.util.Set.of(2);
        java.util.Set<Integer> pond = new java.util.HashSet<>();
        com.hex.kagune.control.village.DryLand.Judge judge = new com.hex.kagune.control.village.DryLand.Judge() {
            int index(int x, int z) {
                for (int i = 0; i < offers.length; i++) {
                    if (offers[i][0] == x && offers[i][1] == z) {
                        return i;
                    }
                }
                return -1;
            }

            @Override
            public int[] grid(int x, int z) {
                int i = index(x, z);
                if (wet.contains(i)) {
                    return null;
                }
                int[] floor = new int[25];
                java.util.Arrays.fill(floor, 70 + i);
                if (steep.contains(i)) {
                    floor[3] = 70 + i + com.hex.kagune.control.village.VillageGround.FLAT + 5;
                }
                return floor;
            }

            @Override
            public boolean biome(int x, int z, int y) {
                return !badBiome.contains(index(x, z));
            }

            @Override
            public boolean dry(int x, int z, int y) {
                return !pond.contains(index(x, z));
            }

            @Override
            public int[] wetness(int x, int z, int most) {
                int i = index(x, z);
                // Offer 0's middle is under water; offer 2 may not stand in its biome; the rest: the further out, the less water.
                return i < 0 || i == 0 || badBiome.contains(i) ? null : new int[] {10 - 2 * i, 70 + i};
            }
        };
        int[] chosen = com.hex.kagune.control.village.DryLand.choose(offers, judge, com.hex.kagune.control.village.VillageGround.FLAT);
        check(chosen != null && chosen[0] == 90 && chosen[1] == 90 && chosen[2] == 74, "the first dry, fairly flat offer in a biome it may stand in: "
            + java.util.Arrays.toString(chosen));
        pond.add(4);
        chosen = com.hex.kagune.control.village.DryLand.choose(offers, judge, com.hex.kagune.control.village.VillageGround.FLAT);
        check(chosen != null && chosen[0] == 41 && chosen[1] == 50 && chosen[2] == 71, "a pond at the flat one: the first dry steep one instead");
        pond.add(1);
        pond.add(3);
        check(com.hex.kagune.control.village.DryLand.choose(offers, judge, com.hex.kagune.control.village.VillageGround.FLAT) == null,
            "no dry land among them: no hideout in the water");
        // 4.1.6 (the owner: "I cant get the aogiri hideout to spawn or the ccg building at all"): none of the near offers dry, so the
        // further ones are weighed; none of those either, so the driest near one (its middle on dry land, in a biome it may stand in).
        int[][] further = {{200, 3}, {-210, 40}};
        java.util.Set<Integer> ponds = new java.util.HashSet<>(pond);
        com.hex.kagune.control.village.DryLand.Judge wide = new com.hex.kagune.control.village.DryLand.Judge() {
            boolean far(int x) {
                return Math.abs(x) >= 200;
            }

            @Override
            public int[] grid(int x, int z) {
                return far(x) ? new int[] {80, 80, 81} : judge.grid(x, z);
            }

            @Override
            public boolean biome(int x, int z, int y) {
                return far(x) || judge.biome(x, z, y);
            }

            @Override
            public boolean dry(int x, int z, int y) {
                return far(x) ? !ponds.contains(x) : judge.dry(x, z, y);
            }

            @Override
            public int[] wetness(int x, int z, int most) {
                return far(x) ? new int[] {50, 80} : judge.wetness(x, z, most);
            }
        };
        int[] picked = com.hex.kagune.control.village.DryLand.pick(offers, further, wide, com.hex.kagune.control.village.VillageGround.FLAT);
        check(picked != null && picked[0] == 200 && picked[1] == 3 && picked[2] == 80, "no near offer dry: the first dry further one "
            + java.util.Arrays.toString(picked));
        ponds.add(200);
        ponds.add(-210);
        picked = com.hex.kagune.control.village.DryLand.pick(offers, further, wide, com.hex.kagune.control.village.VillageGround.FLAT);
        check(picked != null && picked[0] == 90 && picked[1] == 90 && picked[2] == 74,
            "nothing dry anywhere: still a hideout, at the driest near offer standing on land in its biomes " + java.util.Arrays.toString(picked));
        pond.clear();
        picked = com.hex.kagune.control.village.DryLand.pick(offers, further, wide, com.hex.kagune.control.village.VillageGround.FLAT);
        check(picked != null && picked[0] == 40 + 50 && java.util.Arrays.equals(picked, com.hex.kagune.control.village.DryLand.choose(offers, judge,
            com.hex.kagune.control.village.VillageGround.FLAT)), "a world whose near offers held a dry one keeps the same spot as before 4.1.6");
        check(com.hex.kagune.control.village.DryLand.driest(new int[][] {{40, 3}, {-60, 2}}, judge) == null,
            "an offer under water, or in a biome it may not stand in, is never the driest");
        // The wetness counted: a wet biome column 1, water over the ground the 16 biome columns' worth it stands for; it stops past most.
        com.hex.kagune.control.village.DryLand.Ground soaked = new com.hex.kagune.control.village.DryLand.Ground() {
            public int surface(int x, int z) {
                return x >= 50 ? 64 : 70;
            }

            public int floor(int x, int z) {
                return x >= 50 ? 50 : 70;
            }

            public boolean wet(int x, int y, int z) {
                return x >= 50;
            }
        };
        int dryCount = com.hex.kagune.control.village.DryLand.wetness(soaked, 70, -200, 0, 60, 60, Integer.MAX_VALUE);
        int wetAll = com.hex.kagune.control.village.DryLand.wetness(soaked, 70, 100, 0, 60, 60, Integer.MAX_VALUE);
        int biomeColumns = com.hex.kagune.control.village.DryLand.columns(100, 0, 60, 60, com.hex.kagune.control.village.DryLand.BIOME_STEP).length;
        int waterColumns = com.hex.kagune.control.village.DryLand.columns(100, 0, 60, 60, com.hex.kagune.control.village.DryLand.STEP).length;
        check(dryCount == 0 && wetAll == biomeColumns + 16 * waterColumns, "the wetness of dry ground and of the sea: " + dryCount + ", " + wetAll);
        int capped = com.hex.kagune.control.village.DryLand.wetness(soaked, 70, 100, 0, 60, 60, 5);
        check(capped == 6, "it stops counting once past the most it is asked for: " + capped);
        // Its further offers: the regions out to FAR, never a near one, nearest first; the building's also kept from the others' spots.
        for (long seed : new long[] {0L, 99L, -12345L}) {
            int[][] near = com.hex.kagune.control.village.DryLand.hideoutOffers(seed);
            int[][] far = com.hex.kagune.control.village.DryLand.hideoutFurther(seed);
            java.util.Set<String> nearSet = new java.util.HashSet<>();
            for (int[] o : near) {
                nearSet.add(o[0] + "," + o[1]);
            }
            boolean apart = far.length > near.length;
            long last = -1L;
            for (int[] o : far) {
                long d = (long)o[0] * o[0] + (long)o[1] * o[1];
                int rx = Math.floorDiv(o[0], com.hex.kagune.control.village.DryLand.SPACING);
                int rz = Math.floorDiv(o[1], com.hex.kagune.control.village.DryLand.SPACING);
                int far8 = com.hex.kagune.control.village.DryLand.FAR;
                int in4 = com.hex.kagune.control.village.DryLand.REGIONS;
                apart &= !nearSet.contains(o[0] + "," + o[1]) && d >= last && rx >= -far8 && rx < far8 && rz >= -far8 && rz < far8
                    && !(rx >= -in4 && rx < in4 && rz >= -in4 && rz < in4);
                last = d;
            }
            check(apart, "seed " + seed + ": the further offers lie beyond the near ones, nearest first (" + far.length + ")");
            int[][] ccgFar = com.hex.kagune.control.village.DryLand.ccgFurther(seed);
            boolean kept = ccgFar.length > 0;
            for (int[] o : ccgFar) {
                kept &= !com.hex.kagune.control.village.DryLand.near(seed, o[0], o[1], com.hex.kagune.control.village.DryLand.CCG_APART, 40, 20,
                    com.hex.kagune.control.village.DryLand.SALT) && !com.hex.kagune.control.village.DryLand.near(seed, o[0], o[1],
                    com.hex.kagune.control.village.DryLand.CCG_APART, 34, 20, com.hex.kagune.control.village.DryLand.VILLAGE_SALT);
            }
            check(kept, "seed " + seed + ": the building's further offers are kept from the mansion's and the towns' spots too");
        }
        // The spots a place may move to, and the start chunks a jigsaw place may move by: nearest first, all within reach.
        int[][] shifts = com.hex.kagune.control.village.DryLand.shifts(5);
        check(shifts.length == 121 && shifts[0][0] == 0 && shifts[0][1] == 0 && shifts[1][0] * shifts[1][0] + shifts[1][1] * shifts[1][1] == 1
            && shifts[120][0] * shifts[120][0] + shifts[120][1] * shifts[120][1] == 50, "a jigsaw place moved a chunk at a time, nearest first");
        for (int size : new int[] {1, 37, 77, 130, 183, 263, 272}) {
            for (int corner : new int[] {-160, 0, 48, 1600}) {
                int want = corner + 8 - size / 2;
                int[] runs = com.hex.kagune.control.village.DryLand.runs(want, corner, size, 32);
                boolean ok = runs.length >= 1 && runs[0] == ControlStructuresProbe.clampMin(want, corner, size);
                for (int run : runs) {
                    ok &= com.hex.kagune.control.village.DryLand.reachable(corner, corner, run, run, run + size - 1, run + size - 1)
                        && Math.floorMod(run - runs[0], 32) == 0;
                }
                ok &= size < 272 - 32 || runs.length == 1;
                check(ok, "a " + size + "-block place's spots from a chunk at " + corner + ": the middle first, all within its reach");
            }
        }
        int[][] spots = com.hex.kagune.control.village.DryLand.spots(new int[] {10, -22, 42}, new int[] {5, -27}, 9);
        check(spots.length == 6 && spots[0][0] == 10 && spots[0][1] == 5, "the middle spot first, then the nearest");
        check(com.hex.kagune.control.village.DryLand.REACH_BACK == 128 && com.hex.kagune.control.village.DryLand.REACH_ON == 143,
            "a place reaches 8 chunks either way of its start chunk, no further");

        // Every structure: one of the expansion's dry-land types; their biomes never wet; the wet biomes listed.
        try {
            java.nio.file.Path data = java.nio.file.Path.of("src/main/resources/data/hexkagune");
            String wetTag = new String(java.nio.file.Files.readAllBytes(data.resolve("tags/worldgen/biome/wet.json")));
            check(wetTag.contains("\"#minecraft:is_ocean\"") && wetTag.contains("\"#minecraft:is_river\"") && wetTag.contains("\"#minecraft:is_beach\"")
                && wetTag.contains("\"minecraft:swamp\"") && wetTag.contains("\"minecraft:mangrove_swamp\""), "the wet biomes: seas, rivers, beaches, swamps");
            try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(data.resolve("worldgen/structure"))) {
                for (java.nio.file.Path file : files.sorted().toList()) {
                    String json = new String(java.nio.file.Files.readAllBytes(file));
                    String name = file.getFileName().toString();
                    check(json.contains("\"type\": \"hexkagune:dry_place\"") || json.contains("\"type\": \"hexkagune:site_")
                        || name.equals("village.json") && json.contains("\"type\": \"hexkagune:dry_jigsaw\""), name + ": built only on dry land");
                }
            }
            for (String place : List.of("anteiku", "library", "cyber_tower")) {
                String json = new String(java.nio.file.Files.readAllBytes(data.resolve("worldgen/structure/" + place + ".json")));
                check(json.contains("\"type\": \"hexkagune:dry_place\"") && json.contains("\"start_pool\": \"hexkagune:" + place + "/start\""),
                    place + ": moved near its start onto dry land");
                String pool = new String(java.nio.file.Files.readAllBytes(data.resolve("worldgen/template_pool/" + place + "/start.json")));
                check(pool.split("\"element_type\"").length == 2 && pool.contains("minecraft:single_pool_element"),
                    place + ": one template (its pieces come out the same each time they are looked at)");
            }
            // 4.1.9 (the owner: "it should be able to spawn literally in any biome ... It should also be fully compatible with Biomes of
            // Plenty"): no place has a list of biomes any more (a mod's biome missing from it was no place at all: in a world of Regions
            // Unexplored's biomes the hideout, the CCG building and Ken's tower were nowhere, /locate and all). Every structure's biomes are
            // #hexkagune:overworld (only the overworld's: a set runs only where its biomes can be made), and the land is judged in code.
            String overworld = new String(java.nio.file.Files.readAllBytes(data.resolve("tags/worldgen/biome/overworld.json")));
            check(overworld.contains("\"#minecraft:is_overworld\"") && overworld.split("\"#?[a-z_]+:[a-z_/]+\"").length == 2,
                "#hexkagune:overworld is the overworld's biomes and nothing else");
            try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(data.resolve("worldgen/structure"))) {
                for (java.nio.file.Path file : files.sorted().toList()) {
                    String json = new String(java.nio.file.Files.readAllBytes(file));
                    check(json.contains("\"biomes\": \"#hexkagune:overworld\""), file.getFileName() + ": any biome of the overworld");
                }
            }
            try (java.util.stream.Stream<java.nio.file.Path> tags = java.nio.file.Files.walk(data.resolve("tags/worldgen/biome"))) {
                java.util.List<String> left = tags.filter(java.nio.file.Files::isRegularFile).map(f -> data.resolve("tags/worldgen/biome").relativize(f).toString())
                    .sorted().toList();
                check(left.equals(List.of("overworld.json", "wet.json")), "no place's own biome list is left: " + left);
            }
            check(wetTag.contains("\"#forge:is_swamp\"") && wetTag.contains("\"#forge:is_water\"") && wetTag.contains("\"#c:is_swamp\"")
                && wetTag.contains("\"biomesoplenty:marsh\"") && wetTag.split("\"required\": false").length > 10,
                "a mod's swamps and waters are wet too (Forge's and the common tags, Biomes O' Plenty's own), each only if it is there");
            String structures = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/ControlStructures.java")));
            check(structures.contains("TYPES.register(\"dry_place\", () -> DRY_PLACE);")
                && structures.contains("&& DryLand.dry(ground, middle, at[0], at[1], sizeX, sizeZ)")
                && structures.contains("&& DryLand.dry(ground, at.m_123342_(), minX, minZ, maxX - minX + 1, maxZ - minZ + 1)")
                && structures.contains("DryLand.reachable(home.m_45604_(), home.m_45605_(), minX, minZ, maxX, maxZ)")
                && !structures.contains("WET_EDGE") && structures.contains("m_203656_(WET)")
                && structures.contains("ResourceLocation.m_214293_(\"hexkagune\", \"wet\")"),
                "every place checks the dry land under it and round it, wet biomes and water both, within its start chunk's reach");
        } catch (java.io.IOException failure) {
            check(false, "read the sources: " + failure);
        }
    }

    /** The game's legacy random (LegacyRandomSource: next(bits), nextInt(bound) by BitRandomSource), written out on its own. */
    private static int legacyInt(long[] seed, int bound) {
        int r;
        int v;
        do {
            seed[0] = seed[0] * 0x5DEECE66DL + 0xBL & ((1L << 48) - 1);
            r = (int)(seed[0] >> 17);
            v = r % bound;
        } while (r - v + (bound - 1) < 0);
        return v;
    }

    /** A world's ground for {@link #dryLand}: water over the ground where {@code lake}, a wet biome where {@code wet}. */
    private static com.hex.kagune.control.village.DryLand.Ground ground(java.util.function.BiPredicate<Integer, Integer> lake,
                                                                        java.util.function.BiPredicate<Integer, Integer> wet) {
        return new com.hex.kagune.control.village.DryLand.Ground() {
            @Override
            public int surface(int x, int z) {
                return lake.test(x, z) ? 72 : 70;
            }

            @Override
            public int floor(int x, int z) {
                return lake.test(x, z) ? 66 : 70;
            }

            @Override
            public boolean wet(int x, int y, int z) {
                return wet.test(x, z);
            }
        };
    }

    /** ControlStructures.Site.clampMin's sum, written out (the class itself needs the game). */
    private static final class ControlStructuresProbe {
        static int clampMin(int want, int corner, int size) {
            int lo = corner - 128;
            int hi = corner + 143 - size + 1;
            return hi < lo ? lo : Math.max(lo, Math.min(hi, want));
        }
    }

    /**
     * 4.1.4 (the owner: "CCG building, tons of ccg agents guard and patrol the area, sit in their offices"; "Also I forgot to send you the
     * CCG building, this is it, also like the others only 1 of them spawns"): the building as a place of its own, one in a world, on dry
     * land away from the mansion and the towns; its people at their posts, on their patrol and at their desks.
     */
    @SuppressWarnings("unchecked")
    /**
     * 4.1.9 (the owner: "Cant find CCG building, Aogiri Hideout or Kaneki's building AT ALL not even with locate"; "it should be able to
     * spawn literally in any biome as long as there's only one of each structure aside from the villages, of course"): the hideout, the
     * CCG building, Anteiku, the library and Ken's tower are the world's one each, in any land biome, a mod's too.
     */
    static void onePerWorld() {
        com.hex.kagune.control.village.DryLand.Probe everywhere = new com.hex.kagune.control.village.DryLand.Probe() {
            @Override
            public boolean land(int chunkX, int chunkZ) {
                return true;
            }

            @Override
            public int[] placed(int chunkX, int chunkZ) {
                return new int[] {chunkX + 1, chunkZ};
            }
        };
        int[][] near = {{40, 3}, {-50, 7}};
        int[][] further = {{200, 9}};
        int[] first = com.hex.kagune.control.village.DryLand.firstPlaced(near, further, everywhere, at -> true, 24);
        check(first != null && first[0] == 40 && first[1] == 3 && first[2] == 41 && first[3] == 3, "the first offer that stands, with where its middle stands");
        int[] skipped = com.hex.kagune.control.village.DryLand.firstPlaced(near, further, everywhere, at -> at[0] != 41, 24);
        check(skipped != null && skipped[0] == -50, "an offer that would stand too near another place is passed over");
        int[] further1 = com.hex.kagune.control.village.DryLand.firstPlaced(near, further, everywhere, at -> at[0] > 100, 24);
        check(further1 != null && further1[0] == 200, "the further offers once the near ones are all passed over");
        int[] laid = {0};
        com.hex.kagune.control.village.DryLand.Probe sea = new com.hex.kagune.control.village.DryLand.Probe() {
            @Override
            public boolean land(int chunkX, int chunkZ) {
                return chunkX != -50;
            }

            @Override
            public int[] placed(int chunkX, int chunkZ) {
                laid[0]++;
                return null;
            }
        };
        check(com.hex.kagune.control.village.DryLand.firstPlaced(near, further, sea, at -> true, 24) == null && laid[0] == 2,
            "an offer in the sea is never laid out; none that stands: none at all");
        laid[0] = 0;
        int[][] many = new int[100][];
        for (int i = 0; i < many.length; i++) {
            many[i] = new int[] {i * 40, 0};
        }
        check(com.hex.kagune.control.village.DryLand.firstPlaced(many, new int[0][], sea, at -> true, com.hex.kagune.control.village.DryLand.PLACE_TRIES) == null
            && laid[0] == com.hex.kagune.control.village.DryLand.PLACE_TRIES, "no more than PLACE_TRIES offers laid out in full (a world of sea gives up quickly)");
        check(com.hex.kagune.control.village.DryLand.apart(0, 0, List.of(new int[] {12, 0}), 12)
            && !com.hex.kagune.control.village.DryLand.apart(0, 0, List.of(new int[] {11, 3}), 12)
            && com.hex.kagune.control.village.DryLand.apart(5, 5, List.of(), 12), "apart: at least that many chunks from each");
        // Their numbers, each its own salt, the same as their sets'.
        java.util.Set<Integer> salts = new java.util.HashSet<>(List.of(com.hex.kagune.control.village.DryLand.SALT, com.hex.kagune.control.village.DryLand.CCG_SALT,
            com.hex.kagune.control.village.DryLand.VILLAGE_SALT));
        check(List.of(com.hex.kagune.control.village.DryLand.PLACES).equals(List.of("anteiku", "library", "cyber_tower")), "the three jigsaw places, in the order chosen");
        // The offers are read from the set as the world has it (a mod may change every set's spacing): with the file's numbers they are
        // exactly the hideout's and the CCG building's as before; with other numbers, that spread's own offers.
        for (long seed : new long[] {0L, 42L, -7_000_000_000L}) {
            com.hex.kagune.control.village.DryLand.Spread hideoutFile = com.hex.kagune.control.village.DryLand.fixed(seed, 40, 20,
                com.hex.kagune.control.village.DryLand.SALT);
            check(java.util.Arrays.deepEquals(com.hex.kagune.control.village.DryLand.offers(hideoutFile, 0, com.hex.kagune.control.village.DryLand.REGIONS,
                new com.hex.kagune.control.village.DryLand.Spread[0], 0), oldOffers(seed, com.hex.kagune.control.village.DryLand.SALT, 0, 4, false)),
                "the hideout's offers from its set's numbers are the same as 4.1.8's (seed " + seed + ")");
            check(java.util.Arrays.deepEquals(com.hex.kagune.control.village.DryLand.ccgOffers(seed), oldOffers(seed, com.hex.kagune.control.village.DryLand.CCG_SALT, 0, 4, true))
                && java.util.Arrays.deepEquals(com.hex.kagune.control.village.DryLand.hideoutFurther(seed), oldOffers(seed, com.hex.kagune.control.village.DryLand.SALT, 4, 8, false)),
                "the CCG building's and the hideout's further offers are the same as 4.1.8's (seed " + seed + ")");
            com.hex.kagune.control.village.DryLand.Spread[] keep = {hideoutFile, com.hex.kagune.control.village.DryLand.fixed(seed, 34, 20,
                com.hex.kagune.control.village.DryLand.VILLAGE_SALT)};
            check(java.util.Arrays.deepEquals(com.hex.kagune.control.village.DryLand.offers(com.hex.kagune.control.village.DryLand.fixed(seed, 40, 20,
                    com.hex.kagune.control.village.DryLand.CCG_SALT), com.hex.kagune.control.village.DryLand.REGIONS, com.hex.kagune.control.village.DryLand.FAR,
                    keep, com.hex.kagune.control.village.DryLand.CCG_APART), com.hex.kagune.control.village.DryLand.ccgFurther(seed)),
                "the CCG building's further offers from its set's numbers are the same as 4.1.8's (seed " + seed + ")");
            check(java.util.Arrays.deepEquals(com.hex.kagune.control.village.DryLand.ccgFurther(seed), oldOffers(seed, com.hex.kagune.control.village.DryLand.CCG_SALT, 4, 8, true)),
                "the CCG building's further offers, against 4.1.8's own way (seed " + seed + ")");
            // Twice the spacing (a pack that spreads every set out): every offer is the start chunk its region offers, regions twice as far.
            com.hex.kagune.control.village.DryLand.Spread wide = com.hex.kagune.control.village.DryLand.fixed(seed, 80, 40,
                com.hex.kagune.control.village.DryLand.SALT);
            int[][] wideOffers = com.hex.kagune.control.village.DryLand.offers(wide, 0, com.hex.kagune.control.village.DryLand.REGIONS,
                new com.hex.kagune.control.village.DryLand.Spread[0], 0);
            boolean own = wideOffers.length > 0;
            for (int[] offer : wideOffers) {
                int[] again = wide.chunk(offer[0], offer[1]);
                own &= again[0] == offer[0] && again[1] == offer[1];
            }
            check(own, "a wider spread's offers are each the start chunk the game offers in its region (seed " + seed + ")");
        }
        try {
            java.nio.file.Path data = java.nio.file.Path.of("src/main/resources/data/hexkagune");
            for (String place : com.hex.kagune.control.village.DryLand.PLACES) {
                int salt = com.hex.kagune.control.village.DryLand.placeSalt(place);
                check(salt >= 0 && salts.add(salt), place + ": its own salt");
                String set = new String(java.nio.file.Files.readAllBytes(data.resolve("worldgen/structure_set/" + place + ".json")));
                check(set.contains("\"minecraft:random_spread\"") && set.contains("\"spacing\": " + com.hex.kagune.control.village.DryLand.SPACING + ",")
                    && set.contains("\"separation\": " + com.hex.kagune.control.village.DryLand.SEPARATION + ",") && set.contains("\"salt\": " + salt)
                    && set.contains("\"structure\": \"hexkagune:" + place + "\"") && !set.contains("concentric") && !set.contains("\"count\"")
                    && !set.contains("frequency") && !set.contains("exclusion_zone") && !set.contains("\"spread_type\""),
                    place + ": one set of offers, the game's own numbers as DryLand's (no ring of three)");
                int[][] offers = com.hex.kagune.control.village.DryLand.placeOffers(42L, place);
                boolean sorted = offers.length > 10;
                for (int i = 1; i < offers.length; i++) {
                    long a = (long)offers[i - 1][0] * offers[i - 1][0] + (long)offers[i - 1][1] * offers[i - 1][1];
                    long b = (long)offers[i][0] * offers[i][0] + (long)offers[i][1] * offers[i][1];
                    sorted &= a <= b && b >= (long)com.hex.kagune.control.village.DryLand.NEAREST * com.hex.kagune.control.village.DryLand.NEAREST;
                }
                check(sorted && com.hex.kagune.control.village.DryLand.placeFurther(42L, place).length > offers.length, place + ": its offers nearest the middle first");
            }
            check(com.hex.kagune.control.village.DryLand.placeSalt("village_1") < 0 && com.hex.kagune.control.village.DryLand.placeSalt(null) < 0,
                "the villages are not one of a kind");
            try (java.util.stream.Stream<java.nio.file.Path> sets = java.nio.file.Files.list(data.resolve("worldgen/structure_set"))) {
                for (java.nio.file.Path file : sets.sorted().toList()) {
                    String json = new String(java.nio.file.Files.readAllBytes(file));
                    check(!json.contains("concentric_rings") && !json.contains("\"count\""), file.getFileName() + ": no ring of several");
                }
            }
            String structures = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/ControlStructures.java")));
            int overrides = structures.split("public Optional<Structure.GenerationStub> m_262864_\\(Structure.GenerationContext context\\) \\{\n            return this.m_214086_\\(context\\);").length - 1;
            check(overrides == 3, "the jigsaw village, every site and every jigsaw place leave the game's biome-list test out (" + overrides + ")");
            check(structures.contains("return !biome.m_203656_(WET);") && !structures.contains("return context.f_226630_().test(biome);"),
                "a place stands in any biome but a wet one");
            // 4.1.14 (the owner: "Fix structures still not spawning at all or able to be located (still not spawning)"): no one chunk a world
            // any more: every spot of its set where it lays out on dry land may hold it, until the world makes one (PlaceClaims).
            check(structures.contains("if (Site.onPurpose(context)) {\n                return this.here(context);")
                && structures.contains("return claimed(id, context, () -> {")
                && structures.contains("if (!offered(context, placeSpread(context, id))) {")
                && structures.contains("Optional<Structure.GenerationStub> stub = this.here(context);"),
                "a jigsaw place stands at any spot of its set where it lays out on dry land, until one is built (/place builds it anywhere)");
            check(structures.contains("DryLand.near(villageSpread(context), x, z, DryLand.PLACE_APART) || DryLand.near(hideoutSpread(context), x, z, DryLand.PLACE_APART)")
                && structures.contains("x, z, DryLand.PLACE_APART)) {") && structures.contains("if (!other.equals(id) && DryLand.near(placeSpread(context, other), x, z, DryLand.PLACE_APART)) {"),
                "kept clear of the hideout's, the CCG building's, the other places' and the villages' spots");
            check(structures.contains("if (id == null) {\n                return Optional.empty();"), "a place whose name can't be read is never built many times over");
            // 4.1.14: nothing worked out once a world any more (whatever stopped the world's one chunk left the world without it).
            check(!structures.contains("chosen(context, id)") && !structures.contains("= hideout(context)") && !structures.contains("= ccg(context)")
                && !structures.contains("HIDEOUTS") && !structures.contains("BUILDINGS"), "no place hangs on one chunk worked out once a world");
            // Taken as the world makes the chunk (its heights are the chunk being made), never as /locate only asks; a taken spot turns every
            // other away; the first to be made wins, under one lock.
            // 4.1.17: asking is only /locate's own (the level's heights); anything else makes (a mod's height wrapper never builds every spot).
            check(structures.contains("return !(context.f_226629_() instanceof net.minecraft.world.level.Level);")
                && structures.contains("if (taken != null && (taken[0] != chunk.f_45578_ || taken[1] != chunk.f_45579_)) {\n            return Optional.empty();")
                && structures.contains("if (stub.isPresent() && making(context) && !PlaceClaims.claim(id, chunk.f_45578_, chunk.f_45579_)) {"),
                "a spot is taken as the world makes it, and every other spot is turned away from then on");
            // 4.1.17 (the owner: "there's multiple structures of each, which cannot happen"): a place found standing is the world's one if
            // none is taken (a world from before the claims); a second is told in the log; every one-of-a-kind place is looked for.
            String adopt = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/PlaceClaims.java")));
            check(adopt.contains("if (taken == null) {\n                    claim(place, at.f_45578_, at.f_45579_);")
                && adopt.contains("ONE_OF_A_KIND = {\"aogiri_hideout\", \"ccg_building\", \"anteiku\", \"library\", \"cyber_tower\"}"),
                "a one-of-a-kind place already standing is the world's one");
            // 4.1.17 (the owner: "they are spawning under the ground, which cannot happen ... always at the top max area"): every jigsaw
            // place is lifted onto the highest ground of its footprint; the tower starts on the ground, not at y 63.
            check(structures.contains("return Optional.of(raised(stub.get(), DryLand.lift(ground, at.m_123341_(), at.m_123343_(), minX, minZ, maxX, maxZ)));")
                && structures.contains("made.m_192781_(lift);"), "a place stands on the highest ground of its footprint");
            String tower = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/resources/data/hexkagune/worldgen/structure/cyber_tower.json")));
            check(tower.contains("\"project_start_to_heightmap\": \"WORLD_SURFACE_WG\"") && !tower.contains("\"absolute\": 63"), "Ken's tower starts on the ground");
            for (String place : new String[] {"anteiku", "library", "cyber_tower"}) {
                String json = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/resources/data/hexkagune/worldgen/structure/" + place + ".json")));
                check(json.contains("\"project_start_to_heightmap\": \"WORLD_SURFACE_WG\""), place + " starts on the ground's height");
            }
            // The lift: the highest of the footprint's ground, the edges and every LIFT_STEP between.
            com.hex.kagune.control.village.DryLand.Ground hill = new com.hex.kagune.control.village.DryLand.Ground() {
                @Override
                public int surface(int x, int z) {
                    return 64 + Math.max(0, x) / 2 + (z == 37 ? 30 : 0);
                }

                @Override
                public int floor(int x, int z) {
                    return this.surface(x, z);
                }

                @Override
                public boolean wet(int x, int y, int z) {
                    return false;
                }
            };
            check(com.hex.kagune.control.village.DryLand.lift(hill, 0, 0, -20, -20, 20, 20) == 10, "lifted onto the hill's top across its footprint");
            check(com.hex.kagune.control.village.DryLand.lift(hill, 20, 0, -20, -20, 20, 20) == 0, "already at the top: not lifted");
            check(com.hex.kagune.control.village.DryLand.lift(hill, 0, 0, -20, 30, 0, 37) == 30, "a peak at its far edge is found");
            String claims = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/PlaceClaims.java")));
            check(claims.contains("public static void load(LevelEvent.Load event) {") && claims.contains("level.m_8895_().m_164861_(Data::load,")
                && claims.contains("public static void chunk(ChunkEvent.Load event) {") && claims.contains("!ControlStructures.standsIn(level, chunk, id)")
                && claims.contains("@Mod.EventBusSubscriber(modid = \"hexkagune\")"),
                "the taken spots are kept with the world, ready before its ground is made; a taken spot whose chunk holds nothing is given up");
            com.hex.kagune.control.village.TakenPlaces taken = new com.hex.kagune.control.village.TakenPlaces();
            check(taken.at("aogiri_hideout") == null && taken.take("aogiri_hideout", 40, -12) && !taken.take("aogiri_hideout", 80, 3)
                && taken.take("aogiri_hideout", 40, -12) && taken.take("ccg_building", 80, 3) && "ccg_building".equals(taken.whose(80, 3)),
                "the first spot made takes a place; any other is refused; the same spot again is still its own; each place its own");
            check(!taken.release("aogiri_hideout", 80, 3) && taken.release("aogiri_hideout", 40, -12) && taken.take("aogiri_hideout", 80, 3),
                "a spot given up lets the next made take it (and only its own spot gives it up)");
            com.hex.kagune.control.village.TakenPlaces back = com.hex.kagune.control.village.TakenPlaces.read(taken.write());
            check(java.util.Arrays.equals(back.at("aogiri_hideout"), new int[] {80, 3}) && java.util.Arrays.equals(back.at("ccg_building"), new int[] {80, 3})
                && com.hex.kagune.control.village.TakenPlaces.read("anteiku:1,x;library:-5,7;junk;").at("anteiku") == null
                && java.util.Arrays.equals(com.hex.kagune.control.village.TakenPlaces.read("anteiku:1,x;library:-5,7;junk;").at("library"), new int[] {-5, 7})
                && com.hex.kagune.control.village.TakenPlaces.read(null).at("library") == null, "kept and read back as saved; a broken entry is as if not built");
            String commands = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/ControlCommands.java")));
            // (Read in the source: ControlStructures registers the game's own registries as it loads, and there is no game here.)
            check(commands.contains("Commands.m_82127_(\"places\")") && commands.contains("ControlStructures.report()")
                && structures.contains("ONE_OF_A_KIND = java.util.List.of(\"aogiri_hideout\", \"ccg_building\", \"cyber_tower\", \"anteiku\", \"library\")"),
                "/kagune places lists the five places");
            // 4.1.13: from a player it makes each place's start chunk and looks in it: built there, or stopped (one chunk a world: anything that
            // stops it there leaves the world without it).
            check(commands.contains("ControlStructures.report(player.m_284548_())") && structures.contains("if (standsIn(level, level.m_6325_(at[0], at[1]), id)) {")
                && structures.contains("found != null && found.m_73603_()") && structures.contains("PlaceClaims.release(id, at[0], at[1]);"),
                "/kagune places says where each place was built and looks in its chunk; one stopped there is given up for the next spot");
        } catch (java.io.IOException failure) {
            check(false, "read the sets: " + failure);
        }
    }

    /** 4.1.8's DryLand.offers and near, kept here as they were, to hold 4.1.9's (read from a Spread) to the same offers. */
    private static int[][] oldOffers(long seed, int salt, int inner, int outer, boolean apart) {
        java.util.List<int[]> offers = new java.util.ArrayList<>();
        for (int rx = -outer; rx < outer; rx++) {
            for (int rz = -outer; rz < outer; rz++) {
                if (rx >= -inner && rx < inner && rz >= -inner && rz < inner) {
                    continue;
                }
                int[] chunk = com.hex.kagune.control.village.DryLand.spreadChunk(seed, rx, rz, 40, 20, salt);
                if ((long)chunk[0] * chunk[0] + (long)chunk[1] * chunk[1] < 32L * 32L) {
                    continue;
                }
                if (apart && (oldNear(seed, chunk[0], chunk[1], 12, 40, 20, com.hex.kagune.control.village.DryLand.SALT)
                    || oldNear(seed, chunk[0], chunk[1], 12, 34, 20, com.hex.kagune.control.village.DryLand.VILLAGE_SALT))) {
                    continue;
                }
                offers.add(chunk);
            }
        }
        offers.sort(java.util.Comparator.<int[]>comparingLong(c -> (long)c[0] * c[0] + (long)c[1] * c[1]).thenComparingInt(c -> c[0]).thenComparingInt(c -> c[1]));
        return offers.toArray(new int[0][]);
    }

    private static boolean oldNear(long seed, int x, int z, int apart, int spacing, int separation, int salt) {
        int rx = Math.floorDiv(x, spacing);
        int rz = Math.floorDiv(z, spacing);
        int reach = apart / spacing + 1;
        for (int i = rx - reach; i <= rx + reach; i++) {
            for (int j = rz - reach; j <= rz + reach; j++) {
                int[] other = com.hex.kagune.control.village.DryLand.spreadChunk(seed, i, j, spacing, separation, salt);
                long dx = other[0] - x;
                long dz = other[1] - z;
                if (dx * dx + dz * dz < (long)apart * apart) {
                    return true;
                }
            }
        }
        return false;
    }

    static void ccgBuilding() {
        com.hex.kagune.control.village.SiteMaps.Site site = com.hex.kagune.control.village.SiteMaps.of("ccg_building");
        check(site != null && site.width() == 39 && site.depth() == 39 && site.height() == 58 && site.ground() == 0 && site.map() == 6
            && site.across() == 1 && site.down() == 1 && com.hex.kagune.control.village.SiteMaps.byMap(6) == site,
            "the building is the owner's (39 x 39, 58 tall: the empty layers over its roof left off), one tile, its own map");
        check(com.hex.kagune.control.village.SiteMaps.ALL.length == 4 && com.hex.kagune.control.village.SiteMaps.of("aogiri_hideout").map() == 5
            && com.hex.kagune.control.village.SiteMaps.of("village_1").map() == 3 && com.hex.kagune.control.village.SiteMaps.of("village_2").map() == 4,
            "the three older places keep their maps");
        // Its patrol: a loop round the L, a block or two out from its walls, all of it within its perimeter.
        int[][] route = site.route();
        boolean round = route.length >= 12;
        int west = 0;
        int east = 0;
        int north = 0;
        int south = 0;
        for (int i = 0; i < route.length; i++) {
            int[] a = route[i];
            int[] b = route[(i + 1) % route.length];
            round &= Math.hypot(a[0] - b[0], a[2] - b[2]) <= 24.0 && a[1] == 0 && Math.abs(a[0]) <= site.perimeter() && Math.abs(a[2]) <= site.perimeter();
            west += a[0] <= -19 ? 1 : 0;
            east += a[0] >= 19 ? 1 : 0;
            north += a[2] <= -19 ? 1 : 0;
            south += a[2] >= 17 ? 1 : 0;
        }
        check(round && west >= 3 && east >= 2 && north >= 2 && south >= 3, "its patrol goes round it: stops on every side, none far from the next, at street level");
        // Its offers: the game's own for its set, none near a spot of the mansion's or a town's; never the hideout's own offers.
        long[] seeds = {0L, 1L, -7L, 123456789L, 8675309L, Long.MIN_VALUE + 5L, 4242424242L};
        int fewest = Integer.MAX_VALUE;
        for (long seed : seeds) {
            int[][] offers = com.hex.kagune.control.village.DryLand.ccgOffers(seed);
            fewest = Math.min(fewest, offers.length);
            boolean ok = true;
            long last = -1L;
            int[][] hideouts = com.hex.kagune.control.village.DryLand.hideoutOffers(seed);
            for (int[] offer : offers) {
                long d = (long)offer[0] * offer[0] + (long)offer[1] * offer[1];
                ok &= d >= last && d >= (long)com.hex.kagune.control.village.DryLand.NEAREST * com.hex.kagune.control.village.DryLand.NEAREST;
                last = d;
                int[] again = com.hex.kagune.control.village.DryLand.spreadChunk(seed, Math.floorDiv(offer[0], 40), Math.floorDiv(offer[1], 40), 40, 20,
                    com.hex.kagune.control.village.DryLand.CCG_SALT);
                ok &= again[0] == offer[0] && again[1] == offer[1];
                for (int[] hideout : hideouts) {
                    ok &= Math.hypot(hideout[0] - offer[0], hideout[1] - offer[1]) >= com.hex.kagune.control.village.DryLand.CCG_APART;
                }
                // Every town's offer anywhere near, written out: none within reach.
                for (int rx = Math.floorDiv(offer[0], 34) - 2; rx <= Math.floorDiv(offer[0], 34) + 2; rx++) {
                    for (int rz = Math.floorDiv(offer[1], 34) - 2; rz <= Math.floorDiv(offer[1], 34) + 2; rz++) {
                        int[] town = com.hex.kagune.control.village.DryLand.spreadChunk(seed, rx, rz, 34, 20, 1873602241);
                        ok &= Math.hypot(town[0] - offer[0], town[1] - offer[1]) >= com.hex.kagune.control.village.DryLand.CCG_APART;
                    }
                }
            }
            check(ok && java.util.Arrays.deepEquals(offers, com.hex.kagune.control.village.DryLand.ccgOffers(seed)),
                "seed " + seed + ": the building's offers are the game's own, nearest first, none within " + com.hex.kagune.control.village.DryLand.CCG_APART
                    + " chunks of a spot of the mansion's or a town's");
        }
        check(fewest >= 6, "every world has several spots to try (the fewest of these seeds: " + fewest + ")");
        // 12 chunks: the mansion and the towns reach 136 blocks from their middle, the building and its patrol 28.
        check(com.hex.kagune.control.village.DryLand.CCG_APART * 16 >= 136 + 28 + 16 && com.hex.kagune.control.village.DryLand.CCG_SALT != com.hex.kagune.control.village.DryLand.SALT
            && com.hex.kagune.control.village.DryLand.CCG_SALT != com.hex.kagune.control.village.DryLand.VILLAGE_SALT, "far enough that they never touch; its own salt");
        check(com.hex.kagune.control.village.DryLand.near(5L, 100, 100, 1000, 40, 20, 77) && !com.hex.kagune.control.village.DryLand.near(5L, 100, 100, 0, 40, 20, 77),
            "near: anything within a great reach, nothing within none");
        try {
            java.nio.file.Path data = java.nio.file.Path.of("src/main/resources/data/hexkagune");
            String set = new String(java.nio.file.Files.readAllBytes(data.resolve("worldgen/structure_set/ccg_building.json")));
            check(set.contains("\"minecraft:random_spread\"") && set.contains("\"spacing\": " + com.hex.kagune.control.village.DryLand.SPACING + ",")
                && set.contains("\"separation\": " + com.hex.kagune.control.village.DryLand.SEPARATION + ",")
                && set.contains("\"salt\": " + com.hex.kagune.control.village.DryLand.CCG_SALT) && set.contains("\"structure\": \"hexkagune:ccg_building\"")
                && !set.contains("\"spread_type\"") && !set.contains("frequency") && !set.contains("exclusion_zone"),
                "its offers are the game's own (a linear random spread, the same numbers as DryLand's)");
            String villages = new String(java.nio.file.Files.readAllBytes(data.resolve("worldgen/structure_set/villages.json")));
            check(villages.contains("\"spacing\": " + com.hex.kagune.control.village.DryLand.VILLAGE_SPACING + ",")
                && villages.contains("\"separation\": " + com.hex.kagune.control.village.DryLand.VILLAGE_SEPARATION + ",")
                && villages.contains("\"salt\": " + com.hex.kagune.control.village.DryLand.VILLAGE_SALT), "the towns' numbers are their set's");
            String structure = new String(java.nio.file.Files.readAllBytes(data.resolve("worldgen/structure/ccg_building.json")));
            check(structure.contains("\"type\": \"hexkagune:site_ccg_building\"") && structure.contains("\"biomes\": \"#hexkagune:overworld\"")
                && structure.contains("\"terrain_adaptation\": \"beard_box\""), "its structure: its own type, any biome of the overworld, the ground fitted to it");
            String structures = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/ControlStructures.java")));
            check(structures.contains("return claimed(CCG, context, () -> {")
                && structures.contains("DryLand.near(hideoutSpread(context), chunk.f_45578_, chunk.f_45579_, DryLand.CCG_APART)")
                && structures.contains("DryLand.near(villageSpread(context), chunk.f_45578_, chunk.f_45579_, DryLand.CCG_APART)"),
                "the world's one building, at the first of its set's spots on dry land the world makes, clear of the hideout's and the villages'");
            // The template itself: its people, and what each does.
            java.util.Map<String, Object> template;
            try (java.io.DataInputStream in = new java.io.DataInputStream(new java.util.zip.GZIPInputStream(
                java.nio.file.Files.newInputStream(data.resolve("structures/ccg_building/t_0_0.nbt"))))) {
                in.readByte();
                in.readUTF();
                template = (java.util.Map<String, Object>)nbt(in, 10);
            }
            List<Object> size = (List<Object>)template.get("size");
            check(size.equals(List.of(39, 58, 39)), "the template's size is the site's " + size);
            List<Object> palette = (List<Object>)template.get("palette");
            java.util.Map<List<Object>, String> blocks = new java.util.HashMap<>();
            for (Object block : (List<Object>)template.get("blocks")) {
                java.util.Map<String, Object> b = (java.util.Map<String, Object>)block;
                blocks.put((List<Object>)b.get("pos"), (String)((java.util.Map<String, Object>)palette.get((Integer)b.get("state"))).get("Name"));
            }
            int seats = 0;
            int posts = 0;
            int walkers = 0;
            int generals = 0;
            boolean sound = true;
            java.util.Set<Integer> seatFloors = new java.util.HashSet<>();
            java.util.Set<Integer> postFloors = new java.util.HashSet<>();
            java.util.Set<List<Object>> spots = new java.util.HashSet<>();
            List<Object> entities = (List<Object>)template.get("entities");
            for (Object entity : entities) {
                java.util.Map<String, Object> e = (java.util.Map<String, Object>)entity;
                java.util.Map<String, Object> tag = (java.util.Map<String, Object>)e.get("nbt");
                java.util.Map<String, Object> forge = (java.util.Map<String, Object>)tag.get("ForgeData");
                java.util.Map<String, Object> village = (java.util.Map<String, Object>)forge.get("HexVillage");
                List<Object> at = (List<Object>)e.get("blockPos");
                int x = (Integer)at.get(0);
                int y = (Integer)at.get(1);
                int z = (Integer)at.get(2);
                if ("hexkagune:general".equals(tag.get("id"))) {
                    // 4.1.5: the General, in his office on the top floor.
                    generals++;
                    sound &= y == 49 && spots.add(at) && blocks.get(at) == null && blocks.get(List.of(x, y - 1, z)) != null
                        && !forge.containsKey(com.hex.kagune.control.npc.HqRules.SEAT) && !forge.containsKey(com.hex.kagune.control.npc.HqRules.POST);
                    continue;
                }
                sound &= "hexkagune:ccg_investigator".equals(tag.get("id")) && Byte.valueOf((byte)1).equals(tag.get("PersistenceRequired"))
                    && Integer.valueOf(6).equals(village.get("Map")) && spots.add(at)
                    && (Integer)village.get("CentreX") == site.middleX() - x && (Integer)village.get("CentreZ") == site.middleZ() - z
                    && (Integer)village.get("CentreY") == 1 - y;
                String here = blocks.get(at);
                String over = blocks.get(List.of(x, y + 1, z));
                String under = blocks.get(List.of(x, y - 1, z));
                boolean seat = forge.containsKey(com.hex.kagune.control.npc.HqRules.SEAT);
                boolean post = forge.containsKey(com.hex.kagune.control.npc.HqRules.POST);
                if (seat) {
                    seats++;
                    seatFloors.add((y - 1) / 8);
                    int yaw = (Integer)forge.get(com.hex.kagune.control.npc.HqRules.YAW);
                    // On a chair (a stair) with nothing over it but an office chair's back, a floor's own height; the block it looks to free
                    // at knee height or a table's leg.
                    double[] looks = {-Math.sin(Math.toRadians(yaw)), Math.cos(Math.toRadians(yaw))};
                    String front = blocks.get(List.of(x + (int)Math.round(looks[0]), y, z + (int)Math.round(looks[1])));
                    sound &= !post && here != null && here.endsWith("_stairs") && (over == null || over.endsWith("_trapdoor")) && (y - 1) % 8 == 0
                        && yaw % 90 == 0 && (front == null || "minecraft:air".equals(front) || front.endsWith("_fence"))
                        && blocks.get(List.of(x, y + 2, z)) == null;
                } else {
                    sound &= (here == null || "minecraft:air".equals(here)) && (over == null || "minecraft:air".equals(over)) && under != null
                        && !"minecraft:air".equals(under);
                    if (post) {
                        posts++;
                        postFloors.add((y - 1) / 8);
                        sound &= forge.containsKey(com.hex.kagune.control.npc.HqRules.YAW);
                    } else {
                        walkers++;
                        sound &= y <= 2; // out of doors on the plate
                    }
                }
            }
            check(sound, "every one of its people: an investigator of this building, kept, alone on its spot, a seat on a chair with room for its knees, "
                + "the rest with a floor under them and room to stand");
            check(generals == 1, "4.1.5: the General, once, on the top floor");
            check(entities.size() == 39 && seats == 21 && posts == 11 && walkers == 6, "38 investigators: 21 at desks, 11 at posts (4 outside, 7 in the halls), "
                + "6 on patrol (" + entities.size() + ", " + seats + ", " + posts + ", " + walkers + ")");
            check(seatFloors.equals(java.util.Set.of(0, 1, 2, 3, 4, 5, 6)) && postFloors.equals(java.util.Set.of(0, 1, 2, 3, 4, 5, 6)),
                "desks and a hall guard on every one of its seven floors");
            int signs = 0;
            for (String name : blocks.values()) {
                signs += name.endsWith("_sign") ? 1 : 0;
            }
            check(signs == 164, "the owner's file's signs are all there (its builder's credit among them): " + signs);
        } catch (java.io.IOException failure) {
            check(false, "read the building's files: " + failure);
        }
    }

    /** A named binary tag's payload (type {@code type}) read as plain Java: maps, lists, numbers, strings. */
    private static Object nbt(java.io.DataInputStream in, int type) throws java.io.IOException {
        switch (type) {
            case 1:
                return in.readByte();
            case 2:
                return in.readShort();
            case 3:
                return in.readInt();
            case 4:
                return in.readLong();
            case 5:
                return in.readFloat();
            case 6:
                return in.readDouble();
            case 7: {
                byte[] bytes = new byte[in.readInt()];
                in.readFully(bytes);
                return bytes;
            }
            case 8:
                return in.readUTF();
            case 9: {
                int of = in.readByte();
                int n = in.readInt();
                List<Object> list = new java.util.ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    list.add(nbt(in, of));
                }
                return list;
            }
            case 10: {
                java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
                for (int kind = in.readByte(); kind != 0; kind = in.readByte()) {
                    String name = in.readUTF();
                    map.put(name, nbt(in, kind));
                }
                return map;
            }
            case 11: {
                List<Object> ints = new java.util.ArrayList<>();
                for (int n = in.readInt(); n > 0; n--) {
                    ints.add(in.readInt());
                }
                return ints;
            }
            case 12: {
                List<Object> longs = new java.util.ArrayList<>();
                for (int n = in.readInt(); n > 0; n--) {
                    longs.add(in.readLong());
                }
                return longs;
            }
            default:
                throw new java.io.IOException("tag " + type);
        }
    }

    /**
     * 4.0.2 (the owner: "we're gonna replace the Ougiri hideout with another schematic ... While Aogiri Tree only has one hideout, there's
     * multiple villages that can spawn, although they're as rare as a normal village ... at least two guarding the entrances, always in the
     * same spot ... a little message with multiple choices. This only appears once ... If he doesn't like it, they'll attack you ... wait
     * let's say 5 MC days"): the mansion as the hideout, the world's one; the villages as rare as Minecraft's; the gate's question.
     */
    static void mansion() {
        com.hex.kagune.control.village.SiteMaps.Site hideout = com.hex.kagune.control.village.SiteMaps.of("aogiri_hideout");
        check(hideout != null && hideout.width() == 272 && hideout.depth() == 263 && hideout.route().length >= 40,
            "the hideout is the owner's mansion (272 x 263, all a start chunk reaches), its patrol a loop round the house");
        try {
            String set = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/resources/data/hexkagune/worldgen/structure_set/aogiri_hideout.json")));
            String villages = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/resources/data/hexkagune/worldgen/structure_set/villages.json")));
            // 4.1.1 (the owner: "the mansion is currently happily resting at the very bottom of the ocean"): a spread of offers, the
            // world's one hideout at the first on dry land (DryLand.hideoutOffers reads the same numbers as the game).
            check(set.contains("\"minecraft:random_spread\"") && set.contains("\"spacing\": " + com.hex.kagune.control.village.DryLand.SPACING + ",")
                && set.contains("\"separation\": " + com.hex.kagune.control.village.DryLand.SEPARATION + ",")
                && set.contains("\"salt\": " + com.hex.kagune.control.village.DryLand.SALT) && !set.contains("\"spread_type\"")
                && !set.contains("frequency") && !set.contains("exclusion_zone"), "the hideout's offers are the game's own (a linear random spread, the same numbers)");
            // 4.0.5 (the owner: "also game crash upon creating world", "Failed to load registries"): every structure set's placement has
            // every field the game's codec requires (the hideout's ring had no salt: no world could be made), names structures that exist
            // and biome tags that exist.
            java.nio.file.Path data = java.nio.file.Path.of("src/main/resources/data/hexkagune");
            try (java.util.stream.Stream<java.nio.file.Path> sets = java.nio.file.Files.list(data.resolve("worldgen/structure_set"))) {
                for (java.nio.file.Path file : sets.sorted().toList()) {
                    String json = new String(java.nio.file.Files.readAllBytes(file));
                    String name = file.getFileName().toString();
                    java.util.List<String> needs = new java.util.ArrayList<>(List.of("\"salt\":", "\"structures\":", "\"structure\":", "\"weight\":"));
                    if (json.contains("\"minecraft:concentric_rings\"")) {
                        needs.addAll(List.of("\"distance\":", "\"spread\":", "\"count\":", "\"preferred_biomes\":"));
                    } else {
                        check(json.contains("\"minecraft:random_spread\""), name + ": a placement the game knows");
                        needs.addAll(List.of("\"spacing\":", "\"separation\":"));
                    }
                    for (String need : needs) {
                        check(json.contains(need), name + ": its placement has " + need);
                    }
                    java.util.regex.Matcher made = java.util.regex.Pattern.compile("\"structure\": \"hexkagune:([a-z0-9_]+)\"").matcher(json);
                    while (made.find()) {
                        check(java.nio.file.Files.exists(data.resolve("worldgen/structure/" + made.group(1) + ".json")), name + ": its structure " + made.group(1) + " exists");
                    }
                    java.util.regex.Matcher tag = java.util.regex.Pattern.compile("\"#hexkagune:([a-z0-9_/]+)\"").matcher(json);
                    while (tag.find()) {
                        check(java.nio.file.Files.exists(data.resolve("tags/worldgen/biome/" + tag.group(1) + ".json")), name + ": its biome tag " + tag.group(1) + " exists");
                    }
                }
            }
            check(villages.contains("hexkagune:village_1") && villages.contains("hexkagune:village_2") && villages.contains("\"spacing\": 34")
                && !java.nio.file.Files.exists(java.nio.file.Path.of("src/main/resources/data/hexkagune/worldgen/structure_set/village_1.json")),
                "the villages as rare as Minecraft's (one set, its spacing)");
            String brain = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/GhoulNpcBrain.java")));
            check(brain.contains("AogiriGuard.idle(npc, mind, level, now)") && brain.contains("AogiriGuard.unwelcome(npc, player, now)"),
                "the members at their work, and a player kept out is gone for");
            String structures = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/ControlStructures.java")));
            check(structures.contains("return claimed(HIDEOUT, context, () -> {") && structures.contains("if (!offered(context, hideoutSpread(context))) {")
                && structures.contains("int middle = dryGround(context, ground(context), minX + sizeX / 2 - span / 2, minZ + sizeZ / 2 - span / 2, span);")
                && structures.contains("if ((CCG.equals(this.site.id()) || unique(this.site)) && onPurpose(context)) {")
                && structures.contains("return context.f_226630_().test(null);"),
                "the world's one hideout, at the first of its set's spots on dry land the world makes (/place builds it anywhere)");
        } catch (java.io.IOException failure) {
            check(false, "read the sources: " + failure);
        }
        check(com.hex.kagune.control.server.AogiriGuard.BAN == 5L * 24000L, "a wrong answer keeps you out five days");
        java.util.UUID one = new java.util.UUID(1L, 2L);
        java.util.UUID two = new java.util.UUID(3L, 4L);
        java.util.List<com.hex.kagune.control.npc.AogiriGate.Answer> a = com.hex.kagune.control.npc.AogiriGate.answers(one);
        check(a.size() == 6 && a.stream().filter(com.hex.kagune.control.npc.AogiriGate.Answer::welcome).count() == 3
            && a.equals(com.hex.kagune.control.npc.AogiriGate.answers(one)) && !a.equals(com.hex.kagune.control.npc.AogiriGate.answers(two)),
            "at the gate: six answers, three he takes and three he doesn't, the same each time for the same player, another's in another order");
        check(com.hex.kagune.control.npc.AogiriGate.ANSWERS.stream().allMatch(x -> !x.label().isEmpty() && !x.reply().isEmpty()),
            "every answer has its words and his reply");
    }
}
