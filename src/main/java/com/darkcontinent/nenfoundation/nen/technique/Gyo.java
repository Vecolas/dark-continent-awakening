package com.darkcontinent.nenfoundation.nen.technique;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura;
import com.darkcontinent.nenfoundation.nen.aura.FocoDeAura;
import java.util.Set;
import java.util.function.DoubleSupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Gyo: concentra parte da aura numa regiao, e o resto do corpo fica com menos.
 *
 * <p>NO CANONE, Gyo e aplicacao avancada de Ren: o usuario pega uma porcao
 * maior da propria aura e a concentra numa parte do corpo. A regiao concentrada
 * fica mais poderosa, e <b>as outras recebem proporcionalmente menos</b> -- e
 * essa troca e a tecnica inteira. Concentrar sem tirar de lugar nenhum seria
 * bonus, e bonus nao e tatico.
 *
 * <p>NOS OLHOS, ele e a percepcao famosa: ver aura sutil, rastros, objetos de
 * Nen e <b>aura escondida por In</b>. Aqui a cabeca e a regiao que representa
 * isso.
 *
 * <p>ELE NAO EXCLUI NINGUEM. No cânone Gyo se apoia em Ren; e com Ten que ele
 * convive naturalmente. A unica incompatibilidade real seria com Zetsu -- que
 * fecha os nos de aura -- e Zetsu ja exclui tudo que libera.
 *
 * <p>PONTO CEGO DECLARADO, e ele e a metade da tecnica: <b>a percepcao nao faz
 * nada ainda</b>. Concentrar na cabeca muda a alocacao e custa aura, mas nao
 * revela coisa alguma, porque nao ha nada escondido para revelar -- In nao
 * existe, e o canal de presenca ainda manda o mesmo sinal para todos os
 * observadores. Ver a issue #162.
 *
 * <p>O que ele entrega hoje e a metade que o ADR-014 destravou: a distribuicao
 * e real, autoritativa e visivel. A outra metade chega quando In chegar, e a
 * forma da tecnica nao muda -- so o que a cabeca concentrada passa a significar.
 */
public final class Gyo implements NenTechnique, RedistribuiAura, ConsomeAura {

    /** Id congelado: vai para NBT, datapack e quest. */
    public static final ResourceLocation ID = NenFoundation.id("gyo");

    private static final double TICKS_POR_SEGUNDO = 20.0D;

    private final DoubleSupplier custoPorSegundo;
    private final DoubleSupplier fracaoConcentrada;

    /**
     * Recebe FONTES, e nao valores.
     *
     * <p>A REGIAO NAO ENTRA AQUI: ela e escolha de cada jogador e chega como
     * argumento em {@link #alocacaoDesejada}. Guardar a regiao num campo desta
     * classe seria o erro numero 2 da lista do CLAUDE.md.
     */
    public Gyo(DoubleSupplier custoPorSegundo, DoubleSupplier fracaoConcentrada) {
        this.custoPorSegundo = custoPorSegundo;
        this.fracaoConcentrada = fracaoConcentrada;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Set<ResourceLocation> incompativeisCom() {
        return Set.of(Zetsu.ID, Ko.ID);
    }

    @Override
    public AlocacaoDeAura alocacaoDesejada(FocoDeAura foco) {
        return AlocacaoDeAura.concentrando(foco.regiaoEscolhida(),
                (float) this.fracaoConcentrada.getAsDouble());
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
        return TechniqueActivationResult.aceito();
    }

    @Override
    public void onActivate(ServerPlayer jogador, NenContext ctx) {
        // Vazio: a alocacao e DERIVADA do conjunto de ativas, e o servico
        // recalcula ao ativar. O par de menor risco e o que nao existe.
    }

    @Override
    public void serverTick(ServerPlayer jogador, NenContext ctx) {
        // Vazio: a manutencao e cobrada pelo ciclo de vida, via ConsomeAura.
    }

    @Override
    public void onDeactivate(ServerPlayer jogador, NenContext ctx, StopReason motivo) {
        // Nada a limpar: a alocacao volta sozinha ao uniforme quando o servico
        // recalcula sem Gyo no conjunto.
    }
}
