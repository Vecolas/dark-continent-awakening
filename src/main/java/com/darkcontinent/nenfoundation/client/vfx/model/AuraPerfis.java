package com.darkcontinent.nenfoundation.client.vfx.model;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualMode;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Carrega os perfis visuais de {@code assets/nenfoundation/nen_vfx/}.
 *
 * <p>RECURSO DE CLIENTE, e nao datapack. Perfil visual nao muda custo, alcance,
 * dano nem visibilidade: ele e conforto e direcao de arte. Num datapack, o
 * servidor passaria a ditar o visual de cada cliente.
 *
 * <p>RECARREGA COM F3+T, e e isso que torna a sessao de arte possivel: mexer no
 * numero, recarregar, olhar. Sem isso, cada ajuste custaria um `gradlew` inteiro.
 *
 * <p>ARQUIVO QUEBRADO NAO DERRUBA NADA. Um perfil que nao le -- JSON invalido,
 * campo faltando, invariante violada -- e recusado COM MOTIVO no log, e o perfil
 * de emergencia assume. O de emergencia e visivelmente mais fraco que qualquer
 * perfil real, de proposito: "nao carregou" precisa ser perceptivel, e nao
 * indistinguivel de "carregou".
 */
public final class AuraPerfis extends SimpleJsonResourceReloadListener {

    private static final Logger LOG = LoggerFactory.getLogger(AuraPerfis.class);
    private static final Gson GSON = new Gson();

    /** O diretorio varrido dentro de {@code assets/<namespace>/}. */
    public static final String DIRETORIO = "nen_vfx";

    private static final Map<AuraVisualMode, AuraPerfilVisual> CARREGADOS =
            new EnumMap<>(AuraVisualMode.class);

    public AuraPerfis() {
        super(GSON, DIRETORIO);
    }

    /**
     * O perfil de um modo. Nunca nulo.
     *
     * <p>Modos sem brilho ({@code ZETSU}, {@code OFF}) devolvem o perfil apagado
     * em vez de um perfil qualquer: a ausencia e a informacao, e ela precisa
     * estar escrita aqui e nao depender de uma guarda em quem desenha.
     */
    public static AuraPerfilVisual de(AuraVisualMode modo) {
        if (semBrilho(modo)) {
            // O APAGADO NAO PASSA PELA SOBREPOSICAO, de proposito. Zetsu e
            // ausencia total (direcao visual, secao 2); deixar um slider
            // reacender a aura de quem esta suprimido faria a ferramenta de
            // tuning contradizer a unica regra que o AV6 existe para provar.
            return AuraPerfilVisual.SEGURO.apagado();
        }
        AuraPerfilVisual perfil = CARREGADOS.get(modo);
        return com.darkcontinent.nenfoundation.client.vfx.SobreposicaoDeVfx.aplicarNoPerfil(
                perfil != null ? perfil : AuraPerfilVisual.SEGURO);
    }

    /**
     * O perfil de um ESTADO, ja interpolado entre as duas pontas da transicao.
     *
     * <p><b>ELE CORRIGE A TROCA SECA DE PRESET.</b> Buscar o perfil por
     * {@code estado.mode()} -- que era o que a layer fazia ate o AV3 -- devolve
     * o perfil de ORIGEM durante a transicao inteira e o de DESTINO no ultimo
     * tick. O resultado e uma intensidade que sobe suave com um material que
     * salta num quadro: nao lanca, nao aparece em teste, e a pessoa relata como
     * "bug de render".
     *
     * <p>QUEM SO TEM O MODO CONTINUA USANDO {@link #de(AuraVisualMode)} -- hoje
     * isso e a primeira pessoa, que le apenas a borda e nao tem estado inteiro
     * na mao. Tudo o que tem o estado usa esta sobrecarga, particula inclusive:
     * a taxa de faisca de Ten aparecendo de uma vez no ultimo tick da subida e a
     * mesma troca seca, numa escala menor.
     */
    public static AuraPerfilVisual de(
            com.darkcontinent.nenfoundation.client.vfx.AuraVisualState estado) {
        if (estado == null) {
            return AuraPerfilVisual.SEGURO.apagado();
        }
        if (semBrilho(estado.mode()) && semBrilho(estado.modoAlvo())) {
            // ASSENTADO EM ZETSU OU EM NADA: ausencia total, e sem passar pela
            // sobreposicao -- mesma razao de {@link #de(AuraVisualMode)}.
            return AuraPerfilVisual.SEGURO.apagado();
        }
        AuraPerfilVisual origem = cru(estado.mode());
        AuraPerfilVisual alvo = cru(estado.modoAlvo());
        return com.darkcontinent.nenfoundation.client.vfx.SobreposicaoDeVfx.aplicarNoPerfil(
                AuraPerfilVisual.interpolar(origem, alvo, estado.transitionProgress()));
    }

    /** O perfil de um modo SEM a sobreposicao: a materia-prima da interpolacao. */
    private static AuraPerfilVisual cru(AuraVisualMode modo) {
        if (semBrilho(modo)) {
            return AuraPerfilVisual.SEGURO.apagado();
        }
        AuraPerfilVisual perfil = CARREGADOS.get(modo);
        return perfil != null ? perfil : AuraPerfilVisual.SEGURO;
    }

    /** Modos em que a ausencia e a informacao. */
    private static boolean semBrilho(AuraVisualMode modo) {
        return modo == null || modo == AuraVisualMode.ZETSU || modo == AuraVisualMode.OFF;
    }

    /** Se algum perfil chegou a ser carregado. Falso antes do primeiro reload. */
    public static boolean carregados() {
        return !CARREGADOS.isEmpty();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> arquivos, ResourceManager gerenciador,
            ProfilerFiller perfilador) {
        // LIMPA ANTES DE LER. Sem isto, um perfil removido de um resource pack
        // continuaria valendo ate o jogo fechar -- e o sintoma seria um ajuste
        // que "nao volta atras".
        CARREGADOS.clear();

        for (Map.Entry<ResourceLocation, JsonElement> arquivo : arquivos.entrySet()) {
            ResourceLocation id = arquivo.getKey();
            AuraVisualMode modo = modoDe(id);
            if (modo == null) {
                LOG.warn("Perfil de aura '{}' nao corresponde a nenhum modo visual; ignorado."
                        + " Os nomes aceitos sao {}.", id, nomesAceitos());
                continue;
            }
            try {
                AuraPerfilVisual.CODEC.parse(JsonOps.INSTANCE, arquivo.getValue())
                    .resultOrPartial(erro -> LOG.error(
                            "Perfil de aura '{}' recusado: {}. O perfil de emergencia assume,"
                                    + " e ele e visivelmente mais fraco -- se a aura parecer"
                                    + " apagada, e este o motivo.", id, erro))
                    .ifPresent(perfil -> CARREGADOS.put(modo, perfil));
            } catch (RuntimeException erro) {
                // CINTO E SUSPENSORIO. O codec ja transforma dado torto em erro,
                // mas uma excecao aqui derrubaria o RELOAD DE RECURSOS inteiro
                // -- nao so a aura -- por causa de um resource pack de terceiro.
                // Efeito visual nunca crasha o jogo.
                LOG.error("Perfil de aura '{}' explodiu na leitura; ignorado.", id, erro);
            }
        }
        LOG.info("Perfis visuais de aura carregados: {}.", CARREGADOS.keySet());
    }

    /**
     * De {@code nenfoundation:ten} para {@link AuraVisualMode#TEN}.
     *
     * <p>SO O NAMESPACE DO MOD. Um pack de terceiro pode SOBREPOR
     * {@code nenfoundation:ten.json} -- e isso e o ponto de um resource pack --,
     * mas nao pode inventar um modo que o codigo nao conhece.
     */
    private static AuraVisualMode modoDe(ResourceLocation id) {
        if (!NenFoundation.MOD_ID.equals(id.getNamespace())) {
            return null;
        }
        for (AuraVisualMode modo : AuraVisualMode.values()) {
            if (modo.name().toLowerCase(java.util.Locale.ROOT).equals(id.getPath())) {
                return modo;
            }
        }
        return null;
    }

    private static String nomesAceitos() {
        StringBuilder nomes = new StringBuilder();
        for (AuraVisualMode modo : AuraVisualMode.values()) {
            if (nomes.length() > 0) {
                nomes.append(", ");
            }
            nomes.append(modo.name().toLowerCase(java.util.Locale.ROOT));
        }
        return nomes.toString();
    }
}
