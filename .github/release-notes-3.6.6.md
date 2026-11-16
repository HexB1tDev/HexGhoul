## 3.6.6 (HexGhoul): Fix: no RC bar

HexKagune 3.6.6 for Forge 1.20.1. Not tested in game.

Your words: "The RC bar does not exist and is not meant to be a thing anymore, the energy and FOOD bar are the same, RC bar IS NOT A THING"

- **The RC bar is never drawn now.** There is one bar: energy and food together, the flesh reserve where the food bar was.
- **Before:** the original mod's RC bar (a thin bar marked "RC" above the hotbar) was still drawn in two cases, whenever your kagune was out:
  - in creative;
  - on a server with the ghoul diet switched off.
- **Unchanged:** the one bar (energy and food), and what everything costs.
- **Note:** on a server with the diet switched off there is now no energy bar on screen at all; energy is still spent and recovered as before. Tell me if that case should show something.
- Network: protocol 103 (unchanged).
- Tests: GhoulMeatTests (8,027). A new check reads the sources: the RC bar's hook draws nothing, and nothing in the mod draws the original RC bar. Also the full suite.
- **Next:** Potential Unlock's first part, with the one bar (energy and food) turning purple.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 103. Not tested in game.
