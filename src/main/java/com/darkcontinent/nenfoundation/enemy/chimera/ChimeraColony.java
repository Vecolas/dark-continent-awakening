package com.darkcontinent.nenfoundation.enemy.chimera;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

/**
 * UMA colonia: o ninho, quem pertence a ela, o que ela comeu e o quanto ela sabe.
 *
 * <p><b>Ela guarda UUID de membro, e nao entidade.</b> Uma colonia atravessa
 * restart e chunk descarregado; nenhuma referencia de entidade atravessa nenhum
 * dos dois. Guardar tambem impediria os objetos de morrerem -- uma colonia de
 * trinta membros seguraria trinta mobs mortos vivos na memoria, sem erro nenhum.</p>
 *
 * <p><b>Membro que some do mundo NAO some daqui sozinho.</b> A limpeza de orfaos
 * e uma operacao explicita ({@link #esquecerAusentes}), chamada por quem TEM o
 * mundo para perguntar quem ainda existe. Deixar a colonia "adivinhar" faria ela
 * apagar do registro quem esta apenas dormindo num chunk descarregado -- e o
 * membro voltaria como orfao, alimentando uma colonia fantasma.</p>
 *
 * <p><b>O relatorio de batedor tem PRAZO.</b> Sem expiracao, a colonia lembraria
 * para sempre de um jogador que passou por ali uma vez, e o alerta nunca baixaria
 * -- uma colonia permanentemente furiosa, sem nada explicando o motivo.</p>
 */
public final class ChimeraColony {

    /** Alerta maximo; o mesmo teto que {@link ChimeraColonyState} ja cobra. */
    public static final int ALERTA_MAXIMO = 4;

    private static final String ID = "id";
    private static final String RAINHA = "queen";
    private static final String NINHO = "nest";
    private static final String ESTAGIO = "stage";
    private static final String COMIDA = "food";
    private static final String ALERTA = "alert";
    private static final String MEMBROS = "members";
    private static final String GENES = "gene_pool";
    private static final String ULTIMO_TICK = "last_tick";

    private final UUID id;
    private UUID rainha;
    private BlockPos ninho;
    private ChimeraColonyStage estagio = ChimeraColonyStage.FOUNDED;
    private int comida;
    private int alerta;
    private long ultimoTick;

    private final Set<UUID> membros = new LinkedHashSet<>();
    private final Map<String, Integer> genePool = new LinkedHashMap<>();
    /** uuid da ameaca -> tick em que o relatorio expira. */
    private final Map<UUID, Long> relatorios = new LinkedHashMap<>();

    public ChimeraColony(UUID id, BlockPos ninho) {
        this.id = Objects.requireNonNull(id, "colonia sem id");
        this.ninho = Objects.requireNonNull(ninho, "colonia sem ninho: o ninho e a ancora de"
                + " tudo -- spawn, alcance de patrulha e a propria existencia da colonia").immutable();
    }

    public UUID id() { return id; }
    public BlockPos ninho() { return ninho; }
    public Optional<UUID> rainha() { return Optional.ofNullable(rainha); }
    public ChimeraColonyStage estagio() { return estagio; }
    public int comida() { return comida; }
    public int alerta() { return alerta; }
    public long ultimoTick() { return ultimoTick; }
    public Set<UUID> membros() { return Set.copyOf(membros); }
    public Map<String, Integer> genePool() { return Map.copyOf(genePool); }
    public int tamanho() { return membros.size(); }

    public void rainha(UUID rainha) { this.rainha = rainha; }
    public void ninho(BlockPos ninho) {
        this.ninho = Objects.requireNonNull(ninho, "ninho ausente").immutable();
    }

    /**
     * Avanca o estagio. MONOTONO: uma colonia nao volta a ser recem-fundada.
     *
     * <p>Permitir a volta faria o estagio oscilar com a populacao -- perdeu tres
     * membros, voltou a NEST; ganhou tres, subiu de novo -- e o jogador veria a
     * colonia trocando de comportamento sem causa visivel.</p>
     */
    public void estagio(ChimeraColonyStage proximo) {
        Objects.requireNonNull(proximo, "estagio ausente");
        if (proximo.ordinal() > estagio.ordinal()) estagio = proximo;
    }

    public boolean alistar(UUID membro) {
        return membros.add(Objects.requireNonNull(membro, "membro ausente"));
    }

    public boolean desligar(UUID membro) {
        return membros.remove(Objects.requireNonNull(membro, "membro ausente"));
    }

    /**
     * Tira do registro os membros que o MUNDO diz que nao existem mais.
     *
     * @param aindaExiste teste medido por quem tem o servidor; ele precisa
     *        responder {@code true} para quem esta apenas em chunk descarregado
     * @return quantos foram esquecidos
     */
    public int esquecerAusentes(java.util.function.Predicate<UUID> aindaExiste) {
        Objects.requireNonNull(aindaExiste, "teste de existencia ausente");
        int antes = membros.size();
        membros.removeIf(membro -> !aindaExiste.test(membro));
        return antes - membros.size();
    }

    /** Comer alimenta o gene pool E o placar; as duas coisas sempre juntas. */
    public void alimentar(String especie, int quantidade) {
        if (especie == null || especie.isBlank() || quantidade <= 0) {
            throw new IllegalArgumentException("presa invalida");
        }
        genePool.merge(especie, quantidade, Integer::sum);
        comida += quantidade;
    }

    /** Consome comida; nunca abaixo de zero. */
    public boolean gastar(int quantidade) {
        if (quantidade < 0) throw new IllegalArgumentException("gasto negativo");
        if (comida < quantidade) return false;
        comida -= quantidade;
        return true;
    }

    /**
     * Um batedor relatou uma ameaca. O relatorio VENCE.
     *
     * @param ameaca quem foi visto
     * @param tickAtual relogio do servidor
     * @param duracao por quantos ticks o relatorio vale
     */
    public void relatar(UUID ameaca, long tickAtual, int duracao) {
        Objects.requireNonNull(ameaca, "ameaca ausente");
        if (duracao < 1) throw new IllegalArgumentException("relatorio sem duracao");
        relatorios.put(ameaca, tickAtual + duracao);
        alerta = Math.min(ALERTA_MAXIMO, alerta + 1);
    }

    /**
     * Expira relatorios vencidos e baixa o alerta quando nao sobra nenhum.
     *
     * @return quantos relatorios venceram
     */
    public int expirarRelatorios(long tickAtual) {
        int antes = relatorios.size();
        relatorios.values().removeIf(prazo -> prazo <= tickAtual);
        if (relatorios.isEmpty() && alerta > 0) alerta--;
        return antes - relatorios.size();
    }

    public Set<UUID> ameacasConhecidas() { return Set.copyOf(relatorios.keySet()); }

    /**
     * Aplica o rendimento de uma ausencia.
     *
     * <p>A colonia so ATUALIZA O PROPRIO PLACAR; ela nao cria formiga. Quem
     * materializa e o lado que tem mundo, lendo
     * {@code ResultadoOffline.nascimentosAutorizados}.</p>
     */
    public SimulacaoOffline.ResultadoOffline recuperarAusencia(SimulacaoOffline simulacao,
            long tickAtual, ChimeraTrackingBudget orcamento, int peoesVivos, int oficiaisVivos) {
        Objects.requireNonNull(simulacao, "simulacao ausente");
        long decorridos = Math.max(0L, tickAtual - ultimoTick);
        SimulacaoOffline.ResultadoOffline resultado =
                simulacao.recuperar(decorridos, orcamento, peoesVivos, oficiaisVivos);
        comida += resultado.comida();
        ultimoTick = tickAtual;
        return resultado;
    }

    /** Marca o relogio sem recuperar nada -- usado no tick normal, com o chunk ativo. */
    public void marcarTick(long tickAtual) { this.ultimoTick = tickAtual; }

    // ------------------------------------------------------------ persistencia

    public CompoundTag salvar() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(ID, id);
        if (rainha != null) tag.putUUID(RAINHA, rainha);
        tag.put(NINHO, NbtUtils.writeBlockPos(ninho));
        tag.putString(ESTAGIO, estagio.name());
        tag.putInt(COMIDA, comida);
        tag.putInt(ALERTA, alerta);
        tag.putLong(ULTIMO_TICK, ultimoTick);

        ListTag lista = new ListTag();
        membros.forEach(membro -> lista.add(NbtUtils.createUUID(membro)));
        tag.put(MEMBROS, lista);

        CompoundTag genes = new CompoundTag();
        // Ordenado: um Map sem ordem faria o mesmo save produzir NBT diferente a
        // cada gravacao, e todo diff de mundo viraria ruido.
        genePool.keySet().stream().sorted().forEach(especie -> genes.putInt(especie,
                genePool.get(especie)));
        tag.put(GENES, genes);
        return tag;
    }

    public static ChimeraColony carregar(CompoundTag tag) {
        Objects.requireNonNull(tag, "nbt ausente");
        BlockPos ninho = NbtUtils.readBlockPos(tag, NINHO).orElseThrow(
                () -> new IllegalArgumentException("colonia salva sem ninho: ela carregaria sem"
                        + " ancora, e todo alcance medido a partir dela sairia de lugar nenhum"));
        ChimeraColony colonia = new ChimeraColony(tag.getUUID(ID), ninho);
        if (tag.hasUUID(RAINHA)) colonia.rainha = tag.getUUID(RAINHA);
        colonia.estagio = ChimeraColonyStage.valueOf(tag.getString(ESTAGIO));
        colonia.comida = tag.getInt(COMIDA);
        colonia.alerta = Math.min(ALERTA_MAXIMO, Math.max(0, tag.getInt(ALERTA)));
        colonia.ultimoTick = tag.getLong(ULTIMO_TICK);

        ListTag lista = tag.getList(MEMBROS, Tag.TAG_INT_ARRAY);
        for (Tag membro : lista) colonia.membros.add(NbtUtils.loadUUID(membro));

        CompoundTag genes = tag.getCompound(GENES);
        for (String especie : genes.getAllKeys()) {
            colonia.genePool.put(especie, genes.getInt(especie));
        }
        // Relatorios NAO sao salvos de proposito: eles sao memoria de curto prazo
        // com prazo em ticks de mundo, e um prazo salvo atravessaria um restart
        // com o relogio zerado do outro lado -- a colonia acordaria em alerta
        // maximo por uma ameaca de uma sessao anterior.
        return colonia;
    }
}
