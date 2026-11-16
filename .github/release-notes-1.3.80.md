## 1.3.80 (HexGhoul): ghouls are living tanks: resistance, not health (testing)
HexKagune 1.3.80 for Forge 1.20.1. Install on client and server (**protocol 72**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

You said: "Mastery & Forms does not increase HP for the player, instead it basically just boosts resistence by a ton, the more mastery, the more ghoul meat eaten, the better the form, etc all those factors makes the player a living tank, to make it more balanced with the NPCS and pvp."

- **No more extra health from forms.** War's +80% and Peace's +100% max health are gone, and so is the centipede forms' Health Boost. A ghoul's health bar stays a player's 20.
- **A living tank instead.** Every blow a ghoul player takes is cut by three things, each one cutting what is left after the one before:
  - **Mastery:** up to 35% off at mastery 1000.
  - **Ghoul meat devoured:** half of its 25% at 20 pieces, and it keeps rising, each piece a little less than the last.
  - **The form you're in:** kagune out 10%, more for each later rinkaku stage (up to 22% at the centipede stage), any kakuja 32%, a final kakuja (War, Peace, the Dragon, a path's final form) 50%.
- **Examples:** a brand-new ghoul with its kagune in takes blows whole. A ghoul at mastery 500 with 20 pieces eaten, kagune out, takes about 66%. Fully grown (mastery 1000, lots of meat) in a final kakuja, it takes about 24%, before War's and Peace's own cuts (25% and 35%), which stay.
- It applies last, after everything else that changes a blow. The void and the kill command are never softened. Humans are unchanged.
- The War/Peace choice screen now says "Toughness" instead of "Health".

Untested in game: compiled and unit tested (each part lowers what is taken, the most is about a quarter, the void is never softened).
