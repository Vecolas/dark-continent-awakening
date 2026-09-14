package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.content.BatScoutTuning;
import com.darkcontinent.nenfoundation.enemy.perception.HearingEvent;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sustenta que o relatorio do batedor e uma CADENCIA, e nao um estado.
 *
 * <p>O bicho inteiro se resume a uma frase -- ele relata e foge, e calar o
 * mensageiro e a resposta -- e essa frase so e verdadeira enquanto as quatro
 * condicoes de {@link RegrasDeRelatorioDeBatedor#relata} viverem juntas.
 * Espalhadas pelo tick da entidade, cada uma vira um {@code if} diferente e a
 * divergencia nao da erro nenhum: da um batedor que relata cambaleando (e
 * interromper deixa de valer), ou que relata todo tick (e a varredura de vizinhos
 * vira custo por tick numa colonia inteira).</p>
 *
 * <p>E sustenta a outra metade: o aviso e SOM, nao alvo. O que decide se o vizinho
 * aproveita e o alcance de audicao DELE vezes a intensidade daqui.</p>
 */
class RegrasDeRelatorioDeBatedorTest {

    private static final RegrasDeRelatorioDeBatedor REGRAS = BatScoutTuning.relatorio();
    private static final int OBSERVACAO = BatScoutTuning.TICKS_DE_OBSERVACAO;
    private static final int INTERVALO = BatScoutTuning.INTERVALO_ENTRE_RELATORIOS;
    private static final UUID ALVO = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    @Test
    @DisplayName("com alvo, inteiro, observado e com o intervalo cumprido, ele relata")
    void oCasoNormalRelata() {
        assertTrue(REGRAS.relata(true, false, OBSERVACAO, INTERVALO),
                "alvo vivo, sem cambaleio, observacao cumprida e intervalo vencido: e o aviso");
        assertTrue(REGRAS.relata(true, false, OBSERVACAO + 999, INTERVALO + 999),
                "esperar mais nao desarma nada; os dois limiares sao pisos, nao faixas");
    }

    @Test
    @DisplayName("cada condicao ausente recusa o aviso, e por um motivo diferente")
    void cadaRecusaTemMotivo() {
        assertFalse(REGRAS.relata(false, false, OBSERVACAO, INTERVALO),
                "sem alvo nao ha o que relatar -- o aviso seria sobre ninguem");
        assertFalse(REGRAS.relata(true, true, OBSERVACAO, INTERVALO),
                "cambaleando o aviso TEM de ser cortado: e isso que faz interromper valer a pena"
                        + " num bicho que passa a vida fora de alcance");
        assertFalse(REGRAS.relata(true, false, OBSERVACAO - 1, INTERVALO),
                "um tick antes da observacao cumprida ainda nao relata: e essa a janela que"
                        + " separa 'ele me viu' de 'ele contou'");
        assertFalse(REGRAS.relata(true, false, OBSERVACAO, INTERVALO - 1),
                "um tick antes do intervalo tambem nao: sem o intervalo, o aviso sairia a cada"
                        + " tick e a varredura de vizinhos viraria custo por tick");
        assertFalse(REGRAS.relata(true, false, 0, INTERVALO),
                "o alvo que acabou de aparecer nao vira relatorio imediato");
    }

    @Test
    @DisplayName("o raio do aviso e inclusivo, e distancia impossivel nao alcanca ninguem")
    void oRaioRecusaOQueNaoDaParaMedir() {
        assertTrue(REGRAS.alcanca(0.0D), "o vizinho colado esta dentro");
        assertTrue(REGRAS.alcanca(BatScoutTuning.RAIO_DO_AVISO), "exatamente o raio ainda alcanca");
        assertFalse(REGRAS.alcanca(BatScoutTuning.RAIO_DO_AVISO + 0.01D),
                "um centimetro alem do raio ja esta fora");
        assertFalse(REGRAS.alcanca(Double.NaN),
                "NaN nao pode virar aviso de graca: comparacao com NaN ja e falsa nas duas"
                        + " pontas, e depender desse acidente e o que faz a regra parar de dizer"
                        + " o que faz");
        assertFalse(REGRAS.alcanca(Double.POSITIVE_INFINITY),
                "infinito nao esta dentro de raio nenhum");
        assertFalse(REGRAS.alcanca(-1.0D), "distancia negativa e leitura quebrada, nao vizinho perto");
    }

    @Test
    @DisplayName("o aviso carrega a distancia de QUEM OUVE ate o alvo")
    void oAvisoCarregaADistanciaDoOuvinte() {
        HearingEvent evento = REGRAS.aviso(ALVO, 11.5D).orElseThrow();
        assertEquals(ALVO, evento.fonte(), "a fonte do som e o jogador denunciado, nao o batedor");
        assertEquals(11.5D, evento.distancia(), 1.0E-9D,
                "a distancia e a do vizinho ao alvo: mandar a do batedor encheria a memoria de"
                        + " ameaca do vizinho com um numero que nao descreve a situacao dele");
        assertEquals(BatScoutTuning.INTENSIDADE_DO_AVISO, evento.intensidade(), 1.0E-9D);
    }

    @Test
    @DisplayName("distancia que nao da para medir vira recusa, e nao excecao no meio do tick")
    void oAvisoRecusaDistanciaImpossivel() {
        assertTrue(REGRAS.aviso(ALVO, Double.NaN).isEmpty(),
                "NaN aqui derrubaria o tick do servidor por causa de um vizinho em estado"
                        + " estranho: perder um aviso e mais barato que perder o mob");
        assertTrue(REGRAS.aviso(ALVO, -0.5D).isEmpty(), "distancia negativa nao e medida");
        assertTrue(REGRAS.aviso(null, 3.0D).isEmpty(), "som sem fonte nao reconstroi alvo nenhum");
    }

    @Test
    @DisplayName("quem decide se o aviso serve e o alcance de audicao de quem ouve")
    void aIntensidadeEncurtaOAvisoDoLadoDeQuemOuve() {
        // 0.5 e 10.0 sao exatos em binario de proposito: o teste mede a REGRA e nao
        // o arredondamento, e um limite testado com 0.6 passaria ou nao dependendo
        // do ultimo bit do double.
        RegrasDeRelatorioDeBatedor metade = new RegrasDeRelatorioDeBatedor(15, 120, 24.0D, 0.5D, 1200);
        assertTrue(metade.aviso(ALVO, 5.0D).orElseThrow().audivel(10.0D),
                "exatamente alcance * intensidade ainda e audivel");
        assertFalse(metade.aviso(ALVO, 5.1D).orElseThrow().audivel(10.0D),
                "alem disso o vizinho ouve o aviso e NAO aprende nada -- e essa a diferenca"
                        + " entre entregar um som e entregar um alvo");
    }

    @Test
    @DisplayName("numero que apagaria a resposta do encontro reprova na construcao")
    void oConstrutorReprovaOQueNaoDariaErroEmJogo() {
        // ESTE E O CASO QUE DEVE REPROVAR. Intervalo 1 nao quebra nada visivel: o
        // batedor continua avisando, os vizinhos continuam ouvindo, o log fica
        // limpo. O que muda e o CUSTO -- uma varredura de raio 24 por tick, por
        // batedor -- e o sintoma e TPS caindo devagar, que e o relato de bug mais
        // caro que existe.
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeRelatorioDeBatedor(15, 0, 24.0D, 0.6D, 1200),
                "intervalo zero tem de reprovar aqui, onde o numero foi escrito");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeRelatorioDeBatedor(0, 120, 24.0D, 0.6D, 1200),
                "observacao zero apaga a janela entre 'ele me viu' e 'ele contou'");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeRelatorioDeBatedor(15, 120, 0.0D, 0.6D, 1200),
                "raio zero e um aviso que nunca sai da cabeca do batedor");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeRelatorioDeBatedor(15, 120, Double.NaN, 0.6D, 1200),
                "raio NaN aprovaria nada e reprovaria nada, e ninguem saberia qual dos dois");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeRelatorioDeBatedor(15, 120, 24.0D, 1.5D, 1200),
                "intensidade acima de 1 estouraria dentro do HearingEvent, no meio de um tick"
                        + " de servidor, e nao aqui");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeRelatorioDeBatedor(15, 120, 24.0D, 0.6D, 0),
                "duracao zero na colonia seria recusada la dentro, um tick depois; recusar aqui"
                        + " e recusar onde o numero foi escrito");
    }
}
