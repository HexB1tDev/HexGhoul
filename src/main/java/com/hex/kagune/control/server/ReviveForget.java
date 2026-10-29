package com.hex.kagune.control.server;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 6.2.0: when Eto's death does not take ({@link EtoRevive}), everything acts as if it had: whatever was after her, a zombie, a
 * neutral mob she angered, a piglin, the Warden, the Wither, forgets her. Its target is dropped, its grudge (anger, who hurt it,
 * what its brain was set on) is wiped, the Warden's anger is cleared, the Wither's heads look away; while she is in pieces nothing
 * can take her as a target again (she is invulnerable, which no mob's targeting accepts, and any target change onto her is refused),
 * and when she stands up whole they have forgotten her.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class ReviveForget {
    /** How far (blocks) the forgetting reaches: further than anything could follow her from. */
    static final double RANGE = 128.0;
    /** While she is in pieces, how far it is looked again every few ticks. */
    static final double NEAR = 64.0;
    /** The brain memories that hold who a mob is after (cleared when they hold her). */
    private static final String[] MEMORIES = {"attack_target", "angry_at", "hurt_by_entity", "roar_target"};
    /** Those memory types, looked up once (null until then). */
    private static MemoryModuleType<?>[] types;

    private ReviveForget() {
    }

    /** Everything within {@code range} of her forgets her. */
    static void forget(ServerPlayer player, double range) {
        ServerLevel level = player.m_284548_();
        UUID id = player.m_20148_();
        AABB area = player.m_20191_().m_82400_(range);
        for (Mob mob : level.m_6443_(Mob.class, area, mob -> mob.m_6084_())) {
            if (mob.m_5448_() == player) {
                mob.m_6710_(null);
            }
            if (mob.m_21188_() == player) {
                mob.m_6703_(null);
            }
            if (mob instanceof NeutralMob angry && id.equals(angry.m_6120_())) {
                angry.m_21662_();
            }
            if (mob instanceof Warden warden) {
                warden.m_219428_(player);
            }
            if (mob instanceof WitherBoss wither) {
                for (int head = 0; head < 2; head++) {
                    if (wither.m_31512_(head) == player.m_19879_()) {
                        wither.m_31454_(head, 0);
                    }
                }
            }
            forgetIn(mob.m_6274_(), player, id);
        }
    }

    /** Every memory of {@code brain}'s that holds her (as herself or by her UUID), erased. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void forgetIn(Brain<?> brain, ServerPlayer player, UUID id) {
        if (brain == null) {
            return;
        }
        if (types == null) {
            types = new MemoryModuleType<?>[MEMORIES.length];
            for (int i = 0; i < MEMORIES.length; i++) {
                types[i] = ForgeRegistries.MEMORY_MODULE_TYPES.getValue(ResourceLocation.m_214293_("minecraft", MEMORIES[i]));
            }
        }
        for (MemoryModuleType type : types) {
            if (type == null) {
                continue;
            }
            Optional<?> held = ((Brain)brain).m_257414_(type);
            if (held != null && held.isPresent() && (held.get() == player || id.equals(held.get()))) {
                ((Brain)brain).m_21936_(type);
            }
        }
    }

    /** Nothing takes her as its target while she is in pieces. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void target(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        if (target instanceof ServerPlayer player && EtoRevive.down(player)) {
            event.setCanceled(true);
        }
    }
}
