package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import com.darkcontinent.nenfoundation.nen.technique.NenContext;
import com.darkcontinent.nenfoundation.nen.technique.NenTechnique;
import com.darkcontinent.nenfoundation.nen.technique.RegistroDeTecnicas;
import com.darkcontinent.nenfoundation.nen.technique.StopReason;
import com.darkcontinent.nenfoundation.nen.technique.TechniqueActivationResult;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * A porta unica de ligar e desligar tecnica.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. A ORDEM E CONTRATO: perguntar, resolver conflito, gravar, ligar. O
 * {@code onActivate} so roda depois de o conflito estar resolvido e o estado
 * gravado -- quem escuta ou consulta ve o mundo ja consistente.
 *
 * <p>2. IDEMPOTENTE. Reentrar numa tecnica ja ativa e no-op: nao recobra custo,
 * nao reinicia duracao, nao empilha modificador, e nao chama {@code onActivate}
 * de novo. O jogador SEGURA a tecla -- input repetido nao e caso excepcional, e
 * o caso normal.
 *
 * <p>3. QUEM LIGA, DESLIGA, e o desligamento passa por AQUI sempre. Morte,
 * logout e troca de dimensao nao limpam pedacinhos por conta propria: eles
 * chamam {@link #desligarTodas} com o motivo certo. Limpeza espalhada pelos
 * pontos de saida e o desenho que ja perdeu uma chamada -- e o sintoma e um
 * buff ligado para sempre.
 *
 * <p>4. DESLIGAR NUNCA FALHA. {@code onDeactivate} pode lancar -- uma tecnica
 * mal escrita existe --, e uma excecao ali nao pode impedir o desligamento das
 * outras nem deixar o estado pela metade. O erro e registrado e o ciclo segue.
 *
 * <p>PONTO CEGO DECLARADO: <b>nenhuma tecnica esta registrada ainda.</b> Ten,
 * Ren, Zetsu e Gyo tem issues proprias (#86, #87, #88). Ate elas chegarem, este
 * servico funciona contra um registro vazio -- ele esta certo, e nao esta em
 * uso.
 */
public final class NenTechniqueService {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(NenTechniqueService.class);

    /**
     * O registro em uso. {@code volatile} porque o selamento acontece na carga
     * do mod e a leitura na thread do servidor.
     */
    private static volatile RegistroDeTecnicas registro = RegistroDeTecnicas.selar(List.of());

    private NenTechniqueService() {
    }

    /** Troca o registro. So o ciclo de vida do mod chama isto. */
    public static void instalar(RegistroDeTecnicas novo) {
        registro = Objects.requireNonNull(novo, "registro");
    }

    public static RegistroDeTecnicas registro() {
        return registro;
    }

    /** O que aconteceu na tentativa de ligar. */
    public enum Ativacao {
        /** Ligou agora. */
        ATIVOU,

        /** Ja estava ativa. Nada mudou, e isso nao e erro. */
        JA_ATIVA,

        /** Nao existe tecnica com esse id. */
        DESCONHECIDA,

        /** A propria tecnica recusou. O motivo vai junto. */
        RECUSADA
    }

    /** O resultado, com o motivo quando houver. */
    public record Resultado(Ativacao estado, Optional<net.minecraft.network.chat.Component> motivo,
            Set<ResourceLocation> desligadasPorConflito) {

        public Resultado {
            desligadasPorConflito = Set.copyOf(desligadasPorConflito);
        }

        static Resultado de(Ativacao estado) {
            return new Resultado(estado, Optional.empty(), Set.of());
        }
    }

    /**
     * Liga uma tecnica, desligando as incompativeis que estiverem ativas.
     *
     * <p>O conflito e resolvido ANTES de a nova ligar, e as desligadas saem com
     * {@link StopReason#REPLACED_BY_INCOMPATIBLE}. Ligar primeiro deixaria uma
     * janela -- dentro de um tick -- com as duas ativas, e e exatamente nessa
     * janela que um {@code serverTick} rodaria com a combinacao ilegal.
     */
    public static Resultado ativar(ServerPlayer jogador, ResourceLocation id) {
        Objects.requireNonNull(jogador, "jogador");
        Objects.requireNonNull(id, "id");

        RegistroDeTecnicas atual = registro;
        NenTechnique tecnica = atual.porId(id).orElse(null);
        if (tecnica == null) {
            return Resultado.de(Ativacao.DESCONHECIDA);
        }

        RuntimeNenState estado = NenRuntimeService.estadoDe(jogador);
        if (estado.tecnicasAtivas().contains(id)) {
            // IDEMPOTENCIA. Sair aqui, antes de qualquer pergunta, e o que
            // impede o custo de ser cobrado de novo por quem segura a tecla.
            return Resultado.de(Ativacao.JA_ATIVA);
        }

        NenContext ctx = new ContextoDeNen(jogador);
        TechniqueActivationResult permissao = tecnica.canActivate(jogador, ctx);
        if (!permissao.permitido()) {
            return new Resultado(Ativacao.RECUSADA, permissao.motivo(), Set.of());
        }

        Set<ResourceLocation> conflitos =
                atual.conflitosDe(id, estado.tecnicasAtivas());
        for (ResourceLocation conflitante : conflitos) {
            desligar(jogador, conflitante, StopReason.REPLACED_BY_INCOMPATIBLE);
        }

        estado.ativarTecnica(id);
        rodarComProtecao(id, "onActivate", () -> tecnica.onActivate(jogador, ctx));

        return new Resultado(Ativacao.ATIVOU, Optional.empty(), conflitos);
    }

    /**
     * Desliga uma tecnica. Devolve se ela estava ativa.
     *
     * <p>Aguenta ser chamado para tecnica ja parada, como o contrato exige.
     */
    public static boolean desligar(ServerPlayer jogador, ResourceLocation id, StopReason motivo) {
        Objects.requireNonNull(jogador, "jogador");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(motivo, "motivo");

        RuntimeNenState estado = NenRuntimeService.estadoDe(jogador);
        if (!estado.desativarTecnica(id)) {
            return false;
        }

        // A GRAVACAO VEM ANTES do onDeactivate, e nao depois: se a tecnica
        // lancar ali dentro, o estado ja esta limpo e ela nao volta a ser
        // considerada ativa no proximo tick.
        registro.porId(id).ifPresent(tecnica ->
                rodarComProtecao(id, "onDeactivate",
                        () -> tecnica.onDeactivate(jogador, new ContextoDeNen(jogador), motivo)));
        return true;
    }

    /**
     * Desliga tudo que estiver ativo, com o mesmo motivo.
     *
     * <p>E POR AQUI que morte, logout e troca de dimensao passam. A copia da
     * lista antes do laco nao e estilo: {@link #desligar} muta o conjunto, e
     * iterar sobre ele direto lancaria {@code ConcurrentModificationException}
     * no primeiro desligamento.
     */
    public static int desligarTodas(ServerPlayer jogador, StopReason motivo) {
        Objects.requireNonNull(jogador, "jogador");
        Set<ResourceLocation> ativas =
                new LinkedHashSet<>(NenRuntimeService.estadoDe(jogador).tecnicasAtivas());
        int desligadas = 0;
        for (ResourceLocation id : ativas) {
            if (desligar(jogador, id, motivo)) {
                desligadas++;
            }
        }
        return desligadas;
    }

    /**
     * Um tick de servidor para as tecnicas ativas deste jogador.
     *
     * <p>A copia da lista existe porque um {@code serverTick} pode desligar a
     * propria tecnica -- ou outra -- e isso e legitimo.
     */
    public static void tick(ServerPlayer jogador, RuntimeNenState estado) {
        if (estado.tecnicasAtivas().isEmpty()) {
            return;
        }
        RegistroDeTecnicas atual = registro;
        List<ResourceLocation> ativas = new ArrayList<>(estado.tecnicasAtivas());
        NenContext ctx = new ContextoDeNen(jogador);

        for (ResourceLocation id : ativas) {
            // Reconfere: a tecnica anterior deste mesmo tick pode ter
            // desligado esta. Tickar uma tecnica ja desligada roda logica de
            // um estado que nao existe mais.
            if (!estado.tecnicasAtivas().contains(id)) {
                continue;
            }
            atual.porId(id).ifPresent(tecnica ->
                    rodarComProtecao(id, "serverTick", () -> tecnica.serverTick(jogador, ctx)));
        }
    }

    /**
     * Roda um passo do ciclo de vida sem deixar uma tecnica derrubar as outras.
     *
     * <p>Uma tecnica mal escrita existe, e uma excecao dela nao pode virar um
     * tick de servidor perdido para todo mundo. O erro e registrado com o id --
     * sem o id, o log diria que "algo" falhou.
     */
    private static void rodarComProtecao(ResourceLocation id, String etapa, Runnable corpo) {
        try {
            corpo.run();
        } catch (RuntimeException erro) {
            LOG.error("A tecnica {} lancou em {}. O ciclo continua sem ela.", id, etapa, erro);
        }
    }
}
