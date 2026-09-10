package com.darkcontinent.nenfoundation.api.ability;

import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * O que o CLIENTE pediu. Nada aqui e verdade ate o servidor validar.
 *
 * <p>DECISAO QUE ESTE ARQUIVO CARREGA — e a mais importante de seguranca do
 * mod:
 *
 * <p>O alvo chega como um ID DE ENTIDADE, nunca como uma entidade. O servidor
 * resolve o id no seu proprio mundo e reconfere distancia, dimensao, linha de
 * visao e se o alvo pode ser atingido. Aceitar a entidade que o cliente
 * apontou e aceitar dano a qualquer coisa em qualquer lugar — e nao ha teste
 * de gameplay que perceba isso, porque com um cliente honesto tudo funciona.
 *
 * <p>Pelo mesmo motivo o record NAO tem campos de custo, dano, cooldown ou
 * multiplicador. Se um dia alguem precisar acrescentar um, a resposta e nao:
 * esse numero e conta do servidor.
 *
 * @param abilityId  qual habilidade
 * @param slot       slot da barra de habilidades de onde veio o input
 * @param alvoId     id de rede da entidade apontada, se houver
 * @param posicao    posicao apontada, se a habilidade for posicional
 */
public record AbilityRequest(
        ResourceLocation abilityId,
        int slot,
        OptionalInt alvoId,
        Optional<Vec3> posicao) {

    public static AbilityRequest semAlvo(ResourceLocation abilityId, int slot) {
        return new AbilityRequest(abilityId, slot, OptionalInt.empty(), Optional.empty());
    }
}
