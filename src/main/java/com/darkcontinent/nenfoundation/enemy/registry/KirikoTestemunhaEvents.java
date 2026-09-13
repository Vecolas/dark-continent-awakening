package com.darkcontinent.nenfoundation.enemy.registry;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.KirikoEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * O OLHO do kiriko: quem feriu quem, na hora exata em que aconteceu.
 *
 * <p>DECISAO QUE ESTE ARQUIVO CARREGA: "ferir um inocente na frente do kiriko" e
 * um EVENTO, e nao uma varredura. O barramento de jogo ja sabe o instante em que
 * alguem toma dano; o kiriko so precisa ser avisado. A alternativa -- cada
 * kiriko varrer a vizinhanca todo tick atras de bichos machucados -- pagaria uma
 * busca por tick para descobrir uma coisa que acontece uma vez a cada muitos
 * segundos, e ainda assim erraria: dano curado entre dois ticks nao deixa rastro
 * nenhum para a varredura achar, e o jogador que matou uma vaca de um golpe
 * passaria no exame.</p>
 *
 * <p>O CUSTO, declarado: UMA busca por caixa por evento de dano cuja fonte e um
 * JOGADOR -- e ela procura so por {@code KirikoEntity}, que o indice de secao do
 * mundo resolve direto. Zero custo por tick, zero custo quando nao ha kiriko
 * carregado, zero custo quando quem bateu nao foi gente.</p>
 *
 * <p>ELE NAO DECIDE NADA. O raio, a nocao de "inocente" e a pontuacao moram no
 * mob e na regra pura; este arquivo so entrega a notificacao. Repetir aqui o
 * raio de julgamento criaria dois numeros para a mesma distancia, e a
 * divergencia apareceria como um kiriko que as vezes ve e as vezes nao.</p>
 *
 * <p>ARQUIVO PROPRIO, e nao uma linha em {@code EnemyGameEvents}: aquele existe
 * para a recusa de desmontagem do frog-in-waiting, e a anotacao
 * {@link EventBusSubscriber} registra por CLASSE. Duas decisoes de mobs
 * diferentes no mesmo arquivo viram um lugar que duas frentes editam ao mesmo
 * tempo, que e exatamente o conflito que a fronteira de arquivos existe para
 * evitar.</p>
 *
 * <p>SO NO SERVIDOR: o evento tambem chega no cliente em alguns caminhos, e o
 * julgamento e do servidor. {@link KirikoEntity#testemunharAgressao} recusa o
 * lado do cliente por conta propria -- a guarda esta nos dois lugares de
 * proposito, porque este listener e publico e o proximo chamador pode nao
 * lembrar.</p>
 */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class KirikoTestemunhaEvents {
    private KirikoTestemunhaEvents() { }

    /**
     * NO EVENTO DE ENTRADA, e antes de qualquer reducao: o que interessa ao
     * kiriko e que o golpe foi DADO, e nao quanto dele passou pela armadura da
     * vitima. Um jogador que ataca uma vaca com a mao vazia e tao agressivo
     * quanto um que ataca com espada.
     *
     * <p>O evento NAO e cancelado nem alterado. Este listener so observa --
     * mexer no dano aqui seria o segundo handler que o CLAUDE.md ja avisa que
     * este projeto comete, e o numero final ficaria plausivel demais para alguem
     * notar sem medir.</p>
     */
    @SubscribeEvent
    public static void aoFerirAlguem(LivingIncomingDamageEvent evento) {
        if (evento.getEntity().level().isClientSide) {
            return;
        }
        if (!(evento.getSource().getEntity() instanceof Player jogador)) {
            return;
        }
        KirikoEntity.testemunharAgressao(jogador, evento.getEntity());
    }
}
