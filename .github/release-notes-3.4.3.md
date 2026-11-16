## 3.4.3 (HexGhoul): Fix: clashes follow who's stronger

HexKagune 3.4.3 for Forge 1.20.1. Not tested in game.

Your report: *"no matter how strong you are, what CCG investigators are just gonna send you flying during clashes ... If you're stronger than investigators, then you should send them flying in the same for players. If you're max mastery, I don't think that the Reaper should be able to even send you flying ... Even at max mastery, one-eyed, and with max ghoul meat eaten, he still ignores that and sends you flying regardless."*

**What was wrong.** 3.2.4 put you and the investigators on one strength scale, and that part worked. But how far each side got thrown wasn't just the gap between you; it was also the size of the clash, which is both blows added together. So:

- The stronger you got, the harder **you** were thrown, even when you won. A maxed ghoul and the Reaper threw each other about a block a tick.
- The stronger side still took at least a third of the full throw.
- Both sides were thrown upward the same amount, every time, which is what made it look like flying even against weak ranks.
- Being one-eyed didn't count at all.

**Now:**

- **Only the gap between you decides who's thrown.** Your own strength never makes you fly further.
- **The stronger stands its ground.** Barely moved when it wins, not moved at all (and not lifted) once it's twice as strong.
- **The weaker is thrown further,** and once you're twice as strong (was two and a half times), **sent flying** like the kick, down and stunned. A maxed ghoul now does that to every Rank 0 to 3 investigator, whatever they swing, and to Rank 4 and 5 with ordinary blades. One-eyed, up to Rank 6. It works the same between players.
- **An even clash throws both a short way apart,** a little more for a huge one, never flying.
- **The Reaper never sends a maxed ghoul flying,** whatever he swings. At max mastery you're his equal: even clashes push you both back a step or two.
- **One eye counts:** a one-eyed ghoul is 15% stronger in clashes, so a maxed one-eyed ghoul edges past the Reaper. His Dawn blows throw you no further than an even clash would.

Before and after, a maxed ghoul (all quests, max ghoul meat) meeting the Reaper's Dawn finisher: before you flew back about a block a tick and lifted 0.3; now about half a block a tick, lifted 0.15 (one-eyed: 0.5 and 0.15, and he's thrown about as far).

- **Not changed:** the Reaper's kick, and a blow that lands on you without a clash (a finisher's normal knockback). Those aren't clashes. Tell me if either still feels wrong.
- **Approximated:** the numbers (an even clash 0.42 blocks a tick; the stronger stands at twice; flung at twice; one eye 15%) were my picks. Note: on Rinkaku, Ken's two lessons from 3.3.1 count as quests, so they still count toward "max" until he teaches you them.
- No network change (protocol 93). No new Minecraft API.
- Tests (ClashTests): the stronger a player grows, the less any investigator throws them; the Reaper never flings a maxed ghoul; a maxed one-eyed ghoul is thrown no further than an even clash by any of his Dawn blows; a maxed ghoul flings every Rank 0 to 3 and stands its ground; the same between players; one eye counts.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 93. Not tested in game.
