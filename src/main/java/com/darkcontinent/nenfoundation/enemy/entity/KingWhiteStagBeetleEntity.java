package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.base.BaseHxHMob;
import com.darkcontinent.nenfoundation.enemy.base.EnemyRuntime;
import com.darkcontinent.nenfoundation.enemy.combat.AttackController;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHit;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.DecisaoDeViragem;
import com.darkcontinent.nenfoundation.enemy.combat.EstadoDeViragem;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeViragem;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointRegistry;
import com.darkcontinent.nenfoundation.enemy.content.GreedIslandProfiles;
import com.darkcontinent.nenfoundation.enemy.content.KingWhiteStagBeetleTuning;
import com.darkcontinent.nenfoundation.enemy.faction.FactionRelations;
import com.darkcontinent.nenfoundation.enemy.perception.PercepcaoDeAura;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionBudget;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionController;
import com.darkcontinent.nenfoundation.enemy.perception.PerceptionSnapshot;
import com.darkcontinent.nenfoundation.enemy.perception.TargetCandidate;
import com.darkcontinent.nenfoundation.enemy.perception.TargetEvaluator;
import com.darkcontinent.nenfoundation.enemy.perception.ThreatMemory;
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
 * King White Stag Beetle -- criatura de Greed Island.
 *
 * <p>O besouro de carapaca. Armadura alta de proposito: bater na frente nao paga, e o ponto fraco fica embaixo -- o encontro e sobre VIRAR o bicho.</p>
 *
 * <p><b>O QUE ELE FAZ.</b> De pe, ele nao tem ponto fraco alcancavel: a carapaca e
 * a regiao comum, e com armadura 9 uma espadada tira quatro e meio de noventa de
 * vida. O ventre so existe como alvo quando o bicho o APRESENTA, e ele apresenta
 * em duas situacoes:</p>
 *
 * <ol>
 *   <li><b>Empinado.</b> A investida tem 26 ticks de aviso, e durante eles ele
 *       sobe no par traseiro de pernas com os chifres erguidos. Nessa pose o
 *       ventre paga quadruplo para quem estiver NA FRENTE, e dois acertos ali
 *       estouram o limiar de stagger -- que, na carapaca, nenhuma quantidade de
 *       acertos estoura, porque o decaimento come mais do que a espadada poe.
 *       Trancar o stagger nessa pose o DERRUBA.</li>
 *   <li><b>De costas.</b> Ele tambem se derruba sozinho: quando a janela ACTIVE da
 *       investida fecha sem encostar em ninguem, os chifres entram no chao e o
 *       peso da carapaca termina o giro. E o premio de quem desviou, e e a unica
 *       forma de virar o bicho que nao exige bater nele.</li>
 * </ol>
 *
 * <p><b>De cabeca para baixo ele NAO ataca.</b> Sao tres segundos em que o ventre
 * paga de qualquer angulo e o jogador nao arrisca nada -- a unica janela segura do
 * encontro. Os dezoito ticks finais dela ele passa se endireitando, com pose
 * propria, porque encurtar a janela sem dizer transformaria o quebra-cabeca em
 * sorte.</p>
 *
 * <p><b>Onde moram as regras.</b> Toda decisao acima e de {@link RegrasDeViragem}
 * e {@link EstadoDeViragem}, que nao conhecem mundo nem entidade e por isso tem
 * teste proprio. Esta classe mede a geometria no servidor, entrega os numeros e
 * aplica o que voltou; os numeros sao de {@link KingWhiteStagBeetleTuning}.</p>
 *
 * <p>Ela usa {@link EnemyRuntime} por inteiro -- percepcao, cerebro, ataque e
 * stagger -- e acrescenta UM estado proprio, a janela de costas, limpo nos mesmos
 * pontos de saida que o runtime.</p>
 */
public final class KingWhiteStagBeetleEntity extends BaseHxHMob implements software.bernie.geckolib.animatable.GeoEntity {

    /** Fase do ataque: o unico estado de combate que o cliente precisa conhecer. */
    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(KingWhiteStagBeetleEntity.class, EntityDataSerializers.INT);
    /** Cambaleando: sincronizado porque interrupcao invisivel e stagger que so o servidor conhece. */
    private static final EntityDataAccessor<Boolean> CAMBALEANDO =
            SynchedEntityData.defineId(KingWhiteStagBeetleEntity.class, EntityDataSerializers.BOOLEAN);
    /**
     * Ticks que faltam da janela de costas -- zero quando ele esta de pe.
     *
     * <p>Publica-se o CONTADOR e nao um booleano porque o cliente precisa de duas
     * leituras a partir dele: "esta de costas" e "ja esta se levantando". Mandar
     * dois booleanos seria a mesma verdade escrita duas vezes, e no dia em que um
     * chegasse antes do outro o besouro ficaria virado e endireitando ao mesmo
     * tempo -- o que nao da erro, so poe duas poses brigando na tela.</p>
     */
    private static final EntityDataAccessor<Integer> TICKS_DE_COSTAS =
            SynchedEntityData.defineId(KingWhiteStagBeetleEntity.class, EntityDataSerializers.INT);

    private static final RegrasDeViragem REGRAS = KingWhiteStagBeetleTuning.regrasDeViragem();
    private static final WeakPointRegistry PONTOS_FRACOS = KingWhiteStagBeetleTuning.pontosFracos();

    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache cacheDeAnimacao =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    /**
     * A janela de costas DESTA entidade.
     *
     * <p>Campo de instancia, nunca estatico. Um contador de classe faria um besouro
     * virar quando o outro apanha -- e como o card tem uma copia so no mundo,
     * ninguem teria dois deles na tela para descobrir.</p>
     */
    private final EstadoDeViragem viragem = new EstadoDeViragem(REGRAS);

    /**
     * A fase do ataque no tick ANTERIOR.
     *
     * <p>Ela existe para achar a unica transicao que interessa: ACTIVE para
     * RECOVERY, que e a janela de golpe fechando por conta propria. Perguntar so
     * "a fase e RECOVERY?" dispararia a viragem nos vinte e dois ticks da
     * recuperacao inteira, e o besouro cairia, levantaria e cairia de novo sem que
     * nada levantasse excecao.</p>
     */
    private AttackPhase faseAnterior = AttackPhase.IDLE;

    /** A investida em curso ja encostou em alguem. Zerado quando uma nova comeca. */
    private boolean acertouNaInvestida;

    public KingWhiteStagBeetleEntity(EntityType<? extends KingWhiteStagBeetleEntity> type, Level level) {
        super(type, level, GreedIslandProfiles.kingWhiteStagBeetle().metadata(),
                new AwarenessTuning(KingWhiteStagBeetleTuning.TICKS_DE_AVISO,
                        KingWhiteStagBeetleTuning.MEMORIA_DE_ALVO_TICKS));
        instalarRuntime(montarRuntime());
    }

    private EnemyRuntime montarRuntime() {
        var perfil = GreedIslandProfiles.kingWhiteStagBeetle();
        PerceptionController percepcao = new PerceptionController(
                PerceptionBudget.padrao(),
                KingWhiteStagBeetleTuning.coneDeVisao(perfil.attributes().followRange()),
                new ThreatMemory(KingWhiteStagBeetleTuning.MEMORIA_DE_ALVO_TICKS),
                new TargetEvaluator(FactionRelations.padrao(), perfil.metadata().faction(),
                        perfil.attributes().followRange(), perfil.metadata().territorial()),
                PercepcaoDeAura.NENHUMA, KingWhiteStagBeetleTuning.ALCANCE_DE_AUDICAO,
                getUUID().hashCode());
        return new EnemyRuntime(percepcao,
                new EnemyBrain(new AwarenessTuning(KingWhiteStagBeetleTuning.TICKS_DE_AVISO,
                        KingWhiteStagBeetleTuning.MEMORIA_DE_ALVO_TICKS)),
                new AttackController(GreedIslandProfiles.kingWhiteStagBeetleRecarga()),
                new StaggerState(GreedIslandProfiles.kingWhiteStagBeetleStagger()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = GreedIslandProfiles.kingWhiteStagBeetle().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<KingWhiteStagBeetleEntity> registeredType() { return EnemyEntityTypes.KING_WHITE_STAG_BEETLE.get(); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        builder.define(CAMBALEANDO, false);
        builder.define(TICKS_DE_COSTAS, 0);
    }

    public AttackPhase faseDeAtaque() {
        int ordinal = this.entityData.get(FASE_DE_ATAQUE);
        AttackPhase[] fases = AttackPhase.values();
        return ordinal >= 0 && ordinal < fases.length ? fases[ordinal] : AttackPhase.IDLE;
    }

    public boolean cambaleando() { return this.entityData.get(CAMBALEANDO); }

    /** Vale nos DOIS lados: vem do campo sincronizado, e nao do estado de servidor. */
    public boolean deCostas() { return this.entityData.get(TICKS_DE_COSTAS) > 0; }

    /** Os ticks finais da janela: o aviso visivel de que ela vai acabar. */
    public boolean levantando() {
        int restantes = this.entityData.get(TICKS_DE_COSTAS);
        return restantes > 0 && restantes <= KingWhiteStagBeetleTuning.TICKS_PARA_LEVANTAR;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new InvestidaGoal(this));
        goalSelector.addGoal(2, new PerseguirGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
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
                getHealth() <= getMaxHealth() * KingWhiteStagBeetleTuning.FRACAO_DE_VIDA_CRITICA,
                false);
        tickDaViragem();
        aplicarAlvo(snapshot);
        tickDaInvestida(runtime);
        publicarEstado(runtime);
    }

    /**
     * Um tick da janela de costas -- e a imobilidade que vem com ela.
     *
     * <p>A navegacao e parada TODO tick enquanto ele esta virado, e nao apenas no
     * tick do tombo. Um unico {@code stop()} no comeco seria desfeito pelo primeiro
     * goal de locomocao que reavaliasse no tick seguinte, e o besouro andaria de
     * costas -- sem erro, com a pose certa na tela e o corpo se arrastando por
     * baixo dela.</p>
     */
    private void tickDaViragem() {
        if (!viragem.deCostas()) return;
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
        viragem.tick();
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

    /**
     * A investida: o arranco, o acerto, e a viragem de quem errou.
     *
     * <p>O arranco sai UMA vez, no primeiro tick da janela ACTIVE. Aplicado a cada
     * tick, ele viraria aceleracao constante: o besouro atravessaria o alvo e
     * continuaria, e a investida deixaria de poder errar -- que e justamente o que
     * este metodo precisa poder observar.</p>
     */
    private void tickDaInvestida(EnemyRuntime runtime) {
        AttackPhase fase = runtime.ataques().phase();
        if (fase == AttackPhase.ACTIVE) {
            if (faseAnterior != AttackPhase.ACTIVE) arrancar();
            tentarAcertar(runtime);
        }
        // So a transicao NATURAL da janela conta como investida errada. Uma
        // interrupcao manda a fase para IDLE, e ler IDLE como "errou" derrubaria o
        // bicho por ter sido interrompido no meio do golpe -- o que dobraria o
        // premio de quem ja tinha interrompido e apagaria a diferenca entre as duas
        // jogadas que o encontro ensina.
        if (faseAnterior == AttackPhase.ACTIVE && fase == AttackPhase.RECOVERY
                && !acertouNaInvestida) {
            aplicarDecisao(REGRAS.decidir(faseAnterior, false, true, viragem.deCostas()));
        }
        faseAnterior = fase;
    }

    private void arrancar() {
        Vec3 frente = Vec3.directionFromRotation(0.0F, getYRot());
        setDeltaMovement(getDeltaMovement().add(
                frente.x * KingWhiteStagBeetleTuning.VELOCIDADE_DA_INVESTIDA, 0.0D,
                frente.z * KingWhiteStagBeetleTuning.VELOCIDADE_DA_INVESTIDA));
        this.hasImpulse = true;
    }

    private void tentarAcertar(EnemyRuntime runtime) {
        LivingEntity alvo = getTarget();
        if (alvo == null || !alvo.isAlive()) return;
        Optional<AttackHit> acerto = runtime.ataques().tryHit(alvo.getId(), alvo.getBoundingBox(),
                KingWhiteStagBeetleTuning.caixaDaInvestida(), position(), getYRot(),
                KingWhiteStagBeetleTuning.REGIAO_DA_CARAPACA);
        acerto.ifPresent(hit -> {
            acertouNaInvestida = true;
            alvo.hurt(damageSources().mobAttack(this), hit.damage());
            alvo.knockback(KingWhiteStagBeetleTuning.EMPURRAO_DA_INVESTIDA,
                    getX() - alvo.getX(), getZ() - alvo.getZ());
        });
    }

    /**
     * O UNICO lugar que aplica uma {@link DecisaoDeViragem}.
     *
     * <p>As duas causas de tombo chegam aqui pelo mesmo caminho, de proposito. Cada
     * uma escrevendo o proprio {@code virar()} seria a limpeza espalhada pelos
     * pontos de saida vista do outro lado: uma delas esqueceria de parar a
     * navegacao, ou de matar o ataque em curso, e o defeito so apareceria na causa
     * mais rara -- que e a que ninguem testa a mao.</p>
     */
    private void aplicarDecisao(DecisaoDeViragem decisao) {
        if (decisao != DecisaoDeViragem.VIRA_PELA_INVESTIDA
                && decisao != DecisaoDeViragem.VIRA_PELO_TRANCO) {
            return;
        }
        if (!viragem.virar()) return;
        EnemyRuntime runtime = runtimeExigido();
        // O ataque em curso morre com o tombo, e com recarga: de costas ele nao
        // ataca, e voltar de pe com a fase ainda em RECOVERY deixaria um golpe
        // pendurado atravessando a janela inteira.
        runtime.ataques().resetComRecarga(KingWhiteStagBeetleTuning.RECARGA_APOS_INTERRUPCAO);
        faseAnterior = AttackPhase.IDLE;
        acertouNaInvestida = false;
        getNavigation().stop();
        this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
    }

    /** Publica ao cliente SO o que ele precisa para escolher o clipe. */
    private void publicarEstado(EnemyRuntime runtime) {
        int fase = runtime.ataques().phase().ordinal();
        if (this.entityData.get(FASE_DE_ATAQUE) != fase) this.entityData.set(FASE_DE_ATAQUE, fase);
        boolean cambaleando = runtime.stagger().cambaleando();
        if (this.entityData.get(CAMBALEANDO) != cambaleando) {
            this.entityData.set(CAMBALEANDO, cambaleando);
        }
        if (this.entityData.get(TICKS_DE_COSTAS) != viragem.ticksRestantes()) {
            this.entityData.set(TICKS_DE_COSTAS, viragem.ticksRestantes());
        }
        combatState(estadoDeCombate(runtime, cambaleando));
    }

    private EnemyCombatState estadoDeCombate(EnemyRuntime runtime, boolean cambaleando) {
        if (combatState() == EnemyCombatState.DYING) return EnemyCombatState.DYING;
        // De costas conta como STAGGERED, e nao como AGGRO: para quem le o estado de
        // fora -- debug, bestiario, telemetria futura -- "virado" e uma interrupcao
        // longa, e chamar isso de AGGRO diria que ele esta atacando durante a unica
        // janela em que ele nao ataca.
        if (viragem.deCostas() || cambaleando) return EnemyCombatState.STAGGERED;
        if (runtime.consciencia() == EnemyAwarenessState.FLEE) return EnemyCombatState.RETREAT;
        return switch (runtime.ataques().phase()) {
            case WINDUP -> EnemyCombatState.WINDUP;
            case ACTIVE -> EnemyCombatState.ACTIVE;
            case RECOVERY -> EnemyCombatState.RECOVERY;
            default -> getTarget() != null ? EnemyCombatState.AGGRO : EnemyCombatState.IDLE;
        };
    }

    /**
     * UNICO lugar que resolve regiao, aplica multiplicador, alimenta o stagger e
     * decide o tombo.
     *
     * <p>Dano multiplicado em dois handlers e o bug silencioso que este projeto ja
     * sabe que comete: o numero final fica plausivel demais para alguem notar sem
     * medir. A regiao sai da geometria QUE O SERVIDOR TEM; o cliente nao participa.
     * Dano sem atacante (fogo, queda) nao tem angulo: cai na carapaca, passa sem
     * multiplicador e sem stagger -- cambalear por queimadura transformaria fogo em
     * interrupcao permanente, e neste bicho tambem numa forma de vira-lo de
     * graca.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || !(source.getEntity() instanceof LivingEntity atacante)
                || !Float.isFinite(amount) || amount <= 0.0F) {
            return super.hurt(source, amount);
        }
        EnemyRuntime runtime = runtimeExigido();

        // A fase e lida ANTES de qualquer coisa que mexa no controlador de ataque.
        // sofrerStagger() reseta a linha do tempo quando dispara; lida depois, a
        // fase valeria sempre IDLE, o ventre empinado nunca seria reconhecido e
        // VIRA_PELO_TRANCO nunca aconteceria. Nao ha excecao nem log: metade das
        // formas de virar o bicho simplesmente deixa de existir.
        AttackPhase faseNoImpacto = runtime.ataques().phase();
        boolean estavaDeCostas = viragem.deCostas();

        Vec3 impacto = pontoDeImpacto(source, atacante);
        double alturaRelativa = Mth.clamp((impacto.y - getY()) / getBbHeight(), 0.0D, 1.0D);
        String regiao = REGRAS.regiao(faseNoImpacto, estavaDeCostas, alturaRelativa,
                cossenoDoOlharAte(atacante));
        float danoFinal = PONTOS_FRACOS.damage(amount, regiao);

        boolean levou = super.hurt(source, danoFinal);
        if (!levou) return false;

        // SEM_ATAQUE, e nao o id da instancia de ataque DESTE mob -- e a diferenca
        // importa. O campo existe para impedir que UM golpe conte duas vezes, e o id
        // do proprio ataque do besouro e o mesmo durante todo o aviso: com ele, o
        // segundo acerto do jogador dentro do mesmo aviso voltaria REPETIDO, o
        // acumulado pararia em 22, o limiar de 30 nunca seria alcancado e o bicho
        // NUNCA poderia ser derrubado a pancada. Nada disso levanta erro -- o
        // combate continua inteiro, so a jogada que o encontro ensina para de
        // funcionar.
        StaggerResult resultado = runtime.sofrerStagger(StaggerState.SEM_ATAQUE, danoFinal,
                KingWhiteStagBeetleTuning.RECARGA_APOS_INTERRUPCAO);
        if (resultado == StaggerResult.DISPAROU) {
            getNavigation().stop();
            this.entityData.set(CAMBALEANDO, true);
            this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        }
        aplicarDecisao(REGRAS.decidir(faseNoImpacto, resultado == StaggerResult.DISPAROU,
                false, estavaDeCostas));
        return true;
    }

    /**
     * Ponto de impacto medido NO SERVIDOR: traco do olho do atacante contra a caixa.
     *
     * <p>Sem intersecao, cai para a posicao do dano (ou o olho do atacante) -- nunca
     * para algo que o cliente afirmou.</p>
     */
    private Vec3 pontoDeImpacto(DamageSource source, LivingEntity atacante) {
        Vec3 olho = atacante.getEyePosition();
        Vec3 fim = olho.add(atacante.getLookAngle()
                .scale(KingWhiteStagBeetleTuning.ALCANCE_DO_TRACO));
        Vec3 reserva = source.getSourcePosition() != null ? source.getSourcePosition() : olho;
        return getBoundingBox().clip(olho, fim).orElse(reserva);
    }

    /**
     * Morrer PUBLICA o repouso.
     *
     * <p>Depois de morto o passo de IA nao roda mais, e o campo sincronizado fica
     * onde estava: o bicho morreria com a pose do golpe travada no cliente. Nao ha
     * erro nisso -- so um cadaver de chifres erguidos. Aqui e pior do que no boneco
     * de treino: morto durante a janela de costas, ele ficaria com o contador
     * congelado e o clipe de virado rodando para sempre.</p>
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

    /** Um ponto de saida so, para o estado proprio e para o que o cliente ve. */
    private void publicarRepouso() {
        viragem.limpar();
        faseAnterior = AttackPhase.IDLE;
        acertouNaInvestida = false;
        this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        this.entityData.set(CAMBALEANDO, false);
        this.entityData.set(TICKS_DE_COSTAS, 0);
    }

    // ------------------------------------------------------------- animacao

    /**
     * O CLIENTE NAO DECIDE NADA: ele le a fase, o cambaleio e o contador de costas.
     *
     * <p>Ordem de precedencia, da mais especifica para a menos: morte, virado,
     * stagger, fases da investida, locomocao, ocio. O virado vem ANTES do stagger de
     * proposito -- o mesmo golpe que derruba tambem dispara o stagger, e se o
     * cambaleio ganhasse, o tombo ficaria invisivel: o jogador veria o besouro
     * tremer e depois ficar parado tres segundos, sem nada na tela dizendo por
     * que.</p>
     */
    @Override
    public void registerControllers(
            software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<KingWhiteStagBeetleEntity>(
                this, "corpo", 4, estado -> {
                    if (this.deathTime > 0) return estado.setAndContinue(DEATH);
                    if (deCostas()) {
                        return estado.setAndContinue(levantando() ? RIGHTING : CAPSIZED);
                    }
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

    // Os nomes sao um CONTRATO com king_white_stag_beetle.animation.json. Errar um deles nao da erro:
    // o GeckoLib nao acha o clipe e deixa o osso parado. LoopType.DEFAULT em todos --
    // o tipo de repeticao mora no arquivo, e cravar em Java o faria vencer o JSON.
    private static final software.bernie.geckolib.animation.RawAnimation IDLE =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.king_white_stag_beetle.idle", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation WALK =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.king_white_stag_beetle.walk", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation WINDUP =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.king_white_stag_beetle.windup", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation STRIKE =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.king_white_stag_beetle.strike", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation RECOVERY =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.king_white_stag_beetle.recovery", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation STAGGER =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.king_white_stag_beetle.stagger", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation DEATH =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.king_white_stag_beetle.death", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    /** De costas, pernas para o ar. Sem este clipe o tombo seria estado invisivel. */
    private static final software.bernie.geckolib.animation.RawAnimation CAPSIZED =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.king_white_stag_beetle.capsized", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    /** O fim da janela, com pose propria: e o aviso de que ela esta acabando. */
    private static final software.bernie.geckolib.animation.RawAnimation RIGHTING =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.king_white_stag_beetle.righting", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);

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

    // ------------------------------------------------------------------ goals

    /**
     * Anda ate o alvo enquanto ele estiver longe demais para a investida.
     *
     * <p>{@code static} de proposito: todo estado que ela le mora na ENTIDADE. Um
     * campo aqui seria compartilhado por todos os besouros, e o sintoma seria um
     * deles perseguindo o alvo de outro.</p>
     */
    private static final class PerseguirGoal extends Goal {
        private final KingWhiteStagBeetleEntity besouro;

        private PerseguirGoal(KingWhiteStagBeetleEntity besouro) {
            this.besouro = besouro;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            LivingEntity alvo = besouro.getTarget();
            return alvo != null && alvo.isAlive()
                    && !besouro.viragem.deCostas()
                    && !besouro.runtimeExigido().stagger().cambaleando()
                    && besouro.runtimeExigido().ataques().phase() == AttackPhase.IDLE
                    && besouro.distanceTo(alvo) > KingWhiteStagBeetleTuning.ALCANCE_DA_INVESTIDA;
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void tick() {
            LivingEntity alvo = besouro.getTarget();
            if (alvo == null) return;
            besouro.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            besouro.getNavigation().moveTo(alvo, 1.0D);
        }

        @Override public void stop() { besouro.getNavigation().stop(); }
    }

    /** Dispara e conduz a investida; quem mede a janela e o {@link AttackController}. */
    private static final class InvestidaGoal extends Goal {
        private final KingWhiteStagBeetleEntity besouro;

        private InvestidaGoal(KingWhiteStagBeetleEntity besouro) {
            this.besouro = besouro;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (besouro.level().isClientSide) return false;
            // A UNICA leitura de "de cabeca para baixo ele nao ataca" no codigo de
            // comportamento. Escrito aqui como um `if` proprio, o mesmo fato viveria
            // em dois lugares e divergiria no dia em que a regra mudasse -- e a
            // divergencia seria um besouro que ataca de costas por um caminho so.
            if (!REGRAS.podeAtacar(besouro.viragem.deCostas())) return false;
            EnemyRuntime runtime = besouro.runtimeExigido();
            if (runtime.stagger().cambaleando()) return false;
            if (runtime.consciencia() == EnemyAwarenessState.FLEE) return false;
            LivingEntity alvo = besouro.getTarget();
            if (alvo == null || !alvo.isAlive()) return false;
            return besouro.distanceTo(alvo) <= KingWhiteStagBeetleTuning.ALCANCE_DA_INVESTIDA
                    && besouro.hasLineOfSight(alvo)
                    && runtime.ataques().canStart();
        }

        @Override public boolean canContinueToUse() {
            EnemyRuntime runtime = besouro.runtimeExigido();
            return runtime.ataques().phase() != AttackPhase.IDLE
                    && runtime.ataques().phase() != AttackPhase.COMPLETE
                    && !runtime.stagger().cambaleando()
                    && !besouro.viragem.deCostas();
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() {
            besouro.getNavigation().stop();
            besouro.acertouNaInvestida = false;
            besouro.runtimeExigido().ataques().start(KingWhiteStagBeetleTuning.investida());
        }

        @Override public void tick() {
            LivingEntity alvo = besouro.getTarget();
            EnemyRuntime runtime = besouro.runtimeExigido();
            // Ele mira apenas nos primeiros ticks do aviso; depois disso a direcao
            // esta TRAVADA. E essa trava que permite a investida ERRAR, e errar e uma
            // das duas formas de virar o bicho. Mirar o aviso inteiro daria um besouro
            // que nunca erra, e o encontro perderia metade da resposta sem que uma
            // linha de log mudasse.
            if (alvo != null && runtime.ataques().phase() == AttackPhase.WINDUP
                    && runtime.ataques().remainingTicks()
                            > KingWhiteStagBeetleTuning.WINDUP_DA_INVESTIDA
                                    - KingWhiteStagBeetleTuning.TICKS_DE_MIRA_NO_WINDUP) {
                besouro.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            }
            // Freio so no aviso: durante a janela ACTIVE o arranco E o ataque, e
            // amortece-lo aqui faria a investida parar no lugar. Ela erraria sempre,
            // o besouro se derrubaria sozinho toda vez, e nada apontaria para ca.
            if (runtime.ataques().phase() == AttackPhase.WINDUP) {
                besouro.setDeltaMovement(besouro.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
            }
        }

        @Override public void stop() { besouro.getNavigation().stop(); }
    }
}
