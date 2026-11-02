package com.hex.kagune.control;

import com.hex.kagune.control.village.VillageGround;
import com.hex.kagune.control.village.VillageRules;
import com.hex.kagune.control.village.VillageTops;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

/**
 * 2.0.1: the expansion's own structure type, {@code hexkagune:dry_jigsaw} (the village's): a jigsaw structure, read from the same fields
 * as vanilla's, that only starts on dry, fairly flat land ({@link VillageGround}). The owner: "the villages are spawning in big bodies of
 * water when they should look for dry land to attempt to spawn" (vanilla's jigsaw built land under the Brick Village in the sea).
 */
public final class ControlStructures {
    private static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.f_256938_, "hexkagune");
    /** hexkagune:dry_jigsaw. */
    public static final StructureType<DryJigsaw> DRY_JIGSAW = () -> DryJigsaw.CODEC;

    static {
        TYPES.register("dry_jigsaw", () -> DRY_JIGSAW);
    }

    private ControlStructures() {
    }

    static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }

    /** A vanilla jigsaw structure (its own fields, its own placing) started only where {@link VillageGround#suits} says. */
    public static final class DryJigsaw extends Structure {
        /** Vanilla's jigsaw fields, inline (its map codec), read into one and wrapped. */
        static final Codec<DryJigsaw> CODEC = ((MapCodec.MapCodecCodec<JigsawStructure>)JigsawStructure.f_227604_).codec()
            .xmap(DryJigsaw::new, dry -> dry.inner).codec();
        private static Method find;
        final JigsawStructure inner;

        DryJigsaw(JigsawStructure inner) {
            super(inner.modifiableStructureInfo().getOriginalStructureInfo().structureSettings());
            this.inner = inner;
        }

        @Override
        public Optional<Structure.GenerationStub> m_214086_(Structure.GenerationContext context) {
            ChunkGenerator generator = context.f_226622_();
            ChunkPos chunk = context.f_226628_();
            int n = VillageGround.SAMPLES.length;
            int[] surface = new int[n];
            int[] floor = new int[n];
            for (int i = 0; i < n; i++) {
                int x = chunk.m_45604_() + VillageGround.SAMPLES[i][0];
                int z = chunk.m_45605_() + VillageGround.SAMPLES[i][1];
                surface[i] = generator.m_214096_(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.f_226629_(), context.f_226624_());
                floor[i] = generator.m_214096_(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.f_226629_(), context.f_226624_());
            }
            if (!VillageGround.suits(surface, floor)) {
                return Optional.empty();
            }
            // 2.0.2: vanilla's own placing called straight (public in vanilla's jigsaw), by name if the game says it may not be.
            try {
                return this.inner.m_214086_(context);
            } catch (IllegalAccessError denied) {
                try {
                    @SuppressWarnings("unchecked")
                    Optional<Structure.GenerationStub> stub = (Optional<Structure.GenerationStub>)find().invoke(this.inner, context);
                    return stub;
                } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                    Safety.caught(failure, "ControlStructures.DryJigsaw");
                    return Optional.empty();
                }
            } catch (RuntimeException | LinkageError failure) {
                Safety.caught(failure, "ControlStructures.DryJigsaw");
                return Optional.empty();
            }
        }

        /**
         * 2.0.4 (the owner: "the area where the village spawns should be erased"): as each chunk of it is placed, the world's own ground
         * over the village's (a hill, a mountain's foot, standing in its streets and yards) is cleared, column by column, from just over
         * its own highest block there (never below its ground: the world's ground still meets its low edges) up.
         */
        @Override
        public void m_214110_(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
                              ChunkPos chunk, PiecesContainer pieces) {
            try {
                clear(level, chunk, pieces);
            } catch (RuntimeException | LinkageError failure) {
                Safety.caught(failure, "ControlStructures.DryJigsaw.clear");
            }
        }

        /** The world's own blocks over the village in {@code chunk} made air (its start piece, the middle tile, tells where it lies, turned). */
        static void clear(WorldGenLevel level, ChunkPos chunk, PiecesContainer pieces) {
            java.util.List<StructurePiece> all = pieces.f_192741_();
            if (all.isEmpty() || !(all.get(0) instanceof PoolElementStructurePiece start)) {
                return;
            }
            BlockPos at = start.m_72646_();
            int turns = ((Enum<?>)(Object)start.m_6830_()).ordinal(); // NONE, CLOCKWISE_90, CLOCKWISE_180, COUNTERCLOCKWISE_90: quarter turns clockwise
            BlockState air = Blocks.f_50016_.m_49966_();
            for (int x = chunk.m_45604_(); x < chunk.m_45604_() + 16; x++) {
                for (int z = chunk.m_45605_(); z < chunk.m_45605_() + 16; z++) {
                    int[] local = VillageRules.unturn(x - at.m_123341_(), z - at.m_123343_(), turns);
                    int from = VillageTops.clearFrom(local[0] + VillageTops.MIDDLE, local[1] + VillageTops.MIDDLE);
                    if (from < 0) {
                        continue;
                    }
                    int top = level.m_6924_(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                    for (int y = at.m_123342_() + from; y < top; y++) {
                        level.m_7731_(new BlockPos(x, y, z), air, 2);
                    }
                }
            }
        }

        @Override
        public StructureType<?> m_213658_() {
            return DRY_JIGSAW;
        }

        /** Vanilla's findGenerationPoint (protected or public: called by name, as Forge maps it). */
        private static Method find() {
            Method method = find;
            if (method == null) {
                method = net.minecraftforge.fml.util.ObfuscationReflectionHelper.findMethod(Structure.class, "m_214086_", Structure.GenerationContext.class);
                find = method;
            }
            return method;
        }
    }
}
