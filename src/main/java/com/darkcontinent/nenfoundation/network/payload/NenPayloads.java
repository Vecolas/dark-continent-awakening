package com.darkcontinent.nenfoundation.network.payload;

import com.darkcontinent.nenfoundation.network.Direcao;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * O catalogo dos payloads implementados.
 *
 * <p>POR QUE ELE EXISTE: {@code NenProtocol.TABELA} declara quais payloads o
 * protocolo tem e o que cada um pode carregar. Este catalogo declara quais
 * foram de fato escritos em Java. O portao {@code RecordsDePayloadTest} cruza
 * os dois e reprova se divergirem — payload na tabela sem record, record sem
 * entrada na tabela, ou componentes do record diferentes dos campos
 * declarados.
 *
 * <p>Sem esse cruzamento, a tabela vira documentacao: alguem acrescenta um
 * campo ao record e a declaracao de "o que este payload pode carregar" fica
 * para tras — que e exatamente como um campo proibido entra num payload C2S
 * sem ninguem notar.
 *
 * <p>O QUE AINDA NAO ESTA AQUI: o registro no {@code PayloadRegistrar}.
 * Registrar exige handler, e handler de payload C2S precisa do servico de
 * perfil (issue #2) enquanto o de S2C precisa do cache de cliente (issue #6).
 * Registrar agora com handler vazio seria a armadilha da secao 10 da
 * disciplina: codigo que nao roda hoje, e faz a coisa errada em silencio no
 * dia em que rodar.
 *
 * <p>ARQUIVO HOSTIL A MERGE: uma pessoa por vez. Acrescentar um payload e uma
 * linha aqui, uma na tabela e uma no documento.
 */
public final class NenPayloads {

    private NenPayloads() {
    }

    /**
     * Um payload implementado: o tipo, o codec e a direcao que ele declara.
     *
     * @param tipo    o {@link CustomPacketPayload.Type}, de onde sai o id
     * @param codec   o {@link StreamCodec} do payload
     * @param direcao para que lado ele viaja
     * @param classe  a classe do record, lida por reflexao pelo portao
     */
    public record Implementado(
            CustomPacketPayload.Type<? extends CustomPacketPayload> tipo,
            StreamCodec<ByteBuf, ? extends CustomPacketPayload> codec,
            Direcao direcao,
            Class<? extends CustomPacketPayload> classe) {
    }

    /** Todos os payloads escritos. O portao varre esta lista e a tabela. */
    public static final List<Implementado> TODOS = List.of(
            new Implementado(AtivarTecnicaC2S.TYPE, AtivarTecnicaC2S.STREAM_CODEC,
                    Direcao.C2S, AtivarTecnicaC2S.class),
            new Implementado(AjustarOutputC2S.TYPE, AjustarOutputC2S.STREAM_CODEC,
                    Direcao.C2S, AjustarOutputC2S.class),
            new Implementado(DesativarTecnicaC2S.TYPE, DesativarTecnicaC2S.STREAM_CODEC,
                    Direcao.C2S, DesativarTecnicaC2S.class),
            new Implementado(AtivarHabilidadeC2S.TYPE, AtivarHabilidadeC2S.STREAM_CODEC,
                    Direcao.C2S, AtivarHabilidadeC2S.class),
            new Implementado(SnapshotDePerfilS2C.TYPE, SnapshotDePerfilS2C.STREAM_CODEC,
                    Direcao.S2C, SnapshotDePerfilS2C.class),
            new Implementado(DeltaDeRuntimeS2C.TYPE, DeltaDeRuntimeS2C.STREAM_CODEC,
                    Direcao.S2C, DeltaDeRuntimeS2C.class),
            new Implementado(FxDeHabilidadeS2C.TYPE, FxDeHabilidadeS2C.STREAM_CODEC,
                    Direcao.S2C, FxDeHabilidadeS2C.class),
            new Implementado(FeedbackDeErroS2C.TYPE, FeedbackDeErroS2C.STREAM_CODEC,
                    Direcao.S2C, FeedbackDeErroS2C.class),
            new Implementado(PresencaDeAuraS2C.TYPE, PresencaDeAuraS2C.STREAM_CODEC,
                    Direcao.S2C, PresencaDeAuraS2C.class),
            new Implementado(EscolherFocoC2S.TYPE, EscolherFocoC2S.STREAM_CODEC,
                    Direcao.C2S, EscolherFocoC2S.class),
            new Implementado(ImpactoDeAuraS2C.TYPE, ImpactoDeAuraS2C.STREAM_CODEC,
                    Direcao.S2C, ImpactoDeAuraS2C.class),
            new Implementado(BestiarySnapshotS2C.TYPE, BestiarySnapshotS2C.STREAM_CODEC,
                    Direcao.S2C, BestiarySnapshotS2C.class));
}
