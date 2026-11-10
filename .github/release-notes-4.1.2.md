## 4.1.2 (HexGhoul): The Ukaku's wing slams and wing guard

HexKagune 4.1.2 for Forge 1.20.1. Not tested in game.

Your words: "Another rework: Ukaku kagune base firery form no longer fires projectiles, M1s are a series of different wing slams combos. (create highly detailed moveset switching between left wing and right wing slaps, swings, upwards to downwards slams (These can now clash like Rinkaku) and has lightly less range than Rinkaku. Right click (M2) is now a protection like Rinkaku has, wings grow and protect the player , they come together to protect the player, just like rinkaku they break after a bunch of hits, perfectly timed right clicks can do the same as Rinkaku and stagger. Note: Crystal mode still has the same custom shooting crystals and right click, we're only touching the base from, everything else stays as it is."

### M1: the wings slam

The fire (the base form) no longer shoots. Each left click is a blow of a wing, and the string gets heavier as it goes on.

- **Twin wings, 7 blows:**
  1. Right Wing Slap
  2. Left Wing Slap
  3. Right Wing Swing: a wide one, 160°, carried right across your front
  4. Left Rising Wing: from the ground upwards
  5. Right Overhead Slam: from above, down
  6. Left Overhead Slam
  7. Twin Slam: both wings brought down together, the heaviest
- **Rabbit Guise** (a new Ukaku's one wing, the left, under mastery 100), 4 blows: Left Wing Slap, Left Wing Swing, Left Rising Wing, Left Overhead Slam.
- **They clash like Rinkaku's:** two blows that meet stop each other, by the same rules.
- **Reach:** 85% of what Rinkaku's M1 would reach at your mastery.

### M2: the wing guard

- Hold right click: the wings come together in front of you and grow up to 30% longer as they close.
- It follows Rinkaku's block in everything:
  - it covers your front;
  - each blow it stops adds weight, and at enough weight it breaks: you're pushed back hard and can't raise it for 5 seconds;
  - a right click timed just before a blow lands (within 0.15 s) staggers the attacker.
- Rabbit Guise's one wing guards too.
- You can't guard in the middle of a blow of your own.

### What is gone from the base form

Because M2 is now the guard:

- the fire shots;
- the fire's right-click enders: Rabbit Hop, Flare, Rising Flame, Fire Fan, Rabbit Dash and Black Rabbit;
- the W + right-click Fire Dash.

### What stays as it was

- **Crystal mode (V):** its spikes, its right-click enders (its own Rabbit Hop included) and Fire Dash.
- The One-Winged Kagune, both kakuja, the Wing Vortex (Z) and every other key.

### Your mutations

- **Fire Dash** now does nothing in the base form. It still works in crystal mode. Its text in the menu says so.
- **Bursting Shards** has no fire shards to burst in the base form. It still works on crystal spikes, the kakuja's needles and the final kakuja's fire.
- **Fire Dance** (the Wing Vortex), **Agility Boost** and **You Thought You'd Get Rid of Me That Easily?** are unchanged.

### My guesses

- The order of the blows, their speed, damage and width.
- The reach: 85% of Rinkaku's ("lightly less").
- How much the wings grow in the guard: 30%.
- The guard uses Rinkaku's numbers unchanged (weight to break, the 0.15 s perfect window, the stagger).

### Network and tests

- **Network:** protocol 111 (two new M1 strings and five new blows are sent). Clients and the server must both be on 4.1.2.
- **UkakuTests (`wingSlams`):**
  - the twin string goes right, left, then both, and Rabbit Guise's uses only the left wing;
  - no blow is a shot, a flame, a crystal or an ender, so every one can clash;
  - the reach is under Rinkaku's;
  - the guard is on for the twin wings and Rabbit Guise in the fire, and off in crystal mode, for the One-Winged Kagune and for both kakuja;
  - crystal mode still shoots;
  - the guarding wings are in front of the body, and longer than at rest;
  - right and left blows are mirrored, and no blow sinks a wing into the ground.
- **The full suite.**

Validation: the full `python3 build.py` completed with EXIT 0 on these sources before this was pushed. Not tested in game.
