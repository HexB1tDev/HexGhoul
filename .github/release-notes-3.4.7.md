## 3.4.7 (HexGhoul): Fix: the move guide is one narrow column again

HexKagune 3.4.7 for Forge 1.20.1. Not tested in game.

Your report (with a screenshot of War's board): *"I said the menu was cluttered and your solution is to make it like even bigger on the screen ... it should be vertical, and you made it horizontal, bro. No one wants the menu like this."*

You're right: 3.4.6's two columns made the board wider, nearly half the screen. Now:

- **One column, top to bottom**, on every board (each path's, War's and Peace's). Every move is on its own line under the last.
- **Smaller on screen:** 60% of its size, down from 3.4.6's 70% (and 75% before that). The padding and the gaps between key and name are tighter too.
- **Kept from 3.4.6:** every move in a word or two, and the title and form on one line.

On War's board it's now under half as wide as 3.4.6's. It's a bit taller, since the moves stack instead of sitting side by side. Old and new side by side: `docs/handoff/move-guide-3.4.7.png` (an off-game preview, not the game's own font).

- **Approximated:** the size (60%) is my choice. Tell me if it should be smaller or larger.
- No network change (protocol 93). No new Minecraft API.
- Tests (ControlTests): every board of every path, form, state and mastery is one column with every move on its own line, and the scale is 60% or less.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 93. Not tested in game.
