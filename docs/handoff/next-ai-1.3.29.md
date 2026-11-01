# HANDOFF: HexGhoul 1.3.29 (work in progress). Read this first.

Branch **`claude/relaxed-carson-4y0frw`**, last commit `2dc5226`. Never push to `main`. No PR unless the owner asks.
Version is already 1.3.29 in both `build.py` and `patch/KagunePatch.java` (always bump both). Network `PROTOCOL` is **"64"**.
Everything since 1.3.28 is **compiled and unit tested, never tried in game**. Never claim otherwise.

## House rules (from the owner, and the build enforces most of them)

- `python3 build.py` must end with **EXIT 0** (about 6 min; run it in the background and wait for it). It patches the jar, compiles,
  verifies every external API reference, checks that the jar loads on a dedicated server, guards listeners, then runs every
  `src/test/java/**/*Tests.java`.
- New Minecraft/Forge references need a line in `verify/api-allowlist.txt` with evidence (`[srg]` from `build/mapping-cache/`:
  `joined.tsrg`, `intermediary-1.20.1.tiny`, Yarn `.mapping`; `python3 tools/find_srg.py <yarn class> <member>` fetches them) and a
  method in `stubs/`. Unused entries fail the build too. Prefer calls the code already makes.
- Banned in the jar: Kaneki, Ayato, Furuta (Ken is "Ken"). Banned in visible text: Takatsuki, Aogiri, Yoshimura.
- Sounds only from the `hexkagune:` pack (`SfxTests` refuses unmapped vanilla sound names in source).
- Never alter the owner's skins. Commit trailer: `Co-Authored-By: Claude <noreply@anthropic.com>` plus the session line; no model names.
- Enums sent by ordinal (`NpcTalk.Choice`, `EtoGesture`, `Persona.ALL`): append only. Tests pin their ends.

## Done in 1.3.29 (all committed)

1. **Scenes engine** (`npc/scene/`): `SceneBook` reads plain-text scripts `src/main/resources/data/hexkagune/talk/{eto,touka,ken,child,ally}.txt`
   (form documented at the top of `SceneBook.java`; every word allowed is in `SceneWords`). 520+ scenes, ~4,600 lines.
   `SceneTests` checks every script parses and that every companion tier x mood x day/night, every child age x temperament x mood,
   and every ally temper x mood has enough scenes and topics. Add content by editing the `.txt` files, then run `SceneTests`.
2. **NPCs come up to you** (`server/SceneServer.java`): partners (every few minutes), friends (rarer), children, and ghoul allies walk
   up; `S2CNpcApproach` (packet id 46) shows a card (`client/NpcApproachOverlay`); the **Answer key (O)** (`ControlKeys.ANSWER`, sent
   as `NpcTalk.Choice.ANSWER`) or a right-click opens the talk on that scene. Ignored, they react (a clingy partner loses 1 regard, a
   child is sad a while). Never during a fight (10 s after any hurt), never while another talk is open.
   Hooks: `EtoBrain.together` (partner), `EtoBrain.calm` (friends), `ChildBrain.lines`, `GhoulNpcBrain.tick` (allies).
   Topics from the scripts join the companions' "Talk a while" page (`EtoServer.topics`), children's "Talk" page and allies' "Let's talk."
3. **Children's temperament and mood**: `family/ChildTemper` (stable from the child id; leans to its companion parent's).
4. **Touka her own way** (owner's complaints): `Persona.style()` 0 Eto / 1 Ken / 2 Touka; `EtoPose.touka(...)`: one hand to your head,
   cheek or neck when she kisses (varies per kiss), cheek pecks, flustered arms-crossed after, stiff hug, her own pull. `Persona.clings()`
   false for Touka (a request to hold her becomes a hug; `EtoBrain.walkUp` maps it). `Persona.unasked()`: her unasked moves become a cheek
   kiss (PECK) or a **stolen kiss** (`EtoGesture.STEAL`: the client turns your view to her, `EtoClient.pull`, then she complains:
   `ToukaScript.stolen`). Scene effect `steal`. Her past with Ken (she ended it; he never wanted to be touched): topics/scenes in
   `touka.txt`, for friends/partners only, never once married.
5. **Touka's kagune: one small wing** (`WingRules.MASTERY` 170, `SIDE` = right; `NpcWings` throws every spike from it).
6. New gestures appended: `STEAL`, `WATER` (bucket in hand), `TIDY` (bent over a chest). Poses exist in `EtoPose` (WATER, TIDY for
   everyone). **Nothing uses WATER/TIDY yet** (see 7).

## Update: items 7, 8 and 9 are DONE (1.3.29 released on the branch)

Chores and "Go home, please." (7), the quests rework, Romance tab, hunts on request and "you already know this" (8), release notes,
README and `companions-handoff.md` (9) are written and pass the full build. Read the top section of `docs/handoff/companions-handoff.md`
for where everything lives. Protocol is now **"65"**. The original list is kept below for the record.

## Was not done (the owner's list, in their words)

7. **Spouse home life** (task in progress, nothing written yet). The owner: at a declared home the spouse "just goes to the door and
   instantly returns" (that is vanilla `RandomStrollGoal` in `EtoNpc.m_8099_` inside the small home restriction set in `EtoBrain.tick`).
   Wanted: chores. Organise chests, actually clean with the broom (`EtoGesture.SWEEP`, has a brush) the floor/blocks, wipe tables
   (`WIPE`), go outside round the house and water flowers/plants (`WATER`), "let loose", many interactions. Plan I had:
   - New `server/HomeChores.java`, called from `EtoBrain.calm` before the idle-gesture block when `Households.forCompanion(level, npc)` is
     non-null, she isn't following anyone, it's day (`HomeLife.night` false), no target, no `toward`/`kiss`/`holding`/`talking`.
   - Every 40-90 s pick a chore: find blocks within `home.radius()` (+6 outside for flowers): chests/barrels (id ends `chest`/`barrel`),
     tables (crafting table, `_slab`, anything with `table`), floor spots, flowers/crops (`_tulip`, `poppy`, `dandelion`, `wheat`,
     `carrots`...). Walk (`m_26519_` every 10 ticks, give up after ~15 s), face it, play the gesture (add `WATER` to `EtoGesture.still()`).
   - Real effects if you want them: sorting a chest needs `Container` API refs (allowlist with evidence). Growing crops needs a random
     tick or bonemeal call (also new API). Visual-only is acceptable to start.
   - The stroll goal will fight the chore's path; either pause it while a chore runs or let `still()` gestures stop navigation (they do).
   - "Better path finding": repath when stuck (no progress for ~3 s), and a short teleport if hopelessly stuck inside the house.
   - **"Go home, please."** as a talk option for a companion who is following you or settled elsewhere: `npc.homeAt(house.pos())` if
     married with a household (`Households.forCompanion`), else her `origin()`; add it as a SAY-slot option with a key in
     `EtoServer.open`/`main` and handle the key in `EtoServer.heard` (see how `"t:"`/`"s:"` keys are handled there).
8. **Quests rework** (not started). The owner: quests shouldn't appear "randomly that you don't have to"; a quest like "Light on the
   Feet" should only be given when you talk to its master (ask them to teach you); a **Romance tab** in the quest screen (relationship
   progress per companion: stage, regard, errands, married, children); when you ask to be taught something you already know (e.g. via
   a command), the teacher says "you already know this" and moves on. Look at `quest/QuestBook`, `QuestLog`, `QuestServer`,
   `client/QuestScreen`/`QuestJournalView`, `npc/Teaching` (refusals) and `EtoTalk.lesson`/`EtoRules.Lesson` (Eto's lessons are quests).
9. **Release**: `.github/release-notes-1.3.29.md`, a README entry (copy the 1.3.28 one's shape), and a new top section in
   `docs/handoff/companions-handoff.md`. Then a full `python3 build.py` to EXIT 0, commit "1.3.29: ...", push.

## Build state

The last full build (before the Touka work) passed the API verifier, dedicated-server check and guard, then failed one check in
`EtoTests` (the choice enum's length). That is fixed. Since then only `SceneTests`, `EtoTests`, `CompanionTests` and `FamilyTests` have
been run (all pass). **Run the full build before anything else** and fix what it finds (most likely: none, or an enum/pose check).

Quick loop without the 6-minute build (after one full build has made `build/base.jar`):

```
Q=/tmp/quick; mkdir -p $Q/stubs $Q/classes $Q/tests
javac -encoding UTF-8 -proc:none -nowarn --release 17 -d $Q/stubs $(find stubs -name "*.java")
javac -encoding UTF-8 -proc:none -nowarn --release 17 -Xlint:-options -cp build/base.jar:$Q/stubs -d $Q/classes $(find src/main/java -name "*.java")
javac -encoding UTF-8 -proc:none -nowarn --release 17 -cp $Q/classes:build/base.jar:$Q/stubs -d $Q/tests $(find src/test/java -name "*.java")
java -ea -cp $Q/tests:$Q/classes:build/base.jar com.hex.kagune.control.SceneTests
```

## Tell the owner

Untested in game: the approach cards and Answer key, every scene, Touka's poses and stolen kiss (the view turn), the single wing. Ask
them to try `/kagune touka here`, date/marry her (`/kagune touka max`), stand near her a few minutes, and press O when the card shows.
