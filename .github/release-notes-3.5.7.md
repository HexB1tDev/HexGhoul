## 3.5.7 (HexGhoul): Bikaku: the kakuja's four moves and its passives

HexKagune 3.5.7 for Forge 1.20.1. Not tested in game.

The rest of your kakuja spec. All four moves open by mastery alone, with the kakuja itself (1000). In the kakuja they sit on the worm's keys: Z, G, B, C. Nothing else is on a key in it (pressing one says so).

- **Thousand Jaws (Z).** Every mouthed tentacle goes for a body round it (the one you aim at first, then each tentacle the body on its own side; with fewer bodies they share) and bites it again and again on its own rhythm, about every 0.6 s, for about 3 s.
  - Each bite: a little over a heart, some bleeding, a little healing.
  - With nobody for a tentacle, it lashes at the air on its side.
  - Three times during it, the grinning mouths on its body snap at whatever is within about 4.4 blocks of its middle: a big bite (over 3 hearts), a brief stun, and a pull in against the mass.
  - 12 RC, 20 s cooldown.
- **Endless Hunger (G).** Three of its mouths and its mouthless tentacle go after the body you aim at (up to 14 blocks).
  - Caught, the tentacle coils round it and holds it where it is, and the three mouths bite it in turn from round it: six bites after the one that caught it.
  - Then it is pulled into the great maw, which gapes and shuts on it: a crushing bite of about 10 hearts and a stun. Then it is spat out in front.
  - Every bite heals you: 1 heart each, 3 for the crush.
  - Missed, the mouths shut on nothing and draw back.
  - 10 RC, 18 s cooldown.
- **Flesh Cataclysm (B).** The whole mass draws in on itself, then bursts out, flinging all six tentacles out straight and level, while every mouth on it roars (the pack's roar recording).
  - Anything within about 5 blocks of its middle is hurled outward (over 4.5 hearts, a stun).
  - The tentacles then sweep about a third of the way round it, and whatever one passes, out to about 16 blocks, is struck hard: about 8 hearts, a stun, thrown back. Each body is hit once.
  - It settles back as it was.
  - 14 RC, 25 s cooldown.
- **Undying Abomination (C).** For 10 s it regenerates at an extraordinary rate (half a heart every 0.2 s on top of the kakuja's own) and takes still less from every blow (60% of what the kakuja would take: 27% in all).
  - A blow of 4 hearts or more slows its healing to a third for 2 s; it never stops it.
  - Glowing strands of new flesh crawl over its body and wind down its tentacles while it lasts.
  - It runs alongside everything else.
  - 20 RC, 60 s cooldown.
- **Autonomous Predation (passive).** Its mouthed tentacles bite, on their own, anything hostile within 12 blocks of it, each with its own lunge and bite:
  - Hostile means: monsters (not neutral ones unless they're after someone), anything hunting you, and whatever last hurt you.
  - Each tentacle bites at most every 2.25 s or so, never two starting at once.
  - Not while a move of its own is using that tentacle.
  - Each bite: over 2.5 hearts, some bleeding, a little healing.
- **The other passives** were already in (3.5.4): Monstrous Regeneration (1 health every 8 ticks), Abnormal Flesh (45% of every blow, no knockback), Immovable Horror (Slowness VI, reach 14). The board now names its bites of its own.
- The move guide shows the four moves (in the kakuja only) and "Bites on its own"; the menu lists them under "IN THE KAKUJA".

Previews (off-game): `docs/handoff/bikaku-kakuja-moves-3.5.7.png` (each move over time, and its own bites), `docs/handoff/bikaku-kakuja-undying-3.5.7.png` (Undying Abomination up close).

- **My guesses:** all the numbers above (damage shares, heals, cooldowns, RC, timings, reaches, the sweep's size), which tentacles do what, what counts as hostile, the sounds (the pack's writhe, body warp, roar and bite recordings).
- **Approximated:** when your M1 tentacle bite and one of its own bites pick the same tentacle, the M1's is drawn and the other's bite still lands. Flesh Cataclysm's sweep is judged from where the tentacles are on the server, which can differ by a hair from what you see.
- **Next:** the range you asked for (held, up to about 30 blocks, round blocks, no attack until the worm is fully back); then the new forms (two worms, three worms, the walking form), each with its own moves.
- Network: protocol 98 (the state carries a body for each tentacle, when Undying Abomination began, each tentacle's own bite; four moves appended). No new Minecraft API.
- Tests: BikakuWormTests (107,334: each Thousand Jaws bite on its body and shut; Endless Hunger's bites on its catch, its coil round it, the maw gaping and shutting on it; Flesh Cataclysm drawn in, burst, thrown far and level, swept and back; Undying's mending in and out; its own bite on its body; every frame finite and never jumping), BikakuTests (1,401: keys in and out of the kakuja, mastery, the board, the numbers), ControlTests.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 98. Not tested in game.
