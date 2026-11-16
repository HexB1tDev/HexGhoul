## 4.1.64 (HexGhoul): the path choice in the quest menu's style

HexKagune 4.1.64 for Forge 1.20.1. Not tested in game. Protocol unchanged (125).

Your words (your list after 4.1.49): "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as
interactable as it is, Dialog, mastery menus ALL OF THEM". The screen where your RC cells wake and you choose your kagune.

**What it was:** a dark overlay over the whole screen, flat cards and a flat button on it.

**What it is now:** the quest menu's board over the world (it holds every path's card, so it fills the screen less a thin edge):
- "YOUR RC CELLS ARE WAKING UP" in the menus' type, its lines in red, the honeycomb only where the header has room, a rule under it.
- Every path a quest card in its own colour at its edge: lifted under the mouse; picked, raised and lit with a bar down its side and one
  beating along its foot. The silhouettes, names, bullets, tags and the stages along the foot as before (hover a stage for its name).
- "Hold to awaken" as the quest menu's button: it fills in the path's colour as you hold it ("Awakening..."); Decide later under it.
- Everything else is as it was: the cards side by side or stacked by screen width, arrows or the mouse to choose, hold Enter or the
  button to awaken, Esc to decide later.

### Other

- The drawing moved out of the screen into `PathChoiceView`, so it can be rendered and checked outside the game: the render is what
  the game draws.
- Render: `docs/handoff/path-choice-4.1.64.png` (GUI scale 2 at 1080p, nothing picked; scale 3 with Ukaku picked and the pointer on
  Awaken; scale 4 held half way; the smallest screen, stacked).
- Tests: the layout at every screen size from 320 x 240 to 3840 x 2160 (5.5 million checks), now also everything on the board and the
  header's rule never on a card or the title.
- Next: the king's choice, then the cookbook.
