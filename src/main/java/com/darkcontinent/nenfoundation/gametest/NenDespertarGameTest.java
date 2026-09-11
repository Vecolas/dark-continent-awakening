package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.api.event.NenDespertadoEvent;
import com.darkcontinent.nenfoundation.api.event.NenDespertandoEvent;
import com.darkcontinent.nenfoundation.api.event.OrigemDoDespertar;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.progression.Marcos;
import com.darkcontinent.nenfoundation.server.NenAwakeningService;
import com.darkcontinent.nenfoundation.server.NenProfileService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * O ciclo de despertar, com o jogo de pe.
 *
 * <p>O teste unitario cobre a mutacao pura do perfil. O que SO existe aqui:
 * os eventos realmente disparando no barramento, o cancelamento realmente
 * impedindo a gravacao, e o attachment de um {@link ServerPlayer} de verdade.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class NenDespertarGameTest {

    private static final String TEMPLATE = "empty";

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new GameTestAssertException(mensagem);
        }
    }

    /**
     * Registra um listener e garante que ele sai, aconteca o que acontecer.
     *
     * <p>QUEM LIGA, DESLIGA. Um listener vazado de um gametest fica no
     * barramento pelo resto da execucao e contamina os testes seguintes -- e o
     * sintoma seria um teste que passa sozinho e falha na suite.
     */
    private static <T extends net.neoforged.bus.api.Event> void comListener(
            Class<T> tipo, Consumer<T> listener, Runnable corpo) {
        NeoForge.EVENT_BUS.addListener(tipo, listener);
        try {
            corpo.run();
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void despertarGravaEAnuncia(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        List<OrigemDoDespertar> anunciados = new ArrayList<>();

        comListener(NenDespertadoEvent.class,
                e -> {
                    if (e.jogador() == jogador) {
                        anunciados.add(e.origem());
                        // O evento informativo so dispara com o perfil ja
                        // gravado. Se dispararmos antes, quem escuta le o
                        // estado ANTIGO e decide errado.
                        exigir(NenProfileService.ler(jogador).awakened(),
                                "O evento informativo disparou antes de o perfil"
                                        + " ser gravado. A ordem e contrato:"
                                        + " perguntar, gravar, anunciar.");
                    }
                },
                () -> {
                    NenAwakeningService.Resultado r =
                            NenAwakeningService.despertar(jogador, OrigemDoDespertar.TREINO);
                    exigir(r == NenAwakeningService.Resultado.DESPERTOU,
                            "Esperava DESPERTOU, veio " + r);
                });

        PersistentNenData perfil = NenProfileService.ler(jogador);
        exigir(perfil.awakened(), "awakened nao persistiu no attachment.");
        exigir(perfil.temMarco(Marcos.DESPERTOU), "o marco nao foi gravado.");
        exigir(perfil.category() == NenCategory.UNDETERMINED,
                "despertar nao pode atribuir categoria; veio " + perfil.category());
        exigir(anunciados.size() == 1,
                "o evento informativo disparou " + anunciados.size() + " vez(es).");
        exigir(anunciados.get(0) == OrigemDoDespertar.TREINO,
                "a origem nao chegou ao evento: " + anunciados.get(0));

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void cancelarImpedeTudo(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        List<String> anunciados = new ArrayList<>();

        comListener(NenDespertadoEvent.class,
                e -> anunciados.add("anunciado"),
                () -> comListener(NenDespertandoEvent.class,
                        e -> {
                            if (e.jogador() == jogador) {
                                e.setCanceled(true);
                            }
                        },
                        () -> {
                            NenAwakeningService.Resultado r = NenAwakeningService
                                    .despertar(jogador, OrigemDoDespertar.FORCADO);
                            exigir(r == NenAwakeningService.Resultado.CANCELADO,
                                    "Esperava CANCELADO, veio " + r);
                        }));

        PersistentNenData perfil = NenProfileService.ler(jogador);
        exigir(!perfil.awakened(),
                "O jogador despertou mesmo com o evento cancelado. Gravar antes"
                        + " de perguntar produz um estado que ninguem autorizou, e"
                        + " desfazer isso e outra operacao que ninguem escreveu.");
        exigir(!perfil.temMarco(Marcos.DESPERTOU),
                "O marco entrou mesmo com o cancelamento: estado pela metade.");
        exigir(anunciados.isEmpty(),
                "O evento informativo disparou para um despertar que nao"
                        + " aconteceu.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void despertarDuasVezesNaoAnunciaDuasVezes(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        List<String> anunciados = new ArrayList<>();

        comListener(NenDespertadoEvent.class,
                e -> {
                    if (e.jogador() == jogador) {
                        anunciados.add("anunciado");
                    }
                },
                () -> {
                    exigir(NenAwakeningService.despertar(jogador, OrigemDoDespertar.TREINO)
                                    == NenAwakeningService.Resultado.DESPERTOU,
                            "a primeira chamada devia despertar.");
                    exigir(NenAwakeningService.despertar(jogador, OrigemDoDespertar.COMANDO)
                                    == NenAwakeningService.Resultado.JA_ESTAVA,
                            "a segunda chamada devia dizer JA_ESTAVA, e nao ser erro.");
                });

        exigir(anunciados.size() == 1,
                "O onboarding dispararia duas vezes: o evento informativo saiu "
                        + anunciados.size() + " vezes. Quest que completa duas vezes,"
                        + " comando repetido e clique duplo existem.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void doisJogadoresDespertamIndependentes(GameTestHelper helper) {
        ServerPlayer um = helper.makeMockServerPlayerInLevel();
        ServerPlayer outro = helper.makeMockServerPlayerInLevel();

        NenAwakeningService.despertar(um, OrigemDoDespertar.TREINO);

        exigir(NenProfileService.ler(um).awakened(), "o primeiro nao despertou.");
        exigir(!NenProfileService.ler(outro).awakened(),
                "O segundo jogador despertou sozinho. O perfil do primeiro vazou.");
        exigir(!NenProfileService.ler(outro).temMarco(Marcos.DESPERTOU),
                "O marco do primeiro apareceu no segundo.");

        helper.succeed();
    }
}
