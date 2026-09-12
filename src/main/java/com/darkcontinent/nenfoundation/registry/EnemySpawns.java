package com.darkcontinent.nenfoundation.registry;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** Regra de spawn natural do Foxbear, separada do registro do tipo. */
public final class EnemySpawns {
    private EnemySpawns() { }
    public static void registrar(RegisterSpawnPlacementsEvent evento) {
        evento.register(EnemyEntityTypes.FOXBEAR.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
