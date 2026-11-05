## 2.5.0 (HexGhoul): Final sweep: a new world never inherits the last one's timers; favours survive death; protocol 86

HexKagune 2.5.0 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Final sweep for multiplayer and single player. Quitting a world and opening another in the same game session no longer carries over the last world's timers (ability cooldowns, kagune locks, "presumed dead", a companion's recall home, villagers coming up to you, scene and talk cooldowns, a Thy Death or finger crack under way), which could otherwise lock things in the new world for hours. A villager's favour now survives your death, letting go of Z while disconnecting no longer errors, and the network protocol is bumped so a client and server on different builds are refused cleanly.

- `WorldTimes`: every server map of game times (32 of them, in 16 classes) is made through `WorldTimes.map()`, and Thy Death's state through `WorldTimes.kept()`; all are cleared on Forge's `ServerStoppedEvent` (after the final save, so nothing saved at logout is touched). They all start empty when the game launches anyway, so clearing them gives exactly a fresh launch's state. `ControlTests` fails the build if a new timer map bypasses it. New allowlisted API: `ServerStoppedEvent` (with its compile stub).
- Belt and braces: Thy Death never stays held further ahead than one ping gives; a finger crack whose start lies in the future is dropped.
- `HumanErrandServer`: the favour is kept under Forge's `PlayerPersisted` tag (carried over on respawn); one kept the old way is moved in.
- `ThyDeathClient`: when the player leaves the world while holding Z, nothing is sent (there's no connection); the server lets go by itself.
- Protocol 86: 2.4.8 added `S2CUkakuFx.CRACK` and 2.4.9 new `EtoGesture` ordinals without a bump.
- Checked and fine: the dedicated server loads every class with no client code reached; every external reference is verified; every event listener is guarded; packet handlers run on the main thread; Thy Death input is validated on the server; the new commands (marry, quests complete all) need operator rights; effects reach everyone tracking the player; no banned names; sounds only from the mod's pack.
