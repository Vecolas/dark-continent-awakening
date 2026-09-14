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
     * A UNICA porta publica para o runtime, e ela e de LEITURA.
     *
     * <p>Existe para as ferramentas de debug (#113) poderem imprimir fase de
     * ataque, stagger e alvo lembrado de QUALQUER inimigo sem uma linha de
     * logica especifica de criatura -- e sem que um comando de diagnostico ganhe
     * o poder de mexer no estado que ele foi chamado para observar.
     *
     * <p>Vazio nao e erro: os mobs que nasceram antes da fundacao trazem os
     * proprios sensores e nunca chamaram {@code instalarRuntime}. Quem exibe
     * isto precisa DIZER que esta vazio em vez de imprimir zeros -- zero de
     * stagger e "nao apanhou", vazio e "esta peca nao existe neste mob", e
     * confundir os dois manda a proxima pessoa atras de um bug que nao ha.
     */
    public final Optional<EnemyDebugView> visaoDeDebug() {
        return runtime == null ? Optional.empty() : Optional.of(EnemyDebugView.de(runtime));
    }

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

    // ------------------------------------------------------------------- voz

    /**
     * A voz deste mob, resolvida do id da metadata. Vazia enquanto ele nao tiver.
     *
     * <p>Ela e buscada por ID e nao instalada pela subclasse DE PROPOSITO: a
     * instalacao manual seria mais uma linha que o decimo mob esqueceria, e o
     * esquecido nao daria erro -- deixaria o bicho mudo, que e exatamente o
     * estado em que os sete primeiros passaram meses.</p>
     */
    private java.util.Optional<com.darkcontinent.nenfoundation.sound.EnemyVoice> voz() {
        return com.darkcontinent.nenfoundation.sound.EnemySoundEvents
                .voz(enemyMetadata().id().getPath());
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return voz().map(com.darkcontinent.nenfoundation.sound.EnemyVoice::ambienteResolvido)
                .orElse(null);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return voz().map(com.darkcontinent.nenfoundation.sound.EnemyVoice::dorResolvida)
                .orElse(null);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return voz().map(com.darkcontinent.nenfoundation.sound.EnemyVoice::morteResolvida)
                .orElse(null);
    }

    /** Estado de combate do tick anterior; e a borda dele que dispara som. */
    private EnemyCombatState estadoSonoroAnterior = EnemyCombatState.IDLE;

    /**
     * ALERTA e ATAQUE saem daqui, lendo a BORDA do estado de combate.
     *
     * <p><b>Por que na base, e nao em cada mob.</b> Tocar o som dentro da Goal de
     * cada bicho e mais direto e garante que o vigesimo esqueca -- e um mob sem
     * alerta ataca do nada, sem que nada reprove. Aqui a regra e uma so: todo mob
     * que MUDA para AGGRO avisa, e todo mob que MUDA para WINDUP telegrafa.</p>
     *
     * <p><b>Borda, e nao estado.</b> Tocar enquanto o estado VALE repetiria o som
     * a cada tick -- vinte grunhidos por segundo, que o jogador ouve como
     * zumbido e nao como aviso.</p>
     */
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        EnemyCombatState agora = combatState();
        if (agora != estadoSonoroAnterior) {
            voz().ifPresent(voz -> {
                if (agora == EnemyCombatState.AGGRO
                        && estadoSonoroAnterior != EnemyCombatState.WINDUP
                        && estadoSonoroAnterior != EnemyCombatState.ACTIVE
                        && estadoSonoroAnterior != EnemyCombatState.RECOVERY) {
                    // So avisa ao ENTRAR em combate, e nao ao voltar de um golpe:
                    // sem esta condicao o mob grita o alerta depois de cada
                    // ataque, e o aviso perde o significado.
                    playSound(voz.alertaResolvido(), 1.0F, vozGrave());
                } else if (agora == EnemyCombatState.WINDUP) {
                    playSound(voz.ataqueResolvido(), 1.0F, vozGrave());
                }
            });
            estadoSonoroAnterior = agora;
        }
    }

    /**
     * Tom da voz deste individuo.
     *
     * <p>Derivado do uuid, e nao sorteado: uma manada de great stamps com a voz
     * exatamente igual soa como um mob so tocando varias vezes. A variacao e
     * pequena (+-6%) porque acima disso a altura deixa de dizer o TAMANHO do
     * bicho, e o ouvido usa altura para estimar tamanho antes de qualquer outra
     * coisa.</p>
     */
    private float vozGrave() {
        int mistura = getUUID().hashCode();
        return 1.0F + ((mistura & 0xFF) / 255.0F - 0.5F) * 0.12F;
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
