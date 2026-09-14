package com.darkcontinent.nenfoundation.item;

import com.darkcontinent.nenfoundation.NenFoundation;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;

/**
 * Um card de Greed Island. O item e a PROVA de uma conversao, nunca a causa dela.
 *
 * <p><b>Ele nao converte nada e nao paga nada.</b> Quem decide se uma captura
 * vira card e o {@code CardConversionService}, no servidor, com a trava do
 * ledger. Um item que "se converte ao ser usado" seria o segundo caminho de
 * pagamento -- e dois caminhos para a mesma recompensa e exatamente a duplicata
 * que o sistema inteiro existe para impedir.</p>
 *
 * <p><b>A identidade do card mora em CustomData, e nao no id do item.</b> Um item
 * registrado por criatura daria vinte e tres registros para vinte e tres cards, e
 * cada card novo exigiria tocar no registro -- que e arquivo hostil a merge. Com
 * um item so, o card e um dado.</p>
 *
 * <p><b>Card sem criatura nao e erro, e generico.</b> Um stack que perdeu o dado
 * (comando, outro mod, save antigo) continua sendo um card; ele so nao diz de
 * quem. Estourar aqui derrubaria a tela de inventario de quem o tivesse.</p>
 */
public final class GreedIslandCardItem extends Item {

    /** Chave do dado dentro do CustomData. Vai para SAVE: mudar invalida cards existentes. */
    public static final String CHAVE_DA_CRIATURA = "monster_id";
    public static final String CHAVE_DO_RANK = "rank";

    public GreedIslandCardItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    /** Monta um card com a identidade gravada. */
    public static ItemStack de(ResourceLocation monsterId, String rank) {
        ItemStack stack = new ItemStack(
                com.darkcontinent.nenfoundation.registry.NenItems.GREED_ISLAND_CARD.get());
        CompoundTag dados = new CompoundTag();
        dados.putString(CHAVE_DA_CRIATURA, monsterId.toString());
        dados.putString(CHAVE_DO_RANK, rank);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(dados));
        return stack;
    }

    public static Optional<ResourceLocation> criaturaDe(ItemStack stack) {
        CustomData dados = stack.get(DataComponents.CUSTOM_DATA);
        if (dados == null) return Optional.empty();
        String texto = dados.copyTag().getString(CHAVE_DA_CRIATURA);
        return texto.isEmpty() ? Optional.empty() : Optional.ofNullable(ResourceLocation.tryParse(texto));
    }

    public static Optional<String> rankDe(ItemStack stack) {
        CustomData dados = stack.get(DataComponents.CUSTOM_DATA);
        if (dados == null) return Optional.empty();
        String rank = dados.copyTag().getString(CHAVE_DO_RANK);
        return rank.isEmpty() ? Optional.empty() : Optional.of(rank);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext contexto,
            List<Component> linhas, TooltipFlag bandeira) {
        // A traducao da criatura reusa a chave da ENTIDADE. Uma chave propria por
        // card seria uma segunda traducao para o mesmo nome, e as duas divergiriam
        // na primeira revisao de texto -- com o card chamando o bicho de um jeito e
        // o bestiario de outro.
        criaturaDe(stack).ifPresent(id -> linhas.add(Component.translatable(
                "entity." + id.getNamespace() + "." + id.getPath()).withStyle(ChatFormatting.GRAY)));
        rankDe(stack).ifPresent(rank -> linhas.add(Component.translatable(
                "item." + NenFoundation.MOD_ID + ".greed_island_card.rank", rank)
                .withStyle(ChatFormatting.DARK_AQUA)));
    }
}
