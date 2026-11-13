## 4.1.80 (HexGhoul): romance: each companion's courtship their own, ten chapters that ask for something new

HexKagune 4.1.80 for Forge 1.20.1. Not tested in game. Protocol unchanged (126).

Your to-do, its final step (quoted exactly): "Add to the to-do list that the final step is to better the quests, to include everything in the
mod + minecraft, daily  and romance quest betterment, so it's more fun, less boring, same for the joining gang quests." The plan shown to you
(you said "whatever you think is best"): the dailies (4.1.78), the gangs' joining (4.1.79), then romance: "keep all 35 scenes and lines;
swap the repeated tasks so each companion's five differ (Roma's "kill 3 investigators" a circus show for her; Ken's "kill one beside me" a
night reading at the library; Ayato's a spar at the hideout...)". This is that step. The courtship had 35 chapters, each with its own scene, but
only 11 kinds of task: 7 were "kill investigators", 4 "take me somewhere high", 4 "be at my place in the dead of night", 4 "hand me
something" (two of those Ghoul Meat). Now ten chapters ask for something new, each companion's five are five different kinds, and every
companion has at least one no one else asks for. Investigators to kill: 3 chapters (was 7). Somewhere high: 2 (was 4). Things handed over:
3 (was 4), Ghoul Meat once.

- **Touka, chapter 2 (was four investigators): "Help me study".** She has exams and nothing sticks. Ask her about it and press "Okay. Read me
  the first one.": three questions from her homework (sums, science, English; nine in all, three drawn a day, answers in a different order
  each time). Three right in a row and it's done; one wrong and she's done for the day ("Tomorrow").
- **Ken, chapter 4 (was a Rank 5 investigator killed with him along): "Nights at the Library".** At night, with Ken following you, sit and
  read with him at the Library (Eto's place) for two minutes.
- **Ayato, chapter 2 (was three wild ghouls): "Hit me like you mean it".** A spar with him, the same rules as Aogiri's way in (get him to
  half his health before he gets you under a quarter of yours; nobody dies, no bond lost for the blows), with his own words. His scene comes
  to you as soon as you win. **Chapter 3 (was anywhere high): "Where they can't see"**: the CCG tower's roof, him following you.
- **The General, chapter 2 (was a villager who cares for you, which Eto asks too): "Someone to vouch for you"**: two villagers' favours done
  (the ones they ask of you in their talk). **Chapter 3: the Pack, now with a quinque in your hand** (a case's weapon or a ranked
  investigator's). **Chapter 4 (was anywhere high): "The district, not the map"**: in daylight, with him following you, walk through a
  village for two minutes.
- **Roma, chapter 1 (was three investigators): "A show for the ringmaster"**: eight fireworks set off at night by her ring, where she can
  see (right click a block with a firework rocket; she has a word for every one). **Her walk (chapter 2)** is now three minutes, said in
  minutes (see below).
- **Banjou, chapter 1 (was four Ghoul Meat): "Supper for the boys"**: cook bowls from the cookbook (any of its bowls) and hand one to each of
  three of his Gas Masks yourself (right click one with the bowl in your hand; each boy eats once). **Chapter 2 (was three investigators):
  "Something that grows"**: plant six flowers or saplings at the Gas Masks' den with him beside you. Only what really goes into the ground
  counts (the block is looked at the tick after your click).
- **Eto's five are as they were**: they were already hers alone (the village at night, a human's love, the Doves while she watches, a
  human's secret, the Library at three in the morning).

### The scenes

All 35 are kept. Their lines changed only where they named the old task: Touka's homework scene (the book, not a Dove's coat) and the lines
before it that said the Doves came next; Ken's Library scene and the lines before it about the investigator; Ayato's spar scene, his roof
scene's first line (the Doves' own roof) and the pie scene's lines that sent you after wild ghouls; the General's walk (the same bakery,
seen from the street), his Pack scene's first line and the lines that set them up; Roma's show (sparks, not briefcases); Banjou's garden
scene and the line before it. Everything else, every answer and what it says back, is as it was.

### If you were halfway through

A chapter on whose task changed starts again from nothing (what it counted was something else). If it was already done and its scene was
waiting, it stays done: go and see them. Every other chapter, and everything closed, is kept.

### Found on the way

- Roma's walk counted three steps of ten seconds (half a minute) and said it as "0.5 of 3 minutes". Now three minutes, said "1.5 of 3
  minutes".

### Choices of mine you may want changed

- The Library for Ken's reading and the tower for Ayato's roof are found by Eto's and the General's homes (as the daily board's night
  watches are): if one of them has moved house with a spouse, it is where they live now.
- The roof counts when nothing is over you, about as high as the tower's top over the General's floor, within 24 blocks of him.
- Roma must be within 24 blocks to see a firework; Banjou within 16 of where you plant, and you within 40 of his den.
- The homework: three right in a row out of nine questions; one wrong, tomorrow.
- Banjou's boys are any of the Gas Masks. A gang starts nothing with you, but if you joined the CCG every ghoul goes for you, his boys too.

### Other

- Render: `docs/handoff/romance-4.1.80.png` (every new chapter as the companion asks it, the homework's questions and a wrong answer, each
  changed scene's first page, the journal's Romance tab on four of them; the game's own talk screen and journal).
- Tests: `CompanionTests.courtship480` (each companion's five kinds, the counts of each kind, every new chapter as planned, the minutes,
  what is planted and what isn't, the roof, daylight, the homework, old saves, their words, all 35 scenes, and how the server holds each);
  68 rules sabotaged one at a time (`docs/handoff/sabotage-4.1.80.json`).
