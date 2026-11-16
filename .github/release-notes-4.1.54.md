## 4.1.54 (HexGhoul): the Gas Masks and Banjou

HexKagune 4.1.54 for Forge 1.20.1. Not tested in game. **Protocol 125** (a new entity, a new structure, and Banjou's slaps on the wire):
update the server and every client together.

Your words: "New gang: / Gas Masks - Toxic logo, green name. / Leader - Banjou / Personality - wants to be tough but truly isnt, is aware
of how weak he truly is. / Kagune - super small blue Ukaku wing (singular) cannot shoot projectiles, only uses basic m1s with it. / Dialog -
Daily quest giver, Chat, Romanceable (Only females). / Lives in the small house in the schematic. / Gang Bonuses: / While wearing the gas
Mask, gain night vision. / Gain speed 2 when at low HP. / Reputation"; "The gas mask leader puts on the mask at night and is just alex
model by the way, not muscled at all"; your answers "Short chain, like Roma" (how the Gas Masks are joined), "Joining gives the mask", and,
for his home, "The cottage".

**Banjou** (a new official NPC, one per world, back in his cottage a day after he is killed; `/kagune banjou here` for operators):
- Your skin (`Banjou_skin.png`, painted for wide arms) on the slim arms ("just alex model"), arms narrowed in the game's copy; no muscle.
- His **gas mask goes on at night** and comes off by day (every client draws it from the hour).
- His voice: he opens every sentence like a gang boss and finishes it like an apology. He says he's the weakest ghoul in his own gang
  before anyone else can; his men follow him because he's the one who stays. Chat, topics (the Gas Masks, being weak, how he fights, the
  mask at night, the flowers he waters and pretends someone else does), gifts (charcoal for the filters; a blue orchid), three scenes of
  his own, a full courtship and three dates.
- **Romanceable by women only** (a man asking is told no, kindly). His courtship: supper for his men (Ghoul Meat), investigators chased
  off his hideout, somewhere high where he can feel big, a wild ghoul made your friend (he's tried for years; they laugh at him), and the
  cottage in the dead of night with the mask coming off. Dates: the garden at night, the cottage roof, tea.

**His one small blue wing** ("super small blue Ukaku wing (singular) ... only uses basic m1s"):
- One wing, on his right, the Twin Wings at their smallest and drawn smaller still (about half of Touka's wing at rest), crystal, blue.
- It throws nothing. His M1 is a string of the base form's one-wing blows, the player's own (the same lengths and impacts): a **slap**, a
  **wide swing**, a **rising wing** (knocks up), then a short rest. His body turns into each blow as a player's does.
- I left the **overhead slam** out of his string: rendered at this size, the wing's hand comes down beside his own shoulder, short of
  anything in front of him. The other three land on a body at arm's length (renders below). He walks in close to slap.
- His blows are weak (his wing hits at 0.6 of Touka's), as he'd tell you himself.

**The Gas Masks' hideout** (your `GasMask_hideout.schem`, one per world like the circus): Banjou on the cottage's ground floor, four Gas
Masks about it (two at the front door, one in the walled garden, one in the little garden house), each in its own mask, keeping to
their spots. `/kagune places` lists it.

**Joining the Gas Masks** (talk to Banjou: "I want to join the Gas Masks."):
1. He asks why. Of three answers, one takes him seriously (the one that isn't mockery and isn't flattery he knows is false).
2. Kill **3 Clowns** (they keep hurting his men).
3. Bring him a **clown's mask** (any of the five), in your hand.

Joining gives you **a gas mask**. Your name: the **toxic trefoil** emblem, a space, your name in **green** (in chat and over your head).
As every gang: they never attack you, they defend you, and killing more than 10 of them bans you. Gas Masks out roaming are the gang's
own too now (before, a Gas Mask belonged to no gang a player could join).

**The Gas Masks' traits** (for a member):
- **Night Vision** while one of the Gas Masks' masks is on your head; taken back as soon as it comes off (a night vision potion's own is
  left alone).
- **Speed II** while your health is at 30% or under.

**Daily quests from Banjou** ("Anything the boys need today?" on his first page, members only), as Noro's and Roma's:
- Jobs: kill 3 to 5 Clowns; kill 2 to 4 investigators; bring him 6 to 12 charcoal (for the filters).
- Pay: two of a gas mask, charcoal, iron ingots, Ghoul Meat, emeralds, a golden apple; and +10% reputation.

The reputation bar shows in the Gas Masks' green when you talk to Banjou or one of his men; at 0 you're warned to leave the hideout, as
with the other gangs.

### Other

- Fixed: Noro's and Roma's daily lines lowered the whole job ("bring roma 4 alliums"); only its first letter is now ("bring Roma 4
  alliums", "kill 3 Gas Masks").
- Renders: `docs/handoff/banjou-4.1.54.png` (day and night, four sides), `banjou-wing-4.1.54.png` (his wing beside Touka's),
  `banjou-slaps-4.1.54.png` and `.gif` (the string blow by blow), `banjou-impacts-4.1.54.png` (each blow landing on a body at arm's length:
  front, side, above), `gas-hideout-4.1.54.png` (everyone marked), `gang-emblems-4.1.54.png` (the toxic trefoil in a name tag and chat),
  `banjou-talk-4.1.54.png` (joining, and his daily with the green bar).
- Next on your list: Ayato/Touka marriage reactions, a spouse's reactions to gang life, "Quit the gang", the marriage and membership
  rules; then the menus, the talk screen first.
