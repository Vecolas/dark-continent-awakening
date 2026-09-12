package com.darkcontinent.nenfoundation.enemy.registry;

import com.darkcontinent.nenfoundation.enemy.entity.GreatStampEntity;
import net.minecraft.world.entity.MobSpawnType;
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
        event.register(EnemyEntityTypes.GREAT_STAMP.get(),
                (type, level, spawnType, pos, random) -> spawnType != MobSpawnType.SPAWNER
                        && level.getBlockState(pos.below()).isSolid()
                        && level.getMaxLocalRawBrightness(pos) <= 10,
                RegisterSpawnPlacementsEvent.Operation.OR);
    }
}
