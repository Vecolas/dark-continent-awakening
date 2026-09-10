package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.server.NenProfileService;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * O primeiro gametest do projeto: o perfil de Nen contra um {@link ServerPlayer}
 * de verdade, num nivel de verdade.
 *
 * <p>POR QUE ELE EXISTE. Toda entrega ate agora terminou declarando a mesma
 * coisa: "nunca foi executado contra um jogador de verdade". A suite JUnit nao
 * consegue — ela roda sem o Minecraft carregado, e o attachment, o
 * ciclo de vida e o servico so existem com o jogo de pe.
 *
 * <p>E, ate este arquivo existir, {@code runGameTestServer} nem iniciava: ele
 * morria com {@code No test functions were given!} enquanto o Gradle imprimia
 * {@code BUILD SUCCESSFUL}. O primeiro gametest conserta a tarefa alem de
 * cobrir o codigo.
 *
 * <p>PONTO CEGO DECLARADO: {@code makeMockServerPlayerInLevel()} esta marcado
 * {@code @Deprecated(forRemoval = true)} no 1.21.1. Ele funciona hoje e some
 * numa versao futura do Minecraft. Quando sumir, estes testes param de compilar
 * — o que e o modo de falha BOM: barulhento, na hora de atualizar, e nao
 * silencioso em producao.
 *
 * <p>O template {@code nenfoundation:empty} e uma estrutura 3x3x3 sem bloco
 * nenhum. Estes testes nao mexem no mundo; eles precisam de um nivel, nao de um
 * cenario.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class NenPerfilGameTest {

    /**
     * A estrutura usada por todos os testes daqui.
     *
     * <p>Precisa ser explicita: sem {@code template}, o Minecraft usa o NOME DO
     * METODO em minusculas como nome de estrutura, e o servidor morre com
     * {@code Missing test structure: nenfoundation:<nome do metodo>}. O
     * {@code @PrefixGameTestTemplate(false)} so desliga o prefixo do nome da
     * classe; ele nao escolhe a estrutura.
     */
    private static final String TEMPLATE = "empty";

    private static ResourceLocation id(String caminho) {
        return ResourceLocation.fromNamespaceAndPath(NenFoundation.MOD_ID, caminho);
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new GameTestAssertException(mensagem);
        }
    }

    /**
     * Um jogador recem-colocado no mundo responde ao perfil NEUTRO.
     *
     * <p>Isto e o que o {@code PersistentNenDataTest} nao consegue afirmar: la o
     * neutro e um objeto construido no teste; aqui ele vem do attachment de um
     * jogador que o servidor acabou de criar.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void jogadorNovoNasceNeutro(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();

        PersistentNenData perfil = NenProfileService.ler(jogador);

        exigir(!perfil.awakened(),
                "Jogador recem-criado veio com awakened=true. Campo com default"
                        + " util faz todo dado mentir.");
        exigir(perfil.category() == NenCategory.UNDETERMINED,
                "Jogador recem-criado veio com categoria " + perfil.category()
                        + ". O ordinal zero precisa ser o neutro.");
        exigir(!perfil.categoryRevealed(), "categoryRevealed nasceu true.");
        exigir(perfil.unlockedTechniques().isEmpty(), "Tecnicas desbloqueadas no nascimento.");
        exigir(perfil.progressionFlags().isEmpty(), "Marcos no nascimento.");
        exigir(perfil.schemaVersion() == PersistentNenData.SCHEMA_ATUAL,
                "Perfil novo nasceu com schema " + perfil.schemaVersion()
                        + " em vez de " + PersistentNenData.SCHEMA_ATUAL + ".");

        helper.succeed();
    }

    /**
     * Uma mutacao pelo servico chega ao attachment e volta de la.
     *
     * <p>O teste de codec prova ida e volta em memoria. Este prova que o
     * NeoForge realmente guarda o record no jogador e devolve o mesmo.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void mutacaoPersisteNoAttachment(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();

        NenProfileService.atualizar(jogador, antes -> {
            Set<ResourceLocation> tecnicas = new LinkedHashSet<>(antes.unlockedTechniques());
            tecnicas.add(id("ten"));
            return new PersistentNenData(
                    antes.schemaVersion(), true, NenCategory.TRANSMUTATION, true,
                    42.5D, 0.75D, 1.25D,
                    antes.techniqueProficiency(), tecnicas,
                    antes.unlockedAbilities(), Set.of(id("despertou")));
        });

        // Le de novo, do zero, pelo mesmo caminho que o jogo usa.
        PersistentNenData depois = NenProfileService.ler(jogador);

        exigir(depois.awakened(), "awakened nao persistiu.");
        exigir(depois.category() == NenCategory.TRANSMUTATION,
                "categoria nao persistiu: " + depois.category());
        exigir(depois.categoryRevealed(), "categoryRevealed nao persistiu.");
        exigir(depois.auraPotential() == 42.5D,
                "auraPotential nao persistiu: " + depois.auraPotential());
        exigir(depois.unlockedTechniques().contains(id("ten")),
                "tecnica desbloqueada nao persistiu.");
        exigir(depois.temMarco(id("despertou")), "marco nao persistiu.");

        helper.succeed();
    }

    /**
     * A categoria fica escondida do jogador ate a revelacao — e o objeto que vem
     * do attachment respeita isso.
     *
     * <p>E a regra que o snapshot de rede depende: ele envia
     * {@code categoriaVisivel()}, e nao {@code category()}.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void categoriaFicaEscondidaAteRevelar(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();

        NenProfileService.atualizar(jogador, antes -> new PersistentNenData(
                antes.schemaVersion(), true, NenCategory.SPECIALIZATION, false,
                antes.auraPotential(), antes.control(), antes.output(),
                antes.techniqueProficiency(), antes.unlockedTechniques(),
                antes.unlockedAbilities(), antes.progressionFlags()));

        PersistentNenData perfil = NenProfileService.ler(jogador);

        exigir(perfil.category() == NenCategory.SPECIALIZATION,
                "A categoria real precisa existir internamente antes da revelacao.");
        exigir(perfil.categoriaVisivel() == NenCategory.UNDETERMINED,
                "A categoria VAZOU antes da revelacao: categoriaVisivel devolveu "
                        + perfil.categoriaVisivel() + ". O snapshot de rede usa esse"
                        + " metodo, entao um cliente modificado saberia a categoria"
                        + " antes da Water Divination.");

        helper.succeed();
    }

    /**
     * Dois jogadores nao compartilham perfil.
     *
     * <p>O vazamento de estado entre perfis e o defeito que nenhum teste
     * unitario alcanca: ele depende de haver dois attachments de verdade, em
     * duas entidades de verdade.
     */
    @GameTest(template = TEMPLATE)
    @PrefixGameTestTemplate(false)
    public static void perfisNaoSeMisturam(GameTestHelper helper) {
        ServerPlayer um = helper.makeMockServerPlayerInLevel();
        ServerPlayer outro = helper.makeMockServerPlayerInLevel();

        exigir(!um.getUUID().equals(outro.getUUID()),
                "Os dois jogadores mock sao o mesmo; o teste nao provaria nada.");

        NenProfileService.atualizar(um, antes -> new PersistentNenData(
                antes.schemaVersion(), true, NenCategory.EMISSION, true,
                99.0D, antes.control(), antes.output(),
                antes.techniqueProficiency(), Set.of(id("ren")),
                antes.unlockedAbilities(), antes.progressionFlags()));

        PersistentNenData doOutro = NenProfileService.ler(outro);

        exigir(!doOutro.awakened(),
                "O segundo jogador acordou sozinho. O perfil do primeiro vazou.");
        exigir(doOutro.category() == NenCategory.UNDETERMINED,
                "O segundo jogador herdou a categoria do primeiro: " + doOutro.category());
        exigir(doOutro.unlockedTechniques().isEmpty(),
                "O segundo jogador herdou as tecnicas do primeiro.");
        exigir(doOutro.auraPotential() == 0.0D,
                "O segundo jogador herdou o auraPotential do primeiro.");

        helper.succeed();
    }
}
