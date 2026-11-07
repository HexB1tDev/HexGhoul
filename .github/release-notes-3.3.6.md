## 3.3.6 (HexGhoul): A jealous partner acts it out

HexKagune 3.3.6 for Forge 1.20.1. Not tested in game.

From your list after 3.2.1: **a jealous animation.** Before, a jealous partner said a line and made one small gesture (a glare or a huff). Now they act it out.

**The act (about 7 seconds), for Eto, Touka and Ken alike:**

- They turn their back on you, arms folded, and glare at you over their shoulder.
- A huff: the shoulders drop.
- Then the snub: the head turned away from you, a foot tapping.
- At the end they turn back round.

**When it happens.** Under the one-partner rule (3.3.4) there's nobody else to kiss, so the old trigger (seeing you sweet on another partner) almost never fires now. New triggers, while you have a companion partner who is near enough to see it:

- You ask another companion out.
- You ask a villager to be together, or confess to one.

Your partner says something about it in their own voice (new lines for this: Eto amused and menacing, Touka furious, Ken hurt), names who it was, and acts it out. They stay jealous for a while (as before). While they're jealous, now and then (about every 25 seconds, if they're near, can see you and aren't busy) they turn their back on you again.

- **Approximated:** the turn away is about 55 degrees, not all the way round, so the glare over the shoulder still faces you. A preview made off-game: `docs/handoff/jealous-3.3.6.png`.
- Not covered: a villager partner doesn't act jealous (villagers have no gestures).
- **Network change:** protocol 93 (the new gesture is sent by number). Client and server must both be 3.3.6.
- Tests: CompanionTests `jealous` (the gesture appended last; the glare, the huff, the snub with the foot tapping, and turning back round); DialogueTests (every companion has at least five lines for it, all different, filled in, naming who it was, none about a kiss).

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 93. Not tested in game.
