package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.ai.DecisaoDeFerrao;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeFerrao;
import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraNenStatus;
import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenController;
import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenIntent;
import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenSituation;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.DoseDeVeneno;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeVeneno;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da ficha do Scorpion Leader: o veneno, os dois telegrafos e os alcances.
 *
 * <p>Todos os defeitos que ele segura tem a mesma assinatura -- compilam, spawnam,
 * atacam, dropam loot e passam em todo o resto. A formiga continua sendo um mob;
 * ela so deixa de ser ESTA formiga, e o que some primeiro e sempre a mesma coisa:
 * o segundo tipo de dano que o jogador tinha de aprender a evitar.</p>
 */
class ScorpionLeaderTuningTest {

    /** Ciclo completo de um ataque, em ticks -- a conta que a cadencia usa. */
    private static int ciclo(AttackDefinition ataque) {
        return ataque.windupTicks() + ataque.activeTicks() + ataque.recoveryTicks();
    }

    // --------------------------------------------------------------- alcances

    @Test
    @DisplayName("os dois golpes so decidem atacar a uma distancia que a caixa deles alcanca")
    void asDistanciasDeDecisaoCabemDentroDasCaixas() {
        double bordaDaPinca = ScorpionLeaderTuning.ALCANCE_DA_PINCA
                - ScorpionLeaderTuning.MEIA_LARGURA_DE_UM_ALVO;
        assertTrue(bordaDaPinca <= ScorpionLeaderTuning.caixaDaPinca().maxZ(),
                "um alvo parado exatamente na distancia de decisao da pinca tem a borda em "
                        + bordaDaPinca + " e a caixa vai ate "
                        + ScorpionLeaderTuning.caixaDaPinca().maxZ() + ": fora dela a formiga"
                        + " comeca um aviso contra alguem que a garra ja nao alcanca -- e como a"
                        + " navegacao trava durante o golpe, a pinca no limite NUNCA acertaria");

        double bordaDoFerrao = ScorpionLeaderTuning.ALCANCE_DO_FERRAO
                - ScorpionLeaderTuning.MEIA_LARGURA_DE_UM_ALVO;
        assertTrue(bordaDoFerrao <= ScorpionLeaderTuning.caixaDoFerrao().maxZ(),
                "o mesmo para o ferrao, e aqui custa mais caro: um aviso de 28 ticks que termina"
                        + " sem encostar em ninguem ensina o jogador a ignorar o unico telegrafo"
                        + " que anuncia veneno");
    }

    @Test
    @DisplayName("o desenho paga o alcance: garra sozinha, e ferrao mais o arranco")
    void oQueACaixaReivindicaOModeloEntrega() {
        assertTrue(ScorpionLeaderTuning.ALCANCE_DESENHADO_DA_PINCA
                        >= ScorpionLeaderTuning.caixaDaPinca().maxZ(),
                "a pinca nao tem arranco nenhum: o alcance dela sai inteiro do desenho. A outra"
                        + " ponta desta conta e valida_alcance_da_pinca, em"
                        + " scorpion_leader_geo.py -- quem encolher a garra la reprova la, quem"
                        + " esticar a caixa aqui reprova aqui");

        double alcanceDoFerrao = ScorpionLeaderTuning.ALCANCE_DESENHADO_DO_FERRAO
                + ScorpionLeaderTuning.AVANCO_DO_FERRAO;
        assertTrue(alcanceDoFerrao >= ScorpionLeaderTuning.caixaDoFerrao().maxZ(),
                "o ferrao desenhado alcanca " + ScorpionLeaderTuning.ALCANCE_DESENHADO_DO_FERRAO
                        + " e o arranco paga mais " + ScorpionLeaderTuning.AVANCO_DO_FERRAO
                        + ": sem a soma cobrir " + ScorpionLeaderTuning.caixaDoFerrao().maxZ()
                        + ", o jogador e envenenado por uma ponta que, na tela, parou antes dele"
                        + " -- e veneno e justamente o dano que ele nao consegue rastrear");

        assertTrue(ScorpionLeaderTuning.caixaDoFerrao().maxY()
                        > ScorpionLeaderTuning.caixaDaPinca().maxY(),
                "o ferrao desce de CIMA e a garra varre a frente: uma caixa de ferrao tao baixa"
                        + " quanto a da pinca erraria quem esta em pe num bloco no exato angulo em"
                        + " que a animacao mostra a ponta passando na altura da cabeca dele");
        assertTrue(ScorpionLeaderTuning.caixaDoFerrao().maxX()
                        < ScorpionLeaderTuning.caixaDaPinca().maxX(),
                "uma ponta perfura e duas garras varrem: caixa larga no ferrao transformaria o"
                        + " golpe de precisao num tapa de area, e sair de lado deixaria de ser"
                        + " resposta");
    }

    @Test
    @DisplayName("as duas caixas ficam NA FRENTE (+Z), e nao atras")
    void asCaixasFicamNaFrente() {
        for (AttackHitbox caixa : new AttackHitbox[] {
                ScorpionLeaderTuning.caixaDaPinca(), ScorpionLeaderTuning.caixaDoFerrao() }) {
            assertTrue(caixa.minZ() > 0.0D,
                    "ESTE e o erro que este repositorio ja cometeu uma vez: na geometria do modelo"
                            + " a frente e -Z, mas a transformacao de mundo segue o yaw vanilla, em"
                            + " que o olhar aponta para +Z. Com z negativo a formiga ataca, anima,"
                            + " e so encosta em quem esta pelas COSTAS -- fase certa, cooldown"
                            + " certo, log limpo");

            AABB noMundo = caixa.noMundo(Vec3.ZERO, 0.0F);
            assertTrue(noMundo.intersects(new AABB(-0.3D, 0.0D, 0.5D, 0.3D, 0.9D, 0.8D)),
                    "alvo colado a frente tem de estar dentro da caixa");
            assertFalse(noMundo.intersects(new AABB(-0.3D, 0.0D, -0.8D, 0.3D, 0.9D, -0.5D)),
                    "alvo a mesma distancia ATRAS nao pode estar dentro de nada");
        }
    }

    // -------------------------------------------------------------- telegrafo

    @Test
    @DisplayName("o ferrao avisa muito mais que a pinca, e cobra menos na hora")
    void aFormaDosDoisTelegrafosEAFicha() {
        AttackDefinition pinca = ScorpionLeaderTuning.pinca();
        AttackDefinition ferroada = ScorpionLeaderTuning.ferroada();

        assertTrue(ferroada.windupTicks() > pinca.windupTicks() * 2,
                "o telegrafo longo E o preco do veneno. Igualado ao da pinca, o ataque mais caro"
                        + " do bicho vira o mais barato de acertar, sem que uma linha do servidor"
                        + " mude -- e o gerador de animacao cobra a mesma desigualdade do lado da"
                        + " arte, em valida_o_ferrao_se_anuncia");
        assertTrue(ferroada.damage() < pinca.damage(),
                "a ferroada cobra MENOS na hora: a diferenca e paga depois, em veneno. Um ferrao"
                        + " que cobrasse o dano cheio E o veneno faria a pinca nao ter razao de"
                        + " existir, e sem golpe comum o telegrafo longo nao tem com o que ser"
                        + " comparado");

        float danoDaFicha = ChimeraProfiles.scorpionLeader().attributes().attackDamage();
        assertEquals(danoDaFicha, pinca.damage(), 0.0F,
                "o dano da pinca e lido da ficha, e nao repetido: repetido, girar o atributo numa"
                        + " sessao de balanceamento mudaria o jogo sem mudar o Tuning");
        assertEquals(danoDaFicha * ScorpionLeaderTuning.FRACAO_DE_DANO_DO_FERRAO,
                ferroada.damage(), 1.0E-4F,
                "e o do ferrao e DERIVADO dela pela mesma razao");

        assertTrue(ferroada.interruptibleWindup(),
                "o aviso do ferrao PRECISA ser interrompivel: vinte e oito ticks sao o tempo que o"
                        + " jogador tem para trocar o veneno por dois golpes bem colocados, e essa"
                        + " e a unica resposta que o bicho oferece");
        assertFalse(ferroada.interruptibleActive(),
                "depois que a cauda desce, ela desce: cancelar a janela ativa faria a formiga"
                        + " desarmar em silencio um golpe que o jogador ja viu sair");
        assertFalse(ferroada.interruptibleRecovery(),
                "a recuperacao do ferrao ja e a punicao. Cambalear dentro dela devolveria a fase"
                        + " para IDLE, e a recarga por interrupcao acabaria ENCURTANDO a janela em"
                        + " que o jogador bate de graca");
        assertTrue(pinca.interruptibleRecovery(),
                "a da pinca e o contrario: ela e a janela barata, e punir nela tem de ser a coisa"
                        + " obvia a fazer");

        assertTrue(ScorpionLeaderTuning.RECARGA_APOS_INTERRUPCAO
                        > ChimeraProfiles.scorpionLeaderRecarga(),
                "interromper tem de VALER: com recarga igual ou menor, a formiga interrompida"
                        + " voltaria a atacar tao depressa quanto se ninguem tivesse batido");
    }

    // ----------------------------------------------------------- a cadencia
    //
    // A parte mais facil de quebrar do bicho inteiro, e a que menos avisa: se a
    // ferroada seguinte chegar DEPOIS de a dose anterior expirar, o acumulo nunca
    // acontece. O veneno continua funcionando, com duracao, teto e nivel corretos --
    // e a mecanica que ele existe para servir some do jogo.

    @Test
    @DisplayName("a guarda do ferrao cabe entre o ciclo dele e a duracao de uma dose")
    void aCadenciaPermiteQueOAcumuloEXISTA() {
        int cicloDoFerrao = ciclo(ScorpionLeaderTuning.ferroada());
        assertTrue(ScorpionLeaderTuning.TICKS_DE_GUARDA_DO_FERRAO > cicloDoFerrao,
                "guarda de " + ScorpionLeaderTuning.TICKS_DE_GUARDA_DO_FERRAO + " contra um ciclo"
                        + " de " + cicloDoFerrao + " ticks: menor que o ciclo e guarda nenhuma, e"
                        + " a pinca -- que da ao jogador com o que comparar o telegrafo longo --"
                        + " nunca apareceria");

        // O intervalo REAL entre duas ferroadas, com a recarga do perfil e a pinca
        // que se encaixa no meio. E ele, e nao a guarda sozinha, que tem de caber
        // dentro de uma dose.
        int recarga = ChimeraProfiles.scorpionLeaderRecarga();
        int atePodeDeNovo = cicloDoFerrao + recarga;
        int intervaloReal = atePodeDeNovo < ScorpionLeaderTuning.TICKS_DE_GUARDA_DO_FERRAO
                ? atePodeDeNovo + ciclo(ScorpionLeaderTuning.pinca()) + recarga
                : atePodeDeNovo;
        assertTrue(intervaloReal < ScorpionLeaderTuning.DURACAO_POR_FERROADA_EM_TICKS,
                "entre duas ferroadas passam " + intervaloReal + " ticks e uma dose dura "
                        + ScorpionLeaderTuning.DURACAO_POR_FERROADA_EM_TICKS + ": se a dose"
                        + " expirasse antes, toda ferroada seria a primeira, o nivel nunca"
                        + " passaria de I e o teto viraria decoracao -- com o veneno inteiro"
                        + " funcionando e o acumulo ausente do jogo");

        // E a prova de que o acumulo de fato acontece com esse intervalo, rodando a
        // regra real em vez de confiar na desigualdade acima.
        RegrasDeVeneno veneno = ScorpionLeaderTuning.veneno();
        DoseDeVeneno primeira = veneno.aplicar(true, 0);
        int sobra = primeira.duracaoEmTicks() - intervaloReal;
        assertTrue(sobra > 0, "a primeira dose ainda esta correndo quando a segunda chega");
        assertEquals(ScorpionLeaderTuning.AMPLIFICADOR_MAXIMO_DO_VENENO,
                veneno.aplicar(true, sobra).amplificador(),
                "a segunda ferroada sobe o degrau: e este degrau que o jogador ve subir e que lhe"
                        + " diz que ficar perto custa mais a cada acerto");
    }

    // --------------------------------------------------- a fronteira do Nen

    @Test
    @DisplayName("a intencao de Nen vira POSTURA, e so Ken fecha a guarda")
    void aIntencaoDeNenNaoAtravessaAFronteiraComoAura() {
        assertTrue(ScorpionLeaderTuning.guardaFechada(TacticalNenIntent.MANTER_KEN));
        assertFalse(ScorpionLeaderTuning.guardaFechada(TacticalNenIntent.NENHUMA));
        assertFalse(ScorpionLeaderTuning.guardaFechada(TacticalNenIntent.MANTER_TEN),
                "Ten e o piso de quem despertou e nao muda postura nenhuma; fechar a guarda nele"
                        + " deixaria a formiga parada o encontro inteiro");
        assertFalse(ScorpionLeaderTuning.guardaFechada(TacticalNenIntent.ELEVAR_REN),
                "Ren e ofensivo: fechar a guarda nele seria o oposto do que a intencao diz");
        assertFalse(ScorpionLeaderTuning.guardaFechada(TacticalNenIntent.ENTRAR_EM_ZETSU),
                "ela e a ANCORA do esquadrao e nao recua -- um recuo dela deixaria os membros"
                        + " postados em torno de uma peca que saiu de campo");

        // A ponta a ponta, sem servidor: uma formiga TREINADA, sob pressao, decide
        // Ken -- e a postura para o ataque dela. E a prova de que a ligacao existe e
        // significa alguma coisa, mesmo com a aura de mob ainda nao publicada pelo
        // nucleo (ver ScorpionLeaderEntity.FRACAO_DE_AURA_NAO_PUBLICADA).
        TacticalNenIntent intencao = new TacticalNenController(ChimeraNenStatus.TRAINED)
                .decidir(new TacticalNenSituation(1.0D, 0.9D, true, true, false, true, false));
        assertEquals(TacticalNenIntent.MANTER_KEN, intencao);
        assertEquals(DecisaoDeFerrao.SEGURAR_A_GUARDA,
                ScorpionLeaderTuning.ferrao().decidir(1.0D, true, false, true,
                        ScorpionLeaderTuning.TICKS_DE_GUARDA_DO_FERRAO,
                        ScorpionLeaderTuning.guardaFechada(intencao)),
                "com a guarda fechada ela NAO comeca o golpe, mesmo com alvo ao alcance e guarda"
                        + " vencida. Nenhuma aura e calculada aqui: o que atravessa a fronteira e"
                        + " uma decisao de postura");
    }

    // ------------------------------------------------- os casos que REPROVAM

    @Test
    @DisplayName("O CASO QUE DEVE REPROVAR: inverter os alcances, e encurtar o veneno ate o teto")
    void asDuasInversoesTentadorasSaoRecusadas() {
        // 1. "a ferroada acerta de longe demais, encurta o alcance dela" -- encurtada
        // abaixo do da pinca, a ferroada nunca sairia.
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFerrao(ScorpionLeaderTuning.ALCANCE_DO_FERRAO,
                        ScorpionLeaderTuning.ALCANCE_DA_PINCA,
                        ScorpionLeaderTuning.TICKS_DE_GUARDA_DO_FERRAO));

        // 2. "o veneno esta forte demais, baixa o teto" -- baixado ate a dose, a
        // primeira ferroada ja chega ao limite e a segunda nao acumula nada.
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeVeneno(ScorpionLeaderTuning.DURACAO_POR_FERROADA_EM_TICKS,
                        ScorpionLeaderTuning.DURACAO_POR_FERROADA_EM_TICKS,
                        ScorpionLeaderTuning.AMPLIFICADOR_MAXIMO_DO_VENENO));

        // E a prova de que a ficha ATUAL passa pelas duas reguas, em vez de apenas
        // nao ser testada.
        assertTrue(ScorpionLeaderTuning.ALCANCE_DO_FERRAO > ScorpionLeaderTuning.ALCANCE_DA_PINCA);
        assertTrue(ScorpionLeaderTuning.TETO_DE_DURACAO_EM_TICKS
                > ScorpionLeaderTuning.DURACAO_POR_FERROADA_EM_TICKS);
        assertEquals(ScorpionLeaderTuning.TICKS_DE_GUARDA_DO_FERRAO,
                ScorpionLeaderTuning.ferrao().ticksDeGuardaDoFerrao());
    }
}
