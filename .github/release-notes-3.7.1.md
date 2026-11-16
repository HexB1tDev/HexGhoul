## 3.7.1 (HexGhoul): Bikaku: The Walker on tap K, carried 3.5 blocks up over any ground

HexKagune 3.7.1 for Forge 1.20.1. Not tested in game.

Your words: "the walker should lift the player way higher up (like 3-4 blocks up)  and adapt better to literally any terrain, the arms should not swing at any point (they currently do). the walker is the first tap K half kakuja, the full kakuja is the hold K". Asked its cost, you chose "Half the kakuja's cost (Recommended)".

- **K tapped: The Walker** (mastery 750), a half kakuja. It costs 12.5 energy to go in, then drains half the kakuja's rate. Tap K again to leave it; it also ends when your energy runs dry ("Your energy ran dry: The Walker lets you down"), when the kagune goes away, or when you die. Not while a worm is feeding (as a form change).
- **K held: the kakuja** (mastery 1000), as before; from The Walker it goes straight into the kakuja. K, tapped or held, takes you out of the kakuja.
- **Before:** K tapped was the kakuja, and The Walker was a form picked in the menu.
- **The menu:** The Walker is no longer clicked. Its row reads "tap K" (or "ON"), with a line under the forms on what it costs. The kakuja reads "(hold K, mastery 1000)". A profile that had The Walker picked now has Three Worms.
- **The move guide:** "K tap: The Walker (in · out)" and "K hold: Kakuja".
- **Carried 3.5 blocks up for real:** your position, hitbox and camera are up there, on two long legs standing wide (2.1 blocks out to each side, reaching up to 6.6 blocks). The legs grow out of you over 0.7 seconds and go back in as fast.
- **Any terrain:**
  - You're carried 3.5 blocks over the highest ground within 1.2 blocks of you: steps, rocks, ledges, roofs, tree tops, anything up to 2.5 blocks above your feet.
  - Under a roof or a ceiling, you're let down so your head clears it. A wall or a trunk beside you doesn't count as a ceiling.
  - You climb walls you walk into, as before, but never the underside of a roof: you're let down under it instead.
  - With no ground within reach below, you fall, with no fall damage.
- **Moving:** you go at a walking pace, as on the ground (in mid-air Minecraft's own pace is a crawl). You rise and sink to your carried height smoothly, at most 0.4 blocks a tick up and 0.45 down.
- **No fall damage** while carried and for 3 seconds after leaving The Walker, as you're let down. On a server without flight allowed, the 4-second "flying" kick is held off while you're carried.
- **Your arms and legs** hang still (3.7.0). Your body is no longer drawn lifted, since it really is up there.
- **Moves in The Walker:** Three Worms', as before.
- **My guesses:**
  - The numbers: 3.5 blocks up; the stance and reach; felt 1.2 blocks round you; up onto things 2.5 above your feet; the rise and sink speeds; the 0.7 s growth; the 3 s fall grace.
  - No cooldown for The Walker (you didn't name one). The kakuja's own cooldown is unchanged.
  - The flying kick held off by resetting the server's own count each tick, through Forge's reflection helper.
- **Known limits:**
  - Other players see you up there because your position is up there.
  - Mobs that can't reach 3.5 blocks up can't hit you.
- Preview: `docs/handoff/bikaku-walker-3.7.1.png` (sent before building): at rest from eight sides, walking, up steps and rough ground, under a cave roof, up a wall and over it, off a ledge.
- Network: protocol 103 (unchanged; The Walker is sent as the form, as before).
- Tests: BikakuWormTests `walkerLift()`:
  - the carried height: 3.5 on flat ground; up onto a step, a ledge or a bridge; let down under a cave roof and under a room's ceiling with a floor on top; unchanged beside a trunk; falling over nothing;
  - the spring: never faster than its pace, never past the height, there in a moment;
  - the climbing rule: a wall yes, a roof's underside no;
  - both feet planted wide on the ground from 3.5 up, within reach, each leg arching out and down onto its foot;
  - half the kakuja's cost;
  - read from the sources: K tapped is The Walker and held the kakuja; it pays, drains, takes no fall and is never kicked for floating; Bikaku's K is timed; the menu doesn't make it clickable.
  - Also `forms()` (an old pick of The Walker is Three Worms) and the full suite.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 103. Not tested in game.
