## 2.2.7 (HexGhoul): a command to marry a companion outright

HexKagune 2.2.7 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

New operator command: `/kagune eto marry`, `/kagune touka marry` and `/kagune ken marry` (with an optional player after it, like `max`). It marries you to that companion at once and fills everything about the relationship.

- **What it sets**:
  - married, the same as a ring taken (the "Married to ..." tag, the Romance card, a spouse at home)
  - regard 100, met and befriended
  - every chapter of their courtship closed
  - dating since and wed today (earlier dates are kept)
- **What it clears**: nothing held against you any more: no sulk, grudge, offences, being ignored, jealousy, or make-up scene owed.
- **If they're loaded**: they're giddy with you, show the ring gesture, and hearts appear. The companion doesn't need to be near.
- **One spouse each**: if you were married to another companion, that marriage ends. If this one was someone else's partner, they leave them for you. The command says so in both cases.
- What they've taught you stays taught.
- **Code**: `Courtship.wed` (pure, tested in `CompanionTests.wed`), `ControlCommands.marry`, `EtoBrain.married`. Protocol stays 83. Not tested in game.
