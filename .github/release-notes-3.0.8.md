## 3.0.8 (HexGhoul): Jealousy preserves existing marriages

HexKagune 3.0.8 for Forge 1.20.1. Not tested in game.

The marry command used to clear the previous spouse's world relationship and DATING flag. That forced divorce is removed; the existing jealousy timer/mood still runs for other partners. Jealousy costs no affection, including repeated kisses or dates with another partner, so it cannot turn a spouse into an enemy or remove her romance menu.

EtoServer.open migrates relationships before hostility checks. A current partner's low historical regard no longer makes the talk treat them as a stranger/enemy. For the old command's damaged state (unclaimed slot, missing dating flag, surviving per-player wedding date), UniqueNpc.recoverWedding restores MARRIED and the dating flag when spoken to. It keeps courtship, lessons, home, children and wedding date. Claimed companions are never taken by recovery; the existing partner-killed callback deletes the wedding date, so that ended relationship is not recovered. Normal courtship/proposal prerequisites and the operator command's permissions remain as before.

Regression checks cover repeated jealousy without affection/flag/courtship loss, both marriages surviving save/reload, every companion's romance menu while jealous even with historical low regard, and recovery refusing claimed partners, ordinary legacy dating and missing/deleted wedding records. No new Minecraft API or network values.

Validation: full python3 build.py completed with EXIT 0 on Java 17, including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 88 unchanged. Not tested in game.
