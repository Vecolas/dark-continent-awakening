package com.darkcontinent.nenfoundation.network.payload;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.api.SinalDeAura;
import com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura;
import com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo;
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
 * <p>TRES CAMPOS, E NENHUM A MAIS. Id da entidade, sinal e a FORMA da aura.
 * Nao viaja aura atual, nem maxima, nem output, nem categoria, nem lista de
 * tecnicas -- nada disso seria perceptivel por alguem de pe ao lado, e tudo
 * isso seria util demais para um cliente modificado.
 *
 * <p><b>A FORMA ENTROU EM 2026-09-26, e ela corrige um defeito visto em
 * jogo.</b> Gyo, Ko e Shu concentram a aura num ponto. Para o proprio jogador
 * isso funcionava -- a alocacao viaja no {@code nen_runtime_delta}, que so vai
 * para o dono. Para quem OLHAVA, {@code EstadoVisualDeTerceiro} chumbava
 * {@code AuraDistribution.uniforme()} e o corpo inteiro acendia: um Gyo lido
 * como Ken, um Ko lido como Ren de corpo cheio.
 *
 * <p>O javadoc de {@code PresencaDeAura} afirmava, ate ontem, que "quem olha
 * percebe que ha aura, e nao ONDE ela esta". <b>Isso estava errado, e o cânone
 * e quem diz:</b> Gyo nos olhos existe para ver aura ESCONDIDA (In) e aura
 * sutil -- nao para ver concentracao grossa. Aura visivel tem forma visivel, e
 * um punho carregado de Ko e a imagem mais reconhecivel da obra.
 *
 * <p>ELA VIAJA QUANTIZADA, um byte por regiao. O consumidor normaliza pela
 * regiao mais concentrada antes de desenhar, entao o que importa e a PROPORCAO
 * -- e 1/255 de resolucao e mais do que qualquer olho separa numa shell de
 * aura. Seis floats dariam 24 bytes para a mesma imagem.
 *
 * <p>QUEM ESTA EM ZETSU MANDA {@link SinalDeAura#NENHUM}, o mesmo byte de quem
 * nunca despertou. Nao ha bandeira de "escondido" para ler: o segredo nao
 * atravessa a rede, em vez de atravessar e pedir discricao ao cliente.
 *
 * <p>O ID E O DA ENTIDADE, e nao o UUID: e o que o cliente usa para achar quem
 * desenhar, e ele muda a cada sessao -- um UUID na rede seria um identificador
 * estavel de jogador viajando sem necessidade.
 */
public record PresencaDeAuraS2C(int entidadeId, SinalDeAura sinal,
        AlocacaoDeAura alocacao) implements CustomPacketPayload {

    /**
     * Compacto valido para quem nao tem forma a declarar.
     *
     * <p>Existe para o caminho de Zetsu e de quem nunca despertou: o sinal ja e
     * {@code NENHUM}, e mandar {@code null} obrigaria todo leitor a conferir.
     */
    public PresencaDeAuraS2C(int entidadeId, SinalDeAura sinal) {
        this(entidadeId, sinal, AlocacaoDeAura.uniforme());
    }

    public PresencaDeAuraS2C {
        if (sinal == null || alocacao == null) {
            throw new IllegalArgumentException("presenca incompleta");
        }
    }

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
                    ByteBufCodecs.BYTE_ARRAY, PresencaDeAuraS2C::formaEmBytes,
                    (id, ordinal, forma) ->
                            new PresencaDeAuraS2C(id, sinalDe(ordinal), alocacaoDe(forma)));

    /**
     * A forma, um byte por regiao, na ORDEM DO ENUM.
     *
     * <p>A ordem do enum e o contrato do fio. {@code RegiaoDoCorpo} nao esta
     * congelado pelo ADR-004 -- so as categorias estao --, entao reordenar o
     * enum reescreveria o significado de cada byte sem erro nenhum. Se um dia
     * alguem precisar reordena-lo, a versao do protocolo sobe junto.
     */
    private static byte[] formaEmBytes(PresencaDeAuraS2C p) {
        RegiaoDoCorpo[] regioes = RegiaoDoCorpo.values();
        byte[] saida = new byte[regioes.length];
        for (int i = 0; i < regioes.length; i++) {
            float fracao = Math.clamp(p.alocacao().em(regioes[i]), 0.0F, 1.0F);
            saida[i] = (byte) Math.round(fracao * 255.0F);
        }
        return saida;
    }

    /**
     * A volta, defensiva como a do sinal.
     *
     * <p>TAMANHO ERRADO VIRA UNIFORME, e nao excecao: pacote malformado no
     * cliente derrubaria a conexao, e o padrao seguro aqui e desenhar a aura
     * sem concentracao -- que e o que o jogo fazia antes deste campo existir.
     *
     * <p><b>ELA RENORMALIZA, e o portao mostrou que precisa.</b> A primeira
     * versao entregava os bytes crus a {@code AlocacaoDeAura.deFracoes}, que
     * exige soma 1 -- e a quantizacao nao devolve soma 1: com 0.95 num braco os
     * bytes somam 1.008. O resultado era {@code Optional.empty()} em TODO
     * pacote com concentracao, silenciosamente trocado por uniforme. O defeito
     * relatado continuaria igual, agora com codigo novo por cima.
     *
     * <p>ISTO NAO CONTRADIZ o "RECUSA, E NAO NORMALIZA" de {@code deFracoes}.
     * Aquela regra protege contra EMISSOR quebrado, e ela continua valendo: o
     * que este metodo desfaz e uma perda que ele mesmo causou, um passo antes,
     * e cujo tamanho exato ele conhece. Normalizar o que voce quantizou e o
     * inverso da sua propria conta; normalizar o que chegou torto de outro
     * lugar e esconder um defeito.
     *
     * <p>SOMA ZERO CAI EM UNIFORME: nao ha proporcao a recuperar de seis zeros,
     * e dividir por zero produziria NaN em toda regiao.
     */
    private static AlocacaoDeAura alocacaoDe(byte[] forma) {
        RegiaoDoCorpo[] regioes = RegiaoDoCorpo.values();
        if (forma == null || forma.length != regioes.length) {
            return AlocacaoDeAura.uniforme();
        }
        int total = 0;
        for (byte b : forma) {
            total += b & 0xFF;
        }
        if (total <= 0) {
            return AlocacaoDeAura.uniforme();
        }
        float[] fracoes = new float[regioes.length];
        for (int i = 0; i < regioes.length; i++) {
            fracoes[i] = (forma[i] & 0xFF) / (float) total;
        }
        return AlocacaoDeAura.deFracoes(fracoes).orElseGet(AlocacaoDeAura::uniforme);
    }

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
