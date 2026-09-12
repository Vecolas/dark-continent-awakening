package com.darkcontinent.nenfoundation.enemy.registry;

import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import com.darkcontinent.nenfoundation.enemy.entity.FrogInWaitingEntity;
import com.darkcontinent.nenfoundation.enemy.entity.GreatStampEntity;
import com.darkcontinent.nenfoundation.enemy.entity.ManFacedApeEntity;
import com.darkcontinent.nenfoundation.enemy.entity.SpiderEagleEntity;
import com.darkcontinent.nenfoundation.enemy.spawn.SpawnRule;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** Listeners de lifecycle mantidos fora do ponto de entrada do mod. */
public final class EnemyEntityEvents {
    private EnemyEntityEvents() { }

    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(EnemyEntityTypes.GREAT_STAMP.get(), GreatStampEntity.createAttributes().build());
        event.put(EnemyEntityTypes.FROG_IN_WAITING.get(), FrogInWaitingEntity.createAttributes().build());
        event.put(EnemyEntityTypes.MAN_FACED_APE.get(), ManFacedApeEntity.createAttributes().build());
        event.put(EnemyEntityTypes.SPIDER_EAGLE.get(), SpiderEagleEntity.createAttributes().build());
    }

    public static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(EnemyEntityTypes.GREAT_STAMP.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                noChaoComLuzDoPerfil(HunterExamProfiles.greatStamp().spawnRule()),
                RegisterSpawnPlacementsEvent.Operation.OR);

        // O sapo tambem nasce no chao: ele emboscada ENTERRADO, e nao ha "enterrar"
        // sem um bloco solido embaixo. A faixa de luz e a do perfil dele, que vai ate
        // 15 justamente porque exigir escuridao faria a emboscada nunca nascer.
        event.register(EnemyEntityTypes.FROG_IN_WAITING.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                noChaoComLuzDoPerfil(HunterExamProfiles.frogInWaiting().spawnRule()),
                RegisterSpawnPlacementsEvent.Operation.OR);

        // O macaco tambem nasce no chao, e com a MESMA forma de predicado: a faixa de
        // luz dele vai ate 15 porque ele se disfarca de gente, e ninguem e enganado no
        // escuro. Copiar o corpo do predicado aqui faria o terceiro lugar onde a mesma
        // regra pode divergir do perfil -- por isso ele e lido, nao repetido.
        event.register(EnemyEntityTypes.MAN_FACED_APE.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                noChaoComLuzDoPerfil(HunterExamProfiles.manFacedApe().spawnRule()),
                RegisterSpawnPlacementsEvent.Operation.OR);

        // A ave VOA, e mesmo assim nasce no chao: o ninho e o lugar dela, e ela nasce
        // POUSADA nele. Registrar um placement de ar aqui faria a ave aparecer
        // pairando sobre o canyon e ancorar o ninho no vazio -- e o ninho no ar nao
        // da erro nenhum, so faz a coleira medir a partir de um ponto que ninguem
        // consegue alcancar. Chao solido continua valendo, e a faixa de luz e a do
        // perfil, lida e nao repetida.
        event.register(EnemyEntityTypes.SPIDER_EAGLE.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                noChaoComLuzDoPerfil(HunterExamProfiles.spiderEagle().spawnRule()),
                RegisterSpawnPlacementsEvent.Operation.OR);
    }

    /**
     * Predicado unico de "chao solido, dentro da faixa de luz DO PERFIL, e nao
     * vindo de spawner".
     *
     * <p>Os limites de luz sao LIDOS do perfil, nao repetidos aqui. Antes o 10
     * do great stamp estava nos dois lugares; girar o numero na {@link SpawnRule}
     * nao mudava nada em jogo, e o botao morto so aparece depois de uma tarde de
     * balanceamento perdida. Com dois mobs usando a mesma forma, copiar o corpo
     * do predicado faria a mesma divergencia nascer de novo -- em dobro.</p>
     */
    private static <T extends Entity> SpawnPlacements.SpawnPredicate<T> noChaoComLuzDoPerfil(SpawnRule regra) {
        int luzMinima = regra.minLight();
        int luzMaxima = regra.maxLight();
        return (type, level, spawnType, pos, random) -> {
            int luz = level.getMaxLocalRawBrightness(pos);
            return spawnType != MobSpawnType.SPAWNER
                    && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                    && luz >= luzMinima && luz <= luzMaxima;
        };
    }
}
