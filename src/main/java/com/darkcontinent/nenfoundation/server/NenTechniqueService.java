package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.nen.aura.AuraPool;
import java.util.function.DoubleSupplier;
import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import com.darkcontinent.nenfoundation.nen.technique.ConsomeAura;
import com.darkcontinent.nenfoundation.nen.technique.ModificaTetoDeOutput;
import com.darkcontinent.nenfoundation.nen.technique.LimitaTetoDeOutput;
import com.darkcontinent.nenfoundation.nen.technique.ModificaRegeneracao;
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

    /**
     * De onde sai o teto de Output em repouso.
     *
     * <p>E UMA FONTE INJETADA, e nao uma leitura direta de {@code NenConfig}.
     *
     * <p>A primeira versao lia a config aqui dentro, e isso acoplou o inicio de
     * sessao a config estar CARREGADA -- os testes unitarios quebraram com
     * "Cannot get config value before config is loaded", e em producao seria
     * uma bomba de relogio: qualquer sessao criada antes da carga estouraria,
     * num caminho que ninguem exercita.
     *
     * <p>O padrao ja existia no projeto: Ten e Ren recebem {@code
     * DoubleSupplier} pelo mesmo motivo. O default e o teto ABSOLUTO, para que
     * um ambiente sem config se comporte como antes desta mudanca, e nao pior.
     */
    private static volatile DoubleSupplier tetoDeRepouso =
            () -> AuraPool.OUTPUT_MAXIMO_ABSOLUTO;

    /** Troca o registro. So o ciclo de vida do mod chama isto. */
    public static void instalar(RegistroDeTecnicas novo) {
        registro = Objects.requireNonNull(novo, "registro");
    }

    /** Instala a fonte do teto de repouso. So o ciclo de vida do mod chama. */
    public static void instalarTetoDeRepouso(DoubleSupplier fonte) {
        tetoDeRepouso = Objects.requireNonNull(fonte, "fonte");
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
        recalcularRegeneracao(estado);
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

        // O RECALCULO VEM ANTES do onDeactivate pelo mesmo motivo da gravacao:
        // se a tecnica lancar ali dentro, o multiplicador ja voltou ao que as
        // tecnicas restantes pedem. Recalcular depois deixaria a regeneracao
        // acelerada de uma tecnica que ja parou -- e isso nao da erro nenhum.
        recalcularRegeneracao(estado);

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
            NenTechnique tecnica = atual.porId(id).orElse(null);
            if (tecnica == null) {
                continue;
            }
            // A MANUTENCAO E COBRADA ANTES DO TICK, e num lugar so.
            //
            // Antes: a tecnica nao age neste tick se nao puder pagar -- e o
            // que impede um tick de efeito de graca. Num lugar so: "aura zero
            // encerra a tecnica" deixa de ser promessa repetida em cada
            // implementacao, e a enesima nao tem como esquecer.
            if (!pagarManutencao(jogador, ctx, id, tecnica)) {
                continue;
            }
            rodarComProtecao(id, "serverTick", () -> tecnica.serverTick(jogador, ctx));
        }
    }

    /**
     * Debita a manutencao da tecnica. Devolve se ela pode agir neste tick.
     *
     * <p>Tecnica que nao implementa {@link ConsomeAura} nao cobra nada e sempre
     * pode agir. Custo zero ou negativo tambem nao cobra -- e nao derruba.
     *
     * <p>O debito e tudo ou nada: se faltar aura, a tecnica cai com
     * {@link StopReason#OUT_OF_AURA} e NAO tica. Deixa-la tickar depois de nao
     * pagar seria um tick de efeito de graca, todo tick, para quem esta sem
     * aura -- exatamente quem nao deveria ter efeito nenhum.
     */
    private static boolean pagarManutencao(ServerPlayer jogador, NenContext ctx,
            ResourceLocation id, NenTechnique tecnica) {

        if (!(tecnica instanceof ConsomeAura cobrador)) {
            return true;
        }
        double custo;
        try {
            custo = cobrador.custoPorTick();
        } catch (RuntimeException erro) {
            LOG.error("A tecnica {} lancou ao informar o custo. Desligando.", id, erro);
            desligar(jogador, id, StopReason.INTERRUPTED);
            return false;
        }
        if (!Double.isFinite(custo) || custo <= 0.0D) {
            return true;
        }
        if (ctx.gastarAura(custo)) {
            return true;
        }
        desligar(jogador, id, StopReason.OUT_OF_AURA);
        return false;
    }

    /**
     * O multiplicador de regeneracao, derivado das tecnicas ATIVAS (ADR-010).
     *
     * <p>Recalculado inteiro a cada mudanca, e nunca ajustado em incrementos.
     * Incremento exige que toda entrada tenha a saida correspondente; uma
     * perdida deixa o numero errado para sempre, e sem erro. Recalcular do
     * conjunto nao tem como ficar dessincronizado do conjunto.
     *
     * <p>Ele NAO limita pelo teto aqui: o teto e do motor, e aplica-lo duas
     * vezes esconderia um produto estourado atras de um numero plausivel.
     */
    static double multiplicadorDe(RegistroDeTecnicas registro, Set<ResourceLocation> ativas) {
        double produto = 1.0D;
        for (ResourceLocation id : ativas) {
            NenTechnique tecnica = registro.porId(id).orElse(null);
            if (tecnica instanceof ModificaRegeneracao modificador) {
                produto *= modificador.multiplicadorDeRegeneracao();
            }
        }
        return produto;
    }

    /**
     * O teto de Output das tecnicas ATIVAS: o MAIOR entre o repouso e o que
     * cada uma permite.
     *
     * <p>Maior, e nao produto nem soma: duas tecnicas que levantam o teto nao
     * se empilham. Somar produziria teto acima de 100% com duas tecnicas
     * modestas.
     */
    static float tetoDe(RegistroDeTecnicas registro, Set<ResourceLocation> ativas,
            float tetoEmRepouso) {
        float teto = tetoEmRepouso;
        for (ResourceLocation id : ativas) {
            NenTechnique tecnica = registro.porId(id).orElse(null);
            if (tecnica instanceof ModificaTetoDeOutput modificador) {
                teto = Math.max(teto, modificador.tetoDeOutput());
            }
        }

        // OS LIMITADORES VEM DEPOIS, E VENCEM. Um Zetsu que nao vencesse Ren
        // seria uma supressao que nao suprime. Hoje os dois se excluem, entao o
        // defeito ficaria invisivel ate alguem criar a terceira tecnica que
        // combina com ambos -- e apareceria como "Zetsu as vezes nao funciona".
        //
        // Entre varios limitadores vale o MENOR: limitar e restringir, e duas
        // restricoes nao se cancelam.
        for (ResourceLocation id : ativas) {
            NenTechnique tecnica = registro.porId(id).orElse(null);
            if (tecnica instanceof LimitaTetoDeOutput limitador) {
                teto = Math.min(teto, limitador.tetoMaximoPermitido());
            }
        }
        return teto;
    }

    /**
     * Poe runtime e tecnicas ativas de acordo.
     *
     * <p>PUBLICO porque o inicio de sessao tambem precisa chamar: um
     * RuntimeNenState recem-criado nasce com o teto ABSOLUTO, e nao com o de
     * repouso. Sem esta chamada, o jogador comecaria a sessao com o teto de
     * quem esta em Ren -- e so voltaria ao normal depois de ligar e desligar
     * alguma tecnica.
     */
    public static void recalcularDerivados(RuntimeNenState estado) {
        RegistroDeTecnicas atual = registro;
        estado.definirMultiplicadorDeRegeneracao(
                multiplicadorDe(atual, estado.tecnicasAtivas()));
        estado.definirOutputMaximo(
                tetoDe(atual, estado.tecnicasAtivas(), (float) tetoDeRepouso.getAsDouble()));
    }

    private static void recalcularRegeneracao(RuntimeNenState estado) {
        recalcularDerivados(estado);
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
