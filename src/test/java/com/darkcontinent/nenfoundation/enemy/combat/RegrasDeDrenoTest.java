package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A REGRA do dreno, com numeros proprios -- nunca com os de producao.
 *
 * <p>Os valores que o Mosquito Officer leva para o jogo sao provados em
 * {@code MosquitoOfficerTuningTest}, contra a ficha dele. Aqui se prova a regra
 * sozinha: a conta, a ordem das recusas e o teto. Misturar as duas coisas faria
 * uma sessao de balanceamento quebrar testes de regra, e o reflexo seria afrouxar
 * a regra em vez de discutir o numero.</p>
 *
 * <p>Nenhuma das falhas cobertas aqui levanta excecao em jogo: o mod carrega, o
 * oficial nasce, pica e cura. So o encontro deixa de ter fim.</p>
 */
class RegrasDeDrenoTest {

    /** Metade do dano volta; teto de 10; piso de meio ponto. Numeros do TESTE. */
    private static final RegrasDeDreno REGRA = new RegrasDeDreno(0.5F, 10.0F, 0.5F);

    private static final float VIDA_MAXIMA = 55.0F;
    private static final float MEIA_VIDA = 27.5F;

    // ------------------------------------------------------------ caso normal

    @Test
    @DisplayName("picada que entra devolve a fracao do dano APLICADO")
    void oCasoNormal() {
        ResultadoDeDreno r = REGRA.decidir(8.0F, true, true, MEIA_VIDA, VIDA_MAXIMA, 0.0F);
        assertEquals(DecisaoDeDreno.DRENA, r.motivo());
        assertEquals(4.0F, r.cura(),
                "8 de dano aplicado a metade sao 4 de vida; qualquer outra conta aqui e uma"
                        + " segunda regra de dreno vivendo fora desta classe");
    }

    @Test
    @DisplayName("a cura nunca passa da vida que falta, e o excedente NAO fica guardado")
    void nuncaCuraAlemDoMaximo() {
        ResultadoDeDreno r = REGRA.decidir(8.0F, true, true, VIDA_MAXIMA - 1.0F, VIDA_MAXIMA, 0.0F);
        assertEquals(DecisaoDeDreno.DRENA, r.motivo());
        assertEquals(1.0F, r.cura(),
                "faltava 1 de vida: curar 4 empurraria a vida acima do maximo, e o que passa do"
                        + " maximo some sem que nada acuse");
    }

    // --------------------------------------------------------------- o teto

    @Test
    @DisplayName("o teto CORTA a ultima picada em vez de deixar passar")
    void oTetoCortaAUltimaPicada() {
        ResultadoDeDreno r = REGRA.decidir(8.0F, true, true, 10.0F, VIDA_MAXIMA, 8.0F);
        assertEquals(DecisaoDeDreno.DRENA, r.motivo());
        assertEquals(2.0F, r.cura(),
                "restavam 2 de teto e a picada valia 4: passar os 4 faria o teto ser um numero"
                        + " decorativo, ultrapassado uma vez por combate e nunca mais cobrado");
    }

    @Test
    @DisplayName("com o teto gasto ela para de drenar, e o motivo diz qual limite bateu")
    void oTetoRecusa() {
        ResultadoDeDreno r = REGRA.decidir(8.0F, true, true, 10.0F, VIDA_MAXIMA, 10.0F);
        assertEquals(DecisaoDeDreno.TETO_ATINGIDO, r.motivo());
        assertEquals(0.0F, r.cura());
    }

    @Test
    @DisplayName("cem picadas seguidas nao passam do teto -- e e isso que o teto significa")
    void oTetoSeguraUmCombateLongo() {
        float acumulado = 0.0F;
        int picadasQueCuraram = 0;
        for (int picada = 0; picada < 100; picada++) {
            ResultadoDeDreno r = REGRA.decidir(8.0F, true, true, 5.0F, VIDA_MAXIMA, acumulado);
            if (r.motivo() != DecisaoDeDreno.DRENA) continue;
            acumulado += r.cura();
            picadasQueCuraram++;
        }
        assertEquals(10.0F, acumulado,
                "sem teto, cem picadas devolveriam 400 de vida a um bicho de 55: o combate longo"
                        + " nunca termina, e nao ha erro nenhum para procurar");
        assertTrue(picadasQueCuraram > 1 && picadasQueCuraram < 100,
                "o teto tem de ser alcancavel e tem de chegar: com uma picada so ele seria um"
                        + " limite que ninguem sente, e com cem ele nao seria limite");
    }

    // ----------------------------------------------------- os acertos recusados

    @Test
    @DisplayName("o acerto que NAO deveria curar: encostou e nao tirou vida nenhuma")
    void acertoSemDanoNaoCura() {
        ResultadoDeDreno r = REGRA.decidir(0.0F, true, true, MEIA_VIDA, VIDA_MAXIMA, 0.0F);
        assertEquals(DecisaoDeDreno.SEM_DANO, r.motivo(),
                "escudo, absorcao, invulnerabilidade e modo criativo comem o dano inteiro. Curar"
                        + " aqui pagaria por um dano que nunca aconteceu, e a barra dela subiria"
                        + " enquanto a do jogador nao desce -- sem uma linha de log");
        assertEquals(0.0F, r.cura());
    }

    @Test
    @DisplayName("golpe que nao foi dela nao alimenta ninguem")
    void golpeDeOutroNaoCura() {
        assertEquals(DecisaoDeDreno.GOLPE_DE_OUTRO,
                REGRA.decidir(8.0F, false, true, MEIA_VIDA, VIDA_MAXIMA, 0.0F).motivo(),
                "sem esta porta, fogo, queda e a espada de outro jogador alimentariam o oficial,"
                        + " e ele ficaria mais forte quanto mais gente lutasse por perto");
    }

    @Test
    @DisplayName("vitima sem o que dar nao da: a colonia nao vira bateria")
    void vitimaNaoDrenavelNaoCura() {
        assertEquals(DecisaoDeDreno.VITIMA_NAO_DRENAVEL,
                REGRA.decidir(8.0F, true, false, MEIA_VIDA, VIDA_MAXIMA, 0.0F).motivo(),
                "drenar outra formiga faria um esquadrao encurralado se curar em circulo");
    }

    @Test
    @DisplayName("cheia de vida ela nao drena, e o motivo NAO e o teto")
    void jaCheiaNaoCura() {
        assertEquals(DecisaoDeDreno.JA_ESTA_CHEIA,
                REGRA.decidir(8.0F, true, true, VIDA_MAXIMA, VIDA_MAXIMA, 0.0F).motivo(),
                "motivo trocado manda a proxima pessoa procurar um teto quebrado onde so havia"
                        + " um bicho inteiro");
    }

    @Test
    @DisplayName("respingo de dano nao gasta teto")
    void curaPequenaDemaisNaoCura() {
        assertEquals(DecisaoDeDreno.CURA_PEQUENA_DEMAIS,
                REGRA.decidir(0.6F, true, true, MEIA_VIDA, VIDA_MAXIMA, 0.0F).motivo(),
                "0.6 de dano valem 0.3 de cura: sem piso, o teto acabaria sem que nada visivel"
                        + " tivesse acontecido na tela");
    }

    @Test
    @DisplayName("a ordem das recusas e a ordem da historia")
    void golpeDeOutroVenceTodoORestO() {
        // Golpe de outro, contra vitima invalida, sem dano, com o teto gasto e ela
        // cheia: TODAS as recusas valem ao mesmo tempo. A primeira e que importa,
        // porque e ela que descreve o que de fato aconteceu.
        assertEquals(DecisaoDeDreno.GOLPE_DE_OUTRO,
                REGRA.decidir(0.0F, false, false, VIDA_MAXIMA, VIDA_MAXIMA, 99.0F).motivo());
    }

    // ------------------------------------------------- o que DEVE reprovar

    @Test
    @DisplayName("a regua morde: combinacao que nunca pagaria e recusada na construcao")
    void aReguaReprovaOQueNuncaPagaria() {
        IllegalArgumentException piso = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeDreno(0.5F, 4.0F, 4.0F),
                "piso igual ao teto: nenhuma picada jamais pagaria, e o bicho inteiro viraria um"
                        + " oficial fraco sem mecanica nenhuma");
        assertTrue(piso.getMessage().contains("cura minima"),
                "a mensagem tem de dizer O QUE foi medido; so acusar nao ensina");

        assertThrows(IllegalArgumentException.class, () -> new RegrasDeDreno(1.5F, 10.0F, 0.5F),
                "fracao acima de 1 devolveria mais vida do que a picada tirou");
        assertThrows(IllegalArgumentException.class, () -> new RegrasDeDreno(0.5F, 0.0F, 0.5F),
                "teto zero e o mesmo que dreno desligado, e desligar se faz tirando o consumidor");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeDreno(Float.NaN, 10.0F, 0.5F));
    }

    @Test
    @DisplayName("a regua morde: resultado incoerente e recusado na construcao")
    void aReguaReprovaResultadoIncoerente() {
        assertThrows(IllegalArgumentException.class,
                () -> new ResultadoDeDreno(DecisaoDeDreno.DRENA, 0.0F),
                "DRENA sem cura e um teto gasto por nada");
        assertThrows(IllegalArgumentException.class,
                () -> new ResultadoDeDreno(DecisaoDeDreno.SEM_DANO, 3.0F),
                "cura junto de uma recusa e um dreno que ninguem contabiliza no teto");
        assertThrows(IllegalArgumentException.class,
                () -> ResultadoDeDreno.recusa(DecisaoDeDreno.DRENA));
    }

    @Test
    @DisplayName("estado impossivel na chamada reprova em vez de curar um valor qualquer")
    void aReguaReprovaEstadoImpossivel() {
        assertThrows(IllegalArgumentException.class,
                () -> REGRA.decidir(8.0F, true, true, MEIA_VIDA, 0.0F, 0.0F),
                "vida maxima zero faria a conta de 'quanto falta' devolver um negativo, e a"
                        + " recusa sairia como JA_ESTA_CHEIA num bicho que nao tem vida nenhuma");
        assertThrows(IllegalArgumentException.class,
                () -> REGRA.decidir(8.0F, true, true, MEIA_VIDA, VIDA_MAXIMA, -1.0F),
                "acumulado negativo daria teto de graca, e o teto e a unica coisa que impede o"
                        + " combate longo de nunca acabar");
    }
}
