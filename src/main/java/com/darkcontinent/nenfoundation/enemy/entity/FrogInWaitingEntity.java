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
import com.darkcontinent.nenfoundation.enemy.combat.GrabController;
import com.darkcontinent.nenfoundation.enemy.combat.GrabRefusal;
import com.darkcontinent.nenfoundation.enemy.combat.GrabRelease;
import com.darkcontinent.nenfoundation.enemy.combat.GrabRules;
import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import java.util.EnumSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

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
 *
 * <p>CORPO PROPRIO (ADR-017). Ate aqui o sapo vestia geometria e textura do sapo
 * vanilla. Agora ele e um {@link GeoEntity}: modelo, esqueleto, animacoes e
 * textura sao autorais, e o andaime saiu. O comportamento nao mudou uma linha --
 * emboscada, agarrao, recusa de desmontagem e soltura sao os mesmos que os
 * quatro gametests medem.</p>
 */
public final class FrogInWaitingEntity extends BaseHxHMob implements GeoEntity {
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

    /**
     * O maior alvo que cabe na boca: 3.0 de altura por 1.6 de largura.
     *
     * <p>NAO e botao de balanceamento -- e a boca. O sapo engole INTEIRO, e um
     * predador assim precisa de um limite declarado, senao ele engole qualquer
     * coisa que consiga montar nele. A largura e o numero que morde: um Great
     * Stamp tem 1.9 e e recusado, enquanto um golem de ferro (1.4 x 2.7) passa
     * -- e e com o golem que os gametests medem a janela de escape.</p>
     *
     * <p>Sem o limite, a recusa nao existiria e o sintoma seria comico e
     * silencioso: uma manada inteira desaparecendo dentro de um sapo de 1.4
     * bloco, sem uma linha de log.</p>
     */
    private static final double ALTURA_MAXIMA_DA_PRESA = 3.0D;
    private static final double LARGURA_MAXIMA_DA_PRESA = 1.6D;

    // ------------------------------------------------------------- animacao
    // Os nomes abaixo sao um CONTRATO com frog_in_waiting.animation.json. Errar um
    // deles nao da erro: o GeckoLib simplesmente nao acha o clipe e deixa o osso
    // parado. E o tipo de falha que so aparece na tela de quem joga.
    private static final RawAnimation BURROWED = RawAnimation.begin().then("animation.frog_in_waiting.burrowed", Animation.LoopType.DEFAULT);
    private static final RawAnimation EMERGE = RawAnimation.begin().then("animation.frog_in_waiting.emerge", Animation.LoopType.DEFAULT);
    private static final RawAnimation BITE = RawAnimation.begin().then("animation.frog_in_waiting.bite", Animation.LoopType.DEFAULT);
    private static final RawAnimation DIGEST = RawAnimation.begin().then("animation.frog_in_waiting.digest", Animation.LoopType.DEFAULT);
    private static final RawAnimation IDLE = RawAnimation.begin().then("animation.frog_in_waiting.idle", Animation.LoopType.DEFAULT);
    private static final RawAnimation WALK = RawAnimation.begin().then("animation.frog_in_waiting.walk", Animation.LoopType.DEFAULT);

    /** Nome do unico controller; quem registrar um segundo clipe reusa esta constante. */
    private static final String CONTROLLER_DO_CORPO = "corpo";
    /**
     * Ticks de mistura entre um clipe e o proximo.
     *
     * <p>Quatro, e nao cinco: o emerge dura 10 ticks e a mordida dura 4. Uma
     * transicao mais longa do que a fase que ela atravessa comeria a mordida
     * inteira em mistura, e o jogador engolido nunca veria a boca fechar.</p>
     */
    private static final int TRANSICAO_EM_TICKS = 4;

    /** Cache por INSTANCIA. Um cache estatico faria todos os sapos compartilharem um clipe. */
    private final AnimatableInstanceCache cacheDeAnimacao = GeckoLibUtil.createInstanceCache(this);

    // Estado DA INSTANCIA. Guardar qualquer um destes numa Goal (ou num static)
    // faria todos os sapos do mundo compartilharem a mesma vitima -- a Goal e uma
    // por entidade, e a classe aninhada abaixo e static justamente para nao
    // deixar essa dependencia escondida.
    private AttackTimeline linhaDoTempoDaBocada = new AttackTimeline();
    private boolean bocadaEmAndamento;
    /**
     * O agarrao deixou de ser tres campos soltos e virou UM controlador.
     *
     * <p>Ele nasceu aqui dentro e resolveu bem o problema do sapo. O motivo da
     * mudanca (issue #140) e o SEGUNDO bicho: Melanin Lizard e Crab Heavy
     * precisam do mesmo contrato, e copiar do sapo produziria tres
     * implementacoes parecidas que divergem na primeira correcao -- e a que
     * esquecer um ponto de saida deixa um jogador preso ate o restart. O
     * CLAUDE.md e explicito: um unico ciclo de vida por familia.</p>
     *
     * <p>O corpo do agarrao continua sendo a MONTARIA do vanilla; o que saiu
     * daqui foi a contabilidade -- relogio, dano acumulado e a decisao de
     * soltar. O controlador nao toca em mundo.</p>
     */
    private final GrabController agarrao = new GrabController(HunterExamProfiles.frogGrabRules(),
            ALTURA_MAXIMA_DA_PRESA, LARGURA_MAXIMA_DA_PRESA);
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
    public boolean estaAgarrando() { return agarrao.agarrando(); }

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
            agarrao.registrarDanoNoPredador(amount);
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
            // A RECUSA VEM ANTES DO EFEITO, e ela tem motivo. Tentar montar
            // primeiro e descobrir depois que o alvo nao cabe funcionaria, mas
            // deixaria o porque invisivel: o relato de bug seria "as vezes o
            // sapo me morde e nao me engole", e ninguem descobriria que a
            // diferenca era o tamanho do alvo.
            GrabRefusal recusa = agarrao.podeAgarrar(vitima.getBbHeight(), vitima.getBbWidth(),
                    vitima.isPassenger(), true);
            if (recusa != GrabRefusal.NENHUMA || !vitima.startRiding(this, true)) {
                // Nao coube na boca: leva a mordida e o empurrao, e a bocada segue vazia.
                vitima.hurt(damageSources().mobAttack(this), BOCADA.damage());
                vitima.knockback(BOCADA.knockback(), getX() - vitima.getX(), getZ() - vitima.getZ());
                return;
            }
            agarrao.agarrar(vitima.getUUID());
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
        LivingEntity vitima = vitimaAgarradaAgora();
        if (!agarrao.agarrando()) {
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
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));

        // "Ainda presa" e medido no MUNDO e entregue ao controlador; ele nao
        // consulta nada. A vitima pode sumir por fora do nosso ciclo (logout,
        // /kill, outro mod a desmontou), e sem esta medida o sapo ficaria
        // agarrando um fantasma -- e a regra de desmontagem recusaria a
        // desmontagem de todo mundo depois dela.
        boolean aindaPresa = vitima != null && !vitima.isRemoved() && vitima.getVehicle() == this;
        GrabController.GrabTick resultado = agarrao.tick(
                vitima != null && vitima.isAlive(), aindaPresa, isAlive());

        if (resultado.pulsoDeDano() && vitima != null) {
            vitima.hurt(damageSources().mobAttack(this), AGARRAO.danoPorPulso());
        }
        if (resultado.soltou()) soltar();
    }

    /**
     * A vitima AGORA, reconstruida do uuid a cada uso.
     *
     * <p>Guardar a entidade num campo nao dava erro; so impedia o objeto de
     * morrer, e mantinha viva uma referencia que o servidor ja tinha descartado.
     * Reconstruir devolve {@code null} quando ela deixou de existir, que e
     * exatamente a resposta que o tick precisa.</p>
     */
    private LivingEntity vitimaAgarradaAgora() {
        if (!(level() instanceof ServerLevel servidor)) return null;
        return agarrao.vitima()
                .map(servidor::getEntity)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .orElse(null);
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
        LivingEntity vitima = vitimaAgarradaAgora();
        // O controlador e zerado ANTES da desmontagem de proposito: EnemyGameEvents
        // recusa desmontar enquanto o sapo disser que ainda agarra, e isso incluiria
        // esta desmontagem. O motivo UNLOAD e o generico de saida externa: o sapo
        // nao diferencia as consequencias, e inventar um motivo por chamador aqui
        // seria detalhe sem consumidor.
        if (agarrao.agarrando()) agarrao.soltar(GrabRelease.UNLOAD);
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

    // ------------------------------------------------------------- animacao

    /**
     * O CLIENTE NAO DECIDE NADA. Ele le o que o servidor ja publica --
     * {@link #estaEnterrado()} e {@link #faseDeAtaque()}, os dois vindos do
     * SynchedEntityData -- e escolhe o clipe correspondente. Nao existe aqui
     * nenhum timer, nenhuma heuristica de "parece que vai botar" e nenhuma copia
     * da regra de emboscada. Se a animacao e a hitbox discordarem, quem esta
     * errado e o arquivo de animacao, nunca o servidor.
     *
     * <p>Ordem de precedencia, da mais especifica para a menos: toca, telegrafo,
     * mordida, digestao, locomocao, ocio. A toca vem primeiro porque enterrado o
     * sapo nao tem corpo visivel para andar nem para ficar ocioso.</p>
     *
     * <p>A DIGESTAO E LIDA PELA FASE, e nao por {@link #estaAgarrando()}. Aquele
     * metodo le {@code vitimaAgarrada}, um campo de SERVIDOR: no cliente ele
     * responde {@code false} sempre, e usa-lo aqui faria a digestao nunca tocar
     * para quem estiver assistindo -- sem erro nenhum no log. A fase RECOVERY e
     * publicada justamente enquanto o sapo digere.</p>
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<FrogInWaitingEntity>(this, CONTROLLER_DO_CORPO,
                TRANSICAO_EM_TICKS, this::clipeDoCorpo));
    }

    private PlayState clipeDoCorpo(AnimationState<FrogInWaitingEntity> estado) {
        if (estaEnterrado()) return estado.setAndContinue(BURROWED);
        AttackPhase fase = faseDeAtaque();
        if (fase == AttackPhase.WINDUP) return estado.setAndContinue(EMERGE);
        if (fase == AttackPhase.ACTIVE) return estado.setAndContinue(BITE);
        if (fase == AttackPhase.RECOVERY) return estado.setAndContinue(DIGEST);
        if (estado.isMoving()) return estado.setAndContinue(WALK);
        return estado.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cacheDeAnimacao; }

    @Override
    public double getTick(Object entidade) { return this.tickCount; }

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
