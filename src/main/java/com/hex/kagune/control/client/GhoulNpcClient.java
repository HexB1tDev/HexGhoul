package com.hex.kagune.control.client;

import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.graft.GraftStage;
import com.hex.kagune.control.network.S2CGraftState;
import com.hex.kagune.control.network.S2CGhoulNpc;
import com.hex.kagune.control.npc.NpcIdentity;
import com.hex.kagune.control.npc.NpcRules;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 7.0.2: the client's store of every ghoul NPC it can see (fed only by the server, {@code S2CGhoulNpc}): who it is, its kagune out or
 * not, the lash it is making, its volley. Every time is turned onto the client's own clock ({@link PaleClient#clock}) as it arrives,
 * as {@link GraftClient} does, so a lagging server never throws a move back.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class GhoulNpcClient {
    /** One NPC as the server last said; {@code ...Began} are client clock times, {@code ...At} the server's game ticks. */
    public record View(NpcIdentity identity, boolean out, long outAt, double outBegan, GraftMove move, long moveAt, double moveBegan, double x, double y,
                       double z, int prey, long volleyAt, double volleyBegan, long shotAt, double shotBegan, int shotSide, int flip, long flipAt,
                       double flipBegan) {
        /** Ticks since the lash began (huge with none). */
        public double age(double clock) {
            return this.move == GraftMove.NONE ? Double.MAX_VALUE : Math.max(0.0, clock - this.moveBegan);
        }
    }

    private static final Map<Integer, View> VIEWS = new HashMap<>();

    private GhoulNpcClient() {
    }

    public static void accept(S2CGhoulNpc message) {
        Minecraft minecraft = Minecraft.m_91087_();
        long world = minecraft.f_91073_ == null ? 0L : minecraft.f_91073_.m_46467_();
        double clock = PaleClient.clock(0.0F);
        View before = VIEWS.get(message.id());
        double outBegan = began(world, clock, message.outAt(), before == null ? Long.MIN_VALUE : before.outAt(), before == null ? 0.0 : before.outBegan());
        double moveBegan = began(world, clock, message.moveAt(), before == null ? Long.MIN_VALUE : before.moveAt(),
            before == null ? 0.0 : before.moveBegan());
        double volleyBegan = began(world, clock, message.volleyAt(), before == null ? Long.MIN_VALUE : before.volleyAt(),
            before == null ? 0.0 : before.volleyBegan());
        double shotBegan = began(world, clock, message.shotAt(), before == null ? Long.MIN_VALUE : before.shotAt(), before == null ? 0.0 : before.shotBegan());
        double flipBegan = began(world, clock, message.flipAt(), before == null ? Long.MIN_VALUE : before.flipAt(), before == null ? 0.0 : before.flipBegan());
        VIEWS.put(message.id(), new View(NpcIdentity.unpack(message.identity()), message.out(), message.outAt(), outBegan, GraftMove.byOrdinal(message.move()),
            message.moveAt(), moveBegan, message.x(), message.y(), message.z(), message.prey(), message.volleyAt(), volleyBegan, message.shotAt(),
            shotBegan, message.shotSide(), message.flip(), message.flipAt(), flipBegan));
    }

    /** When something the server began at tick {@code at} began on the client clock: told again, as before; fresh, now; old, as long ago. */
    private static double began(long world, double clock, long at, long was, double wasBegan) {
        if (at == Long.MIN_VALUE) {
            return -1.0E6;
        }
        if (at == was) {
            return wasBegan;
        }
        long behind = world - at;
        return clock - (behind > GraftClient.JUST_BEGUN ? Math.min(behind, 10_000L) : 0L);
    }

    /** The NPC {@code id} as last told (null if never). */
    public static View get(int id) {
        return VIEWS.get(id);
    }

    /** How far its kagune is out (0 in .. 1 out), growing out and drawn back over {@link NpcRules#DEPLOY_TICKS}. */
    public static float deploy(View view, double clock) {
        if (view == null) {
            return 0.0F;
        }
        double t = Math.max(0.0, Math.min(1.0, (clock - view.outBegan()) / NpcRules.DEPLOY_TICKS));
        double eased = t * t * (3.0 - 2.0 * t);
        return (float)(view.out() ? eased : 1.0 - eased);
    }

    /** Its lash as the Graft renderer reads a player's: the transplant's stage, the move, its point, its prey. */
    public static GraftClient.View graft(View view) {
        int[] targets = new int[S2CGraftState.TARGETS];
        targets[0] = view.prey();
        return new GraftClient.View(GraftStage.TRANSPLANT, false, Long.MIN_VALUE, -1.0E6, view.move(), view.moveAt(), view.moveBegan(), 0.0F, view.x(),
            view.y(), view.z(), targets, 0, 0);
    }

    /** How far its wings are curled forward to aim (0..1): up over 4 ticks as the volley begins, down over 10 after its last shot. */
    public static float aim(View view, double clock) {
        if (view == null) {
            return 0.0F;
        }
        if (view.volleyAt() != Long.MIN_VALUE) {
            return (float)Math.max(0.0, Math.min(1.0, (clock - view.volleyBegan()) / 4.0));
        }
        double since = clock - Math.max(view.shotBegan(), view.volleyBegan());
        return (float)Math.max(0.0, Math.min(1.0, 1.0 - (since - 4.0) / 10.0));
    }

    /** A wing's kick as its shot leaves ({@code side} 1 right, -1 left): a jolt that dies away. */
    public static float kick(View view, int side, double clock) {
        if (view == null || view.shotAt() == Long.MIN_VALUE || view.shotSide() != side) {
            return 0.0F;
        }
        double since = clock - view.shotBegan();
        return since < 0.0 ? 0.0F : (float)Math.exp(-since * 0.55);
    }

    /**
     * 1.0.41: its flip this frame as a player's is posed ({@code PaleMotions.flip}: the takeoff, the tuck, the whole body over once, the
     * landing), or null when it is not flipping; {@code onGround} whether it has touched down.
     */
    public static com.hex.kagune.control.client.animation.RigPose flip(View view, double clock, boolean onGround) {
        if (view == null || view.flip() < 0 || view.flipAt() == Long.MIN_VALUE) {
            return null;
        }
        double age = clock - view.flipBegan();
        if (age < 0.0 || age > NpcRules.FLIP_POSED) {
            return null;
        }
        double landed = onGround && age > 4.0 ? age - 4.0 : -1.0;
        return com.hex.kagune.control.client.animation.PaleMotions.flip(com.hex.kagune.control.pale.PaleRules.Flip.byOrdinal(view.flip()), age, landed);
    }

    /** Forgets the NPCs no longer in {@code level} (dead, gone, out of range). */
    public static void prune(net.minecraft.client.multiplayer.ClientLevel level) {
        VIEWS.keySet().removeIf(id -> level.m_6815_(id) == null);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VIEWS.clear();
    }
}
