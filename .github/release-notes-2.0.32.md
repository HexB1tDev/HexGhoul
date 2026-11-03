## 2.0.32 (HexGhoul): Dawn's lightning in a player's hand

HexKagune 2.0.32 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

A player holding Dawn fires its yellow lightning ball with right click: the point opens, the ball gathers there for a quarter of a second while the lance comes up onto your aim, then it flies and explodes on impact with the same lightning effects as the White Reaper's. Shift + right click folds Dawn back into its case.

- A player's ball: 16 at its heart (the Reaper's is 26), its blast falling off over 2.5 blocks in proportion. It travels 1.5 blocks a tick for up to 32 blocks, pushes back what it catches, and never burns or breaks blocks. One every 2 seconds; tapping sooner shows the time left on the action bar.
- The point opens (2.0.30's `dawn_fire` frame, its two halves parted about a yellow light) while the ball gathers and for 6 ticks after it leaves, for players and for the White Reaper as he aims. Every client sees it: the times ride on the weapon stack.
- Getting Dawn: the White Reaper now leaves his black case when a player kills him, and Dawn is in the creative Combat tab.
- No new Minecraft API: the cooldown is kept on the server, not vanilla's item cooldown. Previewed off-game: `docs/handoff/dawn-aim-2.0.32.png` (`tools/QuinquePreview.java <out> dawn aim`). Protocol stays 76.
