package com.darkcontinent.nenfoundation.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import java.util.Map;
import java.util.Set;
import com.darkcontinent.nenfoundation.nen.technique.Ren;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import com.darkcontinent.nenfoundation.nen.technique.Zetsu;
import java.util.Optional;
import java.util.stream.Collectors;
import com.darkcontinent.nenfoundation.nen.divination.RecusaDaAdivinhacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da condicao de entrada do ritual.
 *
 * <p>POR QUE ESTE ARQUIVO EXISTE, e a resposta e desconfortavel:
 *
 * <p>O gametest da Water Divination ja afirmava que o teste "nao funciona sem
 * Nen desperto". Ao alimentar esse portao com o defeito -- apagar a condicao do
 * ritual --, ele <b>passou</b>. A razao e que o servico de categoria recusa
 * de novo, mais abaixo: sem despertar, {@code atribuirPorSorteio} devolve
 * NAO_DESPERTO e o perfil nao muda de qualquer jeito.
 *
 * <p>Isso e defesa em profundidade, e e bom -- mas significa que aquele
 * gametest mede a rede de baixo, e nao a condicao do ritual. A diferenca e
 * visivel para o jogador: com a condicao, ele le "voce ainda nao despertou o
 * Nen"; sem ela, a agua tambem nao reage, mas por um motivo que a mensagem nao
 * explica.
 *
 * <p>A condicao e uma funcao pura de perfil, entao ela pode ser medida
 * diretamente. E e aqui que <b>Ren entra no M4</b>: quando a exigencia mudar,
 * este arquivo e o primeiro a reprovar.
 */
class NenAguaDivinatoriaTest {

    private static PersistentNenData perfil(boolean desperto, NenCategory categoria) {
        return new PersistentNenData(
                PersistentNenData.SCHEMA_ATUAL, desperto, categoria, false,
                0.0D, 0.0D, 0.0D, Map.of(), Set.of(), Set.of(), Set.of());
    }

    @Test
    @DisplayName("quem nao despertou nao pode fazer o teste")
    void semNenNaoPodeFazer() {
        assertTrue(NenAguaDivinatoria.recusaPara(
                        PersistentNenData.NAO_DESPERTADO, Set.of(Ren.ID)).isPresent(),
                "Sem esta condicao o ritual segue adiante e so e barrado la"
                        + " embaixo, pelo servico de categoria. A agua tambem nao"
                        + " reage -- mas por um motivo que a mensagem na tela nao"
                        + " explica, e o relato de bug vira 'cliquei e nao"
                        + " aconteceu nada'.");
        assertTrue(NenAguaDivinatoria.recusaPara(
                        perfil(false, NenCategory.EMISSION), Set.of(Ren.ID)).isPresent(),
                "Um perfil com categoria mas sem despertar tambem nao pode: ter"
                        + " categoria nao e ter Nen.");
    }

    @Test
    @DisplayName("desperto e SEM Ren: a agua nao reage, e o motivo e proprio")
    void semRenARecusaTemMotivoProprio() {
        // AQUI MORREU UMA DIVIDA DECLARADA.
        //
        // Ate a issue #90 existia um teste chamado `aCondicaoAindaNaoExigeRen`,
        // que passava justamente porque a condicao NAO exigia Ren. Ele nao
        // media virtude nenhuma: fixava o buraco, para que ele reprovasse no dia
        // em que Ren chegasse. Ren chegou na #87, ele reprovou, e este teste e o
        // que ficou no lugar.
        var recusa = NenAguaDivinatoria.recusaPara(
                perfil(true, NenCategory.UNDETERMINED), Set.of());

        assertTrue(recusa.isPresent(),
                "Desperto e sem Ren, o ritual foi ACEITO. O cânone pede Ren sobre"
                        + " o copo, e sem isso o teste continua mais facil do que"
                        + " deveria ser.");
        assertEquals(RecusaDaAdivinhacao.SEM_REN, recusa.get(),
                "A recusa veio com o motivo errado. Mandar quem esta desperto"
                        + " 'despertar o Nen' e mandar a pessoa para o lugar"
                        + " errado -- ela ja fez isso.");
    }

    @Test
    @DisplayName("quem NAO despertou nao e mandado ativar Ren")
    void naoDespertoNaoOuveFaleDeRen() {
        // A ORDEM DAS PERGUNTAS E O ASSUNTO DESTE TESTE. Quem nao despertou
        // tambem nao tem Ren ativo -- as duas condicoes falham juntas, e se a
        // de Ren for perguntada primeiro a mensagem sai errada para todo
        // jogador novo. O erro nao daria excecao nenhuma: so mandaria a pessoa
        // procurar um botao que ela ainda nao tem.
        var recusa = NenAguaDivinatoria.recusaPara(
                perfil(false, NenCategory.UNDETERMINED), Set.of());

        assertEquals(Optional.of(RecusaDaAdivinhacao.NAO_DESPERTOU), recusa,
                "Quem nunca despertou ouviu falar de Ren. As duas condicoes"
                        + " falham juntas, e a ordem em que sao perguntadas decide"
                        + " qual mensagem o jogador le.");
    }

    @Test
    @DisplayName("desperto e COM Ren: o ritual acontece")
    void comRenORitualAcontece() {
        assertTrue(NenAguaDivinatoria.recusaPara(
                        perfil(true, NenCategory.UNDETERMINED), Set.of(Ren.ID)).isEmpty(),
                "Com Ren ativo o ritual devia rodar; quem despertou e ainda nao"
                        + " tem categoria e exatamente quem ele existe para atender.");
        assertTrue(NenAguaDivinatoria.recusaPara(
                        perfil(true, NenCategory.CONJURATION), Set.of(Ren.ID)).isEmpty(),
                "quem ja tem categoria pode refazer o teste; refazer e no-op,"
                        + " e nao recusa.");
    }

    @Test
    @DisplayName("Ten ou Zetsu ativos nao substituem Ren")
    void outraTecnicaNaoServe() {
        // Perguntar "tem ALGUMA tecnica ativa?" seria mais permissivo e passaria
        // nos outros testes deste arquivo sem falhar em nenhum.
        assertEquals(Optional.of(RecusaDaAdivinhacao.SEM_REN),
                NenAguaDivinatoria.recusaPara(
                        perfil(true, NenCategory.UNDETERMINED), Set.of(Ten.ID)),
                "Ten ativo abriu o ritual. E Ren sobre o copo, e nao aura"
                        + " qualquer.");
        assertEquals(Optional.of(RecusaDaAdivinhacao.SEM_REN),
                NenAguaDivinatoria.recusaPara(
                        perfil(true, NenCategory.UNDETERMINED), Set.of(Zetsu.ID)),
                "Zetsu abriu o ritual -- justamente o estado em que o jogador"
                        + " NAO libera aura nenhuma.");
    }

    @Test
    @DisplayName("cada recusa tem as suas duas chaves, e elas nao se repetem")
    void asChavesDeRecusaSaoDistintas() {
        // Duas recusas apontando para a mesma mensagem dariam ao jogador o mesmo
        // texto para dois problemas diferentes -- e o relato de bug voltaria a
        // ser "cliquei e nao aconteceu nada".
        var regras = java.util.Arrays.stream(RecusaDaAdivinhacao.values())
                .map(RecusaDaAdivinhacao::chaveDaRegra).collect(Collectors.toSet());
        var aguas = java.util.Arrays.stream(RecusaDaAdivinhacao.values())
                .map(RecusaDaAdivinhacao::chaveDaAgua).collect(Collectors.toSet());

        assertEquals(RecusaDaAdivinhacao.values().length, regras.size(),
                "duas recusas compartilham a mensagem de regra: " + regras);
        assertEquals(RecusaDaAdivinhacao.values().length, aguas.size(),
                "duas recusas compartilham a mensagem da agua: " + aguas);
    }
}
