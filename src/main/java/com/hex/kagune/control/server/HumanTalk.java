package com.hex.kagune.control.server;

import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CNpcTalk;
import com.hex.kagune.control.npc.HumanNpc;
import com.hex.kagune.control.npc.NpcTalk;
import com.hex.kagune.control.quest.QuestLog;
import com.hex.kagune.control.village.HumanPersona;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.73: talking with a human (the owner: "Make general conversation"): a right click on one opens the talk screen (the ghouls' own),
 * its face on it, its name and temper ({@link HumanPersona}); a frightened one won't talk. The player can ask how it is, about itself,
 * for the news, or pay it a compliment; each is answered in its temper's words, and a chat a day and a compliment a day make it warmer
 * to them (its regard for them, kept in their quest log as every NPC's). While the screen is open it stands still, facing them.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class HumanTalk {
    /** How near a player must be to talk, and to keep talking (blocks). */
    static final double REACH = 6.0;
    static final double KEEP = 9.0;
    /** What each of the page's reply slots asks. */
    static final String[] ASKS = {"How are you?", "Tell me about yourself.", "Any news?", "You look lovely today."};
    private static final List<NpcTalk.Choice> CHOICES = List.of(NpcTalk.Choice.SAY_1, NpcTalk.Choice.SAY_2, NpcTalk.Choice.SAY_3, NpcTalk.Choice.SAY_4,
        NpcTalk.Choice.LEAVE);

    /** Who each player is talking to (an entity id), and the day they last paid each human a compliment. */
    private static final Map<UUID, Integer> TALKS = new HashMap<>();
    private static final Map<String, Long> PRAISED = new HashMap<>();

    private HumanTalk() {
    }

    /** The player talking to {@code human} now, or null. */
    static ServerPlayer talker(HumanNpc human, ServerLevel level) {
        for (Map.Entry<UUID, Integer> talk : TALKS.entrySet()) {
            if (talk.getValue() != human.m_19879_()) {
                continue;
            }
            ServerPlayer player = level.m_7654_().m_6846_().m_11259_(talk.getKey());
            if (player != null && player.m_9236_() == level && player.m_20280_(human) <= KEEP * KEEP) {
                return player;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof HumanNpc human)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer player && human.m_20280_(player) <= REACH * REACH) {
            open(player, human);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        TALKS.remove(event.getEntity().m_20148_());
    }

    static long seed(HumanNpc human) {
        return human.m_20148_().getMostSignificantBits() ^ human.m_20148_().getLeastSignificantBits();
    }

    static void open(ServerPlayer player, HumanNpc human) {
        ServerLevel level = player.m_284548_();
        long now = level.m_46467_();
        if (!human.m_6084_() || human.mind == null) {
            return;
        }
        HumanPersona.Temper temper = HumanPersona.temper(seed(human));
        String name = HumanPersona.name(seed(human));
        if (now < human.mind.shakenUntil) {
            player.m_5661_(Component.m_237113_(name + " is too frightened to talk."), true);
            return;
        }
        if (human.mind.trade != null) {
            human.mind.trade.end(level, now); // a word from a player comes first
        }
        String key = human.m_20148_().toString();
        QuestLog.Bond bond = QuestServer.bond(player, key);
        if (!bond.has(com.hex.kagune.control.npc.NpcBonds.MET)) {
            QuestServer.bond(player, key, bond.with(com.hex.kagune.control.npc.NpcBonds.MET, true));
        }
        show(player, human, HumanPersona.hello(temper, bond.regard(), human.m_217043_().m_188503_(1000)));
    }

    /** An answer from the talk screen (routed here by {@code C2SNpcTalk} when the one talked to is a human). */
    public static void heard(ServerPlayer player, int npcId, int ordinal) {
        Integer talking = TALKS.get(player.m_20148_());
        NpcTalk.Choice choice = NpcTalk.Choice.byOrdinal(ordinal);
        if (talking == null || talking != npcId || !CHOICES.contains(choice)) {
            return;
        }
        ServerLevel level = player.m_284548_();
        if (!(level.m_6815_(npcId) instanceof HumanNpc human) || !human.m_6084_() || human.m_20280_(player) > KEEP * KEEP) {
            TALKS.remove(player.m_20148_());
            return;
        }
        long seed = seed(human);
        HumanPersona.Temper temper = HumanPersona.temper(seed);
        String key = human.m_20148_().toString();
        QuestLog.Bond bond = QuestServer.bond(player, key);
        int roll = human.m_217043_().m_188503_(1000);
        long day = Math.floorDiv(level.m_46468_(), 24000L);
        if (choice == NpcTalk.Choice.LEAVE) {
            TALKS.remove(player.m_20148_());
            HumanBrain.say(human, level, HumanPersona.bye(temper, bond.regard(), roll));
            return;
        }
        String line;
        switch (choice) {
            case SAY_1 -> {
                line = HumanPersona.chat(temper, roll);
                if (bond.chatted() != day) {
                    QuestServer.bond(player, key, bond.regard(bond.regard() + com.hex.kagune.control.npc.NpcBonds.CHAT).chatted(day));
                }
            }
            case SAY_2 -> line = HumanPersona.about(temper, roll);
            case SAY_3 -> line = HumanPersona.news(roll);
            default -> {
                line = HumanPersona.compliment(temper, bond.regard(), roll);
                String praised = player.m_20148_() + key;
                if (PRAISED.getOrDefault(praised, Long.MIN_VALUE) != day) {
                    PRAISED.put(praised, day);
                    QuestServer.bond(player, key, bond.regard(bond.regard() + HumanPersona.liking(temper)));
                }
            }
        }
        show(player, human, line);
    }

    /** The page: its name and temper, how warm it is to them, what it says, the four asks and goodbye. */
    private static void show(ServerPlayer player, HumanNpc human, String line) {
        long seed = seed(human);
        TALKS.put(player.m_20148_(), human.m_19879_());
        int regard = QuestServer.bond(player, human.m_20148_().toString()).regard();
        int[] ordinals = new int[CHOICES.size()];
        String[] labels = new String[CHOICES.size()];
        for (int i = 0; i < ordinals.length; i++) {
            ordinals[i] = CHOICES.get(i).ordinal();
            labels[i] = i < ASKS.length ? ASKS[i] : "";
        }
        // Its skin as a talk screen knows it: -100 less its look (a vanilla default skin: HumanRules.LOOKS).
        ControlNetwork.sendTo(player, new S2CNpcTalk(human.m_19879_(), HumanPersona.name(seed), HumanPersona.temper(seed).title, regard, false,
            -100 - human.look(), line, ordinals, labels));
    }
}
