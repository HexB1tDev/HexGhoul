## 2.2.0 (HexGhoul): tolerance, and sorry is always there

HexKagune 2.2.0 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Companions are far more tolerant, and you can always say sorry. A first accidental hit or a missed kiss is brushed off with a word. Only more of them close together put your partner out, and then for 20 seconds to a minute, never the old 5 minutes. When they're put out, talking to them opens "I'm sorry." / "It was an accident. I swear." and that forgives you on the spot.

- **What there was before**: an apology existed but was hidden and harsh.
  - Hitting your partner: −8 regard and 2 minutes of them refusing to talk.
  - A kiss left hanging: 5 minutes of being ignored.
  - The only way out early: bring a baby animal on a lead, or hand over a written book (a letter). Both still work and still forgive at once.
- **Tolerance** (`npc/Tolerance`, pure, tested). What they hold fades 3 minutes after the last offence.
  - **Hits**: the first is brushed off ("Ow! Watch it, idiot." / "Ah— it's okay. Accidents happen."), and so is a second one in the middle of a fight (you hit something else in the last 3 seconds). The next puts them out (−2, a 20-second sulk). More than that makes them cross (−6, a 1-minute sulk).
  - **Missed kisses**: the first is shrugged off ("...Fine. Whatever. Not like I wanted one."). The next puts them out (ignored 20 seconds), then 1 minute.
- **"I'm sorry."**: forgives everything at once (+1 regard). If you've hurt them again and again (4 or more offences in 3 minutes), they forgive you but want a 15-second moment first.
- **Tests**: `CompanionTests.tolerance`. Not tested in game.
