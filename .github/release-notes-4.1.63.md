## 4.1.63 (HexGhoul): the gender screen in the quest menu's style

HexKagune 4.1.63 for Forge 1.20.1. Not tested in game. Protocol unchanged (125).

Your words (your list after 4.1.49): "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as
interactable as it is, Dialog, mastery menus ALL OF THEM". The screen after the kakugan screen's Next ("Who are you?").

**What it was:** a dark overlay over the whole screen, two flat cards and two flat buttons on it.

**What it is now:** a small quest menu box over the world (no overlay round it, up to 440 x 300):
- "Who are you?" as the menus' title, its line in red, the honeycomb and the rule under the header.
- Male and Female as quest cards: lifted under the mouse, lit red with a beating bar once picked; the figures as before (hers smaller,
  slim arms, wider hips, the bust).
- Done ("Pick one" until you have) and Back as the quest menu's buttons; the hint at the foot when there is room.

### Other

- Render: `docs/handoff/gender-screen-4.1.63.png` (nothing picked with the pointer on Male; Female picked with the pointer on Done; a
  320 x 200 screen; GUI scale 2 at 1080p).
- Tests: the layout at six screen sizes, now also the box on screen and everything in it, under its header.
- Next: the path choice, then the king's choice and the cookbook, one a version.
