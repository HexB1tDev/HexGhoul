## 4.1.74 (HexGhoul): Arima's change and death slowed; Dawn out of his hand; nothing touches him as he dies

HexKagune 4.1.74 for Forge 1.20.1. Not tested in game. Protocol unchanged (126).

Your words (quoted exactly): "bug found, while transforming arima keeps holding the old quinque instead of getting rid of it, the death
animation is too fast, during his death he should remain immortal and the blade slowly shatter, then he holds still for a bit then dies.
during this death animation he's 100% immortal, ungrabbable and all effects like bleed etc are removed from him." Then: "I think same for
the transformation animation, it goes by too quick, can barely see it".

- **Dawn leaves his hand as his change begins.** It stayed in his hand through the coat and the draw (what puts it away never ran while
  the change played). It is put away, not dropped: nobody picks up a free Dawn mid-fight, and it is still what he leaves when he dies.
- **The change slowed: 5.4 s to 9.5 s.** Every move of it (the coat pulled off and dropped, the golden case drawn from behind his back
  and opened, the blade out of it, the case dropped) plays 1.75 times slower. He is untouchable the whole time, as before.
- **His death slowed: 3.5 s to 8.5 s.** He lowers the blade, its point to the ground (1 s); holds it there, dead still, while it breaks
  a piece at a time from its point back to the hilt (3 s; a glass crack as each piece goes, quieter and lower as it goes, the old heavy
  sound with the last); his arms fall slack with the stub in his hand; he goes down on his knees slowly (1.2 s) and bows his head; then he
  holds still on his knees for 2 s, and dies.
- **100% immortal, ungrabbable, everything off him, through his whole death.** Every tick: every effect taken off him (bleeding
  included: its stacks and the spikes stuck in him too), his fire put out, any stun or knockdown gone. Nothing takes hold of him: on top
  of the holds already let go every tick (the kagune's tails, a Bikaku's, a Pale's, a Graft's, a quinque arm's), a companion's arm, a
  child's kagune and Ken's burst now pass him by, and the official NPCs no longer count him as a foe (Roma's tendrils could impale and
  hold him, and their worms grab him). No blow lands on him, as before.

### Choices of mine you may want changed

- Dawn leaves his hand at once (no animation of him putting it away).
- The whole change is slowed evenly, the coat's and the case's falls with it. They still fall about as fast as real things do (the case
  fell faster than anything could before); say if any part of it should stay quick.
- He holds the lowered blade dead still while it breaks; his knees start to bend a moment before his arms have finished falling.

### Other

- Renders: `docs/handoff/arima-change-4.1.74.gif` and `arima-end-4.1.74.gif` (real speed, a frame a world tick, three-quarter beside
  his right side), `arima-end-4.1.74.png` (his whole end), `arima-end-close-4.1.74.png` (the blade breaking, close), `arima-shed-4.1.74.png`
  (the change, labelled in the world's seconds).
- Tests: `AgentTests.reaperSlowed` (the change's length and clock; the end's order and lengths; the blade breaking point first, never a
  gap, never drawn where a piece has flown; every shard once; held still while it breaks, still on his knees before he dies, no jolt
  going down; the cut meshes; Dawn out of his hand, everything off him, nothing holding him); 22 rules sabotaged one at a time, each
  caught. The Reaper's tests no longer pass when a line they look for is missing (19 such checks were blind to it; one, "untouchable
  while he changes or ends", had checked nothing since 4.1.38): fixed.
