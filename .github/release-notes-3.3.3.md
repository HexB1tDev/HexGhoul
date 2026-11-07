## 3.3.3 (HexGhoul): The Dragon has a voice: real recordings from the web

HexKagune 3.3.3 for Forge 1.20.1. Not tested in game.

From the owner's list: the centipede dragon's sounds. Asked this session: the **Kakuja V2 Dragon** (the one that crawls out of you when you die) is silent. Then: *"Get the sounds from the web, don't generate them yourself."*

**What caused it.** It wasn't quite silent in the code, but nothing reached you:

- Every sound it made came from the pack's quiet sounds, mixed for close-up kagune work: its strain, the clicks of its legs, the bone cracks, its bites, even the break-out. They play at **3 to 37 percent** of full volume (the leg clicks at 2.7 percent, the break-out at 12.5).
- It "breathed" (a strain and a click) only **once every 3.5 seconds**.
- Nothing played as it crawled, and it never roared.

From the camera, following something that size, that came out as nothing.

**Now it has its own voice: 14 real recordings from the web**, played as recorded (no pitch change, nothing made up), at full volume:

- **Roars:** a large animal groan-growl, a demon growl, two lion roars, a monster growling, a beast's roar. It roars as it breaks out of the ground, then every five or six seconds through its rampage.
- **Its death cry:** a big monster's death sound, as it shatters at the end.
- **Crawling:** an insect-animal growl-chitter and a big monster's stomp, every 0.7 seconds while it rampages, moving along its body.
- **Crashing down:** an earthquake, an earth rumble and the stomp, as it erupts out of the ground and when it slams someone into it.
- **Bites:** two crunches and a beast's snarl, as its heads bite and squeeze.
- **Coming apart:** the pack's bone cracks at full volume as it destabilizes at the end of its minute.

Its old sounds still play underneath. Each new sound has a subtitle.

**Where they're from.** All 14 come from freesound.org, by Lewis.B.M, dragonboi50120, craigsmith, NearTheAtmoshphere, JoelEleven, Leadstarson, SecureSubset, Yoyamen1212, anderlk, Reitanna, steenish and qubodup. They were downloaded through the WilhelmSFX sound bank's public GitHub mirror, which is licensed CC0 (the pack's earlier recordings came from the same mirror).

- Every file is credited in `sounds/sfx/recorded/CREDITS.txt`: author, original page, the exact file downloaded, its checksum, and what was done to it.
- What was done: an excerpt (leading silence trimmed), short fades, mono. Three quiet recordings had only their level raised (the death cry +14 dB, the chitter +15, the earthquake +3).
- One honest caveat: freesound.org itself is blocked from this session, so I couldn't open each original page to re-check its licence myself. They're CC0 by the mirror's licence.

**I can't hear them from here.** I picked them by what they are and measured their loudness, not by listening. If any sound wrong or silly, tell me which (the subtitles name them) and I'll swap it.

- No network changes (protocol 92). No new Minecraft API.
- Tests:
  - SfxTests: the Dragon's six pools are there and loud; every sound plays as recorded (no pitch); each is subtitled; each recording is in the pack and credited CC0;
  - DragonRigTest: a roar as it breaks out and every five or six seconds; never more than 0.7 s without its sound in its rampage; quiet (but its cracking) once it comes apart.

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 92. Not tested in game.
