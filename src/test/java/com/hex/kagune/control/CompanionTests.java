package com.hex.kagune.control;

import com.hex.kagune.control.client.animation.EtoPose;
import com.hex.kagune.control.client.animation.RigPose;
import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.EtoInitiative;
import com.hex.kagune.control.npc.EtoMood;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.npc.EtoTalk;
import com.hex.kagune.control.npc.EtoTopics;
import com.hex.kagune.control.npc.EtoVoice;
import com.hex.kagune.control.npc.HandSway;
import com.hex.kagune.control.npc.NpcBonds;
import com.hex.kagune.control.npc.NpcTalk;
import com.hex.kagune.control.npc.Persona;
import com.hex.kagune.control.npc.Romance;
import com.hex.kagune.control.npc.Script;
import com.hex.kagune.control.npc.Teaching;
import com.hex.kagune.control.quest.Gate;
import com.hex.kagune.control.quest.Quest;
import com.hex.kagune.control.quest.QuestBook;
import com.hex.kagune.control.quest.QuestLog;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 1.3.13 on: the companions (pure rules, no game): hands held alive; the personas every one-of-a-kind NPC is built from (Eto, Touka,
 * Ken), their lines, their lessons.
 */
public final class CompanionTests {
    static int passed;
    static final List<String> FAILURES = new ArrayList<>();

    static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    /** 3.2.3: never frozen in a kiss: a held pose with nothing holding it lets go; a blush with nobody to kiss is a shy one. */
    static void stranded() {
        long alone = com.hex.kagune.control.npc.EtoGesture.ALONE;
        com.hex.kagune.control.npc.EtoGesture blush = com.hex.kagune.control.npc.EtoGesture.BLUSH;
        com.hex.kagune.control.npc.EtoGesture kiss = com.hex.kagune.control.npc.EtoGesture.KISS;
        com.hex.kagune.control.npc.EtoGesture pull = com.hex.kagune.control.npc.EtoGesture.PULL;
        com.hex.kagune.control.npc.EtoGesture hand = com.hex.kagune.control.npc.EtoGesture.HAND;
        check(blush.stranded(alone, false, false) && kiss.stranded(alone + 500L, false, false) && pull.stranded(alone, false, true),
            "a blush, a kiss or a pull with no kiss behind it lets go (the bug: she stood frozen leaning in)");
        check(!blush.stranded(alone - 1L, false, false), "a blush as a reaction is seen for a moment first");
        check(!kiss.stranded(99999L, true, false) && !blush.stranded(99999L, true, false) && !pull.stranded(99999L, true, false),
            "a kiss under way keeps its pose however long Shift is held");
        check(hand.stranded(alone, true, false) && !hand.stranded(99999L, false, true), "a hand's pose lasts while the hand is held, no longer");
        boolean timedNever = true;
        for (com.hex.kagune.control.npc.EtoGesture g : com.hex.kagune.control.npc.EtoGesture.values()) {
            if (!g.held()) {
                timedNever &= !g.stranded(99999L, false, false);
            } else {
                timedNever &= g.stranded(alone, false, false) || g == hand || g == EtoGesture.SIT;
            }
        }
        check(timedNever, "only held gestures can be stranded, and every held one is let go of when nothing holds it (4.0.9: but Ken's sit, held until he is called up)");
        check(EtoGesture.SIT.held() && EtoGesture.SIT.still() && !EtoGesture.SIT.stranded(99999L, false, false), "Ken sits until something calls him up, still");
        check(com.hex.kagune.control.client.animation.EtoPose.shown(blush, 0) == com.hex.kagune.control.npc.EtoGesture.SHY
            && com.hex.kagune.control.client.animation.EtoPose.shown(blush, 42) == blush
            && com.hex.kagune.control.client.animation.EtoPose.shown(kiss, 0) == com.hex.kagune.control.npc.EtoGesture.NONE
            && com.hex.kagune.control.client.animation.EtoPose.shown(kiss, 42) == kiss
            && com.hex.kagune.control.client.animation.EtoPose.shown(com.hex.kagune.control.npc.EtoGesture.WAVE, 0) == com.hex.kagune.control.npc.EtoGesture.WAVE,
            "drawn: a blush with nobody to kiss is shy; a kiss with nobody is nothing; anything else as it is");
    }

    /** 3.2.2: one at a time: nobody starts something with a player someone else has, nor right after they let go; their own turn never waits. */
    static void turns() {
        long gap = com.hex.kagune.control.npc.Turns.GAP;
        check(!com.hex.kagune.control.npc.Turns.waits(7, 0, 0, -1L, true), "nobody has them: no waiting");
        check(com.hex.kagune.control.npc.Turns.waits(7, 9, 0, -1L, true) && com.hex.kagune.control.npc.Turns.waits(7, 9, 0, -1L, false),
            "someone else has them now: wait (even at the door)");
        check(!com.hex.kagune.control.npc.Turns.waits(7, 7, 7, 0L, true), "the one who has them is never held back by itself");
        check(com.hex.kagune.control.npc.Turns.waits(7, 0, 9, gap - 1L, true) && !com.hex.kagune.control.npc.Turns.waits(7, 0, 9, gap, true),
            "just let go by someone else: a moment's breath, then free");
        check(!com.hex.kagune.control.npc.Turns.waits(7, 0, 9, 10L, false), "a homecoming waits only for someone with them now");
        check(!com.hex.kagune.control.npc.Turns.waits(7, 0, 7, 10L, true), "its own last turn is no reason to wait");
        check(!com.hex.kagune.control.npc.Turns.waits(0, 0, 9, gap + 5L, true) && com.hex.kagune.control.npc.Turns.waits(0, 4, 0, -1L, true),
            "whoever asks (0): only someone with them, or just gone, holds them back");
        check(gap >= 40L && gap <= 200L, "the breath between two is a few seconds: " + gap);
    }

    /**
     * 4.1.15 (the owner: "improve the making love scene"): the night's captions are each companion's own, say "he" of a man and "she" of a
     * woman (1.3.10's said "her" of everyone), follow the mood, the weather, a first night, the children down the hall, never run past
     * NightWords.MOST nor past what a screen holds wrapped, and travel with the morning's words and come apart again whole.
     */
    static void nightWords() {
        String[] keys = {"eto", "touka", "ken", "ayato", "general", "noro", "someone"};
        boolean[] women = {true, true, false, false, false, false, true};
        String[] moods = {"happy", "sulky", "jealous", "lonely", "sleepy", "giddy", "clingy", "affectionate", "hungry", "bored"};
        java.util.List<java.util.Set<String>> moments = java.util.List.of(java.util.Set.of(), java.util.Set.of("rain"), java.util.Set.of("thunder", "rain"),
            java.util.Set.of("kids"), java.util.Set.of("pregnant", "long"), java.util.Set.of("kids", "away"));
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int k = 0; k < keys.length; k++) {
            for (String mood : moods) {
                for (java.util.Set<String> moment : moments) {
                    for (int round = 0; round < 12; round++) {
                        int seed = round * 1237 + round * round * 4099;
                        for (boolean first : new boolean[] {true, false}) {
                            java.util.List<String> captions = com.hex.kagune.control.npc.NightWords.captions(keys[k], women[k], mood, moment, first, !first, seed);
                            check(captions.size() >= 3 && captions.size() <= com.hex.kagune.control.npc.NightWords.MOST, keys[k] + " " + mood + ": 3 to 5 captions");
                            for (String caption : captions) {
                                seen.add(caption);
                                check(!caption.isBlank() && caption.length() <= 140, "a caption a screen holds: " + caption);
                                if (!women[k]) {
                                    check(!caption.matches(".*\\b(She|she|Her|her|hers|herself)\\b.*"), keys[k] + "'s night never says she: " + caption);
                                } else if (!keys[k].equals("touka") && !keys[k].equals("eto")) {
                                    check(!caption.matches(".*\\b(He|he|His|his|himself)\\b.*"), keys[k] + "'s night never says he: " + caption);
                                }
                            }
                            check(!moment.contains("thunder") || captions.get(1).toLowerCase(java.util.Locale.ROOT).matches(".*(thunder|lightning).*"),
                                "a stormy night has its thunder");
                            check(!moment.contains("kids") || moment.contains("away") || captions.stream().anyMatch(c -> c.toLowerCase(java.util.Locale.ROOT).contains("down the hall")),
                                "the children asleep down the hall");
                            String packed = com.hex.kagune.control.npc.NightWords.pack(captions, "Morning.\nCoffee?");
                            check(com.hex.kagune.control.npc.NightWords.captionsOf(packed).equals(captions)
                                && com.hex.kagune.control.npc.NightWords.wordsOf(packed).equals("Morning.\nCoffee?"), "the captions and the morning travel whole");
                        }
                    }
                }
            }
        }
        check(seen.size() >= 50, "many ways to say it (" + seen.size() + ")");
        check(com.hex.kagune.control.npc.NightWords.captionsOf("Just the morning.").isEmpty()
            && com.hex.kagune.control.npc.NightWords.wordsOf("Just the morning.").equals("Just the morning."), "an old night's words are all morning");
        check(com.hex.kagune.control.npc.NightWords.captions("ken", false, "happy", java.util.Set.of(), true, false, 64).stream()
            .anyMatch(c -> c.contains("So are his")), "a first night with him: his hands shake too");
        for (String mood : moods) {
            check(com.hex.kagune.control.npc.EtoGesture.valueOf(com.hex.kagune.control.npc.NightWords.waking(mood)) != null, "a waking gesture for " + mood);
        }
    }

    public static void main(String[] args) {
        nightWords();
        hands();
        turns();
        stranded();
        personas();
        lessons();
        talk();
        courtship();
        proofs();
        oneSpouse();
        jealous();
        tolerance();
        moods();
        moodKisses();
        poseFade();
        kissBlush();
        wed();
        poses();
        ken();
        touka();
        toukaWingsShow();
        if (!FAILURES.isEmpty()) {
            FAILURES.stream().limit(40).forEach(failure -> System.err.println("FAILED: " + failure));
            System.err.println(FAILURES.size() + " of " + (passed + FAILURES.size()) + " checks failed");
            System.exit(1);
        }
        System.out.println("Companion tests: " + passed + " checks passed");
    }

    /** 1.3.19: Ken fights in Peace, every one of its moves his, with Eto's fists, kick, flips and a dash; Touka and Eto never wear it. */
    static void ken() {
        check(com.hex.kagune.control.npc.Persona.KEN.finalKakuja() == com.hex.kagune.control.king.KingForm.PEACE, "Ken fights in Peace");
        check(com.hex.kagune.control.npc.Persona.ETO.finalKakuja() == com.hex.kagune.control.king.KingForm.NONE
            && com.hex.kagune.control.npc.Persona.TOUKA.finalKakuja() == com.hex.kagune.control.king.KingForm.NONE, "nobody else wears a final kakuja");
        check(com.hex.kagune.control.npc.Persona.KEN.health() > com.hex.kagune.control.npc.Persona.ETO.health(), "Ken is the strongest of them");
        for (int slot = 0; slot < 6; slot++) {
            com.hex.kagune.control.npc.KenRules.Act act = com.hex.kagune.control.npc.KenRules.ability(slot);
            check(act != null && com.hex.kagune.control.npc.KenRules.slot(act) == slot, "Peace's key " + slot + " is one of his acts and back");
            int ticks = com.hex.kagune.control.king.PeaceRules.Ability.bySlot(slot).cooldown();
            check(com.hex.kagune.control.npc.KenRules.cooldown(ticks) < ticks, "his wait for key " + slot + " is shorter than a player's");
        }
        // Over many fights, every move of his comes up.
        java.util.EnumSet<com.hex.kagune.control.npc.KenRules.Act> seen = java.util.EnumSet.noneOf(com.hex.kagune.control.npc.KenRules.Act.class);
        java.util.Random random = new java.util.Random(19);
        long now = 10_000L;
        for (int i = 0; i < 20_000; i++) {
            long[] w = new long[11];
            for (int k = 0; k < w.length; k++) {
                w[k] = random.nextDouble() < 0.5 ? 0L : now + 100L;
            }
            com.hex.kagune.control.npc.KenRules.Ready ready = new com.hex.kagune.control.npc.KenRules.Ready(w[0], w[1], w[2], w[3], w[4], w[5], w[6], w[7],
                w[8], w[9], w[10]);
            double gap = random.nextDouble() * 24.0;
            com.hex.kagune.control.npc.KenRules.Act act = com.hex.kagune.control.npc.KenRules.choose(gap, random.nextDouble() < 0.9,
                random.nextDouble() < 0.85, (float)random.nextDouble(), random.nextBoolean(), random.nextInt(4), now, ready, random.nextDouble());
            seen.add(act);
            int slot = com.hex.kagune.control.npc.KenRules.slot(act);
            check(slot < 0 || w[slot] <= now, "he never throws an ability that isn't ready (" + act + ")");
            check(act != com.hex.kagune.control.npc.KenRules.Act.STRIKE || gap <= com.hex.kagune.control.npc.KenRules.REACH,
                "his blades' chain only in reach");
            check(act != com.hex.kagune.control.npc.KenRules.Act.HORIZON || gap <= com.hex.kagune.control.king.PeaceRules.HORIZON_RANGE,
                "the Final Horizon only on what is in its reach");
            check(act != com.hex.kagune.control.npc.KenRules.Act.WALTZ || gap <= com.hex.kagune.control.king.PeaceRules.WALTZ_RANGE,
                "the Waltz only on what is in its reach");
        }
        check(seen.size() == com.hex.kagune.control.npc.KenRules.Act.values().length, "every one of his moves comes up in his fights (" + seen + ")");
        // Peace comes over him at once: the very first thing in a fight, whatever the distance (his server side wears it before any act).
        check(com.hex.kagune.control.npc.KenRules.OFF_AFTER >= 100L, "it stays on him a while after a fight, never flickering off between blows");
    }

    /** 1.3.19: Touka's wings: a lesser pair, crystal spikes (a volley, a fan, the barrage), her fists up close; only hers. */
    static void touka() {
        check(com.hex.kagune.control.npc.Persona.TOUKA.wings() && !com.hex.kagune.control.npc.Persona.ETO.wings()
            && !com.hex.kagune.control.npc.Persona.KEN.wings(), "only Touka fights with wings of her own");
        check(com.hex.kagune.control.npc.WingRules.MASTERY < com.hex.kagune.control.ukaku.WingForm.KAKUJA.requiredMastery()
            && com.hex.kagune.control.npc.WingRules.MASTERY > com.hex.kagune.control.ukaku.WingForm.TWIN.requiredMastery(), "her wing is a lesser one");
        check(com.hex.kagune.control.npc.WingRules.SHOT < 6.0F && com.hex.kagune.control.npc.WingRules.BARRAGE < com.hex.kagune.control.npc.WingRules.SHOT,
            "her spikes sting; nowhere near Ken's blades");
        java.util.EnumSet<com.hex.kagune.control.npc.WingRules.Act> seen = java.util.EnumSet.noneOf(com.hex.kagune.control.npc.WingRules.Act.class);
        java.util.Random random = new java.util.Random(7);
        long now = 5_000L;
        for (int i = 0; i < 5_000; i++) {
            double gap = random.nextDouble() * 26.0;
            boolean sees = random.nextDouble() < 0.9;
            long shot = random.nextBoolean() ? 0L : now + 5L;
            long fan = random.nextBoolean() ? 0L : now + 50L;
            long barrage = random.nextBoolean() ? 0L : now + 90L;
            com.hex.kagune.control.npc.WingRules.Act act = com.hex.kagune.control.npc.WingRules.choose(gap, sees, now, shot, fan, barrage, random.nextDouble());
            seen.add(act);
            check(act == com.hex.kagune.control.npc.WingRules.Act.NONE || gap >= com.hex.kagune.control.npc.WingRules.NEAR && sees,
                "her spikes only at what she sees, never point blank (her fists there)");
            check(act != com.hex.kagune.control.npc.WingRules.Act.BARRAGE || barrage <= now, "the barrage only when it is ready");
            check(act != com.hex.kagune.control.npc.WingRules.Act.FAN || fan <= now, "the fan only when it is ready");
        }
        check(seen.size() == com.hex.kagune.control.npc.WingRules.Act.values().length, "every one of her wing moves comes up (" + seen + ")");
        check(Math.abs(com.hex.kagune.control.npc.WingRules.fan(0, 5) + com.hex.kagune.control.npc.WingRules.FAN_SPREAD) < 1.0E-9
            && Math.abs(com.hex.kagune.control.npc.WingRules.fan(2, 5)) < 1.0E-9, "the fan spreads evenly about her aim");
    }

    static void hands() {
        // Standing still: she rests at their side, the hands sway anyway (never still for long).
        HandSway still = new HandSway();
        for (int i = 0; i < 200; i++) {
            still.step(0.0, 0.0);
        }
        check(Math.abs(still.trail) < 1.0E-6 && Math.abs(still.out) < 1.0E-6, "standing still, she rests right at their side");
        double low = Double.MAX_VALUE;
        double high = -Double.MAX_VALUE;
        for (int t = 0; t < 400; t++) {
            double sway = HandSway.sway(t);
            low = Math.min(low, sway);
            high = Math.max(high, sway);
        }
        check(high - low > 0.15 && high <= HandSway.SWAY + 1.0E-9 && low >= -HandSway.SWAY - 1.0E-9,
            "the joined hands sway forward and back, about a dozen degrees all told, never past their bound: " + low + ".." + high);

        // Walking: she trails behind, settled, about a third of a block; sprinting further; never past the bound.
        HandSway walk = new HandSway();
        double most = 0.0;
        for (int i = 0; i < 200; i++) {
            walk.step(0.216, 0.0);
            most = Math.max(most, walk.trail);
        }
        check(walk.trail > 0.3 && walk.trail < 0.5, "walking, she trails a little behind, pulled by the hand: " + walk.trail);
        check(most <= HandSway.MAX_TRAIL + 0.15, "the spring never throws her past its bound: " + most);
        HandSway run = new HandSway();
        for (int i = 0; i < 200; i++) {
            run.step(0.28, 0.0);
        }
        check(run.trail > walk.trail && run.trail <= HandSway.MAX_TRAIL + 1.0E-9, "sprinting, a little further behind, still within reach: " + run.trail);

        // Stopping: her own momentum carries her a touch ahead, then she settles back at their side.
        double ahead = 0.0;
        for (int i = 0; i < 120; i++) {
            walk.step(0.0, 0.0);
            ahead = Math.min(ahead, walk.trail);
        }
        check(ahead < -0.01, "stopping, she swings a step ahead before she settles (her momentum): " + ahead);
        check(Math.abs(walk.trail) < 0.01, "and settles back at their side: " + walk.trail);

        // Turning: at their right she swings out turning left (the outside) and in turning right.
        HandSway left = new HandSway();
        HandSway right = new HandSway();
        for (int i = 0; i < 12; i++) {
            left.step(0.2, -9.0);
            right.step(0.2, 9.0);
        }
        check(left.out > 0.05 && right.out < -0.05, "turning away from her she swings out wide, toward her she comes in: " + left.out + " / " + right.out);

        // The arms: the holder's swings back as she trails, hers forward, both by the same reach (each half the gap), same sway.
        float[] holder = HandSway.arm(0.4, 0.0, 0.0, true, 1.0);
        float[] her = HandSway.arm(0.4, 0.0, 0.0, false, 1.0);
        check(holder[0] > 0.2F && her[0] < -0.2F && Math.abs(holder[0] + her[0] + 2.0 * HandSway.sway(0.0)) < 1.0E-5,
            "trailing, the holder's arm reaches back and hers forward, meeting halfway: " + holder[0] + " / " + her[0]);
        float[] smaller = HandSway.arm(0.4, 0.0, 0.0, false, 0.9);
        check(smaller[0] < her[0], "a shorter arm turns further to reach its half");
        float[] swayed = HandSway.arm(0.0, 0.0, HandSway.SLOW_QUARTER, true, 1.0);
        float[] swayedHer = HandSway.arm(0.0, 0.0, HandSway.SLOW_QUARTER, false, 1.0);
        check(swayed[0] < -0.05F && Math.abs(swayed[0] - swayedHer[0]) < 1.0E-5, "the sway takes both hands forward together, the same way");
        check(HandSway.pulled(0.6) > HandSway.pulled(0.3) && HandSway.pulled(-0.2) == 0.0 && HandSway.pulled(5.0) <= 0.16 + 1.0E-9,
            "pulled along she leans in a little, more the further she trails, never when ahead");
        HandSway bad = new HandSway();
        bad.step(Double.NaN, 4.0);
        bad.step(0.1, Double.POSITIVE_INFINITY);
        check(Double.isFinite(bad.trail) && Double.isFinite(bad.out), "nothing unreadable ever reaches the spring");
        check(Math.abs(HandSway.reach(50.0, 1.0)) < Math.PI / 2.0, "a gap too long for the arm turns it most of the way, never wraps round");
    }

    /** The build's own name filter (build.py check_names): none of these may reach the jar. */
    // 4.0.0: Ayato is no longer kept out (the owner asked for him by name).
    static final Pattern FORBIDDEN = Pattern.compile("Kaneki|KANEKI|kaneki|Furuta|FURUTA|furuta");

    static void personas() {
        Set<String> keys = new HashSet<>();
        for (Persona p : Persona.ALL) {
            check(keys.add(p.key()) && Persona.of(p.key()) == p && Persona.known(p.key()), p.key() + ": one key, found by it");
            check(!FORBIDDEN.matcher(p.name()).find() && !FORBIDDEN.matcher(p.key()).find(), p.key() + ": its name passes the name filter");
            // 4.0.0: Noro taller than a player (the owner: "Noro is taller than the average player").
            check(p.size() > 0.7F && p.size() <= (p == Persona.NORO ? 1.25F : 1.0F) && p.health() > 100.0F && p.armor() > 0.0, p.key() + ": a sane body");
            for (EtoMood mood : EtoMood.values()) {
                check(p.idles(mood).length > 0, p.key() + ": something to do " + mood);
            }
            check(p.anyIdle().length >= 5 && p.script() != null && p.colour().startsWith("\u00a7"), p.key() + ": idles, words, a colour");
        }
        check(Persona.ALL.get(0) == Persona.ETO && Persona.ALL.indexOf(Persona.TOUKA) == 1 && Persona.ALL.indexOf(Persona.KEN) == 2,
            "the order the talk screen's faces are sent by (-1, -2, -3) never changes");
        check(Persona.of("nobody") == Persona.ETO && !Persona.known("nobody"), "an unknown key falls back to Eto and is not a companion");
        check(Persona.ETO.owlKit() && !Persona.TOUKA.owlKit() && !Persona.KEN.owlKit(), "only Eto fights with the Owl DNA kit");
        check(Persona.ETO.curves() == 1.0F && Persona.ETO.build() == 1.0F && Persona.TOUKA.curves() > 1.2F && Persona.TOUKA.curves() < 1.6F
            && Persona.TOUKA.build() == 1.0F, "Touka on the same female body as Eto, curvier (1.3.17: legs and chest bigger); Eto as she was");
        check(Persona.ETO.female() && Persona.TOUKA.female() && !Persona.KEN.female() && Persona.TOUKA.slim() && Persona.KEN.slim(),
            "Eto and Touka on the female body, Ken a man's; all on the slim arms their skins are made for");
        check(!Persona.ETO.pecks() && Persona.TOUKA.pecks() && Persona.KEN.pecks(), "Touka and Ken give a quick kiss of their own; Eto waits for hers");
        check(Persona.KEN.leads() && !Persona.TOUKA.leads() && !Persona.ETO.leads(), "Ken leans down into a kiss; the women lean up");
        check(Persona.KEN.likes(true) && !Persona.KEN.likes(false) && Persona.TOUKA.likes(false) && Persona.ETO.likes(true),
            "Ken is only interested in women; Eto and Touka in anyone");
        check(Persona.TOUKA.kissing() < Persona.ETO.kissing() && Persona.TOUKA.forward() < 1.0 && Persona.KEN.forward() > 1.0,
            "Touka would rather die than ask first; Ken reaches for his partner more than anyone");
        check(Persona.TOUKA.blushes(EtoGesture.KISS) && Persona.TOUKA.blushes(EtoGesture.HAND) && !Persona.ETO.blushes(EtoGesture.KISS)
            && Persona.ETO.blushes(EtoGesture.BLUSH) && !Persona.TOUKA.blushes(EtoGesture.CROSS), "Touka goes pink at anything sweet, not at a glare");
        check(Persona.TOUKA.errandItem("minecraft:cocoa_beans") && !Persona.TOUKA.errandItem("minecraft:book") && Persona.KEN.errandItem("minecraft:book")
            && Persona.ETO.errandItem("minecraft:book"), "their first errands: beans for Touka, books for Ken and Eto");
        check(Persona.TOUKA.flower("minecraft:rabbit_foot") && !Persona.TOUKA.flower("minecraft:poppy") && Persona.KEN.flower("minecraft:white_tulip")
            && !Persona.KEN.flower("minecraft:poppy") && Persona.ETO.flower("minecraft:poppy"), "their last gifts: a rabbit's foot, a white flower, any flower");
        check(EtoGesture.CROSS.ordinal() == EtoGesture.HAND.ordinal() + 1 && EtoGesture.HAND.ordinal() > EtoGesture.LAUGH.ordinal()
            && EtoGesture.STEAL.ordinal() == EtoGesture.PECK.ordinal() + 1 && EtoGesture.TIDY.ordinal() == EtoGesture.COOK.ordinal() - 1
            && EtoGesture.FLINCH.ordinal() == EtoGesture.COOK.ordinal() + 1 && EtoGesture.STOKE.ordinal() == EtoGesture.AWE.ordinal() + 1
            && EtoGesture.JEALOUS.ordinal() == EtoGesture.SERVE.ordinal() + 1 && EtoGesture.POSE.ordinal() == EtoGesture.JEALOUS.ordinal() + 1
            && EtoGesture.SIT.ordinal() == EtoGesture.TILT.ordinal() + 1 && EtoGesture.SIT.ordinal() == EtoGesture.values().length - 1,
            "new gestures only ever at the end (sent by ordinal; 4.0.2: Ayato's and Noro's after JEALOUS; 4.0.9: Ken's sit after them)");
        // 4.0.9 (the owner: "make sure he sits on the top"): only Ken sits at home; seated, his hips on the floor (a drop of 10 pixels), one
        // leg out and the other knee up, held still.
        check(Persona.KEN.sitsAtHome() && !Persona.ETO.sitsAtHome() && !Persona.TOUKA.sitsAtHome() && !Persona.AYATO.sitsAtHome() && !Persona.NORO.sitsAtHome(),
            "Ken sits at home, the others as they were");
        // 4.0.9 (the owner: "I sent you a replacement for the Ken tower as well, make sure he sits on the top."): their tower, 73 wide, its
        // repeating floors thinned to stand under a 1.20.1 world's top from the ground (238 tall), Ken on its top deck (its floor the
        // highest that runs across it), looking out.
        try {
            byte[] tower = new java.util.zip.GZIPInputStream(java.nio.file.Files.newInputStream(java.nio.file.Path.of(
                "src/main/resources/data/hexkagune/structures/cyber_tower.nbt"))).readAllBytes();
            int[] size = ints(tower, "size");
            int[] ken = ints(tower, "blockPos");
            check(size != null && size[0] == 73 && size[2] == 73 && size[1] == 238 && 63 + size[1] <= 319, "Ken's tower, the owner's, standing under the world's top: "
                + java.util.Arrays.toString(size));
            check(ken != null && ken[1] > size[1] - 50 && ken[1] < size[1] - 30 && new String(tower, java.nio.charset.StandardCharsets.ISO_8859_1).contains("hexkagune:ken"),
                "Ken on its top deck: " + java.util.Arrays.toString(ken));
        } catch (java.io.IOException failure) {
            check(false, "read Ken's tower: " + failure);
        }
        // 4.1.24 (the owner: "I'm going to give you a schematic where he's going to spawn in always ... outside in the garden, like in this
        // place. Make sure also doesn't spawn like in the ground, like the tower"): the White Reaper's mansion, the owner's, 79 by 121, its
        // empty bottom layer cropped; Arima on its lawn (the ground layer, 5, under him), saved as the Reaper who lives there; an anchor on
        // the ground's layer; 1.20.1's grass for the newer game's short grass.
        try {
            byte[] mansion = new java.util.zip.GZIPInputStream(java.nio.file.Files.newInputStream(java.nio.file.Path.of(
                "src/main/resources/data/hexkagune/structures/reaper_mansion.nbt"))).readAllBytes();
            String text = new String(mansion, java.nio.charset.StandardCharsets.ISO_8859_1);
            int[] size = ints(mansion, "size");
            int[] arima = ints(mansion, "blockPos");
            check(size != null && size[0] == 79 && size[1] == 28 && size[2] == 121, "the mansion, the owner's: " + java.util.Arrays.toString(size));
            check(arima != null && arima[1] == 6 && text.contains("hexkagune:ccg_investigator") && text.contains("HexCcgAgentMansion")
                && text.contains("HexCcgAgentRankLevel") && text.contains("ForgeData"), "Arima on its lawn, the Reaper at home: " + java.util.Arrays.toString(arima));
            check(text.contains("hexkagune:mansion_anchor") && !text.contains("minecraft:short_grass"), "anchored on its ground; only 1.20.1's blocks");
        } catch (java.io.IOException failure) {
            check(false, "read the White Reaper's mansion: " + failure);
        }
        // 4.1.48 (the owner sent circus.schematic; chose "One per world + clowns"): the circus tent, the owner's (77 x 46 x 49) on a floor of
        // dirt path laid under it (a layer of its own: 47 tall), anchored at the floor's middle; four Clowns inside on the floor, each saved
        // as one of the circus's in its own mask, all of one gang, kept with the world; only 1.20.1's blocks.
        try {
            byte[] tent = new java.util.zip.GZIPInputStream(java.nio.file.Files.newInputStream(java.nio.file.Path.of(
                "src/main/resources/data/hexkagune/structures/circus.nbt"))).readAllBytes();
            String text = new String(tent, java.nio.charset.StandardCharsets.ISO_8859_1);
            int[] size = ints(tent, "size");
            int[] first = ints(tent, "blockPos");
            check(size != null && size[0] == 77 && size[1] == 47 && size[2] == 49, "the circus tent, the owner's, on its floor: " + java.util.Arrays.toString(size));
            check(text.split("hexkagune:ghoul", -1).length == 5 && text.split("HexCircusMask", -1).length == 5 && text.split("HexGang", -1).length == 5
                && text.split("PersistenceRequired", -1).length == 6 && first != null && first[1] == 1, "four Clowns (4.1.49: and Roma) standing on its floor: "
                + java.util.Arrays.toString(first));
            check(text.contains("hexkagune:circus_anchor") && text.contains("minecraft:dirt_path") && text.contains("minecraft:red_wool")
                && text.contains("minecraft:white_wool") && !text.contains("minecraft:short_grass"), "anchored on its floor; only 1.20.1's blocks");
        } catch (java.io.IOException failure) {
            check(false, "read the circus tent: " + failure);
        }
        // 4.1.0 (the owner: "after training with Ayato (Ukaku kakuja quest) the player gains a new pull out kagune animation sort of like Ken
        // gives the player. the player lifts both hands god like and then they're unleashed."): both arms out wide, palms up, chin up, up on
        // the toes, held as the wings come out, then down again; his way before Ken's, after Noro's.
        com.hex.kagune.control.client.animation.RigPose lifted = new com.hex.kagune.control.client.animation.RigPose();
        com.hex.kagune.control.client.AyatoPullOutClient.lift(lifted, (float)com.hex.kagune.control.server.AyatoPullOut.OUT_AT);
        com.hex.kagune.control.client.animation.RigPose after = new com.hex.kagune.control.client.animation.RigPose();
        com.hex.kagune.control.client.AyatoPullOutClient.lift(after, (float)com.hex.kagune.control.client.AyatoPullOutClient.LASTS);
        check(lifted.rightArmZ > 1.0F && lifted.leftArmZ < -1.0F && lifted.rightArmY < -1.0F && lifted.leftArmY > 1.0F && lifted.headX < -0.2F && lifted.drop < 0.0F
            && Math.abs(after.rightArmZ) < 1.0E-4F && com.hex.kagune.control.server.AyatoPullOut.OUT_AT < com.hex.kagune.control.client.AyatoPullOutClient.LASTS,
            "Ayato's way: both hands lifted out wide, palms to the sky, the wings out while they are, then down");
        try {
            String path = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("src/main/java/com/hex/kagune/control/server/PathServer.java")));
            int noro = path.indexOf("if (NoroPullOut.taught(player))");
            int ayato = path.indexOf("if (AyatoPullOut.taught(player))");
            int ken = path.indexOf("if (KenPullOut.taught(player))");
            check(noro > 0 && ayato > noro && ken > ayato && com.hex.kagune.control.network.S2CUkakuFx.UNLEASH != com.hex.kagune.control.network.S2CUkakuFx.TILT,
                "taught by Ayato, the kagune comes out his way (Noro's first for a Bikaku player, Ken's crack after)");
        } catch (java.io.IOException failure) {
            check(false, "read the path's key: " + failure);
        }
        com.hex.kagune.control.client.animation.RigPose seated = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.SIT, 400.0, false);
        check(seated != null && Math.abs(seated.drop - 10.0F) < 0.01F && seated.rightLegX < -1.3F && seated.leftKnee > 1.5F && seated.legHold >= 1.0F && seated.armHold >= 1.0F,
            "seated: hips to the floor, a leg out, a knee up, still");
        // 1.3.29: Touka's own ways: never round an arm; a kiss on the cheek or a kiss stolen instead; her own poses, not Eto's.
        check(Persona.ETO.clings() && Persona.KEN.clings() && !Persona.TOUKA.clings(), "Touka never winds round an arm");
        check(Persona.TOUKA.unasked(EtoGesture.CLING) == EtoGesture.PECK && Persona.TOUKA.unasked(EtoGesture.DANCE) == EtoGesture.STEAL
            && Persona.ETO.unasked(EtoGesture.CLING) == EtoGesture.CLING, "unasked, Touka kisses a cheek or steals a kiss; Eto clings");
        check(Persona.ETO.style() == 0 && Persona.KEN.style() == 1 && Persona.TOUKA.style() == 2, "each holds someone their own way");
        // 1.3.30: a child in the arms is cradled the whole time it is held (it faded out with its gesture's time, then came back).
        for (double held = 5.0; held < 4000.0; held += 37.0) {
            com.hex.kagune.control.client.animation.RigPose cradle = com.hex.kagune.control.client.animation.EtoPose.cradle(held, held);
            check(cradle.rightArmX < -0.9F && cradle.leftArmX < -0.9F, "still cradling the child after " + held + " ticks");
        }
        check(com.hex.kagune.control.client.animation.EtoPose.cradle(Double.MAX_VALUE, 3.0).rightArmX < -0.9F, "cradling a child picked up long ago");
        check(com.hex.kagune.control.client.animation.EtoPose.cradle(0.0, 0.0).rightArmX > -0.1F, "picked up: the arms ease into it");
        for (int variant = 0; variant < 3; variant++) {
            // 1.3.30 (the owner): waiting for the kiss she only leans in, her arms as they are; the hand comes up once she is kissed.
            com.hex.kagune.control.client.animation.RigPose waiting = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.KISS, 20.0, 2, variant);
            check(waiting != null && waiting.lean > 0.3F && Math.abs(waiting.rightArmX) < 0.3F && Math.abs(waiting.leftArmX) < 0.3F
                && waiting.rightArmHold == 0.0F && waiting.leftArmHold == 0.0F, "Touka waiting for a kiss: leaning in, no arm thrown forward (" + variant + ")");
            com.hex.kagune.control.client.animation.RigPose kissed = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.BLUSH, 20.0, 2, variant);
            check(kissed != null && kissed.rightArmX < -1.5F && kissed.leftArmX > -0.5F && kissed.lean > 0.3F,
                "Touka kissed: one hand up to them, the other at her side, still leaning in (" + variant + ")");
            com.hex.kagune.control.client.animation.RigPose first = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.BLUSH, 0.0, 2, variant);
            check(first != null && first.rightArmX > -0.3F && first.lean >= 0.4F, "the moment she is kissed: her lean kept, her hand not up yet (" + variant + ")");
            com.hex.kagune.control.client.animation.RigPose eto = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.KISS, 20.0, 0, variant);
            check(eto != null && eto.rightArmX > 0.0F && eto.leftArmX > 0.0F, "Eto's kiss: hands behind her back (" + variant + ")");
        }
        com.hex.kagune.control.client.animation.RigPose a = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.BLUSH, 20.0, 2, 0);
        com.hex.kagune.control.client.animation.RigPose b = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.BLUSH, 20.0, 2, 1);
        check(a.rightArmX != b.rightArmX, "her kiss not the same every time");
        com.hex.kagune.control.client.animation.RigPose stealIn = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.STEAL, 12.0, 2, 0);
        com.hex.kagune.control.client.animation.RigPose stealOut = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.STEAL, 50.0, 2, 0);
        check(stealIn.lean > 0.3F && stealOut.rightArmZ > 0.6F && stealOut.leftArmZ < -0.6F && Math.abs(stealOut.headY) > 0.4F,
            "a kiss stolen: in close, then arms crossed, looking away");
    }

    static void lessons() {
        Set<String> ids = new HashSet<>();
        for (Teaching.Lesson lesson : Teaching.ALL) {
            check(ids.add(lesson.id()) && Teaching.byId(lesson.id()) == lesson, lesson.id() + ": one id");
            // 4.1.3: a fighting style's lesson has no path of its own (any kagune learns it). 4.1.7: nor do the General's (Kick Master's road).
            check(Persona.known(lesson.teacher()) && (lesson.path() != null) == !lesson.gate().id().startsWith("style_"), lesson.id() + ": a teacher and a path");
            check(QuestBook.openers(lesson.gate()).isEmpty(), lesson.gate().id() + ": no quest opens it any more, its teacher does");
            check(Teaching.taught(lesson.gate()) && Teaching.teaching(lesson.gate()) == lesson, lesson.gate().id() + ": taught by one lesson");
            check(!Teaching.gatedText(lesson.gate(), Persona.of(lesson.teacher()).name()).isBlank(), lesson.id() + ": the game says who teaches it");
        }
        for (Quest quest : QuestBook.all()) {
            for (Gate gate : quest.reward().gates()) {
                check(!Teaching.taught(gate), quest.id() + ": opens no gate a companion teaches (" + gate.id() + ")");
            }
        }
        // 4.0.0: the first kakuja is Ayato's now (his three: two of respect, then it); Noro teaches the Bikaku (five).
        check(Teaching.of("touka").size() == 2 && Teaching.of("ken").size() == 6 && Teaching.of("eto").isEmpty() && Teaching.of("ayato").size() == 4
            && Teaching.of("noro").size() == 5, "Touka two lessons, Ken six, Ayato four (4.1.3: Speedster), Noro five");
        check(Teaching.teaching(Gate.UKAKU_KAKUJA).teacher().equals("ayato") && Teaching.teaching(Gate.UKAKU_KAKUJA).kind() == Teaching.Kind.TRIAL
            && Teaching.before(Gate.UKAKU_KAKUJA) == Gate.UKAKU_AYATO_HUNT && Teaching.before(Gate.UKAKU_AYATO_HUNT) == Gate.UKAKU_AYATO_DOVES
            && Teaching.before(Gate.UKAKU_AYATO_DOVES) == Gate.UKAKU_GATLING && Teaching.before(Gate.UKAKU_FINAL_KAKUJA) == Gate.UKAKU_KAKUJA,
            "Ayato's road: Touka's Gatling, his two proofs of respect, then the first kakuja lived through; Ken's final kakuja after it");
        check(Teaching.before(Gate.BIKAKU_WALKER) == Gate.BIKAKU_WORMS && Teaching.before(Gate.BIKAKU_KAKUJA) == Gate.BIKAKU_WALKER
            && Teaching.before(Gate.BIKAKU_DEVOURING) == Gate.BIKAKU_KAKUJA && Teaching.before(Gate.BIKAKU_POTENTIAL) == Gate.BIKAKU_DEVOURING,
            "Noro's road: no stage skipped");
        for (Teaching.Lesson lesson : Teaching.of("ayato")) {
            check(Teaching.proof(lesson) != null, lesson.id() + ": Ayato's respect is earned (a proof)");
        }
        for (Teaching.Lesson lesson : Teaching.of("noro")) {
            check(Teaching.proof(lesson) != null, lesson.id() + ": Noro's training (a proof)");
        }
        // 3.3.1: Ken's Hurl and Impale appended (the old lessons keep their saved bits) and taught first (by mastery).
        check(Teaching.bit(Teaching.byId("ken_centipede")) == 1L && Teaching.bit(Teaching.byId("ken_king")) == 2L && Teaching.bit(Teaching.byId("ken_dragon")) == 4L
            && Teaching.bit(Teaching.byId("ken_storm")) == 8L && Teaching.bit(Teaching.byId("ken_hurl")) == 16L && Teaching.bit(Teaching.byId("ken_impale")) == 32L,
            "Ken's old lessons keep their saved bits; the Hurl and the Impale take new ones");
        check(Teaching.inOrder("ken").get(0).gate() == Gate.RINKAKU_HURL && Teaching.inOrder("ken").get(1).gate() == Gate.RINKAKU_IMPALE
            && Teaching.inOrder("ken").get(2).gate() == Gate.RINKAKU_CENTIPEDE, "Ken teaches the Rubble Hurl first, then the Impale, then the centipede");
        Gate[] gates = Gate.values();
        check(Gate.RINKAKU_IMPALE.ordinal() == Gate.RINKAKU_HURL.ordinal() + 1 && gates[gates.length - 4] == Gate.STYLE_SPEEDSTER && Gate.STYLE_SPEEDSTER.ordinal() == Gate.BIKAKU_POTENTIAL.ordinal() + 1
            && Gate.UKAKU_AYATO_DOVES.ordinal() == Gate.RINKAKU_IMPALE.ordinal() + 1 && Gate.fromId("rinkaku_hurl") == Gate.RINKAKU_HURL
            && Gate.fromId("rinkaku_impale") == Gate.RINKAKU_IMPALE && Gate.RINKAKU_HURL.mastery() == com.hex.kagune.control.RinkakuUnlocks.HURL
            && Gate.RINKAKU_IMPALE.mastery() == com.hex.kagune.control.RinkakuUnlocks.IMPALE, "the two new gates appended, at their moves' own mastery");
        check(Teaching.of("touka").stream().allMatch(l -> l.path() == KagunePath.UKAKU), "Touka teaches the wings only");
        check(Gate.fromId("ukaku_crystal") == Gate.UKAKU_CRYSTAL && Gate.UKAKU_CRYSTAL.mastery() == com.hex.kagune.control.ukaku.UkakuRules.CRYSTAL_MASTERY,
            "Crystal Wings is a gate, at crystal mode's own mastery");
        Set<Gate> open = new HashSet<>();
        Teaching.View human = Teaching.view("touka", false, KagunePath.UKAKU, 1000, open::contains);
        check(!human.ready() && human.refusal() == Teaching.Refusal.HUMAN, "a human has nothing to learn");
        Teaching.View rinkaku = Teaching.view("touka", true, KagunePath.RINKAKU, 1000, open::contains);
        check(!rinkaku.ready() && rinkaku.refusal() == Teaching.Refusal.WRONG_PATH, "Touka knows nothing of tails");
        Teaching.View weak = Teaching.view("touka", true, KagunePath.UKAKU, 100, open::contains);
        check(!weak.ready() && weak.refusal() == Teaching.Refusal.WEAK && weak.next().gate() == Gate.UKAKU_CRYSTAL && weak.need() == 200,
            "too weak: the next lesson named, and the mastery it needs");
        Teaching.View crystal = Teaching.view("touka", true, KagunePath.UKAKU, 250, open::contains);
        check(crystal.ready() && crystal.next().gate() == Gate.UKAKU_CRYSTAL && crystal.next().kind() == Teaching.Kind.TALK, "crystals first, told");
        open.add(Gate.UKAKU_CRYSTAL);
        Teaching.View gatling = Teaching.view("touka", true, KagunePath.UKAKU, 250, open::contains);
        check(!gatling.ready() && gatling.next().gate() == Gate.UKAKU_GATLING && gatling.refusal() == Teaching.Refusal.WEAK, "then the Gatling, at 400");
        open.add(Gate.UKAKU_GATLING);
        open.add(Gate.UKAKU_KAKUJA);
        Teaching.View all = Teaching.view("touka", true, KagunePath.UKAKU, 1000, open::contains);
        check(!all.ready() && all.refusal() == Teaching.Refusal.ALL_TAUGHT && all.next() == null, "all she knows, taught");
        Teaching.View storm = Teaching.view("ken", true, KagunePath.UKAKU, 1000, open::contains);
        check(storm.ready() && storm.next().gate() == Gate.UKAKU_FINAL_KAKUJA && storm.next().kind() == Teaching.Kind.TRIAL,
            "Ken takes the wings the last of the way: lived through");
        Set<Gate> tails = new HashSet<>();
        Teaching.View feeble = Teaching.view("ken", true, KagunePath.RINKAKU, 100, tails::contains);
        check(!feeble.ready() && feeble.refusal() == Teaching.Refusal.WEAK && feeble.next().gate() == Gate.RINKAKU_HURL && feeble.need() == 250,
            "too weak for anything of Ken's: the Rubble Hurl named, at 250");
        Teaching.View hurl = Teaching.view("ken", true, KagunePath.RINKAKU, 800, tails::contains);
        check(hurl.ready() && hurl.next().gate() == Gate.RINKAKU_HURL && hurl.next().kind() == Teaching.Kind.TALK, "Ken's first: the Rubble Hurl, told");
        tails.add(Gate.RINKAKU_HURL);
        Teaching.View impale = Teaching.view("ken", true, KagunePath.RINKAKU, 800, tails::contains);
        check(impale.ready() && impale.next().gate() == Gate.RINKAKU_IMPALE, "then the Impale");
        Teaching.View impaleWeak = Teaching.view("ken", true, KagunePath.RINKAKU, 300, tails::contains);
        check(!impaleWeak.ready() && impaleWeak.refusal() == Teaching.Refusal.WEAK && impaleWeak.need() == 350, "the Impale at 350");
        tails.add(Gate.RINKAKU_IMPALE);
        Teaching.View before = Teaching.view("ken", true, KagunePath.RINKAKU, 800, tails::contains);
        check(!before.ready() && before.refusal() == Teaching.Refusal.BEFORE && before.before() == Gate.RINKAKU_FOUR,
            "the centipede only after the four tails (their quests)");
        tails.add(Gate.RINKAKU_FOUR);
        Teaching.View centipede = Teaching.view("ken", true, KagunePath.RINKAKU, 800, tails::contains);
        check(centipede.ready() && centipede.next().gate() == Gate.RINKAKU_CENTIPEDE && centipede.next().kind() == Teaching.Kind.TRIAL,
            "the centipede: the torture, lived through");
        tails.add(Gate.RINKAKU_CENTIPEDE);
        tails.add(Gate.RINKAKU_FULL_KAKUJA);
        Teaching.View dragon = Teaching.view("ken", true, KagunePath.RINKAKU, 1000, tails::contains);
        check(dragon.ready() && dragon.next().gate() == Gate.RINKAKU_DRAGON, "and last, the Dragon");
        speedster();
        general();
        roma();
        banjou(); // 4.1.54
        kickMaster();
        twirlKick();
    }

    /**
     * 4.1.8 (the owner: "Right click is a chargeable teleport speed kick (dash to an oponent at high speed and side kick with a side
     * twirl."): Kick Master's right click, the Speed Kick charged on the hold, there out of nowhere in a whole side twirl into the kick.
     */
    static void twirlKick() {
        // Its detail: the dash's ticks, the twirl, the charge; every plain speed kick read as before.
        for (int dash = 1; dash <= 4; dash++) {
            check(com.hex.kagune.control.pale.PaleRules.kickTicks(dash) == dash && !com.hex.kagune.control.pale.PaleRules.twirled(dash)
                && com.hex.kagune.control.pale.PaleRules.kickArrive(dash) == com.hex.kagune.control.pale.PaleRules.KICK_BURST + dash
                && com.hex.kagune.control.pale.PaleRules.kickThere(dash) == com.hex.kagune.control.pale.PaleRules.kickArrive(dash),
                "a plain speed kick of " + dash + " ticks: as it was");
            for (float charge : new float[] {0.0F, 0.15F, 0.5F, 1.0F}) {
                int detail = com.hex.kagune.control.pale.PaleRules.twirlDetail(dash, charge);
                check(com.hex.kagune.control.pale.PaleRules.kickTicks(detail) == dash && com.hex.kagune.control.pale.PaleRules.twirled(detail)
                    && Math.abs(com.hex.kagune.control.pale.PaleRules.twirlCharge(detail) - charge) <= 1.0F / 30.0F + 1.0E-6F,
                    "twirl detail " + dash + "/" + charge + ": read back");
                int there = com.hex.kagune.control.pale.PaleRules.kickThere(detail);
                int arrive = com.hex.kagune.control.pale.PaleRules.kickArrive(detail);
                check(there == com.hex.kagune.control.pale.PaleRules.KICK_BURST + dash && arrive == there + com.hex.kagune.control.pale.PaleRules.TWIRL_SPIN,
                    "she is there after the dash, the kick starts after the twirl");
                check(com.hex.kagune.control.pale.PaleRules.kickGone(detail, there - 0.01) && !com.hex.kagune.control.pale.PaleRules.kickGone(detail, there)
                    && com.hex.kagune.control.pale.PaleRules.kickUntouchable(detail, arrive - 0.01)
                    && !com.hex.kagune.control.pale.PaleRules.kickUntouchable(detail, arrive),
                    "gone through the dash, seen through the twirl, nothing hurts her till the kick starts");
                double[] to = {0.5, 0.0, 7.0};
                double[] at = com.hex.kagune.control.pale.PaleRules.kickPlace(to, detail, 1.0, there + 1.0);
                double[] kicked = com.hex.kagune.control.pale.PaleRules.kickPlace(to, detail, 1.0, arrive + 0.5);
                check(at != null && Math.abs(at[0] - 0.5) < 1.0E-9 && Math.abs(at[2] - 7.0) < 1.0E-9 && Math.abs(at[1] - 1.0) < 1.0E-9
                    && kicked != null && Math.abs(kicked[2] - 7.0) < 1.0E-9, "she twirls where she kicks from, up at the kick's height");
                check(com.hex.kagune.control.pale.MoveClashRules.window(com.hex.kagune.control.pale.PaleMove.KICK, detail)
                    == arrive + com.hex.kagune.control.pale.PaleRules.KICK_CONTACT + 1, "a clash waits for its blow");
            }
        }
        // The twirl: a whole turn, fastest as she appears, none left at the chamber.
        double last = Double.MAX_VALUE;
        double lastStep = Double.MAX_VALUE;
        for (double since = 0.0; since <= com.hex.kagune.control.pale.PaleRules.TWIRL_SPIN + 1.0E-9; since += 0.25) {
            double left = com.hex.kagune.control.pale.PaleRules.twirlLeft(since);
            if (last != Double.MAX_VALUE) {
                check(left <= last && last - left <= lastStep + 1.0E-9, "the twirl turns on, slowing: " + since);
                lastStep = last - left;
            }
            last = left;
        }
        check(Math.abs(com.hex.kagune.control.pale.PaleRules.twirlLeft(0.0) - Math.PI * 2.0) < 1.0E-9
            && com.hex.kagune.control.pale.PaleRules.twirlLeft(com.hex.kagune.control.pale.PaleRules.TWIRL_SPIN) == 0.0, "a whole turn, into the chamber");
        // The charge: a floor for a tap, full after its ticks; its reach and its power with it.
        check(com.hex.kagune.control.pale.PaleRules.twirlChargeAt(0.0) == 0.15F
            && com.hex.kagune.control.pale.PaleRules.twirlChargeAt(com.hex.kagune.control.pale.PaleRules.TWIRL_CHARGE_TICKS) == 1.0F
            && com.hex.kagune.control.pale.PaleRules.twirlChargeAt(1000.0) == 1.0F, "a tap kicks at the least charge; held, it fills");
        check(com.hex.kagune.control.pale.PaleRules.twirlRange(0.0F) == com.hex.kagune.control.pale.PaleRules.TWIRL_RANGE_MIN
            && com.hex.kagune.control.pale.PaleRules.twirlRange(1.0F) == com.hex.kagune.control.pale.PaleRules.TWIRL_RANGE_MAX
            && com.hex.kagune.control.pale.PaleRules.twirlRange(1.0F) <= com.hex.kagune.control.pale.PaleRules.KICK_RANGE
            && com.hex.kagune.control.pale.PaleRules.twirlPower(1.0F) == 1.0 && com.hex.kagune.control.pale.PaleRules.twirlPower(0.0F) < 0.5
            && com.hex.kagune.control.pale.PaleRules.twirlPower(0.5F) > com.hex.kagune.control.pale.PaleRules.twirlPower(0.2F),
            "the longer the hold, the further it reaches and the harder it lands (full: the Speed Kick's own)");
        check(com.hex.kagune.control.pale.PaleRules.TWIRL_MAX_HOLD > com.hex.kagune.control.pale.PaleRules.TWIRL_CHARGE_TICKS, "it waits a while full before it goes by itself");
        check(com.hex.kagune.control.pale.PaleMove.values()[com.hex.kagune.control.pale.PaleMove.values().length - 1] == com.hex.kagune.control.pale.PaleMove.TWIRL_CHARGE
            && com.hex.kagune.control.network.C2SPaleAction.TWIRL == 13, "its move and its action appended");
        // Its poses: the coil sunk side-on, out of anything she was doing; the twirl into the chamber, seamless into the kick; nothing
        // through her body.
        int detail = com.hex.kagune.control.pale.PaleRules.twirlDetail(3, 1.0F);
        int there = com.hex.kagune.control.pale.PaleRules.kickThere(detail);
        int arrive = com.hex.kagune.control.pale.PaleRules.kickArrive(detail);
        RigPose atChamber = com.hex.kagune.control.client.animation.PaleMotions.kick(arrive, detail, null);
        RigPose before = com.hex.kagune.control.client.animation.PaleMotions.kick(arrive - 1.0E-4, detail, null);
        check(Math.abs(atChamber.spin - before.spin) < 1.0E-3 && Math.abs(atChamber.rightLegX - before.rightLegX) < 1.0E-3,
            "the twirl ends exactly where the kick starts");
        RigPose appearing = com.hex.kagune.control.client.animation.PaleMotions.kick(there, detail, null);
        check(Math.abs((atChamber.spin - appearing.spin) - Math.PI * 2.0) < 1.0E-3, "she appears a whole turn short of the chamber");
        java.util.List<RigPose> starts = new java.util.ArrayList<>();
        starts.add(null);
        for (com.hex.kagune.control.pale.PaleStrike strike : com.hex.kagune.control.pale.PaleStrike.of(com.hex.kagune.control.pale.PaleStrike.Style.KICKS)) {
            starts.add(com.hex.kagune.control.client.animation.PaleMotions.strike(strike, strike.duration, null));
        }
        double worst = 0.0;
        for (RigPose start : starts) {
            for (double age = 0.0; age <= 40.0; age += 0.25) {
                RigPose coil = com.hex.kagune.control.client.animation.PaleMotions.twirlCharge(age, start);
                coil.settle(1.0F);
                worst = Math.max(worst, com.hex.kagune.control.client.animation.BodyFrame.clipping(coil, 0.25));
            }
        }
        for (double age = there; age <= arrive + com.hex.kagune.control.pale.PaleRules.KICK_TICKS; age += 0.1) {
            RigPose pose = com.hex.kagune.control.client.animation.PaleMotions.kick(age, detail, null);
            pose.settle(1.0F);
            worst = Math.max(worst, com.hex.kagune.control.client.animation.BodyFrame.clipping(pose, 0.25));
        }
        check(worst == 0.0, "the coil and the twirl keep her arms out of her body: " + worst + " px");
        RigPose coiled = com.hex.kagune.control.client.animation.PaleMotions.twirlCharge(10.0, null);
        check(coiled.spin > 0.5F && coiled.leftKnee > 1.0F && coiled.drop > 0.0F, "the coil: side-on, the knees bent, sunk");
        // Where it is wired.
        String moves = read("src/main/java/com/hex/kagune/control/server/PaleMoves.java");
        check(moves.contains("kickPlan(player, PaleRules.twirlRange(charge))") && moves.contains("PaleRules.twirlDetail(plan.dash(), charge)")
            && moves.contains("if (!PaleServer.bare(player) && !KaguneServerController.useEnergy(player, PaleRules.TWIRL_COST, true)) {"),
            "let go: the Speed Kick within the charge's reach, twirling; a human pays no RC");
        String input = read("src/main/java/com/hex/kagune/control/client/PaleInput.java");
        check(input.contains("if (twirlTaken || fights(minecraft.f_91077_)) {") && input.contains("instanceof LivingEntity body && !talks(body)")
            && input.contains("body instanceof com.hex.kagune.control.npc.GhoulNpc"), "the right click is the kick at the air or a body no one talks to");
        // 4.1.49: by Persona.kickMaster (a human's, the General's; and Roma's).
        check(read("src/main/java/com/hex/kagune/control/server/EtoOwl.java").contains("npc.persona().kickMaster() ? PaleRules.twirlDetail(dash, 1.0F) : dash")
            && Persona.GENERAL.kickMaster() && Persona.ROMA.kickMaster() && !Persona.TOUKA.kickMaster(), "the General throws it too (and Roma)");
    }

    /**
     * 4.1.7 (the owner: "He teaches a new combat style mixing kicks and punches (mostly side kicks, spinning kicks etc), he only teaches the
     * player this through quests if the player is either a human or wearing a full mask ... this combat style upon learned fully replaces
     * the default M1 combos ... Make up the quests yourself make sure they're creative and beautful."): the style, its gates, the General's
     * road (three watches of the night shift), who he will and won't teach, and the string itself.
     */
    static void kickMaster() {
        com.hex.kagune.control.FightStyle kicks = com.hex.kagune.control.FightStyle.KICK_MASTER;
        Set<Gate> open = new HashSet<>(java.util.List.of(Gate.STYLE_KICK_MASTER));
        check(com.hex.kagune.control.FightStyle.of(open::contains) == kicks && kicks.ownAttack() && kicks.ownUse()
            && kicks.strikes() == com.hex.kagune.control.pale.PaleStrike.Style.KICKS && kicks.displayName().equals("Kick Master")
            && com.hex.kagune.control.FightStyle.use(open::contains) == kicks,
            "Kick Master learned: M1 is its string; 4.1.8: the right click its own (the twirling speed kick)");
        open.add(Gate.STYLE_SPEEDSTER);
        check(com.hex.kagune.control.FightStyle.of(open::contains) == kicks && com.hex.kagune.control.FightStyle.use(open::contains) == kicks,
            "with Speedster too: Kick Master fully replaces the M1 combos, and its right click the flips");
        check(com.hex.kagune.control.FightStyle.SPEEDSTER.strikes() == com.hex.kagune.control.pale.PaleStrike.Style.BLITZ, "Speedster's string is the blitz");
        for (Gate gate : new Gate[] {Gate.STYLE_KICK_STANCE, Gate.STYLE_KICK_CHAMBER, Gate.STYLE_KICK_MASTER}) {
            check(gate.path() == null && gate.mastery() == 0 && !gate.mutation() && Gate.fromId(gate.id()) == gate && QuestBook.openers(gate).isEmpty(),
                gate + ": no path, no mastery, no quest opens it");
            check(read("src/main/java/com/hex/kagune/control/server/QuestServer.java").contains("gate == Gate.STYLE_KICK_STANCE || gate == Gate.STYLE_KICK_CHAMBER || gate == Gate.STYLE_KICK_MASTER) {\n            return false;"),
                "never had from before");
        }
        check(Gate.values()[Gate.values().length - 1] == Gate.STYLE_KICK_MASTER && Gate.values()[Gate.values().length - 4] == Gate.STYLE_SPEEDSTER,
            "the gates appended after Speedster");
        // His road: three lessons, each a proof, each opening the next.
        java.util.List<Teaching.Lesson> road = Teaching.inOrder(Teaching.GENERAL);
        check(road.size() == 3 && road.get(0).gate() == Gate.STYLE_KICK_STANCE && road.get(1).gate() == Gate.STYLE_KICK_CHAMBER
            && road.get(2).gate() == Gate.STYLE_KICK_MASTER && road.stream().allMatch(l -> l.kind() == Teaching.Kind.TALK),
            "the General's road: the first watch, the middle watch, Kick Master");
        check(Teaching.bit(road.get(0)) == 1L && Teaching.bit(road.get(1)) == 2L && Teaching.bit(road.get(2)) == 4L, "each its own saved bit");
        check(Teaching.before(Gate.STYLE_KICK_STANCE) == null && Teaching.before(Gate.STYLE_KICK_CHAMBER) == Gate.STYLE_KICK_STANCE
            && Teaching.before(Gate.STYLE_KICK_MASTER) == Gate.STYLE_KICK_CHAMBER, "one watch at a time");
        Teaching.Deed[] deeds = {Teaching.Deed.NIGHT_BARE, Teaching.Deed.AIRBORNE, Teaching.Deed.DAWN};
        int[] needs = {8, 3, 3};
        String[] said = {"after dark", "feet off the ground", "at dawn"};
        for (int i = 0; i < 3; i++) {
            Teaching.Proof proof = Teaching.proof(road.get(i));
            check(proof != null && proof.deed() == deeds[i] && proof.need() == needs[i] && proof.ask().length() > 150
                && proof.bar("General Kuroda", 1).contains("1 of " + needs[i]) && proof.bar("General Kuroda", 1).contains(said[i])
                && proof.bar("General Kuroda", needs[i]).endsWith("Go and see him.") && Teaching.fill(proof.progress(), proof, 2).contains("2"),
                road.get(i).name() + ": its proof (" + deeds[i] + ")");
        }
        // Who he teaches: a human (no kagune at all), or one he takes for a human (a ghoul behind a full mask: the caller says what he sees).
        Set<Gate> nothing = new HashSet<>();
        for (KagunePath path : new KagunePath[] {KagunePath.NONE, KagunePath.RINKAKU, KagunePath.UKAKU, KagunePath.BIKAKU, KagunePath.GRAFT}) {
            Teaching.View view = Teaching.view(Teaching.GENERAL, false, path, 0, nothing::contains);
            check(view.ready() && view.next() == road.get(0), path + ": the General starts anyone he takes for a human on the first watch");
            check(Teaching.view(Teaching.GENERAL, true, path, 1000, nothing::contains).refusal() == Teaching.Refusal.GHOUL,
                path + ": a ghoul he can see: never");
        }
        check(Teaching.view(Teaching.GENERAL, false, KagunePath.PALE, 0, nothing::contains).refusal() == Teaching.Refusal.WRONG_PATH,
            "the Owl's body fights its own way");
        check(Teaching.view("ayato", false, KagunePath.NONE, 0, nothing::contains).refusal() == Teaching.Refusal.HUMAN,
            "the ghouls still turn a human away");
        Set<Gate> watches = new HashSet<>(java.util.List.of(Gate.STYLE_KICK_STANCE));
        check(Teaching.view(Teaching.GENERAL, false, KagunePath.NONE, 0, watches::contains).next() == road.get(1), "then the middle watch");
        watches.add(Gate.STYLE_KICK_CHAMBER);
        check(Teaching.view(Teaching.GENERAL, false, KagunePath.NONE, 0, watches::contains).next() == road.get(2), "then Kick Master");
        watches.add(Gate.STYLE_KICK_MASTER);
        check(Teaching.view(Teaching.GENERAL, false, KagunePath.NONE, 0, watches::contains).refusal() == Teaching.Refusal.ALL_TAUGHT, "then nothing more");
        // The dawn watch: first light to full morning, any day.
        long[] dawn = {22300L, 23999L, 0L, 1499L, 24000L + 100L, 5L * 24000L + 23000L};
        long[] notDawn = {1500L, 6000L, 12000L, 18000L, 22299L, 3L * 24000L + 13000L};
        for (long t : dawn) {
            check(Teaching.dawn(t), t + ": dawn");
        }
        for (long t : notDawn) {
            check(!Teaching.dawn(t), t + ": not dawn");
        }
        check(com.hex.kagune.control.npc.EtoRules.night(Teaching.DAWN_FROM) && !com.hex.kagune.control.npc.EtoRules.night(Teaching.DAWN_TO),
            "the dawn watch starts while the night's things are still out, and runs on into the morning");
        // His words.
        for (Teaching.Lesson lesson : road) {
            String words = teaches("GeneralScript", lesson);
            check(words.length() > 200 && !words.contains("(" + lesson.name() + ")"), lesson.name() + ": in his own words");
        }
        check(teaches("GeneralScript", road.get(2)).contains("M1") && teaches("GeneralScript", road.get(2)).contains("nothing in your hand"),
            "and the game says what it does");
        // The string: kicks and punches, the kicks thrown from further out, the tornado kick's jump.
        com.hex.kagune.control.pale.PaleStrike[] string = com.hex.kagune.control.pale.PaleStrike.of(com.hex.kagune.control.pale.PaleStrike.Style.KICKS);
        check(string.length == 8 && string[0] == com.hex.kagune.control.pale.PaleStrike.KICK_JAB && string[7] == com.hex.kagune.control.pale.PaleStrike.TORNADO_KICK
            && string[7].finisher() && java.util.Arrays.stream(string).filter(com.hex.kagune.control.pale.PaleStrike::kick).count() == 5,
            "eight blows, five of them kicks, the tornado kick to finish");
        check(com.hex.kagune.control.pale.PaleStrike.count(Integer.MAX_VALUE, com.hex.kagune.control.pale.PaleStrike.Style.KICKS) == 8
            && com.hex.kagune.control.pale.PaleStrike.next(com.hex.kagune.control.pale.PaleStrike.TORNADO_KICK, Integer.MAX_VALUE,
                com.hex.kagune.control.pale.PaleStrike.Style.KICKS) == com.hex.kagune.control.pale.PaleStrike.KICK_JAB
            && com.hex.kagune.control.pale.PaleStrike.next(com.hex.kagune.control.pale.PaleStrike.CROSS, Integer.MAX_VALUE,
                com.hex.kagune.control.pale.PaleStrike.Style.KICKS) == com.hex.kagune.control.pale.PaleStrike.KICK_JAB,
            "the string all there once learned, round again after the tornado; a blitz strike before it starts it fresh");
        check(com.hex.kagune.control.pale.PaleRules.comboKickStand(0.6) > com.hex.kagune.control.pale.PaleRules.strikeStand(0.6), "a kick is thrown from further out");
        double top = 0.0;
        for (double age = 0.0; age <= string[7].duration; age += 0.25) {
            top = Math.max(top, string[7].lift(age));
        }
        check(top > 8.0 && string[7].lift(string[7].impact) > 8.0 && string[7].lift(0.0) == 0.0 && string[7].lift(string[7].duration) == 0.0,
            "the tornado kick leaves the ground and comes back down (highest " + top + ")");
        // Where it is wired: a human's empty hand, the General's own fight.
        String server = read("src/main/java/com/hex/kagune/control/server/PaleServer.java");
        check(server.contains("return !KaguneProfile.isUnlocked(player) && player.m_21205_().m_41619_() && style(player) == com.hex.kagune.control.FightStyle.KICK_MASTER;")
            && server.contains("if (!eyes && action != C2SPaleAction.STRIKE && action != C2SPaleAction.TWIRL) {"),
            "a human with Kick Master and an empty hand: M1 and (4.1.8) its right click alone");
        String blitz = read("src/main/java/com/hex/kagune/control/server/PaleBlitz.java");
        check(blitz.contains("PaleStrike.next(previous, string == PaleStrike.Style.KICKS ? Integer.MAX_VALUE : mastery, string)")
            && blitz.contains("strike.kick() ? PaleRules.comboKickStand("), "the server throws the style's string, kicks from further out");
        check(read("src/main/java/com/hex/kagune/control/server/EtoOwl.java").contains("PaleStrike.of(npc.persona().kickMaster() ? PaleStrike.Style.KICKS : PaleStrike.Style.BLITZ)"),
            "the General fights with it (4.1.49: and Roma)");
        check(read("src/main/java/com/hex/kagune/control/server/EtoServer.java").contains("boolean seenGhoul = ghoul && !(persona.human() && Sightings.masked(player));"),
            "he teaches as he sees: a full mask hides a ghoul from him");
        java.util.Map<com.hex.kagune.control.AbilityGuide.Key, String> keys = new java.util.EnumMap<>(com.hex.kagune.control.AbilityGuide.Key.class);
        for (com.hex.kagune.control.AbilityGuide.Key key : com.hex.kagune.control.AbilityGuide.Key.values()) {
            keys.put(key, key.name());
        }
        java.util.List<String> listed = guide(keys, new HashSet<>(java.util.List.of(Gate.STYLE_SPEEDSTER)));
        check(listed.contains("Kick Master") && listed.stream().anyMatch(n -> n.startsWith("Kicks and punches (8")) && !listed.contains("Speedster")
            && listed.contains("Twirling Speed Kick (hold, let go)") && !listed.contains("Flips (W A S D)"), "the ability list with Kick Master " + listed);
    }

    /** 4.1.7: what {@code script}'s lesson words say teaching {@code lesson}. */
    static String teaches(String script, Teaching.Lesson lesson) {
        try {
            java.lang.reflect.Field field = Class.forName("com.hex.kagune.control.npc." + script).getDeclaredField("LESSONS");
            field.setAccessible(true);
            Object words = field.get(null);
            java.lang.reflect.Method teach = Class.forName("com.hex.kagune.control.npc.CompanionScript$LessonWords").getDeclaredMethod("teach", Teaching.Lesson.class,
                com.hex.kagune.control.npc.EtoTalk.Context.class);
            teach.setAccessible(true);
            return (String)teach.invoke(words, lesson, null);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
    }

    /**
     * 4.1.5 (the owner: "a new \"general\" there talks to you and can be romanced ... give him some muscles also less than the player ... he
     * is an official character ... Attitude: Tough, softens up when romanced, embarrased easily, proud of the CCG, has to be convinced to
     * love a ghoul, Only interested in woman players"; "use this skin for the general and give him all the formal outfit (but the hat)").
     */
    /**
     * 4.1.49 (the owner: "New Official NPC named \"Roma\", she is the leader of the clowns, can be romanced, has a messed up sense of humor
     * but can be romatic when comes to it. She uses Kick master combat, has a Rinkaku with 4 purple tendrils (can use all moves from
     * Rinakaku, including impale, Debris Hurl, etc. She spawns in this circus tent, is the size of Touka, is female, can be romanced by both
     * Females and Males. (Alex Model, skinny)").
     */
    static void roma() {
        Persona r = Persona.ROMA;
        check(Persona.ALL.indexOf(r) == 6 && r.key().equals("roma") && Persona.of("roma") == r && "Roma".equals(r.name()),
            "Roma, after the others (their places on the talk screen kept; 4.1.54: Banjou after her)");
        check(r.size() == Persona.TOUKA.size() && r.female() && r.slim() && r.curves() < 1.0F && r.legs() < Persona.TOUKA.legs() && r.build() < 1.0F,
            "the size of Touka, a woman, on the slim arms, skinny");
        check(r.romanceable() && r.likes(true) && r.likes(false) && !r.human() && !r.owlKit() && !r.wings() && !r.worms(),
            "dated by women and men alike; a ghoul");
        check(r.kickMaster() && r.tendrils() == 4 && r.kagune() == com.hex.kagune.control.family.KaguneGenes.TAILS,
            "Kick Master, and a Rinkaku of four tendrils (her child takes the tails)");
        int purple = r.kaguneColour();
        int red = purple >> 16 & 255;
        int green = purple >> 8 & 255;
        int blue = purple & 255;
        check(blue > green + 60 && red > green + 30 && blue >= red, "her tendrils purple: " + Integer.toHexString(purple));
        java.util.List<com.hex.kagune.control.npc.Courtship.Chapter> chapters = com.hex.kagune.control.npc.Courtship.of("roma");
        check(chapters.size() == 5 && chapters.get(0).id().equals("roma_show") && chapters.get(chapters.size() - 1).task() == com.hex.kagune.control.npc.Courtship.Task.DAWN,
            "her courtship: five chapters, a show first, the dawn last");
        String scenes = read("src/main/resources/data/hexkagune/talk/roma.txt");
        for (com.hex.kagune.control.npc.Courtship.Chapter chapter : chapters) {
            check(scenes.contains("moment " + chapter.id() + "\n"), chapter.id() + ": its moment in her scenes");
        }
        check(scenes.split("\ndate roma_").length == 4, "three dates of her own");
        // Her script: every voice, mood and page is checked with the others' (talk()); here her fight and kiss lines.
        Script script = r.script();
        check(script.kissed(1, false).length() > 5 && script.maniac(2).length() > 3 && script.won(0).length() > 3 && !script.busy().isEmpty(),
            "her fight and kiss lines");
        // Her four tendrils on the transplant's rig: two a side, the lower pair under the upper, each clear of her at rest, none on another.
        com.hex.kagune.control.graft.GraftRig.Body body = new com.hex.kagune.control.graft.GraftRig.Body(0.0, false, 0.0, true, 0.0, 0.0, 0.0);
        double length = com.hex.kagune.control.graft.GraftStage.TRANSPLANT.length(1000);
        double[][] tips = new double[4][];
        for (int limb = 0; limb < 4; limb++) {
            double[] root = com.hex.kagune.control.graft.GraftRig.root(com.hex.kagune.control.graft.GraftStage.TRANSPLANT, 4, limb, false);
            check(com.hex.kagune.control.graft.GraftRig.side(com.hex.kagune.control.graft.GraftStage.TRANSPLANT, 4, limb) == (limb % 2 == 0 ? 1 : -1)
                && (limb < 2 ? root[1] > 0.9 : root[1] < 0.85), "tendril " + limb + ": two a side, the lower pair under the upper");
            double[][] p = com.hex.kagune.control.graft.GraftRig.pose(new com.hex.kagune.control.graft.GraftRig.Input(com.hex.kagune.control.graft.GraftStage.TRANSPLANT,
                4, limb, length, body, com.hex.kagune.control.graft.GraftMove.NONE, 0.0, null, null, null, 0.3)).points();
            tips[limb] = p[p.length - 1];
            check(Math.hypot(tips[limb][0], tips[limb][2]) > com.hex.kagune.control.graft.GraftRig.BODY_RADIUS, "tendril " + limb + ": clear of her at rest");
        }
        for (int a = 0; a < 4; a++) {
            for (int b = a + 1; b < 4; b++) {
                double gap = Math.sqrt(Math.pow(tips[a][0] - tips[b][0], 2) + Math.pow(tips[a][1] - tips[b][1], 2) + Math.pow(tips[a][2] - tips[b][2], 2));
                check(gap > 0.6, "tendrils " + a + " and " + b + " never on one another at rest: " + gap);
            }
        }
        // Her Rinkaku's moves: the Impale (one tendril, it carried on it), the Rubble Hurl (the lower pair up with it, then whipped).
        check(com.hex.kagune.control.graft.GraftMove.ROMA_IMPALE.beats(4).size() == 1 && com.hex.kagune.control.graft.GraftMove.ROMA_IMPALE.holds()
            && com.hex.kagune.control.graft.GraftMove.ROMA_IMPALE.beats(4).get(0).kind() == com.hex.kagune.control.graft.GraftMove.Kind.SEIZE
            && com.hex.kagune.control.graft.GraftMove.ROMA_IMPALE.beats(4).get(0).reach() >= 10.0, "the Impale: one tendril, as far as ten blocks, carrying");
        check(com.hex.kagune.control.graft.GraftMove.ROMA_HURL.beats(4).stream().allMatch(b -> b.limb() >= 2)
            && com.hex.kagune.control.graft.GraftMove.ROMA_THROW.beats(4).stream().allMatch(b -> b.limb() >= 2 && b.kind() == com.hex.kagune.control.graft.GraftMove.Kind.WHIP),
            "the Rubble Hurl: the lower pair lift it and whip it");
        double[] early = com.hex.kagune.control.graft.GraftRules.heldPoint(com.hex.kagune.control.graft.GraftMove.ROMA_IMPALE, com.hex.kagune.control.graft.GraftMove.Kind.SEIZE, 0.0);
        double[] late = com.hex.kagune.control.graft.GraftRules.heldPoint(com.hex.kagune.control.graft.GraftMove.ROMA_IMPALE, com.hex.kagune.control.graft.GraftMove.Kind.SEIZE, 20.0);
        check(late[1] > early[1] + 2.0 && late[2] > 2.0, "impaled, lifted high on it before her");
        String brain = read("src/main/java/com/hex/kagune/control/server/EtoBrain.java");
        String tendrils = read("src/main/java/com/hex/kagune/control/server/NpcTendrils.java");
        check(brain.indexOf("if (NpcTendrils.fight(npc, mind, target, level, now)) {") < brain.indexOf("EtoOwl.tick(npc, mind, target, level, now, phase);")
            && brain.contains("NpcTendrils.tick(npc, mind, level, now);")
            && tendrils.contains("if (mind.owl.move != PaleMove.NONE || now - st.outAt < NpcRules.DEPLOY_TICKS || now < st.readyAt) {"),
            "her tendrils fight first, never over a kick of hers; her legs (Kick Master) otherwise");
        check(read("src/main/java/com/hex/kagune/control/client/render/GraftRenderer.java").contains("living.persona().tendrils() > 0")
            && read("src/main/java/com/hex/kagune/control/client/render/GraftRenderer.java").contains("tint = npc.persona().kaguneColour();"),
            "every client draws her tendrils, in her purple");
        // In the middle of the circus tent's ring.
        try {
            byte[] tent = new java.util.zip.GZIPInputStream(java.nio.file.Files.newInputStream(java.nio.file.Path.of(
                "src/main/resources/data/hexkagune/structures/circus.nbt"))).readAllBytes();
            String text = new String(tent, java.nio.charset.StandardCharsets.ISO_8859_1);
            check(text.split("hexkagune:roma", -1).length == 2, "Roma in the circus tent, one of her");
        } catch (java.io.IOException failure) {
            check(false, "read the circus tent: " + failure);
        }
    }

    /**
     * 4.1.54 (the owner: "New gang: / Gas Masks - Toxic logo, green name. / Leader - Banjou / Personality - wants to be tough but truly isnt,
     * is aware of how weak he truly is. / Kagune - super small blue Ukaku wing (singular) cannot shoot projectiles, only uses basic m1s with
     * it. / Dialog - Daily quest giver, Chat, Romanceable (Only females). / Lives in the small house in the schematic."; "The gas mask leader
     * puts on the mask at night and is just alex model by the way, not muscled at all"; his home, asked: "The cottage").
     */
    static void banjou() {
        Persona b = Persona.BANJOU;
        check(Persona.ALL.get(Persona.ALL.size() - 1) == b && b.key().equals("banjou") && Persona.of("banjou") == b && "Banjou".equals(b.name())
            && "\u00a7a".equals(b.colour()), "Banjou, after the others (their places on the talk screen kept), his name green");
        check(!b.female() && b.slim() && b.muscle() == 0.0 && b.size() == 1.0F, "a man on the slim arms (\"just alex model\"), no muscle at all");
        check(b.romanceable() && b.likes(true) && !b.likes(false) && !b.human() && !b.owlKit() && !b.worms() && !b.kickMaster() && b.tendrils() == 0,
            "romanced by women only; a ghoul");
        check(b.wings() && b.slapsOnly() && !b.kakuja() && b.wingPower() < 1.0 && b.kagune() == com.hex.kagune.control.family.KaguneGenes.WINGS,
            "his one Ukaku wing, weaker than Touka's, slapping only (his child takes the wings)");
        int blue = b.kaguneColour();
        check((blue & 255) > (blue >> 16 & 255) + 100 && (blue & 255) > (blue >> 8 & 255) + 100, "his wing blue: " + Integer.toHexString(blue));
        check("mask_gas".equals(b.mask()) && b.maskAtNight() && !b.maskInFight() && com.hex.kagune.control.outfit.Outfits.of(b.mask()) != null
            && com.hex.kagune.control.outfit.Outfits.of(b.mask()).mask(), "his gas mask, a real mask, on at night");
        String layer = read("src/main/java/com/hex/kagune/control/client/render/OutfitLayer.java");
        check(layer.contains("if (npc.persona().maskAtNight()) {\n                return com.hex.kagune.control.npc.EtoRules.night(npc.m_9236_().m_46468_()) ? npc.persona().mask() : null;"),
            "every client draws it on him at night and never by day");
        // His courtship: five chapters, only for a woman; his scenes parse, each chapter's moment there, three dates.
        java.util.List<com.hex.kagune.control.npc.Courtship.Chapter> chapters = com.hex.kagune.control.npc.Courtship.of("banjou");
        check(chapters.size() == 5 && chapters.get(chapters.size() - 1).task() == com.hex.kagune.control.npc.Courtship.Task.DEEP,
            "his courtship: five chapters, the mask coming off in the dead of night last");
        String scenes = read("src/main/resources/data/hexkagune/talk/banjou.txt");
        com.hex.kagune.control.npc.scene.SceneBook.Parsed book = com.hex.kagune.control.npc.scene.SceneBook.parse("banjou", scenes);
        check(book.errors().isEmpty() && !FORBIDDEN.matcher(scenes).find(), "his scenes parse: " + book.errors());
        for (com.hex.kagune.control.npc.Courtship.Chapter chapter : chapters) {
            check(scenes.contains("moment " + chapter.id() + "\n"), chapter.id() + ": its moment in his scenes");
        }
        check(scenes.split("\ndate banjou_").length == 4, "three dates of his own");
        // Only a woman: a man asking is told no, kindly; asking a woman is answered.
        EtoTalk.Context man = context(NpcBonds.MOST, Romance.Stage.NONE, null, true, EtoMood.HAPPY, 1, Teaching.View.NONE, false, 6000L);
        EtoTalk.Context woman = context(NpcBonds.MOST, Romance.Stage.NONE, null, true, EtoMood.HAPPY, 1, Teaching.View.NONE, true, 6000L);
        check(!b.script().main(man, "").choices().contains(NpcTalk.Choice.FAVOUR) && b.script().main(woman, "").choices().contains(NpcTalk.Choice.FAVOUR)
            && b.script().answer(man, NpcTalk.Choice.ASK_OUT).effect() == EtoTalk.Effect.NONE, "his courtship offered to a woman, never a man");
        Script script = b.script();
        check(script.kissed(1, false).length() > 5 && script.maniac(2).length() > 3 && script.won(0).length() > 3 && !script.busy().isEmpty(),
            "his fight and kiss lines");
        // His wing: the twin wings' smallest, only his right one, drawn smaller still about its root; a string of the player's one-wing blows.
        com.hex.kagune.control.ukaku.WingRig.Input rest = com.hex.kagune.control.npc.WingRules.banjou(1.0F, 0.0, 1.0F, false, 1.0F, -1, 0.0F);
        check(rest.growRight() == 1.0F && rest.growLeft() == 0.0F && rest.motion() == com.hex.kagune.control.ukaku.WingRig.Motion.NONE
            && rest.aimRight() == 0.0F && rest.mastery() == com.hex.kagune.control.npc.WingRules.SLAP_MASTERY
            && com.hex.kagune.control.npc.WingRules.SLAP_SCALE < 1.0F && com.hex.kagune.control.npc.WingRules.SLAP_SCALE >= 0.5F,
            "one wing (his right), never aiming, small");
        double[] root = {com.hex.kagune.control.ukaku.WingRig.ROOT_X, com.hex.kagune.control.ukaku.WingRig.ROOT_Y, com.hex.kagune.control.ukaku.WingRig.ROOT_Z};
        double[] s = com.hex.kagune.control.npc.WingRules.shrunk(null, 0.5);
        check(Math.abs(s[0] * root[0] + s[9] - root[0]) < 1.0E-9 && Math.abs(s[4] * root[1] + s[10] - root[1]) < 1.0E-9
            && Math.abs(s[8] * root[2] + s[11] - root[2]) < 1.0E-9 && s[0] == 0.5, "shrunk about its root: the shoulder blade stays where it is");
        int n = com.hex.kagune.control.npc.WingRules.SLAPS.length;
        check(n == 3 && com.hex.kagune.control.npc.WingRules.SLAP_TICKS.length == n && com.hex.kagune.control.npc.WingRules.SLAP_IMPACT.length == n
            && com.hex.kagune.control.npc.WingRules.SLAP_DAMAGE.length == n && com.hex.kagune.control.npc.WingRules.SLAP_PATTERNS.length == n,
            "his string: a slap, a wide swing, a rising wing, each with its length, impact, damage and the body's pose");
        for (int i = 0; i < n; i++) {
            int impact = com.hex.kagune.control.npc.WingRules.slapImpact(i);
            double[] hand = com.hex.kagune.control.npc.WingRules.slapTip(i, com.hex.kagune.control.npc.WingRules.SLAP_IMPACT[i]);
            check(impact > 2 && impact < com.hex.kagune.control.npc.WingRules.SLAP_TICKS[i] - 2 && hand[2] > 0.5 && hand[1] > 0.9 && hand[1] < 1.8
                && com.hex.kagune.control.npc.WingRules.SLAP_REACH >= hand[2], "blow " + i + ": lands in front of him at chest height, in his reach ("
                + java.util.Arrays.toString(hand) + ")");
        }
        String wings = read("src/main/java/com/hex/kagune/control/server/NpcWings.java");
        check(wings.indexOf("if (npc.persona().slapsOnly()) {\n            return slaps(npc, mind, st, target, level, now);") < wings.indexOf("case VOLLEY -> {")
            && wings.contains("if (age == WingRules.slapImpact(slap)) {\n                land(npc, st, slap, target, level, gap);")
            && !wings.substring(wings.indexOf("static boolean slaps("), wings.indexOf("private static void land(")).contains("throwSpike"),
            "his wing throws nothing: it slaps, each blow landing on its tick");
        String renderer = read("src/main/java/com/hex/kagune/control/client/render/UkakuWingRenderer.java");
        check(renderer.contains("carry = com.hex.kagune.control.npc.WingRules.shrunk(carry, com.hex.kagune.control.npc.WingRules.SLAP_SCALE);")
            && renderer.contains("emit(consumer, matrix, normals, quads, x, y, z, yaw, scaled, npc.persona().kaguneColour());")
            && read("src/main/java/com/hex/kagune/control/client/render/EtoRenderer.java").contains("return EtoFight.slap(npc, EtoFight.pose("),
            "every client draws it small and blue, slapping, his body turned into each blow");
        // In the cottage of the Gas Masks' hideout, four of them about it.
        try {
            byte[] hideout = new java.util.zip.GZIPInputStream(java.nio.file.Files.newInputStream(java.nio.file.Path.of(
                "src/main/resources/data/hexkagune/structures/gas_hideout.nbt"))).readAllBytes();
            String text = new String(hideout, java.nio.charset.StandardCharsets.ISO_8859_1);
            check(text.split("hexkagune:banjou", -1).length == 2 && text.split("HexGasHideout", -1).length == 5
                && text.contains("hexkagune:gas_hideout_anchor") && text.contains("minecraft:cherry_door") && text.contains("minecraft:pink_glazed_terracotta"),
                "Banjou in the hideout's cottage, one of him; four Gas Masks about it; the owner's build");
        } catch (java.io.IOException failure) {
            check(false, "read the Gas Masks' hideout: " + failure);
        }
    }

    static void general() {
        Persona g = Persona.GENERAL;
        check(Persona.ALL.indexOf(g) == 5 && Persona.ALL.indexOf(Persona.NORO) == 4 && g.key().equals("general") && Persona.of("general") == g,
            "the General, after the others (their places on the talk screen kept; 4.1.49: Roma after him)");
        check(g.human() && !g.female() && g.romanceable() && g.likes(true) && !g.likes(false) && !g.wings() && !g.worms() && !g.owlKit() && !g.kakuja()
            && g.mask() == null && g.kagune() == com.hex.kagune.control.family.KaguneGenes.NONE, "a human man, for women only, with no kagune to pass on");
        for (Persona other : Persona.ALL) {
            check(other == g || !other.human() && other.outfit().isEmpty(), other.key() + ": the others are ghouls in their own clothes");
        }
        check(g.outfit().equals(java.util.List.of("formal_chestplate", "formal_leggings", "formal_boots")), "all the Formal Outfit but its cap");
        for (String id : g.outfit()) {
            com.hex.kagune.control.outfit.Outfits.Piece piece = com.hex.kagune.control.outfit.Outfits.of(id);
            check(piece != null && !piece.mask() && com.hex.kagune.control.outfit.Outfits.FORMAL_SET.contains(piece)
                && piece.slot() != com.hex.kagune.control.outfit.OutfitModels.HELMET, id + ": a piece of the Formal Outfit, never the head's");
        }
        check(g.muscle() > Persona.AYATO.muscle() && g.muscle() < com.hex.kagune.control.npc.ReaperBuild.PLAYER_SHARE,
            "some muscle: more than Ayato's, less than a male player's");
        check(g.slim() && g.blushes(EtoGesture.KISS) && g.blushes(EtoGesture.HUG) && g.blushes(EtoGesture.HAND) && g.kissing() < 0.5 && !g.pecks(),
            "his skin's slim arms; embarrassed easily; never the one to start");
        // His skin is the owner's own, pixel for pixel.
        try {
            java.awt.image.BufferedImage theirs = javax.imageio.ImageIO.read(new java.io.File("art/exploration/General_skin.png"));
            java.awt.image.BufferedImage baked = javax.imageio.ImageIO.read(new java.io.File("src/main/resources/assets/hexkagune/textures/entity/general/general.png"));
            boolean same = theirs.getWidth() == 64 && theirs.getHeight() == 64 && baked.getWidth() == 64 && baked.getHeight() == 64;
            for (int x = 0; same && x < 64; x++) {
                for (int y = 0; y < 64; y++) {
                    int a = theirs.getRGB(x, y);
                    int b = baked.getRGB(x, y);
                    same &= a == b || (a >>> 24) == 0 && (b >>> 24) == 0;
                }
            }
            check(same, "his texture is the owner's skin, every pixel");
            java.awt.image.BufferedImage turned = javax.imageio.ImageIO.read(new java.io.File("src/main/resources/assets/hexkagune/textures/entity/general/general_kakugan.png"));
            boolean none = true;
            for (int x = 0; x < 64; x++) {
                for (int y = 0; y < 64; y++) {
                    none &= (turned.getRGB(x, y) >>> 24) == 0;
                }
            }
            check(none, "no eye of his ever turns");
        } catch (java.io.IOException failure) {
            check(false, "read his skin: " + failure);
        }
        // Convincing him: a human needs only her courtship; a ghoul has to be thought this well of.
        try {
            Class<?> script = Class.forName("com.hex.kagune.control.npc.GeneralScript");
            java.lang.reflect.Field need = script.getDeclaredField("CONVINCED");
            need.setAccessible(true);
            int convinced = need.getInt(null);
            check(convinced > com.hex.kagune.control.npc.NpcBonds.FRIEND && convinced <= com.hex.kagune.control.npc.NpcBonds.MOST,
                "a ghoul must be far past a friend (" + convinced + "), and it can be reached");
            String source = read("src/main/java/com/hex/kagune/control/npc/GeneralScript.java");
            check(source.contains("return !c.ghoul() || c.regard() >= CONVINCED;") && source.contains(": !convinced(c) ? r(one(seed,")
                && source.indexOf("!convinced(c)") < source.indexOf("EtoTalk.Effect.DATE") && source.contains("return c.mixed();"),
                "asked out by a ghoul he isn't sure of: not yet, and no date; a woman only");
        } catch (ReflectiveOperationException failure) {
            check(false, "his script: " + failure);
        }
        check(read("src/main/java/com/hex/kagune/control/server/EtoServer.java").contains("boolean seenGhoul = ghoul && !(persona.human() && Sightings.masked(player));"),
            "behind a full mask a ghoul is a human to him");
        String brain = read("src/main/java/com/hex/kagune/control/server/EtoBrain.java");
        check(brain.contains("dress(npc);") && brain.contains("OutfitServer.dress(npc, pieces);") && brain.contains("armor.m_22100_(OutfitServer.armour(npc, npc.persona().armor()));")
            && brain.contains("if (npc.persona().human() && AgentBrain.ccg(current)) { // 4.1.46: nor one of its ghoul division\n                current = null;"),
            "dressed as he wakes (as hard to hurt as he was made); never against one of his own investigators (4.1.46: nor its ghoul division)");
        java.util.List<com.hex.kagune.control.npc.Courtship.Chapter> chapters = com.hex.kagune.control.npc.Courtship.of("general");
        check(chapters.size() == com.hex.kagune.control.npc.Courtship.CHAPTERS
            && chapters.get(0).task() == com.hex.kagune.control.npc.Courtship.Task.VILLAGE_GHOUL
            && chapters.get(1).task() == com.hex.kagune.control.npc.Courtship.Task.HUMAN_FRIEND
            && chapters.get(2).task() == com.hex.kagune.control.npc.Courtship.Task.GHOUL_PACK
            && chapters.get(4).task() == com.hex.kagune.control.npc.Courtship.Task.DAWN,
            "his courtship: the village defended, a human to vouch, the Pack, somewhere high, the dawn count");
    }

    /**
     * 4.1.3 (the owner: "The default M1 fighting style is only unlocked through a new quest with ayato, until a fighting style is unlocked,
     * the player can only use default punches in kakugan mode, right click is also default, the previous M1 and M2 flips are now all part
     * of a fighting style called Speedster").
     */
    static void speedster() {
        com.hex.kagune.control.FightStyle none = com.hex.kagune.control.FightStyle.NONE;
        com.hex.kagune.control.FightStyle speedster = com.hex.kagune.control.FightStyle.SPEEDSTER;
        Set<Gate> open = new HashSet<>();
        check(com.hex.kagune.control.FightStyle.of(open::contains) == none && !none.ownAttack() && !none.ownUse() && none.gate() == null,
            "no style learned: the game's own punch and right click");
        for (Gate gate : Gate.values()) {
            if (gate != Gate.STYLE_SPEEDSTER && gate != Gate.STYLE_KICK_MASTER) {
                open.add(gate);
            }
        }
        check(com.hex.kagune.control.FightStyle.of(open::contains) == none, "no other gate gives a fighting style (mastery and every other lesson: still bare hands)");
        open.add(Gate.STYLE_SPEEDSTER);
        check(com.hex.kagune.control.FightStyle.of(open::contains) == speedster && speedster.ownAttack() && speedster.ownUse()
            && speedster.gate() == Gate.STYLE_SPEEDSTER && speedster.displayName().equals("Speedster"), "Speedster learned: M1 and right click are its own");
        Gate gate = Gate.STYLE_SPEEDSTER;
        check(gate.path() == null && gate.mastery() == 0 && !gate.mutation() && Gate.fromId("style_speedster") == gate
            && gate.on(KagunePath.RINKAKU) && gate.on(KagunePath.UKAKU) && gate.on(KagunePath.BIKAKU) && gate.on(KagunePath.GRAFT),
            "the Speedster gate: no path of its own, no mastery");
        Teaching.Lesson lesson = Teaching.teaching(gate);
        check(lesson != null && lesson.teacher().equals("ayato") && lesson.kind() == Teaching.Kind.TALK && lesson.name().equals("Speedster")
            && Teaching.before(gate) == null && QuestBook.openers(gate).isEmpty(), "Ayato teaches it, told; nothing before it, no quest opens it");
        check(Teaching.bit(Teaching.byId("ayato_doves")) == 1L && Teaching.bit(Teaching.byId("ayato_hunt")) == 2L
            && Teaching.bit(Teaching.byId("ayato_kakuja")) == 4L && Teaching.bit(lesson) == 8L, "Ayato's old lessons keep their saved bits; Speedster takes a new one");
        check(Teaching.inOrder("ayato").get(0) == lesson, "he teaches it first (it needs no mastery)");
        Teaching.Proof proof = Teaching.proof(lesson);
        check(proof != null && proof.deed() == Teaching.Deed.BARE_HANDS && proof.need() == 6 && proof.bar("Ayato", 2).contains("2 of 6")
            && proof.bar("Ayato", 2).contains("bare hands") && proof.ask().contains("fists") && Teaching.fill(proof.progress(), proof, 3).contains("3"),
            "its proof: six killed with the bare hands, the eyes open");
        String text = Teaching.gatedText(gate, "Ayato");
        check(text.contains("Ayato teaches it") && !text.contains("mastery"), "the game says who teaches it, and names no mastery (" + text + ")");
        check(Teaching.gatedText(Gate.UKAKU_GATLING, "Touka").contains("at mastery 400"), "the others still name theirs");
        // Any kagune but the Owl's learns it; a human never.
        Set<Gate> none0 = new HashSet<>();
        for (KagunePath path : new KagunePath[] {KagunePath.RINKAKU, KagunePath.UKAKU, KagunePath.BIKAKU, KagunePath.GRAFT}) {
            Teaching.View view = Teaching.view("ayato", true, path, 0, none0::contains);
            check(view.ready() && view.next() == lesson && Teaching.fits(lesson, path), path + ": Ayato teaches Speedster at once, at mastery 0");
        }
        check(Teaching.view("ayato", true, KagunePath.PALE, 1000, none0::contains).refusal() == Teaching.Refusal.WRONG_PATH && !Teaching.fits(lesson, KagunePath.PALE)
            && !Teaching.fits(lesson, KagunePath.NONE), "not the Owl (her body fights so already), nor a ghoul with no kagune chosen");
        check(Teaching.view("ayato", false, KagunePath.RINKAKU, 1000, none0::contains).refusal() == Teaching.Refusal.HUMAN, "a human: nothing");
        for (String other : new String[] {"touka", "ken", "noro"}) {
            check(Teaching.of(other).stream().noneMatch(l -> l.path() == null), other + " teaches no fighting style");
        }
        Set<Gate> learned = new HashSet<>(java.util.List.of(gate));
        check(Teaching.view("ayato", true, KagunePath.RINKAKU, 500, learned::contains).refusal() == Teaching.Refusal.ALL_TAUGHT,
            "taught, he has nothing more for tails");
        Teaching.View wings = Teaching.view("ayato", true, KagunePath.UKAKU, 1000, learned::contains);
        check(!wings.ready() && wings.next().gate() == Gate.UKAKU_AYATO_DOVES && wings.refusal() == Teaching.Refusal.BEFORE, "for wings, his road goes on after it");
        check(Teaching.knownElsewhere("ayato", KagunePath.BIKAKU, learned::contains, 0L).contains(lesson)
            && Teaching.knownElsewhere("ayato", KagunePath.BIKAKU, learned::contains, Teaching.bit(lesson)).isEmpty()
            && Teaching.knownElsewhere("ayato", KagunePath.PALE, learned::contains, 0L).isEmpty(), "known some other way (a command): he says so, once");
        // A proof under way (a save from before 4.1.3, half-way through his Doves) stays the next lesson: its count is not thrown away.
        Set<Gate> road = new HashSet<>(java.util.List.of(Gate.UKAKU_CRYSTAL, Gate.UKAKU_GATLING));
        long doves = Teaching.bit(Teaching.byId("ayato_doves"));
        check(Teaching.view("ayato", true, KagunePath.UKAKU, 700, road::contains).next() == lesson, "with no proof set: Speedster first");
        Teaching.View kept = Teaching.view("ayato", true, KagunePath.UKAKU, 700, road::contains, doves);
        check(kept.ready() && kept.next().gate() == Gate.UKAKU_AYATO_DOVES, "his Doves' proof under way: it stays the next");
        road.add(Gate.UKAKU_AYATO_DOVES);
        check(Teaching.view("ayato", true, KagunePath.UKAKU, 700, road::contains, doves).next() == lesson, "that one taught: Speedster next");
        check(Teaching.view("ayato", true, KagunePath.UKAKU, 100, new HashSet<Gate>()::contains, doves).next() == lesson
            && Teaching.view("ayato", true, KagunePath.RINKAKU, 700, new HashSet<Gate>()::contains, doves).next() == lesson,
            "a proof that can't be taught now (too weak, the step before missing, another kagune) holds nothing up");
        // The words, and where the style is checked.
        String words = ayatoTeaches(lesson);
        check(words.length() > 120 && words.contains("Speedster") && words.contains("M1") && words.contains("right click"), "Ayato teaches it in his own words, and the game says what it does");
        String server = read("src/main/java/com/hex/kagune/control/server/PaleServer.java");
        check(server.contains("if (style.ownAttack()) {\n                    PaleBlitz.click(")
            && server.contains("if (com.hex.kagune.control.FightStyle.use(QuestServer.log(player)::open) == com.hex.kagune.control.FightStyle.SPEEDSTER) {\n                    PaleMoves.flip(")
            && server.contains("(eyes(player) || bare(player)) && style(player).ownAttack()) {\n            event.setCanceled(true);"),
            "the server: the eyes' blitz and flips only with the style; without it a punch stays the game's own");
        check(server.contains("case C2SPaleAction.PUNCH -> PaleMoves.punchKey(player, state, arg != 0, targetId, now);\n            case C2SPaleAction.KICK -> PaleMoves.kick(player, state, now);\n            default -> {\n            }\n        }\n    }\n\n    /**\n     * 7.0.1: with only the eyes open a vanilla punch"),
            "the keys' moves (the Big Punch, the Speed Kick) are not a style's");
        String input = read("src/main/java/com/hex/kagune/control/client/PaleInput.java");
        check(input.contains("if (PaleClient.eyes(player) && !PaleClient.useStyle().ownUse()) {\n            return;\n        }\n        event.setCanceled(true);")
            && input.contains("if (!PaleClient.style().ownAttack()) {\n            return;\n        }\n        HitResult hit"),
            "the client: with no style the clicks are left to the game (never cancelled)");
        check(read("src/main/java/com/hex/kagune/control/server/QuestServer.java").contains("if (gate == Gate.STYLE_SPEEDSTER) {\n            return false;"),
            "never had from before: a save starts with its bare hands");
        check(read("src/main/java/com/hex/kagune/control/server/EtoServer.java").contains("boolean hostile = QuestServer.victim(dead).contains(com.hex.kagune.control.quest.Tags.HOSTILE);\n        if (PaleServer.eyes(killer) && killer.m_21205_().m_41619_() && hostile) {"),
            "the proof counts something hostile killed with the eyes open and nothing in the hand");
        // The ability list.
        java.util.Map<com.hex.kagune.control.AbilityGuide.Key, String> keys = new java.util.EnumMap<>(com.hex.kagune.control.AbilityGuide.Key.class);
        for (com.hex.kagune.control.AbilityGuide.Key key : com.hex.kagune.control.AbilityGuide.Key.values()) {
            keys.put(key, key.name());
        }
        Set<Gate> shut = new HashSet<>(java.util.List.of(gate, Gate.STYLE_KICK_MASTER));
        java.util.List<String> bare = guide(keys, shut);
        java.util.List<String> fast = guide(keys, new HashSet<>(java.util.List.of(Gate.STYLE_KICK_MASTER)));
        check(bare.contains("Punch") && bare.stream().noneMatch(n -> n.startsWith("Speed Blitz") || n.startsWith("Flips")) && bare.contains("None: ask Ayato")
            && bare.contains("Big Punch!"), "the list with no style: Punch, who to ask, the keys' moves still there " + bare);
        check(fast.stream().anyMatch(n -> n.startsWith("Speed Blitz")) && fast.stream().anyMatch(n -> n.startsWith("Flips")) && fast.contains("Speedster")
            && !fast.contains("Punch"), "the list with Speedster: the blitz and the flips " + fast);
    }

    private static java.util.List<String> guide(java.util.Map<com.hex.kagune.control.AbilityGuide.Key, String> keys, Set<Gate> shut) {
        com.hex.kagune.control.AbilityGuide.Panel panel = com.hex.kagune.control.AbilityGuide.panel(new com.hex.kagune.control.AbilityGuide.Input(
            KagunePath.RINKAKU, com.hex.kagune.control.AbilityGuide.State.EYES, 300, com.hex.kagune.gameplay.KaguneForm.values()[0],
            com.hex.kagune.control.ukaku.WingForm.TWIN, false, keys, false, com.hex.kagune.control.graft.GraftStage.values()[0], false, shut, false));
        return panel.rows().stream().map(com.hex.kagune.control.AbilityGuide.Row::name).toList();
    }

    private static String read(String path) {
        try {
            return java.nio.file.Files.readString(java.nio.file.Path.of(path));
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    static String ayatoTeaches(Teaching.Lesson lesson) {
        try {
            java.lang.reflect.Field field = Class.forName("com.hex.kagune.control.npc.AyatoScript").getDeclaredField("LESSONS");
            field.setAccessible(true);
            Object words = field.get(null);
            java.lang.reflect.Method teach = Class.forName("com.hex.kagune.control.npc.CompanionScript$LessonWords").getDeclaredMethod("teach", Teaching.Lesson.class,
                com.hex.kagune.control.npc.EtoTalk.Context.class);
            teach.setAccessible(true);
            return (String)teach.invoke(words, lesson, null);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
    }

    /** 2.2.3: a partner's mood shapes what they do: kisses asked for, kisses and hugs taken. */
    static void moods() {
        for (EtoMood mood : EtoMood.values()) {
            double k = com.hex.kagune.control.npc.MoodActs.kissing(mood);
            check(k >= 0.0 && k <= 3.0, mood + ": a sane share");
        }
        check(com.hex.kagune.control.npc.MoodActs.kissing(EtoMood.SULKY) == 0.0 && com.hex.kagune.control.npc.MoodActs.kissing(EtoMood.JEALOUS) == 0.0
            && com.hex.kagune.control.npc.MoodActs.kissing(EtoMood.LONELY) > 1.5 && com.hex.kagune.control.npc.MoodActs.kissing(EtoMood.SLEEPY) < 1.0,
            "mad or jealous: no kisses; lonely: far more; sleepy: fewer");
        for (Persona p : Persona.ALL) {
            for (int seed = 0; seed < 4; seed++) {
                check(com.hex.kagune.control.npc.MoodActs.refuse(p.key(), EtoMood.SULKY, true, seed) == null
                    && com.hex.kagune.control.npc.MoodActs.refuse(p.key(), EtoMood.SULKY, false, seed) != null
                    && com.hex.kagune.control.npc.MoodActs.refuse(p.key(), EtoMood.JEALOUS, false, seed) != null
                    && com.hex.kagune.control.npc.MoodActs.refuse(p.key(), EtoMood.JEALOUS, true, seed) == null
                    && com.hex.kagune.control.npc.MoodActs.refuse(p.key(), EtoMood.BLOODTHIRSTY, true, seed) == null
                    && com.hex.kagune.control.npc.MoodActs.refuse(p.key(), EtoMood.BLOODTHIRSTY, false, seed) == null
                    && com.hex.kagune.control.npc.MoodActs.refuse(p.key(), EtoMood.HAPPY, true, seed) == null
                    && com.hex.kagune.control.npc.MoodActs.refuse(p.key(), EtoMood.AFFECTIONATE, true, seed) == null
                    && com.hex.kagune.control.npc.MoodActs.reassured(p.key(), seed).length() > 5, p.key() + ": takes what the mood allows");
            }
        }
    }

    /** 2.3.5: two poses that differ somewhere (arms, head, lean). */
    static boolean differ(RigPose a, RigPose b) {
        float d = Math.abs(a.rightArmX - b.rightArmX) + Math.abs(a.leftArmX - b.leftArmX) + Math.abs(a.rightArmZ - b.rightArmZ)
            + Math.abs(a.leftArmZ - b.leftArmZ) + Math.abs(a.rightArmY - b.rightArmY) + Math.abs(a.headY - b.headY) + Math.abs(a.headZ - b.headZ)
            + Math.abs(a.lean - b.lean) + Math.abs(a.drop - b.drop);
        return d > 0.15F;
    }

    /** 2.2.7: married by command: everything about it full (Courtship.wed). */
    static void wed() {
        com.hex.kagune.control.quest.QuestLog.Bond[] starts = {new com.hex.kagune.control.quest.QuestLog.Bond(0, -1L, 0, 0),
            new com.hex.kagune.control.quest.QuestLog.Bond(-80, 3L, EtoRules.BOOKS_DONE | EtoRules.TAUGHT_SOAR, EtoRules.gifted(
                new com.hex.kagune.control.quest.QuestLog.Bond(0, 0L, 0, 0), 9L).favours() | 2),
            EtoRules.begin(new com.hex.kagune.control.quest.QuestLog.Bond(70, 5L, com.hex.kagune.control.npc.NpcBonds.MET, 0))};
        for (com.hex.kagune.control.quest.QuestLog.Bond start : starts) {
            com.hex.kagune.control.quest.QuestLog.Bond wed = com.hex.kagune.control.npc.Courtship.wed(start);
            check(wed.regard() == com.hex.kagune.control.npc.NpcBonds.MOST, "wed: regard at the most");
            check(wed.has(EtoRules.DATING) && EtoRules.step(wed) == EtoRules.Step.DATING, "wed: past dating's step (the world keeps them married)");
            check(com.hex.kagune.control.npc.Courtship.closed(wed, com.hex.kagune.control.npc.Courtship.CHAPTERS) == com.hex.kagune.control.npc.Courtship.CHAPTERS
                && !com.hex.kagune.control.npc.Courtship.on(wed, com.hex.kagune.control.npc.Courtship.CHAPTERS) && EtoRules.count(wed) == 0,
                "wed: every chapter closed, nothing owing");
            check(wed.has(com.hex.kagune.control.npc.NpcBonds.MET) && wed.has(com.hex.kagune.control.npc.NpcBonds.BEFRIENDED)
                && !wed.has(EtoRules.BOOKS_DONE), "wed: met, befriended, the old errands gone");
            check(wed.has(EtoRules.TAUGHT_SOAR) == start.has(EtoRules.TAUGHT_SOAR) && EtoRules.giftDay(wed) == EtoRules.giftDay(start)
                && wed.chatted() == start.chatted(), "wed: what was taught kept, and the last gift's day");
            check(com.hex.kagune.control.npc.Courtship.wed(wed).equals(wed), "wed twice: the same");
        }
    }

    /** 2.2.4: a kiss for every mood: the same lean-in and hold, each mood its own words, wait, body and after. */
    static void moodKisses() {
        com.hex.kagune.control.npc.MoodKiss.Style[] styles = com.hex.kagune.control.npc.MoodKiss.Style.values();
        check(styles[0] == com.hex.kagune.control.npc.MoodKiss.Style.SWEET && styles.length == 15
            && styles[9] == com.hex.kagune.control.npc.MoodKiss.Style.SHY, "the kinds of kiss (sent by ordinal: sweet first; 4.1.15's appended)");
        check(com.hex.kagune.control.npc.MoodKiss.of(EtoMood.SULKY) == com.hex.kagune.control.npc.MoodKiss.Style.ANGRY
            && com.hex.kagune.control.npc.MoodKiss.of(EtoMood.JEALOUS) == com.hex.kagune.control.npc.MoodKiss.Style.POSSESSIVE
            && com.hex.kagune.control.npc.MoodKiss.of(EtoMood.HAPPY) == com.hex.kagune.control.npc.MoodKiss.Style.SWEET
            && com.hex.kagune.control.npc.MoodKiss.of(EtoMood.BORED) == com.hex.kagune.control.npc.MoodKiss.Style.TEASING,
            "angry when sulking, possessive when jealous, sweet when happy, teasing when bored");
        Set<com.hex.kagune.control.npc.MoodKiss.Style> used = new HashSet<>();
        for (EtoMood mood : EtoMood.values()) {
            used.add(com.hex.kagune.control.npc.MoodKiss.of(mood));
        }
        check(used.size() == 9, "every mood's kind of kiss comes from some mood");
        // 4.1.15 (the owner: "add more kiss types and better the animation"): the moment's kisses, under a mood's own.
        com.hex.kagune.control.npc.MoodKiss.Moment plain = new com.hex.kagune.control.npc.MoodKiss.Moment(false, false, 20L, true, 99);
        for (EtoMood mood : EtoMood.values()) {
            for (int roll = 0; roll < 100; roll++) {
                for (int bits = 0; bits < 16; bits++) {
                    com.hex.kagune.control.npc.MoodKiss.Moment m = new com.hex.kagune.control.npc.MoodKiss.Moment((bits & 1) != 0, (bits & 2) != 0,
                        (bits & 4) != 0 ? 0L : 20L, (bits & 8) != 0, roll);
                    com.hex.kagune.control.npc.MoodKiss.Style style = com.hex.kagune.control.npc.MoodKiss.of(mood, m);
                    used.add(style);
                    if (com.hex.kagune.control.npc.MoodKiss.of(mood) != com.hex.kagune.control.npc.MoodKiss.Style.SWEET) {
                        check(style == com.hex.kagune.control.npc.MoodKiss.of(mood), mood + ": the mood's own kiss comes first, whatever the moment");
                    } else if (m.morningAfter()) {
                        check(style == com.hex.kagune.control.npc.MoodKiss.Style.MORNING, mood + ": the morning after");
                    } else if (m.raining()) {
                        check(style == com.hex.kagune.control.npc.MoodKiss.Style.RAIN, mood + ": in the rain");
                    } else if (m.kisses() < com.hex.kagune.control.npc.MoodKiss.SHY_UNTIL && !m.married()) {
                        check(style == com.hex.kagune.control.npc.MoodKiss.Style.SHY, mood + ": the first few are shy");
                    }
                }
            }
        }
        check(used.size() == styles.length, "every kind of kiss comes from some mood or moment");
        check(com.hex.kagune.control.npc.MoodKiss.of(EtoMood.HAPPY, plain) == com.hex.kagune.control.npc.MoodKiss.Style.SWEET
            && com.hex.kagune.control.npc.MoodKiss.of(EtoMood.HAPPY, null) == com.hex.kagune.control.npc.MoodKiss.Style.SWEET, "most happy kisses are still the sweet one");
        int sweet = 0;
        for (int roll = 0; roll < 100; roll++) {
            sweet += com.hex.kagune.control.npc.MoodKiss.of(EtoMood.HAPPY, new com.hex.kagune.control.npc.MoodKiss.Moment(false, false, 20L, true, roll))
                == com.hex.kagune.control.npc.MoodKiss.Style.SWEET ? 1 : 0;
        }
        check(sweet >= 50 && sweet <= 70, "a happy kiss is the sweet one more often than not, the others now and then (" + sweet + "%)");
        check(com.hex.kagune.control.npc.MoodKiss.of(EtoMood.HAPPY, new com.hex.kagune.control.npc.MoodKiss.Moment(false, false, 0L, true, 99))
            != com.hex.kagune.control.npc.MoodKiss.Style.SHY, "married: never shy again");
        for (com.hex.kagune.control.npc.MoodKiss.Style style : styles) {
            int waits = com.hex.kagune.control.npc.MoodKiss.waits(style);
            check(waits >= 40 && waits <= 400, style + ": a sane wait");
            check(com.hex.kagune.control.npc.MoodKiss.dodges(style) == (style == com.hex.kagune.control.npc.MoodKiss.Style.TEASING), style + ": only teasing dodges");
            double hearts = com.hex.kagune.control.npc.MoodKiss.hearts(style);
            check(hearts > 0.0 && hearts <= 2.0, style + ": hearts");
            String waiting = com.hex.kagune.control.npc.MoodKiss.waiting(style, "Touka", "her");
            check(waiting.startsWith("Touka") && waiting.length() > 15, style + ": what the screen says while they wait");
            for (Persona p : Persona.ALL) {
                for (int seed = 0; seed < 4; seed++) {
                    for (int h = 0; h < 3; h++) {
                        String line = com.hex.kagune.control.npc.MoodKiss.kissed(p.key(), style, h == 1, h == 2, seed);
                        check(style == com.hex.kagune.control.npc.MoodKiss.Style.SWEET ? line == null : line != null && line.length() > 10,
                            p.key() + " " + style + ": their words after (sweet keeps the script's)");
                    }
                    check(com.hex.kagune.control.npc.MoodKiss.dodged(p.key(), seed).length() > 10, p.key() + ": a dodge");
                }
            }
            for (EtoGesture gesture : new EtoGesture[] {EtoGesture.KISS, EtoGesture.BLUSH, EtoGesture.PULL}) {
                for (int persona = 0; persona < 3; persona++) {
                    for (double age : new double[] {0.0, 7.0, 20.0, 45.0, 90.0, 200.0}) {
                        RigPose usual = EtoPose.gesture(gesture, age, persona, 0);
                        RigPose moody = com.hex.kagune.control.client.animation.MoodKissPose.apply(usual, style, gesture, age, persona);
                        if (style == com.hex.kagune.control.npc.MoodKiss.Style.SWEET) {
                            check(moody == usual, gesture + ": sweet is the usual kiss");
                            continue;
                        }
                        check(moody != null && moody != usual && Float.isFinite(moody.headX) && Math.abs(moody.headX) < 1.1F,
                            style + " " + gesture + " " + persona + ": a pose");
                        // 2.3.5: never an arm raised to face height turned in across the middle (the partner's face, their camera).
                        check(com.hex.kagune.control.client.animation.MoodKissPose.clear(moody), style + " " + gesture + " persona " + persona
                            + " at " + age + ": the partner's face kept clear (arms " + moody.rightArmX + "/" + moody.rightArmY + ", " + moody.leftArmX + "/"
                            + moody.leftArmY + ")");
                    }
                    RigPose held = com.hex.kagune.control.client.animation.MoodKissPose.apply(null, style, gesture, 40.0, persona);
                    if (style != com.hex.kagune.control.npc.MoodKiss.Style.SWEET && gesture == EtoGesture.KISS) {
                        check(held.lean > 0.3F && held.lean < 0.75F, style + " " + persona + ": leaning in");
                    }
                }
                if (style != com.hex.kagune.control.npc.MoodKiss.Style.SWEET) {
                    // 2.3.5: each companion their own way, not one copied to all three.
                    RigPose eto = com.hex.kagune.control.client.animation.MoodKissPose.apply(null, style, gesture, 40.0, 0);
                    RigPose ken = com.hex.kagune.control.client.animation.MoodKissPose.apply(null, style, gesture, 40.0, 1);
                    RigPose touka = com.hex.kagune.control.client.animation.MoodKissPose.apply(null, style, gesture, 40.0, 2);
                    if (gesture == EtoGesture.KISS) {
                        check(differ(eto, ken) && differ(eto, touka) && differ(ken, touka), style + ": Eto's, Ken's and Touka's each their own");
                    }
                }
            }
            if (style != com.hex.kagune.control.npc.MoodKiss.Style.SWEET) {
                for (int persona = 0; persona < 3; persona++) {
                    // 4.1.15 (the owner: "better the animation"): held, or pulled in, it goes on from the pose it was in: never back to
                    // standing straight first (each phase's clock starts again at 0), and it never jumps more than a little in a tick.
                    for (EtoGesture[] step : new EtoGesture[][] {{EtoGesture.KISS, EtoGesture.BLUSH}, {EtoGesture.BLUSH, EtoGesture.PULL}}) {
                        RigPose was = com.hex.kagune.control.client.animation.MoodKissPose.apply(null, style, step[0], 30.0, persona);
                        RigPose then = com.hex.kagune.control.client.animation.MoodKissPose.apply(null, style, step[1], 0.0, persona);
                        check(then.lean > 0.25F && Math.abs(then.lean - was.lean) < 0.12F, style + " " + step[0] + " to " + step[1] + " (" + persona
                            + "): from where it was (" + was.lean + " then " + then.lean + ")");
                        RigPose last = then;
                        for (double age = 0.5; age <= 16.0; age += 0.5) {
                            RigPose next = com.hex.kagune.control.client.animation.MoodKissPose.apply(null, style, step[1], age, persona);
                            check(Math.abs(next.lean - last.lean) < 0.1F && Math.abs(next.rightArmX - last.rightArmX) < 0.45F
                                && Math.abs(next.leftArmX - last.leftArmX) < 0.45F, style + " " + step[1] + " (" + persona + ") at " + age + ": smooth");
                            last = next;
                        }
                    }
                }
            }
            RigPose other = EtoPose.gesture(EtoGesture.WAVE, 20.0, 0, 0);
            check(com.hex.kagune.control.client.animation.MoodKissPose.apply(other, style, EtoGesture.WAVE, 20.0, 0) == other, style + ": only the kiss is changed");
        }
        // 2.3.5: angry looks angry: the face turned away while waiting, on every one of them; Eto stamps, Touka's fists are clenched.
        for (int persona = 0; persona < 3; persona++) {
            RigPose angry = com.hex.kagune.control.client.animation.MoodKissPose.apply(null, com.hex.kagune.control.npc.MoodKiss.Style.ANGRY,
                EtoGesture.KISS, 30.0, persona);
            check(Math.abs(angry.headY) > 0.3F && angry.lean < 0.45F, persona + ": angry, the face turned away, leaning in grudgingly");
        }
        check(com.hex.kagune.control.client.animation.MoodKissPose.apply(null, com.hex.kagune.control.npc.MoodKiss.Style.ANGRY, EtoGesture.KISS, 2.0, 0)
            .rightLegX < 0.0F, "Eto stamps her foot");
        check(com.hex.kagune.control.npc.MoodKiss.waits(com.hex.kagune.control.npc.MoodKiss.Style.ANGRY) < EtoRules.KISS_WAIT
            && com.hex.kagune.control.npc.MoodKiss.waits(com.hex.kagune.control.npc.MoodKiss.Style.SLEEPY) > EtoRules.KISS_WAIT,
            "the angry won't wait long; the sleepy forget to stop");
        check(com.hex.kagune.control.npc.MoodKiss.after(com.hex.kagune.control.npc.MoodKiss.Style.ANGRY) == EtoGesture.ANGRY
            && com.hex.kagune.control.npc.MoodKiss.after(com.hex.kagune.control.npc.MoodKiss.Style.SWEET) == null, "after: arms crossed when angry; the usual when sweet");
    }

    /**
     * 4.1.15 (the owner: "better the animation"): a companion goes from one gesture's pose into the next one's (the kiss into the blush
     * after it; a gesture into none) without dropping back upright in between; the same gesture is drawn as it is.
     */
    static void poseFade() {
        com.hex.kagune.control.client.animation.PoseFade fade = new com.hex.kagune.control.client.animation.PoseFade();
        RigPose kiss = com.hex.kagune.control.client.animation.MoodKissPose.apply(null, com.hex.kagune.control.npc.MoodKiss.Style.MOVIE, EtoGesture.KISS, 40.0, 0);
        check(fade.through(kiss, 100L, 140.0) == kiss && fade.through(kiss, 100L, 141.0) == kiss, "the same gesture: drawn as it is");
        RigPose after = EtoPose.gesture(EtoGesture.KISSED, 0.0, 0, 0);
        RigPose first = fade.through(after, 141L, 141.0);
        check(first != null && Math.abs(first.lean - kiss.lean) < 0.01F, "a new gesture begins from the pose before it (" + first.lean + ", was " + kiss.lean + ")");
        float lastLean = first.lean;
        for (double clock = 141.25; clock <= 141.0 + com.hex.kagune.control.client.animation.PoseFade.TICKS; clock += 0.25) {
            RigPose now = fade.through(EtoPose.gesture(EtoGesture.KISSED, clock - 141.0, 0, 0), 141L, clock);
            check(Math.abs(now.lean - lastLean) < 0.08F, "blended smoothly at " + clock);
            lastLean = now.lean;
        }
        RigPose settled = EtoPose.gesture(EtoGesture.KISSED, 10.0, 0, 0);
        check(fade.through(settled, 141L, 151.0) == settled, "once blended: the new gesture's own pose");
        // Into no gesture: eased down to standing, then none at all.
        RigPose down = fade.through(null, 200L, 200.0);
        check(down != null && Math.abs(down.lean - settled.lean) < 0.01F, "into none: from where it was");
        check(fade.through(null, 200L, 206.0) == null, "then none");
        com.hex.kagune.control.client.animation.PoseFade fresh = new com.hex.kagune.control.client.animation.PoseFade();
        check(fresh.through(null, 5L, 1.0) == null && fresh.through(kiss, 6L, 2.0) != null, "nothing before: nothing to blend from");
    }

    /**
     * 4.1.16 (the owner: "Confirm that all females when kissing have a blush, sort of ... So every time [they] blush, it depends on their
     * mood"): a woman blushes through the whole of a kiss, as deep as its mood, never flickering off between its phases, a while after,
     * then it fades; a man only where it is his way to (Persona.blushes).
     */
    static void kissBlush() {
        com.hex.kagune.control.npc.MoodKiss.Style[] styles = com.hex.kagune.control.npc.MoodKiss.Style.values();
        for (com.hex.kagune.control.npc.MoodKiss.Style style : styles) {
            float deep = com.hex.kagune.control.npc.MoodKiss.blush(style);
            check(deep >= 0.25F && deep <= 1.0F, style + ": a blush, some");
        }
        check(com.hex.kagune.control.npc.MoodKiss.blush(com.hex.kagune.control.npc.MoodKiss.Style.SHY) > com.hex.kagune.control.npc.MoodKiss.blush(
            com.hex.kagune.control.npc.MoodKiss.Style.SWEET) && com.hex.kagune.control.npc.MoodKiss.blush(com.hex.kagune.control.npc.MoodKiss.Style.SWEET)
            > com.hex.kagune.control.npc.MoodKiss.blush(com.hex.kagune.control.npc.MoodKiss.Style.ANGRY), "shy deepest, sweet warm, angry only a flush");
        for (Persona p : Persona.ALL) {
            for (com.hex.kagune.control.npc.MoodKiss.Style style : styles) {
                com.hex.kagune.control.client.animation.KissBlush blush = new com.hex.kagune.control.client.animation.KissBlush();
                double clock = 1000.0;
                float last = 0.0F;
                // Leaning in, held, pulled in, kissed: each phase's own gesture; one frame a quarter tick.
                EtoGesture[][] phases = {{EtoGesture.KISS}, {EtoGesture.BLUSH}, {EtoGesture.PULL}, {EtoGesture.KISSED}};
                float peak = 0.0F;
                for (EtoGesture[] phase : phases) {
                    for (int frame = 0; frame < 120; frame++) {
                        float now = blush.at(p.female(), p.blushes(phase[0]), phase[0], phase[0] == EtoGesture.KISSED
                            ? com.hex.kagune.control.npc.MoodKiss.Style.SWEET : style, clock);
                        check(Math.abs(now - last) <= 0.25F / (float)com.hex.kagune.control.client.animation.KissBlush.RISE + 1.0E-4F,
                            p.key() + " " + style + " " + phase[0] + ": never jumps");
                        if (p.female() && clock > 1000.0 + com.hex.kagune.control.client.animation.KissBlush.RISE + 1.0) {
                            check(now > 0.2F, p.key() + " " + style + " " + phase[0] + ": a woman blushes through every part of a kiss");
                        }
                        peak = Math.max(peak, now);
                        last = now;
                        clock += 0.25;
                    }
                }
                if (p.female()) {
                    check(Math.abs(peak - com.hex.kagune.control.npc.MoodKiss.blush(style)) < 0.01F, p.key() + " " + style + ": as deep as its mood ("
                        + peak + ")");
                }
                // Over: it stays a while, then fades away.
                float after = blush.at(p.female(), false, EtoGesture.NONE, com.hex.kagune.control.npc.MoodKiss.Style.SWEET, clock + 50.0);
                if (p.female()) {
                    check(after > 0.2F, p.key() + " " + style + ": still pink a while after");
                }
                float gone = 0.0F;
                for (double t = clock + 50.0; t < clock + 400.0; t += 1.0) {
                    gone = blush.at(p.female(), false, EtoGesture.NONE, com.hex.kagune.control.npc.MoodKiss.Style.SWEET, t);
                }
                check(gone == 0.0F, p.key() + " " + style + ": then gone");
            }
        }
        // A man who doesn't blush at being kissed doesn't; a shy one (his way) does.
        com.hex.kagune.control.client.animation.KissBlush plain = new com.hex.kagune.control.client.animation.KissBlush();
        float man = 0.0F;
        for (double t = 0.0; t < 40.0; t += 1.0) {
            man = plain.at(false, false, EtoGesture.KISS, com.hex.kagune.control.npc.MoodKiss.Style.SHY, t);
        }
        check(man == 0.0F, "a man not given to blushing doesn't");
    }

    /** 2.2.0: a partner lets go of a first blow or a missed kiss; only more, close together, put them out; sorry always helps. */
    static void tolerance() {
        com.hex.kagune.control.npc.Tolerance.Reaction shrug = com.hex.kagune.control.npc.Tolerance.Reaction.SHRUG;
        check(com.hex.kagune.control.npc.Tolerance.hit(0, false) == shrug && com.hex.kagune.control.npc.Tolerance.hit(1, true) == shrug
            && com.hex.kagune.control.npc.Tolerance.hit(1, false) == com.hex.kagune.control.npc.Tolerance.Reaction.PUT_OUT
            && com.hex.kagune.control.npc.Tolerance.hit(2, false) == com.hex.kagune.control.npc.Tolerance.Reaction.CROSS,
            "a first blow (a second mid-fight) brushed off; then put out; then cross");
        check(com.hex.kagune.control.npc.Tolerance.missed(0) == shrug && com.hex.kagune.control.npc.Tolerance.missed(1)
            == com.hex.kagune.control.npc.Tolerance.Reaction.PUT_OUT, "a first missed kiss shrugged off");
        for (com.hex.kagune.control.npc.Tolerance.Reaction r : com.hex.kagune.control.npc.Tolerance.Reaction.values()) {
            check(r.sulk <= 1200L && r.regard >= -6, r + ": never the five minutes of before, never a big blow to regard");
        }
        check(com.hex.kagune.control.npc.Tolerance.offences(3, 100L, 100L + com.hex.kagune.control.npc.Tolerance.FORGETS) == 0
            && com.hex.kagune.control.npc.Tolerance.offences(3, 100L, 200L) == 3 && com.hex.kagune.control.npc.Tolerance.offences(3, Long.MIN_VALUE, 0L) == 0,
            "what they hold fades after three minutes");
        check(com.hex.kagune.control.npc.Tolerance.apology(1) == com.hex.kagune.control.npc.Tolerance.Apology.FORGIVEN
            && com.hex.kagune.control.npc.Tolerance.apology(5) == com.hex.kagune.control.npc.Tolerance.Apology.A_MOMENT, "sorry forgives (a moment first, if it keeps happening)");
        for (Persona p : Persona.ALL) {
            for (int seed = 0; seed < 6; seed++) {
                for (String line : new String[] {com.hex.kagune.control.npc.Tolerance.shrug(p.key(), seed), com.hex.kagune.control.npc.Tolerance.shrugKiss(p.key(), seed),
                    com.hex.kagune.control.npc.Tolerance.forgiven(p.key(), seed % 2 == 0, seed), com.hex.kagune.control.npc.Tolerance.putOut(p.key(), seed)}) {
                    check(line != null && line.length() > 3, p.key() + " says it");
                }
            }
        }
    }

    /** 3.3.1: what Ken says as he teaches {@code lesson} (his LessonWords, package-private in npc: read by reflection). */
    static String kenTeaches(Teaching.Lesson lesson) {
        try {
            java.lang.reflect.Field field = Class.forName("com.hex.kagune.control.npc.KenScript").getDeclaredField("LESSONS");
            field.setAccessible(true);
            Object words = field.get(null);
            java.lang.reflect.Method teach = Class.forName("com.hex.kagune.control.npc.CompanionScript$LessonWords").getDeclaredMethod("teach", Teaching.Lesson.class,
                com.hex.kagune.control.npc.EtoTalk.Context.class);
            teach.setAccessible(true);
            return (String)teach.invoke(words, lesson, null);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
    }

    /** 3.3.6 (the owner: a jealous animation): EtoGesture.JEALOUS, appended last, acted out in four beats and round again. */
    static void jealous() {
        EtoGesture[] all = EtoGesture.values();
        check(all[EtoGesture.POSE.ordinal() - 1] == EtoGesture.JEALOUS && EtoGesture.JEALOUS.ticks == 140, "JEALOUS is appended (the gestures before keep their numbers)");
        // 4.0.2 (the owner: "The crack fingers animation is only Kaneki's"; Ayato "a massive poser"; Noro "tilts his head and looks at the
        // player weird"): neither of them ever cracks a finger; Ayato poses, Noro tilts his head.
        for (EtoMood mood : EtoMood.values()) {
            check(!java.util.Arrays.asList(Persona.AYATO.idles(mood)).contains(EtoGesture.CRACK) && !java.util.Arrays.asList(Persona.NORO.idles(mood)).contains(EtoGesture.CRACK),
                "no finger crack for Ayato or Noro (" + mood + ")");
        }
        // 4.0.8 (the owner: "I sent you the new ayato skin, make him a little muscular but way less than the male player."): Ayato's build a
        // share of the White Reaper's, way less than a male player's; nobody else's changed; his skin wide-armed, as he is drawn.
        // 4.1.48 (the owner: "ayato is meant to have muscles they're gone? Also give some to Noro, both less than the player, noro more than
        // ayato"; "Ayato and Noro should both be Alex models"): Ayato's build plainer, Noro's more; both slim. Then: "Reduce the player's
        // muscle size to be the same as Noro.": a male player's is Noro's.
        check(Persona.AYATO.muscle() >= 0.4 && Persona.NORO.muscle() > Persona.AYATO.muscle()
            && Persona.NORO.muscle() == com.hex.kagune.control.npc.ReaperBuild.PLAYER_SHARE
            && com.hex.kagune.control.npc.ReaperBuild.PLAYER_SHARE == 0.52
            && Persona.KEN.muscle() == 0.0 && Persona.ETO.muscle() == 0.0 && Persona.AYATO.slim() && Persona.NORO.slim() && Persona.KEN.slim(),
            "Ayato muscular, Noro more, a male player the same as Noro; both on the slim arms; Ken as he was");
        check(com.hex.kagune.control.npc.ReaperBuild.armOut(com.hex.kagune.control.npc.ReaperBuild.PLAYER_SHARE) == com.hex.kagune.control.npc.ReaperBuild.PLAYER_ARM_OUT
            && com.hex.kagune.control.npc.ReaperBuild.armOut(com.hex.kagune.control.npc.ReaperBuild.AYATO_SHARE) < com.hex.kagune.control.npc.ReaperBuild.PLAYER_ARM_OUT,
            "his arms out round his chest, less than a male player's");
        check(java.util.Arrays.asList(Persona.AYATO.anyIdle()).contains(EtoGesture.POSE) && java.util.Arrays.asList(Persona.NORO.anyIdle()).contains(EtoGesture.TILT)
            && !java.util.Arrays.asList(Persona.AYATO.anyIdle()).contains(EtoGesture.CRACK) && !java.util.Arrays.asList(Persona.NORO.anyIdle()).contains(EtoGesture.CRACK),
            "Ayato poses, Noro tilts his head");
        com.hex.kagune.control.client.animation.RigPose tilted = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.TILT, 45.0);
        check(tilted.headZ > 0.5F && Math.abs(tilted.rightArmX) < 1.0E-6F, "Noro's tilt: his head far over, nothing else moving");
        com.hex.kagune.control.client.animation.RigPose glare = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.JEALOUS, 40.0);
        com.hex.kagune.control.client.animation.RigPose huff = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.JEALOUS, 76.0);
        com.hex.kagune.control.client.animation.RigPose snub = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.JEALOUS, 100.0);
        com.hex.kagune.control.client.animation.RigPose done = com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.JEALOUS, 140.0);
        check(glare.spin > 0.8F && glare.headY < -0.8F && glare.rightArmX < -0.9F && glare.leftArmX < -0.9F,
            "she turns away, arms folded, and glares back over her shoulder at you");
        check(huff.drop < -0.5F, "a huff");
        boolean tapped = false;
        for (double t = 85.0; t < 130.0; t += 1.0) {
            tapped |= com.hex.kagune.control.client.animation.EtoPose.gesture(EtoGesture.JEALOUS, t).rightLegX < -0.15F;
        }
        check(snub.headY > 0.2F && tapped, "then the snub: head turned away, a foot tapping");
        check(Math.abs(done.spin) < 0.05F, "and round again at the end");
    }

    /**
     * 3.3.4 (the owner: "you can only have one wife, no matter what ... There's no divorce button ... The only way you can remarry is if
     * you kill them. But if it's an official NPC like Toka, Ken, or Ito, there's no way to unmarry"): nobody new while you have a partner;
     * a companion's death never ends it, a villager's does; asked while someone else's, each companion says no in their own words.
     */
    static void oneSpouse() {
        check(!Romance.mayDate(false, true) && !Romance.mayDate(true, false) && Romance.mayDate(false, false),
            "nobody new while you have a partner anywhere, dating or married");
        check(!Romance.endsWithDeath(true) && Romance.endsWithDeath(false), "a companion's death never ends it; a villager's does");
        check(Romance.villagerKey("123e4567-e89b-12d3-a456-426614174000") && !Romance.villagerKey("eto")
            && !Romance.villagerKey("touka") && !Romance.villagerKey("ken")
            && !Romance.villagerKey(null), "a villager's bond is its id; a companion's is her name");
        for (Persona p : new Persona[] {Persona.of("eto"), Persona.TOUKA, Persona.KEN}) {
            EtoTalk.Context taken = new EtoTalk.Context(80, 0, 0, true, false, false, true, true, null, true, false, 7, Romance.Stage.NONE, null, false,
                6000L, EtoMood.HAPPY, 0L, 0L, "wolf", true, true, true, EtoRules.Pact.NONE, 0, false, false, "Sam", true,
                com.hex.kagune.control.family.FamilyView.NONE, Teaching.View.NONE, true);
            EtoTalk.Reply no = p.script().answer(taken, NpcTalk.Choice.ASK_OUT);
            check(!taken.mayDate() && no.effect() != EtoTalk.Effect.DATE && !no.line().isBlank() && !no.line().contains("married"),
                p.key() + ": asked out by someone already someone else's: no (" + no.line() + ")");
        }
    }

    /** 2.1.8: Ken's final kakujas need their proof first (Teaching.proof); everything else is taught as before. */
    static void proofs() {
        for (Teaching.Lesson lesson : Teaching.ALL) {
            Teaching.Proof proof = Teaching.proof(lesson);
            boolean finalOne = lesson.gate() == Gate.RINKAKU_FULL_KAKUJA || lesson.gate() == Gate.RINKAKU_DRAGON || lesson.gate() == Gate.UKAKU_FINAL_KAKUJA;
            boolean kenMove = lesson.gate() == Gate.RINKAKU_HURL || lesson.gate() == Gate.RINKAKU_IMPALE; // 3.3.1
            // 4.0.0: every one of Ayato's and Noro's is earned (the owner: respect, "meaningful challenges", "progression challenges").
            // 4.1.7: and the General's (the three watches).
            boolean earned = lesson.teacher().equals("ayato") || lesson.teacher().equals("noro") || lesson.teacher().equals("general");
            check((finalOne || kenMove || earned) == (proof != null), lesson.id() + ": a proof first only for a final kakuja, Ken's Hurl and Impale, and Ayato's, Noro's and the General's");
            if (proof != null && earned) {
                check(proof.need() >= 1 && !Teaching.fill(proof.ask(), proof, 0).contains("{") && !Teaching.fill(proof.progress(), proof, 0).contains("{")
                    && proof.bar(lesson.teacher(), proof.need()).contains("Go and see him") && !proof.bar(lesson.teacher(), 0).contains("Go and see"),
                    lesson.id() + ": its proof said cleanly");
                continue;
            }
            if (proof != null) {
                check(proof.need() >= 3 && !Teaching.fill(proof.ask(), proof, 0).contains("{") && Teaching.fill(proof.progress(), proof, 2).contains("2")
                    && proof.bar(proof.need()).contains("Go and see him") && !proof.bar(1).contains("Go and see"), lesson.id() + ": its proof said cleanly");
            }
        }
        check(Teaching.proof(Teaching.byId("ken_dragon")).rank() >= 6, "the Dragon wants Doves worth fearing");
        Teaching.Proof throwing = Teaching.proof(Teaching.byId("ken_hurl"));
        Teaching.Proof crushing = Teaching.proof(Teaching.byId("ken_impale"));
        check(throwing.deed() == Teaching.Deed.THROWS && throwing.need() == 5 && throwing.bar(2).contains("thrown from your tails")
            && crushing.deed() == Teaching.Deed.CRUSHES && crushing.need() == 5 && crushing.bar(5).contains("crushed in your tails"),
            "3.3.1: the Hurl wants five bodies thrown from your tails, the Impale five crushed in them");
        for (String id : new String[] {"ken_hurl", "ken_impale"}) {
            Teaching.Lesson lesson = Teaching.byId(id);
            String words = kenTeaches(lesson);
            check(words.length() > 120 && !words.startsWith("Watch carefully") && words.contains("hold"), id + ": Ken teaches it in his own words, how to hold it");
        }
    }

    /**
     * 2.1.2: each companion's own courtship (Courtship): five chapters asking for different things (no two companions alike), each in its
     * own words; a bond from before read as its chapters; what is handed over taken whole; the first page's words for where they are.
     */
    static void courtship() {
        Set<String> ids = new HashSet<>();
        Set<String> asks = new HashSet<>();
        // 4.0.0: Ayato and Aogiri are named by the owner's ask (The Great Exploration).
        java.util.regex.Pattern banned = java.util.regex.Pattern.compile("Kaneki|Furuta|Takatsuki|Yoshimura", java.util.regex.Pattern.CASE_INSENSITIVE);
        for (Persona p : Persona.ALL) {
            List<com.hex.kagune.control.npc.Courtship.Chapter> chapters = com.hex.kagune.control.npc.Courtship.of(p.key());
            if (!p.romanceable()) {
                // 4.0.0: Noro: no romance (the owner): no courtship at all.
                check(chapters.isEmpty(), p.key() + ": no courtship");
                continue;
            }
            check(chapters.size() == com.hex.kagune.control.npc.Courtship.CHAPTERS, p.key() + " has five chapters");
            Set<com.hex.kagune.control.npc.Courtship.Task> tasks = new HashSet<>();
            for (com.hex.kagune.control.npc.Courtship.Chapter chapter : chapters) {
                check(ids.add(chapter.id()) && chapter.id().startsWith(p.key() + "_"), chapter.id() + ": its own");
                check(asks.add(chapter.ask()), chapter.id() + ": asked in its own words");
                tasks.add(chapter.task());
                check(chapter.need() >= 1 && chapter.need() <= 255, chapter.id() + ": a count the bond can keep");
                check(chapter.task() != com.hex.kagune.control.npc.Courtship.Task.GIVE || !chapter.items().isEmpty() && !chapter.itemName().isEmpty(),
                    chapter.id() + ": what to hand over, named");
                for (String text : new String[] {chapter.title(), chapter.ask(), chapter.progress(), chapter.journal()}) {
                    check(!text.isBlank() && text.length() <= com.hex.kagune.control.npc.scene.SceneBook.LINE_MOST && !banned.matcher(text).find(),
                        chapter.id() + ": a clean line: " + text);
                    String filled = com.hex.kagune.control.npc.Courtship.fill(text, chapter, 1);
                    check(!filled.contains("{"), chapter.id() + ": every count put in: " + filled);
                }
            }
            check(tasks.size() >= 4, p.key() + "'s chapters ask for " + tasks.size() + " kinds of thing");
            // 2.1.5: out of the mod's own world: nothing of the old shared errands' kind (kills of any monster, a sit, ink, beans...).
            for (com.hex.kagune.control.npc.Courtship.Chapter chapter : chapters) {
                check(chapter.task() != com.hex.kagune.control.npc.Courtship.Task.KILLS && chapter.task() != com.hex.kagune.control.npc.Courtship.Task.KILLS_WITH
                    && chapter.task() != com.hex.kagune.control.npc.Courtship.Task.SIT && chapter.task() != com.hex.kagune.control.npc.Courtship.Task.WALK_NIGHT
                    && (chapter.task() != com.hex.kagune.control.npc.Courtship.Task.GIVE || chapter.items().stream().allMatch(i -> i.startsWith("hexkagune:"))),
                    chapter.id() + ": asks for something of the mod's own world");
            }
            // The road: begun, a hand-over taken whole (never past what it needs), its moment closing it, the next on, then asking out.
            QuestLog.Bond bond = com.hex.kagune.control.npc.Courtship.begin(new QuestLog.Bond(70, 0L, NpcBonds.MET, 0));
            check(com.hex.kagune.control.npc.Courtship.chapter(p.key(), bond) == chapters.get(0) && EtoRules.step(bond) == EtoRules.Step.COURTING,
                p.key() + ": begun on chapter one");
            for (int i = 0; i < chapters.size(); i++) {
                com.hex.kagune.control.npc.Courtship.Chapter chapter = chapters.get(i);
                check(com.hex.kagune.control.npc.Courtship.close(p.key(), bond) == bond, chapter.id() + ": nothing closes before its task is done");
                if (chapter.task() == com.hex.kagune.control.npc.Courtship.Task.GIVE) {
                    String item = chapter.items().iterator().next();
                    check(com.hex.kagune.control.npc.Courtship.wants(p.key(), bond, item) && !com.hex.kagune.control.npc.Courtship.wants(p.key(), bond,
                        "minecraft:dirt"), chapter.id() + ": wants what it asks for, only that");
                    check(com.hex.kagune.control.npc.Courtship.takes(p.key(), bond, 64) == chapter.need(), chapter.id() + ": a stack held out: as much as it needs");
                }
                bond = com.hex.kagune.control.npc.Courtship.progress(p.key(), bond, chapter.task(), 300);
                check(EtoRules.count(bond) == chapter.need() && com.hex.kagune.control.npc.Courtship.ready(p.key(), bond), chapter.id() + ": done");
                bond = com.hex.kagune.control.npc.Courtship.close(p.key(), bond);
                check(com.hex.kagune.control.npc.Courtship.closed(bond, chapters.size()) == i + 1 && EtoRules.count(bond) == 0, chapter.id() + ": closed");
            }
            check(EtoRules.step(bond) == EtoRules.Step.ASK && com.hex.kagune.control.npc.Courtship.chapter(p.key(), bond) == null, p.key() + ": then ask");
            // 2.1.5: a Dove below the chapter's rank doesn't count; one at it does.
            for (int i = 0; i < chapters.size(); i++) {
                com.hex.kagune.control.npc.Courtship.Chapter chapter = chapters.get(i);
                if (chapter.rank() > 0) {
                    QuestLog.Bond on = new QuestLog.Bond(70, 0L, NpcBonds.MET | EtoRules.LINE_ON | i << com.hex.kagune.control.npc.Courtship.SHIFT
                        | com.hex.kagune.control.npc.Courtship.FROM_215, 0);
                    check(EtoRules.count(com.hex.kagune.control.npc.Courtship.progress(p.key(), on, chapter.task(), 1, chapter.rank() - 1)) == 0
                        && EtoRules.count(com.hex.kagune.control.npc.Courtship.progress(p.key(), on, chapter.task(), 1, chapter.rank())) == 1,
                        chapter.id() + ": only a Dove of Rank " + chapter.rank() + " or above");
                }
            }
            // 2.1.5: a chapter on from before 2.1.5 starts its count again (it counted something else).
            QuestLog.Bond before = new QuestLog.Bond(70, 0L, NpcBonds.MET | EtoRules.LINE_ON | 1 << com.hex.kagune.control.npc.Courtship.SHIFT, 6);
            QuestLog.Bond now = com.hex.kagune.control.npc.Courtship.migrate(before, 5);
            check(EtoRules.count(now) == 0 && com.hex.kagune.control.npc.Courtship.closed(now, 5) == 1 && com.hex.kagune.control.npc.Courtship.migrate(now, 5) == now,
                p.key() + ": a chapter from before 2.1.5 starts again, once");
            // A bond from before 2.1.2: the errands read as chapters.
            QuestLog.Bond twoDone = new QuestLog.Bond(100, 0L, NpcBonds.MET | EtoRules.LINE_ON | EtoRules.BOOKS_DONE | EtoRules.HUNT_DONE, 7);
            QuestLog.Bond migrated = com.hex.kagune.control.npc.Courtship.migrate(twoDone, 5);
            check(com.hex.kagune.control.npc.Courtship.closed(migrated, 5) == 2 && EtoRules.count(migrated) == 0 && !migrated.has(EtoRules.BOOKS_DONE)
                && EtoRules.step(migrated) == EtoRules.Step.COURTING, p.key() + ": two errands done before: chapter three, from nothing");
            QuestLog.Bond allDone = new QuestLog.Bond(100, 0L, NpcBonds.MET | EtoRules.LINE_ON | EtoRules.BOOKS_DONE | EtoRules.HUNT_DONE
                | EtoRules.FLOWER_DONE, 0);
            check(EtoRules.step(allDone) == EtoRules.Step.ASK && EtoRules.step(com.hex.kagune.control.npc.Courtship.migrate(allDone, 5)) == EtoRules.Step.ASK,
                p.key() + ": every errand done before: still waiting to be asked");
            check(com.hex.kagune.control.npc.Courtship.migrate(bond, 5) == bond, p.key() + ": a bond already kept the new way is left alone");
            // The first page: the courtship's answer, labelled for where they are.
            boolean mixed = true;
            EtoTalk.Context close = context(com.hex.kagune.control.npc.Courtship.BEGIN_AT, Romance.Stage.NONE, null, true, EtoMood.HAPPY, 1,
                Teaching.View.NONE, mixed, 6000L);
            check(p.script().main(close, "").choices().contains(NpcTalk.Choice.FAVOUR) && p.script().answer(close, NpcTalk.Choice.FAVOUR).effect()
                == EtoTalk.Effect.BEGIN && p.script().answer(close, NpcTalk.Choice.FAVOUR).line().equals(chapters.get(0).ask()),
                p.key() + ": close enough, the courtship begins in its first chapter's words");
            EtoTalk.Context holding = courting(p, 0, 0, 5);
            EtoTalk.Option give = option(p.script().main(holding, ""), NpcTalk.Choice.FAVOUR);
            boolean firstGives = chapters.get(0).task() == com.hex.kagune.control.npc.Courtship.Task.GIVE;
            check(give != null && (!firstGives || give.label().contains(chapters.get(0).itemName())), p.key() + ": what's held is offered by name");
            check(!firstGives || p.script().answer(holding, NpcTalk.Choice.FAVOUR).effect() == EtoTalk.Effect.COURT, p.key() + ": held out, handed over");
            EtoTalk.Context done = courting(p, 1, chapters.get(1).need(), 0);
            check(option(p.script().main(done, ""), NpcTalk.Choice.FAVOUR).label().equals("I did what you asked.")
                && p.script().answer(done, NpcTalk.Choice.FAVOUR).effect() == EtoTalk.Effect.COURT, p.key() + ": a task done, its moment");
            EtoTalk.Context dating = context(90, Romance.Stage.DATING, null, true, EtoMood.HAPPY, 2, Teaching.View.NONE, mixed, 6000L);
            check(!p.script().main(dating, "").choices().contains(NpcTalk.Choice.FAVOUR), p.key() + ": dating, no chores");
            EtoTalk.Option dates = null;
            for (EtoTalk.Option o : p.script().sweet(dating, "").options()) {
                if (o.key().equals("dates")) {
                    dates = o;
                }
            }
            check(dates != null && p.script().sweet(dating, "").options().size() <= 12, p.key() + ": a partner can ask for a date");
        }
    }

    /** {@code p}'s courtship on chapter {@code closed} + 1 with {@code done} of it done and {@code held} of what it asks for in hand. */
    static EtoTalk.Context courting(Persona p, int closed, int done, int held) {
        int flags = NpcBonds.MET | EtoRules.LINE_ON | closed << com.hex.kagune.control.npc.Courtship.SHIFT;
        return new EtoTalk.Context(80, flags, done, true, false, false, false, false, null, true, false, 4, Romance.Stage.NONE, null, false, 6000L,
            EtoMood.HAPPY, 0L, 0L, null, false, false, false, EtoRules.Pact.NONE, 0, false, false, "Sam", false,
            com.hex.kagune.control.family.FamilyView.NONE, Teaching.View.NONE, true, held);
    }

    static EtoTalk.Option option(EtoTalk.Page page, NpcTalk.Choice choice) {
        for (EtoTalk.Option o : page.options()) {
            if (o.choice() == choice) {
                return o;
            }
        }
        return null;
    }

    /** A talk with {@code persona}: who they are to it, everything else ordinary. */
    static EtoTalk.Context context(int regard, Romance.Stage stage, String claimedBy, boolean ghoul, EtoMood mood, int seed, Teaching.View teaching,
                                   boolean mixed, long dayTime) {
        return new EtoTalk.Context(regard, stage.together() ? EtoRules.DATING : 0, 0, ghoul, false, false, true, true, null, true, false, seed,
            stage, claimedBy, false, dayTime, mood, 3L, 0L, "wolf", true, true, true, EtoRules.Pact.NONE, 0, false, false, "Sam", false,
            com.hex.kagune.control.family.FamilyView.NONE, teaching, mixed);
    }

    static void talk() {
        for (Persona p : new Persona[] {Persona.TOUKA, Persona.KEN, Persona.AYATO, Persona.ROMA, Persona.BANJOU}) { // 4.1.49: and Roma; 4.1.54: Banjou
            Script script = p.script();
            Set<String> topicIds = new HashSet<>();
            int words = 0;
            for (int voice = 0; voice < 6; voice++) {
                int regard = new int[] {0, 0, NpcBonds.FRIENDLY, NpcBonds.FRIEND, NpcBonds.MOST, NpcBonds.MOST}[voice];
                Romance.Stage stage = voice == 4 ? Romance.Stage.DATING : voice == 5 ? Romance.Stage.MARRIED : Romance.Stage.NONE;
                String claimed = voice == 0 ? "Alex" : null;
                for (EtoMood mood : EtoMood.values()) {
                    for (int seed = 0; seed < 12; seed++) {
                        for (long time : new long[] {6000L, 18000L}) {
                            EtoTalk.Context c = context(regard, stage, claimed, true, mood, seed, Teaching.view(p.key(), true, KagunePath.UKAKU, 500,
                                gate -> false), true, time);
                            for (String line : new String[] {script.greeting(c), script.approach(c), script.welcome(c), script.title(c),
                                script.kissed(seed, voice == 5), script.hugged(seed), script.handTaken(seed), script.peck(seed), script.babyNight(c)}) {
                                check(line != null && !line.isBlank() && !line.contains("{name}") && !line.contains("{partner}") && !FORBIDDEN.matcher(line).find(),
                                    p.key() + ": a line for every voice and mood (" + voice + ", " + mood + "): " + line);
                                words++;
                            }
                            EtoTalk.Page main = script.main(c, "hi");
                            check(main.options().size() <= com.hex.kagune.control.network.S2CNpcTalk.MOST && main.choices().contains(NpcTalk.Choice.LEAVE),
                                p.key() + ": the first page fits the screen and has a way out");
                            for (NpcTalk.Choice choice : main.choices()) {
                                EtoTalk.Reply reply = script.answer(c, choice);
                                check(reply != null && reply.line() != null && !FORBIDDEN.matcher(reply.line()).find(),
                                    p.key() + ": an answer to " + choice);
                            }
                            EtoTalk.Page topics = script.talk(c, "go on");
                            for (EtoTalk.Option option : topics.options()) {
                                if (option.key().startsWith("topic:")) {
                                    EtoTopics.Topic topic = script.topicOf(option.key().substring(6));
                                    check(topic != null && script.topicIndex(topic) >= 0 && script.topicIndex(topic) < 14,
                                        p.key() + ": a topic offered is found (" + option.key() + ")");
                                    EtoTalk.Page page = script.topic(c, topic);
                                    check(!page.line().isBlank() && !FORBIDDEN.matcher(page.line()).find(), p.key() + ": a topic's answer");
                                    for (int i = 0; i < topic.open(c).size(); i++) {
                                        EtoTalk.Reply follow = script.follow(c, topic, i);
                                        check(!follow.line().isBlank() && !FORBIDDEN.matcher(follow.line()).find(), p.key() + ": a reply on " + topic.id());
                                    }
                                    topicIds.add(topic.id());
                                }
                            }
                            if (stage.together()) {
                                EtoTalk.Page sweet = script.sweet(c, "mm");
                                for (EtoTalk.Option option : sweet.options()) {
                                    if (option.key().startsWith("sweet:")) {
                                        check(!script.sweetly(c, option.key().substring(6)).line().isBlank(), p.key() + ": sweet " + option.key());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            check(topicIds.size() >= 6, p.key() + ": plenty to talk about (" + topicIds + ")");
            for (com.hex.kagune.control.npc.Display shown : com.hex.kagune.control.npc.Display.values()) {
                for (EtoVoice voice : EtoVoice.values()) {
                    String line = script.saw(shown, voice, false, 3);
                    check(shown == com.hex.kagune.control.npc.Display.NONE ? line.isEmpty() : !line.isBlank(), p.key() + ": a word on seeing " + shown);
                }
            }
            for (EtoInitiative.Act act : EtoInitiative.Act.values()) {
                check(act == EtoInitiative.Act.NONE || !script.initiative(act, 5).isBlank(), p.key() + ": a word as it starts " + act);
            }
            for (EtoInitiative.Moment moment : EtoInitiative.Moment.values()) {
                check(!script.moment(moment, true, 2).isBlank() && !script.moment(moment, false, 2).isBlank(), p.key() + ": a word at " + moment);
            }
            check(words > 1000, p.key() + ": many lines checked (" + words + ")");
        }
        // A lesson asked for: taught when ready, refused with the reason when not.
        Teaching.View ready = Teaching.view("touka", true, KagunePath.UKAKU, 250, gate -> false);
        EtoTalk.Reply learn = Persona.TOUKA.script().answer(context(20, Romance.Stage.NONE, null, true, EtoMood.HAPPY, 1, ready, true, 6000L),
            NpcTalk.Choice.LESSON);
        check(learn.effect() == EtoTalk.Effect.LEARN && learn.line().contains("Crystal"), "Touka teaches the crystals: " + learn.line());
        // (3.3.1: past Ken's Hurl and Impale, his first two now)
        Teaching.View trial = Teaching.view("ken", true, KagunePath.RINKAKU, 800, gate -> gate == Gate.RINKAKU_FOUR || gate == Gate.RINKAKU_HURL
            || gate == Gate.RINKAKU_IMPALE);
        EtoTalk.Reply torture = Persona.KEN.script().answer(context(20, Romance.Stage.NONE, null, true, EtoMood.HAPPY, 1, trial, true, 6000L),
            NpcTalk.Choice.LESSON);
        check(torture.effect() == EtoTalk.Effect.TORTURE, "Ken's centipede is lived through, not told");
        Teaching.View weak = Teaching.view("ken", true, KagunePath.RINKAKU, 300, gate -> gate == Gate.RINKAKU_FOUR || gate == Gate.RINKAKU_HURL
            || gate == Gate.RINKAKU_IMPALE);
        EtoTalk.Reply refused = Persona.KEN.script().answer(context(20, Romance.Stage.NONE, null, true, EtoMood.HAPPY, 1, weak, true, 6000L),
            NpcTalk.Choice.LESSON);
        check(refused.effect() == EtoTalk.Effect.NONE && refused.line().contains("750"), "too weak: told the mastery it takes: " + refused.line());
        check(!Persona.KEN.script().trialWords(Teaching.byId("ken_centipede"),
            context(20, Romance.Stage.NONE, null, true, EtoMood.HAPPY, 1, trial, true, 6000L)).isBlank(), "Ken's words as the trial ends");
        // Ken only for women: a man at his most regard is never offered the errands or a date.
        EtoTalk.Context man = context(NpcBonds.MOST, Romance.Stage.NONE, null, true, EtoMood.HAPPY, 1, Teaching.View.NONE, false, 6000L);
        EtoTalk.Context woman = context(NpcBonds.MOST, Romance.Stage.NONE, null, true, EtoMood.HAPPY, 1, Teaching.View.NONE, true, 6000L);
        check(!Persona.KEN.script().main(man, "").choices().contains(NpcTalk.Choice.FAVOUR)
            && Persona.KEN.script().main(woman, "").choices().contains(NpcTalk.Choice.FAVOUR)
            && Persona.KEN.script().answer(man, NpcTalk.Choice.ASK_OUT).effect() != EtoTalk.Effect.DATE,
            "Ken: a friend to a man, a date for a woman");
        check(Persona.TOUKA.script().main(man, "").choices().contains(NpcTalk.Choice.FAVOUR), "Touka would date anyone who earns it");
        // A baby: only a woman and a man, married, not already on the way.
        EtoTalk.Context wed = context(NpcBonds.MOST, Romance.Stage.MARRIED, null, true, EtoMood.HAPPY, 1, Teaching.View.NONE, true, 6000L);
        EtoTalk.Context same = context(NpcBonds.MOST, Romance.Stage.MARRIED, null, true, EtoMood.HAPPY, 1, Teaching.View.NONE, false, 6000L);
        check(wed.babyOpen() && !same.babyOpen(), "a baby can be asked for by a married woman and man, not two women or two men");
        check(Persona.ETO.script().greeting(wed).equals(EtoTalk.greeting(wed)) && Persona.ETO.script().kissed(3, true).equals(com.hex.kagune.control.npc.EtoLines.kissed(3, true)), "Eto's script is Eto's own words, unchanged");
    }

    /** The three ints of the NBT list of ints named {@code name} in {@code nbt} (its first), or null. */
    static int[] ints(byte[] nbt, String name) {
        byte[] key = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        outer:
        for (int i = 0; i + 3 + key.length + 5 + 12 <= nbt.length; i++) {
            if (nbt[i] != 9 || nbt[i + 1] != 0 || nbt[i + 2] != key.length) {
                continue;
            }
            for (int k = 0; k < key.length; k++) {
                if (nbt[i + 3 + k] != key[k]) {
                    continue outer;
                }
            }
            int at = i + 3 + key.length;
            if (nbt[at] != 3) {
                continue;
            }
            java.nio.ByteBuffer b = java.nio.ByteBuffer.wrap(nbt, at + 1, 16);
            if (b.getInt() != 3) {
                continue;
            }
            return new int[] {b.getInt(), b.getInt(), b.getInt()};
        }
        return null;
    }

    static void poses() {
        for (boolean leads : new boolean[] {false, true}) {
            for (EtoGesture gesture : EtoGesture.values()) {
                int length = gesture.held() ? 200 : gesture.ticks;
                for (int t = 0; t <= length; t += 3) {
                    RigPose p = EtoPose.gesture(gesture, t, leads);
                    check(gesture == EtoGesture.NONE ? p == null : p != null && Float.isFinite(p.rightArmX) && Float.isFinite(p.lean) && Math.abs(p.flip) < 0.6F
                        && Math.abs(p.drop) < (gesture == EtoGesture.SIT ? 10.01F : 4.0F) && Math.abs(p.lean) < 0.7F,
                        gesture + (leads ? " (led)" : "") + " at " + t + " is a sane pose (4.0.9: sitting, the hips down to the floor)");
                }
            }
        }
        RigPose kiss = EtoPose.gesture(EtoGesture.KISS, 30, true);
        check(kiss.rightArmX < -1.4F && kiss.leftArmX < -0.5F && kiss.lean > 0.1F, "Ken's kiss: a hand to their cheek, one at their waist, leaning down");
        RigPose her = EtoPose.gesture(EtoGesture.KISS, 30, false);
        check(her.rightArmX > 0.3F, "hers: hands behind her back, as always");
        RigPose peck = EtoPose.gesture(EtoGesture.PECK, EtoGesture.PECK.ticks / 2.0, true);
        check(peck.headX > 0.3F && peck.lean > 0.2F, "Ken's quick kiss: down to their forehead");
        RigPose hand = EtoPose.gesture(EtoGesture.HAND, 40, true);
        check(hand.leftArmHold >= 0.999F, "holding hands is the same for everyone (no led pose of its own)");
    }

    /** 1.3.26: the owner: Touka "attacks properly but she's not manifesting at all": her wings drew nothing (both passed as thrown). */
    static void toukaWingsShow() {
        double[] eye = {0.0, -1.5, -4.0};
        for (float deploy : new float[] {0.3F, 1.0F}) {
            for (float crystal : new float[] {0.0F, 1.0F}) {
                int quads = com.hex.kagune.control.ukaku.WingRig.build(com.hex.kagune.control.npc.WingRules.companion(deploy, 5.0, crystal, false, 0.0F,
                    com.hex.kagune.control.ukaku.WingRig.FORWARD, 0.0F, 0.0F, 1.0F), eye).size();
                check(quads > 50, "Touka's wings are drawn as they come out (deploy " + deploy + ", crystal " + crystal + "): " + quads + " quads");
            }
        }
        check(com.hex.kagune.control.ukaku.WingRig.build(com.hex.kagune.control.npc.WingRules.companion(0.0F, 5.0, 0.0F, true, 0.0F,
            com.hex.kagune.control.ukaku.WingRig.FORWARD, 0.0F, 0.0F, 1.0F), eye).isEmpty(), "put away, none are drawn");
        // 1.3.29: the owner: "only one wing, not two, and it should be smaller".
        com.hex.kagune.control.ukaku.WingRig.Input one = com.hex.kagune.control.npc.WingRules.companion(1.0F, 5.0, 1.0F, false, 0.0F,
            com.hex.kagune.control.ukaku.WingRig.FORWARD, 0.0F, 0.0F, 1.0F);
        com.hex.kagune.control.ukaku.WingRig.Input two = new com.hex.kagune.control.ukaku.WingRig.Input(one.form(), one.mastery(), one.deploy(), one.time(),
            one.crystalRight(), one.crystalLeft(), one.motion(), one.progress(), one.impact(), one.volley(), one.recoil(), one.crouching(), one.detail(),
            one.alpha(), one.dissolving(), one.aimRight(), one.aimLeft(), one.aim(), one.kickRight(), one.kickLeft(), 0.0F, 0.0F, 1.0F, 1.0F);
        int alone = com.hex.kagune.control.ukaku.WingRig.build(one, eye).size();
        int pair = com.hex.kagune.control.ukaku.WingRig.build(two, eye).size();
        check(alone > 50 && alone < pair * 0.65, "Touka's kagune: one wing, not two (" + alone + " quads against a pair's " + pair + ")");
        check(one.growRight() + one.growLeft() == 1.0F && com.hex.kagune.control.npc.WingRules.SIDE != 0, "one shoulder blade, every spike from it");
        check(com.hex.kagune.control.ukaku.UkakuRules.wingLength(com.hex.kagune.control.npc.WingRules.MASTERY, com.hex.kagune.control.ukaku.WingForm.TWIN)
            < com.hex.kagune.control.ukaku.UkakuRules.wingLength(380, com.hex.kagune.control.ukaku.WingForm.TWIN), "and smaller than it was");
    }
}
