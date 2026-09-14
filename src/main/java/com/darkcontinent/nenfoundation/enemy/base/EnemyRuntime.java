package com.darkcontinent.nenfoundation.enemy.base;

import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.combat.AttackController;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionController;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionSnapshot;
import com.darkcontinent.nenfoundation.enemy.perception.SensorDeVisao;
import java.util.Objects;

/**
 * As quatro pecas de um inimigo vivo, com UM tick e UMA limpeza.
 *
 * <p><b>Por que elas viajam juntas.</b> Percepcao, cerebro, ataque e stagger tem
 * ciclos de vida identicos: nascem com a entidade, avancam um tick por tick e
 * precisam ser apagados nos mesmos pontos de saida. Deixar cada mob chamar os
 * quatro na ordem que lembrar produz exatamente o erro que este projeto ja sabe
 * que comete -- limpeza espalhada pelos pontos de saida, com um deles esquecido.
 * O esquecido nao da erro: deixa um mob perseguindo um fantasma, ou com a fase
 * presa em ACTIVE machucando quem passar perto, ate o servidor reiniciar.</p>
 *
 * <p><b>A ordem do tick nao e arbitraria.</b> Percepcao mede, cerebro decide,
 * stagger cobra o preco do que ja aconteceu e o ataque avanca o relogio. Inverter
 * stagger e ataque faria a interrupcao chegar um tick depois do golpe que ela
 * deveria cortar -- e um tick de diferenca e invisivel em teste e visivel na
 * tela.</p>
 *
 * <p>Uma instancia por ENTIDADE. Compartilhar uma seria a versao deste projeto do
 * erro classico: dois mobs decidindo com a mesma memoria.</p>
 */
public final class EnemyRuntime {
    private final PerceptionController percepcao;
    private final EnemyBrain cerebro;
    private final AttackController ataques;
    private final StaggerState stagger;

    public EnemyRuntime(PerceptionController percepcao, EnemyBrain cerebro,
            AttackController ataques, StaggerState stagger) {
        this.percepcao = Objects.requireNonNull(percepcao, "percepcao ausente");
        this.cerebro = Objects.requireNonNull(cerebro, "cerebro ausente");
        this.ataques = Objects.requireNonNull(ataques, "controlador de ataque ausente");
        this.stagger = Objects.requireNonNull(stagger, "stagger ausente");
    }

    public PerceptionController percepcao() { return percepcao; }
    public EnemyBrain cerebro() { return cerebro; }
    public AttackController ataques() { return ataques; }
    public StaggerState stagger() { return stagger; }

    /**
     * Um tick do inimigo inteiro.
     *
     * @return o snapshot da percepcao, para quem precisar do alvo ou da distancia
     */
    public PerceptionSnapshot tick(int tickDoMundo, SensorDeVisao sensor,
            boolean vidaCritica, boolean oportunidadeDeEmboscada) {
        PerceptionSnapshot snapshot = percepcao.tick(tickDoMundo, sensor,
                vidaCritica, oportunidadeDeEmboscada);
        cerebro.tick(snapshot.paraCerebro());
        stagger.tick();
        ataques.tick();
        return snapshot;
    }

    public EnemyAwarenessState consciencia() { return cerebro.state(); }

    /**
     * Aplica stagger e, se ele disparar, INTERROMPE o ataque em curso.
     *
     * <p>Esta e a unica ligacao entre os dois sistemas, e ela mora aqui de
     * proposito. Escrita dentro do {@code StaggerState}, ela faria o acumulador
     * conhecer o ataque; escrita dentro do {@code AttackController}, faria o
     * ataque conhecer o acumulador. Os dois acoplamentos sobrevivem ate o
     * primeiro mob que queira cambalear sem ter ataque nenhum.</p>
     *
     * @param ticksDeRecargaAposInterrupcao recarga imposta a quem foi interrompido
     * @return o resultado do stagger, para quem precisa de som ou de troca de estado
     */
    public StaggerResult sofrerStagger(long instanciaDeAtaque, float valorBruto,
            int ticksDeRecargaAposInterrupcao) {
        StaggerResult resultado = stagger.acumular(instanciaDeAtaque, valorBruto);
        if (resultado == StaggerResult.DISPAROU) {
            ataques.reset();
        }
        return resultado;
    }

    /**
     * Ponto UNICO de limpeza: morte, remocao, unload, troca de dimensao.
     *
     * <p>Quem liga, desliga -- e o par mora no ciclo de vida de quem ligou, nunca
     * espalhado pelos varios pontos de saida.</p>
     */
    public void limpar() {
        percepcao.limpar();
        cerebro.reset();
        ataques.reset();
        stagger.limpar();
    }
}
