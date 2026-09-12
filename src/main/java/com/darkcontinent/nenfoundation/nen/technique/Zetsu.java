package com.darkcontinent.nenfoundation.nen.technique;

import com.darkcontinent.nenfoundation.NenFoundation;
import java.util.Set;
import java.util.function.DoubleSupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Zetsu: fecha os nos de aura. Some do radar, e fica exposto.
 *
 * <p>NO CANONE, Zetsu interrompe o fluxo de aura para fora. A presenca do
 * usuario despenca, o cansaco se recupera melhor -- e a defesa de aura some,
 * deixando a pessoa MUITO vulneravel a Nen hostil. Nao e um botao de furtividade
 * de graca; e uma troca.
 *
 * <p>COMO ISSO VIRA MECANICA AQUI:
 *
 * <p>1. O TETO DE OUTPUT VAI A ZERO. Com Zetsu, o jogador nao libera nada --
 * e por isso ele e incompativel com Ten e com Ren, que existem para liberar.
 *
 * <p>2. A REGENERACAO MELHORA MUITO. E o estado de descanso, e o cânone e
 * explicito nisso.
 *
 * <p>3. ELE AINDA CUSTA, so que pouco. Pelo item 6 do ADR-010 usar Nen gasta e
 * nenhum estado sustentado se paga -- entao Zetsu drena devagar, e nao de
 * graca. O saldo e o mais barato das tres tecnicas, o que o torna o estado de
 * repouso obvio SEM torna-lo gratuito.
 *
 * <p>PONTO CEGO DECLARADO, e ele e o mais grave desta classe: <b>a
 * vulnerabilidade nao existe.</b> No cânone esse e O preco de Zetsu -- a defesa
 * de aura some. Nao ha dano de Nen no projeto ({@code nen/combat} tem so o
 * {@code package-info}), entao hoje o unico custo real de Zetsu e nao poder
 * liberar Output. Isso basta enquanto nada consome Output; deixa de bastar no
 * M5, quando habilidade existir.
 *
 * <p>Ate la, Zetsu e mais seguro do que deveria ser. Esta registrado como
 * divida, e nao como acaso.
 */
public final class Zetsu implements NenTechnique, LimitaTetoDeOutput,
        ModificaRegeneracao, ConsomeAura {

    /** Id congelado: vai para NBT, datapack e quest. */
    public static final ResourceLocation ID = NenFoundation.id("zetsu");

    private static final double TICKS_POR_SEGUNDO = 20.0D;

    private final DoubleSupplier custoPorSegundo;
    private final DoubleSupplier multiplicador;
    private final DoubleSupplier teto;

    /** Recebe FONTES de numero, e nao numeros. Ver o construtor de {@link Ten}. */
    public Zetsu(DoubleSupplier custoPorSegundo, DoubleSupplier multiplicador,
            DoubleSupplier teto) {
        this.custoPorSegundo = custoPorSegundo;
        this.multiplicador = multiplicador;
        this.teto = teto;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    /**
     * Zetsu nao coexiste com nada que libere aura.
     *
     * <p>A DECLARACAO ENTRA DOS DOIS LADOS DE UMA VEZ, e o portao de simetria
     * do registro e quem garante isso: ate Zetsu existir, Ten e Ren nao podiam
     * declarar exclusao com ele -- o selamento RECUSA apontar para tecnica que
     * nao existe. Foi ponto cego declarado nos dois arquivos, e morreu aqui.
     *
     * <p>GYO ENTROU DEPOIS, e o portao cobrou na hora: Gyo declarou Zetsu, esta
     * lista nao declarou Gyo, e o servidor RECUSOU SUBIR com
     * "exclusao pela metade". Nao foi teste que pegou -- foi o selamento,
     * que acontece antes de qualquer jogador existir.
     */
    @Override
    public Set<ResourceLocation> incompativeisCom() {
        return Set.of(Ten.ID, Ren.ID, Gyo.ID);
    }

    @Override
    public float tetoMaximoPermitido() {
        return (float) this.teto.getAsDouble();
    }

    @Override
    public double multiplicadorDeRegeneracao() {
        return this.multiplicador.getAsDouble();
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
        // Vazio de proposito: os efeitos de Zetsu sao derivados do conjunto de
        // tecnicas ativas, e o servico recalcula ao ativar.
    }

    @Override
    public void serverTick(ServerPlayer jogador, NenContext ctx) {
        // Vazio: a manutencao e cobrada pelo ciclo de vida, via ConsomeAura.
    }

    @Override
    public void onDeactivate(ServerPlayer jogador, NenContext ctx, StopReason motivo) {
        // Nada a limpar: teto e multiplicador sao derivados, e o servico
        // recalcula ao desligar.
    }
}
