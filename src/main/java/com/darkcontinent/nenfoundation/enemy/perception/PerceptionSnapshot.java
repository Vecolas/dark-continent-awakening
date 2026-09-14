package com.darkcontinent.nenfoundation.enemy.perception;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessInput;
import java.util.Optional;
import java.util.UUID;

/**
 * O que a percepcao apurou neste tick -- a ponte ate o {@code EnemyBrain}.
 *
 * <p>Ela existe para que o cerebro continue nao sabendo NADA sobre mundo. O
 * controller mede; este record carrega o resultado; o cerebro decide. Sem esta
 * fronteira, a primeira urgencia de gameplay coloca um {@code level().getEntities}
 * dentro da maquina de estados, e a partir dai a IA deixa de ser testavel.</p>
 */
public record PerceptionSnapshot(UUID alvo, boolean visivel, boolean audivel,
        boolean dentroDoTerritorio, boolean recuando, int memoriaRestante,
        double distancia, boolean oportunidadeDeEmboscada, boolean vidaCritica) {

    public PerceptionSnapshot {
        if (memoriaRestante < 0) throw new IllegalArgumentException("memoria negativa");
        if (!Double.isNaN(distancia) && (!Double.isFinite(distancia) || distancia < 0.0D)) {
            throw new IllegalArgumentException("distancia invalida");
        }
    }

    /** Leitura sem alvo nenhum; o cerebro cai em ROAM. */
    public static PerceptionSnapshot vazio(boolean vidaCritica) {
        return new PerceptionSnapshot(null, false, false, false, false, 0, Double.NaN, false, vidaCritica);
    }

    public Optional<UUID> alvoOpcional() { return Optional.ofNullable(alvo); }

    /**
     * Traduz para a entrada que o cerebro ja entende.
     *
     * <p>{@code targetLost} e derivado, e nao um campo proprio: alvo perdido e
     * exatamente "nao vejo e nao ouco". Guardar os dois abriria a porta para eles
     * discordarem, e a discordancia nao daria erro -- daria um mob que persegue
     * um alvo que ele mesmo considera perdido.</p>
     */
    public AwarenessInput paraCerebro() {
        boolean perdido = alvo == null || (!visivel && !audivel && memoriaRestante == 0);
        return new AwarenessInput(visivel, audivel, dentroDoTerritorio, recuando, perdido,
                memoriaRestante, oportunidadeDeEmboscada, vidaCritica);
    }
}
