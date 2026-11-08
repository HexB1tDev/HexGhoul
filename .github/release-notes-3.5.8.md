## 3.5.8 (HexGhoul): Bikaku: hold to throw further, up to 30 blocks

HexKagune 3.5.8 for Forge 1.20.1. Not tested in game.

Your words: "it should be a held move that the longer you hold it for, the longer it goes for. And I, if you ask me, this specific kagune should have a lot of range, like covering up to like 30 blocks. Of course, attacks are not able to be performed until the kagune is pulled fully back. So that's the only disadvantage that this kagune has." (Going round blocks is the next version.)

- **M1 and the worm's thrown moves are held now:** M1's bites, Devouring Maw (G), Predatory Drag (J), Ravenous Pursuit (Z) and Carrion Eruption (B).
  - Press and the worm winds up: it draws back and up behind your shoulder, coiling and trembling, its mouth slowly opening. Everyone sees it.
  - Let go and it's thrown as far as you held it for.
  - A tap reaches what it always did. The reach grows the longer you hold, to 30 blocks after 2 s, and no further.
  - While you hold, your reach shows on the action bar ("Reach: N blocks").
- **A longer throw takes longer, out and back:** every beat of it is slower, about 5% for each block past its usual reach. A full 30-block M1 bite takes about 1.3 s, and Predatory Drag at 30 about 4.6 s.
  - Nothing else can be thrown until the worm is back. A click or key let go meanwhile waits for it, as before.
  - Monstrous Regeneration (C) isn't held.
- **Thrown far, the worm stretches the whole way:** it snakes along its full length, the wave running down it as it goes.
- **What it goes after:** the body nearest your line of sight within 31 blocks, not just what's under the crosshair (that only finds bodies close by). The server still checks it's in reach and in sight.
- **M1 is now taken by the mod, out of the kakuja** (as War's is). On a block in reach with nobody to go after, it digs as before; on something that isn't alive (a boat, a frame), it hits as before.
- **In the kakuja, nothing changed:** its M1 is a click, its moves as in 3.5.7.
- The move guide adds "Hold: further"; the menu says how far and how long.

Preview (off-game): `docs/handoff/bikaku-held-3.5.8.png`. Top row: the wind-up over a 2 s hold. Then an M1 bite thrown 30 blocks; Predatory Drag 30 blocks along the ground and back; Ravenous Pursuit at 20; and a 30-block throw seen from above.

- **My guesses:**
  - 2 s to the full 30 blocks, growing evenly.
  - 5% slower per block.
  - The wind-up's look.
  - Picking the body nearest your line of sight.
  - A hold that's never let go throws by itself after 12 s.
  - The kakuja left as it was.
- **Not yet:** it doesn't go round blocks; a throw still needs a clear line to what it bites. That's next (3.5.9), as you said: "go around blocks like a worm".
- **Next:** going round blocks, then the forms (two worms, three worms, the walking form), each with its own moves.
- Network: protocol 99. The state carries the bite's and the move's reach and a wind-up; new actions: a key let go, M1 pressed and let go. No new Minecraft API.
- Tests: BikakuTests (1,588: a tap as before, a full hold 30, the longer the further, slower further, which moves are held). BikakuWormTests (108,113: each M1 bite held full lands on a body 28 blocks out, shut, 25+ blocks long, never jumping, drawn round your body, at rest only after its longer time, snaking along its first half; Devouring Maw and Predatory Drag held full bite 28 out and end at rest; Ravenous Pursuit's worms bite at 20; the wind-up draws back). ControlTests.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 99. Not tested in game.
