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
        // Every trade's wares can be given, and said.
        for (com.hex.kagune.control.village.HumanLife.Job job : com.hex.kagune.control.village.HumanLife.Job.values()) {
            check(!job.wares.isEmpty(), job + ": something to give");
            for (com.hex.kagune.control.village.VillageShop.Offer ware : job.wares) {
                String thing = com.hex.kagune.control.village.HumanApproach.thing(ware.name(), com.hex.kagune.control.village.HumanApproach.gives(ware.count()));
                check(!thing.isBlank() && !thing.startsWith("a a") && !thing.startsWith("a e") && !thing.startsWith("a i"), job + ": " + thing);
            }
        }
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
}
