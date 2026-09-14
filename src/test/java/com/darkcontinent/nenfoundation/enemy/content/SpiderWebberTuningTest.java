package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.DecisaoDeEspacamento;
import com.darkcontinent.nenfoundation.enemy.combat.RecusaDaTeia;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeTeia;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da ficha da Spider Webber: a faixa, o telegrafo e a area da teia.
 *
 * <p>Todos os defeitos que ele segura tem a mesma assinatura -- compilam,
 * spawnam, atiram, dropam loot e passam em todo o resto. A aranha continua sendo
 * um mob; ela so deixa de ser ESTE mob.</p>
 */
class SpiderWebberTuningTest {

    /** Meia-largura e altura de um jogador de pe, para montar o alvo de teste. */
    private static final double MEIA_LARGURA_DO_JOGADOR = 0.3D;
    private static final double ALTURA_DO_JOGADOR = 1.8D;

    /** O alvo a N blocos a frente de um mob em (0,0,0) com yaw 0 -- a frente e +Z. */
    private static AABB alvoA(double distancia) {
        return new AABB(-MEIA_LARGURA_DO_JOGADOR, 0.0D, distancia - MEIA_LARGURA_DO_JOGADOR,
                MEIA_LARGURA_DO_JOGADOR, ALTURA_DO_JOGADOR, distancia + MEIA_LARGURA_DO_JOGADOR);
    }

    private static AABB areaNoMundo() {
        return SpiderWebberTuning.areaDaTeia().noMundo(Vec3.ZERO, 0.0F);
    }

    // ---------------------------------------------------------------- a faixa

    @Test
    @DisplayName("as regras da ficha saem das constantes, e nao de numeros paralelos")
    void asRegrasNascemDasConstantes() {
        RegrasDeTeia regras = SpiderWebberTuning.regras();
        assertEquals(SpiderWebberTuning.ALCANCE_MINIMO_DA_TEIA, regras.alcanceMinimo(), 0.0D);
        assertEquals(SpiderWebberTuning.ALCANCE_MAXIMO_DA_TEIA, regras.alcanceMaximo(), 0.0D);
        assertEquals(SpiderWebberTuning.TICKS_DE_IMOBILIZACAO, regras.ticksDeImobilizacao());
        assertEquals(SpiderWebberTuning.DANO_QUE_LIBERTA, regras.danoQueLiberta(), 0.0F);

        assertTrue(SpiderWebberTuning.ALCANCE_MAXIMO_DA_TEIA
                        < ChimeraProfiles.spiderWebber().attributes().followRange(),
                "ela tem de ENXERGAR mais longe do que alcanca: igualados, o oficial atiraria no"
                        + " instante em que percebe alguem e a aproximacao nunca aconteceria");
    }

    // ----------------------------------------------------- a area contra a faixa

    @Test
    @DisplayName("a area da teia cobre EXATAMENTE a faixa que a regra autoriza")
    void aAreaEAFaixaNaoPodemDivergir() {
        RegrasDeTeia regras = SpiderWebberTuning.regras();
        AABB area = areaNoMundo();

        // O limite de CIMA. Se a area parasse antes, a aranha aprovaria o tiro a
        // nove blocos e nao encostaria em ninguem -- um oficial que erra sozinho no
        // limite da propria faixa, sem uma linha de log.
        assertEquals(RecusaDaTeia.NENHUMA,
                regras.podeLancar(SpiderWebberTuning.ALCANCE_MAXIMO_DA_TEIA, true, false, true));
        assertTrue(area.intersects(alvoA(SpiderWebberTuning.ALCANCE_MAXIMO_DA_TEIA)),
                "a decisao aprova a nove blocos e a area tem de alcancar la");

        // O limite de BAIXO. A area comeca onde a zona morta termina: nem antes
        // (a teia pegaria dentro da distancia em que a regra se recusa a atirar,
        // e a zona morta viraria enfeite de documento), nem depois.
        assertEquals(RecusaDaTeia.NENHUMA,
                regras.podeLancar(SpiderWebberTuning.ALCANCE_MINIMO_DA_TEIA, true, false, true));
        assertTrue(area.intersects(alvoA(SpiderWebberTuning.ALCANCE_MINIMO_DA_TEIA)),
                "no limite de baixo a teia ainda encosta: uma fresta aqui seria uma distancia em"
                        + " que ela atira e erra sempre");

        assertFalse(area.intersects(alvoA(SpiderWebberTuning.ALCANCE_MINIMO_DA_TEIA - 1.0D)),
                "UM BLOCO dentro da zona morta a area ja nao alcanca: e essa a prova de que"
                        + " encostar nela desliga a teia, e nao so o texto do javadoc");
        assertEquals(DecisaoDeEspacamento.RECUAR,
                regras.espacamento(SpiderWebberTuning.ALCANCE_MINIMO_DA_TEIA - 1.0D),
                "e no mesmo lugar em que a teia nao alcanca, ela recua");
    }

    @Test
    @DisplayName("a area aponta para a FRENTE, e a frente de mundo e +Z")
    void aAreaNaoFicaAtrasDaAranha() {
        AABB area = areaNoMundo();
        // Este e o bug que ja custou uma correcao neste repositorio: a caixa ficou
        // ATRAS do mob, que atacava, animava e nao encostava em quem estava na
        // frente -- quem estivesse pelas costas e que apanhava. Fase certa,
        // cooldown certo, log limpo.
        assertTrue(area.minZ > 0.0D,
                "com yaw 0 o olhar vanilla aponta para +Z; uma area em -Z pegaria pelas costas");
        assertFalse(area.intersects(alvoA(-SpiderWebberTuning.ALCANCE_MAXIMO_DA_TEIA)),
                "nada atras dela pode ser pego pela teia");
        assertTrue(area.maxY <= SpiderWebberTuning.ALTURA_DA_AREA + 1.0E-9D
                        && area.minY <= 0.0D,
                "a area parte do chao: uma rede que comeca na altura do peito passa por cima de"
                        + " quem esta agachado, e o agachar viraria a resposta gratuita");
    }

    // -------------------------------------------------------------- telegrafo

    @Test
    @DisplayName("aviso longo, janela curta, recuperacao longa -- e nessa ordem")
    void oTelegrafoTemAFormaCerta() {
        AttackDefinition teia = SpiderWebberTuning.teia();
        assertEquals(SpiderWebberTuning.WINDUP_DA_TEIA, teia.windupTicks());
        assertEquals(SpiderWebberTuning.JANELA_DA_TEIA, teia.activeTicks());
        assertEquals(SpiderWebberTuning.RECUPERACAO_DA_TEIA, teia.recoveryTicks());

        assertTrue(teia.windupTicks() > teia.activeTicks() * 3,
                "um aviso que nao e muito maior que a janela deixa de ser telegrafo e vira uma"
                        + " teia que prende sem avisar -- com o mesmo alcance, o mesmo cooldown e"
                        + " o mesmo log limpo");
        assertTrue(teia.recoveryTicks() > teia.activeTicks(),
                "a recuperacao e a janela de punicao: mais curta que a janela de dano, punir"
                        + " deixaria de ser possivel e so restaria fugir");

        assertTrue(teia.interruptibleWindup(), "o aviso e interrompivel: e o que da sentido a"
                + " correr para cima dela");
        assertFalse(teia.interruptibleActive(),
                "a JANELA nao: depois que o fio sai, ele sai. Cancelar no meio faria a aranha"
                        + " desarmar em silencio um ataque que o jogador ja viu comecar");

        assertTrue(SpiderWebberTuning.TICKS_DE_MIRA_NO_WINDUP > 0
                        && SpiderWebberTuning.TICKS_DE_MIRA_NO_WINDUP < teia.windupTicks(),
                "mirar o aviso INTEIRO transforma a teia em mira-laser e apaga o desvio; nao"
                        + " mirar nada da um oficial que erra sozinho");
        assertTrue(SpiderWebberTuning.RECARGA_APOS_INTERRUPCAO
                        > ChimeraProfiles.spiderWebberRecarga(),
                "interromper tem de VALER: com recarga normal ela recomecaria quase no tick"
                        + " seguinte, e o jogador aprenderia a NAO interromper");
    }

    @Test
    @DisplayName("o dano da teia e uma FRACAO do atributo, e nao um numero proprio")
    void oDanoNaoTemSegundaFonte() {
        float atributo = ChimeraProfiles.spiderWebber().attributes().attackDamage();
        assertEquals(atributo * SpiderWebberTuning.FRACAO_DE_DANO_DA_TEIA,
                SpiderWebberTuning.teia().damage(), 1.0E-6F,
                "repetir o numero aqui criaria duas fontes para a mesma verdade, e girar o"
                        + " atributo numa sessao de balanceamento mudaria a barra de vida do"
                        + " jogador sem mudar este arquivo");
        assertTrue(SpiderWebberTuning.teia().damage() < atributo * 0.5F,
                "o perigo dela e o TEMPO, nao o dano: com dano alto ninguem repararia no que a"
                        + " teia custou, e a imobilizacao deixaria de ser a informacao");
        assertEquals(0.0F, SpiderWebberTuning.teia().knockback(), 0.0F,
                "empurrar e AFASTAR, e afastar e o oposto de prender: com knockback a vitima"
                        + " sairia da area no mesmo tick em que foi pega");
    }

    // ------------------------------------------- o caso que DEVE reprovar

    @Test
    @DisplayName("O CASO QUE DEVE REPROVAR: area escrita a mao pode discordar da faixa")
    void umaAreaParaleaERecusadaPelaMesmaRegua() {
        // Esta e a divergencia que a derivacao de areaDaTeia() existe para impedir:
        // uma caixa escrita a mao que para a oito enquanto a regra aprova a nove.
        // Ela e um AttackHitbox perfeitamente valido -- nada nela levanta excecao --
        // e o sintoma em jogo e um oficial que erra sozinho no limite da faixa.
        AttackHitbox encolhida = new AttackHitbox(-SpiderWebberTuning.MEIA_LARGURA_DA_AREA, 0.0D,
                SpiderWebberTuning.ALCANCE_MINIMO_DA_TEIA - SpiderWebberTuning.MEIA_LARGURA_DE_UM_ALVO,
                SpiderWebberTuning.MEIA_LARGURA_DA_AREA, SpiderWebberTuning.ALTURA_DA_AREA,
                SpiderWebberTuning.ALCANCE_MAXIMO_DA_TEIA - 1.0D);
        assertFalse(encolhida.noMundo(Vec3.ZERO, 0.0F)
                        .intersects(alvoA(SpiderWebberTuning.ALCANCE_MAXIMO_DA_TEIA)),
                "a regua acusa a caixa encolhida: se este assert passar a falhar, a area deixou"
                        + " de ser derivada dos alcances e o portao acima virou carimbo");

        // E a regua tambem morde do outro lado: uma caixa NEGATIVA nem chega a
        // existir, porque o construtor de AttackHitbox recusa antes.
        assertThrows(IllegalArgumentException.class,
                () -> new AttackHitbox(0.0D, 0.0D, SpiderWebberTuning.ALCANCE_MAXIMO_DA_TEIA,
                        1.0D, 1.0D, SpiderWebberTuning.ALCANCE_MINIMO_DA_TEIA),
                "min maior que max e uma area invertida, e uma area invertida nao pega ninguem");
    }

    @Test
    @DisplayName("O CASO QUE DEVE REPROVAR: a ficha inteira nao pode sair da faixa legal")
    void aFichaPassaPelasPropriasReguas() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeTeia(SpiderWebberTuning.ALCANCE_MINIMO_DA_TEIA,
                        SpiderWebberTuning.ALCANCE_MINIMO_DA_TEIA + 0.5D,
                        SpiderWebberTuning.TICKS_DE_IMOBILIZACAO,
                        SpiderWebberTuning.DANO_QUE_LIBERTA),
                "a mesma ficha com a faixa encurtada reprova: e a prova de que a regua morde");
    }
}
