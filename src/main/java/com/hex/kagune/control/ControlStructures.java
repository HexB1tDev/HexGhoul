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

    /**
     * 4.0.0 (The Great Exploration): the owner's three new places, each its own type ({@link Site}): hexkagune:site_village_1 (the human
     * town), hexkagune:site_village_2 (the CCG settlement), hexkagune:site_aogiri_hideout (Aogiri Tree's hideout).
     */
    private static final java.util.Map<String, StructureType<Site>> SITES = new java.util.LinkedHashMap<>();

    static {
        TYPES.register("dry_jigsaw", () -> DRY_JIGSAW);
        for (com.hex.kagune.control.village.SiteMaps.Site site : com.hex.kagune.control.village.SiteMaps.ALL) {
            Codec<Site> codec = Structure.m_226607_(settings -> new Site(settings, site));
            StructureType<Site> type = () -> codec;
            SITES.put(site.id(), type);
            TYPES.register("site_" + site.id(), () -> type);
        }
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

    /**
     * 4.0.0 (The Great Exploration): one of the owner's new places ({@link com.hex.kagune.control.village.SiteMaps}): its tiles (each a
     * template, data/hexkagune/structures/&lt;id&gt;/t_&lt;i&gt;_&lt;j&gt;.nbt) laid side by side, the whole place turned a random quarter turn,
     * its footprint centred on the chunks its start chunk can reach (8 either way: the human town is 272 blocks wide, all of them), its
     * ground on the world's. It starts only on dry, fairly flat land ({@link VillageGround}); its structure's terrain adaptation (beard_box)
     * fills the world's ground up under it and clears it off over it. Its people are its templates' entities.
     */
    public static final class Site extends Structure {
        private static final net.minecraft.world.level.block.Rotation[] TURNS = {net.minecraft.world.level.block.Rotation.NONE,
            net.minecraft.world.level.block.Rotation.CLOCKWISE_90, net.minecraft.world.level.block.Rotation.CLOCKWISE_180,
            net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90};
        /** How far from its start chunk's corner a structure's pieces may reach (its 8 chunks either way). */
        private static final int REACH_BACK = 128;
        private static final int REACH_ON = 143;
        final com.hex.kagune.control.village.SiteMaps.Site site;

        Site(Structure.StructureSettings settings, com.hex.kagune.control.village.SiteMaps.Site site) {
            super(settings);
            this.site = site;
        }

        @Override
        public Optional<Structure.GenerationStub> m_214086_(Structure.GenerationContext context) {
            try {
                return this.place(context);
            } catch (RuntimeException | LinkageError failure) {
                Safety.caught(failure, "ControlStructures.Site");
                return Optional.empty();
            }
        }

        private Optional<Structure.GenerationStub> place(Structure.GenerationContext context) {
            ChunkGenerator generator = context.f_226622_();
            ChunkPos chunk = context.f_226628_();
            int turns = context.f_226626_().m_188503_(4);
            boolean across = (turns & 1) == 1;
            int sizeX = across ? this.site.depth() : this.site.width();
            int sizeZ = across ? this.site.width() : this.site.depth();
            // Its footprint, turned, centred on what the start chunk reaches (never past it).
            int minX = clampMin(chunk.m_45604_() + 8 - sizeX / 2, chunk.m_45604_(), sizeX);
            int minZ = clampMin(chunk.m_45605_() + 8 - sizeZ / 2, chunk.m_45605_(), sizeZ);
            // The ground under it, on a 5 x 5 grid over its footprint: dry and fairly flat, or it is not built here.
            int n = 5;
            int[] surface = new int[n * n];
            int[] floor = new int[n * n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    int x = minX + 2 + (sizeX - 5) * i / (n - 1);
                    int z = minZ + 2 + (sizeZ - 5) * j / (n - 1);
                    surface[i * n + j] = generator.m_214096_(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.f_226629_(), context.f_226624_());
                    floor[i * n + j] = generator.m_214096_(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.f_226629_(), context.f_226624_());
                }
            }
            if (!VillageGround.suitsGrid(surface, floor, n)) {
                return Optional.empty();
            }
            // Its ground on the world's: the middle of the dry ground's heights (the first free height: its ground block one under it).
            int[] dry = java.util.Arrays.stream(floor).sorted().toArray();
            int base = dry[dry.length / 2] - 1 - this.site.ground();
            net.minecraft.world.level.block.Rotation rotation = TURNS[turns];
            // The template's corner (its x 0, z 0) as the turn takes it: the turned footprint's least corner is (minX, minZ).
            int[] least = com.hex.kagune.control.village.SiteMaps.turnedLeast(this.site.width(), this.site.depth(), turns);
            int ox = minX - least[0];
            int oz = minZ - least[1];
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager templates = context.f_226625_();
            com.hex.kagune.control.village.SiteMaps.Site site = this.site;
            BlockPos start = new BlockPos(minX + sizeX / 2, base + site.ground() + 1, minZ + sizeZ / 2);
            return Optional.of(new Structure.GenerationStub(start, builder -> {
                for (int i = 0; i < site.across(); i++) {
                    for (int j = 0; j < site.down(); j++) {
                        int[] corner = VillageRules.turn(site.xEdges()[i], site.zEdges()[j], turns);
                        BlockPos at = new BlockPos(ox + corner[0], base, oz + corner[1]);
                        net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement element =
                            net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement.m_210526_(site.tile(i, j))
                                .apply(net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection.RIGID);
                        BoundingBox box = element.m_214015_(templates, at, rotation);
                        // Its ground one over its tiles' lowest layer by its own height: the world's ground is filled up to it, cleared over it.
                        builder.m_142679_(new PoolElementStructurePiece(templates, element, at, site.ground() + 1, rotation, box));
                    }
                }
            }));
        }

        /** The least corner of a run of {@code size} blocks centred at {@code want}, kept within what the chunk at {@code corner} reaches. */
        static int clampMin(int want, int corner, int size) {
            int lo = corner - REACH_BACK;
            int hi = corner + REACH_ON - size + 1;
            return hi < lo ? lo : Math.max(lo, Math.min(hi, want));
        }

        @Override
        public StructureType<?> m_213658_() {
            return SITES.get(this.site.id());
        }
    }
}
