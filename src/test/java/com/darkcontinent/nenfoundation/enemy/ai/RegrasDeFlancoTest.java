package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadOrder;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRole;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do FLANCO: contornar nao e perseguir, e sozinho ele recua.
 *
 * <p>Os defeitos que ele segura compilam e passam em todo o resto. O bicho
 * continua sendo um mob; ele so deixa de ser ESTE mob -- vira um perseguidor
 * comum que ataca de frente, com a mesma ficha e o mesmo loot.</p>
 */
class RegrasDeFlancoTest {

    private static final UUID ALVO = UUID.nameUUIDFromBytes("wolf-runner-alvo".getBytes());

    private static final int MEMBROS = 2;
    private static final double FLANCO = 80.0D;
    private static final double ARCO_FRONTAL = 55.0D;
    private static final double INVESTIDA = 1.2D;
    private static final double CONTORNO = 3.2D;

    private static RegrasDeFlanco regras() {
        return new RegrasDeFlanco(SquadRules.esquadrao(), MEMBROS, FLANCO, ARCO_FRONTAL,
                INVESTIDA, CONTORNO);
    }

    private static SquadOrder engajar(SquadRole papel) {
        return new SquadOrder(ALVO, false, false, papel);
    }

    /** Cosseno de um angulo em graus -- o mesmo formato que o servidor mede. */
    private static double aGraus(double graus) {
        return Math.cos(Math.toRadians(graus));
    }

    // ------------------------------------------------------------ caso normal

    @Test
    @DisplayName("perto do alvo mas na CARA dele, o flanqueador continua contornando")
    void pertoNaoBastaSeAindaEstaNoArcoFrontal() {
        RegrasDeFlanco flanco = regras();
        SquadOrder ordem = engajar(SquadRole.FLANKER);

        assertEquals(DecisaoDeFlanco.CONTORNAR,
                flanco.decidir(ordem, 4, INVESTIDA * 0.5D, aGraus(10.0D)),
                "colado e de frente: investir aqui e a definicao de NAO flanquear, e o mob"
                        + " passaria a ser um perseguidor comum sem que nada reprovasse");
        assertEquals(DecisaoDeFlanco.INVESTIR,
                flanco.decidir(ordem, 4, INVESTIDA * 0.5D, aGraus(ARCO_FRONTAL + 1.0D)),
                "um grau fora do arco frontal ele fecha");
        assertEquals(DecisaoDeFlanco.CONTORNAR,
                flanco.decidir(ordem, 4, INVESTIDA * 0.5D, aGraus(ARCO_FRONTAL)),
                "exatamente na borda do arco ele AINDA e visto: a borda pertence a quem enxerga,"
                        + " senao o limite declarado seria um grau mais estreito do que o escrito");
        assertEquals(DecisaoDeFlanco.CONTORNAR,
                flanco.decidir(ordem, 4, INVESTIDA + 0.01D, aGraus(180.0D)),
                "pelas costas e fora de alcance: a distancia ainda manda, e ele contorna");
    }

    @Test
    @DisplayName("quem segura a frente NAO espera o flanco: investe de cara, e e por isso que o flanco existe")
    void quemSeguraAFrenteInvesteDeFrente() {
        RegrasDeFlanco flanco = regras();
        assertTrue(RegrasDeFlanco.papelContorna(SquadRole.FLANKER));
        assertTrue(RegrasDeFlanco.papelContorna(SquadRole.SCOUT));
        assertFalse(RegrasDeFlanco.papelContorna(SquadRole.LEADER));
        assertFalse(RegrasDeFlanco.papelContorna(SquadRole.FRONTLINER));

        assertEquals(DecisaoDeFlanco.INVESTIR,
                flanco.decidir(engajar(SquadRole.LEADER), 4, INVESTIDA * 0.5D, aGraus(0.0D)),
                "se TODOS esperassem o flanco, ninguem ficaria na cara do alvo: ele giraria"
                        + " livre, todo mundo continuaria dentro do arco de alguem, e o esquadrao"
                        + " circularia para sempre sem fechar");

        assertEquals(0.0D, flanco.anguloDePostoEmGraus(SquadRole.FRONTLINER, true), 0.0D,
                "quem segura a frente tem posto ZERO");
        assertEquals(FLANCO, flanco.anguloDePostoEmGraus(SquadRole.FLANKER, true), 0.0D);
        assertEquals(-FLANCO, flanco.anguloDePostoEmGraus(SquadRole.FLANKER, false), 0.0D,
                "os dois lados sao simetricos: fossem iguais, dois flanqueadores mirariam o mesmo"
                        + " ponto do arco e se empurrariam");
    }

    // --------------------------------------------------------------- recusado

    @Test
    @DisplayName("sozinho ele recua, e a ordem de recuo do grupo vence ate um alvo colado")
    void sozinhoRecuaEOGrupoMandaMais() {
        RegrasDeFlanco flanco = regras();
        SquadOrder ordem = engajar(SquadRole.FLANKER);

        assertEquals(DecisaoDeFlanco.RECUAR, flanco.decidir(ordem, 1, INVESTIDA * 0.5D, -1.0D),
                "um flanqueador sozinho nao flanqueia: nao ha quem segure a frente, e o alvo"
                        + " simplesmente vira para ele. HP 28 avancando sozinho morre de graca, e"
                        + " nenhum portao reclama disso");
        assertEquals(DecisaoDeFlanco.INVESTIR, flanco.decidir(ordem, 2, INVESTIDA * 0.5D, -1.0D),
                "com dois ele fecha -- e dois e o numero que define o bicho");

        SquadOrder recuo = new SquadOrder(null, true, true, SquadRole.FLANKER);
        assertEquals(DecisaoDeFlanco.RECUAR, flanco.decidir(recuo, 8, 0.1D, -1.0D),
                "moral quebrada vence um alvo na cara: e isso que faz o grupo recuar JUNTO em vez"
                        + " de cada um decidir sozinho e a fuga sair em fila indiana");

        SquadOrder reagrupar = new SquadOrder(null, false, true, SquadRole.FLANKER);
        assertEquals(DecisaoDeFlanco.AGUARDAR, flanco.decidir(reagrupar, 4, 0.1D, -1.0D),
                "sem alvo utilizavel ele espera, e nao corre para a ultima posicao conhecida"
                        + " desmanchando o cerco dos outros");
    }

    @Test
    @DisplayName("medida quebrada nao vira permissao para investir")
    void medidaQuebradaNaoViraInvestida() {
        RegrasDeFlanco flanco = regras();
        SquadOrder ordem = engajar(SquadRole.FLANKER);
        assertThrows(IllegalArgumentException.class,
                () -> flanco.decidir(ordem, 4, Double.NaN, 0.0D),
                "o lado seguro de uma distancia quebrada e o bicho nao se comprometer");
        assertThrows(IllegalArgumentException.class,
                () -> flanco.decidir(ordem, 4, 1.0D, 7.0D),
                "cosseno fora de [-1, 1] e medida quebrada, e medida quebrada nao pode autorizar"
                        + " o golpe");
        assertThrows(IllegalArgumentException.class, () -> flanco.decidir(ordem, -1, 1.0D, 0.0D));
    }

    // ------------------------------------------- os casos que DEVEM reprovar

    @Test
    @DisplayName("PORTAO: posto DENTRO do arco frontal e recusado -- ele nunca atacaria")
    void postoDentroDoArcoFrontalEhRecusado() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFlanco(SquadRules.esquadrao(), MEMBROS,
                        ARCO_FRONTAL - 5.0D, ARCO_FRONTAL, INVESTIDA, CONTORNO));
        assertTrue(erro.getMessage().contains("NUNCA"),
                "a recusa tem de dizer o que acontece em jogo -- um mob que gira em volta do"
                        + " jogador para sempre e nunca ataca. Mensagem: " + erro.getMessage());

        // O caso legitimo continua passando: regua que reprova o certo e pior do
        // que regua que aprova o errado, porque proibe o ajuste seguro.
        assertEquals(ARCO_FRONTAL + 0.5D,
                new RegrasDeFlanco(SquadRules.esquadrao(), MEMBROS, ARCO_FRONTAL + 0.5D,
                        ARCO_FRONTAL, INVESTIDA, CONTORNO).anguloDeFlancoEmGraus(), 0.0D);
    }

    @Test
    @DisplayName("PORTAO: arco apertado demais poe os dois flancos no mesmo ponto")
    void arcoApertadoEhRecusado() {
        double espacamento = SquadRules.esquadrao().espacamento();
        // 1.4 de raio deixa a corda entre os dois flancos em ~2.76 blocos, abaixo
        // dos 3.0 exigidos -- e ainda assim maior que a distancia de investida, o
        // que faz a recusa ter de vir da conta da corda e nao do outro portao.
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFlanco(SquadRules.esquadrao(), MEMBROS, FLANCO, ARCO_FRONTAL,
                        INVESTIDA, 1.4D));
        assertTrue(erro.getMessage().contains("espacamento exigido"),
                "a recusa precisa mostrar a conta: " + erro.getMessage());
        assertTrue(2.0D * CONTORNO * Math.sin(Math.toRadians(FLANCO)) >= espacamento,
                "o raio em uso tem de satisfazer a mesma conta que o portao cobra");
    }

    @Test
    @DisplayName("PORTAO: contorno menor que a investida, e teto menor que o exigido, sao recusados")
    void geometriaImpossivelEhRecusada() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFlanco(SquadRules.esquadrao(), MEMBROS, FLANCO, ARCO_FRONTAL,
                        CONTORNO, INVESTIDA),
                "circular dentro da distancia de mordida e nao circular");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFlanco(SquadRules.esquadrao(), 1, FLANCO, ARCO_FRONTAL,
                        INVESTIDA, CONTORNO),
                "flanquear sozinho nao e flanquear");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFlanco(SquadRules.esquadrao(),
                        SquadRules.esquadrao().maximoDeMembros() + 1, FLANCO, ARCO_FRONTAL,
                        INVESTIDA, CONTORNO),
                "exigir mais membros do que o teto permite faz o bicho recuar para sempre, e o"
                        + " unico sinal e um inimigo que nunca ataca");
    }
}
