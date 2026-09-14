package com.darkcontinent.nenfoundation.enemy.greedisland;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.encounter.EncounterInstance;
import com.darkcontinent.nenfoundation.enemy.encounter.EncounterServerHooks;
import com.darkcontinent.nenfoundation.enemy.content.GreedIslandProfiles;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Registra no episodio como cada criatura de GI saiu de cena. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class GreedIslandCaptureHooks {
    private GreedIslandCaptureHooks() { }

    @SubscribeEvent
    public static void aoMorrer(LivingDeathEvent evento) {
        if (!(evento.getEntity() instanceof LivingEntity criatura)
                || !(evento.getSource().getEntity() instanceof ServerPlayer jogador)) return;
        var tipo = BuiltInRegistries.ENTITY_TYPE.getKey(criatura.getType());
        var condicao = GreedIslandProfiles.capturas().get(tipo.getPath());
        if (condicao == null || condicao.exigeNaoLetal()) return;
        registrar(criatura, jogador, DefeatResult.CAPTURADO);
    }

    @SubscribeEvent
    public static void aoCapturar(PlayerInteractEvent.EntityInteract evento) {
        if (evento.getLevel().isClientSide()
                || !(evento.getEntity() instanceof ServerPlayer jogador)
                || !(evento.getTarget() instanceof LivingEntity criatura)
                || !dentroDaIlha(criatura)) return;

        var tipo = BuiltInRegistries.ENTITY_TYPE.getKey(criatura.getType());
        var condicao = GreedIslandProfiles.capturas().get(tipo.getPath());
        if (condicao == null || !condicao.exigeNaoLetal()
                || criatura.getHealth() > criatura.getMaxHealth() * condicao.vidaMaximaFracao()) return;
        if (criatura instanceof com.darkcontinent.nenfoundation.enemy.entity.BubbleHorseEntity cavalo
                && !cavalo.exausto()) return;

        Optional<EncounterInstance> encontro = encontroDe(criatura);
        if (encontro.isEmpty() || encontro.get().estado().terminou()) return;
        if (!encontro.get().registrarDesfecho(criatura.getUUID(), DefeatResult.CAPTURADO,
                jogador.getUUID())) return;
        encontro.get().entrar(jogador.getUUID());
        var controlador = EncounterServerHooks.controlador().orElseThrow();
        controlador.dados().sujar();
        evento.setCanceled(true);
        evento.setCancellationResult(InteractionResult.SUCCESS);
        criatura.discard();
        jogador.sendSystemMessage(Component.literal("Captura registrada. O card será entregue ao concluir o encontro."));
    }

    private static boolean dentroDaIlha(Entity entidade) {
        return GreedIslandRegion.dentro(entidade.level().dimension());
    }

    private static void registrar(Entity criatura, ServerPlayer jogador, DefeatResult resultado) {
        if (!dentroDaIlha(criatura)) return;
        encontroDe(criatura).ifPresent(encontro -> {
            if (encontro.registrarDesfecho(criatura.getUUID(), resultado, jogador.getUUID())) {
                EncounterServerHooks.controlador().ifPresent(c -> c.dados().sujar());
            }
        });
    }

    private static Optional<EncounterInstance> encontroDe(Entity criatura) {
        return EncounterServerHooks.controlador()
                .flatMap(controlador -> controlador.dados().instancias().values().stream()
                        .filter(encontro -> encontro.dimensao().equals(criatura.level().dimension()))
                        .filter(encontro -> encontro.entidades().contains(criatura.getUUID()))
                        .findFirst());
    }
}
