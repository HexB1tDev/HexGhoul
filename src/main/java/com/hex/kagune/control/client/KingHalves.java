package com.hex.kagune.control.client;

import com.hex.kagune.control.client.fx.RcFx;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.0.13: what Black Reaper's cleave kills is cut in half ({@code KingFx}): while it dies, a body with a humanoid model is drawn twice,
 * its legs where they fall and the rest of it (head, body, arms) sliding off along the cut and tipping over, blood spraying from the
 * wound. Anything else (and players, whose renderer sets its own parts) bleeds out where it falls instead.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class KingHalves {
    /** Each body cut in half: the cut's way across (x, z), and when (world ticks). */
    private record Cut(double dx, double dz, double at) {
    }

    private static final Map<Integer, Cut> CUT = new HashMap<>();
    private static final double LASTS = 40.0;
    private static boolean drawing;

    private KingHalves() {
    }

    /** The bodies {@code ids} were cut in half, along {@code dx, dz}, at {@code now}. */
    static void cut(int[] ids, double dx, double dz, double now) {
        for (int id : ids) {
            CUT.put(id, new Cut(dx, dz, now));
        }
    }

    static void clear() {
        CUT.clear();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void render(RenderLivingEvent.Pre event) {
        if (drawing || CUT.isEmpty()) {
            return;
        }
        LivingEntity body = event.getEntity();
        Cut cut = CUT.get(body.m_19879_());
        if (cut == null) {
            return;
        }
        double age = ControlClient.worldTime(event.getPartialTick()) - cut.at();
        if (age > LASTS) {
            CUT.remove(body.m_19879_());
            return;
        }
        // Blood out of the cut, every frame a little.
        if (Math.random() < 0.5) {
            Vec3 at = body.m_20182_().m_82520_(0.0, body.m_20206_() * 0.55, 0.0);
            RcFx.at(RcFx.MOTE, true, 16, at).velocity(new Vec3((Math.random() - 0.5) * 0.2, 0.1 + Math.random() * 0.15, (Math.random() - 0.5) * 0.2))
                .colors(RcFx.CRIMSON, 0x00200004).size(0.12F, 0.04F).fall(0.03F).spawn();
        }
        LivingEntityRenderer renderer = event.getRenderer();
        if (body instanceof AbstractClientPlayer || !(renderer.m_7200_() instanceof HumanoidModel<?> model)) {
            return;
        }
        event.setCanceled(true);
        PoseStack pose = event.getPoseStack();
        ModelPart[] upper = {model.f_102808_, model.f_102809_, model.f_102810_, model.f_102811_, model.f_102812_};
        ModelPart[] lower = {model.f_102813_, model.f_102814_};
        boolean[] was = new boolean[7];
        for (int i = 0; i < 5; i++) {
            was[i] = upper[i].f_104207_;
        }
        was[5] = lower[0].f_104207_;
        was[6] = lower[1].f_104207_;
        drawing = true;
        try {
            // The legs, where they are.
            for (ModelPart part : upper) {
                part.f_104207_ = false;
            }
            renderer.m_7392_(body, 0.0F, event.getPartialTick(), pose, event.getMultiBufferSource(), event.getPackedLight());
            for (int i = 0; i < 5; i++) {
                upper[i].f_104207_ = was[i];
            }
            // The rest, sliding off along the cut and falling the other way.
            lower[0].f_104207_ = false;
            lower[1].f_104207_ = false;
            double slide = Math.min(1.0, age / 10.0);
            double drop = Math.min(1.0, Math.max(0.0, (age - 4.0) / 10.0));
            pose.m_85836_();
            pose.m_85837_(cut.dx() * 0.45 * slide, -0.5 * drop * drop, cut.dz() * 0.45 * slide);
            renderer.m_7392_(body, 0.0F, event.getPartialTick(), pose, event.getMultiBufferSource(), event.getPackedLight());
            pose.m_85849_();
        } finally {
            drawing = false;
            for (int i = 0; i < 5; i++) {
                upper[i].f_104207_ = was[i];
            }
            lower[0].f_104207_ = was[5];
            lower[1].f_104207_ = was[6];
        }
    }
}
