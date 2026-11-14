## 4.1.100 (HexGhoul): the mod's NPCs found from a list, not a search of the world; far-off NPCs look round less (the lag patch, part 2)

HexKagune 4.1.100 for Forge 1.20.1. Not tested in game. Protocol unchanged (128).

You said (quoted exactly): "make handoff mentioning a massive long, lag patch, making sure absolutely nothing in the mod fails and nothing
ever lags."

**Finding the mod's own NPCs:** whenever an NPC or a system of the mod looked for the mod's NPCs near something (a gang calling its mates,
Aogiri's comrades coming to help, an investigator looking for its partner, the townspeople noticing a ghoul, a child's parents, the
officials near you, your allies within 128 blocks...), it searched every entity in every chunk of a box up to 320 blocks across. There
were 55 such searches. Now each world keeps its own list of the mod's NPCs as they load, and those 55 walk that short list instead: the
same answers, a fraction of the work, most of all in crowded places (the CCG tower's 48 investigators, the hideouts, the villages).

**Far from players:** a wild ghoul or an investigator with no fight on looks round for a new foe (every living thing within its sight,
and a line of sight to each one it would go for) every half second near a player, as before, but every two seconds when no player is
within 64 blocks (the game doesn't draw mobs that far out, so nobody sees the difference). A fight already going on is not slowed.

**`/kagune lag`** now also says how many of the mod's NPCs and players are loaded when it reports.

Not done on purpose: thinking less often as a whole for a far-off NPC. Their minds check many things "once a second" on a fixed tick, and
skipping ticks would quietly stop some of those (or stack every NPC's thinking on the same tick, a stutter every second). The searches
were the cost, and they are what changed.

Please send me `/kagune lag` lines from where it lags; they say what to go after next.
