You're taking over HexGhoul, a Forge 1.20.1 Tokyo-Ghoul-inspired kagune mod (HexKagune), from another AI. Work on branch `claude/hopeful-noether-5c2wex`. Push only there; never push to main; no PR unless I ask.

Read `docs/handoff/next-ai-3.6.3.md` first and do what it says, in its order. Then read `docs/handoff/owner-words-mutations.md` (my words for the two Bikaku mutations, quoted exactly, with my pictures in `docs/handoff/owner-refs/`) and `docs/handoff/next-ai-3.5.4.md` (the house rules and my earlier words).

Dependencies: we are using GeckoLib as a dependency along with Photon and PlayerAnimator. Use any of these libs whenever needed to smooth out the mod as much as possible. GeckoLib is a requirement; its mods.toml entry is already in the work in progress, and the handoff says how to stub and allowlist it.

What to do:
1. Finish 3.6.3 (Grand Devouring and the Bikaku mutations). The server, the network and the drawing are written and compile. Still to do: the mutations in the Bikaku menu, the tests, then the version paperwork, a full `python3 build.py` to EXIT 0, commit, push. Ask me first whether the reshaped arch in `docs/handoff/bikaku-devour-3.6.3.png` is right.
2. Then 3.6.4: the victim's view from inside the mouth (with GeckoLib), and Grand Devouring in the kakuja.
3. Then Potential Unlock, built on Pale's "You Thought You'd Get Rid of Me That Easily?" pieces, with my kagune colour on the strands.

Follow the house rules exactly:
- Each fix or feature is its own version (the last number never reaches 10), with release notes, a README entry, the handoff sections, both version strings, a full build ending with EXIT 0, a commit and a push.
- Nothing is tested in game: never claim it.
- Answer me in English. Quote my words exactly as I wrote them; don't add things I didn't say.
- Show me previews of what you make before you go far.
- If I report bugs, fix those first, each its own version; if my exact words for a report aren't in the handoff, ask me.
- When a version is done, tell me plainly what you made, what you guessed at, and what's next.
