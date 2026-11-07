## 3.3.8 (HexGhoul): Who's at the door

HexKagune 3.3.8 for Forge 1.20.1. Not tested in game.

From your list after 3.2.1: **door scenes that depend on the child's temper and age.** Before (3.2.1), when you brought a child home and their other parent was waiting, Eto, Touka or Ken met you with a scene about whether the child had wandered off or was back from an outing with you. It didn't matter which child it was. Now it does.

**By temper.** Each companion has a door scene for each of the seven tempers, both found and back from an outing (14 each, 42 in all). For example:

- A **bold** kid strolling in unrepentant: Touka grabs them by the collar, Eto knows that chin from her own mirror, Ken asks them to just tell him next time.
- A **shy** kid hiding behind you: they kneel down and promise nobody's in trouble.
- A **mischief** kid who hid on purpose: Touka threatens to hide their dessert forever, Eto explains that a trick's only funny if everyone laughs after.
- A **gentle** kid who brought a flower, a feather or a stone home for them.
- A **curious** kid with full pockets and a hundred questions.
- A **serious** kid giving a rehearsed apology or a report of the outing.
- A **sunny** kid who never knew they were lost.

**By age.** And a scene each for a baby, a toddler (found, or back muddy from an outing) and a bigger child or teen (old enough to know better, or pretending a day with a parent was boring).

The new scenes come up more often than the old general ones (those still play sometimes). They rotate, so you won't see the same one twice running. A baby never gets a temper scene (it can't show one yet), only the baby one.

- **Approximated:** the age split is baby / toddler / child-or-older (a child and a teen share scenes). How often each kind comes up was a guess: roughly 4 in 10 by temper, 4 in 10 by age, the rest the old ones.
- No network change (protocol 93). No new Minecraft API.
- Tests (SceneTests): for every companion, every temper, found or not, and toddler / child / teen, a door scene fits by temper and one by age. None ever fits a different child's temper or age. A baby gets the baby scene, not a temper one.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 93. Not tested in game.
