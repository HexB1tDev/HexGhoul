## 3.0.5 (HexGhoul): Rinkaku's tails stop shaking while holding someone

HexKagune 3.0.5 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Rinkaku's tails stop shaking while one of them holds someone. Not tested in game. This is the cause found in the code, so tell me if any shaking is left.

- **The cause:** the calm rest shape the tails are drawn with (2.0.51) applied only while the kagune was idle. While holding (and lifting or crushing), the mode isn't idle, so every tail not holding anyone fell back on the original mod's own non-idle rest: a coiled, constantly twitching motion, kept at full strength and followed at strike speed.
- **Now:**
  - **The other tails** (the ones holding nobody) rest during a hold, lift or crush, exactly as at peace (`RinkakuIdleClient`, `StrikePaths.holding`).
  - **The holding tail** is followed calmly, keeping a third of the original's sway; its end is still wrapped round the held body by the original's code.
