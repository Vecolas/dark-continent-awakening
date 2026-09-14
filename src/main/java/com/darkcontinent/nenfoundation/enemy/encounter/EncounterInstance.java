package com.darkcontinent.nenfoundation.enemy.encounter;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * UM episodio de encontro: onde, em que estado, com quem, e com quais entidades.
 *
 * <p><b>Ela guarda UUID de entidade, nunca a entidade.</b> Um episodio atravessa
 * o restart do servidor, e nenhuma referencia de entidade atravessa isso. Com o
 * uuid, ao voltar o controlador pergunta ao mundo quem ainda existe -- e a
 * resposta "ninguem" e justamente o que dispara o respawn sem duplicar.</p>
 *
 * <p><b>O ancoradouro tem dimensao junto.</b> Um {@link BlockPos} sozinho e
 * ambiguo entre dimensoes: o mesmo x,y,z existe no Overworld e na World Tree, e
 * um encontro que perde a dimensao no save volta ancorado no lugar errado -- sem
 * erro, so com o chefe do pantano nascendo dentro da arvore.</p>
 */
public final class EncounterInstance {

    private final UUID id;
    private final String definitionId;
    private final ResourceKey<Level> dimensao;
    private final BlockPos ancora;

    private EncounterState estado = EncounterState.DORMANT;
    private int ticksNoEstado;
    private final Set<UUID> participantes = new LinkedHashSet<>();
    private final Set<UUID> entidades = new LinkedHashSet<>();

    public EncounterInstance(UUID id, String definitionId, ResourceKey<Level> dimensao, BlockPos ancora) {
        this.id = Objects.requireNonNull(id, "id de encontro ausente");
        if (definitionId == null || definitionId.isBlank()) {
            throw new IllegalArgumentException("encontro sem definitionId: ele carregaria do save"
                    + " e nao saberia o que spawnar, e isso aparece como um ancoradouro vazio");
        }
        this.definitionId = definitionId;
        this.dimensao = Objects.requireNonNull(dimensao, "dimensao do encontro ausente");
        this.ancora = Objects.requireNonNull(ancora, "ancora do encontro ausente").immutable();
    }

    public UUID id() { return id; }
    public String definitionId() { return definitionId; }
    public ResourceKey<Level> dimensao() { return dimensao; }
    public BlockPos ancora() { return ancora; }
    public EncounterState estado() { return estado; }
    public int ticksNoEstado() { return ticksNoEstado; }
    public Set<UUID> participantes() { return Set.copyOf(participantes); }
    public Set<UUID> entidades() { return Set.copyOf(entidades); }

    /**
     * Troca de estado, cobrada pela tabela de transicoes.
     *
     * <p>O contador de ticks ZERA na troca. Nao zerar faria o relogio de cooldown
     * herdar o tempo em que o encontro esteve ativo, e a recarga acabaria antes de
     * comecar -- sem erro nenhum, so um encontro que reaparece rapido demais.</p>
     */
    public void estado(EncounterState novo) {
        EncounterTransitions.exigir(estado, novo);
        this.estado = novo;
        this.ticksNoEstado = 0;
        if (novo == EncounterState.ARMED || novo == EncounterState.DORMANT) {
            // Voltar ao inicio limpa o EPISODIO inteiro. Herdar participantes de
            // uma rodada anterior faria a segunda comecar "ja com gente dentro", e
            // o primeiro tick a concluiria sozinha.
            participantes.clear();
            entidades.clear();
        }
    }

    public void tick() { ticksNoEstado++; }

    /** @return true se este jogador ainda nao estava no episodio */
    public boolean entrar(UUID jogador) {
        Objects.requireNonNull(jogador, "participante ausente");
        if (estado.terminou()) return false;
        return participantes.add(jogador);
    }

    public boolean sair(UUID jogador) {
        return participantes.remove(Objects.requireNonNull(jogador, "participante ausente"));
    }

    public void registrarEntidade(UUID entidade) {
        entidades.add(Objects.requireNonNull(entidade, "entidade ausente"));
    }

    public boolean esquecerEntidade(UUID entidade) {
        return entidades.remove(Objects.requireNonNull(entidade, "entidade ausente"));
    }

    /** Carga do save: estado e contador entram SEM passar pela tabela de transicao. */
    void restaurar(EncounterState estado, int ticksNoEstado, Set<UUID> participantes, Set<UUID> entidades) {
        this.estado = Objects.requireNonNull(estado, "estado ausente no save");
        if (ticksNoEstado < 0) throw new IllegalArgumentException("ticks negativos no save");
        this.ticksNoEstado = ticksNoEstado;
        this.participantes.clear();
        this.participantes.addAll(participantes);
        this.entidades.clear();
        this.entidades.addAll(entidades);
    }
}
