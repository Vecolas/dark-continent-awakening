package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.api.event.CategoriaAtribuidaEvent;
import com.darkcontinent.nenfoundation.api.event.CategoriaReveladaEvent;
import com.darkcontinent.nenfoundation.api.event.OrigemDoDespertar;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.category.SorteioDeCategoria;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.progression.Marcos;
import com.darkcontinent.nenfoundation.network.payload.SnapshotDePerfilS2C;
import com.darkcontinent.nenfoundation.server.NenAwakeningService;
import com.darkcontinent.nenfoundation.server.NenCategoryService;
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
 * Atribuicao e revelacao de categoria, com o jogo de pe.
 *
 * <p>O teste unitario cobre as mutacoes puras. O que SO existe aqui: os
 * eventos disparando no barramento, a idempotencia do servico inteiro (e nao
 * so da mutacao), a semente do mundo chegando ao sorteio, e o snapshot que o
 * cliente receberia -- que e onde a categoria poderia vazar.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class NenCategoriaGameTest {

    private static final String TEMPLATE = "empty";

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new GameTestAssertException(mensagem);
        }
    }

    /** Registra um listener e garante que ele sai. QUEM LIGA, DESLIGA. */
    private static <T extends net.neoforged.bus.api.Event> void comListener(
            Class<T> tipo, Consumer<T> listener, Runnable corpo) {
        NeoForge.EVENT_BUS.addListener(tipo, listener);
        try {
            corpo.run();
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
    }

    private static ServerPlayer despertado(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        NenAwakeningService.despertar(jogador, OrigemDoDespertar.TREINO);
        return jogador;
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void atribuirGravaEAnuncia(GameTestHelper helper) {
        ServerPlayer jogador = despertado(helper);
        List<NenCategory> anunciadas = new ArrayList<>();

        comListener(CategoriaAtribuidaEvent.class,
                e -> {
                    if (e.jogador() == jogador) {
                        anunciadas.add(e.categoria());
                        // A ordem e contrato: gravar, depois anunciar.
                        exigir(NenProfileService.ler(jogador).category().eReal(),
                                "O evento disparou antes de o perfil ser gravado."
                                        + " Quem escuta leria o estado ANTIGO.");
                    }
                },
                () -> {
                    NenCategoryService.Atribuicao r = NenCategoryService.atribuir(jogador);
                    exigir(r == NenCategoryService.Atribuicao.ATRIBUIU,
                            "Esperava ATRIBUIU, veio " + r);
                });

        PersistentNenData perfil = NenProfileService.ler(jogador);
        exigir(perfil.category().eReal(),
                "A categoria nao persistiu: veio " + perfil.category());
        exigir(!perfil.categoryRevealed(),
                "Atribuir revelou junto. Sao dois fatos, e o schema v1 os separa.");
        exigir(anunciadas.size() == 1,
                "O evento disparou " + anunciadas.size() + " vez(es).");
        exigir(anunciadas.get(0) == perfil.category(),
                "O evento anunciou " + anunciadas.get(0) + " e o perfil guardou "
                        + perfil.category() + ".");

        helper.succeed();
    }

    /**
     * A prova de que a categoria NAO vaza antes da revelacao.
     *
     * <p>Ela e feita sobre o snapshot que sai para o cliente, e nao sobre
     * {@code categoriaVisivel()}. Conferir o metodo provaria que o metodo
     * funciona; conferir o snapshot prova que quem o preenche CHAMOU o metodo
     * certo -- que e o defeito possivel.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void oSnapshotNaoCarregaACategoriaAntesDaRevelacao(GameTestHelper helper) {
        ServerPlayer jogador = despertado(helper);
        NenCategoryService.atribuir(jogador);

        PersistentNenData perfil = NenProfileService.ler(jogador);
        exigir(perfil.category().eReal(), "a atribuicao nao aconteceu.");

        SnapshotDePerfilS2C oculto = NenSondaDeSnapshot.criar(perfil);
        exigir(oculto.categoriaVisivel() == NenCategory.UNDETERMINED,
                "A CATEGORIA VAZOU NO SNAPSHOT: o cliente recebeu "
                        + oculto.categoriaVisivel() + " antes da revelacao. Qualquer"
                        + " cliente modificado leria a resposta da Adivinhacao da"
                        + " Agua antes do ritual.");
        exigir(!oculto.marcos().contains(Marcos.CATEGORIA_REVELADA),
                "O marco de revelacao viajou antes de a revelacao acontecer.");

        NenCategoryService.revelar(jogador);
        PersistentNenData revelado = NenProfileService.ler(jogador);
        SnapshotDePerfilS2C aberto = NenSondaDeSnapshot.criar(revelado);

        exigir(aberto.categoriaVisivel() == perfil.category(),
                "Depois de revelada, o snapshot devia trazer " + perfil.category()
                        + " e trouxe " + aberto.categoriaVisivel() + ".");
        exigir(aberto.marcos().contains(Marcos.CATEGORIA_REVELADA),
                "O marco de revelacao nao chegou ao snapshot.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void revelarAnunciaUmaVezSo(GameTestHelper helper) {
        ServerPlayer jogador = despertado(helper);
        NenCategoryService.atribuir(jogador);
        List<NenCategory> anunciadas = new ArrayList<>();

        comListener(CategoriaReveladaEvent.class,
                e -> {
                    if (e.jogador() == jogador) {
                        anunciadas.add(e.categoria());
                    }
                },
                () -> {
                    exigir(NenCategoryService.revelar(jogador)
                                    == NenCategoryService.Revelacao.REVELOU,
                            "a primeira revelacao devia acontecer.");
                    exigir(NenCategoryService.revelar(jogador)
                                    == NenCategoryService.Revelacao.JA_SABIA,
                            "a segunda devia dizer JA_SABIA, e nao ser erro.");
                });

        exigir(anunciadas.size() == 1,
                "O onboarding dispararia duas vezes: o evento saiu "
                        + anunciadas.size() + " vezes.");
        exigir(anunciadas.get(0) == NenProfileService.ler(jogador).category(),
                "o evento anunciou uma categoria diferente da gravada.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void atribuirDuasVezesNaoTrocaACategoria(GameTestHelper helper) {
        ServerPlayer jogador = despertado(helper);
        List<String> anunciadas = new ArrayList<>();

        NenCategoryService.atribuir(jogador);
        NenCategory primeira = NenProfileService.ler(jogador).category();

        comListener(CategoriaAtribuidaEvent.class,
                e -> anunciadas.add("anunciada"),
                () -> exigir(NenCategoryService.atribuir(jogador)
                                == NenCategoryService.Atribuicao.JA_TINHA,
                        "a segunda atribuicao devia dizer JA_TINHA."));

        exigir(NenProfileService.ler(jogador).category() == primeira,
                "A categoria mudou na segunda chamada: era " + primeira + ", virou "
                        + NenProfileService.ler(jogador).category() + ". Trocar a"
                        + " categoria de alguem e uma decisao de design que ninguem"
                        + " tomou.");
        exigir(anunciadas.isEmpty(),
                "O evento disparou para uma atribuicao que nao aconteceu.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void quemNaoDespertouNaoRecebeCategoria(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();

        exigir(NenCategoryService.atribuir(jogador)
                        == NenCategoryService.Atribuicao.NAO_DESPERTOU,
                "Atribuiu categoria a quem nao despertou.");
        exigir(NenProfileService.ler(jogador).category() == NenCategory.UNDETERMINED,
                "Escreveu categoria mesmo recusando. UNDETERMINED significa 'sem"
                        + " Nen, ou com Nen e ainda sem categoria'; gravar sobre quem"
                        + " nao despertou quebra essa leitura.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void revelarSemCategoriaNaoFazNada(GameTestHelper helper) {
        ServerPlayer jogador = despertado(helper);

        exigir(NenCategoryService.revelar(jogador)
                        == NenCategoryService.Revelacao.SEM_CATEGORIA,
                "Revelou o que nao existe.");
        exigir(!NenProfileService.ler(jogador).categoryRevealed(),
                "Ligou category_revealed sobre UNDETERMINED: o jogador passaria a"
                        + " 'saber' que e indeterminado, e a atribuicao seguinte"
                        + " tornaria isso mentira em silencio.");

        helper.succeed();
    }

    /**
     * Dois jogadores, um mundo: categorias independentes, e cada uma igual ao
     * que o sorteio puro devolve para aquele UUID.
     *
     * <p>Esta e a parte que so o gametest prova: que a semente que chega ao
     * sorteio e mesmo a do mundo, e nao um zero esquecido.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void aSementeDoMundoAlimentaOSorteio(GameTestHelper helper) {
        ServerPlayer um = despertado(helper);
        ServerPlayer outro = despertado(helper);

        long semente = helper.getLevel().getServer().overworld().getSeed();

        NenCategoryService.atribuir(um);
        exigir(!NenProfileService.ler(outro).category().eReal(),
                "O segundo jogador ganhou categoria sozinho: o perfil do"
                        + " primeiro vazou.");

        NenCategoryService.atribuir(outro);

        NenCategory esperadaUm = SorteioDeCategoria.sortear(semente, um.getUUID());
        NenCategory esperadaOutro = SorteioDeCategoria.sortear(semente, outro.getUUID());

        exigir(NenProfileService.ler(um).category() == esperadaUm,
                "A categoria gravada nao bate com o sorteio puro para a semente do"
                        + " mundo. Esperava " + esperadaUm + ", veio "
                        + NenProfileService.ler(um).category()
                        + " -- a semente que chega ao sorteio nao e a do overworld.");
        exigir(NenProfileService.ler(outro).category() == esperadaOutro,
                "Idem para o segundo jogador: esperava " + esperadaOutro + ", veio "
                        + NenProfileService.ler(outro).category());

        helper.succeed();
    }
}
