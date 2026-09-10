package com.darkcontinent.nenfoundation.nen.technique;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * O ciclo de vida de uma tecnica fundamental de Nen: Ten, Ren, Zetsu, Gyo — e,
 * depois do MVP, Ken, Ko, Ryu, Shu, En e In.
 *
 * <p>CONTRATO CONGELADO (plano tecnico, secao 22).
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. Existe UM ciclo de vida, nao quatro sistemas parecidos. Foi essa
 * decisao que fez o plano prever Ken/Ko/Ryu como "adicionar uma classe" em vez
 * de "migrar o NenProfile".
 *
 * <p>2. {@link #incompativeisCom()} declara as exclusoes como DADO, e nao como
 * {@code if} dentro de cada tecnica. Zetsu nao conhece Ten pelo nome dentro do
 * seu corpo; ele declara com quem nao coexiste, e a maquina de estados resolve.
 * Regra de exclusao espalhada por condicionais diverge — Ten sabe que Zetsu o
 * cancela, Zetsu esquece que Ren tambem existe, e a combinacao ilegal fica
 * possivel sem nenhum erro.
 *
 * <p>3. Toda assinatura recebe {@link NenContext}, nunca numeros ja
 * calculados. Ver a decisao 1 de {@code NenContext}.
 *
 * <p>4. {@link #serverTick} existe; nao ha {@code clientTick}. A tecnica e
 * server-side inteira. O cliente recebe o resultado por payload S2C e nada
 * mais. Ver ADR-001.
 *
 * <p>Como escrever uma tecnica nova: {@code docs/api/techniques.md}.
 */
public interface NenTechnique {

    /** Identificador estavel. Vai para NBT, datapack e quest — nao muda. */
    ResourceLocation id();

    /**
     * Tecnicas que nao podem estar ativas junto com esta.
     *
     * <p>A relacao e declarada dos DOIS lados. A maquina de estados verifica
     * simetria na inicializacao e reprova se so um dos lados declarar — uma
     * exclusao declarada pela metade e uma combinacao ilegal que funciona.
     */
    Set<ResourceLocation> incompativeisCom();

    /** O servidor pergunta ANTES de ativar. Recusa sempre traz motivo. */
    TechniqueActivationResult canActivate(ServerPlayer jogador, NenContext ctx);

    /**
     * Liga a tecnica.
     *
     * <p>Precisa ser IDEMPOTENTE: reentrar numa tecnica ja ativa nao pode
     * reiniciar duracao, cobrar custo de novo nem empilhar modificador. Input
     * repetido chega — o jogador segura a tecla.
     */
    void onActivate(ServerPlayer jogador, NenContext ctx);

    /** Um tick de servidor com a tecnica ligada. */
    void serverTick(ServerPlayer jogador, NenContext ctx);

    /**
     * Desliga a tecnica.
     *
     * <p>QUEM LIGA, DESLIGA. Todo modificador, listener, entidade ou timer
     * criado em {@link #onActivate} some aqui — e aqui e o unico lugar. Espalhar
     * a limpeza pelos varios pontos de onde se pode sair (morte, logout, troca
     * de dimensao) e o desenho que ja perdeu uma chamada, com sintoma
     * silencioso: o buff fica ligado para sempre.
     *
     * <p>Precisa aguentar ser chamado para uma tecnica que ja parou.
     */
    void onDeactivate(ServerPlayer jogador, NenContext ctx, StopReason motivo);
}
