## 4.1.45 (HexGhoul): roaming gangs, the Clowns and the Gas Masks

HexKagune 4.1.45 for Forge 1.20.1. Not tested in game. **Update the server and every client together** (protocol 119).

Your words: "roaming gangs, such as the clowns, 3-5 roaming ghouls walking together. Gangs needed: Clowns (all ghouls wear different clown
masks (nothing else) and protect eachother) (Non-hostile unless provoked) ... Gas Masks (... protect eachother) (Non-hostile unless
provoked) (they wear nothing but gas masks)".

**Now:**
- **At night, about 3 in 10 of the wild ghouls that turn up are a gang instead:** the Clowns or the Gas Masks (even odds), **3 to 5 of
  them** together, of the ranks one ghoul met there would be.
- **Every member wears a different mask of their gang's and nothing else** (no coat, no armour, nothing to drop). Five clown masks (yours,
  and purple jester, green creep, blue sad, pink candy) and five gas masks (yours, and olive military, yellow hazmat, slate riot with red
  lenses, rust with green lenses): your mask models, new paint.
- **They walk together:** one leads and wanders, the rest keep up beside it (they run to catch up if left behind).
- **Non-hostile unless provoked:** they never go for a player, a human or another ghoul on their own, and they don't hide or prowl like
  lone ghouls.
- **They protect each other:** hit one and every member within 24 blocks comes for you (and keeps at it for 2 minutes); a mob that goes
  for one of them gets the whole gang.
- Their name tags say which gang: "Kaito, B-rank (Clowns)".
- At dawn they slip away like any wild ghoul.

Coming next (what you asked, in order): **4.1.46** the CCG ghoul division (human CCG agents with a random kagune, no kakugan, in trios);
**4.1.47** joining gangs (Aogiri through Ayato and a quest to kill 5 CCG; the dark red name with the tree emblem).

### Other

- Protocol 119 (a ghoul's gang is sent with it).
- The masks preview: `docs/handoff/gang-masks-4.1.45.png` (`tools/GangPreview.java`); the paints from `tools/gang_masks.py`.
