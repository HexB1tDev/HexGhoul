package com.hex.kagune.control.server;

import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.HumanNpc;
import com.hex.kagune.control.quest.QuestLog;
import com.hex.kagune.control.village.HumanApproach;
import com.hex.kagune.control.village.HumanApproach.Why;
import com.hex.kagune.control.village.HumanGestures;
import com.hex.kagune.control.village.HumanPersona;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 2.2.6: a human coming over by itself to a player it knows ({@link HumanApproach}): now and then (by its temper and how warm it is to
 * them; never one it doesn't know, nor one who hurt it, nor while another has just come over to them), it notices them within
 * {@link HumanApproach#NOTICE} blocks, walks up and says what it came to say: hello, news, a neighbour's gossip, a gift of its trade's
 * (once a day), worry when they're hurt; a partner also that it missed them, a kiss or a hug (its usual walking up for one), or its hand
 * for a walk. By day only for a village's people (by night they're home). Talked to on the way, it lets it go.
 */
public final class HumanApproachServer {
    private HumanApproachServer() {
    }

    /** When each villager may next come over to each player (player + villager), when a player may next be come over to at all. */
    private static final Map<String, Long> DUE = WorldTimes.map();
    private static final Map<UUID, Long> CROWDED = WorldTimes.map();
    /** The day each villager last gave each player a gift, and last came over to them. */
    private static final Map<String, Long> GIFTS = WorldTimes.map();
    private static final Map<String, Long> LAST = WorldTimes.map();

    /** Its tick: true while it is coming over (nothing else of its day this tick). */
    static boolean tick(HumanNpc human, HumanBrain.Mind mind, VillagePlace.Place place, ServerLevel level, long now) {
        if (mind.comeTo != null) {
            return come(human, mind, place, level, now);
        }
        if ((now + human.m_19879_()) % 40L != 0L || now < mind.shakenUntil || mind.trade != null || mind.holding != null || mind.pending != 0
            || mind.gesture != 0 || place != null && com.hex.kagune.control.npc.NpcRules.night(level.m_46468_())) {
            return false;
        }
        look(human, mind, place, level, now);
        return mind.comeTo != null || mind.pending != 0;
    }

    /** Whether one of the players near is due a visit from it; if so it sets off. */
    static void look(HumanNpc human, HumanBrain.Mind mind, VillagePlace.Place place, ServerLevel level, long now) {
        if (DUE.size() > 8192) {
            DUE.clear();
            LAST.clear();
            GIFTS.clear();
        }
        String key = human.m_20148_().toString();
        long day = Math.floorDiv(level.m_46468_(), 24000L);
        HumanPersona.Temper temper = HumanBrain.temper(human);
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_5833_() || !player.m_6084_() || player.m_20280_(human) > HumanApproach.NOTICE * HumanApproach.NOTICE
                || HumanTalk.talking(player) || CROWDED.getOrDefault(player.m_20148_(), Long.MIN_VALUE) > now
                || Engaged.busy(player, human.m_19879_(), now) // 3.2.2: someone else (of any kind) has them: one at a time
                || player.m_20148_().equals(mind.hurtBy) || place != null && !place.inside(player.m_20185_(), player.m_20189_())
                || !human.m_142582_(player)) {
                continue;
            }
            QuestLog.Bond bond = QuestServer.bond(player, key);
            boolean partner = bond.has(HumanPersona.PARTNER);
            long every = HumanApproach.every(temper, bond.regard(), partner);
            if (every <= 0L) {
                continue;
            }
            String pair = player.m_20148_() + key;
            Long due = DUE.get(pair);
            if (due == null) {
                // First seen: not straight away; some while into its usual wait.
                DUE.put(pair, now + (long)(every * (0.3 + 0.7 * human.m_217043_().m_188500_())));
                continue;
            }
            if (now < due) {
                continue;
            }
            DUE.put(pair, now + (long)(every * (0.8 + 0.4 * human.m_217043_().m_188500_())));
            CROWDED.put(player.m_20148_(), now + HumanApproach.CROWD);
            boolean hurt = player.m_21223_() < player.m_21233_() * 0.5F;
            Long last = LAST.get(pair);
            long seen = last != null ? last : bond.chatted();
            boolean missed = partner && seen > 0L && day - seen >= 2L;
            boolean gifted = GIFTS.getOrDefault(pair, Long.MIN_VALUE) == day;
            int roll = human.m_217043_().m_188503_(1 << 16);
            Why why = HumanApproach.pick(temper, bond.regard(), partner, hurt, missed, gifted, roll);
            LAST.put(pair, day);
            start(human, mind, player, temper, why, roll, level, now);
            return;
        }
    }

    /** It sets off toward {@code player} for {@code why} (a kiss or a hug by its usual walking up for one, calling out as it comes). */
    static void start(HumanNpc human, HumanBrain.Mind mind, ServerPlayer player, HumanPersona.Temper temper, Why why, int roll, ServerLevel level,
                      long now) {
        mind.with = player.m_20148_();
        if (why == Why.KISS || why == Why.HUG) {
            boolean kiss = why == Why.KISS;
            mind.pending = HumanGestures.of(kiss ? EtoGesture.KISS : EtoGesture.HUG);
            mind.pendingThen = kiss ? HumanGestures.of(EtoGesture.KISSED) : 0;
            mind.pendingUntil = now + HumanApproach.TRIES;
            mind.pendingLine = kiss ? com.hex.kagune.control.village.HumanRomance.kissed(temper, roll)
                : com.hex.kagune.control.village.HumanRomance.hugged(temper, roll);
            HumanBrain.say(human, level, HumanApproach.line(temper, why, "", "", roll));
            return;
        }
        mind.comeTo = player.m_20148_();
        mind.comeWhy = why;
        mind.comeUntil = now + HumanApproach.TRIES;
        mind.comeRoll = roll;
    }

    /** On its way over: walking up to them (true), there (it says its piece), or given up (they left, it took too long). */
    static boolean come(HumanNpc human, HumanBrain.Mind mind, VillagePlace.Place place, ServerLevel level, long now) {
        net.minecraft.world.entity.Entity found = level.m_8791_(mind.comeTo);
        if (!(found instanceof ServerPlayer player) || !player.m_6084_() || player.m_5833_() || player.m_20280_(human) > 20.0 * 20.0
            || now > mind.comeUntil || place != null && !place.inside(player.m_20185_(), player.m_20189_()) || HumanTalk.talking(player)
            || now % 5L == 0L && Engaged.taken(player, human.m_19879_(), now)) { // 3.2.2: someone else got to them first
            mind.comeTo = null;
            human.m_21573_().m_26573_();
            return false;
        }
        human.m_21563_().m_24960_(player, 30.0F, 30.0F);
        if (player.m_20280_(human) <= HumanApproach.ARRIVE * HumanApproach.ARRIVE) {
            human.m_21573_().m_26573_();
            HumanBrain.face(human, player);
            Why why = mind.comeWhy;
            mind.comeTo = null;
            arrive(human, mind, player, why, mind.comeRoll, level, now);
            return true;
        }
        if (now % 10L == 0L || human.m_21573_().m_26571_()) {
            double pace = mind.comeWhy == Why.MISSED || mind.comeWhy == Why.WORRIED ? 0.8 : com.hex.kagune.control.npc.HumanRules.WALK;
            human.m_21573_().m_26519_(player.m_20185_(), player.m_20186_(), player.m_20189_(), pace);
        }
        return true;
    }

    /** There: what it came over to say and do. */
    static void arrive(HumanNpc human, HumanBrain.Mind mind, ServerPlayer player, Why why, int roll, ServerLevel level, long now) {
        HumanPersona.Temper temper = HumanBrain.temper(human);
        String key = human.m_20148_().toString();
        boolean partner = QuestServer.bond(player, key).has(HumanPersona.PARTNER);
        String name = HumanBrain.name(human);
        switch (why) {
            case GIFT -> {
                List<com.hex.kagune.control.village.VillageShop.Offer> wares = HumanBrain.life(human).job().wares;
                com.hex.kagune.control.village.VillageShop.Offer ware = wares.isEmpty() ? null : wares.get(Math.floorMod(roll >> 3, wares.size()));
                int count = ware == null ? 0 : HumanApproach.gives(ware.count());
                net.minecraft.world.item.ItemStack stack = ware == null ? net.minecraft.world.item.ItemStack.f_41583_ : HumanTalk.stack(ware.item(), count);
                if (stack.m_41619_()) {
                    hello(human, temper, partner, roll, level, now);
                    return;
                }
                HumanTalk.give(player, stack);
                GIFTS.put(player.m_20148_() + key, Math.floorDiv(level.m_46468_(), 24000L));
                String thing = HumanApproach.thing(ware.name(), count);
                HumanBrain.say(human, level, HumanApproach.line(temper, why, thing, "", roll));
                HumanBrain.gesture(human, HumanGestures.hello(temper, partner, roll), 0, now);
                player.m_5661_(Component.m_237113_("§d" + name + " gave you " + thing + "."), true);
            }
            case NEWS -> {
                HumanBrain.say(human, level, HumanApproach.line(temper, why, "", com.hex.kagune.control.village.HumanVoice.news(roll >> 2), roll));
                HumanBrain.gesture(human, HumanGestures.news(roll), 0, now);
            }
            case GOSSIP -> {
                HumanNpc other = neighbour(human, level, roll);
                String said = other == null ? null : com.hex.kagune.control.village.HumanVoice.gossip(HumanBrain.name(other),
                    HumanBrain.life(other).job().title, roll >> 2);
                HumanBrain.say(human, level, said == null ? HumanApproach.line(temper, Why.NEWS, "", com.hex.kagune.control.village.HumanVoice.news(roll >> 2), roll)
                    : HumanApproach.line(temper, why, "", said, roll));
                HumanBrain.gesture(human, HumanGestures.news(roll), 0, now);
            }
            case WORRIED -> {
                HumanBrain.say(human, level, HumanApproach.line(temper, why, "", "", roll));
                if (partner) {
                    HumanBrain.gesture(human, HumanGestures.of(EtoGesture.HUG), 0, now);
                    HumanBrain.hearts(human, level, 3);
                } else {
                    HumanBrain.gesture(human, HumanGestures.chat(temper, roll), 0, now);
                }
            }
            case MISSED -> {
                HumanBrain.say(human, level, HumanApproach.line(temper, why, "", "", roll));
                HumanBrain.gesture(human, HumanGestures.of(EtoGesture.HUG), 0, now);
                HumanBrain.hearts(human, level, 6);
            }
            case WALK -> {
                HumanBrain.takeHand(human, player, now);
                HumanBrain.say(human, level, HumanPersona.walk(temper, roll));
                HumanBrain.hearts(human, level, 4);
                player.m_5661_(Component.m_237113_("§d❤ " + name + " takes your hand for a walk. §7(tap Shift to let go)"), true);
            }
            default -> hello(human, temper, partner, roll, level, now);
        }
    }

    static void hello(HumanNpc human, HumanPersona.Temper temper, boolean partner, int roll, ServerLevel level, long now) {
        HumanBrain.say(human, level, HumanApproach.line(temper, Why.HELLO, "", "", roll));
        HumanBrain.gesture(human, HumanGestures.hello(temper, partner, roll), 0, now);
    }

    /** Another of the people near it (within 32 blocks) to talk about, or null. */
    static HumanNpc neighbour(HumanNpc human, ServerLevel level, int roll) {
        List<HumanNpc> near = level.m_6443_(HumanNpc.class, human.m_20191_().m_82400_(32.0), o -> o != human && o.m_6084_());
        return near.isEmpty() ? null : near.get(Math.floorMod(roll >> 5, near.size()));
    }
}
