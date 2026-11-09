## 3.6.1 (HexGhoul): Bikaku: every move made by all the worms

HexKagune 3.6.1 for Forge 1.20.1. Not tested in game.

Your words: "to basically become overpowered, like, to do enhanced versions of themselves whenever they're, like, in different transformations, such as, like, the move that, like, drag pursuit, you can send all three of them, all three of the worm-like things, and the thing that sends the worm underground and then up can be like more than one, and whatsoever." Then, on the plan: "approved, continue".

- **In Twin Worms and Three Worms every move is made by all the worms.** Each worm moves from its own lane, a beat after the one before it:
  - **Devouring Maw (G):**
    - Each worm goes after its own body: first the one you aim at, then the nearest others in front of you within reach.
    - Each catches its body, shakes it and flings it: the first to your right, the second to your left, the third ahead and up.
    - If there are fewer bodies than worms, the extra worms go for the first body. One worm holds it and the others bite it.
  - **Predatory Drag (J):** works the same way along the ground. Each worm drags its own body in, lifts it and slams it.
  - **Ravenous Pursuit (Z):**
    - Each full-size worm goes after its own body: the body on your right goes to the right worm, the one on your left to the left worm, and the one between to the third.
    - Each worm bites its body three times, drawing back and snaking in again between bites.
    - No thinner worms are added in these forms. With One Worm, Pursuit is as before (three thinner worms).
  - **Carrion Eruption (B):** comes up once per worm, each under its own body, one after the other.
  - Each worm's bite does the move's full damage, with the same heal and bleed as a single worm's bite.
  - Holding a move to throw it further (3.5.8) and going round blocks (3.5.9) work for every worm. Each worm finds its own way round the blocks.
- **Unchanged:**
  - One Worm (exactly as before).
  - Monstrous Regeneration.
  - The kakuja and its moves.
  - Each move's cooldown.

Preview (off-game): `docs/handoff/bikaku-form-moves-3.6.1.png` shows these rows:
- Three Worms: Maw on three bodies.
- Twin Worms: Maw on one body.
- Three Worms: Drag.
- Three Worms: Pursuit, three bites each.
- Three Worms: Eruption, one under each body.

- **My guesses:**
  - The beat between worms: 3 ticks for Maw and Drag, 2 for Pursuit, 6 for Eruption.
  - Which bodies the worms pick: the aimed one first, then the nearest others in front.
  - Each worm's bite at full damage, so the whole move does about two or three times as much in these forms.
  - Pursuit's three bites per worm, 8 ticks apart.
  - Which way each worm flings its catch.
  - The cooldowns kept as they were.
- **Next (3.6.2): The Walker.** Two walking tentacles come out of the worms' root and walk for you: your body stays steady, their feet find the ground and walls, and they climb anything. Its moves will be made stronger as well. Previews come first.
- Network: protocol 102. The state now carries each worm's body, its catch and its point. No new Minecraft API.
- Tests:
  - BikakuWormTests (124,414). With one worm, every move is exactly as before. In a form, each worm lands on its own body at its own beat with its jaws shut, Pursuit's worms bite theirs three times each, and Eruption bursts up under each body. Each worm is at rest until its beat and back at rest at the end, never jumps, and is never drawn through you.
  - Also run: BikakuTests, ControlTests, PathTests, PathScreenTests.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 102. Not tested in game.
