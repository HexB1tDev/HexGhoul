## 2.1.7 (HexGhoul): the Reaper's dashes for Ukaku's final kakuja

HexKagune 2.1.7 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Ukaku's final kakuja gets the White Reaper's dashes: tap sprint twice to dash the way you're moving. You have 3 charges, the action bar shows "Dash 2/3" as one is spent, and one comes back every 5 seconds until you're at 3/3 again. No kakuja vanishes any more. Also, after each release, only the 5 newest releases and the 5 newest action runs are kept.

- **The dash**:
  - A fast, low burst like the Reaper's own: about 6 blocks over 4 ticks, no fall damage from it.
  - Direction: W, S, A, D and between; with no keys, the way you look.
  - Works with the wings out and with the Steel Blade.
  - Charges come back one at a time, each 5 seconds after the last.
  - With cooldowns off (creative, `/kagune cooldowns off`) they're always full.
- **Code**: new pure `ReaperDash` (the charges, the double tap, the direction) and `server/ReaperDashServer`. The client sends the way you're moving (`C2SUkakuAction.REAPER_DASH`, so the protocol goes to 81: install this version on both client and server).
- **No vanish**: breaking into a sprint in Ukaku's Steel Blade or Rinkaku's Full Kakuja no longer makes you invisible. Their right-click dash stays. The ability guide and the Rinkaku menu say so.
- **Releases and actions**: a new cleanup job in the release workflow runs after each release. It deletes every release but the newest 5 (their tags are kept) and every finished workflow run but the newest 5. A run still going is left for the next cleanup.
- **Tests**: `ControlTests.reaperDash` (three charges, one back every 5 seconds, the double tap, the direction surviving the packet). The Steel Blade's board shows the Reaper's dash and no vanish. Not tested in game.
