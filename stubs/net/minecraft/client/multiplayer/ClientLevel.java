// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.client.multiplayer;

public class ClientLevel extends net.minecraft.world.level.Level implements net.minecraft.world.level.BlockAndTintGetter {
    /** getGameTime */ public long m_46467_() { throw new UnsupportedOperationException(); }
    /** getSkyDarken(partialTick): 0.2 at night .. 1 by day */ public float m_104805_(float partialTick) { throw new UnsupportedOperationException(); }
    /** getBlockState (3.6.2; the original JAR links it on ClientLevel) */ public net.minecraft.world.level.block.state.BlockState m_8055_(net.minecraft.core.BlockPos pos) { throw new UnsupportedOperationException(); }
    /** players */ public java.util.List<net.minecraft.client.player.AbstractClientPlayer> m_6907_() { throw new UnsupportedOperationException(); }
}
