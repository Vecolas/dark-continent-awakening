package com.darkcontinent.nenfoundation.client.vfx.debug;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O texto do overlay de tuning.
 *
 * <p>Duas coisas aqui sao caras de errar, e nenhuma das duas da erro.
 *
 * <p>A primeira e escrever ZERO onde ainda nao existe consumidor: um
 * "bloom: 0" faria o overlay afirmar que o passe de pos-processamento rodou e
 * nao custou nada -- e essa afirmacao sobreviveria ate alguem medir.
 *
 * <p>A segunda e nao gritar quando ha sobreposicao ativa. Uma captura tirada
 * com um slider fora do perfil e aprovada como se fosse o jogo, e o jogo que os
 * jogadores veem nunca foi aquele.
 */
class AuraDebugRendererTest {

    private static AuraDebugRenderer.Dados semNada() {
        return new AuraDebugRenderer.Dados(true, false, null, Float.NaN, Float.NaN, null, -1,
                0, 0, 0, 0, false, null, "33ded12", null);
    }

    private static String juntar(List<String> linhas) {
        return String.join("\n", linhas);
    }

    @Test
    @DisplayName("campo sem consumidor aparece como traco, e nunca como zero")
    void semConsumidorNaoEZero() {
        String texto = juntar(AuraDebugRenderer.linhas(semNada()));
        assertTrue(texto.contains("alvo de bloom: " + AuraDebugRenderer.SEM_CONSUMIDOR),
                "o passe de bloom nasce no AV5; escrever um numero aqui seria mentir\n" + texto);
        assertTrue(texto.contains("visibilidade: " + AuraDebugRenderer.SEM_CONSUMIDOR),
                "o resolvedor de visibilidade nasce no AV6\n" + texto);
    }

    @Test
    @DisplayName("sem sobreposicao, o overlay diz que a tela e o perfil")
    void semSobreposicaoNaoGrita() {
        String texto = juntar(AuraDebugRenderer.linhas(semNada()));
        assertFalse(texto.contains("OVERRIDE ATIVO"), texto);
        assertTrue(texto.contains("sem sobreposicao"), texto);
    }

    @Test
    @DisplayName("qualquer sobreposicao acende o aviso de que a captura nao vale")
    void qualquerSobreposicaoGrita() {
        AuraDebugRenderer.Dados so1Slider = new AuraDebugRenderer.Dados(
                true, false, null, Float.NaN, Float.NaN, null, -1,
                0, 0, 0, 0, false, null, "33ded12", "alpha_borda=0.400");
        String texto = juntar(AuraDebugRenderer.linhas(so1Slider));

        assertTrue(texto.contains("OVERRIDE ATIVO"),
                "um unico slider fora do perfil ja invalida a captura como aprovacao\n" + texto);
        assertTrue(texto.contains("alpha_borda=0.400"), texto);
    }

    @Test
    @DisplayName("desenho desligado tambem conta como sobreposicao")
    void desenhoDesligadoGrita() {
        AuraDebugRenderer.Dados desligado = new AuraDebugRenderer.Dados(
                false, false, null, Float.NaN, Float.NaN, null, -1,
                0, 0, 0, 0, false, null, "33ded12", null);
        String texto = juntar(AuraDebugRenderer.linhas(desligado));
        assertTrue(texto.contains("OVERRIDE ATIVO"), texto);
        assertTrue(texto.contains("desenho=off"), texto);
    }

    @Test
    @DisplayName("o custo do quadro aparece sempre, inclusive quando e zero")
    void custoAparece() {
        AuraDebugRenderer.Dados comCusto = new AuraDebugRenderer.Dados(
                true, false, null, Float.NaN, Float.NaN, null, -1,
                6, 48, 12, 2, false, null, "33ded12", null);
        String texto = juntar(AuraDebugRenderer.linhas(comCusto));
        assertTrue(texto.contains("6 chamadas"), texto);
        assertTrue(texto.contains("48 filamentos"), texto);
        assertTrue(texto.contains("12 particulas"), texto);
        assertTrue(texto.contains("2 com aura"), texto);
    }

    @Test
    @DisplayName("o commit do build aparece: uma captura sem ele nao se liga a codigo nenhum")
    void mostraOCommit() {
        assertTrue(juntar(AuraDebugRenderer.linhas(semNada())).contains("33ded12"));
    }
}
