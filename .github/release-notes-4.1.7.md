## 4.1.7 (HexGhoul): Kick Master

HexKagune 4.1.7 for Forge 1.20.1. Not tested in game.

Your words: "He teaches a new combat style mixing kicks and punches (mostly side kicks, spinning kicks etc), he only teaches the player this through quests if the player is either a human or wearing a full mask." "Do a very deep research into combat moves like I described make sure the corehography is perfect, this combat style upon learned fully replaces the default M1 combos." "New fighting style called: "Kick Master" Make up the quests yourself make sure they're creative and beautful."

### The string (M1)

Eight blows, each leaving the body where the next one starts:

1. **Jab**: finds the distance.
2. **Cross**: turns the hips through and loads the rear leg.
3. **Round kick**: the rear shin whipped round into the ribs, the trunk laid away from it.
4. **Side kick**: knee chambered across the body, the standing heel turned to them, the heel driven straight into the middle and held a beat.
5. **Spinning back kick**: she turns her back, finds them over her shoulder, and thrusts the heel straight back (a T on one leg).
6. **Hook punch**: squares her up and winds her the other way.
7. **Spinning hook kick**: a full turn, the leg thrown out, and the heel hooked across them.
8. **Tornado kick** (the finisher): a full turn into a jump, the round kick thrown from the air, landing low.

- Every kick has a chamber, a pivot, an extension, a hair of overshoot and a re-chamber before the foot comes down.
- The kicks are now written as where the foot goes, and the hip angles are solved to put it there. I rendered every kick from the side and checked that each one lands level into the target on the server's impact tick.
- Kicks are thrown from further out than punches: the server stops you a leg's length off (1.15 blocks from the front of the target, against 1.05 for a punch).
- The string is all there once learned: no mastery needed.
- Knockback: the round kicks throw them sideways, the side and back kicks drive them back, the tornado kick throws them back and up.
- Each kick has its own impact sound.

### Who gets it, and how it works

- **A human** who learned it fights with Kick Master whenever the main hand is empty. M1 is the string; nothing else of the eye-mode fight (no Big Punch, no Speed Kick, no flips).
  - A human pays no RC (there is none).
  - A human's blows land like a ghoul's fists at mastery 500, times 0.8. A jab does about 2, the tornado kick about 4.8.
- **A ghoul** who learned it (behind a full mask) uses it with the kakugan open, as Speedster was used. It **replaces the Speed Blitz** on M1.
  - If you also learned Speedster, the right click stays Speedster's flips.
- **The right click** (your chargeable teleport speed kick with the side twirl) is **not in this version**. It's next.

### The General's quests: the three watches of the night shift

Ask him "Teach me to fight." He teaches a human, or a ghoul behind a full mask (he takes them for a human). A ghoul whose eyes he can see is turned away: "I can see your eyes...".

Each watch is a proof he sets you, then a lesson:

1. **The First Watch:** kill **8** hostile things **after dark** with nothing in your hand (no kagune out). He teaches the stance.
2. **The Middle Watch:** kill **3** with **your feet off the ground**, nothing in your hand ("A kick is half a flight"). He teaches the chamber.
3. **The Dawn Watch:** kill **3** **at dawn** (first light to full morning), nothing in your hand. He counts the night shift in from the roof at sunrise. Then he teaches you **Kick Master**.

The action bar counts each proof as you go.

### The General fights with it

- When he's attacked he now throws the Kick Master string, not the Speed Blitz.
- As a human he has no ghoul's Big Punch. He keeps the Speed Kick and his flips.

### Other

- The ability list (kakugan open) shows Kick Master and its string when learned.
- Protocol 115: the new blows and gates go over the network, so server and client must both be 4.1.7.
