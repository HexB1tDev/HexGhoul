## 4.1.43 (HexGhoul): one of each, really: the claim taken as a place is built; never two Etos

HexKagune 4.1.43 for Forge 1.20.1. Not tested in game. **Test in a new world** (old chunks keep what older versions built).

Your words (of 4.1.42 in a new world): "Structures are still spawning doubles, they're not unique"; "and they're also spawning doubled NPCS.
eto spawned 2 of them".

**What was wrong:** a one-of-a-kind place took its spot (its "claim") only when whoever asked for it *looked like* the world's own chunk
making, judged by what the request carried. When it didn't look like it, the place was built anyway, unclaimed. Any mod that drives chunk
generation its own way made that guess wrong, and every region got its own Anteiku, library, Ken's tower, mansion, hideout and CCG
building, each with its people. And with two libraries loaded, the second Eto never left: she waited, or took over as you came near.

**Now:**
- **The claim is taken when the place is actually built** (when its pieces are made), which the world's making and `/place` always do and
  `/locate` never does, whoever drives it. A spot that can't take the place (another spot holds it, or another place is within 512 blocks)
  builds nothing at all. No guessing left.
- **`/place` follows the same rule:** it builds a place only while the world has none yet (that one is then the world's one). If the world
  already has it, `/locate structure hexkagune:<id>` finds it.
- **Never two of her:** a second body of Eto (or Touka, Ken, Ayato, Noro) leaves the moment it finds hers loaded and about. Only a body of
  hers lost in unloaded ground far off is stood in for, as before.
- Everything from 4.1.42 stays (flat ground, foundations, no mountains, spread out).

### Other

- Protocol 118 (unchanged).
