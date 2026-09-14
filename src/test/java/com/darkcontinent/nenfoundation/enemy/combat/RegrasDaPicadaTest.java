package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A REGRA da picada de flanco, com numeros proprios -- nunca com os de producao.
 *
 * <p>O que ela prova e a unica coisa que separa um atacante de flanco de um mob
 * rapido qualquer: a RECUSA. Um mob que ataca de qualquer angulo e perfeitamente
 * normal aos olhos de quem joga e de todo portao que este repositorio tem; a
 * unica coisa que denuncia a regra ausente e o jogador nunca descobrir que havia
 * uma defesa para aprender.</p>
 */
class RegrasDaPicadaTest {

    /** Sessenta graus de arco proibido, anel em 5, agulha em 1. Numeros do TESTE. */
    private static final double COSSENO_DE_SESSENTA_GRAUS = 0.5D;
    private static final RegrasDaPicada REGRA =
            new RegrasDaPicada(COSSENO_DE_SESSENTA_GRAUS, 5.0D, 1.0D);

    /** Pelas costas exatas do alvo. */
    private static final double PELAS_COSTAS = -1.0D;
    /** Bem na cara do alvo. */
    private static final double DE_FRENTE = 1.0D;

    // ------------------------------------------------------------ caso normal

    @Test
    @DisplayName("pelas costas, ao alcance e com caminho: ela pica")
    void oCasoNormal() {
        assertEquals(DecisaoDaPicada.PICA,
                REGRA.decidir(true, false, 0.8D, PELAS_COSTAS, true));
    }

    @Test
    @DisplayName("a borda do arco pertence a quem esta OLHANDO, e nao a quem ataca")
    void aBordaDoArcoEDoAlvo() {
        assertEquals(DecisaoDaPicada.ENCARADA,
                REGRA.decidir(true, false, 0.8D, COSSENO_DE_SESSENTA_GRAUS + 1.0E-9D, true),
                "um fio dentro do arco ja e dentro");
        assertEquals(DecisaoDaPicada.PICA,
                REGRA.decidir(true, false, 0.8D, COSSENO_DE_SESSENTA_GRAUS, true),
                "exatamente na borda ela ataca: a comparacao e estritamente maior, e trocar por"
                        + " maior-ou-igual moveria a fronteira meio grau sem que nada acusasse");
    }

    // ----------------------------------------------------- as quatro recusas

    @Test
    @DisplayName("o alvo encarando recusa o ataque, mesmo colado")
    void encararRecusa() {
        assertEquals(DecisaoDaPicada.ENCARADA,
                REGRA.decidir(true, false, 0.1D, DE_FRENTE, true),
                "sem esta recusa o oficial mergulha de frente contra quem o esta olhando, e a"
                        + " unica defesa que o encontro ensina -- manter o zumbido na tela --"
                        + " deixa de funcionar; nada disso levanta erro");
    }

    @Test
    @DisplayName("encarar VENCE a distancia, e a ordem e a regra")
    void encararVenceADistancia() {
        assertEquals(DecisaoDaPicada.ENCARADA,
                REGRA.decidir(true, false, 40.0D, DE_FRENTE, true),
                "invertida a ordem, um alvo distante e de frente sairia como LONGE, o oficial"
                        + " fecharia a distancia pela frente e so descobriria o arco proibido"
                        + " colado no jogador -- o que le como bicho indeciso");
    }

    @Test
    @DisplayName("longe demais nao e recusa de angulo")
    void longeRecusaPorDistancia() {
        assertEquals(DecisaoDaPicada.LONGE,
                REGRA.decidir(true, false, 1.01D, PELAS_COSTAS, true),
                "um centimetro fora do alcance ja e fora: a agulha tem sessenta centimetros, e"
                        + " uma folga aqui viraria um golpe que sai e nunca encosta");
    }

    @Test
    @DisplayName("parede recusa antes do angulo")
    void paredeRecusa() {
        assertEquals(DecisaoDaPicada.SEM_LINHA_DE_VISAO,
                REGRA.decidir(true, false, 0.5D, PELAS_COSTAS, false),
                "sem esta porta ela picaria atraves da parede em que o jogador se abrigou --"
                        + " dano certo, cooldown certo, log limpo, e um bicho que atravessa blocos");
    }

    @Test
    @DisplayName("sem alvo nao ha decisao nenhuma a tomar")
    void semAlvo() {
        assertEquals(DecisaoDaPicada.SEM_ALVO,
                REGRA.decidir(false, false, 0.5D, PELAS_COSTAS, true));
    }

    @Test
    @DisplayName("quem recolheu a aura nao ataca, e a recusa vem antes de tudo")
    void escondidaRecusaAntesDeQualquerMedida() {
        assertEquals(DecisaoDaPicada.ESCONDIDA,
                REGRA.decidir(true, true, 0.1D, PELAS_COSTAS, true),
                "a postura de quem sumiu vence a oportunidade perfeita: atacar em Zetsu desfaria"
                        + " a unica coisa que ele comprou");
        assertEquals(DecisaoDaPicada.SEM_ALVO,
                REGRA.decidir(false, true, 0.1D, PELAS_COSTAS, true),
                "sem alvo vem antes de escondida: nao ha o que esconder de ninguem");
    }

    // ------------------------------------------------- o que DEVE reprovar

    @Test
    @DisplayName("a regua morde: anel dentro do alcance e recusado na construcao")
    void aReguaReprovaOAnelDentroDoAlcance() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDaPicada(0.5D, 1.0D, 1.0D),
                "anel igual ao alcance: reposicionar deixaria o oficial parado onde ele ja podia"
                        + " picar, e ele oscilaria entre recuar e atacar no mesmo ponto");
        assertTrue(erro.getMessage().contains("raio do contorno"),
                "a mensagem tem de dizer O QUE foi medido; so acusar nao ensina");
    }

    @Test
    @DisplayName("a regua morde: arco que apaga o flanco e recusado na construcao")
    void aReguaReprovaOArcoDegenerado() {
        assertThrows(IllegalArgumentException.class, () -> new RegrasDaPicada(1.0D, 5.0D, 1.0D),
                "cosseno 1 faz ela atacar de qualquer angulo, e o flanco deixa de existir -- um"
                        + " mob rapido atacando de frente parece perfeitamente normal");
        assertThrows(IllegalArgumentException.class, () -> new RegrasDaPicada(-1.0D, 5.0D, 1.0D),
                "cosseno -1 exige as costas exatas e ela nunca ataca");
        assertThrows(IllegalArgumentException.class, () -> new RegrasDaPicada(0.5D, 5.0D, 0.0D),
                "alcance zero e um golpe que nunca encosta");
    }

    @Test
    @DisplayName("medida impossivel reprova em vez de virar um bicho que nunca ataca")
    void aReguaReprovaMedidaImpossivel() {
        assertThrows(IllegalArgumentException.class,
                () -> REGRA.decidir(true, false, Double.NaN, PELAS_COSTAS, true),
                "NaN nao daria erro na comparacao: toda comparacao com ele e falsa, a decisao"
                        + " cairia em PICA e o oficial atacaria de um lugar que ninguem mediu");
        assertThrows(IllegalArgumentException.class,
                () -> REGRA.decidir(true, false, -1.0D, PELAS_COSTAS, true));
        assertThrows(IllegalArgumentException.class,
                () -> REGRA.decidir(true, false, 0.5D, Double.NaN, true));
    }
}
