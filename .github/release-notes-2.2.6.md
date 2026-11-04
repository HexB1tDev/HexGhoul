## 2.2.6 (HexGhoul): village humans come up to you

HexKagune 2.2.6 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Village humans now come up to you on their own. A villager who knows you will sometimes notice you (within 12 blocks, in sight), walk over and say something. Why they come, how often, and what they say depend on their temper (all 12) and how close you are. Partners come most. Strangers never do.

- **How often** (as long as you're nearby): about every 10 minutes if they like you (regard 20+), every 5 minutes if they're close (60+), every 2.5 minutes for a partner. Their temper speeds that up or slows it down: flirty ×0.6; cheerful, bold, playful and romantic ×0.7; kind ×0.85; dreamy and sarcastic ×1.1; nervous ×1.4; stoic ×1.6; shy and grumpy ×1.8. Only one villager comes to you every 45 seconds, so a street never swarms you.
- **Why they come**:
  - **Hello**, just to say hi.
  - **News**: the village's latest, their temper's way in ("Breaking news from our thrilling little village: ...").
  - **Gossip** about a neighbour nearby, by name and trade.
  - **A gift** from their own trade (once a day, close friends and partners only): the baker's bread, the beekeeper's honey, the smith's iron, the florist's flowers. You get it and see "X gave you ...".
  - **Worried**, when you're under half health ("You're bleeding on my shoes. ...Are you alright, though? Really?"). A partner also hugs you.
  - **Partners only**:
    - They missed you, if you haven't seen them in 2+ days. They hurry over and hug you.
    - A kiss: they call out on the way ("Kiss ambush incoming!"), walk up and kiss you.
    - A hug.
    - A walk: they take your hand (tap Shift to let go).
- **Their words**: every temper has its own lines for each reason. A shy one fidgets, a grumpy one grumbles, a sarcastic one jabs, and a romantic one gets poetic.
- **When they don't come**: if you've hit them, if you're talking to someone, or if you've left the village. Village people only come by day (they're home at night). Talk to one on the way over and the visit is dropped.
- **Code**:
  - `village/HumanApproach` (pure: how often, why, the lines).
  - `server/HumanApproachServer` (noticing, walking over, arriving), hooked into `HumanBrain.tick`.
  - Kisses and hugs use the villager's existing walking-up.
  - Tested in `NpcTests.approach226`.
- Protocol stays 83. Not tested in game.
