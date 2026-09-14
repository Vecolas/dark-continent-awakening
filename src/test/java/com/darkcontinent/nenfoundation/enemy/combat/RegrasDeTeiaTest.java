package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sustenta que a Spider Webber tem uma ZONA MORTA e que a zona morta custa.
 *
 * <p>As coisas que este portao segura nao levantam excecao em lugar nenhum do
 * jogo: uma faixa de tiro estreita demais (que faz o oficial nunca atirar), um
 * limiar de recuo diferente do limiar de tiro (que faz o oficial andar para tras
 * e para frente para sempre) e uma imobilizacao sem saida (que e morte sem
 * resposta). Todas saem verdes em compilacao, em spawn e em jogo.</p>
 */
class RegrasDeTeiaTest {

    /** As regras da ficha, escritas aqui para o portao nao depender do pacote de conteudo. */
    private static final RegrasDeTeia TEIA = new RegrasDeTeia(3.5D, 9.0D, 60, 8.0F);

    private static RecusaDaTeia lancarDe(double distancia) {
        return TEIA.podeLancar(distancia, true, false, true);
    }

    // ------------------------------------------------------------ caso normal

    @Test
    @DisplayName("na faixa, com linha de visao e pronta, a teia sai")
    void oCasoNormal() {
        assertEquals(RecusaDaTeia.NENHUMA, lancarDe(6.0D), "meio da faixa e o caso obvio");
        assertEquals(RecusaDaTeia.NENHUMA, lancarDe(3.5D),
                "o limite de baixo e INCLUSIVO: a zona morta termina onde a faixa comeca, e uma"
                        + " fresta entre as duas seria uma distancia em que ela nem atira nem recua");
        assertEquals(RecusaDaTeia.NENHUMA, lancarDe(9.0D), "o limite de cima tambem e inclusivo");

        assertEquals(DecisaoDeEspacamento.MANTER, TEIA.espacamento(6.0D),
                "onde ela atira e onde ela PARA -- parar e o que faz o telegrafo ser lido");
        assertEquals(60, TEIA.ticksDeImobilizacao());
        assertEquals(3.0D, TEIA.segundosDeImobilizacao(), 1.0E-9D);
    }

    @Test
    @DisplayName("a faixa de tiro e a faixa de parada, e isso nao pode ser coincidencia")
    void tiroEParadaSaoAMesmaFaixa() {
        // A invariante inteira, varrida em passos de meio decimo de bloco. Ela e a
        // razao de os dois alcances morarem no mesmo record: separados, o limiar de
        // recuo e o de recusa divergiriam e a aranha recuaria ate uma distancia em
        // que ela ainda se recusa a atirar -- andando para tras e para frente para
        // sempre, sem erro nenhum e sem nunca disparar.
        for (int passo = 0; passo <= 300; passo++) {
            double distancia = passo * 0.05D;
            boolean atira = lancarDe(distancia) == RecusaDaTeia.NENHUMA;
            boolean para = TEIA.espacamento(distancia) == DecisaoDeEspacamento.MANTER;
            assertEquals(para, atira,
                    "a " + distancia + " blocos a decisao de PARAR e a de ATIRAR discordam: e essa"
                            + " discordancia que produz o oficial que nunca ataca");
            assertEquals(atira, TEIA.naFaixaDeTiro(distancia),
                    "naFaixaDeTiro tem de ser derivada das mesmas comparacoes, e nao uma terceira");
        }
    }

    // --------------------------------------------------------- casos recusados

    @Test
    @DisplayName("O CASO RECUSADO: colado nela a teia NAO sai, e ela recua")
    void aZonaMortaEAResposta() {
        assertEquals(RecusaDaTeia.PERTO_DEMAIS, lancarDe(1.0D),
                "encostar nela desliga a teia: e a unica resposta que o encontro oferece");
        assertEquals(RecusaDaTeia.PERTO_DEMAIS, lancarDe(3.49D),
                "um centesimo dentro da zona morta ja e recusa -- o limite e regua, nao sugestao");
        assertEquals(DecisaoDeEspacamento.RECUAR, TEIA.espacamento(1.0D),
                "e a recusa PRECISA virar movimento: uma aranha que recusa e fica parada ensina"
                        + " que ela travou, e nao que ha uma zona morta");

        assertEquals(RecusaDaTeia.LONGE_DEMAIS, lancarDe(9.01D));
        assertEquals(DecisaoDeEspacamento.APROXIMAR, TEIA.espacamento(20.0D),
                "ela ENXERGA a 28 e alcanca a 9; a diferenca e o que produz a aproximacao");
    }

    @Test
    @DisplayName("a distancia vence a parede na hora de dizer POR QUE")
    void aOrdemDasRecusasEnsinaACoisaCerta() {
        // Quem esta colado na aranha tambem costuma estar sem linha de visao limpa.
        // Reportar a parede esconderia a licao -- e a licao e a zona morta.
        assertEquals(RecusaDaTeia.PERTO_DEMAIS, TEIA.podeLancar(1.0D, false, false, true));
        assertEquals(RecusaDaTeia.SEM_LINHA_DE_VISAO, TEIA.podeLancar(6.0D, false, false, true),
                "dentro da faixa, a parede e o motivo -- teia que atravessa bloco transforma"
                        + " cobertura em decoracao");
        assertEquals(RecusaDaTeia.JA_PRENDENDO, TEIA.podeLancar(6.0D, true, true, true),
                "uma fiandeira, uma presa: a segunda teia orfanizaria o relogio da primeira");
        assertEquals(RecusaDaTeia.EM_RECARGA, TEIA.podeLancar(6.0D, true, false, false));
    }

    @Test
    @DisplayName("medida invalida nao libera o tiro, e nao manda ela andar")
    void medidaInvalidaCaiNoLadoSeguro() {
        assertEquals(RecusaDaTeia.MEDIDA_INVALIDA, lancarDe(Double.NaN));
        assertEquals(RecusaDaTeia.MEDIDA_INVALIDA, lancarDe(Double.POSITIVE_INFINITY));
        assertEquals(RecusaDaTeia.MEDIDA_INVALIDA, lancarDe(-1.0D));

        assertEquals(DecisaoDeEspacamento.MANTER, TEIA.espacamento(Double.NaN),
                "para os PES, o lado seguro de um NaN e parar: 'aproximar' com medida invalida a"
                        + " faria andar para dentro do alvo, que e o oposto do papel dela");
        assertFalse(TEIA.naFaixaDeTiro(Double.NaN),
                "para o TIRO, o lado seguro do mesmo NaN e nao: herdar o MANTER daria um"
                        + " lancamento contra um alvo sem posicao");
    }

    // ------------------------------------------------- os casos que DEVEM reprovar

    @Test
    @DisplayName("O CASO QUE DEVE REPROVAR: faixa estreita e um oficial que nunca ataca")
    void faixaDeTiroEstreitaDemaisERecusada() {
        // Min 3.5, max 4.5: um bloco de faixa. A aranha anda ~0.14 bloco por tick e
        // o aviso dura 30 ticks -- um alvo andando atravessa uma faixa assim durante
        // o proprio telegrafo, e ela cancela o tiro sozinha. Nada nisso levanta
        // excecao em jogo: o mob nasce, percebe, persegue e nunca dispara.
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeTeia(3.5D, 4.5D, 60, 8.0F));
        assertTrue(erro.getMessage().contains("nunca ataca"),
                "a recusa tem de dizer o que acontece em jogo, e nao so que o numero e invalido;"
                        + " veio: " + erro.getMessage());

        assertThrows(IllegalArgumentException.class, () -> new RegrasDeTeia(3.5D, 3.5D, 60, 8.0F),
                "alcances IGUAIS: a faixa some e sobra uma casca decimal em que o tiro e legal");
        assertThrows(IllegalArgumentException.class, () -> new RegrasDeTeia(9.0D, 3.5D, 60, 8.0F),
                "invertidos, nao ha faixa nenhuma");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeTeia(3.5D, 3.5D + RegrasDeTeia.FAIXA_MINIMA_DE_TIRO - 0.01D,
                        60, 8.0F),
                "um centesimo abaixo do minimo ja reprova: o piso e uma regua, nao uma sugestao");
    }

    @Test
    @DisplayName("O CASO QUE DEVE REPROVAR: imobilizacao sem saida e morte sem resposta")
    void imobilizacaoSemSaidaERecusada() {
        IllegalArgumentException semDano = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeTeia(3.5D, 9.0D, 60, 0.0F));
        assertTrue(semDano.getMessage().contains("esperar"),
                "sem saida por DANO a jogada certa vira esperar, e a recusa tem de dizer isso;"
                        + " veio: " + semDano.getMessage());

        assertThrows(IllegalArgumentException.class, () -> new RegrasDeTeia(3.5D, 9.0D, 0, 8.0F),
                "prazo zero e teia que nao prende: o mob perde o unico papel que tem no esquadrao");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeTeia(3.5D, 9.0D,
                        RegrasDeTeia.TETO_DE_IMOBILIZACAO_EM_TICKS + 1, 8.0F),
                "um tick acima do teto ja reprova: acima dele o jogador assiste em vez de jogar");
        assertThrows(IllegalArgumentException.class, () -> new RegrasDeTeia(3.5D, 9.0D, 60, -1.0F));
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeTeia(3.5D, 9.0D, 60, Float.NaN),
                "um NaN no limiar nunca e alcancado por soma nenhuma: a saida por dano existiria"
                        + " no papel e nao existiria em jogo");
    }

    @Test
    @DisplayName("O CASO QUE DEVE REPROVAR: sem zona morta, chegar perto deixa de ser resposta")
    void zonaMortaZeradaERecusada() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeTeia(0.0D, 9.0D, 60, 8.0F));
        assertTrue(erro.getMessage().contains("saida"),
                "a recusa tem de dizer que o jogador perde a saida; veio: " + erro.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new RegrasDeTeia(-1.0D, 9.0D, 60, 8.0F));
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeTeia(Double.NaN, 9.0D, 60, 8.0F));
    }
}
