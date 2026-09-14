package com.darkcontinent.nenfoundation.enemy.debug;

import com.darkcontinent.nenfoundation.enemy.api.HxHEnemy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Arranjo determinístico para testes manuais; não é conteúdo de encontro. */
public final class EnemyDebugArena {
    private EnemyDebugArena() { }

    public static int spawn(ServerLevel level, Vec3 origin) {
        int spawned = 0;
        int slot = 0;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (slot >= 8) break;
            var key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (key == null || !com.darkcontinent.nenfoundation.NenFoundation.MOD_ID.equals(key.getNamespace())) {
                continue;
            }
            var entity = type.create(level);
            if (!(entity instanceof Mob mob) || !(entity instanceof HxHEnemy)) continue;
            mob.moveTo(origin.x + (slot % 4) * 3.0D - 4.5D, origin.y,
                    origin.z + (slot / 4) * 4.0D + 3.0D, 0.0F, 0.0F);
            if (level.addFreshEntity(mob)) {
                slot++;
                spawned++;
            }
        }
        return spawned;
    }
}
