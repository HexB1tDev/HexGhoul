## 1.3.75 (HexGhoul): the villagers' own cute skins (testing)
HexKagune 1.3.75 for Forge 1.20.1. Install on client and server (**protocol 72**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

You asked: "make the humans as cute looking as possible you can also adapt skins I gave you for the kids as long as you make sure they are differently looking from the kids, such as adapting hair color, clothes, etc."

- **Ten new villager looks** (five girls, five boys), made from ten of the children's skins: their **hair, clothes and eyes turned to other colours** (each skin its own colour turn and brightness), their **skin tone and blush left as they were**, whites, blacks and greys kept. So a villager with your son's hoodie wears it in another colour, with other hair.
- **New villagers get one of these looks.** Villagers already in a world keep the vanilla look they had (their look is saved).
- The talk screen shows the villager's new face too.
- Your own skin files are untouched: the new skins are generated copies of the game's processed child skins (`tools/human_skins.py`).

Honest look (from a flat front view, not in game): most of them read clearly different from the children; two (from daughter_08 and daughter_19) are mostly white and grey, so they change only a little.

Untested in game: compiled and unit tested (the skin files are there, new looks map to them, old looks keep vanilla's). Try: find a Brick Village in a new part of the world (plains) and look at its people.
