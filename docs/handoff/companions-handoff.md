# Handoff: HexKagune 1.3.14 (Touka and Ken, teachers and companions; hands held alive), for the next agent

Read this first; it replaces `next-handoff.md` as the place to start (that file is still right about the project's basics, Eto, genders,
pregnancy, children and reactions: read its sections 1, 2 and 4). Everything here is "compiled, tests pass, not tried in game".

## 0. Where things are

- **Branch: `claude/relaxed-turing-r5599k`**, at 1.3.14 (`VERSION` in `build.py` and `patch/KagunePatch.java`), network `PROTOCOL` **"60"**.
  It was started from `claude/festive-cray-l5b2pb` (1.3.12). **`main` is still at 1.3.0**; never push there unless the owner says so.
- Releases this session: **1.3.13** (hands held alive) and **1.3.14** (Touka, Ken, teaching, the trial, partner immunity).
- The owner's skins for Touka and Ken are in `art/touka_skin.png` and `art/ken_skin.png` (64x64, both slim-armed, sent as a zip).
  `python3 tools/companion_textures.py [preview-dir]` turns them into textures (see section 3). **Never repaint or "improve" the owner's
  skins**: the tool copies the skin as it is and only recolours the detected eye pixels for overlays. The owner noticed the stand-ins I
  painted before the files arrived (a mouth, plain brows) and was not happy. Ask for files when you can't read an image.

## 1. What the owner asked for (their words, condensed)

- Hands held: "more lively... swing a little more... not the run swing... a nice wobble... let her go a little further and the hand follows
  and a little behind if I pull her". And: while holding hands, nothing the player does (kagune out or not) may harm the partner.
- Two NPCs with their own moods: **Touka** (strict, serious, pouty; blinks and blushes like Eto; breaks composure and blushes a lot; can walk
  up and give a tiny pouty kiss; making out with her works like Eto's) and **Kaneki** (displayed as **Ken**, see the name rule below).
  Ken is **only interested in women**; make him "a really big lover", desirable for women: the owner (a man) left his romance to us.
- Most interesting abilities now need mastery **and** finding the teacher. Touka teaches the Ukaku path: "turn your wings into crystals"
  (Crystal mode) and how to advance. Ken teaches the kakujas (centipede) and the final forms of each path, through a cutscene of the
  tortures he went through. Both are dateable and marriable; partner or not, anyone may learn; others' partners get a cold shoulder.
- They don't spawn naturally yet: the owner is building an Anteiku cafe for them. Commands only for now.
- Pregnancy: "keep the pregnancy as it is, don't change it"; just make sure it works and the belly grows. (I tried a day-clock version
  that followed `/time add`; the owner said no, it was reverted. The timing is game ticks, two Minecraft days.)
- Next on their list, after the companions: **wild ghouls**, male and female, with many good skins from the internet (NameMC), eyes adapted.
  Not started: see section 6.

## 2. The persona system (built once, for every companion to come)

All under `src/main/java/com/hex/kagune/control/`. Eto's classes are now the shared companion machinery; a companion is a `Persona`.

| Piece | Where |
|---|---|
| Who each is: key, name, colour, size, female body or not, slim arms, health, genome colours, one-eyed, Owl kit (Eto only), how readily it kisses/starts things, idles by mood, its errand item and last gift, whether it pecks, whether it leads (Ken's poses), what makes it blush, whom it dates (`likes`) | `npc/Persona.java` (ETO, TOUKA, KEN; `ALL` order is sent over the network: never reorder) |
| Every word: the talk's pages, topics, sweet page, answers, and every unasked line | `npc/Script.java` (interface); Eto's is `npc/EtoScript.java` (delegates to `EtoTalk`, `EtoLines`, `EtoTopics`, `EtoInitiative`, unchanged) |
| The shape of a companion's talk after Eto's (pages, gifts, errands to date, lessons instead of her pact/surgery) | `npc/CompanionScript.java` |
| Touka's and Ken's words and topics | `npc/ToukaScript.java`, `npc/KenScript.java` |
| The bodies | `npc/ToukaNpc.java`, `npc/KenNpc.java` extend `npc/EtoNpc.java` (now the companion body: `persona()`, `displayName()` (a name tag renames it everywhere), `rehome()`); registered in `ControlEntities` (`hexkagune:touka`, `hexkagune:ken`), one each per world (`UniqueNpcs.kind`) |
| The mind and the talk server, persona-aware | `server/EtoBrain.java`, `server/EtoServer.java` (every key, name, line and idle comes from the persona) |
| Drawing: per-persona textures, size, female figure or not; Touka's blush at anything sweet | `client/render/EtoRenderer.java` (`new EtoRenderer(context, persona)`), `client/render/EtoModel.java` |
| Talk screen faces: skin byte -1 Eto, -2 Touka, -3 Ken | `EtoServer.face`, `client/NpcTalkClient.companion` |
| Testing commands (op) | `/kagune <eto|touka|ken> max [player]`, `/kagune <eto|touka|ken> here` (brings the one of them there is to you and makes it home; made if none is loaded; partner kept) |

**Name rule:** `build.py check_names` refuses "Kaneki", "Ayato", "Furuta" anywhere in the jar (the owner's 1.2.5 rule). So he is "Ken" in
code, lines and lang; a name tag reading "Kaneki" renames him in game (lines, talk screen, tag). Touka never names her brother.
`CompanionTests` checks every line against the same pattern. And `PaleTests` refuses "Takatsuki", "Aogiri", "Yoshimura" in any string a
player can see (Eto's alias and her people): Ken loves "a writer nobody's ever seen", unnamed.

### Behaviour per companion

- **Touka:** idles CROSS, TAP, HUFF, SIP, WIPE, KICKS, SCOLD, SHY, READ (studying); kisses at 0.35x Eto's rate and starts things at 0.5x;
  blushes on KISS, HAND, CLING, LEAN, HAIR, DANCE, HUM, PET, RING too. Errands: 3 cocoa beans ("coffee beans"), 10 kills at night, a
  rabbit's foot. Dates anyone.
- **Ken:** idles CHIN, CRACK, BOW, READ, SIP, SWEEP, WRITE; kisses at Eto's rate, starts things at 1.25x; **dates women only**
  (`Persona.likes`, `KenScript.romantic`, the DATE effect guard); with men he is a friend and teacher. His romantic poses
  (`EtoPose.led`): the kiss (hand to their cheek, one at their waist, leaning down), making out (both arms round their waist), the peck
  as a forehead kiss, a wrap-around hug, an arm round their shoulders for CLING, a hand to the back of his neck after a kiss. Errands: 3
  books, 10 kills at night, a white flower. With a female partner, **she** carries their child (`EtoServer.conceive`: whoever is the woman).
- Both **peck**: from `askForKiss`, 65% of the time they walk up and give a quick kiss themselves (`EtoGesture.PECK`); otherwise the
  full kiss wait, Touka hinting first (`Script.kissHint`). On-screen kiss text per persona (`Script.kissWaiting`).
- Fight: the eye-mode kit only (`EtoRules.Ready.only`); Owl DNA moves and the Owl form stay Eto's (`EtoOwl`).

## 3. Textures (`tools/companion_textures.py`)

Reads `art/<key>_skin.png` and writes `textures/entity/<key>/<key>{,_kakugan,_kakugan_glow,_closed,_blush}.png`. Eye detection on the face
front (columns 1-6): whites are bright and at most half as saturated as the skin (and not hair-coloured above the eyes); irises are the
coloured pixels in and next to the whites' rows; the dark row above is the lashes (never recoloured). Kakugan: whites black, irises red
(darker top row), a small vein at the outer corner. Touka turns both eyes; Ken his left only. Blink: the eye's top row in the lash colour
(the lid coming down under the skin's own lashes), the rest skin. Blush: under the eyes. Found for the owner's skins: both have eyes on
face rows 5-6, lashes on row 4; Touka's bangs (outer layer) cover part of her right eye. `EYES` in the tool overrides detection if needed.
Previews: pass a folder; it writes `<key>-faces.png` (skin, turned, glow, shut, blush). Posed previews: `tools/CompanionPreview.java`
(see its comment for the command; PoseRender in the owner's skins).

## 4. Teaching (`npc/Teaching.java`, pure)

| Lesson | Teacher | Gate | Kind | Needs |
|---|---|---|---|---|
| Crystal Wings | Touka | `UKAKU_CRYSTAL` (new, mastery 200; crystal mode, V) | talk | |
| Gatling | Touka | `UKAKU_GATLING` (400) | talk | Crystal Wings |
| The First Kakuja | Touka | `UKAKU_KAKUJA` (750) | talk | Gatling |
| The Centipede | Ken | `RINKAKU_CENTIPEDE` (750) | **trial** | `RINKAKU_FOUR` (still quest-opened) |
| The Full Kakuja | Ken | `RINKAKU_FULL_KAKUJA` (1000) | talk | the centipede |
| The Dragon | Ken | `RINKAKU_DRAGON` (1000) | talk | the Full Kakuja |
| The Final Kakuja | Ken | `UKAKU_FINAL_KAKUJA` (1000) | **trial** | the Ukaku first kakuja |

- A taught gate is no longer opened by quests: `QuestBook.add` strips it from rewards (the quests still pay mastery and sides).
  `QuestServer.pass` says who teaches it. Crystal mode checks its gate in `UkakuServer.toggleCrystal`.
- `QuestLog.VERSION` is 2: a save migrated at 1 gets `UKAKU_CRYSTAL` if its mastery was already past 200 (nothing else re-migrated).
- In talk: `EtoTalk.Context` gained `teaching` (a `Teaching.View`) and `mixed` (a woman and a man: a baby is possible; `babyOpen()`).
  Effects `LEARN` (opens the gate now: `EtoServer.taught`) and `TORTURE` (`server/TortureServer.java`: the cutscene
  `CutsceneOverlay.TORTURE` / `S2CCutscene.TORTURE`, ~46 s; the player held still and unhurt through it, then left at 15% health with
  regeneration, then the gate opens; logging out mid-trial teaches nothing).

## 5. Other changes this session

- **1.3.13 hands held alive:** `npc/HandSway.java` (pure spring: trail behind when pulled, overshoot on stopping, swing out on turns; arms
  reach along the gap; a slow shared sway on its own clock), `client/HandClient.java` (steps it per pair), `EtoRenderer.glue/alive`,
  `KaguneBodyAnimation.setupAnim`.
- **Partner immunity:** `EtoBrain.spared(player, body)` (holding hands, clinging, hugging, kissing: `mind.holding` or `mind.with`);
  `EtoBrain.attacked` cancels any attack caused by that player; `KagunePatch.patchSpared` makes the original's `isHostileTo` and
  `validSwipeTarget` return false for them (so autonomous tails and swipes never pick them).
- **Pregnancy (timing unchanged):** after "Let's have a baby." the partner comes to them at night by herself (`EtoBrain.together`,
  `mind.babyNight`) and the night (and conception) follows on arrival. A baby needs a woman and a man (`conceive` checks `c.mixed()`;
  Eto's baby options use `babyOpen()`). `/kagune family at <percent>` jumps any pregnancy you're part of to that point (belly shows from 20%).
  If the owner reports the belly not growing: it shows from 20% of 48000 game ticks (8 real minutes) and fills out over the rest;
  `/time add` does not move it, by their choice.
- Allowlist: `Entity.m_7770_` (getCustomName) and `Component.getString` (name tags).

## 6. Not done, and what's next

- **Wild ghoul skins** (owner's next item): this environment's network policy blocks NameMC, The Skindex, Planet Minecraft and Mojang's
  texture server (only GitHub is reachable). Either the owner allows those domains (environment settings, Network access) or sends a zip of
  skins. Plan once skins are in: put them in `art/ghouls/{boys,girls}/`, generalize `npc/NpcLook` from its fixed eye spots to per-skin
  `KakuganEyes` found by the same detection as `companion_textures.py` (generate a small table), keep the girl/boy split. Licensing: the
  earlier rule "never ship skins from skin sites" was an agent's caution; the owner asked for NameMC skins, so it's their call.
- Touka's and Ken's kagune (Ukaku shots, Rinkaku tails) on their NPC bodies: `GhoulNpcCombat` is tied to `GhoulNpc`; worth generalizing.
- Natural spawning: the owner's Anteiku cafe structure (then place them like Eto in the Library: `UniqueNpcs`, structure NBT).
- Eto's lessons are still her own system (`EtoRules.Lesson`); she could be moved onto `Teaching` later.
- Nothing above has been tried in game. Riskiest: the bytecode guard in `isHostileTo`/`validSwipeTarget` (runs constantly; wrapped in
  try/catch), Ken drawn on slim arms with no figure, the torture's effects (Slowness VII, Jump Boost 128 to pin, Resistance V), the
  `here` command adopting a new body while an old one is unloaded somewhere.
