## 3.2.3 (HexGhoul): Fix: companions frozen in the kiss pose

HexKagune 3.2.3 for Forge 1.20.1. Not tested in game.

The owner's bug report, with a screenshot of Eto stuck leaning in, alone: "whenever you finish kissing, sometimes the NPC gets stuck in that position after the kiss. Like, they just get stuck in it. That has to be fixed, like, ASAP."

**What caused it.** The kiss poses (leaning in, the blush, pulling you in, a hand held) are "held" gestures: they never run out on their own, and the kiss code lets them go when the kiss ends. But **the blush was also used outside kisses**, and then nothing ever let it go:

- 37 talk answers in Eto's, Touka's and Ken's scripts end with a blush. Plenty of them are the sweet answers you'd pick right after a kiss.
- A partner blushes when you turn your kakugan on near her.

For Eto the blush *is* the kiss lean, so she leaned in for a kiss that wasn't coming, forever (until something else made her move). The villager partners' kiss and hand poses relied on the kiss code alone in the same way.

**The fix:**

- A held pose with nothing behind it (no kiss under way or on its way, no hand held) now lets go after 4 seconds. A kiss that is happening keeps its pose for as long as you hold Shift, as before.
- A blush given as a reaction (an answer, your eyes) is drawn as a shy blush, hands to her face, not as a kiss leaned into. A kiss or pull pose with nobody to kiss is never drawn at all.
- Villager partners: their held kiss or hand pose ends the moment there's neither a kiss nor a hand.
- No network changes (protocol 92). No new Minecraft API.
- Tests: CompanionTests check that every held gesture is let go of when nothing holds it, a real kiss keeps its pose however long, a held hand keeps its pose while held, and the drawing rule (shy blush; no kiss pose with nobody).

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 92. Not tested in game.
