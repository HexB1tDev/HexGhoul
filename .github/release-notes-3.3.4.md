## 3.3.4 (HexGhoul): One partner, for good

HexKagune 3.3.4 for Forge 1.20.1. Not tested in game.

The owner (asked this session about break-ups and divorce, it changed the plan): *"From now on, you can only have one wife, no matter what, okay? ... There's no divorce button. No anything. When you're married to them, that's it. The only way you can remarry is if you kill them. But if it's an official NPC like Toka, Ken, or Ito, there's no way to unmarry or to anything. You guys are stuck forever."*

So there's **no break-up and no divorce**, and the break-up work that was planned is dropped.

**What changed.**

- **One partner at a time, across everyone.** Until now you could date Eto, Touka and Ken all at once (only a second *marriage* was refused), and the village's partners didn't count at all. Now, while you have a partner (dating or married, a companion or a villager), **nobody else will start anything with you**:
  - Eto: *"Go out with you? You're someone else's. That doesn't come undone, not even by me. Go home."*
  - Touka: *"You're taken. Go home to them."*
  - Ken: *"You're with someone. I can't do that to them. ...Or to you."*
  - a villager: *"You're already with someone. I won't be the other one."* (and three other ways of saying no).
- **No ring while you're married to anyone**, a companion or a villager.
- **Eto, Touka and Ken: stuck forever.** Killing your partner used to end it. Now it doesn't. They come back (as they always did), and **you're still bound to them**: *"You killed her. ...She'll come back. You're still bound to her. That doesn't end."* They will remember it, as before.
- **A villager partner: only death ends it.** When your villager partner dies, you're free again (*"... is dead. You're free to marry again."*, and *"by your hand"* if it was you). It's the only way to marry someone else.
- **Children are unaffected.**

**Old saves.** If a save already has more than one partner from before this version, they're all kept (nothing is taken away), but nobody new can be added. The 3.1.9 scenes between co-wives only happen in such saves now.

- No network changes (protocol 92). No new Minecraft API.
- Tests (CompanionTests): nobody new while you have a partner; a companion's death never ends it and a villager's does; a villager's bond is told from a companion's; asked out while someone else's, Eto, Touka and Ken each say no in their own words, without calling you married.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 92. Not tested in game.
