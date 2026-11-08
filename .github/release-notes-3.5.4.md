## 3.5.4 (HexGhoul): Bikaku: the kakuja (K)

HexKagune 3.5.4 for Forge 1.20.1. Not tested in game.

The Bikaku kakuja's body and transformation, as in the previews you saw ("I quite like the kakuji that you made"). Opened by mastery alone.

- **K (mastery 1000), with the worm out:** you grow into the kakuja over 3 s: the worm swells and thrashes, more tendrils come out of your back and wrap round you, lumps of flesh swell up and swallow you, mouths open across them, and six great tentacles burst out. K again lets it fall away (1.5 s). 25 RC to go in, then 1 RC a second; when your RC runs dry it falls away by itself. 60 s before you can go in again.
- **What it is:** a lopsided, breathing mass of seven lumps round you, about 4.8 blocks high, with glowing seams; eight grinning mouths with thick lips and flat teeth, each opening on its own; the worm's own mouth as the great maw; six tentacles (9 to 12 blocks, out every way and curling round it), five of them ending in a mouth. All of it in your kagune colour (teeth and bone keep their own).
- **In it:** you barely move (Slowness VI; you can still jump), you take 45% of every blow, nothing knocks you back, and you heal 1 health every 8 ticks.
- **M1 in it:** one of the tentacles lunges and bites, up to 14 blocks out from the middle of the mass.
- **Your other Bikaku keys** say "not yet" in it: the kakuja's own four moves and its passives come next.
- In your own first person nothing of it is drawn within 2.6 blocks of your eyes (you are inside it).
- The move guide shows K (locked until 1000) and, in it, a KAKUJA board; the menu has a KAKUJA section.

Preview (off-game): `docs/handoff/bikaku-kakuja-3.5.4.png`, `docs/handoff/bikaku-kakuja-close-3.5.4.png`.

- **My guesses:** its whole look, the transformation's timing, the numbers above (RC, drain, cooldown, 45%, reach 14, the heal, Slowness VI), jumping not blocked.
- **Next (your report first):** the worm going through your own body on some moves (Predatory Drag above all); then the kakuja's four moves and passives; then the range you asked for (held, up to about 30 blocks, round blocks); then the new forms.
- Network: protocol 97 (the state carries the kakuja and when it began). No new Minecraft API.
- Tests: BikakuWormTests (the kakuja 4 to 5.4 high, wide and lopsided, its meshes finite, tentacles reaching 7 or more and most with mouths, the growth never jumping either way, the bite landing), BikakuTests.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 97. Not tested in game.
