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
        identity();
        blades();
        tempers();
        spawning();
        strength();
        look();
        moves();
        bonds();
        favours();
        talk();
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
        check(wild.tier() == 2 && wild.skin() == NpcLook.SKINS.length - 1 && wild.name() < NpcNames.count() && wild.seed() <= 0xFFFF,
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

    /** The stronger the player, the stronger the ghoul; each tier tougher; the string hurts but does not shred. */
    static void strength() {
        Random random = new Random(11);
        for (int mastery : new int[] {0, 500, 1000}) {
            int[] tiers = new int[3];
            for (int i = 0; i < 20000; i++) {
                tiers[NpcRules.tier(mastery, random.nextDouble())]++;
            }
            double strong = tiers[2] / 20000.0;
            double expected = 0.05 + 0.35 * mastery / 1000.0;
            check(Math.abs(strong - expected) < 0.02, "strong ones at mastery " + mastery + ": " + strong);
        }
        for (int tier = 1; tier < NpcIdentity.TIERS; tier++) {
            check(NpcRules.health(tier) > NpcRules.health(tier - 1) && NpcRules.damage(tier) > NpcRules.damage(tier - 1)
                && NpcRules.armor(tier) > NpcRules.armor(tier - 1), "tier " + tier + " is tougher");
            check(NpcRules.string(tier) > NpcRules.string(tier - 1), "tier " + tier + " strings longer");
        }
        check(NpcRules.health(0) > 20.0F, "a ghoul outlasts a player's health");
        for (int tier = 0; tier < NpcIdentity.TIERS; tier++) {
            int limbs = GraftStage.TRANSPLANT.limbs(NpcRules.tailMastery(tier));
            check(limbs == (tier == 0 ? 1 : 2), "tier " + tier + ": " + limbs + " tails");
            // One string and the pause after it: the damage a second it averages (before armour and difficulty).
            GraftMove move = GraftMove.firstStrike(GraftStage.TRANSPLANT);
            double power = 0.0;
            int ticks = 0;
            for (int i = 0; i < NpcRules.string(tier); i++) {
                for (GraftMove.Beat beat : move.beats(limbs)) {
                    power += beat.power();
                }
                ticks += move.ticks();
                move = GraftMove.nextStrike(move, GraftStage.TRANSPLANT);
            }
            ticks += (NpcRules.PAUSE_LEAST + NpcRules.PAUSE_MOST) / 2;
            double perSecond = power * NpcRules.damage(tier) * 20.0 / ticks;
            check(perSecond >= 2.0 && perSecond <= 8.0, "tier " + tier + " lashes for " + perSecond + " a second");
            double shots = NpcRules.string(tier) * NpcRules.damage(tier) * 20.0
                / (NpcRules.string(tier) * NpcRules.SHOT_EVERY + 6 + (NpcRules.PAUSE_LEAST + NpcRules.PAUSE_MOST) / 2.0);
            check(shots >= 2.0 && shots <= 8.0, "tier " + tier + " shoots for " + shots + " a second (and burns)");
            for (double r : new double[] {0.0, 0.49, 0.5, 0.99}) {
                int meat = NpcRules.meat(tier, r);
                check(meat >= 1 + (tier == 2 ? 1 : 0) && meat <= 1 + tier, "tier " + tier + " leaves " + meat + " Ghoul Meat");
            }
        }
        check(NpcRules.wingMastery(2) < 450, "the wings stay the plain twin wings (no crystal, no embers)");
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
}
