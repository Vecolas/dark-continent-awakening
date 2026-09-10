package com.darkcontinent.nenfoundation.network.payload;

import com.darkcontinent.nenfoundation.NenFoundation;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * O jogador pediu para ativar uma tecnica.
 *
 * <p>DECISAO: este record carrega o id da tecnica e MAIS NADA.
 *
 * <p>A tabela congelada do protocolo listava tambem um "contextoDeInput". Ele
 * saiu antes da primeira implementacao, porque nenhum consumidor foi
 * encontrado: Ten e Ren sao alternaveis e ja tem um payload proprio de
 * desligamento, e a reativacao repetida de tecla segurada e resolvida por
 * idempotencia no servidor, nao por um sinalizador na rede.
 *
 * <p>Campo que ninguem le e um botao morto: ele nao da erro, e convida a
 * proxima pessoa a preenche-lo com algo errado. Se o M4 revelar que
 * segurar-versus-tocar importa, o campo volta COM o consumidor, o
 * {@code NenProtocol.VERSION} sobe e o handshake recusa cliente antigo. Hoje
 * isso custa zero: o protocolo nunca esteve na rede.
 *
 * <p>O servidor NAO confia em nada disto alem do id. Existencia, unlock,
 * estado, aura e rate limit sao conferidos la.
 */
public record AtivarTecnicaC2S(ResourceLocation tecnicaId)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AtivarTecnicaC2S> TYPE =
            new CustomPacketPayload.Type<>(NenFoundation.id("activate_technique_request"));

    public static final StreamCodec<ByteBuf, AtivarTecnicaC2S> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, AtivarTecnicaC2S::tecnicaId,
                    AtivarTecnicaC2S::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
