## 4.1.99 (HexGhoul): /kagune lag, the mod's own lag profiler (the lag patch, part 1)

HexKagune 4.1.99 for Forge 1.20.1. Not tested in game. Protocol 128 (a new message: everyone on a server needs this jar).

You said (quoted exactly): "make handoff mentioning a massive long, lag patch, making sure absolutely nothing in the mod fails and nothing
ever lags."

This is the first part: a way to see exactly which part of the mod costs time in your game, so the next versions fix what really lags.

**`/kagune lag`** (anyone can run it, no operator needed): for 30 seconds the mod times every one of its own parts, on the server and in
your game at the same time. Keep playing where it lags (a fight, the CCG tower, the hideout, the hotel, a big gang). Then your chat gets
two reports, the server's and your game's:
- the first line: how long a server tick takes on average and at worst (a tick has 50 ms), or your frames and fps, and how much of that is
  the mod's;
- the ten costliest parts of the mod, each with its milliseconds per second, its calls per second and its slowest single call;
- the packets the mod sent per second (the busiest three);
- on the server, the world generation threads (the structures being placed, /locate).

**`/kagune lag log`**: the same every minute, written to `logs/latest.log` (the server's and your game's) until you run it again.

Please run it where you feel lag and send me the lines (or the "HexKagune lag:" lines of the log): that is what the rest of the lag patch
will go after first.

**How it works:** the build already wraps every way the game calls into the mod (every event listener) so an error is caught and logged.
Each of those now also tells a timer when it starts and ends, and so do the NPCs' brains (investigators, townspeople, companions and
officials, children, flesh creatures, wild ghouls), every renderer and render layer of the mod, the structures' searches and placing, and
every packet sent and handled. A part's own time leaves out the timed parts it called, so nothing is counted twice. While nobody measures,
all this costs one check per call.
