# Path design: Bikaku (the worm kagune with the mouth) — in the game since 3.4.9, being built

The owner asked for an entirely new playable path (after 3.4.8), a Bikaku kagune modelled on Noro from Tokyo Ghoul. This file holds
their spec word for word, what the reference pictures show, what is built so far, the plan, and every place in the code a new path
hooks into (found while starting it). **3.4.9 put the path in the game**: choosable, the worm drawn (in the owner's colour, alive, long),
its M1 bites with Predatory Feeding, its card, menu and move guide. Its other moves, passives and kakuja are still to come (the plan below).

Visible names follow the house rule from 1.2.5 (no character names on screen: the paths show as "Rinkaku", "Ukaku"): the path is
**Bikaku** (id `bikaku`). "Noro" is not in the banned list (`build.py` `FORBIDDEN`), but keep it out of visible text all the same.

## Built so far

**3.4.9 (in the game):** `KagunePath.BIKAKU`; `bikaku/BikakuRules` (the M1 string Snap/Lunge/Maul, reach 6.5, damage, bleeding, the
Maul's stun, Predatory Feeding, the plan's gates); `MawMotion.of` (a bite in three styles, or rest), `path` (thrown out it uncoils from the
head back only as far as the bite needs, the rest staying in zigzags behind; nothing through his own head or torso), `grown`,
`firstPerson`; `server/BikakuServer`, `network/S2CBikakuState` (54, protocol 94), `client/BikakuClient`, `client/render/BikakuRenderer`,
`client/BikakuMenuView`/`BikakuMenuScreen`, `PathCards`/`PathSilhouette.bikaku`, `AbilityGuide.bikaku`; every hook below wired (the
original's tendrils hidden, its other keys saying "Your worm only bites for now"). Previews `docs/handoff/bikaku-*-3.4.9.png`; tests
`BikakuTests`, `BikakuWormTests`. Details: the top of `docs/handoff/companions-handoff.md`.

**3.5.5 (the owner's report: the worm through the player's body):** every worm is drawn through `MawMotion.around` (its curve swept
round his head, torso and arms, and legs, the shorter way, smoothed where moved; unchanged elsewhere); Predatory Drag comes down his
right side (`GROUND_SIDE`) to before his right foot (`GROUND_ANCHOR`), holds from before his right shoulder (`DRAG_SHOULDER`) and drags
its catch to its own side (`dragSide`). Details: the top of `docs/handoff/companions-handoff.md`.

**3.5.6 (the owner: "why not make a bigger angle? it's going too close to the body"):** Predatory Drag's worm swings out in a wide arc
round his right side (its spots widened, its coils spread from there, its mouth gripping from its own side, wide on the way back).

**3.5.7:** the kakuja's own moves on Z, G, B, C in it (Thousand Jaws, Endless Hunger, Flesh Cataclysm, Undying Abomination) and
Autonomous Predation (its tentacles biting hostile bodies on their own); its other passives were in since 3.5.4. Previews
`docs/handoff/bikaku-kakuja-moves-3.5.7.png`, `bikaku-kakuja-undying-3.5.7.png`.

**3.5.8 (the owner's range, first half):** M1 and the worm's thrown moves are held: wound up while held, thrown as far as they were held
for (a tap as before, 30 blocks after 2 s), every beat slower for a longer throw, nothing else until it is back. Round blocks: 3.5.9.

**3.5.9 (the owner's range, second half):** the worm goes round blocks to what it goes after (`WormRoute`, found by the server, sent to
everyone, the worm laid along it with `MawMotion.along`) and pulls what it catches back round them; M1, Maw, Drag (along the ground),
Pursuit. Next: the forms.

**3.6.0 (the forms, first part):** One Worm, Twin Worms (250), Three Worms (500) picked in the menu (The Walker, 750, shown, not built);
the worms each in their own lane; M1's bites take turns. Next: 3.6.1 every move by all the worms; 3.6.2 The Walker.

**3.6.1 (every move by all the worms):** in Twin and Three Worms each worm makes the move from its own lane, a beat after the one before
(`BikakuMove.stagger`: Maw and Drag 3 ticks, Pursuit 2, Eruption 6), after its own body (the aimed one first, then the nearest fair ones
in front in reach; Pursuit by side; fewer bodies: the rest go for the first, one worm holding it, the others biting it). Maw and Drag a
catch per worm (flung right, left, ahead-up); Pursuit each full-size worm biting its body three times (`MawMotion.chain`,
`BikakuMove.CHAIN_*`); Carrion Eruption once per worm under its own body. One Worm, Regeneration and the kakuja unchanged. Pure
`MawMotion.formMoves` (one worm: exactly the classic moves). Next: 3.6.2 The Walker.

**3.6.2 (The Walker, 750):** two walking tentacles out of the worms' root, one each side (`WalkerLegs`: each foot planted on the top of
a block out to its side and a little ahead, stepping one at a time when it falls behind, onto a wall's face when climbing, hanging when
nothing is in reach; its shape arching out over his hip and down, its mouth shut on the block); his legs hang still, lifted 3 model pixels
(`WalkerPose`); his client climbs any wall he walks into (`BikakuWalk`: a ladder's pace, sneak to hang, slide slowly); no fall damage while
it is out (server). Its moves: Three Worms'. Forms done; the kakuja unchanged.

**Before 3.4.9 (the pure worm, unchanged since except as above):**

- `control/bikaku/MawWorm.java`: the worm as a mesh (pure). A thick muscular tube along a spine resampled evenly by length
  (`spine`), with annular ridges every 0.16 blocks (`ridge`), folds and lumps, a breathing swell. Its radius by length (`radius`):
  0.1 out of the back, 0.245 body, 0.205 neck, 0.31 head, 0.27 lip. Thin glowing veins wander down it with an RC pulse (dimmer
  in the grooves), and grey bone spurs are raked back along its top. The mouth: a rolled lip round a hole (`holeRadius`: 20% of
  the lip shut, 84% wide; the head flares 42% wide, `lipRadius`). Shut, the lip puckers into creases. A crown of six great fangs
  (broad curved claws, `fang`) sits on the lip: shut, they fold forward over it like a clenched claw; open, they splay forward and
  out, hooked. Three rings of hooked teeth (14, 12, 9) go down a throat that goes dark.
  - Colours: built in the red palette and run through `KaguneTint.recolour(argb, tint)` for the flesh, lip and veins (the owner's
    kagune colour). The teeth, fangs, spurs, gums and throat keep their own colours, so the mouth reads on any colour.
  - LOD: `fine` (close up) about 11 rings a block, 16 sides; far, about 5 a block, 10 sides.
- `control/bikaku/MawMotion.java`: where the worm is, in the body's frame (blocks from the feet, x right, y up, z ahead).
  - At rest (`idle`): out of the lower back (`ROOT`), up behind the player in a zigzag of five coils (`ZIGZAG`), round over
    the right shoulder to the head (`HEAD`, about 3 blocks up). That's about **8 blocks long**. A wave runs up the coils (it
    slithers), the head sways, it scans in short snaps from spot to spot (`idleLook`), and every 7 to 10 seconds it gapes,
    showing its teeth, between little "tasting" opens (`idleMouth`).
  - A bite (`bite`): drawn back with the mouth opening, thrown out at the target gaping, snapped shut on it at the land tick,
    shaken, and back to rest. Thrown out, the coils pull most of the way straight toward a line over the right shoulder
    (`SHOULDER`), never through him.
- `tools/MawPreview.java`: off-game sheets on the player model (`look`, `mouth`, `bite`, `colours`). How to run it is in its
  header. Current sheets: `docs/handoff/bikaku-worm-{look,mouth,bite,colours}-wip.png`.
- `src/test/java/.../BikakuWormTests.java` (26 checks): long, rooted, zigzagging, finite quads, a mouth that grows open and
  still reads shut, recolouring, and a bite landing on its target shut and ending at rest.

Known to check: (fixed in 3.4.9: the coils no longer bunch up behind the head mid-lunge, and a bite no longer covers the first-person
view) in first person the head rests about 1.3 blocks above the eyes to the right, seen only looking up.

## The reference pictures (the owner's `Reference.zip`; not in the repo, ask them for it again)

1. A full-body figure in a long dark coat and a white mask with a grin of teeth. Two very thick worm tendrils come from behind and
   wrap around the body. Their flesh is purple-crimson with bright red veins and a dark red outline. Each ends in a "flower" of
   about six large curved grey petal-like fangs round a black hole.
2. One massive worm tendril arching from behind the figure out to the side and up. It is dark crimson-purple with bright red veins,
   grey bone spikes along its length, and a bulbous head crowned with a ring of big curved grey hooks, smaller teeth inside.
3. The kakuja: a chaotic mass of crimson-purple fleshy tentacles with glowing red outlines, covered in mouths: lipped mouths with
   rows of big flat human-like teeth (grins) and one huge maw with shark teeth and a dark throat.
4. Like 1: one tendril curling round the figure, its petal-fanged mouth open toward the viewer.

## The owner's spec (verbatim)

> We're adding an entirely new playable kagune to HexKagune, based on Noro from Tokyo Ghoul.
>
> This is a Bikaku kagune (new path along with rinkaku and ukaku) and must be as faithful as possible to his manga appearance,
> abilities, regeneration, and final Kakuja transformation.
> Do not replace or interfere with existing kagune systems. Integrate Noro into the existing progression, combat, animation,
> transformation, and ability systems.
>
> NORMAL KAGUNE APPEARANCE
> Noro's kagune is fundamentally different from traditional Bikaku tails. Instead of a smooth, pointed tail, it manifests as a massive,
> thick, fleshy, worm-like appendage emerging from the player's lower back.
> The kagune must have a heavy organic appearance, resembling an enormous predatory parasite or lamprey. Its body is muscular and
> flexible, with irregular fleshy ridges and subtle folds along its length.
> The most important feature is its enormous mouth. At the end of the appendage is a circular, expandable jaw lined with multiple rows
> of sharp, curved teeth. The mouth should physically open, close, stretch, and bite during attacks. It must look like an actual
> biological mouth, not a simple hole with teeth painted onto a texture.
> The kagune should move independently, constantly twisting, curling, and scanning its surroundings like a living creature. When idle,
> it slowly sways behind the player, occasionally opening its mouth and exposing its teeth.
> Noro is also capable of producing multiple kagune appendages. During certain abilities, the primary kagune can divide into several
> smaller but equally grotesque tentacles, each capable of forming its own functional mouth.
> The appendages must use smooth GeckoLib animations, properly segmented organic geometry, and natural bending rather than stiff
> cube-based movements.
> Preserve the player's Minecraft skin, body, head, and existing appearance. Only add the kagune geometry.
>
> IMPORTANT COLOR SYSTEM
> Noro's kagune must fully support our existing kagune color variation and customization systems.
> Do not hardcode one specific color onto the kagune.
> The main fleshy exterior must use the player's existing kagune color selection, with appropriate shading and darker organic details.
> Mouth interiors, teeth, and other biological structures should retain visually distinguishable colors.
> Every appendage, including those generated by abilities and the Kakuja transformation, must inherit the correct player's kagune colors.
>
> NORMAL KAGUNE ABILITIES
>
> 1. Devouring Maw — The massive kagune rapidly stretches forward toward the targeted enemy, opening its circular mouth extremely
> wide before snapping its jaws shut around the target. On a successful hit, the teeth clamp onto the enemy, dealing significant
> damage and briefly immobilizing them while the kagune violently shakes its head. The kagune then releases the enemy with a powerful
> sideways motion, throwing them away. (Bleed, brief stun, minor healing from successful bites.)
>
> 2. Ravenous Pursuit — The kagune splits into three separate mouth-bearing tentacles. Each tentacle independently targets a nearby
> enemy. The tentacles move in fast, snake-like trajectories, attacking from different angles rather than moving in straight lines.
> Against a single target, all three mouths attack consecutively, creating a brutal combination of bites and impacts. (Bleed,
> multiple hits.)
>
> 3. Predatory Drag — The kagune extends across the ground toward a target, with its mouth open and teeth exposed. Once it bites onto
> the enemy, it forcefully drags them directly toward the player, scraping them across the ground. When the victim reaches the
> player, the kagune lifts them off the ground and slams them down before releasing its grip. (Stun, bleed, grab and slam.)
>
> 4. Carrion Eruption — The player drives their kagune into the ground. It travels beneath the surface before violently emerging
> beneath the targeted enemy. The enormous mouth erupts upward with its jaws fully expanded, biting the target and launching them
> into the air. The kagune then retracts underground and returns to the player's back. This should have a distinctive underground
> movement animation, emerging flesh particles, ground debris, and a violent jaw-closing animation. (Knock-up, bleed.)
>
> 5. Monstrous Regeneration — The player temporarily activates Noro's extraordinary regenerative ability. During the ability, wounds
> are visibly repaired by rapidly growing strands of kagune tissue. The player receives strong regeneration for a limited duration.
> The visual effect should resemble organic tissue crawling over damaged body parts and knitting them back together. This ability
> should not make the player permanently invincible and must have an appropriate cooldown.
>
> NORMAL KAGUNE PASSIVES
> Abnormal Regeneration: Noro possesses one of the most extreme regenerative abilities in Tokyo Ghoul. Players using his kagune receive
> substantially improved natural health regeneration while the kagune is active.
> Pain Suppression: Noro barely reacts to injuries. The player receives increased resistance to knockback and reduced interruption
> from ordinary attacks.
> Predatory Feeding: Successful kagune bites restore a small amount of health. Killing enemies with the kagune provides additional
> recovery.
>
> FINAL TRANSFORMATION — NORO KAKUJA
> Noro's final transformation must be based directly on his monstrous Kakuja from Tokyo Ghoul, particularly his appearance during the
> Rose Extermination battle against the Quinx Squad.
> This transformation is NOT a conventional humanoid Kakuja and must NOT resemble Kaneki's armor, Eto's Owl, or a traditional monster
> with two arms and two legs.
> Noro's body becomes engulfed inside an enormous, grotesque, irregular mass of living kagune flesh.
> The transformation should resemble a gigantic, pulsating biological growth, covered in monstrous mouths and surrounded by enormous
> tentacles.
> The player is positioned within the central body of the creature, partially or completely enveloped by the expanding kagune.
> Several huge circular mouths form across the central mass. These mouths have thick fleshy lips, layered teeth, deep interiors, and
> independently animated jaws.
> Multiple gigantic tentacles grow from the main body in different directions. Each appendage has a heavy segmented structure, with
> some ending in enormous toothed mouths.
> The creature should be visibly asymmetrical, with irregular masses of flesh growing around the body. Avoid making it look like a
> perfectly symmetrical spider or octopus.
> Its entire surface should constantly move. Flesh expands and contracts, mouths open and close independently, and the tentacles curl
> around the central mass.
> The complete form should be significantly larger than the player, with its main body approximately 4–5 Minecraft blocks tall and its
> tentacles extending considerably farther.
> Its appearance must prioritize the disturbing organic horror of the manga rather than decorative armor, artificial spikes, or generic
> fantasy monster features.
>
> TRANSFORMATION ANIMATION
> Upon activation, the player's normal kagune begins violently expanding.
> Additional fleshy tendrils emerge from the back, wrapping around the player while the original kagune splits and multiplies.
> The appendages fuse into a gigantic pulsating mass, swallowing the player's silhouette as enormous mouths grow and expand across its
> surface.
> Finally, several massive mouth-bearing tentacles burst outward and spread around the creature.
> The transformation should feel aggressive, grotesque, and powerful.
>
> KAKUJA GAMEPLAY
> Noro's final form is largely stationary in the manga. Preserve this characteristic.
> The player becomes extremely slow or temporarily rooted while transformed, relying on the enormous reach of their appendages rather
> than conventional movement.
> In exchange, the transformation provides exceptional regeneration, enormous attack range, powerful area control, and greatly
> increased resistance to damage.
> The creature must feel like an almost unstoppable biological nightmare rather than a fast-moving fighter.
>
> KAKUJA ABILITIES
>
> 1. Thousand Jaws — Every available mouth-bearing tentacle extends outward and attacks nearby targets independently. The appendages
> repeatedly strike and bite enemies from multiple directions, creating a chaotic area of moving jaws. Enemies close to the central
> mass may be grabbed by the larger mouths, while distant enemies are attacked by the extended tentacles. (Multiple hits, bleed, brief
> stun from large bites.)
>
> 2. Flesh Cataclysm — The entire Kakuja contracts toward its center before explosively extending its enormous appendages outward. The
> tentacles sweep across the surrounding area, striking enemies with tremendous force. The central mouths simultaneously open and
> release a terrifying roar. Enemies caught close to the transformation are thrown outward, while enemies struck by the extending
> tentacles receive heavy damage. (Area damage, knockback, stun.)
>
> 3. Endless Hunger — Several enormous mouths simultaneously target one enemy. The appendages wrap around the target, restricting
> movement before repeatedly biting from different angles. The largest mouth then closes around the restrained target, dealing a
> final crushing bite before releasing them. Each successful bite restores a portion of the player's health. (Grab, bleed, sustained
> damage, healing.)
>
> 4. Undying Abomination — The Kakuja begins regenerating at an extraordinary rate, with fleshy strands visibly repairing the central
> body and damaged appendages. During this period, the creature receives substantially increased regeneration and damage resistance.
> If interrupted by heavy damage, the regeneration slows temporarily but does not immediately disappear.
>
> KAKUJA PASSIVES
> Monstrous Regeneration: Extremely powerful continuous regeneration, considerably stronger than normal kagune regeneration.
> Abnormal Flesh: Significant damage resistance and greatly reduced knockback.
> Autonomous Predation: Large mouth-bearing tentacles automatically react to nearby hostile enemies with occasional bite attacks.
> These attacks must have independent animations and sensible attack intervals.
> Immovable Horror: The transformation sacrifices normal mobility in exchange for increased durability and attack reach.
>
> IMPORTANT IMPLEMENTATION REQUIREMENTS
> All normal kagune and Kakuja abilities must have proper third-person and first-person rendering, smooth attack animations,
> functional hit detection, realistic appendage movement, and synchronized multiplayer behavior.
> Kagune mouths must actually animate during bites. Tentacles must physically extend, curve, retract, and track their targets.
> Keep the existing blood effects, combat sounds, damage systems, status effects, and established project conventions.
> All generated kagune parts must respect the player's existing kagune color configuration. This applies to the normal form, Kakuja
> form, transformations, detached visual effects, and every ability.
> Make the model detailed and organic without creating excessive geometry or causing significant server performance issues.
> Noro's canonical regenerative capabilities and unique mouth-bearing kagune are the foundation of this addition. The proposed named
> attacks are gameplay adaptations of those characteristics rather than claims that Noro performed every individual move in the manga.
> The final result should make Noro's kagune feel genuinely unique: a living, predatory organism with enormous jaws, extraordinary
> regeneration, and an enormous, grotesque final transformation.

Then, mid-build (binding):

- **"this [kagune] should be longer than all the others. It should like have zigzags and all. It shouldn't just be, you know, small.
  So I like the design you're making, but just make sure it's long."** (Done: about 8 blocks of zigzag coils at rest.)
- **"Also, something about the tip of the mouth when it's closed doesn't look right. It just look at it. It doesn't looks like
  there's"** (cut off). Read as "doesn't look like there's a mouth": the shut fangs had met in a spiky point. Reworked: the lip
  puckers into creases round a small dark hole and the fangs fold over it like a clenched claw. **Ask them if that's what they meant.**
- With a picture of the mouth's side view (a gap between the body and the lip showing the throat's teeth, and two dark hexagons):
  **"Does that look right to you? And that's clearly broken there on the side."** Fixed: the body tube now runs right up to the lip
  (it had ended 1.5% short, which on an 8-block worm left a 0.12-block gap), and the ring of "crown" spurs whose ends showed as
  hexagons is gone.

After 3.5.3, with the kakuja still unreleased (binding; to be done after the kakuja, "once you're done with the final form"):

- **"Gotta say, before you continue, all the attacks that we currently have, they need way more range. This is meant to be basically
  more range than the Rinkaku, and yeah, I like the look of the mod, like, so far I like this new design. I actually quite love it, it's
  just it needs a little a few touches, it needs to be more flexible. The thing must feel a little more alive, if you get what I mean.
  I'm struggling to describe what I want exactly, but as well as we need more forms. Currently there's only one one of these things, but
  another form should have like two worms. The third should have three, and honestly this is where it gets hard, but I think that the
  fourth before the Kakuja should be like the player no longer even like walks with their own legs, like they get like two separate
  appendages on either, like both sides emerging from the same spot as the worms, that when the player walks his body doesn't move, but
  like he controls these tentacles that walk around adapt to terrain. Being able to climb anything, like Dr. Octopus pretty much. If you
  look up Dr. Octopus from Spider-Man, you can look at how he walks. That's kind of what I'm trying to describe. The moves, of course,
  changing to something else whenever you have like depending on the form. That's what I'm trying to go for here. Try to adapt these
  changes once you're done with the final form that you currently making. And then we can continue."** Not started. (Keep the film
  character's name out of anything visible in game.)
- On the kakuja previews (it shipped as it was, 3.5.4): **"I quite like the kakuji that you made, if I'm being honest. When I say more
  flexible, I mean, like, make sure that the tentacles don't go through a player's body. I noticed that a few are doing that. Just, like,
  a few moves. Like the predatory drag looks a little strange when you send it and it goes through the player's body instead of going
  around it. And again, the range is really the problem because it shouldn't go far. Like, it should be a held move that the longer you
  hold it for, the longer it goes for. And I, if you ask me, this specific kagune should have a lot of range, like covering up to like 30
  blocks. Of course, attacks are not able to be performed until the kagune is pulled fully back. So that's the only disadvantage that
  this kagune has. It should adapt to blocks, and that's where I say flexibility, like go around blocks like a worm, sort of, and
  actually go around them. It's being able to adapt to the terrain itself and pulling people back and shit, you know what I mean? Just
  carry on, do what you're doing, and then cover everything that I said. But so far, so good, though. I'm, like, happy with the results.
  Just cover all this that I told you and adapt the changes in the new forms, and then we'll see."**

## The forms: the plan put to the owner after 3.5.9 (not started; their answers decide it)

The owner's words (above): two worms, three worms, then a fourth before the kakuja that walks on two tentacles from the worms' root,
climbs anything, "The moves, of course, changing to something else ... depending on the form". Proposed (like the Rinkaku's forms: each
opens with mastery, picked in the menu, K for the kakuja as now):

1. One Worm (0): as it is (M1 bites, Maw G, Drag J, Pursuit Z, Eruption B, Regeneration C).
2. Twin Worms (250): two worms out of the same root, one over each shoulder; M1 alternates them; its own moves.
3. Three Worms (500): three; its own moves.
4. The Walker (750): two walking tentacles out of the same root carry him (his legs off the ground, his body steady as he walks), feet
   finding the ground and walls round him, climbing anything; its own moves.

Asked the owner: whether the existing moves stay with the one worm (each form then has a set of its own) or each form keeps some; and
whether they have moves in mind for forms 2 to 4 or want them designed and previewed first. Their answer (binding): **"to basically
become overpowered, like, to do enhanced versions of themselves whenever they're, like, in different transformations, such as, like, the
move that, like, drag pursuit, you can send all three of them, all three of the worm-like things, and the thing that sends the worm
underground and then up can be like more than one, and whatsoever."**

## Decisions made (tell the owner; change if they say)

- **GeckoLib:** the mod does not ship the GeckoLib library (the Dragon uses the in-house GeckoLib-format reader,
  `awakening/.../geo`), and a GeckoLib model is built of cubes, which the spec rules out ("natural bending rather than stiff
  cube-based movements"). The worm is a procedural mesh that bends smoothly along its whole length, as the other kagune here are.
- The name on screen: "Bikaku".
- The look: 6 great fangs plus 3 rings of teeth; grey bone; red palette recoloured; about 8 blocks long at rest (owner's ask).

## Plan (each its own version, pushed as built; after 3.4.9 comes 3.5.0)

1. **3.4.9: the path and the worm in game.** (Done.) `KagunePath.BIKAKU` (choosable), its starting-screen card and silhouette, its menu
   (stages and mastery), its move guide. `BikakuServer` (state, M1 bites using `MawMotion.bite`'s timing, Predatory Feeding:
   heal on a bite, more on a kill, gates). `S2CBikakuState` sync, `BikakuClient`, and `BikakuRenderer`: the worm out of the lower
   back riding the body's pose, third and first person, `KaguneTints.of(player)`, growing out as the kagune comes out. The
   original Rinkaku tendrils hidden for the path. Protocol 94.
2. **3.5.0:** (Done: G and J, `BikakuMove`, `BikakuMoves`, `BikakuInput`.) Devouring Maw and Predatory Drag (grabs: hold the target at the mouth, shake, throw sideways; drag along the ground, lift,
   slam).
3. **3.5.1:** (Done: Z and B; `MawMotion.pursuit/eruption`, `client/fx/BikakuFx`.) Ravenous Pursuit (the worm splits into three thinner worms from the same root, `MawWorm.Pose.girth` about 0.6, snaking paths)
   and Carrion Eruption (into the ground at the feet, a travelling bulge of debris, up under the target; a path through a ground
   point).
4. **3.5.3** (Done; 3.5.2 was a fix: the small heals): Monstrous Regeneration (the active, with tissue-strand visuals over the body) and the three passives (Abnormal Regeneration
   through `GhoulRegen`, Pain Suppression: knockback resistance and less stagger, Predatory Feeding if not done in 1).
5. **3.5.4** (Done; the owner: "I quite like the kakuji that you made": `MawKakuja`, the kakuja in `BikakuServer`/`BikakuRenderer`; see `docs/handoff/next-ai-3.5.4.md`): The kakuja: the central mass (4 to 5 blocks, irregular, asymmetric, pulsating, covered in lipped mouths with flat teeth, ref 3)
   with its tentacles, the transformation (the worm swells, more tendrils wrap him, a fused mass swallows him, mouths grow, the
   tentacles burst out), rooted or very slow, and its resistances.
6. **3.5.7** (Done; after the owner's reports, 3.5.5 and 3.5.6: `KakujaActs`, `MawKakuja.Act`, `BikakuMoves` startKakuja/jawsTick/hungerTick/cataclysmTick/autoTick): The kakuja's four abilities and its passives (Autonomous Predation bites on their own, at sensible intervals).

Proposed mastery gates (a judgment call; ask or tell): 0 the worm, M1 and Devouring Maw; 150 Predatory Drag; 300 Ravenous Pursuit;
450 Carrion Eruption; 600 Monstrous Regeneration; 1000 the kakuja.

## Every place a new path hooks in (found starting this; follow Graft DNA's code, a complete dormant path)

- `KagunePath`: a constant (its id is saved). `choosable()` picks it up.
- Starting screen: `client/PathCards` (a `Card`, `of(path)`), `client/PathSilhouette` (its animated silhouette).
- Menu: `client/PathMenus.menuFor` (a switch: compile error until it has a case).
- Move guide: `AbilityGuide` (the title switch near line 136, the panel switch near 144, and the checks near 238 to 244).
- Quests: `server/QuestServer` (switches near 246 and 252: stage id, kakuja), `quest/Gate` (path gates by mastery), `QuestBook`.
- Leaving or joining a path: `server/UkakuServer.onPathChanged` (add it to the paths that `RinkakuStash.stash` the Rinkaku progression;
  call its own `onPathChanged`, as Pale's and Graft's are).
- Rinkaku-only mechanics to refuse: `server/UkakuGates` (J slingshot, G grab, N throw, H crush, V lift, B slam, C guard, threat auto,
  fall arrest), `server/KaguneCombat` (M1 `strike` routing near 117, `startInvoluntary` near 145), `ImpaleServer` (111),
  `ThyDeathServer` (71), `UkakuVortex` (86), `RinkakuAutonomy` (37), `TendrilShieldServer` (99), `RinkakuWounds` (37),
  `RinkakuFormBuffs` (29), `KakujaPassive` (86 to 89), `GhoulServer` (123), `ControlServer` (110), `EtoRules` (387), the kakugan
  (`KakuganServer` 53 to 56, `KakuganClient` 94), `CentipedeRenderer` (106), `client/fx/KakujaFx` (311, 641). Each has
  `UkakuServer.isUkaku` / `PaleServer.isPale` / `GraftServer.isGraft`: add the new path's check beside them.
- Client: `UkakuClient.tendrilView` (line 134: the original's tendrils and writhing are not drawn for these paths),
  `ManifestSounds.limb` (58).
- Network: `ControlNetwork` (3.4.9 took 54 for S2CBikakuState and protocol "94"; the next free id is 55, for a `C2SBikakuAction`).
- Server template: `GraftServer` (state map, `broadcast`, `startTracking`, `loggedIn`, `respawned`, `changedDimension`, `loggedOut`),
  `GraftCombat.strikeBody` (damage by mastery with a PvP share, `WoundHits.expect`, `UkakuCrystalCombat.graftBleed`, `PaleStun.stun`,
  `KaguneClash`, `KaguneProfile.addMastery`, `ControlServer.onKaguneHit`), `GraftCombat.aim` (target choice), the bite's sounds
  (`HexKaguneSounds.BONE_CRUSH`, `KAGUNE_WHIP`, `FLESH_IMPACT`).
- Client template: `GraftClient` (views timed on the client's own clock, `PaleClient.clock`), `GraftRenderer` (a
  `RenderLevelStageEvent` AFTER_ENTITIES pass, the `ukaku_white` translucent type, glowing quads full bright, nothing within 1.15
  blocks of the eyes in first person, the root riding `KaguneBodyAnimation.body`), `GraftInput` + `C2SGraftAction` (how a path takes
  the keys), `KaguneTints.of(player)` (the colour), `UkakuClient.deploy` (how far the kagune is out).
