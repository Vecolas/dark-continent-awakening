package com.darkcontinent.nenfoundation.enemy.chimera.nen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraNenStatus;
import com.darkcontinent.nenfoundation.enemy.perception.PercepcaoDeAura;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do Nen tatico de Chimera (issue #145).
 *
 * <p>A falha que ele cobre e de AUTORIDADE, e ela nao da erro: um controlador de
 * inimigo que calculasse a propria aura viraria a segunda autoridade sobre Nen, e
 * duas autoridades sobre a mesma mecanica divergem em silencio -- o sintoma e
 * desbalanceamento que ninguem consegue explicar. Por isso o teste cobra que a
 * decisao seja sempre INTENCAO, nunca aplicacao.</p>
 */
class TacticalNenControllerTest {

    private static TacticalNenSituation situacao(double aura, double vida, boolean combate,
            boolean alcance, boolean escondido, boolean pressao, boolean fuga) {
        return new TacticalNenSituation(aura, vida, combate, alcance, escondido, pressao, fuga);
    }

    @Test
    @DisplayName("formiga DORMENTE nunca usa Nen, em situacao nenhuma")
    void dormenteNuncaUsaNen() {
        TacticalNenController controlador = new TacticalNenController(ChimeraNenStatus.DORMANT);
        for (boolean combate : new boolean[] { false, true }) {
            for (double vida : new double[] { 1.0D, 0.1D }) {
                assertEquals(TacticalNenIntent.NENHUMA,
                        controlador.decidir(situacao(1.0D, vida, combate, combate, false, true, true)),
                        "Dar Nen a quem nao despertou apaga a leitura de que Nen e raro, e nao"
                                + " aparece como erro nenhum.");
            }
        }
    }

    @Test
    @DisplayName("o piso de quem despertou e Ten, e ele vale em paz")
    void despertoMantemTen() {
        TacticalNenController controlador = new TacticalNenController(ChimeraNenStatus.AWAKENED);
        assertEquals(TacticalNenIntent.MANTER_TEN,
                controlador.decidir(TacticalNenSituation.tranquila()));
    }

    @Test
    @DisplayName("sem aura utilizavel, ela para de gastar -- e para ANTES de zerar")
    void reservaMinimaProtegeOTen() {
        TacticalNenController controlador = new TacticalNenController(ChimeraNenStatus.AWAKENED);
        assertEquals(TacticalNenIntent.NENHUMA,
                controlador.decidir(situacao(0.1D, 1.0D, true, true, false, false, true)),
                "Gastar ate zero perde o Ten junto com o Ren, e o jogador le um bicho que"
                        + " desistiu sem entender por que.");
        assertEquals(TacticalNenIntent.ELEVAR_REN,
                controlador.decidir(situacao(0.9D, 1.0D, true, true, false, false, true)));
    }

    @Test
    @DisplayName("FUGIR vem antes de atacar")
    void fugirVemAntesDeAtacar() {
        TacticalNenController controlador = new TacticalNenController(ChimeraNenStatus.AWAKENED);
        assertEquals(TacticalNenIntent.ENTRAR_EM_ZETSU,
                controlador.decidir(situacao(0.9D, 0.15D, true, true, false, true, true)),
                "Atacar enquanto morre nao da erro: da um bicho que parece nao ter instinto.");
    }

    @Test
    @DisplayName("sem saida, sumir so entrega a morte -- entao ela luta")
    void semSaidaNaoEntraEmZetsu() {
        TacticalNenController controlador = new TacticalNenController(ChimeraNenStatus.AWAKENED);
        assertEquals(TacticalNenIntent.ELEVAR_REN,
                controlador.decidir(situacao(0.9D, 0.15D, true, true, false, false, false)));
    }

    @Test
    @DisplayName("Gyo vem antes de Ren: perceber antes de gastar")
    void gyoVemAntesDeRen() {
        TacticalNenController controlador = new TacticalNenController(ChimeraNenStatus.AWAKENED);
        assertEquals(TacticalNenIntent.USAR_GYO,
                controlador.decidir(situacao(0.9D, 1.0D, true, true, true, false, true)));
    }

    @Test
    @DisplayName("Ken exige TREINO, e nao apenas despertar")
    void kenExigeTreino() {
        TacticalNenSituation pressionada = situacao(0.9D, 0.9D, true, true, false, true, true);

        assertEquals(TacticalNenIntent.ELEVAR_REN,
                new TacticalNenController(ChimeraNenStatus.AWAKENED).decidir(pressionada),
                "Sem a trava, a PRIMEIRA formiga desperta da colonia usaria a defesa mais cara"
                        + " do jogo, e a progressao deixaria de ser observavel.");
        assertEquals(TacticalNenIntent.MANTER_KEN,
                new TacticalNenController(ChimeraNenStatus.TRAINED).decidir(pressionada));
    }

    @Test
    @DisplayName("o contador de ticks zera na TROCA de intencao e cresce quando ela se mantem")
    void contadorDeIntencao() {
        TacticalNenController controlador = new TacticalNenController(ChimeraNenStatus.AWAKENED);
        controlador.decidir(TacticalNenSituation.tranquila());
        controlador.decidir(TacticalNenSituation.tranquila());
        assertEquals(1, controlador.ticksNaIntencao());
        controlador.decidir(situacao(0.9D, 1.0D, true, true, false, false, true));
        assertEquals(0, controlador.ticksNaIntencao());
    }

    @Test
    @DisplayName("limpar devolve a formiga ao estado sem Nen")
    void limparZera() {
        TacticalNenController controlador = new TacticalNenController(ChimeraNenStatus.TRAINED);
        controlador.decidir(situacao(0.9D, 0.9D, true, true, false, true, true));
        controlador.limpar();
        assertEquals(TacticalNenIntent.NENHUMA, controlador.atual());
        assertEquals(0, controlador.ticksNaIntencao());
    }

    // ------------------------------------------------------------- fronteira

    @Test
    @DisplayName("toda intencao aponta para uma tecnica que o NUCLEO ja tem")
    void intencoesApontamParaTecnicasReais() {
        List<String> doNucleo = List.of("ten", "ren", "gyo", "zetsu", "ken");
        for (TacticalNenIntent intencao : TacticalNenIntent.values()) {
            if (!intencao.usaNen()) continue;
            assertEquals("nenfoundation", intencao.tecnica().getNamespace());
            assertTrue(doNucleo.contains(intencao.tecnica().getPath()),
                    intencao + " aponta para '" + intencao.tecnica() + "', que o nucleo nao tem."
                            + " Uma intencao sem consumidor nao da erro: da uma formiga que"
                            + " 'decide' algo e nao faz nada.");
        }
    }

    @Test
    @DisplayName("a porta de aura da formiga continua INERTE, e e literalmente a do nucleo")
    void portaDeAuraContinuaInerte() {
        assertSame(PercepcaoDeAura.NENHUMA, PercepcaoDeAuraDeChimera.DORMENTE,
                "Um segundo objeto inerte com o mesmo comportamento seria duas fontes para a"
                        + " mesma verdade, e no dia em que uma ganhasse regra a outra"
                        + " continuaria calada.");
        assertSame(PercepcaoDeAuraDeChimera.DORMENTE, PercepcaoDeAuraDeChimera.para(true),
                "Enquanto a issue #126 (Gyo) estiver aberta no nucleo, inventar aqui a regra de"
                        + " quem-ve-o-que seria a segunda autoridade sobre Nen.");
        List<com.darkcontinent.nenfoundation.enemy.perception.TargetCandidate> candidatos = List.of();
        assertSame(candidatos, PercepcaoDeAuraDeChimera.DORMENTE.aplicar(candidatos));
    }

    // ----------------------------------------------------------- validacao

    @Test
    @DisplayName("fracao fora de 0..1 reprova, em vez de virar formiga sempre cheia")
    void fracaoForaDaFaixaReprova() {
        assertThrows(IllegalArgumentException.class,
                () -> situacao(1.5D, 1.0D, false, false, false, false, true));
        assertThrows(IllegalArgumentException.class,
                () -> situacao(1.0D, -0.1D, false, false, false, false, true));
        assertThrows(IllegalArgumentException.class,
                () -> situacao(Double.NaN, 1.0D, false, false, false, false, true));
    }

    @Test
    @DisplayName("alvo ao alcance sem combate reprova -- os dois campos discordam")
    void camposContraditoriosReprovam() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> situacao(1.0D, 1.0D, false, true, false, false, true));
        assertTrue(erro.getMessage().contains("discordam"));
    }

    @Test
    @DisplayName("NENHUMA e a unica intencao sem tecnica")
    void apenasNenhumaNaoUsaNen() {
        assertFalse(TacticalNenIntent.NENHUMA.usaNen());
        for (TacticalNenIntent intencao : TacticalNenIntent.values()) {
            if (intencao != TacticalNenIntent.NENHUMA) assertTrue(intencao.usaNen());
        }
    }
}
