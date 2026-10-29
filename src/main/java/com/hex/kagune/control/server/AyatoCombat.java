package com.hex.kagune.control.server;

import com.hex.kagune.control.ComboStage;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.sound.HexKaguneSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * What everyone sees and hears of Ayato's close-range combo (server-sent): a light whoosh for kicks, a glassy cut for
 * wing blades and the shard crack of a point-blank burst, plus the RC each strike leaves hanging in the air for a
 * moment: a red arc where a wing cut, molten streaks where shards flew, sparks where a kick landed.
 */
final class AyatoCombat {
    /** Crystallized blade red (the wing's FLAME_RED). */
    private static final DustParticleOptions BLADE = new DustParticleOptions(new Vector3f(0.89F, 0.09F, 0.11F), 1.25F);
    /** Molten shard orange (the wing's MOLTEN core). */
    private static final DustParticleOptions SHARD = new DustParticleOptions(new Vector3f(1.0F, 0.35F, 0.08F), 1.0F);

    private AyatoCombat() {
    }

    /** The swing sound as a strike starts; hits use the shared flesh impact. */
    static void swing(ServerPlayer player, ComboStage stage) {
        float weight = stage.weight();
        switch (stage.pattern()) {
            case KICK -> KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.45F,
                KaguneServerController.vary(player, 1.6F, 0.08F));
            case STEEL_SWEEP, STEEL_RETURN, STEEL_EXECUTION -> {
                // A heavy blade through the air, and the ring of steel.
                net.minecraft.world.phys.Vec3 at = player.m_146892_();
                AyatoFire.sound(player.m_284548_(), at, "entity.player.attack.sweep", 1.0F,
                    KaguneServerController.vary(player, stage.pattern() == com.hex.kagune.control.StrikePattern.STEEL_EXECUTION ? 0.6F : 0.75F, 0.05F));
                AyatoFire.sound(player.m_284548_(), at, "item.trident.throw", 0.7F, KaguneServerController.vary(player, 0.7F, 0.05F));
            }
            case FLAME_SHOT -> KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.3F,
                KaguneServerController.vary(player, 1.75F, 0.08F));
            case RABBIT_HOP, FLARE, RISING_FLAME, FIRE_FAN, RABBIT_DASH, BLACK_RABBIT -> KaguneServerController.playSelf(player,
                HexKaguneSounds.KAGUNE_WHIP, 0.55F, KaguneServerController.vary(player, 1.15F, 0.06F));
            case CRYSTAL_SHOT -> KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.25F,
                KaguneServerController.vary(player, 1.9F, 0.08F));
            case NEEDLE_VOLLEY, NEEDLE_PAIR, NEEDLE_FAN, CROSSFIRE -> KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.2F,
                KaguneServerController.vary(player, 2.0F, 0.1F));
            case FIRE_LASH, FIREBALL -> KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.3F,
                KaguneServerController.vary(player, 1.6F, 0.08F));
            case HARROW, PIERCING_LINE, THORN_FAN, HAIL_OF_THORNS, SIX_TAIL_BARRAGE, CRIMSON_BLOOM, EMBER_HOP, SEARING_HARROW, TWIN_SERPENTS,
                 PYRE_LANCE, HELLRAIN, CRIMSON_PYRE -> KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.6F,
                KaguneServerController.vary(player, 1.0F, 0.06F));
            case CRYSTAL_LANCE, SPIKE_FAN, SPIKE_RAIN, SPIKE_BARRAGE, CRYSTAL_BLOOM -> KaguneServerController.playSelf(player,
                HexKaguneSounds.KAGUNE_WHIP, 0.5F, KaguneServerController.vary(player, 1.35F, 0.06F));
            case SHARD_BURST -> KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.4F,
                KaguneServerController.vary(player, 1.35F, 0.05F));
            default -> KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_WHIP, 0.7F + 0.35F * weight,
                KaguneServerController.vary(player, 1.3F - 0.4F * weight, 0.05F));
        }
    }

    /** RC left in the air at the impact tick, hit or miss. */
    static void impact(ServerPlayer player, ComboStage stage, Vec3 direction, Vec3 impactPoint, double range) {
        ServerLevel level = player.m_284548_();
        Vec3 eye = player.m_146892_();
        double[] forward = horizontal(direction, player.m_20154_());
        double forwardX = forward[0];
        double forwardZ = forward[1];
        double rightX = -forwardZ;
        double rightZ = forwardX;
        double radius = Math.max(1.6, range * 0.7);
        boolean right = stage.usesLimb(0);
        switch (stage.pattern()) {
            case KICK -> level.m_8767_(ParticleTypes.f_123797_, impactPoint.f_82479_, impactPoint.f_82480_, impactPoint.f_82481_, 7,
                0.2, 0.2, 0.2, 0.3);
            case SHARD_BURST -> {
                KaguneServerController.playSelf(player, HexKaguneSounds.SHARD_LAUNCH, 0.9F, KaguneServerController.vary(player, 1.05F, 0.05F));
                // Shards leave from the shoulder blades: both wings, or only the left one in Rabbit Guise.
                double y = eye.f_82480_ - 0.25;
                streak(level, eye.f_82479_ - rightX * 0.35 - forwardX * 0.15, y, eye.f_82481_ - rightZ * 0.35 - forwardZ * 0.15, impactPoint);
                if (right) {
                    streak(level, eye.f_82479_ + rightX * 0.35 - forwardX * 0.15, y, eye.f_82481_ + rightZ * 0.35 - forwardZ * 0.15,
                        impactPoint);
                }
                level.m_8767_(ParticleTypes.f_123797_, impactPoint.f_82479_, impactPoint.f_82480_, impactPoint.f_82481_, 8, 0.25, 0.25,
                    0.25, 0.35);
            }
            // A wing blade cuts down and across from its own shoulder.
            case WING_SLASH -> arc(level, eye, forwardX, forwardZ, rightX, rightZ, radius, stage.shapeParam(), -0.35, right ? 0.4 : -0.4, 9);
            case WING_CROSS -> {
                arc(level, eye, forwardX, forwardZ, rightX, rightZ, radius, stage.shapeParam(), -0.35, 0.4, 9);
                arc(level, eye, forwardX, forwardZ, rightX, rightZ, radius, stage.shapeParam(), -0.35, -0.4, 9);
            }
            case ONE_WING_SLASH, ONE_WING_RETURN -> arc(level, eye, forwardX, forwardZ, rightX, rightZ, radius, stage.shapeParam(), -0.3,
                0.0, 13);
            // The beheading cut passes at neck height.
            case BEHEADING -> arc(level, eye, forwardX, forwardZ, rightX, rightZ, radius, stage.shapeParam(), -0.1, 0.0, 15);
            // The Steel Blade's swings leave no dust: the blade's own trail shows the cut, and blood where it lands (BladeBlood).
            default -> {
            }
        }
    }

    /** Unit horizontal forward from the strike direction (falls back to the look direction, then to +Z). */
    static double[] horizontal(Vec3 direction, Vec3 look) {
        for (Vec3 candidate : new Vec3[] {direction, look}) {
            double length = Math.sqrt(candidate.f_82479_ * candidate.f_82479_ + candidate.f_82481_ * candidate.f_82481_);
            if (length > 1.0E-4) {
                return new double[] {candidate.f_82479_ / length, candidate.f_82481_ / length};
            }
        }
        return new double[] {0.0, 1.0};
    }

    /** Dust along a horizontal arc around the eye: {@code tilt} raises the right side (+) or the left side (-). */
    private static void arc(ServerLevel level, Vec3 eye, double forwardX, double forwardZ, double rightX, double rightZ, double radius,
                            float degrees, double drop, double tilt, int points) {
        double half = Math.toRadians(Math.max(20.0F, Math.min(200.0F, degrees)) * 0.5);
        for (int index = 0; index < points; index++) {
            double angle = -half + 2.0 * half * index / (points - 1);
            double side = Math.sin(angle);
            double ahead = Math.cos(angle);
            double x = eye.f_82479_ + (forwardX * ahead + rightX * side) * radius;
            double y = eye.f_82480_ + drop + tilt * side;
            double z = eye.f_82481_ + (forwardZ * ahead + rightZ * side) * radius;
            level.m_8767_(BLADE, x, y, z, 2, 0.06, 0.06, 0.06, 0.0);
            if (index % 3 == 1) {
                level.m_8767_(ParticleTypes.f_123797_, x, y, z, 1, 0.05, 0.05, 0.05, 0.05);
            }
        }
    }

    /** Molten dust from a shoulder blade to the impact point. */
    private static void streak(ServerLevel level, double x, double y, double z, Vec3 to) {
        int points = 7;
        for (int index = 1; index <= points; index++) {
            double t = index / (double)points;
            level.m_8767_(SHARD, x + (to.f_82479_ - x) * t, y + (to.f_82480_ - y) * t, z + (to.f_82481_ - z) * t, 1, 0.03, 0.03, 0.03, 0.0);
        }
    }
}
