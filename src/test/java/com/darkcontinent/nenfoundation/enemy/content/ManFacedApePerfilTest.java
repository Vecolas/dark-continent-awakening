package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.ai.DisguiseRules;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.data.EnemyDefinition;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Sustenta que o perfil do man-faced ape e a unica fonte dos numeros do disfarce, e que
 * as duas reguas do mob -- o bando alcanca mais longe do que o gatilho, e o corpo e
 * fragil de proposito -- continuam valendo depois de qualquer sessao de balanceamento.
 *
 * <p>As duas falham em SILENCIO no jogo. Raio do bando curto demais vira "um mob fraco";
 * vida alta demais vira "um mob chato". Nenhum dos dois aparece como erro, e nenhum dos
 * dois se investiga sem medir.</p>
 */
class ManFacedApePerfilTest {

    @Test
    void atributosDoMacacoMoramNoPerfil() {
        EnemyDefinition definicao = HunterExamProfiles.manFacedApe();
        assertEquals("nenfoundation:man_faced_ape", definicao.metadata().id().toString());
        assertEquals(24.0F, definicao.attributes().maxHealth());
        assertEquals(0.29F, definicao.attributes().movementSpeed());
        assertEquals(4.0F, definicao.attributes().attackDamage());
        assertEquals(1.0F, definicao.attributes().armor());
        assertEquals(24.0F, definicao.attributes().followRange());
        assertEquals(0.1F, definicao.attributes().knockbackResistance());
    }

    /**
     * O plano diz "forca = engano e NUMEROS". Social ligado e o que autoriza o bando; se
     * alguem marcar o macaco como territorial e solitario, o pack ambush morre sem erro.
     */
    @Test
    void oMacacoEGregarioENaoTerritorial() {
        EnemyDefinition definicao = HunterExamProfiles.manFacedApe();
        assertTrue(definicao.metadata().social(),
                "sem social nao ha bando, e sem bando o 'reveal + pack ambush' e so um "
                        + "macaco solitario pulando de um arbusto");
        assertFalse(definicao.metadata().territorial(),
                "o macaco CACA, nao defende area: territorial o prenderia perto do ninho e "
                        + "ele nunca chegaria disfarcado ate o jogador");
    }

    @Test
    void numerosDoGolpeMoramNoPerfil() {
        AttackDefinition strike = HunterExamProfiles.manFacedApeStrike();
        assertEquals("strike", strike.id());
        assertEquals(8, strike.windupTicks(), "sem windup a emboscada vira dano sem aviso");
        assertEquals(4, strike.activeTicks());
        assertEquals(12, strike.recoveryTicks());
        assertEquals(4.0F, strike.damage());
        assertEquals(0.5F, strike.knockback());
    }

    @Test
    void numerosDoDisfarceMoramNoPerfil() {
        DisguiseRules regras = HunterExamProfiles.manFacedApeDisguise();
        assertEquals(3.5D, regras.distanciaDeRevelacao());
        assertEquals(0.6D, regras.cossenoDeObservacao());
        assertEquals(12.0D, regras.raioDoBando());
        assertEquals(10, regras.ticksDeReveal());
    }

    /**
     * A REGUA DO BANDO. O aviso precisa alcancar MAIS LONGE do que o gatilho que revelou
     * o primeiro macaco -- senao o macaco que revelou esta, por construcao, fora do raio
     * dos companheiros, e o "reveal + pack ambush" do plano vira um macaco sozinho.
     *
     * <p>Nada em jogo acusaria isso. O bando nao apareceria, e o relato seria "esse mob e
     * fraco" -- nao "o raio do bando encolheu abaixo da distancia de revelacao".</p>
     */
    @Test
    void oRaioDoBandoAlcancaMaisLongeQueOGatilhoDeRevelacao() {
        DisguiseRules regras = HunterExamProfiles.manFacedApeDisguise();
        assertTrue(regras.raioDoBando() > regras.distanciaDeRevelacao(),
                "o bando avisa ate " + regras.raioDoBando() + " blocos e o disfarce cai a "
                        + regras.distanciaDeRevelacao() + ": o primeiro macaco revela sozinho "
                        + "e ninguem chega para a emboscada.");
    }

    /**
     * A REGUA DA FRAGILIDADE. A secao 37 escreve "corpo relativamente fragil" -- e
     * "relativamente" so quer dizer alguma coisa comparado com os outros mobs publicados.
     *
     * <p>A comparacao le os PROPRIOS perfis, nao numeros copiados: assim um balanceamento
     * que suba o macaco (ou desca os outros) ate ele virar tanque reprova aqui, e nao em
     * jogo tres meses depois, quando "o macaco esta demorando para morrer" ja vai parecer
     * uma impressao.</p>
     */
    @Test
    void oMacacoEMaisFragilQueOsOutrosMobsPublicados() {
        float macaco = HunterExamProfiles.manFacedApe().attributes().maxHealth();
        float greatStamp = HunterExamProfiles.greatStamp().attributes().maxHealth();
        float sapo = HunterExamProfiles.frogInWaiting().attributes().maxHealth();

        assertTrue(macaco < greatStamp,
                "o macaco tem " + macaco + " de vida e o great stamp tem " + greatStamp
                        + ": o 'corpo relativamente fragil' da secao 37 deixou de ser verdade.");
        assertTrue(macaco < sapo,
                "o macaco tem " + macaco + " de vida e o frog-in-waiting tem " + sapo
                        + ": a forca do macaco e engano e numeros, nao aguentar pancada.");
    }

    @Test
    void aJanelaDeLuzPermiteEmboscadaDeDia() {
        assertEquals(15, HunterExamProfiles.manFacedApe().spawnRule().maxLight(),
                "o macaco se disfarca de GENTE: exigir escuridao o impediria de nascer de "
                        + "dia, que e justamente quando encontrar uma pessoa na trilha "
                        + "parece normal");
        assertTrue(HunterExamProfiles.manFacedApe().spawnRule().maxNearbySameFaction() > 1,
                "macaco anda em bando, nao sozinho");
    }

    /**
     * O mapa de publicados e o que o portao de spawn varre. Se ele deixar de listar um
     * mob, o portao continua verde -- varrendo menos. Por isso a associacao e conferida
     * aqui, e o portao la confere que a varredura nao esta vazia.
     */
    @Test
    void oMacacoEstaNoMapaDePublicadosSobOSeuProprioId() {
        Map<String, EnemyDefinition> publicados = HunterExamProfiles.publicados();
        assertTrue(publicados.keySet()
                        .containsAll(Set.of("great_stamp", "frog_in_waiting", "man_faced_ape")),
                "publicados() e a lista unica de mobs que este repositorio ja poe no mundo, e "
                        + "e ela que o portao de spawn varre: faltar aqui e sair do portao");
        // ESTA LINHA JA ESTEVE INVERTIDA, e a inversao era correta enquanto durou: o
        // foxbear nao tinha tag nem biome modifier, e lista-lo acenderia um alarme que
        // ninguem podia apagar. A issue #266 apagou o motivo, nao o alarme -- ele ganhou
        // os dois arquivos no mesmo PR. Agora a exigencia e a oposta, e ela e que importa:
        // mob registrado e ausente daqui sai do portao de spawn em SILENCIO.
        assertTrue(publicados.containsKey("foxbear"),
                "o foxbear tem EntityType registrado, tag de bioma e biome modifier: fora de "
                        + "publicados() ele fica sem o portao que confere os dois, e o sintoma "
                        + "de uma tag errada passa a ser um bioma vazio que ninguem explica");

        for (Map.Entry<String, EnemyDefinition> entrada : publicados.entrySet()) {
            assertEquals("nenfoundation:" + entrada.getKey(),
                    entrada.getValue().metadata().id().toString(),
                    "a chave do mapa e o id da entidade divergiram, e nada em jogo notaria");
        }
    }

    @Test
    void oMapaDePublicadosEImutavel() {
        Map<String, EnemyDefinition> publicados = HunterExamProfiles.publicados();
        assertThrows(UnsupportedOperationException.class,
                () -> publicados.put("outro", HunterExamProfiles.manFacedApe()),
                "mapa mutavel deixaria um chamador remover um mob da varredura do portao "
                        + "de spawn sem que nada reclamasse");
    }
}
