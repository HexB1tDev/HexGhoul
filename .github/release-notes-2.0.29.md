## 2.0.29 (HexGhoul): /kagune reaper, and the Reaper turns up once in a while

HexKagune 2.0.29 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

`/kagune reaper` (operators) puts the White Reaper five blocks ahead of you for testing; in the wild he now turns up about once in a hundred in-game days instead of about once in 660.

- Never two: a Reaper already loaded anywhere is sent away first, and the command takes over his slot. One standing in unloaded ground gives the slot up; when that ground loads, he wakes as an ordinary Rank 9 investigator with a regular quinque.
- Natural spawns: each appearance of investigators (about one a day near each player) has a 1% chance to be the White Reaper alone (`AgentRules.REAPER_CHANCE`). Before, he only came from Rank 10's 0.15 weight in the rank table. He stays rarer than a Rank 9. Protocol stays 76.
