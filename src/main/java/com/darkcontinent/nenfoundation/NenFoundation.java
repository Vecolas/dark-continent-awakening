package com.darkcontinent.nenfoundation;

import com.darkcontinent.nenfoundation.config.NenConfig;
import com.darkcontinent.nenfoundation.data.attachment.NenAttachments;
import com.darkcontinent.nenfoundation.data.attachment.BestiaryAttachments;
import com.darkcontinent.nenfoundation.network.NenNetwork;
import com.darkcontinent.nenfoundation.network.NenProtocol;
import com.darkcontinent.nenfoundation.registry.NenFeatures;
import com.darkcontinent.nenfoundation.registry.NenFuncoesDeDensidade;
import com.darkcontinent.nenfoundation.registry.NenParticleTypes;
import com.darkcontinent.nenfoundation.server.NenPedidoService;
import com.darkcontinent.nenfoundation.sound.NenSoundEvents;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityEvents;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import com.darkcontinent.nenfoundation.registry.NenItems;
import com.darkcontinent.nenfoundation.registry.NenBlocks;
import com.darkcontinent.nenfoundation.registry.NenMenus;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeRegistries;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeBlocks;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeItems;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ponto de entrada do Nen Foundation.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. Ele so REGISTRA subsistemas. Nenhuma regra de Nen, nenhuma conta de
 * aura e nenhum handler de rede mora aqui. O motivo e de colaboracao, nao de
 * estetica: com duas pessoas trabalhando em paralelo, um arquivo central que
 * cresce vira o unico ponto de conflito permanente do repositorio. Ver
 * docs/processo/fronteira-de-arquivos.md.
 *
 * <p>2. INICIALIZACAO TEM ORDEM, e quem vem antes nao enxerga quem vem depois.
 * O construtor do mod roda ANTES de a configuracao ser carregada; ler uma
 * chave de config aqui lanca {@code IllegalStateException: Cannot get config
 * value before config is loaded} e derruba o carregamento do mod inteiro.
 *
 * <p>Isso nao e hipotese: aconteceu neste arquivo no dia do bootstrap, e so
 * apareceu no {@code runServer}. A compilacao passou, os testes passaram, e o
 * proprio {@code gradlew runServer} saiu com codigo 0 — a tarefa do Gradle
 * teve sucesso enquanto o jogo dentro dela travava. Quem vem depois PUXA o que
 * precisa na propria inicializacao, e e o que {@link #aoPreparar} faz.
 *
 * <p>ARQUIVO HOSTIL A MERGE: uma pessoa por vez. Quem precisa registrar um
 * subsistema novo adiciona UMA linha e diz no titulo do PR que tocou aqui.
 */
@Mod(NenFoundation.MOD_ID)
public final class NenFoundation {

    /**
     * CONGELADO (ADR-004). Este identificador aparece em NBT de save, em ids de
     * datapack, em nomes de asset e em quests do modpack. Muda-lo depois do
     * primeiro mundo criado invalida saves silenciosamente — sem crash e sem
     * mensagem de erro.
     */
    public static final String MOD_ID = "nenfoundation";

    private static final Logger LOG = LoggerFactory.getLogger(NenFoundation.class);

    public NenFoundation(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, NenConfig.SPEC);

        NenAttachments.ATTACHMENT_TYPES.register(modEventBus);
        // TRES FRENTES ACRESCENTARAM REGISTRO AQUI, e os conflitos eram so de
        // linhas vizinhas: nenhuma substitui a outra. Ficam todas.
        NenSoundEvents.register(modEventBus);
        NenParticleTypes.register(modEventBus);
        // A funcao de densidade da ilha precisa existir antes de o datapack
        // ser lido: sem ela, o `noise_settings` de Greed Island nao valida e
        // a dimensao cai no gerador padrao, sem erro visivel para o jogador.
        NenFuncoesDeDensidade.TIPOS.register(modEventBus);
        NenFeatures.register(modEventBus);
        BestiaryAttachments.TYPES.register(modEventBus);
        NenItems.register(modEventBus);
        NenBlocks.register(modEventBus);
        NenMenus.register(modEventBus);
        modEventBus.addListener(NenItems::adicionarAoCriativo);
        WorldTreeRegistries.register(modEventBus);
        WorldTreeBlocks.register(modEventBus);
        WorldTreeItems.register(modEventBus);
        // UMA fila de inimigos, e so uma. Ate aqui eram duas -- um DeferredRegister
        // para o foxbear e outro para os seis irmaos -- e a linha abaixo tinha de
        // escrever o pacote inteiro para desviar da colisao de nome. Duas filas nao
        // dao erro: dao um mob que fica de fora do par atributos/placement e nunca
        // aparece no mundo. O portao FilaUnicaDeInimigosTest guarda esta linha.
        EnemyEntityTypes.register(modEventBus);
        modEventBus.addListener(EnemyEntityEvents::attributes);
        modEventBus.addListener(EnemyEntityEvents::spawnPlacements);

        modEventBus.addListener((RegisterPayloadHandlersEvent evento) ->
                NenNetwork.registrar(evento, NenPedidoService::validar));
        modEventBus.addListener(NenFoundation::aoPreparar);

        LOG.info("Nen Foundation registrado. Protocolo de rede v{}.", NenProtocol.VERSION);
    }

    /**
     * Roda depois de registros e configuracao existirem.
     *
     * <p>E o primeiro momento em que ler {@link NenConfig} e legitimo.
     */
    private static void aoPreparar(FMLCommonSetupEvent evento) {
        if (NenConfig.devModeAtivo()) {
            LOG.info("Modo de desenvolvimento LIGADO. Transicoes de estado em log: {}.",
                    NenConfig.logarTransicoes());
        }
    }

    /** Constroi um {@link ResourceLocation} no namespace do mod. */
    public static ResourceLocation id(String caminho) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, caminho);
    }
}
