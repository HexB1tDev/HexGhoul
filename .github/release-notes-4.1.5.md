## 4.1.5 (HexGhoul): The General

HexKagune 4.1.5 for Forge 1.20.1. Not tested in game.

Your words: "a new "general" there talks to you and can be romanced, use one of the male skins currently available (NOT CHILDREN skins) and change it a bit to make it original, give him some muscles also less than the player, he can be talked to, romanced and teaches a new combat style (and uses it in self defense) Like ayato, ken, touka, eto and noro, he is an official character, means he will respawn." "Attitude: Tough, softens up when romanced, embarrased easily, proud of the CCG, has to be convinced to love a ghoul, Only interested in woman players." And then, with the skin: "use this skin for the general and give him all the formal outfit (but the hat)"

This version is the man himself: where he is, how he looks, his talk and his romance. **Kick Master (his fighting style, and him using it) is the next version**, and it gets all my attention from here.

### Who he is

- **General Kuroda**, the CCG's General. A human. Thirty years in the bureau.
- He's in the CCG building, in the north-west office on the top floor.
- An official character like the other five: there's one of him in a world, and a day after he's killed he's back in his office.
- `/kagune general here` brings him to you and makes that his home, as it does for the others (operators).

### How he looks

- **His skin is the one you sent**, pixel for pixel. It's kept as `art/exploration/General_skin.png`. So I did not take one of the mod's male skins and change it: yours replaced that plan.
- It's a slim-arms skin, so he's drawn on slim arms.
- **He wears the Formal Outfit's coat, trousers and shoes. Never the cap.**
- **Muscle:** his chest is 0.45 of the White Reaper's. Ayato's is 0.3 and a male player's is 0.6.
- He's a player's height. (The game's rule keeps every companion but Noro at that or under.)
- He blinks and blushes. His eyes never turn.

### How he talks

- Tough and clipped with strangers: orders, and two minutes of his time.
- Proud of the CCG: he knows every investigator's name and counts the night shift in every dawn.
- He softens as he comes to like you, and with the woman he loves he goes red to the ears at almost anything kind.
- He has his own topics (the CCG, his thirty years, how he fights, his missing cap, being a ghoul, his blushing), his own scenes and three dates of his own.

### Romance

- **Women only.** To a man he's a friend.
- His courtship has five chapters, each a reason for him to believe what he sees of you:
  1. Kill three wild ghouls preying on a village.
  2. Bring a village human to care for you (someone to vouch for you).
  3. Kill three ghouls of the Pack.
  4. Take him somewhere high, to see his district off the map.
  5. Be with him at dawn, when he counts the night shift in.
- **A ghoul has to convince him.** After the five chapters a human can ask him out and he says yes. A ghoul he turns down ("Not yet... Keep coming back. Convince me.") until his regard for her reaches 90 of 100. Talking to him each day, his paper, his cornflower and his letters all raise it.
- **A full mask hides you.** Behind a full mask (not a half mask) he takes a ghoul for a human, as the agents do.
- His errand item is paper. His flower is a cornflower.
- He never accepts meat as a gift.

### Among the CCG

- He never goes for one of his own investigators, and they never go for him.
- He fights whoever attacks him, and hostile mobs that come close.

### What isn't in yet, or is a compromise

- **He doesn't teach anything yet.** Asked, he says it took him thirty years and to ask another day. Kick Master is next.
- **He fights with the same bare-hands kit the other companions fall back on** until Kick Master replaces it next version.
- **Married life:** at home he uses Ken's household lines, as Ayato does. Some of those were written for a ghoul's kitchen. I'd rather give him his own later than rush them now.
- A child of his and a ghoul's inherits a kagune only from its mother.

### My guesses

- His name, and everything he says.
- The number 90 for convincing him.
- His office, his courtship's five tasks, his errand item and his flower.
- 0.45 for his build.

### Network and tests

- **Network:** protocol 114 (a new entity). Clients and the server must both be on 4.1.5.
- **CompanionTests (`general`):** his texture is your skin, every pixel; no eye of his turns; he wears the three pieces and never a head piece; his build is between Ayato's and a male player's; women only; a ghoul he isn't sure of gets no date; a full mask makes a ghoul a human to him; he's dressed as he wakes; he never targets an investigator.
- **Every companion check that runs over all of them** now runs over him too: his lines for every mood and moment, his courtship's scenes, his dates.
- **NpcTests:** he's in the building's template once, on the top floor.
- **The full suite.**

Validation: the full `python3 build.py` completed with EXIT 0 on these sources before this was pushed. Not tested in game.
