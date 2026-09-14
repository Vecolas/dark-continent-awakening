package com.darkcontinent.nenfoundation.nen.aura;

import com.darkcontinent.nenfoundation.api.SinalDeAura;
import com.darkcontinent.nenfoundation.nen.technique.Ken;
import com.darkcontinent.nenfoundation.nen.technique.Ren;
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
        // KEN ANTES DE REN: os dois se excluem hoje, entao a disputa nao
        // acontece em jogo -- e por isso a ordem precisa estar escrita antes
        // da primeira tecnica que combine com os dois. Ken e o envelope maior,
        // e quem esta perto sente o maior.
        if (ativas.contains(Ken.ID)) {
            return SinalDeAura.KEN;
        }
        if (ativas.contains(Ren.ID)) {
            return SinalDeAura.REN;
        }
        if (ativas.contains(Ten.ID)) {
            return SinalDeAura.TEN;
        }
        // TECNICA DESCONHECIDA NAO ANUNCIA NADA. Um datapack pode registrar
        // tecnica que este codigo nunca viu, e o padrao seguro e o silencio:
        // inventar presenca para ela contaria ao mundo algo que ninguem
        // desenhou.
        return SinalDeAura.NENHUM;
    }
}
