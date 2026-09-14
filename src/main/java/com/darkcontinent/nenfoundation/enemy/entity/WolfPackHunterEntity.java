package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.DecisaoDeMatilha;
import com.darkcontinent.nenfoundation.enemy.ai.DecisaoDeReforco;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.ai.RegistroDeMatilhas;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeMatilha;
import com.darkcontinent.nenfoundation.enemy.ai.squad.Squad;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadController;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadInput;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadOrder;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRegistry;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRole;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.base.BaseHxHMob;
import com.darkcontinent.nenfoundation.enemy.base.EnemyRuntime;
import com.darkcontinent.nenfoundation.enemy.combat.AttackController;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import com.darkcontinent.nenfoundation.enemy.content.GreedIslandProfiles;
import com.darkcontinent.nenfoundation.enemy.content.WolfPackHunterTuning;
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
 * Wolf Pack Hunter -- criatura de Greed Island, e o PRIMEIRO consumidor de Squad.
 *
 * <p><b>O corpo perde; o bando ganha.</b> HP 26, dano 6, armadura 2 -- um lobo
 * sozinho nao e pareo para ninguem, e ele SABE disso: com menos de
 * {@code MEMBROS_PARA_AVANCAR} membros vivos, {@link RegrasDeMatilha} manda
 * recuar, e ele recua. O que vence e o grupo, e toda esta classe existe para que
 * a diferenca entre "quatro lobos" e "um lobo quatro vezes" seja visivel na
 * tela.</p>
 *
 * <p><b>O que o bando faz e que uma multidao nao faria.</b> O alvo e UM so e mora
 * no {@link Squad} -- nao em cada membro, porque alvo por membro daria quatro
 * bichos perseguindo quatro pessoas e um cerco que nunca fecha. Os postos sao
 * distintos: cada papel ocupa um angulo proprio em torno do OLHAR do alvo, e o
 * anel e largo o bastante para que {@code SquadRules.matilha().espacamento()}
 * caiba entre vizinhos. E so dois papeis investem por vez -- os quatro mordendo
 * juntos transformariam o cerco numa trituradora, e a recuperacao longa, que e o
 * que espaca os golpes no tempo, deixaria de valer.</p>
 *
 * <p><b>Matar o chefe muda o bando no MESMO tick, e nao o dissolve.</b>
 * {@code Squad.sair} promove o proximo na ordem de entrada e cobra a moral de uma
 * vez; dali em diante o papel de cada membro -- e portanto o posto dele no anel e
 * se ele investe -- sai do {@code Squad}, nunca de um campo local. O jogador ve a
 * matilha MUDAR, e nao sumir, e sabe que a decisao teve efeito. Perder o chefe
 * nao quebra o bando sozinho: deixa-o a seis golpes de quebrar, em vez de doze.</p>
 *
 * <p><b>O teto e cobrado, e e ele que impede reforco de chamar reforco.</b> Um
 * lobo sem bando procura um vizinho que aceite; o vizinho responde com
 * {@link DecisaoDeReforco}, e {@code BANDO_CHEIO} e {@code BANDO_DESMORALIZADO}
 * sao recusas com motivo. Sem elas, cada lobo que chega e mais um lobo que pode
 * chamar outro, e a matilha cresce ate o chunk inteiro -- sem erro, so com o
 * servidor ficando lento.</p>
 *
 * <p><b>A coordenacao roda no orcamento de {@link SquadRules}, e nao por tick.</b>
 * Dez ticks, com a desfasagem tirada do ID DO BANDO: assim os membros de um mesmo
 * bando decidem no mesmo tick (senao metade cercaria a posicao velha do alvo) e
 * dois bandos diferentes nunca decidem juntos (senao o custo vira um pico
 * periodico, que se parece com problema de rede e e pior de diagnosticar). O que
 * roda TODO tick e so o relogio do golpe -- uma janela de quatro ticks medida de
 * dez em dez acertaria por sorte.</p>
 *
 * <p>Ela usa {@link EnemyRuntime} por inteiro -- percepcao, cerebro, ataque e
 * stagger -- e o comportamento de bando entra SOBRE a fundacao, nunca ao lado
 * dela.</p>
 */
public final class WolfPackHunterEntity extends BaseHxHMob implements software.bernie.geckolib.animatable.GeoEntity {

    /** Fase do ataque: o unico estado de combate que o cliente precisa conhecer. */
    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(WolfPackHunterEntity.class, EntityDataSerializers.INT);
    /** Cambaleando: sincronizado porque interrupcao invisivel e stagger que so o servidor conhece. */
    private static final EntityDataAccessor<Boolean> CAMBALEANDO =
            SynchedEntityData.defineId(WolfPackHunterEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int MEMORIA_DE_ALVO_TICKS = 120;
    private static final int TICKS_DE_AVISO = 20;
    private static final double ABERTURA_DA_VISAO = 75.0D;
    private static final double ALCANCE_DE_AUDICAO = 14.0D;
    private static final float FRACAO_DE_VIDA_CRITICA = 0.25F;

    /**
     * As regras do bando, montadas UMA vez.
     *
     * <p>Estaticas porque sao um record imutavel, sem estado de jogador. O erro
     * que este projeto ja sabe que comete -- estado de jogador num campo de
     * classe -- seria guardar aqui o alvo ou o papel, e ai todos os lobos do
     * servidor dividiriam um. Papel e alvo vivem no {@link Squad}, por bando.</p>
     */
    private static final RegrasDeMatilha REGRAS_DA_MATILHA = WolfPackHunterTuning.matilha();
    private static final SquadRules REGRAS_DO_BANDO = REGRAS_DA_MATILHA.bando();
    private static final AttackHitbox CAIXA_DA_MORDIDA = WolfPackHunterTuning.caixaDaMordida();

    /**
     * Quanto um golpe num membro tira da moral do bando inteiro.
     *
     * <p>Pouco, de proposito. Com moral cheia sao doze golpes ate o bando quebrar;
     * depois que o chefe cai (custo 40), sao seis. Essa conta e o que faz "matar o
     * lider primeiro" ser uma tatica em vez de uma curiosidade -- e ela e do
     * BICHO, nao do framework, por isso mora aqui e nao em {@code SquadRules}.</p>
     */
    private static final int ABALO_POR_GOLPE_NO_MEMBRO = 6;

    /** Tolerancia para nao refazer o caminho quando o posto mal se moveu, em blocos. */
    private static final double TOLERANCIA_DO_POSTO = 1.0D;
    /** Multiplicador de velocidade ao fechar o cerco. */
    private static final double VELOCIDADE_DE_CERCO = 1.15D;
    /** Multiplicador de velocidade ao recuar -- maior que o de avanco, e de proposito. */
    private static final double VELOCIDADE_DE_RECUO = 1.3D;

    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache cacheDeAnimacao =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    /** A ultima decisao de bando. Recalculada no orcamento; consumida todo tick pelas Goals. */
    private DecisaoDeMatilha decisao = DecisaoDeMatilha.AGUARDAR;
    /**
     * O alvo do BANDO, por UUID e nunca por referencia.
     *
     * <p>Guardar a entidade impediria o objeto de morrer: quatro lobos segurariam
     * quatro cadaveres na memoria, sem erro algum. E a referencia sobreviveria ao
     * unload do chunk, deixando o lobo perseguindo uma entidade que o mundo nao
     * tem mais.</p>
     */
    private UUID alvoCompartilhado;
    /** Para onde ir; recalculado no orcamento de coordenacao. */
    private Vec3 destino;
    /** O destino mudou desde o ultimo caminho tracado? Evita recalcular rota todo tick. */
    private boolean destinoPendente;
    /** A instancia de ataque que ja recebeu o salto -- um salto por mordida, nunca quatro. */
    private long ultimaInstanciaComSalto;

    public WolfPackHunterEntity(EntityType<? extends WolfPackHunterEntity> type, Level level) {
        super(type, level, GreedIslandProfiles.wolfPackHunter().metadata(),
                new AwarenessTuning(TICKS_DE_AVISO, MEMORIA_DE_ALVO_TICKS));
        instalarRuntime(montarRuntime());
    }

    private EnemyRuntime montarRuntime() {
        var perfil = GreedIslandProfiles.wolfPackHunter();
        PerceptionController percepcao = new PerceptionController(
                PerceptionBudget.padrao(),
                VisionCone.deGraus(perfil.attributes().followRange(), ABERTURA_DA_VISAO),
                new ThreatMemory(MEMORIA_DE_ALVO_TICKS),
                new TargetEvaluator(FactionRelations.padrao(), perfil.metadata().faction(),
                        perfil.attributes().followRange(), perfil.metadata().territorial()),
                PercepcaoDeAura.NENHUMA, ALCANCE_DE_AUDICAO, getUUID().hashCode());
        return new EnemyRuntime(percepcao,
                new EnemyBrain(new AwarenessTuning(TICKS_DE_AVISO, MEMORIA_DE_ALVO_TICKS)),
                new AttackController(GreedIslandProfiles.wolfPackHunterRecarga()),
                new StaggerState(GreedIslandProfiles.wolfPackHunterStagger()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = GreedIslandProfiles.wolfPackHunter().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<WolfPackHunterEntity> registeredType() { return EnemyEntityTypes.WOLF_PACK_HUNTER.get(); }

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

    /** A decisao de bando deste tick -- leitura de SERVIDOR, para gametest e diagnostico. */
    public DecisaoDeMatilha decisaoDeMatilha() { return decisao; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MordidaGoal(this));
        goalSelector.addGoal(3, new CercoGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // NENHUM target goal vanilla: quem escolhe alvo aqui e o Squad, e antes
        // dele o TargetEvaluator por faccao. Um NearestAttackableTargetGoal ao
        // lado seria uma segunda autoridade sobre a mesma decisao -- e seria a
        // pior possivel NESTE mob, porque ela decide por INDIVIDUO: cada lobo
        // voltaria a escolher o proprio alvo, e o cerco nunca fecharia sem que
        // nada acusasse.
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
        // A desfasagem sai do id do BANDO, e nao do proprio: membros do mesmo
        // bando tem de decidir no MESMO tick, senao metade cerca a posicao velha
        // do alvo. Sem bando ainda, a do proprio serve -- ela so espalha no tempo
        // as tentativas de entrar num.
        int desfasagem = (bando != null ? bando.id() : getUUID()).hashCode();
        if (REGRAS_DO_BANDO.atualizaNesteTick(tickCount, desfasagem)) {
            coordenar(registro, bando, snapshot);
        }

        aplicarAlvo();
        tickDoGolpe(runtime);
        publicarEstado(runtime);
    }

    /**
     * Um passo de coordenacao -- e o UNICO lugar que mexe no bando.
     *
     * <p>Concentrado aqui de proposito. Espalhado pelas Goals, cada uma leria e
     * escreveria o bando no proprio ritmo, e duas delas trocariam o alvo no mesmo
     * tick; a ultima venceria, e o sintoma e um bicho indeciso sem causa visivel.</p>
     */
    private void coordenar(SquadRegistry registro, Squad bandoAtual, PerceptionSnapshot snapshot) {
        Squad bando = bandoAtual;
        // Um bando de um so nao e bando: ele tenta se juntar a um vizinho antes de
        // qualquer outra coisa. Sem esta fusao, quatro lobos que nascem juntos
        // criariam quatro bandos de um e TODOS recuariam para sempre -- com o
        // registro, o teto e a moral funcionando perfeitamente.
        if (bando == null || bando.tamanho() == 1) {
            bando = procurarBando(registro, bando);
        }
        // A faxina roda no MESMO ritmo da coordenacao, e nao "quando der". Bando
        // sem ninguem nao consome tick e nao aparece em lugar nenhum; ele so ocupa
        // memoria e pode ressuscitar mais tarde com o alvo de uma hora atras.
        registro.removerDissolvidos();

        if (bando == null) {
            decisao = DecisaoDeMatilha.AGUARDAR;
            return;
        }
        SquadRole papel = bando.papelDe(getUUID());
        if (papel == null) {
            // O indice e o bando discordaram. Sair e a saida segura: seguir com um
            // papel nulo faria toda decisao adiante estourar um NullPointerException
            // no meio do tick do servidor, com pilha que nao diz qual lobo era.
            registro.sair(getUUID());
            decisao = DecisaoDeMatilha.AGUARDAR;
            return;
        }

        publicarAlvoDoBando(bando, papel, snapshot);
        alvoCompartilhado = bando.alvo().orElse(null);
        LivingEntity alvo = resolver(alvoCompartilhado);

        boolean visivel = alvo != null && hasLineOfSight(alvo);
        boolean ferido = getHealth() <= getMaxHealth() * FRACAO_DE_VIDA_CRITICA;
        // alvoRecuando e FALSO de proposito, e e decisao de design do bicho: esta
        // matilha nao se desfaz porque a presa correu -- correr e o que ela caca.
        // O unico gatilho de recuo coletivo e a moral, e leituraPara ja a soma
        // aqui dentro.
        SquadInput leitura = bando.leituraPara(getUUID(), visivel, false, ferido);
        // O controller e montado a cada coordenacao, a partir do papel que o Squad
        // tem AGORA. Guarda-lo num campo faria dele uma segunda fonte para o
        // papel: depois de uma promocao o campo continuaria dizendo FLANKER
        // enquanto o bando ja diz LEADER, e o lobo ocuparia o posto de outro no
        // anel sem que nada reclamasse.
        SquadOrder ordem = new SquadController(papel).update(leitura);

        double distancia = visivel ? distanceTo(alvo) : 0.0D;
        decisao = REGRAS_DA_MATILHA.decidir(ordem, bando.tamanho(), distancia,
                RegrasDeMatilha.papelInveste(ordem.role()));
        atualizarDestino(alvo, ordem.role());
    }

    /**
     * Procura um bando vizinho que aceite; se nenhum aceitar, funda o proprio.
     *
     * <p>A varredura de mundo acontece SO aqui, SO no orcamento de coordenacao e
     * SO enquanto o lobo esta sozinho. Um lobo em bando formado nunca varre nada
     * -- e e por isso que o teto tambem e um limite de CUSTO, e nao so de
     * tamanho.</p>
     */
    private Squad procurarBando(SquadRegistry registro, Squad meuBandoDeUmSo) {
        AABB area = getBoundingBox().inflate(REGRAS_DO_BANDO.raioDeReforco());
        for (WolfPackHunterEntity vizinho : level().getEntitiesOfClass(WolfPackHunterEntity.class,
                area, outro -> outro != this && outro.isAlive())) {
            Squad candidato = registro.bandoDe(vizinho.getUUID()).orElse(null);
            if (candidato == null) continue;
            if (candidato.contem(getUUID())) continue;
            if (meuBandoDeUmSo != null && candidato.id().equals(meuBandoDeUmSo.id())) continue;

            DecisaoDeReforco resposta = REGRAS_DA_MATILHA.decidirReforco(candidato.tamanho(),
                    candidato.moral(), distanceTo(vizinho));
            if (resposta != DecisaoDeReforco.CHAMA) continue;
            Optional<SquadRole> papel = REGRAS_DA_MATILHA.papelVago(candidato.membros().values());
            if (papel.isEmpty()) continue;

            // Sair ANTES de entrar, sempre: o registro recusa quem ainda consta em
            // outro bando, e a recusa vem como excecao no meio do tick do servidor.
            registro.sair(getUUID());
            if (registro.entrar(candidato.id(), getUUID(), papel.get())) return candidato;
            // Recusado depois de ter saido: sem bando neste tick. O proximo
            // orcamento funda um novo -- melhor do que forcar a entrada e estourar
            // o teto que acabou de ser cobrado.
            return null;
        }
        if (meuBandoDeUmSo != null) return meuBandoDeUmSo;
        return registro.criar(UUID.randomUUID(), REGRAS_DO_BANDO, getUUID());
    }

    /**
     * Quem escreve o alvo do bando, e quando.
     *
     * <p>Todos os membros coordenam no mesmo tick, entao a regra de escrita
     * precisa ser estreita ou eles se sobrescrevem. Duas linhas resolvem:</p>
     *
     * <ul>
     *   <li>quem ENXERGA alguem so publica se o bando nao tiver alvo valido --
     *       senao um lobo que ve um segundo jogador trocaria o alvo do cerco no
     *       meio dele, e a matilha oscilaria entre dois alvos sem nada acusar;</li>
     *   <li>so o LIDER apaga um alvo que deixou de existir. Com qualquer membro
     *       podendo apagar, um que estivesse de costas limparia, no mesmo tick, o
     *       alvo que outro acabou de publicar.</li>
     * </ul>
     */
    private void publicarAlvoDoBando(Squad bando, SquadRole papel, PerceptionSnapshot snapshot) {
        boolean valido = resolver(bando.alvo().orElse(null)) != null;
        Optional<UUID> meu = snapshot.alvoOpcional();
        if (meu.isPresent()) {
            if (!valido) bando.alvo(meu.get());
            return;
        }
        if (!valido && papel == SquadRole.LEADER) bando.alvo(null);
    }

    /** UUID -> entidade viva DESTE nivel, ou nulo. A referencia nunca e guardada. */
    private LivingEntity resolver(UUID id) {
        if (id == null || level().isClientSide) return null;
        Entity achado = ((ServerLevel) level()).getEntity(id);
        return achado instanceof LivingEntity vivo && vivo.isAlive() ? vivo : null;
    }

    /**
     * Onde este lobo deve estar, dada a decisao.
     *
     * <p>O posto do cerco e medido a partir do OLHAR do alvo, e nao do norte: em
     * angulo de mundo o lider ficaria sempre ao norte do jogador, e o cerco
     * deixaria de reagir a quem gira. Com o olhar como referencia, virar as costas
     * para um lobo poe outro na sua frente -- que e o que um cerco faz.</p>
     */
    private void atualizarDestino(LivingEntity alvo, SquadRole papel) {
        Vec3 novo = switch (decisao) {
            case CERCAR -> alvo == null ? null : postoNoAnel(alvo, papel);
            case RECUAR -> pontoDeRecuo(alvo);
            case INVESTIR, AGUARDAR -> null;
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

    private Vec3 postoNoAnel(LivingEntity alvo, SquadRole papel) {
        double graus = alvo.getYRot() + REGRAS_DA_MATILHA.anguloDeCercoEmGraus(papel);
        double radianos = Math.toRadians(graus);
        // Yaw 0 aponta para +Z e yaw positivo gira para -X -- a MESMA convencao de
        // AttackHitbox.noMundo. Trocar o sinal do seno aqui espelharia o anel
        // inteiro, e o cerco giraria ao contrario de quem ele cerca sem erro algum.
        double dx = -Math.sin(radianos) * WolfPackHunterTuning.RAIO_DO_CERCO;
        double dz = Math.cos(radianos) * WolfPackHunterTuning.RAIO_DO_CERCO;
        return new Vec3(alvo.getX() + dx, alvo.getY(), alvo.getZ() + dz);
    }

    /**
     * Para onde fugir.
     *
     * <p>Sem alvo, fugir tem de continuar significando alguma coisa: um lobo cujo
     * bando quebrou e que nao ve ninguem ainda precisa sair de onde esta, senao
     * "recuar" vira "ficar parado" e o jogador nunca ve o bando quebrar.</p>
     */
    private Vec3 pontoDeRecuo(LivingEntity alvo) {
        Vec3 fuga = alvo == null ? getLookAngle().reverse() : position().subtract(alvo.position());
        Vec3 horizontal = new Vec3(fuga.x, 0.0D, fuga.z);
        if (horizontal.lengthSqr() < 1.0E-6D) return null;
        return position().add(horizontal.normalize().scale(WolfPackHunterTuning.RAIO_DO_CERCO));
    }

    /** O alvo da ENTIDADE e sempre o do bando -- nunca um que so este lobo viu. */
    private void aplicarAlvo() {
        LivingEntity alvo = resolver(alvoCompartilhado);
        if (alvo == null) {
            if (getTarget() != null) setTarget(null);
            return;
        }
        if (getTarget() != alvo) setTarget(alvo);
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
     * A janela ACTIVE consulta a caixa da mordida; fora dela o golpe nao existe.
     *
     * <p>Roda TODO tick, e nao no orcamento de coordenacao: quatro ticks de janela
     * medidos de dez em dez ticks acertariam ou errariam por sorte.</p>
     */
    private void tickDoGolpe(EnemyRuntime runtime) {
        AttackController ataques = runtime.ataques();
        if (ataques.phase() != AttackPhase.ACTIVE) return;
        aplicarSalto(ataques.attackInstanceId());

        LivingEntity alvo = getTarget();
        if (alvo == null || !alvo.isAlive()) return;
        ataques.tryHit(alvo.getId(), alvo.getBoundingBox(), CAIXA_DA_MORDIDA,
                        position(), getYRot(), "body")
                .ifPresent(hit -> {
                    alvo.hurt(damageSources().mobAttack(this), hit.damage());
                    alvo.knockback(WolfPackHunterTuning.EMPURRAO_DA_MORDIDA,
                            getX() - alvo.getX(), getZ() - alvo.getZ());
                });
    }

    /**
     * O salto que paga a diferenca entre o focinho desenhado e a caixa de mordida.
     *
     * <p>UMA vez por instancia de ataque. Aplicado nos quatro ticks da janela, o
     * lobo atravessaria o alvo e sairia do outro lado -- e {@code tryHit}, que so
     * acerta cada alvo uma vez por instancia, esconderia o defeito atras de um
     * dano perfeitamente correto.</p>
     */
    private void aplicarSalto(long instancia) {
        if (instancia == ultimaInstanciaComSalto) return;
        ultimaInstanciaComSalto = instancia;
        Vec3 olhar = getLookAngle();
        Vec3 frente = new Vec3(olhar.x, 0.0D, olhar.z);
        if (frente.lengthSqr() < 1.0E-6D) return;
        frente = frente.normalize().scale(WolfPackHunterTuning.IMPULSO_DA_INVESTIDA);
        setDeltaMovement(getDeltaMovement().add(frente.x, 0.0D, frente.z));
        this.hasImpulse = true;
    }

    /**
     * Apanhar interrompe a mordida E abala o bando.
     *
     * <p>Nao ha ponto fraco neste bicho, e a ausencia e deliberada: a licao dele e
     * contar quantos sao, nao mirar um lugar. Um multiplicador por regiao daria ao
     * jogador uma resposta individual para um problema coletivo.</p>
     *
     * <p>Dano sem atacante -- fogo, queda -- nao cambaleia e nao abala: cambalear
     * por queimadura transformaria fogo numa interrupcao permanente, e um bando
     * quebrado por lava nao ensina nada sobre bando.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean levou = super.hurt(source, amount);
        if (!levou || level().isClientSide) return levou;
        if (!(source.getEntity() instanceof LivingEntity)) return true;

        EnemyRuntime runtime = runtimeExigido();
        StaggerResult resultado = runtime.sofrerStagger(runtime.ataques().attackInstanceId(),
                amount, WolfPackHunterTuning.RECARGA_APOS_INTERRUPCAO);
        if (resultado == StaggerResult.DISPAROU) {
            getNavigation().stop();
            this.entityData.set(CAMBALEANDO, true);
            this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        }
        RegistroDeMatilhas.doNivel((ServerLevel) level()).bandoDe(getUUID())
                .ifPresent(bando -> bando.abalar(ABALO_POR_GOLPE_NO_MEMBRO));
        return true;
    }

    private void publicarEstado(EnemyRuntime runtime) {
        int fase = runtime.ataques().phase().ordinal();
        if (this.entityData.get(FASE_DE_ATAQUE) != fase) this.entityData.set(FASE_DE_ATAQUE, fase);
        boolean cambaleando = runtime.stagger().cambaleando();
        if (this.entityData.get(CAMBALEANDO) != cambaleando) {
            this.entityData.set(CAMBALEANDO, cambaleando);
        }
        if (combatState() != EnemyCombatState.DYING) {
            combatState(cambaleando ? EnemyCombatState.STAGGERED
                    : decisao == DecisaoDeMatilha.RECUAR ? EnemyCombatState.RETREAT
                    : getTarget() != null ? EnemyCombatState.AGGRO : EnemyCombatState.IDLE);
        }
    }

    // ---------------------------------------------------------- ciclo de vida

    /**
     * Morrer PUBLICA o repouso.
     *
     * <p>Depois de morto o passo de IA nao roda mais, e o campo sincronizado fica
     * onde estava: o bicho morreria com a pose do golpe travada no cliente. Nao ha
     * erro nisso -- so um cadaver de boca aberta.</p>
     */
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
     * unload mais outro de dimensao e como um deles fica esquecido -- e o
     * esquecido nao da erro: deixa o bando contando um lobo que nao existe mais,
     * ocupando um lugar do teto para sempre, e o reforco que deveria chegar e
     * recusado com {@code BANDO_CHEIO} num bando de dois.</p>
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
    }

    // ------------------------------------------------------------- animacao

    @Override
    public void registerControllers(
            software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<WolfPackHunterEntity>(
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

    // Os nomes sao um CONTRATO com wolf_pack_hunter.animation.json. Errar um deles nao da erro:
    // o GeckoLib nao acha o clipe e deixa o osso parado. LoopType.DEFAULT em todos --
    // o tipo de repeticao mora no arquivo, e cravar em Java o faria vencer o JSON.
    private static final software.bernie.geckolib.animation.RawAnimation IDLE =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.wolf_pack_hunter.idle", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation WALK =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.wolf_pack_hunter.walk", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation WINDUP =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.wolf_pack_hunter.windup", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation STRIKE =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.wolf_pack_hunter.strike", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation RECOVERY =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.wolf_pack_hunter.recovery", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation STAGGER =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.wolf_pack_hunter.stagger", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    private static final software.bernie.geckolib.animation.RawAnimation DEATH =
            software.bernie.geckolib.animation.RawAnimation.begin()
                    .then("animation.wolf_pack_hunter.death", software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);

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
     * Leva o lobo ao posto que a coordenacao escolheu -- cerco ou fuga.
     *
     * <p>Ela nao DECIDE nada: le {@code decisao} e {@code destino}, que a
     * coordenacao escreveu. Uma Goal que recalculasse o posto por conta propria
     * seria a segunda autoridade sobre a mesma pergunta, e as duas divergiriam no
     * tick seguinte a uma promocao.</p>
     */
    private static final class CercoGoal extends Goal {
        private final WolfPackHunterEntity lobo;

        private CercoGoal(WolfPackHunterEntity lobo) {
            this.lobo = lobo;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (lobo.level().isClientSide || lobo.destino == null) return false;
            if (lobo.runtimeExigido().stagger().cambaleando()) return false;
            return lobo.decisao == DecisaoDeMatilha.CERCAR
                    || lobo.decisao == DecisaoDeMatilha.RECUAR;
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public void start() { lobo.destinoPendente = true; }

        @Override public void tick() {
            // O caminho so e refeito quando o posto MUDOU. Chamar moveTo todo tick
            // manda o pathfinder recalcular vinte vezes por segundo por lobo: com
            // quatro deles isso e custo puro, e o sintoma nao e erro -- e TPS
            // caindo enquanto o bando esta em tela.
            if (lobo.destinoPendente && lobo.destino != null) {
                double velocidade = lobo.decisao == DecisaoDeMatilha.RECUAR
                        ? VELOCIDADE_DE_RECUO : VELOCIDADE_DE_CERCO;
                lobo.getNavigation().moveTo(lobo.destino.x, lobo.destino.y, lobo.destino.z,
                        velocidade);
                lobo.destinoPendente = false;
            }
            // Cercar e olhar para o alvo enquanto se anda de lado: e o unico sinal
            // que diz ao jogador que aquele lobo continua no encontro, e nao indo
            // embora. Recuando, o olhar fica solto -- quem foge nao encara.
            LivingEntity alvo = lobo.getTarget();
            if (alvo != null && lobo.decisao == DecisaoDeMatilha.CERCAR) {
                lobo.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            }
        }

        @Override public void stop() {
            lobo.getNavigation().stop();
            lobo.destinoPendente = false;
        }
    }

    /** Dispara e conduz a mordida; quem mede a janela e o {@link AttackController}. */
    private static final class MordidaGoal extends Goal {
        private final WolfPackHunterEntity lobo;

        private MordidaGoal(WolfPackHunterEntity lobo) {
            this.lobo = lobo;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (lobo.level().isClientSide) return false;
            if (lobo.decisao != DecisaoDeMatilha.INVESTIR) return false;
            EnemyRuntime runtime = lobo.runtimeExigido();
            if (runtime.stagger().cambaleando()) return false;
            LivingEntity alvo = lobo.getTarget();
            if (alvo == null || !alvo.isAlive()) return false;
            return lobo.distanceTo(alvo) <= WolfPackHunterTuning.ALCANCE_DA_MORDIDA
                    && lobo.hasLineOfSight(alvo)
                    && runtime.ataques().canStart();
        }

        @Override public boolean canContinueToUse() {
            // A mordida continua ate a fase acabar, MESMO que a coordenacao ja
            // tenha mudado de ideia. Cortar o golpe no meio porque o bando mandou
            // cercar deixaria o jogador vendo um ataque que some -- e o servidor ja
            // cobrou o windup inteiro por ele.
            AttackPhase fase = lobo.runtimeExigido().ataques().phase();
            return fase != AttackPhase.IDLE && fase != AttackPhase.COMPLETE
                    && !lobo.runtimeExigido().stagger().cambaleando();
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() {
            lobo.getNavigation().stop();
            lobo.destinoPendente = false;
            lobo.runtimeExigido().ataques().start(WolfPackHunterTuning.mordida());
        }

        @Override public void tick() {
            LivingEntity alvo = lobo.getTarget();
            // Encarar o alvo so ate o fim do WINDUP: a partir da janela ACTIVE a
            // direcao esta TRAVADA. Sem travar, a mordida viraria mira-laser -- o
            // lobo giraria junto com quem desvia, e num cerco de quatro isso
            // apagaria a unica defesa que sobra, que e sair de perto.
            AttackPhase fase = lobo.runtimeExigido().ataques().phase();
            if (alvo != null && fase == AttackPhase.WINDUP) {
                lobo.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            }
            // Freia no chao fora da janela ativa. O impulso do salto e aplicado uma
            // unica vez, em aplicarSalto; sem o freio, ele se somaria a inercia da
            // corrida e o lobo passaria direto pelo alvo com a caixa junto.
            if (fase != AttackPhase.ACTIVE) {
                lobo.setDeltaMovement(lobo.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
            }
        }

        @Override public void stop() { lobo.getNavigation().stop(); }
    }
}
