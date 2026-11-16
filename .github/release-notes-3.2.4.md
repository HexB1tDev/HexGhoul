## 3.2.4 (HexGhoul): Fix: a maxed ghoul thrown back in clashes by low-rank investigators

HexKagune 3.2.4 for Forge 1.20.1. Not tested in game.

The owner's bug report: "I keep getting pushed back on the clash ... CCG agents, even at low rank, are pushing back ... I'm the one sent flying instead of them ... I'm at like maximum level, maximum mastery, maximum quests done, maximum ghoul meat eaten ... at max level, it should be able to be matching Arima."

**What caused it.** In a clash the weaker blow is thrown further, and a big enough gap sends the weaker one flying like the Speed Kick. "Weaker" was decided by damage, but the two sides weren't measured the same way:

- **An investigator's blow** counted its quinque's full damage times its rank's power.
- **Your blow** counted the kagune's damage *after* the mod's balance had scaled it down for mastery (to 0.55 at full mastery), with no credit at all for quests done or ghoul meat eaten.

So a rank 4 investigator with a mace out-hit a fully mastered ghoul, and the Reaper overwhelmed one outright.

**The fix: one scale for both sides.**

- A blow in a clash is an ordinary blow (the Reaper's Dawn swing) × the fighter's power × how heavy this blow is.
- Weight counts by its square root: a finisher or a mace still counts, just less than who swings it.
- An investigator's power is their rank's.
- **Your power runs from a fresh Rank 0's up to the Reaper's** with how far you've come:
  - mastery: 55%;
  - the gates on your path opened by quests and lessons: 30%;
  - ghoul meat eaten (150 pieces is all of it): 15%.
- Your kakuja form and mutations add up to 1.6× on top for kagune blows.

What that means in a fight:

- **Fully progressed, your ordinary blow meets the Reaper's as an equal.** With your form and mutations, you can out-push him.
- Fully progressed, **any investigator up to Rank 7 is thrown back further than you**, whatever weapon and whichever blow of their string.
- **A Rank 0 or 1 is sent flying** by your blow. A Rank 2 with a mace finisher is just shoved hard.
- **No investigator but the Reaper can send a fully progressed you flying.**
- A brand-new ghoul and a Rank 0 meet about even; halfway, you're in between.

This covers your kagune M1s, the eyes-only fist strikes and graft blows. Wild ghouls and the Owl's moves are unchanged. Your actual damage is unchanged too: this is only who wins the push.

- No network changes (protocol 92). No new Minecraft API.
- Tests: ClashTests check every case listed above (the Reaper's equal, up to Rank 7 losing, Rank 0-1 flung, never flung by anyone but the Reaper, a fresh ghoul even with a Rank 0, halfway in between, the form and mutation cap, heavier blows counting by their root).

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 92. Not tested in game.
