## 3.7.2 (HexGhoul): Bikaku: absorbing a body gives Speed and the food's strength buffs for 15 s

HexKagune 3.7.2 for Forge 1.20.1. Not tested in game.

Your words: "Whenever an entity is absorbed, the player gains a speed buff for 15 seconds and some of our custom effects from foods, any that give special strength and buffs also for 15 seconds". Asked which moments and which effects, you chose "Grand Devouring only (Recommended)" and "Strength, Resistance, Refreshed, Replenished (Recommended)".

- **When a body Grand Devouring swallowed is in you** (the bulge reaches your back and your bar refills), you get, for 15 seconds, at level I:
  - Speed;
  - Strength;
  - Resistance;
  - Refreshed (your energy flows back on its own, a little every second);
  - Replenished (wounds close slowly on their own, at no cost to the flesh reserve).
- They are the same effects the mod's food gives, found by the same keys.
- If you already have a longer or stronger one, Minecraft keeps the better one.
- Nothing else changed about Grand Devouring.
- **My guesses:** level I for all five; they come at the moment the bar refills (the end of digestion), not when the body is swallowed.
- Network: protocol 103 (unchanged).
- Tests: BikakuTests `absorbed()`: exactly those five, for 15 seconds; each one an effect our food gives; given in `fed` and nowhere else (read from the source). Also the full suite.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 103. Not tested in game.
