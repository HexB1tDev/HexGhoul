## 3.4.6 (HexGhoul): A smaller move guide

HexKagune 3.4.6 for Forge 1.20.1. Not tested in game.

Your ask: *"it's time to redo the GUI. This looks super, super clanky. As in, like, it's too big. We need to summarize things a bit more."*

The move guide above the chat (every path's board, and War's and Peace's) is redone:

- **Every move in a word or two.** "Fists: chain · Space held: launch" is "Fists". "Heavy · hold: charge · after a blow: ender" is "Heavy". "Thy Death (hold: every tail hunts on its own)" is "Thy Death". "Kakugan mode (eyes stay open)" is "Eyes only", and "Put it all away" is "Put away". "Juggle, ends in a spike" is "Air juggle". "(Ken teaches it)" is "(Ken)".
- **Two columns** once a board has more than five moves (down the first, then the second), so it's half as many lines.
- **The title and the form on one line** ("WAR  final kakuja"), not two.
- **A bit smaller on screen** (70% of its size instead of 75%).

Nothing is lost from what the board does: every move is still there on the key you bound, in the same order and colour. Moves you haven't reached are still dim with the mastery they need, and War's and Peace's still show their cooldowns. Full Kakuja's board went from 13 lines to 6, and War's from 12 to 6. Side by side (old left, new right): `docs/handoff/move-guide-3.4.6.png`.

- **Approximated:** what each move is shortened to is my choice. The longer explanations ("Space held: launch", "on time: stagger") aren't shown on the board any more. Tell me if one should come back, or if you want it smaller still.
- No network change (protocol 93). No new Minecraft API.
- Tests (ControlTests): every board of every path, form, state and mastery, in brief, keeps every move with its keys, order, colour and mastery; every name is 20 letters or fewer; past five moves it's two columns; the briefs read right ("Aegis of :Re" stays whole).

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 93. Not tested in game.
