package com.darkcontinent.nenfoundation.enemy.registry;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyHearingBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

/** Converte sons emitidos pelo jogo em sinais de audicao server-side. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class EnemyPerceptionEvents {
    private EnemyPerceptionEvents() { }

    @SubscribeEvent
    public static void somEmEntidade(PlayLevelSoundEvent.AtEntity event) {
        if (deveIgnorar(event.getSource())) return;
        double radius = Math.max(4.0D, Math.min(32.0D, event.getNewVolume() * 16.0D));
        EnemyHearingBus.emit(event.getEntity(), radius);
    }

    @SubscribeEvent
    public static void somEmPosicao(PlayLevelSoundEvent.AtPosition event) {
        if (deveIgnorar(event.getSource())) return;
        double radius = Math.max(4.0D, Math.min(32.0D, event.getNewVolume() * 16.0D));
        EnemyHearingBus.emit(event.getLevel(), event.getPosition(), null, radius);
    }

    private static boolean deveIgnorar(net.minecraft.sounds.SoundSource source) {
        return source == net.minecraft.sounds.SoundSource.MUSIC
                || source == net.minecraft.sounds.SoundSource.RECORDS
                || source == net.minecraft.sounds.SoundSource.WEATHER;
    }
}
