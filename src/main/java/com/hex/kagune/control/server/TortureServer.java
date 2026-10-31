package com.hex.kagune.control.server;

import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CCutscene;
import com.hex.kagune.control.npc.EtoNpc;
import com.hex.kagune.control.npc.EtoTalk;
import com.hex.kagune.control.npc.Persona;
import com.hex.kagune.control.npc.Teaching;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.14: a lesson lived through ({@link Teaching.Kind#TRIAL}): Ken's ten days, the cutscene ({@code CutsceneOverlay.TORTURE}). Held
 * where they stand while it plays; at its end they are left with almost nothing (they went through it), knitting back fast, and the
 * gate opens. Logging out in the middle is giving up: nothing is taught, and it can be asked for again.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class TortureServer {
    /** How long it runs (game ticks): the cutscene's length. */
    public static final long LENGTH = 46L * 20L;

    private record Trial(long endsAt, String lesson, String teacher, String name) {
    }

    private static final Map<UUID, Trial> TRIALS = new HashMap<>();

    private TortureServer() {
    }

    /** {@code player} begins {@code lesson} with {@code npc}: the cutscene, held still through it. */
    static void begin(ServerPlayer player, EtoNpc npc, Teaching.Lesson lesson) {
        if (lesson == null || TRIALS.containsKey(player.m_20148_())) {
            return;
        }
        long now = player.m_9236_().m_46467_();
        EtoTalk.Context c = EtoServer.context(player, npc);
        String words = npc.persona().script().trialWords(lesson, c);
        TRIALS.put(player.m_20148_(), new Trial(now + LENGTH, lesson.id(), npc.persona().key(), npc.displayName()));
        ControlNetwork.sendTo(player, new S2CCutscene(S2CCutscene.TORTURE, words));
        int ticks = (int)LENGTH;
        player.m_7292_(new MobEffectInstance(MobEffects.f_19597_, ticks, 6, false, false, false));
        player.m_7292_(new MobEffectInstance(MobEffects.f_19603_, ticks, 128, false, false, false));
        player.m_7292_(new MobEffectInstance(MobEffects.f_19606_, ticks, 4, false, false, false));
    }

    /** True while {@code player} is going through it. */
    public static boolean enduring(ServerPlayer player) {
        return TRIALS.containsKey(player.m_20148_());
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Trial trial = TRIALS.get(player.m_20148_());
        if (trial == null || player.m_9236_().m_46467_() < trial.endsAt()) {
            return;
        }
        TRIALS.remove(player.m_20148_());
        Teaching.Lesson lesson = Teaching.byId(trial.lesson());
        if (lesson == null || !player.m_6084_()) {
            return;
        }
        // They went through it: almost nothing left of them, and it all grows back.
        player.m_21153_(Math.max(2.0F, player.m_21223_() * 0.15F));
        player.m_7292_(new MobEffectInstance(MobEffects.f_19605_, 400, 2, false, true, true));
        EtoServer.taught(player, Persona.of(trial.teacher()), trial.name(), lesson);
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        TRIALS.remove(event.getEntity().m_20148_());
    }
}
