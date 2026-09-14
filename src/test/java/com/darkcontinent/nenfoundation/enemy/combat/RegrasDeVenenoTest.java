package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da regra de veneno: acumulo, teto, duracao e decaimento.
 *
 * <p>Todos os defeitos que ele segura tem a mesma assinatura -- compilam, aplicam
 * um efeito, escrevem um numero plausivel na tela e passam em todo o resto. O
 * veneno continua existindo; ele so deixa de fazer a unica coisa que a ficha do
 * Scorpion Leader promete.</p>
 */
class RegrasDeVenenoTest {

    /** Os numeros do bicho, para o teste falar dos mesmos que o jogo. */
    private static final int DOSE = 240;
    private static final int TETO = 600;
    private static final int NIVEL_MAXIMO = 1;

    private static RegrasDeVeneno regras() {
        return new RegrasDeVeneno(DOSE, TETO, NIVEL_MAXIMO);
    }

    // --------------------------------------------------------- o caso normal

    @Test
    @DisplayName("a primeira ferroada da nivel I; a segunda, dentro do prazo, da nivel II")
    void oAcumuloSobeODegrauQuandoASegundaChegaANTESDaPrimeiraExpirar() {
        RegrasDeVeneno regras = regras();

        DoseDeVeneno primeira = regras.aplicar(true, 0);
        assertEquals(DecisaoDeVeneno.APLICOU, primeira.decisao());
        assertEquals(DOSE, primeira.duracaoEmTicks());
        assertEquals(0, primeira.amplificador(),
                "uma ferroada e veneno I. Comecar em II daria ao primeiro acerto a recompensa do"
                        + " acumulo, e o acumulo deixaria de ser observavel");
        assertTrue(primeira.aplicavel());

        // A segunda chega com a primeira ainda correndo -- e e disso que o acumulo
        // depende. ScorpionLeaderTuning cobra, do lado dele, que a guarda entre
        // ferroadas seja MENOR que a dose, justamente para que este caso aconteca.
        DoseDeVeneno segunda = regras.aplicar(true, DOSE);
        assertEquals(DecisaoDeVeneno.APLICOU, segunda.decisao());
        assertEquals(2 * DOSE, segunda.duracaoEmTicks());
        assertEquals(1, segunda.amplificador(),
                "duas doses inteiras somam veneno II: e este degrau que o jogador tem de ver"
                        + " subir para entender que ficar perto custa mais a cada ferroada");
    }

    @Test
    @DisplayName("o decaimento nao precisa de codigo: ele e a duracao escorrendo")
    void oDegrauSoVoltaQuandoOVenenoACABA() {
        RegrasDeVeneno regras = regras();

        assertEquals(1, regras.amplificadorPara(2 * DOSE));
        assertEquals(1, regras.amplificadorPara(DOSE + 1));
        assertEquals(0, regras.amplificadorPara(DOSE),
                "o degrau e a dose: acima de uma dose inteira o nivel e II, ate ela. Nao ha"
                        + " contador proprio -- um campo 'nivel' ao lado da duracao seria a"
                        + " segunda fonte para a mesma verdade, e ele continuaria alto depois de"
                        + " o efeito expirar sozinho");
        assertEquals(0, regras.amplificadorPara(0));

        // A CONSEQUENCIA DE DESIGN, escrita aqui porque ela nao e obvia: QUALQUER
        // sobra da dose anterior ja promove a proxima ferroada para o nivel II. O
        // degrau so volta para I quando o veneno acaba DE VERDADE -- ou seja, sair
        // de perto por um instante nao basta, e e preciso quebrar o contato o
        // bastante para o efeito escorrer inteiro. E essa a decisao que o bicho
        // cobra do jogador.
        assertEquals(DOSE + 5, regras.aplicar(true, 5).duracaoEmTicks());
        assertEquals(1, regras.aplicar(true, 5).amplificador(),
                "cinco ticks de sobra ja sao sobra: recuar meio segundo nao devolve o degrau");
        assertEquals(0, regras.aplicar(true, 0).amplificador(),
                "e com o veneno esgotado a proxima ferroada recomeca em I -- este e o decaimento,"
                        + " e ele acontece sem uma linha de codigo propria");
    }

    // ------------------------------------------------------------- as recusas

    @Test
    @DisplayName("no teto a ferroada RENOVA e nao acumula -- e diz isso com um motivo proprio")
    void oTetoRecusaComMotivoEmVezDeSubirParaSempre() {
        RegrasDeVeneno regras = regras();

        DoseDeVeneno noTeto = regras.aplicar(true, TETO);
        assertEquals(DecisaoDeVeneno.NO_TETO, noTeto.decisao(),
                "sem um motivo proprio, quem chama nao teria como avisar o jogador, e uma ferroada"
                        + " anunciada por 28 ticks passaria a nao fazer nada em silencio");
        assertEquals(TETO, noTeto.duracaoEmTicks(), "no teto ela ainda RENOVA o prazo");
        assertTrue(noTeto.aplicavel());
        assertEquals(NIVEL_MAXIMO, noTeto.amplificador());

        // O teto morde ANTES do teto exato, e e esse o ponto: uma ferroada perto do
        // limite nao pode empurrar o total para alem dele.
        assertEquals(TETO, regras.aplicar(true, TETO - 1).duracaoEmTicks(),
                "somar a dose inteira aqui passaria do teto, e o teto e a unica coisa que impede"
                        + " um esquadrao de transformar veneno em execucao por dano continuo");
    }

    @Test
    @DisplayName("alvo que nao sofre veneno nao recebe dose nenhuma, e a recusa e de OUTRO tipo")
    void alvoImuneNaoRecebeDose() {
        DoseDeVeneno imune = regras().aplicar(false, 0);
        assertEquals(DecisaoDeVeneno.ALVO_IMUNE, imune.decisao());
        assertEquals(0, imune.duracaoEmTicks());
        assertEquals(0, imune.amplificador());
        assertFalse(imune.aplicavel(),
                "quem chama nao pode aplicar nada aqui: aplicado, o efeito nem entraria, e a"
                        + " mensagem de recusa apareceria ao lado de um veneno que o jogo aceitou");

        // As duas recusas tem de ser DISTINGUIVEIS: elas pedem mensagens opostas --
        // "espere o veneno baixar" contra "este alvo nao sofre veneno". Trocadas,
        // elas ensinam o jogador a fazer a coisa errada.
        assertTrue(imune.decisao() != regras().aplicar(true, TETO).decisao());
    }

    // ------------------------------------------------- os casos que REPROVAM

    @Test
    @DisplayName("O CASO QUE DEVE REPROVAR: teto menor ou igual a uma dose apaga o acumulo")
    void tetoQueCabeNumaDoseEhRecusado() {
        // Este e o ajuste mais tentador que existe nesta regra: "o veneno esta forte
        // demais, baixa o teto". Baixado ate a dose, a PRIMEIRA ferroada ja chega ao
        // limite, a segunda nao acumula nada, e a mecanica que a ficha do bicho
        // promete some -- com o veneno funcionando perfeitamente.
        assertThrows(IllegalArgumentException.class, () -> new RegrasDeVeneno(DOSE, DOSE, 1));
        assertThrows(IllegalArgumentException.class, () -> new RegrasDeVeneno(DOSE, DOSE - 1, 1));

        // E a prova de que a configuracao ATUAL passa por esta mesma regua, em vez
        // de apenas nao ser testada.
        assertTrue(TETO > DOSE);
        assertEquals(3, regras().ferroadasAteOTeto(),
                "duas doses e meia: o teto e folgado para uma formiga sozinha e aperta quando ha"
                        + " mais de uma venenosa no esquadrao, que e o caso para o qual ele existe");
    }

    @Test
    @DisplayName("O CASO QUE DEVE REPROVAR: veneno de duracao zero e nivel acima do limite")
    void duracaoZeroEnivelAcimaDoLimiteSaoRecusados() {
        assertThrows(IllegalArgumentException.class, () -> new RegrasDeVeneno(0, TETO, 1),
                "veneno que some no tick seguinte nao e veneno: e um segundo tipo de dano direto,"
                        + " pago por um telegrafo caro que nao entregou nada");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeVeneno(DOSE, TETO, RegrasDeVeneno.AMPLIFICADOR_LIMITE + 1),
                "acima do limite o intervalo de dano do veneno vanilla cai para tres ticks e o"
                        + " acumulo vira execucao por uma fonte que o jogador nao consegue apontar");
        assertThrows(IllegalArgumentException.class, () -> regras().aplicar(true, -1),
                "duracao restante negativa significa que quem chamou leu o efeito errado, e a"
                        + " conta sairia plausivel demais para alguem notar");
        assertThrows(IllegalArgumentException.class,
                () -> new DoseDeVeneno(DecisaoDeVeneno.ALVO_IMUNE, 40, 0),
                "recusa com dose e o defeito que o record fecha: quem chama olharia so a duracao e"
                        + " a recusa viraria uma aplicacao comum");
    }
}
