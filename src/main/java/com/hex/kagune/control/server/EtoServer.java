package com.hex.kagune.control.server;

import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CEtoSurgery;
import com.hex.kagune.control.network.S2CNpcTalk;
import com.hex.kagune.control.network.S2CPathPrompt;
import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.EtoRules;
import com.hex.kagune.control.npc.EtoTalk;
import com.hex.kagune.control.npc.NpcBonds;
import com.hex.kagune.control.npc.NpcTalk;
import com.hex.kagune.control.quest.Quest;
import com.hex.kagune.control.quest.QuestBook;
import com.hex.kagune.control.quest.QuestLog;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.network.KaguneNetwork;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 1.3.0: talking with Eto, on the server, on the ghoul NPCs' talk screen ({@link EtoTalk}). A right click opens it if she is not cross
 * with the player (an enemy of hers, or left hanging); each answer is checked against what they were offered, then carried out: her
 * regard (the player's bond {@link EtoRules#KEY}, a ghoul's or a human's), the book or flower handed over, a lesson taught (its quest
 * opens), her errands begun and counted (books, kills at night, a flower), dating, a kiss or a hug (she walks up), and her surgery
 * ({@link #surgery}). A player who kills her is her enemy from then on.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class EtoServer {
    static final double REACH = 6.0;
    static final double KEEP = 9.0;

    private record Talk(int npc, List<NpcTalk.Choice> offered) {
    }

    private static final Map<UUID, Talk> TALKS = new HashMap<>();

    private EtoServer() {
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof EtoNpc npc)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer player && npc.m_20280_(player) <= REACH * REACH) {
            open(player, npc);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        TALKS.remove(event.getEntity().m_20148_());
    }

    static void open(ServerPlayer player, EtoNpc npc) {
        long now = player.m_9236_().m_46467_();
        if (!npc.m_6084_() || npc.mind == null) {
            return;
        }
        QuestLog.Bond bond = QuestServer.bond(player, EtoRules.KEY);
        if (!EtoTalk.talks(bond.regard()) || npc.mind.target != 0) {
            player.m_5661_(Component.m_237113_(EtoRules.NAME + " is in no mood to talk."), true);
            return;
        }
        if (EtoBrain.ignoring(npc, player, now)) {
            player.m_5661_(Component.m_237113_(EtoRules.NAME + " turns her nose up at you. Hmph."), true);
            return;
        }
        if (!bond.has(NpcBonds.MET)) {
            QuestServer.bond(player, EtoRules.KEY, bond.with(NpcBonds.MET, true));
        }
        EtoBrain.listen(npc, player, now);
        EtoTalk.Context context = context(player, npc);
        show(player, npc, context, EtoTalk.greeting(context), EtoTalk.choices(context));
    }

    /** An answer from the talk screen (routed here by {@code C2SNpcTalk} when the one talked to is her). */
    public static void heard(ServerPlayer player, int npcId, int ordinal) {
        Talk talk = TALKS.get(player.m_20148_());
        NpcTalk.Choice choice = NpcTalk.Choice.byOrdinal(ordinal);
        if (talk == null || talk.npc() != npcId || choice != NpcTalk.Choice.LEAVE && !talk.offered().contains(choice)) {
            return;
        }
        ServerLevel level = player.m_284548_();
        if (!(level.m_6815_(npcId) instanceof EtoNpc npc) || !npc.m_6084_() || npc.m_20280_(player) > KEEP * KEEP || npc.mind == null) {
            TALKS.remove(player.m_20148_());
            return;
        }
        long now = level.m_46467_();
        if (choice == NpcTalk.Choice.LEAVE) {
            TALKS.remove(player.m_20148_());
            EtoBrain.listen(npc, null, now);
            return;
        }
        EtoTalk.Context context = context(player, npc);
        EtoTalk.Reply reply = EtoTalk.answer(context, choice);
        QuestLog.Bond bond = QuestServer.bond(player, EtoRules.KEY);
        bond = bond.regard(bond.regard() + reply.regard());
        long day = day(level);
        if (choice == NpcTalk.Choice.WHO) {
            bond = bond.with(NpcBonds.ASKED, true);
        }
        if (choice == NpcTalk.Choice.CHAT && !context.chattedToday()) {
            bond = bond.chatted(day);
        }
        switch (reply.effect()) {
            case BOOK, FLOWER -> {
                boolean isBook = reply.effect() == EtoTalk.Effect.BOOK;
                bond = take(player, isBook) ? EtoRules.advance(EtoRules.gifted(bond, day), isBook ? EtoRules.Step.BOOKS : EtoRules.Step.FLOWER, 1)
                    : bond.regard(bond.regard() - reply.regard());
            }
            case TEACH -> {
                if (context.next() != null) {
                    bond = bond.with(context.next().flag, true);
                }
            }
            case BEGIN -> bond = EtoRules.begin(bond);
            case DATE -> {
                bond = EtoRules.date(bond);
                EtoBrain.gesture(npc.mind, EtoGesture.GIGGLE, now);
            }
            case KISS -> EtoBrain.walkUp(npc, npc.mind, player, EtoGesture.KISS, now);
            case HUG -> EtoBrain.walkUp(npc, npc.mind, player, EtoGesture.HUG, now);
            case ANGER -> npc.mind.grudges.put(player.m_20148_(), now + 400L);
            default -> {
            }
        }
        QuestServer.bond(player, EtoRules.KEY, bond);
        if (reply.effect() == EtoTalk.Effect.SURGERY) {
            TALKS.remove(player.m_20148_());
            ControlNetwork.sendTo(player, new S2CNpcTalk(npc.m_19879_(), EtoRules.NAME, title(bond), bond.regard(), false, -1, reply.line(),
                new int[] {NpcTalk.Choice.LEAVE.ordinal()}));
            surgery(player);
            return;
        }
        EtoTalk.Context after = context(player, npc);
        List<NpcTalk.Choice> next = switch (reply.effect()) {
            case OFFER -> EtoTalk.offer();
            case CLOSE, ANGER, KISS, HUG -> List.of(NpcTalk.Choice.LEAVE);
            default -> EtoTalk.choices(after);
        };
        if (reply.effect() == EtoTalk.Effect.ANGER || reply.effect() == EtoTalk.Effect.CLOSE) {
            EtoBrain.listen(npc, null, now);
        }
        show(player, npc, after, reply.line(), next);
    }

    /** One book (or flower) from their hands; false if they no longer hold one. */
    private static boolean take(ServerPlayer player, boolean book) {
        for (ItemStack held : new ItemStack[] {player.m_21205_(), player.m_21206_()}) {
            if (book ? book(held) : flower(held)) {
                held.m_41774_(1);
                return true;
            }
        }
        return false;
    }

    private static String id(ItemStack stack) {
        if (stack == null || stack.m_41619_()) {
            return null;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
        return key == null ? null : key.toString();
    }

    private static boolean book(ItemStack stack) {
        return EtoRules.book(id(stack));
    }

    private static boolean flower(ItemStack stack) {
        return EtoRules.flower(id(stack));
    }

    /** What the next page depends on, as it stands now. */
    static EtoTalk.Context context(ServerPlayer player, EtoNpc npc) {
        QuestLog log = QuestServer.log(player);
        QuestLog.Bond bond = log.bond(EtoRules.KEY);
        boolean ghoul = KaguneProfile.isUnlocked(player);
        EtoRules.Lesson next = EtoRules.next(bond);
        boolean beforeDone = true;
        if (next != null && next.before() != null) {
            Quest before = QuestBook.get(next.before().quest);
            beforeDone = before != null && log.state(before, PathServer.path(player), KaguneProfile.mastery(player)) == QuestLog.State.DONE;
        }
        long day = day(player.m_284548_());
        boolean bookHeld = book(player.m_21205_()) || book(player.m_21206_());
        boolean flowerHeld = flower(player.m_21205_()) || flower(player.m_21206_());
        return new EtoTalk.Context(bond.regard(), bond.flags(), EtoRules.count(bond), ghoul, bond.chatted() == day, EtoRules.giftedOn(bond, day), bookHeld,
            flowerHeld, next, beforeDone, KakuganServer.granted(player), npc.m_19879_() + (int)day);
    }

    private static String title(QuestLog.Bond bond) {
        return bond.has(EtoRules.DATING) ? "Your date" : "The Library";
    }

    private static void show(ServerPlayer player, EtoNpc npc, EtoTalk.Context context, String line, List<NpcTalk.Choice> choices) {
        TALKS.put(player.m_20148_(), new Talk(npc.m_19879_(), List.copyOf(choices)));
        int[] ordinals = new int[choices.size()];
        for (int i = 0; i < ordinals.length; i++) {
            ordinals[i] = choices.get(i).ordinal();
        }
        ControlNetwork.sendTo(player, new S2CNpcTalk(npc.m_19879_(), EtoRules.NAME, title(QuestServer.bond(player, EtoRules.KEY)), context.regard(), false,
            -1, line, ordinals));
    }

    /**
     * Her surgery: their screen goes black, they hear it, and she wakes them. From then on they may wear one kakugan, a ghoul's turning
     * one-eyed at once; a human wakes a ghoul (one-eyed, the path to choose), as she is.
     */
    static void surgery(ServerPlayer player) {
        boolean ghoul = KaguneProfile.isUnlocked(player);
        ControlNetwork.sendTo(player, new S2CEtoSurgery(ghoul));
        if (!ghoul) {
            KaguneProfile.setUnlocked(player, true);
            KaguneNetwork.broadcast(player);
            ControlNetwork.sendTo(player, new S2CPathPrompt());
        }
        KakuganServer.grant(player);
    }

    private static long day(ServerLevel level) {
        return Math.floorDiv(level.m_46468_(), 24000L);
    }

    // ---------------------------------------------------------------- deaths: her errand's kills, and hers

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Entity source = event.getSource().m_7639_();
        if (!(source instanceof ServerPlayer killer) || killer == dead) {
            return;
        }
        if (dead instanceof EtoNpc) {
            QuestServer.bond(killer, EtoRules.KEY, EtoRules.killedBy(QuestServer.bond(killer, EtoRules.KEY)));
            killer.m_5661_(Component.m_237113_(EtoRules.NAME + " will remember this."), true);
            return;
        }
        if (dead instanceof Enemy && EtoRules.night(killer.m_9236_().m_46468_())) {
            QuestLog.Bond bond = QuestServer.bond(killer, EtoRules.KEY);
            if (EtoRules.step(bond) == EtoRules.Step.HUNT) {
                QuestLog.Bond after = EtoRules.advance(bond, EtoRules.Step.HUNT, 1);
                QuestServer.bond(killer, EtoRules.KEY, after);
                killer.m_5661_(Component.m_237113_(EtoRules.step(after) == EtoRules.Step.HUNT
                    ? EtoRules.NAME + "'s errand: " + EtoRules.count(after) + " of " + EtoRules.Step.HUNT.need + " killed tonight"
                    : EtoRules.NAME + "'s errand: done. Go and see her."), true);
            }
        }
    }
}
