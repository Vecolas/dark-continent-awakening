package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.bestiary.BestiaryRegistry;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Descobertas validadas no servidor; observar não depende de carregar o chunk. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class BestiaryDiscoveryService {
    private static final int DISTANCIA_DE_OBSERVACAO = 24;
    private static final int TICKS_DE_OBSERVACAO = 20;
    private static final Map<UUID, Map<Integer, Integer>> OBSERVACOES = new HashMap<>();

    private BestiaryDiscoveryService() { }

    @SubscribeEvent
    public static void aoTickarJogador(PlayerTickEvent.Post evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jogador) || jogador.tickCount % 2 != 0) return;
        var contador = OBSERVACOES.computeIfAbsent(jogador.getUUID(), ignored -> new HashMap<>());
        var candidatos = jogador.serverLevel().getEntitiesOfClass(LivingEntity.class,
                jogador.getBoundingBox().inflate(DISTANCIA_DE_OBSERVACAO),
                entidade -> entidade.getType() == EnemyEntityTypes.FOXBEAR.get()
                        && jogador.hasLineOfSight(entidade)
                        && jogador.distanceToSqr(entidade) <= DISTANCIA_DE_OBSERVACAO * DISTANCIA_DE_OBSERVACAO);
        var idsVisiveis = new java.util.HashSet<Integer>();
        for (LivingEntity entidade : candidatos) {
            idsVisiveis.add(entidade.getId());
            int ticks = contador.merge(entidade.getId(), 2, Integer::sum);
            if (ticks >= TICKS_DE_OBSERVACAO) {
                BestiaryPlayerService.observar(jogador, BestiaryRegistry.FOXBEAR_ID,
                        jogador.serverLevel().getGameTime());
                contador.remove(entidade.getId());
            }
        }
        contador.keySet().removeIf(id -> !idsVisiveis.contains(id));
    }

    @SubscribeEvent
    public static void aoAtacar(LivingDamageEvent.Pre evento) {
        if (evento.getEntity().getType() != EnemyEntityTypes.FOXBEAR.get()) return;
        if (evento.getSource().getEntity() instanceof ServerPlayer jogador) {
            BestiaryPlayerService.lutar(jogador, BestiaryRegistry.FOXBEAR_ID);
        }
    }

    @SubscribeEvent
    public static void aoDerrotar(LivingDeathEvent evento) {
        if (evento.getEntity().getType() != EnemyEntityTypes.FOXBEAR.get()) return;
        if (evento.getSource().getEntity() instanceof ServerPlayer jogador) {
            BestiaryPlayerService.derrotar(jogador, BestiaryRegistry.FOXBEAR_ID);
        }
    }

    @SubscribeEvent
    public static void aoSair(PlayerEvent.PlayerLoggedOutEvent evento) {
        OBSERVACOES.remove(evento.getEntity().getUUID());
    }
}
