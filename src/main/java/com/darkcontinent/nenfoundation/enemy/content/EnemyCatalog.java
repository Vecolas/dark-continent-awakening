package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.data.EnemyDefinition;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A lista UNICA de tudo que este repositorio publica como inimigo registrado.
 *
 * <p><b>Ela existe no dia em que a segunda familia nasceu, e nao depois.</b> Ate
 * aqui {@code HunterExamProfiles.publicados()} era, sozinho, a fonte que os
 * portoes varriam -- e no instante em que {@link GreedIslandProfiles} passou a
 * publicar sete criaturas, aquela lista deixou de ser "tudo" e virou "uma
 * parte". Um portao que continua lendo uma parte nao reprova: ele segue VERDE
 * varrendo menos, que e o falso verde mais barato que existe. Foi exatamente o
 * que custou meses ao foxbear (issue #266), e a licao foi escrita no proprio
 * {@code FilaUnicaDeInimigosTest}.</p>
 *
 * <p><b>Ela reprova id repetido entre familias.</b> Duas familias declarando o
 * mesmo id nao dariam erro em lugar nenhum: uma sobrescreveria a outra num
 * {@code Map}, e o mob perderia os atributos da definicao perdida sem que nada
 * apontasse para a colisao.</p>
 *
 * <p>QUEM CRIAR UMA TERCEIRA FAMILIA ACRESCENTA UMA LINHA AQUI, no mesmo PR. E
 * um lugar so, e e o unico que os portoes leem.</p>
 */
public final class EnemyCatalog {

    private EnemyCatalog() { }

    public static Map<String, EnemyDefinition> publicados() {
        Map<String, EnemyDefinition> tudo = new LinkedHashMap<>();
        juntar(tudo, "HunterExamProfiles", HunterExamProfiles.publicados());
        juntar(tudo, "GreedIslandProfiles", GreedIslandProfiles.publicados());
        juntar(tudo, "ChimeraProfiles", ChimeraProfiles.publicados());
        return Map.copyOf(tudo);
    }

    private static void juntar(Map<String, EnemyDefinition> destino, String familia,
            Map<String, EnemyDefinition> parcela) {
        parcela.forEach((id, definicao) -> {
            EnemyDefinition anterior = destino.putIfAbsent(id, definicao);
            if (anterior != null) {
                throw new IllegalStateException("o id '" + id + "' e publicado por duas familias"
                        + " (a segunda e " + familia + "). Sem esta recusa, uma sobrescreveria a"
                        + " outra no mapa e o mob perderia os atributos da definicao perdida --"
                        + " sem nada apontar para a colisao.");
            }
        });
    }
}
