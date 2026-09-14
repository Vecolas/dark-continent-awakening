package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.FaceDaCarapaca;
import com.darkcontinent.nenfoundation.enemy.combat.GrabController;
import com.darkcontinent.nenfoundation.enemy.combat.GrabRefusal;
import com.darkcontinent.nenfoundation.enemy.combat.GrabRules;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeCarapacaOrientada;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da ficha do Crab Heavy: a carapaca, a pinca e a conta do agarrao.
 *
 * <p>Todos os defeitos que ele segura tem a mesma assinatura -- compilam,
 * spawnam, atacam, dropam loot e passam em todo o resto. O caranguejo continua
 * sendo um mob; ele so deixa de ser ESTE mob.</p>
 */
class CrabHeavyPerfilTest {

    private static final UUID PRESA = UUID.nameUUIDFromBytes("crab-heavy-presa".getBytes());

    private static double aGraus(double graus) {
        return Math.cos(Math.toRadians(graus));
    }

    // ------------------------------------------------------------ carapaca

    @Test
    @DisplayName("a armadura da FICHA chega inteira de frente e quase nada pelas costas")
    void aCarapacaUsaAArmaduraDoPerfil() {
        float armaduraDaFicha = ChimeraProfiles.crabHeavy().attributes().armor();
        RegrasDeCarapacaOrientada carapaca = CrabHeavyTuning.carapaca();

        assertEquals(armaduraDaFicha, carapaca.placaEfetiva(armaduraDaFicha, aGraus(0.0D)),
                1.0E-4F,
                "de frente a placa e a armadura do perfil, sem desconto: qualquer corte aqui"
                        + " transformaria o 8 declarado em ChimeraProfiles num numero que o mob"
                        + " nunca cumpre, e a sessao de balanceamento giraria um botao morto");

        float pelasCostas = carapaca.placaEfetiva(armaduraDaFicha, aGraus(180.0D));
        assertTrue(pelasCostas < armaduraDaFicha * 0.25F,
                "pelas costas tem de sobrar menos de um quarto da placa (" + pelasCostas + " de "
                        + armaduraDaFicha + "). Sobrando mais, contornar deixa de compensar o"
                        + " tempo gasto contornando, e o jogador -- corretamente -- volta a bater"
                        + " de frente num bicho que ele nunca vai entender");
    }

    @Test
    @DisplayName("o arco frontal e estreito o bastante para um passo lateral ja valer flanco")
    void oArcoFrontalNaoCobreOBichoInteiro() {
        RegrasDeCarapacaOrientada carapaca = CrabHeavyTuning.carapaca();
        assertEquals(FaceDaCarapaca.FRENTE, carapaca.faceAtingida(aGraus(0.0D)));
        assertEquals(FaceDaCarapaca.FLANCO, carapaca.faceAtingida(aGraus(90.0D)),
                "a 90 graus o jogador esta exatamente do lado do bicho, e do lado tem de valer"
                        + " flanco: valendo frente, contornar so passa a pagar quando ja se esta"
                        + " atras, e a faixa que ENSINA a contornar desaparece");
        assertEquals(FaceDaCarapaca.VENTRE, carapaca.faceAtingida(aGraus(180.0D)));
    }

    // ------------------------------------------------------------- pincada

    @Test
    @DisplayName("o orcamento da pincada e o mesmo que o gerador de animacao copiou")
    void oOrcamentoDoGolpeEUmSo() {
        AttackDefinition pincada = CrabHeavyTuning.pincada(9.0F);

        assertEquals(CrabHeavyTuning.PINCADA_WINDUP_TICKS, pincada.windupTicks());
        assertEquals(CrabHeavyTuning.PINCADA_ACTIVE_TICKS, pincada.activeTicks());
        assertEquals(CrabHeavyTuning.PINCADA_RECOVERY_TICKS, pincada.recoveryTicks());
        assertEquals(9.0F, pincada.damage(), 1.0E-4F,
                "o dano da pincada tem de ser o que chegou do atributo, e nao uma constante deste"
                        + " arquivo: congelado aqui, todo buff e todo ajuste de perfil sumiriam"
                        + " sem aviso");
        assertFalse(pincada.interruptibleActive(),
                "a janela que agarra nao pode ser interrompivel: interrompida no meio, o agarrao"
                        + " comecaria e nunca receberia o tick que o mantem, e a presa ficaria"
                        + " com o relogio ligado e ninguem segurando");
    }

    @Test
    @DisplayName("a caixa do golpe aponta para a FRENTE do mundo e tem o alcance declarado")
    void aCaixaDoGolpeApontaParaFrente() {
        assertEquals(CrabHeavyTuning.ALCANCE_DA_PINCADA, CrabHeavyTuning.CAIXA_DA_PINCADA.maxZ(),
                0.0D,
                "o alcance de decisao e o limite da caixa tem de ser o MESMO numero: com dois, o"
                        + " bicho decide atacar de uma distancia que a caixa nao cobre e passa a"
                        + " golpear o vazio, sem um erro no log");
        assertTrue(CrabHeavyTuning.CAIXA_DA_PINCADA.minZ() > 0.0D,
                "a caixa tem de ficar inteira a FRENTE (+Z no mundo). Copiada da convencao da"
                        + " geometria Bedrock, onde a frente e -Z, ela iria parar ATRAS do bicho:"
                        + " ele atacaria, animaria, publicaria a fase certa e machucaria quem"
                        + " estivesse pelas costas -- que neste mob e exatamente onde o jogador"
                        + " foi ensinado a ficar");
    }

    // -------------------------------------------------------------- agarrao

    @Test
    @DisplayName("BATER tem de custar menos do que ESPERAR, senao a janela de escape e enfeite")
    void baterCustaMenosDoQueEsperar() {
        GrabRules agarrao = CrabHeavyTuning.agarrao();
        assertTrue(esperarCustaMaisQueBater(agarrao),
                "esperar os " + agarrao.ticksMaximos() + " ticks custa "
                        + custoDeEsperar(agarrao) + " de vida e reagir custa "
                        + agarrao.danoParaEscapar() + " de dano pedido. Invertida, a desigualdade"
                        + " nao levanta erro nenhum: a janela de escape continua existindo,"
                        + " ninguem nunca a usa, e o mob vira um atraso de quatro segundos em vez"
                        + " de uma briga");
    }

    @Test
    @DisplayName("REPROVA: a propria regua acusa uma conta em que esperar sai mais barato")
    void aReguaDoEscapeReprovaAContaInvertida() {
        // A regua que mede a regua. Sem este caso, `baterCustaMenosDoQueEsperar`
        // poderia estar sempre verde por um erro de conta -- e um portao que nunca
        // reprova e carimbo.
        GrabRules invertida = new GrabRules(80, 20, 0.5F, 10.0F);
        assertFalse(esperarCustaMaisQueBater(invertida),
                "aguentar quatro pulsos de 0.5 custa 2 de vida e escapar custa 10: nenhum jogador"
                        + " escaparia, e a regua tem de enxergar isso");
    }

    @Test
    @DisplayName("um jogador cabe entre as pincas; um alvo grande demais e RECUSADO com motivo")
    void oVaoDaGarraTemLimiteDeclarado() {
        GrabController pinca = new GrabController(CrabHeavyTuning.agarrao(),
                CrabHeavyTuning.ALTURA_MAXIMA_DA_PRESA, CrabHeavyTuning.LARGURA_MAXIMA_DA_PRESA);

        assertEquals(GrabRefusal.NENHUMA, pinca.podeAgarrar(1.8D, 0.6D, false, true),
                "um jogador (0.6 x 1.8) tem de caber: nao cabendo, o agarrao inteiro deste mob"
                        + " seria codigo que nunca roda");
        assertEquals(GrabRefusal.ALVO_GRANDE_DEMAIS, pinca.podeAgarrar(2.7D, 1.4D, false, true),
                "um golem de ferro (1.4 x 2.7) nao cabe, e a recusa tem de ter NOME. Sem o"
                        + " limite, um caranguejo de um bloco e meio seguraria um cyclops de"
                        + " quatro pelo chao, e nada acusaria isso");

        // Uma pinca, uma presa. Sobrescrever deixaria a primeira presa para sempre,
        // com o relogio dela perdido e nenhum erro no log.
        pinca.agarrar(PRESA);
        assertEquals(GrabRefusal.JA_AGARRANDO, pinca.podeAgarrar(1.8D, 0.6D, false, true));
        assertThrows(IllegalStateException.class, () -> pinca.agarrar(UUID.randomUUID()));
    }

    // ---------------------------------------------------------------- linha

    @Test
    @DisplayName("a coleira do frontliner cabe dentro do que ele enxerga")
    void aLinhaNaoPassaDoQueEleEnxerga() {
        float alcanceDeVisao = ChimeraProfiles.crabHeavy().attributes().followRange();
        assertTrue(CrabHeavyTuning.ALCANCE_DE_AVANCO < alcanceDeVisao,
                "avancar mais longe do que ele enxerga (" + CrabHeavyTuning.ALCANCE_DE_AVANCO
                        + " contra " + alcanceDeVisao + ") faria a coleira nunca morder: a"
                        + " memoria de alvo expira antes do limite, o bicho para por esquecimento"
                        + " e nao por regra, e o 'frontliner que segura a linha' passa a ser um"
                        + " perseguidor comum sem que nada reprove");
    }

    // ------------------------------------------------------------ a conta

    /** Vida que a presa perde se ficar calada ate o fim: os pulsos que couberem. */
    private static float custoDeEsperar(GrabRules regras) {
        return (regras.ticksMaximos() / regras.intervaloDeDano()) * regras.danoPorPulso();
    }

    /** A desigualdade que mantem a janela de escape viva. */
    private static boolean esperarCustaMaisQueBater(GrabRules regras) {
        return custoDeEsperar(regras) > regras.danoParaEscapar();
    }
}
