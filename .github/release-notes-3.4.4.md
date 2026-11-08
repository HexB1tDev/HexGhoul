## 3.4.4 (HexGhoul): Fix: a crystal forming sounds like crystal

HexKagune 3.4.4 for Forge 1.20.1. Not tested in game.

Your report: *"The ones you added for the crystal form are fucking dog shit, bro. Just download better ones."*

You were right. 3.3.5's sound for a crystal forming was made of kitchen sounds: glasses clinking, a cup on a saucer, and the crack from a glass break. The sound bank I had then had nothing better.

**Now a crystal forming is:**

- **A wine glass being rubbed so it sings:** three separate swells of it, each a short "ring" of a third of a second. Recorded by ani_music.
- **A small bell's bright strike and ring** (about a second). Recorded by AlaskaRobotics.

Each time a crystal forms, one of the four plays at random. They are real recordings, downloaded from Sonic Pi's sample library on GitHub, which collects freesound recordings released as CC0. They're used as recorded: only cut at their quiet points, with short fades, and turned into mono.

It plays wherever "crystal forms" played before: Ukaku's crystal shards, the crystal wings and thorns, the crystal spear and javelin. It's at the same loudness as before.

- **Not changed:** a crystal's **hit** still uses the clinks, and the **shatter** still uses the glass breaks. Tell me if you want those replaced too.
- I couldn't listen to them here (no audio in this session). I picked them by what they are and by measuring them: pitch, loudness, where each swell starts and ends. If they're still wrong, tell me what you'd want instead, for example icier, more of a shimmer, or shorter.
- Credited in `sounds/sfx/recorded/CREDITS.txt` (author, original page, the exact file downloaded and its checksum).
- No network change (protocol 93). No new Minecraft API.
- Tests (SfxTests): a crystal forming uses its own recordings and none of the clinks; the glass and the bell are credited with where they came from.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 93. Not tested in game.
