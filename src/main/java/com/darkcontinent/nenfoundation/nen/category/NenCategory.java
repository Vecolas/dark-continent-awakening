package com.darkcontinent.nenfoundation.nen.category;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * As categorias de Nen.
 *
 * <p>CONTRATO CONGELADO (plano tecnico, secao 22; ADR-004). Estes nomes vao
 * para NBT de save, para arquivos de afinidade em datapack, para chaves de
 * traducao e para condicoes de quest. Renomear um deles depois do primeiro
 * mundo criado nao produz crash: produz um jogador que perde a categoria em
 * silencio.
 *
 * <p>DUAS DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. {@link #UNDETERMINED} e o valor de ordinal ZERO de proposito. Quem
 * esquecer de atribuir categoria recebe o NEUTRO, nunca Enhancement. Afirmar a
 * categoria errada e pior que nao afirmar nenhuma — um jogador que nunca
 * despertou nao pode aparecer como Enhancement em nenhuma tela, nenhum log e
 * nenhuma quest.
 *
 * <p>2. A serializacao e por STRING, nunca por ordinal. Enum salvo como inteiro
 * reescreve o significado de todo dado ja gravado quando alguem insere um valor
 * no meio da lista. Por string, inserir no meio e inofensivo.
 */
public enum NenCategory implements StringRepresentable {

    /** Neutro. Jogador sem Nen, ou com Nen despertado e categoria ainda nao sorteada. */
    UNDETERMINED("undetermined"),

    ENHANCEMENT("enhancement"),
    TRANSMUTATION("transmutation"),
    EMISSION("emission"),
    CONJURATION("conjuration"),
    MANIPULATION("manipulation"),
    SPECIALIZATION("specialization");

    /**
     * As seis categorias reais, na ordem do diagrama classico de adjacencia.
     * Derivada do enum, nunca escrita a mao: uma lista literal aqui divergiria
     * do enum no dia em que alguem acrescentasse um valor.
     */
    public static final List<NenCategory> REAIS =
            Arrays.stream(values()).filter(NenCategory::eReal).toList();

    public static final Codec<NenCategory> CODEC =
            StringRepresentable.fromEnum(NenCategory::values);

    private final String nome;
    private final ResourceLocation id;

    NenCategory(String nome) {
        this.nome = nome;
        this.id = NenFoundation.id("category/" + nome);
    }

    @Override
    public String getSerializedName() {
        return this.nome;
    }

    /** Identificador estavel para datapacks, quests e API. */
    public ResourceLocation id() {
        return this.id;
    }

    /** Chave de traducao. O texto exibido nunca sai do codigo. */
    public String chaveDeTraducao() {
        return "nenfoundation.category." + this.nome;
    }

    /** {@code false} apenas para {@link #UNDETERMINED}. */
    public boolean eReal() {
        return this != UNDETERMINED;
    }
}
