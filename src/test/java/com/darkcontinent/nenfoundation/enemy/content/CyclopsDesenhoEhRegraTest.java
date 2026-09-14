package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.darkcontinent.nenfoundation.Repo;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao que liga o MODELO DESENHADO do Cyclops as regras do servidor.
 *
 * <p><b>Por que ele existe do lado de ca tambem.</b> Os geradores em
 * {@code art-source/enemies/cyclops/} ja cobram estas mesmas medidas, e cobram
 * melhor -- eles conhecem a tabela de caixas. Mas eles so rodam quando alguem os
 * roda, e o {@code .geo.json} e um arquivo VERSIONADO: quem editar o modelo a
 * mao, ou quem mexer em {@link CyclopsTuning} sem reabrir o gerador, nao
 * encontra regua nenhuma. Este teste roda em {@code ./gradlew build}, que e a
 * unica coisa que sempre acontece.</p>
 *
 * <p><b>O defeito que ele pega nao da erro.</b> Um olho desenhado abaixo do
 * limiar do {@code WeakPointResolver} promete um critico que a regra recusa; uma
 * caixa de golpe mais longa que o porrete desenhado acerta quem, na tela, esta
 * fora do alcance da arma. Nos dois casos o mob nasce, ataca, dropa loot e passa
 * em todos os outros portoes -- e o jogador aprende uma geometria que o jogo nao
 * cumpre.</p>
 */
class CyclopsDesenhoEhRegraTest {

    private static final String GEO = "src/main/resources/assets/nenfoundation/geo/entity/cyclops.geo.json";

    /** Copiado do literal de EnemyEntityTypes: .sized(1.8F, 4.2F). */
    private static final double ALTURA_DA_HITBOX_EM_BLOCOS = 4.2D;
    private static final double PX_POR_BLOCO = 16.0D;

    @Test
    @DisplayName("o olho desenhado fica acima do limiar que o servidor chama de ponto fraco")
    void oOlhoDesenhadoCaiNaFaixaDoPontoFraco() {
        JsonObject olho = cubo("eye");
        double baseDoOlhoEmPx = olho.getAsJsonArray("origin").get(1).getAsDouble();
        double limiarEmPx = CyclopsTuning.ALTURA_MINIMA_DO_OLHO
                * ALTURA_DA_HITBOX_EM_BLOCOS * PX_POR_BLOCO;

        assertTrue(baseDoOlhoEmPx >= limiarEmPx,
                "o olho desenhado comeca em y=" + baseDoOlhoEmPx + " px e o servidor so paga ponto"
                        + " fraco acima de " + limiarEmPx + " px (ALTURA_MINIMA_DO_OLHO "
                        + CyclopsTuning.ALTURA_MINIMA_DO_OLHO + "). Desenhado abaixo do limiar, o"
                        + " olho promete um critico que a regra recusa, e nao ha erro nenhum para"
                        + " procurar.");

        // E tem de sobrar corpo comum abaixo: sem isso todo golpe viraria critico
        // e o ponto fraco deixaria de ser um ponto.
        double baseDoTorsoEmPx = cubo("torso").getAsJsonArray("origin").get(1).getAsDouble();
        assertTrue(baseDoTorsoEmPx < limiarEmPx,
                "o torso comeca em y=" + baseDoTorsoEmPx + " px, acima do limiar de " + limiarEmPx
                        + ": nao sobraria regiao de corpo comum.");
    }

    @Test
    @DisplayName("o olho unico fica centrado no eixo, que e onde o cone frontal mira")
    void oOlhoFicaNoEixoDoCone() {
        JsonObject olho = cubo("eye");
        double x0 = olho.getAsJsonArray("origin").get(0).getAsDouble();
        double largura = olho.getAsJsonArray("size").get(0).getAsDouble();

        assertEquals(0.0D, x0 + largura / 2.0D, 1.0E-9D,
                "o cone de ponto fraco do servidor e SIMETRICO em torno do olhar. Um olho fora do"
                        + " eixo faz o desenho apontar um lado e a regra pagar o outro -- o jogador"
                        + " mira no olho, leva dano comum, e nao ha o que procurar.");
    }

    @Test
    @DisplayName("a caixa do golpe nao chega mais longe do que o porrete desenhado")
    void oAlcanceDaCaixaCabeNoPorreteDesenhado() {
        double ombroEmPx = pivot("arm_right").get(1).getAsDouble();
        double pontaDoPorreteEmPx = cubo("club").getAsJsonArray("origin").get(1).getAsDouble();
        double alcanceDesenhadoEmBlocos = (ombroEmPx - pontaDoPorreteEmPx) / PX_POR_BLOCO;
        double alcanceDaCaixa = CyclopsTuning.caixaDoPorrete().maxZ();

        assertTrue(alcanceDaCaixa <= alcanceDesenhadoEmBlocos,
                "a caixa de golpe vai ate " + alcanceDaCaixa + " blocos e o porrete desenhado"
                        + " alcanca " + alcanceDesenhadoEmBlocos + " a partir do ombro: o jogador"
                        + " apanharia de uma tora que, na tela, parou antes dele. Isso nao levanta"
                        + " excecao -- da um mob com alcance invisivel, que e a reclamacao mais"
                        + " dificil de diagnosticar num corpo-a-corpo.");
    }

    // ------------------------------------------------------------ utilitario

    private static JsonObject geometria() {
        return JsonParser.parseString(Repo.texto(GEO)).getAsJsonObject()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
    }

    /**
     * O UNICO cubo de um osso.
     *
     * <p>Falhar quando o osso nao existe, ou quando ele tem mais de um cubo, e
     * deliberado: devolver um cubo "qualquer" faria esta bateria medir uma peca
     * diferente da que o nome diz, e o verde nao significaria mais nada.</p>
     */
    private static JsonObject cubo(String osso) {
        for (JsonElement elemento : geometria().getAsJsonArray("bones")) {
            JsonObject b = elemento.getAsJsonObject();
            if (!osso.equals(b.get("name").getAsString())) continue;
            JsonArray cubos = b.getAsJsonArray("cubes");
            if (cubos == null || cubos.size() != 1) {
                fail("o osso '" + osso + "' do cyclops.geo.json devia ter exatamente um cubo e tem "
                        + (cubos == null ? 0 : cubos.size()) + ": esta bateria mediria a peca"
                        + " errada e continuaria verde.");
            }
            return cubos.get(0).getAsJsonObject();
        }
        return fail("cyclops.geo.json nao tem o osso '" + osso + "'. O modelo mudou e as reguas que"
                + " ligam o desenho a regra pararam de medir qualquer coisa.");
    }

    private static JsonArray pivot(String osso) {
        for (JsonElement elemento : geometria().getAsJsonArray("bones")) {
            JsonObject b = elemento.getAsJsonObject();
            if (osso.equals(b.get("name").getAsString())) return b.getAsJsonArray("pivot");
        }
        return fail("cyclops.geo.json nao tem o osso '" + osso + "'.");
    }
}
