## 2.0.5 (HexGhoul): other players and someone else's children (never dateable)
HexKagune 2.0.5 for Forge 1.20.1. Install on client and server (**protocol 73**, unchanged). Built and tested by CI. **Not yet tried in game**: please report anything that looks or feels wrong.

### New

- **Talking to someone else's son or daughter.** A player who isn't the child's parent used to get only "Hello there." and "...Hi." Now they get a real page. What the child says depends on its age and temper.
  - **Babies** only make sounds and do things: stare, grab your finger, coo, blow a bubble.
  - **Toddlers** babble a word or two at most ("Hi! Hi!", "Mama!", "No no!"), plus what they do.
  - **Children** chatter about themselves and their mom and dad by name. A shy one hides; a careful one says it isn't supposed to talk to strangers.
  - **Teens** are wary.
  - **Grown sons and daughters** make polite small talk and mention their parents.
- **Gentle choices only for strangers:** talk, wave (a baby claps), a high five (from childhood up; a shy child won't), offer the food in your hand, and "Where are your parents?".
  - **Food:** little ones won't take food from strangers. A teen or a grown-up takes it politely for later, or says they can't eat it.
  - **"Where are your parents?":** if a parent is near, the child points them out, and your action bar shows which way and how far. If that parent is a player, they get a chat line saying you asked.
- **Parents only, as before:** carrying, holding hands, follow/wait, teaching to fight, feeding from the hand, bedtime, sending home, tucking in, play and the talk topics. These are checked again when a choice comes in, not only when the page is built.
- **A stranger's kakugan or kagune frightens children.** They say something that fits their age.
  - A kagune makes a little one run behind a parent or keep away.
  - A kakuja or worse makes anyone hide.
  - Hiding from a stranger has its own words: the old ones were for hiding from their own parent.
- **NOT DATEABLE.** No son or daughter, at any age including grown, can ever be dated, kissed, asked out, flirted with or married, by anyone, parent or not. A new test guards this and fails the build if it ever breaks:
  - It checks every choice on every child's page, for every stage, for parents and strangers.
  - It checks the choices the talk sends: no romance choice is ever among them.
  - It checks every child scene's answers and effects, and every new line, for romance words.

### Known approximations

- If two players talk to one child at once, both pages work, but the child faces only one of them. Only one high five or wave can happen at a time; the other is skipped.
- A stranger waving or high-fiving ends a game of hide and seek the child was playing (as a parent does).
- Babies' and toddlers' old lines with their own parents (from before 2.0.5) are unchanged; only the new stranger lines follow the "nothing coherent" rule. They can get a pass of their own if you want.
