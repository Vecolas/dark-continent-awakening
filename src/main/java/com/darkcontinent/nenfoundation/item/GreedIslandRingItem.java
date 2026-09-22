package com.darkcontinent.nenfoundation.item;

import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandTravel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * O anel que abre Greed Island. AGACHAR + botao direito entra na ilha.
 *
 * <p><b>O CANONE MANDA AQUI, e por isso o anel e a porta.</b> Em Greed Island
 * todo jogador PRECISA de um anel para entrar -- ele nao e enfeite nem
 * chave-mestra opcional. Uma entrada por portal de blocos existiria em qualquer
 * mod de dimensao; o anel e o que faz a ilha ser <i>aquela</i> ilha.</p>
 *
 * <p><b>AGACHAR E OBRIGATORIO, e nao preciosismo de controle.</b> Sem o
 * agachamento, todo clique direito com o anel na mao viraria uma viagem: colocar
 * um bloco, abrir um bau ou bater numa porta com ele equipado tiraria o jogador
 * do mundo. O erro seria raro o bastante para ninguem reproduzir e frequente o
 * bastante para irritar.</p>
 *
 * <p><b>O SERVIDOR DECIDE.</b> O cliente so pede: quem le a dimensao, mede o
 * chao e move o jogador e {@link GreedIslandTravel}, no lado autoritativo. O
 * caminho de cliente existe apenas para nao engolir a mao e deixar o braco
 * animar.</p>
 *
 * <p><b>RECUSA TEM MOTIVO.</b> Os dois casos de "nao" -- ja estar na ilha e a
 * dimensao nao existir -- falam com o jogador por chave de traducao. Ativacao
 * que falha em silencio produz o pior relato de bug que existe.</p>
 */
public class GreedIslandRingItem extends Item {

    /**
     * Espera entre usos, em ticks.
     *
     * <p>LIMITE DE DESENHO, e nao botao de balanceamento -- por isso e constante
     * e nao config. Ele nao existe para encarecer a viagem: existe porque cada
     * uso carrega chunk de outra dimensao, e segurar o botao direito enfileiraria
     * dezenas de carregamentos que o servidor atende um por um. O sintoma nao
     * seria erro: seria o servidor travando por alguns segundos.</p>
     */
    private static final int ESPERA_EM_TICKS = 60;

    public GreedIslandRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jogador, InteractionHand mao) {
        ItemStack anel = jogador.getItemInHand(mao);
        if (!jogador.isShiftKeyDown()) {
            return InteractionResultHolder.pass(anel);
        }
        if (nivel.isClientSide()) {
            // O cliente nao decide nada; devolver SUCCESS so anima o braco.
            return InteractionResultHolder.success(anel);
        }
        if (!(jogador instanceof ServerPlayer servidor)) {
            return InteractionResultHolder.pass(anel);
        }

        if (GreedIslandTravel.naIlha(servidor)) {
            servidor.displayClientMessage(
                    Component.translatable("item.nenfoundation.greed_island_ring.ja_na_ilha"), true);
            return InteractionResultHolder.fail(anel);
        }
        if (!GreedIslandTravel.enviar(servidor)) {
            servidor.displayClientMessage(
                    Component.translatable("item.nenfoundation.greed_island_ring.sem_ilha"), true);
            return InteractionResultHolder.fail(anel);
        }

        // O SOM TOCA NO DESTINO, e nao na origem: quem ficou para tras nao ouve
        // nada, e quem viajou ouve a chegada. Tocado antes do cooldown porque o
        // cooldown nao pode ser a razao de o som sumir.
        servidor.level().playSound(null, servidor.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.2F);
        jogador.getCooldowns().addCooldown(this, ESPERA_EM_TICKS);
        return InteractionResultHolder.success(anel);
    }
}
