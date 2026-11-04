## 2.3.4 (HexGhoul): human partners: a real kiss, no fear, a ring

HexKagune 2.3.4 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Village human partners now get the full romance the companions have.

- **The kiss works like Eto's, Touka's and Ken's.** They come up, lean in and wait. A tap of Shift is a quick kiss. Holding makes them blush, and holding longer pulls you in to make out, with hearts. When you let go, you've kissed. If you leave them waiting too long, they give up with a line of their own temper ("Coward. ...Later."). Before, each step was a short timed animation, so they dropped the kiss while you held Shift and then picked it back up, and you could never actually finish it.
- **They don't run from a ghoul they've accepted.** If you've told them you're a ghoul and they took it (or you're together), you can bring your kagune out in front of them: they won't run or shout "ghoul". They still run from you if you hit them.
- **A romance page.** A partner's talk has "Be sweet to ... (romance)", with "I love you", a kiss, a hug, a walk hand in hand, and Back.
- **Marriage.** With a ring in hand, the romance page offers "Marry me. (offer the ring)". They answer by their temper (12 tempers, 2 answers each), the ring is taken, and the server is told you're married. Their talk screen then reads "Your wife" or "Your husband". You can't marry a villager while you're married to a companion.
- **Code**: `HumanBrain.startKiss` and `kissing` (driven by `EtoRules.kiss`), `HumanBrain.knows`; `HumanTalk.sweet`, `propose` and `spouse`; `HumanPersona.MARRIED`; `HumanRomance.proposed` and `hanging` (tested in `NpcTests`). Protocol stays 84. Not tested in game.
