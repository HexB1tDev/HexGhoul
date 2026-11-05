package com.hex.kagune.control.server;

import com.hex.kagune.control.npc.HumanNpc;
import com.hex.kagune.control.village.HumanErrand;
import com.hex.kagune.control.village.HumanPersona;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 2.4.7: villagers' favours ({@link HumanErrand}) on the server: now and then one near a player asks one (a banner and its words), kept with
 * the player (one at a time, lapsing after {@link HumanErrand#LASTS}); monsters they kill count toward a hunt, a neighbour talked to toward a
 * visit; the giver takes what was fetched, or hears it's done, and pays.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class HumanErrandServer {
    private HumanErrandServer() {
    }

    static final String KEY = "HexErrand";
    static final String ASKED = "HexErrandAsked";

    /** {@code player}'s open favour, or null (lapsed ones dropped). */
    static HumanErrand.Open open(ServerPlayer player) {
        HumanErrand.Open open = HumanErrand.load(player.getPersistentData().m_128461_(KEY));
        if (open != null && player.m_9236_().m_46467_() > open.until()) {
            player.getPersistentData().m_128359_(KEY, "");
            return null;
        }
        return open;
    }

    static void keep(ServerPlayer player, HumanErrand.Open open) {
        player.getPersistentData().m_128359_(KEY, open == null ? "" : HumanErrand.save(open.giver(), open.giverName(), open.errand(), open.done(), open.until()));
    }

    /** Now and then (its village's day, called every ten seconds): a favour asked of the player near it, if it's their time. */
    static void maybeAsk(HumanNpc human, ServerLevel level, long now) {
        if (human.m_217043_().m_188500_() >= HumanErrand.CHANCE || human.mind == null || now < human.mind.shakenUntil) {
            return;
        }
        ServerPlayer best = null;
        double nearest = 10.0 * 10.0;
        for (ServerPlayer player : level.m_6907_()) {
            double d = player.m_20280_(human);
            if (d < nearest && player.m_6084_() && !player.m_5833_() && human.m_142582_(player) && !HumanTalk.talking(player)
                && !player.m_20148_().equals(human.mind.hurtBy)) {
                nearest = d;
                best = player;
            }
        }
        if (best == null || open(best) != null || now / 100L - best.getPersistentData().m_128451_(ASKED) < HumanErrand.OFFER_EVERY / 100L) {
            return;
        }
        String neighbour = null;
        for (HumanNpc other : level.m_6443_(HumanNpc.class, human.m_20191_().m_82400_(40.0), o -> o != human && o.m_6084_())) {
            neighbour = HumanBrain.name(other);
            break;
        }
        HumanErrand.Errand errand = HumanErrand.roll(((long)human.m_217043_().m_188503_(1 << 30) << 7) ^ human.m_217043_().m_188503_(1 << 20), neighbour);
        String name = HumanBrain.name(human);
        keep(best, new HumanErrand.Open(human.m_20148_().toString(), name, errand, 0, now + HumanErrand.LASTS));
        best.getPersistentData().m_128405_(ASKED, (int)(now / 100L));
        HumanPersona.Temper temper = HumanBrain.temper(human);
        HumanBrain.face(human, best);
        HumanBrain.say(human, level, HumanErrand.ask(temper, errand, human.m_217043_().m_188503_(1000)));
        HumanBrain.gesture(human, com.hex.kagune.control.village.HumanGestures.hello(temper, false, human.m_217043_().m_188503_(1000)), 0, now);
        com.hex.kagune.control.network.ControlNetwork.sendTo(best, new com.hex.kagune.control.network.S2CNpcAlert(name, com.hex.kagune.control.npc.Romance.Want.TALK.ordinal(),
            errand.summary(name)));
    }

    /** A monster they kill counts toward a hunt they owe someone. */
    @SubscribeEvent
    public static void killed(LivingDeathEvent event) {
        if (!(event.getSource().m_7639_() instanceof ServerPlayer player) || !(event.getEntity() instanceof net.minecraft.world.entity.monster.Enemy)) {
            return;
        }
        HumanErrand.Open open = open(player);
        if (open == null || open.errand().kind() != HumanErrand.Kind.HUNT || open.finished()) {
            return;
        }
        HumanErrand.Open now = new HumanErrand.Open(open.giver(), open.giverName(), open.errand(), open.done() + 1, open.until());
        keep(player, now);
        player.m_5661_(Component.m_237113_(now.finished() ? "§aFavour done: go back to " + open.giverName() + "."
            : "§7Favour for " + open.giverName() + ": " + now.done() + "/" + open.errand().count() + " monsters"), true);
    }

    /** Talking to {@code human}: the neighbour they were sent to see? Then its words for it (and the visit is done); else null. */
    static String visit(ServerPlayer player, HumanNpc human) {
        HumanErrand.Open open = open(player);
        if (open == null || open.errand().kind() != HumanErrand.Kind.VISIT || open.finished() || !HumanBrain.name(human).equals(open.errand().other())
            || human.m_20148_().toString().equals(open.giver())) {
            return null;
        }
        keep(player, new HumanErrand.Open(open.giver(), open.giverName(), open.errand(), 1, open.until()));
        player.m_5661_(Component.m_237113_("§aFavour done: go back to " + open.giverName() + "."), true);
        return HumanErrand.visited(open.giverName(), human.m_217043_().m_188503_(100));
    }

    /** What the talk page offers {@code player} about the favour {@code human} gave them: null if none of theirs. */
    static HumanErrand.Open theirs(ServerPlayer player, HumanNpc human) {
        HumanErrand.Open open = open(player);
        return open != null && human.m_20148_().toString().equals(open.giver()) ? open : null;
    }

    /** True if it can be handed in now (a hunt or visit done; what was asked for held in hand). */
    static boolean ready(ServerPlayer player, HumanErrand.Open open) {
        if (open.errand().kind() != HumanErrand.Kind.FETCH) {
            return open.finished();
        }
        net.minecraft.world.item.ItemStack held = player.m_21205_();
        return !held.m_41619_() && open.errand().item().equals(String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(held.m_41720_())))
            && held.m_41613_() >= open.errand().count();
    }

    /** Handed in: what was fetched taken, paid (emeralds and something of its trade), its regard up; its thanks. */
    static String complete(ServerPlayer player, HumanNpc human, HumanErrand.Open open) {
        if (open.errand().kind() == HumanErrand.Kind.FETCH) {
            player.m_21205_().m_41774_(open.errand().count());
        }
        HumanTalk.give(player, HumanTalk.stack("minecraft:emerald", open.errand().emeralds()));
        java.util.List<com.hex.kagune.control.village.VillageShop.Offer> wares = HumanBrain.life(human).job().wares;
        if (!wares.isEmpty()) {
            com.hex.kagune.control.village.VillageShop.Offer ware = wares.get(human.m_217043_().m_188503_(wares.size()));
            HumanTalk.give(player, HumanTalk.stack(ware.item(), com.hex.kagune.control.village.HumanApproach.gives(ware.count())));
        }
        String key = human.m_20148_().toString();
        com.hex.kagune.control.quest.QuestLog.Bond bond = QuestServer.bond(player, key);
        QuestServer.bond(player, key, bond.regard(bond.regard() + HumanErrand.REGARD));
        keep(player, null);
        HumanBrain.hearts(human, player.m_284548_(), 6);
        return HumanErrand.thanks(HumanBrain.temper(human), human.m_217043_().m_188503_(100));
    }

    /** Given up: dropped, no harm done; its words. */
    static String drop(ServerPlayer player, HumanNpc human) {
        keep(player, null);
        return HumanErrand.dropped(HumanBrain.temper(human), human.m_217043_().m_188503_(100));
    }
}
