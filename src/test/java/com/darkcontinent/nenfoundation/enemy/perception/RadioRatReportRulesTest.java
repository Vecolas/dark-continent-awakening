package com.darkcontinent.nenfoundation.enemy.perception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.content.RadioRatTuning;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sustenta que o relatorio do Radio Rat e uma JANELA, e nao um estado.
 *
 * <p>O bicho inteiro se resume a uma frase -- matar o mensageiro antes do grito
 * e a resposta -- e essa frase so e verdadeira enquanto as quatro condicoes de
 * {@link RadioRatReportRules#denuncia} viverem juntas. Espalhadas pela Goal,
 * cada uma vira um {@code if} diferente e a divergencia nao da erro nenhum: da
 * um rato que grita cambaleando (e interromper deixa de valer), ou que grita no
 * mesmo tick em que ve alguem (e nao ha janela para reagir).</p>
 *
 * <p>E sustenta a outra metade: o aviso e SOM, nao alvo. O que decide se o
 * vizinho aproveita o relatorio e o alcance de audicao DELE vezes a intensidade
 * daqui -- e e por isso que a intensidade e o botao mais afiado deste bicho.</p>
 */
class RadioRatReportRulesTest {

    /** Os mesmos numeros de {@code RadioRatTuning.relatorio()}. */
    private static final RadioRatReportRules REGRAS = RadioRatTuning.relatorio();
    private static final int OBSERVACAO = RadioRatTuning.TICKS_DE_OBSERVACAO;
    private static final UUID ALVO = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    @Test
    @DisplayName("com alvo, inteiro e com a recarga zerada, o rato denuncia")
    void oCasoNormalDenuncia() {
        assertTrue(REGRAS.denuncia(true, false, true, OBSERVACAO),
                "alvo vivo, sem cambaleio, golpe liberado e observacao cumprida: e o grito");
        assertTrue(REGRAS.denuncia(true, false, true, OBSERVACAO + 999),
                "observar por mais tempo nao desarma nada; o limiar e um piso, nao uma faixa");
    }

    @Test
    @DisplayName("cada condicao ausente recusa o grito, e por um motivo diferente")
    void cadaRecusaTemMotivo() {
        assertFalse(REGRAS.denuncia(false, false, true, OBSERVACAO),
                "sem alvo nao ha o que denunciar -- o grito seria um aviso sobre ninguem");
        assertFalse(REGRAS.denuncia(true, true, true, OBSERVACAO),
                "cambaleando o grito TEM de ser cortado: e isso que faz interromper valer a pena");
        assertFalse(REGRAS.denuncia(true, false, false, OBSERVACAO),
                "recarga correndo bloqueia o grito em cadeia, que e o que transformaria o aviso"
                        + " numa varredura de mundo por tick disfarcada de gameplay");
        assertFalse(REGRAS.denuncia(true, false, true, OBSERVACAO - 1),
                "um tick antes da observacao cumprida ainda nao denuncia: e essa a janela que"
                        + " separa 'ele me viu' de 'ele contou'");
        assertFalse(REGRAS.denuncia(true, false, true, 0),
                "o alvo que acabou de aparecer nao vira relatorio imediato");
    }

    @Test
    @DisplayName("o raio do grito e inclusivo, e distancia impossivel nao alcanca ninguem")
    void oRaioRecusaOQueNaoDaParaMedir() {
        assertTrue(REGRAS.alcanca(0.0D), "o vizinho colado esta dentro");
        assertTrue(REGRAS.alcanca(RadioRatTuning.RAIO_DO_RELATORIO),
                "exatamente o raio ainda alcanca");
        assertFalse(REGRAS.alcanca(RadioRatTuning.RAIO_DO_RELATORIO + 0.01D),
                "um centimetro alem do raio ja esta fora");
        assertFalse(REGRAS.alcanca(Double.NaN),
                "NaN nao pode virar aviso de graca: comparacao com NaN ja e falsa nas duas pontas,"
                        + " e depender desse acidente e o que faz a regra parar de dizer o que faz");
        assertFalse(REGRAS.alcanca(Double.POSITIVE_INFINITY), "infinito nao esta dentro de raio nenhum");
        assertFalse(REGRAS.alcanca(-1.0D), "distancia negativa e leitura quebrada, nao vizinho perto");
    }

    @Test
    @DisplayName("o relatorio carrega a distancia de QUEM OUVE ate o alvo")
    void oRelatorioCarregaADistanciaDoOuvinte() {
        HearingEvent evento = REGRAS.relatorio(ALVO, 7.5D).orElseThrow();
        assertEquals(ALVO, evento.fonte(), "a fonte do som e o jogador denunciado, nao o rato");
        assertEquals(7.5D, evento.distancia(), 1.0E-9D,
                "a distancia e a do vizinho ao alvo: mandar a do rato encheria a memoria de"
                        + " ameaca do vizinho com um numero que nao descreve a situacao dele");
        assertEquals(RadioRatTuning.INTENSIDADE_DO_RELATORIO, evento.intensidade(), 1.0E-9D);
    }

    @Test
    @DisplayName("distancia que nao da para medir vira recusa, e nao excecao no meio do tick")
    void oRelatorioRecusaDistanciaImpossivel() {
        assertTrue(REGRAS.relatorio(ALVO, Double.NaN).isEmpty(),
                "NaN aqui derrubaria o tick do servidor por causa de um vizinho em estado"
                        + " estranho: perder um aviso e mais barato que perder o mob");
        assertTrue(REGRAS.relatorio(ALVO, -0.5D).isEmpty(), "distancia negativa nao e medida");
        assertTrue(REGRAS.relatorio(null, 3.0D).isEmpty(), "som sem fonte nao reconstroi alvo nenhum");
    }

    @Test
    @DisplayName("quem decide se o aviso serve e o alcance de audicao de quem ouve")
    void aIntensidadeEncurtaOAvisoDoLadoDeQuemOuve() {
        // 0.5 e 10.0 sao exatos em binario de proposito: o teste mede a REGRA e
        // nao o arredondamento, e um limite testado com 0.85 passaria ou nao
        // dependendo do ultimo bit do double.
        RadioRatReportRules metade = new RadioRatReportRules(20, 16.0D, 0.5D);
        assertTrue(metade.relatorio(ALVO, 5.0D).orElseThrow().audivel(10.0D),
                "exatamente alcance * intensidade ainda e audivel");
        assertFalse(metade.relatorio(ALVO, 5.1D).orElseThrow().audivel(10.0D),
                "alem disso o vizinho ouve o grito e NAO aprende nada -- e essa a diferenca"
                        + " entre entregar um som e entregar um alvo");

        assertTrue(REGRAS.relatorio(ALVO, 4.0D).orElseThrow().audivel(14.0D),
                "com os numeros de producao, um vizinho perto do alvo aproveita o aviso");
        assertFalse(REGRAS.relatorio(ALVO, 13.0D).orElseThrow().audivel(14.0D),
                "e um vizinho longe do alvo nao -- mesmo estando perto do rato que gritou");
    }

    @Test
    @DisplayName("numero que apagaria a resposta do encounter reprova na construcao")
    void oConstrutorReprovaOQueNaoDariaErroEmJogo() {
        // ESTE E O CASO QUE DEVE REPROVAR. Zero tick de observacao nao quebra
        // nada visivel: o rato grita, o relatorio sai, o log fica limpo. O que
        // some e a janela em que se mata o mensageiro -- ou seja, o bicho inteiro.
        assertThrows(IllegalArgumentException.class,
                () -> new RadioRatReportRules(0, 16.0D, 0.85D),
                "observacao zero tem de reprovar aqui, onde o numero foi escrito");
        assertThrows(IllegalArgumentException.class,
                () -> new RadioRatReportRules(-1, 16.0D, 0.85D),
                "observacao negativa e a mesma coisa, escrita de outro jeito");
        assertThrows(IllegalArgumentException.class,
                () -> new RadioRatReportRules(20, 0.0D, 0.85D),
                "raio zero e um relatorio que nunca sai da cabeca do rato");
        assertThrows(IllegalArgumentException.class,
                () -> new RadioRatReportRules(20, Double.NaN, 0.85D),
                "raio NaN aprovaria nada e reprovaria nada, e ninguem saberia qual dos dois");
        assertThrows(IllegalArgumentException.class,
                () -> new RadioRatReportRules(20, 16.0D, 1.5D),
                "intensidade acima de 1 estouraria dentro do HearingEvent, no meio de um tick de"
                        + " servidor, e nao aqui");
        assertThrows(IllegalArgumentException.class,
                () -> new RadioRatReportRules(20, 16.0D, 0.0D),
                "intensidade zero e um grito inaudivel: o mob existiria sem fazer nada");
    }
}
