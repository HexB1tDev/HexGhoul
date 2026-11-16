## 2.3.9 (HexGhoul): an unwanted kiss let go, never stuck

HexKagune 2.3.9 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Fix: a spouse could get stuck leaning in for a kiss you didn't want. She now never stays leaning in for long, and turning down a kiss she came asking for is no offence.

- **A kiss she asks for herself** ("...wants a kiss"): if you walk off, she straightens up with a shrug ("Fine, later.") and moves on. There's no grudge, no sulk and no make-up scene. A tap of Shift still kisses her, and holding still makes her blush and pull you in.
- **Never stuck**: whatever happens (blocked by a wall, out of reach, a missed key), she stops leaning in after at most twice her usual wait and lets it go the same way.
- **A kiss you asked for** and then left hanging still counts against you as before (shrugged off the first time; only repeated ones put her out).
- **Code**: `EtoBrain.Mind.asked`, `EtoBrain.letGoOfKiss`, the cap in her kiss handling. Protocol stays 84. Not tested in game.
