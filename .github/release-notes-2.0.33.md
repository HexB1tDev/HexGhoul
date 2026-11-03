## 2.0.33 (HexGhoul): the White Reaper's ground tendril

HexKagune 2.0.33 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

The White Reaper has a new move only he uses: he raises Dawn and drives it point first into the ground, the spot where his prey stood is marked on the ground for 0.8 s, then a huge black tendril bursts up out of it. Step off the mark in time and it misses.

- Timing: 9 ticks raising and driving the lance in; the mark (a ring of dark smoke and red glints at the tendril's 1.6-block radius, red welling up inside it, more urgent as it comes) for 16 ticks; the tendril; he pulls the lance out at tick 30 to 36. He is rooted the whole time the lance is in the ground: that's the opening to punish him.
- The tendril: about 5.5 blocks tall, as thick as a man where it leaves the ground, black with dark red veins. It is drawn with the original mod's own Rinkaku tube, the same one used for the children's tentacles. It bursts out in two ticks, whips, and sinks back over 1.3 s. It hits everyone he's fighting who stands on the mark: 24 raw damage, thrown up and outward.
- When: now and then (5% a tick when ready) against prey 3.5 to 18 blocks away in sight, at most every 8.5 s, never during a swing, kick or lightning aim. A stun before the lance goes in cancels it.
- Pure rules in `npc/ReaperTendril` (tested). Pose previewed off-game (`docs/handoff/reaper-ground-2.0.33.png`, `tools/QuinquePreview.java <out> dawn ground`), and the tendril's line over its life (`docs/handoff/reaper-tendril-2.0.33.png`). The tube itself can only be seen in game. Protocol 77: a new swing value and two new effect kinds go over the wire, so client and server must both be on this version.
