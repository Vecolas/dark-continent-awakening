package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Sustenta a COLEIRA DO NINHO, que e o mob 06 inteiro: a spider eagle nao caca ninguem --
 * ela defende um lugar, avisa antes, bota depois, e LARGA A PERSEGUICAO quando o intruso
 * recua ou quando ela propria se afastou demais do ninho.
 *
 * <p>As tres perguntas ("ja avisa?", "ja pode botar?", "ja e hora de voltar?") sao uma
 * fonte so, medivel sem mundo. Espalhadas pelos {@code if} da Goal, cada uma viraria um
 * numero solto, e a divergencia entre elas nao daria erro nenhum: apareceria como uma ave
 * que persegue o jogador ate o outro lado do bioma, ou que causa dano sem ter avisado --
 * e a promessa de "recuar e ser poupado", que e o que um dia permite roubar o ovo sem
 * matar a mae, deixaria de existir sem que nada acusasse.</p>
 */
class NestGuardRulesTest {
    /** Os mesmos numeros de {@code HunterExamProfiles.spiderEagleNest()}. */
    private static final NestGuardRules REGRAS = new NestGuardRules(16.0D, 6.0D, 28.0D, 30);

    @Test
    void avisaSoDentroDoRaioDeAvisoDoNinho() {
        assertTrue(REGRAS.avisa(0.0D), "em cima do ninho e o caso mais obvio de aviso");
        assertTrue(REGRAS.avisa(10.0D), "dentro do territorio a ave grita antes de qualquer dano");
        assertFalse(REGRAS.avisa(16.01D),
                "um passo alem do territorio nao e invasao: avisar aqui transformaria a ave "
                        + "num predador de caminho, que e o oposto do perigo de NINHO");
        assertFalse(REGRAS.avisa(40.0D), "longe do ninho a ave nao tem nada a defender");
    }

    @Test
    void oLimiteDoRaioDeAvisoEInclusivo() {
        assertTrue(REGRAS.avisa(16.0D),
                "exatamente na borda do territorio a ave JA avisa: a borda pertence ao aviso, "
                        + "senao existe um anel onde o jogador entra sem receber telegrafo nenhum");
    }

    /**
     * O PAR QUE IMPEDE "ESTAVA PARADA E CAUSOU DANO". O bote exige as DUAS coisas ao mesmo
     * tempo -- estar dentro do raio de bote E ja ter avisado o tempo inteiro.
     *
     * <p>Cada metade sozinha e um mob diferente e quebrado: so a distancia da uma ave que
     * mergulha no instante em que o jogador encosta, sem aviso; so o tempo da uma ave que
     * mergulha de trinta blocos e chega no vazio. Nenhum dos dois aparece como erro.</p>
     */
    @Test
    void oBoteExigeEstarPertoEJaTerAvisadoPeloTempoInteiro() {
        assertTrue(REGRAS.bote(3.0D, 30),
                "perto do ninho e com o aviso cumprido: e aqui que o mergulho acontece");

        assertFalse(REGRAS.bote(3.0D, 0),
                "PERTO MAS SEM AVISO NENHUM: seria dano do nada, e e exatamente o relato "
                        + "'ela estava parada e me matou' que este par existe para impedir");
        assertFalse(REGRAS.bote(3.0D, 29),
                "um tick antes do aviso terminar o mergulho ainda nao esta autorizado");

        assertFalse(REGRAS.bote(6.01D, 30),
                "COM AVISO MAS LONGE: a ave nao bota de fora do raio de bote, senao a "
                        + "defesa do ninho vira perseguicao a distancia");
        assertFalse(REGRAS.bote(15.0D, 9999),
                "nenhum tempo de aviso autoriza um mergulho iniciado no outro extremo do "
                        + "territorio");
    }

    @Test
    void oLimiteDeTicksDeAvisoEInclusivo() {
        assertTrue(REGRAS.bote(6.0D, 30),
                "exatamente o tempo de aviso cumprido, exatamente na borda do raio de bote: "
                        + "as duas pontas sao inclusivas, senao o aviso dura um tick a mais "
                        + "do que o numero configurado diz");
    }

    /**
     * A REGRA QUE POUPA QUEM RECUA, e a razao de este mob existir. A ave desiste por DOIS
     * motivos independentes, e e importante que cada um baste sozinho: se desistir exigisse
     * os dois juntos, um jogador que recuasse continuaria sendo perseguido enquanto a ave
     * estivesse dentro da coleira -- e "recuar e ser poupado" viraria mentira sem que nada
     * desse erro.
     */
    @Test
    void desistePelaColeiraOuPeloIntrusoQueRecuou() {
        assertTrue(REGRAS.desiste(28.01D, 5.0D),
                "A AVE PASSOU DA COLEIRA: mesmo com o intruso colado no ninho ela volta, "
                        + "senao uma perseguicao longa a leva para fora do canyon e ela nunca "
                        + "mais acha o ninho");
        assertTrue(REGRAS.desiste(10.0D, 16.01D),
                "O INTRUSO RECUOU para fora do territorio: quem sai e poupado, e e isso que "
                        + "um dia permite roubar o ovo sem matar a mae");
        assertTrue(REGRAS.desiste(28.01D, 16.01D),
                "os dois motivos valendo juntos continuam desistindo");
        assertFalse(REGRAS.desiste(10.0D, 5.0D),
                "ave dentro da coleira e intruso dentro do territorio: nao ha motivo para "
                        + "largar a defesa");
    }

    @Test
    void naBordaExataDaColeiraEDoTerritorioAindaNaoDesiste() {
        assertFalse(REGRAS.desiste(28.0D, 16.0D),
                "exatamente NA coleira a ave ainda nao passou dela, e exatamente na borda o "
                        + "intruso ainda esta dentro: desistir aqui encolheria os dois raios "
                        + "em silencio, e em jogo isso so parece 'as vezes ela desiste cedo'");
    }

    /**
     * Distancia nao finita vem de estado degenerado (ninho descarregado, alvo em posicao
     * estranha) e nao pode virar aviso nem dano de graca: comparacao com NaN ja e falsa,
     * mas o infinito NEGATIVO passaria por "colado no ninho" em qualquer limite.
     */
    @Test
    void distanciaNaoFinitaNaoViraAvisoNemBote() {
        assertFalse(REGRAS.avisa(Double.NaN), "NaN nao pode acender o aviso do ninho");
        assertFalse(REGRAS.avisa(Double.POSITIVE_INFINITY));
        assertFalse(REGRAS.avisa(Double.NEGATIVE_INFINITY),
                "distancia infinita negativa seria 'dentro do ninho' numa comparacao ingenua");

        assertFalse(REGRAS.bote(Double.NaN, 30), "NaN nao pode autorizar um mergulho");
        assertFalse(REGRAS.bote(Double.POSITIVE_INFINITY, 30));
        assertFalse(REGRAS.bote(Double.NEGATIVE_INFINITY, 30),
                "infinito negativo passaria por 'colado no ninho' e o mergulho sairia do nada");
    }

    @Test
    void regrasImpossiveisSaoRejeitadas() {
        assertThrows(IllegalArgumentException.class,
                () -> new NestGuardRules(16.0D, 0.0D, 28.0D, 30),
                "raio de bote zero e uma ave que avisa para sempre e nunca ataca -- ninguem "
                        + "nunca precisa respeitar o territorio, e isso nao da erro nenhum");
        assertThrows(IllegalArgumentException.class,
                () -> new NestGuardRules(16.0D, -1.0D, 28.0D, 30));

        assertThrows(IllegalArgumentException.class,
                () -> new NestGuardRules(4.0D, 6.0D, 28.0D, 30),
                "aviso MENOR que o bote inverte a escada: a ave entraria em bote antes de "
                        + "ter avisado, e o mergulho viraria dano sem telegrafo");

        assertThrows(IllegalArgumentException.class,
                () -> new NestGuardRules(16.0D, 6.0D, 10.0D, 30),
                "coleira MENOR que o raio de aviso e uma ave que desiste dentro do proprio "
                        + "territorio: ela larga a perseguicao dentro do lugar que deveria "
                        + "defender, e isso so aparece como 'ela desiste cedo demais'");

        assertThrows(IllegalArgumentException.class,
                () -> new NestGuardRules(16.0D, 6.0D, 28.0D, 0),
                "aviso de zero tick e bote sem telegrafo -- o aviso e o unico tempo de "
                        + "reacao que o jogador tem antes do mergulho");
        assertThrows(IllegalArgumentException.class,
                () -> new NestGuardRules(16.0D, 6.0D, 28.0D, -1));

        assertThrows(IllegalArgumentException.class,
                () -> new NestGuardRules(Double.NaN, 6.0D, 28.0D, 30));
        assertThrows(IllegalArgumentException.class,
                () -> new NestGuardRules(16.0D, Double.NaN, 28.0D, 30));
        assertThrows(IllegalArgumentException.class,
                () -> new NestGuardRules(16.0D, 6.0D, Double.NaN, 30));
        assertThrows(IllegalArgumentException.class,
                () -> new NestGuardRules(16.0D, 6.0D, Double.POSITIVE_INFINITY, 30),
                "coleira infinita e uma ave que NUNCA volta para o ninho: ela persegue pelo "
                        + "mundo inteiro e o mob deixa de ser um perigo de lugar");
        assertThrows(IllegalArgumentException.class,
                () -> new NestGuardRules(Double.POSITIVE_INFINITY, 6.0D, 28.0D, 30));
    }
}
