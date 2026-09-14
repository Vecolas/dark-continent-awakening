package com.darkcontinent.nenfoundation.enemy.base;

import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyMetadata;
import com.darkcontinent.nenfoundation.enemy.api.HxHEnemy;
import java.util.Optional;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/**
 * Base comum sem regras de dano; subclasses fornecem sensores e goals.
 *
 * <p>Desde a issue #136 ela tambem carrega o LUGAR do {@link EnemyRuntime} e, com
 * ele, a limpeza. A instalacao e opcional -- os mobs que nasceram antes da
 * fundacao continuam com os proprios sensores -- mas quem instalar ganha de graca
 * a unica coisa que nao pode ser esquecida: o apagamento em morte e em remocao.
 * Deixar cada mob lembrar disso e como o buff fica ligado para sempre num dos
 * pontos de saida, e o esquecido nunca da erro.</p>
 */
public abstract class BaseHxHMob extends PathfinderMob implements HxHEnemy {
    private final EnemyMetadata metadata;
    private final EnemyBrain brain;
    private EnemyCombatState combatState = EnemyCombatState.IDLE;
    private EnemyRuntime runtime;

    protected BaseHxHMob(EntityType<? extends PathfinderMob> type, Level level,
            EnemyMetadata metadata, AwarenessTuning tuning) {
        super(type, level);
        this.metadata = metadata;
        this.brain = new EnemyBrain(tuning);
    }

    @Override public final EnemyMetadata enemyMetadata() { return metadata; }
    protected final EnemyBrain enemyBrain() { return brain; }
    protected final void combatState(EnemyCombatState state) { this.combatState = state; }
    @Override public final EnemyCombatState combatState() { return combatState; }
    @Override public final EnemyAwarenessState awarenessState() { return brain.state(); }

    /**
     * Instala percepcao, ataque e stagger nesta entidade.
     *
     * <p>Chamada no construtor da subclasse. Uma vez so: reinstalar trocaria a
     * memoria do mob no meio de uma perseguicao, e o sintoma seria um bicho que
     * esquece o alvo sem motivo aparente.</p>
     */
    protected final void instalarRuntime(EnemyRuntime runtime) {
        if (this.runtime != null) {
            throw new IllegalStateException("runtime ja instalado em " + getType().getDescriptionId());
        }
        if (runtime == null) throw new NullPointerException("runtime ausente");
        this.runtime = runtime;
    }

    /** Vazio para os mobs anteriores a fundacao; presente para quem instalou. */
    protected final Optional<EnemyRuntime> runtime() { return Optional.ofNullable(runtime); }

    /**
     * Runtime obrigatorio, para quem sabe que instalou.
     *
     * <p>A alternativa seria devolver null e deixar cada chamada decidir. Null
     * aqui viraria {@code NullPointerException} no meio de um tick de servidor,
     * com pilha que nao diz qual mob esqueceu de instalar.</p>
     */
    protected final EnemyRuntime runtimeExigido() {
        if (runtime == null) {
            throw new IllegalStateException(getType().getDescriptionId() + " usa o runtime de"
                    + " inimigo sem ter chamado instalarRuntime no construtor");
        }
        return runtime;
    }

    /**
     * Morte apaga o runtime ANTES de virar loot e evento.
     *
     * <p>Morrer no meio de um ataque nao pode deixar a fase presa em ACTIVE: a
     * entidade some da tela e a caixa de golpe continuaria valendo pelo resto do
     * tick. Nao da erro; da dano vindo de um bicho que ja morreu.</p>
     */
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && runtime != null) {
            runtime.limpar();
            combatState(EnemyCombatState.DYING);
        }
        super.die(source);
    }

    /** Remocao -- unload, troca de dimensao, comando -- passa pela MESMA limpeza. */
    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide && runtime != null) runtime.limpar();
        super.remove(reason);
    }
}
