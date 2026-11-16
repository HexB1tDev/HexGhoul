> **Superseded: read `docs/handoff/next-ai-3.6.6.md` first** (the newest state; 3.6.3 to 3.6.6 shipped; Potential Unlock in progress).

# HANDOFF: HexGhoul after 3.6.3 (Grand Devouring shipped). READ THIS FIRST.

## The session after this note (same branch): the owner's words, and where it got to

Binding; quoted exactly. The ask that started it:

> You're taking over HexGhoul, a Forge 1.20.1 Tokyo-Ghoul-inspired kagune mod (HexKagune), from another AI. Work on branch
> `claude/hopeful-noether-5c2wex`. Push only there; never push to main; no PR unless I ask.
>
> Read `docs/handoff/next-ai-3.6.3.md` first and do what it says, in its order. Then read `docs/handoff/owner-words-mutations.md` (my
> words for the two Bikaku mutations, quoted exactly, with my pictures in `docs/handoff/owner-refs/`) and `docs/handoff/next-ai-3.5.4.md`
> (the house rules and my earlier words).
>
> Dependencies: we are using GeckoLib as a dependency along with Photon and PlayerAnimator. Use any of these libs whenever needed to smooth
> out the mod as much as possible. GeckoLib is a requirement; its mods.toml entry is already in the work in progress, and the handoff says
> how to stub and allowlist it.
>
> What to do:
> 1. Finish 3.6.3 (Grand Devouring and the Bikaku mutations). The server, the network and the drawing are written and compile. Still to
>    do: the mutations in the Bikaku menu, the tests, then the version paperwork, a full `python3 build.py` to EXIT 0, commit, push. Ask
>    me first whether the reshaped arch in `docs/handoff/bikaku-devour-3.6.3.png` is right.
> 2. Then 3.6.4: the victim's view from inside the mouth (with GeckoLib), and Grand Devouring in the kakuja.
> 3. Then Potential Unlock, built on Pale's "You Thought You'd Get Rid of Me That Easily?" pieces, with my kagune colour on the strands.
>
> Follow the house rules exactly:
> - Each fix or feature is its own version (the last number never reaches 10), with release notes, a README entry, the handoff sections,
>   both version strings, a full build ending with EXIT 0, a commit and a push.
> - Nothing is tested in game: never claim it.
> - Answer me in English. Quote my words exactly as I wrote them; don't add things I didn't say.
> - Show me previews of what you make before you go far.
> - If I report bugs, fix those first, each its own version; if my exact words for a report aren't in the handoff, ask me.
> - When a version is done, tell me plainly what you made, what you guessed at, and what's next.

Their words on the arch preview, the cooldown, The Walker and how the mutations unlock are at the end of
`docs/handoff/owner-words-mutations.md` (quoted exactly). In short (exact): "its not meant to be that big at all, never even close";
"the kagune is literally VERTICAL from the point of origin, strange as fuck why is it not arching and progressively increasing size";
"Forgot to mention but Grand Devour should have a cooldown of 1 minute"; "feel free to improve the walker if you think it's viable and a
better choice  using geckolib"; "these mutations are NOT unlocked through mastery they're gonna be unlocked through an NPC's quest
introduced later today." They chose an operator command until the quest is in, and will say later who builds the quest.

**3.6.3 shipped** (Grand Devouring; protocol 103; the version is 3.6.3, the next 3.6.4). The reports on the unreleased preview were fixed
inside 3.6.3 (as 3.5.1's Pursuit was), and the owner was sent `docs/handoff/bikaku-devour-before-after-3.6.3.png` and the new sheet; they
had not said whether the new shape is right when 3.6.3 was pushed: ask. Details: the top section of `docs/handoff/companions-handoff.md`.

**3.6.4 shipped** (a fix: the owner, on `/kagune cooldowns off` not ending running Bikaku cooldowns: "it should."). The owner, on the
first draft of the view from inside the mouth (`docs/handoff/devour-inside-draft-3.6.5.png`; its generator and preview in the next
version): "I like the inside of the mouth, proceed wit hthe mod". So the versions moved on by one:

**3.6.5 shipped** (Grand Devouring from inside the mouth: the mod's first GeckoLib model; details in the top section of
`companions-handoff.md`). Then the owner: "Grand devouring should not be in the kakuja form by the way." So the kakuja part below is
dropped (it already never starts there).

**3.6.6 shipped** (a fix: the owner, "The RC bar does not exist and is not meant to be a thing anymore, the energy and FOOD bar are
the same, RC bar IS NOT A THING": the original RC bar is never drawn). There is ONE bar: energy and food (the flesh reserve). Never make
or mention an "RC bar". Potential Unlock (the owner answered the plan with "proceed", and "Should be Purple, the bar, my bad") is next:
(1) 3.6.7: the mutation, three times the RC, the one bar purple; (2) surviving lethal blows while RC lasts, and rare tearing-off of parts
that fall with his skin and vanish from his model; (3) the strands in his kagune colour pulling each part back over about 5 s. A stash
`potential-unlock-part1-wip` may hold the first part's work if the session was cut short.

**Next, in this order (as it was before 3.6.5; items 1's kakuja part dropped):**
1. **3.6.5 (done):** the victim's first-person view from inside the mouth (GeckoLib; matching `GrandDevour` exactly: the same `at(...)`, the
   jaws' aperture, the cavity's `hole`/`depth`, the devourer's kagune colour), and Grand Devouring in the kakuja (one of its six tentacles
   feeds) as **3.6.6**, its own version.
2. **Potential Unlock** (below), unlocked the same way (`BikakuDevour.grant`; quest-only, not mastery).
3. The NPC's quest that unlocks the mutations, when the owner says (they will tell you; `BikakuDevour.grant(player, mutation, true)` is
   ready for it).
4. The Walker with GeckoLib, only if it is clearly better (the owner: "feel free to improve the walker if you think it's viable and a
   better choice  using geckolib"). The judgment so far, told the owner: its feet are placed on blocks by code each tick, which keyframed
   animation can't do alone; a GeckoLib model with code-driven bones would be about what `WalkerLegs` already is. Weigh it, and ask before
   changing it.

**Fixed in 3.6.4:** `/kagune cooldowns off` didn't end Bikaku cooldowns already running (the owner: "it should.").

---

# (The note as the session before left it; its "still to do" for 3.6.3 is done.)

Branch **`claude/hopeful-noether-5c2wex`** (push only there; never `main`; no PR unless the owner asks). Start yours from it:
`git fetch origin claude/hopeful-noether-5c2wex && git checkout -B <your-branch> origin/claude/hopeful-noether-5c2wex`
(or keep working on that branch if the session gives it to you).

Then read, in this order:
1. **`docs/handoff/owner-words-mutations.md`**: the owner's whole request for the two Bikaku mutations (Grand Devouring and Potential
   Unlock) and everything they said after it, quoted exactly. Binding. Their three pictures are in `docs/handoff/owner-refs/`.
2. `docs/handoff/next-ai-3.5.4.md`: this session's earlier words from the owner (3.5.4 to 3.6.2), the house rules, and the judgment calls.
3. `docs/BIKAKU_PATH.md` and the top sections of `docs/handoff/companions-handoff.md` (one per version, newest first).

## DEPENDENCIES: GeckoLib, Photon and PlayerAnimator (the owner, binding)

The owner, word for word: **"use geckolib it is meant to be a requirement"**, then **"Make the handoff very clear from now on that we
are using Geckolib as a dependancy along with Photon and player animator. We should use any of these libs whenever needed to smooth out
the mod as much as possible."**

So the mod requires all of these. Use them wherever they make the mod smoother:
- **GeckoLib 4** (Forge 1.20.1; mod id `geckolib`; the 1.20.1 branch is 4.8.4): authored models with keyframed animations (`.geo.json`
  and `.animation.json`), and code-driven bones.
- **Photon** (`com.lowdragmc.photon`, already used by `client/fx/RcFx.java`, `PaleSteam.java`): particles and effects.
- **PlayerAnimator** (`dev.kosmx.playerAnim`, the body layer `client/animation/KaguneBodyAnimation.java`, priority 60, posed through
  `RigPose`): the player's body.
- **bendylib**: bent limbs, with PlayerAnimator.

The original jar's `META-INF/mods.toml` already declares PlayerAnimator, bendylib and Photon as mandatory (client side). **GeckoLib is
now declared too:** the 3.6.3 work in progress adds it as mandatory on BOTH sides in `patch/KagunePatch.java`, next to the version
replacement (`versionRange="[4.4,)"`, ordering AFTER).

Nothing calls GeckoLib in code yet. To use it:
- **Stubs:** compile-only stubs go under `stubs/software/bernie/geckolib/...`.
- **Allowlist:** each class, method and field you use needs an entry in `verify/api-allowlist.txt`. Add a `[geckolib]` evidence key to
  its header, as Photon has `[photon]`. The build fails on any reference that isn't proven or allowlisted.
- **Where to read GeckoLib's real signatures:**
  - Reachable from this environment: `https://raw.githubusercontent.com/bernie-g/geckolib/1.20.1/<module>/src/main/java/...`. The
    modules are `core` (`software.bernie.geckolib.core.*`: animatable, animation, AnimatableManager, AnimationController, RawAnimation,
    PlayState) and `Forge` (`software.bernie.geckolib.*`: renderer, model, util, cache.object.GeoBone, animatable.GeoEntity, GeckoLib).
    `gradle.properties` there gives version 4.8.4.
  - Not reachable: the GitHub API and github.com pages (403), and the jars (cloudsmith, Modrinth, BlameJared are refused by the proxy).
    So you can't compile against the real jar. Read the exact source of each signature you stub and quote its path as the evidence.
- **Unchanged in this mod:** the Bikaku worm is the mod's own procedural mesh (`MawWorm`, `MawMotion`, `GrandDevour`), and so are
  Pale's revive pieces (`client/animation/Remains.java`, `client/render/RemainsRenderer.java`). The `awakening/` folder has a pure-Java
  `.geo.json` baker written "exactly as GeckoLib 4 bakes it" (`awakening/src/.../geo/GeoModel.java`, `GeoAnimation.java`, `Molang.java`),
  useful for previewing a geo model off-game.

## The house rules (short; full text in `next-ai-1.3.60.md` and `next-ai-3.5.4.md`)

- **One version per fix or feature;** the last number never reaches 10, so after 3.6.9 comes 3.7.0. Each version has:
  - `VERSION` bumped in `build.py` and in `patch/KagunePatch.java`;
  - `.github/release-notes-<v>.md` and a README entry;
  - a new top section in `docs/handoff/companions-handoff.md` ("# START HERE (state after X)", the old one renamed "# State after Y");
  - the header and a log line in `docs/handoff/next-ai-2.0.45.md`;
  - `docs/BIKAKU_PATH.md` updated, and this file kept current;
  - a full `python3 build.py` ending with **EXIT 0** (about 6 minutes; run it in the background; don't edit sources while it runs);
  - a commit `3.x.y: ...` ending with the trailers `Co-Authored-By: Claude <noreply@anthropic.com>` and the session's
    `Claude-Session:` line (no model names anywhere pushed);
  - then `git push -u origin claude/hopeful-noether-5c2wex` at once.
- The quick loop is `python3 tools/quick_check.py [--noverify] SuiteName ...`.
- **Nothing is tested in game. Never claim it is.**
- **The owner's words:**
  - Answer in English, plainly.
  - Quote the owner's words exactly and add nothing they didn't say. They were angry once at a paraphrase.
  - Bug reports come first, each its own version; if their exact words for a report aren't in the handoff, ask them.
  - Show previews before going far (`tools/MawPreview.java <out> <sheet>`, compiled against `build/quick/classes`). When a version is
    done, say what was made, what was guessed and what is next.
- **Banned:**
  - In the jar: Kaneki, Ayato, Furuta.
  - In visible text: Takatsuki, Aogiri, Yoshimura, and no character names on screen (keep "Noro" and "Dr. Octopus" out of anything shown
    in game).
- **Sounds:** only the hexkagune pack's real recordings, by their `sounds.json` names (for example `hexkagune:sfx.dragon_roar`,
  `sfx.dragon_bite`, `sfx.dragon_slam`, `sfx.grab`, `sfx.eat`, `sfx.drips`, `sfx.gush_long`), played with
  `UkakuFire.sound(level, pos, name, volume, pitch)`. Never make a sound.
- Never alter the owner's files. Leave the hammer and the whip as they are.

## State now

**Shipped this session (all pushed, each built with EXIT 0; none tested in game):**

| Version | What |
|---|---|
| 3.5.4 | The Bikaku kakuja. |
| 3.5.5 | Fix: the worm drawn through the player's own body. |
| 3.5.6 | A wider angle for Predatory Drag. |
| 3.5.7 | The kakuja's four moves and Autonomous Predation. |
| 3.5.8 | Holding to throw further, up to 30 blocks. |
| 3.5.9 | Going round blocks. |
| 3.6.0 | The forms: One, Twin (250), Three (500). |
| 3.6.1 | Every move made by all the worms. |
| 3.6.2 | The Walker (750): two walking tentacles; climbs walls; no fall damage. |

Protocol "102" in the last shipped version.

**In progress, unversioned (the last two commits; no version bump, no full build): 3.6.3, the Bikaku mutations and Grand Devouring.**

The owner saw `docs/handoff/bikaku-devour-3.6.3.png`:
- first version: **"Im not very happy wit hthe shape ngl"**, **"its not arching smoothly it's very.... weird"**;
- then the reshaped one, every pose one smooth arch. They haven't said whether it is right now; ask.

They also said: **"it can catch targets within 10 blocks with complete freedom and independance, dont be afraid to make it stretch arch
longer, etc"**.

Done, compiling, passing the API verifier and the worm tests (`quick_check BikakuWormTests`):
- **Pure:**
  - `bikaku/BikakuMutation` (GRAND_DEVOURING built; POTENTIAL_UNLOCK not yet; points at mastery 600 and 900, a guess).
  - `bikaku/GrandDevour`, the whole timeline in ticks:

    | Beat | Ticks | Starts at |
    |---|---|---|
    | RISE | 50 | 0 |
    | REACH | 30 | 50 |
    | ENGULF | 30 | 80 |
    | TRAPPED | 80 | 110 (CAUGHT) |
    | SWALLOW | 30 | 190 (SHUT) |
    | DIGEST | 180 | 220 (KILL) |
    | SINK | 30 | 400 |
    | RETURN | 30 | 430 (FED) |

    END is 460. Each pose is one smooth Bezier arch (`build`, `raised`, `hang`, `carry`), with sizes from the victim
    (`hole`, `depth`, `body`, `bulge`, `bulgeLength`), `lift` (the victim drawn up), `shown`, `busy`, and `at(...)` giving
    a `Feeding` (shape, `MawWorm.Feed`, clear girth).
  - `MawWorm.Feed` plus `feedRadius` and `throatRadius` (the swollen body, the bell of a mouth round a cavity, 6 rows of teeth, the bulge),
    and `MawWorm.Pose` with an optional `feed`.
  - `BikakuForm.biter(bite, worms, busy)` and `MawMotion.form(..., busy)`.
- **Server:**
  - `server/BikakuDevour`:
    - mutations in the profile (`BikakuMutations`), and `pick`;
    - the trigger: every 4 ticks, a body within 10 blocks at 2 HP or less, a random free worm;
    - the hold (pinned, drawn up), the stripping (hands emptied into a stash and given back, a player's kagune, kakuja and eyes put away
      every tick), the kill at `KILL` (the stash given back first, then the player's damage source), and the bar refilled at `FED`;
    - cancel if the kagune goes away, he dies, or the body is lost or taken away;
    - the sounds;
    - events: a held body can't attack and can't be hurt by anything else, `RightClickItem` and `EntityInteract` are cancelled, logging
      out gives things back.
  - `BikakuServer.State` has the devour fields, `biteWorm` and `moveSkip`; the snapshot carries the mutations and the devouring.
  - Hooks:
    - `BikakuServer.tick` calls `BikakuDevour.tick`;
    - M1 skips the feeding worm (no worm left: "Your worm is feeding");
    - moves need a free worm (in One Worm they wait) and form moves skip it (`moveSkip`);
    - the kakuja and form changes are refused while it feeds;
    - `C2SBikakuAction.MUTATION` (6) picks one;
    - `GhoulServer.press` refuses a held player;
    - `GhoulNpcBrain.tick` and `AgentBrain.tick` stand a held NPC down (kagune in, quinque sheathed, everything cancelled).
- **Network:** `S2CBikakuState` appends `mutations, devourVictim, devourWorm, devourAt, devourSpot, devourWidth, devourHeight`; protocol
  **"103"**.
- **Client:**
  - `BikakuClient.View` has the devour fields, `devourAge`, `busyWorm`, and the static `devoured(entityId, clock)`.
  - `BikakuRenderer` draws the feeding worm with its `Feed` (overriding whatever else that worm would do); bites and the wind-up skip it.
  - `client/BikakuDevourClient` hides the body once it is swallowed (`RenderLivingEvent.Pre`), hides a held player's first-person hands,
    and zeroes a held player's movement keys.
- Lang keys `message.hexkagune.bikaku.feeding`, `.mutation`, `.no_point`.
- The owner's mods.toml requirement: GeckoLib, as above.
- Preview `tools/MawPreview.java devour` (sheet `docs/handoff/bikaku-devour-3.6.3.png`).

**Still to do for 3.6.3, in this order:**
1. **The menu:** a MUTATIONS section in `client/BikakuMenuView.java` (points left; Grand Devouring pick or ON; Potential Unlock "soon"),
   whose clicks send `C2SBikakuAction.MUTATION` from `BikakuMenuScreen`. The model needs the mask from `BikakuClient.get(player).mutations()`.
   Check it with `tools/UiPreview.java <out> bikaku`, compiled with `build/quick/classes:build/quick/base.jar:build/quick/stubs`.
2. **Tests:**
   - in BikakuWormTests:
     - the victim's box is inside the cavity from CAUGHT to SHUT, at the player, small mob and giant sizes;
     - no frame jumps;
     - the bulge runs from the head to the root, monotonically;
     - `busy` holds until END;
     - the drawn worm is never inside him;
     - the plain worm is unchanged when `feed` is null;
   - BikakuMutation points;
   - `biter` with `busy`.
3. **A Photon touch if it helps:** wet particles at the mouth as it closes and at the gulp. `client/fx/RcFx.java` shows the Photon API in
   use.
4. **The paperwork:** release notes, README, the companions-handoff top section, the `next-ai-2.0.45.md` lines, `BIKAKU_PATH.md`;
   `VERSION` 3.6.3 in both places; a full build to EXIT 0; commit `3.6.3: ...`; push.

**Known limits to tell the owner:**
- Not in the kakuja yet: one of its six tentacles should feed.
- The victim's view from inside the mouth isn't built. That is the next version, and GeckoLib is the natural tool for it: a cavity model
  with keyframed pulsing walls, rows of teeth and a throat into darkness; the jaws closing driven by code from `GrandDevour`'s aperture;
  tinted with the devourer's kagune colour.
- Minecraft gives the mod no way to muffle other sounds; the mouth's own sounds play close and loud instead.
- What was taken from a victim's hands lives in memory. A server crash mid-feeding loses it (a logout gives it back).
- `PlayerInteractEvent.RightClickBlock` isn't stubbed, so a held player can still use blocks.
- An investigator's case may still be drawn while it is held. Check `client/render` for the CcgAgent arm or case drawing, and hide it
  while `BikakuClient.devoured(id) >= 0`.

## Next, after 3.6.3 (the owner's request; order mine)

1. **3.6.4: the victim's first-person view from inside the mouth** (GeckoLib; matching the world animation exactly), and Grand
   Devouring in the kakuja.
2. **Potential Unlock**, built on Pale's You Thought You'd Get Rid of Me That Easily? (`server/PaleRevive.java`, `pale/ReviveRules.java`,
   `client/animation/Remains.java`: skin-cut pieces with raw torn ends, thrown and tumbling, flesh lines pulling them back). It needs:
   - the mutation;
   - the RC bar's colour changed for good, and three times the maximum RC, kept through death, relogging and restarts (RC is the shared
     bar: `server/SharedBar.java`, `KaguneProfile.MAX_ENERGY`, `GhoulReserve`);
   - surviving lethal blows while RC lasts;
   - rare tearing-off (arms at the shoulder, legs, the head, torso pieces) on extreme or lethal blows only;
   - the parts falling with the player's skin and hidden on his model;
   - each part pulled back by strands in **his kagune colour** over about 5 s, independently, a small heal after each;
   - RC paid, paused when it is out;
   - animations skipping missing limbs;
   - all of it synced.
   Use GeckoLib, PlayerAnimator and Photon where they help (the owner).

## Open questions for the owner (ask; don't guess further)

- Is Grand Devouring's new shape right (`docs/handoff/bikaku-devour-3.6.3.png`, `bikaku-devour-before-after-3.6.3.png`)? (Sent; no answer
  when 3.6.3 was pushed.)
- The Walker: "two separate appendages on either": two in all (built), or two on each side? (Asked again; they answered with other things.)
- The mutation milestones: gone. The owner: "these mutations are NOT unlocked through mastery".
