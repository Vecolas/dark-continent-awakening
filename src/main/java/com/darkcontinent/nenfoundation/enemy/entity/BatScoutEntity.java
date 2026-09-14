package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.DecisaoDeVoo;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeRelatorioDeBatedor;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.base.BaseChimeraAnt;
import com.darkcontinent.nenfoundation.enemy.base.EnemyRuntime;
import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraColonySavedData;
import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraTrait;
import com.darkcontinent.nenfoundation.enemy.combat.AttackController;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHit;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import com.darkcontinent.nenfoundation.enemy.content.BatScoutTuning;
import com.darkcontinent.nenfoundation.enemy.content.ChimeraProfiles;
import com.darkcontinent.nenfoundation.enemy.faction.FactionRelations;
import com.darkcontinent.nenfoundation.enemy.perception.HearingEvent;
import com.darkcontinent.nenfoundation.enemy.perception.OuvidoDeInimigo;
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
import net.minecraft.server.MinecraftServer;
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
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Bat Scout -- formiga quimera, rank PEON. O bicho que AVISA.
 *
 * <p>Dano 3 de proposito: ele nao e ameaca, e o olho da colonia. O alcance 32 e
 * maior que o de qualquer peon porque ver primeiro E a funcao; matar o batedor
 * antes do relatorio e a resposta que ele ensina.</p>
 *
 * <p><b>O que ele faz, em ordem.</b> Ele percebe com orcamento, como todo inimigo
 * desta fundacao. Ao reconhecer um alvo ele nao avanca: ele SOBE, abre distancia
 * e, passados {@link BatScoutTuning#TICKS_DE_OBSERVACAO} ticks com alvo, RELATA.
 * Depois disso continua fugindo e volta a relatar a cada
 * {@link BatScoutTuning#INTERVALO_ENTRE_RELATORIOS} ticks. A mordida existe, e so
 * existe encurralado -- ver {@code RegrasDeVooDeBatedor}, onde essa ordem mora
 * inteira. Um batedor que persegue seria um mob de combate de 3 de dano, ou seja
 * um inseto chato, e o encontro deixaria de ser sobre calar o mensageiro.</p>
 *
 * <p><b>O relatorio tem DOIS destinos, e nenhum deles sai do servidor.</b> Ele vai
 * para a COLONIA ({@code ChimeraColony.relatar}, com prazo) e para os vizinhos que
 * declararam ouvido, como {@link HearingEvent}. Nao ha payload novo, nao ha campo
 * sincronizado novo, o cliente nao fica sabendo de nada: informacao escondida que
 * viaja ate o cliente para ele filtrar ja esta vazada.</p>
 *
 * <p><b>Por que som, e nao alvo.</b> Um {@code setTarget} direto no vizinho seria
 * uma segunda autoridade sobre a escolha de alvo, discordando do
 * {@link TargetEvaluator} em silencio. Como som, o aviso passa pelo alcance de
 * audicao de QUEM OUVE: um mob longe demais do jogador escuta e nao aprende nada.
 * O batedor aponta; ele nao decide pelos outros. Este e o mesmo transporte do
 * Radio Rat, reusado inteiro -- o que muda e a CADENCIA, e a razao esta escrita em
 * {@link RegrasDeRelatorioDeBatedor}.</p>
 *
 * <p><b>A conta do custo.</b> A varredura de vizinhos e cara, e por isso ela so
 * roda quando a regra autoriza -- no melhor caso uma vez a cada seis segundos --
 * e entrega a no maximo {@link BatScoutTuning#MAXIMO_DE_VIZINHOS_POR_AVISO}
 * vizinhos. Varrer todo tick nao daria erro nenhum: daria TPS caindo devagar numa
 * colonia com muitos batedores.</p>
 *
 * <p><b>NIGHT_VISION deixa de ser palavra aqui.</b> O trait e garantido pelo molde
 * e {@code RegrasDeVisaoNoturna} o transforma no unico fato que importa: o alcance
 * de percepcao deste bicho NAO cai no escuro. O raio da varredura de mundo e
 * recalculado por tick a partir da luz medida no bloco dele -- e e a varredura, e
 * nao o cone, que encurta, porque reconstruir um {@link VisionCone} a cada tick
 * criaria uma segunda autoridade sobre o mesmo alcance. O cone e o TETO; o escuro
 * so encurta o que chega ate ele.</p>
 *
 * <p><b>Interromper funciona, e e a unica recompensa de alcanca-lo.</b> Um golpe
 * solido dispara o stagger, que corta a mordida, impoe
 * {@link BatScoutTuning#RECARGA_APOS_INTERRUPCAO} e, num bicho que voa, tira o
 * CONTROLE DO VOO ({@code MovimentoDeVoo.CAIR}). Com 16 de vida e armadura 0,
 * matar tambem e uma resposta. As duas dizem a mesma frase: o mensageiro tem de
 * ser tratado antes do aviso.</p>
 */
public final class BatScoutEntity extends BaseChimeraAnt
        implements software.bernie.geckolib.animatable.GeoEntity, OuvidoDeInimigo {

    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(BatScoutEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CAMBALEANDO =
            SynchedEntityData.defineId(BatScoutEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int MEMORIA_DE_ALVO_TICKS = 140;
    private static final int TICKS_DE_AVISO = 20;
    private static final double ABERTURA_DA_VISAO = 80.0D;
    private static final double ALCANCE_DE_AUDICAO = 16.0D;
    private static final float FRACAO_DE_VIDA_CRITICA = 0.25F;

    /**
     * Multiplicador que o controle de voo aplica sobre a velocidade do perfil.
     *
     * <p>LIMITE DE DESIGN, e nao botao de tuning: ele diz "ele usa tudo que tem", e
     * so isso. A velocidade do bicho mora em {@code ChimeraProfiles.batScout()};
     * girar os dois lados daria duas fontes para a mesma verdade, e a sessao de
     * balanceamento mexeria numa e seria vencida pela outra.</p>
     */
    private static final double VELOCIDADE_DE_VOO = 1.0D;

    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache cacheDeAnimacao =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    /**
     * Ha quantos ticks seguidos existe alvo. Ele PARA de crescer no limiar.
     *
     * <p>Parar no limiar nao e economia: e o que impede um contador de tick de
     * servidor virar um inteiro que estoura depois de tempo suficiente ligado. Um
     * overflow aqui vira numero negativo, a comparacao do limiar passa a ser falsa
     * e o batedor para de relatar -- para sempre, sem uma linha de log.</p>
     */
    private int ticksComAlvo;

    /**
     * Ticks desde o ultimo relatorio que de fato saiu. Nasce ARMADO.
     *
     * <p>Comecar em zero faria o PRIMEIRO relatorio esperar o intervalo inteiro em
     * vez da observacao, e um batedor recem-nascido ficaria seis segundos olhando
     * para o jogador sem contar nada. Isso nao da erro nenhum -- da um bicho que
     * parece quebrado exatamente na primeira vez que alguem o encontra.</p>
     *
     * <p>Ele tambem para de crescer no teto, pelo mesmo motivo de
     * {@link #ticksComAlvo}.</p>
     */
    private int ticksDesdeOUltimoRelatorio = BatScoutTuning.INTERVALO_ENTRE_RELATORIOS;

    public BatScoutEntity(EntityType<? extends BatScoutEntity> type, Level level) {
        super(type, level, ChimeraProfiles.batScout().metadata(),
                new AwarenessTuning(TICKS_DE_AVISO, MEMORIA_DE_ALVO_TICKS),
                ChimeraProfiles.batScoutMolde());
        // Controle de VOO, e nao o MoveControl de chao que o Mob instala. Com o
        // controle padrao o batedor tentaria ANDAR ate o destino: ele exige chao
        // debaixo do caminho, e o bicho ficaria preso na primeira arvore com a
        // navegacao dizendo que chegou. Nao da erro; da um morcego que caminha.
        this.moveControl = new FlyingMoveControl(this, 20, true);
        instalarRuntime(montarRuntime());
    }

    private EnemyRuntime montarRuntime() {
        var perfil = ChimeraProfiles.batScout();
        PerceptionController percepcao = new PerceptionController(
                PerceptionBudget.padrao(),
                // O cone e o TETO do alcance e nao muda com a luz. Quem encurta no
                // escuro e a varredura (ver alcanceDePercepcao): um cone novo por
                // tick alocaria por tick e, pior, seria uma segunda autoridade sobre
                // o mesmo numero -- as duas discordariam em silencio.
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
                new AttackController(ChimeraProfiles.batScoutRecarga()),
                new StaggerState(ChimeraProfiles.batScoutStagger()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = ChimeraProfiles.batScout().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                // FlyingMoveControl le este atributo a cada tick de voo.
                .add(Attributes.FLYING_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<BatScoutEntity> registeredType() { return EnemyEntityTypes.BAT_SCOUT.get(); }

    /** Navegacao de voo: o caminho nao precisa de chao debaixo dele. */
    @Override
    protected PathNavigation createNavigation(Level nivel) {
        FlyingPathNavigation navegacao = new FlyingPathNavigation(this, nivel);
        navegacao.setCanOpenDoors(false);
        navegacao.setCanPassDoors(true);
        // Boiar em vez de afundar: um batedor que cai na agua e nao boia morre
        // afogado, e o sintoma e uma colonia que perde batedores sem que nada no
        // log explique o desaparecimento.
        navegacao.setCanFloat(true);
        return navegacao;
    }

    /**
     * Queda nao machuca este bicho.
     *
     * <p>A punicao do cambaleio ja e CAIR, e ela e desenhada: o clipe de stagger
     * fecha a asa e o corpo desce. Somar dano de queda por cima transformaria
     * "interrompi" em "matei" sempre que houvesse altura -- e a diferenca entre as
     * duas e justamente o que este encontro ensina. Nao daria erro: daria um
     * batedor que as vezes morre de um tiro e as vezes de dois, dependendo de onde
     * ele estava voando.</p>
     */
    @Override
    public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fonte) {
        return false;
    }

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

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new VooDeBatedorGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Sem target goal vanilla: quem escolhe alvo e o TargetEvaluator, por
        // faccao. Um NearestAttackableTargetGoal ao lado seria uma segunda
        // autoridade sobre a mesma decisao, e as duas discordariam em silencio.
        //
        // E NENHUMA goal de perseguicao, tambem de proposito: a unica goal de
        // combate deste bicho e a de VOO, e ela so morde encurralada.
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;
        EnemyRuntime runtime = runtimeExigido();
        PerceptionSnapshot snapshot = runtime.tick(tickCount, this::varrerCandidatos,
                getHealth() <= getMaxHealth() * FRACAO_DE_VIDA_CRITICA, false);
        aplicarAlvo(snapshot);
        tickDoRelatorio(runtime);
        if (runtime.ataques().phase() == AttackPhase.ACTIVE) {
            LivingEntity alvo = getTarget();
            if (alvo != null && alvo.isAlive()) aplicarMordida(runtime, alvo);
        }
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

    // ------------------------------------------------------- visao no escuro

    /**
     * O alcance que este batedor TEM AGORA -- e ele nao cai no escuro.
     *
     * <p>A regra e pura e mora em {@code RegrasDeVisaoNoturna}; aqui so se mede o
     * que ela pede: o alcance do perfil, a luz do bloco e o trait. Identidade vazia
     * (primeiro tick de servidor, entidade recem-criada) conta como SEM o trait, e
     * isso e conservador de proposito -- deduzir o trait pelo tipo da entidade
     * seria uma segunda fonte para uma verdade que ja mora na identidade sorteada,
     * e as duas divergiriam no dia em que o molde mudasse.</p>
     */
    private double alcanceDePercepcao() {
        boolean visaoNoturna = identidade()
                .map(id -> id.traits().contains(ChimeraTrait.NIGHT_VISION))
                .orElse(false);
        return BatScoutTuning.visaoNoturna().alcanceEfetivo(
                getAttributeValue(Attributes.FOLLOW_RANGE),
                level().getMaxLocalRawBrightness(blockPosition()),
                visaoNoturna);
    }

    /** Varredura de mundo da PERCEPCAO -- a unica, e so quando o orcamento autoriza. */
    private List<TargetCandidate> varrerCandidatos() {
        double alcance = alcanceDePercepcao();
        AABB area = getBoundingBox().inflate(alcance);
        List<TargetCandidate> candidatos = new ArrayList<>();
        for (Player jogador : level().getEntitiesOfClass(Player.class, area,
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative())) {
            // A AABB e o filtro barato; o alcance efetivo e a REGRA. Sem esta linha
            // o batedor enxergaria ate os cantos da caixa, que ficam 1.7x mais longe
            // que a face dela -- e a perda de alcance no escuro deixaria de existir
            // em quatro direcoes, sem nada acusar.
            double distancia = distanceTo(jogador);
            if (distancia > alcance) continue;
            candidatos.add(new TargetCandidate(jogador.getUUID(),
                    com.darkcontinent.nenfoundation.enemy.api.EnemyFaction.HUNTER_ASSOCIATION,
                    distancia, cossenoDoOlharAte(jogador), hasLineOfSight(jogador),
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

    // ----------------------------------------------------------- o relatorio

    /** Conta as duas janelas e dispara o relatorio quando a regra autoriza. */
    private void tickDoRelatorio(EnemyRuntime runtime) {
        RegrasDeRelatorioDeBatedor regras = BatScoutTuning.relatorio();
        LivingEntity alvo = getTarget();
        boolean temAlvo = alvo != null && alvo.isAlive();
        if (!temAlvo) {
            ticksComAlvo = 0;
        } else if (ticksComAlvo < regras.ticksDeObservacao()) {
            ticksComAlvo++;
        }
        if (ticksDesdeOUltimoRelatorio < regras.intervaloEntreRelatorios()) {
            ticksDesdeOUltimoRelatorio++;
        }
        // As quatro condicoes vao JUNTAS para a regra, e nao uma em cada if daqui:
        // espalhadas, a que sobrasse em algum caminho produziria um batedor que
        // relata cambaleando, ou que relata todo tick -- e nenhum dos dois da erro.
        if (!regras.relata(temAlvo, runtime.stagger().cambaleando(), ticksComAlvo,
                ticksDesdeOUltimoRelatorio)) {
            return;
        }
        ticksDesdeOUltimoRelatorio = 0;
        UUID denunciado = alvo.getUUID();
        relatarAColonia(denunciado, regras);
        avisarVizinhos(denunciado, alvo, regras);
    }

    /**
     * O relatorio que alimenta a COLONIA -- com prazo, e so se ele tiver colonia.
     *
     * <p>Formiga orfa nao relata para ninguem, e isso e resposta e nao falha: ela
     * existe (spawn de dev, colonia destruida) e nao pertence a lugar nenhum.
     * Inventar uma colonia aqui faria a orfa alimentar uma colonia fantasma, que e
     * exatamente o que {@code ChimeraIdentity.orfa()} existe para distinguir.</p>
     */
    private void relatarAColonia(UUID alvo, RegrasDeRelatorioDeBatedor regras) {
        Optional<UUID> id = colonia();
        if (id.isEmpty()) return;
        MinecraftServer servidor = level().getServer();
        if (servidor == null) return;
        ChimeraColonySavedData dados = ChimeraColonySavedData.de(servidor);
        dados.colonia(id.get()).ifPresent(colonia -> {
            colonia.relatar(alvo, level().getGameTime(), regras.duracaoNaColonia());
            // setDirty no MESMO ponto da escrita: o save so grava o que foi marcado,
            // e um relatorio nao marcado sobrevive ate o restart e some depois --
            // sem erro, e com a colonia esquecendo a ameaca justamente quando o
            // servidor volta.
            dados.setDirty();
        });
    }

    /**
     * O aviso acustico: uma varredura, com TETO de entregas, e nada sai do servidor.
     *
     * <p>A distancia que viaja em cada evento e a do VIZINHO ao alvo -- medida
     * aqui, entidade por entidade. Mandar a distancia do BATEDOR ao alvo seria mais
     * barato e encheria a memoria de ameaca dos vizinhos com um numero que nao
     * descreve a situacao deles: um mob do outro lado do vale acharia o jogador a
     * dois blocos, e nada acusaria isso.</p>
     *
     * <p><b>O teto corta pela ordem da lista, e nao pela distancia.</b> Isso e
     * assumido: ordenar os vizinhos por distancia custaria mais do que o teto
     * economiza, e o raio -- que e a regra -- ja foi cobrado ANTES do corte. O que
     * o teto garante e que o custo do aviso nao cresca com a populacao do ninho, e
     * e esse crescimento, e nao o raio, que derruba o TPS de um ninho cheio.</p>
     */
    private void avisarVizinhos(UUID alvo, LivingEntity alvoVivo,
            RegrasDeRelatorioDeBatedor regras) {
        AABB area = getBoundingBox().inflate(regras.raioDoAviso());
        int entregues = 0;
        for (Mob vizinho : level().getEntitiesOfClass(Mob.class, area,
                outro -> outro != this && outro.isAlive() && outro instanceof OuvidoDeInimigo)) {
            if (entregues >= BatScoutTuning.MAXIMO_DE_VIZINHOS_POR_AVISO) break;
            // A AABB e o filtro barato; o raio redondo e a REGRA. Sem esta linha o
            // aviso alcancaria os cantos da caixa, que ficam 1.7x mais longe que a
            // face dela -- alcance maior que o numero declarado, e so em quatro
            // direcoes. Isso nao da erro: da um raio que ninguem consegue medir.
            if (!regras.alcanca(distanceTo(vizinho))) continue;
            Optional<HearingEvent> evento = regras.aviso(alvo, vizinho.distanceTo(alvoVivo));
            if (evento.isEmpty()) continue;
            ((OuvidoDeInimigo) vizinho).ouvir(evento.get());
            entregues++;
        }
    }

    /** Som de mundo chega ao batedor como EVENTO: e assim que um batedor avisa o outro. */
    @Override
    public void ouvir(HearingEvent evento) {
        if (!level().isClientSide) runtimeExigido().percepcao().ouvir(evento);
    }

    // ----------------------------------------------------------------- o voo

    /** Distancia no PLANO ate o alvo; a altura e decidida separadamente, e de proposito. */
    private double distanciaHorizontalAte(LivingEntity alvo) {
        double dx = getX() - alvo.getX();
        double dz = getZ() - alvo.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Ele esta sem para onde subir?
     *
     * <p>Este e o UNICO fato que liga a mordida ao mundo, e ele e medido aqui
     * porque a regra de voo nao conhece bloco nenhum. Teto logo acima significa
     * caverna, tunel de ninho ou um canto onde o jogador o prendeu -- e e so ai que
     * os 3 de dano existem. Medir "encurralado" por vida baixa, que seria mais
     * barato, daria um batedor que ataca justamente quando deveria sumir.</p>
     */
    private boolean semSaidaParaCima() {
        return !level().noCollision(this,
                getBoundingBox().move(0.0D, BatScoutTuning.ALTURA_LIVRE_PARA_FUGIR, 0.0D));
    }

    /** Ponto de fuga: para longe do alvo no plano, e mais alto. */
    private Vec3 pontoDeFuga(LivingEntity alvo, double ganhoDeAltura) {
        Vec3 fuga = new Vec3(getX() - alvo.getX(), 0.0D, getZ() - alvo.getZ());
        // Alvo exatamente embaixo: nao ha direcao horizontal de fuga, e a unica
        // saida e subir. Normalizar um vetor nulo devolve NaN, e NaN numa posicao de
        // destino nao da erro -- faz o mob PARAR de se mover, e ninguem consegue
        // dizer por que.
        Vec3 direcao = fuga.lengthSqr() < 1.0E-6D ? Vec3.ZERO : fuga.normalize();
        return position().add(direcao.scale(BatScoutTuning.DISTANCIA_DE_FUGA))
                .add(0.0D, ganhoDeAltura, 0.0D);
    }

    private void voarPara(Vec3 destino) {
        getMoveControl().setWantedPosition(destino.x, destino.y, destino.z, VELOCIDADE_DE_VOO);
    }

    /**
     * Comeca a mordida, se a recarga deixar.
     *
     * <p>O dano e LIDO AGORA, do atributo, e nao guardado em lugar nenhum: congelar
     * um multiplicador na ativacao e o primeiro erro da lista de falhas silenciosas
     * deste projeto.</p>
     */
    private void morder(EnemyRuntime runtime) {
        if (!runtime.ataques().canStart()) return;
        getNavigation().stop();
        runtime.ataques().start(BatScoutTuning.mordida(
                (float) getAttributeValue(Attributes.ATTACK_DAMAGE)));
    }

    /** A mordida so existe dentro da janela ACTIVE; fora dela ela nao e nada. */
    private void aplicarMordida(EnemyRuntime runtime, LivingEntity alvo) {
        Optional<AttackHit> acerto = runtime.ataques().tryHit(alvo.getId(), alvo.getBoundingBox(),
                BatScoutTuning.caixaDaMordida(), position(), getYRot(), "body");
        acerto.ifPresent(hit -> {
            alvo.hurt(damageSources().mobAttack(this), hit.damage());
            alvo.knockback(BatScoutTuning.REPUXAO_DA_MORDIDA,
                    getX() - alvo.getX(), getZ() - alvo.getZ());
        });
    }

    /**
     * UNICO ponto que alimenta o stagger deste bicho -- e o unico que o faz saltar.
     *
     * <p>Sem ponto fraco e sem multiplicador: o batedor nao tem regiao que paga
     * mais, e inventar uma aqui criaria um numero que nenhum perfil declara. O que
     * importa e que o golpe chegue ao acumulador -- e o acumulador, com o limiar
     * baixo que {@code ChimeraProfiles.batScoutStagger()} declara, tira o voo dele
     * na primeira pancada de verdade.</p>
     *
     * <p>Dano sem atacante (fogo, queda, {@code /kill}) nao acumula stagger nem
     * produz recuo: cambalear por queimadura transformaria fogo em interrupcao
     * permanente, e o batedor nunca mais voaria sem que nada explicasse por que.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean levou = super.hurt(source, amount);
        if (level().isClientSide || !levou || !isAlive()) return levou;
        if (!(source.getEntity() instanceof LivingEntity) || !Float.isFinite(amount)
                || amount <= 0.0F) {
            return true;
        }
        EnemyRuntime runtime = runtimeExigido();
        StaggerResult resultado = runtime.sofrerStagger(runtime.ataques().attackInstanceId(),
                amount, BatScoutTuning.RECARGA_APOS_INTERRUPCAO);
        if (resultado == StaggerResult.DISPAROU) {
            getNavigation().stop();
            this.entityData.set(CAMBALEANDO, true);
            this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        }
        // O recuo vem DEPOIS do stagger e le o estado que o stagger acabou de
        // escrever: quem foi interrompido nao salta. Invertido, o batedor que
        // acabou de perder o voo ainda ganharia o impulso, e a diferenca entre
        // "acertei" e "acertei o bastante" sumiria da tela.
        double recuo = BatScoutTuning.voo().recuoAoLevarDano(isAlive(),
                runtime.stagger().cambaleando());
        if (recuo > 0.0D) {
            setDeltaMovement(getDeltaMovement().add(0.0D, recuo, 0.0D));
            this.hasImpulse = true;
        }
        return true;
    }

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
        return switch (runtime.ataques().phase()) {
            case WINDUP -> EnemyCombatState.WINDUP;
            case ACTIVE -> EnemyCombatState.ACTIVE;
            case RECOVERY -> EnemyCombatState.RECOVERY;
            default -> getTarget() != null ? EnemyCombatState.AGGRO : EnemyCombatState.IDLE;
        };
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
     * Repouso publicado E contadores restaurados, no MESMO ponto.
     *
     * <p>Os dois contadores deste bicho nascem aqui na entidade, e nao dentro do
     * runtime, entao {@code EnemyRuntime.limpar()} nao sabe deles. Deixa-los de
     * fora e a versao local do erro que este projeto ja sabe que comete -- limpeza
     * espalhada pelos pontos de saida, com um deles esquecido. Quem liga, desliga,
     * e o par mora no ciclo de vida de quem ligou.</p>
     */
    private void publicarRepouso() {
        this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        this.entityData.set(CAMBALEANDO, false);
        this.ticksComAlvo = 0;
        // Volta ARMADO, e nao zerado: zerar faria o batedor calar por seis segundos
        // sem motivo se esta entidade voltar a ser usada, e "zerar tudo" parece o
        // certo justamente por isso.
        this.ticksDesdeOUltimoRelatorio = BatScoutTuning.INTERVALO_ENTRE_RELATORIOS;
    }

    // ------------------------------------------------------------- animacao

    @Override
    public void registerControllers(
            software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<BatScoutEntity>(
                this, "corpo", 4, estado -> {
                    if (this.deathTime > 0) return estado.setAndContinue(DEATH);
                    if (cambaleando()) return estado.setAndContinue(STAGGER);
                    return switch (faseDeAtaque()) {
                        case WINDUP -> estado.setAndContinue(WINDUP);
                        case ACTIVE -> estado.setAndContinue(STRIKE);
                        case RECOVERY -> estado.setAndContinue(RECOVERY);
                        default -> estado.setAndContinue(voando() ? WALK : IDLE);
                    };
                }));
    }

    // Contrato com bat_scout.animation.json. LoopType.DEFAULT em todos: o tipo de
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
                .then("animation.bat_scout." + nome,
                        software.bernie.geckolib.animation.Animation.LoopType.DEFAULT);
    }

    /** Deslocamento por tick acima do qual o bicho conta como em movimento. */
    private static final double LIMIAR_DE_VOO = 0.02D;

    /**
     * Voando: no AR, ou se deslocando.
     *
     * <p>Este teste e diferente do dos mobs de chao de proposito, e a diferenca e a
     * ARTE. {@code walk} desenha a asa ABERTA e {@code idle} a desenha dobrada;
     * usar so a velocidade horizontal -- que e o teste dos outros bichos -- poria o
     * batedor PAIRADO com a asa fechada, e asa fechada no ar le como bicho caindo.
     * Nao da erro nenhum: da um morcego que parece morto no meio do voo.</p>
     */
    private boolean voando() {
        return !onGround() || velocidadeHorizontal() >= LIMIAR_DE_VOO;
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

    // ------------------------------------------------------------------ goal

    /**
     * Conduz o voo: sobe, afasta, e so morde encurralado.
     *
     * <p>{@code static} de proposito: todo estado que ela le mora na ENTIDADE. Um
     * campo aqui seria compartilhado por todos os batedores da colonia, e o sintoma
     * seria um deles fugindo do alvo de outro.</p>
     *
     * <p><b>Ela nao decide nada.</b> Quem decide e {@code RegrasDeVooDeBatedor}, que
     * roda sem mundo e sem entidade. Esta goal mede os fatos e executa a resposta --
     * e e essa fronteira que permite o comportamento inteiro deste mob ser testado
     * sem servidor.</p>
     */
    private static final class VooDeBatedorGoal extends Goal {
        private final BatScoutEntity batedor;

        private VooDeBatedorGoal(BatScoutEntity batedor) {
            this.batedor = batedor;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (batedor.level().isClientSide) return false;
            LivingEntity alvo = batedor.getTarget();
            return alvo != null && alvo.isAlive();
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void stop() { batedor.getNavigation().stop(); }

        @Override public void tick() {
            LivingEntity alvo = batedor.getTarget();
            if (alvo == null) return;
            EnemyRuntime runtime = batedor.runtimeExigido();
            AttackPhase fase = runtime.ataques().phase();

            if (fase != AttackPhase.IDLE && fase != AttackPhase.COMPLETE) {
                // Golpe em curso: o voo TRAVA. Sem esta trava a regra mandaria
                // afastar no meio da propria mordida -- o morcego recuaria enquanto
                // a animacao do golpe toca, e o jogador veria dano saindo de um
                // bicho que ja estava indo embora.
                //
                // Encarar o alvo so ate o fim do WINDUP. Depois disso a direcao fica
                // TRAVADA: sem travar, a mordida viraria mira-laser e o desvio de
                // quem se afasta no ultimo instante deixaria de existir.
                if (fase == AttackPhase.WINDUP) {
                    batedor.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
                }
                batedor.voarPara(batedor.position());
                return;
            }

            DecisaoDeVoo decisao = BatScoutTuning.voo().decidir(
                    batedor.distanciaHorizontalAte(alvo),
                    batedor.getY() - alvo.getY(),
                    batedor.semSaidaParaCima(),
                    runtime.stagger().cambaleando());

            if (decisao.controlaOVoo()) {
                batedor.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            }

            switch (decisao.movimento()) {
                // CAIR e a unica saida em que ele nao escolhe destino nenhum: quem
                // cambaleia no ar perde o voo, e e essa a recompensa de acerta-lo.
                case CAIR -> batedor.getNavigation().stop();
                case MORDER -> batedor.morder(runtime);
                case AFASTAR -> batedor.voarPara(
                        batedor.pontoDeFuga(alvo, decisao.ganhoDeAltura()));
                case SUBIR, PAIRAR -> batedor.voarPara(
                        batedor.position().add(0.0D, decisao.ganhoDeAltura(), 0.0D));
            }
        }
    }
}
