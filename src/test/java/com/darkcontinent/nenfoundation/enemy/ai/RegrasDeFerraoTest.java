package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da escolha entre os dois golpes do Scorpion Leader.
 *
 * <p>O defeito que ele segura e sempre o mesmo e nunca da erro: um dos dois
 * ataques para de acontecer. Com o ferrao ausente, o veneno continua implementado,
 * testado e documentado -- e some do jogo. Com a pinca ausente, o telegrafo longo
 * vira o unico telegrafo e deixa de significar "este e o caro".</p>
 */
class RegrasDeFerraoTest {

    private static final double ALCANCE_DA_PINCA = 1.1D;
    private static final double ALCANCE_DO_FERRAO = 1.3D;
    private static final int GUARDA = 140;

    private static RegrasDeFerrao regras() {
        return new RegrasDeFerrao(ALCANCE_DA_PINCA, ALCANCE_DO_FERRAO, GUARDA);
    }

    /** Atalho para o caso saudavel: alvo visivel, de pe, com recarga pronta. */
    private static DecisaoDeFerrao decidir(double distancia, int ticksDesdeAFerroada) {
        return regras().decidir(distancia, true, false, true, ticksDesdeAFerroada, false);
    }

    // --------------------------------------------------------- o caso normal

    @Test
    @DisplayName("passada a guarda, ela FERROA; dentro da guarda e perto, ela usa a pinca")
    void osDoisGolpesSeAlternamPelaGuardaEPelaDistancia() {
        assertEquals(DecisaoDeFerrao.ARMAR_O_FERRAO, decidir(1.0D, GUARDA),
                "com a guarda vencida e o alvo ao alcance, o golpe que envenena e o que vale a"
                        + " pena -- ele e a razao de o bicho existir");
        assertEquals(DecisaoDeFerrao.GOLPEAR_COM_PINCA, decidir(1.0D, GUARDA - 1),
                "dentro da guarda ela ainda ataca, com o golpe barato. Uma formiga que so espera"
                        + " entre ferroadas deixaria o jogador bater de graca por sete segundos");
        assertEquals(DecisaoDeFerrao.ARMAR_O_FERRAO,
                decidir(ALCANCE_DO_FERRAO, GUARDA),
                "no limite exato do alcance do ferrao ela ainda decide ferroar: o limiar e"
                        + " inclusivo, e a caixa de dano cobre essa borda -- ver"
                        + " ScorpionLeaderTuningTest");
    }

    @Test
    @DisplayName("entre os dois alcances ela nao improvisa: espera, e nao golpeia com a pinca")
    void naFaixaSoDoFerraoElaNaoUsaAPinca() {
        assertEquals(DecisaoDeFerrao.AGUARDAR, decidir(1.2D, 0),
                "1.2 esta dentro do alcance do ferrao e fora do da pinca, e o ferrao esta em"
                        + " guarda. Deixa-la golpear aqui faria a pinca comecar um aviso contra"
                        + " alguem que a garra nao alcanca -- e como a navegacao trava durante o"
                        + " golpe, esse ataque NUNCA acertaria. Nao da erro: da um bicho que erra"
                        + " sozinho e parece quebrado");
        assertEquals(DecisaoDeFerrao.AGUARDAR, decidir(4.0D, GUARDA),
                "longe dos dois alcances nao ha golpe nenhum a comecar");
    }

    // ------------------------------------------------------------- as recusas

    @Test
    @DisplayName("cada recusa tem motivo proprio, porque cada uma pede coisa diferente")
    void asTresRecusasNaoColapsamEmUmaSo() {
        RegrasDeFerrao regras = regras();

        assertEquals(DecisaoDeFerrao.CAMBALEANDO,
                regras.decidir(1.0D, true, true, true, GUARDA, false),
                "cambaleando e a unica recusa que tambem manda PARAR o caminho: sem a distincao a"
                        + " formiga interrompida continuaria andando para o alvo, e interromper"
                        + " deixaria de parecer que fez alguma coisa");
        assertEquals(DecisaoDeFerrao.SEGURAR_A_GUARDA,
                regras.decidir(1.0D, true, false, true, GUARDA, true),
                "a postura vinda da intencao de Nen segura o golpe. Colapsada em AGUARDAR, a"
                        + " decisao tatica nao teria efeito visivel nenhum na tela");
        assertEquals(DecisaoDeFerrao.AGUARDAR,
                regras.decidir(1.0D, true, false, false, GUARDA, false),
                "em recarga ela nao comeca nada");
        assertEquals(DecisaoDeFerrao.AGUARDAR,
                regras.decidir(0.0D, false, false, true, GUARDA, false),
                "sem alvo visivel, colada no jogador ou nao, nao ha o que atacar");

        // A ORDEM importa e e cobrada aqui: cambaleando VENCE a guarda fechada.
        // Invertida, a formiga interrompida no meio de uma postura de guarda nao
        // pararia o caminho, e o cambaleio ficaria sem o unico efeito que o
        // distingue.
        assertEquals(DecisaoDeFerrao.CAMBALEANDO,
                regras.decidir(1.0D, true, true, true, GUARDA, true));
    }

    // ------------------------------------------------- os casos que REPROVAM

    @Test
    @DisplayName("O CASO QUE DEVE REPROVAR: ferrao alcancando menos que a pinca apaga o veneno")
    void alcancesInvertidosSaoRecusados() {
        // Este e o ajuste mais tentador que existe neste bicho: "a ferroada esta
        // acertando de longe demais, encurta o alcance dela". Encurtado abaixo do da
        // pinca, a formiga sempre entraria na distancia da pinca primeiro, a pinca
        // dispararia a cada recarga e a ferroada NUNCA sairia. Nada reprovaria: o
        // veneno continuaria implementado, testado e documentado, e ausente do jogo.
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFerrao(ALCANCE_DO_FERRAO, ALCANCE_DA_PINCA, GUARDA));
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFerrao(ALCANCE_DA_PINCA, ALCANCE_DA_PINCA, GUARDA),
                "iguais e o mesmo defeito com outro nome: sem faixa exclusiva do ferrao, a"
                        + " escolha entre os dois golpes passa a depender so da guarda");

        // E a prova de que a configuracao ATUAL passa por esta mesma regua.
        assertEquals(DecisaoDeFerrao.AGUARDAR, decidir(1.2D, 0));
    }

    @Test
    @DisplayName("O CASO QUE DEVE REPROVAR: guarda zero apaga o golpe comum, e alcance zero, os dois")
    void guardaZeroEAlcanceZeroSaoRecusados() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFerrao(ALCANCE_DA_PINCA, ALCANCE_DO_FERRAO, 0),
                "sem intervalo entre ferroadas, toda recarga terminaria em veneno e a pinca --"
                        + " que e o que da ao jogador com o que comparar o telegrafo longo --"
                        + " nunca apareceria");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFerrao(0.0D, ALCANCE_DO_FERRAO, GUARDA));
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFerrao(ALCANCE_DA_PINCA, Double.NaN, GUARDA));
        assertThrows(IllegalArgumentException.class, () -> decidir(-1.0D, GUARDA),
                "distancia negativa significa que quem chamou mediu errado, e a decisao sairia"
                        + " plausivel demais para alguem notar");
        assertThrows(IllegalArgumentException.class, () -> decidir(1.0D, -1));
    }
}
