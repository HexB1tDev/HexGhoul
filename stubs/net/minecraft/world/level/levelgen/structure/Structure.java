// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.level.levelgen.structure;

public abstract class Structure {
    protected Structure(Structure.StructureSettings settings) { throw new UnsupportedOperationException(); }
    /** findGenerationPoint (called by name through ObfuscationReflectionHelper) */
    public abstract java.util.Optional<Structure.GenerationStub> m_214086_(Structure.GenerationContext context);
    /** type */ public abstract StructureType<?> m_213658_();
    /** Forge */ public net.minecraftforge.common.world.ModifiableStructureInfo modifiableStructureInfo() { throw new UnsupportedOperationException(); }

    public static final class StructureSettings {
    }

    public static final class GenerationStub {
    }

    public static final class GenerationContext {
        /** chunkGenerator */ public net.minecraft.world.level.chunk.ChunkGenerator f_226622_() { throw new UnsupportedOperationException(); }
        /** randomState */ public net.minecraft.world.level.levelgen.RandomState f_226624_() { throw new UnsupportedOperationException(); }
        /** chunkPos */ public net.minecraft.world.level.ChunkPos f_226628_() { throw new UnsupportedOperationException(); }
        /** heightAccessor */ public net.minecraft.world.level.LevelHeightAccessor f_226629_() { throw new UnsupportedOperationException(); }
    }
}
