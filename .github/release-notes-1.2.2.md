HexKagune 1.2.2 for Forge 1.20.1 (clients need PlayerAnimator, bendy-lib and Photon). Install on client and server (protocol 52). Testing build.

- **Your sound pack:** every sound the mod plays now comes from your Bizarre SFX pack, 110 of its 120 files, grouped into pools so each hit picks a variant.
- **Mono conversion:** all 120 files were converted to mono, since stereo sounds in Minecraft play at full volume for everyone, wherever they are.
- **Loudness evened out:** each file's volume is balanced from its measured loudness; frequent sounds (swings, footfalls) are quieter and the big collisions are loud.
- **Original sounds replaced:** all 16 of the original mod's sound events (whip, slam, bone crack, rip, guard, grab, idle writhe and the rest) now draw from your pack.
- **Vanilla sounds replaced:** every vanilla sound the moves used is now one of yours, picked by job: explosions become bursts, shockwaves or ultimates by how big they are, wing flaps become air bursts, fire shots become spike throws, crystal becomes spikes and shatters.
- **Natural pitches:** pitches are pulled back toward each sound's own pitch, so your SFX aren't slowed to mud or chipmunked.
- **King ritual:** War's and Peace's awakening and departure were rewritten as one sound per beat, instead of four or five vanilla sounds stacked.
- **Anti-spam guard:** the same sound landing on itself within 50 ms plays once, at most 3 of one sound play at once in a spot, and at most 14 of the mod's sounds start together.
- **King footfalls:** War's and Peace's runs now make a footstep on every real stride (heavy for War, light for Peace).
- **Kept as is:** the kakugan's eye-open sound (already yours) and the quest journal's UI cues.
- **Not used yet:** the 4 barrages and 6 run loops; the barrages would double the per-hit sounds and the loops' fixed tempo can't match a King's stride.
- **Peace first person fixed:** Peace's blades, M1 and abilities were never drawn in first person; the code read the wrong clock, so Peace always seemed not yet transformed.
- **War first person fixed:** the same clock bug also hid War's first-person rinkaku and arms.
- **Final Horizon height:** the target is lifted 6 blocks instead of 12 and hangs about 5 blocks in front of you, not over your head.
- **Final Horizon drive:** you now stay where you are; the tentacles drive the target down a steep line and into the ground instead of dragging you after it.
