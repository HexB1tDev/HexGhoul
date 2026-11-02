## 1.3.70 (HexGhoul): CCG ranks, they attack only what shows itself, and protecting humans comes first (testing)
HexKagune 1.3.70 for Forge 1.20.1. Install on client and server (**protocol 72**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

You asked: "CCG agents now have different ranks (random in the wild) the higher the rank the more HP and faster they are, CCG agents dont attack anyone unless their eyes are revealed or kagune shown", and "Humans will run to a nearby ccg agent ... CCG agents top priority if to defend humans, so they will drop what they're doing to protect them if they are in trouble."

- **Six ranks**, rolled when an investigator first appears and kept in its save: Rank 3 (the most common), Rank 2, Rank 1, First Class, Associate Special Class, and Special Class (rare: about 3 in 100). The rank is its name over its head. Health rises from 36 (Rank 3) to 124 (Special Class), and speed rises with it (Special Class is about a quarter faster). In a pair, the one leading takes the better of two rolls and the other the worse, so the leader usually outranks the partner. Agents from older saves get a rank the first time they load.
- **They attack only what shows itself:** a player with their **eyes on** or **kagune out** (or in a kakuja / final kakuja), and a wild ghoul only with its eyes on or kagune out (so by day, when wild ghouls hide, the CCG lets them pass; at night, prowling with their eyes on, they're fair game). Anyone who strikes an agent is still fought, and they still cut down monsters that come too close. A fight already under way carries on even if the ghoul hides again.
- **Protecting humans comes first:** whatever is hurting a human (whoever struck one lately) or hunting one (a ghoul with a human as its prey) within 24 blocks, an agent **drops whatever it's doing** and goes for it, and its partner with it, ahead of anything else.
- **Humans run to the CCG:** a frightened human with an investigator within 24 blocks runs to stand **behind** it (on the far side from the danger) instead of just running away. It never runs past the danger to reach one. With no agent near, it runs away as before.

Judgment calls: the leader usually, not always, outranks the partner (each rolls when it loads). A player in creative mode who hits a human is left alone by the CCG, as creative players always were.

Untested in game: compiled and unit tested (the ranks' odds and stats). Try: a pair of investigators near a human at night with a wild ghoul about; your eyes on near an agent; summon several agents to see the ranks.
