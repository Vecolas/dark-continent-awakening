package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.content.BatScoutTuning;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sustenta a frase que define o bicho: <b>ele nao luta.</b>
 *
 * <p>Um batedor que persegue e um mob de combate de 3 de dano, ou seja um inseto
 * chato, e o encontro deixa de ser sobre calar o mensageiro. Nada disso apareceria
 * como erro: o mob nasceria, atacaria, droparia loot e passaria em todos os outros
 * portoes. O que esta bateria fixa e a ORDEM das decisoes, porque e a ordem que e
 * a personalidade -- e inverter um par nao quebra nada visivel.</p>
 */
class RegrasDeVooDeBatedorTest {

    private static final RegrasDeVooDeBatedor REGRAS = BatScoutTuning.voo();
    private static final double ALTURA = BatScoutTuning.ALTURA_DE_VOO_PREFERIDA;
    private static final double FUGA = BatScoutTuning.DISTANCIA_DE_FUGA;
    private static final double MORDIDA = BatScoutTuning.ALCANCE_DA_MORDIDA;

    @Test
    @DisplayName("o caso normal e FUGIR subindo, e nunca atacar")
    void oCasoNormalEhFugirSubindo() {
        DecisaoDeVoo decisao = REGRAS.decidir(2.0D, 0.0D, false, false);
        assertEquals(MovimentoDeVoo.AFASTAR, decisao.movimento(),
                "com o jogador a dois blocos e ceu livre, o batedor se afasta -- ele ja relatou,"
                        + " e o que ele quer agora e viver");
        assertEquals(ALTURA, decisao.ganhoDeAltura(), 1.0E-9D,
                "e ele sobe ENQUANTO afasta. Alternar os dois faria o batedor cruzar a altura do"
                        + " jogador no meio da fuga, entregando de graca o unico instante em que"
                        + " um arco acerta um bicho de 16 de vida");

        DecisaoDeVoo noAlto = REGRAS.decidir(2.0D, ALTURA + 4.0D, false, false);
        assertEquals(MovimentoDeVoo.AFASTAR, noAlto.movimento());
        assertEquals(0.0D, noAlto.ganhoDeAltura(), 1.0E-9D,
                "ja acima da altura preferida ele nao sobe mais: sem este piso o batedor subiria"
                        + " para sempre e sairia do chunk, e nada acusaria a fuga vertical");
    }

    @Test
    @DisplayName("longe do alvo ele sobe; ja na altura certa, ele so paira")
    void longeEleGanhaAlturaEDepoisPaira() {
        assertEquals(MovimentoDeVoo.SUBIR, REGRAS.decidir(FUGA + 3.0D, 0.0D, false, false).movimento(),
                "longe do alvo e baixo, ele ganha altura: altura e o que torna a proxima fuga"
                        + " possivel, e ganha-la longe e barato");
        assertEquals(MovimentoDeVoo.PAIRAR,
                REGRAS.decidir(FUGA + 3.0D, ALTURA, false, false).movimento(),
                "longe e ja na altura certa, ele vigia -- que e a resposta mais comum deste"
                        + " bicho, e nao passividade");
    }

    @Test
    @DisplayName("a mordida so existe ENCURRALADO e colado: e essa a unica recusa que sobra")
    void aMordidaEhOQueSobraQuandoFugirNaoDa() {
        assertEquals(MovimentoDeVoo.MORDER, REGRAS.decidir(MORDIDA, 0.0D, true, false).movimento(),
                "encurralado e exatamente no alcance, ele morde: e o unico uso dos 3 de dano");
        assertEquals(MovimentoDeVoo.PAIRAR,
                REGRAS.decidir(MORDIDA + 0.01D, 0.0D, true, false).movimento(),
                "encurralado e um centimetro alem do alcance ele NAO morde -- ele espera. Sem"
                        + " esta recusa a mordida viraria perseguicao dentro da caverna");
        assertEquals(MovimentoDeVoo.AFASTAR, REGRAS.decidir(MORDIDA, 0.0D, false, false).movimento(),
                "colado mas com saida, ele foge. ESTE e o caso que separa o batedor de um mob de"
                        + " combate: um enum trocado aqui daria um bicho que ataca sempre que"
                        + " estiver perto, e nada reprovaria isso");
    }

    @Test
    @DisplayName("cambalear vem antes de tudo: quem perde o voo CAI")
    void oCambaleioVenceTodasAsOutrasDecisoes() {
        for (boolean encurralado : new boolean[] {false, true}) {
            for (double distancia : new double[] {0.0D, MORDIDA, FUGA, FUGA + 10.0D}) {
                DecisaoDeVoo decisao = REGRAS.decidir(distancia, 0.0D, encurralado, true);
                assertEquals(MovimentoDeVoo.CAIR, decisao.movimento(),
                        "cambaleando a " + distancia + " blocos (encurralado=" + encurralado
                                + ") o batedor cai: se qualquer outro ramo vencesse, interromper"
                                + " este bicho deixaria de ter recompensa visivel");
                assertEquals(0.0D, decisao.ganhoDeAltura(), 1.0E-9D,
                        "quem esta caindo nao ganha altura");
                assertTrue(!decisao.controlaOVoo(), "e nao manda no proprio voo");
            }
        }
    }

    @Test
    @DisplayName("leitura impossivel vira PAIRAR, e nao excecao no meio de um tick")
    void medidaQuebradaViraRecusaSegura() {
        for (double ruim : new double[] {Double.NaN, Double.POSITIVE_INFINITY, -1.0D}) {
            assertEquals(MovimentoDeVoo.PAIRAR, REGRAS.decidir(ruim, 0.0D, true, false).movimento(),
                    "distancia " + ruim + " nao pode virar mordida de graca nem derrubar o tick;"
                            + " pairar e a recusa segura -- ele nao foge para lugar nenhum e nao"
                            + " morde ninguem");
        }
        assertEquals(MovimentoDeVoo.PAIRAR, REGRAS.decidir(2.0D, Double.NaN, false, false).movimento(),
                "altura NaN viraria um ganho NaN e uma posicao de destino NaN, e o mob pararia de"
                        + " se mover sem uma linha de log");
    }

    @Test
    @DisplayName("o recuo ao levar dano existe, e some exatamente quando devia sumir")
    void oRecuoVerticalDistingueAcertarDeAcertarOBastante() {
        assertEquals(BatScoutTuning.RECUO_VERTICAL, REGRAS.recuoAoLevarDano(true, false), 1.0E-9D,
                "vivo e inteiro, ele SALTA ao apanhar: e o reflexo que faz o bicho ler como"
                        + " voador em vez de como mob fraco");
        assertEquals(0.0D, REGRAS.recuoAoLevarDano(true, true), 1.0E-9D,
                "interrompido, nao: acertar o bastante tem de parecer diferente de acertar");
        assertEquals(0.0D, REGRAS.recuoAoLevarDano(false, false), 1.0E-9D,
                "morto tambem nao -- um cadaver que salta e um cadaver voando");
    }

    @Test
    @DisplayName("numero que apagaria um comportamento reprova na construcao")
    void oConstrutorReprovaOQueNaoDariaErroEmJogo() {
        // ESTE E O CASO QUE DEVE REPROVAR. Alcance de mordida maior ou igual a
        // distancia de fuga nao quebra nada visivel: o bicho nasce, voa, morde e o
        // log fica limpo. O que some e a leitura de "ele so ataca sem saida" --
        // encurralado a qualquer distancia util ele passaria a morder sempre.
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeVooDeBatedor(3.5D, 7.0D, 7.0D, 0.42D),
                "mordida alcancando tao longe quanto a fuga tem de reprovar aqui");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeVooDeBatedor(3.5D, 7.0D, 9.0D, 0.42D),
                "e mais longe ainda e a mesma coisa, escrita de outro jeito");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeVooDeBatedor(0.0D, 7.0D, 0.55D, 0.42D),
                "altura preferida zero faz o batedor voar rente ao chao, e ele deixa de ser um"
                        + " alvo dificil sem que nada mude no codigo");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeVooDeBatedor(3.5D, 0.0D, 0.55D, 0.42D),
                "distancia de fuga zero e um batedor que nunca foge");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeVooDeBatedor(3.5D, 7.0D, 0.55D, Double.NaN),
                "recuo NaN viraria uma velocidade NaN, e a entidade sairia do mundo");
        assertThrows(IllegalArgumentException.class, () -> new DecisaoDeVoo(MovimentoDeVoo.SUBIR, -1.0D),
                "ganho de altura negativo seria um batedor que ESCOLHE descer, e descer e o que o"
                        + " cambaleio impoe -- nunca o que ele escolhe");
        assertThrows(NullPointerException.class, () -> new DecisaoDeVoo(null, 0.0D),
                "decisao sem movimento viraria NullPointerException no meio de um tick, com pilha"
                        + " que nao diz qual regra deixou de responder");
    }
}
