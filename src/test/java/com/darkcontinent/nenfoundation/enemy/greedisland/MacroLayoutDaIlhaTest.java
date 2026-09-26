package com.darkcontinent.nenfoundation.enemy.greedisland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandElevationField;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandMask;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O GATE MACRO da fase G1: "isso parece uma ilha continental?"
 *
 * <p>Ele existe porque o documento de macrogeografia separa dois julgamentos, e
 * o motivo e concreto: e perfeitamente possivel ter Masadora linda, Soufrabi
 * linda e florestas lindas, e Greed Island inteira parecer um parque tematico
 * de cinco quilometros quadrados. Uma screenshot nunca responde isso.
 *
 * <p>Ele nao gera um chunk. A mascara e o campo de elevacao sao funcoes puras,
 * e a ilha inteira e amostrada em milhares de pontos em menos de um segundo.
 *
 * <p>O primeiro nao-negociavel do documento e <b>"No circular island"</b>, e a
 * maior parte deste arquivo existe para cobrar exatamente isso.
 */
class MacroLayoutDaIlhaTest {

    /** Passo de amostragem. Grosso o bastante para ser rapido, fino para a forma. */
    private static final int PASSO = 250;

    // ------------------------------------------------------------------
    // 1. NAO E UM CIRCULO
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a costa NAO e um circulo -- ela se desvia do circulo medio")
    void aIlhaNaoECircular() {
        // A PRIMEIRA VERSAO MEDIA razao entre maior e menor raio, exigindo
        // 1,6x. Estava errada: as treze ancoras do documento dao 1,26x por
        // construcao, e um portao que contradiz os dados da propria
        // especificacao reprova a entrega correta.
        //
        // O que separa ilha de disco nao e alongamento -- uma elipse e lisa e
        // continua lendo como disco. E o DESVIO LOCAL da costa em relacao ao
        // circulo que melhor a aproxima.
        double[] raios = new double[120];
        double soma = 0.0D;
        for (int i = 0; i < raios.length; i++) {
            raios[i] = raioNaDirecao(i * 2.0D * Math.PI / raios.length);
            soma += raios[i];
        }
        double medio = soma / raios.length;
        double variancia = 0.0D;
        for (double r : raios) {
            variancia += (r - medio) * (r - medio);
        }
        double desvio = Math.sqrt(variancia / raios.length) / medio;

        assertTrue(desvio >= 0.07D,
                "a costa se desvia so " + String.format("%.1f%%", desvio * 100)
                        + " do circulo medio. Isso e um disco, e o nao-negociavel"
                        + " numero 1 do documento e 'No circular island'.");
    }

    @Test
    @DisplayName("o NORTE e mais estreito que o centro -- secao 12")
    void oNorteEEstreito() {
        // A primeira versao comparava raio leste com oeste, e as ancoras sao
        // quase simetricas nesse eixo: o teste cobrava algo que a
        // especificacao nao promete. O que a secao 12 promete e "Norte: mais
        // estreito e montanhoso".
        double norte = larguraNaLatitude(-26_000);
        double centro = larguraNaLatitude(0);

        assertTrue(norte < centro * 0.85D,
                "o norte tem " + (int) norte + " contra " + (int) centro + " no centro:"
                        + " a secao 12 pede norte mais estreito");
    }

    @Test
    @DisplayName("a costa tem reentrancias -- ela nao e convexa")
    void aCostaERecortada() {
        // Numa forma convexa, o raio cresce e decresce uma vez so por volta.
        // Peninsulas e baias produzem varias inversoes, e e isso que se conta.
        int inversoes = 0;
        double anterior = raioNaDirecao(0);
        boolean subindo = true;

        for (int grau = 3; grau <= 360; grau += 3) {
            double atual = raioNaDirecao(Math.toRadians(grau));
            boolean agora = atual > anterior;
            if (agora != subindo) {
                inversoes++;
                subindo = agora;
            }
            anterior = atual;
        }
        assertTrue(inversoes >= 6,
                "so " + inversoes + " inversoes de raio: a costa esta lisa demais. O"
                        + " documento pede peninsulas, baias, enseadas e promontorios.");
    }

    // ------------------------------------------------------------------
    // 2. A ESCALA E A DO DOCUMENTO
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a ilha ocupa dezenas de milhares de blocos, e nao centenas")
    void aEscalaEContinental() {
        int[] caixa = caixaDaTerra();
        int larguraX = caixa[1] - caixa[0];
        int larguraZ = caixa[3] - caixa[2];

        assertTrue(larguraX >= 50_000,
                "a ilha tem so " + larguraX + " blocos de leste a oeste; o alvo e ~80.000."
                        + " O andaime tinha 1.760, e este portao existe para ele nunca"
                        + " mais ser confundido com a entrega.");
        assertTrue(larguraZ >= 45_000,
                "a ilha tem so " + larguraZ + " blocos de norte a sul; o alvo e ~70.000");
        // "~80.000" no documento, e nao "exatamente": as peninsulas empurram a
        // costa para fora de proposito. Dez por cento cobre isso sem deixar a
        // ilha crescer sem controle.
        assertTrue(larguraX <= GreedIslandConstants.EXTENSAO_LESTE_OESTE * 1.15,
                "a ilha tem " + larguraX + " de largura, muito alem dos "
                        + GreedIslandConstants.EXTENSAO_LESTE_OESTE + " declarados");
        assertTrue(larguraZ <= GreedIslandConstants.EXTENSAO_NORTE_SUL * 1.15,
                "a ilha tem " + larguraZ + " de altura, muito alem do declarado");
    }

    @Test
    @DisplayName("a barreira fica LONGE da costa, com oceano navegavel")
    void haOceanoAntesDoMuro() {
        int[] caixa = caixaDaTerra();
        int maiorAlcance = Math.max(Math.max(Math.abs(caixa[0]), caixa[1]),
                Math.max(Math.abs(caixa[2]), caixa[3]));
        assertTrue(GreedIslandConstants.RAIO_DA_BARREIRA > maiorAlcance + 3_000,
                "a barreira em " + GreedIslandConstants.RAIO_DA_BARREIRA + " corta a ilha"
                        + " ou encosta nela (alcance da terra: " + maiorAlcance + ")");
    }

    // ------------------------------------------------------------------
    // 3. AS CIDADES
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TODA cidade nasce em terra firme")
    void nenhumaCidadeNoMar() {
        for (var cidade : GreedIslandConstants.CIDADES) {
            assertTrue(GreedIslandMask.terra(cidade.x(), cidade.z()),
                    cidade.id() + " caiu no oceano em (" + cidade.x() + ", " + cidade.z()
                            + "). Uma ancora submersa nao da erro: a cidade simplesmente"
                            + " nao aparece, e ninguem procura por ela.");
        }
    }

    @Test
    @DisplayName("nenhuma cidade cabe no render distance de outra")
    void asCidadesNaoSeAglomeram() {
        // O documento reprova o mapa se mais de uma cidade importante aparecer
        // no mesmo render distance. 32 chunks = 512 blocos; o dobro disso e uma
        // margem folgada, e ainda assim nenhum par pode viola-la.
        int minimo = 1_024;
        var lista = GreedIslandConstants.CIDADES;
        for (int i = 0; i < lista.size(); i++) {
            for (int j = i + 1; j < lista.size(); j++) {
                double d = Math.hypot(lista.get(i).x() - lista.get(j).x(),
                        lista.get(i).z() - lista.get(j).z());
                assertTrue(d > minimo, lista.get(i).id() + " e " + lista.get(j).id()
                        + " estao a " + (int) d + " blocos: uma veria a outra");
            }
        }
    }

    @Test
    @DisplayName("as distancias entre hubs justificam as Spell Cards")
    void aEscalaDaViagemEaDoDocumento() {
        // A pergunta do documento: "se Masadora fica a 600 blocos de Antokiba,
        // por que eu gastaria uma carta?" A resposta tem de ser a geografia.
        double shisoAntokiba = distancia("shiso_tree", "antokiba");
        assertTrue(shisoAntokiba >= 1_200 && shisoAntokiba <= 7_000,
                "Shiso -> Antokiba a " + (int) shisoAntokiba + ": a primeira caminhada"
                        + " tem de ser longa o bastante para explorar e curta o bastante"
                        + " para nao perder o jogador na primeira hora");

        assertTrue(distancia("antokiba", "masadora") >= 15_000,
                "Antokiba -> Masadora curto demais: a carta perde o sentido");
        assertTrue(distancia("shiso_tree", "soufrabi") >= 40_000,
                "os extremos da ilha precisam ser realmente distantes");
    }

    @Test
    @DisplayName("Shiso Tree existe e e o hub de entrada")
    void oPontoDePartidaEstaDeclarado() {
        assertTrue(GreedIslandConstants.cidade(GreedIslandConstants.CIDADE_INICIAL)
                .isPresent(), "a cidade inicial aponta para um id que nao existe");
        assertEquals(8, GreedIslandConstants.CIDADES.size(),
                "o documento congela sete cidades mais o Shiso Tree");
    }

    // ------------------------------------------------------------------
    // 4. A ILHA E UMA SO, E TEM INTERIOR
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a maior parte da area amostrada e OCEANO -- e uma ilha, nao um continente")
    void aIlhaEUmaIlha() {
        // A AMOSTRAGEM VAI ATE PERTO DA BARREIRA, e nao ate a caixa da ilha. A
        // primeira versao amostrava 80.000 x 70.000 -- que E a extensao da
        // ilha -- e deu 91%: ela mediu a ilha dentro de si mesma. A pergunta
        // "sobra oceano?" so faz sentido na area navegavel.
        int terra = 0;
        int total = 0;
        for (int x = -47_000; x <= 47_000; x += 1_000) {
            for (int z = -47_000; z <= 47_000; z += 1_000) {
                total++;
                if (GreedIslandMask.terra(x, z)) {
                    terra++;
                }
            }
        }
        double fracao = terra / (double) total;
        assertTrue(fracao > 0.25D,
                "so " + String.format("%.0f%%", fracao * 100) + " de terra: a ilha sumiu"
                        + " dentro da propria caixa");
        assertTrue(fracao < 0.72D,
                String.format("%.0f%%", fracao * 100) + " de terra: nao sobra oceano, e a"
                        + " ilha deixa de ser uma ilha");
    }

    @Test
    @DisplayName("o relevo segue a escada da secao 17")
    void oRelevoRespeitaAsFaixas() {
        int menor = Integer.MAX_VALUE;
        int maior = Integer.MIN_VALUE;
        for (int x = -40_000; x <= 40_000; x += 2_000) {
            for (int z = -35_000; z <= 35_000; z += 2_000) {
                int y = GreedIslandElevationField.alturaEm(x, z);
                menor = Math.min(menor, y);
                maior = Math.max(maior, y);
                assertTrue(y > GreedIslandConstants.PISO_DO_MUNDO
                                && y < GreedIslandConstants.PISO_DO_MUNDO
                                        + GreedIslandConstants.ALTURA_DO_MUNDO,
                        "altura fora do mundo em (" + x + "," + z + "): " + y);
            }
        }
        assertTrue(menor < GreedIslandConstants.NIVEL_DO_MAR,
                "nenhum ponto abaixo do mar: nao ha oceano");
        assertTrue(maior > GreedIslandConstants.NIVEL_DO_MAR + 40,
                "o relevo nao passa de " + maior + ": a ilha e plana");
    }

    @Test
    @DisplayName("a costa e continua -- nada de terra picotada no mar")
    void naoHaConfeteNoOceano() {
        // Conta quantas amostras de terra estao ISOLADAS. Muitas indicam que o
        // ruido esta forte demais e a ilha virou arquipelago por acidente.
        int isoladas = 0;
        int terra = 0;
        for (int x = -40_000; x <= 40_000; x += PASSO * 4) {
            for (int z = -35_000; z <= 35_000; z += PASSO * 4) {
                if (!GreedIslandMask.terra(x, z)) {
                    continue;
                }
                terra++;
                int vizinhos = 0;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if ((dx != 0 || dz != 0)
                                && GreedIslandMask.terra(x + dx * PASSO * 4,
                                        z + dz * PASSO * 4)) {
                            vizinhos++;
                        }
                    }
                }
                if (vizinhos == 0) {
                    isoladas++;
                }
            }
        }
        assertTrue(isoladas <= terra * 0.03D,
                isoladas + " de " + terra + " amostras de terra estao isoladas: o ruido"
                        + " da costa esta forte demais e a ilha virou confete");
    }

    // ------------------------------------------------------------------
    // 5. DETERMINISMO  (secao 94)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("o layout e IGUAL em toda chamada -- e por isso igual em todo servidor")
    void oLayoutEDeterministico() {
        Set<String> primeira = new HashSet<>();
        for (int x = -30_000; x <= 30_000; x += 7_000) {
            for (int z = -30_000; z <= 30_000; z += 7_000) {
                primeira.add(x + ":" + z + ":" + GreedIslandMask.distanciaComSinal(x, z));
            }
        }
        for (int x = -30_000; x <= 30_000; x += 7_000) {
            for (int z = -30_000; z <= 30_000; z += 7_000) {
                assertTrue(primeira.contains(
                                x + ":" + z + ":" + GreedIslandMask.distanciaComSinal(x, z)),
                        "a mascara devolveu valor diferente para (" + x + "," + z + "):"
                                + " a ilha mudaria entre dois servidores, e o guia de"
                                + " Greed Island deixaria de valer");
            }
        }
    }

    @Test
    @DisplayName("a versao do layout esta declarada como PROTOTIPO")
    void oPrototipoEstaMarcado() {
        assertTrue(GreedIslandLayoutVersion.ehPrototipo(),
                "o layout foi marcado como congelado, e a macrogeografia ainda nao tem"
                        + " cidades, rios nem estradas em bloco. A secao 129 do documento"
                        + " exige a marca justamente para o disco nao ser aceito como DoD.");
        assertTrue(GreedIslandLayoutVersion.AVISO_DE_PROTOTIPO.contains("scaffold"));
    }

    // ------------------------------------------------------------------

    /** A que distancia da origem a costa cruza uma direcao. */
    private static double raioNaDirecao(double angulo) {
        double ultimo = 0.0D;
        for (int r = 0; r <= 45_000; r += PASSO) {
            if (GreedIslandMask.terra(Math.cos(angulo) * r, Math.sin(angulo) * r)) {
                ultimo = r;
            }
        }
        return ultimo;
    }

    /** A largura de terra numa latitude, do ponto mais a oeste ao mais a leste. */
    private static double larguraNaLatitude(int z) {
        int oeste = Integer.MAX_VALUE;
        int leste = Integer.MIN_VALUE;
        for (int x = -47_000; x <= 47_000; x += 500) {
            if (GreedIslandMask.terra(x, z)) {
                oeste = Math.min(oeste, x);
                leste = Math.max(leste, x);
            }
        }
        return leste <= oeste ? 0.0D : leste - oeste;
    }

    /** minX, maxX, minZ, maxZ da terra amostrada. */
    private static int[] caixaDaTerra() {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (int x = -45_000; x <= 45_000; x += 500) {
            for (int z = -40_000; z <= 40_000; z += 500) {
                if (!GreedIslandMask.terra(x, z)) {
                    continue;
                }
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
        }
        return new int[] {minX, maxX, minZ, maxZ};
    }

    private static double distancia(String a, String b) {
        var ca = GreedIslandConstants.cidade(a).orElseThrow();
        var cb = GreedIslandConstants.cidade(b).orElseThrow();
        return Math.hypot(ca.x() - cb.x(), ca.z() - cb.z());
    }
}
