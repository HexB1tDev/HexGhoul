## 2.2.3 (HexGhoul): a partner's mood shapes what they do

HexKagune 2.2.3 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

A partner's mood now shapes what they do, not just what they say. Mad or jealous, they never come up asking for a kiss, and they turn yours down. Lonely, affectionate or clingy, they come for kisses far more often. Sleepy or starving, less. Once a sulk or a jealous spell has passed, they come to you on their own to make it up.

- **Coming for a kiss** (their own asks and quick pecks), against their usual: lonely ×2.2, affectionate ×2, clingy ×1.8, giddy ×1.6, bored ×1.2, happy ×1, sleepy ×0.5, starving ×0.4, sulking or jealous never. Their hand-holding, hugs and leaning already followed their mood; the scenes they come up with already had their own for every mood.
- **Your kisses and hugs**:
  - Sulking, they're turned down ("Touch me and lose a hand." / "...Not right now. Give me a little while.").
  - Jealous, the same ("Kiss whoever you were with earlier. I'm sure they'd love it.").
  - Starving, a kiss is turned down ("If I kiss you now I'll bite. Feed me first, darling.") but a hug is fine.
- **Reassuring**: jealous, their first page offers "It's only you. It's always been you." That ends it (+2 regard) in their own words.
- **Making up**: after a sulk (being hit more than once, a kiss left hanging twice) or a jealous spell, they walk up to you once it's over with a scene of their own: Eto rewrote your death so you live, Touka shoves a can of coffee at you, Ken comes to make it right. You can make up back (a hug, a kiss, a hand) or tease them. Each companion has 2 (`make_up`, `jealous_over`), never picked at random (`when: makeup`).
- **Code**: `npc/MoodActs` (pure, tested in `CompanionTests.moods`); `EtoBrain.makeUp`; the refusals and reassurance in `EtoServer`. `SceneTests` checks every companion has both make-up scenes. Not tested in game.
