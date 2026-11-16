## 3.5.3 (HexGhoul): Bikaku: Monstrous Regeneration (C) and the passives

HexKagune 3.5.3 for Forge 1.20.1. Not tested in game.

The rest of your Bikaku spec before the kakuja, opened by mastery alone.

- **Monstrous Regeneration (C, mastery 600).** For 8 seconds your body regenerates far faster than any ghoul's: half a heart every quarter second (16 hearts in all, while you have room). Strands of your kagune's tissue crawl over your body while it lasts (your head, torso, upper arms and thighs), winding round you and knitting you back, their growing ends glowing, in your kagune colour; they wither as it ends. It does not make you invincible (it takes nothing off a blow), and it cools down for 45 s. 15 RC. The worm keeps biting and moving meanwhile, and it goes on even if you put the worm away.
- **Abnormal Regeneration (passive, while the worm is out).** Your ghoul regeneration starts after 40% of its usual wait and heals in half its usual time.
- **Pain Suppression (passive, while the worm is out).** Hard to knock back (60% knockback resistance added), and every stun on you lasts half as long.
- **Predatory Feeding** was already in (fixed in 3.5.2).
- The move guide shows Monstrous Regeneration on C and the passives; the menu lists them.

Preview (off-game, the strands placed on the body exactly as the game places them): `docs/handoff/bikaku-regeneration-3.5.3.png` (top: with the worm; bottom: from behind, the worm left out so the strands show).

- **My guesses:** the heal (32 health over 8 s), the cooldown and RC, the passives' numbers, how many strands and how thick.
- **Approximated:** the strands follow your body as the mod poses it, but not Minecraft's own walking swing of the arms and legs, so on a limb they stay near the shoulder or hip, where that swing hardly moves it. In your own first person they aren't drawn (your body isn't).
- **Still to come:** the kakuja (its body and transformation, then its abilities).
- Network: protocol 96 (the state carries when Monstrous Regeneration began). No new Minecraft API.
- Tests: BikakuTests (C at 600, alongside the worm, its total heal and long cooldown, the passives' shares; every strand just off its part's skin, near the joint on a limb, many while it lasts, none after).

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 96. Not tested in game.
