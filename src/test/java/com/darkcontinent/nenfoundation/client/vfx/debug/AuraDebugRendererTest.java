package com.darkcontinent.nenfoundation.client.vfx.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.client.vfx.AuraBodyRegion;
import com.darkcontinent.nenfoundation.client.vfx.AuraDistribution;
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
                0, 0, 0, 0, 0, 0, 0, 0, false, null, "33ded12", null,
                AuraDistribution.uniforme(), null);
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
                0, 0, 0, 0, 0, 0, 0, 0, false, null, "33ded12", "alpha_borda=0.400",
                AuraDistribution.uniforme(), null);
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
                0, 0, 0, 0, 0, 0, 0, 0, false, null, "33ded12", null,
                AuraDistribution.uniforme(), null);
        String texto = juntar(AuraDebugRenderer.linhas(desligado));
        assertTrue(texto.contains("OVERRIDE ATIVO"), texto);
        assertTrue(texto.contains("desenho=off"), texto);
    }

    @Test
    @DisplayName("o custo do quadro aparece sempre, inclusive quando e zero")
    void custoAparece() {
        AuraDebugRenderer.Dados comCusto = new AuraDebugRenderer.Dados(
                true, false, null, Float.NaN, Float.NaN, null, -1,
                6, 48, 4, 1, 12, 5, 1, 2, false, null, "33ded12", null,
                AuraDistribution.uniforme(), null);
        String texto = juntar(AuraDebugRenderer.linhas(comCusto));
        assertTrue(texto.contains("6 chamadas"), texto);
        assertTrue(texto.contains("48 filamentos"), texto);
        assertTrue(texto.contains("12 faiscas ativas"), texto);
        assertTrue(texto.contains("2 com aura"), texto);
    }

    // ------------------------------------------------- os seis fatores

    private static AuraDebugRenderer.Dados com(AuraDistribution d) {
        return new AuraDebugRenderer.Dados(true, false, null, Float.NaN, Float.NaN, null, -1,
                0, 0, 0, 0, 0, 0, 0, 0, false, null, "33ded12", null, d, null);
    }

    @Test
    @DisplayName("os seis fatores aparecem, e o desequilibrio marca o pico")
    void gyoSeLeDeRelance() {
        // Gyo no braco direito: 1.8 nele, 0.7 no resto. O renderer multiplica
        // por isto desde o AV1 -- mas ate esta linha existir, o multiplicador
        // era um numero que ninguem conseguia OBSERVAR (#175).
        String texto = juntar(AuraDebugRenderer.linhas(com(
                new AuraDistribution(0.7F, 0.7F, 0.7F, 1.8F, 0.7F, 0.7F))));

        assertTrue(texto.contains("*bD 1.80"),
                "o pico tem de sair marcado, senao ler 'Gyo no braco direito' exige"
                        + " comparar seis numeros de tres casas\n" + texto);
        assertTrue(texto.contains("cab 0.70") && texto.contains("pD 0.70"), texto);
        assertFalse(texto.contains("uniforme"), texto);
    }

    @Test
    @DisplayName("distribuicao plana diz 'uniforme', em vez de seis copias do mesmo numero")
    void uniformeNaoRepete() {
        String texto = juntar(AuraDebugRenderer.linhas(com(AuraDistribution.uniforme())));
        assertTrue(texto.contains("regioes: uniforme 1.00"), texto);
        assertFalse(texto.contains("cab 1.00"),
                "uma linha que repete o mesmo texto seis vezes deixa de ser lida\n" + texto);
    }

    @Test
    @DisplayName("sem aura NAO e distribuicao de zeros -- e a distincao que este overlay guarda")
    void semAuraNaoEZero() {
        String texto = juntar(AuraDebugRenderer.linhas(com(null)));
        assertTrue(texto.contains("regioes: " + AuraDebugRenderer.SEM_CONSUMIDOR), texto);
        assertFalse(texto.contains("0.00"),
                "escrever 0.00 seis vezes confundiria 'nao ha estado' com Zetsu, que"
                        + " zera DE PROPOSITO\n" + texto);
    }

    @Test
    @DisplayName("Ko nao estoura a linha, e o pico continua unico")
    void koTemUmPicoSo() {
        // Ko: quase tudo num membro. O caso extremo do ADR-014.
        String texto = juntar(AuraDebugRenderer.linhas(com(
                new AuraDistribution(0.02F, 0.02F, 0.02F, 1.0F, 0.02F, 0.02F))));
        assertEquals(1, texto.chars().filter(c -> c == '*').count(),
                "dois picos marcados fariam a leitura de relance mentir\n" + texto);
        assertTrue(texto.contains("*bD 1.00"), texto);
    }

    @Test
    @DisplayName("ha um rotulo para CADA regiao -- uma regiao nova sem rotulo some da linha")
    void rotuloPorRegiao() {
        // SEM ESTE TESTE, acrescentar uma regiao ao enum faria a linha mostrar
        // seis de sete e nao lancar nada -- o `switch` de AuraDistribution
        // reprova no compilador, mas um vetor de rotulos nao reprova em lugar
        // nenhum.
        assertEquals(AuraBodyRegion.values().length, AuraDebugRenderer.ROTULOS.length,
                "o numero de rotulos do overlay saiu de sincronia com AuraBodyRegion");
    }

    @Test
    @DisplayName("o commit do build aparece: uma captura sem ele nao se liga a codigo nenhum")
    void mostraOCommit() {
        assertTrue(juntar(AuraDebugRenderer.linhas(semNada())).contains("33ded12"));
    }
}
