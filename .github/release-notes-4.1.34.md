## 4.1.34 (HexGhoul): the quest menu, a cute box

HexKagune 4.1.34 for Forge 1.20.1. Not tested in game. Previews: `docs/handoff/quests-box-4.1.34.png` (your path's tab over a stand-in
world), `docs/handoff/quests-tabs-4.1.34.png` (the Ghoul's Way, Done, Hunts and Romance tabs).

Your words: "We also need to better the looks of the quest menu. It looks very bad. Very, very. I don't know. I just don't even feel like
opening it half the time. I don't feel like opening it. It's just really dull, really bad looking, not interactive at all." Then: "the
background and all, they're also super ugly." And: "Where does the menu have to fill the entire screen? Why not just like a cute like thing
that still allows you to see the background? Like why not just a cute box?"

**What it was:** a full-screen dark overlay with a wobbly oval board, red veins, a flat list of names, a wall of text and a "still locked"
dump at the bottom.

**Now:**
- **A box**, about two thirds of the screen, in the middle; the world stays visible all round it (no dark overlay). A clean dark plate with
  cut corners, red corner brackets, a soft shadow, a faint honeycomb of RC cells.
- **The header:** your kagune and mastery, and a bar to your path's next quest ("300 / 450 · next: Anteiku's Way").
- **Tabs** as pills with counts.
- **Quest cards:** an icon for where it comes from (a kagune tendril for your path, an eye for the ghoul's way, the moon for hunts, hands for
  favours; a tick when done, a cross when closed, a padlock when locked), a status chip, a percentage and a bar. The card under the mouse
  lifts toward you, the picked one lights up, one that's nearly done glows.
- **The quest:** its title large, its story as a quote, the objectives as a checklist (ticked when met, bars in segments for small counts),
  the rewards as badges (+mastery, unlocks, points, ghoul meat), rival way and side as chips, and a COMPLETED or CLOSED stamp.
- **Your road** (path tab): every quest of your path in order of mastery, as nodes on a line lit as far as you've come (rival ways as a
  fork). Hover a node to see its name and state, click it to open it.

### Other

- Protocol 117 (unchanged).
