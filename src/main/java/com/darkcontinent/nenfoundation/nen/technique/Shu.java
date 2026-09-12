package com.darkcontinent.nenfoundation.nen.technique;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura;
import com.darkcontinent.nenfoundation.nen.aura.FocoDeAura;
import java.util.Set;
import java.util.function.DoubleSupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shu: a aura envolve o que esta na mao, e o objeto vira extensao do corpo.
 *
 * <p>NO CANONE, Shu e extensao de Ten aplicada a um objeto: ele fica mais
 * resistente, mais poderoso e capaz de interagir com Nen. Nao se limita a
 * armas -- cartas, pedras, ferramentas, projeteis. Hisoka com cartas e o
 * exemplo classico.
 *
 * <p>O ITEM NUNCA E MARCADO, e essa decisao foi tomada contra a solucao obvia.
 *
 * <p>A primeira ideia era prender a aura ao {@code ItemStack} por attachment.
 * <b>Nao existe:</b> no NeoForge 21.1.250 o {@code ItemStack} nao implementa
 * {@code IAttachmentHolder} -- os holders sao entidade, block entity, level e
 * chunk. Conferido nas classes do jar, e nao suposto.
 *
 * <p>A alternativa seria um {@code DataComponent}, e ela e pior por tres
 * motivos que nao sao tecnicos:
 * <ul>
 *   <li>um componente sincronizado viaja em todo pacote de equipamento, ou
 *       seja, entrega a marca de Shu a <b>todo observador sem filtro</b> -- o
 *       oposto do que {@code PresencaDeAura} faz, e o erro numero 6 da lista
 *       do CLAUDE.md;
 *   <li>um componente <b>divide o stack</b>: Shu numa flecha de sessenta e
 *       quatro criaria dois stacks, com estado de combate vazando para o
 *       inventario;
 *   <li>ele persistiria no item largado ou guardado num bau, e estado de
 *       combate nao sobrevive a nada (ADR-002).
 * </ul>
 *
 * <p>ENTAO SHU E ESTADO DO JOGADOR, como as outras: ela concentra a aura no
 * braco da mao dominante, e o item coberto e lido <b>na hora</b>, a cada tick.
 * Trocar de item com Shu ligado muda o que esta coberto sem nada para limpar --
 * e evita o erro numero 1 da lista, congelar na ativacao aquilo que devia ser
 * consultado depois.
 *
 * <p>PONTO CEGO DECLARADO: <b>o item nao fica mais forte.</b> Nao ha dano de
 * Nen (#127), entao "mais resistente e mais poderoso" nao tem onde acontecer. O
 * que Shu entrega hoje e a concentracao no braco, real e autoritativa, e a
 * recusa quando a mao esta vazia.
 */
public final class Shu implements NenTechnique, RedistribuiAura, ConsomeAura {

    /** Id congelado: vai para NBT, datapack e quest. */
    public static final ResourceLocation ID = NenFoundation.id("shu");

    private static final double TICKS_POR_SEGUNDO = 20.0D;

    private final DoubleSupplier custoPorSegundo;
    private final DoubleSupplier fracaoConcentrada;

    /** Recebe FONTES, e nao numeros. Ver o construtor de {@link Ten}. */
    public Shu(DoubleSupplier custoPorSegundo, DoubleSupplier fracaoConcentrada) {
        this.custoPorSegundo = custoPorSegundo;
        this.fracaoConcentrada = fracaoConcentrada;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    /**
     * Shu nao coexiste com Zetsu, e com mais nada.
     *
     * <p>Zetsu fecha os nos de aura; nao ha como envolver um objeto sem liberar
     * nada. Com Ten ela CONVIVE de proposito -- Shu e extensao de Ten, nao
     * substituta. E com Gyo tambem: as duas concentram, e quem vencer a disputa
     * de concentracao decide, o que e a regra geral e nao uma excecao.
     */
    @Override
    public Set<ResourceLocation> incompativeisCom() {
        return Set.of(Zetsu.ID);
    }

    @Override
    public AlocacaoDeAura alocacaoDesejada(FocoDeAura foco) {
        // O BRACO DA MAO DOMINANTE, e nao a regiao que o jogador escolheu para
        // Gyo. Sao dois fatos diferentes do mesmo jogador, e foi por isso que o
        // foco deixou de ser uma regiao so.
        return AlocacaoDeAura.concentrando(foco.bracoPrincipal(),
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
        if (jogador.getMainHandItem().isEmpty()) {
            // RECUSA COM MOTIVO PROPRIO. "Estado invalido" mandaria o jogador
            // procurar o problema na aura, e o problema esta na mao.
            return TechniqueActivationResult.negado("nenfoundation.error.shu_sem_item");
        }
        return TechniqueActivationResult.aceito();
    }

    @Override
    public void onActivate(ServerPlayer jogador, NenContext ctx) {
        // Vazio: a alocacao e derivada, e o item e lido no tick. Nada e
        // congelado aqui de proposito.
    }

    @Override
    public void serverTick(ServerPlayer jogador, NenContext ctx) {
        // A MAO E CONSULTADA TODO TICK, e nao na ativacao. Guardar o item de
        // quando ligou faria Shu continuar cobrindo uma espada que o jogador ja
        // guardou -- sem erro nenhum, so com aura num objeto que nao esta mais
        // ali.
        if (jogador.getMainHandItem().isEmpty()) {
            ctx.desligar(ID, StopReason.TARGET_LOST);
        }
    }

    @Override
    public void onDeactivate(ServerPlayer jogador, NenContext ctx, StopReason motivo) {
        // Nada a limpar: a alocacao volta sozinha quando o servico recalcula
        // sem Shu no conjunto, e o item nunca foi tocado.
    }
}
