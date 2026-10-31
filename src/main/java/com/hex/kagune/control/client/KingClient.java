package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.king.KingForm;
import com.hex.kagune.control.king.KingMove;
import com.hex.kagune.control.king.KingRules;
import com.hex.kagune.control.network.C2SKingAction;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CKingState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.0.5: the final kakujas on the client, fed only by the server ({@code S2CKingState}): who wears what, since when, and this
 * player's own crown (for the menu and the cards). 1.0.10: K held puts it on and takes it off ({@code UkakuInput}'s K, the server's
 * {@code UkakuForms.rinkakuKey}); the cards ({@link KingChoiceScreen}) come up after the Dragon until it is chosen.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class KingClient {
    /** A final kakuja on a player (or going), as the server last said; 1.0.6: and the move its body is in. */
    /**
     * What a final kakuja is doing on a player. 1.0.14: {@code wasLevel} the M1 blow the one now cut off ({@code wasAt} when that one
     * began; -1: none), for the body to let it go into this one.
     */
    public record View(KingForm form, boolean on, long since, boolean preview, KingMove move, long moveAt, int level, int target, int wasLevel,
                       long wasAt, KingMove was) {
        /** How far grown over him at {@code time} (ticks): 0..1. */
        public double grow(double time) {
            return this.form == KingForm.NONE ? 0.0 : com.hex.kagune.control.king.KingTransform.grow(this.form, this.on, time - this.since); // 1.0.17
        }

        /** The move at {@code time}: the one the server last said, or NONE once its body is done with it. */
        public KingMove moveAt(double time) {
            int ticks = this.move.ticks(this.level);
            return ticks > 0 && time - this.moveAt > ticks ? KingMove.NONE : this.move;
        }
    }

    private static final Map<UUID, View> VIEWS = new HashMap<>();
    /** 1.0.7: each wearer's rinkaku strikes: per rinkaku, {target id, tick, spike (1) or lash (0)}. */
    private static final Map<UUID, long[][]> LASHES = new HashMap<>();
    /** How long a rinkaku stays bent at what it struck (ticks): a lash, a spike. */
    public static final int LASH_TICKS = 9;
    public static final int SPIKE_TICKS = 6;
    /** This player's abilities: when each is ready again (game ticks). */
    private static long[] ready = new long[S2CKingState.ABILITIES];
    /** This player's crown: -1 before the Dragon, else the form chosen (NONE: still to choose). */
    private static int crown = -1;

    private KingClient() {
    }

    public static void accept(S2CKingState message) {
        KingForm form = KingForm.byOrdinal(message.form());
        if (form == KingForm.NONE) {
            VIEWS.remove(message.playerId());
        } else {
            KingMove move = KingMove.byOrdinal(message.move());
            View before = VIEWS.get(message.playerId());
            int wasLevel = -1;
            long wasAt = 0L;
            KingMove was = KingMove.NONE;
            if (before != null) {
                // The same move sent again (a state change elsewhere) keeps what it cut off; a new one cuts off the one before (1.0.16:
                // any move, so Peace's blades are handed over from wherever they were).
                boolean same = before.moveAt() == message.moveAt() && before.move() == move;
                KingMove prior = before.moveAt((double)message.moveAt());
                wasLevel = same ? before.wasLevel() : prior == KingMove.NONE ? -1 : before.level();
                wasAt = same ? before.wasAt() : before.moveAt();
                was = same ? before.was() : prior;
            }
            VIEWS.put(message.playerId(), new View(form, message.on(), message.since(), message.preview(), move, message.moveAt(), message.level(),
                message.target(), wasLevel, wasAt, was == null ? KingMove.NONE : was));
        }
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91074_ != null && minecraft.f_91074_.m_20148_().equals(message.playerId())) {
            // 1.0.10: the crown just woke (or is still waiting at login): the cards come up in a moment.
            if (message.crown() == KingForm.NONE.ordinal() && crown != KingForm.NONE.ordinal()) {
                promptIn = PROMPT_DELAY;
            }
            crown = message.crown();
            ready = message.ready() == null ? new long[S2CKingState.ABILITIES] : message.ready().clone();
        }
    }

    /** Ticks until the final kakuja's cards come up (0: not waiting); given after the Dragon's end, so its camera can finish. */
    private static int promptIn;
    static final int PROMPT_DELAY = 100;

    /** Opens the final kakuja's two cards (the menu does, when it is still to choose). */
    public static void openChoice() {
        if (awake() && chosen() == KingForm.NONE) {
            Minecraft.m_91087_().m_91152_(new KingChoiceScreen());
        }
    }

    /** 1.0.7: a rinkaku of War struck on its own. */
    public static void lash(com.hex.kagune.control.network.S2CKingLash message) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91073_ == null || message.tendril() < 0 || message.tendril() >= 6) {
            return;
        }
        long[][] lashes = LASHES.computeIfAbsent(message.playerId(), id -> new long[6][]);
        lashes[message.tendril()] = new long[] {message.target(), minecraft.f_91073_.m_46467_(), message.spike() ? 1L : 0L};
        if (message.spike()) {
            // 1.0.19: the spike let fly from that rinkaku's point, as the server flies it.
            net.minecraft.world.entity.Entity owner = null;
            for (AbstractClientPlayer p : minecraft.f_91073_.m_6907_()) {
                if (p.m_20148_().equals(message.playerId())) {
                    owner = p;
                }
            }
            net.minecraft.world.entity.Entity prey = minecraft.f_91073_.m_6815_(message.target());
            if (owner != null && prey != null) {
                double[] body = com.hex.kagune.control.king.WarModel.tendrilTip(message.tendril());
                double yaw = Math.toRadians(owner.m_146908_());
                double k = 0.9375 / 16.0;
                net.minecraft.world.phys.Vec3 from = owner.m_20182_().m_82520_(Math.cos(yaw) * body[0] * k + Math.sin(yaw) * body[2] * k, body[1] * k,
                    Math.sin(yaw) * body[0] * k - Math.cos(yaw) * body[2] * k);
                com.hex.kagune.control.client.fx.KingMarks.spike(from, prey.m_20182_().m_82520_(0.0, prey.m_20206_() * 0.5, 0.0), message.target());
            }
        }
    }

    /** {@code player}'s rinkaku strikes now: per rinkaku {target id, tick, spike}, or null. */
    public static long[][] lashes(AbstractClientPlayer player) {
        return player == null ? null : LASHES.get(player.m_20148_());
    }

    /** What {@code player} wears (or is taking off), or null. */
    public static View get(AbstractClientPlayer player) {
        return player == null ? null : VIEWS.get(player.m_20148_());
    }

    /** 1.3.19: what any body wears (Ken in Peace as much as a player), or null. */
    public static View get(net.minecraft.world.entity.Entity body) {
        return body == null ? null : VIEWS.get(body.m_20148_());
    }

    /** 1.3.19: the bodies that are no player seen wearing one lately (drawn this session), by id: what effects find them by. */
    private static final Map<UUID, java.lang.ref.WeakReference<net.minecraft.world.entity.LivingEntity>> BODIES = new HashMap<>();

    /** 1.3.19: {@code body} (no player) drawn now: effects sent for it can find it. */
    public static void seen(net.minecraft.world.entity.LivingEntity body) {
        if (VIEWS.containsKey(body.m_20148_())) {
            BODIES.put(body.m_20148_(), new java.lang.ref.WeakReference<>(body));
        }
    }

    /** 1.3.19: the wearer {@code id} in {@code level}: a player, or a body seen wearing one; null if none. */
    public static net.minecraft.world.entity.LivingEntity find(net.minecraft.client.multiplayer.ClientLevel level, UUID id) {
        for (AbstractClientPlayer p : level.m_6907_()) {
            if (p.m_20148_().equals(id)) {
                return p;
            }
        }
        java.lang.ref.WeakReference<net.minecraft.world.entity.LivingEntity> ref = BODIES.get(id);
        net.minecraft.world.entity.LivingEntity body = ref == null ? null : ref.get();
        return body != null && body.m_9236_() == level && !body.m_213877_() ? body : null;
    }

    /** 1.3.19: every wearer there may be in {@code level}: its players and the bodies seen wearing one. */
    public static java.util.List<net.minecraft.world.entity.LivingEntity> wearers(net.minecraft.client.multiplayer.ClientLevel level) {
        java.util.List<net.minecraft.world.entity.LivingEntity> all = new java.util.ArrayList<>(level.m_6907_());
        BODIES.values().removeIf(ref -> ref.get() == null || ref.get().m_213877_());
        for (java.lang.ref.WeakReference<net.minecraft.world.entity.LivingEntity> ref : BODIES.values()) {
            net.minecraft.world.entity.LivingEntity body = ref.get();
            if (body != null && body.m_9236_() == level) {
                all.add(body);
            }
        }
        return all;
    }

    /** The final kakuja {@code player} shows at {@code time}, or NONE once it is all gone. */
    public static KingForm shown(AbstractClientPlayer player, double time) {
        View view = get(player);
        return view != null && view.grow(time) > 0.0 ? view.form() : KingForm.NONE;
    }

    /** This player's crown: has the Dragon been through them. */
    public static boolean awake() {
        return crown >= 0;
    }

    /** This player's chosen final kakuja, or NONE. */
    public static KingForm chosen() {
        return crown < 0 ? KingForm.NONE : KingForm.byOrdinal(crown);
    }

    /** This player wears {@code form} now. */
    public static boolean wearing(KingForm form) {
        Minecraft minecraft = Minecraft.m_91087_();
        View view = minecraft.f_91074_ == null ? null : get(minecraft.f_91074_);
        return view != null && view.on() && view.form() == form;
    }

    /** When this player's ability {@code slot} is ready again (game ticks). */
    public static long ready(int slot) {
        return slot >= 0 && slot < ready.length ? ready[slot] : 0L;
    }

    /** Chooses {@code form} for good (the menu; the server checks). */
    public static void choose(KingForm form) {
        ControlNetwork.CHANNEL.sendToServer(new C2SKingAction(C2SKingAction.CHOOSE, form.ordinal()));
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91073_ != null && !minecraft.m_91104_()) {
            // 1.0.40: gathering and driving out Piercing Salvation squares Peace's body to his aim: the lance is drawn off his body, so it
            // runs where he looks only while his body faces there too.
            double now = ControlClient.worldTime(0.0F);
            for (net.minecraft.world.entity.player.Player each : minecraft.f_91073_.m_6907_()) {
                if (each instanceof AbstractClientPlayer player) {
                    View view = get(player);
                    if (view != null && view.on() && view.moveAt(now) == KingMove.PIERCE) {
                        player.f_20883_ = player.f_20885_;
                    }
                    // 1.2.2: their runs' footfalls (the user's own pack), on the stride.
                    KingSteps.tick(player, view != null && view.on() ? view.form() : KingForm.NONE);
                }
            }
        }
        if (promptIn > 0 && minecraft.f_91074_ != null && --promptIn == 0) {
            if (minecraft.f_91080_ == null && KaguneClientState.self().unlocked()) {
                openChoice();
            } else if (awake() && chosen() == KingForm.NONE) {
                promptIn = 20; // a screen is open (or the Dragon still has the view): try again soon
            }
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VIEWS.clear();
        BODIES.clear();
        LASHES.clear();
        crown = -1;
        promptIn = 0;
        ready = new long[S2CKingState.ABILITIES];
    }
}
