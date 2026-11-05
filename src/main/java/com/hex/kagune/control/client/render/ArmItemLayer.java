package com.hex.kagune.control.client.render;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.client.WeaponClient;
import com.hex.kagune.control.npc.ArmRules;
import com.hex.kagune.control.quinque.Arm;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 3.0.6: a player's kagune-made weapon ({@link Arm}, the item out of its case) in their right hand, drawn as an investigator's is
 * ({@link WeaponLayer}): its own mesh on the arm, growing out of the fist as it tears out of its case, the spear's javelin gone as it is
 * thrown and grown back; the whip's tendril out of its knot along its path (hanging, each lash, the Flay, the Bind round whoever it caught).
 * The item's own model draws nothing in the hand.
 */
public final class ArmItemLayer extends RenderLayer<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> {
    private static final RenderType SOLID = RenderType.m_110473_(Objects.requireNonNull(ResourceLocation.m_214293_("hexkagune", "textures/misc/white.png")));

    public ArmItemLayer(RenderLayerParent<AbstractClientPlayer, EntityModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void m_6494_(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                        float partial, float ageInTicks, float netHeadYaw, float headPitch) {
        try {
            PlayerLayers.ran(player);
            ItemStack held = player.m_21205_();
            Arm arm = ControlItems.armWeapon(held);
            if (arm == null || player.m_20145_() || player.m_9236_() == null || !(this.m_117386_() instanceof HumanoidModel<?> model)
                || !model.f_102811_.f_104207_) {
                return;
            }
            float shown = WeaponClient.shown(held, partial);
            if (shown <= 0.0F) {
                return;
            }
            boolean master = ControlItems.masterwork(held);
            boolean slim = "slim".equals(player.m_108564_());
            WeaponClient.View view = WeaponClient.view(player.m_19879_());
            double age = WeaponClient.age(view, partial);
            int code = view == null ? -1 : view.code();
            Arm.Ability ability = ArmRules.ability(code);
            if (ability != null && (arm.slot(ability) < 0 || age < 0.0 || age >= ability.total)) {
                ability = null;
            }
            float grown = ability == null ? 1.0F : com.hex.kagune.control.client.animation.WeaponPose.grown(ability, age);
            float k = shown * grown;
            pose.m_85836_();
            PlayerLayers.upperBody(pose, player, partial);
            if (k > 0.002F) {
                pose.m_85836_();
                if (slim) {
                    pose.m_85837_(0.5 / 16.0, 0.0, 0.0); // PlayerModel.translateToHand: a slim arm's half pixel
                }
                model.f_102811_.m_104299_(pose);
                WeaponLayer.mesh(pose, buffers.m_6299_(SOLID), arm, master, k, light);
                pose.m_85849_();
            }
            if (arm == Arm.WHIP) {
                WeaponLayer.whip(pose, buffers, light, player, view == null ? 0 : view.bound(), 1.0F, 1.0F, model.f_102811_, slim, master, shown, code,
                    age, ability, partial);
            }
            pose.m_85849_();
        } catch (RuntimeException failure) {
            PlayerLayers.failed("a player's kagune weapon", failure);
        }
    }

    // ---------------------------------------------------------------- first person

    /**
     * Its wielder's own view: the weapon in the right hand where vanilla holds a sword, point up and away, its broad face turned to them,
     * swung as each blow goes (a sweep across the view, an overhead down through it, a thrust driven forward; the whip's handle alone),
     * grown out of the hand as it tears out of its case, the javelin gone as it is thrown.
     */
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "hexkagune", value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class FirstPerson {
        private FirstPerson() {
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void hand(net.minecraftforge.client.event.RenderHandEvent event) {
            if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
                return;
            }
            Arm arm = ControlItems.armWeapon(event.getItemStack());
            if (arm == null) {
                return;
            }
            event.setCanceled(true); // its model draws nothing; this is it
            try {
                net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.m_91087_().f_91074_;
                if (player == null) {
                    return;
                }
                float partial = event.getPartialTick();
                ItemStack held = event.getItemStack();
                float shown = WeaponClient.shown(held, partial);
                WeaponClient.View view = WeaponClient.view(player.m_19879_());
                double age = WeaponClient.age(view, partial);
                int code = view == null ? -1 : view.code();
                int step = ArmRules.step(code);
                Arm.Ability ability = ArmRules.ability(code);
                if (ability != null && (arm.slot(ability) < 0 || age < 0.0 || age >= ability.total)) {
                    ability = null;
                }
                float grown = ability == null ? 1.0F : com.hex.kagune.control.client.animation.WeaponPose.grown(ability, age);
                float k = shown * grown;
                if (k <= 0.002F) {
                    return;
                }
                double[] swing = swing(arm, step, ability, age);
                PoseStack pose = event.getPoseStack();
                pose.m_85836_();
                pose.m_85837_(0.56 + swing[3], -0.52 + swing[4], -0.72 + swing[5]);
                pose.m_252781_(new org.joml.Quaternionf().rotateAxis((float)Math.toRadians(swing[0]), 0.0F, 1.0F, 0.0F)
                    .rotateAxis((float)Math.toRadians(swing[1]), 1.0F, 0.0F, 0.0F).rotateAxis((float)Math.toRadians(swing[2]), 0.0F, 0.0F, 1.0F));
                pose.m_252781_(new org.joml.Quaternionf().rotateAxis((float)Math.toRadians(15.0), 0.0F, 0.0F, 1.0F)
                    .rotateAxis((float)Math.toRadians(-30.0), 1.0F, 0.0F, 0.0F).rotateAxis((float)Math.toRadians(-50.0), 0.0F, 1.0F, 0.0F)
                    .rotateAxis((float)Math.toRadians(90.0), 1.0F, 0.0F, 0.0F));
                float size = arm == Arm.CRYSTAL_SPEAR ? 0.55F : arm == Arm.WHIP ? 0.9F : 0.68F;
                pose.m_85841_(size, size, size);
                raw(pose, event.getMultiBufferSource().m_6299_(SOLID), arm, ControlItems.masterwork(held), k, event.getPackedLight());
                pose.m_85849_();
            } catch (RuntimeException failure) {
                PlayerLayers.failed("a kagune weapon (first person)", failure);
            }
        }

        /**
         * Where a blow has it ({@code age} ticks in): {yaw, pitch, roll} turned about the hand (degrees), {x, y, z} moved (blocks): wound up
         * to its landing's first half, through it by its landing, back by its end.
         */
        static double[] swing(Arm arm, int step, Arm.Ability ability, double age) {
            double[] out = new double[6];
            double total;
            double land;
            int kind; // 0 a sweep, 1 an overhead, 2 a thrust
            boolean back = false;
            int repeats = 1;
            if (step >= 0) {
                Arm.Strike strike = arm.strike(step);
                total = strike.total();
                land = strike.land();
                kind = strike.blow() == Arm.Strike.STAB ? 2 : strike.arc() >= 100.0 ? 0 : 1;
                back = step % 2 == 1;
            } else if (ability != null) {
                total = ability.total;
                land = ability.hits[ability.hits.length - 1];
                kind = switch (ability) {
                    case JAVELIN, PIERCE -> 2;
                    case QUAKE, LEAP -> 1;
                    default -> 0;
                };
                if (ability == Arm.Ability.FLAY || ability == Arm.Ability.WHIRL) {
                    repeats = ability.hits.length;
                }
            } else {
                return out;
            }
            if (age < 0.0 || age >= total) {
                return out;
            }
            double t = age / total;
            double l = Math.max(0.05, Math.min(0.95, land / total));
            double a;
            if (repeats > 1) {
                // A storm of blows: back and forth through the view, once a blow.
                double phase = Math.min(1.0, t / l) * repeats;
                a = Math.sin(phase * Math.PI) * (t < l ? 1.0 : 1.0 - ease((t - l) / (1.0 - l)));
                a = a * 2.0 - 1.0;
                a *= t < 0.08 ? ease(t / 0.08) : 1.0;
            } else if (t < 0.6 * l) {
                a = -ease(t / (0.6 * l));
            } else if (t < l) {
                a = -1.0 + 2.0 * ease((t - 0.6 * l) / (0.4 * l));
            } else {
                a = 1.0 - ease((t - l) / (1.0 - l));
            }
            switch (kind) {
                case 0 -> {
                    out[0] = (back ? -1.0 : 1.0) * (a < 0.0 ? 55.0 * a : 75.0 * a); // from the right across to the left (a backhand the other way)
                    out[2] = -12.0 * a;
                    out[3] = -0.12 * a * (back ? -1.0 : 1.0);
                }
                case 1 -> {
                    out[1] = a < 0.0 ? -45.0 * a : -85.0 * a; // raised back, then down through the view
                    out[4] = a < 0.0 ? -0.12 * a : -0.1 * a;
                }
                default -> {
                    out[5] = a < 0.0 ? -0.18 * a : -0.65 * a; // drawn back, then driven forward
                    out[1] = -10.0 * Math.max(0.0, a);
                }
            }
            return out;
        }

        private static double ease(double x) {
            double c = Math.max(0.0, Math.min(1.0, x));
            return c * c * (3.0 - 2.0 * c);
        }
    }

    /** {@code arm}'s mesh in its own hand frame (blocks, the grip at the origin), {@code k} of its size. */
    static void raw(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer out, Arm arm, boolean master, float k, int light) {
        com.hex.kagune.control.quinque.ArmMesh.Mesh mesh = com.hex.kagune.control.quinque.ArmMesh.of(arm, master);
        org.joml.Matrix4f m = pose.m_85850_().m_252922_();
        org.joml.Matrix3f n = pose.m_85850_().m_252943_();
        for (int q = 0; q < mesh.quads; q++) {
            int lit = mesh.glow[q] ? 0xF000F0 : light;
            for (int c = 0; c < 4; c++) {
                int v = q * 4 + c;
                int i = v * 3;
                int argb = mesh.argb[v];
                out.m_252986_(m, mesh.pos[i] * k, mesh.pos[i + 1] * k, mesh.pos[i + 2] * k)
                    .m_6122_(argb >> 16 & 255, argb >> 8 & 255, argb & 255, 255).m_7421_(0.5F, 0.5F)
                    .m_86008_(net.minecraft.client.renderer.texture.OverlayTexture.f_118083_).m_85969_(lit)
                    .m_252939_(n, mesh.normal[i], mesh.normal[i + 1], mesh.normal[i + 2]).m_5752_();
            }
        }
    }
}
