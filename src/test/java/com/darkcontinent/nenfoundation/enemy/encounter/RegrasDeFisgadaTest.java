package com.darkcontinent.nenfoundation.enemy.encounter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Sustenta o CABO DE GUERRA, que e o mob 36 inteiro: o master of the swamp nao e um
 * inimigo que se mata, e um que se FISGA. Puxar demais arrebenta a linha e ele escapa;
 * acompanhar o peixe o cansa ate poder ser recolhido.
 *
 * <p>As tres perguntas ("ele vem na isca?", "quanta tensao ha na linha agora?", "ja
 * arrebentou / ja cansou?") sao UMA fonte so, medivel sem mundo. Espalhadas pelos
 * {@code if} da Goal e pelo tick da entidade, cada uma viraria um numero solto, e a
 * divergencia entre elas nao daria erro nenhum -- apareceria como "as vezes a linha
 * arrebenta do nada", que ninguem consegue reproduzir porque depende de qual dos
 * caminhos somou a tensao naquele tick.</p>
 */
class RegrasDeFisgadaTest {

    /** Os mesmos numeros de {@code HunterExamProfiles.masterOfTheSwampFishing()}. */
    private static final RegrasDeFisgada REGRAS =
            new RegrasDeFisgada(8.0D, 200, 100.0D, 6.0D, 4.0D);

    // ------------------------------------------------------------------ a isca

    /**
     * O PAR DE TRES QUE IMPEDE "O PEIXE VEIO DO NADA". Investigar exige as tres coisas
     * juntas -- isca NA AGUA, dentro do raio, e a espera ja cumprida.
     *
     * <p>Cada uma sozinha e um mob diferente e quebrado: sem a agua o peixe sobe atras
     * de uma isca jogada na margem; sem o raio ele atravessa o pantano inteiro por um
     * anzol que o jogador nem sabe que ele viu; sem a espera ele morde no mesmo tick do
     * lancamento, e a pescaria vira um clique. Nenhum dos tres aparece como erro.</p>
     */
    @Test
    void investigarAIscaExigeAguaDistanciaEEsperaCumprida() {
        assertTrue(REGRAS.investigaIsca(true, 4.0D, 0),
                "isca na agua, dentro do raio e com a espera cumprida: e este o unico caso "
                        + "em que o peixe sai do fundo");

        assertFalse(REGRAS.investigaIsca(false, 4.0D, 0),
                "ISCA FORA DA AGUA: um peixe que investiga anzol na margem deixa de ser um "
                        + "encontro de agua, e a pescaria passa a funcionar em terra seca");
        assertFalse(REGRAS.investigaIsca(true, 8.01D, 0),
                "LONGE DEMAIS: um passo alem do raio e uma isca que o peixe nao tem como "
                        + "ter percebido -- investigar aqui transforma o raio em enfeite");
        assertFalse(REGRAS.investigaIsca(true, 40.0D, 0),
                "do outro lado do pantano nenhuma isca chama ninguem");
        assertFalse(REGRAS.investigaIsca(true, 4.0D, 1),
                "UM TICK DE ESPERA RESTANDO: a mordida no instante do lancamento tira do "
                        + "jogador a unica parte da pescaria que e esperar");
        assertFalse(REGRAS.investigaIsca(true, 4.0D, 200),
                "com a espera inteira pela frente o peixe ainda nao se mexeu");
    }

    @Test
    void osLimitesDaIscaSaoInclusivos() {
        assertTrue(REGRAS.investigaIsca(true, 8.0D, 0),
                "exatamente na borda do raio a isca JA vale: a borda pertence ao alcance, "
                        + "senao existe um anel em que o jogador acerta o lance e nada acontece");
        assertTrue(REGRAS.investigaIsca(true, 4.0D, -1),
                "espera vencida ha um tick continua vencida: um contador que passa do zero "
                        + "nao pode cancelar uma mordida que ja estava autorizada");
    }

    // ---------------------------------------------------------------- a tensao

    /**
     * A TENSAO SOBE AO AFASTAR. Sem isto "puxar demais" nao custa nada, e o cabo de
     * guerra vira espera -- o jogador segura o botao e ganha sempre.
     */
    @Test
    void afastarSeDoPeixeSobeATensao() {
        assertEquals(6.0D, REGRAS.tensao(0.0D, 1.0D),
                "um bloco de afastamento vale exatamente tensaoPorAfastamento na linha vazia");
        assertTrue(REGRAS.tensao(50.0D, 1.0D) > 50.0D,
                "afastar-se com a linha ja tensionada tem de somar, nao estabilizar");
    }

    /**
     * E DESCE AO APROXIMAR, que e a contrapartida ensinavel: acompanhar o peixe alivia a
     * linha. Sem o alivio a tensao so cresce, toda fisgada termina em linha arrebentada e
     * o estado CAUGHT nunca acontece.
     */
    @Test
    void acompanharOPeixeAliviaALinha() {
        assertEquals(6.0D, REGRAS.tensao(10.0D, -1.0D),
                "um bloco de aproximacao vale exatamente alivioPorAproximacao");
        assertTrue(REGRAS.tensao(50.0D, -1.0D) < 50.0D,
                "aproximar-se tem de baixar a tensao, senao recuar com o peixe nao e resposta");
    }

    @Test
    void distanciaParadaNaoMexeNaTensao() {
        assertEquals(30.0D, REGRAS.tensao(30.0D, 0.0D),
                "sem variacao de distancia nao ha o que somar nem o que aliviar: cobrar "
                        + "tensao de quem ficou parado puniria justamente a resposta certa");
    }

    /**
     * O PISO EM ZERO. Sem ele a tensao vira uma divida negativa: o jogador acompanha o
     * peixe por uns segundos, acumula "credito", e depois foge em linha reta de graca ate
     * gastar o credito. Em jogo isso nao da erro -- da uma linha que so arrebenta as
     * vezes, e ninguem sabe dizer por que.
     */
    @Test
    void aTensaoTemPisoEmZeroENuncaFicaNegativa() {
        assertEquals(0.0D, REGRAS.tensao(2.0D, -1.0D),
                "aliviar 4 sobre 2 de tensao para em ZERO, nao em -2: tensao negativa e "
                        + "credito para fugir depois sem pagar nada");
        assertEquals(0.0D, REGRAS.tensao(0.0D, -1.0D),
                "linha ja frouxa nao fica mais frouxa que frouxa");
        assertTrue(REGRAS.tensao(1.0D, -100.0D) >= 0.0D,
                "nenhuma aproximacao, por maior que seja, produz tensao abaixo de zero");
    }

    /**
     * Variacao NAO FINITA vem de estado degenerado -- alvo em posicao estranha, entidade
     * recem-descarregada, distancia medida contra um ponto invalido -- e nao pode nem
     * arrebentar a linha nem zerar a tensao.
     *
     * <p>As duas pontas sao ruins e nenhuma da erro: um infinito POSITIVO arrebentaria a
     * linha de um jogador que nao se mexeu, e um infinito NEGATIVO daria a ele um alivio
     * total de graca. Devolver a tensao INTACTA e a unica saida que nao inventa
     * resultado nenhum a partir de uma medida que nao existe.</p>
     */
    @Test
    void variacaoNaoFinitaDevolveATensaoIntacta() {
        assertEquals(40.0D, REGRAS.tensao(40.0D, Double.NaN),
                "NaN nao pode mexer na linha: a medida nao existe");
        assertEquals(40.0D, REGRAS.tensao(40.0D, Double.POSITIVE_INFINITY),
                "infinito positivo arrebentaria a linha de quem estava parado");
        assertEquals(40.0D, REGRAS.tensao(40.0D, Double.NEGATIVE_INFINITY),
                "infinito negativo daria alivio total de graca, e fugir sairia gratis no "
                        + "tick seguinte");
    }

    // ------------------------------------------------------- os dois desfechos

    @Test
    void aLinhaArrebentaNoLimiteEDeleParaCima() {
        assertTrue(REGRAS.arrebenta(100.0D),
                "EXATAMENTE na tensao maxima a linha JA arrebenta: o limite pertence ao "
                        + "rompimento, senao o numero configurado mente em um ponto inteiro");
        assertTrue(REGRAS.arrebenta(180.0D), "acima do limite nao ha duvida");
        assertFalse(REGRAS.arrebenta(99.99D),
                "um centesimo abaixo do limite a linha aguenta -- e essa margem e o que faz "
                        + "o jogador sentir que quase perdeu o peixe");
        assertFalse(REGRAS.arrebenta(0.0D), "linha frouxa nao arrebenta");
    }

    @Test
    void oPeixeCansaNoLimiteEDeleParaCima() {
        assertTrue(REGRAS.cansou(200),
                "EXATAMENTE no tempo configurado o peixe JA cansou: o limite pertence ao "
                        + "cansaco, senao a fisgada dura um tick a mais do que o perfil diz");
        assertTrue(REGRAS.cansou(600), "depois do tempo ele segue cansado");
        assertFalse(REGRAS.cansou(199),
                "um tick antes ele ainda briga: e o ultimo tick em que o jogador ainda pode "
                        + "estragar tudo puxando");
        assertFalse(REGRAS.cansou(0), "no tick da mordida o peixe esta inteiro");
    }

    // ------------------------------------------------------ o record impossivel

    @Test
    void regrasImpossiveisSaoRejeitadas() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFisgada(0.0D, 200, 100.0D, 6.0D, 4.0D),
                "raio de isca zero e um peixe que NUNCA investiga: a vara deixa de ser a "
                        + "forma de encontrar o mob, e nada acusa -- o pantano so parece vazio");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFisgada(-1.0D, 200, 100.0D, 6.0D, 4.0D));

        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFisgada(8.0D, 0, 100.0D, 6.0D, 4.0D),
                "cansar em zero tick e um peixe recolhido no instante da mordida -- o cabo "
                        + "de guerra inteiro deixa de existir e a captura vira um clique");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFisgada(8.0D, -1, 100.0D, 6.0D, 4.0D));

        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFisgada(8.0D, 200, 0.0D, 6.0D, 4.0D),
                "tensao maxima zero e uma linha que arrebenta na primeira puxada: ninguem "
                        + "nunca captura nada, e o mob vira uma animacao de escape");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFisgada(8.0D, 200, -1.0D, 6.0D, 4.0D));

        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFisgada(8.0D, 200, 100.0D, 0.0D, 4.0D),
                "tensao por afastamento zero e o falso verde deste mob: a linha NUNCA "
                        + "tensiona, puxar demais nao custa nada, e o cabo de guerra vira "
                        + "espera -- sem um unico erro no log");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFisgada(8.0D, 200, 100.0D, -1.0D, 4.0D));

        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFisgada(8.0D, 200, 100.0D, 6.0D, -1.0D),
                "alivio NEGATIVO inverte o cabo de guerra: acompanhar o peixe passaria a "
                        + "tensionar a linha, e a resposta certa viraria a errada");

        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFisgada(Double.NaN, 200, 100.0D, 6.0D, 4.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFisgada(Double.POSITIVE_INFINITY, 200, 100.0D, 6.0D, 4.0D),
                "raio infinito e um peixe que investiga qualquer isca do mundo, e o encontro "
                        + "deixa de ser de LUGAR");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFisgada(8.0D, 200, Double.NaN, 6.0D, 4.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFisgada(8.0D, 200, Double.POSITIVE_INFINITY, 6.0D, 4.0D),
                "tensao maxima infinita e uma linha que nunca arrebenta: puxar passa a ser "
                        + "sempre a melhor jogada, e o mob perde a unica decisao que tem");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFisgada(8.0D, 200, 100.0D, Double.NaN, 4.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFisgada(8.0D, 200, 100.0D, 6.0D, Double.NaN));
    }
}
