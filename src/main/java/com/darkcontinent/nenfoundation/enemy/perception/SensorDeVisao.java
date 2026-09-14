package com.darkcontinent.nenfoundation.enemy.perception;

import java.util.List;

/**
 * A UNICA porta entre a percepcao e o mundo.
 *
 * <p>Quem implementa e a entidade, no servidor: ela varre a caixa que quiser,
 * mede distancia, angulo e linha de visao, e devolve candidatos ja apurados.
 * Nada deste pacote chama {@code level()} -- e por isso que a regra de escolha
 * de alvo roda em teste unitario, sem servidor, e que nenhum caminho de cliente
 * consegue alimenta-la.</p>
 *
 * <p>O metodo e chamado com PARCIMONIA: {@link PerceptionController} so o invoca
 * quando {@link PerceptionBudget} autoriza. Implementacoes podem, portanto,
 * gastar o que for necessario -- o custo ja esta amortizado pelo intervalo.</p>
 */
@FunctionalInterface
public interface SensorDeVisao {
    /** Candidatos dentro do alcance do mob, medidos AGORA pelo servidor. */
    List<TargetCandidate> varrer();
}
