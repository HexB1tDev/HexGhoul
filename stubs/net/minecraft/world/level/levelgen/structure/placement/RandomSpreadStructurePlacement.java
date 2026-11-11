// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.level.levelgen.structure.placement;

public class RandomSpreadStructurePlacement extends StructurePlacement {
    /** (spacing, separation, spreadType, salt) (4.1.36) */
    public RandomSpreadStructurePlacement(int spacing, int separation, RandomSpreadType spreadType, int salt) { throw new UnsupportedOperationException(); }
    /** spacing (4.1.9) */ public int m_205003_() { throw new UnsupportedOperationException(); }
    /** getPotentialStructureChunk (4.1.9: the start chunk offered in the region holding a chunk, from the level's seed) */
    public net.minecraft.world.level.ChunkPos m_227008_(long seed, int x, int z) { throw new UnsupportedOperationException(); }
    /** isPlacementChunk (4.1.36) */
    @Override protected boolean m_214090_(net.minecraft.world.level.chunk.ChunkGeneratorStructureState state, int x, int z) { throw new UnsupportedOperationException(); }
}
