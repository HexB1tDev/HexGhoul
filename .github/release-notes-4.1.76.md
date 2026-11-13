## 4.1.76 (HexGhoul): Arima's blade shatters at once in his one hand, and he falls straight on his face

HexKagune 4.1.76 for Forge 1.20.1. Not tested in game. Protocol unchanged (126).

Your words (quoted exactly): "why tf does he hold off on that michael jackson ah pose????? and why does he hold his weapon like that when
it's breaking??" Then: "just make the blade instantly shatter  while he's holding it on one hand, and the fall straight on his face".

- **The blade shatters at once, in his one hand.** His end starts in his one-handed guard, the blade in his right hand; a tenth of a
  second later it shatters, all of it at once, its shards flying out round him, with one crash. No more lowering it with both hands, no
  more breaking a piece at a time.
- **He falls straight on his face.** His arm drops slack with the broken hilt, and he goes over forward from where he stands, stiff, about
  the front of his feet, faster and faster, and hits the ground face down with a thud (a second after the shatter). No kneeling: the lean
  you saw was his kneel (a body with no knees to bend reads from the side as a stiff lean), and it is gone.
- **He lies there a bit, then dies:** 3 seconds face down, still, the broken hilt flat beside his hand; he stays face down as he dies
  (never tipped onto his side), as in 4.1.75. His whole end: about 4 seconds (10 in 4.1.75).
- Untouchable, ungrabbable, everything off him, until the end, as before.

### Choices of mine you may want changed

- A tenth of a second in his guard before it shatters (the blow that brings his end lands first).
- His feet stay where he stood, so he lies his whole length (about two blocks) in front of where he fell.
- He keeps hold of the broken hilt; his last second (Minecraft's own death) still flashes red, as every mob's does.

### Other

- Renders: `docs/handoff/arima-end-4.1.76.gif` (his whole end at real speed, a frame a world tick, three-quarter beside his right side),
  `arima-end-4.1.76.png` (frame by frame, both sides).
- Tests: `AgentTests.reaperFallen` (his one-handed guard until it shatters; it shatters at once and he falls straight after; nothing
  held between the shatter and the ground; never knelt or bent back; over the front of his feet, kept where they stand; faster every
  tick; face down, flat, on the ground; still till he dies and after; the hilt flat beside him; never tipped on his side; the shards
  carried), `reaperEnd` (the whole blade, then all at once the stub and its shards; one crash); 14 rules sabotaged one at a time, each
  caught. 4.1.74's piece-by-piece break, lowering and kneel removed with their tests.
