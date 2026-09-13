package com.darkcontinent.nenfoundation.enemy;

import com.darkcontinent.nenfoundation.registry.EnemyEntityTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.EnumSet;
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
 * Primeiro mob territorial; alvo, estado e dano são sempre decididos no servidor.
 *
 * <p>CORPO PROPRIO (ADR-017). Ate aqui o Foxbear vestia a geometria do
 * URSO-POLAR vanilla com uma textura autoral pintada na UV emprestada. Ele era o
 * ULTIMO nome da divida visual. Agora e um {@link GeoEntity} de id proprio
 * {@code foxbear}, com esqueleto e clipes fixados pela diretriz de mobs
 * customizados. O COMPORTAMENTO NAO MUDOU uma linha: o raio de territorio, a
 * escada de aviso e as Goals sao os mesmos que o gametest mede -- esta entrega
 * acrescenta, nao reescreve.</p>
 *
 * <p>O ESTADO PASSOU A CHEGAR AO CLIENTE, e essa e a unica mudanca de
 * comportamento observavel. O campo {@code state} e memoria de SERVIDOR: no
 * cliente ele responderia {@code ROAM} para todo Foxbear, para sempre, e escolher
 * animacao por ele mostraria o mesmo clipe em toda tela sem nada acusar. Quem o
 * cliente le e {@link #estadoVisivel()}, vindo do {@link SynchedEntityData}. O
 * campo antigo e {@link #state()} continuam servindo o servidor -- as Goals e os
 * gametests leem dali, e ter os dois NAO e duas fontes de verdade: a publicacao
 * acontece dentro do unico {@code setState}, entao nao existe caminho que mude o
 * estado sem publicar.</p>
 */
public final class FoxbearEntity extends Animal implements GeoEntity {
    private static final TargetingConditions PLAYER_TARGET = TargetingConditions.forCombat()
            .range(FoxbearTerritory.TERRITORY_RADIUS);

    /**
     * O estado, em ordinal, para o cliente.
     *
     * <p>E o UNICO pedaco do cerebro que atravessa a rede, e atravessa porque o
     * cliente precisa dele para desenhar -- nunca para decidir. O servidor
     * continua sendo quem escolhe alvo, aviso e dano.</p>
     */
    private static final EntityDataAccessor<Integer> ESTADO_VISIVEL =
            SynchedEntityData.defineId(FoxbearEntity.class, EntityDataSerializers.INT);

    private FoxbearState state = FoxbearState.ROAM;
    private double warningDistance = Double.NaN;
    private int warningTicks;
    /** Quanto tempo o aviso dura antes de o Foxbear desistir e voltar. */
    private static final int TICKS_MAXIMOS_DE_AVISO = 80;
    public FoxbearEntity(EntityType<? extends FoxbearEntity> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 44.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D).add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ARMOR, 2.0D).add(Attributes.FOLLOW_RANGE, FoxbearTerritory.TERRITORY_RADIUS);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this)); goalSelector.addGoal(1, new TerritorialGoal(this));
        goalSelector.addGoal(2, new FoxbearAttackGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }
    @Override public boolean isFood(net.minecraft.world.item.ItemStack stack) { return false; }
    @Override public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob parent) {
        return EnemyEntityTypes.FOXBEAR.get().create(level);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        // ROAM e o default de proposito: e o mesmo valor com que o campo de servidor
        // nasce. Um default diferente daria um bicho que aparece avisando -- ou
        // dormindo -- no primeiro frame depois do spawn, e depois "corrige" sozinho.
        builder.define(ESTADO_VISIVEL, FoxbearState.ROAM.ordinal());
    }

    /** Verdade de SERVIDOR: as Goals e os gametests leem daqui. */
    public FoxbearState state() { return state; }

    /**
     * O mesmo estado, valido NOS DOIS LADOS, porque vem do {@link SynchedEntityData}.
     *
     * <p>E este -- nunca {@link #state()} -- que a escolha de animacao usa.</p>
     */
    public FoxbearState estadoVisivel() {
        int ordinal = this.entityData.get(ESTADO_VISIVEL);
        FoxbearState[] estados = FoxbearState.values();
        return ordinal >= 0 && ordinal < estados.length ? estados[ordinal] : FoxbearState.ROAM;
    }

    public boolean isWarning() { return state == FoxbearState.WARN; }
    private void setState(FoxbearState next) {
        if (state == next) return; state = next;
        // PUBLICA AQUI, e so aqui. Este e o unico ponto que muda o estado; espalhar a
        // publicacao criaria um segundo caminho que troca o estado sem avisar o
        // cliente, e o sintoma seria uma animacao presa no clipe anterior -- sem erro.
        this.entityData.set(ESTADO_VISIVEL, next.ordinal());
        if (next != FoxbearState.WARN) { warningDistance = Double.NaN; warningTicks = 0; }
    }
    // ------------------------------------------------------------- animacao
    // Os nomes abaixo sao um CONTRATO com foxbear.animation.json. Errar um deles nao
    // da erro: o GeckoLib procura o clipe, nao acha, e deixa o osso parado -- o tipo
    // de falha que so aparece na tela de quem joga.
    //
    // O tipo de repeticao NAO mora aqui. LoopType.DEFAULT delega para o que o
    // .animation.json declarar, e e la que o artista mexe: cravar thenLoop no codigo
    // faria o arquivo dizer uma coisa e o jogo fazer outra, em silencio.
    private static final RawAnimation IDLE =
            RawAnimation.begin().then("animation.foxbear.idle", Animation.LoopType.DEFAULT);
    private static final RawAnimation WALK =
            RawAnimation.begin().then("animation.foxbear.walk", Animation.LoopType.DEFAULT);
    private static final RawAnimation RUN =
            RawAnimation.begin().then("animation.foxbear.run", Animation.LoopType.DEFAULT);
    private static final RawAnimation WARN_CLIP =
            RawAnimation.begin().then("animation.foxbear.warn", Animation.LoopType.DEFAULT);
    private static final RawAnimation SLEEP =
            RawAnimation.begin().then("animation.foxbear.sleep", Animation.LoopType.DEFAULT);

    /** Nome do unico controller; quem registrar um segundo clipe reusa esta constante. */
    private static final String CONTROLLER_DO_CORPO = "corpo";
    /** Ticks de mistura entre um clipe e o proximo -- o mesmo dos quatro mobs irmaos. */
    private static final int TRANSICAO_EM_TICKS = 4;

    /** Cache por INSTANCIA. Um cache estatico faria todos os Foxbears compartilharem um clipe. */
    private final AnimatableInstanceCache cacheDeAnimacao = GeckoLibUtil.createInstanceCache(this);

    /**
     * O CLIENTE NAO DECIDE NADA. Ele le {@link #estadoVisivel()}, que o servidor
     * publica, e o {@code isMoving} que o proprio GeckoLib mede do corpo. Nao ha
     * aqui timer, copia da escada de aviso nem heuristica de "parece que vai
     * atacar": se a animacao e o comportamento discordarem, quem esta errado e o
     * arquivo de animacao, nunca o servidor.
     *
     * <p>PONTO CEGO DECLARADO -- SETE dos doze clipes da diretriz existem no
     * arquivo e ninguem os pede ainda:</p>
     *
     * <ul>
     *   <li>{@code rear_warn}: o aviso em duas patas depende de saber que o intruso
     *       esta PERTO DEMAIS, e a unica medida disso ({@code warningDistance}) e de
     *       servidor. Medir distancia no cliente escolhendo um jogador por conta
     *       propria seria o cliente decidindo -- exatamente o que o ADR-001 proibe --
     *       e daria um urso que se levanta para o jogador errado.</li>
     *   <li>{@code claw_swipe}, {@code bite}, {@code short_charge}: a entidade usa
     *       {@code MeleeAttackGoal} e nao publica fase de ataque nenhuma. Inventar um
     *       gatilho a partir do estado {@code ENGAGE} mostraria a patada sem pancada
     *       alguma, o que e pior do que nao mostrar.</li>
     *   <li>{@code idle_sniff}: depende de {@code FORAGE}, que hoje nenhuma Goal
     *       produz -- o estado existe no enum e nunca e atingido.</li>
     *   <li>{@code hurt} e {@code death}: levar dano e morrer nao passam por estado
     *       sincronizado neste mob.</li>
     * </ul>
     *
     * <p>{@code sleep} e um caso a parte e merece o nome certo: ele TEM mapeamento
     * ({@code REST}) e mesmo assim nao toca, porque {@code REST} -- como
     * {@code FORAGE} -- existe no enum e nenhuma Goal o produz;
     * {@link FoxbearTerritory#stateFor} so devolve {@code WARN}, {@code ENGAGE} e
     * {@code RETURN_HOME}. Escrever o mapeamento assim mesmo e deliberado: no dia em
     * que a rotina de descanso chegar, ela liga sozinha -- e enquanto nao chegar, a
     * ausencia esta escrita aqui em vez de virar "o urso nunca dorme, ninguem sabe
     * por que".</p>
     *
     * <p>Eles entram quando houver um gatilho de SERVIDOR para le-los. Clipe sem quem
     * o toque e divida, e divida se declara.</p>
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<FoxbearEntity>(this, CONTROLLER_DO_CORPO,
                TRANSICAO_EM_TICKS, this::clipeDoCorpo));
    }

    private PlayState clipeDoCorpo(AnimationState<FoxbearEntity> estado) {
        FoxbearState visivel = estadoVisivel();
        // Avisar e uma exibicao PARADA: o urso planta as patas e encara. Avisando em
        // movimento cai para a caminhada logo abaixo, porque o clipe de aviso nao tem
        // passada e o bicho deslizaria pelo chao.
        if (visivel == FoxbearState.WARN && !estado.isMoving()) {
            return estado.setAndContinue(WARN_CLIP);
        }
        if (visivel == FoxbearState.ENGAGE && estado.isMoving()) {
            return estado.setAndContinue(RUN);
        }
        if (visivel == FoxbearState.REST) return estado.setAndContinue(SLEEP);
        if (estado.isMoving()) return estado.setAndContinue(WALK);
        return estado.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cacheDeAnimacao; }

    @Override
    public double getTick(Object entidade) { return this.tickCount; }

    // ------------------------------------------------------------------ goals

    private static final class TerritorialGoal extends Goal {
        private final FoxbearEntity foxbear; private Player player;
        private TerritorialGoal(FoxbearEntity foxbear) { this.foxbear = foxbear; setFlags(EnumSet.of(Flag.LOOK)); }
        @Override public boolean canUse() {
            if (foxbear.level().isClientSide || foxbear.state == FoxbearState.ENGAGE) return false;
            player = foxbear.level().getNearestPlayer(PLAYER_TARGET, foxbear); return player != null;
        }
        @Override public boolean canContinueToUse() {
            return player != null && player.isAlive() && foxbear.distanceToSqr(player)
                    <= FoxbearTerritory.TERRITORY_RADIUS * FoxbearTerritory.TERRITORY_RADIUS;
        }
        @Override public void start() {
            // `foxbear.` E OBRIGATORIO: esta Goal e static, e os campos de
            // aviso sao da ENTIDADE -- e tem de ser, porque a implementacao
            // seria compartilhada entre todos os Foxbears se morasse aqui.
            foxbear.warningDistance = foxbear.distanceTo(player);
            foxbear.setState(FoxbearState.WARN);
        }
        @Override public void tick() {
            if (player == null) return; double distance = foxbear.distanceTo(player);
            foxbear.getLookControl().setLookAt(player, 30.0F, 30.0F); foxbear.warningTicks++;
            boolean advanced = distance + FoxbearTerritory.ADVANCE_TOLERANCE < foxbear.warningDistance;
            boolean retreated = distance > foxbear.warningDistance + 1.0D
                    || foxbear.warningTicks > TICKS_MAXIMOS_DE_AVISO;
            FoxbearState next = FoxbearTerritory.stateFor(distance <= FoxbearTerritory.TERRITORY_RADIUS,
                    advanced, retreated, true); foxbear.setState(next);
            if (next == FoxbearState.ENGAGE) foxbear.setTarget(player);
            if (next == FoxbearState.RETURN_HOME) foxbear.setTarget(null);
        }
        @Override public void stop() {
            if (foxbear.state != FoxbearState.ENGAGE) foxbear.setState(FoxbearState.RETURN_HOME);
            player = null;
        }
    }
    private static final class FoxbearAttackGoal extends MeleeAttackGoal {
        private final FoxbearEntity foxbear;
        private FoxbearAttackGoal(FoxbearEntity foxbear) { super(foxbear, 1.15D, true); this.foxbear = foxbear; }
        @Override public boolean canUse() { return foxbear.state == FoxbearState.ENGAGE && super.canUse(); }
        @Override public boolean canContinueToUse() { return foxbear.state == FoxbearState.ENGAGE && super.canContinueToUse(); }
    }
}
