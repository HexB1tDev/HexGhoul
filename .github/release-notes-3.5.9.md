## 3.5.9 (HexGhoul): Bikaku: the worm goes round blocks

HexKagune 3.5.9 for Forge 1.20.1. Not tested in game.

Your words: "It should adapt to blocks, and that's where I say flexibility, like go around blocks like a worm, sort of, and actually go around them. It's being able to adapt to the terrain itself and pulling people back and shit, you know what I mean?"

- **A thrown worm finds a way round what's between it and its target:**
  - The server looks for a way through the open blocks (anything without a collision box), keeping a little off walls and never squeezing through a gap only a corner wide. It pulls that way taut into a few bends and sends it to everyone.
  - The worm is drawn along that way: its head travels the way out, its body lies along it behind, still snaking. It bites where the way ends.
  - If nothing is in the way, it's the straight line as before.
- **Which attacks:**
  - M1's bites (held or tapped).
  - Devouring Maw (G).
  - Predatory Drag (J), along the ground round obstacles, over them if that's the way.
  - Ravenous Pursuit (Z): each of its three worms from its own lane.
  - Carrion Eruption tunnels under the ground as before. The kakuja's tentacles are as they were.
- **What you aim at behind a wall** can now be bitten, if there's a way to it within the reach you held for. The way round counts toward the reach: a body 10 blocks away behind a wall may need 14 blocks of worm.
- **Pulling people back:**
  - Devouring Maw pulls its catch back round the way it went, to before you.
  - Predatory Drag drags its catch back along the ground round the way it went, to you, then lifts and slams it as before.
- **With no way to it within reach,** it goes after something else in front of you in a clear line if there is one. Otherwise it goes straight along your look and stops at the first block (M1's bites and Devouring Maw no longer go through walls).

Preview (off-game, the route finder and the drawing exactly as the game uses them): `docs/handoff/bikaku-round-3.5.9.png`. A wall between you and a body 10 blocks out: M1 held, Devouring Maw and Predatory Drag each go round it, and the catches are pulled back round it.

- **My guesses:**
  - How hard it searches (up to 6,000 blocks looked at, at most 6 bends).
  - How much it keeps off walls.
  - That a catch is pulled back along the way it went.
  - That a body is bitten if it is still near where the way ends.
- **Approximated:**
  - The way is found once, as the throw begins, to where the body was then. If it moves a lot meanwhile, the worm still bites where its way ends.
  - The worm's side-to-side sway can graze a wall's edge.
  - The search treats the worm as thin; a gap one block wide is enough.
- **Next:** the forms (two worms, three worms, the walking form), each with its own moves.
- Network: protocol 100 (the state carries up to four ways round blocks). No new Minecraft API (the block checks the mod already uses).
- Tests: BikakuWormTests (108,146): the route finder (straight when clear; round a wall, every leg clear, within reach, a few points, in negative coordinates too; none out of a sealed box, none past its reach, never through a corner-wide gap); laying a worm along a way (straight changes nothing, start and end kept, halfway is halfway, the coils behind left alone); a bite round a wall never entering it and biting where its way ends. BikakuTests.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 100. Not tested in game.
