HexKagune 6.0.10 for Forge 1.20.1 (needs PlayerAnimator, bendy-lib and Photon on clients; the server needs only Forge). Install on both the client and the server.

**Fixed in 6.0.10**
- Fixed Ayato's Rising Flame (and two other fire moves) kicking the player off the server with "Internal server error" when they hit something.
- The mod can no longer kick a player or crash the server. Every place Minecraft calls into the mod is now protected: any error inside is written to the log and the game carries on.
- Includes everything up to 6.0.9.
