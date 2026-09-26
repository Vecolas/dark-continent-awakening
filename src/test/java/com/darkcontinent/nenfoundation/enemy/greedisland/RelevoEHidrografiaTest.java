package com.darkcontinent.nenfoundation.enemy.greedisland;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandElevationField;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandHydrologyField;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandMask;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandRidgeField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Os portoes das fases G2 (montanhas e bacias) e G3 (hidrografia).
 *
 * <p>O que eles cobram nao e "esta bonito" -- e que a estrutura EXISTA e seja
 * coerente. Uma cordilheira que nao sobe, um rio que corre morro acima, uma
 * bacia que ficou num morro: nenhum desses da erro. Todos aparecem como um
 * mapa que nao faz sentido.
 */
class RelevoEHidrografiaTest {

    // ------------------------------------------------------------------
    // G2 — MONTANHAS
    // ------------------------------------------------------------------

    @Test
    @DisplayName("as tres cordilheiras SOBEM de verdade, e ate a faixa do documento")
    void asCadeiasExistem() {
        for (var c : GreedIslandConstants.CORDILHEIRAS) {
            int maior = Integer.MIN_VALUE;
            for (var no : c.nos()) {
                maior = Math.max(maior, GreedIslandElevationField.alturaEm(no.x(), no.z()));
            }
            assertTrue(maior >= c.alturaMinima(),
                    c.id() + " chega so a Y=" + maior + ", abaixo do minimo declarado de "
                            + c.alturaMinima() + ". Uma cordilheira que nao sobe nao"
                            + " separa nada, e o mapa deixa de ser memorizavel.");
            assertTrue(maior <= GreedIslandConstants.PICO_MAXIMO,
                    c.id() + " passou do teto de " + GreedIslandConstants.PICO_MAXIMO);
        }
    }

    @Test
    @DisplayName("a cadeia e uma LINHA -- longe da crista o terreno cai")
    void aCordilheiraTemEixo() {
        // O que separa cadeia de mancha alta: a altura tem de cair conforme se
        // afasta da crista. Ruido alto nao faz isso -- ele da manchas.
        var c = GreedIslandConstants.CORDILHEIRAS.get(0);
        var meio = c.nos().get(c.nos().size() / 2);

        int naCrista = GreedIslandElevationField.alturaEm(meio.x(), meio.z());
        int aLado = GreedIslandElevationField.alturaEm(meio.x(), meio.z() + c.larguraBase());

        assertTrue(naCrista > aLado + 60,
                "a " + c.larguraBase() + " blocos da crista o terreno ainda esta a Y="
                        + aLado + " contra Y=" + naCrista + " em cima: isso e um planalto"
                        + " alto, e nao uma cadeia");
    }

    @Test
    @DisplayName("ha PASSOS na cadeia -- ela nao e uma muralha de altura constante")
    void aCadeiaTemPassos() {
        var c = GreedIslandConstants.CORDILHEIRAS.get(0);
        int menor = Integer.MAX_VALUE;
        int maior = Integer.MIN_VALUE;
        var primeiro = c.nos().get(0);
        var ultimo = c.nos().get(c.nos().size() - 1);

        for (int i = 0; i <= 60; i++) {
            double t = i / 60.0D;
            double x = primeiro.x() + (ultimo.x() - primeiro.x()) * t;
            double z = primeiro.z() + (ultimo.z() - primeiro.z()) * t;
            if (GreedIslandRidgeField.distanciaAteACrista(x, z) > 1_500) {
                continue;
            }
            int y = GreedIslandElevationField.alturaEm(x, z);
            menor = Math.min(menor, y);
            maior = Math.max(maior, y);
        }
        assertTrue(maior - menor >= 40,
                "a crista varia so " + (maior - menor) + " blocos de ponta a ponta: sem"
                        + " variacao nao ha passo, e sem passo nao ha por onde a estrada"
                        + " do G5 atravessar");
    }

    @Test
    @DisplayName("as bacias ficam BAIXAS -- a cidade precisa caber nelas")
    void asBaciasSaoBaixas() {
        for (var b : GreedIslandConstants.BACIAS) {
            int noCentro = GreedIslandElevationField.alturaEm(b.x(), b.z());
            int foraDela = GreedIslandElevationField.alturaEm(
                    b.x() + b.raio() * 1.6, b.z());
            assertTrue(noCentro < foraDela,
                    "a bacia " + b.id() + " esta a Y=" + noCentro + " e o entorno a Y="
                            + foraDela + ": ela virou um morro, e a cidade nasce em cima");
        }
    }

    // ------------------------------------------------------------------
    // G3 — HIDROGRAFIA
    // ------------------------------------------------------------------

    @Test
    @DisplayName("todo rio DESCE da nascente ate a foz")
    void aAguaCorreParaBaixo() {
        for (var rio : GreedIslandConstants.RIOS) {
            var nascente = rio.curso().get(0);
            var foz = rio.curso().get(rio.curso().size() - 1);
            int yNascente = GreedIslandElevationField.alturaEm(nascente.x(), nascente.z());
            int yFoz = GreedIslandElevationField.alturaEm(foz.x(), foz.z());

            assertTrue(yNascente > yFoz,
                    "o rio " + rio.id() + " nasce a Y=" + yNascente + " e desemboca a Y="
                            + yFoz + ": ele corre morro acima. Isso nao levanta excecao --"
                            + " so produz um rio impossivel.");
        }
    }

    @Test
    @DisplayName("todo rio TERMINA no mar ou perto dele")
    void nenhumRioMorreEmTerraSeca() {
        for (var rio : GreedIslandConstants.RIOS) {
            var foz = rio.curso().get(rio.curso().size() - 1);
            double distanciaDaCosta = GreedIslandMask.distanciaComSinal(foz.x(), foz.z());
            assertTrue(distanciaDaCosta < 3_000.0D,
                    "a foz do " + rio.id() + " esta a " + (int) distanciaDaCosta
                            + " blocos para dentro da ilha: o rio acaba numa vala que nao"
                            + " leva a lugar nenhum");
        }
    }

    @Test
    @DisplayName("o rio ENGROSSA da nascente para a foz")
    void aLarguraCresce() {
        for (var rio : GreedIslandConstants.RIOS) {
            var nascente = rio.curso().get(0);
            var foz = rio.curso().get(rio.curso().size() - 1);
            double naNascente = GreedIslandHydrologyField.medir(
                    nascente.x(), nascente.z(), rio)[1];
            double naFoz = GreedIslandHydrologyField.medir(foz.x(), foz.z(), rio)[1];

            assertTrue(naFoz > naNascente * 1.5D,
                    rio.id() + ": largura " + (int) naNascente + " na cabeceira e "
                            + (int) naFoz + " na foz. Sem a escada da secao 34 o jogador"
                            + " nao sabe para que lado a agua desce.");
        }
    }

    @Test
    @DisplayName("o rio ESCAVA o terreno, e nao flutua sobre ele")
    void oLeitoFicaAbaixoDaMargem() {
        for (var rio : GreedIslandConstants.RIOS) {
            var meio = rio.curso().get(rio.curso().size() / 2);
            int noLeito = GreedIslandElevationField.alturaEm(meio.x(), meio.z());
            int naMargem = GreedIslandElevationField.alturaEm(meio.x() + 900, meio.z() + 900);

            assertTrue(noLeito <= naMargem,
                    rio.id() + " tem leito a Y=" + noLeito + " e margem a Y=" + naMargem
                            + ": a agua correria morro acima");
        }
    }

    @Test
    @DisplayName("todo lago esta em terra, e nao no oceano")
    void osLagosEstaoNaIlha() {
        for (var lago : GreedIslandConstants.LAGOS) {
            assertTrue(GreedIslandMask.terra(lago.x(), lago.z()),
                    "o lago " + lago.id() + " caiu no mar: ele viraria uma mancha de agua"
                            + " dentro de outra agua, invisivel e inutil");
        }
    }

    @Test
    @DisplayName("os sete sistemas e os oito lagos do documento estao todos la")
    void aContagemBateComODocumento() {
        assertTrue(GreedIslandConstants.RIOS.size() == 7,
                "o documento lista sete sistemas hidrograficos; ha "
                        + GreedIslandConstants.RIOS.size());
        assertTrue(GreedIslandConstants.LAGOS.size() >= 8,
                "a secao 33 lista oito lagos principais; ha "
                        + GreedIslandConstants.LAGOS.size());
        assertTrue(GreedIslandConstants.CORDILHEIRAS.size() == 3,
                "a secao 18 lista tres sistemas montanhosos");
    }
}
