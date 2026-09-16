package com.darkcontinent.nenfoundation.enemy.chimera;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * As colonias do mundo, salvas no Overworld e VERSIONADAS.
 *
 * <p><b>No Overworld, e nao em cada dimensao.</b> Uma colonia e um fato do
 * MUNDO: a formiga que atravessa um portal continua pertencendo a ela, e o teto
 * de rastreamento e do servidor inteiro. Espalhado por dimensao, o mesmo
 * servidor teria varios tetos independentes -- e a soma deles seria o dobro ou o
 * triplo do que alguem decidiu, sem nada acusando.</p>
 *
 * <p><b>Versao explicita, e futura REPROVA.</b> Ler pela metade um formato
 * desconhecido produz colonias com estagio e populacao plausiveis e errados, e o
 * sintoma seria um ninho que volta no estagio errado depois de um downgrade.</p>
 *
 * <p><b>A recuperacao offline acontece AQUI, uma vez por colonia</b>, e nao a
 * cada tick: ela existe justamente porque ninguem estava olhando, e chamar a
 * cada tick seria recuperar tempo que ja passou com o chunk ativo -- crescimento
 * em dobro, sem erro nenhum.</p>
 */
public final class ChimeraColonySavedData extends SavedData {

    public static final String DATA_ID = "nen_chimera_colonies";
    public static final int CURRENT_VERSION = 1;

    private static final Logger LOG = LoggerFactory.getLogger(ChimeraColonySavedData.class);

    private static final String VERSION = "version";
    private static final String COLONIAS = "colonies";

    private final Map<UUID, ChimeraColony> colonias = new LinkedHashMap<>();

    public static SavedData.Factory<ChimeraColonySavedData> factory() {
        return new SavedData.Factory<>(ChimeraColonySavedData::new,
                ChimeraColonySavedData::carregar, null);
    }

    /** O unico lugar onde este save nasce; sempre a partir do Overworld. */
    public static ChimeraColonySavedData de(MinecraftServer servidor) {
        ServerLevel overworld = Objects.requireNonNull(servidor, "servidor ausente")
                .getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("servidor sem Overworld: as colonias moram nele, e"
                    + " escolher outra dimensao faria o teto de rastreamento deixar de valer"
                    + " para o servidor inteiro");
        }
        return overworld.getDataStorage().computeIfAbsent(factory(), DATA_ID);
    }

    public Map<UUID, ChimeraColony> colonias() { return Map.copyOf(colonias); }

    public Optional<ChimeraColony> colonia(UUID id) {
        return Optional.ofNullable(colonias.get(Objects.requireNonNull(id, "id ausente")));
    }

    /** Evita que dois carregamentos próximos fundem a mesma colônia em silêncio. */
    public boolean existePerto(net.minecraft.core.BlockPos centro, int raio) {
        Objects.requireNonNull(centro, "centro ausente");
        if (raio < 0) throw new IllegalArgumentException("raio negativo");
        long quadrado = (long) raio * raio;
        return colonias.values().stream().anyMatch(colonia ->
                colonia.ninho().distSqr(centro) <= quadrado);
    }

    public ChimeraColony registrar(ChimeraColony colonia) {
        Objects.requireNonNull(colonia, "colonia ausente");
        ChimeraColony anterior = colonias.putIfAbsent(colonia.id(), colonia);
        if (anterior != null) {
            throw new IllegalStateException("colonia duplicada: " + colonia.id() + ". Duas"
                    + " colonias com o mesmo id competiriam pelos mesmos membros, e a segunda"
                    + " roubaria em silencio os da primeira.");
        }
        setDirty();
        return colonia;
    }

    public boolean remover(UUID id) {
        boolean removida = colonias.remove(Objects.requireNonNull(id, "id ausente")) != null;
        if (removida) setDirty();
        return removida;
    }

    /**
     * Conta quantas formigas o servidor rastreia, por lado do teto.
     *
     * <p>Contar MEMBROS DE COLONIA, e nao entidades do mundo: o teto existe para
     * limitar o que as colonias produzem, e uma formiga de comando de dev nao
     * deveria impedir a colonia de crescer. O custo esta declarado -- uma formiga
     * orfa nao entra na conta, e por isso nao ha teto sobre spawn manual.</p>
     */
    public int membrosDeColonias() {
        return colonias.values().stream().mapToInt(ChimeraColony::tamanho).sum();
    }

    /**
     * Recupera a ausencia de TODAS as colonias, uma vez, ao iniciar.
     *
     * @return quantos nascimentos o conjunto de colonias ficou autorizado a
     *         materializar -- NUMERO, e nao entidade; quem cria e quem tem mundo
     */
    public int recuperarAusencias(long tickAtual, SimulacaoOffline simulacao,
            ChimeraTrackingBudget orcamento) {
        Objects.requireNonNull(simulacao, "simulacao ausente");
        Objects.requireNonNull(orcamento, "orcamento ausente");
        int autorizados = 0;
        int peoes = membrosDeColonias();
        for (ChimeraColony colonia : colonias.values()) {
            var resultado = colonia.recuperarAusencia(simulacao, tickAtual, orcamento, peoes, 0);
            if (!resultado.rendeuAlgo()) continue;
            autorizados += resultado.nascimentosAutorizados();
            // A contagem sobe A CADA colonia processada. Sem isto, dez colonias
            // leriam a mesma populacao inicial e cada uma se acharia autorizada a
            // encher o teto sozinha -- dez vezes o limite, dentro das regras.
            peoes += resultado.nascimentosAutorizados();
            LOG.info("Colonia {} recuperou {} ciclos: +{} de comida, {} nascimentos autorizados.",
                    colonia.id(), resultado.ciclos(), resultado.comida(),
                    resultado.nascimentosAutorizados());
        }
        if (autorizados > 0) setDirty();
        return autorizados;
    }

    /**
     * Faxina de orfaos: tira das colonias os membros que o mundo nao tem mais.
     *
     * @param aindaExiste teste medido por quem tem o servidor; precisa responder
     *        {@code true} para quem esta apenas em chunk descarregado
     * @return quantos membros foram esquecidos
     */
    public int esquecerAusentes(java.util.function.Predicate<UUID> aindaExiste) {
        int total = 0;
        for (ChimeraColony colonia : colonias.values()) {
            total += colonia.esquecerAusentes(aindaExiste);
        }
        if (total > 0) setDirty();
        return total;
    }

    /** Marca para salvar; esquecer esta chamada faz tudo funcionar e nada persistir. */
    public void sujar() { setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(VERSION, CURRENT_VERSION);
        ListTag lista = new ListTag();
        for (ChimeraColony colonia : colonias.values()) lista.add(colonia.salvar());
        tag.put(COLONIAS, lista);
        return tag;
    }

    public static ChimeraColonySavedData carregar(CompoundTag tag, HolderLookup.Provider registries) {
        int versao = tag.getInt(VERSION);
        if (versao <= 0 || versao > CURRENT_VERSION) {
            throw new IllegalArgumentException("versao de save de colonias nao suportada: "
                    + versao + ". Ler pela metade produz colonias com estagio e populacao"
                    + " plausiveis e errados, e o ninho volta no estagio errado.");
        }
        ChimeraColonySavedData dados = new ChimeraColonySavedData();
        ListTag lista = tag.getList(COLONIAS, Tag.TAG_COMPOUND);
        List<String> quebradas = new ArrayList<>();
        for (int i = 0; i < lista.size(); i++) {
            try {
                ChimeraColony colonia = ChimeraColony.carregar(lista.getCompound(i));
                dados.colonias.put(colonia.id(), colonia);
            } catch (RuntimeException erro) {
                // Uma colonia quebrada NAO derruba as outras. O oposto seria
                // perder o mundo inteiro de colonias por causa de um ninho com
                // NBT corrompido -- e o log diz qual foi, para alguem poder olhar.
                quebradas.add(erro.getMessage());
            }
        }
        if (!quebradas.isEmpty()) {
            LOG.error("{} colonia(s) descartadas por NBT invalido: {}", quebradas.size(), quebradas);
        }
        return dados;
    }
}
