// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.level.levelgen.structure;

public abstract class Structure {
    protected Structure(Structure.StructureSettings settings) { throw new UnsupportedOperationException(); }
    /** findGenerationPoint (called by name through ObfuscationReflectionHelper) */
    public abstract java.util.Optional<Structure.GenerationStub> m_214086_(Structure.GenerationContext context);
    /** type */ public abstract StructureType<?> m_213658_();
    /**
     * findValidGenerationPoint (4.1.9: public and overridable in 1.20.1: findGenerationPoint filtered by the structure's biomes at the
     * stub's position; world generation and StructureCheck, /locate's, both call it)
     */
    public java.util.Optional<Structure.GenerationStub> m_262864_(Structure.GenerationContext context) { throw new UnsupportedOperationException(); }
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
        /** getPiecesBuilder (4.1.1: where a start's pieces lie, for the dry-land search) */
        public net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder m_226677_() { throw new UnsupportedOperationException(); }
        /** position (record accessor) */ public net.minecraft.core.BlockPos f_226669_() { throw new UnsupportedOperationException(); }
    }

    public static final class GenerationContext {
        /** 4.1.1: the record's canonical constructor (a start moved to find dry land) */
        public GenerationContext(net.minecraft.core.RegistryAccess registryAccess, net.minecraft.world.level.chunk.ChunkGenerator chunkGenerator,
            net.minecraft.world.level.biome.BiomeSource biomeSource, net.minecraft.world.level.levelgen.RandomState randomState,
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager structureTemplateManager,
            net.minecraft.world.level.levelgen.WorldgenRandom random, long seed, net.minecraft.world.level.ChunkPos chunkPos,
            net.minecraft.world.level.LevelHeightAccessor heightAccessor, java.util.function.Predicate<net.minecraft.core.Holder<?>> validBiome) {
            throw new UnsupportedOperationException();
        }
        /** 4.1.9: the constructor the game itself makes each start's context with (its random seeded from the seed and the chunk) */
        public GenerationContext(net.minecraft.core.RegistryAccess registryAccess, net.minecraft.world.level.chunk.ChunkGenerator chunkGenerator,
            net.minecraft.world.level.biome.BiomeSource biomeSource, net.minecraft.world.level.levelgen.RandomState randomState,
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager structureTemplateManager, long seed,
            net.minecraft.world.level.ChunkPos chunkPos, net.minecraft.world.level.LevelHeightAccessor heightAccessor,
            java.util.function.Predicate<net.minecraft.core.Holder<?>> validBiome) {
            throw new UnsupportedOperationException();
        }
        /** registryAccess */ public net.minecraft.core.RegistryAccess f_226621_() { throw new UnsupportedOperationException(); }
        /** biomeSource */ public net.minecraft.world.level.biome.BiomeSource f_226623_() { throw new UnsupportedOperationException(); }
        /** seed */ public long f_226627_() { throw new UnsupportedOperationException(); }
        /** validBiome */ public java.util.function.Predicate<net.minecraft.core.Holder<?>> f_226630_() { throw new UnsupportedOperationException(); }
        /** chunkGenerator */ public net.minecraft.world.level.chunk.ChunkGenerator f_226622_() { throw new UnsupportedOperationException(); }
        /** randomState */ public net.minecraft.world.level.levelgen.RandomState f_226624_() { throw new UnsupportedOperationException(); }
        /** structureTemplateManager */
        public net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager f_226625_() { throw new UnsupportedOperationException(); }
        /** random */ public net.minecraft.world.level.levelgen.WorldgenRandom f_226626_() { throw new UnsupportedOperationException(); }
        /** chunkPos */ public net.minecraft.world.level.ChunkPos f_226628_() { throw new UnsupportedOperationException(); }
        /** heightAccessor */ public net.minecraft.world.level.LevelHeightAccessor f_226629_() { throw new UnsupportedOperationException(); }
    }
}
