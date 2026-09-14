package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do PRAZO do rastro -- o numero que separa "tenso" de "injusto".
 *
 * <p>Todos os defeitos que ele segura tem a mesma assinatura: compilam, o mob
 * nasce, persegue, morde e dropa loot. O que muda e o CARATER do encontro, e
 * carater nao aparece em nenhum outro portao deste repositorio.</p>
 */
class RegrasDeRastroTest {

    private static final int PRAZO = 100;
    private static final int MEMORIA = 140;
    private static final double RAIO = 1.5D;

    private static RegrasDeRastro regras() {
        return new RegrasDeRastro(PRAZO, RAIO, MEMORIA);
    }

    // ------------------------------------------------------------ caso normal

    @Test
    @DisplayName("DENTRO do prazo ele vai atras; DEPOIS do prazo ele desiste")
    void oPrazoDecideEntrePerseguirEDesistir() {
        RegrasDeRastro rastro = regras();
        double longe = RAIO + 20.0D;

        assertEquals(DecisaoDeRastro.SEGUIR_O_RASTRO, rastro.decidir(false, 0, longe),
                "acabou de perder de vista: o rastro esta inteiro");
        assertEquals(DecisaoDeRastro.SEGUIR_O_RASTRO, rastro.decidir(false, PRAZO - 1, longe),
                "um tick antes do fim o rastro AINDA vale -- se ele parasse aqui, o prazo"
                        + " declarado seria um tick mais curto do que o escrito, e ninguem"
                        + " perceberia");
        assertEquals(DecisaoDeRastro.DESISTIR, rastro.decidir(false, PRAZO, longe),
                "no tick do prazo ele larga. Sem esta ponta, 'perdi de vista' vira 'persigo para"
                        + " sempre': o bicho atravessa o mapa, nunca volta para casa e o jogador"
                        + " chama isso de injusto");
        assertEquals(DecisaoDeRastro.DESISTIR, rastro.decidir(false, PRAZO * 3, longe));

        assertFalse(rastro.expirou(PRAZO - 1));
        assertTrue(rastro.expirou(PRAZO));
    }

    @Test
    @DisplayName("ver o alvo vence tudo: a posicao de agora manda, nao o cheiro")
    void verOAlvoVenceORastro() {
        RegrasDeRastro rastro = regras();
        assertEquals(DecisaoDeRastro.PERSEGUIR_A_VISTA, rastro.decidir(true, 0, 30.0D));
        assertEquals(DecisaoDeRastro.PERSEGUIR_A_VISTA, rastro.decidir(true, PRAZO * 2, 30.0D),
                "com o alvo a vista, um prazo vencido nao pode mandar desistir -- o bicho"
                        + " desengajaria de alguem que esta na frente dele");
        assertEquals(DecisaoDeRastro.PERSEGUIR_A_VISTA, rastro.decidir(true, 0, 0.0D),
                "a vista e colado: ainda e perseguicao, e nao 'cheguei ao fim do cheiro'");
    }

    // --------------------------------------------------------------- recusado

    @Test
    @DisplayName("chegar ao fim do rastro e nao achar ninguem tambem encerra")
    void chegarAoPontoSemAlvoEncerra() {
        RegrasDeRastro rastro = regras();
        assertEquals(DecisaoDeRastro.DESISTIR, rastro.decidir(false, 10, RAIO),
                "em cima do ponto e sem ver ninguem: o cheiro acabou. Sem esta saida o bicho fica"
                        + " parado sobre o ponto ate o prazo vencer, e o jogador ve um mob"
                        + " travado");
        assertEquals(DecisaoDeRastro.SEGUIR_O_RASTRO, rastro.decidir(false, 10, RAIO + 0.01D),
                "um centimetro fora do raio ele ainda anda");
    }

    @Test
    @DisplayName("medida quebrada nao vira perseguicao: NaN e tick negativo sao recusa")
    void medidaQuebradaNaoViraPerseguicao() {
        RegrasDeRastro rastro = regras();
        assertThrows(IllegalArgumentException.class,
                () -> rastro.decidir(false, 10, Double.NaN),
                "NaN virando 'segue' mandaria o bicho perseguir um ponto que nao existe, e a"
                        + " unica pista seria um mob andando para o nada");
        assertThrows(IllegalArgumentException.class,
                () -> rastro.decidir(false, 10, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> rastro.decidir(false, -1, 10.0D),
                "ticks negativos seriam um rastro do futuro, e ele nunca expiraria");
    }

    // ------------------------------------------- o caso que DEVE reprovar

    @Test
    @DisplayName("PORTAO: rastro que dura mais que a memoria da ameaca e recusado")
    void oRastroNaoPodeSobreviverAMemoria() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeRastro(MEMORIA + 1, RAIO, MEMORIA));
        assertTrue(erro.getMessage().contains("memoria da ameaca"),
                "a recusa precisa dizer QUAL das duas pontas passou da outra; so acusar nao"
                        + " ensina. Mensagem: " + erro.getMessage());

        // E a regua tem de ACEITAR o limite exato -- uma regua que reprova o caso
        // legitimo e tao inutil quanto uma que aprova o ilegitimo, e esta seria
        // pior: o ajuste seguro passaria a parecer proibido.
        assertEquals(MEMORIA, new RegrasDeRastro(MEMORIA, RAIO, MEMORIA).ticksDeRastro());
    }

    @Test
    @DisplayName("PORTAO: prazo zero e raio zero sao recusados, com motivo")
    void prazoOuRaioZeradosSaoRecusados() {
        assertThrows(IllegalArgumentException.class, () -> new RegrasDeRastro(0, RAIO, MEMORIA),
                "um rastro que nasce vencido faz fugir ser dobrar uma esquina");
        assertThrows(IllegalArgumentException.class, () -> new RegrasDeRastro(PRAZO, 0.0D, MEMORIA),
                "com raio zero o bicho nunca 'chega' ao ponto e fica parado ali");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeRastro(PRAZO, Double.NaN, MEMORIA));
    }
}
