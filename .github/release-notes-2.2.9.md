## 2.2.9 (HexGhoul): investigators give mastery and rations; Dawn a rare prize

HexKagune 2.2.9 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

CCG investigators now pay out properly. Every one you bring down as a ghoul gives mastery, and now and then one leaves a field ration: human food the cookbook's recipes use. The White Reaper's Dawn is now a rare prize instead of a sure drop.

- **Mastery for every investigator** a ghoul brings down, whatever you kill it with: 6 for a Rank 0, 2 more per rank (Rank 9: 24), and 60 for the White Reaper. The action bar shows "+N mastery". A kill with your kagune out still earns the original's few on top.
- **Rations** (uncommon: 1 in 8, a little more with Looting; any investigator a player kills): bread, wheat, eggs, sugar, beetroot, carrots, potatoes, baked potatoes, dried kelp, cocoa beans, sweet berries, brown mushrooms, apples, a honey bottle or a bucket of milk. You get 1 to 4 of one item, and every one of them is an ingredient in the kitchen recipes. Bread, wheat and eggs come most often.
- **Dawn**: the White Reaper leaves his case to whoever kills him 1 time in 5 (up to about 1 in 3 with Looting III). It was every time. He still always leaves human meat.
- The quinque case from regular investigators stays as rare as before (2.5%).
- **Code**: `AgentRules.mastery`, `dawnChance`, `RATIONS`, `rationChance`, `ration`, `rations` (pure, tested in `AgentTests.spoils`, which checks every ration is a cookbook ingredient); `AgentBrain.drop` and `slain`. Protocol stays 83. Not tested in game.
