package com.hex.kagune.control.server;

import com.hex.kagune.control.family.Genome;

/** 1.3.10: a newborn's name, picked from its seed: soft Japanese names, as fits a child of the ward. */
final class ChildNames {
    private static final String[] GIRLS = {"Hana", "Yui", "Mio", "Rin", "Sora", "Aoi", "Mei", "Kaede", "Nanami", "Hinata", "Akari", "Chiyo", "Fumiko", "Haru",
        "Koharu", "Mizuki", "Natsu", "Sakura", "Tsubaki", "Yuzuki"};
    private static final String[] BOYS = {"Haruto", "Ren", "Sota", "Yuki", "Kaito", "Riku", "Daichi", "Hikaru", "Itsuki", "Kenta", "Minato", "Nao", "Ryo",
        "Shin", "Taiga", "Takumi", "Yamato", "Yuto", "Akio", "Hiro"};

    private ChildNames() {
    }

    static String pick(Genome genome) {
        String[] names = genome.gender().female() ? GIRLS : BOYS;
        return names[(int)Math.floorMod(genome.seed() >>> 7, (long)names.length)];
    }
}
