## 3.3.5 (HexGhoul): Crystal sounds from real glass

HexKagune 3.3.5 for Forge 1.20.1. Not tested in game.

The owner, on the Dragon's first sounds: *"Get the sounds from the web, don't generate them yourself."* The crystal fix in 3.2.6 was made the same way (the pack's steel and needle sounds raised in pitch into a "ring"), and it asked for real crystal files. Here they are.

**Now every crystal sound is a real glass recording, as recorded** (no pitch change, nothing made over):

- **Shattering:** three real glass breaks: a glass break, a breaking mirror, and another glass break.
- **Forming and a crystal's hit:** real glasses clinking, a single clink, and the first crack of a glass break.

It's the same places as before: Ukaku's crystal shards, the crystal wings, the crystal spear and javelin, glass breaking. They play at the same loudness as before (the shatters a touch louder). No punch or bone in any of it.

**Where they're from.** freesound.org, by avrahamy, LazyLids, mccormick_iain, waheegle and BMacZero, downloaded through the WilhelmSFX sound bank's public GitHub mirror (licensed CC0), the same way as the Dragon's in 3.3.3.

- Every file is credited in `sounds/sfx/recorded/CREDITS.txt`: author, original page, the exact file downloaded, its checksum, and what was done to it.
- What was done: an excerpt, short fades, mono. One quiet glass break had its level raised 6 dB.
- freesound.org itself is blocked from this session, so each original page couldn't be re-opened to re-check its licence. They're CC0 by the mirror's licence.
- I couldn't listen to them here: if one sounds wrong, say which.

- No network changes (protocol 92). No new Minecraft API.
- Tests (SfxTests): every crystal pool is real glass recordings with no pitch and no steel sounds in it, each credited; no punch or bone, as before; a crystal's hit and break still play the crystal pools.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 92. Not tested in game.
