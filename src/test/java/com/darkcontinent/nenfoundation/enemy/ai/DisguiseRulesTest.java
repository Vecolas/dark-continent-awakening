package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Sustenta a INCONSISTENCIA OBSERVAVEL que e o macaco inteiro: enquanto disfarcado ele so
 * se aproxima quando ninguem esta olhando para ele.
 *
 * <p>Essa tabela e o que o jogador tem de aprender -- "nao tire os olhos dele" -- e e
 * tambem o que nenhum playtest consegue auditar: um macaco que anda sendo encarado nao
 * produz erro nenhum, produz um mob que "parece meio quebrado" e que ninguem consegue
 * explicar. Por isso a tabela mora numa fonte so, medivel sem mundo, e nao espalhada
 * pelos {@code if} da Goal.</p>
 */
class DisguiseRulesTest {
    /** Os mesmos numeros de {@code HunterExamProfiles.manFacedApeDisguise()}. */
    private static final DisguiseRules REGRAS = new DisguiseRules(3.5D, 0.6D, 12.0D, 10);

    @Test
    void semAlvoVisivelNinguemEstaEncarando() {
        assertFalse(REGRAS.observado(false, 1.0D),
                "alvo que nao ve o macaco (parede, folhagem, costas) nao o esta encarando, "
                        + "por mais alinhado que o olhar esteja no papel");
        assertFalse(REGRAS.observado(false, 0.99D));
    }

    @Test
    void olharParaOLadoNaoContaComoEncarar() {
        assertTrue(REGRAS.observado(true, 0.95D), "olhando quase de frente, esta encarando");
        assertFalse(REGRAS.observado(true, 0.59D),
                "um fio abaixo do limite ja e desviar o olhar -- e e nessa fresta que o "
                        + "macaco avanca");
        assertFalse(REGRAS.observado(true, 0.0D), "olhar a 90 graus nao e encarar");
        assertFalse(REGRAS.observado(true, -1.0D), "de costas para o macaco nao e encarar");
    }

    @Test
    void oLimiteDeCossenoEInclusivo() {
        assertTrue(REGRAS.observado(true, 0.6D),
                "exatamente no limite o alvo JA conta como encarando: a borda pertence ao "
                        + "jogador, senao o macaco ganha um passo de graca em cima do numero");
    }

    /**
     * A TABELA QUE DEFINE O MOB. As quatro combinacoes estao aqui porque cada uma delas,
     * sozinha, e um mob diferente: sem a terceira linha o macaco anda encarado e a
     * contrapartida ensinavel desaparece; sem a primeira ele congela depois de revelar e
     * a emboscada nunca acontece.
     */
    @Test
    void reveladoAndaSempreEDisfarcadoSoAndaSemPlateia() {
        assertTrue(REGRAS.podeAproximar(false, false),
                "revelado e sem ninguem olhando: avanca");
        assertTrue(REGRAS.podeAproximar(false, true),
                "REVELADO E ENCARADO TAMBEM AVANCA -- depois do reveal olhar nao protege "
                        + "mais ninguem, e e isso que fecha a licao");
        assertTrue(REGRAS.podeAproximar(true, false),
                "disfarcado e sem ninguem olhando: e exatamente quando ele se aproxima");
        assertFalse(REGRAS.podeAproximar(true, true),
                "DISFARCADO E ENCARADO TRAVA: quem o encara o segura, e essa e a unica "
                        + "inconsistencia que o jogador pode observar antes da emboscada");
    }

    @Test
    void revelaPorDistanciaEPorDano() {
        assertTrue(REGRAS.revela(true, 1.0D, false),
                "chegou perto o bastante: revela, e o bando revela junto");
        assertTrue(REGRAS.revela(true, 40.0D, true),
                "tomou dano: o disfarce acabou onde quer que ele esteja");
    }

    @Test
    void oLimiteDeDistanciaDeRevelacaoEInclusivo() {
        assertTrue(REGRAS.revela(true, 3.5D, false),
                "exatamente na distancia de revelacao ja revela");
        assertFalse(REGRAS.revela(true, 3.51D, false),
                "um centimetro alem ainda e disfarce");
    }

    /**
     * Revelar quem ja esta revelado nao da erro -- da um mob que reemite o aviso do bando
     * a cada tick, e o "reveal + pack ambush" vira um chamado permanente.
     */
    @Test
    void quemJaEstaReveladoNuncaRevelaDeNovo() {
        assertFalse(REGRAS.revela(false, 0.0D, false),
                "ja revelado e colado no alvo: nao ha o que revelar");
        assertFalse(REGRAS.revela(false, 1.0D, true),
                "nem tomar dano revela de novo quem ja saiu do disfarce");
        assertFalse(REGRAS.revela(false, 40.0D, false));
    }

    @Test
    void semDistanciaNemDanoODisfarceSeMantem() {
        assertFalse(REGRAS.revela(true, 12.0D, false),
                "longe e intocado: o macaco continua parecendo gente");
    }

    /**
     * Valor nao finito vindo de um alvo em estado estranho nao pode virar observacao nem
     * revelacao de graca: comparacao com NaN ja e falsa nas duas pontas, e o infinito
     * positivo no cosseno passaria por "encarando" em qualquer limite.
     */
    @Test
    void cossenoEDistanciaNaoFinitosNaoValemNada() {
        assertFalse(REGRAS.observado(true, Double.NaN),
                "NaN no olhar nao pode congelar o macaco");
        assertFalse(REGRAS.observado(true, Double.POSITIVE_INFINITY),
                "infinito passaria por 'encarando de frente' em qualquer limite de cosseno");
        assertFalse(REGRAS.observado(true, Double.NEGATIVE_INFINITY));

        assertFalse(REGRAS.revela(true, Double.NaN, false),
                "NaN na distancia nao pode revelar o bando inteiro do nada");
        assertFalse(REGRAS.revela(true, Double.POSITIVE_INFINITY, false));
        assertFalse(REGRAS.revela(true, Double.NEGATIVE_INFINITY, false),
                "distancia negativa infinita seria 'perto demais' numa comparacao ingenua");
    }

    @Test
    void regrasImpossiveisSaoRejeitadas() {
        assertThrows(IllegalArgumentException.class,
                () -> new DisguiseRules(0.0D, 0.6D, 12.0D, 10),
                "distancia de revelacao zero e um macaco que nunca revela: ele encosta no "
                        + "jogador ainda fingindo ser gente, e nada acusa isso");
        assertThrows(IllegalArgumentException.class,
                () -> new DisguiseRules(-1.0D, 0.6D, 12.0D, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new DisguiseRules(Double.NaN, 0.6D, 12.0D, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new DisguiseRules(Double.POSITIVE_INFINITY, 0.6D, 12.0D, 10));

        assertThrows(IllegalArgumentException.class,
                () -> new DisguiseRules(3.5D, 2.0D, 12.0D, 10),
                "cosseno fora de [-1, 1] e um limite que nenhum olhar alcanca: o macaco "
                        + "nunca seria observado e andaria encarado o tempo todo");
        assertThrows(IllegalArgumentException.class,
                () -> new DisguiseRules(3.5D, -2.0D, 12.0D, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new DisguiseRules(3.5D, Double.NaN, 12.0D, 10));

        assertThrows(IllegalArgumentException.class,
                () -> new DisguiseRules(3.5D, 0.6D, 3.0D, 10),
                "raio do bando menor que a distancia de revelacao: o primeiro macaco "
                        + "revelaria sozinho e o bando nunca chegaria -- em jogo isso so "
                        + "parece um mob fraco");
        assertThrows(IllegalArgumentException.class,
                () -> new DisguiseRules(3.5D, 0.6D, -1.0D, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new DisguiseRules(3.5D, 0.6D, Double.NaN, 10));

        assertThrows(IllegalArgumentException.class,
                () -> new DisguiseRules(3.5D, 0.6D, 12.0D, 0),
                "reveal de zero tick e uma transformacao sem quadro nenhum: o jogador nao "
                        + "ve a inconsistencia se concretizar");
        assertThrows(IllegalArgumentException.class,
                () -> new DisguiseRules(3.5D, 0.6D, 12.0D, -1));
    }
}
