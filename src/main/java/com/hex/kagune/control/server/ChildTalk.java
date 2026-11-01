package com.hex.kagune.control.server;

import com.hex.kagune.control.family.ChildLines;
import com.hex.kagune.control.family.ChildRules;
import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CNpcTalk;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.NpcTalk;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.10: talking with a child, on the same screen as every NPC: its parents may pick it up and put it down (a baby or a toddler), play
 * with it, teach it to fight (a child or a teen, a lesson a day), tell it to follow or to wait where it is; anyone else gets a shy hello.
 * Shift and right-click puts down a child you carry.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class ChildTalk {
    /** 1.3.20: the page open (its keys), and a moment chosen on it that begins once the talk is closed (so it is seen). */
    private record Talk(int npc, List<String> keys, com.hex.kagune.control.family.ChildMoment queued) {
    }

    private static final Map<UUID, Talk> TALKS = new HashMap<>();
    static final double REACH = 4.5;

    private ChildTalk() {
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof ChildNpc npc)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player) || npc.m_20280_(player) > REACH * REACH
            || npc.mind == null) {
            return;
        }
        if (npc.m_5803_()) {
            // 1.3.21: fast asleep in its bed: not woken by a word (hurt, picked up, or the morning wakes it); a parent can tuck it in.
            // 1.3.37: a hand on it half wakes it, drowsy, by its age.
            asleep(player, npc, drowsyLine(player, npc), null);
            return;
        }
        if (player.m_6047_() && player.m_20148_().equals(npc.mind.carrier)) {
            ChildBrain.putDown(npc, npc.mind);
            Children.Child child = Children.get(player.m_284548_(), npc.childId());
            if (child != null) {
                ChildBrain.say(player, npc, child, ChildLines.down(npc.mind.stage, npc.m_217043_().m_188503_(100)));
            }
            return;
        }
        open(player, npc, null);
    }

    /** The page: what it says ({@code line}, or a greeting), and what can be done with it. */
    static void open(ServerPlayer player, ChildNpc npc, String line) {
        open(player, npc, line, false, null);
    }

    /**
     * 1.3.20: the main page, or the play page ({@code play}: the moments its stage can have, its shoulders, back); {@code queued}: a moment
     * chosen, to begin once the talk closes (the page then has only its line and goodbye).
     */
    static void open(ServerPlayer player, ChildNpc npc, String line, boolean play, com.hex.kagune.control.family.ChildMoment queued) {
        ServerLevel level = player.m_284548_();
        Children.Child child = Children.get(level, npc.childId());
        if (child == null) {
            return;
        }
        if (line == null && !play && queued == null) {
            // 1.3.29: it came up to them with something: the talk opens on it.
            SceneServer.Pending pending = SceneServer.take(player, npc.m_19879_());
            com.hex.kagune.control.npc.scene.Scene scene = pending == null ? null : SceneServer.scene(pending.scene());
            if (scene != null) {
                com.hex.kagune.control.npc.scene.SceneContext sc = SceneServer.context(player, npc, child, level.m_46467_());
                scene(player, npc, child, scene, scene.first(), sc, pending.line());
                return;
            }
        }
        ChildStage stage = child.stage(level.m_46467_());
        boolean parent = child.parent(Pregnancies.player(player.m_20148_()));
        String call = ChildLines.call(stage, ChildBrain.isMother(child, player));
        int seed = npc.m_217043_().m_188503_(1000);
        List<String> keys = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        boolean carrying = player.m_20148_().equals(npc.mind.carrier);
        if (queued != null) {
            // Chosen: its line, then goodbye; the moment begins as the talk closes.
        } else if (parent && play) {
            for (com.hex.kagune.control.family.ChildMoment moment : com.hex.kagune.control.family.ChildMoment.PLAY) {
                if (moment.fits(stage)) {
                    add(keys, labels, "m:" + moment.name(), moment.label());
                }
            }
            if (com.hex.kagune.control.family.ChildRules.shoulders(stage)) {
                boolean riding = carrying && npc.mind.ride == com.hex.kagune.control.family.ChildRules.RIDE_SHOULDERS;
                add(keys, labels, riding ? "down" : "shoulders", riding ? "Down you get." : "Up on my shoulders!");
            }
            add(keys, labels, "back", "...");
        } else if (parent) {
            if (stage.carried() || carrying) {
                add(keys, labels, carrying ? "down" : "up", carrying ? "Put you down." : "Pick up.");
            }
            add(keys, labels, "play", stage == ChildStage.TEEN ? "Want to hang out?" : "Let's play!");
            if (stage.learns()) {
                add(keys, labels, "teach", "Let me teach you to fight.");
            }
            // 1.3.29: talking, at every age (a baby is talked to; it answers its own way).
            add(keys, labels, "talk", switch (stage) {
                case BABY -> "Talk to the baby.";
                case TODDLER -> "Tell me something!";
                case CHILD -> "How are you?";
                case TEEN -> "Talk to me.";
            });
            if (stage.walks()) {
                boolean following = player.m_20148_().equals(npc.following());
                add(keys, labels, following ? "stay" : "follow", following ? "Wait here." : "Follow me.");
            }
            // 1.3.21: hand in hand; fed; to bed; home.
            boolean handed = carrying && npc.mind.ride == com.hex.kagune.control.family.ChildRules.RIDE_HAND;
            if (com.hex.kagune.control.family.ChildRules.hand(stage) && (!carrying || handed)) {
                add(keys, labels, handed ? "down" : "hand", handed ? "Let go." : "Hold my hand.");
            }
            if (!player.m_21205_().m_41619_() && (player.m_21205_().m_41614_() || com.hex.kagune.control.ControlItems.ghoulMeat(player.m_21205_()))) {
                add(keys, labels, "feed", "Here, eat this. (the food in your hand)");
            }
            Households.Home house = Households.forChild(level, child);
            if (house != null && HomeLife.night(level.m_46468_(), true) && !npc.m_5803_()) {
                add(keys, labels, "bed", "Bedtime.");
            }
            if (house != null && (npc.following() != null || npc.waiting()) && !house.at(npc.home())) {
                add(keys, labels, "home", "Go home.");
            }
        } else {
            add(keys, labels, "hi", "Hello there.");
        }
        TALKS.put(player.m_20148_(), new Talk(npc.m_19879_(), keys, queued));
        int[] choices = new int[keys.size() + 1];
        String[] shown = new String[keys.size() + 1];
        for (int i = 0; i < keys.size(); i++) {
            choices[i] = NpcTalk.Choice.SAYS[i].ordinal();
            shown[i] = labels.get(i);
        }
        choices[keys.size()] = NpcTalk.Choice.LEAVE.ordinal();
        shown[keys.size()] = "";
        String said = line != null ? line : parent ? ChildLines.greet(stage, call, seed) : stage == ChildStage.BABY ? "*stares at you*" : "...Hi.";
        ControlNetwork.sendTo(player, new S2CNpcTalk(npc.m_19879_(), child.name, title(child, stage, player), parent ? 100 : 30, parent, -1, said, choices, shown));
    }

    /**
     * 1.3.21: the page of a child asleep in its bed: only its parents may tuck it in ({@code queued}: chosen, done once the talk closes,
     * the page then only its goodbye).
     */
    private static void asleep(ServerPlayer player, ChildNpc npc, String line, com.hex.kagune.control.family.ChildMoment queued) {
        ServerLevel level = player.m_284548_();
        Children.Child child = Children.get(level, npc.childId());
        if (child == null) {
            return;
        }
        List<String> keys = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        boolean parent = child.parent(Pregnancies.player(player.m_20148_()));
        if (queued == null && parent && !ChildBrain.busy(npc.mind, level.m_46467_())) {
            add(keys, labels, "tuck", "*tuck them in*");
        }
        if (queued == null) {
            // 1.3.37: a hand on their hair, or a word to go back to sleep.
            if (parent) {
                add(keys, labels, "stroke", "*stroke their hair*");
            }
            add(keys, labels, "back", "Go back to sleep.");
        }
        TALKS.put(player.m_20148_(), new Talk(npc.m_19879_(), keys, queued));
        int[] choices = new int[keys.size() + 1];
        String[] shown = new String[keys.size() + 1];
        for (int i = 0; i < keys.size(); i++) {
            choices[i] = NpcTalk.Choice.SAYS[i].ordinal();
            shown[i] = labels.get(i);
        }
        choices[keys.size()] = NpcTalk.Choice.LEAVE.ordinal();
        shown[keys.size()] = "";
        ControlNetwork.sendTo(player, new S2CNpcTalk(npc.m_19879_(), child.name, "Asleep", 100, true, -1, line, choices, shown));
    }

    /** 1.3.37: what a sleeping child mumbles, half woken (a baby sleeps on). */
    private static String drowsyLine(ServerPlayer player, ChildNpc npc) {
        Children.Child child = Children.get(player.m_284548_(), npc.childId());
        if (child == null) {
            return "*fast asleep, breathing softly*";
        }
        return com.hex.kagune.control.family.Drowsy.child(child.stage(player.m_284548_().m_46467_()), npc.m_217043_().m_188503_(100));
    }

    private static void add(List<String> keys, List<String> labels, String key, String label) {
        if (keys.size() < NpcTalk.Choice.SAYS.length && keys.size() < S2CNpcTalk.MOST - 1) {
            keys.add(key);
            labels.add(label);
        }
    }

    /** "Baby · your daughter", "Teen · Eto's son"... */
    private static String title(Children.Child child, ChildStage stage, ServerPlayer player) {
        String age = switch (stage) {
            case BABY -> "Baby";
            case TODDLER -> "Toddler";
            case CHILD -> "Child";
            case TEEN -> "Teen";
        };
        boolean girl = child.genome().gender().female();
        String whose = child.parent(Pregnancies.player(player.m_20148_())) ? "your " + (girl ? "daughter" : "son")
            : child.motherName + "'s " + (girl ? "daughter" : "son");
        return age + " · " + whose;
    }

    public static void heard(ServerPlayer player, int npcId, int ordinal) {
        Talk talk = TALKS.get(player.m_20148_());
        NpcTalk.Choice choice = NpcTalk.Choice.byOrdinal(ordinal);
        if (choice == NpcTalk.Choice.ANSWER) {
            // 1.3.29: the Answer key: it came up to them with something.
            if (talk == null && player.m_284548_().m_6815_(npcId) instanceof ChildNpc npc && npc.mind != null && npc.m_20280_(player) <= 24.0 * 24.0
                && !npc.m_5803_()) {
                open(player, npc, null);
            }
            return;
        }
        if (talk == null || talk.npc() != npcId) {
            return;
        }
        ServerLevel level = player.m_284548_();
        // 1.3.29: a scene's answer may come while it is still on its way (further off).
        boolean scenic = false;
        for (String key : talk.keys()) {
            scenic |= key.startsWith("s:") || key.startsWith("t:");
        }
        double keep = scenic ? 16.0 : 8.0;
        if (choice == NpcTalk.Choice.LEAVE || !(level.m_6815_(npcId) instanceof ChildNpc npc) || npc.mind == null || npc.m_20280_(player) > keep * keep) {
            TALKS.remove(player.m_20148_());
            // 1.3.20: a moment chosen begins now the talk is closed.
            if (talk.queued() != null && level.m_6815_(npcId) instanceof ChildNpc chosen && chosen.mind != null) {
                boolean began = chosen.m_20280_(player) <= 12.0 * 12.0 && ChildBrain.start(chosen, talk.queued(), player, level.m_46467_());
                if (!began && talk.queued() == com.hex.kagune.control.family.ChildMoment.FEED && chosen.mind.food != null) {
                    // It couldn't eat it now: the food back in their hands.
                    net.minecraft.world.item.ItemStack back = chosen.mind.food;
                    chosen.mind.food = null;
                    if (!player.m_36356_(back)) {
                        player.m_36176_(back, false);
                    }
                }
            }
            return;
        }
        if (talk.queued() != null) {
            return;
        }
        int index = -1;
        for (int i = 0; i < NpcTalk.Choice.SAYS.length; i++) {
            if (NpcTalk.Choice.SAYS[i] == choice) {
                index = i;
            }
        }
        Children.Child child = Children.get(level, npc.childId());
        if (index < 0 || index >= talk.keys().size() || child == null) {
            return;
        }
        if ("back".equals(talk.keys().get(index)) || "stroke".equals(talk.keys().get(index))) {
            // 1.3.37: settled back to sleep (stroked: a parent's hand on their hair, and it eases them).
            boolean stroked = "stroke".equals(talk.keys().get(index));
            TALKS.remove(player.m_20148_());
            if (npc.m_5803_()) {
                String line = com.hex.kagune.control.family.Drowsy.childSettles(child.stage(level.m_46467_()), stroked, npc.m_217043_().m_188503_(100));
                ChildBrain.say(player, npc, child, line);
            }
            return;
        }
        if ("tuck".equals(talk.keys().get(index))) {
            // Asleep: the covers smoothed once the talk closes (awake again meanwhile: its own page).
            if (npc.m_5803_()) {
                asleep(player, npc, "*stirs, mumbling, and snuggles in*", com.hex.kagune.control.family.ChildMoment.TUCK);
            } else {
                open(player, npc, null);
            }
            return;
        }
        if (npc.m_5803_()) {
            asleep(player, npc, "*fast asleep, breathing softly*", null); // it fell asleep while they talked
            return;
        }
        long now = level.m_46467_();
        ChildStage stage = child.stage(now);
        int seed = npc.m_217043_().m_188503_(1000);
        String reply;
        switch (talk.keys().get(index)) {
            case "up" -> {
                ChildBrain.pickUp(npc, player);
                reply = ChildLines.up(stage, seed);
            }
            case "down" -> {
                ChildBrain.putDown(npc, npc.mind);
                reply = ChildLines.down(stage, seed);
            }
            case "play" -> {
                open(player, npc, ChildLines.play(stage, seed), true, null);
                return;
            }
            case "back" -> {
                open(player, npc, null, false, null);
                return;
            }
            case "shoulders" -> {
                ChildBrain.pickUp(npc, player, com.hex.kagune.control.family.ChildRules.RIDE_SHOULDERS);
                open(player, npc, stage == ChildStage.TODDLER ? "Wheee! I'm so tall!" : "Whoa! I can see everything from up here!", true, null);
                return;
            }
            case "teach" -> {
                if (ChildRules.teachable(stage, child.skill, child.lesson, now)) {
                    child.skill++;
                    child.lesson = now;
                    Children.changed(level);
                    reply = ChildLines.lesson(stage, child.skill, seed);
                } else {
                    reply = child.skill >= ChildRules.SKILL_MOST ? ChildLines.lesson(stage, child.skill, seed) : ChildLines.tooSoon(stage, seed);
                }
            }
            case "follow" -> {
                npc.follow(player.m_20148_());
                npc.waiting(false);
                reply = ChildLines.follow(stage, seed);
            }
            case "stay" -> {
                npc.follow(null);
                npc.settle();
                npc.waiting(true); // 1.3.21: waits there, not at home, till told
                reply = ChildLines.stay(stage, seed);
            }
            case "hand" -> {
                ChildBrain.pickUp(npc, player, com.hex.kagune.control.family.ChildRules.RIDE_HAND);
                reply = ChildLines.hand(stage, seed);
            }
            case "feed" -> {
                net.minecraft.world.item.ItemStack held = player.m_21205_();
                boolean ghoul = child.genome().ghoul();
                if (held.m_41619_()) {
                    reply = "*looks at your empty hand*";
                } else if (!ChildBrain.eats(ghoul, held)) {
                    reply = ChildLines.wontEat(stage, ghoul, seed);
                } else {
                    // Taken now; eaten once the talk closes (so it is seen), given back if it can't be.
                    net.minecraft.world.item.ItemStack one = new net.minecraft.world.item.ItemStack(held.m_41720_(), 1);
                    if (!player.m_7500_()) {
                        held.m_41774_(1);
                    }
                    npc.mind.food = one;
                    open(player, npc, stage == ChildStage.BABY ? "*opens its mouth wide*" : "Ooh! For me?", false,
                        com.hex.kagune.control.family.ChildMoment.FEED);
                    return;
                }
            }
            case "bed" -> reply = ChildBrain.bedtime(npc, player, now) ? ChildLines.bedtime(stage, seed) : ChildLines.noBed(stage, seed);
            case "home" -> reply = ChildBrain.goHome(npc, level) ? ChildLines.home(stage, seed) : "Home? Where's home?";
            case "talk" -> {
                // 1.3.29: what can be talked about with it (its scripted topics); none fit, its own word.
                if (topics(player, npc, child, ChildLines.idle(stage, ChildLines.call(stage, ChildBrain.isMother(child, player)), seed))) {
                    return;
                }
                reply = ChildLines.idle(stage, ChildLines.call(stage, ChildBrain.isMother(child, player)), seed);
            }
            case "topics" -> {
                if (topics(player, npc, child, stage == ChildStage.TEEN ? "What else?" : stage == ChildStage.BABY ? "*blinks at you*" : "What else? What else?")) {
                    return;
                }
                reply = "...";
            }
            default -> {
                String key = talk.keys().get(index);
                if (key.startsWith("s:")) {
                    sceneAnswer(player, npc, child, key);
                    return;
                }
                if (key.startsWith("t:")) {
                    com.hex.kagune.control.npc.scene.Scene scene = SceneServer.scene(key.substring(2));
                    if (scene != null) {
                        scene(player, npc, child, scene, scene.first(), SceneServer.context(player, npc, child, now), null);
                        return;
                    }
                }
                if (key.startsWith("m:")) {
                    com.hex.kagune.control.family.ChildMoment moment = com.hex.kagune.control.family.ChildMoment.valueOf(key.substring(2));
                    if (moment.fits(stage)) {
                        // Its eager line now; the moment itself once the talk closes.
                        open(player, npc, moment == com.hex.kagune.control.family.ChildMoment.HUG ? "*holds its arms out wide*"
                            : stage == ChildStage.BABY ? "*wriggles, excited*" : "Yes! Yes yes yes!", false, moment);
                        return;
                    }
                }
                reply = stage == ChildStage.TEEN ? "...Hey." : "*hides shyly*";
            }
        }
        open(player, npc, reply);
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        TALKS.remove(event.getEntity().m_20148_());
    }

    /** 1.3.29: true while {@code player} has a talk with a child open. */
    static boolean talking(ServerPlayer player) {
        return TALKS.containsKey(player.m_20148_());
    }

    /** 1.3.29: a scene's page ({@code line}: what it says; null, the page's own), each answer keyed to it; a topic's has "back". */
    static void scene(ServerPlayer player, ChildNpc npc, Children.Child child, com.hex.kagune.control.npc.scene.Scene scene,
                      com.hex.kagune.control.npc.scene.Scene.Page page, com.hex.kagune.control.npc.scene.SceneContext sc, String line) {
        List<String> keys = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < page.options().size(); i++) {
            add(keys, labels, "s:" + scene.key() + ":" + page.id() + ":" + i, com.hex.kagune.control.npc.scene.SceneBook.fill(page.options().get(i).label(), sc));
        }
        if (scene.kind() == com.hex.kagune.control.npc.scene.Scene.Kind.TOPIC) {
            add(keys, labels, "topics", "...");
        }
        String said = line != null ? line : com.hex.kagune.control.npc.scene.SceneBook.fill(com.hex.kagune.control.npc.scene.SceneBook.one(page.lines(), sc.seed()), sc);
        send(player, npc, child, keys, labels, said, null);
    }

    /** 1.3.29: what can be talked about with it now (its topics, a different few each time), and back. */
    static boolean topics(ServerPlayer player, ChildNpc npc, Children.Child child, String line) {
        long now = player.m_284548_().m_46467_();
        List<com.hex.kagune.control.npc.scene.Scene> all = new ArrayList<>(SceneServer.childTopics(player, npc, child, now));
        if (all.isEmpty()) {
            return false;
        }
        java.util.Collections.shuffle(all, new java.util.Random(npc.m_217043_().m_188503_(100000)));
        com.hex.kagune.control.npc.scene.SceneContext sc = SceneServer.context(player, npc, child, now);
        List<String> keys = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (com.hex.kagune.control.npc.scene.Scene scene : all) {
            if (keys.size() >= 6) {
                break;
            }
            add(keys, labels, "t:" + scene.key(), com.hex.kagune.control.npc.scene.SceneBook.fill(scene.prompt(), sc));
        }
        add(keys, labels, "back", "...");
        send(player, npc, child, keys, labels, line, null);
        return true;
    }

    /** A page sent: its keys and words, then goodbye. */
    private static void send(ServerPlayer player, ChildNpc npc, Children.Child child, List<String> keys, List<String> labels, String line,
                             com.hex.kagune.control.family.ChildMoment queued) {
        ServerLevel level = player.m_284548_();
        ChildStage stage = child.stage(level.m_46467_());
        boolean parent = child.parent(Pregnancies.player(player.m_20148_()));
        TALKS.put(player.m_20148_(), new Talk(npc.m_19879_(), keys, queued));
        int[] choices = new int[keys.size() + 1];
        String[] shown = new String[keys.size() + 1];
        for (int i = 0; i < keys.size(); i++) {
            choices[i] = NpcTalk.Choice.SAYS[i].ordinal();
            shown[i] = labels.get(i);
        }
        choices[keys.size()] = NpcTalk.Choice.LEAVE.ordinal();
        shown[keys.size()] = "";
        ControlNetwork.sendTo(player, new S2CNpcTalk(npc.m_19879_(), child.name, title(child, stage, player), parent ? 100 : 30, parent, -1, line, choices,
            shown));
    }

    /** 1.3.29: an answer on one of its scenes: what it says and does, then the page it leads to, or its own. */
    private static void sceneAnswer(ServerPlayer player, ChildNpc npc, Children.Child child, String key) {
        ServerLevel level = player.m_284548_();
        long now = level.m_46467_();
        SceneServer.ChildAnswer answer = SceneServer.childAnswer(player, npc, child, key, now);
        if (answer == null) {
            open(player, npc, null);
            return;
        }
        ChildStage stage = child.stage(now);
        com.hex.kagune.control.family.ChildMoment queued = null;
        boolean play = false;
        boolean closing = false;
        for (String effect : answer.option().effects()) {
            switch (effect) {
                case "up" -> {
                    if (stage.carried() && npc.mind.carrier == null) {
                        ChildBrain.pickUp(npc, player);
                    }
                }
                case "down" -> {
                    if (player.m_20148_().equals(npc.mind.carrier)) {
                        ChildBrain.putDown(npc, npc.mind);
                    }
                }
                case "shoulders" -> {
                    if (ChildRules.shoulders(stage) && npc.mind.carrier == null) {
                        ChildBrain.pickUp(npc, player, ChildRules.RIDE_SHOULDERS);
                    }
                }
                case "hand" -> {
                    if (ChildRules.hand(stage) && npc.mind.carrier == null) {
                        ChildBrain.pickUp(npc, player, ChildRules.RIDE_HAND);
                    }
                }
                case "follow" -> {
                    if (stage.walks()) {
                        npc.follow(player.m_20148_());
                        npc.waiting(false);
                    }
                }
                case "stay" -> {
                    if (stage.walks()) {
                        npc.follow(null);
                        npc.settle();
                        npc.waiting(true);
                    }
                }
                case "bed" -> ChildBrain.bedtime(npc, player, now);
                case "home" -> ChildBrain.goHome(npc, level);
                case "teach" -> {
                    if (ChildRules.teachable(stage, child.skill, child.lesson, now)) {
                        child.skill++;
                        child.lesson = now;
                        Children.changed(level);
                    }
                }
                case "play" -> play = true;
                case "close" -> closing = true;
                default -> {
                    if (queued == null && !Set.of("sad", "proud").contains(effect) && !effect.startsWith("gift:")) {
                        try {
                            com.hex.kagune.control.family.ChildMoment moment = com.hex.kagune.control.family.ChildMoment.valueOf(effect.toUpperCase(java.util.Locale.ROOT));
                            if (moment.fits(stage)) {
                                queued = moment;
                            }
                        } catch (IllegalArgumentException unknown) {
                            // (the tests keep every word known)
                        }
                    }
                }
            }
        }
        if (answer.next() != null && queued == null && !closing) {
            scene(player, npc, child, answer.scene(), answer.next(), answer.context(), answer.line());
            return;
        }
        if (queued != null) {
            open(player, npc, answer.line(), false, queued);
            return;
        }
        if (closing) {
            send(player, npc, child, new ArrayList<>(), new ArrayList<>(), answer.line(), null);
            return;
        }
        open(player, npc, answer.line(), play, null);
    }
}
