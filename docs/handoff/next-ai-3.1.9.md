# HANDOFF: HexGhoul after 3.1.9. Read this first.

Then, for the house rules and history: `docs/handoff/next-ai-2.0.45.md` (header: versions, protocol history, the 2.x/3.x log),
`docs/handoff/next-ai-1.3.60.md` (the owner's house rules in full), and the top sections of `docs/handoff/companions-handoff.md`
(one per version, newest first: 3.1.5 to 3.1.9 are this session's, with file-level detail).

## Where things are

- **Branch:** this session's work is on **`ccr-d6106b24-xl17h5`** (pushed; 3.1.5 to 3.1.9 on top of 3.1.4, which was the newest branch,
  `claude/loving-albattani-xld4zv`). The next session will be given its own branch: start it from `origin/ccr-d6106b24-xl17h5`
  (`git fetch origin ccr-d6106b24-xl17h5 && git checkout -B <your-branch> origin/ccr-d6106b24-xl17h5`). Push only to the branch you are
  given; never to `main`; no PR unless asked.
- **Version 3.1.9** in `build.py` and `patch/KagunePatch.java`. **The next is 3.2.0** (the last number never reaches 10: after x.y.9
  comes x.(y+1).0). **Protocol "92"** (`ControlNetwork.PROTOCOL`).
- Full `python3 build.py` ended **EXIT 0** for every version this session (javac `--release 17` on JDK 21 here). **Nothing is tested in
  game. Never claim it is.**
- The quick loop: `python3 tools/quick_check.py [--noverify] ControlTests DialogueTests ...` (needs one full `build.py` first).

## What this session shipped (each its own version)

| Version | What | Key files |
|---|---|---|
| 3.1.5 | **Impale** on H (Crush) when no tail holds anyone: a random tail (never a centipede leg) rises over its shoulder and shoots straight out along the look, sharpened end, held = longer (2.1 b/t, 16 to 30 blocks), drags what it runs through, pins at walls, tears out on release. With someone held, H is still the crush. | `RinkakuImpale`, `ImpaleServer`, `ImpaleClient`, `S2CImpale` (52), `C2SUkakuAction.IMPALE` (10), `RinkakuRibbon.width/build(..., sharp)`, `UkakuGates.crush` |
| 3.1.6 | **Rubble Hurl** on N (Throw) when no tail holds anyone (the owner picked this idea): tails tear a chunk out of the aimed block or the ground (really removed via `BlockSmash.tear`, permission-checked, restored after the config time), hold to grow it, release to lob it onto the aim point; bursts (falloff damage, knockback, stun, crater at 75%+ charge). 3 pieces in the final kakuja. | `RinkakuHurl`, `HurlServer`, `HurlClient`, `S2CHurl` (53), `C2SUkakuAction.HURL` (11), `ItemDisplayContext.NONE` allowlisted |
| 3.1.7 | **Fix (owner's bug report):** the hurl's gripping tails went through the body. Now routed out of the back, round their own side, up behind the shoulder beside the head (`RinkakuHurl.grip`). 4,400-pose regression check `ControlTests.hurlGrip`. | `RinkakuHurl.gripLaunch/grip`, `HurlClient.apply` |
| 3.1.8 | **Signature moves on Sneak + M1** for the kagune-made weapons the owner meant ("the heavy blade, hammer and whip"): Tendril Whip **Tendril Swing** (lash a block, get hauled to it; latch packed into the swing packet), Flesh Hammer **Fault Line** (ground breaks open in a line), Bone Greatsword **Bone Wall** (guard x0.25; perfect parry in 5 ticks staggers and readies the overhead cleave). Investigators unchanged. | `Arm.Ability` SWING/FAULT/GUARD (appended), `Arm.signature()`, `PlayerArms`, `WeaponPose`, `WhipPath.swing/packAnchor`, `C2SArmAction.THIRD` (4) |
| 3.1.9 | **Blended family:** a partner who sees you being sweet on another says so (gesture, jealous 2 days); two partners near each other talk (first meeting / rival / kids / settled, 18 talks); half-siblings talk about their other mothers and fathers (8 talks); new scene conditions `cowife`, `halfsib` + 13 scenes. | `family/Blended`, `BlendedServer`, `RomanceServer.romanced`, `SceneWords`, `SceneServer`, `talk/*.txt` |

Previews (off-game geometry, looked at honestly): `docs/handoff/rinkaku-impale-3.1.5.png`, `rinkaku-hurl-3.1.7.png`,
`weapon-signatures-3.1.8.png`. Tools: `tools/ImpalePreview.java`, `tools/HurlPreview.java` (both apply the game's own BodyClearance and
RinkakuRoot.attach), `tools/WeaponPosePreview.java` (now draws the signature row), `tools/QuinquePreview.java`.

## Still open from the owner's last list (do these next, each its own version)

The owner's message (trimmed): "add more interactions ... improve the move sets of the briefcase [done: 3.1.8] ... more conversations,
like bringing the child home, like more diverse conversations, depending on how the character is or who it is, the personality they
have. And ... make the relationships even more complex [done: 3.1.9] ... as immersive as possible, so don't just quote what I'm saying.
You gotta make up your own shit."

1. **3.2.0: more "bringing the child home" conversations, by personality.** Not started. The scene system is data: `talk/eto.txt`,
   `touka.txt`, `ken.txt`, `child.txt` (format in `SceneBook`; every allowed word in `SceneWords`; `SceneTests` validates: option
   labels at most 50 characters, child effects from `CHILD_EFFECTS` only, e.g. no `laugh` for children). Useful conditions: child flags
   `home`, `carried`, `siblings`, `halfsib`, moods `missed`/`scared`/`sleepy`, tempers (`sunny shy bold curious mischief gentle serious`)
   via `mood:`/`who:`; companion flags `kids`, `home`, `away`, `following`, `cowife`. Write new lines in each character's voice (Eto sly
   and theatrical, Touka blunt and secretly soft, Ken gentle and apologetic), by the child's temper and age. Also consider ambient lines
   when a parent actually brings a child home (the existing fetch-home flow: `ParentDuty`/`Conversation` "word at the door").
2. Then ask the owner what to deepen next (see the questions below).

## Withheld (ask before doing anything with it)

Before the owner clarified "the briefcase" meant the heavy blade/hammer/whip, two moves each were built for the four *first* melee
quinques (Bikaku Blade, Katana, Koukaku Spear, Flesh Mace: Flurry/Riposte, Flash Step/Crescent, Lunge/Vault, Ground Pound/Skybreaker)
with right click no longer folding them. It was **not shipped and not kept** (it would undo the owner's 1.3.33 ask that a tap of right
click folds a melee quinque). If the owner wants it, it has to be rebuilt; ask first.

## Judgment calls the owner hasn't confirmed (tell them, ask if unsure)

- **Impale "drag":** read as carried forward on the spear (they drop where it tears out), not pulled back to the player. Its line
  follows the look slowly (9 degrees a tick), not fixed at the press. Unlocks with the crush (mastery 350).
- **Rubble Hurl:** the block really leaves the world but comes back (BlockSmash rules; block smashing off in the config = world
  untouched). Unlocks with the throw (250). The block underfoot is never taken.
- **Bone Wall / Fault Line / Tendril Swing numbers** are first guesses (cooldowns 4 / 6.5 / 3.5 s; perfect parry 5 ticks).
- **Blended family:** partners talk at most once per half day per pair; witnessed jealousy lasts 2 days. It may feel too frequent or too
  rare in play.

## Things to watch when the owner plays (all untested in game)

- Impale: the tail's straight line and point drawn at long range; carried mobs' movement looks (server-moved each tick); players carried.
- Rubble Hurl: the chunk drawn as the block's item model (`ItemRenderer.renderStatic`, `ItemDisplayContext.NONE`) for unusual blocks;
  BlockSmash restoring the torn block.
- Tendril Swing in tight spaces (it stops short of the latch; falls forgiven via `safeUntil`).
- Blended talks: two companions starting a talk while one is doing chores or following the player.

## House rules (unchanged; full text in `next-ai-1.3.60.md`)

- Each fix or feature its own version: both version strings, `.github/release-notes-<v>.md`, a README entry, a new top section in
  `companions-handoff.md` (rename the old START HERE to "State after ..."), the header lines of `next-ai-2.0.45.md`, full `python3 build.py`
  to EXIT 0 (don't edit sources while it runs), then commit (`3.x.y: ...`, the session's Co-Authored-By and Claude-Session trailers,
  no model names in anything pushed) and push. Stage files explicitly; never commit scratch files.
- Owner's bug reports first, each its own version. Answer the owner in English, plainly; say what is untested and what was approximated.
- Banned in the jar: Kaneki, Ayato, Furuta. Banned in visible text: Takatsuki, Aogiri, Yoshimura. Sounds only from the hexkagune: pack.
  Never alter the owner's files (skins, their sounds).
- New Minecraft API: an allowlist entry with [srg]/[order]/[forge] evidence (`tools/find_srg.py`, `build/mapping-cache/joined.tsrg`) plus a
  stub; stale entries fail the build. Ordinal-sent enums: append only. Server maps of game times through `WorldTimes.map()/kept()`.
- Look at your own previews honestly (the owner caught tails through the body in a preview this session: render with the game's own
  BodyClearance, and turn visual promises into numeric tests where you can).
