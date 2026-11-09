## 3.6.4 (HexGhoul): Fix: /kagune cooldowns off ends the Bikaku cooldowns

HexKagune 3.6.4 for Forge 1.20.1. Not tested in game.

Your words, on `/kagune cooldowns off` not ending the Bikaku cooldowns already running: "it should."

- **`/kagune cooldowns off` now ends every Bikaku cooldown that is running:**
  - each move's;
  - the kakuja's;
  - Grand Devouring's (the menu's seconds clear too).
- **Before:** new Bikaku cooldowns were already skipped while cooldowns were off, but the ones running when you typed it kept going. The Bikaku path's reset was written but never called.
- **Creative:** switching to creative now ends them too, as it does every other path's.
- **Unchanged:** Autonomous Predation's pace (how often the kakuja's tentacles bite on their own) is not a cooldown and stays as it was.
- Network: protocol 103 (unchanged).
- Tests: BikakuTests (1,649). A new check reads the sources: every path that keeps cooldowns of its own must be ended by `/kagune cooldowns off`, and the Bikaku path's reset ends its moves', the kakuja's and Grand Devouring's. It finds all seven paths and would have failed before this fix. Also the full suite of every path.
- **Next:** the view from inside the mouth (GeckoLib), as you approved: "I like the inside of the mouth, proceed wit hthe mod".

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 103. Not tested in game.
