package com.hex.kagune.control.client;

import com.hex.kagune.control.network.S2CEto;
import com.hex.kagune.control.npc.EtoGesture;
import com.hex.kagune.control.npc.EtoRules;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.0: the client's store of every Eto it can see (fed only by the server, {@code S2CEto}): her gesture, her move, her flip, her eye
 * open for a fight. Times are turned onto the client's own clock ({@link PaleClient#clock}) as they arrive, as {@link GhoulNpcClient}
 * does; her eye's fade is followed here, from whatever it was when it was last told to open or close.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class EtoClient {
    /** Her as the server last said; {@code ...Began} are client clock times. */
    public record View(EtoGesture gesture, long gestureAt, double gestureBegan, EtoRules.Move move, long moveAt, double moveBegan, int flip, long flipAt,
                       double flipBegan, boolean turned, int with, String tag) {
        public double gestureAge(double clock) {
            return Math.max(0.0, clock - this.gestureBegan);
        }

        public double moveAge(double clock) {
            return this.move == EtoRules.Move.NONE ? Double.MAX_VALUE : Math.max(0.0, clock - this.moveBegan);
        }
    }

    /** Her eye: open or closing, from how open, since when (client clock). */
    private static final class Eye {
        boolean on;
        float from;
        double since;
    }

    private static final Map<Integer, View> VIEWS = new HashMap<>();
    private static final Map<Integer, Eye> EYES = new HashMap<>();
    /** 1.3.1: her bust's bounce: how far it lags, how fast, her height and the clock when last carried on. */
    private static final Map<Integer, double[]> JIGGLES = new HashMap<>();

    private EtoClient() {
    }

    public static void accept(S2CEto message) {
        Minecraft minecraft = Minecraft.m_91087_();
        long world = minecraft.f_91073_ == null ? 0L : minecraft.f_91073_.m_46467_();
        double clock = PaleClient.clock(0.0F);
        View before = VIEWS.get(message.id());
        double gestureBegan = began(world, clock, message.gestureAt(), before == null ? Long.MIN_VALUE : before.gestureAt(),
            before == null ? 0.0 : before.gestureBegan());
        double moveBegan = began(world, clock, message.moveAt(), before == null ? Long.MIN_VALUE : before.moveAt(), before == null ? 0.0 : before.moveBegan());
        double flipBegan = began(world, clock, message.flipAt(), before == null ? Long.MIN_VALUE : before.flipAt(), before == null ? 0.0 : before.flipBegan());
        VIEWS.put(message.id(), new View(EtoGesture.byOrdinal(message.gesture()), message.gestureAt(), gestureBegan, EtoRules.Move.byOrdinal(message.move()),
            message.moveAt(), moveBegan, message.flip(), message.flipAt(), flipBegan, message.turned(), message.with(), message.tag()));
    }

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

    public static View get(int id) {
        return VIEWS.get(id);
    }

    /** How open her kakugan is (0..1): at night or in a fight, faded in and out over {@link EtoRules#FADE_TICKS}. */
    public static float eye(int id, View view, long dayTime, double clock) {
        boolean on = EtoRules.night(dayTime) || view != null && view.turned();
        Eye eye = EYES.computeIfAbsent(id, ignored -> {
            Eye fresh = new Eye();
            fresh.on = on;
            fresh.from = on ? 1.0F : 0.0F;
            fresh.since = clock;
            return fresh;
        });
        if (eye.on != on) {
            eye.from = EtoRules.eye(eye.on, eye.from, (float)(clock - eye.since));
            eye.on = on;
            eye.since = clock;
        }
        return EtoRules.eye(eye.on, eye.from, (float)(clock - eye.since));
    }

    /** 1.3.1: how far (model pixels, up) her bust's tip is off its rest this frame, her body at height {@code y} (blocks). */
    public static float jiggle(int id, double y, double clock) {
        double[] state = JIGGLES.get(id);
        if (state == null) {
            JIGGLES.put(id, new double[] {0.0, 0.0, y, clock});
            return 0.0F;
        }
        if (clock > state[3]) {
            EtoRules.jiggle(state, y - state[2], clock - state[3]);
            state[2] = y;
            state[3] = clock;
        }
        return EtoRules.jigglePixels(state[0]);
    }

    /** Her flip this frame, posed as a player's is, or null. */
    public static com.hex.kagune.control.client.animation.RigPose flip(View view, double clock, boolean onGround) {
        if (view == null || view.flip() < 0 || view.flipAt() == Long.MIN_VALUE) {
            return null;
        }
        double age = clock - view.flipBegan();
        if (age < 0.0 || age > com.hex.kagune.control.npc.NpcRules.FLIP_POSED) {
            return null;
        }
        double landed = onGround && age > 4.0 ? age - 4.0 : -1.0;
        return com.hex.kagune.control.client.animation.PaleMotions.flip(com.hex.kagune.control.pale.PaleRules.Flip.byOrdinal(view.flip()), age, landed);
    }

    public static void prune(net.minecraft.client.multiplayer.ClientLevel level) {
        VIEWS.keySet().removeIf(id -> level.m_6815_(id) == null);
        EYES.keySet().removeIf(id -> level.m_6815_(id) == null);
        JIGGLES.keySet().removeIf(id -> level.m_6815_(id) == null);
    }

    /**
     * 1.3.2: a kiss held long enough that she pulls this player in: their view is drawn to her face, a little more each frame, until
     * she lets go (called each frame before the world is drawn).
     */
    public static void pull() {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91073_ == null || minecraft.f_91074_ == null) {
            return;
        }
        int me = minecraft.f_91074_.m_19879_();
        for (Map.Entry<Integer, View> entry : VIEWS.entrySet()) {
            View view = entry.getValue();
            if (view.with() != me || view.gesture() != EtoGesture.PULL) {
                continue;
            }
            net.minecraft.world.entity.Entity her = minecraft.f_91073_.m_6815_(entry.getKey());
            if (her == null) {
                continue;
            }
            double dx = her.m_20185_() - minecraft.f_91074_.m_20185_();
            double dy = her.m_20186_() + her.m_20206_() * 0.86 - (minecraft.f_91074_.m_20186_() + minecraft.f_91074_.m_20206_() * 0.9);
            double dz = her.m_20189_() - minecraft.f_91074_.m_20189_();
            double flat = Math.sqrt(dx * dx + dz * dz);
            if (flat < 1.0E-3) {
                continue;
            }
            float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float)-Math.toDegrees(Math.atan2(dy, flat));
            float turn = ((yaw - minecraft.f_91074_.m_146908_()) % 360.0F + 540.0F) % 360.0F - 180.0F;
            minecraft.f_91074_.m_146922_(minecraft.f_91074_.m_146908_() + turn * 0.12F);
            minecraft.f_91074_.m_146926_(minecraft.f_91074_.m_146909_() + (pitch - minecraft.f_91074_.m_146909_()) * 0.12F);
            return;
        }
    }

    /** 1.3.3: 1 if an Eto holds the hand of the player {@code id} (their own arm reaches out to her), else 0. */
    public static float handHeld(int id) {
        for (View view : VIEWS.values()) {
            if (view.with() == id && view.gesture() == EtoGesture.HAND) {
                return 1.0F;
            }
        }
        return 0.0F;
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VIEWS.clear();
        EYES.clear();
        JIGGLES.clear();
    }
}
