package com.darkcontinent.nenfoundation.nen.technique;

import com.darkcontinent.nenfoundation.NenFoundation;
import java.util.Set;
import java.util.function.DoubleSupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ten: o estado que segura a aura em volta do corpo.
 *
 * <p>NO CANONE, Ten impede a aura de vazar e a conserva melhor que o vazamento
 * descontrolado de quem nao treinou. Ele e o estado normal de quem tem
 * experiencia, e a base de varias tecnicas avancadas.
 *
 * <p>COMO ISSO VIRA MECANICA AQUI, e por que nao e vazamento: o motor de Aura
 * nao tem perda passiva -- ele regenera. "Conservar melhor" foi modelado como
 * REGENERAR MELHOR, que e o que a arquitetura do projeto prescreve para estado
 * de Nen ativo. A decisao, o custo e as duas aprovacoes estao no ADR-010.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. O SALDO E NEGATIVO, de proposito (ADR-010, item 6). Ten cobra mais do
 * que a regeneracao devolve. Uma tecnica que rendesse mais do que custa viraria
 * o estado obviamente sempre-ligado, e o jogo perderia a escolha. Ha portao
 * exigindo isso dos numeros distribuidos.
 *
 * <p>2. OS NUMEROS SAO LIDOS NO TICK, e nao guardados na ativacao. Congelar o
 * custo em {@code onActivate} faria a tecnica ignorar toda recarga de config
 * posterior -- e ignorar em silencio. E o erro numero 1 da lista do CLAUDE.md.
 *
 * <p>3. NENHUM ESTADO DE JOGADOR MORA NESTA CLASSE. A instancia e unica e
 * compartilhada por todos; um campo aqui seria dois jogadores escrevendo no
 * mesmo lugar. E o erro numero 2 da lista.
 *
 * <p>4. Ten DECLARA o preco e nao o cobra. Quem debita e desliga por falta de
 * aura e o ciclo de vida, via {@link ConsomeAura} -- assim "aura zero encerra a
 * tecnica" e garantia de um lugar so, e nao promessa repetida em cada tecnica.
 *
 * <p>5. TEN EXCLUI ZETSU, e a exclusao entrou dos dois lados na issue #88.
 * Enquanto Zetsu nao existia isto era ponto cego declarado aqui: o portao de
 * simetria do registro RECUSA declarar exclusao com uma tecnica que nao existe,
 * e foi ele quem garantiu que ela nao entrasse pela metade.
 *
 * <p>PONTO CEGO DECLARADO: <b>Ten nao defende de nada.</b> A defesa passiva
 * contra aura hostil, que a issue #86 pedia, nao tem em que se apoiar: nao ha
 * dano de Nen no projeto. Ela vai para o sistema de combate, com issue propria.
 */
public final class Ten implements NenTechnique, ModificaRegeneracao, ConsomeAura {

    /** Id congelado: vai para NBT, datapack e quest. */
    public static final ResourceLocation ID = NenFoundation.id("ten");

    private static final double TICKS_POR_SEGUNDO = 20.0D;

    private final DoubleSupplier custoPorSegundo;
    private final DoubleSupplier multiplicador;

    /**
     * Recebe FONTES de numero, e nao numeros.
     *
     * <p>E o que permite exercitar Ten sem a config do jogo carregada, e o que
     * garante que uma recarga de config seja vista no tick seguinte em vez de
     * no proximo restart.
     */
    public Ten(DoubleSupplier custoPorSegundo, DoubleSupplier multiplicador) {
        this.custoPorSegundo = custoPorSegundo;
        this.multiplicador = multiplicador;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Set<ResourceLocation> incompativeisCom() {
        return Set.of(Zetsu.ID);
    }

    @Override
    public double multiplicadorDeRegeneracao() {
        return this.multiplicador.getAsDouble();
    }

    /** Quanto Ten cobra por tick, lido agora. Quem debita e o servico. */
    @Override
    public double custoPorTick() {
        return this.custoPorSegundo.getAsDouble() / TICKS_POR_SEGUNDO;
    }

    @Override
    public TechniqueActivationResult canActivate(ServerPlayer jogador, NenContext ctx) {
        if (!ctx.perfil().awakened()) {
            return TechniqueActivationResult.negado("nenfoundation.error.nao_desperto");
        }
        // NAO exige aura suficiente para ativar: Ten tem custo CONTINUO, nao de
        // entrada. Exigir saldo aqui inventaria um custo de ativacao que nao
        // existe, e o jogador nao teria como saber de onde ele veio.
        return TechniqueActivationResult.aceito();
    }

    @Override
    public void onActivate(ServerPlayer jogador, NenContext ctx) {
        // Nada a fazer. O efeito de Ten e o multiplicador de regeneracao, que o
        // servico recalcula ao ativar, e o custo continuo, que sai no tick.
        //
        // Vazio de PROPOSITO, e nao por esquecimento: qualquer coisa criada
        // aqui teria de ser desfeita em onDeactivate, e o par de menor risco e
        // o que nao existe.
    }

    @Override
    public void serverTick(ServerPlayer jogador, NenContext ctx) {
        // Vazio, e isso e a coisa certa. A manutencao de Ten e cobrada pelo
        // ciclo de vida, que le `custoPorTick()` antes de tickar e desliga com
        // OUT_OF_AURA quando nao da.
        //
        // A primeira versao cobrava aqui dentro e chamava o servico para se
        // desligar -- o que fazia `nen/` importar `server/`, contra a regra de
        // dependencia do projeto. O conserto foi melhor que o codigo original:
        // "aura zero encerra a tecnica" virou garantia de UM lugar, em vez de
        // promessa repetida em toda tecnica que vier.
    }

    @Override
    public void onDeactivate(ServerPlayer jogador, NenContext ctx, StopReason motivo) {
        // Nada a limpar, pelo mesmo motivo de onActivate estar vazio. O
        // multiplicador volta sozinho: ele e DERIVADO do conjunto de tecnicas
        // ativas, e o servico recalcula ao desligar.
    }
}
