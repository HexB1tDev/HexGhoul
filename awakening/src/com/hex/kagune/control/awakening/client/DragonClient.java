package com.hex.kagune.control.awakening.client;

import com.hex.kagune.control.awakening.AwakeningStage;
import com.hex.kagune.control.awakening.DragonRig;
import com.hex.kagune.control.awakening.DragonTimeline;
import com.hex.kagune.control.awakening.S2CAwakeningStatus;
import com.hex.kagune.control.awakening.S2CDragonEvent;
import com.hex.kagune.control.awakening.S2CDragonFrame;
import com.hex.kagune.control.awakening.V3;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Client copy of every Dragon in view, the shatter debris, and the screen effects they cause. */
public final class DragonClient {
    static final class Dragon {
        final UUID owner;
        final ArrayDeque<S2CDragonFrame> queue = new ArrayDeque<>();
        S2CDragonFrame previous;
        S2CDragonFrame current;
        S2CDragonFrame lastIntact;
        int staleTicks;
        boolean ended;
        int endedAt;
        V3 endPosition;

        Dragon(UUID owner) {
            this.owner = owner;
        }

        final DragonPose.Gait gait = new DragonPose.Gait();

        DragonPose pose(float partialTick) {
            if (current == null) return null;
            DragonPose pose = new DragonPose(previous == null ? current : previous, current, partialTick);
            gait.apply(pose, partialTick);
            return pose;
        }

        /** The next frame in the queue. */
        void advance() {
            previous = current;
            current = queue.poll();
            gait.advance(previous, current);
        }

        int tick() {
            return current == null ? -1 : current.tick();
        }
    }

    /** A shard of kagune flung out when the Dragon shatters. */
    static final class Fragment {
        double x, y, z;
        double vx, vy, vz;
        final double ax, ay, az;
        double angle;
        final double spin;
        final double size;
        final int color;
        final int life;
        int age;
        boolean resting;

        Fragment(double x, double y, double z, double vx, double vy, double vz, double ax, double ay, double az, double spin, double size, int color, int life) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            double length = Math.sqrt(ax * ax + ay * ay + az * az);
            this.ax = ax / length;
            this.ay = ay / length;
            this.az = az / length;
            this.spin = spin;
            this.size = size;
            this.color = color;
            this.life = life;
        }
    }

    /** The expanding ground shockwave. */
    static final class Ring {
        final double x, y, z;
        final double radius;
        final int life;
        int age;

        Ring(double x, double y, double z, double radius, int life) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.life = life;
        }
    }

    static final Map<UUID, Dragon> DRAGONS = new HashMap<>();
    static final List<Fragment> FRAGMENTS = new ArrayList<>();
    static final List<Ring> RINGS = new ArrayList<>();
    private static final SplittableRandom RANDOM = new SplittableRandom();
    static S2CAwakeningStatus status = new S2CAwakeningStatus(AwakeningStage.DORMANT, false);
    /** 1.0.11: where this player stands while they turn (their chest), or null. */
    static V3 preludeAt;
    static int statusChangedAt = -10_000;
    static int ticks;
    static double shake;
    static double redFlash;
    static double whiteFlash;
    static double fovPunch;
    static double crackGlow;

    private DragonClient() {
    }

    public static void acceptFrame(S2CDragonFrame frame) {
        // 1.0.11: the Dragon is out of him: he has turned.
        com.hex.kagune.control.client.TurningClient.end(frame.owner());
        Dragon dragon = DRAGONS.computeIfAbsent(frame.owner(), Dragon::new);
        if (dragon.ended) return;
        dragon.queue.add(frame);
        for (float radius : frame.radii()) {
            if (radius > 0.05F) {
                dragon.lastIntact = frame;
                break;
            }
        }
    }

    public static void acceptEvent(S2CDragonEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        double dx = camera.x - event.x();
        double dy = camera.y - event.y();
        double dz = camera.z - event.z();
        boolean mine = minecraft.player != null && event.owner().equals(minecraft.player.getUUID());
        double near = Math.max(0.0, 1.0 - Math.sqrt(dx * dx + dy * dy + dz * dz) / 160.0);
        double felt = mine ? Math.max(0.55, near) : near;
        double magnitude = event.magnitude();
        switch (event.kind()) {
            case ERUPTION -> {
                kick(1.1 * felt);
                redFlash = Math.max(redFlash, 0.75 * felt);
                burst(event.x(), event.y() + 2.0, event.z(), 80, 3.0);
            }
            case BITE -> kick(0.35 * felt);
            case LASH -> kick(0.12 * felt);
            case GRAB -> kick(0.08 * felt);
            case SLAM -> {
                kick(0.6 * felt);
                RINGS.add(new Ring(event.x(), event.y() + 0.2, event.z(), 9.0, 14));
            }
            case CRACK -> {
                kick((0.2 + 0.5 * magnitude) * felt);
                crackGlow = Math.max(crackGlow, magnitude);
            }
            case SHATTER -> {
                kick(1.6 * felt);
                whiteFlash = Math.max(whiteFlash, mine ? 1.0 : 0.8 * felt);
                fovPunch = Math.max(fovPunch, 18.0 * felt);
                Dragon dragon = DRAGONS.get(event.owner());
                if (dragon != null) shatter(dragon);
                RINGS.add(new Ring(event.x(), event.y() + 0.3, event.z(), 34.0, 26));
            }
            case EMERGE -> {
            }
            case PRELUDE -> {
                // 1.0.11: he is turning, this far in; the first time, the camera comes for him (if he is this player).
                com.hex.kagune.control.client.TurningClient.sync(event.owner(), (int) magnitude);
                if (mine) preludeAt = new V3(event.x(), event.y(), event.z());
            }
            case BURST -> {
                kick((0.5 + 0.9 * magnitude) * felt);
                redFlash = Math.max(redFlash, (0.25 + 0.5 * magnitude) * felt);
                fovPunch = Math.max(fovPunch, 6.0 * magnitude * felt);
                burst(event.x(), event.y(), event.z(), (int) (20 + 50 * magnitude), 0.6 + 0.8 * magnitude);
                RINGS.add(new Ring(event.x(), event.y() - 1.0, event.z(), 4.0 + 6.0 * magnitude, 12));
            }
            case END -> {
                Dragon dragon = DRAGONS.get(event.owner());
                if (dragon != null) {
                    dragon.ended = true;
                    dragon.endedAt = ticks;
                    dragon.endPosition = new V3(event.x(), event.y(), event.z());
                }
            }
        }
    }

    public static void acceptStatus(S2CAwakeningStatus message) {
        if (!message.equals(status)) statusChangedAt = ticks;
        status = message;
    }

    private static void kick(double amount) {
        shake = Math.min(2.2, Math.max(shake, amount));
    }

    private static void burst(double x, double y, double z, int count, double spread) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        // 1.0.19: real blood and gore (KingMarks), not the game's dust.
        com.hex.kagune.control.client.fx.KingMarks.play(level, com.hex.kagune.control.king.KingMarkFx.GORE, new net.minecraft.world.phys.Vec3(x, y + spread * 0.5, z),
                new net.minecraft.world.phys.Vec3(0.0, 1.0, 0.0), (float) Math.min(3.0, count / 40.0), null);
    }

    /** The colossal body bursts into countless fragments of kagune. */
    private static void shatter(Dragon dragon) {
        S2CDragonFrame frame = dragon.lastIntact != null ? dragon.lastIntact : dragon.current;
        if (frame == null) return;
        float[] s = frame.segments();
        float[] r = frame.radii();
        for (int i = 0; i < DragonRig.SEGMENTS; i++) {
            if (r[i] <= 0.05F) continue;
            double cx = frame.anchorX() + s[i * 3];
            double cy = frame.anchorY() + s[i * 3 + 1];
            double cz = frame.anchorZ() + s[i * 3 + 2];
            int pieces = 6 + (int) (r[i] * 1.4);
            for (int p = 0; p < pieces; p++) {
                double theta = RANDOM.nextDouble() * Math.PI * 2.0;
                double phi = Math.acos(2.0 * RANDOM.nextDouble() - 1.0);
                double nx = Math.sin(phi) * Math.cos(theta);
                double ny = Math.cos(phi);
                double nz = Math.sin(phi) * Math.sin(theta);
                double speed = 0.35 + RANDOM.nextDouble() * 0.9;
                // 1.0.31: pieces of its flesh.
                int color = RANDOM.nextInt(4) == 0 ? DragonPalette.MEAT_DEEP : (RANDOM.nextInt(3) == 0 ? DragonPalette.MEAT_HI : DragonPalette.MEAT);
                FRAGMENTS.add(new Fragment(cx + nx * r[i], cy + ny * r[i], cz + nz * r[i], nx * speed, ny * speed * 0.8 + 0.35 + RANDOM.nextDouble() * 0.4, nz * speed,
                        RANDOM.nextDouble() - 0.5, RANDOM.nextDouble() - 0.5, RANDOM.nextDouble() - 0.5, (RANDOM.nextDouble() - 0.5) * 0.6,
                        0.5 + RANDOM.nextDouble() * 1.8 * Math.min(1.0, r[i] / 4.0), color, 50 + RANDOM.nextInt(40)));
            }
            burst(cx, cy - r[i] * 0.5, cz, 4, r[i]);
        }
    }

    /** 1.0.11: this player is turning into the Dragon (before it breaks out of them): how far, 0..1, or -1. */
    static double turning(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        double p = com.hex.kagune.control.client.TurningClient.progress(minecraft.player, com.hex.kagune.control.client.ControlClient.worldTime(partialTick));
        if (p < 0.0) preludeAt = null;
        return p;
    }

    static Dragon local() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? null : DRAGONS.get(minecraft.player.getUUID());
    }

    /** True while this player's human body is gone, for hiding their model everywhere. */
    static boolean hidden(UUID player) {
        Dragon dragon = DRAGONS.get(player);
        return dragon != null && !dragon.ended;
    }

    static void tick() {
        ticks++;
        ClientLevel level = Minecraft.getInstance().level;
        Iterator<Map.Entry<UUID, Dragon>> iterator = DRAGONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Dragon dragon = iterator.next().getValue();
            if (!dragon.queue.isEmpty()) {
                // Play frames back one tick behind the network so motion stays smooth under jitter.
                while (dragon.queue.size() > 2) dragon.advance();
                dragon.advance();
                dragon.staleTicks = 0;
            } else {
                dragon.previous = dragon.current;
                dragon.gait.hold();
                dragon.staleTicks++;
            }
            boolean finished = dragon.ended && ticks - dragon.endedAt > 40;
            if (finished || dragon.staleTicks > 60 || (dragon.current != null && dragon.current.tick() > DragonTimeline.END_TICK + 40)) iterator.remove();
        }
        Iterator<Fragment> fragments = FRAGMENTS.iterator();
        while (fragments.hasNext()) {
            Fragment f = fragments.next();
            if (++f.age >= f.life) {
                fragments.remove();
                continue;
            }
            if (f.resting) continue;
            f.vy -= 0.045;
            f.vx *= 0.985;
            f.vz *= 0.985;
            double nx = f.x + f.vx;
            double ny = f.y + f.vy;
            double nz = f.z + f.vz;
            if (level != null) {
                BlockPos below = BlockPos.containing(nx, ny - f.size * 0.3, nz);
                if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                    f.vx *= 0.4;
                    f.vz *= 0.4;
                    f.vy = Math.abs(f.vy) > 0.25 ? -f.vy * 0.25 : 0.0;
                    if (f.vy == 0.0 && Math.abs(f.vx) + Math.abs(f.vz) < 0.05) f.resting = true;
                    ny = f.y;
                }
            }
            f.x = nx;
            f.y = ny;
            f.z = nz;
            f.angle += f.spin;
        }
        RINGS.removeIf(ring -> ++ring.age >= ring.life);
        shake *= 0.86;
        redFlash *= 0.9;
        whiteFlash *= 0.93;
        fovPunch *= 0.9;
        crackGlow *= 0.97;
        if (shake < 0.005) shake = 0.0;
    }

    static void clear() {
        preludeAt = null;
        DRAGONS.clear();
        FRAGMENTS.clear();
        RINGS.clear();
        shake = 0.0;
        redFlash = 0.0;
        whiteFlash = 0.0;
        fovPunch = 0.0;
        crackGlow = 0.0;
        status = new S2CAwakeningStatus(AwakeningStage.DORMANT, false);
    }
}
