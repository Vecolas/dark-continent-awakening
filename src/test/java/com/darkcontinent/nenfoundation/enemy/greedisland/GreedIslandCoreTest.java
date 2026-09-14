package com.darkcontinent.nenfoundation.enemy.greedisland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.encounter.EncounterInstance;
import com.darkcontinent.nenfoundation.enemy.encounter.EncounterState;
import com.darkcontinent.nenfoundation.enemy.encounter.RewardLedger;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do nucleo de Greed Island (issue #142).
 *
 * <p>O gate da issue e "o dummy de GI converte em card exatamente uma vez sob
 * corrida multiplayer". O que este arquivo prova e a REGRA que aquele gate vai
 * exercitar: a exclusao e atomica, o limite de copias e cobrado no mesmo ato, e
 * nenhuma saida que nao seja captura converte coisa alguma.</p>
 */
class GreedIslandCoreTest {

    private static final ResourceLocation CYCLOPS =
            ResourceLocation.fromNamespaceAndPath("nenfoundation", "cyclops");
    private static final ResourceLocation PUFFBALL =
            ResourceLocation.fromNamespaceAndPath("nenfoundation", "hyper_puffball");

    private static EncounterInstance concluido() {
        EncounterInstance encontro = new EncounterInstance(UUID.randomUUID(), "gi:cyclops",
                GreedIslandRegion.DIMENSAO, new BlockPos(0, 70, 0));
        encontro.estado(EncounterState.ARMED);
        encontro.estado(EncounterState.ACTIVE);
        encontro.estado(EncounterState.COMPLETED);
        return encontro;
    }

    private static CardConversionService servico(RewardLedger ledger, int copiasDoCyclops) {
        return new CardConversionService(ledger, Map.of(
                CYCLOPS, new CardSpec(CYCLOPS, "B", copiasDoCyclops),
                PUFFBALL, new CardSpec(PUFFBALL, "F", 0)));
    }

    // ------------------------------------------------------------- conversao

    @Test
    @DisplayName("a corrida de dois jogadores no mesmo tick paga UM card")
    void conversaoAcontecaUmaVezSo() {
        RewardLedger ledger = new RewardLedger();
        CardConversionService servico = servico(ledger, 0);
        EncounterInstance encontro = concluido();

        assertTrue(servico.converter(encontro, CYCLOPS, DefeatResult.CAPTURADO).isPresent());
        assertTrue(servico.converter(encontro, CYCLOPS, DefeatResult.CAPTURADO).isEmpty(),
                "Checagem e pagamento na mesma chamada nao tem meio onde o segundo jogador"
                        + " caiba. As duas transacoes seriam legitimas, so que a mesma.");
        assertEquals(1, servico.emitidas(CYCLOPS));
    }

    @Test
    @DisplayName("so CAPTURADO converte; morrer nao e sinonimo de virar card")
    void apenasCapturaConverte() {
        CardConversionService servico = servico(new RewardLedger(), 0);
        EncounterInstance encontro = concluido();
        for (DefeatResult resultado : DefeatResult.values()) {
            if (resultado == DefeatResult.CAPTURADO) continue;
            assertTrue(servico.converter(encontro, CYCLOPS, resultado).isEmpty(),
                    resultado + " converteu: o card sairia de todo cadaver, e a condicao de"
                            + " captura viraria decoracao.");
        }
        assertEquals(0, servico.emitidas(CYCLOPS));
    }

    @Test
    @DisplayName("um episodio paga cards de criaturas DIFERENTES -- a matilha nao vale um so")
    void criaturasDiferentesNoMesmoEpisodio() {
        CardConversionService servico = servico(new RewardLedger(), 0);
        EncounterInstance encontro = concluido();
        assertTrue(servico.converter(encontro, CYCLOPS, DefeatResult.CAPTURADO).isPresent());
        assertTrue(servico.converter(encontro, PUFFBALL, DefeatResult.CAPTURADO).isPresent(),
                "Uma chave generica faria o primeiro alvo bloquear os outros, e um Wolf Pack"
                        + " inteiro valeria um card so.");
    }

    @Test
    @DisplayName("o limite de copias do MUNDO e cobrado, e a recusada nao consome copia")
    void limiteDeCopiasECobrado() {
        RewardLedger ledger = new RewardLedger();
        CardConversionService servico = servico(ledger, 2);

        assertTrue(servico.converter(concluido(), CYCLOPS, DefeatResult.CAPTURADO).isPresent());
        assertTrue(servico.converter(concluido(), CYCLOPS, DefeatResult.CAPTURADO).isPresent());
        assertTrue(servico.converter(concluido(), CYCLOPS, DefeatResult.CAPTURADO).isEmpty(),
                "Sem limite, a escassez que faz a coleta ser disputa deixa de existir.");
        assertEquals(2, servico.emitidas(CYCLOPS),
                "A tentativa recusada nao pode consumir uma copia do mundo: a escassez viraria"
                        + " erosao, e o card sumiria do jogo sem ninguem ter recebido.");
    }

    @Test
    @DisplayName("a contagem de cards restaurada continua cobrando o limite mundial")
    void contagemRestauradaNaoResetaEscassez() {
        CardConversionService servico = servico(new RewardLedger(), 2);
        servico.carregarEmitidas(Map.of(CYCLOPS, 2));

        assertTrue(servico.converter(concluido(), CYCLOPS, DefeatResult.CAPTURADO).isEmpty(),
                "reiniciar o contador em memoria faria uma copia finita voltar a sair depois"
                        + " do restart, mesmo com um save valido");
        assertEquals(2, servico.emitidas(CYCLOPS));
    }

    @Test
    @DisplayName("converter antes de COMPLETED recusa COM MOTIVO")
    void converterAntesDoFimRecusa() {
        CardConversionService servico = servico(new RewardLedger(), 0);
        EncounterInstance emCurso = new EncounterInstance(UUID.randomUUID(), "gi:cyclops",
                GreedIslandRegion.DIMENSAO, BlockPos.ZERO);
        emCurso.estado(EncounterState.ARMED);
        emCurso.estado(EncounterState.ACTIVE);
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> servico.converter(emCurso, CYCLOPS, DefeatResult.CAPTURADO));
        assertTrue(erro.getMessage().contains("abandonou"));
    }

    @Test
    @DisplayName("criatura fora do catalogo reprova em vez de virar captura que nunca paga")
    void criaturaForaDoCatalogoReprova() {
        CardConversionService servico = servico(new RewardLedger(), 0);
        assertThrows(IllegalArgumentException.class, () -> servico.converter(concluido(),
                ResourceLocation.fromNamespaceAndPath("nenfoundation", "radio_rat"),
                DefeatResult.CAPTURADO));
    }

    @Test
    @DisplayName("catalogo com chave divergente reprova no carregamento")
    void catalogoTortoReprova() {
        assertThrows(IllegalArgumentException.class, () -> new CardConversionService(
                new RewardLedger(), Map.of(CYCLOPS, new CardSpec(PUFFBALL, "B", 0))));
    }

    // ------------------------------------------------------------- condicao

    @Test
    @DisplayName("captura por enfraquecimento exige vida baixa E o bicho vivo")
    void condicaoDeEnfraquecimento() {
        CaptureCondition condicao = CaptureCondition.porEnfraquecimento();
        assertTrue(condicao.satisfeita(0.2D, false, false, 100));
        assertFalse(condicao.satisfeita(0.2D, true, false, 100),
                "Matar CANCELA a captura: e isso que faz o jogador ter de parar de bater.");
        assertFalse(condicao.satisfeita(0.6D, false, false, 100));
    }

    @Test
    @DisplayName("condicao impossivel reprova no construtor")
    void condicaoImpossivelReprova() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new CaptureCondition(0.0D, true, false, 0));
        assertTrue(erro.getMessage().contains("nunca"),
                "Uma condicao que nunca se satisfaz nao da erro: aparece como um bicho que"
                        + " nunca vira card, e o jogador culpa a propria mira.");
    }

    @Test
    @DisplayName("limite de tempo de combate so vale quando declarado")
    void limiteDeTempoEOpcional() {
        assertTrue(CaptureCondition.porAbate().satisfeita(1.0D, true, false, 999_999));
        CaptureCondition comPressa = new CaptureCondition(1.0D, false, false, 200);
        assertTrue(comPressa.satisfeita(1.0D, true, false, 200));
        assertFalse(comPressa.satisfeita(1.0D, true, false, 201));
    }

    // ------------------------------------------------------------ isolamento

    @Test
    @DisplayName("a ilha PROIBE por padrao -- proibir primeiro, abrir depois")
    void ilhaProibePorPadrao() {
        assertTrue(GreedIslandRegion.dentro(GreedIslandRegion.DIMENSAO));
        assertFalse(GreedIslandRegion.dentro(Level.OVERWORLD));
        assertFalse(GreedIslandRegion.dentro(ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("nenfoundation", "world_tree"))));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> GreedIslandRegion.exigirDentro(Level.OVERWORLD, "converter card"));
        assertTrue(erro.getMessage().contains("atalho"),
                "A metade esquecida seria levar o bicho para fora e converter la, onde as"
                        + " regras da ilha nao valem.");
    }

    // ------------------------------------------------------------------ card

    @Test
    @DisplayName("rank fora do catalogo da obra reprova")
    void rankInvalidoReprova() {
        assertThrows(IllegalArgumentException.class, () -> new CardSpec(CYCLOPS, "Z", 0));
        assertThrows(IllegalArgumentException.class, () -> new CardSpec(CYCLOPS, "AA", 0));
        assertEquals("S", new CardSpec(CYCLOPS, "S", 1).rank());
    }

    @Test
    @DisplayName("copias zero significa ILIMITADO, e e assim que um card comum se declara")
    void zeroEIlimitado() {
        CardSpec comum = new CardSpec(PUFFBALL, "F", 0);
        assertTrue(comum.ilimitado());
        assertTrue(comum.cabeMaisUma(9999));
        assertFalse(new CardSpec(CYCLOPS, "B", 3).cabeMaisUma(3));
    }

    @Test
    @DisplayName("contagem de copias carregada do save SUBSTITUI, nunca soma")
    void contagemCarregadaSubstitui() {
        CardConversionService servico = servico(new RewardLedger(), 0);
        servico.converter(concluido(), CYCLOPS, DefeatResult.CAPTURADO);
        servico.carregarEmitidas(Map.of(PUFFBALL, 5));
        assertEquals(0, servico.emitidas(CYCLOPS));
        assertEquals(5, servico.emitidas(PUFFBALL));
        assertEquals(Optional.empty(), Optional.ofNullable(servico.emitidas().get(CYCLOPS)));
    }
}
