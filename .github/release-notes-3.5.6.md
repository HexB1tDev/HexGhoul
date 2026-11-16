## 3.5.6 (HexGhoul): Fix: Predatory Drag swings out wide round you

HexKagune 3.5.6 for Forge 1.20.1. Not tested in game.

Your words on 3.5.5's Drag (its preview from above): "why not make a bigger angle? it's going too close to the body".

In 3.5.5 the Drag's worm went round you instead of through you, but it hugged you: measured off-game, past its first block and a half out of your back, its skin passed within about 5 cm of you in half its frames.

- **A wide arc:** the worm comes out of your back and swings out wide to your right before it goes for its target:
  - The coils it leaves behind you sit out to your right, about 1 to 1.6 blocks out.
  - It comes down past a point about 1.75 blocks out to your right.
  - It reaches the ground about 1.5 blocks out to your right and a block ahead.
  - On a long throw, its coils spread along that whole wide way (before, they all went far out and the worm ran past your hip to them).
- **Its mouth grips the catch from its own side** (from out at your right front), not from between you and the catch. While it drags a catch in to you, its body no longer lies along your shins.
- **It holds the catch up from further out**, about 1.35 blocks to your right and in front of your shoulder.
- **On its way back to rest** (after the slam, or after a miss), its head swings out wide instead of passing close in front of your face.
- Measured over the same targets: past its first 1.6 blocks it now keeps about 0.6 blocks clear of you in half its frames, and never comes closer than about 0.25. It never goes through you (as in 3.5.5).
- Where the catch is dragged, lifted and slammed is unchanged.

Preview (off-game, 3.5.5 against 3.5.6, the same frames): `docs/handoff/bikaku-drag-wide-3.5.6.png`.

- **My guesses:** how wide (the numbers above), and that the arc is round your right side, where the worm lives.
- **Unchanged:** the other moves (only the Drag was too close); the snap for a target behind you or straight to your left (3.5.5's known limit).
- **Next:** the kakuja's four moves and its passives; then the range (held, up to about 30 blocks, round blocks); then the new forms.
- Network: unchanged (protocol 97). No new Minecraft API.
- Tests: BikakuWormTests (105,678: the Drag never closer than 0.2 past its first 1.6 blocks, and 0.45 or more in half its frames, at six targets in front of you; all of 3.5.5's checks).

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 97. Not tested in game.
