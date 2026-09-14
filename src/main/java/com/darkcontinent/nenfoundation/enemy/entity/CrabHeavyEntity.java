package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyCombatState;
import com.darkcontinent.nenfoundation.enemy.base.BaseChimeraAnt;
import com.darkcontinent.nenfoundation.enemy.base.EnemyRuntime;
import com.darkcontinent.nenfoundation.enemy.combat.AttackController;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHit;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.FaceDaCarapaca;
import com.darkcontinent.nenfoundation.enemy.combat.GrabController;
import com.darkcontinent.nenfoundation.enemy.combat.GrabRefusal;
import com.darkcontinent.nenfoundation.enemy.combat.GrabRelease;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeCarapacaOrientada;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDePinca;
import com.darkcontinent.nenfoundation.enemy.combat.SafeReleaseSpot;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import com.darkcontinent.nenfoundation.enemy.content.ChimeraProfiles;
import com.darkcontinent.nenfoundation.enemy.content.CrabHeavyTuning;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
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
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Crab Heavy -- formiga quimera, rank PEON. O que SEGURA a linha.
 *
 * <p><b>A CARAPACA TEM LADO.</b> A armadura 8 da ficha vale inteira de frente,
 * pela metade no flanco e quase nada pelas costas -- {@link
 * RegrasDeCarapacaOrientada} decide, {@link CrabHeavyTuning#carapaca()} fornece
 * os numeros, e a conta de dano continua sendo a do proprio jogo, chamada uma
 * vez so, em {@link #getDamageAfterArmorAbsorb}. Baixar a armadura resolveria o
 * mesmo problema e apagaria o encontro junto: o quebra-cabeca deste bicho e
 * CHEGAR POR TRAS, e sem a diferenca de angulo ele e um peon gordo.</p>
 *
 * <p><b>O angulo sai do CORPO, e nao do olhar.</b> {@code getLookAngle()} segue
 * a cabeca, e a cabeca vira sozinha para acompanhar quem passa. Se a placa
 * seguisse o olhar, a armadura do bicho giraria quando ele apenas olhasse de
 * lado, e o jogador que contornou com cuidado levaria a recusa cheia sem
 * entender por que. Nao ha erro nisso -- ha uma regra que se move debaixo de
 * quem esta obedecendo a ela.</p>
 *
 * <p><b>ELE AGARRA, E SEGURA SEM MONTAR.</b> O relogio, os pulsos de dano e as
 * dez saidas do agarrao sao do {@link GrabController} compartilhado -- nao de
 * uma copia; o CLAUDE.md e explicito sobre um unico ciclo de vida por familia. O
 * que e proprio deste bicho e o JEITO de segurar, e isso mora em {@link
 * RegrasDePinca}: a presa fica no chao, na frente da pinca, puxada com forca
 * limitada. Montar um jogador tiraria a camera dele e transformaria o agarrao
 * numa cutscene; preso pela pinca ele continua olhando em volta -- e olhar em
 * volta e como ele descobre que alguem precisa ir para tras do bicho.</p>
 *
 * <p><b>A soltura passa por {@link SafeReleaseSpot}.</b> A soltura obvia -- "poe
 * a vitima na frente" -- mata quem foi preso contra uma parede, e esse caso so
 * acontece quando o caranguejo encurralou alguem, que e justamente quando o
 * agarrao e interessante.</p>
 *
 * <p><b>ELE NAO PERSEGUE ALEM DA LINHA.</b> Ele avanca ate {@link
 * CrabHeavyTuning#ALCANCE_DE_AVANCO} da posicao em que travou combate e volta.
 * Um frontliner que persegue atravessa o encontro atras de um alvo e deixa
 * aberto exatamente o lugar que ele existia para fechar -- continuando a atacar,
 * acertar e morrer normalmente, sem que nada reprove.</p>
 *
 * <p><b>O CLIENTE NAO DECIDE NADA:</b> ele le a fase de ataque e o cambaleio que
 * o servidor publica, e escolhe o clipe. Nenhum pacote de cliente carrega
 * acerto, angulo de impacto ou agarrao.</p>
 */
public final class CrabHeavyEntity extends BaseChimeraAnt
        implements software.bernie.geckolib.animatable.GeoEntity {

    private static final EntityDataAccessor<Integer> FASE_DE_ATAQUE =
            SynchedEntityData.defineId(CrabHeavyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CAMBALEANDO =
            SynchedEntityData.defineId(CrabHeavyEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int MEMORIA_DE_ALVO_TICKS = 140;
    private static final int TICKS_DE_AVISO = 20;
    private static final double ABERTURA_DA_VISAO = 80.0D;
    private static final double ALCANCE_DE_AUDICAO = 16.0D;
    private static final float FRACAO_DE_VIDA_CRITICA = 0.25F;

    /**
     * Quanto da placa tambem protege do CAMBALEIO.
     *
     * <p>LIMITE DE DESIGN, e nao botao de tuning: ele existe para que a carapaca
     * signifique a mesma coisa nos dois sistemas. Em 1.0 a placa cheia zeraria o
     * stagger e o bicho seria ininterrompivel de frente -- inclusive por um aliado
     * tentando libertar alguem; em 0.0 a carapaca protegeria da vida e nao do
     * equilibrio, e contornar deixaria de ter a recompensa imediata que ensina.
     * Meia placa e o meio-termo, declarado aqui e nao escondido numa conta.</p>
     */
    private static final float FRACAO_DE_ABSORCAO_DO_STAGGER = 0.5F;

    private static final RegrasDeCarapacaOrientada CARAPACA = CrabHeavyTuning.carapaca();
    private static final RegrasDePinca PINCA = CrabHeavyTuning.pinca();

    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache cacheDeAnimacao =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    // Estado DA INSTANCIA. Qualquer um destes num campo static faria todos os
    // caranguejos do mundo compartilharem a mesma vitima, o mesmo relogio e a
    // mesma linha -- que e o erro que este projeto ja sabe que comete.
    private final GrabController agarrao = new GrabController(CrabHeavyTuning.agarrao(),
            CrabHeavyTuning.ALTURA_MAXIMA_DA_PRESA, CrabHeavyTuning.LARGURA_MAXIMA_DA_PRESA);
    private int retiradaRestante;

    /**
     * De onde ele nao sai. Nula ate ele ver alguem pela primeira vez.
     *
     * <p>Ela e a POSICAO EM QUE O COMBATE COMECOU, e nao a de spawn: um
     * caranguejo alistado num esquadrao e levado para o portao do ninho tem de
     * segurar o portao, e nao o ponto onde nasceu. Ancorada no spawn, a peca
     * defenderia um lugar que ninguem esta atacando -- sem erro nenhum.</p>
     */
    private Vec3 linha;

    public CrabHeavyEntity(EntityType<? extends CrabHeavyEntity> type, Level level) {
        super(type, level, ChimeraProfiles.crabHeavy().metadata(),
                new AwarenessTuning(TICKS_DE_AVISO, MEMORIA_DE_ALVO_TICKS),
                ChimeraProfiles.crabHeavyMolde());
        instalarRuntime(montarRuntime());
    }

    private EnemyRuntime montarRuntime() {
        var perfil = ChimeraProfiles.crabHeavy();
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
                new AttackController(ChimeraProfiles.crabHeavyRecarga()),
                new StaggerState(ChimeraProfiles.crabHeavyStagger()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var attrs = ChimeraProfiles.crabHeavy().attributes();
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, attrs.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, attrs.movementSpeed())
                .add(Attributes.ATTACK_DAMAGE, attrs.attackDamage())
                .add(Attributes.ARMOR, attrs.armor())
                .add(Attributes.FOLLOW_RANGE, attrs.followRange())
                .add(Attributes.KNOCKBACK_RESISTANCE, attrs.knockbackResistance());
    }

    public static EntityType<CrabHeavyEntity> registeredType() { return EnemyEntityTypes.CRAB_HEAVY.get(); }

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

    /**
     * Verdade do SERVIDOR sobre haver alguem preso.
     *
     * <p>Nao e sincronizada de proposito: quem precisa dela e a regra de soltura,
     * e ela so vale no servidor. No cliente responde {@code false}, e por isso o
     * clipe de "segurando" e escolhido pela FASE publicada, nunca por aqui.</p>
     */
    public boolean estaAgarrando() { return agarrao.agarrando(); }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PincaGoal(this));
        goalSelector.addGoal(2, new SegurarALinhaGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Sem target goal vanilla: quem escolhe alvo e o TargetEvaluator, por
        // faccao. Um NearestAttackableTargetGoal ao lado seria uma segunda
        // autoridade sobre a mesma decisao, e as duas discordariam em silencio.
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide) return;
        if (retiradaRestante > 0) retiradaRestante--;
        EnemyRuntime runtime = runtimeExigido();
        PerceptionSnapshot snapshot = runtime.tick(tickCount, this::varrerCandidatos,
                getHealth() <= getMaxHealth() * FRACAO_DE_VIDA_CRITICA, false);

        // O agarrao e tickado AQUI, e nao dentro da Goal, porque a Goal pode ser
        // preemptada por outra de prioridade maior (a FloatGoal, por exemplo). Se o
        // relogio do agarrao dependesse da Goal viva, um mergulho na agua deixaria
        // a vitima presa com o relogio parado -- sem erro nenhum no log.
        tickDoAgarrao();
        aplicarAlvo(snapshot);
        tickDaPincada(runtime);
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
            // A linha e fincada no PRIMEIRO alvo e nao se move enquanto ele existir
            // como entidade. Refinca-la a cada alvo novo transformaria a coleira num
            // passeio em etapas: cada alvo puxaria o bicho mais doze blocos, e o
            // frontliner acabaria do outro lado do mapa cumprindo a regra ao pe da
            // letra.
            if (linha == null) linha = position();
        } else if (getTarget() != null) {
            setTarget(null);
        }
    }

    /** Varredura de mundo -- a UNICA da percepcao, e so quando o orcamento autoriza. */
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

    /** 1 quando o alvo esta na frente do OLHAR (cabeca); e o numero da percepcao. */
    private double cossenoDoOlharAte(Entity alvo) {
        Vec3 olhar = getLookAngle();
        return cosseno(new Vec3(olhar.x, 0.0D, olhar.z), alvo);
    }

    /**
     * 1 quando quem bate esta na frente do CORPO; -1 quando esta pelas costas.
     *
     * <p>NAO e o mesmo numero de {@link #cossenoDoOlharAte}. Aquele mede a cabeca
     * e serve a percepcao, que e o que a cabeca faz. Este mede o corpo e serve a
     * carapaca, que e o que o corpo veste. Trocar os dois nao levanta erro: faz a
     * armadura do bicho girar toda vez que ele olha para o lado, e o jogador que
     * contornou com cuidado leva a recusa cheia sem ter feito nada errado.</p>
     */
    private double cossenoDoCorpoAte(Entity atacante) {
        return cosseno(frenteDoCorpo(), atacante);
    }

    /**
     * Para onde o CORPO aponta, no plano.
     *
     * <p>Em Minecraft yaw 0 aponta para +Z e yaw positivo gira para -X -- a mesma
     * convencao de {@code AttackHitbox.noMundo}, e o oposto da geometria Bedrock
     * do modelo, onde a frente e -Z. Misturar as duas ja custou um bug neste
     * repositorio.</p>
     */
    private Vec3 frenteDoCorpo() {
        float radianos = this.yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(radianos), 0.0D, Mth.cos(radianos));
    }

    private double cosseno(Vec3 direcaoHorizontal, Entity outro) {
        Vec3 ateOOutro = new Vec3(outro.getX() - getX(), 0.0D, outro.getZ() - getZ());
        if (direcaoHorizontal.lengthSqr() < 1.0E-6D || ateOOutro.lengthSqr() < 1.0E-6D) {
            // Empilhado exatamente em cima do bicho nao ha angulo nenhum. Zero cai
            // no flanco, que e o meio-termo honesto: dar frente premiaria quem
            // conseguiu se sobrepor, e dar ventre entregaria o bicho por um acidente
            // de colisao.
            return 0.0D;
        }
        return Mth.clamp(direcaoHorizontal.normalize().dot(ateOOutro.normalize()), -1.0D, 1.0D);
    }

    // -------------------------------------------------------------- pincada

    /** A janela ACTIVE consulta a caixa do golpe; fora dela o golpe nao existe. */
    private void tickDaPincada(EnemyRuntime runtime) {
        if (runtime.ataques().phase() != AttackPhase.ACTIVE) return;
        LivingEntity alvo = getTarget();
        if (alvo == null || !alvo.isAlive()) return;

        Optional<AttackHit> acerto = runtime.ataques().tryHit(alvo.getId(), alvo.getBoundingBox(),
                CrabHeavyTuning.CAIXA_DA_PINCADA, position(), getYRot(), "corpo");
        if (acerto.isEmpty()) return;

        // AGARRA PRIMEIRO, MACHUCA DEPOIS -- e nao o contrario. Condicionar o
        // agarrao ao dano parece natural e quebra o mob em dois casos reais: no
        // PACIFICO o dano de mob contra jogador e zerado e hurt() devolve false, e
        // durante os ticks de invulnerabilidade de um golpe anterior tambem. Nos
        // dois o caranguejo pincaria para sempre sem nunca prender ninguem.
        boolean prendeu = tentarAgarrar(alvo);
        alvo.hurt(damageSources().mobAttack(this), acerto.get().damage());
        // Empurrao SO em quem escapou. Empurrar quem acabou de ser preso o
        // arrancaria da propria pinca, e o sintoma seria um agarrao que "as vezes
        // nao pega" -- sem nada no log.
        if (!prendeu) {
            alvo.knockback(CrabHeavyTuning.PINCADA_KNOCKBACK,
                    getX() - alvo.getX(), getZ() - alvo.getZ());
        }
    }

    /**
     * Prende o alvo, ou recusa com motivo.
     *
     * <p>A RECUSA VEM ANTES DO EFEITO. Fechar a pinca primeiro e descobrir depois
     * que o alvo nao cabe funcionaria, e deixaria o porque invisivel: o relato de
     * bug seria "as vezes ele me pinca e nao me prende", e ninguem descobriria que
     * a diferenca era o tamanho do alvo ou o caranguejo ja estar com alguem.</p>
     */
    private boolean tentarAgarrar(LivingEntity alvo) {
        if (retiradaRestante > 0 || agarrao.agarrando() || !presaValida(alvo)) return false;
        GrabRefusal recusa = agarrao.podeAgarrar(alvo.getBbHeight(), alvo.getBbWidth(),
                alvo.isPassenger(), true);
        if (recusa != GrabRefusal.NENHUMA) return false;
        agarrao.agarrar(alvo.getUUID());
        return true;
    }

    /**
     * Presa legitima: viva, atacavel, nao aliada, nao outra formiga desta especie
     * e nao alguem que ja esteja montado em outra coisa.
     *
     * <p>O filtro de espectador e criativo esta aqui, e nao no dano: prender um
     * espectador nao daria erro, so travaria em pe quem so queria assistir.</p>
     */
    private boolean presaValida(LivingEntity candidato) {
        if (candidato == null || candidato == this) return false;
        if (candidato.getType() == getType()) return false;
        if (!candidato.isAlive() || !candidato.isAttackable() || isAlliedTo(candidato)) return false;
        if (candidato.isPassenger()) return false;
        return !(candidato instanceof Player jogador && (jogador.isCreative() || jogador.isSpectator()));
    }

    // -------------------------------------------------------------- agarrao

    private void tickDoAgarrao() {
        if (!agarrao.agarrando()) return;
        LivingEntity vitima = vitimaAgarradaAgora();

        // Segurando alguem ele nao anda: arrastar a presa pelo mundo faria a
        // soltura acontecer a metros de onde o agarrao comecou -- e um frontliner
        // que anda enquanto segura deixa de estar segurando a linha.
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));

        // "Ainda presa" e medido no MUNDO e entregue ao controlador; ele nao
        // consulta nada. Segurar sem montar significa que ninguem garante que a
        // vitima continua ali: teleporte, perola, elytra e outro mod tiram a presa
        // sem avisar, e sem esta medida o caranguejo ficaria agarrando um fantasma
        // com o relogio correndo sozinho ate o fim.
        boolean aindaPresa = vitima != null && !vitima.isRemoved()
                && PINCA.aindaPresa(vitima.position().distanceTo(pontoDaPinca()));
        GrabController.GrabTick resultado = agarrao.tick(
                vitima != null && vitima.isAlive(), aindaPresa, isAlive());

        if (resultado.pulsoDeDano() && vitima != null) {
            vitima.hurt(damageSources().mobAttack(this), agarrao.regras().danoPorPulso());
        }
        if (resultado.soltou()) {
            soltar(resultado.soltura());
            return;
        }
        if (vitima != null) prenderNaPinca(vitima);
    }

    /** Onde a presa e mantida: a frente do CORPO, na altura do chao do bicho. */
    private Vec3 pontoDaPinca() {
        return PINCA.pontoDaPinca(position(), frenteDoCorpo());
    }

    /**
     * Mantem a presa na pinca -- puxando, e nao teleportando.
     *
     * <p>Teleportar todo tick seria exato e brigaria com a predicao do cliente: o
     * jogador veria a propria camera pular para a frente e para tras vinte vezes
     * por segundo. O puxao limitado ({@link RegrasDePinca#puxao}) chega no mesmo
     * lugar e deixa o jogador continuar lutando contra ele -- que e o que faz o
     * agarrao ser uma briga e nao uma cutscene.</p>
     *
     * <p>O eixo Y fica intocado de proposito: segurar a presa contra a gravidade a
     * faria tremer no ar, e soltar alguem no alto vira dano de queda que ninguem
     * pediu.</p>
     */
    private void prenderNaPinca(LivingEntity vitima) {
        Vec3 puxao = PINCA.puxao(vitima.position(), pontoDaPinca());
        if (puxao.lengthSqr() < 1.0E-8D) return;
        vitima.setDeltaMovement(puxao.x, vitima.getDeltaMovement().y, puxao.z);
        // hurtMarked faz o servidor MANDAR a velocidade nova para o dono da
        // entidade. Sem ele o jogador continua andando pela propria predicao e o
        // puxao so existe no servidor: o bicho "segura" alguem que, na tela de quem
        // foi preso, esta indo embora normalmente.
        vitima.hurtMarked = true;
    }

    /**
     * A vitima AGORA, reconstruida do uuid a cada uso.
     *
     * <p>Guardar a entidade num campo nao dava erro; so impedia o objeto de morrer.
     * Reconstruir devolve {@code null} quando ela deixou de existir, que e
     * exatamente a resposta que o tick precisa.</p>
     */
    private LivingEntity vitimaAgarradaAgora() {
        if (!(level() instanceof ServerLevel servidor)) return null;
        return agarrao.vitima()
                .map(servidor::getEntity)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .orElse(null);
    }

    /**
     * SAIDA UNICA do agarrao.
     *
     * <p>Tempo, dano, morte da vitima, morte do caranguejo, remocao da entidade,
     * interrupcao e troca de dimensao terminam TODOS aqui. Se um caminho deixar de
     * passar por aqui, o jogador fica preso para sempre -- e isso nao aparece como
     * erro, aparece como um relato de bug impossivel de reproduzir.</p>
     */
    private void soltar(GrabRelease motivo) {
        if (!agarrao.agarrando()) return;
        LivingEntity vitima = vitimaAgarradaAgora();
        agarrao.soltar(motivo);
        if (vitima != null && tocaMundo(motivo)) reposicionar(vitima);
        retiradaRestante = CrabHeavyTuning.TICKS_DE_RETIRADA;
    }

    /**
     * Saidas que NAO podem tocar em mundo.
     *
     * <p>UNLOAD e DIMENSAO acontecem quando o mundo de origem esta saindo de cena:
     * procurar bloco livre ou teleportar ali mexe num nivel que ja nao e o da
     * vitima. O {@link GrabRelease} documenta isso, e o preco de ignorar nao e uma
     * excecao -- e uma vitima teleportada para a coordenada de outra dimensao.</p>
     */
    private static boolean tocaMundo(GrabRelease motivo) {
        return motivo != GrabRelease.UNLOAD && motivo != GrabRelease.DIMENSAO;
    }

    /**
     * Onde a presa cai -- {@link SafeReleaseSpot} decide, o servidor mede.
     *
     * <p>Sem lugar livre nenhum ela fica ONDE ESTA. O javadoc do
     * {@code SafeReleaseSpot} sugere tentar de novo no proximo tick; aqui nao se
     * adia, e a razao fica escrita para nao virar descuido: adiar exigiria manter
     * vivo um agarrao que ja terminou, e "jogador preso para sempre" e pior do que
     * "jogador dentro do caranguejo por um tick" -- o empurrao natural das
     * entidades resolve o segundo sozinho.</p>
     */
    private void reposicionar(LivingEntity vitima) {
        Vec3 saida = frenteDoCorpo();
        // Vetor nulo vira NaN ao normalizar, e o SafeReleaseSpot recusa com
        // excecao. O reserva e a frente do mundo: qualquer direcao serve melhor do
        // que derrubar o tick de servidor.
        if (saida.lengthSqr() < 1.0E-6D) saida = new Vec3(0.0D, 0.0D, 1.0D);
        SafeReleaseSpot.escolher(position(), saida, CrabHeavyTuning.DISTANCIA_DE_SOLTURA,
                        CrabHeavyTuning.ALTURA_DE_ESCAPE, ponto -> cabeEm(vitima, ponto))
                .ifPresent(ponto -> vitima.teleportTo(ponto.x, ponto.y, ponto.z));
    }

    /** O teste de espaco livre, medido por quem tem o servidor na mao. */
    private boolean cabeEm(LivingEntity vitima, Vec3 ponto) {
        AABB caixa = vitima.getBoundingBox().move(ponto.x - vitima.getX(),
                ponto.y - vitima.getY(), ponto.z - vitima.getZ());
        return level().noCollision(vitima, caixa);
    }

    // ------------------------------------------------------------------ dano

    /**
     * A CARAPACA, e ela mora aqui porque aqui e onde a armadura ja era cobrada.
     *
     * <p>Este e o unico gancho do jogo entre o dano pedido e a formula de
     * armadura. Substituir o valor da placa aqui deixa a conta inteira nas maos de
     * {@link CombatRules} -- a mesma que todo mob usa, chamada uma vez so. A
     * alternativa obvia seria multiplicar o dano em {@link #hurt}, e ela e o erro
     * que este projeto ja sabe que comete: duas contas de dano no mesmo bicho
     * produzem um numero final plausivel demais para alguem notar sem medir.</p>
     *
     * <p>Dano que ignora armadura ({@code BYPASSES_ARMOR}) e dano sem atacante --
     * fogo, queda, cacto -- passam pelo caminho vanilla inteiro. Sem atacante nao
     * ha angulo, e inventar um faria queimadura contar como golpe pelas costas.</p>
     */
    @Override
    protected float getDamageAfterArmorAbsorb(DamageSource source, float amount) {
        if (level().isClientSide || source.is(DamageTypeTags.BYPASSES_ARMOR)
                || !(source.getEntity() instanceof LivingEntity atacante)) {
            return super.getDamageAfterArmorAbsorb(source, amount);
        }
        // hurtArmor nao e chamado aqui de proposito: em Mob ele e vazio (so Player
        // e ArmorStand gastam durabilidade de peca), e chama-lo por simetria criaria
        // uma dependencia do corpo vazio de um metodo do vanilla.
        float placa = CARAPACA.placaEfetiva((float) getArmorValue(), cossenoDoCorpoAte(atacante));
        return CombatRules.getDamageAfterAbsorb(this, amount, source, placa,
                (float) getAttributeValue(Attributes.ARMOR_TOUGHNESS));
    }

    /**
     * UNICO lugar que alimenta o stagger e acumula o dano de escape.
     *
     * <p>A carapaca NAO e aplicada aqui -- ela ja foi cobrada em
     * {@link #getDamageAfterArmorAbsorb}, dentro do caminho de dano do proprio
     * jogo. Reaplicar o angulo aqui seria a segunda conta.</p>
     *
     * <p>O que conta para o escape e o dano PEDIDO, antes de armadura: o preco da
     * soltura e o esforco de quem bate, e nao a defesa do caranguejo. Fosse o dano
     * final, escapar de frente -- que e de onde quem esta preso bate -- custaria
     * quase o dobro, e a janela de escape viraria decoracao para o jogador
     * sozinho.</p>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return super.hurt(source, amount);
        boolean agarrandoAntes = agarrao.agarrando();
        boolean levou = super.hurt(source, amount);
        if (!levou) return false;

        if (agarrandoAntes && Float.isFinite(amount) && amount > 0.0F) {
            agarrao.registrarDanoNoPredador(amount);
        }

        if (!(source.getEntity() instanceof LivingEntity atacante)
                || !Float.isFinite(amount) || amount <= 0.0F) {
            // Dano sem atacante nao tem angulo: passa sem stagger. Cambalear por
            // queimadura transformaria fogo em interrupcao permanente, e um
            // frontliner permanentemente interrompido nao segura nada.
            return true;
        }

        EnemyRuntime runtime = runtimeExigido();
        // O stagger le o que a CARAPACA deixou passar, e nao o dano pedido: quem
        // chega por tras interrompe mais depressa. E a segunda recompensa por
        // contornar, e a unica que o jogador VE na hora.
        FaceDaCarapaca face = CARAPACA.faceAtingida(cossenoDoCorpoAte(atacante));
        float paraOStagger = amount
                * (1.0F - CARAPACA.fracaoDaPlaca(face) * FRACAO_DE_ABSORCAO_DO_STAGGER);
        StaggerResult resultado = runtime.sofrerStagger(runtime.ataques().attackInstanceId(),
                paraOStagger, CrabHeavyTuning.RECARGA_APOS_INTERRUPCAO);
        if (resultado == StaggerResult.DISPAROU) {
            getNavigation().stop();
            this.entityData.set(CAMBALEANDO, true);
            this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
            // Interromper SOLTA quem estava preso. Sem isto, cambalear seria a unica
            // interrupcao do jogo que nao interrompe nada: o caranguejo pararia de
            // atacar e continuaria segurando, e quem bateu para libertar o amigo
            // teria feito exatamente a coisa certa sem nenhum efeito visivel.
            soltar(GrabRelease.INTERROMPIDO);
        }
        return true;
    }

    // -------------------------------------------------------- ciclo de vida

    private void publicarEstado(EnemyRuntime runtime) {
        // Segurando alguem, a fase publicada e RECOVERY pelo agarrao INTEIRO -- e
        // nao a fase real do controlador. O clipe de recuperacao e o unico com as
        // pincas fechadas a frente; publicar a fase real poria o caranguejo de
        // volta no ocio com um jogador na garra.
        int fase = agarrao.agarrando() ? AttackPhase.RECOVERY.ordinal()
                : runtime.ataques().phase().ordinal();
        if (this.entityData.get(FASE_DE_ATAQUE) != fase) this.entityData.set(FASE_DE_ATAQUE, fase);
        boolean cambaleando = runtime.stagger().cambaleando();
        if (this.entityData.get(CAMBALEANDO) != cambaleando) {
            this.entityData.set(CAMBALEANDO, cambaleando);
        }
        if (combatState() != EnemyCombatState.DYING) {
            combatState(cambaleando ? EnemyCombatState.STAGGERED
                    : getTarget() != null ? EnemyCombatState.AGGRO : EnemyCombatState.IDLE);
        }
    }

    /** Morrer SOLTA quem estava preso e PUBLICA o repouso. */
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            soltar(GrabRelease.PREDADOR_MORTO);
            publicarRepouso();
        }
        super.die(source);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide) {
            soltar(GrabRelease.UNLOAD);
            publicarRepouso();
        }
        super.remove(reason);
    }

    /**
     * Troca de dimensao solta ANTES de viajar.
     *
     * <p>Sem passar por aqui, o caranguejo chegaria do outro lado achando que ainda
     * segura alguem, e todo agarrao seguinte seria recusado para sempre por
     * JA_AGARRANDO.</p>
     */
    @Override
    public Entity changeDimension(DimensionTransition transition) {
        if (!level().isClientSide) soltar(GrabRelease.DIMENSAO);
        return super.changeDimension(transition);
    }

    private void publicarRepouso() {
        this.entityData.set(FASE_DE_ATAQUE, AttackPhase.IDLE.ordinal());
        this.entityData.set(CAMBALEANDO, false);
    }

    // -------------------------------------------------------------- as goals

    /**
     * Avanca ate o alvo, e so ate a linha.
     *
     * <p>{@code static} de proposito: todo estado que ela le mora na ENTIDADE. Um
     * campo aqui seria compartilhado por todos os caranguejos do mundo, e o
     * sintoma seria um deles defendendo a linha de outro.</p>
     */
    private static final class SegurarALinhaGoal extends Goal {
        private final CrabHeavyEntity caranguejo;

        private SegurarALinhaGoal(CrabHeavyEntity caranguejo) {
            this.caranguejo = caranguejo;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            LivingEntity alvo = caranguejo.getTarget();
            if (alvo == null || !alvo.isAlive() || caranguejo.estaAgarrando()) return false;
            if (caranguejo.runtimeExigido().stagger().cambaleando()) return false;
            if (caranguejo.runtimeExigido().ataques().phase() != AttackPhase.IDLE) return false;
            return caranguejo.distanceTo(alvo) > CrabHeavyTuning.ALCANCE_DA_PINCADA;
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void tick() {
            LivingEntity alvo = caranguejo.getTarget();
            if (alvo == null) return;
            // Olhar SEMPRE, andar so dentro da linha. Um frontliner que perde o alvo
            // de vista ao parar apresentaria o flanco a quem esta na frente dele -- e
            // a regra da carapaca puniria o proprio bicho pela coleira que ele esta
            // obedecendo.
            caranguejo.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            if (caranguejo.linha != null
                    && alvo.position().distanceTo(caranguejo.linha)
                            > CrabHeavyTuning.ALCANCE_DE_AVANCO) {
                // Fora da linha ele VOLTA, e nao so para. Parar no ponto mais avancado
                // deixaria a coleira crescer passo a passo: cada alvo que fugisse
                // puxaria o bicho mais um pouco, e a linha andaria junto com ele.
                caranguejo.getNavigation().moveTo(caranguejo.linha.x, caranguejo.linha.y,
                        caranguejo.linha.z, 1.0D);
                return;
            }
            caranguejo.getNavigation().moveTo(alvo, 1.0D);
        }

        @Override public void stop() { caranguejo.getNavigation().stop(); }
    }

    /**
     * Arma e fecha a pinca; quem mede a janela e o {@link AttackController}.
     *
     * <p><b>Ela RECUSA o golpe enquanto ele ja segura alguem, e a recusa tem
     * motivo.</b> Uma pinca, uma presa: atacar com alguem na garra faria o
     * caranguejo abrir a mao no windup sem que nada dissesse por que a primeira
     * vitima saiu.</p>
     */
    private static final class PincaGoal extends Goal {
        private final CrabHeavyEntity caranguejo;

        private PincaGoal(CrabHeavyEntity caranguejo) {
            this.caranguejo = caranguejo;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            if (caranguejo.level().isClientSide) return false;
            EnemyRuntime runtime = caranguejo.runtimeExigido();
            if (runtime.stagger().cambaleando()) return false;
            if (runtime.consciencia() == EnemyAwarenessState.FLEE) return false;
            if (caranguejo.estaAgarrando()) return false;
            LivingEntity alvo = caranguejo.getTarget();
            if (alvo == null || !alvo.isAlive()) return false;
            return caranguejo.distanceTo(alvo) <= CrabHeavyTuning.ALCANCE_DA_PINCADA
                    && caranguejo.hasLineOfSight(alvo)
                    && runtime.ataques().canStart();
        }

        @Override public boolean canContinueToUse() {
            EnemyRuntime runtime = caranguejo.runtimeExigido();
            // A CONTINUACAO NAO RECONSULTA A DISTANCIA, de proposito. Depois que a
            // pinca subiu, o golpe acontece: cancelar no meio porque o alvo recuou
            // faria o caranguejo desarmar em silencio um ataque que o jogador ja viu
            // comecar, e o telegrafo passaria a mentir. Quem recua ganha o golpe
            // ERRANDO, e nao o golpe desaparecendo.
            return runtime.ataques().phase() != AttackPhase.IDLE
                    && runtime.ataques().phase() != AttackPhase.COMPLETE
                    && !runtime.stagger().cambaleando();
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void start() {
            caranguejo.getNavigation().stop();
            // O dano sai do ATRIBUTO agora, e nao de uma constante: congelar o numero
            // aqui faria todo buff, debuff e ajuste de perfil sumirem sem aviso -- e o
            // numero do perfil viraria um botao morto.
            caranguejo.runtimeExigido().ataques().start(CrabHeavyTuning.pincada(
                    (float) caranguejo.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        }

        @Override public void tick() {
            EnemyRuntime runtime = caranguejo.runtimeExigido();
            LivingEntity alvo = caranguejo.getTarget();
            if (alvo != null && runtime.ataques().phase() == AttackPhase.WINDUP) {
                // Ele encara o alvo durante o AVISO inteiro, e isso e a ficha: a
                // carapaca vale de frente, entao virar-se E a defesa dele. Parar de
                // mirar no meio do windup entregaria o flanco de graca a quem
                // simplesmente ficou parado.
                caranguejo.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
            }
            // Um caranguejo nao desliza enquanto fecha a pinca. Sem isto ele chegaria
            // ao fim do aviso ainda carregando a inercia do avanco, e o golpe sairia
            // de um lugar diferente daquele onde foi armado -- sem erro nenhum, e com
            // o telegrafo apontando para o lugar errado.
            caranguejo.setDeltaMovement(caranguejo.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
        }

        @Override public void stop() { caranguejo.getNavigation().stop(); }
    }

    // ------------------------------------------------------------- animacao

    @Override
    public void registerControllers(
            software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<CrabHeavyEntity>(
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

    // Contrato com crab_heavy.animation.json. LoopType.DEFAULT em todos: o tipo de
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
                .then("animation.crab_heavy." + nome,
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
