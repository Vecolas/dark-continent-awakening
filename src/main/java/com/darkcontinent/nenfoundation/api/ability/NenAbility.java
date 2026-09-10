package com.darkcontinent.nenfoundation.api.ability;

import com.darkcontinent.nenfoundation.nen.technique.NenContext;
import com.darkcontinent.nenfoundation.nen.technique.StopReason;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * O contrato de uma habilidade de Nen (Hatsu e derivados).
 *
 * <p>CONTRATO CONGELADO (plano tecnico, secao 22).
 *
 * <p>O CRITERIO DE SUCESSO desta interface, copiado do plano tecnico secao 11 e
 * repetido aqui porque e a unica coisa que impede o nucleo de virar um monte de
 * excecoes:
 *
 * <blockquote>Acrescentar uma habilidade simples nova NAO pode exigir editar
 * AuraEngine, NenProfile, o registro de rede ou o HUD. Registra-se um
 * {@link AbilitySpec} mais uma implementacao desta interface, e o resto do
 * pipeline continua igual.</blockquote>
 *
 * <p>Quando isso deixar de ser verdade — e vai deixar, provavelmente na terceira
 * ou quarta habilidade — a resposta certa nao e acrescentar um {@code if} no
 * engine com o nome da habilidade. E extrair um COMPONENTE reutilizavel. O
 * marco M5 tem uma revisao arquitetural marcada exatamente para isso, depois da
 * sexta habilidade de prova.
 *
 * <p>Como escrever uma habilidade nova: {@code docs/api/abilities.md}.
 */
public interface NenAbility {

    /** Identificador estavel. */
    ResourceLocation id();

    /**
     * Metadados atuais.
     *
     * <p>Valor derivado, lido na hora: a implementacao consulta o registro de
     * definicoes, e nao guarda um {@link AbilitySpec} recebido no construtor.
     * Recarga de datapack precisa valer para quem ja tem a habilidade.
     */
    AbilitySpec spec();

    /**
     * Valida o pedido. Roda no servidor, antes de qualquer efeito.
     *
     * <p>A validacao COMUM (unlock, aura, cooldown, categoria, alcance,
     * dimensao) nao se reimplementa aqui: o pipeline ja a executou. Este metodo
     * so responde pelo que e especifico da habilidade.
     */
    ActivationResult validate(ServerPlayer caster, AbilityRequest pedido, NenContext ctx);

    /** Executa. Ja passou por validacao comum e especifica. */
    void activate(ServerPlayer caster, AbilityRequest pedido, NenContext ctx);

    /** Um tick de uma instancia viva. Habilidade instantanea nao implementa. */
    default void tick(ServerPlayer caster, ActiveAbility instancia, NenContext ctx) {
    }

    /**
     * Encerra uma instancia.
     *
     * <p>QUEM LIGA, DESLIGA: entidade, particula persistente, modificador e
     * listener criados em {@link #activate} somem aqui. Precisa aguentar ser
     * chamado duas vezes para a mesma instancia.
     */
    default void stop(ServerPlayer caster, ActiveAbility instancia, StopReason motivo) {
    }
}
