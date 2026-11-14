package com.hex.kagune.control;

import com.hex.kagune.control.quest.QuestLog;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 4.1.101: rules of 4.1.96 to 4.1.98 their sabotage lists (made late: the owner had asked to skip testing for speed) found untested.
 * 4.1.97 (the owner: "Its not needed in chat telling you how to craft, details are in the Quest menu"; "that applies for every quest
 * info"): quest news on the action bar, the chat keeping only NPC speech and warnings. 4.1.96: a mission's new try counts from nothing;
 * /kagune places says where each official is. 4.1.98: an official's remake, a body of hers sent away and her death told in the log (what
 * the owner is asked for if a leader is still missing).
 */
public final class TraceTests {
    private static int passed;
    private static final List<String> FAILURES = new ArrayList<>();

    private static void check(boolean condition, String description) {
        if (condition) {
            passed++;
        } else {
            FAILURES.add(description);
        }
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    public static void main(String[] args) throws Exception {
        notices();
        retries();
        whereabouts();
        traces();
        if (!FAILURES.isEmpty()) {
            FAILURES.forEach(failure -> System.out.println("FAILED: " + failure));
            System.exit(1);
        }
        System.out.println("Trace tests: " + passed + " checks passed");
    }

    /** Every displayClientMessage call in {@code source}: {its text argument, its last argument}. */
    static List<String[]> messages(String source) {
        List<String[]> out = new ArrayList<>();
        for (int at = source.indexOf("m_5661_("); at >= 0; at = source.indexOf("m_5661_(", at + 1)) {
            int i = at + "m_5661_(".length();
            int depth = 1;
            int last = -1;
            boolean quoted = false;
            for (; i < source.length() && depth > 0; i++) {
                char c = source.charAt(i);
                if (c == '"' && source.charAt(i - 1) != '\\') {
                    quoted = !quoted;
                } else if (!quoted && (c == '(' || c == '[' || c == '{')) {
                    depth++;
                } else if (!quoted && (c == ')' || c == ']' || c == '}')) {
                    depth--;
                } else if (!quoted && c == ',' && depth == 1) {
                    last = i;
                }
            }
            String inner = source.substring(at + "m_5661_(".length(), i - 1);
            int split = last - (at + "m_5661_(".length());
            out.add(split < 0 ? new String[] {inner, ""} : new String[] {inner.substring(0, split), inner.substring(split + 1).trim()});
        }
        return out;
    }

    /** 4.1.97: the quests' and gangs' news on the action bar; in the chat only an NPC speaking or a warning. */
    private static void notices() throws Exception {
        String[] speechOrWarning = {"JoinVoice.say(", "CourtVoice.spar(", "GangWords.demand(", "has banned you for killing its own",
            "you are not welcome here anymore", "turns on you.", "\"§7You left \"", "married to the leader of", "\"You joined \""};
        int bar = 0;
        for (String file : new String[] {"JoinServer", "QuestServer", "GangServer"}) {
            String source = read("src/main/java/com/hex/kagune/control/server/" + file + ".java");
            for (String[] message : messages(source)) {
                if (message[1].equals("true")) {
                    bar++;
                    continue;
                }
                boolean allowed = false;
                for (String kind : speechOrWarning) {
                    allowed |= message[0].contains(kind);
                }
                check(allowed, file + ": quest news in the chat (the quest menu and the action bar hold it): " + message[0].replaceAll("\\s+", " "));
            }
        }
        check(bar >= 22, "the quests' and gangs' news on the action bar (" + bar + ")");
        String join = read("src/main/java/com/hex/kagune/control/server/JoinServer.java");
        check(join.contains("    static void flash(ServerPlayer player, Live live, String text, long now) {\n        player.m_5661_(Component.m_237113_(text), true);\n"
            + "        live.quietUntil = now + 60L;"), "a trial's news flashed on the action bar, held three seconds");
        check(join.contains("        if (now < live.quietUntil) {\n            return; // 4.1.97: a flash is showing"), "the trial's own bar waits for a flash");
        check(!join.contains("Ken hands you the makings"), "how the coffee is made is in the quest menu, not the chat");
    }

    /** 4.1.96: a mission begun again counts from nothing. */
    private static void retries() {
        QuestLog log = new QuestLog();
        log.startMission("SHIFT", 0L, com.hex.kagune.control.quest.Missions.limit("SHIFT"));
        com.hex.kagune.control.quest.Quest quest = log.mission("SHIFT").quest();
        check(log.missionProgress("SHIFT", 2) && log.progress(quest, 0) == 2, "a mission's progress counted (2 served)");
        check(!log.missionProgress("SHIFT", 2), "the same count again changes nothing");
        log.endMission("SHIFT", false, 100L);
        log.startMission("SHIFT", 100L + com.hex.kagune.control.quest.Missions.COOLDOWN, com.hex.kagune.control.quest.Missions.limit("SHIFT"));
        check(log.progress(log.mission("SHIFT").quest(), 0) == 0, "a new try counts from nothing (4.1.96)");
    }

    /** 4.1.96: /kagune places, said by a player, adds where each official is now. */
    private static void whereabouts() throws Exception {
        String commands = read("src/main/java/com/hex/kagune/control/server/ControlCommands.java");
        int places = commands.indexOf("Commands.m_82127_(\"places\")");
        int added = commands.indexOf("lines.addAll(UniqueNpcs.whereabouts(player.m_284548_()));", places);
        check(places > 0 && added > places && added - places < 1200, "/kagune places lists where each official is");
        String unique = read("src/main/java/com/hex/kagune/control/server/UniqueNpcs.java");
        int where = unique.indexOf("public static java.util.List<String> whereabouts(ServerLevel level) {");
        check(where > 0 && unique.indexOf("for (com.hex.kagune.control.npc.Persona persona : com.hex.kagune.control.npc.Persona.ALL) {", where) > where
            && unique.indexOf("if (com.hex.kagune.control.npc.HomePlace.of(key) == null) {", where) > where
            && unique.indexOf("line += \"here now at x \"", where) > where && unique.indexOf("line += \"dead; back at home a day after\";", where) > where,
            "each official with a place: here now (where), dead (back a day after), or what else");
    }

    /** 4.1.98: what the owner is asked to send if a leader is still missing: each remake, each body sent away (why), each death. */
    private static void traces() throws Exception {
        String unique = read("src/main/java/com/hex/kagune/control/server/UniqueNpcs.java");
        check(unique.contains("            com.hex.kagune.KaguneMod.LOGGER.info(\"HexKagune: \" + key + \" made at her spot \" + spot[0] + \" \" + spot[1] + \" \" + spot[2]"),
            "an official made at her spot: told in the log, where");
        String eto = read("src/main/java/com/hex/kagune/control/server/EtoBrain.java");
        int gone = eto.indexOf("    static void gone(EtoNpc npc, String why) {");
        check(gone > 0 && eto.indexOf("if (com.hex.kagune.control.npc.HomePlace.of(npc.persona().key()) != null) {", gone) - gone < 120
            && eto.indexOf("+ (int)Math.floor(npc.m_20186_()) + \" \" + (int)Math.floor(npc.m_20189_()) + \" sent away: \" + why);", gone) - gone < 500,
            "a body of an official sent away: told in the log, where and why");
        for (String why : new String[] {"gone(npc, \"the world's own is elsewhere (\" + slot.body + \")\");", "gone(npc, \"her body is about elsewhere\");",
            "gone(npc, \"another body holds her slot\");"}) {
            check(eto.contains(why), "told why: " + why);
        }
        check(eto.contains("com.hex.kagune.KaguneMod.LOGGER.info(\"HexKagune: \" + npc.persona().key() + \" died at \"")
            && eto.contains("+ \"); back at her spot a day after\");"), "an official's death told in the log, where and of what");
    }
}
