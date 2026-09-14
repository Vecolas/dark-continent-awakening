package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.darkcontinent.nenfoundation.Repo;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao que liga o MODELO DESENHADO do Bat Scout as regras do servidor.
 *
 * <p><b>Por que ele existe do lado de ca tambem.</b> Os geradores em
 * {@code art-source/enemies/bat_scout/} ja cobram estas mesmas medidas, e cobram
 * melhor -- eles conhecem a tabela de caixas e as curvas. Mas eles so rodam quando
 * alguem os roda, e o {@code .geo.json} e o {@code .animation.json} sao arquivos
 * VERSIONADOS: quem editar um deles a mao, ou quem mexer em {@link BatScoutTuning}
 * sem reabrir os geradores, nao encontra regua nenhuma. Este teste roda em
 * {@code ./gradlew build}, que e a unica coisa que sempre acontece.</p>
 *
 * <p><b>O defeito que ele pega nao da erro.</b> Uma caixa de mordida mais longa do
 * que o focinho desenhado mais a arremetida acerta quem, na tela, esta fora do
 * alcance -- e num bicho de 3 de dano ninguem associa a mordida invisivel a nada,
 * porque a morte nunca vem dela. Uma asa que nao abre no clipe de voo deixa o
 * morcego pairando com a asa fechada, e asa fechada no ar le como bicho caindo.
 * Nos dois casos o mob nasce, voa, relata, dropa loot e passa em todos os outros
 * portoes.</p>
 */
class BatScoutDesenhoEhRegraTest {

    private static final String MOB = "bat_scout";
    private static final String GEO =
            "src/main/resources/assets/nenfoundation/geo/entity/" + MOB + ".geo.json";
    private static final String ANIM =
            "src/main/resources/assets/nenfoundation/animations/entity/" + MOB + ".animation.json";

    private static final double PX_POR_BLOCO = 16.0D;

    /** Copiado do literal de EnemyEntityTypes: .sized(0.8F, 0.9F). */
    private static final double LARGURA_DA_HITBOX_EM_BLOCOS = 0.8D;

    /** Os mesmos limiares de {@code bat_scout_animacoes.py}, em graus. */
    private static final double ASA_DOBRADA_MAXIMA = 25.0D;
    private static final double ASA_ABERTA_MINIMA = 60.0D;

    @Test
    @DisplayName("focinho desenhado + arremetida do clipe cobrem a caixa da mordida")
    void aMordidaNaoAlcancaAlemDoQueODesenhoPromete() {
        // -Z e a frente da GEOMETRIA; a caixa do servidor mede em +Z. O focinho e,
        // portanto, o quanto a cabeca avanca no z NEGATIVO do modelo.
        double focinhoPx = -menorZ(cubo("head"));
        double arremetidaPx = -menorPositionZ("strike", "body");
        double alcanceDesenhado = (focinhoPx + arremetidaPx) / PX_POR_BLOCO;

        assertTrue(focinhoPx > 0.0D,
                "a cabeca nao avanca para a frente do corpo: o alcance da mordida passaria a ser"
                        + " prometido por uma peca que nao morde");
        assertTrue(arremetidaPx > 0.0D,
                "o clipe 'strike' nao avanca o corpo: sem arremetida o alcance vira so o focinho"
                        + " desenhado, e a caixa do servidor acerta de onde nao ha bicho");
        assertTrue(BatScoutTuning.ALCANCE_DA_MORDIDA <= alcanceDesenhado,
                "a caixa de golpe vai ate " + BatScoutTuning.ALCANCE_DA_MORDIDA + " blocos e o"
                        + " desenho alcanca " + alcanceDesenhado + " (focinho " + focinhoPx
                        + " px + arremetida " + arremetidaPx + " px): o jogador levaria a mordida"
                        + " de um morcego que, na tela, parou antes dele. Isso nao levanta excecao"
                        + " -- da um mob com alcance invisivel, e num bicho de 3 de dano ninguem"
                        + " sequer associa as duas coisas.");
    }

    @Test
    @DisplayName("a asa e a silhueta: dobrada cabe na hitbox, aberta nao cabe")
    void aAsaAbertaNaoCabeNaCaixaDeColisao() {
        double ombroX = pivot("wing_left").get(0).getAsDouble();
        double ombroY = pivot("wing_left").get(1).getAsDouble();
        double peDaPonta = cubo("wing_left_tip").getAsJsonArray("origin").get(1).getAsDouble();
        double envergaduraPx = 2.0D * (ombroX + (ombroY - peDaPonta));
        double hitboxPx = LARGURA_DA_HITBOX_EM_BLOCOS * PX_POR_BLOCO;

        assertTrue(envergaduraPx > hitboxPx,
                "a asa aberta alcanca " + envergaduraPx + " px e a hitbox tem " + hitboxPx
                        + " px: se ela COUBESSE na caixa de colisao, ela nao seria a silhueta"
                        + " deste bicho, e nada impediria alguem de desenha-la aberta como pose"
                        + " de repouso -- um morcego permanentemente de asas abertas, que o"
                        + " jogador le como bug de animacao.");
    }

    @Test
    @DisplayName("o degrau entre pousado e voando existe, e ele se mede em graus")
    void oClipeDeVooAbreAAsaEOClipeDeOcioNaoAbre() {
        double dobrada = maiorRotacaoZ("idle", "wing_left");
        assertTrue(dobrada <= ASA_DOBRADA_MAXIMA,
                "o clipe 'idle' abre a asa ate " + dobrada + " graus e o teto de asa dobrada e "
                        + ASA_DOBRADA_MAXIMA + ": pousado com a asa aberta, o morcego perde o"
                        + " degrau que diz 'ele saiu voando'");

        for (String clipe : new String[] {"walk", "windup", "strike"}) {
            double aberta = maiorRotacaoZ(clipe, "wing_left");
            assertTrue(aberta >= ASA_ABERTA_MINIMA,
                    "o clipe '" + clipe + "' abre a asa so ate " + aberta + " graus e o minimo de"
                            + " asa aberta e " + ASA_ABERTA_MINIMA + ": a silhueta de voo fica"
                            + " igual a de pouso, e a locomocao deste bicho deixa de ler como"
                            + " voo -- sem que nada no jogo acuse");
        }
    }

    @Test
    @DisplayName("a asa direita e o espelho da esquerda, e nao uma curva escrita a mao")
    void asDuasAsasBatemJuntas() {
        for (String clipe : new String[] {"idle", "walk", "windup", "strike", "recovery",
                "stagger", "death"}) {
            Map<String, JsonElement> esquerda = quadros(clipe, "wing_left", "rotation");
            Map<String, JsonElement> direita = quadros(clipe, "wing_right", "rotation");
            assertEquals(esquerda.keySet(), direita.keySet(),
                    "no clipe '" + clipe + "' as duas asas tem de ter os MESMOS instantes: chaves"
                            + " diferentes sao uma asa escrita a mao, e escrita a mao ela fica"
                            + " meio quadro atras na primeira correcao da outra");
            for (String instante : esquerda.keySet()) {
                double e = esquerda.get(instante).getAsJsonArray().get(2).getAsDouble();
                double d = direita.get(instante).getAsJsonArray().get(2).getAsDouble();
                assertEquals(-e, d, 1.0E-6D,
                        "no clipe '" + clipe + "', instante " + instante + ": a asa direita vale "
                                + d + " e devia ser o espelho de " + e + ". Assimetria aqui nao"
                                + " da erro -- da um morcego que bate uma asa mais que a outra,"
                                + " e ninguem consegue descrever isso.");
            }
        }
    }

    // ------------------------------------------------------------ utilitario

    private static JsonObject geometria() {
        return JsonParser.parseString(Repo.texto(GEO)).getAsJsonObject()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
    }

    private static JsonObject animacoes() {
        return JsonParser.parseString(Repo.texto(ANIM)).getAsJsonObject()
                .getAsJsonObject("animations");
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
                fail("o osso '" + osso + "' do " + MOB + ".geo.json devia ter exatamente um cubo e"
                        + " tem " + (cubos == null ? 0 : cubos.size()) + ": esta bateria mediria a"
                        + " peca errada e continuaria verde.");
            }
            return cubos.get(0).getAsJsonObject();
        }
        return fail(MOB + ".geo.json nao tem o osso '" + osso + "'. O modelo mudou e as reguas que"
                + " ligam o desenho a regra pararam de medir qualquer coisa.");
    }

    private static JsonArray pivot(String osso) {
        for (JsonElement elemento : geometria().getAsJsonArray("bones")) {
            JsonObject b = elemento.getAsJsonObject();
            if (osso.equals(b.get("name").getAsString())) return b.getAsJsonArray("pivot");
        }
        return fail(MOB + ".geo.json nao tem o osso '" + osso + "'.");
    }

    private static double menorZ(JsonObject cubo) {
        return cubo.getAsJsonArray("origin").get(2).getAsDouble();
    }

    private static Map<String, JsonElement> quadros(String clipe, String osso, String canal) {
        String completo = "animation." + MOB + "." + clipe;
        JsonObject animacoes = animacoes();
        if (!animacoes.has(completo)) {
            return fail(MOB + ".animation.json nao tem o clipe '" + completo + "', que o codigo"
                    + " cita. O GeckoLib nao acha o clipe e a entidade fica na pose neutra.");
        }
        JsonObject ossos = animacoes.getAsJsonObject(completo).getAsJsonObject("bones");
        if (ossos == null || !ossos.has(osso) || !ossos.getAsJsonObject(osso).has(canal)) {
            return fail("o clipe '" + completo + "' nao move o canal " + canal + " de '" + osso
                    + "'. Este bicho E a asa dele: um clipe que a deixa parada mostra um morcego"
                    + " congelado enquanto o corpo inteiro se mexe.");
        }
        return ossos.getAsJsonObject(osso).getAsJsonObject(canal).asMap();
    }

    private static double maiorRotacaoZ(String clipe, String osso) {
        double maior = 0.0D;
        for (JsonElement valor : quadros(clipe, osso, "rotation").values()) {
            maior = Math.max(maior, Math.abs(valor.getAsJsonArray().get(2).getAsDouble()));
        }
        return maior;
    }

    private static double menorPositionZ(String clipe, String osso) {
        double menor = 0.0D;
        for (JsonElement valor : quadros(clipe, osso, "position").values()) {
            menor = Math.min(menor, valor.getAsJsonArray().get(2).getAsDouble());
        }
        return menor;
    }
}
