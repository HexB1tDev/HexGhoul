# HANDOFF: HexGhoul after 3.5.3, with the Bikaku kakuja started (not released). Read this first.

Then `docs/BIKAKU_PATH.md` (the owner's whole Bikaku spec word for word, the plan, every hook), `docs/handoff/next-ai-3.4.8.md` (the
session before: its words, judgment calls and house rules, kept current through 3.5.3), `docs/handoff/next-ai-1.3.60.md` (the house rules
in full), and the top sections of `docs/handoff/companions-handoff.md` (one per version, newest first, file-level detail).

## The session after this note (branch `claude/hopeful-noether-5c2wex`, started from `7b0b94d`): its owner's words

Binding; quoted exactly as written. Kept current as that session goes.

- The ask: **"What I want next: first show me the kakuja previews (docs/handoff/bikaku-kakuja-3.5.4.png and
  `bikaku-kakuja-close-3.5.4.png`) and ask me what I think before you ship it; fix what I say, then ship it as 3.5.4. Then the kakuja's
  four abilities and its passives (3.5.5), all opened by mastery alone (a custom NPC will teach them later). Leave the hammer and the
  whip as they are. Show me previews of what you make before you go far. If I report bugs, fix those first, each its own version; if my
  exact words for a report aren't in the handoff, ask me. When a version is done, tell me plainly what you made, what you guessed at, and
  what's next."**
- Before the previews were sent again (the last session had sent them): **"Gotta say, before you continue, all the attacks that we
  currently have, they need way more range. This is meant to be basically more range than the Rinkaku, and yeah, I like the look of the
  mod, like, so far I like this new design. I actually quite love it, it's just it needs a little a few touches, it needs to be more
  flexible. The thing must feel a little more alive, if you get what I mean. I'm struggling to describe what I want exactly, but as well
  as we need more forms. Currently there's only one one of these things, but another form should have like two worms. The third should
  have three, and honestly this is where it gets hard, but I think that the fourth before the Kakuja should be like the player no longer
  even like walks with their own legs, like they get like two separate appendages on either, like both sides emerging from the same spot
  as the worms, that when the player walks his body doesn't move, but like he controls these tentacles that walk around adapt to
  terrain. Being able to climb anything, like Dr. Octopus pretty much. If you look up Dr. Octopus from Spider-Man, you can look at how he
  walks. That's kind of what I'm trying to describe. The moves, of course, changing to something else whenever you have like depending
  on the form. That's what I'm trying to go for here. Try to adapt these changes once you're done with the final form that you currently
  making. And then we can continue."** (Also in `docs/BIKAKU_PATH.md`. Asked them: is "more flexible ... more alive" about the kakuja
  pictures or the worm in game, and does "done with the final form" mean after 3.5.4 or after 3.5.5 too.)
- Their answer, on seeing the kakuja previews: **"I quite like the kakuji that you made, if I'm being honest. When I say more flexible,
  I mean, like, make sure that the tentacles don't go through a player's body. I noticed that a few are doing that. Just, like, a few
  moves. Like the predatory drag looks a little strange when you send it and it goes through the player's body instead of going around
  it. And again, the range is really the problem because it shouldn't go far. Like, it should be a held move that the longer you hold it
  for, the longer it goes for. And I, if you ask me, this specific kagune should have a lot of range, like covering up to like 30 blocks.
  Of course, attacks are not able to be performed until the kagune is pulled fully back. So that's the only disadvantage that this
  kagune has. It should adapt to blocks, and that's where I say flexibility, like go around blocks like a worm, sort of, and actually go
  around them. It's being able to adapt to the terrain itself and pulling people back and shit, you know what I mean? Just carry on, do
  what you're doing, and then cover everything that I said. But so far, so good, though. I'm, like, happy with the results. Just cover
  all this that I told you and adapt the changes in the new forms, and then we'll see."**
  So the kakuja shipped as it was (3.5.4). The order taken (a judgment call, told them): 3.5.4 the kakuja; then the reported bug first
  (the worm through his own body, Predatory Drag above all); then the kakuja's four abilities and passives; then the range (held, up to
  about 30 blocks, round blocks, no attack until it is fully back); then the forms (two worms, three, the walking form), each with its
  own moves.

- After 3.5.5, then **"Continue"**, then, with a crop of 3.5.5's preview (`bikaku-drag-round-3.5.5.png`, the "AFTER, ahead, from above"
  row: the Drag's worm coming out of his back round his right side, hugging it): **"why not make a bigger angle? it's going too close to
  the body"**. Taken as a report on 3.5.5, so first (3.5.6): the Drag's worm swings out wide round his right side; the kakuja's moves after.

## State now (kept current by the session on `claude/hopeful-noether-5c2wex`)

- **3.5.4 shipped** (the kakuja as the last session left it; the owner liked it).
- **3.5.5 shipped:** the owner's report fixed (the worm through the player's own body; `MawMotion.around`, the Drag rerouted, its catch
  dragged to its own side). Protocol "97"; next free message id 56.
- **3.5.6 shipped:** the owner's "bigger angle" (Predatory Drag swung out wide round his right). Versions 3.5.6.
- **3.5.7 shipped:** the kakuja's four moves and Autonomous Predation (`MawKakuja.Act`, `KakujaActs`, `BikakuMoves`). The preview was
  sent to the owner before the server side (`docs/handoff/bikaku-kakuja-moves-3.5.7.png`, `bikaku-kakuja-undying-3.5.7.png`); no answer
  before it was pushed. Versions 3.5.7; protocol "98"; next free message id 56.
- Then **"PROCEED"**. **3.5.8 shipped:** held to throw further (M1 and G/J/Z/B; a tap as before, 30 blocks after 2 s; slower out and back;
  nothing else until it is back). Preview sent first (`docs/handoff/bikaku-held-3.5.8.png`). Versions 3.5.8; protocol "99".
- **3.5.9 shipped:** round blocks (`WormRoute`, `BikakuRoutes`, `MawMotion.along`; M1, Maw, Drag, Pursuit; catches pulled back along the
  way). Preview sent first (`docs/handoff/bikaku-round-3.5.9.png`). Versions 3.5.9; protocol "100". **The next version is 3.6.0.**
- **Next: the forms** (the owner's words above: two worms, three, then the walking form before the kakuja, "the moves, of course, changing
  to something else ... depending on the form"). A plan for the owner first (how a form is chosen, its mastery, which moves each has), then
  previews before going far.

## Where things were when this note was written (after 3.5.3)

- **Branch:** `claude/peaceful-ramanujan-eldmlu` (pushed). Start yours from it:
  `git fetch origin claude/peaceful-ramanujan-eldmlu && git checkout -B <your-branch> origin/claude/peaceful-ramanujan-eldmlu`.
  Push only to the branch you are given; never to `main`; no PR unless asked.
- **Released:** 3.4.9 to 3.5.3 (`.github/workflows/release.yml` publishes `v<version>` for a push that adds
  `.github/release-notes-<version>.md`; the owner plays those).
- **Version 3.5.3** in `build.py` and `patch/KagunePatch.java`. **The next is 3.5.4.** Protocol in the tree: **"97"** (the released
  3.5.3 is "96"; the unreleased kakuja work bumped it, see below). Next free message id: 56.
- **The last commit is unversioned work in progress:** the Bikaku kakuja's body, transformation and gameplay (below). It compiles, the
  API verifier passes, and `quick_check` passes BikakuTests (1,335), BikakuWormTests (77,062), ControlTests, QuestTests. **No full
  `python3 build.py` was run on it, nothing of it was released, and the owner stopped the session before it was finished.**
- Full build: `python3 build.py` (about 6 minutes; run it in the background; don't edit sources while it runs). Quick loop:
  `python3 tools/quick_check.py [--noverify] Suite ...`. **Nothing is tested in game. Never claim it is.**

## What this session did (details: the top sections of `companions-handoff.md`)

| Version | What |
|---|---|
| 3.4.9 | Bikaku in the game: a third choosable path; the worm (about 8 blocks of zigzag coils, a round mouth of fangs) in the owner's kagune colour, alive at rest, long (a bite uncoils it only as far as it needs); M1 a string of three bites (Snap, Lunge, Maul); Predatory Feeding; first person kept clear; card, menu, move guide. |
| 3.5.0 | Devouring Maw (G) and Predatory Drag (J, mastery 150), on the original's kagune keys (`BikakuInput`, `C2SBikakuAction`); grabs held at the mouth by the server. |
| 3.5.1 | Ravenous Pursuit (Z, 300): three thinner worms in their own wide lanes (reworked after the owner's report, before release); Carrion Eruption (B, 450): tunnels under the ground and bursts up under its mark (`client/fx/BikakuFx`). |
| 3.5.2 | Fix (found by reading): Bikaku's small heals were cancelled as food's natural regeneration; `GhoulRegen.healOwn`. |
| 3.5.3 | Monstrous Regeneration (C, 600: kagune strands crawling over the body, `MawTissue`) and the passives (Abnormal Regeneration, Pain Suppression). |
| (no version) | **The kakuja started** (see "Unfinished" below). |

## The owner's words this session (binding; quote them as they are, never dress them up)

- The ask: **"What I want next: my Bikaku path (the worm kagune with the mouth), built in the versions planned in `BIKAKU_PATH.md`,
  starting with 3.4.9: the path in the game with the worm drawn, alive, in my kagune colour, with its M1 bites. Keep it long, with its
  zigzags; I liked that design. Leave the hammer and the whip as they are; they're good. Show me previews of what you make before you go
  far. If I report bugs, fix those first, each its own version; if my exact words for a report aren't in the handoff, ask me. When a
  version is done, tell me plainly what you made, what you guessed at, and what's next."**
- After 3.4.9's previews: **"I think it's fine now. You can continue for the final stuff. Transformation and I don't know if the
  abilities are done or not, but if they aren't, carry on. You're free to continue. These abilities are not going to be taught by anyone
  currently in the mod. We're going to advance to that later. It's going to be a custom NPC. So"** (cut off). So every Bikaku move opens
  by mastery alone: no Ken/Eto lessons, no quest gates.
- On Ravenous Pursuit's first preview (the three worms seen from behind): **"Being completely honest, it looks like a mess. Don't be afraid
  to separate the more and make them wider, like away from the body. That's a mess, bro. Everyone can tell that is a big"** (cut off).
  Then, when my reply paraphrased it with things they hadn't said: **"Dude, I don't know what the fuck this translation is. I said none
  of that. I said that they're too close up to each other to make it's like forming a kind of mass in the back. I tried to tell you that
  it just like separate them more, make them further away from the body if need be, just so it doesn't create that mass in the back
  ever."** Fixed in 3.5.1 (each worm in its own lane; a test keeps them at least 0.85 apart behind him). **Lesson: repeat the owner's
  words exactly; don't add images or ideas of your own to them.**
- **"Prepare a handoff and a prompt for the next AI to pick up."** then **"No I said, NOW you aint finishing shit"** (this file and
  `next-prompt-3.5.4.md`, written at once; 3.5.4 was left unfinished).

## Unfinished: the Bikaku kakuja (in the last commit, unversioned)

What is in the tree (all untested in game; the owner was sent `docs/handoff/bikaku-kakuja-3.5.4.png` and
`bikaku-kakuja-close-3.5.4.png` and **had not answered** when the session ended: ask them first what they think of it):

- Pure `bikaku/MawKakuja`: the body (7 breathing lumps piled round him, lopsided, about 4.8 high, growing out of and shrinking into his
  middle, glowing seams), 8 grinning mouths (thick lips, rows of big flat teeth, each opening on its own rhythm), the great maw (the worm's
  own mouth at `MAW_GIRTH` 3.2), 6 tentacles (`TENTACLES`: out every way and curling round it, girth 1.8 to 2.2, 9 to 12 blocks, 5 with
  mouths, one tapering: `MawWorm.build(..., mouth false)` / `MawWorm.tip`), the wrapping tendrils of the transformation (`wrap`), the
  transformation's growth (`growth(kakuja, age)`: in over `IN` 60 ticks: the worm swells and thrashes, tendrils wrap round him, the lumps
  swell up and swallow him, the mouths open, the tentacles burst out overshooting; out over `OUT` 30 ticks, faster), the tentacle bite
  (`biter`, `tentacle(i, time, lunge, reach)`, `lunge(age)`, `lungeMouth(age)`).
- `BikakuRules`: `Bite.TENTACLE` (18 ticks, lands 9, power 1.8, appended), `KAKUJA_COST` 25, `KAKUJA_DRAIN` 0.05 a tick,
  `KAKUJA_COOLDOWN` 1200, `KAKUJA_TAKEN` 0.45, `KAKUJA_REACH` 14 (from `KAKUJA_CENTRE` (0, 2.4, 0)), `KAKUJA_REGEN_EVERY` 8, `KAKUJA_SLOW`
  5 (Slowness amplifier: "extremely slow"; jumping is not blocked), `KAKUJA_STEADY` 1.0.
- `BikakuServer`: `kakujaKey` (K through `UkakuServer`: mastery 1000, its RC, its cooldown, not mid-transformation), `leaveKakuja`,
  `kakujaTick` (RC drain, out when dry with `message.hexkagune.bikaku.kakuja_spent`, Slowness, regeneration 1 health / 8 ticks through
  `GhoulRegen.healOwn`), `hurt` (a LivingHurtEvent: takes 45%), M1 in the kakuja is the tentacle bite (`begin`/`land` with `aimFrom`,
  `kakujaCentre`, `mouth(from, aim, reach)`), the move keys say "not yet" in it, `steady(player, on, amount)` (full knockback resistance in
  it; the amount remembered in `STEADY_AMOUNT`), `kakuja(player)` (QuestServer's tag). `S2CBikakuState` carries `kakuja`, `kakujaAt`
  (protocol 97); `BikakuClient.View.kakujaAge`.
- `BikakuRenderer`: the worm swells and fades into it (`growth.swell/worm`), the kakuja built in his body's frame through a placing
  sink, the tentacle bite; in first person nothing of it within `KAKUJA_CLEAR` 2.6 of the eyes (he is inside it).
- Board (`AbilityGuide.bikaku`: a K row locked to 1000; in it a "KAKUJA" board: tentacle bite, rooted · tough · regenerating, turn
  back; `AbilityHud` passes the Bikaku kakuja as the Input's kakuja flag), menu (`BikakuMenuView` KAKUJA lines; "The kakuja's own moves"
  under "coming"). Preview: `tools/MawPreview.java kakuja`. Tests: BikakuWormTests `kakuja()` (height 4 to 5.4, wide, lopsided, finite
  sane meshes, tentacles reach 7+, most mouthed, growth never jumps either way, the bite lands), BikakuTests.

**To ship it as 3.5.4** (after asking the owner about the previews and fixing what they say): bump both versions to 3.5.4, write
`.github/release-notes-3.5.4.md` and a README entry, add a top section to `companions-handoff.md` (rename "START HERE (state after
3.5.3)"), the header and log lines of `next-ai-2.0.45.md`, update `BIKAKU_PATH.md` step 5, run the full `python3 build.py` to EXIT 0,
commit `3.5.4: ...` with the session's trailers, push.

## What's next after 3.5.4

**3.5.5: the kakuja's four abilities and its passives** (the owner's spec in `BIKAKU_PATH.md`): Thousand Jaws, Flesh Cataclysm, Endless
Hunger, Undying Abomination; Autonomous Predation (its tentacles bite nearby hostiles on their own at sensible intervals). Put them on the
same keys as the base moves while in the kakuja (`BikakuServer.onAction` says "not yet" there now); use `MawKakuja.tentacle(..., lunge,
reach)` for the tentacles' strikes; mastery alone opens them.

## Judgment calls the owner hasn't confirmed (tell them, ask if unsure)

- 3.4.9 to 3.5.3: in `next-ai-3.4.8.md` (bites, reaches, damage, heals, cooldowns, the first-person aside, the grabs, Pursuit's lanes,
  the eruption, the regeneration and passives' numbers, strands only near the joints on limbs).
- Kakuja (unreleased): its whole look (lumps, grins, the maw, tentacle layout), the transformation's timing, its numbers (25 RC, drain,
  60 s cooldown, takes 45%, reach 14, regen 1 / 8 ticks, Slowness VI), jumping not blocked, the moves "not yet" in it.
- Still open from before: the shut mouth of the worm (the owner's cut-off "It doesn't looks like there's"; reworked as a puckered lip,
  never confirmed) and the reference pictures (`Reference.zip`, not in the repo: ask for them again).

## House rules (unchanged; full text in `next-ai-1.3.60.md`)

Each fix or feature is its own version (the last number never reaches 10), with both version strings, release notes, a README entry, a
new top section in `companions-handoff.md`, the header and log lines of `next-ai-2.0.45.md`, a full `python3 build.py` to EXIT 0, then a
commit (`3.x.y: ...`, trailers `Co-Authored-By: Claude <noreply@anthropic.com>` and the session's `Claude-Session:` line, no model names in
anything pushed) and a push. Owner's bug reports first, each its own version; when their exact words for a report are missing, ask them.
Banned in the jar: Kaneki, Ayato, Furuta; in visible text: Takatsuki, Aogiri, Yoshimura; no character names on screen (paths are named by
kagune type: "Bikaku"; keep "Noro" out of visible text). Sounds only from the hexkagune pack (real recordings; never make one). Never alter
the owner's files. New Minecraft API needs an allowlist entry with [srg]/[forge] evidence plus a stub (this session needed none). Leave the
hammer and the whip as they are. Show previews early (`tools/MawPreview.java`: look, mouth, bite, bites, firstperson, grabs, pursuit,
eruption, regen, kakuja, grow, colours; `tools/UiPreview.java <out> bikaku|hud`; `tools/PathScreenPreview.java`). Answer in English,
plainly; say what is untested and what was guessed.
