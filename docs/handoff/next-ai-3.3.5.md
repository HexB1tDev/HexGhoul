# HANDOFF: HexGhoul after 3.3.9 (kept current as versions ship). Read this first.

Then, for the house rules and history: `docs/handoff/next-ai-3.2.1.md` and `docs/handoff/next-ai-3.1.9.md` (the two sessions before,
their judgment calls), `docs/handoff/next-ai-2.0.45.md` (header: versions, protocol history, the 2.x/3.x log), `docs/handoff/next-ai-1.3.60.md`
(the owner's house rules in full), and the top sections of `docs/handoff/companions-handoff.md` (one per version, newest first: 3.2.7 to 3.3.5
are this session's, with file-level detail).

Note on the session before this one (3.2.2 to 3.2.6, branch `claude/magical-fermi-cec8n1`): it wrote a handoff at 3.2.1 and then went on
for five versions without updating it; its parked work (a break-up/divorce `git stash`, a move-guide patch) was only in its container and was
lost. **Commit and push every version as it is built, and keep this file current**: this session's worker was restarted four times.

## Where things are

- **Branch:** **`claude/exciting-bell-3kklnc`** (pushed; 3.2.7 to 3.3.9 on top of 3.2.6 from `claude/magical-fermi-cec8n1`). The next session
  will be given its own branch: start it from `origin/claude/exciting-bell-3kklnc`
  (`git fetch origin claude/exciting-bell-3kklnc && git checkout -B <your-branch> origin/claude/exciting-bell-3kklnc`). Push only to the
  branch you are given; never to `main`; no PR unless asked.
- **Releases:** `.github/workflows/release.yml` publishes `v<version>` on GitHub for every push to `claude/**` that adds
  `.github/release-notes-<version>.md` (about 5 minutes). The owner plays those. When they say "push an update", push what is built.
- **Version 3.3.9** in `build.py` and `patch/KagunePatch.java`. **The next is 3.4.0** (the last number never reaches 10). **Protocol "93"** (3.3.6: EtoGesture JEALOUS appended).
- Full `python3 build.py` ended **EXIT 0** for every version (javac `--release 17` on JDK 21; about 5 minutes; run it in the background).
  **Nothing is tested in game. Never claim it is.**
- The quick loop: `python3 tools/quick_check.py [--noverify] KingTests UkakuTests ...` (needs one full `build.py` first). It does **not**
  rebuild the Dragon's `awakening/` code (Mojang names, `-Werror`): only the full build checks that.

## What this session shipped (each its own version)

| Version | What | Key files |
|---|---|---|
| 3.2.7 | **Fix:** Peace's Aegis shields were knight's heater shields hanging off their tendrils, the top one over his head: now fans of crimson flesh on seven bone ribs grown on each tendril's end, the top one a hood behind his head. | `PeaceFxModel.shield/edge/stalk/shieldRest`, KingTests `aegisGrown` |
| 3.2.8 | **Fix:** War's Spire Wave (B) in first person: the left arm drawn in the hand pass (`PlayerRenderer.renderLeftHand`, allowlisted), coils round it, the beam a cone out of the fist. Third person byte-identical. | `king/SpireArm`, `KingFirstPerson.spireArm`, `WarHoldModel.coils/mass` |
| 3.2.9 | **Fix:** the Wing Vortex: a thin round corkscrew straight along its flight; flown straight forward (it dived at a body's middle into the ground and ended: "stays in place"); a see-through jet in first person. | `WingVortex` (rebuilt), `UkakuRules.vortexHeading`, `UkakuFx` |
| 3.3.0 | The move guide: Rinkaku's rows follow whether a tail holds someone. | `AbilityGuide.Input.holding`, `AbilityHud` |
| 3.3.1 | Ken teaches the Rubble Hurl and the Impale, after proofs (5 throws, 5 crushes); never grandfathered. | `Gate.RINKAKU_HURL/IMPALE`, `Teaching` (`inOrder`, proofs), `UkakuGates` |
| 3.3.2 | Dawn has an idle (breathing, a turn in the fingers, carried aside at ease). Hold and M1s unchanged. | `QuinquePose.idle`, `QuinqueClient.stillFor` |
| 3.3.3 | The Kakuja V2 Dragon has a voice: 14 real CC0 recordings (roars, a death cry, crawl, quakes, bites). | `sfx_pack.py` `dragon_*`, `DragonTimeline.roars/crawls`, `DragonSession` |
| 3.3.4 | One partner, for good (no break-up, no divorce): nobody new while you have a partner anywhere; killing Eto/Touka/Ken never ends it; a villager partner's death frees you. | `server/Vows`, `Romance.endsWithDeath/villagerKey`, `EtoServer`, `HumanTalk` |
| 3.3.5 | Crystal sounds from real glass recordings (3.2.6's were pitched-up steel). | `sfx_pack.py` crystal pools, `sfx_recordings.json` |
| 3.3.6 | A jealous partner acts it out (back turned, glare, huff, snub, foot tapping), when you ask someone else out or confess to a villager while spoken for; new lines in voice; replayed now and then while jealous. Protocol 93. | `EtoGesture.JEALOUS`, `EtoPose`, `RomanceServer.tempted`, `Blended.tempted`, `BlendedServer.sulks` |
| 3.3.7 | Talking it out: "Can we talk about it?" / "Are we okay?" / "Are we still friends?" for a cross companion, "Can we talk about earlier?" for a jealous partner: branching talks that end it (`mend`) or make it worse (`sulk`); a second way each comes to make up. | `talk/*.txt` `mend_*`, `make_up_2`, `jealous_over_2`; `SceneServer.mend/variant`, `SceneBook.variantOf` |
| 3.3.8 | Door scenes by the brought child's temper (7, found or outing) and age (baby, toddler, child or older): 19 `door_*` scenes per companion. | `Homecoming.flags`, `SceneWords` (`<t>kid`, `babykid/littlekid/bigkid`), `SceneServer` |
| 3.3.9 | Homecomings without a home: the place a child lives counts (16 blocks); house-and-door homecoming scenes wait on `home`; new `!home` scenes (4 per companion, 7 for children). | `ChildBrain` home check, `Homecoming.NO_HOUSE`, `talk/*.txt`, SceneTests `HOUSE` |

Previews (off-game, looked at honestly): `docs/handoff/peace-aegis-3.2.7.png`, `war-spire-first-person-3.2.8.png`, `ukaku-vortex-3.2.9.png`,
`dawn-idle-3.3.2.png`, `jealous-3.3.6.png`. Tools: `tools/art-preview.sh <out> aegis|spire-fp|vortex`, `tools/QuinquePreview.java <out> dawn idle`.

## The owner's words this session (binding)

- Peace's keys "not matching the other forms'" (a 3.2.6-era report): **"Ignore that bug its done."**
- War's janky first-person move: **"The move that charges centipides from the left arm and shoots them forwards in a beam of sorts of
  centipides."** (the Spire Wave, B).
- Wing Vortex: **"Basically the vortex is meant to be less janky, thinner and more circular, cant see anything in first person and glitches
  a ton instead of going straight forward somestimes it stays in place, its really really strange."**
- Dawn ("hold, ambient and M1s"): asked, they picked **only the idle/ambient**.
- The centipede dragon's sounds: **the Kakuja V2 Dragon is silent.**
- Break-up/divorce, replaced: **"I changed my mind. From now on, you can only have one wife, no matter what, okay? ... There's no divorce
  button. No anything. When you're married to them, that's it. The only way you can remarry is if you kill them. But if it's an official NPC
  like Toka, Ken, or Ito, there's no way to unmarry or to anything. You guys are stuck forever."** Never build break-ups or divorce.
- Sounds: **"Get the sounds from the web, don't generate them yourself. You suck at that."** Never make a sound by pitching, layering or
  synthesis: fetch real recordings (see below).
- **"push an update asap"**: push each version the moment it builds.

## Still open (do these next, each its own version; the owner's 3.2.2 list, what is left of it)

From the owner's message after 3.2.1 (nine asks; 3.2.2 did "one at a time", break-up/divorce was replaced by 3.3.4):

1. ~~A jealous animation~~ (done: 3.3.6).
2. ~~Reconciliation talks~~ (done: 3.3.7).
3. ~~Temper-aware door scenes~~ (done: 3.3.8).
4. ~~Homecomings without a home~~ (done: 3.3.9).
5. **Siblings** at home reacting to the one brought back ("where WERE you?", the little one tattling, the big one covering for it).
6. **Companion-child door talk:** the companion and the child talking to each other at the door after the player's scene (a `Conversation`,
   as `BlendedServer` does).
7. **Kids greeting you after days away** (the child greeting the player who comes home after days away, by temper).

Family/scene system notes are in `next-ai-3.1.9.md` and `next-ai-3.2.1.md` (scenes are data: `talk/*.txt`, `SceneBook`, `SceneWords`,
validated by SceneTests).

## Judgment calls the owner hasn't confirmed (tell them, ask if unsure)

- **3.2.7:** the look of the bulwarks (a fan of flesh on bone ribs; the top one a hood behind the head); at rest from the front the side
  ones blend into the crimson mass.
- **3.2.8:** the first-person arm placement and the beam's cone (0.26); not seen how the hand pass's FOV and view bobbing line the arm up
  with the beam, which stays in the world.
- **3.2.9:** "more circular" read as a round corkscrew round a straight line; the pitch limits (-0.22 to 0.3 of the heading) and the
  18-degree aim assist. The owner's 2.4.6 reference gif was never in the repo: ask for it if this isn't it.
- **3.3.1:** five throws / five crushes; never grandfathered (so even the owner's maxed save goes to Ken); a maxed Rinkaku's clash gate
  share is 9/11 until taught.
- **3.3.2:** stiff arms can't rest Dawn on the shoulder (the hand would be over the head) nor point it down (into the ground): it is carried
  aside at ease instead; "hand on hip" is the left arm angled out.
- **3.3.3 / 3.3.5:** which recordings: chosen by name and measured loudness, **never heard** (no audio here). freesound.org is blocked, so
  each sound's own page couldn't be re-checked: CC0 per the WilhelmSFX mirror's licence. Ask which sound wrong.
- **3.3.9:** a child put down elsewhere settles there, so that becomes the place it lives; the 16 blocks.
- **3.3.8:** a child and a teen share the `bigkid` scenes; weights (about 4 in 10 by temper, 4 in 10 by age, the rest the old ones).
- **3.3.7:** the talk doesn't know what made them cross (a hit or a bad answer); villager partners have no such talk.
- **3.3.6:** the turn away is about 55 degrees (the glare over the shoulder must reach you); villager partners don't act jealous (no
  gestures); asking someone out / confessing while spoken for is the trigger (the one-partner rule leaves nobody to kiss).
- **3.3.4:** a villager partner's death frees you whoever killed it (the owner said "if you kill them"); only partners online in its world
  when it dies are freed; old saves keep partners they already had.

## Things to watch when the owner plays (all untested in game)

- Peace's bulwarks swinging out to meet blows (they stay one piece with their tendrils: tested), and from the front at rest.
- War's B in first person: the arm, the coils, the beam leaving the fist; an off-hand item hidden during it.
- The Wing Vortex: flying straight; ending only at real walls; the first-person jet.
- Ken's two lessons and their proofs' action-bar counts; the guide's "Ken teaches it" rows.
- Dawn's idle starting after 1.5 s still and stopping on any move.
- The Dragon's voice (volume, how often it roars: every 5.5 s) and the crystal sounds.
- A second partner refused everywhere; killing Eto/Touka/Ken keeping the marriage.

## How to get sounds (the owner's rule: real recordings from the web)

- freesound.org, OpenGameArt, Wikimedia, Pixabay and Sonniss are blocked from these sessions; `raw.githubusercontent.com` is not.
- The WilhelmSFX sound bank mirrors freesound recordings on GitHub under CC0:
  `https://raw.githubusercontent.com/Wh1teDuke/WilhelmSFX/ec4875a35d265b5431e3ba81fef6cc0ca0281bfc/` (`LICENSE` is CC0; `config.yaml`
  names every sample, `Samples/<id>__<user>__<name>.wav`; `README.md` links each to its freesound page: take authors from those links).
  Take only freesound-sourced ones (its Sonniss/Bluezone/CREA ones aren't CC0).
- Add each source (title, author, page, download URL, sha256) and clip (excerpt, optional `gain_db` for a quiet one) to
  `tools/sfx_recordings.json`, put the downloaded files (named as the manifest's sources) and the shipped `CC0-1.0.txt` in a directory, and
  run `python3 tools/sfx_pack.py --import-recordings <dir> --only <clip,clip>`: it encodes only those, keeps every earlier clip byte for byte,
  and rewrites `sfx/recorded/CREDITS.txt` and `SOURCES.json`. Then add the pool in `POOLS` (no pitch) and run `python3 tools/sfx_pack.py`
  (regenerates sounds.json; diff it: only your pools should change). Subtitles go in `en_us.json`.

## House rules (unchanged; full text in `next-ai-1.3.60.md`)

Each fix or feature its own version: both version strings, `.github/release-notes-<v>.md`, a README entry, a new top section in
`companions-handoff.md` (rename the old START HERE to "State after ..."), the header lines and the log line of `next-ai-2.0.45.md`, full
`python3 build.py` to EXIT 0 (don't edit sources while it runs), commit (`3.x.y: ...` with the session's trailers, no model names in anything
pushed), push. Owner's bug reports first. Banned in the jar: Kaneki, Ayato, Furuta; in visible text: Takatsuki, Aogiri, Yoshimura. Sounds only
from the hexkagune pack (real recordings). Never alter the owner's files. New Minecraft API: an allowlist entry with [srg]/[forge] evidence plus a
stub. Answer the owner in English, plainly; say what is untested and what was approximated. When the owner's exact words for a report are
missing, ask them (AskUserQuestion) rather than guess.
