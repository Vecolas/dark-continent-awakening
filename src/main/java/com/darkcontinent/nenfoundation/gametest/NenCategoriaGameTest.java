package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.api.event.CategoriaAtribuidaEvent;
import com.darkcontinent.nenfoundation.api.event.CategoriaReveladaEvent;
import com.darkcontinent.nenfoundation.api.event.OrigemDoDespertar;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.progression.Marcos;
import com.darkcontinent.nenfoundation.server.NenAwakeningService;
import com.darkcontinent.nenfoundation.server.NenCategoryService;
import com.darkcontinent.nenfoundation.server.NenProfileService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Atribuicao e revelacao de categoria, com o jogo de pe.
 *
 * <p>O teste unitario cobre as mutacoes puras do perfil. O que SO existe aqui:
 * as recusas do servico, os eventos realmente disparando no barramento, o
 * attachment de um {@link ServerPlayer} de verdade, e a pergunta que da titulo
 * ao M3 -- o que o cliente CONSEGUE saber antes da revelacao.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class NenCategoriaGameTest {

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

    private static ServerPlayer jogadorDesperto(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        NenAwakeningService.despertar(jogador, OrigemDoDespertar.TREINO);
        return jogador;
    }

    // ------------------------------------------------------ atribuicao

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void atribuirGravaEscondidoEAnuncia(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        List<NenCategory> anunciadas = new ArrayList<>();

        comListener(CategoriaAtribuidaEvent.class,
                e -> {
                    if (e.jogador() == jogador) {
                        anunciadas.add(e.categoria());
                        // A ordem e contrato: gravar, depois anunciar. Quem
                        // escuta le o perfil e ve o estado NOVO.
                        exigir(NenProfileService.ler(jogador).category().eReal(),
                                "O evento disparou antes de o perfil ser gravado.");
                    }
                },
                () -> {
                    NenCategoryService.Atribuicao r = NenCategoryService
                            .atribuir(jogador, NenCategory.CONJURATION);
                    exigir(r == NenCategoryService.Atribuicao.ATRIBUIU,
                            "Esperava ATRIBUIU, veio " + r);
                });

        PersistentNenData perfil = NenProfileService.ler(jogador);
        exigir(perfil.category() == NenCategory.CONJURATION,
                "a categoria nao persistiu; veio " + perfil.category());
        exigir(!perfil.categoryRevealed(),
                "Atribuir revelou junto. Sao dois fatos diferentes.");
        exigir(anunciadas.size() == 1,
                "o evento de atribuicao disparou " + anunciadas.size() + " vez(es).");
        exigir(anunciadas.get(0) == NenCategory.CONJURATION,
                "a categoria nao chegou ao evento: " + anunciadas.get(0));

        helper.succeed();
    }

    /**
     * O teste que o M3 existe para poder fazer.
     *
     * <p>O snapshot e o unico caminho pelo qual a categoria chega ao cliente.
     * Se {@code categoriaVisivel()} entregar a categoria real antes da hora,
     * qualquer cliente modificado ja sabe o resultado da Water Divination -- e
     * nenhum playtest com cliente honesto encontraria isso.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void antesDeRevelarOClienteNaoConsegueSaber(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        NenCategoryService.atribuir(jogador, NenCategory.SPECIALIZATION);

        PersistentNenData perfil = NenProfileService.ler(jogador);

        exigir(perfil.category() == NenCategory.SPECIALIZATION,
                "o servidor perdeu a categoria real.");
        exigir(perfil.categoriaVisivel() == NenCategory.UNDETERMINED,
                "A projecao entregou " + perfil.categoriaVisivel() + " antes da"
                        + " revelacao. E este o valor que vai para o snapshot:"
                        + " com ele errado, a revelacao vira teatro.");
        exigir(!perfil.temMarco(Marcos.CATEGORIA_REVELADA),
                "O marco da revelacao viaja no snapshot ate o cliente. Ele nao"
                        + " pode existir antes de o jogador saber.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void atribuirRecusaQuemNaoDespertou(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        List<String> anunciadas = new ArrayList<>();

        comListener(CategoriaAtribuidaEvent.class,
                e -> anunciadas.add("anunciada"),
                () -> {
                    NenCategoryService.Atribuicao r = NenCategoryService
                            .atribuir(jogador, NenCategory.ENHANCEMENT);
                    exigir(r == NenCategoryService.Atribuicao.NAO_DESPERTO,
                            "Esperava NAO_DESPERTO, veio " + r);
                });

        exigir(NenProfileService.ler(jogador).category() == NenCategory.UNDETERMINED,
                "Um jogador que nunca tocou em Nen ficou com categoria escondida."
                        + " O invariante 'categoria real implica desperto' precisa"
                        + " valer para o relatorio e para a quest.");
        exigir(anunciadas.isEmpty(),
                "O evento disparou para uma atribuicao que nao aconteceu.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void atribuirDuasVezesNaoTrocaNemReanuncia(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        List<NenCategory> anunciadas = new ArrayList<>();

        comListener(CategoriaAtribuidaEvent.class,
                e -> {
                    if (e.jogador() == jogador) {
                        anunciadas.add(e.categoria());
                    }
                },
                () -> {
                    exigir(NenCategoryService.atribuir(jogador, NenCategory.EMISSION)
                                    == NenCategoryService.Atribuicao.ATRIBUIU,
                            "a primeira chamada devia atribuir.");
                    exigir(NenCategoryService.atribuir(jogador, NenCategory.MANIPULATION)
                                    == NenCategoryService.Atribuicao.JA_TINHA,
                            "a segunda chamada devia dizer JA_TINHA, e nao ser erro.");
                });

        exigir(NenProfileService.ler(jogador).category() == NenCategory.EMISSION,
                "A segunda chamada TROCOU a categoria do jogador. Trocar"
                        + " categoria nao faz parte do M3, e se um dia fizer"
                        + " precisa de decisao registrada e de nome proprio.");
        exigir(anunciadas.size() == 1,
                "o evento saiu " + anunciadas.size() + " vezes.");

        helper.succeed();
    }

    // ------------------------------------------------------- revelacao

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void revelarAnunciaEAbreAProjecao(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        NenCategoryService.atribuir(jogador, NenCategory.TRANSMUTATION);
        List<NenCategory> anunciadas = new ArrayList<>();

        comListener(CategoriaReveladaEvent.class,
                e -> {
                    if (e.jogador() == jogador) {
                        anunciadas.add(e.categoria());
                        // Aqui o perfil ja foi gravado E o snapshot ja saiu.
                        exigir(NenProfileService.ler(jogador).categoriaVisivel()
                                        == NenCategory.TRANSMUTATION,
                                "O evento de revelacao disparou antes de a"
                                        + " projecao abrir. Quem escuta mandaria a"
                                        + " mensagem de descoberta para um cliente"
                                        + " que ainda ve Undetermined.");
                    }
                },
                () -> {
                    NenCategoryService.Revelacao r = NenCategoryService.revelar(jogador);
                    exigir(r == NenCategoryService.Revelacao.REVELOU,
                            "Esperava REVELOU, veio " + r);
                });

        PersistentNenData perfil = NenProfileService.ler(jogador);
        exigir(perfil.categoryRevealed(), "categoryRevealed nao persistiu.");
        exigir(perfil.temMarco(Marcos.CATEGORIA_REVELADA), "o marco nao foi gravado.");
        exigir(perfil.category() == NenCategory.TRANSMUTATION,
                "a revelacao trocou a categoria; veio " + perfil.category());
        exigir(anunciadas.size() == 1,
                "o evento de revelacao disparou " + anunciadas.size() + " vez(es).");
        exigir(anunciadas.get(0) == NenCategory.TRANSMUTATION,
                "a categoria nao chegou ao evento: " + anunciadas.get(0));

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void revelarSemCategoriaRecusaEmVezDeSortear(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        List<String> anunciadas = new ArrayList<>();

        comListener(CategoriaReveladaEvent.class,
                e -> anunciadas.add("anunciada"),
                () -> {
                    NenCategoryService.Revelacao r = NenCategoryService.revelar(jogador);
                    exigir(r == NenCategoryService.Revelacao.SEM_CATEGORIA,
                            "Esperava SEM_CATEGORIA, veio " + r);
                });

        PersistentNenData perfil = NenProfileService.ler(jogador);
        exigir(perfil.category() == NenCategory.UNDETERMINED,
                "A revelacao INVENTOU uma categoria: " + perfil.category()
                        + ". Consertar 'quando falta' esconde para sempre quem"
                        + " chamou o ritual fora de ordem.");
        exigir(!perfil.categoryRevealed(),
                "Revelou o neutro: o jogador veria 'Undetermined' como resultado.");
        exigir(anunciadas.isEmpty(),
                "O evento disparou para uma revelacao que nao aconteceu.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void revelarDuasVezesNaoReanuncia(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        NenCategoryService.atribuir(jogador, NenCategory.ENHANCEMENT);
        List<String> anunciadas = new ArrayList<>();

        comListener(CategoriaReveladaEvent.class,
                e -> {
                    if (e.jogador() == jogador) {
                        anunciadas.add("anunciada");
                    }
                },
                () -> {
                    exigir(NenCategoryService.revelar(jogador)
                                    == NenCategoryService.Revelacao.REVELOU,
                            "a primeira chamada devia revelar.");
                    exigir(NenCategoryService.revelar(jogador)
                                    == NenCategoryService.Revelacao.JA_SABIA,
                            "a segunda devia dizer JA_SABIA, e nao ser erro.");
                });

        exigir(anunciadas.size() == 1,
                "A cutscene de descoberta tocaria duas vezes: o evento saiu "
                        + anunciadas.size() + " vezes.");

        helper.succeed();
    }

    /**
     * Consertar um perfil pela metade nao pode reanunciar a descoberta.
     *
     * <p>UM PERFIL REVELADO PODE CHEGAR SEM O MARCO: qualquer save gravado
     * antes de {@code nenfoundation:categoria_revelada} existir esta nesse
     * estado. A revelacao conserta isso -- e tem de consertar EM SILENCIO.
     *
     * <p>Este teste existe porque a primeira versao do servico decidia se
     * anunciava olhando o booleano E o marco juntos. Com o marco faltando, ela
     * tratava um jogador que ja sabia a categoria ha semanas como uma
     * descoberta nova: cutscene, mensagem e quest disparando de novo, no
     * primeiro login depois da atualizacao. Nenhum teste pegava, porque todo
     * teste construia perfis consistentes.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void consertarPerfilPelaMetadeNaoReanuncia(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);
        NenCategoryService.atribuir(jogador, NenCategory.ENHANCEMENT);

        // Um save de antes deste marco existir: revelado, e sem o marco.
        NenProfileService.atualizar(jogador, antes -> new PersistentNenData(
                antes.schemaVersion(),
                antes.awakened(),
                antes.category(),
                true,
                antes.auraPotential(),
                antes.control(),
                antes.output(),
                antes.techniqueProficiency(),
                antes.unlockedTechniques(),
                antes.unlockedAbilities(),
                Set.of(Marcos.DESPERTOU)));

        PersistentNenData meiaEstado = NenProfileService.ler(jogador);
        exigir(meiaEstado.categoryRevealed() && !meiaEstado.temMarco(Marcos.CATEGORIA_REVELADA),
                "o teste nao conseguiu montar o estado pela metade que ele mede.");

        List<String> anunciadas = new ArrayList<>();
        comListener(CategoriaReveladaEvent.class,
                e -> {
                    if (e.jogador() == jogador) {
                        anunciadas.add("anunciada");
                    }
                },
                () -> {
                    NenCategoryService.Revelacao r = NenCategoryService.revelar(jogador);
                    exigir(r == NenCategoryService.Revelacao.JA_SABIA,
                            "Esperava JA_SABIA para quem ja tinha o booleano"
                                    + " ligado, veio " + r);
                });

        exigir(NenProfileService.ler(jogador).temMarco(Marcos.CATEGORIA_REVELADA),
                "o marco que faltava nao foi consertado.");
        exigir(anunciadas.isEmpty(),
                "A descoberta foi reanunciada so porque um marco estava"
                        + " faltando. Quem ja sabia a categoria veria a cutscene"
                        + " de novo, no primeiro login depois da atualizacao.");

        helper.succeed();
    }

    // ------------------------------------------- sorteio e dois jogadores

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void sorteioEDeterministicoEPrevisivel(GameTestHelper helper) {
        ServerPlayer jogador = jogadorDesperto(helper);

        NenCategory previsto = NenCategoryService.sorteioPara(jogador);
        exigir(previsto == NenCategoryService.sorteioPara(jogador),
                "Duas consultas ao sorteio deram categorias diferentes para o"
                        + " mesmo jogador no mesmo mundo.");
        exigir(previsto.eReal(), "o sorteio devolveu o neutro: " + previsto);

        NenCategoryService.Atribuicao r = NenCategoryService.atribuirPorSorteio(jogador);
        exigir(r == NenCategoryService.Atribuicao.ATRIBUIU, "Esperava ATRIBUIU, veio " + r);
        exigir(NenProfileService.ler(jogador).category() == previsto,
                "A previsao disse " + previsto + " e a atribuicao gravou "
                        + NenProfileService.ler(jogador).category()
                        + ". Prever e atribuir por dois caminhos diferentes e a"
                        + " mesma verdade em duas fontes.");

        helper.succeed();
    }

    /** O item do gate: duas pessoas, categorias independentes e persistentes. */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void doisJogadoresSaoIndependentes(GameTestHelper helper) {
        ServerPlayer um = jogadorDesperto(helper);
        ServerPlayer outro = jogadorDesperto(helper);

        NenCategoryService.atribuir(um, NenCategory.EMISSION);
        NenCategoryService.revelar(um);

        exigir(NenProfileService.ler(um).category() == NenCategory.EMISSION,
                "o primeiro perdeu a categoria.");
        exigir(NenProfileService.ler(outro).category() == NenCategory.UNDETERMINED,
                "A categoria do primeiro vazou para o segundo.");
        exigir(!NenProfileService.ler(outro).categoryRevealed(),
                "A revelacao do primeiro revelou o segundo.");
        exigir(!NenProfileService.ler(outro).temMarco(Marcos.CATEGORIA_REVELADA),
                "O marco do primeiro apareceu no segundo.");

        // E o segundo consegue ter uma categoria DIFERENTE, persistida em
        // paralelo. Sem isto, "independentes" so provaria que um deles fica
        // vazio.
        NenCategoryService.atribuir(outro, NenCategory.MANIPULATION);
        exigir(NenProfileService.ler(um).category() == NenCategory.EMISSION,
                "atribuir ao segundo mudou o primeiro.");
        exigir(NenProfileService.ler(outro).category() == NenCategory.MANIPULATION,
                "o segundo nao recebeu a propria categoria.");

        helper.succeed();
    }
}
