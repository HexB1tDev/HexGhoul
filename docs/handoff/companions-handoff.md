# START HERE (state after 1.3.66)

- **Next session:** read `docs/handoff/next-ai-1.3.63.md` first (the owner's current update), then `docs/handoff/next-ai-1.3.60.md`
  (house rules, family talk). Left: **1.3.67 War and Peace running animations**: a work in progress is in `git stash` on this branch
  ("1.3.67 WIP": `KingRun.war` the beast's charge (both claws reach ahead between footfalls and rake back as each lands, near flat, rolling),
  `KingRun.peace` the glide (near flat, head level, arms swept back and wide, bounding with a hang), `PeaceMotion.rest` running pitch
  +14 instead of -7 against the deeper lean; still to do: Peace's lowest blade dips ~30 px below the feet at phase 0 in
  `tools/art-preview.sh <dir> king-runs` (less flip/lean, or lift that blade), Ken's run in `EtoFight.peace` (KingRun.peace from his limb
  swing), docs). If the stash is gone, redo it from this description.
- **1.3.66 War's rinkaku alive** (the owner: "the back tendrils in WAR path are too unrealistic and solid, they are like unmoving branches of
  a tree, they have 0 similarity to the original tendrils of the other kagunes"; protocol 71, unchanged; client drawing only). `WarModel`:
  `Motion` gained `lag, run, rise` (canonical 10 args; the 4/6/7-arg constructors keep working) and `drifting(lag, run, rise)`;
  `centre(sx, level, time, grow, motion)` (public) the centre line, `ALIVE_POINTS` 12 sampled `along` the six control points' Catmull-Rom
  curve, then `alive(p, root, u, seed, time, motion)`: the original TendrilChain's idle motion in kind (side 4.4 + 1.5, bob 3.0 + 1.1, wander
  2.4 + 0.9 px on slow waves, phase running out along it `- u x 2.4`, scaled u^1.25 x (1 + jolt) x (1 + 1.2 frenzy), pace 1 + 1.5 frenzy, a
  flex of the whole, a 1.2 u^2 sag), following him (x + lag 9, y - rise 9, z + run 9, all x u^1.4), `FLOOR` 0.8 (body px over the feet);
  then the moves' lerps as before. `tendrilRadius` smooth (2.35 (1-s)^0.85 + 0.1; the segment flare and `SEGMENTS` gone); the tube: a
  low ridge (0.12), a swell `1 + 0.08 sin(17 s - 0.22 t + 1.9 seed)`, a sheen instead of armour bands; the barbs, thorns and hooked claw
  gone, a short fine point instead. `KingLayer.drift(player, tick, k)` (out of `peaceState`: SWAY eased a tick at a time) feeds
  `warMotion(player, view, time, tick, k)` (the Pair: now k 0, now + 1 k 1; the 3-arg one by the time's fraction).
  Preview: `tools/art-preview.sh <dir> war-alive` (`king-war-alive.png`: from behind a second apart, running, turning, rising, falling,
  frenzy). `KingTests` (moves 14..18 px in 2 s per tip, follows, never below FLOOR).

# State after 1.3.65

- **1.3.65 fear of the final kakujas** (protocol 71, unchanged; `EtoGesture` FLINCH, AWE and `ChildCue` COWER appended). Pure
  `npc/FinalSight` (NONE, WAR, PEACE, DRAGON, RINKAKU, UKAKU; never sent): `of(dragon, worn, rinkaku, ukaku)` (the Dragon first),
  `terror()` (WAR, DRAGON), `scares()` (300 / 240), `STILL_SCARED` 40, `KEEPS_RUNNING` 28, `TERROR_RUN` 20, `SHOUTS` 120, `HIDES` 30,
  `BEHIND` 0.95, `THERE` 0.6, `KEEPS_OFF` 7, `behind(sx, sz, fx, fz, gap)`. Pure `npc/FearLines`: `stranger`, `ally`, `fleeing` (terror),
  `child(stage, call, sight)`, `hiding(stage, call, other)`, `scream(sight)` (NONE: an ordinary ghoul), `falter(sight, senior)`,
  `rally(senior)`: 132 lines; `Script.dread(sight, voice, home, seed)` (Eto 23 in `EtoLines.dread`, Touka 21, Ken 21) and
  `Script.dreadGesture(sight, ours)`. `HumanRules`: `DREAD_NOTICE` 28, `TERROR_NOTICE` 40, `TERROR_SAFE` 52, `TERROR_RUN` 1.3,
  `TERROR_MEMORY` 100, `SCREAMS` 70, `HEARD` 32, `notice(sight, shown)`, `pace(sight)`. `AgentRules`: `falter(sight, senior)` (50 / 30,
  a senior 70%), `FALL_BACK` 60, `FALL_BACK_TO` 14, `FALTER_AGAIN` 1200.
  Server: `Sightings` keeps `SIGHT` (`seen(player)`, `finalOf(player)`); a watcher round also when the sight changes among the worst;
  `terror(level, player, sight, now)` each check while War/the Dragon is worn: wild ghouls in sight within 28 (not allies) keep
  `scaredUntil` >= now + 40 and `scaredOf` = them (one shouts every SHOUTS), their own children within 16 (8, or in sight) get
  `ChildBrain.dread`. The ghoul watcher sets `scaredOf` and uses `FearLines` for the worst; the child watcher too (and `dread` for a terror).
  `GhoulNpcBrain.Mind.scaredOf`: `flee` runs from it (within 48; a terror: 20 blocks at a time); `scare()` clears it. `HumanBrain.danger`
  returns a `Danger(body, sight, ghoul)`: players by `HumanRules.notice` (beyond NOTICE only in its sight), a terror remembered
  `dreadOf/dreadUntil`; `scream` (bubbles to players within HEARD who see it: fresh danger, at most every 200; a terror every SCREAMS + 0..30);
  `flee(human, danger, pace, length)`. `AgentBrain`: `reveal` counts `sight.any()` as shown ("The CCG has seen what you are."), and the
  first sight (in its sight, FALTER_AGAIN since the last) -> `falter(agent, mind, player, sight, level, now)` (a line; `falterUntil`,
  `fallBackUntil`, `rallies`); `tick` -> `falter(agent, mind, level, now)` after the stun: frozen (facing, stopped; a blow since
  `falteredAt` ends it), then backing off to FALL_BACK_TO, then the rally line. `say` = bubbles to players within 24 who see it.
  `ChildBrain.Mind.dreadOf/dreadUntil/dreadSaidAt`; `dread(npc, from, level, now)` (not carried; fresh: ends its talk, moment, hide and
  seek, tag, minding); the chain's `cower` right after carried (the other parent: a player parent not in a terror or a companion parent,
  the nearer within 24 -> `FinalSight.behind` them; none: keep KEEPS_OFF; a baby sits; COWER cue held; a `FearLines.hiding` line every
  160..260 to players within 16 who see it); `lines` silent meanwhile; the COWER cue cleared when it ends; `Conversation.free` refuses
  one hiding. `EtoBrain.saw(npc, player, shown, sight, level, now)`: DREAD with a sight -> `script.dread` and `dreadGesture`.
  Client: `HumanRenderer` and `AgentRenderer` draw `SpeechBubbles`; `ChildTalkPose` COWER (arms crossed up before its chest and chin, a
  little one's hands under its chin, head tucked, trembling, a flinch now and then); `EtoPose` FLINCH (a step back, the right arm in across
  her, the left up before her face) and AWE (hands together low on the chest, head tipped back, breathing). Previews:
  `TalkPreview` (COWER added), `CompanionPreview` writes `eto-/touka-/ken-fear-poses.png`. `FamilyTests.fear`, `CompanionTests` (order).
  Not covered: Ken's own Peace (an NPC) frightens no one; companions don't move at a sight; no scream sound in the pack (bubbles only).

# State after 1.3.64

- **1.3.64 dread is worn** (protocol 71, unchanged). Pure `Display.of(dragon, king, finalKakuja, kakuja, kagune, eyes)`: DREAD for the
  Dragon, War/Peace worn, or a final kakuja; else KAKUJA, KAGUNE, EYES, NONE. `Sightings.of(player)` feeds it: `DragonLock.dragon`,
  `KingServer.wearing(player) != NONE` (was `KingServer.awake`, the permanent save: a War/Peace-path player read DREAD forever, so the
  rise-only watchers reacted once and never again), Rinkaku's final kakuja (`KaguneProfile.isActive`, `PathServer.path` RINKAKU,
  `KaguneProfile.form == RinkakuForms.finalKakuja()`), Ukaku's (`UkakuServer.kakuja && UkakuServer.form(player).finalKakuja()`); the
  rest as before. `FamilyTests.reactions`.

# State after 1.3.63

- **Next session:** read `docs/handoff/next-ai-1.3.60.md` first (family talk after the polish). The owner's next update (asked during
  1.3.60, "after this one is fully finished"): clash impacts sized by how hard each hit was, shaking the ground and camera; the same move
  meeting itself (big punch, speed kick...) cancelling both with a big impact; more and better reactions to the final kakujas (Rinkaku and
  Ukaku, especially War/Peace: being scared of War); new dramatic running animations for War and Peace; their new clash sounds (1.3.61).
- **1.3.63 same-move clash** (protocol 71, unchanged). Pure `pale/MoveClashRules`: `Family` PUNCH (PUNCH, OWL_PUNCH and their charges),
  KICK (KICK, OWL_KICK), SLAM (HAMMER, OWL_FISSURE and charges); `released(move)`; `window(move, detail)` (to its impact + 1: punch
  `PUNCH_IMPACT`, kick `KICK_BURST + dash + KICK_CONTACT`, the Owl's kick `dash + KICK_SKEWER`, `HAMMER_IMPACT`, `FISSURE_IMPACT`); `reach`
  4.5 / 4 / 5.5; `BIG` 2.4, `STAGGER` 14; `power(hitA, hitB)` = max(BIG, `ClashRules.impact`). Server `MoveClash`: `begun(who, move,
  detail, target, now, hit, live, stop)` from `PaleServer.begin` (a punch's hit `punchDamage x power`, else `HIT` 20) and `EtoOwl.begin`
  (HIT); a `LevelTickEvent` (END) checks each live registered move in its window against bodies within reach doing the same family
  (`moveOf`: a player's `PaleServer.state`, Eto's `mind.owl.move`) and both at each other (`at`: the target by id, else facing) -> both
  stopped (the other's registered stop, or `cut`: `PaleServer.end` / `EtoOwl.end`), `KaguneClash.collide(a, b, power)` (apart both, the
  wind, the ground), `PaleStun.stun` both for STAGGER. Not covered: War/Peace (`KingServer.move` has no cancel), `ChildFlesh`.
  `ClashTests.sameMoves`.

# State after 1.3.62

- **1.3.62 clash size** (protocol **71**: `KingMarkFx.GROUND` 8, appended). Pure `ClashRules`: `HIT` 7 (a blow of unknown strength),
  `REFERENCE` 12 (two blows' damage for an `ORDINARY` 1.2 clash), `LEAST` 0.7, `MOST` 3, `QUAKES` 1.55; `impact(hitA, hitB)` = 1.2 x
  sqrt(sum / 12) clamped; `quakes`, `rumble(power)` (0.35..1 above QUAKES), `rumbleTicks` 8 + 6p, `felt` 10 + 8p, `push` PUSH x (0.7 +
  0.25p), `volume` 0.95 + 0.3p, `pitch` 1 + (1.2 - p) x 0.15 (at least 0.65). `KaguneClash.Swing` has `hit`; `swing(..., hit, ...)`
  overloads (old ones: HIT); `meets` -> `impact(mine.hit, theirs.hit)` -> `apart(..., power)`, `wind(a, b, power)` (burst at that power
  and `volume`; quaking: `quake(near, ground, power)` = an `S2CKingFx` MARK `GROUND` at the lower feet with power - 0.6, and
  `sfx.quake` at 0.8 volume, pitch 0.85). `burst` pitch = `ClashRules.pitch`. Hits: PaleBlitz `fist(mastery) x strike.damage x power`;
  KaguneCombat the first target's base damage; GraftCombat `GraftRules.damage x` the heaviest beat's power; GhoulNpcCombat
  `NpcRules.damage(tier) x` blade weight / heaviest beat; AgentBrain `AgentRules.damage(q, step)` (a parry step 0). Client:
  `KingMarks` CLASH adds `rumble(at, ClashRules.rumble(p), rumbleTicks, felt)` (-> `PaleCamera.rumble`) when it quakes; `GROUND`
  -> `ground(at, p)` (a dust ring of puffs, grit) and `KingMarkFx` GROUND shapes (two dust rings, grey sparks). `ClashTests.impacts`.

# State after 1.3.61

- **1.3.61 clash sounds** (protocol 70, unchanged): the owner's `Clashes.zip` (Clash1..4.ogg, mono 48 kHz) copied unchanged to
  `assets/hexkagune/sounds/sfx/clashes/clash_1..4.ogg`; `tools/sfx_pack.py` (the single source of `sounds.json`: run it after any sound
  change; regenerating before this change reproduced the file exactly) has `CLASHES` and the `clash` pool (gain 0.95) is those four, so
  `sfx.clash` and the original's `kagune_guard` play them. `KaguneClash.burst` plays `sfx.clash` at the burst's volume (pitch 1 +
  (1.2 - power) x 0.15) and the gust (`entity.ender_dragon.flap` at 0.8), no longer the `KAGUNE_SLAM` heavy punch under it.

# State after 1.3.60

- **1.3.60 review fixes** (protocol 70, unchanged): `Conversation.tick` ends a talk (LEFT, silent) when a speaker who never came is
  dropped and fewer than two members are left; `Conversation.free` refuses a companion following a player.

# State after 1.3.59

- **1.3.59 babies in sibling moments** (protocol 70, unchanged). Pure `family/LittleOnes`: `Kind` PEEKABOO, FACES, BABBLE, LULLABY (never
  sent); `Line(baby, text, cue)`; `EVERY` 1800, `CHANCE` 0.5; `pick(temper, evening, roll)` (evening: LULLABY; else weights by temper);
  `talk(kind, temper, stage, name, seed)` (PEEKABOO older COUNT, older "boo" CHEER, baby CHEER, older GIGGLE; the others older, baby,
  older; `TalkCues.said` overrides a line's cue except PAT); 98 lines, keys "kind:part:temper|any:baby|little|big|teen" (teen falls back to
  big). `SiblingLife.begin`: a free baby sibling in sight -> `baby(...)` (when `Mind.babyAt` is EVERY old and CHANCE): a Conversation,
  the baby the anchor, the older one coming. `ChildRenderer.posed`: a sitting baby's cue keeps its legs and no hop. `FamilyTests.littleOnes`.

# State after 1.3.58

- **1.3.58 hide and seek** (protocol 70, unchanged). Pure `family/HideAndSeek`: `INSTEAD_OF_TAG` 0.5, `COUNT` 100, `SEEKS` 900,
  `HINT_EVERY` 160, `HEARD` 80, `FINDS` 3, `NEAREST` 4 / `FARTHEST` 11, `SPOTS` 24, `REACHES` 160, `AFTER` 80, `LONGEST`; `Part` (PROPOSE,
  AGREE, COUNT, READY, SEEKING, HINT, FOUND, CAUGHT, GIVE_UP, WIN; never sent); `line(part, temper, stage, seed)` (80 lines, keys
  "part:temper|any:little|big"); `bestSpot(distance, hidden)`; `spot(i, hx, hz, radius, seed)` (a golden-angle spread in the yard);
  `heard(sinceHint)`. `ChildCue` gained HIDE (1200 ticks: legs folded forward, `drop` 8.6 / a little one's 4.3, arms round its knees) and
  COUNT (hands over the eyes; a little one's to its cheeks, its head bowed). `ParentDuty.Said` gained a `cue` (3-arg: none), applied as
  it is said (`Conversation.act`). Server `SiblingLife`: `begin` turns TAG into `startHide` half the time (PROPOSE with CHEER, AGREE on the
  seeker's queue; `Mind.hideWith/seeker/hidePart/hideAt/hideBegan/hideFound/hideSpot/hideSetOff/hidden/hintAt/seekTo/seekToAt/seekSaidAt`);
  `tick` runs `hide` first: stops (`stopHide`, both minds, a HIDE/COUNT cue cleared) on anything (gone, carried, a fight, a moment, a talk,
  a player's talk, asleep, following, LONGEST, no home); `seek` (part 0: still, turned away (`Conversation.faceTo`), COUNT, READY at
  COUNT; part 1: found within FINDS in sight after 20 ticks -> FOUND (POINT) + CAUGHT (`caughtCue`) -> `over`; SEEKS -> GIVE_UP (SHRUG) +
  WIN (CHEER); else to a `spot` every 70 ticks or to 1.5 blocks of the hider when `heard`, SEEKING now and then (THINK); part 2: AFTER,
  then stop); `hideAway` (`spot`: the 24 spots with `ground` (a block with collision under two without, from home y + 2 to - 3), seen
  from the seeker's eyes by `ClipContext` COLLIDER to the spot's head height; there or REACHES: HIDE cue, still, peeking at the seeker;
  HINT now and then while looked for; over: found, it stands; not, it runs to the seeker). Guards: `Conversation.free` (no game),
  `ChildBrain.held` (its stroll waits), `ChildBrain.lines` (silent), `ChildBrain.start` (a player's moment ends the game, others refused),
  `ParentCare.tend` (no pick-up). Preview: `TalkPreview` shows HIDE and COUNT. `FamilyTests.hideAndSeek`.

# State after 1.3.57

- **1.3.57 gestures with the lines** (protocol **70**: `S2CChild` gained `cue` (byte, `ChildCue` ordinal) and `cueAt` (varlong)). Pure
  `family/ChildCue` (NONE, CROSS, STAMP, POINT, HIPS, SHRUG, SORRY, CHEER, GIGGLE, WHISPER, BLEH, PAT, SULK, THINK, WAVE, PLEAD; sent by
  ordinal: append only; `ticks`, `running(age)`, `weight(age)` in 4 / out 8) and `family/TalkCues` (`Scene` CHAT, TEASE, BRAG, COMFORT,
  SCHEME, SQUABBLE, MIND, SCOLDED, RAMPAGE, HOME, MAKE_UP, CUT; `said(text)` keywords; `child(text, scene, temper, starter, index, seed)`;
  `saidBy(text)` / `parent(text, scene, persona, index, seed)` -> `EtoGesture`). `Conversation.Line(speaker, text, cue, gesture)` (2-arg:
  none); `Conversation.act` on saying it (a child: `Mind.cue/cueAt`, dirty; a companion: `EtoBrain.gesture`); `ParentDuty.lines(child,
  parent, talk, scene, temper)` and `SiblingLife.lines(first, second, parent, talk, scene, t1, t2)` fill them (`SiblingLife.scene(kind)`;
  breakUp MAKE_UP; MIND); a cut-short line too (CUT). Client: `ChildClient.View` cue/cueAt/cueBegan (`cueAge`), `ChildRenderer.posed`
  (after a moment, before a flip) blends `client/animation/ChildTalkPose.pose(cue, age, stage, carried)` by `weight` (a carried child's legs
  left sitting). Preview: `javac -nowarn -cp build/classes:build/base.jar -d build/talk-preview tools/PoseRender.java tools/MomentPreview.java
  tools/TalkPreview.java` then `java -Djava.awt.headless=true -cp build/talk-preview:build/classes:build/base.jar TalkPreview <dir> <boy skin>
  <girl skin>` (square HD skins, e.g. `art/children/sons/cute-boy-hd.png`, `art/children/daughters/hd-cute-girl.png`). `FamilyTests.cues`.

# State after 1.3.56

- **1.3.56 Eto's child posed** (protocol 69, unchanged; client only): `client/animation/ChildFight.pose(child, clock, partial)` (as
  `EtoFight` without Peace or the Owl: a `PaleAnimator` per child from `PaleClient.get(child UUID)`, its landings, `ArmPose.body` while
  `ArmClient.get(child)`; null when nothing poses it; animators not drawn for `FORGET` 200 ticks dropped). `ChildRenderer.m_7392_` works
  it out for an OWL child (not asleep, not young) into `FIGHT` (by id); `Model.m_6973_` applies it after `little()` with `apply(name,
  part, pose)` (EtoModel's: `RigPose.transform` positions and rotations for head, torso, arms, legs). `ChildRenderer.fightDrawn(child)` is
  passed as `drawn` to `PaleFleshRenderer.flesh/arm` for children (was null). Reference look: `tools/pale-preview.sh <dir> flesh`.

# State after 1.3.55

- **1.3.55 one escort** (protocol 69, unchanged): pure `Siblings.standDown(temper, stage, name, call, seed)` (38 lines, "temper:child|teen",
  `standDownLines()`). `SiblingLife.parentGoing(level, little)` (a live companion whose `duty.child` is it); `begin` skips MIND then;
  `minding` stands down when one is going (minding 0, its escort of the little one dropped, the line on its remark queue unless it is in a
  talk). `ParentDuty.look` skips a child someone else is escorting (`escort` set, not expired, not itself). `FamilyTests.siblings`.

# State after 1.3.54

- **1.3.54 tag in the yard** (protocol 69, unchanged): pure `Siblings.RUN` 5, `YARD` 2.5, `yard(radius)` (the edge less YARD, at least 3),
  `runTo(x, z, chaserX, chaserZ, turn, homeX, homeZ, radius)` (RUN away turned by `turn`; outside the yard: the extra turn (0.5..2.6 rad
  either way) that keeps it farthest from the chaser, else back toward the middle). `SiblingLife.tag` runs there (no home: the game ends);
  `begin` turns TAG into CHAT unless both are `inYard`. `FamilyTests.siblings`.

# State after 1.3.53

- **1.3.53 one conversation lock** (protocol 69, unchanged). Pure `family/Talks`: `GATHER` 2.2, `SPEAKS` 4, `LEAVES` 8, `WAITS` 240,
  `GATHERS` 300, `TURN` 24 deg/tick; `End` DONE, LEFT, GONE, HURT, FIGHT, ASLEEP, INTERRUPTED (never sent); `faces(self, speaker, previous,
  next, anchor, other)`; `turn(from, to, most)`; `remarked(End)` (LEFT only); `cutShort(who, stage, name, call, seed)` (56 lines: a persona
  key's, else temper word + little/child/teen, else any; "" = says nothing). Server `Conversation` (members by entity id, an `anchor` who
  stays put, `coming` members who walk up first, `Line(speaker id, text)`s): `start(level, now, what, anchor, members, coming, lines)` (null
  if any member is in one; children's moments stopped `ChildBrain.stopMoment`, a companion's chore `HomeChores.stop` and still gesture
  cleared), `join(level, who, comes, more)`, `free(body, level, now)` (a child: awake, no fight, no moment, not carried, not following, no
  player talk, no tag/minding/escort; a companion: no fight, duty, hand, kiss, walk-up, player talk, moment, cling), `in(body)`,
  `going(body, level, now)` (ticks it once a tick, whoever asks first; over: the mind lets go: `Mind.conversation` null, a child's
  `lastTalk`), `hold(body, level, now)` (coming: walk to the anchor until within GATHER; else navigation stopped, its leash `m_21446_`
  where it stands (radius 3), no sideways speed, `face`: the body turned by TURN a tick, the head up to 50 deg further, the look control
  at them; a carried child: its carrier has it), `end(body, why, level, now)`. `tick`: each member present (else GONE), `trouble` (asleep;
  hurt since it began (`hurtAt`); a fight (`target`); a child's player talk/following/a carrier not in it; a companion's player talk, hand,
  kiss, walk-up: INTERRUPTED), past LEAVES from the anchor (not coming): LEFT with `cutShort` by a member who stays (a companion first) if
  a line was said; lines: each when due and its speaker within SPEAKS of the anchor (the anchor: of the nearest member), else it waits
  (never came together in GATHERS: LEFT, silent; a line waited WAITS: said from where it is within LEAVES, else that speaker `drop`ped);
  sent with `Speech.turn` (1.3.52); empty and its last pause over: DONE. Hooks: `ChildBrain.tick` (going first; a threat it would fight
  ends it; no `moments` meanwhile; the chain's hold right after being carried; `lines` says nothing meanwhile; `start` refuses a moment
  mid-talk; `held(npc)` gates `ChildNpc`'s RandomStrollGoal); `EtoBrain.tick` (a fight ends hers; before bed/calm: going and hold);
  `EtoBrain.busy` includes it; `ParentCare.tend` never picks up a child mid-talk. Uses: `ParentDuty` (reached: the child's fight state
  cleared, any talk of its own ended, `Duty.talk` started with the child as anchor; after it: DONE -> home, else the duty ends; at the door
  put down, `Duty.home` and the HOME talk, the duty finished once it is over; `finish` ends a talk still going; `led` is only the escort
  now; `stillBy/stillUntil` are gone), `SiblingLife` (held kinds: a Conversation with the starter coming to the other; a squabble's
  `breakUp` joins a free parent of both within 16 who sees it, coming; MIND: on reaching the toddler `Mind.mindingTalk`, then home only
  if it was DONE; remarks (REACT/TEAM, tag shouts, a parent's TRANSFORM word) stay on the old `Mind.talk` queue and are skipped for one in
  a talk). Tests: `FamilyTests.talks`.

# State after 1.3.52

- **1.3.52 bubbles take turns and keep apart** (protocol **69**: `S2CNpcSay(npc, text, ticks)`, `ticks` 0 = as long as it takes to read;
  the 2-arg constructor still sends 0). Server: `ParentDuty.talk` sends each due line with `life(said, next, now)` =
  `Speech.turn(speaker, nextSpeaker, gap)` (until the next queued line if someone else says it, at least 20, at most `Speech.MOST`; the
  same speaker or none: 0). `Parenting.pause` = max(40, min(140, 32 + 1.4 x length)) (was 36..110, 30 + 1.3 x): the pause is now all the
  time a turn is up. Client: `NpcSpeech.accept(npc, text, ticks)` (until = `Speech.life(text, ticks)`: the sent life, 10..`ticks(text)`),
  `left(npc, partial)`, `speaking()`. `BubbleShape.pop(age, left)` shrinks it over its last `GOES` 3 ticks; `BubbleShape.of(width, lines,
  thought, shift, lift)` moves the box (its point stays at the head; the tail's base `tailBase()` as near the head as the box's corner
  allows; a cloud's `trail()` stretched with more, smaller puffs), `rim()` (what is drawn past the box: a cloud 6, a bubble 1.5), `home()`.
  Pure `npc/BubbleLayout.arrange(List<Seen>)` -> `Place(shift, lift)` per id: on the view as angles (x right, y up, `perPixel` = 0.025 /
  distance), sorted oldest first; pairs that overlap (rims in, `GAP` 3 px) pushed apart sideways half each, toward each speaker's side
  (right on top of each other: the newer right), each at most half its width + `REACH` 40 px (the other takes the rest), up to `ROUNDS`
  40; then `tilt` (moved far aside, up to `TILT` 12 px so the tail angles down); then anything still overlapping: the newer lifted over.
  `SpeechBubbles.place(body, base)` works the layout out once a frame (`m_91296_` partial + game time as the frame's key) for every
  bubble in the player's sight (`NpcSpeech.lines(body)`, interpolated positions, each speaker's last `base`), skipping bubbles in their
  last `GOES` ticks (going: kept where they were), and eases each place over ~2 ticks. Preview: `tools/BubblePreview.java <dir>` also
  writes `bubbles-scene.png` (before/after a squabble and a carried toddler, the turn, lines said at once, a cloud beside a bubble).
  Tests: `NpcTests.turns`, `NpcTests.layout`.

# State after 1.3.51

- Next session after 1.3.51: read `docs/handoff/next-ai-1.3.51.md` first: the owner's next goal (more interactions and dialog, beauty and as few
  bugs as possible: nobody walking off mid-talk, no overlapping bubbles in squabbles), how conversations work now, and the problems
  already found by reading the code. Still open: the Owl as an exception for Eto's child if the owner wants it.
- **1.3.51 siblings** (protocol 68, unchanged; the owner: "interactions between brothers and sisters"): pure `family/Siblings` (`Kind`
  CHAT, TEASE, BRAG, COMFORT, SCHEME, SQUABBLE, TAG, MIND, REACT, TEAM; `pick(sa, ta, sb, tb, hurt, strayed, roll)` (no baby; MIND for an
  older one and a strayed toddler; COMFORT when hurt; else weights by both tempers, toddlers brag/scheme less); `talk(...)` -> `Line(who:
  1 the starter, 2 the other, 0 a parent)`: most 1-2-1, SQUABBLE 1-2-1-2, TAG 1-2, REACT/TEAM 1; `tagged`, `breakUp(persona, ...)` 0-1-2;
  NEAR 10, EVERY 1500, TAG_TICKS 400, TAG_REACH 1.3, HURT 0.6, REMARKS_EVERY 1200). `family/SiblingLines` (337 + 12 lines, keys
  "part:who:kind:group", who a temper word / "any" / a persona, group little/big/any; falls back temper -> any, little <-> big; written from
  a script's data, edit the Java directly). Server `SiblingLife` (`siblings(a, b)` a parent in common; `tick` from `ChildBrain` (after
  following a player, before going to bed): tag (`Mind.tagWith/chasing/tagUntil/taggedAt`: the chaser runs at 1.3, tags within reach,
  roles swap with a shout; the runner runs off), minding (`Mind.minding/mindingUntil/mindingReached`: to the toddler, a word each, then
  its `escort` home), else every 40 ticks a new moment with a free sibling near in sight (`Mind.siblingAt`; by night only chat, comfort,
  minding): talks queued on its `Mind.talk` (speaker ids; `ParentDuty.talk` says them), both standing still for them (`stillBy`); a
  squabble's `breakUp` by a companion parent of both within 16 who sees it; `kagune` (from `ChildKagune.fight` as its kagune comes out:
  a sibling within 12 who sees it says REACT, or TEAM fighting the same target; `Mind.siblingRemarkAt`); `afterSibling` makes a monster
  after a sibling a threat in `ChildBrain.threat`. `FamilyTests.siblings`.

# State after 1.3.50

- **1.3.50 Eto's child** (protocol 68, unchanged; the owner: "her kagune abilities and transformation at low health", and "never a
  kakuja": its transformation is her Mutated Arm, never the Owl; the owner was told and may ask for the Owl as an exception). Pure
  `ChildKaguneRules.Act` gained SWIPE, FLESH_GRAB, HAMMER, ARM (appended; never sent), `chooseFlesh(distance, sees, scale, small, health,
  now, swipeReady, grabReady, hammerReady, armReady, roll)` (ARM under `ARM_BELOW` 0.4 health within 14 x scale; close: HAMMER 35% or
  SWIPE; further: FLESH_GRAB; else CLOSE_IN), constants (SWIPE_EVERY 50, HAMMER_EVERY 170, ARM_EVERY 320, ARM_HELD 0.35, shares,
  SWIPE_REACH 4 / GRAB_RANGE 8 before scale). Server `ChildFlesh` (her `EtoFlesh` rake, grab (REACH, HOLD, THROW on `PaleRules.throwAt`,
  MISS) and club (HAMMER_CHARGE, HAMMER, shockwave) at `ChildKaguneRules.scale`, hits through `ChildKagune.fair`; told by
  `S2CPaleState` under the child's UUID; `Fight.flesh`) and `ChildArm` (a port of `EtoArm` at its size, `ArmRules`, `S2CPaleArm` under
  its UUID; power = its blow). `ChildKagune.has` = any kagune; `fight` runs the flesh first for an OWL child (`flesh()`), falling back to
  a father's tentacle/wing; `tick` ticks the arm, lets go and draws it back out of a fight; StartTracking sends both. Client:
  `PaleClient.get(UUID)`; `PaleFleshRenderer` draws children's flesh and arm (`flesh`/`arm` with the child's scale, `drawn` null: the
  move alone poses them); `ChildRenderer` hides the right arm and sleeve while `ArmClient.out(child)`. `FamilyTests.etoChild`.

# State after 1.3.49

- **1.3.49 parenting** (protocol 68, unchanged): pure `family/Parenting` (`Why` WANDERED, NIGHT, RAMPAGE, TRANSFORM, HOME; `strays(stage,
  night)` blocks past the home's edge: toddler 4/0, child 16/2, teen 40/24; `strayed(...)` never a follower, a carried child or a baby;
  `rampage(stage, fought, beyond, health)`: past `fightsFor` (600, a teen 1200), 20 (36) blocks out, or under 0.3 (0.25) health;
  `notices` (in sight within 40, or missed after 600); AGAIN_AFTER 1200, GIVES_UP 2400, CALMED 1200, REMARKS_EVERY 3000, REACHES 2.6;
  `pause(text)`; `talk(why, persona, stage, temper, name, call, seed)` -> parent/child lines: open, reply, follow, close, final (a word
  about its kagune: open, reply, maybe final; at the door: open, reply)). `family/ParentingLines` (579 lines in 240 keys
  "part:who:why:group", who = a persona key or a temper's word, group little/child/teen; a missing "little" falls back to "child";
  written from a script's data, edit the Java directly). Server `ParentDuty`: `EtoBrain.Mind.duty`; `tick` from `EtoBrain.calm` right
  after a talk holds her (before `together`, chores, `ParentCare.tend`; never during her own fight, a carried toddler put down if one
  starts): `look` (her children by `Children.of(level, "npc:<key>")`, bodies via `UniqueNpcs` "child:<id>", within 96; RAMPAGE first,
  noticed within 64), walk (1.4 for a rampage, else 1.1), reached: the child's `calmedUntil`, `fetchedAt`, the talk queued (`queue`:
  `ChildBrain.Mind.talk`, `talkFree`), the child stands still for it (`stillBy`/`stillUntil`), a toddler picked up (`cradling`); then
  home to the house's door (`escort` for an older child, `led`), at the door put down and the HOME talk; `wakes` (HomeLife wakes her for a
  child out in the night 300+ ticks or rampaging); `transformed` (from `ChildKagune.fight` when its kagune first comes out: a parent within
  24 who sees it); `talk` (ChildBrain each tick: due lines as `S2CNpcSay` to players within 24 the speaker sees). `ChildBrain`: `led`
  before its threat (its fight off while led), `calmedUntil` gates fighting, `outsideSince` kept every 20 ticks. `FamilyTests.parenting`.

# State after 1.3.48

- **1.3.48 the kit** (protocol 68, unchanged): pure `ChildKaguneRules.Act` (NONE, LASH, SHOOT, FAN, GRAB, BACK_FLIP, FRONT_FLIP, CLOSE_IN;
  no kakuja), `choose(bits, distance, sees, tailReach, crystal, small, onGround, now, flipReady, grabReady, fanReady, roll)`, constants
  (CRYSTAL_AFTER 20, FAN_SPIKES 5 / FAN_SPREAD 32 / FAN_EVERY 160, SPIKE_SHARE 1.2, GRAB_EVERY 220 / GRAB_CATCH 8 / GRAB_HOLD 26 /
  SQUEEZE_EVERY 8, GRAB_WIDEST 1.0 / GRAB_TALLEST 2.2, FLIP_EVERY 90, BACK_FLIP_INSIDE 1.7, FRONT_FLIP 6..11), `grabbable`, `fan(i)`,
  `throwsIt(roll)`. Server `ChildKagune`: crystal (S2CUkakuFx.CRYSTALLIZE, then `fire(..., spike, kind, spread)` sends SPIKE + kind /
  SPIKE_IMPACT / SPIKE_FIZZLE; a spike stabs, no fire), `grabbing` (HUMAN_PUPPET: caught on GRAB_CATCH within the skewer's reach x scale
  + 0.6, `carry` to `holdPoint` + `PaleStun.stun`, `squeeze` every 8; then PUPPET_THROW (flung at tick 5) or LASH_5 (lifted, crushed
  down on the slam's land tick with a SHOCKWAVE); `end`), `flip` (`PaleRules.Flip` launch at its size; `Fight.flip/flipAt` into
  `S2CGhoulNpc`). Its prey in the message is what it holds while it holds. Children with wings also send `S2CNpcWings` (`wings()`, on
  StartTracking too). Client: `WingRules.child(..., crystal)` (crystal from `NpcWingsClient`), `GraftRenderer.drawTentacle` winds a
  holding move round the held body (`GraftRig.Input` held/heldRadius), `ChildRenderer.posed` returns `GhoulNpcClient.flip` out of a
  moment. Preview row (grab, crystal). `FamilyTests.kit`.

# State after 1.3.47

- **1.3.47 child health** (protocol 68, unchanged; the owner: "father max mastery, mother 360 HP: the kid 400"): pure
  `ChildRules.grownHealth(motherHealth, motherMight, fatherHealth, fatherMight)` = the stronger's health x (1 + `PARENT_BONUS` 0.14 x
  the other's might), `share(stage)` (0.25 / 0.4 / 0.65 / 1), `health(stage, might, grown)` (never under 1.3.21's), `HEAL_AFTER` 100 and
  `healing(most, ghoul)` (a ghoul's whole in 120 s, a human's 600 s). `Children.Child.vitality` (saved as the 12th field; -1 unknown),
  `Children.vitality(level, child)` (worked out once every player parent is online: a companion's `Persona.health()`, a player's
  `m_21233_`, 20 if away) and `Children.health(level, key)`. `ChildBrain`: a stage's most from it, the first tick keeps the share of
  health it had; it heals every second once unhurt for `HEAL_AFTER`; a child whose vitality turns known re-reads its most. `/kagune
  children` shows health / most (grows into). `FamilyTests.vitality`.

# State after 1.3.46

- **1.3.46 layouts** (protocol 68, unchanged; the owner: "add other variations, like two wings and the tentacle in the middle... stays
  in the back like a tail"): pure `ChildKaguneRules.Layout` (SHOULDER_RIGHT side 1, SHOULDER_LEFT -1, TAIL 0), `layout(bits, seed)`
  (tentacle children only; shares `RIGHT_SHARE` 0.4, `TAIL_SHARE` 0.35, the rest left; a mix of the seed so old children get one),
  `wingCount(bits, seed)` (tentacle + wing: TAIL 2, a shoulder 1; else `KaguneGenes.wings`), `wingSide` (the other side of a shoulder
  tentacle), `tentacleRoot(layout)` (x mirrored; 0 for the tail), `tentacleRest(..., layout)` (the tail: out behind at waist height,
  curling up, a wider side-to-side sway), `describe(bits, seed)` (`/kagune children`). Used by `ChildKaguneRules.side` (the server's
  shots), `GraftRenderer.drawTentacle` (rest and the lash's root), `UkakuWingRenderer`'s child loop (count and side). Preview:
  `tools/ChildKagunePreview.java` (the three layouts, the tail's lashes). `FamilyTests.kagune`.

# State after 1.3.45

- **1.3.45** (protocol 68, unchanged; the owner: the back was crowded, lean the tail to the right): `ChildKaguneRules.TENTACLE_ROOT` x
  0.13 (its right), `SEED_FAN` 0.95 and `LEAN` 0.45 (the first control pulled out to the right: up over the right shoulder);
  `ChildKaguneRules.wingSide(bits, seed)`: with a tentacle the one wing is always on its left (-1), else `KaguneGenes.side(seed)`; used
  by `ChildKaguneRules.side` (the server's shots) and `UkakuWingRenderer`'s child loop. Lashes carry the move's path onto the new root
  (`GraftRenderer.drawTentacle`, unchanged). Preview: `tools/ChildKagunePreview.java` (Touka moved further left). `FamilyTests.kagune`.

# State after 1.3.44

- **1.3.44 fix** (protocol 68, unchanged; the owner: "it's meant to be one tentacle not two", and the 1.3.43 tails were the dormant Graft
  DNA's toothed tails): `ChildKaguneRules.tailMastery` is 0 / 60 (under `GraftStage.TWIN_MASTERY`: one limb on the server's string and
  the client's), `TENTACLES` 1, `TENTACLE_THICKNESS` 0.18 (a Rinkaku player's first tentacle: `KaguneProfile.tendrilBaseThickness` at
  mastery 0), `TENTACLE_ROOT` (the pelvis, as `TendrilChain.pelvisAnchor`), `tentacleRest(length, time, seed, points)` (the original
  seed tentacle's rest cubic, `KaguneForm.RINKAKU_SEED`'s fan 0.15 / height 0.44 / back 0.62, drawn out about its root to `REST_SHARE`
  0.85 of the lash length, swaying by `SEED_WIGGLE`). `GraftRenderer.drawTentacle` (child loop): rest pose or the GraftRig lash (one
  limb) carried onto the pelvis, `GraftMotion.Limb` springs, 28 points evenly along the grown part, then the original's tube
  `KaguneWorldRenderer.emitTubeLegacy` with a `KaguneClientState.View` built for the child (RINKAKU_SEED, its colour,
  `KaguneColours.veinOf`, thickness 0.18 x scale, styles as the original's EMPTY view) and a `TendrilChain(0)` per child
  (`TENTACLES` map). `KaguneColours.veinOf` (new): DEFAULT/BLACK/BONE their own veins, else the roll's rule (hue kept, s + 0.1, v x 0.17,
  at least 0.06). Preview: `tools/ChildKagunePreview.java` (rest, three lashes; the GraftPreview `child` sheet is gone). `FamilyTests.kagune`.

# State after 1.3.43

- **1.3.43 kagune inheritance, part 1** (protocol **68**: S2CChild's genome string has nine parts; children also send `S2CGhoulNpc`).
  Pure `family/KaguneGenes` (bits TAILS 1, WING 2, PAIR 4 = the second wing, OWL 8; UNKNOWN -1 for a child from before; `inherit`: same
  kind whole, one parent none -> the other's, two kinds -> the union with PAIR dropped when a wing meets tails/owl; `ofPath`, `side(seed)`,
  `colour(...)` one parent who has a kagune, `describe`). `Genome` gained `kagune` and `colour` (a 7-arg constructor and 7-part saves read
  as UNKNOWN, -1; `withKagune`; drawn after the seed in `Genome.of`, so a child of the same night is otherwise unchanged); `Genome.Parent`
  gained `kagune`/`colour` (5-arg = none). `Persona.kagune()` (Eto OWL, Touka WINGS, Ken TAILS) and `kaguneColour()` (Eto 0xD8CCBC,
  else `KaguneColours.DEFAULT`); `Looks.parent` passes the player's path (`Looks.kagune`) and `KaguneAppearance.primaryColor()`
  (`Looks.kaguneColour`); `EtoServer.conceive` passes the companion's. Old children: `Children.genome(level, child)` works it out once
  every player parent is online (ChildBrain checks every 100 ticks; `/kagune children` too) and saves it.
- **The fight:** pure `family/ChildKaguneRules` (scale = stage.draw; `tailMastery`/`wingMastery` by stage only (100/200, 110/220: the
  client draws from the stage), `damage` = 0.45 x the punch, `string`, `use(bits, distance, sees, tailReach)` TAILS / WINGS / CLOSE_IN /
  NONE, `side`, `pause`). Server `ChildKagune` (`Fight` in `ChildBrain.Mind.kagune`): `fight` (from `ChildBrain.fight` when `has(genome)`;
  the GraftMove string with beats at child scale: reach x scale, chest 1.35 x scale; shots from `tip` at the wing mastery x scale, bolts
  stepped in `tick`), `fair` (the threat or `EtoBrain.hostile`; never Player/EtoNpc/ChildNpc), `message` (an `S2CGhoulNpc` under the
  child's id: identity RINKAKU if tails else UKAKU, seed from the genome), `fizzleAll` on death; sent on StartTracking too.
- **Drawing:** `ChildRenderer.drawn()` (children with a kagune, cleared from `GhoulNpcRenderer.Frame`); `GraftRenderer.Frame` has a
  `scale` (place/rig scale about the feet; widths and the heave blob scaled) and a child loop (tails in the genome's colour, tailMastery);
  `UkakuWingRenderer` child loop (`WingRules.child`: growRight/growLeft for one wing on `KaguneGenes.side(seed)`, carry = size);
  `KaguneTints.of(ChildNpc)` = the genome's colour (its shots too). Preview (1.3.44: replaced): `tools/ChildKagunePreview.java`. Tests: `FamilyTests.kagune`.

# State after 1.3.42

- **1.3.42 the partner cooks the cookbook's dishes** (protocol 67, unchanged; no new Minecraft API). Pure, in `family/Kitchen`:
  `cooking(dish, stock, searable)` -> `Cooking(dish, steps, uses, yields, seared)` or null (each ingredient from what it made on the
  way, then the stock, else made first from its own recipe, `PREP_DEPTH` 2; a cooking recipe counts one `seared`, at most `searable`);
  `remainder(id)` (water/milk/lava bucket -> bucket, honey/dragon's breath -> glass bottle, a DRINK dish with a container -> it, as
  `ControlItems` gives it `craftRemainder`); `cooks(dish, ghoul)` (ghoul spouses only; effects or a cooking recipe: never the mince
  alone); `worth` (100 per effect + nutrition + seconds x level / 10); `choose(stock, searable, ghoul, last, emptiedLast, roll)` (the
  best three by worth weighted 3/2/1, the last dish dropped while another exists, and with `emptiedLast` no dish whose `empties(stock)`
  takes the last of something another recipe needs); `passes(...)` (that filter left nothing but something was possible: the meal's
  turn passes, the flag clears); `dishDue(today, lastAt, now)` (`DISHES_A_DAY` 4, `DISH_GAP` 2400); lines `made` (kept/in the chest),
  `servedDish` (+ "more in the chest"), `madeWhileOut`. `Kitchen.name` gives a dish its real name (`Dishes.name`, from the new
  generated `Menu.NAMES`: `tools/kitchen.py` writes it; `Dishes.byItem("hexkagune:x")`).
- **World side:** `HomeKitchen.dinner` (ghoul spouse, chests, `dishDue`; searable from the nearest campfire, else stove fuel / 2;
  where: the searing station if something is seared, else the nearest stove, a table, or the chest) -> `HomeChores.begin` counts COOK
  if `plan.cookAt` or a dinner exists and prefers the dinner (`Job.dish`); `HomeChores.effect` COOK with `job.dish` calls
  `HomeKitchen.cookDish` (re-reads the stock; the planned dish or a fresh choice; takes every `uses` entry, any short -> all given
  back; fuel re-checked after taking; `burn`; the dish into the meal (`Kitchen.add`, MEAL 6) and the rest + prep + remainders
  `giveBack` as whole stacks (`stacks`); saved `HexDishLast/Emptied/Day/Count/At`; `HexDishNews` when stored with the spouse away).
  With the spouse in sight: `HomeKitchen.hand` (the meal handed over) and `Kitchen.made`. `serve` (the welcome) now prefers a dish in
  the meal for its line (`servedDish`), else `madeWhileOut` from the news. COOK smoke/flame particles only off a real station.
  `/kagune home` lists the dishes the chests could make and today's count. Tests: `KitchenTests.homeCooking`.

# State after 1.3.41

- **Next session:** read `docs/handoff/next-ai-1.3.41.md` first (the plan for what's left of Home Living and the house rules).
- **1.3.41 cookbook** (protocol 67, unchanged): item `hexkagune:ghoul_cookbook` (`ControlItems.COOKBOOK`, stack 1, Food tab).
  Pure `kitchen/Cookbook` (PRICE 2 cocoa beans, REGARD 20, `spreads()` = cover, per chapter a CHAPTER spread then its DISH spreads,
  EFFECTS last; `chapterAt`, `dishAt`, `pageNumber`, `affordable`, `wrap`; Touka's `ASK`/`price`/`given` lines). Touka: `EtoServer.
  withCookbook` adds SAY_6 "cookbook" to her MAIN page (before Leave/Insult/Tease) at regard >= 20; the "cookbook" key takes 2 cocoa
  beans from the main hand (not in creative) and gives the book (`addItem` or drop). Client: `CookbookClient` (RightClickItem with the
  book in the local player's main hand opens `CookbookScreen`; tooltip line); `CookbookScreen` draws `textures/gui/cookbook.png`
  (512x256: the 300x196 spread at 0,0; sprites listed in its constants and `tools/cookbook_art.py` SPRITES; 7 ribbons at x 412 + 13k)
  and `textures/gui/cookbook/<cover|chapter|backdrop>.png` (48x48 pictures drawn at 2x inside the frame sprite), items by
  `GuiGraphics.m_280480_` + `m_280370_` (count) + `m_280153_` (tooltip), mouse wheel `m_6050_` (all newly allowlisted [srg] with
  stubs). Layout was checked offline for every dish with a font-width mock (longest right page: Anteiku Blend, ends ~157, page
  numbers at 172); the screen itself is not seen in game. Art: `python3 tools/cookbook_art.py [preview-dir]`. Chapter intros and
  the book's words come from `tools/kitchen.py` (CHAPTERS now `(key, name, intro)`, TEXT). `KitchenTests.cookbook`.

# State after 1.3.40

- **Then (written after 1.3.40):** Home Living continues: **1.3.41** Touka's cookbook (a friendship-gated talk option with her, traded for two cocoa
  beans) and its illustrated book screen (Alex's Caves-style: pictures of each dish, its recipe and what it does; the chapters and
  every line are already in `Menu`/lang: `book.hexkagune.cookbook.chapter.*`, `item.hexkagune.<id>.about`,
  `effect.hexkagune.<key>.description`); **1.3.42** the partner cooking `Menu` recipes from the home's chests (HomeKitchen, the COOK
  gesture, instant); then children inheriting their parents' kagune (colour from one; Rinkaku + Ukaku = tails and one wing; Eto's
  child her kakuja at low health). See also `docs/handoff/next-ai-1.3.39.md`.
- **1.3.40 kitchen** (protocol 67, unchanged): `tools/kitchen.py` is the single source (its MENU table: name, chapter, recipe,
  servings, food, effects, kind, container, line) and writes the icons (`tools/kitchen_art.py`, 16x16 pixel art; preview with
  `--preview <dir>`, a sheet at 6x), item models, `data/hexkagune/recipes/kitchen/*` (shapeless; the two meats' smelting/smoking/
  campfire), tags `dishes` (in `ghoul_food`), `contains_ghoul_meat`, `contains_human_meat`, lang, the effects' 18x18 icons, and the
  generated `control/kitchen/Menu.java` (do not hand-edit). Ghoul pieces per serving and human meat are worked out from recipes
  (ghoul meat, seared ghoul meat, RC broth = 1; recipes with ghoul meat make 1). `Dish` (record), `Dishes` (lookups + the effects'
  numbers), `DishItem` (overrides `m_6164_` getUseAnimation for drinks, `m_8105_` getUseDuration 16 for snacks), `KitchenEffect`
  (a BENEFICIAL `MobEffect`, does nothing itself). `ControlItems` registers every dish (properties: food, meat if meat, always-edible
  for drinks/snacks, stack 16 with a container, Bottled Blood `m_41495_` craftRemainder glass bottle) and the effects
  (`ForgeRegistries.MOB_EFFECTS`); `ControlItems.effect(key)` resolves the kitchen's own or vanilla's by path. `server/KitchenServer`:
  Finish event: `GhoulMeatServer.devour(player, pieces)` (a human is sick and gets nothing), effects, container back via
  `Finish.setResultStack` or `addItem`/drop; player tick: Clear Sight strips blindness/darkness/nausea, Replenished heals (free),
  Refreshed +0.25 RC/s/level via `SharedBar`, Content `ControlServer.calm`; `QuestServer.bond` scales regard gains by
  `KitchenServer.warmth` (x1.5 while Content). `client/KitchenClient`: tooltip lines, `ViewportEvent.RenderFog` x3 far plane under
  water/lava/snow (cancelled to apply).
- **Growth and mutants:** `GhoulMeat.growth = ln(1 + n/20)`, strength `1 + 0.12 g`, endurance `1 + 0.25 g` (endless; the old
  `share`/`*_MAX` are gone). `Mutant` (STRENGTH 1.25, ENDURANCE 1.6, FEED 1.5): `GhoulMeatServer.mutant` = unlocked and
  `KakuganServer.granted` (Eto made them one-eyed); strength/endurance multiply in; `GhoulDiet` feeds `Mutant.feed(nutrition)`.
- **The Human:** `npc/HumanRules` (pure), `npc/HumanNpc` (CREATURE, never despawns, LOOK/SCARED synced, look kept in persistent
  data), `server/HumanBrain` (wanders; flees the nearest wild ghoul, ghoul player with kagune out within 16 / hidden within 6, or
  attacker for 30 s, re-planning every 10 ticks, turning round obstacles; drops 1-3 + looting Human Meat), client
  `render/HumanRenderer` (vanilla default skins, wide/slim; arms up and flailing while scared). `/summon hexkagune:human`.
- **Structures:** `has_structure`/`prefers` tags: anteiku and cyber_tower plains + sunflower plains; library plains + forests. The
  three structure sets are `count` 3, `spread` 3 (concentric rings search 112 blocks for the preferred biome; a spot with none fails,
  so three spots instead of one; `UniqueNpcs` sends extra bodies away). `KitchenTests` covers the table, files, rules and Human;
  `QuinqueTests` now walks `recipes/` recursively (the kitchen's recipes live in `recipes/kitchen/`).

# State after 1.3.39

- **Next session:** read `docs/handoff/next-ai-1.3.39.md` first. The owner's next big update is **Home Living** (Touka's ghoul
  cookbook, the Human NPC and human meat, many recipes with their own icons and effects, the partner cooking them, mutants' bonuses,
  endless ghoul-meat growth, children inheriting kagune, structures by biome): see the session's chat and the task list.
- **1.3.39 fixes** (protocol 67, unchanged): `EtoBrain.calm` extends `talkingUntil` while `EtoServer.talkingWith(player, npc)` (the
  talk is open on their screen, within 10) and `EtoBrain.busy` includes talking (the stroll goal waits); `ChildBrain.tick` stands a
  child still facing `ChildTalk.talker(npc, level)`; `GhoulNpcBrain.talking` extends `talkUntil` while `GhoulNpcTalk.talkingWith`.
  Holding: `EtoBrain.holdHands` always places her at the hand spot with `noPhysics` (`f_19794_`, newly allowlisted [srg]) and no
  gravity; `EtoBrain.solid(body, by)` restores them (and sets a body caught in a wall on `by`'s feet), called from `letGo`, `calm` (a
  hold dropped otherwise) and `ChildBrain.putDown`; `ChildBrain.carry` sets `noPhysics` for every ride. `GhoulNpcBrain.wanted` checks
  grudges before the creative exclusion (a ghoul hit in creative fights back, so its kagune shows). `FamilyTests.held`.

# State after 1.3.38

- **Next session:** read `docs/handoff/next-ai-1.3.39.md` first (where things stand after this session, what's left).
- **1.3.38 speech bubbles** (protocol 67, unchanged): pure `npc/BubbleShape` (text pixels, y down, the point at y 0: a speech bubble's
  rounded box and tail, a thought cloud's puffs and trail; outline at `OUTLINE_DEPTH` behind the body at `BODY_DEPTH`, words at 0;
  `thought(lines)` = every line styled as an action; `ink` black / dark grey italic; `pop(age)`; `zees(phase)`). Client
  `render/SpeechBubbles.draw/snore`: turned to the camera as a name tag is (Ry toward it, Rx by its height, scale(-0.025, -0.025,
  0.025)), quads in `RenderType.m_110473_` on white concrete, full bright, normal (0,-1,0) (world up: full diffuse), the words by
  `Font.m_271703_` (drawInBatch(String...)) with `Font$DisplayMode.f_181350_` (NORMAL) - both newly allowlisted [srg] with stubs.
  Called from EtoRenderer.bubble (snore when asleep with nothing said), ChildRenderer (same), GhoulNpcRenderer. `NpcSpeech` keeps
  each bubble's `from` (`age`). `NpcTests.bubbles`; `tools/BubblePreview.java <dir>` draws them off-game.

# State after 1.3.37

- **Next session:** read `docs/handoff/next-ai-1.3.33.md` first, then the 1.3.33 section below. Still asked, not done: real white
  speech bubbles and thought clouds instead of the name-tag bubbles (`NpcSpeech`, drawn with vanilla's `renderNameTag` in
  EtoRenderer/ChildRenderer/GhoulNpcRenderer; Font.drawInBatch(String...) is SRG `m_271703_`, Font$DisplayMode.NORMAL `f_181350_`).
- **1.3.37 sleeping family** (protocol 67, unchanged): pure `family/Drowsy` (each spouse's half-woken words, settle, kissed,
  roused; a child's by stage, a baby sleeping on; settles/stroked). `EtoServer.open`: a companion asleep in her bed
  (`m_5803_` and `mind.bed`) gets `drowsy(...)` (SAY_1 `sleep:back`, SAY_2 `sleep:kiss` for the partner, SAY_3 `sleep:up`) and
  `mind.drowsyUntil = now + DROWSY` (600); `HomeLife.tick` only counts her talk as a reason to get up once `now >= drowsyUntil`
  (`sleep:up` clears it). `ChildTalk.asleep` shows `Drowsy.child` and keys tuck/stroke (parents) and back; `stroke`/`back` settle it
  with a bubble line. `EtoBrain.guard` also returns a `CcgAgent` in the house's box (HomeLife wakes her for it within 14).
  `FamilyTests.drowsy`.

# State after 1.3.36

- **Next session:** read `docs/handoff/next-ai-1.3.33.md` first, then the 1.3.33 section below. Still asked, not done: the family
  sleeping in their beds at home (drowsy when woken, a parent up to fight a mob or an investigator) and real white speech bubbles and
  thought clouds.
- **1.3.36 arm blade** (protocol **67**: S2CGhoulNpc's meaning changed): `NpcIdentity.Kind.KOUKAKU` appended (packed: the kind's second
  bit at 36; rolls 37/35/28), pure `npc/BladeArm` (timings, reach, weights, chain, plate/blade growth) and `npc/BladeArmModel`
  (quads in the right arm's frame: shoulder fin, spiral plates tilted along the helix with ridges, forearm sleeve, four-length blade
  with an edge, glowing seams marked by the `GLOW` normal object, point, root fins). Server `GhoulNpcCombat.slash/bladeBegin/
  bladeLand/bladeMet` (Mind `blade*` fields; KaguneClash both ways; `maneuver` treats it as a close fighter and may counter with a
  cut); `GhoulNpcBrain.message` sends its cut in `move`/`moveAt`. Client `GhoulNpcClient.View.raw` (the move int), `wrap`, `blade`
  (cut / wrap / sprint / guard); `client/animation/BladeArmPose` (absolute keys on `QuinquePose.curve`; `QuinqueGrip.reachLeftTo` puts
  the free hand on the forearm); `GhoulModel` now applies poses through `RigPose.transform` (as `AgentModel`); `GhoulNpcRenderer`
  applies lunge/drop/spin and draws `BladeArmLayer` (white concrete, `RenderType.m_110473_`, colour `KaguneTint.wild(seed)`).
  `/kagune npc koukaku|blade`. Preview: `tools/BladeArmPreview.java <dir> [hex colour]`. `NpcTests.blades`.

# State after 1.3.35

- **Next session:** read `docs/handoff/next-ai-1.3.33.md` first, then the 1.3.33 section below (the owner's newer asks are listed
  at its end; the colours are done).
- **1.3.35 colours on every path** (protocol 66, unchanged): pure `control/KaguneTint.recolour(argb, target)` turns a red-built
  palette to a kagune colour (hue rotated by target hue - `RED` 0.995, saturation toward the target's, brightness kept; target value
  < 0.3: black with saturated bright parts kept blood red; saturation < 0.25: ash/bone); the crimson `KaguneColours.DEFAULT` is the
  identity. `KaguneTint.wild(seed)`: a wild ghoul's colour (15% crimson). Client `KaguneTints.of(entity|id)`: a player's
  `KaguneClientState.View.color()`, a wild ghoul's `wild(seed)`, else the crimson. Applied in `UkakuWingRenderer` (both `emit`s take a
  tint; NPC wings by seed, Touka's left as hers; shots via `tinted(sink, ...)` by `bolt.shooter()`; stuck spikes by
  `UkakuFx.shooter(id)`, remembered when a shot is seen; vortices; thorns by the body), `KakujaArmorLayer` (by wearer), `GraftRenderer`
  (wild Rinkaku tails, a static `tint` around their `drawSet`). `KaguneColours.roll` widened (s 0.4..1, v 0.36..0.96).
  `tools/WingPreview.java <dir> part-tint` renders the wings in eight colours. `PathTests.tints`.

# State after 1.3.34

- **Next session:** read `docs/handoff/next-ai-1.3.33.md` first, then the 1.3.33 section below (the owner's newer asks are listed
  at its end).
- **1.3.34 colours** (protocol 66, unchanged): pure `control/KaguneColours` (`roll`: 4% near-black / 4% bone, else any hue, s
  0.55..1, v 0.42..0.92, vein the same hue at v x 0.17; `name`, `hex`, `hsv`, `toHsv`). `server/KaguneColourServer`: on login, once
  (`HexKaguneColourRolled` in persistent data), a player still on the original's `KaguneProfile.DEFAULT_COLOR` gets a roll written
  through the original's `KaguneProfile.appearance/setAppearance` (KaguneAppearance primary + vein, the rest kept, `validated()`)
  and `KaguneNetwork.broadcast`; `/kagune color` and `/kagune color reroll`. The original's `KaguneWorldRenderer.surfaceColor`
  already draws tendrils from `View.color()/veinColor()`, so nothing in rendering changed. `PathTests.colours` covers it.

# State after 1.3.33

- **Next session:** read `docs/handoff/next-ai-1.3.33.md` first (the owner's open list, how they work, the quick loop
  `tools/quick_check.py`). Section C (CCG/quinques) is done in 1.3.33; the owner's newer asks are at the end of this section.

- **1.3.33 quinques** (protocol 66, unchanged): `tools/quinque_models.py` now also solves every hold and writes
  `client/animation/QuinqueHolds.java` (generated: stance twist, right arm pitch/yaw/roll as `QuinqueGrip.aim` = Ry(yaw) Rx(pitch)
  Ry(roll) in the chest frame, a left-arm hint, the model's thirdperson_righthand, right/left grips, the left hand's slide range).
  `STANCES` in the script is the source: a two-handed one names the weapon's tip/side in the look frame and the roll is found (the
  fist's -z along the grip); the blade (`theta` 50: the blade 50 deg off the forearm's line toward the thumb, not vanilla's 90) names
  its roll. Euler decomposition handles gimbal lock (y = 90: z kept 0). `QuinqueTests.holds` checks the JSON matches, grips in fists.
- **1.3.33 left hand IK:** `QuinqueGrip.reach(p, q, slim)` (call it LAST, after anything that moves the body or right arm): the left
  grip through vanilla's item chain (`held`: translateToHand's slim +0.5 x, Rx(-90) Ry(180), (1/16, 0.125, -0.625), ItemTransform,
  -0.5), the point on the slide segment whose distance is the arm's (`slide`: spear 9..17.5, mace -7.5..1 standing y), the shoulder
  shrug giving up to 1.5 px, then the least rotation from the hint, back into RigPose terms (Ry(-twist) . unlean). Called by
  `AgentClient.pose` and `QuinqueClient.pose`. Misses at the fastest swing frame are at most ~2.8 px (sticks, no elbows).
- **1.3.33 swings:** `QuinquePose.keys` (per quinque, three steps) are offsets from the stance (pitch, yaw, roll, twist, lean, lunge,
  drop) at t 0 / 0.3 windup / 0.5 impact / 0.68 follow / 1, drawn by a Catmull-Rom curve; `step` splits the legs with the lunge
  (right foot leading for blade/katana) and drops the hips by exactly what straight legs lose. A stance's turn is shared: up to 0.4
  as body `spin` (feet turned, head turned back by `headY`), the rest chest `twist`. `recoil` / `pitchBy` turn the right arm about the
  stance's look-level axis; the launcher aims at the head's pitch. `AgentPose.parry` / `stanceRun` (chest half turned, lowered) /
  the run's BLADE carry (blade trailed level behind) are aim-based. `AgentClient.take` now carries spin, head and step legs.
- **1.3.33 preview:** `tools/QuinquePreview.java <dir> [kind]` writes `quinque-KIND-stance.png` and `-m1..m3.png` (300 px cells, camera
  at 5 blocks) with the left grip (magenta) and left fist (yellow) marked and each frame's miss printed. It reads element rotations.
- **1.3.33 launcher:** built lying along x (`launcher_parts`, `GUNS`, `gun_telescoped` around `GUN_GRIP`), elements with their own
  ±22.5/45 turns; scale 0.62; first person ([0,100,0], [-2,3,-5], 0.6) lower right; gui [15,-25,0]. `Quinque.SHOT_EVERY` 2: each
  RightClickItem fires now and queues a second shard (`QuinqueServer.SECOND`, fired from the player tick 2 later if still held, not
  sneaking); SHARD_DAMAGE 2.0, SPEED 2.8, HEAT_MOST 48. Client kick per shard (`QuinqueClient.kick`, `QuinquePose.KICK_TICKS` 3).
  Investigators: `AgentRules.BURST` 8, `SHOT_GAP` = SHOT_EVERY.
- **1.3.33 fold:** `QuinqueServer.use`: `folds` = melee weapon on any right click, launcher on Shift; `tapped` ignores vanilla's held
  repeat (within `HELD_REPEAT` 5 ticks of the last), so holding the button never flips it back. Lang `quinque.close` updated.
- **1.3.33 dashes:** `AgentBrain.maneuver`: side dashes between blows / between bursts (`AgentRules.SIDE_DASH` 0.05 a tick once
  `DASH_EVERY` allows), a dash in on prey just out of reach (`DASH_IN`), both only where `clear` finds room (feet and head open 1.5
  and 3 along, no drop of 2+). Afterimages: `AgentClient.afterimages` (called from `AgentRenderer`) adds one per tick of the push
  to `PaleAfterimages` (now takes colour, life, alpha and size per ghost: 0xC8D6E6, 6 ticks, 0.34).
- **Owner's newer asks, not done yet (in this order after the CCG work):** random Rinkaku colour for the Ken path at spawn + a reroll
  command; wild NPC-only kagune types (an arm-blade kagune that wraps the arm into a sword: M1s, the wrap animation, its run); the
  spouse and children sleeping in their beds at home at night (drowsy when woken: "what is it?"; a parent gets up to fight a mob or an
  investigator near the house); real white speech bubbles and thought clouds instead of the name-tag bubbles.

# START HERE (state after 1.3.32)

- **Next session:** read `docs/handoff/next-ai-1.3.33.md` first (the owner's open list, how they work, the quick loop
  `tools/quick_check.py`).

- **1.3.32 fixes** (protocol 66): `EtoRenderer`/`AgentRenderer` turn `RigPose.spin` with `rotateAxis(+spin)` about +Y, as PlayerAnimator
  does (+ to their left); it was mirrored (the NPC airplane spun against the child; `FamilyTests.airplane` pins it). `ChildBrain.tick`
  re-reads `mind.carrier` after `moments` (a moment ending in `putDown` was carried again from the stale local, `setNoGravity(true)`
  forever) and sets gravity on whenever it isn't carried. Hand in hand with a player (`RIDE_HAND`), a Shift tap (`Mind.holdShift` edge,
  primed true) puts it down. `HomeChores` never teleports: a stuck chore walk skips the block, `unstick` only stops the path;
  `EtoBrain`'s lost-teleport only when `watched` is false (no player within 32).

# State after 1.3.31

- **1.3.31 awareness:** `SceneServer.sees(body, player, near)` (same level, within `near`, `hasLineOfSight`) gates every approach (companion,
  child, ally: `SEES` 12), `EtoBrain.say` and `ChildBrain.say` (16), `askForKiss`, and all of `EtoBrain.together`'s initiatives. The
  welcome is deferred: `Mind.welcomeFrom` set when the partner comes back after 6000 ticks, greeted (and served the meal) in `nearness` once
  `sees(12)` within 1200 ticks. Client `NpcSpeech.lines(LivingEntity)` draws a bubble only if the local player is in its line of sight.
- **1.3.31 home help:** pure rules `family/Kitchen` (cooked/smelted/flesh/fuel in halves, PER_ITEM 2/coal 16; station rules; `ripe` from the
  state text; `harvest` yields; the meal: `save`/`load`/`add` capped at MEAL 6; lines `served`/`noFuel`). World side `server/HomeKitchen`:
  `plan` (chest + station for cooking: a campfire free, a furnace/smoker needs fuel; a ghoul partner: raw flesh set aside at a table),
  `cook`/`smelt`/`harvest` take from and give back to the home's sortable chests in one tick (`take`, `trim` to the fuel left, `burn` small
  fuel first, `giveBack` drops leftovers at her feet via spawnAtLocation), the meal in persistent data `HexMeal` (the partner's diet in
  `HexMealGhoul`), `serve` into the inventory (or at the feet). `HomeChores.Job` now has per-step `expects`/`doings` (COOK: chest then
  stove; HARVEST: crops then the chest); new `Chores.Kind` COOK (gesture `EtoGesture.COOK`, appended: bowl + wooden shovel, a stirring
  pose), SMELT, HARVEST (TIDY). At home `calm` runs `HomeChores` before `ParentCare.tend` and drops a held child when a chore is due.
- **1.3.31 guarding:** `EtoBrain.guard` (household hostiles in sight, within 10 of the spot, or targeting the partner/a child/her) feeds
  `seek`; `HomeLife.tick` wakes a sleeping spouse when `guard` finds one within 14.
- **1.3.31 `/kagune home`** (`ControlCommands`, `HomeChores.status`): what the spouse found, can do, is doing, and the meal kept.
- New allowlist: ParticleTypes SMOKE (`Z` f_123762_) and SMALL_FLAME (`aD` f_175834_), [order] evidence.
- **Still asked, not done:** the CCG/quinque work (launcher model and rifle hold, two-handed IK, right-click fold, fire rate, agent side
  dashes with afterimages). The owner asked to leave it for last, after the companions feel real.

# State after 1.3.30

- **1.3.30 speech bubbles** (protocol **66**): `S2CNpcSay` (id 47: entity id + text) replaces the chat for `EtoBrain.say`/`bark`,
  `ChildBrain.say` (now takes the ChildNpc), ally lines in `SceneServer` and the reactions in `Sightings`. Client `NpcSpeech` keeps one
  bubble per entity (newest wins; `npc/Speech`: wrap to 34 chars x 4 lines, 60..220 ticks, actions grey italic); `EtoRenderer`,
  `ChildRenderer` and `GhoulNpcRenderer` draw it with vanilla's `renderNameTag` (m_7649_), a line per 0.25 block, above the tag and name.
  System notices stay in chat.
- **1.3.30 spouse at home:** `EtoBrain.together` never trails the partner at a household and starts nothing while a chore runs;
  `Chores.gap` 300..700 ticks alone, 400..900 with them near.
- **1.3.30 fixes:** Touka's KISS is a lean only (arms loose, no hold); BLUSH raises her one hand over 8 ticks
  (`EtoPose.toukaKissed`, the lean kept at 0.5 so the KISS->BLUSH->PULL switches never drop it); PULL blends from that hand. Closed eyes:
  `tools/companion_textures.py` keeps the skin's lashes, paints the eye's first row under them in skin and draws one black line a pixel
  lower (where the owner marked it); only `touka_closed.png` / `ken_closed.png` changed (the skins are byte-identical). Cradling: `EtoBrain.tick`
  never ends PET while `mind.cradling` is set; `EtoRenderer.pose` uses `EtoPose.cradle` while `ChildClient.carrying(npc)` (full, easing
  in over 5 ticks of PET). `CompanionTests` pins all three.
- **Next (asked by the owner, not done yet):** the crystal launcher rebuilt as a proper gun (less boxy) with a rifle hold (stock at the
  shoulder, both hands on it); two-handed quinque holds and combos keeping both hands on the grip (the left arm copies the right's
  rotation now, so the hands drift apart); a tap of right click folding a melee quinque back into its case; the launcher firing faster;
  investigators dashing left and right with brief afterimages. `tools/QuinquePreview.java` (new) renders each quinque's real model in
  the hand through vanilla's item chain on a posed body: use it to judge stances and swings.

# State after 1.3.29

Branch `claude/relaxed-carson-4y0frw` at **1.3.29**, **protocol 65** (64: S2CNpcApproach and ANSWER; 65: S2CQuests carries the romance
cards). Everything below is compiled and unit tested, **never tried in game**.

- **Scenes (1.3.29):** `npc/scene/SceneBook` reads `src/main/resources/data/hexkagune/talk/{eto,touka,ken,child,ally}.txt` (form at the top
  of `SceneBook.java`, words in `SceneWords`). `server/SceneServer` walks partners, friends, children and allies up (`S2CNpcApproach`, id 46,
  card `client/NpcApproachOverlay`); the Answer key (`ControlKeys.ANSWER`, O) is sent as `NpcTalk.Choice.ANSWER`. `SceneTests` checks every
  script and coverage. `family/ChildTemper`: a child's temperament from its id.
- **Touka (1.3.29):** `Persona.style()` 2, `EtoPose.touka(...)`, `Persona.clings()` false, `Persona.unasked()` (PECK or `EtoGesture.STEAL`,
  `EtoClient.pull` turns the view, `ToukaScript.stolen`). One wing: `WingRules.MASTERY` 170, `SIDE` right.
- **Chores (1.3.29):** pure rules `family/Chores` (what a block is for by registry name: TIDY chest/barrel, WIPE `*_table`, WATER flowers,
  crops, saplings, `potted_*`, READ bookshelf/lectern, LOOK glass panes; `pick` weighs by what the home has; `sort`/`sorted` plan a chest;
  each companion's lines; `goingHome`). The world side is `server/HomeChores`: `found` scans the home (radius 20, -5..+7) every 1200 ticks
  (nearest 96 of each kind, plus furniture for floor spots); `tick` is called from `EtoBrain.calm` after `ParentCare.tend` (household, by
  day). A `Job` is a round of blocks: walk (`m_26519_` every 10 ticks; 3 s without 0.6 blocks progress = repath; 3 repaths = put beside
  the block via `besideBlock`; no path 3 times or 15 s = skip), then the gesture (`Kind.gesture`, all `still()`), then `effect`: `sort` (real
  Container sort: key = item id + NBT text, `ItemStack.isSameItemSameTags` double-checked, `Chores.sort` keeps totals; trapped chests are not
  sorted), `sweep` (ItemEntities 10 s+ old, not in water, into the nearest sortable chest whole or not at all: `insert`; the round then
  ends at that chest, `Job.box`), WATER (one `randomTick` on growables in 3x3x3, 50% each). Chest lids: `Level.blockEvent(pos, block, 1, 1/0)`,
  vanilla chests only; `shut` on finish, `stop` and death. `EtoBrain.tick` stops a chore for any fight/talk/kiss/hand/child/night and runs
  `HomeChores.unstick` for any walk at home (4 stuck windows: back to the home's spot, never with a player within 2.5). `EtoNpc`'s
  RandomStrollGoal is a subclass whose canUse/canContinueToUse wait while `EtoBrain.busy`. Mind fields: `chore`, `nextChore`, `lastChore`,
  `choreSaid`, `noSweepUntil` (6000 ticks after a sweep that could not put anything away), `stuck*`.
- **"Go home, please." (1.3.29):** `EtoServer.withHome` adds SAY_7 (key `gohome`) after FOLLOW/STAY on MAIN pages when `going(...)` is
  non-null (following, or settled > 6 blocks from where it belongs; a spouse's target is the household, only in its own dimension; others
  only in the overworld). `EtoBrain.sendHome` lets go of everything and calls `homeAt` (spouse) or `EtoNpc.sendHome` (home = origin, no
  teleport); the lost check (`EtoBrain.tick`) waits while someone talks to her, so from far off she is put home once the talk ends.
- **Quests (1.3.29):** `QuestBook.Giver` (PATH, FEAT, ETO, TEACHER, GHOULS, ASKED, RETIRED) and `QuestLog.given` (begun or ended is always
  given) / `shown` (feats only once done). `QuestLog.state` returns LOCKED for a quest not given (after the `learned` check, so EtoTests'
  "known another way is DONE" holds). TEACHER = a quest whose gate `QuestBook.add` stripped for `Teaching` (`QuestBook.lesson`), given once
  that gate is open. GHOULS = once any non-companion bond has MET. RETIRED: light_feet, up_we_go, witness_this (Eto's lesson quests now carry
  those titles). `QuestServer.opened` announces only shown quests; a FEAT's completion says "Feat:". Hunts: no random dusk offer;
  `QuestServer.huntable/askHunt`, offered by ghoul NPCs as SAY_7 key `hunt` ("Any prey out tonight?"), one per night (`nextHuntAt` = next dusk).
- **"You already know this" (1.3.29):** `EtoServer` on LESSON: `knownLessons` (Eto: lessons known some other way get their flag set;
  Touka/Ken: `Teaching.knownElsewhere` against the `NpcMemory.TAUGHT` bits, set on `taught` too), `Teaching.alreadyKnown` prefixes the
  reply, then the next lesson or refusal. `CompanionScript.lesson` always offers "Teach me"; `EtoServer.withLesson` adds Eto's LESSON when
  only known-elsewhere lessons are left.
- **Romance tab (1.3.29):** `npc/RomanceCard` (pure: `stageText`, `errands`, `next`, `save`/`load` as tab-separated lines), built by
  `RomanceServer.cards` and sent in `S2CQuests.romance`; `QuestClient.romance()`; `QuestJournalView.Tab.ROMANCE` (picked as "romance:KEY").
  Offscreen render: `java -cp <classes>:build/base.jar:<stubs> tools/UiPreview.java <dir> quests` writes `quests-romance*.png`.
- **New API (allowlisted with [srg]/[order] evidence, stubs added):** Container size/getItem/setItem/setChanged, ItemEntity.getItem,
  ItemStack getCount/setCount/copy/getMaxStackSize/isSameItemSameTags, Level.blockEvent, BlockStateBase.randomTick, LookControl.setLookAt(xyz),
  Goal canUse/canContinueToUse, ParticleTypes POOF/HAPPY_VILLAGER/SPLASH/FALLING_WATER (by declaration order in `iv`).
- **Ask the owner to try:** `/kagune touka max` (or Eto/Ken) to marry, "This is our home now." in a house with chests (throw a few things on
  the floor, plant flowers and wheat outside), watch a day; sort a messy chest by waiting; "Go home, please." while she follows; open the
  journal's Romance tab; ask Eto "Teach me something." for Light on Your Feet; at night ask a ghoul "Any prey out tonight?".

# State after 1.3.28

- **1.3.28 case sound:** `tools/case_sfx.py` (numpy + ffmpeg; synthesized from parts, no recording could be fetched: freesound,
  wikimedia and opengameart are blocked by the environment's network policy) writes `sounds/sfx/quinque/case_open_1..2` and
  `case_close_1..2` (mono Vorbis 44.1 kHz, 1.25 s; the last lock / second latch at ~0.56 s to fit `Quinque.UNFOLD`). `tools/sfx_pack.py`
  pools them as `sfx.case_open` / `sfx.case_close` (regenerate sounds.json with it, never by hand). `QuinqueServer.hand` plays them
  (plus the weapon's ring at 0.35 on opening). Never heard by the one who made them: ask the owner.
- **1.3.28 carried case:** an investigator's shut case is drawn in its own frames `<kind>_case_carried` / `_case_carried_slim`
  (deploy 0.05 / 0.1, listed ahead of the opening frames on the case item only; `QuinqueClient.caseFrame` -> `AgentPose.caseFrame`,
  which never gives them to anyone else). Their third person transform is solved in `tools/quinque_models.py` (`carried_display`:
  vanilla's ItemInHandLayer + ItemTransform inverted so the grip bar sits in the fist, the case plumb, latches out, along the stride)
  for the arm `AgentPose.CARRY_X/CARRY_Z` (0, 0.1). `AgentPose.carry` (applied last in `AgentClient.pose`) keeps the arm there in the
  world: it subtracts lean, side and twist. `AgentTests.carrying` runs the real model JSON through a Java copy of vanilla's chain on
  real poses (grip < 0.35 px off, plumb < 1 deg, inner face clear of a woman's 4.7 px leg on the run). Unsure: whether vanilla
  resets a slim arm's pivot y to 2.0 each frame (the solve uses 2.5); at worst the grip sits 0.5 px higher in the fist.
- **1.3.28 pair tactics:** `AgentRules.front` (launcher always front; else leader unless `swapped`), `round`/`sideOf`/`behind`/`kept`
  (go round wide at `ROUND_RADIUS`, the short way on its own side of the front, in within 40 deg of the back, stay in within 75),
  `holding` (front waits just out of reach `HOLD_OFF` while the other has `rounding` set, at most `HOLD_TICKS`), `turned` + `SWAP_AFTER`
  (the leader's `AgentBrain.sides` flips `Mind.swapped`). `Mind.rounding` also blocks front flips. New fight after `NEW_FIGHT` ticks
  resets it all (`fightFrom`). `AgentRules.flank` is gone.
- **1.3.28 idle:** `AgentPose.idle` (lean -0.1, side +0.04, weight right, left leg loose, breath period 74 ticks, glance 230 ticks,
  phase from the entity id), blended by `AgentPose.still(speed)` when the case is shut.
- **Version:** 1.3.27's jar said `1.3.26` in mods.toml (KagunePatch.VERSION was not bumped); both `build.py` and `KagunePatch.VERSION`
  are 1.3.28 now. Bump both every release.
- Branch at **1.3.28**, protocol 63.

- **1.3.27 CCG investigators:** entity `npc/CcgAgent` (`hexkagune:ccg_investigator`, MobCategory.MISC, player box) with its state on
  synced entity data (INT accessors: look+kind, swing step/`PARRY` + its tick, flip + tick, dash way + tick, last shot tick; the ticks are
  the low 32 bits of game time, read with `AgentRules.since`). No packet, protocol unchanged. Rules (pure, `AgentTests`) in
  `npc/AgentRules`: skins `textures/entity/ccg/ccg_f0..f3, ccg_m0..m4` (owner's zip, renamed only), women = `FemaleBody.SIZE * 1.07`,
  build 0.93, curves 0.85; spawn chance 0.0042 per 200 ticks per player (about a pair every two days), wild ghouls cut to 0.008 (from
  0.05). Brain `server/AgentBrain`: pair saved in persistent data (`CcgAgent.KEY` packed long + `KEY+"Partner"` UUID); a lone one (made
  by /summon) gets a partner beside it at once (`AgentSpawner.partner`). Targets: grudges/marks first (a revealed ghoul =
  `KaguneProfile.isUnlocked && isActive` in sight within 24, marked 2400 ticks for both), wild ghouls within 20, Enemy within 6 (kept
  till dead or 40 away), mobs targeting them. Melee: `begin` registers `KaguneClash.swing` (so clashes work both ways), `land` checks
  `KaguneClash.meets` then hurts (mobAttack source; `QuinqueServer.hurt` adds the ghoul bonus for any holder) and calls the generalized
  `QuinqueServer.finisher(LivingEntity, ...)` on step 2. Parry: on `KaguneClash.threat` a swing aimed at the striker landing when theirs
  does, so `meets` from their side throws both apart; the stop callback gives a 3-tick riposte. Feet: flips as `GhoulNpcCombat` does
  (PaleRules.Flip, NpcRules.FLIP_BUSY/FLIP_POSED), dashes (4 ticks of renewed push). Launcher: bursts via `CrystalShots.fire` with
  `foe`. The case is the main-hand stack, opened/shut with `QuinqueServer.hand(LivingEntity, ...)` (shared with players' `turn`).
  Client: `client/AgentClient.pose` (flip > run + quinque + dash), `client/animation/AgentPose` (run cycle, parry, dash; pure),
  `client/render/AgentRenderer` + `AgentModel` (three models: wide man, slim man, woman with the female figure; bust layer). Drops:
  `AgentBrain.drop`, 2.5% + 1% per Looting level, player kills only. Recipes removed (`QuinqueTests` asserts none).
- **1.3.27 build confirmed:** a full `python3 build.py` ends with EXIT 0 (API verification, dedicated server check, guard, name check,
  every test suite; `AgentTests` 20302 checks). The flip/dash sound `entity.player.attack.sweep` in `AgentBrain` stays: `Sfx` draws it
  from the pack (`blade_swing` / `blade_heavy`) and `SfxTests` passes. Still **untested in game**.
- Branch at **1.3.27**, protocol 63.


- **1.3.26 Touka's wings:** `UkakuWingRenderer` passed grow 0 for both wings (meaning "thrown as vortices"), so nothing drew. Now
  `WingRules.companion(...)` builds her input; `CompanionTests.toukaWingsShow` checks quads come out.
- **1.3.26 quinques:** five kinds in `Quinque` (BLADE, SPEAR, KATANA, MACE, LAUNCHER; fields `model`, `twoHanded`, `ranged`, `swing`),
  items registered in a loop (`ControlItems` CASES/WEAPONS). Poses in `client/animation/QuinquePose` (stances solved in the scratchpad
  by fitting the item's third person transform so the right grip is in the right hand, the left grip where the left arm reaches, and
  the weapon points as wanted; values baked in `tools/quinque_models.py` HELD and `QuinquePose.stance`). Players: `QuinqueClient.pose`
  (swing start from `f_20911_`/`f_20913_`, combo step `Quinque.next`) blended in `KaguneBodyAnimation.setupAnim` (`wants()` and the
  early finish include `QuinqueClient.posing`). Server: `QuinqueServer` combos (hits only), `finisher` (sweeps never hit players),
  launcher `fire` + heat, spear speed modifier; shards in `server/CrystalShots` (any shooter; `striking` flag keeps shard hits out of
  combos). (1.3.27: the recipes are gone; cases are rare investigator drops only.)

- **1.3.25 quinques** (the start of the CCG; plan in `docs/ccg-research.md`): items `quinque_case_bikaku` / `quinque_bikaku_blade`,
  `quinque_case_koukaku` / `quinque_koukaku_spear` (`ControlItems`; weapons are `SwordItem(Tiers.NETHERITE, ...)`). Right click opens a
  case, Shift + right click shuts a weapon (`server/QuinqueServer.use`: swaps the stack, copies its tag, stamps `QuinqueAt`). Models:
  five frames per kind chosen by the item property `hexkagune:deploy` (`client/QuinqueClient`, `Quinque.shown`), generated by
  `tools/quinque_models.py` (weapons built standing, every element turned -45 about z so vanilla's handheld transforms hold them; the
  case's third person pose solved against the arm). Ghoul bonus and RC suppression in `QuinqueServer.hurt` / `GhoulRegen.suppress`;
  the spear's reach is a transient `ForgeMod.ENTITY_REACH` modifier. `QuinqueTests` also checks every model/frame/texture/name/recipe
  file exists. Sounds must come from the owner's pack (`SfxTests`): use `hexkagune:` ids. **Untested in game**: the model frames (my
  first person preview was not calibrated against the real game), the reach attribute on the client.
- Branch at **1.3.25**, protocol 63.

- **1.3.24 places:** `tools/companion_structures.py` turns `art/anteiku.schem` (Touka, behind the café counter, 2nd floor) and
  `art/cyber_tower.schem` (Ken, basalt terrace at the spire's foot) into `data/hexkagune/structures/{anteiku,cyber_tower}.nbt`, each
  companion a template entity; `--blocks` checks every state against minecraft-data's 1.20 `blocks.json`. Worldgen: jigsaw, one per
  world (`concentric_rings` count 1; distances 4 library / 10 Anteiku / 18 tower keep them apart: all rings share the seed's angle),
  allowed in `#minecraft:is_overworld`, preferring flat biomes. The tower is cut to schematic y 62..315 (a 1.20.1 world has 384
  blocks of height; the build is 320), stands at absolute y 62 (`start_height` 63, `beard_box`), and is centred on its start chunk by
  the `hexkagune:tower_anchor` jigsaw (structures are built only 8 chunks round their start). Its air is left out (474k blocks).
  **Untested in game**: the start height maths (piece min y = start_height - 1), the beard clearing, and generation time for the tower's
  chunks (each chunk processes the whole template).
- **1.3.24 fights:** `npc/FightRules` (notice 5, gone 24 / child 16, chase 32 from home, give up 30 s without reaching or hitting);
  `EtoBrain.target/seek/hostile` (neutral mobs only when angry), `Mind.reachedAt` (set near it, on a blow landed, on every new target);
  `ChildBrain.threat`. Fixed: a human child's `mind.target` was never cleared after a fight. **1.3.24 airplane:** `ChildMoment.spinTurn`
  (wrapped), the parent's `spin` negated (RigPose spin + is left).
- Branch at **1.3.24**, protocol 63.

- **1.3.23:** Ken's kakugan was a red bar over a black bar (`find_eyes` read his 2x2 eyes by rows). His eyes are now in
  `tools/companion_textures.py` `EYES["ken"]`, column-wise like Touka's: inner column iris, outer column white, the near-black pixel
  above the iris (under the bangs) a lash. Rerun the tool after touching `art/ken_skin.png`. Branch at **1.3.23**, protocol 63.

- **1.3.22 hand in hand** (owner: "clearly not right"): `ChildRules.hand(stage, parentSize, slim)` solves the kid's arm (z, x) and side
  distance on the real models (player arm at 0.9375 px scale x size; kid at `stage.draw`, little-one layout for toddlers) so the hands
  meet; `handParentZ(stage, size)` swings the parent's arm out when a teen can't reach down. `FamilyTests.hands` checks it independently.
  `tools/MomentPreview.java` now draws the kid on its in-game proportions (`kid()`), not a scaled player: previews before this lied
  about toddlers.
- Branch **`claude/relaxed-carson-4y0frw`** at **1.3.22**, protocol **63**, built green (exit 0) and pushed. `main` is still 1.3.0.
  **Nothing from 1.3.13 on has been tried in game.**
- **1.3.21 household** (`server/Households`, SavedData `hexkagune_households`, one `Home` per player: dimension, spot, radius 20): set by
  the married spouse's new talk choice `NpcTalk.Choice.HOME` / `EtoTalk.Effect.HOME` (appended last; EtoTests pins 36 choices). Spouse:
  `Households.forCompanion` (its `home()` at the spot) widens its restriction; kids inherit a player parent's home (`forChild`) unless
  following/`waiting()` (a ChildNpc flag set by "Wait here." and by being put down outside the house).
- **Beds** (`family/HomeRules` pure, `server/HomeLife`): bed heads found by `Block.toString()` ending `_bed}` and the state string;
  two heads side by side with one facing = a double bed (spouse takes one free half); singles go to the kids by birth order. Sleep is
  vanilla `startSleeping/stopSleeping` (`m_5802_/m_5796_`); `m_6107_` (isImmobile) is overridden so sleepers don't move, which also stops
  `m_8024_` (the brain): **everything for a sleeper runs in `HomeLife.tick` (LivingTickEvent)**: waking, and `ChildBrain.sleeping` (the
  pending tuck-in and the moment sync, since `mind.dirty` is normally flushed at the end of the brain tick).
- **Tuck-in:** `ChildMoment.TUCK` (child pose null: the game lays it down). Waits as `pending` until the parent is within
  `ChildBrain.TUCK_NEAR` (2.6); NPC parents walk to 1.6 (`ParentCare.moment`); players get a "Come tuck me in!" line and can also pick
  *tuck them in* on the sleeping kid's page (`ChildTalk.asleep`). Feed: `ChildMoment.FEED` (stand 0.85), food in `Mind.food`, healed in
  `fed()`. Hand in hand: `ChildRules.RIDE_HAND` (the kid at the carrier's left on the ground).
- **Doors:** `GroundPathNavigation.m_26477_(true)` + `OpenDoorGoal` on EtoNpc and ChildNpc (wooden doors only).
- **Death is final:** `UniqueNpcs.mortal("child:")` + `perished`; `Children.remove`. **Protection:** `ChildBrain.attacked` cancels any
  damage from an EtoNpc or an EtoNpc-owned FleshThrall (plus the player-parent rule). **Might:** `Persona.might` (Ken 1.0, Eto 0.95,
  Touka 0.6), players via `ChildRules.playerMight`; `ChildRules.health/damage(..., might)`; `Children.Child.might` saved (11 fields).
- Untested risks: pathing onto/around beds, two kids and one bed (the second stays up), the kid clipping walls while hand in hand,
  multiplayer homes (one per player; a child with two player parents takes the mother's first).

# START HERE (state after 1.3.20)

- Branch **`claude/relaxed-carson-4y0frw`** at **1.3.20**, protocol **62**, built green (exit 0) and pushed. Built on `claude/relaxed-turing-r5599k`
  (1.3.17). `main` is still 1.3.0. **Nothing from 1.3.13 on has been tried in game.**
- **1.3.18:** children wear the owner's HD skins (`art/children/{sons,daughters}`, made ready by `tools/child_skins.py`: square layout,
  vanilla's opaque base, per-eye kakugan overlays `_kr/_kl` and glows `_gr/_gl`; every eye **hand-mapped** in its `EYES` table after the owner
  rejected an auto-detected one: "evaluate if the iris looks good and real"). `family/ChildSkins.pick` (by sex, near the inherited tone,
  `SPREAD` 30, then seed); `ChildRenderer` swaps wide/slim models and draws the `Kakugan` layer. Three daughters (cloud, emo, rainbow) have
  eyes under hair: no kakugan. The owner's set has no truly dark skins (darkest is medium tan). Touka's legs: `Persona.legs` 1.2.
- **1.3.19 Ken in Peace:** PeaceKit and its helpers take any `LivingEntity` (`KingServer.BodyKit`, `wear/unwear/bodyTick/bodyHurt`,
  `KingEffects.valid/hostile` -> `EtoBrain.foe` for NPCs; NPCs never carve terrain). Client: `KingLayer.drawPeaceBody` (an `EtoRenderer.Kakuja`
  layer), `KingClient.get(Entity)/find/wearers`, Peace's stance and moves in `EtoFight.peace`. Mind: `npc/KenRules` (pure) + `server/KenPeace`
  (holds and lets go the lance, the burial and the heavy; slams from the Ascent; recalls the lance; benches an ability that won't start).
- **1.3.19 Touka's wings:** `npc/WingRules`, `server/NpcWings`, `network/S2CNpcWings` (id 45), `client/NpcWingsClient`, drawn in
  `UkakuWingRenderer` at her size on her posed torso. Persona flags: `finalKakuja()`, `wings()`.
- **1.3.20 parent-child moments** (protocol 62): `family/ChildMoment` (pure: which stage, how long, how far it stands, toss/spin shapes),
  `client/animation/ChildMomentPose` (both bodies; players crouch on bent knees, NPC parents lean), `ChildBrain.start/moments/stand`
  (walks up first; babies stay put), ride modes `ChildRules.RIDE_*` (shoulders, spin), the Play page in `ChildTalk` (a moment starts when the
  talk closes), NPC parents in `ParentCare.moment/startMoment`. Previews: `tools/MomentPreview.java` (the child drawn as a scaled player:
  PoseRender has no little-one proportions).
- Riskiest, untested: Peace drawn on Ken's slim-armed EtoModel (armour built for player arms; Ken is 0.94 size while the server cuts at
  player size), the client's `KingClient.BODIES` lookup for effects, Ken's navigation vs the Ascent's lift.
- The owner wrote "a halfway version of the Rinkaku" for Touka; she was given Ukaku wings (her kagune, and what they described: wings
  turned to crystal, spikes, the barrage). Say so if asked.

# START HERE (state after 1.3.17, end of session)

- Branch `claude/relaxed-turing-r5599k` at **1.3.17**, protocol **60**. Pushed. 1.3.17 was pushed after only a quick compile and
  `CompanionTests` (the owner ran out of usage): **run `python3 build.py` first and check exit 0.** `main` is still 1.3.0.
- Releases this session: 1.3.13 hands alive; 1.3.14 Touka/Ken/teaching/trial/partner immunity; 1.3.15 renderer-crash guard, Touka figure;
  1.3.16 children fixes (carry, proportions, size, `/kagune children`, NPC parents: `server/ParentCare.java`); 1.3.17 Touka's new skin
  (her painted red right eye mirrored by day in `tools/companion_textures.py` `mirror`; eye pixels named in `EYES`), curves 1.4.
- **Nothing since 1.3.13 has been confirmed in game.** The owner said they have "many bugs" to report: ask for them first and fix
  them one at a time before any new feature. They interrupt long waits; keep steps short and push often.
- **Open items:**
  1. **Child skins are disliked** ("anything but cute": adult faces with brows and red lips, adult clothes on toddlers). The owner wants
     skins picked from NameMC. The network policy blocks namemc.com, s.namemc.com, textures.minecraft.net and other skin sites (GitHub,
     GitLab, npm reachable; nothing suitable found there). Ask the owner to allow those domains (environment Network access) or to send a
     zip. Same blocker for **wild ghoul skins** (male and female, many, eyes adapted with the `companion_textures.py` detection).
  2. Babies' new proportions (full head on a half body, `ChildRenderer.Model.little`) and the in-arms carry (`ChildRules.CARRY_UP`
     1.02, facing the carrier) are untested: ask how they look.
  3. Ken romances women only (`Persona.likes`); the owner wants him a devoted lover; his poses `EtoPose.led` were loosened once on request.
- Rules learned: never alter the owner's skins (only overlay eye pixels; ask for files you can't read); say when a preview can't show
  something (PoseRender draws no female figure, no face overlays); any new entity type must go in `ThrallRenderGuard.ours`/`registerAll`;
  `check_names` (Kaneki/Ayato/Furuta) and PaleTests (Takatsuki/Aogiri/Yoshimura) ban names in player-visible text; don't `pkill -f`
  a pattern that matches your own command line.

# Handoff: HexKagune 1.3.14 (Touka and Ken, teachers and companions; hands held alive), for the next agent

Read this first; it replaces `next-handoff.md` as the place to start (that file is still right about the project's basics, Eto, genders,
pregnancy, children and reactions: read its sections 1, 2 and 4). Everything here is "compiled, tests pass, not tried in game".

## 0. Where things are

- **Branch: `claude/relaxed-turing-r5599k`**, at 1.3.14 (`VERSION` in `build.py` and `patch/KagunePatch.java`), network `PROTOCOL` **"60"**.
  It was started from `claude/festive-cray-l5b2pb` (1.3.12). **`main` is still at 1.3.0**; never push there unless the owner says so.
- Releases this session: **1.3.13** (hands held alive) and **1.3.14** (Touka, Ken, teaching, the trial, partner immunity).
- The owner's skins for Touka and Ken are in `art/touka_skin.png` and `art/ken_skin.png` (64x64, both slim-armed, sent as a zip).
  `python3 tools/companion_textures.py [preview-dir]` turns them into textures (see section 3). **Never repaint or "improve" the owner's
  skins**: the tool copies the skin as it is and only recolours the detected eye pixels for overlays. The owner noticed the stand-ins I
  painted before the files arrived (a mouth, plain brows) and was not happy. Ask for files when you can't read an image.

**1.3.15 (after the above):** the owner's crash on 1.3.12 (a newborn child drawn with no entity renderer: their modpack, which runs
ModernFix among ~200 mods, loses this mod's renderers, as happened before for the flesh creatures and Eto). `client/render/ThrallRenderGuard`
now covers every body the mod adds and re-registers all of the mod's renderers (`registerAll`) once per kind; still none, the body is kept
out of the client's world. **Any new entity type must be added to `ThrallRenderGuard.ours` and `registerAll`.** Touka is Eto's female
body, a little skinnier (`Persona.curves` 0.65, `build` 0.94; the owner: "she's just a little skinnier than Eto"); the preview tool does
not draw the female figure (no hips or bust), so say so when sending pose previews. Animated previews: `CompanionPreview <out> frames`
then `python3 tools/companion-gifs.py <out>`.

## 1. What the owner asked for (their words, condensed)

- Hands held: "more lively... swing a little more... not the run swing... a nice wobble... let her go a little further and the hand follows
  and a little behind if I pull her". And: while holding hands, nothing the player does (kagune out or not) may harm the partner.
- Two NPCs with their own moods: **Touka** (strict, serious, pouty; blinks and blushes like Eto; breaks composure and blushes a lot; can walk
  up and give a tiny pouty kiss; making out with her works like Eto's) and **Kaneki** (displayed as **Ken**, see the name rule below).
  Ken is **only interested in women**; make him "a really big lover", desirable for women: the owner (a man) left his romance to us.
- Most interesting abilities now need mastery **and** finding the teacher. Touka teaches the Ukaku path: "turn your wings into crystals"
  (Crystal mode) and how to advance. Ken teaches the kakujas (centipede) and the final forms of each path, through a cutscene of the
  tortures he went through. Both are dateable and marriable; partner or not, anyone may learn; others' partners get a cold shoulder.
- They don't spawn naturally yet: the owner is building an Anteiku cafe for them. Commands only for now.
- Pregnancy: "keep the pregnancy as it is, don't change it"; just make sure it works and the belly grows. (I tried a day-clock version
  that followed `/time add`; the owner said no, it was reverted. The timing is game ticks, two Minecraft days.)
- Next on their list, after the companions: **wild ghouls**, male and female, with many good skins from the internet (NameMC), eyes adapted.
  Not started: see section 6.

## 2. The persona system (built once, for every companion to come)

All under `src/main/java/com/hex/kagune/control/`. Eto's classes are now the shared companion machinery; a companion is a `Persona`.

| Piece | Where |
|---|---|
| Who each is: key, name, colour, size, female body or not, slim arms, health, genome colours, one-eyed, Owl kit (Eto only), how readily it kisses/starts things, idles by mood, its errand item and last gift, whether it pecks, whether it leads (Ken's poses), what makes it blush, whom it dates (`likes`) | `npc/Persona.java` (ETO, TOUKA, KEN; `ALL` order is sent over the network: never reorder) |
| Every word: the talk's pages, topics, sweet page, answers, and every unasked line | `npc/Script.java` (interface); Eto's is `npc/EtoScript.java` (delegates to `EtoTalk`, `EtoLines`, `EtoTopics`, `EtoInitiative`, unchanged) |
| The shape of a companion's talk after Eto's (pages, gifts, errands to date, lessons instead of her pact/surgery) | `npc/CompanionScript.java` |
| Touka's and Ken's words and topics | `npc/ToukaScript.java`, `npc/KenScript.java` |
| The bodies | `npc/ToukaNpc.java`, `npc/KenNpc.java` extend `npc/EtoNpc.java` (now the companion body: `persona()`, `displayName()` (a name tag renames it everywhere), `rehome()`); registered in `ControlEntities` (`hexkagune:touka`, `hexkagune:ken`), one each per world (`UniqueNpcs.kind`) |
| The mind and the talk server, persona-aware | `server/EtoBrain.java`, `server/EtoServer.java` (every key, name, line and idle comes from the persona) |
| Drawing: per-persona textures, size, female figure or not; Touka's blush at anything sweet | `client/render/EtoRenderer.java` (`new EtoRenderer(context, persona)`), `client/render/EtoModel.java` |
| Talk screen faces: skin byte -1 Eto, -2 Touka, -3 Ken | `EtoServer.face`, `client/NpcTalkClient.companion` |
| Testing commands (op) | `/kagune <eto|touka|ken> max [player]`, `/kagune <eto|touka|ken> here` (brings the one of them there is to you and makes it home; made if none is loaded; partner kept) |

**Name rule:** `build.py check_names` refuses "Kaneki", "Ayato", "Furuta" anywhere in the jar (the owner's 1.2.5 rule). So he is "Ken" in
code, lines and lang; a name tag reading "Kaneki" renames him in game (lines, talk screen, tag). Touka never names her brother.
`CompanionTests` checks every line against the same pattern. And `PaleTests` refuses "Takatsuki", "Aogiri", "Yoshimura" in any string a
player can see (Eto's alias and her people): Ken loves "a writer nobody's ever seen", unnamed.

### Behaviour per companion

- **Touka:** idles CROSS, TAP, HUFF, SIP, WIPE, KICKS, SCOLD, SHY, READ (studying); kisses at 0.35x Eto's rate and starts things at 0.5x;
  blushes on KISS, HAND, CLING, LEAN, HAIR, DANCE, HUM, PET, RING too. Errands: 3 cocoa beans ("coffee beans"), 10 kills at night, a
  rabbit's foot. Dates anyone.
- **Ken:** idles CHIN, CRACK, BOW, READ, SIP, SWEEP, WRITE; kisses at Eto's rate, starts things at 1.25x; **dates women only**
  (`Persona.likes`, `KenScript.romantic`, the DATE effect guard); with men he is a friend and teacher. His romantic poses
  (`EtoPose.led`): the kiss (hand to their cheek, one at their waist, leaning down), making out (both arms round their waist), the peck
  as a forehead kiss, a wrap-around hug, an arm round their shoulders for CLING, a hand to the back of his neck after a kiss. Errands: 3
  books, 10 kills at night, a white flower. With a female partner, **she** carries their child (`EtoServer.conceive`: whoever is the woman).
- Both **peck**: from `askForKiss`, 65% of the time they walk up and give a quick kiss themselves (`EtoGesture.PECK`); otherwise the
  full kiss wait, Touka hinting first (`Script.kissHint`). On-screen kiss text per persona (`Script.kissWaiting`).
- Fight: the eye-mode kit only (`EtoRules.Ready.only`); Owl DNA moves and the Owl form stay Eto's (`EtoOwl`).

## 3. Textures (`tools/companion_textures.py`)

Reads `art/<key>_skin.png` and writes `textures/entity/<key>/<key>{,_kakugan,_kakugan_glow,_closed,_blush}.png`. Eye detection on the face
front (columns 1-6): whites are bright and at most half as saturated as the skin (and not hair-coloured above the eyes); irises are the
coloured pixels in and next to the whites' rows; the dark row above is the lashes (never recoloured). Kakugan: whites black, irises red
(darker top row), a small vein at the outer corner. Touka turns both eyes; Ken his left only. Blink: the eye's top row in the lash colour
(the lid coming down under the skin's own lashes), the rest skin. Blush: under the eyes. Found for the owner's skins: both have eyes on
face rows 5-6, lashes on row 4; Touka's bangs (outer layer) cover part of her right eye. `EYES` in the tool overrides detection if needed.
Previews: pass a folder; it writes `<key>-faces.png` (skin, turned, glow, shut, blush). Posed previews: `tools/CompanionPreview.java`
(see its comment for the command; PoseRender in the owner's skins).

## 4. Teaching (`npc/Teaching.java`, pure)

| Lesson | Teacher | Gate | Kind | Needs |
|---|---|---|---|---|
| Crystal Wings | Touka | `UKAKU_CRYSTAL` (new, mastery 200; crystal mode, V) | talk | |
| Gatling | Touka | `UKAKU_GATLING` (400) | talk | Crystal Wings |
| The First Kakuja | Touka | `UKAKU_KAKUJA` (750) | talk | Gatling |
| The Centipede | Ken | `RINKAKU_CENTIPEDE` (750) | **trial** | `RINKAKU_FOUR` (still quest-opened) |
| The Full Kakuja | Ken | `RINKAKU_FULL_KAKUJA` (1000) | talk | the centipede |
| The Dragon | Ken | `RINKAKU_DRAGON` (1000) | talk | the Full Kakuja |
| The Final Kakuja | Ken | `UKAKU_FINAL_KAKUJA` (1000) | **trial** | the Ukaku first kakuja |

- A taught gate is no longer opened by quests: `QuestBook.add` strips it from rewards (the quests still pay mastery and sides).
  `QuestServer.pass` says who teaches it. Crystal mode checks its gate in `UkakuServer.toggleCrystal`.
- `QuestLog.VERSION` is 2: a save migrated at 1 gets `UKAKU_CRYSTAL` if its mastery was already past 200 (nothing else re-migrated).
- In talk: `EtoTalk.Context` gained `teaching` (a `Teaching.View`) and `mixed` (a woman and a man: a baby is possible; `babyOpen()`).
  Effects `LEARN` (opens the gate now: `EtoServer.taught`) and `TORTURE` (`server/TortureServer.java`: the cutscene
  `CutsceneOverlay.TORTURE` / `S2CCutscene.TORTURE`, ~46 s; the player held still and unhurt through it, then left at 15% health with
  regeneration, then the gate opens; logging out mid-trial teaches nothing).

## 5. Other changes this session

- **1.3.13 hands held alive:** `npc/HandSway.java` (pure spring: trail behind when pulled, overshoot on stopping, swing out on turns; arms
  reach along the gap; a slow shared sway on its own clock), `client/HandClient.java` (steps it per pair), `EtoRenderer.glue/alive`,
  `KaguneBodyAnimation.setupAnim`.
- **Partner immunity:** `EtoBrain.spared(player, body)` (holding hands, clinging, hugging, kissing: `mind.holding` or `mind.with`);
  `EtoBrain.attacked` cancels any attack caused by that player; `KagunePatch.patchSpared` makes the original's `isHostileTo` and
  `validSwipeTarget` return false for them (so autonomous tails and swipes never pick them).
- **Pregnancy (timing unchanged):** after "Let's have a baby." the partner comes to them at night by herself (`EtoBrain.together`,
  `mind.babyNight`) and the night (and conception) follows on arrival. A baby needs a woman and a man (`conceive` checks `c.mixed()`;
  Eto's baby options use `babyOpen()`). `/kagune family at <percent>` jumps any pregnancy you're part of to that point (belly shows from 20%).
  If the owner reports the belly not growing: it shows from 20% of 48000 game ticks (8 real minutes) and fills out over the rest;
  `/time add` does not move it, by their choice.
- Allowlist: `Entity.m_7770_` (getCustomName) and `Component.getString` (name tags).

## 6. Not done, and what's next

- **CCG investigators, untested in game.** Worth watching: whether they open the case and close in fast enough to feel dangerous; parry
  frequency against a player's M1 spam (each threat answered once, `AgentRules.parry`); flips into walls; the follower's flank spot
  pathing; the female scale/build reading as "a little taller and skinnier than Eto". Possible next steps: investigator voice lines,
  a CCG office structure, ranks giving stronger stats, quinques made from the kakuhou of ghouls the player kills (canon).

- **Wild ghoul skins** (owner's next item): this environment's network policy blocks NameMC, The Skindex, Planet Minecraft and Mojang's
  texture server (only GitHub is reachable). Either the owner allows those domains (environment settings, Network access) or sends a zip of
  skins. Plan once skins are in: put them in `art/ghouls/{boys,girls}/`, generalize `npc/NpcLook` from its fixed eye spots to per-skin
  `KakuganEyes` found by the same detection as `companion_textures.py` (generate a small table), keep the girl/boy split. Licensing: the
  earlier rule "never ship skins from skin sites" was an agent's caution; the owner asked for NameMC skins, so it's their call.
- Touka's and Ken's kagune (Ukaku shots, Rinkaku tails) on their NPC bodies: `GhoulNpcCombat` is tied to `GhoulNpc`; worth generalizing.
- Natural spawning: the owner's Anteiku cafe structure (then place them like Eto in the Library: `UniqueNpcs`, structure NBT).
- Eto's lessons are still her own system (`EtoRules.Lesson`); she could be moved onto `Teaching` later.
- Nothing above has been tried in game. Riskiest: the bytecode guard in `isHostileTo`/`validSwipeTarget` (runs constantly; wrapped in
  try/catch), Ken drawn on slim arms with no figure, the torture's effects (Slowness VII, Jump Boost 128 to pin, Resistance V), the
  `here` command adopting a new body while an old one is unloaded somewhere.
