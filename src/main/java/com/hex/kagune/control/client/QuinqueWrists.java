package com.hex.kagune.control.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 4.1.21: whose wrists the quinque being drawn is in. The arms are posed by the body's animation, the item drawn after by ItemInHandLayer
 * through {@link QuinqueItemRenderer}, which is told neither who holds it nor how: each body's weapon turn (its wrists, {@code
 * RigPose.weaponX}, Y, Z) is kept here as it is posed (players by KaguneBodyAnimation, investigators by AgentClient), and the body being
 * drawn is known from RenderLivingEvent. First person tells its own ({@link #firstPerson}). A body is only taken at its word for the
 * very stack in its main hand, drawn in its right hand: anything else (an inventory slot, a frame, a left hand) is drawn straight.
 */
@Mod.EventBusSubscriber(modid = "hexkagune", value = Dist.CLIENT)
public final class QuinqueWrists {
    private QuinqueWrists() {
    }

    private static final java.util.Map<LivingEntity, float[]> POSED = new java.util.WeakHashMap<>();
    private static LivingEntity drawing;
    private static float[] firstPerson;

    /** {@code body} posed with {@code wrists} ({x, y, z}; null: none) this frame. */
    public static void posed(LivingEntity body, float[] wrists) {
        if (body == null) {
            return;
        }
        if (wrists == null) {
            POSED.remove(body);
        } else {
            POSED.put(body, wrists);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void before(net.minecraftforge.client.event.RenderLivingEvent.Pre<?, ?> event) {
        drawing = event.getEntity();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void after(net.minecraftforge.client.event.RenderLivingEvent.Post<?, ?> event) {
        drawing = null;
    }

    /** First person draws its quinque with {@code wrists} until {@link #firstPersonDone}. */
    public static void firstPerson(float[] wrists) {
        firstPerson = wrists;
    }

    public static void firstPersonDone() {
        firstPerson = null;
    }

    /** The wrists {@code stack} is drawn in now in {@code context} (right hand only), or null. */
    static float[] of(ItemStack stack, net.minecraft.world.item.ItemDisplayContext context) {
        if (context != net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            return null;
        }
        if (firstPerson != null) {
            return firstPerson;
        }
        LivingEntity body = drawing;
        return body != null && body.m_21205_() == stack ? POSED.get(body) : null;
    }
}
