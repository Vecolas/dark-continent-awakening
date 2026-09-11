package com.darkcontinent.nenfoundation.data.definition;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.category.MatrizDeAfinidade;
import com.darkcontinent.nenfoundation.nen.category.NenAffinity;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Le a matriz de afinidade do datapack e a entrega ao {@link NenAffinity}.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. UM ARQUIVO, UM ID. A matriz inteira mora em
 * {@code data/<ns>/nen_afinidade/matriz.json}. Um datapack que queira mudar
 * afinidade SUBSTITUI o arquivo todo, e nao mescla celulas. Mesclagem parcial
 * de uma tabela e o tipo de coisa que funciona no teste com um pack e produz
 * uma matriz meio de cada no servidor com quatro -- sem erro nenhum.
 *
 * <p>2. RECARGA QUE FALHA NAO APAGA A MATRIZ BOA. Se o JSON novo estiver
 * quebrado, o servidor registra o motivo e CONTINUA com o que ja estava
 * carregado. O contrario -- limpar e ficar sem matriz -- transformaria um erro
 * de digitacao num {@code /reload} em todo mundo com afinidade zero, no meio de
 * uma sessao.
 *
 * <p>3. O ERRO E BARULHENTO E ACIONAVEL. Ele diz o arquivo, e diz a lista
 * INTEIRA de pares faltando ou sobrando -- nao o primeiro. Quem edita datapack
 * quer os cinco erros de uma vez, e nao cinco recargas.
 *
 * <p>4. ELE NAO TOCA EM PERFIL DE JOGADOR. Nem por engano: nao ha aqui
 * nenhuma referencia a jogador. A matriz e sobre categorias, nao sobre pessoas.
 * E o que sustenta o criterio "recarregar o datapack com um jogador online nao
 * altera o perfil dele".
 */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class CarregadorDeAfinidade extends SimpleJsonResourceReloadListener {

    private static final Logger LOG = LoggerFactory.getLogger(CarregadorDeAfinidade.class);

    /** A pasta dentro de {@code data/<namespace>/}. */
    public static final String PASTA = "nen_afinidade";

    /** O id do unico arquivo que este carregador le. */
    public static final ResourceLocation ID_DA_MATRIZ = NenFoundation.id("matriz");

    private static final Gson GSON = new Gson();

    public CarregadorDeAfinidade() {
        super(GSON, PASTA);
    }

    @SubscribeEvent
    public static void aoRegistrarRecarregadores(AddReloadListenerEvent evento) {
        evento.addListener(new CarregadorDeAfinidade());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> arquivos,
            ResourceManager gerenciador, ProfilerFiller profiler) {

        JsonElement json = arquivos.get(ID_DA_MATRIZ);
        if (json == null) {
            LOG.error("Nao achei a matriz de afinidade em data/{}/{}/{}.json."
                            + " Nenhuma matriz nova foi aplicada; a anterior, se"
                            + " havia, continua em uso.",
                    ID_DA_MATRIZ.getNamespace(), PASTA, ID_DA_MATRIZ.getPath());
            return;
        }

        MatrizDeAfinidade.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(erro -> LOG.error(
                        "A matriz de afinidade em data/{}/{}/{}.json nao pode ser"
                                + " lida: {}. Nenhuma matriz nova foi aplicada.",
                        ID_DA_MATRIZ.getNamespace(), PASTA, ID_DA_MATRIZ.getPath(), erro))
                .ifPresent(CarregadorDeAfinidade::aplicarSeValida);
    }

    /**
     * So instala depois de a matriz passar pela propria validacao.
     *
     * <p>O codec ja garante FORMA -- tipos, campos obrigatorios, nomes de
     * categoria que existem. Ele nao garante COBERTURA: uma matriz com quatro
     * linhas em vez de cinco e JSON perfeitamente valido, e produziria zero
     * numa categoria so. Esse e o defeito que passaria despercebido.
     */
    private static void aplicarSeValida(MatrizDeAfinidade candidata) {
        List<String> problemas = candidata.problemas();
        if (!problemas.isEmpty()) {
            LOG.error("A matriz de afinidade em data/{}/{}/{}.json esta"
                            + " incompleta ou tem pares a mais. Nenhuma matriz"
                            + " nova foi aplicada. Problemas ({}):",
                    ID_DA_MATRIZ.getNamespace(), PASTA, ID_DA_MATRIZ.getPath(),
                    problemas.size());
            for (String problema : problemas) {
                LOG.error("  - {}", problema);
            }
            return;
        }

        NenAffinity.instalar(candidata);
        candidata.nota().ifPresent(nota -> LOG.info("Matriz de afinidade: {}", nota));
    }
}
