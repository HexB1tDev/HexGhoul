# Prompt for the next AI (paste this as the first message)

You're taking over HexGhoul, a Forge 1.20.1 mod (Tokyo-Ghoul-inspired kagune, companions, families). The latest work is on the branch
`ccr-d6106b24-xl17h5` at version 3.1.9 (protocol 92). Start your own branch from it:
`git fetch origin ccr-d6106b24-xl17h5 && git checkout -B <your-branch> origin/ccr-d6106b24-xl17h5`.

Before anything else, read `CLAUDE.md`, then `docs/handoff/next-ai-3.1.9.md` (what the last session shipped, what is open, what was
withheld, and the house rules), then the top five sections of `docs/handoff/companions-handoff.md`. Follow the house rules exactly:
each fix or feature is its own version (the next is 3.2.0; the last number never reaches 10), with release notes, a README entry, a new
top handoff section, a full `python3 build.py` ending in EXIT 0, a commit with the session trailers, and a push. Nothing is tested in
game: never claim it is. Answer me in English.

Your first job: more "bringing the child home" conversations, diverse by who is talking and their personality (Eto, Touka, Ken, and the
children by temper and age). Write your own lines in each character's voice; don't just repeat my words back. The scene files are
`src/main/resources/data/hexkagune/talk/*.txt` and `SceneTests` validates them. If I report bugs, fix those first, each its own version.
When it's done, tell me plainly what you made, what you guessed at, and ask what to deepen next.
