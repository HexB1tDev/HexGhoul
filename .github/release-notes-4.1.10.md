## 4.1.10 (HexGhoul): the Ukaku's wing slams thrown like real blows

HexKagune 4.1.10 for Forge 1.20.1. Not tested in game: previewed off-game only (the sheets and the GIF in `docs/handoff/ukaku-slams-*`).

Your words: "put all your focus into the combat rework, and kagune rework for the ukaku, this model is very expensive and I need you to focus on what's hard, study complex fighting moves, and combos and make it look smooth and beautiful."

### What was wrong

- Every wing slam (and the body throwing it) **slowed down as it landed**, then held still. A blow should land at its fastest.
- The wing swung as one stiff paddle: its tip was locked to its root.

### Every slam now has four beats

1. **Chamber:** the wing is cocked back, then loads a touch further: the coil before the blow.
2. **Drive:** it speeds up all the way into the hit and lands at full speed, on the same tick the hit lands.
3. **Overshoot:** it carries on past the target, slowing down.
4. **Recoil:** it bounces back off the hit a little, then settles to rest.

This applies to all seven slams of the string (Right/Left Wing Slap, Right Wing Swing, Left Rising Wing, Right/Left Overhead Slam, Twin Slam) and to Rabbit Guise's one wing.

### What makes it look alive

- **The whip:** the wing bends along its length. Its tip trails behind as it drives, then snaps on past the root after the hit, like a whip cracking.
- **The feathers:** tucked in tight as it drives (a blade cutting the air), thrown open on the hit.
- **The fire:** streams back off the wing through the drive.
- **The other wing:** pulls back while one strikes, like a fighter's other hand pulling back to the hip.
- **The body leads:** your body lands its part of the blow a beat before the wing, hips and chest first, the arm and the wing whipped round after.
- **The slap now follows through** across the front, as a slap does.

### The guard (right click)

- The wings flare open first, slam shut a little past where they hold, then ease back to it.

### Unchanged

- Damage, reach, timing on the server, clashes and the guard's rules are all as before. This is how it looks, not how it hits.
- Crystal mode is untouched.
- Protocol 116 (unchanged).
