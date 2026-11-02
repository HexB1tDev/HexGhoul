## 1.3.47 (HexGhoul): children are as tough as their parents: they grow into the stronger parent's health and more, and heal on their own (testing)
HexKagune 1.3.47 for Forge 1.20.1. Install on client and server (**protocol 68**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

- **The stronger the parents, the stronger the child.** A child grows into the stronger parent's health, plus up to 14% more for the other parent's strength (a player's kagune mastery, more with a final kakuja chosen).
  - Your own example works out: a mother with 360 health and a father at full mastery give a child that grows into **400**.
  - With Touka (260), Eto (300) or Ken (340) and a strong player, children grow into roughly 290, 335 and 380.
- **By stage:** a baby has a quarter of it, a toddler 40%, a child 65%, and a teen all of it. It's never less than before.
- **Children from before** get theirs worked out once a player parent is online, and keep how hurt they were (the same share of the new most).
- **They heal on their own** a few seconds after they were last hurt: a ghoul child fully in about two minutes, a human child in ten.
- `/kagune children` now shows each child's health and what it grows into.

Untested in game: compiled and unit tested only.
