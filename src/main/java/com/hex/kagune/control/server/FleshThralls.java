package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlEntities;
import com.hex.kagune.control.king.PeaceRules;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CKingFx;
import com.hex.kagune.control.npc.FleshThrall;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.0.34: the minds of Peace's flesh creatures (Rebirth Instinct, {@link FleshThrall}), on the server. Each keeps its own place at its
 * maker's side (a slot of its own round behind him, kept a step apart from the others, never on top of him), walks there round what
 * is in the way, and takes its own time over everything (when it looks again for a way, when it swings), so no two move as one. What
 * is hostile near him it goes for (spread over them: one does not pile in where another already is), its arms grown into blades, and
 * cuts at; with nothing near, the blades go back in and it follows again. Stuck, it jumps, tries another way round, and in the end
 * finds its way back to him. Its time over, it melts down into the ground and is gone.
 */
public final class FleshThralls {
    /** What one of them is about: whose, which of how many, born when, its prey, its timers. */
    static final class Mind {
        final LivingEntity owner;
        final int index;
        final int count;
        final long born;
        final int life;
        final double pace;
        LivingEntity prey;
        long lookAt;
        long pathAt;
        long swingAt = Long.MIN_VALUE / 2;
        long nextSwing;
        boolean struck = true;
        boolean blades;
        boolean melting;
        Vec3 goal;
        Vec3 was;
        long wasAt;
        int stuck;
        double detour;

        Mind(LivingEntity owner, int index, int count, long born, int life, double pace) {
            this.owner = owner;
            this.index = index;
            this.count = count;
            this.born = born;
            this.life = life;
            this.pace = pace;
        }
    }

    private static final Map<FleshThrall, Mind> MINDS = new WeakHashMap<>();
    /** How far off him one may get before it is called back to his side at once (blocks). */
    private static final double LEASH = 22.0;

    private FleshThralls() {
    }

    /** One brought up at {@code x, y, z} facing {@code yaw}, the {@code index}th of {@code count}, for {@code owner}; its id, or 0. */
    public static int summon(LivingEntity owner, double x, double y, double z, float yaw, int index, int count) {
        ServerLevel level = (ServerLevel)owner.m_9236_();
        FleshThrall thrall = ControlEntities.THRALL.get().m_20615_(level);
        if (thrall == null) {
            return 0;
        }
        thrall.m_7678_(x, y, z, yaw, 0.0F);
        if (!level.m_7967_(thrall)) {
            return 0;
        }
        long now = level.m_46467_();
        double pace = 0.94 + 0.04 * ((thrall.m_19879_() * 7 + index * 3) % 5);
        MINDS.put(thrall, new Mind(owner, index, count, now, PeaceRules.THRALL_LIFE, pace));
        fx(owner, thrall, 0, PeaceRules.THRALL_LIFE);
        return thrall.m_19879_();
    }

    /** Every one of {@code owner}'s made to start melting now (a new Rebirth calls up new ones). */
    public static void dismiss(LivingEntity owner) {
        for (Map.Entry<FleshThrall, Mind> e : new ArrayList<>(MINDS.entrySet())) {
            Mind mind = e.getValue();
            if (mind.owner == owner && !mind.melting) {
                melt(e.getKey(), mind);
            }
        }
    }

    /** True if {@code body} is one of {@code owner}'s (what his own blows and shields leave alone). */
    public static boolean ownedBy(LivingEntity body, LivingEntity owner) {
        if (!(body instanceof FleshThrall thrall)) {
            return false;
        }
        Mind mind = MINDS.get(thrall);
        return mind != null && mind.owner == owner;
    }

    // ------------------------------------------------------------------ each tick

    public static void tick(FleshThrall thrall) {
        Mind mind = MINDS.get(thrall);
        ServerLevel level = (ServerLevel)thrall.m_9236_();
        if (mind == null || mind.owner.m_213877_() || !mind.owner.m_6084_() || mind.owner.m_9236_() != level) {
            thrall.m_146870_(); // no one's: not kept
            return;
        }
        long now = level.m_46467_();
        long age = now - mind.born;
        if (age >= mind.life) {
            thrall.m_146870_();
            return;
        }
        if (!mind.melting && age >= mind.life - PeaceRules.THRALL_MELT) {
            melt(thrall, mind);
        }
        if (age < PeaceRules.THRALL_RISE || mind.melting) {
            // Climbing up out of the ground, or sinking back into it: it stands where it is.
            thrall.m_21573_().m_26573_();
            thrall.m_20334_(0.0, Math.min(0.0, thrall.m_20184_().f_82480_), 0.0);
            return;
        }
        LivingEntity owner = mind.owner;
        apart(thrall, mind);
        double far = thrall.m_20280_(owner);
        if (far > LEASH * LEASH) {
            home(level, thrall, mind, now);
            return;
        }
        hunt(level, thrall, mind, now);
        if (mind.prey != null) {
            fight(thrall, mind, now);
        } else {
            follow(level, thrall, mind, now);
        }
        unstick(level, thrall, mind, now);
    }

    /** Its own place by him: one of a fan round behind him, wider the more there are, the odd ones a little further out. */
    static Vec3 slot(LivingEntity owner, Mind mind) {
        double yaw = Math.toRadians(owner.m_146908_());
        double spread = Math.toRadians(mind.count <= 2 ? 70.0 : 52.0);
        double a = yaw + Math.PI + (mind.index - (mind.count - 1) * 0.5) * spread + mind.detour;
        double r = PeaceRules.THRALL_FOLLOW + 0.5 * (mind.index % 2);
        return new Vec3(owner.m_20185_() - Math.sin(a) * r, owner.m_20186_(), owner.m_20189_() + Math.cos(a) * r);
    }

    /** Following: to its place, at its own pace, looking for the way again now and then (each on its own clock), still when there. */
    private static void follow(ServerLevel level, FleshThrall thrall, Mind mind, long now) {
        if (mind.blades) {
            mind.blades = false;
            fx(mind.owner, thrall, 2, 0);
        }
        Vec3 to = slot(mind.owner, mind);
        double off = thrall.m_20238_(to);
        if (off < 1.1 * 1.1) {
            thrall.m_21573_().m_26573_();
            mind.goal = null;
            if ((now + thrall.m_19879_()) % 40L == 0L) {
                thrall.m_21563_().m_24960_(mind.owner, 20.0F, 20.0F);
            }
            return;
        }
        boolean moved = mind.goal == null || mind.goal.m_82557_(to) > 1.5 * 1.5;
        if (moved && now >= mind.pathAt || thrall.m_21573_().m_26571_() && now >= mind.pathAt) {
            double y = ground(level, to.f_82479_, to.f_82481_, to.f_82480_);
            double speed = (off > 64.0 ? 1.45 : off > 16.0 ? 1.2 : 0.95) * mind.pace;
            thrall.m_21573_().m_26519_(to.f_82479_, Double.isNaN(y) ? to.f_82480_ : y, to.f_82481_, speed);
            mind.goal = to;
            mind.pathAt = now + 8 + (thrall.m_19879_() + mind.index * 5) % 7;
        }
    }

    /** What is hostile near him: kept while it is, else the best for this one (near it, not already set upon by the others). */
    private static void hunt(ServerLevel level, FleshThrall thrall, Mind mind, long now) {
        if (mind.prey != null && (!mind.prey.m_6084_() || mind.prey.m_5833_() || mind.prey.m_20280_(mind.owner) > Math.pow(PeaceRules.THRALL_HUNT + 4.0, 2.0))) {
            mind.prey = null;
        }
        LivingEntity struck = thrall.m_21188_();
        if (mind.prey == null && struck != null && struck.m_6084_() && struck != mind.owner && !ownedBy(struck, mind.owner)
            && struck.m_20280_(thrall) < PeaceRules.THRALL_HUNT * PeaceRules.THRALL_HUNT) {
            mind.prey = struck; // what hurt it, it answers
        }
        if (mind.prey != null || now < mind.lookAt) {
            return;
        }
        mind.lookAt = now + 10 + (thrall.m_19879_() + mind.index * 3) % 6;
        double r = PeaceRules.THRALL_HUNT;
        Vec3 at = mind.owner.m_20182_();
        List<LivingEntity> near = level.m_6443_(LivingEntity.class, new AABB(at.f_82479_ - r, at.f_82480_ - 6.0, at.f_82481_ - r, at.f_82479_ + r,
            at.f_82480_ + 6.0, at.f_82481_ + r), body -> body != thrall && body != mind.owner && !(body instanceof FleshThrall) && KingEffects.hostile(mind.owner, body));
        LivingEntity best = null;
        double score = Double.MAX_VALUE;
        for (LivingEntity body : near) {
            if (body.m_20280_(mind.owner) > r * r) {
                continue;
            }
            int taken = 0;
            for (Map.Entry<FleshThrall, Mind> e : MINDS.entrySet()) {
                if (e.getKey() != thrall && e.getValue().owner == mind.owner && e.getValue().prey == body) {
                    taken++;
                }
            }
            double s = Math.sqrt(thrall.m_20280_(body)) + 5.0 * taken;
            if (s < score) {
                score = s;
                best = body;
            }
        }
        mind.prey = best;
    }

    /** At its prey: in to reach, the blades out, cutting at it on its own beat; the blow lands a few ticks into the swing. */
    private static void fight(FleshThrall thrall, Mind mind, long now) {
        LivingEntity prey = mind.prey;
        if (!mind.blades) {
            mind.blades = true;
            fx(mind.owner, thrall, 1, prey.m_19879_());
        }
        double reach = 1.7 + prey.m_20205_() * 0.5;
        double d = Math.sqrt(thrall.m_20280_(prey));
        thrall.m_21563_().m_24960_(prey, 40.0F, 40.0F);
        if (!mind.struck && now - mind.swingAt >= 5) {
            mind.struck = true;
            if (d <= reach + 0.6 && prey.m_6084_()) {
                if (prey.m_6469_(thrall.m_269291_().m_269333_(thrall), PeaceRules.THRALL_DAMAGE)) {
                    prey.m_147240_(0.35, thrall.m_20185_() - prey.m_20185_(), thrall.m_20189_() - prey.m_20189_());
                    Vec3 c = prey.m_20182_().m_82520_(0.0, prey.m_20206_() * 0.6, 0.0);
                    Vec3 way = c.m_82546_(thrall.m_20182_().m_82520_(0.0, 1.2, 0.0));
                    ControlNetwork.broadcastAround(thrall, new S2CKingFx(mind.owner.m_20148_(), S2CKingFx.MARK, new int[] {com.hex.kagune.control.king.KingMarkFx.BLOOD,
                        prey.m_19879_()}, new float[] {(float)c.f_82479_, (float)c.f_82480_, (float)c.f_82481_, (float)way.f_82479_, (float)way.f_82480_,
                        (float)way.f_82481_, 0.8F, 0.0F, 0.0F}));
                }
            }
        }
        if (d > reach) {
            if (now >= mind.pathAt || thrall.m_21573_().m_26571_()) {
                thrall.m_21573_().m_5624_(prey, 1.25 * mind.pace);
                mind.pathAt = now + 6 + (thrall.m_19879_() + mind.index) % 5;
            }
            return;
        }
        thrall.m_21573_().m_26573_();
        if (now >= mind.nextSwing) {
            mind.swingAt = now;
            mind.struck = false;
            mind.nextSwing = now + PeaceRules.THRALL_SWING + (thrall.m_19879_() * 3 + mind.index * 5) % 7;
            fx(mind.owner, thrall, 3, prey.m_19879_());
        }
    }

    /** A step apart from the others and off him: shouldered aside a little each tick it is too near. */
    private static void apart(FleshThrall thrall, Mind mind) {
        double px = 0.0;
        double pz = 0.0;
        for (Map.Entry<FleshThrall, Mind> e : MINDS.entrySet()) {
            FleshThrall other = e.getKey();
            if (other == thrall || other.m_9236_() != thrall.m_9236_() || e.getValue().owner != mind.owner) {
                continue;
            }
            double dx = thrall.m_20185_() - other.m_20185_();
            double dz = thrall.m_20189_() - other.m_20189_();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < PeaceRules.THRALL_APART) {
                if (d < 1.0E-3) {
                    double a = thrall.m_19879_() * 2.39996;
                    dx = Math.cos(a);
                    dz = Math.sin(a);
                    d = 1.0;
                }
                double k = (PeaceRules.THRALL_APART - Math.min(d, PeaceRules.THRALL_APART)) * 0.09 / d;
                px += dx * k;
                pz += dz * k;
            }
        }
        double ox = thrall.m_20185_() - mind.owner.m_20185_();
        double oz = thrall.m_20189_() - mind.owner.m_20189_();
        double od = Math.sqrt(ox * ox + oz * oz);
        if (od < 1.5 && od > 1.0E-3) {
            px += ox / od * 0.08;
            pz += oz / od * 0.08;
        }
        if (px != 0.0 || pz != 0.0) {
            Vec3 v = thrall.m_20184_();
            thrall.m_20334_(v.f_82479_ + px, v.f_82480_, v.f_82481_ + pz);
        }
    }

    /** Going nowhere though it means to: it jumps, then tries its place from another side, then in the end goes back to him. */
    private static void unstick(ServerLevel level, FleshThrall thrall, Mind mind, long now) {
        if (now - mind.wasAt < 20L) {
            return;
        }
        Vec3 at = thrall.m_20182_();
        boolean wants = mind.prey != null ? thrall.m_20280_(mind.prey) > 9.0 : mind.goal != null && thrall.m_20238_(mind.goal) > 4.0;
        if (mind.was != null && wants && at.m_82557_(mind.was) < 0.35 * 0.35) {
            mind.stuck++;
            if (thrall.m_20096_()) {
                thrall.m_21569_().m_24901_();
            }
            if (mind.stuck == 2) {
                mind.detour = (mind.index % 2 == 0 ? 1.0 : -1.0) * 0.9; // round the other side
                mind.pathAt = now;
                mind.goal = null;
            } else if (mind.stuck >= 4) {
                home(level, thrall, mind, now);
            }
        } else {
            if (mind.stuck > 0 && !wants) {
                mind.detour = 0.0;
            }
            mind.stuck = 0;
        }
        mind.was = at;
        mind.wasAt = now;
    }

    /** Back to its place by him at once (too far off, or stuck): onto open ground there, if there is any. */
    private static void home(ServerLevel level, FleshThrall thrall, Mind mind, long now) {
        mind.detour = 0.0;
        mind.stuck = 0;
        Vec3 to = slot(mind.owner, mind);
        for (int k = 0; k < 6; k++) {
            double a = k * 1.1 + mind.index;
            double x = to.f_82479_ + (k == 0 ? 0.0 : Math.cos(a) * 1.2);
            double z = to.f_82481_ + (k == 0 ? 0.0 : Math.sin(a) * 1.2);
            double y = ground(level, x, z, mind.owner.m_20186_());
            if (!Double.isNaN(y)) {
                thrall.m_21573_().m_26573_();
                thrall.m_6021_(x, y, z);
                mind.goal = null;
                mind.pathAt = now + 5;
                return;
            }
        }
    }

    private static void melt(FleshThrall thrall, Mind mind) {
        mind.melting = true;
        mind.prey = null;
        thrall.m_21573_().m_26573_();
        fx(mind.owner, thrall, 4, 0);
    }

    /**
     * The ground at (x, z) near {@code y}: the top of the first solid block with room for a body over it, from a few blocks over
     * {@code y} down; NaN for none.
     */
    public static double ground(ServerLevel level, double x, double z, double y) {
        int bx = (int)Math.floor(x);
        int bz = (int)Math.floor(z);
        int top = (int)Math.floor(y) + 3;
        for (int by = top; by >= top - 10; by--) {
            if (!solid(level, bx, by, bz)) {
                continue;
            }
            if (solid(level, bx, by + 1, bz) || solid(level, bx, by + 2, bz)) {
                continue;
            }
            return by + 1.0;
        }
        return Double.NaN;
    }

    private static boolean solid(ServerLevel level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return !level.m_8055_(pos).m_60812_(level, pos).m_83281_();
    }

    /** What every client near is told of it: 0 born (its life), 1 its blades out (at whom), 2 in again, 3 a swing (at whom), 4 melting. */
    private static void fx(LivingEntity owner, FleshThrall thrall, int what, int arg) {
        ControlNetwork.broadcastAround(thrall, new S2CKingFx(owner.m_20148_(), S2CKingFx.PEACE_THRALL, new int[] {thrall.m_19879_(), what, arg}, new float[0]));
    }
}
