## 1.3.64 (HexGhoul): NPCs see War and Peace when they are worn, and the Rinkaku and Ukaku final kakujas count as final (testing)
HexKagune 1.3.64 for Forge 1.20.1. Install on client and server (**protocol 71**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

A bug found by reading the code (part of your update: "more and better reactions to the final kakujas").

- **The bug:** once the Dragon had been through a player (the save that unlocks War and Peace), every NPC read that player as wearing a final kakuja all the time, worn or not. NPCs only react when what you show gets worse, so they reacted once and then never again: not to War or Peace, not to your kagune, not to a kakuja.
- **Fixed:** the worst sight is now what you wear right now: War or Peace on (not the menu's preview), or the Dragon itself. Take it off and put it on again and they react again.
- **The Rinkaku and Ukaku final kakujas now count as final too.** Before, both read as an ordinary kakuja. Now they get the same reaction as War and Peace (wild ghouls that don't know you run longer, even one that was hunting you, with their worst lines; Eto, Touka and Ken say their final-kakuja lines and Eto always reacts, however recently she last did), and going from the first kakuja to the final one is noticed as it happens.

What each NPC says and does at these sights is still the old set. The next version (1.3.65) gives the final kakujas their own reactions, with War feared most.

Untested in game: compiled and unit tested only. Try: put War on near Eto or a wild ghoul, take it off, put it on again (both times they should react); go into Rinkaku's or Ukaku's final kakuja near them.
