# HANDOFF: HexGhoul after 2.0.4, for 2.0.5. Read this first, then `docs/handoff/next-ai-2.0.1.md` (what 2.0.1 to 2.0.4 did), then
`docs/handoff/next-ai-1.3.60.md` (house rules), then the top sections of `docs/handoff/companions-handoff.md`.

Branch **`claude/relaxed-carson-4y0frw`** (check it out from origin; push only there with `git push -u origin claude/relaxed-carson-4y0frw`).
Never push to `main`; no PR unless asked. Version **2.0.4** in `build.py` and `patch/KagunePatch.java`; the next is **2.0.5**. Protocol
**"73"** (`ControlNetwork.PROTOCOL`; 2.0.3 added `ChildStage.ADULT`). Nothing is tested in game except what the owner reports.

## How the owner wants to work now (their words, this stretch)

- "dont run it localy import it asap": they are short on usage. Don't run the full `python3 build.py` (4 minutes) unless something needs
  it. Push as soon as a piece is done: CI (`.github/workflows/release.yml`) builds, runs every suite and publishes the jar when
  `.github/release-notes-<ver>.md` is pushed (it fires only when that file changes; to re-run a failed release, touch the notes).
- They stopped local runs twice. One quick check (`python3 tools/quick_check.py <Suites>`, about 40 s: compile, API verify, the named
  suites) before pushing new Minecraft API or a broad change has paid off: it caught a missing allowlist line, a stale one, and old tests
  that 2.0.2 broke, each of which would have been a red CI round. Run only the suites the change touches. Say in one line why you ran it.
- Each fix or feature is its own version (bump both version strings, release notes, README entry, a new top section in
  `companions-handoff.md`, commit `2.0.x: ...`, push). Bug reports first.
- Answer in English. Be plain about judgment calls and about what was not run.

## The owner's request for this session

> "Interaction between other players with another player's sons/daughters (NOT DATEABLE). Double check that the kids have a chance to
> enherit both kagunes (so far only have seen the ukaku on kids, never seen a kid with both)."

### 1. Other players and someone else's children (never dateable)

**Now:** `server/ChildTalk.open` (around line 109): `parent = child.parent(Pregnancies.player(uuid))`. A parent gets the full page (play,
carry, teach, talk, follow, hand, feed, bed, home). Anyone else gets one choice, "Hello there.", and the child says "...Hi." (a baby
"*stares at you*"). Regard 30, `ally` false. The title already reads "Grown · Akane's daughter" for a stranger (`ChildTalk.title`). Child
scenes (`SceneServer`, `talk/child.txt`, `SceneWords.CHILD_FLAGS`) assume the player is a parent.

**Build (a suggestion; keep the house style of lines by stage, temper and age, each with a cue from `family/TalkCues`):**
- A stranger's page by stage: a baby (coo, grab a finger), a toddler (shy, hides behind its parent if one is near, "Who are you?"), a
  child (asks your name, tells you about its mum or dad by name, shows you something), a teen (wary small talk), a grown-up (polite adult
  small talk, mentions their parents). Use `child.motherName` and `child.fatherName` (`server/Children.Child`).
- Gentle choices only: talk, wave, high five (child and up, `ChildMoment.HIGH_FIVE` already exists for parents), offer food (little ones
  may answer "Mum says no food from strangers"), "Where are your parents?" (the child points or walks you to its parent; or says they are
  away). Never for strangers: carry, take by the hand, follow, teach to fight, put to bed, send home (parents only, as now).
- Temper and fear: a stranger showing kakugan or kagune frightens it (`npc/FearLines`, `ReactionLines` by stage); a child runs to its
  parent. A friend of the family (the parent's companion spouse, or a player the parent is partnered with, if such a link exists) could be
  warmer: optional.
- **NOT DATEABLE, anywhere, any stage including `ChildStage.ADULT`:** no confess, date, kiss, hug-as-romance, hand-holding-as-date, marriage
  or "ask out" for any `ChildNpc`, for any player, parent or not. Today nothing offers romance to a `ChildNpc` (romance lives in `EtoTalk`
  for companions and `HumanTalk` for villagers), so add a test that fails if it ever does: for every stage, parent and stranger,
  `ChildTalk`'s choice keys contain no romance key, and no `SceneServer` child scene carries a romance effect. A grown child stays someone's
  son or daughter: talk is friendly, never flirty (check every new line for that, by reading).
- Multiplayer: talk pages are per player (`TALKS` by UUID); two players talking to one child at once should both work (check
  `Conversation` and `ChildBrain`'s "a player's talk" step: the child stands for whoever opened the talk).

### 2. Kids inheriting both kagunes

**What the code does now (read, not run):** `family/KaguneGenes.inherit(mother, father)` is not a chance: the same kind from both, that kind;
one parent with none, the other's; **two different kinds, always both** (with wings, down to one wing). What each parent passes:
- A companion: `Persona.kagune()`: Touka WINGS, Ken TAILS, Eto OWL.
- A player: `Looks.kagune(player)` -> `KaguneGenes.ofPath(PathServer.path(player).id(), ghoul)`: "ukaku" -> WINGS; **every other path
  ("none" undecided, "rinkaku", "owl", "graft") -> TAILS**; a human player -> NONE.
- `Genome.of`: the child is a ghoul only if a parent is; then `inherit`.

So with Touka, an Ukaku player gives an Ukaku-only child, and a human player gives an Ukaku-only child (only Touka's wings). Only a Rinkaku
(or undecided, Owl or Graft) player gives tails and one wing. That likely explains "only Ukaku", but check it all:
1. Ask the owner (or have them run `/kagune children` near the kids: it prints each child's inherited kagune, `ControlCommands` ~line 383)
   which path they were on and whose children they were. If the data says "tails and one wing" but they only saw a wing, it's a drawing bug.
2. **A lead for a drawing bug:** `server/ChildKagune.message` (~line 469) tells clients the child's kind as one `NpcIdentity.Kind`: RINKAKU if
   it has tails, else UKAKU. Check what the client draws for a child with tails AND a wing (`ChildKaguneRules` has a both-kinds layout,
   lines ~102, 197, 294; `GraftRenderer` ~185 draws children's tails). If one path draws only one kind, fix it. Make a preview if you
   change the drawing.
3. Gaps to decide with the owner: an Owl-path player passes tails (not Eto's Owl flesh), a Graft-path player passes tails. Maybe intended.
4. **"Have a chance":** the owner expects a chance, the code gives a certainty when the kinds differ. Ask before changing it. A proposal:
   kinds differ -> 40% the mother's kind, 40% the father's, 20% both (the wings down to one with tails). Decided at conception from the
   seeded `Random` in `Genome.of`, drawn after everything else so existing genomes don't change. Children already born keep theirs (their
   kagune is saved; `UNKNOWN` ones are worked out from their parents when they next think).
5. Tests: `FamilyTests` (the kagune genes block): add one per pairing (Touka x Rinkaku, Touka x Ukaku, Touka x human, Ken x Ukaku, Eto x
   Rinkaku...) and, if it becomes a chance, the shares over many seeds.

## Open from earlier (ask the owner when it fits)

- **Grown daughters' height:** drawn at Touka's size (`Persona.TOUKA.size()` 0.86 x `ChildStage.ADULT.draw` 1.0 in `ChildRenderer`); a teen
  is drawn at 0.88, so she gets about 2% shorter when she grows up (see `docs/handoff/grown-daughters-2.0.3.png`, made by
  `tools/GrownDaughterPreview.java`). Offered to the owner: at least the teen's height (e.g. 0.9). Not answered yet.
- Grown children act as teens (lines, moves, bedtime, living at home). Adult life of their own is not built.

## Untested in game (2.0.1 to 2.0.4; watch these)

- 2.0.4: the night for another baby at the shared bed (come within 3 blocks of her in bed, or lie down within 6); the village clearing the
  world's ground over itself as it is placed (`ControlStructures.DryJigsaw` afterPlace, `village/VillageTops`).
- 2.0.3: the grown stage (day 16), grown daughters with Touka's figure; protocol 73.
- 2.0.2: the four structures in many more biomes (Biomes O' Plenty's optional), the gentler village land check.
- 2.0.1: the new village (25 jigsaw tiles, sunk by 8), its type `hexkagune:dry_jigsaw`, 26 villagers, 6 investigators, the talk-screen
  faces, the bubble crash fix.

## House rules (unchanged; full text in `next-ai-1.3.60.md`)

Nothing is tested in game, never claim it is. Look at your own previews honestly, zoom into every face and texture. Banned names in the
jar: Kaneki, Ayato, Furuta. Banned in visible text: Takatsuki, Aogiri, Yoshimura. Sounds only from the hexkagune: pack. Never alter the
owner's skins or files (only generated copies). New Minecraft API: an entry in `verify/api-allowlist.txt` with [srg] or [order] evidence
plus a stub (`tools/find_srg.py <yarn class> <member>`; the mapping cache is in `build/mapping-cache`); enum constants keep their names;
stale entries fail the build. Append only to ordinal-sent enums. No model names in commits or files.
