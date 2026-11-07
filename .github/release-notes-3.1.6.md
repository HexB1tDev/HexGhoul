## 3.1.6 (HexGhoul): Rinkaku's Rubble Hurl on the Throw key

HexKagune 3.1.6 for Forge 1.20.1. Not tested in game.

- **N (Throw) with nobody held is now the Rubble Hurl.** With someone held by a tail, N is still the throw, exactly as before. The original's "no target" message on an empty throw is gone. The owner chose this idea for the Throw key: "N always throws something: a held body, or, with none, the ground itself."
- **Press N**: up to two free tails (never a centipede leg) dig into the block you look at (within 7 blocks), or the ground just in front of you, and tear a chunk out. It rises into the tails' grip and hangs in front of and above you.
- **The block really comes out**, the same way Pale's smashed blocks do: only where you may break it (claims, spawn protection and adventure mode are asked first), nothing harder than stone/ores (3.0), no blocks with contents, and it is put back exactly after the block-smash restore time (30 s by default). Turning block smashing off in the config leaves the world untouched; the chunk still tears off. The block you stand on is never taken.
- **Hold N** and the chunk grows: from about one block across to two at full charge (1.5 s). Held 4 s it goes by itself.
- **Let go** and the tails whip it at what you're looking at (a body under your aim, else the block, up to 48 blocks). It is lobbed so it lands on the mark. In the final kakuja it splits into three pieces that fan out.
- **It bursts** on the first body or wall it meets: everything near is hurt (1x to 2x the tail's swipe damage at the middle, 40% at the edge of the burst), knocked back and up, and stunned for 0.5 to 1.4 s. The block breaks up there with its own particles and sound. A chunk charged 75% or more also craters the weak ground where it lands (restored later like any smashed block). Hits tear wounds as a crush. Never hits your allies, owned pets, partner or creative players.
- Costs 1.6 RC; 44 ticks of cooldown after the last piece bursts. Unlocks with the throw (mastery 250). Works in Seed, Twin, Four-Tail and the final kakuja; not the first kakuja. While the tails hold a chunk, Thy Death, the block and the Impale wait (and it waits for them).
- Drawn on every client as the torn block's own model (a big cube with smaller lumps stuck round it), turning in the grip and tumbling in flight; the gripping tails bend to it and whip after it on the throw.
- Ability guide: the G·N row reads "Grab · Throw · Rubble Hurl (hold N, nothing held)".
- Network: C2SUkakuAction HURL (11) and a new S2CHurl (message 53). **Protocol 91**: client and server must both have 3.1.6.
- One new allowlisted Minecraft member: `ItemDisplayContext.NONE` (joined.tsrg cfw.a, retained name; intermediary field_4315), to draw the block's model untransformed. The `Block` compile stub now declares `ItemLike`, as the real 1.20.1 class does.
- Pure rules in `RinkakuHurl`; 28 new checks in ControlTests (charge and size, the burst's reach and falloff, the throw landing exactly on its mark near, far, up and down, the kakuja's spread, hit tests, the rubble's shape, the guide). Offline preview: `docs/handoff/rinkaku-hurl-3.1.6.png` (`tools/HurlPreview.java`; flat grey cubes stand in for the block's model there).

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 91. Not tested in game.
