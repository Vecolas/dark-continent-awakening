package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.DecisaoDeFerrao;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.ai.RegistroDeMatilhas;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeFerrao;
import com.darkcontinent.nenfoundation.enemy.ai.squad.Squad;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRegistry;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRole;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.base.BaseChimeraAnt;
import com.darkcontinent.nenfoundation.enemy.base.EnemyRuntime;
import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraRank;
import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenController;
import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenIntent;
import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenSituation;
import com.darkcontinent.nenfoundation.enemy.combat.AttackController;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.DoseDeVeneno;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeVeneno;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import com.darkcontinent.nenfoundation.enemy.content.ChimeraProfiles;
import com.darkcontinent.nenfoundation.enemy.content.ScorpionLeaderTuning;
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
import java.util.Objects;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Scorpion Leader -- formiga quimera, rank SQUADRON_LEADER. A que ENVENENA.
 *
 * <p><b>O golpe nao e o preco.</b> HP 120, armadura 8 e dano 12 -- MENOS que o do
 * guepardo -- porque o que ela cobra de verdade chega depois: cada ferroada soma
 * duracao ao veneno que o alvo ja carrega, e o nivel do efeito sai dessa duracao.
 * Um jogador que trocar golpes com ela sai do encontro perdendo vida sem ter
 * ninguem por perto para culpar, e e exatamente por isso que tudo neste arquivo
 * gira em torno de tornar o veneno LEGIVEL antes de ele acontecer.</p>
 *
 * <p><b>Dois ataques, e a existencia do barato e o que explica o caro.</b> A
 * pinca avisa por 12 ticks, alcanca 1.1 e nao envenena; o ferrao avisa por 28,
 * alcanca 1.3, cobra metade do dano direto e envenena. {@link RegrasDeFerrao}
 * escolhe entre os dois e recusa uma configuracao em que o ferrao alcance menos
 * que a pinca -- invertidos, a pinca dispararia a cada recarga e a ferroada nunca
 * sairia, com o veneno implementado, testado e ausente do jogo. Os dois tambem tem
 * CLIPES proprios, e o gerador de animacao reprova um aviso de pinca que mexa a
 * cauda: se os dois telegrafos se parecessem, o jogador nao teria como saber qual
 * dos dois esta vindo.</p>
 *
 * <p><b>O acumulo mora no ALVO, e nao nela.</b> {@link RegrasDeVeneno} le o que
 * ainda resta do efeito na vitima e soma em cima. Guardado aqui, num mapa por
 * formiga, o teto deixaria de ser teto no unico caso em que ele importa -- um
 * esquadrao com mais de uma formiga venenosa -- e o mapa ainda vazaria uma entrada
 * por alvo tocado. O preco desta escolha esta escrito em {@link #envenenar}.</p>
 *
 * <p><b>Ela LIDERA, e a lideranca e promocao e nao molde.</b> O molde dela nasce
 * {@link SquadRole#FRONTLINER}, como manda {@code ChimeraDefinition}; o que a
 * torna lider e ter FUNDADO o bando em {@link SquadRegistry#criar}. Os membros
 * entram com o papel do PROPRIO molde, e formigas de rank igual ou superior sao
 * recusadas -- alistada como membro, uma segunda squadron leader perderia em
 * silencio o comportamento inteiro que a define.</p>
 *
 * <p><b>O Nen dela e uma INTENCAO, e o efeito e postura.</b>
 * {@link TacticalNenController} responde o que ela QUERIA fazer com aura; este
 * arquivo traduz isso em guarda fechada, que e uma decisao de combate do inimigo.
 * Nenhuma aura e calculada, nenhum custo e pago, nenhuma tecnica e ativada -- ver
 * {@link #FRACAO_DE_AURA_NAO_PUBLICADA} para o estado honesto disso hoje.</p>
 */
public final class ScorpionLeaderEntity extends BaseChimeraAnt
        implements software.bernie.geckolib.animatable.GeoEntity {

    /** Fase do ataque: o unico estado de combate que o cliente precisa conhecer. */
    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(ScorpionLeaderEntity.class, EntityDataSerializers.INT);
    /** Cambaleando: sincronizado porque interrupcao invisivel e stagger que so o servidor conhece. */
    private static final EntityDataAccessor<Boolean> CAMBALEANDO =
            SynchedEntityData.defineId(ScorpionLeaderEntity.class, EntityDataSerializers.BOOLEAN);
    /**
     * QUAL dos dois ataques esta em curso.
     *
     * <p>Sincronizado porque a fase sozinha nao basta: WINDUP de pinca e WINDUP de
     * ferrao sao a mesma fase e poses opostas. Sem este campo o cliente escolheria
     * um dos dois clipes para os dois ataques, o telegrafo longo deixaria de
     * existir na tela, e o jogador levaria veneno de um golpe que ele leu como o
     * barato. Nada disso da erro: o servidor acerta tudo.</p>
     */
    private static final EntityDataAccessor<Boolean> FERRAO_ARMADO =
            SynchedEntityData.defineId(ScorpionLeaderEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * As regras, montadas UMA vez.
     *
     * <p>Estaticas porque sao records imutaveis, sem estado de jogador. O erro que
     * este projeto ja sabe que comete -- estado de jogador num campo de classe --
     * seria guardar aqui o alvo, o acumulo de veneno ou o papel no bando; ai todas
     * as formigas do servidor dividiriam um. Alvo e papel vivem no {@link Squad};
     * o acumulo vive no proprio alvo.</p>
     */
    private static final RegrasDeFerrao REGRAS_DO_FERRAO = ScorpionLeaderTuning.ferrao();
    private static final RegrasDeVeneno REGRAS_DO_VENENO = ScorpionLeaderTuning.veneno();
    private static final SquadRules REGRAS_DO_ESQUADRAO = ScorpionLeaderTuning.esquadrao();
    private static final AttackHitbox CAIXA_DA_PINCA = ScorpionLeaderTuning.caixaDaPinca();
    private static final AttackHitbox CAIXA_DO_FERRAO = ScorpionLeaderTuning.caixaDoFerrao();

    /**
     * A fracao de aura que esta entidade declara: ZERO, e isso e honesto.
     *
     * <p>Aura e autoridade do Nen Foundation (CLAUDE.md), e nenhum nucleo publica
     * hoje a aura de um mob -- a superficie de leitura em {@code api/query} ainda
     * esta vazia. Inventar um valor aqui seria a segunda autoridade sobre Nen, e o
     * sintoma nao seria erro: seria uma formiga usando defesa de Nen porque um
     * inimigo se atribuiu aura.</p>
     *
     * <p>Com zero, {@link TacticalNenController} devolve {@code NENHUMA} em todo
     * tick -- ele para na propria reserva minima. A ligacao existe MONTADA e
     * INERTE, pelo mesmo motivo que {@code PercepcaoDeAuraDeChimera}: no dia em que
     * a aura de mob existir, a mudanca e uma linha aqui, e nao uma arqueologia
     * atras de todos os pontos onde alguem espalhou "se ela tem aura". A traducao
     * de intencao em postura -- {@code ScorpionLeaderTuning.guardaFechada} -- e
     * testada isoladamente, e nao depende deste zero.</p>
     */
    private static final double FRACAO_DE_AURA_NAO_PUBLICADA = 0.0D;

    /** Teto do contador de ferroadas: ele so precisa passar da guarda, nunca crescer para sempre. */
    private static final int TETO_DO_CONTADOR = ScorpionLeaderTuning.TICKS_DE_GUARDA_DO_FERRAO * 4;
    /** Ticks entre dois recalculos de rota. Repathing todo tick e custo puro. */
    private static final int TICKS_ENTRE_ROTAS = 10;
    /** Multiplicador de velocidade ao avancar. Ela e lenta; correr nao e o recurso dela. */
    private static final double VELOCIDADE_DE_AVANCO = 1.0D;
    /** Deslocamento por tick acima do qual o clipe passa de ocio para caminhada. */
    private static final double LIMIAR_DE_CAMINHADA = 0.02D;

    private static final String CHAVE_VENENO_NO_TETO =
            "message.nenfoundation.scorpion_leader.veneno_no_teto";
    private static final String CHAVE_ALVO_IMUNE =
            "message.nenfoundation.scorpion_leader.alvo_imune";

    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache cacheDeAnimacao =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    /** A escolha de ataque deste tick. Recalculada todo tick; consumida pelas Goals. */
    private DecisaoDeFerrao decisao = DecisaoDeFerrao.AGUARDAR;
    /** A intencao de Nen deste tick -- leitura de servidor, para gametest e diagnostico. */
    private TacticalNenIntent intencaoDeNen = TacticalNenIntent.NENHUMA;
    /**
     * Ticks desde a ultima ferroada.
     *
     * <p>Comeca no teto de proposito: uma formiga recem-nascida nao pode ter de
     * esperar sete segundos antes de poder ferroar pela primeira vez. Comecando em
     * zero, o primeiro encontro dela seria so pinca, e o jogador terminaria a briga
     * sem nunca ver o que a distingue.</p>
     */
    private int ticksDesdeAUltimaFerroada = TETO_DO_CONTADOR;
    /** A instancia de ataque que ja recebeu o arranco -- um por ferroada, nunca quatro. */
    private long ultimaInstanciaComArranco;
    /** Contador de repathing; a rota so e refeita a cada TICKS_ENTRE_ROTAS. */
    private int ticksAteReplanejar;

    public ScorpionLeaderEntity(EntityType<? extends ScorpionLeaderEntity> type, Level level) {
        super(type, level, ChimeraProfiles.scorpionLeader().metadata(),
                new AwarenessTuning(ScorpionLeaderTuning.TICKS_DE_AVISO,
                        ScorpionLeaderTuning.MEMORIA_DE_ALVO_TICKS),
                ChimeraProfiles.scorpionLeaderMolde());
        instalarRuntime(montarRuntime());
    }

    private EnemyRuntime montarRuntime() {
        var perfil = ChimeraProfiles.scorpionLeader();
        PerceptionController percepcao = new PerceptionController(
                PerceptionBudget.padrao(),
                ScorpionLeaderTuning.coneDeVisao(perfil.attributes().followRange()),
                new ThreatMemory(ScorpionLeaderTuning.MEMORIA_DE_ALVO_TICKS),
                new TargetEvaluator(FactionRelations.padrao(), perfil.metadata().faction(),
                        perfil.attributes().followRange(), perfil.metadata().territorial()),
                // A porta de aura da formiga continua INERTE enquanto a #126 (Gyo)
                // estiver aberta no nucleo: inventar aqui a regra de quem-ve-o-que
                // seria a segunda autoridade sobre Nen que o CLAUDE.md proibe.
                PercepcaoDeAura.NENHUMA, ScorpionLeaderTuning.ALCANCE_DE_AUDICAO,
                getUUID().hashCode());
        return new EnemyRuntime(percepcao,
                new EnemyBrain(new AwarenessTuning(ScorpionLeaderTuning.TICKS_DE_AVISO,
                        ScorpionLeaderTuning.MEMORIA_DE_ALVO_TICKS)),
                new AttackController(ChimeraProfiles.scorpionLeaderRecarga()),
                new StaggerState(ChimeraProfiles.scorpionLeaderStagger()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = ChimeraProfiles.scorpionLeader().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<ScorpionLeaderEntity> registeredType() { return EnemyEntityTypes.SCORPION_LEADER.get(); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        builder.define(CAMBALEANDO, false);
        builder.define(FERRAO_ARMADO, false);
    }

    public AttackPhase faseDeAtaque() {
        int ordinal = this.entityData.get(FASE_DE_ATAQUE);
        AttackPhase[] fases = AttackPhase.values();
        return ordinal >= 0 && ordinal < fases.length ? fases[ordinal] : AttackPhase.IDLE;
    }

    public boolean cambaleando() { return this.entityData.get(CAMBALEANDO); }

    /** O ataque em curso e o ferrao? E ele que separa os clipes no cliente. */
    public boolean ferraoArmado() { return this.entityData.get(FERRAO_ARMADO); }

    /** A escolha deste tick -- leitura de SERVIDOR, para gametest e diagnostico. */
    public DecisaoDeFerrao decisaoDeFerrao() { return decisao; }

    /** A intencao de Nen deste tick -- leitura de SERVIDOR, nunca um comando. */
    public TacticalNenIntent intencaoDeNen() { return intencaoDeNen; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new AtaqueGoal(this));
        goalSelector.addGoal(3, new AvancoGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Sem target goal vanilla: quem escolhe alvo e o TargetEvaluator, por
        // faccao. Um NearestAttackableTargetGoal ao lado seria uma segunda
        // autoridade sobre a mesma decisao, e as duas discordariam em silencio --
        // aqui com um agravante, porque o alvo dela e tambem o alvo do esquadrao.
    }

    // ------------------------------------------------------------------ tick

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;
        EnemyRuntime runtime = runtimeExigido();
        PerceptionSnapshot snapshot = runtime.tick(tickCount, this::varrerCandidatos,
                getHealth() <= getMaxHealth() * ScorpionLeaderTuning.FRACAO_DE_VIDA_CRITICA, false);
        aplicarAlvo(snapshot);

        // O contador para no teto em vez de crescer para sempre: ele so precisa
        // responder "ja passou da guarda?", e um int que sobe por toda a vida de um
        // mob acabaria estourando -- com o sinal virando, a guarda passando a nunca
        // ser atingida, e o ferrao sumindo do encontro sem nenhum erro no log.
        if (ticksDesdeAUltimaFerroada < TETO_DO_CONTADOR) ticksDesdeAUltimaFerroada++;

        intencaoDeNen = decidirIntencaoDeNen();
        decisao = escolherAtaque(runtime);

        if (REGRAS_DO_ESQUADRAO.atualizaNesteTick(tickCount, getUUID().hashCode())) {
            comandarEsquadrao((ServerLevel) level());
        }

        tickDoGolpe(runtime);
        publicarEstado(runtime);
    }

    /**
     * A escolha entre pinca e ferrao, delegada inteira a regra pura.
     *
     * <p>Nenhum {@code if} de ataque mora aqui de proposito. Espalhados pela
     * entidade, os limiares de alcance e a guarda do ferrao acabariam repetidos
     * dentro das Goals -- e duas copias do mesmo limiar divergem na primeira sessao
     * de balanceamento, sem que nada reprove.</p>
     */
    private DecisaoDeFerrao escolherAtaque(EnemyRuntime runtime) {
        LivingEntity alvo = getTarget();
        boolean visivel = alvo != null && alvo.isAlive() && hasLineOfSight(alvo);
        double distancia = visivel ? distanceTo(alvo) : 0.0D;
        return REGRAS_DO_FERRAO.decidir(distancia, visivel,
                runtime.stagger().cambaleando(), runtime.ataques().canStart(),
                ticksDesdeAUltimaFerroada,
                ScorpionLeaderTuning.guardaFechada(intencaoDeNen));
    }

    /**
     * Pergunta ao controlador tatico o que ela QUERIA fazer com Nen.
     *
     * <p>Ela nao calcula aura, nao paga custo e nao ativa tecnica: recebe uma
     * intencao, e o unico uso dela e postura. Ver
     * {@link #FRACAO_DE_AURA_NAO_PUBLICADA} para por que a resposta e sempre
     * {@code NENHUMA} hoje -- e por que isso e o estado honesto, e nao um
     * esquecimento.</p>
     */
    private TacticalNenIntent decidirIntencaoDeNen() {
        Optional<TacticalNenController> controlador = nenTatico();
        if (controlador.isEmpty()) return TacticalNenIntent.NENHUMA;
        LivingEntity alvo = getTarget();
        boolean emCombate = alvo != null && alvo.isAlive();
        boolean aoAlcance = emCombate
                && distanceTo(alvo) <= ScorpionLeaderTuning.ALCANCE_DO_FERRAO;
        double vida = Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F);
        // "Sob pressao" e medido de graca: hurtTime so e positivo nos ticks logo
        // depois de um golpe. Contar atacantes por varredura custaria uma consulta
        // de mundo por tick, fora do PerceptionBudget -- exatamente o gasto que a
        // fundacao existe para impedir, e que nao aparece como erro, so como TPS.
        boolean sobPressao = this.hurtTime > 0 && vida <= 0.5D;
        // podeFugir = false: ela e a ANCORA do esquadrao. Um recuo dela deixaria os
        // membros postados em torno de uma peca que saiu de campo.
        return controlador.get().decidir(new TacticalNenSituation(
                FRACAO_DE_AURA_NAO_PUBLICADA, vida, emCombate, aoAlcance, false,
                sobPressao, false));
    }

    // -------------------------------------------------------------- esquadrao

    /**
     * Funda o esquadrao, recruta quem cabe e publica o alvo -- e so aqui.
     *
     * <p>Concentrado num ponto de proposito, e no orcamento de
     * {@link SquadRules#atualizaNesteTick}. Espalhado pelas Goals, cada uma leria e
     * escreveria o bando no proprio ritmo, e duas delas trocariam o alvo no mesmo
     * tick; a ultima venceria, e o sintoma e um esquadrao indeciso sem causa
     * visivel. E fora do orcamento, a varredura de recrutamento viraria uma consulta
     * de mundo por tick por lider.</p>
     */
    private void comandarEsquadrao(ServerLevel nivel) {
        SquadRegistry registro = RegistroDeMatilhas.doNivel(nivel);
        Squad esquadrao = registro.bandoDe(getUUID()).orElse(null);
        if (esquadrao == null) {
            // Ela FUNDA, e por isso e a lider: em Squad, lideranca e promocao, e
            // quem cria e promovido no construtor. Entrar como LEADER num bando
            // existente e recusado la, e com razao -- dois lideres dariam duas
            // ordens ao mesmo bando.
            esquadrao = registro.criar(UUID.randomUUID(), REGRAS_DO_ESQUADRAO, getUUID());
            alistarNoEsquadrao(esquadrao.id());
        }
        recrutar(registro, esquadrao);
        publicarAlvoDoEsquadrao(esquadrao);
        // A faxina roda no MESMO ritmo do comando, e nao "quando der": bando sem
        // ninguem nao consome tick e nao aparece em lugar nenhum -- ele so ocupa
        // memoria e pode ressuscitar mais tarde com o alvo de uma hora atras.
        registro.removerDissolvidos();
    }

    /**
     * Alista as formigas soltas que cabem no teto.
     *
     * <p><b>Rank igual ou superior e RECUSADO.</b> Uma segunda squadron leader
     * alistada como membro perderia o papel de lider dela para sempre: o
     * {@code comandarEsquadrao} dela encontraria um bando e nunca fundaria o seu.
     * Isso nao daria erro -- daria um oficial que atravessa o encontro inteiro sem
     * o comportamento que o define.</p>
     *
     * <p>Os membros entram com o papel do PROPRIO molde. Nao ha guarda contra
     * {@code LEADER} aqui porque {@code ChimeraDefinition} ja recusa no construtor
     * um molde que nasca lider -- a invariante e cobrada uma vez, la, em vez de ser
     * relembrada em cada chamador.</p>
     */
    private void recrutar(SquadRegistry registro, Squad esquadrao) {
        if (esquadrao.tamanho() >= REGRAS_DO_ESQUADRAO.maximoDeMembros()) return;
        AABB area = getBoundingBox().inflate(REGRAS_DO_ESQUADRAO.raioDeReforco());
        for (BaseChimeraAnt formiga : level().getEntitiesOfClass(BaseChimeraAnt.class, area,
                outra -> outra != this && outra.isAlive())) {
            if (esquadrao.tamanho() >= REGRAS_DO_ESQUADRAO.maximoDeMembros()) return;
            if (formiga.molde().rank().ordinal() >= ChimeraRank.SQUADRON_LEADER.ordinal()) continue;
            if (registro.bandoDe(formiga.getUUID()).isPresent()) continue;
            SquadRole papel = formiga.molde().papelNoSquad();
            if (registro.entrar(esquadrao.id(), formiga.getUUID(), papel)) {
                formiga.alistarNoEsquadrao(esquadrao.id());
            }
        }
    }

    /**
     * So a LIDER escreve o alvo do esquadrao.
     *
     * <p>Com qualquer membro podendo escrever, um que enxergasse um segundo jogador
     * trocaria o alvo no meio do cerco e o esquadrao oscilaria entre dois alvos sem
     * nada acusar. Aqui a regra e trivial porque ela e a unica com esta classe -- e
     * esta escrita assim mesmo, para que o proximo oficial a entrar no bando
     * encontre a regra em vez de inventar a dele.</p>
     */
    private void publicarAlvoDoEsquadrao(Squad esquadrao) {
        LivingEntity alvo = getTarget();
        UUID id = alvo != null && alvo.isAlive() ? alvo.getUUID() : null;
        if (!Objects.equals(esquadrao.alvo().orElse(null), id)) esquadrao.alvo(id);
    }

    // ------------------------------------------------------------- percepcao

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

    /** Varredura de mundo da percepcao -- a UNICA, e so quando o orcamento autoriza. */
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

    // ---------------------------------------------------------------- golpe

    /**
     * A janela ACTIVE consulta a caixa do ataque em curso; fora dela nada existe.
     *
     * <p>Roda TODO tick, e nao no orcamento do esquadrao: quatro ticks de janela de
     * ferrao medidos de dez em dez ticks acertariam ou errariam por sorte.</p>
     */
    private void tickDoGolpe(EnemyRuntime runtime) {
        AttackController ataques = runtime.ataques();
        if (ataques.phase() != AttackPhase.ACTIVE) return;
        boolean ferrao = ferraoArmado();
        if (ferrao) aplicarArranco(ataques.attackInstanceId());

        LivingEntity alvo = getTarget();
        if (alvo == null || !alvo.isAlive()) return;
        AttackHitbox caixa = ferrao ? CAIXA_DO_FERRAO : CAIXA_DA_PINCA;
        float empurrao = ferrao ? ScorpionLeaderTuning.EMPURRAO_DO_FERRAO
                : ScorpionLeaderTuning.EMPURRAO_DA_PINCA;
        ataques.tryHit(alvo.getId(), alvo.getBoundingBox(), caixa,
                        position(), getYRot(), "body")
                .ifPresent(hit -> {
                    alvo.hurt(damageSources().mobAttack(this), hit.damage());
                    alvo.knockback(empurrao, getX() - alvo.getX(), getZ() - alvo.getZ());
                    // O veneno sai daqui, e SO daqui: um segundo ponto de aplicacao
                    // -- num handler de dano, por exemplo -- somaria duas doses por
                    // ferroada, e o numero final seria plausivel demais para alguem
                    // notar sem medir.
                    if (ferrao) envenenar(alvo);
                });
    }

    /**
     * Aplica o veneno lendo o que o alvo JA carrega.
     *
     * <p><b>O acumulador e o proprio efeito vanilla.</b> {@link RegrasDeVeneno} soma
     * duracao em cima do que resta e deriva o nivel dessa duracao; o decaimento e o
     * relogio do efeito escorrendo sozinho. Isso da UMA fonte de verdade, faz o teto
     * valer para o encontro inteiro (e nao por formiga) e mantem o veneno correndo
     * depois que ela morre.</p>
     *
     * <p><b>Por que POISON e nao um efeito proprio, e o que isso custa.</b>
     * Registrar um {@code MobEffect} proprio mexeria em {@code enemy/registry}, que
     * nao pertence a esta frente. Mas a escolha tambem se defende sozinha: POISON
     * nao MATA -- ele para em um de vida --, e essa e a unica coisa entre o acumulo
     * e uma execucao por uma fonte de dano que o jogador nao consegue apontar. O
     * preco e real e esta declarado: o efeito e compartilhado, entao uma pocao de
     * veneno que o proprio jogador tomou entra na mesma conta, e um balde de leite
     * apaga o preco inteiro do telegrafo mais caro do bicho.</p>
     *
     * <p>As duas recusas tem mensagem: uma ferroada anunciada por 28 ticks que nao
     * faz nada e o pior relato de bug que existe.</p>
     *
     * <p><b>Sobre {@code canBeAffected}, que o compilador marca como depreciada.</b>
     * Ela continua sendo a unica pergunta que responde "este alvo sofre veneno?"
     * ANTES de tentar aplicar. A alternativa -- chamar {@code addEffect} e ler o
     * retorno -- confunde duas coisas diferentes: ele tambem devolve falso quando o
     * alvo ja carrega um efeito mais forte de OUTRA fonte, e a formiga passaria a
     * dizer "alvo imune" a um jogador que acabou de beber uma pocao. Recusa com o
     * motivo errado ensina o jogador a fazer a coisa errada, que e pior do que nao
     * avisar.</p>
     */
    private void envenenar(LivingEntity alvo) {
        MobEffectInstance atual = alvo.getEffect(MobEffects.POISON);
        int restantes = atual == null ? 0 : Math.max(0, atual.getDuration());
        boolean pode = alvo.canBeAffected(new MobEffectInstance(MobEffects.POISON,
                REGRAS_DO_VENENO.duracaoPorFerroadaEmTicks(), 0));
        DoseDeVeneno dose = REGRAS_DO_VENENO.aplicar(pode, restantes);
        switch (dose.decisao()) {
            case ALVO_IMUNE -> avisar(alvo, CHAVE_ALVO_IMUNE);
            case NO_TETO -> avisar(alvo, CHAVE_VENENO_NO_TETO);
            case APLICOU -> { }
        }
        if (!dose.aplicavel()) return;
        // ambient=false, visivel=true, icone=true: o jogador precisa VER o nivel
        // subir. Um veneno sem icone seria dano continuo sem origem declarada, que e
        // exatamente o que este arquivo inteiro existe para evitar.
        alvo.addEffect(new MobEffectInstance(MobEffects.POISON, dose.duracaoEmTicks(),
                dose.amplificador(), false, true, true), this);
    }

    /** Aviso na barra de acao. Recusa, como desfecho, sempre tem mensagem traduzida. */
    private void avisar(LivingEntity quem, String chave) {
        if (quem instanceof Player jogador) {
            jogador.displayClientMessage(Component.translatable(chave), true);
        }
    }

    /**
     * O arranco que paga a diferenca entre o ferrao desenhado e a caixa do ferrao.
     *
     * <p>UMA vez por instancia de ataque. Aplicado nos quatro ticks da janela, ela
     * atravessaria o alvo e sairia do outro lado -- e {@code tryHit}, que so acerta
     * cada alvo uma vez por instancia, esconderia o defeito atras de um dano
     * perfeitamente correto.</p>
     */
    private void aplicarArranco(long instancia) {
        if (instancia == ultimaInstanciaComArranco) return;
        ultimaInstanciaComArranco = instancia;
        Vec3 olhar = getLookAngle();
        Vec3 frente = new Vec3(olhar.x, 0.0D, olhar.z);
        if (frente.lengthSqr() < 1.0E-6D) return;
        frente = frente.normalize().scale(ScorpionLeaderTuning.AVANCO_DO_FERRAO);
        setDeltaMovement(getDeltaMovement().add(frente.x, 0.0D, frente.z));
        this.hasImpulse = true;
    }

    /**
     * Apanhar interrompe o golpe E abala o esquadrao.
     *
     * <p>Nao ha ponto fraco neste bicho, e a ausencia e deliberada: a licao dele e
     * escolher QUANDO entrar, e nao onde mirar. Um multiplicador por regiao daria ao
     * jogador uma resposta de precisao para um problema de tempo.</p>
     *
     * <p>Dano sem atacante -- fogo, queda -- nao cambaleia e nao abala: cambalear
     * por queimadura transformaria fogo numa interrupcao permanente do telegrafo
     * mais caro do bicho, e incendiar o chao passaria a ser a resposta certa para um
     * encontro que nao e sobre isso.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean levou = super.hurt(source, amount);
        if (!levou || level().isClientSide) return levou;
        if (!(source.getEntity() instanceof LivingEntity)) return true;

        EnemyRuntime runtime = runtimeExigido();
        StaggerResult resultado = runtime.sofrerStagger(runtime.ataques().attackInstanceId(),
                amount, ScorpionLeaderTuning.RECARGA_APOS_INTERRUPCAO);
        if (resultado == StaggerResult.DISPAROU) {
            getNavigation().stop();
            this.entityData.set(CAMBALEANDO, true);
            this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
            this.entityData.set(FERRAO_ARMADO, false);
        }
        RegistroDeMatilhas.doNivel((ServerLevel) level()).bandoDe(getUUID())
                .ifPresent(bando -> bando.abalar(ScorpionLeaderTuning.ABALO_POR_GOLPE_NA_LIDER));
        return true;
    }

    /**
     * Publica fase, cambaleio e estado de combate.
     *
     * <p>WINDUP, ACTIVE e RECOVERY sao publicados como estado de COMBATE, e nao so
     * como fase: e a borda de {@code WINDUP} que {@code BaseHxHMob} usa para tocar a
     * voz de ataque. Sem publicar, a formiga armaria o ferrao em silencio -- e este
     * e o bicho do mod em que um telegrafo mudo custa mais caro.</p>
     */
    private void publicarEstado(EnemyRuntime runtime) {
        AttackPhase fase = runtime.ataques().phase();
        if (this.entityData.get(FASE_DE_ATAQUE) != fase.ordinal()) {
            this.entityData.set(FASE_DE_ATAQUE, fase.ordinal());
        }
        boolean cambaleando = runtime.stagger().cambaleando();
        if (this.entityData.get(CAMBALEANDO) != cambaleando) {
            this.entityData.set(CAMBALEANDO, cambaleando);
        }
        if (combatState() == EnemyCombatState.DYING) return;
        combatState(cambaleando ? EnemyCombatState.STAGGERED : switch (fase) {
            case WINDUP -> EnemyCombatState.WINDUP;
            case ACTIVE -> EnemyCombatState.ACTIVE;
            case RECOVERY -> EnemyCombatState.RECOVERY;
            default -> getTarget() != null ? EnemyCombatState.AGGRO : EnemyCombatState.IDLE;
        });
    }

    // ---------------------------------------------------------- ciclo de vida

    /** Morrer PUBLICA o repouso: depois de morto o passo de IA nao roda mais. */
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) publicarRepouso();
        super.die(source);
    }

    /**
     * O UNICO ponto de saida do esquadrao e da intencao de Nen.
     *
     * <p>Morte, unload de chunk, troca de dimensao e comando administrativo passam
     * todos por {@code remove}. Espalhar a saida por {@code die} mais um evento de
     * unload mais outro de dimensao e como um deles fica esquecido -- e o esquecido
     * nao da erro: deixa o esquadrao contando uma lider que nao existe mais,
     * segurando um lugar do teto para sempre e publicando o alvo de um combate que
     * acabou.</p>
     */
    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide && level() instanceof ServerLevel nivel) {
            publicarRepouso();
            // Quem liga, desliga: o controlador tatico e criado junto com a
            // identidade e nao tem outro ponto de saida.
            nenTatico().ifPresent(TacticalNenController::limpar);
            intencaoDeNen = TacticalNenIntent.NENHUMA;
            SquadRegistry registro = RegistroDeMatilhas.doNivel(nivel);
            registro.sair(getUUID());
            registro.removerDissolvidos();
        }
        super.remove(reason);
    }

    private void publicarRepouso() {
        this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        this.entityData.set(CAMBALEANDO, false);
        this.entityData.set(FERRAO_ARMADO, false);
    }

    // ------------------------------------------------------------- animacao

    @Override
    public void registerControllers(
            software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<ScorpionLeaderEntity>(
                this, "corpo", 4, estado -> {
                    if (this.deathTime > 0) return estado.setAndContinue(DEATH);
                    if (cambaleando()) return estado.setAndContinue(STAGGER);
                    // O par de clipes vem do ATAQUE em curso, e nao so da fase: os
                    // dois golpes compartilham WINDUP/ACTIVE/RECOVERY e mostram
                    // coisas opostas. Ler so a fase daria o mesmo aviso para o golpe
                    // que envenena e para o que nao envenena.
                    boolean ferrao = ferraoArmado();
                    return switch (faseDeAtaque()) {
                        case WINDUP -> estado.setAndContinue(ferrao ? STING_WINDUP : WINDUP);
                        case ACTIVE -> estado.setAndContinue(ferrao ? STING : STRIKE);
                        case RECOVERY -> estado.setAndContinue(
                                ferrao ? STING_RECOVERY : RECOVERY);
                        default -> estado.setAndContinue(
                                velocidadeHorizontal() >= LIMIAR_DE_CAMINHADA ? WALK : IDLE);
                    };
                }));
    }

    // Contrato com scorpion_leader.animation.json. LoopType.DEFAULT em todos: o tipo de
    // repeticao mora no arquivo, e cravar em Java o faria vencer o JSON.
    private static final software.bernie.geckolib.animation.RawAnimation IDLE = clipe("idle");
    private static final software.bernie.geckolib.animation.RawAnimation WALK = clipe("walk");
    private static final software.bernie.geckolib.animation.RawAnimation WINDUP = clipe("windup");
    private static final software.bernie.geckolib.animation.RawAnimation STRIKE = clipe("strike");
    private static final software.bernie.geckolib.animation.RawAnimation RECOVERY = clipe("recovery");
    private static final software.bernie.geckolib.animation.RawAnimation STING_WINDUP =
            clipe("sting_windup");
    private static final software.bernie.geckolib.animation.RawAnimation STING = clipe("sting");
    private static final software.bernie.geckolib.animation.RawAnimation STING_RECOVERY =
            clipe("sting_recovery");
    private static final software.bernie.geckolib.animation.RawAnimation STAGGER = clipe("stagger");
    private static final software.bernie.geckolib.animation.RawAnimation DEATH = clipe("death");

    private static software.bernie.geckolib.animation.RawAnimation clipe(String nome) {
        return software.bernie.geckolib.animation.RawAnimation.begin()
                .then("animation.scorpion_leader." + nome,
                        software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    }

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

    // ------------------------------------------------------------------ goals

    /**
     * Dispara e conduz o golpe que {@link RegrasDeFerrao} escolheu.
     *
     * <p>Ela nao DECIDE nada: le {@code decisao}, que o passo de IA escreveu. Uma
     * Goal que reescolhesse o ataque por conta propria seria a segunda autoridade
     * sobre a mesma pergunta, e as duas divergiriam no tick seguinte a uma ferroada
     * -- com a guarda do ferrao valendo num lado e nao no outro.</p>
     */
    private static final class AtaqueGoal extends Goal {
        private final ScorpionLeaderEntity formiga;

        private AtaqueGoal(ScorpionLeaderEntity formiga) {
            this.formiga = formiga;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (formiga.level().isClientSide) return false;
            return formiga.decisao == DecisaoDeFerrao.GOLPEAR_COM_PINCA
                    || formiga.decisao == DecisaoDeFerrao.ARMAR_O_FERRAO;
        }

        @Override public boolean canContinueToUse() {
            // O golpe continua ate a fase acabar, MESMO que a decisao ja tenha
            // mudado. Cortar no meio porque o alvo saiu de alcance deixaria o
            // jogador vendo um ataque que some -- e o servidor ja cobrou o windup
            // inteiro por ele.
            AttackPhase fase = formiga.runtimeExigido().ataques().phase();
            return fase != AttackPhase.IDLE && fase != AttackPhase.COMPLETE
                    && !formiga.runtimeExigido().stagger().cambaleando();
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() {
            formiga.getNavigation().stop();
            boolean ferrao = formiga.decisao == DecisaoDeFerrao.ARMAR_O_FERRAO;
            AttackDefinition definicao = ferrao
                    ? ScorpionLeaderTuning.ferroada() : ScorpionLeaderTuning.pinca();
            // O campo sincronizado e escrito ANTES de start(): o cliente escolhe o
            // clipe a partir da fase, e se a fase chegasse primeiro ele tocaria um
            // quadro do aviso errado. Um quadro e pouco, e e o PRIMEIRO -- que e
            // justamente o que o jogador usa para decidir se recua.
            formiga.entityData.set(FERRAO_ARMADO, ferrao);
            if (ferrao) formiga.ticksDesdeAUltimaFerroada = 0;
            formiga.runtimeExigido().ataques().start(definicao);
        }

        @Override public void tick() {
            LivingEntity alvo = formiga.getTarget();
            // Encarar o alvo so ate o fim do WINDUP: a partir da janela ACTIVE a
            // direcao esta TRAVADA. Sem travar, a ferroada viraria mira-laser -- ela
            // giraria junto com quem desvia, e o telegrafo de 28 ticks, que e a
            // unica defesa contra o veneno, deixaria de valer alguma coisa.
            AttackPhase fase = formiga.runtimeExigido().ataques().phase();
            if (alvo != null && fase == AttackPhase.WINDUP) {
                formiga.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            }
            // Freia no chao fora da janela ativa. O arranco do ferrao e aplicado uma
            // unica vez; sem o freio, ele se somaria a inercia da caminhada e ela
            // passaria direto pelo alvo com a caixa junto.
            if (fase != AttackPhase.ACTIVE) {
                formiga.setDeltaMovement(formiga.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
            }
        }

        @Override public void stop() {
            formiga.getNavigation().stop();
            // O ataque acabou: o cliente volta a nao ter ferrao armado. Deixar o
            // campo ligado faria o proximo golpe de pinca tocar o clipe do ferrao
            // ate o servidor publicar a fase -- o aviso errado, no quadro que mais
            // importa.
            formiga.entityData.set(FERRAO_ARMADO, false);
        }
    }

    /**
     * Aproxima quando ha alvo, e PLANTA quando a postura manda segurar.
     *
     * <p>As duas coisas na mesma Goal de proposito: elas disputam o mesmo
     * {@code Flag.MOVE}, e duas Goals disputando o mesmo flag se revezam a cada tick
     * -- o que aparece como um bicho tremendo no lugar, sem causa visivel.</p>
     */
    private static final class AvancoGoal extends Goal {
        private final ScorpionLeaderEntity formiga;

        private AvancoGoal(ScorpionLeaderEntity formiga) {
            this.formiga = formiga;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (formiga.level().isClientSide) return false;
            if (formiga.runtimeExigido().stagger().cambaleando()) return false;
            LivingEntity alvo = formiga.getTarget();
            if (alvo == null || !alvo.isAlive()) return false;
            return formiga.decisao == DecisaoDeFerrao.AGUARDAR
                    || formiga.decisao == DecisaoDeFerrao.SEGURAR_A_GUARDA;
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() { formiga.ticksAteReplanejar = 0; }

        @Override public void tick() {
            LivingEntity alvo = formiga.getTarget();
            if (alvo == null) return;
            formiga.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            if (formiga.decisao == DecisaoDeFerrao.SEGURAR_A_GUARDA) {
                // Guarda fechada: ela encara e NAO avanca. Continuar andando
                // enquanto a postura manda segurar faria a intencao de Nen nao ter
                // efeito nenhum na tela -- uma decisao tomada e invisivel.
                formiga.getNavigation().stop();
                return;
            }
            // A rota so e refeita de tempos em tempos. Chamar moveTo todo tick manda
            // o pathfinder recalcular vinte vezes por segundo por formiga: num
            // esquadrao de oito isso e custo puro, e o sintoma nao e erro -- e TPS
            // caindo enquanto o grupo esta em tela.
            if (formiga.ticksAteReplanejar > 0) {
                formiga.ticksAteReplanejar--;
                return;
            }
            formiga.ticksAteReplanejar = TICKS_ENTRE_ROTAS;
            formiga.getNavigation().moveTo(alvo, VELOCIDADE_DE_AVANCO);
        }

        @Override public void stop() { formiga.getNavigation().stop(); }
    }
}
