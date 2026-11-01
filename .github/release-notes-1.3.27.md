## 1.3.27 (HexGhoul): CCG investigators: pairs in the owner's skins, fast and acrobatic, with quinques, clashes and parries; wild ghouls rarer; cases can't be crafted (testing)
HexKagune 1.3.27 for Forge 1.20.1. Install on client and server (**protocol 63**, unchanged: the investigators ride on their own entity data, no new packet). Testing build: compiled, tests pass, not tried in game.

- **CCG investigators (new).** Humans in the owner's nine skins (four women, five men, each on the arms its skin is painted for). Men are a player's size. Women use **Eto's body, a little taller (+7%) and a little skinnier**, with a touch less of the figure.
- **Always two.** They turn up in pairs and never alone: one leads, the other walks at its side. If one is summoned with `/summon hexkagune:ccg_investigator`, its partner turns up next to it. A pair never spawns near another pair.
- **Rare.** About one pair every two in-game days near a player, any time of day, 28 to 52 blocks out. **Wild ghouls are rarer now:** about one every two or three nights instead of one or two a night. So the investigators are a little more common than wild ghouls, but neither is an everyday sight.
- **Who they fight:**
  - A monster that comes within 6 blocks: cut down, and chased until it is dead or far away.
  - A wild ghoul within 20 blocks: hunted. Wild ghouls go for them on sight too, so whenever the two meet they fight.
  - **You, only once you reveal you're a ghoul:** if your kagune is out where they can see it (within 24 blocks), they mark you ("The CCG has seen your kagune.") and come for you. They keep hunting you for two minutes even if you put the kagune away. A ghoul who keeps it in is left alone.
  - Anyone who hits either of them: both fight back.
  - They never hurt each other, and leave companions and children alone unless attacked.
- **Their quinques.** Each one carries one of the five quinques shut in its case, and the two in a pair always carry different ones (never two launchers). The case **opens in their hand** when a fight starts (the same unfolding animation as yours) and shuts again a while after.
- **Their own M1s.** Each weapon has its own three-hit string (the same swings and finishers as yours: the katana's wide cut, the spear's piercing thrust, the mace's slam). The launcher keeps 7 to 16 blocks away and fires bursts of five crystal shards, leading its target.
- **A complex fight:**
  - **Fast:** quicker on their feet than any wild ghoul, faster again in a fight.
  - **Front flips** to close distance, **back flips** out (often right after a finisher, or when they're losing), **side flips** and **side dashes** to dodge.
  - **Clashes:** every swing of theirs counts as a blow on its way, so it clashes with a ghoul's kagune exactly as yours does (both thrown apart, nothing lands).
  - **Parries ("slapping tentacles away"):** when a kagune blow comes at them they often meet it with a quick cut up across it. The blow is knocked aside, they're thrown apart, and they answer with a riposte straight away. The katana parries most often, the mace least, the launcher never.
  - Their swings knock crystal shards out of the air like yours do.
  - In a fight the second one comes in **from the far side** (a pincer).
- **Their run (new animation):** the same for every investigator. Body pitched forward, head up, a long stride, the shoulders turning against the hips, and a small bounce. The free arm pumps; the case swings low at their side; a blade trails low behind them; a two-handed quinque stays in its stance, a little lowered.
- **Quinque cases can't be crafted any more.** The two recipes are gone. The only way to get a case is from a fallen investigator, and it's **really rare**: 2.5% when you kill one, up to 5.5% with Looting III. Nothing else they carry drops.
- Investigators show their rank when you look at them (a senior and a junior, e.g. "First Class Investigator" and "Rank 2 Investigator"). They leave when everyone is far away, like any wanderer; if one is left alone with no one around, it leaves too.

Untested in game: the investigators' fighting and spawning are new and only compiled and unit tested.
