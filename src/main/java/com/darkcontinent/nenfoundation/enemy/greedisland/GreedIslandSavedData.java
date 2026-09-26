package com.darkcontinent.nenfoundation.enemy.greedisland;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * O que Greed Island guarda por MUNDO. Secao 89, fase G13.
 *
 * <p><b>O QUE ELE GUARDA E MINUSCULO, e isso e o desenho.</b> A secao 106
 * limita o uso de disco, e a razao e estrutural: o layout macro e uma FUNCAO,
 * nao um mapa. A ilha inteira -- costa, cordilheiras, rios, regioes, estradas,
 * cidades e landmarks -- e recalculavel a partir de constantes, e guardar isso
 * seria salvar um numero que ja se sabe.
 *
 * <p>O que NAO e recalculavel, e por isso mora aqui:
 *
 * <ul>
 *   <li><b>a versao do layout</b> com que este mundo nasceu. Sem ela, uma
 *       atualizacao que mude a geografia reescreveria o mapa que o jogador
 *       aprendeu, e os chunks ja gerados nao combinariam com os novos --
 *       costura visivel de terreno, sem erro nenhum;
 *   <li><b>a versao do mod</b> que criou o mundo, para o diagnostico de quem
 *       receber um relato de bug meses depois.
 * </ul>
 *
 * <p>O QUE NAO ENTRA AQUI, e a secao 89 lista: o mapa de terreno inteiro.
 * Guardar altura por coluna de uma ilha de 80.000 x 70.000 seriam bilhoes de
 * valores.
 *
 * <p>O progresso de descoberta do JOGADOR nao mora aqui -- ele e por pessoa, e
 * a secao 90 o separa de proposito: dois jogadores no mesmo servidor conhecem
 * lugares diferentes.
 */
public final class GreedIslandSavedData extends SavedData {

    /** O nome do arquivo dentro de {@code data/}. */
    public static final String NOME = "greed_island";

    private final int versaoDoLayout;
    private final String versaoDoMod;

    private GreedIslandSavedData(int versaoDoLayout, String versaoDoMod) {
        this.versaoDoLayout = versaoDoLayout;
        this.versaoDoMod = versaoDoMod;
    }

    /** Um mundo novo, nascendo na versao de layout de hoje. */
    public static GreedIslandSavedData novo() {
        return new GreedIslandSavedData(GreedIslandLayoutVersion.ATUAL,
                versaoDoModAtual());
    }

    /**
     * A leitura, defensiva.
     *
     * <p>CAMPO AUSENTE VIRA ZERO, e zero e o prototipo: um save gravado antes
     * de este arquivo existir NAO pode ser tratado como se tivesse nascido na
     * versao de hoje -- ele nasceu no disco de andaime, e a geografia dele nao
     * existe mais.
     */
    public static GreedIslandSavedData ler(CompoundTag tag, HolderLookup.Provider registros) {
        return new GreedIslandSavedData(
                tag.getInt("versaoDoLayout"),
                tag.contains("versaoDoMod") ? tag.getString("versaoDoMod") : "desconhecida");
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
        tag.putInt("versaoDoLayout", this.versaoDoLayout);
        tag.putString("versaoDoMod", this.versaoDoMod);
        return tag;
    }

    /** A versao de layout com que este mundo nasceu. */
    public int versaoDoLayout() {
        return this.versaoDoLayout;
    }

    /** A versao do mod que criou o mundo. Diagnostico, e nao regra. */
    public String versaoDoMod() {
        return this.versaoDoMod;
    }

    /**
     * Se a geografia deste mundo ainda combina com a do codigo.
     *
     * <p><b>QUEM DESCOBRE A DIVERGENCIA NAO A CONSERTA.</b> Reescrever chunk
     * gerado seria pior que o problema: metade do mapa mudaria e a outra
     * metade nao, e a costura ficaria visivel para sempre. A resposta certa e
     * avisar, e a secao 128 manda apagar e regenerar durante o
     * desenvolvimento.
     */
    public boolean geografiaCombina() {
        return this.versaoDoLayout == GreedIslandLayoutVersion.ATUAL;
    }

    /**
     * Se este mundo pode ser considerado permanente.
     *
     * <p>Enquanto o layout for prototipo, a resposta e nao -- e o aviso da
     * secao 129 existe justamente para isso nao ser esquecido.
     */
    public boolean ehPermanente() {
        return this.versaoDoLayout >= GreedIslandLayoutVersion.PRIMEIRA_CONGELADA;
    }

    /**
     * A versao do mod, ou "desconhecida" quando nao da para saber.
     *
     * <p><b>DEFENSIVA, e o portao mostrou que precisa.</b> {@code ModList.get()}
     * devolve NULO fora de um jogo carregado -- na suite de testes, e tambem
     * durante a inicializacao, antes de a lista existir. A versao aqui e
     * DIAGNOSTICO, e nao regra: derrubar a criacao de um mundo porque nao deu
     * para carimbar a versao seria trocar uma informacao util por uma falha.
     */
    private static String versaoDoModAtual() {
        try {
            var lista = net.neoforged.fml.ModList.get();
            if (lista == null) {
                return "desconhecida";
            }
            return lista
                    .getModContainerById(
                            com.darkcontinent.nenfoundation.NenFoundation.MOD_ID)
                    .map(c -> c.getModInfo().getVersion().toString())
                    .orElse("desconhecida");
        } catch (RuntimeException fora) {
            return "desconhecida";
        }
    }
}
