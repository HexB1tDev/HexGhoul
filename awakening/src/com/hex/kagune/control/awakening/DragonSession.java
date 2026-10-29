package com.hex.kagune.control.awakening;

import com.hex.kagune.control.king.TurningModel;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.RegistryObject;

/**
 * One running Dragon awakening. Owns the rig, holds Kaneki's body in place while the Dragon acts on
 * its own, applies every strike, carves and schedules terrain repair, and streams the pose to clients.
 */
final class DragonSession {
    private static final float INSIDE_CRUSH = 10.0F;
    private static final float PRESSED_CRUSH = 8.0F;
    private static final float SHADOW_CRUSH = 3.0F;

    private record Held(LivingEntity entity, boolean noGravity, boolean noAi) {
    }

    final UUID ownerId;
    private ServerPlayer owner;
    final ServerLevel level;
    final V3 anchor;
    private final DragonRig rig;
    private final TerrainProbe ground;
    private final V3[] lastCarve = new V3[DragonRig.SEGMENTS];
    private final Map<Integer, Held> held = new HashMap<>();
    private final Map<Integer, Integer> lashHits = new HashMap<>();
    private final int[] lashSounds = new int[DragonRig.TENTACLES];
    private V3 pin;
    private V3 emergence;
    private int tick = -1;
    /** 1.0.11: ticks into the turning, before the Dragon breaks out of him ({@link TurningModel#PRELUDE_TICKS} of them). */
    private int prelude;
    private boolean finished;

    DragonSession(ServerPlayer owner, V3 anchor) {
        this.ownerId = owner.getUUID();
        this.owner = owner;
        this.level = owner.serverLevel();
        this.anchor = anchor;
        // Hold the body at the eruption point (the ground or water surface where Kaneki fell), so the
        // owner's screen is never tinted by water, lava or a block while the camera is elsewhere.
        this.pin = anchor.add(0.0, 0.05, 0.0);
        this.ground = new TerrainProbe(level, anchor.y());
        this.rig = new DragonRig(anchor, level.getGameTime() ^ ownerId.getLeastSignificantBits());
    }

    /** 1.0.19: a mark a blow leaves (KingMarkFx), seen by all near the owner. */
    private void mark(int style, V3 at, V3 way, double power, net.minecraft.world.entity.Entity body) {
        float[] data = {(float) at.x(), (float) at.y(), (float) at.z(), (float) way.x(), (float) way.y(), (float) way.z(), (float) power, 0.0F, 0.0F};
        com.hex.kagune.control.network.ControlNetwork.broadcast(owner, new com.hex.kagune.control.network.S2CKingFx(owner.getUUID(),
                com.hex.kagune.control.network.S2CKingFx.MARK, new int[] {style, body == null ? 0 : body.getId()}, data));
    }

    ServerPlayer owner() {
        return owner;
    }

    int tick() {
        return tick;
    }

    boolean finished() {
        return finished;
    }

    /** Advances one server tick; returns false once the session is over. */
    boolean step() {
        if (finished) return false;
        if (owner.isRemoved() || owner.serverLevel() != level) {
            finish(true);
            return false;
        }
        if (prelude < TurningModel.PRELUDE_TICKS) {
            turn();
            return true;
        }
        tick++;
        holdOwner();
        List<LivingEntity> nearby = nearby();
        List<DragonRig.Prey> prey = new ArrayList<>(nearby.size());
        for (LivingEntity entity : nearby) {
            boolean grabbable = entity.getBbWidth() <= 3.5F && entity.getBbHeight() <= 4.5F;
            prey.add(new DragonRig.Prey(entity.getId(), new V3(entity.getX(), entity.getY(), entity.getZ()), entity.getBbWidth(), entity.getBbHeight(), entity instanceof Player, grabbable));
        }
        for (DragonRig.Strike strike : rig.step(tick, prey, ground)) apply(strike, nearby);
        moveHeld();
        if (tick % 4 == 0) crush(nearby);
        carveBody();
        ambience();
        if (tick == DragonTimeline.RAMPAGE_END) {
            event(S2CDragonEvent.Kind.CRACK, rig.centroid(), 1.0F);
            play(rig.centroid(), HexKaguneSounds.BONE_CRACK, 10.0F, 0.45F);
        }
        if (tick == DragonTimeline.SHATTER_TICK) shatter();
        AwakeningNetwork.toWatchers(owner, S2CDragonFrame.capture(ownerId, rig, emergence));
        if (tick >= DragonTimeline.END_TICK) {
            finish(false);
            return false;
        }
        return true;
    }

    /** Vanilla's sounds by name, for the turning (the mod has no scream of its own). */
    private static final SoundEvent SCREAM = vanilla("entity.ghast.scream");
    private static final SoundEvent HURT = vanilla("entity.player.hurt");
    private static final SoundEvent ROAR = vanilla("entity.warden.roar");
    private static final SoundEvent HEARTBEAT = vanilla("entity.warden.heartbeat");
    private static final SoundEvent EXPLODE = vanilla("entity.generic.explode");

    private static SoundEvent vanilla(String name) {
        return SoundEvent.createVariableRangeEvent(new ResourceLocation("minecraft", name));
    }

    /**
     * 1.0.11: one tick of Kaneki turning, before the Dragon breaks out of him: held where he fell, whole and untouchable (as all through
     * the Dragon), his heart pounding faster and faster; he screams as the kagune tears out of his chest, and of his head, and at the
     * last; the bursts out of him throw blood and darkness about and everything near him off its feet. Every client is told how far in
     * it is ({@code PRELUDE}), so it draws him turning ({@code TurningModel}).
     */
    private void turn() {
        holdOwner();
        int t = prelude++;
        V3 chest = pin.add(0.0, 1.2, 0.0);
        if (t % 10 == 0) event(S2CDragonEvent.Kind.PRELUDE, chest, t);
        int beat = Math.max(7, 22 - t / 12);
        if (t % beat == 0) level.playSound(null, chest.x(), chest.y(), chest.z(), HEARTBEAT, SoundSource.PLAYERS, 2.6F, 0.75F + t / 600.0F);
        for (int[] scream : TurningModel.SCREAMS) {
            if (t == scream[0]) {
                boolean last = scream == TurningModel.SCREAMS[TurningModel.SCREAMS.length - 1];
                level.playSound(null, chest.x(), chest.y() + 0.5, chest.z(), SCREAM, SoundSource.PLAYERS, 3.0F, last ? 0.45F : 0.6F);
                level.playSound(null, chest.x(), chest.y() + 0.5, chest.z(), HURT, SoundSource.PLAYERS, 2.5F, 0.5F);
                if (last) level.playSound(null, chest.x(), chest.y(), chest.z(), ROAR, SoundSource.PLAYERS, 3.0F, 0.7F);
            }
        }
        for (int[] burst : TurningModel.BURSTS) {
            if (t == burst[0]) burst(chest, burst[1] / 10.0F, t >= TurningModel.HEAD_RIP ? pin.add(0.0, 1.75, 0.0) : chest);
        }
        // Blood off the wounds, and darkness gathering round him once the plates are over him.
        if (t >= TurningModel.CHEST_RIP && t % 4 == 0) {
            mark(com.hex.kagune.control.king.KingMarkFx.BLOOD, chest, new V3(Math.random() - 0.5, 0.3, Math.random() - 0.5), 0.35, null);
        }
        if (t >= TurningModel.HEAD_RIP && t % 6 == 3) {
            mark(com.hex.kagune.control.king.KingMarkFx.BLOOD, chest.add(0.0, 0.6, 0.0), new V3(Math.random() - 0.5, 0.5, Math.random() - 0.5), 0.3, null);
        }
        if (t >= 90 && t % 2 == 0) {
            double a = t * 0.37;
            double r = 2.6 - 1.2 * (t - 90) / 90.0;
            mark(com.hex.kagune.control.king.KingMarkFx.SOOT, new V3(pin.x() + Math.cos(a) * r, pin.y() + 0.3 + (t % 20) * 0.1, pin.z() + Math.sin(a) * r), new V3(0.0, 1.0, 0.0), 0.5, null);
        }
    }

    /** A violent burst out of him at {@code from}, {@code strength} (0..1) hard: blood, darkness, the sound of it, and everything near thrown. */
    private void burst(V3 chest, float strength, V3 from) {
        level.playSound(null, from.x(), from.y(), from.z(), EXPLODE, SoundSource.PLAYERS, 2.0F + 3.0F * strength, 0.9F - 0.35F * strength);
        play(from, HexKaguneSounds.MANIFEST_RIP, 4.0F + 4.0F * strength, 0.55F);
        play(from, HexKaguneSounds.BONE_CRACK, 3.0F + 3.0F * strength, 0.5F);
        play(from, HexKaguneSounds.FLESH_IMPACT, 3.0F, 0.45F);
        int count = (int) (40 + 120 * strength);
        mark(com.hex.kagune.control.king.KingMarkFx.GORE, from, new V3(Math.random() - 0.5, 0.6, Math.random() - 0.5), 1.0 + 1.5 * strength, null);
        mark(com.hex.kagune.control.king.KingMarkFx.SOOT, from, new V3(0.0, 1.0, 0.0), 1.0 + strength, null);
        mark(com.hex.kagune.control.king.KingMarkFx.FLARE, from, V3.ZERO, 1.0 + 1.5 * strength, null);
        double radius = 3.0 + 5.0 * strength;
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, new AABB(chest.x() - radius, chest.y() - radius, chest.z() - radius,
                chest.x() + radius, chest.y() + radius, chest.z() + radius), this::isPrey);
        for (LivingEntity entity : near) {
            double distance = center(entity).distance(chest);
            if (distance > radius) continue;
            double falloff = 1.0 - distance / radius;
            hurt(entity, (float) (2.0 + 6.0 * strength * falloff), chest, 0.8 + 1.8 * strength * falloff, 0.35 + 0.5 * strength * falloff);
        }
        event(S2CDragonEvent.Kind.BURST, from, strength);
    }

    /** Kaneki's human body is gone: keep the player invisible, still and safe inside the Dragon. */
    private void holdOwner() {
        V3 target = emergence != null ? emergence : pin;
        if (owner.isPassenger()) owner.stopRiding();
        owner.setNoGravity(true);
        owner.setDeltaMovement(Vec3.ZERO);
        owner.fallDistance = 0.0F;
        owner.clearFire();
        double dx = owner.getX() - target.x();
        double dy = owner.getY() - target.y();
        double dz = owner.getZ() - target.z();
        if (dx * dx + dy * dy + dz * dz > 0.04) owner.teleportTo(target.x(), target.y(), target.z());
        if (owner.getHealth() < owner.getMaxHealth()) owner.setHealth(owner.getMaxHealth());
    }

    private List<LivingEntity> nearby() {
        double[] box = rig.bounds(30.0);
        V3 centroid = rig.centroid();
        V3 lead = rig.lead();
        double range = DragonRig.DETECTION * DragonRig.DETECTION;
        List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, new AABB(box[0], box[1], box[2], box[3], box[4], box[5]), entity -> {
            if (held.containsKey(entity.getId())) return entity.isAlive();
            if (!isPrey(entity)) return false;
            V3 at = new V3(entity.getX(), entity.getY(), entity.getZ());
            return at.sub(centroid).dot(at.sub(centroid)) <= range || at.distance(lead) <= 40.0;
        });
        for (Held h : held.values()) if (h.entity.isAlive() && !found.contains(h.entity)) found.add(h.entity);
        return found;
    }

    /** Every living thing is prey except Kaneki, spectators, creative players and decorations. */
    private boolean isPrey(LivingEntity entity) {
        if (entity == owner || !entity.isAlive() || entity.isSpectator() || entity.isInvulnerable()) return false;
        if (entity instanceof ArmorStand || entity instanceof FakePlayer) return false;
        if (entity instanceof Player player && (player.isCreative() || DragonSessions.isAwakening(player.getUUID()))) return false;
        return true;
    }

    private LivingEntity byId(List<LivingEntity> nearby, int id) {
        for (LivingEntity entity : nearby) if (entity.getId() == id) return entity;
        return null;
    }

    private static V3 center(LivingEntity entity) {
        return new V3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ());
    }

    private void apply(DragonRig.Strike strike, List<LivingEntity> nearby) {
        V3 at = strike.position();
        switch (strike.kind()) {
            case ERUPTION -> {
                TerrainScars.of(level).carveSphere(owner, at.add(0.0, -1.0, 0.0), 7.0, at.y() - 6.0);
                for (LivingEntity entity : nearby) {
                    double distance = center(entity).distance(at);
                    if (distance <= strike.radius()) hurt(entity, strike.damage(), at, 2.2, 0.9);
                }
                play(at, HexKaguneSounds.MANIFEST_RIP, 10.0F, 0.5F);
                play(at, HexKaguneSounds.BONE_CRACK, 8.0F, 0.5F);
                play(at, HexKaguneSounds.KAGUNE_STRAIN, 8.0F, 0.45F);
                mark(com.hex.kagune.control.king.KingMarkFx.QUAKE, at, V3.ZERO, 3.0, null);
                mark(com.hex.kagune.control.king.KingMarkFx.GORE, at.add(0.0, 2.0, 0.0), new V3(0.0, 1.0, 0.0), 2.5, null);
                event(S2CDragonEvent.Kind.ERUPTION, at, 1.0F);
            }
            case BITE -> {
                for (LivingEntity entity : nearby) {
                    if (!held.containsKey(entity.getId()) && center(entity).distance(at) <= strike.radius() + entity.getBbWidth() * 0.5) {
                        hurt(entity, strike.damage(), rig.headRoot(rig.heads[strike.source()]), 1.6, 0.5);
                    }
                }
                TerrainScars.of(level).carveSphere(owner, at, 2.8, at.y() - 3.0);
                play(at, HexKaguneSounds.BONE_CRUSH, 5.0F, 0.6F);
                play(at, HexKaguneSounds.FLESH_IMPACT, 4.0F, 0.5F);
                mark(com.hex.kagune.control.king.KingMarkFx.GORE, at, new V3(0.0, 1.0, 0.0), 1.5, null);
                event(S2CDragonEvent.Kind.BITE, at, 0.6F);
            }
            case LASH -> {
                for (LivingEntity entity : nearby) {
                    if (held.containsKey(entity.getId())) continue;
                    Integer previous = lashHits.get(entity.getId());
                    if (previous != null && previous == strike.serial()) continue;
                    if (center(entity).distance(at) <= strike.radius() + entity.getBbWidth() * 0.5) {
                        lashHits.put(entity.getId(), strike.serial());
                        hurt(entity, strike.damage(), rig.tentacleRoot(rig.tentacles[strike.source()]), 1.9, 0.6);
                    }
                }
                if (at.y() - ground.surface(at.x(), at.z()) < 1.5) TerrainScars.of(level).carveSphere(owner, at, 1.9, at.y() - 2.0);
                if (lashSounds[strike.source()] != strike.serial()) {
                    lashSounds[strike.source()] = strike.serial();
                    play(at, HexKaguneSounds.KAGUNE_WHIP, 5.0F, 0.55F);
                    event(S2CDragonEvent.Kind.LASH, at, 0.3F);
                }
            }
            case GRAB -> {
                LivingEntity entity = byId(nearby, strike.targetId());
                if (entity == null || held.containsKey(entity.getId())) {
                    rig.dropHeld(strike.targetId());
                    return;
                }
                held.put(entity.getId(), new Held(entity, entity.isNoGravity(), entity instanceof Mob mob && mob.isNoAi()));
                play(at, HexKaguneSounds.KAGUNE_GRAB, 4.0F, 0.6F);
                event(S2CDragonEvent.Kind.GRAB, at, 0.3F);
            }
            case SQUEEZE -> {
                Held h = held.get(strike.targetId());
                if (h != null) {
                    hurt(h.entity, strike.damage(), at, 0.0, 0.0);
                    play(at, HexKaguneSounds.BONE_CRACK, 2.5F, 0.7F);
                }
            }
            case SLAM -> {
                Held h = held.get(strike.targetId());
                if (h != null) {
                    h.entity.teleportTo(at.x(), at.y(), at.z());
                    hurt(h.entity, strike.damage(), at.add(0.0, 4.0, 0.0), 0.0, -0.4);
                    release(h);
                }
                for (LivingEntity entity : nearby) {
                    if (h != null && entity == h.entity) continue;
                    if (!held.containsKey(entity.getId()) && center(entity).distance(at) <= strike.radius()) hurt(entity, DragonRig.SLAM_SPLASH_DAMAGE, at, 1.4, 0.7);
                }
                TerrainScars.of(level).carveSphere(owner, at, 4.2, at.y() - 3.5);
                play(at, HexKaguneSounds.KAGUNE_SLAM, 7.0F, 0.5F);
                play(at, HexKaguneSounds.BONE_CRUSH, 5.0F, 0.55F);
                mark(com.hex.kagune.control.king.KingMarkFx.QUAKE, at, V3.ZERO, 2.5, null);
                event(S2CDragonEvent.Kind.SLAM, at, 0.8F);
            }
            case RELEASE -> {
                Held h = held.get(strike.targetId());
                if (h != null) release(h);
            }
            case SHOCKWAVE -> {
                for (LivingEntity entity : nearby) {
                    if (held.containsKey(entity.getId())) continue;
                    double distance = center(entity).distance(at);
                    if (distance <= strike.radius()) {
                        double falloff = 1.0 - distance / strike.radius();
                        hurt(entity, (float) (strike.damage() * (0.5 + 0.5 * falloff)), at, 1.0 + 2.6 * falloff, 0.4 + 0.6 * falloff);
                    }
                }
            }
        }
    }

    /** Massive damage that ignores the usual half-second of hurt immunity, with an optional shove away from {@code from}. */
    private void hurt(LivingEntity target, float amount, V3 from, double push, double lift) {
        if (!target.isAlive()) return;
        target.invulnerableTime = 0;
        boolean hit = target.hurt(owner.damageSources().playerAttack(owner), amount);
        if (!hit) return;
        if (!target.isAlive()) {
            play(center(target), HexKaguneSounds.CRUSH_KILL, 1.5F, 0.8F);
            mark(com.hex.kagune.control.king.KingMarkFx.GORE, center(target), center(target).sub(center(owner)).withY(0.0).normalize().add(0.0, 0.5, 0.0), 1.3, null);
            return;
        }
        if (push > 0.0 || lift != 0.0) {
            V3 away = center(target).sub(from).withY(0.0).normalize();
            target.setDeltaMovement(away.x() * push, lift, away.z() * push);
            target.hurtMarked = true;
            syncMotion(target);
        }
    }

    private static void syncMotion(LivingEntity target) {
        if (target instanceof ServerPlayer player && player.connection != null) player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    private void moveHeld() {
        for (DragonRig.Tentacle tentacle : rig.tentacles) {
            if (tentacle.heldId == 0) continue;
            Held h = held.get(tentacle.heldId);
            if (h == null) continue;
            LivingEntity entity = h.entity;
            if (!entity.isAlive() || entity.isRemoved() || entity.level() != level) {
                rig.dropHeld(tentacle.heldId);
                release(h);
                continue;
            }
            entity.setNoGravity(true);
            if (entity instanceof Mob mob) mob.setNoAi(true);
            if (entity instanceof Creeper creeper) creeper.setSwellDir(-1);
            V3 tip = tentacle.tip.add(0.0, -entity.getBbHeight() * 0.5, 0.0);
            if (entity instanceof ServerPlayer player) {
                player.teleportTo(tip.x(), tip.y(), tip.z());
            } else {
                Vec3 move = new Vec3(tip.x() - entity.getX(), tip.y() - entity.getY(), tip.z() - entity.getZ());
                entity.move(MoverType.SELF, move);
                if (new V3(entity.getX(), entity.getY(), entity.getZ()).distance(tip) > 2.0) entity.teleportTo(tip.x(), tip.y(), tip.z());
            }
            entity.setDeltaMovement(Vec3.ZERO);
            entity.fallDistance = 0.0F;
            entity.hasImpulse = true;
        }
    }

    private void release(Held h) {
        held.remove(h.entity.getId());
        LivingEntity entity = h.entity;
        entity.setNoGravity(h.noGravity);
        if (entity instanceof Mob mob) mob.setNoAi(h.noAi);
        if (entity instanceof Creeper creeper) creeper.setSwellDir(-1);
        entity.fallDistance = 0.0F;
        entity.hurtMarked = true;
        syncMotion(entity);
    }

    /** Anything inside the body is crushed; anything in its shadow is pressed into the ground. */
    private void crush(List<LivingEntity> nearby) {
        for (LivingEntity entity : nearby) {
            if (held.containsKey(entity.getId())) continue;
            V3 c = center(entity);
            double half = entity.getBbWidth() * 0.5;
            boolean crushed = false;
            for (int i = 0; i + 1 < DragonRig.SEGMENTS; i++) {
                double r = Math.max(rig.radius(i), rig.radius(i + 1));
                if (r < 0.5) continue;
                double d = c.distanceToSegment(rig.segment(i), rig.segment(i + 1));
                if (d <= r + half) {
                    V3 axis = rig.segment(i).lerp(rig.segment(i + 1), 0.5);
                    hurt(entity, INSIDE_CRUSH, axis, 1.2, -0.2);
                    crushed = true;
                    break;
                }
            }
            if (crushed) continue;
            double top = entity.getY() + entity.getBbHeight();
            for (int i = 0; i < DragonRig.SEGMENTS; i++) {
                double r = rig.radius(i);
                if (r < 0.5) continue;
                V3 s = rig.segment(i);
                double horizontal = new V3(s.x() - entity.getX(), 0.0, s.z() - entity.getZ()).length();
                double gap = (s.y() - r) - top;
                if (horizontal > r * 0.9 + half || gap < -1.0) continue;
                if (gap <= 8.0) {
                    hurt(entity, PRESSED_CRUSH, s, 0.0, -0.6);
                    break;
                }
                if (gap <= 20.0) {
                    hurt(entity, SHADOW_CRUSH, s, 0.0, -0.3);
                    break;
                }
            }
        }
    }

    /**
     * The body ploughs a trench through whatever lies in its path: trees, buildings and the ground it
     * is pressed into. Each segment carves again after every two blocks it travels; the segments
     * behind follow the same path, so only the front breaks new ground and the per-tick cost stays low.
     */
    private void carveBody() {
        TerrainScars scars = TerrainScars.of(level);
        // Start at a different segment every tick so a spent budget never starves the same part of the body.
        int start = Math.floorMod(tick, DragonRig.SEGMENTS);
        for (int n = 0; n < DragonRig.SEGMENTS; n++) {
            int i = (start + n) % DragonRig.SEGMENTS;
            double r = rig.radius(i);
            if (r < 1.2) continue;
            V3 s = rig.segment(i);
            double surface = ground.surface(s.x(), s.z());
            double land = ground.terrain(s.x(), s.z());
            if (s.y() - r > surface + 0.5 || s.y() + r < land - 10.0) continue;
            if (lastCarve[i] != null && lastCarve[i].distance(s) < 2.0) continue;
            lastCarve[i] = s;
            scars.carveSphere(owner, s, r * 0.92, land - 10.0);
        }
    }

    private void ambience() {
        if (tick > 0 && tick < DragonTimeline.RAMPAGE_END && tick % 70 == 0) {
            V3 c = rig.centroid();
            play(c, HexKaguneSounds.KAGUNE_STRAIN, 9.0F, 0.42F + (tick % 3) * 0.05F);
            play(rig.lead(), HexKaguneSounds.CENTIPEDE_CLICK, 7.0F, 0.5F);
        }
        if (tick > DragonTimeline.RAMPAGE_END && tick < DragonTimeline.SHATTER_TICK && tick % 6 == 0) {
            V3 at = rig.segment((tick * 7) % DragonRig.SEGMENTS);
            play(at, HexKaguneSounds.BONE_CRACK, 6.0F, (float) (0.5 + DragonTimeline.instability(tick) * 0.4));
            event(S2CDragonEvent.Kind.CRACK, at, (float) DragonTimeline.instability(tick));
        }
    }

    /** Solid footing for Kaneki where the body collapsed, never on top of water or lava. */
    private V3 footing() {
        TerrainScars scars = TerrainScars.of(level);
        V3 near = rig.emergence(ground);
        scars.restoreNear(near, 7.0);
        V3 spot = ground.solidNear(near, 18);
        if (spot == null) return pin;
        scars.restoreNear(spot, 6.0);
        // Re-read the column now that the holes around it are closed.
        return spot.withY(ground.rescan(spot.x(), spot.z()));
    }

    private void shatter() {
        emergence = footing();
        V3 at = emergence;
        for (int i = 0; i < DragonRig.SEGMENTS; i += 4) {
            V3 s = rig.segment(i);
            mark(i % 8 == 0 ? com.hex.kagune.control.king.KingMarkFx.GORE : com.hex.kagune.control.king.KingMarkFx.SOOT, s, new V3(Math.random() - 0.5, 0.5, Math.random() - 0.5), 2.0, null);
        }
        play(at, HexKaguneSounds.KAGUNE_SLAM, 12.0F, 0.45F);
        play(at, HexKaguneSounds.BONE_CRUSH, 12.0F, 0.5F);
        play(at, HexKaguneSounds.MANIFEST_RIP, 12.0F, 0.6F);
        owner.teleportTo(at.x(), at.y(), at.z());
        FinalKakuja.unlock(owner);
        event(S2CDragonEvent.Kind.SHATTER, at, 1.0F);
        TerrainScars.of(level).hastenAll(400L);
    }

    /** Ends the awakening; {@code forced} covers disconnects, dimension changes and shutdown. */
    void finish(boolean forced) {
        if (finished) return;
        finished = true;
        for (Held h : new ArrayList<>(held.values())) release(h);
        held.clear();
        if (emergence == null) emergence = footing();
        TerrainScars.of(level).hastenAll(forced ? 200L : 400L);
        if (owner.serverLevel() == level) owner.teleportTo(emergence.x(), emergence.y(), emergence.z());
        owner.setNoGravity(false);
        owner.setDeltaMovement(Vec3.ZERO);
        owner.fallDistance = 0.0F;
        owner.clearFire();
        FinalKakuja.unlock(owner);
        FinalKakuja.emerge(owner, !forced);
        AwakeningNetwork.toWatchers(owner, new S2CDragonEvent(ownerId, S2CDragonEvent.Kind.END, emergence.x(), emergence.y(), emergence.z(), forced ? 0.0F : 1.0F));
    }

    private void event(S2CDragonEvent.Kind kind, V3 at, float magnitude) {
        AwakeningNetwork.toWatchers(owner, new S2CDragonEvent(ownerId, kind, at.x(), at.y(), at.z(), magnitude));
    }

    private void play(V3 at, RegistryObject<?> sound, float volume, float pitch) {
        level.playSound(null, at.x(), at.y(), at.z(), (SoundEvent) sound.get(), SoundSource.HOSTILE, volume, pitch);
    }
}
