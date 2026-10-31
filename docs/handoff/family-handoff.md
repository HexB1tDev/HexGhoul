# Handoff: Eto 1.3.4 → genders, pregnancy, children, reactions (HexKagune), for the next agent

Read this whole file, then `docs/handoff/eto-handoff.md` sections 1 to 4 (what the project is, how it builds, the release rules,
how the owner works). Both are self-contained. This one picks up where 1.3.4 left off and lists what to build next.

## 0. Before anything

- **Base your work on branch `claude/blissful-cray-qq01db`** (1.3.4, commit `bf81f1b` or later). `main` is still at 1.3.0: every 1.3.1 to
  1.3.4 change lives only on that branch. Branch from it (or merge it into yours) first, or you lose all of it. Tell the owner `main`
  is behind; never push to `main` unless they say so.
- **Always run `python3 build.py` and check its exit code before every push.** The owner once asked to skip local builds to save time;
  the push that followed broke CI on a missing import and they asked for local builds back. A green build means: it compiles, every
  external reference is proven or allowlisted (`verify/api-allowlist.txt`), the jar loads on a dedicated server, no show character
  names are in the jar, and every `*Tests.java` passes.
- CI builds every push to `main` and `claude/**`; a release is published only when a new `.github/release-notes-<version>.md` is
  pushed. Every release: bump `VERSION` in `build.py` and `patch/KagunePatch.java`, bump `PROTOCOL` in
  `control/network/ControlNetwork.java` (it is `"57"` now) whenever anything sent over the network changes (a new gesture, a new
  `NpcTalk.Choice`, a packet's fields), a one-line-per-item release notes file, one line at the top of `README.md`.
- **Say "Owl DNA", never "Pale".** In code the Owl DNA path is `KagunePath.PALE` and its classes are `Pale*` (`PaleMoves`,
  `PaleRules`, `PaleAnimator`...). The owner doesn't know that name; in game it is "Owl DNA".
- Nothing below has been tried in game unless it says so. Say "compiled, tests pass, not tried in game" when that is the truth.
- The owner wants everything here to work for **every NPC to come** (they plan more one-of-a-kind NPCs). Build each system once,
  generic, and use it for Eto first.

## 1. What exists now (1.3.2 to 1.3.4), and where

All paths under `src/main/java/com/hex/kagune/control/` unless noted.

### One-of-a-kind NPCs (reuse for every new NPC)

| Piece | Where |
|---|---|
| Marker interface: no fall damage, a fixed home (origin), goes home | `npc/UniqueBody.java` (Eto implements it) |
| World registry (one body alive per key, partner, home, origin, death, company) | `npc/UniqueNpc.java` (pure, tested), `server/UniqueNpcs.java` (SavedData on the overworld; respawn at origin a day after death; recall home after a day away with no player about; `LivingAttackEvent` cancels fall damage for any `UniqueBody`) |
| Registering a kind so it can be remade | `UniqueNpcs.kind(key, entityType)` (Eto's in `ControlEntities.register`) |
| The Library generates once per world | `resources/data/hexkagune/worldgen/structure_set/library.json` (`concentric_rings`, `count` 1) |

### Per-player memory and romance (reuse)

| Piece | Where |
|---|---|
| What a player and each NPC remember (numbers by name, per NPC key) | `npc/NpcMemory.java` (pure), `server/NpcMemoryServer.java` (kept in the player's profile root, `HexNpcMemory`) |
| Romance rules: stages NONE/DATING/MARRIED, one partner for good, the ring, jealousy, the tag over the head | `npc/Romance.java` (pure), `server/RomanceServer.java` (`romanced(player, key)`: other partners get jealous) |
| Gold Ring item + recipe | `ControlItems.GOLD_RING`, `resources/data/hexkagune/recipes/gold_ring.json`, `assets/.../textures/item/gold_ring.png` |
| Private alerts to a partner (banner + chime, only they get it) | `network/S2CNpcAlert.java`, `client/NpcAlertOverlay.java`, `Romance.Want` |
| Cutscenes (fade to black, cues, captions, the NPC's last words) | `network/S2CCutscene.java`, `client/CutsceneOverlay.java` (scripts `SURGERY`, `NIGHT`; add new ones there) |

### The talk screen (shared by ghoul NPCs and Eto)

| Piece | Where |
|---|---|
| Choices, sent **by ordinal** (new ones only at the **end** of the enum); `SAY_1..SAY_8` carry labels the server sends | `npc/NpcTalk.java` (`Choice`, `Choice.SAYS`) |
| Page packet with per-answer labels | `network/S2CNpcTalk.java` |
| Drawing: rising board, big name, mood tag, typed-out text with a voice blip, answers sliding in, bond meter with a gliding heart and floating regard changes | `client/NpcTalkView.java` (pure drawing on `UiCanvas`; `revealed`/`duration` tested), `client/NpcTalkScreen.java` (timing, skip, voice, the heart's glide) |
| Offline previews of the screen | `tools/UiPreview.java <out-dir> eto-talk` (needs a build first: `java -Djava.awt.headless=true -cp build/classes:build/base.jar tools/UiPreview.java <out> eto-talk`) |

### Eto

| Piece | Where |
|---|---|
| Entity (`UniqueBody`, origin, home, follow) | `npc/EtoNpc.java` |
| Rules: size 0.86, 300 health, armour 14, lessons, a human's pact, phases (calm, serious, maniac), her fight's choice (`Act`, `choose`), kiss states incl. PULL | `npc/EtoRules.java` |
| Who you are to her (claimed by someone else, wary, friendly, close, partner, wife) | `npc/EtoVoice.java` |
| Moods (12) and the idle gestures per mood | `npc/EtoMood.java` |
| Her lines (greetings, approaches, kisses, the ring, the night, the surgery, gifts, fights...) | `npc/EtoLines.java` |
| Topics with replies | `npc/EtoTopics.java` |
| Pages (main, talk, topic, sweet, gifts) and answers | `npc/EtoTalk.java` |
| Gestures (sent by ordinal, append only), poses | `npc/EtoGesture.java`, `client/animation/EtoPose.java` |
| Hand holding geometry | `npc/EtoHand.java` |
| Server brain: uniqueness, partner, clinging, approaches, welcome home, protecting her partner, never fighting her partner, kiss/pull, holding hands, props in her hands, phases | `server/EtoBrain.java` |
| Her fight: the **Owl DNA** kit, the player's own moves and rules: Speed Blitz string (blinks, finisher), Big Punch charge, blink, launch, full charge into Witness This, Speed Kick dash, flips | `server/EtoOwl.java` (uses `pale/PaleRules`, `pale/PaleStrike`, `pale/WitnessRules`, `server/PaleStun`, `server/PaleKnockdown`, `S2CPaleFx` impacts) |
| Talk on the server: pages, gifts, puppy and letter forgiveness, the ring and the wedding broadcast, the night scene, the surgery (warm or cold), the pact | `server/EtoServer.java` |
| Sync to clients (gesture, Owl DNA move as a `PaleView`, who she holds, the tag over her head) | `network/S2CEto.java`, `client/EtoClient.java` |
| Her body posed exactly like a player's (`RigPose.transform`, the same conversion PlayerAnimator uses) and her fight animated by the player's own `PaleAnimator` | `client/render/EtoModel.java`, `client/animation/EtoFight.java`, `client/render/EtoRenderer.java` (also: the tag over her head, the bust wedge + jiggle, glued to her partner's side while holding hands) |
| The player's arm reaching out to her while holding hands | `client/animation/KaguneBodyAnimation.java` (`EtoClient.handHeld`) |
| Tests | `src/test/java/com/hex/kagune/control/EtoTests.java` (22,403 checks at 1.3.4) |

### Riskiest, untried in game (check these first if the owner reports trouble)

1. Holding hands: she is moved to the player's side every tick on the server and glued there when drawn; walls and stairs.
2. Her Owl DNA fight on an NPC body: the Speed Kick's dash (she is teleported along it each tick), Witness This (she and the target are
   placed each tick, up to 17 blocks in the air; the home leash is skipped while a move carries her), her model posed through
   `RigPose.transform` (no elbows or knees: a mob has no bendy-lib).
3. Coming home: a fresh body made at home when the old one lies in unloaded ground (the old one is discarded when it loads).

## 2. What to build next (the owner's words, then the plan), in this order

### 2.1 Check 1.3.4 first

CI for `bf81f1b` was still running when this was written. If it is red, fix it (build locally, read the error). Ask the owner how her
Owl DNA fight, holding hands and the talk screen look in game before stacking more on top.

### 2.2 Her remaining Owl DNA moves (agreed with the owner, not started)

The flesh arm (`PaleMove.SWIPE`, `GRAB_*`), the flesh leg (`HAMMER_CHARGE`, `HAMMER`), Watch This (`WATCH`, `server/PaleWatch.java`,
`client/WatchClient.java`), the Mutated Arm (`server/PaleArm.java`, `client/ArmClient.java`), then the Owl kakuja (`OWL_*` moves,
`server/PaleOwl*.java`, `server/Owl*.java`, `client/OwlClient.java`). Their visuals are drawn only for players
(`client/render/PaleFleshRenderer.java` loops over players and reads the body from `KaguneBodyAnimation`). Generalise each renderer to
any `LivingEntity` with a pose source (`EtoFight` for her), then add the move to `EtoOwl` the way the Big Punch and Speed Kick were
done (pure rules shared, server logic re-written for an NPC body). Suggested: the flesh arm, flesh leg and Watch This in her serious
and maniac phases; the Owl kakuja as her last resort.

### 2.3 Gender, chosen after becoming a ghoul

> "A new menu after you unlock the ghoul. After you do the ghoul command and adjust your eye, there should be a button under it
> saying Next, and you pick your gender, male or female. The females will have the exact same body, same size, as Eto. That's the
> base model for all NPCs, for all players that choose to be female."

- The eye placement screen is `client/KakuganScreen.java` (opened from the path screens, `client/PathScreens.java`). Add a "Next"
  button under it that opens a small gender picker (Male / Female), in the house style (`UkakuMenuView`, `GraftMenuView`).
- Store the choice in the player's profile root (e.g. `HexGender`), send it to the server (new C2S packet, at the end of
  `ControlNetwork.init()`), and to everyone tracking the player (it changes how they are drawn).
- **One female body, shared**: pull Eto's figure out into a reusable piece used by Eto, female players and every female NPC to come:
  slim arms, legs drawn wider and deeper (`EtoModel.HIPS`, `THIGHS`, leg pivots `LEG_X`), the bust wedge and its jiggle
  (`EtoRenderer.bust`, `EtoClient.jiggle`, `EtoRules.jiggle`), and the size (`EtoRules.SIZE` 0.86).
- For players: scale and reshape at render time the way `client/render/OwlScaler.java` already does (it scales a player's model parts
  in `RenderPlayerEvent.Pre` and puts them back after); the bust as a layer on the torso (see `PlayerLayers`). Players are posed by
  PlayerAnimator and bent by bendy-lib: test that the bust follows the torso's lean. Whether the hitbox shrinks too is the owner's call
  (Pehkui is installed in their pack, but the mod doesn't depend on it): ask.

### 2.4 Pregnancy (female players and female NPCs)

> "Female players should be able to get pregnant. And NPCs as well. Eto and all the other females are to be able to get pregnant,
> and visibly swell their belly a little bit. After 2 Minecraft days, they give birth."

- Generic `Pregnancy` rules (pure, tested) + server state, keyed by mother (player UUID or NPC key) with the other parent, the day
  conceived, and what the child will inherit (decided at conception, see 2.5).
- How: through the partner's night scene (`EtoServer.night`, the `NIGHT` cutscene) with a chance per night, or a choice the owner
  approves ("Let's have a baby"; the wife topic "Do you want a family someday?" in `EtoTopics` is the hook). Only between partners
  (`Romance`). Keep it a fade to black; nothing explicit.
- The belly: a gentle swell on the torso front, growing over the 48000 ticks (2 days), drawn like the bust wedge, on female players
  and female NPCs; she says things about it (moods, cravings, tiredness), private alerts to the partner as birth comes.
- Birth: a cutscene (`CutsceneOverlay`, a new script) that reads **"9 months later..."**, then the baby is there.

### 2.5 Children (MCA-Reborn-like)

> "The baby NPC spawns as a baby that just sits down. You can pick them up. Overall insane interactability. There's a variety of
> dialogue between the stages of the child. It can be a male or female. You can teach them how to fight, and tell them to follow you.
> If one parent is a one-eyed ghoul there's a chance for the kid to be one-eyed. This is going to be reused for future NPCs."

- A child entity (its own type, in `ControlEntities`), its stages (baby: sits, can be picked up and carried, e.g. as a passenger
  drawn in the player's arms; then toddler, child, teen), each with its own size, poses and lines.
- **Children and babies are never drawn with the adult female body.** No bust, no curves before the adult stage. The owner's "the
  females have a larger chest" applies to adult women only (the shared female body in 2.3). Do not add anything sexual about a child
  in any line, pose or texture.
- Looks mixed from both parents. The owner: "detect the main colour of the top of the player's head". The server has no skins, so
  the client reads it: the player's own skin's top-of-head face (u 8..16, v 0..8 on a 64 px skin) and the hair rows of the front face;
  take the dominant non-skin colour, send it to the server (C2S), and keep it with the child. Make the child's skin on the client from
  a template, its hair the blend of both parents' hair colours (Eto's hair is the teal of `art/eto_skin.png`), its eyes from a parent.
- Ghoul inheritance: a ghoul child if a parent is a ghoul; one-eyed (her kakugan in one eye) with a good chance if a parent is one-eyed
  (`KakuganServer.granted`, `EtoRules.GRANT`).
- Lessons (reuse the student track idea: `EtoRules.Lesson`, quests), follow / wait here (reuse `FOLLOW`/`STAY`), a home with the
  family; use `UniqueBody` rules for coming home, but decide with the owner whether children respawn.

### 2.6 Reactions to kagune, kakuja, fear

> "Reactions of NPCs to transformations, kakujas, fear."

NPCs (Eto first, then ghoul NPCs) notice and react when a player near them opens their eyes, brings a kagune out, goes into a kakuja
or the Dragon: lines, gestures, moods (a partner impressed or worried, strangers scared and backing off, Eto amused or annoyed in her
library). The server already knows each player's state (`KaguneServerController`, `PathServer`, the path servers); add a hook and
react in `EtoBrain`.

### 2.7 More interaction, more lines

> "Make the NPCs approach you and talk, and not just request, but do things with you. Just out of nowhere grab your hand if they feel
> they need attention. More interaction overall. More voice lines."

- Unprompted: she takes your hand herself (start `HAND` from her side; reuse `EtoBrain.walkUp` + `holdHands`), takes your arm
  (`CLING`), leans on you, leads you somewhere to show you something, reads next to you, sits with you, hums at you.
- Far more lines in every mood and stage (`EtoLines`, `EtoTopics`), including reactions to what you did (a kill near her, a long
  absence, an injury, the weather, the time).

### 2.8 Future NPCs (do not start; the owner will describe them)

The owner plans teachers who unlock abilities, and more partners. Everything above is meant for them. Note: the build's name check
(`check_names` in `build.py`) refuses some show character names in the jar (it lists them); ask the owner before naming anyone.

## 3. How the owner works (on top of `eto-handoff.md` section 4)

- They play on Android (Amethyst launcher) and test jars from CI. Crash reports come as uploaded text files.
- They type fast, by voice: read for intent. Keep replies short; say plainly what was and wasn't tested.
- Push when a step is done (after a green local build). Release notes one line per item.
- When something visual changes, render a preview (`tools/UiPreview.java`, `tools/art-preview.sh`) and look at it yourself.


## 4. Done since (1.3.5 to 1.3.12, branch `claude/festive-cray-l5b2pb`): compiled, tests pass, NOT tried in game

The owner's answers to the open questions: **a female player's hitbox and eyes shrink for real** (Forge's size event, both sides);
**a pregnancy is asked for, then the next night** (married couples only: "Let's have a baby." in Eto's "us" topic); **female players
wait for male NPCs** (the system carries any mother, but no one can father a female player's child yet; no player-player marriage);
**children respawn at home** a day after dying. Also: **children's skin tone comes only from their parents' real skin** (read off each
player's skin), never at random; the owner rejected hand-painted child skins: they are now built from CC BY-SA parts (below).

| Release | What | Where |
|---|---|---|
| 1.3.5 | Holding hands: both held arms drop vanilla's swing (`RigPose.leftArmHold`, the player's `rightArmHold`), stretched to ~15 degrees | `npc/EtoHand.java`, `client/animation/EtoPose.java`, `KaguneBodyAnimation` |
| 1.3.6 | Eto's flesh arm (swipe, grab), flesh leg (Flesh Hammer), Watch This | `server/EtoFlesh.java`, `server/EtoWatch.java`, `EtoRules.choose(..., Ready, ...)` |
| 1.3.7 | Eto's Mutated Arm | `server/EtoArm.java` |
| 1.3.8 | Eto's Owl kakuja below 25% health (Rend, Reaping Whirl, Pounce; half damage taken) | `server/EtoOwlForm.java` |
| 1.3.6-8 | The Owl DNA renderers draw on any body: `Frame` has a scale; `WatchClient`, `ArmClient`, `OwlClient` keyed by entity UUID; `EtoRenderer.kit` hands over each Eto's drawn pose | `client/render/PaleFleshRenderer.java`, `EtoRenderer`, `EtoFight` |
| 1.3.9 | Gender screen after the eyes (Done is Next); one shared female body (`FemaleBody`, `FemaleFigure`): Eto and female players (0.86, slim arms, hips, bust layer); real shrink | `client/GenderView.java`, `GenderScreen`, `Genders`, `server/GenderServer`, `client/render/FemalePlayers`, `FigureLayer` |
| 1.3.10 | Pregnancy (`family/Pregnancy`, `server/Pregnancies`: any mother "player:<uuid>" or "npc:<key>"), belly, alerts, the birth cutscene ("9 months later..."); children (`npc/ChildNpc`, `server/Children`, `ChildBrain`, `ChildTalk`, `family/ChildStage`, `ChildRules`, `ChildLines`, `Genome`) | protocol 59 |
| 1.3.10 | Child looks: hair and skin tone read off each player's own skin (`family/HairColour`, `SkinTone`, `SkinReader`, `C2SLooks`); skins composed on the client from edit_skin parts (`family/ChildSkin`, `family/Png`) | parts and CREDITS in `resources/assets/hexkagune/textures/entity/child/` |
| 1.3.11 | NPCs react to what a player shows (`npc/Display`: eyes, kagune, kakuja, dread) | `server/Sightings.java` (one hook: `Sightings.watch`), `EtoBrain.saw`, `npc/ReactionLines` |
| 1.3.12 | Eto starts things herself (hand, cling, hug, lean, hum, read, dance) and remarks on moments (rain, dusk, dawn, your wound, a kill) | `npc/EtoInitiative.java`, `EtoBrain.together/moments/killedNear` |

Testing shortcuts (operators): `/kagune family soon` (a pregnancy you are part of: the birth in ten seconds), `/kagune family grow`
(your nearest child: the next stage), `/kagune eto max`.

### Riskiest, untried in game (check first)

1. **Female players**: drawn scaled in `RenderPlayerEvent.Pre/Post` (LOWEST; a pose pushed and popped); the legs' pivots spread through
   `KaguneBodyAnimation.get3DTransform` (PlayerAnimator resets them each frame), which makes that layer always active for women; the
   bust as `FigureLayer` with the upper body's bend. The hitbox change runs on both sides (desync if a client misses `S2CGender`).
2. **Children**: carried by teleporting each tick on the server and glued in front of the carrier on clients; the size event per stage;
   `UniqueNpcs` prefix kinds remake a dead child's body at home; the skin made with `NativeImage` + `DynamicTexture` on the client
   (parts read by the mod's own PNG reader through `getResourceAsStream`). A baby sits by holding its navigation still each tick.
3. **Eto's new moves on an NPC body**: the grab and the arm carry players by velocity each tick; the Owl creature drawn in her place
   while her model is posed but not drawn.
4. **Hair/skin read**: phones that can't read textures back (SkinReader says so in the log) fall back to brown hair, light skin.

### Not done (next)

- The Owl on Eto lacks its Big Punch, Speed Kick, bone spikes, Fissure, volley, lances and great hand (players' Owl has them).
- No player-player marriage (so no pregnancy for a female player until a male NPC partner exists). Children of two players: the
  system supports it (keys), nothing triggers it.
- Children never become adults (teen is the last stage). No child names chosen by the parents (picked from the seed).
- First-person: a female player's own hands are not slimmed.
