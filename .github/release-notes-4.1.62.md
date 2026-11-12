## 4.1.62 (HexGhoul): the kakugan screen in the quest menu's style

HexKagune 4.1.62 for Forge 1.20.1. Not tested in game. Protocol unchanged (125).

Your words (your list after 4.1.49): "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as
interactable as it is, Dialog, mastery menus ALL OF THEM". After the five mastery menus, the first of the other screens: where you place
your kakugan (the eyes button in every menu, `/kagune eyes`, and the first time you turn).

**What it was:** a dark overlay over the whole screen, the face and its controls floating on it.

**What it is now:** the quest menu's box over the world (no overlay round it), as big as your screen lets it be up to the menus' size:
- "YOUR KAKUGAN" as the menus' title, its lines in red under it, the honeycomb and the rule under the header.
- Your face in a framed tile whose edge beats, now drawn bigger when there is room (up to 36 GUI pixels a skin pixel, from 28), still
  eight skin pixels square and dragged the same way.
- The controls (Mirror eyes, Eye height, Iris, One eyed, Find my eyes, Reset) and Next / Later as the quest menu's buttons: lifted
  under the mouse with a red bar under them, lit when on.
- "Which is your ghoul eye?" opens as a quest card over the controls; only the box dims under it, your face left lit so the pick shows on
  it.
- Everything works as before: drag an eye, click it and use the arrow keys, Tab, Enter, Esc; on narrow screens the controls go under the
  face.

### Other

- Render: `docs/handoff/kakugan-screen-4.1.62.png` (GUI scale 2 at 1080p with the pointer on Mirror eyes; the which-eye box open with
  Left eye under the pointer; GUI scale 4 at 1080p; a narrow screen with one eye turned).
- Tests: the layout is still checked at every screen size from 320 x 240 to 3840 x 2160 (1.6 million checks), now also that the box
  fits the screen, everything is inside it and under its header, and the which-eye box stays inside it.
- Next: the gender screen (it follows this one), then the path choice, the king's choice and the cookbook, one a version.
