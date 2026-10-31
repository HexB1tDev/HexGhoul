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
        if (player.m_6047_() && player.m_20148_().equals(npc.mind.carrier)) {
            ChildBrain.putDown(npc, npc.mind);
            Children.Child child = Children.get(player.m_284548_(), npc.childId());
            if (child != null) {
                ChildBrain.say(player, child, ChildLines.down(npc.mind.stage, npc.m_217043_().m_188503_(100)));
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
                add(keys, labels, "talk", "How are you?");
            }
            if (stage.walks()) {
                boolean following = player.m_20148_().equals(npc.following());
                add(keys, labels, following ? "stay" : "follow", following ? "Wait here." : "Follow me.");
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
        if (talk == null || talk.npc() != npcId) {
            return;
        }
        ServerLevel level = player.m_284548_();
        if (choice == NpcTalk.Choice.LEAVE || !(level.m_6815_(npcId) instanceof ChildNpc npc) || npc.mind == null || npc.m_20280_(player) > 8.0 * 8.0) {
            TALKS.remove(player.m_20148_());
            // 1.3.20: a moment chosen begins now the talk is closed.
            if (talk.queued() != null && level.m_6815_(npcId) instanceof ChildNpc chosen && chosen.mind != null && chosen.m_20280_(player) <= 12.0 * 12.0) {
                ChildBrain.start(chosen, talk.queued(), player, level.m_46467_());
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
                reply = ChildLines.follow(stage, seed);
            }
            case "stay" -> {
                npc.follow(null);
                npc.settle();
                reply = ChildLines.stay(stage, seed);
            }
            case "talk" -> reply = ChildLines.idle(stage, ChildLines.call(stage, ChildBrain.isMother(child, player)), seed);
            default -> {
                String key = talk.keys().get(index);
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
}
