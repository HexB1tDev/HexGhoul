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
        // 4.1.72 (the owner: "from now on the small city is gone, erased."): hexkagune:small_city and its placement hexkagune:city are gone
        // with it; every site is its own type again (4.1.72: hexkagune:site_hotel, the hotels beside some villages).
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

    /** 4.1.72: the hotels' site id (hexkagune:hotel, its structure set's too: {@link com.hex.kagune.control.village.HotelPlan}). */
    static final String HOTEL = "hotel";

    /**
     * 4.1.42 (the owner, of a city tower and the mansion hanging over hills: "How does this generation pass your bug test, bro?"): nothing of
     * ours ever floats. Under every piece's floor (its lowest block in its first layers, up to its ground's height: column by column, in this
     * chunk) the world's own ground is carried up: the first solid block under the gap copied up to the floor (stone under stone, dirt under
     * dirt, the riverbed under a river), at most {@link #FOUNDATION} deep. A column the floor already stands on is left as it is; one with no
     * floor in its first layers (a yard, the air beside a wall) is not filled.
     */
    static void foundation(WorldGenLevel level, BoundingBox chunk, PiecesContainer pieces) {
        for (StructurePiece piece : pieces.f_192741_()) {
            if (clearing(piece)) {
                continue; // 4.1.57: the site's clearing (air) has no floor to stand on: the apron fills round the place
            }
            BoundingBox b = piece.m_73547_();
            int x0 = Math.max(b.m_162395_(), chunk.m_162395_());
            int x1 = Math.min(b.m_162399_(), chunk.m_162399_());
            int z0 = Math.max(b.m_162398_(), chunk.m_162398_());
            int z1 = Math.min(b.m_162401_(), chunk.m_162401_());
            if (x0 > x1 || z0 > z1) {
                continue;
            }
            int bottom = b.m_162396_();
            int layers = piece instanceof PoolElementStructurePiece pool ? Math.max(1, pool.m_72647_()) : 1;
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    final int cx = x;
                    final int cz = z;
                    // Each height of the column: 2 a floor of ours or ground to stand on, 1 something there that is no ground (a plant), 0 air
                    // or water (DryLand.footing reads the floor and the ground under it).
                    int[] fill = DryLand.footing(y -> {
                        BlockPos at = new BlockPos(cx, y, cz);
                        BlockState state = level.m_8055_(at);
                        // 4.1.57: a tree's trunk or leaves under the floor are no ground: filled over as a gap is.
                        return state.m_60795_() || !state.m_60819_().m_76178_() || tree(state) ? DryLand.GAP
                            : holds(level, state, at) ? DryLand.SOLID : DryLand.SOFT;
                    }, bottom, layers, Math.max(LOWEST, bottom - FOUNDATION));
                    if (fill == null) {
                        continue;
                    }
                    BlockState ground = level.m_8055_(new BlockPos(x, fill[0], z));
                    for (int f = fill[0] + 1; f < fill[1]; f++) {
                        level.m_7731_(new BlockPos(x, f, z), ground, 2);
                    }
                }
            }
        }
    }

    /**
     * 4.1.57 (the owner: "CLEAR THE FUCKING AREA OF SPAWN, CLEAR ALL BLOCKS OR TREES, AND THEN PLACE IT DIRECTLY ON THE FLOOR, NOT FLYING, NOT
     * HOVERING OVER THE FLOOR, I SAID ON IT."): the clearing template, a cube of air {@link com.hex.kagune.control.village.SitePrep#CLEAR} on a
     * side (data/hexkagune/structures/clearing.nbt). Its tiles are laid over a site before the place's own pieces (a structure places its
     * pieces in each chunk in their order), so whatever the world had over the floor there, hill, trunk or leaves, is air before the place
     * is built; they are the game's own pieces (a single pool element), saved and loaded as any.
     */
    static final String CLEARING = "hexkagune:clearing";
    /** 4.1.57: the world's top (its highest block), which no clearing passes. */
    static final int CEILING = 319;
    /** 4.1.57: the deepest the apron round a place is filled down to its ground (a ravine deeper is left as it is). */
    static final int APRON_DEEP = 24;
    /** 4.1.57: the world's tree blocks (a trunk, leaves): never ground to stand on or to copy. */
    static final net.minecraft.tags.TagKey<?> LOGS = net.minecraft.tags.TagKey.m_203882_(Registries.f_256747_,
        net.minecraft.resources.ResourceLocation.m_214293_("minecraft", "logs"));
    static final net.minecraft.tags.TagKey<?> LEAVES = net.minecraft.tags.TagKey.m_203882_(Registries.f_256747_,
        net.minecraft.resources.ResourceLocation.m_214293_("minecraft", "leaves"));

    /** 4.1.57: true for a trunk or leaves. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static boolean tree(BlockState state) {
        return state.m_204336_((net.minecraft.tags.TagKey)LOGS) || state.m_204336_((net.minecraft.tags.TagKey)LEAVES);
    }

    /**
     * 4.1.57: true for a tile of a site's clearing: a single pool element laid unturned whose ground is at or under its foot (none over it;
     * a place's own pieces have one: a jigsaw's 1, a site's tiles their ground's), {@link com.hex.kagune.control.village.SitePrep#CLEAR} on
     * every side.
     */
    static boolean clearing(StructurePiece piece) {
        if (!(piece instanceof PoolElementStructurePiece pool) || pool.m_72647_() > 0) {
            return false;
        }
        BoundingBox b = piece.m_73547_();
        int size = com.hex.kagune.control.village.SitePrep.CLEAR;
        return b.m_162399_() - b.m_162395_() + 1 == size && b.m_162401_() - b.m_162398_() + 1 == size;
    }

    /**
     * 4.1.57: the clearing's tiles for a place whose footprint is {@code minX}..{@code maxX}, {@code minZ}..{@code maxZ} (and
     * {@link com.hex.kagune.control.village.SitePrep#MARGIN} round it, within its start chunk's reach from ({@code cornerX}, {@code cornerZ})),
     * from over its floor ({@code floor}: its ground block) to {@code top}, given to {@code add} (before the place's own pieces).
     */
    static void clearing(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager templates,
                         java.util.function.Consumer<StructurePiece> add, int minX, int minZ, int maxX, int maxZ, int cornerX, int cornerZ, int floor,
                         int top) {
        net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement element =
            net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement.m_210526_(CLEARING)
                .apply(net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection.RIGID);
        net.minecraft.world.level.block.Rotation none = net.minecraft.world.level.block.Rotation.NONE;
        for (com.hex.kagune.control.village.SitePrep.Tile tile : com.hex.kagune.control.village.SitePrep.clearing(minX, minZ, maxX, maxZ, cornerX,
            cornerZ, floor, top)) {
            BlockPos at = new BlockPos(tile.x(), tile.y(), tile.z());
            // Every tile's ground is the place's floor (a tile over the bank's top is told it stands that far down): the game's own shaping
            // of the land round a structure (its beard) then carves the site down to the floor and slopes it away, never building a rim of
            // land up to a tile's foot round the place.
            add.accept(new PoolElementStructurePiece(templates, element, at, com.hex.kagune.control.village.SitePrep.groundDelta(floor, tile.y()), none,
                element.m_214015_(templates, at, none)));
        }
    }

    /** 4.1.57: snow and what anything may grow or settle over (grass, flowers, vines, a snow layer): never ground to copy. */
    static final net.minecraft.tags.TagKey<?> SNOW = net.minecraft.tags.TagKey.m_203882_(Registries.f_256747_,
        net.minecraft.resources.ResourceLocation.m_214293_("minecraft", "snow"));
    static final net.minecraft.tags.TagKey<?> REPLACEABLE = net.minecraft.tags.TagKey.m_203882_(Registries.f_256747_,
        net.minecraft.resources.ResourceLocation.m_214293_("minecraft", "replaceable"));

    /** 4.1.57: true for the world's ground under a site: something solid to stand on that is no tree, no snow, nothing that grows over it. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static boolean ground(WorldGenLevel level, BlockState state, BlockPos at) {
        return !state.m_60795_() && state.m_60819_().m_76178_() && !tree(state) && !state.m_204336_((net.minecraft.tags.TagKey)SNOW)
            && !state.m_204336_((net.minecraft.tags.TagKey)REPLACEABLE) && holds(level, state, at);
    }

    /**
     * 4.1.57: the site graded round the place, in {@code chunk} (its clearing's columns, after the place itself): within
     * {@link com.hex.kagune.control.village.SitePrep#FLAT} of its footprint the ground flat at its floor, then a bank a block up or down a
     * block out to the land round it (cut into a hill, built up over a dip: never a cliff or a ledge), no tree left on any of it (under the
     * place itself, its foundation and the game's beard fill: a gap at its floor may be its own, a pool or a stairwell). The ground is the world's own,
     * the first under the gap copied up (stone under stone, grass under grass), a tree's trunk or leaves or snow never taken for it.
     */
    static void apron(WorldGenLevel level, BoundingBox chunk, PiecesContainer pieces) {
        int floor = Integer.MAX_VALUE;
        java.util.List<BoundingBox> clear = new java.util.ArrayList<>();
        java.util.List<BoundingBox> own = new java.util.ArrayList<>();
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (StructurePiece piece : pieces.f_192741_()) {
            BoundingBox b = piece.m_73547_();
            if (clearing(piece)) {
                clear.add(b);
                floor = Math.min(floor, b.m_162396_() - 1);
            } else {
                own.add(b);
                minX = Math.min(minX, b.m_162395_());
                minZ = Math.min(minZ, b.m_162398_());
                maxX = Math.max(maxX, b.m_162399_());
                maxZ = Math.max(maxZ, b.m_162401_());
            }
        }
        if (clear.isEmpty() || own.isEmpty()) {
            return;
        }
        BlockState air = Blocks.f_50016_.m_49966_();
        int bankTop = floor + com.hex.kagune.control.village.SitePrep.bank();
        for (int x = chunk.m_162395_(); x <= chunk.m_162399_(); x++) {
            for (int z = chunk.m_162398_(); z <= chunk.m_162401_(); z++) {
                if (!inside(clear, x, z)) {
                    continue;
                }
                int d;
                if (inside(own, x, z)) {
                    // Under the place itself only its foundation fills (and the game's beard under its foot): a gap at its floor may be its
                    // own (a pool, a stairwell), never to be filled.
                    continue;
                } else {
                    d = com.hex.kagune.control.village.SitePrep.distance(x, z, minX, minZ, maxX, maxZ);
                    int cut = com.hex.kagune.control.village.SitePrep.cut(floor, d);
                    int low = com.hex.kagune.control.village.SitePrep.fill(floor, d);
                    BlockState skin = null; // the ground's own top (grass on grassland) where the bank is cut through it
                    for (int y = bankTop; y > low; y--) {
                        BlockPos at = new BlockPos(x, y, z);
                        BlockState state = level.m_8055_(at);
                        if (state.m_60795_()) {
                            continue;
                        }
                        if (skin == null && ground(level, state, at)) {
                            skin = state;
                        }
                        if (y >= cut || tree(state)) {
                            level.m_7731_(at, air, 2);
                        }
                    }
                    BlockPos under = new BlockPos(x, cut - 1, z);
                    if (skin != null && ground(level, level.m_8055_(under), under)) {
                        level.m_7731_(under, skin, 2);
                    }
                }
                int top = com.hex.kagune.control.village.SitePrep.fill(floor, d);
                int deepest = floor - APRON_DEEP - 1;
                int found = Integer.MIN_VALUE;
                BlockState copy = null;
                for (int y = top; y >= deepest; y--) {
                    BlockPos at = new BlockPos(x, y, z);
                    BlockState state = level.m_8055_(at);
                    if (ground(level, state, at)) {
                        found = y;
                        copy = state;
                        break;
                    }
                }
                int[] fill = com.hex.kagune.control.village.SitePrep.apron(floor, d, found, APRON_DEEP);
                if (fill == null || copy == null) {
                    continue;
                }
                for (int y = fill[0]; y <= fill[1]; y++) {
                    level.m_7731_(new BlockPos(x, y, z), copy, 2);
                }
            }
        }
    }

    /** 4.1.57: true if column ({@code x}, {@code z}) is under any of {@code boxes}. */
    static boolean inside(java.util.List<BoundingBox> boxes, int x, int z) {
        for (BoundingBox b : boxes) {
            if (x >= b.m_162395_() && x <= b.m_162399_() && z >= b.m_162398_() && z <= b.m_162401_()) {
                return true;
            }
        }
        return false;
    }

    /** 4.1.42: the deepest a foundation goes (blocks), and the overworld's floor. */
    static final int FOUNDATION = 64;
    static final int LOWEST = -64;

    /** 4.1.42: true for ground a foundation stands on: not air, no water, not a plant (grass, flowers: nothing to stand on). */
    static boolean holds(WorldGenLevel level, BlockState state, BlockPos at) {
        if (state.m_60795_() || !state.m_60819_().m_76178_()) {
            return false;
        }
        float hard = state.m_60800_(level, at);
        return hard < 0.0F || hard > 0.05F;
    }

    /**
     * 4.1.72: the world's own village placement (the set minecraft:villages, as a pack may have changed it), or null if it is not a random
     * spread (another mod's way: then no hotels, rather than hotels beside nothing).
     */
    static net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement villages(Structure.GenerationContext context) {
        net.minecraft.world.level.levelgen.structure.StructureSet found = context.f_226621_().m_175515_(Registries.f_256998_)
            .m_7745_(net.minecraft.resources.ResourceLocation.m_214293_("minecraft", "villages"));
        return found != null && found.f_210004_() instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement spread
            ? spread : null;
    }

    /** 4.1.72: the village biome tags ({@link com.hex.kagune.control.village.HotelPlan#VILLAGE_TAGS}: vanilla's has_structure/village_*). */
    static final java.util.List<net.minecraft.tags.TagKey<?>> VILLAGE_BIOMES = java.util.Arrays.stream(com.hex.kagune.control.village.HotelPlan.VILLAGE_TAGS)
        .<net.minecraft.tags.TagKey<?>>map(tag -> net.minecraft.tags.TagKey.m_203882_(Registries.f_256952_,
            net.minecraft.resources.ResourceLocation.m_214293_("minecraft", "has_structure/" + tag))).toList();

    /** 4.1.72: true if a village really stands at its set's offer, chunk ({@code x}, {@code z}): its biome there is one a village may stand in. */
    static boolean village(Structure.GenerationContext context, int x, int z) {
        int bx = x << 4;
        int bz = z << 4;
        int y = ground(context).surface(bx, bz);
        net.minecraft.core.Holder<?> biome = context.f_226623_().m_203407_(bx >> 2, y >> 2, bz >> 2, context.f_226624_().m_224579_());
        for (net.minecraft.tags.TagKey<?> tag : VILLAGE_BIOMES) {
            if (biome.m_203656_(tag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 4.1.72: true if a hotel may stand within {@link com.hex.kagune.control.village.HotelPlan#MARGIN} chunks of start chunk ({@code x},
     * {@code z}) (a village offer one in four of which has one, on its side; judged by the offers alone, whether a village stands there or
     * not: it costs no biome, and keeps a one-of-a-kind place clear of every hotel). Every place asks before it takes a spot (as it asked
     * {@code inCity} before, the Small City's, gone with it).
     */
    static boolean inHotel(Structure.GenerationContext context, int x, int z) {
        net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement spread = villages(context);
        if (spread == null) {
            return false;
        }
        long seed = context.f_226627_();
        return com.hex.kagune.control.village.HotelPlan.near(seed, villageOffers(spread, seed), x, z);
    }

    /** 4.1.72: the world's village offers ({@link com.hex.kagune.control.village.HotelPlan.Offers}), as its placement makes them. */
    static com.hex.kagune.control.village.HotelPlan.Offers villageOffers(
            net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement spread, long seed) {
        return (cx, cz) -> {
            ChunkPos offer = spread.m_227008_(seed, cx, cz);
            return new int[] {offer.f_45578_, offer.f_45579_};
        };
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
            if (HOTEL.equals(this.site.id())) {
                return this.hotel(context, chunk); // 4.1.72
            }
            if ((CCG.equals(this.site.id()) || unique(this.site)) && onPurpose(context)) {
                // 4.1.6 (the owner: "I cant get the aogiri hideout to spawn or the ccg building at all"): /place structure builds the
                // world's one hideout or CCG building where it is asked (its footprint round this chunk, on the middle of its ground).
                // 4.1.43: only while the world has none yet (it is then the world's one): never a second.
                int minX = clampMin(chunk.m_45604_() + 8 - sizeX / 2, chunk.m_45604_(), sizeX);
                int minZ = clampMin(chunk.m_45605_() + 8 - sizeZ / 2, chunk.m_45605_(), sizeZ);
                return claimed(CCG.equals(this.site.id()) ? CCG : HIDEOUT, context, () -> Optional.of(this.stub(context, turns, minX, minZ, sizeX, sizeZ,
                    standing(ground(context), minX, minZ, sizeX, sizeZ))));
            }
            if (CCG.equals(this.site.id())) {
                // 4.1.4 (the owner: "CCG building ... like the others only 1 of them spawns"): the world's one. 4.1.14: at whichever of its
                // set's spots on dry land the world makes first (PlaceClaims), clear of the hideout's and the villages' spots.
                // 4.1.44 (the owner: "Im 99% sure the Aogiri place IS not spawning at all, same with the CCG building"): no longer only if the
                // game's spot is the one our own numbers expect (a mod that moves the spots made it never), and no longer only on the one
                // spot: the flattest dry spot within two chunks of it (the claim keeps it to one: onlyIfClaimed).
                return claimed(CCG, context, () -> {
                    if (DryLand.near(hideoutSpread(context), chunk.f_45578_, chunk.f_45579_, DryLand.CCG_APART) || inHotel(context, chunk.f_45578_, chunk.f_45579_)) {
                        return Optional.empty();
                    }
                    int span = Math.max(sizeX, sizeZ) + 2 * CCG_ROUND;
                    DryLand.Ground ground = ground(context);
                    int bestRough = Integer.MAX_VALUE;
                    int[] best = null;
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            int cx = chunk.m_45604_() + 8 + dx * 16;
                            int cz = chunk.m_45605_() + 8 + dz * 16;
                            int middle = dryGround(context, ground, cx - span / 2, cz - span / 2, span);
                            if (middle == NO_GROUND) {
                                continue;
                            }
                            int rough = roughness(ground, cx - span / 2, cz - span / 2, span, span);
                            if (rough < bestRough) {
                                bestRough = rough;
                                best = new int[] {cx, cz, middle};
                            }
                        }
                    }
                    return best == null ? Optional.empty()
                        : Optional.of(this.stub(context, turns, best[0] - sizeX / 2, best[1] - sizeZ / 2, sizeX, sizeZ, best[2]));
                });
            }
            if (unique(this.site)) {
                // 4.1.1 (the owner: "the mansion is currently happily resting at the very bottom of the ocean"): the world's one hideout,
                // only ever on dry land. 4.1.14: at whichever of its set's spots on dry land the world makes first (PlaceClaims).
                // 4.1.44: no longer only at the spot our own numbers expect, and judged as the Small City is (it is as big: 272 x 263): land
                // that rises and falls no more than HIDEOUT_FLAT, a stream or a pond under it allowed (its foundation fills them), never a sea,
                // a lake or a steep biome (DryLand.land). Before, one pond anywhere under it, or 10 blocks of rise (4.1.42), and no hideout.
                return claimed(HIDEOUT, context, () -> {
                    if (inHotel(context, chunk.f_45578_, chunk.f_45579_)) {
                        return Optional.empty();
                    }
                    int minX = clampMin(chunk.m_45604_() + 8 - sizeX / 2, chunk.m_45604_(), sizeX);
                    int minZ = clampMin(chunk.m_45605_() + 8 - sizeZ / 2, chunk.m_45605_(), sizeZ);
                    int middle = DryLand.land(ground(context), minX, minZ, sizeX, sizeZ, 9, HIDEOUT_FLAT, DryLand.DEEP);
                    if (middle == Integer.MIN_VALUE || !biome(context, minX + sizeX / 2, middle, minZ + sizeZ / 2)) {
                        return Optional.empty();
                    }
                    return Optional.of(this.stub(context, turns, minX, minZ, sizeX, sizeZ, middle));
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
         * 4.1.72 (the owner: "From now on close to a village will be an hotel where humans work, live and walk around doing their own thing.
         * / CCG patrols around the hotel at all times."; they chose "Beside some villages"): a hotel at this start chunk if it is the one
         * beside a village: the world's village offer {@link com.hex.kagune.control.village.HotelPlan#DISTANCE} chunks back on one of the
         * four sides whose hotel stands here ({@link com.hex.kagune.control.village.HotelPlan#hotelOf}: one village in four, on the first side
         * clear of every other village, never overlapping another hotel), a village really there (its biome one a village may stand in),
         * clear of a one-of-a-kind place an older world already holds, on land as the hideout judges its own (dry, fairly flat, no sea, no
         * deep water), its forecourt turned to the village. /place structure builds one where it is asked.
         */
        private Optional<Structure.GenerationStub> hotel(Structure.GenerationContext context, ChunkPos chunk) {
            if (onPurpose(context)) {
                int turns = context.f_226626_().m_188503_(4);
                return this.hotelAt(context, chunk, turns, true);
            }
            net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement spread = villages(context);
            if (spread == null) {
                return Optional.empty();
            }
            long seed = context.f_226627_();
            com.hex.kagune.control.village.HotelPlan.Offers offers = villageOffers(spread, seed);
            for (int side = 0; side < 4; side++) {
                int[] at = com.hex.kagune.control.village.HotelPlan.village(chunk.f_45578_, chunk.f_45579_, side);
                int[] offer = offers.at(at[0], at[1]);
                if (offer[0] != at[0] || offer[1] != at[1]) {
                    continue;
                }
                int[] hotel = com.hex.kagune.control.village.HotelPlan.hotelOf(seed, offers, at[0], at[1]);
                if (hotel == null || hotel[0] != chunk.f_45578_ || hotel[1] != chunk.f_45579_) {
                    continue;
                }
                if (!village(context, at[0], at[1]) || holdsPlace(chunk)) {
                    return Optional.empty();
                }
                return this.hotelAt(context, chunk, com.hex.kagune.control.village.HotelPlan.turns(hotel[2]), false);
            }
            return Optional.empty();
        }

        /** 4.1.72: the hotel, turned {@code turns}, its footprint round start chunk {@code chunk}, on its land's middle (or not built). */
        private Optional<Structure.GenerationStub> hotelAt(Structure.GenerationContext context, ChunkPos chunk, int turns, boolean asked) {
            boolean across = (turns & 1) == 1;
            int sizeX = across ? this.site.depth() : this.site.width();
            int sizeZ = across ? this.site.width() : this.site.depth();
            int minX = clampMin(chunk.m_45604_() + 8 - sizeX / 2, chunk.m_45604_(), sizeX);
            int minZ = clampMin(chunk.m_45605_() + 8 - sizeZ / 2, chunk.m_45605_(), sizeZ);
            DryLand.Ground ground = ground(context);
            int middle = DryLand.land(ground, minX, minZ, sizeX, sizeZ, 9, HIDEOUT_FLAT, DryLand.DEEP);
            if (asked && middle == Integer.MIN_VALUE) {
                middle = standing(ground, minX, minZ, sizeX, sizeZ); // asked for: on whatever ground is there
            }
            if (middle == Integer.MIN_VALUE || !asked && !biome(context, minX + sizeX / 2, middle, minZ + sizeZ / 2)) {
                return Optional.empty();
            }
            // 4.1.72 (the owner: "Only spawns on plains or biomes o plenty equivallent biomes, we cant have that shit spawning on
            // mountains"): every column of its footprint and round it a plains biome (hexkagune:plains), unless /place asked for it.
            int y = middle;
            if (!asked && !com.hex.kagune.control.village.HotelPlan.onPlains(minX, minZ, sizeX, sizeZ, (x, z) -> plains(context, x, y, z))) {
                return Optional.empty();
            }
            return Optional.of(this.stub(context, turns, minX, minZ, sizeX, sizeZ, middle));
        }

        /** 4.1.72: true if a one-of-a-kind place an older world already took stands where a hotel started at {@code chunk} would reach. */
        static boolean holdsPlace(ChunkPos chunk) {
            for (String id : ONE_OF_A_KIND) {
                int[] at = PlaceClaims.claimed(id);
                if (at != null && com.hex.kagune.control.village.HotelPlan.covers(chunk.f_45578_, chunk.f_45579_, at[0], at[1], 2)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * The place laid with its footprint's least corner at ({@code minX}, {@code minZ}), turned {@code turns}, its ground on the world's
         * at {@code middle} (the middle of the dry ground's heights: the first free height, its ground block one under it).
         */
        Structure.GenerationStub stub(Structure.GenerationContext context, int turns, int minX, int minZ, int sizeX, int sizeZ, int middle) {
            int base = middle - 1 - this.site.ground();
            net.minecraft.world.level.block.Rotation rotation = TURNS[turns];
            // The template's corner (its x 0, z 0) as the turn takes it: the turned footprint's least corner is (minX, minZ).
            int[] least = com.hex.kagune.control.village.SiteMaps.turnedLeast(this.site.width(), this.site.depth(), turns);
            int ox = minX - least[0];
            int oz = minZ - least[1];
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager templates = context.f_226625_();
            com.hex.kagune.control.village.SiteMaps.Site site = this.site;
            BlockPos start = new BlockPos(minX + sizeX / 2, base + site.ground() + 1, minZ + sizeZ / 2);
            // 4.1.57: its site cleared first, from over its ground block (one under its middle) up past the highest ground's trees.
            int margin = com.hex.kagune.control.village.SitePrep.MARGIN;
            int top = com.hex.kagune.control.village.SitePrep.top(com.hex.kagune.control.village.SitePrep.highest(ground(context), minX - margin,
                minZ - margin, minX + sizeX - 1 + margin, minZ + sizeZ - 1 + margin), Math.min(CEILING, base + site.height()), CEILING);
            ChunkPos home = context.f_226628_();
            return new Structure.GenerationStub(start, builder -> {
                clearing(templates, piece -> builder.m_142679_(piece), minX, minZ, minX + sizeX - 1, minZ + sizeZ - 1, home.m_45604_(),
                    home.m_45605_(), middle - 1, top);
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
            // 4.1.42: no more than its own flatness over its whole footprint, sampled every few blocks (a 5 x 5 grid let 24 blocks of
            // hillside through, and it hung over the low side).
            if (floor == null || DryLand.spread(floor) > STEEPEST || roughness(ground, minX, minZ, size, size) > flatFor(size)) {
                return NO_GROUND;
            }
            int middle = DryLand.median(floor);
            return ControlStructures.biome(context, minX + size / 2, middle, minZ + size / 2) && DryLand.dry(ground, middle, minX, minZ, size, size)
                ? middle : NO_GROUND;
        }

        /** 4.1.14: the steepest ground (blocks between its lowest and highest of a 5 x 5 grid) a one-of-a-kind place is built on. */
        static final int STEEPEST = BIG_PLACE_FLAT;

        /** 4.1.42: the most a place {@code size} across may rise and fall: a small place's {@link #PLACE_FLAT}, a big one's (the hideout) more. */
        static int flatFor(int size) {
            return size <= 80 ? PLACE_FLAT : BIG_PLACE_FLAT;
        }

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
            // 4.1.42 (the owner: "The structures are spawning multiple times. There's tons of them spawning like hundreds of times in the
            // fucking world. And they're all fucking flying"): the biome test alone was the tell, and a mod that rewrites the structures'
            // biome test (one that takes anything, as Structure Essentials' can) made every chunk the world generated look like /place: each
            // region built its own, unclaimed, lifted onto its highest ground with no other check. /place asks on the live world (its
            // heights are the level's); the world's own making never does (its heights are the chunk being made), whatever any mod does.
            if (!(context.f_226629_() instanceof net.minecraft.world.level.Level)) {
                return false;
            }
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

        /** 4.1.42: the place (the hideout, the CCG building) on a foundation of the world's own ground, never over air. */
        @Override
        public void m_214110_(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
                              ChunkPos chunk, PiecesContainer pieces) {
            try {
                foundation(level, box, pieces);
                apron(level, box, pieces);
            } catch (RuntimeException | LinkageError failure) {
                Safety.caught(failure, "ControlStructures.Site.foundation");
            }
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
    static final java.util.List<String> ONE_OF_A_KIND = java.util.List.of("aogiri_hideout", "ccg_building", "anteiku", "library", "reaper_mansion",
        "circus", "gas_hideout"); // 4.1.68: Ken's tower gone (he lives on Anteiku's roof)

    /** 4.1.14: each one-of-a-kind place's name, as /kagune places says it. */
    static String named(String id) {
        return switch (id) {
            case "aogiri_hideout" -> "Aogiri Tree's hideout";
            case "ccg_building" -> "the CCG building";
            case "anteiku" -> "Anteiku";
            case "reaper_mansion" -> "the White Reaper's mansion";
            case "circus" -> "the Clowns' circus tent";
            case "gas_hideout" -> "the Gas Masks' hideout";
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

    /** 4.1.17: true only if hexkagune:{@code id}'s start is really in {@code chunk} of {@code level} (false if it is no structure of this world). */
    static boolean holds(net.minecraft.world.level.LevelReader level, net.minecraft.world.level.chunk.ChunkAccess chunk, String id) {
        Structure structure = level.m_9598_().m_175515_(Registries.f_256944_).m_7745_(net.minecraft.resources.ResourceLocation.m_214293_("hexkagune", id));
        if (structure == null) {
            return false;
        }
        net.minecraft.world.level.levelgen.structure.StructureStart found = chunk.m_213652_(structure);
        return found != null && found.m_73603_();
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
     * 4.1.82: the middle of the one-of-a-kind place {@code id} as this world built it ({x, its lowest y, z}: its start's box round all its
     * pieces, from its claimed start chunk), or null if it isn't built, or its chunk isn't loaded (nothing is loaded for this).
     */
    public static int[] middle(net.minecraft.server.level.ServerLevel level, String id) {
        net.minecraft.world.level.levelgen.structure.StructureStart found = start(level, id);
        if (found == null) {
            return null;
        }
        BoundingBox box = found.m_73601_();
        return com.hex.kagune.control.npc.HomePlace.middle(box.m_162395_(), box.m_162399_(), box.m_162398_(), box.m_162401_(), box.m_162396_());
    }

    /**
     * 4.1.82: the one-of-a-kind place {@code id} as this world built it (its start, kept with its claimed start chunk), or null if it isn't
     * built, or its chunk isn't loaded (nothing is loaded for this).
     */
    static net.minecraft.world.level.levelgen.structure.StructureStart start(net.minecraft.server.level.ServerLevel level, String id) {
        int[] chunk = PlaceClaims.claimed(id);
        if (chunk == null || !level.m_46805_(new BlockPos((chunk[0] << 4) + 8, 64, (chunk[1] << 4) + 8))) {
            return null;
        }
        Structure structure = level.m_9598_().m_175515_(Registries.f_256944_).m_7745_(net.minecraft.resources.ResourceLocation.m_214293_("hexkagune", id));
        if (structure == null) {
            return null;
        }
        net.minecraft.world.level.levelgen.structure.StructureStart found = level.m_6325_(chunk[0], chunk[1]).m_213652_(structure);
        return found == null || !found.m_73603_() ? null : found;
    }

    /**
     * 4.1.83 (the owner: "Gas mask guy is ALSO not spawning in his room, or anywhere"; "and I'm 99% sure the general of the CCg isnt spawning
     * either"): official {@code key}'s own spot in the world ({x, feet y, z, yaw}: npc.HomePlace#spot where her place stands, as this world
     * built it), or null if her place isn't built or its start chunk isn't loaded. Read from the place's own first piece (past its
     * clearing's: a jigsaw place's start, a site's first tile, both laid with the template's corner at their position, turned as they are),
     * so a place built by any version has it.
     */
    public static int[] spot(net.minecraft.server.level.ServerLevel level, String key) {
        int[] local = com.hex.kagune.control.npc.HomePlace.spot(key);
        net.minecraft.world.level.levelgen.structure.StructureStart found = local == null ? null : start(level, com.hex.kagune.control.npc.HomePlace.of(key));
        if (found == null) {
            return null;
        }
        for (StructurePiece piece : found.m_73602_()) {
            if (piece instanceof PoolElementStructurePiece pool && !clearing(piece)) {
                BlockPos at = pool.m_72646_();
                int turns = ((Enum<?>)(Object)pool.m_6830_()).ordinal(); // NONE, CLOCKWISE_90, CLOCKWISE_180, COUNTERCLOCKWISE_90
                return com.hex.kagune.control.npc.HomePlace.placed(local, at.m_123341_(), at.m_123342_(), at.m_123343_(), turns);
            }
        }
        return null;
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
        if (taken == null && PlaceClaims.crowded(id, chunk.f_45578_, chunk.f_45579_)) {
            return Optional.empty(); // 4.1.42: another place is too near: not here (nor does /locate point here)
        }
        Optional<Structure.GenerationStub> stub = here.get();
        return stub.isEmpty() ? stub : Optional.of(onlyIfClaimed(id, chunk, stub.get()));
    }

    /**
     * 4.1.43 (the owner, of 4.1.42 in a new world: "Structures are still spawning doubles, they're not unique"; "and they're also spawning
     * doubled NPCS. eto spawned 2 of them"): the spot taken as the place is really built: when its pieces are asked for, which the world's
     * making (and /place) always does and /locate never does, however the making is driven or wrapped by any mod. Before, the claim was taken
     * only when the asker looked like the world's making (its heights not the level's) and the place was built either way: a mod making
     * chunks through the level built one at every spot, each with its people (Eto in every library). Now a spot that cannot take the
     * place (another holds it, or another place is too near) builds nothing: no pieces, no start.
     */
    static Structure.GenerationStub onlyIfClaimed(String id, ChunkPos chunk, Structure.GenerationStub stub) {
        int x = chunk.f_45578_;
        int z = chunk.f_45579_;
        return new Structure.GenerationStub(stub.f_226669_(), builder -> {
            if (!PlaceClaims.claim(id, x, z)) {
                return; // not this world's one: nothing of it here
            }
            for (StructurePiece piece : stub.m_226677_().m_192780_().f_192741_()) {
                builder.m_142679_(piece);
            }
        });
    }

    /** 4.1.14: true if this start chunk is one of {@code spread}'s spots (the game asks only there; a stray ask is turned away). */
    static boolean offered(Structure.GenerationContext context, DryLand.Spread spread) {
        ChunkPos chunk = context.f_226628_();
        int[] offer = spread.chunk(chunk.f_45578_, chunk.f_45579_);
        return offer[0] == chunk.f_45578_ && offer[1] == chunk.f_45579_;
    }

    /**
     * 4.1.42: how far the ground rises and falls (blocks) over a footprint, its highest less its lowest, sampled every {@link #ROUGH_STEP}
     * blocks and along its far edges (the water's surface counted as ground).
     */
    static int roughness(DryLand.Ground ground, int minX, int minZ, int sizeX, int sizeZ) {
        return DryLand.roughness(ground, minX, minZ, sizeX, sizeZ, ROUGH_STEP);
    }

    /** 4.1.42: the ground's samples (blocks apart); the most a place's ground may rise and fall, a big place's; as flat as land comes. */
    static final int ROUGH_STEP = 6;
    static final int PLACE_FLAT = 6;
    static final int BIG_PLACE_FLAT = 10;
    static final int FLAT_ENOUGH = 2;
    /** 4.1.44: the most the hideout's ground (272 x 263) may rise and fall: as the Small City's, and its foundation and terrain fit meet the rest. */
    static final int HIDEOUT_FLAT = 14;

    /**
     * 4.1.42 (the owner: "research the ... biomes of plenty mods in the basic Minecraft mods and separate them ... instead just spawns on a
     * fucking mountain"): the steep biomes no place of ours stands in (vanilla's mountains and hills, Forge's mountain, peak, slope and
     * plateau tags that biome mods fill, and the steep biomes of Biomes O' Plenty and Regions Unexplored by name, each only if it is there):
     * data/hexkagune/tags/worldgen/biome/steep.json. The flatness tests stand behind it whatever biome a mod adds.
     */
    static final net.minecraft.tags.TagKey<?> STEEP = net.minecraft.tags.TagKey.m_203882_(Registries.f_256952_,
        net.minecraft.resources.ResourceLocation.m_214293_("hexkagune", "steep"));

    /**
     * 4.1.72 (the owner: "Only spawns on plains or biomes o plenty equivallent biomes, we cant have that shit spawning on mountains"): the
     * biomes a hotel stands in (vanilla's plains, sunflower plains and snowy plains; Biomes O' Plenty's field, grassland, pasture, prairie
     * and lavender field, each only if it is there): data/hexkagune/tags/worldgen/biome/plains.json. Forge's forge:is_plains is not used:
     * it holds the meadow, and Biomes O' Plenty puts its highland and rocky shrubland in it.
     */
    static final net.minecraft.tags.TagKey<?> PLAINS = net.minecraft.tags.TagKey.m_203882_(Registries.f_256952_,
        net.minecraft.resources.ResourceLocation.m_214293_("hexkagune", "plains"));

    /** 4.1.72: true if the biome at ({@code x}, {@code y}, {@code z}) is one a hotel may stand in ({@link #PLAINS}). */
    static boolean plains(Structure.GenerationContext context, int x, int y, int z) {
        return context.f_226623_().m_203407_(x >> 2, y >> 2, z >> 2, context.f_226624_().m_224579_()).m_203656_(PLAINS);
    }

    /** 4.1.1: the biomes no structure may stand in or beside (a sea, a river, a beach, a swamp): data/hexkagune/tags/worldgen/biome/wet.json. */
    static final net.minecraft.tags.TagKey<?> WET = net.minecraft.tags.TagKey.m_203882_(Registries.f_256952_,
        net.minecraft.resources.ResourceLocation.m_214293_("hexkagune", "wet"));
    /** 4.1.36: the seas (minecraft:is_ocean, every ocean, deep or not): the Small City stands over a river, never a sea. */
    static final net.minecraft.tags.TagKey<?> SEA = net.minecraft.tags.TagKey.m_203882_(Registries.f_256952_,
        net.minecraft.resources.ResourceLocation.m_214293_("minecraft", "is_ocean"));

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
            public boolean sea(int x, int y, int z) {
                return biomes.m_203407_(x >> 2, y >> 2, z >> 2, sampler).m_203656_(SEA);
            }

            @Override
            public boolean steep(int x, int y, int z) {
                return biomes.m_203407_(x >> 2, y >> 2, z >> 2, sampler).m_203656_(STEEP);
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

    /** 4.1.9: the hideout's spread as this world has it (4.1.36: the villages' is gone: {@link #cityMiddles}). */
    static DryLand.Spread hideoutSpread(Structure.GenerationContext context) {
        return spread(context, "aogiri_hideout", DryLand.fixed(context.f_226627_(), DryLand.SPACING, DryLand.SEPARATION, DryLand.SALT));
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
        return !biome.m_203656_(WET) && !biome.m_203656_(STEEP); // 4.1.42: and never a steep one
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
        /** 4.1.42: the nearest spots the flattest is looked for among (the first 5 x 5: two chunks either way). */
        static final int STRICT_SHIFTS = 25;
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
            String id = this.id(context);
            if (id == null) {
                return Optional.empty(); // its name could not be read: never one of many where the world's one belongs (asked again later)
            }
            if (DryLand.placeSalt(id) < 0) {
                return this.here(context); // a jigsaw place of someone else's on this type: wherever its own set offers it
            }
            if (Site.onPurpose(context)) {
                // /place: where it is asked, but (4.1.43) only while the world has none yet: it is then the world's one, never a second.
                return claimed(id, context, () -> this.here(context));
            }
            return claimed(id, context, () -> {
                // 4.1.44: wherever the game asks (no longer only if its spot is the one our own numbers expect: a mod that moves the spots made
                // it never); the claim keeps it to one (onlyIfClaimed).
                Optional<Structure.GenerationStub> stub = this.flattest(context);
                if (stub.isEmpty()) {
                    return stub;
                }
                BoundingBox box = stub.get().m_226677_().m_192798_();
                int x = Math.floorDiv((box.m_162395_() + box.m_162399_()) >> 1, 16);
                int z = Math.floorDiv((box.m_162398_() + box.m_162401_()) >> 1, 16);
                if (inHotel(context, x, z) || DryLand.near(hideoutSpread(context), x, z, DryLand.PLACE_APART)
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

        /**
         * 4.1.42 (the owner: "I see so many flat areas they could have a structure and instead just spawns on a fucking mountain"): of every
         * spot its start reaches ({@link #SHIFTS}) that is dry and in a biome it may stand in (no mountain, hill, peak or slope:
         * {@link ControlStructures#STEEP}), the flattest ({@link ControlStructures#roughness}); none at all if even that one's ground rises
         * and falls more than {@link ControlStructures#PLACE_FLAT}. Lifted onto its highest ground (never buried) on a foundation (never over
         * air: {@link ControlStructures#foundation}).
         */
        Optional<Structure.GenerationStub> flattest(Structure.GenerationContext context) {
            return this.spot(context, true);
        }

        /** 4.1.1: the place started where vanilla would start it if the land there is dry, or moved a chunk at a time onto dry land near it. */
        Optional<Structure.GenerationStub> here(Structure.GenerationContext context) {
            return this.spot(context, false);
        }

        /** {@code strict}: the flattest spot, if flat enough ({@link #flattest}); else the first that will do (/place: {@link #here}). */
        private Optional<Structure.GenerationStub> spot(Structure.GenerationContext context, boolean strict) {
            ChunkPos home = context.f_226628_();
            DryLand.Ground ground = ground(context);
            Structure.GenerationStub best = null;
            int bestRough = Integer.MAX_VALUE;
            int[] bestAt = null;
            int tried = 0;
            for (int[] shift : SHIFTS) {
                if (strict && ++tried > STRICT_SHIFTS) {
                    break; // 4.1.42: the nearest spots only, so a region with no flat land costs the world's making little
                }
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
                    int rough = strict ? roughness(ground, minX, minZ, maxX - minX + 1, maxZ - minZ + 1) : 0;
                    if (rough < bestRough) {
                        bestRough = rough;
                        best = stub.get();
                        bestAt = new int[] {at.m_123341_(), at.m_123343_(), minX, minZ, maxX, maxZ};
                    }
                    if (rough <= FLAT_ENOUGH) {
                        break; // as flat as land comes: no need to look further
                    }
                }
            }
            if (best == null || bestRough > PLACE_FLAT) {
                return Optional.empty();
            }
            // 4.1.57 (the owner: "PLACE IT DIRECTLY ON THE FLOOR, NOT FLYING, NOT HOVERING OVER THE FLOOR"): on its footprint's own floor (the
            // middle of its ground), no longer lifted onto its highest ground (4.1.17), which left it on a plinth over its low side; its site
            // cleared over that floor first (the high side cut down to it, trees and all) and its apron filled up to it.
            int margin = com.hex.kagune.control.village.SitePrep.MARGIN;
            int floor = com.hex.kagune.control.village.SitePrep.floor(ground, bestAt[2], bestAt[3], bestAt[4], bestAt[5]);
            int highest = com.hex.kagune.control.village.SitePrep.highest(ground, bestAt[2] - margin, bestAt[3] - margin, bestAt[4] + margin,
                bestAt[5] + margin);
            return Optional.of(settled(best, floor - ground.surface(bestAt[0], bestAt[1]), context.f_226625_(), bestAt[2], bestAt[3], bestAt[4],
                bestAt[5], home.m_45604_(), home.m_45605_(), floor - 1, com.hex.kagune.control.village.SitePrep.top(highest, Integer.MIN_VALUE, CEILING)));
        }

        /**
         * 4.1.57: {@code stub} moved {@code shift} blocks (up or down) onto its floor, and its site's clearing laid before its own pieces (its
         * footprint {@code minX}..{@code maxX}, {@code minZ}..{@code maxZ}, its ground block {@code floor}, cleared up to {@code top}, within its
         * start chunk's reach from ({@code cornerX}, {@code cornerZ})). A jigsaw's stub makes its pieces afresh each time they are asked for,
         * so the settled one makes them and moves them every time. (4.1.17 lifted it onto its highest ground instead.)
         */
        static Structure.GenerationStub settled(Structure.GenerationStub stub, int shift,
                                                net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager templates, int minX,
                                                int minZ, int maxX, int maxZ, int cornerX, int cornerZ, int floor, int top) {
            BlockPos at = stub.f_226669_();
            return new Structure.GenerationStub(new BlockPos(at.m_123341_(), at.m_123342_() + shift, at.m_123343_()), builder -> {
                net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder made = stub.m_226677_();
                if (shift != 0) {
                    made.m_192781_(shift);
                }
                clearing(templates, piece -> builder.m_142679_(piece), minX, minZ, maxX, maxZ, cornerX, cornerZ, floor, top);
                for (StructurePiece piece : made.m_192780_().f_192741_()) {
                    builder.m_142679_(piece);
                }
            });
        }

        @Override
        public StructureType<?> m_213658_() {
            return DRY_PLACE;
        }

        /** 4.1.42: the place on a foundation of the world's own ground, never over air ({@link ControlStructures#foundation}). */
        @Override
        public void m_214110_(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
                              ChunkPos chunk, PiecesContainer pieces) {
            try {
                foundation(level, box, pieces);
                apron(level, box, pieces);
            } catch (RuntimeException | LinkageError failure) {
                Safety.caught(failure, "ControlStructures.DryPlace.foundation");
            }
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
