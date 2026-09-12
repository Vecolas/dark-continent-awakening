package com.darkcontinent.nenfoundation.network.payload;

import com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura;
import com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.OptionalDouble;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Os {@link StreamCodec} compartilhados pelos payloads.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. Cada codec e escrito a mao, campo por campo. Nada de serializacao
 * generica de objeto Java: o que trafega e exatamente o que esta declarado
 * aqui, e uma mudanca acidental na forma de um record vira erro de compilacao
 * em vez de bytes diferentes na rede. [NF-4]
 *
 * <p>2. {@link #CATEGORIA} viaja como STRING, e nao como ordinal. Dentro de uma
 * conexao os dois lados tem a mesma versao, entao o ordinal seria seguro hoje —
 * mas o mesmo habito aplicado a um dado persistido reescreve o significado de
 * todo save no dia em que alguem inserir um valor no meio do enum. Uma regra
 * so, em todo lugar, custa menos que duas regras e a lembranca de qual vale
 * onde.
 *
 * <p>3. {@link #OPCIONAL_INT} carrega um booleano de presenca em vez de usar um
 * valor sentinela. Id de entidade {@code 0} e valido, e {@code -1} como
 * "ausente" e a classe de bug em que um alvo legitimo vira "sem alvo" — em
 * silencio.
 */
public final class CodecsDePayload {

    /**
     * A alocacao de aura pelas seis regioes do corpo.
     *
     * <p>UM COMPONENTE SO, e nao seis. O {@code StreamCodec.composite} para no
     * sexto par, e o delta de runtime ja usa cinco -- seis floats soltos
     * estourariam o limite e obrigariam a partir o payload em dois. Empacotar
     * aqui gasta o ultimo slot com a alocacao inteira, que e o que ela e.
     *
     * <p>A ORDEM E A DO ENUM, e ela e contrato: reordenar {@code RegiaoDoCorpo}
     * trocaria braco por perna em todo cliente conectado, sem erro nenhum.
     *
     * <p>A LEITURA E DEFENSIVA. Um pacote malformado devolve a alocacao
     * uniforme em vez de lancar: excecao na thread de rede do cliente derruba a
     * conexao, e o padrao seguro aqui e "a aura esta espalhada", que e o estado
     * de repouso.
     */
    public static final StreamCodec<ByteBuf, AlocacaoDeAura> ALOCACAO =
            new StreamCodec<>() {
                @Override
                public AlocacaoDeAura decode(ByteBuf buffer) {
                    RegiaoDoCorpo[] regioes = RegiaoDoCorpo.values();
                    float[] fracoes = new float[regioes.length];
                    for (int i = 0; i < regioes.length; i++) {
                        fracoes[i] = buffer.readFloat();
                    }
                    return AlocacaoDeAura.deFracoes(fracoes)
                            .orElseGet(AlocacaoDeAura::uniforme);
                }

                @Override
                public void encode(ByteBuf buffer, AlocacaoDeAura alocacao) {
                    for (RegiaoDoCorpo regiao : RegiaoDoCorpo.values()) {
                        buffer.writeFloat(alocacao.em(regiao));
                    }
                }
            };

    private CodecsDePayload() {
    }

    /** Nome serializado -> categoria. Derivado do enum, nunca escrito a mao. */
    private static final Map<String, NenCategory> CATEGORIA_POR_NOME = criarIndice();

    private static Map<String, NenCategory> criarIndice() {
        Map<String, NenCategory> indice = new LinkedHashMap<>();
        Arrays.stream(NenCategory.values())
                .forEach(c -> indice.put(c.getSerializedName(), c));
        return Map.copyOf(indice);
    }

    /**
     * Categoria pelo nome serializado.
     *
     * <p>Nome desconhecido lanca. Cair para {@code UNDETERMINED} seria pior:
     * um cliente passaria a mostrar "categoria indefinida" para um jogador que
     * tem categoria, sem nada no log dizendo que o protocolo divergiu.
     */
    private static NenCategory categoriaPorNome(String nome) {
        NenCategory c = CATEGORIA_POR_NOME.get(nome);
        if (c == null) {
            throw new IllegalArgumentException(
                    "Categoria desconhecida no protocolo: '" + nome + "'."
                            + " Cliente e servidor discordam sobre NenCategory.");
        }
        return c;
    }

    public static final StreamCodec<ByteBuf, NenCategory> CATEGORIA =
            ByteBufCodecs.STRING_UTF8.map(
                    CodecsDePayload::categoriaPorNome,
                    NenCategory::getSerializedName);

    /** Conjunto de ids. Viaja como lista; vira {@link Set} imutavel na chegada. */
    public static final StreamCodec<ByteBuf, Set<ResourceLocation>> CONJUNTO_DE_IDS =
            ResourceLocation.STREAM_CODEC
                    .apply(ByteBufCodecs.list())
                    .map(Set::copyOf, List::copyOf);

    /** Mapa de id para ticks. Usado por cooldowns. */
    public static final StreamCodec<ByteBuf, Map<ResourceLocation, Integer>> ID_PARA_TICKS =
            ByteBufCodecs.map(LinkedHashMap::new, ResourceLocation.STREAM_CODEC,
                    ByteBufCodecs.VAR_INT);

    /** Posicao no mundo. Tres doubles, sem compressao. */
    public static final StreamCodec<ByteBuf, Vec3> VEC3 = new StreamCodec<>() {
        @Override
        public Vec3 decode(ByteBuf buf) {
            return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        @Override
        public void encode(ByteBuf buf, Vec3 v) {
            buf.writeDouble(v.x);
            buf.writeDouble(v.y);
            buf.writeDouble(v.z);
        }
    };

    /** Posicao opcional. */
    public static final StreamCodec<ByteBuf, java.util.Optional<Vec3>> VEC3_OPCIONAL =
            ByteBufCodecs.optional(VEC3);

    /** Inteiro opcional, com booleano de presenca. Ver a decisao 3 acima. */
    public static final StreamCodec<ByteBuf, OptionalInt> OPCIONAL_INT = new StreamCodec<>() {
        @Override
        public OptionalInt decode(ByteBuf buf) {
            return buf.readBoolean()
                    ? OptionalInt.of(ByteBufCodecs.VAR_INT.decode(buf))
                    : OptionalInt.empty();
        }

        @Override
        public void encode(ByteBuf buf, OptionalInt valor) {
            buf.writeBoolean(valor.isPresent());
            if (valor.isPresent()) {
                ByteBufCodecs.VAR_INT.encode(buf, valor.getAsInt());
            }
        }
    };

    /**
     * Double opcional, com booleano de presenca.
     *
     * <p>Usado pelo delta de runtime para transportar vigor e vigorMaximo
     * somente quando a segunda barra esta habilitada no servidor. Um sentinela
     * como -1f seria ambiguo se um dia o vigor admitir valores fracionarios
     * ou negativos por efeito de status. O booleano elimina a ambiguidade.
     */
    public static final StreamCodec<ByteBuf, OptionalDouble> OPCIONAL_DOUBLE = new StreamCodec<>() {
        @Override
        public OptionalDouble decode(ByteBuf buf) {
            return buf.readBoolean()
                    ? OptionalDouble.of(buf.readDouble())
                    : OptionalDouble.empty();
        }

        @Override
        public void encode(ByteBuf buf, OptionalDouble valor) {
            buf.writeBoolean(valor.isPresent());
            if (valor.isPresent()) {
                buf.writeDouble(valor.getAsDouble());
            }
        }
    };
}
