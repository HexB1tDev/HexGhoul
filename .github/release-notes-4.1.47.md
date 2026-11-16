## 4.1.47 (HexGhoul): the ghouls' arm blade rebuilt, its moveset and stance

HexKagune 4.1.47 for Forge 1.20.1. Not tested in game. **Update the server and every client together** (protocol 120 to 121).

Your words: "Improve severely the kagune arm blade of the ghouls, it looks way too small, not sharp and horribly made compared to literally
every other kagune", your picture ("it should look like this"), your choice "B: blade turned over", then "also give it a proper moveset,
slash attacks and a proper ambient stand" and "make sure that kagunes use random colors on this one too".

**The look (as your picture):**
- **Two broad, smooth ribbons wound round the whole arm**, shoulder to wrist, standing off it with gaps between, flaring at the wrist
  where they flow into the blade, **red veins branching across them**.
- **One huge, smooth, single-edged blade** grown through the forearm, about 26 pixels past the fist (it was 15): more than twice the
  arm's length from the shoulder, widening toward its far end, its edge a long curve sweeping up to a sharp point. A **bright glowing
  gleam runs all down the edge**, and veins mark its root where the ribbons flow in.
- **Turned over** (your choice B): with the arm held out, the edge and the point face down.
- **Random colours:** every ghoul's blade is its own random kagune colour, as all wild kagune are (the original's crimson, any hue, the
  rare near-black and bone). Veins are crimson (a darker red on a red kagune).
- No more spikes, thorns or plates.

**The moveset (four slashes to a string, each edge-first):**
1. **Chop down** from high over the right shoulder to low on the left.
2. **Rising backhand** from low on the left up to high on the right (the arm rolled so the edge leads upward).
3. **Sweep:** drawn far round to the right, then cut level across to the far left. **It cuts everyone in front of it** within reach,
   not only its target (never one it would spare).
4. **Cleave (finisher):** the blade raised high behind the head, held a breath, then a lunging overhead cleave that throws the target
   back.
- Every cut wounds as a slash. Reach goes up a little with the bigger blade (3.2 blocks, plus 0.4 for the sweep and 1.2 for the cleave).
- The blade never goes into the ground in any frame of any cut, the stance included (checked in the tests).

**The stance:** feet apart, right foot forward, weight down, the blade held low and ready. It breathes and sways, the blade drifts, and
every eight seconds or so it turns the blade over on the wrist and back. While it circles, its legs walk.

### Other

- The CCG ghoul division's arm blades are the new blade too.
- Protocol 121.
- Previews: `docs/handoff/arm-blade-4.1.47.png`, `arm-blade-moves-4.1.47.png`, `arm-blade-colours-4.1.47.png`
  (`tools/BladeArmShowcase.java`, `BladeArmMoves.java`, `BladeArmColours.java`).
