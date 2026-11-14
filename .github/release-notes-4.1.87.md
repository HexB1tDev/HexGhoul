## 4.1.87 (HexGhoul): Kick Master's right click: a charged dash kick that sends what it hits flying to your left

HexKagune 4.1.87 for Forge 1.20.1. Not tested in game. **Protocol 127** (was 126): a server and its players need this version together.

You said (quoted exactly): "we need to redo the Kickmaster combat right click ability, which is basically we just copy pasted the V
ability, which is the kick move. The right click should be a chargeable dash kick, not a teleport, none of that, just like a dash. And as
soon as you reach the location, you kick. Whatever's in the way is kicked to the left of the player, like hard to the left of the player.
It goes basically flying, just like the V move sends you the enemy flying ragdolled." and "Get a location, the left leg cocks, I mean,
sorry, the right leg cocks back, and then it just kicks, it goes upwards and leftwards, sort of, to the height of like the enemy's head,
pretty much, kind of around that height" and "Make sure that the head is also proportional. It doesn't rotate out of place." Asked, you
answered: "Actually, just changed my mind. He doesn't even do it 360. He faces where he kicked at. So basically, he ends up facing to the
left." and, for what it aims at, "A spot, and the dash's path too".

The old right click (4.1.8's "Twirling Speed Kick") vanished and was there out of nowhere, then threw the V kick. It is gone. Now:

- **Hold right click to charge** (full in 1.25 s; held 4 s it goes by itself), **let go: a real run along the ground**, no vanishing:
  8 blocks at the least charge, 20 at full, in at most 0.4 s, fastest off the mark and slowing into the spot.
- **Where to:** the body nearest your crosshair within reach (you stop just in front of it); else the spot where your crosshair meets a
  block within the charge's reach; else as far as the charge reaches. On the ground the whole way: up a half step (a slab, a stair), down
  a drop; a wall or a whole block up stops the run short of it.
- **On the way, whatever you run into is kicked aside to your left** (a lighter blow than the kick's), each body once.
- **At the spot, the kick:** your right leg cocks back, then sweeps up and to your left, through about the head height of what you aimed
  at (you hop up into it for a tall one, at most 1.2 blocks), and carries on round to your left. Your body turns with it a quarter turn
  left, so **you end facing where it flew**, your foot comes down and you are in your guard. The head stays level, square on the
  shoulders, turning with the body.
- **What the sweep reaches** (in front of you and out to your left, not behind you or far to your right) **is kicked hard to your
  left**, the way the V kick launches: flying, tumbling, smashing through what it hits, down a moment after. The charge sets how hard (30%
  at none, all of the V kick's at full).
- Nothing hurts you from letting go until the blow lands. 6 RC as before (nothing for a human), 2.5 s before the next.
- In first person your view turns the quarter left with the kick (as the mouse would turn it), and you see your own leg.
- The General and Roma, who fight as Kick Masters, throw it too: they run in, kick their target to their left and turn to face it.
- The ability guide calls it "Dash Kick (hold, let go)". It clashes as a kick.

### Choices of mine you may want changed

- The turn is a quarter turn left (90 degrees), so you face the way it flew.
- An NPC's dash kick hits only its target, not what is on its way.
- No dust or trail on the run.

### Other

- Renders: `docs/handoff/dash-kick-4.1.87.png` (from the side, her front-left and above: the coil, the run, the leg cocked back, the
  sweep, the blow at the head, through to her left, turned, down, her guard; a body of a player's height where it stood, launched left)
  and `docs/handoff/dash-kick-close-4.1.87.png` (the kick every half tick from behind and in front). `tools/PalePreview.java <dir>
  dash-kick`.
- Tests: `CompanionTests.dashKick487`; 48 rules sabotaged one at a time (`docs/handoff/sabotage-4.1.87.json`).
