package com.darkcontinent.nenfoundation.nen.technique;

import com.darkcontinent.nenfoundation.NenFoundation;
import java.util.Set;
import java.util.function.DoubleSupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ken: Ten e Ren sustentados juntos, no corpo inteiro.
 *
 * <p>NO CANONE, Ken e manter muito mais aura envolvendo o corpo todo -- a obra
 * cita cerca de dez vezes a de Ten, sem que isso seja formula. E a principal
 * defesa geral contra usuarios de Nen, e muito mais cansativo que Ten. Biscuit
 * treina Gon e Killua justamente na DURACAO de Ken.
 *
 * <p>A DECISAO DE DESENHO QUE ESTE ARQUIVO CARREGA: <b>Ken nao mexe em
 * distribuicao.</b>
 *
 * <p>A tentacao era obvia -- "Ken distribui alto em TODAS as regioes" parece
 * pedir {@link RedistribuiAura}. Mas a alocacao soma 1.0 e diz <b>onde</b> a
 * aura esta, nao <b>quanta</b>: "alto em todas" e literalmente a alocacao
 * uniforme, que e o repouso. Ken implementando redistribuicao pediria
 * exatamente o que ja existe sem tecnica nenhuma, e a tecnica nao faria nada.
 *
 * <p>Ken e MAGNITUDE. O que ele muda e o teto de Output, como Ren -- porque ele
 * <i>e</i> Ren, sustentado. Isso tambem o mantem fora do ADR-014 inteiro: ele
 * nao toca o modelo de alocacao, e por isso nao depende de nenhuma emenda a
 * ele.
 *
 * <p>ELE SUBSTITUI TEN E REN, em vez de conviver. Ken e os dois juntos; deixar
 * os tres ligados cobraria tres manutencoes pelo mesmo efeito e somaria tetos
 * que ja se sobrepoem. A exclusao e o jeito honesto de dizer "isto ja inclui
 * aquilo".
 *
 * <p>O TETO DE KEN FICA ABAIXO DO DE REN, e o custo tambem. Ren e o pico: a
 * torneira aberta, cara e insustentavel. Ken e a versao que se aguenta --
 * menos teto, menos dreno, e por isso e ele que se treina para durar. Se os
 * dois numeros fossem iguais aos de Ren, Ken seria Ren com outro nome.
 *
 * <p>PONTO CEGO DECLARADO, e ele e a razao de ser da tecnica: <b>a defesa nao
 * existe.</b> Ken e a principal defesa geral contra Nen, e nao ha dano de Nen
 * neste projeto ({@code nen/combat} tem so o {@code package-info}, issue #127).
 * O que Ken entrega hoje e teto, dreno e uma presenca propria para quem olha.
 * Isso e pouco, e esta escrito aqui em vez de descoberto depois.
 */
public final class Ken implements NenTechnique, ModificaTetoDeOutput, ConsomeAura,
        ProtegeComAura, ReforcaGolpe {

    /** Id congelado: vai para NBT, datapack e quest. */
    public static final ResourceLocation ID = NenFoundation.id("ken");

    private static final double TICKS_POR_SEGUNDO = 20.0D;

    private final DoubleSupplier custoPorSegundo;
    private final DoubleSupplier teto;
    private final DoubleSupplier protecao;
    private final DoubleSupplier reforco;

    /** Recebe FONTES de numero, e nao numeros. Ver o construtor de {@link Ten}. */
    public Ken(DoubleSupplier custoPorSegundo, DoubleSupplier teto,
            DoubleSupplier protecao, DoubleSupplier reforco) {
        this.custoPorSegundo = custoPorSegundo;
        this.teto = teto;
        this.protecao = protecao;
        this.reforco = reforco;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    /**
     * Ken substitui Ten e Ren, e nao coexiste com Zetsu.
     *
     * <p>Gyo e Shu CONTINUAM valendo com Ken ligado, de proposito: as duas
     * concentram aura, e concentrar dentro de um Ken e exatamente o que Ryu
     * vira quando chegar. Excluir as duas aqui fecharia a porta do caminho que
     * o proprio cânone descreve.
     */
    @Override
    public Set<ResourceLocation> incompativeisCom() {
        return Set.of(Ten.ID, Ren.ID, Zetsu.ID);
    }

    @Override
    public float tetoDeOutput() {
        return (float) this.teto.getAsDouble();
    }

    @Override
    public double custoPorTick() {
        return this.custoPorSegundo.getAsDouble() / TICKS_POR_SEGUNDO;
    }

    @Override
    public TechniqueActivationResult canActivate(ServerPlayer jogador, NenContext ctx) {
        if (!ctx.perfil().awakened()) {
            return TechniqueActivationResult.negado("nenfoundation.error.nao_desperto");
        }
        // SEM CUSTO DE ENTRADA, como Ten e Ren. O preco de Ken e continuo, e
        // aparece na barra caindo -- exigir saldo para ligar inventaria um
        // preco que o jogador nao tem como ver de onde veio.
        return TechniqueActivationResult.aceito();
    }

    @Override
    public void onActivate(ServerPlayer jogador, NenContext ctx) {
        // Vazio: o teto e derivado, e o servico recalcula ao ativar.
    }

    @Override
    public void serverTick(ServerPlayer jogador, NenContext ctx) {
        // Vazio: a manutencao e cobrada pelo ciclo de vida, via ConsomeAura.
    }

    @Override
    public void onDeactivate(ServerPlayer jogador, NenContext ctx, StopReason motivo) {
        // Nada a limpar: o teto volta sozinho quando o servico recalcula sem
        // Ken no conjunto.
    }

    /**
     * A razao de ser da tecnica.
     *
     * <p>Ken e a principal defesa geral contra usuarios de Nen, e por isso este
     * numero e o maior dos tres. Se ele nao for claramente maior que o de Ten,
     * Ken vira um Ten caro -- e ha portao para isso.
     */
    @Override
    public double protecaoBase() {
        return this.protecao.getAsDouble();
    }

    @Override
    public double reforcoBase() {
        return this.reforco.getAsDouble();
    }
}
