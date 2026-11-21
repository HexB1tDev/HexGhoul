package com.hex.kagune.control;

import com.hex.kagune.control.npc.NpcBonds;
import com.hex.kagune.control.npc.Provocation;
import com.hex.kagune.control.npc.scene.SceneWords;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 4.3.1 (the owner, after 4.3.0, quoted exactly): "whenever you're talking to them and you reach a friendly state with them, they start
 * following you out of nowhere. Like, I don't understand. Like, without you telling them to follow, they just follow you for no given
 * reason." And: "I'm doing the quest with Ito, that you gotta kill a few CCG agents in front of her. Like, for some reason, I'm attacking the
 * CCG agents. She gets pissed at me and spam kills me ... official NPCs should not attack you, unless you fucking get rid of 50 heart, 50 HP
 * or something ... they can get pissed at you, sure. You not talk to you if they get pissed at you, but starting to kill you while you're
 * while you told them to follow you and all is ridiculous."
 *
 * <p>Pins: only "Follow me." starts anyone following (no scene deed does; a date's following ends with the date); an official fights a
 * player back only after {@link Provocation#FIGHTS_BACK} health done her on purpose (stray blows mid-fight are nothing, a follower's own
 * player's blows pass her by), never on sight for a poor regard nor for an insult, and a player's death ends every fight with them.
 */
public final class ProvocationTests {
    private static int checks;
    private static int failures;

    private ProvocationTests() {
    }

    private static void check(boolean good, String message) {
        checks++;
        if (!good) {
            failures++;
            System.out.println("FAILED: " + message);
        }
    }

    public static void main(String[] args) throws Exception {
        rules();
        questScene();
        brain();
        talk();
        follow();
        System.out.println("Provocation tests: " + checks + " checks " + (failures == 0 ? "passed" : "run, " + failures + " FAILED"));
        if (failures > 0) {
            throw new AssertionError(failures + " provocation checks failed");
        }
    }

    static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    /** The text of {@code source} from {@code from} to the next {@code to} (empty if either is missing). */
    static String between(String source, String from, String to) {
        int a = source.indexOf(from);
        if (a < 0) {
            return "";
        }
        int b = source.indexOf(to, a + from.length());
        return b < 0 ? "" : source.substring(a, b);
    }

    /** The pure rules, as the owner put them. */
    static void rules() {
        check(Provocation.FIGHTS_BACK == 50.0F, "the owner's 50 HP before an official fights back");
        check(!Provocation.fightsBack(49.9F) && Provocation.fightsBack(50.0F) && Provocation.fightsBack(80.0F), "50 health and more, not less");
        // Accidents: struck something else within the fight's window.
        check(Provocation.accident(1000L, 1030L, 60L) && Provocation.accident(1000L, 1060L, 60L), "struck another a moment ago: an accident");
        check(!Provocation.accident(1000L, 1061L, 60L), "long after: on purpose");
        check(!Provocation.accident(Long.MIN_VALUE, 1000L, 60L), "never struck anything else: on purpose");
        check(!Provocation.accident(2000L, 1000L, 60L), "a time from a later world: not an accident");
        // Harm: counted on purpose, nothing for accidents, forgotten.
        check(Provocation.harm(0.0F, Long.MIN_VALUE, 100L, 12.0F, false) == 12.0F, "a first blow counts its harm");
        check(Provocation.harm(12.0F, 100L, 120L, 12.0F, false) == 24.0F, "blows add up");
        check(Provocation.harm(24.0F, 100L, 120L, 30.0F, true) == 24.0F, "an accident adds nothing");
        check(Provocation.harm(40.0F, 100L, 100L + Provocation.FORGETS, 5.0F, false) == 5.0F, "forgotten after a while");
        check(Provocation.harm(40.0F, 100L, 100L + Provocation.FORGETS - 1, 5.0F, false) == 45.0F, "kept until then");
        check(Provocation.harm(0.0F, Long.MIN_VALUE, 100L, -3.0F, false) == 0.0F, "nothing negative");
        // Regard: once a second at most, never for an accident, and far less than a blow cost before.
        check(Provocation.stings(Long.MIN_VALUE, 100L, false), "a first blow on purpose stings");
        check(!Provocation.stings(100L, 100L, false) && !Provocation.stings(100L, 100L + Provocation.STING_EVERY - 1, false),
            "the rest of the same swing does not sting again");
        check(Provocation.stings(100L, 100L + Provocation.STING_EVERY, false), "the next blow does");
        check(!Provocation.stings(Long.MIN_VALUE, 100L, true), "an accident never stings");
        check(Provocation.STING < 0 && Provocation.STING > NpcBonds.BLOW, "a blow costs less than the old -30");
        // The old fault: two stray hits took a friendly official to an enemy who hunted them on sight.
        int regard = NpcBonds.FRIENDLY;
        long last = Long.MIN_VALUE;
        for (int hit = 0; hit < 2; hit++) {
            long now = 1000L + hit * 30L;
            if (Provocation.stings(last, now, false)) {
                last = now;
                regard += Provocation.STING;
            }
        }
        check(regard > NpcBonds.ENEMY, "two blows on purpose leave a friend short of an enemy (" + regard + ")");
        check(Provocation.SNUBS > 0L, "an insult that turns her: a while without a word");
    }

    /** Eto's "Let me watch you", as the owner played it: a long fight with the Doves round her; and a real beating, by contrast. */
    static void questScene() {
        float harm = 0.0F;
        long harmAt = Long.MIN_VALUE;
        long stung = Long.MIN_VALUE;
        int regard = 70;
        long struck = Long.MIN_VALUE;
        boolean fought = false;
        for (int tick = 0; tick < 1200; tick += 7) {
            struck = tick; // a Dove cut down
            boolean accident = Provocation.accident(struck, tick + 1, 60L);
            float after = Provocation.harm(harm, harmAt, tick + 1, 18.0F, accident); // her caught in the sweep
            if (Provocation.stings(stung, tick + 1, accident)) {
                stung = tick + 1;
                regard += Provocation.STING;
            }
            if (!accident) {
                harm = after;
                harmAt = tick + 1;
            }
            fought |= Provocation.fightsBack(after);
        }
        check(!fought && regard == 70 && harm == 0.0F, "a long fight beside her, caught again and again: nothing held against them");
        harm = 0.0F;
        harmAt = Long.MIN_VALUE;
        int blows = 0;
        for (long now = 5000L; !Provocation.fightsBack(harm) && blows < 100; now += 20L) {
            harm = Provocation.harm(harm, harmAt, now, 10.0F, false);
            harmAt = now;
            blows++;
        }
        check(blows == 5, "beaten on purpose, 10 a blow: she fights back at the fifth (" + blows + ")");
    }

    /** EtoBrain: no fight before the harm, none on sight, none past their death; their own follower spared; she lets go when she fights. */
    static void brain() throws Exception {
        String brain = read("src/main/java/com/hex/kagune/control/server/EtoBrain.java");
        String seek = between(brain, "private static LivingEntity seek(", "// 1.3.24: a hostile mob within five blocks of her");
        check(!seek.isEmpty() && seek.contains("mind.grudges.containsKey(player.m_20148_())") && !seek.contains("NpcBonds.ENEMY"),
            "a player fought on sight only for a grudge, never for an enemy's regard");
        String hurt = between(brain, "public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {", "private static void provoked(");
        check(!hurt.isEmpty() && !hurt.contains("EtoRules.BLOW") && !hurt.contains("NpcBonds.BLOW"), "a blow no longer costs the old -30");
        int fights = hurt.indexOf("if (!com.hex.kagune.control.npc.Provocation.fightsBack(harm)) {");
        int returns = fights < 0 ? -1 : hurt.indexOf("return;", fights);
        int provoked = hurt.indexOf("provoked(npc, mind, player, now);");
        int targets = hurt.indexOf("mind.target = attacker.m_19879_();");
        check(fights >= 0 && returns > fights && returns < provoked && provoked < targets, "short of 50 health on purpose: back before any fight");
        check(hurt.contains("Provocation.accident(STRUCK.getOrDefault(id, Long.MIN_VALUE), now, FIGHTING)")
            && hurt.contains("Provocation.harm(mind.harm.getOrDefault(id, 0.0F)") && hurt.contains("event.getAmount(), accident)"),
            "the harm counted from the blow, accidents mid-fight apart");
        check(hurt.contains("Provocation.stings(mind.stungAt.getOrDefault(id, Long.MIN_VALUE), now, accident)")
            && hurt.contains("bond.regard(bond.regard() + com.hex.kagune.control.npc.Provocation.STING)"), "regard stung once a second at most");
        check(!hurt.replace("provoked(npc, mind, player, now);", "").contains("mind.grudges.put("), "no grudge but through provoked()");
        String provokedBody = between(brain, "private static void provoked(", "public static void playerDied(");
        check(provokedBody.contains("mind.grudges.put(player.m_20148_(), now + 400L);")
            && provokedBody.contains("if (player.m_20148_().equals(npc.following())) {\n            npc.follow(null);"),
            "provoked: the grudge, and she walks with them no more");
        String died = between(brain, "public static void playerDied(LivingDeathEvent event) {", "/**");
        check(brain.contains("@SubscribeEvent(priority = EventPriority.LOWEST)\n    public static void playerDied(")
            && died.contains("for (EtoNpc npc : LIVE) {") && died.contains("mind.grudges.remove(id);") && died.contains("mind.harm.remove(id);")
            && died.contains("if (mind.target == player.m_19879_()) {\n                mind.target = 0;"), "a player dead: no official still after them");
        String spared = between(brain, "public static boolean spared(ServerPlayer player, LivingEntity body) {", "} catch (RuntimeException failure) {");
        check(spared.contains("boolean along = player.m_20148_().equals(npc.following()) && !JoinServer.sparring(player, npc);")
            && spared.contains("return along ||"), "the player she follows: their blows pass her by (not in a spar)");
        String attacked = between(brain, "public static void attacked(net.minecraftforge.event.entity.living.LivingAttackEvent event) {", "/**");
        check(attacked.contains("spared(by, npc)") && attacked.contains("event.setCanceled(true);"), "spared: the blow cancelled");
    }

    /** The talk: an insult that turns her ends the talk, never in a fight; no reply promises one. */
    static void talk() throws Exception {
        String server = read("src/main/java/com/hex/kagune/control/server/EtoServer.java");
        String anger = between(server, "            case ANGER -> {", "            case FOLLOW ->");
        check(!anger.isEmpty() && !anger.contains("grudges") && anger.contains("npc.mind.ignoring.put(player.m_20148_(), now + com.hex.kagune.control.npc.Provocation.SNUBS);"),
            "insulted into an enemy: she won't talk to them, she doesn't fight them");
        check(anger.contains("npc.follow(null);"), "and walks with them no more");
        for (String file : List.of("npc/EtoTalk.java", "npc/ToukaScript.java", "npc/AyatoScript.java", "npc/GeneralScript.java")) {
            String text = read("src/main/java/com/hex/kagune/control/" + file);
            for (String promise : List.of("Fine. Let's play.", "You want to die. I can do that.", "I'm not holding back.", "you're a threat")) {
                check(!text.contains(promise), file + ": no promise of a fight that never comes (" + promise + ")");
            }
        }
    }

    /** Only "Follow me." starts anyone following: no scene deed does; a date's ends with it. */
    static void follow() throws Exception {
        check(!SceneWords.effect("eto", "follow") && !SceneWords.effect("touka", "follow") && !SceneWords.effect("ken", "follow"),
            "no companion's scene answer may make her follow");
        check(!SceneWords.effect("ally", "follow"), "nor an ally's");
        check(SceneWords.effect("eto", "stay") && SceneWords.effect("ally", "stay"), "waiting still can");
        String scenes = read("src/main/java/com/hex/kagune/control/server/SceneServer.java");
        check(!scenes.contains("case \"follow\"") && !scenes.contains("does(\"follow\")") && !scenes.contains("Effect.FOLLOW"),
            "the scenes' answers never set anyone following");
        for (String name : List.of("eto", "touka", "ken", "ally", "ayato", "banjou", "general", "roma")) {
            String text = read("src/main/resources/data/hexkagune/talk/" + name + ".txt");
            for (String line : text.split("\n")) {
                if (line.startsWith(">") && line.contains("|")) {
                    String deeds = line.substring(line.lastIndexOf('|') + 1);
                    check(!(" " + deeds + " ").contains(" follow "), name + ".txt: an answer that would make them follow: " + line);
                }
            }
        }
        // The one way: asked.
        String server = read("src/main/java/com/hex/kagune/control/server/EtoServer.java");
        check(server.contains("case FOLLOW -> npc.follow(player.m_20148_());"), "\"Follow me.\" still makes her follow");
        String ghoulTalk = read("src/main/java/com/hex/kagune/control/server/GhoulNpcTalk.java");
        check(ghoulTalk.contains("case FOLLOW -> GhoulNpcBrain.follow(npc, true);"), "and an ally");
        // A date: for the date only.
        String trials = read("src/main/java/com/hex/kagune/control/server/CourtTrials.java");
        String dateOn = between(trials, "static void dateOn(", "static boolean dating(");
        check(dateOn.contains("if (!player.m_20148_().equals(npc.following())) {\n            npc.followForDate(player.m_20148_());")
            && !dateOn.contains("npc.follow(player"), "a date: she follows them there, for the date only");
        String eto = read("src/main/java/com/hex/kagune/control/npc/EtoNpc.java");
        String followBody = between(eto, "public void follow(java.util.UUID player) {", "public void followForDate(");
        check(followBody.contains("tag.m_128473_(DATE_FOLLOW);"), "told to follow (or stop): for good, no longer the date's");
        check(between(eto, "public void followForDate(java.util.UUID player) {", "}\n").contains("this.follow(player);")
            && eto.contains("this.getPersistentData().m_128379_(DATE_FOLLOW, true);"), "the date's following marked as such");
        String brain = read("src/main/java/com/hex/kagune/control/server/EtoBrain.java");
        check(brain.contains("if (now % 20L == 9L && npc.followsForDate() && !CourtTrials.dating(npc.following(), npc.persona().key())) {\n"
            + "            npc.follow(null);"), "the date over (had, stood up, logged out, restarted): she stops following");
        check(trials.contains("return d != null && d.key().equals(key);"), "a date is on only with her");
    }
}
