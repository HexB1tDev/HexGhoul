## 1.3.52 (HexGhoul): speech bubbles take turns and keep out of each other's way (testing)
HexKagune 1.3.52 for Forge 1.20.1. Install on client and server (**protocol 69**: the speech bubble message carries how long the bubble stays up). Testing build: compiled, tests pass, **not tried in game**.

The owner: "speech bubbles overlapping each other during squabbles". Two causes, both fixed:

- **A talk's turns no longer overlap.** In a squabble, a parent's scolding or any back-and-forth, a line used to stay up for as long as it takes to read while the reply came sooner, so both bubbles were up together for one or two seconds every turn (a toddler on its mother's hip answered right under her line). Now each line stays up exactly until the reply goes up: it shrinks away over its last three ticks as the answer pops in. The same speaker's next line simply replaces its bubble, and a talk's last line stays up as long as it takes to read.
- **A little more time to read each turn.** Because a line now comes down when the reply starts, the pause before a reply is a little longer: about 4 seconds for a typical line (34 letters), 6.3 for a long one (68 letters), never under 2 (it was 3.7 and 5.5).
- **Bubbles that aren't one talk's turns are kept apart.** Two things said at once by different people (two talks side by side, a parent's own remark while a child says something, a word across a squabble) used to be drawn on top of each other. Now, as the camera sees them, two bubbles that would overlap are spread apart sideways, each toward its own speaker's side, with their tails leaning back down to their heads, as a comic places two speakers' bubbles. A bubble moved well aside also rises a little so its tail angles down instead of lying flat. A cloud's puffs count, not just its box. Only when there's truly no room (a crowd) does the newest go up over the others. Moves glide over a few ticks instead of jumping.

Previews (off-game, the game's own shapes and layout code): `tools/BubblePreview.java` now also draws `bubbles-scene.png`, with a squabble and a toddler on its mother's hip before and after, the turn itself, and lines said at once.

Untested in game: compiled and unit tested only. Try: two children at home by day until they squabble, and a toddler that wanders off and is carried home.
