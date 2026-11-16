## 4.1.56 (HexGhoul): the Ukaku menu in the quest menu's style

HexKagune 4.1.56 for Forge 1.20.1. Not tested in game. Protocol unchanged (125).

Your words (your list after 4.1.49): "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as
interactable as it is, Dialog, mastery menus ALL OF THEM"; and of the quest menu: "Why not just like a cute like thing that still allows
you to see the background? Like why not just a cute box?" The second menu remade, after the talk screen: the Ukaku menu (P on the Ukaku
path). One menu a version from here.

**What it was:** the old organic board filling the whole screen (veined panel, everything at once in three columns), a mutation spent the
moment its card was clicked.

**What it is now, the quest menu's box** (the world seen all round it, no dark overlay):
- **The header as the quest menu's:** "UKAKU", the matrix and your mastery in red, and top right a bar from the last step you reached to the
  next, with what it brings ("760 / 800 · next: Rushing Death (Z), Thorn Fan"; green and full once every step is reached). Chips for
  your points to spend and whether your kagune is out ("R RETRACTS") or in.
- **Two tabs, ORGAN and MUTATIONS** (its badge: points to spend), lit and lifting under the pointer as the quest tabs do.
- **ORGAN:** the five stages (One Wing, Twin Wings, Full Wings, Kakuja I, Kakuja II) as quest cards, each with its own glyph (one wing
  beside the faint ghost of the second, two small wings, two great wings, six curling tentacles, needles and flames), a chip (CURRENT,
  REACHED, TAP K, HOLD K, or "at mastery 750"), the bar to it, and how far along you are for the next one (it glows gold when you're near).
  Click a stage to see it on the right, as a quest is shown: its name, where you stand with it, what it is told as a story, its kagune in
  a framed tile (the wings as big as that stage has them; the kakuja now drawn as six armoured tentacles from your shoulder blades, the
  final one with needles on one side and fire tendrils on the other; a stage ahead veiled with a padlock), the organ's rows, and its
  abilities as a checklist (ticked when reached). Under the stages: your combat readout and the kakugan with its ADJUST button.
- **YOUR ROAD:** every step of the Ukaku (0 to 1000) as the quest road's nodes, ticked when reached, the next one beating; hover one to
  see what it brings, click it to jump to its stage. The picked stage's steps are ringed.
- **MUTATIONS:** the five mutations as cards (UNLOCKED, READY · 1 POINT glowing gold, NEEDS A POINT, NEEDS ITS QUEST). Click one to see
  it on the right: what it does, what it costs (and its quest), **WITH IT**: what it changes in the game's own numbers (Agility Boost:
  Speed II to Speed VI, Jump II to Jump IV at 760; Fire Dance: the vortex's damage, reach and size; and so on), then an **UNLOCK · SPEND
  1 POINT** button. A point is spent only by that button now, never by a stray click on a card. Under the cards: your points, where they
  came from, and when the next comes; at the bottom the milestones that pay a point, on a road.

### Other

- The other menus' pieces are now shared (`QuestFrame`), so Rinkaku, Bikaku, the Graft and Pale will match this one exactly.
- Render: `docs/handoff/ukaku-menu-4.1.56.png` (mastery 300 on the organ, 760 in the first kakuja with a road step under the pointer, the
  mutations with UNLOCK under the pointer, a stage ahead picked).
- Next (your words after this menu): 4.1.57 the structures placed on the ground, the area cleared of blocks and trees first, and the
  CCG building's glass panes joined. Then the menus go on (Rinkaku next).
