package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.divination.RecusaDaAdivinhacao;
import com.darkcontinent.nenfoundation.nen.divination.ResultadoDaAdivinhacao;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.technique.Ren;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * O teste da Water Divination: agua, uma folha, e a categoria aparece.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. ELE NAO ESCREVE PERFIL. Nem {@code category}, nem
 * {@code categoryRevealed}, nem marco. Ele chama
 * {@link NenCategoryService#atribuirPorSorteio} e
 * {@link NenCategoryService#revelar}, e le o resultado. Um ritual que ligasse o
 * booleano direto seria uma SEGUNDA autoridade sobre a revelacao, e as duas
 * divergiriam na primeira regra nova -- sem erro nenhum.
 *
 * <p>2. O TESTE DESCOBRE, NAO CRIA. A categoria ja estava determinada pelo par
 * (mundo, jogador) antes de alguem encostar num caldeirao: o sorteio e
 * deterministico. Por isso "atribuir e revelar em seguida" nao e um atalho --
 * e literalmente descobrir um fato que ja existia. E por isso refazer o teste
 * nunca muda nada.
 *
 * <p>3. RECUSA COM MOTIVO, sempre. Sem Nen desperto a agua nao reage E o
 * jogador e informado do porque. Um ritual que falha em silencio produz o pior
 * relato de bug que existe: "cliquei e nao aconteceu nada".
 *
 * <p>4. A MONTAGEM E DE BLOCO VANILLA, e nao de um bloco proprio: um caldeirao
 * com agua, e uma folha na mao. E uma decisao de escopo declarada -- um
 * "copo" autoral exige arte, e arte fraca entrando no repositorio e pior que
 * arte que ainda nao existe. Ver o PR. A regra de montagem esta isolada em
 * {@link #montagemValida} justamente para poder ser trocada sem tocar no
 * resto.
 *
 * <p>5. O TESTE EXIGE REN, como o cânone pede. Isto era divida declarada: o PR
 * #77 entregou o ritual com o portao em "ter Nen desperto" porque Ren nao
 * existia, e disse isso em voz alta aqui e num teste que REPROVAVA quando a
 * condicao mudasse. Ren chegou na #87, o teste reprovou, e a divida foi paga
 * na #90.
 *
 * <p>A condicao mora inteira em {@link #recusaPara}, e foi por isso que ela
 * nasceu isolada: acrescentar Ren nao virou cirurgia espalhada pelo ritual.
 *
 * <p>E as duas recusas sao DISTINGUIVEIS. "Nao despertou" e "despertou mas
 * esta sem Ren" mandam o jogador para lugares diferentes; uma mensagem so
 * daria o mesmo relato de bug para dois problemas que nao tem nada a ver.
 */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class NenAguaDivinatoria {

    private NenAguaDivinatoria() {
    }

    @SubscribeEvent
    public static void aoClicarNoBloco(PlayerInteractEvent.RightClickBlock evento) {
        // O EVENTO RODA NOS DOIS LADOS. Sair cedo no cliente nao e otimizacao:
        // o cliente nao tem o perfil autoritativo, e deixa-lo executar produz
        // uma previsao que discorda do servidor -- a agua reage no cliente e
        // nao no servidor, ou o contrario.
        if (evento.getLevel().isClientSide()
                || !(evento.getEntity() instanceof ServerPlayer jogador)) {
            return;
        }

        ServerLevel nivel = jogador.serverLevel();
        BlockPos posicao = evento.getPos();
        if (!montagemValida(nivel.getBlockState(posicao), evento.getItemStack())) {
            return;
        }

        // A partir daqui o clique E uma tentativa de teste. Cancelar impede
        // que a folha seja COLOCADA como bloco em cima do caldeirao, que e o
        // que o vanilla faria -- e o jogador perderia a folha sem entender.
        evento.setCanceled(true);
        evento.setCancellationResult(InteractionResult.SUCCESS);

        fazerOTeste(jogador, nivel, posicao);
    }

    /**
     * A montagem canonica, em blocos que ja existem: agua e uma folha.
     *
     * <p>PUBLICO de proposito: e uma pergunta, nao uma mutacao, e o gametest
     * vive noutro pacote. Deixa-la fechada obrigaria a testar a montagem so
     * pelo efeito colateral do clique -- e ai um "nao aconteceu nada" nao
     * distinguiria montagem recusada de ritual quebrado.
     *
     * <p>O nivel do caldeirao precisa ser maior que zero. Um caldeirao com
     * agua "pela metade" continua valendo: o cânone fala de um copo com agua, e
     * nao de uma medida.
     */
    public static boolean montagemValida(BlockState estado, ItemStack naMao) {
        if (!estado.is(Blocks.WATER_CAULDRON)) {
            return false;
        }
        if (estado.getValue(LayeredCauldronBlock.LEVEL) <= 0) {
            return false;
        }
        return naMao.is(ItemTags.LEAVES);
    }

    /**
     * Por que o ritual foi recusado -- ou vazio, quando ele pode acontecer.
     *
     * <p>DEVOLVE O MOTIVO, e nao um booleano, porque recusa sempre tem motivo.
     * Com um `false` o chamador teria de re-deduzir o porque, e a deducao ficaria
     * em dois lugares: aqui e la. Duas fontes para a mesma verdade divergem no
     * dia em que uma terceira condicao entrar.
     *
     * <p>A ORDEM DAS PERGUNTAS IMPORTA. Quem nao despertou tambem nao tem Ren,
     * e dizer a essa pessoa "ative Ren" e mandar ela para o lugar errado.
     */
    static Optional<RecusaDaAdivinhacao> recusaPara(PersistentNenData perfil,
            Set<ResourceLocation> tecnicasAtivas) {
        if (!perfil.awakened()) {
            return Optional.of(RecusaDaAdivinhacao.NAO_DESPERTOU);
        }
        if (!tecnicasAtivas.contains(Ren.ID)) {
            return Optional.of(RecusaDaAdivinhacao.SEM_REN);
        }
        return Optional.empty();
    }


    private static void fazerOTeste(ServerPlayer jogador, ServerLevel nivel, BlockPos posicao) {
        Optional<RecusaDaAdivinhacao> recusa = recusaPara(NenProfileService.ler(jogador),
                NenRuntimeService.estadoDe(jogador).tecnicasAtivas());
        if (recusa.isPresent()) {
            // Recusa com motivo. Sem isto o relato de bug e "cliquei e nao
            // aconteceu nada", que nao diz onde procurar.
            jogador.sendSystemMessage(Component.translatable(recusa.get().chaveDaRegra()));
            jogador.sendSystemMessage(Component.translatable(recusa.get().chaveDaAgua()));
            return;
        }

        // A ORDEM E CONTRATO, e as duas chamadas passam pelos servicos.
        // `atribuirPorSorteio` e no-op para quem ja tem categoria, e e isso
        // que faz refazer o teste nao re-sortear nada.
        NenCategoryService.atribuirPorSorteio(jogador);
        NenCategoryService.Revelacao revelacao = NenCategoryService.revelar(jogador);

        NenCategory categoria = NenProfileService.ler(jogador).category();
        Optional<ResultadoDaAdivinhacao> resultado = ResultadoDaAdivinhacao.para(categoria);

        if (resultado.isEmpty()) {
            // Nao deveria acontecer: o sorteio nunca devolve o neutro, e a
            // tabela cobre as seis. Se acontecer, e um estado que ninguem
            // desenhou -- e o jogador precisa saber disso em vez de olhar
            // para uma agua parada.
            jogador.sendSystemMessage(
                    Component.translatable("nenfoundation.error.sem_categoria"));
            return;
        }

        mostrar(jogador, nivel, posicao, resultado.orElseThrow(),
                revelacao == NenCategoryService.Revelacao.REVELOU);
    }

    /**
     * A reacao da agua: particula, som e frase.
     *
     * <p>TRES SINAIS, e nao so a frase. O teste canonico e VISUAL, e quem
     * estiver com o chat fechado nao pode ficar sem resposta nenhuma.
     */
    private static void mostrar(ServerPlayer jogador, ServerLevel nivel, BlockPos posicao,
            ResultadoDaAdivinhacao resultado, boolean primeiraVez) {

        double x = posicao.getX() + 0.5D;
        double y = posicao.getY() + 0.9D;
        double z = posicao.getZ() + 0.5D;

        nivel.sendParticles(resultado.particula(), x, y, z, 24, 0.2D, 0.1D, 0.2D, 0.02D);
        nivel.playSound(null, posicao, resultado.som(), SoundSource.BLOCKS, 0.8F, 1.0F);

        // A frase da DESCOBERTA sai uma vez. Repetir o teste da a frase de
        // repeticao: dizer "voce descobre que..." pela segunda vez sugere que
        // algo mudou, e nada mudou nem pode mudar.
        jogador.sendSystemMessage(Component.translatable(
                primeiraVez ? resultado.chaveDeTraducao() : resultado.chaveDeRepeticao(),
                Component.translatable(resultado.categoria().chaveDeTraducao())));
    }
}
