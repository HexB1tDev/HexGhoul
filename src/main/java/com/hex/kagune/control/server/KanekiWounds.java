package com.hex.kagune.control.server;

import com.hex.kagune.control.StrikePattern;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CAyatoFx;
import com.hex.kagune.control.wound.WoundBook;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneRuntime;
import com.hex.kagune.gameplay.KaguneServerController;
import com.hex.kagune.gameplay.TendrilMode;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Kaneki's tails draw blood and tear wounds (3.0.3). Whenever a Kaneki with the kagune out hurts a body (a combo strike, the
 * Centipede legs, a follow-up, a crush or a slam), every client near it is told ({@link S2CAyatoFx#KANEKI_HIT}): blood bursts
 * out where it struck, and on a player the wounds that hit opens take the blow's shape ({@link WoundBook.Blow}): a lash leaves
 * a long gash along the swing where it struck, a thrust a deep hole, a crush big tears round the torso and arms, a slam big tears
 * down the back and legs.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class KanekiWounds {
    private KanekiWounds() {
    }

    /**
     * True if this hit is a Kaneki's own (his kagune out, not an Ayato, dealt by him and not by something he shot): his tails,
     * which shape their own wounds ({@link #hurt}); every other hit on a player is {@link WoundHits}'.
     */
    static boolean tails(LivingEntity target, net.minecraft.world.damagesource.DamageSource source) {
        return source.m_7639_() instanceof ServerPlayer player && player != target && source.m_7640_() == player && KaguneProfile.isActive(player)
            && !AyatoServer.isAyato(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (event.getAmount() <= 0.0F || !tails(target, event.getSource())) {
            return;
        }
        ServerPlayer player = (ServerPlayer)event.getSource().m_7639_();
        KaguneRuntime.State state = KaguneRuntime.peek(player.m_20148_());
        TendrilMode mode = state == null ? null : state.mode;
        // 3.1.0: the centipede legs' crush and slam tear like the tails' own.
        if (state != null && state.centipedeAttackActive() && state.centipedeAttackTargetId == target.m_19879_()) {
            if (state.centipedeTechnique == com.hex.kagune.gameplay.CentipedeTechnique.CRUSH) {
                mode = TendrilMode.CRUSH;
            } else if (state.centipedeTechnique == com.hex.kagune.gameplay.CentipedeTechnique.SLAM) {
                mode = TendrilMode.SLAM;
            }
        }
        Vec3 center = KaguneServerController.center(target);
        Vec3 toward = center.m_82546_(player.m_146892_());
        Vec3 into = toward.m_82556_() < 1.0E-6 ? player.m_20154_() : toward.m_82541_();
        WoundBook.Blow blow;
        double angle = (player.m_217043_().m_188500_() - 0.5) * 1.6;
        Vec3 at;
        if (mode == TendrilMode.CRUSH) {
            blow = WoundBook.Blow.CRUSH;
            at = center;
        } else if (mode == TendrilMode.SLAM) {
            blow = WoundBook.Blow.SLAM;
            at = target.m_20182_().m_82520_(0.0, 0.3, 0.0);
            into = new Vec3(0.0, -1.0, 0.0);
        } else {
            StrikePattern pattern = mode == TendrilMode.SWIPE ? KaguneCombat.livePattern(player, state) : null;
            blow = pattern == StrikePattern.THRUST || pattern == StrikePattern.IMPALE ? WoundBook.Blow.STAB : WoundBook.Blow.SLASH;
            if (pattern != null) {
                angle = switch (pattern) {
                    case OVERHEAD -> Math.PI * 0.5;
                    case RISING -> -0.8;
                    case BACKHAND -> -0.3;
                    case LASH -> 0.3;
                    default -> 0.0;
                };
            }
            // Where it struck: the side that faced him, somewhere between the hips and the shoulders.
            double height = (player.m_217043_().m_188500_() - 0.5) * 0.7;
            at = center.m_82546_(into.m_82490_(0.3)).m_82520_(0.0, height, 0.0);
        }
        float power = Math.max(0.5F, Math.min(2.0F, event.getAmount() / 5.0F)) * (blow == WoundBook.Blow.SLAM || blow == WoundBook.Blow.CRUSH ? 1.4F : 1.0F);
        int degrees = (int)Math.round(Math.toDegrees(angle)) + 180;
        int extra = blow.ordinal() + 16 * Math.max(0, Math.min(360, degrees));
        ControlNetwork.broadcastAround(target, new S2CAyatoFx(S2CAyatoFx.KANEKI_HIT, player.m_19879_(), target.m_19879_(), at.f_82479_, at.f_82480_,
            at.f_82481_, (float)into.f_82479_, (float)into.f_82480_, (float)into.f_82481_, power, extra));
    }
}
