package com.darkcontinent.nenfoundation.enemy.registry;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.FrogInWaitingEntity;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;

/**
 * Listeners de JOGO da foundation de inimigos (o barramento do NeoForge, nao o
 * do mod). Registra-se sozinho pela anotacao, para o ponto de entrada do mod
 * continuar apenas registrando subsistemas.
 *
 * <p>DECISAO QUE ESTE ARQUIVO CARREGA: quem esta preso na boca do
 * {@link FrogInWaitingEntity} NAO sai apertando shift. A saida legitima e por
 * DANO ou por TEMPO, e quem decide isso e o servidor, uma vez so, em
 * {@code GrabRules.solta}. Sem esta recusa a janela de escape que o mob inteiro
 * existe para criar some: bastaria agachar, e o agarrao viraria enfeite.</p>
 *
 * <p>A recusa vale SO no servidor. O cliente que se desmontar sozinho e
 * corrigido pelo pacote de veiculo no tick seguinte; recusar dos dois lados so
 * acrescentaria uma segunda fonte de verdade para a mesma decisao.</p>
 *
 * <p>PONTO CEGO DECLARADO: qualquer desmontagem forcada de fora (um
 * {@code /tp} no jogador agarrado, por exemplo) tambem e recusada enquanto o
 * agarrao durar. O prejuizo esta limitado aos 100 ticks do agarrao, e o preco
 * de nao recusar seria pior -- um jogador solto de graca por qualquer efeito
 * que mexa em veiculo.</p>
 */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class EnemyGameEvents {
    private EnemyGameEvents() { }

    @SubscribeEvent
    public static void desmontarSoQuandoOSapoSolta(EntityMountEvent event) {
        if (event.isMounting() || event.getLevel().isClientSide) return;
        Entity veiculo = event.getEntityBeingMounted();
        if (!(veiculo instanceof FrogInWaitingEntity sapo)) return;
        // Nao basta "o sapo esta agarrando": tem de estar agarrando ESTA entidade.
        // Um segundo passageiro qualquer nao deve herdar a prisao da vitima.
        if (sapo.estaAgarrando() && sapo.hasPassenger(event.getEntityMounting())) {
            event.setCanceled(true);
        }
    }
}
