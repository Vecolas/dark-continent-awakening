package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.ai.SingleEyeRules;
import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointResolver;
import com.darkcontinent.nenfoundation.enemy.faction.FactionRelations;
import com.darkcontinent.nenfoundation.enemy.perception.PercepcaoDeAura;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionBudget;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionController;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionSnapshot;
import com.darkcontinent.nenfoundation.enemy.perception.TargetCandidate;
import com.darkcontinent.nenfoundation.enemy.perception.TargetEvaluator;
import com.darkcontinent.nenfoundation.enemy.perception.ThreatMemory;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da ficha do Cyclops: o olho, o telegrafo e o alcance do porrete.
 *
 * <p>Todos os defeitos que ele segura tem a mesma assinatura -- compilam,
 * spawnam, atacam, dropam loot e passam em todo o resto. O gigante continua
 * sendo um mob; ele so deixa de ser ESTE mob.</p>
 */
class CyclopsTuningTest {

    private static final UUID JOGADOR = UUID.nameUUIDFromBytes("cyclops-alvo".getBytes());

    private static double aGraus(double graus) {
        return Math.cos(Math.toRadians(graus));
    }

    // ------------------------------------------------------------------ olho

    @Test
    @DisplayName("o resolver e o cone nascem das MESMAS regras, e nao de numeros paralelos")
    void oResolverEOConeNaoPodemDivergir() {
        SingleEyeRules regras = CyclopsTuning.olhoUnico();
        WeakPointResolver olho = CyclopsTuning.olho();

        assertEquals(regras.alturaMinimaDoOlho(), olho.alturaMinima(), 0.0D,
                "a altura do ponto fraco tem de sair das regras do olho: escrita de novo aqui, ela"
                        + " divergiria na primeira vez que alguem ajustasse um lado so");
        assertEquals(regras.cossenoDoOlho(), olho.cossenoMinimo(), 0.0D);
        assertEquals(regras.cossenoDoCampoDeVisao(),
                CyclopsTuning.coneDeVisao(32.0D).cossenoDeAbertura(), 0.0D,
                "o cone de visao tambem: com dois numeros, o gigante passaria a enxergar mais longe"
                        + " do que o proprio olho enxerga");

        assertEquals(CyclopsTuning.REGIAO_DO_OLHO, olho.regiaoVulneravel());
        assertEquals(CyclopsTuning.REGIAO_COMUM, olho.regiaoPadrao());
        assertEquals(CyclopsTuning.MULTIPLICADOR_DO_OLHO,
                CyclopsTuning.pontosFracos().multiplier(CyclopsTuning.REGIAO_DO_OLHO), 0.0F);
        assertEquals(1.0F, CyclopsTuning.pontosFracos().multiplier(CyclopsTuning.REGIAO_COMUM), 0.0F,
                "corpo comum nao multiplica nada");
    }

    @Test
    @DisplayName("alto e de frente e olho; alto pelas costas e baixo de frente nao sao")
    void oOlhoSoPagaAltoEDeFrente() {
        WeakPointResolver olho = CyclopsTuning.olho();
        assertEquals(CyclopsTuning.REGIAO_DO_OLHO, olho.resolver(0.95D, aGraus(10.0D)));
        assertEquals(CyclopsTuning.REGIAO_COMUM, olho.resolver(0.95D, aGraus(50.0D)),
                "dentro do campo de visao, mas fora do arco do olho: acerto alto de esguelha nao e"
                        + " critico, senao o olho deixaria de ser um alvo");
        assertEquals(CyclopsTuning.REGIAO_COMUM, olho.resolver(0.95D, aGraus(170.0D)),
                "pelas costas nao ha olho nenhum para acertar");
        assertEquals(CyclopsTuning.REGIAO_COMUM, olho.resolver(0.4D, aGraus(10.0D)),
                "de frente, mas na altura do joelho de um gigante de 4.2 blocos");
    }

    @Test
    @DisplayName("acertar o olho cambaleia em DOIS golpes; a perna precisaria de sete")
    void oMultiplicadorDoOlhoMudaOStaggerEnaoSoABarraDeVida() {
        // O golpe cru de um jogador razoavelmente equipado. O numero nao e ficha de
        // ninguem: ele existe para a conta abaixo ser LEGIVEL.
        float golpe = 7.0F;
        float noOlho = CyclopsTuning.pontosFracos().damage(golpe, CyclopsTuning.REGIAO_DO_OLHO);

        StaggerState olho = new StaggerState(GreedIslandProfiles.cyclopsStagger());
        assertEquals(StaggerResult.ACUMULOU, olho.acumular(1L, noOlho));
        assertEquals(StaggerResult.DISPAROU, olho.acumular(2L, noOlho),
                "dois acertos no olho interrompem o gigante -- e essa e a recompensa que o ponto"
                        + " fraco existe para ensinar");

        StaggerState perna = new StaggerState(GreedIslandProfiles.cyclopsStagger());
        for (long instancia = 1L; instancia <= 2L; instancia++) {
            assertNotEquals(StaggerResult.DISPAROU, perna.acumular(instancia, golpe),
                    "dois golpes no corpo comum NAO podem interromper: se interrompessem, mirar"
                            + " deixaria de valer a pena e o ponto fraco viraria decoracao");
        }
    }

    // ------------------------------------------------------------- telegrafo

    @Test
    @DisplayName("aviso longo, janela curta, recuperacao longa -- nesta ordem")
    void aFormaDoTelegrafoEAFicha() {
        AttackDefinition porrete = CyclopsTuning.porrete();

        assertTrue(porrete.windupTicks() > porrete.activeTicks() * 3,
                "o aviso tem de ser MUITO maior que a janela: invertidos, o gigante bate sem aviso"
                        + " com o mesmo dano, o mesmo cooldown e o mesmo log limpo");
        assertTrue(porrete.recoveryTicks() > porrete.activeTicks() * 3,
                "a recuperacao e a janela em que o jogador pune; curta demais, ele apanha sem ter"
                        + " tido vez e nada acusa");
        assertTrue(porrete.windupTicks() >= porrete.recoveryTicks(),
                "este bicho e LENTO: o aviso e a fase mais longa do golpe");

        assertEquals(GreedIslandProfiles.cyclops().attributes().attackDamage(), porrete.damage(),
                0.0F, "o dano e lido do perfil, e nao repetido: repetido, girar o atributo numa"
                        + " sessao de balanceamento mudaria o jogo sem mudar o Tuning");

        assertFalse(porrete.interruptibleActive(),
                "depois que a tora desce, ela desce: cancelar a janela ativa faria o gigante"
                        + " desarmar em silencio um golpe que o jogador ja viu sair");
        assertTrue(porrete.interruptibleWindup(),
                "o aviso PRECISA ser interrompivel, senao acertar o olho durante ele nao pagaria"
                        + " nada");

        assertTrue(CyclopsTuning.RECARGA_APOS_INTERRUPCAO > GreedIslandProfiles.cyclopsRecarga(),
                "interromper tem de VALER: com recarga igual ou menor, o gigante interrompido"
                        + " voltaria a atacar tao depressa quanto se ninguem tivesse batido, e o"
                        + " jogador aprenderia a nao interromper");
        assertTrue(CyclopsTuning.TICKS_DE_MIRA_NO_WINDUP < CyclopsTuning.WINDUP_DO_PORRETE,
                "mirar o aviso inteiro transforma o porrete em mira-laser e apaga o desvio");
    }

    // --------------------------------------------------------------- alcance

    @Test
    @DisplayName("a caixa do golpe fica NA FRENTE (+Z), e nao atras")
    void aCaixaDoGolpeFicaNaFrente() {
        AttackHitbox caixa = CyclopsTuning.caixaDoPorrete();

        assertTrue(caixa.minZ() > 0.0D,
                "ESTE e o erro que este repositorio ja cometeu uma vez: na geometria do modelo a"
                        + " frente e -Z, mas a transformacao de mundo segue o yaw vanilla, em que o"
                        + " olhar aponta para +Z. Com z negativo o gigante ataca, anima, e so"
                        + " encosta em quem esta pelas COSTAS -- fase certa, cooldown certo, log"
                        + " limpo");

        // Uma prova de que a transformacao concorda: com yaw 0 a caixa tem de
        // conter um alvo plantado a frente e nao conter o mesmo alvo atras.
        AABB noMundo = caixa.noMundo(Vec3.ZERO, 0.0F);
        assertTrue(noMundo.intersects(new AABB(-0.3D, 0.0D, 2.2D, 0.3D, 1.8D, 2.8D)),
                "alvo a 2.5 blocos na frente tem de estar dentro da caixa");
        assertFalse(noMundo.intersects(new AABB(-0.3D, 0.0D, -2.8D, 0.3D, 1.8D, -2.2D)),
                "alvo a 2.5 blocos ATRAS nao pode estar dentro de nada");
    }

    @Test
    @DisplayName("ele so decide golpear a uma distancia que a tora ainda alcanca")
    void aDistanciaDeDecisaoCabeDentroDoAlcanceDaCaixa() {
        AttackHitbox caixa = CyclopsTuning.caixaDoPorrete();
        double bordaDoAlvoNoLimite =
                CyclopsTuning.ALCANCE_DO_GOLPE - CyclopsTuning.MEIA_LARGURA_DE_UM_ALVO;

        assertTrue(bordaDoAlvoNoLimite <= caixa.maxZ(),
                "um alvo parado exatamente na distancia de decisao (" + CyclopsTuning.ALCANCE_DO_GOLPE
                        + ") tem a borda em " + bordaDoAlvoNoLimite + " e a caixa vai ate "
                        + caixa.maxZ() + ": fora dela, o gigante comeca um aviso de "
                        + CyclopsTuning.WINDUP_DO_PORRETE + " ticks contra alguem que ja esta fora"
                        + " do alcance da tora -- e como ele trava a navegacao durante o golpe, o"
                        + " ataque no limite NUNCA acertaria. Isso nao da erro: da um chefe que"
                        + " erra sozinho e parece quebrado");

        // A folga existe, mas e curta: o desvio tem de depender de sair de perto.
        assertTrue(caixa.maxZ() - bordaDoAlvoNoLimite < 1.0D,
                "folga maior que um bloco faria o desvio acontecer sozinho, sem o jogador fazer"
                        + " nada");
    }

    // ---------------------------------------------------- o ponto cego, vivo

    @Test
    @DisplayName("quem esta no flanco nao e adquirido -- nem depois de ferir o gigante")
    void oPontoCegoAtravessaAPercepcaoInteira() {
        assertFalse(alvoAdquirido(aGraus(70.0D)),
                "a 70 graus ele esta fora dos 60 do cone: o PerceptionController filtra o candidato"
                        + " DEPOIS da avaliacao por faccao, entao nem 'este aqui me feriu'"
                        + " atravessa a cegueira. E disso que sai 'circular por fora funciona'");
        assertTrue(alvoAdquirido(aGraus(10.0D)),
                "e a 10 graus ele adquire normalmente -- sem este assert o teste acima passaria"
                        + " tambem num mob que simplesmente nao enxerga nada");
    }

    /** Roda a percepcao real do Cyclops por um ciclo de orcamento e diz se sobrou alvo. */
    private static boolean alvoAdquirido(double cossenoDoOlhar) {
        var perfil = GreedIslandProfiles.cyclops();
        PerceptionController percepcao = new PerceptionController(
                PerceptionBudget.padrao(),
                CyclopsTuning.coneDeVisao(perfil.attributes().followRange()),
                new ThreatMemory(CyclopsTuning.MEMORIA_DE_ALVO_TICKS),
                new TargetEvaluator(FactionRelations.padrao(), perfil.metadata().faction(),
                        perfil.attributes().followRange(), perfil.metadata().territorial()),
                PercepcaoDeAura.NENHUMA, CyclopsTuning.ALCANCE_DE_AUDICAO, 0);

        // jaFeriuOMob = true de proposito: e o candidato de PRIORIDADE MAXIMA do
        // TargetEvaluator. Se ate ele for barrado pelo cone, o ponto cego vale
        // para todo mundo.
        TargetCandidate agressor = new TargetCandidate(JOGADOR, EnemyFaction.HUNTER_ASSOCIATION,
                6.0D, cossenoDoOlhar, true, true, true, true);

        PerceptionSnapshot snapshot = null;
        for (int tick = 0; tick < 12; tick++) {
            snapshot = percepcao.tick(tick, () -> List.of(agressor), false, false);
        }
        return snapshot != null && snapshot.alvoOpcional().isPresent();
    }

    // ------------------------------------------------------ o caso que reprova

    @Test
    @DisplayName("O CASO QUE DEVE REPROVAR: afrouxar o olho ate ele ficar mais largo que a visao")
    void afrouxarOOlhoAlemDoCampoDeVisaoEhRecusado() {
        // Este e o ajuste de balanceamento mais tentador que existe neste mob:
        // "o critico esta dificil demais, abre o arco do olho". Aberto alem dos 60
        // graus da visao, ele passa a pagar multiplicador a quem o gigante nao
        // enxerga -- dano de graca, sem risco e sem log.
        assertThrows(IllegalArgumentException.class,
                () -> SingleEyeRules.deGraus(CyclopsTuning.MEIA_ABERTURA_DA_VISAO_EM_GRAUS + 5.0D,
                        CyclopsTuning.MEIA_ABERTURA_DA_VISAO_EM_GRAUS + 10.0D,
                        CyclopsTuning.ALTURA_MINIMA_DO_OLHO));

        // E a prova de que a ficha ATUAL passa por essa mesma regua, em vez de
        // apenas nao ser testada.
        assertTrue(CyclopsTuning.MEIA_ABERTURA_DO_OLHO_EM_GRAUS
                        < CyclopsTuning.MEIA_ABERTURA_DA_VISAO_EM_GRAUS,
                "o arco do olho tem de ser mais estreito que o campo de visao");
        assertEquals(CyclopsTuning.MEIA_ABERTURA_DA_VISAO_EM_GRAUS,
                CyclopsTuning.olhoUnico().meiaAberturaDaVisaoEmGraus(), 1.0E-6D);
    }
}
