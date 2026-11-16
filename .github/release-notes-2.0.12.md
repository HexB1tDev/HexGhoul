## 2.0.12 (HexGhoul): clashes shake the screen
HexKagune 2.0.12 for Forge 1.20.1. Install on client and server (**protocol 73**, unchanged). Built and tested by CI. **Not yet tried in game**: please report anything that looks or feels wrong.

### From your report

- **"I dont think clashes are shaking the screen at all":** they barely did.
  - **Every clash** gave a 1.6° nod of the view that was gone in about a quarter of a second.
  - **Only the hardest clashes** added a ground rumble of 1–2°.
- **Now every clash shakes the view hard.** It's an uneven shudder up and down, side to side and a little left and right, dying away. As you asked, it's stronger again than the first version:
  - **An ordinary clash:** about 6° at its peak, for about a second.
  - **The hardest clashes:** about 10°, for nearly a second and a half.
  - **Who feels it:** everyone within about 30 blocks, harder the nearer you are. The two fighters feel it most.
- The old nod and the ground rumble still come with it.
- **Toning it down:** the client settings still apply. `cameraEffects` turns all camera shake off, and `cameraEffectStrength` (0 to 1.5) scales it.
