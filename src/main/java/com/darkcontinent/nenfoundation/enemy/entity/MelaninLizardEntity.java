package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeCamuflagemDeRocha;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.base.BaseHxHMob;
import com.darkcontinent.nenfoundation.enemy.base.EnemyRuntime;
import com.darkcontinent.nenfoundation.enemy.combat.AttackController;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHit;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.GrabController;
import com.darkcontinent.nenfoundation.enemy.combat.GrabRefusal;
import com.darkcontinent.nenfoundation.enemy.combat.GrabRelease;
import com.darkcontinent.nenfoundation.enemy.combat.SafeReleaseSpot;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointRegistry;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointResolver;
import com.darkcontinent.nenfoundation.enemy.content.GreedIslandProfiles;
import com.darkcontinent.nenfoundation.enemy.content.MelaninLizardTuning;
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
import net.minecraft.world.entity.EntityDimensions;
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
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Melanin Lizard -- a pedra que agarra.
 *
 * <p><b>A ficha, em uma frase:</b> enquanto ninguem olha para ele de perto ele e
 * paisagem; quem o encara acorda um bicho que salta, PRENDE e so solta quando
 * apanha. HP 40, dano 7, velocidade 0.3, armadura 3, ThreatTier HUNTER.</p>
 *
 * <p><b>Camuflagem.</b> Camuflado, o lagarto nao anda, nao persegue e nao e alvo
 * de IA nenhuma -- nem da mira automatica, nem de outro mob varrendo inimigos.
 * Ele volta a existir quando alguem olha para ele de perto (distancia e cone de
 * {@link RegrasDeCamuflagemDeRocha}) ou quando leva dano; perdido o alvo, espera
 * e vira pedra outra vez. A regra inteira mora num record puro, provado sem
 * servidor de pe. Espalhada pelas Goals, a divergencia entre "parou de andar" e
 * "parou de ser alvo" nao daria erro nenhum -- daria um bicho que se mexe
 * enquanto o servidor o declara impossivel de mirar, e isso o jogador le como
 * trapaca.</p>
 *
 * <p><b>Agarrao.</b> Ele REUSA o {@link GrabController} compartilhado, e nao uma
 * copia. Esta e a SEGUNDA prova de que o contrato de agarrao e um so -- o
 * Frog-In-Waiting foi a primeira -- e o CLAUDE.md e explicito sobre um unico
 * ciclo de vida por familia. Copiar do sapo produziria duas implementacoes
 * parecidas que divergem na primeira correcao, e a que esquecer um ponto de
 * saida deixa um jogador preso ate o restart.</p>
 *
 * <p><b>Soltura.</b> Toda soltura passa por {@link #soltar(GrabRelease)}, e onde
 * a vitima cai sai de {@link SafeReleaseSpot} -- este mob e o primeiro consumidor
 * daquela classe. O motivo esta no javadoc dela: a soltura obvia ("poe a vitima
 * na frente") mata quem foi agarrado encostado numa parede, e esse caso so
 * acontece quando o lagarto encurralou alguem, que e justamente quando o agarrao
 * e interessante.</p>
 *
 * <p><b>Captura NAO-LETAL.</b> {@code GreedIslandProfiles.capturas()} declara
 * {@code porEnfraquecimento} para este bicho: um quarto da vida, e matar CANCELA
 * o card. E por isso que ele tem ponto fraco com multiplicador -- sem um jeito de
 * derrubar HP com precisao, o jogador so teria o golpe comum, que passa do ponto
 * e mata o card junto com o bicho.</p>
 *
 * <p>Tudo que decide -- camuflagem, alvo, acerto, agarrao e soltura -- roda no
 * servidor. Os tres campos sincronizados carregam apenas CAMUFLADO, a FASE do
 * ataque e o CAMBALEIO, que e o que o cliente precisa para escolher o clipe. O
 * cliente nunca informa acerto.</p>
 */
public final class MelaninLizardEntity extends BaseHxHMob implements software.bernie.geckolib.animatable.GeoEntity {

    /** Fase do ataque: o unico estado de combate que o cliente precisa conhecer. */
    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(MelaninLizardEntity.class, EntityDataSerializers.INT);
    /** Cambaleando: sincronizado porque interrupcao invisivel e stagger que so o servidor conhece. */
    private static final EntityDataAccessor<Boolean> CAMBALEANDO =
            SynchedEntityData.defineId(MelaninLizardEntity.class, EntityDataSerializers.BOOLEAN);
    /**
     * Camuflado: sincronizado porque a camuflagem E uma pose.
     *
     * <p>Sem este campo o cliente tocaria o clipe de ocio -- um lagarto vivo,
     * respirando -- enquanto o servidor recusa a mira dele. Nao daria erro nenhum,
     * e seria a diferenca entre uma mecanica e uma trapaca.</p>
     */
    private static final EntityDataAccessor<Boolean> CAMUFLADO =
            SynchedEntityData.defineId(MelaninLizardEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int MEMORIA_DE_ALVO_TICKS = 120;
    private static final int TICKS_DE_AVISO = 20;
    private static final double ABERTURA_DA_VISAO = 75.0D;
    private static final double ALCANCE_DE_AUDICAO = 14.0D;
    private static final float FRACAO_DE_VIDA_CRITICA = 0.25F;

    /**
     * De quantos em quantos ticks ele confere se alguem esta olhando.
     *
     * <p>LIMITE DE DESIGN, e nao botao de balanceamento: quatro ticks e um quinto
     * de segundo, abaixo do que se percebe como atraso, e e o que impede uma
     * varredura de mundo por tick por lagarto. Conferir todo tick funcionaria
     * perfeitamente com um bicho e custaria TPS com vinte, sem uma linha no log.
     * O {@code getId()} entra na conta para que dois lagartos do mesmo chunk nao
     * varram no mesmo tick.</p>
     */
    private static final int INTERVALO_DE_OBSERVACAO = 4;

    private static final WeakPointResolver PONTO_FRACO = MelaninLizardTuning.pontoFraco();
    private static final WeakPointRegistry PONTOS_FRACOS = MelaninLizardTuning.pontosFracos();
    private static final RegrasDeCamuflagemDeRocha CAMUFLAGEM = MelaninLizardTuning.camuflagem();

    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache cacheDeAnimacao =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    // Estado DA INSTANCIA. Qualquer um destes num campo static faria todos os
    // lagartos do mundo compartilharem a mesma vitima e o mesmo relogio -- que e o
    // erro que este projeto ja sabe que comete.
    private final GrabController agarrao = new GrabController(MelaninLizardTuning.agarrao(),
            MelaninLizardTuning.ALTURA_MAXIMA_DA_PRESA, MelaninLizardTuning.LARGURA_MAXIMA_DA_PRESA);
    private int ticksSemAlvo;
    private int retiradaRestante;

    public MelaninLizardEntity(EntityType<? extends MelaninLizardEntity> type, Level level) {
        super(type, level, GreedIslandProfiles.melaninLizard().metadata(),
                new AwarenessTuning(TICKS_DE_AVISO, MEMORIA_DE_ALVO_TICKS));
        instalarRuntime(montarRuntime());
    }

    private EnemyRuntime montarRuntime() {
        var perfil = GreedIslandProfiles.melaninLizard();
        PerceptionController percepcao = new PerceptionController(
                PerceptionBudget.padrao(),
                VisionCone.deGraus(perfil.attributes().followRange(), ABERTURA_DA_VISAO),
                new ThreatMemory(MEMORIA_DE_ALVO_TICKS),
                new TargetEvaluator(FactionRelations.padrao(), perfil.metadata().faction(),
                        perfil.attributes().followRange(), perfil.metadata().territorial()),
                PercepcaoDeAura.NENHUMA, ALCANCE_DE_AUDICAO, getUUID().hashCode());
        return new EnemyRuntime(percepcao,
                new EnemyBrain(new AwarenessTuning(TICKS_DE_AVISO, MEMORIA_DE_ALVO_TICKS)),
                new AttackController(GreedIslandProfiles.melaninLizardRecarga()),
                new StaggerState(GreedIslandProfiles.melaninLizardStagger()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = GreedIslandProfiles.melaninLizard().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<MelaninLizardEntity> registeredType() { return EnemyEntityTypes.MELANIN_LIZARD.get(); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        builder.define(CAMBALEANDO, false);
        // Nasce PEDRA. Um lagarto que aparece andando nunca enganou ninguem, e a
        // primeira leitura que o jogador faz da especie e a que fica.
        builder.define(CAMUFLADO, true);
    }

    public AttackPhase faseDeAtaque() {
        int ordinal = this.entityData.get(FASE_DE_ATAQUE);
        AttackPhase[] fases = AttackPhase.values();
        return ordinal >= 0 && ordinal < fases.length ? fases[ordinal] : AttackPhase.IDLE;
    }

    public boolean cambaleando() { return this.entityData.get(CAMBALEANDO); }

    /** Camuflado; vale nos DOIS lados, porque vem do SynchedEntityData. */
    public boolean camuflado() { return this.entityData.get(CAMUFLADO); }

    /**
     * Verdade do SERVIDOR sobre haver alguem preso.
     *
     * <p>Nao e sincronizado de proposito: quem precisa dela e a regra de soltura,
     * e ela so vale no servidor. No cliente responde {@code false}, e por isso o
     * clipe de "segurando" e escolhido pela FASE publicada, nunca por aqui.</p>
     */
    public boolean estaAgarrando() { return agarrao.agarrando(); }

    /**
     * Camuflado ele nao e ALVO -- nem da mira automatica, nem de outro mob.
     *
     * <p>E aqui, e nao em {@code isAttackable()}. A diferenca e a mecanica
     * inteira: {@code isAttackable()} falso tornaria o lagarto IMPOSSIVEL DE
     * ACERTAR, e ai a regra "dano quebra a camuflagem" viraria codigo morto --
     * ninguem conseguiria produzir o dano que a quebra exige. O que se nega aqui
     * e a varredura automatica de inimigos, que e o que significa "ele e
     * paisagem". Quem VIU o bicho e bate nele acerta, e o acerto acorda a pedra.</p>
     */
    @Override
    public boolean canBeSeenAsEnemy() {
        return CAMUFLAGEM.podeSerAlvo(camuflado()) && super.canBeSeenAsEnemy();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BoteGoal(this));
        goalSelector.addGoal(2, new PerseguirGoal(this));
        // As goals de paisagem sao envelopadas para respeitar a camuflagem. Sem
        // isso a pedra passearia: o tick de servidor para a navegacao, a goal a
        // reinicia no tick seguinte, e o resultado e um bicho que se arrasta
        // alguns pixels por segundo enquanto o cliente desenha a pose parada.
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D) {
            @Override public boolean canUse() {
                return CAMUFLAGEM.podeMover(camuflado()) && super.canUse();
            }

            @Override public boolean canContinueToUse() {
                return CAMUFLAGEM.podeMover(camuflado()) && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F) {
            @Override public boolean canUse() { return !camuflado() && super.canUse(); }

            @Override public boolean canContinueToUse() {
                return !camuflado() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(8, new RandomLookAroundGoal(this) {
            @Override public boolean canUse() { return !camuflado() && super.canUse(); }

            @Override public boolean canContinueToUse() {
                return !camuflado() && super.canContinueToUse();
            }
        });
        // NENHUM target goal vanilla: quem escolhe alvo aqui e o TargetEvaluator,
        // por faccao. Um NearestAttackableTargetGoal ao lado seria uma segunda
        // autoridade sobre a mesma decisao, e as duas discordariam em silencio.
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;
        if (retiradaRestante > 0) retiradaRestante--;
        EnemyRuntime runtime = runtimeExigido();
        PerceptionSnapshot snapshot = runtime.tick(tickCount, this::varrerCandidatos,
                getHealth() <= getMaxHealth() * FRACAO_DE_VIDA_CRITICA, camuflado());

        // O agarrao e tickado AQUI, e nao dentro da Goal, porque a Goal pode ser
        // preemptada por outra de prioridade maior (a FloatGoal, por exemplo). Se o
        // relogio do agarrao dependesse da Goal viva, um mergulho na agua deixaria
        // a vitima presa com o relogio parado -- sem erro nenhum no log.
        tickDoAgarrao();
        tickDaCamuflagem();
        if (!camuflado()) aplicarAlvo(snapshot);
        tickDoBote(runtime);
        publicarEstado(runtime);
    }

    // ----------------------------------------------------------- camuflagem

    private void tickDaCamuflagem() {
        if (camuflado()) {
            // Pedra nao navega e nao desliza. O eixo Y fica intocado de proposito:
            // zerar a queda faria o lagarto flutuar se o bloco sob ele sumisse.
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
            if (getTarget() != null) setTarget(null);
            if ((tickCount + getId()) % INTERVALO_DE_OBSERVACAO != 0) return;
            Player observador = quemEstaOlhando();
            if (CAMUFLAGEM.quebra(true, observador != null, false)) revelar(observador);
            return;
        }
        boolean temAlvo = getTarget() != null && getTarget().isAlive();
        ticksSemAlvo = temAlvo ? 0 : ticksSemAlvo + 1;
        // Recem-solto ele nao vira pedra: sumir da paisagem no mesmo segundo em que
        // cuspiu alguem tiraria da vitima a unica chance de revidar.
        if (retiradaRestante > 0) return;
        if (CAMUFLAGEM.recamufla(false, temAlvo, agarrao.agarrando(), ticksSemAlvo)) camuflar();
    }

    /**
     * O primeiro jogador que esta OLHANDO para este lagarto, de perto.
     *
     * <p>A caixa e pequena -- o raio da propria regra -- e a varredura so roda a
     * cada {@link #INTERVALO_DE_OBSERVACAO} ticks, e so enquanto ele esta
     * camuflado, que e quando ele nao faz mais nada.</p>
     */
    private Player quemEstaOlhando() {
        AABB zona = getBoundingBox().inflate(CAMUFLAGEM.distanciaDeQuebra());
        for (Player jogador : level().getEntitiesOfClass(Player.class, zona,
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative())) {
            if (CAMUFLAGEM.observado(hasLineOfSight(jogador), distanceTo(jogador),
                    cossenoDoOlharDe(jogador))) {
                return jogador;
            }
        }
        return null;
    }

    /**
     * Quanto o olhar do OBSERVADOR aponta para este lagarto.
     *
     * <p>Nao e o mesmo numero de {@link #cossenoDoOlharAte}: aquele mede para onde
     * o BICHO olha. Trocar os dois nao da erro nenhum -- daria uma camuflagem que
     * quebra quando o lagarto encara o jogador, ou seja, uma pedra que se denuncia
     * sozinha.</p>
     */
    private double cossenoDoOlharDe(Entity observador) {
        Vec3 olhar = observador.getLookAngle();
        Vec3 horizontal = new Vec3(olhar.x, 0.0D, olhar.z);
        Vec3 ateMim = new Vec3(getX() - observador.getX(), 0.0D, getZ() - observador.getZ());
        if (horizontal.lengthSqr() < 1.0E-6D || ateMim.lengthSqr() < 1.0E-6D) return 0.0D;
        return Mth.clamp(horizontal.normalize().dot(ateMim.normalize()), -1.0D, 1.0D);
    }

    private void camuflar() {
        this.entityData.set(CAMUFLADO, true);
        setTarget(null);
        getNavigation().stop();
        ticksSemAlvo = 0;
        if (combatState() != EnemyCombatState.DYING) combatState(EnemyCombatState.IDLE);
    }

    /** Saida UNICA da camuflagem: olhar, dano e qualquer revelacao futura passam aqui. */
    private void revelar(LivingEntity quemAcordou) {
        if (!camuflado()) return;
        this.entityData.set(CAMUFLADO, false);
        ticksSemAlvo = 0;
        if (quemAcordou != null && quemAcordou.isAlive()) setTarget(quemAcordou);
        if (combatState() != EnemyCombatState.DYING) combatState(EnemyCombatState.AGGRO);
    }

    // ----------------------------------------------------------------- alvo

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

    /** Varredura de mundo -- a UNICA da percepcao, e so quando o orcamento autoriza. */
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

    /** 1 quando o alvo esta bem na frente DO LAGARTO, -1 quando esta atras dele. */
    private double cossenoDoOlharAte(Entity alvo) {
        Vec3 olhar = getLookAngle();
        Vec3 horizontal = new Vec3(olhar.x, 0.0D, olhar.z);
        Vec3 ateOAlvo = new Vec3(alvo.getX() - getX(), 0.0D, alvo.getZ() - getZ());
        if (horizontal.lengthSqr() < 1.0E-6D || ateOAlvo.lengthSqr() < 1.0E-6D) return 0.0D;
        return Mth.clamp(horizontal.normalize().dot(ateOAlvo.normalize()), -1.0D, 1.0D);
    }

    // ----------------------------------------------------------------- bote

    /** A janela ACTIVE consulta a caixa do golpe; fora dela o golpe nao existe. */
    private void tickDoBote(EnemyRuntime runtime) {
        if (runtime.ataques().phase() != AttackPhase.ACTIVE) return;
        LivingEntity alvo = getTarget();
        if (alvo == null || !alvo.isAlive()) return;

        Optional<AttackHit> acerto = runtime.ataques().tryHit(alvo.getId(), alvo.getBoundingBox(),
                MelaninLizardTuning.CAIXA_DO_BOTE, position(), getYRot(), "corpo");
        if (acerto.isEmpty()) return;

        // AGARRA PRIMEIRO, MORDE DEPOIS -- e nao o contrario. Condicionar o agarrao
        // ao dano parece natural e quebra o mob em dois casos reais: no PACIFICO o
        // dano de mob contra jogador e zerado e hurt() devolve false, e durante os
        // ticks de invulnerabilidade de um golpe anterior tambem. Nos dois o lagarto
        // morderia para sempre sem nunca prender ninguem.
        boolean prendeu = tentarAgarrar(alvo);
        alvo.hurt(damageSources().mobAttack(this), acerto.get().damage());
        // Empurrao SO em quem escapou. Empurrar quem acabou de ser preso o
        // arrancaria da propria mordida, e o sintoma seria um agarrao que "as vezes
        // nao pega" -- sem nada no log.
        if (!prendeu) {
            alvo.knockback(MelaninLizardTuning.BOTE_KNOCKBACK,
                    getX() - alvo.getX(), getZ() - alvo.getZ());
        }
    }

    /**
     * Prende o alvo, ou recusa com motivo.
     *
     * <p>A RECUSA VEM ANTES DO EFEITO. Tentar montar primeiro e descobrir depois
     * que o alvo nao cabe funcionaria, e deixaria o porque invisivel: o relato de
     * bug seria "as vezes ele me morde e nao me prende", e ninguem descobriria que
     * a diferenca era o tamanho do alvo.</p>
     */
    private boolean tentarAgarrar(LivingEntity alvo) {
        if (agarrao.agarrando() || !presaValida(alvo)) return false;
        GrabRefusal recusa = agarrao.podeAgarrar(alvo.getBbHeight(), alvo.getBbWidth(),
                alvo.isPassenger(), true);
        if (recusa != GrabRefusal.NENHUMA) return false;
        if (!alvo.startRiding(this, true)) return false;
        agarrao.agarrar(alvo.getUUID());
        return true;
    }

    /**
     * Presa legitima: viva, atacavel, nao aliada, nao outro lagarto e nao alguem
     * que ja esteja montado em outra coisa.
     *
     * <p>O filtro de espectador e criativo esta aqui, e nao no dano: prender um
     * espectador nao daria erro, so travaria a camera de quem so queria
     * assistir.</p>
     */
    private boolean presaValida(LivingEntity candidato) {
        if (candidato == null || candidato == this) return false;
        if (candidato.getType() == getType()) return false;
        if (!candidato.isAlive() || !candidato.isAttackable() || isAlliedTo(candidato)) return false;
        if (candidato.isPassenger()) return false;
        return !(candidato instanceof Player jogador && (jogador.isCreative() || jogador.isSpectator()));
    }

    // -------------------------------------------------------------- agarrao

    private void tickDoAgarrao() {
        LivingEntity vitima = vitimaAgarradaAgora();
        if (!agarrao.agarrando()) {
            // RELOAD. O vanilla restaura passageiros salvos, mas o agarrao inteiro --
            // vitima, relogio e dano acumulado -- e runtime e nao sobrevive ao save.
            // Sem esta linha o jogador volta montado num lagarto que nao sabe que o
            // prendeu: sem pulso de dano, sem soltura, sem erro nenhum no log.
            if (!getPassengers().isEmpty()) ejectPassengers();
            return;
        }
        // Segurando alguem ele nao anda: arrastar a vitima pelo mundo faria a
        // soltura acontecer a metros de onde o agarrao comecou.
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));

        // "Ainda presa" e medido no MUNDO e entregue ao controlador; ele nao
        // consulta nada. A vitima pode sumir por fora do nosso ciclo (logout,
        // /kill, outro mod a desmontou), e sem esta medida o lagarto ficaria
        // agarrando um fantasma com o relogio correndo sozinho ate o fim.
        boolean aindaPresa = vitima != null && !vitima.isRemoved() && vitima.getVehicle() == this;
        GrabController.GrabTick resultado = agarrao.tick(
                vitima != null && vitima.isAlive(), aindaPresa, isAlive());

        if (resultado.pulsoDeDano() && vitima != null) {
            vitima.hurt(damageSources().mobAttack(this), agarrao.regras().danoPorPulso());
        }
        if (resultado.soltou()) soltar(resultado.soltura());
    }

    /**
     * A vitima AGORA, reconstruida do uuid a cada uso.
     *
     * <p>Guardar a entidade num campo nao dava erro; so impedia o objeto de morrer.
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
     * SAIDA UNICA do agarrao.
     *
     * <p>Tempo, dano, morte da vitima, morte do lagarto, remocao da entidade e
     * troca de dimensao terminam TODOS aqui. Se um caminho deixar de passar por
     * aqui, o jogador fica preso para sempre -- e isso nao aparece como erro,
     * aparece como um relato de bug impossivel de reproduzir.</p>
     */
    private void soltar(GrabRelease motivo) {
        if (!agarrao.agarrando() && getPassengers().isEmpty()) return;
        LivingEntity vitima = vitimaAgarradaAgora();
        // O controlador e zerado ANTES da desmontagem de proposito: qualquer regra
        // que recuse desmontar enquanto o lagarto disser que agarra incluiria ESTA
        // desmontagem.
        if (agarrao.agarrando()) agarrao.soltar(motivo);
        if (vitima != null && vitima.getVehicle() == this) vitima.stopRiding();
        ejectPassengers();
        if (vitima != null && tocaMundo(motivo)) reposicionar(vitima);
        retiradaRestante = MelaninLizardTuning.TICKS_DE_RETIRADA;
        ticksSemAlvo = 0;
    }

    /**
     * Saidas que NAO podem tocar em mundo.
     *
     * <p>UNLOAD e DIMENSAO acontecem quando o mundo de origem esta saindo de cena:
     * procurar bloco livre ou teleportar ali mexe num nivel que ja nao e o da
     * vitima. O {@link GrabRelease} documenta isso, e o preco de ignorar nao e uma
     * excecao -- e uma vitima teleportada para a coordenada de outra dimensao.</p>
     */
    private static boolean tocaMundo(GrabRelease motivo) {
        return motivo != GrabRelease.UNLOAD && motivo != GrabRelease.DIMENSAO;
    }

    /**
     * Onde a vitima cai -- {@link SafeReleaseSpot} decide, o servidor mede.
     *
     * <p>Sem lugar livre nenhum ela fica ONDE ESTA, e a desmontagem ja aconteceu.
     * O javadoc do {@code SafeReleaseSpot} sugere tentar de novo no proximo tick;
     * aqui nao se adia, e a razao fica escrita para nao virar descuido: adiar
     * exigiria manter vivo um agarrao que ja terminou, e "jogador preso para
     * sempre" e pior do que "jogador dentro do lagarto por um tick" -- o empurrao
     * natural das entidades resolve o segundo sozinho.</p>
     */
    private void reposicionar(LivingEntity vitima) {
        Vec3 olhar = getLookAngle();
        Vec3 saida = new Vec3(olhar.x, 0.0D, olhar.z);
        // Vetor nulo vira NaN ao normalizar, e o SafeReleaseSpot recusa com excecao.
        // O reserva e a frente do mundo: qualquer direcao serve melhor do que
        // derrubar o tick de servidor.
        if (saida.lengthSqr() < 1.0E-6D) saida = new Vec3(0.0D, 0.0D, 1.0D);
        SafeReleaseSpot.escolher(position(), saida, MelaninLizardTuning.DISTANCIA_DE_SOLTURA,
                        MelaninLizardTuning.ALTURA_DE_ESCAPE, ponto -> cabeEm(vitima, ponto))
                .ifPresent(ponto -> vitima.teleportTo(ponto.x, ponto.y, ponto.z));
    }

    /** O teste de espaco livre, medido por quem tem o servidor na mao. */
    private boolean cabeEm(LivingEntity vitima, Vec3 ponto) {
        AABB caixa = vitima.getBoundingBox().move(ponto.x - vitima.getX(),
                ponto.y - vitima.getY(), ponto.z - vitima.getZ());
        return level().noCollision(vitima, caixa);
    }

    /**
     * A vitima fica presa NO CHAO, sob a mandibula -- nao sentada em cima.
     *
     * <p>Em 1.21.1 este e o ponto real: {@code positionRider} deriva a posicao de
     * {@code getPassengerRidingPosition}, que chama este metodo. A fracao e a mesma
     * que o gerador de geometria confere contra a caixa da boca; girada so de um
     * lado, o preso flutua ao lado de uma mandibula que nao segura nada.</p>
     */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0.0D, dimensions.height() * MelaninLizardTuning.FRACAO_DE_ENCAIXE_DA_VITIMA, 0.0D);
    }

    // ------------------------------------------------------------------ dano

    /**
     * UNICO lugar que aplica o multiplicador do ponto fraco, alimenta o stagger,
     * acumula o dano de escape e acorda a pedra.
     *
     * <p>Dano multiplicado em dois handlers e o bug silencioso que este projeto ja
     * sabe que comete: o numero final fica plausivel demais para alguem notar sem
     * medir. A regiao e resolvida com a geometria QUE O SERVIDOR TEM; o cliente
     * nao participa.</p>
     *
     * <p>O que conta para o escape e o dano PEDIDO, antes de armadura: o preco da
     * soltura e o esforco de quem bate, nao a defesa do lagarto.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return super.hurt(source, amount);
        boolean agarrandoAntes = agarrao.agarrando();
        if (!(source.getEntity() instanceof LivingEntity atacante)
                || !Float.isFinite(amount) || amount <= 0.0F) {
            // Dano sem atacante (fogo, queda, cacto) nao tem angulo: cai na regiao
            // padrao, passa sem multiplicador e tambem sem stagger -- cambalear por
            // queimadura transformaria fogo em interrupcao permanente. Ele AINDA
            // assim acorda a pedra: um lagarto pegando fogo parado nao e camuflagem.
            boolean levouSemAtacante = super.hurt(source, amount);
            if (levouSemAtacante) revelar(null);
            return levouSemAtacante;
        }

        Vec3 impacto = pontoDeImpacto(source, atacante);
        double alturaRelativa = Mth.clamp((impacto.y - getY()) / getBbHeight(), 0.0D, 1.0D);
        String regiao = PONTO_FRACO.resolver(alturaRelativa, cossenoDoOlharAte(atacante));
        float danoFinal = PONTOS_FRACOS.damage(amount, regiao);

        boolean levou = super.hurt(source, danoFinal);
        if (!levou) return false;

        if (agarrandoAntes) agarrao.registrarDanoNoPredador(amount);
        EnemyRuntime runtime = runtimeExigido();
        // O stagger le o dano REAL ja multiplicado: acertar o olho interrompe mais
        // depressa, que e a resposta que este bicho ensina.
        StaggerResult resultado = runtime.sofrerStagger(runtime.ataques().attackInstanceId(),
                danoFinal, MelaninLizardTuning.RECARGA_APOS_INTERRUPCAO);
        if (resultado == StaggerResult.DISPAROU) {
            getNavigation().stop();
            this.entityData.set(CAMBALEANDO, true);
            this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        }
        revelar(atacante);
        return true;
    }

    /**
     * Ponto de impacto medido NO SERVIDOR: traco do olho do atacante contra a caixa.
     *
     * <p>Sem intersecao, cai para a posicao do dano (ou o olho do atacante) --
     * nunca para algo que o cliente afirmou.</p>
     */
    private Vec3 pontoDeImpacto(DamageSource source, LivingEntity atacante) {
        Vec3 olho = atacante.getEyePosition();
        Vec3 fim = olho.add(atacante.getLookAngle().scale(MelaninLizardTuning.ALCANCE_DO_TRACO));
        Vec3 reserva = source.getSourcePosition() != null ? source.getSourcePosition() : olho;
        return getBoundingBox().clip(olho, fim).orElse(reserva);
    }

    // -------------------------------------------------------- ciclo de vida

    /**
     * Morrer SOLTA quem estava preso e PUBLICA o repouso.
     *
     * <p>Depois de morto o passo de IA nao roda mais e o campo sincronizado fica
     * onde estava: o bicho morreria com a pose do bote travada no cliente e a
     * vitima presa a um cadaver. Nao ha erro nisso -- ha um jogador dentro de um
     * lagarto morto.</p>
     */
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            soltar(GrabRelease.PREDADOR_MORTO);
            publicarRepouso();
        }
        super.die(source);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide) {
            soltar(GrabRelease.UNLOAD);
            publicarRepouso();
        }
        super.remove(reason);
    }

    /**
     * Troca de dimensao solta ANTES de viajar.
     *
     * <p>O vanilla desmonta os passageiros ao mudar de dimensao; sem passar por
     * aqui, o lagarto chegaria do outro lado achando que ainda segura alguem, e
     * todo agarrao seguinte seria recusado para sempre por JA_AGARRANDO.</p>
     */
    @Override
    public Entity changeDimension(DimensionTransition transition) {
        if (!level().isClientSide) soltar(GrabRelease.DIMENSAO);
        return super.changeDimension(transition);
    }

    private void publicarRepouso() {
        this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        this.entityData.set(CAMBALEANDO, false);
        // Ele NAO volta a ser pedra ao morrer: um lagarto morto na pose de pedra e
        // indistinguivel de um lagarto vivo camuflado, e quem precisa captura-lo
        // VIVO perderia a unica leitura que diz se ele passou do ponto.
        this.entityData.set(CAMUFLADO, false);
    }

    /** Publica ao cliente SO o que ele precisa para escolher o clipe. */
    private void publicarEstado(EnemyRuntime runtime) {
        // Segurando alguem, a fase publicada e RECOVERY pelo agarrao INTEIRO -- que
        // dura mais do que a linha do tempo do ataque. Ler a fase do controlador
        // aqui faria o lagarto voltar para o ocio com a vitima na boca.
        int fase = agarrao.agarrando()
                ? AttackPhase.RECOVERY.ordinal()
                : runtime.ataques().phase().ordinal();
        if (this.entityData.get(FASE_DE_ATAQUE) != fase) this.entityData.set(FASE_DE_ATAQUE, fase);
        boolean cambaleando = runtime.stagger().cambaleando();
        if (this.entityData.get(CAMBALEANDO) != cambaleando) {
            this.entityData.set(CAMBALEANDO, cambaleando);
        }
        if (combatState() != EnemyCombatState.DYING) {
            combatState(cambaleando ? EnemyCombatState.STAGGERED
                    : getTarget() != null ? EnemyCombatState.AGGRO : EnemyCombatState.IDLE);
        }
    }

    // ------------------------------------------------------------- animacao

    /**
     * O CLIENTE NAO DECIDE NADA: ele le camuflagem, cambaleio e fase, os tres
     * vindos do SynchedEntityData.
     *
     * <p>Ordem de precedencia, da mais especifica para a menos: morte, cambaleio,
     * fases do bote, camuflagem, locomocao, ocio. A camuflagem vem DEPOIS das
     * fases de ataque de proposito: se as duas pudessem valer ao mesmo tempo, uma
     * pedra que morde seria pior do que um lagarto que anda.</p>
     */
    @Override
    public void registerControllers(
            software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<MelaninLizardEntity>(
                this, "corpo", TRANSICAO_EM_TICKS, estado -> {
                    if (this.deathTime > 0) return estado.setAndContinue(DEATH);
                    if (cambaleando()) return estado.setAndContinue(STAGGER);
                    return switch (faseDeAtaque()) {
                        case WINDUP -> estado.setAndContinue(WINDUP);
                        case ACTIVE -> estado.setAndContinue(STRIKE);
                        case RECOVERY -> estado.setAndContinue(RECOVERY);
                        default -> {
                            if (camuflado()) yield estado.setAndContinue(CAMOUFLAGE);
                            yield estado.setAndContinue(
                                    velocidadeHorizontal() >= LIMIAR_DE_CAMINHADA ? WALK : IDLE);
                        }
                    };
                }));
    }

    /**
     * Ticks de mistura entre um clipe e o proximo.
     *
     * <p>Tres, e nao cinco: a janela ACTIVE do bote dura 5 ticks. Uma transicao
     * mais longa do que a fase que ela atravessa comeria a mordida inteira em
     * mistura, e o jogador preso nunca veria a boca fechar.</p>
     */
    private static final int TRANSICAO_EM_TICKS = 3;

    // Os nomes sao um CONTRATO com melanin_lizard.animation.json. Errar um deles nao da erro:
    // o GeckoLib nao acha o clipe e deixa o osso parado. LoopType.DEFAULT em todos --
    // o tipo de repeticao mora no arquivo, e cravar em Java o faria vencer o JSON.
    private static final software.bernie.geckolib.animation.RawAnimation IDLE =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.melanin_lizard.idle", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation WALK =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.melanin_lizard.walk", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation CAMOUFLAGE =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.melanin_lizard.camouflage", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation WINDUP =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.melanin_lizard.windup", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation STRIKE =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.melanin_lizard.strike", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation RECOVERY =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.melanin_lizard.recovery", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation STAGGER =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.melanin_lizard.stagger", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation DEATH =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.melanin_lizard.death", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);

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

    // ----------------------------------------------------------------- goals

    /**
     * Anda ate o alvo enquanto ele estiver longe demais para o bote.
     *
     * <p>{@code static} de proposito: todo estado que ela le mora na ENTIDADE. Um
     * campo aqui seria compartilhado por todos os lagartos do mundo, e o sintoma
     * seria um deles perseguindo o alvo de outro.</p>
     */
    private static final class PerseguirGoal extends Goal {
        private final MelaninLizardEntity lagarto;

        private PerseguirGoal(MelaninLizardEntity lagarto) {
            this.lagarto = lagarto;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (lagarto.camuflado() || lagarto.estaAgarrando()) return false;
            LivingEntity alvo = lagarto.getTarget();
            return alvo != null && alvo.isAlive()
                    && !lagarto.runtimeExigido().stagger().cambaleando()
                    && lagarto.runtimeExigido().ataques().phase() == AttackPhase.IDLE
                    && lagarto.distanceTo(alvo) > MelaninLizardTuning.ALCANCE_DO_BOTE;
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void tick() {
            LivingEntity alvo = lagarto.getTarget();
            if (alvo == null) return;
            lagarto.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            lagarto.getNavigation().moveTo(alvo, 1.0D);
        }

        @Override public void stop() { lagarto.getNavigation().stop(); }
    }

    /** Dispara e conduz o bote; quem mede a janela e o {@link AttackController}. */
    private static final class BoteGoal extends Goal {
        private final MelaninLizardEntity lagarto;

        private BoteGoal(MelaninLizardEntity lagarto) {
            this.lagarto = lagarto;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (lagarto.level().isClientSide) return false;
            // Camuflado ele nao ataca: a pedra que bota abre o bote sem ter dado o
            // aviso, e o telegrafo e o unico jeito de o jogador ler este mob.
            if (lagarto.camuflado() || lagarto.estaAgarrando()) return false;
            // Recem-solto ele tambem nao: sem esta pausa o mesmo tick que cospe ja
            // recomecaria a morder, e quem escapou nao chegaria a ver que escapou.
            if (lagarto.retiradaRestante > 0) return false;
            EnemyRuntime runtime = lagarto.runtimeExigido();
            if (runtime.stagger().cambaleando()) return false;
            LivingEntity alvo = lagarto.getTarget();
            if (alvo == null || !alvo.isAlive()) return false;
            return lagarto.distanceTo(alvo) <= MelaninLizardTuning.ALCANCE_DO_BOTE
                    && lagarto.hasLineOfSight(alvo)
                    && runtime.ataques().canStart();
        }

        @Override public boolean canContinueToUse() {
            EnemyRuntime runtime = lagarto.runtimeExigido();
            return runtime.ataques().phase() != AttackPhase.IDLE
                    && runtime.ataques().phase() != AttackPhase.COMPLETE
                    && !runtime.stagger().cambaleando();
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() {
            lagarto.getNavigation().stop();
            // O dano sai do ATRIBUTO, lido agora. Congelado numa constante, todo buff
            // e todo ajuste de perfil sumiriam sem aviso.
            lagarto.runtimeExigido().ataques().start(MelaninLizardTuning.bote(
                    (float) lagarto.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        }

        @Override public void tick() {
            LivingEntity alvo = lagarto.getTarget();
            // Encarar o alvo so ate o fim do WINDUP: a partir da janela ACTIVE a
            // direcao esta TRAVADA. Sem travar, o bote viraria mira-laser -- o
            // lagarto giraria junto com quem desvia, e o desvio deixaria de existir.
            if (alvo != null && lagarto.runtimeExigido().ataques().phase() == AttackPhase.WINDUP) {
                lagarto.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            }
            lagarto.setDeltaMovement(lagarto.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
        }

        @Override public void stop() { lagarto.getNavigation().stop(); }
    }
}
