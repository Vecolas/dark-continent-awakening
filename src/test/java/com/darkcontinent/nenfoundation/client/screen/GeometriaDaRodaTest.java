package com.darkcontinent.nenfoundation.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da selecao da roda.
 *
 * <p>Menu radial erra de um jeito especifico, e todos os jeitos sao invisiveis
 * lendo o codigo: a fatia de cima partida ao meio, o indice deslocado em um, o
 * miolo selecionando a fatia da direita, a roda girada um quarto de volta. Este
 * arquivo mede cada um deles.
 */
class GeometriaDaRodaTest {

    private static final double RAIO_MORTO = 20.0D;

    private static int fatia(double dx, double dy, int fatias) {
        OptionalInt r = GeometriaDaRoda.fatiaEm(dx, dy, fatias, RAIO_MORTO);
        assertTrue(r.isPresent(), "esperava selecao em (" + dx + "," + dy + ")");
        return r.getAsInt();
    }

    @Test
    @DisplayName("apontar para CIMA seleciona a primeira fatia")
    void cimaEAPrimeira() {
        // A referencia visual poe a primeira fatia no topo. Usar atan2(dy, dx)
        // cru colocaria a primeira a direita -- a roda inteira girada um quarto
        // de volta em relacao ao desenho, e ninguem repara lendo o codigo.
        for (int fatias : new int[] {1, 2, 3, 4, 6, 8, 10}) {
            assertEquals(0, fatia(0.0D, -100.0D, fatias),
                    "com " + fatias + " fatias, o topo nao caiu na primeira");
        }
    }

    @Test
    @DisplayName("a fatia do topo NAO fica partida entre a primeira e a ultima")
    void topoNaoEPartido() {
        // Sem o deslocamento de meia fatia, o topo exato cai na fronteira: um
        // pixel para a esquerda seleciona a ULTIMA fatia, um para a direita
        // seleciona a primeira. E a posicao mais obvia de apontar.
        int fatias = 8;
        assertEquals(0, fatia(-6.0D, -100.0D, fatias), "um pouco a esquerda do topo");
        assertEquals(0, fatia(0.0D, -100.0D, fatias), "o topo exato");
        assertEquals(0, fatia(6.0D, -100.0D, fatias), "um pouco a direita do topo");
    }

    @Test
    @DisplayName("o sentido e HORARIO")
    void sentidoHorario() {
        int fatias = 4;
        assertEquals(0, fatia(0.0D, -100.0D, fatias), "cima");
        assertEquals(1, fatia(100.0D, 0.0D, fatias), "direita e a SEGUNDA, nao a ultima");
        assertEquals(2, fatia(0.0D, 100.0D, fatias), "baixo");
        assertEquals(3, fatia(-100.0D, 0.0D, fatias), "esquerda");
    }

    @Test
    @DisplayName("a zona morta nao seleciona nada")
    void zonaMortaNaoSeleciona() {
        // Sem a zona morta, o mouse parado no centro "aponta" para a fatia da
        // direita, porque o angulo de (0,0) e zero -- e o jogador ativa uma
        // tecnica que nunca escolheu.
        assertTrue(GeometriaDaRoda.fatiaEm(0.0D, 0.0D, 8, RAIO_MORTO).isEmpty(),
                "o centro exato selecionou alguma coisa");
        assertTrue(GeometriaDaRoda.fatiaEm(5.0D, 5.0D, 8, RAIO_MORTO).isEmpty(),
                "dentro da zona morta selecionou alguma coisa");
        assertTrue(GeometriaDaRoda.fatiaEm(0.0D, -RAIO_MORTO, 8, RAIO_MORTO).isPresent(),
                "exatamente na borda da zona morta deveria ja selecionar");
    }

    @Test
    @DisplayName("toda direcao cai numa fatia valida, e todas sao alcancaveis")
    void todasAsFatiasSaoAlcancaveis() {
        int fatias = 10;
        boolean[] vistas = new boolean[fatias];

        for (int grau = 0; grau < 360; grau++) {
            double rad = Math.toRadians(grau);
            double dx = Math.sin(rad) * 100.0D;
            double dy = -Math.cos(rad) * 100.0D;

            int indice = fatia(dx, dy, fatias);
            assertTrue(indice >= 0 && indice < fatias,
                    "indice fora da faixa em " + grau + " graus: " + indice);
            vistas[indice] = true;
        }
        for (int i = 0; i < fatias; i++) {
            assertTrue(vistas[i], "a fatia " + i + " e inalcancavel: nenhuma direcao cai nela");
        }
    }

    @Test
    @DisplayName("uma roda de UMA fatia sempre devolve zero")
    void umaFatiaSo() {
        // E o caso de hoje: so Ten existe. Uma roda de uma fatia nao pode ter
        // borda nenhuma que devolva indice 1.
        for (int grau = 0; grau < 360; grau += 7) {
            double rad = Math.toRadians(grau);
            assertEquals(0, fatia(Math.sin(rad) * 80.0D, -Math.cos(rad) * 80.0D, 1),
                    "em " + grau + " graus");
        }
    }

    @Test
    @DisplayName("roda sem fatia nenhuma nao estoura")
    void zeroFatias() {
        assertTrue(GeometriaDaRoda.fatiaEm(0.0D, -100.0D, 0, RAIO_MORTO).isEmpty(),
                "Uma roda vazia e o estado de quem ainda nao tem tecnica. Ela"
                        + " precisa devolver vazio, e nao dividir por zero.");
    }

    @Test
    @DisplayName("coordenada nao-finita devolve vazio em vez de indice negativo")
    void naoFinitoNaoEstoura() {
        assertTrue(GeometriaDaRoda.fatiaEm(Double.NaN, -100.0D, 8, RAIO_MORTO).isEmpty());
        assertTrue(GeometriaDaRoda.fatiaEm(0.0D, Double.POSITIVE_INFINITY, 8, RAIO_MORTO).isEmpty());
    }

    @Test
    @DisplayName("o centro de cada fatia cai DENTRO dela")
    void centroDaFatiaEConsistente() {
        // O render usa anguloCentralDaFatia para posicionar o rotulo. Se ele
        // discordar da selecao, o texto aparece numa fatia e o clique ativa
        // outra -- e o jogador jura que o menu esta quebrado.
        int fatias = 6;
        for (int i = 0; i < fatias; i++) {
            double angulo = GeometriaDaRoda.anguloCentralDaFatia(i, fatias);
            double dx = Math.sin(angulo) * 90.0D;
            double dy = -Math.cos(angulo) * 90.0D;
            assertEquals(i, fatia(dx, dy, fatias),
                    "o centro desenhado da fatia " + i + " seleciona outra fatia");
        }
    }
}
