## 1.3.67 (HexGhoul): new, more dramatic runs for War and Peace (testing)
HexKagune 1.3.67 for Forge 1.20.1. Install on client and server (**protocol 71**, unchanged; client-side animation only). Testing build: compiled, tests pass, **not tried in game**.

You asked for "new more dramatic running animations for both War and peace".

- **War charges like a beast.** Thrown forward almost flat, the head thrust out ahead, glaring along the ground. Between footfalls both claws reach far out ahead together, and as each foot slams down they rake back, so he drags himself forward with every stomp. The whole body rolls and lurches onto the side he lands on, and every stomp drops him and jolts his head. Walking, his claws still pump wide and low as before.
- **Peace glides.** Tipped so far forward he's nearly flat, his head held dead level with his eyes on what he runs at. Both arms are flung back and spread wide like swept wings, lifting a little at the top of each stride. Long bounding strides with the knee driven high, a hang in the air between them, landing light, and a slight bank into each one.
- **Peace's blades keep up:** while running, their points lift against the deeper lean (the lower pairs a little more), so they stay level ahead of him like lances. Measured off-game through a whole sprint, the lowest blade stays 0.14 blocks above the ground (at first the new lean put it 0.4 blocks into the ground).
- **Ken's Peace runs too.** Before, his legs only did the plain walk. He now glides like a player in Peace, his stride taken from his own legs' swing.
- Everything that used the runs still does: your footstep sounds, blows thrown while running (the legs keep running underneath), the sprint afterimages.

Approximations: the body can only tip so far (the rig's limit), so "nearly flat" is about 60 to 70 degrees over. Ken's stride comes from the vanilla walk swing, which tops out sooner than a player's stride, so his legs may turn over a little slower than his speed.

Untested in game: compiled and unit tested, and checked frame by frame on the off-game preview. Try: sprint in War, then in Peace, from the side and behind.
