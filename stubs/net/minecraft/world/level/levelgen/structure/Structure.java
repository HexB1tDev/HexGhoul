// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.level.levelgen.structure;

public abstract class Structure {
    protected Structure(Structure.StructureSettings settings) { throw new UnsupportedOperationException(); }
    /** findGenerationPoint (called by name through ObfuscationReflectionHelper) */
    public abstract java.util.Optional<Structure.GenerationStub> m_214086_(Structure.GenerationContext context);
    /** type */ public abstract StructureType<?> m_213658_();
    /** afterPlace (per chunk, its pieces placed) */
    public void m_214110_(net.minecraft.world.level.WorldGenLevel level, net.minecraft.world.level.StructureManager structures,
        net.minecraft.world.level.chunk.ChunkGenerator generator, net.minecraft.util.RandomSource random, BoundingBox box, net.minecraft.world.level.ChunkPos chunk,
        net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer pieces) { throw new UnsupportedOperationException(); }
    /** Forge */ public net.minecraftforge.common.world.ModifiableStructureInfo modifiableStructureInfo() { throw new UnsupportedOperationException(); }

    /** simpleCodec (4.0.0: the owner's places, ControlStructures.Site) */
    public static <S extends Structure> com.mojang.serialization.Codec<S> m_226607_(java.util.function.Function<Structure.StructureSettings, S> factory) {
        throw new UnsupportedOperationException();
    }

    public static final class StructureSettings {
    }

    public static final class GenerationStub {
        public GenerationStub(net.minecraft.core.BlockPos position,
            java.util.function.Consumer<net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder> generator) {
            throw new UnsupportedOperationException();
        }
    }

    public static final class GenerationContext {
        /** chunkGenerator */ public net.minecraft.world.level.chunk.ChunkGenerator f_226622_() { throw new UnsupportedOperationException(); }
        /** randomState */ public net.minecraft.world.level.levelgen.RandomState f_226624_() { throw new UnsupportedOperationException(); }
        /** structureTemplateManager */
        public net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager f_226625_() { throw new UnsupportedOperationException(); }
        /** random */ public net.minecraft.world.level.levelgen.WorldgenRandom f_226626_() { throw new UnsupportedOperationException(); }
        /** chunkPos */ public net.minecraft.world.level.ChunkPos f_226628_() { throw new UnsupportedOperationException(); }
        /** heightAccessor */ public net.minecraft.world.level.LevelHeightAccessor f_226629_() { throw new UnsupportedOperationException(); }
    }
}
