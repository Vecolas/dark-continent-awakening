package com.darkcontinent.nenfoundation.network.payload;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.api.ability.AbilityRequest;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * O jogador pediu para usar uma habilidade.
 *
 * <p>DECISAO, e e a mais importante de seguranca do mod: o alvo viaja como
 * ID DE REDE, nunca como entidade, e o nome do campo diz CANDIDATO para que
 * nenhum handler o trate como resolvido.
 *
 * <p>O servidor resolve o id no proprio mundo e reconfere existencia,
 * dimensao, distancia contra o {@code AbilitySpec} e linha de visao quando a
 * habilidade exige. Aceitar o que o cliente apontou e aceitar dano a qualquer
 * coisa em qualquer lugar — e com um cliente honesto isso funciona
 * perfeitamente, que e por que nenhum playtest encontra.
 *
 * <p>Pelo mesmo motivo nao ha aqui campo de custo, dano, cooldown ou
 * multiplicador. Se um dia parecer que precisa, a resposta e nao.
 */
public record AtivarHabilidadeC2S(ResourceLocation habilidadeId,
        int slot,
        OptionalInt alvoIdCandidato,
        Optional<Vec3> posicaoCandidata)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AtivarHabilidadeC2S> TYPE =
            new CustomPacketPayload.Type<>(NenFoundation.id("activate_ability_request"));

    public static final StreamCodec<ByteBuf, AtivarHabilidadeC2S> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, AtivarHabilidadeC2S::habilidadeId,
                    net.minecraft.network.codec.ByteBufCodecs.VAR_INT, AtivarHabilidadeC2S::slot,
                    CodecsDePayload.OPCIONAL_INT, AtivarHabilidadeC2S::alvoIdCandidato,
                    CodecsDePayload.VEC3_OPCIONAL, AtivarHabilidadeC2S::posicaoCandidata,
                    AtivarHabilidadeC2S::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Converte para o pedido que a API de habilidade consome.
     *
     * <p>O nome muda de "candidato" para o do contrato porque a conversao
     * acontece no servidor, depois do rate limit e antes da validacao. Nada
     * aqui resolve o alvo: {@link AbilityRequest} tambem carrega apenas o id.
     */
    public AbilityRequest paraPedido() {
        return new AbilityRequest(habilidadeId, slot, alvoIdCandidato, posicaoCandidata);
    }
}
