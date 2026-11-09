You're taking over HexGhoul, a Forge 1.20.1 Tokyo-Ghoul-inspired kagune mod (HexKagune), from another AI. Work on branch `claude/hopeful-noether-5c2wex`. Push only there; never push to main; no PR unless I ask.

Read `docs/handoff/next-ai-3.6.6.md` first (its top section, "The session after this note", is the newest state) and do what it says, in its order. Then read `docs/handoff/owner-words-mutations.md` (my words for the two Bikaku mutations and everything I said after, quoted exactly, with my answers to your questions) and `docs/handoff/next-ai-3.6.3.md` and `docs/handoff/next-ai-3.5.4.md` (the dependencies, the house rules and my earlier words). The top sections of `docs/handoff/companions-handoff.md` (one per version, newest first) have the file-level detail.

Dependencies: we are using GeckoLib as a dependency along with Photon and PlayerAnimator. Use any of these libs whenever needed to smooth out the mod as much as possible. GeckoLib is in use (Grand Devouring's inside); the handoff says how its stubs and allowlist work.

There is no RC bar. Energy and food are one bar.

What to do:
1. If the version the handoff says is in progress isn't pushed yet, finish it (a full build to EXIT 0, commit, push).
2. Potential Unlock's tearing-off and the strands in my kagune colour pulling each part back. Its preview was sent to me (`docs/handoff/potential-reattach-preview.png`, made from `docs/handoff/potential-prototype/`). Ask me what I think of it before you build it, and fix what I say.
3. Then the NPC's quest that unlocks the mutations, when I tell you.

Follow the house rules exactly:
- Each fix or feature is its own version (the last number never reaches 10), with release notes, a README entry, the handoff sections, both version strings, a full build ending with EXIT 0, a commit and a push.
- Nothing is tested in game: never claim it.
- Answer me in English. Quote my words exactly as I wrote them; don't add things I didn't say.
- Show me previews of what you make before you go far.
- If I report bugs, fix those first, each its own version; if my exact words for a report aren't in the handoff, ask me.
- When a version is done, tell me plainly what you made, what you guessed at, and what's next.
- Keep the handoff and this prompt current and pushed with every version (I said: "as soon as the weekly limit touches 99% push a handoff and a prompt to continue with another AI.").
