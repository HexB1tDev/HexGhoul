## 3.5.0 (HexGhoul): Bikaku: Devouring Maw (G) and Predatory Drag (J)

HexKagune 3.5.0 for Forge 1.20.1. Not tested in game.

Your words after 3.4.9's previews: *"I think it's fine now. You can continue for the final stuff ... These abilities are not going to be taught by anyone currently in the mod. We're going to advance to that later. It's going to be a custom NPC."* So the worm's moves open by mastery alone: no lesson, no quest.

What's new:

- **The worm's moves are on your kagune keys.** On the Bikaku path the original's grab, grapple, slam and other keys (rebindable in Controls) are the worm's own moves now. The move guide and the menu show each move on its key, dim with the mastery it needs until then, and the menu shows each cooldown.
- **Devouring Maw (G, from the start).** The worm draws far back with its mouth opening extremely wide, is thrown out at what you aim at (about 6.5 blocks from over your shoulder), and snaps shut round it. The teeth clamp on: it is pulled up in front of you and violently shaken for about a second (stunned, bleeding, a heavy bite: 2.2 times a bite's damage), then flung away out to your right. A catch heals you a heart (two more on a kill). Missed, it snaps on nothing and draws back. 8 s cooldown, 3 RC.
- **Predatory Drag (J, at mastery 150).** The worm runs down beside your heel and along the ground at what you aim at (up to 10 blocks), mouth open, teeth bared, weaving like a snake. It bites on low, drags them back across the ground to you, lifts them high and slams them down in front of you (the slam: 1.8 times a bite's damage, a 1.2 s stun, bleeding), then lets go. 10 s cooldown, 4 RC.
- The catch is held where the worm's mouth is: the server moves it there every tick, and every player sees the mouth on it.
- A key pressed while the worm is still biting waits for the bite to end. Keys with nothing on them yet (Z, B, C, V, N, H) say so.

Preview (off-game, the same motion code the game uses; the green stand-in is moved exactly as the server moves a catch): `docs/handoff/bikaku-grabs-3.5.0.png`. Menu: `docs/handoff/bikaku-menu-3.5.0.png`.

- **My guesses:** each beat's timing; the damage shares; the stuns; the throw's strength (1.25 sideways, 0.5 up); the hold height; reaches; cooldowns and RC; that the fling goes to your right.
- **Not built yet:** Ravenous Pursuit, Carrion Eruption, Monstrous Regeneration, the passives besides feeding, the kakuja (the next versions). No body animation goes with a move yet.
- Network: protocol 95 (the move keys, and the worm's move in its state). No new Minecraft API.
- Tests: BikakuWormTests (both moves at near, far, sideways and out-of-reach targets, caught and missed: the mouth shuts on its target and stays on its catch, never jumps, never passes through your head, body or legs, ends at rest; the fling goes right; the drag keeps to the ground, lifts high and slams down), BikakuTests (each move on its own key, opened by its mastery, its beats in order, the board).

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 95. Not tested in game.
