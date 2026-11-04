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
    /** 1.3.77: how close a villager comes up to its partner for a kiss or a hug, and how long it tries to. */
    static final double CLOSE = 1.3;
    static final long COMING = 100L;
    /** What each of the page's reply slots asks. */
    static final String[] ASKS = {"How are you?", "Tell me about yourself.", "Any news?", "You look lovely today."};

    /** 1.3.74: what the romance's reply slots say (SAY_5..SAY_8). */
    static final String[] ROMANCE = {"There's something I have to tell you... I'm a ghoul.", "Please, hear me out. I'd never hurt you.",
        "Go out with me.", "I love you."};

    /** Who each player is talking to (an entity id) and what they were offered; the day they last paid each human a compliment. */
    private record Talk(int npc, List<NpcTalk.Choice> offered, List<com.hex.kagune.control.village.VillageShop.Offer> deals,
                        com.hex.kagune.control.village.VillageShop.Offer selling, List<com.hex.kagune.control.village.HumanVoice.Topic> topics,
                        List<com.hex.kagune.control.village.HumanRomance.Partner> ours) {
        Talk(int npc, List<NpcTalk.Choice> offered) {
            this(npc, offered, null, null, null, null);
        }

        Talk(int npc, List<NpcTalk.Choice> offered, List<com.hex.kagune.control.village.VillageShop.Offer> deals,
             com.hex.kagune.control.village.VillageShop.Offer selling) {
            this(npc, offered, deals, selling, null, null);
        }
    }

    /** 2.1.4: the day each player last flirted with each human (player + human). */
    private static final Map<String, Long> FLIRTED = new HashMap<>();
    /** 2.1.4: a partner's talk on the page (at most this many of them a time, the rest of the page its life's). */
    static final int OURS = 3;

    /** 2.1.3: the day each player last gave each human a gift (player + human). */
    private static final Map<String, Long> GIFTED = new HashMap<>();

    private static final Map<UUID, Talk> TALKS = new HashMap<>();
    private static final Map<String, Long> PRAISED = new HashMap<>();
    /** 1.3.74: a confession it doubts (player and human: waiting for the plea), and the day one was turned down (no more till tomorrow). */
    private static final java.util.Set<String> DOUBTED = new java.util.HashSet<>();
    private static final Map<String, Long> REFUSED = new HashMap<>();
    /** 1.3.78: the day each player last took each villager on a date (player + villager), and how much it warms to them for it. */
    private static final Map<String, Long> DATED = new HashMap<>();
    static final int DATE = 4;

    private HumanTalk() {
    }

    /** 2.2.6: true while {@code player} has a villager's talk open. */
    static boolean talking(ServerPlayer player) {
        return TALKS.containsKey(player.m_20148_());
    }

    /** The player talking to {@code human} now, or null. */
    static ServerPlayer talker(HumanNpc human, ServerLevel level) {
        for (Map.Entry<UUID, Talk> talk : TALKS.entrySet()) {
            if (talk.getValue().npc() != human.m_19879_()) {
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

    /** 2.0.35: who {@code human} falls for (by its own seed and whether it is a woman: its look). */
    static HumanPersona.Attraction attraction(HumanNpc human) {
        return HumanPersona.attraction(seed(human), com.hex.kagune.control.npc.HumanRules.female(human.look()));
    }

    /** 2.0.35: {@code player} a woman (true), a man (false), or never said (null). */
    static Boolean female(ServerPlayer player) {
        com.hex.kagune.control.Gender gender = com.hex.kagune.control.Genders.of(player.m_20148_());
        return gender == com.hex.kagune.control.Gender.UNSET ? null : gender.female();
    }

    /** 2.0.35: the ask that has it say who it falls for and what it looks for. */
    static final NpcTalk.Choice TYPE = NpcTalk.Choice.WHO;

    static void open(ServerPlayer player, HumanNpc human) {
        ServerLevel level = player.m_284548_();
        long now = level.m_46467_();
        if (!human.m_6084_() || human.mind == null) {
            return;
        }
        HumanPersona.Temper temper = HumanBrain.temper(human);
        String name = HumanBrain.name(human);
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
        int roll = human.m_217043_().m_188503_(1000);
        // 2.1.3: now and then caught at its odd habit first.
        show(player, human, com.hex.kagune.control.village.HumanVoice.caught(HumanBrain.life(human), roll)
            + HumanPersona.hello(temper, bond.regard(), bond.has(HumanPersona.PARTNER), roll));
        human.mind.with = player.m_20148_();
        HumanBrain.gesture(human, com.hex.kagune.control.village.HumanGestures.hello(temper, bond.has(HumanPersona.PARTNER), roll), 0, now); // 1.3.77
    }

    /** An answer from the talk screen (routed here by {@code C2SNpcTalk} when the one talked to is a human). */
    public static void heard(ServerPlayer player, int npcId, int ordinal) {
        Talk talking = TALKS.get(player.m_20148_());
        NpcTalk.Choice choice = NpcTalk.Choice.byOrdinal(ordinal);
        if (talking == null || talking.npc() != npcId || choice != NpcTalk.Choice.LEAVE && !talking.offered().contains(choice)) {
            return;
        }
        ServerLevel level = player.m_284548_();
        if (!(level.m_6815_(npcId) instanceof HumanNpc human) || !human.m_6084_() || human.m_20280_(player) > KEEP * KEEP) {
            TALKS.remove(player.m_20148_());
            return;
        }
        long seed = seed(human);
        HumanPersona.Temper temper = HumanBrain.temper(human);
        String key = human.m_20148_().toString();
        QuestLog.Bond bond = QuestServer.bond(player, key);
        int roll = human.m_217043_().m_188503_(1000);
        long day = Math.floorDiv(level.m_46468_(), 24000L);
        if (choice == NpcTalk.Choice.LEAVE) {
            TALKS.remove(player.m_20148_());
            HumanBrain.say(human, level, HumanPersona.bye(temper, bond.regard(), roll));
            HumanBrain.gesture(human, com.hex.kagune.control.village.HumanGestures.bye(temper, roll), 0, level.m_46467_()); // 1.3.77
            return;
        }
        long now = level.m_46467_();
        human.mind.with = player.m_20148_();
        // 2.1.3: "Talk a while.": its topics; on that page, one of them asked (and the page again), or back.
        if (talking.topics() != null) {
            if (choice == NpcTalk.Choice.BACK) {
                show(player, human, com.hex.kagune.control.village.VillageShop.done(roll));
                return;
            }
            int slot = choice.say();
            if (slot >= 0 && slot < talking.topics().size()) {
                topic(player, human, talking.topics().get(slot), roll);
            } else if (talking.ours() != null && slot >= talking.topics().size() && slot < talking.topics().size() + talking.ours().size()) {
                // 2.1.4: its partner's talk.
                com.hex.kagune.control.village.HumanRomance.Partner ours = talking.ours().get(slot - talking.topics().size());
                HumanBrain.gesture(human, com.hex.kagune.control.village.HumanGestures.love(temper, roll), 0, now);
                HumanBrain.hearts(human, level, 3);
                topics(player, human, com.hex.kagune.control.village.HumanRomance.partner(temper, ours, HumanPersona.taste(seed),
                    HumanBrain.life(human).dream(), roll));
            }
            return;
        }
        if (choice == NpcTalk.Choice.TEASE) {
            // 2.1.4: a flirt: how it lands by how warm it is and whether the player is who it falls for; once a day for the warmth.
            boolean lands = HumanPersona.attracted(attraction(human), female(player)) && !bond.has(HumanPersona.NOT_TYPE);
            String both = player.m_20148_() + key;
            if (lands && FLIRTED.getOrDefault(both, Long.MIN_VALUE) != day) {
                FLIRTED.put(both, day);
                QuestServer.bond(player, key, bond.regard(bond.regard() + com.hex.kagune.control.village.HumanRomance.flirted(temper, true)));
            }
            HumanBrain.gesture(human, lands ? com.hex.kagune.control.village.HumanGestures.compliment(temper, roll)
                : com.hex.kagune.control.village.HumanGestures.about(temper, roll), 0, now);
            if (lands && bond.regard() >= HumanPersona.CLOSE) {
                HumanBrain.hearts(human, level, 2);
            }
            show(player, human, com.hex.kagune.control.village.HumanRomance.flirt(temper, bond.regard(), lands, roll));
            return;
        }
        if (choice == NpcTalk.Choice.CHAT) {
            topics(player, human, com.hex.kagune.control.village.HumanVoice.TOPICS_ASKED[Math.floorMod(roll, com.hex.kagune.control.village.HumanVoice.TOPICS_ASKED.length)]);
            return;
        }
        if (choice == NpcTalk.Choice.GIFT) {
            gift(player, human, temper, key, bond, day, roll);
            return;
        }
        // 2.0.13: trading: "Let's trade." opens its wares; on that page, a buy, the sale of what is in their hand, or back.
        if (choice == TRADE && talking.deals() == null) {
            trade(player, human, com.hex.kagune.control.village.VillageShop.hello(roll));
            return;
        }
        if (talking.deals() != null) {
            if (choice == NpcTalk.Choice.BACK) {
                show(player, human, com.hex.kagune.control.village.VillageShop.done(roll));
                return;
            }
            int slot = choice.say();
            if (slot >= 0 && slot < talking.deals().size()) {
                trade(player, human, buy(player, talking.deals().get(slot), roll));
            } else if (choice == SELL && talking.selling() != null) {
                trade(player, human, sell(player, talking.selling(), roll));
            }
            return;
        }
        // 1.3.77: a kiss or a hug: the talk ends so it can be seen; it walks up to them, and does it (a kiss: leaning in, then kissed and
        // hearts), with a line in a bubble.
        // 1.3.78: a date: it takes their hand and walks with them, glued at their side, inside the village; the talk ends so it can be
        // seen. Once a day it warms to them a little for it.
        // 2.3.4: the romance page, back from it, a ring.
        if (choice == NpcTalk.Choice.SWEET) {
            sweet(player, human, HumanPersona.hello(temper, bond.regard(), true, roll));
            return;
        }
        if (choice == NpcTalk.Choice.BACK && talking.topics() == null && talking.deals() == null) {
            show(player, human, HumanPersona.hello(temper, bond.regard(), bond.has(HumanPersona.PARTNER), roll));
            return;
        }
        if (choice == NpcTalk.Choice.PROPOSE) {
            propose(player, human, temper, key, bond, roll, now);
            return;
        }
        if (choice == NpcTalk.Choice.INVITE) {
            TALKS.remove(player.m_20148_());
            HumanBrain.takeHand(human, player, now);
            HumanBrain.say(human, level, HumanPersona.walk(temper, roll));
            HumanBrain.hearts(human, level, 4);
            String both = player.m_20148_() + key;
            if (DATED.getOrDefault(both, Long.MIN_VALUE) != day) {
                DATED.put(both, day);
                QuestServer.bond(player, key, bond.regard(bond.regard() + DATE));
            }
            ControlNetwork.sendTo(player, new S2CNpcTalk(human.m_19879_(), HumanBrain.name(human), temper.title, bond.regard(), false,
                S2CNpcTalk.HUMAN_FACE, "", new int[0], new String[0]));
            return;
        }
        if (choice == NpcTalk.Choice.KISS || choice == NpcTalk.Choice.HUG) {
            TALKS.remove(player.m_20148_());
            boolean kiss = choice == NpcTalk.Choice.KISS;
            human.mind.pending = com.hex.kagune.control.village.HumanGestures.of(kiss ? com.hex.kagune.control.npc.EtoGesture.KISS
                : com.hex.kagune.control.npc.EtoGesture.HUG);
            human.mind.pendingThen = kiss ? com.hex.kagune.control.village.HumanGestures.of(com.hex.kagune.control.npc.EtoGesture.KISSED) : 0;
            human.mind.pendingUntil = now + COMING;
            human.mind.pendingLine = kiss ? com.hex.kagune.control.village.HumanRomance.kissed(temper, roll)
                : com.hex.kagune.control.village.HumanRomance.hugged(temper, roll);
            ControlNetwork.sendTo(player, new S2CNpcTalk(human.m_19879_(), HumanBrain.name(human), temper.title, bond.regard(), false,
                S2CNpcTalk.HUMAN_FACE, "", new int[0], new String[0]));
            return;
        }
        String line;
        boolean turnedDown = false;
        switch (choice) {
            case SAY_1 -> {
                line = com.hex.kagune.control.village.HumanVoice.chat(temper, HumanBrain.life(human), roll); // 2.1.3
                if (bond.chatted() != day) {
                    QuestServer.bond(player, key, bond.regard(bond.regard() + com.hex.kagune.control.npc.NpcBonds.CHAT).chatted(day));
                }
            }
            case SAY_2 -> line = HumanPersona.about(temper, roll);
            case SAY_3 -> line = HumanPersona.news(roll);
            case SAY_5, SAY_6 -> line = confess(player, human, temper, key, bond, choice == NpcTalk.Choice.SAY_6, day, roll);
            case SAY_7 -> {
                HumanPersona.Attraction attraction = attraction(human);
                if (HumanPersona.attracted(attraction, female(player))) {
                    line = HumanPersona.yes(temper, roll);
                    QuestServer.bond(player, key, bond.with(HumanPersona.PARTNER, true));
                    player.m_5661_(Component.m_237113_("\u00a7d" + HumanBrain.name(human) + " and you are together now."), false);
                } else {
                    // 2.0.35: not who it falls for: a kind no, and friends still (no asking again).
                    line = HumanPersona.notMyType(temper, attraction, roll);
                    QuestServer.bond(player, key, bond.with(HumanPersona.NOT_TYPE, true));
                    turnedDown = true;
                }
            }
            case WHO -> line = HumanPersona.type(temper, attraction(human), HumanPersona.taste(seed), roll); // 2.0.35
            case SAY_8 -> line = com.hex.kagune.control.village.HumanRomance.loved(temper, roll); // 2.1.4
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
        // 1.3.77: its body with its words.
        int move = switch (choice) {
            case SAY_1 -> com.hex.kagune.control.village.HumanGestures.chat(temper, roll);
            case SAY_2 -> com.hex.kagune.control.village.HumanGestures.about(temper, roll);
            case SAY_3 -> com.hex.kagune.control.village.HumanGestures.news(roll);
            case SAY_5, SAY_6 -> com.hex.kagune.control.village.HumanGestures.confessed(answered);
            case SAY_7 -> turnedDown ? com.hex.kagune.control.village.HumanGestures.about(temper, roll)
                : com.hex.kagune.control.village.HumanGestures.yes(temper);
            case WHO -> com.hex.kagune.control.village.HumanGestures.about(temper, roll);
            case SAY_8 -> com.hex.kagune.control.village.HumanGestures.love(temper, roll);
            default -> com.hex.kagune.control.village.HumanGestures.compliment(temper, roll);
        };
        HumanBrain.gesture(human, move, 0, now);
        if (choice == NpcTalk.Choice.SAY_7 && !turnedDown || choice == NpcTalk.Choice.SAY_8) {
            HumanBrain.hearts(human, level, choice == NpcTalk.Choice.SAY_7 ? 10 : 5);
        }
    }

    /** 1.3.77: how its last confession was taken (for its gesture; set by {@link #confess}). */
    private static HumanPersona.Answer answered = HumanPersona.Answer.DOUBT;

    /**
     * 1.3.74: a ghoul's confession ({@code plea}: the second try, after a doubt): taken (it knows, and they may go out), doubted (they may
     * plead), or turned down (its regard for them cools a little; no more till tomorrow).
     */
    private static String confess(ServerPlayer player, HumanNpc human, HumanPersona.Temper temper, String key, QuestLog.Bond bond, boolean plea,
                                  long day, int roll) {
        String both = player.m_20148_() + key;
        DOUBTED.remove(both);
        HumanPersona.Answer answer = HumanPersona.confession(temper, bond.regard(), plea, human.m_217043_().m_188500_(),
            HumanPersona.trusts(HumanPersona.taste(seed(human)))); // 2.0.35: what it looks for in someone
        answered = answer;
        switch (answer) {
            case YES -> QuestServer.bond(player, key, bond.with(HumanPersona.CONFESSED, true));
            case DOUBT -> DOUBTED.add(both);
            case NO -> {
                REFUSED.put(both, day);
                QuestServer.bond(player, key, bond.regard(bond.regard() - 8));
            }
        }
        return HumanPersona.confessed(temper, answer, roll);
    }

    /** 2.0.13: the choice that opens its wares on the main page, and the one that sells what is in their hand on the trade page. */
    static final NpcTalk.Choice TRADE = NpcTalk.Choice.FAVOUR;
    static final NpcTalk.Choice SELL = NpcTalk.Choice.SAY_8; // 2.0.14: seven wares on SAY_1..SAY_7

    /**
     * 2.0.13: its wares ({@code VillageShop.stock}, its own for life): each to buy with the emeralds in their hand, the sale of what they
     * hold if it buys that, back to the talk, goodbye.
     */
    private static void trade(ServerPlayer player, HumanNpc human, String line) {
        long seed = seed(human);
        HumanPersona.Temper temper = HumanBrain.temper(human);
        QuestLog.Bond bond = QuestServer.bond(player, human.m_20148_().toString());
        List<com.hex.kagune.control.village.VillageShop.Offer> stock = com.hex.kagune.control.village.VillageShop.stock(seed,
            HumanBrain.life(human).job()); // 2.1.3: by its trade
        net.minecraft.world.item.ItemStack held = player.m_21205_();
        String heldId = held.m_41619_() ? "" : String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(held.m_41720_()));
        List<NpcTalk.Choice> offered = new java.util.ArrayList<>();
        List<String> said = new java.util.ArrayList<>();
        for (int i = 0; i < stock.size() && i < SELL.say(); i++) {
            offered.add(NpcTalk.Choice.SAYS[i]);
            said.add("Buy " + stock.get(i).label());
        }
        com.hex.kagune.control.village.VillageShop.Offer buys = com.hex.kagune.control.village.VillageShop.buys(heldId);
        if (buys != null && com.hex.kagune.control.village.VillageShop.lots(buys, held.m_41613_()) > 0) {
            offered.add(SELL);
            said.add(com.hex.kagune.control.village.VillageShop.sellLabel(buys, held.m_41613_()));
        } else {
            buys = null;
        }
        offered.add(NpcTalk.Choice.BACK);
        said.add("Let's talk instead.");
        offered.add(NpcTalk.Choice.LEAVE);
        said.add("");
        TALKS.put(player.m_20148_(), new Talk(human.m_19879_(), List.copyOf(offered), stock, buys));
        int[] ordinals = new int[offered.size()];
        String[] labels = new String[offered.size()];
        for (int i = 0; i < ordinals.length; i++) {
            ordinals[i] = offered.get(i).ordinal();
            labels[i] = said.get(i);
        }
        int coins = heldId.equals(com.hex.kagune.control.village.VillageShop.EMERALD) ? held.m_41613_() : 0;
        ControlNetwork.sendTo(player, new S2CNpcTalk(human.m_19879_(), HumanBrain.name(human), title(human), bond.regard(), false,
            S2CNpcTalk.HUMAN_FACE, line + " " + com.hex.kagune.control.village.VillageShop.purse(coins), ordinals, labels));
    }

    /** 2.0.13: {@code offer} bought with the emeralds in their hand (a creative player pays nothing), or what it says if they can't. */
    private static String buy(ServerPlayer player, com.hex.kagune.control.village.VillageShop.Offer offer, int roll) {
        net.minecraft.world.item.ItemStack held = player.m_21205_();
        boolean coin = !held.m_41619_() && com.hex.kagune.control.village.VillageShop.EMERALD.equals(
            String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(held.m_41720_())));
        if (!player.m_7500_() && (!coin || held.m_41613_() < offer.price())) {
            return com.hex.kagune.control.village.VillageShop.short_(offer, coin ? held.m_41613_() : 0, roll);
        }
        net.minecraft.world.item.ItemStack goods = stack(offer.item(), offer.count());
        if (goods.m_41619_()) {
            return "...Sorry, I'm out of that today.";
        }
        if (!player.m_7500_()) {
            held.m_41774_(offer.price());
        }
        give(player, goods);
        return com.hex.kagune.control.village.VillageShop.sold(offer, roll);
    }

    /** 2.0.13: what they hold sold to it (as many whole lots as they have), the emeralds into their hands. */
    private static String sell(ServerPlayer player, com.hex.kagune.control.village.VillageShop.Offer offer, int roll) {
        net.minecraft.world.item.ItemStack held = player.m_21205_();
        String heldId = held.m_41619_() ? "" : String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(held.m_41720_()));
        int lots = heldId.equals(offer.item()) ? com.hex.kagune.control.village.VillageShop.lots(offer, held.m_41613_()) : 0;
        if (lots <= 0) {
            return "Hm? You're not holding enough of that anymore.";
        }
        held.m_41774_(lots * offer.count());
        give(player, stack(com.hex.kagune.control.village.VillageShop.EMERALD, lots * offer.price()));
        return com.hex.kagune.control.village.VillageShop.bought(offer, lots, roll);
    }

    /** {@code count} of {@code id} ("minecraft:wheat"), or empty if there is no such item. */
    static net.minecraft.world.item.ItemStack stack(String id, int count) {
        int colon = id.indexOf(':');
        net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
            net.minecraft.resources.ResourceLocation.m_214293_(id.substring(0, colon), id.substring(colon + 1)));
        return item == null ? net.minecraft.world.item.ItemStack.f_41583_ : new net.minecraft.world.item.ItemStack(item, count);
    }

    /** Into their inventory, or dropped at their feet if it is full. */
    static void give(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        if (!player.m_36356_(stack) && !stack.m_41619_()) {
            player.m_36176_(stack, false);
        }
    }

    /** The page: its name and temper, how warm it is to them, what it says, the asks (1.3.74: and the romance's, as they stand) and goodbye. */
    private static void show(ServerPlayer player, HumanNpc human, String line) {
        long seed = seed(human);
        String key = human.m_20148_().toString();
        QuestLog.Bond bond = QuestServer.bond(player, key);
        int regard = bond.regard();
        if (regard >= com.hex.kagune.control.npc.Courtship.HUMAN_CLOSE) {
            CourtshipServer.event(player, com.hex.kagune.control.npc.Courtship.Task.HUMAN_FRIEND, 1); // 2.1.5: a human who cares for them
        }
        String both = player.m_20148_() + key;
        long day = Math.floorDiv(player.m_284548_().m_46468_(), 24000L);
        boolean ghoul = com.hex.kagune.data.KaguneProfile.isUnlocked(player);
        boolean partner = bond.has(HumanPersona.PARTNER);
        boolean close = regard >= HumanPersona.CLOSE;
        // 2.1.3: how it is, a while's talk (its life: TOPICS), a compliment, a gift, its wares.
        List<NpcTalk.Choice> offered = new java.util.ArrayList<>(List.of(NpcTalk.Choice.SAY_1, NpcTalk.Choice.CHAT, NpcTalk.Choice.SAY_4));
        List<String> said = new java.util.ArrayList<>(List.of(ASKS[0], "Talk a while.", ASKS[3]));
        net.minecraft.world.item.ItemStack held = player.m_21205_();
        if (!held.m_41619_()) {
            offered.add(NpcTalk.Choice.GIFT);
            said.add("This is for you. (the " + thing(held) + " in your hand)");
        }
        offered.add(TRADE); // 2.0.13
        said.add("Let's trade.");
        if (!partner) {
            offered.add(NpcTalk.Choice.TEASE); // 2.1.4
            said.add(regard >= HumanPersona.CLOSE ? "Flirt with " + HumanBrain.name(human) + "." : "Flirt a little.");
        }
        boolean doubted = DOUBTED.contains(both);
        if (doubted) {
            offered.add(NpcTalk.Choice.SAY_6);
            said.add(ROMANCE[1]);
        } else if (ghoul && close && !partner && !bond.has(HumanPersona.CONFESSED) && REFUSED.getOrDefault(both, Long.MIN_VALUE) != day) {
            offered.add(NpcTalk.Choice.SAY_5);
            said.add(ROMANCE[0]);
        }
        if (close && !partner && !doubted && !bond.has(HumanPersona.NOT_TYPE) && (!ghoul || bond.has(HumanPersona.CONFESSED))) {
            offered.add(NpcTalk.Choice.SAY_7);
            said.add(ROMANCE[2]);
        }
        if (partner) {
            // 2.3.4 (the owner: "there's no romance tab like in the other NPCs"): the romance on a page of its own, as a companion's is.
            offered.add(NpcTalk.Choice.SWEET);
            said.add("Be sweet to " + HumanBrain.name(human) + ". (romance)");
        }
        offered.add(NpcTalk.Choice.LEAVE);
        said.add("");
        TALKS.put(player.m_20148_(), new Talk(human.m_19879_(), List.copyOf(offered)));
        int[] ordinals = new int[offered.size()];
        String[] labels = new String[offered.size()];
        for (int i = 0; i < ordinals.length; i++) {
            ordinals[i] = offered.get(i).ordinal();
            labels[i] = said.get(i);
        }
        // 2.0.1: its face as the client draws it (HUMAN_FACE: the talk screen reads its look off the entity).
        ControlNetwork.sendTo(player, new S2CNpcTalk(human.m_19879_(), HumanBrain.name(human), spouse(human, bond) + title(human), regard, false,
            S2CNpcTalk.HUMAN_FACE, line, ordinals, labels));
    }

    /** 2.3.4: "Your wife, " or "Your husband, " before its title, married to them; else nothing. */
    static String spouse(HumanNpc human, QuestLog.Bond bond) {
        return bond.has(HumanPersona.MARRIED) ? (com.hex.kagune.control.npc.HumanRules.female(human.look()) ? "Your wife, " : "Your husband, ") : "";
    }

    /**
     * 2.3.4: the romance page (a partner's): "I love you.", a kiss (as a companion's: Shift tapped or held), a hug, a walk hand in hand,
     * a ring offered (one in hand, not yet married), back.
     */
    private static void sweet(ServerPlayer player, HumanNpc human, String line) {
        String key = human.m_20148_().toString();
        QuestLog.Bond bond = QuestServer.bond(player, key);
        String name = HumanBrain.name(human);
        List<NpcTalk.Choice> offered = new java.util.ArrayList<>();
        List<String> said = new java.util.ArrayList<>();
        offered.add(NpcTalk.Choice.SAY_8);
        said.add(ROMANCE[3]);
        offered.add(NpcTalk.Choice.KISS);
        said.add("Kiss " + name + ".");
        offered.add(NpcTalk.Choice.HUG);
        said.add("Hug " + name + ".");
        if (human.mind == null || human.mind.holding == null) {
            offered.add(NpcTalk.Choice.INVITE);
            said.add("Let's go for a walk, hand in hand.");
        }
        if (!bond.has(HumanPersona.MARRIED) && (com.hex.kagune.control.ControlItems.ring(player.m_21205_())
            || com.hex.kagune.control.ControlItems.ring(player.m_21206_()))) {
            offered.add(NpcTalk.Choice.PROPOSE);
            said.add("Marry me. (offer the ring)");
        }
        offered.add(NpcTalk.Choice.BACK);
        said.add("Back.");
        offered.add(NpcTalk.Choice.LEAVE);
        said.add("");
        TALKS.put(player.m_20148_(), new Talk(human.m_19879_(), List.copyOf(offered)));
        int[] ordinals = new int[offered.size()];
        String[] labels = new String[offered.size()];
        for (int i = 0; i < ordinals.length; i++) {
            ordinals[i] = offered.get(i).ordinal();
            labels[i] = said.get(i);
        }
        ControlNetwork.sendTo(player, new S2CNpcTalk(human.m_19879_(), name, spouse(human, bond) + title(human), bond.regard(), false,
            S2CNpcTalk.HUMAN_FACE, line, ordinals, labels));
    }

    /** 2.3.4: a ring offered to a partner: married (the ring taken), one spouse at a time (no companion already). */
    private static void propose(ServerPlayer player, HumanNpc human, HumanPersona.Temper temper, String key, QuestLog.Bond bond, int roll, long now) {
        ServerLevel level = player.m_284548_();
        if (!bond.has(HumanPersona.PARTNER) || bond.has(HumanPersona.MARRIED)) {
            sweet(player, human, com.hex.kagune.control.village.HumanRomance.loved(temper, roll));
            return;
        }
        if (UniqueNpcs.spouseOf(level, player, null) != null) {
            sweet(player, human, "You're already married. ...To someone else. I'm not doing that.");
            return;
        }
        boolean taken = false;
        for (net.minecraft.world.item.ItemStack held : new net.minecraft.world.item.ItemStack[] {player.m_21205_(), player.m_21206_()}) {
            if (!taken && com.hex.kagune.control.ControlItems.ring(held)) {
                held.m_41774_(1);
                taken = true;
            }
        }
        if (!taken) {
            sweet(player, human, "A ring? Where? ...Don't tease me like that.");
            return;
        }
        QuestServer.bond(player, key, bond.with(HumanPersona.MARRIED, true).regard(Math.max(bond.regard(), com.hex.kagune.control.npc.NpcBonds.MOST)));
        HumanBrain.gesture(human, com.hex.kagune.control.village.HumanGestures.of(com.hex.kagune.control.npc.EtoGesture.RING), 0, now);
        HumanBrain.hearts(human, level, 20);
        if (level.m_7654_() != null) {
            level.m_7654_().m_6846_().m_240416_(Component.m_237113_("\u00a7d\u2764 " + player.m_36316_().getName() + " and " + HumanBrain.name(human)
                + " are married!"), false);
        }
        sweet(player, human, com.hex.kagune.control.village.HumanRomance.proposed(temper, roll));
    }

    /** 2.1.3: the talk screen's line under its name: its temper and its trade ("Shy baker"). */
    static String title(HumanNpc human) {
        return HumanBrain.temper(human).title + " " + HumanBrain.life(human).job().title;
    }

    /** 2.1.3: what {@code stack} is called, plainly ("sweet berries"). */
    static String thing(net.minecraft.world.item.ItemStack stack) {
        net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.m_41720_());
        return id == null ? "thing" : id.m_135815_().replace('_', ' ');
    }

    /** 2.1.3: the "Talk a while." page: what can be asked about its life now ({@code line}: its words). */
    private static void topics(ServerPlayer player, HumanNpc human, String line) {
        String key = human.m_20148_().toString();
        QuestLog.Bond bond = QuestServer.bond(player, key);
        int turn = human.m_217043_().m_188503_(1000);
        List<com.hex.kagune.control.village.HumanVoice.Topic> topics = com.hex.kagune.control.village.HumanVoice.topics(bond.regard(),
            bond.has(HumanPersona.PARTNER), bond.flags(), turn);
        // 2.1.4: its partner's talk too (a few a time), the page's room shared with its life's.
        List<com.hex.kagune.control.village.HumanRomance.Partner> ours = new java.util.ArrayList<>();
        if (bond.has(HumanPersona.PARTNER)) {
            com.hex.kagune.control.village.HumanRomance.Partner[] all = com.hex.kagune.control.village.HumanRomance.Partner.values();
            for (int i = 0; i < OURS; i++) {
                ours.add(all[Math.floorMod(turn + i, all.length)]);
            }
            topics = new java.util.ArrayList<>(topics.subList(0, Math.min(topics.size(), com.hex.kagune.control.village.HumanVoice.ROOM - OURS)));
        }
        List<NpcTalk.Choice> offered = new java.util.ArrayList<>();
        List<String> said = new java.util.ArrayList<>();
        for (int i = 0; i < topics.size(); i++) {
            offered.add(NpcTalk.Choice.SAYS[i]);
            said.add(topics.get(i).label);
        }
        for (int i = 0; i < ours.size(); i++) {
            offered.add(NpcTalk.Choice.SAYS[topics.size() + i]);
            said.add(ours.get(i).label);
        }
        offered.add(NpcTalk.Choice.BACK);
        said.add("Let's talk about something else.");
        offered.add(NpcTalk.Choice.LEAVE);
        said.add("");
        TALKS.put(player.m_20148_(), new Talk(human.m_19879_(), List.copyOf(offered), null, null, List.copyOf(topics), List.copyOf(ours)));
        int[] ordinals = new int[offered.size()];
        String[] labels = new String[offered.size()];
        for (int i = 0; i < ordinals.length; i++) {
            ordinals[i] = offered.get(i).ordinal();
            labels[i] = said.get(i);
        }
        ControlNetwork.sendTo(player, new S2CNpcTalk(human.m_19879_(), HumanBrain.name(human), title(human), bond.regard(), false,
            S2CNpcTalk.HUMAN_FACE, line, ordinals, labels));
    }

    /** 2.1.3: {@code topic} asked: its answer (the first time something personal is learned, it warms to them for it), and the page again. */
    private static void topic(ServerPlayer player, HumanNpc human, com.hex.kagune.control.village.HumanVoice.Topic topic, int roll) {
        ServerLevel level = player.m_284548_();
        String key = human.m_20148_().toString();
        QuestLog.Bond bond = QuestServer.bond(player, key);
        HumanPersona.Temper temper = HumanBrain.temper(human);
        String line;
        if (topic == com.hex.kagune.control.village.HumanVoice.Topic.TYPE) {
            line = HumanPersona.type(temper, attraction(human), HumanPersona.taste(seed(human)), roll); // 2.0.35
        } else {
            String gossip = null;
            if (topic == com.hex.kagune.control.village.HumanVoice.Topic.GOSSIP && human.m_217043_().m_188499_()) {
                HumanNpc other = neighbour(human, level);
                if (other != null) {
                    gossip = com.hex.kagune.control.village.HumanVoice.gossip(HumanBrain.name(other), HumanBrain.life(other).job().title, roll);
                }
            }
            line = com.hex.kagune.control.village.HumanVoice.answer(temper, HumanBrain.life(human), topic, roll, gossip);
        }
        if (topic == com.hex.kagune.control.village.HumanVoice.Topic.SECRET) {
            CourtshipServer.event(player, com.hex.kagune.control.npc.Courtship.Task.HUMAN_SECRET, 1); // 2.1.5
        }
        if (topic.personal() && !bond.has(topic.flag())) {
            QuestServer.bond(player, key, bond.with(topic.flag(), true).regard(bond.regard() + com.hex.kagune.control.village.HumanVoice.LEARNED));
        }
        int move = switch (topic) {
            case GOSSIP -> com.hex.kagune.control.village.HumanGestures.news(roll);
            case WORK, HOBBY, QUIRK -> com.hex.kagune.control.village.HumanGestures.chat(temper, roll);
            default -> com.hex.kagune.control.village.HumanGestures.about(temper, roll);
        };
        HumanBrain.gesture(human, move, 0, level.m_46467_());
        topics(player, human, line);
    }

    /** 2.1.3: another of the village's people near {@code human} (for a word about them), or null. */
    private static HumanNpc neighbour(HumanNpc human, ServerLevel level) {
        List<HumanNpc> near = level.m_6443_(HumanNpc.class, human.m_20191_().m_82400_(48.0), other -> other != human && other.m_6084_());
        return near.isEmpty() ? null : near.get(human.m_217043_().m_188503_(near.size()));
    }

    /**
     * 2.1.3: what is in their hand given to it: how it lands (its favourite wins it over, food and flowers please it, something nasty
     * stings), once a day for the warmth; one of it taken.
     */
    private static void gift(ServerPlayer player, HumanNpc human, HumanPersona.Temper temper, String key, QuestLog.Bond bond, long day, int roll) {
        net.minecraft.world.item.ItemStack held = player.m_21205_();
        if (held.m_41619_()) {
            show(player, human, "Hm? Your hands are empty.");
            return;
        }
        String id = String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(held.m_41720_()));
        com.hex.kagune.control.village.HumanLife.Life life = HumanBrain.life(human);
        com.hex.kagune.control.village.HumanLife.Gift how = com.hex.kagune.control.village.HumanLife.gift(life, id);
        String both = player.m_20148_() + key;
        boolean again = GIFTED.getOrDefault(both, Long.MIN_VALUE) == day;
        String name = how == com.hex.kagune.control.village.HumanLife.Gift.LOVED ? life.lovesName() : thing(held);
        if (how == com.hex.kagune.control.village.HumanLife.Gift.DISLIKED) {
            QuestServer.bond(player, key, bond.regard(bond.regard() + how.regard));
        } else {
            held.m_41774_(1);
            if (!again) {
                GIFTED.put(both, day);
                QuestServer.bond(player, key, bond.regard(bond.regard() + how.regard));
            }
            if (how == com.hex.kagune.control.village.HumanLife.Gift.LOVED && !again) {
                HumanBrain.hearts(human, player.m_284548_(), 6);
            }
        }
        HumanBrain.gesture(human, how == com.hex.kagune.control.village.HumanLife.Gift.DISLIKED ? com.hex.kagune.control.village.HumanGestures.about(temper, roll)
            : com.hex.kagune.control.village.HumanGestures.compliment(temper, roll), 0, player.m_284548_().m_46467_());
        show(player, human, com.hex.kagune.control.village.HumanVoice.gift(temper, how, name, again, roll));
    }
}
