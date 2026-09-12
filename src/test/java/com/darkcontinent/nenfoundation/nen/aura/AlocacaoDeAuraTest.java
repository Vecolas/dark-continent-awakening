package com.darkcontinent.nenfoundation.nen.aura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A invariante que da risco as tecnicas.
 *
 * <p>A SOMA SER 1.0 E A TECNICA INTEIRA. Concentrar num lugar tem de TIRAR de
 * outro -- e isso que faz Gyo ser escolha e Ko ser perigoso. Uma alocacao que
 * nao fechasse viraria bonus, e bonus nao e tatico.
 */
class AlocacaoDeAuraTest {

    @Test
    @DisplayName("a uniforme fecha, e todas as regioes valem o mesmo")
    void uniformeFecha() {
        AlocacaoDeAura a = AlocacaoDeAura.uniforme();
        assertTrue(a.soma(), "a alocacao de repouso nao fecha em 1.0");

        float esperado = RegiaoDoCorpo.fracaoUniforme();
        for (RegiaoDoCorpo r : RegiaoDoCorpo.values()) {
            assertEquals(esperado, a.em(r), 1.0e-6F, "regiao desigual em repouso: " + r);
        }
    }

    @Test
    @DisplayName("concentrar fecha para qualquer fracao, e o resto se divide")
    void concentrarFecha() {
        // A CONTA DO RESTO E ONDE UM ERRO PASSA BATIDO. Dividir por 6 em vez de
        // por 5 -- esquecendo que a regiao concentrada nao entra no rateio --
        // produz uma soma menor que 1, e nada no jogo acusaria: a aura
        // simplesmente sumiria um pouco.
        for (float fracao : new float[] {0.0F, 0.1F, 0.45F, 0.5F, 0.99F, 1.0F}) {
            AlocacaoDeAura a = AlocacaoDeAura.concentrando(RegiaoDoCorpo.BRACO_DIREITO, fracao);
            assertTrue(a.soma(), "nao fecha com fracao " + fracao + ": " + a);
            assertEquals(fracao, a.em(RegiaoDoCorpo.BRACO_DIREITO), 1.0e-5F);
        }
    }

    @Test
    @DisplayName("concentrar TIRA das outras regioes")
    void concentrarTiraDasOutras() {
        AlocacaoDeAura repouso = AlocacaoDeAura.uniforme();
        AlocacaoDeAura gyo = AlocacaoDeAura.concentrando(RegiaoDoCorpo.CABECA, 0.45F);

        assertTrue(gyo.em(RegiaoDoCorpo.CABECA) > repouso.em(RegiaoDoCorpo.CABECA),
                "a regiao concentrada nao ganhou nada");
        for (RegiaoDoCorpo r : RegiaoDoCorpo.values()) {
            if (r == RegiaoDoCorpo.CABECA) {
                continue;
            }
            assertTrue(gyo.em(r) < repouso.em(r),
                    "a regiao " + r + " nao perdeu aura. Se concentrar nao tira de"
                            + " lugar nenhum, Gyo vira bonus em vez de escolha -- e"
                            + " Ko deixa de ter risco.");
        }
    }

    @Test
    @DisplayName("Ko leva quase tudo, e o resto fica perto de zero")
    void koEsvaziaOResto() {
        // O caso extremo do canone: ~100% num punho, ~0% no resto. Se a conta
        // do resto estourasse para negativo, a soma continuaria fechando e o
        // defeito so apareceria como defesa negativa mais tarde.
        AlocacaoDeAura ko = AlocacaoDeAura.concentrando(RegiaoDoCorpo.BRACO_DIREITO, 1.0F);
        assertTrue(ko.soma());
        for (RegiaoDoCorpo r : RegiaoDoCorpo.values()) {
            assertTrue(ko.em(r) >= 0.0F, "regiao com aura NEGATIVA: " + r + " = " + ko.em(r));
        }
        assertEquals(RegiaoDoCorpo.BRACO_DIREITO, ko.maisConcentrada());
    }

    @Test
    @DisplayName("NaN e infinito sao recusados, e nao clampados")
    void naoAceitaNaoNumero() {
        // Math.min/max PROPAGAM NaN em vez de segura-lo. Este projeto ja deixou
        // NaN atravessar o protocolo uma vez por causa disso, e o sintoma foi
        // uma barra congelada para sempre, sem excecao e sem log.
        for (float ruim : new float[] {Float.NaN, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, -0.1F, 1.1F}) {
            assertThrows(IllegalArgumentException.class,
                    () -> AlocacaoDeAura.concentrando(RegiaoDoCorpo.TRONCO, ruim),
                    "aceitou fracao invalida: " + ruim);
        }
        assertThrows(IllegalArgumentException.class,
                () -> AlocacaoDeAura.concentrando(null, 0.5F));
    }

    @Test
    @DisplayName("concentrar devolve objeto NOVO, sem mexer no anterior")
    void imutavel() {
        // Com Ryu a redistribuicao acontece constantemente sobre estado
        // compartilhado. Um objeto mutavel ali seria o erro numero 2 da lista
        // do CLAUDE.md: dois jogadores escrevendo no mesmo lugar.
        AlocacaoDeAura antes = AlocacaoDeAura.uniforme();
        AlocacaoDeAura depois = AlocacaoDeAura.concentrando(RegiaoDoCorpo.PERNA_ESQUERDA, 0.8F);

        assertNotSame(antes, depois);
        assertEquals(RegiaoDoCorpo.fracaoUniforme(), antes.em(RegiaoDoCorpo.PERNA_ESQUERDA),
                1.0e-6F, "a alocacao anterior foi alterada");
    }

    @Test
    @DisplayName("sao SEIS regioes, e a ordem e contrato")
    void seisRegioesEmOrdem() {
        // A ORDEM VIRA A ORDEM DOS CAMPOS NA REDE. Reordenar aqui trocaria
        // braco por perna em todo cliente conectado -- sem erro nenhum, so com
        // a aura no lugar errado.
        assertEquals(6, RegiaoDoCorpo.values().length);
        assertEquals(List.of("CABECA", "TRONCO", "BRACO_ESQUERDO", "BRACO_DIREITO",
                        "PERNA_ESQUERDA", "PERNA_DIREITA"),
                java.util.Arrays.stream(RegiaoDoCorpo.values()).map(Enum::name).toList(),
                "a ordem ou o conjunto das regioes mudou. Juntar os bracos num so"
                        + " impediria Ko num punho, que e o exemplo canonico.");
    }
}
