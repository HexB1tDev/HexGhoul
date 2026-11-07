package com.hex.kagune.control.client;

import com.hex.kagune.client.KaguneClientState;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.RinkakuForms;
import com.hex.kagune.control.RinkakuImpale;
import com.hex.kagune.control.RinkakuRoot;
import com.hex.kagune.control.network.C2SUkakuAction;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CImpale;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.1.5: Rinkaku's Impale on the client ({@link RinkakuImpale}): H held with no tail holding anyone is told to the server (pressed, a ping
 * every few ticks, let go; with someone held H stays the crush), and every Impale it is told of ({@link S2CImpale}) is drawn over the tail's
 * own motion ({@link #apply}, from {@code StrikePaths}): up over its shoulder and straight out along its line, eased between the server's
 * ticks, its end drawn to a point ({@link #sharp}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class ImpaleClient {
    private ImpaleClient() {
    }

    /** One player's Impale as drawn. */
    private static final class Spear {
        int limb;
        double startedAt;
        double heardAt;
        double fromLength;
        double length;
        double[] fromWay;
        double[] way;
        double backAt = Double.NaN;
        double backFrom;
    }

    private static final Map<Integer, Spear> SPEARS = new HashMap<>();
    /** Ticks it is drawn on without hearing from the server before it is dropped (the server tells every tick it is out). */
    private static final double STALE = 12.0;
    private static boolean held;
    private static boolean sent;
    private static int pinged;

    public static void accept(S2CImpale message) {
        double now = ControlClient.worldTime(0.0F);
        if (message.phase() == S2CImpale.GONE) {
            SPEARS.remove(message.player());
            return;
        }
        double[] way = {message.x(), message.y(), message.z()};
        Spear spear = SPEARS.get(message.player());
        if (spear == null || spear.limb != message.limb()) {
            spear = new Spear();
            spear.limb = message.limb();
            spear.startedAt = now;
            spear.heardAt = now;
            spear.fromLength = 0.0;
            spear.length = 0.0;
            spear.fromWay = way;
            spear.way = way;
            SPEARS.put(message.player(), spear);
            if (SPEARS.size() > 64) {
                SPEARS.clear();
                SPEARS.put(message.player(), spear);
            }
        }
        if (message.phase() == S2CImpale.BACK) {
            if (Double.isNaN(spear.backAt)) {
                spear.backAt = now;
                spear.backFrom = message.length();
                spear.way = way;
                spear.fromWay = way;
            }
            return;
        }
        // Eased on from wherever it is drawn now toward what the server says.
        double f = ease(now - spear.heardAt);
        spear.fromLength = spear.fromLength + (spear.length - spear.fromLength) * f;
        spear.fromWay = mix(spear.fromWay, spear.way, f);
        spear.length = message.length();
        spear.way = way;
        spear.heardAt = now;
    }

    private static double ease(double sinceHeard) {
        return Math.max(0.0, Math.min(1.0, sinceHeard));
    }

    private static double[] mix(double[] a, double[] b, double f) {
        double[] m = {a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f};
        double l = Math.sqrt(m[0] * m[0] + m[1] * m[1] + m[2] * m[2]);
        return l < 1.0E-9 ? b : new double[] {m[0] / l, m[1] / l, m[2] / l};
    }

    // ---------------------------------------------------------------- his own key

    /**
     * True when this client's player could impale: a Rinkaku with the tails out, not the first kakuja. Whether a tail holds someone (then H
     * is the crush) is the server's to say ({@code ImpaleServer.start}): it alone knows exactly what the crush would take.
     */
    private static boolean ready(LocalPlayer player) {
        if (PathClient.get(player) != KagunePath.RINKAKU) {
            return false;
        }
        KaguneClientState.View view = KaguneClientState.self();
        return view.unlocked() && view.active() && !RinkakuForms.autonomous(view.form());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91074_ == null || minecraft.f_91080_ != null) {
            if (held) {
                held = false;
                if (sent && minecraft.f_91074_ != null) {
                    ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(C2SUkakuAction.IMPALE, 0));
                }
                sent = false;
            }
            if (minecraft.f_91073_ == null) {
                SPEARS.clear();
            }
            return;
        }
        boolean down = com.hex.kagune.client.KaguneKeys.CRUSH.m_90857_();
        if (down && !held) {
            held = true;
            pinged = 0;
            // The original's own key handler sends the crush as well; with someone held the server crushes and ignores this.
            sent = ready(minecraft.f_91074_);
            if (sent) {
                ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(C2SUkakuAction.IMPALE, 2));
            }
        } else if (down && sent && ++pinged % 4 == 0) {
            ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(C2SUkakuAction.IMPALE, 1));
        } else if (!down && held) {
            held = false;
            if (sent) {
                ControlNetwork.CHANNEL.sendToServer(new C2SUkakuAction(C2SUkakuAction.IMPALE, 0));
            }
            sent = false;
        }
    }

    // ---------------------------------------------------------------- drawn

    /**
     * Tail {@code limb} of {@code owner}'s {@code path} (world, root first) while it impales, or null when it isn't: {@code fanSide} its
     * layout's, {@code feet} his feet, {@code yaw} his body yaw in radians, {@code time} the world's ticks. {@link #sharp} then says how
     * far its end is drawn to a point.
     */
    public static double[][] apply(double[][] path, int limb, Entity owner, double fanSide, double[] feet, double yaw, double time) {
        if (SPEARS.isEmpty() || path == null || path.length < 4 || owner == null) {
            return null;
        }
        Spear spear = SPEARS.get(owner.m_19879_());
        if (spear == null || spear.limb != limb) {
            return null;
        }
        if (Double.isNaN(spear.backAt) && time - spear.heardAt > STALE) {
            SPEARS.remove(owner.m_19879_());
            return null;
        }
        double length;
        double[] way;
        double weight;
        if (Double.isNaN(spear.backAt)) {
            double f = ease(time - spear.heardAt);
            length = spear.fromLength + (spear.length - spear.fromLength) * f;
            way = mix(spear.fromWay, spear.way, f);
            // The first two ticks it rises out of its own motion into the line.
            weight = Math.max(0.0, Math.min(1.0, (time - spear.startedAt) / 2.0));
        } else {
            double age = time - spear.backAt;
            if (age >= RinkakuImpale.RETRACT) {
                return null;
            }
            length = spear.backFrom * RinkakuImpale.back(age);
            way = spear.way;
            // Its last two ticks home it goes back into its own motion.
            weight = Math.max(0.0, Math.min(1.0, (RinkakuImpale.RETRACT - age) / 2.0));
        }
        if (!(weight > 0.0)) {
            return null;
        }
        double side = RinkakuRoot.side(limb, fanSide);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        Minecraft minecraft = Minecraft.m_91087_();
        boolean own = owner == minecraft.f_91074_ && minecraft.f_91066_.m_92176_().m_90612_();
        double[] launch = RinkakuImpale.launch(feet, owner.m_20206_(), side, rightX, rightZ, forwardX, forwardZ, own);
        double[][] spearPath = RinkakuImpale.spear(path[0], launch, way, length, path.length, side, rightX, rightZ, forwardX, forwardZ);
        lastSharp = weight;
        return RinkakuImpale.blend(path, spearPath, weight);
    }

    private static double lastSharp;

    /** How far the last tail {@link #apply} drew is drawn to a point (0 .. 1). */
    public static double sharp() {
        return lastSharp;
    }
}
