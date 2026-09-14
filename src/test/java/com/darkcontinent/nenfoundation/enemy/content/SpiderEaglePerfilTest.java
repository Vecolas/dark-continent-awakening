package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.ai.NestGuardRules;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.data.EnemyDefinition;
import org.junit.jupiter.api.Test;

/**
 * Sustenta que o perfil da spider eagle e a unica fonte dos numeros da coleira do ninho, e
 * que as tres reguas deste mob continuam valendo depois de qualquer sessao de balanceamento.
 *
 * <p>As tres falham em SILENCIO. O alcance de percepcao menor que a coleira produz uma ave
 * que larga a perseguicao por um motivo e deixa de enxergar o alvo por outro; a escada de
 * raios invertida produz bote sem aviso; o dano do mergulho copiado do atributo produz dois
 * numeros que discordam depois do primeiro ajuste. Nenhum desses tres aparece como erro --
 * aparecem como "as vezes ela desiste cedo demais", que ninguem consegue reproduzir.</p>
 */
class SpiderEaglePerfilTest {

    @Test
    void atributosDaAveMoramNoPerfil() {
        EnemyDefinition definicao = HunterExamProfiles.spiderEagle();
        assertEquals("nenfoundation:spider_eagle", definicao.metadata().id().toString());
        assertEquals(28.0F, definicao.attributes().maxHealth());
        assertEquals(0.35F, definicao.attributes().movementSpeed());
        assertEquals(6.0F, definicao.attributes().attackDamage());
        assertEquals(2.0F, definicao.attributes().armor());
        assertEquals(32.0F, definicao.attributes().followRange());
        assertEquals(0.0F, definicao.attributes().knockbackResistance(),
                "ave leve: levar empurrao e o que permite quebrar o mergulho no ar");
    }

    /**
     * Territorial e o que autoriza a coleira do ninho; social e o que a ave NAO e. Marcada
     * ao contrario, ela viraria um bando de cacadoras de caminho -- e o mob deixaria de ser
     * um perigo de LUGAR sem que nada desse erro.
     */
    @Test
    void aAveEterritorialENaoGregaria() {
        EnemyDefinition definicao = HunterExamProfiles.spiderEagle();
        assertTrue(definicao.metadata().territorial(),
                "sem territorial nao ha lugar a defender, e a coleira do ninho fica sem "
                        + "sentido: a ave passaria a cacar quem so estava de passagem");
        assertFalse(definicao.metadata().social(),
                "a ave defende O NINHO DELA, sozinha; em bando o encounter deixa de ser "
                        + "evitavel recuando");
    }

    @Test
    void numerosDoMergulhoMoramNoPerfil() {
        AttackDefinition dive = HunterExamProfiles.spiderEagleDive();
        assertEquals("dive", dive.id());
        assertEquals(14, dive.windupTicks(),
                "sem windup o mergulho vira dano vindo do ceu sem telegrafo nenhum");
        assertEquals(6, dive.activeTicks());
        assertEquals(18, dive.recoveryTicks());
        assertEquals(0.9F, dive.knockback());
    }

    @Test
    void numerosDaColeiraDoNinhoMoramNoPerfil() {
        NestGuardRules ninho = HunterExamProfiles.spiderEagleNest();
        assertEquals(16.0D, ninho.raioDeAviso());
        assertEquals(6.0D, ninho.raioDeBote());
        assertEquals(28.0D, ninho.raioDeColeira());
        assertEquals(30, ninho.ticksDeAviso());
    }

    /**
     * A REGUA DESTE MOB, e ela cruza DOIS arquivos: o alcance de percepcao precisa cobrir
     * a coleira inteira.
     *
     * <p>Se a coleira for maior que o FOLLOW_RANGE, a ave larga a perseguicao por um motivo
     * (saiu da coleira) e para de enxergar o alvo por outro (saiu do alcance), e os dois
     * numeros passam a discordar sobre a mesma coisa. Em jogo isso nao aparece como erro:
     * aparece como "as vezes ela desiste cedo demais", um relato que ninguem consegue
     * reproduzir porque depende de qual dos dois limites o jogador cruzou primeiro.</p>
     */
    @Test
    void oAlcanceDePercepcaoCobreAColeiraInteira() {
        float alcance = HunterExamProfiles.spiderEagle().attributes().followRange();
        double coleira = HunterExamProfiles.spiderEagleNest().raioDeColeira();

        assertTrue(alcance >= coleira,
                "a ave enxerga ate " + alcance + " blocos e so desiste ao passar de " + coleira
                        + ": existe uma faixa em que ela ainda deveria perseguir e ja nao ve o "
                        + "alvo. Suba o followRange ou desca o raio de coleira.");
    }

    /**
     * A SEGUNDA REGUA: a escada bote &lt; aviso &lt;= coleira precisa valer COM OS NUMEROS
     * DO PERFIL, e nao so no construtor.
     *
     * <p>O construtor protege quem instancia o record; ele nao protege de um perfil que,
     * depois de um balanceamento, encoste os numeros uns nos outros dentro do que o
     * construtor ainda aceita. Aviso igual ao bote e uma ave que grita e mergulha no mesmo
     * bloco; coleira igual ao aviso e o minimo tolerado, e menos que isso e desistir dentro
     * do proprio territorio.</p>
     */
    @Test
    void aEscadaDeRaiosValeComOsNumerosDoPerfil() {
        NestGuardRules ninho = HunterExamProfiles.spiderEagleNest();

        assertTrue(ninho.raioDeBote() < ninho.raioDeAviso(),
                "bote em " + ninho.raioDeBote() + " e aviso em " + ninho.raioDeAviso()
                        + ": sem folga entre os dois o jogador recebe o aviso e o mergulho no "
                        + "mesmo passo, e o telegrafo deixa de ser uma chance de recuar.");
        assertTrue(ninho.raioDeAviso() <= ninho.raioDeColeira(),
                "aviso em " + ninho.raioDeAviso() + " e coleira em " + ninho.raioDeColeira()
                        + ": a ave desistiria dentro do proprio territorio, largando um "
                        + "intruso que ela acabou de avisar.");
    }

    /**
     * A TERCEIRA REGUA: o mergulho e o ataque comum da ave, entao o dano dele E o
     * ATTACK_DAMAGE do perfil -- nao uma copia dele.
     *
     * <p>Por isso a comparacao le os DOIS lados em vez de conferir cada um contra um
     * literal: um literal aprovaria em silencio o dia em que alguem girasse o atributo numa
     * sessao de balanceamento e o mergulho continuasse batendo o numero antigo. A
     * representacao prometeria um dano e o jogador levaria outro.</p>
     */
    @Test
    void oDanoDoMergulhoEOMesmoAttackDamageDoPerfil() {
        float atributo = HunterExamProfiles.spiderEagle().attributes().attackDamage();
        float mergulho = HunterExamProfiles.spiderEagleDive().damage();

        assertEquals(atributo, mergulho,
                "o atributo diz " + atributo + " e o mergulho bate " + mergulho
                        + ": viraram dois numeros para a mesma verdade, e girar um so nao "
                        + "produz erro nenhum -- produz um ataque que mente sobre o proprio dano.");
    }

    @Test
    void aJanelaDeLuzPermiteNinhoDeDia() {
        assertEquals(15, HunterExamProfiles.spiderEagle().spawnRule().maxLight(),
                "o ninho fica em canyon aberto e a ave e diurna: exigir escuridao a impediria "
                        + "de nascer justamente quando alguem escala o penhasco, e o bioma "
                        + "ficaria vazio sem que nada reclamasse");
    }

    /**
     * O mapa de publicados e o que o portao de spawn varre. Faltar aqui nao acende alarme
     * nenhum -- o portao segue verde varrendo um mob a menos, e a ave nunca aparece no
     * mundo por falta de tag ou de biome modifier.
     */
    @Test
    void aAveEstaNoMapaDePublicadosSobOSeuProprioId() {
        EnemyDefinition publicada = HunterExamProfiles.publicados().get("spider_eagle");
        assertNotNull(publicada,
                "spider_eagle fora de publicados(): o portao de spawn nao varre a tag de "
                        + "bioma dela, e um mob sem tag ou sem biome modifier simplesmente "
                        + "nunca nasce, sem erro nenhum");
        assertEquals("nenfoundation:spider_eagle", publicada.metadata().id().toString(),
                "a chave do mapa e o id da entidade divergiram, e nada em jogo notaria");
    }
}
