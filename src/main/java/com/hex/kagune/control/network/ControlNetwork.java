package com.hex.kagune.control.network;

import com.hex.kagune.KaguneMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Separate channel for the control/instinct/combo expansion. The original {@code hexkagune:main}
 * channel and its packets are untouched. Both sides must have this channel at the same version, so mismatched
 * releases (1.0.x without the channel, 1.1.x at version 1, 1.2.x at version 2) are refused cleanly at login
 * instead of failing mid-game. Each release that adds messages bumps the version.
 */
public final class ControlNetwork {
    /**
     * 2: adds the kagune path and the Ukaku kit (1.2.0); 3: adds the kakugan (1.2.0 builds from the kakugan on); 4: Ukaku's
     * fire shots and right-click enders (1.2.3); 5: the crystal mode, its stuck spikes and bleeding (1.3.0 builds); 6: the Wing
     * Vortex replaces the Shard Volley in the Ukaku state (1.3.0 builds); 7: the Gatling (1.3.0); 10: ghoul mode, R and Alt + R
     * (3.0.0); 12: Rinkaku's forms reordered (the original sends forms by ordinal) and his K timed by the client (3.0.1); 13: the
     * final kakuja's own kit (new combo styles, burning needles); 14: the flesh reserve that replaces a ghoul's food bar, Rinkaku's wound hits (3.0.3); 16: the Wing Block leaves the Ukaku state
     * (5.0.13), so a 5.0.12 client and a 5.0.13 server (or the reverse) are refused at login instead of misreading it; 17: the
     * tentacles guarding Rinkaku against shots (5.0.14); 18: the crystal thorns' effects (5.0.15); 19: the Steel Blade in the Ukaku state (5.0.18); 20: the kakuja's dash
     * and vanish, blood from the blade (6.0.0); 21: the Pale path (6.1.0: a fourth path ordinal, her actions, moves and effects); 26:
     * Graft DNA (7.0.0: a fifth path ordinal replacing the dormant Owl DNA, its state and actions); 27: the eye-mode fight's M1,
     * Ghoul Meat and the quests (7.0.1); 32: the final kakujas, War and Peace (1.0.5); 33: War's moves and waits (1.0.6), its rinkaku's own strikes (1.0.7); 34: what War's
     * abilities do out in the world (1.0.13); 35: War's M1, its blows and their effects (1.0.14); 36: the Black Sun out of the ground (1.0.15); 37: Peace playable, its blades' moves and cuts (1.0.16); 38: the transformations, nothing hurts him nor any ability while it comes (1.0.17). 39: Peace's six blades, their numbering and moves (1.0.18). 40: the marks blows leave instead of vanilla particles (1.0.19). 41: the fighting grammar: M1 flags, the heavy, the charge (1.0.20). 42: Peace's blades reach farther, its paths changed (1.0.21).
     * 54: Eto (1.3.0): her state and surgery, her talk choices, the one-eye grant in the kakugan state.
     */
    private static final String PROTOCOL = "54";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        KaguneMod.id("control"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static boolean initialized;

    private ControlNetwork() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        CHANNEL.registerMessage(0, S2CControlState.class, S2CControlState::encode, S2CControlState::decode, S2CControlState::handle);
        CHANNEL.registerMessage(1, S2CPathState.class, S2CPathState::encode, S2CPathState::decode, S2CPathState::handle);
        CHANNEL.registerMessage(2, C2SChoosePath.class, C2SChoosePath::encode, C2SChoosePath::decode, C2SChoosePath::handle);
        CHANNEL.registerMessage(3, S2CPathPrompt.class, S2CPathPrompt::encode, S2CPathPrompt::decode, S2CPathPrompt::handle);
        CHANNEL.registerMessage(4, S2CUkakuState.class, S2CUkakuState::encode, S2CUkakuState::decode, S2CUkakuState::handle);
        CHANNEL.registerMessage(5, S2CKakuganState.class, S2CKakuganState::encode, S2CKakuganState::decode, S2CKakuganState::handle);
        CHANNEL.registerMessage(6, S2CKakuganPrompt.class, S2CKakuganPrompt::encode, S2CKakuganPrompt::decode, S2CKakuganPrompt::handle);
        CHANNEL.registerMessage(7, C2SPlaceKakugan.class, C2SPlaceKakugan::encode, C2SPlaceKakugan::decode, C2SPlaceKakugan::handle);
        CHANNEL.registerMessage(8, S2CUkakuFx.class, S2CUkakuFx::encode, S2CUkakuFx::decode, S2CUkakuFx::handle);
        CHANNEL.registerMessage(9, C2SUkakuAction.class, C2SUkakuAction::encode, C2SUkakuAction::decode, C2SUkakuAction::handle);
        CHANNEL.registerMessage(10, S2CUkakuSpikes.class, S2CUkakuSpikes::encode, S2CUkakuSpikes::decode, S2CUkakuSpikes::handle);
        CHANNEL.registerMessage(11, C2SGhoulKey.class, C2SGhoulKey::encode, C2SGhoulKey::decode, C2SGhoulKey::handle);
        CHANNEL.registerMessage(12, S2CGhoulState.class, S2CGhoulState::encode, S2CGhoulState::decode, S2CGhoulState::handle);
        CHANNEL.registerMessage(13, S2CGhoulReserve.class, S2CGhoulReserve::encode, S2CGhoulReserve::decode, S2CGhoulReserve::handle);
        CHANNEL.registerMessage(14, S2CAbilityHud.class, S2CAbilityHud::encode, S2CAbilityHud::decode, S2CAbilityHud::handle);
        CHANNEL.registerMessage(15, S2CTendrilShield.class, S2CTendrilShield::encode, S2CTendrilShield::decode, S2CTendrilShield::handle);
        CHANNEL.registerMessage(16, C2SPaleAction.class, C2SPaleAction::encode, C2SPaleAction::decode, C2SPaleAction::handle);
        CHANNEL.registerMessage(17, S2CPaleState.class, S2CPaleState::encode, S2CPaleState::decode, S2CPaleState::handle);
        CHANNEL.registerMessage(18, S2CPaleFx.class, S2CPaleFx::encode, S2CPaleFx::decode, S2CPaleFx::handle);
        CHANNEL.registerMessage(19, S2CPaleWatch.class, S2CPaleWatch::encode, S2CPaleWatch::decode, S2CPaleWatch::handle);
        CHANNEL.registerMessage(20, S2CPaleArm.class, S2CPaleArm::encode, S2CPaleArm::decode, S2CPaleArm::handle);
        CHANNEL.registerMessage(21, S2CPaleOwl.class, S2CPaleOwl::encode, S2CPaleOwl::decode, S2CPaleOwl::handle);
        CHANNEL.registerMessage(22, S2CPaleRevive.class, S2CPaleRevive::encode, S2CPaleRevive::decode, S2CPaleRevive::handle);
        CHANNEL.registerMessage(23, S2CPaleMutations.class, S2CPaleMutations::encode, S2CPaleMutations::decode, S2CPaleMutations::handle);
        CHANNEL.registerMessage(24, S2COwlLance.class, S2COwlLance::encode, S2COwlLance::decode, S2COwlLance::handle);
        CHANNEL.registerMessage(25, S2COwlShard.class, S2COwlShard::encode, S2COwlShard::decode, S2COwlShard::handle);
        CHANNEL.registerMessage(26, S2CGraftState.class, S2CGraftState::encode, S2CGraftState::decode, S2CGraftState::handle);
        CHANNEL.registerMessage(27, C2SGraftAction.class, C2SGraftAction::encode, C2SGraftAction::decode, C2SGraftAction::handle);
        CHANNEL.registerMessage(28, S2CGhoulMeat.class, S2CGhoulMeat::encode, S2CGhoulMeat::decode, S2CGhoulMeat::handle);
        CHANNEL.registerMessage(29, S2CQuests.class, S2CQuests::encode, S2CQuests::decode, S2CQuests::handle);
        CHANNEL.registerMessage(30, S2CGhoulNpc.class, S2CGhoulNpc::encode, S2CGhoulNpc::decode, S2CGhoulNpc::handle);
        CHANNEL.registerMessage(31, S2CNpcTalk.class, S2CNpcTalk::encode, S2CNpcTalk::decode, S2CNpcTalk::handle);
        CHANNEL.registerMessage(32, C2SNpcTalk.class, C2SNpcTalk::encode, C2SNpcTalk::decode, C2SNpcTalk::handle);
        CHANNEL.registerMessage(33, S2CKingState.class, S2CKingState::encode, S2CKingState::decode, S2CKingState::handle);
        CHANNEL.registerMessage(34, C2SKingAction.class, C2SKingAction::encode, C2SKingAction::decode, C2SKingAction::handle);
        CHANNEL.registerMessage(35, S2CKingLash.class, S2CKingLash::encode, S2CKingLash::decode, S2CKingLash::handle);
        CHANNEL.registerMessage(36, S2CKingFx.class, S2CKingFx::encode, S2CKingFx::decode, S2CKingFx::handle); // 1.0.13
        CHANNEL.registerMessage(37, S2CEto.class, S2CEto::encode, S2CEto::decode, S2CEto::handle); // 1.3.0
        CHANNEL.registerMessage(38, S2CEtoSurgery.class, S2CEtoSurgery::encode, S2CEtoSurgery::decode, S2CEtoSurgery::handle);
    }

    /** To the subject and every client tracking them. */
    public static void broadcast(ServerPlayer subject, Object message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> subject), message);
    }

    /** To every client tracking {@code subject} (and {@code subject} itself if it is a player). */
    public static void broadcastAround(net.minecraft.world.entity.Entity subject, Object message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> subject), message);
    }

    /** 6.1.0: to every player in {@code level} within {@code range} blocks of {@code at} (something that belongs to no one). */
    public static void sendNear(net.minecraft.server.level.ServerLevel level, net.minecraft.world.phys.Vec3 at, double range, Object message) {
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_20238_(at) <= range * range) {
                sendTo(player, message);
            }
        }
    }

    public static void sendTo(ServerPlayer receiver, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver), message);
    }
}
