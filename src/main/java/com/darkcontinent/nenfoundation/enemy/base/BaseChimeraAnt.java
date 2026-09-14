package com.darkcontinent.nenfoundation.enemy.base;

import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.api.EnemyMetadata;
import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraDefinition;
import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraIdentity;
import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraNenStatus;
import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenController;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/**
 * A base de TODA formiga: identidade gerada, filiacao e o Nen tatico.
 *
 * <p><b>Ela existe pela razao que a issue #120 escreve com todas as letras:</b>
 * no maximo tres {@code EntityType} por rank, e nunca um por recolor. Sem uma
 * base comum, cada familia de formiga reimplementaria identidade, colonia,
 * esquadrao e persistencia -- e a decima esqueceria de salvar um dos quatro. O
 * esquecido nao daria erro: daria uma formiga que volta do save sem saber de
 * quem e.</p>
 *
 * <p><b>A identidade e GERADA no primeiro tick de servidor, e nunca no
 * construtor.</b> O construtor roda tambem no cliente e na leitura de save, e
 * sortear ali produziria uma formiga diferente em cada lado -- o cliente
 * desenharia traits que o servidor nao tem. Aqui o sorteio acontece uma vez, no
 * servidor, e o resultado vai para o NBT.</p>
 *
 * <p><b>A filiacao nao e a identidade.</b> Colonia e esquadrao mudam ao longo da
 * vida da formiga; rank, morfologia e traits, nao. Guardar os dois no mesmo
 * lugar faria um {@code set} de colonia parecer capaz de trocar o corpo do
 * bicho.</p>
 */
public abstract class BaseChimeraAnt extends BaseHxHMob {

    private static final String NBT_IDENTIDADE = "chimera_identity";

    private final ChimeraDefinition molde;
    private ChimeraIdentity identidade;
    private TacticalNenController nenTatico;

    protected BaseChimeraAnt(EntityType<? extends PathfinderMob> type, Level level,
            EnemyMetadata metadata, AwarenessTuning tuning, ChimeraDefinition molde) {
        super(type, level, metadata, tuning);
        this.molde = Objects.requireNonNull(molde, "formiga sem molde: ela nao saberia que"
                + " traits pode ter, e o sorteio nasceria de uma lista vazia");
    }

    public final ChimeraDefinition molde() { return molde; }

    /**
     * A identidade desta formiga. Vazia ate o primeiro tick de servidor.
     *
     * <p>Vazio nao e erro: e o estado legitimo de uma entidade que ainda nao
     * nasceu do lado que decide. Quem ler isto no cliente antes da sincronizacao
     * recebe vazio, e tratar vazio como "sem traits" e a leitura certa.</p>
     */
    public final Optional<ChimeraIdentity> identidade() { return Optional.ofNullable(identidade); }

    public final Optional<UUID> colonia() {
        return identidade().flatMap(ChimeraIdentity::colonia);
    }

    public final Optional<UUID> esquadrao() {
        return identidade().flatMap(ChimeraIdentity::squad);
    }

    public final ChimeraNenStatus estagioDeNen() {
        return identidade().map(ChimeraIdentity::nen).orElse(ChimeraNenStatus.DORMANT);
    }

    /** O controlador tatico; vazio enquanto a identidade nao existir. */
    public final Optional<TacticalNenController> nenTatico() {
        return Optional.ofNullable(nenTatico);
    }

    /**
     * Garante identidade -- sorteando SO no servidor, e so uma vez.
     *
     * <p>A semente sai do uuid da entidade. Ela e estavel entre execucoes e
     * diferente por bicho, que sao as duas propriedades que o sorteio precisa:
     * sem estabilidade, um servidor e seu backup divergem; sem variacao, o gene
     * pool inteiro viraria decoracao.</p>
     */
    protected final void garantirIdentidade() {
        if (level().isClientSide || identidade != null) return;
        aplicarIdentidade(molde.sortear(getUUID().getMostSignificantBits()
                ^ getUUID().getLeastSignificantBits()));
    }

    /** Troca a identidade inteira e refaz o controlador tatico junto. */
    protected final void aplicarIdentidade(ChimeraIdentity nova) {
        this.identidade = Objects.requireNonNull(nova, "identidade ausente");
        // O controlador e refeito, e nao atualizado: o estagio de Nen e final
        // dentro dele, e um controlador que "aprende" o estagio no meio da vida
        // teria duas verdades sobre a mesma coisa.
        this.nenTatico = new TacticalNenController(nova.nen());
    }

    /** Alista numa colonia. Devolve a identidade nova, para quem quiser registrar. */
    public final ChimeraIdentity alistarEm(UUID colonia) {
        garantirIdentidade();
        aplicarIdentidade(exigirIdentidade().comColonia(colonia));
        return identidade;
    }

    public final ChimeraIdentity alistarNoEsquadrao(UUID esquadrao) {
        garantirIdentidade();
        aplicarIdentidade(exigirIdentidade().comSquad(esquadrao));
        return identidade;
    }

    /**
     * Desliga da colonia SEM apagar a formiga.
     *
     * <p>Colonia destruida nao mata quem estava fora do ninho. A formiga vira
     * orfa e continua existindo -- apagar junto faria entidades sumirem na frente
     * do jogador sem motivo visivel.</p>
     */
    public final void tornarOrfa() {
        if (identidade != null) aplicarIdentidade(identidade.semColonia());
    }

    private ChimeraIdentity exigirIdentidade() {
        if (identidade == null) {
            throw new IllegalStateException(getType().getDescriptionId() + " sem identidade:"
                    + " garantirIdentidade() precisa rodar no servidor antes de qualquer"
                    + " alistamento");
        }
        return identidade;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // Sem esta linha, a formiga volta do save sorteando de novo: mesmo nome,
        // corpo diferente, e nenhum erro em lugar nenhum.
        if (identidade != null) tag.put(NBT_IDENTIDADE, identidade.salvar());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(NBT_IDENTIDADE)) {
            aplicarIdentidade(ChimeraIdentity.carregar(tag.getCompound(NBT_IDENTIDADE)));
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        // O primeiro tick de servidor e o lugar do sorteio: aqui a entidade ja
        // tem uuid definitivo e o lado e o autoritativo.
        garantirIdentidade();
    }
}
