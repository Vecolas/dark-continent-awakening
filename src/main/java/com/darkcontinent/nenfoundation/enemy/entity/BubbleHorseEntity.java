package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.DecisaoDeExaustaoDeBolha;
import com.darkcontinent.nenfoundation.enemy.ai.DecisaoDeSaltoDeBolha;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeExaustaoDeBolha;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeSaltoDeBolha;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.base.BaseHxHMob;
import com.darkcontinent.nenfoundation.enemy.base.EnemyRuntime;
import com.darkcontinent.nenfoundation.enemy.combat.AttackController;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHit;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import com.darkcontinent.nenfoundation.enemy.content.BubbleHorseTuning;
import com.darkcontinent.nenfoundation.enemy.content.GreedIslandProfiles;
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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Bubble Horse -- criatura de Greed Island.
 *
 * <p>O cavalo que so vale VIVO. A condicao de captura dele e nao-letal: matar cancela o card, e e isso que faz o jogador ter de parar de bater.</p>
 *
 * <p><b>O QUE ELE FAZ.</b> Ele <b>foge</b>, e a fuga tem compasso: a cada ciclo
 * de vinte ticks ele passa doze parado e oito no ar, num impulso -- nunca numa
 * caminhada. Essa pausa e a mecanica: e nela que o jogador ve as bolhas
 * acharem o chao e consegue ANTECIPAR onde o proximo salto cai. Perseguir um
 * bicho de velocidade 0.45 nao funciona; cortar a linha do salto, sim. A regra
 * mora em {@link RegrasDeSaltoDeBolha}, fora daqui, e a locomocao NAO passa por
 * {@code PathNavigation}: navegacao entregaria um cavalo que desliza suavemente
 * para longe, sem um unico quadro em que alguem possa ler a intencao dele.</p>
 *
 * <p><b>ELE NAO PROCURA BRIGA.</b> Os 3 de dano sao o coice de quem foi
 * encurralado: {@link RegrasDeSaltoDeBolha#coiceia} so aprova o golpe quando o
 * servidor mediu que a fuga parou de sair do lugar. E por isso que o alcance do
 * coice e obrigatoriamente menor que a distancia de conforto -- invertidos, os
 * dois numeros dariam um perseguidor de dano 3 que ninguem projetou, e nenhum
 * portao veria isso.</p>
 *
 * <p><b>A JANELA DE CAPTURA E O BICHO INTEIRO.</b> Abaixo do limiar ele para de
 * fugir e fica EXAUSTO por um prazo ({@link RegrasDeExaustaoDeBolha}). Esse
 * prazo <em>e</em> a captura. Quem continuar batendo mata o premio -- e a perda e
 * anunciada, porque perda que o jogador nao ve e licao que ele nao aprende: ele
 * simplesmente nao recebe um card que talvez nem soubesse que existia. O limiar
 * NAO e escrito aqui: ele e lido da condicao de captura publicada, para que o
 * ponto em que o cavalo para seja o mesmo ponto em que o card passa a valer.</p>
 *
 * <p><b>Quem paga o card e o {@code CardConversionService}</b>, com a trava de
 * recompensa do encontro. Esta classe nao emite card nenhum: ela publica estado e
 * torna o desfecho legivel. Decidir o card aqui seria a segunda autoridade sobre
 * a mesma verdade, e as duas discordariam em silencio.</p>
 *
 * <p>Ela usa {@link EnemyRuntime} por inteiro -- percepcao, cerebro, ataque e
 * stagger -- para que o comportamento proprio entre SOBRE a fundacao, e nao ao
 * lado dela.</p>
 */
public final class BubbleHorseEntity extends BaseHxHMob implements software.bernie.geckolib.animatable.GeoEntity {

    /** Fase do ataque: o unico estado de combate que o cliente precisa conhecer. */
    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(BubbleHorseEntity.class, EntityDataSerializers.INT);
    /** Cambaleando: sincronizado porque interrupcao invisivel e stagger que so o servidor conhece. */
    private static final EntityDataAccessor<Boolean> CAMBALEANDO =
            SynchedEntityData.defineId(BubbleHorseEntity.class, EntityDataSerializers.BOOLEAN);
    /**
     * Exausto: a janela de captura esta aberta.
     *
     * <p>Sincronizado porque e a UNICA coisa que o jogador precisa enxergar para
     * parar de bater na hora certa. Mantido so no servidor, o cavalo pararia na
     * tela sem nada que distinguisse "estou entregue" de "estou preso num bloco",
     * e a diferenca entre essas duas leituras e um card.</p>
     */
    private static final EntityDataAccessor<Boolean> EXAUSTO =
            SynchedEntityData.defineId(BubbleHorseEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int MEMORIA_DE_ALVO_TICKS = 120;
    private static final int TICKS_DE_AVISO = 20;
    private static final double ABERTURA_DA_VISAO = 75.0D;
    private static final double ALCANCE_DE_AUDICAO = 14.0D;

    /**
     * As duas regras, montadas UMA vez.
     *
     * <p>{@code BubbleHorseTuning.exaustao()} le a tabela de capturas, e essa
     * tabela e reconstruida a cada chamada -- sete {@code ResourceLocation} novos.
     * Monta-la dentro do tick nao daria erro nenhum: daria lixo por tick por
     * cavalo, e o sintoma seria o TPS caindo devagar numa manada, sem nada
     * apontando para ca.</p>
     */
    private static final RegrasDeSaltoDeBolha SALTO = BubbleHorseTuning.salto();
    private static final RegrasDeExaustaoDeBolha EXAUSTAO = BubbleHorseTuning.exaustao();

    /**
     * A vida em que o cerebro ja considera fuga -- o MESMO limiar do colapso.
     *
     * <p>Derivado, e nao escrito de novo: um segundo numero aqui faria o cavalo
     * mudar de postura num ponto e parar de fugir noutro, e a leitura do jogador
     * -- "ele esta acabando" -- passaria a apontar para o instante errado.</p>
     */
    private static final float FRACAO_DE_VIDA_CRITICA = (float) EXAUSTAO.fracaoDeVidaDoColapso();

    /**
     * Teto do contador de ticks sem salto.
     *
     * <p>Limite de DESIGN, e nao botao de balanceamento: o contador so e comparado
     * com o dobro de um ciclo, entao qualquer valor acima disso significa a mesma
     * coisa. Sem teto ele cresceria para sempre num cavalo que nunca fugiu, e
     * estouro de {@code int} e o tipo de defeito que aparece uma vez por ano e
     * nunca e reproduzido.</p>
     */
    private static final int TETO_DO_CONTADOR_DE_SALTO = 20_000;

    /** Ticks desde o ultimo impulso aplicado. Estado de SERVIDOR, por instancia. */
    private int ticksDesdeOSalto;
    /** Ticks decorridos desde que a janela de captura abriu. */
    private int ticksNaJanela;
    /** Ticks de carencia que ainda faltam para ele poder exaurir de novo. */
    private int folegoRestante;

    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache cacheDeAnimacao =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    public BubbleHorseEntity(EntityType<? extends BubbleHorseEntity> type, Level level) {
        super(type, level, GreedIslandProfiles.bubbleHorse().metadata(),
                new AwarenessTuning(TICKS_DE_AVISO, MEMORIA_DE_ALVO_TICKS));
        instalarRuntime(montarRuntime());
    }

    private EnemyRuntime montarRuntime() {
        var perfil = GreedIslandProfiles.bubbleHorse();
        PerceptionController percepcao = new PerceptionController(
                PerceptionBudget.padrao(),
                VisionCone.deGraus(perfil.attributes().followRange(), ABERTURA_DA_VISAO),
                new ThreatMemory(MEMORIA_DE_ALVO_TICKS),
                new TargetEvaluator(FactionRelations.padrao(), perfil.metadata().faction(),
                        perfil.attributes().followRange(), perfil.metadata().territorial()),
                PercepcaoDeAura.NENHUMA, ALCANCE_DE_AUDICAO, getUUID().hashCode());
        return new EnemyRuntime(percepcao,
                new EnemyBrain(new AwarenessTuning(TICKS_DE_AVISO, MEMORIA_DE_ALVO_TICKS)),
                new AttackController(GreedIslandProfiles.bubbleHorseRecarga()),
                new StaggerState(GreedIslandProfiles.bubbleHorseStagger()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = GreedIslandProfiles.bubbleHorse().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<BubbleHorseEntity> registeredType() { return EnemyEntityTypes.BUBBLE_HORSE.get(); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        builder.define(CAMBALEANDO, false);
        builder.define(EXAUSTO, false);
    }

    public AttackPhase faseDeAtaque() {
        int ordinal = this.entityData.get(FASE_DE_ATAQUE);
        AttackPhase[] fases = AttackPhase.values();
        return ordinal >= 0 && ordinal < fases.length ? fases[ordinal] : AttackPhase.IDLE;
    }

    public boolean cambaleando() { return this.entityData.get(CAMBALEANDO); }

    /** A janela de captura esta aberta. Vale nos dois lados: vem do SynchedEntityData. */
    public boolean exausto() { return this.entityData.get(EXAUSTO); }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CoiceGoal(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // NENHUMA goal de locomocao, e a ausencia e a ficha deste bicho.
        //
        // Um WaterAvoidingRandomStrollGoal ao lado reescreveria o vetor de
        // movimento a cada tick, e o impulso do salto viraria um tranco que nao
        // sai do lugar: a navegacao e o salto disputam a MESMA grandeza, e a
        // navegacao escreve por ultimo. Isso nao levanta erro nenhum -- da um
        // cavalo que treme e nao foge, com o log limpo.
        //
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
        // A exaustao decide ANTES da fuga: e ela que pode proibir o salto neste
        // mesmo tick. Invertida, o cavalo daria um impulso e so entao descobriria
        // que estava entregue -- e a janela de captura comecaria com o bicho no
        // ar, que e o oposto do sinal que ela existe para dar.
        tickDaExaustao(runtime);
        tickDaFuga(runtime);
        tickDoCoice(runtime);
        publicarEstado(runtime);
    }

    // ------------------------------------------------------------- exaustao

    private void tickDaExaustao(EnemyRuntime runtime) {
        DecisaoDeExaustaoDeBolha decisao = EXAUSTAO.decidir(getHealth(), getMaxHealth(),
                false, exausto(), ticksNaJanela, folegoRestante);
        switch (decisao) {
            case EXAURE -> abrirAJanela(runtime);
            case JANELA_ABERTA -> {
                ticksNaJanela++;
                // Entregue quer dizer PARADO. Sem a frenagem o cavalo deslizaria
                // pelo resto do salto que ja tinha comecado, e o quadro em que ele
                // "desiste" -- que e o sinal inteiro -- chegaria tarde.
                setDeltaMovement(getDeltaMovement().multiply(0.35D, 1.0D, 0.35D));
            }
            case JANELA_FECHOU -> fecharAJanela();
            case RECUPERANDO_O_FOLEGO -> folegoRestante--;
            // ACIMA_DO_LIMIAR e o estado normal e nao faz nada. MORREU_SEM_CARD nao
            // chega aqui -- o passo de IA nao roda em cadaver --, e quem o consulta
            // e die(), com a MESMA regra.
            case ACIMA_DO_LIMIAR, MORREU_SEM_CARD -> { }
        }
    }

    private void abrirAJanela(EnemyRuntime runtime) {
        this.entityData.set(EXAUSTO, true);
        ticksNaJanela = 0;
        setDeltaMovement(getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
        // Um coice em curso morre junto com a fuga: o bicho esta entregue, e um
        // golpe saindo de dentro da janela devolveria ao jogador o motivo para
        // continuar batendo -- que e exatamente o que perde o card.
        runtime.ataques().resetComRecarga(BubbleHorseTuning.RECARGA_APOS_INTERRUPCAO);
        estourarBolhas(10);
        avisar("message.nenfoundation.bubble_horse.exausto");
    }

    private void fecharAJanela() {
        this.entityData.set(EXAUSTO, false);
        ticksNaJanela = 0;
        folegoRestante = BubbleHorseTuning.TICKS_DE_FOLEGO;
        // O contador de salto e zerado junto: sem isso o cavalo sairia da janela
        // ja "encurralado" -- os ciclos parados da janela contariam como fuga que
        // nao saiu do lugar -- e coicearia em vez de fugir, invertendo a ficha
        // dele no pior instante possivel.
        ticksDesdeOSalto = 0;
    }

    // ------------------------------------------------------------------ fuga

    private void tickDaFuga(EnemyRuntime runtime) {
        LivingEntity ameaca = getTarget();
        AttackPhase fase = runtime.ataques().phase();
        boolean emGolpe = fase != AttackPhase.IDLE && fase != AttackPhase.COMPLETE;
        DecisaoDeSaltoDeBolha decisao = SALTO.decidir(exausto(), runtime.stagger().cambaleando(),
                emGolpe, onGround(), ticksDesdeOSalto, distanciaDaAmeaca(ameaca));
        ticksDesdeOSalto = Math.min(ticksDesdeOSalto + 1, TETO_DO_CONTADOR_DE_SALTO);
        if (decisao != DecisaoDeSaltoDeBolha.SALTA || ameaca == null) return;
        impulsionar(ameaca);
        ticksDesdeOSalto = 0;
    }

    /** NaN quando nao ha alvo: "sem medida" e diferente de "distancia zero". */
    private double distanciaDaAmeaca(LivingEntity ameaca) {
        return ameaca != null && ameaca.isAlive() ? distanceTo(ameaca) : Double.NaN;
    }

    /**
     * O impulso, escrito no vetor de movimento -- nunca na navegacao.
     *
     * <p>A direcao e medida do alvo para o bicho, no plano. Vetor degenerado
     * (alguem exatamente em cima dele) cai no proprio olhar: normalizar um vetor
     * de comprimento zero devolve NaN, e um NaN no delta de movimento nao levanta
     * excecao -- ele tira a entidade do mundo, e o relato e "o cavalo sumiu".</p>
     */
    private void impulsionar(LivingEntity ameaca) {
        Vec3 fuga = new Vec3(getX() - ameaca.getX(), 0.0D, getZ() - ameaca.getZ());
        if (fuga.lengthSqr() < 1.0E-6D) {
            Vec3 olhar = getLookAngle();
            fuga = new Vec3(olhar.x, 0.0D, olhar.z);
            if (fuga.lengthSqr() < 1.0E-6D) fuga = new Vec3(0.0D, 0.0D, 1.0D);
        }
        fuga = fuga.normalize().scale(BubbleHorseTuning.IMPULSO_HORIZONTAL);
        setDeltaMovement(fuga.x, BubbleHorseTuning.IMPULSO_VERTICAL, fuga.z);
        // hasImpulse avisa o servidor que a velocidade foi escrita de fora do passo
        // de fisica; sem ele a mudanca pode nao chegar ao cliente neste tick, e o
        // salto aparece um quadro atrasado -- que e justamente o quadro que o
        // jogador usa para decidir para onde correr.
        this.hasImpulse = true;
        // O corpo aponta para onde o salto vai. Deixar o corpo virado para o alvo
        // enquanto o bicho se afasta nao da erro: da um cavalo que desliza de re, e
        // a direcao do proximo salto deixa de ser legivel.
        float yaw = (float) (Mth.atan2(fuga.z, fuga.x) * (180.0D / Math.PI)) - 90.0F;
        setYRot(yaw);
        this.yBodyRot = yaw;
    }

    // ----------------------------------------------------------------- coice

    /**
     * A fuga parou de sair do lugar -- medido pelo SERVIDOR, nunca deduzido.
     *
     * <p>Duas provas, e as duas sao baratas: a colisao horizontal do tick anterior
     * (bateu numa parede) e o contador de ciclos sem impulso (tentou e nao
     * conseguiu -- agua, borda, bloco no caminho). Sem a segunda, um cavalo preso
     * numa cova sem parede lateral apanharia parado ate morrer, sem uma linha de
     * log.</p>
     */
    private boolean saidaBloqueada() {
        return this.horizontalCollision
                || ticksDesdeOSalto > SALTO.cicloEmTicks()
                        * BubbleHorseTuning.CICLOS_PERDIDOS_PARA_ENCURRALAR;
    }

    /** A janela ACTIVE consulta a caixa do coice; fora dela o golpe nao existe. */
    private void tickDoCoice(EnemyRuntime runtime) {
        if (runtime.ataques().phase() != AttackPhase.ACTIVE) return;
        LivingEntity alvo = getTarget();
        if (alvo == null || !alvo.isAlive()) return;
        Optional<AttackHit> acerto = runtime.ataques().tryHit(alvo.getId(), alvo.getBoundingBox(),
                BubbleHorseTuning.CAIXA_DA_PATADA, position(), getYRot(),
                BubbleHorseTuning.REGIAO_COMUM);
        acerto.ifPresent(hit -> {
            alvo.hurt(damageSources().mobAttack(this), hit.damage());
            // O empurrao afasta quem encurralou: sem ele o coice machuca e deixa o
            // cavalo exatamente onde estava -- preso, apanhando, ate morrer.
            alvo.knockback(BubbleHorseTuning.EMPURRAO_DA_PATADA,
                    getX() - alvo.getX(), getZ() - alvo.getZ());
        });
    }

    // ------------------------------------------------------------------ dano

    /**
     * O UNICO ponto que alimenta o stagger.
     *
     * <p>Este bicho nao tem {@code WeakPointResolver}, e a ausencia e deliberada:
     * a captura dele e por enfraquecimento, e um ponto fraco ensinaria o jogador a
     * bater com mais precisao -- que e como o card se perde. Por isso o stagger le
     * o dano COMO ELE ENTROU, sem multiplicador nenhum.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean levou = super.hurt(source, amount);
        if (level().isClientSide || !levou || !Float.isFinite(amount) || amount <= 0.0F) {
            return levou;
        }
        EnemyRuntime runtime = runtimeExigido();
        StaggerResult resultado = runtime.sofrerStagger(runtime.ataques().attackInstanceId(),
                amount, BubbleHorseTuning.RECARGA_APOS_INTERRUPCAO);
        if (resultado == StaggerResult.DISPAROU) {
            this.entityData.set(CAMBALEANDO, true);
            this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        }
        return true;
    }

    // ------------------------------------------------------- alvo e percepcao

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
        if (combatState() != EnemyCombatState.DYING) {
            // RETREAT, e nao AGGRO, e o estado normal deste bicho com alvo a vista:
            // ele nao tem agressao nenhuma a publicar. Publicar AGGRO faria as
            // ferramentas de debug -- e qualquer consumidor futuro -- lerem "ele vem
            // para cima", que e o contrario do que esta acontecendo na tela.
            combatState(cambaleando ? EnemyCombatState.STAGGERED
                    : exausto() ? EnemyCombatState.RETREAT
                    : switch (runtime.ataques().phase()) {
                        case WINDUP -> EnemyCombatState.WINDUP;
                        case ACTIVE -> EnemyCombatState.ACTIVE;
                        case RECOVERY -> EnemyCombatState.RECOVERY;
                        default -> getTarget() != null ? EnemyCombatState.RETREAT
                                : EnemyCombatState.IDLE;
                    });
        }
    }

    // ------------------------------------------------- o desfecho, e o recibo

    /**
     * Morrer PUBLICA o repouso -- e ANUNCIA a perda do card.
     *
     * <p>Depois de morto o passo de IA nao roda mais, e o campo sincronizado fica
     * onde estava: o bicho morreria com a pose do golpe travada no cliente. Nao ha
     * erro nisso -- so um cadaver de patas erguidas.</p>
     *
     * <p>O anuncio usa a MESMA {@link RegrasDeExaustaoDeBolha} do tick, com
     * {@code morreu = true}. Um {@code if} proprio aqui seria a segunda regra
     * sobre a mesma pergunta, e as duas divergiriam no primeiro ajuste -- com o
     * aviso saindo em mortes que ainda pagariam card, ou calando nas que nao
     * pagam.</p>
     */
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            if (EXAUSTAO.decidir(getHealth(), getMaxHealth(), true, exausto(),
                    ticksNaJanela, folegoRestante) == DecisaoDeExaustaoDeBolha.MORREU_SEM_CARD) {
                estourarBolhas(24);
                // So quem MATOU recebe o aviso. Morte por fogo, queda ou afogamento
                // nao tem a quem ensinar nada, e uma mensagem sem destinatario
                // viraria ruido para quem so passava por perto.
                if (source.getEntity() instanceof Player jogador) {
                    jogador.displayClientMessage(
                            Component.translatable("message.nenfoundation.bubble_horse.card_perdido"),
                            true);
                }
            }
            publicarRepouso();
        }
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
        this.entityData.set(EXAUSTO, false);
    }

    /**
     * O estouro visivel das bolhas.
     *
     * <p>Ele acompanha os dois momentos que o jogador precisa ver -- a entrega e a
     * morte -- e nao substitui nenhum dos dois: a silhueta que murcha vem do clipe
     * de animacao, que e geometria e sobrevive ao LOD baixo. Particula aqui e
     * acabamento, e por isso o mob continua legivel sem ela.</p>
     */
    private void estourarBolhas(int quantidade) {
        ((ServerLevel) level()).sendParticles(ParticleTypes.POOF,
                getX(), getY() + getBbHeight() * 0.6D, getZ(), quantidade,
                0.35D, 0.3D, 0.35D, 0.02D);
    }

    /** Avisa quem estava batendo. Desfecho, como recusa, sempre tem mensagem traduzida. */
    private void avisar(String chaveDeTraducao) {
        if (getLastHurtByMob() instanceof Player jogador) {
            jogador.displayClientMessage(Component.translatable(chaveDeTraducao), true);
        }
    }

    // ------------------------------------------------------------- animacao

    @Override
    public void registerControllers(
            software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<BubbleHorseEntity>(
                this, "corpo", 4, estado -> {
                    if (this.deathTime > 0) return estado.setAndContinue(DEATH);
                    if (cambaleando()) return estado.setAndContinue(STAGGER);
                    // Exausto le IDLE mesmo se ainda houver inercia: a janela de
                    // captura e a unica coisa que este bicho precisa comunicar, e um
                    // clipe de salto rodando sobre um cavalo entregue diria o
                    // contrario do que o servidor esta fazendo.
                    if (exausto()) return estado.setAndContinue(IDLE);
                    return switch (faseDeAtaque()) {
                        case WINDUP -> estado.setAndContinue(WINDUP);
                        case ACTIVE -> estado.setAndContinue(STRIKE);
                        case RECOVERY -> estado.setAndContinue(RECOVERY);
                        default -> estado.setAndContinue(
                                velocidadeHorizontal() >= LIMIAR_DE_CAMINHADA ? WALK : IDLE);
                    };
                }));
    }

    // Os nomes sao um CONTRATO com bubble_horse.animation.json. Errar um deles nao da erro:
    // o GeckoLib nao acha o clipe e deixa o osso parado. LoopType.DEFAULT em todos --
    // o tipo de repeticao mora no arquivo, e cravar em Java o faria vencer o JSON.
    private static final software.bernie.geckolib.animation.RawAnimation IDLE =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.bubble_horse.idle", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation WALK =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.bubble_horse.walk", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation WINDUP =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.bubble_horse.windup", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation STRIKE =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.bubble_horse.strike", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation RECOVERY =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.bubble_horse.recovery", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation STAGGER =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.bubble_horse.stagger", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation DEATH =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.bubble_horse.death", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);

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

    /**
     * Dispara e conduz o coice; quem mede a janela e o {@code AttackController}.
     *
     * <p>{@code static} de proposito: todo estado que ela le mora na ENTIDADE. Um
     * campo aqui seria compartilhado por todos os cavalos do mundo, e o sintoma
     * seria um deles coiceando o alvo de outro.</p>
     */
    private static final class CoiceGoal extends Goal {
        private final BubbleHorseEntity cavalo;

        private CoiceGoal(BubbleHorseEntity cavalo) {
            this.cavalo = cavalo;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (cavalo.level().isClientSide) return false;
            EnemyRuntime runtime = cavalo.runtimeExigido();
            if (runtime.stagger().cambaleando()) return false;
            LivingEntity alvo = cavalo.getTarget();
            if (alvo == null || !alvo.isAlive()) return false;
            return SALTO.coiceia(cavalo.exausto(), cavalo.distanceTo(alvo), cavalo.saidaBloqueada())
                    && runtime.ataques().canStart();
        }

        @Override public boolean canContinueToUse() {
            EnemyRuntime runtime = cavalo.runtimeExigido();
            return runtime.ataques().phase() != AttackPhase.IDLE
                    && runtime.ataques().phase() != AttackPhase.COMPLETE
                    && !runtime.stagger().cambaleando()
                    && !cavalo.exausto();
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() {
            // O dano e LIDO do atributo no instante em que o golpe comeca. Congelado
            // numa constante, todo buff, debuff e ajuste de perfil sumiriam sem aviso.
            cavalo.runtimeExigido().ataques().start(BubbleHorseTuning.patada(
                    (float) cavalo.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        }

        @Override public void tick() {
            LivingEntity alvo = cavalo.getTarget();
            // Encarar o alvo so ate o fim do WINDUP: a partir da janela ACTIVE a
            // direcao esta TRAVADA. Sem travar, a empinada viraria mira-laser -- o
            // cavalo giraria junto com quem desvia, e o desvio deixaria de existir.
            if (alvo != null && cavalo.runtimeExigido().ataques().phase() == AttackPhase.WINDUP) {
                cavalo.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            }
            cavalo.setDeltaMovement(cavalo.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
        }
    }
}
