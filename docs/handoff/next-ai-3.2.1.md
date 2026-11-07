# HANDOFF: HexGhoul after 3.2.1. Read this first.

Then, for the house rules and history: `docs/handoff/next-ai-3.1.9.md` (the 3.1.5 to 3.1.9 session, its withheld work and judgment calls,
still valid), `docs/handoff/next-ai-2.0.45.md` (header: versions, protocol history, the 2.x/3.x log), `docs/handoff/next-ai-1.3.60.md`
(the owner's house rules in full), and the top sections of `docs/handoff/companions-handoff.md` (one per version, newest first: 3.2.0 and
3.2.1 are this session's, with file-level detail).

## Where things are

- **Branch:** this session's work is on **`claude/magical-fermi-cec8n1`** (pushed; 3.2.0 and 3.2.1 on top of 3.1.9 from
  `ccr-d6106b24-xl17h5`). The next session will be given its own branch: start it from `origin/claude/magical-fermi-cec8n1`
  (`git fetch origin claude/magical-fermi-cec8n1 && git checkout -B <your-branch> origin/claude/magical-fermi-cec8n1`). Push only to the
  branch you are given; never to `main`; no PR unless asked.
- **Version 3.2.1** in `build.py` and `patch/KagunePatch.java`. **The next is 3.2.2** (the last number never reaches 10). **Protocol "92"**
  (unchanged this session).
- Full `python3 build.py` ended **EXIT 0** for both versions (javac `--release 17` on JDK 21; about 5 minutes). **Nothing is tested in game.**
- The quick loop: `python3 tools/quick_check.py [--noverify] FamilyTests SceneTests DialogueTests` (needs one full `build.py` first).

## What this session shipped (each its own version)

| Version | What | Key files |
|---|---|---|
| 3.2.0 | **The word at the door** when Eto, Touka or Ken brings a child home: from one line each to the parent knowing *why* (wandered, night, rampage) by parent and age, the child answering in its temper about that, the parent's answer to that kind of child, sometimes the child's last word. About 350 lines. | `Parenting.home`, `ParentingLines` (door/doorreply/answer/last), `ParentDuty`, `TalkCues` HOME, FamilyTests `doorTalk` |
| 3.2.1 | **The player bringing a child home**: carried in or following, after 45 s out. Its other parent at home meets you at the door (12 scenes each: found, found at night, outing, outing at night, carrying by day/asleep, you hurt, jealous, sleepy, clingy, after visiting your other partner, co-parents who are just friends) while the child calls out to them; with nobody waiting, the child talks to you itself (58 scenes by age x temper x found/outing, plus night/sibling/other-parent/hurt). About 950 lines. | `family/Homecoming`, `HomecomingServer`, `ChildBrain` home check, `SceneServer` (`approachBrought`, `approachBack`, flags), `SceneWords` (`brought`, `found`, `carrying`, `back`), `talk/*.txt`, SceneTests `homecomings`, FamilyTests `homecoming` |

## Judgment calls the owner hasn't confirmed (tell them, ask if unsure)

- **3.2.0:** the parent's answer is chosen by the child's temper, not its exact reply, so each was written to follow any reply that temper
  gives; reason-aware door lines 70% of the time (the old generic ones 30%); the child's last word 55%.
- **3.2.1:** a homecoming needs 45 s out, at most once per child per 5 minutes, and its scenes stay possible for a minute after. "Found" vs
  "outing" is decided by who the child went out with: a child that went out with you, that you left outside and came back for, still reads
  as an outing. The companion must see you within 12 blocks (the usual approach rule), or the child talks instead. A companion who was out
  with you (following) never gets the "where were you two?" scenes. Friends-tier co-parents get one neutral scene each.

## Things to watch when the owner plays (all untested in game)

- Whether homecomings fire at all in practice: the child's home check runs every 20 ticks and needs a family home (`Households.forChild`);
  no home, no homecoming.
- The companion's card and the child's call-out bubble at the same moment (the bubble is queued 8 ticks after the card).
- Frequency: if walking in and out of the yard with a child following feels naggy, raise `Homecoming.AWAY` or `AGAIN`.

## Ask the owner what to deepen next (offered at the end of this session)

1. Siblings at home reacting to the one brought back ("where WERE you?", the little one tattling, the big one covering for it).
2. The companion and the child talking to *each other* at the door after the player's scene (a Conversation, as `BlendedServer` does).
3. Letting the companion's door scene depend on the child's temper (e.g. Touka to a bold kid you found vs a shy one), with new companion
   flags for the brought child's temper and age.
4. The reverse: the child greeting the player who comes home after days away (`away`), by temper.

## House rules (unchanged; full text in `next-ai-1.3.60.md`, summary in `next-ai-3.1.9.md`)

Each fix or feature its own version: both version strings, `.github/release-notes-<v>.md`, a README entry, a new top section in
`companions-handoff.md`, the header lines of `next-ai-2.0.45.md`, full `python3 build.py` to EXIT 0, commit (`3.x.y: ...` with the session's
trailers, no model names in anything pushed), push. Owner's bug reports first. Banned in the jar: Kaneki, Ayato, Furuta; in visible text:
Takatsuki, Aogiri, Yoshimura. Answer the owner in English, plainly; say what is untested and what was approximated.
