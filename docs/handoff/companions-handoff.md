# START HERE (state after 2.2.7)

New operator command: `/kagune eto marry`, `/kagune touka marry` and `/kagune ken marry` (with an optional player after it, like `max`). It marries you to that companion at once and fills everything about the relationship.

- **What it sets**:
  - married, the same as a ring taken (the "Married to ..." tag, the Romance card, a spouse at home)
  - regard 100, met and befriended
  - every chapter of their courtship closed
  - dating since and wed today (earlier dates are kept)
- **What it clears**: nothing held against you any more: no sulk, grudge, offences, being ignored, jealousy, or make-up scene owed.
- **If they're loaded**: they're giddy with you, show the ring gesture, and hearts appear. The companion doesn't need to be near.
- **One spouse each**: if you were married to another companion, that marriage ends. If this one was someone else's partner, they leave them for you. The command says so in both cases.
- What they've taught you stays taught.
- **Code**: `Courtship.wed` (pure, tested in `CompanionTests.wed`), `ControlCommands.marry`, `EtoBrain.married`. Protocol stays 83. Not tested in game.

# State after 2.2.6

Village humans now come up to you on their own. A villager who knows you will sometimes notice you (within 12 blocks, in sight), walk over and say something. Why they come, how often, and what they say depend on their temper (all 12) and how close you are. Partners come most. Strangers never do.

- **How often** (as long as you're nearby): about every 10 minutes if they like you (regard 20+), every 5 minutes if they're close (60+), every 2.5 minutes for a partner. Their temper speeds that up or slows it down: flirty ×0.6; cheerful, bold, playful and romantic ×0.7; kind ×0.85; dreamy and sarcastic ×1.1; nervous ×1.4; stoic ×1.6; shy and grumpy ×1.8. Only one villager comes to you every 45 seconds, so a street never swarms you.
- **Why they come**:
  - **Hello**, just to say hi.
  - **News**: the village's latest, their temper's way in ("Breaking news from our thrilling little village: ...").
  - **Gossip** about a neighbour nearby, by name and trade.
  - **A gift** from their own trade (once a day, close friends and partners only): the baker's bread, the beekeeper's honey, the smith's iron, the florist's flowers. You get it and see "X gave you ...".
  - **Worried**, when you're under half health ("You're bleeding on my shoes. ...Are you alright, though? Really?"). A partner also hugs you.
  - **Partners only**:
    - They missed you, if you haven't seen them in 2+ days. They hurry over and hug you.
    - A kiss: they call out on the way ("Kiss ambush incoming!"), walk up and kiss you.
    - A hug.
    - A walk: they take your hand (tap Shift to let go).
- **Their words**: every temper has its own lines for each reason. A shy one fidgets, a grumpy one grumbles, a sarcastic one jabs, and a romantic one gets poetic.
- **When they don't come**: if you've hit them, if you're talking to someone, or if you've left the village. Village people only come by day (they're home at night). Talk to one on the way over and the visit is dropped.
- **Code**:
  - `village/HumanApproach` (pure: how often, why, the lines).
  - `server/HumanApproachServer` (noticing, walking over, arriving), hooked into `HumanBrain.tick`.
  - Kisses and hugs use the villager's existing walking-up.
  - Tested in `NpcTests.approach226`.
- Protocol stays 83. Not tested in game.

# State after 2.2.5

A male player's chest is a little smaller. The upper chest no longer stands out as far: it's broader than the waist, as before, but only 60% as far out past the torso as the White Reaper's, across the shoulders, in front and behind. The arms hang a little closer in to match. The White Reaper keeps his build exactly as it was.

- Widest across the chest: 4.41 px a side, down from 4.68 (the torso is 4). Out in front of the belly: 0.63 px, down from 1.05. The back: 0.21 px past the torso, down from 0.35. The waist is unchanged, so the taper (the V) stays.
- Arms 0.45 px further out than a bare torso, down from 0.75.
- The Rinkaku mark on a man's back follows the slimmer back.
- **Code**:
  - `ReaperBuild.PLAYER` (the Reaper's outline at `PLAYER_SHARE` 0.6 past the torso) and `PLAYER_ARM_OUT`.
  - `ReaperBuild.chest` and `back` take an outline.
  - Used by `BuildLayer`, `RinkakuMarkLayer` and `KaguneBodyAnimation`. The Reaper's `AgentRenderer`/`AgentModel` keep `PROFILE` and `ARM_OUT`.
  - Tested in `AgentTests.reaperBuild`.
- Preview, rendered off-game: `docs/handoff/player-build-2.2.5.png` (player before, player now, the Reaper; front, side, three-quarter). Made with `tools/ReaperPreview.java <skin> <out> player`. Protocol stays 83. Not tested in game.

# State after 2.2.4

A kiss for every mood. It works the same as before: they lean in and wait, a tap of Shift is a quick kiss, holding makes them blush, and holding longer makes them pull you in. Now each mood gives its own kiss: how they wait, how long, what they say, how they move, and what they do after. A kiss is never turned down any more. Ask an angry partner for one and you get an angry one.

- **Angry** (sulking): grabs your collar with one fist, the other clenched, looking away and tapping a foot ("Well? Hurry up."). Waits only half as long as usual. After, they shove you back with arms crossed ("There. Happy? I'm still annoyed. ...Less annoyed."). **The kiss ends the sulk.** Worth fewer hearts.
- **Possessive** (jealous): pulls you down at once with both arms round your neck, gripping tight ("Mine. Got it? Mine."). Clings to your arm after. **The kiss ends the jealousy.**
- **Giddy**: hands clasped at the chest, bouncing on their toes, head bobbing. After, a burst of laughter ("Again! No... again!").
- **Tender** (affectionate or clingy): both hands up to your face, slowly, swaying. After, forehead to forehead ("Stay like this. Just a minute."). Worth more hearts.
- **Desperate** (lonely): throws their arms round you with their whole weight, trembling a little. Waits longer, and won't let go after (a hug).
- **Sleepy**: arms hanging, head lolling, drooping in and catching themselves. Waits the longest. After, their head on your shoulder.
- **Hungry** (bloodthirsty): head tipped as if to bite, a hand at your jaw, the other on the hip. After, a nip at your lip and licking their lips ("You taste like dinner.").
- **Teasing** (bored): hands behind the back, bobbing in and out of reach. **Dodges your first try**, laughing ("Too slow! Try again."), then leans back in for a real one, then hops away.
- **Sweet** (happy and anything else): exactly the kiss as it was.
- Each companion (Eto, Touka, Ken) has their own lines for every kind: 2 for each, and which one you get also depends on whether it was quick, held or pulled.
- Hugs are still turned down when they're sulking or jealous. Their own asks for a kiss still follow their mood (2.2.3: mad or jealous, they don't come asking).
- **Code**:
  - `npc/MoodKiss` (pure): styles, waits, the dodge, after gestures, hearts and lines.
  - `client/animation/MoodKissPose` (pure): the body for the wait, the blush and the pull.
  - `EtoBrain`'s kiss machine picks the style from the mood when the kiss begins.
  - `S2CEto` carries it: **protocol "83"**, so install on both client and server.
  - Tested in `CompanionTests.moodKisses`. Not tested in game.

# State after 2.2.3

A partner's mood now shapes what they do, not just what they say. Mad or jealous, they never come up asking for a kiss, and they turn yours down. Lonely, affectionate or clingy, they come for kisses far more often. Sleepy or starving, less. Once a sulk or a jealous spell has passed, they come to you on their own to make it up.

- **Coming for a kiss** (their own asks and quick pecks), against their usual: lonely ×2.2, affectionate ×2, clingy ×1.8, giddy ×1.6, bored ×1.2, happy ×1, sleepy ×0.5, starving ×0.4, sulking or jealous never. Their hand-holding, hugs and leaning already followed their mood; the scenes they come up with already had their own for every mood.
- **Your kisses and hugs**:
  - Sulking, they're turned down ("Touch me and lose a hand." / "...Not right now. Give me a little while.").
  - Jealous, the same ("Kiss whoever you were with earlier. I'm sure they'd love it.").
  - Starving, a kiss is turned down ("If I kiss you now I'll bite. Feed me first, darling.") but a hug is fine.
- **Reassuring**: jealous, their first page offers "It's only you. It's always been you." That ends it (+2 regard) in their own words.
- **Making up**: after a sulk (being hit more than once, a kiss left hanging twice) or a jealous spell, they walk up to you once it's over with a scene of their own: Eto rewrote your death so you live, Touka shoves a can of coffee at you, Ken comes to make it right. You can make up back (a hug, a kiss, a hand) or tease them. Each companion has 2 (`make_up`, `jealous_over`), never picked at random (`when: makeup`).
- **Code**: `npc/MoodActs` (pure, tested in `CompanionTests.moods`); `EtoBrain.makeUp`; the refusals and reassurance in `EtoServer`. `SceneTests` checks every companion has both make-up scenes. Not tested in game.

# State after 2.2.2

A ghoul's and a human's child, once grown (adult), uses its kagune's kakuja in a fight. A Rinkaku child grows two centipede legs out of its back beside its tail, which strike what's in reach by turns. An Ukaku child's wings become Ukaku's first kakuja, and its blows land harder. Other children still never have a kakuja.

- **Centipede legs** (tails):
  - The same motion and mesh as a player's: plates in greys, the flesh between in the child's own kagune colour (`KakujaPalette`).
  - At rest they curl round it; with a target within 5.5 blocks they take turns to rear up and strike.
  - Each strike deals the child's blow × 1.4.
- **Ukaku's first kakuja** (wings): drawn as a player's first kakuja (`WingForm.KAKUJA` through `WingRules.child`), and its blows deal × 1.3.
- **When**: only while it fights with its kagune out, so a grown child at home looks as it always did.
- **Code**: `HalfBlood.kakuja` (pure, tested in `FamilyTests`); `CentipedeRenderer.childLegs`; the strikes in `ChildKagune.fight`. Not tested in game, and not previewed off-game.

# State after 2.2.1

A child of a ghoul and a human (a human player and Eto, Touka or Ken) is now rarely carried to term. 7 in 10 such pregnancies are lost, somewhere in the first half. The one that lives is born a one-eyed ghoul, always, with the ghoul parent's kagune whole. It has 60% more health, 50% harder blows (fists and kagune) and 30% more speed than any other child, and it fights with the White Reaper's dashes. Its kakuja once grown (Ukaku's first kakuja, Rinkaku's centipede legs) comes in the next version.

- **Losing the baby**: the pregnancy ends between 15% and 55% of the way through. The bump goes, and both parents are told gently: "You lost the baby. A ghoul's child and a human's rarely lives. It isn't anyone's fault."
- **The dashes**: in a fight it closes a 3.5 to 14 block gap in one burst, stopping just in reach. Hurt, it dashes aside out of the way of the next blow. It has 3 charges, one back every 5 seconds, the same rules as the player's (`ReaperDash`).
- **The rest**: two ghouls' children and two humans' children are as they were. Children born before keep everything they had.
- **Code**:
  - `family/HalfBlood` (pure, tested); `Genome.half` (a tenth saved part; older saves read as not half);
  - the pregnancy's `lostAt` (an eighth saved field), with the loss in `Pregnancies.lost`;
  - `server/HalfBloodServer` for the dashes; the health, speed and damage in `ChildBrain` and `ChildKagune`.
- **Tests**: `FamilyTests.halfBlood`; the older genome tests now leave half-human pairs out. Not tested in game.

# State after 2.2.0

Companions are far more tolerant, and you can always say sorry. A first accidental hit or a missed kiss is brushed off with a word. Only more of them close together put your partner out, and then for 20 seconds to a minute, never the old 5 minutes. When they're put out, talking to them opens "I'm sorry." / "It was an accident. I swear." and that forgives you on the spot.

- **What there was before**: an apology existed but was hidden and harsh.
  - Hitting your partner: −8 regard and 2 minutes of them refusing to talk.
  - A kiss left hanging: 5 minutes of being ignored.
  - The only way out early: bring a baby animal on a lead, or hand over a written book (a letter). Both still work and still forgive at once.
- **Tolerance** (`npc/Tolerance`, pure, tested). What they hold fades 3 minutes after the last offence.
  - **Hits**: the first is brushed off ("Ow! Watch it, idiot." / "Ah— it's okay. Accidents happen."), and so is a second one in the middle of a fight (you hit something else in the last 3 seconds). The next puts them out (−2, a 20-second sulk). More than that makes them cross (−6, a 1-minute sulk).
  - **Missed kisses**: the first is shrugged off ("...Fine. Whatever. Not like I wanted one."). The next puts them out (ignored 20 seconds), then 1 minute.
- **"I'm sorry."**: forgives everything at once (+1 regard). If you've hurt them again and again (4 or more offences in 3 minutes), they forgive you but want a 15-second moment first.
- **Tests**: `CompanionTests.tolerance`. Not tested in game.

# State after 2.1.9

Rinkaku's final kakuja (Peace or War) now breaks the nerve of whoever faces it. Doves of Rank 0 to 4 and the two weaker tiers of wild ghouls run from it, and keep running while it's in sight. Rank 5 to 7 Doves and the strongest wild ghouls stand and fight, but tremble the whole time, and every few seconds their nerve gives and they back off before coming again. Rank 8 and above (Special Class, the White Reaper) stand as before.

- **Runners**: they drop their weapon, flee to about 26 blocks and stop to look back, and run again if you come closer. It's kept up twice a second while you're in sight within 32 blocks.
- **Shakers**: a fast, small, uneven tremble of the whole body, drawn on every client. About every 4.5 seconds (each at its own moment) they back off for a bit under 2 seconds, then come at you again.
- **War** used to send every wild ghoul running. Now the strongest tier shakes instead. The Dragon still sends everyone running, and Doves still freeze ("falter") on first sight as before.
- **Code**: `FinalSight.Nerve` and its rules (pure, tested in `AgentTests.dread`); `Sightings.dread`; the investigators' `routed` state; the shakers' breaks in `AgentBrain` and `GhoulNpcBrain`; a synced `AFRAID` on `CcgAgent` and `GhoulNpc` for the tremble. Protocol 82: install this version on both client and server. Not tested in game.

# State after 2.1.8

Ken's final-kakuja lessons are harder. Before he teaches the Full Kakuja, the Dragon or Ukaku's final kakuja, he asks you to prove you're ready. The proof is set the moment you ask, tracked on the action bar, and the lesson is taught (or the trial lived through) once it's done.

- **The Full Kakuja**: eat 5 ghoul meat. "The Full Kakuja isn't taught. It's eaten."
- **The Dragon**: kill 3 Doves of Rank 6 or above. "The Dragon only wakes in someone who's stood at the edge and come back."
- **Ukaku's final kakuja**: kill 10 wild ghouls with your first kakuja's wings out, then the trial as before. "The storm has to be fed first."
- **Unchanged**: the centipede and Touka's lessons. The mastery each lesson needs is unchanged too.
- **Code**: `Teaching.Proof` (pure, tested), counted in the player's memory of Ken (`NpcMemory.PROOF`). Ghoul meat eaten (`QuestServer.ate`) and Doves and ghouls killed (`EtoServer.died`) count toward it. Tests in `CompanionTests.proofs`. Not tested in game.

# State after 2.1.7

Ukaku's final kakuja gets the White Reaper's dashes: tap sprint twice to dash the way you're moving. You have 3 charges, the action bar shows "Dash 2/3" as one is spent, and one comes back every 5 seconds until you're at 3/3 again. No kakuja vanishes any more. Also, after each release, only the 5 newest releases and the 5 newest action runs are kept.

- **The dash**:
  - A fast, low burst like the Reaper's own: about 6 blocks over 4 ticks, no fall damage from it.
  - Direction: W, S, A, D and between; with no keys, the way you look.
  - Works with the wings out and with the Steel Blade.
  - Charges come back one at a time, each 5 seconds after the last.
  - With cooldowns off (creative, `/kagune cooldowns off`) they're always full.
- **Code**: new pure `ReaperDash` (the charges, the double tap, the direction) and `server/ReaperDashServer`. The client sends the way you're moving (`C2SUkakuAction.REAPER_DASH`, so the protocol goes to 81: install this version on both client and server).
- **No vanish**: breaking into a sprint in Ukaku's Steel Blade or Rinkaku's Full Kakuja no longer makes you invisible. Their right-click dash stays. The ability guide and the Rinkaku menu say so.
- **Releases and actions**: a new cleanup job in the release workflow runs after each release. It deletes every release but the newest 5 (their tags are kept) and every finished workflow run but the newest 5. A run still going is left for the next cleanup.
- **Tests**: `ControlTests.reaperDash` (three charges, one back every 5 seconds, the double tap, the direction surviving the packet). The Steel Blade's board shows the Reaper's dash and no vanish. Not tested in game.

# State after 2.1.6

Fix: Rinkaku tails no longer swing across the back after an attack or when the kagune comes out. Each tail's rest spot was on the opposite side from where the original's own motion carries that tail. So every time a tail eased back into rest (after a blow, when transforming), it crossed over to the other side, which looked like the tails rotating or swapping. Once it settled the picture looked right, because the rest shape is symmetric. The kagune's model, shape and rest position are unchanged. Each tail now rests on its own side.

- **The cause**: the original lays a tail out along its owner's left, times the layout's fan side (`TendrilChain.initialize`, read from the original jar). With no fan side, it puts an even tail on the player's right. `RinkakuRoot.side` read the sign the other way round. That rule decides the rest pose (2.0.51), the root sunk in the lower back (2.0.38) and the block's guard pose (2.0.42). The side `StrikePaths` hands the body clearance had the same mix-up.
- **The fix**: one side rule, matching the original, used by all four. A tail whose layout gives no side takes it from its own place among the limbs, as the original does.
- **Tests**: `ControlTests.rinkakuAlive` now checks the rule against the original's and that every tail rests on the side the original lays it out on. Not tested in game. Protocol stays 80.

# State after 2.1.5

The companions' courtships now come from the mod's own world, not generic chores. The CCG's Doves (some of a given rank), wild ghouls of the Pack, a ghoul preying on a human village, a wild ghoul made your ally, village humans who come to care for you or tell you a secret, a walk through a human village at night, a drink from Touka's cookbook, ghoul meat. Every chapter's scene is rewritten to match.

- **Eto**:
  1. walk her through a human village after dark, her following you (2 minutes among its people);
  2. make a village human care about you (their regard at 60);
  3. kill 3 Doves while she watches, her following you;
  4. get a village human to tell you their secret;
  5. the Library in the dead of night.
- **Touka**:
  1. make her a drink from her own cookbook (the Anteiku Blend, or an RC Tonic or Kakuja Shot made with ghoul meat);
  2. kill 4 Doves asking about the cafe;
  3. somewhere high;
  4. kill the wild ghoul preying on her human friend's village (a ghoul killed with village humans within 32 blocks);
  5. kill the investigator hunting the rabbit: Rank 4 or above. She gives you her brother's rabbit's foot.
- **Ken**:
  1. 3 ghoul meat for the ghouls who won't kill;
  2. make a lonely wild ghoul your ally;
  3. stop a ghoul of the Pack;
  4. with him following you, kill the investigator tracking him: Rank 5 or above;
  5. one sunrise not alone.
- **Scenes**: 10 new or rewritten chapter scenes (Eto's walk past a child's window, her question about being loved by a human; Touka's human friend and why she studies; Ken feeding the ghouls who won't hunt, his first friend at the cafe, the fight beside him...).
- **Saves from before**: whoever is mid-chapter keeps the chapter number and its count starts again (it was counting something else). Every chapter closed still means waiting to be asked out.
- **New chapter kinds** in `npc/Courtship.Task` (appended): DOVES, DOVES_WITH, GHOUL_PACK, VILLAGE_GHOUL, ALLY, HUMAN_FRIEND, HUMAN_SECRET, VILLAGE_NIGHT. A chapter can ask for a Dove of a minimum rank (`Chapter.rank`).
- **Hooks**:
  - Dove and ghoul kills in `EtoServer.died` (`AgentBrain.rankLevel`);
  - an ally made in `GhoulNpcTalk`;
  - a human's regard and secret in `HumanTalk`;
  - the night walk in `CourtshipServer`.
- **Tests**: every chapter asks for something of the mod's own world (no plain monster kills, sits, walks or vanilla items); a Dove below a chapter's rank doesn't count; a chapter from before starts again once. Protocol stays 80.

# State after 2.1.4

Romance with village humans now has words for every personality. You can flirt with anyone (strangers, friends and close friends each answer differently). A partner gets conversations of their own. Kisses, hugs and "I love you" have more variety.

- **Flirting**: "Flirt a little." (or "Flirt with Hana." once you're close) on every non-partner's talk page.
  - All 12 personalities have their own answers for a stranger (2), a friend (3) and a close friend (3). A close friend's answers push you to ask them out.
  - If you're not who they fall for, it's a kind no (2 each) and nothing more.
  - A flirt that lands warms them once a day: the flirty and the romantic most (+5), the stoic and the sarcastic least (+1).
- **A partner's talk**: three at a time on the "Talk a while." page, beside the rest of their life.
  - "What do you love about me?": their personality's opener, then what they look for, found in you (by their taste: strong, gentle, funny, mysterious, clever, daring, loyal, caring).
  - "Where do you see us in a year?" ends with their own dream, with you in it now: "And someday... I'd still like to see the sea. With you beside me, this time."
  - Also: "Do you remember when we met?", "Do you ever get jealous?", "What if I were in danger?" and "Tell me something sweet.", each in every personality's words.
- **More love**: two more lines each, per personality, after a kiss, in a hug and to "I love you", beside the first two.
- **New code**: `village/HumanRomance` (pure). Tests: `NpcTests.romance214` (every personality's romance said cleanly at every warmth and for every taste; each has at least 60 different lines in love). Protocol stays 80.

# State after 2.1.3

Every village human is now someone of their own. Each has a trade, a family, a pastime, an odd habit, a fear, a dream, a secret, a favourite gift and a view of ghouls, all fixed for life, and says them in their personality's voice. A new "Talk a while." page lets you ask about all of it. Getting to know them warms them to you, and their trade decides what they sell.

- **Their lives** (`village/HumanLife`, pure, tested): 21 trades, each with what it does all day (4 lines) and how today went (3):
  - baker, farmer, beekeeper, florist, smith, carpenter, tailor;
  - scribe, candle-maker, miller, cook, shepherd, potter, mason;
  - herbalist, night watch, innkeeper, teacher, gravedigger, fisher, merchant.

  Each life also draws, by its own seed:
  - 24 family situations and 24 pastimes;
  - 20 fears, 20 dreams, 20 odd habits and 20 secrets;
  - 20 favourite gifts;
  - 5 views of ghouls: afraid, hates them, curious, sorry for them, too tired to care.

  Out of 400 villagers in the tests, no two lives were alike. A village gives each of its people a different trade.
- **Names**: 48 women's names and 48 men's. Villagers placed from now on get one that fits their body, with no two alike in a village. Villagers who already have a name keep it.
- **How they say it** (`village/HumanVoice`): each of the 12 personalities frames every answer its own way, with a softer, more careful way for fears, dreams and secrets. So two shy bakers still say different things, and a shy baker and a bold one say the same thing differently.
  - "How are you?" now has 5 small-talk lines per personality, or else how their day at work went.
  - The news has 24 items.
  - 20 bits of gossip name real neighbours: "Have you met Hana? The beekeeper..."
  - Now and then you catch them at their odd habit as you start talking.
- **"Talk a while."** opens a page of topics: what do you do, your family, for fun, what's that you're doing, ghouls, gossip; from regard 20 your favourite thing, your type and what scares you; from 60 what you dream of; from 80 a secret. What you haven't learned yet comes first. The first time you learn each personal thing, it's worth 2 regard.
- **Gifts**: "This is for you." gives them what's in your hand.
  - Their favourite: +8 and hearts.
  - Food, flowers and pretty things: +3.
  - Anything else: +1.
  - Something nasty (rotten flesh, ghoul meat...): -3, handed back.

  Gifts count once a day. Every personality has its own reaction to each.
- **Trades by trade**: a baker sells bread, cookies, cake and pie; a smith iron, buckets, shears and lanterns; a beekeeper honey, honeycomb and candles; and so on. Each sells 5 of their trade's wares (its signature first), one of the cookbook's foods and one farming or basic good. Still no meat and no fish, and every cookbook ingredient is still sold somewhere.
- **The talk screen** says who they are under their name: "Shy beekeeper". The main page is now: How are you / Talk a while / a compliment / a gift / trade / the romance answers. "What's your type?" moved to the topics.
- **Tests**: `NpcTests.lives` (names; lives for life and all different; every personality saying every part of every life cleanly; the topics as they warm; gifts) and the shop test updated. Preview: `docs/handoff/village-talk-2.1.3.png`. Romance lines for every personality come next (2.1.4). Protocol stays 80.

# State after 2.1.2

Eto, Touka and Ken no longer share the same three errands (three books, ten kills at night, a flower). Each now has their own five-chapter courtship. Every chapter asks for something only that companion would ask for, and closes with a written scene that has choices, played the moment the task is done. Once you're together, you can take them on dates.

- **Eto** (her courtship begins once she's a close friend, regard 60, not 100): write her something in a book and quill and sign it; a walk with her after dark (3 minutes, her following you); ink sacs for her pen (4); kill 6 things at night while she watches you; the Library in the dead of night, where she tells you what she is.
- **Touka**: 8 cocoa beans for the cafe (then a cup after closing, where she wants you to be honest); clear 8 monsters at night to keep the Doves off the ward (then her brother, on a rooftop); take her somewhere high, height 100 or more, with her following you (and her wings, the soft way); sit with her at the cafe for 2 minutes while she studies (then she makes you quiz her); a rabbit's foot (why she wanted it).
- **Ken**: a book he hasn't read; coffee at his place, 2 minutes sitting with him (his habit with his finger); kill one wild ghoul that eats people; 3 white flowers for his door (the red ones he used to dream of); stay up with him until dawn.
- **What's handed over** is held out on the talk screen ("Here. What you asked for.") and taken all at once: a stack of 8 beans is one hand-over. Kills, walks, sits, the climb, dawn and the dead of night count on their own. When one finishes with them right there, they come up to you with the scene.
- **The scenes**: 15 chapter scenes, 2 to 4 pages each. Your answers change their regard and what they do; each closed chapter is also worth 4 regard. After the fifth, they ask you to ask them out.
- **Dates**: "Let's go somewhere. (a date)" on the sweet page, once a game day. There are 8 per companion, each a short scene of its own, some only by day, at night, in the rain or at home. Examples:
  - Eto: a bookstall, a rooftop, a graveyard, reading in the rain;
  - Touka: a movie, the beach, the arcade, fireworks;
  - Ken: a bookstore, the river, a bench in the rain, cooking.
  A date brings 3 regard, and its answers count once a day. The journal's dating line now points to dates for getting to propose.
- **The journal**: the Romance tab shows "Courting: chapter 2 of 5", the chapter's name, and what to do next with the count so far.
- **Saves from before**: an errand in progress is read as so many chapters done, with its count started over. All three errands done still means waiting to be asked out.
- **Code**:
  - new pure `npc/Courtship` (the chapters, their words, the state in three bits of the bond's flags);
  - new `server/CourtshipServer` (the timed and place-based chapters, checked every 10 s from the companion's own tick);
  - `moment` and `date` kinds in the talk scripts (`SceneBook`, `Scene.Kind`);
  - `EtoRules.Step.COURTING` appended (BOOKS, HUNT and FLOWER kept, never returned);
  - `EtoTalk.Effect.COURT`, `EtoTalk.Context.held`, `NpcMemory.DATE_DAY`;
  - the Romance card's `chapter`.
- **Tests**: `CompanionTests.courtship`, `EtoTests.errands`, and `SceneTests` (every chapter has its scene; every companion has at least 3 dates for a partner by day and by night). Journal preview: `docs/handoff/romance-courtship-2.1.2.png`. Protocol stays 80.

# State after 2.1.1

The final forms (War and Peace) now have a 5-minute cooldown, shown as a new custom effect, Exhaustion, with its own icon: a kakugan half shut under a heavy, cracked lid, its red dimmed, a drop of RC running from it. Once a final form is let go (taken off, run dry, or the player killed in it), Exhaustion runs for 5 minutes, and calling War or Peace meanwhile is refused with the time left ("Exhausted: War can't be called again for 4:12").

- The rest is kept on the player's profile (`KingRules.RESTS`, the game tick it ends), not in the effect itself, so drinking milk, dying or relogging can't end it early. The server puts the effect back while the rest lasts and takes it away when it's over (`KingServer.restTick`, every half second).
- None while cooldowns are off (`/kagune cooldowns off`, or creative), for testing. The admin preview of a form doesn't start it.
- New `ExhaustionEffect` (harmful, registered with the kitchen's effects), its icon `textures/mob_effect/exhaustion.png` (drawn by the new `tools/exhaustion_icon.py`, 18x18 in the style of the kitchen's four), and lang entries for the effect, its description and the refusal.
- Tests in `KingTests.exhaustion`. Icon beside the kitchen's: `docs/handoff/exhaustion-icon-2.1.1.png`. Protocol stays 80.

# State after 2.1.0

A Rinkaku kakuja's centipede legs and mask now take on the kagune's colour, blending with it while staying distinct. Before, both were a fixed plum. The centipede's segments are always greys, black and white: charcoal plates with pale grey and white rims, or bone-white plates for a kagune too dark to stand apart from them. They carry only a breath of the kagune's hue. The glowing flesh between the plates is the kagune's own colour. The mask has the same grey shell leaning a little more toward the kagune's colour, with the kagune's dark in its edges. The red kakugan lens stays. From this version on, the last number never reaches 10 (after x.y.9 comes x.(y+1).0), the owner's rule.

- New `KakujaPalette` (pure, tested in `ControlTests.kakujaPalette`): the plates (always greys, measured by chroma), their lit rims, the claws, and the flesh (`KaguneTint.recolour` of the original red flesh; a black kagune's hottest flesh glows blood red, as its veins do).
- `CentipedeTail.colours` and `CentipedeMask.colours` take a palette. They're set from each player's kagune colour where the legs are drawn (`CentipedeRenderer`), where the mask is drawn (`RinkakuMaskLayer`), and when either breaks apart (`KakujaFx.shatter`).
- Previews in six kagune colours (crimson, blue, purple, green, black, white): `docs/handoff/kakuja-legs-colours-2.1.0.png` and `docs/handoff/kakuja-mask-colours-2.1.0.png` (`tools/ArtPreview.java` takes `-Dkagune=RRGGBB`). Protocol stays 80.

# State after 2.0.51

The Rinkaku tails are fixed and alive. The texture breakage the owner showed in game is gone. At rest the tails take the shape from the showcase render instead of the original chain's coiled, twitching rest. They stretch and draw in, swells run along them, their middles breathe, and the veins are fine and realistic.

- **Texture breakage**: the scales were a second layer drawn 1.8% outside the tail, and a few blocks off the two flickered against each other and broke up in patches. Now there's one skin: the scales are baked into its texture (`rinkaku_skin.png`, greys the tail's colour is multiplied into, brightened back by `RinkakuRibbon.lifted`) and the glowing veins are drawn over it on a plain white texel (`white.png`). `RinkakuTube` keeps every tail as it's drawn and draws them all after the original's tails (skins first, then veins). Nothing goes into the original's own buffer anymore.
- **Twitching, snapping, short**: the original's chain coils a 4.6-block tail (the default length) into a short knot at rest, twitching as its links settle. Now at rest (`RinkakuIdle`, `RinkakuIdleClient`, applied in `StrikePaths.clearOwner`) each tail takes its designed shape at its own full length: the first pair arcs up off the back and over, the second fans out wide, the third sweeps back. Longer tails spread further rather than tower, and they never come forward over the player. They sway gently (each tail out of step), trail back and down when running, lean away when stepping aside, and lag when turning (the player's motion smoothed so they can't twitch with it). A tail settles into its rest over about 8 ticks and leaves it in about 2 for a move, so blows aren't late.
- **Alive**: each tail slowly stretches and draws in by about a tenth with a quicker small flutter (`RinkakuRibbon.stretch`; past its last point it carries on along the way its end points). A swell of extra thickness runs out along it every 2 s (`swell`), and its middle breathes thicker and thinner. The scales stay put on it.
- **Veins**: fine cores (0.004 blocks), a narrow glow and a faint halo. Each thins along its length, and branches are finer than the vein they leave.
- Tests: `ControlTests.rinkakuAlive` (the rest pose's shape, length, sway, trail and lag, settling; stretch and swell bounds; the swell running out along the tail; the stretched mesh carrying on past its end; the skin's brightening) and the thin veins.
- Previews made with the game's own code (`tools/KaguneShowcase.java` now draws the in-game rest pose, the baked skin and the animation): `docs/handoff/kagune-showcase-2.0.51.png` and the animated `docs/handoff/rinkaku-alive-2.0.51.gif` (a whole sway cycle). Protocol stays 80.

# State after 2.0.50

The Rinkaku tails are reworked as asked: realistic scales, thin where they leave the body, and more glowing veins.

- **Scales**: the thin black-and-white chevrons are replaced by overlapping keeled scales, as on a reptile. They're staggered row to row with each scale's rounded free edge toward the tip, lying over the base of the next. Each scale is darker toward its base where it tucks under, with a raised keel down its middle, a lit rim along its free edge, a thin dark outline and the shadow its edge casts on the scale beyond. The scales are four across a face, large enough to read a few blocks off. They're in greys over the tail's own colour, so every kagune colour keeps its own scales (a faint light stripe still runs along each face). The new `tools/rinkaku_scales.py` regenerates `rinkaku_scales.png`; it tiles seamlessly.
- **Shape** (the owner's drawing): a quarter of its width where it leaves the body, widening steadily to its broadest 78% of the way along, then a short rounded end (`RinkakuRibbon.profile`, `BROADEST`, `PEAK`, `ROOT_WIDTH`). Before, it swelled just past the back and tapered over its second half.
- **Veins and glow**: 6 main veins (was 4), each forked and half of them forked twice, plus a glowing spine down the middle of each broad face: 17 glowing lines a tail, about double. Wider halos, brighter cores, a wide faint aura round the main veins and spines, a stronger RC pulse, and a hotter root glowing further out.
- Tests updated in `ControlTests` (the leaf profile: thin root, widening to its broadest near the far end, rounded end; more veins; brighter cores).
- Previews: `docs/handoff/kagune-showcase-2.0.50.png` (on a body, every colour, a close-up of the scales and veins, at night) and `docs/handoff/rinkaku-scales-2.0.50.png` (the scale tile in crimson, stretched as it lies on the tail). Protocol stays 80.

# State after 2.0.49

The White Reaper fights as a tactician. Each fight he reads his opponent and picks a play ahead of the moves every investigator has: a backflip launched as Up We Go launches one, firing the volley from mid-air; closing in on openings; vaulting over a guard; shooting a target out of the air.

- **Soar**: a backflip built up over five ticks, climbing about 5 blocks (two and a half times a flip) and carrying him about 5 back. He's turned over by the apex, hangs there aiming Dawn down for 6 ticks, then lets the five-ball volley go (a single ball if very close) and comes down without fall damage. He soars when his string has just landed (60%), when he's below 40% health with someone within 5 blocks, and now and then (5% of his reads) from 3.5 to 9 blocks to keep you guessing. Once every 5 s at most, and never with a wall at his back or a ceiling over him.
- **Punish**: an opening within 12 blocks: you're stunned, you just swung and missed, you're in the air, or you're below 30% health (within 10 blocks). He goes straight in (a front flip from past 6 blocks, zigzag dashes from nearer) and starts his string the moment he's in reach.
- **Vault**: you're guarding in front of him (a Rinkaku block facing him, or a raised shield) within 1.2 to 6 blocks. He front-flips clean over you, lands about 2 blocks behind the guard, and strikes at once.
- **Sky shot**: you're in the air 5 to 22 blocks off (a jump, or launched by his kick). A quick 6-tick aim and a shot where you can't step aside (the volley from 7 blocks or more), ready again every 2.3 s.
- **Leading**: every shot of his is aimed ahead along the way you're moving (60% of the ball's flight time), read from how far you moved last tick.
- Pure rules in `npc/ReaperTactics` (the read, the plays, the soar's launch, the lead), tested in `AgentTests`, including the soar's flight simulated as a mob flies. `AgentBrain` reads the fight every other tick (at once after his string lands). The soar flies before everything else, and his shot is one `release` for every kind. Clients draw the soar as its own long flip (`CcgAgent.SOARING` added to the flip's number, `AgentPose.soar`), with no running legs while he drifts down. Protocol 80.
- Preview: `docs/handoff/reaper-soar-2.0.49.png` (a strobe from the side, in his skin: the soar, the hanging aim, and the volley closing on the target). New `tools/SoarPreview.java`.

# State after 2.0.48

The White Reaper can fire a five-ball volley from Dawn. All five lightning balls leave its point together, open out into a ring around the line he aimed along (1.6 blocks across, 1.0 up and down, one ball at the top and the rest evenly round), then close in on the spot he aimed at from every side at once: a pincer. Standing still gets you caught, while a good step aside slips it. Past that spot they cross and spread again. Each ball carries 40% of a single shot, so all five on one target is twice a single ball. From 7 blocks or more, half his shots are the volley.

- `quinque/DawnVolley` (pure, tested in `QuinqueTests`): the path every ball follows. The server flies and hits along it (`CrystalShots.volley`), and every client draws it from the same numbers, so nothing more is sent per tick. The ring is flattened up and down so the lower balls don't hit the ground from chest height.
- New FX kind `S2CUkakuFx.DAWN_RING` (46): each ball's start, its way, the aimed-at distance, and its place in the ring. Protocol 79.
- Preview of the flight paths (from behind him, and from the side): `docs/handoff/dawn-volley-2.0.48.png` (new `tools/VolleyPreview.java`). Players' Dawn still fires a single ball.

# State after 2.0.47

Dawn, the White Reaper's lance, is bigger and smoother. It's drawn about 20% bigger in the hand (scale 0.74 to 0.9). Its spike is half as thick again all along and meets the cone guard without a step; before, it read as a needle beside the guard. The spike and grip are 16-sided (were 8) in finer steps. Same silhouette as before: a black lance with a flared, gold-banded guard, a gun-sword style grip with its hooked knuckle guard, and a spiked pommel.

- `tools/quinque_models.py`: `DAWN_SPIKE` and `DAWN_TAPER` thickened, a new `rod16`, finer spike steps, Dawn's hand scale 0.9 and first-person scale 0.66. The generator re-solved the hold, so `QuinqueHolds` DAWN is regenerated. Only Dawn's frames changed (595 elements, was 487). The open firing frame still parts about its light.
- The lightning now leaves from the bigger lance's point: a new `Quinque.DAWN_TIP` (2.1 blocks from the hand), used by both the player's shot and the Reaper's (they had 1.9 and 1.72).
- Previews: `docs/handoff/dawn-bigger-2.0.47.png` (model, before and after) and `docs/handoff/dawn-aim-2.0.47.png` (in hand while aiming, three views). Protocol stays 78.

# State after 2.0.46

Players who pick male on the gender screen now have the White Reaper's build. The upper torso is drawn a little bigger over their own: broader across the shoulders and chest, the chest standing out in front of the belly, tapering into the normal waist, in their own skin and jacket layer. The arms hang slightly further out to clear it. Players who haven't picked a gender look as before, and female players keep their figure.

- `BuildLayer` (new) draws `ReaperBuild`'s chest on a man's torso as it is posed (upper-body bend carried, as the female bust is). The body layer sets the arms 0.75 px out (`KaguneBodyAnimation`, as it spreads a woman's legs; PlayerAnimator resets pivots every frame). New `Genders.male`.
- Not drawn under a chestplate, nor while something fitted to the usual torso is grown over it: Ukaku's kakuja armour, Graft's armour, the King's forms or the turning into one. The jacket layer follows the skin's jacket setting, and is also drawn when 3D Skin Layers replaces the flat jacket.
- The Rinkaku emergence mark lies on a man's broader back instead of under it: cut into bands that follow the build's back (`ReaperBuild.back`, tested).
- Previews: `docs/handoff/male-build-2.0.46.png` (before and after). `docs/handoff/kagune-showcase-2.0.46.png` shows the current Rinkaku kagune on a body, drawn with the game's own tail mesh, markings, veins, mark and colours at the game's real tail thickness (new `tools/KaguneShowcase.java`). Protocol stays 78.

# State after 2.0.45

The White Reaper has a heavier build. His upper torso is a little bigger than the part that meets his legs: broader across the shoulders and chest, with the chest standing about a pixel out in front of his belly and a touch fuller at the back, tapering into his normal waist (a V-shape). No spheres or sculpted muscle: it's his own torso drawn again a little larger, in his own skin and coat layer. His arms hang slightly further out to clear it.

- `ReaperBuild` (pure, tested in `AgentTests`): the chest's outline down the torso, each face painted from the torso's own texels (and the jacket layer's), every face turned outward. Drawn by a new layer on the investigator renderer, for the Reaper only, carried by his torso as it is posed (leans, flips, his idle). His arms are set 0.75 px out while he is drawn, then put back, since the model is shared with other investigators. Dawn is one-handed, so its grip is unaffected.
- Previewed off-game in his skin through the game's transforms (`tools/ReaperPreview.java ... build`): before and after, front, sides (with the near arm hidden to show the profile), three-quarters, above: `docs/handoff/reaper-build-2.0.45.png`. Protocol stays 78.

# State after 2.0.44

Fix for 2.0.43: the White Reaper's free hand now actually goes into his pocket in game. Investigators are drawn on vanilla's player model, which doesn't bend elbows or knees, but 2.0.43's pocket arm relied on a bent elbow, so in game it would have hung off at an angle. The arm is now solved for a straight arm: angled in to his hip, the hand sunk into the pocket at its front.

- `AgentPose.reaperIdle`: the pocket arm re-solved through the game's transforms with no elbow; the unused knee bend is dropped. `tools/ReaperPreview.java` now renders as an investigator is drawn (no bends), and `docs/handoff/reaper-idle-2.0.43.png` is re-rendered that way. Protocol stays 78.

# State after 2.0.43

The White Reaper has his own idle when he stands around with his case shut, in place of the regular investigators' one. He leans well back from the hips with his hip cocked and his weight sunk on the leg that holds the case. His other leg is loose, out in front and turned out. His chest is turned open, his head cocked with his chin a touch up so he looks down at you, and his free hand is in his coat pocket. It's a pose: only a slow breath, and no glancing about.

- `AgentPose.reaperIdle` (tested in `AgentTests`), used by `AgentClient` for the Reaper's look. The pocket arm was solved through the game's own transforms (`tools/ArmSolve`'s method).
- Previewed off-game in his skin with the new `tools/ReaperPreview.java`, next to the regular idle: `docs/handoff/reaper-idle-2.0.43.png`. Protocol stays 78.

# State after 2.0.42

Rinkaku has a new move, Blocking, on the right click: hold it with the tails out and they swing up off your back, round your shoulders and cross in front of you (an X per pair, one pair above the next), while you raise your forearms over your face in a ready stance. Every blow from in front or the sides is stopped completely. Too many hits break the guard: the tails are thrown back, you are knocked back and the block goes on a 5 s cooldown. A perfect block (the blow lands within 3 ticks, 0.15 s, of pressing) causes an impact, gives you no knockback and staggers the attacker for 1.2 s: they are stunned and pushed off, and can't hurt anything, so you get a free hit.

- Blocks: any damage with something behind it (a body, a shot, a blast) from up to 115 degrees either side of where you look. A fall, fire, hunger or the void still hurt, and so do hits from behind. A blocked hit pushes you back a little, and you walk slowly while the guard is up.
- Breaking: each blocked hit adds weight (1 for a light hit, up to 3 for a crushing one; a flurry counts once every 5 ticks). Six light hits break the guard. The weight drains once you go 1.5 s without being hit, and a perfect block takes 1 off.
- Perfect block: only a fresh press opens the window, and only if your last window opened at least 0.8 s earlier, so mashing the button gives nothing. The stagger reaches attackers within 7 blocks; a far-off archer's arrow is simply stopped.
- Attacking (left click) drops the block for your free hit. To raise it again, let go and press again. Move keys and grabs also drop it. It can't be raised in the first kakuja (its legs fight on their own) or the final kakuja (whose right click is still its dash). Vanilla's right click still comes first (doors, chests, blocks, items), crouching gives the normal use, and Instinct strikes don't fire while you block.
- How it looks: the tails swing in within 3 ticks. A hit presses them back with a shake, a perfect block snaps them out, a break throws them up behind you with your arms flung open. In first person the guard sits across the lower part of your view so it doesn't blind you. Other players see it all.
- Ability guide: a "Block (hold) · on time: stagger" row. Action-bar messages when the guard breaks, or when you press during the cooldown.
- Pure rules in `RinkakuBlock` (window, weight, cover, the tails' guard path; tested) and `RinkakuBlockPose` (body). Previewed off-game with the new `tools/BlockPreview.java`: `docs/handoff/rinkaku-block-2.0.42.png` (body alone; 1 to 4 tails; hit, perfect block, break; first person). New allowlist entry: `LivingAttackEvent.getAmount` ([forge]). Protocol 78 (`S2CRinkakuBlock`, `C2SUkakuAction.BLOCK`).

# State after 2.0.41

Rinkaku tails now have a living material at the level of Ukaku's wings: glowing veins running through the flesh with a pulse of RC flowing from the body to the point, a glossy rim toward the silhouette, a wet glint where the light catches them, a slightly clear skin seen edge-on (the veins on the far side show faintly through), a hotter, glowing root where they leave the back and darker points. All in the player's kagune colour.

- Shading is worked out per vertex from the camera, as Ukaku's fire is. The veins (four per tail, each with a fork) wander along the surface and are drawn at full brightness, so they glow in the dark. A bright band travels along them from root to point.
- The chevrons and stripe fade where the tail is seen edge-on instead of shimmering at its silhouette.
- Colours: every part follows the kagune colour (blue tails glow blue). A black kagune's veins glow blood red, as its wings' glow does.
- Pure rules in `RinkakuRibbon` (`surface`, `markAlpha`, `veins`; tested). Previewed off-game with the upgraded `tools/RibbonPreview.java` (now shading by an eye, blending the clear skin, drawing the veins unlit): `docs/handoff/rinkaku-material-2.0.41.png` (six colours) and `rinkaku-material-zoom-2.0.41.png`. Protocol stays 77.

# State after 2.0.40

While a Rinkaku's tails are out, the skin of the lower back shows where they tear out, as in the owner's reference: a glowing opening with a torn rim and red veins branching out from it across the back, in the kagune's own colour.

- A layer on the player's torso (as the kakugan is on the head): it follows every pose, sits over the skin and its second layer, comes up as the kagune comes out and goes as it goes back. Its heart and the veins' cores glow in the dark, breathing slightly.
- Colours follow the kagune (the reference red turned to it), like the tails. Tiles generated by `tools/rinkaku_mark.py` (`textures/misc/rinkaku_mark.png`, `rinkaku_mark_glow.png`), previewed off-game on three skins in two colours (`docs/handoff/rinkaku-mark-2.0.40.png`). Protocol stays 77.

# State after 2.0.39

Rinkaku tails are drawn as the owner's reference picture has them: broad, flat ribbons that swell a little past the back and taper to a point, bright at the middle of each face and deep at the edges with darker smudges along them, a light stripe running along each face near one edge, and thin chevrons all over them, black and light, pointing back toward the body. Every colour follows the player's own kagune colour.

- Colours: the picture's crimson palette is turned to the tail's kagune colour (`KaguneTint`, as the wings and shards are), so a blue kagune gets a blue ribbon with pale blue stripe and marks, and the original crimson matches the picture exactly. A black kagune is black with smoke-grey stripe and marks. The chevrons' black stays black.
- How: the ribbon is drawn into the original's own buffer (per-vertex colours), and the markings (a generated tile, `tools/rinkaku_scales.py` -> `textures/misc/rinkaku_scales.png`) a hair outside it after every tail is drawn. The original's tube is only used if this ever fails. Ghoul children's Rinkaku tentacles get the same look in their own colour. Centipede legs are unchanged.
- Pure shape and colours in `RinkakuRibbon` (tested: the crimson palette exact, blue stays blue, black is black with visible marks, edges deeper than the middle, the taper to a point, no NaN). Previewed off-game with a software renderer over the real mesh and texture in six colours (`tools/RibbonPreview.java`, `docs/handoff/rinkaku-ribbon-2.0.39.png`); the test tails there are short, synthetic paths. Protocol stays 77.

# State after 2.0.38

Rinkaku tails now grow out of the body: each tail's root is sunk inside the lower back (its start hidden in the body) and its first 0.4 blocks leave the back as a rigid base, fanned out to its own side, before it blends into the motion it always had. Before, every tail started at a point hanging just behind the hips and fell from there like a rope.

- Tails leave backwards and round to their own side, the first pair rising a little, the next level, the next falling, each pair a little further round. Past about 0.7 blocks the tail is wholly its own motion again, so strikes, grabs and the idle sway are unchanged.
- The roots still ride the torso as it is drawn (a lean, a lunge, a crouch, a flip), as since 1.0.24. Centipede legs are unchanged (their own renderer).
- Pure geometry in `RinkakuRoot` (tested: root inside the lower back, the base straight out of it, the tip untouched, no NaN). Plotted before and after on a body silhouette: `docs/handoff/rinkaku-roots-2.0.38.png`. Protocol stays 77.

# State after 2.0.37

Rinkaku's C move, the tendril guard, is gone: C does nothing on the Rinkaku path any more and the guard is no longer listed in the ability guide. Rinkaku's defence becomes its right click (the block, in a coming version).

- The original's `toggleGuardLegacy` was the only thing that ever raised the guard; it is never called now, and a guard somehow still up is let down when C is pressed. C still works for the paths that use it (Ukaku's final kakuja Steel Blade, Graft's own C). Protocol stays 77.

# State after 2.0.36

Villagers have four new tempers, for twelve in all: Playful (teasing, games and pranks), Stoic (calm, few words, steady), Sarcastic (dry wit), and Romantic (a hopeless romantic, flowery and sincere). Each has its own lines for everything a villager says and its own body language.

- Every line set has them: greetings (stranger, warm, close, partner), small talk, about themselves, compliments, goodbyes, a ghoul's confession (taken, doubted, refused), saying yes, "I love you", kisses, hugs, walks hand in hand, letting go, the village's edge, "What's your type?", and the kind "not my type". They also get their own trust for a confession and their own warmth to compliments.
- Body language: the playful and romantic move brightly (Eto's way), the stoic steadily (Ken's), the sarcastic reserved (Touka's), with their own hello, chat, compliment and idle gestures.
- Nobody already in your world changes: villagers placed before keep the temper their seed always gave them. Villagers placed from now on (new villages) roll any of the twelve, saved with them (`HexHumanTemper`). The enum is appended to (saved by ordinal).
- Tested by the existing per-temper checks, which now cover all twelve: every line filled, no banned names. Also: villagers placed before only ever have the first eight, and new ones can have all twelve. Protocol stays 77.

# State after 2.0.35

Every villager, man or woman, now has their own idea of who they fall for and what they look for in a partner. You can romance male and female villagers alike, as long as you're who they fall for; ask "What's your type?" to find out.

- Who they fall for, their own for life (from their seed): most the other sex (62%), a fair share both (22%), the rest their own (16%). Men and women alike.
- What they look for, also their own: strong, gentle, funny, mysterious, clever, a little dangerous, loyal, or caring. Those who like mystery or danger take a ghoul's confession more readily (+15%); loyal or caring ones a little (+5%).
- "What's your type?" (once they're warm to you, regard 20): they tell you who they fall for and what they look for, in their temper's voice.
- Asking out someone who doesn't fall for your gender (your choice on the gender screen) gets a kind, in-character no ("I only fall for women. Still my favourite person to talk to!"). You stay friends and the ask isn't offered again. A player who never chose a gender can date anyone, and couples already together stay together.
- Pure rules and lines in `village/HumanPersona` (`Attraction`, `Taste`, `type`, `notMyType`), tested: the shares for men and women over 20,000 seeds, who may date whom, every line for every temper, attraction and taste filled, saying who they fall for, with no banned names. Protocol stays 77.

# State after 2.0.34

Village women now have the female body the game already gives its CCG women and grown daughters: hips and thighs shaped, a wide-painted skin's arms drawn slim, the bust in her own skin (and its second layer), bouncing as she moves, drawn a little shorter and slimmer than the men.

- Who: the 12 villager skins made from daughters' skins (`HumanSkins` marks them `girl`), and the women among vanilla's default skins (Alex, Ari, Efe, Makena, Noor) for humans made before 1.3.75.
- Her proportions are the CCG women's (`HumanRules.WOMAN_*` = `AgentRules.FEMALE_*`): about 0.92 of a man's height, 7% narrower, 85% of the figure's curves.
- The villagers share one model, so each is put back to a man's legs, arms and leg pivots before a woman's are shaped: no woman's shape carries over onto the next man drawn.
- Previewed off-game with the new `tools/VillageWomenPreview.java`: every village woman before and now, front, three-quarter and side, a man beside for scale (`docs/handoff/village-women-2.0.34.png`). Protocol stays 77.

# State after 2.0.33

The White Reaper has a new move only he uses: he raises Dawn and drives it point first into the ground, the spot where his prey stood is marked on the ground for 0.8 s, then a huge black tendril bursts up out of it. Step off the mark in time and it misses.

- Timing: 9 ticks raising and driving the lance in; the mark (a ring of dark smoke and red glints at the tendril's 1.6-block radius, red welling up inside it, more urgent as it comes) for 16 ticks; the tendril; he pulls the lance out at tick 30 to 36. He is rooted the whole time the lance is in the ground: that's the opening to punish him.
- The tendril: about 5.5 blocks tall, as thick as a man where it leaves the ground, black with dark red veins. It is drawn with the original mod's own Rinkaku tube, the same one used for the children's tentacles. It bursts out in two ticks, whips, and sinks back over 1.3 s. It hits everyone he's fighting who stands on the mark: 24 raw damage, thrown up and outward.
- When: now and then (5% a tick when ready) against prey 3.5 to 18 blocks away in sight, at most every 8.5 s, never during a swing, kick or lightning aim. A stun before the lance goes in cancels it.
- Pure rules in `npc/ReaperTendril` (tested). Pose previewed off-game (`docs/handoff/reaper-ground-2.0.33.png`, `tools/QuinquePreview.java <out> dawn ground`), and the tendril's line over its life (`docs/handoff/reaper-tendril-2.0.33.png`). The tube itself can only be seen in game. Protocol 77: a new swing value and two new effect kinds go over the wire, so client and server must both be on this version.

# State after 2.0.32

A player holding Dawn fires its yellow lightning ball with right click: the point opens, the ball gathers there for a quarter of a second while the lance comes up onto your aim, then it flies and explodes on impact with the same lightning effects as the White Reaper's. Shift + right click folds Dawn back into its case.

- A player's ball: 16 at its heart (the Reaper's is 26), its blast falling off over 2.5 blocks in proportion. It travels 1.5 blocks a tick for up to 32 blocks, pushes back what it catches, and never burns or breaks blocks. One every 2 seconds; tapping sooner shows the time left on the action bar.
- The point opens (2.0.30's `dawn_fire` frame, its two halves parted about a yellow light) while the ball gathers and for 6 ticks after it leaves, for players and for the White Reaper as he aims. Every client sees it: the times ride on the weapon stack.
- Getting Dawn: the White Reaper now leaves his black case when a player kills him, and Dawn is in the creative Combat tab.
- No new Minecraft API: the cooldown is kept on the server, not vanilla's item cooldown. Previewed off-game: `docs/handoff/dawn-aim-2.0.32.png` (`tools/QuinquePreview.java <out> dawn aim`). Protocol stays 76.

# State after 2.0.31

Dawn has its own three M1s, made for a three-block lance held in one hand at the hip: a wide level sweep, a rising backhand, and a lunging thrust as the finisher. Before, it borrowed the katana's two-handed chops, and every swing ended with its point in the ground. The White Reaper and any player holding Dawn swing it the same way.

- First: drawn out to the right and swept level round the front to the left at chest height. It also takes the others in front within 3 blocks for 60% of the blow (a player's sweep never hits other players; the Reaper's only hits what he is fighting).
- Second: from low on the left, rising across the front to high on the right.
- Finisher: drawn back and sunk, then driven out its whole length with a long lunge at shoulder height. It hits the others in its line up to 4.5 blocks for 80% and knocks its target back. The Reaper steps in hard on it, as the spear does.
- Dark ribbons: as each swing passes through, dark smoke edged with a faint dark-red glow is laid along the way its point goes, fading in about a third of a second (Photon particles, client only).
- Previewed off-game with `tools/QuinquePreview.java` (now including Dawn): `docs/handoff/dawn-m1s-2.0.31.png`. Protocol stays 76.

# State after 2.0.30

Dawn is round where it was square: its guard is a 16-sided cone in fine steps (it was an 8-sided one in 12 big steps), and its spike, grip and butt are 8-sided along smooth profiles, with the spike's two ridges turning about it along its thicker middle.

- Same length, grip and holds as before (the hold solver's output is unchanged), same black and gold. 487 boxes (171 before).
- New frame `dawn_fire`: the point split in two and parted around a yellow light, for when a lightning ball leaves it. Not shown yet: the next versions choose it as a shot is fired. 22.5 degrees each way is the smallest turn an item model allows, so only the last 6 pixels of the point part.
- Previewed off-game with the new `tools/model_preview.py` (draws a model JSON from any side, with zoom); before and after: `docs/handoff/dawn-smooth-2.0.30.png`. Protocol stays 76.

# State after 2.0.29

`/kagune reaper` (operators) puts the White Reaper five blocks ahead of you for testing; in the wild he now turns up about once in a hundred in-game days instead of about once in 660.

- Never two: a Reaper already loaded anywhere is sent away first, and the command takes over his slot. One standing in unloaded ground gives the slot up; when that ground loads, he wakes as an ordinary Rank 9 investigator with a regular quinque.
- Natural spawns: each appearance of investigators (about one a day near each player) has a 1% chance to be the White Reaper alone (`AgentRules.REAPER_CHANCE`). Before, he only came from Rank 10's 0.15 weight in the rank table. He stays rarer than a Rank 9. Protocol stays 76.

# State after 2.0.28

The White Reaper always wears the owner's own skin, on the slim arms it is painted for; no other investigator can roll it.

- The owner's file is kept unchanged as `art/white_reaper_skin.png` and shipped byte for byte as `textures/entity/ccg/ccg_reaper.png` (the tenth look, appended: `AgentRules.REAPER_LOOK`).
- Ordinary investigators roll from the first nine looks only (`AgentRules.regularLook`); a Reaper saved before this version changes into his skin when he next loads. Protocol stays 76.

# State after 2.0.27

Wild ghouls' kagune no longer depends on their body's draw call being noticed: Rinkaku tails and Ukaku wings are drawn for every living wild ghoul with its kagune out within 48 blocks, and one ghoul failing to draw never hides the others.

- The owner's report: wild ghouls (likely Rinkaku) used their moves but their kagune never showed.
- Found by reading and an out-of-game harness: the server sends the kagune out correctly, and the tails' geometry builds cleanly for every tier and every lash (600 frames with flips and the grow-out, no throws, no NaN, full alpha). The one fragile link was that a ghoul's kagune was only drawn if its body's render layer had noted it that frame, and one failure aborted every ghoul's tails, the children's tentacles and every Graft player after it for the whole frame.
- Now: a ghoul is noted at the start of its body's draw; the tails' and wings' passes also take every living ghoul within 48 blocks whose kagune is out; each ghoul and each Graft player is drawn on its own, a failure logged ("HexKagune's a wild ghoul's tails layer failed ...") and skipped.
- If the tails are still invisible on your device, the log line above names the cause: please send it. Protocol stays 76.

# State after 2.0.26

The White Reaper drops raw Human Meat on death using the existing human loot rule.

- Drops one to three pieces, plus one per Looting level.
- Dawn and its black briefcase are cleared from the Reaper’s hands before vanilla rolls equipment drops. Protocol remains 76.

# State after 2.0.25

The White Reaper now points Dawn at distant targets and fires an explosive yellow lightning ball.

- The shot has a 12-tick aim, travels 1.5 blocks per tick, and can fly up to 32 blocks. It detonates on a body or solid surface.
- Direct hits deal 26 raw damage. The blast falls off to 10.4 at 2.5 blocks, pushes exposed targets away, and leaves terrain intact. No fire.
- Protocol 76 carries the aim and lightning effects. Dawn remains encounter-only pending a player acquisition decision.
- Targeted Java compilation and the Agent and Quinque suites passed. CI builds and publishes the release jar.

# State after 2.0.24

A pair loaded from an older save can include a former Rank 10. Its superior now caps correctly at Rank 9 and keeps the superior’s health and damage bonus. Protocol 75 is unchanged.

# State after 2.0.23

The White Reaper can mix a telegraphed front kick into Dawn’s melee strings.

- The kick has a six-tick windup, deals 22 raw damage, and launches and tumbles the target. The target can step out of range or break line of sight.
- Dawn’s NPC damage uses its stated values before the target’s defenses. Protocol 75 carries the kick pose.

# State after 2.0.22

The White Reaper now carries Dawn, a black lance swung as a sword.

- Dawn has a tapered blade, a hollow flared guard with two engraved gold bands, and a hooked grip. It folds into a black briefcase. The model and texture were previewed outside the game.
- Dawn has a three-block melee reach. Its ordinary hits deal 23.7 raw damage and its finisher deals 32.0.
- Dawn is reserved for the White Reaper encounter pending a player acquisition decision. Protocol 74 carries the new weapon ordinal.

# State after 2.0.21

Rank 10 is now the White Reaper, a solo encounter with a single world-wide slot.

- A rare surface investigator roll can spawn him without a partner. Regular pairs and village patrols cap at Rank 9. The slot survives unloading and is freed on death.
- He has 280 HP, 18 armor, a 35% incoming damage cut, 0.60 movement speed, 0.85 knockback resistance, and up to eight zigzag dashes.
- He uses the existing katana moves until Dawn ships. No Reaper case drops in this version. Protocol stays at 73.

# State after 2.0.20: read `docs/handoff/next-ai-2.0.9.md` first, then `next-ai-2.0.5.md`, then `next-ai-2.0.1.md`

- **2.0.20** (the owner: "Make sure that no matter the RANK of the investigator his superior is still stronger"). Before, each agent of a
  pair rolled its own rank (`Rank.of(leader, ...)` per agent, separate draws), so a partner could out-rank its leader. Pure
  `AgentRules.superior(own, partner)` / `subordinate(own, leader)`, `SUPERIOR_HEALTH` 1.25, `SUPERIOR_POWER` 1.15, `health(rank,
  superior)`, `power(rank, superior)`. `AgentBrain.wake`: with the partner loaded (`level.m_8791_`), its rank (mind, else saved
  `RANK_LEVEL`) sets this one's; `Mind.superior` = leader with a partner; health and every damage use the superior's edge. Tests in
  `AgentTests.ranks` (every pair of ranks, either waking first). Harness only.

# State after 2.0.19

- **2.0.19** (the owner's crash on 2.0.18 while loading, Forge 47.4.10 on Android/Amethyst: `IllegalAccessError: QuinqueClient tried to
  access private method ItemProperties.m_174570_`). The call is unchanged since 1.3.25 (Forge's access transformer normally makes it
  public; that launch did not). `QuinqueClient.register` now gets it with `ObfuscationReflectionHelper.findMethod` (accessible either way)
  and `invoke`s it, everything caught into `Safety.caught`. The allowlist's `ItemProperties m_174570_` line is gone (no longer referenced;
  the class lines stay). Lesson: the verifier checks names and descriptors, not access; a vanilla-private member used directly only works
  where an access transformer applies. Not run.

# State after 2.0.18

- **2.0.18** (the owner: "fastly increase the dash amount/speed also with mastery"; asked, they meant the CCG agents' dash: "CCG agents
  dash more than once in zigzags"). `AgentRules.dashes(rank)` (1, 1, 2, 2, 2, 3, 3, 3, 4, 5, 6), `dashSpeed(rank)` (DASH_SPEED x (1 +
  0.06 level + 0.15 per rank from 8)), `dashEvery(rank)` (x pace), `dashOften(rank)` (1 + 0.15 level, on SIDE_DASH and the close-in
  0.06), `zig(way, axis, in)` (mirrored across the line to the prey, leaning in by 0.6 when out of reach). `AgentBrain.Mind`: `zigzag`,
  `zigAxis`, `zigIn`; `dash()` starts a zigzag (axis to `mind.prey`, else `target`); `feet()` chains the next the tick the last ends
  (`DASH_TICKS`), its side picked for the animation, ended by `clear()` failing. Tests in `AgentTests.ranks` (harness only).

# State after 2.0.17

- **2.0.17** (the owner: "buff CCG agents depending on the rank ... add more ranks RANK 0 to 10 (10,9,8 being rare but really
  dangerous, with a decent amount of HP.)"). `AgentRules.Rank` is now RANK_0..RANK_10 (weight, health, speed, armor, power, pace; title
  "Rank N Investigator", red from `DANGEROUS` 8); `fromOld(ordinal)` maps 1.3.70's six. `AgentBrain`: saved by number under `RANK_LEVEL`
  (the old `RANK` read once through `fromOld`); armour (`Attributes.f_22284_`, already referenced by `CcgAgent`) set by rank; damage x
  `rank.power` (blows, both clash swings, launcher shards via `agent.mind`); the rest between blows x `rank.pace`. `AgentTests.ranks`
  rewritten (harness only).

# State after 2.0.16

- **2.0.16** (the owner: "make sure the trail of the projectiles (ALL OF THEM) match the kagune color a lot don't"; "also ... the voltex
  move (the one with a horizontal tornado)"). `client/fx/UkakuFx`: a static `tint` (the shooter's `KaguneTints.of`) and `c(argb)` =
  `KaguneTint.recolour(argb, tint)` round all 22 `.colors(...)`; set in `accept` (the message's shooter, or `shooter(id)` for an impact;
  the old body is `acceptTinted`) and per bolt/vortex/lasting in the tick (`tickTinted`), reset in finally. The vortex's mesh was already
  tinted (`UkakuWingRenderer`, 1.3.35). `KingMarks.Flight` carries a colour (`spike(..., colour)`; `KingClient` passes the owner's). Owl
  fx (blood, bone, dust, white streaks) left as they are (not kagune-coloured fire). Not run.

# State after 2.0.15

- **2.0.15** (the owner: "Ukaku's projectiles should not burn targets anymore only knockback + damage"). `UkakuFire.burn(..., 0, ...)` now
  means no fire, a STAB wound, no burn sound; new `UkakuFire.shot(...)` (no fire, + a push away from a burst's centre) for bolt hits
  (`strike`), shard explosions and needle-burst blasts. `UkakuCrystalCombat`: a spike's `burnSeconds` and a fire burst's
  `PYRE_BURN_SECONDS` no longer set alight. `ChildKagune.burn` and `GhoulNpcCombat.burn` (wild ghouls, `NpcRules.SHOT_BURN` now unused):
  STAB wound, no fire. Kept burning (not projectiles): the flame dash art, the pillar, Ember Hop (`scorch`), embers, the vortex. Not run.

# State after 2.0.14

- **2.0.14** (the owner: "seeds, farming materials, food etc"): `VillageShop.FARMING` (12 offers: seeds, bone meal, oak sapling, sugar cane,
  hay, stone/iron hoe, bucket, composter); `stock` = staple + 2 `FOODS` + 2 `FARMING` + 2 `BASICS` (seven); `HumanTalk.SELL` moved to SAY_8
  (wares on SAY_1..SAY_7). The draw order changed, so existing villagers' wares change once. `NpcTests.village` updated; harness only.

# State after 2.0.13

- **2.0.13** (the owner couldn't find trading with villagers: there was none, only villager-to-villager street trades (1.3.72); then: "they
  mostly trade basic stuff like villagers, but a lot more food, the ones needed for our recipes on touka (not meat)"). Pure
  `village/VillageShop`: `Offer(item, name, count, price)`, `STAPLES`, `FOODS` (the cookbook's vanilla non-meat ingredients; cod left out),
  `BASICS`, `BUYS`; `stock(seed)` (a staple + 3 foods + 2 basics, from the villager's uuid seed), `buys(item)`, `lots`, `sellLabel`, lines.
  `HumanTalk`: main page `TRADE` (= `Choice.FAVOUR`, label "Let's trade."); `trade(...)` page: SAY_1..SAY_6 buy, `SELL` (= SAY_7) sells
  the held stack's whole lots, BACK to `show`, LEAVE; `Talk` carries `deals`/`selling`. Paid from the main hand's emeralds only (no
  inventory API); creative pays nothing. `NpcTests.village` extended (every non-meat recipe ingredient sold somewhere); harness only.

# State after 2.0.12

- **2.0.12** (the owner: "I dont think clashes are shaking them screen at all", then "increase it further"). By reading: the CLASH mark's
  `kick` was `PaleCamera`'s 1.6-degree pitch jolt (e^-0.45t, ~5 ticks); the rumble only from `QUAKES`. New `PaleCamera.clash(strength,
  ticks, time)` (its own state; pitch 4.0, roll 3.0, yaw 1.8 degrees x strength x uneven sines, fading ^1.6; under `cameraEffects` /
  `cameraEffectStrength`), called by `KingMarks` CLASH with `ClashRules.shake(p)` (0.7 + 0.55p, up to `SHAKE_MOST` 2.6; p is the drawn
  `shown` power, 1.68 for an ordinary clash -> 1.62), `shakeTicks` 14 + 6p, `shakeReach` 20 + 5p, linear falloff. No new API
  (`ComputeCameraAngles.setYaw` was allowlisted). `ClashTests` extended (harness only).

# State after 2.0.11

- **2.0.11** (the owner: "Increase the volume of the clash sounds, the stronger the users the louder and even bigger the impact ... the
  weakest player/entity gets pushed further back, if the difference is too vast ... pushed far back a ton, stunned and ragdolled"; then:
  "its pretty much the same effect entirely of the kick without the animation, send them through blocks"). `ClashRules`: `volume` 1.3 +
  0.55p, `layers(p)` (1, 2 from 1.5, 3 from 2.2: `KaguneClash.burst` plays sfx.clash that many times), `shown(p)` (p x 1.4, the
  S2CKingFx CLASH size: the client's puffs/sparks/kick and its rumble threshold read it), `ratio(own, other)`, `push(p, ratio)` (x
  ratio^0.8 in 0.35..2.2), `GAP` 2.5, `overwhelmed`. `KaguneClash.meets` passes each side's ratio and the other's hit; `apart(...)`:
  overwhelmed -> the Speed Kick's speed/rise (`PaleRules.KICK_*`, knockback resistance), `BlockSmash.kick(player, ...)` when the stronger
  is a player (an NPC: velocity only, no smashing), `PaleKnockdown.launched(..., KICK_DOWN_TICKS, true)`, `S2CPaleFx.KICKED` (the tumble).
  `MoveClash.collide` keeps the even push. `ClashTests` extended; only those checks were run (harness).

# State after 2.0.10

- **2.0.10**: CI's `SfxTests` failed 2.0.8's (and so 2.0.9's) release: every vanilla sound name in the code must be remapped to the user's
  pack (`Sfx.remapped`) or listed in `SfxTests.KEPT_VANILLA`. The three stove sounds the owner approved are now listed there. No mod change.

# State after 2.0.9

- **2.0.9** (the owner: "spouse will sort items from chests all of them in the house perimeter, separating items in different chests from
  NBT data, food in one chest, blocks in another, etc. If it doesnt know what to do with them it puts them in a "Whatever" chest"; asked,
  they chose that she labels them). Pure `family/ChestSort`: `Kind` (FOOD, BLOCKS, TOOLS, ARMOR, ORES, PLANTS, DROPS, WHATEVER, each with
  its label), `kind(id, food, block)` (by the id's path words; food first), `named(name)`, `box(name)` (a kind's name / the player's own
  other name: `theirs` / free), `assign(boxes, counts)` (named kinds kept; free chests to the wanted kinds by count; a Whatever chest kept,
  the last free one), `target`, `nameOf`/`saved` (CustomName JSON), `half(state)`. Server `ChestSorter.organize(level, chests, at)` from
  `HomeChores.effect` TIDY (before the in-place `sort`): units (double chests paired by `type=`/`facing=` in the state's string), counts,
  names written through `BlockEntity.m_187480_` + `m_142466_` (CustomName; no new API), each stack in `at` that belongs elsewhere moved
  whole with `HomeChores.insert`. Block items by the item's class chain's simple name "BlockItem" (reflection: no game API). Only the
  ChestSort checks were run (harness).

# State after 2.0.8

- **2.0.8** (the owner: "longer cooking animations over the furnace, with sizzling sounds, while an NPC is doing a task make sure it doesnt
  cancel it out to do another. Cooking process should envolve grabbing items needed from a chest, going to the furnace and start cooking
  over it. dont allow them to rotate while cooking"; asked about the sound, they chose vanilla's crackle: the one exception to the
  hexkagune-only rule). `Chores.COOK_LOOPS` 3, `stepTicks(gesture)`, `crackle(block)` (furnace/smoker/campfire; null at a table). In
  `HomeChores.tick` a step ends only by time (`stepTicks`); a changed or finished gesture is set back (`EtoBrain.gesture`), so the COOK
  stir loops; the stove sounds every 40 ticks (`SoundEvent.m_262824_(ResourceLocation.m_214293_("minecraft", ...))`, `SoundSource.PLAYERS`,
  all already proven). `EtoBrain`: a chore under way ticks before `ParentDuty`/`together`/friend scenes/`ParentCare`; `ParentDuty` leaves
  a non-RAMPAGE duty while a chore runs. `EtoNpc`: `LookAtPlayerGoal`/`RandomLookAroundGoal` wait while `EtoBrain.busy`. The chest ->
  stove round was already there (1.3.31); ingredients are not shown in her hands. Only the Chores checks were run (harness).

# State after 2.0.7

- **2.0.7** (the owner: "NPCS should not be teleported home unless they die or bug inside a wall ... go outside the max radius, just with
  the feeling that by night time they have to be home, this includes both kids and spouse"). Pure `HomeRules.roam(stage)` (null = spouse;
  `ROAM` 24 for spouse/teen/grown, 12 child, 0 toddler/baby: within `Parenting.strays` by day, tested), `leash(radius, night, roam)`,
  `WALLED` 6, `NO_HEADWAY` 2400, `noHeadway(...)`. `EtoBrain`: the LOST teleport is gone; at a shared home `m_21446_` uses the day/night
  leash (her chase still widens it); `ChildBrain`: the same by its age (`HomeLife.night(..., true)`), its LOST `goHome` gone. Day-long
  "no company" returns now call `sendHome()` (new on `ChildNpc`, already on `EtoNpc`): home set, no teleport ("set off home to"). New
  `server/Homeward.bugged(mob, level, home, away, now)` (every 10 ticks; WeakHashMap state): body box (deflated 0.15) colliding
  (`m_45756_`) for 6 looks, not noPhysics/asleep/carried, or `away` (night or past the leash + 4, idle) with no 2-block headway in 2400
  ticks and no player within 32 -> `Homeward.put`. Following teleports (to the leader) and hand-holding are unchanged. Walking home is
  vanilla's `MoveTowardsRestrictionGoal` (both have it at priority 4). Only the HomeRules checks were run (harness).

# State after 2.0.6

- **2.0.6** (the owner: "Double check that the kids have a chance to enherit both kagunes ... only have seen the ukaku on kids"; they were
  on the Ukaku path with Touka: wings x wings = wings, no bug; asked, they chose 40/40/20). `KaguneGenes.inherit(mother, father, roll)`:
  two different kinds, roll < `ONE_KIND` 0.4 the mother's alone, < 0.8 the father's, else both (the old two-argument `inherit`); same kind
  or a parent with none, as before. `colourOf(...)`: one kind alone, that parent's colour. `Genome.of` draws the roll last (after the
  colour's, so nothing earlier changes); `Children.genome` (UNKNOWN children) rolls from `Random(seed ^ 0x6B62)`. Drawing checked by
  reading: `GraftRenderer` (tails bits) and `UkakuWingRenderer` (`wingCount`) gate independently on the genome; `S2CGhoulNpc`'s one kind
  only feeds the shared fight view. `FamilyTests.kagune`: shares over 2000 seeds, each pairing. Only the genes block was run (harness).

# State after 2.0.5

- **2.0.5** (the owner: "Interaction between other players with another player's sons/daughters (NOT DATEABLE)"; mid-session: "make sure
  chat is accurate to the child's age, dont make a toddler/baby be able to say anything coherent"; protocol 73, unchanged). Pure
  `family/ChildPage`: every child talk page's choices (parent main page `parent(stage, Parent(carrying, ride, following, food, bedtime,
  away))`, `play`, `asleep(parent, free)`, moved out of `ChildTalk` unchanged) and the new `stranger(stage, food)`: keys `x:chat`, `x:wave`,
  `x:five` (HIGH_FIVE stages), `x:treat` (food in hand, not a baby), `x:parents`. `PARENT_ONLY` (+ "m:", "s:", "t:") is refused in
  `ChildTalk.heard` for a non-parent whatever the page held. `ChildPage.choices(n)` builds every page's choice ordinals (SAYS + LEAVE).
  NOT DATEABLE guards: `ROMANCE_KEYS`, `ROMANCE_CHOICES` (the talk's ASK_OUT, KISS, HUG, PROPOSE, NIGHT...), `romantic(text)` (phrases;
  a parent's "Kiss it better" is not one). Pure `family/StrangerLines` (by stage and `ChildTemper`): `hello`, `chat` (its name, mom and
  dad by name), `wave`, `five` (`shyOfHands`: a shy child), `moment` (said by `ChildBrain.begin` when the partner is not a parent),
  `treat` (little ones refuse; teen/grown take one politely, no FEED moment), `parents(near, mother, who)` (+ `compass(dx, dz)` for the
  asker's action bar; a player parent gets a chat line), `scared(stage, temper, Display)`, `hiding` (used by `ChildBrain.cower` when the one
  it hides from is not its parent). Babies: actions only; toddlers: two spoken words at most outside *actions* (tested by counting).
  `Sightings.children`: a stranger who shows more near (12 blocks or in sight) gets `stranger(...)`: a line, and `ChildBrain.dread(..., ticks)`
  (new overload; 120 for a kagune to a little one, 200 for a kakuja or worse to anyone; `dreadUntil` only ever extended). Test
  `FamilyTests.strangers` (every stage x 32 page states x 4 rides, both stranger pages, `choices(n)`, every scene of `talk/child.txt`,
  every new line x temper x 12 seeds). Only the new test block was run (in a harness against the pure classes); the server code was not
  compiled locally (no base jar here): CI is the check. Untested in game.

# State after 2.0.4 (and the preview after it)

- Preview since 2.0.4 (no mod change): `tools/GrownDaughterPreview.java`, sheet `docs/handoff/grown-daughters-2.0.3.png` (grown daughters
  beside themselves as teens, to scale; it showed them about 2% shorter grown, 0.86 against a teen's 0.88: offered to the owner, open).


- **2.0.4** (protocol 73, unchanged). (1) The owner: after a first child, "you can request another but the cutscene is not triggering
  anymore whenever near the bed at night": at a shared home `EtoBrain.toBed` takes the night instead of `calm`, so `together`'s baby
  night never ran. `EtoBrain.babyNightInBed` (from `toBed`, she asleep in the shared bed): the partner within `BED_NEAR` 3 of her, or lying
  down within `BED_LYING` 6, with the same conditions (night, not used, married, mixed, wantsBaby, not pregnant) -> `EtoServer.night`.
  (2) The owner: "the area where the village spawns should be erased" (the world's hills stood in it): `DryJigsaw` overrides
  `Structure.afterPlace` (m_214110_, per chunk): from the start piece (pieces.get(0), the middle tile: its position and rotation, ordinal
  = quarter turns clockwise, `VillageRules.unturn`) each column's template x, z; `village/VillageTops` (generated by the tool: each
  column's highest template block, `clearFrom` = max(top, GROUND) + 1) -> air from there up to WORLD_SURFACE_WG. New API allowlisted
  (afterPlace, PiecesContainer.pieces, PoolElementStructurePiece.getPosition, StructurePiece.getRotation, LevelWriter.setBlock, classes).

# State after 2.0.3

- **2.0.3** (the owner: daughters should have Touka's model "only ... when they reach adulthood"; protocol **73**): `ChildStage.ADULT`
  (from 16 days, box and draw 1.0, health 30; appended last: S2CChild sends the ordinal). `ChildStage.older()` (TEEN or ADULT) replaced
  every `== TEEN` (and `case TEEN` -> `case TEEN, ADULT`, moment masks) so a grown-up acts as a teen; own words: talk title "Grown",
  `ChildLines.grown` "is all grown up now", the grow command stops at ADULT. `ChildRenderer.woman(view)` (ADULT and female only):
  `Model.m_6973_` shapes Touka's legs (`Persona.TOUKA.legs()`) and slim arms after `little()` resets the parts each frame; drawn at
  `Persona.TOUKA.size()`; layer `Figure` draws `FemaleFigure.bust` (Touka's curves, skin and its second layer, `jiggle`). No younger
  stage ever gets the female body (the owner first asked for daughters in general: refused; this is the adult-only version).

# State after 2.0.2

- **2.0.2** (the owner: "stop limiting it to plains ... including some from biomes o plenty currently cant even find a village with locate
  structure ... same for anteikou, and cybertower and library"): the biome tags `has_structure/{village,anteiku,cyber_tower,library}` and
  `prefers/{anteiku,cyber_tower}` list open vanilla biomes (+ forests and taigas for the three buildings) and Biomes O' Plenty's as
  optional entries (`"required": false`); `VillageGround` FLAT 12, up to `WET_EDGE` 2 wet edge samples (the middle 9 dry);
  `ControlStructures.DryJigsaw` calls `inner.m_214086_` straight, the reflection only on IllegalAccessError; village spacing 40/20.
  Pushed without a local build at the owner's word: CI's build is the check.

# State after 2.0.1

- **2.0.1** (the owner's report on 2.0.0; protocol 72, unchanged): the speech-bubble crash (`Font.DisplayMode.NORMAL`) and bubbles that
  can never crash the game (`SpeechBubbles` catch + `Safety.caught`); talk screens show the skin the child or villager is drawn in
  (`S2CNpcTalk.CHILD_FACE`/`HUMAN_FACE`; the face byte had cut villagers' looks); the structure renamed `hexkagune:village`, built from
  the owner's schematic `art/village/japanesevillagev4.schem` by `tools/village_structure.py` (25 jigsaw tiles, 13 houses, 26 villagers,
  6 investigators; map `village/VillageMap`, legacy `village/BrickVillage` for old worlds); only on dry, fairly flat land
  (`ControlStructures` `hexkagune:dry_jigsaw`, `village/VillageGround`); wild investigator pairs about once a day. Details:
  `next-ai-2.0.1.md`.

# State after 2.0.0

- **2.0.0 is Release 2.0**: the version bump and the roll-up notes (`.github/release-notes-2.0.0.md`) over 1.3.68-1.3.83; no code change
  from 1.3.83. The owner tests it in game next: their reports come first, each its own version (the next is 2.0.1).

# State after 1.3.83

- The owner's last request (Release 2.0): more villagers per village with unique looks, a bug/lag pass, an animation pass, mastery/forms
  as resistance not health, then release 2.0. Done so far: 1.3.80 (the tank), 1.3.81 (villagers), 1.3.82 (bug/lag pass), 1.3.83
  (animation pass).
- **1.3.83 animation pass** (client only). New preview `tools/VillagerPreview.java` (every HumanGestures move per style, through
  `PoseRender` stiff, on a villager skin brought down to 64 px: PoseRender reads only 64-pixel skins). Found on it and on all EtoGesture
  values on Eto: the early "hand to the face" poses had the arm's Y turn outward (+ on the right arm; CHIN and LOOK, which read right, use
  -): fixed in `EtoPose` LEAN, GIGGLE (one hand at the mouth, the other across the tummy), YAWN, LICK, RING (hands over the mouth), and
  LAUGH (was a T; now hands on the belly). Shared with Eto/Touka/Ken. Sheet `docs/handoff/villager-moves-2.0.png`.
- **1.3.82 bug/lag pass** (protocol 72, unchanged). Read every tick handler's gate (67: all phase-gated, most state- or modulo-gated),
  the wide entity scans (the 128-block ally count only on a talk; agents' and ghouls' prey scans every 10 ticks, filtered before the
  sight ray), the villager tick end to end. Fixed: a villager walking up for a kiss/hug never leaves its village for it (at the edge it
  does it there: `place.inside(partner)`); `ChildRenderer.texture` caches its ResourceLocations (`TEXTURES`; it made several a frame per
  child). 1.3.81 already throttled calm villagers' danger scans (`LOOKS`). Nothing else found by reading; no profiler run (no game here).
- **1.3.81 villagers** (protocol 72, unchanged). `tools/village_structure.py`: two humans per home (Home = k and 8 + k: the slot).
  `tools/human_skins.py`: the 10 old looks first (unchanged), then two turns of every ChildBlinks skin (`MORE_TURNS`), mid greys and dull
  colours (s < 0.3, v .25-.8, not skin) tinted at sat .42 for the new ones, kept if >= 30% of the skin changed (`DIFFERENT`), `DROP`ped by
  hand after looking (daughter_01 #2, son_08's, son_13 #2): 26. `HumanRules.villageLook(midX, midZ, slot)` / `villageName(..., names)`
  (a seeded shuffle per village, `shuffled`); `HumanBrain`: once per village human (`UNIQUE` flag) after `VillagePlace.of`: look + name
  (`NAME`) by `VillagePlace.slot`; `HumanBrain.name(human)` everywhere in `HumanTalk`; `HumanPersona.names()/named(i)`. Lag: `LOOKS` 4 (a
  calm villager's danger scan every 4 ticks, staggered). Sheet: `docs/handoff/villagers-1.3.81.png`. `NpcTests.village`.
- **1.3.80 the living tank** (protocol 72, unchanged). Pure `control/GhoulTank`: `taken(mastery, eaten, form)` = (1 - 0.35 m/1000)(1 -
  0.25 eaten/(eaten+20))(1 - form); `form(out, stage, kakuja, final)`: in 0, out 0.10 + 0.04 x min(3, KaguneForm ordinal), any kakuja
  0.32, a final 0.50; `bypasses(msgId)` (outOfWorld, genericKill). `server/GhoulTankServer`: LivingHurtEvent LOWEST on ghoul players
  (`Sightings.kakuja`, new: owl/ukaku/graft/rinkaku kakuja; `Sightings.finalOf`). War/Peace no longer add max health for players
  (`KingServer.boost`; `unboost` still removes the old HEALTH modifier; Ken the NPC keeps his); `RinkakuFormBuffs` skips HEALTH_BOOST
  (stub `MobEffects.f_19616_`: proven, the original links it). `KingChoiceView`: "Toughness". `GhoulMeatTests.tank`.

# State after 1.3.79

- **1.3.79 children's looks** (the owner: "make dozens of other versions with different hair colors and eye colors ... careful with the
  hair cause skins usually have hair on the back"; protocol 72, unchanged; client only). `tools/child_variants.py` writes
  `textures/entity/child/skins/<key>_v1..4.png` (136 for 34 skins) and `family/ChildLooks.java` (`Look(texture, hair, eyes)`, `of(key)`
  its own first, `pick(key, hair, eyes)`: 2 x hair distance + eye distance, `ChildSkins.distance`). Hair: shades from the face front's
  fringe (first 1.5 rows, not skin, >= 4%, up to 10; a vivid shade far from the commonest's hue dropped: son_13's horns); `Hair` matches
  by distance < 38, by hue for coloured hair (sat >= 0.25, v >= 0.1; +-16 degrees, sat and light in range) and by light for greyish hair;
  never skinlike. Head: all matching pixels (the face and hat fronts only flooded from the hairline; eye boxes never); girls' body: floods
  from the top of torso/jacket faces and arm tops (backs also from their top 40%; chest fronts only their outer 2.5 units). `BODY_OFF`
  (daughter_06, 07), `NOT_HAIR` (son_07's hood on the hat layer, daughter_13's skirt), `NO_VARIANTS` (daughter_02, 04, 07, 10, son_14),
  `EYES_ONLY` (daughter_08, 17, 23; and every skin whose fringe is skin-coloured: 05, 09, 14). Irises: inside blink_skins.BOXES, not
  white/skin/black/glint. Recolour by light against the mask's mean (highlights toward white). `ChildRenderer.m_5478_` draws
  `ChildLooks.pick(skin.key(), genome.hair(), genome.eyes())`; overlays still by the skin's key. Existing children may change colours.
  Review sheet: `docs/handoff/child-looks-1.3.79.png` (every version front and back). Previews were checked skin by skin, zoomed in
  (the first passes recoloured boys' hoodies and shirts, a hood, bows: all fixed). `NpcTests.village`.

# State after 1.3.78

- **1.3.78 dates** (protocol 72, unchanged; `HumanNpc.HOLDING` new entity data: holder id + 1). Pure `village/HumanHand`: `hand(slim)`
  (0.34/0.375 + 0.75 sin EtoHand.ARM), `holder(woman)` (a woman's at Eto's size, as `EtoHand.OUT_FEMALE`), `out(villagerSlim, holderWoman)`,
  `offset(bodyYaw, out)` (to the holder's right). `HumanPersona.walk`, `stroll` (3 each), `letGo`, `edge` (16 each). `HumanTalk`:
  partners get `Choice.INVITE` ("Let's go for a walk, hand in hand.", hidden while holding): the talk ends, `HumanBrain.takeHand`, a line,
  hearts, `DATE` +4 regard once a day (`DATED`). `HumanBrain`: `Mind.holding/holdShift/strollAt`; right after the danger branch,
  `holdHands` (EtoBrain.holdHands' way: HAND gesture held (`gestureUntil` MAX), glued every tick at `HumanHand` beside the player's body
  yaw, noPhysics + no gravity, their motion, facing their yaw, a heart every 80 ticks, a `stroll` line every 300..700) and nothing else that
  tick; `letGo` on a shift tap (a `letGo` line and a WAVE), far (> 12), dead, spectator, a fright (silent), or the spot outside the
  village (`place.inside`: an `edge` line, it stays inside); `EtoBrain.solid` restores the body; a new Mind clears no-gravity and holding.
  Client: `HumanRenderer.glue` (EtoRenderer.glue's way with `HumanHand`, registers `HandClient.villager(id)`), its left arm alive by
  `HandSway.arm(..., size 1.0)`; `HandClient` steps villagers' sways too and `heldByVillager(holder)`; `EtoClient.handHeld` counts it, so
  the player's own arm reaches out (KaguneBodyAnimation). Not previewed; untested in game. `NpcTests.village`.

# State after 1.3.77

- **1.3.77 villagers' gestures** (protocol 72, unchanged; `HumanNpc.GESTURE` is new entity data, no message). Pure `village/HumanGestures`:
  a move = an `EtoGesture` ordinal or `CUE` 128 + a `ChildCue` ordinal; `pack(code, style, count)` (8 + 2 + 22 bits); `style(temper)`
  (EtoPose style: CHEERFUL/FLIRTY/DREAMY Eto's 0, KIND/BOLD Ken's 1, SHY/GRUMPY/NERVOUS Touka's 2); `ticks` (held Eto gestures `HELD` 40),
  `eyesClosed`, `blush` (Eto's blush flag; the cues GIGGLE, PLEAD), `still`; picks by temper: `hello(temper, partner)`, `chat`, `about`,
  `news`, `compliment`, `confessed(answer)` (AWE / child CROSS / FLINCH), `yes`, `love`, `bye`, `greet`, `dusk`, `idle`; none hold props.
  `HumanPersona.kissed`, `hugged` (16 lines each). Server `HumanBrain`: `Mind.gesture/gestureUntil/gestures/then/with/idleAt/pending*`;
  `gesture(human, code, then, now)` (sets the entity data); `gestures(...)` each tick (ends it; the `then` move next, KISSED with hearts; a
  fright ends any); `hearts(human, level, n)` (`ParticleTypes.HEART` between it and `with`); before the talker check: walking up to its
  partner for a pending kiss/hug (`HumanTalk.CLOSE` 1.3, `COMING` 100 ticks), then the move and its line; a still move holds it in
  place facing `with`. `village`: a dusk word comes with YAWN/STRETCH; standing (navigation done) every 500..1400 ticks an idle move;
  `meet`: both face each other, greet moves. `HumanTalk`: hello/bye/each answer's move (`answered` keeps the last confession's answer);
  SAY_7/SAY_8 hearts; partners get `Choice.KISS`/`HUG` (labels "Kiss <name>.", "Hug <name>."): the talk ends (`S2CNpcTalk` with no choices:
  `NpcTalkClient` closes the screen), `pending` set. Client `HumanRenderer`: `posed(human, age)` (a new packed value restarts it from
  `shownAt`; a cue: `ChildTalkPose.pose(cue, t, null, false)` x `cue.weight`; a gesture: `EtoPose.gesture(g, t, style, count % 3)`),
  drop/spin/flip/roll as `EtoRenderer`, `Model.pose` applied per part (`RigPose.transform`); the `Blink` layer also draws the blush
  (`HumanRules.blush(look)`: `ChildBlinks.blush(child)`, `tools/blink_skins.py` writes `<key>_blush.png`: an oval under each boxed eye,
  only on skin, alpha to 170) rising over 8 ticks, and shuts the eyes for `eyesClosed` moves. Not previewed on a villager (reused poses).
  `NpcTests.village`.

# State after 1.3.76

- **1.3.76 blinks** (the owner: "they should all blink like Touka, Eto and all the kids do"; protocol 72, unchanged; client only). By
  reading, the children never blinked (only `EtoRenderer`'s `closed`); now they do. `tools/blink_skins.py` (imports `child_skins.py`'s
  face readers) writes `textures/entity/child/skins/<key>_closed.png`: each eye boxed BY HAND (`BOXES`, face pixels, rectangles holding the
  whole eye and no hair; looked at on a 40 px-per-face-pixel grid); everything inside (but `KEEP`: son_12's blood) is grown over from the
  skin round it (`inpaint`: rings, each pixel the mean of its filled or skinlike neighbours, then from anything if no skin reaches), a lash
  line (0.36 face px thick, at 68% down; on 8x and finer skins a 0.12 px sag and thinner ends) in a neutral dark as deep as the eye's
  darkest (never its hue), painted on the hat layer wherever the hat has any alpha. `TONE_ROWS` (son_11: its skin tone read from rows
  2.5..4.5). `NONE`: daughter_02, 04, 06, 07, 10, 11, 12, 20, 21, 22, son_14. Writes `family/ChildBlinks` (`has`, `closed`): 28 of 39.
  **The first try (found by colour, one flat fill) was rejected by the owner with a dozen faces marked: never judge faces from a
  thumbnail; zoom every one.** Review sheet: `docs/handoff/blinks-1.3.76.png`. `HumanSkins.Skin` gained `child`; `HumanRules.closed(look)`;
  `HumanRenderer.Blink` layer and `ChildRenderer.Kakugan` (now first: shut eyes over any kakugan) draw it while `EtoRules.blink(id * 31 +
  7, age)` or asleep. `NpcTests.village`.

# State after 1.3.75


- The owner dropped RULE 1 in the session that finished 1.3.75 ("ignore the 97% rule, I changed my mind"); `CLAUDE.md` still holds it
  until they say otherwise for good. Commit and push often anyway.
- **Next:** what is left of the owner's update (`docs/handoff/next-ai-1.3.67.md`, plan 5): humans' gestures (kisses, hugs, blushes: from
  Eto's, the kids', Touka's and Ken's animations), then dates (walking together hand in hand, inside the village).
- **1.3.75 the villagers' own skins** (protocol 72, unchanged). `tools/human_skins.py` makes `assets/hexkagune/textures/entity/human/
  human_00..09.png` from ten of the children's processed skins (`textures/entity/child/skins`, never the owner's originals: daughter_21,
  08, 18, 19, 13, son_04, 08, 12, 06, 13), hair, clothes and eyes turned in hue (each its own turn and lightness), skin kept (matched
  against its ChildSkins tone: within 14 degrees, saturation .16, a blush kept; on the head's own layer a wider match, 24 degrees and .24,
  as its shading strays: without it #0's cheeks went teal), whites, blacks and greys kept; writes `village/HumanSkins.java` (path, girl,
  slim). `HumanRules.CUTE` 1000, `pick(roll)`, `namespace(look)`, `skin`/`slim` for looks >= CUTE; `HumanBrain` gives a new human
  `HumanRules.pick` (old ones keep their vanilla look, saved); `HumanRenderer` maps looks >= CUTE to the mod's textures (HD skins: 64 to
  1024 px, the model's UVs are fractions); `NpcTalkClient` uses `HumanRules.namespace` for a human's face. Honest look (a flat front view,
  not a model): most read clearly different from the children (hair and clothes); #1 (from daughter_08) and #3 (daughter_19) only a little
  (mostly grey and white). `NpcTests.village` (files there, new looks map to them, old ones to vanilla's).

# State after 1.3.74


- **RULE 1 (the owner's, every session): at 97% of the 5-hour or weekly usage, stop, save, hand off, give a prompt. See `CLAUDE.md`.**
- **Next session:** read `docs/handoff/next-ai-1.3.67.md` first (the owner's last update and its plan, in order), then
  `docs/handoff/next-ai-1.3.60.md` (house rules). Done of that plan: 1-4 (1.3.68-1.3.72), 5: talking (1.3.73), romance and the ghoul's
  confession (1.3.74). Left of 5: dates (walking together hand in hand), gestures for humans (kisses, hugs, blushes...: the owner: "take
  animations from eto, kids, touka and kaneki for base, mix it together and adapt it to NPCS": HumanRenderer.Model would apply an
  EtoPose RigPose via RigPose.transform as EtoModel does, the gesture synced by a new S2C message or a field), and "make the humans as cute
  looking as possible": skins adapted from the owner's kids' skins (art/children) with other hair and clothes, never altering the
  owner's files (generated copies only). Optional, from the earlier list: War/Peace same-move cancels.
- **1.3.74 romance with humans** (protocol 72, unchanged). Pure `HumanPersona`: `CONFESSED` 16 / `PARTNER` 32 (bond flags beside
  NpcBonds' 1..8), `Answer` YES/DOUBT/NO, `confession(temper, regard, convincing, roll)` (trust KIND .6 .. NERVOUS .15, + regard past CLOSE
  /100, + .35 pleading; a failed first try is a DOUBT, a failed plea a NO), `confessed(temper, answer)`, `yes(temper)`, `love(temper)`,
  `hello(temper, regard, partner)`. `HumanTalk`: offered choices per state (`Talk(npc, offered)`): SAY_5 the confession (a ghoul player,
  regard >= CLOSE, not confessed or partnered, not refused today), SAY_6 the plea (after a DOUBT: `DOUBTED`), SAY_7 ask out (CLOSE, not
  partnered, a human player or confessed: PARTNER and a chat line), SAY_8 "I love you." (partnered); a NO: regard -8, `REFUSED` for the
  day. `NpcTests.village`.

# State after 1.3.73
- **1.3.73 talking to humans** (protocol 72, unchanged). Pure `village/HumanPersona`: `Temper` x8 (title), `temper(seed)`, `name(seed)`
  (24 names), `WARM` 20 / `CLOSE` 60 regard, `liking(temper)` (compliment), `hello(temper, regard)`, `chat`, `about`, `news`,
  `compliment(temper, regard)`, `bye(temper, regard)` (~128 lines). Server `HumanTalk`: right click (`EntityInteract`, cancelled) within
  `REACH` 6 opens the talk (`S2CNpcTalk`, skin `-100 - look`: `NpcTalkClient` maps <= -100 to vanilla's `HumanRules.skin`); frightened:
  refused; a trade ends; MET flag; asks SAY_1..4 (chat: +`NpcBonds.CHAT` once a day via `chatted`; about; news; compliment: +liking once a
  day, `PRAISED`), LEAVE: a bubble goodbye. `talker(human, level)` keeps it still and facing them (`HumanBrain.tick`, after the danger).
  `C2SNpcTalk` routes a `HumanNpc` to `HumanTalk.heard`. Seed: the UUID's halves xored. `NpcTests.village`.

# State after 1.3.72
- **1.3.72 trading and chat in the village** (protocol 72, unchanged). Pure `village/VillageTalk`: `Goods(item, name, price)` x12,
  `COIN` emerald, `trade(goods, seed)` (5 lines: seller call, buyer ask, seller price, buyer pays `hands` true, seller parting),
  `greet(seed)` 10, `dusk(seed)` 4. Server `VillageTrade` (both `HumanBrain.Mind.trade`; ticked once a tick by either: walk to the street
  spot nearest their midpoint (seller) and `APART` 1.8 off it (buyer), `GATHER` 300 then talk where they are; still, facing; lines every
  `40..120` ticks by length via `HumanBrain.say`; the hands line swaps the items; ends on gone, fright (`shakenUntil`), night, `LONGEST`
  1200 or done: hands emptied, `tradeReady` +`AGAIN` 1600 + 0..600). `HumanBrain`: `Mind.trade/tradeReady/greetAt/dusk`; a danger ends a
  trade; `village`: a trade runs first; at dusk a word (1 in 3); by day `meet` once a second (a greeting to one within 4, each a minute
  apart, 1 in 4; else a trade with one within 20 when both may, 1 in 6); `face`, `say` (bubbles to players within 24 who see it).
  `NpcTests.village`.

# State after 1.3.71

- **1.3.71 the Brick Village** (protocol 72, unchanged). `tools/village_structure.py` (uses `library_structure.py`'s NBT) turns
  `art/village/Brick_Village-from-abfielder.litematic` (1.21.8, author PaaskeVillads) into `data/hexkagune/structures/brick_village.nbt`
  (99x41x97; the template's y 0 is the walking level: the ground is the world's; every cell to y 12, above it only solid or roofed;
  SWAPS for 1.21-only blocks; block entities as 1.20.1 reads them; armour stands as ArmorItems) and writes
  `control/village/BrickVillage.java` (do not edit by hand): `PERIMETER` 51, `STREETS` (open-sky ground every 4th block, y 0..1),
  `ROUTE` (16 street spots 14..34 out, by angle, ~20 degrees apart), `HOMES` (inside each street door: a flood fill on the inner side
  stays roofed), all from the template's middle (49, 0, 48). The template holds 8 `hexkagune:human` (at the homes) and 2
  `hexkagune:ccg_investigator` (streets), yaw 0, PersistenceRequired, `ForgeData.HexVillage` {CentreX/Y/Z: to the middle, Home}.
  Worldgen: `structure/brick_village.json` (jigsaw, beard_thin, start_height absolute 0 on WORLD_SURFACE_WG), its pool, its set
  (random_spread 44/18, salt 1371206533), `tags/worldgen/biome/has_structure/brick_village.json` (plains, sunflower_plains).
  Pure `village/VillageRules`: `turns(yaw)` (a yaw-0 body turned with the template), `turn/unturn` (CLOCKWISE_90: x, z -> -z, x),
  `inside`, `clamp`, `next`, `nearest`, paces and times. Server `VillagePlace.of(mob)` (first tick: the turn from its yaw, the middle in the
  world and its home = where it stands, kept as Turns/MidX..Z/HomeX..Z in HexVillage) -> `Place.world(spot)`, `inside`, `clamp`, `local`.
  `HumanBrain`: a village human: `village(...)` (night: home and still; day: a street spot within 22 every 140..340 ticks); fleeing
  clamped inside the perimeter. `AgentBrain`: a village investigator: `wake` makes no partner, `lost` never removes it, `patrol` ->
  `village(...)` (`Mind.waypoint` on `ROUTE`, joined at the nearest, one way or the other by its id). `NpcTests.village`.

# State after 1.3.70

- **1.3.70 CCG ranks and protection** (protocol 72, unchanged). Pure `AgentRules.Rank` (RANK_3 30, RANK_2 25, RANK_1 20, FIRST_CLASS 14,
  ASSOCIATE_SPECIAL 8, SPECIAL 3 in 100; health 36..124, speed 0.32..0.395; `roll(r)`, `of(leader, a, b)` the better of two for the
  leader, `byOrdinal`), `AgentRules.DEFEND` 24. `AgentBrain`: `Mind.rank`, saved as `RANK` (`CcgAgent.KEY + "Rank"`) in its persistent data,
  rolled once in `wake` (health and speed attributes set every wake, healed whole when first rolled; the name is its title; the old
  SENIOR/JUNIOR names gone); `wanted`: a `GhoulNpc` only if `mind.out || mind.eyes` (or `current`); `reveal`: shown also by
  `GhoulServer.ghoul` or any `Sightings.last`; `prey`: every 10 ticks `defend(agent, level, now)` (a `HumanNpc` within DEFEND hurt lately:
  its `hurtBy` while `hurtUntil`, not an agent or a creative player; or a `GhoulNpc` whose prey is a human), marked for both of the pair
  and taken as `current` over anything. `HumanBrain`: fleeing, `guard(human, level, danger)` (the nearest agent within DEFEND, not past
  the danger) -> to `FinalSight.behind(agent, danger, 1.6)`, standing there once within 1.2; else away. `AgentTests.ranks`.

# State after 1.3.69

- **1.3.69 the ghoul arm blade** (protocol 72, unchanged; client drawing only). `BladeArmModel`: `sabre(sink, cx, root, front, length,
  width, thick, ...)`: `SECTIONS` 12 diamond rings (forward edge z -0.55 w, back edge +0.45 w, flats +-t), width `0.75 W (0.82 + 0.5
  sin(min(1, 4u) pi/2)(1 - 0.2u)) (1-u)^0.75`, thick `0.5 T (1-u)^0.6`, swept `z - 1.3 u^2`, the edge faces lighter, a glowing seam on both
  flats but the last two sections; `wedge(sink, base, dir, across, length, width, thick, colour, keel)`: a pointed plate (four sloping
  faces and its base) for the spiral's scales, the shoulder fins and the root fins; `outward(corners, inside)` normals (two-sided drawing:
  winding never matters; never exactly up, the glow mark). The sleeve is still a box. Preview `tools/BladeArmPreview.java`.

# State after 1.3.68

- **1.3.68 wild ghouls by night and day** (protocol **72**: `S2CGhoulNpc` gained `eyes` (a 15-arg constructor keeps false for
  `ChildKagune`); `GhoulNpcClient.View.eyes`; `GhoulNpcRenderer.Eyes` draws the kakugan whole while it is set). Pure
  `NpcRules.prowls(wild, dayTime)` (wild and night) / `hides(wild, dayTime)` (wild and day). `GhoulNpcBrain.Mind.eyes` (sent as eyes and
  not out); `tick`: a wild ghoul by day runs (`flee`) from any prey (only a grudge makes one by day); `wanted`: (after the grudge and the
  owner) by day false, `HumanNpc` at night true. `HumanRules.notice(sight, shown)`: not shown -> 0 (was CLOSE 6); `HumanBrain.danger`:
  a player shown = kagune, a final sight, `GhoulServer.ghoul` (kakugan open) or any `Sightings.last`; a `GhoulNpc` only with `mind.out` or
  `mind.eyes`. `FamilyTests.fear`.

# State after 1.3.67

- **1.3.67 War and Peace runs** (protocol 71, unchanged; client only). `KingRun.war`: the beast's charge (lean 0.24 + 0.76 s, flip -0.15 s,
  head out and up; `rake` = cos 2 phase: both claws `s (-1.25 + 0.9 rake)` reach between footfalls and rake back as each lands, elbows bent
  as they pull; walking: the old pump x (1 - s); twist, side, roll lurching; heavy wide legs; stomp drop 0.4 + 1.8 s). `KingRun.peace`: the
  glide (lean 0.12 + 0.7 s, head level, arms back 0.2 + 0.2 s and out 0.1 + 0.75 s like swept wings, a wingbeat with the bound; bank; legs
  0.55 + 0.5 s swing, 0.8 + 1.2 s fold; drop `(0.25 + 0.45 s) land - (0.2 + 2.8 s) air`). `PeaceMotion.rest` running: pitch
  `+(14 + 9 tier) run` (was -7), `y + 0.12 tier run`. `EtoFight.peace`: Ken's run (`KingRun.peace` on his limb swing x 0.6662, run as
  sprint, free unless a move; not in the air). Preview: `tools/art-preview.sh <dir> king-runs`; the lowest point of Peace's form through a
  sprint (32 phases) is +0.14 blocks (measured by a scratch class over `ArtPreview.peaceRun/peaceTris`).

# State after 1.3.66

- **1.3.66 War's rinkaku alive** (the owner: "the back tendrils in WAR path are too unrealistic and solid, they are like unmoving branches of
  a tree, they have 0 similarity to the original tendrils of the other kagunes"; protocol 71, unchanged; client drawing only). `WarModel`:
  `Motion` gained `lag, run, rise` (canonical 10 args; the 4/6/7-arg constructors keep working) and `drifting(lag, run, rise)`;
  `centre(sx, level, time, grow, motion)` (public) the centre line, `ALIVE_POINTS` 12 sampled `along` the six control points' Catmull-Rom
  curve, then `alive(p, root, u, seed, time, motion)`: the original TendrilChain's idle motion in kind (side 4.4 + 1.5, bob 3.0 + 1.1, wander
  2.4 + 0.9 px on slow waves, phase running out along it `- u x 2.4`, scaled u^1.25 x (1 + jolt) x (1 + 1.2 frenzy), pace 1 + 1.5 frenzy, a
  flex of the whole, a 1.2 u^2 sag), following him (x + lag 9, y - rise 9, z + run 9, all x u^1.4), `FLOOR` 0.8 (body px over the feet);
  then the moves' lerps as before. `tendrilRadius` smooth (2.35 (1-s)^0.85 + 0.1; the segment flare and `SEGMENTS` gone); the tube: a
  low ridge (0.12), a swell `1 + 0.08 sin(17 s - 0.22 t + 1.9 seed)`, a sheen instead of armour bands; the barbs, thorns and hooked claw
  gone, a short fine point instead. `KingLayer.drift(player, tick, k)` (out of `peaceState`: SWAY eased a tick at a time) feeds
  `warMotion(player, view, time, tick, k)` (the Pair: now k 0, now + 1 k 1; the 3-arg one by the time's fraction).
  Preview: `tools/art-preview.sh <dir> war-alive` (`king-war-alive.png`: from behind a second apart, running, turning, rising, falling,
  frenzy). `KingTests` (moves 14..18 px in 2 s per tip, follows, never below FLOOR).

# State after 1.3.65

- **1.3.65 fear of the final kakujas** (protocol 71, unchanged; `EtoGesture` FLINCH, AWE and `ChildCue` COWER appended). Pure
  `npc/FinalSight` (NONE, WAR, PEACE, DRAGON, RINKAKU, UKAKU; never sent): `of(dragon, worn, rinkaku, ukaku)` (the Dragon first),
  `terror()` (WAR, DRAGON), `scares()` (300 / 240), `STILL_SCARED` 40, `KEEPS_RUNNING` 28, `TERROR_RUN` 20, `SHOUTS` 120, `HIDES` 30,
  `BEHIND` 0.95, `THERE` 0.6, `KEEPS_OFF` 7, `behind(sx, sz, fx, fz, gap)`. Pure `npc/FearLines`: `stranger`, `ally`, `fleeing` (terror),
  `child(stage, call, sight)`, `hiding(stage, call, other)`, `scream(sight)` (NONE: an ordinary ghoul), `falter(sight, senior)`,
  `rally(senior)`: 132 lines; `Script.dread(sight, voice, home, seed)` (Eto 23 in `EtoLines.dread`, Touka 21, Ken 21) and
  `Script.dreadGesture(sight, ours)`. `HumanRules`: `DREAD_NOTICE` 28, `TERROR_NOTICE` 40, `TERROR_SAFE` 52, `TERROR_RUN` 1.3,
  `TERROR_MEMORY` 100, `SCREAMS` 70, `HEARD` 32, `notice(sight, shown)`, `pace(sight)`. `AgentRules`: `falter(sight, senior)` (50 / 30,
  a senior 70%), `FALL_BACK` 60, `FALL_BACK_TO` 14, `FALTER_AGAIN` 1200.
  Server: `Sightings` keeps `SIGHT` (`seen(player)`, `finalOf(player)`); a watcher round also when the sight changes among the worst;
  `terror(level, player, sight, now)` each check while War/the Dragon is worn: wild ghouls in sight within 28 (not allies) keep
  `scaredUntil` >= now + 40 and `scaredOf` = them (one shouts every SHOUTS), their own children within 16 (8, or in sight) get
  `ChildBrain.dread`. The ghoul watcher sets `scaredOf` and uses `FearLines` for the worst; the child watcher too (and `dread` for a terror).
  `GhoulNpcBrain.Mind.scaredOf`: `flee` runs from it (within 48; a terror: 20 blocks at a time); `scare()` clears it. `HumanBrain.danger`
  returns a `Danger(body, sight, ghoul)`: players by `HumanRules.notice` (beyond NOTICE only in its sight), a terror remembered
  `dreadOf/dreadUntil`; `scream` (bubbles to players within HEARD who see it: fresh danger, at most every 200; a terror every SCREAMS + 0..30);
  `flee(human, danger, pace, length)`. `AgentBrain`: `reveal` counts `sight.any()` as shown ("The CCG has seen what you are."), and the
  first sight (in its sight, FALTER_AGAIN since the last) -> `falter(agent, mind, player, sight, level, now)` (a line; `falterUntil`,
  `fallBackUntil`, `rallies`); `tick` -> `falter(agent, mind, level, now)` after the stun: frozen (facing, stopped; a blow since
  `falteredAt` ends it), then backing off to FALL_BACK_TO, then the rally line. `say` = bubbles to players within 24 who see it.
  `ChildBrain.Mind.dreadOf/dreadUntil/dreadSaidAt`; `dread(npc, from, level, now)` (not carried; fresh: ends its talk, moment, hide and
  seek, tag, minding); the chain's `cower` right after carried (the other parent: a player parent not in a terror or a companion parent,
  the nearer within 24 -> `FinalSight.behind` them; none: keep KEEPS_OFF; a baby sits; COWER cue held; a `FearLines.hiding` line every
  160..260 to players within 16 who see it); `lines` silent meanwhile; the COWER cue cleared when it ends; `Conversation.free` refuses
  one hiding. `EtoBrain.saw(npc, player, shown, sight, level, now)`: DREAD with a sight -> `script.dread` and `dreadGesture`.
  Client: `HumanRenderer` and `AgentRenderer` draw `SpeechBubbles`; `ChildTalkPose` COWER (arms crossed up before its chest and chin, a
  little one's hands under its chin, head tucked, trembling, a flinch now and then); `EtoPose` FLINCH (a step back, the right arm in across
  her, the left up before her face) and AWE (hands together low on the chest, head tipped back, breathing). Previews:
  `TalkPreview` (COWER added), `CompanionPreview` writes `eto-/touka-/ken-fear-poses.png`. `FamilyTests.fear`, `CompanionTests` (order).
  Not covered: Ken's own Peace (an NPC) frightens no one; companions don't move at a sight; no scream sound in the pack (bubbles only).

# State after 1.3.64

- **1.3.64 dread is worn** (protocol 71, unchanged). Pure `Display.of(dragon, king, finalKakuja, kakuja, kagune, eyes)`: DREAD for the
  Dragon, War/Peace worn, or a final kakuja; else KAKUJA, KAGUNE, EYES, NONE. `Sightings.of(player)` feeds it: `DragonLock.dragon`,
  `KingServer.wearing(player) != NONE` (was `KingServer.awake`, the permanent save: a War/Peace-path player read DREAD forever, so the
  rise-only watchers reacted once and never again), Rinkaku's final kakuja (`KaguneProfile.isActive`, `PathServer.path` RINKAKU,
  `KaguneProfile.form == RinkakuForms.finalKakuja()`), Ukaku's (`UkakuServer.kakuja && UkakuServer.form(player).finalKakuja()`); the
  rest as before. `FamilyTests.reactions`.

# State after 1.3.63

- **Next session:** read `docs/handoff/next-ai-1.3.60.md` first (family talk after the polish). The owner's next update (asked during
  1.3.60, "after this one is fully finished"): clash impacts sized by how hard each hit was, shaking the ground and camera; the same move
  meeting itself (big punch, speed kick...) cancelling both with a big impact; more and better reactions to the final kakujas (Rinkaku and
  Ukaku, especially War/Peace: being scared of War); new dramatic running animations for War and Peace; their new clash sounds (1.3.61).
- **1.3.63 same-move clash** (protocol 71, unchanged). Pure `pale/MoveClashRules`: `Family` PUNCH (PUNCH, OWL_PUNCH and their charges),
  KICK (KICK, OWL_KICK), SLAM (HAMMER, OWL_FISSURE and charges); `released(move)`; `window(move, detail)` (to its impact + 1: punch
  `PUNCH_IMPACT`, kick `KICK_BURST + dash + KICK_CONTACT`, the Owl's kick `dash + KICK_SKEWER`, `HAMMER_IMPACT`, `FISSURE_IMPACT`); `reach`
  4.5 / 4 / 5.5; `BIG` 2.4, `STAGGER` 14; `power(hitA, hitB)` = max(BIG, `ClashRules.impact`). Server `MoveClash`: `begun(who, move,
  detail, target, now, hit, live, stop)` from `PaleServer.begin` (a punch's hit `punchDamage x power`, else `HIT` 20) and `EtoOwl.begin`
  (HIT); a `LevelTickEvent` (END) checks each live registered move in its window against bodies within reach doing the same family
  (`moveOf`: a player's `PaleServer.state`, Eto's `mind.owl.move`) and both at each other (`at`: the target by id, else facing) -> both
  stopped (the other's registered stop, or `cut`: `PaleServer.end` / `EtoOwl.end`), `KaguneClash.collide(a, b, power)` (apart both, the
  wind, the ground), `PaleStun.stun` both for STAGGER. Not covered: War/Peace (`KingServer.move` has no cancel), `ChildFlesh`.
  `ClashTests.sameMoves`.

# State after 1.3.62

- **1.3.62 clash size** (protocol **71**: `KingMarkFx.GROUND` 8, appended). Pure `ClashRules`: `HIT` 7 (a blow of unknown strength),
  `REFERENCE` 12 (two blows' damage for an `ORDINARY` 1.2 clash), `LEAST` 0.7, `MOST` 3, `QUAKES` 1.55; `impact(hitA, hitB)` = 1.2 x
  sqrt(sum / 12) clamped; `quakes`, `rumble(power)` (0.35..1 above QUAKES), `rumbleTicks` 8 + 6p, `felt` 10 + 8p, `push` PUSH x (0.7 +
  0.25p), `volume` 0.95 + 0.3p, `pitch` 1 + (1.2 - p) x 0.15 (at least 0.65). `KaguneClash.Swing` has `hit`; `swing(..., hit, ...)`
  overloads (old ones: HIT); `meets` -> `impact(mine.hit, theirs.hit)` -> `apart(..., power)`, `wind(a, b, power)` (burst at that power
  and `volume`; quaking: `quake(near, ground, power)` = an `S2CKingFx` MARK `GROUND` at the lower feet with power - 0.6, and
  `sfx.quake` at 0.8 volume, pitch 0.85). `burst` pitch = `ClashRules.pitch`. Hits: PaleBlitz `fist(mastery) x strike.damage x power`;
  KaguneCombat the first target's base damage; GraftCombat `GraftRules.damage x` the heaviest beat's power; GhoulNpcCombat
  `NpcRules.damage(tier) x` blade weight / heaviest beat; AgentBrain `AgentRules.damage(q, step)` (a parry step 0). Client:
  `KingMarks` CLASH adds `rumble(at, ClashRules.rumble(p), rumbleTicks, felt)` (-> `PaleCamera.rumble`) when it quakes; `GROUND`
  -> `ground(at, p)` (a dust ring of puffs, grit) and `KingMarkFx` GROUND shapes (two dust rings, grey sparks). `ClashTests.impacts`.

# State after 1.3.61

- **1.3.61 clash sounds** (protocol 70, unchanged): the owner's `Clashes.zip` (Clash1..4.ogg, mono 48 kHz) copied unchanged to
  `assets/hexkagune/sounds/sfx/clashes/clash_1..4.ogg`; `tools/sfx_pack.py` (the single source of `sounds.json`: run it after any sound
  change; regenerating before this change reproduced the file exactly) has `CLASHES` and the `clash` pool (gain 0.95) is those four, so
  `sfx.clash` and the original's `kagune_guard` play them. `KaguneClash.burst` plays `sfx.clash` at the burst's volume (pitch 1 +
  (1.2 - power) x 0.15) and the gust (`entity.ender_dragon.flap` at 0.8), no longer the `KAGUNE_SLAM` heavy punch under it.

# State after 1.3.60

- **1.3.60 review fixes** (protocol 70, unchanged): `Conversation.tick` ends a talk (LEFT, silent) when a speaker who never came is
  dropped and fewer than two members are left; `Conversation.free` refuses a companion following a player.

# State after 1.3.59

- **1.3.59 babies in sibling moments** (protocol 70, unchanged). Pure `family/LittleOnes`: `Kind` PEEKABOO, FACES, BABBLE, LULLABY (never
  sent); `Line(baby, text, cue)`; `EVERY` 1800, `CHANCE` 0.5; `pick(temper, evening, roll)` (evening: LULLABY; else weights by temper);
  `talk(kind, temper, stage, name, seed)` (PEEKABOO older COUNT, older "boo" CHEER, baby CHEER, older GIGGLE; the others older, baby,
  older; `TalkCues.said` overrides a line's cue except PAT); 98 lines, keys "kind:part:temper|any:baby|little|big|teen" (teen falls back to
  big). `SiblingLife.begin`: a free baby sibling in sight -> `baby(...)` (when `Mind.babyAt` is EVERY old and CHANCE): a Conversation,
  the baby the anchor, the older one coming. `ChildRenderer.posed`: a sitting baby's cue keeps its legs and no hop. `FamilyTests.littleOnes`.

# State after 1.3.58

- **1.3.58 hide and seek** (protocol 70, unchanged). Pure `family/HideAndSeek`: `INSTEAD_OF_TAG` 0.5, `COUNT` 100, `SEEKS` 900,
  `HINT_EVERY` 160, `HEARD` 80, `FINDS` 3, `NEAREST` 4 / `FARTHEST` 11, `SPOTS` 24, `REACHES` 160, `AFTER` 80, `LONGEST`; `Part` (PROPOSE,
  AGREE, COUNT, READY, SEEKING, HINT, FOUND, CAUGHT, GIVE_UP, WIN; never sent); `line(part, temper, stage, seed)` (80 lines, keys
  "part:temper|any:little|big"); `bestSpot(distance, hidden)`; `spot(i, hx, hz, radius, seed)` (a golden-angle spread in the yard);
  `heard(sinceHint)`. `ChildCue` gained HIDE (1200 ticks: legs folded forward, `drop` 8.6 / a little one's 4.3, arms round its knees) and
  COUNT (hands over the eyes; a little one's to its cheeks, its head bowed). `ParentDuty.Said` gained a `cue` (3-arg: none), applied as
  it is said (`Conversation.act`). Server `SiblingLife`: `begin` turns TAG into `startHide` half the time (PROPOSE with CHEER, AGREE on the
  seeker's queue; `Mind.hideWith/seeker/hidePart/hideAt/hideBegan/hideFound/hideSpot/hideSetOff/hidden/hintAt/seekTo/seekToAt/seekSaidAt`);
  `tick` runs `hide` first: stops (`stopHide`, both minds, a HIDE/COUNT cue cleared) on anything (gone, carried, a fight, a moment, a talk,
  a player's talk, asleep, following, LONGEST, no home); `seek` (part 0: still, turned away (`Conversation.faceTo`), COUNT, READY at
  COUNT; part 1: found within FINDS in sight after 20 ticks -> FOUND (POINT) + CAUGHT (`caughtCue`) -> `over`; SEEKS -> GIVE_UP (SHRUG) +
  WIN (CHEER); else to a `spot` every 70 ticks or to 1.5 blocks of the hider when `heard`, SEEKING now and then (THINK); part 2: AFTER,
  then stop); `hideAway` (`spot`: the 24 spots with `ground` (a block with collision under two without, from home y + 2 to - 3), seen
  from the seeker's eyes by `ClipContext` COLLIDER to the spot's head height; there or REACHES: HIDE cue, still, peeking at the seeker;
  HINT now and then while looked for; over: found, it stands; not, it runs to the seeker). Guards: `Conversation.free` (no game),
  `ChildBrain.held` (its stroll waits), `ChildBrain.lines` (silent), `ChildBrain.start` (a player's moment ends the game, others refused),
  `ParentCare.tend` (no pick-up). Preview: `TalkPreview` shows HIDE and COUNT. `FamilyTests.hideAndSeek`.

# State after 1.3.57

- **1.3.57 gestures with the lines** (protocol **70**: `S2CChild` gained `cue` (byte, `ChildCue` ordinal) and `cueAt` (varlong)). Pure
  `family/ChildCue` (NONE, CROSS, STAMP, POINT, HIPS, SHRUG, SORRY, CHEER, GIGGLE, WHISPER, BLEH, PAT, SULK, THINK, WAVE, PLEAD; sent by
  ordinal: append only; `ticks`, `running(age)`, `weight(age)` in 4 / out 8) and `family/TalkCues` (`Scene` CHAT, TEASE, BRAG, COMFORT,
  SCHEME, SQUABBLE, MIND, SCOLDED, RAMPAGE, HOME, MAKE_UP, CUT; `said(text)` keywords; `child(text, scene, temper, starter, index, seed)`;
  `saidBy(text)` / `parent(text, scene, persona, index, seed)` -> `EtoGesture`). `Conversation.Line(speaker, text, cue, gesture)` (2-arg:
  none); `Conversation.act` on saying it (a child: `Mind.cue/cueAt`, dirty; a companion: `EtoBrain.gesture`); `ParentDuty.lines(child,
  parent, talk, scene, temper)` and `SiblingLife.lines(first, second, parent, talk, scene, t1, t2)` fill them (`SiblingLife.scene(kind)`;
  breakUp MAKE_UP; MIND); a cut-short line too (CUT). Client: `ChildClient.View` cue/cueAt/cueBegan (`cueAge`), `ChildRenderer.posed`
  (after a moment, before a flip) blends `client/animation/ChildTalkPose.pose(cue, age, stage, carried)` by `weight` (a carried child's legs
  left sitting). Preview: `javac -nowarn -cp build/classes:build/base.jar -d build/talk-preview tools/PoseRender.java tools/MomentPreview.java
  tools/TalkPreview.java` then `java -Djava.awt.headless=true -cp build/talk-preview:build/classes:build/base.jar TalkPreview <dir> <boy skin>
  <girl skin>` (square HD skins, e.g. `art/children/sons/cute-boy-hd.png`, `art/children/daughters/hd-cute-girl.png`). `FamilyTests.cues`.

# State after 1.3.56

- **1.3.56 Eto's child posed** (protocol 69, unchanged; client only): `client/animation/ChildFight.pose(child, clock, partial)` (as
  `EtoFight` without Peace or the Owl: a `PaleAnimator` per child from `PaleClient.get(child UUID)`, its landings, `ArmPose.body` while
  `ArmClient.get(child)`; null when nothing poses it; animators not drawn for `FORGET` 200 ticks dropped). `ChildRenderer.m_7392_` works
  it out for an OWL child (not asleep, not young) into `FIGHT` (by id); `Model.m_6973_` applies it after `little()` with `apply(name,
  part, pose)` (EtoModel's: `RigPose.transform` positions and rotations for head, torso, arms, legs). `ChildRenderer.fightDrawn(child)` is
  passed as `drawn` to `PaleFleshRenderer.flesh/arm` for children (was null). Reference look: `tools/pale-preview.sh <dir> flesh`.

# State after 1.3.55

- **1.3.55 one escort** (protocol 69, unchanged): pure `Siblings.standDown(temper, stage, name, call, seed)` (38 lines, "temper:child|teen",
  `standDownLines()`). `SiblingLife.parentGoing(level, little)` (a live companion whose `duty.child` is it); `begin` skips MIND then;
  `minding` stands down when one is going (minding 0, its escort of the little one dropped, the line on its remark queue unless it is in a
  talk). `ParentDuty.look` skips a child someone else is escorting (`escort` set, not expired, not itself). `FamilyTests.siblings`.

# State after 1.3.54

- **1.3.54 tag in the yard** (protocol 69, unchanged): pure `Siblings.RUN` 5, `YARD` 2.5, `yard(radius)` (the edge less YARD, at least 3),
  `runTo(x, z, chaserX, chaserZ, turn, homeX, homeZ, radius)` (RUN away turned by `turn`; outside the yard: the extra turn (0.5..2.6 rad
  either way) that keeps it farthest from the chaser, else back toward the middle). `SiblingLife.tag` runs there (no home: the game ends);
  `begin` turns TAG into CHAT unless both are `inYard`. `FamilyTests.siblings`.

# State after 1.3.53

- **1.3.53 one conversation lock** (protocol 69, unchanged). Pure `family/Talks`: `GATHER` 2.2, `SPEAKS` 4, `LEAVES` 8, `WAITS` 240,
  `GATHERS` 300, `TURN` 24 deg/tick; `End` DONE, LEFT, GONE, HURT, FIGHT, ASLEEP, INTERRUPTED (never sent); `faces(self, speaker, previous,
  next, anchor, other)`; `turn(from, to, most)`; `remarked(End)` (LEFT only); `cutShort(who, stage, name, call, seed)` (56 lines: a persona
  key's, else temper word + little/child/teen, else any; "" = says nothing). Server `Conversation` (members by entity id, an `anchor` who
  stays put, `coming` members who walk up first, `Line(speaker id, text)`s): `start(level, now, what, anchor, members, coming, lines)` (null
  if any member is in one; children's moments stopped `ChildBrain.stopMoment`, a companion's chore `HomeChores.stop` and still gesture
  cleared), `join(level, who, comes, more)`, `free(body, level, now)` (a child: awake, no fight, no moment, not carried, not following, no
  player talk, no tag/minding/escort; a companion: no fight, duty, hand, kiss, walk-up, player talk, moment, cling), `in(body)`,
  `going(body, level, now)` (ticks it once a tick, whoever asks first; over: the mind lets go: `Mind.conversation` null, a child's
  `lastTalk`), `hold(body, level, now)` (coming: walk to the anchor until within GATHER; else navigation stopped, its leash `m_21446_`
  where it stands (radius 3), no sideways speed, `face`: the body turned by TURN a tick, the head up to 50 deg further, the look control
  at them; a carried child: its carrier has it), `end(body, why, level, now)`. `tick`: each member present (else GONE), `trouble` (asleep;
  hurt since it began (`hurtAt`); a fight (`target`); a child's player talk/following/a carrier not in it; a companion's player talk, hand,
  kiss, walk-up: INTERRUPTED), past LEAVES from the anchor (not coming): LEFT with `cutShort` by a member who stays (a companion first) if
  a line was said; lines: each when due and its speaker within SPEAKS of the anchor (the anchor: of the nearest member), else it waits
  (never came together in GATHERS: LEFT, silent; a line waited WAITS: said from where it is within LEAVES, else that speaker `drop`ped);
  sent with `Speech.turn` (1.3.52); empty and its last pause over: DONE. Hooks: `ChildBrain.tick` (going first; a threat it would fight
  ends it; no `moments` meanwhile; the chain's hold right after being carried; `lines` says nothing meanwhile; `start` refuses a moment
  mid-talk; `held(npc)` gates `ChildNpc`'s RandomStrollGoal); `EtoBrain.tick` (a fight ends hers; before bed/calm: going and hold);
  `EtoBrain.busy` includes it; `ParentCare.tend` never picks up a child mid-talk. Uses: `ParentDuty` (reached: the child's fight state
  cleared, any talk of its own ended, `Duty.talk` started with the child as anchor; after it: DONE -> home, else the duty ends; at the door
  put down, `Duty.home` and the HOME talk, the duty finished once it is over; `finish` ends a talk still going; `led` is only the escort
  now; `stillBy/stillUntil` are gone), `SiblingLife` (held kinds: a Conversation with the starter coming to the other; a squabble's
  `breakUp` joins a free parent of both within 16 who sees it, coming; MIND: on reaching the toddler `Mind.mindingTalk`, then home only
  if it was DONE; remarks (REACT/TEAM, tag shouts, a parent's TRANSFORM word) stay on the old `Mind.talk` queue and are skipped for one in
  a talk). Tests: `FamilyTests.talks`.

# State after 1.3.52

- **1.3.52 bubbles take turns and keep apart** (protocol **69**: `S2CNpcSay(npc, text, ticks)`, `ticks` 0 = as long as it takes to read;
  the 2-arg constructor still sends 0). Server: `ParentDuty.talk` sends each due line with `life(said, next, now)` =
  `Speech.turn(speaker, nextSpeaker, gap)` (until the next queued line if someone else says it, at least 20, at most `Speech.MOST`; the
  same speaker or none: 0). `Parenting.pause` = max(40, min(140, 32 + 1.4 x length)) (was 36..110, 30 + 1.3 x): the pause is now all the
  time a turn is up. Client: `NpcSpeech.accept(npc, text, ticks)` (until = `Speech.life(text, ticks)`: the sent life, 10..`ticks(text)`),
  `left(npc, partial)`, `speaking()`. `BubbleShape.pop(age, left)` shrinks it over its last `GOES` 3 ticks; `BubbleShape.of(width, lines,
  thought, shift, lift)` moves the box (its point stays at the head; the tail's base `tailBase()` as near the head as the box's corner
  allows; a cloud's `trail()` stretched with more, smaller puffs), `rim()` (what is drawn past the box: a cloud 6, a bubble 1.5), `home()`.
  Pure `npc/BubbleLayout.arrange(List<Seen>)` -> `Place(shift, lift)` per id: on the view as angles (x right, y up, `perPixel` = 0.025 /
  distance), sorted oldest first; pairs that overlap (rims in, `GAP` 3 px) pushed apart sideways half each, toward each speaker's side
  (right on top of each other: the newer right), each at most half its width + `REACH` 40 px (the other takes the rest), up to `ROUNDS`
  40; then `tilt` (moved far aside, up to `TILT` 12 px so the tail angles down); then anything still overlapping: the newer lifted over.
  `SpeechBubbles.place(body, base)` works the layout out once a frame (`m_91296_` partial + game time as the frame's key) for every
  bubble in the player's sight (`NpcSpeech.lines(body)`, interpolated positions, each speaker's last `base`), skipping bubbles in their
  last `GOES` ticks (going: kept where they were), and eases each place over ~2 ticks. Preview: `tools/BubblePreview.java <dir>` also
  writes `bubbles-scene.png` (before/after a squabble and a carried toddler, the turn, lines said at once, a cloud beside a bubble).
  Tests: `NpcTests.turns`, `NpcTests.layout`.

# State after 1.3.51

- Next session after 1.3.51: read `docs/handoff/next-ai-1.3.51.md` first: the owner's next goal (more interactions and dialog, beauty and as few
  bugs as possible: nobody walking off mid-talk, no overlapping bubbles in squabbles), how conversations work now, and the problems
  already found by reading the code. Still open: the Owl as an exception for Eto's child if the owner wants it.
- **1.3.51 siblings** (protocol 68, unchanged; the owner: "interactions between brothers and sisters"): pure `family/Siblings` (`Kind`
  CHAT, TEASE, BRAG, COMFORT, SCHEME, SQUABBLE, TAG, MIND, REACT, TEAM; `pick(sa, ta, sb, tb, hurt, strayed, roll)` (no baby; MIND for an
  older one and a strayed toddler; COMFORT when hurt; else weights by both tempers, toddlers brag/scheme less); `talk(...)` -> `Line(who:
  1 the starter, 2 the other, 0 a parent)`: most 1-2-1, SQUABBLE 1-2-1-2, TAG 1-2, REACT/TEAM 1; `tagged`, `breakUp(persona, ...)` 0-1-2;
  NEAR 10, EVERY 1500, TAG_TICKS 400, TAG_REACH 1.3, HURT 0.6, REMARKS_EVERY 1200). `family/SiblingLines` (337 + 12 lines, keys
  "part:who:kind:group", who a temper word / "any" / a persona, group little/big/any; falls back temper -> any, little <-> big; written from
  a script's data, edit the Java directly). Server `SiblingLife` (`siblings(a, b)` a parent in common; `tick` from `ChildBrain` (after
  following a player, before going to bed): tag (`Mind.tagWith/chasing/tagUntil/taggedAt`: the chaser runs at 1.3, tags within reach,
  roles swap with a shout; the runner runs off), minding (`Mind.minding/mindingUntil/mindingReached`: to the toddler, a word each, then
  its `escort` home), else every 40 ticks a new moment with a free sibling near in sight (`Mind.siblingAt`; by night only chat, comfort,
  minding): talks queued on its `Mind.talk` (speaker ids; `ParentDuty.talk` says them), both standing still for them (`stillBy`); a
  squabble's `breakUp` by a companion parent of both within 16 who sees it; `kagune` (from `ChildKagune.fight` as its kagune comes out:
  a sibling within 12 who sees it says REACT, or TEAM fighting the same target; `Mind.siblingRemarkAt`); `afterSibling` makes a monster
  after a sibling a threat in `ChildBrain.threat`. `FamilyTests.siblings`.

# State after 1.3.50

- **1.3.50 Eto's child** (protocol 68, unchanged; the owner: "her kagune abilities and transformation at low health", and "never a
  kakuja": its transformation is her Mutated Arm, never the Owl; the owner was told and may ask for the Owl as an exception). Pure
  `ChildKaguneRules.Act` gained SWIPE, FLESH_GRAB, HAMMER, ARM (appended; never sent), `chooseFlesh(distance, sees, scale, small, health,
  now, swipeReady, grabReady, hammerReady, armReady, roll)` (ARM under `ARM_BELOW` 0.4 health within 14 x scale; close: HAMMER 35% or
  SWIPE; further: FLESH_GRAB; else CLOSE_IN), constants (SWIPE_EVERY 50, HAMMER_EVERY 170, ARM_EVERY 320, ARM_HELD 0.35, shares,
  SWIPE_REACH 4 / GRAB_RANGE 8 before scale). Server `ChildFlesh` (her `EtoFlesh` rake, grab (REACH, HOLD, THROW on `PaleRules.throwAt`,
  MISS) and club (HAMMER_CHARGE, HAMMER, shockwave) at `ChildKaguneRules.scale`, hits through `ChildKagune.fair`; told by
  `S2CPaleState` under the child's UUID; `Fight.flesh`) and `ChildArm` (a port of `EtoArm` at its size, `ArmRules`, `S2CPaleArm` under
  its UUID; power = its blow). `ChildKagune.has` = any kagune; `fight` runs the flesh first for an OWL child (`flesh()`), falling back to
  a father's tentacle/wing; `tick` ticks the arm, lets go and draws it back out of a fight; StartTracking sends both. Client:
  `PaleClient.get(UUID)`; `PaleFleshRenderer` draws children's flesh and arm (`flesh`/`arm` with the child's scale, `drawn` null: the
  move alone poses them); `ChildRenderer` hides the right arm and sleeve while `ArmClient.out(child)`. `FamilyTests.etoChild`.

# State after 1.3.49

- **1.3.49 parenting** (protocol 68, unchanged): pure `family/Parenting` (`Why` WANDERED, NIGHT, RAMPAGE, TRANSFORM, HOME; `strays(stage,
  night)` blocks past the home's edge: toddler 4/0, child 16/2, teen 40/24; `strayed(...)` never a follower, a carried child or a baby;
  `rampage(stage, fought, beyond, health)`: past `fightsFor` (600, a teen 1200), 20 (36) blocks out, or under 0.3 (0.25) health;
  `notices` (in sight within 40, or missed after 600); AGAIN_AFTER 1200, GIVES_UP 2400, CALMED 1200, REMARKS_EVERY 3000, REACHES 2.6;
  `pause(text)`; `talk(why, persona, stage, temper, name, call, seed)` -> parent/child lines: open, reply, follow, close, final (a word
  about its kagune: open, reply, maybe final; at the door: open, reply)). `family/ParentingLines` (579 lines in 240 keys
  "part:who:why:group", who = a persona key or a temper's word, group little/child/teen; a missing "little" falls back to "child";
  written from a script's data, edit the Java directly). Server `ParentDuty`: `EtoBrain.Mind.duty`; `tick` from `EtoBrain.calm` right
  after a talk holds her (before `together`, chores, `ParentCare.tend`; never during her own fight, a carried toddler put down if one
  starts): `look` (her children by `Children.of(level, "npc:<key>")`, bodies via `UniqueNpcs` "child:<id>", within 96; RAMPAGE first,
  noticed within 64), walk (1.4 for a rampage, else 1.1), reached: the child's `calmedUntil`, `fetchedAt`, the talk queued (`queue`:
  `ChildBrain.Mind.talk`, `talkFree`), the child stands still for it (`stillBy`/`stillUntil`), a toddler picked up (`cradling`); then
  home to the house's door (`escort` for an older child, `led`), at the door put down and the HOME talk; `wakes` (HomeLife wakes her for a
  child out in the night 300+ ticks or rampaging); `transformed` (from `ChildKagune.fight` when its kagune first comes out: a parent within
  24 who sees it); `talk` (ChildBrain each tick: due lines as `S2CNpcSay` to players within 24 the speaker sees). `ChildBrain`: `led`
  before its threat (its fight off while led), `calmedUntil` gates fighting, `outsideSince` kept every 20 ticks. `FamilyTests.parenting`.

# State after 1.3.48

- **1.3.48 the kit** (protocol 68, unchanged): pure `ChildKaguneRules.Act` (NONE, LASH, SHOOT, FAN, GRAB, BACK_FLIP, FRONT_FLIP, CLOSE_IN;
  no kakuja), `choose(bits, distance, sees, tailReach, crystal, small, onGround, now, flipReady, grabReady, fanReady, roll)`, constants
  (CRYSTAL_AFTER 20, FAN_SPIKES 5 / FAN_SPREAD 32 / FAN_EVERY 160, SPIKE_SHARE 1.2, GRAB_EVERY 220 / GRAB_CATCH 8 / GRAB_HOLD 26 /
  SQUEEZE_EVERY 8, GRAB_WIDEST 1.0 / GRAB_TALLEST 2.2, FLIP_EVERY 90, BACK_FLIP_INSIDE 1.7, FRONT_FLIP 6..11), `grabbable`, `fan(i)`,
  `throwsIt(roll)`. Server `ChildKagune`: crystal (S2CUkakuFx.CRYSTALLIZE, then `fire(..., spike, kind, spread)` sends SPIKE + kind /
  SPIKE_IMPACT / SPIKE_FIZZLE; a spike stabs, no fire), `grabbing` (HUMAN_PUPPET: caught on GRAB_CATCH within the skewer's reach x scale
  + 0.6, `carry` to `holdPoint` + `PaleStun.stun`, `squeeze` every 8; then PUPPET_THROW (flung at tick 5) or LASH_5 (lifted, crushed
  down on the slam's land tick with a SHOCKWAVE); `end`), `flip` (`PaleRules.Flip` launch at its size; `Fight.flip/flipAt` into
  `S2CGhoulNpc`). Its prey in the message is what it holds while it holds. Children with wings also send `S2CNpcWings` (`wings()`, on
  StartTracking too). Client: `WingRules.child(..., crystal)` (crystal from `NpcWingsClient`), `GraftRenderer.drawTentacle` winds a
  holding move round the held body (`GraftRig.Input` held/heldRadius), `ChildRenderer.posed` returns `GhoulNpcClient.flip` out of a
  moment. Preview row (grab, crystal). `FamilyTests.kit`.

# State after 1.3.47

- **1.3.47 child health** (protocol 68, unchanged; the owner: "father max mastery, mother 360 HP: the kid 400"): pure
  `ChildRules.grownHealth(motherHealth, motherMight, fatherHealth, fatherMight)` = the stronger's health x (1 + `PARENT_BONUS` 0.14 x
  the other's might), `share(stage)` (0.25 / 0.4 / 0.65 / 1), `health(stage, might, grown)` (never under 1.3.21's), `HEAL_AFTER` 100 and
  `healing(most, ghoul)` (a ghoul's whole in 120 s, a human's 600 s). `Children.Child.vitality` (saved as the 12th field; -1 unknown),
  `Children.vitality(level, child)` (worked out once every player parent is online: a companion's `Persona.health()`, a player's
  `m_21233_`, 20 if away) and `Children.health(level, key)`. `ChildBrain`: a stage's most from it, the first tick keeps the share of
  health it had; it heals every second once unhurt for `HEAL_AFTER`; a child whose vitality turns known re-reads its most. `/kagune
  children` shows health / most (grows into). `FamilyTests.vitality`.

# State after 1.3.46

- **1.3.46 layouts** (protocol 68, unchanged; the owner: "add other variations, like two wings and the tentacle in the middle... stays
  in the back like a tail"): pure `ChildKaguneRules.Layout` (SHOULDER_RIGHT side 1, SHOULDER_LEFT -1, TAIL 0), `layout(bits, seed)`
  (tentacle children only; shares `RIGHT_SHARE` 0.4, `TAIL_SHARE` 0.35, the rest left; a mix of the seed so old children get one),
  `wingCount(bits, seed)` (tentacle + wing: TAIL 2, a shoulder 1; else `KaguneGenes.wings`), `wingSide` (the other side of a shoulder
  tentacle), `tentacleRoot(layout)` (x mirrored; 0 for the tail), `tentacleRest(..., layout)` (the tail: out behind at waist height,
  curling up, a wider side-to-side sway), `describe(bits, seed)` (`/kagune children`). Used by `ChildKaguneRules.side` (the server's
  shots), `GraftRenderer.drawTentacle` (rest and the lash's root), `UkakuWingRenderer`'s child loop (count and side). Preview:
  `tools/ChildKagunePreview.java` (the three layouts, the tail's lashes). `FamilyTests.kagune`.

# State after 1.3.45

- **1.3.45** (protocol 68, unchanged; the owner: the back was crowded, lean the tail to the right): `ChildKaguneRules.TENTACLE_ROOT` x
  0.13 (its right), `SEED_FAN` 0.95 and `LEAN` 0.45 (the first control pulled out to the right: up over the right shoulder);
  `ChildKaguneRules.wingSide(bits, seed)`: with a tentacle the one wing is always on its left (-1), else `KaguneGenes.side(seed)`; used
  by `ChildKaguneRules.side` (the server's shots) and `UkakuWingRenderer`'s child loop. Lashes carry the move's path onto the new root
  (`GraftRenderer.drawTentacle`, unchanged). Preview: `tools/ChildKagunePreview.java` (Touka moved further left). `FamilyTests.kagune`.

# State after 1.3.44

- **1.3.44 fix** (protocol 68, unchanged; the owner: "it's meant to be one tentacle not two", and the 1.3.43 tails were the dormant Graft
  DNA's toothed tails): `ChildKaguneRules.tailMastery` is 0 / 60 (under `GraftStage.TWIN_MASTERY`: one limb on the server's string and
  the client's), `TENTACLES` 1, `TENTACLE_THICKNESS` 0.18 (a Rinkaku player's first tentacle: `KaguneProfile.tendrilBaseThickness` at
  mastery 0), `TENTACLE_ROOT` (the pelvis, as `TendrilChain.pelvisAnchor`), `tentacleRest(length, time, seed, points)` (the original
  seed tentacle's rest cubic, `KaguneForm.RINKAKU_SEED`'s fan 0.15 / height 0.44 / back 0.62, drawn out about its root to `REST_SHARE`
  0.85 of the lash length, swaying by `SEED_WIGGLE`). `GraftRenderer.drawTentacle` (child loop): rest pose or the GraftRig lash (one
  limb) carried onto the pelvis, `GraftMotion.Limb` springs, 28 points evenly along the grown part, then the original's tube
  `KaguneWorldRenderer.emitTubeLegacy` with a `KaguneClientState.View` built for the child (RINKAKU_SEED, its colour,
  `KaguneColours.veinOf`, thickness 0.18 x scale, styles as the original's EMPTY view) and a `TendrilChain(0)` per child
  (`TENTACLES` map). `KaguneColours.veinOf` (new): DEFAULT/BLACK/BONE their own veins, else the roll's rule (hue kept, s + 0.1, v x 0.17,
  at least 0.06). Preview: `tools/ChildKagunePreview.java` (rest, three lashes; the GraftPreview `child` sheet is gone). `FamilyTests.kagune`.

# State after 1.3.43

- **1.3.43 kagune inheritance, part 1** (protocol **68**: S2CChild's genome string has nine parts; children also send `S2CGhoulNpc`).
  Pure `family/KaguneGenes` (bits TAILS 1, WING 2, PAIR 4 = the second wing, OWL 8; UNKNOWN -1 for a child from before; `inherit`: same
  kind whole, one parent none -> the other's, two kinds -> the union with PAIR dropped when a wing meets tails/owl; `ofPath`, `side(seed)`,
  `colour(...)` one parent who has a kagune, `describe`). `Genome` gained `kagune` and `colour` (a 7-arg constructor and 7-part saves read
  as UNKNOWN, -1; `withKagune`; drawn after the seed in `Genome.of`, so a child of the same night is otherwise unchanged); `Genome.Parent`
  gained `kagune`/`colour` (5-arg = none). `Persona.kagune()` (Eto OWL, Touka WINGS, Ken TAILS) and `kaguneColour()` (Eto 0xD8CCBC,
  else `KaguneColours.DEFAULT`); `Looks.parent` passes the player's path (`Looks.kagune`) and `KaguneAppearance.primaryColor()`
  (`Looks.kaguneColour`); `EtoServer.conceive` passes the companion's. Old children: `Children.genome(level, child)` works it out once
  every player parent is online (ChildBrain checks every 100 ticks; `/kagune children` too) and saves it.
- **The fight:** pure `family/ChildKaguneRules` (scale = stage.draw; `tailMastery`/`wingMastery` by stage only (100/200, 110/220: the
  client draws from the stage), `damage` = 0.45 x the punch, `string`, `use(bits, distance, sees, tailReach)` TAILS / WINGS / CLOSE_IN /
  NONE, `side`, `pause`). Server `ChildKagune` (`Fight` in `ChildBrain.Mind.kagune`): `fight` (from `ChildBrain.fight` when `has(genome)`;
  the GraftMove string with beats at child scale: reach x scale, chest 1.35 x scale; shots from `tip` at the wing mastery x scale, bolts
  stepped in `tick`), `fair` (the threat or `EtoBrain.hostile`; never Player/EtoNpc/ChildNpc), `message` (an `S2CGhoulNpc` under the
  child's id: identity RINKAKU if tails else UKAKU, seed from the genome), `fizzleAll` on death; sent on StartTracking too.
- **Drawing:** `ChildRenderer.drawn()` (children with a kagune, cleared from `GhoulNpcRenderer.Frame`); `GraftRenderer.Frame` has a
  `scale` (place/rig scale about the feet; widths and the heave blob scaled) and a child loop (tails in the genome's colour, tailMastery);
  `UkakuWingRenderer` child loop (`WingRules.child`: growRight/growLeft for one wing on `KaguneGenes.side(seed)`, carry = size);
  `KaguneTints.of(ChildNpc)` = the genome's colour (its shots too). Preview (1.3.44: replaced): `tools/ChildKagunePreview.java`. Tests: `FamilyTests.kagune`.

# State after 1.3.42

- **1.3.42 the partner cooks the cookbook's dishes** (protocol 67, unchanged; no new Minecraft API). Pure, in `family/Kitchen`:
  `cooking(dish, stock, searable)` -> `Cooking(dish, steps, uses, yields, seared)` or null (each ingredient from what it made on the
  way, then the stock, else made first from its own recipe, `PREP_DEPTH` 2; a cooking recipe counts one `seared`, at most `searable`);
  `remainder(id)` (water/milk/lava bucket -> bucket, honey/dragon's breath -> glass bottle, a DRINK dish with a container -> it, as
  `ControlItems` gives it `craftRemainder`); `cooks(dish, ghoul)` (ghoul spouses only; effects or a cooking recipe: never the mince
  alone); `worth` (100 per effect + nutrition + seconds x level / 10); `choose(stock, searable, ghoul, last, emptiedLast, roll)` (the
  best three by worth weighted 3/2/1, the last dish dropped while another exists, and with `emptiedLast` no dish whose `empties(stock)`
  takes the last of something another recipe needs); `passes(...)` (that filter left nothing but something was possible: the meal's
  turn passes, the flag clears); `dishDue(today, lastAt, now)` (`DISHES_A_DAY` 4, `DISH_GAP` 2400); lines `made` (kept/in the chest),
  `servedDish` (+ "more in the chest"), `madeWhileOut`. `Kitchen.name` gives a dish its real name (`Dishes.name`, from the new
  generated `Menu.NAMES`: `tools/kitchen.py` writes it; `Dishes.byItem("hexkagune:x")`).
- **World side:** `HomeKitchen.dinner` (ghoul spouse, chests, `dishDue`; searable from the nearest campfire, else stove fuel / 2;
  where: the searing station if something is seared, else the nearest stove, a table, or the chest) -> `HomeChores.begin` counts COOK
  if `plan.cookAt` or a dinner exists and prefers the dinner (`Job.dish`); `HomeChores.effect` COOK with `job.dish` calls
  `HomeKitchen.cookDish` (re-reads the stock; the planned dish or a fresh choice; takes every `uses` entry, any short -> all given
  back; fuel re-checked after taking; `burn`; the dish into the meal (`Kitchen.add`, MEAL 6) and the rest + prep + remainders
  `giveBack` as whole stacks (`stacks`); saved `HexDishLast/Emptied/Day/Count/At`; `HexDishNews` when stored with the spouse away).
  With the spouse in sight: `HomeKitchen.hand` (the meal handed over) and `Kitchen.made`. `serve` (the welcome) now prefers a dish in
  the meal for its line (`servedDish`), else `madeWhileOut` from the news. COOK smoke/flame particles only off a real station.
  `/kagune home` lists the dishes the chests could make and today's count. Tests: `KitchenTests.homeCooking`.

# State after 1.3.41

- **Next session:** read `docs/handoff/next-ai-1.3.41.md` first (the plan for what's left of Home Living and the house rules).
- **1.3.41 cookbook** (protocol 67, unchanged): item `hexkagune:ghoul_cookbook` (`ControlItems.COOKBOOK`, stack 1, Food tab).
  Pure `kitchen/Cookbook` (PRICE 2 cocoa beans, REGARD 20, `spreads()` = cover, per chapter a CHAPTER spread then its DISH spreads,
  EFFECTS last; `chapterAt`, `dishAt`, `pageNumber`, `affordable`, `wrap`; Touka's `ASK`/`price`/`given` lines). Touka: `EtoServer.
  withCookbook` adds SAY_6 "cookbook" to her MAIN page (before Leave/Insult/Tease) at regard >= 20; the "cookbook" key takes 2 cocoa
  beans from the main hand (not in creative) and gives the book (`addItem` or drop). Client: `CookbookClient` (RightClickItem with the
  book in the local player's main hand opens `CookbookScreen`; tooltip line); `CookbookScreen` draws `textures/gui/cookbook.png`
  (512x256: the 300x196 spread at 0,0; sprites listed in its constants and `tools/cookbook_art.py` SPRITES; 7 ribbons at x 412 + 13k)
  and `textures/gui/cookbook/<cover|chapter|backdrop>.png` (48x48 pictures drawn at 2x inside the frame sprite), items by
  `GuiGraphics.m_280480_` + `m_280370_` (count) + `m_280153_` (tooltip), mouse wheel `m_6050_` (all newly allowlisted [srg] with
  stubs). Layout was checked offline for every dish with a font-width mock (longest right page: Anteiku Blend, ends ~157, page
  numbers at 172); the screen itself is not seen in game. Art: `python3 tools/cookbook_art.py [preview-dir]`. Chapter intros and
  the book's words come from `tools/kitchen.py` (CHAPTERS now `(key, name, intro)`, TEXT). `KitchenTests.cookbook`.

# State after 1.3.40

- **Then (written after 1.3.40):** Home Living continues: **1.3.41** Touka's cookbook (a friendship-gated talk option with her, traded for two cocoa
  beans) and its illustrated book screen (Alex's Caves-style: pictures of each dish, its recipe and what it does; the chapters and
  every line are already in `Menu`/lang: `book.hexkagune.cookbook.chapter.*`, `item.hexkagune.<id>.about`,
  `effect.hexkagune.<key>.description`); **1.3.42** the partner cooking `Menu` recipes from the home's chests (HomeKitchen, the COOK
  gesture, instant); then children inheriting their parents' kagune (colour from one; Rinkaku + Ukaku = tails and one wing; Eto's
  child her kakuja at low health). See also `docs/handoff/next-ai-1.3.39.md`.
- **1.3.40 kitchen** (protocol 67, unchanged): `tools/kitchen.py` is the single source (its MENU table: name, chapter, recipe,
  servings, food, effects, kind, container, line) and writes the icons (`tools/kitchen_art.py`, 16x16 pixel art; preview with
  `--preview <dir>`, a sheet at 6x), item models, `data/hexkagune/recipes/kitchen/*` (shapeless; the two meats' smelting/smoking/
  campfire), tags `dishes` (in `ghoul_food`), `contains_ghoul_meat`, `contains_human_meat`, lang, the effects' 18x18 icons, and the
  generated `control/kitchen/Menu.java` (do not hand-edit). Ghoul pieces per serving and human meat are worked out from recipes
  (ghoul meat, seared ghoul meat, RC broth = 1; recipes with ghoul meat make 1). `Dish` (record), `Dishes` (lookups + the effects'
  numbers), `DishItem` (overrides `m_6164_` getUseAnimation for drinks, `m_8105_` getUseDuration 16 for snacks), `KitchenEffect`
  (a BENEFICIAL `MobEffect`, does nothing itself). `ControlItems` registers every dish (properties: food, meat if meat, always-edible
  for drinks/snacks, stack 16 with a container, Bottled Blood `m_41495_` craftRemainder glass bottle) and the effects
  (`ForgeRegistries.MOB_EFFECTS`); `ControlItems.effect(key)` resolves the kitchen's own or vanilla's by path. `server/KitchenServer`:
  Finish event: `GhoulMeatServer.devour(player, pieces)` (a human is sick and gets nothing), effects, container back via
  `Finish.setResultStack` or `addItem`/drop; player tick: Clear Sight strips blindness/darkness/nausea, Replenished heals (free),
  Refreshed +0.25 RC/s/level via `SharedBar`, Content `ControlServer.calm`; `QuestServer.bond` scales regard gains by
  `KitchenServer.warmth` (x1.5 while Content). `client/KitchenClient`: tooltip lines, `ViewportEvent.RenderFog` x3 far plane under
  water/lava/snow (cancelled to apply).
- **Growth and mutants:** `GhoulMeat.growth = ln(1 + n/20)`, strength `1 + 0.12 g`, endurance `1 + 0.25 g` (endless; the old
  `share`/`*_MAX` are gone). `Mutant` (STRENGTH 1.25, ENDURANCE 1.6, FEED 1.5): `GhoulMeatServer.mutant` = unlocked and
  `KakuganServer.granted` (Eto made them one-eyed); strength/endurance multiply in; `GhoulDiet` feeds `Mutant.feed(nutrition)`.
- **The Human:** `npc/HumanRules` (pure), `npc/HumanNpc` (CREATURE, never despawns, LOOK/SCARED synced, look kept in persistent
  data), `server/HumanBrain` (wanders; flees the nearest wild ghoul, ghoul player with kagune out within 16 / hidden within 6, or
  attacker for 30 s, re-planning every 10 ticks, turning round obstacles; drops 1-3 + looting Human Meat), client
  `render/HumanRenderer` (vanilla default skins, wide/slim; arms up and flailing while scared). `/summon hexkagune:human`.
- **Structures:** `has_structure`/`prefers` tags: anteiku and cyber_tower plains + sunflower plains; library plains + forests. The
  three structure sets are `count` 3, `spread` 3 (concentric rings search 112 blocks for the preferred biome; a spot with none fails,
  so three spots instead of one; `UniqueNpcs` sends extra bodies away). `KitchenTests` covers the table, files, rules and Human;
  `QuinqueTests` now walks `recipes/` recursively (the kitchen's recipes live in `recipes/kitchen/`).

# State after 1.3.39

- **Next session:** read `docs/handoff/next-ai-1.3.39.md` first. The owner's next big update is **Home Living** (Touka's ghoul
  cookbook, the Human NPC and human meat, many recipes with their own icons and effects, the partner cooking them, mutants' bonuses,
  endless ghoul-meat growth, children inheriting kagune, structures by biome): see the session's chat and the task list.
- **1.3.39 fixes** (protocol 67, unchanged): `EtoBrain.calm` extends `talkingUntil` while `EtoServer.talkingWith(player, npc)` (the
  talk is open on their screen, within 10) and `EtoBrain.busy` includes talking (the stroll goal waits); `ChildBrain.tick` stands a
  child still facing `ChildTalk.talker(npc, level)`; `GhoulNpcBrain.talking` extends `talkUntil` while `GhoulNpcTalk.talkingWith`.
  Holding: `EtoBrain.holdHands` always places her at the hand spot with `noPhysics` (`f_19794_`, newly allowlisted [srg]) and no
  gravity; `EtoBrain.solid(body, by)` restores them (and sets a body caught in a wall on `by`'s feet), called from `letGo`, `calm` (a
  hold dropped otherwise) and `ChildBrain.putDown`; `ChildBrain.carry` sets `noPhysics` for every ride. `GhoulNpcBrain.wanted` checks
  grudges before the creative exclusion (a ghoul hit in creative fights back, so its kagune shows). `FamilyTests.held`.

# State after 1.3.38

- **Next session:** read `docs/handoff/next-ai-1.3.39.md` first (where things stand after this session, what's left).
- **1.3.38 speech bubbles** (protocol 67, unchanged): pure `npc/BubbleShape` (text pixels, y down, the point at y 0: a speech bubble's
  rounded box and tail, a thought cloud's puffs and trail; outline at `OUTLINE_DEPTH` behind the body at `BODY_DEPTH`, words at 0;
  `thought(lines)` = every line styled as an action; `ink` black / dark grey italic; `pop(age)`; `zees(phase)`). Client
  `render/SpeechBubbles.draw/snore`: turned to the camera as a name tag is (Ry toward it, Rx by its height, scale(-0.025, -0.025,
  0.025)), quads in `RenderType.m_110473_` on white concrete, full bright, normal (0,-1,0) (world up: full diffuse), the words by
  `Font.m_271703_` (drawInBatch(String...)) with `Font$DisplayMode.f_181350_` (NORMAL) - both newly allowlisted [srg] with stubs.
  Called from EtoRenderer.bubble (snore when asleep with nothing said), ChildRenderer (same), GhoulNpcRenderer. `NpcSpeech` keeps
  each bubble's `from` (`age`). `NpcTests.bubbles`; `tools/BubblePreview.java <dir>` draws them off-game.

# State after 1.3.37

- **Next session:** read `docs/handoff/next-ai-1.3.33.md` first, then the 1.3.33 section below. Still asked, not done: real white
  speech bubbles and thought clouds instead of the name-tag bubbles (`NpcSpeech`, drawn with vanilla's `renderNameTag` in
  EtoRenderer/ChildRenderer/GhoulNpcRenderer; Font.drawInBatch(String...) is SRG `m_271703_`, Font$DisplayMode.NORMAL `f_181350_`).
- **1.3.37 sleeping family** (protocol 67, unchanged): pure `family/Drowsy` (each spouse's half-woken words, settle, kissed,
  roused; a child's by stage, a baby sleeping on; settles/stroked). `EtoServer.open`: a companion asleep in her bed
  (`m_5803_` and `mind.bed`) gets `drowsy(...)` (SAY_1 `sleep:back`, SAY_2 `sleep:kiss` for the partner, SAY_3 `sleep:up`) and
  `mind.drowsyUntil = now + DROWSY` (600); `HomeLife.tick` only counts her talk as a reason to get up once `now >= drowsyUntil`
  (`sleep:up` clears it). `ChildTalk.asleep` shows `Drowsy.child` and keys tuck/stroke (parents) and back; `stroke`/`back` settle it
  with a bubble line. `EtoBrain.guard` also returns a `CcgAgent` in the house's box (HomeLife wakes her for it within 14).
  `FamilyTests.drowsy`.

# State after 1.3.36

- **Next session:** read `docs/handoff/next-ai-1.3.33.md` first, then the 1.3.33 section below. Still asked, not done: the family
  sleeping in their beds at home (drowsy when woken, a parent up to fight a mob or an investigator) and real white speech bubbles and
  thought clouds.
- **1.3.36 arm blade** (protocol **67**: S2CGhoulNpc's meaning changed): `NpcIdentity.Kind.KOUKAKU` appended (packed: the kind's second
  bit at 36; rolls 37/35/28), pure `npc/BladeArm` (timings, reach, weights, chain, plate/blade growth) and `npc/BladeArmModel`
  (quads in the right arm's frame: shoulder fin, spiral plates tilted along the helix with ridges, forearm sleeve, four-length blade
  with an edge, glowing seams marked by the `GLOW` normal object, point, root fins). Server `GhoulNpcCombat.slash/bladeBegin/
  bladeLand/bladeMet` (Mind `blade*` fields; KaguneClash both ways; `maneuver` treats it as a close fighter and may counter with a
  cut); `GhoulNpcBrain.message` sends its cut in `move`/`moveAt`. Client `GhoulNpcClient.View.raw` (the move int), `wrap`, `blade`
  (cut / wrap / sprint / guard); `client/animation/BladeArmPose` (absolute keys on `QuinquePose.curve`; `QuinqueGrip.reachLeftTo` puts
  the free hand on the forearm); `GhoulModel` now applies poses through `RigPose.transform` (as `AgentModel`); `GhoulNpcRenderer`
  applies lunge/drop/spin and draws `BladeArmLayer` (white concrete, `RenderType.m_110473_`, colour `KaguneTint.wild(seed)`).
  `/kagune npc koukaku|blade`. Preview: `tools/BladeArmPreview.java <dir> [hex colour]`. `NpcTests.blades`.

# State after 1.3.35

- **Next session:** read `docs/handoff/next-ai-1.3.33.md` first, then the 1.3.33 section below (the owner's newer asks are listed
  at its end; the colours are done).
- **1.3.35 colours on every path** (protocol 66, unchanged): pure `control/KaguneTint.recolour(argb, target)` turns a red-built
  palette to a kagune colour (hue rotated by target hue - `RED` 0.995, saturation toward the target's, brightness kept; target value
  < 0.3: black with saturated bright parts kept blood red; saturation < 0.25: ash/bone); the crimson `KaguneColours.DEFAULT` is the
  identity. `KaguneTint.wild(seed)`: a wild ghoul's colour (15% crimson). Client `KaguneTints.of(entity|id)`: a player's
  `KaguneClientState.View.color()`, a wild ghoul's `wild(seed)`, else the crimson. Applied in `UkakuWingRenderer` (both `emit`s take a
  tint; NPC wings by seed, Touka's left as hers; shots via `tinted(sink, ...)` by `bolt.shooter()`; stuck spikes by
  `UkakuFx.shooter(id)`, remembered when a shot is seen; vortices; thorns by the body), `KakujaArmorLayer` (by wearer), `GraftRenderer`
  (wild Rinkaku tails, a static `tint` around their `drawSet`). `KaguneColours.roll` widened (s 0.4..1, v 0.36..0.96).
  `tools/WingPreview.java <dir> part-tint` renders the wings in eight colours. `PathTests.tints`.

# State after 1.3.34

- **Next session:** read `docs/handoff/next-ai-1.3.33.md` first, then the 1.3.33 section below (the owner's newer asks are listed
  at its end).
- **1.3.34 colours** (protocol 66, unchanged): pure `control/KaguneColours` (`roll`: 4% near-black / 4% bone, else any hue, s
  0.55..1, v 0.42..0.92, vein the same hue at v x 0.17; `name`, `hex`, `hsv`, `toHsv`). `server/KaguneColourServer`: on login, once
  (`HexKaguneColourRolled` in persistent data), a player still on the original's `KaguneProfile.DEFAULT_COLOR` gets a roll written
  through the original's `KaguneProfile.appearance/setAppearance` (KaguneAppearance primary + vein, the rest kept, `validated()`)
  and `KaguneNetwork.broadcast`; `/kagune color` and `/kagune color reroll`. The original's `KaguneWorldRenderer.surfaceColor`
  already draws tendrils from `View.color()/veinColor()`, so nothing in rendering changed. `PathTests.colours` covers it.

# State after 1.3.33

- **Next session:** read `docs/handoff/next-ai-1.3.33.md` first (the owner's open list, how they work, the quick loop
  `tools/quick_check.py`). Section C (CCG/quinques) is done in 1.3.33; the owner's newer asks are at the end of this section.

- **1.3.33 quinques** (protocol 66, unchanged): `tools/quinque_models.py` now also solves every hold and writes
  `client/animation/QuinqueHolds.java` (generated: stance twist, right arm pitch/yaw/roll as `QuinqueGrip.aim` = Ry(yaw) Rx(pitch)
  Ry(roll) in the chest frame, a left-arm hint, the model's thirdperson_righthand, right/left grips, the left hand's slide range).
  `STANCES` in the script is the source: a two-handed one names the weapon's tip/side in the look frame and the roll is found (the
  fist's -z along the grip); the blade (`theta` 50: the blade 50 deg off the forearm's line toward the thumb, not vanilla's 90) names
  its roll. Euler decomposition handles gimbal lock (y = 90: z kept 0). `QuinqueTests.holds` checks the JSON matches, grips in fists.
- **1.3.33 left hand IK:** `QuinqueGrip.reach(p, q, slim)` (call it LAST, after anything that moves the body or right arm): the left
  grip through vanilla's item chain (`held`: translateToHand's slim +0.5 x, Rx(-90) Ry(180), (1/16, 0.125, -0.625), ItemTransform,
  -0.5), the point on the slide segment whose distance is the arm's (`slide`: spear 9..17.5, mace -7.5..1 standing y), the shoulder
  shrug giving up to 1.5 px, then the least rotation from the hint, back into RigPose terms (Ry(-twist) . unlean). Called by
  `AgentClient.pose` and `QuinqueClient.pose`. Misses at the fastest swing frame are at most ~2.8 px (sticks, no elbows).
- **1.3.33 swings:** `QuinquePose.keys` (per quinque, three steps) are offsets from the stance (pitch, yaw, roll, twist, lean, lunge,
  drop) at t 0 / 0.3 windup / 0.5 impact / 0.68 follow / 1, drawn by a Catmull-Rom curve; `step` splits the legs with the lunge
  (right foot leading for blade/katana) and drops the hips by exactly what straight legs lose. A stance's turn is shared: up to 0.4
  as body `spin` (feet turned, head turned back by `headY`), the rest chest `twist`. `recoil` / `pitchBy` turn the right arm about the
  stance's look-level axis; the launcher aims at the head's pitch. `AgentPose.parry` / `stanceRun` (chest half turned, lowered) /
  the run's BLADE carry (blade trailed level behind) are aim-based. `AgentClient.take` now carries spin, head and step legs.
- **1.3.33 preview:** `tools/QuinquePreview.java <dir> [kind]` writes `quinque-KIND-stance.png` and `-m1..m3.png` (300 px cells, camera
  at 5 blocks) with the left grip (magenta) and left fist (yellow) marked and each frame's miss printed. It reads element rotations.
- **1.3.33 launcher:** built lying along x (`launcher_parts`, `GUNS`, `gun_telescoped` around `GUN_GRIP`), elements with their own
  ±22.5/45 turns; scale 0.62; first person ([0,100,0], [-2,3,-5], 0.6) lower right; gui [15,-25,0]. `Quinque.SHOT_EVERY` 2: each
  RightClickItem fires now and queues a second shard (`QuinqueServer.SECOND`, fired from the player tick 2 later if still held, not
  sneaking); SHARD_DAMAGE 2.0, SPEED 2.8, HEAT_MOST 48. Client kick per shard (`QuinqueClient.kick`, `QuinquePose.KICK_TICKS` 3).
  Investigators: `AgentRules.BURST` 8, `SHOT_GAP` = SHOT_EVERY.
- **1.3.33 fold:** `QuinqueServer.use`: `folds` = melee weapon on any right click, launcher on Shift; `tapped` ignores vanilla's held
  repeat (within `HELD_REPEAT` 5 ticks of the last), so holding the button never flips it back. Lang `quinque.close` updated.
- **1.3.33 dashes:** `AgentBrain.maneuver`: side dashes between blows / between bursts (`AgentRules.SIDE_DASH` 0.05 a tick once
  `DASH_EVERY` allows), a dash in on prey just out of reach (`DASH_IN`), both only where `clear` finds room (feet and head open 1.5
  and 3 along, no drop of 2+). Afterimages: `AgentClient.afterimages` (called from `AgentRenderer`) adds one per tick of the push
  to `PaleAfterimages` (now takes colour, life, alpha and size per ghost: 0xC8D6E6, 6 ticks, 0.34).
- **Owner's newer asks, not done yet (in this order after the CCG work):** random Rinkaku colour for the Ken path at spawn + a reroll
  command; wild NPC-only kagune types (an arm-blade kagune that wraps the arm into a sword: M1s, the wrap animation, its run); the
  spouse and children sleeping in their beds at home at night (drowsy when woken: "what is it?"; a parent gets up to fight a mob or an
  investigator near the house); real white speech bubbles and thought clouds instead of the name-tag bubbles.

# START HERE (state after 1.3.32)

- **Next session:** read `docs/handoff/next-ai-1.3.33.md` first (the owner's open list, how they work, the quick loop
  `tools/quick_check.py`).

- **1.3.32 fixes** (protocol 66): `EtoRenderer`/`AgentRenderer` turn `RigPose.spin` with `rotateAxis(+spin)` about +Y, as PlayerAnimator
  does (+ to their left); it was mirrored (the NPC airplane spun against the child; `FamilyTests.airplane` pins it). `ChildBrain.tick`
  re-reads `mind.carrier` after `moments` (a moment ending in `putDown` was carried again from the stale local, `setNoGravity(true)`
  forever) and sets gravity on whenever it isn't carried. Hand in hand with a player (`RIDE_HAND`), a Shift tap (`Mind.holdShift` edge,
  primed true) puts it down. `HomeChores` never teleports: a stuck chore walk skips the block, `unstick` only stops the path;
  `EtoBrain`'s lost-teleport only when `watched` is false (no player within 32).

# State after 1.3.31

- **1.3.31 awareness:** `SceneServer.sees(body, player, near)` (same level, within `near`, `hasLineOfSight`) gates every approach (companion,
  child, ally: `SEES` 12), `EtoBrain.say` and `ChildBrain.say` (16), `askForKiss`, and all of `EtoBrain.together`'s initiatives. The
  welcome is deferred: `Mind.welcomeFrom` set when the partner comes back after 6000 ticks, greeted (and served the meal) in `nearness` once
  `sees(12)` within 1200 ticks. Client `NpcSpeech.lines(LivingEntity)` draws a bubble only if the local player is in its line of sight.
- **1.3.31 home help:** pure rules `family/Kitchen` (cooked/smelted/flesh/fuel in halves, PER_ITEM 2/coal 16; station rules; `ripe` from the
  state text; `harvest` yields; the meal: `save`/`load`/`add` capped at MEAL 6; lines `served`/`noFuel`). World side `server/HomeKitchen`:
  `plan` (chest + station for cooking: a campfire free, a furnace/smoker needs fuel; a ghoul partner: raw flesh set aside at a table),
  `cook`/`smelt`/`harvest` take from and give back to the home's sortable chests in one tick (`take`, `trim` to the fuel left, `burn` small
  fuel first, `giveBack` drops leftovers at her feet via spawnAtLocation), the meal in persistent data `HexMeal` (the partner's diet in
  `HexMealGhoul`), `serve` into the inventory (or at the feet). `HomeChores.Job` now has per-step `expects`/`doings` (COOK: chest then
  stove; HARVEST: crops then the chest); new `Chores.Kind` COOK (gesture `EtoGesture.COOK`, appended: bowl + wooden shovel, a stirring
  pose), SMELT, HARVEST (TIDY). At home `calm` runs `HomeChores` before `ParentCare.tend` and drops a held child when a chore is due.
- **1.3.31 guarding:** `EtoBrain.guard` (household hostiles in sight, within 10 of the spot, or targeting the partner/a child/her) feeds
  `seek`; `HomeLife.tick` wakes a sleeping spouse when `guard` finds one within 14.
- **1.3.31 `/kagune home`** (`ControlCommands`, `HomeChores.status`): what the spouse found, can do, is doing, and the meal kept.
- New allowlist: ParticleTypes SMOKE (`Z` f_123762_) and SMALL_FLAME (`aD` f_175834_), [order] evidence.
- **Still asked, not done:** the CCG/quinque work (launcher model and rifle hold, two-handed IK, right-click fold, fire rate, agent side
  dashes with afterimages). The owner asked to leave it for last, after the companions feel real.

# State after 1.3.30

- **1.3.30 speech bubbles** (protocol **66**): `S2CNpcSay` (id 47: entity id + text) replaces the chat for `EtoBrain.say`/`bark`,
  `ChildBrain.say` (now takes the ChildNpc), ally lines in `SceneServer` and the reactions in `Sightings`. Client `NpcSpeech` keeps one
  bubble per entity (newest wins; `npc/Speech`: wrap to 34 chars x 4 lines, 60..220 ticks, actions grey italic); `EtoRenderer`,
  `ChildRenderer` and `GhoulNpcRenderer` draw it with vanilla's `renderNameTag` (m_7649_), a line per 0.25 block, above the tag and name.
  System notices stay in chat.
- **1.3.30 spouse at home:** `EtoBrain.together` never trails the partner at a household and starts nothing while a chore runs;
  `Chores.gap` 300..700 ticks alone, 400..900 with them near.
- **1.3.30 fixes:** Touka's KISS is a lean only (arms loose, no hold); BLUSH raises her one hand over 8 ticks
  (`EtoPose.toukaKissed`, the lean kept at 0.5 so the KISS->BLUSH->PULL switches never drop it); PULL blends from that hand. Closed eyes:
  `tools/companion_textures.py` keeps the skin's lashes, paints the eye's first row under them in skin and draws one black line a pixel
  lower (where the owner marked it); only `touka_closed.png` / `ken_closed.png` changed (the skins are byte-identical). Cradling: `EtoBrain.tick`
  never ends PET while `mind.cradling` is set; `EtoRenderer.pose` uses `EtoPose.cradle` while `ChildClient.carrying(npc)` (full, easing
  in over 5 ticks of PET). `CompanionTests` pins all three.
- **Next (asked by the owner, not done yet):** the crystal launcher rebuilt as a proper gun (less boxy) with a rifle hold (stock at the
  shoulder, both hands on it); two-handed quinque holds and combos keeping both hands on the grip (the left arm copies the right's
  rotation now, so the hands drift apart); a tap of right click folding a melee quinque back into its case; the launcher firing faster;
  investigators dashing left and right with brief afterimages. `tools/QuinquePreview.java` (new) renders each quinque's real model in
  the hand through vanilla's item chain on a posed body: use it to judge stances and swings.

# State after 1.3.29

Branch `claude/relaxed-carson-4y0frw` at **1.3.29**, **protocol 65** (64: S2CNpcApproach and ANSWER; 65: S2CQuests carries the romance
cards). Everything below is compiled and unit tested, **never tried in game**.

- **Scenes (1.3.29):** `npc/scene/SceneBook` reads `src/main/resources/data/hexkagune/talk/{eto,touka,ken,child,ally}.txt` (form at the top
  of `SceneBook.java`, words in `SceneWords`). `server/SceneServer` walks partners, friends, children and allies up (`S2CNpcApproach`, id 46,
  card `client/NpcApproachOverlay`); the Answer key (`ControlKeys.ANSWER`, O) is sent as `NpcTalk.Choice.ANSWER`. `SceneTests` checks every
  script and coverage. `family/ChildTemper`: a child's temperament from its id.
- **Touka (1.3.29):** `Persona.style()` 2, `EtoPose.touka(...)`, `Persona.clings()` false, `Persona.unasked()` (PECK or `EtoGesture.STEAL`,
  `EtoClient.pull` turns the view, `ToukaScript.stolen`). One wing: `WingRules.MASTERY` 170, `SIDE` right.
- **Chores (1.3.29):** pure rules `family/Chores` (what a block is for by registry name: TIDY chest/barrel, WIPE `*_table`, WATER flowers,
  crops, saplings, `potted_*`, READ bookshelf/lectern, LOOK glass panes; `pick` weighs by what the home has; `sort`/`sorted` plan a chest;
  each companion's lines; `goingHome`). The world side is `server/HomeChores`: `found` scans the home (radius 20, -5..+7) every 1200 ticks
  (nearest 96 of each kind, plus furniture for floor spots); `tick` is called from `EtoBrain.calm` after `ParentCare.tend` (household, by
  day). A `Job` is a round of blocks: walk (`m_26519_` every 10 ticks; 3 s without 0.6 blocks progress = repath; 3 repaths = put beside
  the block via `besideBlock`; no path 3 times or 15 s = skip), then the gesture (`Kind.gesture`, all `still()`), then `effect`: `sort` (real
  Container sort: key = item id + NBT text, `ItemStack.isSameItemSameTags` double-checked, `Chores.sort` keeps totals; trapped chests are not
  sorted), `sweep` (ItemEntities 10 s+ old, not in water, into the nearest sortable chest whole or not at all: `insert`; the round then
  ends at that chest, `Job.box`), WATER (one `randomTick` on growables in 3x3x3, 50% each). Chest lids: `Level.blockEvent(pos, block, 1, 1/0)`,
  vanilla chests only; `shut` on finish, `stop` and death. `EtoBrain.tick` stops a chore for any fight/talk/kiss/hand/child/night and runs
  `HomeChores.unstick` for any walk at home (4 stuck windows: back to the home's spot, never with a player within 2.5). `EtoNpc`'s
  RandomStrollGoal is a subclass whose canUse/canContinueToUse wait while `EtoBrain.busy`. Mind fields: `chore`, `nextChore`, `lastChore`,
  `choreSaid`, `noSweepUntil` (6000 ticks after a sweep that could not put anything away), `stuck*`.
- **"Go home, please." (1.3.29):** `EtoServer.withHome` adds SAY_7 (key `gohome`) after FOLLOW/STAY on MAIN pages when `going(...)` is
  non-null (following, or settled > 6 blocks from where it belongs; a spouse's target is the household, only in its own dimension; others
  only in the overworld). `EtoBrain.sendHome` lets go of everything and calls `homeAt` (spouse) or `EtoNpc.sendHome` (home = origin, no
  teleport); the lost check (`EtoBrain.tick`) waits while someone talks to her, so from far off she is put home once the talk ends.
- **Quests (1.3.29):** `QuestBook.Giver` (PATH, FEAT, ETO, TEACHER, GHOULS, ASKED, RETIRED) and `QuestLog.given` (begun or ended is always
  given) / `shown` (feats only once done). `QuestLog.state` returns LOCKED for a quest not given (after the `learned` check, so EtoTests'
  "known another way is DONE" holds). TEACHER = a quest whose gate `QuestBook.add` stripped for `Teaching` (`QuestBook.lesson`), given once
  that gate is open. GHOULS = once any non-companion bond has MET. RETIRED: light_feet, up_we_go, witness_this (Eto's lesson quests now carry
  those titles). `QuestServer.opened` announces only shown quests; a FEAT's completion says "Feat:". Hunts: no random dusk offer;
  `QuestServer.huntable/askHunt`, offered by ghoul NPCs as SAY_7 key `hunt` ("Any prey out tonight?"), one per night (`nextHuntAt` = next dusk).
- **"You already know this" (1.3.29):** `EtoServer` on LESSON: `knownLessons` (Eto: lessons known some other way get their flag set;
  Touka/Ken: `Teaching.knownElsewhere` against the `NpcMemory.TAUGHT` bits, set on `taught` too), `Teaching.alreadyKnown` prefixes the
  reply, then the next lesson or refusal. `CompanionScript.lesson` always offers "Teach me"; `EtoServer.withLesson` adds Eto's LESSON when
  only known-elsewhere lessons are left.
- **Romance tab (1.3.29):** `npc/RomanceCard` (pure: `stageText`, `errands`, `next`, `save`/`load` as tab-separated lines), built by
  `RomanceServer.cards` and sent in `S2CQuests.romance`; `QuestClient.romance()`; `QuestJournalView.Tab.ROMANCE` (picked as "romance:KEY").
  Offscreen render: `java -cp <classes>:build/base.jar:<stubs> tools/UiPreview.java <dir> quests` writes `quests-romance*.png`.
- **New API (allowlisted with [srg]/[order] evidence, stubs added):** Container size/getItem/setItem/setChanged, ItemEntity.getItem,
  ItemStack getCount/setCount/copy/getMaxStackSize/isSameItemSameTags, Level.blockEvent, BlockStateBase.randomTick, LookControl.setLookAt(xyz),
  Goal canUse/canContinueToUse, ParticleTypes POOF/HAPPY_VILLAGER/SPLASH/FALLING_WATER (by declaration order in `iv`).
- **Ask the owner to try:** `/kagune touka max` (or Eto/Ken) to marry, "This is our home now." in a house with chests (throw a few things on
  the floor, plant flowers and wheat outside), watch a day; sort a messy chest by waiting; "Go home, please." while she follows; open the
  journal's Romance tab; ask Eto "Teach me something." for Light on Your Feet; at night ask a ghoul "Any prey out tonight?".

# State after 1.3.28

- **1.3.28 case sound:** `tools/case_sfx.py` (numpy + ffmpeg; synthesized from parts, no recording could be fetched: freesound,
  wikimedia and opengameart are blocked by the environment's network policy) writes `sounds/sfx/quinque/case_open_1..2` and
  `case_close_1..2` (mono Vorbis 44.1 kHz, 1.25 s; the last lock / second latch at ~0.56 s to fit `Quinque.UNFOLD`). `tools/sfx_pack.py`
  pools them as `sfx.case_open` / `sfx.case_close` (regenerate sounds.json with it, never by hand). `QuinqueServer.hand` plays them
  (plus the weapon's ring at 0.35 on opening). Never heard by the one who made them: ask the owner.
- **1.3.28 carried case:** an investigator's shut case is drawn in its own frames `<kind>_case_carried` / `_case_carried_slim`
  (deploy 0.05 / 0.1, listed ahead of the opening frames on the case item only; `QuinqueClient.caseFrame` -> `AgentPose.caseFrame`,
  which never gives them to anyone else). Their third person transform is solved in `tools/quinque_models.py` (`carried_display`:
  vanilla's ItemInHandLayer + ItemTransform inverted so the grip bar sits in the fist, the case plumb, latches out, along the stride)
  for the arm `AgentPose.CARRY_X/CARRY_Z` (0, 0.1). `AgentPose.carry` (applied last in `AgentClient.pose`) keeps the arm there in the
  world: it subtracts lean, side and twist. `AgentTests.carrying` runs the real model JSON through a Java copy of vanilla's chain on
  real poses (grip < 0.35 px off, plumb < 1 deg, inner face clear of a woman's 4.7 px leg on the run). Unsure: whether vanilla
  resets a slim arm's pivot y to 2.0 each frame (the solve uses 2.5); at worst the grip sits 0.5 px higher in the fist.
- **1.3.28 pair tactics:** `AgentRules.front` (launcher always front; else leader unless `swapped`), `round`/`sideOf`/`behind`/`kept`
  (go round wide at `ROUND_RADIUS`, the short way on its own side of the front, in within 40 deg of the back, stay in within 75),
  `holding` (front waits just out of reach `HOLD_OFF` while the other has `rounding` set, at most `HOLD_TICKS`), `turned` + `SWAP_AFTER`
  (the leader's `AgentBrain.sides` flips `Mind.swapped`). `Mind.rounding` also blocks front flips. New fight after `NEW_FIGHT` ticks
  resets it all (`fightFrom`). `AgentRules.flank` is gone.
- **1.3.28 idle:** `AgentPose.idle` (lean -0.1, side +0.04, weight right, left leg loose, breath period 74 ticks, glance 230 ticks,
  phase from the entity id), blended by `AgentPose.still(speed)` when the case is shut.
- **Version:** 1.3.27's jar said `1.3.26` in mods.toml (KagunePatch.VERSION was not bumped); both `build.py` and `KagunePatch.VERSION`
  are 1.3.28 now. Bump both every release.
- Branch at **1.3.28**, protocol 63.

- **1.3.27 CCG investigators:** entity `npc/CcgAgent` (`hexkagune:ccg_investigator`, MobCategory.MISC, player box) with its state on
  synced entity data (INT accessors: look+kind, swing step/`PARRY` + its tick, flip + tick, dash way + tick, last shot tick; the ticks are
  the low 32 bits of game time, read with `AgentRules.since`). No packet, protocol unchanged. Rules (pure, `AgentTests`) in
  `npc/AgentRules`: skins `textures/entity/ccg/ccg_f0..f3, ccg_m0..m4` (owner's zip, renamed only), women = `FemaleBody.SIZE * 1.07`,
  build 0.93, curves 0.85; spawn chance 0.0042 per 200 ticks per player (about a pair every two days), wild ghouls cut to 0.008 (from
  0.05). Brain `server/AgentBrain`: pair saved in persistent data (`CcgAgent.KEY` packed long + `KEY+"Partner"` UUID); a lone one (made
  by /summon) gets a partner beside it at once (`AgentSpawner.partner`). Targets: grudges/marks first (a revealed ghoul =
  `KaguneProfile.isUnlocked && isActive` in sight within 24, marked 2400 ticks for both), wild ghouls within 20, Enemy within 6 (kept
  till dead or 40 away), mobs targeting them. Melee: `begin` registers `KaguneClash.swing` (so clashes work both ways), `land` checks
  `KaguneClash.meets` then hurts (mobAttack source; `QuinqueServer.hurt` adds the ghoul bonus for any holder) and calls the generalized
  `QuinqueServer.finisher(LivingEntity, ...)` on step 2. Parry: on `KaguneClash.threat` a swing aimed at the striker landing when theirs
  does, so `meets` from their side throws both apart; the stop callback gives a 3-tick riposte. Feet: flips as `GhoulNpcCombat` does
  (PaleRules.Flip, NpcRules.FLIP_BUSY/FLIP_POSED), dashes (4 ticks of renewed push). Launcher: bursts via `CrystalShots.fire` with
  `foe`. The case is the main-hand stack, opened/shut with `QuinqueServer.hand(LivingEntity, ...)` (shared with players' `turn`).
  Client: `client/AgentClient.pose` (flip > run + quinque + dash), `client/animation/AgentPose` (run cycle, parry, dash; pure),
  `client/render/AgentRenderer` + `AgentModel` (three models: wide man, slim man, woman with the female figure; bust layer). Drops:
  `AgentBrain.drop`, 2.5% + 1% per Looting level, player kills only. Recipes removed (`QuinqueTests` asserts none).
- **1.3.27 build confirmed:** a full `python3 build.py` ends with EXIT 0 (API verification, dedicated server check, guard, name check,
  every test suite; `AgentTests` 20302 checks). The flip/dash sound `entity.player.attack.sweep` in `AgentBrain` stays: `Sfx` draws it
  from the pack (`blade_swing` / `blade_heavy`) and `SfxTests` passes. Still **untested in game**.
- Branch at **1.3.27**, protocol 63.


- **1.3.26 Touka's wings:** `UkakuWingRenderer` passed grow 0 for both wings (meaning "thrown as vortices"), so nothing drew. Now
  `WingRules.companion(...)` builds her input; `CompanionTests.toukaWingsShow` checks quads come out.
- **1.3.26 quinques:** five kinds in `Quinque` (BLADE, SPEAR, KATANA, MACE, LAUNCHER; fields `model`, `twoHanded`, `ranged`, `swing`),
  items registered in a loop (`ControlItems` CASES/WEAPONS). Poses in `client/animation/QuinquePose` (stances solved in the scratchpad
  by fitting the item's third person transform so the right grip is in the right hand, the left grip where the left arm reaches, and
  the weapon points as wanted; values baked in `tools/quinque_models.py` HELD and `QuinquePose.stance`). Players: `QuinqueClient.pose`
  (swing start from `f_20911_`/`f_20913_`, combo step `Quinque.next`) blended in `KaguneBodyAnimation.setupAnim` (`wants()` and the
  early finish include `QuinqueClient.posing`). Server: `QuinqueServer` combos (hits only), `finisher` (sweeps never hit players),
  launcher `fire` + heat, spear speed modifier; shards in `server/CrystalShots` (any shooter; `striking` flag keeps shard hits out of
  combos). (1.3.27: the recipes are gone; cases are rare investigator drops only.)

- **1.3.25 quinques** (the start of the CCG; plan in `docs/ccg-research.md`): items `quinque_case_bikaku` / `quinque_bikaku_blade`,
  `quinque_case_koukaku` / `quinque_koukaku_spear` (`ControlItems`; weapons are `SwordItem(Tiers.NETHERITE, ...)`). Right click opens a
  case, Shift + right click shuts a weapon (`server/QuinqueServer.use`: swaps the stack, copies its tag, stamps `QuinqueAt`). Models:
  five frames per kind chosen by the item property `hexkagune:deploy` (`client/QuinqueClient`, `Quinque.shown`), generated by
  `tools/quinque_models.py` (weapons built standing, every element turned -45 about z so vanilla's handheld transforms hold them; the
  case's third person pose solved against the arm). Ghoul bonus and RC suppression in `QuinqueServer.hurt` / `GhoulRegen.suppress`;
  the spear's reach is a transient `ForgeMod.ENTITY_REACH` modifier. `QuinqueTests` also checks every model/frame/texture/name/recipe
  file exists. Sounds must come from the owner's pack (`SfxTests`): use `hexkagune:` ids. **Untested in game**: the model frames (my
  first person preview was not calibrated against the real game), the reach attribute on the client.
- Branch at **1.3.25**, protocol 63.

- **1.3.24 places:** `tools/companion_structures.py` turns `art/anteiku.schem` (Touka, behind the café counter, 2nd floor) and
  `art/cyber_tower.schem` (Ken, basalt terrace at the spire's foot) into `data/hexkagune/structures/{anteiku,cyber_tower}.nbt`, each
  companion a template entity; `--blocks` checks every state against minecraft-data's 1.20 `blocks.json`. Worldgen: jigsaw, one per
  world (`concentric_rings` count 1; distances 4 library / 10 Anteiku / 18 tower keep them apart: all rings share the seed's angle),
  allowed in `#minecraft:is_overworld`, preferring flat biomes. The tower is cut to schematic y 62..315 (a 1.20.1 world has 384
  blocks of height; the build is 320), stands at absolute y 62 (`start_height` 63, `beard_box`), and is centred on its start chunk by
  the `hexkagune:tower_anchor` jigsaw (structures are built only 8 chunks round their start). Its air is left out (474k blocks).
  **Untested in game**: the start height maths (piece min y = start_height - 1), the beard clearing, and generation time for the tower's
  chunks (each chunk processes the whole template).
- **1.3.24 fights:** `npc/FightRules` (notice 5, gone 24 / child 16, chase 32 from home, give up 30 s without reaching or hitting);
  `EtoBrain.target/seek/hostile` (neutral mobs only when angry), `Mind.reachedAt` (set near it, on a blow landed, on every new target);
  `ChildBrain.threat`. Fixed: a human child's `mind.target` was never cleared after a fight. **1.3.24 airplane:** `ChildMoment.spinTurn`
  (wrapped), the parent's `spin` negated (RigPose spin + is left).
- Branch at **1.3.24**, protocol 63.

- **1.3.23:** Ken's kakugan was a red bar over a black bar (`find_eyes` read his 2x2 eyes by rows). His eyes are now in
  `tools/companion_textures.py` `EYES["ken"]`, column-wise like Touka's: inner column iris, outer column white, the near-black pixel
  above the iris (under the bangs) a lash. Rerun the tool after touching `art/ken_skin.png`. Branch at **1.3.23**, protocol 63.

- **1.3.22 hand in hand** (owner: "clearly not right"): `ChildRules.hand(stage, parentSize, slim)` solves the kid's arm (z, x) and side
  distance on the real models (player arm at 0.9375 px scale x size; kid at `stage.draw`, little-one layout for toddlers) so the hands
  meet; `handParentZ(stage, size)` swings the parent's arm out when a teen can't reach down. `FamilyTests.hands` checks it independently.
  `tools/MomentPreview.java` now draws the kid on its in-game proportions (`kid()`), not a scaled player: previews before this lied
  about toddlers.
- Branch **`claude/relaxed-carson-4y0frw`** at **1.3.22**, protocol **63**, built green (exit 0) and pushed. `main` is still 1.3.0.
  **Nothing from 1.3.13 on has been tried in game.**
- **1.3.21 household** (`server/Households`, SavedData `hexkagune_households`, one `Home` per player: dimension, spot, radius 20): set by
  the married spouse's new talk choice `NpcTalk.Choice.HOME` / `EtoTalk.Effect.HOME` (appended last; EtoTests pins 36 choices). Spouse:
  `Households.forCompanion` (its `home()` at the spot) widens its restriction; kids inherit a player parent's home (`forChild`) unless
  following/`waiting()` (a ChildNpc flag set by "Wait here." and by being put down outside the house).
- **Beds** (`family/HomeRules` pure, `server/HomeLife`): bed heads found by `Block.toString()` ending `_bed}` and the state string;
  two heads side by side with one facing = a double bed (spouse takes one free half); singles go to the kids by birth order. Sleep is
  vanilla `startSleeping/stopSleeping` (`m_5802_/m_5796_`); `m_6107_` (isImmobile) is overridden so sleepers don't move, which also stops
  `m_8024_` (the brain): **everything for a sleeper runs in `HomeLife.tick` (LivingTickEvent)**: waking, and `ChildBrain.sleeping` (the
  pending tuck-in and the moment sync, since `mind.dirty` is normally flushed at the end of the brain tick).
- **Tuck-in:** `ChildMoment.TUCK` (child pose null: the game lays it down). Waits as `pending` until the parent is within
  `ChildBrain.TUCK_NEAR` (2.6); NPC parents walk to 1.6 (`ParentCare.moment`); players get a "Come tuck me in!" line and can also pick
  *tuck them in* on the sleeping kid's page (`ChildTalk.asleep`). Feed: `ChildMoment.FEED` (stand 0.85), food in `Mind.food`, healed in
  `fed()`. Hand in hand: `ChildRules.RIDE_HAND` (the kid at the carrier's left on the ground).
- **Doors:** `GroundPathNavigation.m_26477_(true)` + `OpenDoorGoal` on EtoNpc and ChildNpc (wooden doors only).
- **Death is final:** `UniqueNpcs.mortal("child:")` + `perished`; `Children.remove`. **Protection:** `ChildBrain.attacked` cancels any
  damage from an EtoNpc or an EtoNpc-owned FleshThrall (plus the player-parent rule). **Might:** `Persona.might` (Ken 1.0, Eto 0.95,
  Touka 0.6), players via `ChildRules.playerMight`; `ChildRules.health/damage(..., might)`; `Children.Child.might` saved (11 fields).
- Untested risks: pathing onto/around beds, two kids and one bed (the second stays up), the kid clipping walls while hand in hand,
  multiplayer homes (one per player; a child with two player parents takes the mother's first).

# START HERE (state after 1.3.20)

- Branch **`claude/relaxed-carson-4y0frw`** at **1.3.20**, protocol **62**, built green (exit 0) and pushed. Built on `claude/relaxed-turing-r5599k`
  (1.3.17). `main` is still 1.3.0. **Nothing from 1.3.13 on has been tried in game.**
- **1.3.18:** children wear the owner's HD skins (`art/children/{sons,daughters}`, made ready by `tools/child_skins.py`: square layout,
  vanilla's opaque base, per-eye kakugan overlays `_kr/_kl` and glows `_gr/_gl`; every eye **hand-mapped** in its `EYES` table after the owner
  rejected an auto-detected one: "evaluate if the iris looks good and real"). `family/ChildSkins.pick` (by sex, near the inherited tone,
  `SPREAD` 30, then seed); `ChildRenderer` swaps wide/slim models and draws the `Kakugan` layer. Three daughters (cloud, emo, rainbow) have
  eyes under hair: no kakugan. The owner's set has no truly dark skins (darkest is medium tan). Touka's legs: `Persona.legs` 1.2.
- **1.3.19 Ken in Peace:** PeaceKit and its helpers take any `LivingEntity` (`KingServer.BodyKit`, `wear/unwear/bodyTick/bodyHurt`,
  `KingEffects.valid/hostile` -> `EtoBrain.foe` for NPCs; NPCs never carve terrain). Client: `KingLayer.drawPeaceBody` (an `EtoRenderer.Kakuja`
  layer), `KingClient.get(Entity)/find/wearers`, Peace's stance and moves in `EtoFight.peace`. Mind: `npc/KenRules` (pure) + `server/KenPeace`
  (holds and lets go the lance, the burial and the heavy; slams from the Ascent; recalls the lance; benches an ability that won't start).
- **1.3.19 Touka's wings:** `npc/WingRules`, `server/NpcWings`, `network/S2CNpcWings` (id 45), `client/NpcWingsClient`, drawn in
  `UkakuWingRenderer` at her size on her posed torso. Persona flags: `finalKakuja()`, `wings()`.
- **1.3.20 parent-child moments** (protocol 62): `family/ChildMoment` (pure: which stage, how long, how far it stands, toss/spin shapes),
  `client/animation/ChildMomentPose` (both bodies; players crouch on bent knees, NPC parents lean), `ChildBrain.start/moments/stand`
  (walks up first; babies stay put), ride modes `ChildRules.RIDE_*` (shoulders, spin), the Play page in `ChildTalk` (a moment starts when the
  talk closes), NPC parents in `ParentCare.moment/startMoment`. Previews: `tools/MomentPreview.java` (the child drawn as a scaled player:
  PoseRender has no little-one proportions).
- Riskiest, untested: Peace drawn on Ken's slim-armed EtoModel (armour built for player arms; Ken is 0.94 size while the server cuts at
  player size), the client's `KingClient.BODIES` lookup for effects, Ken's navigation vs the Ascent's lift.
- The owner wrote "a halfway version of the Rinkaku" for Touka; she was given Ukaku wings (her kagune, and what they described: wings
  turned to crystal, spikes, the barrage). Say so if asked.

# START HERE (state after 1.3.17, end of session)

- Branch `claude/relaxed-turing-r5599k` at **1.3.17**, protocol **60**. Pushed. 1.3.17 was pushed after only a quick compile and
  `CompanionTests` (the owner ran out of usage): **run `python3 build.py` first and check exit 0.** `main` is still 1.3.0.
- Releases this session: 1.3.13 hands alive; 1.3.14 Touka/Ken/teaching/trial/partner immunity; 1.3.15 renderer-crash guard, Touka figure;
  1.3.16 children fixes (carry, proportions, size, `/kagune children`, NPC parents: `server/ParentCare.java`); 1.3.17 Touka's new skin
  (her painted red right eye mirrored by day in `tools/companion_textures.py` `mirror`; eye pixels named in `EYES`), curves 1.4.
- **Nothing since 1.3.13 has been confirmed in game.** The owner said they have "many bugs" to report: ask for them first and fix
  them one at a time before any new feature. They interrupt long waits; keep steps short and push often.
- **Open items:**
  1. **Child skins are disliked** ("anything but cute": adult faces with brows and red lips, adult clothes on toddlers). The owner wants
     skins picked from NameMC. The network policy blocks namemc.com, s.namemc.com, textures.minecraft.net and other skin sites (GitHub,
     GitLab, npm reachable; nothing suitable found there). Ask the owner to allow those domains (environment Network access) or to send a
     zip. Same blocker for **wild ghoul skins** (male and female, many, eyes adapted with the `companion_textures.py` detection).
  2. Babies' new proportions (full head on a half body, `ChildRenderer.Model.little`) and the in-arms carry (`ChildRules.CARRY_UP`
     1.02, facing the carrier) are untested: ask how they look.
  3. Ken romances women only (`Persona.likes`); the owner wants him a devoted lover; his poses `EtoPose.led` were loosened once on request.
- Rules learned: never alter the owner's skins (only overlay eye pixels; ask for files you can't read); say when a preview can't show
  something (PoseRender draws no female figure, no face overlays); any new entity type must go in `ThrallRenderGuard.ours`/`registerAll`;
  `check_names` (Kaneki/Ayato/Furuta) and PaleTests (Takatsuki/Aogiri/Yoshimura) ban names in player-visible text; don't `pkill -f`
  a pattern that matches your own command line.

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

**1.3.15 (after the above):** the owner's crash on 1.3.12 (a newborn child drawn with no entity renderer: their modpack, which runs
ModernFix among ~200 mods, loses this mod's renderers, as happened before for the flesh creatures and Eto). `client/render/ThrallRenderGuard`
now covers every body the mod adds and re-registers all of the mod's renderers (`registerAll`) once per kind; still none, the body is kept
out of the client's world. **Any new entity type must be added to `ThrallRenderGuard.ours` and `registerAll`.** Touka is Eto's female
body, a little skinnier (`Persona.curves` 0.65, `build` 0.94; the owner: "she's just a little skinnier than Eto"); the preview tool does
not draw the female figure (no hips or bust), so say so when sending pose previews. Animated previews: `CompanionPreview <out> frames`
then `python3 tools/companion-gifs.py <out>`.

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

- **CCG investigators, untested in game.** Worth watching: whether they open the case and close in fast enough to feel dangerous; parry
  frequency against a player's M1 spam (each threat answered once, `AgentRules.parry`); flips into walls; the follower's flank spot
  pathing; the female scale/build reading as "a little taller and skinnier than Eto". Possible next steps: investigator voice lines,
  a CCG office structure, ranks giving stronger stats, quinques made from the kakuhou of ghouls the player kills (canon).

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
