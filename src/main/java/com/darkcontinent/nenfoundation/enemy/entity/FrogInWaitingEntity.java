package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AmbushRules;
import com.darkcontinent.nenfoundation.enemy.ai.AwarenessInput;
import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.base.BaseHxHMob;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.AttackTimeline;
import com.darkcontinent.nenfoundation.enemy.combat.GrabRules;
import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import java.util.EnumSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Frog-in-waiting: o emboscador enterrado. A ameaca e o chao que se abre, e a
 * resposta do jogador e BATER -- nele ou por um aliado -- antes de a digestao
 * terminar.
 *
 * <p>DECISAO CENTRAL QUE ESTE ARQUIVO CARREGA: engolir NAO MATA NA HORA. A
 * bocada prende a vitima e abre uma janela de {@link GrabRules#ticksMaximos()}
 * ticks em que ela pode se soltar no dano. Sem essa janela o mob seria morte
 * instantanea sem aviso, que e exatamente o que o plano proibe.</p>
 *
 * <p>SEGUNDA DECISAO: existe UM metodo que solta ({@link #soltar()}), e todos
 * os caminhos passam por ele -- tempo, dano, morte da vitima, morte do sapo,
 * remocao da entidade e troca de dimensao. Um caminho de saida que limpasse o
 * agarrao "por conta propria" nao daria erro nenhum; daria um jogador preso
 * dentro de um sapo ate o servidor reiniciar.</p>
 *
 * <p>Tudo que decide -- se a emboscada dispara, quem foi engolido, quanto dano
 * pulsa e quando solta -- roda no servidor. Os dois campos sincronizados
 * carregam apenas ENTERRADO e a FASE do ataque, que e o que o cliente precisa
 * para desenhar terra, emerge e mastigada. O cliente nunca informa acerto.</p>
 */
public final class FrogInWaitingEntity extends BaseHxHMob {
    /** Enterrado ou nao; o cliente precisa saber para nao desenhar o sapo inteiro. */
    private static final EntityDataAccessor<Boolean> ENTERRADO =
            SynchedEntityData.defineId(FrogInWaitingEntity.class, EntityDataSerializers.BOOLEAN);
    /** Fase do ataque; o unico estado de combate que o cliente precisa conhecer. */
    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(FrogInWaitingEntity.class, EntityDataSerializers.INT);

    private static final AttackDefinition BOCADA = HunterExamProfiles.frogSwallow();
    private static final AmbushRules EMBOSCADA = HunterExamProfiles.frogAmbushRules();
    private static final GrabRules AGARRAO = HunterExamProfiles.frogGrabRules();

    /** Quanto tempo o sapo lembra de um alvo que saiu da linha de visao. */
    private static final int MEMORIA_DE_ALVO_TICKS = 120;
    /** Quantos ticks de aviso antes de o cerebro passar de WARN para ENGAGE. */
    private static final int TICKS_DE_AVISO = 20;
    /** Dentro deste raio o intruso ja esta em cima da toca. */
    private static final double RAIO_DE_TERRITORIO = 8.0D;
    /** Afastar-se mais do que isto num tick conta como recuo. */
    private static final double TOLERANCIA_DE_RECUO = 0.35D;
    /** Fracao da vida abaixo da qual o cerebro pode decidir fugir. */
    private static final float FRACAO_DE_VIDA_CRITICA = 0.25F;
    /** Folga da caixa usada para achar quem cabe na bocada. */
    private static final double ALCANCE_DA_BOCADA = 0.6D;
    /**
     * Quanto tempo o sapo fica de boca fechada depois de cuspir alguem.
     *
     * <p>Nao e botao de balanceamento: e o minimo para a soltura ser LEGIVEL --
     * sem ele o mesmo tick que cospe ja recomecaria a perseguir, e o jogador
     * nao veria que escapou.</p>
     */
    private static final int TICKS_DE_RETIRADA = 40;

    // Estado DA INSTANCIA. Guardar qualquer um destes numa Goal (ou num static)
    // faria todos os sapos do mundo compartilharem a mesma vitima -- a Goal e uma
    // por entidade, e a classe aninhada abaixo e static justamente para nao
    // deixar essa dependencia escondida.
    private AttackTimeline linhaDoTempoDaBocada = new AttackTimeline();
    private boolean bocadaEmAndamento;
    private LivingEntity vitimaAgarrada;
    private int ticksAgarrado;
    private float danoDesdeOAgarrao;
    private int recargaRestante;
    private int ticksSemAlvo;
    private int retiradaRestante;
    private LivingEntity presaDaEmboscada;

    // Sensores do cerebro.
    private int memoriaDeAlvo;
    private double distanciaAnteriorDoAlvo = Double.NaN;

    public FrogInWaitingEntity(EntityType<? extends FrogInWaitingEntity> type, Level level) {
        super(type, level, HunterExamProfiles.frogInWaiting().metadata(),
                new AwarenessTuning(TICKS_DE_AVISO, MEMORIA_DE_ALVO_TICKS));
        // Nasce escondido: o sapo que aparece andando nunca emboscou ninguem.
        setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = HunterExamProfiles.frogInWaiting().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<FrogInWaitingEntity> registeredType() {
        return EnemyEntityTypes.FROG_IN_WAITING.get();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ENTERRADO, Boolean.TRUE);
        builder.define(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
    }

    /** Enterrado; vale nos dois lados, porque vem do SynchedEntityData. */
    public boolean estaEnterrado() { return this.entityData.get(ENTERRADO); }

    /** Fase corrente da bocada; vale nos dois lados, pelo mesmo motivo. */
    public AttackPhase faseDeAtaque() {
        int ordinal = this.entityData.get(FASE_DE_ATAQUE);
        AttackPhase[] fases = AttackPhase.values();
        return ordinal >= 0 && ordinal < fases.length ? fases[ordinal] : AttackPhase.IDLE;
    }

    /**
     * Verdade do SERVIDOR sobre haver alguem preso.
     *
     * <p>Nao e sincronizado de proposito: quem precisa dela e a regra que
     * recusa a desmontagem, e essa regra so vale no servidor. No cliente ela
     * responde {@code false}, e o cliente e corrigido pelo pacote de veiculo.</p>
     */
    public boolean estaAgarrando() { return vitimaAgarrada != null; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new EmboscadaGoal(this));
        goalSelector.addGoal(2, new AtaqueCorpoACorpoGoal(this));
        goalSelector.addGoal(6, new PasseioForaDaTocaGoal(this));
        goalSelector.addGoal(7, new EncararForaDaTocaGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;
        if (recargaRestante > 0) recargaRestante--;
        if (retiradaRestante > 0) retiradaRestante--;

        // A bocada e o agarrao sao tickados AQUI, e nao dentro da Goal, porque a
        // Goal pode ser preemptada por outra de prioridade maior (a FloatGoal, por
        // exemplo). Se o relogio da bocada dependesse da Goal viva, um mergulho no
        // pantano congelaria a fase em ACTIVE -- sem erro nenhum no log.
        if (bocadaEmAndamento) tickDaBocada();
        tickDoAgarrao();

        if (estaEnterrado()) manterEnterrado();
        else tickForaDaTerra();

        EnemyAwarenessState consciencia = enemyBrain().tick(lerSensores());
        alinharEstadoDeCombate(consciencia);
    }

    /**
     * Acumula o dano recebido ENQUANTO agarra -- e so enquanto agarra.
     *
     * <p>E este acumulador que faz "bater para escapar" e "um aliado te tirar de
     * la" existirem: sem ele a unica saida seria o relogio, e a janela viraria
     * decoracao. O sapo nao tem ponto fraco e este metodo NAO multiplica dano
     * nenhum; ele so mede.</p>
     *
     * <p>O que conta e o dano PEDIDO, antes de armadura e resistencia: o preco
     * da soltura e o esforco de quem bate, nao a defesa do sapo.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean agarrandoAntes = estaAgarrando();
        boolean aplicou = super.hurt(source, amount);
        if (aplicou && !level().isClientSide && agarrandoAntes
                && Float.isFinite(amount) && amount > 0.0F) {
            danoDesdeOAgarrao += amount;
        }
        return aplicou;
    }

    @Override
    public void die(DamageSource source) {
        // Quem liga, desliga: morrer com alguem na boca nao pode deixar a vitima presa.
        if (!level().isClientSide) {
            combatState(EnemyCombatState.DYING);
            soltar();
        }
        super.die(source);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide) soltar();
        super.remove(reason);
    }

    /**
     * Troca de dimensao solta ANTES de viajar.
     *
     * <p>O vanilla desmonta os passageiros ao mudar de dimensao, e a nossa regra
     * de desmontagem recusaria justamente essa desmontagem -- o sapo iria para a
     * outra dimensao e a vitima ficaria para tras presa a um veiculo que nao
     * existe mais. Este e o terceiro ponto de saida, e ele passa pelo mesmo
     * {@link #soltar()}.</p>
     */
    @Override
    public Entity changeDimension(DimensionTransition transition) {
        if (!level().isClientSide) soltar();
        return super.changeDimension(transition);
    }

    // ------------------------------------------------------------- enterrado

    /** Enterrado o sapo nao navega, nao anda e nao faz barulho. */
    private void manterEnterrado() {
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
        presaDaEmboscada = procurarPresa();
        if (presaDaEmboscada != null) setTarget(presaDaEmboscada);
    }

    private void tickForaDaTerra() {
        presaDaEmboscada = null;
        boolean temAlvo = getTarget() != null && getTarget().isAlive();
        ticksSemAlvo = temAlvo ? 0 : ticksSemAlvo + 1;
        if (bocadaEmAndamento || estaAgarrando()) return;
        if (EMBOSCADA.reenterra(estaEnterrado(), temAlvo, ticksSemAlvo)) enterrar();
    }

    private void enterrar() {
        this.entityData.set(ENTERRADO, Boolean.TRUE);
        setSilent(true);
        setTarget(null);
        getNavigation().stop();
        ticksSemAlvo = 0;
        combatState(EnemyCombatState.IDLE);
    }

    private void desenterrar() {
        this.entityData.set(ENTERRADO, Boolean.FALSE);
        setSilent(false);
        ticksSemAlvo = 0;
    }

    /**
     * Quem esta pisando na zona AGORA. O cilindro (raio x altura) e a recarga
     * vem do {@link AmbushRules}; este metodo so mede e filtra.
     */
    private LivingEntity procurarPresa() {
        double raio = EMBOSCADA.raioDeGatilho();
        double altura = EMBOSCADA.alturaDeGatilho();
        AABB zona = getBoundingBox().inflate(raio, altura, raio);
        LivingEntity melhor = null;
        double menorDistancia = Double.MAX_VALUE;
        for (LivingEntity candidato : level().getEntitiesOfClass(LivingEntity.class, zona, this::presaValida)) {
            double dx = candidato.getX() - getX();
            double dz = candidato.getZ() - getZ();
            double distanciaHorizontal = Math.sqrt(dx * dx + dz * dz);
            double diferencaDeAltura = candidato.getY() - getY();
            if (!EMBOSCADA.dispara(estaEnterrado(), true, distanciaHorizontal,
                    diferencaDeAltura, recargaRestante)) {
                continue;
            }
            if (distanciaHorizontal < menorDistancia) {
                menorDistancia = distanciaHorizontal;
                melhor = candidato;
            }
        }
        return melhor;
    }

    /**
     * Presa legitima: viva, atacavel, nao aliada, nao outro sapo e nao alguem
     * que ja esteja montado em outra coisa.
     *
     * <p>O filtro de espectador/criativo esta aqui e nao no dano: engolir um
     * espectador nao daria erro, so prenderia a camera de quem so queria
     * assistir.</p>
     */
    private boolean presaValida(LivingEntity candidato) {
        if (candidato == this || candidato == null) return false;
        if (candidato.getType() == getType()) return false;
        if (!candidato.isAlive() || !candidato.isAttackable() || isAlliedTo(candidato)) return false;
        if (candidato.isPassenger()) return false;
        return !(candidato instanceof Player jogador && (jogador.isCreative() || jogador.isSpectator()));
    }

    // ---------------------------------------------------------------- bocada

    /** Comeca o emerge; a linha do tempo e NOVA para nunca herdar fase presa. */
    private void iniciarBocada() {
        desenterrar();
        bocadaEmAndamento = true;
        linhaDoTempoDaBocada = new AttackTimeline();
        linhaDoTempoDaBocada.start(BOCADA);
        getNavigation().stop();
        if (presaDaEmboscada != null) setTarget(presaDaEmboscada);
        combatState(EnemyCombatState.WINDUP);
        publicarFase(AttackPhase.WINDUP);
    }

    /**
     * WINDUP e o emerge -- o unico aviso que o jogador recebe. ACTIVE e a
     * mordida. RECOVERY e a digestao.
     */
    private void tickDaBocada() {
        AttackPhase fase = linhaDoTempoDaBocada.phase();
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
        LivingEntity alvo = getTarget();
        switch (fase) {
            case WINDUP -> {
                combatState(EnemyCombatState.WINDUP);
                if (alvo != null) getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            }
            case ACTIVE -> {
                combatState(EnemyCombatState.ACTIVE);
                tentarAgarrar();
            }
            case RECOVERY -> combatState(EnemyCombatState.ACTIVE);
            default -> { }
        }
        linhaDoTempoDaBocada.tick();
        AttackPhase proxima = linhaDoTempoDaBocada.phase();
        if (proxima != AttackPhase.COMPLETE) {
            publicarFase(proxima);
            return;
        }
        bocadaEmAndamento = false;
        if (estaAgarrando()) {
            // A digestao dura mais do que a linha do tempo do ataque: quem termina
            // o episodio e o agarrao, nao o relogio da bocada.
            publicarFase(AttackPhase.RECOVERY);
            return;
        }
        // Bocada no vazio: arma a recarga pelo MESMO caminho de sempre.
        soltar();
    }

    /** A primeira presa valida na boca e engolida; uma por bocada. */
    private void tentarAgarrar() {
        if (estaAgarrando()) return;
        AABB boca = getBoundingBox().inflate(ALCANCE_DA_BOCADA);
        for (LivingEntity vitima : level().getEntitiesOfClass(LivingEntity.class, boca, this::presaValida)) {
            // AGARRA PRIMEIRO, MORDE DEPOIS -- e nao o contrario.
            //
            // Condicionar o agarrao ao dano da mordida parece natural e quebra o mob
            // inteiro em dois casos reais: no PACIFICO o dano de mob contra jogador e
            // zerado e hurt() devolve false, e durante os ticks de invulnerabilidade
            // de um golpe anterior tambem. Nos dois o sapo morderia para sempre sem
            // nunca engolir ninguem -- sem erro nenhum no log.
            if (!vitima.startRiding(this, true)) {
                // Nao coube na boca: leva a mordida e o empurrao, e a bocada segue vazia.
                vitima.hurt(damageSources().mobAttack(this), BOCADA.damage());
                vitima.knockback(BOCADA.knockback(), getX() - vitima.getX(), getZ() - vitima.getZ());
                return;
            }
            vitimaAgarrada = vitima;
            ticksAgarrado = 0;
            danoDesdeOAgarrao = 0.0F;
            vitima.hurt(damageSources().mobAttack(this), BOCADA.damage());
            return;
        }
    }

    // --------------------------------------------------------------- agarrao

    /**
     * Relogio do agarrao. Quem decide soltar e {@link GrabRules#solta}, uma vez
     * so, com as quatro razoes juntas.
     */
    private void tickDoAgarrao() {
        LivingEntity vitima = vitimaAgarrada;
        if (vitima == null) {
            // RELOAD. O vanilla restaura passageiros salvos, mas o agarrao inteiro --
            // vitima, relogio e dano acumulado -- e runtime e nao sobrevive ao save.
            // Sem esta linha o jogador volta montado num sapo que nao sabe que o
            // agarrou: sem pulso de dano, sem soltura, sem erro nenhum no log.
            if (!getPassengers().isEmpty()) ejectPassengers();
            return;
        }
        // A vitima pode sumir por fora do nosso ciclo (logout, /kill, outro mod a
        // desmontou). Sem esta checagem o sapo ficaria "agarrando" um fantasma e a
        // regra de desmontagem recusaria a desmontagem de todo mundo depois dela.
        if (vitima.isRemoved() || vitima.getVehicle() != this) {
            soltar();
            return;
        }
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
        ticksAgarrado++;
        if (AGARRAO.aplicaDano(ticksAgarrado)) {
            vitima.hurt(damageSources().mobAttack(this), AGARRAO.danoPorPulso());
        }
        if (AGARRAO.solta(ticksAgarrado, danoDesdeOAgarrao, !vitima.isAlive(), !isAlive())) soltar();
    }

    /**
     * SAIDA UNICA do agarrao e do episodio de emboscada.
     *
     * <p>Tempo, dano, morte da vitima, morte do sapo, remocao da entidade,
     * troca de dimensao e bocada no vazio terminam TODOS aqui. E aqui que o
     * dano para, a passageira sai, a recarga e armada e a fase volta para IDLE.
     * Se um caminho deixar de passar por aqui, o jogador fica preso para
     * sempre -- e isso nao aparece como erro, aparece como um relato de bug
     * impossivel de reproduzir.</p>
     */
    private void soltar() {
        LivingEntity vitima = vitimaAgarrada;
        // A referencia cai ANTES da desmontagem de proposito: EnemyGameEvents recusa
        // desmontar enquanto o sapo disser que ainda agarra, e isso incluiria esta.
        vitimaAgarrada = null;
        ticksAgarrado = 0;
        danoDesdeOAgarrao = 0.0F;
        if (vitima != null && vitima.getVehicle() == this) vitima.stopRiding();
        ejectPassengers();

        bocadaEmAndamento = false;
        linhaDoTempoDaBocada = new AttackTimeline();
        recargaRestante = EMBOSCADA.ticksDeRecarga();
        retiradaRestante = TICKS_DE_RETIRADA;
        ticksSemAlvo = 0;
        presaDaEmboscada = null;
        publicarFase(AttackPhase.IDLE);
        if (combatState() != EnemyCombatState.DYING) combatState(EnemyCombatState.RETREAT);
    }

    private void publicarFase(AttackPhase fase) {
        this.entityData.set(FASE_DE_ATAQUE, fase.ordinal());
    }

    // -------------------------------------------------------------- carona

    /**
     * A vitima fica DENTRO do sapo, e nao sentada em cima dele.
     *
     * <p>Em 1.21.1 este e o ponto real: {@code positionRider} deriva a posicao
     * de {@code getPassengerRidingPosition}, que chama este metodo. Sobrescrever
     * so aqui evita duas fontes para a mesma posicao.</p>
     */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0.0D, dimensions.height() * 0.25D, 0.0D);
    }

    /** Cuspida: a vitima sai AO LADO da boca, nao em cima dela. */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 olhar = getLookAngle();
        Vec3 lado = new Vec3(-olhar.z, 0.0D, olhar.x);
        if (lado.lengthSqr() < 1.0E-6D) lado = new Vec3(1.0D, 0.0D, 0.0D);
        double afastamento = getBbWidth() * 0.5D + passenger.getBbWidth() * 0.5D + 0.2D;
        Vec3 destino = lado.normalize().scale(afastamento);
        return new Vec3(getX() + destino.x, getBoundingBox().maxY, getZ() + destino.z);
    }

    // -------------------------------------------------------------- sensores

    /** Leitura de mundo do tick: e isto que faz awarenessState() parar de ser decorativo. */
    private AwarenessInput lerSensores() {
        LivingEntity alvo = getTarget();
        boolean alvoVivo = alvo != null && alvo.isAlive();
        boolean visivel = alvoVivo && hasLineOfSight(alvo);
        double distancia = alvoVivo ? distanceTo(alvo) : Double.NaN;

        if (visivel) memoriaDeAlvo = MEMORIA_DE_ALVO_TICKS;
        else if (memoriaDeAlvo > 0) memoriaDeAlvo--;

        boolean recuando = visivel && !Double.isNaN(distanciaAnteriorDoAlvo)
                && distancia > distanciaAnteriorDoAlvo + TOLERANCIA_DE_RECUO;
        distanciaAnteriorDoAlvo = distancia;

        boolean audivel = !visivel && memoriaDeAlvo > 0;
        boolean noTerritorio = visivel && distancia <= RAIO_DE_TERRITORIO;
        boolean vidaCritica = getHealth() <= getMaxHealth() * FRACAO_DE_VIDA_CRITICA;

        // AQUI o ambushOpportunity do EnemyBrain deixa de ser papel: o sapo esta
        // enterrado E tem alguem em cima da toca. O great stamp nunca pode dizer
        // isto, porque manada nao embosca.
        boolean emboscando = estaEnterrado() && presaDaEmboscada != null;

        return new AwarenessInput(visivel, audivel, noTerritorio, recuando, !alvoVivo,
                memoriaDeAlvo, emboscando, vidaCritica);
    }

    /** A bocada, o agarrao e a retirada mandam; fora deles quem manda e a consciencia. */
    private void alinharEstadoDeCombate(EnemyAwarenessState consciencia) {
        if (bocadaEmAndamento || estaAgarrando() || retiradaRestante > 0
                || combatState() == EnemyCombatState.DYING) {
            return;
        }
        if (consciencia == EnemyAwarenessState.FLEE) {
            combatState(EnemyCombatState.RETREAT);
            return;
        }
        if (estaEnterrado()) {
            combatState(EnemyCombatState.IDLE);
            return;
        }
        combatState(getTarget() != null ? EnemyCombatState.AGGRO : EnemyCombatState.IDLE);
    }

    /** Enterrado, digerindo ou recuando, o corpo nao e livre para andar nem bater. */
    private boolean corpoLivre() {
        return !estaEnterrado() && !bocadaEmAndamento && !estaAgarrando() && retiradaRestante == 0;
    }

    // ------------------------------------------------------------------ goals

    /**
     * Dispara a emboscada. E static de proposito: todo estado que ela le e
     * escreve mora na ENTIDADE, e o prefixo {@code sapo.} deixa isso impossivel
     * de esquecer. Um campo aqui seria compartilhado por todos os sapos.
     *
     * <p>Ela NAO ticka a bocada: quem faz isso e o {@code customServerAiStep},
     * para que uma Goal de prioridade maior nao congele a fase ao preemptar.</p>
     */
    private static final class EmboscadaGoal extends Goal {
        private final FrogInWaitingEntity sapo;

        private EmboscadaGoal(FrogInWaitingEntity sapo) {
            this.sapo = sapo;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (sapo.level().isClientSide) return false;
            if (sapo.bocadaEmAndamento || sapo.estaAgarrando()) return false;
            return sapo.presaDaEmboscada != null;
        }

        @Override public boolean canContinueToUse() {
            return sapo.bocadaEmAndamento || sapo.estaAgarrando();
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() { sapo.iniciarBocada(); }

        @Override public void tick() {
            LivingEntity alvo = sapo.getTarget();
            if (alvo != null) sapo.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
        }
    }

    /** Corpo a corpo comum, calado enquanto a toca, a bocada ou a retirada mandam. */
    private static final class AtaqueCorpoACorpoGoal extends MeleeAttackGoal {
        private final FrogInWaitingEntity sapo;

        private AtaqueCorpoACorpoGoal(FrogInWaitingEntity sapo) {
            super(sapo, 1.0D, true);
            this.sapo = sapo;
        }

        @Override public boolean canUse() { return sapo.corpoLivre() && super.canUse(); }

        @Override public boolean canContinueToUse() { return sapo.corpoLivre() && super.canContinueToUse(); }
    }

    /** Passear enterrado seria uma toca andando pelo pantano. */
    private static final class PasseioForaDaTocaGoal extends WaterAvoidingRandomStrollGoal {
        private final FrogInWaitingEntity sapo;

        private PasseioForaDaTocaGoal(FrogInWaitingEntity sapo) {
            super(sapo, 0.7D);
            this.sapo = sapo;
        }

        @Override public boolean canUse() { return sapo.corpoLivre() && super.canUse(); }

        @Override public boolean canContinueToUse() { return sapo.corpoLivre() && super.canContinueToUse(); }
    }

    /** Encarar so vale de fora da terra; enterrado o sapo nao tem cabeca visivel. */
    private static final class EncararForaDaTocaGoal extends LookAtPlayerGoal {
        private final FrogInWaitingEntity sapo;

        private EncararForaDaTocaGoal(FrogInWaitingEntity sapo) {
            super(sapo, Player.class, 8.0F);
            this.sapo = sapo;
        }

        @Override public boolean canUse() { return !sapo.estaEnterrado() && super.canUse(); }

        @Override public boolean canContinueToUse() { return !sapo.estaEnterrado() && super.canContinueToUse(); }
    }
}
