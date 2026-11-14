## 4.1.89 (HexGhoul): a woman's coat takes her chest's shape (the CCG and ghoul outfits)

HexKagune 4.1.89 for Forge 1.20.1. Not tested in game. Protocol unchanged (127).

You said (quoted exactly): "Yet another bug: when women are wearing our armor, our CCG armor or the ghoul armor, their breasts literally
stick out from the armor, which is very awkward and very awfully putting. So you got to fix that so the armor adapts to their chest."

**Why it happened:** a woman's bust is drawn as its own shape on her torso's front. A player's bust was already hidden under any
chestplate, but every NPC woman's (an investigator in the Formal Outfit, a villager, a companion, a grown daughter) was drawn whatever she
wore, and it stands out further than the coats' fronts, so it came straight through them.

- **The coat now adapts to her chest:** on a woman, the Formal Outfit's coat (CCG) and the Crimson Outfit's (ghoul) are shaped over her
  bust: the coat's front is cut into thin strips across the chest and pushed out by exactly her bust's shape there, so it rises over it
  and falls under it. The lapels, the tie, the buttons and the Crimson coat's buckles ride the curve with it; the panels thicken over it
  rather than coming away from her; the coat's sides close the shape (no gaps from the side); nothing on her back moves.
- **Her own figure:** the coat follows each woman's own curves (a player's full figure, an investigator's slimmer one, each companion's
  own, a villager's, a grown daughter's), and bounces with her bust as she jumps and lands (an investigator's stays still, as her bust
  always has).
- **Her skin's bust is no longer drawn under anything worn on her chest:** under one of our coats the coat takes its shape; under any
  other chestplate it lies flat over her, as a player's always has.
- **Players too:** a woman player in either coat now has it shaped over her chest (before, it was flat).
- Fast to draw: each coat shape is made once per figure and bounce and kept.

### Choices of mine you may want changed

- The Crimson coat keeps its own open seam down the middle (your model's); over the chest it is a deeper groove now.
- Vanilla chestplates on a woman stay flat (only our two coats are shaped).

### Other

- Renders: `docs/handoff/armor-fit-4.1.89.png` (before, her bust through each coat; now, the coat shaped over it: from the front, both
  sides, three quarters and below, a slimmer figure, bounced up and down, walking, close up). `tools/ArmorFitPreview.java <out.png>`.
- The outfit previews in `tools/ArtPreview.java` lit every outfit face as if facing inward (only visible on slopes and tops); fixed, so
  the renders show the coat lit as the game lights it.
- Tests: `OutfitTests.fitted489` (the coat in front of her bust everywhere, its front out by exactly her bust's depth and nowhere else,
  half-pixel strips, every corner pushed only outward, texture kept, normals outward, the cache, the wiring); 35 rules sabotaged one at a
  time (`docs/handoff/sabotage-4.1.89.json`), all caught.
