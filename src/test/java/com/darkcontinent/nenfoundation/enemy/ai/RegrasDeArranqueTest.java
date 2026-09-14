package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do ARRANQUE COM FADIGA -- a regra que impede uma perseguicao sem fim.
 *
 * <p>Todos os defeitos que ele segura tem a mesma assinatura: compilam, o mob
 * nasce, corre, morde e dropa loot. O que muda e se o encontro TERMINA, e
 * "termina" nao aparece em nenhum outro portao deste repositorio.</p>
 *
 * <p>Os numeros usados aqui sao PROPRIOS do teste, e nao os de producao. Os de
 * producao sao provados em {@code CheetahLeaderTuningTest}: separados, um ajuste
 * de balanceamento que quebrasse a regra reprovaria la, com a mensagem certa, em
 * vez de reescrever o que a regra significa.</p>
 */
class RegrasDeArranqueTest {

    private static final int ARRANQUE = 30;
    private static final int FADIGA = 45;
    private static final int RECARGA = 60;
    private static final double RAPIDO = 1.55D;
    private static final double LENTO = 0.55D;
    private static final double MINIMA = 6.0D;
    private static final double MAXIMA = 26.0D;
    private static final double ABERTURA = 4.0D;

    private static RegrasDeArranque regras() {
        return new RegrasDeArranque(ARRANQUE, FADIGA, RECARGA, RAPIDO, LENTO,
                MINIMA, MAXIMA, ABERTURA);
    }

    /** Ninguem por perto: o valor que tem de passar por toda comparacao de proximidade. */
    private static final double SEM_ALIADO = Double.POSITIVE_INFINITY;

    // ------------------------------------------------------------ caso normal

    @Test
    @DisplayName("PRONTA, com alvo visivel na faixa e ninguem na frente: ela arranca")
    void oCasoEmQueOArranqueSai() {
        RegrasDeArranque regras = regras();
        assertEquals(DecisaoDeArranque.ARRANCAR,
                regras.decidir(FaseDoArranque.PRONTA, 12.0D, SEM_ALIADO, true, true));
        assertEquals(DecisaoDeArranque.ARRANCAR,
                regras.decidir(FaseDoArranque.PRONTA, MINIMA, SEM_ALIADO, true, true),
                "a ponta de baixo e INCLUSIVA: recusa-la aqui encolheria a faixa util em silencio,"
                        + " e o numero escrito na ficha deixaria de ser o numero em jogo");
        assertEquals(DecisaoDeArranque.ARRANCAR,
                regras.decidir(FaseDoArranque.PRONTA, MAXIMA, SEM_ALIADO, true, true),
                "a ponta de cima tambem e inclusiva, pela mesma razao");
    }

    @Test
    @DisplayName("o multiplicador sai da FASE, e cada fase tem o seu")
    void cadaFaseTemOProprioMultiplicador() {
        RegrasDeArranque regras = regras();
        assertEquals(RAPIDO, regras.multiplicadorDe(FaseDoArranque.ARRANCANDO));
        assertEquals(LENTO, regras.multiplicadorDe(FaseDoArranque.FATIGADA));
        assertEquals(1.0D, regras.multiplicadorDe(FaseDoArranque.PRONTA));
        assertEquals(1.0D, regras.multiplicadorDe(FaseDoArranque.EM_RECARGA),
                "a recarga NAO e lenta: ela e so a negacao do proximo arranque. Torna-la lenta"
                        + " somaria dois precos ao mesmo ganho, e o bicho passaria mais tempo"
                        + " abaixo da propria velocidade do que acima dela");
        assertThrows(IllegalArgumentException.class, () -> regras.multiplicadorDe(null),
                "devolver 1.0 para uma fase ausente esconderia a ausencia atras de um bicho que"
                        + " anda normalmente");
    }

    @Test
    @DisplayName("a duracao de cada fase e a declarada; PRONTA nao tem prazo")
    void cadaFaseTemOProprioPrazo() {
        RegrasDeArranque regras = regras();
        assertEquals(ARRANQUE, regras.duracaoDe(FaseDoArranque.ARRANCANDO));
        assertEquals(FADIGA, regras.duracaoDe(FaseDoArranque.FATIGADA));
        assertEquals(RECARGA, regras.duracaoDe(FaseDoArranque.EM_RECARGA));
        assertEquals(Integer.MAX_VALUE, regras.duracaoDe(FaseDoArranque.PRONTA),
                "PRONTA espera uma decisao, nao um relogio. Com um prazo finito ela viraria uma"
                        + " quarta fase que 'expira' para si mesma, e o contador zeraria de tempos"
                        + " em tempos sem que nada mudasse na tela");
        assertEquals(ARRANQUE + FADIGA + RECARGA, regras.ticksDoCiclo());
    }

    // --------------------------------------------------------------- recusado

    @Test
    @DisplayName("a FASE vence tudo: fatigada ou em recarga, nenhum alvo autoriza o arranque")
    void aFaseVenceOAlvoPerfeito() {
        RegrasDeArranque regras = regras();
        assertEquals(DecisaoDeArranque.EM_ANDAMENTO,
                regras.decidir(FaseDoArranque.ARRANCANDO, 12.0D, SEM_ALIADO, true, true),
                "reautorizar no meio do arranque reiniciaria o relogio a cada pedido, a fase nunca"
                        + " terminaria, e o bicho correria acelerado para sempre sem pagar a fadiga");
        assertEquals(DecisaoDeArranque.FATIGADA,
                regras.decidir(FaseDoArranque.FATIGADA, 12.0D, SEM_ALIADO, true, true),
                "arrancar durante a fadiga apaga a unica janela de resposta que o jogador tem"
                        + " contra um bicho mais rapido que ele");
        assertEquals(DecisaoDeArranque.EM_RECARGA,
                regras.decidir(FaseDoArranque.EM_RECARGA, 12.0D, SEM_ALIADO, true, true));
    }

    @Test
    @DisplayName("sem alvo e sem visao sao recusas nomeadas, e nao o mesmo silencio")
    void semAlvoESemVisaoSaoRecusasDiferentes() {
        RegrasDeArranque regras = regras();
        assertEquals(DecisaoDeArranque.SEM_ALVO,
                regras.decidir(FaseDoArranque.PRONTA, 12.0D, SEM_ALIADO, false, false));
        assertEquals(DecisaoDeArranque.SEM_VISAO,
                regras.decidir(FaseDoArranque.PRONTA, 12.0D, SEM_ALIADO, true, false),
                "arrancar contra quem ela nao ve gasta o ciclo numa parede, e pune o jogador por"
                        + " ter feito exatamente a coisa certa, que e quebrar a linha de visao");
    }

    @Test
    @DisplayName("perto demais e longe demais recusam, cada um pelo proprio motivo")
    void aFaixaDeDistanciaRecusaDosDoisLados() {
        RegrasDeArranque regras = regras();
        assertEquals(DecisaoDeArranque.PERTO_DEMAIS,
                regras.decidir(FaseDoArranque.PRONTA, MINIMA - 0.01D, SEM_ALIADO, true, true),
                "colado no alvo o arranque viaja quase nada e entrega a FADIGA colada no jogador:"
                        + " a vantagem do bicho vira uma desvantagem, e nada acusa");
        assertEquals(DecisaoDeArranque.LONGE_DEMAIS,
                regras.decidir(FaseDoArranque.PRONTA, MAXIMA + 0.01D, SEM_ALIADO, true, true),
                "alem da faixa ela chega FATIGADA, e o jogador aprende que o arranque e inofensivo");
    }

    @Test
    @DisplayName("companheiro ja em cima do alvo: nao ha combate para ABRIR")
    void aliadoNoAlvoCancelaAAbertura() {
        RegrasDeArranque regras = regras();
        assertEquals(DecisaoDeArranque.ALIADO_JA_ABRIU,
                regras.decidir(FaseDoArranque.PRONTA, 12.0D, ABERTURA, true, true),
                "no limite exato o combate JA conta como aberto: deixar a ponta de fora daria um"
                        + " lider que arranca para dentro de uma briga que comecou, chegando"
                        + " fatigado no meio dela");
        assertEquals(DecisaoDeArranque.ARRANCAR,
                regras.decidir(FaseDoArranque.PRONTA, 12.0D, ABERTURA + 0.01D, true, true),
                "um centimetro fora do anel de abertura ela ainda abre");
    }

    @Test
    @DisplayName("medida quebrada nao vira arranque: NaN e distancia negativa sao recusa")
    void medidaQuebradaNaoViraArranque() {
        RegrasDeArranque regras = regras();
        assertThrows(IllegalArgumentException.class,
                () -> regras.decidir(FaseDoArranque.PRONTA, Double.NaN, SEM_ALIADO, true, true),
                "TODA comparacao com NaN e falsa, entao uma distancia NaN passaria por PERTO_DEMAIS"
                        + " e por LONGE_DEMAIS e chegaria ao fim do metodo como AUTORIZACAO -- o"
                        + " unico caminho em que uma medida quebrada vira 'pode'");
        assertThrows(IllegalArgumentException.class,
                () -> regras.decidir(FaseDoArranque.PRONTA, 12.0D, Double.NaN, true, true));
        assertThrows(IllegalArgumentException.class,
                () -> regras.decidir(FaseDoArranque.PRONTA, -1.0D, SEM_ALIADO, true, true));
        assertThrows(IllegalArgumentException.class,
                () -> regras.decidir(null, 12.0D, SEM_ALIADO, true, true));
        assertEquals(DecisaoDeArranque.ARRANCAR,
                regras.decidir(FaseDoArranque.PRONTA, 12.0D, Double.POSITIVE_INFINITY, true, true),
                "infinito NAO e medida quebrada: e como 'nao ha aliado nenhum' se escreve, e"
                        + " recusa-lo desligaria o arranque de todo lider que esta sozinho");
    }

    // ------------------------------------------- os casos que DEVEM reprovar

    @Test
    @DisplayName("PORTAO: fadiga curta demais para ser vista e recusada, com motivo")
    void aFadigaPrecisaCaberNumaRespostaHumana() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeArranque(ARRANQUE, RegrasDeArranque.FADIGA_MINIMA - 1, RECARGA,
                        1.2D, LENTO, MINIMA, MAXIMA, ABERTURA));
        assertTrue(erro.getMessage().contains("janela de resposta"),
                "a recusa precisa dizer O QUE some quando a fadiga encolhe; so acusar nao ensina."
                        + " Mensagem: " + erro.getMessage());

        // E a regua tem de ACEITAR o limite exato -- uma regua que reprova o caso
        // legitimo e tao inutil quanto uma que aprova o ilegitimo, e esta seria
        // pior: o ajuste seguro passaria a parecer proibido.
        assertEquals(RegrasDeArranque.FADIGA_MINIMA,
                new RegrasDeArranque(ARRANQUE, RegrasDeArranque.FADIGA_MINIMA, RECARGA,
                        1.2D, 0.5D, MINIMA, MAXIMA, ABERTURA).ticksDeFadiga());
    }

    @Test
    @DisplayName("PORTAO: arranque que GANHA terreno liquido no ciclo e recusado")
    void oArranqueNaoPodeCriarVelocidade() {
        // 30 ticks a 2.0 e 45 a 0.9 dao 100.5 contra os 75 que ela andaria parada
        // na propria velocidade: um ganho liquido por ciclo, para sempre.
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeArranque(ARRANQUE, FADIGA, RECARGA, 2.0D, 0.9D,
                        MINIMA, MAXIMA, ABERTURA));
        assertTrue(erro.getMessage().contains("CRIAR velocidade"),
                "a mensagem precisa nomear o defeito -- arranque que cria em vez de redistribuir --"
                        + " senao quem ler vai achar que o problema e o numero, e nao a conta."
                        + " Mensagem: " + erro.getMessage());

        // O limite EXATO passa: media 1.0 e redistribuicao perfeita, que e o caso
        // que a regra existe para permitir.
        RegrasDeArranque noLimite = new RegrasDeArranque(20, 20, RECARGA, 1.5D, 0.5D,
                MINIMA, MAXIMA, ABERTURA);
        assertEquals(1.0D, noLimite.mediaDoCiclo(), 1.0E-9D);
        assertTrue(regras().mediaDoCiclo() <= RegrasDeArranque.TETO_DA_MEDIA_DO_CICLO,
                "os numeros deste teste tambem tem de respeitar o teto, senao a bateria inteira"
                        + " estaria rodando sobre uma configuracao que o jogo recusaria");
    }

    @Test
    @DisplayName("PORTAO: recarga mais curta que o arranque e recusada")
    void aRecargaNaoPodeSerMaisCurtaQueOArranque() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeArranque(ARRANQUE, FADIGA, ARRANQUE - 1, RAPIDO, LENTO,
                        MINIMA, MAXIMA, ABERTURA));
        assertTrue(erro.getMessage().contains("mais disponivel do que e caro"),
                "Mensagem: " + erro.getMessage());
        assertEquals(ARRANQUE, new RegrasDeArranque(ARRANQUE, FADIGA, ARRANQUE, RAPIDO, LENTO,
                MINIMA, MAXIMA, ABERTURA).ticksDeRecarga(),
                "o limite exato -- recarga igual ao arranque -- e legitimo e tem de passar");
    }

    @Test
    @DisplayName("PORTAO: anel de abertura mais largo que a distancia minima e recusado")
    void oAnelDeAberturaNaoPodeDesligarOArranque() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeArranque(ARRANQUE, FADIGA, RECARGA, RAPIDO, LENTO,
                        MINIMA, MAXIMA, MINIMA + 0.01D));
        assertTrue(erro.getMessage().contains("desligaria em silencio"),
                "Mensagem: " + erro.getMessage());
        assertEquals(MINIMA, new RegrasDeArranque(ARRANQUE, FADIGA, RECARGA, RAPIDO, LENTO,
                MINIMA, MAXIMA, MINIMA).distanciaDeAbertura(),
                "anel igual a distancia minima ainda e util, e tem de passar");
    }

    @Test
    @DisplayName("PORTAO: multiplicadores fora das faixas e faixa de distancia vazia sao recusados")
    void osNumerosSemSentidoSaoRecusados() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeArranque(ARRANQUE, FADIGA, RECARGA, 1.0D, LENTO,
                        MINIMA, MAXIMA, ABERTURA),
                "multiplicador de arranque em 1.0 paga a fadiga por uma vantagem que nunca houve");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeArranque(ARRANQUE, FADIGA, RECARGA, RAPIDO, 1.0D,
                        MINIMA, MAXIMA, ABERTURA),
                "fadiga em 1.0 nao custa nada, e o arranque fica de graca");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeArranque(ARRANQUE, FADIGA, RECARGA, RAPIDO, 0.0D,
                        MINIMA, MAXIMA, ABERTURA),
                "fadiga em zero CONGELA o bicho no meio do campo, o que le como travamento");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeArranque(ARRANQUE, FADIGA, RECARGA, RAPIDO, LENTO,
                        MAXIMA, MINIMA, ABERTURA),
                "faixa invertida: nenhuma distancia a satisfaz, e o arranque nunca sai");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeArranque(0, FADIGA, RECARGA, RAPIDO, LENTO,
                        MINIMA, MAXIMA, ABERTURA),
                "arranque de zero tick nunca sai, e o bicho passa em todo portao sem a ficha dele");
    }
}
