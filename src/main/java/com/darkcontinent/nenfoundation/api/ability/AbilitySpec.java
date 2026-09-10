package com.darkcontinent.nenfoundation.api.ability;

import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Os METADADOS de uma habilidade: o que o modpack pode ajustar sem recompilar.
 *
 * <p>CONTRATO CONGELADO (plano tecnico, secao 22).
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. A fronteira entre dado e codigo esta AQUI. Custo, cooldown, alcance,
 * requisito e categoria sao dado — alguem vai querer girar esses numeros numa
 * sessao de balanceamento, sem abrir a IDE. Comportamento e codigo. O que o
 * spec NAO contem e tao importante quanto o que contem: nada aqui decide se um
 * alvo e valido, se ha linha de visao ou quanto dano sai. Isso e invariante de
 * multiplayer e mora em Java.
 *
 * <p>2. Estes numeros sao lidos do registro NA HORA DO USO, nunca copiados
 * para dentro da instancia da habilidade na criacao. Uma recarga de datapack
 * tem de mudar o custo de quem ja tem a habilidade equipada.
 *
 * <p>3. {@code cooldownTicks} e {@code alcance} sao limites de DESIGN e podem
 * ser zero. {@code custoBase} zero e legitimo (habilidade gratuita). Por isso
 * "nao configurado" nunca e representado por zero em lugar nenhum deste
 * registro: zero e valor valido.
 *
 * @param id                identificador estavel
 * @param primaryCategory   categoria que governa a eficiencia
 * @param custoBase         aura cobrada na ativacao; pode ser 0
 * @param custoPorTick      aura cobrada por tick enquanto ativa; pode ser 0
 * @param cooldownTicks     recarga em ticks; pode ser 0
 * @param alcance           alcance maximo em blocos; 0 significa "so no proprio caster"
 * @param tecnicasExigidas  tecnicas que precisam estar DESBLOQUEADAS
 * @param marcosExigidos    marcos de progressao exigidos
 */
public record AbilitySpec(
        ResourceLocation id,
        NenCategory primaryCategory,
        double custoBase,
        double custoPorTick,
        int cooldownTicks,
        double alcance,
        Set<ResourceLocation> tecnicasExigidas,
        Set<ResourceLocation> marcosExigidos) {

    public AbilitySpec {
        if (primaryCategory == NenCategory.UNDETERMINED) {
            throw new IllegalArgumentException(
                    "Habilidade " + id + " sem categoria. UNDETERMINED e o neutro de"
                            + " jogador, nao um valor valido para definicao de habilidade.");
        }
        if (custoBase < 0 || custoPorTick < 0 || cooldownTicks < 0 || alcance < 0) {
            throw new IllegalArgumentException(
                    "Habilidade " + id + " com valor negativo no spec.");
        }
        tecnicasExigidas = Set.copyOf(tecnicasExigidas);
        marcosExigidos = Set.copyOf(marcosExigidos);
    }
}
