package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessInput;
import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.NestGuardRules;
import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.base.BaseHxHMob;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.AttackTimeline;
import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Spider eagle: a ameaca e o NINHO, nao o caminho. A ave nao caca ninguem --
 * ela defende um lugar, e a resposta do jogador e RECUAR.
 *
 * <p>DECISAO CENTRAL QUE ESTE ARQUIVO CARREGA: quem recua e poupado. Toda
 * medida que decide comportamento sai do NINHO, nunca do corpo da ave, e a
 * {@link NestGuardRules#desiste} devolve a ave para casa assim que o intruso
 * sai do raio de aviso ou assim que ela mesma passa da coleira. E isso, e so
 * isso, que vai permitir -- quando existir um bloco de ninho, em outro PR --
 * roubar os ovos sem matar a mae. Um mob que persegue ate matar nao tem esse
 * encounter, e a diferenca nao aparece como erro: aparece como uma promessa de
 * design que o codigo silenciosamente nao cumpre.</p>
 *
 * <p>SEGUNDA DECISAO: o NINHO E PERSISTENTE, e e a unica coisa deste mob que
 * sobrevive ao save. Sem grava-lo, recarregar o mundo re-ancoraria o ninho onde
 * a ave estivesse voando naquele instante, e a colonia inteira migraria sozinha
 * ao longo das sessoes -- sem erro nenhum no log.</p>
 *
 * <p>Tudo que decide -- quem e intruso, quando o aviso comeca, quando o
 * mergulho dispara e quando a ave desiste -- roda no servidor. Os dois campos
 * sincronizados carregam apenas AVISANDO e a FASE do ataque, que e o que o
 * cliente precisa para desenhar o voo de aviso e o mergulho. O cliente nunca
 * informa alvo nem acerto.</p>
 */
public final class SpiderEagleEntity extends BaseHxHMob {
    /** Avisando ou nao; o cliente precisa saber para desenhar o voo de aviso. */
    private static final EntityDataAccessor<Boolean> AVISANDO =
            SynchedEntityData.defineId(SpiderEagleEntity.class, EntityDataSerializers.BOOLEAN);
    /** Fase do ataque; o unico estado de combate que o cliente precisa conhecer. */
    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(SpiderEagleEntity.class, EntityDataSerializers.INT);

    private static final AttackDefinition MERGULHO = HunterExamProfiles.spiderEagleDive();
    private static final NestGuardRules NINHO = HunterExamProfiles.spiderEagleNest();

    /** Chave NBT do ninho; muda-la aposenta os ninhos de todos os mundos salvos. */
    private static final String CHAVE_DO_NINHO = "nest_pos";

    /** Quanto tempo a ave lembra de um intruso que saiu da linha de visao. */
    private static final int MEMORIA_DE_ALVO_TICKS = 100;
    /** Afastar-se do NINHO mais do que isto num tick conta como recuo. */
    private static final double TOLERANCIA_DE_RECUO = 0.35D;
    /** Fracao da vida abaixo da qual o cerebro pode decidir fugir. */
    private static final float FRACAO_DE_VIDA_CRITICA = 0.25F;
    /**
     * Quanto a ave sobe no telegrafo, medido a partir do alvo.
     *
     * <p>DERIVADO do raio de bote de proposito: subir mais alto do que o
     * mergulho alcanca produz uma ave que sobe, desce e para no ar antes de
     * chegar -- e isso nao da erro nenhum, aparece como um ataque que nunca
     * acerta. A metade e limite de design, nao botao de tuning: e a altura que
     * deixa a sombra visivel sem tirar a ave do alcance da propria descida.</p>
     */
    private static final double ALTURA_DO_TELEGRAFO = NINHO.raioDeBote() * 0.5D;
    /**
     * Blocos por tick da descida, DERIVADOS da geometria -- nao e um numero
     * proprio.
     *
     * <p>A pior descida possivel e a diagonal entre a altura do telegrafo e a
     * borda do raio de bote, e ela precisa caber na janela ACTIVE do mergulho.
     * Escrever uma velocidade solta aqui criaria a divergencia classica: girar
     * o raio de bote no perfil deixaria de mudar o alcance real do ataque.</p>
     */
    private static final double VELOCIDADE_DO_MERGULHO =
            Math.hypot(NINHO.raioDeBote(), ALTURA_DO_TELEGRAFO) / MERGULHO.activeTicks();
    /** Folga da caixa usada para achar quem a descida pegou. */
    private static final double FOLGA_DO_MERGULHO = 0.35D;
    /**
     * Duracao da "web disruption" no alvo atingido.
     *
     * <p>Nao e botao de balanceamento: e o teto que a promessa do mob permite.
     * Lentidao que dure mais do que o ciclo de mergulho prenderia o jogador
     * dentro do raio de aviso, e "quem recua e poupado" viraria mentira --
     * punindo justamente a resposta que o mob ensina. Dois segundos de
     * Lentidao I atrapalham a fuga sem impedi-la.</p>
     */
    private static final int TICKS_DE_TEIA = 40;
    /** Amplificador da teia; I, e nao II, pelo mesmo motivo do tempo. */
    private static final int FORCA_DA_TEIA = 0;
    /** Modificador de velocidade dos voos guiados por navegacao e move control. */
    private static final double VELOCIDADE_DE_VOO = 1.0D;
    /** Altura maxima da patrulha acima do ninho. */
    private static final int ALTURA_DA_PATRULHA = 4;
    /** Quanto tempo a ave descansa entre dois voos de patrulha. */
    private static final int TICKS_ENTRE_VOOS = 40;

    // Estado DA INSTANCIA. Guardar qualquer um destes numa Goal (ou num static)
    // faria todas as aves do mundo compartilharem o mesmo ninho e o mesmo
    // mergulho -- a Goal e uma por entidade, e as classes aninhadas abaixo sao
    // static justamente para nao deixar essa dependencia escondida.
    private BlockPos ninho;
    private AttackTimeline linhaDoTempoDoMergulho = new AttackTimeline();
    private boolean mergulhoEmAndamento;
    private boolean jaAcertouNesteMergulho;
    private int ticksAvisando;

    // Sensores do cerebro.
    private int memoriaDeAlvo;
    private double distanciaAnteriorAoNinho = Double.NaN;
    private boolean intrusoNaZonaDoNinho;

    public SpiderEagleEntity(EntityType<? extends SpiderEagleEntity> type, Level level) {
        super(type, level, HunterExamProfiles.spiderEagle().metadata(),
                // O cerebro passa de WARN para ENGAGE exatamente quando a guarda
                // permite o bote: um segundo numero de aviso aqui divergiria do perfil
                // sem dar erro, e a ave "avisaria" num relogio e atacaria em outro.
                new AwarenessTuning(NINHO.ticksDeAviso(), MEMORIA_DE_ALVO_TICKS));
        // Primeira entidade VOADORA do mod. O molde e a Bee do vanilla: move control
        // de voo, navegacao de voo e nenhum dano de queda.
        this.moveControl = new FlyingMoveControl(this, 20, false);
        setPathfindingMalus(PathType.WATER, -1.0F);
        setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = HunterExamProfiles.spiderEagle().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                // FLYING_SPEED nao vem de createMobAttributes, e e ela que a
                // FlyingMoveControl le fora do chao. Sem esta linha o atributo nao
                // existe e a leitura estoura no primeiro voo. O valor e o MESMO do
                // perfil de proposito: a ave nao tem duas velocidades para girar.
                .add(Attributes.FLYING_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<SpiderEagleEntity> registeredType() {
        return EnemyEntityTypes.SPIDER_EAGLE.get();
    }

    @Override
    protected PathNavigation createNavigation(Level nivel) {
        FlyingPathNavigation navegacao = new FlyingPathNavigation(this, nivel);
        navegacao.setCanOpenDoors(false);
        navegacao.setCanFloat(false);
        navegacao.setCanPassDoors(false);
        return navegacao;
    }

    /**
     * A ave nao leva dano de queda -- a queda E o ataque dela.
     *
     * <p>{@code checkFallDamage} vazio e o que a Bee faz, e e ele que impede a
     * distancia de queda de virar dano no fim do mergulho. O
     * {@code causeFallDamage} esta aqui pela mesma decisao, para quem chamar o
     * dano de queda por fora (montaria, outro mod) receber a mesma resposta.</p>
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
        builder.define(AVISANDO, Boolean.FALSE);
        builder.define(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
    }

    /** Avisando; vale nos dois lados, porque vem do SynchedEntityData. */
    public boolean estaAvisando() { return this.entityData.get(AVISANDO); }

    /** Fase corrente do mergulho; vale nos dois lados, pelo mesmo motivo. */
    public AttackPhase faseDeAtaque() {
        int ordinal = this.entityData.get(FASE_DE_ATAQUE);
        AttackPhase[] fases = AttackPhase.values();
        return ordinal >= 0 && ordinal < fases.length ? fases[ordinal] : AttackPhase.IDLE;
    }

    /**
     * O ninho desta ave.
     *
     * <p>Verdade do SERVIDOR: no cliente nao ha NBT nem tick de ancoragem, e a
     * resposta cai para a posicao atual. Quem desenha nao deve depender dela.</p>
     */
    public BlockPos ninho() {
        return ninho != null ? ninho : blockPosition();
    }

    // ------------------------------------------------------------ persistencia

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ninho != null) tag.put(CHAVE_DO_NINHO, NbtUtils.writeBlockPos(ninho));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Ausente quer dizer "ave recem-nascida": a ancoragem acontece uma unica vez,
        // no primeiro tick de servidor, e nunca mais.
        ninho = NbtUtils.readBlockPos(tag, CHAVE_DO_NINHO).orElse(null);
    }

    // -------------------------------------------------------------------- tick

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;
        ancorarNinhoUmaVez();

        LivingEntity intruso = alvoValido();
        // Medido UMA vez por tick, a partir do NINHO, e todo mundo le o mesmo valor.
        intrusoNaZonaDoNinho = intruso != null && NINHO.avisa(distanciaDoNinho(intruso));

        // O mergulho e tickado AQUI, e nao dentro da Goal, porque a Goal pode ser
        // preemptada por outra de prioridade maior. Se o relogio do mergulho
        // dependesse da Goal viva, um banho no rio congelaria a fase em ACTIVE --
        // sem erro nenhum no log.
        if (mergulhoEmAndamento) tickDoMergulho(intruso);
        else avaliarEscadaDeGuarda(intruso);

        EnemyAwarenessState consciencia = enemyBrain().tick(lerSensores(intruso));
        alinharEstadoDeCombate(consciencia);
    }

    /** O ninho nasce na posicao de spawn, e so quando o save nao trouxe um. */
    private void ancorarNinhoUmaVez() {
        if (ninho == null) ninho = blockPosition();
    }

    /**
     * A escada inteira do mob, medida a partir do NINHO.
     *
     * <p>Sem intruso, ou com o intruso longe, a saida e a mesma: voltar para
     * casa. Dentro do raio de aviso a ave avisa; dentro do raio de bote e
     * depois do aviso completo, ela mergulha. A ordem importa: {@code desiste}
     * vem antes de {@code avisa} porque e a unica que pode terminar o
     * episodio.</p>
     */
    private void avaliarEscadaDeGuarda(LivingEntity intruso) {
        if (intruso == null) {
            if (estaAvisando()) voltarAoNinho();
            return;
        }
        double doIntrusoAoNinho = distanciaDoNinho(intruso);
        if (NINHO.desiste(distanciaDaAveAoNinho(), doIntrusoAoNinho)) {
            voltarAoNinho();
            return;
        }
        if (!NINHO.avisa(doIntrusoAoNinho)) return;
        avisar(intruso);
        if (NINHO.bote(doIntrusoAoNinho, ticksAvisando)) iniciarMergulho();
    }

    /** Sobe, encara e publica o aviso. Avisando a ave NAO ataca. */
    private void avisar(LivingEntity intruso) {
        this.entityData.set(AVISANDO, Boolean.TRUE);
        ticksAvisando++;
        combatState(EnemyCombatState.AGGRO);
        getNavigation().stop();
        getLookControl().setLookAt(intruso, 30.0F, 30.0F);
        // Sobe sobre o PROPRIO ninho, nao sobre o intruso: o aviso e uma exibicao
        // de posse do lugar, e quem se aproxima do jogador e o mergulho.
        BlockPos casa = ninho();
        getMoveControl().setWantedPosition(casa.getX() + 0.5D,
                casa.getY() + 1.0D + ALTURA_DO_TELEGRAFO, casa.getZ() + 0.5D, VELOCIDADE_DE_VOO);
    }

    // ------------------------------------------------------------- mergulho

    /** Comeca um mergulho do zero; a linha do tempo e NOVA para nunca herdar fase presa. */
    private void iniciarMergulho() {
        mergulhoEmAndamento = true;
        jaAcertouNesteMergulho = false;
        linhaDoTempoDoMergulho = new AttackTimeline();
        linhaDoTempoDoMergulho.start(MERGULHO);
        getNavigation().stop();
        combatState(EnemyCombatState.WINDUP);
        publicarFase(AttackPhase.WINDUP);
    }

    /**
     * WINDUP e a subida (o telegrafo), ACTIVE e a descida em cima do alvo,
     * RECOVERY e a volta ao alto.
     *
     * <p>A gravidade continua sendo da {@link FlyingMoveControl}: o mergulho
     * escreve velocidade, nunca {@code setNoGravity}. Duas fontes para a mesma
     * gravidade dariam uma ave que as vezes flutua parada no fim da descida.</p>
     */
    private void tickDoMergulho(LivingEntity intruso) {
        if (intruso == null) {
            // Morte ou sumico do alvo no meio do mergulho: mesma saida de sempre.
            voltarAoNinho();
            return;
        }
        // RECUAR DURANTE O TELEGRAFO AINDA SALVA. Quem sai do raio de aviso enquanto a
        // ave sobe e poupado, e a autorizacao para cancelar vem do proprio ataque
        // ({@code interruptibleWindup}), nao de um segundo criterio escrito aqui. A
        // descida ja lancada NAO para: ACTIVE e declarada ininterruptivel, e um
        // mergulho que evapora no ar seria um golpe que nunca da para ler.
        if (linhaDoTempoDoMergulho.canInterrupt()
                && NINHO.desiste(distanciaDaAveAoNinho(), distanciaDoNinho(intruso))) {
            voltarAoNinho();
            return;
        }
        switch (linhaDoTempoDoMergulho.phase()) {
            case WINDUP -> {
                combatState(EnemyCombatState.WINDUP);
                getLookControl().setLookAt(intruso, 30.0F, 30.0F);
                // Reescrito a cada tick: a MoveControl consome um destino por tick.
                getMoveControl().setWantedPosition(intruso.getX(),
                        intruso.getY() + ALTURA_DO_TELEGRAFO, intruso.getZ(), VELOCIDADE_DE_VOO);
            }
            case ACTIVE -> {
                combatState(EnemyCombatState.ACTIVE);
                descerSobreOAlvo(intruso);
                aplicarDanoDoMergulho();
            }
            case RECOVERY -> {
                combatState(EnemyCombatState.RECOVERY);
                BlockPos casa = ninho();
                getMoveControl().setWantedPosition(casa.getX() + 0.5D,
                        casa.getY() + 1.0D + ALTURA_DO_TELEGRAFO, casa.getZ() + 0.5D,
                        VELOCIDADE_DE_VOO);
            }
            default -> { }
        }
        linhaDoTempoDoMergulho.tick();
        AttackPhase proxima = linhaDoTempoDoMergulho.phase();
        if (proxima == AttackPhase.COMPLETE) {
            // Fim da linha do tempo: o episodio acaba, e acaba pela saida unica. Isto e
            // o que forca um aviso NOVO antes do proximo bote -- a ave nao encadeia
            // mergulhos em cima de quem ja esta no chao.
            voltarAoNinho();
            return;
        }
        publicarFase(proxima);
    }

    /** A descida: direcao recalculada no tick, velocidade derivada da geometria. */
    private void descerSobreOAlvo(LivingEntity intruso) {
        getLookControl().setLookAt(intruso, 30.0F, 30.0F);
        Vec3 delta = intruso.position().add(0.0D, intruso.getBbHeight() * 0.5D, 0.0D)
                .subtract(position());
        if (delta.lengthSqr() < 1.0E-6D) return;
        Vec3 direcao = delta.normalize();
        setDeltaMovement(direcao.scale(VELOCIDADE_DO_MERGULHO));
        float rotacao = (float) (Mth.atan2(direcao.z, direcao.x) * (180.0D / Math.PI)) - 90.0F;
        setYRot(rotacao);
        this.yBodyRot = rotacao;
        this.hasImpulse = true;
    }

    /**
     * A primeira vitima viva no caminho da descida leva dano, empurrao e teia --
     * UMA vez por mergulho.
     *
     * <p>A caixa e esticada na direcao do proprio deslocamento: a descida anda
     * mais de um bloco por tick, e uma caixa parada deixaria a ave atravessar o
     * alvo entre dois ticks sem encostar nele. Isso nao daria erro: daria um
     * mergulho que "passa por dentro" do jogador de vez em quando.</p>
     *
     * <p>Este metodo NAO multiplica dano nenhum: o numero vem da definicao do
     * ataque, que por sua vez le o ATTACK_DAMAGE do perfil.</p>
     */
    private void aplicarDanoDoMergulho() {
        if (jaAcertouNesteMergulho) return;
        AABB caminho = getBoundingBox().expandTowards(getDeltaMovement()).inflate(FOLGA_DO_MERGULHO);
        for (LivingEntity vitima : level().getEntitiesOfClass(LivingEntity.class, caminho,
                v -> v != this && v.getType() != getType() && v.isAlive()
                        && v.isAttackable() && !isAlliedTo(v))) {
            if (!vitima.hurt(damageSources().mobAttack(this), MERGULHO.damage())) continue;
            vitima.knockback(MERGULHO.knockback(), getX() - vitima.getX(), getZ() - vitima.getZ());
            // "Web disruption": a teia que atrapalha a saida, e nao a impede.
            vitima.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    TICKS_DE_TEIA, FORCA_DA_TEIA), this);
            jaAcertouNesteMergulho = true;
            return;
        }
    }

    /**
     * SAIDA UNICA do episodio.
     *
     * <p>Desistencia, fim da linha do tempo, morte do alvo, morte da ave e
     * remocao da entidade terminam TODOS aqui. E aqui que o aviso apaga, o
     * contador zera, a fase volta para IDLE e o alvo e largado. Se um caminho
     * deixar de passar por aqui, sobra uma ave presa em AVISANDO ou num
     * mergulho pela metade -- e isso nao aparece como erro, aparece como um
     * bicho que "ficou estranho" e ninguem consegue reproduzir.</p>
     */
    private void voltarAoNinho() {
        mergulhoEmAndamento = false;
        jaAcertouNesteMergulho = false;
        linhaDoTempoDoMergulho = new AttackTimeline();
        ticksAvisando = 0;
        memoriaDeAlvo = 0;
        distanciaAnteriorAoNinho = Double.NaN;
        intrusoNaZonaDoNinho = false;
        this.entityData.set(AVISANDO, Boolean.FALSE);
        publicarFase(AttackPhase.IDLE);
        setTarget(null);
        getNavigation().stop();
        if (combatState() != EnemyCombatState.DYING) combatState(estadoOcioso());
        // Quem leva o corpo de volta e a PatrulhaDoNinhoGoal, que so roda sem alvo --
        // e agora nao ha mais alvo.
    }

    private void publicarFase(AttackPhase fase) {
        this.entityData.set(FASE_DE_ATAQUE, fase.ordinal());
    }

    // ----------------------------------------------------------------- saidas

    /**
     * SER FERIDA NAO MUDA A COLEIRA.
     *
     * <p>Um agressor de FORA do raio de aviso do ninho nao vira alvo: a ave que
     * persegue o arqueiro pelo canyon inteiro quebra a promessa de "quem recua
     * e poupado", e o encounter de roubar sem matar morre junto. E exatamente
     * por isso que este mob NAO tem {@code HurtByTargetGoal} -- ele adotaria o
     * agressor sem perguntar onde ele esta, e a excecao viveria escondida numa
     * Goal do vanilla em vez de escrita aqui.</p>
     *
     * <p>Dentro da zona, sim: quem atira de dentro do ninho ja e intruso, e
     * adota-lo aqui apenas antecipa o que o alvo por proximidade faria.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean aplicou = super.hurt(source, amount);
        if (!aplicou || level().isClientSide || !isAlive()) return aplicou;
        if (!(source.getEntity() instanceof LivingEntity agressor) || !agressor.isAlive()) {
            return aplicou;
        }
        if (!NINHO.avisa(distanciaDoNinho(agressor))) return aplicou;
        setTarget(agressor);
        return aplicou;
    }

    @Override
    public void die(DamageSource source) {
        // Quem liga, desliga: morrer no meio da descida nao pode deixar a fase presa
        // em ACTIVE para o cliente que ainda ve o corpo cair.
        if (!level().isClientSide) {
            combatState(EnemyCombatState.DYING);
            voltarAoNinho();
        }
        super.die(source);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide) voltarAoNinho();
        super.remove(reason);
    }

    // -------------------------------------------------------------- geometria

    /** Distancia do intruso ao NINHO -- nunca a ave. Alvo ausente vira NaN. */
    private double distanciaDoNinho(LivingEntity quem) {
        if (quem == null || !quem.isAlive()) return Double.NaN;
        return Vec3.atCenterOf(ninho()).distanceTo(quem.position());
    }

    /** Distancia da propria ave ao ninho; e ela que a coleira mede. */
    private double distanciaDaAveAoNinho() {
        return Vec3.atCenterOf(ninho()).distanceTo(position());
    }

    /** O alvo do tick, ja filtrado por vivo; null e "nao tenho intruso". */
    private LivingEntity alvoValido() {
        LivingEntity alvo = getTarget();
        return alvo != null && alvo.isAlive() ? alvo : null;
    }

    // --------------------------------------------------------------- sensores

    /** Leitura de mundo do tick: e isto que faz awarenessState() parar de ser decorativo. */
    private AwarenessInput lerSensores(LivingEntity intruso) {
        boolean alvoVivo = intruso != null;
        boolean visivel = alvoVivo && hasLineOfSight(intruso);
        // RECUAR, para este mob, e afastar-se do NINHO -- nao da ave. Medir a partir
        // do corpo faria a ave em voo "sentir" recuo so por ter passado do alvo.
        double distancia = alvoVivo ? distanciaDoNinho(intruso) : Double.NaN;

        if (visivel) memoriaDeAlvo = MEMORIA_DE_ALVO_TICKS;
        else if (memoriaDeAlvo > 0) memoriaDeAlvo--;

        boolean recuando = visivel && !Double.isNaN(distanciaAnteriorAoNinho)
                && distancia > distanciaAnteriorAoNinho + TOLERANCIA_DE_RECUO;
        distanciaAnteriorAoNinho = distancia;

        boolean audivel = !visivel && memoriaDeAlvo > 0;
        boolean vidaCritica = getHealth() <= getMaxHealth() * FRACAO_DE_VIDA_CRITICA;

        // AQUI o targetInTerritory deixa de ser papel: este e o primeiro mob do
        // repositorio em que "territorio" e um LUGAR de verdade, e nao um raio ao
        // redor do proprio bicho. A ave nunca embosca: ela avisa antes, sempre.
        return new AwarenessInput(visivel, audivel, intrusoNaZonaDoNinho, recuando, !alvoVivo,
                memoriaDeAlvo, false, vidaCritica);
    }

    /**
     * O mergulho manda; fora dele quem manda e a consciencia.
     *
     * <p>PONTO CEGO DECLARADO: {@link EnemyAwarenessState#FLEE} vira apenas
     * {@code RETREAT} para quem observa (bestiario, HUD). A ave NAO foge do
     * ninho com vida critica, e isso e escolha: um guarda de ninho que abandona
     * o ninho nao e guarda de ninho. Nao ha Goal de fuga, e nao ha de proposito.</p>
     */
    private void alinharEstadoDeCombate(EnemyAwarenessState consciencia) {
        if (mergulhoEmAndamento || combatState() == EnemyCombatState.DYING) return;
        if (consciencia == EnemyAwarenessState.FLEE) {
            combatState(EnemyCombatState.RETREAT);
            return;
        }
        combatState(estaAvisando() ? EnemyCombatState.AGGRO : estadoOcioso());
    }

    private EnemyCombatState estadoOcioso() {
        return enemyMetadata().social() ? EnemyCombatState.GROUP_ROAM : EnemyCombatState.IDLE;
    }

    /** Voo de patrulha e de volta ao ninho: um destino so, dois motivos. */
    private void navegarPeloEntornoDoNinho() {
        BlockPos casa = ninho();
        if (distanciaDaAveAoNinho() > NINHO.raioDeBote()) {
            getNavigation().moveTo(casa.getX() + 0.5D, casa.getY() + 1.0D, casa.getZ() + 0.5D,
                    VELOCIDADE_DE_VOO);
            return;
        }
        double raio = NINHO.raioDeBote();
        double destinoX = casa.getX() + 0.5D + (getRandom().nextDouble() * 2.0D - 1.0D) * raio;
        double destinoZ = casa.getZ() + 0.5D + (getRandom().nextDouble() * 2.0D - 1.0D) * raio;
        double destinoY = casa.getY() + 1.0D + getRandom().nextInt(ALTURA_DA_PATRULHA);
        getNavigation().moveTo(destinoX, destinoY, destinoZ, VELOCIDADE_DE_VOO);
    }

    // ------------------------------------------------------------------ goals

    @Override
    protected void registerGoals() {
        // SEM FloatGoal, de proposito: o construtor dela liga setCanFloat(true) na
        // navegacao, e isso brigaria com o setCanFloat(false) escrito em
        // createNavigation -- duas fontes para a mesma verdade, decididas pela ordem
        // de inicializacao. A Bee do vanilla, que e o molde deste mob, tambem nao usa
        // FloatGoal. O preco esta declarado: a ave nao se salva sozinha na agua.
        goalSelector.addGoal(1, new GuardaDoNinhoGoal(this));
        goalSelector.addGoal(2, new PatrulhaDoNinhoGoal(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // SEM HurtByTargetGoal, de proposito -- ver hurt(). O alvo entra por
        // proximidade DO NINHO, e o predicado e a coleira escrita no alvo: quem esta
        // fora da zona nunca e escolhido, mesmo visivel e mesmo dentro do
        // FOLLOW_RANGE.
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10,
                true, false, candidato -> NINHO.avisa(distanciaDoNinho(candidato))));
    }

    /**
     * Segura MOVE e LOOK enquanto a ave avisa ou mergulha.
     *
     * <p>Ela NAO ticka o mergulho: quem faz isso e o {@code customServerAiStep},
     * para que uma Goal de prioridade maior nao congele a fase ao preemptar.
     * O trabalho desta Goal e impedir que a patrulha herde o corpo no meio do
     * aviso -- sem ela, a ave sairia passeando exatamente enquanto deveria
     * estar parada no ar encarando o intruso, e o telegrafo sumiria.</p>
     *
     * <p>E static de proposito: todo estado que ela le mora na ENTIDADE, e o
     * prefixo {@code ave.} deixa isso impossivel de esquecer.</p>
     */
    private static final class GuardaDoNinhoGoal extends Goal {
        private final SpiderEagleEntity ave;

        private GuardaDoNinhoGoal(SpiderEagleEntity ave) {
            this.ave = ave;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (ave.level().isClientSide) return false;
            return ave.mergulhoEmAndamento || ave.estaAvisando();
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void stop() { ave.getNavigation().stop(); }
    }

    /**
     * Patrulha e volta para casa: a ave sem intruso circula perto do ninho, e a
     * ave longe dele volta em linha reta.
     *
     * <p>Os dois casos vivem no mesmo metodo da entidade de proposito: separados,
     * "voltar" e "patrulhar" poderiam discordar sobre onde e o ninho.</p>
     */
    private static final class PatrulhaDoNinhoGoal extends Goal {
        private final SpiderEagleEntity ave;
        private int esperaRestante;

        private PatrulhaDoNinhoGoal(SpiderEagleEntity ave) {
            this.ave = ave;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override public boolean canUse() {
            if (ave.level().isClientSide) return false;
            if (ave.mergulhoEmAndamento || ave.estaAvisando()) return false;
            return ave.alvoValido() == null;
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() { esperaRestante = 0; }

        @Override public void tick() {
            if (esperaRestante > 0) {
                esperaRestante--;
                return;
            }
            if (!ave.getNavigation().isDone()) return;
            esperaRestante = TICKS_ENTRE_VOOS;
            ave.navegarPeloEntornoDoNinho();
        }

        @Override public void stop() { ave.getNavigation().stop(); }
    }
}
