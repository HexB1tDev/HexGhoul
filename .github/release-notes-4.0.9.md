## 4.0.9 (HexGhoul): Ken's new tower, and he sits on the top

HexKagune 4.0.9 for Forge 1.20.1. Not tested in game.

Your words: "I sent you a replacement for the Ken tower as well, make sure he sits on the top."

### The tower

- **Your new tower** replaces the old one, read straight from your Litematica file: the podium, the three glass tiers stepping in, the star-shaped roof deck behind its parapet, the quartz crown and the netherite spire.
- **The height:** it's 310 blocks tall, and a 1.20.1 world ends at y 320, so it can't stand on the ground whole.
  - Your tower's two tallest tiers repeat the same floor every four blocks, so nine floors come out of each. The cuts are seamless.
  - It's now 238 tall and keeps every part: the podium, every tier, the deck, the crown and the whole spire.
- **Blocks:** those newer than 1.20.1 are swapped for their nearest. Polished tuff becomes polished andesite (its stairs and walls the andesite ones), and chiseled tuff bricks become chiseled stone bricks.
- **Where it spawns:** where it always did. Find it with `/locate structure hexkagune:cyber_tower`.

### Ken on the top

- **Where:** he lives on the roof deck, at the tip of its east arm by the parapet.
- **Sitting:** with nothing to do, he sits there, looking out. He's down on the deck with one leg out and the other knee up, his forearm on it, leaning back a little on one hand, and his head drifts as he looks about.
- **Getting up:** he stands to talk to you, follow you, hold your hand or fight, and sits back down when it's over. Once you're married and have a home of your own, he doesn't sit there (he lives with you).

### My guesses

- Which floors come out (the middle of each of the two tallest tiers).
- Ken's spot, and the way he sits.

### Network and tests

- **Network:** protocol 108 (Ken's sit is a new gesture), so clients and the server must both be on 4.0.9.
- **CompanionTests:**
  - the tower is 73 × 238 × 73 and stands under the world's top, with Ken on its top deck;
  - only Ken sits, held until something calls him up, and the seated pose is right.
- **The full suite.**

Preview: `docs/handoff/ken-tower-4.0.9.png` (the converted tower from the side, Ken marked), drawn outside the game.

Validation: the full `python3 build.py` completed with EXIT 0 on these sources before this was pushed. Not tested in game.
