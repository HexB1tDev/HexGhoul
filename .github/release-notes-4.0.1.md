## 4.0.1 (HexGhoul): Outfits thinner and finished; The Walker strides

HexKagune 4.0.1 for Forge 1.20.1. Not tested in game.

### Outfits

Your words: "They seem a little too thick. Can you add more details to it as well to make it as polished and like not so thick in general?" and "you could add like a color or something to the formal outfit. For example, to make it look a bit more complex."

- **Thinner:** both outfits are drawn close to the body. Anything standing more than a quarter pixel off a body part keeps only a quarter of that distance. Each layer stays just over the one under it, so nothing flickers.
- **The Formal Outfit:**
  - Navy piping on its edges, hems and cuffs.
  - A white shirt with collar points beside the tie.
  - Silver buttons.
  - A navy-edged breast pocket with a red pocket square.
  - A silver badge on the chest.
  - A leather belt with a silver buckle.
  - Navy epaulettes with silver buttons.
  - Silver cuff buttons.
  - Navy trousers.
  - Laced, polished shoes.
  - The cap gets a navy band, a silver badge and a short black visor.
- **The Crimson Outfit:**
  - Dark piping on its front edges and hems.
  - A black belt with a silver buckle.
  - Strapped, buckled cuffs and boots.
  - Stitched seams down the back and the hood.
- **Slim arms:** all of it fits slim (Alex) arms too.
- **Your files:** your models and textures are unchanged. The details are new pieces painted in an unused part of each texture, and recoloured pieces keep their shading.

### The Walker (the Bikaku half kakuja)

Your words: "It's too robotic, too slow. Like the steps should be like they should vary in length ... it should always be random and like feel like more of a wobble, like a longer distance than the other ... it still feels like it's two sticks walking"; then "One leg at a time, not two in front at once ... They don't go back like further back enough or forward enough ... you can always tell where the next step is gonna be, which is what I don't want."

- **A real stride, one leg at a time:**
  - Each foot is put down well ahead of you and stays planted while you pass over it.
  - It lifts only once it's well behind you, while the other foot holds you.
  - The feet are out of step: one forward while the other is back.
  - Walking, planted feet range from about 2–3 blocks behind you to 1–2.5 blocks ahead.
- **Every step is random:**
  - Its length grows with your speed, and is usually 70–130% of the normal stride. About one step in ten is a short stutter and one in ten a long lunge.
  - Each step lifts at a different point and lands a little in or out.
  - Each step has its own height and speed (quicker when shorter).
- **Wobble:**
  - The middle of each leg is on a spring: it lags behind as you move and is thrown up as a foot lifts and down as it lands.
  - Your body rolls over the foot you stand on and dips while the other is up.
- **Not sticks:**
  - Each leg is a tentacle's S: up out of your hip to a high knee, then bowing out and down onto the foot.
  - Legs are thicker, and thick at the thigh.
  - A slow wave runs down them.

### My guesses

- **Outfits:** the quarter-distance and minimum gap; the colours (navy, silver, white, leather, red).
- **The Walker:** stride 1.6 blocks plus 8 per block-a-tick of speed; the 10% stutters and lunges; step times of 3.5–9 ticks; the spring's stiffness; the body's roll and dip.

### Tests and validation

Network: protocol 105 (unchanged since 4.0.0).

- **OutfitTests:** every one of your cubes is still there, plus the details; sleeves sit within a pixel of the arm; the coat front sits within a pixel of the chest.
- **BikakuWormTests:**
  - feet step strictly one at a time;
  - strides vary, short and long;
  - feet reach well ahead and go well behind you, out of step;
  - feet never jump, always stand on a block, and stay within the legs' reach;
  - the body only sways a little.
- **The full suite.**

Validation: the full `python3 build.py` completed with EXIT 0 on these sources before this was pushed. Not tested in game.
