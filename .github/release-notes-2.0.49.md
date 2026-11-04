## 2.0.49 (HexGhoul): the White Reaper fights as a tactician

HexKagune 2.0.49 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

The White Reaper fights as a tactician. Each fight he reads his opponent and picks a play ahead of the moves every investigator has: a backflip launched as Up We Go launches one, firing the volley from mid-air; closing in on openings; vaulting over a guard; shooting a target out of the air.

- **Soar**: a backflip built up over five ticks, climbing about 5 blocks (two and a half times a flip) and carrying him about 5 back. He's turned over by the apex, hangs there aiming Dawn down for 6 ticks, then lets the five-ball volley go (a single ball if very close) and comes down without fall damage. He soars when his string has just landed (60%), when he's below 40% health with someone within 5 blocks, and now and then (5% of his reads) from 3.5 to 9 blocks to keep you guessing. Once every 5 s at most, and never with a wall at his back or a ceiling over him.
- **Punish**: an opening within 12 blocks: you're stunned, you just swung and missed, you're in the air, or you're below 30% health (within 10 blocks). He goes straight in (a front flip from past 6 blocks, zigzag dashes from nearer) and starts his string the moment he's in reach.
- **Vault**: you're guarding in front of him (a Rinkaku block facing him, or a raised shield) within 1.2 to 6 blocks. He front-flips clean over you, lands about 2 blocks behind the guard, and strikes at once.
- **Sky shot**: you're in the air 5 to 22 blocks off (a jump, or launched by his kick). A quick 6-tick aim and a shot where you can't step aside (the volley from 7 blocks or more), ready again every 2.3 s.
- **Leading**: every shot of his is aimed ahead along the way you're moving (60% of the ball's flight time), read from how far you moved last tick.
- Pure rules in `npc/ReaperTactics` (the read, the plays, the soar's launch, the lead), tested in `AgentTests`, including the soar's flight simulated as a mob flies. `AgentBrain` reads the fight every other tick (at once after his string lands). The soar flies before everything else, and his shot is one `release` for every kind. Clients draw the soar as its own long flip (`CcgAgent.SOARING` added to the flip's number, `AgentPose.soar`), with no running legs while he drifts down. Protocol 80.
- Preview: `docs/handoff/reaper-soar-2.0.49.png` (a strobe from the side, in his skin: the soar, the hanging aim, and the volley closing on the target). New `tools/SoarPreview.java`.
