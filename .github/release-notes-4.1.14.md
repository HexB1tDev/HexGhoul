## 4.1.14 (HexGhoul): the places are built wherever the world can hold them, coats attached to the legs, no Ukaku M1 spam

HexKagune 4.1.14 for Forge 1.20.1. Not tested in game. Coat previews: `docs/handoff/coat-crimson-4.1.14.png` and `docs/handoff/coat-formal-4.1.14.png`.

### The structures

Your words: "Fix structures still not spawning at all or able to be located (still not spawning)"

**What was wrong:** the hideout, the CCG building, Ken's tower, Anteiku and the library were each allowed in **one chunk per world only**, worked out in advance. If anything stopped the place being built in that one chunk, the world never had it, and `/locate` found nothing. That includes another mod's structure rules: Structure Essentials, in your pack, can keep structures a set distance apart.

**Now:**
- **Any good spot works:** every spot a place's structure set offers on dry land can hold it, like any normal structure. `/locate` checks them nearest first.
- **Still one of each:** the first spot the world actually builds takes the place, and from then on every other spot is refused. It's saved with the world, so restarting never builds a second one.
- **A blocked spot doesn't matter:**
  - If another mod stops a spot, nothing is taken there, and the next spot gets it. `/locate` simply moves on to it.
  - If a place's spot was taken but its chunk later loads without it, the spot is given up and the next one takes over.
- The server log says where each place was built (`HexKagune: aogiri_hideout is built at x ..., z ...`).
- **`/kagune places`** lists where each one was built and checks it's really there. If one hasn't been built yet, it tells you which `/locate` to run.
- Use a **new world**: chunks made by older versions can't get them.

### Coats

Your words: "Change the armors so they dont open like skirts, simply attach to the leg properly."

- Each half of a coat's tails is now **attached to its own leg**, exactly like the trousers. It swings, turns and bends at the knee with the leg.
- No more separate draping, and no opening up like a skirt.
- Both outfits (Crimson and Formal) are previewed in a stride, a tucked jump and three kicks.

### Ukaku M1 spam

Your words: "Prevent m1 spam with ukaku."

- **The problem:** spamming M1 started the next wing slam about 60% of the way through the current one, right after its hit. That cut off the follow-through and rebound and snapped the wing straight into the next wind-up.
- **Now:** each wing slam, in the twin wings' string and Rabbit Guise's, **plays out in full** before the next one can start, however fast you click. The next hit still never lands inside the target's hit immunity.
- **Damage:** each wing slam does 1.3× its 4.1.13 damage, so the string's damage over time stays at the same pace as every other string's. Tell me if you'd rather keep the old damage and have the string a little weaker.

### Other

- Protocol 116 (unchanged).
