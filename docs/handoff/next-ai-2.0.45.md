# HANDOFF: HexGhoul after 2.0.45 (updated through 2.1.2: see "Since 2.0.45" and "Since 2.1.0" below). Read this first, then `docs/handoff/next-ai-2.0.36.md` (2.0.27 to 2.0.36 and what to ask the owner),
`docs/handoff/next-ai-2.0.21.md`, `docs/handoff/next-ai-2.0.9.md`, `docs/handoff/next-ai-2.0.5.md` (how the owner works),
`docs/handoff/next-ai-1.3.60.md` (house rules), then the top sections of `docs/handoff/companions-handoff.md` (one per version, newest first).

Branch **`claude/loving-albattani-xld4zv`** (push only to the branch the session is given). Never push to `main`; no PR unless asked.
Version **2.1.9** in `build.py` and `patch/KagunePatch.java`; the next is **2.2.0** (the last number never reaches 10). Protocol **"82"** (`ControlNetwork.PROTOCOL`; 2.1.9 added the synced AFRAID on CcgAgent and GhoulNpc; 2.1.7 added `C2SUkakuAction.REAPER_DASH`; 2.0.42
added `S2CRinkakuBlock`, message id 48, and `C2SUkakuAction.BLOCK`; 2.0.48 `S2CUkakuFx.DAWN_RING`; 2.0.49 `CcgAgent.SOARING` on the flip). Nothing is tested in game.

## The owner's requests since 2.0.36 (their words, trimmed)

> "change the texture to match the reference picture on the tail ... attach properly to the body, like actually attach to it, not just
> glide behind it ... add some texture to the skin as if it's like, you know, red on the area it's emerging from ... just like the eyes
> are ... completely delete the C move called guard ... right click with the Rinkaku ... a new move called blocking. While holding right
> click, your kagune is going to go in front of you ... a defensive position with your hands, like ... covering your face ... You block
> literally all moves ... if you get too many hits, your kagune will like be pushed back and the move will go on cooldown. Perfectly timed
> blocks ... cause the enemy to stagger ... it has to be extremely perfectly timed ... you don't get knocked back, and you get a free shot
> ... a small ambient animation to the White Reaper ... really edgy and as if he knows that he can kill you at any point ... super relaxed
> overall, lean back ... just a pose ... adding some muscle to him ... The top part of the torso, should be just a little bit bigger than the
> one in the bottom that connects the legs ... make the chest subside a little bit from the belly ... don't actually make muscle ... Just
> simply increase the size of parts of the already existing player structure. Dawn drops"
>
> "remember we gotta adapt the texture to the different colors" / "Continue, and improve the kagune texture and material to be the same
> quality and beauty of ukaku, veins inside and etc"

The message ended at **"Dawn drops"**: it was cut off. I asked what they meant and got no answer. **Ask again.** Current behaviour: the
Reaper always leaves his case (Dawn) to a player who kills him (2.0.32), and Dawn is in the creative Combat tab.

## What shipped (each its own version; details at the top of `companions-handoff.md`)

- **2.0.37** Rinkaku's C guard removed (never raised again, gone from the ability guide). C still serves Ukaku's Steel Blade and Graft.
- **2.0.38** the tails grow out of the lower back (`RinkakuRoot`: root sunk in the body, a rigid base fanned to each tail's side and lane,
  blended into the tail's motion; `StrikePaths.attachRoot`).
- **2.0.39** the tail per the reference: a flat crimson ribbon, darker edges, chevrons and a light stripe (`RinkakuRibbon`,
  `client/render/RinkakuTube`, `tools/rinkaku_scales.py`). Markings are drawn in a deferred pass after entities.
- **2.0.40** the emergence mark on the skin (`RinkakuMarkLayer`, `tools/rinkaku_mark.py`): a torn glowing opening and veins on the
  lower back while the tails are out.
- **2.0.41** the tails' material at Ukaku's level: camera-based gloss, rim and glint, a slightly clear skin seen edge-on, glowing veins with
  an RC pulse, a hotter root. Every colour follows the kagune colour (`KaguneTint`; black kagune glow blood red, as its wings do).
- **2.0.42** **Rinkaku Blocking** (right click held, tails out). Rules in `RinkakuBlock` (pure, tested), server `RinkakuBlockServer`, client
  `RinkakuBlockClient` (input with local prediction, tails pose from `StrikePaths.clearOwner`), body `RinkakuBlockPose`.
  - Covers 115 degrees either side of the look. Anything with a source is blocked; a fall, fire, hunger or the void is not.
  - The guard breaks at 6 weight (1 per light hit, up to 3 for a heavy one, a flurry counted once per 5 ticks): tails thrown back, a hard
    push, 5 s cooldown.
  - Perfect: a hit within 3 ticks of a fresh press, and only if the last window opened at least 16 ticks earlier. It cancels the push and
    staggers the attacker (`PaleStun`, 24 ticks, within 7 blocks).
  - Left click drops the block for the free hit. Instinct strikes don't fire while blocking. The first and final kakuja can't block (the
    final kakuja's right click stays its dash).
  - In first person the guard sits lower (`RinkakuBlock.OWN_DROP`) so it doesn't blind you.
  - New allowlist entry: `LivingAttackEvent.getAmount` ([forge]).
- **2.0.43** the White Reaper's own idle (`AgentPose.reaperIdle`): leaning well back, hip cocked, loose leg turned out in front, chest open,
  chin up, free hand in his pocket, a slow breath, no glancing.
- **2.0.44** fix: investigators are drawn on vanilla's player model, which **bends no elbows or knees**. 2.0.43's pocket arm was re-solved
  for a straight arm. Preview with `PoseRender.stiff = true` for any agent pose.
- **2.0.45** the White Reaper's build (`npc/ReaperBuild`, the `Build` layer in `AgentRenderer`): his upper torso drawn again over his own,
  broader and about 1 px out in front, tapering into his waist, in his own skin and its jacket layer. His arms are set 0.75 px out while he
  is drawn (`AgentModel.brawny`/`settle`; the model is shared with other investigators).

## Since 2.0.45

The owner asked:

> "whenever the player model picks male, I want you to give the same model as [the White Reaper] in terms of the muscle. And show me the new
> kagunes."
>
> "I want [the White Reaper] to be ... a little tech, a bit more of a tactician ... make his [lance] a little bigger. It's too small. And a
> little smoother ... It's [IXA] ... shoot, like ... 5 at once in front of him. Like, all emerging from the same point and then separating
> into, like, a circle ... backflip with the mutation thing that makes him backflip higher ... midair, start shooting lightning balls ...
> get close when he sees an opportunity. It should be the pinnacle of combat in this mod."

- **2.0.46** male players get the Reaper's build:
  - `BuildLayer`; arms set out in `KaguneBodyAnimation.get3DTransform`;
  - hidden under a chestplate and torso armour forms;
  - the Rinkaku mark lies on the broader back (`ReaperBuild.back`).
- The owner was sent `docs/handoff/kagune-showcase-2.0.46.png` (new `tools/KaguneShowcase.java`: the game's own ribbon mesh, markings,
  veins and mark on a body, at the real tail thickness of 0.18 + 3.5e-5 per mastery point, times appearance 0.6, times the per-tail
  scale).
- **2.0.47** Dawn bigger (hand scale 0.9) with a thicker 16-sided spike. `Quinque.DAWN_TIP` = 2.1 is where its shots leave.
- **2.0.48** the five-ball volley (`quinque/DawnVolley`: open into a flattened ring, close on the aimed point; `CrystalShots.volley`;
  FX `DAWN_RING`).
- **2.0.49** the tactician (`npc/ReaperTactics`, plus `AgentBrain.tactics`, `soar` and `release`):
  - **soar**: an Up We Go backflip, about 5 up and 5 back, then the volley from the apex;
  - **punish** openings;
  - **vault** over guards;
  - **sky shot** at targets in the air;
  - every shot leads its target.

Previews: `dawn-bigger-2.0.47.png`, `dawn-aim-2.0.47.png`, `dawn-volley-2.0.48.png`, `reaper-soar-2.0.49.png`, `male-build-2.0.46.png`.

- **2.0.50** Rinkaku tail scales (overlapping keeled), the leaf shape (the owner's drawing: thin at the root, broadest near the end), more veins.
- **2.0.51** Rinkaku tails fixed and alive, after the owner's in-game screenshots:
  - one skin with the scales baked in (the second layer broke up at a distance);
  - the designed rest pose at the full length (`RinkakuIdle`), replacing the original chain's coiled, twitching rest;
  - stretch, swell and breath (`RinkakuRibbon.stretch`, `swell`);
  - fine, tapering veins;
  - an animated preview, `rinkaku-alive-2.0.51.gif`.
- **Waiting on the owner's approval of the tails**, then (their words): "move and add way more dialog to normal humans, romance dialog for
  every personality type, different trades per NPC (if it isnt in place yet)". Look at `village/HumanPersona`, `HumanTalk`,
  `VillageTrade` first.

## Since 2.1.0

- **2.1.0** Kakuja legs and mask in the kagune's colours (`KakujaPalette`: plates always greys, flesh the kagune colour).
- **2.1.1** Final forms rest five minutes: the Exhaustion effect (`ExhaustionEffect`, `KingRules.RESTS`).
- **2.1.2** The owner: "adapt the Ken, Eto and Touka quests for romance better, so that it's more immersive and less repetitive, it's
  super boring at it stands." Each companion's own five-chapter courtship (`npc/Courtship`, `server/CourtshipServer`), each chapter closed
  by a `moment` scene in their talk script; eight `date` scenes each, from the sweet page, once a day. Begins at regard 60.
- **2.1.3** Every villager is someone: `village/HumanLife` (trade, family, pastime, fear, dream, habit, secret, favourite gift, view of ghouls;
  gendered names), `village/HumanVoice` (the 12 tempers' framing, the "Talk a while." topics, gifts, gossip), trades by trade
  (`VillageShop.stock(seed, job)`). Female village humans checked: the body follows `HumanRules.female(look)` (unchanged since 2.0.34).
- **2.1.4** Romance for every temper (`village/HumanRomance`): flirting at every warmth (a kind no if not their type), a partner's talk
  (what they love about you by their taste, your future with their dream, when you met, jealousy, danger, something sweet), more kisses,
  hugs and "I love you".
- **Next:** villagers coming up to the player with something to say (a `human.txt` scene script would fit: `SceneBook` already reads
  companions', children's and allies'), by temper and how close they are, including partners.
- **The owner's words for this work:** "As for humans this is where we gotta spend the most time, we gotta add wayyyyy more options
  and text to each personality and make it feel like every NPC is unique, double check that Female humans have the female body model."
  Also still open from before: "romance dialog for every personality type, different trades per NPC". Look at `village/HumanPersona`,
  `server/HumanTalk`, `village/VillageTalk`, `village/VillageShop`, `HumanRules.female`, the `HumanRenderer` Figure layer.

Ask the owner:
- how the courtships read in game (pace: 3-minute walk, 2-minute sits; whether regard 60 to begin is right);
- whether the Reaper is now too hard or too easy (soar every 5 s at most, punishes, sky shots every 2.3 s, 5 balls at 40% each);
- "Dawn drops" (still unanswered).

## How it was checked

`tools/quick_check.py` (compile, API verify, the touched suites) for every version; CI built and released each (2.0.41 to 2.0.44's
releases confirmed green; 2.0.45 was building when this was written). Previews, all off-game, in `docs/handoff/`:
- `rinkaku-material-2.0.41.png` and `rinkaku-material-zoom-2.0.41.png`;
- `rinkaku-block-2.0.42.png` (body pose; 1 to 4 tails; hit, perfect and break; first person);
- `reaper-idle-2.0.43.png` (re-rendered stiff in 2.0.44);
- `reaper-build-2.0.45.png`.

The tools: `tools/BlockPreview.java`, `tools/ReaperPreview.java <skin> <out> [build]`, `tools/RibbonPreview.java`. Compile them with
`javac -sourcepath src/main/java` plus `tools/PoseRender.java` and `tools/ArmSolve.java`.

## Open, to ask the owner

- **"Dawn drops"**: what they meant (cut-off message).
- The block's numbers are first guesses:
  - 3-tick perfect window;
  - 6 light hits to break;
  - 5 s cooldown;
  - 1.2 s stagger;
  - 115 degree cover;
  - Slowness I while the guard is up.
- Whether the Reaper's build reads as "a little" more muscular, or should be more or less.
- 2.0.27: do wild Rinkaku ghouls' tails show now? If not, ask for the log line "HexKagune's a wild ghoul's tails layer failed".

## House rules (unchanged; full text in `next-ai-1.3.60.md`)

- Nothing is tested in game; never claim it is. Look at your own previews honestly, zoom in, and match the owner's pictures.
- Banned names in the jar: Kaneki, Ayato, Furuta. Banned in visible text: Takatsuki, Aogiri, Yoshimura.
- Sounds only from the hexkagune: pack.
- Never alter the owner's skins or files (only generated copies).
- New Minecraft API needs an allowlist entry with [srg], [order] or [forge] evidence plus a stub. Stale entries fail the build. Never call
  a vanilla-private member directly (use `ObfuscationReflectionHelper`).
- Append only to ordinal-sent enums.
- **Version numbers (the owner's rule):** the last number never reaches 10: after x.y.9 comes x.(y+1).0 (2.1.9 is followed by 2.2.0).
  It was missed through 2.0.51, and published releases can't be renumbered, so it applies from 2.1.0 on.
- Each fix or feature is its own version:
  - both version strings, release notes, a README entry and a new top section in `companions-handoff.md`;
  - commit `2.0.x: ...` with `Co-Authored-By: Claude <noreply@anthropic.com>` and the session line, no model names;
  - then push.
