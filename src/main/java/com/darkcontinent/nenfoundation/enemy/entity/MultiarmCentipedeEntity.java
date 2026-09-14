package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.base.BaseChimeraAnt;
import com.darkcontinent.nenfoundation.enemy.base.EnemyRuntime;
import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenIntent;
import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenSituation;
import com.darkcontinent.nenfoundation.enemy.combat.AttackController;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHit;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.EntradaDaSequencia;
import com.darkcontinent.nenfoundation.enemy.combat.EstadoDaSequencia;
import com.darkcontinent.nenfoundation.enemy.combat.GolpeEncadeado;
import com.darkcontinent.nenfoundation.enemy.combat.SequenciaDeGolpes;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import com.darkcontinent.nenfoundation.enemy.content.ChimeraProfiles;
import com.darkcontinent.nenfoundation.enemy.content.MultiarmCentipedeTuning;
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
 * Multiarm Centipede -- formiga quimera, rank OFFICER. A que ataca em SEQUENCIA.
 *
 * <p><b>O que ela faz, numa frase:</b> ela nao da um golpe, ela da TRES seguidos,
 * e a unica coisa que o jogador precisa aprender e que a janela para revidar fica
 * depois do ULTIMO.</p>
 *
 * <p><b>Os tres golpes tem telegrafos proprios, e o terceiro e o dobro dos
 * outros.</b> Doze ticks, onze ticks, vinte e quatro. Cada golpe sai do seu par de
 * bracos, cada par alcanca mais longe que o anterior, e quem recua um passo depois
 * do primeiro continua dentro do ultimo. A recuperacao dos dois primeiros e
 * CORTADA pelo golpe seguinte depois de quatro ticks de emenda -- e por isso que
 * eles nao tem janela de punicao; a do terceiro dura vinte e seis ticks e e a
 * unica que tem. Inverter esse degrau nao daria erro nenhum: daria um combo que o
 * jogador nao consegue contar, e a resposta viraria sorte. Quem reprova a inversao
 * e {@link SequenciaDeGolpes}.</p>
 *
 * <p><b>A sequencia pode ser CORTADA, e cortar e a jogada mais cara do
 * encontro.</b> Stagger no meio dela cancela o resto: {@link EnemyRuntime}
 * interrompe o ataque e impoe uma recarga PUNITIVA de oitenta ticks, maior do que
 * a recarga normal entre sequencias, e {@link EstadoDaSequencia} joga fora o
 * indice para que o proximo combo comece do primeiro golpe. Sem a recarga punitiva
 * ela poderia recomecar no tick seguinte, e o jogador aprenderia a NAO interromper
 * -- o oposto do que o stagger existe para ensinar.</p>
 *
 * <p><b>Ela NAO tem um segundo relogio de ataque.</b> Quem mede janela continua
 * sendo o {@link AttackController}; a sequencia so responde "qual golpe comeca
 * agora". Dois relogios no mesmo mob andariam juntos ate a primeira interrupcao e
 * depois discordariam por um tick, que e invisivel em teste e visivel na tela.</p>
 *
 * <p><b>O Nen dela e uma INTENCAO, e nunca aura.</b> Ela consulta o
 * {@code TacticalNenController} todo tick e a unica coisa que faz com a resposta e
 * decidir se comeca ou nao um combo -- postura, e nao poder. Quem calcula aura,
 * cobra custo e ativa tecnica e o Nen Foundation, e ele nao publica pool para
 * entidade hoje; ver {@code MultiarmCentipedeTuning.FRACAO_DE_AURA_NAO_PUBLICADA},
 * que declara esse ponto cego em vez de inventar um numero.</p>
 *
 * <p>Ela usa {@link EnemyRuntime} por inteiro -- percepcao com orcamento, cerebro,
 * ataque e stagger -- e publica ao cliente so a fase e o cambaleio, que e do que o
 * controlador de animacao precisa para escolher o clipe.</p>
 */
public final class MultiarmCentipedeEntity extends BaseChimeraAnt
        implements software.bernie.geckolib.animatable.GeoEntity {

    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(MultiarmCentipedeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CAMBALEANDO =
            SynchedEntityData.defineId(MultiarmCentipedeEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * A sequencia, resolvida uma vez.
     *
     * <p>Estatica porque ela NAO guarda estado de jogador -- e um record imutavel
     * de records imutaveis. O que e por instancia e o {@link EstadoDaSequencia},
     * que guarda o indice: um indice estatico seria o erro classico deste projeto,
     * e o sintoma seria uma formiga pulando para o terceiro golpe porque a irma do
     * outro lado do mapa chegou la primeiro.</p>
     */
    private static final SequenciaDeGolpes SEQUENCIA = MultiarmCentipedeTuning.sequencia();

    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache cacheDeAnimacao =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    /** Em que golpe do combo ESTA formiga esta. Uma instancia por entidade. */
    private final EstadoDaSequencia sequencia = new EstadoDaSequencia(SEQUENCIA);

    /**
     * A ultima intencao que o controlador tatico devolveu.
     *
     * <p>Guardada em campo porque as Goals rodam ANTES de {@code customServerAiStep}
     * no passo de IA do vanilla: elas leem a intencao do tick anterior, e um tick de
     * atraso numa decisao de postura nao muda nada que o jogador consiga ver.
     * Recalcular dentro da Goal faria a mesma decisao rodar duas vezes por tick,
     * com o contador de {@code ticksNaIntencao} andando o dobro.</p>
     */
    private TacticalNenIntent intencaoTatica = TacticalNenIntent.NENHUMA;

    public MultiarmCentipedeEntity(EntityType<? extends MultiarmCentipedeEntity> type, Level level) {
        super(type, level, ChimeraProfiles.multiarmCentipede().metadata(),
                new AwarenessTuning(MultiarmCentipedeTuning.TICKS_DE_AVISO,
                        MultiarmCentipedeTuning.MEMORIA_DE_ALVO_TICKS),
                ChimeraProfiles.multiarmCentipedeMolde());
        instalarRuntime(montarRuntime());
    }

    private EnemyRuntime montarRuntime() {
        var perfil = ChimeraProfiles.multiarmCentipede();
        PerceptionController percepcao = new PerceptionController(
                PerceptionBudget.padrao(),
                VisionCone.deGraus(perfil.attributes().followRange(),
                        MultiarmCentipedeTuning.ABERTURA_DA_VISAO_EM_GRAUS),
                new ThreatMemory(MultiarmCentipedeTuning.MEMORIA_DE_ALVO_TICKS),
                new TargetEvaluator(FactionRelations.padrao(), perfil.metadata().faction(),
                        perfil.attributes().followRange(), perfil.metadata().territorial()),
                // A porta de aura da formiga continua INERTE enquanto a #126 (Gyo)
                // estiver aberta no nucleo: inventar aqui a regra de quem-ve-o-que
                // seria a segunda autoridade sobre Nen que o CLAUDE.md proibe.
                PercepcaoDeAura.NENHUMA, MultiarmCentipedeTuning.ALCANCE_DE_AUDICAO,
                getUUID().hashCode());
        return new EnemyRuntime(percepcao,
                new EnemyBrain(new AwarenessTuning(MultiarmCentipedeTuning.TICKS_DE_AVISO,
                        MultiarmCentipedeTuning.MEMORIA_DE_ALVO_TICKS)),
                new AttackController(ChimeraProfiles.multiarmCentipedeRecarga()),
                new StaggerState(ChimeraProfiles.multiarmCentipedeStagger()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = ChimeraProfiles.multiarmCentipede().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<MultiarmCentipedeEntity> registeredType() { return EnemyEntityTypes.MULTIARM_CENTIPEDE.get(); }

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

    /** Quantos golpes do combo ja sairam. Server-side: e leitura de debug e de teste. */
    public int golpesDaSequencia() { return sequencia.golpesDados(); }

    /** A ultima intencao tatica de Nen. Ela NAO vira aura; ver a classe de tuning. */
    public TacticalNenIntent intencaoTatica() { return intencaoTatica; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SequenciaGoal(this));
        goalSelector.addGoal(2, new PerseguirGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Sem target goal vanilla: quem escolhe alvo e o TargetEvaluator, por
        // faccao. Um NearestAttackableTargetGoal ao lado seria uma segunda
        // autoridade sobre a mesma decisao, e as duas discordariam em silencio.
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;
        EnemyRuntime runtime = runtimeExigido();
        PerceptionSnapshot snapshot = runtime.tick(tickCount, this::varrerCandidatos,
                getHealth() <= getMaxHealth() * MultiarmCentipedeTuning.FRACAO_DE_VIDA_CRITICA,
                false);
        aplicarAlvo(snapshot);
        intencaoTatica = decidirNenTatico(runtime);
        tickDoGolpe(runtime);
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
     * Pergunta ao controlador tatico o que ela QUERIA fazer com Nen.
     *
     * <p>Todos os fatos passados aqui sao MEDIDOS pelo servidor -- menos um. A
     * fracao de aura vem de {@code FRACAO_DE_AURA_NAO_PUBLICADA}, que e zero,
     * porque o nucleo nao mantem pool de aura para entidade e inventar um valor
     * aqui seria a segunda autoridade sobre Nen. Com zero, o controlador responde
     * {@code NENHUMA} para toda formiga, e isso e a resposta CERTA hoje.</p>
     *
     * <p>{@code alvoEscondido} e falso pela mesma razao: a percepcao de aura dela e
     * {@code NENHUMA} enquanto a #126 estiver aberta, entao ela nao tem como medir
     * "ha algo que a visao comum nao explica". Passar verdadeiro seria afirmar uma
     * percepcao que o mob nao tem.</p>
     */
    private TacticalNenIntent decidirNenTatico(EnemyRuntime runtime) {
        var controlador = nenTatico().orElse(null);
        if (controlador == null) return TacticalNenIntent.NENHUMA;
        LivingEntity alvo = getTarget();
        boolean emCombate = alvo != null && alvo.isAlive();
        boolean aoAlcance = emCombate
                && distanceTo(alvo) <= MultiarmCentipedeTuning.ALCANCE_DA_SEQUENCIA;
        double fracaoDeVida = getMaxHealth() <= 0.0F
                ? 0.0D : Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F);
        // Sob pressao = ha stagger acumulado agora. E a unica medida de "dano pesado
        // recente" que este mob ja tem; inventar um segundo contador de agressao
        // seria duas fontes para a mesma verdade.
        boolean sobPressao = runtime.stagger().cambaleando()
                || runtime.stagger().acumulado() > 0.0F;
        return controlador.decidir(new TacticalNenSituation(
                MultiarmCentipedeTuning.FRACAO_DE_AURA_NAO_PUBLICADA, fracaoDeVida,
                emCombate, aoAlcance, false, sobPressao, true));
    }

    /** Ela recua em vez de comecar combo: vida critica, ou a intencao de sumir. */
    private boolean recuando() {
        return runtimeExigido().consciencia() == EnemyAwarenessState.FLEE
                || MultiarmCentipedeTuning.recuaEmVezDeAtacar(intencaoTatica);
    }

    /**
     * A janela ACTIVE consulta a caixa DO GOLPE CORRENTE; fora dela o golpe nao existe.
     *
     * <p>A caixa sai de {@code sequencia.golpeAtual()}, e nao de um campo fixo: os
     * tres golpes tem alcances diferentes, e usar sempre o maior faria o primeiro
     * golpe acertar de longe demais -- com o dano do primeiro, a fase do primeiro e o
     * log limpo. O jogador apanharia de um braco curto a uma distancia que ele ja
     * tinha aprendido a considerar segura.</p>
     */
    private void tickDoGolpe(EnemyRuntime runtime) {
        if (runtime.ataques().phase() != AttackPhase.ACTIVE || !sequencia.emCurso()) return;
        LivingEntity alvo = getTarget();
        if (alvo == null || !alvo.isAlive()) return;

        GolpeEncadeado golpe = sequencia.golpeAtual();
        Optional<AttackHit> acerto = runtime.ataques().tryHit(alvo.getId(), alvo.getBoundingBox(),
                golpe.caixa(), position(), getYRot(), MultiarmCentipedeTuning.REGIAO_COMUM);
        acerto.ifPresent(hit -> {
            alvo.hurt(damageSources().mobAttack(this), hit.damage());
            alvo.knockback(golpe.definicao().knockback(),
                    getX() - alvo.getX(), getZ() - alvo.getZ());
        });
    }

    private void publicarEstado(EnemyRuntime runtime) {
        int fase = runtime.ataques().phase().ordinal();
        if (this.entityData.get(FASE_DE_ATAQUE) != fase) this.entityData.set(FASE_DE_ATAQUE, fase);
        boolean cambaleando = runtime.stagger().cambaleando();
        if (this.entityData.get(CAMBALEANDO) != cambaleando) {
            this.entityData.set(CAMBALEANDO, cambaleando);
        }
        if (combatState() != EnemyCombatState.DYING) {
            combatState(estadoDeCombate(runtime, cambaleando));
        }
    }

    private EnemyCombatState estadoDeCombate(EnemyRuntime runtime, boolean cambaleando) {
        if (cambaleando) return EnemyCombatState.STAGGERED;
        if (runtime.consciencia() == EnemyAwarenessState.FLEE) return EnemyCombatState.RETREAT;
        return switch (runtime.ataques().phase()) {
            case WINDUP -> EnemyCombatState.WINDUP;
            case ACTIVE -> EnemyCombatState.ACTIVE;
            case RECOVERY -> EnemyCombatState.RECOVERY;
            default -> getTarget() != null ? EnemyCombatState.AGGRO : EnemyCombatState.IDLE;
        };
    }

    /**
     * UNICO lugar que alimenta o stagger -- e e por ele que o combo e cortado.
     *
     * <p>Dano multiplicado em dois handlers e o bug silencioso que este projeto ja
     * sabe que comete: o numero final fica plausivel demais para alguem notar sem
     * medir. Aqui nao ha multiplicador nenhum -- a carapaca dela e uniforme, ela nao
     * tem ponto fraco -- e por isso o stagger le o dano BRUTO do golpe.</p>
     *
     * <p>Quando o stagger dispara, {@link EnemyRuntime#sofrerStagger} interrompe o
     * ataque em curso com a recarga PUNITIVA. O indice da sequencia so e jogado fora
     * no tick seguinte, dentro da Goal, porque e la que a decisao mora -- limpar aqui
     * tambem seria a mesma limpeza escrita em dois lugares, e o dia em que os dois
     * discordassem produziria um combo que recomeca pelo golpe do meio.</p>
     *
     * <p>Dano sem atacante (fogo, queda, {@code /kill}) nao alimenta o stagger:
     * cambalear por queimadura transformaria fogo numa interrupcao permanente, e a
     * formiga nunca mais completaria uma sequencia perto de lava.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean levou = super.hurt(source, amount);
        if (level().isClientSide || !levou || !(source.getEntity() instanceof LivingEntity)
                || !Float.isFinite(amount) || amount <= 0.0F) {
            return levou;
        }
        EnemyRuntime runtime = runtimeExigido();
        StaggerResult resultado = runtime.sofrerStagger(runtime.ataques().attackInstanceId(),
                amount, MultiarmCentipedeTuning.RECARGA_APOS_INTERRUPCAO);
        if (resultado == StaggerResult.DISPAROU) {
            getNavigation().stop();
            this.entityData.set(CAMBALEANDO, true);
            this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        }
        return true;
    }

    /** Morrer PUBLICA o repouso: depois de morto o passo de IA nao roda mais. */
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
     * O ponto UNICO de repouso: fase, cambaleio e o indice do combo saem JUNTOS.
     *
     * <p>Deixar o indice para tras nao daria erro: a entidade recriada pelo
     * carregamento do chunk comecaria a proxima sequencia pelo golpe do meio --
     * telegrafo curto, alcance errado, e sem o golpe que ensina.</p>
     */
    private void publicarRepouso() {
        this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        this.entityData.set(CAMBALEANDO, false);
        sequencia.limpar();
    }

    // ------------------------------------------------------------------ goals

    /**
     * Anda ate o alvo enquanto ele estiver longe demais para o PRIMEIRO golpe.
     *
     * <p>{@code static} de proposito: todo estado que ela le mora na ENTIDADE. Um
     * campo aqui seria compartilhado por todas as centopeias do mundo, e o sintoma
     * seria uma delas perseguindo o alvo de outra.</p>
     */
    private static final class PerseguirGoal extends Goal {
        private final MultiarmCentipedeEntity formiga;

        private PerseguirGoal(MultiarmCentipedeEntity formiga) {
            this.formiga = formiga;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            LivingEntity alvo = formiga.getTarget();
            return alvo != null && alvo.isAlive()
                    && !formiga.runtimeExigido().stagger().cambaleando()
                    && !formiga.sequencia.emCurso()
                    && formiga.distanceTo(alvo) > MultiarmCentipedeTuning.ALCANCE_DA_SEQUENCIA;
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void tick() {
            LivingEntity alvo = formiga.getTarget();
            if (alvo == null) return;
            formiga.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            formiga.getNavigation().moveTo(alvo, 1.0D);
        }

        @Override public void stop() { formiga.getNavigation().stop(); }
    }

    /**
     * Conduz o combo inteiro -- e nao mede um tick sequer.
     *
     * <p>Quem mede a janela e o {@link AttackController}; quem decide qual golpe
     * comeca e {@link EstadoDaSequencia}. Esta Goal apenas HONRA a decisao: ela
     * inicia o golpe que a sequencia apontou, corta a recuperacao do golpe anterior
     * quando a emenda manda, e solta o controle quando o combo acaba.</p>
     *
     * <p><b>A emenda usa {@code reset()} antes de {@code start()}, e isso e
     * deliberado.</b> A linha do tempo do ataque so aceita comecar em IDLE ou
     * COMPLETE, e COMPLETE e justamente onde a recarga de sessenta e cinco ticks e
     * armada. Emendar durante a RECUPERACAO -- antes de a linha fechar -- e o que
     * transforma tres ataques separados num combo; esperar COMPLETE daria um golpe a
     * cada tres segundos e meio, que e um mob completamente diferente com os mesmos
     * numeros.</p>
     */
    private static final class SequenciaGoal extends Goal {
        private final MultiarmCentipedeEntity formiga;

        private SequenciaGoal(MultiarmCentipedeEntity formiga) {
            this.formiga = formiga;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (formiga.level().isClientSide) return false;
            EnemyRuntime runtime = formiga.runtimeExigido();
            if (runtime.stagger().cambaleando()) return false;
            if (formiga.recuando()) return false;
            LivingEntity alvo = formiga.getTarget();
            if (alvo == null || !alvo.isAlive()) return false;
            // O alcance de DECISAO e o do PRIMEIRO golpe, e nao o do maior: comecar
            // um combo de quatro segundos a uma distancia que o primeiro braco nao
            // alcanca faz os dois primeiros golpes baterem no ar, sempre.
            return formiga.distanceTo(alvo) <= MultiarmCentipedeTuning.ALCANCE_DA_SEQUENCIA
                    && formiga.hasLineOfSight(alvo)
                    && runtime.ataques().canStart();
        }

        @Override public boolean canContinueToUse() {
            EnemyRuntime runtime = formiga.runtimeExigido();
            // Enquanto houver combo em curso OU relogio de ataque correndo, a Goal
            // segura a navegacao. Soltar antes faria a PerseguirGoal assumir no meio
            // do combo e a formiga deslizaria para cima do alvo durante o telegrafo --
            // o aviso apontaria para um lugar e o golpe sairia de outro.
            return !runtime.stagger().cambaleando()
                    && (formiga.sequencia.emCurso()
                        || runtime.ataques().phase() != AttackPhase.IDLE
                           && runtime.ataques().phase() != AttackPhase.COMPLETE);
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() { formiga.getNavigation().stop(); }

        @Override public void tick() {
            EnemyRuntime runtime = formiga.runtimeExigido();
            LivingEntity alvo = formiga.getTarget();
            EntradaDaSequencia entrada = new EntradaDaSequencia(
                    runtime.ataques().phase(), runtime.ataques().remainingTicks(),
                    runtime.stagger().cambaleando(), alvo != null && alvo.isAlive(),
                    runtime.ataques().canStart(), formiga.recuando());

            switch (formiga.sequencia.decidir(entrada)) {
                case COMECAR -> runtime.ataques().start(
                        formiga.sequencia.golpeAtual().definicao());
                case ENCADEAR -> {
                    // reset() zera a linha do tempo SEM armar recarga: ele so existe
                    // para a fase voltar a IDLE e o proximo golpe poder comecar. Usar
                    // resetComRecarga aqui poria a recarga entre dois golpes do MESMO
                    // combo, e o combo deixaria de existir.
                    runtime.ataques().reset();
                    runtime.ataques().start(formiga.sequencia.golpeAtual().definicao());
                }
                default -> { }
            }
            mirarNoComecoDoAviso(runtime, alvo);
            // Ela nao desliza enquanto bate. Sem isto, ela chegaria ao fim do aviso
            // ainda carregando a inercia da perseguicao, e o golpe sairia de um lugar
            // diferente daquele onde foi armado -- sem erro nenhum, e com o telegrafo
            // apontando para o lugar errado.
            formiga.setDeltaMovement(formiga.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
        }

        /**
         * Ela mira so nos primeiros ticks de CADA aviso, e depois trava.
         *
         * <p>O relogio e o do servidor, nunca o do clipe. E a conta e feita contra o
         * windup DO GOLPE CORRENTE: usar o windup do golpe final para os tres faria os
         * dois encadeados -- que avisam por doze e onze ticks -- mirarem do inicio ao
         * fim, e eles virariam mira-laser sem que nada acusasse.</p>
         */
        private void mirarNoComecoDoAviso(EnemyRuntime runtime, LivingEntity alvo) {
            if (alvo == null || !formiga.sequencia.emCurso()) return;
            if (runtime.ataques().phase() != AttackPhase.WINDUP) return;
            int windup = formiga.sequencia.golpeAtual().definicao().windupTicks();
            int gastos = windup - runtime.ataques().remainingTicks();
            if (gastos < MultiarmCentipedeTuning.TICKS_DE_MIRA_NO_WINDUP) {
                formiga.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            }
        }

        @Override public void stop() {
            formiga.sequencia.limpar();
            formiga.getNavigation().stop();
        }
    }

    // ------------------------------------------------------------- animacao

    @Override
    public void registerControllers(
            software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<MultiarmCentipedeEntity>(
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

    // Contrato com multiarm_centipede.animation.json. LoopType.DEFAULT em todos: o tipo de
    // repeticao mora no arquivo, e cravar em Java o faria vencer o JSON.
    //
    // OS TRES GOLPES DIVIDEM OS MESMOS TRES CLIPES, e isso e uma decisao. O clipe
    // de aviso dura onze ticks -- o MENOR dos tres windups -- para que todo golpe
    // mostre o gesto inteiro, e ele segura o ultimo quadro para que o golpe final,
    // que avisa por vinte e quatro, termine com os bracos armados em vez de
    // relaxados. Um clipe mais longo que onze ticks seria cortado no meio pelos
    // encadeados, e o jogador veria um aviso que "as vezes" acontece. A regua que
    // cobra isso e `valida_aviso_cabe_no_golpe_mais_curto`, no gerador de animacao.
    private static final software.bernie.geckolib.animation.RawAnimation IDLE = clipe("idle");
    private static final software.bernie.geckolib.animation.RawAnimation WALK = clipe("walk");
    private static final software.bernie.geckolib.animation.RawAnimation WINDUP = clipe("windup");
    private static final software.bernie.geckolib.animation.RawAnimation STRIKE = clipe("strike");
    private static final software.bernie.geckolib.animation.RawAnimation RECOVERY = clipe("recovery");
    private static final software.bernie.geckolib.animation.RawAnimation STAGGER = clipe("stagger");
    private static final software.bernie.geckolib.animation.RawAnimation DEATH = clipe("death");

    private static software.bernie.geckolib.animation.RawAnimation clipe(String nome) {
        return software.bernie.geckolib.animation.RawAnimation.begin()
                .then("animation.multiarm_centipede." + nome,
                        software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    }

    private static final double LIMIAR_DE_CAMINHADA = 0.02D;

    /** Blocos andados no ultimo tick, medidos por POSICAO -- o delta remoto fica zerado. */
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
