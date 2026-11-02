package com.hex.kagune.control.server;

import com.hex.kagune.control.npc.HumanNpc;
import com.hex.kagune.control.village.VillageTalk;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 1.3.72: two of the Brick Village's humans trading in the street (the owner: "Humans often trade in the streets"): they walk to a
 * street spot between them (the seller on it, the buyer a step off it), the seller with the goods in hand and the buyer with the coin,
 * face each other and say a trade's lines in turn ({@link VillageTalk#trade}), the goods and the coin changing hands on the line for it;
 * then they part, hands empty. Both minds hold it; whichever ticks first each tick runs it. Over at once if either is gone, frightened,
 * or it gets dark, or it has taken too long.
 */
final class VillageTrade {
    /** The longest a trade may take (ticks), and how long its two wait before they may trade again. */
    static final long LONGEST = 1200L;
    static final long AGAIN = 1600L;
    /** How long (ticks) they may take walking up before they talk where they are. */
    static final long GATHER = 300L;
    /** How near (blocks) its spot each must be to talk; how far apart they stand. */
    static final double THERE = 1.3;
    static final double APART = 1.8;

    final int seller;
    final int buyer;
    final double x;
    final double y;
    final double z;
    final double buyerX;
    final double buyerZ;
    final VillageTalk.Goods goods;
    final List<VillageTalk.Line> lines;
    final long began;
    int step;
    long nextAt;
    long lastTick = Long.MIN_VALUE;
    boolean talking;
    boolean over;

    private VillageTrade(HumanNpc seller, HumanNpc buyer, double[] spot, double[] buyerSpot, VillageTalk.Goods goods, long now) {
        this.seller = seller.m_19879_();
        this.buyer = buyer.m_19879_();
        this.x = spot[0];
        this.y = spot[1];
        this.z = spot[2];
        this.buyerX = buyerSpot[0];
        this.buyerZ = buyerSpot[1];
        this.goods = goods;
        this.lines = VillageTalk.trade(goods, seller.m_217043_().m_188503_(1 << 20));
        this.began = now;
    }

    /** {@code seller} and {@code buyer} begin one, on {@code place}'s street spot nearest between them; both minds hold it. */
    static VillageTrade start(HumanNpc seller, HumanNpc buyer, VillagePlace.Place place, long now) {
        double mx = (seller.m_20185_() + buyer.m_20185_()) / 2.0;
        double mz = (seller.m_20189_() + buyer.m_20189_()) / 2.0;
        double[] spot = {mx, seller.m_20186_(), mz};
        double best = 12.0 * 12.0;
        for (int[] street : com.hex.kagune.control.village.VillageMap.STREETS) {
            double[] at = place.world(street);
            double d = (at[0] - mx) * (at[0] - mx) + (at[2] - mz) * (at[2] - mz);
            if (d < best) {
                best = d;
                spot = at;
            }
        }
        double dx = buyer.m_20185_() - spot[0];
        double dz = buyer.m_20189_() - spot[2];
        double d = Math.max(1.0E-3, Math.sqrt(dx * dx + dz * dz));
        double[] buyerSpot = place.clamp(spot[0] + dx / d * APART, spot[2] + dz / d * APART, 1.0);
        VillageTalk.Goods goods = VillageTalk.goods(seller.m_217043_().m_188503_(1000));
        VillageTrade trade = new VillageTrade(seller, buyer, spot, buyerSpot, goods, now);
        hold(seller, goods.item());
        hold(buyer, VillageTalk.COIN);
        seller.mind.trade = trade;
        buyer.mind.trade = trade;
        return trade;
    }

    /** Runs it a tick (once a tick, whichever of its two asks); true while it goes on. */
    boolean tick(HumanNpc asking, ServerLevel level, long now) {
        if (this.over) {
            return false;
        }
        if (this.lastTick == now) {
            return true;
        }
        this.lastTick = now;
        HumanNpc s = level.m_6815_(this.seller) instanceof HumanNpc h && h.m_6084_() && h.mind != null && h.mind.trade == this ? h : null;
        HumanNpc b = level.m_6815_(this.buyer) instanceof HumanNpc h && h.m_6084_() && h.mind != null && h.mind.trade == this ? h : null;
        if (s == null || b == null || now - this.began > LONGEST || com.hex.kagune.control.npc.NpcRules.night(level.m_46468_())
            || now < s.mind.shakenUntil || now < b.mind.shakenUntil) {
            this.end(level, now);
            return false;
        }
        if (!this.talking) {
            boolean sThere = near(s, this.x, this.z);
            boolean bThere = near(b, this.buyerX, this.buyerZ);
            if (sThere && bThere || now - this.began > GATHER) {
                this.talking = true;
                this.nextAt = now + 8L;
            } else {
                if (!sThere && (now % 20L == 0L || s.m_21573_().m_26571_())) {
                    s.m_21573_().m_26519_(this.x, this.y, this.z, com.hex.kagune.control.village.VillageRules.STROLL);
                }
                if (!bThere && (now % 20L == 0L || b.m_21573_().m_26571_())) {
                    b.m_21573_().m_26519_(this.buyerX, this.y, this.buyerZ, com.hex.kagune.control.village.VillageRules.STROLL);
                }
                return true;
            }
        }
        // Face to face, still.
        for (HumanNpc[] pair : new HumanNpc[][] {{s, b}, {b, s}}) {
            pair[0].m_21573_().m_26573_();
            pair[0].m_21563_().m_24960_(pair[1], 30.0F, 30.0F);
            HumanBrain.face(pair[0], pair[1]);
        }
        if (now >= this.nextAt) {
            if (this.step >= this.lines.size()) {
                this.end(level, now);
                return false;
            }
            VillageTalk.Line line = this.lines.get(this.step++);
            HumanNpc speaker = line.seller() ? s : b;
            HumanBrain.say(speaker, level, line.text());
            if (line.hands()) {
                hold(s, VillageTalk.COIN);
                hold(b, this.goods.item());
            }
            this.nextAt = now + Math.max(40L, Math.min(120L, 30L + (long)(line.text().length() * 1.4)));
        }
        return true;
    }

    /** Over: both let go of it, their hands empty, and wait a while before another. */
    void end(ServerLevel level, long now) {
        this.over = true;
        for (int id : new int[] {this.seller, this.buyer}) {
            if (level.m_6815_(id) instanceof HumanNpc h && h.mind != null && h.mind.trade == this) {
                h.mind.trade = null;
                h.mind.tradeReady = now + AGAIN + h.m_217043_().m_188503_(600);
                h.m_21008_(InteractionHand.MAIN_HAND, ItemStack.f_41583_);
            }
        }
    }

    private static boolean near(HumanNpc human, double x, double z) {
        double dx = x - human.m_20185_();
        double dz = z - human.m_20189_();
        return dx * dx + dz * dz <= THERE * THERE;
    }

    /** {@code human} with one of {@code id} (an item id) in its hand. */
    private static void hold(HumanNpc human, String id) {
        int colon = id.indexOf(':');
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.m_214293_(id.substring(0, colon), id.substring(colon + 1)));
        human.m_21008_(InteractionHand.MAIN_HAND, item == null ? ItemStack.f_41583_ : new ItemStack(item, 1));
    }
}
