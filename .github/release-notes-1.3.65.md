## 1.3.65 (HexGhoul): everyone fears War, and every final kakuja gets its own reactions (testing)
HexKagune 1.3.65 for Forge 1.20.1. Install on client and server (**protocol 71**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

You asked for "more and better reactions to the final kakujas, Rinkaku and Ukaku both, but especially the War/Peace path, being scared of the War form."

NPCs now tell the worst sights apart: **War, Peace, the Dragon, Rinkaku's final kakuja and Ukaku's final kakuja**. Each one gets its own reaction from everyone. War (and the Dragon) is a **terror**: the fear lasts as long as it's in sight, not just the moment you put it on.

- **Wild ghouls** that don't know you run from War, and keep running while it's in sight (within 28 blocks), 20 blocks at a time, away from *you* (before, they ran from whoever was nearest). Every few seconds one of them shouts while it runs ("Don't stop! Don't look back!"). Peace and the final kakujas scare them off for a long while too, each with its own words ("That's not a kagune. That's a cathedral.", "It's on fire— it's ON FIRE and it's still coming!"). Ghouls that follow you have their own words for each.
- **Humans** see War and the Dragon from 40 blocks (other final kakujas from 28, a kagune from 16, as before), if they can see you. They sprint away and keep running after losing sight of you, until they're 52 blocks off. They **scream** in a bubble over their heads as they start to run, and over and over while War is near ("It's the King! The One-Eyed King! RUN!"). Before this, a player in War or Peace (whose kagune goes in) only scared them from 6 blocks. Humans also shout once at an ordinary ghoul now.
- **CCG investigators** now see War, Peace and the final kakujas as what they are and mark you, as they do for a kagune. At first sight they **falter**: frozen where they stand (War longest; a junior longer than the senior), with a line. A senior steadies itself ("Hold. Nobody move. Nobody breathe."), a junior shakes ("Sir? Sir, my hands won't stop shaking."). Then they fall back to about 14 blocks, find their nerve with a word ("On me! It bleeds like the rest of them!") and come in. They won't falter again for a minute. A hit snaps them out of the freeze.
- **Your children** each have something to say for every final kakuja and age. When you're in War (or the Dragon) near them, they **hide behind their other parent**: you, if you're the other parent, or their companion parent, whoever is nearer. They stand on the far side from you, shielding themselves (a new pose: arms crossed up in front of the chest, head tucked, trembling; a toddler clutches its hands under its chin), peeking out at you and saying something now and then ("Is that really Papa? Is Papa okay?"). With no one to hide behind, they keep their distance. A baby just sits and whimpers. Whatever they were doing stops: a talk, a game, a moment.
- **Eto, Touka and Ken** each react in their own way, with their own words and a gesture, depending on whether you're their partner:
  - **Eto** is fascinated by War ("Mine. Even like that. Especially like that."), with a hungry finger at her lips, or studying it with a hand to her chin if you're not hers. Peace leaves her without a clever word.
  - **Touka** is sick with worry ("Don't you dare become that. Not you. Not you too.") and **flinches** (new gesture: a step back, one arm shielding her face). Peace she calls beautiful, then tells you not to tell anyone.
  - **Ken** knows where War's road goes ("I know where that road goes. I walked it."). Peace is his own ("That's mine. How do you— ...It suits you better than it ever did me."). He stands in **awe** of it (new gesture: hands together on the chest, head tipped back looking up). At Rinkaku's final kakuja he cracks his finger.
- 197 new lines in all (194 different; a few short screams repeat). All tested: something for every sight, age and person, every name filled in, none of the names that must stay out.

Judgment calls and approximations:
- **There are no scream sounds:** your hexkagune pack has none, and only its sounds are used, so the screams are speech bubbles. Send me scream sounds if you want them heard.
- Agents now go after War and Peace players (they mark you as they would for a kagune), because the falter only makes sense if they then come at you.
- Only players are watched. Ken fighting in his own Peace doesn't frighten anyone.
- Companions react with a line and a gesture. They don't walk toward or away from you.
- The new poses (the children's cowering, the flinch, the awe) were checked on the off-game pose renderer, which has stiff limbs and no elbows. Not seen in game.

Untested in game: compiled and unit tested only. Try: put War on near a wild ghoul, a human (`/summon hexkagune:human`), a pair of investigators, your children with their other parent near, and each companion.
