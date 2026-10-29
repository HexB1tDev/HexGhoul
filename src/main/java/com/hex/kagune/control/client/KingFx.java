package com.hex.kagune.control.client;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.ukaku.UkakuModel;
import com.hex.kagune.control.client.fx.RcFx;
import com.hex.kagune.control.king.KingFxModel;
import com.hex.kagune.control.king.WarRules;
import com.hex.kagune.control.king.WarStrike;
import com.hex.kagune.control.network.S2CKingFx;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.hex.kagune.control.king.KingForm;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 1.0.13: what War's abilities do out in the world, on every client near ({@code S2CKingFx}): kept for as long as each lasts and
 * drawn every frame from {@link KingFxModel} (the black centipedes of King's Execution, the cleave, the Spire Wave, the Black Sun,
 * its rain and its crescent, the cocoon's burst), with RC particles ({@link RcFx}) on top. The Black Sun swallows in darkness the
 * player at the keys while they are under it ({@link #darkness}); what the cleave kills is cut in half ({@link KingHalves}). 1.0.14:
 * and his M1's blows: the blade's arcs swung with him, the shock where a fist or a foot lands, the Verdict's rinkaku.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class KingFx {
    private static final RenderType SHAPES = RenderType.m_110473_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    private static final RenderType LIGHT = RenderType.m_110488_(Objects.requireNonNull(KaguneMod.id("textures/misc/ukaku_white.png")));
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final double FAR = 160.0;

    /** One effect: its kind, whose it is, what and where (as the server said), and when it came (world ticks). */
    private record Fx(int kind, UUID owner, int[] ids, float[] data, double at) {
        double age(double now) {
            return now - this.at;
        }

        Vec3 point(int k) {
            return new Vec3(this.data[k * 3], this.data[k * 3 + 1], this.data[k * 3 + 2]);
        }
    }

    private static final List<Fx> ACTIVE = new ArrayList<>();

    private KingFx() {
    }

    /** How long each kind lasts (ticks). */
    static int lasts(int kind) {
        return switch (kind) {
            case S2CKingFx.GRASP -> WarRules.GRASP_TICKS;
            case S2CKingFx.CRUSH -> 10;
            case S2CKingFx.DASH -> 10;
            case S2CKingFx.CLEAVE -> 12;
            case S2CKingFx.SPIRE -> WarRules.SPIRE_LENGTH + 3 + WarRules.SPIRE_STAND + 8;
            case S2CKingFx.BLINK -> 8;
            case S2CKingFx.BURST -> WarRules.BURST_TICKS + 4;
            case S2CKingFx.SUN -> WarRules.SUN_TICKS + 4;
            case S2CKingFx.SEIZE -> WarRules.SUN_TICKS - WarRules.SUN_OPEN + 2;
            case S2CKingFx.SLAM -> 16;
            case S2CKingFx.PEACE_HIT -> 6;
            case S2CKingFx.RAIN -> 12;
            case S2CKingFx.SWING -> WarStrike.VERDICT.duration + 4;
            case S2CKingFx.STRIKE -> 16;
            case S2CKingFx.MARK -> com.hex.kagune.control.king.KingMarkFx.LASTS;
            default -> Math.max(1, Math.max(Math.max(com.hex.kagune.control.client.fx.WarPathFx.lasts(kind), com.hex.kagune.control.client.fx.WarHoldFx.lasts(kind)),
                com.hex.kagune.control.client.fx.PeaceFx.lasts(kind)));
        };
    }

    public static void accept(S2CKingFx message) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91073_ == null) {
            return;
        }
        double now = ControlClient.worldTime(0.0F);
        Fx fx = new Fx(message.kind(), message.playerId(), message.ids(), message.data(), now);
        ACTIVE.add(fx);
        // 1.0.27: the warpath's own (the spike cone, the blitz, the crater, the Black Sun's ring).
        com.hex.kagune.control.client.fx.WarPathFx.accept(message.kind(), message.playerId(), message.ids(), message.data(), now);
        com.hex.kagune.control.client.fx.WarHoldFx.accept(message.kind(), message.playerId(), message.ids(), message.data(), now); // 1.0.28
        com.hex.kagune.control.client.fx.PeaceFx.accept(message.kind(), message.playerId(), message.ids(), message.data(), now); // 1.0.34
        switch (message.kind()) {
            case S2CKingFx.CRUSH -> com.hex.kagune.control.client.fx.KingMarks.strike(minecraft.f_91073_, com.hex.kagune.control.king.KingMarkFx.FLARE, fx.point(0), Vec3.f_82478_, 1.2F, null);
            case S2CKingFx.DASH -> trail(fx.point(0), fx.point(1));
            case S2CKingFx.CLEAVE -> {
                Vec3 at = fx.point(0);
                Vec3 face = new Vec3(fx.data()[3], 0.0, fx.data()[5]);
                KingHalves.cut(message.ids(), face.f_82479_, face.f_82481_, now); // 1.0.19: its blood and dust come as marks
                com.hex.kagune.control.client.fx.KingMarks.strike(minecraft.f_91073_, com.hex.kagune.control.king.KingMarkFx.FLARE, at.m_82549_(face.m_82490_(1.6)).m_82520_(0.0, 0.9, 0.0), face, 1.6F, null);
            }
            case S2CKingFx.BLINK -> {
                trail(fx.point(0), fx.point(1));
                com.hex.kagune.control.client.fx.KingMarks.strike(minecraft.f_91073_, com.hex.kagune.control.king.KingMarkFx.SOOT, fx.point(0), new Vec3(0.0, 1.0, 0.0), 1.2F, null);
                com.hex.kagune.control.client.fx.KingMarks.strike(minecraft.f_91073_, com.hex.kagune.control.king.KingMarkFx.SOOT, fx.point(1), new Vec3(0.0, 1.0, 0.0), 1.2F, null);
            }
            case S2CKingFx.RAIN -> com.hex.kagune.control.client.fx.KingMarks.strike(minecraft.f_91073_, com.hex.kagune.control.king.KingMarkFx.BLOOD, fx.point(0), new Vec3(0.0, -1.0, 0.0), 0.8F, null);
            case S2CKingFx.STRIKE -> struck(minecraft, fx);
            case S2CKingFx.MARK -> {
                if (message.ids().length >= 2 && message.data().length >= 9) {
                    Entity body = message.ids()[1] == 0 ? null : minecraft.f_91073_.m_6815_(message.ids()[1]);
                    com.hex.kagune.control.client.fx.KingMarks.play(minecraft.f_91073_, message.ids()[0], fx.point(0), fx.point(1), message.data()[6], body);
                }
            }
            case S2CKingFx.PEACE_HIT -> {
                // A brief pale flash where a blade met it, a few crimson fragments.
                for (int i = 0; i * 3 + 2 < fx.data().length; i++) {
                    Vec3 at = fx.point(i);
                    Entity body = i < message.ids().length ? minecraft.f_91073_.m_6815_(message.ids()[i]) : null;
                    Entity owner = player(minecraft.f_91073_, message.playerId());
                    Vec3 way = owner == null ? Vec3.f_82478_ : at.m_82546_(owner.m_20182_().m_82520_(0.0, 1.2, 0.0));
                    com.hex.kagune.control.client.fx.KingMarks.strike(minecraft.f_91073_, com.hex.kagune.control.king.KingMarkFx.GLINT, at, way, 0.8F, null);
                    com.hex.kagune.control.client.fx.KingMarks.strike(minecraft.f_91073_, com.hex.kagune.control.king.KingMarkFx.BLOOD, at, way, 0.8F, body);
                }
                LocalPlayer self = minecraft.f_91074_;
                if (self != null && self.m_20148_().equals(message.playerId()) && fx.data().length >= 3) {
                    PaleCamera.kick(0.25F, PaleClient.clock(0.0F)); // 1.0.20: his own blades landing, felt
                }
            }

            default -> {
            }
        }
    }

    /**
     * 1.0.14: a blow of War's M1 landed: sparks and blood out of every body it met (sprayed along the cut for the blade), and for the
     * Verdict the ground burst open and the view of everyone near it kicked.
     */
    private static void struck(Minecraft minecraft, Fx fx) {
        if (fx.ids().length == 0 || fx.data().length < 6) {
            return;
        }
        WarStrike blow = WarStrike.byOrdinal(fx.ids()[0]);
        Vec3 facing = new Vec3(fx.data()[3], 0.0, fx.data()[5]);
        int hits = fx.ids().length - 1;
        boolean blade = blow.kind == WarStrike.Kind.BLADE || blow == WarStrike.VERDICT;
        for (int i = 0; i < hits && 2 + i < fx.data().length / 3; i++) {
            Vec3 at = fx.point(2 + i);
            Entity body = minecraft.f_91073_.m_6815_(fx.ids()[1 + i]);
            if (blade) {
                // 1.0.19: blood thrown on along the cut, a red flash and sparks.
                com.hex.kagune.control.client.fx.KingMarks.strike(minecraft.f_91073_, com.hex.kagune.control.king.KingMarkFx.BLOOD, at, facing, blow.heavy() ? 1.4F : 1.0F, body);
            } else {
                // A fist or a foot: a hot flash, a ring of shock, a little blood.
                com.hex.kagune.control.client.fx.KingMarks.strike(minecraft.f_91073_, com.hex.kagune.control.king.KingMarkFx.FLARE, at, facing, blow.heavy() ? 1.2F : 0.8F, null);
                com.hex.kagune.control.client.fx.KingMarks.strike(minecraft.f_91073_, com.hex.kagune.control.king.KingMarkFx.BLOOD, at, facing, 0.5F, body);
            }
        }
        LocalPlayer self = minecraft.f_91074_;
        if (blow == WarStrike.VERDICT) {
            Vec3 ground = fx.point(fx.data().length / 3 - 1);
            com.hex.kagune.control.client.fx.KingMarks.strike(minecraft.f_91073_, com.hex.kagune.control.king.KingMarkFx.QUAKE, ground, Vec3.f_82478_, 1.8F, null);
            if (self != null) {
                double away = self.m_20182_().m_82554_(ground);
                if (self.m_20148_().equals(fx.owner()) || away < 10.0) {
                    PaleCamera.kick(self.m_20148_().equals(fx.owner()) ? 0.9F : (float)(0.8 * (1.0 - away / 10.0)), PaleClient.clock(0.0F));
                }
            }
        } else if (hits > 0 && blow.heavy() && self != null && self.m_20148_().equals(fx.owner())) {
            PaleCamera.kick(0.35F, PaleClient.clock(0.0F));
        }
    }

    /** The Black Sun's darkness on the player at the keys now (0..1): under it, as the sun rises, and gone as it ends. */
    public static double darkness() {
        Minecraft minecraft = Minecraft.m_91087_();
        LocalPlayer self = minecraft.f_91074_;
        if (self == null) {
            return 0.0;
        }
        double now = ControlClient.worldTime(0.0F);
        double most = 0.0;
        for (Fx fx : ACTIVE) {
            if (fx.kind() != S2CKingFx.SUN || fx.owner().equals(self.m_20148_())) {
                continue;
            }
            if (self.m_20182_().m_82554_(fx.point(0)) > WarRules.SUN_DARK) {
                continue;
            }
            double age = fx.age(now);
            double in = Math.min(1.0, age / WarRules.SUN_OPEN);
            double out = Math.min(1.0, Math.max(0.0, (WarRules.SUN_TICKS - age) / 10.0));
            most = Math.max(most, in * out);
        }
        return most;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        comings();
        goings();
        com.hex.kagune.control.client.fx.KingMarks.tick(Minecraft.m_91087_().f_91073_);
        if (ACTIVE.isEmpty()) {
            return;
        }
        double now = ControlClient.worldTime(0.0F);
        Iterator<Fx> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Fx fx = it.next();
            double age = fx.age(now);
            if (age > lasts(fx.kind())) {
                it.remove();
                continue;
            }
            com.hex.kagune.control.client.fx.WarPathFx.tick(fx.kind(), fx.owner(), fx.ids(), fx.data(), age, now);
            com.hex.kagune.control.client.fx.WarHoldFx.tick(fx.kind(), fx.owner(), fx.ids(), fx.data(), age, now);
            if (fx.kind() == S2CKingFx.SUN && age < WarRules.SUN_OPEN && Math.random() < 0.9) {
                // 1.0.15: the ground cracking open: black dust and red light seeping up out of the cracks.
                double a = Math.random() * Math.PI * 2.0;
                double r = Math.sqrt(Math.random()) * WarRules.SUN_RADIUS * Math.min(1.0, age / WarRules.SUN_OPEN + 0.2);
                Vec3 at = fx.point(0).m_82520_(Math.cos(a) * r, 0.1, Math.sin(a) * r);
                RcFx.at(RcFx.EMBER, true, 24, at).velocity(new Vec3(0.0, 0.08 + Math.random() * 0.08, 0.0)).colors(RcFx.RED, RcFx.clear(RcFx.CRIMSON))
                    .size(0.22F, 0.05F).wander(0.02F).spawn();
            }
        }
    }


    /** The dash's trail: black streaks and red embers all along the way, left hanging. */
    static void trail(Vec3 from, Vec3 to) {
        if (!RcFx.near(from, FAR)) {
            return;
        }
        Vec3 d = to.m_82546_(from);
        int n = RcFx.count(36);
        for (int i = 0; i < n; i++) {
            double t = Math.random();
            Vec3 at = from.m_82549_(d.m_82490_(t)).m_82520_((Math.random() - 0.5) * 0.6, (Math.random() - 0.5) * 1.2, (Math.random() - 0.5) * 0.6);
            boolean red = i % 3 == 0;
            RcFx.at(red ? RcFx.EMBER : RcFx.STREAK, red, 10 + (int)(Math.random() * 8), at).velocity(d.m_82541_().m_82490_(0.05))
                .colors(red ? RcFx.RED : 0xD0141012, red ? RcFx.clear(RcFx.CRIMSON) : 0x00141012).size(red ? 0.18F : 0.5F, 0.05F).drag(0.85F).spawn();
        }
    }

    /** Where step {@code step} of a Spire Wave is (on the ground). */
    static Vec3 spireStep(Fx fx, int step) {
        float[] d = fx.data();
        double x = d[0] + d[3] * (1.6 + step);
        double z = d[2] + d[4] * (1.6 + step);
        double y = 5 + step < d.length ? d[5 + step] : d[1];
        return new Vec3(x, y, z);
    }

    // ------------------------------------------------------------------ drawing

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void render(RenderLevelStageEvent event) {
        // 1.0.17: not only while something sent is showing: Peace's trails (and the comings) are each client's own.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null) {
            return;
        }
        float partial = event.getPartialTick();
        double now = ControlClient.worldTime(partial);
        Vec3 camera = minecraft.f_91063_.m_109153_().m_90583_();
        PoseStack stack = event.getPoseStack();
        Matrix4f matrix = stack.m_85850_().m_252922_();
        Matrix3f normals = stack.m_85850_().m_252943_();
        MultiBufferSource.BufferSource buffers = minecraft.m_91269_().m_110104_();
        List<float[]> lit = new ArrayList<>();
        List<float[]> glow = new ArrayList<>();
        for (Fx fx : ACTIVE) {
            Vec3 where = fx.data().length >= 3 ? fx.point(0) : camera;
            if (where.m_82557_(camera) > FAR * FAR) {
                continue;
            }
            int light = LevelRenderer.m_109541_(level, BlockPos.m_274561_(where.f_82479_, where.f_82480_ + 1.0, where.f_82481_));
            UkakuModel.Sink sink = (a, b, c, d, n, ca, cb, cc, cd) -> {
                boolean burns = n[0] == 0.0 && n[1] == 1.0 && n[2] == 0.0;
                float[] q = {(float)(a[0] - camera.f_82479_), (float)(a[1] - camera.f_82480_), (float)(a[2] - camera.f_82481_), (float)(b[0] - camera.f_82479_),
                    (float)(b[1] - camera.f_82480_), (float)(b[2] - camera.f_82481_), (float)(c[0] - camera.f_82479_), (float)(c[1] - camera.f_82480_),
                    (float)(c[2] - camera.f_82481_), (float)(d[0] - camera.f_82479_), (float)(d[1] - camera.f_82480_), (float)(d[2] - camera.f_82481_),
                    (float)n[0], (float)n[1], (float)n[2], ca, cb, cc, cd, light};
                (burns ? glow : lit).add(q);
            };
            draw(sink, fx, level, now, partial, camera);
        }
        // 1.0.19: War's RC spikes in flight, and the marks this client made itself.
        com.hex.kagune.control.client.fx.KingMarks.draw((a, b, c, d, n, ca, cb, cc, cd) -> {
            boolean burns = n[0] == 0.0 && n[1] == 1.0 && n[2] == 0.0;
            int light = burns ? FULL_BRIGHT : LevelRenderer.m_109541_(level, BlockPos.m_274561_(a[0], a[1] + 0.5, a[2]));
            (burns ? glow : lit).add(new float[] {(float)(a[0] - camera.f_82479_), (float)(a[1] - camera.f_82480_), (float)(a[2] - camera.f_82481_),
                (float)(b[0] - camera.f_82479_), (float)(b[1] - camera.f_82480_), (float)(b[2] - camera.f_82481_), (float)(c[0] - camera.f_82479_),
                (float)(c[1] - camera.f_82480_), (float)(c[2] - camera.f_82481_), (float)(d[0] - camera.f_82479_), (float)(d[1] - camera.f_82480_),
                (float)(d[2] - camera.f_82481_), (float)n[0], (float)n[1], (float)n[2], ca, cb, cc, cd, light});
        }, partial, camera, now);
        // (1.0.16 drew Peace's blades thin trails along the paths they cut; 1.0.34: gone, the user's call: added over the ground they read
        // as yellow streaks.)
        for (net.minecraft.client.player.AbstractClientPlayer player : level.m_6907_()) {
            if (player.m_20182_().m_82557_(camera) > FAR * FAR) {
                continue;
            }
            // 1.0.17: a final kakuja coming over him (KingTransform), and what it leaves.
            UkakuModel.Sink both = (a, b, c, d, n, ca, cb, cc, cd) -> {
                boolean burns = n[0] == 0.0 && n[1] == 1.0 && n[2] == 0.0;
                int light = burns ? FULL_BRIGHT : LevelRenderer.m_109541_(level, BlockPos.m_274561_(a[0], a[1] + 0.5, a[2]));
                (burns ? glow : lit).add(new float[] {(float)(a[0] - camera.f_82479_), (float)(a[1] - camera.f_82480_), (float)(a[2] - camera.f_82481_),
                    (float)(b[0] - camera.f_82479_), (float)(b[1] - camera.f_82480_), (float)(b[2] - camera.f_82481_), (float)(c[0] - camera.f_82479_),
                    (float)(c[1] - camera.f_82480_), (float)(c[2] - camera.f_82481_), (float)(d[0] - camera.f_82479_), (float)(d[1] - camera.f_82480_),
                    (float)(d[2] - camera.f_82481_), (float)n[0], (float)n[1], (float)n[2], ca, cb, cc, cd, light});
            };
            coming(both, player, partial, now, camera);
            // 1.0.28: what War's held moves put out of his body (Black Reaper's centipedes, the Spire Wave's round his arm).
            com.hex.kagune.control.client.fx.WarHoldFx.player(both, player, partial, now, camera);
            // 1.0.34: what Peace's moves grow out of his back (the Aegis's shields, the Ascent's two tendrils).
            com.hex.kagune.control.client.fx.PeaceFx.player(both, player, partial, now, camera);
        }
        // 1.0.34: the red flesh the Ascent's landing tears up out of the ground.
        com.hex.kagune.control.client.fx.PeaceFx.world((a, b, c, d, n, ca, cb, cc, cd) -> {
            boolean burns = n[0] == 0.0 && n[1] == 1.0 && n[2] == 0.0;
            int light = burns ? FULL_BRIGHT : LevelRenderer.m_109541_(level, BlockPos.m_274561_(a[0], a[1] + 0.5, a[2]));
            (burns ? glow : lit).add(new float[] {(float)(a[0] - camera.f_82479_), (float)(a[1] - camera.f_82480_), (float)(a[2] - camera.f_82481_),
                (float)(b[0] - camera.f_82479_), (float)(b[1] - camera.f_82480_), (float)(b[2] - camera.f_82481_), (float)(c[0] - camera.f_82479_),
                (float)(c[1] - camera.f_82480_), (float)(c[2] - camera.f_82481_), (float)(d[0] - camera.f_82479_), (float)(d[1] - camera.f_82480_),
                (float)(d[2] - camera.f_82481_), (float)n[0], (float)n[1], (float)n[2], ca, cb, cc, cd, light});
        }, level, partial, now, camera);
        VertexConsumer shapes = buffers.m_6299_(SHAPES);
        for (float[] q : lit) {
            for (int k = 0; k < 4; k++) {
                vertex(shapes, matrix, normals, q, k, (int)q[15 + k], (int)q[19]);
            }
        }
        buffers.m_109912_(SHAPES);
        // 1.0.14: what burns is drawn both ways round: the glow's type culls back faces, and a slash or a ring is seen from either side
        // (only one winding ever faces the camera, so nothing is drawn twice over).
        VertexConsumer lights = buffers.m_6299_(LIGHT);
        for (float[] q : glow) {
            for (int k = 0; k < 4; k++) {
                vertex(lights, matrix, normals, q, k, premultiplied((int)q[15 + k]), FULL_BRIGHT);
            }
            for (int k = 3; k >= 0; k--) {
                vertex(lights, matrix, normals, q, k, premultiplied((int)q[15 + k]), FULL_BRIGHT);
            }
        }
        buffers.m_109912_(LIGHT);
    }

    /** 1.0.17: where each coming is drawn round (its ground: followed until War's burst or Peace's light, then left where it was). */
    private static final Map<UUID, double[]> COMING_AT = new HashMap<>();

    /** {@code player}'s final kakuja coming over him, if it is (or what it left is still there), drawn {@code now}. */
    private static void coming(UkakuModel.Sink sink, net.minecraft.client.player.AbstractClientPlayer player, float partial, double now, Vec3 camera) {
        KingClient.View view = KingClient.get(player);
        if (view == null || !view.on()) {
            return;
        }
        double age = now - view.since();
        if (age < 0.0 || age > com.hex.kagune.control.king.KingComingFx.lasts(view.form())) {
            return;
        }
        double[] at = COMING_AT.get(player.m_20148_());
        if (at == null || at[3] != view.since()) {
            return;
        }
        double[] feet = {at[0], at[1], at[2]};
        double[] eye = {camera.f_82479_, camera.f_82480_, camera.f_82481_};
        long seed = player.m_20148_().getLeastSignificantBits() ^ view.since();
        if (view.form() == KingForm.WAR) {
            if (age < com.hex.kagune.control.king.KingTransform.WAR_BURST) {
                // The darkness pours into him where he is now.
                feet = new double[] {player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial, player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial,
                    player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial};
            }
            com.hex.kagune.control.king.KingComingFx.war(sink, feet, age, seed, eye, now);
        } else if (view.form() == KingForm.PEACE) {
            if (age < com.hex.kagune.control.king.KingTransform.PEACE_LIGHT) {
                feet = new double[] {player.f_19790_ + (player.m_20185_() - player.f_19790_) * partial, player.f_19791_ + (player.m_20186_() - player.f_19791_) * partial,
                    player.f_19792_ + (player.m_20189_() - player.f_19792_) * partial};
            }
            com.hex.kagune.control.king.KingComingFx.peace(sink, feet, age, seed, eye, now);
        }
    }

    /**
     * 1.0.17: each tick, for every final kakuja coming over a player near: where its ground is (followed until War's burst or Peace's
     * light), its motes (War's black smoke and blood, Peace's rising light), and War's burst kicking the view of everyone near.
     */
    private static void comings() {
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null) {
            COMING_AT.clear();
            return;
        }
        long tick = level.m_46467_();
        Entity self = minecraft.f_91074_;
        for (net.minecraft.client.player.AbstractClientPlayer player : level.m_6907_()) {
            KingClient.View view = KingClient.get(player);
            if (view == null || !view.on()) {
                continue;
            }
            long age = tick - view.since();
            if (age < 0L || age > com.hex.kagune.control.king.KingComingFx.lasts(view.form())) {
                continue;
            }
            double[] at = COMING_AT.get(player.m_20148_());
            long freeze = view.form() == KingForm.WAR ? com.hex.kagune.control.king.KingTransform.WAR_BURST : com.hex.kagune.control.king.KingTransform.PEACE_LIGHT;
            if (at == null || at[3] != view.since() || age <= freeze) {
                COMING_AT.put(player.m_20148_(), new double[] {player.m_20185_(), player.m_20186_(), player.m_20189_(), view.since()});
            }
            Vec3 chest = new Vec3(player.m_20185_(), player.m_20186_() + 1.2, player.m_20189_());
            if (!RcFx.near(chest, FAR)) {
                continue;
            }
            if (view.form() == KingForm.WAR) {
                long burst = com.hex.kagune.control.king.KingTransform.WAR_BURST;
                if (age < burst && Math.random() < 0.8) {
                    // Black smoke drawn in to him from all round.
                    double a = Math.random() * Math.PI * 2.0;
                    double r = 2.5 + Math.random() * 2.0;
                    Vec3 from = chest.m_82520_(Math.cos(a) * r, (Math.random() - 0.6) * 1.2, Math.sin(a) * r);
                    RcFx.at(RcFx.STREAK, false, 12, from).velocity(chest.m_82546_(from).m_82490_(0.08)).colors(0xD0060405, 0x00060405).size(0.6F, 0.2F).spawn();
                }
                if (age == burst) {
                    // 1.0.19: torn out of him: gore, black smoke and a hot flash (KingMarks), no glowing dots.
                    com.hex.kagune.control.client.fx.KingMarks.strike(level, com.hex.kagune.control.king.KingMarkFx.GORE, chest, new Vec3(0.0, 1.0, 0.0), 2.4F, player);
                    com.hex.kagune.control.client.fx.KingMarks.strike(level, com.hex.kagune.control.king.KingMarkFx.SOOT, chest, new Vec3(0.0, 1.0, 0.0), 2.2F, null);
                    com.hex.kagune.control.client.fx.KingMarks.strike(level, com.hex.kagune.control.king.KingMarkFx.FLARE, chest, Vec3.f_82478_, 2.0F, null);
                    if (self != null) {
                        double away = self.m_20182_().m_82554_(chest);
                        if (away < 14.0) {
                            PaleCamera.kick(self == player ? 1.0F : (float)(0.9 * (1.0 - away / 14.0)), PaleClient.clock(0.0F));
                        }
                    }
                }
                if (age > burst && age < burst + 20 && Math.random() < 0.7) {
                    // Darkness rising off him after.
                    Vec3 from = chest.m_82520_((Math.random() - 0.5) * 1.2, (Math.random() - 0.5) * 1.4, (Math.random() - 0.5) * 1.2);
                    RcFx.at(RcFx.MOTE, false, 22, from).velocity(new Vec3(0.0, 0.05 + Math.random() * 0.04, 0.0)).colors(0xC0080506, 0x00080506)
                        .size(0.55F, 0.9F).wander(0.02F).spawn();
                }
            } else if (view.form() == KingForm.PEACE && age < com.hex.kagune.control.king.KingTransform.PEACE_LIGHT && Math.random() < 0.6) {
                // Soft motes of light rising round him.
                double a = Math.random() * Math.PI * 2.0;
                double r = 0.6 + Math.random() * 0.9;
                Vec3 from = new Vec3(player.m_20185_() + Math.cos(a) * r, player.m_20186_() + Math.random() * 0.6, player.m_20189_() + Math.sin(a) * r);
                RcFx.at(RcFx.MOTE, true, 30, from).velocity(new Vec3(0.0, 0.035 + Math.random() * 0.02, 0.0)).colors(0xFFFFF6E4, 0x00FFF6E4)
                    .size(0.12F, 0.04F).wander(0.01F).spawn();
            }
        }
    }

    /**
     * 1.0.19: each tick, for every final kakuja leaving a player near (KingTransform): War crumbling to ash (black flakes falling off
     * him, the rinkaku's first and from farther out, a smoke of it rising, a last black breath out of him); Peace shedding feathers as its
     * wings wither, motes of light drawn back into him, a soft light at his heart at the end.
     */
    private static void goings() {
        Minecraft minecraft = Minecraft.m_91087_();
        ClientLevel level = minecraft.f_91073_;
        if (level == null) {
            return;
        }
        long tick = level.m_46467_();
        for (net.minecraft.client.player.AbstractClientPlayer player : level.m_6907_()) {
            KingClient.View view = KingClient.get(player);
            if (view == null || view.on()) {
                continue;
            }
            long age = tick - view.since();
            if (!com.hex.kagune.control.king.KingTransform.going(view.form(), false, age)) {
                continue;
            }
            Vec3 feet = player.m_20182_();
            Vec3 chest = feet.m_82520_(0.0, 1.2, 0.0);
            if (!RcFx.near(chest, FAR)) {
                continue;
            }
            double yaw = Math.toRadians(player.f_20883_);
            Vec3 back = new Vec3(Math.sin(yaw), 0.0, -Math.cos(yaw));
            Vec3 left = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
            if (view.form() == KingForm.WAR) {
                if (age >= 2L && age <= 28L) {
                    // Farther out at first (the rinkaku crumbling from their tips), in to his body at the last.
                    double out = 2.6 * Math.max(0.0, 1.0 - (age - 2.0) / 20.0) + 0.3;
                    int flakes = RcFx.count(4.0F);
                    for (int i = 0; i < flakes; i++) {
                        double a = Math.random() * Math.PI * 2.0;
                        double r = out * Math.sqrt(Math.random());
                        Vec3 from = chest.m_82549_(back.m_82490_(Math.abs(Math.cos(a)) * r * 0.8)).m_82549_(left.m_82490_(Math.sin(a) * r))
                            .m_82520_(0.0, (Math.random() - 0.4) * 1.4, 0.0);
                        RcFx.at(RcFx.ASH, false, 26 + (int)(Math.random() * 18), from)
                            .velocity(new Vec3((Math.random() - 0.5) * 0.04, 0.01 + Math.random() * 0.02, (Math.random() - 0.5) * 0.04))
                            .colors(0xF0201C1B, 0x00100E0E).size(0.09F + (float)Math.random() * 0.1F, 0.05F).fall(0.006F).wander(0.015F).spawn();
                    }
                    if (Math.random() < 0.6) {
                        Vec3 from = chest.m_82520_((Math.random() - 0.5) * 1.0, (Math.random() - 0.5) * 1.2, (Math.random() - 0.5) * 1.0);
                        RcFx.at(RcFx.PUFF, false, 30, from).velocity(new Vec3(0.0, 0.025 + Math.random() * 0.02, 0.0)).colors(0x90141212, 0x00141212)
                            .size(0.5F, 1.3F).wander(0.01F).spawn();
                    }
                }
                if (age == 24L) {
                    // A last black breath out of him.
                    double pitch = Math.toRadians(player.m_146909_());
                    Vec3 look = new Vec3(-Math.sin(Math.toRadians(player.f_20885_)) * Math.cos(pitch), -Math.sin(pitch) - 0.25,
                        Math.cos(Math.toRadians(player.f_20885_)) * Math.cos(pitch));
                    Vec3 mouth = feet.m_82520_(0.0, 1.5, 0.0).m_82549_(look.m_82490_(0.25));
                    for (int i = 0; i < RcFx.count(7.0F); i++) {
                        RcFx.at(RcFx.PUFF, false, 24, mouth).velocity(look.m_82490_(0.06 + Math.random() * 0.04).m_82520_((Math.random() - 0.5) * 0.03,
                            Math.random() * 0.02, (Math.random() - 0.5) * 0.03)).colors(0xB0100D0D, 0x00100D0D).size(0.25F, 0.8F).drag(0.9F).spawn();
                    }
                }
            } else if (view.form() == KingForm.PEACE) {
                if (age >= com.hex.kagune.control.king.KingTransform.PEACE_WITHER && age <= 28L) {
                    // Feathers off the withering wings, white and a few of crimson, drifting down slowly.
                    int feathers = RcFx.count(3.0F);
                    for (int i = 0; i < feathers; i++) {
                        double side = Math.random() < 0.5 ? -1.0 : 1.0;
                        double reach = 0.3 + Math.random() * 1.8;
                        Vec3 from = feet.m_82549_(left.m_82490_(side * reach)).m_82549_(back.m_82490_(0.3 + reach * 0.35))
                            .m_82520_(0.0, 0.5 + Math.random() * 2.2, 0.0);
                        boolean crimson = Math.random() < 0.3;
                        RcFx.at(RcFx.FEATHER, false, 50 + (int)(Math.random() * 30), from)
                            .velocity(new Vec3(left.f_82479_ * side * 0.02, 0.01, left.f_82481_ * side * 0.02))
                            .colors(crimson ? 0xFFA0182A : 0xFFF4F2F2, crimson ? 0x00A0182A : 0x00F4F2F2).size(0.2F + (float)Math.random() * 0.1F, 0.16F)
                            .fall(0.004F).drag(0.92F).wander(0.025F).spawn();
                    }
                }
                if (age >= 10L && age <= 30L && Math.random() < 0.7) {
                    // Motes of light drawn in to his heart from round him.
                    double a = Math.random() * Math.PI * 2.0;
                    Vec3 from = chest.m_82520_(Math.cos(a) * 1.3, (Math.random() - 0.3) * 1.2, Math.sin(a) * 1.3);
                    RcFx.at(RcFx.MOTE, true, 14, from).velocity(chest.m_82546_(from).m_82490_(0.07)).colors(0xFFFFF4E8, 0x00FFF4E8).size(0.1F, 0.03F).spawn();
                }
                if (age == 32L) {
                    RcFx.at(RcFx.MOTE, true, 10, chest.m_82520_(0.0, 0.1, 0.0)).colors(0xFFFFF6EC, 0x00FFF6EC).size(0.9F, 1.6F).spawn();
                    com.hex.kagune.control.client.fx.KingMarks.strike(level, com.hex.kagune.control.king.KingMarkFx.GLINT, chest, Vec3.f_82478_, 1.2F, null);
                }
            }
        }
    }

    /** One effect's shapes, {@code now} (world ticks, with the frame's share of one) into its life. */
    private static void draw(UkakuModel.Sink sink, Fx fx, ClientLevel level, double now, float partial, Vec3 camera) {
        double age = fx.age(now);
        float[] d = fx.data();
        switch (fx.kind()) {
            case S2CKingFx.MARK -> {
                if (fx.ids().length >= 1 && d.length >= 9 && com.hex.kagune.control.king.KingMarkFx.drawn(fx.ids()[0])) {
                    long seed = Double.doubleToLongBits(fx.at()) ^ Float.floatToIntBits(d[0]) * 31L ^ Float.floatToIntBits(d[2]);
                    com.hex.kagune.control.king.KingMarkFx.draw(sink, fx.ids()[0], new double[] {d[0], d[1], d[2]}, new double[] {d[3], d[4], d[5]}, d[6], age, seed,
                        new double[] {camera.f_82479_, camera.f_82480_, camera.f_82481_});
                }
            }
            case S2CKingFx.GRASP -> {
                Entity owner = player(level, fx.owner());
                if (owner == null) {
                    return;
                }
                double[] hand = hand(owner, partial);
                Entity target = fx.ids().length > 0 && fx.ids()[0] != 0 ? level.m_6815_(fx.ids()[0]) : null;
                double[] at;
                double radius;
                double height;
                if (target != null) {
                    at = centre(target, partial);
                    radius = target.m_20205_() * 0.5;
                    height = target.m_20206_();
                } else {
                    at = new double[] {d[0], d[1], d[2]};
                    radius = 0.3;
                    height = 1.0;
                }
                double reach = Math.min(1.0, age / WarRules.GRASP_REACH) * (1.0 - Math.max(0.0, (age - (WarRules.GRASP_TICKS - 8)) / 8.0));
                double coil = target == null ? 0.0 : Math.max(0.0, Math.min(1.0, (age - WarRules.GRASP_REACH) / (WarRules.GRASP_COIL - WarRules.GRASP_REACH)));
                double squeeze = Math.max(0.0, Math.min(1.0, (age - WarRules.GRASP_COIL) / (double)(WarRules.GRASP_CRUSH - WarRules.GRASP_COIL)));
                if (age > WarRules.GRASP_CRUSH) {
                    coil *= Math.max(0.0, 1.0 - (age - WarRules.GRASP_CRUSH) / 6.0);
                }
                for (int i = 0; i < WarRules.GRASP_ARMS; i++) {
                    double[][] path = KingFxModel.grasp(hand, at, radius, height, i, WarRules.GRASP_ARMS, reach, coil, squeeze, now);
                    KingFxModel.centipede(sink, path, 0.2, 31 + i, 0.3 + 0.7 * (1.0 - coil), true, 1.0, now * 0.4);
                }
            }
            case S2CKingFx.CLEAVE -> {
                double fade = 1.0 - age / lasts(fx.kind());
                double[] centre = {d[0], d[1] + 0.45, d[2]};
                double[] forward = {d[3], 0.0, d[5]};
                double length = Math.hypot(forward[0], forward[2]);
                if (length < 1.0E-6) {
                    return;
                }
                forward = new double[] {forward[0] / length, 0.0, forward[2] / length};
                double sweep = Math.min(1.0, age / 3.0);
                KingFxModel.slash(sink, centre, new double[] {0, 1, 0}, forward, 2.4, 0.4, 0.2, 0.2 + 2.3 * sweep, 0.95 * fade);
            }
            case S2CKingFx.SPIRE -> {
                for (int step = 0; step < WarRules.SPIRE_LENGTH; step++) {
                    double since = age - step;
                    if (since <= 0.0) {
                        break;
                    }
                    double out = Math.min(1.0, since / 3.0) * Math.min(1.0, Math.max(0.0, (3.0 + WarRules.SPIRE_STAND + 6.0 - since) / 6.0));
                    if (out <= 0.01) {
                        continue;
                    }
                    Vec3 at = spireStep(fx, step);
                    double fx0 = d[3];
                    double fz0 = d[4];
                    for (int k = 0; k < 2; k++) {
                        double[] lay = KingFxModel.spireAt(step, k);
                        if (lay == null) {
                            continue;
                        }
                        // Across the wave is (-z, x) of its way.
                        double[] base = {at.f_82479_ - fz0 * lay[0] + fx0 * lay[1], at.f_82480_, at.f_82481_ + fx0 * lay[0] + fz0 * lay[1]};
                        double[] lean = {-fz0 * lay[2] + fx0 * lay[3], 0.0, fx0 * lay[2] + fz0 * lay[3]};
                        KingFxModel.spike(sink, KingFxModel.spire(base, lean, lay[4], out, now, step * 3 + k), lay[5], step * 3 + k, now, 1.0);
                    }
                }
            }
            case S2CKingFx.BURST -> {
                double r = WarRules.BURST_RADIUS * Math.min(1.0, age / WarRules.BURST_TICKS);
                KingFxModel.crescent(sink, new double[] {d[0], d[1] - 1.0, d[2]}, r, 1.2, Math.max(0.0, 1.0 - age / lasts(fx.kind())), now);
            }
            case S2CKingFx.SUN -> {
                // 1.0.15: the crack racing out across the ground round the spot, and darkening there until it is over.
                double out = WarRules.SUN_RADIUS * Math.min(1.0, age / WarRules.SUN_OPEN);
                double fade = Math.min(1.0, Math.max(0.0, (WarRules.SUN_TICKS + 4 - age) / 10.0));
                KingFxModel.crescent(sink, new double[] {d[0], d[1] - 0.25, d[2]}, out, 0.45, 0.85 * fade, now);
            }
            case S2CKingFx.SEIZE -> {
                // A centipede up out of the ground for each body seized, coiled round it wherever it is held: up, high, slammed down.
                double since = age;
                double out = Math.min(1.0, since / 4.0);
                double after = since - (WarRules.SUN_SLAM - WarRules.SUN_OPEN);
                if (after > 0.0) {
                    out *= Math.max(0.0, 1.0 - after / 10.0);
                }
                for (int i = 0; i < fx.ids().length; i++) {
                    Entity body = level.m_6815_(fx.ids()[i]);
                    if (body == null) {
                        continue;
                    }
                    double[] centre = centre(body, partial);
                    double w = body.m_20205_() * 0.5;
                    double turn = i * 2.39996;
                    double[] base = {centre[0] + Math.cos(turn) * (w + 0.8), d[1], centre[2] + Math.sin(turn) * (w + 0.8)};
                    double[][] path = KingFxModel.seize(base, centre, w, body.m_20206_(), out, now, 40 + i);
                    KingFxModel.centipede(sink, path, 0.34, 40 + i, 0.4, true, 1.0, now * 0.4 + i);
                }
            }
            case S2CKingFx.SLAM -> {
                double r = 3.6 * Math.min(1.0, age / 5.0);
                KingFxModel.crescent(sink, new double[] {d[0], d[1] - 0.3, d[2]}, r, 0.9, Math.max(0.0, 1.0 - age / lasts(fx.kind())), now);
            }
            case S2CKingFx.RAIN -> {
                double out = age < 4.0 ? age / 4.0 : age < 8.0 ? 1.0 : Math.max(0.0, 1.0 - (age - 8.0) / 4.0);
                double[] target = {d[0], d[1], d[2]};
                double[] sky = {d[3], d[4], d[5]};
                int seed = fx.ids().length > 0 ? fx.ids()[0] : 0;
                KingFxModel.spike(sink, KingFxModel.rain(sky, target, out, seed), 0.55, 70 + seed, now, 1.0);
            }
            default -> {
                com.hex.kagune.control.client.fx.WarPathFx.draw(sink, fx.kind(), fx.ids(), fx.data(), age, now);
                com.hex.kagune.control.client.fx.WarHoldFx.draw(sink, fx.kind(), fx.owner(), fx.ids(), fx.data(), age, now, partial);
            }
        }
    }

    // ------------------------------------------------------------------ 1.0.16: Peace

    /**
     * {@code player}'s blades sent at things now, per blade (as {@link com.hex.kagune.control.king.PeaceMotion.State} takes them), or
     * null for none. 1.0.34: Peace's reworked moves' ({@link com.hex.kagune.control.client.fx.PeaceFx#aims}).
     */
    public static double[][] peaceAims(net.minecraft.client.player.AbstractClientPlayer player, double now) {
        return com.hex.kagune.control.client.fx.PeaceFx.aims(player, now);
    }

    /** 1.2.8: as above, for the frame {@code partial} of the way to the next tick (where the body is drawn). */
    public static double[][] peaceAims(net.minecraft.client.player.AbstractClientPlayer player, double now, float partial) {
        return com.hex.kagune.control.client.fx.PeaceFx.aims(player, now, partial);
    }

    /** A point in a Peace's body pixels out in the world, his feet at {@code feet}, facing {@code yaw} (radians, as the game turns). */
    /** 1.0.26: as below, the point first carried by his torso as posed ({@code a}: {@code BodyFrame.carry}), facing his body's {@code yaw}. */
    static double[] toWorld(double[] px, double[] feet, double yaw, double[] a) {
        double k = 1.0 / com.hex.kagune.control.king.PeaceMotion.BLOCK;
        double[] r = {-px[0] * k, px[1] * k, -px[2] * k};
        double x = a[0] * r[0] + a[1] * r[1] + a[2] * r[2] + a[9];
        double y = a[3] * r[0] + a[4] * r[1] + a[5] * r[2] + a[10];
        double z = a[6] * r[0] + a[7] * r[1] + a[8] * r[2] + a[11];
        return new double[] {feet[0] - Math.cos(yaw) * x - Math.sin(yaw) * z, feet[1] + y, feet[2] - Math.sin(yaw) * x + Math.cos(yaw) * z};
    }

    static double[] toWorld(double[] px, double[] feet, double yaw) {
        double k = 1.0 / com.hex.kagune.control.king.PeaceMotion.BLOCK;
        double[] left = {Math.cos(yaw), 0.0, Math.sin(yaw)};
        double[] forward = {-Math.sin(yaw), 0.0, Math.cos(yaw)};
        return new double[] {feet[0] + left[0] * px[0] * k - forward[0] * px[2] * k, feet[1] + px[1] * k, feet[2] + left[2] * px[0] * k - forward[2] * px[2] * k};
    }

    /** The player {@code id} in {@code level}, or null. */
    static Entity player(ClientLevel level, UUID id) {
        for (net.minecraft.client.player.AbstractClientPlayer p : level.m_6907_()) {
            if (p.m_20148_().equals(id)) {
                return p;
            }
        }
        return null;
    }

    /** Where {@code owner}'s free hand is, thrown out in front of him (the left arm, at the shoulder's height). */
    static double[] hand(Entity owner, float partial) {
        double x = owner.f_19790_ + (owner.m_20185_() - owner.f_19790_) * partial;
        double y = owner.f_19791_ + (owner.m_20186_() - owner.f_19791_) * partial;
        double z = owner.f_19792_ + (owner.m_20189_() - owner.f_19792_) * partial;
        double yaw = Math.toRadians(owner instanceof net.minecraft.world.entity.LivingEntity living ? living.f_20883_ : owner.m_146908_());
        double k = 0.9375 / 16.0;
        double[] left = {Math.cos(yaw), 0.0, Math.sin(yaw)};
        double[] forward = {-Math.sin(yaw), 0.0, Math.cos(yaw)};
        return new double[] {x + left[0] * 5.5 * k + forward[0] * 0.7, y + 21.0 * k, z + left[2] * 5.5 * k + forward[2] * 0.7};
    }

    /** An entity's middle, interpolated for the frame. */
    static double[] centre(Entity entity, float partial) {
        return new double[] {entity.f_19790_ + (entity.m_20185_() - entity.f_19790_) * partial,
            entity.f_19791_ + (entity.m_20186_() - entity.f_19791_) * partial + entity.m_20206_() * 0.5,
            entity.f_19792_ + (entity.m_20189_() - entity.f_19792_) * partial};
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normals, float[] q, int corner, int colour, int light) {
        consumer.m_252986_(matrix, q[corner * 3], q[corner * 3 + 1], q[corner * 3 + 2])
            .m_6122_((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, colour >>> 24)
            .m_7421_(0.5F, 0.5F)
            .m_86008_(OverlayTexture.f_118083_)
            .m_85969_(light)
            .m_252939_(normals, q[12], q[13], q[14])
            .m_5752_();
    }

    /** An ARGB colour for the additive type: its alpha folded into its colour. */
    private static int premultiplied(int argb) {
        double a = (argb >>> 24) / 255.0;
        int r = (int)Math.round(((argb >> 16) & 0xFF) * a);
        int g = (int)Math.round(((argb >> 8) & 0xFF) * a);
        int b = (int)Math.round((argb & 0xFF) * a);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
        KingHalves.clear();
        com.hex.kagune.control.client.fx.PeaceFx.clear();
    }
}
