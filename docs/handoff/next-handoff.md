# Handoff: HexKagune 1.3.12 (Eto's full Owl DNA kit, genders, pregnancy, children, reactions), for the next agent

Read this whole file first. It is the current state and replaces `family-handoff.md` as the place to start (that file's sections 0 to 3
are the plan this session worked through; its section 4 is a shorter summary of the same work). For how the project builds and the
rules of the repo in depth, read `eto-handoff.md` sections 1 to 4 too; the essentials are repeated below.

## 0. Where things are

- **Branch: `claude/festive-cray-l5b2pb`**, at 1.3.12 (`VERSION` in `build.py` and `patch/KagunePatch.java`), network `PROTOCOL` **"59"**
  (`control/network/ControlNetwork.java`). Everything from 1.3.1 on lives only on this branch and the one before it
  (`claude/blissful-cray-qq01db`, 1.3.4). **`main` is still at 1.3.0.** Never push to `main` unless the owner says so; tell them it is
  behind.
- CI was green for every push from 1.3.5 to 1.3.11 (build and release). 1.3.12 was still running when this was written: check it first
  (`Build HexKagune` / `Release HexKagune` runs on the branch).
- **Nothing from 1.3.5 to 1.3.12 has been tried in game.** All of it is "compiled, tests pass, not tried in game". The owner has not yet
  reported back on any of it. Ask them how it looks before building on top.
- Local build at 1.3.12: exit 0, Eto tests 22,575 checks, Family tests 165 checks, every other suite green.
- **Pushed after the 1.3.12 release, not in any release yet:** a child whose skin parts can't be read is drawn in Steve's skin instead of
  invisible (`ChildRenderer.make`), and `tools/ChildSkinPreview.java`. Put the fallback in the next release's notes.

## 1. The project in brief (details: `eto-handoff.md` sections 1 to 4)

- Forge 1.20.1 mod (Java 17). The original mod is a binary (`input/hexkagune-1.0.0.jar`) patched by `patch/KagunePatch.java`; all new
  code is the expansion in `src/main/java/com/hex/kagune/control/**`, compiled against the patched jar plus compile-only stubs in
  `stubs/`. Minecraft is called by **SRG names** (`m_6469_` = hurt...), each new member added to a stub and proven in
  `verify/api-allowlist.txt` (`python3 tools/find_srg.py <yarn class> <member>` finds names; see the allowlist's last blocks for the
  format).
- **Build: `python3 build.py`** (about 5 minutes). Always check the exit code before every push. It verifies every external reference,
  loads the jar on a dedicated server (no client class reachable from server code: client code only in `control/client/**`, reached
  through `DistExecutor` or `Dist.CLIENT` subscribers), wraps every listener, checks names, and runs every `src/test/java/**/*Tests.java`.
- Fast compile while iterating (after one full build):
  `javac -nowarn -encoding UTF-8 -proc:none --release 17 -d <out>/stubs $(find stubs -name '*.java')` then
  `javac -nowarn -encoding UTF-8 -proc:none --release 17 -cp build/base.jar:<out>/stubs -d <out>/classes $(find src/main/java -name '*.java')`.
- **Release rules:** a release is published only when a new `.github/release-notes-<version>.md` is pushed. Every release: bump
  `VERSION` in both files, bump `PROTOCOL` whenever anything sent over the network changes, release notes one line per item, one line at
  the top of `README.md`. New network messages go at the end of `ControlNetwork.init()` (the last id is 44); new `NpcTalk.Choice`,
  `PaleMove`, `EtoGesture`, `Romance.Want`, `Gender` values only at the end (sent by ordinal).
- **Say "Owl DNA", never "Pale"** to the owner (in code the path is `KagunePath.PALE` and its classes are `Pale*`).

### Traps this session fell into (so you don't)

- The allowlist must be an exact inventory: an entry the original jar already uses is "stale" and fails the build (it happened with
  `ServerLevel.m_8791_`). Add a line only when the build says a reference is unverified.
- `check_names` refuses show character names in the jar **as substrings** (`build.py`: Kaneki, Ayato, Furuta): the child name "Hayato"
  failed it. Run any new names past that regex.
- `SfxTests` requires every vanilla sound name in code to be remapped to the owner's pack or listed in its `KEPT_VANILLA` (cutscene cues
  are listed there).
- Some members are missing from the stubs even though the game has them (`Entity.blockPosition`, `BlockPos.distSqr`,
  `LivingEntity.swing(hand)` as `m_6674_`): look at what other code already uses before adding new API.

## 2. How the owner works

- Plays on Android (Amethyst launcher), tests jars from CI. Types fast, by voice: read for intent. Tone: flirty, witty, casual, blunt;
  say plainly when an idea is weak and stress-test ideas. Keep replies short. **Never claim something works in game when it was only
  compiled.**
- Wants every system built once, generic, for every NPC to come (they plan teachers who unlock abilities, and more partners), Eto first.
- When something visual changes, render a preview and look at it yourself (they rejected art that looked crude: see section 4).
- Push after each step, with a green local build.

### Decisions the owner made this session

- **A female player's hitbox and eye height shrink for real** to 0.86 (Forge's size event, no Pehkui).
- **A pregnancy is asked for, then the next night:** married couples only, "Let's have a baby." in Eto's talk; the next night scene she
  conceives. Always a fade to black, nothing explicit.
- **Female players wait for male NPCs:** the system supports any mother, but no one can father a female player's child yet (no male NPC
  partner, no player-player marriage).
- **Children respawn at home** a day after they die.
- **Hand-painted child skins were rejected** ("they look terrible"): child skins are now built from proper skin parts (see section 4).
- **A child's skin tone must come from its parents' real skin**, never anything else (the owner did not want a light-skinned player's
  child to come out dark-skinned): each player's tone is read off their own skin.
- Holding hands (feedback on 1.3.4): the held hands must not swing as they walk, and the arms should stretch further apart. Fixed in 1.3.5.

## 3. Releases this session

| Release | What |
|---|---|
| 1.3.5 | Holding hands: both held arms drop vanilla's walk swing (`RigPose.leftArmHold`, the player's `rightArmHold`); arms out about 15 degrees, a block apart |
| 1.3.6 | Eto's flesh arm (swipe, grab), flesh leg (Flesh Hammer), Watch This |
| 1.3.7 | Eto's Mutated Arm |
| 1.3.8 | Eto's Owl kakuja below 25% health: Rend, Reaping Whirl, Pounce; half damage taken |
| 1.3.9 | Gender screen after the eyes (the eye screen's Done is now Next); one shared female body for Eto and female players; real shrink (protocol 58) |
| 1.3.10 | Pregnancy (belly, alerts, the "9 months later..." birth cutscene) and children (protocol 59; pushed in two commits, released once) |
| 1.3.11 | NPCs react to what a player shows: eyes, kagune, kakuja, the worst forms |
| 1.3.12 | Eto starts things herself (takes your hand, clings, hugs, leans, hums, reads, dances), remarks on moments, many more lines |

## 4. What exists, and where (all under `src/main/java/com/hex/kagune/control/`)

### Eto's fight: the whole Owl DNA kit on an NPC body

| Piece | Where |
|---|---|
| Her choice of move (pure, tested): which move by distance, phase (calm, serious, maniac), what is ready | `npc/EtoRules.java` (`choose(..., Ready, roll)`, `Act`, `chooseOwl`, `owlNow`, `hammerHold`, `armHeld`) |
| Strikes, Big Punch, Witness This, Speed Kick, flips (1.3.4) | `server/EtoOwl.java` (her `Fight` state; dispatches the rest) |
| Swipe, grab, Flesh Hammer | `server/EtoFlesh.java` |
| Watch This tentacles | `server/EtoWatch.java` (sends `S2CPaleWatch` with her UUID) |
| Mutated Arm | `server/EtoArm.java` (sends `S2CPaleArm` with her UUID; lengths at a player's size, times `EtoRules.SIZE` in the world) |
| The Owl kakuja (Rend, Whirl, Pounce; half damage; shed out of a fight) | `server/EtoOwlForm.java` (sends `S2CPaleOwl` with her UUID) |
| Drawing it all on any body | `client/render/PaleFleshRenderer.java`: `Frame` takes a `scale`; `flesh`, `tentacles`, `arm`, `owl` take any `LivingEntity` and a drawn pose; `EtoRenderer.kit` runs them for each Eto with her pose (`EtoRenderer.drawn`); `WatchClient`, `ArmClient`, `OwlClient` are keyed by entity UUID |
| Her body posed by the player's animator, the arm's weight, the Owl growing | `client/animation/EtoFight.java` |

Not on her yet: the Owl's Big Punch, Speed Kick, bone spikes, Fissure, blade volley, Watch This lances and great hand (players have them:
`server/PaleOwlMoves.java`, `OwlVolley`, `OwlWatch`, `OwlGrab`). Her moves never break blocks (her Library stands).

### Genders and the one female body

| Piece | Where |
|---|---|
| `Gender` (UNSET, MALE, FEMALE; kept in the profile root `HexGender`) | `Gender.java` |
| Every player's gender on each side; the size event (a woman's box and eyes at 0.86; a child's box by its stage) | `Genders.java` |
| Server store, sync on login/respawn/dimension/tracking, a refusal hook (no switching away from female while pregnant) | `server/GenderServer.java`, `network/C2SGender`, `S2CGender`, `client/GenderClient` |
| The screen (pure drawing, previewable: `tools/UiPreview.java <out> gender`) | `client/GenderView.java`, `client/GenderScreen.java`, opened by Next on `client/KakuganScreen.java` |
| The shared figure (size, slim arms, hips, bust geometry, bounce) | `FemaleBody.java` (pure), `client/render/FemaleFigure.java` (legs, arms, bust, belly, jiggle) |
| Female players drawn: scaled in `RenderPlayerEvent.Pre/Post`, legs widened; their leg pivots spread through `KaguneBodyAnimation.get3DTransform` (PlayerAnimator resets pivots each frame, so that layer is always active for women) | `client/render/FemalePlayers.java`, `client/animation/KaguneBodyAnimation.java` |
| The bust (and belly) on players, with the upper body's bend, the jacket layer over it, none through a chestplate | `client/render/FigureLayer.java` (attached in `PlayerLayers.attach`) |
| Eto on the shared figure | `client/render/EtoModel.java`, `EtoRenderer.java` |
| Hand in hand with a woman: she stands closer | `npc/EtoHand.out(woman)` |

### Pregnancy (any woman: female player or female NPC)

| Piece | Where |
|---|---|
| Rules (pure): two Minecraft days, the belly from a fifth of the way, stages, alerts | `family/Pregnancy.java` |
| What the child will be, decided at conception (pure): gender, ghoul if a parent is, one kakugan 60% if a parent is one-eyed, hair blended, eyes from one parent, skin between both parents' | `family/Genome.java` |
| Every pregnancy with the world (mother/father keys `"player:<uuid>"` or `"npc:<key>"`), alerts, the birth, `onBirth` hooks, `hurry` for testing | `server/Pregnancies.java` |
| The belly on clients | `network/S2CPregnancy`, `client/PregnancyClient`, `FemaleFigure.belly` |
| The birth cutscene ("9 months later...", a small cry) | `client/CutsceneOverlay.BIRTH`, `S2CCutscene.BIRTH` |
| Eto: "Let's have a baby." (wife), conception on the next night, the "baby" topic pinned first in talk, lines by stage | `npc/EtoTopics.java`, `server/EtoServer.java` (`conceive`, `family`), `npc/EtoLines.java` (`conceived`, `pregnant`), `npc/NpcMemory.BABY/CHILDREN`, `family/FamilyView` in `EtoTalk.Context` |
| Lines for any partner's alerts | `family/FamilyLines.java` |

### Children

| Piece | Where |
|---|---|
| The entity (one of a kind per child: `UniqueBody` key `"child:<id>"`; babies and toddlers report `isBaby` for the model's little-one proportions) | `npc/ChildNpc.java`, `ControlEntities.CHILD` |
| Every child with the world (name, genome, parents, birth, skill, last lesson) | `server/Children.java` (`init` registers the birth hook and the respawn) |
| Respawn at home: `UniqueNpcs.kinds(prefix, type, made)` remakes any key with that prefix | `server/UniqueNpcs.java` |
| Stages (baby 0 days, toddler 2, child 5, teen 9: box, draw size, health), rules (a lesson a day up to 5, damage, follow distances, carry offsets), lines | `family/ChildStage.java`, `ChildRules.java`, `ChildLines.java` |
| Its mind: claim and report, growing, carried (teleported to the carrier's chest each tick), a baby sitting, following, fighting threats to its family or fleeing to a parent, idle and greeting lines; parents can't hurt it | `server/ChildBrain.java` |
| Talk (the shared talk screen): pick up / put down, play, teach, follow / wait, how are you; Shift + right-click puts it down | `server/ChildTalk.java` (routed from `network/C2SNpcTalk`) |
| Drawing: glued at the carrier's chest, sitting pose, skin made per child, the parent's cradling arms | `client/render/ChildRenderer.java`, `client/ChildClient.java`, `KaguneBodyAnimation` (carrying) |
| Looks from the parents: each player's hair and skin tone read off their own skin by their client and sent | `family/HairColour.java`, `family/SkinTone.java`, `client/SkinReader.java`, `network/C2SLooks`, `server/Looks.java` |
| The skin: composed from edit_skin parts (girls' long hair 1, 4, 11; boys' short 3, 5, 10, 12; outfits by age; no beards: the hair is cleared off the lower face; eyes recoloured; a kakugan version) | `family/ChildSkin.java`, `family/Png.java` (the mod's own PNG reader), parts in `src/main/resources/assets/hexkagune/textures/entity/child/` |
| Name | `server/ChildNames.java` (soft Japanese names, checked against the name filter) |

**Licensing:** the child skin parts come from the edit_skin mod (MrRar and others, CC BY-SA 3.0 / 4.0). Every part is credited in
`textures/entity/child/CREDITS.txt`; parts whose origin wasn't listed were left out on purpose. Keep it that way: `FamilyTests` fails if a
part has no credit. Never ship skins from the usual skin sites (no licence).

Preview child skins off-game after a build: `java -Djava.awt.headless=true -cp build/classes tools/ChildSkinPreview.java <out-dir>`.

### Reactions (any NPC)

| Piece | Where |
|---|---|
| What a player shows: NONE, EYES, KAGUNE, KAKUJA (any path's, the Owl), DREAD (a final kakuja, the Dragon) | `npc/Display.java` |
| Looked at twice a second; when it rises, every watcher reacts; `Sightings.watch(...)` for new NPCs | `server/Sightings.java` |
| Eto's (by who they are to her, in her Library or not; a gesture, hearts) | `EtoBrain.saw`, `EtoLines.saw` |
| Ghoul NPCs (strangers flee a kakuja and worse through their own `scaredUntil`; followers impressed), children | `npc/ReactionLines.java` |

### Eto doing things unasked (1.3.12)

| Piece | Where |
|---|---|
| What she starts by mood (pure): hand, cling, hug, lean, hum, read, dance; at most every 90 seconds, within 10 blocks | `npc/EtoInitiative.java` (`pick`, `line`), used in `EtoBrain.together` (she walks up through `walkUp`, which already handles each gesture on arrival) |
| Moments: rain starting, dusk, dawn, her partner badly hurt, a kill near her | `EtoInitiative.moment`, `EtoBrain.moments`, `EtoBrain.killedNear` |
| More lines in every pool, an Owl line, pregnancy talk in her approaches | `npc/EtoLines.java` |

### Testing shortcuts (operators)

- `/kagune family soon`: every pregnancy you are part of, ten seconds before the birth.
- `/kagune family grow`: your nearest child (within 16 blocks) grows into its next stage.
- `/kagune eto max [player]`: Eto's regard at the most (she must be within 10 blocks).
- To reach a baby quickly: date and marry Eto (ring), then "Let's have a baby.", then her night, then `/kagune family soon`.

## 5. Riskiest, untried in game (check these first if the owner reports trouble)

1. **Female players' render:** a pose pushed in `RenderPlayerEvent.Pre` (LOWEST) and popped in `Post` (LOWEST); if another mod cancels the
   render after ours, the pose stack would be left unbalanced. The legs' pivots are spread through our PlayerAnimator layer, which is now
   always active for women. The bust follows the upper body's bend (`PlayerLayers.upperBody`): check it during leaning moves.
2. **The hitbox shrink** runs on both sides; a client that misses `S2CGender` would move a box the server doesn't (rubber-banding).
3. **Carried children** are teleported every tick on the server and glued in front of the carrier on clients. Watch for jitter, walls,
   and dismounts.
4. **Child skins** are made on the client with `NativeImage` + `DynamicTexture`, the parts read through `getResourceAsStream`. If the
   parts can't be read on the phone, the child is drawn in Steve's skin instead (`ChildRenderer.make`).
5. **Hair and skin read:** phones that can't read textures back (`SkinReader` logs it) fall back to brown hair and light skin.
6. **Eto's new moves:** the grab and the Mutated Arm carry players by velocity every tick; the Owl creature is drawn in her place while
   her model is posed but not drawn; Watch This on her uses her UUID in the same packets players use.
7. **Despawning:** children use `setPersistenceRequired` and the CREATURE category; Eto never despawned, but children are new.

## 6. Not done, and what could come next

- The rest of the Owl's moves on Eto (section 4).
- No player-player marriage, so no pregnancy for a female player (and no child of two players) until the owner wants it or a male NPC
  partner exists. The keys already support both.
- Children stop at teen; no adulthood. Parents can't name them (picked from the seed). Children don't follow you through dimensions.
- A female player's own first-person hands aren't slimmed.
- The owner's next NPCs (teachers who unlock abilities, more partners) are not started: wait for them to describe them, and ask before
  naming anyone (the name filter).
- Older handoffs, for other parts of the mod: `requests.md` (the owner's own words for the Graft DNA path, 7.0.0: binding for that path),
  `kings.md` and `kings-next-prompt.md` (the final kakujas, War and Peace), `next-session.md` (superseded by `kings.md`), `eto-handoff.md`
  (Eto at 1.3.0 and the project basics), `family-handoff.md` (the plan this session worked through).
