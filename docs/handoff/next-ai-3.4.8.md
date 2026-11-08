# HANDOFF: HexGhoul after 3.4.9, the Bikaku path in the game (kept current as versions ship). Read this first.

Then `docs/BIKAKU_PATH.md` (the owner's whole spec for the new path, word for word, what is built, the plan, and every place a path
hooks into). Then, for the house rules and history: `docs/handoff/next-ai-3.3.5.md` (the session before: its judgment calls, how to
get sounds, the house rules), `docs/handoff/next-ai-1.3.60.md` (the house rules in full), and the top sections of
`docs/handoff/companions-handoff.md` (one per version, newest first).

## Where things are

- **Branch (3.4.9 on):** `claude/peaceful-ramanujan-eldmlu` (pushed), started from `claude/wizardly-hopper-h08wf2` (3.4.8 and its
  unversioned Bikaku commit). Start your own branch from the newest one:
  `git fetch origin claude/peaceful-ramanujan-eldmlu && git checkout -B <your-branch> origin/claude/peaceful-ramanujan-eldmlu`.
- **Branch (3.4.8):** `claude/wizardly-hopper-h08wf2` (pushed). It started from tag `v3.4.7`. The two "awaiting approval" Owl mask commits
  after 3.4.7 on `claude/exciting-bell-3kklnc` are not in it: the owner said of that rebuild "Wtf is that model. That looks like
  shit". Start your own branch from this one:
  `git fetch origin claude/wizardly-hopper-h08wf2 && git checkout -B <your-branch> origin/claude/wizardly-hopper-h08wf2`.
  Push only to the branch you are given; never to `main`; no PR unless asked. A plain push is a fast-forward; this session's
  environment refused `--force-with-lease`, so don't reach for it.
- **Releases:** `.github/workflows/release.yml` publishes `v<version>` for every push to `claude/**` that adds
  `.github/release-notes-<version>.md`. The owner plays those.
- **Version 3.4.9** in `build.py` and `patch/KagunePatch.java`. **The next is 3.5.0** (the last number never reaches 10: after 3.4.9
  comes 3.5.0). **Protocol "94"** (3.4.9: KagunePath BIKAKU, S2CBikakuState message 54; the next free message id is 55).
- Full `python3 build.py` ended **EXIT 0** for 3.4.9 and 3.4.8 (about 5 minutes; run it in the background, don't edit sources while it runs).
  The quick loop: `python3 tools/quick_check.py [--noverify] Suite ...`. **Nothing is tested in game. Never claim it is.**
- The commit after 3.4.8 ("Bikaku path started") adds the work in progress below. It has no version and no release, and the tree
  compiles and verifies (`quick_check`, `BikakuWormTests` 26 checks, `GreatswordTests` 49).

## What this session did

| Version | What | Key files |
|---|---|---|
| 3.4.8 | **Fix:** the Bone Greatsword's moves rebuilt with both hands on the grip. A stick arm with the sword set in the fist at one angle can't hold most blade ways two-handed (the left hand hung up to 9 px off the grip), and `settle`'s arm clamp twisted the overhead blows up to 88 degrees in game. Now the weapon turns in the fist (`RigPose.weaponX/Y/Z`). Keys give the blade's way, the hands' place round the chest and the cut's plane, and all six blows and three moves are rekeyed. The hammer, whip and spear are byte for byte as before (every pose diffed), and `settle` is unchanged. | `WeaponPose.hold/Grip/wristed/alongLine`, `RigPose.weapon*`, `WeaponLayer.mesh` (+`ArmItemLayer`), `QuinqueGrip.held/blendTurn`, `AgentClient.flow` (slerp only with a turn), `GreatswordTests`, `docs/handoff/bone_sword-moves-3.4.8.png` |
| (no version) | **The Bikaku path started:** the worm kagune's model and motion (pure), their preview and tests. Nothing is in the game yet. | `control/bikaku/MawWorm`, `MawMotion`, `tools/MawPreview.java`, `BikakuWormTests`, `docs/BIKAKU_PATH.md`, `docs/handoff/bikaku-worm-*-wip.png` |
| 3.4.9 | **Bikaku in the game** (next session): a third choosable path; the worm drawn in the owner's kagune colour, alive at rest, out of the lower back as the kagune comes out, kept long (a bite uncoils it only as far as it needs; the rest stays zigzagging behind); M1 a string of three bites (Snap, Lunge, Maul) with Predatory Feeding; first person kept clear; card, menu, move guide; the original's tendrils and other keys refused. Protocol 94. | `BikakuRules`, `MawMotion.of/path/firstPerson/grown`, `BikakuServer`, `S2CBikakuState`, `BikakuClient`, `BikakuRenderer`, `BikakuMenuView`, `PathSilhouette.bikaku`, `AbilityGuide.bikaku`, `BikakuTests`, `docs/handoff/bikaku-*-3.4.9.png` |

## The owner's words this session (binding)

- The quinques: **"You need to fix the animations of the three new [quinques] the heavy sword, the whip, and the hammer. Especially the
  heavy sword, if you look at the moves, if you look at the sheet, you can tell that the moves are ridiculously stupidly broken.
  Broken in terms of animation is horrible, and that's the most literal way possible. It is completely botched. It's super
  horrible."** Then, with the Noro spec: **"You can leave the hammer and the whip as they are, they're good."** So never touch the
  hammer's or the whip's animations unless they ask.
- The new path: the whole spec is in `docs/BIKAKU_PATH.md`, word for word, with their three follow-ups:
  - **"this [kagune] should be longer than all the others. It should like have zigzags and all ... make sure it's long."**
  - **"something about the tip of the mouth when it's closed doesn't look right ... It doesn't looks like there's"** (cut off).
  - With a picture of the mouth's side: **"Does that look right to you? And that's clearly broken there on the side."**
  - What was done about each is in `BIKAKU_PATH.md`. Ask them whether the shut mouth now is what they meant.
- **"prepare a handoff and a prompt for the next AI to take over."** (This file and `next-prompt-3.4.8.md`.)
- The next session (3.4.9 on), the owner's ask (binding): **"What I want next: my Bikaku path (the worm kagune with the mouth), built in
  the versions planned in `BIKAKU_PATH.md`, starting with 3.4.9: the path in the game with the worm drawn, alive, in my kagune colour,
  with its M1 bites. Keep it long, with its zigzags; I liked that design. Leave the hammer and the whip as they are; they're good. Show me
  previews of what you make before you go far. If I report bugs, fix those first, each its own version; if my exact words for a report
  aren't in the handoff, ask me. When a version is done, tell me plainly what you made, what you guessed at, and what's next."**

## What's next

3.4.9 put the path and the worm in the game (details: the top section of `companions-handoff.md`). Next, in the versions planned in
`docs/BIKAKU_PATH.md`: **3.5.0: Devouring Maw and Predatory Drag** (grabs: hold the target at the mouth, shake, throw it sideways; drag it
along the ground to him, lift, slam), then 3.5.1 Ravenous Pursuit and Carrion Eruption, 3.5.2 Monstrous Regeneration and the passives,
3.5.3 the kakuja's body and transformation, 3.5.4 its abilities. Each goes on a key the original's Rinkaku moves used (they say "Your
worm only bites for now" on this path: `BikakuServer.notYet`); take them before the original's key handling as `GraftInput` does
(a `C2SBikakuAction`, message 55). Show the owner previews early: they look closely at pictures and say plainly when something is broken.
Ask them about 3.4.9's previews if they haven't answered (the shut mouth too: see below).

## Judgment calls the owner hasn't confirmed (tell them, ask if unsure)

- **3.4.9:** the three bites (Snap 12 ticks landing at 6, Lunge 13 at 7 reared up high, Maul 18 at 8 hooked in from the right, shaken,
  stunning 12 ticks and tossing aside); reach 6.5 from over the right shoulder; 0.4/0.4/0.6 RC a bite; heal 1 health a bite, 4 more on a
  kill; no body animation with a bite; in first person the worm's near part moved aside to the top right (only for the player himself);
  no manifest-pain doubling over and no feral idle (the spec: Noro "barely reacts to injuries"); at rest in first person the worm only
  shows looking up; the card's and menu's words; the original's other keys say "Your worm only bites for now".

- **3.4.8:** each blow's angles and timing; the spinning cut now turns right (the blade meets them three-quarters round, so it lands
  at its landing tick); the Leaping Cut's raise grazes the top of the head by under a pixel; the first-person greatsword view is
  unchanged (it was not part of the report).
- **Bikaku (WIP):** no GeckoLib (a procedural mesh bending smoothly along its whole length; the mod has no GeckoLib library and a
  GeckoLib model is cubes, which the spec rules out); the on-screen name "Bikaku"; 6 great fangs and 3 rings of teeth; grey bone; the
  worm about 8 blocks of zigzag coils, its head resting about 3 blocks up over the right shoulder; the shut mouth as a puckered lip
  with the fangs folded over it; the proposed mastery gates in `BIKAKU_PATH.md`.
- **The reference pictures** (`Reference.zip`) are not in the repo: they are described in `BIKAKU_PATH.md`. Ask the owner to attach
  them again.

## Things to watch when the owner plays (all untested in game)

- 3.4.9: the worm on a player in multiplayer and single player: out of the lower back as the kagune comes out (mouth first), its colour,
  its idle (slither, scan, gapes), the bites landing where the server lands them (a target that moves: the worm follows it), the first
  person view on a bite, the RC per bite, the healing, nothing of the original Rinkaku tendrils showing, the action-bar "not yet".

- 3.4.8: the greatsword's blows on a player and on investigators: both hands on the grip, the blade where it should be, no weapon
  jumping when blows chain (the first 3 ticks of a blow blend from the last, turned the shortest way).

## House rules (unchanged; full text in `next-ai-1.3.60.md`)

Each fix or feature is its own version, with:

- both version strings, `.github/release-notes-<v>.md`, and a README entry;
- a new top section in `companions-handoff.md` (rename the old START HERE to "State after ...");
- the header lines and the log line of `next-ai-2.0.45.md`;
- a full `python3 build.py` to EXIT 0 (don't edit sources while it runs);
- a commit (`3.x.y: ...` with the session's trailers, no model names in anything pushed), then push.

Owner's bug reports first. Banned in the jar: Kaneki, Ayato, Furuta. Banned in visible text: Takatsuki, Aogiri, Yoshimura; and since
1.2.5 no character names on screen at all (the paths are named by kagune type). Sounds only from the hexkagune pack (real recordings;
never make one yourself). Never alter the owner's files. New Minecraft API needs an allowlist entry with [srg]/[forge] evidence plus a
stub. Answer the owner in English, plainly; say what is untested and what was approximated. When the owner's exact words for a report
are missing, ask them (AskUserQuestion) rather than guess.
