## 4.1.92 (HexGhoul): Kick Master's right click animates again (the charge and the dash kick)

HexKagune 4.1.92 for Forge 1.20.1. Not tested in game. Protocol unchanged (127).

You said (quoted exactly): "One bug, the right-click animation is not working for the kick of the Kickmaster. It sends stuff flying,
yeah, sure, but there's no running towards them animation, and there's no kick animation at all. Like, you just walk towards them, like,
without moving, that's it. There's no kick animation, there's nothing. For some reason, the one you made is no longer, I mean, it's not
playing."

**Why:** her body only takes a move the animator is told to animate, and the charge (right click held) and the dash kick (let go) were
never on that list. Their poses were made, rendered and tested on their own, but in game the animator never put them on her, so she
slid there in her stance while the server did the rest (the dash, the hit, the launch). That was my miss in 4.1.87: the renders were
drawn straight from the poses, not through the animator the game uses.

- **The charge:** held, she sinks side-on into the coil, facing where you aim.
- **The dash kick:** let go, she leans off the mark, really runs at them, cocks the right leg back, sweeps it up and to her left through
  the head, turns with it and lands in her stance facing left.
- **The General and Roma** (NPC Kick Masters) use the same animator: they were sliding too, and now run and kick as well.

### Other

- Render: `docs/handoff/dash-kick-drawn-4.1.92.png`: the charge and every tick of the dash kick, side on and from her front-left, drawn
  through the game's own animator (`tools/DashDrawnPreview.java`), so a render can't pass while the game shows nothing again.
- Tests: `CompanionTests.dashKickDrawn492` drives the real animator through the charge and the whole dash kick and checks her body takes
  exactly those poses (the run's legs, the kick's height), and that every move the animator has a pose for is let through; 4 rules
  sabotaged (`docs/handoff/sabotage-4.1.92.json`), all caught.
