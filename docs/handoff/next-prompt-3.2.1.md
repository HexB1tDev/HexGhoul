# Prompt for the next AI (paste this as the first message)

You're taking over HexGhoul, a Forge 1.20.1 mod (Tokyo-Ghoul-inspired kagune, companions, families). The latest work is on the branch
`claude/magical-fermi-cec8n1` at version 3.2.1 (protocol 92). Start your own branch from it:
`git fetch origin claude/magical-fermi-cec8n1 && git checkout -B <your-branch> origin/claude/magical-fermi-cec8n1`.

Before anything else, read `CLAUDE.md`, then `docs/handoff/next-ai-3.2.1.md` (what the last session shipped, its judgment calls, what to
watch, and the house rules), then the top five sections of `docs/handoff/companions-handoff.md`. Follow the house rules exactly: each fix
or feature is its own version (the next is 3.2.2; the last number never reaches 10), with release notes, a README entry, a new top
handoff section, a full `python3 build.py` ending in EXIT 0, a commit with the session trailers, and a push. Nothing is tested in game:
never claim it is. Answer me in English.

If I report bugs, fix those first, each its own version. Otherwise, do what I picked from the "what to deepen next" list in that handoff
(if I haven't picked, ask me). Write your own lines in each character's voice; don't just repeat my words back. When it's done, tell me
plainly what you made, what you guessed at, and ask what to deepen next.
