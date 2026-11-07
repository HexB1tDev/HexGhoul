## 3.1.5 (HexGhoul): Rinkaku's Impale on the Crush key

HexKagune 3.1.5 for Forge 1.20.1. Not tested in game.

- **H (Crush) with nobody held is now the Impale.** With someone held by a tail, H is still the crush, exactly as before. The original's "no target" message on an empty crush is gone.
- **One random tail goes**: never a centipede leg, never a tail already busy. It works in every form whose tails are yours: Rinkaku Seed, Twin, Four-Tail and the final kakuja (its tentacles, not its centipede legs). Not in the first kakuja, whose legs fight on their own and where no move key works.
- **It rises out of the back, over its own shoulder, and shoots straight out where you look**, its end drawn to a sharp spear point (the tail's mesh narrows over its last 1.5 blocks; it is drawn exactly as long as it goes, so the point is where it strikes).
- **Held: the longer you hold, the further it goes.** 2.1 blocks a tick (the whole reach in well under a second). Reach 16 blocks at mastery 0, 30 at 1000 (a grab reaches 8.5 to 15). A tap still runs it out 6 blocks. It follows your look like a heavy spear (9 degrees a tick at most).
- **Whatever its point runs through is impaled and dragged** along on it, mobs and players alike (whoever the M1 may hit: never your allies, owned pets or partner, and never a creative player). Up to 5 at once, the newest nearest the point. It hits for 1.5x the tail's swipe damage and bleeds them as the centipede legs do. Carried bodies are moved only where they fit (never into a wall); one held back by the ground or a ledge tears free.
- **A wall stops it**: anyone it carries is pinned there (0.6x swipe damage more, a bone crack).
- **Let go** (after a 3-tick beat at its end), or after 3.5 seconds out, it tears back out of everyone (0.4x swipe damage, more bleeding) and they drop where they are. The tail comes home in 6 ticks.
- Costs 1.4 RC; 36 ticks of cooldown (the original's cooldown scaling applies). Unlocks with the crush (mastery 350). It waits for Thy Death and for the block, and neither starts while it is out.
- Ability guide: H reads "Impale (hold) · Crush what a tail holds".
- Network: C2SUkakuAction IMPALE (10) and a new S2CImpale (message 52). **Protocol 90**: client and server must both have 3.1.5.
- Pure rules in `RinkakuImpale`; 147 new checks in ControlTests (reach, tap and hold, return, carrying order, tail choice, turning, hit test, the drawn path, the sharpened mesh, the guide). Offline preview with the game's own mesh: `docs/handoff/rinkaku-impale-3.1.5.png` (`tools/ImpalePreview.java`). No new Minecraft API.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 90. Not tested in game.
