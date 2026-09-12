package com.darkcontinent.nenfoundation.network.payload;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.api.SinalDeAura;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * O que se percebe da aura de OUTRA pessoa.
 *
 * <p>E O PRIMEIRO PAYLOAD DESTE MOD QUE FALA DE TERCEIROS. Todos os outros vao
 * so para o dono do perfil; este vai para quem esta por perto, e por isso o
 * conteudo dele foi escolhido pelo que pode ser visto, e nao pelo que e
 * conveniente ter no cliente.
 *
 * <p>DOIS CAMPOS, E NENHUM A MAIS. O id da entidade e um sinal. Nao viaja aura
 * atual, nem maxima, nem output, nem categoria, nem lista de tecnicas -- nada
 * disso seria perceptivel por alguem de pe ao lado, e tudo isso seria util
 * demais para um cliente modificado.
 *
 * <p>QUEM ESTA EM ZETSU MANDA {@link SinalDeAura#NENHUM}, o mesmo byte de quem
 * nunca despertou. Nao ha bandeira de "escondido" para ler: o segredo nao
 * atravessa a rede, em vez de atravessar e pedir discricao ao cliente.
 *
 * <p>O ID E O DA ENTIDADE, e nao o UUID: e o que o cliente usa para achar quem
 * desenhar, e ele muda a cada sessao -- um UUID na rede seria um identificador
 * estavel de jogador viajando sem necessidade.
 */
public record PresencaDeAuraS2C(int entidadeId, SinalDeAura sinal)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PresencaDeAuraS2C> TYPE =
            new CustomPacketPayload.Type<>(NenFoundation.id("aura_presence"));

    /**
     * O sinal viaja como BYTE do ordinal, e a leitura e defensiva.
     *
     * <p>Um ordinal fora da faixa vira {@link SinalDeAura#seguro()} em vez de
     * lancar: pacote malformado no cliente derrubaria a conexao, e o padrao
     * seguro aqui e nao desenhar nada.
     */
    public static final StreamCodec<ByteBuf, PresencaDeAuraS2C> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PresencaDeAuraS2C::entidadeId,
                    ByteBufCodecs.BYTE, p -> (byte) p.sinal().ordinal(),
                    (id, ordinal) -> new PresencaDeAuraS2C(id, sinalDe(ordinal)));

    private static SinalDeAura sinalDe(byte ordinal) {
        SinalDeAura[] valores = SinalDeAura.values();
        if (ordinal < 0 || ordinal >= valores.length) {
            return SinalDeAura.seguro();
        }
        return valores[ordinal];
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
