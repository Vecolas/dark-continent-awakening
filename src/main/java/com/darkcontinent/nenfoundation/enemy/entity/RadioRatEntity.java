package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.base.BaseHxHMob;
import com.darkcontinent.nenfoundation.enemy.base.EnemyRuntime;
import com.darkcontinent.nenfoundation.enemy.combat.AttackController;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHit;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import com.darkcontinent.nenfoundation.enemy.content.GreedIslandProfiles;
import com.darkcontinent.nenfoundation.enemy.content.RadioRatTuning;
import com.darkcontinent.nenfoundation.enemy.faction.FactionRelations;
import com.darkcontinent.nenfoundation.enemy.perception.HearingEvent;
import com.darkcontinent.nenfoundation.enemy.perception.OuvidoDeInimigo;
import com.darkcontinent.nenfoundation.enemy.perception.PercepcaoDeAura;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionBudget;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionController;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionSnapshot;
import com.darkcontinent.nenfoundation.enemy.perception.RadioRatReportRules;
import com.darkcontinent.nenfoundation.enemy.perception.TargetCandidate;
import com.darkcontinent.nenfoundation.enemy.perception.TargetEvaluator;
import com.darkcontinent.nenfoundation.enemy.perception.ThreatMemory;
import com.darkcontinent.nenfoundation.enemy.perception.VisionCone;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Radio Rat -- criatura de Greed Island.
 *
 * <p>O rato que DENUNCIA. Ele nao machuca quase nada; o que ele faz e contar aos outros onde voce esta, e matar o mensageiro e a resposta.</p>
 *
 * <p><b>O que ele faz, em ordem.</b> Ele percebe com orcamento, como todo inimigo
 * desta fundacao. Passados {@link RadioRatTuning#TICKS_DE_OBSERVACAO} ticks com
 * alvo, ele empina e GRITA: um golpe telegrafado de verdade, com windup, janela
 * ativa e recuperacao. Na janela ativa duas coisas acontecem -- o relatorio sai
 * para os vizinhos que declararam ouvido, e quem estiver colado nele leva a
 * mordida de 2. Fora dessa janela ele nao faz nada: nao persegue, nao cerca, nao
 * revida.</p>
 *
 * <p><b>O relatorio e inteiramente SERVER-SIDE, e isso e a regra e nao um
 * detalhe.</b> Nao ha payload, nao ha campo sincronizado novo, o cliente nao fica
 * sabendo de nada. Informacao escondida que viaja ate o cliente para ele filtrar
 * ja esta vazada: basta um cliente modificado parar de filtrar. Aqui o aviso
 * nasce, viaja e morre dentro do servidor -- ele vira um {@link HearingEvent}
 * entregue ao {@link PerceptionController} de cada vizinho, que continua decidindo
 * com as regras dele.</p>
 *
 * <p><b>Por que som e nao alvo.</b> Um {@code setTarget} direto no vizinho seria
 * uma segunda autoridade sobre a escolha de alvo, discordando do
 * {@link TargetEvaluator} em silencio. Como som, o aviso passa pelo alcance de
 * audicao de quem ouve: um mob longe demais do jogador escuta o grito e nao
 * aprende nada. O rato aponta; ele nao decide pelos outros.</p>
 *
 * <p><b>A conta do custo.</b> A varredura de vizinhos e cara, e por isso ela so
 * roda uma vez por grito -- guardada pelo id da instancia de ataque, e nao por um
 * booleano que algum ponto de saida esqueceria de limpar. Entre dois gritos ha, no
 * minimo, o telegrafo inteiro mais a recarga do perfil. Varrer todo tick nao daria
 * erro nenhum: daria TPS caindo devagar num mundo com muitos ratos.</p>
 *
 * <p><b>Interromper funciona, e e para isso que o windup e longo.</b> Um golpe
 * solido durante os ticks de telegrafo dispara o stagger, que corta o ataque e
 * impoe {@link RadioRatTuning#RECARGA_APOS_INTERRUPCAO} -- o relatorio nao sai.
 * Com 10 de vida, matar tambem e uma resposta. As duas sao a mesma frase: o
 * mensageiro tem de ser tratado antes do grito.</p>
 */
public final class RadioRatEntity extends BaseHxHMob
        implements software.bernie.geckolib.animatable.GeoEntity, OuvidoDeInimigo {

    /** Fase do ataque: o unico estado de combate que o cliente precisa conhecer. */
    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(RadioRatEntity.class, EntityDataSerializers.INT);
    /** Cambaleando: sincronizado porque interrupcao invisivel e stagger que so o servidor conhece. */
    private static final EntityDataAccessor<Boolean> CAMBALEANDO =
            SynchedEntityData.defineId(RadioRatEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int MEMORIA_DE_ALVO_TICKS = 120;
    private static final int TICKS_DE_AVISO = 20;
    private static final double ABERTURA_DA_VISAO = 75.0D;
    private static final double ALCANCE_DE_AUDICAO = 14.0D;
    private static final float FRACAO_DE_VIDA_CRITICA = 0.25F;

    /**
     * A mordida do grito, em coordenadas LOCAIS. A FRENTE E +Z.
     *
     * <p>+Z, e nao -Z: -Z e a frente da GEOMETRIA do modelo (formato Bedrock), e
     * esta caixa passa por {@link AttackHitbox#noMundo}, que e matematica de
     * mundo -- com yaw 0 o olhar vanilla aponta para +Z. Trocar os dois poe a
     * caixa ATRAS do rato: ele grita, anima, e morde quem estiver pelas costas.
     * Nao ha erro nenhum nisso; foi um gametest do boneco de treino que achou a
     * primeira vez, e a coerencia entre os mobs e o que impede a proxima
     * inversao.</p>
     *
     * <p>Ela e pequena de proposito -- pouco menos de um bloco a frente, meio
     * bloco de altura. A mordida existe para o jogador nao encostar de graca no
     * mensageiro, e nao para o rato disputar espaco com quem chegou perto.</p>
     */
    private static final AttackHitbox CAIXA_DA_MORDIDA =
            new AttackHitbox(-0.35D, 0.0D, 0.0D, 0.35D, 0.6D, 0.9D);

    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache cacheDeAnimacao =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    /**
     * Ha quantos ticks seguidos existe alvo. Ele PARA de crescer no limiar.
     *
     * <p>Parar no limiar nao e economia: e o que impede um contador de tick de
     * servidor virar um inteiro que estoura depois de tempo suficiente ligado. Um
     * overflow aqui vira numero negativo, a comparacao do limiar passa a ser falsa
     * e o rato para de gritar -- para sempre, sem uma linha de log.</p>
     */
    private int ticksComAlvo;

    /**
     * A ultima instancia de grito que ja virou relatorio.
     *
     * <p>A janela ACTIVE dura quatro ticks; sem esta marca, os quatro varreriam o
     * mundo e o aviso sairia quatro vezes. Nao daria erro: daria quatro vezes o
     * custo e uma memoria de ameaca recarregada de graca. O id de instancia vem do
     * {@link AttackController} e e o mesmo que o stagger usa para nao contar o
     * mesmo golpe duas vezes -- a mesma defesa, pelo mesmo motivo.</p>
     */
    private long ultimaInstanciaRelatada;

    public RadioRatEntity(EntityType<? extends RadioRatEntity> type, Level level) {
        super(type, level, GreedIslandProfiles.radioRat().metadata(),
                new AwarenessTuning(TICKS_DE_AVISO, MEMORIA_DE_ALVO_TICKS));
        instalarRuntime(montarRuntime());
    }

    private EnemyRuntime montarRuntime() {
        var perfil = GreedIslandProfiles.radioRat();
        PerceptionController percepcao = new PerceptionController(
                PerceptionBudget.padrao(),
                VisionCone.deGraus(perfil.attributes().followRange(), ABERTURA_DA_VISAO),
                new ThreatMemory(MEMORIA_DE_ALVO_TICKS),
                new TargetEvaluator(FactionRelations.padrao(), perfil.metadata().faction(),
                        perfil.attributes().followRange(), perfil.metadata().territorial()),
                PercepcaoDeAura.NENHUMA, ALCANCE_DE_AUDICAO, getUUID().hashCode());
        return new EnemyRuntime(percepcao,
                new EnemyBrain(new AwarenessTuning(TICKS_DE_AVISO, MEMORIA_DE_ALVO_TICKS)),
                new AttackController(GreedIslandProfiles.radioRatRecarga()),
                new StaggerState(GreedIslandProfiles.radioRatStagger()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = GreedIslandProfiles.radioRat().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<RadioRatEntity> registeredType() { return EnemyEntityTypes.RADIO_RAT.get(); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        builder.define(CAMBALEANDO, false);
    }

    public AttackPhase faseDeAtaque() {
        int ordinal = this.entityData.get(FASE_DE_ATAQUE);
        AttackPhase[] fases = AttackPhase.values();
        return ordinal >= 0 && ordinal < fases.length ? fases[ordinal] : AttackPhase.IDLE;
    }

    public boolean cambaleando() { return this.entityData.get(CAMBALEANDO); }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new GritoGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // NENHUM target goal vanilla: quem escolhe alvo aqui e o TargetEvaluator,
        // por faccao. Um NearestAttackableTargetGoal ao lado seria uma segunda
        // autoridade sobre a mesma decisao, e as duas discordariam em silencio.
        //
        // E NENHUMA goal de perseguicao, tambem de proposito: este bicho e um
        // sensor com patas. Um rato que corre atras do jogador vira um mob de
        // combate ruim -- 2 de dano nao ameaca ninguem -- e o encounter deixa de
        // ser sobre calar o mensageiro para virar sobre matar um inseto chato.
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;
        EnemyRuntime runtime = runtimeExigido();
        PerceptionSnapshot snapshot = runtime.tick(tickCount, this::varrerCandidatos,
                getHealth() <= getMaxHealth() * FRACAO_DE_VIDA_CRITICA, false);
        aplicarAlvo(snapshot);
        tickDoGrito(runtime);
        publicarEstado(runtime);
    }

    private void aplicarAlvo(PerceptionSnapshot snapshot) {
        Optional<UUID> lembrado = snapshot.alvoOpcional();
        if (lembrado.isEmpty()) {
            if (getTarget() != null) setTarget(null);
            return;
        }
        Entity encontrado = ((ServerLevel) level()).getEntity(lembrado.get());
        if (encontrado instanceof LivingEntity vivo && vivo.isAlive()) {
            if (getTarget() != vivo) setTarget(vivo);
        } else if (getTarget() != null) {
            setTarget(null);
        }
    }

    /** Varredura de mundo da PERCEPCAO -- a unica, e so quando o orcamento autoriza. */
    private List<TargetCandidate> varrerCandidatos() {
        double alcance = getAttributeValue(Attributes.FOLLOW_RANGE);
        AABB area = getBoundingBox().inflate(alcance);
        List<TargetCandidate> candidatos = new ArrayList<>();
        for (Player jogador : level().getEntitiesOfClass(Player.class, area,
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative())) {
            candidatos.add(new TargetCandidate(jogador.getUUID(),
                    com.darkcontinent.nenfoundation.enemy.api.EnemyFaction.HUNTER_ASSOCIATION,
                    distanceTo(jogador), cossenoDoOlharAte(jogador), hasLineOfSight(jogador),
                    true, true, jogador == getLastHurtByMob()));
        }
        return candidatos;
    }

    private double cossenoDoOlharAte(Entity alvo) {
        Vec3 olhar = getLookAngle();
        Vec3 horizontal = new Vec3(olhar.x, 0.0D, olhar.z);
        Vec3 ateOAlvo = new Vec3(alvo.getX() - getX(), 0.0D, alvo.getZ() - getZ());
        if (horizontal.lengthSqr() < 1.0E-6D || ateOAlvo.lengthSqr() < 1.0E-6D) return 0.0D;
        return Mth.clamp(horizontal.normalize().dot(ateOAlvo.normalize()), -1.0D, 1.0D);
    }

    // -------------------------------------------------------------- o grito

    /** Conta o tempo com alvo e conduz a janela ativa: relatorio primeiro, mordida depois. */
    private void tickDoGrito(EnemyRuntime runtime) {
        LivingEntity alvo = getTarget();
        boolean temAlvo = alvo != null && alvo.isAlive();
        if (!temAlvo) {
            ticksComAlvo = 0;
        } else if (ticksComAlvo < RadioRatTuning.TICKS_DE_OBSERVACAO) {
            ticksComAlvo++;
        }
        if (!temAlvo || runtime.ataques().phase() != AttackPhase.ACTIVE) return;
        emitirRelatorio(runtime, alvo);
        aplicarMordida(runtime, alvo);
    }

    /**
     * O relatorio: uma varredura, uma vez por grito, e nada sai do servidor.
     *
     * <p>A distancia que viaja em cada evento e a do VIZINHO ao alvo -- medida
     * aqui, entidade por entidade. Mandar a distancia do RATO ao alvo seria mais
     * barato e encheria a memoria de ameaca dos vizinhos com um numero que nao
     * descreve a situacao deles: um mob do outro lado do vale acharia o jogador a
     * dois blocos. Nada acusaria isso.</p>
     */
    private void emitirRelatorio(EnemyRuntime runtime, LivingEntity alvo) {
        long instancia = runtime.ataques().attackInstanceId();
        if (instancia == 0L || instancia == ultimaInstanciaRelatada) return;
        ultimaInstanciaRelatada = instancia;

        RadioRatReportRules regras = RadioRatTuning.relatorio();
        AABB area = getBoundingBox().inflate(regras.raioDoRelatorio());
        UUID denunciado = alvo.getUUID();
        for (Mob vizinho : level().getEntitiesOfClass(Mob.class, area,
                outro -> outro != this && outro.isAlive() && outro instanceof OuvidoDeInimigo)) {
            // A AABB e o filtro barato; o raio redondo e a REGRA. Sem esta linha o
            // grito alcancaria os cantos da caixa, que ficam 1.7x mais longe que a
            // face dela -- alcance maior que o numero declarado, e so em quatro
            // direcoes. Isso nao da erro: da um raio que ninguem consegue medir.
            if (!regras.alcanca(distanceTo(vizinho))) continue;
            regras.relatorio(denunciado, vizinho.distanceTo(alvo))
                    .ifPresent(((OuvidoDeInimigo) vizinho)::ouvir);
        }
    }

    /** A mordida so existe dentro da janela ACTIVE; fora dela ela nao e nada. */
    private void aplicarMordida(EnemyRuntime runtime, LivingEntity alvo) {
        Optional<AttackHit> acerto = runtime.ataques().tryHit(alvo.getId(), alvo.getBoundingBox(),
                CAIXA_DA_MORDIDA, position(), getYRot(), "body");
        acerto.ifPresent(hit -> {
            alvo.hurt(damageSources().mobAttack(this), hit.damage());
            alvo.knockback(RadioRatTuning.REPUXAO_DA_MORDIDA,
                    getX() - alvo.getX(), getZ() - alvo.getZ());
        });
    }

    /**
     * UNICO ponto que alimenta o stagger deste bicho.
     *
     * <p>Sem ponto fraco e sem multiplicador: o Radio Rat nao tem regiao que paga
     * mais, e inventar uma aqui criaria um numero que nenhum perfil declara. O que
     * importa e que o golpe chegue ao acumulador -- e o acumulador, com limiar 5 e
     * resistencia 0, corta o grito na primeira pancada de verdade.</p>
     *
     * <p>Dano sem atacante (fogo, queda, {@code /kill}) nao acumula stagger:
     * cambalear por queimadura transformaria fogo em interrupcao permanente, e o
     * rato nunca mais gritaria sem que nada explicasse por que.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean levou = super.hurt(source, amount);
        if (level().isClientSide || !levou || !isAlive()) return levou;
        if (!(source.getEntity() instanceof LivingEntity) || !Float.isFinite(amount)
                || amount <= 0.0F) {
            return true;
        }
        EnemyRuntime runtime = runtimeExigido();
        StaggerResult resultado = runtime.sofrerStagger(runtime.ataques().attackInstanceId(),
                amount, RadioRatTuning.RECARGA_APOS_INTERRUPCAO);
        if (resultado == StaggerResult.DISPAROU) {
            getNavigation().stop();
            this.entityData.set(CAMBALEANDO, true);
            this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        }
        return true;
    }

    /** Som de mundo chega ao rato como EVENTO: e assim que um rato avisa o outro. */
    @Override
    public void ouvir(HearingEvent evento) {
        if (!level().isClientSide) runtimeExigido().percepcao().ouvir(evento);
    }

    private void publicarEstado(EnemyRuntime runtime) {
        int fase = runtime.ataques().phase().ordinal();
        if (this.entityData.get(FASE_DE_ATAQUE) != fase) this.entityData.set(FASE_DE_ATAQUE, fase);
        boolean cambaleando = runtime.stagger().cambaleando();
        if (this.entityData.get(CAMBALEANDO) != cambaleando) {
            this.entityData.set(CAMBALEANDO, cambaleando);
        }
        combatState(estadoDeCombate(runtime, cambaleando));
    }

    private EnemyCombatState estadoDeCombate(EnemyRuntime runtime, boolean cambaleando) {
        if (combatState() == EnemyCombatState.DYING) return EnemyCombatState.DYING;
        if (cambaleando) return EnemyCombatState.STAGGERED;
        return switch (runtime.ataques().phase()) {
            case WINDUP -> EnemyCombatState.WINDUP;
            case ACTIVE -> EnemyCombatState.ACTIVE;
            case RECOVERY -> EnemyCombatState.RECOVERY;
            default -> getTarget() != null ? EnemyCombatState.AGGRO : EnemyCombatState.IDLE;
        };
    }

    /**
     * Morrer PUBLICA o repouso.
     *
     * <p>Depois de morto o passo de IA nao roda mais, e o campo sincronizado fica
     * onde estava: o bicho morreria com a pose do golpe travada no cliente. Nao
     * ha erro nisso -- so um cadaver com a antena erguida no meio de um grito que
     * nao saiu.</p>
     */
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) publicarRepouso();
        super.die(source);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide) publicarRepouso();
        super.remove(reason);
    }

    /**
     * Repouso publicado E contadores zerados, no MESMO ponto.
     *
     * <p>Os dois contadores deste bicho nascem aqui na entidade, e nao dentro do
     * runtime, entao {@code EnemyRuntime.limpar()} nao sabe deles. Deixa-los de
     * fora e a versao local do erro que este projeto ja sabe que comete -- limpeza
     * espalhada pelos pontos de saida, com um deles esquecido. Quem liga, desliga,
     * e o par mora no ciclo de vida de quem ligou.</p>
     */
    private void publicarRepouso() {
        this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        this.entityData.set(CAMBALEANDO, false);
        this.ticksComAlvo = 0;
        this.ultimaInstanciaRelatada = 0L;
    }

    // ------------------------------------------------------------- animacao

    @Override
    public void registerControllers(
            software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<RadioRatEntity>(
                this, "corpo", 4, estado -> {
                    if (this.deathTime > 0) return estado.setAndContinue(DEATH);
                    if (cambaleando()) return estado.setAndContinue(STAGGER);
                    return switch (faseDeAtaque()) {
                        case WINDUP -> estado.setAndContinue(WINDUP);
                        case ACTIVE -> estado.setAndContinue(STRIKE);
                        case RECOVERY -> estado.setAndContinue(RECOVERY);
                        default -> estado.setAndContinue(
                                velocidadeHorizontal() >= LIMIAR_DE_CAMINHADA ? WALK : IDLE);
                    };
                }));
    }

    // Os nomes sao um CONTRATO com radio_rat.animation.json. Errar um deles nao da erro:
    // o GeckoLib nao acha o clipe e deixa o osso parado. LoopType.DEFAULT em todos --
    // o tipo de repeticao mora no arquivo, e cravar em Java o faria vencer o JSON.
    private static final software.bernie.geckolib.animation.RawAnimation IDLE =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.radio_rat.idle", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation WALK =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.radio_rat.walk", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation WINDUP =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.radio_rat.windup", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation STRIKE =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.radio_rat.strike", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation RECOVERY =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.radio_rat.recovery", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation STAGGER =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.radio_rat.stagger", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation DEATH =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.radio_rat.death", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);

    /** Deslocamento por tick acima do qual o clipe passa de ocio para caminhada. */
    private static final double LIMIAR_DE_CAMINHADA = 0.02D;

    /**
     * Blocos andados no ultimo tick, medidos por POSICAO.
     *
     * <p>Nao usa getDeltaMovement: no cliente o delta de uma entidade remota so e
     * escrito quando chega pacote de velocidade, entao ele fica zerado na maior
     * parte dos ticks e o bicho andaria sempre no clipe de ocio.</p>
     */
    private double velocidadeHorizontal() {
        double dx = getX() - this.xo;
        double dz = getZ() - this.zo;
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
            getAnimatableInstanceCache() {
        return cacheDeAnimacao;
    }

    @Override
    public double getTick(Object entidade) { return this.tickCount; }

    // ------------------------------------------------------------------ goal

    /**
     * Dispara e conduz o grito; quem mede a janela e o {@link AttackController}.
     *
     * <p>{@code static} de proposito: todo estado que ela le mora na ENTIDADE. Um
     * campo aqui seria compartilhado por todos os ratos do bando, e o sintoma
     * seria um deles gritando sobre o alvo de outro.</p>
     */
    private static final class GritoGoal extends Goal {
        private final RadioRatEntity rato;

        private GritoGoal(RadioRatEntity rato) {
            this.rato = rato;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (rato.level().isClientSide) return false;
            EnemyRuntime runtime = rato.runtimeExigido();
            LivingEntity alvo = rato.getTarget();
            // As quatro condicoes vao JUNTAS para a regra, e nao uma em cada if
            // daqui: espalhadas, a que sobrasse em algum caminho produziria um
            // grito sem telegrafo ou um grito cambaleando, e nenhum dos dois da
            // erro -- os dois so tiram do jogador a resposta que o bicho ensina.
            return RadioRatTuning.relatorio().denuncia(alvo != null && alvo.isAlive(),
                    runtime.stagger().cambaleando(), runtime.ataques().canStart(),
                    rato.ticksComAlvo);
        }

        @Override public boolean canContinueToUse() {
            EnemyRuntime runtime = rato.runtimeExigido();
            return runtime.ataques().phase() != AttackPhase.IDLE
                    && runtime.ataques().phase() != AttackPhase.COMPLETE
                    && !runtime.stagger().cambaleando();
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() {
            rato.getNavigation().stop();
            // O dano e LIDO AGORA, do atributo, e nao guardado em lugar nenhum:
            // congelar um multiplicador na ativacao e o primeiro erro da lista de
            // falhas silenciosas deste projeto.
            rato.runtimeExigido().ataques().start(RadioRatTuning.grito(
                    (float) rato.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        }

        @Override public void tick() {
            LivingEntity alvo = rato.getTarget();
            // Encarar o alvo so ate o fim do WINDUP. Depois disso a direcao esta
            // TRAVADA: sem travar, a mordida viraria mira-laser e o desvio de quem
            // se afasta no ultimo instante deixaria de existir.
            if (alvo != null && rato.runtimeExigido().ataques().phase() == AttackPhase.WINDUP) {
                rato.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            }
            rato.setDeltaMovement(rato.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
        }

        @Override public void stop() { rato.getNavigation().stop(); }
    }
}
