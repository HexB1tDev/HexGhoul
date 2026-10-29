package com.hex.kagune.control.client.animation;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.ControlConfig;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.client.ControlClient;
import com.hex.kagune.control.client.PathClient;
import com.hex.kagune.gameplay.TendrilMode;
import net.minecraft.client.player.AbstractClientPlayer;

/**
 * When to stand in the path's ambient stance ({@link IdlePose}): a player whose kagune is out and who has stood still for
 * a second eases into it, and eases out the moment they move, jump, crouch, swim, ride, use an item, guard or attack.
 * The kagune body layer owns one of these and adds the stance to its own pose.
 */
final class IdleStance {
    private final AbstractClientPlayer player;
    private int stillTicks;
    private float blend;
    private float previousBlend;
    private double lastX = Double.NaN;
    private double lastZ = Double.NaN;
    private int age;
    private IdlePose.Style style = IdlePose.Style.NONE;

    IdleStance(AbstractClientPlayer player) {
        this.player = player;
    }

    void tick() {
        this.age++;
        this.previousBlend = this.blend;
        KaguneClientState.View view = KaguneClientState.get(this.player);
        KagunePath path = PathClient.get(this.player);
        IdlePose.Style wanted = path == KagunePath.UKAKU ? (com.hex.kagune.control.client.UkakuClient.kakuja(this.player) ? IdlePose.Style.UKAKU_KAKUJA
            : IdlePose.Style.UKAKU_RELAXED) : path == KagunePath.RINKAKU ? IdlePose.Style.RINKAKU_FERAL
            : path == KagunePath.PALE ? IdlePose.Style.PALE_PLAYFUL : IdlePose.Style.NONE;
        double x = this.player.m_20185_();
        double z = this.player.m_20189_();
        boolean moved = !Double.isNaN(this.lastX) && ((x - this.lastX) * (x - this.lastX) + (z - this.lastZ) * (z - this.lastZ)) > 1.0E-4;
        this.lastX = x;
        this.lastZ = z;
        boolean calm = view.active() && this.player.m_6084_() && this.player.m_20096_() && !moved && this.player.f_267362_.m_267711_(1.0F) < 0.05F
            && !this.player.m_6047_() && !this.player.m_6069_() && !this.player.m_20159_() && !this.player.m_5803_() && !this.player.m_21255_()
            && !this.player.m_6117_() && view.mode() == TendrilMode.IDLE && !view.guarding()
            && !ControlClient.get(this.player).comboPlaying(ControlClient.worldTime(0.0F)) && ControlConfig.bodyAnimations()
            && com.hex.kagune.control.client.PaleClient.get(this.player).move() == com.hex.kagune.control.pale.PaleMove.NONE;
        this.stillTicks = calm ? this.stillTicks + 1 : 0;
        if (wanted != this.style && this.blend <= 0.0F) {
            this.style = wanted;
        }
        boolean idle = calm && this.stillTicks >= IdlePose.STILL_TICKS && wanted == this.style && wanted != IdlePose.Style.NONE;
        this.blend = IdlePose.step(this.blend, idle);
    }

    /** True while the stance shows at all (the body layer keeps itself active for it). */
    boolean showing() {
        return this.blend > 0.0F || this.previousBlend > 0.0F;
    }

    /** 0..1 eased weight of the stance this frame. */
    float weight(float tickDelta) {
        return IdlePose.ease(this.previousBlend + (this.blend - this.previousBlend) * tickDelta);
    }

    /** 6.1.0: the style the stance is in (Pale's is drawn by {@link PaleIdle} with this stance's weight). */
    IdlePose.Style style() {
        return this.style;
    }

    /** The stance for this frame (all zero when not standing in it). */
    RigPose pose(float tickDelta) {
        return IdlePose.pose(this.style, this.weight(tickDelta), (this.age + tickDelta) / 20.0, BodyAnimations.limbsBend());
    }
}
