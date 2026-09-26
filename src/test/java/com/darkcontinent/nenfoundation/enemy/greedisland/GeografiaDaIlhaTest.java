package com.darkcontinent.nenfoundation.enemy.greedisland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.worldtree.generation.FormaDaIlha;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O portao que impede a costa e a barreira de se afastarem uma da outra.
 *
 * <p>O raio da ilha vive em DOIS formatos que nao se leem: no
 * {@code noise_settings}, que desenha o relevo, e em
 * {@link GreedIslandGeografia}, que poe a barreira do mundo. Duplicacao
 * consciente -- e o que a impede de virar divergencia e este arquivo.
 *
 * <p>Sem ele, alguem afasta a barreira num dia e a costa no outro, e o sintoma
 * e nadar quinhentos blocos de mar vazio ate bater num muro invisivel. Nada
 * levanta excecao.
 */
class GeografiaDaIlhaTest {

    private static final String RUIDO =
            "src/main/resources/data/nenfoundation/worldgen/noise_settings/greed_island.json";

    // ------------------------------------------------------------------
    // 1. O JSON E O JAVA CONCORDAM
    // ------------------------------------------------------------------

    @Test
    @DisplayName("todo uso da funcao de ilha no JSON usa o raio e a transicao declarados")
    void oRelevoEABarreiraMedemAMesmaIlha() {
        List<JsonObject> usos = acharIlhas(JsonParser.parseString(Repo.texto(RUIDO)));
        assertTrue(usos.size() >= 2,
                "a funcao de ilha tem de alimentar o relevo E o `continents` -- com so um"
                        + " dos dois, ou o chao some sem virar oceano, ou vira oceano com"
                        + " chao embaixo. Achei " + usos.size());

        for (JsonObject uso : usos) {
            assertEquals(GreedIslandGeografia.RAIO_DA_TERRA, uso.get("raio").getAsDouble(),
                    0.001D, "raio no JSON diferente do declarado em GreedIslandGeografia");
            assertEquals(GreedIslandGeografia.TRANSICAO, uso.get("transicao").getAsDouble(),
                    0.001D, "transicao no JSON diferente da declarada");
        }
    }

    @Test
    @DisplayName("a barreira fica DEPOIS da costa, com mar aberto no meio")
    void haMarAntesDoMuro() {
        double folga = GreedIslandGeografia.RAIO_DA_BARREIRA
                - GreedIslandGeografia.fimDaCosta();
        assertTrue(folga > 0,
                "a barreira caiu em cima da praia (ou antes dela): a ilha apareceria"
                        + " cortada, e nao cercada. Folga = " + folga);
        assertTrue(folga >= 60,
                "so " + folga + " blocos de mar: nao le como ilha cercada");
        assertTrue(folga <= 400,
                folga + " blocos de mar vazio: o jogador rema achando que ha algo la fora");
    }

    @Test
    @DisplayName("o id registrado e o id que o JSON usa")
    void oTipoDaFuncaoTemRegistro() {
        for (JsonObject uso : acharIlhas(JsonParser.parseString(Repo.texto(RUIDO)))) {
            assertEquals("nenfoundation:ilha", uso.get("type").getAsString(),
                    "um id que nao bate com o registro faz o datapack nao validar, e a"
                            + " dimensao cai no gerador padrao SEM erro visivel");
        }
    }

    // ------------------------------------------------------------------
    // 2. A FUNCAO FAZ O QUE DIZ
    // ------------------------------------------------------------------

    @Test
    @DisplayName("dentro e terra, longe e mar, e a costa fica no meio")
    void aFormaEaEsperada() {
        double raio = GreedIslandGeografia.RAIO_DA_TERRA;
        double transicao = GreedIslandGeografia.TRANSICAO;

        assertEquals(1.0D, FormaDaIlha.valorEm(0, 0, raio, transicao), 0.001D,
                "o centro e terra");
        assertEquals(1.0D, FormaDaIlha.valorEm(raio - 5, 0, raio, transicao), 0.001D,
                "logo antes do raio ainda e terra");
        assertEquals(-1.0D, FormaDaIlha.valorEm(5000, 5000, raio, transicao), 0.001D,
                "longe e mar aberto");

        double naCosta = FormaDaIlha.valorEm(raio + transicao / 2.0D, 0, raio, transicao);
        assertTrue(naCosta < 1.0D && naCosta > -1.0D,
                "no meio da transicao o valor tem de estar entre os extremos: " + naCosta);
    }

    @Test
    @DisplayName("a ilha e redonda nos quatro sentidos -- e nao so no eixo X")
    void aDistanciaERadialEnaoManhattan() {
        double norte = FormaDaIlha.valorEm(0, -130, 100.0D, 40.0D);
        double leste = FormaDaIlha.valorEm(130, 0, 100.0D, 40.0D);
        double diagonal = FormaDaIlha.valorEm(92, 92, 100.0D, 40.0D);

        assertEquals(norte, leste, 0.001D, "a forma mudou de eixo");
        assertEquals(norte, diagonal, 0.02D,
                "na diagonal a distancia deu outro valor: a conta virou Manhattan e a"
                        + " ilha ficaria com cara de losango");
    }

    @Test
    @DisplayName("a saida fica presa entre -1 e 1")
    void osExtremosSaoRespeitados() {
        for (int d = 0; d < 2000; d += 37) {
            double v = FormaDaIlha.valorEm(d, 0, 100.0D, 40.0D);
            assertTrue(v >= -1.0D && v <= 1.0D, "estourou em " + d + ": " + v);
        }
    }

    @Test
    void medidasImpossiveisReprovamNaConstrucao() {
        assertThrows(IllegalArgumentException.class,
                () -> FormaDaIlha.exigirMedidasValidas(0.0D, 40.0D));
        assertThrows(IllegalArgumentException.class,
                () -> FormaDaIlha.exigirMedidasValidas(100.0D, 0.0D),
                "transicao zero seria um paredao de pedra no lugar da costa");
        assertThrows(IllegalArgumentException.class,
                () -> FormaDaIlha.exigirMedidasValidas(Double.NaN, 40.0D));
    }

    // ------------------------------------------------------------------

    /** Todos os objetos {@code nenfoundation:ilha} em qualquer profundidade do JSON. */
    private static List<JsonObject> acharIlhas(JsonElement raiz) {
        List<JsonObject> achados = new ArrayList<>();
        varrer(raiz, achados);
        return achados;
    }

    private static void varrer(JsonElement e, List<JsonObject> achados) {
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            JsonElement tipo = o.get("type");
            if (tipo != null && tipo.isJsonPrimitive()
                    && "nenfoundation:ilha".equals(tipo.getAsString())) {
                achados.add(o);
            }
            o.entrySet().forEach(par -> varrer(par.getValue(), achados));
        } else if (e.isJsonArray()) {
            e.getAsJsonArray().forEach(item -> varrer(item, achados));
        }
    }

}
