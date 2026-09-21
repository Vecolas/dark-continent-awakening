package com.darkcontinent.nenfoundation.enemy.registry;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandRegion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/** Última barreira server-side contra vazamento de criaturas e bosses. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class EnemySpawnSafetyEvents {
    private EnemySpawnSafetyEvents() { }

    @SubscribeEvent
    public static void validarLimites(FinalizeSpawnEvent evento) {
        Entity entidade = evento.getEntity();
        if (GreedIslandRegion.eCriatura(entidade.getType().builtInRegistryHolder().key().location())
                && !GreedIslandRegion.dentro(evento.getLevel().getLevel().dimension())) {
            evento.setSpawnCancelled(true);
            return;
        }

        // Profiles ENCOUNTER_ONLY não entram na lista natural. Esta segunda trava
        // existe para o caso de datapack/mod externo tentar adicioná-los por engano:
        // o encontro continua podendo usar COMMAND/STRUCTURE, mas o boss não nasce
        // solto em um mundo novo.
        if (entidade.getType() == EnemyEntityTypes.DUMMY_ENEMY.get()
                || entidade.getType() == EnemyEntityTypes.CYCLOPS.get()
                || entidade.getType() == EnemyEntityTypes.HYPER_PUFFBALL.get()
                || entidade.getType() == EnemyEntityTypes.MELANIN_LIZARD.get()
                || entidade.getType() == EnemyEntityTypes.RADIO_RAT.get()
                || entidade.getType() == EnemyEntityTypes.BUBBLE_HORSE.get()
                || entidade.getType() == EnemyEntityTypes.KING_WHITE_STAG_BEETLE.get()
                || entidade.getType() == EnemyEntityTypes.WOLF_PACK_HUNTER.get()
                || entidade.getType() == EnemyEntityTypes.CRAB_HEAVY.get()
                || entidade.getType() == EnemyEntityTypes.BAT_SCOUT.get()
                || entidade.getType() == EnemyEntityTypes.WOLF_RUNNER.get()
                || entidade.getType() == EnemyEntityTypes.SPIDER_WEBBER.get()
                || entidade.getType() == EnemyEntityTypes.MOSQUITO_OFFICER.get()
                || entidade.getType() == EnemyEntityTypes.MULTIARM_CENTIPEDE.get()
                || entidade.getType() == EnemyEntityTypes.CHEETAH_LEADER.get()
                || entidade.getType() == EnemyEntityTypes.SCORPION_LEADER.get()
                || entidade.getType() == EnemyEntityTypes.AVIAN_COMMANDER.get()) {
            if (evento.getSpawnType() == MobSpawnType.NATURAL
                    || evento.getSpawnType() == MobSpawnType.CHUNK_GENERATION) {
                evento.setSpawnCancelled(true);
            }
        }
    }
}
