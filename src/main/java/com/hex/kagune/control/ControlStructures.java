package com.hex.kagune.control;

import com.hex.kagune.control.village.DryLand;
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
    /** 4.1.1: hexkagune:dry_place (Anteiku, the library, Ken's tower): a jigsaw place moved, near its start, onto dry land. */
    public static final StructureType<DryPlace> DRY_PLACE = () -> DryPlace.CODEC;

    /**
     * 4.0.0 (The Great Exploration): the owner's three new places, each its own type ({@link Site}): hexkagune:site_village_1 (the human
     * town), hexkagune:site_village_2 (the CCG settlement), hexkagune:site_aogiri_hideout (Aogiri Tree's hideout). 4.1.4: and
     * hexkagune:site_ccg_building (the CCG's building).
     */
    private static final java.util.Map<String, StructureType<Site>> SITES = new java.util.LinkedHashMap<>();

    static {
        TYPES.register("dry_jigsaw", () -> DRY_JIGSAW);
        TYPES.register("dry_place", () -> DRY_PLACE);
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
            return run(this.inner, context);
        }

        /** 2.0.2: vanilla's own placing called straight (public in vanilla's jigsaw), by name if the game says it may not be. */
        static Optional<Structure.GenerationStub> run(JigsawStructure inner, Structure.GenerationContext context) {
            try {
                return inner.m_214086_(context);
            } catch (IllegalAccessError denied) {
                try {
                    @SuppressWarnings("unchecked")
                    Optional<Structure.GenerationStub> stub = (Optional<Structure.GenerationStub>)find().invoke(inner, context);
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

        /**
         * 4.1.9 (the owner: "it should be able to spawn literally in any biome"): the game's own test of the biome at the start (its
         * structure's biomes, a list: a mod's biome missing from it was no place at all) is left out; this place judges its own land (any
         * biome but a wet one: {@link ControlStructures#biome}, with the dry ground under and round it). Its structure's biomes
         * (#hexkagune:overworld) only keep it to the overworld (a set runs only where its biomes can be made). The world's making and
         * /locate both ask through here.
         */
        @Override
        public Optional<Structure.GenerationStub> m_262864_(Structure.GenerationContext context) {
            return this.m_214086_(context);
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
            ChunkPos chunk = context.f_226628_();
            int turns = context.f_226626_().m_188503_(4);
            boolean across = (turns & 1) == 1;
            int sizeX = across ? this.site.depth() : this.site.width();
            int sizeZ = across ? this.site.width() : this.site.depth();
            if ((CCG.equals(this.site.id()) || unique(this.site)) && onPurpose(context)) {
                // 4.1.6 (the owner: "I cant get the aogiri hideout to spawn or the ccg building at all"): /place structure builds the
                // world's one hideout or CCG building where it is asked (its footprint round this chunk, on the middle of its ground), as
                // well as where the world put its own: an old world's own may lie in ground made before it could.
                int minX = clampMin(chunk.m_45604_() + 8 - sizeX / 2, chunk.m_45604_(), sizeX);
                int minZ = clampMin(chunk.m_45605_() + 8 - sizeZ / 2, chunk.m_45605_(), sizeZ);
                return Optional.of(this.stub(context, turns, minX, minZ, sizeX, sizeZ, standing(ground(context), minX, minZ, sizeX, sizeZ)));
            }
            if (CCG.equals(this.site.id())) {
                // 4.1.4 (the owner: "CCG building ... like the others only 1 of them spawns"): the world's one. 4.1.14: at whichever of its
                // set's spots on dry land the world makes first (PlaceClaims), clear of the hideout's and the villages' spots.
                return claimed(CCG, context, () -> {
                    if (!offered(context, spread(context, CCG, DryLand.fixed(context.f_226627_(), DryLand.SPACING, DryLand.SEPARATION, DryLand.CCG_SALT)))
                        || DryLand.near(hideoutSpread(context), chunk.f_45578_, chunk.f_45579_, DryLand.CCG_APART)
                        || DryLand.near(villageSpread(context), chunk.f_45578_, chunk.f_45579_, DryLand.CCG_APART)) {
                        return Optional.empty();
                    }
                    int span = Math.max(sizeX, sizeZ) + 2 * CCG_ROUND;
                    int middle = dryGround(context, ground(context), chunk.m_45604_() + 8 - span / 2, chunk.m_45605_() + 8 - span / 2, span);
                    return middle == NO_GROUND ? Optional.empty()
                        : Optional.of(this.stub(context, turns, chunk.m_45604_() + 8 - sizeX / 2, chunk.m_45605_() + 8 - sizeZ / 2, sizeX, sizeZ, middle));
                });
            }
            if (unique(this.site)) {
                // 4.1.1 (the owner: "the mansion is currently happily resting at the very bottom of the ocean"): the world's one hideout,
                // only ever on dry land. 4.1.14: at whichever of its set's spots on dry land the world makes first (PlaceClaims).
                return claimed(HIDEOUT, context, () -> {
                    if (!offered(context, hideoutSpread(context))) {
                        return Optional.empty();
                    }
                    int minX = clampMin(chunk.m_45604_() + 8 - sizeX / 2, chunk.m_45604_(), sizeX);
                    int minZ = clampMin(chunk.m_45605_() + 8 - sizeZ / 2, chunk.m_45605_(), sizeZ);
                    int span = Math.max(sizeX, sizeZ) + 2 * CCG_ROUND;
                    int middle = dryGround(context, ground(context), minX + sizeX / 2 - span / 2, minZ + sizeZ / 2 - span / 2, span);
                    return middle == NO_GROUND ? Optional.empty() : Optional.of(this.stub(context, turns, minX, minZ, sizeX, sizeZ, middle));
                });
            }
            // 4.1.1: every other place at the first spot its start chunk reaches (from the middle out) on dry, fairly flat land, with no
            // water under it or at its edge, in a biome it may stand in; or not here.
            DryLand.Ground ground = ground(context);
            int[] xs = DryLand.runs(chunk.m_45604_() + 8 - sizeX / 2, chunk.m_45604_(), sizeX, SHIFT);
            int[] zs = DryLand.runs(chunk.m_45605_() + 8 - sizeZ / 2, chunk.m_45605_(), sizeZ, SHIFT);
            for (int[] at : DryLand.spots(xs, zs, TRIES)) {
                int[] floor = grid(ground, at[0], at[1], sizeX, sizeZ);
                if (floor == null || !flat(floor)) {
                    continue;
                }
                int middle = DryLand.median(floor);
                if (biome(context, at[0] + sizeX / 2, middle, at[1] + sizeZ / 2) && DryLand.dry(ground, middle, at[0], at[1], sizeX, sizeZ)) {
                    return Optional.of(this.stub(context, turns, at[0], at[1], sizeX, sizeZ, middle));
                }
            }
            return Optional.empty();
        }

        /**
         * The place laid with its footprint's least corner at ({@code minX}, {@code minZ}), turned {@code turns}, its ground on the world's
         * at {@code middle} (the middle of the dry ground's heights: the first free height, its ground block one under it).
         */
        private Structure.GenerationStub stub(Structure.GenerationContext context, int turns, int minX, int minZ, int sizeX, int sizeZ, int middle) {
            int base = middle - 1 - this.site.ground();
            net.minecraft.world.level.block.Rotation rotation = TURNS[turns];
            // The template's corner (its x 0, z 0) as the turn takes it: the turned footprint's least corner is (minX, minZ).
            int[] least = com.hex.kagune.control.village.SiteMaps.turnedLeast(this.site.width(), this.site.depth(), turns);
            int ox = minX - least[0];
            int oz = minZ - least[1];
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager templates = context.f_226625_();
            com.hex.kagune.control.village.SiteMaps.Site site = this.site;
            BlockPos start = new BlockPos(minX + sizeX / 2, base + site.ground() + 1, minZ + sizeZ / 2);
            return new Structure.GenerationStub(start, builder -> {
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
            });
        }

        /** 4.1.1: how far apart (blocks) the spots a place may be moved to are, and how many are tried before it gives up there. */
        static final int SHIFT = 32;
        static final int TRIES = 9;
        /** 4.1.4: the CCG building's site, and how far out round it the ground is judged too (its patrol walks there). 4.1.14: the hideout's too. */
        static final String CCG = "ccg_building";
        static final String HIDEOUT = "aogiri_hideout";
        static final int CCG_ROUND = 8;

        /** 4.1.14: a 5 x 5 grid's ground with water over it, or too steep, or not to be stood on: no ground. */
        static final int NO_GROUND = Integer.MIN_VALUE;

        /**
         * 4.1.14: where a one-of-a-kind place stands on the {@code size} x {@code size} ground from ({@code minX}, {@code minZ}): the middle of
         * its 5 x 5 grid's heights if none is under water, it is no steeper than {@link #STEEPEST}, the biome at its middle may hold it and no
         * water is in it or at its edge; else {@link #NO_GROUND}.
         */
        static int dryGround(Structure.GenerationContext context, DryLand.Ground ground, int minX, int minZ, int size) {
            int[] floor = grid(ground, minX, minZ, size, size);
            if (floor == null || DryLand.spread(floor) > STEEPEST) {
                return NO_GROUND;
            }
            int middle = DryLand.median(floor);
            return ControlStructures.biome(context, minX + size / 2, middle, minZ + size / 2) && DryLand.dry(ground, middle, minX, minZ, size, size)
                ? middle : NO_GROUND;
        }

        /** 4.1.14: the steepest ground (blocks between its lowest and highest of a 5 x 5 grid) a one-of-a-kind place is built on. */
        static final int STEEPEST = 2 * VillageGround.FLAT;

        /**
         * 4.1.6: where a place asked for on purpose stands: the middle of the dry ground of its 5 x 5 grid, or of the floor under the water
         * if all of it is under water.
         */
        static int standing(DryLand.Ground ground, int minX, int minZ, int sizeX, int sizeZ) {
            int[] all = heights(ground, minX, minZ, sizeX, sizeZ);
            int[] dry = dryHeights(all);
            if (dry.length > 0) {
                return DryLand.median(dry);
            }
            int n = 5;
            int[] floor = new int[n * n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    floor[i * n + j] = ground.floor(minX + 2 + (sizeX - 5) * i / (n - 1), minZ + 2 + (sizeZ - 5) * j / (n - 1));
                }
            }
            return DryLand.median(floor);
        }

        /**
         * 4.1.6: true if whoever asks for the place lets it stand in any biome at all: the /place structure command does (its test takes
         * anything); the world's own making and /locate test the place's own biomes (a tag: a missing biome is no biome of it).
         */
        static boolean onPurpose(Structure.GenerationContext context) {
            try {
                return context.f_226630_().test(null);
            } catch (RuntimeException asked) {
                return false;
            }
        }

        /**
         * The world's dry ground on a 5 x 5 grid over a footprint (sample i * 5 + j at row i, column j), or null if there is water over the
         * ground at any of them.
         */
        static int[] grid(DryLand.Ground ground, int minX, int minZ, int sizeX, int sizeZ) {
            int n = 5;
            int[] floor = new int[n * n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    int x = minX + 2 + (sizeX - 5) * i / (n - 1);
                    int z = minZ + 2 + (sizeZ - 5) * j / (n - 1);
                    int at = ground.floor(x, z);
                    if (ground.surface(x, z) > at) {
                        return null;
                    }
                    floor[i * n + j] = at;
                }
            }
            return floor;
        }

        /** 4.1.6: a sample of {@link #heights} with water over its ground. */
        static final int WATER = Integer.MIN_VALUE;

        /** 4.1.6: the world's ground on a 5 x 5 grid over a footprint (sample i * 5 + j at row i, column j), {@link #WATER} where water is over it. */
        static int[] heights(DryLand.Ground ground, int minX, int minZ, int sizeX, int sizeZ) {
            int n = 5;
            int[] floor = new int[n * n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    int x = minX + 2 + (sizeX - 5) * i / (n - 1);
                    int z = minZ + 2 + (sizeZ - 5) * j / (n - 1);
                    int at = ground.floor(x, z);
                    floor[i * n + j] = ground.surface(x, z) > at ? WATER : at;
                }
            }
            return floor;
        }

        /** 4.1.6: the samples of {@link #heights} that are dry ground. */
        static int[] dryHeights(int[] heights) {
            return java.util.Arrays.stream(heights).filter(at -> at != WATER).toArray();
        }

        /** True if the dry ground's highest and lowest are no more than {@link VillageGround#FLAT} apart. */
        static boolean flat(int[] floor) {
            return floor.length > 0 && DryLand.spread(floor) <= VillageGround.FLAT;
        }

        /** 4.0.2: true if {@code site} is the world's one of its kind (placed by a ring, never turned away): the Aogiri Tree's hideout. */
        static boolean unique(com.hex.kagune.control.village.SiteMaps.Site site) {
            return "aogiri_hideout".equals(site.id());
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

        /**
         * 4.1.9 (the owner: "it should be able to spawn literally in any biome"): the game's own test of the biome at the start (its
         * structure's biomes, a list: a mod's biome missing from it was no place at all) is left out; this place judges its own land (any
         * biome but a wet one: {@link ControlStructures#biome}, with the dry ground under and round it). Its structure's biomes
         * (#hexkagune:overworld) only keep it to the overworld (a set runs only where its biomes can be made). The world's making and
         * /locate both ask through here.
         */
        @Override
        public Optional<Structure.GenerationStub> m_262864_(Structure.GenerationContext context) {
            return this.m_214086_(context);
        }
    }

    /** 4.1.12: the one-of-a-kind places, as /kagune places lists them. */
    static final java.util.List<String> ONE_OF_A_KIND = java.util.List.of("aogiri_hideout", "ccg_building", "cyber_tower", "anteiku", "library");

    /** 4.1.14: each one-of-a-kind place's name, as /kagune places says it. */
    static String named(String id) {
        return switch (id) {
            case "aogiri_hideout" -> "Aogiri Tree's hideout";
            case "ccg_building" -> "the CCG building";
            case "cyber_tower" -> "Ken's tower";
            case "anteiku" -> "Anteiku";
            default -> "the library";
        };
    }

    /** 4.1.12: the lines /kagune places shows (4.1.14: where each was built in the world being made, or that it is not yet). */
    public static java.util.List<String> report() {
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (String id : ONE_OF_A_KIND) {
            int[] at = PlaceClaims.claimed(id);
            lines.add(at == null ? notYet(id) : named(id) + ": x " + ((at[0] << 4) + 8) + ", z " + ((at[1] << 4) + 8));
        }
        return lines;
    }

    /** 4.1.14: a place not built yet. */
    static String notYet(String id) {
        return named(id) + ": not built yet. /locate structure hexkagune:" + id + " finds the nearest spot that will hold it (it is built there).";
    }

    /**
     * 4.1.13 (the owner: "And I still can't" find them with /locate): as {@link #report()}, and each built place's start chunk made in full in
     * {@code level} and looked in: built there; or, if something stopped it after its spot was taken (another mod's structure rules), the spot
     * given up (PlaceClaims), so the next one the world makes holds it.
     */
    public static java.util.List<String> report(net.minecraft.world.level.LevelReader level) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (String id : ONE_OF_A_KIND) {
            int[] at = PlaceClaims.claimed(id);
            if (at == null) {
                lines.add(notYet(id));
                continue;
            }
            String where = named(id) + ": x " + ((at[0] << 4) + 8) + ", z " + ((at[1] << 4) + 8);
            try {
                if (standsIn(level, level.m_6325_(at[0], at[1]), id)) {
                    lines.add(where + " (built there)");
                } else {
                    PlaceClaims.release(id, at[0], at[1]);
                    lines.add(where + ": NOT built there, something stopped it (another mod's structure rules). Given up: /locate structure hexkagune:"
                        + id + " finds the next spot.");
                }
            } catch (RuntimeException | LinkageError failure) {
                Safety.caught(failure, "ControlStructures.report");
                lines.add(where + " (could not be checked)");
            }
        }
        return lines;
    }

    /** 4.1.13: true if hexkagune:{@code id}'s start is in {@code chunk} of {@code level}, or it is no structure of this world (nothing to wait for). */
    static boolean standsIn(net.minecraft.world.level.LevelReader level, net.minecraft.world.level.chunk.ChunkAccess chunk, String id) {
        Structure structure = level.m_9598_().m_175515_(Registries.f_256944_).m_7745_(net.minecraft.resources.ResourceLocation.m_214293_("hexkagune", id));
        if (structure == null) {
            return true;
        }
        net.minecraft.world.level.levelgen.structure.StructureStart found = chunk.m_213652_(structure);
        return found != null && found.m_73603_();
    }

    /**
     * 4.1.14: a one-of-a-kind place asked for at this start chunk: turned away if another spot of the world already holds it
     * ({@link PlaceClaims}); else laid out here ({@code here}: empty if this spot won't do), and, as the world makes this chunk (not as
     * /locate only asks), the spot taken: the first the world makes holds it, and no other ever does.
     */
    static Optional<Structure.GenerationStub> claimed(String id, Structure.GenerationContext context, java.util.function.Supplier<Optional<Structure.GenerationStub>> here) {
        ChunkPos chunk = context.f_226628_();
        int[] taken = PlaceClaims.claimed(id);
        if (taken != null && (taken[0] != chunk.f_45578_ || taken[1] != chunk.f_45579_)) {
            return Optional.empty();
        }
        Optional<Structure.GenerationStub> stub = here.get();
        if (stub.isPresent() && making(context) && !PlaceClaims.claim(id, chunk.f_45578_, chunk.f_45579_)) {
            return Optional.empty(); // another spot was taken while this one was laid out
        }
        return stub;
    }

    /**
     * 4.1.14: true if the world is making this start chunk (its context's heights are the chunk being made), false if /locate (or the
     * structure check behind it) only asks whether a start could be here (its heights are the level's).
     */
    static boolean making(Structure.GenerationContext context) {
        return context.f_226629_() instanceof net.minecraft.world.level.chunk.ChunkAccess;
    }

    /** 4.1.14: true if this start chunk is one of {@code spread}'s spots (the game asks only there; a stray ask is turned away). */
    static boolean offered(Structure.GenerationContext context, DryLand.Spread spread) {
        ChunkPos chunk = context.f_226628_();
        int[] offer = spread.chunk(chunk.f_45578_, chunk.f_45579_);
        return offer[0] == chunk.f_45578_ && offer[1] == chunk.f_45579_;
    }

    /** 4.1.1: the biomes no structure may stand in or beside (a sea, a river, a beach, a swamp): data/hexkagune/tags/worldgen/biome/wet.json. */
    static final net.minecraft.tags.TagKey<?> WET = net.minecraft.tags.TagKey.m_203882_(Registries.f_256952_,
        net.minecraft.resources.ResourceLocation.m_214293_("hexkagune", "wet"));

    /** 4.1.1: the world as the game will make it (its two worldgen heightmaps: water counted as ground, and not; its biomes). */
    static DryLand.Ground ground(Structure.GenerationContext context) {
        ChunkGenerator generator = context.f_226622_();
        net.minecraft.world.level.LevelHeightAccessor heights = context.f_226629_();
        net.minecraft.world.level.levelgen.RandomState random = context.f_226624_();
        net.minecraft.world.level.biome.BiomeSource biomes = context.f_226623_();
        net.minecraft.world.level.biome.Climate.Sampler sampler = random.m_224579_();
        return new DryLand.Ground() {
            @Override
            public boolean wet(int x, int y, int z) {
                return biomes.m_203407_(x >> 2, y >> 2, z >> 2, sampler).m_203656_(WET);
            }

            @Override
            public int surface(int x, int z) {
                return generator.m_214096_(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, random);
            }

            @Override
            public int floor(int x, int z) {
                return generator.m_214096_(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights, random);
            }
        };
    }

    /**
     * 4.1.9: the set hexkagune:{@code set}'s random spread as this world has it (its placement in the structure set registry: its own
     * spacing and offers, whatever a mod made of them; Structure Essentials, in the owner's pack, can change every set's), or
     * {@code fallback} (the set's file's numbers) if it can't be read or is not a random spread.
     */
    static DryLand.Spread spread(Structure.GenerationContext context, String set, DryLand.Spread fallback) {
        try {
            net.minecraft.world.level.levelgen.structure.StructureSet found = context.f_226621_().m_175515_(Registries.f_256998_)
                .m_7745_(net.minecraft.resources.ResourceLocation.m_214293_("hexkagune", set));
            if (found != null && found.f_210004_() instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement live) {
                long seed = context.f_226627_();
                int spacing = live.m_205003_();
                return new DryLand.Spread() {
                    @Override
                    public int spacing() {
                        return spacing;
                    }

                    @Override
                    public int[] chunk(int x, int z) {
                        ChunkPos at = live.m_227008_(seed, x, z);
                        return new int[] {at.f_45578_, at.f_45579_};
                    }
                };
            }
        } catch (RuntimeException | LinkageError failure) {
            Safety.caught(failure, "ControlStructures.spread");
        }
        return fallback;
    }

    /** 4.1.9: the hideout's, the villages' and the CCG building's spreads as this world has them. */
    static DryLand.Spread hideoutSpread(Structure.GenerationContext context) {
        return spread(context, "aogiri_hideout", DryLand.fixed(context.f_226627_(), DryLand.SPACING, DryLand.SEPARATION, DryLand.SALT));
    }

    static DryLand.Spread villageSpread(Structure.GenerationContext context) {
        return spread(context, "villages", DryLand.fixed(context.f_226627_(), DryLand.VILLAGE_SPACING, DryLand.VILLAGE_SEPARATION, DryLand.VILLAGE_SALT));
    }

    /**
     * 4.1.1: true if a structure may stand in the biome at block ({@code x}, {@code y}, {@code z}). 4.1.9 (the owner: "it should be able
     * to spawn literally in any biome ... It should also be fully compatible with Biomes of Plenty"): any biome at all, a mod's too
     * (Biomes O' Plenty's, Regions Unexplored's, any), but a wet one (a sea, a river, a beach, a swamp: hexkagune:wet). Before, it was the
     * structure's own short list, and every biome a mod added was no place: in a world of a biome mod's biomes the hideout, the CCG building
     * and Ken's tower were nowhere, /locate and all.
     */
    static boolean biome(Structure.GenerationContext context, int x, int y, int z) {
        net.minecraft.core.Holder<?> biome = context.f_226623_().m_203407_(x >> 2, y >> 2, z >> 2, context.f_226624_().m_224579_());
        return !biome.m_203656_(WET);
    }

    /**
     * 4.1.1 (the owner: "make sure NONE of the structures spawns in water or collides with water bodies"): a vanilla jigsaw place (Anteiku,
     * the library, Ken's tower: one template each), read from the same fields as vanilla's, started where vanilla would start it if the land
     * there is dry; if not, moved a chunk at a time ({@link DryLand#shifts}, nearest first, as far as its start chunk reaches) to the first
     * spot where no water lies under it or round it, in a biome it may stand in; or not built at that start.
     */
    public static final class DryPlace extends Structure {
        static final Codec<DryPlace> CODEC = ((MapCodec.MapCodecCodec<JigsawStructure>)JigsawStructure.f_227604_).codec()
            .xmap(DryPlace::new, dry -> dry.inner).codec();
        /** The chunks either way a start may be moved. */
        static final int MOST = 5;
        private static final int[][] SHIFTS = DryLand.shifts(MOST);
        final JigsawStructure inner;

        DryPlace(JigsawStructure inner) {
            super(inner.modifiableStructureInfo().getOriginalStructureInfo().structureSettings());
            this.inner = inner;
        }

        @Override
        public Optional<Structure.GenerationStub> m_214086_(Structure.GenerationContext context) {
            try {
                return this.place(context);
            } catch (RuntimeException | LinkageError failure) {
                Safety.caught(failure, "ControlStructures.DryPlace");
                return Optional.empty();
            }
        }

        /**
         * 4.1.9 (the owner: "only one of each structure aside from the villages"): one Anteiku, one library and one Ken's tower a world.
         * 4.1.14: each at whichever of its set's spots the world makes first where it stands on dry land ({@link #here}) clear of the
         * villages', the hideout's, the CCG building's and the other places' spots (PlaceClaims); /place structure builds one where it is
         * asked.
         */
        private Optional<Structure.GenerationStub> place(Structure.GenerationContext context) {
            if (Site.onPurpose(context)) {
                return this.here(context);
            }
            String id = this.id(context);
            if (id == null) {
                return Optional.empty(); // its name could not be read: never one of many where the world's one belongs (asked again later)
            }
            if (DryLand.placeSalt(id) < 0) {
                return this.here(context); // a jigsaw place of someone else's on this type: wherever its own set offers it
            }
            return claimed(id, context, () -> {
                if (!offered(context, placeSpread(context, id))) {
                    return Optional.empty();
                }
                Optional<Structure.GenerationStub> stub = this.here(context);
                if (stub.isEmpty()) {
                    return stub;
                }
                BoundingBox box = stub.get().m_226677_().m_192798_();
                int x = Math.floorDiv((box.m_162395_() + box.m_162399_()) >> 1, 16);
                int z = Math.floorDiv((box.m_162398_() + box.m_162401_()) >> 1, 16);
                if (DryLand.near(villageSpread(context), x, z, DryLand.PLACE_APART) || DryLand.near(hideoutSpread(context), x, z, DryLand.PLACE_APART)
                    || DryLand.near(spread(context, Site.CCG, DryLand.fixed(context.f_226627_(), DryLand.SPACING, DryLand.SEPARATION, DryLand.CCG_SALT)),
                        x, z, DryLand.PLACE_APART)) {
                    return Optional.empty();
                }
                for (String other : DryLand.PLACES) {
                    if (!other.equals(id) && DryLand.near(placeSpread(context, other), x, z, DryLand.PLACE_APART)) {
                        return Optional.empty();
                    }
                }
                return stub;
            });
        }

        /** 4.1.9: the one-of-a-kind jigsaw place {@code id}'s set's spread as this world has it. */
        static DryLand.Spread placeSpread(Structure.GenerationContext context, String id) {
            return spread(context, id, DryLand.fixed(context.f_226627_(), DryLand.SPACING, DryLand.SEPARATION, DryLand.placeSalt(id)));
        }

        /**
         * 4.1.9: this place's name in the structure registry (hexkagune:&lt;id&gt;: its id; "" for none of ours), looked up once; null if it
         * could not be read (not kept: asked again next time).
         */
        private volatile String id;

        String id(Structure.GenerationContext context) {
            String known = this.id;
            if (known == null) {
                try {
                    net.minecraft.resources.ResourceLocation key = context.f_226621_().m_175515_(Registries.f_256944_).m_7981_(this);
                    if (key == null) {
                        return null; // not (yet) the registry's own: never one of many where the world's one belongs
                    }
                    known = "hexkagune".equals(key.m_135827_()) ? key.m_135815_() : "";
                } catch (RuntimeException | LinkageError failure) {
                    Safety.caught(failure, "ControlStructures.DryPlace.id");
                    return null;
                }
                this.id = known;
            }
            return known;
        }

        /** 4.1.1: the place started where vanilla would start it if the land there is dry, or moved a chunk at a time onto dry land near it. */
        Optional<Structure.GenerationStub> here(Structure.GenerationContext context) {
            ChunkPos home = context.f_226628_();
            DryLand.Ground ground = ground(context);
            for (int[] shift : SHIFTS) {
                Structure.GenerationContext moved = shift[0] == 0 && shift[1] == 0 ? context : new Structure.GenerationContext(context.f_226621_(),
                    context.f_226622_(), context.f_226623_(), context.f_226624_(), context.f_226625_(), context.f_226626_(), context.f_226627_(),
                    new ChunkPos(home.f_45578_ + shift[0], home.f_45579_ + shift[1]), context.f_226629_(), context.f_226630_());
                Optional<Structure.GenerationStub> stub = DryJigsaw.run(this.inner, moved);
                if (stub.isEmpty()) {
                    continue;
                }
                BoundingBox box = stub.get().m_226677_().m_192798_();
                int minX = box.m_162395_();
                int minZ = box.m_162398_();
                int maxX = box.m_162399_();
                int maxZ = box.m_162401_();
                if (!DryLand.reachable(home.m_45604_(), home.m_45605_(), minX, minZ, maxX, maxZ)) {
                    continue;
                }
                BlockPos at = stub.get().f_226669_();
                if (biome(context, at.m_123341_(), at.m_123342_(), at.m_123343_())
                    && DryLand.dry(ground, at.m_123342_(), minX, minZ, maxX - minX + 1, maxZ - minZ + 1)) {
                    return stub;
                }
            }
            return Optional.empty();
        }

        @Override
        public StructureType<?> m_213658_() {
            return DRY_PLACE;
        }

        /**
         * 4.1.9 (the owner: "it should be able to spawn literally in any biome"): the game's own test of the biome at the start (its
         * structure's biomes, a list: a mod's biome missing from it was no place at all) is left out; this place judges its own land (any
         * biome but a wet one: {@link ControlStructures#biome}, with the dry ground under and round it). Its structure's biomes
         * (#hexkagune:overworld) only keep it to the overworld (a set runs only where its biomes can be made). The world's making and
         * /locate both ask through here.
         */
        @Override
        public Optional<Structure.GenerationStub> m_262864_(Structure.GenerationContext context) {
            return this.m_214086_(context);
        }
    }
}
