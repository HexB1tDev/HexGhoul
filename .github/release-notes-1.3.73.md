## 1.3.73 (HexGhoul): talk to the villagers, each with a name and a personality of their own (testing)
HexKagune 1.3.73 for Forge 1.20.1. Install on client and server (**protocol 72**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

You asked for "general conversation" and "different types of personalities" for the humans.

- **Right-click a human to talk.** The talk screen opens (the same one as the ghouls'), with their face, their **name**, and their **personality**: one of 8 (Cheerful, Shy, Bold, Kind, Grumpy, Flirty, Dreamy, Nervous), fixed for life. A frightened human won't talk ("Rin is too frightened to talk."). A villager you talk to stops trading, stands still and faces you.
- **What you can say:** "How are you?", "Tell me about yourself.", "Any news?" (village gossip: CCG patrols, red eyes in the fields, a market day...), "You look lovely today." and goodbye. Each villager answers in their personality's voice: a shy one goes red and stares at the floor, a grumpy one mutters, a flirty one flirts back, a dreamy one talks about the sky.
- **They remember you:** how warm each one is to you is kept per player (the same bond system as ghouls and companions). Greetings change as you become friendly and then close ("There you are! My day just got better."). A chat a day and a compliment a day make them warmer (flirty ones like compliments most, grumpy ones least). About 128 different lines.

Next: romance and dating, including **admitting you're a ghoul** to a human before a relationship can start (which may take convincing), and the humans' cuter looks.

Untested in game: compiled and unit tested (every personality's lines, the names). Try: right-click villagers in a Brick Village (or `/summon hexkagune:human`).
