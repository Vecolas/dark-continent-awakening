package com.darkcontinent.nenfoundation.enemy.registry;

import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import com.darkcontinent.nenfoundation.enemy.entity.GreatStampEntity;
import com.darkcontinent.nenfoundation.enemy.spawn.SpawnRule;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** Listeners de lifecycle mantidos fora do ponto de entrada do mod. */
public final class EnemyEntityEvents {
    private EnemyEntityEvents() { }

    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(EnemyEntityTypes.GREAT_STAMP.get(), GreatStampEntity.createAttributes().build());
    }

    public static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        // Os limites de luz sao LIDOS do perfil, nao repetidos aqui. Antes o 10 estava
        // nos dois lugares; girar o numero na SpawnRule nao mudava nada em jogo, e o
        // botao morto so aparece depois de uma tarde de balanceamento perdida.
        SpawnRule regra = HunterExamProfiles.greatStamp().spawnRule();
        int luzMinima = regra.minLight();
        int luzMaxima = regra.maxLight();
        event.register(EnemyEntityTypes.GREAT_STAMP.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) -> {
                    int luz = level.getMaxLocalRawBrightness(pos);
                    return spawnType != MobSpawnType.SPAWNER
                            && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                            && luz >= luzMinima && luz <= luzMaxima;
                },
                RegisterSpawnPlacementsEvent.Operation.OR);
    }
}
