package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.ai.OrdemDoComandante;
import com.darkcontinent.nenfoundation.enemy.ai.PosturaDeComando;
import com.darkcontinent.nenfoundation.enemy.ai.RegistroDeMatilhas;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeComandoAereo;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeOrdemDeEsquadrao;
import com.darkcontinent.nenfoundation.enemy.ai.SituacaoDeComandoAereo;
import com.darkcontinent.nenfoundation.enemy.ai.SituacaoDeOrdem;
import com.darkcontinent.nenfoundation.enemy.ai.squad.Squad;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRegistry;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRole;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.base.BaseChimeraAnt;
import com.darkcontinent.nenfoundation.enemy.base.EnemyRuntime;
import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenIntent;
import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenSituation;
import com.darkcontinent.nenfoundation.enemy.combat.AttackController;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import com.darkcontinent.nenfoundation.enemy.content.AvianCommanderTuning;
import com.darkcontinent.nenfoundation.enemy.content.ChimeraProfiles;
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
 * Avian Commander -- formiga quimera, rank SQUADRON_LEADER: a que COMANDA de cima.
 *
 * <p><b>O alcance 36 -- o maior da familia inteira -- e o bicho.</b> Ela enxerga o
 * campo antes de qualquer um dos seus, e o que ela faz com isso e trocar o alvo do
 * esquadrao e mandar fechar. Traze-la para o chao apaga a unica coisa que a
 * distingue de um oficial forte, e e por isso que a altitude nao e detalhe visual
 * aqui: e {@link RegrasDeComandoAereo}, com a altura medida TODO tick.</p>
 *
 * <p><b>O ciclo tem quatro passos, e cada elo quebrado apaga uma parte diferente
 * do encontro.</b> Ela SOBE ate a altitude de comando; COMANDA de la, e so de la;
 * MERGULHA, porque o mergulho e o ataque; e RECOLHE -- sobe de novo, calada, por
 * {@code TICKS_DE_SUBIDA_APOS_MERGULHO}. Essa subida e a janela do jogador e o
 * unico preco do golpe. Sem ela a comandante desce, machuca e volta a comandar no
 * tick seguinte: nao da erro nenhum, e o que se perde e sobreviver ao mergulho
 * valer alguma coisa.</p>
 *
 * <p><b>Enquanto esta em baixo, ela NAO comanda -- e isso e o que o jogador
 * aprende a forcar.</b> A recusa tem motivo e nome:
 * {@link OrdemDoComandante#BAIXA_DEMAIS}. Ela vem ANTES da recusa de orcamento de
 * proposito, para que quem for diagnosticar "por que o esquadrao parou de se
 * reorganizar" leia a altitude e nao o relogio.</p>
 *
 * <p><b>A ordem sai no ORCAMENTO de {@link SquadRules}, nunca por tick.</b> Dez
 * ticks, com a desfasagem tirada do id do BANDO: assim os membros de um mesmo
 * bando decidem no mesmo tick e dois bandos nunca decidem juntos. O que roda TODO
 * tick e so a altitude e o relogio do golpe -- uma janela de seis ticks medida de
 * dez em dez acertaria por sorte.</p>
 *
 * <p><b>O molde dela NAO nasce LEADER, e ela vira lider aqui.</b> Lideranca e
 * promocao em {@link Squad}: ela funda o bando com {@code SquadRegistry.criar} e e
 * a lider daquele bando; os vizinhos entram com o papel do PROPRIO molde. Um molde
 * que ja nascesse lider daria dois lideres ao primeiro bando com duas dessas
 * formigas -- duas ordens no mesmo tick, e a ultima vence.</p>
 *
 * <p><b>Nen: ela DECIDE, e nao calcula.</b> {@code TacticalNenController} devolve
 * uma INTENCAO; o efeito que sai daqui e do INIMIGO -- postura e permissao de
 * mergulhar --, nunca aura, custo ou tecnica. O Nen Foundation e a unica
 * autoridade sobre Nen (CLAUDE.md), e uma comandante que gerisse a propria aura
 * seria a segunda. Enquanto o nucleo nao expuser a aura de uma formiga, a fracao
 * entra como {@link #AURA_NAO_MEDIDA_PELO_NUCLEO} e a decisao e sempre
 * {@code NENHUMA} -- inerte e DECLARADA, que e o oposto de inventada.</p>
 */
public final class AvianCommanderEntity extends BaseChimeraAnt
        implements software.bernie.geckolib.animatable.GeoEntity {

    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(AvianCommanderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CAMBALEANDO =
            SynchedEntityData.defineId(AvianCommanderEntity.class, EntityDataSerializers.BOOLEAN);
    /**
     * A postura, sincronizada.
     *
     * <p>O cliente precisa dela para escolher entre pairar e bater asa: sem isso o
     * unico sinal seria o deslocamento por tick, e uma comandante PARADA no posto
     * de comando cairia no clipe de ocio -- uma ave suspensa no ar sem bater as
     * asas. Isso nao da erro; da um bicho que parece congelado.</p>
     */
    private static final EntityDataAccessor<Integer> POSTURA =
            SynchedEntityData.defineId(AvianCommanderEntity.class, EntityDataSerializers.INT);

    private static final int MEMORIA_DE_ALVO_TICKS = 140;
    private static final int TICKS_DE_AVISO = 20;
    private static final double ABERTURA_DA_VISAO = 80.0D;
    private static final double ALCANCE_DE_AUDICAO = 16.0D;
    private static final float FRACAO_DE_VIDA_CRITICA = 0.25F;

    /**
     * As regras, montadas UMA vez.
     *
     * <p>Estaticas porque sao records imutaveis, sem estado de jogador. O erro que
     * este projeto ja sabe que comete -- estado de jogador num campo de classe --
     * seria guardar aqui o alvo, a postura ou o bando, e ai TODAS as comandantes do
     * servidor dividiriam um. Postura e mergulho moram na instancia; alvo e papel
     * moram no {@link Squad}.</p>
     */
    private static final RegrasDeComandoAereo REGRAS_DO_VOO = AvianCommanderTuning.comandoAereo();
    private static final RegrasDeOrdemDeEsquadrao REGRAS_DAS_ORDENS = AvianCommanderTuning.ordens();
    private static final SquadRules REGRAS_DO_BANDO = AvianCommanderTuning.bando();
    private static final AttackHitbox CAIXA_DO_MERGULHO = AvianCommanderTuning.caixaDoMergulho();

    /**
     * Quanto uma ordem de reagrupamento devolve de moral ao bando.
     *
     * <p><b>E aqui que "ela comanda" deixa de ser uma palavra.</b> O bando perde
     * moral a cada membro ferido; a comandante, do alto e no orcamento, devolve um
     * pedaco. Enquanto ela esta no ar, o esquadrao nao quebra; assim que ela e
     * trazida para baixo, ele volta a acumular abalo ate quebrar sozinho.</p>
     *
     * <p>Oito, e o numero e pequeno de proposito: grande, a moral nunca cairia, o
     * bando viraria imune a pressao e "derrube a comandante primeiro" deixaria de
     * ser uma escolha para virar a unica jogada. Nao e botao de balanceamento: e a
     * inclinacao da curva que o encontro inteiro ensina.</p>
     */
    private static final int MORAL_DEVOLVIDA_POR_REAGRUPAMENTO = 8;

    /** Quanto um golpe NELA abala o bando -- ela e o moral do esquadrao, e apanha por ele. */
    private static final int ABALO_POR_GOLPE_NA_COMANDANTE = 10;

    /**
     * Teto da sonda de altura, em blocos.
     *
     * <p>A sonda desce bloco a bloco procurando o primeiro solido. SEM teto, uma
     * comandante sobre um oceano ou sobre um penhasco varreria centenas de blocos
     * por tick -- e o sintoma nao seria erro, seria TPS caindo perto de um abismo.
     * O teto e maior que {@code ALTITUDE_DE_COMANDO} de proposito: acima dele,
     * "alta o bastante" ja e a resposta certa para toda pergunta que esta classe
     * faz.</p>
     */
    private static final int TETO_DA_SONDA_DE_ALTURA = 12;

    /**
     * A fracao de aura que o nucleo AINDA NAO entrega para uma formiga.
     *
     * <p>Zero, e zero e a unica escolha honesta. O Nen Foundation e a unica
     * autoridade sobre aura (CLAUDE.md); enquanto nao houver consulta por onde
     * pedir a aura desta formiga, qualquer outro valor aqui seria esta classe
     * INVENTANDO aura -- a segunda autoridade, que diverge da primeira sem dar
     * erro. Com zero, {@code TacticalNenController} devolve {@code NENHUMA} por
     * falta de reserva: o caminho fica ligado e inerte, e o dia em que o nucleo
     * expuser o numero muda UMA linha.</p>
     */
    private static final double AURA_NAO_MEDIDA_PELO_NUCLEO = 0.0D;

    /** Tolerancia para nao refazer a rota quando o posto mal se moveu, em blocos. */
    private static final double TOLERANCIA_DO_POSTO = 1.5D;

    /** Folga acima da altitude de comando: o posto fica um pouco mais alto que o minimo. */
    private static final double FOLGA_SOBRE_A_ALTITUDE = 2.0D;

    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache cacheDeAnimacao =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    /** A postura deste tick. Recalculada TODO tick: altitude muda rapido demais para o orcamento. */
    private PosturaDeComando postura = PosturaDeComando.SUBIR;
    /** Ela ja esta comprometida com um mergulho? Cortado por altura, por alvo perdido ou por stagger. */
    private boolean mergulhando;
    /** Ticks desde o FIM do ultimo mergulho. Satura, para nao estourar e prender o bicho em RECOLHER. */
    private int ticksDesdeOMergulho = AvianCommanderTuning.TICKS_DE_SUBIDA_APOS_MERGULHO;
    /** A ultima resposta de ordem, com o motivo da recusa -- leitura de SERVIDOR. */
    private OrdemDoComandante ultimaOrdem = OrdemDoComandante.SEM_BANDO;
    /** A ultima intencao de Nen. Ela nao ativa nada: so escolhe postura. */
    private TacticalNenIntent intencaoDeNen = TacticalNenIntent.NENHUMA;
    /**
     * O alvo do BANDO, por UUID e nunca por referencia.
     *
     * <p>Guardar a entidade impediria o objeto de morrer e sobreviveria ao unload
     * do chunk, deixando a comandante mergulhando sobre uma entidade que o mundo
     * nao tem mais.</p>
     */
    private UUID alvoCompartilhado;
    /** Para onde voar; recalculado todo tick, mas so vira rota quando muda de verdade. */
    private Vec3 destino;
    private boolean destinoPendente;
    /** A instancia de ataque que ja recebeu o impulso -- um impulso por mergulho, nunca seis. */
    private long ultimaInstanciaComImpulso;

    public AvianCommanderEntity(EntityType<? extends AvianCommanderEntity> type, Level level) {
        super(type, level, ChimeraProfiles.avianCommander().metadata(),
                new AwarenessTuning(TICKS_DE_AVISO, MEMORIA_DE_ALVO_TICKS),
                ChimeraProfiles.avianCommanderMolde());
        // Voo de verdade, e nao "pular alto": com o MoveControl de chao ela cairia
        // no tick seguinte a cada subida, e a altitude -- que e a regra inteira deste
        // mob -- viraria um numero que nunca passa de dois.
        this.moveControl = new FlyingMoveControl(this, 20, true);
        setPathfindingMalus(PathType.WATER, -1.0F);
        setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        instalarRuntime(montarRuntime());
    }

    private EnemyRuntime montarRuntime() {
        var perfil = ChimeraProfiles.avianCommander();
        PerceptionController percepcao = new PerceptionController(
                PerceptionBudget.padrao(),
                VisionCone.deGraus(perfil.attributes().followRange(), ABERTURA_DA_VISAO),
                new ThreatMemory(MEMORIA_DE_ALVO_TICKS),
                new TargetEvaluator(FactionRelations.padrao(), perfil.metadata().faction(),
                        perfil.attributes().followRange(), perfil.metadata().territorial()),
                // A porta de aura da formiga continua INERTE enquanto a #126 (Gyo)
                // estiver aberta no nucleo: inventar aqui a regra de quem-ve-o-que
                // seria a segunda autoridade sobre Nen que o CLAUDE.md proibe.
                PercepcaoDeAura.NENHUMA, ALCANCE_DE_AUDICAO, getUUID().hashCode());
        return new EnemyRuntime(percepcao,
                new EnemyBrain(new AwarenessTuning(TICKS_DE_AVISO, MEMORIA_DE_ALVO_TICKS)),
                new AttackController(ChimeraProfiles.avianCommanderRecarga()),
                new StaggerState(ChimeraProfiles.avianCommanderStagger()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = ChimeraProfiles.avianCommander().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance())
                // FLYING_SPEED existe porque o FlyingMoveControl le ELE, e nao
                // MOVEMENT_SPEED. Sem este atributo o mob voa no default de 0.4 e a
                // velocidade da ficha (0.32) vira decoracao -- sem erro nenhum.
                .add(Attributes.FLYING_SPEED, attrs.movementSpeed());
    }

    public static EntityType<AvianCommanderEntity> registeredType() { return EnemyEntityTypes.AVIAN_COMMANDER.get(); }

    /**
     * Navegacao de VOO.
     *
     * <p>Com a navegacao de chao, todo destino no ar vira "caminho impossivel" e a
     * comandante fica parada procurando rota -- sem erro, sem log, e com a altitude
     * congelada em zero. Nenhum portao do repositorio ve isso: o unico sinal seria
     * alguem abrir o jogo e encontrar uma ave andando.</p>
     */
    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navegacao = new FlyingPathNavigation(this, level);
        navegacao.setCanOpenDoors(false);
        navegacao.setCanFloat(false);
        navegacao.setCanPassDoors(true);
        return navegacao;
    }

    /**
     * Quem voa nao se machuca ao encostar no chao.
     *
     * <p>Sem isto, o mergulho -- que termina raspando o solo -- cobraria dano de
     * queda DELA, e a comandante se mataria depois de alguns golpes bem dados. O
     * relato que chegaria seria "ela as vezes morre sozinha".</p>
     */
    @Override
    public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fonte) {
        return false;
    }

    @Override
    protected void checkFallDamage(double queda, boolean noChao, BlockState estado, BlockPos posicao) {
        // Nada: a distancia de queda nem chega a ser acumulada. Acumular e depois
        // ignorar deixaria o contador crescendo entre mergulhos, e bastaria alguem
        // reintroduzir o dano de queda para ela morrer no primeiro golpe.
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        builder.define(CAMBALEANDO, false);
        builder.define(POSTURA, PosturaDeComando.SUBIR.ordinal());
    }

    public AttackPhase faseDeAtaque() {
        int ordinal = this.entityData.get(FASE_DE_ATAQUE);
        AttackPhase[] fases = AttackPhase.values();
        return ordinal >= 0 && ordinal < fases.length ? fases[ordinal] : AttackPhase.IDLE;
    }

    public boolean cambaleando() { return this.entityData.get(CAMBALEANDO); }

    /** A postura publicada -- serve nos DOIS lados, e por isso le do campo sincronizado. */
    public PosturaDeComando postura() {
        int ordinal = this.entityData.get(POSTURA);
        PosturaDeComando[] posturas = PosturaDeComando.values();
        return ordinal >= 0 && ordinal < posturas.length ? posturas[ordinal] : PosturaDeComando.SUBIR;
    }

    /** A ultima resposta de ordem, com o motivo da recusa. Leitura de servidor. */
    public OrdemDoComandante ultimaOrdem() { return ultimaOrdem; }

    /** A ultima intencao de Nen. Ela NAO ativa tecnica: so escolhe postura. */
    public TacticalNenIntent intencaoDeNen() { return intencaoDeNen; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MergulhoGoal(this));
        goalSelector.addGoal(3, new VooDeComandoGoal(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // NAO ha WaterAvoidingRandomStrollGoal: ela nao passeia no chao. Uma Goal de
        // passeio terrestre num mob voador disputa o mesmo flag MOVE com o voo e
        // ganha sempre que o voo esta sem destino -- o resultado e uma comandante
        // que desce sozinha para caminhar, e a altitude, que e o bicho inteiro, vira
        // um numero que oscila sem causa visivel.
        //
        // Nenhum target goal vanilla: quem escolhe alvo e o TargetEvaluator, por
        // faccao, e depois dele o Squad. Um NearestAttackableTargetGoal ao lado seria
        // uma segunda autoridade sobre a mesma decisao -- e neste mob seria a pior
        // possivel, porque ele decide por INDIVIDUO: a comandante voltaria a
        // perseguir o proprio alvo e o esquadrao nunca receberia o dela.
    }

    // ------------------------------------------------------------------ tick

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;
        EnemyRuntime runtime = runtimeExigido();
        PerceptionSnapshot snapshot = runtime.tick(tickCount, this::varrerCandidatos,
                getHealth() <= getMaxHealth() * FRACAO_DE_VIDA_CRITICA, false);

        SquadRegistry registro = RegistroDeMatilhas.doNivel((ServerLevel) level());
        Squad bando = registro.bandoDe(getUUID()).orElse(null);
        // A desfasagem sai do id do BANDO, e nao do proprio: membros do mesmo bando
        // decidem no MESMO tick (senao metade cerca a posicao velha do alvo) e dois
        // bandos nunca decidem juntos (senao o custo vira um pico periodico, que se
        // parece com problema de rede e e pior de diagnosticar).
        int desfasagem = (bando != null ? bando.id() : getUUID()).hashCode();

        // A ALTITUDE E A INTENCAO DE NEN RODAM TODO TICK; a ordem so no orcamento.
        // Invertido, ela mediria a propria altura de dez em dez ticks -- e o mergulho
        // inteiro cabe dentro de dez ticks.
        intencaoDeNen = decidirIntencaoDeNen(bando);
        atualizarPostura(runtime);

        if (REGRAS_DAS_ORDENS.noOrcamento(tickCount, desfasagem)) {
            bando = coordenar(registro, bando, snapshot);
        }
        if (bando != null) alvoCompartilhado = bando.alvo().orElse(null);

        atualizarDestino(resolver(alvoCompartilhado), bando);
        aplicarAlvo();
        tickDoGolpe(runtime);
        publicarEstado(runtime);
    }

    // -------------------------------------------------------------- altitude

    /**
     * A altura ATE O CHAO, medida com teto.
     *
     * <p>Ate o chao, e nao ate o alvo, porque chao e o que o jogador manipula: ele
     * a empurra para baixo, a prende sob um teto, a faz descer atras de alguem.
     * Medida ate o alvo, a mesma comandante estaria "alta" sobre um jogador num
     * buraco e "baixa" sobre um jogador numa torre -- e a tatica que o encontro
     * ensina pararia de funcionar exatamente onde o jogador construiu.</p>
     */
    private double alturaSobreOChao() {
        int pe = Mth.floor(getY());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(getBlockX(), pe, getBlockZ());
        for (int descida = 0; descida <= TETO_DA_SONDA_DE_ALTURA; descida++) {
            cursor.setY(pe - descida);
            if (level().isOutsideBuildHeight(cursor)) break;
            if (!level().getBlockState(cursor).getCollisionShape(level(), cursor).isEmpty()) {
                return Math.max(0.0D, getY() - (cursor.getY() + 1));
            }
        }
        return TETO_DA_SONDA_DE_ALTURA;
    }

    /**
     * Um passo de voo -- e o UNICO lugar que escreve postura e mergulho.
     *
     * <p>Concentrado aqui de proposito. Espalhado pelas Goals, cada uma leria e
     * escreveria o estado no proprio ritmo, e duas delas mudariam a postura no
     * mesmo tick; a ultima venceria, e o sintoma e um bicho indeciso sem causa
     * visivel.</p>
     */
    private void atualizarPostura(EnemyRuntime runtime) {
        double altura = alturaSobreOChao();
        LivingEntity alvo = resolver(alvoCompartilhado);
        boolean temAlvo = alvo != null;
        boolean cambaleando = runtime.stagger().cambaleando();

        // O mergulho termina por TRES motivos, e os tres moram aqui juntos.
        // Espalhados, um deles fica esquecido -- e o esquecido deixa a comandante
        // com o mergulho "em curso" para sempre: ela nunca zera o relogio da subida,
        // nunca volta a COMANDAR, e o esquadrao fica sem ordem ate o restart.
        if (mergulhando && (cambaleando || !temAlvo || REGRAS_DO_VOO.mergulhoTerminou(altura))) {
            mergulhando = false;
            ticksDesdeOMergulho = 0;
        }

        double distancia = temAlvo ? distanciaHorizontal(alvo) : 0.0D;
        PosturaDeComando decidida = REGRAS_DO_VOO.decidir(new SituacaoDeComandoAereo(
                altura, distancia, temAlvo, mergulhando, ticksDesdeOMergulho, cambaleando));

        // O Nen entra AQUI, e so como VETO de postura. Ele nao cria postura nova, nao
        // toca em aura e nao ativa tecnica -- quem faz isso e o Nen Foundation. Uma
        // formiga que decidiu sumir (Zetsu) ou se fechar (Ken) nao mergulha, e a
        // conversao dessa intencao em comportamento do INIMIGO e toda a ligacao que
        // este arquivo tem com Nen.
        if (decidida == PosturaDeComando.MERGULHAR
                && !AvianCommanderTuning.mergulhoPermitidoPor(intencaoDeNen)) {
            decidida = PosturaDeComando.COMANDAR;
        }
        postura = decidida;

        if (postura == PosturaDeComando.MERGULHAR) {
            mergulhando = true;
            ticksDesdeOMergulho = 0;
        } else {
            // Satura no proprio limite: um contador que cresce para sempre vira
            // negativo depois de algumas horas de mundo aberto, e negativo compara
            // MENOR que a janela -- a comandante entraria em RECOLHER e nunca mais
            // sairia. Nao daria erro: daria um chefe que parou de comandar.
            ticksDesdeOMergulho = Math.min(ticksDesdeOMergulho + 1,
                    AvianCommanderTuning.TICKS_DE_SUBIDA_APOS_MERGULHO);
        }
    }

    // ----------------------------------------------------------- coordenacao

    /**
     * Um passo de comando -- e o UNICO lugar que mexe no bando.
     *
     * <p>Ele funda o bando se nao houver, recruta quem cabe, pergunta a
     * {@link RegrasDeOrdemDeEsquadrao} o que sai, e aplica. A escrita e concentrada
     * de proposito: com duas Goals escrevendo o alvo do bando, a ultima do tick
     * venceria e o cerco oscilaria entre dois alvos sem nada acusar.</p>
     */
    private Squad coordenar(SquadRegistry registro, Squad bandoAtual, PerceptionSnapshot snapshot) {
        Squad bando = bandoAtual;
        if (bando == null) {
            // Ela FUNDA o bando e e a lider dele. O molde nao nasce LEADER porque
            // lideranca e promocao; aqui a promocao acontece na criacao, que e o
            // unico ponto em que Squad aceita um lider declarado.
            bando = registro.criar(UUID.randomUUID(), REGRAS_DO_BANDO, getUUID());
            alistarNoEsquadrao(bando.id());
        }
        // A faxina roda no MESMO ritmo da coordenacao, e nao "quando der": bando sem
        // ninguem nao consome tick e nao aparece em lugar nenhum -- ele so ocupa
        // memoria e pode receber um membro novo mais tarde, ressuscitando com o alvo
        // de um combate que acabou ha uma hora.
        registro.removerDissolvidos();
        if (registro.bandoDe(getUUID()).isEmpty()) {
            // A faxina levou o bando. Seguir com a referencia velha escreveria num
            // objeto que o registro ja esqueceu, e a ordem sumiria sem nenhum erro.
            ultimaOrdem = OrdemDoComandante.SEM_BANDO;
            return null;
        }

        recrutar(registro, bando);

        SquadRole papel = bando.papelDe(getUUID());
        if (papel == null) {
            // O indice e o bando discordaram. Sair e a saida segura: seguir com um
            // papel nulo estouraria NullPointerException no meio do tick do servidor,
            // com pilha que nao diz qual comandante era.
            registro.sair(getUUID());
            ultimaOrdem = OrdemDoComandante.SEM_BANDO;
            return null;
        }

        Optional<UUID> meuAlvo = snapshot.alvoOpcional();
        UUID alvoDoBando = bando.alvo().orElse(null);
        boolean alvoDoBandoValido = resolver(alvoDoBando) != null;
        boolean inedito = meuAlvo.isPresent()
                && (!alvoDoBandoValido || !meuAlvo.get().equals(alvoDoBando));

        ultimaOrdem = REGRAS_DAS_ORDENS.decidir(new SituacaoDeOrdem(
                papel == SquadRole.LEADER, true, true, postura, inedito,
                bandoEspalhado(bando), bando.tamanho()));

        switch (ultimaOrdem) {
            case TROCAR_ALVO -> bando.alvo(meuAlvo.orElse(null));
            // Reagrupar DEVOLVE moral, e e assim que "ela comanda" vira mecanica. O
            // efeito passa pela API publica do Squad de proposito: um campo novo la
            // seria uma segunda verdade sobre a coesao do bando, e os dois numeros
            // divergiriam na primeira baixa.
            case REAGRUPAR -> bando.moral(bando.moral() + MORAL_DEVOLVIDA_POR_REAGRUPAMENTO);
            default -> { }
        }
        if (!alvoDoBandoValido && ultimaOrdem != OrdemDoComandante.TROCAR_ALVO
                && alvoDoBando != null && papel == SquadRole.LEADER) {
            // So a LIDER apaga um alvo que deixou de existir. Com qualquer membro
            // podendo apagar, um que estivesse de costas limparia, no mesmo tick, o
            // alvo que outro acabou de publicar.
            bando.alvo(null);
        }
        return bando;
    }

    /**
     * Recruta vizinhas que ainda nao tem bando.
     *
     * <p>A varredura de mundo acontece SO aqui, SO no orcamento e SO enquanto o
     * bando nao esta cheio. Um teto que nao poupa varredura e um teto que limita o
     * tamanho e nao o CUSTO -- e o custo e o que derruba TPS numa colonia.</p>
     *
     * <p>Quem entra entra com o papel do PROPRIO molde, nunca com um papel
     * escolhido aqui: o papel decide o posto de cada familia no cerco, e decidi-lo
     * de fora faria a comandante transformar um batedor em frontliner sem que nada
     * reclamasse.</p>
     */
    private void recrutar(SquadRegistry registro, Squad bando) {
        if (bando.tamanho() >= REGRAS_DO_BANDO.maximoDeMembros()) return;
        AABB area = getBoundingBox().inflate(REGRAS_DO_BANDO.raioDeReforco());
        for (BaseChimeraAnt vizinha : level().getEntitiesOfClass(BaseChimeraAnt.class, area,
                outra -> outra != this && outra.isAlive())) {
            if (bando.tamanho() >= REGRAS_DO_BANDO.maximoDeMembros()) return;
            if (registro.bandoDe(vizinha.getUUID()).isPresent()) continue;
            SquadRole papel = vizinha.molde().papelNoSquad();
            if (papel == SquadRole.LEADER) {
                // Recusa com motivo: Squad.entrar levanta excecao para LEADER, e um
                // molde assim NAO deveria existir (ChimeraDefinition ja o proibe). A
                // guarda esta aqui para que, se um dia existir, o tick do servidor nao
                // morra no meio da coordenacao por causa de um dado de conteudo.
                continue;
            }
            if (registro.entrar(bando.id(), vizinha.getUUID(), papel)) {
                vizinha.alistarNoEsquadrao(bando.id());
            }
        }
    }

    /** Ha membro alem do raio de reforco? E isso que "espalhado" quer dizer. */
    private boolean bandoEspalhado(Squad bando) {
        double limite = REGRAS_DO_BANDO.raioDeReforco() * REGRAS_DO_BANDO.raioDeReforco();
        for (UUID id : bando.ids()) {
            if (id.equals(getUUID())) continue;
            LivingEntity membro = resolver(id);
            // Membro que o nivel nao acha NAO conta como espalhado: ele pode estar em
            // chunk descarregado, e tratar ausencia como distancia faria a comandante
            // mandar reagrupar para sempre por causa de quem nem esta carregado.
            if (membro != null && membro.distanceToSqr(this) > limite) return true;
        }
        return false;
    }

    // -------------------------------------------------------------------- Nen

    /**
     * A intencao de Nen deste tick -- decisao, nunca ativacao.
     *
     * <p>Ela devolve uma {@link TacticalNenIntent} e para por ai. O Nen Foundation
     * e a unica autoridade sobre Nen: esta classe nao calcula aura, nao aplica
     * custo e nao ativa tecnica. O unico efeito que sai daqui e do INIMIGO --
     * postura e permissao de mergulhar.</p>
     */
    private TacticalNenIntent decidirIntencaoDeNen(Squad bando) {
        LivingEntity alvo = getTarget();
        boolean emCombate = alvo != null;
        boolean aoAlcance = emCombate
                && distanciaHorizontal(alvo) <= AvianCommanderTuning.ALCANCE_DAS_GARRAS;
        return nenTatico().map(controlador -> controlador.decidir(new TacticalNenSituation(
                AURA_NAO_MEDIDA_PELO_NUCLEO,
                Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F),
                emCombate, aoAlcance,
                // alvoEscondido e FALSO enquanto a porta de aura estiver inerte: dizer
                // "escondido" sem ter como medir faria a formiga pedir Gyo o tempo todo.
                false,
                bando != null && bando.desmoralizado(),
                // Ela SEMPRE tem para onde recuar: o ceu. Dizer o contrario faria a
                // regra de fuga do controlador nunca disparar num bicho que voa.
                true))).orElse(TacticalNenIntent.NENHUMA);
    }

    // --------------------------------------------------------------- destino

    private void atualizarDestino(LivingEntity alvo, Squad bando) {
        Vec3 novo = switch (postura) {
            case MERGULHAR -> alvo == null ? null : alvo.position();
            case SUBIR, RECOLHER -> postoNoAlto(position());
            case COMANDAR -> postoNoAlto(centroDoBando(bando, alvo));
        };
        if (novo == null) {
            destino = null;
            destinoPendente = false;
            return;
        }
        double tolerancia = TOLERANCIA_DO_POSTO * TOLERANCIA_DO_POSTO;
        if (destino == null || destino.distanceToSqr(novo) > tolerancia) {
            destino = novo;
            destinoPendente = true;
        }
    }

    /** O posto fica acima do PONTO dado, na altitude de comando mais a folga. */
    private Vec3 postoNoAlto(Vec3 base) {
        double subir = AvianCommanderTuning.ALTITUDE_DE_COMANDO + FOLGA_SOBRE_A_ALTITUDE
                - alturaSobreOChao();
        return new Vec3(base.x, getY() + Math.max(0.0D, subir), base.z);
    }

    /**
     * O centro do bando -- e o posto de comando fica sobre ele.
     *
     * <p>Sobre o BANDO e nao sobre o alvo: sobre o alvo, ela estaria sempre na
     * vertical de quem ela manda atacar, e "comandar" viraria "pairar em cima do
     * jogador" -- o mob voador chato que esta classe existe para nao ser. Sem
     * membro carregado, o alvo serve de referencia; sem os dois, ela fica onde
     * esta.</p>
     */
    private Vec3 centroDoBando(Squad bando, LivingEntity alvo) {
        if (bando == null) return alvo != null ? alvo.position() : position();
        double x = 0.0D;
        double z = 0.0D;
        int contados = 0;
        for (UUID id : bando.ids()) {
            if (id.equals(getUUID())) continue;
            LivingEntity membro = resolver(id);
            if (membro == null) continue;
            x += membro.getX();
            z += membro.getZ();
            contados++;
        }
        if (contados == 0) return alvo != null ? alvo.position() : position();
        return new Vec3(x / contados, getY(), z / contados);
    }

    // ------------------------------------------------------------------ alvo

    /** O alvo da ENTIDADE e sempre o do bando -- nunca um que so ela viu e nao publicou. */
    private void aplicarAlvo() {
        LivingEntity alvo = resolver(alvoCompartilhado);
        if (alvo == null) {
            if (getTarget() != null) setTarget(null);
            return;
        }
        if (getTarget() != alvo) setTarget(alvo);
    }

    /** UUID -> entidade viva DESTE nivel, ou nulo. A referencia nunca e guardada. */
    private LivingEntity resolver(UUID id) {
        if (id == null || level().isClientSide) return null;
        Entity achado = ((ServerLevel) level()).getEntity(id);
        return achado instanceof LivingEntity vivo && vivo.isAlive() ? vivo : null;
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
     * Distancia HORIZONTAL, e nao {@code distanceTo}.
     *
     * <p>A regra de mergulho compara a distancia com um alcance que o jogador le no
     * plano. Medida em tres dimensoes, a altitude entraria na conta: quanto mais
     * alto ela estivesse, mais longe o alvo pareceria -- e uma comandante no posto
     * de comando nunca se comprometeria com o mergulho de quem esta exatamente
     * embaixo dela. O sintoma seria "ela so ataca de longe", sem nenhum erro.</p>
     */
    private double distanciaHorizontal(Entity alvo) {
        double dx = alvo.getX() - getX();
        double dz = alvo.getZ() - getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    // ---------------------------------------------------------------- golpe

    /**
     * A janela ACTIVE consulta a caixa do mergulho; fora dela o golpe nao existe.
     *
     * <p>Roda TODO tick, e nao no orcamento de coordenacao: seis ticks de janela
     * medidos de dez em dez ticks acertariam ou errariam por sorte.</p>
     */
    private void tickDoGolpe(EnemyRuntime runtime) {
        AttackController ataques = runtime.ataques();
        if (ataques.phase() != AttackPhase.ACTIVE) return;
        aplicarImpulso(ataques.attackInstanceId());

        LivingEntity alvo = getTarget();
        if (alvo == null || !alvo.isAlive()) return;
        ataques.tryHit(alvo.getId(), alvo.getBoundingBox(), CAIXA_DO_MERGULHO,
                        position(), getYRot(), "body")
                .ifPresent(hit -> {
                    alvo.hurt(damageSources().mobAttack(this), hit.damage());
                    alvo.knockback(AvianCommanderTuning.EMPURRAO_DO_MERGULHO,
                            getX() - alvo.getX(), getZ() - alvo.getZ());
                });
    }

    /**
     * O impulso que paga a diferenca entre a garra desenhada e a caixa do mergulho.
     *
     * <p>UMA vez por instancia de ataque. Aplicado nos seis ticks da janela, ela
     * atravessaria o alvo e sairia do outro lado -- e {@code tryHit}, que so acerta
     * cada alvo uma vez por instancia, esconderia o defeito atras de um dano
     * perfeitamente correto.</p>
     */
    private void aplicarImpulso(long instancia) {
        if (instancia == ultimaInstanciaComImpulso) return;
        ultimaInstanciaComImpulso = instancia;
        Vec3 olhar = getLookAngle();
        Vec3 frente = new Vec3(olhar.x, 0.0D, olhar.z);
        if (frente.lengthSqr() < 1.0E-6D) return;
        frente = frente.normalize().scale(AvianCommanderTuning.IMPULSO_DO_MERGULHO);
        setDeltaMovement(getDeltaMovement().add(frente.x, 0.0D, frente.z));
        this.hasImpulse = true;
    }

    /**
     * Apanhar interrompe o mergulho E abala o esquadrao.
     *
     * <p>Ela e o moral do bando, e apanha por ele: um golpe nela custa mais moral
     * que um golpe num membro. E a outra metade da conta que
     * {@link #MORAL_DEVOLVIDA_POR_REAGRUPAMENTO} escreve -- enquanto ela comanda o
     * bando se recompoe; enquanto ela apanha, ele anda para a quebra.</p>
     *
     * <p>Dano sem atacante -- fogo, queda -- nao cambaleia e nao abala: cambalear
     * por queimadura transformaria fogo numa interrupcao permanente, e derrubar uma
     * comandante com lava nao ensina nada sobre comando.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean levou = super.hurt(source, amount);
        if (!levou || level().isClientSide) return levou;
        if (!(source.getEntity() instanceof LivingEntity)) return true;

        EnemyRuntime runtime = runtimeExigido();
        StaggerResult resultado = runtime.sofrerStagger(runtime.ataques().attackInstanceId(),
                amount, AvianCommanderTuning.RECARGA_APOS_INTERRUPCAO);
        if (resultado == StaggerResult.DISPAROU) {
            getNavigation().stop();
            this.entityData.set(CAMBALEANDO, true);
            this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        }
        RegistroDeMatilhas.doNivel((ServerLevel) level()).bandoDe(getUUID())
                .ifPresent(bando -> bando.abalar(ABALO_POR_GOLPE_NA_COMANDANTE));
        return true;
    }

    private void publicarEstado(EnemyRuntime runtime) {
        int fase = runtime.ataques().phase().ordinal();
        if (this.entityData.get(FASE_DE_ATAQUE) != fase) this.entityData.set(FASE_DE_ATAQUE, fase);
        boolean cambaleando = runtime.stagger().cambaleando();
        if (this.entityData.get(CAMBALEANDO) != cambaleando) {
            this.entityData.set(CAMBALEANDO, cambaleando);
        }
        if (this.entityData.get(POSTURA) != postura.ordinal()) {
            this.entityData.set(POSTURA, postura.ordinal());
        }
        if (combatState() != EnemyCombatState.DYING) {
            combatState(cambaleando ? EnemyCombatState.STAGGERED
                    : postura == PosturaDeComando.RECOLHER ? EnemyCombatState.RETREAT
                    : getTarget() != null ? EnemyCombatState.AGGRO : EnemyCombatState.IDLE);
        }
    }

    // ---------------------------------------------------------- ciclo de vida

    /** Morrer PUBLICA o repouso: depois de morto o passo de IA nao roda mais. */
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) publicarRepouso();
        super.die(source);
    }

    /**
     * O UNICO ponto de saida do bando.
     *
     * <p>Morte, unload de chunk, troca de dimensao e comando administrativo passam
     * todos por {@code remove}. Espalhar a saida por {@code die} mais um evento de
     * unload e como um deles fica esquecido -- e o esquecido nao da erro: deixa o
     * bando com uma lider que nao existe mais, e a promocao que deveria acontecer
     * no mesmo tick nunca acontece. O esquadrao fica sem ninguem no comando e
     * continua funcionando, calado, para sempre.</p>
     */
    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide && level() instanceof ServerLevel nivel) {
            publicarRepouso();
            SquadRegistry registro = RegistroDeMatilhas.doNivel(nivel);
            registro.sair(getUUID());
            registro.removerDissolvidos();
        }
        super.remove(reason);
    }

    private void publicarRepouso() {
        this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        this.entityData.set(CAMBALEANDO, false);
        this.entityData.set(POSTURA, PosturaDeComando.SUBIR.ordinal());
    }

    // ------------------------------------------------------------- animacao

    @Override
    public void registerControllers(
            software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<AvianCommanderEntity>(
                this, "corpo", 4, estado -> {
                    if (this.deathTime > 0) return estado.setAndContinue(DEATH);
                    if (cambaleando()) return estado.setAndContinue(STAGGER);
                    return switch (faseDeAtaque()) {
                        case WINDUP -> estado.setAndContinue(WINDUP);
                        case ACTIVE -> estado.setAndContinue(STRIKE);
                        case RECOVERY -> estado.setAndContinue(RECOVERY);
                        // Fora do golpe quem manda no clipe e a POSTURA, e nao so a
                        // velocidade: uma comandante parada no posto de comando ainda
                        // esta batendo asa, e o clipe de ocio num bicho suspenso no ar
                        // le como travamento -- nunca como repouso.
                        default -> estado.setAndContinue(
                                postura() == PosturaDeComando.COMANDAR
                                        && velocidadeHorizontal() < LIMIAR_DE_CAMINHADA
                                        ? IDLE : WALK);
                    };
                }));
    }

    // Contrato com avian_commander.animation.json. LoopType.DEFAULT em todos: o tipo de
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
                .then("animation.avian_commander." + nome,
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

    // ------------------------------------------------------------------ goals

    /**
     * Leva a comandante ao posto que a postura escolheu -- subir, comandar ou
     * recolher.
     *
     * <p>Ela nao DECIDE nada: le {@code postura} e {@code destino}, que
     * {@code atualizarPostura} escreveu. Uma Goal que recalculasse a altitude por
     * conta propria seria a segunda autoridade sobre a mesma pergunta, e as duas
     * divergiriam no tick seguinte a um mergulho.</p>
     */
    private static final class VooDeComandoGoal extends Goal {
        private final AvianCommanderEntity comandante;

        private VooDeComandoGoal(AvianCommanderEntity comandante) {
            this.comandante = comandante;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (comandante.level().isClientSide || comandante.destino == null) return false;
            if (comandante.runtimeExigido().stagger().cambaleando()) return false;
            // Durante o golpe quem conduz o corpo e a MergulhoGoal. Duas Goals
            // mandando na navegacao no mesmo tick dao um bicho que trava no lugar --
            // cada uma cancela a rota da outra, e nada acusa.
            return comandante.runtimeExigido().ataques().phase() == AttackPhase.IDLE
                    || comandante.runtimeExigido().ataques().phase() == AttackPhase.COMPLETE;
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public void start() { comandante.destinoPendente = true; }

        @Override public void tick() {
            // A rota so e refeita quando o posto MUDOU. Chamar moveTo todo tick manda
            // o pathfinder de VOO recalcular vinte vezes por segundo -- e a malha de
            // voo e mais cara que a de chao. O sintoma nao e erro: e TPS caindo
            // enquanto a comandante esta em tela.
            if (comandante.destinoPendente && comandante.destino != null) {
                comandante.getNavigation().moveTo(comandante.destino.x, comandante.destino.y,
                        comandante.destino.z, velocidade());
                comandante.destinoPendente = false;
            }
            // Empurrao DIRETO no MoveControl quando a rota acabou ou nao existiu.
            // O FlyingPathNavigation trabalha em nos de bloco, e uma coluna
            // puramente vertical -- que e exatamente o destino de SUBIR e de
            // RECOLHER -- costuma sair sem rota. Sem este empurrao, a comandante
            // ficaria no chao "procurando caminho" para sempre: nenhum erro, nenhum
            // log, e a regra inteira deste mob apagada.
            if (comandante.getNavigation().isDone() && comandante.destino != null) {
                comandante.getMoveControl().setWantedPosition(comandante.destino.x,
                        comandante.destino.y, comandante.destino.z, velocidade());
            }
            LivingEntity alvo = comandante.getTarget();
            // Comandando, ela ENCARA o alvo do alto: e o unico sinal que diz ao
            // jogador que aquela silhueta la em cima esta no encontro, e nao de
            // passagem. Recolhendo, o olhar fica solto -- quem esta pagando o preco do
            // golpe nao encara.
            if (alvo != null && comandante.postura == PosturaDeComando.COMANDAR) {
                comandante.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            }
        }

        private double velocidade() {
            return switch (comandante.postura) {
                case MERGULHAR -> AvianCommanderTuning.VELOCIDADE_DE_MERGULHO;
                case COMANDAR -> AvianCommanderTuning.VELOCIDADE_DE_COMANDO;
                case SUBIR, RECOLHER -> AvianCommanderTuning.VELOCIDADE_DE_SUBIDA;
            };
        }

        @Override public void stop() {
            comandante.getNavigation().stop();
            comandante.destinoPendente = false;
        }
    }

    /** Dispara e conduz o mergulho; quem mede a janela e o {@link AttackController}. */
    private static final class MergulhoGoal extends Goal {
        private final AvianCommanderEntity comandante;

        private MergulhoGoal(AvianCommanderEntity comandante) {
            this.comandante = comandante;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (comandante.level().isClientSide) return false;
            if (comandante.postura != PosturaDeComando.MERGULHAR) return false;
            EnemyRuntime runtime = comandante.runtimeExigido();
            if (runtime.stagger().cambaleando()) return false;
            LivingEntity alvo = comandante.getTarget();
            if (alvo == null || !alvo.isAlive()) return false;
            return comandante.distanciaHorizontal(alvo) <= AvianCommanderTuning.ALCANCE_DAS_GARRAS
                    && comandante.hasLineOfSight(alvo)
                    && runtime.ataques().canStart();
        }

        @Override public boolean canContinueToUse() {
            // O golpe continua ate a fase acabar, MESMO que a postura ja tenha mudado.
            // Cortar o mergulho no meio porque a altura chegou ao fim deixaria o
            // jogador vendo um ataque que some -- e o servidor ja cobrou o windup
            // inteiro por ele.
            AttackPhase fase = comandante.runtimeExigido().ataques().phase();
            return fase != AttackPhase.IDLE && fase != AttackPhase.COMPLETE
                    && !comandante.runtimeExigido().stagger().cambaleando();
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() {
            comandante.getNavigation().stop();
            comandante.destinoPendente = false;
            comandante.runtimeExigido().ataques().start(AvianCommanderTuning.mergulho());
        }

        @Override public void tick() {
            LivingEntity alvo = comandante.getTarget();
            // Encarar o alvo so ate o fim do WINDUP: a partir da janela ACTIVE a
            // direcao esta TRAVADA. Sem travar, o mergulho viraria mira-laser -- ela
            // giraria junto com quem desvia, e sair de baixo dela, que e a resposta
            // que este mob ensina, deixaria de funcionar.
            AttackPhase fase = comandante.runtimeExigido().ataques().phase();
            if (alvo != null && fase == AttackPhase.WINDUP) {
                comandante.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            }
            if (fase != AttackPhase.ACTIVE) {
                // Freia fora da janela ativa. O impulso e aplicado uma unica vez; sem o
                // freio ele se somaria a inercia do mergulho e ela passaria direto pelo
                // alvo com a caixa junto.
                comandante.setDeltaMovement(comandante.getDeltaMovement().multiply(0.4D, 1.0D, 0.4D));
            }
        }

        @Override public void stop() { comandante.getNavigation().stop(); }
    }
}
