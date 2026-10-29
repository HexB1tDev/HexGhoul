package com.hex.kagune.control.awakening;

import java.util.ArrayList;
import java.util.List;

/** Temporary-destruction bookkeeping: every hole remembers its original block once and closes on time. */
public final class ScarLedgerTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        ScarLedger<String> ledger = new ScarLedger<>();
        check(ledger.record(1L, "stone", 300L), "first destruction recorded");
        check(!ledger.record(1L, "air", 50L), "a second hit never overwrites the original block");
        check(ledger.record(2L, "dirt", 100L), "second position");
        check(ledger.record(3L, "grass", 200L), "third position");
        check(ledger.poll(99L) == null, "nothing restores early");
        ScarLedger.Scar<String> first = ledger.poll(100L);
        check(first != null && first.position == 2L && first.original.equals("dirt"), "earliest restore first");
        ledger.defer(first, 140L, true);
        check(first.retries == 1, "occupied holes count retries");
        check(ledger.poll(120L) == null, "deferred restore waits");
        check(ledger.poll(140L) == first, "deferred restore returns");
        ledger.finish(first);
        check(!ledger.contains(2L) && ledger.size() == 2, "finished restores leave the ledger");

        ledger.hasten(150L, 160L);
        List<Long> order = new ArrayList<>();
        ScarLedger.Scar<String> scar;
        while ((scar = ledger.poll(160L)) != null) {
            check(scar.original.equals(scar.position == 1L ? "stone" : "grass"), "original block kept");
            order.add(scar.position);
            ledger.finish(scar);
        }
        check(order.size() == 2 && ledger.size() == 0, "hasten brings every restore inside the window");

        ScarLedger<Integer> full = new ScarLedger<>();
        for (int i = 0; i < ScarLedger.MAX_SCARS; i++) check(full.record(i, i, 10L), "within capacity");
        check(full.full() && !full.record(-1L, 0, 10L), "destruction stops at the cap instead of growing without bound");
        System.out.println("Verified: one original per hole, ordered and deferred restores, hastened repair, bounded ledger.");
    }
}
