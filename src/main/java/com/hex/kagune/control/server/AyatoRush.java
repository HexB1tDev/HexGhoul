package com.hex.kagune.control.server;

import com.hex.kagune.control.ComboStyle;
import com.hex.kagune.control.ayato.AyatoRules;
import com.hex.kagune.data.KaguneProfile;
import com.hex.kagune.gameplay.KaguneServerController;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Rushing Death (Z in the first kakuja): he backflips high into the air and well back (about six blocks up and seven back),
 * and at the top of the jump all six tentacles snap forward and throw twelve needles at once, two each. A move of its own
 * through the combo system ({@link ComboStyle#AYATO_RUSHING_DEATH}), so its timing, the body's flip and the tentacles are in
 * step on every client; the needles leave at its impact tick ({@code AyatoCrystal.resolve}). The original's ghoul durability
 * already takes the fall damage while the kagune is out.
 */
final class AyatoRush {
    /** Game time each player may next use it. */
    private static final Map<UUID, Long> READY_AT = new HashMap<>();

    private AyatoRush() {
    }

    /** Z in the kakuja: starts Rushing Death if it may start now. Returns true if it started. */
    static boolean start(ServerPlayer player) {
        if (!AyatoServer.form(player).kakuja()) {
            return false;
        }
        if (KaguneProfile.mastery(player) < AyatoRules.RUSH_MASTERY) {
            AyatoServer.locked(player, "Rushing Death", AyatoRules.RUSH_MASTERY);
            return false;
        }
        long now = player.m_9236_().m_46467_();
        Long ready = READY_AT.get(player.m_20148_());
        if (ready != null && now < ready && now >= ready - AyatoRules.RUSH_COOLDOWN) {
            return false;
        }
        if (!KaguneCombat.startArt(player, ComboStyle.AYATO_RUSHING_DEATH)) {
            return false;
        }
        READY_AT.put(player.m_20148_(), now + Cooldowns.of(player, AyatoRules.RUSH_COOLDOWN));
        // Up and back, away from where he is looking.
        Vec3 back = AyatoFire.horizontal(player.m_20154_()).m_82490_(-AyatoRules.RUSH_BACK);
        AyatoFire.push(player, new Vec3(back.f_82479_, AyatoRules.RUSH_UP, back.f_82481_));
        AyatoFire.sound(player.m_284548_(), player.m_20182_(), "entity.ender_dragon.flap", 0.9F, KaguneServerController.vary(player, 0.9F, 0.05F));
        AyatoFire.sound(player.m_284548_(), player.m_20182_(), "entity.player.attack.sweep", 0.6F, KaguneServerController.vary(player, 0.6F, 0.05F));
        return true;
    }

    static void forget(ServerPlayer player) {
        READY_AT.remove(player.m_20148_());
    }
}
