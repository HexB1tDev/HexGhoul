package com.hex.kagune.control.server;

import com.hex.kagune.control.StrikePattern;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CUkakuFx;
import com.hex.kagune.gameplay.KaguneServerController;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 5.0.20: blood from the Steel Blade. Every body its swing lands on is cut where the blade crossed it, the blade going the
 * swing's way (the sweep from his right across to his left, the backhand the other way, the execution down across him), and every
 * client near it is told ({@link S2CUkakuFx#BLADE_HIT}): blood flung along the cut, the blade bloodied, and on a kill a burst,
 * gushes from the body as it falls and a pool under it.
 */
final class BladeBlood {
    private BladeBlood() {
    }

    /** {@code player}'s Steel Blade swing {@code pattern} (aimed along {@code direction}) landed on {@code target} for {@code damage}. */
    static void cut(ServerPlayer player, LivingEntity target, StrikePattern pattern, Vec3 direction, float damage) {
        if (!(target instanceof Mob) && !(target instanceof Player)) {
            return; // an armor stand: wood, nothing in it
        }
        double[] forward = UkakuCombat.horizontal(direction, player.m_20154_());
        Vec3 ahead = new Vec3(forward[0], 0.0, forward[1]);
        Vec3 right = new Vec3(-forward[1], 0.0, forward[0]);
        Vec3 way = switch (pattern) {
            case STEEL_RETURN -> right.m_82549_(ahead.m_82490_(0.25));
            case STEEL_EXECUTION -> new Vec3(0.0, -1.0, 0.0).m_82549_(ahead.m_82490_(0.45)).m_82546_(right.m_82490_(0.3));
            default -> right.m_82490_(-1.0).m_82549_(ahead.m_82490_(0.25));
        };
        way = way.m_82541_();
        // Where the blade crossed it: through its middle, on the side facing him.
        Vec3 centre = KaguneServerController.center(target);
        Vec3 at = centre.m_82546_(ahead.m_82490_(target.m_20205_() * 0.45));
        boolean killed = !target.m_6084_();
        float power = Math.max(0.5F, Math.min(2.5F, damage / 5.0F)) * (killed ? 1.4F : 1.0F);
        ControlNetwork.broadcastAround(target, new S2CUkakuFx(S2CUkakuFx.BLADE_HIT, player.m_19879_(), target.m_19879_(), at.f_82479_, at.f_82480_,
            at.f_82481_, (float)way.f_82479_, (float)way.f_82480_, (float)way.f_82481_, power, killed ? 1 : 0));
    }
}
