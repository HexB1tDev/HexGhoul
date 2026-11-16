# HANDOFF: HexGhoul after 1.3.60. Read this first, then the top sections of `docs/handoff/companions-handoff.md`.

Branch **`claude/relaxed-carson-4y0frw`** (check it out from origin; push with `git push -u origin claude/relaxed-carson-4y0frw`).
Never push to `main`; no PR unless asked. Version **1.3.60** in `build.py` and `patch/KagunePatch.java` (bump both; the next is
**1.3.61**). Network `PROTOCOL` is **"70"**. Nothing since 1.3.28 has been tried in game: never say anything works in game.

## The goal (the owner's words, unchanged since 1.3.51)

> "The next goal is to increase the amount of interactions and dialog, focusing mainly on the beauty of interactions and making it as
> bug free as possible, prevent NPCs from walking away during conversations for example ... or speech bubbles overlapping each other
> during squabbles."

And in the session that made 1.3.52 to 1.3.60: "Start by fixing the problems listed in the handoff, each its own version ... After that, add
more interactions and dialog (gestures and poses with the lines, group scenes, hide and seek, babies in sibling moments...), keeping every
line set tested and in character. If I report bugs, fix those first, each its own version."

## House rules (the owner's, verbatim where quoted)

- "Rules: nothing is tested in game, never claim it is. Look at your own previews honestly and match my marks exactly. Banned names in
  the jar: Kaneki, Ayato, Furuta. Banned in visible text: Takatsuki, Aogiri, Yoshimura. Sounds only from the hexkagune: pack. Never
  alter the owner's skins (only the generated overlays). Any new Minecraft API reference needs an entry in verify/api-allowlist.txt with
  [srg] or [order] evidence plus a stub; stale entries also fail the build. Append only to ordinal-sent enums."
- SRG lookups: `python3 tools/find_srg.py <yarn class> <member>`; obf names and ids in `build/mapping-cache/joined.tsrg` and
  `intermediary-1.20.1.tiny`. This session needed no new API: everything used was already allowlisted (ClipContext, BlockState collision
  shapes, `BlockPos.m_274561_`, `m_146892_` eye position, the client's `m_91296_` partial tick, `f_19790_..2_` old positions). Note: the
  stubs have no `BlockPos.below()/above()`; build neighbours with `BlockPos.m_274561_(x, y +- 1, z)`.
- Every fix or feature ships as its own version: bump `build.py` VERSION and `patch/KagunePatch.java` VERSION, write
  `.github/release-notes-<version>.md`, add a README entry (same shape as the others), add a new top section to
  `docs/handoff/companions-handoff.md` (rename the old "START HERE" to "State after ..."), run the full `python3 build.py` to EXIT 0
  (about 4 minutes; run it in the background and wait on the log's `EXIT` line), commit as `1.3.xx: ...` with the trailers
  `Co-Authored-By: Claude <noreply@anthropic.com>` and the session's `Claude-Session:` line, no model names anywhere, then push. Stage
  the version's files explicitly; never commit scratch files. Don't edit sources while a full build runs.
- Bug reports from the owner come first, each its own version. Answer the owner in English only. Tell them plainly about judgment calls
  and approximations (this session: "hands on hips" without elbows, a toddler's counting hands only reaching its cheeks).
- The quick loop: `python3 tools/quick_check.py FamilyTests NpcTests` (compile + API verify + suites, ~40 s; `--noverify` faster).
- Count the lines you claim in release notes (a short python regex over the Java map does it); this session caught two wrong counts that
  way before they shipped.

## What this session shipped (details: the top sections of `companions-handoff.md`)

- **1.3.52** bubbles take turns (`S2CNpcSay` carries the bubble's life; protocol 69): a talk's line comes down as the reply goes up,
  shrinking away; `Parenting.pause` a little longer; other bubbles said at once spread apart sideways on the client (`npc/BubbleLayout`),
  tails leaning back to their speakers. Preview: `tools/BubblePreview.java` writes `bubbles-scene.png` (before/after).
- **1.3.53** one conversation lock (`server/Conversation`, pure `family/Talks`): every held family talk (fetching a child, the word at the
  door, siblings' talks and squabbles, a parent breaking one up, a big sibling minding a toddler) keeps its members standing, bodies turned
  to the speaker, nothing else starting; the starter walks up first; a parent comes over to a squabble; it ends cleanly on hurt, fight,
  sleep, gone, walked off (a cut-short line), a player wanting one of them.
- **1.3.54** tag stays in the yard (`Siblings.runTo`). **1.3.55** one escort per toddler (`Siblings.standDown`, 38 lines).
- **1.3.56** Eto's child posed for her flesh's moves (`client/animation/ChildFight`, the same `PaleAnimator` as hers).
- **1.3.57** gestures with every line (protocol 70: `S2CChild` carries a `ChildCue`): 15 child talk poses (`client/animation/ChildTalkPose`),
  companions' own gestures, chosen by `family/TalkCues` from what the line says, else the talk and the temper. Preview: `tools/TalkPreview.java`.
- **1.3.58** hide and seek (`family/HideAndSeek`, 80 lines; in `SiblingLife`; poses HIDE and COUNT).
- **1.3.59** big siblings play with the baby (`family/LittleOnes`, 98 lines: peekaboo, faces, baby talk, an evening hum).
- **1.3.60** review fixes (a talk with nobody left ends; a following companion never breaks up a squabble).

## How family talk works now (read before changing it)

- **Held talks are `Conversation`s.** Members by entity id, an `anchor` who stays put, `coming` members who walk up first (to `Talks.GATHER`
  2.2), `Line(speaker, text, ChildCue cue, EtoGesture gesture)`s said in turn: each when its pause is over and its speaker is within
  `Talks.SPEAKS` 4 of the anchor (else it waits up to `WAITS`, then says it from where it is within `LEAVES` 8, else that speaker is dropped).
  Sent as `S2CNpcSay(speaker, text, life)` with `Speech.turn` (the bubble lives until someone else's next line). Each member's mind holds
  the same object (`ChildBrain.Mind.conversation`, `EtoBrain.Mind.conversation`); whichever member's brain asks first ticks it
  (`Conversation.going`), then `Conversation.hold` stands it (navigation stopped, its leash pinned where it stands, body turned
  `Talks.TURN` a tick toward `faces(...)`) or walks it up. Start with `Conversation.start(...)` (null if a member is busy in another),
  check members with `Conversation.free(...)`, add a late joiner with `join(...)`.
- **Remarks are not held**: a word in a fight (a parent's TRANSFORM word, a sibling's REACT/TEAM), tag shouts, hide-and-seek lines, a
  stand-down: queued on a child's `Mind.talk` (`ParentDuty.Said(speaker, text, at, cue)`), said by `ParentDuty.talk` each tick, with the
  same bubble life rule and the cue applied as said.
- **The bubble** (client): `NpcSpeech` keeps `until` from the sent life; `SpeechBubbles.place` lays every bubble in sight out once a frame
  (`BubbleLayout.arrange`: angles on the view, pairs spread apart half each toward their speaker's side, `tilt`, a crowd stacked) and eases
  moves; `BubbleShape.of(width, lines, thought, shift, lift)` draws the moved box with its tail (a cloud's trail) back to the head.
- **Poses with lines**: a child's `Mind.cue/cueAt` -> `S2CChild` -> `ChildClient.View.cue` -> `ChildRenderer.posed` blends
  `ChildTalkPose.pose(cue, age, stage, carriedOrSitting)` by `ChildCue.weight` (after a moment, before a fight's flip). A companion's
  gesture is just `EtoBrain.gesture(mind, g, now)` (her normal gesture pipeline; a carried toddler keeps her cradle pose).
- **Order of a child's tick** (`ChildBrain.tick`): stage, carrier, `Conversation.going` (+ a threat it would fight ends it), `moments` only
  if not talking, then the chain: carried -> `Conversation.hold` -> a moment -> a player's talk -> a baby sits -> `ParentDuty.led` (escort
  only now) -> threat/fight -> run to parent -> follow -> `SiblingLife.tick` (hide and seek, tag, minding, new moments incl. baby ones)
  -> bed. Its `lines` say nothing during a talk or a game.
- **A companion's tick**: a fight ends her talk; else `Conversation.going/hold` before bed and before `calm` (so nothing in calm, not
  even a player's welcome, interrupts; a player opening a talk, a hand, a kiss interrupts the family talk instead).

## Known and likely problems (by reading; nothing seen in game)

1. **Speech is sent only to players who see the speaker** (within 24): a talk around a corner is half heard (unchanged, maybe right).
2. **Calmed children don't defend themselves** for `Parenting.CALMED` after a parent stops them (unchanged; the parent guards).
3. **Bubble layout** spreads bubbles by their angles on the view; a line's bubble and its reply can still overlap for a tick of network
   jitter (it shrinks away over its last 3 ticks, and a bubble in its last 3 ticks is out of the layout). Untested in game: how the eased
   moves look, and whether the spread is too wide for long lines (a 200-pixel line is 5 blocks wide in the world).
4. **"Hands on hips"** (HIPS) is arms out and back (the child model has no elbows); a toddler's COUNT hands reach only its cheeks.
5. **Hide and seek** looks for ground within 3 blocks of the home's y; a steep yard may leave a hider with no spot (it hides where it
   is). Glass and leaves count as hiding places (collision, as the seeker's own line of sight also uses).
6. **The parent breaking up a squabble** may come from 16 blocks; the children wait (standing) for her line up to 12 s after the
   squabble's lines.
7. Everything below is untested.

## Ideas for what's next (the owner's list, what's left of it)

- **Group scenes with player choices**: the squabble the player is asked to judge. Studied, not built: the scene system
  (`SceneServer`, `talk/child.txt`, `SceneBook`/`SceneWords`) is one NPC to one player; an approach is `SceneServer.approach` (a
  `Pending` and an `S2CNpcApproach` prompt), answers go through `ChildTalk.sceneAnswer` (effects by word, `SceneWords.CHILD_EFFECTS`).
  A plan: scenes `judge_*` with `when: squabble` (a new child flag in `SceneWords.CHILD_FLAGS`, set only by a new
  `SceneServer.judge(npc, child, parent, other)` that filters `book("child")` to `judge_` scenes and puts the other sibling's name in
  `{sib}`); new effect words (`mine`, `theirs`, `share`, `enough`) handled in `ChildTalk.sceneAnswer` to have the other sibling react on
  its remark queue (its cue too); triggered from `SiblingLife.breakUp` when no companion parent is free but a player parent of both is
  near, sees them and `SceneServer.free`. The talk screen covers the world, so the other sibling's bubble should come after the screen
  closes ("close" effect) or be delayed.
- **A family meal** (the partner's 1.3.42 dishes eaten together: the children come to the table, a line each about the food), a
  **bedtime story** (the parent sits by a child's bed, the child asks for "one more"), **sleeping near each other**, **sharing food**,
  **copying the big one** (a toddler mimics an older sibling's cue a beat later), **comforting each other after a scolding**, **rivalry over
  the player's attention**.
- **Children and the player**: reacting to the player's kagune and its colour, asking about the other parent, "watch me!" after a lesson.
- Keep every new line set in the same shape (by persona, temper, age), tested for filling in and banned names; give lines cues; preview
  new poses with `tools/TalkPreview.java` and new bubbles with `tools/BubblePreview.java`.

## Open questions for the owner

- Eto's child and the Owl (unchanged from 1.3.51): the Owl as an exception, if they want it.
- Hide and seek replaces tag half the time; the owner may want a different share, or a second round with roles swapped.

## Things worth checking when the owner plays (all untested in game)

- Two children squabbling near a companion parent: bubbles taking turns, nobody walking off, the parent walking over, her gesture, the
  children's poses (pointing, folded arms, a stamp, a sorry, a tongue stuck out), the cut-short line if one is picked up mid-squabble.
- A toddler wandering off: fetched, carried (its bubble beside its mother's if both talk at once), the word at the door with both standing.
- Hide and seek: the count, the hiding spot (out of sight?), the crouch, the giggle hints, found / given up.
- An older child playing with a baby (peekaboo's hands over the face then arms up).
- Eto's child raking and hammering: its body moving with her flesh.
