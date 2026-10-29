package com.hex.kagune.control.awakening;

import com.hex.kagune.control.king.KingCombo;
import com.hex.kagune.control.server.KingEffects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.0.20: the fighting grammar's moves on bodies ({@link KingCombo}), the same for War's kit and Peace's: a body launched and him going
 * up after it; air blows keeping a body floating in front of him while he hangs there with it; the spike driving it down, him plunging
 * after, and the crater where it lands.
 */
final class KingCombos {
    /** A body spiked into the ground: by whom, until when it is watched for its landing, how hard. */
    private record Spiked(int body, long until, double power) {
    }

    private static final Map<UUID, List<Spiked>> SPIKED = new HashMap<>();

    private KingCombos() {
    }

    /** {@code body} set moving at (x, y, z) blocks a tick, its own client told at once if it is a player. */
    static void motion(Entity body, double x, double y, double z) {
        body.setDeltaMovement(new Vec3(x, y, z));
        body.hurtMarked = true;
        if (body instanceof ServerPlayer player && player.connection != null) player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    /** The way from {@code player} to {@code body}, level (unit), or {@code fallback}. */
    static V3 toward(ServerPlayer player, Entity body, V3 fallback) {
        V3 to = new V3(body.getX() - player.getX(), 0.0, body.getZ() - player.getZ());
        return to.length() < 1.0E-4 ? fallback : to.normalize();
    }

    /** An air blow landed on {@code body}: it floats up a little, carried on along the blow; he hangs where he is, his fall forgotten. */
    static void air(ServerPlayer player, LivingEntity body, V3 facing) {
        double steady = 1.0 - 0.75 * KingEffects.steady(body);
        motion(body, facing.x() * 0.12 * steady, KingCombo.FLOAT_UP, facing.z() * 0.12 * steady);
        motion(player, 0.0, KingCombo.HANG_UP, 0.0); // 1.0.36: (not his last speed: the server's idea of it is stale)
        player.fallDistance = 0.0F;
        KingEffects.stun(body, KingCombo.AIR_STUN);
    }

    /** The launcher landed on {@code body}: it goes up high; he goes up after it. */
    static void launch(ServerPlayer player, LivingEntity body, V3 facing) {
        double steady = 1.0 - 0.6 * KingEffects.steady(body);
        motion(body, facing.x() * 0.1, KingCombo.LAUNCH_UP * steady, facing.z() * 0.1);
        V3 to = new V3(body.getX() - player.getX(), 0.0, body.getZ() - player.getZ());
        double d = to.length();
        V3 lean = d > 1.0E-3 ? to.scale(Math.min(0.32, d * 0.1) / d) : V3.ZERO;
        motion(player, lean.x(), KingCombo.CHASE_UP, lean.z());
        player.fallDistance = 0.0F;
        KingEffects.stun(body, KingCombo.LAUNCH_STUN);
    }

    /** The spike landed on {@code body}: it is driven down into the ground, he plunging after it; where it lands the ground breaks. */
    static void spike(ServerPlayer player, LivingEntity body, V3 facing, double power, long now) {
        motion(body, facing.x() * 0.2, -KingCombo.SPIKE_DOWN, facing.z() * 0.2);
        motion(player, 0.0, -KingCombo.PLUNGE_DOWN, 0.0);
        player.fallDistance = 0.0F;
        KingEffects.stun(body, 30);
        SPIKED.computeIfAbsent(player.getUUID(), id -> new ArrayList<>()).add(new Spiked(body.getId(), now + 40L, power));
    }

    /** A body {@code player} spiked that has come down this tick (where its feet are), and how hard it was spiked. */
    record Landing(LivingEntity body, V3 at, double power) {
    }

    /** What {@code player} spiked that hit the ground this tick (each once; given up on after a while). */
    static List<Landing> landed(ServerPlayer player, long now) {
        List<Spiked> spiked = SPIKED.get(player.getUUID());
        if (spiked == null || spiked.isEmpty()) return List.of();
        List<Landing> out = new ArrayList<>();
        double r = 48.0;
        List<LivingEntity> near = player.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(player.getX() - r, player.getY() - r, player.getZ() - r,
                player.getX() + r, player.getY() + r, player.getZ() + r), b -> b != player);
        for (Iterator<Spiked> it = spiked.iterator(); it.hasNext(); ) {
            Spiked s = it.next();
            LivingEntity body = null;
            for (LivingEntity b : near) if (b.getId() == s.body()) body = b;
            if (body == null || !body.isAlive() || now > s.until()) {
                it.remove();
                continue;
            }
            if (KingEffects.grounded(body) && now > s.until() - 38L) {
                it.remove();
                out.add(new Landing(body, new V3(body.getX(), body.getY(), body.getZ()), s.power()));
            }
        }
        return out;
    }

    /** It came off, or he died: nothing he spiked is watched any more. */
    static void forget(ServerPlayer player) {
        SPIKED.remove(player.getUUID());
    }
}
