package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da FORCA DO BRILHO: ela e aplicada UMA vez, e no lugar certo.
 *
 * <p>POR QUE ELE EXISTE. Ate 2026-09-22 a forca do perfil entrava duas vezes no
 * caminho do halo: uma por jogador, na escrita do alvo de brilho
 * ({@code AuraPlayerRenderLayer}, {@code alphaDoPasse * forcaDoBrilho}), e outra
 * no composite de tela inteira ({@code AuraBloomRenderer}, {@code maiorForca *
 * intensidadeDoBloom()}).
 *
 * <p><b>A segunda vez usava o valor de OUTRA PESSOA</b>, porque {@code
 * maiorForca} e o maximo da tela. A conta, com um Ten na tela:
 *
 * <pre>
 *   observador em Ten -> 0,20 (escrita) x 0,20 (composite) = 0,040
 *   observador em Ren -> 0,20 (escrita) x 0,55 (composite) = 0,110
 * </pre>
 *
 * <p>O Ten de um terceiro ficava <b>2,75x mais claro porque quem olhava trocou
 * de tecnica</b>. Nao lancava, nao aparecia em teste nenhum, e o build ficava
 * verde: o relato chegou de olho humano, em jogo -- <i>"ativar Ren deixa a
 * tecnica dos outros mais clara"</i>.
 *
 * <p><b>O QUE ESTE PORTAO MEDE, e por que assim.</b> A conta de verdade acontece
 * na GPU, e nenhum teste unitario a alcanca. O que da para medir e a FONTE: que
 * {@code AuraBloomRenderer} nao multiplique o peso por {@code maiorForca}, e que
 * {@code AuraPlayerRenderLayer} continue aplicando a forca por jogador. As duas
 * metades importam -- tirar a de baixo apagaria a diferenca entre Ten e Ren no
 * halo, que e o oposto do defeito e igualmente invisivel.
 *
 * <p><b>PONTO CEGO DECLARADO.</b> Ele le TEXTO. Uma terceira multiplicacao,
 * escrita de outro jeito ou noutro arquivo, passa. E ele nao prova que o halo
 * ficou certo NA TELA -- so a sessao de captura mostra, e foi ela que achou o
 * defeito que este portao agora tranca.
 */
class ForcaDoBrilhoTest {

    private static final String BLOOM =
            "src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/AuraBloomRenderer.java";
    private static final String LAYER =
            "src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/AuraPlayerRenderLayer.java";

    /** Os dois perfis que o defeito misturava, como o dado os declara. */
    private static final String TEN = "src/main/resources/assets/nenfoundation/nen_vfx/ten.json";
    private static final String REN = "src/main/resources/assets/nenfoundation/nen_vfx/ren.json";

    @Test
    @DisplayName("o composite NAO multiplica o peso pela forca da aura mais forte da tela")
    void oCompositeNaoReaplicaAForca() {
        String fonte = semComentarios(Repo.texto(BLOOM));

        assertTrue(fonte.contains("intensidadeDoBloom()"),
                "O composite parou de ler o slider do jogador.");

        assertTrue(fonte.contains("ESCALA_DO_COMPOSITE"),
                "O composite perdeu a escala CONSTANTE. Sem ela o peso vira 1,0 e o halo fica"
                        + " CINCO VEZES mais forte do que o que passou no gate do AV1 -- foi"
                        + " exatamente isso que aconteceu em 22/09, e o relato veio em minutos:"
                        + " \"o Ten atravessa parede e parece flecha espectral a 50 blocos\"."
                        + " A escala nao pode vir de jogador nenhum, ou o acoplamento volta.");

        assertFalse(Pattern.compile("peso\\s*=\\s*[^;]*maiorForca").matcher(fonte).find(),
                "O peso do composite voltou a multiplicar por maiorForca. A forca do perfil JA"
                        + " entrou por jogador na escrita do alvo; aplicada de novo aqui, ela"
                        + " entra DUAS vezes -- e a segunda com o valor de outra pessoa. O"
                        + " sintoma nao e erro: e a aura de um terceiro ficando mais clara"
                        + " porque QUEM OLHA ligou Ren.");
    }

    @Test
    @DisplayName("maiorForca continua decidindo SE ha aura e qual o raio -- os dois usos legitimos")
    void maiorForcaAindaDecidePuloERaio() {
        String fonte = semComentarios(Repo.texto(BLOOM));

        assertTrue(Pattern.compile("haAura\\s*=\\s*maiorForca\\s*>").matcher(fonte).find(),
                "O predicado de pulo parou de usar maiorForca. Sem ele o passe inteiro roda com"
                        + " a tela sem aura nenhuma, e o custo ZERO que o AV8 mede vira custo"
                        + " pequeno.");
        assertTrue(fonte.contains("raioDoMaior"),
                "O raio parou de vir da aura mais forte. Um alvo compartilhado tem UM raio, e"
                        + " escolher o maior e o que impede o Ren de perder o halo ao lado de"
                        + " varios Ten.");
    }

    @Test
    @DisplayName("a forca POR JOGADOR continua na escrita do alvo -- a outra metade da conta")
    void aForcaPorJogadorContinuaNaEscrita() {
        String fonte = semComentarios(Repo.texto(LAYER));

        assertTrue(Pattern.compile("alphaDeBrilho\\s*=\\s*alphaDoPasse\\s*\\*\\s*forcaDoBrilho")
                        .matcher(fonte).find(),
                "A escrita do alvo parou de multiplicar pela forca DAQUELE jogador. Sem ela,"
                        + " Ten e Ren contribuem igual para o halo e a diferenca entre as duas"
                        + " tecnicas some -- o oposto do defeito de 22/09, e tao invisivel"
                        + " quanto: o build continua verde e a tela e que muda.");
    }

    @Test
    @DisplayName("a escala do composite recompoe o Ten que o AV1 aprovou (0,04)")
    void aEscalaRecompoeOTenAprovado() {
        var m = Pattern.compile("ESCALA_DO_COMPOSITE\s*=\s*([0-9.]+)F")
                .matcher(semComentarios(Repo.texto(BLOOM)));
        assertTrue(m.find(), "nao achei a constante ESCALA_DO_COMPOSITE");
        float escala = Float.parseFloat(m.group(1));

        // O que o olho aprovou no AV1: Ten em 0,20 (escrita) x 0,20 (composite).
        assertEquals(0.04F, forcaDeBloomEm(TEN) * escala, 1.0E-4F,
                "O Ten deixou de sair na magnitude aprovada no gate do AV1. Este numero nao e"
                        + " opiniao: e o produto que estava na tela quando alguem olhou e disse"
                        + " que passava. Mudar a escala sem olhar de novo troca um gate aprovado"
                        + " por um que ninguem julgou.");
    }

    @Test
    @DisplayName("Ren brilha mais que Ten no DADO -- senao a conta acima nao mede nada")
    void osPerfisMantemADiferenca() {
        float ten = forcaDeBloomEm(TEN);
        float ren = forcaDeBloomEm(REN);

        assertTrue(ren > ten,
                "Ren (" + ren + ") deixou de ser mais forte que Ten (" + ten + ") no perfil."
                        + " Os testes acima continuariam verdes medindo uma diferenca que nao"
                        + " existe mais -- o pior estado de uma regua.");
        assertEquals(0.20F, ten, 1.0E-4F,
                "A forca de bloom do Ten mudou. Nao e erro por si, mas a conta registrada no"
                        + " javadoc deste teste e no AuraBloomRenderer cita este numero: mude"
                        + " os dois juntos, ou o texto passa a descrever outro jogo.");
        assertEquals(0.55F, ren, 1.0E-4F, "A forca de bloom do Ren mudou; ver acima.");
    }

    @Test
    @DisplayName("a mascara de oclusao e copiada depois do CUTOUT -- grama tambem oclui")
    void aMascaraIncluiOCutout() {
        String fonte = semComentarios(Repo.texto(BLOOM));

        assertTrue(fonte.contains("Stage.AFTER_CUTOUT_BLOCKS"),
                "A copia da profundidade voltou para antes do cutout. Grama, folhas e flores"
                        + " desenham no passe de CUTOUT, depois do solido: fora da mascara, o"
                        + " halo atravessa exatamente esses blocos e nenhum outro. Foi o defeito"
                        + " de 2026-09-22, e o relato de jogo entregou a causa -- \"so atravessa"
                        + " na grama; outros blocos nao da para ver\".");

        assertFalse(fonte.contains("Stage.AFTER_SOLID_BLOCKS"),
                "Sobrou uma referencia a AFTER_SOLID_BLOCKS. Duas condicoes de estagio no mesmo"
                        + " arquivo e como uma delas fica para tras.");

        assertTrue(fonte.contains("Stage.AFTER_WEATHER"),
                "O composite deixou de rodar depois do clima. Rodar antes deixa chuva e neve"
                        + " desenhadas POR CIMA do halo.");
    }

    // ---------------------------------------------------------------- leitura

    /** O {@code "forca"} de dentro do bloco {@code "bloom"} do perfil. */
    private static float forcaDeBloomEm(String perfil) {
        var m = Pattern.compile("\"bloom\"\\s*:\\s*\\{[^}]*\"forca\"\\s*:\\s*([0-9.]+)")
                .matcher(Repo.texto(perfil));
        assertTrue(m.find(), "nao achei o bloco bloom.forca em " + perfil);
        return Float.parseFloat(m.group(1));
    }

    /**
     * Remove comentario de bloco e de linha.
     *
     * <p>A correcao ESCREVEU a conta errada no comentario, de proposito, para que
     * a proxima pessoa saiba o que nao fazer. Sem esta limpeza o portao acharia
     * `maiorForca` na explicacao e reprovaria a propria documentacao dela.
     */
    private static String semComentarios(String fonte) {
        return fonte.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("(?m)//.*$", " ");
    }
}
