package com.hex.kagune.control.client;

import com.hex.kagune.control.client.animation.Remains;
import com.hex.kagune.control.client.fx.AyatoBlood;
import com.hex.kagune.control.client.fx.RcFx;
import com.hex.kagune.control.client.render.BloodStains;
import com.hex.kagune.control.eto.ReviveRules;
import com.hex.kagune.control.network.S2CEtoMutations;
import com.hex.kagune.control.network.S2CEtoRevive;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 6.2.0, You Thought You'd Get Rid of Me That Easily?, on every client (fed by {@link S2CEtoRevive}): which Eto players are in
 * pieces, since when (on {@link EtoClient#clock}), and how they came apart ({@link Remains#scatter}, worked out once, the same
 * on every client). While she is in pieces her body is not drawn (the pieces are: {@code RemainsRenderer}), nor her hand in first
 * person, and her keys don't move her. The blood is the explosion's alone: three gushes as she comes apart, sprayed off every piece
 * as it flies, pooling where each comes down and round her; then none while she lies there or pulls back together, and the gust as
 * she stands.
 *
 * <p>Also her own mutations, for her menu ({@link S2CEtoMutations}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class ReviveClient {
    /** Played from its first frame if it began at most this long before it got here. */
    static final long JUST_BEGUN = 4L;
    /** Beyond this (blocks from the camera) no blood is thrown. */
    static final double RANGE = 64.0;

    /** One Eto player in pieces: since when (EtoClient's clock), how she came apart, where she stood and faced. */
    public static final class Corpse {
        public final UUID player;
        public final double began;
        public final Remains.Scatter scatter;
        public final Vec3 feet;
        public final float yaw;
        public final long seed;
        double last = -1.0;

        Corpse(UUID player, double began, Remains.Scatter scatter, Vec3 feet, float yaw, long seed) {
            this.player = player;
            this.began = began;
            this.scatter = scatter;
            this.feet = feet;
            this.yaw = yaw;
            this.seed = seed;
        }

        /** Ticks since she came apart, at {@code clock}. */
        public double age(double clock) {
            return clock - this.began;
        }

        /** A rig-space point (x to her right, y up from her feet, z the way she faced) in the world. */
        public Vec3 world(double[] rig) {
            double yaw = Math.toRadians(this.yaw);
            double rx = -Math.cos(yaw);
            double rz = -Math.sin(yaw);
            double fx = -Math.sin(yaw);
            double fz = Math.cos(yaw);
            return new Vec3(this.feet.f_82479_ + rx * rig[0] + fx * rig[2], this.feet.f_82480_ + rig[1], this.feet.f_82481_ + rz * rig[0] + fz * rig[2]);
        }
    }

    private static final Map<UUID, Corpse> CORPSES = new HashMap<>();
    /** Her own mutations (a bitmask of EtoMutation) and when her death that does not take is ready again (game time). */
    private static int mask;
    private static long readyAt;
    private static final Random RANDOM = new Random();

    private ReviveClient() {
    }

    public static void accept(S2CEtoRevive message) {
        if (message.over()) {
            CORPSES.remove(message.playerId());
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        long behind = minecraft.f_91073_ == null ? 0L : minecraft.f_91073_.m_46467_() - message.startedAt();
        double began = EtoClient.clock(0.0F) - (behind > JUST_BEGUN ? behind : 0L);
        Corpse was = CORPSES.get(message.playerId());
        if (was != null && was.seed == message.seed()) {
            return; // told again (someone came into sight of her): it runs on
        }
        AbstractClientPlayer player = player(message.playerId());
        boolean slim = player != null && "slim".equals(player.m_108564_());
        CORPSES.put(message.playerId(), new Corpse(message.playerId(), began, Remains.scatter(message.seed(), message.ground(), slim),
            new Vec3(message.x(), message.y(), message.z()), message.yaw(), message.seed()));
    }

    public static void mutations(S2CEtoMutations message) {
        mask = message.mask();
        readyAt = message.reviveReadyAt();
    }

    /** Her own unlocked mutations (a bitmask of EtoMutation). */
    public static int mask() {
        return mask;
    }

    /** The game tick her death that does not take is ready again. */
    public static long readyAt() {
        return readyAt;
    }

    /** The player {@code id} in this client's world, or null. */
    public static AbstractClientPlayer player(UUID id) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        if (level != null) {
            for (AbstractClientPlayer player : level.m_6907_()) {
                if (player.m_20148_().equals(id)) {
                    return player;
                }
            }
        }
        return null;
    }

    /** {@code player}'s body in pieces, or null. */
    public static Corpse get(Player player) {
        return player == null ? null : CORPSES.get(player.m_20148_());
    }

    /** Every body in pieces now. */
    public static Iterable<Corpse> all() {
        return CORPSES.values();
    }

    /** Whether {@code player} is in pieces this frame ({@code partial} of the way through the tick): her body is not drawn. */
    public static boolean apart(Player player, float partial) {
        Corpse corpse = get(player);
        return corpse != null && ReviveRules.apart(corpse.age(EtoClient.clock(partial)));
    }

    @SubscribeEvent
    public static void body(RenderPlayerEvent.Pre event) {
        if (apart(event.getEntity(), event.getPartialTick())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void hand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91074_ != null && apart(minecraft.f_91074_, minecraft.m_91296_())) {
            event.setCanceled(true); // either hand
        }
    }

    @SubscribeEvent
    public static void movementInput(MovementInputUpdateEvent event) {
        if (event.getEntity() instanceof LocalPlayer self && apart(self, 0.0F)) {
            Input input = event.getInput();
            input.f_108567_ = 0.0F;
            input.f_108566_ = 0.0F;
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (event.phase != TickEvent.Phase.END || minecraft.m_91104_()) {
            return;
        }
        if (level == null) {
            CORPSES.clear();
            return;
        }
        double clock = EtoClient.clock(0.0F);
        Vec3 camera = minecraft.f_91063_ == null ? null : minecraft.f_91063_.m_109153_().m_90583_();
        CORPSES.values().removeIf(corpse -> corpse.age(clock) > ReviveRules.WHOLE + 40.0);
        for (Corpse corpse : CORPSES.values()) {
            double age = corpse.age(clock);
            if (camera != null && corpse.feet.m_82557_(camera) < RANGE * RANGE && minecraft.f_91074_ != null) {
                bleed(level, minecraft.f_91074_, corpse, corpse.last, age);
            }
            corpse.last = age;
            if (age < ReviveRules.WHOLE + 1.0) {
                // 6.2.0: her body faces the way the pieces went back to, so it stands up exactly where they came together.
                AbstractClientPlayer player = player(corpse.player);
                if (player != null) {
                    player.f_20883_ = corpse.yaw;
                    player.f_20884_ = corpse.yaw;
                }
            }
        }
    }

    /**
     * What this tick of her coming apart and back together throws about, from {@code last} ticks in to {@code age}. The blood is the
     * explosion's alone (6.2.0, the user: "just the initial explosion blood and then stop"): the gushes as she bursts, blood flung off
     * every piece as it flies, a pool where each comes down; then nothing while she lies there or pulls back together, only the gust
     * as she stands up whole.
     */
    static void bleed(ClientLevel level, LocalPlayer self, Corpse corpse, double last, double age) {
        Remains.Scatter scatter = corpse.scatter;
        Vec3 middle = corpse.world(new double[] {0.0, 1.05, 0.0});
        // Burst apart (three gushes over three ticks, so it pours out of her rather than popping): blood flung every way from her
        // middle, down onto the ground round her feet and up in a fountain; a red mist; pools spreading out round her.
        for (int beat = 0; beat < 3; beat++) {
            if (crossed(last, age, beat) && age < beat + 4.0) {
                burst(level, self, corpse, middle, beat);
            }
        }
        for (Remains.Piece piece : Remains.Piece.values()) {
            Remains.Track track = scatter.track(piece);
            Vec3 at = corpse.world(Remains.place(scatter, piece, age).at());
            if (age < track.landed()) {
                // Blood thrown off it as it flies, every tick.
                Vec3 was = corpse.world(Remains.place(scatter, piece, Math.max(0.0, age - 1.0)).at());
                AyatoBlood.spurt(level, at, at.m_82546_(was).m_82520_(0.0, 0.05, 0.0), 0.5F, null);
            }
            if (crossed(last, age, track.landed())) {
                // Down: a pool where it lies (and no more blood out of it after).
                pool(level, self, at.m_82520_(0.0, 0.2, 0.0), piece.part == com.hex.kagune.control.wound.Wound.Part.BODY ? 1.1 : 0.7, 80);
            }
        }
        if (crossed(last, age, ReviveRules.WHOLE)) {
            // Whole: the gust bursts out of her.
            gust(corpse);
        }
    }

    /** True if {@code at} (ticks in) falls between the last tick and this one. */
    static boolean crossed(double last, double age, double at) {
        return last < at && age >= at;
    }

    /** One gush of her bursting apart ({@code beat} 0, 1, 2: each a little less). */
    static void burst(ClientLevel level, LocalPlayer self, Corpse corpse, Vec3 middle, int beat) {
        int around = beat == 0 ? 8 : beat == 1 ? 6 : 4;
        float power = beat == 0 ? 2.5F : beat == 1 ? 2.0F : 1.5F;
        for (int i = 0; i < around; i++) {
            double angle = (i + 0.5 * beat) * Math.PI * 2.0 / around + RANDOM.nextDouble() * 0.4;
            AyatoBlood.spurt(level, middle, new Vec3(Math.cos(angle), 0.25 + RANDOM.nextDouble() * 0.6, Math.sin(angle)), power, null);
        }
        // Down onto the ground round her feet, and a fountain up out of her.
        AyatoBlood.spurt(level, middle, new Vec3(RANDOM.nextGaussian() * 0.3, -1.0, RANDOM.nextGaussian() * 0.3), power * 0.8F, null);
        AyatoBlood.spurt(level, middle, new Vec3(RANDOM.nextGaussian() * 0.2, 1.0, RANDOM.nextGaussian() * 0.2), power * 0.7F, null);
        mist(middle, beat == 0 ? 44.0F : 20.0F, 0.3, 1.5);
        if (beat == 0) {
            Remains.Scatter scatter = corpse.scatter;
            pool(level, self, corpse.world(new double[] {0.0, scatter.ground() + 0.1, 0.0}), 1.7, 160);
            for (int i = 0; i < 7; i++) {
                double angle = i * Math.PI * 2.0 / 7.0 + RANDOM.nextDouble() * 0.6;
                double out = 1.0 + RANDOM.nextDouble() * 1.8;
                pool(level, self, corpse.world(new double[] {Math.cos(angle) * out, scatter.ground() + 0.3, Math.sin(angle) * out}), 0.35 + RANDOM.nextDouble() * 0.3, 60);
            }
        }
    }

    /** A red mist out of {@code at}: {@code count} puffs, spread {@code spread} blocks, {@code size} big at the most. */
    static void mist(Vec3 at, float count, double spread, double size) {
        for (int i = RcFx.count(count); i > 0; i--) {
            Vec3 out = new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian() * 0.6 + 0.3, RANDOM.nextGaussian()).m_82490_(0.05 + 0.06 * RANDOM.nextDouble());
            RcFx.at(RcFx.PUFF, false, 18 + RANDOM.nextInt(14), at.m_82520_(RANDOM.nextGaussian() * spread, RANDOM.nextGaussian() * spread * 1.4,
                RANDOM.nextGaussian() * spread)).velocity(out).drag(0.86F).fall(0.004F).colors(0xC8960A16, 0x00500408).size(0.3F, (float)size).spawn();
        }
    }

    /** 6.2.0: the gust bursting out of her as she stands up whole: a ring of air and dust thrown out along the ground round her. */
    static void gust(Corpse corpse) {
        Vec3 feet = corpse.world(new double[] {0.0, corpse.scatter.ground() + 0.15, 0.0});
        for (int i = RcFx.count(36.0F); i > 0; i--) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            Vec3 way = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            RcFx.at(RcFx.PUFF, false, 14 + RANDOM.nextInt(10), feet.m_82549_(way.m_82490_(0.4 + RANDOM.nextDouble() * 0.3)).m_82520_(0.0, RANDOM.nextDouble() * 0.5, 0.0))
                .velocity(way.m_82490_(0.38 + RANDOM.nextDouble() * 0.2).m_82520_(0.0, 0.02, 0.0)).drag(0.84F)
                .colors(0x70E6E2DA, 0x00B8B2A8).size(0.35F, 1.4F).spawn();
        }
        for (int i = RcFx.count(12.0F); i > 0; i--) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            Vec3 way = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            RcFx.at(RcFx.STREAK, false, 8 + RANDOM.nextInt(5), feet.m_82549_(way.m_82490_(0.6)).m_82520_(0.0, 0.3 + RANDOM.nextDouble() * 1.2, 0.0))
                .velocity(way.m_82490_(0.6)).drag(0.82F).colors(0x60FFFFFF, 0x00FFFFFF).size(0.2F, 0.6F).spawn();
        }
        RcFx.at(RcFx.RING, false, 10, feet).colors(0x60F0ECE4, 0x00F0ECE4).size(0.6F, 9.0F).spawn();
    }

    /**
     * 6.2.0: how far (0..1) {@code player} is back in her own body's ways after standing up whole: her model takes over in the pose
     * the pieces went back to (standing, the way she faced as she came apart) and eases into whatever she does from there.
     */
    public static float risen(Player player, double clock) {
        Corpse corpse = get(player);
        if (corpse == null) {
            return 1.0F;
        }
        double since = corpse.age(clock) - ReviveRules.WHOLE;
        if (since >= RISE) {
            return 1.0F;
        }
        double u = Math.max(0.0, since / RISE);
        return (float)(u * u * (3.0 - 2.0 * u));
    }

    /** Ticks her body takes to ease from the pose the pieces went back to into her own. */
    static final double RISE = 10.0;

    /** Blood run onto whatever is under {@code over}, {@code size} wide, spreading over {@code spread} ticks. */
    static void pool(ClientLevel level, LocalPlayer self, Vec3 over, double size, int spread) {
        BlockHitResult hit = level.m_45547_(new ClipContext(over, over.m_82520_(0.0, -3.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
        if (hit.m_6662_() == HitResult.Type.BLOCK && hit.m_82450_().f_82480_ < over.f_82480_ - 1.0E-3) {
            BloodStains.pool(level, hit.m_82450_(), size, 2400, spread);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CORPSES.clear();
        mask = 0;
        readyAt = 0L;
    }
}
