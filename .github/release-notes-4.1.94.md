## 4.1.94 (HexGhoul): the CCG building, the mansion and the hotels spawn; the mansion and the hotel trimmed to the main house; /locate no longer freezes; the gang leaders are made in their places

HexKagune 4.1.94 for Forge 1.20.1. Not tested in game. Protocol unchanged (127).

You said (quoted exactly): "the hotel on the villages, the hotel, the CCG in the mansion, they are confirmed to not be spawning. I do not
believe that they're spawning. I try to locate them. They never show up. The server usually freezes trying to look for it. When it doesn't,
if it doesn't crash, it just returns that it cannot find the structure type hex Kagune CCG building" and "Problem is that the main guys
from gangs, circus, gas mask gang, ccg, none of them spawn the main dude/girl".

**Why they didn't spawn:**
- Every one-of-a-kind place (the hideout, the CCG building, Anteiku, the library, the mansion, the circus, the Gas Masks' hideout) has its
  own set of possible spots, and all seven sets put theirs in the same 20 x 20 chunks of each 640-block region. A place was refused any
  spot within 12 chunks of a possible spot of any other set. Worked out over 4,000 random seeds: only 1 to 2 of every 100 of a place's spots
  passed. The small places scraped by on those few; the mansion also needed its 121 blocks to rise and fall no more than 6 blocks, so it
  passed nowhere. The CCG building lost more than four in ten of its spots to the hideout's alone.
- /locate worked out the ground at 25 nearby spots for every spot it looked at, before that cheap rule turned it away: thousands of ground
  samples per spot, spot after spot. That is the freeze.
- A hotel needed every chunk of its 200 x 200 blocks to be a plains biome, and only one village in four tried, on one side only.

**Now:**
- That rule is gone. The places are kept 512 blocks apart where they really stand (as before), and away from the hotels.
- The flatness a place needs grows with its size: up to 8 blocks for a middling place (the CCG tower with its yard, the circus, the
  mansion), 10 for a big one. Its site is still cut down to its floor, graded round, and filled under, so nothing floats or is buried.
- /locate turns spots away cheaply first (the hotels, the claims), stops sampling a spot's ground the moment it is too rough, and only then
  looks for water.
- If the spot a place was recorded at holds nothing (another mod stopped it there), /locate checks that spot once, gives it up, and goes
  on to find the next one, instead of saying it can't be found forever.
- These places are built in the overworld only. A mod's dimension made like the overworld could take the world's one before.
- Hotels: one village in two tries for one, on every side clear of other villages (the first whose land holds it), with its middle on
  plains and three quarters of its ground plains (never a mountain, a sea or a steep biome).
- Roma, Banjou, the General (and every official) are made at their own spot in whichever of their places you stand in, as the world built
  it. Before, their spot came only from the place's record, and a body of theirs found standing in their place but not where the record
  said was taken for a stray and sent away.

**The mansion and the hotel, trimmed** (you said, quoted exactly: "trim the building of the mansion so that it's smaller and dynamic.
Like it's too big as it stands. Same for the hospital. I mean, for the hotel, I'm pretty sure. Just trim them up so that they're not as big.
It just keeps the main house itself. Be careful with the floor though."):
- The mansion keeps the house and its small back yard (its pool and striped lawn): 79 x 67 blocks, from 79 x 121. The walled garden in
  front (tennis court, big pool, front lawn, helipad) is gone. Its floor and the ground under it are the same as before. Arima now stands
  on the back yard's lawn, facing the house.
- The hotel keeps its tower, its forecourt with the trees, and a road's width round it: 115 x 152 blocks, from 191 x 196. Its slab floor
  and basement are kept under all of it. The 20 people living in it are still at home in its rooms, and its six investigators patrol the
  road round it and the forecourt.

Existing worlds keep what was already built. The mansion, CCG building and hotels appear in new chunks, and /locate finds them.
