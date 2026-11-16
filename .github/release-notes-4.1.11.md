## 4.1.11 (HexGhoul): the wings lash out as far as they hit

HexKagune 4.1.11 for Forge 1.20.1. Not tested in game: previewed off-game only (`docs/handoff/ukaku-slams-4.1.11.png` and `.gif`).

Your words: "You can make the wing stretch further forward, because it currently just feels like a little chicken wings, bro. Whenever they stretch to slap something, they can stretch to be larger in size and length as well. You don't have to be limited by such small range."

### Why they felt like chicken wings

- The slams already hit far: Rinkaku's range × 0.85, about **8.3 blocks at mastery 300** and **12.8 at mastery 1000**.
- But the wing was drawn about 3 blocks long and never changed size, so you hit things far away with a wing that stayed at your shoulder.

### Now every slam lashes out

- As the blow drives, the striking wing **grows bigger and longer** until its tip reaches **exactly as far as the slam hits** at the moment it lands.
- It holds that length as it carries through, then **pulls back in** to its own size as it recoils.
- While it's out, its feathers fold in tight, so it reads as a **long tongue of fire** rather than a giant sheet. It flicks open a little on the hit.
- Its tip is aimed **ahead at the target's height**, where the blow sweeps, instead of up into the sky.
  - Slaps, swings and overhead slams lash level and forward.
  - The rising wing lashes upward.
- The twin slam lashes both wings out together.

### Fixed from 4.1.10

- The whip's spring was too slow for a fast blow: on a slap, the tip was still out to your side when the hit landed.
- The spring now keeps pace with each blow, so the tip trails by a beat and lands on the target.

### Limits

- The smallest wings (Rabbit Guise, mastery under 100, and the first twin wings) stretch at most 3.2× their length, so they stop a little short of the hit instead of becoming a thread.
- Reach mutations aren't counted in how far the wing stretches; the hit still counts them.

### Please check in game

- In **first person** your own wings are drawn at 45% opacity, so a lash crosses your view as a quick see-through streak. Tell me if it gets in the way.

### Unchanged

- Damage, reach, the server's timing and clashes are as before: only how it looks changed.
- Protocol 116 (unchanged).
