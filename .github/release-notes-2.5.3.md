## 2.5.3 (HexGhoul): The finger crack: the owner's sound; the arm stretches out, then bends, smoothly

HexKagune 2.5.3 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

The finger crack uses the owner's own sound now, and the arm stretches out straight first, then bends at the elbow (the hand curling up toward the chest) before the crack, all of it eased, never a jump; it straightens and lowers together afterwards.

- `sounds/sfx/bone/finger_crack.ogg`: the owner's finger pop (supplied as MP3), converted to mono OGG Vorbis (Minecraft only places mono sounds in the world), the 0.2 s of silence before the pop trimmed so the pop lands on the crack frame (`EtoGesture.CRACK_SNAP`), the two smaller pops after it kept, faded out at 0.27 s; every tag stripped and the file named for its job (the jar's name rules).
- `EtoPose.crack` / `crackOver`: reach (`CRACK_REACH_X/Y`, straight) by tick 14; the elbow bends from tick 9 to the press (`crackBend`), the arm settling a little lower and in; the crack at 22; straightening and lowering from 34 to 52 (`crackRelease`). A companion's stiff arm shows the reach and the settle; a player's shows the bend too.
- `EtoTests`: straight at tick 9, bent before the crack, never a jump between quarter ticks. Previews: `docs/handoff/ken-crack-2.5.3.png`, `docs/handoff/player-crack-2.5.3.png`.
