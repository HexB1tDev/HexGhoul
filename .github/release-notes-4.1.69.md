## 4.1.69 (HexGhoul): Ken's sitting leg

HexKagune 4.1.69 for Forge 1.20.1. Not tested in game. Protocol unchanged (125).

Your words (quoted exactly): "Ken's sitting animation is broken, one of his legs is just in the air."

- **Why it was broken:** Ken's model (like every companion's) has no knee joints. His old sitting pose had one leg out and the other
  knee drawn up, with the shin meant to fold back down. With no knee to fold, that whole leg pointed stiffly up into the air.
- **Now:** he sits with his hips down on the deck and both legs stretched out along it in a loose V (the right a little wider), his right
  hand resting by his hip and his left forward on his thigh, leaning a touch to one side, his head turned a little and slowly drifting as
  he looks out. Every limb is somewhere a body without knees can put it, so no leg is ever in the air.
- **Clear of the parapet:** with his legs out in front, sitting on his spot itself would put his boots through Anteiku's parapet. He now
  sits back from any wall ahead as far as his boots need (never off his own block) and slides the last bit onto that spot before he sits
  down, instead of sitting wherever his walk home happened to stop (up to half a block off). This works in worlds where he's already on
  the roof too.

### Other

- Renders: `docs/handoff/ken-sit-4.1.69.png` (all the way round) and `docs/handoff/ken-sit-roof-4.1.69.png` (on the roof by its
  parapet: from the street side, both sides, behind, above).
- Tests: `EtoTests.kenSits` works out where his limbs end up the way the game poses them: both legs on the deck end to end, both hands
  resting, his boots within reach of the spot rules (`SitSpot`). Put back to the old raised leg, it fails.
- Next: the Anteiku faction through Ken, then the CCG faction through the General, then the hotel.
