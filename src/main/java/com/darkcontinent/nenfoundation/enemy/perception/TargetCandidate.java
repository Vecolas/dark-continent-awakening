package com.darkcontinent.nenfoundation.enemy.perception;

import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import java.util.UUID;

/**
 * Um alvo possivel, ja MEDIDO pelo servidor.
 *
 * <p>Todo campo aqui e fato apurado do lado autoritativo: distancia, angulo,
 * linha de visao e dimensao. O avaliador nao consulta nada -- ele so ordena. E
 * isso que permite testar a escolha de alvo sem servidor e garante que nenhum
 * dado do cliente entre na decisao.</p>
 *
 * @param id uuid da entidade candidata
 * @param faccao faccao a que ela pertence, do ponto de vista do mod
 * @param distancia blocos ate o mob
 * @param cossenoDoOlhar 1 quando o mob ja olha para ela, -1 quando esta atras
 * @param linhaDeVisao false quando ha bloco no caminho
 * @param mesmaDimensao false encerra a candidatura, por mais perto que pareca
 * @param dentroDoTerritorio se ela esta dentro do territorio declarado do mob
 * @param jaFeriuOMob agressao recente promove o candidato acima da distancia
 */
public record TargetCandidate(UUID id, EnemyFaction faccao, double distancia,
        double cossenoDoOlhar, boolean linhaDeVisao, boolean mesmaDimensao,
        boolean dentroDoTerritorio, boolean jaFeriuOMob) {
    public TargetCandidate {
        if (id == null) throw new NullPointerException("candidato sem id");
        if (faccao == null) throw new NullPointerException("candidato sem faccao");
        if (!Double.isFinite(distancia) || distancia < 0.0D
                || !Double.isFinite(cossenoDoOlhar)
                || cossenoDoOlhar < -1.0D || cossenoDoOlhar > 1.0D) {
            throw new IllegalArgumentException("candidato com geometria invalida");
        }
    }
}
