package com.hex.kagune.control.client;

import com.hex.kagune.control.bikaku.PotentialRules;
import com.hex.kagune.control.client.animation.Reattach;
import com.hex.kagune.control.network.S2CPotential;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.7.3, Potential Unlock: what this client knows of each player's parts torn off ({@link S2CPotential}), and their timelines run on from
 * there (each held while it waits for energy). A part off is not drawn on him (the owner: "the missing body part must actually disappear
 * from the player's normal model until regeneration is complete"), nor its hand in first person; {@code PotentialRenderer} draws it where
 * it is, its strands and the wound.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class PotentialClient {
    /** One part off as this client has it: thrown the same way on every client ({@link Reattach#thrown}). */
    public record Torn(PotentialRules.Part part, long seed, double[] blow, double x, double y, double z, float yaw, float ground, double age,
                       double at, boolean paused, Reattach.Flight flight) {
        /** Ticks into its timeline at {@code clock} ({@code PaleClient.clock}). */
        public double age(double clock) {
            return Math.min(PotentialRules.WHOLE, this.paused ? this.age : this.age + Math.max(0.0, clock - this.at));
        }
    }

    private static final Map<UUID, List<Torn>> TORN = new HashMap<>();

    private PotentialClient() {
    }

    public static void accept(S2CPotential message) {
        double clock = PaleClient.clock(0.0F);
        if (message.torn().length == 0) {
            TORN.remove(message.playerId());
            return;
        }
        boolean slim = false;
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91073_ != null) {
            for (Player player : minecraft.f_91073_.m_6907_()) {
                if (player.m_20148_().equals(message.playerId()) && player instanceof AbstractClientPlayer client) {
                    slim = "slim".equals(client.m_108564_());
                }
            }
        }
        List<Torn> before = TORN.getOrDefault(message.playerId(), List.of());
        List<Torn> now = new ArrayList<>();
        for (S2CPotential.Torn t : message.torn()) {
            PotentialRules.Part part = PotentialRules.Part.byOrdinal(t.part());
            if (part == null) {
                continue;
            }
            Reattach.Flight flight = null;
            for (Torn old : before) {
                if (old.part() == part && old.seed() == t.seed()) {
                    flight = old.flight(); // the same part, told again: thrown as it was
                }
            }
            double[] blow = {t.blowX(), 0.0, t.blowZ()};
            if (flight == null) {
                flight = Reattach.thrown(part, t.seed(), blow, t.ground(), slim);
            }
            now.add(new Torn(part, t.seed(), blow, t.x(), t.y(), t.z(), t.yaw(), t.ground(), t.age(), clock, t.paused(), flight));
        }
        TORN.put(message.playerId(), now);
    }

    /** {@code player}'s parts off (being pulled back or not yet whole); empty for none. */
    public static List<Torn> of(Player player) {
        return TORN.getOrDefault(player.m_20148_(), List.of());
    }

    /** True while {@code part} is off {@code player} at {@code clock} (not back on him yet). */
    public static boolean missing(Player player, PotentialRules.Part part, double clock) {
        for (Torn torn : of(player)) {
            if (torn.part() == part && !Reattach.onHim(torn.age(clock))) {
                return true;
            }
        }
        return false;
    }

    private static final List<net.minecraft.client.model.geom.ModelPart> HIDDEN = new ArrayList<>();

    /**
     * His model without the parts off him (and their outer layer), put back after he is drawn. Last of all (a drawing cancelled before it
     * never gets here), and anything still hidden from a drawing that never finished is put back first: the model is every player's.
     */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void hide(RenderPlayerEvent.Pre event) {
        restore(null);
        Player player = event.getEntity();
        if (of(player).isEmpty() || !(event.getRenderer().m_7200_() instanceof PlayerModel<?> model)) {
            return;
        }
        double clock = PaleClient.clock(event.getPartialTick());
        for (PotentialRules.Part part : PotentialRules.Part.values()) {
            if (missing(player, part, clock)) {
                switch (part) {
                    case HEAD -> hide(model.f_102808_, model.f_102809_);
                    case RIGHT_ARM -> hide(model.f_102811_, model.f_103375_);
                    case LEFT_ARM -> hide(model.f_102812_, model.f_103374_);
                    case RIGHT_LEG -> hide(model.f_102813_, model.f_103377_);
                    case LEFT_LEG -> hide(model.f_102814_, model.f_103376_);
                }
            }
        }
    }

    private static void hide(net.minecraft.client.model.geom.ModelPart... parts) {
        for (net.minecraft.client.model.geom.ModelPart part : parts) {
            if (part.f_104207_) {
                part.f_104207_ = false;
                HIDDEN.add(part);
            }
        }
    }

    @SubscribeEvent
    public static void restore(RenderPlayerEvent.Post event) {
        for (net.minecraft.client.model.geom.ModelPart part : HIDDEN) {
            part.f_104207_ = true;
        }
        HIDDEN.clear();
    }

    /** In first person, an arm that is off isn't there: nor is its hand (or what it holds). */
    @SubscribeEvent
    public static void hideHands(RenderHandEvent event) {
        LocalPlayer player = Minecraft.m_91087_().f_91074_;
        if (player == null || of(player).isEmpty()) {
            return;
        }
        double clock = PaleClient.clock(event.getPartialTick());
        boolean main = event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND;
        boolean rightHanded = ((Enum<?>)player.m_5737_()).ordinal() == 1; // (HumanoidArm: LEFT 0, RIGHT 1)
        PotentialRules.Part arm = main == rightHanded ? PotentialRules.Part.RIGHT_ARM : PotentialRules.Part.LEFT_ARM;
        if (missing(player, arm, clock)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        TORN.clear();
    }
}
