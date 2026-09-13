package com.darkcontinent.nenfoundation.enemy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/** Codecs compartilhados que transformam erro de conteúdo em falha de dados acionável. */
public final class EnemyCodecs {
    private EnemyCodecs() { }

    public static <E extends Enum<E>> Codec<E> enumCodec(Class<E> tipo) {
        return Codec.STRING.comapFlatMap(valor -> {
            try {
                return DataResult.success(Enum.valueOf(tipo, valor));
            } catch (IllegalArgumentException erro) {
                return DataResult.error(() -> "valor '" + valor + "' invalido para " + tipo.getSimpleName());
            }
        }, Enum::name);
    }
}
