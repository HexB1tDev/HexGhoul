# HANDOFF: HexGhoul after 1.3.51. Read this first, then the top sections of `docs/handoff/companions-handoff.md`.

Branch **`claude/relaxed-carson-4y0frw`** (check it out from origin; push with `git push -u origin claude/relaxed-carson-4y0frw`).
Never push to `main`; no PR unless asked. Version **1.3.51** in `build.py` and `patch/KagunePatch.java` (bump both; the next is
**1.3.52**). Network `PROTOCOL` is **"68"**. Nothing since 1.3.28 has been tried in game: never say anything works in game.

## The next goal (the owner's words, after 1.3.51)

> "The next goal is to increase the amount of interactions and dialog, focusing mainly on the beauty of interactions and making it as
> bug free as possible, prevent NPCs from walking away during conversations for example (not a known bug that I know of but it's an
> example) or speech bubbles overlapping each other during squabbles."

So: **more** interactions and dialog, but **beauty and polish first**: conversations that hold together (everyone stays, faces the
speaker, bubbles never collide, nothing interrupts mid-line), and as few bugs as possible. Read "Known and likely problems" below
before adding anything: they are found by reading the code (nothing has been seen in game), and the two the owner named are both real.

## House rules (the owner's, verbatim where quoted)

- "Rules: nothing is tested in game, never claim it is. Look at your own previews honestly and match my marks exactly. Banned names in
  the jar: Kaneki, Ayato, Furuta. Banned in visible text: Takatsuki, Aogiri, Yoshimura. Sounds only from the hexkagune: pack. Never
  alter the owner's skins (only the generated overlays). Any new Minecraft API reference needs an entry in verify/api-allowlist.txt with
  [srg] or [order] evidence plus a stub; stale entries also fail the build. Append only to ordinal-sent enums."
- SRG lookups: `python3 tools/find_srg.py <yarn class> <member>`; obf names and ids in `build/mapping-cache/joined.tsrg` and
  `intermediary-1.20.1.tiny`.
- Every fix or feature ships as its own version: bump `build.py` VERSION and `patch/KagunePatch.java` VERSION, write
  `.github/release-notes-<version>.md`, add a README entry (same shape as the others), add a new top section to
  `docs/handoff/companions-handoff.md`, run the full `python3 build.py` to EXIT 0 (about 4 minutes now; run it in the background), commit
  as `1.3.xx: ...` with the trailers `Co-Authored-By: Claude <noreply@anthropic.com>` and the session's `Claude-Session:` line, no model
  names anywhere (commits, code, docs), then push. Stage the version's files explicitly; never commit scratch files.
- Bug reports from the owner come first, each its own version. A bigger ask may be split into parts (each its own version); say so.
- Answer the owner in English only. They value companion life, realism and depth; they read release notes and look at previews; tell
  them plainly when you made a judgment call (and what), and when something is only an approximation.
- The quick loop: `python3 tools/quick_check.py FamilyTests` (compile + API verify + the suite, ~30 s; `--noverify` faster). The full
  build is still what must end with EXIT 0.

## What this session shipped (details: the top sections of `companions-handoff.md`)

- **1.3.42** the partner cooks the cookbook's dishes at home (`family/Kitchen.cooking/choose`, `server/HomeKitchen.dinner/cookDish`).
- **1.3.43** children inherit their parents' kagune (`family/KaguneGenes`, `Genome.kagune/colour`), colour from one parent; protocol 68.
- **1.3.44** fix: a Rinkaku child has **one** real Rinkaku tentacle (drawn by the original tube, `KaguneWorldRenderer.emitTubeLegacy`).
- **1.3.45** the tentacle leans right, the wing on the left (an uncrowded back).
- **1.3.46** three layouts by seed: over the right shoulder / the left / a tail with both wings (`ChildKaguneRules.Layout`).
- **1.3.47** child health from the parents' (`ChildRules.grownHealth`: the stronger's health + up to 14%), and it heals on its own.
- **1.3.48** the kit: crystal wings and spikes, the tentacle's grab/throw/crush, back and front flips; **never a kakuja** (the owner).
- **1.3.49** parenting (`family/Parenting`, `ParentingLines` 579 lines, `server/ParentDuty`): fetching a wandering child, night, stopping a
  rampage, scolding talks back and forth in bubbles, a word when a child's kagune comes out.
- **1.3.50** Eto's child: her flesh (rake, grab, club: `server/ChildFlesh`) and her Mutated Arm at low health (`server/ChildArm`); no Owl.
- **1.3.51** brothers and sisters (`family/Siblings`, `SiblingLines` 337 + 12 lines, `server/SiblingLife`): chat, tease, brag, comfort,
  scheme, squabble (a companion parent breaks it up), tag, an older one minding a toddler, side by side in a fight, kagune remarks.

## How conversations work now (read before changing them)

- **Lines are queued, then said one at a time.** A talk is a list of `(speaker entity id, text, tick)` on **one child's**
  `ChildBrain.Mind.talk` (`ParentDuty.Said`), even when the parent or a sibling speaks; `ParentDuty.talk` (called every tick from
  `ChildBrain.tick`) sends each due line as `S2CNpcSay(speaker, text)` to every player within 24 blocks the speaker can see
  (`SceneServer.sees`). Spacing: `Parenting.pause(text)` = max(36, min(110, 30 + 1.3 x length)) ticks.
- **The bubble** (`client/NpcSpeech`): one per entity; a new line replaces it; it stays up `Speech.ticks(text)` = max(60, min(220, 40 +
  2 x length)) ticks. Drawn by `client/render/SpeechBubbles.draw` over the head (`EtoRenderer`, `ChildRenderer`, `GhoulNpcRenderer`),
  shapes in `npc/BubbleShape` (offline: `tools/BubblePreview.java <dir>`).
- **Holding still.** A child stands for a talk while `Mind.stillBy` (who it faces) and `Mind.stillUntil` hold (`ParentDuty.led`, in the
  `ChildBrain.tick` chain after carried/moments/a player's talk/a baby, before its threat). A companion on a duty stands while
  `ParentDuty.Duty.talkEnds` holds (`ParentDuty.tick`, early in `EtoBrain.calm`). Escorting: `Mind.escort` (the child follows that UUID).
- **Order of a child's tick** (`ChildBrain.tick`): stage and health, carried?, `moments(...)` (parent/child moments, ~line 204), then the
  chain: carried -> `busyStanding` (a moment) -> a player talking to it -> a baby sits -> `ParentDuty.led` -> else its threat/fight
  (gated by `Mind.calmedUntil`), running to a parent, following a player, `SiblingLife.tick` (tag, minding, new moments), going to bed.
  After the chain: lines, its message, the talk queue, `outsideSince`, `ChildKagune.tick`.
- **A companion's calm** (`EtoBrain.calm`): holding hands, kiss walk-ups, cling, a player talking to her, then `ParentDuty.tick`
  (~line 838), then `together` with the partner, friend scenes, chores (`HomeChores`), `ParentCare.tend` (her children: moments,
  cradling), kisses, gestures.
- **Words.** `ParentingLines` and `SiblingLines` are plain Java maps ("part:who:why-or-kind:group"); they were written from a script's
  data that is not in the repo: edit the Java directly (keep the key shapes; `FamilyTests.parenting` / `siblings` check every combination
  fills in, alternates, and never names a banned name). The older talk scenes (a player talking to a companion or a child) are the
  data files `src/main/resources/data/hexkagune/talk/*.txt` (`SceneServer`, checked by `SceneTests`).

## Known and likely problems (found by reading the code; fix these first, each its own version)

1. **Bubbles overlap in a back-and-forth (the owner's example).** A bubble lives longer than the gap before the reply (above), so in a
   squabble both kids' bubbles are up at once, a block or two apart, and wide lines collide. Worse: a toddler carried on its parent's hip
   speaks right under the parent's bubble (ParentDuty: the toddler is picked up as the talk begins). Ideas: end the previous speaker's
   bubble when the next line starts (an S2CNpcSay with a shorter life, or a "hush" message: a new client-side bubble field, so a
   protocol bump), stagger heights by speaker order, nudge bubbles apart sideways when two speakers are within ~2.5 blocks of each other
   (client side, `SpeechBubbles`), or make the pause at least the previous bubble's life (slower talks). Preview it offline with
   `tools/BubblePreview.java` before and after.
2. **Walking away mid-talk (the owner's example), the ways it can happen today:**
   - `stillUntil` is capped (`ParentDuty.java:99` now + 400; `SiblingLife.java:108-110` now + 360; `:137-138` + 300; `:203` + 200),
     while a long talk (five lines up to 110 ticks apart) can run past the cap: the child walks off before its last line.
   - **The parent who breaks up a squabble is never held still** (`SiblingLife.breakUp` only queues its lines): she may keep doing a
     chore or walk away while her bubble appears.
   - `moments(...)` and `busyStanding` come before `ParentDuty.led` in `ChildBrain.tick`, so a parent's moment (a hug, a toss) can start
     mid-talk and pull the child away; `ParentCare.tend` can pick a talking toddler up; a player opening a talk with a child steals it.
   - The talk queue lives on one child: if that child unloads or dies, the rest (the parent's lines too) is lost; a participant who is
     hurt, attacked or leaves (> ~6 blocks) doesn't end the talk.
   - **Suggested fix:** one conversation lock: a talk with its participants (ids), its end tick, and who speaks now; every participant's
     brain honours it first (stand, turn the body as well as the head to the current speaker, no new moments/chores/cradling/tag), it
     ends cleanly when the queue empties or anyone leaves, is hurt, or a fight starts (then the remaining lines are dropped, maybe with a
     short "later!" line). `ParentDuty.led` only turns the head (`m_24960_`); turn the body too (`EtoBrain.face` does it for a companion).
3. **Tag runs off home.** The runner picks a point 5 blocks away with no home bounds (`SiblingLife.tag`), so a toddler playing can
   trigger its parent's WANDERED duty; clamp tag to the home's radius.
4. **Two escorts for one toddler.** A parent's duty (`ParentDuty`) and an older sibling's minding (`SiblingLife.minding`) can go after
   the same toddler at once; each overwrites `Mind.escort` every tick. Let a sibling stand down when a parent is already going (or the
   reverse), and say so in a line ("Mom's got you. ...You're so in trouble.").
5. **Eto's child's body isn't posed for her moves.** Her flesh and arm are drawn (`PaleFleshRenderer`, the child's UUID, `drawn` null),
   but the child's body doesn't animate (no `PaleAnimator` on `ChildRenderer`): it stands still while it rakes or hammers.
6. **Speech reaches only players who can see the speaker** (within 24): a talk around a corner is half heard. Fine for realism; maybe
   let a parent/child of the player hear their own family through a wall at a short range.
7. **Calmed children don't defend themselves** for `Parenting.CALMED` (60 s) after a parent stops them; their parent fights instead
   (`EtoBrain.guard`). Probably right, but check it feels fair.
8. Everything in "Things worth checking" below is untested.

## Ideas for more (and more beautiful) interactions, once the above holds

- **Gestures and poses with the words:** a companion's `EtoGesture`s (hands on hips for a scolding, a hug at the end), a child's poses
  (arms crossed for a sulk, a stamp of the foot, hiding behind a parent's leg for SHY) timed to each line; thought clouds (`BubbleShape`
  thought: lines in asterisks) for actions.
- **Group scenes:** the parent, two children and the player together (a meal the partner cooked, eaten together: the 1.3.42 dishes; a
  bedtime story; a squabble the player is asked to judge), with player choices like the existing talk screens.
- **More sibling life:** hide and seek (the owner liked the idea), babies in sibling moments (an older one entertaining a baby), sleeping
  near each other, sharing food, copying the big one, comforting each other after a scolding, rivalry over the player's attention.
- **Children and the player:** reacting to the player's kagune and colour, asking about the other parent, proud "watch me!" moments
  after a lesson, stories about what their siblings did today.
- **Weather and time of day lines,** birthdays (a child's stage change), the first time a child's kagune comes out (a bigger scene).
- Keep every new line set in the same shape (by persona, temper, age), tested for filling in and banned names, and preview bubbles.

## Open questions for the owner

- Eto's child and the Owl: the owner said "Eto's child gets her kagune abilities and transformation at low health" and later "the kid
  will never have access to the [kakujas]" (the owner's word came through as "cockroaches"). 1.3.50 gave her child the Mutated Arm as its
  transformation, never the Owl; the owner was told the Owl could be an exception if they want it. Unanswered.
- Hide and seek and babies in sibling moments: offered, not asked for yet.

## Things worth checking when the owner plays (all untested in game)

- Bubbles in squabbles and a carried toddler's lines (overlap), children walking off mid-talk (see above).
- The child's one tentacle (the original tube built from a hand-made `KaguneClientState.View`): thickness, veins, the point; the three
  layouts; crystal wings; the grab's hold; flips (the child's body turned over by `ChildRenderer.posed`).
- Eto's child's flesh and Mutated Arm at its size; its right arm hidden while the flesh arm is out.
- Parents fetching a toddler (carried home), an older child walked home, a sleeping parent waking for a child out at night.
- Children's health (`/kagune children` shows health / most and what it grows into), healing on their own.
- The partner's cookbook dishes (1.3.42): `/kagune home` lists what the chests could make.
