## 3.5.1 (HexGhoul): Bikaku: Ravenous Pursuit (Z) and Carrion Eruption (B)

HexKagune 3.5.1 for Forge 1.20.1. Not tested in game.

The next two of your Bikaku abilities, opened by mastery alone (no NPC or quest).

- **Ravenous Pursuit (Z, mastery 300).** The worm thins and splits into three thinner mouth-bearing worms from the same root, each in its own place well apart from the others and away from your body: one out past your right shoulder, one out past your left, one high and further back behind your head. Your words on the first preview: *"they're too close up to each other ... forming a kind of mass in the back ... separate them more, make them further away from the body if need be, just so it doesn't create that mass in the back ever."* Now no two of them come within about 0.9 blocks of each other behind you, at any moment (a test checks it). Each goes after its own body near you (within 7 blocks), the right worm after the one on your right, the left worm after the one on your left, the high one after the one between. They bite one after another, each thrown out in a fast snaking lunge from its own side, never in a straight line. Against one body all three bite it in turn. Each bite: 1.1 times a bite's damage, bleeding, and half a heart back. Then the two draw back into the root and the worm is whole again. 12 s cooldown, 5 RC.
- **Carrion Eruption (B, mastery 450).** The worm comes over your shoulder and drives down into the ground in front of you. It tunnels under the surface to what you aim at (up to 12 blocks), the ground heaving and breaking over it all the way (dust and chips). It bursts up under them: a crater blown out of the ground, gobbets of flesh thrown up, the ground shaking for whoever is near, its jaws thrown wide. It shuts on everything standing there (1.6 times a bite's damage, bleeding) and throws them up into the air (about 4 blocks). It hangs there a moment, sinks back into the ground, and comes back out in front of you. 14 s cooldown, 6 RC.
- Where the eruption comes up is fixed as it arrives under them (dodging during the tunnelling works).

Previews (off-game; the preview floor hides what is underground, as the ground does in game; brown marks show where the ground heaves): `docs/handoff/bikaku-pursuit-3.5.1.png`, `docs/handoff/bikaku-eruption-3.5.1.png`.

- **My guesses:** how far out the three worms are (1.5 blocks to each side, the middle one 1.1 higher and 0.5 further back), their thickness (62%), their bite timing (12, 17, 22 ticks), the snaking; the tunnel depth (1.5 blocks), how high it rises (2.6 blocks), the launch (0.85), the radius it hits (1.6 blocks); reaches 7 and 12; cooldowns 12 s and 14 s; RC 5 and 6; the sounds (the original's anchor thud as it comes under them, a bone crunch as it bites).
- If the ground is not solid between you and them (a cave, water, a drop), the worm's tunnel can show through it.
- **Still to come:** Monstrous Regeneration and the passives, then the kakuja.
- Network: protocol 95, unchanged. No new Minecraft API.
- Tests: BikakuWormTests (each Pursuit worm on its own body and shut as it bites; never closer than 0.85 to another behind you; split thin, whole again at the end; none jumps or passes through you; the eruption under the ground while tunnelling, up over its mark and shut as it bites, its jaws wide just before, never jumping, back to rest), BikakuTests (Z and B, their mastery and beats).

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 95. Not tested in game.
