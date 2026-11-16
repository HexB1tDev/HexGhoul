## 4.0.2 (HexGhoul): Armour that moves; the Aogiri mansion; Ayato poses, Noro tilts his head

HexKagune 4.0.2 for Forge 1.20.1. Not tested in game.

### Armour and animations

Your words: "Bug, the armor does not allow for animation, make it compatible with animations"; "the jacket also breaks when walking".

- **Bends with your body:** the outfits now bend wherever PlayerAnimator bends you: knees, elbows and the middle of the torso. Every piece across a joint is cut there when the models are built. Each half bends exactly as your skin does, and the joint itself bends halfway so it stays closed. Crouches, jumps, flips, kagune poses and the rest now carry the trousers, sleeves and coat with them.
- **Coat tails:** the coat's tails hang from your hip and follow your legs only part of the way (about half forwards and back, a little more to the side), as cloth does. They no longer tear open down the front as you walk.
- Trousers, shoes and sleeves follow fully.

### The Aogiri Tree hideout: your mansion

Your words: "we're gonna replace the Ougiri hideout with another schematic ... I give them a mansion".

- **Your mansion is the hideout now:** its walled grounds, the house, the ponds, the helipad and the lawn.
  - It's 320 blocks wide, but a structure in 1.20.1 can only reach 272 blocks from its start.
  - So a 24-block band of plain paving and the inner edge of the trees is left out on each side. The walls, the trees' trunks and everything else are as you built it.
  - Blocks newer than 1.20.1 are swapped for their nearest: pale oak becomes birch; copper bars become iron bars; shelves become a wooden board on the wall; and so on.
- **One per world:** "Aogiri Tree only has one hideout." There's exactly one, placed once per world about 500–1000 blocks from the world's centre on suitable land, and always built there. Find it with `/locate structure hexkagune:aogiri_hideout`. Hideouts already generated in your worlds stay.
- **Villages:** "there's multiple villages that can spawn, although they're as rare as a normal village." The human town and the CCG settlement now share Minecraft's village spacing (34 chunks), never closer than 20 chunks to each other.
- **Its people:**
  - Two guards stand at each gate (north and south), one either side of the path.
  - Six members patrol a loop round the house, stop by stop, all the time.
  - Twelve more are spread through every floor and the grounds.
  - Noro has a suite on the top floor (west), quiet and out of the way. Ayato has the great lounge on the first floor (east).
- **The guards:**
  - Posted guards always go back to their spot after anything (a fight, a chase) and stand facing out.
  - The first time you come to a gate, one guard walks up and asks your business, once: six answers, three he takes and three he doesn't, in an order of their own for each player.
  - Answer well and he lets you pass and goes back to his post.
  - Answer badly and the whole hideout comes for you. They won't let you in for 5 Minecraft days; after that, they let you pass.
  - Walk off without answering and you're stopped again a little later.

### Ayato and Noro

Your words: "give Ayato like animations of like ambient animations of like being cocky"; "The crack fingers animation is only Kaneki's ... so don't give him that"; "Noro simply like tilts his head and looks at the player weird"; "when you train with him ... you tilt your head before the Kagune pops out."

- **Ayato poses** (new animations):
  - Hands in his pockets, chin up, looking down his nose.
  - A hand dragged back through his hair, then that arm folded behind his head.
  - A lazy shrug.
- **Noro** tilts his head slowly far over and stares, and nothing else moves. His replies use it too.
- **No finger crack** for Ayato or Noro.
- **The finger crack stays Kaneki's:** only players Kaneki taught a kakuja crack a finger before the kagune comes out. Ayato's First Kakuja lesson never gives it.
- **Noro's way:** once Noro has taught you the Worms and The Walker (a Bikaku player), bringing your kagune out is his way: you stand, your head tilts slowly far over, and the kagune comes out quietly.

### Creative menu

Your words: "we should have our own category in the creative menu with like the armors and quinques and whatever, everything separated and well organized." Everything of ours is now in four tabs of its own (out of Minecraft's Food and Combat tabs):

- **HexKagune:** Ghoul Meat, the Gold Ring, the cookbook and the kitchen's dishes.
- **HexKagune: Quinques:** each case beside its weapon, then the investigators' kagune-made weapons.
- **HexKagune: Outfits:** the Formal Outfit, then the Crimson Outfit, piece by piece.
- **HexKagune: Masks:** your 24 in your sheet's order, then the nine generic ones.

### My guesses

- How much the coat tails follow the legs (45% forwards and back, 60% to the side).
- The mansion: how many guards, patrollers and members; the patrol loop; which rooms Noro and Ayato get.
- The ring's distance (12).
- The gate's question and its ten possible answers; asking again a while after you walk off.
- The poses' timing.
- Noro's way starting after the Worms and The Walker.

### Network and tests

- **Network:** protocol 106. There are new gestures, Noro's tilt and the coat-tail pieces, so clients and the server must both be on 4.0.2.
- **OutfitTests:**
  - the four tabs, named; nothing of ours left in Minecraft's tabs;
  - no straight piece crosses an elbow, a knee or the torso's middle;
  - the coat tails hang apart from the trousers;
  - the layer bends with PlayerAnimator.
- **NpcTests:**
  - the hideout is the mansion, one per world;
  - the villages use Minecraft's spacing;
  - the gate's answers;
  - the 5-day ban;
  - the guards at their work.
- **CompanionTests:**
  - no finger crack for Ayato or Noro;
  - Ayato poses, and Noro's tilt;
  - gestures appended.
- **The full suite.**

Validation: the full `python3 build.py` completed with EXIT 0 on these sources before this was pushed. Not tested in game.
