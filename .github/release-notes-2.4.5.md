## 2.4.5 (HexGhoul): Rinkaku's Thy Death: every tail hunts on its own

HexKagune 2.4.5 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Rinkaku's Rapid Strike is now **Thy Death**. Hold Z and every tail you have out hunts on its own.

- **How it works**: while Z is held, each tail picks its own foe within reach (7 blocks, up to 10 at mastery 1000). Where there are enough foes, each tail takes a different one. It lunges at its foe from its own side, the point going through it, then pulls back and goes again. The tails are staggered so they never strike as one. Let go of Z and they all come back.
- **Damage**: 4 per lunge at mastery 0, up to 10 at 1000. Each tail lunges about every 0.7 seconds, so four tails out land about six blows a second spread over the crowd. It also feeds your mastery.
- **What it goes for**: monsters, anything after you or that hurt you, CCG investigators, and wild ghouls that aren't yours. Never your companions, your children, villagers who haven't hurt you, animals, or other players who haven't hit you.
- **Looks**: drawn on the Rinkaku tendrils as they are, a lunge bent over their own motion, going round your own side of the body as every strike now does.
- Unlocked at the same mastery as Rapid Strike (150). The ability guide and the Rinkaku menu call it Thy Death.
- **Code**: `ThyDeath` (pure, tested in `ControlTests.thyDeath`), `ThyDeathServer`, `ThyDeathClient`, `S2CThyDeath` (message 49), `C2SUkakuAction.THY_DEATH`; drawn in `StrikePaths`. **Protocol "85"**: install on both client and server. Not tested in game.
