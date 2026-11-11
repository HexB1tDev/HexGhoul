package com.hex.kagune.control;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 4.1.14 (the owner: "Fix structures still not spawning at all or able to be located (still not spawning)"): where each one-of-a-kind place
 * (the hideout, the CCG building, Anteiku, the library, Ken's tower) was built in this world, kept with the world.
 *
 * <p>4.1.1 to 4.1.13 worked out ONE start chunk a world for each and built it nowhere else, so whatever stopped it there (another mod's
 * structure rules: Structure Essentials, in the owner's pack, can keep every structure a set distance from the others) left the world
 * without it, and /locate found nothing. Now every spot its set offers on dry land may hold it until one is built: the first the world
 * makes takes it ({@link #claim}, kept here), every other spot is turned away from then on. A spot something stops is never taken, so the
 * next is built (and /locate goes on to it). If a taken spot's chunk turns out to hold no start after all (stopped after it was taken), it is
 * given up when that chunk loads ({@link #chunk}) and the next spot may take it.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class PlaceClaims {
    static final String NAME = "hexkagune_places";

    /** The world being made's claims (its overworld's saved data), or null while there is none (between worlds, on a client). */
    private static volatile Data current;

    private PlaceClaims() {
    }

    static final class Data extends SavedData {
        final com.hex.kagune.control.village.TakenPlaces taken;

        Data(com.hex.kagune.control.village.TakenPlaces taken) {
            this.taken = taken;
        }

        static Data load(CompoundTag tag) {
            return new Data(com.hex.kagune.control.village.TakenPlaces.read(tag.m_128461_("claims")));
        }

        @Override
        public CompoundTag m_7176_(CompoundTag tag) {
            tag.m_128359_("claims", this.taken.write());
            return tag;
        }
    }

    @SubscribeEvent
    public static void load(LevelEvent.Load event) {
        try {
            // Posted as each level is made, before any of its ground is (the spawn's is made after): the overworld's claims are ready first.
            if (event.getLevel() instanceof ServerLevel level && level.m_46472_() == Level.f_46428_) {
                current = level.m_8895_().m_164861_(Data::load, () -> new Data(new com.hex.kagune.control.village.TakenPlaces()), NAME);
            }
        } catch (RuntimeException | LinkageError failure) {
            Safety.caught(failure, "PlaceClaims.load");
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        current = null;
    }

    /**
     * A taken spot's chunk loaded in full with no start of its place in it (something stopped it after the spot was taken): the spot is
     * given up, and the next spot the world makes may take it.
     */
    @SubscribeEvent
    public static void chunk(ChunkEvent.Load event) {
        try {
            Data data = current;
            if (data == null || !(event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk)
                || !(event.getLevel() instanceof ServerLevel level) || level.m_46472_() != Level.f_46428_) {
                return;
            }
            net.minecraft.world.level.ChunkPos at = chunk.m_7697_();
            String id = data.taken.whose(at.f_45578_, at.f_45579_);
            if (id != null && !ControlStructures.standsIn(level, chunk, id)) {
                release(id, at.f_45578_, at.f_45579_);
                com.hex.kagune.KaguneMod.LOGGER.info("HexKagune: " + id + " was stopped at chunk " + at.f_45578_ + ", " + at.f_45579_
                    + " (another mod's structure rules?); the next spot the world makes will hold it.");
            }
        } catch (RuntimeException | LinkageError failure) {
            Safety.caught(failure, "PlaceClaims.chunk");
        }
    }

    /**
     * The world being made's claims: as its overworld loaded them; or, if its ground is being made before that (a new world's spawn is found
     * as its levels are made), read from the running server's overworld now; null if there is no server's overworld to keep them on.
     */
    static Data data() {
        Data data = current;
        if (data != null) {
            return data;
        }
        synchronized (PlaceClaims.class) {
            if (current != null) {
                return current;
            }
            try {
                net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    for (ServerLevel level : server.m_129785_()) {
                        if (level.m_46472_() == Level.f_46428_) {
                            current = level.m_8895_().m_164861_(Data::load, () -> new Data(new com.hex.kagune.control.village.TakenPlaces()), NAME);
                            break;
                        }
                    }
                }
            } catch (RuntimeException | LinkageError failure) {
                Safety.caught(failure, "PlaceClaims.data");
            }
            return current;
        }
    }

    /** {@code id}'s taken start chunk {x, z} in the world being made, or null if none is taken yet (or there is no world). */
    public static int[] claimed(String id) {
        Data data = data();
        return data == null ? null : data.taken.at(id);
    }

    /**
     * {@code id} built at start chunk ({@code x}, {@code z}) as the world makes it: true if that is its spot (just taken, or taken before);
     * false if another spot already holds it, or if there is nowhere to remember it (never a second one: the next spot made asks again).
     */
    public static boolean claim(String id, int x, int z) {
        Data data = data();
        if (data == null) {
            return false;
        }
        int[] before = data.taken.at(id);
        boolean mine = data.taken.take(id, x, z);
        if (before == null && mine) {
            data.m_77762_();
            com.hex.kagune.KaguneMod.LOGGER.info("HexKagune: " + id + " is built at x " + ((x << 4) + 8) + ", z " + ((z << 4) + 8)
                + " (/locate structure hexkagune:" + id + ").");
        }
        return mine;
    }

    /** {@code id}'s spot ({@code x}, {@code z}) given up (nothing of it was built there). */
    public static void release(String id, int x, int z) {
        Data data = data();
        if (data != null && data.taken.release(id, x, z)) {
            data.m_77762_();
        }
    }
}
