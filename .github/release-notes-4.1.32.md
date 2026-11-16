## 4.1.32 (HexGhoul): a quinque's M1 comes first

HexKagune 4.1.32 for Forge 1.20.1. Not tested in game. No pose or model changed (nothing to render): this is which M1 wins.

Your words: "whenever using a Quinque, I think that it should be prioritized over any combat that we got in the game. So basically, if you
got your Kakugan activated, but you got a Quinque activated, like your M1s are gonna be ignored. You know what I mean? Like the M1s of the
Kakugan are gonna be ignored, and be you're gonna be using the one from the Quinques."

**What it was:** with your kagune out, the original mod's own click handler cancelled the game's attack and threw a kagune swipe instead, so
a quinque in your hand never landed its blow. (The eye-mode styles, War and Peace, Bikaku and the kagune-made weapons already gave way.)

**Now:** with a CCG quinque out in your main hand, M1 is always the quinque's blow, its own string and finisher, whatever is active:
- it is heard before the kagune's handler and lands the hit the way the game does, so the kagune's swipe never starts;
- on the server, no kagune swipe starts from any M1 path while a quinque or a kagune-made weapon is out;
- a block is still dug as ever.

Put the quinque away (or hold its case) and your kagune's M1 is back.

### Other

- Protocol 117 (unchanged).
- API: `MultiPlayerGameMode.attack` and `Minecraft.gameMode` stubbed and allowlisted (joined.tsrg and intermediary evidence).
