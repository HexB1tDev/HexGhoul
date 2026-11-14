## 4.1.103 (HexGhoul): the no-fail audit: the ways a bug could still crash the game or the server, closed (the lag patch, part 5)

HexKagune 4.1.103 for Forge 1.20.1. Not tested in game. Protocol unchanged (128).

You said (quoted exactly): "make handoff mentioning a massive long, lag patch, making sure absolutely nothing in the mod fails and nothing
ever lags."

Since 6.0.10 every event listener of the mod is wrapped so an error is logged and the game goes on. This audit looked for every other way
the game calls into the mod, and found these still open:

**The server:**
- The investigators' and the townspeople's minds (run every tick) and what they drop when they die had no catch at all: one bug there was
  a "Ticking entity" crash of the whole server. Now logged, and the game goes on.
- The companions', the officials' and the children's minds caught ordinary errors only (not the rarer kinds); a child's push had none.
  Now all of them. An official, a companion or the White Reaper is never removed because of a bug (a broken flesh creature is, as a broken
  wild ghoul always was).

**Your game:**
- Every screen of the mod (the quest menu, the talk screen, each kagune's menu, the cookbook, the choices...): an error while drawing it
  crashed the game. Now logged, and the screen keeps going.
- Every NPC's renderer (wild ghouls, investigators, townspeople, companions and officials, children): an error while drawing one crashed
  the game ("Rendering entity in world"). Now that NPC is skipped for that frame, what the renderer had pushed of the pose stack is put
  back, the error is told once in the log, and the world is drawn on.

**The NPC list (4.1.100):** a body whose arrival another mod cancels (a mob limit) is never on it, and each one it finds is checked to be
really in its world.

**The build itself:** the tool that wraps all this rewrites the mod's bytecode, and its own check never read the parts the Java machine
checks as it loads a class. Now the build loads every rewritten class (320) in the Java machine itself and fails if any would be refused:
a mistake there could otherwise only show up as a crash in your game. Proved by breaking it on purpose: only this new check caught it.

**Looked at and already safe:** the saved data the mod reads (families, children, homes, the kitchen, quests, places, the officials'
records) drops what it can't read; a command that fails shows the game's own error line. Left as the game has it: a malformed packet
(only a modified game sends one) disconnects that player.
