package com.darkcontinent.nenfoundation.nen.technique;

import com.darkcontinent.nenfoundation.NenFoundation;
import java.util.Set;
import java.util.function.DoubleSupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ren: libera muito mais aura em volta do corpo, e queima depressa.
 *
 * <p>NO CANONE, Ren aumenta o volume de aura liberada. Ele sustenta ataque e
 * defesa, e a diferenca para Ten e o CONSUMO: Ten conserva, Ren gasta muito
 * mais rapido. A presenca do usuario tambem fica bem mais forte -- uma pessoa
 * em Ren e sentida.
 *
 * <p>COMO ISSO VIRA MECANICA AQUI: Ren levanta o TETO de Output. Em repouso o
 * jogador so consegue liberar uma fracao do que seria capaz; com Ren, o teto
 * sobe. E o primeiro codigo a mexer naquele limite -- ver
 * {@link ModificaTetoDeOutput}.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. ELE E CARO, e essa e a diferenca para Ten. Ten drena devagar porque
 * conserva; Ren drena rapido porque libera. Os dois numeros sao config, e a
 * PROPORCAO entre eles e o que faz Ren ser um estado de combate e Ten um
 * estado de repouso -- nao uma frase no javadoc.
 *
 * <p>2. TEN E REN CONVIVEM, de proposito. No cânone Ren se apoia em Ten, e nao
 * o substitui. Quem ligar os dois paga os dois, e a Aura acaba mais depressa --
 * que e exatamente a regra "o limite simultaneo e a Aura".
 *
 * <p>3. REN NAO MEXE NA REGENERACAO. A retencao e o assunto de Ten. Dar a Ren
 * um multiplicador tambem faria as duas tecnicas competirem pela mesma
 * alavanca, e o efeito de cada uma deixaria de ser legivel.
 *
 * <p>PONTO CEGO DECLARADO: <b>Ren ainda nao e incompativel com Zetsu</b>, pelo
 * mesmo motivo de Ten -- o portao de simetria recusa exclusao com tecnica que
 * nao existe, e Zetsu nasce na issue #88. A declaracao entra dos dois lados de
 * uma vez.
 *
 * <p>PONTO CEGO DECLARADO: <b>a presenca de aura nao existe.</b> O cânone diz
 * que Ren torna o usuario sentido de longe, e nao ha camada de percepcao no
 * projeto -- En e In sao pos-MVP, e Gyo e a #88. Quando existir, ela se prende
 * aqui.
 */
public final class Ren implements NenTechnique, ModificaTetoDeOutput, ConsomeAura {

    /** Id congelado: vai para NBT, datapack e quest. */
    public static final ResourceLocation ID = NenFoundation.id("ren");

    private static final double TICKS_POR_SEGUNDO = 20.0D;

    private final DoubleSupplier custoPorSegundo;
    private final DoubleSupplier teto;

    /** Recebe FONTES de numero, e nao numeros. Ver o construtor de {@link Ten}. */
    public Ren(DoubleSupplier custoPorSegundo, DoubleSupplier teto) {
        this.custoPorSegundo = custoPorSegundo;
        this.teto = teto;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Set<ResourceLocation> incompativeisCom() {
        // Vazio ate Zetsu existir. Ver o ponto cego no topo da classe.
        return Set.of();
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
        // SEM CUSTO DE ENTRADA, como Ten. Exigir saldo para ligar inventaria um
        // preco que o jogador nao tem como ver de onde veio; o preco de Ren e
        // continuo, e ele aparece na barra caindo.
        return TechniqueActivationResult.aceito();
    }

    @Override
    public void onActivate(ServerPlayer jogador, NenContext ctx) {
        // Vazio de proposito. O efeito de Ren e o teto, que o servico recalcula
        // ao ativar, e o custo continuo, que sai no tick. O par de menor risco
        // e o que nao existe.
    }

    @Override
    public void serverTick(ServerPlayer jogador, NenContext ctx) {
        // Vazio: a manutencao e cobrada pelo ciclo de vida, via ConsomeAura.
        // Ver a decisao 4 no javadoc de Ten.
    }

    @Override
    public void onDeactivate(ServerPlayer jogador, NenContext ctx, StopReason motivo) {
        // Nada a limpar: o teto e DERIVADO do conjunto de tecnicas ativas, e o
        // servico recalcula ao desligar.
    }
}
