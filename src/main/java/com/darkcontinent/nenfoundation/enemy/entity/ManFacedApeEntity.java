package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessInput;
import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.DisguiseRules;
import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.base.BaseHxHMob;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import java.util.EnumSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Man-faced ape: a ameaca e o ENGANO, e a resposta do jogador e reconhecer a
 * inconsistencia antes da emboscada.
 *
 * <p>DECISAO CENTRAL QUE ESTE ARQUIVO CARREGA: a inconsistencia e OBSERVAVEL e
 * tem uma contrapartida ensinavel. Enquanto disfarcado o macaco so se aproxima
 * quando ninguem esta olhando para ele -- encarado, ele PARA, e fica parado. Ele
 * nao finge um passeio aleatorio nem disfarca a pausa, porque a pausa e a pista:
 * e exatamente isso que o jogador precisa aprender a ler. A contrapartida e uma
 * frase: nao tire os olhos dele.</p>
 *
 * <p>SEGUNDA DECISAO: quando o disfarce cai, cai o do BANDO junto. Um macaco
 * revelado chama todo disfarcado dentro do raio do bando e passa o mesmo alvo --
 * a forca do mob e engano e numeros, e revelar um por vez devolveria ao jogador
 * uma briga de 1 contra 1 que ele ja ganhou.</p>
 *
 * <p>Tudo que decide -- se esta sendo observado, quando o disfarce cai, quem e
 * chamado e quando o golpe acontece -- roda no servidor. Os dois campos
 * sincronizados carregam apenas DISFARCADO e a FASE do ataque, que e o que o
 * cliente precisa para desenhar o disfarce e o bote. O cliente nunca informa
 * olhar, acerto nem revelacao.</p>
 */
public final class ManFacedApeEntity extends BaseHxHMob {
    /** Disfarcado ou nao; o cliente precisa saber para desenhar gente ou macaco. */
    private static final EntityDataAccessor<Boolean> DISFARCADO =
            SynchedEntityData.defineId(ManFacedApeEntity.class, EntityDataSerializers.BOOLEAN);
    /** Fase do ataque; o unico estado de combate que o cliente precisa conhecer. */
    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(ManFacedApeEntity.class, EntityDataSerializers.INT);

    private static final DisguiseRules DISFARCE = HunterExamProfiles.manFacedApeDisguise();

    /** Quanto tempo o macaco lembra de um alvo que saiu da linha de visao. */
    private static final int MEMORIA_DE_ALVO_TICKS = 100;
    /** Quantos ticks de aviso antes de o cerebro passar de WARN para ENGAGE. */
    private static final int TICKS_DE_AVISO = 20;
    /** Afastar-se mais do que isto num tick conta como recuo. */
    private static final double TOLERANCIA_DE_RECUO = 0.35D;
    /** Fracao da vida abaixo da qual o cerebro pode decidir fugir. */
    private static final float FRACAO_DE_VIDA_CRITICA = 0.3F;
    /**
     * Ate aqui o bote ja e o proximo passo -- e isto que "alvo perto" quer dizer
     * quando o sensor oferece oportunidade de emboscada ao cerebro. Nao e botao
     * de tuning: e o limite a partir do qual a aproximacao disfarcada deixou de
     * ser passeio e virou caca.
     */
    private static final double RAIO_DE_OPORTUNIDADE = 8.0D;
    /**
     * Quanto tempo SEM ALVO o macaco revelado espera antes de voltar ao disfarce.
     *
     * <p>Nao e botao de balanceamento: e o minimo para o retorno ser honesto.
     * Voltar depressa demais deixaria o macaco virar paisagem de novo perto de
     * quem acabou de ve-lo revelado, e o jogador aprenderia que a pista mente.</p>
     */
    private static final int TICKS_PARA_VOLTAR_AO_DISFARCE = 200;
    /** Recalcular caminho todo tick e desperdicio; quatro vezes por segundo basta. */
    private static final int TICKS_ENTRE_RECALCULOS = 5;
    private static final double VELOCIDADE_DE_APROXIMACAO = 1.0D;
    private static final double VELOCIDADE_DE_FUGA = 1.3D;
    private static final int RAIO_DA_FUGA = 16;
    private static final int ALTURA_DA_FUGA = 7;

    // Estado DA INSTANCIA. Guardar qualquer um destes numa Goal (ou num static)
    // faria todos os macacos do mundo compartilharem a mesma revelacao -- a Goal e
    // uma por entidade, e as classes aninhadas abaixo sao static justamente para
    // nao deixar essa dependencia escondida.
    private int revelacaoRestante;
    private int ticksSemAlvo;
    private boolean observadoNesteTick;

    // Sensores do cerebro.
    private int memoriaDeAlvo;
    private double distanciaAnteriorDoAlvo = Double.NaN;

    public ManFacedApeEntity(EntityType<? extends ManFacedApeEntity> type, Level level) {
        super(type, level, HunterExamProfiles.manFacedApe().metadata(),
                new AwarenessTuning(TICKS_DE_AVISO, MEMORIA_DE_ALVO_TICKS));
        // Nasce disfarcado e CALADO: um bicho que grunhe enquanto finge ser gente
        // ja se entregou antes de o jogador ter o que observar.
        setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = HunterExamProfiles.manFacedApe().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<ManFacedApeEntity> registeredType() {
        return EnemyEntityTypes.MAN_FACED_APE.get();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DISFARCADO, Boolean.TRUE);
        builder.define(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
    }

    /** Disfarcado; vale nos dois lados, porque vem do SynchedEntityData. */
    public boolean estaDisfarcado() { return this.entityData.get(DISFARCADO); }

    /** Fase corrente do ataque; vale nos dois lados, pelo mesmo motivo. */
    public AttackPhase faseDeAtaque() {
        int ordinal = this.entityData.get(FASE_DE_ATAQUE);
        AttackPhase[] fases = AttackPhase.values();
        return ordinal >= 0 && ordinal < fases.length ? fases[ordinal] : AttackPhase.IDLE;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FugaGoal(this));
        goalSelector.addGoal(2, new AproximacaoDisfarcadaGoal(this));
        goalSelector.addGoal(3, new AtaqueCorpoACorpoGoal(this));
        goalSelector.addGoal(6, new PasseioSemAlvoGoal(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;

        LivingEntity alvo = alvoValido();
        // A observacao e medida UMA vez por tick, aqui, e todo mundo le o mesmo
        // valor. Medir de novo dentro da Goal daria duas respostas para a mesma
        // pergunta no mesmo tick -- e a divergencia apareceria como um macaco que
        // anda meio bloco enquanto encarado.
        observadoNesteTick = DISFARCE.observado(alvo != null && hasLineOfSight(alvo),
                cossenoDoOlharDoAlvo(alvo));

        if (revelacaoRestante > 0) tickDaRevelacao();
        else if (estaDisfarcado()) tentarRevelarPorDistancia(alvo);
        else tentarVoltarAoDisfarce(alvo);

        EnemyAwarenessState consciencia = enemyBrain().tick(lerSensores(alvo));
        alinharEstadoDeCombate(consciencia);
    }

    /**
     * Sofrer dano revela -- e o SEGUNDO caminho de {@link DisguiseRules#revela}.
     *
     * <p>Este metodo NAO multiplica dano nenhum: o macaco nao tem ponto fraco, e
     * o corpo fragil (24 de vida, armadura 1) ja faz esse trabalho pelo perfil.
     * Um multiplicador aqui seria o segundo handler que este projeto ja sabe que
     * comete -- o numero final fica plausivel demais para alguem notar sem
     * medir.</p>
     *
     * <p>Quem morre do golpe NAO chama o bando: matar um disfarcado de primeira e
     * o premio de quem leu a pista antes da emboscada, e tirar esse premio seria
     * punir justamente a resposta que o mob ensina.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean aplicou = super.hurt(source, amount);
        if (!aplicou || level().isClientSide || !isAlive() || !estaDisfarcado()) return aplicou;
        if (!DISFARCE.revela(true, distanciaAte(getTarget()), true)) return aplicou;
        LivingEntity culpado = source.getEntity() instanceof LivingEntity vivo ? vivo : getTarget();
        revelar(culpado, true);
        return aplicou;
    }

    @Override
    public void die(DamageSource source) {
        // Quem liga, desliga: morrer no meio do telegrafo nao pode deixar a fase
        // presa em WINDUP para o cliente que ainda ve o corpo cair.
        if (!level().isClientSide) {
            combatState(EnemyCombatState.DYING);
            encerrarEpisodio();
        }
        super.die(source);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide) encerrarEpisodio();
        super.remove(reason);
    }

    // -------------------------------------------------------------- disfarce

    /**
     * UNICO metodo que LIGA o disfarce.
     *
     * <p>O retorno ao disfarce depois da fuga passa por aqui, e nao por um
     * segundo caminho: duas maneiras de ficar disfarcado divergiriam em silencio
     * (uma esquece de largar o alvo, outra esquece de zerar a fase) e o macaco
     * voltaria a ser paisagem ainda perseguindo alguem.</p>
     */
    private void ligarDisfarce() {
        this.entityData.set(DISFARCADO, Boolean.TRUE);
        setSilent(true);
        setTarget(null);
        revelacaoRestante = 0;
        ticksSemAlvo = 0;
        observadoNesteTick = false;
        getNavigation().stop();
        publicarFase(AttackPhase.IDLE);
        combatState(estadoOcioso());
    }

    /**
     * UNICO metodo que DERRUBA o disfarce, e o unico que chama o bando.
     *
     * <p>{@code chamaOBando} e a guarda contra recursao: quem foi chamado nao
     * chama de volta. A segunda guarda e a saida de cima -- quem ja esta revelado
     * nao revela de novo -- e ela sozinha ja bastaria, mas as duas juntas fazem o
     * ciclo impossivel em vez de improvavel.</p>
     */
    private void revelar(LivingEntity alvo, boolean chamaOBando) {
        if (level().isClientSide || !estaDisfarcado()) return;
        this.entityData.set(DISFARCADO, Boolean.FALSE);
        setSilent(false);
        if (alvo != null && alvo.isAlive()) setTarget(alvo);
        ticksSemAlvo = 0;
        // O telegrafo e o UNICO aviso que o jogador recebe antes do bote.
        revelacaoRestante = DISFARCE.ticksDeReveal();
        getNavigation().stop();
        combatState(EnemyCombatState.WINDUP);
        publicarFase(AttackPhase.WINDUP);
        if (chamaOBando) chamarOBando(alvo);
    }

    /** Todo disfarcado dentro do raio revela junto e herda o mesmo alvo. */
    private void chamarOBando(LivingEntity alvo) {
        double raio = DISFARCE.raioDoBando();
        AABB volta = getBoundingBox().inflate(raio);
        for (ManFacedApeEntity irmao : level().getEntitiesOfClass(ManFacedApeEntity.class, volta,
                outro -> outro != this && outro.isAlive() && outro.estaDisfarcado())) {
            // A caixa e um cubo; o bando e uma esfera. Sem esta linha o macaco do
            // canto do cubo (raio x 1,7) revelaria junto, e o raio declarado no
            // perfil viraria um numero que nao descreve nada.
            if (irmao.distanceTo(this) > raio) continue;
            irmao.revelar(alvo, false);
        }
    }

    /** O telegrafo: parado, encarando, sem bater. Quem bate espera ele acabar. */
    private void tickDaRevelacao() {
        revelacaoRestante--;
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
        LivingEntity alvo = getTarget();
        if (alvo != null) getLookControl().setLookAt(alvo, 30.0F, 30.0F);
        combatState(EnemyCombatState.WINDUP);
        if (revelacaoRestante > 0) return;
        publicarFase(AttackPhase.IDLE);
        combatState(getTarget() != null ? EnemyCombatState.AGGRO : estadoOcioso());
    }

    private void tentarRevelarPorDistancia(LivingEntity alvo) {
        if (!DISFARCE.revela(estaDisfarcado(), distanciaAte(alvo), false)) return;
        revelar(alvo, true);
    }

    /**
     * O macaco so volta ao disfarce SEM ALVO e depois da espera.
     *
     * <p>O {@code temAlvo} aqui e o que impede a fuga de terminar em disfarce na
     * cara de quem o perseguiu: enquanto houver alvo vivo, o contador nao anda.
     * Reaparecer como gente na frente de quem acabou de ver o macaco ensinaria
     * ao jogador que a pista mente.</p>
     */
    private void tentarVoltarAoDisfarce(LivingEntity alvo) {
        boolean temAlvo = alvo != null;
        ticksSemAlvo = temAlvo ? 0 : ticksSemAlvo + 1;
        if (temAlvo || ticksSemAlvo < TICKS_PARA_VOLTAR_AO_DISFARCE) return;
        ligarDisfarce();
    }

    /**
     * Limpeza UNICA do episodio. Morte e remocao passam por aqui.
     *
     * <p>O macaco nao deixa estado fora de si (nao agarra, nao carrega ninguem,
     * nao cria projetil), entao a limpeza e curta de proposito -- mas ela existe
     * num lugar so para que o proximo campo nasca limpo junto, em vez de ganhar
     * um {@code if} novo em cada ponto de saida.</p>
     */
    private void encerrarEpisodio() {
        revelacaoRestante = 0;
        ticksSemAlvo = 0;
        observadoNesteTick = false;
        memoriaDeAlvo = 0;
        distanciaAnteriorDoAlvo = Double.NaN;
        getNavigation().stop();
        publicarFase(AttackPhase.IDLE);
    }

    private void publicarFase(AttackPhase fase) {
        this.entityData.set(FASE_DE_ATAQUE, fase.ordinal());
    }

    // ------------------------------------------------------------ geometria

    /**
     * Cosseno entre o olhar horizontal DO ALVO e a direcao horizontal
     * alvo-&gt;macaco. 1 e olhar em cheio, 0 e de lado, -1 e de costas.
     *
     * <p>Medido no SERVIDOR, com a rotacao que o servidor tem. Sem alvo, ou com
     * o alvo exatamente em cima do macaco, o vetor nao existe e a resposta e
     * NaN -- que {@link DisguiseRules#observado} reprova de proposito, em vez de
     * virar um zero que significaria "nao esta olhando".</p>
     */
    private double cossenoDoOlharDoAlvo(LivingEntity alvo) {
        if (alvo == null) return Double.NaN;
        Vec3 olhar = alvo.getLookAngle();
        Vec3 olharHorizontal = new Vec3(olhar.x, 0.0D, olhar.z);
        Vec3 ateOMacaco = new Vec3(getX() - alvo.getX(), 0.0D, getZ() - alvo.getZ());
        if (olharHorizontal.lengthSqr() < 1.0E-6D || ateOMacaco.lengthSqr() < 1.0E-6D) {
            return Double.NaN;
        }
        return Mth.clamp(olharHorizontal.normalize().dot(ateOMacaco.normalize()), -1.0D, 1.0D);
    }

    /** Alvo ausente vira NaN, e NaN nao revela nada. */
    private double distanciaAte(LivingEntity alvo) {
        return alvo == null || !alvo.isAlive() ? Double.NaN : distanceTo(alvo);
    }

    /** O alvo do tick, ja filtrado por vivo; null e "nao tenho alvo". */
    private LivingEntity alvoValido() {
        LivingEntity alvo = getTarget();
        return alvo != null && alvo.isAlive() ? alvo : null;
    }

    // -------------------------------------------------------------- sensores

    /** Leitura de mundo do tick: e isto que faz awarenessState() parar de ser decorativo. */
    private AwarenessInput lerSensores(LivingEntity alvo) {
        boolean alvoVivo = alvo != null;
        boolean visivel = alvoVivo && hasLineOfSight(alvo);
        double distancia = alvoVivo ? distanceTo(alvo) : Double.NaN;

        if (visivel) memoriaDeAlvo = MEMORIA_DE_ALVO_TICKS;
        else if (memoriaDeAlvo > 0) memoriaDeAlvo--;

        boolean recuando = visivel && !Double.isNaN(distanciaAnteriorDoAlvo)
                && distancia > distanciaAnteriorDoAlvo + TOLERANCIA_DE_RECUO;
        distanciaAnteriorDoAlvo = distancia;

        boolean audivel = !visivel && memoriaDeAlvo > 0;
        boolean vidaCritica = getHealth() <= getMaxHealth() * FRACAO_DE_VIDA_CRITICA;

        // AQUI o ambushOpportunity deixa de ser papel: disfarcado, com alvo perto, e
        // SEM estar sendo observado. Encarado, o macaco nao tem emboscada nenhuma a
        // oferecer -- ele esta parado fingindo ser gente.
        boolean emboscando = estaDisfarcado() && visivel && !observadoNesteTick
                && distancia <= RAIO_DE_OPORTUNIDADE;

        // territorial=false no perfil: o macaco nao defende lugar nenhum, entao ele
        // nunca AVISA. Sem territorio o cerebro vai de SUSPICIOUS para STALK, que e
        // o caminho certo para um emboscador -- dizer "no territorio" aqui o faria
        // rosnar um aviso que o mob inteiro existe para nao dar.
        return new AwarenessInput(visivel, audivel, false, recuando, !alvoVivo,
                memoriaDeAlvo, emboscando, vidaCritica);
    }

    /** O telegrafo manda; fora dele quem manda e a consciencia. */
    private void alinharEstadoDeCombate(EnemyAwarenessState consciencia) {
        if (revelacaoRestante > 0 || combatState() == EnemyCombatState.DYING) return;
        if (consciencia == EnemyAwarenessState.FLEE) {
            combatState(EnemyCombatState.RETREAT);
            return;
        }
        if (estaDisfarcado()) {
            combatState(estadoOcioso());
            return;
        }
        combatState(getTarget() != null ? EnemyCombatState.AGGRO : estadoOcioso());
    }

    private EnemyCombatState estadoOcioso() {
        return enemyMetadata().social() ? EnemyCombatState.GROUP_ROAM : EnemyCombatState.IDLE;
    }

    /** Disfarcado, telegrafando ou fugindo, o corpo nao esta livre para bater. */
    private boolean prontoParaBater() {
        return !estaDisfarcado() && revelacaoRestante == 0
                && awarenessState() != EnemyAwarenessState.FLEE;
    }

    // ------------------------------------------------------------------ goals

    /**
     * A aproximacao disfarcada. E static de proposito: todo estado que ela le e
     * escreve mora na ENTIDADE, e o prefixo {@code macaco.} deixa isso impossivel
     * de esquecer. Um campo aqui seria compartilhado por todos os macacos.
     */
    private static final class AproximacaoDisfarcadaGoal extends Goal {
        private final ManFacedApeEntity macaco;
        private int recalculoRestante;

        private AproximacaoDisfarcadaGoal(ManFacedApeEntity macaco) {
            this.macaco = macaco;
            // LOOK junto de MOVE: disfarcado, esta Goal e a UNICA autoridade sobre a
            // cabeca. Sem isto o LookAtPlayerGoal escreveria no mesmo look control no
            // mesmo tick, e "para quem ele esta olhando" teria duas fontes.
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (macaco.level().isClientSide || !macaco.estaDisfarcado()) return false;
            if (macaco.awarenessState() == EnemyAwarenessState.FLEE) return false;
            return macaco.alvoValido() != null;
        }

        /**
         * SEGURA O MOVE MESMO PARADO.
         *
         * <p>Devolver false enquanto encarado pareceria mais limpo e quebraria o
         * mob inteiro: o passeio aleatorio (prioridade menor) herdaria o MOVE no
         * mesmo tick e o macaco sairia caminhando pela selva justamente enquanto o
         * jogador o encara -- que e a unica hora em que ele PRECISA estar parado.
         * A pista sumiria sem erro nenhum no log.</p>
         */
        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() { recalculoRestante = 0; }

        @Override public void tick() {
            LivingEntity alvo = macaco.alvoValido();
            if (alvo == null) return;
            macaco.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            if (!DISFARCE.podeAproximar(macaco.estaDisfarcado(), macaco.observadoNesteTick)) {
                macaco.getNavigation().stop();
                macaco.setDeltaMovement(macaco.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
                recalculoRestante = 0;
                return;
            }
            if (recalculoRestante > 0) {
                recalculoRestante--;
                return;
            }
            recalculoRestante = TICKS_ENTRE_RECALCULOS;
            macaco.getNavigation().moveTo(alvo, VELOCIDADE_DE_APROXIMACAO);
        }

        @Override public void stop() { macaco.getNavigation().stop(); }
    }

    /**
     * Fuga com vida critica, decidida pelo {@link EnemyAwarenessState#FLEE} do
     * cerebro -- e nao por um segundo limiar de vida escrito aqui.
     */
    private static final class FugaGoal extends Goal {
        private final ManFacedApeEntity macaco;
        private double destinoX;
        private double destinoY;
        private double destinoZ;

        private FugaGoal(ManFacedApeEntity macaco) {
            this.macaco = macaco;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override public boolean canUse() {
            if (macaco.level().isClientSide || macaco.estaDisfarcado()) return false;
            if (macaco.revelacaoRestante > 0) return false;
            if (macaco.awarenessState() != EnemyAwarenessState.FLEE) return false;
            LivingEntity alvo = macaco.alvoValido();
            return alvo != null && procurarDestino(alvo);
        }

        @Override public boolean canContinueToUse() {
            return macaco.awarenessState() == EnemyAwarenessState.FLEE
                    && !macaco.getNavigation().isDone();
        }

        @Override public void start() {
            macaco.getNavigation().moveTo(destinoX, destinoY, destinoZ, VELOCIDADE_DE_FUGA);
        }

        @Override public void stop() { macaco.getNavigation().stop(); }

        private boolean procurarDestino(LivingEntity alvo) {
            Vec3 destino = DefaultRandomPos.getPosAway(macaco, RAIO_DA_FUGA, ALTURA_DA_FUGA,
                    alvo.position());
            if (destino == null) return false;
            destinoX = destino.x;
            destinoY = destino.y;
            destinoZ = destino.z;
            return true;
        }
    }

    /** Corpo a corpo comum -- e ele SO existe revelado, depois do telegrafo. */
    private static final class AtaqueCorpoACorpoGoal extends MeleeAttackGoal {
        private final ManFacedApeEntity macaco;

        private AtaqueCorpoACorpoGoal(ManFacedApeEntity macaco) {
            super(macaco, 1.0D, true);
            this.macaco = macaco;
        }

        @Override public boolean canUse() { return macaco.prontoParaBater() && super.canUse(); }

        @Override public boolean canContinueToUse() {
            return macaco.prontoParaBater() && super.canContinueToUse();
        }
    }

    /**
     * Passear so vale SEM ALVO.
     *
     * <p>Com alvo, quem manda no corpo e a aproximacao disfarcada (que para
     * quando encarado) ou o ataque. Deixar o passeio rodar com alvo devolveria o
     * "fingir passeio aleatorio" pela porta dos fundos.</p>
     */
    private static final class PasseioSemAlvoGoal extends WaterAvoidingRandomStrollGoal {
        private final ManFacedApeEntity macaco;

        private PasseioSemAlvoGoal(ManFacedApeEntity macaco) {
            super(macaco, 0.8D);
            this.macaco = macaco;
        }

        private boolean livre() {
            return macaco.alvoValido() == null && macaco.revelacaoRestante == 0;
        }

        @Override public boolean canUse() { return livre() && super.canUse(); }

        @Override public boolean canContinueToUse() { return livre() && super.canContinueToUse(); }
    }
}
