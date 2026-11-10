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
