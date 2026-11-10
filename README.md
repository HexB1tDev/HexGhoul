# HexKagune 7.0.1 — Kagune Paths

Forge 1.20.1 mod update built from the supplied `hexkagune-1.0.0.jar`.

The original Gradle project was never available, so this repository contains the **exact input JAR**, a narrow,
verified Java 17 bytecode patch, and — new in 1.1.0 — a separately compiled expansion package
(`com.hex.kagune.control`) that the patch hands specific moments to. Nothing in Minecraft itself is modified (no
Mixins), and every existing kagune system (forms, mutations, rendering, grapple/leap, guard, crush/lift/slam, Centipede
autonomy, projectile deflection, particles, sounds, saves, networking) keeps working as before.


## 4.1.3 (HexGhoul): Fighting styles, and Speedster from Ayato (testing)

See `.github/release-notes-4.1.3.md`. Your words: "The default M1 fighting style is only unlocked through a new quest with ayato, until a fighting style is unlocked, the player can only use default punches in kakugan mode, right click is also default, the previous M1 and M2 flips are now all part of a fighting style called Speedster." With only the kakugan open and no style learned, M1 is the game's own punch and right click the game's own. Ayato teaches Speedster (the Speed Blitz on M1, the flips on right click) to any ghoul, after his proof: six hostile things killed with your bare hands, eyes open. No save starts with it. Z and V are as they were. Protocol 112.

## 4.1.2 (HexGhoul): The Ukaku's wing slams and wing guard (testing)

See `.github/release-notes-4.1.2.md`. Your words: "Another rework: Ukaku kagune base firery form no longer fires projectiles, M1s are a series of different wing slams combos. (create highly detailed moveset switching between left wing and right wing slaps, swings, upwards to downwards slams (These can now clash like Rinkaku) and has lightly less range than Rinkaku. Right click (M2) is now a protection like Rinkaku has, wings grow and protect the player , they come together to protect the player, just like rinkaku they break after a bunch of hits, perfectly timed right clicks can do the same as Rinkaku and stagger. Note: Crystal mode still has the same custom shooting crystals and right click, we're only touching the base from, everything else stays as it is." The fire's M1 is a string of wing blows (twin wings: two slaps, a wide swing, a rising wing, two overhead slams and a Twin Slam; Rabbit Guise: four with the left wing), which clash like Rinkaku's and reach 85% as far. Right click is the wing guard, by Rinkaku's rules (it breaks, a perfect one staggers). The base form's fire shots, enders and Fire Dash are gone; crystal mode keeps its own. Protocol 111.

## 4.1.1 (HexGhoul): No structure in water (testing)

See `.github/release-notes-4.1.1.md`. Your words: "bug, make sure NONE of the structures spawns in water or collides with water bodies, the mansion is currently happily resting at the very bottom of the ocean..." Every structure checks the land under it and 8 blocks round it: no wet biome (sea, river, beach, swamp) every 4 blocks, no water on the ground every 16. The mansion is built at the nearest of its world's offered spots that is dry land, still one per world; the villages try nearby spots, and Anteiku, the library and Ken's tower move up to 5 chunks to dry land. Chunks already made don't change. Protocol 110.

## 4.1.0 (HexGhoul): Ayato's way of bringing the kagune out (testing)

See `.github/release-notes-4.1.0.md`. Your words: "after training with Ayato (Ukaku kakuja quest) the player gains a new pull out kagune animation sort of like Ken gives the player. the player lifts both hands god like and then they're unleashed." Once Ayato has taught you the First Kakuja, you bring the kagune out his way: up on your toes, both arms lifted out wide, palms to the sky, chin up, and the wings are unleashed. Protocol 109.

## 4.0.9 (HexGhoul): Ken's new tower, and he sits on the top (testing)

See `.github/release-notes-4.0.9.md`. Your words: "I sent you a replacement for the Ken tower as well, make sure he sits on the top." Your new tower (from `Ken_tower.litematic`), nine of the repeating floors taken out of each of its two tallest tiers so it stands under a 1.20.1 world's top (238 tall, every part kept; tuff swapped for andesite and stone bricks). Ken lives on its top deck and sits there looking out whenever nothing calls him. Protocol 108.

## 4.0.8 (HexGhoul): Ayato's new skin, a little muscular (testing)

See `.github/release-notes-4.0.8.md`. Your words: "I sent you the new ayato skin, make him a little muscular but way less than the male player." Ayato wears your new skin (his kakugan, closed eyes and blush made from it, on the wide arms) and a broader chest, 30% of the White Reaper's build (a male player's is 60%). Protocol 107.

## 4.0.7 (HexGhoul): A full mask hides your kakugan (testing)

See `.github/release-notes-4.0.7.md`. Your words: "Full masks (only full masks not half masks) prevent CCG agents and humans to tell you're a ghoul when your eyes are in Kakugan." With a full mask on (16 of yours, 8 generic), investigators and townsfolk don't see your kakugan; your kagune or a kakuja still give you away; half masks and the Cloth Wrap don't hide the eyes. Protocol 107.

## 4.0.6 (HexGhoul): A torn-off limb takes its clothes, armour and mask with it (testing)

See `.github/release-notes-4.0.6.md`. Your words: "Make sure that when limbs are removed, over layer of the skin also goes with the head and etc, coats and armors go with the limbs as well. (same with masks)". A part torn off by Potential Unlock carries its skin's outer layer (as before), its coat sleeve, trouser leg and boot, and the head its cap and mask; your body draws none of it, nor vanilla armour on that part, nor the kakuja masks on a head that's off. Coat tails stay with the body. Protocol 107.

## 4.0.5 (HexGhoul): New worlds load again; official NPCs stay at home (testing)

See `.github/release-notes-4.0.5.md`. Your words: "also game crash upon creating world": since 4.0.2 the one-per-world Aogiri hideout's placement lacked the `salt` Minecraft requires, so no new world could be made; fixed, and every placement is now tested for its required fields. Your words: "Official NPCS disappear from their residential areas and dont come back they should always stay there unless married to a player and set home in another place." Once anyone has met an official character, no other body of hers takes her place while she lives (one from another of her buildings did, once hers had lain unloaded five days, and hers was sent away). If you've been at her home half a minute and she's nowhere about (not out or left waiting), she's made there again. A married character's home is the one you share. Protocol 107.

## 4.0.4 (HexGhoul): Your 24 new masks (testing)

See `.github/release-notes-4.0.4.md`. Your words, with `Tokyo_Ghoul_24_Masks_1.20.1.zip`: "New masks, replace ALL the masks with these models." All 24 of your masks are your new pack's models and textures, every cube as you made it, read from your Blockbench projects with no refitting (half masks leave the eyes clear, full masks' eye holes lowered, every shell clear of the head and its hat layer); the asymmetric details on the wearer's left, as your renders have them. "Gourmet's Half Mask" is now "Gourmet's Mask". The nine generic masks are unchanged. Protocol 107.

## 4.0.3 (HexGhoul): Coats that hang over the legs; sleeves closed; the Crimson Outfit without its hood, with a high collar (testing)

See `.github/release-notes-4.0.3.md`. Your words: "Do I need to say anything about both outfit's legs and arms?"; "On the Ougiri outfit, no more hood, make it less bulky around the shoulders and head to be similar to the formal outfit, and, give it a high collar on the sides of the head pointing outwards." The coat tails hang over both legs (fronts with the leg furthest forward, backs with the one furthest back, folded at the knee, pushed clear of shins and boots, falling back slowly), so no leg comes through them; every sleeve closes over the arm's inner side; each half of a piece cut at a joint shows its own part of the texture. The Crimson Outfit has no hood (the Crimson Hood item is gone): its coat has a high collar beside the head, flaring out, and the head is free for a mask. Protocol 107.

## 4.0.2 (HexGhoul): Armour that moves; the Aogiri mansion; Ayato poses, Noro tilts his head (testing)

See `.github/release-notes-4.0.2.md`. Your words: "the armor does not allow for animation"; "the jacket also breaks when walking"; "replace the Ougiri hideout with another schematic ... I give them a mansion"; Ayato "a massive poser"; "Noro simply like tilts his head"; "The crack fingers animation is only Kaneki's". The outfits bend at the knees, elbows and torso with PlayerAnimator, the coat tails hang from the hip; your mansion is the world's one Aogiri hideout (guards at both gates who ask your business once, a patrol round the house, members on every floor, Noro and Ayato in rooms of their own; a wrong answer keeps you out five days); the villages as rare as Minecraft's; Ayato poses, Noro tilts his head (and his students tilt theirs as the kagune comes out); the finger crack is Kaneki's alone; the mod's own creative tabs (HexKagune, Quinques, Outfits, Masks). Protocol 106.

## 4.0.1 (HexGhoul): Outfits thinner and finished; The Walker strides (testing)

See `.github/release-notes-4.0.1.md`. Your words: "They seem a little too thick ... add more details"; "add like a color or something to the formal outfit"; on The Walker: "One leg at a time, not two in front at once ... They don't go back like further back enough or forward enough ... you can always tell where the next step is gonna be". Both outfits drawn close to the body; the Formal Outfit in navy piping, a white shirt, silver buttons and badges, a red pocket square, a belt, epaulettes, polished shoes and a visored cap; the Crimson Outfit with dark piping, a belt and buckled straps. The Walker strides one leg at a time, each foot from well ahead to well behind, every step's length, landing, height and speed random, the legs springy tentacle S-curves, your body swaying over them. Protocol 105.

## 4.0.0 (HexGhoul): The Great Exploration (testing)

See `.github/release-notes-4.0.0.md`. Your 4.0.0 spec, as one release: the human town (`hexkagune:village_1`), the CCG settlement (`hexkagune:village_2`) and the Aogiri Tree hideout (`hexkagune:aogiri_hideout`) from your schematics; the Formal and Crimson outfits and your 24 masks as real items (first and third person, players and NPCs), plus nine generic masks for wild ghouls; wild ghouls ranked D to SS; investigators in the Formal Outfit (each piece its own small drop); Aogiri members in the Crimson Outfit and their faction's mask; Ayato Kirishima (Ukaku, his kakuja, a romance on Ken's animations, the Ukaku kakuja questline and its cutscene); Noro (quiet, never romanced, the Bikaku teacher, three worms in a fight). Your to-do list too: masks fitted (half masks below the eyes, eye holes on the skin's eyes, more detail), armour snug and slim-armed for Alex skins, Bikaku regeneration free of energy, The Walker's legs arched and eased, a Bikaku parent's children (a worm; one parent's kind or both, by chance). Protocol 105.

## 3.7.3 (HexGhoul): Bikaku: Potential Unlock, torn apart and pulled back together (testing)

See `.github/release-notes-3.7.3.md`. Your request is in `docs/handoff/owner-words-mutations.md`; on the preview: "Continue Im still at 96". With energy, a blow that would kill you leaves you at one heart and tears a part off (an arm, a leg, now and then the head); a single blow of half your health does too. Each part is thrown, lies there in your skin, and strands of your kagune in your kagune colour reach it, wrap it and haul it back upright; whole in about 5 s, then a small heal; no energy, it waits. Off your model until then. Protocol 104.

## 3.7.2 (HexGhoul): Bikaku: absorbing a body gives Speed and the food's strength buffs for 15 s (testing)

See `.github/release-notes-3.7.2.md`. Your words: "Whenever an entity is absorbed, the player gains a speed buff for 15 seconds and some of our custom effects from foods, any that give special strength and buffs also for 15 seconds"; you chose "Grand Devouring only (Recommended)" and "Strength, Resistance, Refreshed, Replenished (Recommended)". When a body Grand Devouring swallowed is in you: Speed, Strength, Resistance, Refreshed and Replenished, level I, 15 seconds. Protocol 103.

## 3.7.1 (HexGhoul): Bikaku: The Walker on tap K, carried 3.5 blocks up over any ground (testing)

See `.github/release-notes-3.7.1.md`. Your words: "the walker should lift the player way higher up (like 3-4 blocks up)  and adapt better to literally any terrain ... the walker is the first tap K half kakuja, the full kakuja is the hold K"; its cost: "Half the kakuja's cost (Recommended)". K tapped: The Walker (12.5 energy, then half the kakuja's drain); K held: the kakuja. Carried 3.5 blocks up for real on two long legs, over the highest ground round you, let down under roofs, climbing walls, at a walking pace; no fall damage; not kicked for floating. Protocol 103. Preview: `docs/handoff/bikaku-walker-3.7.1.png`.

## 3.7.0 (HexGhoul): Fix: The Walker's arms never swing (testing)

See `.github/release-notes-3.7.0.md`. Your words: "the arms should not swing at any point (they currently do)". In The Walker your arms hang still at your sides (none of Minecraft's walking or attack swing), as your legs do. Protocol 103.

## 3.6.9 (HexGhoul): Fix: the Bikaku kakuja shatters when you turn back (testing)

See `.github/release-notes-3.6.9.md`. Your words: "Also Kakuja detransformation needs a simple shatter animation like the rinkaku has." Turning back, the kakuja breaks into pieces that fly off, spin, bounce once and fade, with the glass, as the Rinkaku's does (the same machinery); the worm grows back out of you. Protocol 103. Preview: `docs/handoff/bikaku-shatter-3.6.9.png`.

## 3.6.8 (HexGhoul): Fix: the Bikaku kakuja's camera fits its size (testing)

See `.github/release-notes-3.6.8.md`. Your words: "IN kakuja form you cant see anything, the camera needs to adapt to the new size." In the Bikaku kakuja the third-person camera turns round its crest and sits 9 blocks behind it (Minecraft's own sat inside its back); in first person your view is lifted to its crest. It grows in and comes back down with the kakuja; nothing of it is drawn within 2.6 blocks of the lens. Protocol 103.

## 3.6.7 (HexGhoul): Bikaku: Potential Unlock, first part (testing)

See `.github/release-notes-3.6.7.md`. Your words: "The maximum energy capacity is also permanently multiplied by 3", "Upon unlocking Potential Unlock, the player's energy bar permanently changes color", then "Should be Purple, the bar, my bad". Potential Unlock (unlocked by the NPC's quest; operators meanwhile: `/kagune bikaku mutation potential_unlock on|off`): three times the energy (everything spends a third of the bar; the menu reads "192 / 300"), the one bar (energy and food) purple, the menu's too; kept through death, relogging and restarts; Bikaku only. The Bikaku menu's bar reads "ENERGY" now (your words: "RC bar IS NOT A THING"). Coming: surviving, tearing off and the strands. Protocol 103. Previews: `docs/handoff/potential-bar-3.6.7.png`, `bikaku-menu-potential-3.6.7.png`.

## 3.6.6 (HexGhoul): Fix: no RC bar (testing)

See `.github/release-notes-3.6.6.md`. Your words: "The RC bar does not exist and is not meant to be a thing anymore, the energy and FOOD bar are the same, RC bar IS NOT A THING". The original's RC bar is never drawn now (it still was in creative and with the diet switched off, while the kagune was out). One bar: energy and food. Protocol 103.

## 3.6.5 (HexGhoul): Bikaku: Grand Devouring from inside the mouth (GeckoLib) (testing)

See `.github/release-notes-3.6.5.md`. Your words: "If the victim is another player, they must experience a special first-person perspective from inside the kagune's mouth"; on the draft: "I like the inside of the mouth, proceed wit hthe mod". A player being eaten sees, in first person, the inside of the mouth drawn with GeckoLib: breathing walls of flesh in the eater's kagune colour, rows of fangs, the throat into darkness, and the jaws closing in step with the world worm until the outside is shut out; the gulp squeezes up the throat. It is placed along the worm's own curve. The worm's head now stays down over the body until it is swallowed. Your words: "Grand devouring should not be in the kakuja form by the way." (it isn't). Protocol 103. Previews: `docs/handoff/devour-inside-3.6.5.png`, `bikaku-devour-3.6.5.png`.

## 3.6.4 (HexGhoul): Fix: /kagune cooldowns off ends the Bikaku cooldowns (testing)

See `.github/release-notes-3.6.4.md`. Your words, on `/kagune cooldowns off` not ending the Bikaku cooldowns already running: "it should." It now ends every Bikaku cooldown that is running (each move's, the kakuja's, Grand Devouring's), and switching to creative does too. Protocol 103.

## 3.6.3 (HexGhoul): Bikaku: Grand Devouring (testing)

See `.github/release-notes-3.6.3.md`. Your words are in `docs/handoff/owner-words-mutations.md`. GeckoLib is now required (your words: "use geckolib it is meant to be a requirement"). Grand Devouring: a body within 10 blocks at 1 heart or less is swallowed whole by one of your worms on its own. It rises and swells, arches over, comes down over the body, closes slowly, gulps, and a bulge runs down to your back; your bar refills. It is reshaped after your reports: thin out of your back, one smooth arch, steadily bigger to the mouth. 1-minute cooldown (your words: "Forgot to mention but Grand Devour should have a cooldown of 1 minute"). Not opened by mastery (your words: "these mutations are NOT unlocked through mastery they're gonna be unlocked through an NPC's quest introduced later today"); until then `/kagune bikaku mutation grand_devouring on|off` (operators). The body caught is held and stripped; its things are given back if it lives. The feeding worm does nothing else. Protocol 103. Previews: `docs/handoff/bikaku-devour-3.6.3.png`, `bikaku-devour-before-after-3.6.3.png`, `bikaku-menu-3.6.3.png`.

## 3.6.2 (HexGhoul): Bikaku: The Walker (testing)

See `.github/release-notes-3.6.2.md`. Your words: "the fourth before the Kakuja should be like the player no longer even like walks with their own legs, like they get like two separate appendages on either, like both sides emerging from the same spot as the worms, that when the player walks his body doesn't move, but like he controls these tentacles that walk around adapt to terrain. Being able to climb anything, like Dr. Octopus pretty much." The Walker (750): two walking tentacles out of the worms' root, one on each side, carry you. Your legs hang still, the feet step on the blocks around you (steps, ledges), and you climb any wall you walk into (sneak to hang on). No fall damage while it is out. Its moves are Three Worms'. Protocol 102. Preview: `docs/handoff/bikaku-walker-3.6.2.png`.

## 3.6.1 (HexGhoul): Bikaku: every move made by all the worms (testing)

See `.github/release-notes-3.6.1.md`. Your words: "to basically become overpowered, like, to do enhanced versions of themselves whenever they're, like, in different transformations, such as, like, the move that, like, drag pursuit, you can send all three of them, all three of the worm-like things, and the thing that sends the worm underground and then up can be like more than one, and whatsoever." In Twin Worms and Three Worms every move is made by all the worms, each from its own lane and a beat after the one before. Devouring Maw and Predatory Drag catch a body per worm. Ravenous Pursuit sends every worm after its own body, three bites each. Carrion Eruption comes up once per worm, under each one's body. One Worm is as before. Protocol 102. Preview: `docs/handoff/bikaku-form-moves-3.6.1.png`.

## 3.6.0 (HexGhoul): Bikaku: the forms (Twin Worms, Three Worms) (testing)

See `.github/release-notes-3.6.0.md`. Your words: "another form should have like two worms. The third should have three". Forms picked in the menu: One Worm, Twin Worms (250), Three Worms (500); The Walker (750) shown, coming. The worms come out of one root, each in its own lane; M1's bites take turns among them. The moves are made by the first worm for now (every worm in 3.6.1). Protocol 101. Preview: `docs/handoff/bikaku-forms-3.6.0.png`.

## 3.5.9 (HexGhoul): Bikaku: the worm goes round blocks (testing)

See `.github/release-notes-3.5.9.md`. Your words: "go around blocks like a worm, sort of, and actually go around them. It's being able to adapt to the terrain itself and pulling people back". M1's bites, Devouring Maw, Predatory Drag (along the ground) and Ravenous Pursuit find a way round what is between them and their target (the server's search through the open blocks, sent to everyone), are drawn along it, bite where it ends, and pull what they catch back round it; the way counts toward the reach. Protocol 100. Preview: `docs/handoff/bikaku-round-3.5.9.png`.

## 3.5.8 (HexGhoul): Bikaku: hold to throw further, up to 30 blocks (testing)

See `.github/release-notes-3.5.8.md`. Your words: "it should be a held move that the longer you hold it for, the longer it goes for ... up to like 30 blocks. Of course, attacks are not able to be performed until the kagune is pulled fully back." M1 and the worm's thrown moves (G, J, Z, B) wind up while held and are thrown as far as they were held for: a tap as before, 30 blocks after 2 s; a longer throw is slower out and back, nothing else until it is back; the reach on the action bar. Not round blocks yet (next). Protocol 99. Preview: `docs/handoff/bikaku-held-3.5.8.png`.

## 3.5.7 (HexGhoul): Bikaku: the kakuja's four moves and its passives (testing)

See `.github/release-notes-3.5.7.md`. In the kakuja (mastery 1000, mastery alone): Thousand Jaws (Z: every mouthed tentacle biting its own body again and again, the grins snapping at what is close), Endless Hunger (G: three mouths and a coiling tentacle hold one body and bite it, then the great maw crushes it; every bite heals), Flesh Cataclysm (B: draws in, bursts out roaring, tentacles flung out and swept round: close bodies hurled, struck ones hit hard), Undying Abomination (C: 10 s of extraordinary regeneration and still less damage, slowed but never stopped by a heavy blow). Autonomous Predation: its tentacles bite hostile bodies near it on their own. Protocol 98. Previews: `docs/handoff/bikaku-kakuja-moves-3.5.7.png`, `bikaku-kakuja-undying-3.5.7.png`.

## 3.5.6 (HexGhoul): Fix: Predatory Drag swings out wide round you (testing)

See `.github/release-notes-3.5.6.md`. Your words on 3.5.5's Drag: "why not make a bigger angle? it's going too close to the body". The Drag's worm now swings out in a wide arc to your right (its coils, the point it comes down past, where it meets the ground, where it holds its catch from), grips its catch from its own side, and swings wide on the way back: about 0.6 blocks clear of you typically, never under about 0.25 (it was about 5 cm). Protocol 97. Preview: `docs/handoff/bikaku-drag-wide-3.5.6.png`.

## 3.5.5 (HexGhoul): Fix: the Bikaku worm goes round your body, not through it (testing)

See `.github/release-notes-3.5.5.md`. Your report: Predatory Drag (and a few moves) went through the player's body. Every Bikaku worm is now drawn round you (the stretch that would pass through you swept round your side, smoothed; unchanged where it doesn't come near you). Predatory Drag comes down your right side to in front of your right foot, and drags its catch in to the side it came from. Protocol 97. Preview: `docs/handoff/bikaku-drag-round-3.5.5.png`.

## 3.5.4 (HexGhoul): Bikaku: the kakuja (testing)

See `.github/release-notes-3.5.4.md`. K at mastery 1000: you grow into a lopsided mass about 4.8 blocks high, covered in grinning mouths, with the worm's maw and six great tentacles (3 s in, 1.5 s out). In it you barely move, take 45% of every blow, can't be knocked back and regenerate; M1 is a tentacle's bite up to 14 blocks out. 25 RC, then 1 RC a second; 60 s cooldown. Protocol 97. Previews: `docs/handoff/bikaku-kakuja-3.5.4.png`, `bikaku-kakuja-close-3.5.4.png`.

## 3.5.3 (HexGhoul): Bikaku: Monstrous Regeneration and the passives (testing)

See `.github/release-notes-3.5.3.md`. Monstrous Regeneration (C, mastery 600): 8 s of strong regeneration, strands of the kagune crawling over your body and knitting it; 45 s cooldown, not invincible. With the worm out: Abnormal Regeneration (a ghoul's regeneration sooner and faster) and Pain Suppression (hard to knock back, stuns half as long). Protocol 96. Preview: `docs/handoff/bikaku-regeneration-3.5.3.png`.

## 3.5.2 (HexGhoul): Fix: Bikaku's bites heal again (testing)

See `.github/release-notes-3.5.2.md`. A Bikaku bite's half a heart (Predatory Feeding) was cancelled by the ghoul's regeneration, which takes any small heal it didn't make for food's natural regeneration. Every Bikaku heal now goes through as the ghoul's own.

## 3.5.1 (HexGhoul): Bikaku: Ravenous Pursuit and Carrion Eruption (testing)

See `.github/release-notes-3.5.1.md`. Ravenous Pursuit (Z, mastery 300): the worm splits into three thinner worms that lunge in snakes at up to three bodies (all three on one). Carrion Eruption (B, mastery 450): it tunnels under the ground (the ground heaving over it) and bursts up under its mark, biting and throwing it into the air. Previews: `docs/handoff/bikaku-pursuit-3.5.1.png`, `bikaku-eruption-3.5.1.png`.

## 3.5.0 (HexGhoul): Bikaku: Devouring Maw and Predatory Drag (testing)

See `.github/release-notes-3.5.0.md`. The worm's moves are on your kagune keys, opened by mastery alone. Devouring Maw (G): thrown wide, it clamps on, holds them up and shakes them, then flings them aside. Predatory Drag (J, mastery 150): along the ground, it drags them back to you, lifts them and slams them down. Protocol 95. Preview: `docs/handoff/bikaku-grabs-3.5.0.png`.

## 3.4.9 (HexGhoul): Bikaku, the worm kagune, in the game (testing)

See `.github/release-notes-3.4.9.md`. A third path on the starting screen: Bikaku. One thick worm out of the lower back (about 8 blocks of zigzag coils, a round mouth of fangs over the right shoulder), alive at rest, in your kagune colour, with a string of three M1 bites (Snap, Lunge, Maul) that heal you a little when they land. Its other moves and its kakuja come next. Protocol 94. Previews: `docs/handoff/bikaku-*-3.4.9.png`.

## 3.4.8 (HexGhoul): Fix: the Bone Greatsword's moves rebuilt (testing)

See `.github/release-notes-3.4.8.md`. The greatsword's six blows and its three moves are rebuilt with both hands on the grip: the sword turns in the hands (an arm here is a stiff stick, and set at one angle in the fist the left hand hung up to half a block off the grip and the blade flicked about). The stance, the hammer, the whip and the spear are unchanged. Sheet: `docs/handoff/bone_sword-moves-3.4.8.png`.

## 3.4.7 (HexGhoul): Fix: the move guide is one narrow column again (testing)

See `.github/release-notes-3.4.7.md`. The move guide above the chat is one narrow vertical column again (3.4.6's two columns made it wider, not smaller), drawn at 60% of its size with tighter spacing; every move is still a word or two.

## 3.4.6 (HexGhoul): A smaller move guide (testing)

See `.github/release-notes-3.4.6.md`. The owner's ask: the move guide was too big and clanky. Now every move is named in a word or two ("Fists", "Heavy", "Thy Death", "Eyes only"), the board goes to two columns past five moves, the title and form share one line, and it's a little smaller. Same moves, keys, colours and cooldowns, about half the size. Not tested in game.

## 3.4.5 (HexGhoul): Fix: the Spire Wave stays on your hand (testing)

See `.github/release-notes-3.4.5.md`. Fix (the owner's report): War's Spire Wave came loose from your hand in first person when you moved. 3.2.8 started the mass at the spot it set out from. It now starts at the fist you see, found again every frame from your eyes and look, so it stays on your hand as you move and turn. Not tested in game.

## 3.4.4 (HexGhoul): Fix: a crystal forming sounds like crystal (testing)

See `.github/release-notes-3.4.4.md`. Fix (the owner: 3.3.5's crystal forming sounds were bad): a crystal forming is now a rubbed wine glass singing (three swells) or a small bell's ring. These are real CC0 freesound recordings by ani_music and AlaskaRobotics, from Sonic Pi's sample library, used as recorded. A crystal's hit and shatter are unchanged. Not tested in game.

## 3.4.3 (HexGhoul): Fix: clashes follow who's stronger (testing)

See `.github/release-notes-3.4.3.md`. Fix (the owner's report: investigators, even the Reaper, sent a maxed ghoul flying in clashes): clash throws now follow only the gap between the two. The stronger stands its ground, the weaker is thrown, and at twice as strong it's flung. An even clash pushes both a short way apart, and one eye counts. A maxed ghoul flings Rank 0 to 3 and is never flung by the Reaper. Not tested in game.

## 3.4.2 (HexGhoul): Welcome home (testing)

See `.github/release-notes-3.4.2.md`. From the owner's list after 3.2.1, the last of it: kids greet you after days away. Each child now remembers (kept with your save) when it last saw you. After two or more in-game days away, it greets you by its temper and age, with more to say after a week, then runs in for its hug. Not tested in game.

## 3.4.1 (HexGhoul): A word at the door (testing)

See `.github/release-notes-3.4.1.md`. From the owner's list after 3.2.1: the companion and the child talk to each other at the door after your scene. Once you've answered the companion's door card, they and the child brought home have a four-line talk: the parent in their voice, found or after an outing, to a toddler or a bigger child; the child by its temper; the parent's answer to that; the child's last word. To a baby the parent talks alone. Not tested in game.

## 3.4.0 (HexGhoul): Where WERE you? (testing)

See `.github/release-notes-3.4.0.md`. From the owner's list after 3.2.1: siblings at home react to the one brought back. An older one covers for it or wants to know where it WAS, a younger one tells on it, one left behind from an outing minds it, and the one brought back answers, all by their tempers and ages (about 150 new lines). Not tested in game.

## 3.3.9 (HexGhoul): Homecomings without a home (testing)

See `.github/release-notes-3.3.9.md`. From the owner's list after 3.2.1: homecomings without a home. Bringing a child back used to need a family home; without one nothing happened. Now the place the child lives counts as home (16 blocks round it), with new scenes for the companions and the children that don't talk about a house, and the old house-and-door scenes kept for families that have one. Not tested in game.

## 3.3.8 (HexGhoul): Who's at the door (testing)

See `.github/release-notes-3.3.8.md`. From the owner's list after 3.2.1: temper-aware door scenes. When you bring a child home, Eto, Touka or Ken now meet you at the door by who that child is: a scene for each of the seven tempers, found or back from an outing (42 new scenes), and by age (baby, toddler, child or teen). Not tested in game.

## 3.3.7 (HexGhoul): Talking it out (testing)

See `.github/release-notes-3.3.7.md`. From the owner's list after 3.2.1: reconciliation talks. A companion who is cross with you, or a jealous partner, can now be asked "Can we talk about it?" (or "Are we okay?", "Are we still friends?", "Can we talk about earlier?"). It's a real talk with branches: owning it, being honest or offering to make it up ends it; "you're overreacting" or "you're being paranoid" makes it worse. New lines in each one's voice, and a second way each comes to make it up on their own (picked at random). Not tested in game.

## 3.3.6 (HexGhoul): A jealous partner acts it out (testing)

See `.github/release-notes-3.3.6.md`. From the owner's list after 3.2.1: a jealous animation. A jealous companion partner now acts it out (about 7 s): back turned, arms folded, a glare over the shoulder, a huff, the snub with a foot tapping, turning back round. It plays when you ask another companion out, or ask a villager to be together or confess to one, while they see it. They also say a new line about it in their own voice, and replay the act now and then while still jealous. Protocol 93. Not tested in game.

## 3.3.5 (HexGhoul): Crystal sounds from real glass (testing)

See `.github/release-notes-3.3.5.md`. 3.2.6's crystal sounds were the pack's steel and needle sounds raised in pitch. Now real glass recordings from the web, as recorded: three glass breaks for the shatter, glasses clinking and a crack for forming and hits (CC0, freesound via the WilhelmSFX mirror, credited). Same loudness as before. Not tested in game.

## 3.3.4 (HexGhoul): One partner, for good (testing)

See `.github/release-notes-3.3.4.md`. The owner's rule: one partner, no break-up, no divorce. While you have a partner (dating or married, Eto, Touka, Ken or a villager) nobody else will start anything with you, and no ring while you're married. Killing Eto, Touka or Ken no longer ends it: they come back, still yours. A villager partner's death is the only thing that frees you. Old saves keep the partners they have. Not tested in game.

## 3.3.3 (HexGhoul): The Dragon has a voice: real recordings from the web (testing)

See `.github/release-notes-3.3.3.md`. The Kakuja V2 Dragon only played the pack's quiet sounds (3-37% volume) and breathed once every 3.5 s: from the camera, silence. Now 14 real CC0 recordings from freesound (via the WilhelmSFX mirror), as recorded, at full volume: roars as it breaks out and every few seconds, a death cry as it shatters, its crawl (a growl-chitter and heavy stomps), earthquake crashes, crunching bites, its body cracking apart. Credited in sfx/recorded/CREDITS.txt. Not tested in game (and not listened to: tell me which sounds wrong).

## 3.3.2 (HexGhoul): Dawn has an idle (testing)

See `.github/release-notes-3.3.2.md`. Standing still with Dawn out, a slow loop: breathing, Dawn turned to and fro in your fingers, then carried up and aside at ease (point raised to your right, left hand on your hip, head tilted, the point tapping), and back down. Moving, swinging or aiming stops it. Hold and M1s unchanged. Not tested in game.

## 3.3.1 (HexGhoul): Ken teaches the Rubble Hurl and the Impale, with proofs (testing)

See `.github/release-notes-3.3.1.md`. The Rubble Hurl and the Impale now come from Ken, not mastery alone: past 250 and 350 he teaches them (the Hurl first), each after a proof in his words: throw five bodies from your tails, then crush five in them. Until then the key says who teaches it and the guide shows them greyed. New this week, so no save keeps them. Not tested in game.

## 3.3.0 (HexGhoul): The move guide shows what your keys do right now (Rinkaku) (testing)

See `.github/release-notes-3.3.0.md`. The Rinkaku guide on the left crammed what G, N and H do with and without someone held onto shared lines, and showed Lift and Slam as ready when they need someone held. Now it follows your tails live: nothing held, Grab, Rubble Hurl (N held), Impale (H held), Lift · Slam greyed; a tail holding someone, Throw, Crush, Lift, Slam them and Grab another. Not tested in game.

## 3.2.9 (HexGhoul): Fix: the Wing Vortex: thinner, round, straight forward, and you can see past it (testing)

See `.github/release-notes-3.2.9.md`. The vortex was big rolling loops of flat ribbons (crumpled panels up close), aimed from the shoulder at a body's middle or the ground under the crosshair (so it often went into the ground a few blocks off and ended: "stays in place"), and in first person it and its launch burst filled the view. Now a thin round corkscrew (three round strands, a thin jet) flying straight forward along your look, only a little up or down; from your own eyes a see-through jet that opens as it goes, no burst or smoke at your eyes. Not tested in game.

## 3.2.8 (HexGhoul): Fix: War's Spire Wave was a mess in first person (testing)

See `.github/release-notes-3.2.8.md`. In first person the Spire Wave's charge centipedes coiled round the third-person body's arm under your eyes, and the beam bent back to it, a wall of centipede heads in your face. Now your own left arm is drawn (your skin, War's sleeve), reaching out as it charges and thrust with the beam, the centipedes coiled round it; the beam leaves your fist as a cone that widens away from you. Third person unchanged. Not tested in game.

## 3.2.7 (HexGhoul): Fix: Peace's shields looked like shields and floated off the tails (testing)

See `.github/release-notes-3.2.7.md`. Aegis of :Re's three shields were knight's heater shields (silver rim, red cross), held in the air off their tendrils' ends, the top one flat over his head. Now each tendril opens like a hand into a cupped fan of crimson flesh on seven white bone ribs that run on into claws, grown on a stalk at the tendril's end; the top one rises behind his head like a hood, the side ones hang close. Gameplay unchanged. Not tested in game.

## 3.2.6 (HexGhoul): Fix: crystals sounded like punches (testing)

See `.github/release-notes-3.2.6.md`. The crystal's shatter was a bone crack and punch collisions, its hit the punch hits. Now crystal forms, hits and shatters from the pack's brightest steel and needle sounds raised into a ring, no punch or bone in any of it. The pack still has no real glass recording: send crystal sounds (or OK vanilla's amethyst) for the real thing. Not tested in game.

## 3.2.5 (HexGhoul): Fix: clash impacts were barely audible (testing)

See `.github/release-notes-3.2.5.md`. The clash sounds were set to 3.5-8% of their volume (a cap to keep three stacked from clipping); now every clash sound plays at full gain (21-29 dB louder), the loudest in the pack, and the ground rumble at 60%. Not tested in game.

## 3.2.4 (HexGhoul): Fix: a maxed ghoul thrown back in clashes by low-rank investigators (testing)

See `.github/release-notes-3.2.4.md`. Clashes now measure both sides on one scale: an investigator by their rank, you from a fresh Rank 0's power up to the Reaper's with your mastery, the gates of your path opened by quests and lessons, and the ghoul meat in you (plus form and mutations). Fully progressed, you meet the Reaper as an equal, out-push every investigator up to Rank 7, and send Rank 0-1 flying. Not tested in game.

## 3.2.3 (HexGhoul): Fix: companions frozen in the kiss pose (testing)

See `.github/release-notes-3.2.3.md`. The owner's bug: a companion sometimes stayed stuck leaning in after a kiss. A blush given as a reaction (37 talk answers, your kakugan) used the kiss's held pose and never let go. Now a held pose with no kiss or hand behind it ends after 4 seconds, a reaction blush is drawn as a shy one, and villagers' kiss and hand poses end with the kiss. Not tested in game.

## 3.2.2 (HexGhoul): One at a time: nobody piles on (testing)

See `.github/release-notes-3.2.2.md`. While someone has you (walking up, a hug, a kiss, on your arm, by the hand, a child's moment, a card or a talk), nobody else starts something with you by themselves; the next waits 4 seconds after they let go. Companions, children, villagers and ghouls alike; what you ask for is never held back. Not tested in game.

## 3.2.1 (HexGhoul): Bringing your child home yourself: who meets you at the door (testing)

See `.github/release-notes-3.2.1.md`. Carry or walk a child of yours home after a while out: its other parent at home (Eto, Touka, Ken) meets you at the door, and it matters whether you found the child or the two of you were out together, the hour, carrying it, their mood, your other partner; the child calls out to them in its own temper. With nobody waiting, the child talks to you itself, by age and temper. 94 new scenes, about 950 lines. Not tested in game.

## 3.2.0 (HexGhoul): The word at the door: bringing the child home, by who is talking (testing)

See `.github/release-notes-3.2.0.md`. When Eto, Touka or Ken brings a wandering, night-roaming or rampaging child home, the word at the door is now a real exchange that knows why: the parent on what happened, the child in its own temper, the parent's answer to that kind of child, sometimes the child's last word. About 350 new lines. Not tested in game.

## 3.1.9 (HexGhoul): Jealousy you can see; co-wives and half-siblings who know about each other (testing)

See `.github/release-notes-3.1.9.md`. A partner who sees you being sweet on another says so to your face. Two of your partners who meet talk (first meeting, rivalry, the kids, settled), and your children by different mothers or fathers talk about it. New conversations about the other wife and the half-siblings. Not tested in game.

## 3.1.8 (HexGhoul): A signature move for the greatsword, the hammer and the whip (testing)

See `.github/release-notes-3.1.8.md`. Sneak + M1: the Tendril Whip's Tendril Swing (lash a block, get hauled to it), the Flesh Hammer's Fault Line (the ground breaks open in a line ahead), the Bone Greatsword's Bone Wall (a guard; a perfect parry staggers them and readies the overhead cleave). Protocol 92. Not tested in game.

## 3.1.7 (HexGhoul): Rubble Hurl tails go round the body, never through it (testing)

See `.github/release-notes-3.1.7.md`. The tails gripping the Rubble Hurl's chunk cut straight through the torso and head; now they leave the back, go round their own side and up behind the shoulder beside the head, then to the chunk. A new check builds 4,400 grip poses and requires the tail never to touch the body. Not tested in game.

## 3.1.6 (HexGhoul): Rinkaku's Rubble Hurl on the Throw key (testing)

See `.github/release-notes-3.1.6.md`. With nobody held, N is the Rubble Hurl: two tails tear a chunk out of the block you look at (or the ground before you; put back later, only where you may break it), hold N to make it bigger, let go to hurl it at what you aim at. It bursts on what it hits: damage, knockback, a short stun; in the final kakuja it splits in three. With someone held, N is still the throw. Protocol 91. Not tested in game.

## 3.1.5 (HexGhoul): Rinkaku's Impale on the Crush key (testing)

See `.github/release-notes-3.1.5.md`. With nobody held, H is the Impale: one random tail (never a centipede leg) rises over its shoulder and shoots straight out where you look, its end a sharp point; hold H and it keeps going (16 to 30 blocks). Whatever it runs through is impaled and dragged along, mobs and players; a wall pins them; let go and it tears out. With someone held, H is still the crush. Protocol 90. Not tested in game.

## 3.1.4 (HexGhoul): Expand personality and mood dialogue (testing)

See `.github/release-notes-3.1.4.md`. Over 730 additional spoken lines for Touka, Eto, Ken and every human temperament, retaining the original dialogue and relationship rules. Not tested in game.

## 3.1.3 (HexGhoul): Show every quinque attack in first person (testing)

See `.github/release-notes-3.1.3.md`. Dedicated first-person attacks for old and new quinques, with the full whip tendril, visible impact poses and direct swing tracking. Held quinques take M1 priority over eye-mode/final-form unarmed attacks. Not tested in game.

## 3.1.2 (HexGhoul): Add a rebindable briefcase key for every quinque (testing)

See `.github/release-notes-3.1.2.md`. Y (rebindable in Controls) folds the held quinque into its briefcase. Both right-click abilities remain available. Applies to old and new quinques. Not tested in game.

## 3.1.1 (HexGhoul): Limit weapon carry fixes to heavy sword, hammer and whip (testing)

See `.github/release-notes-3.1.1.md`. The walking/carry fix targets only the heavy sword (Bone Greatsword), heavy hammer (Flesh Hammer) and Tendril Whip. Other quinques keep their existing poses. Includes the marriage and family pathfinding fixes. Not tested in game.

## 3.1.0 (HexGhoul): Investigators keep walking while carrying kagune quinques (testing)

See `.github/release-notes-3.1.0.md`. Moving investigators retain their walking legs and body motion with all four kagune-made quinques; the whip curls above the floor instead of lying flat, and the long spear clears the floor on a sprint. Not tested in game.

## 3.0.9 (HexGhoul): Family followers walk instead of teleporting (testing)

See `.github/release-notes-3.0.9.md`. Spouses and children walk to catch up, including beyond 24 blocks; clinging no longer snaps to the player. Home recovery ignores followers and fighters and counts only block collisions. Not tested in game.

## 3.0.8 (HexGhoul): Jealousy preserves existing marriages (testing)

See `.github/release-notes-3.0.8.md`. Marrying another companion by command keeps the previous spouse married. Jealousy is temporary and no longer drains affection. A surviving wedding record can recover a marriage erased by the old command when its companion is unclaimed. Not tested in game.

## 3.0.7 (HexGhoul): Recorded insect, swing and flesh sounds; finger crack leads the mix (testing)

See `.github/release-notes-3.0.7.md`. Centipede legs use real insect recordings, both Rinkaku half-kakuja transformations are quiet hisses, and kagune swings use recorded air swishes. The synthetic flesh-impact, blood-spray and gush pools are replaced with recorded melon foley; stab samples stay the same. The owner's finger-crack recording is unchanged and plays at full gain, with every other pack sound mixed lower. All new recordings are CC0 and credited in the pack. Protocol 88, unchanged. Not tested in game.

## 3.0.6 (HexGhoul): The kagune-made weapons are real items (testing)

See `.github/release-notes-3.0.6.md`. The four kagune-made weapons are real items now. The Tendril Whip, Crystal Spear, Flesh Hammer and Bone Greatsword each have their own case and weapon item: given by /give, in the Combat tab, and dropped by the investigators who carry them (their own case now, not the regular quinque case; a masterwork's stays a masterwork). Right click the case and the weapon tears out of it in your hand (drawn from the same mesh the investigators use). M1 runs its whole string of blows with the investigators' animations, right click is its first move and Shift + right click its second (Bind/Flay, Crystal Javelin/Piercing Charge, Quake/Whirlwind, Leaping Cut/Cleave), each on a cooldown. Put it in another slot and it folds back into its case. Investigators carry the new case at peace too.

## 3.0.5 (HexGhoul): Rinkaku's tails stop shaking while holding someone (testing)

See `.github/release-notes-3.0.5.md`. Rinkaku's tails stop shaking while one of them holds someone. Not tested in game. This is the cause found in the code, so tell me if any shaking is left.

## 3.0.4 (HexGhoul): Eto is in the library you find (testing)

See `.github/release-notes-3.0.4.md`. Eto is in the library you find. A world has three libraries (placed in a ring, like strongholds), but only one Eto, and whichever library loaded first claimed her, often one a player only passed within range of without ever seeing it. The library they then found removed its own Eto on her first tick. Not tested in game.

## 3.0.3 (HexGhoul): No pause at the start of the finger-crack pull-out (testing)

See `.github/release-notes-3.0.3.md`. The pause at the start of the finger-crack pull-out is gone: the arm moves the moment you press the key, and the kagune comes out about a quarter of a second sooner. Not tested in game.

## 3.0.2 (HexGhoul): Investigators' case drops scaled by how common the case is (testing)

See `.github/release-notes-3.0.2.md`. Investigators' case drops now follow how common the case is: the commoner the weapon an investigator fought with, the likelier its case drops. It used to be a flat 2.5% for every one of them. Not tested in game.

## 3.0.1 (HexGhoul): CCG weapons re-animated (footwork, no folding) and the greatsword, spear and hammer remodelled (testing)

See `.github/release-notes-3.0.1.md`. The CCG weapons, redone after 3.0.0 shipped with animations that folded the body at the hips and a greatsword that looked like a strip of paper. Every blow and move of all four weapons is re-animated, and the sword, spear and hammer have new models. Not tested in game.

## 3.0.0 (HexGhoul): CCG rework: kagune-made weapons for the higher ranks (experimental) (testing)

See `.github/release-notes-3.0.0.md`. Experimental. The CCG rework: investigators of Rank 4 and up may carry a weapon grown from a kagune (a tendril whip, a crystal spear, a flesh hammer, a bone greatsword), each with a long M1 string and two moves of its own that they use whenever they see fit. Not tested in game. Install matching versions on client and server (network protocol 87).

## 2.5.7 (HexGhoul): Another baby on the sweet page, five days apart; the night comes at home (testing)

See `.github/release-notes-2.5.7.md`. Another baby: asked for on the sweet page now, in each companion's own words, five Minecraft days after the last was born; and at the home you share, the night you asked for actually comes (she comes to you for it, as she did before you had a home).

## 2.5.6 (HexGhoul): Lag check: children's threat search and the kagune passes' scans throttled (testing)

See `.github/release-notes-2.5.6.md`. Lag check. Two hot spots found, both from recent versions, both fixed: children looked for monsters with a line-of-sight ray to every monster within 10 blocks on every tick, and the kagune passes scanned for nearby children and wild ghouls up to six times a frame. Everything else that runs every tick or every frame was checked and is already throttled.

## 2.5.5 (HexGhoul): A child's Rinkaku is drawn whenever it is out (testing)

See `.github/release-notes-2.5.5.md`. A child's Rinkaku (and its wings) is now drawn whenever it is out, whether or not the child's body was drawn the usual way that frame. Before, the tentacle was only drawn for a child its body's renderer had noted during the same frame, so anything that skips or replaces that call (an entity-culling or performance mod, a body drawn some other way) left the kagune invisible even while the child fought with it. This is the fix the wild ghouls' tails got in 2.0.27, which children never had.

## 2.5.4 (HexGhoul): Children fight monsters that come close in their sight; ghoul children's kakugan at night (testing)

See `.github/release-notes-2.5.4.md`. Your sons and daughters stand their ground now: any child old enough to be taught (child, teen or grown) goes for a monster that comes close and that it can see, taught or not (lessons still make its blows harder); babies and toddlers still run to a parent. And at night a ghoul child's eyes turn to kakugan (the eye or eyes it was born with), not only while it fights.

## 2.5.3 (HexGhoul): The finger crack: the owner's sound; the arm stretches out, then bends, smoothly (testing)

See `.github/release-notes-2.5.3.md`. The finger crack uses the owner's own sound now, and the arm stretches out straight first, then bends at the elbow (the hand curling up toward the chest) before the crack, all of it eased, never a jump; it straightens and lowers together afterwards.

## 2.5.2 (HexGhoul): No hearts when the kagune or a kakuja comes out (testing)

See `.github/release-notes-2.5.2.md`. No more hearts when the kagune or a kakuja comes out. They were vanilla's damage-indicator particles (dark red hearts) that the original mod's fallback effects drew as "blood" and "mist" where the tails root at your lower back, on every limb tearing out and every landing impact; and a companion partner seeing your kagune also puffed hearts between you (behind you when they follow you). Rinkaku now throws the mod's own blood out of the back as each limb tears out.

## 2.5.1 (HexGhoul): The finger crack flows: eased up and down, never a snap; a cleaner crack sound (testing)

See `.github/release-notes-2.5.1.md`. The finger crack is now one flowing movement: the arm eases up over almost a second, the thumb presses into the finger, the finger cracks (a small flick of the hand, the head tipping with it), a moment held, then the arm eases back down to rest. On a player it starts and ends exactly where their body already was, instead of jumping to the pose. The crack sound is replaced with a cleaner, shorter knuckle-style crack.

## 2.5.0 (HexGhoul): Final sweep: a new world never inherits the last one's timers; favours survive death; protocol 86 (testing)

See `.github/release-notes-2.5.0.md`. Final sweep for multiplayer and single player. Quitting a world and opening another in the same game session no longer carries over the last world's timers (ability cooldowns, kagune locks, "presumed dead", a companion's recall home, villagers coming up to you, scene and talk cooldowns, a Thy Death or finger crack under way), which could otherwise lock things in the new world for hours. A villager's favour now survives your death, letting go of Z while disconnecting no longer errors, and the network protocol is bumped so a client and server on different builds are refused cleanly.

## 2.4.9 (HexGhoul): Spouse cooking: a furnace needed, in front of it, a full routine; the food to the Food chest, handed to you at home, sometimes for a kiss (testing)

See `.github/release-notes-2.4.9.md`. The spouse's cooking is redone. A furnace or smoker at home is now needed (a campfire, table or chest no longer counts). At the stove the spouse stands right in front of it (the way it faces) and goes through a whole routine: feeding the fire, stirring, seasoning, stirring, tasting off the spoon, stirring. When the food is done they carry it to the chest they labeled "Food" when sorting. If you're inside the home's grounds, they walk up and hand you your meal (holding it out), and sometimes, depending on their mood, ask for a kiss for it.

## 2.4.8 (HexGhoul): Ken's one-arm finger crack with a real bone crack; Ken's pull-out for kakuja users; no regeneration hearts (testing)

See `.github/release-notes-2.4.8.md`. Ken's finger crack now puts one arm out (the right, held out low with the elbow bent; the left hangs at his side) and is heard: a real bone crack fetched from the web (Oculus Audio Pack 01, CC BY 4.0, credited in the pack). A player who has learned a kakuja (Centipede, Full Kakuja, Dragon or Final Kakuja) now brings their kagune out his way: the same one-arm crack, then the kagune tears out of their back with blood thrown out behind and up, a ripping crunch, and everything close shoved away. The heart particles that puffed behind the player when Rinkaku came out are gone.

## 2.4.7 (HexGhoul): villagers' favours: rare mini quests, more village talk (testing)

See `.github/release-notes-2.4.7.md`. Village humans now ask for favours: rare mini quests that come up through the alert banner, raise their regard and pay out. More village talk too.

## 2.4.6 (HexGhoul): Ukaku's Vortex: a slow, massive rolling blow (testing)

See `.github/release-notes-2.4.6.md`. Ukaku's Vortex reworked to match the reference: a slow, massive, twisting blow instead of a fast horizontal tornado. The wing still leaves the shoulder blade, but now it rears up and comes down ahead, then rolls on along the ground in great upright loops, like the black mass in the reference, curling over each loop's top. The loops grow in as it lengthens, so it starts as a rising arch.

## 2.4.5 (HexGhoul): Rinkaku's Thy Death: every tail hunts on its own (testing)

See `.github/release-notes-2.4.5.md`. Rinkaku's Rapid Strike is now **Thy Death**. Hold Z and every tail you have out hunts on its own.

## 2.4.4 (HexGhoul): better reactions to War and Peace (testing)

See `.github/release-notes-2.4.4.md`. Better reactions to the final forms, War and Peace.

## 2.4.3 (HexGhoul): NPCs swim like players, never stuck in water (testing)

See `.github/release-notes-2.4.3.md`. NPCs swim like players now and don't get stuck in water. This covers every NPC in the mod: companions, children, villagers, wild ghouls, investigators and thralls. Before, they only bobbed at the surface, crawled at the game's slow mob pace, and nosed at the bank without getting out.

## 2.4.2 (HexGhoul): Not That Easily: everything thinks you're dead (testing)

See `.github/release-notes-2.4.2.md`. "You Thought You'd Get Rid of Me That Easily?" now makes everything think you're dead. That covers every path's version: Rinkaku's passive, Ukaku's, Graft's and Pale's mutations. Vanilla mobs already forgot you, but this mod's own fighters kept their own minds and came straight back for you.

## 2.4.1 (HexGhoul): partners come to you far less; no false talk alerts (testing)

See `.github/release-notes-2.4.1.md`. Partners come to you far less, and the false "wants to talk to you" alert is gone. Spouses and partners were walking up to kiss, hug, cling and take your hand almost constantly. The biggest cause was a 1-in-100 chance every tick of clinging to you whenever they stood near you in a soft mood, which made it every few seconds.

## 2.4.0 (HexGhoul): final sweep: health kept across logins, children's halves only added (testing)

See `.github/release-notes-2.4.0.md`. The final sweep after 2.3.2 to 2.3.9. All 34 test suites pass (millions of checks), the Minecraft API check passes, and two problems that would have shown up in play are fixed.

## 2.3.9 (HexGhoul): an unwanted kiss let go, never stuck (testing)

See `.github/release-notes-2.3.9.md`. Fix: a spouse could get stuck leaning in for a kiss you didn't want. She now never stays leaning in for long, and turning down a kiss she came asking for is no offence.

## 2.3.8 (HexGhoul): a child can always be sent home (testing)

See `.github/release-notes-2.3.8.md`. Fix: there was no button to send a child home. "Go home." only showed while the child was out away from home, never while it followed you around nearby. Now it's on the page whenever the child can walk.

## 2.3.7 (HexGhoul): bubbles steady in bed; children's kagune shown, both parents' halves (testing)

See `.github/release-notes-2.3.7.md`. Fixes: bubbles jumping over NPCs in bed, and children's kagune that couldn't be seen. Children now also have both parents' halves of the kagune.

## 2.3.6 (HexGhoul): endgame ghouls far tougher: more health, more resistance (testing)

See `.github/release-notes-2.3.6.md`. Endgame ghouls are far tougher. Before, a player with mastery 1000, every quest done and 100+ ghoul meat eaten still had only 20 health and took about 46% of every blow with the kagune out, so a Rank 7 investigator could nearly one-shot them. Now every factor counts for more, the quests count too, and health grows.

## 2.3.5 (HexGhoul): mood kisses: each companion's own, the face kept clear (testing)

See `.github/release-notes-2.3.5.md`. The mood kisses, redone: each companion has their own, and no kiss closes the arms in front of the face any more. Before, every kiss was the same pose on all three, and some brought both arms up crossed or together in front of her face. That's exactly where your face and first-person camera are, so it would have blocked your view.

## 2.3.4 (HexGhoul): human partners: a real kiss, no fear, a ring (testing)

See `.github/release-notes-2.3.4.md`. Village human partners now get the full romance the companions have.

## 2.3.3 (HexGhoul): the male chest stays in every form (testing)

See `.github/release-notes-2.3.3.md`. Fix: a male player's chest vanished whenever he transformed. The build was switched off in every form (Ukaku's kakuja, Graft's hide, War, Peace and the Dragon's turning), so the chest dropped back to the plain torso. It now stays in every form, and each form's torso armour is widened to fit over it instead: about 10% wider and 28% deeper, centred on the torso, so it covers the chest and the jacket layer.

## 2.3.2 (HexGhoul): Rinkaku strikes go round the right side again (testing)

See `.github/release-notes-2.3.2.md`. Fix: Rinkaku strikes came round the wrong side. A strike from your right tail swept round through your left side, and the other way round, in every Rinkaku form. 2.1.6 changed which side a tail is "on" to stop tails swinging across on their way back. That side also feeds the code that steers a strike round your body, and that code's "+1" is your left even though it calls it right, so every strike passing you went round the opposite side. Strikes now go round the tail's own side again. The 2.1.6 fix for tails swinging across on their way back is kept.

## 2.3.1 (HexGhoul): the mood kisses rendered; possessive, giddy and tender reworked (testing)

See `.github/release-notes-2.3.1.md`. The mood kisses (2.2.4) are rendered for the first time, and three of them are reworked after seeing the renders. Possessive and tender looked almost the same (both arms straight out). Giddy's "hands clasped at the chest" just looked like arms hanging, because a mob's arms don't bend at the elbow.

## 2.3.0 (HexGhoul): grown daughters take their mother's height (testing)

See `.github/release-notes-2.3.0.md`. Grown daughters now take their mother's height, and each one varies a little. A daughter grows up to her mother's size: Eto's, Touka's, or a woman player's (the female body). She then comes out up to 4% taller or shorter than her mother, by her own genes, so sisters aren't all the same height. Before, every grown daughter was drawn at one fixed size.

## 2.2.9 (HexGhoul): investigators give mastery and rations; Dawn a rare prize (testing)

See `.github/release-notes-2.2.9.md`. CCG investigators now pay out properly. Every one you bring down as a ghoul gives mastery, and now and then one leaves a field ration: human food the cookbook's recipes use. The White Reaper's Dawn is now a rare prize instead of a sure drop.

## 2.2.8 (HexGhoul): quests complete all unlocks everything (testing)

See `.github/release-notes-2.2.8.md`. `/kagune quests complete all` now unlocks everything. Besides finishing every open quest, it opens every gate, including the ones a companion teaches: the Centipede, the Full Kakuja, the Dragon, Crystal Wings, Gatling, the first and final Ukaku kakujas, and every mutation's gate. Nobody sends you to a teacher any more.

## 2.2.7 (HexGhoul): a command to marry a companion outright (testing)

See `.github/release-notes-2.2.7.md`. New operator command: `/kagune eto marry`, `/kagune touka marry` and `/kagune ken marry` (with an optional player after it, like `max`). It marries you to that companion at once and fills everything about the relationship.

## 2.2.6 (HexGhoul): village humans come up to you (testing)

See `.github/release-notes-2.2.6.md`. Village humans now come up to you on their own. A villager who knows you will sometimes notice you (within 12 blocks, in sight), walk over and say something. Why they come, how often, and what they say depend on their temper (all 12) and how close you are. Partners come most. Strangers never do.

## 2.2.5 (HexGhoul): a male player's chest a little smaller (testing)

See `.github/release-notes-2.2.5.md`. A male player's chest is a little smaller. The upper chest no longer stands out as far: it's broader than the waist, as before, but only 60% as far out past the torso as the White Reaper's, across the shoulders, in front and behind. The arms hang a little closer in to match. The White Reaper keeps his build exactly as it was.

## 2.2.4 (HexGhoul): a kiss for every mood (testing)

See `.github/release-notes-2.2.4.md`. A kiss for every mood. It works the same as before: they lean in and wait, a tap of Shift is a quick kiss, holding makes them blush, and holding longer makes them pull you in. Now each mood gives its own kiss: how they wait, how long, what they say, how they move, and what they do after. A kiss is never turned down any more. Ask an angry partner for one and you get an angry one.

## 2.2.3 (HexGhoul): a partner's mood shapes what they do (testing)

See `.github/release-notes-2.2.3.md`. A partner's mood now shapes what they do, not just what they say. Mad or jealous, they never come up asking for a kiss, and they turn yours down. Lonely, affectionate or clingy, they come for kisses far more often. Sleepy or starving, less. Once a sulk or a jealous spell has passed, they come to you on their own to make it up.

## 2.2.2 (HexGhoul): a grown half-human child's kakuja (testing)

See `.github/release-notes-2.2.2.md`. A ghoul's and a human's child, once grown (adult), uses its kagune's kakuja in a fight. A Rinkaku child grows two centipede legs out of its back beside its tail, which strike what's in reach by turns. An Ukaku child's wings become Ukaku's first kakuja, and its blows land harder. Other children still never have a kakuja.

## 2.2.1 (HexGhoul): a ghoul's and a human's child (testing)

See `.github/release-notes-2.2.1.md`. A child of a ghoul and a human (a human player and Eto, Touka or Ken) is now rarely carried to term. 7 in 10 such pregnancies are lost, somewhere in the first half. The one that lives is born a one-eyed ghoul, always, with the ghoul parent's kagune whole. It has 60% more health, 50% harder blows (fists and kagune) and 30% more speed than any other child, and it fights with the White Reaper's dashes. Its kakuja once grown (Ukaku's first kakuja, Rinkaku's centipede legs) comes in the next version.

## 2.2.0 (HexGhoul): tolerance, and sorry is always there (testing)

See `.github/release-notes-2.2.0.md`. Companions are far more tolerant, and you can always say sorry. A first accidental hit or a missed kiss is brushed off with a word. Only more of them close together put your partner out, and then for 20 seconds to a minute, never the old 5 minutes. When they're put out, talking to them opens "I'm sorry." / "It was an accident. I swear." and that forgives you on the spot.

## 2.1.9 (HexGhoul): Peace and War break the nerve of whoever faces them (testing)

See `.github/release-notes-2.1.9.md`. Rinkaku's final kakuja (Peace or War) now breaks the nerve of whoever faces it. Doves of Rank 0 to 4 and the two weaker tiers of wild ghouls run from it, and keep running while it's in sight. Rank 5 to 7 Doves and the strongest wild ghouls stand and fight, but tremble the whole time, and every few seconds their nerve gives and they back off before coming again. Rank 8 and above (Special Class, the White Reaper) stand as before.

## 2.1.8 (HexGhoul): Ken asks for proof before a final kakuja (testing)

See `.github/release-notes-2.1.8.md`. Ken's final-kakuja lessons are harder. Before he teaches the Full Kakuja, the Dragon or Ukaku's final kakuja, he asks you to prove you're ready. The proof is set the moment you ask, tracked on the action bar, and the lesson is taught (or the trial lived through) once it's done.

## 2.1.7 (HexGhoul): the Reaper's dashes for Ukaku's final kakuja (testing)

See `.github/release-notes-2.1.7.md`. Ukaku's final kakuja gets the White Reaper's dashes: tap sprint twice to dash the way you're moving. You have 3 charges, the action bar shows "Dash 2/3" as one is spent, and one comes back every 5 seconds until you're at 3/3 again. No kakuja vanishes any more. Also, after each release, only the 5 newest releases and the 5 newest action runs are kept.

## 2.1.6 (HexGhoul): Rinkaku tails no longer swing across on their way back (testing)

See `.github/release-notes-2.1.6.md`. Fix: Rinkaku tails no longer swing across the back after an attack or when the kagune comes out. Each tail's rest spot was on the opposite side from where the original's own motion carries that tail. So every time a tail eased back into rest (after a blow, when transforming), it crossed over to the other side, which looked like the tails rotating or swapping. Once it settled the picture looked right, because the rest shape is symmetric. The kagune's model, shape and rest position are unchanged. Each tail now rests on its own side.

## 2.1.5 (HexGhoul): courtships out of the mod's own world (testing)

See `.github/release-notes-2.1.5.md`. The companions' courtships now come from the mod's own world, not generic chores. The CCG's Doves (some of a given rank), wild ghouls of the Pack, a ghoul preying on a human village, a wild ghoul made your ally, village humans who come to care for you or tell you a secret, a walk through a human village at night, a drink from Touka's cookbook, ghoul meat. Every chapter's scene is rewritten to match.

## 2.1.4 (HexGhoul): romance for every personality (testing)

See `.github/release-notes-2.1.4.md`. Romance with village humans now has words for every personality. You can flirt with anyone (strangers, friends and close friends each answer differently). A partner gets conversations of their own. Kisses, hugs and "I love you" have more variety.

## 2.1.3 (HexGhoul): every villager is someone (testing)

See `.github/release-notes-2.1.3.md`. Every village human is now someone of their own. Each has a trade, a family, a pastime, an odd habit, a fear, a dream, a secret, a favourite gift and a view of ghouls, all fixed for life, and says them in their personality's voice. A new "Talk a while." page lets you ask about all of it. Getting to know them warms them to you, and their trade decides what they sell.

## 2.1.2 (HexGhoul): each companion's own courtship, and dates (testing)

See `.github/release-notes-2.1.2.md`. Eto, Touka and Ken no longer share the same three errands (three books, ten kills at night, a flower). Each now has their own five-chapter courtship. Every chapter asks for something only that companion would ask for, and closes with a written scene that has choices, played the moment the task is done. Once you're together, you can take them on dates.

## 2.1.1 (HexGhoul): final forms rest five minutes: Exhaustion (testing)

See `.github/release-notes-2.1.1.md`. The final forms (War and Peace) now have a 5-minute cooldown, shown as a new custom effect, Exhaustion, with its own icon: a kakugan half shut under a heavy, cracked lid, its red dimmed, a drop of RC running from it. Once a final form is let go (taken off, run dry, or the player killed in it), Exhaustion runs for 5 minutes, and calling War or Peace meanwhile is refused with the time left ("Exhausted: War can't be called again for 4:12").

## 2.1.0 (HexGhoul): kakuja legs and mask in the kagune's colours (testing)

See `.github/release-notes-2.1.0.md`. A Rinkaku kakuja's centipede legs and mask now take on the kagune's colour, blending with it while staying distinct. Before, both were a fixed plum. The centipede's segments are always greys, black and white: charcoal plates with pale grey and white rims, or bone-white plates for a kagune too dark to stand apart from them. They carry only a breath of the kagune's hue. The glowing flesh between the plates is the kagune's own colour. The mask has the same grey shell leaning a little more toward the kagune's colour, with the kagune's dark in its edges. The red kakugan lens stays. From this version on, the last number never reaches 10 (after x.y.9 comes x.(y+1).0), the owner's rule.

## 2.0.51 (HexGhoul): Rinkaku tails fixed and alive (testing)

See `.github/release-notes-2.0.51.md`. The Rinkaku tails are fixed and alive. The texture breakage the owner showed in game is gone. At rest the tails take the shape from the showcase render instead of the original chain's coiled, twitching rest. They stretch and draw in, swells run along them, their middles breathe, and the veins are fine and realistic.

## 2.0.50 (HexGhoul): Rinkaku scales, leaf shape and more glow (testing)

See `.github/release-notes-2.0.50.md`. The Rinkaku tails are reworked as asked: realistic scales, thin where they leave the body, and more glowing veins.

## 2.0.49 (HexGhoul): the White Reaper fights as a tactician (testing)

See `.github/release-notes-2.0.49.md`. The White Reaper fights as a tactician. Each fight he reads his opponent and picks a play ahead of the moves every investigator has: a backflip launched as Up We Go launches one, firing the volley from mid-air; closing in on openings; vaulting over a guard; shooting a target out of the air.

## 2.0.48 (HexGhoul): the White Reaper's five-ball volley (testing)

See `.github/release-notes-2.0.48.md`. The White Reaper can fire a five-ball volley from Dawn. All five lightning balls leave its point together, open out into a ring around the line he aimed along (1.6 blocks across, 1.0 up and down, one ball at the top and the rest evenly round), then close in on the spot he aimed at from every side at once: a pincer. Standing still gets you caught, while a good step aside slips it. Past that spot they cross and spread again. Each ball carries 40% of a single shot, so all five on one target is twice a single ball. From 7 blocks or more, half his shots are the volley.

## 2.0.47 (HexGhoul): Dawn bigger and smoother (testing)

See `.github/release-notes-2.0.47.md`. Dawn, the White Reaper's lance, is bigger and smoother. It's drawn about 20% bigger in the hand (scale 0.74 to 0.9). Its spike is half as thick again all along and meets the cone guard without a step; before, it read as a needle beside the guard. The spike and grip are 16-sided (were 8) in finer steps. Same silhouette as before: a black lance with a flared, gold-banded guard, a gun-sword style grip with its hooked knuckle guard, and a spiked pommel.

## 2.0.46 (HexGhoul): male players get the White Reaper's build (testing)

See `.github/release-notes-2.0.46.md`. Players who pick male on the gender screen now have the White Reaper's build. The upper torso is drawn a little bigger over their own: broader across the shoulders and chest, the chest standing out in front of the belly, tapering into the normal waist, in their own skin and jacket layer. The arms hang slightly further out to clear it. Players who haven't picked a gender look as before, and female players keep their figure.

## 2.0.45 (HexGhoul): the White Reaper's build (testing)

See `.github/release-notes-2.0.45.md`. The White Reaper has a heavier build. His upper torso is a little bigger than the part that meets his legs: broader across the shoulders and chest, with the chest standing about a pixel out in front of his belly and a touch fuller at the back, tapering into his normal waist (a V-shape). No spheres or sculpted muscle: it's his own torso drawn again a little larger, in his own skin and coat layer. His arms hang slightly further out to clear it.

## 2.0.44 (HexGhoul): the Reaper's pocket hand on a straight arm (testing)

See `.github/release-notes-2.0.44.md`. Fix for 2.0.43: the White Reaper's free hand now actually goes into his pocket in game. Investigators are drawn on vanilla's player model, which doesn't bend elbows or knees, but 2.0.43's pocket arm relied on a bent elbow, so in game it would have hung off at an angle. The arm is now solved for a straight arm: angled in to his hip, the hand sunk into the pocket at its front.

## 2.0.43 (HexGhoul): the White Reaper at ease (testing)

See `.github/release-notes-2.0.43.md`. The White Reaper has his own idle when he stands around with his case shut, in place of the regular investigators' one. He leans well back from the hips with his hip cocked and his weight sunk on the leg that holds the case. His other leg is loose, out in front and turned out. His chest is turned open, his head cocked with his chin a touch up so he looks down at you, and his free hand is in his coat pocket. It's a pose: only a slow breath, and no glancing about.

## 2.0.42 (HexGhoul): Rinkaku Blocking with perfect blocks (testing)

See `.github/release-notes-2.0.42.md`. Rinkaku has a new move, Blocking, on the right click: hold it with the tails out and they swing up off your back, round your shoulders and cross in front of you (an X per pair, one pair above the next), while you raise your forearms over your face in a ready stance. Every blow from in front or the sides is stopped completely. Too many hits break the guard: the tails are thrown back, you are knocked back and the block goes on a 5 s cooldown. A perfect block (the blow lands within 3 ticks, 0.15 s, of pressing) causes an impact, gives you no knockback and staggers the attacker for 1.2 s: they are stunned and pushed off, and can't hurt anything, so you get a free hit.

## 2.0.41 (HexGhoul): Rinkaku tails' living material (testing)

See `.github/release-notes-2.0.41.md`. Rinkaku tails now have a living material at the level of Ukaku's wings: glowing veins running through the flesh with a pulse of RC flowing from the body to the point, a glossy rim toward the silhouette, a wet glint where the light catches them, a slightly clear skin seen edge-on (the veins on the far side show faintly through), a hotter, glowing root where they leave the back and darker points. All in the player's kagune colour.

## 2.0.40 (HexGhoul): where the Rinkaku tails tear out (testing)

See `.github/release-notes-2.0.40.md`. While a Rinkaku's tails are out, the skin of the lower back shows where they tear out, as in the owner's reference: a glowing opening with a torn rim and red veins branching out from it across the back, in the kagune's own colour.

## 2.0.39 (HexGhoul): Rinkaku tails as in the reference (testing)

See `.github/release-notes-2.0.39.md`. Rinkaku tails are drawn as the owner's reference picture has them: broad, flat ribbons that swell a little past the back and taper to a point, bright at the middle of each face and deep at the edges with darker smudges along them, a light stripe running along each face near one edge, and thin chevrons all over them, black and light, pointing back toward the body. Every colour follows the player's own kagune colour.

## 2.0.38 (HexGhoul): Rinkaku tails grow out of the back (testing)

See `.github/release-notes-2.0.38.md`. Rinkaku tails now grow out of the body: each tail's root is sunk inside the lower back (its start hidden in the body) and its first 0.4 blocks leave the back as a rigid base, fanned out to its own side, before it blends into the motion it always had. Before, every tail started at a point hanging just behind the hips and fell from there like a rope.

## 2.0.37 (HexGhoul): Rinkaku's guard removed (testing)

See `.github/release-notes-2.0.37.md`. Rinkaku's C move, the tendril guard, is gone: C does nothing on the Rinkaku path any more and the guard is no longer listed in the ability guide. Rinkaku's defence becomes its right click (the block, in a coming version).

## 2.0.36 (HexGhoul): four new villager tempers (testing)

See `.github/release-notes-2.0.36.md`. Villagers have four new tempers, for twelve in all: Playful (teasing, games and pranks), Stoic (calm, few words, steady), Sarcastic (dry wit), and Romantic (a hopeless romantic, flowery and sincere). Each has its own lines for everything a villager says and its own body language.

## 2.0.35 (HexGhoul): who villagers fall for (testing)

See `.github/release-notes-2.0.35.md`. Every villager, man or woman, now has their own idea of who they fall for and what they look for in a partner. You can romance male and female villagers alike, as long as you're who they fall for; ask "What's your type?" to find out.

## 2.0.34 (HexGhoul): village women's bodies (testing)

See `.github/release-notes-2.0.34.md`. Village women now have the female body the game already gives its CCG women and grown daughters: hips and thighs shaped, a wide-painted skin's arms drawn slim, the bust in her own skin (and its second layer), bouncing as she moves, drawn a little shorter and slimmer than the men.

## 2.0.33 (HexGhoul): the White Reaper's ground tendril (testing)

See `.github/release-notes-2.0.33.md`. The White Reaper has a new move only he uses: he raises Dawn and drives it point first into the ground, the spot where his prey stood is marked on the ground for 0.8 s, then a huge black tendril bursts up out of it. Step off the mark in time and it misses.

## 2.0.32 (HexGhoul): Dawn's lightning in a player's hand (testing)

See `.github/release-notes-2.0.32.md`. A player holding Dawn fires its yellow lightning ball with right click: the point opens, the ball gathers there for a quarter of a second while the lance comes up onto your aim, then it flies and explodes on impact with the same lightning effects as the White Reaper's. Shift + right click folds Dawn back into its case.

## 2.0.31 (HexGhoul): Dawn's own M1s (testing)

See `.github/release-notes-2.0.31.md`. Dawn has its own three M1s, made for a three-block lance held in one hand at the hip: a wide level sweep, a rising backhand, and a lunging thrust as the finisher. Before, it borrowed the katana's two-handed chops, and every swing ended with its point in the ground. The White Reaper and any player holding Dawn swing it the same way.

## 2.0.30 (HexGhoul): Dawn smoothed (testing)

See `.github/release-notes-2.0.30.md`. Dawn is round where it was square: its guard is a 16-sided cone in fine steps (it was an 8-sided one in 12 big steps), and its spike, grip and butt are 8-sided along smooth profiles, with the spike's two ridges turning about it along its thicker middle.

## 2.0.29 (HexGhoul): /kagune reaper, and the Reaper turns up once in a while (testing)

See `.github/release-notes-2.0.29.md`. `/kagune reaper` (operators) puts the White Reaper five blocks ahead of you for testing; in the wild he now turns up about once in a hundred in-game days instead of about once in 660.

## 2.0.28 (HexGhoul): the White Reaper's own skin (testing)

See `.github/release-notes-2.0.28.md`. The White Reaper always wears the owner's own skin, on the slim arms it is painted for; no other investigator can roll it.

## 2.0.27 (HexGhoul): wild ghouls' kagune always drawn (testing)

See `.github/release-notes-2.0.27.md`. Wild ghouls' kagune no longer depends on their body's draw call being noticed: Rinkaku tails and Ukaku wings are drawn for every living wild ghoul with its kagune out within 48 blocks, and one ghoul failing to draw never hides the others.

## 2.0.26 (HexGhoul): White Reaper human-meat loot (testing)

See `.github/release-notes-2.0.26.md`. The White Reaper drops raw Human Meat on death using the existing human loot rule.

## 2.0.25 (HexGhoul): Dawn lightning (testing)

See `.github/release-notes-2.0.25.md`. The White Reaper now points Dawn at distant targets and fires an explosive yellow lightning ball.

## 2.0.24 (HexGhoul): Cap legacy investigator pairs at Rank 9 (testing)

See `.github/release-notes-2.0.24.md`. A pair loaded from an older save can include a former Rank 10. Its superior now caps correctly at Rank 9 and keeps the superior’s health and damage bonus. Protocol 75 is unchanged.

## 2.0.23 (HexGhoul): White Reaper front kick (testing)

See `.github/release-notes-2.0.23.md`. The White Reaper can mix a telegraphed front kick into Dawn’s melee strings.

## 2.0.22 (HexGhoul): Dawn and its black briefcase (testing)

See `.github/release-notes-2.0.22.md`. The White Reaper now carries Dawn, a black lance swung as a sword.

## 2.0.21 (HexGhoul): White Reaper encounter (testing)

See `.github/release-notes-2.0.21.md`. Rank 10 is now the White Reaper, a solo encounter with a single world-wide slot.

## 2.0.20 (HexGhoul): an investigator's superior is always the stronger (testing)

See `.github/release-notes-2.0.20.md`: a pair's leader ranks above its partner and has a superior's edge (more health, harder hits) on top.

## 2.0.19 (HexGhoul): fix the crash while the game loads (testing)

See `.github/release-notes-2.0.19.md`: the quinques' model frames are registered through Forge's reflection helper (the vanilla method is private; the launch did not make it public), and a failure there can no longer stop the game.

## 2.0.18 (HexGhoul): CCG agents dash in zigzags, more by rank (testing)

See `.github/release-notes-2.0.18.md`: chained dashes, each the other way, 1 at Rank 0 up to 6 at Rank 10, faster, likelier and sooner by rank.

## 2.0.17 (HexGhoul): CCG ranks 0 to 10, stronger by rank (testing)

See `.github/release-notes-2.0.17.md`: Rank 0 to 10, each with more health, speed, armour, harder hits and shorter rests; 8 to 10 rare and dangerous.

## 2.0.16 (HexGhoul): projectile trails in the kagune's colour (testing)

See `.github/release-notes-2.0.16.md`: every Ukaku particle (trails, the vortex's, impacts, bursts) and War's spike embers in the shooter's kagune colour.

## 2.0.15 (HexGhoul): Ukaku's shots no longer burn (testing)

See `.github/release-notes-2.0.15.md`: every Ukaku projectile (players', children's, wild ghouls') only hurts and pushes; fire moves that aren't shots are unchanged.

## 2.0.14 (HexGhoul): villagers sell seeds and farming goods too (testing)

See `.github/release-notes-2.0.14.md`: seven wares each: a staple, two foods, two farming goods (seeds, bone meal, saplings, cane, hay, a hoe, a bucket, a composter), two basic goods.

## 2.0.13 (HexGhoul): trading with village humans (testing)

See `.github/release-notes-2.0.13.md`: "Let's trade." on a villager's talk page; mostly the cookbook's non-meat ingredients plus basic goods, for the emeralds in your hand; they buy crops and plain goods.

## 2.0.12 (HexGhoul): clashes shake the screen (testing)

See `.github/release-notes-2.0.12.md`: every clash shakes the view hard (about 6 degrees, the hardest about 10), harder the nearer.

## 2.0.11 (HexGhoul): bigger, louder clashes; the weaker one flies (testing)

See `.github/release-notes-2.0.11.md`: the clash sound louder and layered, its wind bigger; the weaker side pushed further; a 2.5x gap sends it off like the Speed Kick, through blocks, down and stunned.

## 2.0.10 (HexGhoul): the release of 2.0.8 and 2.0.9's changes (testing)

See `.github/release-notes-2.0.10.md`: the build's sound check now allows the stove's crackle you approved; this jar carries the cooking and chest sorting.

## 2.0.9 (HexGhoul): your spouse sorts the house's chests by kind (testing)

See `.github/release-notes-2.0.9.md`: chests labelled Food, Blocks, Tools & Weapons, Armor, Ores & Minerals, Plants & Seeds, Mob Drops and Whatever; your own named chests left alone.

## 2.0.8 (HexGhoul): cooking at the stove takes its time, and a task is finished first (testing)

See `.github/release-notes-2.0.8.md`: three rounds of stirring at the stove with its crackle, no turning away, chores finished before her own errands.

## 2.0.7 (HexGhoul): no more teleporting home; out by day, home by night (testing)

See `.github/release-notes-2.0.7.md`: the spouse and children walk home instead of being put there; farther out by day, home by night; put home only when stuck.

## 2.0.6 (HexGhoul): children have a chance at one kagune or both (testing)

See `.github/release-notes-2.0.6.md`: parents of two kinds give 40% the mother's, 40% the father's, 20% both; why Touka and an Ukaku player only ever had Ukaku children.

## 2.0.5 (HexGhoul): other players and someone else's children, never dateable (testing)

See `.github/release-notes-2.0.5.md`: a real talk page for players who aren't the parents (by age and temper; babies and toddlers never coherent; gentle choices only), parents-only things kept for parents, children frightened by a stranger's kagune, and a test that no child is ever dateable.

## 2.0.4 (HexGhoul): babies after the first, and the village clears its ground (testing)

See `.github/release-notes-2.0.4.md`: the night for another child comes at the shared bed; the world's own ground over the village is cleared as it is placed.

## 2.0.3 (HexGhoul): children grow up; grown daughters get the woman's body (testing)

See `.github/release-notes-2.0.3.md`: a grown-up stage after the teen years (from day 16, protocol 73); grown daughters drawn with Touka's adult female body; no younger stage ever has it.

## 2.0.2 (HexGhoul): villages, Anteiku, the Cyber Tower and the library in many more biomes (testing)

See `.github/release-notes-2.0.2.md`: all four structures beyond plains (meadows, cherry groves, savannas, snowy plains, forests and taigas for the buildings, and Biomes O' Plenty's open biomes), a gentler land check for the village, villages a little closer together.

## 2.0.1 (HexGhoul): the crash fixed, the new village, on dry land (testing)

See `.github/release-notes-2.0.1.md`: the speech-bubble crash fixed (and bubbles can never crash the game), the right face on every talk screen, the new village from the owner's schematic (`hexkagune:village`, 26 villagers, 6 investigators) only on dry, flat land, investigator pairs about once a day.

## 2.0.0 (HexGhoul): Release 2.0

See `.github/release-notes-2.0.0.md`: the living tank (resistance, not health), the Brick Village with 16 villagers (talk, romance, gestures, dates), blinking, 136 new children's looks, CCG ranks, the bug/lag and animation passes.

## 1.3.83 (HexGhoul): animation pass: six broken poses fixed (testing)

See `.github/release-notes-1.3.83.md`.

## 1.3.82 (HexGhoul): bug and lag pass (testing)

See `.github/release-notes-1.3.82.md`.

## 1.3.81 (HexGhoul): twice the villagers, never two alike in a village (testing)

See `.github/release-notes-1.3.81.md`.

## 1.3.80 (HexGhoul): ghouls are living tanks: resistance, not health (testing)

See `.github/release-notes-1.3.80.md`.

## 1.3.79 (HexGhoul): 136 new looks for the children: other hair and eye colours (testing)

See `.github/release-notes-1.3.79.md`.

## 1.3.78 (HexGhoul): dates with a villager: walking hand in hand through the village (testing)

See `.github/release-notes-1.3.78.md`.

## 1.3.77 (HexGhoul): villagers' gestures: kisses, hugs, blushes and more (testing)

See `.github/release-notes-1.3.77.md`.

## 1.3.76 (HexGhoul): villagers and children blink (testing)

See `.github/release-notes-1.3.76.md`.

## 1.3.75 (HexGhoul): the villagers' own cute skins (testing)

See `.github/release-notes-1.3.75.md`.

## 1.3.74 (HexGhoul): romance with the villagers, and a ghoul must confess first (testing)

See `.github/release-notes-1.3.74.md`.

## 1.3.73 (HexGhoul): talk to the villagers, each with a name and a personality of their own (testing)

See `.github/release-notes-1.3.73.md`.

## 1.3.72 (HexGhoul): the Brick Village's people trade and chat in the streets (testing)

See `.github/release-notes-1.3.72.md`.

## 1.3.71 (HexGhoul): the Brick Village spawns in plains, with its people living in it (testing)

See `.github/release-notes-1.3.71.md`.

## 1.3.70 (HexGhoul): CCG ranks, they attack only what shows itself, and protecting humans comes first (testing)

See `.github/release-notes-1.3.70.md`.

## 1.3.69 (HexGhoul): the ghouls' arm blade, sharper and less cubic (testing)

See `.github/release-notes-1.3.69.md`.

## 1.3.68 (HexGhoul): wild ghouls prowl and hunt humans at night, and hide what they are by day (testing)

See `.github/release-notes-1.3.68.md`.

## 1.3.67 (HexGhoul): new, more dramatic runs for War and Peace (testing)

See `.github/release-notes-1.3.67.md`.

## 1.3.66 (HexGhoul): War's back tendrils come alive, like your other kagunes' tentacles (testing)

See `.github/release-notes-1.3.66.md`.

## 1.3.65 (HexGhoul): everyone fears War, and every final kakuja gets its own reactions (testing)

See `.github/release-notes-1.3.65.md`.

## 1.3.64 (HexGhoul): NPCs see War and Peace when they are worn, and the Rinkaku and Ukaku final kakujas count as final (testing)

See `.github/release-notes-1.3.64.md`.

## 1.3.63 (HexGhoul): the same big move meeting itself cancels both with a big impact (testing)

See `.github/release-notes-1.3.63.md`.

## 1.3.62 (HexGhoul): a clash is as big as the two blows were hard, and a hard one shakes the ground (testing)

See `.github/release-notes-1.3.62.md`.

## 1.3.61 (HexGhoul): the owner's new clash sounds on every clash (testing)

See `.github/release-notes-1.3.61.md`.

## 1.3.60 (HexGhoul): two fixes to family talks, found reviewing 1.3.53 to 1.3.59 (testing)

See `.github/release-notes-1.3.60.md`.

## 1.3.59 (HexGhoul): big brothers and sisters play with the baby (testing)

See `.github/release-notes-1.3.59.md`.

## 1.3.58 (HexGhoul): brothers and sisters play hide and seek (testing)

See `.github/release-notes-1.3.58.md`.

## 1.3.57 (HexGhoul): they act out what they say: gestures and poses with every line (testing)

See `.github/release-notes-1.3.57.md`.

## 1.3.56 (HexGhoul): Eto's child moves its body when it fights with her flesh (testing)

See `.github/release-notes-1.3.56.md`.

## 1.3.55 (HexGhoul): one escort for a toddler that wandered off (testing)

See `.github/release-notes-1.3.55.md`.

## 1.3.54 (HexGhoul): a game of tag stays in the yard (testing)

See `.github/release-notes-1.3.54.md`.

## 1.3.53 (HexGhoul): nobody walks away mid-conversation (testing)

See `.github/release-notes-1.3.53.md`.

## 1.3.52 (HexGhoul): speech bubbles take turns and keep out of each other's way (testing)

See `.github/release-notes-1.3.52.md`.

## 1.3.51 (HexGhoul): brothers and sisters: they talk, tease, scheme, squabble, play tag, mind the little ones and fight side by side (testing)

See `.github/release-notes-1.3.51.md`.

## 1.3.50 (HexGhoul): Eto's child fights with her flesh, and badly hurt its arm mutates (no kakuja) (testing)

See `.github/release-notes-1.3.50.md`.

## 1.3.49 (HexGhoul): parenting: the companion parent fetches a wandering child, stops a rampage, and scolds it, back and forth in their bubbles (testing)

See `.github/release-notes-1.3.49.md`.

## 1.3.48 (HexGhoul): children use their parents' whole kit: crystal wings and spikes, the tentacle's grab, throw and crush, back and front flips (never a kakuja) (testing)

See `.github/release-notes-1.3.48.md`.

## 1.3.47 (HexGhoul): children are as tough as their parents: they grow into the stronger parent's health and more, and heal on their own (testing)

See `.github/release-notes-1.3.47.md`.

## 1.3.46 (HexGhoul): a child of a Rinkaku and an Ukaku parent is born with one of three looks: over either shoulder, or a tail with both wings (testing)

See `.github/release-notes-1.3.46.md`.

## 1.3.45 (HexGhoul): a child's tentacle leans out to the right, its wing on the left: the back isn't crowded any more (testing)

See `.github/release-notes-1.3.45.md`.

## 1.3.44 (HexGhoul): fix: a Rinkaku child has one real Rinkaku tentacle, not two graft tails (testing)

See `.github/release-notes-1.3.44.md`.

## 1.3.43 (HexGhoul): children inherit their parents' kagune, part 1: Rinkaku tails and Ukaku wings, colour from one parent (testing)

See `.github/release-notes-1.3.43.md`.

## 1.3.42 (HexGhoul): Home Living, part 3: your partner cooks the cookbook's dishes at home (testing)

See `.github/release-notes-1.3.42.md`.

## 1.3.41 (HexGhoul): Home Living, part 2: Touka's cookbook (trade it for two cocoa beans) and its illustrated book (testing)

See `.github/release-notes-1.3.41.md`.

## 1.3.40 (HexGhoul): Home Living, part 1: the ghoul kitchen (66 foods with their own icons, recipes and effects), The Human, mutants, endless ghoul-meat growth, homes by biome (testing)

See `.github/release-notes-1.3.40.md`.

## 1.3.39 (HexGhoul): fixes: NPCs stay put while you talk to them; nobody suffocates in blocks while holding your hand; wild ghouls fight back in creative, so their kagune shows (testing)

See `.github/release-notes-1.3.39.md`.

## 1.3.38 (HexGhoul): real speech bubbles: white, rounded, outlined, with a tail; thought clouds for actions; a "z Z z" cloud over sleepers (testing)

See `.github/release-notes-1.3.38.md`.

## 1.3.37 (HexGhoul): a sleeping spouse or child only half wakes when you touch them, drowsy and in their own way; a parent gets up for a CCG investigator by the house too (testing)

See `.github/release-notes-1.3.37.md`.

## 1.3.36 (HexGhoul): a new kagune only wild ghouls have: the arm blade, a Koukaku that spirals down the arm into a blade, with its own wind-up, three-cut string, guard and sprint (testing)

See `.github/release-notes-1.3.36.md`.

## 1.3.35 (HexGhoul): random colours for every path: Ukaku's wings, their crystal forms and every shard in your kagune's colour; wild ghouls get random kagune colours too (testing)

See `.github/release-notes-1.3.35.md`.

## 1.3.34 (HexGhoul): every Rinkaku kagune gets a random colour of its own; /kagune color reroll for another (testing)

See `.github/release-notes-1.3.34.md`.

## 1.3.33 (HexGhoul): the crystal launcher rebuilt as a real gun with a rifle hold and twice the fire rate; both hands stay on two-handed quinques through every combo; new M1s for every quinque; a tap of right click folds a quinque back into its case; investigators dash left and right with brief afterimages (testing)

See `.github/release-notes-1.3.33.md`.

## 1.3.32 (HexGhoul): the airplane spins the right way on NPC parents; kids never hover after a moment; a Shift tap lets go of your kid's hand; the spouse never teleports around the house (testing)

See `.github/release-notes-1.3.32.md`.

## 1.3.31 (HexGhoul): a spouse who actually helps at home: cooks (and keeps you a meal), smelts, harvests and replants, guards the house and the kids; nobody greets or talks to you through walls; /kagune home (testing)

See `.github/release-notes-1.3.31.md`.

## 1.3.30 (HexGhoul): NPCs talk in speech bubbles over their heads (not the chat); the spouse keeps house instead of following you about; Touka leans in for a kiss; shut eyes show a line; a parent holds a baby the whole time (testing)

See `.github/release-notes-1.3.30.md`.

## 1.3.29 (HexGhoul): companions, children and allies come up to you with something to say (the Answer key); a spouse keeps house at home (chests sorted, the floor swept into a chest, flowers watered); "Go home, please."; quests only from whoever gives them, feats, hunts on request, a Romance tab; Touka her own way (testing)

See `.github/release-notes-1.3.29.md`.

## 1.3.28 (HexGhoul): investigators carry their case by the handle, fight as a pair (one in front, one round the back), idle at ease; the quinque case's own mechanical sound (testing)

See `.github/release-notes-1.3.28.md`.

## 1.3.27 (HexGhoul): CCG investigators: pairs in the owner's skins, fast and acrobatic, with quinques, clashes and parries; wild ghouls rarer; cases can't be crafted (testing)

See `.github/release-notes-1.3.27.md`.

## 1.3.26 (HexGhoul): Touka's wings show again; quinques reworked: a bigger mid-grip spear, a two-handed katana, a flesh mace, a crystal launcher, custom M1s (testing)

See `.github/release-notes-1.3.26.md`.

## 1.3.25 (HexGhoul): the CCG's quinques: briefcases that open into a Bikaku blade and a Koukaku spear (testing)

See `.github/release-notes-1.3.25.md`.

## 1.3.24 (HexGhoul): Touka lives at Anteiku, Ken on top of the tower; NPCs fight to the end; the airplane spin fixed (testing)

See `.github/release-notes-1.3.24.md`.

## 1.3.23 (HexGhoul): Ken's kakugan iris fixed (testing)

See `.github/release-notes-1.3.23.md`.

## 1.3.22 (HexGhoul): holding hands fixed: the hands actually meet now (testing)

See `.github/release-notes-1.3.22.md`.

## 1.3.21 (HexGhoul): a family home: beds at night, doors, hand in hand, tucked in, fed; kids die for good; companions never hit their kids; strong parents, strong kids (testing)

See `.github/release-notes-1.3.21.md`.

## 1.3.20 (HexGhoul): moments with your kids: hugs, head pats, high fives, tickles, peekaboo, tosses, airplane spins, shoulder rides (testing)

See `.github/release-notes-1.3.20.md`.

## 1.3.19 (HexGhoul): Ken fights in Peace, every move of it; Touka's crystal wings (testing)

See `.github/release-notes-1.3.19.md`.

## 1.3.18 (HexGhoul): the owner's skins for sons and daughters, with real kakugan; Touka's legs a touch slimmer (testing)

See `.github/release-notes-1.3.18.md`.

## 1.3.17 (HexGhoul): Touka reskinned (slim arms), curvier (testing)

See `.github/release-notes-1.3.17.md`.

## 1.3.16 (HexGhoul): carrying a child fixed, little ones on little proportions and bigger, /kagune children, NPC parents (testing)

See `.github/release-notes-1.3.16.md`.

## 1.3.15 (HexGhoul): crash fix (a child with no renderer), Touka a little skinnier, Ken's embrace loosened (testing)

See `.github/release-notes-1.3.15.md`.

## 1.3.14 (HexGhoul): Touka and Ken: companions who teach the wings and the kakujas; Ken's trial; your partner is never hurt by you (testing)

See `.github/release-notes-1.3.14.md`.

## 1.3.13 (HexGhoul): holding hands is alive: she trails as you pull her, swings out on turns, the hands sway (testing)

See `.github/release-notes-1.3.13.md`.

## 1.3.12 (HexGhoul): Eto takes your hand on her own, hugs, leans, hums, reads, dances; many more lines (testing)

See `.github/release-notes-1.3.12.md`.

## 1.3.11 (HexGhoul): NPCs react to your eyes, kagune, kakuja and the worst forms; strangers fear a kakuja (testing)

See `.github/release-notes-1.3.11.md`.

## 1.3.10 (HexGhoul): pregnancy and children: married to Eto, ask for a baby; two days later, "9 months later..." (testing)

See `.github/release-notes-1.3.10.md`. The child skins are made from CC BY-SA parts of the edit_skin mod (credits:
`src/main/resources/assets/hexkagune/textures/entity/child/CREDITS.txt`).

## 1.3.9 (HexGhoul): pick your gender after your eyes; female players get Eto's body and size (testing)

See `.github/release-notes-1.3.9.md`.

## 1.3.8 (HexGhoul): Eto's Owl kakuja, her last resort (testing)

See `.github/release-notes-1.3.8.md`.

## 1.3.7 (HexGhoul): Eto's Mutated Arm (testing)

See `.github/release-notes-1.3.7.md`.

## 1.3.6 (HexGhoul): Eto's Owl DNA flesh: the swipe, the grab and throw, the Flesh Hammer, Watch This (testing)

See `.github/release-notes-1.3.6.md`.

## 1.3.5 (HexGhoul): holding hands: the held hands stay still as you walk, arms stretched further apart (testing)

See `.github/release-notes-1.3.5.md`.

## 1.3.4 (HexGhoul): Eto fights with the Owl DNA kit (Speed Blitz, Big Punch into Witness This, Speed Kick), hand in hand for real (testing)

See `.github/release-notes-1.3.4.md`.

## 1.3.3 (HexGhoul): a living talk screen, holding hands with Eto, she clings to your arm, no fall damage, she always finds her way home (testing)

See `.github/release-notes-1.3.3.md`.

## 1.3.2 (HexGhoul): Eto reworked: one of her per world, talk by who you are to her, moods, marriage with a ring, her kagune in a fight (testing)

See `.github/release-notes-1.3.2.md`.

## 1.2.7 (HexGhoul): your transformation sounds on every form (kakujas, the Dragon, Centipede Dominion); 1.2.6: War and Peace Z/V fixed (testing)

See `.github/release-notes-1.2.7.md`.

## 1.2.5 (HexGhoul): the paths are Rinkaku and Ukaku, Furuta DNA no longer pickable, no character names anywhere in the mod (testing; saves reset)

See `.github/release-notes-1.2.5.md`.

## 1.2.5 (HexGhoul): the paths are Rinkaku and Ukaku, Furuta DNA no longer pickable, no character names anywhere in the mod (testing; saves reset)

See `.github/release-notes-1.2.5.md`.

## 1.2.4 (HexGhoul): War's first person cleaned up: vanilla's hand swinging on his blows, the rinkaku kept off the eyes and lined up with the look (testing)

See `.github/release-notes-1.2.4.md`.

## 1.2.3 (HexGhoul): Rebirth's flesh men are Steve-shaped (player head, body, arms and legs), same flesh and arm blades (testing)

See `.github/release-notes-1.2.3.md`.

## 1.2.2 (HexGhoul): every sound is the user's own pack (mono, balanced, spam-guarded), Peace visible in first person, the Final Horizon stays put (testing)

See `.github/release-notes-1.2.2.md`.

## 1.2.1 (HexGhoul): Rebirth Instinct's flesh creatures can no longer crash the game for want of a renderer (testing)

See `.github/release-notes-1.2.1.md`.

## 1.2.0 (HexGhoul): Ctrl + R brings the kagune out or puts it all away, R switches to kakugan mode without losing the eyes (testing)

See `.github/release-notes-1.2.0.md`. From 1.2.0 on, updates are published from the HexGhoul repository (versions 1.2.x, then 1.3.0 after 1.2.9).

## 1.0.43 (HexKagunesv2): breaking Kaneki's grab is a timed parry, swatting shards costs your hit (testing)

See `.github/release-notes-1.0.43.md`.

## 1.0.42 (HexKagunesv2): swat Ayato's non-crystal shards with an M1, Kaneki's grab is slower and breakable (testing)

See `.github/release-notes-1.0.42.md`.

## 1.0.41 (HexKagunesv2): Kaneki loses mutations for the revive passive, Ayato gets it too, wild ghouls get skins, flips, heavies and clashes (testing)

See `.github/release-notes-1.0.41.md`.

## 1.0.40 (HexKagunesv2): kagune clashes in PvP, Piercing Salvation points where you aim (testing)

See `.github/release-notes-1.0.40.md`.

## 1.0.39 (HexKagunesv2): Centipede Dominion steers where you look, body follows the head, steady chase camera (testing)

See `.github/release-notes-1.0.39.md`.

## 1.0.38 (HexKagunesv2): crash fix for Rebirth Instinct's flesh creatures (testing)

See `.github/release-notes-1.0.38.md`.

## 1.0.37 (HexKagunesv2): Piercing Salvation's blades fold into one smooth spear (testing)

See `.github/release-notes-1.0.37.md`.

## 1.0.36 (HexKagunesv2): ultimates fly forward-down, scripted moves stop shaking, Peace blade/Waltz/lance fixes, first person, lighter afterimages (testing)

See `.github/release-notes-1.0.36.md`.

## 1.0.35 (HexKagunesv2): dash-style sprint afterimages, per-path cooldowns, rinkaku auto-attack nerfed, War M1 uses the rinkaku (testing)

See `.github/release-notes-1.0.35.md`.

## 1.0.34 (HexKagunesv2): Peace reworked: Crossblade Waltz, Aegis of :Re, White Wing Ascent, Piercing Salvation, Rebirth Instinct, The Final Horizon (testing)

See `.github/release-notes-1.0.34.md`.

## 1.0.33 (HexKagunesv2): RC scales with mastery, creative is free, Black Sun reworked, Centipede Dominion drivable, War first person (testing)

See `.github/release-notes-1.0.33.md`.

## 1.0.32 (HexKagunesv2): the Dragon's legs walk with it and stand on the real ground, rounder body (testing)

See `.github/release-notes-1.0.32.md`.

## 1.0.31 (HexKagunesv2): the Dragon redone as a lore-accurate centipede: plates, legs, eyes, Furuta heads (testing)

See `.github/release-notes-1.0.31.md`.

## 1.0.30 (HexKagunesv2): the Dragon's eyes, body and heads, Centipede Dominion driven (testing)

See `.github/release-notes-1.0.30.md`.

## 1.0.29 (HexKagunesv2): War's held moves: Black Reaper drag and impale, King's Defiance cocoon, Spire Wave charge and centipede mass (testing)

See `.github/release-notes-1.0.29.md`.

## 1.0.28 (HexKagunesv2): playability fixes: War's rinkaku smooth, blitz and Black Sun indoors and on hills, no M1 mid-combo (testing)

See `.github/release-notes-1.0.28.md`.

## 1.0.27 (HexKagunesv2): smooth animation clock, War's spike cone, King's Execution blitz and Black Sun reworked (testing)

See `.github/release-notes-1.0.27.md`.

## 1.0.26 (HexKagunesv2): War and Peace runs, legs free of attacks, sprint speed and afterimages, first person (testing)

See `.github/release-notes-1.0.26.md`.

## 1.0.25 (HexKagunesv2): Peace's arms alive, strikes that whip and never go the same way twice (testing)

See `.github/release-notes-1.0.25.md`.

## 1.0.24 (HexKagunesv2): kagune tails and wings stay on the body through its animations (testing)

See `.github/release-notes-1.0.24.md`.

## 1.0.23 (HexKagunesv2): War and Peace run on RC and fall away when it runs out (testing)

See `.github/release-notes-1.0.23.md`.

## 1.0.22 (HexKagunesv2): the panel, hold K and R fixed for War and Peace (testing)

See `.github/release-notes-1.0.22.md`.

## 1.0.21 (HexKagunesv2): Peace with heft (testing)

See `.github/release-notes-1.0.21.md`.

## 1.0.20 (HexKagunesv2): combos, launchers, the heavy (testing)

See `.github/release-notes-1.0.20.md`.

## 1.0.19 (HexKagunesv2): detransforms, real hit effects (testing)

See `.github/release-notes-1.0.19.md`.

## 1.0.18 (HexKagunesv2): Peace's original model back, six blades, alive (testing)

See `.github/release-notes-1.0.18.md`.

## 1.0.17 (HexKagunesv2): War's cloth, the transformations (testing)

See `.github/release-notes-1.0.17.md`.

## 1.0.16 (HexKagunesv2): Peace playable, its blades reanimated

See `.github/release-notes-1.0.16.md`.

## 1.0.15 (HexKagunesv2): wild ghouls fight back, War fixes, first person

See `.github/release-notes-1.0.15.md`.

## 1.0.14 (HexKagunesv2): War's M1, six blows

In War, M1 is a chain of six blows (`WarStrike`, pure): jab, reap, side kick, rising slash, spinning heel and the King's Verdict.
They chain like Eto's blitz:
- a click during a blow is queued until its chain tick;
- a click within `CHAIN_WINDOW` after a blow carries the chain on;
- the Verdict has a short recovery.

The parts:
- **Client input:** `KingInput.attackInput` takes the attack key at HIGH priority while War is worn, so the eyes' blitz never also
  fires. It mines on a block unless a body is by the crosshair, and sends `C2SKingAction.STRIKE`.
- **Server:** `WarKit.strike` / `striking` handle lock-on (`validSwipeTarget`), step-in, the impact tick's hits, knockback
  (`KingEffects.steady`), stun, blade bleed, and the Verdict's area around the target.
- **Body:** `KingMove.STRIKE` with the blow as its level (`ticks(level)`). `WarPose.strike` keys a coil, a hit and a
  follow-through. `WarPose.strikes` hands a blow cut off by the next over `HANDOFF` ticks, through `KingClient.View.wasLevel` and
  `wasAt`. `WarModel.Motion.of(move, level, progress)` gives the rinkaku per blow.
- **Effects:** `S2CKingFx.SWING` (at the start of the blow; the arc is swung with the player, timed by `KingFxModel.sweep`) and
  `S2CKingFx.STRIKE` (at impact: contacts, `KingFxModel.impact` shocks, `KingFxModel.verdict`).

`KingFx` now draws glow quads with both windings: the glow type (eyes) culls back faces.

Protocol 35.

## 1.0.13 (HexKagunesv2): War's six abilities, rebuilt

War's kit is rewritten around the centipede (`WarRules`, `KingMove`, `WarKit`), with each move drawn by `KingFx` from the pure
builders in `KingFxModel` and sent as `S2CKingFx` events:
- **Z King's Execution:** five black centipedes (the Centipede Form's legs through `KingFxModel.black`) shoot from the left hand
  at the aimed target within 15 blocks. They coil, squeeze and crush it; bodies over `HOLD_LIMIT` are crushed where they stand.
- **J Black Reaper:** a clipped 10-block dash and a cleave in a forward arc. It bleeds what it cuts, and `KingHalves` draws what it
  kills (humanoid models other than players) as two halves sliding apart.
- **G Centipede Dominion:** `WarWyrm` runs a small Dragon for 140 ticks: `WYRM_SCALE` girth, ring spacing 0.32, steered toward the
  look. It is sent as `S2CDragonFrame` with `mini` set, so `DragonCamera` chases it from behind (`CHASE_BACK`, `CHASE_UP`). The
  sculptor gives a body this thin fewer, smaller eyes.
- **B Spire Wave:** a 20-block line of War spikes that follows the ground, then launches and pins.
- **C King's Defiance:** a 0.6 s parry that blinks behind the attacker (`connection.teleport`), counters, stuns and bleeds; if no hit
  comes, a 3 s cocoon that stores damage and bursts (`WarRules.burst`, capped at 60).
- **V Black Sun:** 30 blocks of slow and darkness (HUD vignette), spike rain on marked enemies with `WarRules.executes` (30%, max
  health up to 300), and a crescent to finish.

New API references: `RenderLivingEvent` getters, `LivingEntityRenderer.render` (`m_7392_`) and
`ServerGamePacketListenerImpl.teleport` (`m_9774_`), each with evidence in `verify/api-allowlist.txt`.

## 1.0.12 (HexKagunesv2): War's mask keeps its shape wherever the eye is

1.0.11 moved the mask around the eye: placed high, the mouth opening stretched up into a bare patch of face, a face plate hung
over the bare skin, and the forehead ring was pushed onto the eye. Now `WarModel.mask` keeps its own shape wherever the kakugan
is placed, and only the eye moves:
- The mouth opens no higher than usual, and lower when the eye sits low.
- Only the plates and cracks the eye would land on make way (`flatDistance`).
- The ring moves across the brow to the far side only when the eye is set right under it.

## 1.0.11 (HexKagunesv2): the Dragon comes out of him; the camera never goes black

The turning: `DragonSession.turn` holds the first `TurningModel.PRELUDE_TICKS` (9 s) before the rig starts. Kaneki stands where he
fell, whole and untouchable (as all through the Dragon), and the keys don't move him (`TurningClient.hold`). His heart pounds
faster and faster (vanilla warden heartbeat, by name through `SoundEvent.createVariableRangeEvent`). He screams on
`TurningModel.SCREAMS` (ghast scream, player hurt, the warden's roar at the last). `BURSTS` throw blood and darkness about,
knock everything near him off its feet and hurt it. The server repeats how far in he is (`S2CDragonEvent` `PRELUDE`, every half
second), so a player who comes into view midway still sees the rest.

On the client, `TurningClient` drives the turning:
- `WarPose.turning` poses the body from beat to beat: clutching his chest, flung back screaming as the kagune tears out of it,
  clawing at his head as it tears out of that, curled in, bursting open.
- `TurningModel` (drawn by `KingLayer`) grows the kagune: four segmented centipede tendrils out of his chest, then three out of
  his head; black centipede plates spreading from the wound with red cracks running ahead of them; a half mask; a segmented tail
  out of his lower back; and his kakugan.
- `DragonCamera.shootTurning` films it close from in front, circling and drawing back, and the eruption's shot takes over from
  there.

The blackouts, fixed: Minecraft paints the block (or water, or fire) round the player's own head over the whole screen, and the
player's body is held in the ground under the Dragon. While the Dragon's camera has the view,
`AwakeningClientEvents.noHeadInTheWall` cancels Forge's `RenderBlockScreenEffectEvent`. The shot itself now looks round and over
what blocks it (`DragonCamera.clearShot`: hills, the Dragon's own coils) and hurries to a clear view; a buried focus is brought up
to the surface first.

Also:
- The Dragon is Kaneki DNA's alone. `DragonSessions.start` refuses anyone else, even through the admin trigger, and
  `KingServer.awaken` gives the crown to no one else.
- The final kakuja cards are a page of the kagune menu now: the same organic board, veins, spaced title, rows, and a capsule bar
  held to choose. Choosing transforms you there and then.
- War's one eye sits right on the player's kakugan as they placed it (`WarModel.eyeOn`). On a one-eyed right placement the
  whole mask is its own mirror image.
- `/kagune quest complete all` (and `/kagune quests complete all`) finishes every quest on right now: the journal's open ones,
  tonight's hunts, the favours (`QuestServer.completeOpen`).

## 1.0.10 (HexKagunesv2): the final kakuja, K, the cards and the themed menu

Naming: Kaneki's Centipede Form and Full Kakuja are his **half kakujas**. War and Peace are the **final kakuja**
(`KingForm.title()`, the lang keys, the Dragon's closing title).

K (`AyatoInput.kakujaKey` client side, `AyatoForms.kanekiKey` server side):
- **Tap:** after the Dragon, a tap brings out the strongest half kakuja the player has. That is the Full Kakuja once its quest is
  done, otherwise the Centipede Form. A tap in a half kakuja goes back out.
- **Hold:** puts on the chosen final kakuja. It first leaves any half kakuja (`leaveKanekiKakuja`), then `KingServer.toggle`.
- **K while the final kakuja is worn:** takes it off. `C2SAyatoAction` lets K through under a final kakuja; only the Dragon blocks it.
- The Y key (`ControlKeys.KING`) is gone.

The choice: `KingChoiceView` / `KingChoiceScreen` show two cards, War and Peace. A click picks a card and holding the button (or
Enter) for 1.2 s chooses it, once and for good; the server keeps it. The cards come up on their own 5 s after the Dragon
(`KingClient.promptIn`), again at each login, and from the menu's final kakuja rows, until a card is chosen.

The menu: `KingTheme` recolours `KanekiMenuView` by the choice. War is pure black with dark red veins, Peace is white with
feathers and no text shadows. The menu shows a FINAL KAKUJA section and a K tap/hold legend.

Peace's model: each cross blade is held by its own blade arm (`PeaceModel.bladeArm`). A blade arm is a thicker tendril from the
upper back with white plates, ending in four white claws around the blade's heart. These arms will carry Peace's attacks. Every
band now ends in a point (`PeaceModel.point`). `KingTests` checks both.

## 1.0.8 (HexKagunesv2): Peace, first look

`control/king/PeaceModel` is Peace, Kaneki :Re, after `tools/kakuja/reference/peace.webp`, in the same smooth procedural style as
War. It has no mask, white and silver armour with horns, twisted crimson wings that sweep back, and four turning cross blades
per wing. `KingLayer` draws it and `WarPose.peace` poses it. It is not playable yet: the menu keeps it sealed, and operators
see it with `/kagune awakening preview <player> peace`.

## 1.0.7 (HexKagunesv2): War stacked

The rinkaku hunt on their own (`WarKit.instinct`: lashes near, homing RC spikes far, `S2CKingLash`). War also gets no
knockback, no fall damage, lifesteal and harder abilities, and stands in the fused Zamasu stance (`WarPose`).

## 1.0.6 (HexKagunesv2): War's abilities

While War is worn, the kagune's keys are its kit: Z King's Execution (charged), J Black Reaper (three dashes), G Centipede
Dominion (seize; G crush, crouch+G impale), B Thronebreaker (blades through the ground, `TerrainScars`), C King's Defiance
(counter) and V Final Judgment (storm, leap, the eye).
- **Rules:** `control/king/WarRules` holds the keys, cooldowns and numbers (tested). `control/king/KingMove` lists the body's
  moves.
- **Server:** `control.awakening.WarKit` (Mojang names, in the Dragon's package for its terrain and particles) is installed as
  `KingServer.Kit` at common setup.
- **Client:** `client/KingInput` reads the keys and `client/KingHud` draws the board. `WarPose.act` and `WarModel.Motion` animate
  every move.

## 1.0.5 (HexKagunesv2): Half Kakuja, War

Kaneki's last evolution after the Dragon. It is a *half kakuja*, not a kakuja: its own key (Y), its own rows in the kagune menu
(War choosable once the Dragon is spent, Peace shown sealed), and no part in K.
- **Model:** `control/king/WarModel` is pure procedural geometry in the mod's smooth kagune style, after
  `tools/kakuja/reference/war.webp`, drawn per body part by `client/render/KingLayer`. That layer builds War once a tick and
  draws it again between ticks. By night it darkens the body and blooms the eye with distance.
- **Pose:** `client/animation/WarPose` gives the hunch, the snapping head tilts and the twitches.
- **Server:** `server/KingServer` holds the crown (saved in the profile as `HexKing`), the toggle, the stats (transient attribute
  modifiers), the damage reduction and the sync (`S2CKingState`, `C2SKingAction`, protocol 32). While War is worn,
  `DragonLock` keeps the kagune in.
- **Previews:** `tools/art-preview.sh <dir> war war-views war-night`.

## 1.0.4 (HexKagunesv2): Kakuja V2, the Dragon, on top of 7.0.1

The first HexKagunesv2 release on the full 7.x mod (Ayato, Furuta, quests, ghoul NPCs). It adds the Dragon for Kaneki DNA:
- **Quest:** "Kakuja V2: The Dragon" (mastery 1000) asks for 100 kills in the Full Kakuja and 7 brinks lived through in a
  kakuja, and opens the gate `kaneki_dragon`.
- **Trigger:** the next death is cancelled, and a 200-block centipede Dragon rampages for exactly 60 seconds instead. It
  crawls head first, bites with fanged heads, and grabs and slams with its tentacles. It crushes what is under it and ploughs
  through terrain, which repairs itself afterwards.
- **No control:** every kagune, eye-mode and path ability is locked while it runs (`control/DragonLock`, checked by the
  original actions and every action packet).
- **The end:** it shatters, and Kaneki stands back up.
- **Source:** the Dragon lives in `awakening/`. It is written with Mojang names against its own stubs, then remapped to SRG
  through `awakening/mappings.srg` (proved by `tools/check_mappings.py`). It is packaged as
  `com.hex.kagune.control.awakening` and verified like the rest of the expansion: allowlist entries come from
  `tools/dragon_allowlist.py`.

## 7.0.1: every ghoul fights in eye mode, Ghoul Meat, quests

With only the kakugan open (**R**; the kagune put away), every ghoul, on any path, now fights with the One-Eyed Owl's body: the
combat and movement kit built for Owl DNA, reused as it is.

| Key (eyes open, kagune away) | Move | Needs |
| --- | --- | --- |
| LMB | **Speed Blitz**: jab, cross, hook, liver shot; the uppercut and blinking palm at mastery 100; the spinning backfist and the twin-palm finisher at 350. Locks on and steps in. | 0 |
| Right click (empty off hand) + W/A/S/D | **Flips**: front, back and side flips, no fall damage | 0 |
| Z held, let go | **Big Punch!** (charged; a blink in and a launch) | 0 |
| V | **Speed Kick** (the lunge, unseen dash and the kick into its middle) | 250 |

- **It scales with mastery.** The blows hit ×0.70 at mastery 0, rising evenly to ×1.00 at 1000, on top of the blitz growing
  longer with mastery. A sustained blitz comes to about 5 damage a second for a new ghoul and about 14 at 1000, below any
  kagune's own M1 (18 to 24 a second at 1000).
- **M1 on a block still mines** unless a body is right by the crosshair (within 4.5 blocks and about 25 degrees). Boats, frames
  and carts are hit as ever.
- **The kagune out (Alt + R) ends it at once.** Whatever move was in flight stops cleanly (a body caught mid-air still lands
  without fall damage) and the path's own kit takes over. Closing the eyes ends it the same way. Nothing of any kagune was
  removed or changed.
- Only the body moves come across: no flesh swipe, grab, hammer, Mutated Arm, Watch This or Owl kakuja (those were Owl DNA's
  kagune). Hits in eye mode raise mastery and never feed the Kaneki kagune's frenzy.
- The abilities panel lists the eye-mode moves while the eyes are open.

### Ghoul Meat

A new item: a torn hunk of ghoul flesh, dense with RC. It is in the creative Food & Drinks tab for now; the ghoul NPCs coming
next will drop it. Eating it makes a ghoul permanently stronger, and it stacks:

| Pieces eaten | Strength (every blow you land) | RC lasts |
| --- | --- | --- |
| 1 | +1% | x1.02 |
| 10 | +9% | x1.22 |
| 40 | +25% | x1.63 |
| 100 | +37% | x1.92 |
| limit | +40% | x2.00 |

- **Strength** multiplies every blow the ghoul lands: kagune, eye-mode fists and weapons.
- **RC lasts longer**: every RC cost (each move, the kagune's and the eyes' upkeep, transformations) is divided by it. A full
  bar holds that much more fighting, and the bar itself is unchanged.
- Every piece adds something, a little less each time. Each piece also fills 12 of the 20-point flesh reserve and can be eaten
  on a full reserve.
- A human who eats it gets nausea, poison and hunger, and no bonus.
- The count is kept through death; `/kagune reset` clears it. The tooltip shows what you have and what the next piece adds.
  `/kagune ghoulmeat` reports it, and operators can use `/kagune ghoulmeat set <n>`.

### Quests: progression is no longer mastery alone

Mastery still matters: it **opens** a quest. Finishing the quest **opens** the transformation, move or mutation behind it. Most
gates have two ways in, and some rival ways close each other. Press **U** (rebindable in Controls) for the journal: your path's
quests, the ghoul's way, tonight's hunts, and what is done or closed.

| Path | What a quest gates | The ways in |
| --- | --- | --- |
| Any ghoul (eye mode) | Speed Kick; Up We Go (soaring flips); Witness This (the full Big Punch's combo) | Light on Your Feet; Up We Go!; Witness This |
| Kaneki DNA | Twin Rinkaku | Rize's Gift **or** Learning to Say No |
| | Four-Tail Rinkaku | Anteiku's Way **or** The Pack's Way (rivals: the first finished closes the other and picks your side) |
| | Centipede Form (K) | 1000 Minus 7 **or** Ghoul Eats Ghoul |
| | Full Kakuja (hold K) | The One-Eyed King **or** The Centipede's Feast |
| | Centipede Body, Projectile Reflex Organ (mutations) | 1000 Minus 7; Arrow Eater |
| Ayato DNA | Gatling | Crystal Rain **or** The Black Rabbit |
| | First Kakuja | The Pack's Rabbit **or** Touka's Shadow (rivals) |
| | Final Kakuja | Rabbit Unbound **or** Eye of the Storm |
| | Bursting Shards (mutation) | Bursting |
| Furuta DNA | Centipede Rinkaku | Rejection **or** The Clowns' Game |
| | Four Thorns | Four Thorns |
| | Many-Eyed Kagune | Let's Play House **or** Rize's Nursery |
| | Kakuja | The Dragon **or** Washuu's Heir |
| | Rize's Appetite, You Thought You'd Get Rid of Me? (mutations) | Rize's Appetite; You Thought You'd Get Rid of Me? |

- **Objectives vary**: kills of particular things (iron golems, skeletons, bosses, other ghouls); kills done a particular way
  (eyes only, kagune out, in a kakuja, mid-air, at night, with shots); eating raw flesh or Ghoul Meat; dying; living through
  the brink (dropping below a fifth of your health and surviving it); rampages (kills in a row); restraining the kagune's urge;
  damage dealt or taken; time spent in a form; landing the eye-mode moves.
- **The ghoul's way** (any path) adds quests that pay mastery, Ghoul Meat or **mutation points**: Night Stalker, The Brink,
  Rampage, Die Hard, Cannibal, Ghoul Eater, First Meal. Quest points add to the mastery milestones' in Ayato's and Furuta's
  menus; Kaneki's mutations come with mastery tiers, so he gets 60 mastery per point instead.
- **Hunts**: some nights at dusk you get a hunt (kill N witches before dawn, kill bare-handed, go to the brink, a rampage,
  feed...), rolled at random along with its reward (mastery, Ghoul Meat, or rarely a mutation point). It ends at dawn.
- **Sides**: Anteiku's Way and Touka's Shadow put you on Anteiku's side; The Pack's Way and The Pack's Rabbit put you on the
  Pack's. Ghoul NPCs will remember it.
- **Menus show it**: a form or stage past its mastery but waiting for its quest says *quest*; a gated mutation says *needs its
  quest*. The server refuses it either way, and the action bar names the quests that open it.
- **Old saves keep what they had**: the first time a 7.0.1 server sees a ghoul, every gate its mastery already reached (and every
  mutation it already took) stays open. To try the new progression from the start, an operator runs `/kagune quests reset`.
  `/kagune quests` lists your open quests; operators can also use `/kagune quests complete <id>`, `/kagune quests gate <id>` and
  `/kagune quests hunt`.

## 7.0.0: Furuta DNA replaces Owl DNA

**Owl DNA is dormant.** All of its code, models, moves and mutations stay in the mod, but nothing reaches them: the starting
screen, the menus and `/kagune path` offer Furuta DNA in its place, the old ids `owl` and `eto` (commands, saves, network) read as
Furuta DNA, and a player saved on Owl DNA is moved to Furuta DNA when they join. Bringing it back is one line
(`KagunePath.dormant`).

**Furuta DNA** (Kichimura Washuu's transplanted kakuhou; every path is now named "X DNA"): a rinkaku that grows from the lower back, five stages, a kakuja, and
regeneration paid for in RC. The kagune is drawn the way the other paths' are (procedural meshes, plus a texture for the
kakuja's hide; the mod has no GeckoLib or Blockbench files to reuse), and it reuses the mod's combat, wounds, bleeding,
stuns, block smashing with restoration, Photon effects and sounds.

- **Kakugan:** the right eye only, always (the kakugan screen can't pick the left or both). R opens the eyes on their own as on
  every path.
- **One bar (every path):** RC, healing and food are one bar now, the flesh reserve where the food bar was: abilities pay half
  their old RC price from it, healing and the kagune out drain it, only eating (raw flesh, or what the kagune devours) fills it.
  Without the ghoul diet (server config) or in creative, RC is its own bar as before.
- **Regeneration (passive):** wounds start closing sooner and close faster than a plain ghoul's (wait 50% → 25% of the usual
  with mastery, heal 75% → 50%; in the kakuja 12% and 35%). Each point healed costs RC, so it stops when the bar runs dry. After
  a heavy blow (4+ damage) part of it grows back over 1.5 s (35%, 65% in the kakuja). Once every 3 minutes a killing blow, with
  30 RC to spare, leaves him on half a heart instead, and up to 5 hearts grow back.
- **Stages** (menu, or K held to step to the next one you have):

| Stage | Mastery | Limbs |
|---|---|---|
| Rize's Kakuhou | 0 | 1 thick muscular rinkaku tail, 2 from mastery 100 (thin where they leave his back) |
| Centipede | 250 | 3 armored centipedes, spiked; one has a toothed mouth that opens, snaps and bites |
| Thorns | 450 | 4 enormous thorn limbs |
| Many-Eyed Kagune | 700 | posed and shaped after the manga's "let's play house" panel: 5 broad crescents stacked as a towering wall to one side of him, the lowest a tail along the ground, crests, claw tendrils off their ends; almond eyes that blink, roll and stare |
| Kakuja (K tapped) | 900 | living black hide with burning slashes, a spiked crown, a great white star of an eye over a huge grin; 2 colossal centipedes with eyes and spikes, never still |

- **Controls** (Options → Controls → HexKagune): Ability 1 to 8 are the original kagune keys (Z, J, V, B, N, H, G, C by default),
  K is "Kakuja / Transform", R manifests, the menu key opens the HexKagune menu.
- **M1 strings** (click within 9 ticks to go on; each stage its own):
  - Rize's Kakuhou: Rising Lash, Backhand Lash, Twin Skewer, Gutting Upper (ripped up into the air), Guillotine (both tails down on it).
  - Centipede: Hook Rip, Hook Rip, Snapping Lunge (the mouth), Spiked Impale, Centipede Crash (ripped up, crashed down).
  - Thorns: Thorn Sweep, Thorn Sweep, Cross Cleave, Fourfold Pierce, Quadruple Crush.
  - Many-Eyed: Crescent, Crescent, Staring Barrage, Pincer, Crescent Cyclone.
  - Kakuja: Rake, Rake, Centipede Strike, Double Rake, Devouring Lunge (his own jaws), Predator's Finish. His body rakes, lunges and
    slams with them; in the kakuja he stands hunched like a predator, claws flexing, head twitching, the colossi swaying, snapping
    and rearing on their own.
- **Abilities:**

| Ability | Base kagune (Rize's Kakuhou, Centipede, Thorns) | Many-Eyed (evolved) | Kakuja |
|---|---|---|---|
| 1 | Rinkaku Barrage: nine rapid stabs | Dragon's Tendrils: every ribbon after its own target, twice (700) | Centipede Execution: one colossus holds it up, the other runs it through six times |
| 2 | Predator's Grasp: seized, lifted, slammed into the ground (60) | Thousand Thorns: flung out bristling with enormous spikes all round (740) | Kakuja Rampage: claws, colossi, jaws and sweeps, eleven blows |
| 3 | Crimson Impaler: one limb shot 16 blocks through everything in line (150) | Living Fortress: wrapped round him, blocking blows (780) | Dragon's Descent: a leap, the colossi raised, driven into the ground (920) |
| 4 | Flesh Devourer: its point becomes a toothed mouth, bites twice, a kill feeds the bar (250) | Predatory Evolution: 15 s of a bigger, longer, harder-hitting kagune with shorter cooldowns (820) | Monstrous Regeneration: 60% of his health knit back over 2.4 s, paid in RC, the bleeding stopped (940) |
| 5 | Centipede Rush: hurled forward tearing through everything, the next blows harder (350) | Centipede Rush | Final Calamity: both colossi swept across everything, then the ground erupts in spikes 10 blocks round (980) |
| 6 | Thorn Cyclone: spun round him three times, spikes out (450) | Thorn Cyclone | — |

  Numbers are the mastery each needs. Right click + W/A/S/D is the dash. K: tap for the kakuja (900), hold for the next base
  stage. With a sword or an axe in hand M1 is the blade, swung as Minecraft swings it, and the kagune follows through on what it
  cut. The ground strikes break blocks and put them back later.
- **Mutations** (a point at mastery 500, 800, 900 and 1000, spent in his menu):
  - **You Thought You'd Get Rid of Me That Easily?** (from Owl DNA): once every 5 minutes, death blows him apart and everything
    forgets him; he lies in pieces 11 seconds before the flesh lines haul him back together (slower than it was), whole, in his kagune.
  - **Rize's Appetite:** every bite that lands feeds the bar; a kill feeds it far more and heals him.
  - **Kakuhou Overdrive:** wounds start closing sooner and close faster; heavy blows grow back far more of what they took.
  - **Washuu Blood:** the kakuja costs 15 RC instead of 25, burns RC half as fast, and its blows land 15% harder.
- **Deploy and transform:** the limbs grow out of the lower back (a heave under the skin first), unfold, and fold back in when
  put away. The kakuja draws the stage back in, spreads the hide from the small of the back over the body and up the face,
  then the colossi unfold; turning back plays it in reverse. In first person nothing is drawn near the camera.
- **Kakuja:** 25 RC to enter, drains RC while in it, takes 55% of every blow, Strength II, Speed and Jump Boost; 10 s cooldown.

![Furuta DNA stages](docs/images/furuta-stages.png)
![Furuta DNA kakuja](docs/images/furuta-kakuja.png)
![Furuta DNA head](docs/images/furuta-head.png)
![Furuta DNA moves](docs/images/furuta-moves.png)
![Furuta DNA growing out](docs/images/furuta-deploy.png)

## 6.2.3: Witness This further, varied and heavier

- Knocks of 7.5 (quick jabs) to 23 blocks (heavy kicks) round a 16 × 22 block space, 17 up; never the same pace twice running
  (2 to 9 ticks), a hit stop on every blow; a roundhouse, a spinning heel kick, an axe kick and a flying knee join the side kick
  and the stomp; big flashes, squash and stretch on the target, her body driven into each blow.
- Fixed: her facing wound past a whole turn and was clamped (wrong way from the fourth blow); the target's head is its body's.
- It lies stunned in the crater 3.5 s before it gets up.

## 6.2.2: Instant Big Punch, a bigger and slower Witness This, unseen dashes

- **Instant Big Punch** (menu switch on the Witness This card, off by default): a Z tap is the full-charge punch at once, straight
  into the combo.
- **Witness This**: blows knock the target 11 blocks (a sphere about 28 across), it rises 13, one blow every 5 ticks, a slower
  tumble, a jolt and head whip on every blow; her head stays square on her body; the target no longer "lands" mid-air.
- **Dashes**: untouchable through the approach of the kick, the Big Punch blink and the combo; the target is stunned from the
  dash, so golems can't swing first.

## 6.2.1: damage cut, Witness This slowed down, the Owl a fortress

- **Damage**: every Owl DNA move does about 45% of what it did (a fist is 1.8 at mastery 0, 4.5 at 1000; was 4 and 10), and
  launched bodies take far less crash damage.
- **Witness This**: 20 blows, one every 3 ticks (about 3 s), each knocking the target 7.5 blocks across a sphere over 8 blocks
  wide; the uppercut throws it 9 up. The stomp winds up above it, then drives it into a bigger crater (3.6) with a wider shock (8),
  a flash and thunder. She kicks off it and falls out of the sky (no fall damage) instead of being put back on the ground.
- **The Owl**: takes only 15% of any damage while in it. The mask is a tall egg-shaped shell over her head, tapering to a V chin.

## 6.2.0: Kizaru's kick, Witness This, True Anonymity

The release (version 6.2.0; it also carries everything of 6.1.0 and 6.2.0 below: the Owl DNA path, the Owl and its moves). The
network protocol is now "25": everyone on a server needs this jar.

- **The sealed Kakuja V2 is gone.** The Owl is her only kakuja: one wide slot in her menu, K tapped or held both grow it or
  draw it back, the unlock track and the path card end with it.
- **Speed Kick (V tap), reworked again: Kizaru's kick, everything behind it.** Range 20 blocks (the Owl's own kick keeps 40).
  She throws herself into a sprinter's lunge (the user's stick figure: leaned far over, the right arm reaching out ahead, the
  left swept back with the forearm hanging, the left knee up under her, the right leg stretched out long behind) and is
  **gone**: through the dash nothing of her is drawn (body, held items, armour, name, her own hands in first person) and she is
  invisible to the server too, so not even her shadow shows; **nothing can hurt her** from the lunge until she is there. A
  pale afterimage of the lunge is left where she was, the ground is torn up along the way, the wind streams on. The air cracks
  where she appears, already thrown back with her right knee drawn up hard, and she drives a **flying side kick** into its
  middle (its gut, or its backside if it is turned away): her whole body thrown out flat in the air behind the leg, the leg
  locked out dead straight, the left knee tucked (the user's second stick figure). A blinding flash, rays of light bursting out
  every way (the Kizaru frame), rings of shock, a cone of torn air, the dust blasted off the ground, a sonic boom. It flies
  **straight away from her, low and very far (about 55 blocks on open ground)**, carried by the kick (no air drag), trailing torn
  air, smashing through **anything up to iron** (stone, deepslate, ores, bricks, even iron and diamond blocks; never obsidian;
  every block put back exactly later) and bowling over whatever it flies into; the same tumble and lie-down as before. She holds
  the leg out flat a moment, **rebounds off it in a backflip** (unrolling as she goes over, tucked upside down), lands in a deep
  crouch and comes up into her guard. No target: only "No target found", as before.
- **Mutated Arm (N): the flesh arm travels slower:** it takes twice as long to shoot out (12 ticks) and half again as long to
  drag back (14), a heavy thing hurled rather than a beam.
- **The Owl's mask, after the anime (first pass):** the beaked owl face and its feathers are gone; a smooth bone shell with a
  ridge down the middle, one round eye hole with her kakugan burning red round a black pupil, scars over the other eye, thin
  cracks across it, a dark hood over the rest of her head. Its pointed V chin and the rest of the Owl's polish come next.
- **Two new mutations (points at 900 and 1000; the menu's MUTATIONS card now holds four):**
  - **True Anonymity:** the Flesh Hammer's club (V held) grows on past its full size, up to 1.7x: longer, thicker, bristling with
    bone spikes all over the flesh (a raw red collar where each breaks out, bone, a dark point); its stomp reaches further, its
    shockwave is wider (up to 7.25 blocks), its crater bigger, its blow harder, and **everything it hits bleeds**. No dearer.
  - **Witness This:** a **fully charged Big Punch** that lands goes on into a combo. The blow drives it back; she is gone and
    **behind it**, and her **uppercut throws it 7 blocks up**, a **rag doll from then on** (the Speed Kick's tumble); she is
    after it at once, while it still rises (no pause), and **blitzes it in midair: a storm of 24 punches and kicks, one a
    tick** (crosses, hooks, jabs, a liver shot, uppercuts from below, the Speed Kick's flying side kick, stomps from above,
    twin palms), **each from another point of a sphere round it, above, below and every side**, each from about the far side
    of the last, **every blow knocking it 3 blocks** the way it was struck, so it is batted all over the sky (as Rock Lee does
    Gaara; her whole body is tipped to aim each blow, an afterimage left at every blink, a white flash at every blow); then she
    is **right above it, tucked, and stomps it straight down**, riding it down into the ground:
    it hits the ground in a **crater**, the shock throws everything near up and away, and it comes down flat, lies there and
    gets up. **It is frozen
    all through**: it can't move or hurt anything; a player can only look round (their own client carries them along its path),
    and no kagune of theirs answers. A stray key of hers never cuts the combo short.

## 6.1.0: Owl DNA

A fourth kagune path, the One-Eyed Owl strain's, built for close fighting: a fighter's body that moves like nothing else in
the mod, and pale flesh that pours out of her when she wants it. Pick it on the starting screen (or, as an operator,
`/kagune path owl` for yourself, `/kagune path set <player> owl` for anyone). 6.2.0: renamed Owl DNA (it is no character's
any more: an experiment's); players already on it keep it, and the old id still works in the commands. Her menu (the path menu key) shows her organ, stage, mastery and unlock track, a live preview, every move with its
key, and her kakuja, **the Owl** (6.2.0, K; 6.2.0: her only one, the sealed V2 is gone).

| Key | Move | Unlocks |
|---|---|---|
| R | Ghoul Eyes (every path) | start |
| Alt+R | Her form: one kakugan, steam off her skin | start |
| M1 | Speed Blitz: jab, cross, hook, liver shot; then uppercut and blinking palm; then spinning backfist and twin-palm finisher | 0 / 100 / 350 |
| M2 + W/S/A/D | A huge front, back or side flip | start |
| Z held, let go | Big Punch (6.2.0: Witness This at the full charge, with the mutation) | start |
| J tapped | Flesh Swipe | 150 |
| V tapped | Speed Kick (6.2.0: Kizaru's kick, 20 blocks) | 250 |
| J held, let go | Flesh Grab and Throw | 450 |
| V held, let go | Flesh Hammer | 600 |
| B | Watch This (6.2.0) | 650 |
| N tapped / held | Mutated Arm (6.2.0) | 700 |
| K (tap or hold) | The Owl, her kakuja (6.2.0; every key is its own move in it) | 750 |
| Menu (mutation) | You Thought You'd Get Rid of Me That Easily? (6.2.0, passive) | a mutation point at 500 |

- **Ghoul Eyes (R, every path):** the eyes-only form is now a form of its own: Resistance I and Strength I while the eyes are
  open, taken away the moment they close, the kagune comes out or you die. It drains a little RC (less with mastery) and
  holds back the rest-time recovery; run dry, the eyes close.
- **Her form (Alt+R):** no tendrils, no blood, no pain, never feral; only one eye turns (her chosen one, or the right).
  Faint steam rises off her shoulders, back, arms and chest, three soft crossed planes per wisp so it never shows an edge,
  fewer on the move and almost none in first person. At rest she stands hip-shot and plays: a shrug, a stretch, a head
  tilt, a glance, an arm swing, a weight shift, a foot tap, a heel rock, a shoulder roll, a giggling wave, and a loose
  saunter when she walks.
- **Speed Blitz (M1):** click by click. Every strike coils (weight back, chest wound away), then drives: the whole body
  pivots, the lead foot steps into a lunge, the shoulder goes through and the other hand pulls back, a hair of
  overshoot, a hold, and back to her guard (kept up a second). A click during a strike is thrown from its extension.
  She locks onto what she clicked (or the nearest body in front) and steps in so the fist lands on time; the blinking
  strikes vanish her to the target's side or front, leaving a pale afterimage, the view swinging round over a few frames.
  Each hit has its own sound, a white ring and sparks, the body reels like a spring, and the view kicks.
- **Flips (M2 + W/S/A/D):** right click with a direction (or none: forward) throws her into a huge flip: arms swung up
  from behind, a tight tuck (spread out like an aerial to the side), opened out to land. No fall damage.
- **Big Punch (Z):** hold to sink into a deep coil, wound right round, the steam pouring off her harder the fuller it gets,
  the body trembling near full; let go (a tap still punches) and she blinks in front of what she looks at (8 blocks, up to
  24 charged) and drives a gut punch from her longest, lowest lunge. Heavy damage, the body launched up to 4.4 blocks a
  tick, everything round the blow knocked back by the air it throws.
- **Flesh Swipe (J tap):** a huge mass of pale flesh rears out of her upper back, over her right shoulder, ending in one long
  blade of bone lying flat, gripped by cords of flesh at its root: a honed edge leading the cut, sawteeth down its back, a clip
  to a needle point. It sweeps across everything in front of her from right to left out to 7 blocks like an enormous whip: the
  flesh goes first, the heavy blade trails behind it, then whips through and swings on past before it recoils. What the blade
  cuts bleeds, sprays blood the way it swung, and is thrown hard to her left.
- **Flesh that carries its weight (every flesh move):** the flesh is never a rigid thing set in place: it follows its move like a
  heavy living limb, the thin flesh near her back moving first and bending ahead, the heavy end lagging, overshooting and swinging
  back, trailing after her when she moves. What it holds stays held at its end.
- **Flesh Grab and Throw (J hold):** the flesh arches high and comes down on what she looks at (up to 14 blocks) from
  above, a knot of flesh whose big tendrils wrap round it, and holds it up before her wherever she turns. Let J go: it
  hauls the body over her shoulder and all the way back, hangs there straining, then whips it over her and hurls it
  forward at its fastest. Nothing is held for good: it lets go the moment she can't hold it, and a miss snaps back.
- **Speed Kick (V tap):** 6.2.0 made it Kizaru's flying side kick out of a vanishing dash: see the 6.2.0 section above.
- **Flesh Hammer (V hold):** up on one leg, she swings the other up straight in front of her, all the way up, her body tipped
  back under it, and a club of flesh grows out of the foot on up into the sky, longer and thicker the longer she holds. Let
  go: the last of the wind-up, then everything comes down over the leg and the club swings through its whole arc into the
  ground ahead, knocking out a crater and sending a shockwave (3 to 5.5 blocks wide) that throws everything round it up and
  away. In first person she sees her own leg swing up in front of her (in her skin) and the club grow round it, then slam
  out ahead: seen from where her head has gone, so it never sits on the camera.
- **Watch This (B), 6.2.0, mastery 650:** she throws her arms out into a cross and two tentacles of her pale flesh burst
  out of her upper back, one over each shoulder, each ending in a long hooked beak of bone. For exactly ten seconds they hunt
  on their own: each picks the nearest thing hostile to her (a mob after her, or anything that hurt her lately; its own side
  first), rears back and lunges, faster and faster, and drives the beak through its chest (it bleeds, blood bursts out of its
  back, a player is left with a stab wound and a mob with a hole through it). It is hoisted on the beak in a great sweep up over
  her and out to that tentacle's side and thrown on at the speed of the sweep: the left tentacle's to her left, the right's to
  her right. The body stays on the beak the whole way. Then the tentacles draw back into her. 18 RC, 15 s cooldown.
- **Mutated Arm (N), 6.2.0, mastery 700:** her own right arm stays hers: she throws it out pointing where she looks, and her
  pale flesh bursts out all round it, from just above the elbow down her forearm and over her fist (her sleeve and skin
  showing between its lumps), then on out of her fist, swelling as it goes, a great clawed hand at the end. Tap: it shoots
  out up to 20 blocks, and whatever its hand closes on is hurt, hauled straight back to her and flung out to her right. Hold:
  it keeps swelling, thicker and more monstrous, reaching out to 35 blocks over three seconds, swinging after where she looks
  with the lag of its own weight and carrying whatever it catches (her arm stays pointed along it, her whole body leans and
  turns under it, her left arm out behind to balance it). At the full charge, if it holds anything, the flesh at its end folds
  in over it into a sphere and crushes it three times (blood forced out between the folds), held out on the stretched arm,
  then opens and drops what is left. The flesh draws slowly back into her arm, the mutation going down, its claws folding in,
  her arm coming down level and back to her side. She can walk the whole time. Her arm never points across her own face (the
  flesh curves on from her fist to where it goes); nothing goes through her body; never more than half a block of sag, so it
  never drags on the ground; it stops short of walls. In first person she sees her own arm out ahead, in her skin, the flesh
  wrapped round it. 8 RC (12 more for the sphere), 2.5 s cooldown once it is back in.
- **The Owl (K), her kakuja, 6.2.0, mastery 750** (in it, every key is its own move). She doubles over,
  shaking, as it tears its way out of her; her raw pale flesh bursts out of her upper back, blood out of both shoulders,
  throwing her back, and arcs up over her shoulders and down; the kakuja hardens over it: two giant red arms off her
  shoulders, each forearm a bone scimitar (flat, edged, bowed, veined red where it leaves the elbow) sweeping forward and down
  to a point near the ground, crimson crystal spurs off the elbows; the One-Eyed Owl's mask over her face, turning with her
  head (6.2.0 release, after the anime: a smooth bone shell, a ridge down its middle, cheekbone ridges, her kakugan burning red
  in a round hole with a round black pupil, scars across the other eye, thin cracks in the shell, a dark hood over the rest of
  her head; no beak, no feathers); the ukaku's jagged crimson crystals flaring up off her back behind the shoulders, burning (they
  and the eye glow in the dark); and her own body bulked up under it as HexHaki's haki physique draws it (thicker arms pushed
  out, a broad chest, in her own skin). It is alive: she stands hunched under it breathing heavily, the giant arms heaving with
  each breath and swaying, the blades flexing and now and then flicking out and back, the crystals flickering, the feathers
  ruffling; walking, the arms swing against her stride and the blades lift clear and lag behind them. Her heavy head turns and
  nods only so far in it. K again:
  her flesh wells out over it a moment, she bends over as it draws back into her, and she is herself again. 15 RC to grow it;
  while it grows or goes, nothing else starts. 6.2.0: the sealed V2 is gone; the Owl is her only kakuja, and K held is the
  same as K tapped. It goes at once if her form is put away, she dies or changes world.
- **Mutations (6.2.0):** like the other paths, Owl DNA now has mutations: each mastery milestone earns a point (the first at 500),
  spent in her menu (the MUTATIONS card under her kakuja: click it). Death keeps them; `/kagune reset` clears them.
- **You Thought You'd Get Rid of Me That Easily? (her first mutation, passive):** once every five minutes, the blow that would
  kill her blows her body apart instead. Her head is torn off at the neck, her torso torn in two at the waist, each arm and leg
  torn off and torn again at the elbow or knee: eleven pieces of her, in her own skin, every torn end raw (torn skin and fat,
  meat bulging out, the bone snapped off in the middle of a limb, the spine at the neck and the waist), thrown out from her
  middle, tumbling, bouncing, coming down within a few blocks and lying flat where they fall; her guts spill out of both halves
  of her torso in long ropes; blood bursts out every way, sprays off every piece as it flies and pools where each one lies. For
  five seconds she lies there in pieces (nothing can hurt her, nothing she does counts, her keys don't move her). Then flesh
  lines tear out of what is left of her chest and run out to every piece, two strands twisting round each other, writhing
  while they are slack; they go taut, haul, faster and faster, and everything is dragged up off the ground and slams back
  into place, her guts reeled in after it: she stands there whole, at full health, "You thought you'd get rid of me that
  easily?". Ready again five minutes after it took (her menu counts it down). Falling out of the world and /kill still kill her.
- **Idle only when idle:** her playful idle gestures play only after she has stood still (not moving from the spot; looking
  round is fine) for a second, and stop as soon as she moves, so they never turn her head mid-flip or mid-run.
- **One eye stays one eye:** on Owl DNA the kakugan screen never gives her both eyes, Reset included.
- **Bodies through walls:** everything her blows launch (and the hammer's crater) breaks the weak blocks in its way: dirt,
  sand, gravel, wood, glass, leaves, wool; nothing that needs a pickaxe or the right tool. Each block is offered to Forge's
  break event first (claims, spawn protection and adventure mode are respected), remembered exactly (contents too), saved
  with the world, and grows back after 30 seconds, never over anything built there since. Server config:
  `owlBlockSmash`, `owlBlockSmashRestoreSeconds` (6.2.0: renamed; set them again if you had changed them).
- **RC lasts at high mastery (every path):** mastery now takes up to half off every move's cost (it took 30%). Owl DNA's RC also
  comes back while her form is out, from 0.6 a second for a new ghoul to 2.4 a second at mastery 1000.
- **Built to never clip:** every pose is checked against the exact player model (PlayerAnimator and bendy-lib's own
  maths): the arms of every frame of every move, from anything she might be doing when it starts, stay out of her body,
  and every vertex of the flesh (trunk, tendrils, grabbing tendrils, the hammer's club) stays out of her torso, head and
  arms. Her moves are timed on a clock of the client's own, so a server running behind no longer makes them twitch.

![Her form: the steam](docs/images/eto-steam.png)
![Her idle](docs/images/eto-idle.png)
![Speed Blitz](docs/images/eto-blitz.png)
![Flips](docs/images/eto-flips.png)
![Big Punch](docs/images/eto-big-punch.png)
![The flesh: swipe, grab, throw](docs/images/eto-flesh.png)
![The swipe's blade](docs/images/eto-blade.png)
![Speed Kick](docs/images/eto-kick.png)
![Speed Kick: the body it lands on](docs/images/eto-kicked.png)
![Speed Kick in first person](docs/images/eto-kick-first-person.png)
![Watch This](docs/images/eto-watch.png)
![Mutated Arm](docs/images/eto-arm.png)
![The Owl](docs/images/eto-owl.png)
![The Owl alive](docs/images/eto-owl-anim.png)
![The Owl in the game](docs/images/eto-owl-transform.png)
![Flesh Hammer](docs/images/eto-hammer.png)
![Flesh Hammer in first person](docs/images/eto-hammer-first-person.png)
![Her menu](docs/images/eto-menu.png)
![Her mutations in her menu](docs/images/eto-menu-mutation.png)
![You Thought You'd Get Rid of Me That Easily?](docs/images/eto-revive.png)

## 6.0.11: arrows leave holes, zombies bite

- **Arrows:** an arrow's hole was sized by the health it cost, so a light one opened a hole about a pixel wide, lost on the
  skin. It is now at least about three pixels across, deep, where the arrow went in (a hard one still goes through and out
  the far side).
- **Zombies and other unarmed mobs:** a bite. The flesh torn open where the jaws closed (down to the bone on a hard one), the
  teeth's punctures above and below it, and a claw's two rakes down the side. A light hit is still plain to see.

## 6.0.10: the mod can no longer kick a player or crash the server

- **The crash:** Ayato's Rising Flame (and two other fire moves) passed no push direction to `AyatoFire.burn`, which read it
  anyway: every target caught threw, the player's tick failed ("Ticking player") and he was kicked. A missing direction is
  now from him toward the target.
- **Never again:** the build's last step (`patch/KaguneGuard`) wraps every way Minecraft calls into the mod while the game runs:
  all 137 event listeners (the original's and ours, server and client) and the original's spike tick, hits and saves, bleeding
  tick and spike renderer. An error inside any of them is logged (`Safety`: in full once from each place, then a count) and
  the game carries on; a spike whose tick or hit failed is removed. Out-of-memory and the like still pass. The guard checks
  every listener is wrapped and type-checks each one, the archive scope is checked, and the dedicated-server verifier runs
  again on the guarded JAR.

## 6.0.9: Kaneki's auto block stops the arrows

Auto Projectile (and the Projectile Reflex Organ) let most arrows through. A tentacle needs `TendrilShield.RAISE_TICKS` (4) to
swing into place before it stops anything, but projectile entities were seen only 8 blocks out along a straight line, as the
original did: under three ticks for a drawn bow's arrow, and a straight line missed arrows shot on an arc. Measured with
vanilla's arrow flight, only skeletons between 8 and 24 blocks were stopped; a bow's arrows 0 to 46% of the time, a crossbow's
almost never.
- **Seen far enough out** (`TendrilShield.incoming(p, v, gravity, drag, centre, drift, lookahead, reach)`): projectile
  entities within 48 blocks are followed along their real flight for 20 ticks, vanilla's order a tick (move, keep 0.99 of the
  speed, fall 0.05 for arrows and tridents, 0.03 for thrown things, fireballs straight), closest approach checked along each
  tick's stretch, his own movement included (not the steady fall gravity presses into him on the ground).
- **Guarded before the shot**: a shot loosed from close up lands before any tentacle could swing in, so the guard goes up
  toward a shooter about to loose at him: a ranged mob (skeletons, pillagers, drowned, witches) after him within 24 blocks, or a
  player drawing something or holding a loaded crossbow within 48 blocks, aimed within 15 degrees of him
  (`TendrilShield.aimedAt`). The tentacle is in place when the arrow arrives.
- Tests fire vanilla-accurate arrows from every side (a skeleton's aim and spread, a drawn bow, a crossbow, 8 to 40 blocks): every
  one that would hit is seen in time to be stopped.

Every other rule of the guard stands: guards drop while his tentacles attack, grab or move him, at most two at once, each holds
so much before it breaks, and every stopped shot costs RC. Same channel version (20): 6.0.9 mixes with every 6.0.

## 6.0.8: the kakugan's veins wind, its cracks zigzag

The veins in the skin (`KakuganModel.branch`) no longer run ruler-straight: each arches (a C, or mostly an S that bends back
part of the way along), meanders in a long slow wave with a short tighter one on top and kinks a little at random, never more
than about 70 degrees off its course. The cracks across the black (`KakuganModel.crack`) zigzag like lightning, and the
longer ones split once. Purely visual.

Same channel version (20): 6.0.8 mixes with every 6.0.

## 6.0.7: a one-eyed ghoul's kakugan is the stronger one

A one-eyed kakugan (left or right only) draws everything a two-eyed one does on that eye, plus more: three more red cracks
across the black, a little wider (`KakuganModel.ONE_EYED_CRACKS`), and four more veins spreading from the outer side, longer
and thicker than the rest (up over the brow, out along the temple twice, and down the cheek). Two-eyed ghouls are unchanged.
Purely visual.

Same channel version (20): 6.0.7 mixes with every 6.0.

## 6.0.6: a ghoul cannot eat human food, and nothing draws over the flesh reserve

- **Human food cannot be eaten** by a ghoul on the flesh reserve (`GhoulDiet.refuseHumanFood` on the server,
  `ReserveHud.refuseHumanFood` on the client cancel `LivingEntityUseItemEvent.Start`). Vanilla hunger is held at 17 under the
  reserve, so the game always let the meal start; it was eaten and only then rejected. Raw flesh and tolerated drinks still go
  down. The old rejection stays as a fallback.
- **AppleSkin's food icons no longer draw over the reserve bar.** Its exhaustion underlay, saturation outline and meal preview
  (`HUDOverlayEvent$Exhaustion/$Saturation/$HungerRestored`) are cancelled while the reserve shows; they drew outside the
  off-screen push that hides vanilla's food icons. Hooked by name at client setup, so nothing changes without AppleSkin.

Same channel version (20): 6.0.6 mixes with every 6.0.

## 6.0.5: the kakuja's vanish is true invisibility

While the kakuja's passive has him vanished (`KakujaPassive`), nothing gives him away:
- **Nothing of him is drawn, for anyone, himself included in third person** (`PlayerLayers.pre` cancels the whole player
  render: body, armour, held items, name, every layer). Until now his own view still showed his armour and what he held, the
  way a potion of invisibility leaves them.
- **No dirt kicked up as he sprints** (`VanishClient`). Vanilla throws it from `Entity.baseTick` for any sprinting entity, and
  Forge has no hook there; on every client a vanished player's sprint flag (`Entity.setSharedFlag(3)`, by reflection) is held
  off from the start of his tick (`PlayerTickEvent` START) until his movement input (`MovementInputUpdateEvent`, fired in
  `LocalPlayer.aiStep` after `baseTick` and before his sprint logic) or the end of his tick for anyone else, so his speed, his
  sprinting and what the server hears are untouched. Not in water, where the flag keeps him swimming.
- **No dust where he lands**: his fall distance stays at zero while vanished (the manifested kagune takes no fall damage
  anyway: the original's `kaguneBlocksFallDamage`), and no tendrils catch his fall (`AyatoGates.tryStartFallArrest`), so the
  original's landing burst never plays.
- **No wind or sound when he dashes.**

Same channel version (20): 6.0.5 mixes with every 6.0.

## 6.0.4: the Steel Blade hangs at his side, mastery gets harder, Kaneki's moves unlock with it

**The Steel Blade's hold** (`SteelSwing.hold`) hangs down at his right side, out from the leg about 58 degrees below level (6
degrees behind the side line), the edge forward: neither standing out level (6.0.1) nor swept behind him (6.0.2). The blade
runs about 2.8 blocks from the shoulder against 1.4 blocks of shoulder height, so resting there it draws in
(`BladeCarry.fit`: the longest it can be with every part a pixel off the ground, never under 30% of its length), to about 1.4
blocks out of the shoulder, its point just off the ground. It drives out to full length over a swing's wind-up, stays out
through the cut and draws back in as it settles (`SteelSwing.fit`, `RigPose.bladeFit`/`bladeReach`); the third-person blade,
its trail and the first-person blade are all drawn at that length (`SteelBlade.build(..., reach, ...)`). Crouched it hangs 16
degrees higher (even drawn in, 58 degrees would touch). A swing cut short now eases back from its very first frame (it
snapped to the hold for one frame before).

**Mastery gets harder as it climbs** (`MasteryCurve`, `MasteryGain`). The original added every raw point straight to mastery (4
a kill, 1 a hit, a grapple, a guard held, a hunt; Ayato's shots the same), so the last points came as fast as the first. Each
point now costs more raw the higher it is: 1 at the start, 2 at 100, 6 at 500, 10 at 900, so 1000 takes 5995 raw where it
took 1000 (100 takes 149, 500 takes 1747). Progress toward the next point carries over in the profile (`HexMasteryCarry`,
thousandths). The bytecode hook is the one call inside `KaguneProfile.addMastery(player, action, amount)` that added the
amount; the action's own tally and `/kagune` setting or adding mastery by hand stay exact.

**Kaneki's moves unlock with mastery** (`KanekiUnlocks`), as Ayato's do: the seed starts with its M1 whips and the guard; the
grapple (J) at 50, Rapid Strike (Z) at 150, grab and throw (G, N) at 250, crush (H) at 350, lift (V) at 450, slam (B) at 500.
The gates are the existing hooks (`AyatoGates`, `AyatoVortex.startRapidStrike`); a locked key says when it unlocks. The
abilities board shows each move dim with the mastery it needs, and Kaneki's menu lists the track.

Same channel version (20): 6.0.4 mixes with every 6.0.

## 6.0.3: only Kaneki's five mutations in his menu

Kaneki's menu (`KanekiMenuView`) lists only the five mutations the path keeps, one a tier (`KanekiMutations.nodes()`: Reach &
Control, Auto Projectile, Centipede Body, Projectile Reflex Organ, Full Kakuja: Hunter). The ten cut in 3.0.3 are not shown at
all; 6.0.0's menu walked every `MutationNode` and listed them greyed out (6.0.2 only took their 2147483647 away). Same channel
version (20): 6.0.3 mixes with every 6.0.

## 6.0.2: a low Steel Blade, a fitted mask, Kaneki's eyes and stance, a cleaner menu

**The Steel Blade's hold** (`SteelSwing.hold`) no longer stands out to his side like a T-pose. The blade runs about 2.8 blocks
from the shoulder against 1.4 blocks of shoulder height, so it can't hang straight down: about 26 degrees below level is the
lowest it goes in any direction without touching the ground (19.5 crouched). It now trails low behind him past his right hip
(a low side guard, the tip back and down near the ground, the edge down): 68 degrees back from his side and 22.5 below level,
streaming further back at a run and riding higher crouched (on the same frame vanilla's crouch drops the model), so the ground
guard never has to swing it round. `SteelSwing.path` now carries the edge along the blade's actual way and spreads the one
turn left over (the short way) evenly: until now the turn between the two ends' edges was taken afresh each frame and flipped
half over in a frame when they came out opposite, which the new hold exposed. The backhand winds up a little longer from it;
a test holds every wind-up slower than its cut.

**Ayato's half mask** (`KakujaModel.halfMask`) is fitted to the face: its surface lies 0.12 pixels off whatever is really under
it, front and sides, turning the corner of the face round a small radius: the face itself when the skin's second layer leaves
the lower face bare, that layer as vanilla draws it (4.5 pixels out) or 3D Skin Layers' voxels (4.72) when it covers any of it
(`halfMaskShell`, from `KakuganSkin.covered`; a skin not read yet counts as covered). It stood half a pixel wider than the head
before, and under 3D Skin Layers it was stretched 1.18 wide and 1.3 deep: a band floating two pixels off the face. Tests: never
inside what it covers, its front just off it, no wider than the head, for the three surfaces and several eye placements.

**Kaneki's eyes in his transformation** (`ControlledManifestAnimation`): the original's doubling over is now a lean of the
whole upper body about the hips (`RigPose`), as Ayato's leans became in 5.0.19, instead of PlayerAnimator's body bend. The
head part itself carries it, so the kakugan, Kaneki's mask and 3D Skin Layers' second layer go down with his face instead of
hanging in front of his chest.

**Kaneki's ambient stance** (`IdlePose`): where elbows and knees can't be drawn bent (3D Skin Layers, or no bendy-lib;
`BodyAnimations.limbsBend`), his crouch, all bent knees and elbows, came out as stiff splayed legs in a broken lunge. He now
stands in a feral stance built for straight limbs: hunched over wide-set feet, the head up to watch, both arms reaching forward
and out like claws. Ayato's stances drop their small bends there, the body set so both soles stay on the ground.

**Kaneki's menu**: the mutations cut from the path (their mastery parked at `Integer.MAX_VALUE`) no longer print 2147483647;
they stay greyed out with nothing beside them. The Appearance & Abilities button and its caption are gone.

Same channel version (20): 6.0.2 mixes with 6.0.0 and 6.0.1.

## 6.0.1: vanilla's right click comes first

The kakuja's dash (`AyatoInput.useInput`) no longer takes right click away from vanilla. Neither the original JAR (its only
mouse hook is left click, `KaguneKeys$ForgeBus.attackInput`; every key it binds is on the keyboard) nor the expansion before
6.0.0 (its right click was Ayato's alone) did anything with Kaneki's right click, so vanilla's was the one there: doors,
chests, levers, trades, mounts, placing blocks, using items. Forge fires `InteractionKeyMappingTriggered` once per hand inside
`Minecraft.startUseItem`, main hand first, and only reaches the off hand when the main hand's interaction did nothing; the dash
now waits for that off-hand turn and takes it only when the off hand is empty. So whatever right click did before still
happens, and the dash comes when it would have done nothing (a shield or a torch in the off hand keeps right click for that
item). Ayato's enders outside the blade are unchanged. Same channel version (20): 6.0.1 and 6.0.0 mix.

## 6.0.0: the official release

**The kakuja's passive and dash (`KakujaPassive`).** Ayato with the Steel Blade out and Kaneki in his Full Kakuja: breaking into a
sprint vanishes him in a gust of wind for 5 s (the invisibility, mobs drop him and can't retarget, `PlayerLayers` cancels the
whole player render for everyone else, the base tendril renderer already skips invisible players); attacking by any means
(`KaguneCombat` swings and enders, Ayato's actions, `AttackEntityEvent`, `ArrowLooseEvent`, any damage dealt) or losing the form
ends it, then a 30 s cooldown. Kaneki's legs don't hunt while he is vanished (`CentipedeHunt`). Right click dashes
(`C2SAyatoAction.DASH`: W or nothing forward, S back), far for the blade, much less for Kaneki. Wind: `S2CAyatoFx`
VANISH/REVEAL/GUST, client `VanishFx`.

**A spent kagune (`KaguneLock`).** At 0 RC the kagune goes in and `PathServer.toggle` refuses it for 10 s; the eyes stay.

**The Steel Blade in first person** (`SteelBladeLayer.FirstPerson`, `RenderHandEvent`): drawn from the body layer's own arm pose
(`KaguneBodyAnimation.bladePose`) through the world's camera-to-body turns, so it is held and swung exactly as others see it; and
slimmer (the edge stands out 5.4 px at most).

**Balance.** Ayato's first kakuja: 2 needles a tentacle per M1 volley; the J Needle Gatling 0.5 salvos a tick from the first and
1.4 at full spin, in a second.

**Kaneki's menu** (`KanekiMenuScreen`, `KanekiMenuView`): the Ayato board in crimson: organ, mastery, forms, RC, the kakuja's
keys, the mutation tree, Adjust Kakugan, and the original screen for appearance and toggles. The abilities board shows the
Steel Blade (C), its dash, vanish and fireballs, and the Full Kakuja's dash and vanish. Channel version 20.

## 5.0.20-beta: a relaxed hold, real swings, blood

`SteelSwing`: the Steel Blade arm's whole turn in the chest's frame (quaternions). The hold hangs low at his right side,
trailing behind, breathing and swaying with his stride. Each swing winds up from wherever the blade is, then cuts about its own
axis edge-first, fastest exactly at the impact tick. It carries through and settles back into the hold, the chest twist
composed as a real turn of the arm (`RigPose`). Blade hits send `S2CAyatoFx.BLADE_HIT` (`BladeBlood`): blood along the cut,
bursts, gushes and pools on kills (`BladeBloodFx`), and a bloodied blade that dries (`SteelBlade.build(..., blood, dry, ...)`).

## 5.0.19-beta: the Steel Blade rebuilt after the manga, its carry, and no more drifting skin layers

**The Steel Blade (`SteelBlade`)** is a cone with an edge, no flat blade: round and massive where it swallows the arm from the
shoulder, tapering to a needle point, one side drawn out into a single long curved cutting edge (its cross-section a teardrop
with hollow-ground flanks). Six wing plates lie along its back like stacked feathers, the outer ones shorter, each narrowing to a
point that peels up into a forward-swept barb: the saw-toothed row along its back. A pauldron of layered plates covers the
shoulder. It is streamed straight into the vertex buffer with smooth per-vertex normals (`SteelBlade.Sink`), no quads built on
the way. Forming (24 ticks, every phase eased with no jerk, checked at 60 fps): the steel tentacles draw in, the plates sweep
round the arm from behind and wrap it, the steel swells out under them and the pauldron fans in, the blade drives out past the
fist (a touch past home, then settles), the edge unfurls, the barbs spring from the shoulder's to the point's, and a glint runs
to the point. Ambient life: a glint running down the edge, the plates breathing, the barbs flexing. In first person the
pauldron is left off.

**Its carry (`BladeCarry`).** While the blade is out the right arm holds a pose of its own, always: en garde, the blade out
ahead of him to his right and a little raised, edge down, barbs up, breathing and bobbing with his stride. Vanilla's walking
swing, crouch and attack swing are dropped for that arm (`RigPose.rightArmHold`), and the arm, sealed in steel, doesn't bend.
The three steel swings (`StrikePattern.STEEL_SWEEP`, `STEEL_RETURN`, `STEEL_EXECUTION`) take the arm from the carry and give it
back (`AyatoBody.envelope`). Whatever the pose (a swing, a lean, a crouch, a drop into a landing), `BladeCarry.guard` keeps
every part of the blade off the ground: it reckons the blade's lowest point from its real geometry (`SteelBlade.extent`) and
lifts the arm along a smooth flow (up, drawn round toward the carry), just far enough, keeping the edge down. It runs in
`RigPose`, so the previews pose exactly what the game does.

**Drifting skin layers (`RigPose`).** Leaning the upper body used PlayerAnimator's `body` bend, which draws the head and arms
under a turn their model parts never carry; anything drawn from the parts (3D Skin Layers' outer skin, the kakugan) was left
behind or vanished during ambient animations. The lean now turns the head, torso and arms about the hips through their own
pivots and rotations, and limb bends are skipped under 3D Skin Layers.

![Steel Blade](docs/images/ayato-steel-blade.png)
![Steel Blade up close](docs/images/ayato-steel-blade-detail.png)

## 5.0.17-beta: the Steel Blade, and wounds down to the bone

**Ayato's final kakuja: Steel Blade (C).** The steel wings close round his right arm into one massive blade (`SteelBlade`, drawn on
the arm by `SteelBladeLayer` so it rides every pose; the steel tentacles hide while it is out). Speed +3 and Jump Boost +2, M1 is
`ComboStyle.AYATO_STEEL_BLADE` (wide, fast sweeps from the right), reach `SteelBlade.REACH` = the arm plus the blade's length,
M2 unchanged, J fires only fireballs from the fire tendrils. The Ayato state carries it: channel version 19.

**Wounds.** Every hit on a player is read by the server (`WoundHits`: the damage type, the attacker's aim, the projectile, the
explosion) and opens its own kind of wound where it landed (`WoundBook.Blow`); much bigger, and deep ones show the body under the
skin (`Anatomy`: ribs, breastbone, spine, shoulder blades, collarbones, pelvis, arm and leg bones, kneecap, skull, guts; skin,
fat and muscle layers in `WoundShape`). New kinds: gash, cavity, fracture (the bone out of the skin), burn, bruise.

![Wounds](docs/images/wounds-anatomy.png)

## 5.0.16-beta: the second wing shows as soon as mastery reaches 100

**Fixed: an Ayato past mastery 100 kept one wing until he used the crystal mode.** A new Ayato has one wing and the twin wings
come at mastery 100, but every client draws the wing form the server last sent (`S2CAyatoState`), and the server only sent it
again when something marked the state dirty: the crystal mode, a kakuja, the Wing Vortex. Reaching 100 (or setting the mastery
with a command) never did, so the second wing waited for V. Now the server remembers the form and mutations it last sent and
sends again the tick either changes (`AyatoServer.tick`), and clients show a one wing they still hold as the twin wings once his
mastery reaches 100 (`WingForm.shown`, used by the wings, the kakuja checks, the abilities board and the Ayato menu). The
abilities board calls a new Ayato's wing "ONE WING" (it said "RABBIT GUISE"). No message changed: the channel stays version
18, so 5.0.16 and 5.0.15 work together.

## 5.0.15-beta: crystal thorns break Kaneki's grab

**An Ayato in the crystal mode can't be held.** When a Kaneki's tails grab him while his wings are crystallized, or he turns the
crystal mode on (V) while they hold him, 44 long crystal spikes (1.5 to 2.7 blocks) break out of every side of his body, through
the tails wrapped round him, and one second after they appear the kagune lets go (`CrystalThorns`, ticked by `AyatoThorns` from
`AyatoServer` after the crystal mode is settled). The release is the original's own (`KaguneServerController.releaseHeldSlot`,
as when a crush ends: his AI, gravity and motion as they were), for every tail of every Kaneki holding him; the first kakuja's
legs end their hunt on him (`CentipedeHunt.finish`). The thorns stay out a moment after he is free (0.8 s): a grab that closes
on them lets go after 0.3 s. Then they sink back into him, or shatter if the crystal thaws (V again, the kagune going in, death).
The thorns do no damage: the escape is the point, and the second they take still lets a crush or a slam land.

Every client lays the thorns out from the same seed (`AyatoThorns` on the client, drawn by `AyatoWingRenderer` as
`CrystalSpike`s riding his body): they snap out over 0.2 s in a ragged wave, up from his shoulders and head, level from his
chest and back, down from his legs, with a crack of amethyst and a flash of red; when the tails let go, crystal and their blood
burst out round him. In first person he sees only the thorns clear of his eyes. Three new kinds of `S2CAyatoFx` carry it, so the
`hexkagune:control` channel is version 18. `CrystalThornsTests` checks the timing (out at once, let go at exactly 20 ticks, the
quick re-grab release, sinking and shattering) and the layout (all round him, outward, never through his far side). Preview:
`tools/art-preview.sh <out> crystal-thorns`.

![Crystal thorns](docs/images/ayato-crystal-thorns.png)

## 5.0.14-beta: Auto Projectile is a tentacle guard

**Auto Projectile (and the Projectile Reflex Organ) guard with a tentacle instead of slapping at the shot.** The original swung
one random tentacle at where an incoming projectile had been, which never looked like it touched it. Now a free tentacle swings
out of its path and holds itself across the line the shots come in on, between them and him, and they stop on it
(`TendrilShield.pose`, applied in `StrikePaths.clearOwner` after the smoothing and before the body clearance). Shots from more
than 50° away bring another tentacle. It now also guards against Ayato's fire shots, crystal spikes and needles, which are
simulated rather than entities, so the original never saw them: `AyatoFire` and `AyatoCrystal` ask `TendrilShieldServer.guard`
at the moment of impact. A stopped fire shot bursts on the tentacle (it doesn't burn him). A stopped spike or needle goes into the
tentacle and stays stuck there as it would in a body (`AyatoCrystal.stickInTentacle`, the stuck-spike message now says which
tentacle): every client keeps it on the tentacle as drawn (`TendrilPaths`, `AyatoSpikes.place`), it falls out when the tentacle
goes back in, and it bursts on time (or in a Crystal Bloom) out of the tentacle, whose guard takes the burst instead of his body.
With Bursting Shards its blast goes off right beside him and catches him in full (a spike bursting out of a body gives its host
only half the blast, on top of the burst itself), so Ayato's explosive build evens out the guard. Arrows and fireballs drop off
the tentacle; with the Reflex Organ they fly back at whoever shot them. The original's `tickAutoProjectile`
and `deflectProjectileImpact` are trampolines to `TendrilShieldServer`; the menu's two descriptions say what they do now.

**A guard, not a wall: Ayato keeps every answer** (`TendrilShieldTests` checks each one against his own numbers):
- a tentacle takes 0.2 s to swing into place and stops nothing before: his spikes from within about 11 blocks and needles from
  within about 17 land first;
- each tentacle's guard holds 6 damage at mastery 0 to 12 at 1000 (half as much again with the Reflex Organ), about three of his
  shots at the same mastery. What a shot has beyond that goes through, so heavy shots punch through a worn guard, and a broken
  guard is knocked aside for 3 s. The Gatling at full spin breaks one in about a second. Guards heal 2 a second;
- two guards at most at once: fans, Spike Rain, crossing shots and flanking get through;
- the guards drop whenever he attacks, grabs, crushes or grapples (the tentacles are needed);
- every stopped shot costs RC: an arrow what the original's deflection did, Ayato's shots by how much the guard took;
- every spike it stops stays in the tentacle: with Bursting Shards each one comes back as a blast beside him (0.8 of its burst,
  more than half the spike's hit), and Crystal Bloom sets them all off at once.

Server settings (`hexkagune-server.toml`, section `combat`): `tendrilGuardStrength` (a multiplier on guard health; 0 turns the
guard off) and `tendrilGuardMax` (guards at once). The guards are synced to every client tracking him (`S2CTendrilShield`), so
the `hexkagune:control` channel is version 17. Preview: `tools/art-preview.sh <out> tendril-guard`.

![Auto Projectile's guard](docs/images/kaneki-tendril-guard.png)

## 5.0.13-beta: Ayato's Wing Block is gone, the kakugan glows in the kakuja, dedicated servers checked

**The Wing Block (C) is removed from the Ayato path.** `server/AyatoBlock` is deleted. The Guard key's trampoline
(`KaguneServerController.toggleGuard`) now lands in `AyatoGates.toggleGuard`: on the Ayato path C does nothing; on Kaneki's it
is the original tendril guard, still refused in his first kakuja. With it go the blocking state (server and client), the
frontal damage cut and its RC drain, the 50 step of the unlock track (the first step after the start is now the twin wings at
100), the abilities board and Ayato menu rows, and everything that drew it: the wing wrapped around the front
(`WingRig`'s block input and root shift), the kakuja's cage of plates (`KakujaRig.guarding`), the shell (`UkakuShell`, and the
first-pass feathered fan in `UkakuFeathers` that only it drew) and the body's guard pose. The Gatling, the Wing Vortex and the
strikes no longer have a block to drop. The Ayato state message lost its blocking flag, so the `hexkagune:control` channel is
version 16: a 5.0.12 client and a 5.0.13 server (or the reverse) are refused at login instead of misreading it.

**The kakugan glows in the kakuja forms (and every leaning stance).** PlayerAnimator draws the head and arms turned by the body
bend about mid-torso (`BipedEntityModelMixin` → `IBendHelper.rotateMatrixStack`), but a layer that places itself with
`head.translateAndRotate` never gets that turn. Ayato's kakuja stance hunches forward 0.16 rad, Kaneki's crouch 0.3, and the
kakuja's running pumps the lean, so the face was drawn about 1.4 to 2.5 pixels in front of where the kakugan was put: the eyes and
their glow (a fraction of a pixel off the skin) were inside the head. `PlayerLayers.upperBody` now gives the layers the same
turn, read from the player's whole animation stack (`BodyAnimations.upperTurn`, the pure `RigPose.upperTurn`), before the
kakugan, Kaneki's mask, Ayato's half mask and wounds on the head and arms are placed. New checks in `IdlePoseTests`; preview:
`tools/art-preview.sh <out> kakugan-lean`.

![The kakugan on a leaning body, before and after](docs/images/kakugan-lean.png)

**Dedicated servers, checked on every build.** `patch/KaguneServerCheck` walks what a server runs (the `@Mod` constructor, the
12 event subscribers Forge registers there, and everything they reach, except the client half of `DistExecutor`) and fails
the build if any of it touches a client-only class (Minecraft's client, Blaze3D, Forge's client package, PlayerAnimator,
bendy-lib, Photon), if a server subscriber's public method names one (Forge reflects over them), or if the bytecode verifier
would have to load one for any method of a class the server loads. It catches each of those when seeded into a copy of the
JAR. The packet handlers check the sending side, hand off to the main thread and validate what clients send; client config
values read on a server fall back to their defaults.

## 5.0.12-beta: the cracks inside the eye glow too

The red cracks across the black of the eye (`KakuganModel.cracks`) now glow as hard as the veins on the skin:
- they're thicker (0.14 px at the iris, tapering to 0.04) and brighter (`CRACK` 0xFF2A24);
- each has a soft glow three times its width (`CRACK_GLOW`, `CRACK_BLOOM`) with a hot core along it (`CRACK_CORE`);
- the glow is drawn at every detail level, not only up close (only a few quads per eye).

The glow still covers well under the tested fifth of the black (8% at most), so the black stays black.

![The cracks inside the eye, before and after](docs/images/kakugan-cracks.png)

## 5.0.11-beta: the kakugan glows harder

`kakugan/KakuganModel` makes every glowing part brighter:
- the iris core (`CORE` 0xFF2410, now covering the iris to 0.1 px from its edges);
- the halo (`GLOW`, strength 0.85 plus 0.2 on the heartbeat, reaching 1.8 px across and 1.1 + 0.4 per eye row down, falling
  off as fall^1.5 instead of fall^2, so it spreads further over the skin);
- the veins (`VEIN_GLOW`) and cracks (`CRACK_GLOW`), brighter and steadier through the heartbeat.

The black of the eye stays black: the halo still leaves the eye out, and the crack glow is as thin as before.

![The kakugan glow, before and after](docs/images/kakugan-glow.png)

## 5.0.10-beta: in the first kakuja Kaneki only walks

`server/KanekiAutonomy` makes the first kakuja the kagune's, not the player's. `refuse` is called at the top of every move key's
trampoline target: M1 (`KaguneCombat.startSwipe`), Rapid Strike (`AyatoVortex.startRapidStrike`), the grapple, grab, throw,
crush, lift and slam (`AyatoGates`) and guard (`AyatoGates.toggleGuard` since 5.0.13; `AyatoBlock` before). In the first kakuja each of them does nothing and
shows a hint on the action bar at most every 3 s. K (the transformation), R, Alt + R and the menu still work.

Each tick, `tick` (called from `ControlServer` after the original controller) lets go of whatever the player was doing with
the tails when the form took over. Bodies he grabbed drop (the legs' own catches stay theirs), a grab, crush, lift, slam or
grapple in progress ends, and the guard comes down. Nothing waits on a key he can no longer press.

The legs keep the original's automatic block and `CentipedeHunt`. They go after mobs targeting him and anything that hit him
in the last 12 s; passive mobs and players who haven't hit him are left alone. The abilities board lists only the legs, the
walk, K and R for this form.

## 5.0.9-beta: playability fixes

- **Kaneki's first kakuja has no M1** (`KanekiForms.autonomous`, gated in `KaguneCombat.startSwipe`). Every M1 goes through
  that method: the attack key, the vanilla punch (which the original cancels and reroutes) and a buffered click. In the first
  kakuja it does nothing and shows a hint on the action bar at most every 3 s. The centipede legs hunt on their own
  (`CentipedeHunt`). Instinct strikes, Z, grab, crush, lift, slam and guard are unchanged. The abilities board drops the M1 row.
- **The first kakuja runs at Speed V** (`KanekiForms.FIRST_KAKUJA_SPEED`; the final kakuja keeps the original's Speed III).
  This is applied in `KanekiFormBuffs.refreshFormEffect`, the trampoline on the original's form buff refresh. Ghoul mode
  (eyes only) carries it over at half, like every movement buff.
- **The Wing Vortex is fire only** (`AyatoVortex.crystallized`). In crystal mode Z is refused with a hint, and a throw still
  winding up when he crystallizes is cancelled. The final kakuja's fire cyclone is untouched.
- **Ayato's kakuja comes out of his back** (`KakujaRig`). The separation pass that keeps neighbouring tentacles apart used to
  apply the clearance of full-grown tentacles to stubs sitting side by side at their sockets. It shoved the newest one about a
  block away, down his back to his feet (below them in the air), and it jumped whenever a new tier started. The clearance now
  follows what is drawn: the width so far, spikes once sprouted, and the root allowance measured along the full length. A
  growing tentacle is also the first part of its own curve, so it pushes straight out of the shoulder blade and curls into
  place, instead of a shrunken copy of its full hook. Fully out, both are exactly as before. New checks in `AyatoTests` fail
  on 5.0.8.

![Ayato's kakuja coming out, before and after](docs/images/ayato-kakuja-pullout.png)

## 4.0.0: Kaneki's centipede rework, the abilities board, one-eyed kakugan

![Both of Kaneki's kakuja with the new legs and mask](docs/images/kaneki-kakuja-forms.png)

**The centipede legs, rebuilt** (both kakuja). The original's 15-block segmented tubes are replaced by plated legs after the
user's reference (`ayato/CentipedeTail`, drawn by `client/render/CentipedeRenderer`): boxy plum plates with a pale rim, red
flesh between them, one pair of black sickle claws a plate, a square fang and jaw claws that open. They are **6.5 blocks at
mastery 750, 7.5 at 1000**, and move by `ayato/CentipedeMotion`: rest poses (the scorpion over his head and the sweep for the
first kakuja; for the final one, out of his hips between the upper and lower tentacle pairs, heads hooking forward), rearing,
the strike arc, the coil round a held body and the guard sweep, each fitted to the leg's length and followed by critically
damped springs solved exactly per frame. They never snap, overshoot or stretch.

**The hunt and the grab** (`server/CentipedeHunt`, in place of the original's autonomous attack). The legs go for the nearest
hostile body within a leg's real reach (5.6 blocks at 750), using the leg on its side, one leg at a time. A **strike** rears
back to tick 3 and lands on tick 5. A **grab** (42% of hunts) rears with the jaw open, bites on tick 7 and wraps the body in
one coil. The leg then hoists it to beside him where he can see it, crushes it one to three times, and slams or throws it.
The caught body is placed every tick where the coil holds it, so it no longer drags through terrain, lags or bobs. A leg
holding a body can't guard.

![The strike and the grab, in game](docs/images/kaneki-centipede-ingame.png)

**The bleed is Ayato's.** Every centipede hit (the hunt's strikes, bites and crushes, and M1 strikes with the legs) bleeds a
body through Ayato's own bleeding (a stack a hit, up to nine) with his Photon blood: drips, trails and pools. A centipede crush
or slam tears a player's wounds like the tails' own.

**The mask** (`kakugan/CentipedeMask`): a crumpled, glossy plum shell over the forehead with a round black lens and red pupil
over one eye, the other eye bare in a window of skin, and a long ridged beak. It follows the player's own eye placement.

**Smoother tentacles.** Kaneki's rinkaku tentacles go through `PathSmoother` before the body clearance: springs per point in
the player's own frame, so his walking and turning carry them exactly as before. Only their own motion is smoothed: quick in
a strike (0.6 ticks), calm at rest (2 ticks).

**No mist on his back.** The original's red mist, puffed at the kagune's root every 8 ticks while it is out, is gone for
Kaneki too (Ayato never had it). The blood as each limb tears out stays.

**The abilities board.** A small card above the chat lists a ghoul's moves with the keys bound in Controls, Forge modifiers
included. It also shows how to open the eyes (R), unleash the kagune (Alt + R) and enter the kakuja (K, tap or hold, with the
mastery each needs). Names and colours follow the path and form: Kaneki's combo is named for his form, crimson in the rinkaku
forms and plum in the kakuja; each of Ayato's fire, crystal, needle and pyre moves has its own colour. It reads the keys twice
a second, rebuilds only when something it shows changes, and hides while any screen is open. **`/kagune hud toggle`** turns
it on and off and saves the choice to the client config (`abilityHud`).

![The abilities board on each path and form](docs/images/abilities-hud.png)

**One eyed kakugan.** The eye screen has a **One eyed** control. It opens a box beside the face asking which eye becomes the
ghoul eye (right, left, or both again), and the face previews the pick under the mouse. Only that eye turns; the other stays
human. The choice is saved with the placement, and older saves read as both eyes. A player whose ghoul eye is the right one
wears the centipede mask mirrored, so the bare window is always round the eye that turns.

![One eyed](docs/images/kakugan-one-eyed.png)

Network protocol "15" (3.0.3 clients are refused at login).

## 3.0.2: the kakuja stop hurting, the Partial Kakuja is gone

**No more damage in a kakuja.** The original re-adds the centipede forms' buffs (Speed, Resistance, Health Boost) every
quarter second, and re-adding Health Boost cut the health back down to 20 each time; ghoul regeneration filled it straight
back in, so a Kaneki kakuja flashed red and tore a fresh wound four times a second. The health is now put back in the same
tick (`server/KanekiFormBuffs`, a trampoline on `KaguneServerController.refreshFormEffect`). Leaving a kakuja can still drop
the four extra hearts once, as Health Boost running out does in vanilla.

**The Partial Kakuja is removed.** Kaneki's forms, a new one every 250 mastery: **Rinkaku Seed** 0 (1 tentacle), **Twin
Rinkaku** 250 (2), **Four-Tail Rinkaku** 500 (4), **Centipede Form** 750 (the two centipede legs: **first kakuja**, tap K),
**Full Kakuja** 1000 (centipede legs and tentacles: **final kakuja**, hold K). The Partial Kakuja has no menu button, never
unlocks and `/kagune form` doesn't know it; a save that was in it loads as the Rinkaku Seed (pick your form again). The menu's
milestones follow: Emergent 0, Responsive 100 (first mutations), Developed 250, Complex organ 500, Centipede Kakuja 750, Full
Kakuja 1000, and the third mutation tier (Multi-Tendril Control, Centipede Body, Regenerative Reservoir) moves from 450 to 500.

| Mastery | Kaneki unlocks |
|---|---|
| 0 | Rinkaku Seed |
| 100 | Mutation tier 1 (Mass & Impact, Reach & Control, Speed & Recovery) |
| 250 | Twin Rinkaku; mutation tier 2 (Arm Wrapping, Auto Projectile, Guard Membrane) |
| 500 | Four-Tail Rinkaku; mutation tier 3 |
| 750 | Centipede Form, the first kakuja (tap K); mutation tier 4 |
| 1000 | Full Kakuja, the final kakuja (hold K); mutation tier 5 |

## 3.0.2: the final kakuja's own kit (bleed and burn)

Needles come from the three right tentacles and **bleed**; fire comes from the three left fire tendrils and **burns**
(`server/AyatoPyre`, every number in `AyatoRules`, section "The final kakuja"). **Scorched wounds** (passive): a needle or a
burst opening a body that is on fire bleeds it twice as hard.

| Input | Move | What it does |
|---|---|---|
| L | **Needle Pair** | The upper two right tentacles each fire a volley of 3 needles. |
| L L | **Fire Lash** | The upper fire tendril whips 3 shards across the target, left to right, one a tick. |
| L L L | **Needle Fan** | All three right tentacles fan 9 needles across the target (±18°), three a tick. |
| L x4 | **Fireball** | One big ball of fire from between the upper two tendrils: it bursts where it lands and leaves the ground burning. |
| L x5 | **Crossfire** | Burning needles from the right (2 each) and a shard (double damage) from each tendril, all crossing on the target. |
| R | **Ember Hop** | The tendrils beat down: everyone within 2.4 blocks burns and is shoved away, he is thrown back, the ground burns. |
| L, R | **Searing Harrow** | A point-blank cone of burning needles (3 per right tentacle, they shove) and 2 fire shards from each tendril. |
| L L, R | **Twin Serpents** | 20 needles sweep right to left over ±60° while 9 fire shards sweep left to right over ±50°. |
| L L L, R | **Pyre Lance** | One thick burning needle through up to 5 bodies; the ground burns where it stops. |
| L x4, R | **Hellrain** | Needles and fireballs thrown high, then 24 needles and 8 fireballs come down round the target. |
| L x5, R | **Crimson Pyre** | Every needle he has stuck in anyone bursts into flame (bodies go up, fire blasts round each wound), and a ring of needles and fire flies out. |
| Z | **Fire cyclone** | The Wing Vortex's fire cyclone, thrown from the fire side as the tendrils whip forward (Fire Dance makes it bigger). |
| J (held) | **Pyre Gatling** | A needle salvo from a right tentacle, then a fireball from a fire tendril, in turn round them, faster and faster. |

Burning needles fly wrapped in Photon fire (`S2CAyatoFx.FLAME_NEEDLE`). The tentacles answer every move
(`client/KakujaMotions`): the side that fires turns on the target and kicks as each needle or shard leaves; the fire tendrils
can aim on their own (`KakujaRig.Action.aimLeft`): they sweep for the Fire Lash and Twin Serpents, beat down for the Ember Hop,
coil and whip forward to throw the cyclone. Preview: `tools/art-preview.sh <out> kakuja-final-kit`.

![The final kakuja's kit](docs/images/ayato-kakuja-final-kit.png)

## 3.0.1: Kaneki's kakuja are his last two forms (tap K / hold K)

(Form order and mastery superseded by 3.0.2 above.)

Kaneki's forms in order: **Rinkaku Seed** 0 (1 tentacle), **Twin Rinkaku** 100 (2), **Four-Tail Rinkaku** 250 (4),
**Partial Kakuja** 450 (4 larger), **Centipede Form** 750 (the two centipede legs: **first kakuja**), **Full Kakuja** 1000
(centipede legs and tentacles together: **final kakuja**). The Centipede Form and the Partial Kakuja traded places; the last two
forms are always the kakuja and their menu buttons are purple. Each form keeps its own limbs, combo and id (old saves load as
they were); damage and the kakuja's hurt reduction go with the place, so each form still hits harder than the one before.
The mastery stage at 750 reads "Centipede Kakuja".

**K works like Ayato's:** tap K (mastery 750) for the Centipede kakuja, hold K (0.4 s, mastery 1000) for the Full Kakuja,
from your form or straight from the Centipede; tap or hold again to turn back to the form you came from. Below 1000 a hold is
just a tap. A kakuja K brought out ends when the kagune goes back in.

![Kaneki's form buttons](docs/images/kaneki-forms.png)

**Player layers on any device.** The kakugan, the kakuja's chest armor and half mask, Kaneki's mask and the wounds are drawn as
layers of the player renderer. On some setups (seen on a phone launcher) none of them showed while the kagune did. They are now
added to any player renderer that turns up without them, drawn right after the player if a renderer skips its layers, and a
layer that fails is logged once and skipped without taking the others down. Skins are no longer trusted when the graphics
driver can't read them back (OpenGL ES translation on phones): the kakugan then uses the eyes placed on the eye screen or the
default ones, instead of eyes "found" in garbage. With **3D Skin Layers** installed, the chest plates and the half mask stand
out past its voxels. The log says which path a game took (lines starting "HexKagune").

## 3.0.0: ghoul mode, wounds and regeneration, the kakugan behind glasses, Kaneki fixes

- **R is ghoul mode:** R alone opens your kakugan without the kagune (R again closes it). **Alt + R** unleashes the kagune;
  with the kagune out, **R** puts everything away (kagune and eyes).
- **Wounds:** every hit tears your body open, carved into it (you look into the flesh): cuts, tears down to the muscle, and
  deep punctures where Ayato's spikes sat; more and bigger the more health you lose, with wet rims and trickles of blood.
  They close as you heal, the newest first, pink as they knit.
- **Ghoul regeneration:** food no longer heals a ghoul (potions still do). After a while without being hurt the wounds close
  and the health comes back: at mastery 0 after 15 seconds, healed 5 seconds later; at 1000 full health within 7 seconds of
  the last hit. Any hit restarts it. It is paid from the flesh reserve; a ghoul with an empty reserve doesn't regenerate. The Regenerative
  Reservoir mutation shortens the wait.
- **The kakugan and your skin's second layer:** the kakugan shows through see-through glasses on the second layer (the
  glass is drawn over the ghoul eyes) and on eyes painted fully on the second layer; nothing is cut away. Your eyes are found
  on your skin if you never placed them ("Find my eyes" on the eye screen).
- **Kaneki:** J lands you exactly on top of the block your tendril grabbed; M1 tentacles never pass through your body; the
  kakuja mask stays on your face through every animation.
- **Ayato:** the first kakuja no longer wears a mask; the final kakuja's fire tendrils burn like his wings (model only).

![Wounds](docs/images/wounds.png)

![The kakugan behind glasses](docs/images/kakugan-second-layer.png)

## 3.0.0: Ayato's final kakuja (hold K)

**Tap K** for the first kakuja, **hold K** (0.4 s) for the final kakuja (mastery 1000), from the wings or straight from
the first kakuja; tap or hold again to turn back. The final kakuja is half his first kakuja and half his wings: **three armored
spike tentacles on his right** (the first kakuja's, unchanged) and **three slim fire tendrils on his left**, riding the very
same living spines, so they sway, stretch, trail on the spring chains and keep clear of each other exactly as the
tentacles do. A fire tendril (`ayato/FireTendril`) is gas, not plate: a slim tube of the :re starry fire, red at the root,
dark navy deepening to the abyss toward the tip, thinning at its silhouette into a lavender-pink glow and densest where you
look straight through it (shaded per vertex from the camera, like the wings), a red core seen through the middle, flame
licks peeling off it and streaming back, a few stars glinting, the tip fraying into a wisp, and a hot glow at the root.
The left sockets on his back burn orange instead of magenta.

Instead of the first kakuja's horned faceplate he wears **Ayato's half mask** (`KakujaModel.halfMask`): black lacquer over
the lower face only, so the kakugan stays bare; drawn to a sharp beak down the middle, its ridge stepped into lips; the
cheeks wrap back round the jaw; a pointed wing sweeps back and up from each cheek with a short spur under it; three
slanted vents a side glow a dim red; steel rivets down the beak and by the wings. Its top edge follows the player's
kakugan eyes wherever they are set and rises to a point over the nose bridge. About 2,790 quads close up, 1,130 far away.

In the game since 3.0.0 (`WingForm.FINAL_KAKUJA`, `ayato/KakujaKey`, `KakujaArmorLayer` draws the half mask); its own kit
since 3.0.2 (below). Preview: `tools/art-preview.sh <out> kakuja-final`.

![The final kakuja](docs/images/ayato-kakuja-final.png)

## 1.3.0: Ayato's abilities spread over mastery

Ayato no longer gets everything at once: his moves come one after another all the way to 1000, so every stretch of mastery
brings something. The keys check it themselves, and pressing one too early says when it comes ("The Wing Vortex unlocks at
mastery 150"). In an M1 chain, a locked ender falls back to the strongest unlocked one, as before. The Ayato menu's **UNLOCKS**
track shows the last steps reached and the next ones with their mastery, the line above it how far the next is, and the
combat readout says "at mastery N" for anything still locked (one table, `ayato/AyatoUnlock`, holding the game's own gates).

| Mastery | Unlocks |
| --- | --- |
| 0 | One small wing: fire shots (M1), Rabbit Hop and Flare (M2) |
| 50 | Wing Block (C); removed in 5.0.13 |
| 100 | Twin wings; Rising Flame |
| 150 | Wing Vortex (Z) |
| 200 | Crystal mode (V): crystal spikes, Crystal Lance, Spike Fan |
| 250 | Wing blades up close; Fire Fan, Spike Rain |
| 300 | Mutation point |
| 350 | Rabbit Dash, Spike Barrage |
| 400 | Gatling (J, in the crystal mode; was 200) |
| 450 | Ukaku speed II, hotter fire |
| 500 | Full-size wings; Black Rabbit, Crystal Bloom; mutation point |
| 700 | Mutation point |
| 750 | First kakuja (K): Needle Storm, Harrow, Piercing Line |
| 800 | Rushing Death (Z in the kakuja), Thorn Fan |
| 850 | Needle Gatling (J in the kakuja), Hail of Thorns |
| 900 | Six-Tail Barrage; mutation point |
| 950 | Crimson Bloom |
| 1000 | Final kakuja (hold K): the first kakuja's kit for now, its own next |

![The unlock track in the Ayato menu](docs/images/ayato-menu-mutations.png)

## 1.3.0: Ayato's mutations

Ayato gets his own mutations, separate from the Kaneki path's tree: **four of them, and he can have all four** (no
branches, nothing locks anything else out). **Each mastery milestone (300, 500, 700 and 900) earns a mutation point**,
spent in the Ayato menu (P): the mutations panel shows the points to spend, every mutation as a card, and a click on one
you can afford unlocks it at once (the server checks, saves it in the profile as the `AyatoMutations` bitmask and says so).
Order is yours. Death keeps them; `/kagune reset` clears them.

| Mutation | What it does |
| --- | --- |
| **Agility Boost** | With the kagune out: Speed four levels and Jump Boost two levels higher than your mastery gives (Speed V, VI from 450 since 3.0.3; Jump Boost III, IV from 750). |
| **Fire Dash** | The right-click Rabbit Hop throws you much further back (1.5 instead of 0.95, higher too, with a bigger burst). **Hold W and right click** (as the first M2, before any M1) to **Rabbit Dash forward** through everything in your way instead. |
| **Fire Dance** | The Wing Vortex's fire cyclone is far bigger (1.7 times as wide, the model 1.6 times its size) and hits 1.5 times as hard. |
| **Bursting Shards** | Every stuck spike or needle that bursts also explodes (2.2 blocks, the burst's damage times 0.8), and every fire shard explodes where it lands (1.8 blocks, 0.4 of its damage, setting fire). |

The forward dash rides on the M2 you already know: the client tells the server W was held with the click (`hexkagune:control`
message 9, action 2) and the 0-click ender becomes the Rabbit Dash. Unlocking is action 3; the mask travels in the Ayato
state. The channel is version 9.

![The mutations panel](docs/images/ayato-menu-mutations.png)

## 1.3.0: K is the kakuja transformation (every path); Ayato's first kakuja — in progress

**K is now the kakuja transformation, on both paths.** It does nothing until a kakuja is available (mastery 750, the
original's Partial Kakuja; it says so). From then on, with the kagune out, K transforms and K again turns back:
- **Kaneki** (superseded by 3.0.1, see the top): the current form becomes the Partial Kakuja (the Full Kakuja from mastery 1000), and back to the form it came
  from (remembered in the profile as `KakujaBaseForm`). A kakuja K brought out ends when the kagune goes back in. Since
  3.0.0 a Kaneki kakuja is any form with centipede legs: the Centipede form counts too, so K takes it back to a form
  without legs and never returns to it. The old
  tendril-count cycle is gone; forms are still picked in the menu as before.
- **Ayato**: the wings give way to his first kakuja and back. The wings now simply follow his mastery (one small wing, then
  the twin wings at 100); Rabbit Guise and the One-Winged Kagune are no longer on K (kept in the code for their own way in
  later). The transformation lasts while the kagune is out. The tentacles grow out one after another (the upper pair first)
  with the armor spreading over him, and turning back regrows the wings.

**Ayato's first kakuja** is not wings: six armored tentacles grow from his shoulder blades, three a side, matched to the kakuja art and the anime
frames. Each is a chain of overlapping dark navy steel plates (each lip riding over the plate before it, a pale ridge down
its back, a cold blue rim on every lip, a jagged spur on both edges of every plate), its open belly glowing deep magenta,
with two staggered rows of long magenta crystal spikes raked toward the hooked claw. The upper pair arches high over the
shoulders, the middle pair spreads like wings, the lower pair sweeps back and hooks forward like pincers. They come out in
turn (upper pair first) and the kakuja's armor grows over the body with them: a dark faceplate with a ridge down the
middle and slanted eye openings where the player's kakugan is (it shows through), two red horns sweeping back from the
brow, and a plated chest (pectorals, stacked stomach plates, ribs) with a spine ridge and six glowing sockets on the back.

**They are alive**, like Kaneki's tendrils. Each one writhes on its own slow rhythm (several drifting sways at once, a wave
running out to the claw), every few seconds stretches out long and curls back with its spikes bristling, and the spikes
ripple. The three on a side breathe together like one wing (each adds only a little of its own), and neighbours never pass
through each other: wherever two would come closer than their armor and spikes allow, both are pushed apart smoothly along
their length, every frame, after everything else has moved them. They answer the body: they stream back as you run and bob
with your stride, tuck in on the way up a jump and spread like wings on the way down, fold close when you crouch, and never
sink into the floor (a claw that would is pressed along it). Standing still, the kakuja has its own stance: hunched, shoulders
up, arms hanging loose, chin down and glaring. On top of that designed pose each tentacle is a spring chain run in the world on the client (the root fixed to the
back, every point chasing its place relative to the one before, looser toward the claw, a little under-damped), so they
trail, swing round when you turn, sag and whip over a jump and overshoot when you stop.

**M1: Needle Storm.** The kakuja fires **needles**: thinner, sharper, smaller and faster than any spike (4.2 blocks a tick,
4.6 at 1000; 32 blocks), in its own magenta, with a faint streak. Each click turns a pair of tentacles on the target (the
upper pair, the middle pair, the lower pair, then the upper and middle again): they swing out, **roll a quarter turn so
their spike rows face where you are looking**, cup forward round the aim, and three needles leave each from the points of
the spikes along its belly, one a tick, so a click is six needles from six spots. Every needle snaps its tentacle open a
little and ripples its spikes. Volleys come every 4 ticks (3 at 1000), faster than the wings ever fired; each needle is
light (0.08 of the melee damage) but sticks for 15 s, bleeds and bursts like any spike. Between volleys the tentacles stay
half turned. The hands thrust in turn.

**M2: the kakuja's enders**, picked like the other kits by how many volleys came before:

| Click | Ender | What it does |
| --- | --- | --- |
| R | **Harrow** | All six slam forward: a point-blank cone of 18 needles (18 degrees, 12 blocks), each shoving hard. |
| L, R | **Piercing Line** | The six converge in front of the chest: one thick needle that runs through 5 targets (1.4 melee, two bleed stacks). |
| L L, R | **Thorn Fan** | 24 needles swept across 120 degrees over 4 ticks, the right tentacles covering the right half, the left ones the left. |
| L L L, R | **Hail of Thorns** | Needles flung high out of all six, then 30 rain down over the target (3.5 blocks round it). |
| L L L L, R | **Six-Tail Barrage** | All six stream needles where you look, two a tick, round the tentacles (the sides take turns), 56 in all. |
| L L L L L, R | **Crimson Bloom** | Every needle and spike you have stuck in anyone bursts at once, 1.6 times as hard; a ring of 24 needles flies out. |

The tentacles act each one out: all six on the target and kicking for Harrow and the Line, the aim sweeping across for the
Fan, the rows turned up to the sky for the Hail, held on the target and kicking in turn for the Barrage, every spike on them
bristling for the Bloom.

![The kakuja firing](docs/images/ayato-kakuja-aim.png)

**Z: Rushing Death.** A crouch, then a tucked backflip high into the air and well back (launched 0.75 back and 0.95 up: at
least six blocks back, about five up), the tentacles pulled in tight through the flip; at the top, the flip done, all six
snap forward onto where you look and **throw twelve needles at once, two each**, the arms flung forward with them; then the
fall, arms out for balance (the original's ghoul durability takes the fall). Six seconds' cooldown.

**J: the Needle Gatling** (held; no crystal mode needed). After a short spin-up all six turn their spike rows on where you
look and fire **salvos of two needles from one tentacle at a time**, round the six with the sides taking turns (never all at
once), from 3 to 12 salvos a second as it spins up. Each needle is light (0.06 of the melee damage) but sticks, bleeds and
bursts. It drains RC every tick, a fifth more than the wings' Gatling and more the longer it runs; it stops when you let go
or the RC runs out. The body braces as for the wings' Gatling.

![The kakuja's kit](docs/images/ayato-kakuja-kit.png)

The kakuja has no wings: V (crystal mode) tells you to turn back. Still to come for it: its own running and jumping
animations.

![The first kakuja](docs/images/ayato-kakuja.png)

![The kakuja alive](docs/images/ayato-kakuja-motion.png)

## 1.3.0: Gatling (J, crystal mode)

**Hold J in the crystal mode** (the original's Slingshot key; Ukaku wings never grappled): both crystallized wings curl
forward over the shoulders and spin up for 6 ticks, then fire a stream of crystal spikes from their tips, the wings taking
turns, at wherever you are looking (still on the target if it is roughly under the crosshair; 3-degree scatter). The
stream speeds up from 4 to 12 spikes a second over two seconds; the wings shudder about their centre lines with it and the
body braces, both palms held out. Each spike is light (0.2 of the melee damage) but sticks, bleeds and bursts like any
other crystal spike, so a held stream fills a target with spikes for a Crystal Bloom. It drains RC every tick, **more the
longer it runs** (0.22 a tick, plus 0.005 for every tick held, times the low-RC multiplier), and stops when you let go,
the RC runs out, the wings leave the crystal mode or go in, or a wing is away as a Wing Vortex.
Outside the crystal mode J reminds you to crystallize first. While it fires, the wings are busy: no M1, no enders, no
vortex. The stream trains the kagune once a second while it hits (not once per spike). The client tells the server the
trigger is held every 4 ticks (`hexkagune:control` message 9, action 1); the channel is version 7.

![J: the Gatling](docs/images/ayato-gatling.png)

## 1.3.0: Wing Vortex (Z)

**Z shoots a wing forward as a vortex.** The body coils and one wing (a random one; one small wing or Rabbit Guise uses its
only wing) twirls up over the shoulder for a quarter of a second, then the arm on that side whips forward and the wing
shoots out as a spinning vortex of RC fire, **still growing out of his shoulder blade**: a narrow jet leaving his back that
opens out like a tornado lying on its side into a big spinning mass at its head (`ayato/WingVortex`: the wing's tongues
wound into broad ribbons around a burning cone, a white-hot core at the front, the pink aura; the :re starry navy from
mastery 250 on). Its head bores 25 blocks out (1.5 to 1.8 blocks a tick), and everything its whole twisting length
touches, within 1.2 blocks (1.55 from 500), is caught once: 1.5 times the melee damage, set on fire for 5 seconds (6 from
450) and thrown hard along it and up, players too. When the head reaches the end of its range or a wall (or he dies,
dismisses the wings or is pulled far from it) the vortex ends: **only then does the wing detach** from his back, unwind and
fade where it is, and a new wing grows back out of the shoulder blade in its place (16 ticks). Cooldown 100 ticks (70 at
mastery 1000, always longer than the regrowth), RC cost 10 times the low-RC multiplier. While a wing is gone, M1 shots and
spikes leave from the other wing. The One-Winged Kagune never throws its blade.

It replaces the Shard Volley, which is gone (its gameplay code, its network fields and its rules; the wing model's volley
pose and flashes stay as unused model features). Network: the `hexkagune:control` channel is version 6 (7 with the Gatling).

![Z: the Wing Vortex](docs/images/ayato-vortex.png)

## 1.3.0: Ayato's crystal mode (V)

**V crystallizes the wings** (the original's Lift key, which Ukaku wings have no use for; mastery 200, twin wings or
Rabbit Guise; the :re One-Winged Kagune is crystal already). The crystal climbs each wing from the shoulder blade to the
very tip in about 1.2 seconds (24 ticks, a steady front, not a crossfade): crystal below the front, the fire still burning
above it, white-hot pink where it is freezing. V again thaws them back down into fire (16 ticks). Dismissing the wings,
dying or changing form ends the mode. While it is on, left and right click fire crystal instead of fire:

| Input | Move | Mastery | What it does |
|---|---|---|---|
| L (up to 5) | **Crystal Spike** | 200 | The wing on that hand's side curls forward and fires one spike; it sticks in what it hits, bleeds it, then bursts. |
| R alone | **Rabbit Hop** | 0 | The same hop as the fire (crystal chips instead of flame). |
| L, R | **Crystal Lance** | 200 | One long lance from both wings (6 blocks more range) that runs through 3 bodies (4 from 750) and sticks in each, 2 bleeding stacks a hit. |
| L L, R | **Spike Fan** | 200 | A tight fan of 7 spikes (9 from 450) across 48 degrees. |
| L L L, R | **Spike Rain** | 250 | Spikes thrown high out of both wings come down on the target and around it (8, 10 from 450, 12 from 750). |
| L L L L, R | **Spike Barrage** | 350 | Both wings machine-gun spikes where you look, one a tick (12, 16 from 750) (30-tick cooldown). |
| L L L L L, R | **Crystal Bloom** | 500 | Every spike you have stuck in anyone within 40 blocks bursts at once, 1.6 times as hard, and a ring of spikes flies out around you (60-tick cooldown). |

**Spikes.** Faster than the fire (2.6 to 3.3 blocks a tick) and much further (26 blocks, 30 from 500; the fire reaches 14
and 18). A spike that hits deals the shot's damage, adds a stack of the original's `hexkagune:bleeding` (heavier every
second stack, up to IV at 7 stacks; up to 11 seconds, renewed by each hit; the icon shows, no vanilla particles) and stays
stuck in the body, in the body's own frame so it turns and moves with it: shoved in at the entry wound (it slams in, quivers
and settles, most of it standing out of the body, drawn a quarter bigger than in flight so it reads), the body's yaw synced
with it so it stays where it went in. It stays **fifteen seconds**, then it bursts: a smaller hit (1 + 0.45 of the spike's), one more stack, crystal shards and a spurt of blood. As the burst comes the
stuck spike glows hotter, white cracks open in it and it trembles. A body carries at most 10 of one Ayato's spikes (48 in
all); more still hit and bleed. Spikes that hit a block stay planted in it for two seconds, then crumble. The trade:
spikes come one tick slower than the fire's shots, for the reach, the bleeding and the burst.

**Blood.** Ported from Hex God of Time (its code and textures, free to use): drops fall from every spike still in a body (from
the body once they have burst), a still body pools where it stands, a running one leaves a trail; a spike going in or
bursting throws a sheet of blood, gobs and a fine spray. About half the drops leave a mark where they land; puddles widen as
blood runs into them, shine while fresh, and darken as they dry (`client/fx/AyatoBlood`, `client/render/BloodStains`,
`textures/blood`, `textures/particle/blood_*`). **When Hex God of Time is installed it already draws the original's bleeding
itself, so the steady bleeding is left to it** and only the spurts are drawn here: blood is never doubled.

**Mutation hook.** `AyatoCrystal.burstingSpikes(player)` is where the Bursting Shards mutation switches on (see Ayato's
mutations above): with it, every bursting spike also explodes for extra damage around it (2.2 blocks, the burst's damage
times 0.8), with its own crystal blast.

Network: the `hexkagune:control` channel is now version 5 (the crystal mode in the Ayato state, stuck spikes and bleeding
per body in message 10, the crystal's effects in message 8).

![V: the crystal climbing the wings](docs/images/ayato-crystal.png)
![Crystal spikes: flying, the lance, stuck in a body, about to burst, the barrage](docs/images/ayato-spikes.png)

## 1.2.3: Ayato's fire M1 and right-click enders

**Controls (Ayato, wings out).** Left click fires; up to five shots make a chain, right wing then left wing. A right click
ends the chain, and *which* ender you get depends on how many shots came before it:

| Input | Move | Mastery | What it does |
|---|---|---|---|
| R alone | **Rabbit Hop** | 0 | One downbeat of the wings throws him back out of reach (16-tick cooldown). |
| L, R | **Flare** | 0 | Both palms thrust; the wings hurl one big fireball that bursts and splashes (2.6 blocks), leaving embers. |
| L L, R | **Rising Flame** | 100 | A pillar of RC fire erupts under the target and throws everything in it into the air. |
| L L L, R | **Fire Fan** | 250 | Both wings sweep across and spray a fan of 7 shards (9 from 450). |
| L L L L, R | **Rabbit Dash** | 350 | About six blocks straight through, wings streaming fire, burning everything he passes (30-tick cooldown). |
| L L L L L, R | **Black Rabbit** | 500 | A backflip about three and a half blocks up, then he hangs on his wings and rains a barrage of 10 shards (14 from 750) (60-tick cooldown). |

A locked ender falls back to the strongest unlocked one below it (a chain always ends in at least the Flare). Right click
stays the normal use when that is clearly meant: crouching, holding something with a use (food, a bow, a shield), or
pointing at a block (a door, a chest) outside a fight. Holding the button does not repeat the ender. A right click
pressed while a shot is still playing waits for it, so mashing L L R works.

**The shots.** On each left click the wing on that hand's side keeps its full size and spread and curls forward in one
smooth arc from the shoulder blade until its tip points where you are looking (a real bend of the wing, `UkakuModel.Bend`,
not a stiff turn, and never folded thin or shrunk). It fires from the tip, flares a little as the fire leaves, and eases
back to a half-curled ready stance while the chain is live; the next shot curls on from there, so nothing snaps. The
same-side hand thrusts forward with it. In first person the curled tip shows in the upper corner of the screen. A
shot is a shard of the wing's own fire (`ayato/RcShard`: a white-hot spindle, five licking tongues, a red vein, the pink
aura), simulated on the server and flown on every client from its start and velocity (`hexkagune:control` message 8).

**Particles.** Every particle of Ayato's fire is a Photon particle (`client/fx/RcFx`, ported from Hex God of Death's
`DeathFx`), using Hex God of Death's flame, ember, mote, puff, ring and vein sprites (`textures/particle/`) tinted in the
wing's palette. No vanilla particle types are left in Ayato's M1. Shards shed fire and embers as they fly, burst into a
flower of fire with a ring (and red veins on a hit), the pillar pours fire upward, the dash leaves a trail.

Network: the `hexkagune:control` channel is now version 4 (fire effects and the right-click action).

![The M1 chain and the enders, off-game](docs/images/ayato-moves.png)

## 1.2.3: idle stances rebuilt on PlayerAnimator's real conventions
The 1.2.1/1.2.2 stances were posed with three conventions backwards, which is why they looked broken in game. A positive
bendy-lib arm bend folds the forearm *backwards*, so Ayato's "hand in pocket" flipped out behind him. A torso rotation or
torso bend moves the torso mesh *alone*, so every lean and hunch tore the arms and head off the shoulders. A twist did the
same to the shoulder pivots. All posing now goes through `client/animation/RigPose`, the single place that maps physical
channels (lean, elbow and knee flexion, chest twist, body drop) to PlayerAnimator. A lean is the `body` bend, which carries
the head and arms; elbows are written negated; twists carry the shoulder pivots.

- **Ayato, relaxed** (reference: Jotaro's stance): leaning back a little, chin up, right hand in the front trouser pocket
  with the elbow back, left forearm loosely raised at the waist, weight on the right leg.
- **Kaneki, animalistic** (reference: Bakugo's fighting crouch): a wide, low stance on bent knees with the soles planted,
  the upper body hunched, shoulders up, elbows out and both hands forward like claws, head up to watch.
- **Only the Kaneki path is feral.** Ayato no longer gets the low-control posture (hunching, trembling, spasms, clutching,
  stumbling, the manifestation pain pose) at any mastery; instinct urges only turn his head.
- The original manifestation pain pose (Kaneki) now doubles over through the body bend, so the arms stay attached.

`tools/pose-preview.sh <out-dir> [idle closeup]` renders the stances off-game with the exact PlayerAnimator 1.0.2 and
bendy-lib 4.0.0 transforms (`tools/PoseRender.java`, including bendy-lib's bend and bulge formula). `tools/ArmSolve.java`
finds the limb angles that put a hand, elbow, knee or sole where a reference picture has it.

![Idle stances, rendered with the game's transforms](docs/images/idle-stances.png)

## 1.2.0 (in progress): choose your kagune path
The original progression becomes the **Kaneki path** (Rinkaku → Centipede → Kakuja). A second path, **Ayato**
(Ukaku wings, "Black Rabbit"), is being built one step at a time; its canon research and full design are in
[docs/AYATO_PATH.md](docs/AYATO_PATH.md). The Ayato path is **canon-only**: every move is one Ayato uses in the
manga or anime, the look follows his eras (*Tokyo Ghoul* / *√A*, then *:re*), and it ends at mastered *:re* Ayato
because he never has a kakuja.

Done so far:
- **Path data.** Each profile stores its path (`Path` inside the existing profile NBT), so death keeps it and
  `/kagune reset` clears it. Players who already grew their kagune (any mastery, mutation or grown form) are
  quietly kept on the Kaneki path. An unlocked but untouched kagune is marked *undecided* and will get the
  starting screen.
- **Commands.** `/kagune path` shows your path. Operators can use `/kagune path set <player> <kaneki|ayato|none>`
  (`none` sends the player back to the starting screen). A manifested kagune is retracted when its path changes.
- **Starting screen.** *Choose your kagune* opens by itself as soon as a kagune is unlocked without a path (on
  unlock, on login, after `/kagune reset` or `/kagune path set <player> none`). It waits politely if chat, an
  inventory or the pause menu is open. Until a path is chosen the kagune **cannot manifest** (the manifest key
  explains why and reopens the screen) and the HexKagune menu key opens the choice instead of the kagune menu. Two cards (Kaneki: crimson Rinkaku tendrils; Ayato: vermilion Ukaku wings that
  flicker when gaseous and crystallize when selected) show each path's style, its progression strip (hover a node
  for the stage name) and how it ends. Pick with the mouse or arrow keys, then **hold** *Awaken* (or Enter) for about
  a second, because the choice is permanent. Esc or *Decide later* closes it. The layout adapts down to Minecraft's
  smallest GUI (320x240), stacking the cards on narrow windows. The server accepts a choice only from an unlocked
  player with no path yet.

  ![Path choice screen at 1080p, GUI scale 4, Ayato selected and awakening](docs/images/path-choice-1080p.png)

  Off-game mock-ups made from the real layout and silhouette code (stand-in font):
  `java -cp build/classes tools/PathScreenPreview.java docs/images` after a build.
  [Smallest GUI (320x240), nothing selected](docs/images/path-choice-smallest.png).
- **Ayato's wings.** An Ayato kagune manifests as Ukaku wings from the shoulder blades instead of tendrils (the
  tendril renderer and the writhing sounds skip Ayato players). The shapes copy the anime: at rest the wings are
  RC flame (a molten red-orange core, dark crimson flame tongues and a pale pink aura); to strike, block or fire they
  crystallize into jagged blades (dark, starry violet inside, red edges, pink rim). The look follows his eras:
  *Tokyo Ghoul* / *√A* wings up to mastery 749, much larger *:re* wings from 750, and the *:re* **One-Winged
  Kagune** (one huge faceted crimson crescent). *Rabbit Guise* shows the left wing only. Every client learns each
  Ayato player's wing state from the server (`hexkagune:control` message 4).
- **Ayato's close-range combo (M1)** *(superseded in 1.2.3: the twin and Rabbit Guise wings now fire, see above; the
  One-Winged Kagune keeps its blade combo).* Kicks and point-blank shard bursts at first; from mastery 250 (*Wing
  Blades*) the hardened wings cut like blades between kicks (Right Wing Blade → Left Wing Blade → Kick → Cross Cut);
  *Rabbit Guise* fights one-sided; the *:re* One-Winged Kagune swings One-Wing Slash → Return Slash → Beheading Cut.
  Right after a Shard Volley, an untrained Ayato's crystallized wings sit out (kicks only), as in canon. Damage
  follows his stages (`6 + 0.006 × mastery`, ×1.0 to ×1.8), strikes cost 1.6× the Kaneki RC at first (his low RC
  supply) falling to 1× at mastery 1000, and the combo runs on the same server-authoritative timing, hit volumes
  and input buffer as 1.1.0 (it stays on for Ayato even if the combo config is off, since it is his whole kit). Each
  strike has its own body animation (high roundhouse kicks with the guard up, cuts that carry the chest across, the
  burst's recoil, the beheading cut's rise and lunge) and leaves RC in the air: red arcs where a wing cut, molten
  streaks from the shoulder blades for shards.
- **Shard Volley (Rapid Strike key)** *(replaced in 1.3.0 by the Wing Vortex, see above)*. The wings crystallize and fire RC shards in waves, 10 ticks apart (vanilla
  hurt immunity, so every wave can land on the same target): 6 shards in 2 waves at first, up to 16 in 3 waves at
  mastery 1000, faster (2.2 to 2.8 blocks per tick) and tighter (13° to 8° cone) with mastery; one wing (Rabbit
  Guise) fires half as many. Shards leave from points along the drawn blades, aim at the target under the crosshair
  (tracked between waves) or at whatever the crosshair points at, never start inside a wall, and deal whole damage
  (3 at first, 8 at mastery 1000) for over 28 blocks of flight. A volley costs 9 RC × the low-RC multiplier and
  recharges in 44 ticks (30 at 1000). The One-Winged Kagune is a blade and does not fire (canon). The shards are the
  original mod's spike projectile, which the 1.0.0 code never spawned, reshaped from a stubby 1.6-block javelin into
  a thin 1-block crystal needle (as in the anime); they leave a red dust trail and never stick in the target. Hits train mastery (once per volley) and feed bloodlust.
- **Ukaku passives.** While the wings are out: Speed I (Speed II from mastery 450) and Jump Boost I (II from 750),
  refreshed exactly like the original Centipede form buffs (short, ambient, never overriding a stronger potion).
- **Kaneki-only mechanics are off on the Ayato path.** The grapple, grab, throw, crush, lift and slam keys and Threat
  Auto Attack refuse with a short reminder ("Ukaku wings don't grab or grapple"), and no tendril catches his falls
  (fall damage is still cancelled for any manifested kagune, as before). His original profile keeps the neutral
  Rinkaku seed with no mutations, so no Kaneki-only system (Centipede autonomy, autonomous mutations, form buffs)
  runs for him. A player switched to Ayato by `/kagune path set` keeps their Kaneki form, mutations and Threat Auto
  setting parked in the profile (`KanekiStash`) and gets them back if switched to Kaneki again.
- **Wing Block (Guard key; removed in 5.0.13, C now does nothing on the Ayato path).** The crystallized left wing wraps around the front (canon: he blocks Touka's kagune
  with one wing). Melee hits and projectiles from the front (within 70° of where his body faces) lose 50% of their
  damage, 75% at mastery 1000; falls, fire and magic go around it. Raising it costs a quarter second of upkeep, the
  upkeep drains 0.3 RC per tick × the low-RC multiplier, and every stopped hit costs RC in proportion to the damage
  it stopped. It drops when the RC runs out, when he strikes or fires (the wing is needed), or when the wings go
  away. The original omnidirectional tendril guard never runs on the Ayato path. In first person the wrapped wing
  is drawn faint so the view stays clear; the body turns its left shoulder into the attack behind it.
- **Wing forms (tail-cycle key).** Twin Wings → *Rabbit Guise* (mastery 100: the right wing retracts, as when he
  passes as Touka) → the *:re* **One-Winged Kagune** (mastery 750) → back to the twin wings. The choice is stored in
  the profile (`AyatoWingForm`, kept on death, cleared by `/kagune reset`), works with the wings put away too, and
  shows on every client with a quick reshape of the wings. Each form has its own close-range combo (above); the
  One-Winged Kagune does not fire shards.
- **Network.** The `hexkagune:control` channel moves to version 2 and syncs each player's path to everyone
  tracking them. 1.1.x and 1.2.x are refused cleanly at login if mixed.

## 1.1.0: learning to control the kagune

Progression is no longer just "unlock another tail". A new ghoul's kagune is a dangerous organism they can barely
command; a master's kagune is an extension of their body. Everything below scales **smoothly** with a numeric
*control* value, not with arbitrary stages.

### Control, bloodlust and hunger
- **Control (0–100%)** comes from existing mastery (`mastery^0.85`, so mastery 1000 alone is full control and old
  saves keep their progress), plus up to +18% from **Restraint** practice and small bonuses from the *Reach & Control*
  and *Multi-Tendril Control* mutations. Restraint is earned by restraining instinct urges (crouch), enduring a full
  manifestation while untrained, and calming bloodlust by feeding properly. It is stored in the profile's existing
  `ActionMastery` NBT (key `Restraint`), so there is no save-format change.
- **Bloodlust (frenzy)** spikes when the kagune tears out, when you hit, kill or get hurt, and settles over time —
  faster for masters. Starvation keeps an untrained ghoul on edge.
- **Hunger** comes from the **flesh reserve** and feeds into everything above.

**Flesh reserve (3.0.3).** A ghoul has no food bar: in its place is a rinkaku tail full of blood, rooted in a kakuhou whose
kakugan watches you (it blinks, its pupil widens as you starve, the tail writhes harder, and the kakuhou beats like a heart
when you're low). Only raw flesh fills it (raw beef: 4.5 of 20); every point of health you regenerate is paid from it (half a
point each, so healing back from one heart costs about half the reserve), the kagune out sips at it (`manifestHungerDrain`,
0 turns that off), and human food your body rejects costs 2. Empty, you don't regenerate; below 14 the hunger behind bloodlust
and instinct grows. It refills on respawn. Vanilla hunger is held still under it, so you can always sprint and never starve.

![Flesh reserve](docs/images/reserve-bar.png)
- `/kagune control` shows your control, mastery, restraint, hunger, bloodlust, instinct and combat style.
  A thin crimson-to-bone line under the RC bar shows the same at a glance (client config `controlIndicator`).

### Ghoul diet
While HexKagune is unlocked, ordinary food no longer nourishes you. It is still eaten, but its food and saturation are
undone and a **particle-free Hunger effect** is applied as your body rejects it ("it tastes like ash"). Raw flesh
nourishes a ghoul about as well as cooked food nourishes a human (and its own Hunger side effect is removed); coffee and
water-like drinks are tolerated but useless. Detection is data-driven and mod-friendly:
1. `hexkagune:not_ghoul_food` (force reject), `hexkagune:ghoul_food` (force accept — includes vanilla raw meats/fish,
   rotten flesh, spider eye and the optional common tags `forge:raw_meats`, `forge:raw_fishes`, `forge:raw_beef`, …),
   `hexkagune:ghoul_tolerated`;
2. otherwise a name heuristic (`raw_*`, flesh/fish names, `FoodProperties#isMeat`) rejects anything cooked or prepared.

Keeping a kagune manifested slowly burns food (more tails cost more; mastery reduces it).

### Instinct (involuntary actions)
When instinct is strong enough (low control, amplified by hunger and bloodlust), the kagune occasionally acts on its
own toward nearby prey: a **glance** (your view is pulled toward it), an aggressive **step**, a short **lunge**, or —
after a visible tell — a forced **strike**. Each lasts under a second, urges are at least ~5 s apart, and you can fight
them: steer away, or **crouch to restrain** (chance improves with control; successful restraint trains Restraint).
Forced attacks only ever target hostile mobs, recent attackers or (when starving) unowned, unnamed animals — never
players, villagers or pets; people only draw glances. A fed, calm ghoul at mid progression (mastery ~450) is in
control and only breaks under bloodlust or starvation; above roughly mastery 750 instinct never breaks through, even
starving and frenzied.

### Kagune combat (M1)
Each form fights differently, with multi-stage combos that alternate naturally and authoritative multi-target hit
shapes resolved on the server at the impact tick (each entity is hit at most once per strike; chained impacts always
land outside vanilla's 10-tick hurt immunity, so faster combos never stack damage):

| Form | Style | Stages |
|---|---|---|
| Rinkaku Seed | Whip | Lash → Backlash → Whip Crack (long pierce) |
| Twin Rinkaku | Scissor | Right Lash → Left Lash → Scissor (both tails, wide) |
| Four-Tail Rinkaku | Flurry | Jab → Cross Jab → Low Sweep (lower tails) → Quad Impale |
| Partial Kakuja (450) | Blade | Rising Slash → Cross Slash → Tail Pierce → Whirlwind (360°) |
| Centipede Form (750, first kakuja) | Heavy | Crash (slam AoE) → Scythe (150° sweep) → Drill (both legs) |
| Full Kakuja | Predator | Cleave → Flurry → Crash → Devour (four-tail impale) |

Untrained strikes are slower, wander a little off-line and recoil; mastered strikes are fast, precise and chain over
a longer window. Clicks are buffered for a few ticks so combos flow without animation locks; you can move, jump and use
every other ability as before. Threat Auto Attack, Rapid Strike and all other actions keep their original behaviour.

### Body and screen
- A PlayerAnimator layer gives the body a kagune posture that tracks control: painful, shaking, spasming activation,
  ragged breathing, clutching, stumbling and sudden head movements early on; a calm, deliberate stance later. The
  original manifestation pain pose is kept and scaled by control. Strikes are driven by the torso, shoulders, hips and
  arms in sync with the server's timing, with a distinct shape per stage.
- Tendrils trace per-stage paths (lash, backhand, overhead crash, thrust, rising slash, spin, scissor, impale), several
  limbs strike together on finishers, and low control makes them wilder.
- Bloodlust draws pulsing dark-red veins at the **edge** of the screen (never a full-screen filter); first-person camera
  feedback (activation shake, strike kick) respects Minecraft's *Distortion Effects* slider.

### Configuration
`serverconfig/hexkagune-server.toml` (per world): `ghoulDiet`, `manifestHungerDrain`, `involuntaryActions`,
`involuntaryAttacks`, `instinctStrength`, `kaguneCombos` (false restores the original single swipe).
`config/hexkagune-client.toml`: `bodyAnimations`, `bloodlustOverlay`, `overlayOpacity`, `cameraEffects`,
`cameraEffectStrength`, `urgeViewPull` (motion comfort), `controlIndicator`.

Operators can test with `/kagune mastery set <0-1000>`, `/kagune control frenzy <0-1>` and
`/kagune control urge <glance|step|lunge|strike>`.

### Compatibility
- **Worlds and saves** from 1.0.x load unchanged (no registry changes, no schema change; Restraint starts at 0 and
  control is derived from existing mastery). Downgrading ignores the extra `Restraint` key.
- **Multiplayer**: the expansion uses its own `hexkagune:control` channel (version 1, required on both sides), so a
  1.1.0 client and a 1.0.x server (or vice versa) are refused at login with a clear channel mismatch instead of
  failing mid-game. The original `hexkagune:main` channel and its packets are unchanged. All damage, urges and pushes
  are server-authoritative; clients only animate.

## 1.0.2 changes (kept)
- Remove dust and Photon wisp particles along the physical kagune limbs; manifestation effects at the back remain.
- Staged, tree-clearing grapple leap (2.7 blocks/tick climb, then 4.4–6.0 horizontal), free of RC cost, with an
  independent 10-tick grapple cooldown.

## Build

Prebuilt JARs: every push to a branch publishes a pre-release named `build-<branch>` on the repository's
Releases page (for this branch: `build-claude-wonderful-bardeen-9blpsl`).

With Java 17+ and Python 3 installed (no downloads):

```sh
python3 build.py
```

Output: `build/hexkagune-4.0.0.jar`. The build:
1. checks the SHA-256 of the original JAR and applies `patch/KagunePatch.java` (1.0.2 changes, access widening and
   straight-line trampolines/redirects into the expansion: `startSwipe`, the SWIPE tick, tendril strike paths and
   active-limb selection, the manifestation pose, and mod start-up);
2. compiles `src/main/java` against the patched JAR plus compile-only API stubs in `stubs/` (never packaged);
3. assembles the JAR (new classes, textures, item tags, merged language keys);
4. runs `patch/KaguneVerify.java`: the 1.0.2 contracts, every new hook, a type-checking analysis of every new or
   changed method, and an audit that every Minecraft/Forge/library member or class the new code touches is either
   already linked by the original JAR or listed in `verify/api-allowlist.txt` with its verification source
   (MCPConfig/Yarn SRG join, Forge 1.20.1 sources, library API);
5. checks that only the intended original entries changed;
6. runs `patch/KaguneServerCheck.java` (5.0.13): nothing a dedicated server runs touches a client-only class, names one in an
   event listener's public signature, or makes the verifier load one;
7. runs the pure-logic test suites in `src/test/java` (progression curves, instinct thresholds, food classification,
   combo tables, combo pacing and balance, strike paths, camera math).

This is packaging and bytecode verification. An in-game test still requires a Forge 1.20.1 client and server with
PlayerAnimator, bendy-lib and Photon installed — see the test checklist below.

## In-game test checklist
- **4.0.0 first:** Kaneki at 750 (tap K) and 1000 (hold K):
  - The legs. New plated legs, no 15-block tubes. The final kakuja's legs leave his hips between the upper and lower
    tentacles. Run, turn, jump and crouch: no snapping, no stretching.
  - The hunt. Stand near a zombie: the leg on its side strikes it (rears, then the arc lands). Sometimes it bites, coils,
    hoists it beside you, crushes it and slams or throws it. The body never drags through blocks.
  - Bleeding. The bitten mob bleeds like an Ayato spike target: drips, trail, puddle.
  - The mask. One eye bare, the lens over the other, the beak; with the right eye chosen as the ghoul eye, it's mirrored.
  - The kagune. No mist puffs on his back; the tentacles move smooth at rest and still land quick in an M1.
  - A second player sees the same.
- **4.0.0 board:**
  - The card above the chat lists your keys. Rebind one in Controls (with a modifier): the card follows within a second.
  - R (eyes), Alt + R (kagune), K forms: names and colours change (Kaneki rinkaku crimson, kakuja plum; Ayato's crystal
    mode and kakuja).
  - Open chat or any screen: it hides. `/kagune hud toggle` twice: off, then on, and it stays after a relog.
- **4.0.0 eyes:** `/kagune eyes`, One eyed, pick Left eye, Done. Open the kagune: only the left eye turns; the right stays
  your skin's. Again with Right eye, then Both eyes. Find my eyes keeps your one-eyed choice.
- **3.0.2 first:** Kaneki in either kakuja (750 Centipede, 1000 Full) standing, running and fighting for a minute: no red
  flashes, no fresh wounds, no hearts flickering (leaving a kakuja may drop the four extra hearts once). The menu reads
  SEE DUA QUA CEN FUL; Twin unlocks at 250, Four-Tail at 500, the third mutation tier at 500; `/kagune form partial` is
  unknown; a save left in the Partial Kakuja loads as the Seed.
- Each form (`/kagune form <seed|dual|quad|centipede|full>`), each at mastery 0, 500 and 1000
  (`/kagune mastery set`): activation pose and edge veins, idle body, full M1 combo, hit counts on groups of mobs.
- Starving (food ≤ 6) versus fed: instinct frequency, crouch restraint, bloodlust overlay, raw versus cooked food.
- Toggle the kagune repeatedly; attack while sprinting, jumping, falling, swimming and grappling; first and third person.
- Dedicated server with several players using different forms: each sees the others' body animation, tendril paths
  and combos; no duplicate damage, rubber-banding or teleporting; mixed-version clients are refused at login.
- Ayato crystal mode (mastery 200+): V with the wings out (and below 200: the locked message); watch the crystal climb
  from the shoulder blades to the tips, then V again to thaw. M1 spikes at a mob 20+ blocks away; spikes stuck in it turn
  with it, glow and crack before they burst; it bleeds (icon on players; blood drops, trail pools, a puddle when it stands
  still). Each ender (L R .. L L L L L R), the Bloom with several spiked mobs around. Once with Hex God of Time installed:
  one set of blood only.
- Ayato Z (Wing Vortex): the wing twirls up and shoots out as a vortex still attached to the shoulder blade; it hits a line
  of mobs (they burn and fly), and mobs walking into it; at 25 blocks or a wall it detaches, fades, and the wing grows back;
  Z again on cooldown does nothing; M1 right after it fires from the other wing; Rabbit Guise uses its one wing.
- Ayato J (Gatling): outside the crystal mode the reminder; in it, hold J: spin-up, then a stream that speeds up; let go and
  it stops at once; hold until the RC runs out; Z refuses it, C does nothing; mobs fill up with spikes and bleed.
- K below mastery 750 (the message), with the kagune in (the message). Kaneki menu: the forms read SEE DUA QUA CEN FUL,
  CEN and FUL purple; 750 is the Centipede Form. Kaneki at 750: tap K into the Centipede
  kakuja and back to the form it came from; at 1000: hold K into the Full Kakuja (from your form or the Centipede), tap or
  hold to come back; a hold below 1000 is a tap; a K kakuja ends when the kagune goes in; turning back never lands on a kakuja.
- Ayato first kakuja (mastery 750, K): the tentacles come out upper pair first and the armor grows with them (mask with the
  kakugan showing through its eye holes, horns, chest plates, sockets on the back); stand still and watch them writhe and
  stretch; sprint, stop dead, turn fast, jump, crouch; walk along a wall and up stairs (no claw through the floor); first
  person (faint); a second player sees the same; V refuses with the "switch back to your wings" message.
- Ayato kakuja kit: M1 at a mob 20+ blocks away (pairs turn, rows face the crosshair, needles stick for 15 s and are shoved
  in visibly); each ender (R .. L L L L L R); Z (Rushing Death: backflip, twelve needles at the top, cooldown); hold J (the
  Needle Gatling, one tentacle at a time).
- Ayato unlocks: at mastery 0 press Z and (in the crystal mode, at 200) J: each says when it comes (C does nothing, no message); at 760 in the kakuja
  Z and J say 800 / 850, and five volleys then a right click give the Piercing Line; the P menu's track at 0, 300, 760, 1000.
- Ayato final kakuja kit (mastery 1000, hold K): M1 five times at a mob (needle pair from the right, a fire lash from a left
  tendril sweeping across, a needle fan, a fireball that bursts and leaves embers, the crossfire of burning needles and
  fire); a burning mob's bleeding climbs faster; each ender after 0..5 shots (Ember Hop throws you back out of a burst of
  fire, Searing Harrow, Twin Serpents sweeping opposite ways, the Pyre Lance with embers where it stops, Hellrain, the
  Crimson Pyre with needles already stuck in mobs); Z throws the fire cyclone from the left as the tendrils whip forward;
  hold J (needle salvo, fireball, in turn). Run, jump and land in either kakuja: the body pumps and tucks, the tentacles and
  the chest armor lean with it.
- Ayato mutations: at mastery 299 / 300 / 900 the points in the P menu; unlock each, relog and check they stay; Agility
  Boost (speed and jump with the kagune out); Fire Dash (right click throws further; W + right click dashes forward); Fire
  Dance (a bigger, harder cyclone on Z); Bursting Shards (fire shards and bursting spikes explode); `/kagune reset` clears.
