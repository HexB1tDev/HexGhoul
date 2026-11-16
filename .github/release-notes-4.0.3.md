## 4.0.3 (HexGhoul): Coats that hang over the legs; sleeves closed; the Crimson Outfit without its hood, with a high collar

HexKagune 4.0.3 for Forge 1.20.1. Not tested in game.

### Legs and arms

Your words, of the 4.0.2 renders: "Do I need to say anything about both outfit's legs and arms?"

- **No leg through the coat:** the coat tails now hang over both legs instead of following each one only half way.
  - Both fronts go with whichever leg is furthest forward, and both backs with whichever is furthest back. The coat never parts down the middle, and neither thigh comes through it. Each side spreads between the two.
  - The tails fold at the knee, as the trousers do. Below the knee they hang straight down. Where a shin or a boot reaches them (a bent knee, a long stride, a crouch, sitting, kneeling, a kick), they're pushed just clear of it.
  - A leg pushes the coat at once. As the leg swings away, the coat falls back slowly, as cloth does.
  - Out to the side and turning, the tails go with their own leg.
- **Sleeves closed:** each sleeve's inner side (facing the body) sat just inside the arm, so your skin showed there as the arms swung. Every sleeve, wide and slim, now closes over it.
- **Textures at the joints:** in 4.0.2, each half of a piece cut at a knee, an elbow or the torso's middle showed the whole texture squeezed in. Each half now shows its own part, so trousers, sleeves and coats look as they did before 4.0.2.

### The Crimson Outfit (Aogiri Tree's)

Your words: "On the Ougiri outfit, no more hood, make it less bulky around the shoulders and head to be similar to the formal outfit, and, give it a high collar on the sides of the head pointing outwards."

- **No hood.** The Crimson Hood item is gone, and the head and shoulders are now the same build as the Formal Outfit's. Your model file still has the hood; the build leaves it out.
- **A high collar on the Crimson Coat:**
  - A tall panel beside each side of the head, from the shoulders up to the cheek, flaring outward as it rises.
  - A lower panel behind the neck, flaring back.
  - The front is open, so a mask shows.
  - Outside is your hood's crimson cloth and inside its black lining, with the coat's dark piping on the edges.
- **The head slot is free:** with the coat on you can wear any mask.
- **Aogiri members** wear the coat, tails and boots, with their faction's mask on their faces.
- **Old worlds:** Crimson Hoods already in your worlds disappear (the item no longer exists). The hideout's chests no longer hold them.

### My guesses

- How fast the coat falls back (0.12 radians a tick); how far clear of a leg it stays.
- The collar: 5 pixels high at the sides, flaring 18°; the back one 4 high, flaring 12°.
- Putting the collar on the coat (not the head slot), so a mask can be worn with it.

### Known limits

- A high collar beside the head: turning your head far to one side can take its corner through the collar's base.
- In the most extreme tuck (thighs right up, knees fully bent) a tenth of a pixel of knee can show at the coat's front.

### Network and tests

- **Network:** protocol 107. The Crimson Hood is gone and the outfits file is version 3, so clients and the server must both be on 4.0.3.
- **OutfitTests:**
  - each half of a cut piece carries its own part of the texture;
  - every sleeve closes over the arm's inner side;
  - no leg comes through the coat in 19 poses: standing, walking, running, bent-knee strides, crouches, sitting, kneeling, kicks and a tuck;
  - the two fronts never part;
  - no Crimson Hood, and nothing drawn on the head;
  - seven outfit pieces.
- **The full suite.**

Previews: `docs/handoff/formal-outfit-4.0.3.png` and `docs/handoff/crimson-outfit-4.0.3.png` (standing, walking, running, crouching, sitting), drawn from the baked models outside the game.

Validation: the full `python3 build.py` completed with EXIT 0 on these sources before this was pushed. Not tested in game.
