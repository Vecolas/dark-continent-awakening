package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.base.BaseChimeraAnt;
import com.darkcontinent.nenfoundation.enemy.base.EnemyRuntime;
import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenIntent;
import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenSituation;
import com.darkcontinent.nenfoundation.enemy.combat.AttackController;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHit;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.DecisaoDaPicada;
import com.darkcontinent.nenfoundation.enemy.combat.DecisaoDeDreno;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDaPicada;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeDreno;
import com.darkcontinent.nenfoundation.enemy.combat.ResultadoDeDreno;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import com.darkcontinent.nenfoundation.enemy.content.ChimeraProfiles;
import com.darkcontinent.nenfoundation.enemy.content.MosquitoOfficerTuning;
import com.darkcontinent.nenfoundation.enemy.faction.FactionRelations;
import com.darkcontinent.nenfoundation.enemy.perception.HearingEvent;
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
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Mosquito Officer -- formiga quimera, rank OFFICER. A que DRENA.
 *
 * <p><b>O que ela faz, numa frase:</b> ela fica mais forte com o que tira, e por
 * isso o corpo dela e o mais fraco da familia -- um oficial que ja nasce forte
 * nao precisa drenar, e a mecanica inteira vira decoracao.</p>
 *
 * <p><b>O dreno tem TETO, e o teto e a mecanica.</b> Cada picada que ENTRA na
 * vitima devolve metade do dano aplicado como vida. "Que entra" nao e detalhe de
 * redacao: o valor e medido pela vida da vitima ANTES e DEPOIS do {@code hurt},
 * entao escudo, absorcao, invulnerabilidade e modo criativo comem o dreno junto
 * com o dano -- o acerto que nao machucou tambem nao cura. E o total de um
 * encontro para em {@code TETO_DE_DRENO_POR_ENCONTRO}: sem esse limite, um
 * combate longo a leva a vida praticamente infinita e o encontro deixa de ter
 * fim, sem uma linha de log. A regra inteira mora em {@link RegrasDeDreno}, que e
 * pura; esta classe apenas mede, aplica e guarda o acumulado.</p>
 *
 * <p><b>Ela VOA e chega PELO LADO.</b> O corpo usa {@code FlyingMoveControl} e
 * {@code FlyingPathNavigation} e nunca pousa por vontade propria. E ela RECUSA a
 * picada contra quem esta encarando: {@link RegrasDaPicada} mede o cosseno do
 * olhar DA VITIMA -- e nao o dela -- e devolve {@code ENCARADA} enquanto o alvo a
 * tiver no campo de visao. Quem mantem o zumbido na tela ve o oficial circular
 * sem atacar, e essa e a prova em tela de que a regra existe. Medir o cosseno do
 * lado errado nao levantaria erro: a conta ficaria plausivel, o valor ficaria
 * sempre proximo de 1, e a regra aprovaria todo ataque.</p>
 *
 * <p><b>A agulha e curta e a caixa de dano e estreita.</b> Sessenta centimetros
 * de alcance e setenta de arco, contra os tres blocos e os quatro de arco do
 * porrete do Cyclops: ela precisa ENCOSTAR. E esse preco que torna o dreno
 * generoso aceitavel. A picada tambem nao empurra -- empurrar a vitima a tiraria
 * do proprio alcance e quebraria a cadeia picar-curar-picar que e a ficha
 * dela.</p>
 *
 * <p><b>Nen: ela DECIDE, e nao ativa.</b> O controlador tatico devolve uma
 * INTENCAO; esta classe nao calcula aura, nao aplica custo e nao liga tecnica
 * nenhuma -- o Nen Foundation e a unica autoridade sobre Nen. O unico efeito
 * ligado a intencao e de POSTURA do inimigo: em {@code ENTRAR_EM_ZETSU} ela para
 * de picar. Ver {@link #FRACAO_DE_AURA_NAO_MEDIDA} para o que isso vale hoje, e
 * por que.</p>
 *
 * <p>Ela usa {@link EnemyRuntime} por inteiro -- percepcao com orcamento,
 * cerebro, ataque e stagger -- e publica ao cliente so a fase e o cambaleio, que
 * e do que o controlador de animacao precisa para escolher o clipe.</p>
 */
public final class MosquitoOfficerEntity extends BaseChimeraAnt
        implements software.bernie.geckolib.animatable.GeoEntity {

    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(MosquitoOfficerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CAMBALEANDO =
            SynchedEntityData.defineId(MosquitoOfficerEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * Fracao de aura com que a situacao tatica e montada hoje: ZERO, e o zero e
     * declarado em vez de escondido.
     *
     * <p><b>Por que zero.</b> O nucleo de Nen nao publica a aura de uma formiga --
     * a camada de percepcao (#126) ainda esta aberta -- e inventar aqui uma
     * fracao seria a segunda autoridade sobre Nen que o CLAUDE.md proibe. Zero
     * significa exatamente "nenhuma aura utilizavel medida", e o controlador
     * tatico responde a isso com {@code NENHUMA} para qualquer estagio. A
     * consequencia esta escrita para ninguem se enganar: HOJE a intencao desta
     * formiga e sempre {@code NENHUMA}, e o ramo {@code ESCONDIDA} do
     * {@link RegrasDaPicada} nunca dispara em jogo.</p>
     *
     * <p><b>Por que a chamada existe assim mesmo.</b> Porque o caminho ser real e
     * o que impede a alternativa pior: um numero plausivel chutado aqui faria a
     * formiga "usar Nen" de mentira, e desbalanceamento vindo de aura inventada e
     * exatamente o defeito que nunca aparece como erro. Quando o nucleo publicar
     * a leitura, muda esta constante e mais nada.</p>
     */
    public static final double FRACAO_DE_AURA_NAO_MEDIDA = 0.0D;

    /**
     * As pecas imutaveis da ficha, resolvidas uma vez.
     *
     * <p>Estaticas porque NENHUMA delas guarda estado de jogador -- sao records
     * imutaveis. Um campo estatico com estado seria o erro classico deste
     * projeto: dois oficiais decidindo com a mesma memoria. O acumulado do dreno,
     * que E estado, e campo de INSTANCIA logo abaixo.</p>
     */
    private static final RegrasDeDreno DRENO = MosquitoOfficerTuning.regrasDeDreno();
    private static final RegrasDaPicada FLANCO = MosquitoOfficerTuning.regrasDaPicada();
    private static final AttackHitbox CAIXA_DA_PICADA = MosquitoOfficerTuning.caixaDaPicada();

    /**
     * Regiao passada ao {@code AttackHit} deste golpe.
     *
     * <p>Ela e a COMUM e nao ha outra: este oficial nao tem ponto fraco, e nao
     * ter e a ficha dele -- a defesa aqui nao e uma regiao dura, e nao ser
     * alcancado. Inventar uma regiao com multiplicador para o golpe que ele DA
     * poria o multiplicador no lado errado do combate, e o dano final ficaria
     * plausivel demais para alguem notar sem medir.</p>
     */
    private static final String REGIAO_COMUM = "corpo";

    /** Cache por INSTANCIA; um cache estatico faria todos os oficiais dividirem um clipe. */
    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache cacheDeAnimacao =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    /**
     * Quanto este ENCONTRO ja devolveu de vida.
     *
     * <p>Campo de instancia e nao de classe: um acumulador estatico faria a
     * colonia inteira dividir o mesmo teto, e o sintoma seria um oficial que
     * chega a briga ja sem poder drenar porque outro drenou do outro lado do
     * mapa. Ele e zerado no MESMO ponto em que o encontro acaba -- perder o alvo,
     * trocar de alvo, morrer, ser removido -- e nao em quatro lugares diferentes;
     * ver {@link #encerrarEncontro()}.</p>
     *
     * <p>Nao vai para o NBT de proposito: o teto limita uma LUTA, e uma luta nao
     * sobrevive ao logout. Persistir isso faria um oficial voltar do save com o
     * teto gasto por uma briga que o jogador nao lembra de ter comecado.</p>
     */
    private float drenadoNesteEncontro;

    /** A intencao de Nen deste tick. Ver {@link #FRACAO_DE_AURA_NAO_MEDIDA}. */
    private TacticalNenIntent intencaoDeNen = TacticalNenIntent.NENHUMA;

    public MosquitoOfficerEntity(EntityType<? extends MosquitoOfficerEntity> type, Level level) {
        super(type, level, ChimeraProfiles.mosquitoOfficer().metadata(),
                new AwarenessTuning(MosquitoOfficerTuning.TICKS_DE_AVISO,
                        MosquitoOfficerTuning.MEMORIA_DE_ALVO_TICKS),
                ChimeraProfiles.mosquitoOfficerMolde());
        // O molde garante WINGS, e as asas tem de valer no CORPO e nao so na
        // ficha. O par move control + navegacao de voo e o mesmo da Bee vanilla.
        // Sem ele o trait existiria no genoma, apareceria no debug e a formiga
        // andaria pelo chao -- um dado verdadeiro e um bicho que o desmente, sem
        // uma linha de erro em lugar nenhum.
        this.moveControl = new FlyingMoveControl(this, 20, false);
        setPathfindingMalus(PathType.WATER, -1.0F);
        setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        instalarRuntime(montarRuntime());
    }

    private EnemyRuntime montarRuntime() {
        var perfil = ChimeraProfiles.mosquitoOfficer();
        PerceptionController percepcao = new PerceptionController(
                PerceptionBudget.padrao(),
                VisionCone.deGraus(perfil.attributes().followRange(),
                        MosquitoOfficerTuning.ABERTURA_DA_VISAO),
                new ThreatMemory(MosquitoOfficerTuning.MEMORIA_DE_ALVO_TICKS),
                new TargetEvaluator(FactionRelations.padrao(), perfil.metadata().faction(),
                        perfil.attributes().followRange(), perfil.metadata().territorial()),
                // A porta de aura da formiga continua INERTE enquanto a #126 (Gyo)
                // estiver aberta no nucleo: inventar aqui a regra de quem-ve-o-que
                // seria a segunda autoridade sobre Nen que o CLAUDE.md proibe.
                PercepcaoDeAura.NENHUMA, MosquitoOfficerTuning.ALCANCE_DE_AUDICAO,
                getUUID().hashCode());
        return new EnemyRuntime(percepcao,
                new EnemyBrain(new AwarenessTuning(MosquitoOfficerTuning.TICKS_DE_AVISO,
                        MosquitoOfficerTuning.MEMORIA_DE_ALVO_TICKS)),
                new AttackController(ChimeraProfiles.mosquitoOfficerRecarga()),
                new StaggerState(ChimeraProfiles.mosquitoOfficerStagger()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = ChimeraProfiles.mosquitoOfficer().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                // FLYING_SPEED nao vem de createMobAttributes, e e ela que a
                // FlyingMoveControl le fora do chao. Sem esta linha o atributo nao
                // existe e a leitura estoura no primeiro voo. O valor e o MESMO do
                // perfil de proposito: o oficial nao tem duas velocidades para girar.
                .add(Attributes.FLYING_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<MosquitoOfficerEntity> registeredType() { return EnemyEntityTypes.MOSQUITO_OFFICER.get(); }

    @Override
    protected PathNavigation createNavigation(Level nivel) {
        FlyingPathNavigation navegacao = new FlyingPathNavigation(this, nivel);
        navegacao.setCanOpenDoors(false);
        navegacao.setCanFloat(false);
        navegacao.setCanPassDoors(true);
        return navegacao;
    }

    /**
     * Ela nao leva dano de queda -- ela nao cai, ela desce.
     *
     * <p>{@code checkFallDamage} vazio e o que a Bee faz, e e ele que impede a
     * distancia acumulada num mergulho de virar dano no pouso. O
     * {@code causeFallDamage} esta aqui pela mesma decisao, para quem chamar o
     * dano de queda por fora receber a mesma resposta. Sem os dois, um oficial de
     * 55 de vida e armadura 2 morreria sozinho descendo do anel de espera, e o
     * relato seria "ele as vezes morre do nada".</p>
     */
    @Override
    protected void checkFallDamage(double y, boolean noChao, BlockState estado, BlockPos pos) { }

    @Override
    public boolean causeFallDamage(float distanciaDeQueda, float multiplicador, DamageSource fonte) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        builder.define(CAMBALEANDO, false);
    }

    /** Fase corrente da picada; vale nos dois lados, porque vem do SynchedEntityData. */
    public AttackPhase faseDeAtaque() {
        int ordinal = this.entityData.get(FASE_DE_ATAQUE);
        AttackPhase[] fases = AttackPhase.values();
        return ordinal >= 0 && ordinal < fases.length ? fases[ordinal] : AttackPhase.IDLE;
    }

    public boolean cambaleando() { return this.entityData.get(CAMBALEANDO); }

    /** Quanto este encontro ja devolveu de vida; leitura, para teste e para debug. */
    public float drenadoNesteEncontro() { return drenadoNesteEncontro; }

    /** A intencao de Nen do tick corrente. Intencao, nunca acao. */
    public TacticalNenIntent intencaoDeNen() { return intencaoDeNen; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PicadaGoal(this));
        goalSelector.addGoal(2, new FlancoGoal(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // NENHUM WaterAvoidingRandomStrollGoal, e a ausencia e deliberada. Ele
        // escolhe destinos NO CHAO, e uma navegacao de voo obedece: o oficial
        // pousaria para vagar, e o unico sinal que a silhueta dele da -- "isto
        // voa" -- passaria a ser desmentido pela propria IA em repouso. Nada
        // disso levanta erro: a rota e valida e o pathfinding funciona.
        //
        // NENHUM target goal vanilla, pelo motivo de sempre: quem escolhe alvo
        // aqui e o TargetEvaluator, por faccao. Um NearestAttackableTargetGoal ao
        // lado seria uma segunda autoridade sobre a mesma decisao, e as duas
        // discordariam em silencio.
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;
        EnemyRuntime runtime = runtimeExigido();
        PerceptionSnapshot snapshot = runtime.tick(tickCount, this::varrerCandidatos,
                getHealth() <= getMaxHealth() * MosquitoOfficerTuning.FRACAO_DE_VIDA_CRITICA,
                false);
        aplicarAlvo(snapshot);
        decidirNen();
        tickDaPicada(runtime);
        publicarEstado(runtime);
    }

    /**
     * Traduz o alvo LEMBRADO em {@code setTarget} -- e e aqui que o encontro acaba.
     *
     * <p>O uuid e resolvido para entidade a cada uso, nunca guardado: um campo com
     * a entidade seria uma referencia viva presa num mob, e isso nao da erro --
     * so impede o objeto de morrer.</p>
     *
     * <p>Perder ou trocar de alvo zera o teto do dreno. Este e o unico lugar em
     * jogo onde "o encontro terminou" de fato acontece: se o teto so zerasse na
     * morte, um oficial que perseguisse tres jogadores seguidos chegaria ao
     * terceiro sem poder drenar nada, e o terceiro lutaria contra um bicho
     * diferente do que os outros dois enfrentaram -- sem nada acusando.</p>
     */
    private void aplicarAlvo(PerceptionSnapshot snapshot) {
        Optional<UUID> lembrado = snapshot.alvoOpcional();
        if (lembrado.isEmpty()) {
            if (getTarget() != null) {
                setTarget(null);
                encerrarEncontro();
            }
            return;
        }
        Entity encontrado = ((ServerLevel) level()).getEntity(lembrado.get());
        if (encontrado instanceof LivingEntity vivo && vivo.isAlive()) {
            if (getTarget() != vivo) {
                encerrarEncontro();
                setTarget(vivo);
            }
        } else if (getTarget() != null) {
            setTarget(null);
            encerrarEncontro();
        }
    }

    /** Zera o que e do ENCONTRO. Ponto unico, chamado de todos os fins. */
    private void encerrarEncontro() {
        drenadoNesteEncontro = 0.0F;
    }

    /**
     * Varredura de mundo -- a UNICA, e so quando o orcamento autoriza.
     *
     * <p>Ela entrega candidatos MEDIDOS e nao filtra por cone: quem aplica o cone
     * e o {@link PerceptionController}, depois da avaliacao por faccao.</p>
     */
    private List<TargetCandidate> varrerCandidatos() {
        double alcance = getAttributeValue(Attributes.FOLLOW_RANGE);
        AABB area = getBoundingBox().inflate(alcance);
        List<TargetCandidate> candidatos = new ArrayList<>();
        for (Player jogador : level().getEntitiesOfClass(Player.class, area,
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative())) {
            candidatos.add(new TargetCandidate(jogador.getUUID(),
                    EnemyFaction.HUNTER_ASSOCIATION,
                    distanceTo(jogador), cossenoDoOlharAte(jogador), hasLineOfSight(jogador),
                    true, true, jogador == getLastHurtByMob()));
        }
        return candidatos;
    }

    /** 1 quando o alvo esta bem na frente DELA, -1 quando esta atras dela. */
    private double cossenoDoOlharAte(Entity alvo) {
        return cosseno(getLookAngle(), alvo.getX() - getX(), alvo.getZ() - getZ());
    }

    /**
     * 1 quando ELA esta bem na frente do alvo, -1 quando esta pelas costas dele.
     *
     * <p>E este -- e nao {@link #cossenoDoOlharAte(Entity)} -- que a regra de
     * flanco consome. Passar o outro nao levantaria erro: o oficial sempre olha
     * para a vitima, entao o valor ficaria colado em 1, {@code ENCARADA} seria a
     * resposta eterna e o bicho circularia para sempre sem nunca atacar.</p>
     */
    private double cossenoDoOlharDoAlvo(LivingEntity alvo) {
        return cosseno(alvo.getLookAngle(), getX() - alvo.getX(), getZ() - alvo.getZ());
    }

    private static double cosseno(Vec3 olhar, double dx, double dz) {
        Vec3 horizontal = new Vec3(olhar.x, 0.0D, olhar.z);
        Vec3 ate = new Vec3(dx, 0.0D, dz);
        if (horizontal.lengthSqr() < 1.0E-6D || ate.lengthSqr() < 1.0E-6D) return 0.0D;
        return Mth.clamp(horizontal.normalize().dot(ate.normalize()), -1.0D, 1.0D);
    }

    /**
     * A janela ACTIVE consulta a caixa da picada; fora dela o golpe nao existe.
     *
     * <p><b>O DANO E MEDIDO, e nao suposto.</b> {@code hurt} devolve apenas SE o
     * dano entrou, e nao QUANTO: a vida antes e depois -- somando a absorcao, que
     * e consumida primeiro -- e a unica medida honesta do que a picada de fato
     * tirou. Usar {@code hit.damage()} no lugar seria mais curto e curaria a
     * formiga por um dano que um escudo comeu: o acerto que nao machucou passaria
     * a curar, com o log limpo e a barra dela subindo.</p>
     */
    private void tickDaPicada(EnemyRuntime runtime) {
        if (runtime.ataques().phase() != AttackPhase.ACTIVE) return;
        LivingEntity alvo = getTarget();
        if (alvo == null || !alvo.isAlive()) return;

        Optional<AttackHit> acerto = runtime.ataques().tryHit(alvo.getId(), alvo.getBoundingBox(),
                CAIXA_DA_PICADA, position(), getYRot(), REGIAO_COMUM);
        if (acerto.isEmpty()) return;

        float vidaAntes = alvo.getHealth() + alvo.getAbsorptionAmount();
        boolean entrou = alvo.hurt(damageSources().mobAttack(this), acerto.get().damage());
        float aplicado = entrou
                ? vidaAntes - (alvo.getHealth() + alvo.getAbsorptionAmount())
                : 0.0F;
        drenar(aplicado, podeSerDrenado(alvo));
    }

    /**
     * O que esta vitima tem para dar.
     *
     * <p>Outra formiga quimera NAO alimenta esta: um esquadrao encurralado se
     * curaria em circulo, e o encontro deixaria de ter fim. E ela mesma, nunca --
     * dano refletido a faria se curar com o proprio golpe, o que nao da erro e da
     * um oficial imortal diante de qualquer espinho.</p>
     */
    private boolean podeSerDrenado(LivingEntity alvo) {
        return alvo != this && !(alvo instanceof BaseChimeraAnt);
    }

    /**
     * O UNICO lugar que cura esta formiga, e o UNICO que mexe no teto do encontro.
     *
     * <p>Dois pontos de cura seriam a versao deste bicho do erro que este projeto
     * ja sabe que comete -- dano multiplicado em dois handlers. Aqui a conta
     * inteira e da {@link RegrasDeDreno}, que e pura e testada; esta classe mede,
     * aplica e soma no acumulado, nesta ordem. Somar antes de aplicar faria o
     * teto ser gasto por uma cura que nao aconteceu.</p>
     */
    private void drenar(float danoAplicado, boolean vitimaDrenavel) {
        ResultadoDeDreno resultado = DRENO.decidir(danoAplicado, true, vitimaDrenavel,
                getHealth(), getMaxHealth(), drenadoNesteEncontro);
        if (resultado.motivo() != DecisaoDeDreno.DRENA) return;
        heal(resultado.cura());
        drenadoNesteEncontro += resultado.cura();
    }

    /**
     * Decide a INTENCAO de Nen deste tick -- e nao ativa nada.
     *
     * <p>O controlador recebe fatos ja medidos e devolve uma intencao; quem
     * ativaria tecnica, gastaria aura ou aplicaria custo e o Nen Foundation, que e
     * a unica autoridade sobre Nen. Ver {@link #FRACAO_DE_AURA_NAO_MEDIDA} para o
     * que a fracao de aura vale hoje, e para a consequencia disso.</p>
     */
    private void decidirNen() {
        LivingEntity alvo = getTarget();
        boolean emCombate = alvo != null && alvo.isAlive();
        // alvoAoAlcance sem emCombate e recusado pelo proprio record: os dois
        // campos discordando fariam a decisao tomar o caminho de ataque contra um
        // alvo que o resto do sistema diz nao existir.
        boolean aoAlcance = emCombate
                && distanceTo(alvo) <= MosquitoOfficerTuning.ALCANCE_DA_PICADA;
        TacticalNenSituation situacao = new TacticalNenSituation(
                FRACAO_DE_AURA_NAO_MEDIDA,
                Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F),
                emCombate, aoAlcance, false,
                runtimeExigido().stagger().cambaleando(), true);
        intencaoDeNen = nenTatico().map(controlador -> controlador.decidir(situacao))
                .orElse(TacticalNenIntent.NENHUMA);
    }

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
        return switch (runtime.ataques().phase()) {
            case WINDUP -> EnemyCombatState.WINDUP;
            case ACTIVE -> EnemyCombatState.ACTIVE;
            case RECOVERY -> EnemyCombatState.RECOVERY;
            default -> getTarget() != null ? EnemyCombatState.AGGRO : EnemyCombatState.IDLE;
        };
    }

    /**
     * UNICO lugar que alimenta o stagger.
     *
     * <p>Ela nao tem ponto fraco, e a ausencia e a ficha: a defesa deste oficial
     * nao e uma regiao dura, e nao ser alcancado. Em compensacao o limiar de
     * stagger dela e o mais baixo da familia (26, contra 34 do spider webber), e
     * e por isso que interromper e a resposta que o encontro ensina.</p>
     *
     * <p>Dano sem atacante (fogo, queda, {@code /kill}) tambem acumula aqui, ao
     * contrario do que o Cyclops faz -- e a diferenca e deliberada: aquele mob
     * tem um multiplicador de regiao que exige angulo, e este nao tem regiao
     * nenhuma. Filtrar por atacante aqui criaria um caso especial sem regra.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || !Float.isFinite(amount) || amount <= 0.0F) {
            return super.hurt(source, amount);
        }
        boolean levou = super.hurt(source, amount);
        if (!levou) return false;

        EnemyRuntime runtime = runtimeExigido();
        StaggerResult resultado = runtime.sofrerStagger(runtime.ataques().attackInstanceId(),
                amount, MosquitoOfficerTuning.RECARGA_APOS_INTERRUPCAO);
        if (resultado == StaggerResult.DISPAROU) {
            getNavigation().stop();
            this.entityData.set(CAMBALEANDO, true);
            this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        }
        return true;
    }

    /** Som de mundo chega ao oficial como EVENTO -- e assim que a audicao existe. */
    public void ouvir(HearingEvent evento) {
        if (!level().isClientSide) runtimeExigido().percepcao().ouvir(evento);
    }

    // ------------------------------------------------------------ ciclo de vida

    /**
     * Morrer PUBLICA o repouso e encerra o encontro.
     *
     * <p>Depois de morta o passo de IA nao roda mais, e o campo sincronizado fica
     * onde estava: ela morreria com a pose da picada travada no cliente. Nao ha
     * erro nisso -- so um cadaver com a agulha cravada no ar.</p>
     */
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) publicarRepouso();
        super.die(source);
    }

    /** Remocao -- unload, dimensao, comando -- passa pelo MESMO ponto. */
    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide) publicarRepouso();
        super.remove(reason);
    }

    private void publicarRepouso() {
        this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        this.entityData.set(CAMBALEANDO, false);
        encerrarEncontro();
        intencaoDeNen = TacticalNenIntent.NENHUMA;
        // Quem liga, desliga: o controlador tatico guarda a intencao corrente e
        // ha quantos ticks ela dura, e esses dois campos sobreviveriam a um
        // unload sem que nada acusasse.
        nenTatico().ifPresent(controlador -> controlador.limpar());
    }

    // ------------------------------------------------------------------ goals

    /** A decisao de flanco deste instante, montada com o que o SERVIDOR mediu. */
    private DecisaoDaPicada decidirFlanco(LivingEntity alvo) {
        boolean vivo = alvo != null && alvo.isAlive();
        return FLANCO.decidir(vivo,
                intencaoDeNen == TacticalNenIntent.ENTRAR_EM_ZETSU,
                vivo ? distanceTo(alvo) : 0.0D,
                vivo ? cossenoDoOlharDoAlvo(alvo) : 0.0D,
                vivo && hasLineOfSight(alvo));
    }

    /**
     * Um ponto no anel de espera, ao LADO do alvo e na altura de leitura.
     *
     * <p>O lado escolhido e aquele para o qual ela ja esta virada -- o produto
     * vetorial entre o olhar do alvo e a direcao ate ela da o sinal. Escolher
     * sempre o mesmo lado nao daria erro: daria um oficial que cruza a FRENTE do
     * jogador para chegar a esquerda dele, entrando exatamente no arco que acabou
     * de recusar, e o encontro leria como um bicho indeciso.</p>
     */
    private Vec3 pontoNoAnelDeEspera(LivingEntity alvo) {
        Vec3 olhar = alvo.getLookAngle();
        Vec3 frente = new Vec3(olhar.x, 0.0D, olhar.z);
        // Alvo olhando reto para cima ou para baixo nao tem frente horizontal. Sem
        // este ramo a normalizacao devolveria NaN, o destino viraria NaN, e a
        // MoveControl simplesmente pararia de mover o bicho -- sem excecao nenhuma.
        if (frente.lengthSqr() < 1.0E-6D) frente = new Vec3(0.0D, 0.0D, 1.0D);
        frente = frente.normalize();
        Vec3 lado = new Vec3(-frente.z, 0.0D, frente.x);
        double sinal = lado.dot(new Vec3(getX() - alvo.getX(), 0.0D, getZ() - alvo.getZ())) >= 0.0D
                ? 1.0D : -1.0D;
        return alvo.position()
                .add(lado.scale(sinal * MosquitoOfficerTuning.RAIO_DO_CONTORNO))
                // Um passo PARA TRAS do alvo junto com o passo lateral: so o
                // lateral deixaria o anel tangente ao arco proibido, e ela ficaria
                // entrando e saindo dele a cada tick.
                .subtract(frente.scale(MosquitoOfficerTuning.RAIO_DO_CONTORNO * 0.35D))
                .add(0.0D, MosquitoOfficerTuning.ALTURA_DE_ESPERA, 0.0D);
    }

    /**
     * Circula pelo anel de espera enquanto o alvo a encara, e fecha quando ele nao.
     *
     * <p>{@code static} de proposito: todo estado que ela le mora na ENTIDADE. Um
     * campo aqui seria compartilhado por todos os oficiais do mundo, e o sintoma
     * seria um deles reposicionando contra o alvo de outro.</p>
     */
    private static final class FlancoGoal extends Goal {
        private final MosquitoOfficerEntity oficial;

        private FlancoGoal(MosquitoOfficerEntity oficial) {
            this.oficial = oficial;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            LivingEntity alvo = oficial.getTarget();
            if (alvo == null || !alvo.isAlive()) return false;
            if (oficial.runtimeExigido().stagger().cambaleando()) return false;
            if (oficial.runtimeExigido().ataques().phase() != AttackPhase.IDLE) return false;
            DecisaoDaPicada decisao = oficial.decidirFlanco(alvo);
            return decisao == DecisaoDaPicada.ENCARADA || decisao == DecisaoDaPicada.LONGE
                    || decisao == DecisaoDaPicada.SEM_LINHA_DE_VISAO;
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void tick() {
            LivingEntity alvo = oficial.getTarget();
            if (alvo == null) return;
            oficial.getLookControl().setLookAt(alvo, 30.0F, 30.0F);

            // ENCARADA manda para o ANEL, e nao para cima do alvo. Ir direto seria
            // o mesmo que nao ter regra: ela chegaria ao alcance pela frente, a
            // picada seria recusada la, e ela ficaria parada colada no jogador --
            // um bicho travado, sem erro nenhum.
            Vec3 destino = oficial.decidirFlanco(alvo) == DecisaoDaPicada.ENCARADA
                    ? oficial.pontoNoAnelDeEspera(alvo)
                    : alvo.position().add(0.0D,
                            MosquitoOfficerTuning.ALTURA_DE_ESPERA * 0.5D, 0.0D);
            // Reescrito a cada tick: a MoveControl consome um destino por tick.
            oficial.getMoveControl().setWantedPosition(destino.x, destino.y, destino.z, 1.0D);
        }

        @Override public void stop() { oficial.getNavigation().stop(); }
    }

    /**
     * Arma e crava a agulha; quem mede a janela e o {@link AttackController}.
     *
     * <p><b>Ela RECUSA a picada contra quem esta encarando, e a recusa e a ficha
     * do bicho.</b> A memoria de ameaca dura 140 ticks e sobrevive ao alvo virar
     * de frente -- sem esta condicao, o oficial mergulharia contra quem o esta
     * olhando, e a unica defesa que o encontro ensina deixaria de funcionar. Quem
     * mantem o zumbido na tela VE o bicho circular sem atacar, e essa e a prova em
     * tela de que a regra existe.</p>
     */
    private static final class PicadaGoal extends Goal {
        private final MosquitoOfficerEntity oficial;

        private PicadaGoal(MosquitoOfficerEntity oficial) {
            this.oficial = oficial;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (oficial.level().isClientSide) return false;
            EnemyRuntime runtime = oficial.runtimeExigido();
            if (runtime.stagger().cambaleando()) return false;
            if (runtime.consciencia() == EnemyAwarenessState.FLEE) return false;
            LivingEntity alvo = oficial.getTarget();
            if (alvo == null || !alvo.isAlive()) return false;
            return oficial.decidirFlanco(alvo) == DecisaoDaPicada.PICA
                    && runtime.ataques().canStart();
        }

        @Override public boolean canContinueToUse() {
            EnemyRuntime runtime = oficial.runtimeExigido();
            // A CONTINUACAO NAO RECONSULTA O FLANCO, de proposito. Depois que a
            // agulha avancou, o golpe acontece: cancelar no meio porque o jogador
            // virou de frente faria o oficial desarmar em silencio um ataque que
            // ja comecou, e o telegrafo -- que dura catorze ticks e e a unica
            // coisa que este bicho da de graca -- passaria a mentir. Quem vira a
            // tempo ganha o golpe ERRANDO, e nao o golpe desaparecendo.
            return runtime.ataques().phase() != AttackPhase.IDLE
                    && runtime.ataques().phase() != AttackPhase.COMPLETE
                    && !runtime.stagger().cambaleando();
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() {
            oficial.getNavigation().stop();
            oficial.runtimeExigido().ataques().start(MosquitoOfficerTuning.picada());
        }

        @Override public void tick() {
            EnemyRuntime runtime = oficial.runtimeExigido();
            LivingEntity alvo = oficial.getTarget();
            if (alvo != null && runtime.ataques().phase() == AttackPhase.WINDUP
                    && ticksDeAvisoJaGastos(runtime)
                            < MosquitoOfficerTuning.TICKS_DE_MIRA_NO_WINDUP) {
                oficial.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            }
            // Ela nao desliza enquanto crava. Sem isto ela chegaria ao fim do
            // aviso ainda carregando a inercia da aproximacao, e a picada sairia
            // de um lugar diferente daquele onde foi armada -- com uma caixa de
            // dano de setenta centimetros de arco, isso e a diferenca entre
            // acertar e errar, e nao ha erro nenhum para procurar.
            oficial.setDeltaMovement(oficial.getDeltaMovement().multiply(0.2D, 0.2D, 0.2D));
        }

        /** Quantos ticks do aviso ja passaram; o relogio e o do servidor, nunca o do clipe. */
        private static int ticksDeAvisoJaGastos(EnemyRuntime runtime) {
            return MosquitoOfficerTuning.WINDUP_DA_PICADA - runtime.ataques().remainingTicks();
        }

        @Override public void stop() { oficial.getNavigation().stop(); }
    }

    // ------------------------------------------------------------- animacao

    @Override
    public void registerControllers(
            software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<MosquitoOfficerEntity>(
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

    // Contrato com mosquito_officer.animation.json. LoopType.DEFAULT em todos: o tipo de
    // repeticao mora no arquivo, e cravar em Java o faria vencer o JSON.
    private static final software.bernie.geckolib.animation.RawAnimation IDLE = clipe("idle");
    private static final software.bernie.geckolib.animation.RawAnimation WALK = clipe("walk");
    private static final software.bernie.geckolib.animation.RawAnimation WINDUP = clipe("windup");
    private static final software.bernie.geckolib.animation.RawAnimation STRIKE = clipe("strike");
    private static final software.bernie.geckolib.animation.RawAnimation RECOVERY = clipe("recovery");
    private static final software.bernie.geckolib.animation.RawAnimation STAGGER = clipe("stagger");
    private static final software.bernie.geckolib.animation.RawAnimation DEATH = clipe("death");

    private static software.bernie.geckolib.animation.RawAnimation clipe(String nome) {
        return software.bernie.geckolib.animation.RawAnimation.begin()
                .then("animation.mosquito_officer." + nome,
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
