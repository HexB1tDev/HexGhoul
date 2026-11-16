## 3.1.4 (HexGhoul): Expand personality and mood dialogue

HexKagune 3.1.4 for Forge 1.20.1. Not tested in game.

- Add 29 conversations each for Touka, Eto and Ken (87 total; 414 spoken lines including unanswered approaches), in their own voices. Cover ordinary chatter, flirting, anger, jealousy, reassurance, missing a partner, tiredness, hunger, injury, home life, pregnancy, parenting and bringing the kids home.
- Add 36 replies to the actual first-baby, another-baby and already-agreed tonight requests. The real baby request stays on the sweet page, using the existing marriage/compatibility/pregnancy and five-Minecraft-day recovery checks.
- Add 288 spoken lines across all twelve adult human temperaments: greetings and injury concern, flirting at each relationship level, jealousy, danger, affection, kisses/hugs, feelings, family planning and private bedtime invitations. Keep all original lines. Human family planning and bedtime additions are dialogue only; they add no pregnancy, teleport or intimacy mechanics.
- New named-companion private invitations require marriage, a shared home, night and an affectionate mood; the existing night action/cooldown handles acceptance. Family conversations require real children or the appropriate pregnancy state. Human private/family topics require marriage and recheck state when answered; injured/upset partners answer in that mood. Add a Talk about us entry on the human romance page.
- Keep swearing uncensored, with frequency and tone matching each personality. Private invitations are suggestive and non-graphic, and remain separate from ChildNpc. No child script, network schema, relationship status, jealousy affection, movement or combat mechanics changed.
- Add DialogueTests for invitation eligibility, gentle alternatives, family conditions, all twelve human voices under changing context, and baby-request guards/response variety. The focused SceneTests, DialogueTests, CompanionTests, EtoTests and NpcTests passed before the full build.

Validation: full python3 build.py completed with EXIT 0 on Java 17, including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 89. Not tested in game.
