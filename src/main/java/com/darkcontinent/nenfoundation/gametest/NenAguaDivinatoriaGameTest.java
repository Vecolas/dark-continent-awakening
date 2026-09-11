package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.api.event.CategoriaReveladaEvent;
import com.darkcontinent.nenfoundation.api.event.OrigemDoDespertar;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.divination.ResultadoDaAdivinhacao;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.progression.Marcos;
import com.darkcontinent.nenfoundation.server.NenAguaDivinatoria;
import com.darkcontinent.nenfoundation.server.NenAwakeningService;
import com.darkcontinent.nenfoundation.server.NenCategoryService;
import com.darkcontinent.nenfoundation.server.NenProfileService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * O ritual da Water Divination, com o jogo de pe.
 *
 * <p>O teste unitario cobre a TABELA de reacoes. O que SO existe aqui: a
 * montagem sendo reconhecida num mundo de verdade, o evento de interacao
 * chegando ao ritual, e o efeito no perfil de um {@link ServerPlayer} real --
 * inclusive a pergunta que da titulo a issue: refazer o teste nao re-sorteia
 * nada.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class NenAguaDivinatoriaGameTest {

    private static final String TEMPLATE = "empty";

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new GameTestAssertException(mensagem);
        }
    }

    private static <T extends net.neoforged.bus.api.Event> void comListener(
            Class<T> tipo, Consumer<T> listener, Runnable corpo) {
        NeoForge.EVENT_BUS.addListener(tipo, listener);
        try {
            corpo.run();
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
    }

    /** Monta o teste: um caldeirao com agua, e uma folha na mao do jogador. */
    private static BlockPos montarOTeste(GameTestHelper helper, ServerPlayer jogador) {
        BlockPos relativa = new BlockPos(1, 1, 1);
        helper.setBlock(relativa, Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, 3));
        jogador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_LEAVES));
        return helper.absolutePos(relativa);
    }

    /**
     * Dispara o clique pelo BARRAMENTO, e nao chamando o metodo.
     *
     * <p>O que precisa ser provado e que o ritual esta LIGADO ao evento de
     * interacao. Chamar {@code aoClicarNoBloco} direto provaria que o metodo
     * funciona e nao diria nada sobre a ligacao -- e um {@code @SubscribeEvent}
     * que ninguem registrou produz exatamente o mesmo verde.
     */
    private static void clicar(ServerPlayer jogador, BlockPos posicao) {
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(
                jogador, InteractionHand.MAIN_HAND, posicao,
                net.minecraft.world.phys.BlockHitResult.miss(
                        net.minecraft.world.phys.Vec3.atCenterOf(posicao),
                        net.minecraft.core.Direction.UP, posicao)));
    }

    /**
     * Os seis resultados sao distinguiveis em PARTICULA e em SOM.
     *
     * <p>Este pedaco do criterio de aceite so pode ser medido aqui: ler
     * {@code ParticleTypes} exige o registro do Minecraft de pe, e o JUnit puro
     * do projeto nao o tem. As frases sao conferidas la; particula e som, aqui.
     *
     * <p>Dois resultados com a mesma particula sao indistinguiveis na pratica
     * ainda que os textos difiram -- quem joga com o chat fechado ve so estes
     * dois sinais.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void osSeisSinaisSaoDistintos(GameTestHelper helper) {
        Set<String> particulas = new LinkedHashSet<>();
        Set<String> sons = new LinkedHashSet<>();

        for (ResultadoDaAdivinhacao resultado : ResultadoDaAdivinhacao.values()) {
            exigir(particulas.add(resultado.particula().toString()),
                    "duas categorias usam a mesma particula; a segunda foi "
                            + resultado.categoria().getSerializedName());
            exigir(sons.add(resultado.som().getLocation().toString()),
                    "duas categorias usam o mesmo som; a segunda foi "
                            + resultado.categoria().getSerializedName());
        }

        exigir(particulas.size() == NenCategory.REAIS.size(),
                "particulas distintas: " + particulas.size() + " para "
                        + NenCategory.REAIS.size() + " categorias.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void oTesteRevelaPelaApi(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        NenAwakeningService.despertar(jogador, OrigemDoDespertar.TREINO);
        BlockPos copo = montarOTeste(helper, jogador);

        List<NenCategory> reveladas = new ArrayList<>();
        comListener(CategoriaReveladaEvent.class,
                e -> {
                    if (e.jogador() == jogador) {
                        reveladas.add(e.categoria());
                    }
                },
                () -> clicar(jogador, copo));

        PersistentNenData perfil = NenProfileService.ler(jogador);
        exigir(perfil.category().eReal(),
                "o teste nao atribuiu categoria; veio " + perfil.category());
        exigir(perfil.categoryRevealed(), "o teste nao revelou.");
        exigir(perfil.temMarco(Marcos.CATEGORIA_REVELADA),
                "o marco da revelacao nao entrou. O ritual precisa passar pelo"
                        + " servico, e nao ligar o booleano direto.");
        exigir(reveladas.size() == 1,
                "O evento de revelacao saiu " + reveladas.size() + " vez(es). Se"
                        + " sair zero, o ritual escreveu o perfil por fora do"
                        + " servico -- uma segunda autoridade sobre a revelacao.");
        exigir(reveladas.get(0) == perfil.category(),
                "o evento anunciou categoria diferente da gravada.");
        exigir(ResultadoDaAdivinhacao.para(perfil.category()).isPresent(),
                "a categoria revelada nao tem reacao na tabela.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void refazerNaoResorteiaNada(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        NenAwakeningService.despertar(jogador, OrigemDoDespertar.TREINO);
        BlockPos copo = montarOTeste(helper, jogador);

        clicar(jogador, copo);
        NenCategory primeira = NenProfileService.ler(jogador).category();

        List<String> reanunciadas = new ArrayList<>();
        comListener(CategoriaReveladaEvent.class,
                e -> {
                    if (e.jogador() == jogador) {
                        reanunciadas.add("de novo");
                    }
                },
                () -> {
                    clicar(jogador, copo);
                    clicar(jogador, copo);
                });

        exigir(NenProfileService.ler(jogador).category() == primeira,
                "Refazer o teste TROCOU a categoria: era " + primeira + " e virou "
                        + NenProfileService.ler(jogador).category()
                        + ". O teste DESCOBRE um fato que ja existia; se ele"
                        + " re-sorteia, vira uma maquina de escolher categoria.");
        exigir(reanunciadas.isEmpty(),
                "A descoberta foi reanunciada ao refazer o teste. A cutscene e a"
                        + " quest disparariam de novo.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void semNenDespertoNaoFunciona(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        BlockPos copo = montarOTeste(helper, jogador);

        clicar(jogador, copo);

        PersistentNenData perfil = NenProfileService.ler(jogador);
        exigir(perfil.category() == NenCategory.UNDETERMINED,
                "O teste atribuiu categoria a quem nao despertou; veio "
                        + perfil.category());
        exigir(!perfil.categoryRevealed(), "o teste revelou para quem nao despertou.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void aMontagemPrecisaEstarCompleta(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        NenAwakeningService.despertar(jogador, OrigemDoDespertar.TREINO);

        BlockState comAgua = Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, 3);
        BlockState vazio = Blocks.CAULDRON.defaultBlockState();
        ItemStack folha = new ItemStack(Items.OAK_LEAVES);
        ItemStack naoFolha = new ItemStack(Items.STONE);

        exigir(NenAguaDivinatoria.montagemValida(comAgua, folha),
                "agua mais folha devia ser uma montagem valida.");
        exigir(!NenAguaDivinatoria.montagemValida(vazio, folha),
                "caldeirao SEM agua foi aceito. O cânone e um copo com AGUA.");
        exigir(!NenAguaDivinatoria.montagemValida(comAgua, naoFolha),
                "aceitou um item que nao e folha.");
        exigir(!NenAguaDivinatoria.montagemValida(
                        Blocks.STONE.defaultBlockState(), folha),
                "aceitou um bloco que nao e caldeirao.");

        // E qualquer folha serve, e nao so a de carvalho: amarrar o ritual a
        // UMA especie faria o teste falhar em silencio para quem estiver num
        // bioma de betula.
        exigir(NenAguaDivinatoria.montagemValida(comAgua, new ItemStack(Items.BIRCH_LEAVES)),
                "folha de betula nao foi aceita; a regra esta presa a uma especie.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void clicarSemAMontagemNaoFazNada(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        NenAwakeningService.despertar(jogador, OrigemDoDespertar.TREINO);

        BlockPos pedra = new BlockPos(2, 1, 2);
        helper.setBlock(pedra, Blocks.STONE);
        jogador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_LEAVES));

        clicar(jogador, helper.absolutePos(pedra));

        exigir(NenProfileService.ler(jogador).category() == NenCategory.UNDETERMINED,
                "Clicar numa pedra revelou a categoria. O ritual precisa ser um"
                        + " teste montado, e nao um clique em qualquer lugar.");

        helper.succeed();
    }

    /**
     * O ritual nao atropela quem ja tem categoria por outro caminho.
     *
     * <p>Cenario real: o operador montou o cenario com {@code /nen category set}
     * e o jogador entao faz o teste. O teste tem de REVELAR aquela categoria, e
     * nao sortear outra por cima.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void oTesteRevelaACategoriaQueJaExISTIA(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        NenAwakeningService.despertar(jogador, OrigemDoDespertar.TREINO);
        NenCategoryService.atribuir(jogador, NenCategory.SPECIALIZATION);

        BlockPos copo = montarOTeste(helper, jogador);
        clicar(jogador, copo);

        PersistentNenData perfil = NenProfileService.ler(jogador);
        exigir(perfil.category() == NenCategory.SPECIALIZATION,
                "O ritual sorteou por cima de uma categoria que ja existia; veio "
                        + perfil.category());
        exigir(perfil.categoryRevealed(), "o ritual nao revelou a categoria existente.");

        helper.succeed();
    }
}
