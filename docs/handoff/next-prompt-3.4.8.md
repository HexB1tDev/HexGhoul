# Prompt for the next AI (paste this as the first message)

You're taking over HexGhoul, a Forge 1.20.1 mod (Tokyo-Ghoul-inspired kagune, companions, families). The latest work is on the branch
`claude/wizardly-hopper-h08wf2` at version 3.4.8 (protocol 93), plus an unversioned commit that starts the new Bikaku path. Start your
own branch from it:
`git fetch origin claude/wizardly-hopper-h08wf2 && git checkout -B <your-branch> origin/claude/wizardly-hopper-h08wf2`.

Before anything else, read:

1. `CLAUDE.md`, if there is one.
2. `docs/handoff/next-ai-3.4.8.md`: what the last session shipped, my words it went by, what's next, its judgment calls, and the house
   rules.
3. `docs/BIKAKU_PATH.md`: my whole spec for the new path word for word, what's built, the plan, and every place a path hooks into.
4. The top sections of `docs/handoff/companions-handoff.md`.

Follow the house rules exactly. Each fix or feature is its own version (the next is 3.4.9; the last number never reaches 10), with:

- release notes, a README entry and a new top handoff section;
- a full `python3 build.py` ending in EXIT 0;
- a commit with the session trailers, pushed right away (every pushed version becomes a release I download).

Keep the handoff current as you go, not only at the end. Nothing is tested in game: never claim it is. Answer me in English.

What I want next: my Bikaku path (the worm kagune with the mouth), built in the versions planned in `BIKAKU_PATH.md`, starting with
3.4.9: the path in the game with the worm drawn, alive, in my kagune colour, with its M1 bites. Keep it long, with its zigzags; I liked
that design. Leave the hammer and the whip as they are; they're good. Show me previews of what you make before you go far. If I report
bugs, fix those first, each its own version; if my exact words for a report aren't in the handoff, ask me. When a version is done, tell
me plainly what you made, what you guessed at, and what's next.
