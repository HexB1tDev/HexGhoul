## 3.0.1 (HexGhoul): CCG weapons re-animated (footwork, no folding) and the greatsword, spear and hammer remodelled

HexKagune 3.0.1 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

The CCG weapons, redone after 3.0.0 shipped with animations that folded the body at the hips and a greatsword that looked like a strip of paper. Every blow and move of all four weapons is re-animated, and the sword, spear and hammer have new models. Not tested in game.

- **Animations, every blow and every move.** Each blow is now seven stages: anticipation, windup, the blow quickening, impact, follow-through, settling, recovery. The keys are timed so impact is the moment the server lands the hit.
- **Footwork.** Every strike takes a real stride: the front foot goes out, the back foot stays planted, and the body is carried over it. The hips drop only as far as the stride lowers them, so feet no longer sink into the ground. Windups rock the weight back onto the back foot, so a thrust surges forward through the stride.
- **Body.** Lean is capped at about 15 degrees, so nothing folds at the hips any more; heavy blows reach the ground with the arms instead. The chest twists against the hips through each swing, the body leans sideways into wide sweeps, and spins are whole-body turns. The head stays on the foe.
- **Arms and chaining.** The whip's free arm swings against each lash. Consecutive blows and moves now blend into each other over 3 ticks instead of snapping back to the stance between them.
- **Whip.** Every lash arc is re-timed so the tendril points at the foe exactly when the blow lands (the forehand used to be 55 degrees past them).
- **Bone Greatsword, new model.** A broad, heavy blade with real thickness and a faceted cross-section: a honed pale edge, bevels, a fuller on each face with a cord of living flesh lying in it, and a thick saw-backed spine. The blade is made of bone plates with chevron seams. A hooked notch at its foot, a clipped point, a cross-guard of vertebrae ending in claws round a flesh knot with an eye on each face, a two-handed grip wound in sinew, and a bone pommel.
- **Crystal Spear, new model.** A thicker jointed bone shaft with sinew-wound grips and crystal shards breaking out of the upper joints. Bone claws grip a broad faceted ruby crystal blade (it was a small pink tuft), with swept-back wings, barbs and a crystal butt spike.
- **Flesh Hammer.** Dark Koukaku armour bands and a plate over each striking face, with the flesh core and its eyes bulging between them, plus two curved bone horns out of the crown.
- **Previews:** `docs/handoff/*-moves-3.0.1.png` show every blow and both moves of each weapon frame by frame. The fifth frame of a blow is where it lands. `arms-3.0.1.png` and the per-weapon images show the new models.
