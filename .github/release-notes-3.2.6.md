## 3.2.6 (HexGhoul): Fix: crystals sounded like punches

HexKagune 3.2.6 for Forge 1.20.1. Not tested in game.

The owner's bug report (Ukaku): "Crystals sound DOES not sound like crystals, it sounds like punches..."

**What caused it.** Every crystal sound in the game (Ukaku's crystal shards, the crystal wings, the crystal spear and javelin, glass breaking) plays from the mod's own sound pack, and the pack had no glass or crystal sound in it. So:

- the **shatter** was a bone crack and two punch collisions;
- a **crystal shard's hit** was the pack's punch hits ("a spike sinks in");
- a crystal **forming** was two heavy spike throws.

**The fix:**

- No punch, punch collision or bone sound plays for crystal any more.
- Crystal is built from the pack's brightest metal, each file raised into a ring:
  - **forming:** a steel spike, a needle flick, a spinning spike, raised into a glassy ring;
  - **shattering:** four of them raised high into a tinkle;
  - **a crystal's hit:** its own new pool, a raised needle flick, steel spike or dart. Real stabs keep their sound.

**Not perfect, and why.** The pack still has no real glass recording. I couldn't add one: every recording in the pack is CC0 with its licence checked at the source, and the source site is blocked from here. **If you want true crystal, send me crystal or glass sound files** (as you did with the clash sounds), or tell me if vanilla's amethyst sounds are OK to use, and they go in.

- No network changes (protocol 92). No new Minecraft API.
- Tests: SfxTests check that no crystal pool has a punch or bone sound in it, that every crystal sound is raised into a ring, and that a crystal's hit and break play the crystal pools.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 92. Not tested in game.
