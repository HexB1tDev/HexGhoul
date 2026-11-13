## 4.1.69 (HexGhoul): Ken's sitting fixed, no leg in the air

HexKagune 4.1.69 for Forge 1.20.1. Not tested in game. Protocol unchanged (125).

Your words (quoted exactly): "Ken's sitting animation is broken, one of his legs is just in the air."

- **Why it broke:** the companions' bodies (Ken, Touka, Eto) turn each arm and leg whole: they have no knees or elbows. His sit
  (from 4.0.9) raised his left knee and folded the shin back, but with no knee to fold, the whole straight leg pointed up in front of
  his chest.
- **His new sit, made for stiff legs:** both legs flat on Anteiku's roof, their undersides on the deck (measured: 0.02 px above it, hip
  to foot), the right one straight out before him, the left swung out to the side. He leans back and over onto his right hand, its heel
  on the deck behind him (0.16 px above it), as anyone propped on one hand does; his left hand rests on his thigh (0.16 px above it). His
  head is brought forward again, so he looks out over the street, not up at the sky, and still drifts slowly about.
- **Sitting down:** his hips come down only as fast as his legs swing up, so his heels slide out along the floor (never more than
  0.12 px off it). Before, his feet went almost 3 px through the floor halfway down. His hand reaches the deck just as he lands.
- **Getting up** (called away, a fight, someone to follow): the blend from one pose into the next took his hips up in a straight line
  while his legs swung down, his feet 2.9 px through the floor halfway. Now, out of (or into) sitting, his hips go no lower than his
  legs let them, so his feet stay on the floor as he gets up. Every other gesture's blend is unchanged.

### Other

- Renders: `docs/handoff/ken-sit-before-after-4.1.69.png` (the old sit and the new, from the front, both sides and behind) and
  `docs/handoff/ken-sit-4.1.69.png` (the new sit all round, sitting down tick by tick from his side and the front, getting up from his side).
- Tests: the old test required the raised knee; it now checks both legs level, no knee or elbow asked of a stiff body, the lean onto
  his hand, and his feet on the floor through the whole sit-down and getting up (into nothing, his hand to his chin in thought, a bow). Breaking any of it
  (the old leg back, the old straight drop, the blend without the floor) fails it.
- Next: the Anteiku faction through Ken, then the CCG faction through the General with dailies, then the hotel.
