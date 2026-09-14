package com.darkcontinent.nenfoundation.enemy.chimera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRole;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do nucleo de Chimera (issues #120 e #121).
 *
 * <p>As falhas cobertas sao de PERSISTENCIA e de CRESCIMENTO, e nenhuma da erro:
 * uma identidade que nao sobrevive ao save produz um bicho diferente a cada
 * relogada com o mesmo nome, e uma colonia sem teto deixa o mundo mais lento a
 * cada hora com o relato "o servidor piora com o tempo".</p>
 */
class ChimeraCoreTest {

    private static final ResourceLocation MOLDE =
            ResourceLocation.fromNamespaceAndPath("nenfoundation", "crab_heavy");
    private static final UUID COLONIA = UUID.nameUUIDFromBytes("colonia".getBytes());
    private static final UUID BANDO = UUID.nameUUIDFromBytes("bando".getBytes());

    // ------------------------------------------------------------ identidade

    @Test
    @DisplayName("a identidade sobrevive ao save inteira")
    void identidadeAtravessaOSave() {
        ChimeraIdentity original = ChimeraIdentity
                .nova(MOLDE, ChimeraRank.PEON, ChimeraMorphology.HEAVY,
                        Set.of(ChimeraTrait.ARMOR_PLATE, ChimeraTrait.STRENGTH))
                .comColonia(COLONIA).comSquad(BANDO).comNen(ChimeraNenStatus.AWAKENING);

        ChimeraIdentity lida = ChimeraIdentity.carregar(original.salvar());
        assertEquals(original, lida,
                "Sem isso cada relogada produz um bicho diferente com o mesmo nome, e o"
                        + " jogador jura que ele mudou de cor.");
    }

    @Test
    @DisplayName("formiga sem colonia e sem squad tambem atravessa, e continua SEM")
    void ausenciaAtravessaComoAusencia() {
        ChimeraIdentity orfa = ChimeraIdentity.nova(MOLDE, ChimeraRank.PEON,
                ChimeraMorphology.HEAVY, Set.of());
        ChimeraIdentity lida = ChimeraIdentity.carregar(orfa.salvar());
        assertTrue(lida.orfa());
        assertTrue(lida.colonia().isEmpty(),
                "Ausencia virando UUID nulo faria a busca devolver 'existe' para um id que nao"
                        + " existe, e a orfa passaria a alimentar uma colonia fantasma.");
        assertTrue(lida.squad().isEmpty());
    }

    @Test
    @DisplayName("o NBT dos traits sai em ordem ESTAVEL")
    void traitsSaemEmOrdemEstavel() {
        ChimeraIdentity a = ChimeraIdentity.nova(MOLDE, ChimeraRank.PEON, ChimeraMorphology.HEAVY,
                Set.of(ChimeraTrait.STRENGTH, ChimeraTrait.ARMOR_PLATE, ChimeraTrait.CLAWS));
        assertEquals(a.salvar().toString(), a.salvar().toString());
        ChimeraIdentity b = ChimeraIdentity.nova(MOLDE, ChimeraRank.PEON, ChimeraMorphology.HEAVY,
                Set.of(ChimeraTrait.CLAWS, ChimeraTrait.ARMOR_PLATE, ChimeraTrait.STRENGTH));
        assertEquals(a.salvar().toString(), b.salvar().toString(),
                "Ordem variavel nao daria erro: encheria todo diff de mundo de ruido, e a"
                        + " mudanca de verdade passaria despercebida.");
    }

    @Test
    @DisplayName("schema desconhecido REPROVA em vez de virar formiga plausivel e errada")
    void schemaDesconhecidoReprova() {
        CompoundTag futuro = ChimeraIdentity.nova(MOLDE, ChimeraRank.PEON,
                ChimeraMorphology.HEAVY, Set.of()).salvar();
        futuro.putInt("version", 99);
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> ChimeraIdentity.carregar(futuro));
        assertTrue(erro.getMessage().contains("plausivel"));
    }

    @Test
    @DisplayName("o teto de traits e da ARTE, e reprova")
    void tetoDeTraitsReprova() {
        assertThrows(IllegalArgumentException.class, () -> ChimeraIdentity.nova(MOLDE,
                ChimeraRank.PEON, ChimeraMorphology.HEAVY,
                Set.of(ChimeraTrait.WINGS, ChimeraTrait.CLAWS, ChimeraTrait.VENOM,
                        ChimeraTrait.TAIL, ChimeraTrait.SPEED)));
    }

    @Test
    @DisplayName("colonia destruida deixa a formiga ORFA, e nao apagada")
    void coloniaDestruidaNaoMataAFormiga() {
        ChimeraIdentity comDona = ChimeraIdentity.nova(MOLDE, ChimeraRank.PEON,
                ChimeraMorphology.HEAVY, Set.of()).comColonia(COLONIA).comSquad(BANDO);
        ChimeraIdentity orfa = comDona.semColonia();
        assertTrue(orfa.orfa());
        assertEquals(BANDO, orfa.squad().orElseThrow(),
                "Perder a colonia nao pode desalistar do bando: sao duas filiacoes.");
    }

    @Test
    @DisplayName("o Nen nao regride")
    void nenNaoRegride() {
        ChimeraIdentity desperta = ChimeraIdentity.nova(MOLDE, ChimeraRank.PEON,
                ChimeraMorphology.HEAVY, Set.of()).comNen(ChimeraNenStatus.AWAKENED);
        assertEquals(ChimeraNenStatus.AWAKENED,
                desperta.comNen(ChimeraNenStatus.DORMANT).nen(),
                "Permitir a volta abriria a porta para um estado oscilando a cada tick, e o"
                        + " jogador leria isso como um bicho piscando, sem causa visivel.");
    }

    @Test
    @DisplayName("toda formiga nasce SEM Nen")
    void nasceDormente() {
        for (ChimeraDefinition molde : ChimeraPeonDefinitions.todos().values()) {
            assertEquals(ChimeraNenStatus.DORMANT, molde.sortear(1L).nen(),
                    molde.id() + " nasceu com Nen: dar Nen ativo a todo peon apaga a leitura"
                            + " de que Nen e raro, e nao aparece como erro nenhum.");
        }
    }

    // ---------------------------------------------------------------- molde

    @Test
    @DisplayName("o sorteio e DETERMINISTICO pela semente")
    void sorteioEDeterministico() {
        ChimeraDefinition molde = ChimeraPeonDefinitions.wolfRunner();
        assertEquals(molde.sortear(42L), molde.sortear(42L),
                "Sem determinismo um servidor e seu backup divergem, e isso so aparece numa"
                        + " investigacao de dessincronia.");
        // Varre um intervalo em vez de comparar DUAS sementes: com poucos candidatos
        // extras, duas sementes podem cair no mesmo resultado por azar, e um teste
        // que depende disso reprova em dias alternados -- flaky e pior que ausente.
        long distintos = java.util.stream.LongStream.range(0L, 64L)
                .mapToObj(molde::sortear)
                .map(ChimeraIdentity::traits)
                .distinct().count();
        assertTrue(distintos > 1,
                "Em 64 sementes o molde produziu sempre o MESMO conjunto de traits: o gene"
                        + " pool inteiro seria decoracao, e nada acusaria isso.");
    }

    @Test
    @DisplayName("os traits garantidos vem SEMPRE, em qualquer semente")
    void garantidosVemSempre() {
        ChimeraDefinition molde = ChimeraPeonDefinitions.crabHeavy();
        for (long semente = 0; semente < 40; semente++) {
            assertTrue(molde.sortear(semente).traits().containsAll(molde.traitsGarantidos()),
                    "Sem os garantidos o Crab Heavy vira um peon comum mais gordo, e a"
                            + " resposta que ele ensina desaparece.");
        }
    }

    @Test
    @DisplayName("o sorteio nunca estoura o teto de traits")
    void sorteioRespeitaOTeto() {
        for (ChimeraDefinition molde : ChimeraPeonDefinitions.todos().values()) {
            for (long semente = 0; semente < 60; semente++) {
                assertTrue(molde.sortear(semente).traits().size()
                                <= ChimeraIdentity.MAXIMO_DE_TRAITS,
                        molde.id() + " estourou o teto na semente " + semente);
            }
        }
    }

    @Test
    @DisplayName("garantir trait que o molde nao declara como possivel reprova")
    void traitGarantidoForaDosPossiveisReprova() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new ChimeraDefinition(MOLDE, ChimeraRank.PEON, ChimeraMorphology.HEAVY,
                        Set.of(ChimeraTrait.CLAWS), Set.of(ChimeraTrait.WINGS),
                        SquadRole.FRONTLINER));
        assertTrue(erro.getMessage().contains("osso"));
    }

    @Test
    @DisplayName("molde que nasce LEADER reprova -- lideranca e promocao")
    void moldeLiderReprova() {
        assertThrows(IllegalArgumentException.class, () -> new ChimeraDefinition(MOLDE,
                ChimeraRank.PEON, ChimeraMorphology.HEAVY, Set.of(), Set.of(), SquadRole.LEADER));
    }

    @Test
    @DisplayName("os tres peoes tem papeis DISTINTOS -- nao sao tres recolores")
    void tresPapeisDistintos() {
        var papeis = ChimeraPeonDefinitions.todos().values().stream()
                .map(ChimeraDefinition::papelNoSquad).distinct().count();
        assertEquals(3, papeis,
                "Tres bichos parecidos com funcoes parecidas nao ensinam nada ao jogador.");
    }

    // ------------------------------------------------------------ orcamento

    @Test
    @DisplayName("o teto por RANK impede a colonia de gastar a cota so com chefes")
    void tetoPorRank() {
        ChimeraTrackingBudget orcamento = new ChimeraTrackingBudget(24, 6, 30);
        assertTrue(orcamento.cabe(ChimeraRank.PEON, 20, 2));
        assertFalse(orcamento.cabe(ChimeraRank.OFFICER, 10, 6),
                "Teto global unico deixaria a colonia gastar tudo com os bichos caros, e o"
                        + " resultado seria uma colonia sem tropa e com cinco chefes.");
        assertFalse(orcamento.cabe(ChimeraRank.PEON, 24, 0));
    }

    @Test
    @DisplayName("o total corta antes dos tetos por rank quando a soma estoura")
    void totalCortaPrimeiro() {
        ChimeraTrackingBudget orcamento = ChimeraTrackingBudget.padrao();
        assertFalse(orcamento.cabe(ChimeraRank.PEON, 24, 4),
                "Sem o total, dois tetos generosos somariam mais do que o servidor aguenta,"
                        + " e o mundo ficaria lento sem nada acusar.");
    }

    @Test
    @DisplayName("total fora da faixa util reprova -- nos DOIS lados")
    void orcamentoIncoerenteReprova() {
        IllegalArgumentException acima = assertThrows(IllegalArgumentException.class,
                () -> new ChimeraTrackingBudget(24, 6, 99));
        assertTrue(acima.getMessage().contains("botao morto"),
                "Acima da soma o total nunca morde, e vira um numero que alguem gira sem"
                        + " efeito nenhum.");
        IllegalArgumentException abaixo = assertThrows(IllegalArgumentException.class,
                () -> new ChimeraTrackingBudget(24, 6, 10));
        assertTrue(abaixo.getMessage().contains("tropa e chefia"));
        assertThrows(IllegalArgumentException.class,
                () -> new ChimeraTrackingBudget(0, 6, 28));
    }

    @Test
    @DisplayName("a fronteira tropa/chefia mora num lugar so")
    void fronteiraDeChefia() {
        assertFalse(ChimeraTrackingBudget.oficial(ChimeraRank.PEON));
        assertFalse(ChimeraTrackingBudget.oficial(ChimeraRank.DRUDGE));
        assertTrue(ChimeraTrackingBudget.oficial(ChimeraRank.OFFICER));
        assertTrue(ChimeraTrackingBudget.oficial(ChimeraRank.KING),
                "Espalhada por switch em cada consumidor, ela divergiria no dia em que um rank"
                        + " novo aparecesse -- e o rank novo cairia no lado errado em metade"
                        + " dos lugares, sem erro nenhum.");
    }
}
