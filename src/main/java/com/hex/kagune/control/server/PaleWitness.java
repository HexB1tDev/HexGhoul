package com.hex.kagune.control.server;

import com.hex.kagune.control.pale.PaleMove;
import com.hex.kagune.control.pale.PaleMutation;
import com.hex.kagune.control.pale.PaleRules;
import com.hex.kagune.control.pale.WitnessRules;
import com.hex.kagune.control.network.S2CPaleFx;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.network.KaguneNetwork;
import com.hex.kagune.sound.HexKaguneSounds;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 6.2.0, Witness This ({@link PaleMutation#WITNESS_THIS}): a fully charged Big Punch that lands goes on into the combo
 * {@link WitnessRules} lays out, here on the server: what she hit is held frozen on its path every tick (stunned: it can't hurt
 * anything; a mob is set where it must be; a player's own client carries them there, put back only if they stray, so they can still
 * look round; no kagune of theirs answers meanwhile, {@link #frozen}), each blow lands on its tick (the uppercut, the blitz from every
 * side, the kick down), and where it comes down a crater is knocked out round it and the shock throws everything near up and away.
 */
final class PaleWitness {
    /** Frozen bodies, by entity id: the game tick they are free again. */
    private static final Map<Integer, Long> FROZEN = new HashMap<>();

    private PaleWitness() {
    }

    /** Whether {@code player} has it. */
    static boolean has(ServerPlayer player) {
        // 7.0.1: and the eyes alone's, once its quest is done.
        return PaleRevive.hasMutation(player, PaleMutation.WITNESS_THIS)
            || PaleServer.eyes(player) && QuestServer.open(player, com.hex.kagune.control.quest.Gate.EYES_WITNESS);
    }

    /** The punch at the full charge landed on {@code target}: the combo, from now. */
    static void start(ServerPlayer player, PaleServer.PaleState state, LivingEntity target, Vec3 facing, long now) {
        ServerLevel level = player.m_284548_();
        Vec3 at = target.m_20182_();
        float yaw = (float)Math.toDegrees(Math.atan2(-facing.f_82479_, facing.f_82481_));
        // The ground it will come down on, under where it rises.
        Vec3 over = at.m_82549_(facing.m_82490_(WitnessRules.KNOCK_BACK));
        BlockHitResult ground = level.m_45547_(new ClipContext(over.m_82520_(0.0, 0.5, 0.0), over.m_82520_(0.0, -6.0, 0.0), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, target));
        double groundY = ground.m_6662_() == HitResult.Type.BLOCK ? ground.m_82450_().f_82480_ - at.f_82480_ : 0.0;
        PaleServer.begin(player, state, PaleMove.WITNESS, 0, target.m_19879_(), (float)groundY, at, now, yaw);
        hold(player, state, target, 0L, now);
        UkakuFire.sound(level, at, "entity.illusioner.mirror_move", 0.8F, KaguneServerController.vary(player, 1.1F, 0.05F));
    }

    /** True while {@code entity} is held frozen in someone's Witness This: no kagune of a frozen player answers. */
    static boolean frozen(LivingEntity entity) {
        Long until = FROZEN.get(entity.m_19879_());
        return until != null && entity.m_9236_().m_46467_() < until;
    }

    /** One tick of it. */
    static void tick(ServerPlayer player, PaleServer.PaleState state, long now) {
        long age = now - state.startedAt;
        player.f_19789_ = 0.0F;
        LivingEntity target = state.targetId == 0 ? null
            : player.m_9236_().m_6815_(state.targetId) instanceof LivingEntity living && living.m_6084_() ? living : null;
        if (target == null && age < WitnessRules.IMPACT) {
            // What she hit is gone (dead, away): she drops out of it.
            PaleServer.end(player, state);
            return;
        }
        if (target != null && WitnessRules.frozen(age)) {
            hold(player, state, target, age, now);
        }
        ServerLevel level = player.m_284548_();
        if (WitnessRules.blinks((int)age)) {
            UkakuFire.sound(level, player.m_20182_(), "entity.illusioner.mirror_move", 0.5F, KaguneServerController.vary(player, 1.5F, 0.08F));
        }
        int mastery = KaguneProfile.mastery(player);
        if (age == WitnessRules.UPPER && target != null) {
            blow(player, state, target, PaleRules.fist(mastery) * PaleServer.power(player) * WitnessRules.UPPER_DAMAGE, new Vec3(0.0, 1.0, 0.0), 2.4F, true);
            UkakuFire.sound(level, target.m_20182_(), "entity.player.attack.crit", 1.2F, KaguneServerController.vary(player, 0.7F, 0.05F));
            // A rag doll from here on, as the Speed Kick's: it tumbles through the storm, comes down flat in the crater, lies there and
            // gets up (every client draws it, KickTumble; the server keeps it down, PaleKnockdown).
            // 6.2.3: and once it is down in the crater it lies there stunned a good while (WitnessRules.DOWN_TICKS).
            PaleKnockdown.launched(target, now, WitnessRules.DOWN_TICKS, true);
            PaleBlitz.fx(player, S2CPaleFx.KICKED, target.m_19879_(), target.m_20182_(), local(state, 0.0, 1.2, 0.8), 1.0F, 0);
        }
        for (int hit = 0; hit < WitnessRules.HITS; hit++) {
            if (age == WitnessRules.hitAt(hit) && target != null) {
                double[] k = WitnessRules.knock(hit);
                Vec3 push = local(state, k[0], k[1], k[2]);
                WitnessRules.Blow kind = WitnessRules.blow(hit);
                // 6.2.3: every blow its own weight: the quick jabs a sharp crack, the slow heavy kicks a boom, a big flash and a
                // shockwave off it, sending it far further.
                boolean heavy = WitnessRules.heavy(hit);
                boolean hard = heavy || kind.kick();
                float power = (float)WitnessRules.power(hit);
                float size = (heavy ? 2.1F : hard ? 1.7F : 1.35F) * (0.75F + 0.25F * power);
                blow(player, state, target, PaleRules.fist(mastery) * PaleServer.power(player) * WitnessRules.HIT_DAMAGE * (0.6F + 0.4F * power), push, size, hard, true);
                if (kind.kick()) {
                    UkakuFire.sound(level, target.m_20182_(), "entity.player.attack.sweep", 0.9F, KaguneServerController.vary(player, heavy ? 0.6F : 0.8F, 0.06F));
                }
                if (heavy) {
                    UkakuFire.sound(level, target.m_20182_(), "entity.generic.explode", 0.45F, KaguneServerController.vary(player, 1.5F, 0.06F));
                    UkakuFire.sound(level, target.m_20182_(), "entity.warden.sonic_boom", 0.25F, KaguneServerController.vary(player, 1.7F, 0.06F));
                }
            }
        }
        if (age == WitnessRules.DROP && target != null) {
            // The stomp lands on it: down it goes, she riding it.
            blow(player, state, target, 0.0F, new Vec3(0.0, -1.0, 0.0), 3.4F, true);
            UkakuFire.sound(level, target.m_20182_(), "entity.generic.explode", 0.7F, KaguneServerController.vary(player, 1.3F, 0.05F));
            UkakuFire.sound(level, target.m_20182_(), "entity.player.attack.sweep", 1.2F, KaguneServerController.vary(player, 0.5F, 0.05F));
            UkakuFire.sound(level, target.m_20182_(), "entity.warden.sonic_boom", 0.4F, KaguneServerController.vary(player, 1.4F, 0.05F));
        }
        if (age == WitnessRules.IMPACT) {
            crater(player, state, target, now);
        }
        // 6.2.1: she falls out of the sky after the stomp: over once she is down (no fall damage meanwhile).
        if (age >= WitnessRules.LONGEST || age >= WitnessRules.TICKS && (player.m_20096_() || player.m_20069_())) {
            PaleServer.end(player, state);
        }
    }

    /** It is where the combo has it this tick, can do nothing, and takes no fall. */
    private static void hold(ServerPlayer player, PaleServer.PaleState state, LivingEntity target, long age, long now) {
        double[] at = WitnessRules.target(age + 1.0, state.charge);
        Vec3 feet = state.point.m_82549_(local(state, at[0], at[1], at[2]));
        FROZEN.put(target.m_19879_(), now + 2L);
        PaleStun.stun(target, now, 3);
        target.f_19789_ = 0.0F;
        if (target instanceof ServerPlayer frozen) {
            // A player's own client carries them along (they can still look round); put back only if they stray.
            if (frozen.m_20182_().m_82554_(feet) > 2.5) {
                CentipedeHunt.place(player, target, feet);
            }
            frozen.m_20256_(Vec3.f_82478_);
            return;
        }
        target.m_6021_(feet.f_82479_, feet.f_82480_, feet.f_82481_);
        target.m_20256_(Vec3.f_82478_);
        target.f_19864_ = true;
    }

    /** A blow of the combo: the hurt, the flash and ring on it, the body reeling the way it was struck. */
    private static void blow(ServerPlayer player, PaleServer.PaleState state, LivingEntity target, float damage, Vec3 push, float size, boolean hard) {
        blow(player, state, target, damage, push, size, hard, true);
    }

    /** ...{@code loud}: with the crack of it (every other blow of the blitz, a tick apart, is the flash and the thud alone). */
    private static void blow(ServerPlayer player, PaleServer.PaleState state, LivingEntity target, float damage, Vec3 push, float size, boolean hard,
                             boolean loud) {
        Vec3 middle = target.m_20182_().m_82520_(0.0, target.m_20206_() * 0.5, 0.0);
        if (damage > 0.0F) {
            target.f_19802_ = 0;
            WoundHits.expect(target, com.hex.kagune.control.wound.WoundBook.Blow.BLUNT, middle, push, 0.0);
            target.m_6469_(player.m_269291_().m_269075_(player), damage);
        }
        PaleBlitz.fx(player, S2CPaleFx.IMPACT, target.m_19879_(), middle.m_82546_(push.m_82490_(target.m_20205_() * 0.5)), push, size, hard ? S2CPaleFx.BIG_PUNCH : 0);
        ServerLevel level = player.m_284548_();
        if (loud || hard) {
            UkakuFire.sound(level, middle, "entity.player.attack.strong", 1.0F, KaguneServerController.vary(player, hard ? 0.6F : 0.9F, 0.08F));
        }
        UkakuFire.sound(level, middle, "entity.player.attack.knockback", 0.8F, KaguneServerController.vary(player, 0.8F, 0.08F));
        if (hard) {
            KaguneServerController.playAt(player, target, HexKaguneSounds.BONE_CRACK, 0.9F, KaguneServerController.vary(player, 0.7F, 0.05F));
        }
    }

    /**
     * Down: it hits the ground, a crater is knocked out round it (put back later, as every smashed block is), the shock hurts
     * everything near and throws it up and away; the rag doll it has been since the uppercut comes down flat and lies there a moment.
     */
    private static void crater(ServerPlayer player, PaleServer.PaleState state, LivingEntity target, long now) {
        ServerLevel level = player.m_284548_();
        double[] at = WitnessRules.target(WitnessRules.IMPACT, state.charge);
        Vec3 down = state.point.m_82549_(local(state, at[0], at[1], at[2]));
        BlockPos below = BlockPos.m_274561_(down.f_82479_, down.f_82480_ - 0.01, down.f_82481_);
        int groundState = Block.m_49956_(level.m_8055_(below));
        BlockSmash.crater(player, down, WitnessRules.CRATER);
        int mastery = KaguneProfile.mastery(player);
        if (target != null) {
            target.f_19802_ = 0;
            WoundHits.expect(target, com.hex.kagune.control.wound.WoundBook.Blow.SLAM, target.m_20182_(), new Vec3(0.0, -1.0, 0.0), 0.0);
            target.m_6469_(player.m_269291_().m_269075_(player), PaleRules.fist(mastery) * PaleServer.power(player) * WitnessRules.DROP_DAMAGE);
            target.m_20256_(Vec3.f_82478_);
            target.f_19864_ = true;
            // 6.2.3: stunned from the moment it hits (PaleKnockdown keeps it so while it lies there and gets up).
            PaleStun.stun(target, now, 20);
            KaguneServerController.playAt(player, target, HexKaguneSounds.BONE_CRACK, 1.0F, KaguneServerController.vary(player, 0.55F, 0.05F));
        }
        AABB area = new AABB(down.f_82479_ - WitnessRules.SHOCK, down.f_82480_ - 2.0, down.f_82481_ - WitnessRules.SHOCK, down.f_82479_ + WitnessRules.SHOCK,
            down.f_82480_ + 3.0, down.f_82481_ + WitnessRules.SHOCK);
        for (LivingEntity body : level.m_6443_(LivingEntity.class, area, entity -> entity != target && PaleBlitz.valid(player, entity))) {
            Vec3 off = body.m_20182_().m_82546_(down);
            double flat = Math.sqrt(off.f_82479_ * off.f_82479_ + off.f_82481_ * off.f_82481_);
            if (flat > WitnessRules.SHOCK) {
                continue;
            }
            double near = 1.0 - flat / WitnessRules.SHOCK;
            Vec3 away = flat < 0.2 ? local(state, 0.0, 0.0, 1.0) : new Vec3(off.f_82479_ / flat, 0.0, off.f_82481_ / flat);
            body.f_19802_ = 0;
            if (body.m_6469_(player.m_269291_().m_269075_(player), PaleRules.fist(mastery) * PaleServer.power(player) * WitnessRules.SHOCK_DAMAGE * (float)(0.4 + 0.6 * near))) {
                BlockSmash.launch(player, body, away.m_82490_(0.6 + 0.9 * near).m_82520_(0.0, 0.5 + 0.6 * near, 0.0), 30);
            }
        }
        PaleBlitz.fx(player, S2CPaleFx.SHOCKWAVE, 0, down, local(state, 0.0, 0.0, 1.0), (float)WitnessRules.SHOCK, groundState);
        // 6.2.1: more impact: the flash and ring of a full-charge punch on it, a second ring of the shock out wide, thunder.
        PaleBlitz.fx(player, S2CPaleFx.IMPACT, target == null ? 0 : target.m_19879_(), down.m_82520_(0.0, 0.4, 0.0), new Vec3(0.0, -1.0, 0.0), 4.0F,
            S2CPaleFx.BIG_PUNCH);
        PaleBlitz.fx(player, S2CPaleFx.SHOCKWAVE, 0, down, local(state, 1.0, 0.0, 0.0), (float)(WitnessRules.SHOCK * 0.6), groundState);
        UkakuFire.sound(level, down, "entity.lightning_bolt.thunder", 0.6F, KaguneServerController.vary(player, 1.2F, 0.05F));
        UkakuFire.sound(level, down, "entity.generic.explode", 1.5F, KaguneServerController.vary(player, 0.5F, 0.05F));
        UkakuFire.sound(level, down, "entity.generic.explode", 1.3F, KaguneServerController.vary(player, 0.65F, 0.05F));
        UkakuFire.sound(level, down, "block.anvil.land", 0.8F, KaguneServerController.vary(player, 0.45F, 0.05F));
        UkakuFire.sound(level, down, "entity.warden.sonic_boom", 0.5F, KaguneServerController.vary(player, 0.8F, 0.05F));
        KaguneServerController.playSelf(player, HexKaguneSounds.KAGUNE_SLAM, 1.0F, KaguneServerController.vary(player, 0.55F, 0.05F));
        KaguneProfile.addMastery(player, "Swipe", 3);
        KaguneNetwork.broadcast(player);
        PaleServer.landed(player, 1);
    }

    /** A point in the move's frame (x to her right as she punched, y up, z the way it went) as a world offset. */
    static Vec3 local(PaleServer.PaleState state, double x, double y, double z) {
        double yaw = Math.toRadians(state.yaw);
        Vec3 facing = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(-facing.f_82481_, 0.0, facing.f_82479_);
        return right.m_82490_(x).m_82549_(facing.m_82490_(z)).m_82520_(0.0, y, 0.0);
    }
}
