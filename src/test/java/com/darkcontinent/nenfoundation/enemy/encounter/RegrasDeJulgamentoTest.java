package com.darkcontinent.nenfoundation.enemy.encounter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Sustenta o JULGAMENTO, que e o kiriko inteiro e o oposto de todos os outros mobs deste
 * repositorio: ele nao ataca o jogador -- ele o AVALIA.
 *
 * <p>O encontro se vence NAO LUTANDO. Quem espera, nao saca arma e deixa o bicho em paz e
 * aprovado; quem ataca o kiriko, ou fere um animal pacifico na frente dele, e reprovado e
 * ai sim leva briga. Isso e canone e e o ponto da ficha: "Magical Beast != monster".</p>
 *
 * <p>POR QUE AS TRES PERGUNTAS MORAM NUM RECORD PURO. Espalhadas pelo tick da entidade e
 * pelas Goals, "quanto vale o que o jogador acabou de fazer", "isso ja reprovou?" e "isso
 * ja aprovou?" viravam tres {@code if} diferentes, e a divergencia entre eles nao daria
 * erro nenhum: apareceria como um kiriko que as vezes ataca quem nao fez nada e as vezes
 * premia quem bateu nele -- e ninguem conseguiria dizer em que arquivo isso foi decidido.
 * Aqui elas sao uma fonte so, mediveis sem mundo e sem entidade.</p>
 */
class RegrasDeJulgamentoTest {

    /** Os mesmos numeros de {@code HunterExamProfiles.kirikoJulgamento()}. */
    private static final RegrasDeJulgamento REGRAS = new RegrasDeJulgamento(200, 40, 25, 1, 60);

    // ------------------------------------------------------------------ pontuar

    /**
     * PACIENCIA SOMA. E a unica coisa que o jogador pode fazer para subir a nota, e ela e
     * deliberadamente barata por tick: o premio e o TEMPO, nao a intensidade.
     */
    @Test
    void esperarEmPazSomaOGanhoDePaciencia() {
        assertEquals(1, REGRAS.pontuar(0, false, false, true),
                "um tick de espera vale exatamente ganhoPorPaciencia na nota zerada");
        assertEquals(51, REGRAS.pontuar(50, false, false, true),
                "esperar soma sobre a nota que ja existia; recomecar do zero a cada tick "
                        + "apagaria o unico investimento que o jogador faz neste encontro");
    }

    /**
     * AGRESSAO SUBTRAI, e esta e a assimetria que o mob existe para ensinar. O custo e
     * declarado POSITIVO no record e entra NEGATIVO na conta -- se ele entrasse positivo,
     * bater no kiriko passaria a aprovar o jogador, e nada acusaria: o encontro seguiria
     * terminando com recompensa, so que premiando exatamente o comportamento errado.
     */
    @Test
    void atacarOKirikoSubtraiOCustoDeAgressao() {
        assertEquals(-40, REGRAS.pontuar(0, true, false, false),
                "um golpe no kiriko vale MENOS custoDeAgressao: o sinal e a regra inteira");
        assertEquals(60, REGRAS.pontuar(100, true, false, false),
                "o custo desconta da nota acumulada, nao a substitui");
    }

    /**
     * CRUELDADE TAMBEM SUBTRAI, e ela e a metade do julgamento que nao depende do kiriko:
     * ferir um bicho pacifico na frente dele reprova igual.
     *
     * <p>Sem isto o teste vira "nao bata NESTE mob", que qualquer jogador aprende por
     * tentativa e erro sem entender nada. O que esta sendo avaliado e como a pessoa se
     * comporta quando acha que ninguem esta olhando.</p>
     */
    @Test
    void ferirUmInocenteSubtraiOCustoDeCrueldade() {
        RegrasDeJulgamento regras = new RegrasDeJulgamento(200, 80, 25, 1, 60);

        // Do teto, uma crueldade desce 25: 60 -> 35, e ainda nao reprova. Ferir um bicho
        // custa, mas nao e o mesmo que bater no proprio kiriko.
        assertEquals(35, regras.pontuar(60, false, true, false),
                "ferir um inocente precisa custar, e custar o valor declarado");

        // E A PACIENCIA NAO PASSA DO TETO. Esta e a metade que prende o teto no lugar:
        // sem ela, esperar viraria credito e o jogador paciente compraria o direito de
        // bater. A primeira versao deste assert perguntava o que acontece a partir de
        // 100 -- e 100 e estado INALCANCAVEL, porque pontuar nunca devolve acima do
        // limite. Regua que mede entrada impossivel nao mede nada.
        assertEquals(60, regras.pontuar(60, false, false, true),
                "esperar em cima do teto empurrou a nota para cima dele");
    }

    /**
     * As tres entradas somam NO MESMO TICK, cada uma uma vez.
     *
     * <p>Um {@code else if} escondido aqui nao apareceria em teste nenhum de uma entrada
     * so: o jogador que ataca o kiriko enquanto pisa num coelho pagaria por apenas um dos
     * dois, e o mais caro dos erros sairia de graca dependendo da ordem em que alguem
     * escreveu os ifs.</p>
     */
    @Test
    void asTresEntradasSomamNoMesmoTick() {
        assertEquals(100 - 40 - 25 + 1, REGRAS.pontuar(100, true, true, true),
                "agressao, crueldade e paciencia entram JUNTAS: cobrar so a primeira que "
                        + "casou faria o pior tick possivel custar menos que o segundo pior");
    }

    @Test
    void tickSemNadaNaoMexeNaNota() {
        assertEquals(50, REGRAS.pontuar(50, false, false, false),
                "quem nao esperou em paz, nao atacou e nao feriu ninguem nao ganha nem "
                        + "perde: cobrar aqui puniria o jogador por estar simplesmente "
                        + "andando, e premiar aqui daria a nota de graca a quem passou reto");
    }

    // ------------------------------------------------------------------ reprova

    /**
     * REPROVA QUANDO FICA NEGATIVA, e o ZERO NAO REPROVA.
     *
     * <p>A fronteira e a ficha: a nota comeca em zero, entao um kiriko que reprovasse no
     * zero atacaria todo jogador no primeiro tick do encontro, antes de existir qualquer
     * comportamento para avaliar. Isso nao daria erro -- daria "mais um bicho que ataca",
     * que e exatamente o mob que a ficha proibe.</p>
     */
    @Test
    void reprovaSomenteComNotaNegativa() {
        assertTrue(REGRAS.reprova(-1),
                "um ponto abaixo de zero JA e reprovacao: a fronteira e o sinal");
        assertTrue(REGRAS.reprova(-40), "um golpe basta");
        assertFalse(REGRAS.reprova(0),
                "ZERO NAO REPROVA: a nota nasce em zero, e reprovar aqui faria o kiriko "
                        + "atacar antes de o jogador ter feito coisa nenhuma -- o encontro "
                        + "social viraria um spawn hostil comum, sem uma linha no log");
        assertFalse(REGRAS.reprova(1));
        assertFalse(REGRAS.reprova(200), "nota alta nao reprova ninguem");
    }

    /**
     * UM GOLPE, A PARTIR DO ZERO, REPROVA NA HORA. E a pergunta que o mob faz ao jogador,
     * lida de ponta a ponta: {@code pontuar} e {@code reprova} juntos, como a entidade os
     * usa.
     *
     * <p>Medir so o {@code -40} deixaria passar em silencio o dia em que a fronteira de
     * {@code reprova} mudasse: os dois testes ficariam verdes e o kiriko aguentaria
     * pancada calado.</p>
     */
    @Test
    void umUnicoGolpeNoInicioDoEncontroJaReprova() {
        assertTrue(REGRAS.reprova(REGRAS.pontuar(0, true, false, false)),
                "o primeiro golpe tem de reprovar NA HORA. Se nao reprovar, o jogador bate "
                        + "no kiriko, nada acontece, e a leitura que ele leva do encontro e "
                        + "que a violencia era uma das respostas aceitas.");
        assertTrue(REGRAS.reprova(REGRAS.pontuar(0, false, true, false)),
                "ferir um inocente na frente dele tambem reprova do zero: o julgamento nao "
                        + "e sobre o kiriko, e sobre o jogador");
    }

    // ------------------------------------------------------------------- aprova

    /**
     * APROVAR EXIGE AS DUAS CONDICOES, e cada uma sozinha nao basta.
     *
     * <p>Sem o piso de nota, qualquer um que aparecesse e sumisse seria aprovado. Sem o
     * piso de TEMPO, a nota chegaria ao limite em {@code limiteDeAprovacao /
     * ganhoPorPaciencia} ticks e o jogador seria aprovado em segundos, sem que o kiriko
     * chegasse a observar nada -- o teste que da nome ao mob deixaria de existir e o
     * encontro viraria uma entrega de item por proximidade.</p>
     */
    @Test
    void aprovarExigeNotaEObservacaoJuntas() {
        assertTrue(REGRAS.aprova(60, 200),
                "nota no limite e janela de observacao cumprida: e este o unico caso de "
                        + "aprovacao");

        assertFalse(REGRAS.aprova(59, 200),
                "NOTA ABAIXO DO LIMITE com o tempo cumprido: esperar do lado do kiriko nao "
                        + "pode aprovar sozinho, senao quem atacou e ficou parado depois "
                        + "recebe o mesmo premio de quem nunca sacou arma");
        assertFalse(REGRAS.aprova(1000, 199),
                "TEMPO INSUFICIENTE com nota altissima: aprovar antes da janela faz o "
                        + "kiriko premiar sem ter observado -- o 'teste' vira um enfeite "
                        + "narrativo e nada em jogo acusa");
        assertFalse(REGRAS.aprova(0, 0), "no primeiro tick nao ha nem nota nem observacao");
        assertFalse(REGRAS.aprova(-40, 100000),
                "quem esta com nota negativa nao e aprovado por insistir parado: sem isso, "
                        + "bater e esperar seria uma estrategia");
    }

    @Test
    void osLimitesDaAprovacaoSaoInclusivos() {
        assertTrue(REGRAS.aprova(60, 200),
                "EXATAMENTE no limite de nota e EXATAMENTE na janela a aprovacao JA vale: "
                        + "os dois numeros configurados mentiriam em um ponto inteiro se a "
                        + "borda nao pertencesse a aprovacao");
        assertTrue(REGRAS.aprova(61, 201), "acima dos dois limites nao ha duvida");
    }

    // ---------------------------------------------- o que o record NAO faz

    /**
     * O RECORD NAO LEMBRA DA REPROVACAO, e isso e decisao, nao esquecimento.
     *
     * <p>Ele e puro: cada chamada responde sobre os numeros que recebeu. Quem reprovou e
     * depois ficou parado tempo suficiente volta a ter nota positiva, e {@code reprova}
     * volta a dizer {@code false} -- o que em jogo seria um kiriko que perdoa o jogador
     * que o esfaqueou, se ele aguentar a briga sem revidar por mais um tanto.</p>
     *
     * <p>ENTAO A TRAVA MORA EM QUEM CHAMA: a entidade guarda "reprovado" e nao volta
     * atras. Este teste existe para que essa fronteira esteja ESCRITA em algum lugar que
     * reprova quando alguem apagar a trava achando que o record ja cuidava disso.</p>
     */
    @Test
    void oRecordNaoLatchaAReprovacaoEPorIssoQuemChamaPrecisaLatchar() {
        int nota = REGRAS.pontuar(0, true, false, false);
        assertTrue(REGRAS.reprova(nota), "o golpe reprovou");

        for (int tick = 0; tick < 41; tick++) {
            nota = REGRAS.pontuar(nota, false, false, true);
        }

        assertFalse(REGRAS.reprova(nota),
                "depois de 41 ticks de paciencia a nota voltou a ser positiva e o record "
                        + "deixou de chamar isso de reprovacao (nota " + nota + "). Isto NAO "
                        + "e um bug do record: e a prova de que a memoria da reprovacao "
                        + "precisa viver em KirikoEntity. Se alguem tirar a trava de la "
                        + "confiando neste record, o kiriko perdoa quem o atacou e o "
                        + "julgamento perde a consequencia -- sem erro nenhum no log.");
    }

    // ------------------------------------------------------ o record impossivel

    @Test
    void regrasImpossiveisSaoRejeitadas() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeJulgamento(0, 40, 25, 1, 60),
                "janela de observacao ZERO aprova no primeiro tick em que a nota bater no "
                        + "limite: o kiriko premia sem ter observado, e o mob deixa de ser "
                        + "um teste");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeJulgamento(-1, 40, 25, 1, 60));

        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeJulgamento(200, 0, 25, 1, 60),
                "custo de agressao ZERO e o falso verde deste mob: bater no kiriko passa a "
                        + "nao custar nada, o jogador agride e continua sendo aprovado, e "
                        + "tudo continua funcionando -- so que ensinando o oposto");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeJulgamento(200, -40, 25, 1, 60),
                "custo NEGATIVO entra na conta como PREMIO: atacar o kiriko subiria a nota, "
                        + "e o encontro passaria a recompensar exatamente o que reprova");

        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeJulgamento(200, 40, 0, 1, 60),
                "crueldade de graca reduz o julgamento a 'nao bata NESTE bicho', e o que a "
                        + "ficha pede e avaliar o jogador, nao proteger o mob");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeJulgamento(200, 40, -25, 1, 60));

        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeJulgamento(200, 40, 25, 0, 60),
                "ganho de paciencia ZERO e uma aprovacao IMPOSSIVEL: a nota nunca sai do "
                        + "zero, ninguem nunca e aprovado, e o unico desfecho que sobra e a "
                        + "briga -- o mob vira inimigo comum com passos extras");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeJulgamento(200, 40, 25, -1, 60),
                "ganho NEGATIVO faz esperar em paz REPROVAR");

        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeJulgamento(200, 40, 25, 1, 0),
                "limite de aprovacao ZERO dispensa a nota: cumprida a janela, ate quem "
                        + "acabou de atacar com nota zerada seria aprovado");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeJulgamento(200, 40, 25, 1, -60));
    }
}
