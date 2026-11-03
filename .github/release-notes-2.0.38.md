## 2.0.38 (HexGhoul): Rinkaku tails grow out of the back

HexKagune 2.0.38 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Rinkaku tails now grow out of the body: each tail's root is sunk inside the lower back (its start hidden in the body) and its first 0.4 blocks leave the back as a rigid base, fanned out to its own side, before it blends into the motion it always had. Before, every tail started at a point hanging just behind the hips and fell from there like a rope.

- Tails leave backwards and round to their own side, the first pair rising a little, the next level, the next falling, each pair a little further round. Past about 0.7 blocks the tail is wholly its own motion again, so strikes, grabs and the idle sway are unchanged.
- The roots still ride the torso as it is drawn (a lean, a lunge, a crouch, a flip), as since 1.0.24. Centipede legs are unchanged (their own renderer).
- Pure geometry in `RinkakuRoot` (tested: root inside the lower back, the base straight out of it, the tip untouched, no NaN). Plotted before and after on a body silhouette: `docs/handoff/rinkaku-roots-2.0.38.png`. Protocol stays 77.
