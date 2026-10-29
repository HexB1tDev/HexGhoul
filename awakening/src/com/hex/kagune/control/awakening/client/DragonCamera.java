package com.hex.kagune.control.awakening.client;

import com.hex.kagune.control.awakening.DragonRig;
import com.hex.kagune.control.awakening.DragonTimeline;
import com.hex.kagune.control.awakening.V3;
import com.hex.kagune.entity.KaguneEntities;
import com.hex.kagune.entity.KaguneSpikeEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Cinematic third-person camera for the owner of a Dragon. The view is moved to a private, never
 * ticked camera entity that orbits the creature, scales its distance and height to the body's size,
 * climbs over terrain and the body itself when they block the view, and finally settles back into
 * Rinkaku's eyes as he emerges.
 *
 * <p>Every frame the final camera position, after smoothing, shake and the release, is pushed out
 * of the Dragon's body and out of solid blocks ({@link #safe}). A camera inside either renders the
 * inside of the mesh or of the block, which reads as a black screen.
 */
final class DragonCamera {
    private static final double RELEASE_SECONDS = 0.6;
    /** Eye height of the 0.24-block spike entity used as the camera: 0.85 of its height. */
    private static final double EYE = 0.24 * 0.85;
    /** Clearance kept between the lens and the outside of the body. */
    private static final double BODY_MARGIN = 2.0;
    private static Entity eye;
    private static CameraType savedType;
    private static boolean active;
    private static boolean releasing;
    private static double release;
    private static V3 position;
    private static V3 focus;
    private static double orbit;
    private static double lift;
    private static double bars;
    private static long lastNanos;
    private static V3 hotSpot;
    private static double hotWeight;
    private static float yaw;
    private static float pitch;

    private DragonCamera() {
    }

    static boolean active() {
        return active;
    }

    /** 1.0.13: the camera is behind War's small Dragon (Centipede Dominion), not filming the great one. */
    static boolean chase() {
        return active && chase;
    }

    private static boolean chase;

    static double bars() {
        return bars;
    }

    static void hot(V3 world, double weight) {
        hotSpot = world;
        hotWeight = Math.max(hotWeight, weight);
    }

    /** Client tick: start, release or stop the cinematic as the local Dragon comes and goes. */
    static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            reset();
            return;
        }
        DragonClient.Dragon dragon = DragonClient.local();
        // 1.0.11: and from the first moment he turns, before the Dragon is out of him.
        boolean turning = DragonClient.turning(1.0F) >= 0.0;
        boolean running = dragon != null && dragon.current != null && !dragon.ended || turning && (dragon == null || !dragon.ended);
        if (running && !active) {
            start(minecraft, player, turning && (dragon == null || dragon.current == null));
            chase = dragon != null && dragon.current != null && dragon.current.mini();
        }
        if (active && !running && !releasing) {
            releasing = true;
            release = 0.0;
        }
        if (active && eye != null && eye.level() != minecraft.level) reset();
        if (active && minecraft.screen != null && !(minecraft.screen instanceof ChatScreen) && !(minecraft.screen instanceof PauseScreen)) {
            minecraft.setScreen(null);
        }
        hotWeight *= 0.95;
    }

    private static void start(Minecraft minecraft, LocalPlayer player, boolean turning) {
        ClientLevel level = minecraft.level;
        @SuppressWarnings("unchecked")
        EntityType<? extends KaguneSpikeEntity> type = (EntityType<? extends KaguneSpikeEntity>) KaguneEntities.KAGUNE_SPIKE.get();
        eye = new KaguneSpikeEntity(type, level);
        savedType = minecraft.options.getCameraType();
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        Vec3 eyes = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        position = new V3(eyes.x, eyes.y, eyes.z);
        focus = position.add(look.x * 8.0, look.y * 8.0, look.z * 8.0);
        orbit = Math.atan2(-look.z, -look.x);
        lift = 0.0;
        if (turning) {
            // 1.0.11: cut straight to him, from in front, close: his face as it happens to him.
            orbit = Math.atan2(look.z, look.x);
            focus = new V3(player.getX(), player.getY() + 1.35, player.getZ());
            position = safe(focus.add(Math.cos(orbit) * PRELUDE_NEAR, 0.4, Math.sin(orbit) * PRELUDE_NEAR), null, level);
        }
        lastChoice = 0;
        yaw = player.getYRot();
        pitch = player.getXRot();
        place(position, yaw, pitch);
        minecraft.setCameraEntity(eye);
        active = true;
        releasing = false;
        release = 0.0;
        lastNanos = System.nanoTime();
        if (minecraft.screen != null && !(minecraft.screen instanceof ChatScreen) && !(minecraft.screen instanceof PauseScreen)) minecraft.setScreen(null);
    }

    static void reset() {
        Minecraft minecraft = Minecraft.getInstance();
        if (active) {
            if (minecraft.player != null) minecraft.setCameraEntity(minecraft.player);
            if (savedType != null) minecraft.options.setCameraType(savedType);
        }
        active = false;
        chase = false;
        chaseFlat = null;
        releasing = false;
        eye = null;
        savedType = null;
        bars = 0.0;
        hotSpot = null;
        hotWeight = 0.0;
    }

    /** Render frame, before the camera is set up: move the camera entity to this frame's shot. */
    static void frame(float partialTick) {
        if (!active || eye == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            reset();
            return;
        }
        long now = System.nanoTime();
        double dt = Math.max(0.0, Math.min(0.1, (now - lastNanos) / 1.0E9));
        lastNanos = now;
        bars += ((releasing || chase ? 0.0 : 1.0) - bars) * (1.0 - Math.exp(-dt * 3.0));
        DragonClient.Dragon dragon = DragonClient.local();
        DragonPose pose = dragon == null ? null : dragon.pose(partialTick);
        double turning = DragonClient.turning(partialTick);
        if (pose != null && !releasing && chase) {
            chaseShot(pose, player, level, dt, partialTick);
        } else if (pose != null && !releasing) {
            shoot(pose, level, dt);
        } else if (turning >= 0.0 && !releasing) {
            shootTurning(player, level, dt, turning);
        }
        V3 shot = position;
        V3 target = focus;
        if (releasing) {
            release = Math.min(1.0, release + dt / RELEASE_SECONDS);
            Vec3 eyes = player.getEyePosition(partialTick);
            Vec3 look = player.getViewVector(partialTick);
            double k = DragonTimeline.smooth(release);
            // Settle at Rinkaku's feet: once the view returns to the player it rises to eye level,
            // so the hand-off reads as him standing up out of the fragments rather than a cut.
            V3 home = new V3(player.getX(), player.getY(), player.getZ());
            shot = position.lerp(home, k);
            target = focus.lerp(new V3(eyes.x + look.x * 8.0, eyes.y + look.y * 8.0, eyes.z + look.z * 8.0), k);
            if (release >= 1.0) {
                reset();
                return;
            }
        }
        double shake = DragonClient.shake;
        double t = now / 1.0E9;
        V3 jitter = new V3(Math.sin(t * 41.0), Math.sin(t * 37.0 + 1.3), Math.sin(t * 43.0 + 2.1)).scale(shake * 0.22);
        // The final position, shake and release included, is what must never be inside anything.
        V3 lens = safe(shot.add(jitter), pose, level);
        V3 view = target.sub(lens.sub(jitter));
        double horizontal = Math.sqrt(view.x() * view.x() + view.z() * view.z());
        yaw = (float) Math.toDegrees(Math.atan2(-view.x(), view.z()));
        pitch = (float) Math.toDegrees(-Math.atan2(view.y(), Math.max(1.0E-4, horizontal)));
        place(lens, yaw, pitch);
    }

    /**
     * Moves a camera position out of the Dragon (the body tubes, necks and jaws, with a margin) and
     * then up until neither the position nor the camera entity's eye above it is in a solid block.
     */
    static V3 safe(V3 at, DragonPose pose, ClientLevel level) {
        V3 p = at;
        for (int pass = 0; pass < 3; pass++) {
            if (pose != null) p = pose.outside(p, BODY_MARGIN);
            boolean clear = true;
            for (int step = 0; step < 96 && (solid(level, p) || solid(level, p.add(0.0, EYE, 0.0))); step++) {
                p = p.add(0.0, 0.5, 0.0);
                clear = false;
            }
            if (clear) break;
        }
        return p.withY(Math.max(level.getMinBuildHeight() + 2.0, p.y()));
    }

    private static boolean solid(ClientLevel level, V3 p) {
        BlockPos pos = BlockPos.containing(p.x(), p.y(), p.z());
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    /**
     * 1.0.13: behind the small Dragon's head, over it, looking where he aims (he steers it by looking): close, and quick to follow, so
     * it plays as he does.
     */
    /** 1.0.13: where the chase camera sits: this far behind the jaws and this high above them (blocks). */
    static final double CHASE_BACK = 9.5;
    static final double CHASE_UP = 4.0;
    /** 1.0.39: the last level aim the chase camera sat behind. */
    private static V3 chaseFlat;

    private static void chaseShot(DragonPose pose, LocalPlayer player, ClientLevel level, double dt, float partialTick) {
        // 1.0.39: on its neck, not its jaws: the jaws lunge four blocks out for a bite and rear up for a slam, and the lens lurched
        // after them with every one.
        V3 head = pose.world(pose.segments[0]);
        Vec3 view = player.getViewVector(partialTick);
        V3 look = new V3(view.x, view.y, view.z).normalize();
        // High and well back along the level heading, so pitching the jaws down never drives the lens into the body and the side
        // heads stay low in the corners of the frame instead of in front of it.
        V3 flat = new V3(look.x(), 0.0, look.z());
        // 1.0.39: looking (nearly) straight down, the level aim is a sliver that swings wildly with the mouse: keep the last one.
        flat = flat.length() < 0.08 ? (chaseFlat != null ? chaseFlat : new V3(0.0, 0.0, 1.0)) : flat.normalize();
        chaseFlat = flat;
        focus = focus.lerp(head.add(look.scale(8.0)), 1.0 - Math.exp(-dt * 10.0));
        V3 desired = head.sub(flat.scale(CHASE_BACK)).add(0.0, CHASE_UP, 0.0);
        position = safe(position.lerp(safe(desired, pose, level), 1.0 - Math.exp(-dt * 8.0)), pose, level);
    }

    /** 1.0.11: how close the camera starts on him as he turns, and how far it has drawn back by the end (blocks). */
    private static final double PRELUDE_NEAR = 3.4;
    private static final double PRELUDE_FAR = 7.5;

    /**
     * 1.0.11: the turning: close on him from in front, circling slowly, drawing back as it takes him (and with every burst), a little
     * lower when he arches back screaming; then the eruption's shot takes over from wherever it is.
     */
    private static void shootTurning(LocalPlayer player, ClientLevel level, double dt, double p) {
        V3 chest = DragonClient.preludeAt != null ? DragonClient.preludeAt : new V3(player.getX(), player.getY() + 1.2, player.getZ());
        double scream = com.hex.kagune.control.king.TurningModel.scream(p);
        double burst = com.hex.kagune.control.king.TurningModel.burst(p);
        V3 wantFocus = chest.add(0.0, 0.2 + 0.25 * scream, 0.0);
        double distance = PRELUDE_NEAR + (PRELUDE_FAR - PRELUDE_NEAR) * DragonTimeline.smooth(p) + 1.4 * burst;
        double elevation = 8.0 + 14.0 * p - 6.0 * scream;
        orbit += Math.toRadians(7.0) * dt;
        focus = focus.lerp(wantFocus, 1.0 - Math.exp(-dt * 4.0));
        V3 desired = clearShot(level, null, focus, distance, elevation, dt);
        if (desired == null) {
            desired = focus.add(new V3(Math.cos(orbit), 0.3, Math.sin(orbit)).normalize().scale(distance));
        }
        position = safe(position.lerp(safe(desired, null, level), 1.0 - Math.exp(-dt * (sight(level, position, focus) ? 2.6 : 8.0))), null, level);
    }

    /** The last clear shot's way round and up from the one wanted (an index into {@link #CHOICES}), tried first the next frame. */
    private static int lastChoice;
    /** Ways to look for a clear shot: {degrees round the orbit, degrees higher}; the shot wanted first. */
    private static final double[][] CHOICES = {{0, 0}, {30, 0}, {-30, 0}, {0, 14}, {30, 14}, {-30, 14}, {60, 10}, {-60, 10}, {0, 28}, {90, 20},
        {-90, 20}, {0, 42}, {150, 25}, {-150, 25}, {180, 35}, {0, 60}};

    /**
     * 1.0.11: where the lens can see {@code focus} from: the shot wanted ({@code distance} out on the orbit, {@code elevation} degrees up)
     * if nothing is in the way, else the nearest way round or up that is clear of the ground and of the Dragon; null if none is. The
     * orbit and the lift drift toward the one found, so the next frames start there and the camera settles instead of hunting.
     */
    private static V3 clearShot(ClientLevel level, DragonPose pose, V3 focus, double distance, double elevation, double dt) {
        int[] order = new int[CHOICES.length + 1];
        order[0] = lastChoice;
        for (int i = 0; i < CHOICES.length; i++) order[i + 1] = i;
        for (int i : order) {
            double[] choice = CHOICES[i];
            double around = orbit + Math.toRadians(choice[0]);
            double angle = Math.toRadians(Math.min(80.0, elevation + lift + choice[1]));
            V3 at = focus.add(new V3(Math.cos(around) * Math.cos(angle), Math.sin(angle), Math.sin(around) * Math.cos(angle)).scale(distance));
            if (!sight(level, at, focus) || pose != null && behindBody(pose, at, focus) || solid(level, at)) continue;
            lastChoice = i;
            double settle = 1.0 - Math.exp(-dt * 1.4);
            orbit += Math.toRadians(choice[0]) * settle;
            lift = Math.max(0.0, Math.min(50.0, lift + choice[1] * settle));
            return at;
        }
        return null;
    }

    /** Nothing solid between {@code from} and {@code to}. */
    private static boolean sight(ClientLevel level, V3 from, V3 to) {
        BlockHitResult hit = level.clip(new ClipContext(new Vec3(from.x(), from.y(), from.z()), new Vec3(to.x(), to.y(), to.z()), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, Minecraft.getInstance().player));
        return hit.getType() != HitResult.Type.BLOCK;
    }

    /** The Dragon's own body between {@code lens} and {@code focus} (not counting the part round the focus). */
    private static boolean behindBody(DragonPose pose, V3 lens, V3 focus) {
        for (int i = 0; i < DragonRig.SEGMENTS; i++) {
            if (!pose.born(i)) continue;
            V3 center = pose.world(pose.segments[i]);
            double r = pose.radii[i] * 1.08;
            if (center.distance(focus) < r + 6.0) continue;
            if (center.distanceToSegment(lens, focus) < r) return true;
        }
        return false;
    }

    /** 1.0.11: a focus buried in the ground (a head digging) brought up to the surface over it, so there is something to see. */
    private static V3 surfaced(ClientLevel level, V3 at) {
        V3 p = at;
        for (int step = 0; step < 64 && solid(level, p); step++) p = p.add(0.0, 0.5, 0.0);
        return p == at ? at : p.add(0.0, 1.0, 0.0);
    }

    /** Chooses where the camera wants to be for the current phase, then resolves what blocks it. */
    private static void shoot(DragonPose pose, ClientLevel level, double dt) {
        double tick = pose.tick;
        V3 anchor = new V3(pose.anchorX, pose.anchorY, pose.anchorZ);
        V3 centroid = pose.world(pose.centroid());
        double extent = Math.max(10.0, pose.extent(pose.centroid()));
        V3 wantFocus;
        double distance;
        double elevation;
        double spin;
        if (tick < DragonTimeline.RAMPAGE_START) {
            // The head bursts out of the ground in a low arch: watch it from close by, just above the crater.
            double k = DragonTimeline.smooth(tick / DragonTimeline.ERUPTION_TICKS);
            wantFocus = anchor.add(0.0, 2.0 + 7.0 * k, 0.0);
            distance = 14.0 + 22.0 * k;
            elevation = 14.0 + 10.0 * k;
            spin = 3.0;
        } else if (tick < DragonTimeline.RAMPAGE_END) {
            // Follow the head as it leads, with the body in frame behind it.
            wantFocus = centroid.lerp(pose.world(pose.headTips[0]), 0.55);
            if (hotSpot != null) wantFocus = wantFocus.lerp(hotSpot, 0.3 * Math.min(1.0, hotWeight));
            distance = Math.max(38.0, Math.min(115.0, extent * 1.25 + 12.0));
            elevation = 30.0 + 4.0 * Math.sin(tick * 0.004);
            spin = 5.0;
        } else if (tick < DragonTimeline.SHATTER_TICK) {
            wantFocus = centroid;
            distance = Math.max(30.0, Math.min(90.0, extent * 1.05));
            elevation = 26.0;
            spin = 2.0;
        } else {
            V3 emergence = pose.world(pose.emergence).add(0.0, 1.2, 0.0);
            double k = DragonTimeline.smooth((tick - DragonTimeline.SHATTER_TICK) / DragonTimeline.EMERGENCE_TICKS);
            wantFocus = emergence;
            distance = 48.0 + (6.0 - 48.0) * k;
            elevation = 30.0 + (10.0 - 30.0) * k;
            spin = 6.0;
        }
        orbit += Math.toRadians(spin) * dt;
        focus = focus.lerp(surfaced(level, wantFocus), 1.0 - Math.exp(-dt * 3.0));
        // 1.0.11: a shot that sees it, round or over whatever is in the way (the hills, the Dragon's own coils), found before the old
        // pull-in below is ever needed; and the lens hurries there when it can't see from where it is.
        V3 clear = clearShot(level, pose, focus, distance, elevation, dt);
        if (clear != null) {
            V3 lens = safe(clear, pose, level);
            position = safe(position.lerp(lens, 1.0 - Math.exp(-dt * (sight(level, position, focus) ? 2.2 : 7.0))), pose, level);
            return;
        }
        double angle = Math.toRadians(Math.min(75.0, elevation + lift));
        V3 offset = new V3(Math.cos(orbit) * Math.cos(angle), Math.sin(angle), Math.sin(orbit) * Math.cos(angle)).scale(distance);
        V3 desired = focus.add(offset);
        boolean blocked = false;
        // Terrain between the creature and the lens: pull in to the obstruction and start climbing.
        BlockHitResult hit = level.clip(new ClipContext(new Vec3(focus.x(), focus.y(), focus.z()), new Vec3(desired.x(), desired.y(), desired.z()),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Minecraft.getInstance().player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 at = hit.getLocation();
            V3 back = focus.sub(desired).normalize();
            desired = new V3(at.x, at.y, at.z).add(back.scale(0.8));
            blocked = true;
        }
        // The Dragon's own body between the lens and the focus: rise above it.
        for (int i = 0; i < DragonRig.SEGMENTS; i++) {
            if (!pose.born(i)) continue;
            V3 center = pose.world(pose.segments[i]);
            double r = pose.radii[i] * 1.08;
            if (center.distance(focus) < r + 6.0) continue;
            if (center.distanceToSegment(desired, focus) < r) blocked = true;
            double inside = desired.distance(center);
            if (inside < r + 3.0) desired = center.add(desired.sub(center).normalize().scale(r + 3.0));
        }
        lift = blocked ? Math.min(50.0, lift + 40.0 * dt) : Math.max(0.0, lift - 12.0 * dt);
        // The smoothed path itself must stay clear too, or it cuts through hills and the body.
        position = safe(position.lerp(safe(desired, pose, level), 1.0 - Math.exp(-dt * 2.2)), pose, level);
    }

    private static void place(V3 at, float yawDegrees, float pitchDegrees) {
        eye.setPos(at.x(), at.y(), at.z());
        eye.xo = at.x();
        eye.yo = at.y();
        eye.zo = at.z();
        eye.xOld = at.x();
        eye.yOld = at.y();
        eye.zOld = at.z();
        eye.setYRot(yawDegrees);
        eye.setXRot(pitchDegrees);
        eye.yRotO = yawDegrees;
        eye.xRotO = pitchDegrees;
    }
}
