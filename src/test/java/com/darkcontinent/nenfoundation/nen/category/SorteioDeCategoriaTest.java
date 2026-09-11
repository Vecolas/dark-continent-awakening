package com.darkcontinent.nenfoundation.nen.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do sorteio de categoria.
 *
 * <p>O QUE ELE AFIRMA, e por que cada coisa e uma invariante e nao um numero:
 *
 * <ul>
 *   <li>determinismo -- sem ele, nenhum bug de categoria e reproduzivel;
 *   <li>{@code UNDETERMINED} nunca sai;
 *   <li>as SEIS sao alcancaveis -- uma que nunca sai nao produz erro nenhum;
 *   <li>a semente do mundo muda o resultado;
 *   <li>a distribuicao nao e degenerada.
 * </ul>
 *
 * <p>Nenhum teste aqui crava "a semente 7 da Emission". Um portao que fixa o
 * valor de saida envelhece junto com qualquer ajuste na mistura e passa a
 * reprovar codigo certo -- e, pior, ele nao afirma NADA sobre a propriedade
 * que importa.
 */
class SorteioDeCategoriaTest {

    /** UUIDs derivados de um contador: reproduziveis, e nao aleatorios. */
    private static UUID jogador(int n) {
        return new UUID(0x5EED_0000_0000_0000L | n, 0xBEEF_0000_0000_0000L | (long) n * 31L);
    }

    @Test
    @DisplayName("mesma semente e mesmo jogador: sempre a mesma categoria")
    void deterministico() {
        UUID gon = jogador(1);
        NenCategory primeira = SorteioDeCategoria.sortear(1234L, gon);

        for (int i = 0; i < 100; i++) {
            assertSame(primeira, SorteioDeCategoria.sortear(1234L, gon),
                    "O sorteio mudou entre chamadas. Sem determinismo, a"
                            + " primeira pergunta de qualquer investigacao --"
                            + " 'aconteceu de novo?' -- fica sem resposta.");
        }
    }

    @Test
    @DisplayName("UNDETERMINED nunca e sorteada")
    void nuncaSorteiaONeutro() {
        int conferidos = 0;
        for (long semente = 0; semente < 2_000; semente++) {
            for (int p = 0; p < 5; p++) {
                NenCategory c = SorteioDeCategoria.sortear(semente, jogador(p));
                assertTrue(c.eReal(),
                        "O sorteio devolveu " + c + ". UNDETERMINED e o NEUTRO:"
                                + " ele significa 'ainda nao sorteada'. Sorteá-lo"
                                + " tornaria os dois estados indistinguiveis.");
                conferidos++;
            }
        }
        assertEquals(10_000, conferidos,
                "O laco nao percorreu o que devia. Tabela vazia nao e aprovacao:"
                        + " um portao com zero verificacoes tem de reprovar.");
    }

    @Test
    @DisplayName("as seis categorias sao alcancaveis")
    void todasAsSeisSaem() {
        Set<NenCategory> vistas = new HashSet<>();
        for (long semente = 0; semente < 500 && vistas.size() < 6; semente++) {
            vistas.add(SorteioDeCategoria.sortear(semente, jogador(0)));
        }

        assertEquals(new HashSet<>(NenCategory.REAIS), vistas,
                "Ha categoria que o sorteio nunca produz. O sintoma disso no"
                        + " jogo e mudo: a categoria existe no enum, aparece na"
                        + " lista, e nenhum jogador jamais a recebe.");
    }

    @Test
    @DisplayName("a semente do mundo muda o resultado")
    void aSementeImporta() {
        UUID mesmoJogador = jogador(7);
        Set<NenCategory> porSemente = new HashSet<>();
        for (long semente = 0; semente < 200; semente++) {
            porSemente.add(SorteioDeCategoria.sortear(semente, mesmoJogador));
        }

        assertTrue(porSemente.size() > 1,
                "O mesmo jogador recebe a mesma categoria em TODO mundo. Isso"
                        + " tornaria a categoria uma funcao so do UUID, e uma"
                        + " tabela publicada na internet mataria a Adivinhacao da"
                        + " Agua no dia seguinte ao lancamento.");
    }

    @Test
    @DisplayName("o jogador muda o resultado: dois jogadores no mesmo mundo diferem")
    void oJogadorImporta() {
        Set<NenCategory> porJogador = new HashSet<>();
        for (int p = 0; p < 200; p++) {
            porJogador.add(SorteioDeCategoria.sortear(99L, jogador(p)));
        }

        assertTrue(porJogador.size() > 1,
                "Todos os jogadores do mesmo mundo receberam a mesma categoria."
                        + " Um servidor inteiro de Enhancement nao da erro nenhum;"
                        + " so parece um mundo sem graca.");
    }

    /**
     * Controle contra distribuicao degenerada.
     *
     * <p>O que este teste afirma e a REGRA -- "nenhuma categoria e rara a ponto
     * de nao existir na pratica" --, e nao um numero. O piso de 5% e folgado de
     * proposito: com 60 000 sorteios o esperado e 16,7% cada, e o desvio padrao
     * fica na casa de 0,15 pontos. Chegar a 5% exigiria um defeito grosseiro na
     * mistura, nao azar.
     *
     * <p>Medido na escrita deste teste: entre 16,3% e 17,0%. O numero esta aqui
     * como NOTA historica; nao e o que o portao afirma.
     */
    @Test
    @DisplayName("nenhuma categoria fica abaixo de 5% em 60 000 sorteios")
    void distribuicaoNaoEDegenerada() {
        final int sorteios = 60_000;
        Map<NenCategory, Integer> contagem = new EnumMap<>(NenCategory.class);
        for (NenCategory c : NenCategory.REAIS) {
            contagem.put(c, 0);
        }

        for (int i = 0; i < sorteios; i++) {
            NenCategory c = SorteioDeCategoria.sortear(i, jogador(i % 997));
            contagem.merge(c, 1, Integer::sum);
        }

        int piso = sorteios / 20;
        for (Map.Entry<NenCategory, Integer> e : contagem.entrySet()) {
            assertTrue(e.getValue() >= piso,
                    e.getKey() + " saiu " + e.getValue() + " vez(es) em " + sorteios
                            + ", abaixo do piso de " + piso + ". A distribuicao esta"
                            + " degenerada: a categoria existe, mas na pratica"
                            + " ninguem a recebe.");
        }
        assertEquals(sorteios, contagem.values().stream().mapToInt(Integer::intValue).sum(),
                "A soma das contagens nao fecha com o numero de sorteios.");
    }

    /**
     * As duas metades do UUID entram em rodadas de mistura SEPARADAS.
     *
     * <p>Uma implementacao que as combinasse com um XOR unico daria o mesmo
     * resultado para {@code UUID(a, b)} e {@code UUID(b, a)} -- SEMPRE. A
     * colisao seria invisivel: dois jogadores diferentes, a mesma categoria,
     * nenhum erro em lugar nenhum.
     *
     * <p>O teste conta PARES, e nao um par. Um par so tem uma chance em seis
     * de coincidir por acaso, e um portao que reprova por azar e pior que
     * portao nenhum. Com mistura correta o esperado e 5/6 dos pares
     * diferentes; com XOR seria zero. O piso de 60% separa os dois casos com
     * folga enorme.
     *
     * <p>Medido na escrita deste teste: 83,4% dos pares diferentes -- nota
     * historica, nao o que o portao afirma.
     */
    @Test
    @DisplayName("trocar as metades do UUID muda o resultado na maioria dos pares")
    void asMetadesDoUuidNaoSeAnulam() {
        final int pares = 3_000;
        int diferentes = 0;

        for (int i = 0; i < pares; i++) {
            long a = 0x0123456789ABCDEFL * (i + 1);
            long b = 0x76543210FEDCBA98L * (i + 7);
            if (SorteioDeCategoria.sortear(5L, new UUID(a, b))
                    != SorteioDeCategoria.sortear(5L, new UUID(b, a))) {
                diferentes++;
            }
        }

        int piso = pares * 3 / 5;
        assertTrue(diferentes >= piso,
                "So " + diferentes + " de " + pares + " pares mudaram ao trocar as"
                        + " metades do UUID (piso " + piso + "). As metades estao se"
                        + " anulando na mistura, e UUIDs espelhados colidem em"
                        + " silencio.");
    }
}
