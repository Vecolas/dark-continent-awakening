package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.base.BaseChimeraAnt;
import com.darkcontinent.nenfoundation.enemy.base.EnemyRuntime;
import com.darkcontinent.nenfoundation.enemy.combat.AttackController;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHit;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.DecisaoDeEspacamento;
import com.darkcontinent.nenfoundation.enemy.combat.EstadoDeTeia;
import com.darkcontinent.nenfoundation.enemy.combat.RecusaDaTeia;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeTeia;
import com.darkcontinent.nenfoundation.enemy.combat.SolturaDaTeia;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import com.darkcontinent.nenfoundation.enemy.content.ChimeraProfiles;
import com.darkcontinent.nenfoundation.enemy.content.SpiderWebberTuning;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Spider Webber -- formiga quimera, rank OFFICER. A que PRENDE a distancia.
 *
 * <p><b>O que ela faz, numa frase:</b> ela nao vence pelo golpe, vence pelo TEMPO
 * que tira de voce -- e a resposta a ela nao e bater mais, e CHEGAR PERTO.</p>
 *
 * <p><b>O ataque e a teia, e nao o golpe.</b> Depois de um aviso de trinta ticks
 * com o abdome erguido e a fiandeira mirando, uma area telegrafada de dois blocos
 * e meio de arco se abre a frente e IMOBILIZA quem estiver dentro por tres
 * segundos. O dano e um quinto do atributo -- dois pontos -- de proposito: o
 * perigo nao e o que a teia tira de vida, e sim o jogador parado quando o resto
 * do esquadrao chega. Dano alto faria a teia virar enfeite, porque ninguem
 * repararia no que ela custou.</p>
 *
 * <p><b>A imobilizacao tem DUAS saidas, e as duas sao obrigatorias.</b> O relogio
 * solta quem nao conseguiu fazer nada; oito de dano na fiandeira rasgam o fio
 * antes da hora e soltam quem reagiu. Com so o relogio, a jogada certa seria
 * esperar -- e esperar e exatamente o que o esquadrao quer. Prender sem saida nao
 * e dificuldade: e morte sem resposta, e o plano proibe. As cinco portas de saida
 * vivem em {@link EstadoDeTeia}, inclusive a que mais custa se faltar -- matar a
 * aranha no tick exato em que ela segura alguem.</p>
 *
 * <p><b>Ela MANTEM distancia, e o papel RANGED vale em jogo por causa disso.</b>
 * Fora de nove blocos ela aproxima; dentro de tres e meio ela RECUA e se recusa a
 * atirar. Os dois limiares sao o MESMO par de numeros ({@link RegrasDeTeia}), e e
 * essa identidade que impede o oficial de recuar ate uma distancia em que ele
 * ainda nao atira -- o andar-para-tras-e-para-frente eterno que nao levanta
 * excecao nenhuma. A zona morta e a licao do encontro: encostar nela DESLIGA a
 * teia, e e por isso que ela nao ganhou um golpe corpo-a-corpo de consolo.</p>
 *
 * <p><b>Nen: ela DECIDE, nao calcula.</b> O {@code TacticalNenController} que a
 * base instala devolve uma intencao, e nada nesta classe le aura, aplica custo ou
 * ativa tecnica -- o Nen Foundation e a unica autoridade sobre Nen. Nesta entrega
 * a intencao nao esta ligada a efeito nenhum, e a ausencia esta escrita de
 * proposito: um efeito inventado aqui seria indistinguivel, na tela, de aura de
 * verdade, e o jogador aprenderia uma regra que o nucleo nao tem.</p>
 *
 * <p>Ela usa {@link EnemyRuntime} por inteiro -- percepcao com orcamento, cerebro,
 * ataque e stagger -- e publica ao cliente so a fase e o cambaleio, que e do que o
 * controlador de animacao precisa para escolher o clipe.</p>
 */
public final class SpiderWebberEntity extends BaseChimeraAnt
        implements software.bernie.geckolib.animatable.GeoEntity {

    /** Fase do ataque: o unico estado de combate que o cliente precisa conhecer. */
    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(SpiderWebberEntity.class, EntityDataSerializers.INT);
    /** Cambaleando: sincronizado porque interrupcao invisivel e stagger que so o servidor conhece. */
    private static final EntityDataAccessor<Boolean> CAMBALEANDO =
            SynchedEntityData.defineId(SpiderWebberEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * As pecas imutaveis da ficha, resolvidas uma vez.
     *
     * <p>Estaticas porque NENHUMA delas guarda estado de jogador -- as duas sao
     * records imutaveis. Um campo estatico com estado seria o erro classico deste
     * projeto: duas aranhas decidindo com a mesma memoria. E exatamente por isso
     * que {@link EstadoDeTeia}, que guarda QUEM esta preso, e de instancia.</p>
     */
    private static final RegrasDeTeia REGRAS = SpiderWebberTuning.regras();
    private static final AttackHitbox AREA_DA_TEIA = SpiderWebberTuning.areaDaTeia();

    /**
     * Id da regiao atingida pela teia.
     *
     * <p>Ela nao tem ponto fraco declarado, e este id existe porque
     * {@code AttackController.tryHit} exige um: a regiao e o gancho do
     * multiplicador, e inventar uma regiao aqui abriria a porta para um critico
     * que nenhum catalogo declarou.</p>
     */
    private static final String REGIAO_COMUM = "body";

    /** Chave do aviso que o preso recebe -- e ele diz COMO sair, nao so que prendeu. */
    private static final String MENSAGEM_PRESO = "message.nenfoundation.spider_webber.preso";
    /** Chave do aviso de soltura: sem ele a teia pareceria durar um tempo aleatorio. */
    private static final String MENSAGEM_LIVRE = "message.nenfoundation.spider_webber.livre";

    /** Cache por INSTANCIA; um cache estatico faria todas as aranhas dividirem um clipe. */
    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache cacheDeAnimacao =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    /** A teia DESTA aranha: quem esta preso, ha quanto tempo, e quanto ja apanhou. */
    private final EstadoDeTeia teia = new EstadoDeTeia(REGRAS);

    public SpiderWebberEntity(EntityType<? extends SpiderWebberEntity> type, Level level) {
        super(type, level, ChimeraProfiles.spiderWebber().metadata(),
                new AwarenessTuning(SpiderWebberTuning.TICKS_DE_AVISO,
                        SpiderWebberTuning.MEMORIA_DE_ALVO_TICKS),
                ChimeraProfiles.spiderWebberMolde());
        instalarRuntime(montarRuntime());
    }

    /**
     * Monta as quatro pecas do inimigo.
     *
     * <p>A desfasagem sai do hashCode do uuid, para que um esquadrao inteiro nao
     * varra o mundo no mesmo tick. Travada periodica e pior de diagnosticar do que
     * lentidao constante, porque se parece com rede.</p>
     */
    private EnemyRuntime montarRuntime() {
        var perfil = ChimeraProfiles.spiderWebber();
        PerceptionController percepcao = new PerceptionController(
                PerceptionBudget.padrao(),
                SpiderWebberTuning.coneDeVisao(perfil.attributes().followRange()),
                new ThreatMemory(SpiderWebberTuning.MEMORIA_DE_ALVO_TICKS),
                new TargetEvaluator(FactionRelations.padrao(), perfil.metadata().faction(),
                        perfil.attributes().followRange(), perfil.metadata().territorial()),
                // A porta de aura da formiga continua INERTE enquanto a #126 (Gyo)
                // estiver aberta no nucleo: inventar aqui a regra de quem-ve-o-que
                // seria a segunda autoridade sobre Nen que o CLAUDE.md proibe.
                PercepcaoDeAura.NENHUMA, SpiderWebberTuning.ALCANCE_DE_AUDICAO,
                getUUID().hashCode());
        return new EnemyRuntime(percepcao,
                new EnemyBrain(new AwarenessTuning(SpiderWebberTuning.TICKS_DE_AVISO,
                        SpiderWebberTuning.MEMORIA_DE_ALVO_TICKS)),
                new AttackController(ChimeraProfiles.spiderWebberRecarga()),
                new StaggerState(ChimeraProfiles.spiderWebberStagger()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = ChimeraProfiles.spiderWebber().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<SpiderWebberEntity> registeredType() { return EnemyEntityTypes.SPIDER_WEBBER.get(); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        builder.define(CAMBALEANDO, false);
    }

    /** Fase corrente do lancamento; vale nos dois lados, porque vem do SynchedEntityData. */
    public AttackPhase faseDeAtaque() {
        int ordinal = this.entityData.get(FASE_DE_ATAQUE);
        AttackPhase[] fases = AttackPhase.values();
        return ordinal >= 0 && ordinal < fases.length ? fases[ordinal] : AttackPhase.IDLE;
    }

    public boolean cambaleando() { return this.entityData.get(CAMBALEANDO); }

    /** Ha alguem preso nesta fiandeira agora? Pergunta de SERVIDOR. */
    public boolean prendendo() { return teia.prendendo(); }

    @Override
    protected void registerGoals() {
        // GoalSelector cuida de LOCOMOCAO. A intencao -- quem e o alvo, quando a
        // teia sai -- e do runtime.
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TeiaGoal(this));
        goalSelector.addGoal(2, new EspacamentoGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // NENHUM LookAtPlayerGoal, e a ausencia e deliberada: ele encara o jogador
        // MAIS PROXIMO sem consultar cone nenhum, e a cabeca passaria a acompanhar
        // quem a percepcao ainda nao viu. Como ela vira o corpo para onde olha,
        // isso a faria PERCEBER por causa da animacao -- a tela decidindo o que o
        // servidor enxerga, e nada disso da erro.
        //
        // NENHUM target goal vanilla, pelo motivo de sempre: quem escolhe alvo aqui
        // e o TargetEvaluator, por faccao. Um NearestAttackableTargetGoal ao lado
        // seria uma segunda autoridade sobre a mesma decisao, e as duas
        // discordariam em silencio.
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;
        EnemyRuntime runtime = runtimeExigido();
        PerceptionSnapshot snapshot = runtime.tick(tickCount, this::varrerCandidatos,
                getHealth() <= getMaxHealth() * SpiderWebberTuning.FRACAO_DE_VIDA_CRITICA, false);
        aplicarAlvo(snapshot);
        tickDoLancamento(runtime);
        tickDaPrisao();
        publicarEstado(runtime);
    }

    /**
     * Traduz o alvo LEMBRADO em {@code setTarget}.
     *
     * <p>O uuid e resolvido para entidade a cada uso, nunca guardado: um campo com
     * a entidade seria uma referencia viva presa num mob, e isso nao da erro -- so
     * impede o objeto de morrer.</p>
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

    /**
     * Varredura de mundo -- a UNICA, e so quando o orcamento autoriza.
     *
     * <p>Ela entrega candidatos MEDIDOS e nao filtra por cone: quem aplica o cone e
     * o {@link PerceptionController}, depois da avaliacao por faccao.</p>
     */
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

    /** 1 quando o alvo esta bem na frente, -1 quando esta atras. */
    private double cossenoDoOlharAte(Entity alvo) {
        Vec3 olhar = getLookAngle();
        Vec3 horizontal = new Vec3(olhar.x, 0.0D, olhar.z);
        Vec3 ateOAlvo = new Vec3(alvo.getX() - getX(), 0.0D, alvo.getZ() - getZ());
        if (horizontal.lengthSqr() < 1.0E-6D || ateOAlvo.lengthSqr() < 1.0E-6D) return 0.0D;
        return Mth.clamp(horizontal.normalize().dot(ateOAlvo.normalize()), -1.0D, 1.0D);
    }

    // ------------------------------------------------------------------ teia

    /**
     * A janela ACTIVE resolve a AREA da teia; fora dela a teia nao existe.
     *
     * <p>A area e conferida contra a caixa que o SERVIDOR tem do alvo. Nenhum
     * pacote de cliente diz "fui pego" nem "escapei": se dissesse, escapar seria
     * de graca para quem quisesse, e nada no log distinguiria os dois casos.</p>
     *
     * <p>Uma teia por instancia de ataque, e uma presa por fiandeira. Lancar sobre
     * quem ja esta preso nao daria erro -- reiniciaria o relogio da vitima a cada
     * lancamento, e a saida por tempo, que existe justamente para quem nao
     * consegue reagir, nunca chegaria.</p>
     */
    private void tickDoLancamento(EnemyRuntime runtime) {
        if (runtime.ataques().phase() != AttackPhase.ACTIVE) return;
        if (teia.prendendo()) return;
        LivingEntity alvo = getTarget();
        if (alvo == null || !alvo.isAlive()) return;

        Optional<AttackHit> acerto = runtime.ataques().tryHit(alvo.getId(), alvo.getBoundingBox(),
                AREA_DA_TEIA, position(), getYRot(), REGIAO_COMUM);
        if (acerto.isEmpty()) return;

        alvo.hurt(damageSources().mobAttack(this), acerto.get().damage());
        // Prender SO depois de conferir que o alvo sobreviveu ao proprio dano da
        // teia. Prender um cadaver nao da erro: gasta o lancamento e ocupa a
        // fiandeira ate o tick seguinte perceber que nao ha ninguem la.
        if (!alvo.isAlive()) return;
        teia.prender(alvo.getUUID());
        avisar(alvo, MENSAGEM_PRESO);
    }

    /**
     * Um tick da prisao: segurar quem esta preso, ou anunciar a soltura.
     *
     * <p>O uuid e lido ANTES de avancar o relogio, porque quem sai da teia some do
     * estado no mesmo tick -- sem isso, a soltura aconteceria e nao haveria mais a
     * quem avisar. O aviso e a outra metade da saida: uma teia que solta em
     * silencio ensina que ela dura um tempo aleatorio.</p>
     */
    private void tickDaPrisao() {
        Optional<UUID> presoAntes = teia.presa();
        if (presoAntes.isEmpty()) return;
        LivingEntity presa = presaViva(presoAntes.get());

        SolturaDaTeia saida = teia.tick(presa != null, isAlive());
        if (saida == SolturaDaTeia.NENHUMA) {
            segurar(presa);
            return;
        }
        // PRESA_SUMIU e PREDADOR_CAIU nao tem a quem avisar: no primeiro caso nao
        // ha mais vitima; no segundo a aranha esta caindo, e a mensagem chegaria
        // junto com o loot. As duas saidas que o jogador VIVEU sao avisadas.
        if (presa != null && (saida == SolturaDaTeia.TEMPO_ESGOTADO
                || saida == SolturaDaTeia.FIO_ROMPIDO)) {
            avisar(presa, MENSAGEM_LIVRE);
        }
    }

    /**
     * Segura a presa por UM tick.
     *
     * <p><b>Duas metades, e as duas sao necessarias.</b> O servidor zera a
     * velocidade horizontal e marca {@code hurtMarked}, que e o que faz a correcao
     * CHEGAR ao cliente -- sem isso o servidor moveria e o cliente devolveria a
     * posicao antiga no tick seguinte. E a lentidao vanilla faz o CLIENTE parar
     * sozinho: sem ela os dois lados brigariam a cada tick e o preso apareceria
     * tremendo no lugar, que o jogador le como lag e nao como teia.</p>
     *
     * <p>A queda continua: so o plano horizontal e zerado. Congelar o eixo Y
     * deixaria quem foi pego no ar flutuando -- e flutuar nao e prender, e uma
     * plataforma de graca.</p>
     *
     * <p>O efeito e um PULSO curto reaplicado, e nao um efeito longo removido na
     * soltura. Remover apagaria tambem a lentidao que a vitima ja tinha por outro
     * motivo, o que nao da erro: da um jogador curado de uma pocao pela aranha.
     * Um efeito PROPRIO, com icone e nome, seria melhor que emprestar o vanilla --
     * ele nao cabe aqui porque registrar um {@code MobEffect} mexe em
     * {@code enemy/registry}, que nao pertence a esta frente.</p>
     */
    private void segurar(LivingEntity presa) {
        if (presa == null) return;
        presa.setDeltaMovement(0.0D, Math.min(0.0D, presa.getDeltaMovement().y), 0.0D);
        presa.hurtMarked = true;
        MobEffectInstance lentidao = presa.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        // Reaplicar so quando o pulso esta acabando: um addEffect por tick manda
        // um pacote por tick para cada preso, e isso nao da erro -- da uma conta
        // de rede que cresce com o tamanho do esquadrao.
        if (lentidao == null || lentidao.getAmplifier() < SpiderWebberTuning.FORCA_DA_LENTIDAO
                || lentidao.getDuration() * 2 <= SpiderWebberTuning.TICKS_DO_PULSO_DE_LENTIDAO) {
            presa.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    SpiderWebberTuning.TICKS_DO_PULSO_DE_LENTIDAO,
                    SpiderWebberTuning.FORCA_DA_LENTIDAO, false, true, true), this);
        }
    }

    /** A presa, se ela ainda existe e esta viva. Nulo e resposta legitima. */
    private LivingEntity presaViva(UUID id) {
        Entity encontrada = ((ServerLevel) level()).getEntity(id);
        return encontrada instanceof LivingEntity viva && viva.isAlive() ? viva : null;
    }

    /** Aviso na barra de acao. Desfecho, como recusa, sempre tem mensagem traduzida. */
    private void avisar(LivingEntity quem, String chave) {
        if (quem instanceof Player jogador) {
            jogador.displayClientMessage(Component.translatable(chave), true);
        }
    }

    /**
     * UNICO lugar que alimenta o stagger E o rompimento do fio.
     *
     * <p>O mesmo golpe conta uma vez para cada coisa, e as duas contagens saem
     * daqui. Espalhar isso por dois handlers e o bug silencioso que este projeto ja
     * sabe que comete: o numero final fica plausivel demais para alguem notar sem
     * medir.</p>
     *
     * <p><b>O valor contado e o dano DECLARADO, antes da armadura dela.</b> E a
     * mesma regua com que {@code SpiderWebberTuning.DANO_QUE_LIBERTA} foi escrito,
     * e as duas precisam ser a mesma: contar o dano depois dos 4 de armadura faria
     * o limiar de oito exigir quase o dobro de golpes na pratica, e a saida ativa
     * ficaria bem mais cara do que o numero ao lado dela diz -- sem erro nenhum, e
     * com o jogador concluindo que bater nao funciona.</p>
     *
     * <p><b>So dano com ATACANTE rasga o fio.</b> Queimadura, queda e {@code /kill}
     * nao puxam fio nenhum -- senao bastaria a aranha pegar fogo para soltar a
     * presa de graca, e o jogador aprenderia a incendiar o chao em vez de acertar a
     * fiandeira. Pelo mesmo motivo eles tambem nao cambaleiam: fogo viraria uma
     * interrupcao permanente.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean levou = super.hurt(source, amount);
        if (!levou || level().isClientSide) return levou;
        if (!(source.getEntity() instanceof LivingEntity) || !Float.isFinite(amount)
                || amount <= 0.0F) {
            return true;
        }
        teia.registrarDanoNaFiandeira(amount);

        EnemyRuntime runtime = runtimeExigido();
        StaggerResult resultado = runtime.sofrerStagger(runtime.ataques().attackInstanceId(),
                amount, SpiderWebberTuning.RECARGA_APOS_INTERRUPCAO);
        if (resultado == StaggerResult.DISPAROU) {
            getNavigation().stop();
            this.entityData.set(CAMBALEANDO, true);
            this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        }
        return true;
    }

    // ------------------------------------------------------------- publicacao

    /** Publica ao cliente SO o que ele precisa para escolher o clipe. */
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
        if (runtime.consciencia() == EnemyAwarenessState.FLEE) return EnemyCombatState.RETREAT;
        switch (runtime.ataques().phase()) {
            case WINDUP: return EnemyCombatState.WINDUP;
            case ACTIVE: return EnemyCombatState.ACTIVE;
            case RECOVERY: return EnemyCombatState.RECOVERY;
            default: break;
        }
        LivingEntity alvo = getTarget();
        if (alvo == null) return EnemyCombatState.IDLE;
        // RETREAT e DERIVADO da mesma regra que recusa o tiro, e nao de um campo
        // que a Goal escreveria. Um campo ficaria ligado no tick em que a Goal
        // parasse de rodar -- por stagger, por morte do alvo -- e a aranha
        // apareceria recuando parada.
        return REGRAS.espacamento(distanceTo(alvo)) == DecisaoDeEspacamento.RECUAR
                ? EnemyCombatState.RETREAT : EnemyCombatState.AGGRO;
    }

    // ------------------------------------------------------------ ciclo de vida

    /**
     * Morrer SOLTA a presa e publica o repouso.
     *
     * <p>As duas coisas pelo mesmo motivo: depois de morta o passo de IA nao roda
     * mais. O campo sincronizado ficaria onde estava -- um cadaver com o abdome
     * erguido -- e, muito pior, a presa ficaria imobilizada ate o restart, porque
     * ninguem mais avancaria o relogio dela.</p>
     */
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) soltarTudo();
        super.die(source);
    }

    /** Remocao -- unload, dimensao, comando -- passa pelo MESMO ponto. */
    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide) soltarTudo();
        super.remove(reason);
    }

    private void soltarTudo() {
        teia.limpar();
        this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        this.entityData.set(CAMBALEANDO, false);
    }

    // ------------------------------------------------------------------ goals

    /**
     * Arma e lanca a teia; quem mede a janela e o {@link AttackController}.
     *
     * <p><b>Ela RECUSA, e cada recusa tem nome.</b> {@link RegrasDeTeia#podeLancar}
     * devolve um motivo em vez de um booleano, e o motivo que importa em tela e
     * {@link RecusaDaTeia#PERTO_DEMAIS}: quem encosta na aranha VE o lancamento
     * parar e o recuo comecar, e e essa a prova de que a zona morta existe.</p>
     *
     * <p>{@code static} de proposito: todo estado que ela le mora na ENTIDADE. Um
     * campo aqui seria compartilhado por todas as aranhas do mundo, e o sintoma
     * seria uma delas mirando o alvo de outra.</p>
     */
    private static final class TeiaGoal extends Goal {
        private final SpiderWebberEntity aranha;

        private TeiaGoal(SpiderWebberEntity aranha) {
            this.aranha = aranha;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (aranha.level().isClientSide) return false;
            EnemyRuntime runtime = aranha.runtimeExigido();
            if (runtime.stagger().cambaleando()) return false;
            if (runtime.consciencia() == EnemyAwarenessState.FLEE) return false;
            LivingEntity alvo = aranha.getTarget();
            if (alvo == null || !alvo.isAlive()) return false;
            return REGRAS.podeLancar(aranha.distanceTo(alvo), aranha.hasLineOfSight(alvo),
                    aranha.teia.prendendo(), runtime.ataques().canStart()) == RecusaDaTeia.NENHUMA;
        }

        @Override public boolean canContinueToUse() {
            EnemyRuntime runtime = aranha.runtimeExigido();
            // A CONTINUACAO NAO RECONSULTA A DISTANCIA, de proposito. Depois que o
            // abdome subiu, o lancamento acontece: cancelar no meio porque o alvo se
            // aproximou faria a aranha desarmar em silencio um ataque que o jogador
            // ja viu comecar, e o telegrafo passaria a mentir. Quem entra na zona
            // morta durante o aviso ganha a teia ERRANDO -- a area so comeca a 3.2
            // blocos --, e nao a teia desaparecendo.
            return runtime.ataques().phase() != AttackPhase.IDLE
                    && runtime.ataques().phase() != AttackPhase.COMPLETE
                    && !runtime.stagger().cambaleando();
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() {
            aranha.getNavigation().stop();
            aranha.runtimeExigido().ataques().start(SpiderWebberTuning.teia());
        }

        @Override public void tick() {
            EnemyRuntime runtime = aranha.runtimeExigido();
            LivingEntity alvo = aranha.getTarget();
            if (alvo != null && runtime.ataques().phase() == AttackPhase.WINDUP
                    && ticksDeAvisoJaGastos(runtime) < SpiderWebberTuning.TICKS_DE_MIRA_NO_WINDUP) {
                aranha.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            }
            // Uma atiradora nao desliza enquanto mira. Sem isto ela chegaria ao fim
            // do aviso ainda carregando a inercia da aproximacao, e a teia sairia de
            // um lugar diferente daquele onde foi armada -- sem erro nenhum, e com o
            // telegrafo apontando para o lugar errado.
            aranha.setDeltaMovement(aranha.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
        }

        /** Quantos ticks do aviso ja passaram; o relogio e o do servidor, nunca o do clipe. */
        private static int ticksDeAvisoJaGastos(EnemyRuntime runtime) {
            return SpiderWebberTuning.WINDUP_DA_TEIA - runtime.ataques().remainingTicks();
        }

        @Override public void stop() { aranha.getNavigation().stop(); }
    }

    /**
     * Os pes do oficial de alcance: aproxima, para, ou RECUA.
     *
     * <p><b>Ela tem prioridade MENOR que a teia, e a ordem e a decisao.</b> As duas
     * disputam {@code MOVE} e {@code LOOK}; enquanto o lancamento estiver em curso,
     * esta goal nao roda. E isso que faz a recuperacao de vinte ticks ser uma
     * janela de verdade: a aranha nao recua no meio do proprio golpe, e quem fechou
     * a distancia tem esse tempo para cobrar.</p>
     *
     * <p>O destino do recuo e recalculado so quando a navegacao termina. Recalcular
     * todo tick nao da erro: da uma aranha que troca de ideia vinte vezes por
     * segundo e fica vibrando no lugar, o que na tela le como mob quebrado.</p>
     */
    private static final class EspacamentoGoal extends Goal {
        private final SpiderWebberEntity aranha;

        private EspacamentoGoal(SpiderWebberEntity aranha) {
            this.aranha = aranha;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (aranha.level().isClientSide) return false;
            if (aranha.runtimeExigido().stagger().cambaleando()) return false;
            LivingEntity alvo = aranha.getTarget();
            if (alvo == null || !alvo.isAlive()) return false;
            return REGRAS.espacamento(aranha.distanceTo(alvo)) != DecisaoDeEspacamento.MANTER;
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void tick() {
            LivingEntity alvo = aranha.getTarget();
            if (alvo == null) return;
            aranha.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            if (REGRAS.espacamento(aranha.distanceTo(alvo)) == DecisaoDeEspacamento.APROXIMAR) {
                aranha.getNavigation().moveTo(alvo, SpiderWebberTuning.VELOCIDADE_DE_APROXIMACAO);
                return;
            }
            recuar(alvo);
        }

        private void recuar(LivingEntity alvo) {
            if (!aranha.getNavigation().isDone()) return;
            Vec3 destino = DefaultRandomPos.getPosAway(aranha, SpiderWebberTuning.RAIO_DO_RECUO,
                    SpiderWebberTuning.ALTURA_DO_RECUO, alvo.position());
            if (destino == null) {
                // Encurralada: sem caminho para tras ela PARA de tentar, em vez de
                // empurrar a parede. Continuar chamando a navegacao daria um bicho
                // vibrando contra a pedra, e o jogador leria isso como bug em vez de
                // como a vitoria que e.
                aranha.getNavigation().stop();
                return;
            }
            aranha.getNavigation().moveTo(destino.x, destino.y, destino.z,
                    SpiderWebberTuning.VELOCIDADE_DE_RECUO);
        }

        @Override public void stop() { aranha.getNavigation().stop(); }
    }

    // ------------------------------------------------------------- animacao

    /**
     * O CLIENTE NAO DECIDE NADA: ele le a fase e o cambaleio que o servidor publica.
     *
     * <p>Ordem de precedencia, da mais especifica para a menos: morte, stagger,
     * fases do lancamento, locomocao, ocio. Morte vem primeiro porque uma aranha
     * que morre no meio do aviso nao pode terminar o clipe -- e stagger vem antes
     * do lancamento porque interromper e, por definicao, o que ganha do golpe.</p>
     */
    @Override
    public void registerControllers(
            software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<SpiderWebberEntity>(
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

    /**
     * Delega a repeticao ao .animation.json.
     *
     * <p>Declarada ANTES dos clipes porque um campo estatico usado acima da
     * propria declaracao e referencia adiantada ilegal -- aqui o compilador
     * reprova, que e o unico lugar deste arquivo onde a ordem de declaracao
     * importa.</p>
     */
    private static final software.bernie.geckolib.animation.Animation.LoopType LOOP_DO_ARQUIVO =
            software.bernie.geckolib.animation.Animation.LoopType.DEFAULT;

    // Os nomes sao um CONTRATO com spider_webber.animation.json. Errar um deles nao
    // da erro: o GeckoLib nao acha o clipe e deixa o osso parado.
    //
    // ELES SAO LITERAIS INTEIROS, e nao "animation.spider_webber." + nome, e a
    // diferenca nao e estilo. O portao clipeCitadoNoCodigoExiste varre a fonte
    // procurando o padrao animation.<mob>.<clipe>; montado por concatenacao, o
    // nome nunca aparece inteiro no arquivo, a varredura nao casa, e o mob sai da
    // cobertura sem que nada reprove -- o portao continua verde varrendo menos, que
    // e o falso verde mais barato que existe.
    //
    // LoopType.DEFAULT em todos: o tipo de repeticao mora no arquivo, e cravar em
    // Java o faria vencer o JSON, transformando o dicionario LOOPS do gerador em
    // documentacao que discorda do comportamento.
    private static final software.bernie.geckolib.animation.RawAnimation IDLE =
            software.bernie.geckolib.animation.RawAnimation.begin().then(
                    "animation.spider_webber.idle", LOOP_DO_ARQUIVO);
    private static final software.bernie.geckolib.animation.RawAnimation WALK =
            software.bernie.geckolib.animation.RawAnimation.begin().then(
                    "animation.spider_webber.walk", LOOP_DO_ARQUIVO);
    private static final software.bernie.geckolib.animation.RawAnimation WINDUP =
            software.bernie.geckolib.animation.RawAnimation.begin().then(
                    "animation.spider_webber.windup", LOOP_DO_ARQUIVO);
    private static final software.bernie.geckolib.animation.RawAnimation STRIKE =
            software.bernie.geckolib.animation.RawAnimation.begin().then(
                    "animation.spider_webber.strike", LOOP_DO_ARQUIVO);
    private static final software.bernie.geckolib.animation.RawAnimation RECOVERY =
            software.bernie.geckolib.animation.RawAnimation.begin().then(
                    "animation.spider_webber.recovery", LOOP_DO_ARQUIVO);
    private static final software.bernie.geckolib.animation.RawAnimation STAGGER =
            software.bernie.geckolib.animation.RawAnimation.begin().then(
                    "animation.spider_webber.stagger", LOOP_DO_ARQUIVO);
    private static final software.bernie.geckolib.animation.RawAnimation DEATH =
            software.bernie.geckolib.animation.RawAnimation.begin().then(
                    "animation.spider_webber.death", LOOP_DO_ARQUIVO);

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
