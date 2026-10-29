# The user's requests for 7.0.0, Furuta DNA (verbatim, binding)

Recorded 2026-10-24. The user's own words, unedited. The first message came with five reference images (Furuta's centipede kagune,
the many-eyed kagune from the manga, the kakuja); the later ones were said while the work was under way.


## The Furuta ability rework (session 3, 2026-10-24; verbatim, binding)

Sent while phase 5 of the progression overhaul was being pushed. The user's title was "ability rework".

> Normal Kagune — Two Tails
>
> 1. Mockingbird (Stun): The player deliberately leaves themselves exposed. When attacked, they perform an extremely fast sideways dodge while one tail hooks around the attacker and the other delivers a devastating counterstrike from behind. A successful counter briefly stuns the enemy and flows directly into another attack.
> 2. Eto's Execution (Bleed): The player rushes toward an opponent and unleashes a blindingly fast sequence of precise kagune cuts. The tails continuously switch angles while the player circles the enemy, finishing with a simultaneous strike intended to break through defenses. Successful cutting attacks apply bleeding.
> 3. Scorpion Vault (Stun on Landing): Both tails stab into the ground behind the player and launch them into the air. While airborne, the player can aim a powerful downward piercing attack or transition into another ability. A successful downward impact briefly stuns the target.
> 4. Human Puppet (Stun on Grab): One tail catches an enemy and drags them toward the player, briefly stunning them. The player can then choose to throw them, strike them with the second tail, or pull themselves toward the restrained enemy for a melee combination.
>
> Centipede Kagune — Two Centipedes and One Mouth
>
> 1. Hungry Maw (Bleed): The mouth-bearing appendage begins chasing a selected enemy, bending around obstacles and snapping repeatedly. When it catches them, its jaws clamp shut, applying bleeding. The player can drag the victim while continuing to move and attack with the other appendages.
> 2. Centipede Locomotion (Stun on Bite): Both centipedes plant their tips against the terrain and violently propel the player forward in a rapid, unnatural crawling movement. The player can change direction and perform attacks during the movement, finishing with a lunging bite from the third appendage that briefly stuns on impact.
> 3. The Ambush (Stun): Both centipedes launch an obvious frontal attack, drawing the enemy's attention. Meanwhile, the mouth-bearing appendage curves underneath or around the target before unexpectedly snapping upward. This is a feint-based guard-breaking attack that briefly stuns the victim when successful.
> 4. Living Carapace (Stun on Counter): The centipedes rapidly coil around the player, creating a temporary physical defense. If an enemy strikes the guarded area, the appendages spring outward, briefly stunning the attacker and immediately retaliating. The mouth-bearing appendage remains available for a follow-up bite.
>
> Four Thorned Tentacles
>
> 1. Crucifixion (Bleed): All four appendages attack a single target from different angles. Two catch and suspend the enemy while the other two deliver a rapid sequence of impaling strikes, applying bleeding. The target is then violently thrown away.
> 2. Unstoppable Breach (Stun + Bleed): The player launches forward with all four appendages spread ahead of them, tearing through enemies and temporarily breaking terrain in their path. The appendages continuously alternate between smashing obstacles and striking enemies while the player charges forward. Direct impacts briefly stun enemies, while the thorned strikes apply bleeding.
> 3. Dragon's Dragnet (Stun): Two appendages extend far to either side and sweep inward, gathering multiple enemies toward the center. The remaining two appendages then launch a devastating synchronized attack against the grouped targets, briefly stunning them on impact.
> 4. Living Catapult (Stun on Landing): The four appendages plant themselves into the ground and launch the player an enormous distance into the air. The player can steer during flight and select where to land. Upon impact, the appendages strike the surrounding ground in succession, briefly stunning nearby enemies and allowing an immediate transition into normal combat.
>
> Mass of Eyes
>
> 1. Eye of the Storm (Bleed): The gigantic kagune begins expanding and rotating around the player. Its enormous extensions travel in different directions, continuously changing their trajectories and striking enemies within the area. The embedded eyes independently move and blink throughout the animation. Direct cutting hits apply bleeding.
> 2. Flesh Bloom (Bleed): The entire organic mass contracts before expanding outward. Huge spikes rapidly grow along its extended surfaces in several consecutive waves, applying bleeding to enemies struck. The spikes retract as the kagune contracts again.
> 3. Black Curtain (Stun): The kagune rises and spreads into an enormous wall of living flesh in front of the player. The wall then folds forward, overwhelming enemies beneath its weight and briefly stunning those directly struck. Its extremities continue curling around the sides, preventing opponents from easily escaping the attack.
> 4. The King's Game (Bleed on Sword Strike): The kagune extends toward an enemy and begins attacking from different directions while the player remains free to move independently. If the player is holding a sword, they can rush toward the distracted opponent and deliver a coordinated sword attack that applies bleeding. The kagune must remain physically connected to the player at all times.
>
> Final Kakuja
>
> 1. King's Checkmate (Stun): One centipede launches a frontal attack to force an enemy into defending. As they react, the second centipede curves around their position and catches them from behind, briefly stunning them. The player then charges forward and delivers a powerful close-range combination against the restrained enemy.
> 2. Centipede Hunt (Bleed): Both gigantic appendages propel the player forward in a terrifying high-speed pursuit. One appendage continuously strikes at the target while the other anchors itself against the terrain to change direction. On catching the target, the attacking appendage grabs and violently drags them across the ground, applying bleeding before throwing them.
> 3. Monstrous Recovery (No Stun or Bleed): The player enters a temporary regenerative state, rapidly recovering health while their Kakuja's appendages curl protectively around their body. Regeneration consumes substantial RC energy. The player can still move, but their offensive abilities are temporarily restricted while concentrating on recovery.
> 4. The Final Performance (Stun + Bleed): The player unleashes a sustained, controllable sequence of aggressive attacks. The two centipedes independently attack from different directions while the player closes in with extremely fast melee strikes. The player can redirect the appendages during the assault. Repeated cutting strikes apply bleeding. The finisher catches the primary target with one centipede, briefly stuns them, pulls them directly toward the player, and delivers a devastating combined attack with the other appendage and a final physical strike.
>
> All abilities must have fluid, continuous animations with proper physical contact, independently animated kagune structures, and responsive transitions. Their actual mechanics must be implemented rather than represented by particles or cosmetic animations.
> Stun should briefly prevent enemy movement and attacking, while Bleed should inflict damage over time. Repeated hits should refresh the relevant effect rather than endlessly stacking its duration or strength. Both effects must be balanced according to the strength and cooldown of each ability.
> These attacks are gameplay adaptations based on Furuta's demonstrated speed, swordsmanship, physical strength, regeneration, and rinkaku manipulation. Do not introduce magical or supernatural powers unrelated to those capabilities.

Note (session 3): "Eto's Execution" collides with the user's own 6.2.0 rule (no player-visible "Eto", enforced by
`EtoTests.owlDna`). It is shown as **"Owl's Execution"** until the user says otherwise; told them so.

## The progression overhaul (session 2, 2026-10-24; verbatim, binding)

> From now on, every ghoul, regardless of their DNA or Kagune path, will have access to the combat and movement system originally developed for the Owl DNA. However, this system must only activate when the player has their ghoul eye mode enabled without activating their Kagune.
>
> This includes the existing combat animations, attacks, backflips, front flips, evasive movements, and all other mechanics already implemented in the Owl DNA combat system. Reuse the existing code rather than creating an entirely new system. The effectiveness of this combat system, including damage and overall combat performance, should scale with the player's mastery.
>
> Once the player activates their full Kagune mode, the shared eye-mode combat and movement system must be disabled and overridden by the combat and movement mechanics specifically designed for their equipped Kagune. Do not remove or replace any existing Kagune-specific functionality. The two systems must transition properly without interfering with each other. The shared Owl combat system should only be available while using eye mode without the Kagune.
>
> Next, completely rework the progression system. The current progression is too linear and becomes repetitive, so I want a more interactive, rewarding, and unpredictable experience. Leveling and mastery can remain relevant, but unlocking abilities, mutations, and transformations should also require completing unique objectives, quests, and challenges.
>
> Introduce ghoul NPCs that naturally spawn throughout the Minecraft world. They should be able to appear in any biome, but exclusively at night. Their spawn rate should be relatively low without making them excessively difficult to encounter. Finding one should feel interesting rather than ordinary. Avoid situations where multiple ghoul NPCs frequently spawn close together.
>
> These NPCs should have different behaviors and personalities. Some should be hostile and attack the player immediately, while others may be neutral or friendly. Players should be able to right-click non-hostile NPCs to open an interactive dialogue menu, communicate with them, accept quests, make choices, and potentially develop friendships. Their behavior and available interactions should vary instead of every NPC functioning identically.
>
> For combat, ghoul NPCs should only possess basic Kagune forms. They should be capable of using the existing basic M1 combat attacks from Kagune paths such as Ayato and Kaneki. Reuse the already implemented mechanics and animations wherever possible. They do not need access to advanced mutations or Kakuja transformations.
>
> When a ghoul NPC dies, it should drop a new custom item called Ghoul Meat. Create an appropriate 2D item texture that visually represents ghoul flesh and matches the existing mod's art style.
>
> Ghoul Meat must be consumable and provide permanent, cumulative improvements to the player's power. Every piece consumed should contribute additional strength and increase the player's maximum energy capacity. These bonuses must stack permanently rather than functioning as temporary potion effects. A larger maximum energy capacity means the player can maintain their abilities and transformations for longer before exhausting their energy. The more ghouls the player defeats and consumes, the stronger they can eventually become.
>
> This system should make hunting other ghouls a meaningful alternative way of becoming stronger, alongside mastery progression, quests, and mutations.
>
> The quest system requires particular attention. Tevold, I want you to design a large variety of appropriate quests and challenges for the different Kagune paths. Do not restrict progression to simply gaining levels and automatically unlocking the next ability. Certain abilities, mutations, and transformations should require specific achievements or completed objectives.
>
> Quest objectives can include defeating particular enemies, consuming certain items, surviving challenging encounters, dying a certain number of times, completing combat challenges, interacting with NPCs, or fulfilling other conditions appropriate to the ghoul's progression. These are examples, not an exhaustive list.
>
> Different Kagune paths should have their own appropriate progression objectives. The quests should reflect the nature of the abilities and mutations being unlocked, rather than using exactly the same repetitive requirements across every path.
>
> Make progression less predictable and more interactive. Introduce meaningful choices through NPC interactions, different quest opportunities, and alternative ways to make progress where appropriate. Some NPCs might offer quests, others might become allies through dialogue, and others might become enemies depending on the player's interactions.
>
> You have creative freedom to design the individual quests, NPC personalities, dialogue options, progression objectives, and interactions. I am providing the overall direction, not an exhaustive specification of every quest or mechanic. Use your judgment to make these systems engaging, varied, and appropriate to Tokyo Ghoul and the existing mod.
>
> The priority is to eliminate the repetitive feeling of the current progression. Players should have reasons to explore the world, encounter other ghouls, engage in combat, interact with NPCs, and pursue objectives beyond simply grinding mastery levels.
>
> Preserve and reuse the existing functional systems wherever possible. Ensure the shared eye-mode combat, Kagune-specific combat, NPC behaviors, permanent consumption bonuses, quest progression, mastery, and ability unlocks all work together correctly without breaking the existing mod.
>
> The result should feel like a substantial gameplay and progression overhaul rather than simply adding a few NPCs and additional quests.

## Request 1: replace Owl DNA with Furuta DNA

Replace the existing Owl DNA progression with a completely new Furuta Nimura DNA path based on Kichimura Washuu from Tokyo Ghoul:re. This is a full replacement of the currently active DNA path, not simply a cosmetic change. Preserve all existing Owl DNA source code, models, animations, abilities, mutations, and progression concepts in a separate dormant implementation so they can be restored in a future update. Remove every Owl-related option from active gameplay, selection menus, unlock trees, spawning systems, and progression, without deleting the original work or destabilizing the remaining paths. The new progression must integrate into the existing mod architecture, reuse functional combat mechanics where appropriate, and introduce Furuta's unique kagune, biological traits, transformation stages, animations, and combat abilities.

Furuta is an artificial one-eyed ghoul created through the transplantation of Rize Kamishiro's kakuhou. His original half-human physiology already gives him exceptional physical abilities, while the implanted kakuhou grants him a rinkaku kagune, extraordinary regeneration, and enhanced physical strength. Implement the transplanted kakuhou as the foundation of the DNA path and the biological source of his abilities. His kakugan appears in the right eye, turning the sclera black and the iris crimson red, with visible red veins around the eye. The kakugan should activate during kagune release, high-intensity combat, and kakuja transformation. Allow independent activation of the ghoul eye without requiring the kagune to remain extended.

Furuta's initial kagune consists of one or two muscular rinkaku appendages emerging directly from the lower back. These are not rigid blades or ordinary tentacles. They are living biological extensions, dark blood-red in coloration, with thick muscular roots, irregular tapered surfaces, and sharpened tips. Each appendage must possess convincing depth and organic curvature, with a smooth progression from the wide base to its narrower, deadly end. Their movement should resemble controlled predatory limbs, bending naturally through multiple joints, twisting, coiling, retracting, and violently extending without looking like disconnected Minecraft blocks. Their attacks emphasize speed and precision rather than sheer size. This early form must reproduce the essence of Furuta's confrontation with Eto, where his newly revealed kagune is already powerful enough to tear through her weakened kakuja.

The next stage evolves the kagune into three considerably more aggressive, centipede-like appendages. Their surfaces become uneven and armored, developing hardened organic segments, overlapping ridges, hooked protrusions, and numerous sharp spikes along their sides. Some segments should look like dense muscle covered by an irregular outer carapace. The limbs become visibly thicker, heavier, and considerably more threatening, while retaining exceptional flexibility. At least one appendage must be capable of developing a grotesque organic mouth filled with sharp teeth, allowing the kagune to bite, seize, and crush targets. This mouth must be a real modeled and animated feature, capable of opening, closing, and moving with the appendage, rather than a painted texture or stationary ornament. The other appendages should specialize in stabbing, sweeping, constriction, and defensive interception.

As the kagune progresses, Furuta develops four enormous thorn-covered rinkaku appendages. These should be significantly larger than the player character, with thick muscular foundations, pronounced sectional ridges, serrated edges, and massive pointed extensions. Their visual appearance should evoke gigantic predatory centipedes mixed with living armored flesh, rather than ordinary tails. They must move as enormous but agile limbs, capable of rapidly changing direction, extending far beyond normal melee range, lifting enemies, tearing through terrain, and attacking several targets simultaneously. Their size and shape should communicate overwhelming biological strength without making the character's actual humanoid body unnecessarily bulky.

Furuta's ultimate non-kakuja kagune progression resembles a grotesque, Dragon-inspired biological formation. This is not Kaneki's complete Dragon transformation and must never be modeled as the enormous Dragon creature surrounding Tokyo. Instead, a substantial black and crimson organic mass expands behind Furuta, branching into gigantic curved, tentacle-like structures. Large unnatural eyes emerge directly from these appendages, surrounded by thick rings of muscular tissue. Each eye should possess visible depth, a distinct iris or pupil, and independent movement or blinking where practical. These eyes must look embedded within living flesh, not pasted onto the surface. The appendages become immensely thick and irregular, with sweeping crescent-like curves, sharpened extremities, and the ability to generate additional spikes. Their motion should convey the sense of an intelligent, constantly changing organism. Portions of the mass can contract, swell, divide into attacking extensions, or extend outward before violently retracting. This stage should have an intimidating silhouette dominated by the enormous multi-eyed kagune behind the otherwise recognizable humanoid user.

The final Kakuja is a distinct transformation rather than merely enlarging the preceding kagune. Furuta's own body becomes partially encased in a dense layer of living rinkaku armor. The armor should closely follow the anatomical structure of the torso, shoulders, neck, arms, and legs, using tightly packed organic layers and intricate muscle-like patterns. It must look grown from the character's own flesh, with visible ridges, overlapping muscular structures, and hardened biological surfaces. It is not metal armor and should not resemble the Owl's feathered mantle, Kaneki's armor, or an external costume. Furuta's head is transformed into a disturbing, flesh-covered monstrous appearance. One large, conspicuous, glowing kakugan remains visible, while the lower face develops an enormous predatory mouth containing large, jagged teeth. Its jaw should open unnaturally wide during attacks, with visible internal depth, layered teeth, and moving flesh around the cheeks and mouth. Preserve the anatomical continuity of the original head beneath the transformation. It must never look like a separate floating mask or helmet placed over a normal head.

The Kakuja's defining rear appendages are two colossal centipede-like rinkaku structures, considerably larger and more heavily armored than those of the earlier stages. Their bodies are thick, elongated, segmented, and covered in rows of dangerous lateral spikes. Multiple oversized eyes are embedded along their lengths, surrounded by folds of dark organic tissue. These appendages should curl above, beside, and behind the player, creating the silhouette of a humanoid predator controlling two enormous living monsters from its back. Both must move independently, with broad sweeping movement, coiling animations, sudden predatory thrusts, and aggressive repositioning during melee combinations. The appendages are not wings, feathers, mechanical arms, or detached creatures. They are continuous biological extensions connected to Furuta's body. The entire Kakuja must remain fundamentally humanoid, with the rear appendages creating most of its enormous visual presence.

Implement combat abilities that express the progression of this biology. In the initial kagune stage, allow rapid piercing thrusts, controlled horizontal slashes, simultaneous attacks from multiple appendages, short-range grappling, defensive interception, and sudden high-speed movement. Add a precise attack that fires a single tentacle forward like a spear before retracting it, and a wider attack that sweeps an appendage through enemies in a curved arc. The kagune should react continuously to player movement and attack direction, rather than disappearing and reappearing between abilities.

The centipede stage should unlock considerably more brutal abilities, including high-speed chained thrusts, spinning attacks with several appendages, constriction grabs, violent directional slams, and a snapping bite from the toothed kagune. The spikes along the appendages should be functional damage surfaces rather than purely decorative geometry. Allow the user to impale a target, lift it using the kagune, reposition it, and throw it in the direction of the attack. Include a short-range defensive spin where the appendages curl around the user before violently expanding outward.

The four-appendage stage emphasizes overwhelming power and battlefield manipulation. Add synchronized strikes capable of attacking different enemies, enormously powerful frontal sweeps, extended piercing attacks, and a ground impact where the appendages strike the terrain with enough force to generate a shockwave. Introduce attacks that launch the player forward using the kagune as propulsion, allowing high-speed closing attacks followed by an immediate combination. Allow extended appendages to grip surfaces and help propel the user through the environment. Where attacks destroy terrain, reuse the existing temporary block-destruction and restoration systems already established within the mod.

The giant multi-eyed kagune should unlock large-scale biological manipulation. Allow the appendages to expand their effective reach, generate clusters of protruding spikes, change curvature during attacks, and perform simultaneous attacks from several directions. Include a major ability where the black organic mass surges forward, splitting into multiple attacking limbs before collapsing back toward the player. Another ability should curl the massive appendages defensively around the user, temporarily intercepting incoming attacks. The numerous embedded eyes should blink, rotate, and react during attack charging or targeting, without automatically turning them into supernatural laser weapons, since Furuta has not demonstrated laser-based kagune attacks in the source material.

The Kakuja unlocks the most violent and physically dominant combat style. Replace ordinary light attacks with rapid clawing movements, enormous centipede strikes, and combined humanoid-and-kagune attack sequences. Introduce a crushing attack in which one appendage seizes a target while the second strikes it, a long-reaching multi-hit combination using both centipedes, and a massive overhead strike that drives an appendage into the ground. Add a high-speed predatory rush in which the transformed character lunges forward while both rear appendages attack alongside the body. Include a wide-area attack in which the appendages rise above the player, curl outward, and slam down on opposite sides. The Kakuja should also gain substantially increased regeneration, resistance to physical damage, and rapid recovery after heavy impacts.

Regeneration must be a central passive ability of the Furuta DNA. As shown throughout the series, Furuta can survive catastrophic injuries, recover from impalement, repair damaged tissue, and even restore severed limbs. Implement a powerful passive regeneration system that scales with DNA progression and becomes significantly stronger during Kakuja transformation. Regeneration should use the existing mod's biological resource or RC energy mechanics and must not grant unlimited invulnerability. If the mod already supports severe injury, limb reattachment, or animated body reconstruction, integrate Furuta's regeneration with those systems instead of creating disconnected duplicate mechanics.

Furuta is also an extremely skilled close-combat fighter and swordsman. Preserve the ability to use normal Minecraft weapons while the kagune is active. Optionally integrate a katana-based fighting style that combines fast sword slashes, precision dodges, feints, parries, and synchronized kagune counterattacks. His kagune should complement close-range weapon combat without forcing the player into a separate weapon system. His movements should communicate confidence, composure, and exceptional combat intelligence rather than a permanently uncontrolled berserker state.

Every transformation requires a unique, fully animated deployment sequence. The kagune should grow from the lower back progressively, beginning with muscular movement beneath the body before extending outward. New appendages must grow and unfold rather than instantly appearing. Higher evolutions should add expanding biological tissue, forming spikes, opening eyes, and visibly increasing appendage mass. The Kakuja transformation should begin with the kagune contracting toward the player's back as organic armor rapidly spreads across the body, climbs the neck, and reshapes the face. The two colossal centipedes then emerge and unfold into their final positions. Reverting must animate the transformation in reverse instead of instantly removing the entire model.

All models must use the existing Blockbench and GeckoLib architecture for Forge 1.20.1. Prioritize accurate anatomy, organic silhouettes, seamless geometry, smooth motion, and efficient rendering. Avoid excessive disconnected cubes, huge overlapping geometric structures, crude flat appendages, and needlessly large model files. Use optimized model groups, appropriate texture atlases, dedicated animation bones, and reusable segment structures. Each large appendage must have enough independent articulation to produce smooth natural curvature. Embedded eyes, moving mouths, snapping jaws, spikes, armored sections, and transformation components should be separately controllable where animation requires it. Keep textures detailed enough to reproduce the organic flesh, dark crimson muscle, black carapace, and eye patterns without relying on enormous texture resolutions.

Reuse the functional GeckoLib animations, hit detection, targeting logic, physics, particles, sound systems, ability interface, and transformation infrastructure from the existing mod wherever possible. Maintain animation cancellation and responsive transitions so abilities can chain naturally in combat. Ensure that appendages continue reacting to player movement while idle, walking, sprinting, jumping, attacking, and transforming. The player's arms, legs, head, and body must never become permanently locked by kagune animations. Support third-person visual accuracy and avoid intrusive geometry obstructing the first-person camera.

The DNA progression should unlock biological developments gradually rather than granting every ability immediately. Stronger kagune versions replace or enhance earlier abilities while retaining useful movement and basic combat functionality. Kakuja must require substantial progression, use the established transformation resource system, and possess appropriate limitations and cooldowns. Ensure that no previous Owl DNA progression can accidentally become available through old player data, commands, menu references, or cached selection states. Preserve all Owl-related content safely for a future update, but make Furuta's DNA the only active replacement for that particular path.

Use the original Tokyo Ghoul:re manga as the primary visual and biological reference, especially Furuta's confrontation with Eto, his later displays of centipede-like kagune, and his final battle with Kaneki in chapters 172–174. Use the anime as a secondary source for color, lighting, and movement. Do not merge his transformation with Eto's Owl Kakuja, Kaneki's Dragon transformation, or another character's kagune. The final result must faithfully reproduce Furuta's actual visual identity while expanding his existing abilities into a polished, satisfying, mechanically complete Minecraft combat progression.

## Said while it was being planned

As I said, we use Gecko Lib and that's going to remain a thing. Your job is basically replacing the owl DNA with this one, the new one. So just get to work. There's not much to verify. Just disable one and start building another that is simpler and that doesn't require you to make any models. It just requires you to adapt the current models we got for the other paths and making this one. It's that simple, bro. You're overcomplicating things.

I mean you deff need to create new ones wtf I meant you can use parts of others

(Note: the mod has no GeckoLib or Blockbench assets; every path's kagune is a procedural mesh. "Use parts of others" was taken as
reusing those meshes' building blocks: KakujaModel.Spine, CentipedeTail, CentipedeMotion, the Ukaku sink.)

## On the first previews (with a screenshot of them and the manga's many-eyed panel)

I would argue that I really don't enjoy how the mouse being closed looks, and you can also look at it and see that it's not right. It looks weird. I like when it's open, though. It looks cool, but also the main base Kagune, it looks really thin, really weird, honestly. I don't really like the look of it. It's strange. And where's the one from the reference picture? I also don't see it anywhere. It should be a stage or something, right? I mean, I don't know. And also the final Kakuja armor looks anything but threatening. It looks like something you'd laugh at. If I'm being honest,

Final form btw

(Three images of the kakuja's final form: a black body covered in red fingerprint-like whorls, three glowing red vertical slashes
across the belly, a glowing white star-shaped eye, a wide grin of big block teeth, spiky black hair like a crown.)

## On the many-eyed kagune, session 2 (verbatim, with their screenshots)

With the manga's "let's play house" panel: "It doesnt look like the reference still, its still off, forced... I dont like it"

Then, side by side with the panel: "How are they even close to looking the same? I mean, like, the other one is, like, badass. It's,
like, sharp edges. It has, like, a bunch of, like, other weird tendrils and all. Like, yours is just, like, flat. It's flat. They're
all flat and look bad. They look like a fucking 2D fucking piece of shit. ... Even the pose, man. Like, dude, you're just doing the
bare minimum."

On the dark grey disc eyes with red slits (crossed out on a screenshot): "never use these, hell no ugly as shit". **Never use disc
eyes on this kagune; the almond eyes stay.**

## The 7.0.0 patch (after the first in-game test; verbatim)

Patch:
Energy bar is now the same shared bar of the Healing, decreases slower obviously,  but energy and food are now the same bar.

All paths should be Kaneki DNA, Ayato DNA (etc).

Ability list for all stages in the picture,

Add mutations as you see fit, one of them though has to be "Thought you'd get rid of me that easily?" from  the earlier Owl DNA, I liked the ability, just change it to make it take longer to regenerate

update the minecraft controls to be Ability 1, 2 , 3 etc and the name HexKagune (of the category).

(The picture: Base Kagune: Rinkaku Barrage, Predator's Grasp, Crimson Impaler, Flesh Devourer, Centipede Rush, Thorn Cyclone.
Evolved Kagune: Dragon's Tendrils, Thousand Thorns, Living Fortress, Predatory Evolution. Kakuja: Centipede Execution, Kakuja
Rampage, Dragon's Descent, Monstrous Regeneration, Final Calamity; each with the one-line description it had.)

Bugs found:

1- Mouth kagune is see through in some areas
2- Many eyed mess is way too thick in the back.
3- theres no form like the one in the reference (the manga's "let's play house" panel)
4- M1 combos are very lackluster, very boring and unoriginal ngl
5- Kagune on first stage is also super thick in the back.
6- Kakuja m1s are horrible, no combos what so ever???
7- Kakuja should also have an ambient animation,

the eye in kakuja is badly placed and missshaped btw. (Then: it must be a star like their sketch, a classic five-pointed star
big on the face over the grin, glowing; "the hair is far too small".)

"Make sure that it's ferocious. It's meant to be fucking scary. It's meant to fucking bite hard ... For every single one of them.
Like, fuck, give them some life." (the fanged kagune)

# The user's requests for 6.2.0, part 2 (verbatim, binding)

Recorded 2026-10-18. Both messages are the user's own words, unedited (voice-typed). Request 1 came first; request 2 was
added while the work on request 1 was being planned.

## Request 1: Up We Go, animation cancelling, the Kakuja movesets

First of all, add a new mutation called "Up We Go". This mutation improves the existing right-click movement ability, making the player launch significantly higher and travel further upward. The movement should feel much more powerful and fluid, with proper momentum and smooth acceleration rather than an abrupt vertical launch.

More importantly, all abilities must now be usable while performing other movement animations, including during the right-click launch. Introduce a proper animation-cancellation and transition system that allows the player to activate any ability at any point, even midway through another animation. This should not result in snapping, broken poses, overlapping animations, or unnatural movements. Instead, animations must smoothly transition into the newly activated ability. For example, if the player is spinning and activates a punch, the spin should quickly and naturally conclude as the body transitions into the punching animation. Upper-body attacks should be able to blend with ongoing movement whenever possible, while full-body attacks should smoothly take control of the animation. Make sure this works consistently with every existing ability, including while airborne.

Now for the biggest addition: the Kakuja abilities. Currently, the Kakuja forms need their own proper combat movesets. Every existing base-form combat ability must have a unique, significantly enhanced Kakuja counterpart. Do not simply increase damage, range, or particle quantity. I want entirely reimagined attack animations, more impressive visuals, stronger physical interactions, and abilities that actually utilize the Kakuja's monstrous anatomy. Every ability should feel substantially more powerful, aggressive, and visually spectacular while remaining recognizable as an evolution of its original version.

Starting with the existing punch ability, its Kakuja version should become an incredibly powerful charged punch that sends the target flying directly away from the direction of impact. The most important requirement is that the entity travels in a perfectly straight horizontal line, without being launched upward or downward. Its movement direction must be determined by the punch's impact direction, not by random knockback or the target's position relative to the ground. Once struck, the entity should continue being propelled straight forward with tremendous force, smashing through blocks and obstacles along its trajectory. Anything obstructing its path within the ability's ten-block terrain-destruction reach should be violently broken apart, with debris and impact effects accompanying the destruction. All affected blocks must automatically repair themselves afterward, restoring the original terrain without permanent damage.

The punch must retain its existing charge mechanic. The longer the player holds the button, the greater the force, and the further the target travels. A quick tap should already produce an impressive knockback, while a fully charged punch should send the victim flying an enormous distance, smashing through multiple obstacles without losing its horizontal direction. Use physically convincing acceleration, a powerful initial impact, convincing debris, and appropriately timed screen shake and sound effects. The attack should feel genuinely devastating, not like ordinary Minecraft knockback with exaggerated numbers.

Next, completely redesign the tap kick for Kakuja. This remains a high-speed targeted dash ability, but now incorporates the two enormous tentacle-like appendages positioned above the Kakuja's arms. Upon activation, the creature should rapidly dash toward its target, almost appearing to disappear from its original position due to its speed. As it reaches the victim, the first upper tentacle violently thrusts forward and skewers the target, briefly holding them in place. Immediately afterward, the second tentacle sweeps across the target's head and neck area in an extremely fast, sharp cutting motion. Finally, the Kakuja twists its upper body, performs a powerful spinning kick, and sends the victim flying away.

This entire combination must be exceptionally fluid, fast, and visually beautiful. The tentacles should move independently with believable momentum, their heavier ends trailing slightly behind their roots as they accelerate and curve. They must not teleport between poses or behave like rigid sticks. The dash, impalement, slash, body rotation, and final kick should flow together as one continuous animation with precise impact timing. Add appropriately restrained blood particles, sharp motion trails, impact flashes, and satisfying sounds. Keep the ability responsive rather than making the player sit through an excessively slow cinematic sequence. If no valid target is found, retain the existing no-target behavior rather than performing the entire attack against empty space.

Next, rework the Kakuja version of the grab ability into a completely different attack based around the creature's back. Instead of grabbing a target normally, the Kakuja should aggressively lower its head and upper torso, hunching forward as its shoulders and back tense up. Immediately afterward, numerous enormous bone-like spikes erupt outward from its back, rapidly extending into the surrounding area. These spikes should be organic, irregular, wickedly sharp, and visually consistent with the Kakuja's existing anatomy and textures.

The spikes must not all emerge simultaneously in a static pattern. They should burst outward in a fast, slightly staggered sequence, with different lengths, angles, and trajectories, creating the impression of a living creature violently expanding its skeletal structure. Enemies struck by the spikes should take damage and receive the existing bleeding effect. Some spikes can briefly impale or stagger targets before retracting, giving the attack much more visual weight. The creature should visibly react to the eruption through its shoulders, spine, and arms. After the attack, the spikes should retract smoothly into its body rather than instantly disappearing. Add bone-cracking sounds, flesh movement, blood effects on successful hits, and subtle environmental impact effects where appropriate.

For every other existing base-form ability that I have not specifically described, I want you to take creative responsibility for designing its Kakuja counterpart. First, inspect the entire current combat system, identify every existing ability, its activation mechanics, animation, intended purpose, damage behavior, cooldowns, and special effects. Then design a unique Kakuja version of each one. Do not overlook any ability, and do not create arbitrary replacements without understanding what the original ability is supposed to accomplish.

Be creative and ambitious with these remaining attacks. Some abilities could evolve into enormous sweeping bone-blade attacks, others into devastating tentacle combinations, rapid successive strikes, piercing kagune extensions, circular area attacks, or brutal attacks that incorporate the creature's entire body. Choose whichever transformation makes the most sense for each original ability. The important thing is that every Kakuja attack has its own recognizable identity, unique animation, satisfying combat purpose, and visual spectacle. Avoid turning everything into generic explosions, shockwaves, or oversized particle effects. Make the creature's anatomy itself the main attraction.

Make extensive use of the appendages, claws, body weight, shoulders, and skeletal structures that already exist in the model. Where appropriate, create additional animated bone or kagune extensions that emerge during attacks and retract afterward. Ensure these extensions are properly attached to the creature's skeleton, respect its body proportions, and never appear as floating objects or disconnected geometry.

All new combat animations should follow believable physical principles. Heavy appendages must have inertia, attacks must demonstrate anticipation and follow-through, body rotations should transfer force convincingly, and different anatomical parts should respond naturally to movement. Use smooth interpolation and appropriate animation blending. Avoid rigid movements, unnatural pauses, awkward slow motion, instant rotations, and abrupt pose transitions.

The new animation-cancellation system must also work with all Kakuja attacks. Players should be able to chain abilities together naturally and perform attacks while airborne, without the creature losing its shape or breaking its animations. However, animation cancellation must not accidentally duplicate damage, trigger multiple hits from a single attack, bypass intended cooldowns, or leave temporary geometry and effects behind.

Keep the combat responsive, visually impressive, and multiplayer-compatible. Damage, bleeding, knockback, terrain destruction, and restoration must be handled reliably, with effects synchronized correctly between clients and the server. Temporary destruction must preserve the original block states and avoid permanently damaging the world. Make sure particle effects, temporary geometry, and procedural animations are optimized so even the most impressive attacks do not cause significant FPS drops.

Do not unnecessarily modify unrelated mechanics, existing progression, menus, mutations, models, or abilities outside the changes requested. Preserve everything that already works correctly.

The final goal is to make Kakuja feel like a completely different level of power compared to the base form. Every attack should feel monstrous, heavy, fast, vicious, and carefully animated. I want the difference to be immediately obvious the moment the player transforms, not just through higher damage numbers, but through the quality of the movement, the creature's physical presence, and how dramatically its attacks interact with enemies and the environment.
## Request 2: the M1 stun, a cooldowns command, first-person M1, Kakuja M1/M2, and the handoff

Also, I need to add that by default, punching someone like like with your left click it should stun them at least for half a second so that they can't fight back. Like currently, iron golems are fucking me up just as soon as I touch them, you know what I mean? So they should be stunned. For like, just a little bit, so that they can fight back while you're doing your combos. Um, also, we need a command to reset cooldowns on everything. Like, a command that does like, I don't know, do like, slash kagune, unlock, I mean, slash kagune, cooldown, cooldowns off or something, so that even the mutations and all the other things are instantly, like, refreshed when they finished, you know what I mean? Uh, like, when they're used, I mean. Also, the, there's a current bug that first-person of the of um, of Ito, the left-click doesn't have, I mean, there's no first-person animations. They have to be added and they have to match exactly the ones on third person. You already got the deal I just want to see what, what I'm doing. You know what I mean? I don't think I'm gonna be able to finish this so uh, It's better if you prepare a to-do list for the next AI to take over It's gonna be huge. Just on another account. You already know the deal. So give me a prompt here on the chat and uh, just a handoff that is completely ready for the AI to instantly pick up on everything we're gonna do And um, yeah, everything that is left to be done. Um, also want to add that the Kakuji of U1 also needs to have custom M1s in M like M2s should be like I don't know I'll let you decide what's best for M2 for combat. I can't think of anything, so it's up to you. Just get ready for that. Just

## Request 3: the revive (You Thought You'd Get Rid of Me That Easily?)

And one more thing, when the player does explode in the Itto form, the new mutation, it looks beautiful, it's perfect, but I want even more blood and I want like every single entity to just act as if the player died for real. Like forget completely whatever the creature is, a boss, anything, literally everything forgets that the player is is there. They just pretend like he died.

## Request 4

no agents!!!!!!!!!

## Request 5: the giant arms (from testing the Fissure)

oh wait thats the rear view my bad, its good then, however the blades are not meant to move so much like wtf, the red part is the muscle, it should be the one doing most of the work

## Request 6: rename the path to Owl DNA

Also, when you're done, we got one more thing to do. We got to rename this whole path Owl DNA. Because this is not Ito accurate. This is not Ito from the show anymore. This is some kind of fucking experiment, clearly. The ID for the slash path, like Kagunay path command, is going to be slash Kagunay space path space Owl. And everything else in the menu has to be renamed to that as well. No longer, I don't want to see Ito anywhere in the mod.

# The user's requests for the 6.2.0 release, part 3 (verbatim, binding)

Recorded 2026-10-19, session on branch `claude/exciting-shannon-5imyb5`.

## Request 1: V2 gone, the Owl polished, the Speed Kick again

get the latest version edited from the mod.

our goal now is to delete the Kakuja V2, improve models of the V1, smoother moves, more polished look, a cooler mask.

Speed kick rework AGAIN.
First of all, a  better dash , complete vanish and unable to be attacked while dashing, max range of 20 blocks.

Dash position of the stick figure (more or less) .

the kick should now be to the but in this position of the reference (kizaru) and send whoever is hit way further and through more blocks. (keep the same ragdoll effect I like it).

(Two images: a stick figure leaning far forward in a sprinter's lunge, one arm reaching ahead, the other swept back, one leg
stretched out behind; and the "Yonko agenda" Kizaru frame, a straight-leg kick into Luffy's middle.)

## Request 2 (on seeing the first kick render)

put some more power into that KICK man  go crazy

(An image: a second stick figure, the body laid out flat in the air behind one straight leg, the other knee tucked, one arm
reaching over, the other bent under the head: a flying side kick.)

## Request 3

Rules:
NO AGENTS.

Changes:
Flesh arm travels slower.

New mutations and additions:

True Anonamy: Flesh Leg stomp mutation, the flesh leg grows larger, bigger AOE and range, the leg grows bone spikes all over the flesh causing bleed on hit.

Witness this: at full charge, at the end of Big Punch ability, the player appears behind the entity/player hit, and punches him upwards with an uppercut (at least 6 blocks high) and jumps after him speed blitzing him nonstop in midair punching him from one side to another like the gif I sent, at the end  the user appears above the target and kicks him downwards creating a big crater on the floor. (during this whole thing the entity is completely frozen unable to fight back or move, for players, they can only look around, kagunes are unusable  during this combo.)

Hurry up this is all in general taking too long.

(A zip with a gif: Rock Lee knocking Gaara around in midair from every side, white impact flashes.)

## Request 4 (on seeing Witness This)

I need a gif of the Witness this, that shit does not looks very good from what im seeing.

thats what I thought, it should mix a bunch of blows, kicks etc at extreme speeds around a sphere.

push an update release 6.2.0 when this move is patched

## Request 5 (the Owl's mask)

the mask should look like this

(Two anime frames of the One-Eyed Owl's mask: a smooth, tall shell tapering to a pointed V chin, a ridge down the middle, one
round eye hole with a red pupil, scars over the other eye, thin cracks across it; no beak, no feathers.)
