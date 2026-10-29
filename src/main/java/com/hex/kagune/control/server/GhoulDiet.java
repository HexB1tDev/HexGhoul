package com.hex.kagune.control.server;

import com.hex.kagune.KaguneMod;
import com.hex.kagune.control.ControlConfig;
import com.hex.kagune.control.FoodClassifier;
import com.hex.kagune.control.GhoulReserve;
import com.hex.kagune.data.KaguneProfile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Ghouls (HexKagune unlocked) are nourished only by raw flesh. Detection order: the item tags
 * {@code hexkagune:not_ghoul_food}, {@code hexkagune:ghoul_food}, {@code hexkagune:ghoul_tolerated}
 * (which pull in common {@code forge:raw_*} tags so other mods' raw meat works), then a name/isMeat
 * heuristic. Raw flesh fills the flesh reserve that replaces a ghoul's food bar ({@link GhoulReserve}); a ghoul living on the
 * reserve cannot start eating ordinary food at all (6.0.6). Should one still be eaten, the body rejects it and that costs reserve.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class GhoulDiet {
    public static final TagKey<Item> GHOUL_FOOD = ItemTags.create(KaguneMod.id("ghoul_food"));
    public static final TagKey<Item> GHOUL_TOLERATED = ItemTags.create(KaguneMod.id("ghoul_tolerated"));
    public static final TagKey<Item> NOT_GHOUL_FOOD = ItemTags.create(KaguneMod.id("not_ghoul_food"));

    private GhoulDiet() {
    }

    private static boolean isGhoul(LivingEntity entity) {
        return entity instanceof ServerPlayer player && ControlConfig.ghoulDiet() && KaguneProfile.isUnlocked(player);
    }

    public static FoodClassifier.Diet classify(ItemStack stack, LivingEntity eater) {
        if (stack.m_204117_(NOT_GHOUL_FOOD)) {
            return FoodClassifier.Diet.HUMAN_FOOD;
        }
        if (stack.m_204117_(GHOUL_FOOD)) {
            return FoodClassifier.Diet.GHOUL_FOOD;
        }
        if (stack.m_204117_(GHOUL_TOLERATED)) {
            return FoodClassifier.Diet.TOLERATED;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
        FoodProperties food = stack.getFoodProperties(eater);
        return FoodClassifier.classify(key == null ? "" : key.m_135815_(), food != null && food.m_38746_());
    }

    /** Before {@link #startEating}: a ghoul on the reserve does not get to eat human food (the client refuses it too). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void refuseHumanFood(LivingEntityUseItemEvent.Start event) {
        ItemStack stack = event.getItem();
        if (!(event.getEntity() instanceof ServerPlayer player) || !GhoulReserveServer.applies(player) || stack.m_41619_()
            || !stack.m_41614_() || classify(stack, player) != FoodClassifier.Diet.HUMAN_FOOD) {
            return;
        }
        event.setCanceled(true);
        ServerControlState state = ControlServer.state(player);
        message(player, state, player.m_9236_().m_46467_(), "message.hexkagune.food_refused");
    }

    @SubscribeEvent
    public static void startEating(LivingEntityUseItemEvent.Start event) {
        remember(event.getEntity(), event.getItem(), true);
    }

    @SubscribeEvent
    public static void eating(LivingEntityUseItemEvent.Tick event) {
        remember(event.getEntity(), event.getItem(), false);
    }

    /** The Tick event fires on the same tick just before the food is applied, so this is the pre-meal state. */
    private static void remember(LivingEntity entity, ItemStack stack, boolean start) {
        if (!isGhoul(entity) || stack.m_41619_() || !stack.m_41614_()) {
            return;
        }
        ServerPlayer player = (ServerPlayer)entity;
        ServerControlState state = ControlServer.state(player);
        FoodData food = player.m_36324_();
        state.preEatFood = food.m_38702_();
        state.preEatSaturation = food.m_38722_();
        if (start || state.preEatItem != stack.m_41720_()) {
            state.preEatHadHunger = player.m_21023_(MobEffects.f_19612_);
        }
        state.preEatItem = stack.m_41720_();
        state.preEatTick = player.m_9236_().m_46467_();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void finishEating(LivingEntityUseItemEvent.Finish event) {
        if (!isGhoul(event.getEntity())) {
            return;
        }
        ServerPlayer player = (ServerPlayer)event.getEntity();
        ItemStack eaten = event.getItem();
        if (eaten.m_41619_() || !eaten.m_41614_()) {
            return;
        }
        ServerControlState state = ControlServer.state(player);
        long now = player.m_9236_().m_46467_();
        if (state.preEatTick < 0L || now - state.preEatTick > 5L || state.preEatItem != eaten.m_41720_()) {
            return;
        }
        state.preEatTick = -1L;
        FoodData food = player.m_36324_();
        switch (classify(eaten, player)) {
            case GHOUL_FOOD -> {
                FoodProperties properties = eaten.getFoodProperties(player);
                int nutrition = properties == null ? 2 : Math.max(1, properties.m_38744_());
                // Raw flesh fills the reserve (vanilla hunger is held still under it).
                GhoulReserveServer.set(player, GhoulReserve.eat(GhoulReserveServer.get(player), nutrition));
                if (!state.preEatHadHunger) {
                    player.m_21195_(MobEffects.f_19612_);
                }
                float frenzyBefore = state.frenzy;
                ControlServer.calm(player, 0.35F);
                if (KaguneProfile.isActive(player) && frenzyBefore > 0.4F && now >= state.nextFeedAwardAt) {
                    state.nextFeedAwardAt = now + 600L;
                    ControlServer.awardRestraint(player, state, 1);
                }
                if (state.hunger > 0.3F) {
                    message(player, state, now, "message.hexkagune.flesh_satiates");
                }
            }
            case TOLERATED -> {
                food.m_38705_(state.preEatFood);
                food.m_38717_(state.preEatSaturation);
                message(player, state, now, "message.hexkagune.food_tolerated");
            }
            case HUMAN_FOOD -> {
                food.m_38705_(state.preEatFood);
                food.m_38717_(Math.min(state.preEatSaturation, state.preEatFood));
                // The body throws it back up, and that costs flesh.
                GhoulReserveServer.set(player, GhoulReserve.reject(GhoulReserveServer.get(player)));
                ControlServer.addFrenzy(player, 0.05F);
                message(player, state, now, "message.hexkagune.food_rejected");
            }
        }
    }

    private static void message(ServerPlayer player, ServerControlState state, long now, String key) {
        if (now >= state.nextDietMessageAt) {
            state.nextDietMessageAt = now + 100L;
            ControlServer.tell(player, key);
        }
    }
}
