package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.enemy.ai.AmbushRules;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.GrabRules;
import com.darkcontinent.nenfoundation.enemy.data.EnemyDefinition;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Sustenta que o perfil do frog-in-waiting e a unica fonte dos numeros da emboscada, e
 * que a JANELA DE ESCAPE do agarrao continua existindo depois de qualquer balanceamento.
 *
 * <p>Carrega tambem o portao de spawn: tag de bioma declarada no perfil sem arquivo de
 * tag, ou tag sem biome modifier, nao produz erro nenhum -- produz um mob que nunca
 * aparece no mundo.</p>
 */
class FrogInWaitingPerfilTest {

    /** Vida de um jogador cheio, em pontos. Nao e ajustavel: e a regra do jogo base. */
    private static final float VIDA_DE_UM_JOGADOR = 20.0F;

    /**
     * Tres segundos. E o minimo para "janela de escape" querer dizer alguma coisa: menos
     * que isso o jogador morre antes de terminar de entender o que o pegou.
     */
    private static final int JANELA_MINIMA_DE_REACAO = 60;

    @Test
    void numerosDaEmboscadaMoramNoPerfil() {
        AmbushRules regras = HunterExamProfiles.frogAmbushRules();
        assertEquals(2.5D, regras.raioDeGatilho());
        assertEquals(2.0D, regras.alturaDeGatilho());
        assertEquals(100, regras.ticksDeRecarga());
        assertEquals(60, regras.ticksParaReenterrar());
    }

    @Test
    void numerosDoAgarraoMoramNoPerfil() {
        GrabRules regras = HunterExamProfiles.frogGrabRules();
        assertEquals(100, regras.ticksMaximos());
        assertEquals(20, regras.intervaloDeDano());
        assertEquals(3.0F, regras.danoPorPulso());
        assertEquals(12.0F, regras.danoParaEscapar());
    }

    @Test
    void numerosDaEngolidaMoramNoPerfil() {
        AttackDefinition swallow = HunterExamProfiles.frogSwallow();
        assertEquals("swallow", swallow.id());
        assertEquals(10, swallow.windupTicks(), "sem windup a emboscada vira morte sem aviso");
        assertEquals(4, swallow.activeTicks());
        assertEquals(20, swallow.recoveryTicks());
        assertEquals(10.0F, swallow.damage());
        assertEquals(0.4F, swallow.knockback());
    }

    /**
     * A REGUA DA JANELA DE ESCAPE, e o coracao deste mob.
     *
     * <p>A secao 38 do plano proibe que engolir mate NA HORA. Ela nao proibe que engolir
     * mate: um predador que te engole e uma ameaca de morte, senao o agarrao vira
     * enfeite. O que o plano exige e TEMPO para reagir -- bater, escapar, um aliado
     * ajudar.</p>
     *
     * <p>Por isso a regua mede as duas coisas separadas, e a conta inclui A BOCADA:
     * medir so os pulsos daria 15 contra 20 e pareceria folgado, quando o total real e
     * 10 da mordida mais os pulsos. Uma regua que esquece a mordida aprova em silencio
     * justamente o balanceamento que mata sem aviso.</p>
     */
    @Test
    void aBocadaSozinhaNaoMataUmJogadorCheio() {
        float bocada = HunterExamProfiles.frogSwallow().damage();
        assertTrue(bocada < VIDA_DE_UM_JOGADOR,
                "a mordida sozinha tira " + bocada + " de " + VIDA_DE_UM_JOGADOR
                        + ": engolir virou morte instantanea, que e o que a secao 38 proibe.");
    }

    @Test
    void quemFoiEngolidoTemTempoDeVerdadeParaReagir() {
        GrabRules regras = HunterExamProfiles.frogGrabRules();
        float acumulado = HunterExamProfiles.frogSwallow().damage();
        int ticksAteMorrer = -1;
        for (int tick = 1; tick <= regras.ticksMaximos() && ticksAteMorrer < 0; tick++) {
            if (regras.aplicaDano(tick)) acumulado += regras.danoPorPulso();
            if (acumulado >= VIDA_DE_UM_JOGADOR) ticksAteMorrer = tick;
        }

        // -1 significa que quem ficou parado sai vivo; qualquer valor >= JANELA_MINIMA
        // significa que morreu, mas depois de tempo suficiente para ter feito algo.
        assertTrue(ticksAteMorrer < 0 || ticksAteMorrer >= JANELA_MINIMA_DE_REACAO,
                "um jogador cheio e sem armadura morre no tick " + ticksAteMorrer
                        + " do agarrao, e a janela minima para reagir e "
                        + JANELA_MINIMA_DE_REACAO + " ticks. Aumente intervaloDeDano, reduza "
                        + "danoPorPulso ou reduza o dano da bocada.");
    }

    /**
     * A outra metade da mesma regua: escapar tem de ser mais barato que matar o sapo.
     * Se nao for, a "janela de escape" existe so no papel -- em jogo o unico caminho e
     * abater o predador, e ninguem consegue explicar por que.
     */
    @Test
    void escaparEMaisBaratoQueMatarOSapo() {
        GrabRules regras = HunterExamProfiles.frogGrabRules();
        float vidaDoSapo = HunterExamProfiles.frogInWaiting().attributes().maxHealth();

        assertTrue(regras.danoParaEscapar() < vidaDoSapo,
                "escapar custa " + regras.danoParaEscapar() + " de dano e matar o sapo custa "
                        + vidaDoSapo + ": matar o predador e mais facil que se soltar dele, "
                        + "entao a janela de escape nao existe na pratica.");
    }

    @Test
    void aJanelaDeLuzPermiteEmboscadaDeDia() {
        assertEquals(15, HunterExamProfiles.frogInWaiting().spawnRule().maxLight(),
                "o sapo fica ENTERRADO: exigir escuridao o impediria de nascer no pantano "
                        + "de dia, que e justamente quando alguem passa por cima dele");
    }

    /**
     * PORTAO DE SPAWN. Para cada id que ESTE repositorio ja publica como entidade, a tag
     * de bioma declarada no perfil precisa existir como arquivo de tag E ser referenciada
     * por pelo menos um biome modifier.
     *
     * <p>A lista dos publicados vem de {@link HunterExamProfiles#publicados()}, e nao de
     * um bloco escrito a mao aqui dentro. Escrita a mao ela era uma SEGUNDA fonte da
     * mesma verdade: o mob novo entrava no jogo e ficava de fora do portao, que seguia
     * verde varrendo menos. Agora o lugar de lembrar e um so, e todo mob publicado cai
     * neste portao sozinho.</p>
     *
     * <p>O "foxbear" era a excecao escrita aqui, e deixou de ser na issue #266: ele
     * ganhou tag e biome modifier no mesmo PR que o pos em {@code publicados()}, e
     * agora cai neste portao como os outros seis. Enquanto a excecao existiu, o custo
     * dela foi invisivel do jeito pior: o mob simplesmente nao nascia em lugar nenhum,
     * e este portao ficava verde porque nao o varria.</p>
     */
    @Test
    void todaTagDeBiomaDeclaradaExisteETemBiomeModifier() {
        Map<String, EnemyDefinition> publicados = HunterExamProfiles.publicados();
        assertFalse(publicados.isEmpty(),
                "HunterExamProfiles.publicados() veio vazio: varredura vazia nao e aprovacao, "
                        + "e um portao que nao varre ninguem passa sempre");

        Path raiz = Repo.raiz();
        List<Path> modificadores = Repo.varrer(
                "src/main/resources/data/nenfoundation/neoforge/biome_modifier", ".json");
        assertFalse(modificadores.isEmpty(),
                "nenhum biome modifier encontrado; varredura vazia nao e aprovacao");

        for (Map.Entry<String, EnemyDefinition> entrada : publicados.entrySet()) {
            String id = entrada.getKey();
            for (String tag : entrada.getValue().spawnRule().biomeTags()) {
                assertTrue(tag.startsWith("#"),
                        id + " declara o bioma '" + tag + "' sem o '#': isso nao e uma tag");
                String semCerquilha = tag.substring(1);
                int doisPontos = semCerquilha.indexOf(':');
                assertTrue(doisPontos > 0 && doisPontos < semCerquilha.length() - 1,
                        id + " declara a tag '" + tag + "' sem namespace:caminho");

                String namespace = semCerquilha.substring(0, doisPontos);
                String caminho = semCerquilha.substring(doisPontos + 1);
                Path arquivoDaTag = raiz.resolve("src/main/resources/data").resolve(namespace)
                        .resolve("tags/worldgen/biome").resolve(caminho + ".json");

                assertTrue(Files.isRegularFile(arquivoDaTag),
                        id + " declara a tag de bioma " + tag + ", mas o arquivo "
                                + raiz.relativize(arquivoDaTag)
                                + " nao existe. Tag sem arquivo nao da erro nenhum: da um mob "
                                + "que nunca aparece no mundo.");

                boolean referenciada = modificadores.stream()
                        .map(FrogInWaitingPerfilTest::ler)
                        .anyMatch(texto -> texto.contains(tag));
                assertTrue(referenciada,
                        id + " declara a tag de bioma " + tag + ", que existe como arquivo mas "
                                + "nenhum biome_modifier em "
                                + "src/main/resources/data/nenfoundation/neoforge/biome_modifier/ "
                                + "referencia. Sem modifier o jogo nunca adiciona o spawn, e "
                                + "tambem nao reclama.");
            }
        }
    }

    private static String ler(Path arquivo) {
        try {
            return Files.readString(arquivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
