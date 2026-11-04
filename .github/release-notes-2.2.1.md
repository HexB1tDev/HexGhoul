## 2.2.1 (HexGhoul): a ghoul's and a human's child

HexKagune 2.2.1 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

A child of a ghoul and a human (a human player and Eto, Touka or Ken) is now rarely carried to term. 7 in 10 such pregnancies are lost, somewhere in the first half. The one that lives is born a one-eyed ghoul, always, with the ghoul parent's kagune whole. It has 60% more health, 50% harder blows (fists and kagune) and 30% more speed than any other child, and it fights with the White Reaper's dashes. Its kakuja once grown (Ukaku's first kakuja, Rinkaku's centipede legs) comes in the next version.

- **Losing the baby**: the pregnancy ends between 15% and 55% of the way through. The bump goes, and both parents are told gently: "You lost the baby. A ghoul's child and a human's rarely lives. It isn't anyone's fault."
- **The dashes**: in a fight it closes a 3.5 to 14 block gap in one burst, stopping just in reach. Hurt, it dashes aside out of the way of the next blow. It has 3 charges, one back every 5 seconds, the same rules as the player's (`ReaperDash`).
- **The rest**: two ghouls' children and two humans' children are as they were. Children born before keep everything they had.
- **Code**:
  - `family/HalfBlood` (pure, tested); `Genome.half` (a tenth saved part; older saves read as not half);
  - the pregnancy's `lostAt` (an eighth saved field), with the loss in `Pregnancies.lost`;
  - `server/HalfBloodServer` for the dashes; the health, speed and damage in `ChildBrain` and `ChildKagune`.
- **Tests**: `FamilyTests.halfBlood`; the older genome tests now leave half-human pairs out. Not tested in game.
