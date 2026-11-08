# Prompt for the next AI (paste this as the first message)

You're taking over HexGhoul, a Forge 1.20.1 mod (Tokyo-Ghoul-inspired kagune, companions, families). The latest work is on the branch
`claude/exciting-bell-3kklnc` at version 3.4.6 (protocol 93). Start your own branch from it:
`git fetch origin claude/exciting-bell-3kklnc && git checkout -B <your-branch> origin/claude/exciting-bell-3kklnc`.

Before anything else, read `CLAUDE.md`, then `docs/handoff/next-ai-3.3.5.md` (what the last session shipped, my words it went by, what is
still open, its judgment calls, how to get sounds, and the house rules), then the top five sections of `docs/handoff/companions-handoff.md`.
Follow the house rules exactly: each fix or feature is its own version (the next is 3.4.7; the last number never reaches 10), with release
notes, a README entry, a new top handoff section, a full `python3 build.py` ending in EXIT 0, a commit with the session trailers, and a push
right away (every pushed version becomes a release I can download). Keep `next-ai-3.3.5.md` (or a newer handoff you write) up to date as you
go, not only at the end. Nothing is tested in game: never claim it is. Answer me in English.

If I report bugs, fix those first, each its own version; if my exact words for a report aren't in the handoff, ask me. The "Still
open" list in that handoff is done (3.2.7 to 3.4.2; 3.4.3 fixed my clash report, 3.4.4 the crystal forming sounds, 3.4.5 the Spire Wave, 3.4.6 the move guide redo): ask me what to do next, and tell me the judgment calls it lists. Never make sounds yourself: get real recordings from the web as the handoff says. Write your own
lines in each character's voice; don't just repeat my words back. When it's done, tell me plainly what you made, what you guessed at, and ask
what to do next.
