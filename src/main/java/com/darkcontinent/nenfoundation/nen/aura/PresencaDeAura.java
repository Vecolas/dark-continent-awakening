package com.darkcontinent.nenfoundation.nen.aura;

import com.darkcontinent.nenfoundation.api.SinalDeAura;
import com.darkcontinent.nenfoundation.nen.technique.Gyo;
import com.darkcontinent.nenfoundation.nen.technique.Ken;
import com.darkcontinent.nenfoundation.nen.technique.Ko;
import com.darkcontinent.nenfoundation.nen.technique.PrecedenciaDeTecnicas;
import com.darkcontinent.nenfoundation.nen.technique.Ren;
import com.darkcontinent.nenfoundation.nen.technique.Shu;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import com.darkcontinent.nenfoundation.nen.technique.Zetsu;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * O que os outros percebem de quem esta usando Nen.
 *
 * <p>ESTA CLASSE SUBSTITUI UMA DECISAO QUE ESTAVA NO CLIENTE. Existia uma
 * {@code AuraVisibilityPolicy} em {@code client.vfx} com a assinatura
 * {@code podeRenderizar(observadorDesperto, alvoEmZetsu, alvoUsaIn,
 * observadorUsaGyo)} -- e ela era a propria fuga que tentava impedir: para o
 * cliente decidir nao desenhar alguem em Zetsu, o servidor teria de contar ao
 * cliente que a pessoa esta em Zetsu.
 *
 * <p>Funciona perfeitamente com cliente honesto, e por isso nenhum playtest
 * encontra. E o erro numero 6 da lista do CLAUDE.md.
 *
 * <p>AQUI A PERGUNTA E OUTRA: nao e "o cliente pode mostrar?", e sim "o que o
 * servidor conta?". Quem esta em Zetsu vira {@link SinalDeAura#NENHUM}, igual a
 * quem nunca despertou, e o segredo nao atravessa a rede.
 *
 * <p>FUNCAO PURA, sem {@code ServerPlayer} e sem registro: da para provar a
 * tabela inteira sem subir o jogo.
 */
public final class PresencaDeAura {

    private PresencaDeAura() {
    }

    /**
     * O sinal que as outras pessoas percebem.
     *
     * <p>A ORDEM DAS PERGUNTAS E A REGRA. Zetsu vem primeiro e vence tudo: ele
     * existe para sumir do radar, e um Zetsu que perdesse para Ten anunciaria
     * justamente quem esta tentando se esconder. Depois Ren, que e o estado
     * mais alto e o que o canone descreve como sentido de longe. Ten por
     * ultimo.
     *
     * @param desperto  se a pessoa ja despertou o Nen
     * @param ativas    tecnicas ligadas, do lado do servidor
     */
    public static SinalDeAura percebida(boolean desperto, Set<ResourceLocation> ativas) {
        if (!desperto || ativas == null || ativas.isEmpty()) {
            return SinalDeAura.NENHUM;
        }
        if (ativas.contains(Zetsu.ID)) {
            return SinalDeAura.NENHUM;
        }
        // A ORDEM VEM DO DOMINIO, e nao de uma escada de `if` propria daqui.
        // Ate 2026-09-25 este metodo tinha a propria lista, e ela conhecia
        // quatro das sete: Gyo, Shu e Ko caiam no NENHUM final. Como as tres so
        // excluem Zetsu (e Ko exclui Gyo), elas podem estar ligadas SOZINHAS --
        // e o resultado era um jogador em Ko, com quase toda a aura num membro,
        // sendo percebido como quem nunca despertou.
        //
        // KEN ANTES DE REN, como esta escrito la: os dois se excluem hoje,
        // entao a disputa nao acontece em jogo, e por isso a ordem precisa
        // existir antes da primeira tecnica que combine com os dois.
        //
        // A BUSCA PULA QUEM NAO TEM SINAL, em vez de mapear so a dominante e
        // desistir. A diferenca foi achada alimentando o portao: com Ko fora da
        // tabela, o conjunto {Ten, Ko} ficava INVISIVEL -- a precedencia
        // escolhia Ko, Ko nao tinha sinal, e o Ten embaixo era ignorado. Uma
        // tecnica esquecida deve custar a propria leitura, e nunca a das outras.
        for (ResourceLocation id : PrecedenciaDeTecnicas.ordem()) {
            if (!ativas.contains(id)) {
                continue;
            }
            SinalDeAura sinal = sinalDe(id);
            if (sinal != SinalDeAura.NENHUM) {
                return sinal;
            }
        }
        // TECNICA DESCONHECIDA NAO ANUNCIA NADA. Um datapack pode registrar
        // tecnica que este codigo nunca viu, e o padrao seguro e o silencio:
        // inventar presenca para ela contaria ao mundo algo que ninguem
        // desenhou.
        return SinalDeAura.NENHUM;
    }

    /**
     * O sinal que cada tecnica produz, e por que o codominio e pobre.
     *
     * <p>{@link SinalDeAura} tem quatro valores porque ele ATRAVESSA A REDE, e
     * cada valor a mais e uma coisa a mais que o cliente do vizinho aprende
     * sobre voce. Sete tecnicas nao viram sete sinais: elas viram "nada", "aura
     * retida" ou "aura liberada", e essa perda e a mecanica de Zetsu e de In
     * funcionando.
     *
     * <p>KO CONTA COMO REN, e nao como um sinal proprio. Ko e aura liberada em
     * volume -- concentrada, mas liberada --, e quem esta perto sente o volume.
     * Dar a ele um valor proprio diria ao cliente do vizinho exatamente qual
     * membro esta carregado, que e informacao de combate que ninguem pediu para
     * dar de graca.
     *
     * <p>GYO E SHU CONTAM COMO TEN, pela razao oposta: as duas redistribuem uma
     * aura que continua retida junto ao corpo. Quem olha percebe que ha aura, e
     * nao ONDE ela esta -- descobrir isso e o que Gyo nos olhos existe para
     * fazer, e ele e a camada de percepcao (#126), nao este metodo.
     */
    private static SinalDeAura sinalDe(ResourceLocation id) {
        if (Zetsu.ID.equals(id)) {
            return SinalDeAura.NENHUM;
        }
        if (Ken.ID.equals(id)) {
            return SinalDeAura.KEN;
        }
        if (Ren.ID.equals(id) || Ko.ID.equals(id)) {
            return SinalDeAura.REN;
        }
        if (Ten.ID.equals(id) || Gyo.ID.equals(id) || Shu.ID.equals(id)) {
            return SinalDeAura.TEN;
        }
        return SinalDeAura.NENHUM;
    }
}
