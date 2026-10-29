package com.hex.kagune.control.awakening;

import com.hex.kagune.control.king.WarRules;
import com.hex.kagune.control.server.KingEffects;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

/**
 * 1.0.13: Centipede Dominion: War as a small Dragon ({@link WarRules#WYRM_TICKS} of it). It goes on forward on its own, turning toward
 * where he aims (no faster than {@link WarRules#WYRM_TURN}). 1.0.15: it crawls on the land as a centipede does, never through it:
 * over every rise, up the face of a wall before it goes on over it, down off edges, its head rearing up when he looks up; its front
 * head bites what it runs into, the heads along its back snap at what comes near,
 * and its body crushes what it rolls over. Drawn by every client exactly as the great Dragon is, from the same frames, a quarter of
 * its girth and a fraction of its length; his own body is held where he stood (unseen), and comes out where the small Dragon ends.
 * 1.0.29: his to drive ({@link #input}): it goes on W (rushing when he sprints) and stops when he lets go, turning hard with his aim
 * and harder on A and D; M1 lunges its head out at what he looks at and bites ({@link #lunge}), RMB rears it up and slams it down
 * ({@link #slam}), Space leaps it ({@code WarRules.WYRM_LEAP_*}), sneaking ends it. 1.0.33: it goes the way he walks, W A S D round
 * where he looks ({@link WarRules#wyrmWay}; S the brake), M1 and RMB come with the keys; and first a little turning of his own (a
 * short one of the Dragon's): doubled over, then it tears out of his back, rears up over him roaring and comes down before him
 * ({@link WarRules#WYRM_PRELUDE}, {@link WarRules#WYRM_RISE}), its body pouring out of him after it.
 */
final class WarWyrm {
    /** Blocks between the small Dragon's segments (the great one's are {@link DragonRig#SPACING}): it is about 27 blocks long. */
    static final double SPACING = 0.32;
    static final double LENGTH = DragonRig.SEGMENTS * SPACING;

    private final ServerPlayer owner;
    private final ServerLevel level;
    private final V3 anchor;
    /** Where his own body is held, unseen (1.0.39: brought along under its head, see {@link #follow}). */
    private V3 pin;
    private final DragonTrail trail;
    private V3 lead;
    private V3 heading;
    private int age;
    private final V3[] segments = new V3[DragonRig.SEGMENTS];
    private final double[] radii = new double[DragonRig.SEGMENTS];
    private final V3[] heads = new V3[DragonRig.HEADS];
    private final double[] jaws = new double[DragonRig.HEADS];
    private final int[] snapping = new int[DragonRig.HEADS];
    private final Map<Integer, Integer> bitten = new HashMap<>();
    /** 1.0.15: the land it crawls on. */
    private final TerrainProbe ground;
    /** 1.0.29: driven: the keys held (WarRules.WYRM_* bits), its pace now, when it ends; the lunge, the slam and the leap (when each began, -1: not, and when each is ready again). */
    private int keys;
    private double speed;
    private int endAt = WarRules.WYRM_PRELUDE + WarRules.WYRM_RISE + WarRules.WYRM_TICKS;
    /** 1.0.33: where it comes out of him (his back), and the tick it is his to drive. */
    private final V3 chest;
    private static final int DRIVEN = WarRules.WYRM_PRELUDE + WarRules.WYRM_RISE;
    private int lungeAt = -100;
    private int lungeReady;
    private int slamAt = -100;
    private int slamReady;
    private int leapAt = -100;
    private int leapReady;
    private V3 look = new V3(0.0, 0.0, 1.0);
    /** 1.0.39: where he looks, as his client last sent it (null: none yet, his body's own aim is used). */
    private Vec3 aim;

    /** 1.0.19: a mark it leaves (KingMarkFx), seen by all near its owner. */
    private void mark(int style, V3 at, V3 way, double power, net.minecraft.world.entity.Entity body) {
        float[] data = {(float) at.x(), (float) at.y(), (float) at.z(), (float) way.x(), (float) way.y(), (float) way.z(), (float) power, 0.0F, 0.0F};
        com.hex.kagune.control.network.ControlNetwork.broadcast(owner, new com.hex.kagune.control.network.S2CKingFx(owner.getUUID(),
                com.hex.kagune.control.network.S2CKingFx.MARK, new int[] {style, body == null ? 0 : body.getId()}, data));
    }

    WarWyrm(ServerPlayer owner) {
        this.owner = owner;
        this.level = owner.serverLevel();
        Entity body = owner;
        Vec3 eye = body.getEyePosition(1.0F);
        Vec3 view = body.getViewVector(1.0F);
        V3 flat = new V3(view.x, 0.0, view.z);
        this.heading = flat.length() < 1.0E-4 ? new V3(0.0, 0.0, 1.0) : flat.normalize();
        this.anchor = new V3(owner.getX(), owner.getY(), owner.getZ());
        this.pin = this.anchor;
        this.ground = new TerrainProbe(this.level, owner.getY());
        // 1.0.33: it comes out of his back, and pours out of him from there.
        this.chest = new V3(owner.getX(), owner.getY() + 1.1, owner.getZ()).sub(this.heading.scale(0.3));
        this.lead = this.chest;
        this.trail = new DragonTrail(this.chest, LENGTH + 4.0);
        play(this.chest, HexKaguneSounds.KAGUNE_STRAIN, 2.0F, 0.6F);
        play(this.chest, HexKaguneSounds.BONE_CRACK, 1.6F, 0.7F);
    }

    boolean done() {
        return age >= endAt;
    }

    /**
     * 1.0.29: the keys he holds this tick: W A S D, Space, sprint, sneak (sneaking ends it, after its first second). 1.0.39: and where
     * he looks ({@link WarRules#wyrmAim}): the server hears nothing else of his turning while the chase camera has his view, so it
     * steered by the way he faced when it began, not by where the camera looked.
     */
    void input(int keys, int aim) {
        this.keys = keys;
        double[] seen = WarRules.wyrmLook(aim);
        if (seen != null) {
            this.aim = new Vec3(seen[0], seen[1], seen[2]);
            // (and his body's own aim kept with it, so bringing it along under the head never turns his view back to a stale one)
            double[] angles = WarRules.wyrmAngles(aim);
            owner.setYRot((float) angles[0]);
            owner.setXRot((float) angles[1]);
        }
        if (age < DRIVEN) return; // still coming out of him
        if ((keys & WarRules.WYRM_SNEAK) != 0 && age > DRIVEN + 10) endAt = Math.min(endAt, age + 10);
        if ((keys & WarRules.WYRM_KEY_BITE) != 0) lunge();
        if ((keys & WarRules.WYRM_KEY_SLAM) != 0) slam();
        if ((keys & WarRules.WYRM_JUMP) != 0 && age >= leapReady && age < endAt - 12) {
            leapAt = age;
            leapReady = age + WarRules.WYRM_LEAP_EVERY;
            play(lead, HexKaguneSounds.KAGUNE_WHIP, 2.0F, 0.6F);
        }
    }

    /** 1.0.29: M1: its head lunged out at what he looks at, biting. */
    void lunge() {
        if (age < DRIVEN || age < lungeReady) return;
        lungeAt = age;
        lungeReady = age + WarRules.WYRM_LUNGE_EVERY;
        play(lead, HexKaguneSounds.KAGUNE_WHIP, 1.8F, 0.9F);
        play(lead, HexKaguneSounds.CENTIPEDE_CLICK, 2.0F, 0.6F);
    }

    /** 1.0.29: RMB: reared up and slammed down before it. */
    void slam() {
        if (age < DRIVEN || age < slamReady) return;
        slamAt = age;
        slamReady = age + WarRules.WYRM_SLAM_EVERY;
        play(lead, HexKaguneSounds.KAGUNE_STRAIN, 2.0F, 0.6F);
    }

    /** One tick: steer, go, lay the body along the way it went, hurt what it meets, and show it to everyone near. */
    void tick() {
        age++;
        hold();
        if (age < WarRules.WYRM_PRELUDE) {
            prelude();
            return; // (nothing of it yet: his body is still his, seen)
        }
        if (age == WarRules.WYRM_PRELUDE) burst();
        Vec3 view = aim != null ? aim : owner.getViewVector(1.0F);
        look = new V3(view.x, view.y, view.z).normalize();
        if (age < DRIVEN) {
            rise(age - WarRules.WYRM_PRELUDE);
        } else {
            drive(view);
        }
        trail.record(lead);
        lay();
        if (age >= DRIVEN) hunt();
        if (age - leapAt == WarRules.WYRM_LEAP_TICKS) landed(heads[0], WarRules.WYRM_SLAM_RADIUS * 0.7, WarRules.WYRM_SLAM_DAMAGE * 0.6F);
        if (age - slamAt == WarRules.WYRM_SLAM_LANDS) landed(heads[0], WarRules.WYRM_SLAM_RADIUS, WarRules.WYRM_SLAM_DAMAGE);
        if (age % 3 == 0) {
            mark(com.hex.kagune.control.king.KingMarkFx.SOOT, lead, new V3(0.0, 1.0, 0.0), 0.8, null);
        }
        if (age % 6 == 0) play(lead, HexKaguneSounds.CENTIPEDE_CLICK, 1.6F, 0.7F + 0.1F * (age % 4));
        if (age >= DRIVEN) follow();
        AwakeningNetwork.toWatchers(owner, frame());
    }

    /** 1.0.33: doubled over, shaking: his back splitting, blood and soot out of it, his bones cracking. */
    private void prelude() {
        V3 back = chest.add(0.0, 0.2, 0.0);
        if (age % 3 == 0) {
            mark(age % 6 == 0 ? com.hex.kagune.control.king.KingMarkFx.GORE : com.hex.kagune.control.king.KingMarkFx.SOOT, back, heading.scale(-1.0).add(0.0, 0.8, 0.0),
                    0.5 + 0.05 * age, owner);
        }
        if (age % 4 == 1) play(back, HexKaguneSounds.BONE_CRACK, 1.4F + 0.05F * age, 0.6F + 0.02F * age);
    }

    /** 1.0.33: it tears out of his back. */
    private void burst() {
        mark(com.hex.kagune.control.king.KingMarkFx.GORE, chest, new V3(0.0, 1.0, 0.0), 1.4, null);
        mark(com.hex.kagune.control.king.KingMarkFx.SOOT, chest, new V3(0.0, 1.0, 0.0), 1.2, null);
        AwakeningNetwork.toWatchers(owner, new S2CDragonEvent(owner.getUUID(), S2CDragonEvent.Kind.BURST, chest.x(), chest.y(), chest.z(), 0.5F));
        play(chest, HexKaguneSounds.MANIFEST_RIP, 3.0F, 0.8F);
        play(chest, HexKaguneSounds.KAGUNE_STRAIN, 2.0F, 0.7F);
        WarKit.vanilla(owner, chest, "entity.generic.explode", 1.2F, 1.4F);
        WarKit.vanilla(owner, chest, "hexkagune:sfx.transform.body_warp", 2.4F, 1.0F); // 1.2.7: the user's own transformation
    }

    /** 1.0.33: rising out of him {@code t} ticks: up over him, the roar at the top, then down onto the ground before him. */
    private void rise(int t) {
        double[] at = WarRules.wyrmRise(t);
        V3 head = chest.add(heading.scale(at[0])).add(0.0, at[1], 0.0);
        double floor = ground.surface(head.x(), head.z());
        lead = head.withY(Math.max(head.y(), floor + DragonRig.baseRadius(0) * WarRules.WYRM_SCALE * 0.9 + 0.25));
        if (t == WarRules.WYRM_RISE_TOP) {
            WarKit.vanilla(owner, lead, "entity.ravager.roar", 3.0F, 0.6F);
            play(lead, HexKaguneSounds.CENTIPEDE_CLICK, 2.4F, 0.5F);
        }
        if (t == WarRules.WYRM_RISE - 2) {
            mark(com.hex.kagune.control.king.KingMarkFx.QUAKE, lead.withY(floor), V3.ZERO, 1.0, null);
            play(lead, HexKaguneSounds.KAGUNE_SLAM, 2.0F, 0.7F);
        }
    }

    /**
     * 1.0.29: driven: it turns to where he steers it and goes at its pace. 1.0.33: W A S D round where he looks, as he would walk
     * ({@link WarRules#wyrmWay}): W ahead, A and D off to his left and right of it (on their own too), S the brake; sprint rushes.
     */
    private void drive(Vec3 view) {
        boolean forward = (keys & WarRules.WYRM_FORWARD) != 0;
        int side = ((keys & WarRules.WYRM_LEFT) != 0 ? 1 : 0) - ((keys & WarRules.WYRM_RIGHT) != 0 ? 1 : 0);
        boolean braking = (keys & WarRules.WYRM_BACK) != 0;
        boolean leaping = age - leapAt <= WarRules.WYRM_LEAP_TICKS;
        // 1.0.39: looking (nearly) straight down his level aim is a sliver that swings with the mouse: steer from where it heads.
        boolean down = view.x * view.x + view.z * view.z < 0.08 * 0.08;
        double[] way = braking ? null : WarRules.wyrmWay(down ? heading.x() : view.x, down ? heading.z() : view.z, forward, side);
        if (way != null) {
            heading = steer(heading, new V3(way[0], 0.0, way[1]), speed > 0.1 || leaping ? WarRules.WYRM_TURN_MOVING : WarRules.WYRM_TURN);
        } else if (leaping && new V3(view.x, 0.0, view.z).length() > 1.0E-4) {
            heading = steer(heading, new V3(view.x, 0.0, view.z).normalize(), WarRules.WYRM_TURN_MOVING);
        }
        double pace = way == null ? 0.0 : (keys & WarRules.WYRM_SPRINT) != 0 ? WarRules.WYRM_RUSH : WarRules.WYRM_SPEED;
        speed += Math.max(-WarRules.WYRM_SLOW_DOWN * (braking ? 2.0 : 1.0), Math.min(WarRules.WYRM_SPEED_UP, pace - speed));
        if (leaping) speed = Math.max(speed, WarRules.WYRM_LEAP_SPEED);
        crawl(view.y, speed, leaping ? WarRules.wyrmLeap(age - leapAt) : 0.0);
    }

    /**
     * 1.0.15: one tick on the land: on along its heading at its pace, its head kept just over the ground there (and up to
     * {@link WarRules#WYRM_REAR} over it when he looks up, {@code lookUp} his view's rise); a wall it can't step up it climbs
     * straight up the face of first, an edge it eases down off. Never into the ground, never through a block.
     */
    private void crawl(double lookUp, double pace, double above) {
        double radius = DragonRig.baseRadius(0) * WarRules.WYRM_SCALE;
        double rear = Math.max(0.0, Math.min(1.0, lookUp * 1.6)) * WarRules.WYRM_REAR + above;
        V3 next = lead.add(heading.scale(pace));
        double floor = ground.surface(next.x(), next.z());
        double want = floor + radius * 0.9 + 0.25 + rear;
        double rise = want - lead.y();
        if (rise > WarRules.WYRM_CLIMB * 1.5 && ground.surface(lead.x(), lead.z()) + radius < want - 0.5) {
            // A wall before it: up its face first, where it is.
            lead = lead.withY(lead.y() + WarRules.WYRM_CLIMB);
        } else {
            lead = new V3(next.x(), lead.y() + Math.max(-WarRules.WYRM_FALL, Math.min(WarRules.WYRM_CLIMB, rise)), next.z());
        }
        lead = lead.withY(Math.max(level.getMinBuildHeight() + 2.0, Math.min(level.getMaxBuildHeight() - 2.0, lead.y())));
    }

    /** 1.0.15: {@code at} set down on the land under it (as high as its body is thick over it). */
    private V3 onGround(V3 at) {
        double floor = ground.surface(at.x(), at.z());
        return at.withY(Math.abs(floor - at.y()) < 12.0 ? floor + DragonRig.baseRadius(40) * WarRules.WYRM_SCALE + 0.2 : at.y());
    }

    /** It turns toward {@code wanted} from {@code now}, no more than {@code most} (radians) in a tick. */
    static V3 steer(V3 now, V3 wanted, double most) {
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, now.dot(wanted))));
        if (angle < 1.0E-6) return wanted;
        double t = Math.min(1.0, most / angle);
        V3 out = now.lerp(wanted, t);
        // (Straight round, the two cancel out: turn it a little to one side first.)
        return out.length() < 1.0E-4 ? turned(now, most) : out.normalize();
    }

    /** 1.0.29: {@code way} (level) turned {@code angle} radians to its left. */
    static V3 turned(V3 way, double angle) {
        V3 right = new V3(-way.z(), 0.0, way.x());
        return way.scale(Math.cos(angle)).sub(right.scale(Math.sin(angle))).normalize();
    }

    /** 1.0.29: a slam or a leap come down at {@code at}: everything round it hurt and thrown up, the ground shaken. */
    private void landed(V3 at, double radius, float damage) {
        V3 ground = at.withY(this.ground.surface(at.x(), at.z()));
        for (LivingEntity body : level.getEntitiesOfClass(LivingEntity.class, new AABB(ground.x() - radius, ground.y() - 2.0, ground.z() - radius,
                ground.x() + radius, ground.y() + 4.0, ground.z() + radius), entity -> entity != owner && entity.isAlive() && !entity.isSpectator()
                && !(entity instanceof ArmorStand) && !(entity instanceof net.minecraft.world.entity.player.Player p && p.isCreative()))) {
            V3 c = center(body);
            if (new V3(c.x() - ground.x(), 0.0, c.z() - ground.z()).length() > radius + body.getBbWidth() * 0.5) continue;
            body.invulnerableTime = 0;
            if (!body.hurt(owner.damageSources().playerAttack(owner), damage)) continue;
            KingEffects.heal(owner, (float) (damage * WarRules.LIFESTEAL));
            if (body.isAlive()) {
                V3 away = c.sub(ground).withY(0.0).normalize();
                body.setDeltaMovement(away.x() * 0.6, 0.55, away.z() * 0.6);
                body.hurtMarked = true;
                KingEffects.stun(body, 20);
            }
            mark(com.hex.kagune.control.king.KingMarkFx.BLOOD, c, new V3(0.0, 1.0, 0.0), 1.0, body);
        }
        mark(com.hex.kagune.control.king.KingMarkFx.QUAKE, ground, V3.ZERO, radius / 3.0, null);
        mark(com.hex.kagune.control.king.KingMarkFx.SOOT, ground.add(0.0, 0.3, 0.0), new V3(0.0, 1.0, 0.0), radius / 3.0, null);
        play(ground, HexKaguneSounds.KAGUNE_SLAM, 2.4F, 0.6F);
        play(ground, HexKaguneSounds.BONE_CRACK, 1.8F, 0.7F);
    }

    /** 1.0.39: how far its head may get from his held body before the body is brought along under it (blocks). */
    static final double FOLLOW = 12.0;

    /**
     * 1.0.39: his body (unseen, safe) brought along under its head once it is {@link #FOLLOW} off: the world is loaded, sent and
     * ticked round a player's body, so a body left where he stood had the land under a far-driven Dragon unloaded (it ran on a flat
     * plane through the hills, and the land and the mobs there were gone from his screen) and its frames unsent to those near it.
     */
    private void follow() {
        double dx = lead.x() - pin.x();
        double dz = lead.z() - pin.z();
        if (dx * dx + dz * dz < FOLLOW * FOLLOW) return;
        double floor = ground.surface(lead.x(), lead.z());
        pin = new V3(lead.x(), Math.abs(floor - lead.y()) < 12.0 ? floor : lead.y(), lead.z());
    }

    /** His own body: held where he stood, unseen, safe. */
    private void hold() {
        if (owner.isPassenger()) owner.stopRiding();
        owner.setNoGravity(true);
        owner.setDeltaMovement(Vec3.ZERO);
        owner.fallDistance = 0.0F;
        if (owner.getX() != pin.x() || owner.getY() != pin.y() || owner.getZ() != pin.z()) {
            double dx = owner.getX() - pin.x();
            double dy = owner.getY() - pin.y();
            double dz = owner.getZ() - pin.z();
            if (dx * dx + dy * dy + dz * dz > 0.04) owner.teleportTo(pin.x(), pin.y(), pin.z());
        }
    }

    /** The body along the way the head went, swelling out of nothing at first and gone again at the end; the heads and their jaws. */
    private void lay() {
        double grow = Math.min(1.0, (age - WarRules.WYRM_PRELUDE + 1) / 6.0) * Math.min(1.0, (endAt - age) / 10.0);
        double out = trail.length();
        for (int i = 0; i < DragonRig.SEGMENTS; i++) {
            V3 at = trail.behind(i * SPACING);
            // 1.0.33: only as much of it as has come out of him yet, thinning into his back where it still comes.
            double born = Math.max(0.0, Math.min(1.0, (out - i * SPACING) / (SPACING * 4.0) + 0.25));
            radii[i] = DragonRig.baseRadius(i) * WarRules.WYRM_SCALE * Math.max(0.0, grow) * born;
            // 1.0.15: each ring rests on the land under it, never sunk into it (its thick middle is fatter than its head).
            double floor = ground.surface(at.x(), at.z());
            segments[i] = floor > at.y() - radii[i] * 0.95 && floor < at.y() + 3.0 ? at.withY(floor + radii[i] * 0.95) : at;
        }
        for (int h = 0; h < DragonRig.HEADS; h++) {
            V3 root = segments[DragonRig.HEAD_ROOTS[h]];
            if (h == 0) {
                // 1.0.29: the head turned (a little) to where he looks; lunged out at it; reared up for the slam.
                V3 flatLook = look.withY(0.0).length() < 1.0E-4 ? heading : look.withY(0.0).normalize();
                V3 facing = heading.lerp(flatLook, 0.45).normalize();
                double lunged = WarRules.wyrmLunge(age - lungeAt);
                V3 aim = facing.lerp(look, 0.6).normalize();
                heads[h] = lead.add(facing.scale(1.4 + radii[0])).add(aim.scale(WarRules.WYRM_LUNGE * lunged)).add(0.0, WarRules.wyrmRear(age - slamAt), 0.0);
                jaws[h] = lunged > 0.0 ? (age - lungeAt < 3 ? 1.0 : 0.15) : 0.35 + 0.35 * Math.sin(age * 0.5);
                int rising = age - WarRules.WYRM_PRELUDE;
                if (rising < WarRules.WYRM_RISE) {
                    // 1.0.33: coming out of him: up, the jaws thrown wide for the roar, then down before him.
                    heads[h] = lead.add(heading.scale(1.0 + radii[0])).add(0.0, 0.4 * Math.min(1.0, rising / 6.0), 0.0);
                    jaws[h] = rising >= WarRules.WYRM_RISE_TOP && rising < WarRules.WYRM_RISE_DOWN + 2 ? 1.0 : 0.45;
                }
            } else {
                V3 back = h + 1 < DragonRig.HEADS ? segments[Math.max(0, DragonRig.HEAD_ROOTS[h] - 3)].sub(root) : heading;
                V3 side = back.cross(new V3(0.0, 1.0, 0.0)).normalize().scale(h % 2 == 0 ? 1.0 : -1.0);
                // Leaning out to the flanks, low, out of the chase camera's way.
                V3 rest = root.add(0.0, radii[DragonRig.HEAD_ROOTS[h]] + 0.5, 0.0).add(side.scale(1.5 + 0.3 * Math.sin(age * 0.2 + h)));
                if (snapping[h] > 0 && heads[h] != null) {
                    snapping[h]--;
                } else {
                    heads[h] = rest;
                    jaws[h] = 0.2;
                }
            }
        }
    }

    /** What it meets: bitten by the front head, snapped at by those along its back, crushed under it. */
    private void hunt() {
        bitten.values().removeIf(until -> until < age);
        V3 front = heads[0];
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, box(), entity -> entity != owner && entity.isAlive() && !entity.isSpectator()
                && !(entity instanceof ArmorStand) && !(entity instanceof net.minecraft.world.entity.player.Player p && p.isCreative()));
        for (LivingEntity body : near) {
            if (bitten.containsKey(body.getId())) continue;
            V3 c = center(body);
            double reach = WarRules.wyrmLunge(age - lungeAt) > 0.3 ? WarRules.WYRM_LUNGE_RADIUS : 1.6;
            if (c.distance(front) < reach + body.getBbWidth() * 0.5) {
                bite(body, WarRules.WYRM_BITE, front, 1.0);
                if (body.isAlive()) KingEffects.bleed(body, owner, 80, 1);
                jaws[0] = 1.0;
                play(c, HexKaguneSounds.BONE_CRUSH, 1.4F, 0.8F);
                continue;
            }
            for (int h = 1; h < DragonRig.HEADS; h++) {
                V3 root = segments[DragonRig.HEAD_ROOTS[h]];
                if (c.distance(root) < 3.6 + body.getBbWidth() * 0.5) {
                    heads[h] = c;
                    jaws[h] = 1.0;
                    snapping[h] = 4;
                    bite(body, WarRules.WYRM_SNAP, root, 0.6);
                    play(c, HexKaguneSounds.KAGUNE_WHIP, 1.0F, 1.1F);
                    break;
                }
            }
            if (bitten.containsKey(body.getId())) continue;
            for (int i = 0; i < DragonRig.SEGMENTS; i += 2) {
                if (c.distance(segments[i]) < radii[i] + 0.6 + body.getBbWidth() * 0.5) {
                    bite(body, WarRules.WYRM_CRUSH, segments[i], 0.8);
                    break;
                }
            }
        }
    }

    private void bite(LivingEntity body, float damage, V3 from, double push) {
        bitten.put(body.getId(), age + WarRules.WYRM_AGAIN);
        body.invulnerableTime = 0;
        if (!body.hurt(owner.damageSources().playerAttack(owner), damage)) return;
        KingEffects.heal(owner, (float) (damage * WarRules.LIFESTEAL));
        V3 c = center(body);
        mark(com.hex.kagune.control.king.KingMarkFx.BLOOD, c, c.sub(from).normalize(), 1.0, body);
        if (body.isAlive() && push > 0.0) {
            V3 away = c.sub(from).withY(0.0).normalize();
            body.setDeltaMovement(away.x() * push, 0.3, away.z() * push);
            body.hurtMarked = true;
        }
    }

    /** It is over: the small Dragon comes apart and he steps out of its head, on the ground under it. */
    void end() {
        V3 at = lead;
        TerrainProbe ground = new TerrainProbe(level, at.y());
        double y = ground.surface(at.x(), at.z());
        V3 out = new V3(at.x(), Math.abs(y - at.y()) < 24.0 ? y : at.y(), at.z());
        owner.setNoGravity(false);
        owner.setDeltaMovement(Vec3.ZERO);
        owner.teleportTo(out.x(), out.y(), out.z());
        owner.fallDistance = 0.0F;
        for (int i = 0; i < DragonRig.SEGMENTS; i += 6) {
            V3 s = segments[i];
            if (s == null || radii[i] < 0.05) continue; // (ended before it was out of him)
            mark(i % 12 == 0 ? com.hex.kagune.control.king.KingMarkFx.GORE : com.hex.kagune.control.king.KingMarkFx.SOOT, s, new V3(0.0, 1.0, 0.0), 0.9, null);
        }
        play(out, HexKaguneSounds.BONE_CRACK, 2.0F, 0.6F);
        play(out, HexKaguneSounds.MANIFEST_RIP, 2.0F, 0.9F);
        AwakeningNetwork.toWatchers(owner, new S2CDragonEvent(owner.getUUID(), S2CDragonEvent.Kind.BURST, out.x(), out.y() + 1.0, out.z(), 0.4F));
        AwakeningNetwork.toWatchers(owner, new S2CDragonEvent(owner.getUUID(), S2CDragonEvent.Kind.END, out.x(), out.y(), out.z(), 0.0F));
    }

    /** This tick as a Dragon frame, marked small: every client draws it as it draws the great one (without the great one's camera). */
    private S2CDragonFrame frame() {
        V3 a = anchor;
        float[] s = new float[S2CDragonFrame.SEGMENT_FLOATS];
        float[] r = new float[DragonRig.SEGMENTS];
        for (int i = 0; i < DragonRig.SEGMENTS; i++) {
            s[i * 3] = (float) (segments[i].x() - a.x());
            s[i * 3 + 1] = (float) (segments[i].y() - a.y());
            s[i * 3 + 2] = (float) (segments[i].z() - a.z());
            r[i] = (float) radii[i];
        }
        float[] h = new float[S2CDragonFrame.HEAD_FLOATS];
        byte[] hs = new byte[DragonRig.HEADS];
        for (int i = 0; i < DragonRig.HEADS; i++) {
            V3 tip = heads[i] == null ? segments[DragonRig.HEAD_ROOTS[i]] : heads[i];
            h[i * 4] = (float) (tip.x() - a.x());
            h[i * 4 + 1] = (float) (tip.y() - a.y());
            h[i * 4 + 2] = (float) (tip.z() - a.z());
            h[i * 4 + 3] = (float) jaws[i];
            hs[i] = (byte) (snapping[i] > 0 ? DragonRig.HeadState.STRIKE.ordinal() : DragonRig.HeadState.IDLE.ordinal());
        }
        float[] t = new float[S2CDragonFrame.TENTACLE_FLOATS];
        byte[] ts = new byte[DragonRig.TENTACLES];
        for (int i = 0; i < DragonRig.TENTACLES; i++) {
            int root = DragonRig.TENTACLE_ROOTS[i];
            V3 c = segments[root];
            V3 back = segments[Math.min(DragonRig.SEGMENTS - 1, root + 2)].sub(c);
            V3 side = back.cross(new V3(0.0, 1.0, 0.0)).normalize().scale(DragonBody.tentacleSide(i));
            V3 tip = c.add(side.scale(radii[root] + 1.6)).add(0.0, -0.3 + 0.4 * Math.sin(age * 0.3 + i), 0.0);
            t[i * 3] = (float) (tip.x() - a.x());
            t[i * 3 + 1] = (float) (tip.y() - a.y());
            t[i * 3 + 2] = (float) (tip.z() - a.z());
            ts[i] = (byte) DragonRig.TentacleState.IDLE.ordinal();
        }
        float[] e = {0.0F, 0.0F, 0.0F};
        return new S2CDragonFrame(owner.getUUID(), 100 + age, a.x(), a.y(), a.z(), s, r, h, hs, t, ts, new int[DragonRig.TENTACLES], e, true);
    }

    private AABB box() {
        double[] b = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (int i = 0; i < DragonRig.SEGMENTS; i += 4) {
            V3 p = segments[i];
            b[0] = Math.min(b[0], p.x());
            b[1] = Math.min(b[1], p.y());
            b[2] = Math.min(b[2], p.z());
            b[3] = Math.max(b[3], p.x());
            b[4] = Math.max(b[4], p.y());
            b[5] = Math.max(b[5], p.z());
        }
        return new AABB(b[0] - 5.0, b[1] - 5.0, b[2] - 5.0, b[3] + 5.0, b[4] + 5.0, b[5] + 5.0);
    }

    private static V3 center(Entity entity) {
        return new V3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ());
    }

    private void play(V3 at, RegistryObject<?> sound, float volume, float pitch) {
        level.playSound(null, at.x(), at.y(), at.z(), (SoundEvent) sound.get(), SoundSource.PLAYERS, volume, pitch);
    }
}
