package com.darkcontinent.nenfoundation.command;

import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.network.NenProtocol;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;

/**
 * Como um perfil vira texto.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. E funcao PURA, separada do comando. Formatacao acoplada ao Brigadier so
 * poderia ser exercitada com um servidor de pe — e logica que so roda com o
 * jogo aberto nao e exercitada.
 *
 * <p>2. TUDO SAI ORDENADO. Conjunto e mapa nao tem ordem garantida, e um dump
 * que muda de ordem entre duas execucoes nao pode ser comparado. Comparar dois
 * dumps e a coisa mais util que se faz com eles.
 *
 * <p>3. O dump carrega as VERSOES — schema e protocolo. Um relato de bug sem
 * versao obriga a perguntar, e a resposta chega um dia depois.
 *
 * <p>4. Ele NAO carrega UUID. O nome do jogador basta para correlacionar dentro
 * de um servidor, e um dump colado numa issue publica nao precisa levar
 * identificador de conta junto.
 */
public final class RelatorioDePerfil {

    private RelatorioDePerfil() {
    }

    /** Uma linha so. Para ler no chat sem rolar a tela. */
    public static String resumo(PersistentNenData perfil) {
        return String.format(Locale.ROOT,
                "NenProfile{schema=%d, awakened=%s, category=%s, revealed=%s, "
                        + "auraPotential=%.3f, control=%.3f, output=%.3f, "
                        + "proficiencies={%s}, techniques=[%s], abilities=[%s], flags=[%s]}",
                perfil.schemaVersion(),
                perfil.awakened(),
                perfil.category().getSerializedName(),
                perfil.categoryRevealed(),
                perfil.auraPotential(),
                perfil.control(),
                perfil.output(),
                proficienciasOrdenadas(perfil.techniqueProficiency()),
                idsOrdenados(perfil.unlockedTechniques()),
                idsOrdenados(perfil.unlockedAbilities()),
                idsOrdenados(perfil.progressionFlags()));
    }

    /**
     * Varias linhas, para colar numa issue.
     *
     * <p>O objetivo e concreto: quem le o relato consegue reconstruir o estado
     * sem pedir acesso ao mundo e sem ninguem editar NBT a mao.
     */
    public static List<String> dump(String nomeDoJogador, PersistentNenData perfil) {
        List<String> linhas = new ArrayList<>();
        linhas.add("--- nen dump ---");
        linhas.add("jogador: " + nomeDoJogador);
        linhas.add("schema: " + perfil.schemaVersion()
                + " (atual: " + PersistentNenData.SCHEMA_ATUAL + ")");
        linhas.add("protocolo: v" + NenProtocol.VERSION);
        linhas.add("awakened: " + perfil.awakened());
        linhas.add("category: " + perfil.category().getSerializedName()
                + "  revealed: " + perfil.categoryRevealed()
                + "  visivel: " + perfil.categoriaVisivel().getSerializedName());
        linhas.add(String.format(Locale.ROOT, "auraPotential: %.3f  control: %.3f  output: %.3f",
                perfil.auraPotential(), perfil.control(), perfil.output()));
        linhas.add("techniques: [" + idsOrdenados(perfil.unlockedTechniques()) + "]");
        linhas.add("abilities: [" + idsOrdenados(perfil.unlockedAbilities()) + "]");
        linhas.add("flags: [" + idsOrdenados(perfil.progressionFlags()) + "]");
        linhas.add("proficiencies: {" + proficienciasOrdenadas(perfil.techniqueProficiency()) + "}");
        linhas.add("--- fim ---");
        return List.copyOf(linhas);
    }

    private static String proficienciasOrdenadas(Map<ResourceLocation, Double> proficiencias) {
        return proficiencias.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entrada -> String.format(Locale.ROOT, "%s=%.3f",
                        entrada.getKey(), entrada.getValue()))
                .collect(Collectors.joining(", "));
    }

    private static String idsOrdenados(Set<ResourceLocation> ids) {
        return ids.stream().map(Object::toString).sorted().collect(Collectors.joining(", "));
    }
}
