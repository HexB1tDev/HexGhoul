## 2.1.9 (HexGhoul): Peace and War break the nerve of whoever faces them

HexKagune 2.1.9 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Rinkaku's final kakuja (Peace or War) now breaks the nerve of whoever faces it. Doves of Rank 0 to 4 and the two weaker tiers of wild ghouls run from it, and keep running while it's in sight. Rank 5 to 7 Doves and the strongest wild ghouls stand and fight, but tremble the whole time, and every few seconds their nerve gives and they back off before coming again. Rank 8 and above (Special Class, the White Reaper) stand as before.

- **Runners**: they drop their weapon, flee to about 26 blocks and stop to look back, and run again if you come closer. It's kept up twice a second while you're in sight within 32 blocks.
- **Shakers**: a fast, small, uneven tremble of the whole body, drawn on every client. About every 4.5 seconds (each at its own moment) they back off for a bit under 2 seconds, then come at you again.
- **War** used to send every wild ghoul running. Now the strongest tier shakes instead. The Dragon still sends everyone running, and Doves still freeze ("falter") on first sight as before.
- **Code**: `FinalSight.Nerve` and its rules (pure, tested in `AgentTests.dread`); `Sightings.dread`; the investigators' `routed` state; the shakers' breaks in `AgentBrain` and `GhoulNpcBrain`; a synced `AFRAID` on `CcgAgent` and `GhoulNpc` for the tremble. Protocol 82: install this version on both client and server. Not tested in game.
