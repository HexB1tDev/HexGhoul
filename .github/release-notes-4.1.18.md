## 4.1.18 (HexGhoul): a real katana, five cuts that hit where they look, quinques that slide out of their cases

HexKagune 4.1.18 for Forge 1.20.1. Not tested in game. Previews: `docs/handoff/katana-model-4.1.18.png`,
`docs/handoff/katana-hits-front-4.1.18.png`, `docs/handoff/katana-hits-side-4.1.18.png`.

Your words: "We need to improve every single M1 of every single weapon. They're bad. I mean, you keep passing them, but they're just the
most basic garbage you can have, which I don't approve of ... So upgrade the Quinkays, add better pull-out animations to the Quinkays so
that they are more realistic when they're returning to the case ... improve the ones that are already in here, like the katana looks like
shit."

This is the first part of the quinque overhaul: the katana done properly, and the framework every other quinque will use.

### The katana, rebuilt

**Before:** a black bar with a glowing red stripe down one side, a box for a guard, a flat-colour hilt.

**Now:**
- **The blade** is steel, each flat running spine to edge: the spine's shade, the steel, a continuous glowing crimson hamon (the temper line; it's a Bikaku quinque), and the bright edge.
- **The curve** is a katana's: the point drawn back toward the spine, the edge on the outside. It thins toward the point, and the edge rises to meet the spine in three steps.
- **The guard** (tsuba) is an iron oval between two gold washers, with a gold collar (habaki) on the blade.
- **The hilt** (tsuka) is wrapped in black diamonds over white ray skin, with a gold ornament on each side, a gold collar and an iron cap.

### Five cuts, not three

**Before:** three swings written as raw arm angles, each one ending pointing straight ahead, the legs never moving.

**Now there are five, each its own:**
1. **Kesa-giri:** up into hasso by the right shoulder, then down across the body to the left hip, the right foot stepping in.
2. **Gyaku-kesa:** gathered low on the left, rising across the front to high on the right.
3. **Yoko-giri:** the blade laid level out to the right, swept flat across the chest, the hips whipping round.
4. **Tsuki:** drawn back to the right hip, then driven straight out with a long lunge.
5. **Kaiten-giri, the finisher:** coiled low, then the whole body spins round once with the blade level out in front. Its 3.5-block sweep through everyone around you finally matches what you see.

Every cut has a breath of anticipation, the windup, the cut at full speed through the impact, a follow-through that carries on past it, and a settle back into the stance. The hits now run 9 to 14 ticks each, the spin the longest. CCG investigators with katanas fight with the same five.

**How it's built** (and how every other quinque will be):
- Each moment of a cut is written by what it does: which way the blade points, and where the hands are.
- The arm's angles are solved for it on the katana's real held model. This is what was wrong before: the same wrist angle that lays a blade flat with the arm out to the side tips it into the ground once the arm swings in front.
- The solved keys are worked out ahead of time, so nothing is calculated mid-fight.
- Tests check the blade points where each moment says, the arm never flips over between frames, and the left hand stays on the hilt (within 1.25 pixels through all five).

### Quinques sliding out of their cases

**Before:** a weapon came out of its case (and went back in) in two jumps, a third and two-thirds of the way out.

**Now:** every quinque slides out and back through seven frames, a seventh at a time.

### What's next

- **First person:** in first person every quinque still plays one of three generic stand-in motions (a wave, an overhead, a thrust) that don't match what your body does. Next, first person gets drawn from this same animation: the real hand and blade, seen from your eyes.
- **The rest:** the blade, the spear, the mace, the launcher and Dawn get their own strings the same way. Then the new kagune-born quinques (whips and more swords).

### Other

- Protocol 117 (unchanged).
