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
            // 3.4.8: the body as it is posed this frame: the weapon turned in the fist as its blow has it (both hands on its grip).
            com.hex.kagune.control.client.animation.RigPose posed = com.hex.kagune.control.client.animation.KaguneBodyAnimation.current(player, partial);
            pose.m_85836_();
            PlayerLayers.upperBody(pose, player, partial);
            if (k > 0.002F) {
                pose.m_85836_();
                if (slim) {
                    pose.m_85837_(0.5 / 16.0, 0.0, 0.0); // PlayerModel.translateToHand: a slim arm's half pixel
                }
                model.f_102811_.m_104299_(pose);
                WeaponLayer.mesh(pose, buffers.m_6299_(SOLID), arm, master, k, light, posed);
                pose.m_85849_();
            }
            if (arm == Arm.WHIP) {
                WeaponLayer.whip(pose, buffers, light, player, view == null ? 0 : view.bound(), 1.0F, 1.0F, model.f_102811_, slim, master, shown, code,
                    age, ability, partial, posed);
            }
            pose.m_85849_();
        } catch (RuntimeException failure) {
            PlayerLayers.failed("a player's kagune weapon", failure);
        }
    }

    // ---------------------------------------------------------------- first person

    /**
     * 4.1.19: {@code pose} (the camera's) turned onto the right hand of a body posed {@code p}, as the player model and ItemInHandLayer
     * place a held item in third person, seen as {@link com.hex.kagune.control.client.animation.FirstPersonQuinque} sees it: the view
     * zoomed and looking down, the body's step carrying the weapon, from its eye behind the head, the body's whole turn left out. Each
     * step here is one of FirstPersonQuinque.view's, in its order (QuinqueTests.firstPerson holds them to it).
     */
    static void throughBody(PoseStack pose, com.hex.kagune.control.client.animation.RigPose p, boolean slim) {
        throughBody(pose, p, slim, true);
    }

    /**
     * 4.1.22: as above; {@code hand} false stops on the right arm itself (its pivot, Rz Ry Rx: what a kagune-made weapon's mesh is drawn on
     * in third person, WeaponLayer.mesh, a slim arm's half pixel in), not on into the hand as ItemInHandLayer places a held item.
     */
    static void throughBody(PoseStack pose, com.hex.kagune.control.client.animation.RigPose p, boolean slim, boolean hand) {
        com.hex.kagune.control.client.animation.RigPose still = com.hex.kagune.control.client.animation.FirstPersonQuinque.still(p);
        float[] eye = com.hex.kagune.control.client.animation.FirstPersonQuinque.EYE;
        float model = com.hex.kagune.control.client.animation.FirstPersonQuinque.MODEL;
        float zoom = com.hex.kagune.control.client.animation.FirstPersonQuinque.ZOOM;
        // The view zoomed about its middle (not its depth), looking down.
        pose.m_85841_(zoom, zoom, 1.0F);
        pose.m_252781_(new org.joml.Quaternionf().rotateAxis(com.hex.kagune.control.client.animation.FirstPersonQuinque.PITCH, 1, 0, 0));
        // The body's step (PlayerRendererMixin's body position, blocks): its drop down, its lunge ahead.
        pose.m_85837_(0.0, -p.drop / 16.0, -p.lunge / 16.0);
        // The model's frame (x left, y down, sixteenths of a block, the player model's 0.9375) from the eye.
        pose.m_85841_(-model, -model, model);
        pose.m_85837_(-eye[0] / 16.0, -eye[1] / 16.0, -eye[2] / 16.0);
        // The right arm as ModelPart.translateAndRotate places it (its pivot, then Rz Ry Rx), the posed angles PlayerAnimator gives it.
        float[] at = still.transform("rightArm", com.hex.kagune.control.client.animation.RigPose.POSITION, -5.0F, 2.0F, 0.0F);
        float[] turn = still.transform("rightArm", com.hex.kagune.control.client.animation.RigPose.ROTATION, 0.0F, 0.0F, 0.0F);
        pose.m_85837_(at[0] / 16.0, at[1] / 16.0, at[2] / 16.0);
        pose.m_252781_(new org.joml.Quaternionf().rotateAxis(turn[2], 0, 0, 1).rotateAxis(turn[1], 0, 1, 0).rotateAxis(turn[0], 1, 0, 0));
        // ItemInHandLayer: a slim arm's half pixel, Rx(-90), Ry(180), its offset into the fist.
        if (slim) {
            pose.m_85837_(0.5 / 16.0, 0.0, 0.0);
        }
        if (!hand) {
            return;
        }
        pose.m_252781_(new org.joml.Quaternionf().rotateAxis((float)Math.toRadians(-90.0), 1, 0, 0).rotateAxis((float)Math.PI, 0, 1, 0));
        pose.m_85837_(1.0 / 16.0, 0.125, -0.625);
    }

    /** 3.1.3: every quinque's attacks in the camera view, independently of PlayerAnimator's third-person layer. */
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "hexkagune", value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class FirstPerson {
        private FirstPerson() {
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGH)
        public static void hand(net.minecraftforge.client.event.RenderHandEvent event) {
            if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
                return;
            }
            ItemStack held = event.getItemStack();
            Arm arm = ControlItems.armWeapon(held);
            com.hex.kagune.control.quinque.Quinque legacy = ControlItems.quinqueWeapon(held);
            if (arm == null && legacy == null) {
                return;
            }
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.m_91087_();
            net.minecraft.client.player.LocalPlayer player = minecraft.f_91074_;
            if (player == null) {
                return;
            }
            event.setCanceled(true);
            try {
                float partial = event.getPartialTick();
                float equip = event.getEquipProgress();
                WeaponClient.View view = arm == null ? null : WeaponClient.view(player.m_19879_());
                double age = WeaponClient.age(view, partial);
                int code = view == null ? -1 : view.code();
                int step = ArmRules.step(code);
                Arm.Ability ability = ArmRules.ability(code);
                if (ability != null && (arm == null || arm.slot(ability) < 0 || age < 0 || age >= ability.total)) {
                    ability = null;
                }
                double[] motion;
                float k = 1.0F;
                if (arm != null) {
                    float grown = ability == null ? 1.0F : com.hex.kagune.control.client.animation.WeaponPose.grown(ability, age);
                    k = WeaponClient.shown(held, partial) * grown;
                    motion = com.hex.kagune.control.client.animation.QuinqueOwn.arm(arm, step, ability, age);
                } else {
                    com.hex.kagune.control.client.QuinqueClient.Attack attack = com.hex.kagune.control.client.QuinqueClient.attack(player, partial);
                    motion = com.hex.kagune.control.client.animation.QuinqueOwn.legacy(legacy, attack.step(), attack.age());
                    if (legacy.firesOnUse()) {
                        float recoil = com.hex.kagune.control.client.QuinqueClient.recoil(player, partial);
                        motion[1] -= 5.0 * recoil;
                        motion[5] += 0.08 * recoil;
                    }
                }
                if (k <= 0.002F) {
                    return; // the thrown javelin has not grown back yet
                }
                PoseStack pose = event.getPoseStack();
                pose.m_85836_();
                try {
                    // 4.1.19 (the owner: "don't forget the the first person animation, like first person view to match the animation that
                    // they got, because they currently don't have that"): a melee quinque with a stance (the spear, the katana, the mace,
                    // the blade, Dawn) is drawn in first person exactly as the body swings it in third person (the same arm, the same hand,
                    // the same model and transform, the body's step), seen from behind the head (FirstPersonQuinque), so each cut crosses
                    // the screen the way it goes. The launcher keeps its own (it aims along the crosshair).
                    // 4.1.22 (the owner: "just make the first person of the other weapons. Don't touch their looks"): a kagune-made
                    // weapon (the bone greatsword, the flesh hammer, the crystal spear) the same way, its mesh on the posed arm as its
                    // third person layer draws it (WeaponLayer.mesh, its turn in the fist with it). The whip keeps its own: its tendril
                    // is laid along a path of its own.
                    com.hex.kagune.control.client.animation.RigPose posed = arm != null && arm != Arm.WHIP
                        ? com.hex.kagune.control.client.animation.KaguneBodyAnimation.current(player, partial) : null;
                    if (posed != null) {
                        pose.m_85837_(0.0, -0.6 * equip, 0.0);
                        throughBody(pose, posed, "slim".equals(player.m_108564_()), false);
                        WeaponLayer.mesh(pose, event.getMultiBufferSource().m_6299_(SOLID), arm, ControlItems.masterwork(held), k,
                            event.getPackedLight(), posed);
                        return;
                    }
                    com.hex.kagune.control.client.animation.RigPose body = legacy != null && arm == null && !legacy.ranged
                        && com.hex.kagune.control.client.animation.QuinqueHolds.of(legacy) != null
                        ? com.hex.kagune.control.client.QuinqueClient.pose(player, partial) : null;
                    if (body != null) {
                        pose.m_85837_(0.0, -0.6 * equip, 0.0);
                        throughBody(pose, body, "slim".equals(player.m_108564_()));
                        // 4.1.21: drawn in this pose's wrists (a quinque drawn by code turns itself in the fist; QuinqueWrists).
                        com.hex.kagune.control.client.QuinqueWrists.firstPerson(new float[] {body.weaponX, body.weaponY, body.weaponZ});
                        try {
                            minecraft.m_91291_().m_269128_(held, net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                                event.getPackedLight(), net.minecraft.client.renderer.texture.OverlayTexture.f_118083_, pose,
                                event.getMultiBufferSource(), player.m_9236_(), player.m_19879_());
                        } finally {
                            com.hex.kagune.control.client.QuinqueWrists.firstPersonDone();
                        }
                        return;
                    } else {
                        pose.m_85837_(0.42 + motion[3], -0.43 + motion[4] - 0.6 * equip, -0.95 + motion[5]);
                        pose.m_252781_(new org.joml.Quaternionf().rotateAxis((float)Math.toRadians(motion[0]), 0, 1, 0)
                            .rotateAxis((float)Math.toRadians(motion[1]), 1, 0, 0).rotateAxis((float)Math.toRadians(motion[2]), 0, 0, 1));
                    }
                    if (arm == null) {
                        minecraft.m_91291_().m_269128_(held, net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                            event.getPackedLight(), net.minecraft.client.renderer.texture.OverlayTexture.f_118083_, pose,
                            event.getMultiBufferSource(), player.m_9236_(), player.m_19879_());
                    } else {
                        pose.m_252781_(new org.joml.Quaternionf().rotateAxis((float)Math.toRadians(15), 0, 0, 1)
                            .rotateAxis((float)Math.toRadians(-30), 1, 0, 0).rotateAxis((float)Math.toRadians(-50), 0, 1, 0)
                            .rotateAxis((float)Math.toRadians(90), 1, 0, 0));
                        float size = com.hex.kagune.control.client.animation.QuinqueOwn.scale(arm);
                        pose.m_85841_(size, size, size);
                        raw(pose, event.getMultiBufferSource().m_6299_(SOLID), arm, ControlItems.masterwork(held), k, event.getPackedLight());
                    }
                } finally {
                    pose.m_85849_();
                }
                if (arm == Arm.WHIP) {
                    boolean master = ControlItems.masterwork(held);
                    double time = player.f_19797_ + partial;
                    double[][] path = com.hex.kagune.control.client.animation.QuinqueOwn.whip(motion, step, ability, age, master, k, equip, time);
                    RinkakuTube.immediate(event.getMultiBufferSource(), pose.m_85850_().m_252922_(), pose.m_85850_().m_252943_(), path,
                        com.hex.kagune.control.quinque.WhipPath.thickness(master) * k, master ? 0x6E0A14 : 0xA0101E,
                        event.getPackedLight(), player.m_19879_() * 31 + 7, time);
                }
            } catch (RuntimeException failure) {
                PlayerLayers.failed("a quinque (first person)", failure);
            }
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
