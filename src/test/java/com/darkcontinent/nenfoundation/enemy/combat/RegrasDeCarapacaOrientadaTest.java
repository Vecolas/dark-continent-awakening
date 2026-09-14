package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da carapaca orientada: o arco de frente, o degrau do flanco e o ventre.
 *
 * <p>Todos os defeitos que ele segura tem a mesma assinatura -- compilam,
 * spawnam, atacam e passam em todo o resto. O bicho continua sendo um mob de
 * armadura 8; ele so deixa de ser um quebra-cabeca e vira um atraso.</p>
 */
class RegrasDeCarapacaOrientadaTest {

    /** As mesmas fracoes do Crab Heavy, escritas aqui como CASO e nao como fonte. */
    private static RegrasDeCarapacaOrientada regras() {
        return new RegrasDeCarapacaOrientada(0.45D, -0.35D, 1.0F, 0.55F, 0.15F);
    }

    /** Cosseno de um atacante a `graus` da frente do corpo. */
    private static double aGraus(double graus) {
        return Math.cos(Math.toRadians(graus));
    }

    // ------------------------------------------------------- o caso normal

    @Test
    @DisplayName("de frente a placa vale inteira; pelas costas quase nada sobra dela")
    void aPlacaTemLado() {
        RegrasDeCarapacaOrientada carapaca = regras();

        assertEquals(FaceDaCarapaca.FRENTE, carapaca.faceAtingida(aGraus(0.0D)));
        assertEquals(FaceDaCarapaca.FRENTE, carapaca.faceAtingida(aGraus(60.0D)));
        assertEquals(FaceDaCarapaca.FLANCO, carapaca.faceAtingida(aGraus(90.0D)));
        assertEquals(FaceDaCarapaca.VENTRE, carapaca.faceAtingida(aGraus(180.0D)));

        assertEquals(8.0F, carapaca.placaEfetiva(8.0F, aGraus(0.0D)), 1.0E-4F,
                "a armadura da ficha tem de chegar inteira em quem bate de frente: qualquer"
                        + " desconto aqui e um numero de perfil que o mob nao cumpre");
        assertTrue(carapaca.placaEfetiva(8.0F, aGraus(180.0D)) < 2.0F,
                "pelas costas tem de sobrar quase nada da placa. Sobrando muito, contornar deixa"
                        + " de pagar e o encontro inteiro -- que e sobre contornar -- vira um saco"
                        + " de pancada com armadura 8");
    }

    @Test
    @DisplayName("o degrau do flanco fica ENTRE os dois, porque e ele que ensina")
    void oFlancoEUmDegrauEIntermediario() {
        RegrasDeCarapacaOrientada carapaca = regras();
        float frente = carapaca.placaEfetiva(8.0F, aGraus(0.0D));
        float flanco = carapaca.placaEfetiva(8.0F, aGraus(90.0D));
        float costas = carapaca.placaEfetiva(8.0F, aGraus(180.0D));

        assertTrue(frente > flanco && flanco > costas,
                "sem degrau intermediario o jogador que contornou pela metade recebe a mesma"
                        + " recusa que recebia de frente, conclui que contornar nao serve e volta"
                        + " a bater na placa -- com a regra funcionando perfeitamente e nunca"
                        + " tendo sido descoberta. Medido: frente=" + frente + " flanco=" + flanco
                        + " costas=" + costas);
    }

    // ----------------------------------------------------- o caso recusado

    @Test
    @DisplayName("cosseno nao finito e RECUSADO, e nao tratado como pelas costas")
    void cossenoInvalidoRecusaComMotivo() {
        RegrasDeCarapacaOrientada carapaca = regras();
        // NaN perde TODA comparacao, entao os dois `if` do resolver falhariam e a
        // resposta cairia em VENTRE -- entregando o bicho a quem batesse de
        // qualquer angulo, sem uma linha de log.
        assertThrows(IllegalArgumentException.class, () -> carapaca.faceAtingida(Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> carapaca.faceAtingida(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> carapaca.placaEfetiva(Float.NaN, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> carapaca.placaEfetiva(-1.0F, 0.0D));
    }

    // ------------------------------------- os casos que DEVEM ser reprovados
    // Cada um destes e uma carapaca que compila, roda, spawna e ensina o
    // contrario do que a regra cobra. Se algum deles passar a ser aceito, esta
    // regua virou carimbo.

    @Test
    @DisplayName("REPROVA: costas protegidas tanto quanto a frente")
    void costasIguaisAFrenteDevemReprovar() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeCarapacaOrientada(0.45D, -0.35D, 1.0F, 1.0F, 1.0F),
                "uma carapaca que vale igual em volta do bicho inteiro nao levanta erro em jogo:"
                        + " ela so faz o jogador circular, nao sentir diferenca nenhuma e desistir"
                        + " de circular");
    }

    @Test
    @DisplayName("REPROVA: flanco fora da ordem frente > flanco > costas")
    void flancoForaDaOrdemDeveReprovar() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeCarapacaOrientada(0.45D, -0.35D, 1.0F, 0.05F, 0.5F),
                "flanco mais fraco que as costas ensina o caminho errado: o jogador aprende a"
                        + " parar do lado, e o servidor pune justamente quem fez o que o bicho"
                        + " ensinou");
    }

    @Test
    @DisplayName("REPROVA: arco de flanco vazio")
    void arcoDeFlancoVazioDeveReprovar() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeCarapacaOrientada(-0.5D, 0.5D, 1.0F, 0.55F, 0.15F),
                "com o limiar do flanco acima do da frente nao existe faixa intermediaria, e o"
                        + " degrau que ensina desaparece sem que nada reprove");
    }

    @Test
    @DisplayName("REPROVA: fracao de placa acima de 1")
    void placaAcimaDeUmDeveReprovar() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeCarapacaOrientada(0.45D, -0.35D, 1.4F, 0.55F, 0.15F),
                "fracao acima de 1 inventa armadura que o perfil nao declara, e o numero do"
                        + " perfil vira um botao morto numa sessao de balanceamento");
    }
}
