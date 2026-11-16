## 1.3.49 (HexGhoul): parenting: the companion parent fetches a wandering child, stops a rampage, and scolds it, back and forth in their bubbles (testing)
HexKagune 1.3.49 for Forge 1.20.1. Install on client and server (**protocol 68**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

- **Wandering off.** Touka, Ken or Eto notices when one of their children has gone too far from home:
  - **How far is too far:** a toddler can barely leave the yard (4 blocks past the home's edge), a child 16, a teen 40.
  - **When they notice:** a child in their sight within 40 blocks, or one that's been gone from home about 30 seconds.
  - **What they do:** they stop what they're doing (a chore, a cuddle) and go after it. A toddler gets **picked up and carried home**. An older child **walks home at the parent's side**.
- **Out at night.** At night the limits shrink: a toddler mustn't leave the home at all, a child barely past the yard, a teen 24 blocks. A sleeping parent **wakes up** when a child has been out in the night a while (or is in trouble) and goes looking.
- **Rampages.** If a child keeps fighting with its kagune out for too long (30 seconds, a minute for a teen), chases its prey too far from home, or keeps going badly hurt, the companion parent **runs to it and stops it**. The child holds back from fighting for a minute after, and the parent deals with what's left.
- **They talk it out.** When the parent reaches the child, they have a real exchange, back and forth in their speech bubbles, each line up long enough to read. The parent scolds, the child answers in its own temper, the parent explains why, the child answers again, and the parent has the last word. At the door, one more line each.
  - **Who says what:** Touka is blunt and fierce and soft underneath, Ken is gentle and anxious and firm when he has to be, Eto is theatrical, teasing and a little eerie. Each child talks like its temper (sunny, shy, bold, curious, mischievous, gentle or serious), and toddlers talk like toddlers.
  - **How many lines:** 579, which combine into thousands of different conversations.
- **When their kagune comes out**, a parent nearby has a word about it ("Careful. Remember: you're in charge of it. Not the other way round."), and the child answers.
- A parent never leaves its own fight for any of this, and puts a carried toddler down if a fight finds it. Children following you or carried by you are left alone.

Untested in game: compiled and unit tested only. Try: let a toddler wander past the garden with its mother home, or let a taught child fight a zombie for half a minute.
