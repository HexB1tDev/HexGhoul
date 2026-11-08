> **Newest handoff: read `docs/handoff/next-ai-3.4.8.md` first** (then `BIKAKU_PATH.md`). Before it: **`docs/handoff/next-ai-3.3.5.md`** (the 3.2.7 to 3.3.5 session; the prompt for the next AI is `docs/handoff/next-prompt-3.3.5.md`; before it, `next-ai-3.2.1.md` (3.2.0 to 3.2.6) and `next-ai-3.1.9.md` (3.1.5 to 3.1.9)).

# HANDOFF: HexGhoul after 2.0.45 (updated through 3.5.1, see "3.1.5" to "3.5.1" at the end of "Since 2.1.0"; earlier through 2.1.2: see "Since 2.0.45" and "Since 2.1.0" below). Read this first, then `docs/handoff/next-ai-2.0.36.md` (2.0.27 to 2.0.36 and what to ask the owner),
`docs/handoff/next-ai-2.0.21.md`, `docs/handoff/next-ai-2.0.9.md`, `docs/handoff/next-ai-2.0.5.md` (how the owner works),
`docs/handoff/next-ai-1.3.60.md` (house rules), then the top sections of `docs/handoff/companions-handoff.md` (one per version, newest first).

Branch **`claude/peaceful-ramanujan-eldmlu`** (3.4.9 on; 3.4.8 was `claude/wizardly-hopper-h08wf2`; `claude/exciting-bell-3kklnc` 3.2.7 to 3.4.7; 3.2.0 to 3.2.6 were `claude/magical-fermi-cec8n1`; push only to the branch the session is given). Never push to `main`; no PR unless asked.
Version **3.5.1** in `build.py` and `patch/KagunePatch.java`; the next is **3.5.2** (the last number never reaches 10). Protocol **"95"** (`ControlNetwork.PROTOCOL`; 3.5.0 added C2SBikakuAction (message 55) and the move to S2CBikakuState; 3.4.9 added KagunePath BIKAKU (appended, S2CPathState sends its ordinal) and S2CBikakuState (message 54); 3.3.6 added EtoGesture JEALOUS (appended, S2CEto sends it by ordinal); 3.1.8 added C2SArmAction.THIRD (4) and Arm.Ability SWING/FAULT/GUARD; 3.1.6 added C2SUkakuAction.HURL (11) and S2CHurl (53) for the Rubble Hurl; 3.1.5 added C2SUkakuAction.IMPALE (10) and S2CImpale (52) for Rinkaku's Impale; 3.1.2 added C2SArmAction.FOLD (3) for the dedicated briefcase key; 3.0.6 bumped it for C2SArmAction (50) and S2CArmSwing (51); 3.0.0 bumped it for CcgAgent ARM, ARM_AT, ARM_TARGET and the ArmRules SWING codes; 2.5.0 bumped it for 2.4.8's S2CUkakuFx CRACK and 2.4.9's new EtoGesture ordinals; 2.4.5 added S2CThyDeath and C2SUkakuAction THY_DEATH; 2.3.0 added the mother's size to `S2CChild`; 2.2.4 added the kiss's mood to `S2CEto`; 2.1.9 added the synced AFRAID on CcgAgent and GhoulNpc; 2.1.7 added `C2SUkakuAction.REAPER_DASH`; 2.0.42
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
- **Next (done in 2.2.6):** villagers coming up to the player with something to say (a `human.txt` scene script would fit: `SceneBook` already reads
  companions', children's and allies'), by temper and how close they are, including partners.
- **The owner's words for this work:** "As for humans this is where we gotta spend the most time, we gotta add wayyyyy more options
  and text to each personality and make it feel like every NPC is unique, double check that Female humans have the female body model."
  Also still open from before: "romance dialog for every personality type, different trades per NPC". Look at `village/HumanPersona`,
  `server/HumanTalk`, `village/VillageTalk`, `village/VillageShop`, `HumanRules.female`, the `HumanRenderer` Figure layer.

## Since 2.1.5 (each its own version; details at the top of `companions-handoff.md`)

- **2.1.5** courtships out of the mod's own world (the owner: "why are the quests so boring"): Doves by rank, Pack ghouls, a ghoul preying on
  a village, a ghoul ally, village humans who care or confide, a night walk through a village, a cookbook drink, ghoul meat.
- **2.1.6** fix: Rinkaku tails swung across the back easing into rest: `RinkakuRoot.side` had the original's fanSide backwards (the original
  lays a tail along its owner's LEFT times fanSide). Don't touch the kagune model or rest position (the owner likes them).
- **2.1.7** the White Reaper's dashes for Ukaku's final kakuja (sprint double-tapped, 3 charges, 1 back each 5 s); no kakuja vanishes any
  more; the release workflow keeps only the 5 newest releases and runs (cleanup job).
- **2.1.8** Ken wants proof before a final kakuja (`Teaching.Proof`).
- **2.1.9** Peace and War: Doves Rank 0-4 and weaker wild ghouls run; Rank 5-7 and the strongest shake and keep backing off.
- **2.2.0** tolerance and "I'm sorry." (`Tolerance`).
- **2.2.1** half-human children (`HalfBlood`): 70% lost; the rest one-eyed, stronger, Reaper dashes. **2.2.2** grown, their kakuja.
- **2.2.3** a partner's mood shapes what they do (`MoodActs`), and they come to make up. The owner called this "the last thing for the mod".
- **2.2.4** a kiss for every mood (`MoodKiss`, `MoodKissPose`): angry, possessive, giddy, tender, desperate, sleepy, hungry, teasing, sweet. Kisses are never refused now.
- **2.2.5** a male player's chest a little smaller (`ReaperBuild.PLAYER`, 60% as far out as the Reaper's); the White Reaper unchanged.
- **2.2.6** village humans come up to the player by themselves (`HumanApproach`, `HumanApproachServer`): hello, news, gossip, a gift of their
  trade, worry when hurt; partners also missed you, a kiss, a hug, a walk. This was the "Next" left open since 2.1.4 (below): done.
- **2.2.7** `/kagune <eto|touka|ken> marry [player]`: married at once, everything full (`Courtship.wed`).
- **2.2.8** `/kagune quests complete all` opens every gate (the taught ones too: the Dragon, the kakujas) and gives a Rinkaku player the crown.
- **2.2.9** every investigator a ghoul kills gives mastery by rank; an uncommon field ration (cookbook food); Dawn 1 in 5 (was always).
  The owner answered "Dawn drops": yes, but rare.
- **2.3.0** grown daughters are their mother's height, give or take 4% (`ChildRules.daughter`); the owner answered the old height question.
- **2.3.2 to 2.4.0** the owner's bug list after testing (their words: "do not dare say anything until everything I listed ... is fixed"):
  Rinkaku strikes round the wrong side (2.3.2, `RinkakuRoot.clearance`: BodyClearance's +1 is the owner's LEFT); the male chest kept in
  every form, armour widened over it (2.3.3); human partners' kiss as the companions', no fear once confessed, a romance page and a
  ring (2.3.4); mood kisses per companion, arms never closed in front of the face (2.3.5, `MoodKissPose.clear`); endgame ghouls tougher,
  health up to 40 (2.3.6); bubbles steady in bed, children's kagune shown, both parents' halves (2.3.7); "Go home." always (2.3.8); an
  unwanted kiss let go (2.3.9); the sweep (2.4.0).
- **2.4.1 to 2.4.7** the owner's next list: partners far less clingy, no false "wants to talk" (2.4.1); Not That Easily makes the mod's own
  fighters forget you too (2.4.2); NPCs swim like players (2.4.3); War/Peace reactions (2.4.4); Rinkaku's Rapid Strike is Thy Death,
  every tail hunting on its own while Z is held (2.4.5, protocol 85); Ukaku's Vortex a slow rolling loop of black-and-pink mass per the
  owner's reference gif (2.4.6, `WingVortex.Coil`); villagers' favours (2.4.7, `HumanErrand`).
- **2.4.8** Ken's finger crack one-armed (`EtoPose.crack`) with a real bone crack (`sounds/sfx/bone/`, CC BY 4.0, credited there;
  `tools/sfx_pack.py` writes sounds.json, so add sounds there); a player with a kakuja learned brings the kagune out his way
  (`KenPullOut`); the original's regeneration hearts silenced in the patch (`GhoulRegen.particles`).
- **2.4.9** spouse cooking: a furnace or smoker needed (`Kitchen.oven`), stood at its front (`Chores.front`, `HomeChores.front`), the
  routine `Chores.COOKING` (new gestures STOKE, SEASON, TASTE, SERVE appended), the food carried (`HexCarry`) to the chest named "Food"
  (`ChestSorter.food`), the meal handed over when the player is home (`HomeChores.serve`), a kiss asked by mood (`Kitchen.kissAfterMeal`).
- **2.5.0** final sweep: every server map of game times is made by `WorldTimes.map()` (state under way by `WorldTimes.kept()`) and
  cleared on `ServerStoppedEvent`, so a single player's next world never inherits cooldowns or "until"s (`ControlTests` enforces it: make
  new ones that way); favours kept under `PlayerPersisted` (survive death); protocol 86.
- **2.5.1** the finger crack eased up and down (`EtoPose.crackWeight`; players blended with `crackOver`), cracking at
  `EtoGesture.CRACK_SNAP` with its sound; a cleaner `finger_crack.ogg`.
- **2.5.2** no hearts as the kagune/kakuja comes out: the original's fallback blood/mist/landing drew DAMAGE_INDICATOR (patched to
  `UkakuBlood.FALLBACK` by `KagunePatch.noHearts`); Rinkaku's manifest blood is `UkakuBlood.spurt`; partners no longer puff hearts at it.
- **2.5.3** the owner's own finger pop as `finger_crack.ogg` (mono, trimmed to the pop, tags stripped); the arm stretches out straight,
  then bends (`EtoPose.crackBend`), then straightens and lowers.
- **2.5.4** children (child/teen/grown) fight a monster close and in sight, taught or not (`ChildRules.fights`, `ChildRules.spots`);
  a ghoul child's kakugan shows all night (`ChildClient.eyesOut`).
- **2.5.5** a child's kagune drawn whenever it is out (`ChildRenderer.drawn` also takes every child within 48 blocks with it out, as
  `GhoulNpcRenderer.drawn` since 2.0.27); each child guarded in `GraftRenderer`/`UkakuWingRenderer`.
- **2.5.6** lag: a child at peace looks for threats every 5 ticks, sight cast last (`ChildBrain.THREAT_EVERY`); `ChildRenderer.drawn` and
  `GhoulNpcRenderer.drawn` cached once a frame. Keep new per-tick scans throttled and per-frame lists shared.
- **2.5.7** the baby ask is on the sweet page (`EtoTalk.babyOption`/`babyAnswer`; "another" once they have a child), 5 MC days after
  the last birth (`FamilyView.BABY_WAIT`, `Children.lastBorn`); at a shared home she comes to them for the night asked for
  (`EtoBrain.comesForBabyNight`, from `toBed`): before, she only waited in bed, so after the first child it never came.
- **3.0.0** (experimental) the CCG rework: kagune-made weapons for Rank 4+ (`quinque/Arm`, `npc/ArmRules`: rolled once by rank,
  saved under `HexCcgAgentArm`; Rank 8-9 masterworks; never the Reaper). Server: `server/AgentArms` (draw/sheathe, the M1 strings,
  the eight moves, bleeding) from `AgentBrain.fight`; `AgentBrain.ranged/fighting/reach` keep a spear-carrier (LAUNCHER case) melee.
  Client: `client/animation/WeaponPose` (keys in the chest frame, weapon direction exact: `point` puts the arm on its cone; keys are
  lean-free; the weapon placed along its base quinque's item line, so `QuinqueGrip.reach` grips it), `AgentClient.armed`,
  `client/render/WeaponLayer` (meshes from `quinque/ArmMesh`, the whip via `RinkakuTube` along `quinque/WhipPath`). Previews:
  `tools/ArmPreview.java`, `tools/WeaponPosePreview.java` (`docs/handoff/*-3.0.0.png`). `/kagune investigator <weapon> [rank]`.
  NOTE: `client/animation/ArmPose` is Pale's arm (not these weapons).
- **3.0.1** after the owner's anger at 3.0.0 ("How does animations like this pass?"; the sword "looks like a piece of paper"): the
  weapon keys rebuilt (`WeaponPose.K`: weapon/arm ways relative to the foe, twist, lean capped 0.26, side, spin, stride with the back
  foot planted (lunge = 12 sin stride, drop from the legs only), `bk()` weight back, the free arm; 7 stages a blow), `AgentClient.flow`
  blends one blow into the next, `WhipPath.arc` passes through the foe at the landing. New sword (`ArmMesh.Builder.faceted`), spear,
  hammer armour. JUDGE POSES AT FULL SIZE FROM SEVERAL SIDES (`tools/WeaponPosePreview.java` strips; a big single frame tells more),
  as motion, not only "does it point the right way".
- **3.0.2** case drops by commonness (`AgentRules.caseChance(kind, arm, masterwork, looting)`): Blade 10, Katana 9.5, Spear 9, Mace 8,
  Launcher 7; whip/crystal spear 5, hammer/greatsword 3, masterwork 2 (%); Looting +25%/level; Dawn 20% (`dawnChance`). A kagune-weapon
  carrier drops its kagune's ordinary case (the weapons are not items).
- **3.0.3** the crack's start: `KenPullOutClient.apply` moved after `settle(fade)` in `KaguneBodyAnimation.finish` (the layer's fade-in
  from 0 was scaling it away), `crackWeight` rises eased-out (sin), CRACK_RISE 10, BEND 7, SNAP 17, DOWN 29, END 47; `sfx_pack.py`
  PRELOAD (finger_crack, kagune_burst, gush).
- **3.0.4** companions' slot: three libraries a world, one Eto; an unmet one (`UniqueNpc.met`, kept in the slot line's 13th field) is
  taken over by the body a player comes to (`EtoBrain.tick` first branch: dormant until `someoneAbout`, then `UniqueNpcs.adopt`).
- **3.0.5** Rinkaku HOLD/LIFT/CRUSH: tails not holding (`view.heldEntityId(limb) == 0`) take the idle rest (RinkakuIdleClient) and idle
  smoothing; the holder HOLD_RESPONSE 2 / HOLD_WIGGLE 0.35 (`StrikePaths.clearOwner`).
- **3.0.6** The four Arms are player items (`quinque_case_<key>` / `quinque_<key>`, `ControlItems.armCaseOf/armWeaponOf`, masterwork tag): opened by right click on the case (`PlayerArms.open`), folded back when put in another slot (`PlayerArms.fold`, needs `Player.getInventory` stub); M1 = string (`C2SArmAction.STRIKE`), right click / Shift + right click = moves; `S2CArmSwing` drives `WeaponClient.pose` and `ArmItemLayer` (third and first person). Investigators carry and drop the arm's own case. Untested in game; the first-person placement is hand-tuned and may need adjusting.
- **3.1.5** Rinkaku's **Impale** on H when no tail holds anyone (the owner's request; details at the top of `companions-handoff.md`). The owner also
  asked for **an idea for the Throw key (N) when nobody is held** ("I'm out of ideas"). Proposed in chat, waiting on their answer, not built:
  **Rubble Hurl** — N always throws something: a held body, or, with none, the ground itself. The tails dig into the block he looks at
  (or the ground before him), tear a chunk out and hurl it along his look; it bursts where it lands (damage, knockback, a little stun, real
  debris). Held N tears a bigger chunk (slower, heavier); the final kakuja tears out several. It gives Rinkaku a ranged hit that isn't a
  tail and fits the key's name. (Also offered: **Reel In**, every tail lashing out in a cone and dragging what it catches to his feet.)
  If they say yes, put it in `UkakuGates.throwHeld` the way 3.1.5 put the Impale behind `crush` (`ImpaleServer.crushes`); mind block
  griefing (a game rule or config to leave the world untouched).
- **3.1.6** The owner answered "Do that one": **Rubble Hurl built** on N with nobody held (details at the top of `companions-handoff.md`).
  Read as the main pitch; if they meant Reel In, it is still open. The torn block is taken out by `BlockSmash` (permission-checked,
  restored after the config's time, untouched when block smashing is off).
- **3.1.7** fix: the Rubble Hurl's gripping tails went through his body (the owner's report on the 3.1.6 preview); now routed round his
  shoulders (`RinkakuHurl.grip`), with 4,400 regression checks. **Next, in order:** the quinque (briefcase) movesets, more family
  conversations by personality (bringing a child home), then jealousy, co-wives and half-siblings (see the top of `companions-handoff.md`).
- **3.1.8** "the briefcase" meant the heavy blade, hammer and whip: each got a signature move on Sneak + M1 (Tendril Swing, Fault Line,
  Bone Wall). Moves for the four first melee quinques were built first by mistake and withheld (they undo the 1.3.33 right-click fold).
- **3.1.9** jealousy seen and said, partners meeting each other, half-siblings talking (`family/Blended`, `BlendedServer`, `cowife`/`halfsib`
  scenes). **Next:** more "bringing the child home" conversations by personality.
- **3.2.0** the word at the door when a companion parent brings a child home: a back and forth that knows why it went after the child
  (`Parenting.home`, `ParentingLines` door/doorreply/answer/last, `ParentDuty`, `TalkCues` HOME). **Next:** the player bringing a child home (3.2.1).
- **3.2.1** the player bringing a child home: its waiting companion parent meets them at the door (`brought`, `found`, `carrying` scenes) while
  the child calls out (`Homecoming.hello`); else the child's own `back` scene (`HomecomingServer`, `ChildBrain` home check, `SceneServer`).
- **3.2.2** one at a time: nobody starts something with a player someone else has (or let go of under 4 s ago); `npc/Turns`, `Engaged`,
  gates in `EtoBrain`, `ChildBrain`, `HumanApproachServer`, `SceneServer.free`.
- **3.2.3** fix (owner's report): companions frozen in the kiss pose: a held gesture (a reaction blush) with no kiss or hand behind it lets
  go after 4 s (`EtoGesture.stranded`), a reaction blush drawn shy (`EtoPose.shown`), villagers' held poses end with the kiss. Break-up /
  divorce work parked in `git stash`.
- **3.2.4** fix (owner's report): a maxed ghoul thrown back in clashes: both sides on one scale (`ClashPower`, `KaguneClash.strength`): a
  player's power from Rank 0's to the Reaper's by mastery, gates opened and ghoul meat.
- **3.2.5** fix (owner's report): clash sounds at full gain (`tools/sfx_pack.py`; they were capped at 3.5-8%).
- **3.2.6** fix (owner's report): crystals sounded like punches: crystal pools rebuilt from raised steel/needle sounds, no punch or bone
  (`sfx_pack.py` per-file pitch, new `crystal_hit`). No real glass in the pack: ask the owner for files.
- **3.2.7** fix (owner's report): Peace's Aegis shields were knight's heater shields hanging off their tendrils, the top one over his head:
  now fans of crimson flesh on seven bone ribs grown on a stalk at each tendril's end, the top one a hood behind his head
  (`PeaceFxModel.shield/edge/stalk/shieldRest`; KingTests `aegisGrown`). Peace's keys: the owner says done.
- **3.2.8** fix (owner's report): War's Spire Wave (B) janky in first person: the left arm drawn in the hand pass (`SpireArm`,
  `KingFirstPerson.spireArm`, `PlayerRenderer.m_117813_`), coils round it, the beam a cone out of the fist (`WarHoldModel.coils/mass`
  thick/near); third person unchanged.
- **3.2.9** fix (owner's report): the Wing Vortex: a thin round corkscrew (3 round strands, a jet) straight along its flight (`WingVortex`
  rebuilt, `Coil` gone), flown straight forward (`UkakuRules.vortexHeading`; it used to dive at a body's middle into the ground and end
  there), first person a see-through jet, no burst or smoke at your eyes.
- **3.3.0** the move guide (owner's list): Rinkaku's rows follow whether a tail holds someone (`AbilityGuide.Input.holding`, `AbilityHud`
  from `view.hasHeldEntities()`): Grab / Hurl (N held) / Impale (H held) / Lift · Slam greyed, or Throw / Crush / Lift / Slam them / Grab
  another.
- **3.3.1** Ken teaches the Rubble Hurl and the Impale (owner's list): new gates `RINKAKU_HURL`/`RINKAKU_IMPALE` (appended, never
  grandfathered), Ken's lessons appended (old bits kept) and taught by mastery (`Teaching.inOrder`), proofs: 5 throws / 5 crushes
  (`Deed.THROWS/CRUSHES`, counted in `UkakuGates`).
- **3.3.2** Dawn's idle (owner's list; they picked only the idle): `QuinquePose.idle` (breathing, a turn in the fingers, carried aside at
  ease with the left hand on the hip) after 1.5 s still (`QuinqueClient.stillFor`). Hold and M1s unchanged.
- **3.3.3** the Kakuja V2 Dragon's voice (owner: it was silent; "Get the sounds from the web, don't generate them yourself"): 14 real CC0
  freesound recordings via the WilhelmSFX GitHub mirror (`sfx_recordings.json`, `sfx_pack.py --import-recordings --only`), as recorded,
  roars and crawl on `DragonTimeline.roars/crawls`, crashes, bites, a death cry in `DragonSession`.
- **3.3.4** one partner, for good (the owner, replacing break-up/divorce): nobody new while you have a partner anywhere (`Vows.partnerElsewhere`),
  no ring while married anywhere, killing Eto/Touka/Ken no longer ends it (`Romance.endsWithDeath`), a villager partner's death frees you
  (`Vows.died`).
- **3.3.5** crystal sounds from real glass (the owner: get sounds from the web): 3.2.6's pitched steel replaced by CC0 glass breaks and
  clinks (`sfx_recordings.json`, `crystal_*`), as recorded.
- **3.3.6** a jealous animation (the owner's 3.2.2 list): `EtoGesture.JEALOUS` (appended; protocol 93), turned back, glare, huff, snub;
  triggered by asking out or confessing to someone else while spoken for (`RomanceServer.tempted`, `Blended.tempted` lines), replayed
  while jealous (`BlendedServer.sulks`).
- **3.3.7** reconciliation talks (the owner's 3.2.2 list): "Can we talk about it?" for a cross companion and "Can we talk about earlier?"
  for a jealous partner (`mend_*` topics, effect word `mend`, `SceneServer.mend`), going well or badly by the answer; a second way each
  comes to make up (`make_up_2`, `jealous_over_2`, `SceneBook.variantOf`).
- **3.3.8** temper-aware door scenes (the owner's 3.2.2 list): `Homecoming.flags` gives the brought child's temper (`<t>kid`) and age
  (`babykid`/`littlekid`/`bigkid`) to the companion's scenes; 19 `door_*` scenes each (by temper x found/outing, by age).
- **3.3.9** homecomings without a home (the owner's 3.2.2 list): with no family home the place a child lives is home (`Homecoming.NO_HOUSE`
  16); house-and-door homecoming scenes wait on `home`; new `!home` scenes (4 per companion, 7 for children). **Next:** siblings reacting
  to the one brought back, then the rest of the list.
- **3.4.0** siblings react to the one brought back (the owner's 3.2.2 list): `family/SiblingWelcome` (cover, where WERE you, tattle, left
  behind, take me next time; answers by temper), `HomecomingServer.siblings`.
- **3.4.1** companion-child door talk (the owner's 3.2.2 list): after the player's door card, the companion and the child brought home talk
  in four turns (`family/DoorTalk`, `HomecomingServer.doorTalk`, a `Conversation`).
- **3.4.2** kids greeting you after days away (the owner's 3.2.2 list, its last): `family/KidGreeting`, the last sight kept in the player's
  NpcMemory (`kid_<id>`) so it outlasts an unloaded child; by temper and age, more after a week.
- **3.4.3** fix (the owner's report: investigators, even the Reaper, flung a maxed ghoul in clashes): `ClashRules.thrown/lifted` by the gap
  alone (the stronger stands at 2x, flung at `GAP` 2.0, an even clash a short push), `ClashPower.ONE_EYED`.
- **3.4.4** fix (the owner: the crystal forming sounds were bad): `crystal_form` is now a rubbed wine glass singing (three swells, ani_music)
  and a small bell (AlaskaRobotics), CC0 freesound recordings from Sonic Pi's samples on GitHub.
- **3.4.5** fix (the owner: the Spire Wave came loose from the hand in first person when moving): the mass leaves `SpireArm.fist` (from the
  eyes and look, every frame) instead of the launch spot.
- **3.4.6** the move guide redone (the owner: "too big ... summarize"): `AbilityGuide.brief` (a word or two a move), two columns past five,
  one-line header, scale 0.7.
- **3.4.7** fix (the owner: 3.4.6's two columns made the move guide wider; "it should be vertical"): one column on every board, scale 0.6,
  tighter spacing. **Next:** the Owl rebuild (the owner hated the in-progress renders: ask what's wrong first).
- **3.4.8** fix (the owner: the heavy sword's moves "ridiculously stupidly broken"; the hammer and whip "are good"): the Bone Greatsword held two-handed: the weapon turned in the fist (`RigPose.weapon*`, `WeaponPose.hold`), both hands on the grip every frame, its six blows and three moves rekeyed. Hammer, whip, spear unchanged. `GreatswordTests`.
- **3.4.9** the Bikaku path in the game (the owner's spec, `docs/BIKAKU_PATH.md`): a third choosable path; the worm (`MawWorm`/`MawMotion`) drawn by `BikakuRenderer` in the owner's kagune colour, alive at rest, long (thrown out it uncoils only as far as a bite needs); M1 a string of three bites (`BikakuRules`, `BikakuServer`, `S2CBikakuState`), Predatory Feeding; card, menu, move guide. Protocol 94. `BikakuTests`, `BikakuWormTests`.
- **3.5.0** Bikaku's first moves, on the original's kagune keys (`BikakuInput`, `C2SBikakuAction`), opened by mastery alone (the owner: a custom NPC will teach them later): Devouring Maw (G) and Predatory Drag (J, 150): grabs held at the worm's mouth by the server (`BikakuMoves`, `MawMotion.mawHeld/dragHeld`). Protocol 95.
- **3.5.1** Bikaku: Ravenous Pursuit (Z, 300: three thinner worms, each after a body on its own side, snaking lunges) and Carrion Eruption (B, 450: tunnels under the ground with the ground heaving over it, bursts up under its mark and throws it up; `client/fx/BikakuFx`).
- **2.3.1** the mood kisses rendered for the owner (`docs/handoff/*-mood-kisses-2.3.1.png`, `mood-kisses-waiting-2.3.1.gif`); possessive, giddy
  and tender reworked to read apart. Render poses with `tools/CompanionPreview.java <out> kisses` (stiff limbs, as in game).

Ask the owner:
- how the courtships read in game (pace: 3-minute walk, 2-minute sits; whether regard 60 to begin is right);
- whether the Reaper is now too hard or too easy (soar every 5 s at most, punishes, sky shots every 2.3 s, 5 balls at 40% each);
- ("Dawn drops" answered: yes, rare. Done in 2.2.9.)

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
