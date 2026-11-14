## 4.1.102 (HexGhoul): your game finds the mod's NPCs from a list too (the lag patch, part 4: the client)

HexKagune 4.1.102 for Forge 1.20.1. Not tested in game. Protocol unchanged (128).

You said (quoted exactly): "make handoff mentioning a massive long, lag patch, making sure absolutely nothing in the mod fails and nothing
ever lags."

**Every frame, your game looked for:** the wild ghouls with their kagune out near you and the children's (in a box 96 blocks across), Roma
with her tendrils (128 blocks across), and the flesh creatures round Peace's work: each a search of every entity in every chunk of the box,
every frame. Now your game keeps its own list of the mod's NPCs as they come into view (as the server has since 4.1.100), and those walk
the list: no world search is left anywhere in the mod for its own NPCs.

**A coat's first-person sleeve** made its texture's name anew every frame; now once.

**Looked at and left as they are** (each already returns at once when there is nothing of its kind to draw, or loops over the players
only): the 38 client ticks and the 17 world render stages; the outfits and coats are drawn with no allocation per vertex; the companions'
renderer makes a few small things per companion per frame, not worth changing. What `/kagune lag` shows from your game will say what
to go after next.
