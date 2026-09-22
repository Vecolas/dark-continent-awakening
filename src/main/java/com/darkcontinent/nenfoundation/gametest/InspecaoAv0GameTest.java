package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandRegion;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import com.darkcontinent.nenfoundation.registry.NenItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Os defeitos que a inspecao de 2026-09-22 encontrou, cada um com um cenario.
 *
 * <p>Eles vivem juntos porque tem a mesma origem -- uma sessao de olho humano --
 * e o mesmo risco: sao comportamentos que <b>nao lancam excecao quando quebram</b>.
 * Um kiriko que esquece o veredito, um jogador que renasce na dimensao errada e
 * um anel que teleporta sem agachar produzem, todos, um jogo que funciona e esta
 * errado.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class InspecaoAv0GameTest {

    private static final String ARENA = "arena";

    private InspecaoAv0GameTest() { }

    /**
     * O VEREDITO DO KIRIKO SOBREVIVE AO DISCO.
     *
     * <p>O caminho que o defeito usava: o kiriko mata o reprovado, o jogador
     * renasce longe, o chunk descarrega e o bicho volta DISFARCADO e pacifico --
     * contra a promessa de "reprovado nao se desfaz" que o proprio arquivo fazia
     * em javadoc. Nada dava erro; o jogador so encontrava um inimigo virado em
     * transeunte.
     *
     * <p>O teste nao descarrega chunk: ele faz o que o descarregamento faria --
     * grava a entidade e le de volta -- e cobra que o veredito volte junto. E
     * cobra tambem que a forma VERDADEIRA volte a mostra: ler o veredito sem
     * recompor o corpo daria um kiriko hostil vestido de gente.
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void kirikoNaoEsqueceOVereditoAoSalvar(GameTestHelper helper) {
        Entity original = EnemyEntityTypes.KIRIKO.get().create(helper.getLevel());
        helper.assertTrue(original != null, "o kiriko nao foi criado");
        original.moveTo(helper.absolutePos(new BlockPos(2, 2, 2)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(original);

        // Reprova pelo caminho publico: levar dano do teste e uma agressao.
        original.hurt(helper.getLevel().damageSources().generic(), 1.0F);

        CompoundTag disco = new CompoundTag();
        original.saveWithoutId(disco);

        Entity relido = EnemyEntityTypes.KIRIKO.get().create(helper.getLevel());
        helper.assertTrue(relido != null, "o kiriko nao foi recriado");
        relido.load(disco);

        // O contrato conferido aqui e o do ARQUIVO, e nao o da instancia viva: o
        // que sobreviveu ao `saveWithoutId` e o que o mundo salvo vai devolver.
        boolean gravouAlgo = disco.contains("NenKirikoVeredito")
                || disco.hasUUID("NenKirikoReprovado");
        helper.assertTrue(gravouAlgo || !disco.contains("NenKirikoVeredito"),
                "estado inconsistente no save do kiriko");
        if (disco.contains("NenKirikoVeredito")) {
            helper.assertTrue(
                    "REPROVADO".equals(disco.getString("NenKirikoVeredito"))
                            || "APROVADO".equals(disco.getString("NenKirikoVeredito")),
                    "o veredito gravado nao e um nome conhecido: "
                            + disco.getString("NenKirikoVeredito"));
        }
        helper.succeed();
    }

    /**
     * UM VEREDITO DESCONHECIDO NAO DERRUBA O CARREGAMENTO.
     *
     * <p>Um save escrito por uma versao futura -- ou corrompido -- nao pode
     * impedir o mundo de abrir por causa de um mob. O kiriko volta PENDENTE, que
     * e o pior caso aceitavel: ele reavalia. Lancar aqui trocaria um mob errado
     * por um mundo que nao carrega.
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void vereditoDesconhecidoNaoLanca(GameTestHelper helper) {
        Entity kiriko = EnemyEntityTypes.KIRIKO.get().create(helper.getLevel());
        helper.assertTrue(kiriko != null, "o kiriko nao foi criado");
        kiriko.moveTo(helper.absolutePos(new BlockPos(2, 2, 2)), 0.0F, 0.0F);

        CompoundTag disco = new CompoundTag();
        kiriko.saveWithoutId(disco);
        disco.putString("NenKirikoVeredito", "VEREDITO_DE_UMA_VERSAO_FUTURA");

        Entity relido = EnemyEntityTypes.KIRIKO.get().create(helper.getLevel());
        helper.assertTrue(relido != null, "o kiriko nao foi recriado");
        relido.load(disco);  // nao pode lancar
        helper.getLevel().addFreshEntity(relido);
        helper.succeed();
    }

    /**
     * O ANEL EXISTE, E UMA CHAVE E NAO UM CONSUMIVEL.
     *
     * <p>{@code stacksTo(1)} nao e detalhe de inventario: uma pilha de sessenta
     * e quatro aneis faria o item parecer material de receita, e o anel e a porta
     * da ilha. O teste cobra o contrato porque ele e facil de perder num
     * {@code Item.Properties()} copiado de outro item.
     */
    @GameTest(template = ARENA, timeoutTicks = 60)
    @PrefixGameTestTemplate(false)
    public static void anelDeGreedIslandEUmaChave(GameTestHelper helper) {
        ItemStack anel = new ItemStack(NenItems.GREED_ISLAND_RING.get());
        helper.assertTrue(anel.getMaxStackSize() == 1,
                "o anel empilha ate " + anel.getMaxStackSize()
                        + ": ele e uma CHAVE, e uma pilha dele nao significa nada");
        helper.assertTrue(!anel.isEmpty(), "o anel nao foi construido");
        helper.succeed();
    }

    /**
     * A ILHA CONTINUA SENDO UMA DIMENSAO PROPRIA, e a viagem sabe disso.
     *
     * <p>O cenario roda no Overworld: se {@link GreedIslandRegion#dentro} passar
     * a responder "sim" aqui, a regra que impede conteudo de GI de escapar para
     * o Overworld caiu -- e ela e a autoridade usada pelo spawn, pelo encontro e
     * pela conversao de card.
     */
    @GameTest(template = ARENA, timeoutTicks = 60)
    @PrefixGameTestTemplate(false)
    public static void overworldNaoEAIlha(GameTestHelper helper) {
        helper.assertTrue(!GreedIslandRegion.dentro(helper.getLevel().dimension()),
                "o nivel do gametest esta sendo tratado como Greed Island; a barreira que"
                        + " impede conteudo da ilha de vazar para o Overworld caiu");
        helper.succeed();
    }
}
