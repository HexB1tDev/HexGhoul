## 4.1.65 (HexGhoul): the king's choice in the quest menu's style

HexKagune 4.1.65 for Forge 1.20.1. Not tested in game. Protocol unchanged (125).

Your words (your list after 4.1.49): "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as
interactable as it is, Dialog, mastery menus ALL OF THEM". The choice after the Dragon: War or Peace.

**What it was:** the old organic board over the whole screen, its veins, the spaced-out title, a thin bar to hold.

**What it is now:** the quest menu's box over the world (the mastery menus' size, no overlay round it):
- "What kind of King will you become?" as the menus' title, "THE DRAGON IS SPENT" in red, how to choose under it, the honeycomb and the
  rule under the header.
- War and Peace as they were: War black with its veins and its one burning eye, Peace white with its feathers and its cross blade
  (sealed until it is built), each card lifted and edged red once picked.
- "HOLD TO BECOME WAR" as the quest menu's button, filling red as you hold it ("BECOMING WAR... 55%"); Decide later under it.
- As before: click a card, hold the button or Enter, once and for good.

### Other

- Render: `docs/handoff/king-choice-4.1.65.png` (nothing picked; War picked with the pointer on it; held half way; Peace picked).
- Test: `QuestTests.kingChoice`: both cards, the button and Decide later to click, inside the box and apart, every word inside it, for
  every pick, hold and pointer; the screen sized from the box, no overlay (proven able to fail: Decide later moved onto the button).
- Next: the cookbook, then the old to-do and your new list.
