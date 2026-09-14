package com.darkcontinent.nenfoundation.enemy.encounter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Os encontros do mundo inteiro, salvos no Overworld e VERSIONADOS.
 *
 * <p><b>Por que no Overworld e nao em cada dimensao.</b> Um encontro de Greed
 * Island precisa ser conhecido por quem esta no Overworld -- e o ledger de
 * recompensas precisa ser UM so. Espalhado por dimensao, o mesmo premio poderia
 * ser travado numa e pago na outra, e a trava nao daria erro: daria um item
 * duplicado que so aparece no bau de quem soube atravessar o portal na hora
 * certa.</p>
 *
 * <p><b>Versao explicita, e futura reprova.</b> Um save de versao maior nao e
 * lido "na melhor das hipoteses": ele e recusado. Ler pela metade um formato que
 * nao se conhece produz encontros com estado plausivel e errado, e o sintoma e
 * uma recompensa paga de novo depois de um downgrade.</p>
 */
public final class EncounterSavedData extends SavedData {

    public static final String DATA_ID = "nen_encounters";
    public static final int CURRENT_VERSION = 1;

    private static final String VERSION = "version";
    private static final String INSTANCIAS = "instances";
    private static final String TRAVAS = "reward_locks";

    private static final String ID = "id";
    private static final String DEFINICAO = "definition";
    private static final String DIMENSAO = "dimension";
    private static final String ANCORA = "anchor";
    private static final String ESTADO = "state";
    private static final String TICKS = "ticks_in_state";
    private static final String PARTICIPANTES = "participants";
    private static final String ENTIDADES = "entities";

    private final Map<UUID, EncounterInstance> instancias = new LinkedHashMap<>();
    private final RewardLedger ledger = new RewardLedger();

    public static SavedData.Factory<EncounterSavedData> factory() {
        return new SavedData.Factory<>(EncounterSavedData::new, EncounterSavedData::carregar, null);
    }

    /** O unico lugar onde este save nasce; sempre a partir do Overworld. */
    public static EncounterSavedData de(MinecraftServer servidor) {
        ServerLevel overworld = servidor.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("servidor sem Overworld: o save de encontros mora"
                    + " nele, e escolher outra dimensao faria o ledger de recompensas deixar"
                    + " de ser unico");
        }
        return overworld.getDataStorage().computeIfAbsent(factory(), DATA_ID);
    }

    public Map<UUID, EncounterInstance> instancias() { return Map.copyOf(instancias); }

    public RewardLedger ledger() { return ledger; }

    public EncounterInstance registrar(EncounterInstance instancia) {
        EncounterInstance anterior = instancias.putIfAbsent(instancia.id(), instancia);
        if (anterior != null) {
            throw new IllegalStateException("encontro duplicado: " + instancia.id()
                    + ". Registrar duas vezes o mesmo id faria dois episodios competirem pelo"
                    + " mesmo ancoradouro, e o segundo spawnaria em cima do primeiro.");
        }
        setDirty();
        return instancia;
    }

    public EncounterInstance remover(UUID id) {
        EncounterInstance removida = instancias.remove(id);
        if (removida != null) setDirty();
        return removida;
    }

    /**
     * Marca para salvar.
     *
     * <p>Esquecer esta chamada e a falha mais silenciosa deste arquivo inteiro:
     * tudo funciona na sessao, e nada persiste. O relato de bug vira "o encontro
     * reseta sozinho quando eu volto ao mundo".</p>
     */
    public void sujar() { setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(VERSION, CURRENT_VERSION);

        ListTag lista = new ListTag();
        for (EncounterInstance instancia : instancias.values()) {
            CompoundTag entrada = new CompoundTag();
            entrada.putUUID(ID, instancia.id());
            entrada.putString(DEFINICAO, instancia.definitionId());
            entrada.putString(DIMENSAO, instancia.dimensao().location().toString());
            entrada.put(ANCORA, NbtUtils.writeBlockPos(instancia.ancora()));
            entrada.putString(ESTADO, instancia.estado().name());
            entrada.putInt(TICKS, instancia.ticksNoEstado());
            entrada.put(PARTICIPANTES, uuids(instancia.participantes()));
            entrada.put(ENTIDADES, uuids(instancia.entidades()));
            lista.add(entrada);
        }
        tag.put(INSTANCIAS, lista);

        ListTag travas = new ListTag();
        for (String chave : ledger.chaves()) travas.add(net.minecraft.nbt.StringTag.valueOf(chave));
        tag.put(TRAVAS, travas);
        return tag;
    }

    private static ListTag uuids(Set<UUID> valores) {
        ListTag lista = new ListTag();
        for (UUID valor : valores) lista.add(NbtUtils.createUUID(valor));
        return lista;
    }

    private static Set<UUID> lerUuids(ListTag lista) {
        Set<UUID> valores = new LinkedHashSet<>();
        for (Tag entrada : lista) valores.add(NbtUtils.loadUUID(entrada));
        return valores;
    }

    public static EncounterSavedData carregar(CompoundTag tag, HolderLookup.Provider registries) {
        int versao = tag.getInt(VERSION);
        if (versao <= 0 || versao > CURRENT_VERSION) {
            throw new IllegalArgumentException("versao de save de encontros nao suportada: "
                    + versao + ". Ler pela metade um formato desconhecido produz encontros com"
                    + " estado plausivel e errado, e o sintoma e recompensa paga de novo.");
        }
        EncounterSavedData dados = new EncounterSavedData();

        ListTag lista = tag.getList(INSTANCIAS, Tag.TAG_COMPOUND);
        for (int i = 0; i < lista.size(); i++) {
            CompoundTag entrada = lista.getCompound(i);
            ResourceLocation dimensao = ResourceLocation.parse(entrada.getString(DIMENSAO));
            BlockPos ancora = NbtUtils.readBlockPos(entrada, ANCORA).orElseThrow(
                    () -> new IllegalArgumentException("encontro salvo sem ancora"));
            EncounterInstance instancia = new EncounterInstance(entrada.getUUID(ID),
                    entrada.getString(DEFINICAO), ResourceKey.create(Registries.DIMENSION, dimensao),
                    ancora);
            instancia.restaurar(EncounterState.valueOf(entrada.getString(ESTADO)),
                    entrada.getInt(TICKS),
                    lerUuids(entrada.getList(PARTICIPANTES, Tag.TAG_INT_ARRAY)),
                    lerUuids(entrada.getList(ENTIDADES, Tag.TAG_INT_ARRAY)));
            dados.instancias.put(instancia.id(), instancia);
        }

        List<String> travas = new ArrayList<>();
        ListTag salvas = tag.getList(TRAVAS, Tag.TAG_STRING);
        for (int i = 0; i < salvas.size(); i++) travas.add(salvas.getString(i));
        dados.ledger.carregar(travas);
        return dados;
    }
}
