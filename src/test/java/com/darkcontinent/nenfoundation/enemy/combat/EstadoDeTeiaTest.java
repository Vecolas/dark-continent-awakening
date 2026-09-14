package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sustenta que quem entra na teia SAI dela -- pelas cinco portas, e por nenhuma outra.
 *
 * <p>Este portao existe por uma falha que nao levanta excecao e que custa o
 * servidor: uma saida esquecida deixa um jogador imobilizado ate o restart. Nada
 * no log aponta para a aranha, porque a aranha ja morreu; o que chega e "eu
 * travei".</p>
 */
class EstadoDeTeiaTest {

    private static final RegrasDeTeia REGRAS = new RegrasDeTeia(3.5D, 9.0D, 60, 8.0F);
    private static final UUID PRESA = UUID.nameUUIDFromBytes("teia-presa".getBytes());
    private static final UUID OUTRA = UUID.nameUUIDFromBytes("teia-outra".getBytes());

    private static EstadoDeTeia presa() {
        EstadoDeTeia teia = new EstadoDeTeia(REGRAS);
        teia.prender(PRESA);
        return teia;
    }

    /** Avanca n ticks com presa e predador vivos; devolve a primeira saida que aparecer. */
    private static SolturaDaTeia avancar(EstadoDeTeia teia, int ticks) {
        for (int i = 0; i < ticks; i++) {
            SolturaDaTeia saida = teia.tick(true, true);
            if (saida != SolturaDaTeia.NENHUMA) return saida;
        }
        return SolturaDaTeia.NENHUMA;
    }

    // ------------------------------------------------------------ caso normal

    @Test
    @DisplayName("prende, segura o prazo inteiro, e solta por TEMPO")
    void oCasoNormal() {
        EstadoDeTeia teia = presa();
        assertTrue(teia.prendendo());
        assertEquals(PRESA, teia.presa().orElseThrow());
        assertEquals(60, teia.ticksRestantes());

        assertEquals(SolturaDaTeia.NENHUMA, avancar(teia, 59),
                "cinquenta e nove ticks ainda seguram: a saida por tempo e a ULTIMA, e nao a"
                        + " primeira -- adiantar um tick encurtaria a janela do esquadrao");
        assertEquals(SolturaDaTeia.TEMPO_ESGOTADO, teia.tick(true, true));

        assertFalse(teia.prendendo(), "soltar LIMPA: um relogio correndo sem ninguem dentro"
                + " dispararia a proxima soltura antes da proxima teia existir");
        assertEquals(0, teia.ticksRestantes());
        assertEquals(0.0F, teia.danoNaFiandeira(), 0.0F);
        assertTrue(teia.presa().isEmpty());
    }

    @Test
    @DisplayName("teia vazia nao tem o que ticar, e isso nao e erro")
    void semPresaOTickENeutro() {
        EstadoDeTeia teia = new EstadoDeTeia(REGRAS);
        assertEquals(SolturaDaTeia.NENHUMA, teia.tick(false, false),
                "sem ninguem preso, nem a morte do predador e uma soltura: soltar o vazio"
                        + " mandaria um aviso de libertacao para quem nunca foi pego");
        assertFalse(teia.prendendo());
    }

    // ------------------------------------------------------------ as saidas

    @Test
    @DisplayName("a SAIDA ATIVA: acertar a fiandeira rasga o fio no MESMO tick")
    void danoNaFiandeiraSoltaAntesDoPrazo() {
        EstadoDeTeia teia = presa();
        avancar(teia, 10);

        teia.registrarDanoNaFiandeira(5.0F);
        assertEquals(SolturaDaTeia.NENHUMA, teia.tick(true, true),
                "cinco de oito ainda nao rasgam: o limiar existe para que UM respingo nao solte");
        teia.registrarDanoNaFiandeira(3.0F);
        assertEquals(SolturaDaTeia.FIO_ROMPIDO, teia.tick(true, true),
                "o golpe que chega ao limiar solta no tick em que chegou -- 'bati e nao soltou'"
                        + " e o relato que faz o jogador parar de tentar");
        assertFalse(teia.prendendo());
    }

    @Test
    @DisplayName("o dano de uma briga anterior nao solta a proxima presa")
    void oAcumuladoNaoAtravessaDuasTeias() {
        EstadoDeTeia teia = new EstadoDeTeia(REGRAS);
        // Apanhar SEM ninguem preso nao acumula nada: somar sempre faria a proxima
        // vitima ser solta instantaneamente por causa de uma briga anterior, e o
        // sintoma seria uma teia que "as vezes nao pega".
        teia.registrarDanoNaFiandeira(50.0F);
        assertEquals(0.0F, teia.danoNaFiandeira(), 0.0F);

        teia.prender(PRESA);
        assertEquals(0.0F, teia.danoNaFiandeira(), 0.0F);
        assertEquals(SolturaDaTeia.NENHUMA, teia.tick(true, true));
        assertTrue(teia.prendendo());
    }

    @Test
    @DisplayName("A SAIDA QUE MAIS CUSTA SE FALTAR: a aranha cai e a presa sai junto")
    void predadorMortoSoltaAPresa() {
        EstadoDeTeia teia = presa();
        avancar(teia, 5);
        assertEquals(SolturaDaTeia.PREDADOR_CAIU, teia.tick(true, false),
                "sem esta saida, matar a aranha no tick em que ela segura alguem deixaria a"
                        + " vitima presa ate o restart -- sem excecao, sem log");
        assertFalse(teia.prendendo());
    }

    @Test
    @DisplayName("presa que some sai da teia antes de qualquer outra pergunta")
    void presaSumidaEncerraAntesDoDanoEDoPrazo() {
        EstadoDeTeia teia = presa();
        teia.registrarDanoNaFiandeira(100.0F);
        assertEquals(SolturaDaTeia.PRESA_SUMIU, teia.tick(false, true),
                "presa e predador vem antes do dano e do relogio: um dos dois ter sumido invalida"
                        + " as outras duas perguntas");
        assertFalse(teia.prendendo());
    }

    @Test
    @DisplayName("limpeza simetrica: os tres campos saem juntos")
    void limparApagaOEstadoInteiro() {
        EstadoDeTeia teia = presa();
        teia.registrarDanoNaFiandeira(7.0F);
        avancar(teia, 3);
        teia.limpar();

        assertFalse(teia.prendendo());
        assertEquals(0, teia.ticksRestantes());
        assertEquals(0.0F, teia.danoNaFiandeira(), 0.0F,
                "zerar so o relogio deixaria o dano pronto para rasgar a teia SEGUINTE antes de"
                        + " ela existir");
    }

    // ------------------------------------------------- os casos que DEVEM reprovar

    @Test
    @DisplayName("O CASO QUE DEVE REPROVAR: a segunda teia orfaniza o relogio da primeira")
    void prenderDuasVezesERecusado() {
        EstadoDeTeia teia = presa();
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> teia.prender(OUTRA));
        assertTrue(erro.getMessage().contains("sem prazo"),
                "a recusa tem de dizer o que acontece com a PRIMEIRA vitima; veio: "
                        + erro.getMessage());
        assertEquals(PRESA, teia.presa().orElseThrow(),
                "a recusa nao pode ter trocado a presa pelo caminho");
    }

    @Test
    @DisplayName("O CASO QUE DEVE REPROVAR: um NaN somado contamina o acumulado para sempre")
    void danoInvalidoERecusado() {
        EstadoDeTeia teia = presa();
        assertThrows(IllegalArgumentException.class,
                () -> teia.registrarDanoNaFiandeira(Float.NaN),
                "NaN >= limiar e sempre falso: a saida por dano existiria no papel e a presa"
                        + " so sairia pelo relogio, sem nada acusar");
        assertThrows(IllegalArgumentException.class,
                () -> teia.registrarDanoNaFiandeira(-3.0F),
                "dano negativo DESFARIA o progresso de quem esta batendo");
        assertEquals(0.0F, teia.danoNaFiandeira(), 0.0F);
    }

    @Test
    @DisplayName("O CASO QUE DEVE REPROVAR: teia sem regras nao tem prazo nem limiar")
    void regrasAusentesSaoRecusadas() {
        NullPointerException erro = assertThrows(NullPointerException.class,
                () -> new EstadoDeTeia(null));
        assertTrue(erro.getMessage().contains("saidas"),
                "a recusa tem de dizer que a presa ficaria sem nenhuma das duas saidas; veio: "
                        + erro.getMessage());
    }
}
