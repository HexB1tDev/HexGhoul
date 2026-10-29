package com.hex.kagune.control.client.fx;

import com.hex.kagune.control.client.EtoCamera;
import com.hex.kagune.control.client.EtoClient;
import com.hex.kagune.control.king.KingMarkFx;
import com.hex.kagune.control.king.WarRules;
import com.hex.kagune.control.ayato.UkakuModel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.0.19: the motes of a final kakuja's marks on this client ({@link KingMarkFx} draws their shapes): real drops of blood thrown along
 * the blow that fall and stain where they land ({@link AyatoBlood}), black smoke and ash, dust and earth thrown up from the ground,
 * embers, pale motes; the view kicked by what lands near. And War's RC spikes in flight, homing on their prey as the server flies them.
 * None of the game's own particles.
 */
public final class KingMarks {
    /** How far away (blocks) a mark is still shown. */
    static final double FAR = 64.0;

    /** One of War's RC spikes in flight: where it was and is, which way it goes, what it hunts, how long it has flown. */
    private static final class Flight {
        Vec3 was;
        Vec3 at;
        Vec3 dir;
        final int target;
        int life;

        Flight(Vec3 at, Vec3 dir, int target) {
            this.was = at;
            this.at = at;
            this.dir = dir;
            this.target = target;
        }
    }

    private static final List<Flight> FLIGHTS = new ArrayList<>();

    /** A mark this client made itself (from what the server sent of a blow): its shapes, drawn until they are done. */
    private record Local(int style, double[] at, double[] way, double power, double born, long seed) {
    }

    private static final List<Local> LOCAL = new ArrayList<>();

    private KingMarks() {
    }

    /** Mark {@code style} struck at {@code at} along {@code way}, {@code power} strong, on {@code body} (or null). */
    public static void play(ClientLevel level, int style, Vec3 at, Vec3 way, float power, Entity body) {
        if (!RcFx.near(at, FAR)) {
            return;
        }
        float p = Math.max(0.3F, Math.min(3.0F, power));
        Vec3 along = way.m_82556_() < 1.0E-6 ? new Vec3(Math.random() - 0.5, 0.6, Math.random() - 0.5).m_82541_() : way.m_82541_();
        switch (style) {
            case KingMarkFx.BLOOD -> {
                AyatoBlood.spurt(level, at, along, p, body);
                mist(at, along, p, 0xB0700812);
            }
            case KingMarkFx.GORE -> {
                AyatoBlood.spurt(level, at, along, 1.8F * p, body);
                AyatoBlood.spurt(level, at, along.m_82490_(-0.6).m_82520_(0.0, 0.8, 0.0), p, body);
                mist(at, along, 1.6F * p, 0xC0700812);
                kick(at, 0.45F * p, 10.0);
            }
            case KingMarkFx.SOOT -> smoke(at, along, p);
            case KingMarkFx.QUAKE -> quake(at, p);
            case KingMarkFx.FLARE -> {
                for (int i = 0; i < RcFx.count(8.0F * p); i++) {
                    Vec3 v = new Vec3(Math.random() - 0.5, Math.random() * 0.8, Math.random() - 0.5).m_82490_(0.12 * p);
                    RcFx.at(RcFx.EMBER, true, 14 + (int)(Math.random() * 10), at).velocity(v).colors(0xFFFF5A40, 0x00800010).size(0.08F, 0.02F).fall(0.006F)
                        .drag(0.9F).spawn();
                }
                kick(at, 0.25F * p, 8.0);
            }
            case KingMarkFx.GLINT -> {
                for (int i = 0; i < RcFx.count(7.0F * p); i++) {
                    Vec3 v = new Vec3(Math.random() - 0.5, Math.random() * 0.6, Math.random() - 0.5).m_82490_(0.1 * p);
                    RcFx.at(RcFx.SPARK, true, 10 + (int)(Math.random() * 8), at).velocity(v).colors(0xFFFFFFFF, 0x00FFB0C0).size(0.14F, 0.03F).drag(0.88F)
                        .spawn();
                }
            }
            case KingMarkFx.CLASH -> {
                // 1.0.40: the wind of two kagune meeting: puffs of air blown out across the line they met on, sparks, the view of all near
                // kicked hard.
                Vec3 any = Math.abs(along.f_82480_) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
                Vec3 u = along.m_82537_(any).m_82541_();
                Vec3 w = along.m_82537_(u).m_82541_();
                int n = RcFx.count(14.0F * p);
                for (int i = 0; i < n; i++) {
                    double a = Math.PI * 2.0 * (i + Math.random() * 0.5) / n;
                    Vec3 out = u.m_82490_(Math.cos(a)).m_82549_(w.m_82490_(Math.sin(a)));
                    RcFx.at(RcFx.PUFF, false, 14 + (int)(Math.random() * 8), at.m_82549_(out.m_82490_(0.3))).velocity(out.m_82490_(0.32 * p))
                        .colors(0x90E8EEF6, 0x00E8EEF6).size(0.35F * p, 1.3F * p).drag(0.84F).spawn();
                }
                for (int i = 0; i < RcFx.count(10.0F * p); i++) {
                    Vec3 v = new Vec3(Math.random() - 0.5, Math.random() - 0.3, Math.random() - 0.5).m_82490_(0.3 * p);
                    RcFx.at(RcFx.SPARK, true, 8 + (int)(Math.random() * 8), at).velocity(v).colors(0xFFFFFFFF, 0x00FF5060).size(0.12F, 0.03F).drag(0.86F)
                        .spawn();
                }
                kick(at, 0.6F * p, 14.0);
            }
            case KingMarkFx.TRAIL -> {
                smoke(at, along.m_82490_(-1.0), 0.5F * p);
                for (int i = 0; i < RcFx.count(3.0F); i++) {
                    RcFx.at(RcFx.EMBER, true, 16, at.m_82520_((Math.random() - 0.5) * 0.6, (Math.random() - 0.5) * 1.0, (Math.random() - 0.5) * 0.6))
                        .velocity(new Vec3(0.0, 0.02, 0.0)).colors(0xFFE8121C, 0x00400006).size(0.06F, 0.02F).wander(0.01F).spawn();
                }
            }
            default -> {
            }
        }
    }

    /** Mark {@code style} struck now on this client alone: its motes ({@link #play}) and its shapes. */
    public static void strike(ClientLevel level, int style, Vec3 at, Vec3 way, float power, Entity body) {
        if (level == null || !RcFx.near(at, FAR)) {
            return;
        }
        play(level, style, at, way, power, body);
        if (KingMarkFx.drawn(style) && LOCAL.size() < 128) {
            Vec3 w = way.m_82556_() < 1.0E-6 ? Vec3.f_82478_ : way.m_82541_();
            LOCAL.add(new Local(style, new double[] {at.f_82479_, at.f_82480_, at.f_82481_}, new double[] {w.f_82479_, w.f_82480_, w.f_82481_}, power,
                level.m_46467_(), (long)(Math.random() * Long.MAX_VALUE)));
        }
    }

    /** A fine red mist off a wound, hanging a moment. */
    static void mist(Vec3 at, Vec3 along, float p, int colour) {
        for (int i = 0; i < RcFx.count(4.0F * p); i++) {
            Vec3 v = along.m_82490_(0.04 + Math.random() * 0.06).m_82520_((Math.random() - 0.5) * 0.04, Math.random() * 0.03, (Math.random() - 0.5) * 0.04);
            RcFx.at(RcFx.PUFF, false, 12 + (int)(Math.random() * 8), at).velocity(v).colors(colour, colour & 0xFFFFFF).size(0.18F * p, 0.5F * p).drag(0.86F)
                .spawn();
        }
    }

    /** Black smoke rolling off along {@code along}, flakes of ash in it. */
    static void smoke(Vec3 at, Vec3 along, float p) {
        for (int i = 0; i < RcFx.count(3.0F * p); i++) {
            Vec3 from = at.m_82520_((Math.random() - 0.5) * 0.6, (Math.random() - 0.5) * 0.8, (Math.random() - 0.5) * 0.6);
            RcFx.at(RcFx.PUFF, false, 24 + (int)(Math.random() * 12), from).velocity(along.m_82490_(0.03).m_82520_(0.0, 0.015, 0.0)).colors(0xA0120F10, 0x00120F10)
                .size(0.4F * p, 1.1F * p).wander(0.01F).drag(0.92F).spawn();
        }
        for (int i = 0; i < RcFx.count(3.0F * p); i++) {
            RcFx.at(RcFx.ASH, false, 30, at.m_82520_((Math.random() - 0.5) * 0.8, (Math.random() - 0.5) * 0.8, (Math.random() - 0.5) * 0.8))
                .velocity(new Vec3((Math.random() - 0.5) * 0.04, 0.02, (Math.random() - 0.5) * 0.04)).colors(0xE0201C1B, 0x00100E0E).size(0.08F, 0.04F)
                .fall(0.004F).wander(0.012F).spawn();
        }
    }

    /** The ground struck: dust rolling out low all round, earth thrown up and falling back, the view of all near kicked. */
    static void quake(Vec3 at, float p) {
        int n = RcFx.count(10.0F * p);
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2.0 * i / Math.max(1, n) + Math.random() * 0.4;
            Vec3 out = new Vec3(Math.cos(a), 0.0, Math.sin(a));
            RcFx.at(RcFx.PUFF, false, 26 + (int)(Math.random() * 12), at.m_82549_(out.m_82490_(0.3)).m_82520_(0.0, 0.15, 0.0))
                .velocity(out.m_82490_(0.12 + 0.08 * p * Math.random()).m_82520_(0.0, 0.01, 0.0)).colors(0xB06E655A, 0x006E655A).size(0.45F * p, 1.3F * p)
                .drag(0.86F).spawn();
        }
        for (int i = 0; i < RcFx.count(12.0F * p); i++) {
            double a = Math.random() * Math.PI * 2.0;
            Vec3 v = new Vec3(Math.cos(a) * (0.05 + Math.random() * 0.12), 0.2 + Math.random() * 0.25 * p, Math.sin(a) * (0.05 + Math.random() * 0.12));
            RcFx.at(RcFx.ASH, false, 22 + (int)(Math.random() * 10), at.m_82520_(0.0, 0.2, 0.0)).velocity(v).colors(0xFF4A423A, 0x004A423A)
                .size(0.1F + (float)Math.random() * 0.08F, 0.08F).fall(0.045F).drag(0.98F).spawn();
        }
        kick(at, 0.6F * p, 14.0);
    }

    /** The view of this client's player kicked by something landing {@code at}, harder the nearer, nothing past {@code reach}. */
    static void kick(Vec3 at, float strength, double reach) {
        Entity self = Minecraft.m_91087_().f_91074_;
        if (self == null) {
            return;
        }
        double away = self.m_20182_().m_82554_(at);
        if (away < reach) {
            EtoCamera.kick((float)Math.min(1.0, strength * (1.0 - away / reach)), EtoClient.clock(0.0F));
        }
    }

    // ------------------------------------------------------------------ War's RC spikes

    /** A spike let fly from {@code from} toward {@code target} (as the server lets it fly from that rinkaku's point). */
    public static void spike(Vec3 from, Vec3 toward, int target) {
        if (!RcFx.near(from, FAR) || FLIGHTS.size() > 64) {
            return;
        }
        Vec3 d = toward.m_82546_(from);
        FLIGHTS.add(new Flight(from, d.m_82556_() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : d.m_82541_(), target));
    }

    /** Each client tick: the spikes fly on (bending toward their prey as the server's do), embers shed behind them. */
    public static void tick(ClientLevel level) {
        if (level == null) {
            FLIGHTS.clear();
            LOCAL.clear();
            return;
        }
        long now = level.m_46467_();
        LOCAL.removeIf(mark -> now - mark.born() > KingMarkFx.LASTS + 1);
        for (Iterator<Flight> it = FLIGHTS.iterator(); it.hasNext(); ) {
            Flight f = it.next();
            Entity prey = f.target == 0 ? null : level.m_6815_(f.target);
            if (prey != null) {
                Vec3 c = prey.m_20182_().m_82520_(0.0, prey.m_20206_() * 0.5, 0.0);
                Vec3 to = c.m_82546_(f.at);
                if (to.m_82556_() > 1.0E-6) {
                    f.dir = f.dir.m_82490_(0.65).m_82549_(to.m_82541_().m_82490_(0.35)).m_82541_();
                }
            }
            f.was = f.at;
            f.at = f.at.m_82549_(f.dir.m_82490_(WarRules.SPIKE_SPEED));
            f.life++;
            for (int k = 0; k < 2; k++) {
                Vec3 p = f.was.m_82549_(f.at.m_82546_(f.was).m_82490_(Math.random()));
                RcFx.at(RcFx.EMBER, true, 10, p).velocity(new Vec3((Math.random() - 0.5) * 0.03, 0.01, (Math.random() - 0.5) * 0.03)).colors(0xFFFF3A40, 0x00600008)
                    .size(0.07F, 0.02F).spawn();
            }
            boolean met = prey != null && prey.m_20182_().m_82520_(0.0, prey.m_20206_() * 0.5, 0.0).m_82554_(f.at) < 0.6 + prey.m_20205_() * 0.5;
            if (met || f.life >= WarRules.SPIKE_LIFE) {
                it.remove();
            }
        }
    }

    /** The spikes in flight this frame (a burning crimson streak each, white-hot at its head), and this client's own marks. */
    public static void draw(UkakuModel.Sink sink, float partial, Vec3 camera, double now) {
        double[] eye = {camera.f_82479_, camera.f_82480_, camera.f_82481_};
        for (Local mark : LOCAL) {
            KingMarkFx.draw(sink, mark.style(), mark.at(), mark.way(), mark.power(), now - mark.born(), mark.seed(), eye);
        }
        for (Flight f : FLIGHTS) {
            Vec3 head = f.was.m_82549_(f.at.m_82546_(f.was).m_82490_(partial));
            Vec3 tail = head.m_82546_(f.dir.m_82490_(1.6));
            KingMarkFx.bolt(sink, new double[] {tail.f_82479_, tail.f_82480_, tail.f_82481_}, new double[] {head.f_82479_, head.f_82480_, head.f_82481_}, eye);
        }
    }
}
