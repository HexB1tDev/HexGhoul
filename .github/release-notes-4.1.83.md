## 4.1.83 (HexGhoul): every official back at her own spot; Banjou in his bedroom

HexKagune 4.1.83 for Forge 1.20.1. Not tested in game. Protocol unchanged (126).

Your reports, quoted exactly: **"Gas mask guy is ALSO not spawning in his room, or anywhere"** and **"and I'm 99% sure the general of the
CCg isnt spawning either"**. Asked which is Banjou's room (the plan of his cottage shown), you chose "Upstairs bedroom (Recommended)".

- **Every official has her own spot now**, where her place's template puts her: Eto in the Library, Touka behind Anteiku's counter, Ken on
  Anteiku's roof, Roma in the middle of the ring, Banjou in his bedroom, Ayato in the Aogiri hideout's lounge, Noro in his top-floor suite,
  the General in the boss's office on the CCG tower's top floor. The spot is read from the place as your world built it (where it stands
  and which way it's turned), so places already built in your worlds have it too.
- **Half a minute within 24 blocks of her spot with none of her in her place, and she is made there**, facing the way her place faces,
  and any body of hers elsewhere is sent away. Not if she's out with someone (following you, holding your hand), left waiting somewhere
  by a player (she goes home herself after a day alone, as before), or married (she lives where you made your home). 4.1.82 did this for
  Roma only.
- **One already in her place but whose home isn't her spot is given her spot as home and walks there.** So a Banjou already in your
  world, downstairs, goes up to his bedroom.
- **Dead, she comes back to her spot** a day later (unless married).
- **Banjou's place in the template moved** to his bedroom upstairs, beside the foot of the two magenta beds, facing into the room (new
  dens have him there from the start).

### How `/kagune <name> here` works (and why they were missing)

`/kagune <name> here` brings that official to where you stand: the one the world has if she's loaded, else a new body. Until now it also
made that spot her home for good ("<name> lives here now."). Once you had met her, her own place's body was sent away the moment it loaded,
since only one of her lives, so she lived wherever the command was used. That is the likeliest reason Roma, Banjou and the General weren't
in their places. Now the command says "<name> is here now. Her own place is still her home: once someone is there half a minute without
her, she is made there again and this one leaves." (his/him for the men). For an official married to someone it still says "lives here
now", as her home is the one you share.

### Other

- Renders: `docs/handoff/banjou-room-4.1.83.png` (his cottage from its template: both floors, his old spot and his bedroom, and a side
  section through his row: him on the upstairs floor by the bed) and `docs/handoff/officials-4.1.83.png` (all eight at their spots, each
  from her own place's template at her feet's height). `tools/floor_plan.py` draws them.
- Found on the way: the template baker (`tools/companion_structures.py`) couldn't bake the circus since 4.1.73 (a name reused); fixed. The
  other three templates baked again are byte for byte the same.
- Tests: `CompanionTests.officials483` (each spot read back from its place's built template, turned, its facing; the keeper's rules); 34
  rules sabotaged one at a time (`docs/handoff/sabotage-4.1.83.json`).
