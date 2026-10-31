package com.hex.kagune.control.client;

import com.hex.kagune.control.client.animation.ArmPose;
import com.hex.kagune.control.pale.ArmRules;
import com.hex.kagune.control.network.S2CPaleArm;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 6.2.0: the client's store of every Pale player's Mutated Arm (fed only by the server, {@link S2CPaleArm}), timed on
 * {@link PaleClient#clock}; and each tick, the way each arm points turned after where its player looks, lagging as the server's
 * does ({@link ArmRules#follow}), so every client swings it alike.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class ArmClient {
    /** One arm as the server last said, the clock when that began here, and the way it points now (and a tick ago). */
    public static final class Arm {
        public final S2CPaleArm said;
        public final ArmRules.Phase phase;
        public final double began;
        /** Ticks the charge had counted when this began. */
        public final double heldBefore;
        double[] aim;
        double[] lastAim;

        Arm(S2CPaleArm said, double began, double heldBefore) {
            this.said = said;
            this.phase = ArmRules.Phase.byOrdinal(said.phase());
            this.began = began;
            this.heldBefore = heldBefore;
            this.aim = new double[] {said.aimX(), said.aimY(), said.aimZ()};
            this.lastAim = this.aim.clone();
        }

        /** Ticks into what it is doing, at {@code clock}. */
        public double age(double clock) {
            return Math.max(0.0, clock - this.began);
        }

        /** Whether it draws back in after the sphere (it was made: the server said where). */
        public boolean afterSphere() {
            return this.said.x() != 0.0 || this.said.y() != 0.0 || this.said.z() != 0.0;
        }

        /** How far out, how thick, at {@code clock}. */
        public ArmPose.Reach reach(double clock) {
            return ArmPose.reach(this.phase, this.age(clock), this.said.length(), this.said.charge(), this.heldBefore, this.said.caught().length > 0,
                this.afterSphere());
        }

        /** The way it points {@code partial} of the way through this tick. */
        public double[] aim(float partial) {
            double[] a = {this.lastAim[0] + (this.aim[0] - this.lastAim[0]) * partial, this.lastAim[1] + (this.aim[1] - this.lastAim[1]) * partial,
                this.lastAim[2] + (this.aim[2] - this.lastAim[2]) * partial};
            double l = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
            return l < 1.0E-9 ? this.aim.clone() : new double[] {a[0] / l, a[1] / l, a[2] / l};
        }
    }

    private static final Map<UUID, Arm> ARMS = new HashMap<>();

    private ArmClient() {
    }

    public static void accept(S2CPaleArm message) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (message.phase() == ArmRules.Phase.NONE.ordinal()) {
            ARMS.remove(message.playerId());
            return;
        }
        long now = minecraft.f_91073_ == null ? message.startedAt() : minecraft.f_91073_.m_46467_();
        long behind = now - message.startedAt();
        double began = PaleClient.clock(0.0F) - (behind > PaleClient.JUST_BEGUN || behind < 0L ? behind : 0L);
        Arm before = ARMS.get(message.playerId());
        Arm arm = new Arm(message, began, message.startedAt() - message.downAt());
        if (before != null && before.said.startedAt() == message.startedAt() && before.said.phase() == message.phase()) {
            // The same, told again (it caught something): its clock and its swing run on.
            arm = new Arm(message, before.began, before.heldBefore);
            arm.aim = before.aim;
            arm.lastAim = before.lastAim;
        } else if (before != null && before.phase != ArmRules.Phase.SPHERE) {
            // On from where it was swinging (after the sphere, from the sphere where it is now, as the server has it).
            arm.aim = before.aim;
            arm.lastAim = before.lastAim;
        }
        ARMS.put(message.playerId(), arm);
    }

    /** {@code player}'s arm, or null when it is in her. 1.3.6: anyone's, by their UUID (a player's is their profile's; Eto's is hers). */
    public static Arm get(net.minecraft.world.entity.Entity player) {
        return player == null ? null : ARMS.get(player.m_20148_());
    }

    public static boolean out(net.minecraft.world.entity.Entity player) {
        return get(player) != null;
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (event.phase != TickEvent.Phase.END || ARMS.isEmpty() || minecraft.f_91073_ == null || minecraft.m_91104_()) {
            return;
        }
        for (Player player : minecraft.f_91073_.m_6907_()) {
            Arm arm = ARMS.get(player.m_36316_().getId());
            if (arm == null) {
                continue;
            }
            arm.lastAim = arm.aim;
            if (arm.phase == ArmRules.Phase.SHOOT || arm.phase == ArmRules.Phase.HOLD || arm.phase == ArmRules.Phase.DRAG) {
                Vec3 look = player.m_20154_();
                arm.aim = ArmRules.follow(arm.aim, new double[] {look.f_82479_, look.f_82480_, look.f_82481_});
            }
            if (arm.phase != ArmRules.Phase.RETRACT) {
                // Its weight squares her body up to it (her head looks where she likes).
                player.f_20883_ = ArmPose.squared(player.f_20883_, arm.aim);
            }
        }
        // 1.3.7: Eto's arm swings after where she looks, as a player's does.
        for (int id : EtoClient.ids()) {
            net.minecraft.world.entity.Entity her = minecraft.f_91073_.m_6815_(id);
            Arm arm = her == null ? null : ARMS.get(her.m_20148_());
            if (arm == null) {
                continue;
            }
            arm.lastAim = arm.aim;
            if (arm.phase == ArmRules.Phase.SHOOT || arm.phase == ArmRules.Phase.HOLD || arm.phase == ArmRules.Phase.DRAG) {
                Vec3 look = her.m_20154_();
                arm.aim = ArmRules.follow(arm.aim, new double[] {look.f_82479_, look.f_82480_, look.f_82481_});
            }
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ARMS.clear();
    }
}
