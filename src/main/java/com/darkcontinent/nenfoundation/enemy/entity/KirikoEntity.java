package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessInput;
import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.api.HxHEnemy;
import com.darkcontinent.nenfoundation.enemy.base.BaseHxHMob;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.AttackTimeline;
import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import com.darkcontinent.nenfoundation.enemy.encounter.RegrasDeJulgamento;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Kiriko: o Magical Beast que NAO te ataca -- ele te AVALIA.
 *
 * <p>DECISAO CENTRAL QUE ESTE ARQUIVO CARREGA, e ela e o oposto de todos os
 * outros mobs deste repositorio: <b>o jogador vence este encontro NAO
 * LUTANDO.</b> O kiriko aparece em forma humana, fica parado olhando, e decide.
 * Quem ataca -- ele ou um bicho pacifico na frente dele -- e REPROVADO: o
 * disfarce cai e vira briga. Quem espera, nao saca arma e o deixa em paz e
 * APROVADO: o disfarce cai, ele agradece, deixa a recompensa e vai embora
 * andando, sem um golpe. Isto e canone e e o ponto do mob. Se alguem
 * "consertar" isto dando a ele um {@code NearestAttackableTargetGoal}, a ficha
 * inteira foi apagada e NADA vai acusar: o build segue verde, o mob spawna,
 * anda e bate -- exatamente como os outros seis, que e o que ele nao pode ser.
 * A frase que a ficha usa e a que este arquivo existe para cumprir:
 * <b>Magical Beast != monster.</b></p>
 *
 * <p>ELE NAO TEM {@code targetSelector}. Nenhum. Nem
 * {@code NearestAttackableTargetGoal}, nem {@code HurtByTargetGoal}. O "nao
 * aggro aleatorio" da ficha esta ESCRITO em {@link #registerGoals()} em vez de
 * prometido num comentario: o unico {@code setTarget} nao nulo deste arquivo
 * mora no fim da transformacao de quem foi REPROVADO. Um alvo escolhido por
 * Goal seria uma segunda autoridade sobre a unica decisao que o mob tem, e ela
 * ganharia calada.</p>
 *
 * <p>QUEM DECIDE E {@link RegrasDeJulgamento}, e so ela. Esta entidade MEDE o
 * mundo -- quem esta perto e visivel, quem bateu em quem, ha quantos ticks
 * ninguem saca arma -- e pergunta. Nenhum limiar de aprovacao, nenhum custo de
 * agressao e nenhuma contagem de observacao moram aqui.</p>
 *
 * <p>DUAS SILHUETAS, DOIS IDS, como o man-faced ape: {@code kiriko_disfarce}
 * (forma humana) e {@code kiriko} (forma verdadeira). A hitbox e UMA SO --
 * 1.0 x 2.1 -- e isso nao e economia: se a caixa mudasse na revelacao, o
 * disfarce se entregaria pela colisao antes de o corpo mudar, e o jogador
 * aprenderia a ler a hitbox em vez de ler o bicho.</p>
 *
 * <p>Tudo que decide roda no SERVIDOR. Os quatro campos sincronizados carregam
 * apenas DISFARCADO, TRANSFORMANDO, REPROVADO e AVALIANDO (mais a FASE do
 * golpe), que e o que o cliente precisa para escolher o corpo e o clipe. O
 * cliente nunca informa agressao, paciencia nem veredito.</p>
 *
 * <p>SAIDA UNICA: existe UM metodo que termina o encontro
 * ({@link #encerrarEncontro}), e reprovacao, aprovacao, morte e
 * {@link #remove} passam TODOS por ele. Um caminho que limpasse por conta
 * propria nao daria erro nenhum: daria um kiriko que ja foi embora e continua
 * somando paciencia de um jogador que nao esta mais ali.</p>
 */
public final class KirikoEntity extends BaseHxHMob implements GeoEntity {

    // ------------------------------------------------------ estado publicado
    /**
     * Vestindo o corpo humano.
     *
     * <p>Ele continua LIGADO durante a transformacao inteira, de proposito: o
     * clipe {@code transform} mora no arquivo do DISFARCE e move ossos que so o
     * corpo humano tem. Desligado no primeiro tick da revelacao, o cliente
     * trocaria de modelo antes do clipe que encena a troca -- o unico momento
     * que o mob inteiro existe para mostrar nunca apareceria, e nada daria erro:
     * um clipe que procura osso inexistente deixa o membro parado, calado.</p>
     */
    private static final EntityDataAccessor<Boolean> DISFARCADO =
            SynchedEntityData.defineId(KirikoEntity.class, EntityDataSerializers.BOOLEAN);
    /** A janela do clipe {@code transform}; durante ela ele nao age nem apanha. */
    private static final EntityDataAccessor<Boolean> TRANSFORMANDO =
            SynchedEntityData.defineId(KirikoEntity.class, EntityDataSerializers.BOOLEAN);
    /**
     * Veredito de reprovacao, publicado.
     *
     * <p>E ele que separa os dois kirikos revelados na tela: o reprovado luta, o
     * aprovado agradece e vai embora. Sem ele o cliente teria de adivinhar pelo
     * alvo -- que nao e sincronizado -- e o clipe {@code approve} nunca tocaria
     * para o jogador que fez tudo certo.</p>
     */
    private static final EntityDataAccessor<Boolean> REPROVADO =
            SynchedEntityData.defineId(KirikoEntity.class, EntityDataSerializers.BOOLEAN);
    /**
     * Tem alguem em exame agora.
     *
     * <p>E a UNICA diferenca entre os clipes {@code idle} e {@code observe} do
     * disfarce, e ela e a pista do mob: parado olhando para VOCE nao e a mesma
     * coisa que parado olhando para o nada. Sem este campo o cliente so saberia
     * "parado", os dois clipes colapsariam num so, e o arquivo de animacao
     * passaria a declarar um clipe que ninguem pede -- sem erro nenhum.</p>
     */
    private static final EntityDataAccessor<Boolean> AVALIANDO =
            SynchedEntityData.defineId(KirikoEntity.class, EntityDataSerializers.BOOLEAN);
    /** Fase do golpe; o unico estado de combate que o cliente precisa conhecer. */
    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(KirikoEntity.class, EntityDataSerializers.INT);

    private static final AttackDefinition GOLPE = HunterExamProfiles.kirikoStrike();
    private static final RegrasDeJulgamento JULGAMENTO = HunterExamProfiles.kirikoJulgamento();

    // ------------------------------------------------------------ constantes
    /**
     * Ate onde ele avalia, e tambem ate onde ele testemunha uma crueldade.
     *
     * <p>Menor que a {@code FOLLOW_RANGE} (24) de proposito: o exame comeca
     * quando ele CONSEGUE observar, nao quando ele consegue ver um pixel. Igual
     * ao alcance de percepcao, o julgamento comecaria de tao longe que o jogador
     * seria reprovado por uma briga que ele nem sabia que estava sendo vista.</p>
     */
    private static final double RAIO_DE_JULGAMENTO = 16.0D;
    /**
     * Quantos ticks sem sacar arma contra nada fazem o tick contar como PAZ.
     *
     * <p>Um segundo. Nao e botao de balanceamento: e o tempo que separa "parou"
     * de "esta entre dois golpes". Zerado, o tick seguinte a uma pancada ja
     * renderia paciencia e o exame nao mediria nada.</p>
     */
    private static final int TICKS_DE_PAZ = 20;
    /**
     * Quanto tempo ele lembra de um candidato que sumiu de vista.
     *
     * <p>Dez segundos. Sair do campo de visao e voltar NAO recomeca o exame --
     * quem contornou uma arvore nao virou outra pessoa. Passado o prazo, a
     * proxima pessoa (ou a mesma) comeca com a ficha limpa.</p>
     */
    private static final int MEMORIA_DE_CANDIDATO = 200;
    /** Quanto tempo ele lembra de um alvo que saiu da linha de visao. */
    private static final int MEMORIA_DE_ALVO_TICKS = 100;

    /**
     * Duracao do clipe {@code transform}, e da janela em que ele nao age.
     *
     * <p>Trinta ticks -- um segundo e meio. E o unico momento do encontro em que
     * o jogador ve o que estava na frente dele o tempo todo, e ele precisa durar
     * o bastante para ser visto. E o telegrafo do kiriko reprovado tambem: quem
     * bateu tem esse tempo para recuar antes do primeiro golpe.</p>
     */
    private static final int TICKS_DE_TRANSFORMACAO = 30;
    /**
     * O tempo TOTAL de despedida do kiriko aprovado, contado do fim da
     * transformacao ate ele sumir do mundo.
     */
    private static final int TICKS_DE_PARTIDA = 240;
    /**
     * Os primeiros ticks da despedida, parado, encarando quem passou.
     *
     * <p>E o clipe {@code approve}, e ele e o premio narrativo do encontro: sem
     * essa pausa o kiriko aprovado viraria de costas no mesmo tick da revelacao
     * e o jogador nunca saberia que foi aprovado -- so veria um bicho estranho
     * andando embora.</p>
     */
    private static final int TICKS_DE_SAUDACAO = 40;
    /** Velocidade com que o aprovado se afasta; ele nao foge, ele se retira. */
    private static final double VELOCIDADE_DE_PARTIDA = 0.9D;
    /** Velocidade do recuo por vida critica -- essa sim e fuga. */
    private static final double VELOCIDADE_DE_RECUO = 1.3D;
    private static final int RAIO_DA_PARTIDA = 20;
    private static final int ALTURA_DA_PARTIDA = 7;

    /** Folga, alem do meio-corpo, para a garra alcancar o alvo. */
    private static final double ALCANCE_DO_GOLPE = 2.0D;
    /** Respiro entre um golpe e o proximo; sem ele a garra sairia todo tick. */
    private static final int TICKS_ENTRE_GOLPES = 20;
    /** Alem disto o reprovado larga a perseguicao -- mas NAO perdoa. */
    private static final double DISTANCIA_DE_DESISTENCIA = 32.0D;
    /** Recalcular caminho todo tick e desperdicio; quatro vezes por segundo basta. */
    private static final int TICKS_ENTRE_RECALCULOS = 5;
    private static final double VELOCIDADE_DE_APROXIMACAO = 1.0D;
    /** Fracao da vida abaixo da qual o cerebro pode decidir recuar. */
    private static final float FRACAO_DE_VIDA_CRITICA = 0.25F;

    // ------------------------------------------------------------- animacao
    // Os nomes abaixo sao um CONTRATO com kiriko_disfarce.animation.json e com
    // kiriko.animation.json. Errar um deles nao da erro: o GeckoLib simplesmente nao
    // acha o clipe e deixa o osso parado. E o tipo de falha que so aparece na tela de
    // quem joga -- e CoerenciaDeGeckoLibTest existe para pegar exatamente isto.
    //
    // Os quatro primeiros moram no arquivo do DISFARCE e movem ossos que so o corpo
    // humano tem (hat); os seis ultimos moram no arquivo da forma VERDADEIRA e movem
    // ossos que so o bicho tem (beak, crest, tail, claw_*). Por isso o clipe e os tres
    // recursos precisam trocar pela MESMA pergunta -- ver estaDisfarcado().
    private static final RawAnimation DISFARCE_IDLE = RawAnimation.begin()
            .then("animation.kiriko_disfarce.idle", Animation.LoopType.DEFAULT);
    private static final RawAnimation DISFARCE_WALK = RawAnimation.begin()
            .then("animation.kiriko_disfarce.walk", Animation.LoopType.DEFAULT);
    private static final RawAnimation DISFARCE_OBSERVE = RawAnimation.begin()
            .then("animation.kiriko_disfarce.observe", Animation.LoopType.DEFAULT);
    private static final RawAnimation DISFARCE_TRANSFORM = RawAnimation.begin()
            .then("animation.kiriko_disfarce.transform", Animation.LoopType.DEFAULT);
    private static final RawAnimation IDLE = RawAnimation.begin()
            .then("animation.kiriko.idle", Animation.LoopType.DEFAULT);
    private static final RawAnimation WALK = RawAnimation.begin()
            .then("animation.kiriko.walk", Animation.LoopType.DEFAULT);
    private static final RawAnimation STRIKE = RawAnimation.begin()
            .then("animation.kiriko.strike", Animation.LoopType.DEFAULT);
    private static final RawAnimation APPROVE = RawAnimation.begin()
            .then("animation.kiriko.approve", Animation.LoopType.DEFAULT);
    private static final RawAnimation HURT = RawAnimation.begin()
            .then("animation.kiriko.hurt", Animation.LoopType.DEFAULT);
    private static final RawAnimation DEATH = RawAnimation.begin()
            .then("animation.kiriko.death", Animation.LoopType.DEFAULT);

    /** Nome do unico controller; quem registrar um segundo clipe reusa esta constante. */
    private static final String CONTROLLER_DO_CORPO = "corpo";
    /**
     * Ticks de mistura entre um clipe e o proximo.
     *
     * <p>Quatro, e nao mais: a janela ACTIVE do golpe dura 5 ticks. Uma transicao
     * mais longa do que a fase que ela atravessa comeria o golpe inteiro em
     * mistura, e quem levasse a pancada nunca veria a garra descer.</p>
     */
    private static final int TRANSICAO_EM_TICKS = 4;

    /** Cache por INSTANCIA. Um cache estatico faria todos os kirikos dividirem um clipe. */
    private final AnimatableInstanceCache cacheDeAnimacao = GeckoLibUtil.createInstanceCache(this);

    /** O que ja foi decidido sobre o encontro deste kiriko. */
    private enum Veredito { PENDENTE, REPROVADO, APROVADO, ABORTADO }

    // -------------------------------------------------------- estado runtime
    // Estado DA INSTANCIA. Guardar qualquer um destes numa Goal (ou num static)
    // faria todos os kirikos do mundo dividirem o mesmo exame -- a Goal e uma por
    // entidade, e as classes aninhadas abaixo sao static justamente para nao deixar
    // essa dependencia escondida.
    //
    // Nada disto e persistido, DE PROPOSITO: o exame e estado de RUNTIME (ADR-002).
    // Um save no meio da avaliacao devolve um kiriko que recomeca a observar, que e
    // o comportamento certo -- ele nao viu o que aconteceu enquanto o mundo estava
    // fechado.
    private Veredito veredito = Veredito.PENDENTE;
    /** Quem esta em exame agora. Referencia de runtime; pode sumir por fora. */
    private Player candidato;
    private int pontuacao;
    private int ticksObservados;
    private int ticksSemCandidato;
    /** Ticks desde a ultima vez que o candidato sacou arma contra qualquer coisa. */
    private int ticksDesdeAgressao;

    // As duas entradas que chegam DE FORA do tick, e por isso sao flags de um tick
    // so: hurt() e o testemunho de agressao podem acontecer em qualquer momento do
    // tick, e o julgamento le e limpa as duas uma vez por tick, num lugar so.
    private boolean atacouOKirikoNesteTick;
    private boolean feriuInocenteNesteTick;
    /**
     * De QUEM e a ofensa registrada neste tick.
     *
     * <p>Sem ele as duas flags seriam anonimas, e o tick cobraria a conta de quem
     * estivesse mais perto. Com duas pessoas em volta -- uma atirando de longe,
     * outra parada olhando -- a parada e que seria reprovada, e nada acusaria.</p>
     */
    private Player ofensor;

    private int transformacaoRestante;
    private int partidaRestante;
    /**
     * Quem reprovou este kiriko, por UUID e nao por referencia.
     *
     * <p>UUID porque a referencia morre no logout e o veredito nao: quem reprovou
     * e voltou ao servidor continua reprovado, e o kiriko volta a encontra-lo. Uma
     * referencia guardada aqui viraria um objeto de jogador zumbi que impede o
     * antigo de ser coletado e aponta para uma sessao que acabou.</p>
     */
    private UUID reprovadoUuid;
    private Player jogadorAprovado;

    private AttackTimeline linhaDoTempoDoGolpe = new AttackTimeline();
    private boolean golpeEmAndamento;
    private int esperaEntreGolpes;

    // Sensores do cerebro.
    private int memoriaDeAlvo;
    private double distanciaAnteriorDoAlvo = Double.NaN;

    public KirikoEntity(EntityType<? extends KirikoEntity> type, Level level) {
        super(type, level, HunterExamProfiles.kiriko().metadata(),
                // O cerebro passa de WARN para ENGAGE exatamente no tempo do aviso do
                // golpe: um segundo numero aqui divergiria do perfil sem dar erro, e o
                // bicho "avisaria" num relogio e bateria em outro.
                new AwarenessTuning(GOLPE.windupTicks(), MEMORIA_DE_ALVO_TICKS));
        // Nasce DISFARCADO e CALADO: um bicho que grunhe enquanto finge ser gente ja
        // se entregou antes de o jogador ter o que observar.
        setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = HunterExamProfiles.kiriko().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<KirikoEntity> registeredType() {
        return EnemyEntityTypes.KIRIKO.get();
    }

    /** Ninguem poe coleira num Magical Beast que veio te avaliar. */
    @Override
    public boolean canBeLeashed() {
        return false;
    }

    // -------------------------------------------------- dados sincronizados

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DISFARCADO, Boolean.TRUE);
        builder.define(TRANSFORMANDO, Boolean.FALSE);
        builder.define(REPROVADO, Boolean.FALSE);
        builder.define(AVALIANDO, Boolean.FALSE);
        builder.define(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
    }

    /**
     * Vestindo o corpo humano; vale nos dois lados, porque vem do
     * SynchedEntityData.
     *
     * <p>ESTA E A UNICA PERGUNTA QUE TROCA silhueta, textura, arquivo de animacao
     * e clipe, e ela continua verdadeira durante a transformacao inteira -- ver o
     * campo {@link #DISFARCADO}. Modelo e clipe escolhidos por perguntas
     * diferentes nao dariam erro nenhum: dariam um clipe procurando osso que o
     * modelo carregado nao tem, e osso que nao existe fica parado, calado.</p>
     */
    public boolean estaDisfarcado() {
        return this.entityData.get(DISFARCADO);
    }

    /** Na janela do clipe {@code transform}; vale nos dois lados, pelo mesmo motivo. */
    public boolean estaTransformando() {
        return this.entityData.get(TRANSFORMANDO);
    }

    /** Reprovado -- e reprovado nao se desfaz. Vale nos dois lados, pelo mesmo motivo. */
    public boolean foiReprovado() {
        return this.entityData.get(REPROVADO);
    }

    /** Tem alguem em exame; separa {@code observe} de {@code idle} no disfarce. */
    public boolean estaAvaliando() {
        return this.entityData.get(AVALIANDO);
    }

    /** Fase corrente do golpe; vale nos dois lados, pelo mesmo motivo. */
    public AttackPhase faseDeAtaque() {
        int ordinal = this.entityData.get(FASE_DE_ATAQUE);
        AttackPhase[] fases = AttackPhase.values();
        return ordinal >= 0 && ordinal < fases.length ? fases[ordinal] : AttackPhase.IDLE;
    }

    // ------------------------------------------------------------------ tick

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) {
            return;
        }

        // A transformacao e a linha do tempo do golpe sao tickadas AQUI, e nao dentro
        // das Goals, porque uma Goal pode ser preemptada por outra de prioridade maior.
        // Se o relogio dependesse da Goal viva, a revelacao congelaria no meio do clipe
        // -- sem erro nenhum no log, e com o bicho preso entre dois corpos.
        if (transformacaoRestante > 0) {
            tickDaTransformacao();
        } else {
            switch (veredito) {
                case PENDENTE -> tickDoJulgamento();
                case REPROVADO -> tickDaHostilidade();
                case APROVADO -> tickDaPartida();
                default -> { }
            }
        }

        EnemyAwarenessState consciencia = enemyBrain().tick(lerSensores());
        alinharEstadoDeCombate(consciencia);
    }

    // ------------------------------------------------------------ julgamento

    /**
     * O EXAME. Ele nao decide nada: mede o mundo e pergunta a
     * {@link RegrasDeJulgamento}.
     *
     * <p>As tres entradas chegam de tres lugares diferentes e de proposito:
     * <b>atacouOKiriko</b> vem de {@link #hurt}, <b>feriuInocente</b> vem do
     * testemunho de agressao ({@link #testemunharAgressao}) e <b>esperouEmPaz</b>
     * e medido aqui, por tempo. As duas primeiras sao flags de UM tick, lidas e
     * limpas neste metodo -- se alguma sobrevivesse ao tick, um unico golpe seria
     * cobrado para sempre e o jogador seria reprovado por algo que fez uma vez
     * so.</p>
     *
     * <p>VER E CONDICAO PARA PREMIAR, NAO PARA PUNIR, e a assimetria e
     * deliberada. Paciencia e tempo de observacao so correm com o candidato A
     * VISTA -- ganhar credito escondido atras de uma arvore nao e ser observado.
     * Mas a FLECHA vinda de tras da arvore conta: o kiriko sentiu, e esta e a
     * unica leitura que nao pode depender de linha de visao. Exigir os dois lados
     * visiveis daria a receita de um exame que se passa com arco, sem erro nenhum
     * no log.</p>
     */
    private void tickDoJulgamento() {
        // A OFENSA MANDA SOBRE A PROXIMIDADE, e essa precedencia nao e detalhe: com
        // duas pessoas por perto, quem atira escondido seria trocado pelo transeunte
        // mais proximo no tick seguinte, e a conta de -40 cairia na ficha de quem
        // estava parado sem fazer nada. O jogador reprovado nunca saberia por que.
        Player ofensa = atacouOKirikoNesteTick || feriuInocenteNesteTick ? ofensor : null;
        Player aVista = candidatoDoTick();
        if (ofensa != null) {
            ticksSemCandidato = 0;
            adotarCandidato(ofensa);
        } else if (aVista != null) {
            ticksSemCandidato = 0;
            adotarCandidato(aVista);
        } else if (candidato != null && ++ticksSemCandidato >= MEMORIA_DE_CANDIDATO) {
            esquecerCandidato();
        }

        // "Observando" e o candidato ATUAL estar a vista -- e nao "ha alguem a vista".
        boolean observando = aVista != null && aVista == candidato;
        this.entityData.set(AVALIANDO, Boolean.valueOf(observando));
        Player quem = candidato;
        if (quem == null) {
            limparFlagsDoTick();
            return;
        }

        if (observando) {
            ticksObservados++;
        }
        ticksDesdeAgressao++;

        boolean esperouEmPaz = observando && ticksDesdeAgressao >= TICKS_DE_PAZ;
        pontuacao = JULGAMENTO.pontuar(pontuacao, atacouOKirikoNesteTick,
                feriuInocenteNesteTick, esperouEmPaz);
        limparFlagsDoTick();

        if (JULGAMENTO.reprova(pontuacao)) {
            encerrarEncontro(Veredito.REPROVADO, quem);
            return;
        }
        if (JULGAMENTO.aprova(pontuacao, ticksObservados)) {
            encerrarEncontro(Veredito.APROVADO, quem);
        }
    }

    /**
     * O jogador em exame neste tick: o mais proximo, dentro do raio, vivo,
     * visivel e nao espectador.
     *
     * <p>LINHA DE VISAO E OBRIGATORIA. O kiriko avalia o que VE; sem isto ele
     * reprovaria alguem que cacou um bicho do outro lado de uma parede, e o
     * jogador nunca teria como saber por que.</p>
     */
    private Player candidatoDoTick() {
        Player perto = level().getNearestPlayer(this, RAIO_DE_JULGAMENTO);
        if (perto == null || !perto.isAlive() || perto.isSpectator()) {
            return null;
        }
        return hasLineOfSight(perto) ? perto : null;
    }

    /**
     * Troca de candidato COMECA UM EXAME NOVO.
     *
     * <p>A ficha do exame e de uma pessoa. Herdar a pontuacao de quem estava ali
     * antes aprovaria quem acabou de chegar por paciencia alheia, e reprovaria
     * quem chegou depois de uma briga que nao era dele -- nenhum dos dois daria
     * erro.</p>
     */
    private void adotarCandidato(Player quem) {
        if (candidato == quem) {
            return;
        }
        candidato = quem;
        pontuacao = 0;
        ticksObservados = 0;
        ticksDesdeAgressao = 0;
    }

    private void esquecerCandidato() {
        candidato = null;
        pontuacao = 0;
        ticksObservados = 0;
        ticksSemCandidato = 0;
        ticksDesdeAgressao = 0;
    }

    private void limparFlagsDoTick() {
        atacouOKirikoNesteTick = false;
        feriuInocenteNesteTick = false;
        ofensor = null;
    }

    // ----------------------------------------------------------- testemunho

    /**
     * UM JOGADOR FERIU ALGUEM -- avisa os kirikos que estao avaliando por perto.
     *
     * <p>ESTE E O CAMINHO DE "FERIU INOCENTE", e ele e um EVENTO, nao uma
     * varredura. O barramento de jogo ja sabe a hora exata em que alguem toma
     * dano ({@code LivingIncomingDamageEvent}); o kiriko so precisa ser avisado.
     * A alternativa -- varrer a vizinhanca todo tick atras de bichos machucados
     * -- custaria uma busca por kiriko por tick para descobrir uma coisa que
     * acontece uma vez a cada muitos segundos, e ainda assim erraria: dano curado
     * entre dois ticks nao deixa rastro nenhum para a varredura encontrar.</p>
     *
     * <p>O CUSTO DESTA ESCOLHA, declarado: uma busca por caixa
     * ({@code getEntitiesOfClass(KirikoEntity.class, ...)}) por evento de dano
     * causado por JOGADOR. Zero custo por tick quando ninguem bate em nada; a
     * caixa e a do raio de julgamento em volta do agressor, e a classe filtra na
     * propria consulta de secao. Nenhum kiriko no mundo, nenhum trabalho.</p>
     *
     * <p>Mora aqui, e nao no listener, porque o RAIO e a nocao de INOCENTE sao do
     * mob: o listener so sabe que alguem bateu em alguem. Espalhado, o raio
     * viraria dois numeros que podem divergir.</p>
     */
    public static void testemunharAgressao(Player jogador, LivingEntity vitima) {
        if (jogador == null || vitima == null) {
            return;
        }
        Level nivel = jogador.level();
        if (nivel.isClientSide || jogador.isSpectator()) {
            return;
        }
        AABB volta = jogador.getBoundingBox().inflate(RAIO_DE_JULGAMENTO);
        for (KirikoEntity kiriko : nivel.getEntitiesOfClass(KirikoEntity.class, volta,
                testemunha -> testemunha.emExame())) {
            kiriko.anotarAgressao(jogador, vitima);
        }
    }

    /**
     * A UNICA pergunta "este kiriko ainda esta avaliando alguem?".
     *
     * <p>Ela e lida pelo testemunho, por {@link #hurt} e pelas duas Goals do
     * disfarce. Escrita de novo em cada um deles -- uma esquecendo a
     * transformacao, outra o veredito -- daria um kiriko que soma paciencia no
     * meio da propria revelacao, e nada acusaria.</p>
     */
    private boolean emExame() {
        return isAlive() && veredito == Veredito.PENDENTE && estaDisfarcado() && !estaTransformando();
    }

    /**
     * QUALQUER agressao quebra a paz; ferir um INOCENTE ainda custa crueldade.
     *
     * <p>Os dois efeitos sao separados de proposito. Bater num bicho que estava te
     * atacando nao e crueldade -- mas tambem nao e paz: enquanto a briga durar, a
     * paciencia nao anda. Juntar os dois numa condicao so faria quem se defende
     * ser reprovado, e quem caca em silencio ser aprovado.</p>
     */
    private void anotarAgressao(Player jogador, LivingEntity vitima) {
        // Bater NELE tem caminho proprio -- hurt(). Contar aqui tambem cobraria o
        // mesmo golpe duas vezes, e o numero final seria plausivel demais para alguem
        // notar sem medir.
        if (vitima == this) {
            return;
        }
        if (distanceTo(jogador) > RAIO_DE_JULGAMENTO || !hasLineOfSight(jogador)) {
            return;
        }
        adotarCandidato(jogador);
        ticksDesdeAgressao = 0;
        if (!inocente(jogador, vitima)) {
            return;
        }
        // O kiriko julga o que VE. A vitima precisa estar no campo de visao dele, e
        // nao so perto do agressor: condenar por uma morte atras de uma parede daria
        // ao jogador uma reprovacao que ele nao tem como entender.
        if (distanceTo(vitima) > RAIO_DE_JULGAMENTO || !hasLineOfSight(vitima)) {
            return;
        }
        ofensor = jogador;
        feriuInocenteNesteTick = true;
    }

    /**
     * INOCENTE: fauna pacifica -- nossa ou do vanilla -- que nao estava atacando
     * ninguem.
     *
     * <p>A regra tem tres recusas, e cada uma existe por um motivo. Jogador e
     * outro kiriko nao contam (o primeiro nao e fauna; o segundo tem caminho
     * proprio). Monstro nao conta: zumbi morto na frente do kiriko nao e
     * crueldade, e cobrar isso ensinaria ao jogador a coisa errada sobre o mob.
     * E QUEM ESTAVA ATACANDO O JOGADOR nao conta -- defender-se de um
     * frog-in-waiting que ja te mordeu nao e crueldade, ainda que a faccao dele
     * seja WILDLIFE. Sem esta ultima linha o exame puniria exatamente a resposta
     * que os outros mobs deste repositorio ensinam.</p>
     */
    private static boolean inocente(Player jogador, LivingEntity vitima) {
        if (vitima instanceof Player || vitima instanceof KirikoEntity || vitima instanceof Enemy) {
            return false;
        }
        if (vitima instanceof Mob bicho && bicho.getTarget() == jogador) {
            return false;
        }
        if (vitima instanceof HxHEnemy nosso) {
            return nosso.enemyMetadata().faction() == EnemyFaction.WILDLIFE;
        }
        return vitima instanceof Animal;
    }

    // -------------------------------------------------------- transformacao

    /**
     * A REVELACAO. Aprovado e reprovado passam pelo MESMO estado, e isso e
     * deliberado: o corpo verdadeiro aparece do mesmo jeito nos dois casos, e so
     * o que vem DEPOIS e diferente. Dois caminhos de revelacao divergiriam em
     * silencio -- um esqueceria de parar a navegacao, o outro de publicar a fase.
     */
    private void iniciarTransformacao() {
        transformacaoRestante = TICKS_DE_TRANSFORMACAO;
        this.entityData.set(TRANSFORMANDO, Boolean.TRUE);
        this.entityData.set(AVALIANDO, Boolean.FALSE);
        golpeEmAndamento = false;
        linhaDoTempoDoGolpe = new AttackTimeline();
        publicarFase(AttackPhase.IDLE);
        getNavigation().stop();
        combatState(EnemyCombatState.WINDUP);
    }

    /** Parado, sem navegar, sem bater e sem apanhar -- ver {@link #isInvulnerableTo}. */
    private void tickDaTransformacao() {
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
        Player quem = veredito == Veredito.APROVADO ? jogadorAprovado : reprovadoVivo();
        if (quem != null) {
            getLookControl().setLookAt(quem, 30.0F, 30.0F);
        }
        if (--transformacaoRestante > 0) {
            return;
        }
        // O corpo verdadeiro so aparece AGORA, no fim do clipe que o mostra chegando.
        this.entityData.set(TRANSFORMANDO, Boolean.FALSE);
        this.entityData.set(DISFARCADO, Boolean.FALSE);
        setSilent(false);
        if (veredito == Veredito.REPROVADO) {
            // O UNICO setTarget nao nulo deste arquivo, e ele e explicito e nominal.
            setTarget(reprovadoVivo());
            combatState(EnemyCombatState.AGGRO);
            return;
        }
        entregarRecompensa();
        partidaRestante = TICKS_DE_PARTIDA;
        combatState(EnemyCombatState.RETREAT);
    }

    // ------------------------------------------------------------- aprovado

    /**
     * A DESPEDIDA: agradece, depois vai embora andando e some.
     *
     * <p>SEM TELEPORTE, de proposito. O kiriko aprovado sai do mundo pelo mesmo
     * caminho por que qualquer bicho sai -- andando para longe --, e so depois
     * disso e removido. Um {@code teleportTo} resolveria em um tick e apagaria a
     * unica prova visual de que o encontro terminou bem.</p>
     */
    private void tickDaPartida() {
        if (--partidaRestante <= 0) {
            // Sai por DISCARDED, e nao por KILLED: ele nao morreu, ele foi embora. A
            // diferenca importa para quem for pendurar bestiario ou quest neste ponto.
            remove(Entity.RemovalReason.DISCARDED);
            return;
        }
        if (partidaRestante > TICKS_DE_PARTIDA - TICKS_DE_SAUDACAO) {
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
            if (jogadorAprovado != null && jogadorAprovado.isAlive()) {
                getLookControl().setLookAt(jogadorAprovado, 30.0F, 30.0F);
            }
            return;
        }
        afastarSe();
    }

    /** Anda para longe de quem passou no teste; impulso nenhum, rota de verdade. */
    private void afastarSe() {
        if (partidaRestante % TICKS_ENTRE_RECALCULOS != 0 || !getNavigation().isDone()) {
            return;
        }
        Vec3 origem = jogadorAprovado != null && jogadorAprovado.isAlive()
                ? jogadorAprovado.position()
                : position().subtract(getLookAngle());
        Vec3 destino = DefaultRandomPos.getPosAway(this, RAIO_DA_PARTIDA, ALTURA_DA_PARTIDA, origem);
        if (destino == null) {
            return;
        }
        getNavigation().moveTo(destino.x, destino.y, destino.z, VELOCIDADE_DE_PARTIDA);
    }

    /**
     * A RECOMPENSA de quem venceu o encontro NAO LUTANDO.
     *
     * <p>PONTO CEGO DECLARADO: hoje ela e a MESMA tabela de loot que cairia de
     * quem o matasse a pancada -- o que separa os dois caminhos e o trabalho, nao
     * o premio. A tabela propria da aprovacao, o bestiario e a quest amarrados a
     * este ponto sao de outra frente, e estao anotados. O
     * {@code lastHurtByPlayer} e escrito para a tabela enxergar sorte e
     * encantamento de saque de quem passou, que e o mesmo caminho que o master of
     * the swamp usa na captura.</p>
     */
    private void entregarRecompensa() {
        if (jogadorAprovado == null || !jogadorAprovado.isAlive()) {
            return;
        }
        this.lastHurtByPlayer = jogadorAprovado;
        this.lastHurtByPlayerTime = 100;
        dropFromLootTable(damageSources().playerAttack(jogadorAprovado), true);
    }

    // ------------------------------------------------------------ reprovado

    /** Reprovado: ele caca QUEM O REPROVOU, e mais ninguem. */
    private void tickDaHostilidade() {
        if (esperaEntreGolpes > 0) {
            esperaEntreGolpes--;
        }
        if (golpeEmAndamento) {
            tickDoGolpe();
        }
        LivingEntity alvo = getTarget();
        if (alvo != null && (!alvo.isAlive() || distanceTo(alvo) > DISTANCIA_DE_DESISTENCIA)) {
            // Largar a perseguicao NAO e perdoar: o veredito continua REPROVADO e o
            // reencontro abaixo volta a mirar a mesma pessoa. Sem esta desistencia ele
            // atravessaria o mundo atras de quem ja fugiu.
            setTarget(null);
            alvo = null;
        }
        if (alvo == null) {
            setTarget(reprovadoVivo());
        }
    }

    /** Quem reprovou este kiriko, se estiver perto, vivo e a vista. Nunca outro. */
    private Player reprovadoVivo() {
        if (reprovadoUuid == null) {
            return null;
        }
        Player quem = level().getPlayerByUUID(reprovadoUuid);
        if (quem == null || !quem.isAlive() || quem.isSpectator() || quem.level() != level()) {
            return null;
        }
        return distanceTo(quem) <= DISTANCIA_DE_DESISTENCIA && hasLineOfSight(quem) ? quem : null;
    }

    /** Comeca o aviso; a linha do tempo e NOVA para nunca herdar fase presa. */
    private void iniciarGolpe() {
        golpeEmAndamento = true;
        linhaDoTempoDoGolpe = new AttackTimeline();
        linhaDoTempoDoGolpe.start(GOLPE);
        getNavigation().stop();
        combatState(EnemyCombatState.WINDUP);
        publicarFase(AttackPhase.WINDUP);
    }

    /**
     * WINDUP e a garra subindo -- o aviso. ACTIVE e o golpe. RECOVERY e a
     * recuperacao.
     *
     * <p>O dano so acontece em ACTIVE, e SO contra o alvo: este mob nao bate em
     * area e nao machuca quem passou perto. Quem foi reprovado e quem apanha.</p>
     */
    private void tickDoGolpe() {
        AttackPhase fase = linhaDoTempoDoGolpe.phase();
        getNavigation().stop();
        LivingEntity alvo = getTarget();
        switch (fase) {
            case WINDUP -> {
                combatState(EnemyCombatState.WINDUP);
                if (alvo != null) {
                    getLookControl().setLookAt(alvo, 30.0F, 30.0F);
                }
            }
            case ACTIVE -> {
                combatState(EnemyCombatState.ACTIVE);
                aplicarDanoDoGolpe(alvo);
            }
            case RECOVERY -> combatState(EnemyCombatState.RECOVERY);
            default -> { }
        }
        linhaDoTempoDoGolpe.tick();
        AttackPhase proxima = linhaDoTempoDoGolpe.phase();
        if (proxima != AttackPhase.COMPLETE) {
            publicarFase(proxima);
            return;
        }
        golpeEmAndamento = false;
        esperaEntreGolpes = TICKS_ENTRE_GOLPES;
        publicarFase(AttackPhase.IDLE);
        combatState(getTarget() != null ? EnemyCombatState.AGGRO : EnemyCombatState.IDLE);
    }

    private void aplicarDanoDoGolpe(LivingEntity alvo) {
        if (alvo == null || !alvo.isAlive() || !alvo.isAttackable()) {
            return;
        }
        if (alvo instanceof Player jogador && (jogador.isCreative() || jogador.isSpectator())) {
            return;
        }
        if (distanceTo(alvo) > getBbWidth() * 0.5D + ALCANCE_DO_GOLPE) {
            return;
        }
        if (!alvo.hurt(damageSources().mobAttack(this), GOLPE.damage())) {
            return;
        }
        alvo.knockback(GOLPE.knockback(), getX() - alvo.getX(), getZ() - alvo.getZ());
    }

    // -------------------------------------------------------- ciclo de vida

    /**
     * SER FERIDO POR JOGADOR E O CAMINHO DA REPROVACAO.
     *
     * <p>E o unico. Fogo, queda, outro mob, pocao: nada disso julga ninguem --
     * o kiriko avalia PESSOAS, e cobrar dele uma reprovacao por um cacto seria
     * transformar o exame em azar. Este metodo tambem NAO multiplica dano nenhum
     * e nao escolhe alvo: ele registra a agressao e deixa
     * {@link RegrasDeJulgamento} decidir no tick, que e onde o veredito mora.</p>
     *
     * <p>DURANTE A TRANSFORMACAO ele nem chega aqui -- ver
     * {@link #isInvulnerableTo}.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean aplicou = super.hurt(source, amount);
        if (!aplicou || level().isClientSide || !isAlive()) {
            return aplicou;
        }
        if (!emExame() || !(source.getEntity() instanceof Player jogador) || jogador.isSpectator()) {
            return aplicou;
        }
        // Quem bate ASSUME o exame. Sem isto, um segundo jogador poderia bater a
        // vontade enquanto o kiriko avalia o primeiro -- e a conta cairia na ficha de
        // quem estava parado.
        adotarCandidato(jogador);
        ofensor = jogador;
        atacouOKirikoNesteTick = true;
        ticksDesdeAgressao = 0;
        return aplicou;
    }

    /**
     * DURANTE A TRANSFORMACAO ELE NAO E ATACAVEL -- escolha declarada.
     *
     * <p>A revelacao e o unico momento que este mob existe para mostrar, e ela
     * dura um segundo e meio. Deixa-la atravessavel faria um jogador com arco
     * derrubar o kiriko no meio do clipe: o encontro terminaria antes de o
     * jogador descobrir que era um encontro, e a ficha inteira viraria "um bicho
     * que morre disfarcado". Em troca, ele tambem NAO ataca e NAO navega nesse
     * periodo -- a janela custa o mesmo para os dois lados.</p>
     *
     * <p>Dano que IGNORA invulnerabilidade continua passando ({@code /kill}, o
     * void). Recusar ate isso deixaria uma entidade que um operador nao consegue
     * remover, e trocaria um problema visual por um problema de servidor.</p>
     */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (estaTransformando() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            combatState(EnemyCombatState.DYING);
            encerrarEncontro(Veredito.ABORTADO, null);
            // NENHUM KIRIKO MORRE FINGINDO SER GENTE. O corpo que cai e o verdadeiro --
            // e isso nao e enfeite: o clipe de morte mora no arquivo da forma
            // verdadeira, e um cadaver em corpo humano pediria um clipe que nao existe.
            // O membro ficaria parado, calado, e o unico sinal disso seria alguem ver a
            // cena.
            this.entityData.set(DISFARCADO, Boolean.FALSE);
            setSilent(false);
        }
        super.die(source);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide) {
            encerrarEncontro(Veredito.ABORTADO, null);
        }
        super.remove(reason);
    }

    /**
     * SAIDA UNICA do encontro.
     *
     * <p>Reprovacao, aprovacao, morte e {@link #remove} terminam TODOS aqui. E
     * aqui que o exame zera, o veredito e gravado UMA vez e a revelacao comeca.
     * Se um caminho deixar de passar por aqui, o kiriko continua somando
     * paciencia de um jogador que ja foi embora, ou aprova depois de reprovar --
     * e nenhuma das duas coisas aparece como erro.</p>
     *
     * <p>A LIMPEZA RODA SEMPRE; o VEREDITO, so uma vez. E essa assimetria que faz
     * "reprovado nao se desfaz" ser verdade mesmo quando este metodo e chamado de
     * novo pela morte de um kiriko que ja estava lutando.</p>
     */
    private void encerrarEncontro(Veredito novo, Player quem) {
        candidato = null;
        pontuacao = 0;
        ticksObservados = 0;
        ticksSemCandidato = 0;
        ticksDesdeAgressao = 0;
        limparFlagsDoTick();
        this.entityData.set(AVALIANDO, Boolean.FALSE);
        getNavigation().stop();

        if (veredito == Veredito.PENDENTE && novo != Veredito.PENDENTE) {
            veredito = novo;
            if (novo == Veredito.REPROVADO) {
                reprovadoUuid = quem == null ? null : quem.getUUID();
                this.entityData.set(REPROVADO, Boolean.TRUE);
            } else if (novo == Veredito.APROVADO) {
                jogadorAprovado = quem;
            }
            if (novo != Veredito.ABORTADO) {
                iniciarTransformacao();
                return;
            }
        }

        if (novo == Veredito.ABORTADO) {
            // Morte e remocao NAO deixam relogio andando. Uma transformacao pendente num
            // cadaver deixaria a fase presa para o cliente que ainda ve o corpo cair.
            transformacaoRestante = 0;
            partidaRestante = 0;
            golpeEmAndamento = false;
            linhaDoTempoDoGolpe = new AttackTimeline();
            this.entityData.set(TRANSFORMANDO, Boolean.FALSE);
            publicarFase(AttackPhase.IDLE);
        }
    }

    private void publicarFase(AttackPhase fase) {
        this.entityData.set(FASE_DE_ATAQUE, fase.ordinal());
    }

    // ------------------------------------------------------------- animacao

    /**
     * O CLIENTE NAO DECIDE NADA. Ele le o que o servidor ja publica --
     * {@link #estaDisfarcado()}, {@link #estaTransformando()},
     * {@link #foiReprovado()}, {@link #estaAvaliando()} e
     * {@link #faseDeAtaque()}, os cinco vindos do SynchedEntityData -- e escolhe
     * o clipe correspondente. Nao existe aqui nenhum timer, nenhuma heuristica de
     * "parece que vai revelar" e nenhuma copia da regra de julgamento. Se a
     * animacao e o servidor discordarem, quem esta errado e o arquivo de
     * animacao.
     *
     * <p>Ordem de precedencia, da mais especifica para a menos: morte,
     * transformacao, corpo humano (observando, andando, parado) e depois a forma
     * verdadeira (dor, golpe, despedida, andar, ocio). O CORPO VEM ANTES DA FASE
     * porque o clipe e o modelo tem de sair do MESMO arquivo -- ver
     * {@link #estaDisfarcado()}.</p>
     *
     * <p>"APROVADO" NAO TEM CAMPO PROPRIO, e isso e uma leitura e nao uma
     * adivinhacao: o kiriko so sai do disfarce por veredito, entao revelado e
     * NAO reprovado ja quer dizer aprovado. Um quinto booleano seria uma segunda
     * fonte para a mesma verdade, e as duas divergiriam no dia em que alguem
     * esquecesse de escrever uma delas.</p>
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<KirikoEntity>(this, CONTROLLER_DO_CORPO,
                TRANSICAO_EM_TICKS, this::clipeDoCorpo));
    }

    private PlayState clipeDoCorpo(AnimationState<KirikoEntity> estado) {
        // isDeadOrDying e hurtTime valem nos DOIS lados: vida e sincronizada, e o
        // piscar de dano chega ao cliente pelo evento de entidade do vanilla.
        if (isDeadOrDying()) {
            return estado.setAndContinue(DEATH);
        }
        if (estaTransformando()) {
            return estado.setAndContinue(DISFARCE_TRANSFORM);
        }
        if (estaDisfarcado()) {
            if (estado.isMoving()) {
                return estado.setAndContinue(DISFARCE_WALK);
            }
            return estado.setAndContinue(estaAvaliando() ? DISFARCE_OBSERVE : DISFARCE_IDLE);
        }
        if (this.hurtTime > 0) {
            return estado.setAndContinue(HURT);
        }
        AttackPhase fase = faseDeAtaque();
        if (fase == AttackPhase.WINDUP || fase == AttackPhase.ACTIVE || fase == AttackPhase.RECOVERY) {
            return estado.setAndContinue(STRIKE);
        }
        if (estado.isMoving()) {
            return estado.setAndContinue(WALK);
        }
        // Parado e revelado sem ter reprovado ninguem: e a saudacao da aprovacao.
        return estado.setAndContinue(foiReprovado() ? IDLE : APPROVE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cacheDeAnimacao;
    }

    @Override
    public double getTick(Object entidade) {
        return this.tickCount;
    }

    // -------------------------------------------------------------- sensores

    /** Leitura de mundo do tick: e isto que faz awarenessState() parar de ser decorativo. */
    private AwarenessInput lerSensores() {
        // O "alvo" deste mob e quem ele esta OLHANDO -- o candidato em exame enquanto
        // disfarcado, quem o reprovou depois disso. Ele nunca escolhe uma presa, e por
        // isso nao tem targetSelector.
        LivingEntity alvo = veredito == Veredito.REPROVADO ? getTarget() : candidato;
        boolean alvoVivo = alvo != null && alvo.isAlive();
        boolean visivel = alvoVivo && isPerceivedVisible(alvo);
        double distancia = alvoVivo ? distanceTo(alvo) : Double.NaN;

        if (visivel) {
            memoriaDeAlvo = MEMORIA_DE_ALVO_TICKS;
        } else if (memoriaDeAlvo > 0) {
            memoriaDeAlvo--;
        }

        boolean recuando = visivel && !Double.isNaN(distanciaAnteriorDoAlvo)
                && distancia > distanciaAnteriorDoAlvo;
        distanciaAnteriorDoAlvo = distancia;

        boolean audivel = !visivel && (isPerceivedAudible(alvo) || memoriaDeAlvo > 0);
        boolean vidaCritica = getHealth() <= getMaxHealth() * FRACAO_DE_VIDA_CRITICA;

        // ambushOpportunity FALSO, sempre, e isto e a ficha escrita em codigo: o
        // kiriko disfarcado nao esta esperando o momento de botar -- ele esta
        // avaliando. Oferecer emboscada ao cerebro colocaria o mob no mesmo caminho do
        // man-faced ape, que e justamente o que ele nao e.
        //
        // territorial=false no perfil: ele nao defende lugar nenhum, entao nunca avisa.
        return new AwarenessInput(visivel, audivel, false, recuando, !alvoVivo,
                memoriaDeAlvo, false, vidaCritica);
    }

    /** A transformacao e o golpe mandam; fora deles quem manda e a consciencia. */
    private void alinharEstadoDeCombate(EnemyAwarenessState consciencia) {
        if (combatState() == EnemyCombatState.DYING) {
            return;
        }
        if (transformacaoRestante > 0 || golpeEmAndamento) {
            return;
        }
        if (veredito == Veredito.APROVADO) {
            combatState(EnemyCombatState.RETREAT);
            return;
        }
        if (consciencia == EnemyAwarenessState.FLEE) {
            combatState(EnemyCombatState.RETREAT);
            return;
        }
        if (veredito == Veredito.REPROVADO) {
            combatState(getTarget() != null ? EnemyCombatState.AGGRO : EnemyCombatState.IDLE);
            return;
        }
        combatState(EnemyCombatState.IDLE);
    }

    /** Transformando, aprovado ou recuando, o corpo nao esta livre para bater. */
    private boolean prontoParaBater() {
        return veredito == Veredito.REPROVADO && transformacaoRestante == 0
                && awarenessState() != EnemyAwarenessState.FLEE;
    }

    // ------------------------------------------------------------------ goals

    /**
     * NENHUM {@code targetSelector}. Nenhum.
     *
     * <p>Sem {@code NearestAttackableTargetGoal} -- ele nao caca jogador. Sem
     * {@code HurtByTargetGoal} -- quem bate nele e REPROVADO pelo exame, e o alvo
     * vem do veredito, no fim da transformacao. Acrescentar qualquer um dos dois
     * daria ao mob um segundo caminho para escolher alvo, e esse caminho ganharia
     * primeiro: o kiriko passaria a revidar ANTES de julgar, a revelacao nunca
     * aconteceria, e nada acusaria -- ele so pareceria mais um bicho do mato.</p>
     */
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SegurarOCorpoGoal(this));
        goalSelector.addGoal(2, new RecuoGoal(this));
        goalSelector.addGoal(3, new GolpeGoal(this));
        goalSelector.addGoal(4, new ObservarGoal(this));
        goalSelector.addGoal(6, new PasseioCalmoGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    /**
     * Segura MOVE e LOOK durante a transformacao e a despedida.
     *
     * <p>Ela nao faz nada: existe para as Goals de passeio nao disputarem o corpo
     * enquanto o tick da entidade o conduz. Sem ela, o passeio aleatorio herdaria
     * o MOVE no mesmo tick e o kiriko sairia caminhando no meio do proprio clipe
     * de revelacao -- sem erro nenhum no log.</p>
     */
    private static final class SegurarOCorpoGoal extends Goal {
        private final KirikoEntity kiriko;

        private SegurarOCorpoGoal(KirikoEntity kiriko) {
            this.kiriko = kiriko;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            return !kiriko.level().isClientSide
                    && (kiriko.transformacaoRestante > 0 || kiriko.partidaRestante > 0);
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public boolean requiresUpdateEveryTick() { return true; }
    }

    /**
     * O EXAME EM CENA: parado, encarando quem esta sendo avaliado.
     *
     * <p>E o oposto exato do man-faced ape, e essa oposicao e o desenho: o macaco
     * so anda quando NAO esta sendo olhado; o kiriko para para OLHAR. Quem ja viu
     * um dos dois aprende a diferenca na primeira vez -- um se aproxima quando
     * voce desvia, o outro fica plantado te encarando.</p>
     *
     * <p>E static de proposito: todo estado que ela le mora na ENTIDADE, e o
     * prefixo {@code kiriko.} deixa isso impossivel de esquecer. Um campo aqui
     * seria dividido por todos os kirikos do mundo.</p>
     */
    private static final class ObservarGoal extends Goal {
        private final KirikoEntity kiriko;

        private ObservarGoal(KirikoEntity kiriko) {
            this.kiriko = kiriko;
            // LOOK junto de MOVE: avaliando, esta Goal e a UNICA autoridade sobre a
            // cabeca. Sem isto o LookAtPlayerGoal escreveria no mesmo look control no
            // mesmo tick, e "para quem ele esta olhando" teria duas fontes.
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            return !kiriko.level().isClientSide && kiriko.emExame() && kiriko.candidato != null;
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void stop() { kiriko.getNavigation().stop(); }

        @Override public void tick() {
            Player quem = kiriko.candidato;
            if (quem == null) {
                return;
            }
            kiriko.getLookControl().setLookAt(quem, 30.0F, 30.0F);
            kiriko.getNavigation().stop();
            kiriko.setDeltaMovement(kiriko.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
        }
    }

    /** Passeio calmo de quem finge ser gente -- e so SEM ninguem em exame. */
    private static final class PasseioCalmoGoal extends WaterAvoidingRandomStrollGoal {
        private final KirikoEntity kiriko;

        private PasseioCalmoGoal(KirikoEntity kiriko) {
            super(kiriko, 0.8D);
            this.kiriko = kiriko;
        }

        private boolean livre() {
            return kiriko.emExame() && kiriko.candidato == null;
        }

        @Override public boolean canUse() { return livre() && super.canUse(); }

        @Override public boolean canContinueToUse() { return livre() && super.canContinueToUse(); }
    }

    /**
     * Aproxima e golpeia -- e SO existe depois da reprovacao.
     *
     * <p>Ela NAO ticka a linha do tempo do golpe: quem faz isso e o
     * {@code customServerAiStep}, para que uma Goal de prioridade maior nao
     * congele a fase ao preemptar.</p>
     */
    private static final class GolpeGoal extends Goal {
        private final KirikoEntity kiriko;
        private int recalculoRestante;

        private GolpeGoal(KirikoEntity kiriko) {
            this.kiriko = kiriko;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (kiriko.level().isClientSide || !kiriko.prontoParaBater()) {
                return false;
            }
            LivingEntity alvo = kiriko.getTarget();
            return alvo != null && alvo.isAlive();
        }

        @Override public boolean canContinueToUse() {
            return kiriko.golpeEmAndamento || canUse();
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() { recalculoRestante = 0; }

        @Override public void stop() { kiriko.getNavigation().stop(); }

        @Override public void tick() {
            LivingEntity alvo = kiriko.getTarget();
            if (alvo == null || kiriko.golpeEmAndamento) {
                return;
            }
            kiriko.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            boolean aoAlcance = kiriko.distanceTo(alvo)
                    <= kiriko.getBbWidth() * 0.5D + ALCANCE_DO_GOLPE;
            if (aoAlcance && kiriko.esperaEntreGolpes == 0) {
                kiriko.iniciarGolpe();
                return;
            }
            if (aoAlcance) {
                kiriko.getNavigation().stop();
                return;
            }
            if (recalculoRestante > 0) {
                recalculoRestante--;
                return;
            }
            recalculoRestante = TICKS_ENTRE_RECALCULOS;
            kiriko.getNavigation().moveTo(alvo, VELOCIDADE_DE_APROXIMACAO);
        }
    }

    /**
     * Recuo por vida critica, decidido pelo {@link EnemyAwarenessState#FLEE} do
     * cerebro -- e nao por um segundo limiar de vida escrito aqui.
     *
     * <p>ELA EXISTE PARA O SENSOR NAO MENTIR. {@code lerSensores} informa vida
     * critica ao cerebro, e o cerebro responde FLEE; sem uma Goal que faca alguma
     * coisa com essa resposta, {@code awarenessState()} viraria enfeite e toda
     * ferramenta de diagnostico repetiria a mentira. Recuar tambem e coerente com
     * o mob: um Magical Beast inteligente ja provou o ponto dele e nao morre por
     * teimosia. O VEREDITO NAO MUDA -- ele recua reprovado.</p>
     */
    private static final class RecuoGoal extends Goal {
        private final KirikoEntity kiriko;
        private double destinoX;
        private double destinoY;
        private double destinoZ;

        private RecuoGoal(KirikoEntity kiriko) {
            this.kiriko = kiriko;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override public boolean canUse() {
            if (kiriko.level().isClientSide || kiriko.veredito != Veredito.REPROVADO) {
                return false;
            }
            if (kiriko.transformacaoRestante > 0 || kiriko.golpeEmAndamento) {
                return false;
            }
            if (kiriko.awarenessState() != EnemyAwarenessState.FLEE) {
                return false;
            }
            LivingEntity alvo = kiriko.getTarget();
            return alvo != null && alvo.isAlive() && procurarDestino(alvo);
        }

        @Override public boolean canContinueToUse() {
            return kiriko.awarenessState() == EnemyAwarenessState.FLEE
                    && !kiriko.getNavigation().isDone();
        }

        @Override public void start() {
            kiriko.getNavigation().moveTo(destinoX, destinoY, destinoZ, VELOCIDADE_DE_RECUO);
        }

        @Override public void stop() { kiriko.getNavigation().stop(); }

        private boolean procurarDestino(LivingEntity alvo) {
            Vec3 destino = DefaultRandomPos.getPosAway(kiriko, RAIO_DA_PARTIDA, ALTURA_DA_PARTIDA,
                    alvo.position());
            if (destino == null) {
                return false;
            }
            destinoX = destino.x;
            destinoY = destino.y;
            destinoZ = destino.z;
            return true;
        }
    }
}
