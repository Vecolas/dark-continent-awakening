package com.darkcontinent.nenfoundation.enemy.greedisland;

import com.darkcontinent.nenfoundation.NenFoundation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Morrer na ilha devolve o jogador A ILHA, e nao ao Overworld.
 *
 * <p><b>Por que isto precisa de codigo.</b> A ilha tem {@code bed_works} e
 * {@code respawn_anchor_works} em {@code false}, de proposito: cama e ancora
 * sao progresso do Overworld, e deixa-las funcionar la dentro daria uma saida
 * lateral para o ciclo de encontro. Mas sem nenhuma das duas, a vanilla manda o
 * morto para o spawn do Overworld -- e uma ilha da qual se sai MORRENDO nao e
 * uma ilha, e um corredor.</p>
 *
 * <p><b>O DADO VIAJA NO JOGADOR, e nao num mapa estatico.</b> Um
 * {@code Map<UUID, Boolean>} aqui pareceria mais simples e perderia o caso que
 * mais acontece em servidor de verdade: morrer, fechar o jogo na tela de morte
 * e voltar depois. O mapa some com o reinicio; a tag persistente viaja no disco
 * junto do jogador.</p>
 *
 * <p><b>A COPIA E EXPLICITA.</b> {@code getPersistentData()} NAO e copiado pela
 * vanilla na morte -- o jogador novo nasce com a tag vazia. Por isso
 * {@link #aoClonar} copia a marca do corpo antigo para o novo. Sem essa linha o
 * recurso funcionaria em toda troca de dimensao e falharia exatamente no unico
 * caso que ele existe para cobrir.</p>
 *
 * <p><b>E ela e LIMPA ao usar.</b> Uma marca que sobrevivesse ao renascimento
 * mandaria o jogador para a ilha na proxima morte em qualquer lugar do mundo --
 * e o sintoma seria alguem morrendo no Overworld e acordando numa ilha que nao
 * visitava ha uma semana, sem nada no log.</p>
 */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class GreedIslandRespawnHooks {

    /** Chave da marca. Namespaced: {@code getPersistentData()} e compartilhado com todo mod. */
    static final String TAG_MORREU_NA_ILHA = NenFoundation.MOD_ID + ":morreu_na_ilha";

    private GreedIslandRespawnHooks() { }

    /**
     * Marca o corpo NOVO quando o antigo morreu na ilha.
     *
     * <p>Roda so com {@code wasDeath}: {@code Clone} tambem dispara quando o
     * jogador volta do End, e marcar ali daria um teleporte para a ilha a quem
     * nunca esteve nela.</p>
     */
    @SubscribeEvent
    public static void aoClonar(PlayerEvent.Clone evento) {
        if (!evento.isWasDeath()
                || !(evento.getOriginal() instanceof ServerPlayer antigo)
                || !(evento.getEntity() instanceof ServerPlayer novo)) {
            return;
        }
        if (!GreedIslandRegion.dentro(antigo.level().dimension())) {
            return;
        }
        novo.getPersistentData().putBoolean(TAG_MORREU_NA_ILHA, true);
    }

    /**
     * Devolve a ilha a quem morreu nela -- uma vez, e so uma.
     *
     * <p>A marca sai ANTES do teleporte, e nao depois: se o envio falhar por a
     * dimensao nao existir (datapack desligado), a marca ja limpa impede que o
     * jogador fique preso tentando voltar a cada morte, para sempre.</p>
     */
    @SubscribeEvent
    public static void aoRenascer(PlayerEvent.PlayerRespawnEvent evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jogador)) {
            return;
        }
        CompoundTag dados = jogador.getPersistentData();
        if (!dados.getBoolean(TAG_MORREU_NA_ILHA)) {
            return;
        }
        dados.remove(TAG_MORREU_NA_ILHA);
        GreedIslandTravel.enviar(jogador);
    }
}
