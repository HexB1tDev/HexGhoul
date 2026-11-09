# HANDOFF: HexGhoul after 3.6.6, kept current through 3.6.7. READ THIS FIRST.

## The session after this note (same branch): where it is

- **3.6.7 shipped** (Potential Unlock's first part: three times the energy, the one bar purple; the Bikaku menu's bar labelled
  "ENERGY"). Version 3.6.7, protocol 103; the next is **3.6.8**.
- The owner then sent new words (quoted exactly at the end of `docs/handoff/owner-words-mutations.md`): a new thing ("Whenever an entity
  is absorbed, the player gains a speed buff for 15 seconds and some of our custom effects from foods, any that give special strength and
  buffs also for 15 seconds"), two bugs ("IN kakuja form you cant see anything, the camera needs to adapt to the new size."; "Also Kakuja
  detransformation needs a simple shatter animation like the rinkaku has."), and The Walker ("the walker should lift the player way higher
  up (like 3-4 blocks up)  and adapt better to literally any terrain, the arms should not swing at any point (they currently do). the
  walker is the first tap K half kakuja, the full kakuja is the hold K").
- **Order (bugs first, each its own version):** the kakuja's camera; the kakuja's shatter; The Walker's arms; The Walker on tap K, higher
  and on any terrain; then the absorbing buffs; then Potential Unlock's tearing-off and strands (preview first).


Branch **`claude/hopeful-noether-5c2wex`** (push only there; never `main`; no PR unless the owner asks). Start from it:
`git fetch origin claude/hopeful-noether-5c2wex && git checkout -B <your-branch> origin/claude/hopeful-noether-5c2wex`
(or keep working on that branch if the session gives it to you).

Then read, in this order:
1. **`docs/handoff/owner-words-mutations.md`**: the owner's whole request for the two Bikaku mutations and everything they said after it,
   quoted exactly, to the end (this session's words are its last sections). Binding.
2. `docs/handoff/next-ai-3.6.3.md` (the dependencies, GeckoLib's stubs and allowlist, the session's ask) and `docs/handoff/next-ai-3.5.4.md`
   (house rules, earlier words).
3. The top sections of `docs/handoff/companions-handoff.md` (one per version, newest first) and `docs/BIKAKU_PATH.md`.

## Dependencies (the owner, binding)

GeckoLib (required), Photon and PlayerAnimator: use them wherever they make the mod smoother. **GeckoLib is now in use** (3.6.5,
`client/render/DevourInside`): stubs under `stubs/software/bernie/geckolib/**`, a `[geckolib]` block in `verify/api-allowlist.txt` (every
signature read from bernie-g/geckolib branch 1.20.1, 4.8.4, via raw.githubusercontent.com; the jar can't be downloaded), assets under
`assets/hexkagune/geo/` and `animations/` (allowed in `build.py` `NEW_PREFIXES`), an off-game baker in `awakening/src/.../geo` and its tests
in `awakening/test/.../geo` (they run in the full build).

## Shipped this session (each built with EXIT 0, pushed; none tested in game)

| Version | What |
|---|---|
| 3.6.3 | Grand Devouring (reshaped after the owner's three reports: thin out of his back, one smooth arch, steadily bigger to the mouth; 1-minute cooldown; quest-unlocked, `/kagune bikaku mutation <id> on|off` for operators; menu MUTATIONS; Photon drool). |
| 3.6.4 | Fix: `/kagune cooldowns off` ends the Bikaku cooldowns (the owner: "it should."). |
| 3.6.5 | Grand Devouring from inside the mouth: the mod's first GeckoLib model, drawn round the eaten player's first-person camera along the worm's own curve. Not in the kakuja (the owner: "Grand devouring should not be in the kakuja form by the way."). |
| 3.6.6 | Fix: no RC bar (the owner: "The RC bar does not exist and is not meant to be a thing anymore, the energy and FOOD bar are the same, RC bar IS NOT A THING"). The original's RC bar is never drawn. |

Version **3.6.6**; protocol **"103"**. The next version is **3.6.7**.

**There is ONE bar: energy and food (the flesh reserve, where the food bar was). Never make, draw or mention an "RC bar".**

## Shipped as 3.6.7: Potential Unlock, first part (what was in progress; kept for the record)

The owner answered the plan (three parts: (1) the mutation, the bar purple, three times the energy; (2) surviving and tearing off;
(3) the strands pulling parts back) with "proceed", then "Should be Purple, the bar, my bad", then the RC-bar message above.

The last commit after 3.6.6 is **unversioned work in progress** for part (1) (no version bump, no full build; it compiles, the API
verifier passes, BikakuTests pass). What it has:
- `BikakuMutation.POTENTIAL_UNLOCK` built (grantable by `BikakuDevour.grant` and the operator command).
- `BikakuRules.POTENTIAL_CAPACITY` 3, `POTENTIAL_PURPLE` 0x8A2BE2, `capacity(mutations)`.
- Server `SharedBar.capacity(player)` (Bikaku only) divides `SharedBar.price` by it: energy stays on the bar's own 0..100 scale (the
  original's clamps, its rest recovery `energy < 100`, and its sync all assume it); every price is a third of the bar's share.
- Client `BikakuClient.capacity(player)`; `ReserveHud.render` draws the one bar through `client/ui/TintedCanvas` (every colour
  `KaguneTint.recolour`ed to the purple); `BikakuMenuScreen` passes energy and max times the capacity ("192 / 300"); `BikakuMenuView`'s
  bar purple.
- BikakuTests `mutations()` checks capacity and purple.

**To finish 3.6.7:**
1. Wording: the mutation's description (`BikakuMutation`) must say energy, not RC ("Three times the energy; your bar turns purple. Coming:
   torn apart, you pull yourself back together while energy lasts."); check nothing new says "RC bar".
2. Previews: `tools/UiPreview.java <out> bikaku` writes `potential-bars.png` (the one bar as it is and with Potential Unlock) and
   `bikaku-menu-potential.png`; save them as `docs/handoff/potential-bar-3.6.7.png` and `bikaku-menu-potential-3.6.7.png` and show the
   owner. (The rows of the original RC bar were removed from that sheet: there is no RC bar.)
3. Paperwork: a draft of the release notes is below; README entry, companions-handoff top section, `next-ai-2.0.45.md` lines,
   `BIKAKU_PATH.md`; VERSION 3.6.7 in `build.py` and `patch/KagunePatch.java`; full `python3 build.py` to EXIT 0; commit `3.6.7: ...`;
   push.

Release-note points for 3.6.7 (plain English, the owner's words quoted exactly): unlocked by the quest (operators meanwhile:
`/kagune bikaku mutation potential_unlock on|off`); three times the energy (everything spends a third of the bar; eating fills it as fast,
so it gives three times as much; the menu reads "192 / 300"); the one bar (energy and food) turns purple, the menu's too; kept for good (the
mutation is in the profile: death, relogging, restarts; `/kagune reset` clears it); Bikaku path only. Guesses: the shade 0x8A2BE2; a third
of the price rather than a longer bar; Bikaku only.

## Next after 3.6.7: Potential Unlock's tearing-off and the strands (preview first)

Do parts (2) and (3) together: a torn-off part must be pulled back by strands (the owner: "Do not simply wait five seconds and teleport the
body part back into position"), so tearing off without the strands would break the spec. A draft plan:
- **Server** (`server/BikakuPotential`, new): `LivingDeathEvent` (HIGHEST, as `PaleRevive.dying`): Bikaku + Potential Unlock + energy left:
  death cancelled, health 1, energy paid (`SharedBar.consumeEnergy`), a part torn off by the blow. `LivingHurtEvent`: a single blow of about
  half his max health or more also tears one off (rare; never ordinary blows). No energy: no protection. Parts: head, each arm (at the
  shoulder), each leg, a torso chunk; a part already off is never torn again. Each part: when, a seed, where, the blow's direction; synced
  (extend `S2CBikakuState`, protocol 104, or a message of its own).
- **Timeline per part, independently, about 5 s (100 ticks):** thrown off, falls, tumbles, lands; strands out of the wound (vein-like, his
  kagune colour `KaguneTints.of`), stretching and twisting to it; they take hold of its torn end; they tighten and haul it up and back,
  turning it upright, tissue growing between; home: flesh closes, strands sink in; a small heal only then. Energy paid while pulling;
  paused when out.
- **Client:** missing parts hidden on his model (RenderPlayerEvent Pre/Post: `ModelPart.f_104207_`, as `ChildRenderer` does); in first
  person a missing arm's hand not drawn; the part drawn with his skin and raw torn ends (reuse `client/animation/Remains`: `fly`, `mesh`,
  `openings`; add whole-arm and whole-leg slices without changing Pale's own piece list, which her revive scatters); the strands as tubes
  pulsing; the head off: the camera stays where it is. Use GeckoLib, PlayerAnimator and Photon where they help (the owner).
- Show the owner a preview (an off-game sheet like `tools/MawPreview.java`) before building the server side.

## Then

- The NPC's quest that unlocks the mutations, when the owner says (they will tell you; `BikakuDevour.grant(player, mutation, true)` is ready).
- The Walker with GeckoLib only if clearly better (the owner: "feel free to improve the walker if you think it's viable and a better choice
  using geckolib"); the judgment told them: its feet are placed on blocks by code each tick, so GeckoLib would add little. Ask first.

## Open questions for the owner

- Is Grand Devouring's reshaped look right (`docs/handoff/bikaku-devour-3.6.5.png`)? Sent; no answer.
- The Walker: "two separate appendages on either": two in all (built) or two each side? Asked; no answer.
- With the ghoul diet switched off (a server setting) nothing shows energy on screen since 3.6.6: told them; no answer.

## House rules (short; full text in `next-ai-1.3.60.md` and `next-ai-3.5.4.md`)

- One version per fix or feature (the last number never reaches 10): both version strings, release notes, a README entry, a new top
  section in `companions-handoff.md`, the `next-ai-2.0.45.md` header and log line, `BIKAKU_PATH.md`, a full `python3 build.py` ending with
  **EXIT 0** (about 8 minutes; run it in the background; don't edit sources while it runs), a commit `3.x.y: ...` with the trailers
  `Co-Authored-By: Claude <noreply@anthropic.com>` and the session's `Claude-Session:` line (no model names anywhere pushed), then push.
- Quick loop: `python3 tools/quick_check.py [--noverify] SuiteName ...`.
- **Nothing is tested in game. Never claim it.**
- Answer in English, plainly. Quote the owner's words exactly; add nothing they didn't say. Bug reports first, each its own version; if
  their exact words for a report aren't in the handoff, ask. Show previews before going far. When a version is done, say what was made,
  what was guessed, and what's next.
- Banned: in the jar Kaneki, Ayato, Furuta; in visible text Takatsuki, Aogiri, Yoshimura and no character names (keep "Noro" out).
- Sounds: only the hexkagune pack's recordings by their `sounds.json` names. Never make a sound. Never alter the owner's files.
