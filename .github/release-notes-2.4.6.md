## 2.4.6 (HexGhoul): Ukaku's Vortex: a slow, massive rolling blow

HexKagune 2.4.6 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Ukaku's Vortex reworked to match the reference: a slow, massive, twisting blow instead of a fast horizontal tornado. The wing still leaves the shoulder blade, but now it rears up and comes down ahead, then rolls on along the ground in great upright loops, like the black mass in the reference, curling over each loop's top. The loops grow in as it lengthens, so it starts as a rising arch.

- **Looks**: a solid, near-black maroon mass with burning pink edges (the reference's colours), its flame tongues broad enough to read as one mass. It turns slowly (0.9 turns a second, was 3.2). It's thicker, with the white-hot point and the aura toned down. The loops stand up to about 3 blocks high.
- **Slow and heavy**: it rolls about 12 blocks a second, so it takes around 1.7 seconds to cover its 20 blocks (it used to cross 25 blocks in under a second). It catches everything within 2.1 blocks of its line, 2.6 from mastery 500 (was 1.2 and 1.55). Each body it catches takes 2.2× a melee blow (was 1.5×), and it's thrown up and over (lift 0.95, was 0.55).
- The cooldown still never lets you throw a wing that hasn't grown back.
- **Preview**, rendered off-game: `docs/handoff/ukaku-vortex-2.4.6.png`.
- **Code**: `WingVortex` (`Coil`: the rolling line, a prolate cycloid in the upright plane of its flight; colours; weight) and `UkakuRules` (speed, range, radius, damage, throw). Tested in `UkakuTests`. Protocol stays 85. Not tested in game.
