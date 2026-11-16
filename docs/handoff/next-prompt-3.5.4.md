You're taking over HexGhoul, a Forge 1.20.1 mod (Tokyo-Ghoul-inspired kagune, companions, families). The latest work is on the branch
`claude/peaceful-ramanujan-eldmlu`: released up to 3.5.3 (protocol 96), plus an unversioned commit with the Bikaku kakuja started (not
released, not full-built, protocol 97 in the tree). Start your own branch from it:
`git fetch origin claude/peaceful-ramanujan-eldmlu && git checkout -B <your-branch> origin/claude/peaceful-ramanujan-eldmlu`.

Before anything else, read:

1. `CLAUDE.md`.
2. `docs/handoff/next-ai-3.5.4.md`: what the last session shipped, my words it went by, the unfinished kakuja, what's next, its judgment
   calls, and the house rules.
3. `docs/BIKAKU_PATH.md`: my whole spec for the Bikaku path word for word, the plan, and every place a path hooks into.
4. The top sections of `docs/handoff/companions-handoff.md`.

Follow the house rules exactly. Each fix or feature is its own version (the next is 3.5.4; the last number never reaches 10), with:

- release notes, a README entry and a new top handoff section;
- a full `python3 build.py` ending in EXIT 0;
- a commit with the session trailers, pushed right away (every pushed version becomes a release I download).

Keep the handoff current as you go, not only at the end. Nothing is tested in game: never claim it is. Answer me in English. Quote my
words exactly as I wrote them; don't add things I didn't say.

What I want next: first show me the kakuja previews (`docs/handoff/bikaku-kakuja-3.5.4.png` and `bikaku-kakuja-close-3.5.4.png`) and ask
me what I think before you ship it; fix what I say, then ship it as 3.5.4. Then the kakuja's four abilities and its passives (3.5.5), all
opened by mastery alone (a custom NPC will teach them later). Leave the hammer and the whip as they are. Show me previews of what you make
before you go far. If I report bugs, fix those first, each its own version; if my exact words for a report aren't in the handoff, ask me.
When a version is done, tell me plainly what you made, what you guessed at, and what's next.
