package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.DecisaoDeFlanco;
import com.darkcontinent.nenfoundation.enemy.ai.DecisaoDeRastro;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.ai.RegistroDeMatilhas;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeFlanco;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeRastro;
import com.darkcontinent.nenfoundation.enemy.ai.squad.Squad;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadController;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadInput;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadOrder;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRegistry;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRole;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.base.BaseChimeraAnt;
import com.darkcontinent.nenfoundation.enemy.base.EnemyRuntime;
import com.darkcontinent.nenfoundation.enemy.combat.AttackController;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHit;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.DecisaoDeSalto;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeSalto;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import com.darkcontinent.nenfoundation.enemy.content.ChimeraProfiles;
import com.darkcontinent.nenfoundation.enemy.content.WolfRunnerTuning;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Wolf Runner -- formiga quimera, rank PEON. O que FLANQUEIA.
 *
 * <p>Ele nao vence pelo golpe: vence por CHEGAR. Tres comportamentos fazem isso,
 * e cada um existe contra uma leitura errada do encontro.</p>
 *
 * <p><b>1. Memoria de rastro com prazo.</b> Perder o alvo de vista NAO encerra a
 * perseguicao: ele vai ate onde o cheiro termina. O prazo
 * ({@link WolfRunnerTuning#TICKS_DE_RASTRO}) e o que separa "tenso" de
 * "injusto" -- curto demais, fugir e dobrar uma esquina; longo demais, o bicho
 * atravessa o mapa atras de uma memoria e nunca desengaja. Quando o prazo vence,
 * ele DESISTE, e o esquecimento e explicito: a {@code ThreatMemory} tambem e
 * apagada, senao ela devolveria o mesmo alvo no tick seguinte e o prazo do
 * rastro seria uma frase falsa. A regra mora em {@link RegrasDeRastro}, fora
 * desta classe, porque o prazo precisa ser provavel sem abrir um jogo.</p>
 *
 * <p><b>2. Flanquear nao e perseguir.</b> Em esquadrao ele NAO vai pelo caminho
 * mais curto: anda para um posto medido a partir do OLHAR do alvo e so fecha
 * depois de ter saido do arco frontal dele. Quem segura a frente -- o lider e o
 * frontliner -- e a razao de o flanco funcionar, e por isso
 * {@link RegrasDeFlanco#papelContorna} distingue os dois grupos. <b>Sozinho ele
 * recua</b>: um bicho de 28 de vida que avanca sem ninguem segurando a frente
 * morre de graca, e o que se perde nao e dificuldade, e a licao.</p>
 *
 * <p><b>3. O bote e um salto com telegrafo, e nao um teleporte.</b> Ele fecha os
 * ultimos metros com um salto: 16 ticks de agachamento visivel, depois UM
 * impulso. O aviso e cobrado nos dois lados -- {@link RegrasDeSalto} recusa
 * telegrafo abaixo do minimo legivel, e {@code wolf_runner_animacoes.py} recusa
 * um clipe de aviso que nao dure o aviso inteiro nem mostre o agachamento. Sem
 * esse par, o impulso continua saindo igual e o jogador ve um bicho que aparece
 * em cima dele -- dano certo, cooldown certo, log limpo.</p>
 *
 * <p><b>O que ele NAO faz:</b> nao ativa tecnica de Nen. A intencao tatica mora
 * em {@code TacticalNenController}, herdado de {@link BaseChimeraAnt}, e
 * continua inerte enquanto o nucleo nao expuser a porta -- inventar aqui a
 * ativacao seria a segunda autoridade sobre Nen que o CLAUDE.md proibe.</p>
 */
public final class WolfRunnerEntity extends BaseChimeraAnt
        implements software.bernie.geckolib.animatable.GeoEntity {

    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(WolfRunnerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CAMBALEANDO =
            SynchedEntityData.defineId(WolfRunnerEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * As pecas imutaveis da ficha, resolvidas uma vez.
     *
     * <p>Estaticas porque NENHUMA delas guarda estado de jogador -- sao records
     * imutaveis. Um campo estatico com estado seria o erro classico deste
     * projeto: dois bichos decidindo com a mesma memoria.</p>
     */
    private static final RegrasDeRastro RASTRO = WolfRunnerTuning.rastro();
    private static final RegrasDeFlanco FLANCO = WolfRunnerTuning.flanco();
    private static final RegrasDeSalto SALTO = WolfRunnerTuning.salto();
    private static final AttackHitbox CAIXA_DA_MORDIDA = WolfRunnerTuning.caixaDaMordida();
    private static final SquadRules REGRAS_DO_ESQUADRAO = SquadRules.esquadrao();

    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache cacheDeAnimacao =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    // ------------------------------------------------------- estado de servidor
    //
    // Tudo abaixo e POR INSTANCIA e so existe no servidor. Um destes campos em
    // static seria a segunda armadilha que o CLAUDE.md lista: dois lobos
    // escrevendo no mesmo rastro, e o sintoma seria uma matilha inteira correndo
    // para o mesmo ponto errado.

    /** O que o flanco decidiu no ultimo tick de coordenacao. */
    private DecisaoDeFlanco decisao = DecisaoDeFlanco.AGUARDAR;
    /** De quem e o rastro que ele carrega; nulo quando nao ha nenhum. */
    private UUID alvoDoRastro;
    /** Onde o alvo foi visto pela ultima vez. */
    private Vec3 rastro;
    /** Ticks desde o ultimo CONTATO com o alvo. Zero enquanto ele esta a vista. */
    private int ticksDesdeOContato;
    /** O alvo estava visivel no ultimo tick de percepcao. */
    private boolean alvoVisivel;
    /** O golpe em curso e o bote, e nao a mordida curta. */
    private boolean boteEmCurso;
    /** Instancia de ataque que ja recebeu o impulso; impede somar o salto todo tick. */
    private long instanciaComImpulso;

    public WolfRunnerEntity(EntityType<? extends WolfRunnerEntity> type, Level level) {
        super(type, level, ChimeraProfiles.wolfRunner().metadata(),
                new AwarenessTuning(WolfRunnerTuning.TICKS_DE_AVISO,
                        WolfRunnerTuning.MEMORIA_DE_ALVO_TICKS),
                ChimeraProfiles.wolfRunnerMolde());
        instalarRuntime(montarRuntime());
    }

    private EnemyRuntime montarRuntime() {
        var perfil = ChimeraProfiles.wolfRunner();
        PerceptionController percepcao = new PerceptionController(
                PerceptionBudget.padrao(),
                VisionCone.deGraus(perfil.attributes().followRange(),
                        WolfRunnerTuning.MEIA_ABERTURA_DA_VISAO_EM_GRAUS),
                new ThreatMemory(WolfRunnerTuning.MEMORIA_DE_ALVO_TICKS),
                new TargetEvaluator(FactionRelations.padrao(), perfil.metadata().faction(),
                        perfil.attributes().followRange(), perfil.metadata().territorial()),
                // A porta de aura da formiga continua INERTE enquanto a #126 (Gyo)
                // estiver aberta no nucleo: inventar aqui a regra de quem-ve-o-que
                // seria a segunda autoridade sobre Nen que o CLAUDE.md proibe.
                PercepcaoDeAura.NENHUMA, WolfRunnerTuning.ALCANCE_DE_AUDICAO,
                getUUID().hashCode());
        return new EnemyRuntime(percepcao,
                new EnemyBrain(new AwarenessTuning(WolfRunnerTuning.TICKS_DE_AVISO,
                        WolfRunnerTuning.MEMORIA_DE_ALVO_TICKS)),
                new AttackController(ChimeraProfiles.wolfRunnerRecarga()),
                new StaggerState(ChimeraProfiles.wolfRunnerStagger()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = ChimeraProfiles.wolfRunner().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<WolfRunnerEntity> registeredType() { return EnemyEntityTypes.WOLF_RUNNER.get(); }

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

    /** O que o flanco decidiu; leitura para o debug de inimigos. */
    public DecisaoDeFlanco decisaoDeFlanco() { return decisao; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BoteGoal(this));
        goalSelector.addGoal(2, new MordidaGoal(this));
        goalSelector.addGoal(3, new ManobraGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // NENHUM LookAtPlayerGoal: ele encara o jogador MAIS PROXIMO sem consultar
        // nada, e num flanqueador isso desmente a propria manobra -- a cabeca
        // acompanharia quem esta na frente enquanto o corpo contorna, e o jogador
        // leria que foi visto quando nao foi.
        //
        // NENHUM target goal vanilla: quem escolhe alvo e o Squad, e antes dele o
        // TargetEvaluator por faccao. Um NearestAttackableTargetGoal ao lado seria
        // uma segunda autoridade sobre a mesma decisao, e ela decide por
        // INDIVIDUO: cada formiga voltaria a escolher a propria presa, e o cerco
        // nunca fecharia sem que nada acusasse.
    }

    // -------------------------------------------------------------------- tick

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;
        EnemyRuntime runtime = runtimeExigido();
        PerceptionSnapshot snapshot = runtime.tick(tickCount, this::varrerCandidatos,
                getHealth() <= getMaxHealth() * WolfRunnerTuning.FRACAO_DE_VIDA_CRITICA, false);

        atualizarRastro(snapshot);
        aplicarAlvo(runtime, snapshot);

        SquadRegistry registro = RegistroDeMatilhas.doNivel((ServerLevel) level());
        Squad esquadrao = registro.bandoDe(getUUID()).orElse(null);
        // A desfasagem sai do id do ESQUADRAO, e nao do proprio: membros do mesmo
        // grupo tem de decidir no MESMO tick, senao metade contorna a posicao
        // velha do alvo e o cerco nunca fecha. Sem esquadrao ainda, a do proprio
        // serve -- ela so espalha no tempo as tentativas de entrar num.
        int desfasagem = (esquadrao != null ? esquadrao.id() : getUUID()).hashCode();
        if (REGRAS_DO_ESQUADRAO.atualizaNesteTick(tickCount, desfasagem)) {
            coordenar(registro, esquadrao);
        }

        tickDoGolpe(runtime);
        publicarEstado(runtime);
    }

    // ------------------------------------------------------------------ rastro

    /**
     * Atualiza o rastro com o que a percepcao mediu neste tick.
     *
     * <p>O rastro e uma POSICAO, e nao uma entidade. Guardar a entidade seria uma
     * referencia viva presa num campo de mob: nao da erro, so impede o objeto de
     * morrer. E a posicao e o que faz a perseguicao continuar depois que o alvo
     * troca de dimensao ou sai do chunk -- o uuid deixa de resolver, e o ponto
     * ainda esta la.</p>
     */
    private void atualizarRastro(PerceptionSnapshot snapshot) {
        Optional<UUID> lembrado = snapshot.alvoOpcional();
        if (lembrado.isEmpty()) {
            esquecerRastro();
            return;
        }
        UUID id = lembrado.get();
        if (!id.equals(alvoDoRastro)) {
            // Alvo novo: o rastro do anterior nao serve, e reaproveita-lo mandaria
            // o bicho correr para onde OUTRA pessoa estava. Nao da erro -- da um
            // mob que persegue quem nunca o viu.
            alvoDoRastro = id;
            rastro = null;
            ticksDesdeOContato = 0;
        }
        alvoVisivel = snapshot.visivel();
        Entity encontrado = ((ServerLevel) level()).getEntity(id);
        if (alvoVisivel && encontrado != null && encontrado.isAlive()) {
            rastro = encontrado.position();
            ticksDesdeOContato = 0;
        } else {
            ticksDesdeOContato++;
        }
    }

    private void esquecerRastro() {
        alvoDoRastro = null;
        rastro = null;
        ticksDesdeOContato = 0;
        alvoVisivel = false;
    }

    /** O que o rastro manda fazer agora. Sem rastro, a resposta e DESISTIR. */
    private DecisaoDeRastro decisaoDeRastro() {
        if (alvoDoRastro == null || rastro == null) return DecisaoDeRastro.DESISTIR;
        return RASTRO.decidir(alvoVisivel, ticksDesdeOContato, position().distanceTo(rastro));
    }

    /**
     * Traduz o alvo LEMBRADO em {@code setTarget} -- e o prazo do rastro manda.
     *
     * <p><b>Por que a {@code ThreatMemory} e apagada junto.</b> Ela dura 140
     * ticks e o rastro dura 100: se apenas o alvo da entidade fosse limpo, a
     * memoria devolveria o mesmo uuid no tick seguinte e o bicho voltaria a
     * persegui-lo. O prazo do rastro viraria uma frase sem efeito, e a unica
     * pista seria um mob que "desiste" e reengaja sozinho -- sem erro nenhum, e
     * com dois prazos discordando em silencio sobre a mesma coisa.</p>
     */
    private void aplicarAlvo(EnemyRuntime runtime, PerceptionSnapshot snapshot) {
        Optional<UUID> lembrado = snapshot.alvoOpcional();
        if (lembrado.isEmpty()) {
            if (getTarget() != null) setTarget(null);
            return;
        }
        if (decisaoDeRastro() == DecisaoDeRastro.DESISTIR) {
            runtime.percepcao().limpar();
            esquecerRastro();
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

    // --------------------------------------------------------------- esquadrao

    /**
     * Um passo de coordenacao -- e o UNICO lugar que mexe no esquadrao.
     *
     * <p>Concentrado aqui de proposito. Espalhado pelas Goals, cada uma leria e
     * escreveria o grupo no proprio ritmo, e duas delas trocariam o alvo no mesmo
     * tick; a ultima venceria, e o sintoma e um bicho indeciso sem causa
     * visivel.</p>
     *
     * <p><b>O id do esquadrao NAO vai para a identidade persistida.</b>
     * {@code BaseChimeraAnt.alistarNoEsquadrao} existe e nao e chamado aqui de
     * proposito: o {@code SquadRegistry} e RUNTIME e morre com o processo, e a
     * identidade e PERSISTENTE (ADR-002). Gravar um id de runtime no save faria a
     * formiga voltar do restart dizendo pertencer a um esquadrao que nao existe
     * mais -- e nada acusaria isso, porque um uuid orfao e indistinguivel de um
     * uuid valido.</p>
     */
    private void coordenar(SquadRegistry registro, Squad esquadraoAtual) {
        Squad esquadrao = esquadraoAtual;
        // Um esquadrao de um so nao e esquadrao: ele tenta se juntar a um vizinho
        // antes de qualquer outra coisa. Sem essa fusao, quatro formigas que
        // nascem juntas criariam quatro grupos de um e TODAS recuariam para
        // sempre -- com o registro, o teto e a moral funcionando perfeitamente.
        if (esquadrao == null || esquadrao.tamanho() == 1) {
            esquadrao = procurarEsquadrao(registro, esquadrao);
        }
        // A faxina roda no MESMO ritmo da coordenacao, e nao "quando der". Grupo
        // sem ninguem nao consome tick e nao aparece em lugar nenhum; ele so ocupa
        // memoria e pode ressuscitar mais tarde com o alvo de uma hora atras.
        registro.removerDissolvidos();

        if (esquadrao == null) {
            decisao = DecisaoDeFlanco.AGUARDAR;
            return;
        }
        SquadRole papel = esquadrao.papelDe(getUUID());
        if (papel == null) {
            // O indice e o grupo discordaram. Sair e a saida segura: seguir com um
            // papel nulo faria toda decisao adiante estourar um NullPointerException
            // no meio do tick do servidor, com pilha que nao diz qual formiga era.
            registro.sair(getUUID());
            decisao = DecisaoDeFlanco.AGUARDAR;
            return;
        }

        publicarAlvoDoEsquadrao(esquadrao);
        LivingEntity alvo = resolver(esquadrao.alvo().orElse(null));
        boolean visivel = alvo != null && hasLineOfSight(alvo);
        boolean ferido = getHealth() <= getMaxHealth() * WolfRunnerTuning.FRACAO_DE_VIDA_CRITICA;
        // alvoRecuando e FALSO de proposito, e e decisao de ficha: este bicho nao
        // desiste porque a presa correu -- correr e exatamente o que ele caca, e
        // o rastro existe para isso. O unico gatilho de recuo coletivo e a moral,
        // e leituraPara ja a soma aqui dentro.
        SquadInput leitura = esquadrao.leituraPara(getUUID(), visivel, false, ferido);
        // O controller e montado a cada coordenacao, a partir do papel que o Squad
        // tem AGORA. Guarda-lo num campo faria dele uma segunda fonte para o
        // papel: depois de uma promocao o campo continuaria dizendo FLANKER
        // enquanto o grupo ja diz LEADER, e o bicho contornaria quando deveria
        // estar segurando a frente.
        SquadOrder ordem = new SquadController(papel).update(leitura);

        double distancia = alvo != null ? distanceTo(alvo) : 0.0D;
        double cosseno = alvo != null ? cossenoDoOlharDoAlvoAteMim(alvo) : 0.0D;
        decisao = FLANCO.decidir(ordem, esquadrao.tamanho(), distancia, cosseno);
        this.papelNoEsquadrao = ordem.role();
    }

    /** Papel do ultimo tick de coordenacao; e dele que sai o posto do contorno. */
    private SquadRole papelNoEsquadrao = SquadRole.FLANKER;

    /**
     * Publica o proprio alvo no grupo quando o grupo ainda nao tem um valido.
     *
     * <p>O alvo mora no {@link Squad} e nao em cada membro justamente para que
     * eles nao possam divergir -- e a divergencia nao daria erro, daria um cerco
     * que nunca fecha porque cada um esta contornando uma pessoa diferente.</p>
     */
    private void publicarAlvoDoEsquadrao(Squad esquadrao) {
        LivingEntity atual = resolver(esquadrao.alvo().orElse(null));
        if (atual != null && atual.isAlive()) return;
        LivingEntity meu = getTarget();
        esquadrao.alvo(meu != null && meu.isAlive() ? meu.getUUID() : null);
    }

    private LivingEntity resolver(UUID id) {
        if (id == null) return null;
        Entity encontrado = ((ServerLevel) level()).getEntity(id);
        return encontrado instanceof LivingEntity vivo && vivo.isAlive() ? vivo : null;
    }

    /**
     * Procura um esquadrao vizinho que aceite; se nenhum aceitar, funda o proprio.
     *
     * <p>A varredura de mundo acontece SO aqui, SO no orcamento de coordenacao e
     * SO enquanto a formiga esta sozinha. Uma formiga em grupo formado nunca
     * varre nada -- e e por isso que o teto tambem e um limite de CUSTO, e nao so
     * de tamanho.</p>
     */
    private Squad procurarEsquadrao(SquadRegistry registro, Squad meuGrupoDeUmSo) {
        AABB area = getBoundingBox().inflate(REGRAS_DO_ESQUADRAO.raioDeReforco());
        for (WolfRunnerEntity vizinho : level().getEntitiesOfClass(WolfRunnerEntity.class, area,
                outro -> outro != this && outro.isAlive())) {
            Squad candidato = registro.bandoDe(vizinho.getUUID()).orElse(null);
            if (candidato == null) continue;
            if (candidato.contem(getUUID())) continue;
            if (meuGrupoDeUmSo != null && candidato.id().equals(meuGrupoDeUmSo.id())) continue;
            if (candidato.tamanho() >= REGRAS_DO_ESQUADRAO.maximoDeMembros()) continue;
            if (candidato.moral() < REGRAS_DO_ESQUADRAO.moralMinima()) continue;

            // Sair ANTES de entrar, sempre: o registro recusa quem ainda consta em
            // outro grupo, e a recusa vem como excecao no meio do tick do servidor.
            registro.sair(getUUID());
            if (registro.entrar(candidato.id(), getUUID(), molde().papelNoSquad())) {
                return candidato;
            }
            // Recusado depois de ter saido: sem grupo neste tick. O proximo
            // orcamento funda um novo -- melhor do que forcar a entrada e estourar
            // o teto que acabou de ser cobrado.
            return null;
        }
        if (meuGrupoDeUmSo != null) return meuGrupoDeUmSo;
        return registro.criar(UUID.randomUUID(), REGRAS_DO_ESQUADRAO, getUUID());
    }

    // ------------------------------------------------------------------ golpe

    /**
     * A janela ACTIVE consulta a caixa da mordida; fora dela o golpe nao existe.
     *
     * <p>O impulso do bote e aplicado UMA vez por instancia de ataque, e nao a
     * cada tick da janela: somado cinco vezes, o bicho atravessaria o alvo e
     * sairia do outro lado, e o proprio {@code tryHit} -- que so acerta cada
     * alvo uma vez por instancia -- esconderia o defeito atras de um dano
     * correto.</p>
     */
    private void tickDoGolpe(EnemyRuntime runtime) {
        AttackController ataques = runtime.ataques();
        if (ataques.phase() != AttackPhase.ACTIVE) return;
        if (boteEmCurso && instanciaComImpulso != ataques.attackInstanceId()) {
            instanciaComImpulso = ataques.attackInstanceId();
            aplicarImpulsoDoBote();
        }
        LivingEntity alvo = getTarget();
        if (alvo == null || !alvo.isAlive()) return;

        // A regiao passada aqui e a COMUM: este e o golpe que ele DA, e ponto
        // fraco e coisa do golpe que ele RECEBE. Passar uma regiao vulneravel nao
        // daria erro nenhum -- daria o multiplicador aplicado no lado errado do
        // combate, e o dano final ficaria plausivel demais para alguem notar sem
        // medir.
        Optional<AttackHit> acerto = ataques.tryHit(alvo.getId(), alvo.getBoundingBox(),
                CAIXA_DA_MORDIDA, position(), getYRot(), WolfRunnerTuning.REGIAO_COMUM);
        float empurrao = boteEmCurso
                ? WolfRunnerTuning.EMPURRAO_DO_BOTE : WolfRunnerTuning.EMPURRAO_DA_MORDIDA;
        acerto.ifPresent(hit -> {
            alvo.hurt(damageSources().mobAttack(this), hit.damage());
            alvo.knockback(empurrao, getX() - alvo.getX(), getZ() - alvo.getZ());
        });
    }

    /**
     * O salto em si: UM impulso, na direcao horizontal do alvo, mais o vertical.
     *
     * <p>A direcao e horizontal de proposito. Apontar o impulso para o alvo em
     * tres dimensoes faria o bicho mergulhar quando o jogador estivesse abaixo, e
     * um quadrupede que cai de bico nao le como bote -- le como bug de fisica. A
     * subida vem so de {@code impulsoVertical}, e e ela que
     * {@link RegrasDeSalto} cobra contra o desnivel aceito.</p>
     */
    private void aplicarImpulsoDoBote() {
        LivingEntity alvo = getTarget();
        Vec3 direcao = alvo != null
                ? new Vec3(alvo.getX() - getX(), 0.0D, alvo.getZ() - getZ())
                : direcaoDoOlharNoPlano();
        if (direcao.lengthSqr() < 1.0E-6D) direcao = direcaoDoOlharNoPlano();
        direcao = direcao.normalize();
        setDeltaMovement(direcao.x * SALTO.impulsoHorizontal(), SALTO.impulsoVertical(),
                direcao.z * SALTO.impulsoHorizontal());
        // Sem isto o servidor nao manda o pacote de velocidade e o salto acontece
        // so do lado de ca: no cliente o bicho desliza ate a posicao nova, e a
        // unica coisa que o jogador tinha para ler -- o arco do pulo -- some.
        this.hasImpulse = true;
    }

    private Vec3 direcaoDoOlharNoPlano() {
        double radianos = Math.toRadians(getYRot());
        return new Vec3(-Math.sin(radianos), 0.0D, Math.cos(radianos));
    }

    // ------------------------------------------------------------- percepcao

    /** Varredura de mundo -- a UNICA, e so quando o orcamento autoriza. */
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

    /** 1 quando o alvo esta bem na frente DESTE bicho, -1 quando esta atras. */
    private double cossenoDoOlharAte(Entity alvo) {
        return cosseno(getLookAngle(), alvo.getX() - getX(), alvo.getZ() - getZ());
    }

    /**
     * 1 quando o ALVO esta olhando direto para este bicho, -1 quando de costas.
     *
     * <p>E o inverso de {@link #cossenoDoOlharAte}, e confundir os dois nao daria
     * erro: daria um flanqueador que mede o proprio olhar em vez do olhar do
     * jogador, e ele fecharia sempre que estivesse encarando o alvo -- ou seja,
     * atacaria sempre de frente, que e a definicao de nao flanquear.</p>
     */
    private double cossenoDoOlharDoAlvoAteMim(LivingEntity alvo) {
        return cosseno(alvo.getLookAngle(), getX() - alvo.getX(), getZ() - alvo.getZ());
    }

    private static double cosseno(Vec3 olhar, double dx, double dz) {
        Vec3 horizontal = new Vec3(olhar.x, 0.0D, olhar.z);
        Vec3 ate = new Vec3(dx, 0.0D, dz);
        if (horizontal.lengthSqr() < 1.0E-6D || ate.lengthSqr() < 1.0E-6D) return 0.0D;
        return Mth.clamp(horizontal.normalize().dot(ate.normalize()), -1.0D, 1.0D);
    }

    /**
     * Por qual lado este bicho contorna. Bit ESTAVEL do uuid, nunca sorteio.
     *
     * <p>Sorteado a cada tick, ele trocaria de lado no meio do contorno e andaria
     * em zigue-zague na frente do jogador. Nao da erro -- da um mob que parece
     * indeciso, e "indeciso" e a descricao que nunca leva ninguem a causa.</p>
     */
    private boolean contornaPelaDireita() { return (getUUID().hashCode() & 1) == 0; }

    /** O ponto do arco em que este bicho se posta, medido a partir do OLHAR do alvo. */
    private Vec3 postoDeContorno(LivingEntity alvo) {
        double angulo = FLANCO.anguloDePostoEmGraus(papelNoEsquadrao, contornaPelaDireita());
        double radianos = Math.toRadians(alvo.getYRot() + angulo);
        // yaw 0 aponta para +Z e yaw positivo gira para -X: a mesma convencao de
        // AttackHitbox.noMundo. Inverter o sinal aqui poria o posto do lado
        // oposto do alvo, e o contorno atravessaria a frente dele.
        return alvo.position().add(-Math.sin(radianos) * WolfRunnerTuning.RAIO_DO_CONTORNO,
                0.0D, Math.cos(radianos) * WolfRunnerTuning.RAIO_DO_CONTORNO);
    }

    // -------------------------------------------------------------- publicacao

    private void publicarEstado(EnemyRuntime runtime) {
        int fase = runtime.ataques().phase().ordinal();
        if (this.entityData.get(FASE_DE_ATAQUE) != fase) this.entityData.set(FASE_DE_ATAQUE, fase);
        boolean cambaleando = runtime.stagger().cambaleando();
        if (this.entityData.get(CAMBALEANDO) != cambaleando) {
            this.entityData.set(CAMBALEANDO, cambaleando);
        }
        if (combatState() == EnemyCombatState.DYING) return;
        combatState(estadoDeCombate(runtime, cambaleando));
    }

    private EnemyCombatState estadoDeCombate(EnemyRuntime runtime, boolean cambaleando) {
        if (cambaleando) return EnemyCombatState.STAGGERED;
        if (decisao == DecisaoDeFlanco.RECUAR
                || runtime.consciencia() == EnemyAwarenessState.FLEE) {
            return EnemyCombatState.RETREAT;
        }
        return switch (runtime.ataques().phase()) {
            case WINDUP -> EnemyCombatState.WINDUP;
            case ACTIVE -> EnemyCombatState.ACTIVE;
            case RECOVERY -> EnemyCombatState.RECOVERY;
            default -> getTarget() != null ? EnemyCombatState.AGGRO : EnemyCombatState.IDLE;
        };
    }

    /**
     * UNICO lugar que alimenta o stagger e abala a moral do grupo.
     *
     * <p>Dano sem atacante (fogo, queda, {@code /kill}) tambem cambaleia aqui, e
     * isso e deliberado: este mob nao tem ponto fraco por regiao, entao nao ha
     * geometria de impacto a resolver, e exigir atacante deixaria o bicho imune a
     * interrupcao por qualquer fonte ambiental sem que nada dissesse por que.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean levou = super.hurt(source, amount);
        if (!levou || level().isClientSide || !Float.isFinite(amount) || amount <= 0.0F) {
            return levou;
        }
        // O golpe que MATA ja passou por die(), que apagou o runtime e publicou o
        // repouso. Acumular stagger depois disso ligaria CAMBALEANDO num cadaver,
        // logo apos o proprio ciclo de vida te-lo desligado -- estado publicado
        // por cima da limpeza que acabou de acontecer, e sem erro nenhum.
        if (isDeadOrDying()) return true;

        EnemyRuntime runtime = runtimeExigido();
        StaggerResult resultado = runtime.sofrerStagger(runtime.ataques().attackInstanceId(),
                amount, WolfRunnerTuning.RECARGA_APOS_INTERRUPCAO);
        if (resultado == StaggerResult.DISPAROU) {
            getNavigation().stop();
            boteEmCurso = false;
            this.entityData.set(CAMBALEANDO, true);
            this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        }
        RegistroDeMatilhas.doNivel((ServerLevel) level()).bandoDe(getUUID())
                .ifPresent(grupo -> grupo.abalar(WolfRunnerTuning.ABALO_POR_GOLPE_NO_MEMBRO));
        return true;
    }

    // ---------------------------------------------------------- ciclo de vida

    /** Morrer PUBLICA o repouso: depois de morto o passo de IA nao roda mais. */
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) publicarRepouso();
        super.die(source);
    }

    /**
     * O UNICO ponto de saida do esquadrao.
     *
     * <p>Morte, unload de chunk, troca de dimensao e comando administrativo
     * passam todos por {@code remove}. Espalhar a saida por {@code die} mais um
     * evento de unload mais outro de dimensao e como um deles fica esquecido -- e
     * o esquecido nao da erro: deixa o grupo contando uma formiga que nao existe
     * mais, ocupando um lugar do teto para sempre, e o reforco que deveria chegar
     * e recusado num esquadrao de dois.</p>
     */
    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide && level() instanceof ServerLevel nivel) {
            publicarRepouso();
            esquecerRastro();
            SquadRegistry registro = RegistroDeMatilhas.doNivel(nivel);
            registro.sair(getUUID());
            registro.removerDissolvidos();
        }
        super.remove(reason);
    }

    private void publicarRepouso() {
        boteEmCurso = false;
        this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        this.entityData.set(CAMBALEANDO, false);
    }

    // ------------------------------------------------------------------ goals

    /**
     * O bote: o salto que fecha os ultimos metros, com telegrafo proprio.
     *
     * <p><b>Ele so sai enquanto a decisao e CONTORNAR</b>, e isso e a ficha: o
     * bote e a forma de CHEGAR, e nao o golpe de quem ja chegou. Autorizado
     * tambem em INVESTIR, ele viraria um segundo ataque corpo-a-corpo com aviso
     * mais longo, e o jogador nunca veria um salto de verdade.</p>
     *
     * <p><b>E ele exige o flanco.</b> Saltar de frente e a mesma coisa que
     * carregar; o que torna este mob legivel e o salto vir do lado de fora do
     * campo de visao, depois de o jogador ter escolhido olhar para outro.</p>
     *
     * <p>{@code static} de proposito: todo estado que ela le mora na ENTIDADE. Um
     * campo aqui seria compartilhado por todas as formigas do mundo, e o sintoma
     * seria uma saltando no alvo de outra.</p>
     */
    private static final class BoteGoal extends Goal {
        private final WolfRunnerEntity lobo;

        private BoteGoal(WolfRunnerEntity lobo) {
            this.lobo = lobo;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (lobo.level().isClientSide) return false;
            EnemyRuntime runtime = lobo.runtimeExigido();
            if (runtime.stagger().cambaleando()) return false;
            if (runtime.consciencia() == EnemyAwarenessState.FLEE) return false;
            if (lobo.decisao != DecisaoDeFlanco.CONTORNAR) return false;
            LivingEntity alvo = lobo.getTarget();
            if (alvo == null || !alvo.isAlive() || !lobo.hasLineOfSight(alvo)) return false;
            if (!FLANCO.foraDoArcoFrontal(lobo.cossenoDoOlharDoAlvoAteMim(alvo))) return false;
            return SALTO.decidir(lobo.distanceTo(alvo), alvo.getY() - lobo.getY(),
                    lobo.onGround(), runtime.ataques().canStart()) == DecisaoDeSalto.SALTAR;
        }

        @Override public boolean canContinueToUse() {
            EnemyRuntime runtime = lobo.runtimeExigido();
            // A CONTINUACAO NAO RECONSULTA O FLANCO, de proposito. Depois que o
            // bicho agachou, o salto acontece: cancelar no meio porque o jogador
            // virou faria o bicho desarmar em silencio um golpe que o jogador ja
            // viu comecar, e o telegrafo passaria a mentir. Quem vira a tempo
            // ganha o salto ERRANDO, e nao o salto desaparecendo.
            return runtime.ataques().phase() != AttackPhase.IDLE
                    && runtime.ataques().phase() != AttackPhase.COMPLETE
                    && !runtime.stagger().cambaleando();
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() {
            lobo.getNavigation().stop();
            lobo.boteEmCurso = true;
            lobo.runtimeExigido().ataques().start(WolfRunnerTuning.bote());
        }

        @Override public void tick() {
            EnemyRuntime runtime = lobo.runtimeExigido();
            LivingEntity alvo = lobo.getTarget();
            if (runtime.ataques().phase() != AttackPhase.WINDUP) return;
            if (alvo != null) lobo.getLookControl().setLookAt(alvo, 40.0F, 40.0F);
            // Ele PARA durante o agachamento, e so durante ele. Sem isto o bicho
            // chegaria ao fim do aviso ainda carregando a inercia do contorno, e o
            // salto sairia de um lugar diferente daquele onde foi telegrafado --
            // sem erro nenhum, e com o aviso apontando para o lugar errado. Na
            // janela ACTIVE a inercia e o proprio salto, e zera-la ali mataria o
            // impulso no mesmo tick em que ele foi aplicado.
            lobo.setDeltaMovement(lobo.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
        }

        @Override public void stop() {
            lobo.boteEmCurso = false;
            lobo.getNavigation().stop();
        }
    }

    /** A mordida curta, para quando o alvo ja esta colado e ja foi flanqueado. */
    private static final class MordidaGoal extends Goal {
        private final WolfRunnerEntity lobo;

        private MordidaGoal(WolfRunnerEntity lobo) {
            this.lobo = lobo;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (lobo.level().isClientSide) return false;
            EnemyRuntime runtime = lobo.runtimeExigido();
            if (runtime.stagger().cambaleando()) return false;
            if (runtime.consciencia() == EnemyAwarenessState.FLEE) return false;
            if (lobo.decisao != DecisaoDeFlanco.INVESTIR) return false;
            LivingEntity alvo = lobo.getTarget();
            if (alvo == null || !alvo.isAlive() || !lobo.hasLineOfSight(alvo)) return false;
            return lobo.distanceTo(alvo) <= WolfRunnerTuning.ALCANCE_DA_MORDIDA
                    && runtime.ataques().canStart();
        }

        @Override public boolean canContinueToUse() {
            EnemyRuntime runtime = lobo.runtimeExigido();
            return runtime.ataques().phase() != AttackPhase.IDLE
                    && runtime.ataques().phase() != AttackPhase.COMPLETE
                    && !runtime.stagger().cambaleando();
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() {
            lobo.getNavigation().stop();
            lobo.boteEmCurso = false;
            lobo.runtimeExigido().ataques().start(WolfRunnerTuning.mordida());
        }

        @Override public void tick() {
            LivingEntity alvo = lobo.getTarget();
            if (alvo != null) lobo.getLookControl().setLookAt(alvo, 40.0F, 40.0F);
            lobo.setDeltaMovement(lobo.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
        }

        @Override public void stop() { lobo.getNavigation().stop(); }
    }

    /**
     * A UNICA autoridade de navegacao fora dos golpes.
     *
     * <p><b>Por que uma Goal so, e nao uma por decisao.</b> Contornar, recuar e
     * seguir rastro sao tres destinos diferentes para a mesma perna. Em tres
     * Goals com {@code Flag.MOVE}, duas podem estar ativas em ticks alternados e
     * as duas chamam {@code moveTo} -- a ultima do tick vence, e o caminho e
     * recalculado do zero a cada troca. Isso nao levanta excecao: da um bicho que
     * treme entre dois destinos e um pathfinder rodando duas vezes por tick, por
     * formiga, numa colonia inteira.</p>
     */
    private static final class ManobraGoal extends Goal {
        private final WolfRunnerEntity lobo;

        private ManobraGoal(WolfRunnerEntity lobo) {
            this.lobo = lobo;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (lobo.level().isClientSide) return false;
            EnemyRuntime runtime = lobo.runtimeExigido();
            if (runtime.stagger().cambaleando()) return false;
            if (runtime.ataques().phase() != AttackPhase.IDLE
                    && runtime.ataques().phase() != AttackPhase.COMPLETE) {
                return false;
            }
            return lobo.getTarget() != null || lobo.rastro != null;
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void tick() {
            LivingEntity alvo = lobo.getTarget();
            if (alvo == null) {
                seguirRastro();
                return;
            }
            lobo.getLookControl().setLookAt(alvo, 40.0F, 40.0F);
            switch (lobo.decisao) {
                case RECUAR -> recuar(alvo);
                case CONTORNAR -> contornar(alvo);
                case INVESTIR -> lobo.getNavigation().moveTo(alvo,
                        WolfRunnerTuning.MULTIPLICADOR_DE_INVESTIDA);
                // AGUARDAR nao para: sem alvo utilizavel no grupo, o que ainda
                // vale e o rastro. Parar aqui faria a memoria de rastro existir
                // so no papel -- a decisao diria SEGUIR e a perna nao andaria.
                case AGUARDAR -> seguirRastro();
                default -> lobo.getNavigation().stop();
            }
        }

        private void contornar(LivingEntity alvo) {
            Vec3 posto = lobo.postoDeContorno(alvo);
            lobo.getNavigation().moveTo(posto.x, posto.y, posto.z,
                    WolfRunnerTuning.MULTIPLICADOR_DE_CONTORNO);
        }

        private void recuar(LivingEntity alvo) {
            Vec3 fuga = lobo.position().subtract(alvo.position());
            if (fuga.lengthSqr() < 1.0E-6D) fuga = lobo.direcaoDoOlharNoPlano();
            Vec3 destino = lobo.position()
                    .add(fuga.normalize().scale(WolfRunnerTuning.RAIO_DO_CONTORNO));
            lobo.getNavigation().moveTo(destino.x, destino.y, destino.z,
                    WolfRunnerTuning.MULTIPLICADOR_DE_RECUO);
        }

        private void seguirRastro() {
            if (lobo.decisaoDeRastro() != DecisaoDeRastro.SEGUIR_O_RASTRO) {
                lobo.getNavigation().stop();
                return;
            }
            Vec3 ponto = lobo.rastro;
            lobo.getNavigation().moveTo(ponto.x, ponto.y, ponto.z,
                    WolfRunnerTuning.MULTIPLICADOR_DE_INVESTIDA);
        }

        @Override public void stop() { lobo.getNavigation().stop(); }
    }

    // ------------------------------------------------------------- animacao

    @Override
    public void registerControllers(
            software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<WolfRunnerEntity>(
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

    // Contrato com wolf_runner.animation.json. LoopType.DEFAULT em todos: o tipo de
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
                .then("animation.wolf_runner." + nome,
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
