# HANDOFF: HexGhoul 4.0.0 "The Great Exploration" (released). READ THIS FIRST.

Branch: `claude/tender-brahmagupta-yhr1ga` (started from 3.7.3, `origin/claude/hopeful-noether-5c2wex` 9c5dcc8). Push only there.

The owner's whole request was the 4.0.0 spec they pasted (new villages, Aogiri hideouts, armour and masks, ranked wild ghouls, Ayato
Kirishima, Noro), as ONE release: **4.0.0, The Great Exploration**. Their files are in `art/exploration/` (the three schematics, Ayato's
and Noro's skins) and `art/outfits/` (the two outfits and 24 masks, canonical geometry; never edit them: the fitting is in the baker).
Release notes: `.github/release-notes-4.0.0.md`. Protocol 105. Not tested in game.

## Where things are

1. Structures: `tools/exploration_structures.py` -> `data/hexkagune/structures/{village_1,village_2,aogiri_hideout}/t_i_j.nbt`,
   `village/SiteMaps.java`; placed by `ControlStructures.Site` (types `hexkagune:site_<id>`, beard_box); old village's structure_set removed.
2. Outfits and masks: `tools/outfit_models.py` (bakes `models/outfits.bin` v2: each cube carries its texture net, so fitted cubes keep
   their texture; `fit_mask`: half masks squashed below the eyes, full masks' eye bands moved to y -3.5, 5% girth; `fit_outfit`: x/z
   drawn in past 0.3 to 45%, Formal Cap out past the hat layer, slim-arm sleeves as bones 6 and 7; the generic masks drawn on a
   quarter-pixel grid: `Face`, `clown()` ... `kabuki()`). Before shipping a change, render the baked cubes (with `faces()`) front, side
   and three-quarter on a Steve-like head and body and look at them (4.0.0 did this with a scratch script, not kept in the repo).
   `outfit/{OutfitModels,Outfits,OutfitItem}`,
   `client/render/{OutfitLayer,OutfitFirstPerson}` (`OutfitLayer.slim(model)` registers an NPC renderer's slim-armed model; players by
   their skin), `OutfitTests`.
3. Investigators in the Formal Outfit (`server/OutfitServer`, `AgentBrain`), Reaper excepted; per-piece drops.
4. Wild ghouls ranked D..SS (`NpcRules`, `NpcIdentity` packs rank/mask/faction), nine generic masks (`Outfits.GENERIC_MASKS`: append
   only, a saved ghoul's mask is its index).
5. Aogiri members (`GhoulNpcBrain`: `HexAogiri`, `aogiriFoe`, comrades' call, hideout leash; mask via `OutfitLayer.faction`).
6. Ayato (Persona AYATO, `AyatoNpc`, `AyatoScript`, `talk/ayato.txt`, Courtship chapters, `NpcWings` kakuja, Ukaku first-kakuja trial
   with cutscene `S2CCutscene.AYATO_KAKUJA`).
7. Noro (Persona NORO, `NoroScript`, Bikaku gates in `BikakuServer`/`quest/Gate`, `NpcWorms` fight, drawn by `BikakuRenderer.drawNpc`).
8. The owner's to-do list: Bikaku regeneration free (`BikakuPotential`, `BikakuMove.regenerates()`, `GhoulRegen`); The Walker
   (`WalkerLegs.shape/carried/raised`, `MawWorm.legRadius`); children (`KaguneGenes.WORM/WORM_TWIN`, `ONE_KIND` a third,
   `Children.both` no longer adds halves, `ChildWorm` fight, `BikakuRenderer` draws a child's worm).

Previews (renders of the baked geometry outside the game, a Steve-like mannequin, the skin's eye row marked red at the sides):
`docs/handoff/generic-masks-4.0.0.png`, `owner-masks-fitted-4.0.0.png`, `outfits-fitted-4.0.0.png` (wide and slim arms),
`walker-legs-4.0.0.png` (leg paths, front and side).

## 4.0.1 (released)

The owner, after 4.0.0, on the outfits: "They seem a little too thick. Can you add more details to it as well to make it as polished and
like not so thick in general?"; "you could add like a color or something to the formal outfit". On The Walker: "the walking is way too
weird, bro. It's too robotic, too slow. Like the steps should be like they should vary in length ... it still feels like it's two sticks
walking"; then "See, there's the issue. One leg at a time, not two in front at once. Come on, bro ... They don't go back like further back
enough or forward enough. This feels very unrealistic, and it's always you can always tell where the next step is gonna be, which is what I
don't want." Done in `tools/outfit_models.py` (`layers`/`through`/`snugger`, `details`, `RECOLOUR`, `finish`) and
`bikaku/WalkerLegs.java` (`update`/`step`: a true alternating stride by chance; `wobble`, `body`, the S `shape`); `WalkerPose` takes the
body's roll and dip from `BikakuRenderer.walkerBody`.

## 4.0.2 (released)

The owner (quoted exactly): "Bug, the armor does not allow for animation, make it compatible with animations"; "the jacket also breaks
when walking"; with `art/exploration/aogiri_mansion.schem` (their upload `Ougiri.schem`): "replace the Ougiri hideout with another
schematic ... I give them a mansion because I just thought the Alligator Tree deserves it. Make sure that the guards currently patrol the
place very often. There's at least two guarding the entrances, always in the same spot, always returning there no matter what. And first
time when you arrive, they should give you a little message with multiple choices. This only appears once, and only one of them will ask
you this once ... If he doesn't like it, they'll attack you. And they won't let you in, and you have to wait let's say 5 MC days. Make
sure to put Noro and Ayato in different rooms that are comfortable"; "While gated tree only has one hideout, there's multiple villages that
can spawn, although they're as rare as a normal village"; "give Ayato like animations of like ambient animations of like being cocky ...
The crack fingers animation is only Kaneki's in the players whenever he trains with Kaneki ... Noro simply like tilts his head and looks at
the player weird ... when you train with him ... you tilt your head before the Kagune pops out"; "Whenever the mention update is released,
just publish a release with it"; "onto the to-do list that we should have our own category in the creative menu with like the armors and
quinques and whatever, everything separated and well organized" (done: `ControlItems.TABS`, four tabs; their API allowlisted and
stubbed; the old vanilla-tab hook removed).

Where: `OutfitLayer` (`Bend`, `bend`, `tails`), `BodyAnimations.bend`, `tools/outfit_models.py` (`split`, `TAIL_PARTS`, bones 8 and 9);
`tools/exploration_structures.py` (`cut_x`, `mansion`, `site_loop`, the 1.21 swaps), `ControlStructures.Site.unique`,
`structure_set/{aogiri_hideout,villages}.json`; `server/AogiriGuard`, `npc/AogiriGate`, `GhoulNpcTalk.gate`; `EtoGesture`
POSE/SLICK/SHRUG/TILT, `EtoPose.tilt/tiltOver`, `server/NoroPullOut`, `client/NoroPullOutClient`.

## 4.0.3 (released)

The owner, of the 4.0.2 renders (quoted exactly): "Do I need to say anything about both outfit's legs and arms?" Then: "On the Ougiri
outfit, no more hood, make it less bulky around the shoulders and head to be similar to the formal outfit, and, give it a high collar on
the sides of the head pointing outwards."

Where: `outfit/CoatDrape` (pure, tested: the tails' fronts with the leg furthest forward, backs with the one furthest back, folded at the
knee on the leg's middle as bendy-lib bends the trousers, pushed clear of every point of both legs' outlines); `OutfitLayer.drape` (the
lag: `TAIL_FALL`, `HUNG`; the knee's bend from `BodyAnimations.bend`) and `tails` (only the leg's z and y turns now); `tools/outfit_models.py`
(`SLEEVE_IN`: a sleeve's inner side over the arm; `split` gives each half its `cut` of the net and cuts turned cubes and the tails at the
knee; `collar` and the hood left out of the Crimson; outfits file version 3: one more float per cube, where its sides start in the net);
`OutfitModels.read` (versions 1 to 3). The Crimson Hood item is removed (`Outfits.set(style, head)`; lang, icon, item model, the
hideout's loot). Previews: `docs/handoff/{formal,crimson}-outfit-4.0.3.png`.

## 4.0.4 (released)

The owner (quoted exactly), with `Tokyo_Ghoul_24_Masks_1.20.1.zip`: "New masks, replace ALL the masks with these models."

Their pack is `art/outfits/masks/` (Blockbench projects, 256 x 256 textures, Java ModelPart classes, manifest, README, preview sheets; its
OBJ and GeckoLib copies left out: the same geometry). `tools/outfit_models.py` `load_mask` reads each `blockbench/<id>.bbmodel` as it is
(y flipped from Blockbench's floor to the game's neck, x and z kept: their Java export matches cube for cube, buckles on the wearer's
left as their renders show); the old fitting (`fit_mask`, `HALF_MASKS`, `MASK_EYES`, `MASK_GIRTH`) is gone, their masks being made to fit
(eye holes at their reference skin's low eyes, x +-2.12, 2.15 above the chin). `MASKS` maps their ids to the same items; only "Gourmet's
Half Mask" became "Gourmet's Mask". OutfitTests: every cube their manifest counts, half masks clear of their eye zones, no mask in the head
or its hat layer. Preview: `docs/handoff/masks-4.0.4.png`.

## 4.0.5 (released)

The owner (quoted exactly): "also game crash upon creating world" (their crash report: `RegistryDataLoader` "Failed to load registries"):
`worldgen/structure_set/aogiri_hideout.json`'s `concentric_rings` placement (4.0.2) had no `salt`, which the game's codec requires (only
`frequency`, `locate_offset`, `frequency_reduction_method`, `exclusion_zone` are optional). Fixed; `NpcTests.mansion` now checks every
structure set's required fields, structures and biome tags. The CCG building they said they sent ("this is it") did not arrive.

The owner (quoted exactly): "Official NPCS disappear from their residential areas and dont come back they should always stay there unless
married to a player and set home in another place." Cause: `UniqueNpc.claim` let another body of hers (made with another of her
buildings) take her slot once hers had gone unseen `GONE_QUIET` (five days); hers was sent away when it loaded. Now a met slot is never
taken while she lives; `UniqueNpc.missing` + `UniqueNpcs.levelTick` (`WATCHED`) make her again at `spawn()` when a player has been within
24 blocks of it half a minute and her body has not ticked; `report` keeps the record's origin up with her body's (a married one's shared
home). Their other three reports this round (limbs taking their layers, full masks hiding kakugan, Ayato's skin, Ken's tower, fighting
styles, the CCG building and General, Kick Master) follow as 4.0.6 onward.

## 4.0.6 (released)

The owner (quoted exactly): "Make sure that when limbs are removed, over layer of the skin also goes with the head and etc, coats and
armors go with the limbs as well. (same with masks)". `OutfitLayer` skips a bone whose part is not visible (tails follow the body);
`PotentialRenderer.worn` draws the worn outfit cubes and the `EXTRA` mask on each torn part (`Reattach.point`/`way`: part pixels into the
part's frame); `PotentialClient` sets `ModelPart.skipDraw` (f_233556_, allowlisted) on vanilla armour models' parts while a part is off (the
armour layer's models found by type through reflection); `KakujaArmorLayer` and `RinkakuMaskLayer` skip a hidden head.

## 4.0.7 (released)

The owner (quoted exactly): "Full masks (only full masks not half masks) prevent CCG agents and humans to tell you're a ghoul when your
eyes are in Kakugan." `Outfits.HALF_MASKS`/`fullMask`; `Sightings.masked` (head slot a full mask) takes the eyes out of what a player shows
(`Sightings.of`) and out of `AgentBrain.reveal` and `HumanBrain`'s danger check; a kagune, a kakuja and the final sights still show.

## 4.0.8 (released)

The owner (quoted exactly): "I sent you the new ayato skin, make him a little muscular but way less than the male player." Their skin is
`art/exploration/Ayato_skin_2.png` (copied to `art/ayato_skin.png`, the bake's source; `tools/companion_textures.py` ayato: wide arms, eyes
rows 5-6). `Persona.muscle()` (0 but Ayato's `ReaperBuild.AYATO_SHARE` 0.3); `EtoRenderer.Build` draws `ReaperBuild.chest` of it;
`EtoModel` sets the arms out by `ReaperBuild.armOut` (put back each frame: `settle`). Next on their list: Ayato's god-like pull-out
(both hands lifted, then the kagune) after his Ukaku kakuja training.

## 4.0.9 (released)

The owner (quoted exactly): "I sent you a replacement for the Ken tower as well, make sure he sits on the top." Their file:
`art/exploration/Ken_tower.litematic` (1.21.4, 73x310x73). `tools/companion_structures.py` reads Litematica (`litematic`, `unpack`) and drops
whole layers (`drop`: 108-143 and 204-239, seamless 4-layer repeats) -> `structures/cyber_tower.nbt` 73x238x73, Ken at (46, 193, 36) yaw -90.
`EtoGesture.SIT` (held, appended; protocol 108), `EtoPose` SIT, `Persona.sitsAtHome` (Ken), `EtoBrain.sit/sitting` (`SIT_YAW`: the yaw he
was made with). Previews: `docs/handoff/ken-tower-4.0.9.png`. Their later asks, queued: Ayato's god-like pull-out; the Ukaku base form's
wing slams and wing guard; then fighting styles (Speedster), the CCG building (`art/exploration/ccg_building.schem`), the General, Kick
Master.

## 4.1.0 (released)

The owner (quoted exactly): "Add to the todo list the new animation after training with Ayato (Ukaku kakuja quest) the player gains a new
pull out kagune animation sort of like Ken gives the player. the player lifts both hands god like and then they're unleashed." (with a
picture: arms out wide and low, palms up, floating). `server/AyatoPullOut` (taught: Ukaku path and `Gate.UKAKU_KAKUJA` open; OUT_AT 20),
`client/AyatoPullOutClient.lift`, `S2CUkakuFx.UNLEASH` (49), `PathServer.toggle` (Noro, then Ayato, then Ken), `KaguneBodyAnimation`.
Protocol 109.

## 4.1.1 (released)

The owner (quoted exactly): "bug, make sure NONE of the structures spawns in water or collides with water bodies, the mansion is currently
happily resting at the very bottom of the ocean..." Cause: 4.0.2 built the one hideout at its concentric ring's spot whatever the ground;
the villages' 5 x 5 check allowed water at the rim; the jigsaw places never checked. `village/DryLand` (pure: `columns`, `dry` (the wet
biome tag every 4 blocks, water over the ground every 16, footprint and 8 out), `spreadChunk` (RandomSpreadStructurePlacement's linear
offer, java.util.Random), `hideoutOffers`, `choose`, `shifts`, `runs`, `spots`, `reachable`). `ControlStructures.Site`: the hideout only
at `hideout(context)` (cached per seed and generator; its structure set is now `random_spread` 40/20, the same numbers as `DryLand`);
other sites try `TRIES` spots `SHIFT` apart. `ControlStructures.DryPlace` (`hexkagune:dry_place`, Anteiku, library, cyber tower): the
vanilla jigsaw run in a `GenerationContext` moved up to 5 chunks, its pieces' box kept within the start chunk's reach. Tag
`hexkagune:wet`. Protocol 110. Old worlds keep what they generated.

## 4.1.2 (released)

The owner (quoted exactly): "Another rework: Ukaku kagune base firery form no longer fires projectiles, M1s are a series of different wing slams combos. (create highly detailed moveset switching between left wing and right wing slaps, swings, upwards to downwards slams (These can now clash like Rinkaku) and has lightly less range than Rinkaku. Right click (M2) is now a protection like Rinkaku has, wings grow and protect the player , they come together to protect the player, just like rinkaku they break after a bunch of hits, perfectly timed right clicks can do the same as Rinkaku and stagger. Note: Crystal mode still has the same custom shooting crystals and right click, we're only touching the base from, everything else stays as it is."
`StrikePattern` WING_SLAP, WING_SWING, WING_RISE, WING_SLAM, TWIN_SLAM (after BEHEADING; not shot, flame or crystal, so `KaguneCombat`
registers them with `KaguneClash`). `ComboStyle.UKAKU_WINGS` (7 stages) and `UKAKU_LEFT_WING` (Rabbit Guise, 4), after UKAKU_FIRE (which
no form gives any more; `UKAKU_ENDERS` is now only reached by crystal mode's Fire Dash). `UkakuRules.style`, `SLAM_REACH` 0.85, `slams`,
`wingGuard`. `KaguneCombat.range`. `WingRig` motions SLAP/SWING/RISE/SLAM _RIGHT/_LEFT, TWIN_SLAM, GUARD (`GUARD_GROWTH` 0.3).
`RinkakuBlockServer.ukakuGuard` and `can`; `RinkakuBlockClient.ready`; `UkakuInput.useInput` returns early when `wingGuard`.
`UkakuMutation.FIRE_DASH`'s text says crystal mode. `ComboStyle` ordinals are sent, so protocol 111. `UkakuTests.wingSlams`.

## 4.1.3 (released)

The owner (quoted exactly): "The default M1 fighting style is only unlocked through a new quest with ayato, until a fighting style is unlocked, the player can only use default punches in kakugan mode, right click is also default, the previous M1 and M2 flips are now all part of a fighting style called Speedster."
`control/FightStyle` (pure: NONE, SPEEDSTER; `of(open)`, `ownAttack`, `ownUse`; the next style, Kick Master, goes here). `Gate.STYLE_SPEEDSTER`
(appended; no path; never grandfathered: `QuestServer.had`). `Teaching`: lesson `ayato_speedster` (appended, bit 8), `fits(lesson, path)`
(a lesson with no path: any chosen kagune but PALE), `Deed.BARE_HANDS` (proof: 6), `view(..., proving)` (a proof under way stays the next
lesson). `PaleServer.style`, `eyesAction` and `punched` (the blitz and the flips only with a style; Z and V untouched). `PaleClient.style`,
`PaleInput.useInput` and `attackInput`. `EtoServer`: the proof's count in the kill handler. `AbilityGuide` rows. Protocol 112.
`CompanionTests.speedster`.

## 4.1.4 (released)

The owner (quoted exactly): "CCG building, tons of ccg agents guard and patrol the area, sit in their offices" and "Also I forgot to send you the CCG building, this is it, also like the others only 1 of them spawns." Their file: `art/exploration/ccg_building.schem` (39x68x39; its top 10 layers
are empty and left off: 58). `tools/exploration_structures.py`: a fourth site, `ccg_building` (map 6, kind `ccg_hq`, `top`, `headquarters`:
posts, patrol spawns, seats per floor (a bottom straight quartz or dark oak stair, nothing over it but an open trapdoor), a hall guard per
floor; its `loop` is the route, some of it outside the plate). Run it with `--blocks` (minecraft-data's pc/1.20/blocks.json, from the npm
package `minecraft-data`); it rewrites every site's tiles (the same bytes) and `SiteMaps.java`. `DryLand.ccgOffers`, `near`, `CCG_SALT`,
`CCG_APART` (12 chunks from any hideout or village offer). `ControlStructures.Site.ccg` and `chooseCcg` (cached per world like the
hideout's; its judge looks at the footprint and `CCG_ROUND` out). Data: `worldgen/structure/ccg_building.json`,
`structure_set/ccg_building.json`, `tags/worldgen/biome/has_structure/ccg_building.json`. `npc/HqRules` (pure: SEAT, POST, YAW in the
entity's saved data, `yaw`, `seat`, `sits`, `posted`). `server/AgentDesk.idle` (called from `AgentBrain.patrol`; `Mind.seatKept`, and
`tick` stands it up when not kept). `CcgAgent.SEAT` (entity data, appended). `AgentPose.seated`, `AgentClient.pose`. Protocol 113.
`NpcTests.ccgBuilding` (reads the template back), `AgentTests.headquarters`. Next: the General goes in this building (a room on the
top floor: add him to `headquarters` as the mansion's `rooms` are).

## 4.1.5 (released)

The owner (quoted exactly): "Attitude: Tough, softens up when romanced, embarrased easily, proud of the CCG, has to be convinced to love a ghoul, Only interested in woman players." and, with a skin attached in chat: "use this skin for the general and give him all the formal outfit (but the hat)" (which replaced "use one of the male skins currently available
... and change it a bit"). Their skin: `art/exploration/General_skin.png` (recovered byte for byte from the chat; copied to
`art/general_skin.png`, the bake's source; `tools/companion_textures.py` general: slim, no eye turns, eyes on the face's row 4).
`Persona.GENERAL` (appended; key `general`, name "General Kuroda", `human()`, `outfit()` = the Formal Outfit less its helmet, `muscle()`
= `ReaperBuild.GENERAL_SHARE` 0.45, size 1.0 because `CompanionTests.personas` caps it), `GeneralNpc`, `ControlEntities.GENERAL`,
`GeneralScript` (`CONVINCED` 90, `convinced(c)`; `LESSONS` only refuses for now), `Courtship.GENERAL` (no GIVE: the tests want a GIVE's
items to be the mod's own, and every dish has meat in it), `talk/general.txt`, his lines in `MoodActs`, `MoodKiss`, `Tolerance`, `Drowsy`.
`EtoBrain.dress` (as the mind is made) and the CcgAgent filter after `seek`. `EtoServer.context`: `asGhoul` (a full mask hides a ghoul
from a human persona). Placed by `tools/exploration_structures.py` (`rooms` of `ccg_building`). Protocol 114. `CompanionTests.general`.
**Next: Kick Master** (`FightStyle.KICK_MASTER`, a gate, a `Teaching` lesson of his for a human or a ghoul in a full mask, and his own
fight using it), then the owner's newest ask, quoted exactly: "after that please put all your focus into the combat rework, and kagune
rework for the ukaku, this model is very expensive and I need you to focus on what's hard, study complex fighting moves, and combos and
make it look smooth and beautiful."

## 4.1.6 (released)

The owner (quoted exactly): "also I cant get the aogiri hideout to spawn or the ccg building at all."
Cause: `DryLand.choose` built each only at the first offer that was entirely dry (no wet biome every 4 blocks, no water over the
ground every 16, over the footprint and 8 out); the mansion's footprint is all a start chunk reaches (272 x 263), so many worlds had no
such offer and no hideout (`choose` returned null; `/locate` found nothing). The spot is fixed per world, so an old world's may lie in
ground made before; and `/place structure` failed everywhere else. Now `DryLand.pick`: `choose` over the near offers (unchanged where it
found one), then over `hideoutFurther`/`ccgFurther` (regions `REGIONS` to `FAR` 8), then `driest` (`Judge.wetness`: the middle on dry
land in its biomes, the least `DryLand.wetness`), near then further. `ControlStructures.Site.onPurpose` (the context's biome test takes
null: only /place's does) builds either where asked (`standing`). `told` logs the spot once a world. Protocol 114 (unchanged).
`NpcTests` (the chain, wetness, further offers). Not tested in game.

## 4.1.7 (released)

The owner (quoted exactly): "He teaches a new combat style mixing kicks and punches (mostly side kicks, spinning kicks etc), he only teaches the
player this through quests if the player is either a human or wearing a full mask." "Do a very deep research into combat moves like I described
make sure the corehography is perfect, this combat style upon learned fully replaces the default M1 combos." "New fighting style called:
"Kick Master" Make up the quests yourself make sure they're creative and beautful."
`PaleStrike.Style` KICKS: KICK_JAB, KICK_CROSS, ROUND_KICK, SIDE_KICK, BACK_KICK, KICK_HOOK, HOOK_KICK, TORNADO_KICK (`of`, `next(prev, mastery,
style)`, `kick()`, `lift()`). The kicks are designed in `tools/PalePoseDesign.java` by where the sole goes (`Spec.sole`, `solveLeg`: the leg's two
angles and the lunge solved together; a straight leg's sole is a leg's length from the hip) and checked from the side (`tools/pale-preview.sh <out>
kicks kicks-film`). Measured on the rig: side-on, `roll` only bows her forward or back; the trunk is laid away by `side`. `RigPose.settle`: `side` up to
0.85 (was 0.6), drop up to -10 (the tornado's jump; down still 4), spin up to +-4 pi (the tornado's 2 pi + 1.25). `PaleRules.comboKickStand` (1.15 +
w/2): kicks from further out. `FightStyle.KICK_MASTER` (`strikes()`, `of` prefers the last learned, `use`: right click Speedster's flips till Kick
Master's own). Gates `STYLE_KICK_STANCE`, `STYLE_KICK_CHAMBER`, `STYLE_KICK_MASTER` (appended). `Teaching.GENERAL`: three lessons, each a proof
(`Deed.NIGHT_BARE` 8, `AIRBORNE` 3, `DAWN` 3; `Teaching.dawn` 22300..1500), `Refusal.GHOUL`; `view` takes what he sees (`EtoServer.context`
`seenGhoul`). `PaleServer.bare` (a human with it, the hand empty: M1 alone), `EyeRules.HUMAN_POWER`/`HUMAN_MASTERY`; `PaleClient.bare`, `fighting`
(a human posed while the server says a blow of theirs is under way), `useStyle`. `EtoOwl`: a human persona throws KICKS and no Big Punch. Protocol
115. `CompanionTests.kickMaster`; `PaleTests` (the lift; the fist in view for punches only). The kicks hold the kicking guard (`KICK_GUARD`:
counterweight arms clipped in the blends). Previews: `docs/handoff/kick-master-4.1.7.png`, `kick-master-film-4.1.7.png`.

## 4.1.8 (released)

The owner (quoted exactly): "Right click is a chargeable teleport speed kick (dash to an oponent at high speed and side kick with a side twirl."
The Speed Kick (`PaleMove.KICK`), charged on the hold and twirling: `PaleMove.TWIRL_CHARGE` (appended), `C2SPaleAction.TWIRL` (13),
`PaleMoves.twirlKey`/`tickTwirlCharge`/`releaseTwirl` (`kickPlan(player, range)`). A speed kick's detail is now its dash's ticks
(`KICK_DASH_BITS`), `TWIRL`, and the charge in sixteenths above them (`twirlDetail`, `kickTicks`, `twirled`, `twirlCharge`); every
`PaleRules.kick*` rule reads it so (plain kicks unchanged). `kickThere` (seen) vs `kickArrive` (the chamber, `TWIRL_SPIN` 3 later);
`twirlLeft`; `twirlRange` 6..20, `twirlPower` 0.3..1. `PaleMotions.kick` (the twirl: the chamber turned), `twirlCharge` (the coil; the head
turns only 0.27 against the guard's fists). `PaleInput.twirls`/`fights` (the air, or a body no one talks to: `talks`)/`twirlKey`. `EtoOwl`: a human persona's
speed kick twirls at full. `FightStyle.ownUse` true for Kick Master. Protocol 116. `CompanionTests.twirlKick`. Preview:
`docs/handoff/twirl-kick-4.1.8.png` (`tools/pale-preview.sh <out> twirl-kick`, also a GIF).

## 4.1.9 (released)

The owner (quoted exactly): "Important top priority bugs: Cant find CCG building, Aogiri Hideout or Kaneki's building AT ALL not even with
locate. Opening the inventory crashes the game instantly."; "it should be able to spawn literally in any biome as long as there's only one of
each structure aside from the villages, of course. It should also be fully compatible with Biomes of Plenty". The creative tabs' lambdas
implement `DisplayItemsGenerator` by its SRG name `m_257865_` (the stub had `accept`: an AbstractMethodError on opening the inventory).
Every structure's biomes are `#hexkagune:overworld`; every place type overrides `findValidGenerationPoint` (`m_262864_`) and judges its own
land (any biome but `hexkagune:wet`); the old per-place biome tags are gone. Anteiku, the library and Ken's tower are random spreads, one
each per world (`DryPlace.chosen`, `DryLand.firstPlaced`, `PLACES`); every chooser reads its set's spread from the world
(`ControlStructures.spread`). Protocol 116 (unchanged). `NpcTests.onePerWorld`. Details: `next-ai-4.1.9.md`.

## 4.1.10 (released)

The owner (quoted exactly): "put all your focus into the combat rework, and kagune rework for the ukaku, this model is very expensive and I need
you to focus on what's hard, study complex fighting moves, and combos and make it look smooth and beautiful." The base form's wing slams
(`WingRig.Blow`, `beats`, `slam`, `tip`, `slamFrame`, `slamFold`, `slamThrust`, `slamHard`, `counterPull`, `guard`) and their body
(`UkakuBody.slams`, `Keys.blow`): a chamber, a drive landing at full speed, an overshoot, a recoil, the whip, the feathers, the other wing's
pull, the body a beat ahead. Visual only; protocol 116. `UkakuTests.slamBeats`, `slamBody`. Previews `docs/handoff/ukaku-slams-*`
(`tools/art-preview.sh <out> slams slams-gif`). Details: `next-ai-4.1.9.md`.

## 4.1.11 (released)

The owner (quoted exactly): "You can make the wing stretch further forward, because it currently just feels like a little chicken wings, bro.
Whenever they stretch to slap something, they can stretch to be larger in size and length as well." A slamming wing stretches to the slam's
reach (`UkakuRules.slamReach`, `WingRig.lash`, `stretchMost`, `TIP_SHARE`, `STRETCH_MOST`), lashed level ahead (`slamFrame`, `lashElevation`,
`LASH_TRAIL`), folded tight (`LASH_FOLD`); the whip's spring scaled to each blow's drive (`WHIP_PER_DRIVE`). Visual only; protocol 116.
Details: `next-ai-4.1.9.md`.

## 4.1.12 (released)

Places never nowhere and `/kagune places`; the wing slams without particles and ~1.75x as long; each coat half on its own leg. Details and the
owner's words: `next-ai-4.1.9.md` ("4.1.12, what and where"). The structures' failure in the owner's game is still unexplained.

## Next

Nothing outstanding from the owner. If they report in-game problems with the outfits' fit, adjust `CLEAR`, `SNUG`, the drape
(`outfit/CoatDrape`) or the collar (`COLLAR_FLARE`) in `tools/outfit_models.py` and rebake (`python3 -I tools/outfit_models.py`), never
their art files; their masks are drawn as their pack makes them.

House rules as in `next-ai-3.6.6.md` (nothing tested in game; answer in English; quote the owner exactly).

## The owner's words during this session (quoted exactly)

- On the failing CI: "note that all the latest pushed failed!" (fixed: two name checks in the tests predated the owner allowing Ayato and
  Aogiri; the dotted mask's id; every push since builds with EXIT 0 locally first).
- Of the 4.0.2 renders: "Do I need to say anything about both outfit's legs and arms?" (4.0.3: the coat drapes over both legs, the
  sleeves close over the arm's inner side, each half of a cut piece shows its own part of the texture.)
- "On the Ougiri outfit, no more hood, make it less bulky around the shoulders and head to be similar to the formal outfit, and, give it a
  high collar on the sides of the head pointing outwards." (4.0.3: no hood, a high flaring collar on the coat.)
- With `Tokyo_Ghoul_24_Masks_1.20.1.zip`: "New masks, replace ALL the masks with these models." (4.0.4.)
- "Ayato romance should be quite similar to kaneki, animations can be the same. Noro is taller than the average player and is very brief,
  barely talks." (Ayato: Ken's style and household words; Noro: size 1.15, nearly silent.)
- "Add to the to-do list: bugs and needed fixes: Bikaku: Sometimes the limbs break and dont regenerate cause of the lack of energy, so let me
  specify, Bikaku users dont have the same regeneration as regular wounds, they either regenerate with this mutation or with their other
  ability to regenerate, wounds appear like any other ghoul but regen for them does not cost energy at all, it works with cooldowns, and
  waiting. The walker legs needs more flluidity and better arching, it walks super robotic, sure it works but it's super ugly to see, the
  legs look like sticks, and dont move naturally. Make sure The child of a Bikaku user with all the other combinations matches perfectly.
  Children: Kids work with chances now, depending on the parents, assuming for example a Bikaku user and a Ukaku user, the child has a
  chance to come out with only 1 kagune type like just 2 ukaku wings, just Bikaku worm tendrils, or both. Armor and Masks: Armor is too
  bulky, if the player is alex model it should adapt to it as well as the male body's muscle shape. Masks are… very very lack luster, half
  masks are covering most of the face instead of being lower, way lower. The other masks are very basic and lack details, not only that
  but the eye position of the masks are very off."
