package com.hex.kagune.control.npc.scene;

import java.util.Map;
import java.util.Set;

/**
 * 1.3.29: what a scene is picked and filled by (pure): who the player is to the speaker ({@code tier}: a companion's EtoVoice, a child's
 * age), its mood, a child's temperament ({@code temper}, "" for a companion), the moments that hold now ({@code flags}: the hour, the
 * weather, their family...), the names its lines may hold, and a seed for which of its words.
 */
public record SceneContext(String tier, String mood, String temper, Set<String> flags, Map<String, String> names, int seed) {
    public boolean has(String flag) {
        return this.flags.contains(flag);
    }

    /** The same with another seed. */
    public SceneContext reseed(int seed) {
        return new SceneContext(this.tier, this.mood, this.temper, this.flags, this.names, seed);
    }
}
