package com.darkcontinent.nenfoundation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.nen.aura.SaldoSustentado;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A conferencia dos numeros que o jogo CARREGOU.
 *
 * <p>O portao que ja existia para o item 6 do ADR-010 le o padrao escrito no
 * codigo-fonte, por regex sobre {@code NenConfig.java}. Ele nunca viu o toml.
 * Este arquivo cobre a conta que passou a ser feita sobre o valor de verdade.
 */
class ConferenciaDeBalanceamentoTest {

    @Test
    @DisplayName("a conta usa a regeneracao TOTAL, e nao o ganho sobre a base")
    void aContaCertaEADoTotal() {
        // ESTE E O DEFEITO QUE ACONTECEU, com os numeros exatos que rodaram na
        // instancia de teste: regeneracao 1.0, multiplicador 2.0, custo 1.5.
        //
        // Pelo GANHO SOBRE A BASE a conta da 1.0 contra 1.5 -- parece caro, e
        // foi assim que o portao antigo aprovou. Pelo TOTAL da 2.0 contra 1.5:
        // saldo de +0.5 por segundo, e a aura sobe com a tecnica ligada.
        assertEquals(0.5D, SaldoSustentado.porSegundo(1.0D, 2.0D, 1.5D), 1.0e-9D);
        assertTrue(SaldoSustentado.sePaga(1.0D, 2.0D, 1.5D),
                "Ten com custo 1.5 se paga, e a conferencia nao percebeu. Foi"
                        + " exatamente este numero que rodou em jogo -- a aura"
                        + " subia com Ten ligado, e ele virava buff permanente"
                        + " de graca.");

        // Com o custo corrigido para 3.0, o saldo fica negativo.
        assertEquals(-1.0D, SaldoSustentado.porSegundo(1.0D, 2.0D, 3.0D), 1.0e-9D);
        assertFalse(SaldoSustentado.sePaga(1.0D, 2.0D, 3.0D));
    }

    @Test
    @DisplayName("saldo exatamente zero TAMBEM se paga")
    void oEmpateNaoPassa() {
        // Um estado de saldo neutro nao custa nada para manter: nao ha motivo
        // para desliga-lo, e ele vira o padrao silencioso. "Negativo em
        // absoluto" nao admite empate -- e um `<=` trocado por `<` aqui e a
        // mudanca de um caractere que ninguem revisa.
        assertTrue(SaldoSustentado.sePaga(1.0D, 2.0D, 2.0D),
                "saldo zero passou como aceitavel.");
    }

    @Test
    @DisplayName("o problema relatado diz o numero, a conta e o que fazer")
    void oRelatoEInstrutivo() {
        List<String> achados = ConferenciaDeBalanceamento.problemas(1.0D,
                List.of(new ConferenciaDeBalanceamento.Estado("Ten", 2.0D, 1.5D)));

        assertEquals(1, achados.size(), "o problema nao foi detectado: " + achados);
        String texto = achados.get(0);

        // Recusa sem instrucao gera a mesma pergunta toda vez. Quem ler o log
        // precisa sair sabendo qual chave girar e para quanto.
        assertTrue(texto.contains("+0.50"), "o saldo nao aparece: " + texto);
        assertTrue(texto.contains("tecnica.ten.custoPorSegundo"),
                "a chave a girar nao aparece: " + texto);
        assertTrue(texto.contains("2.00"),
                "o custo minimo necessario nao aparece: " + texto);
    }

    @Test
    @DisplayName("configuracao sadia nao produz ruido")
    void semProblemaSemRuido() {
        // Um aviso que aparece sempre deixa de ser lido. Se esta lista nao for
        // vazia no caso bom, o log vira barulho de fundo e o proximo defeito
        // de verdade passa junto.
        assertTrue(ConferenciaDeBalanceamento.problemas(1.0D, List.of(
                        new ConferenciaDeBalanceamento.Estado("Ten", 2.0D, 3.0D),
                        new ConferenciaDeBalanceamento.Estado("Zetsu", 3.0D, 4.0D)))
                .isEmpty());
    }

    @Test
    @DisplayName("cada estado quebrado vira um problema proprio")
    void doisEstadosDoisProblemas() {
        assertEquals(2, ConferenciaDeBalanceamento.problemas(1.0D, List.of(
                new ConferenciaDeBalanceamento.Estado("Ten", 2.0D, 1.5D),
                new ConferenciaDeBalanceamento.Estado("Zetsu", 3.0D, 1.2D))).size(),
                "reportar so o primeiro faria o segundo aparecer apenas depois"
                        + " de o primeiro ser consertado -- duas rodadas para um"
                        + " problema que da para ver de uma vez.");
    }
}
