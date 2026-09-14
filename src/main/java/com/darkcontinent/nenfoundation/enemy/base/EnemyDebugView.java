package com.darkcontinent.nenfoundation.enemy.base;

import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import java.util.Optional;
import java.util.UUID;

/**
 * Fotografia SO-LEITURA das quatro pecas de um {@link EnemyRuntime}.
 *
 * <p><b>Por que ela existe em vez de um getter do runtime.</b> Um
 * {@code public EnemyRuntime runtime()} resolveria o mesmo problema e abriria
 * outro: qualquer chamador -- um comando de debug, um adapter de pack, um mixin
 * de terceiro -- passaria a poder chamar {@code limpar()}, {@code reset()} ou
 * {@code start()} de fora do ciclo de vida da entidade. Isso nao daria erro: o
 * mob so perderia a memoria do alvo no meio de uma perseguicao, ou cambalearia
 * sem ninguem ter batido, e o sintoma apareceria longe da causa.
 *
 * <p><b>Ela e montada NA HORA e nunca guardada.</b> Uma instancia cacheada seria
 * a segunda fonte da mesma verdade -- e a copia velha nao acusa nada, so mostra
 * a fase de ataque de dois segundos atras para quem esta tentando entender por
 * que o golpe saiu.
 *
 * @param consciencia estado do cerebro
 * @param faseDeAtaque janela corrente do golpe
 * @param recargaRestante ticks que faltam para o proximo golpe poder comecar
 * @param instanciaDeAtaque id da instancia corrente; zero quando nao ha golpe
 * @param staggerAcumulado quanto de stagger esta somado agora
 * @param cambaleando se a janela de interrupcao esta aberta
 * @param alvoLembrado uuid que a memoria de ameaca ainda guarda
 * @param varredurasDePercepcao quantas varreduras caras o mob ja gastou
 */
public record EnemyDebugView(EnemyAwarenessState consciencia, AttackPhase faseDeAtaque,
        int recargaRestante, long instanciaDeAtaque, float staggerAcumulado,
        boolean cambaleando, Optional<UUID> alvoLembrado, long varredurasDePercepcao) {

    /** Le as quatro pecas de uma vez, no mesmo instante. */
    static EnemyDebugView de(EnemyRuntime runtime) {
        return new EnemyDebugView(
                runtime.consciencia(),
                runtime.ataques().phase(),
                runtime.ataques().cooldownRemaining(),
                runtime.ataques().attackInstanceId(),
                runtime.stagger().acumulado(),
                runtime.stagger().cambaleando(),
                runtime.percepcao().alvoLembrado(),
                runtime.percepcao().varredurasFeitas());
    }
}
