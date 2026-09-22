package com.darkcontinent.nenfoundation.network.payload;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.combat.FaixaDoCorpo;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * O servidor diz ONDE um golpe acertou, e com que forca (#103).
 *
 * <p>POR QUE ELE EXISTE. O ripple de impacto era desenhado no CORPO INTEIRO, e
 * nao na regiao atingida -- nao por escolha de arte, mas porque <b>o cliente
 * nao sabia onde a pancada caiu</b>. Ele inferia o golpe de uma queda de vida,
 * e a queda de vida nao tem endereco. O javadoc de {@code
 * AuraDistribution.comImpacto} registrava a espera por este payload:
 * <i>"{@code AuraImpactState.region()} continua existindo para o dia em que o
 * servidor souber dizer onde bateu"</i>.
 *
 * <p><b>O SERVIDOR JA SABIA.</b> {@code NenDanoService} chama {@code
 * faixaAtingida(jogador, fonte)} para calcular a reducao de dano, e descartava
 * a regiao logo depois. Este payload e o descarte virando informacao.
 *
 * <p><b>POR QUE A FORCA VEM DAQUI, e nao continua sendo inferida.</b> O
 * servidor tem o numero de dano REAL; o cliente tinha a diferenca entre duas
 * leituras de vida, que e o mesmo numero depois de passar por arredondamento,
 * absorcao e um tick de atraso. Manter as duas seria duas fontes para a mesma
 * verdade -- e a pior delas ganharia metade das vezes.
 *
 * <p><b>ELE CARREGA A FAIXA, E NAO UMA REGIAO.</b> {@code FaixaDoCorpo} tem
 * tres valores, e {@code PERNAS} cobre as DUAS pernas. Mandar uma regiao so
 * obrigaria o servidor a escolher uma perna -- inventar lateralidade que o
 * golpe nao tem. A faixa e exatamente o que ele sabe, nem mais nem menos.
 *
 * <p><b>ELE NAO CARREGA DANO, NEM VIDA.</b> {@code forca} ja chega normalizada
 * em {@code 0..1}: quanto o golpe vale em termos de leitura visual, e nao
 * quanto ele tirou. Um cliente que receba este payload nao aprende a vida de
 * ninguem, e e por isso que ele pode ser enviado a quem enxerga a entidade e
 * nao so a quem apanhou.
 */
public record ImpactoDeAuraS2C(int entidadeId, FaixaDoCorpo faixa, float forca)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ImpactoDeAuraS2C> TYPE =
            new CustomPacketPayload.Type<>(NenFoundation.id("aura_impact"));

    public static final StreamCodec<ByteBuf, ImpactoDeAuraS2C> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ImpactoDeAuraS2C::entidadeId,
                    ByteBufCodecs.BYTE, p -> (byte) p.faixa().ordinal(),
                    ByteBufCodecs.FLOAT, ImpactoDeAuraS2C::forca,
                    (id, ordinal, forca) -> new ImpactoDeAuraS2C(id, faixaDe(ordinal), forca));

    /**
     * A leitura e DEFENSIVA, como a de {@code PresencaDeAuraS2C}.
     *
     * <p>Um ordinal fora da faixa vem de um cliente de outra versao ou de um
     * pacote corrompido. Cair no TRONCO -- a regiao mais neutra -- desenha um
     * ripple no lugar aproximado; lancar derrubaria a conexao por causa de um
     * efeito visual, que e o oposto da regra "efeito visual nunca crasha".
     */
    private static FaixaDoCorpo faixaDe(byte ordinal) {
        FaixaDoCorpo[] valores = FaixaDoCorpo.values();
        if (ordinal < 0 || ordinal >= valores.length) {
            return FaixaDoCorpo.TRONCO;
        }
        return valores[ordinal];
    }

    public ImpactoDeAuraS2C {
        if (faixa == null) {
            faixa = FaixaDoCorpo.TRONCO;
        }
        if (!Float.isFinite(forca)) {
            forca = 0.0F;
        }
        forca = Math.max(0.0F, Math.min(1.0F, forca));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
