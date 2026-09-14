package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.base.BaseHxHMob;
import com.darkcontinent.nenfoundation.enemy.base.EnemyRuntime;
import com.darkcontinent.nenfoundation.enemy.combat.AttackController;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.DecisaoDeEstouro;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeEstouro;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import com.darkcontinent.nenfoundation.enemy.content.GreedIslandProfiles;
import com.darkcontinent.nenfoundation.enemy.content.HyperPuffballTuning;
import com.darkcontinent.nenfoundation.enemy.faction.FactionRelations;
import com.darkcontinent.nenfoundation.enemy.perception.PercepcaoDeAura;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionBudget;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionController;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionSnapshot;
import com.darkcontinent.nenfoundation.enemy.perception.TargetCandidate;
import com.darkcontinent.nenfoundation.enemy.perception.TargetEvaluator;
import com.darkcontinent.nenfoundation.enemy.perception.ThreatMemory;
import com.darkcontinent.nenfoundation.enemy.perception.VisionCone;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Hyper Puffball -- criatura de Greed Island.
 *
 * <p>O fungo que NAO anda. Velocidade zero e a ficha: ele nao persegue ninguem, ele espera -- e o perigo e o estouro de esporos de quem bateu de perto.</p>
 *
 * <p><b>O QUE ELE FAZ.</b> Tres coisas, e nenhuma delas e perseguir:</p>
 *
 * <ul>
 *   <li><b>Ele fica.</b> MOVEMENT_SPEED 0.0 e nenhum goal de deslocamento: nao ha
 *       passeio, nao ha perseguicao e nao ha corpo a corpo. Ele ainda PERCEBE --
 *       a percepcao com orcamento continua rodando -- porque o alvo percebido e
 *       quem o estufo de aviso encara.</li>
 *   <li><b>Ele avisa.</b> Com alguem dentro de {@code DISTANCIA_DO_AVISO} ele
 *       roda um ataque de dano ZERO: incha, estufa e desincha. E o unico
 *       telegrafo que um bicho imovel consegue dar, e e ele que transforma o
 *       estouro em consequencia em vez de armadilha.</li>
 *   <li><b>Ele estoura, uma vez.</b> Dano que o leve a
 *       {@code FRACAO_DE_VIDA_DO_GATILHO} da vida, vindo de alguem a distancia de
 *       TOQUE, dispara dano em area num raio pequeno -- e ele morre no mesmo ato.
 *       Matar de longe (flecha, magia) nao dispara nada: essa e a licao inteira
 *       do bicho.</li>
 * </ul>
 *
 * <p><b>A decisao de estourar mora num lugar so:</b> {@link #hurt}, perguntando a
 * {@link RegrasDeEstouro}, e executada por {@link #estourar()}. Espalhada por
 * {@code die}, {@code remove} e um tick de vigia, ela produziria o pior defeito
 * possivel aqui -- o estouro duplo -- e ele nao daria erro nenhum: daria um
 * jogador levando o dobro do dano documentado sem nenhuma linha de log.</p>
 *
 * <p>Ela ja usa {@link EnemyRuntime} por inteiro -- percepcao, cerebro, ataque e
 * stagger -- para que o comportamento proprio entre SOBRE a fundacao, e nao ao
 * lado dela.</p>
 */
public final class HyperPuffballEntity extends BaseHxHMob implements software.bernie.geckolib.animatable.GeoEntity {

    /** Fase do ataque: o unico estado de combate que o cliente precisa conhecer. */
    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(HyperPuffballEntity.class, EntityDataSerializers.INT);
    /** Cambaleando: sincronizado porque interrupcao invisivel e stagger que so o servidor conhece. */
    private static final EntityDataAccessor<Boolean> CAMBALEANDO =
            SynchedEntityData.defineId(HyperPuffballEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int MEMORIA_DE_ALVO_TICKS = 120;
    private static final int TICKS_DE_AVISO = 20;
    private static final double ABERTURA_DA_VISAO = 75.0D;
    private static final double ALCANCE_DE_AUDICAO = 14.0D;
    private static final float FRACAO_DE_VIDA_CRITICA = 0.25F;

    /** As regras do bicho, numa fonte so. Ver {@link HyperPuffballTuning}. */
    private static final RegrasDeEstouro REGRAS = HyperPuffballTuning.regrasDeEstouro();
    /** O estufo: dano ZERO, e por isso ele nunca consulta caixa de golpe. */
    private static final AttackDefinition AVISO = HyperPuffballTuning.aviso();

    /** Quantas particulas de esporo o estouro cospe. Leitura, nao balanceamento. */
    private static final int PARTICULAS_DO_ESTOURO = 60;

    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache cacheDeAnimacao =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    /**
     * O marcador de uso unico. DA INSTANCIA, e nunca da classe.
     *
     * <p>Estatico, ele seria a versao deste bicho do erro que o CLAUDE.md ja
     * conhece -- estado de jogador num campo de classe: o primeiro puffball a
     * estourar calaria todos os outros do mundo, e ninguem ligaria uma coisa a
     * outra.</p>
     *
     * <p>Ele nao e persistido de proposito: o estouro e a morte acontecem no mesmo
     * ato, entao nao existe estado salvo em que ele valha {@code true}. Persistir
     * criaria um campo que so pode estar errado.</p>
     */
    private boolean estourou;

    public HyperPuffballEntity(EntityType<? extends HyperPuffballEntity> type, Level level) {
        super(type, level, GreedIslandProfiles.hyperPuffball().metadata(),
                new AwarenessTuning(TICKS_DE_AVISO, MEMORIA_DE_ALVO_TICKS));
        instalarRuntime(montarRuntime());
    }

    private EnemyRuntime montarRuntime() {
        var perfil = GreedIslandProfiles.hyperPuffball();
        PerceptionController percepcao = new PerceptionController(
                PerceptionBudget.padrao(),
                VisionCone.deGraus(perfil.attributes().followRange(), ABERTURA_DA_VISAO),
                new ThreatMemory(MEMORIA_DE_ALVO_TICKS),
                new TargetEvaluator(FactionRelations.padrao(), perfil.metadata().faction(),
                        perfil.attributes().followRange(), perfil.metadata().territorial()),
                PercepcaoDeAura.NENHUMA, ALCANCE_DE_AUDICAO, getUUID().hashCode());
        return new EnemyRuntime(percepcao,
                new EnemyBrain(new AwarenessTuning(TICKS_DE_AVISO, MEMORIA_DE_ALVO_TICKS)),
                new AttackController(GreedIslandProfiles.hyperPuffballRecarga()),
                new StaggerState(GreedIslandProfiles.hyperPuffballStagger()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = GreedIslandProfiles.hyperPuffball().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<HyperPuffballEntity> registeredType() { return EnemyEntityTypes.HYPER_PUFFBALL.get(); }

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

    /**
     * Nenhum goal move este bicho, e essa ausencia e a ficha dele.
     *
     * <p>O {@code WaterAvoidingRandomStrollGoal} que o andaime trazia SAIU. Com
     * MOVEMENT_SPEED 0.0 ele nao teria movido o fungo um pixel -- e e justamente
     * por isso que era perigoso: um goal de passeio que nao passeia fica verde
     * para sempre, ate o dia em que alguem subir a velocidade "so para testar" e
     * o fungo sair andando pela ilha sem que ninguem tenha pedido.</p>
     *
     * <p>O {@code FloatGoal} FICA: ele nao e locomocao, e sobrevivencia. Sem ele o
     * fungo afunda e morre afogado sozinho, sem estourar, e o jogador encontra um
     * loot boiando sem entender o que aconteceu.</p>
     *
     * <p>Olhar fica: a percepcao mede o cone a partir de {@code getLookAngle()}.
     * Um fungo que nunca vira a cabeca teria um cone congelado apontando para o
     * norte, e o aviso so funcionaria para quem chegasse por um lado.</p>
     */
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // NENHUM target goal vanilla: quem escolhe alvo aqui e o TargetEvaluator,
        // por faccao. Um NearestAttackableTargetGoal ao lado seria uma segunda
        // autoridade sobre a mesma decisao, e as duas discordariam em silencio.
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;
        EnemyRuntime runtime = runtimeExigido();
        PerceptionSnapshot snapshot = runtime.tick(tickCount, this::varrerCandidatos,
                getHealth() <= getMaxHealth() * FRACAO_DE_VIDA_CRITICA, false);
        aplicarAlvo(snapshot);
        avisar(runtime);
        publicarEstado(runtime);
    }

    /**
     * O estufo de aviso -- o unico telegrafo de um bicho que nao anda.
     *
     * <p>Ele nao machuca ninguem: nenhuma linha daqui chama {@code tryHit}, e a
     * definicao carrega dano zero de qualquer forma. O que ele faz e publicar
     * FASE, e a fase e o que o cliente le para escolher o clipe.</p>
     *
     * <p>Um fungo que ja estourou nao avisa: ele esta morto no mesmo tick, e
     * comecar um telegrafo aqui deixaria a fase publicada em WINDUP no ultimo
     * pacote que o cliente recebe -- um cadaver inflado.</p>
     */
    private void avisar(EnemyRuntime runtime) {
        if (estourou) return;
        LivingEntity alvo = getTarget();
        double distancia = alvo != null && alvo.isAlive() ? distanceTo(alvo) : Double.NaN;
        if (!REGRAS.deveAvisar(distancia)) return;
        // canStart() cobre recarga E fase em curso. Sem ele, start() lanca -- e
        // um IllegalStateException por tick derrubaria o tick do servidor inteiro
        // por causa de um bicho decorativo.
        if (runtime.ataques().canStart()) runtime.ataques().start(AVISO);
    }

    /**
     * O alvo aqui NAO e perseguicao -- e para onde olhar e a quem avisar.
     *
     * <p>Nenhum goal deste mob usa {@code getTarget()} para andar, e a velocidade
     * e zero. Ele existe porque o estufo precisa de alguem para encarar e porque
     * {@code publicarEstado} usa a presenca de alvo para separar AGGRO de IDLE.</p>
     */
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

    /** Varredura de mundo -- a UNICA, e so quando o orcamento autoriza. */
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

    private void publicarEstado(EnemyRuntime runtime) {
        int fase = runtime.ataques().phase().ordinal();
        if (this.entityData.get(FASE_DE_ATAQUE) != fase) this.entityData.set(FASE_DE_ATAQUE, fase);
        boolean cambaleando = runtime.stagger().cambaleando();
        if (this.entityData.get(CAMBALEANDO) != cambaleando) {
            this.entityData.set(CAMBALEANDO, cambaleando);
        }
        if (combatState() != EnemyCombatState.DYING) combatState(estadoDeCombate(runtime, cambaleando));
    }

    /**
     * O estado que as ferramentas de diagnostico leem.
     *
     * <p>As fases do estufo entram aqui de proposito. Deixadas de fora, o comando
     * de debug mostraria AGGRO durante o telegrafo inteiro, e quem investigasse
     * "o fungo nao avisa" veria um estado que nao distingue avisar de so estar
     * olhando -- e concluiria que o aviso nao roda, quando ele roda.</p>
     */
    private EnemyCombatState estadoDeCombate(EnemyRuntime runtime, boolean cambaleando) {
        if (cambaleando) return EnemyCombatState.STAGGERED;
        return switch (runtime.ataques().phase()) {
            case WINDUP -> EnemyCombatState.WINDUP;
            case ACTIVE -> EnemyCombatState.ACTIVE;
            case RECOVERY -> EnemyCombatState.RECOVERY;
            default -> getTarget() != null ? EnemyCombatState.AGGRO : EnemyCombatState.IDLE;
        };
    }

    // ---------------------------------------------------------------- estouro

    /**
     * A UNICA porta do estouro, e a UNICA que alimenta o stagger.
     *
     * <p>Tudo que decide corre aqui: o dano ja aplicado, a vida que sobrou e a
     * distancia de quem bateu. Uma segunda decisao espalhada por {@code die} ou
     * por um tick de vigia produziria o estouro duplo -- dano dobrado, nenhum
     * erro, nenhuma linha de log, e um numero final plausivel demais para alguem
     * notar sem medir.</p>
     *
     * <p><b>Quem mede a distancia e o CAUSADOR, nunca o projetil.</b>
     * {@code getEntity()} devolve o arqueiro; {@code getDirectEntity()} devolveria
     * a flecha, que esta encostada no fungo por definicao. Medir pela flecha faria
     * todo tiro contar como toque e apagaria a unica licao deste bicho -- matar de
     * longe e seguro -- sem que nada reprovasse.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || !Float.isFinite(amount) || amount <= 0.0F) {
            return super.hurt(source, amount);
        }
        if (!super.hurt(source, amount)) return false;

        DecisaoDeEstouro decisao = REGRAS.decidir(getHealth(), getMaxHealth(),
                distanciaDoCausador(source), estourou);
        if (decisao == DecisaoDeEstouro.ESTOURA) {
            estourar();
            return true;
        }
        // Stagger so vem de corpo. Cambalear por queimadura ou afogamento
        // transformaria fogo em interrupcao permanente, e o fungo passaria a vida
        // inteira no clipe de tranco sem nunca chegar a avisar ninguem.
        if (source.getEntity() instanceof LivingEntity) alimentarStagger(amount);
        return true;
    }

    /** Distancia ate quem CAUSOU o dano, ou NaN quando nao ha corpo (fogo, queda, veneno). */
    private double distanciaDoCausador(DamageSource source) {
        return source.getEntity() instanceof LivingEntity causador && causador != this
                ? distanceTo(causador) : Double.NaN;
    }

    private void alimentarStagger(float dano) {
        EnemyRuntime runtime = runtimeExigido();
        if (runtime.sofrerStagger(runtime.ataques().attackInstanceId(), dano,
                HyperPuffballTuning.RECARGA_APOS_INTERRUPCAO_TICKS) != StaggerResult.DISPAROU) {
            return;
        }
        // Publicado no mesmo tick, e nao no proximo passo de IA: o passo de IA
        // pode nao rodar (mob fora de simulacao, morte no mesmo tick) e o cliente
        // ficaria com a pose do estufo enquanto o servidor ja cortou o ataque.
        this.entityData.set(CAMBALEANDO, true);
        this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
    }

    /**
     * O estouro: dano em area, uma vez, e a morte no mesmo ato.
     *
     * <p>A ordem importa e nao e arbitraria. O marcador vem PRIMEIRO, antes de
     * qualquer coisa que possa reentrar aqui -- machucar uma vitima pode devolver
     * dano por encantamento ou efeito, e esse dano volta por {@link #hurt}. Com o
     * marcador ja em pe, a reentrada encontra {@code JA_ESTOUROU} e para.</p>
     *
     * <p>A morte vem por ULTIMO e por dano, e nao por {@code discard()}: descartar
     * pula o loot, o evento de morte e o credito do abate, e o jogador que
     * arriscou chegar perto ficaria sem o card que foi buscar.</p>
     */
    private void estourar() {
        estourou = true;
        EnemyRuntime runtime = runtimeExigido();
        runtime.ataques().reset();
        this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());

        AABB area = getBoundingBox().inflate(REGRAS.raio());
        for (LivingEntity vitima : level().getEntitiesOfClass(LivingEntity.class, area,
                v -> v != this && v.isAttackable())) {
            if (!REGRAS.atinge(vitima.getType() == getType(), vitima.isAlive(),
                    distanceTo(vitima))) {
                continue;
            }
            vitima.hurt(damageSources().mobAttack(this), REGRAS.dano());
        }

        nuvemDeEsporos();
        // Ainda vivo quer dizer que o golpe que cruzou o limiar nao o matou: e
        // este ato que o mata. Ja morto, a morte ja esta em curso e um segundo
        // dano so duplicaria evento.
        if (isAlive()) hurt(damageSources().genericKill(), Float.MAX_VALUE);
    }

    /**
     * A unica coisa que o jogador VE do estouro.
     *
     * <p>Sem som proprio (ver {@code docs/testing/o-que-nao-provamos.md}) e com o
     * corpo desaparecendo no mesmo tick, um estouro sem particula seria dano
     * vindo do nada -- o formato de bug mais caro que existe, porque o jogador
     * relata "perdi vida sem motivo" e ninguem consegue reproduzir.</p>
     */
    private void nuvemDeEsporos() {
        if (!(level() instanceof ServerLevel nivel)) return;
        // sendParticles do SERVIDOR: o cliente nao inventa nada, ele recebe. Um
        // spawnParticles local so apareceria para quem hospeda o mundo.
        nivel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + getBbHeight() * 0.6D,
                getZ(), PARTICULAS_DO_ESTOURO, REGRAS.raio() * 0.4D, 0.3D, REGRAS.raio() * 0.4D,
                0.02D);
    }

    /**
     * Morrer PUBLICA o repouso.
     *
     * <p>Depois de morto o passo de IA nao roda mais, e o campo sincronizado fica
     * onde estava: o bicho morreria com a pose do golpe travada no cliente. Nao
     * ha erro nisso -- so um cadaver de bracos erguidos.</p>
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

    private void publicarRepouso() {
        this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        this.entityData.set(CAMBALEANDO, false);
    }

    // ------------------------------------------------------------- animacao

    @Override
    public void registerControllers(
            software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<HyperPuffballEntity>(
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

    // Os nomes sao um CONTRATO com hyper_puffball.animation.json. Errar um deles nao da erro:
    // o GeckoLib nao acha o clipe e deixa o osso parado. LoopType.DEFAULT em todos --
    // o tipo de repeticao mora no arquivo, e cravar em Java o faria vencer o JSON.
    private static final software.bernie.geckolib.animation.RawAnimation IDLE =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.hyper_puffball.idle", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation WALK =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.hyper_puffball.walk", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation WINDUP =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.hyper_puffball.windup", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation STRIKE =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.hyper_puffball.strike", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation RECOVERY =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.hyper_puffball.recovery", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation STAGGER =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.hyper_puffball.stagger", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation DEATH =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.hyper_puffball.death", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);

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
}
