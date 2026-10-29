package com.hex.kagune.control.awakening;

import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.quest.Gate;
import com.hex.kagune.control.server.PathServer;
import com.hex.kagune.control.server.QuestServer;
import com.hex.kagune.data.KaguneProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-side wiring: quest progress, the death that wakes the Dragon, and the loss of control. */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class AwakeningEvents {
    private AwakeningEvents() {
    }

    /** Kakuja V2 belongs to Rinkaku. */
    static boolean rinkakuPath(ServerPlayer player) {
        return KaguneProfile.isUnlocked(player) && PathServer.path(player) == KagunePath.RINKAKU;
    }

    /** The quest "Kakuja V2: The Dragon" is done: its gate is open (QuestServer.open lets other paths' gates through, so ask the path first). */
    static boolean gateOpen(ServerPlayer player) {
        return rinkakuPath(player) && QuestServer.open(player, Gate.RINKAKU_DRAGON);
    }

    private static boolean real(Object entity) {
        return entity instanceof ServerPlayer && !(entity instanceof FakePlayer);
    }

    // ---- The awakening itself -------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void deathAwakensDragon(LivingDeathEvent event) {
        if (!real(event.getEntity())) return;
        ServerPlayer player = (ServerPlayer) event.getEntity();
        if (DragonSessions.isProtected(player)) {
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth());
            return;
        }
        boolean inVoid = player.getY() < player.serverLevel().getMinBuildHeight();
        if (!AwakeningQuest.awakensOnDeath(AwakeningProfile.stage(player), rinkakuPath(player), gateOpen(player), inVoid)) return;
        // Instead of dying, Rinkaku's body erupts. Cancelling at the highest priority keeps graves,
        // drops and death messages from other listeners out of it.
        if (DragonSessions.start(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void dragonIsUntouchable(LivingAttackEvent event) {
        if (real(event.getEntity()) && DragonSessions.isProtected((ServerPlayer) event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void dragonStaysInItsWorld(EntityTravelToDimensionEvent event) {
        if (real(event.getEntity()) && DragonSessions.isAwakening(((ServerPlayer) event.getEntity()).getUUID())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void noAttacks(AttackEntityEvent event) {
        if (locked(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void noBlockUse(PlayerInteractEvent.RightClickBlock event) {
        if (locked(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void noItemUse(PlayerInteractEvent.RightClickItem event) {
        if (locked(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void noBlockHits(PlayerInteractEvent.LeftClickBlock event) {
        if (locked(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void noEntityUse(PlayerInteractEvent.EntityInteract event) {
        if (locked(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void noEntityUseAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (locked(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void noMining(BlockEvent.BreakEvent event) {
        if (locked(event.getPlayer())) event.setCanceled(true);
    }

    private static boolean locked(Object entity) {
        return real(entity) && DragonSessions.isAwakening(((ServerPlayer) entity).getUUID());
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) DragonSessions.tick();
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        DragonSessions.endAll();
        TerrainScars.restoreAll();
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            DragonSessions.endIn(level);
            TerrainScars.restoreAll(level);
        }
    }

    // ---- Quest ----------------------------------------------------------------------------------

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !real(event.player)) return;
        ServerPlayer player = (ServerPlayer) event.player;
        if (player.tickCount % 20 == 0) progress(player);
    }

    /** Arms the Dragon once its quest is done, and says so once. */
    static void progress(ServerPlayer player) {
        AwakeningStage before = AwakeningProfile.stage(player);
        if (before == AwakeningStage.DRAGON || before == AwakeningStage.FINAL) return;
        AwakeningStage after = AwakeningQuest.advance(before, rinkakuPath(player), gateOpen(player));
        if (after == before) return;
        AwakeningProfile.setStage(player, after);
        if (after == AwakeningStage.ARMED) {
            announce(player, "\u00a7c\u00a7lTHE DRAGON SLEEPS", "\u00a77Your next death will not be yours");
            player.displayClientMessage(Component.literal("\u00a7c" + AwakeningQuest.TITLE + " \u00a77is done. The next time you die, the Dragon wakes instead."), false);
        }
        AwakeningNetwork.sendStatus(player);
    }

    private static void announce(ServerPlayer player, String title, String subtitle) {
        if (player.connection == null) return;
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
    }

    // ---- Lifecycle ------------------------------------------------------------------------------

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!real(event.getEntity())) return;
        ServerPlayer player = (ServerPlayer) event.getEntity();
        DragonSessions.recover(player);
        AwakeningNetwork.sendStatus(player);
        // 1.0.5: a Dragon spent before the final kakujas existed still leaves its crown.
        if (FinalKakuja.isUnlocked(player) && rinkakuPath(player)) com.hex.kagune.control.server.KingServer.awaken(player);
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!real(event.getEntity())) return;
        ServerPlayer player = (ServerPlayer) event.getEntity();
        DragonSessions.end(player.getUUID());
        DragonSessions.forget(player.getUUID());
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (real(event.getOriginal()) && real(event.getEntity())) {
            AwakeningProfile.copy((ServerPlayer) event.getOriginal(), (ServerPlayer) event.getEntity());
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (real(event.getEntity())) AwakeningNetwork.sendStatus((ServerPlayer) event.getEntity());
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (real(event.getEntity())) AwakeningNetwork.sendStatus((ServerPlayer) event.getEntity());
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        AwakeningCommands.register(event.getDispatcher());
    }
}
