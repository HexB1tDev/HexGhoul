## 4.1.66 (HexGhoul): the cookbook in the quest menu's style

HexKagune 4.1.66 for Forge 1.20.1. Not tested in game. Protocol unchanged (125).

Your words (your list after 4.1.49): "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as
interactable as it is, Dialog, mastery menus ALL OF THEM". The last screen left: the ghoul cookbook (Touka's, for two cocoa beans).

**What it was:** an open book over a dark overlay, turned a spread at a time with arrows in its corners.

**What it is now, the quest menu's box** over the world (no overlay):
- "The Ghoul's Kitchen" as the menus' title, how many dishes in how many chapters.
- **A tab for each chapter** (Basics, Breads, Bowls, Plates, Sweets, Drinks) **and one for the kitchen's own effects.** No more turning
  page by page to find a dish: open its chapter and click it.
- **A chapter:** its picture and its few words; every dish in it as a slot with the dish itself (the name under the mouse).
- **A dish, as a page:** its name, what kind it is (a plate, a bowl, a drink, a snack), how many it makes, its words; the dish on the
  kitchen table at four times its size; its recipe in slots with the real items (hover one for its tooltip; the meat off a human says it
  has no recipe), what it fills (shanks), ghoul or human meat in it, and what it gives (each effect's icon, level and time).
- **The effects:** Refreshed, Replenished, Content and Clear Sight as cards with how many dishes give each; the one picked large, what it
  does, and every dish that gives it as a slot: a click opens that dish in its chapter.
- Arrows (or the wheel) go dish to dish and on into the next chapter; Home back to the first. It opens where you left it.

### Other

- The cookbook's drawing is now a pure view (`CookbookView`), so it renders outside the game: the menus' canvas learned to draw an
  item, a texture and the game's words. In the renders the game's own items (a bowl, wheat) and effects (Strength) are plain squares,
  since only the mod's own art is there; in the game they are the real ones.
- Render: `docs/handoff/cookbook-4.1.66.png` (The Basics; Bowls with a dish picked and the pointer on an ingredient; Plates with twenty
  dishes; the effects with Replenished picked).
- Every screen of the mod is now in the quest menu's style. Next: the old to-do (Ayato and Touka's marriage reactions, a spouse on gang
  life, "Quit the gang", the marriage and membership rules), then your new list, then the hotel.
