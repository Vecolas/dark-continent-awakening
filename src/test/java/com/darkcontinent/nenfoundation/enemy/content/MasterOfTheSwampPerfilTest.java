package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.data.EnemyDefinition;
import com.darkcontinent.nenfoundation.enemy.encounter.RegrasDeFisgada;
import org.junit.jupiter.api.Test;

/**
 * Sustenta que o perfil do master of the swamp e a unica fonte dos numeros da pescaria, e
 * que o CABO DE GUERRA continua sendo um cabo de guerra depois de qualquer sessao de
 * balanceamento.
 *
 * <p>Este mob nao e um inimigo que se mata: e um que se FISGA. Todo numero aqui existe
 * para manter as duas respostas vivas -- puxar demais arrebenta a linha, acompanhar o
 * peixe o cansa. Quando os numeros se desencontram, nada da erro: o que aparece e uma
 * pescaria em que segurar o botao ganha sempre, e ninguem consegue dizer em que arquivo
 * isso foi decidido.</p>
 */
class MasterOfTheSwampPerfilTest {

    /**
     * Blocos por tick de um jogador ANDANDO (~4,3 blocos/s dividido por 20).
     *
     * <p>E de proposito o caso mais LENTO de fuga: quem corre, nada com impulso ou monta
     * um barco se afasta mais rapido e arrebenta antes. Medir a regua pelo fugitivo mais
     * devagar e o unico jeito de ela valer para todos os outros -- uma regua calibrada
     * pelo sprint aprovaria em silencio um balanceamento em que so o sprint custa
     * alguma coisa.</p>
     */
    private static final double PASSO_DE_FUGA = 0.21D;

    @Test
    void atributosDoPeixeMoramNoPerfil() {
        EnemyDefinition definicao = HunterExamProfiles.masterOfTheSwamp();
        assertEquals("nenfoundation:master_of_the_swamp", definicao.metadata().id().toString());
        assertEquals(60.0F, definicao.attributes().maxHealth());
        assertEquals(0.6F, definicao.attributes().movementSpeed());
        assertEquals(6.0F, definicao.attributes().attackDamage());
        assertEquals(4.0F, definicao.attributes().armor(),
                "a armadura e o que torna matar a pancada CHATO: ela nao esta aqui para o "
                        + "peixe vencer a briga, e sim para a briga nao ser o caminho curto");
        assertEquals(24.0F, definicao.attributes().followRange());
        assertEquals(0.6F, definicao.attributes().knockbackResistance(),
                "peixe enorme nao sai de perto do anzol com um tapa: knockback baixo demais "
                        + "empurraria o alvo para fora do proprio encontro");
    }

    /**
     * Nem territorial nem gregario, e as duas negativas sao decisao.
     *
     * <p>Territorial faria dele um guardiao de lugar, que ataca quem passa -- e o jogador
     * que vem PESCAR passaria a ser agredido antes de lancar a linha. Social faria dele um
     * cardume, e "um por regiao" viraria mentira sem que nada acusasse: a ficha pede um
     * encontro ambiental raro, nao um bioma cheio de peixes gigantes.</p>
     */
    @Test
    void oPeixeNaoEterritorialNemGregario() {
        EnemyDefinition definicao = HunterExamProfiles.masterOfTheSwamp();
        assertFalse(definicao.metadata().territorial(),
                "territorial transformaria a pescaria em emboscada: o peixe atacaria quem "
                        + "chegou na margem, antes de existir isca nenhuma na agua");
        assertFalse(definicao.metadata().social(),
                "social e um cardume de peixes gigantes -- a ficha pede UM por regiao, e o "
                        + "premio de captura so vale se o encontro for raro");
    }

    @Test
    void aRegraDeSpawnPoeUmPeixeNaAgua() {
        EnemyDefinition definicao = HunterExamProfiles.masterOfTheSwamp();
        assertTrue(definicao.spawnRule().biomeTags().contains("#nenfoundation:swamp_water_biomes"),
                "a tag e o que liga o perfil ao biome modifier; trocada, o mob nunca nasce");
        assertTrue(definicao.spawnRule().allowWater(),
                "um peixe que nao pode nascer na agua nao nasce em lugar nenhum");
        assertFalse(definicao.spawnRule().requireGround(),
                "exigir chao no fundo do pantano limitaria o spawn a agua rasa, e o vulto "
                        + "submerso -- que e como o jogador descobre o mob -- deixaria de "
                        + "aparecer onde vale a pena pescar");
        assertEquals(0, definicao.spawnRule().minLight());
        assertEquals(15, definicao.spawnRule().maxLight(),
                "o peixe nao e noturno: exigir escuridao o impediria de nascer justamente "
                        + "quando alguem senta na margem para pescar, e o pantano ficaria "
                        + "vazio sem que nada reclamasse");
        assertEquals(1, definicao.spawnRule().maxNearbySameFaction(),
                "UM por regiao e a ficha inteira: com o limite acima de 1 o encontro raro "
                        + "vira fauna comum, e o premio de captura perde o motivo de existir");
    }

    @Test
    void numerosDaMordidaMoramNoPerfil() {
        AttackDefinition bite = HunterExamProfiles.masterOfTheSwampBite();
        assertEquals("bite", bite.id());
        assertEquals(12, bite.windupTicks(),
                "sem windup a mordida vira dano vindo da agua sem telegrafo nenhum");
        assertEquals(4, bite.activeTicks());
        assertEquals(16, bite.recoveryTicks());
        assertEquals(0.3F, bite.knockback());
    }

    /**
     * O combate e SECUNDARIO, e por isso a mordida e o ataque comum do peixe: o dano dela
     * E o ATTACK_DAMAGE do perfil, nao uma copia dele.
     *
     * <p>A comparacao le os DOIS lados em vez de conferir cada um contra um literal. Um
     * literal aprovaria em silencio o dia em que alguem girasse o atributo numa sessao de
     * balanceamento e a mordida continuasse batendo o numero antigo -- a representacao
     * prometeria um dano e o jogador levaria outro.</p>
     */
    @Test
    void oDanoDaMordidaEOMesmoAttackDamageDoPerfil() {
        float atributo = HunterExamProfiles.masterOfTheSwamp().attributes().attackDamage();
        float mordida = HunterExamProfiles.masterOfTheSwampBite().damage();

        assertEquals(atributo, mordida,
                "o atributo diz " + atributo + " e a mordida bate " + mordida
                        + ": viraram dois numeros para a mesma verdade, e girar um so nao "
                        + "produz erro nenhum -- produz um ataque que mente sobre o proprio dano.");
    }

    @Test
    void numerosDaFisgadaMoramNoPerfil() {
        RegrasDeFisgada fisgada = HunterExamProfiles.masterOfTheSwampFishing();
        assertEquals(8.0D, fisgada.raioDaIsca());
        assertEquals(200, fisgada.ticksParaCansar());
        assertEquals(100.0D, fisgada.tensaoMaxima());
        assertEquals(6.0D, fisgada.tensaoPorAfastamento());
        assertEquals(4.0D, fisgada.alivioPorAproximacao());
    }

    /**
     * A REGUA DESTE MOB, e ela e a razao de o mob existir: com os numeros do perfil, o
     * jogador que so FOGE em linha reta tem de arrebentar a linha ANTES de o peixe cansar.
     *
     * <p>Se nao arrebentar, "puxar demais" nao custa nada: fugir passa a ser a jogada
     * dominante, o jogador anda para tras ate os {@code ticksParaCansar} passarem e recolhe
     * o peixe sem nunca ter tomado uma decisao. O cabo de guerra vira ESPERA, e nada
     * acusa -- o mob continua spawnando, mordendo, cansando e sendo capturado, com todos
     * os testes verdes.</p>
     *
     * <p>A conta NAO esta escrita aqui como literal de proposito: ela e rodada contra o
     * proprio {@code tensao(...)}. Um numero fechado a mao seria uma SEGUNDA fonte da
     * mesma verdade -- mudar a formula da tensao deixaria este teste verde medindo uma
     * regra que o jogo nao usa mais.</p>
     */
    @Test
    void fugirEmLinhaRetaArrebentaALinhaAntesDeOPeixeCansar() {
        RegrasDeFisgada fisgada = HunterExamProfiles.masterOfTheSwampFishing();

        double tensao = 0.0D;
        int ticksAteArrebentar = -1;
        for (int tick = 1; tick <= fisgada.ticksParaCansar() && ticksAteArrebentar < 0; tick++) {
            tensao = fisgada.tensao(tensao, PASSO_DE_FUGA);
            if (fisgada.arrebenta(tensao)) ticksAteArrebentar = tick;
        }

        assertTrue(ticksAteArrebentar > 0,
                "um jogador ANDANDO para longe do peixe atravessou os "
                        + fisgada.ticksParaCansar() + " ticks de fisgada sem arrebentar a linha "
                        + "(tensao final " + tensao + " de " + fisgada.tensaoMaxima()
                        + "). Puxar demais deixou de custar alguma coisa: fugir virou a jogada "
                        + "dominante e o cabo de guerra virou espera. Suba tensaoPorAfastamento, "
                        + "desca tensaoMaxima ou suba ticksParaCansar.");
        assertTrue(ticksAteArrebentar < fisgada.ticksParaCansar(),
                "a linha arrebenta no tick " + ticksAteArrebentar + " e o peixe cansa no tick "
                        + fisgada.ticksParaCansar() + ": o castigo por fugir chega junto ou "
                        + "depois do premio, entao na pratica ele nao existe.");
    }

    /**
     * A OUTRA METADE DA MESMA REGUA, e sem ela a primeira aprovaria um mob impossivel: quem
     * ACOMPANHA o peixe tem de chegar ao cansaco com a linha inteira.
     *
     * <p>Medida sozinha, "fugir arrebenta" ficaria verde num balanceamento em que a tensao
     * sobe de qualquer jeito -- inclusive parado -- e ai nenhuma captura acontece nunca. As
     * duas juntas dizem a coisa que interessa: existe uma resposta certa E ela funciona.</p>
     */
    @Test
    void acompanharOPeixeChegaAoCansacoComALinhaInteira() {
        RegrasDeFisgada fisgada = HunterExamProfiles.masterOfTheSwampFishing();

        double tensao = 0.0D;
        for (int tick = 1; tick <= fisgada.ticksParaCansar(); tick++) {
            tensao = fisgada.tensao(tensao, 0.0D);
            assertFalse(fisgada.arrebenta(tensao),
                    "a linha arrebentou no tick " + tick + " de um jogador que acompanhou o "
                            + "peixe sem se afastar um bloco (tensao " + tensao + "). Se ficar "
                            + "junto tambem custa tensao, nenhuma captura e possivel e o mob so "
                            + "sabe escapar.");
        }

        assertTrue(fisgada.cansou(fisgada.ticksParaCansar()),
                "quem aguentou a fisgada inteira sem tensionar a linha precisa terminar com um "
                        + "peixe cansado -- senao o estado CAUGHT nao tem como acontecer");
    }

    /**
     * Regua que cruza DOIS arquivos: o peixe precisa ENXERGAR a isca que ele investiga.
     *
     * <p>Com o raio da isca maior que o followRange, o perfil autorizaria uma investigacao
     * que o alcance de percepcao nao sustenta: em jogo isso nao da erro, da uma isca que
     * as vezes chama o peixe e as vezes nao, dependendo de qual dos dois limites o
     * lancamento cruzou.</p>
     */
    @Test
    void oAlcanceDePercepcaoCobreORaioDaIsca() {
        float alcance = HunterExamProfiles.masterOfTheSwamp().attributes().followRange();
        double raio = HunterExamProfiles.masterOfTheSwampFishing().raioDaIsca();

        assertTrue(alcance >= raio,
                "o peixe enxerga ate " + alcance + " blocos e a isca chama de " + raio
                        + ": existe uma faixa em que o perfil manda investigar e a percepcao "
                        + "nao alcanca. Suba o followRange ou desca o raio da isca.");
    }

    /**
     * O mapa de publicados e o que o portao de spawn varre. Faltar aqui nao acende alarme
     * nenhum -- o portao segue verde varrendo um mob a menos, e o peixe nunca aparece no
     * mundo por falta de tag ou de biome modifier.
     */
    @Test
    void oPeixeEstaNoMapaDePublicadosSobOSeuProprioId() {
        EnemyDefinition publicado = HunterExamProfiles.publicados().get("master_of_the_swamp");
        assertNotNull(publicado,
                "master_of_the_swamp fora de publicados(): o portao de spawn nao varre a tag "
                        + "de bioma dele, e um mob sem tag ou sem biome modifier simplesmente "
                        + "nunca nasce, sem erro nenhum");
        assertEquals("nenfoundation:master_of_the_swamp", publicado.metadata().id().toString(),
                "a chave do mapa e o id da entidade divergiram, e nada em jogo notaria");
    }
}
