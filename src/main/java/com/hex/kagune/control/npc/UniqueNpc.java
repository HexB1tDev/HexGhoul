package com.hex.kagune.control.npc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 1.3.2: a one-of-a-kind NPC as its world remembers it (pure, tested): there is only ever one of each (Eto...), whose body this is
 * ({@link #body}), whether she lives ({@link #alive}, and when she was last seen ticking: one gone quiet for days can be replaced),
 * when she died (she comes back to her home some days later: ghouls heal), where her home is, and her partner: one player for good,
 * dating or married ({@link Romance.Stage}), by id and by name (for the line over her head). Kept with the world ({@code UniqueNpcs}).
 */
public final class UniqueNpc {
    /** Dead this long (game ticks: 1.3.3, a day), she comes back to her home once a player is near it. */
    public static final long COMES_BACK = 24000L;
    /** 1.3.3: away from home (waiting somewhere, following, lost) with no one about this long (a day), she goes back home. */
    public static final long GOES_HOME = 24000L;
    /** Unseen this long (game ticks: five days) while thought alive, a new body of hers may take her place. */
    public static final long GONE_QUIET = 120000L;

    public UUID body;
    public boolean alive;
    public long seen = Long.MIN_VALUE;
    public long diedAt = Long.MIN_VALUE;
    public int homeX;
    public int homeY;
    public int homeZ;
    public boolean homed;
    public UUID partner;
    public String partnerName = "";
    public Romance.Stage stage = Romance.Stage.NONE;
    /** 1.3.3: where she belongs (made there, back there after she dies or is left too long), whether she is away from it, and when she last had company. */
    public int originX;
    public int originY;
    public int originZ;
    public boolean origined;
    public boolean away;
    public long company = Long.MIN_VALUE;

    /** Whether {@code body}, ticking at {@code now}, may live: hers, or none of hers lives (or hers has gone quiet), and it takes her place. */
    public boolean claim(UUID body, long now) {
        if (body.equals(this.body)) {
            this.alive = true;
            this.seen = now;
            return true;
        }
        boolean taken = this.body != null && this.alive && (this.seen == Long.MIN_VALUE || now - this.seen < GONE_QUIET);
        if (taken) {
            return false;
        }
        this.body = body;
        this.alive = true;
        this.seen = now;
        this.diedAt = Long.MIN_VALUE;
        return true;
    }

    /** She died at {@code now}. */
    public void died(UUID body, long now) {
        if (body.equals(this.body)) {
            this.alive = false;
            this.diedAt = now;
        }
    }

    /** True if she is dead and has been long enough to come back. */
    public boolean dueBack(long now) {
        return !this.alive && (this.origined || this.homed) && this.diedAt != Long.MIN_VALUE && now - this.diedAt >= COMES_BACK;
    }

    /** 1.3.3: true if she is alive, away from home, and has had no one about for {@link #GOES_HOME}. */
    public boolean dueHome(long now) {
        return this.alive && this.away && this.origined && this.company != Long.MIN_VALUE && now - this.company >= GOES_HOME;
    }

    /** Where she comes back to: her origin (her home before 1.3.3 kept it). */
    public int[] spawn() {
        return this.origined ? new int[] {this.originX, this.originY, this.originZ} : new int[] {this.homeX, this.homeY, this.homeZ};
    }

    /** The partner as the line over her head reads. */
    public String tag() {
        return Romance.tag(this.stage, this.partnerName);
    }

    /** {@code player}'s stage with her (none for anyone but her partner). */
    public Romance.Stage stageOf(UUID player) {
        return player != null && player.equals(this.partner) ? this.stage : Romance.Stage.NONE;
    }

    /** True if she has a partner and it is not {@code player}. */
    public boolean claimedFrom(UUID player) {
        return this.partner != null && this.stage.together() && !this.partner.equals(player);
    }

    public void pair(UUID player, String name, Romance.Stage stage) {
        this.partner = stage.together() ? player : null;
        this.partnerName = stage.together() && name != null ? name : "";
        this.stage = stage.together() ? stage : Romance.Stage.NONE;
    }

    // ---------------------------------------------------------------- saved with the world: one line each

    String save(String key) {
        return String.join("|", key, this.body == null ? "-" : this.body.toString(), this.alive ? "1" : "0", Long.toString(this.seen),
            Long.toString(this.diedAt), this.homed ? this.homeX + "," + this.homeY + "," + this.homeZ : "-",
            this.partner == null ? "-" : this.partner.toString(), this.partnerName.isEmpty() ? "-" : this.partnerName, Integer.toString(this.stage.ordinal()),
            this.origined ? this.originX + "," + this.originY + "," + this.originZ : "-", this.away ? "1" : "0", Long.toString(this.company));
    }

    /** Every NPC as one string, a line each. */
    public static String saveAll(Map<String, UniqueNpc> all) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, UniqueNpc> entry : all.entrySet()) {
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(entry.getValue().save(entry.getKey()));
        }
        return out.toString();
    }

    /** Read back ({@link #saveAll}); a line that cannot be read is dropped. */
    public static Map<String, UniqueNpc> loadAll(String saved) {
        Map<String, UniqueNpc> all = new LinkedHashMap<>();
        if (saved == null || saved.isEmpty()) {
            return all;
        }
        for (String line : saved.split("\n")) {
            String[] parts = line.split("\\|", -1);
            if (parts.length != 9 && parts.length != 12 || parts[0].isEmpty()) {
                continue;
            }
            try {
                UniqueNpc npc = new UniqueNpc();
                npc.body = parts[1].equals("-") ? null : UUID.fromString(parts[1]);
                npc.alive = parts[2].equals("1");
                npc.seen = Long.parseLong(parts[3]);
                npc.diedAt = Long.parseLong(parts[4]);
                if (!parts[5].equals("-")) {
                    String[] home = parts[5].split(",");
                    npc.homeX = Integer.parseInt(home[0]);
                    npc.homeY = Integer.parseInt(home[1]);
                    npc.homeZ = Integer.parseInt(home[2]);
                    npc.homed = true;
                }
                npc.partner = parts[6].equals("-") ? null : UUID.fromString(parts[6]);
                npc.partnerName = parts[7].equals("-") ? "" : parts[7];
                npc.stage = Romance.Stage.byOrdinal(Integer.parseInt(parts[8]));
                if (npc.partner == null) {
                    npc.stage = Romance.Stage.NONE;
                }
                if (parts.length == 12) {
                    if (!parts[9].equals("-")) {
                        String[] origin = parts[9].split(",");
                        npc.originX = Integer.parseInt(origin[0]);
                        npc.originY = Integer.parseInt(origin[1]);
                        npc.originZ = Integer.parseInt(origin[2]);
                        npc.origined = true;
                    }
                    npc.away = parts[10].equals("1");
                    npc.company = Long.parseLong(parts[11]);
                }
                all.put(parts[0], npc);
            } catch (RuntimeException ignored) {
                // dropped
            }
        }
        return all;
    }
}
