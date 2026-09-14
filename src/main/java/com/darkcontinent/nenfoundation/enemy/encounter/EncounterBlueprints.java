package com.darkcontinent.nenfoundation.enemy.encounter;

import com.darkcontinent.nenfoundation.NenFoundation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * O QUE cada encontro coloca no mundo -- e quantos.
 *
 * <p><b>Separado do controlador de proposito.</b> O controlador decide QUANDO
 * spawnar, e essa e a parte que precisa ser provada contra restart, corrida e
 * abandono. O QUE muda por criatura. Juntas, a regra de duplicacao seria
 * reescrita em cada encontro novo, e a decima copia esqueceria de registrar os
 * uuids -- um spawn que nao volta na lista fica invisivel para o controlador, e
 * o restart seguinte spawna outra leva por cima.</p>
 *
 * <p><b>A quantidade e parte da identidade do encontro, e nao dificuldade.</b>
 * Um Cyclops e UM; uma matilha de lobos sao quatro, senao nao e matilha. Trocar
 * esses numeros nao deixa o encontro mais dificil: deixa outro encontro no lugar
 * dele.</p>
 */
public final class EncounterBlueprints {

    /**
     * Uma receita de encontro.
     *
     * @param tipo o que nasce
     * @param quantidade quantos; parte da identidade, e nao botao de dificuldade
     * @param raioDeEspalhamento blocos a partir do ancoradouro. Zero empilharia
     *        todos no mesmo bloco, e o empurrao das entidades os espalharia num
     *        jorro -- que o jogador le como bug, nao como chegada
     */
    public record Receita(ResourceLocation tipo, int quantidade, double raioDeEspalhamento) {
        public Receita {
            Objects.requireNonNull(tipo, "receita sem tipo de entidade");
            if (quantidade < 1) {
                throw new IllegalArgumentException("encontro de " + quantidade + " criaturas nao"
                        + " e encontro; para desligar um encontro nao se registra a receita");
            }
            if (!Double.isFinite(raioDeEspalhamento) || raioDeEspalhamento <= 0.0D) {
                throw new IllegalArgumentException("raio de espalhamento invalido: empilhados no"
                        + " mesmo bloco, os bichos sao arremessados pelo empurrao de colisao, e"
                        + " isso le como bug em vez de chegada");
            }
        }
    }

    private static final Map<ResourceLocation, Receita> RECEITAS = new LinkedHashMap<>();

    static {
        // Os sete de Greed Island. Cada um e o ENCONTRO inteiro dele.
        solo("cyclops");
        // O fungo vem em tres: um so nao ensina que eles estouram em cadeia.
        grupo("hyper_puffball", 3, 5.0D);
        solo("melanin_lizard");
        // O rato e alarme: dois, para que matar um nao resolva.
        grupo("radio_rat", 2, 6.0D);
        solo("bubble_horse");
        solo("king_white_stag_beetle");
        // Quatro, senao NAO E MATILHA -- e a matilha e o mob.
        grupo("wolf_pack_hunter", 4, 7.0D);

        // O boneco de treino tem receita para a arena de testes poder cria-lo
        // pelo MESMO caminho dos outros. Um caminho de spawn so para teste seria
        // um segundo caminho, e o de teste nunca exercita o de producao.
        grupo("dummy_enemy", 1, 2.0D);
    }

    private EncounterBlueprints() { }

    private static void solo(String id) { grupo(id, 1, 3.0D); }

    private static void grupo(String id, int quantidade, double raio) {
        ResourceLocation chave = NenFoundation.id(id);
        // O TIPO E GUARDADO COMO ID, e resolvido no registro so na hora de
        // spawnar. Guardar o DeferredHolder aqui puxaria EnemyEntityTypes para a
        // inicializacao desta classe, e com ele o DeferredRegister -- que exige o
        // bootstrap do Minecraft. O portao desta tabela deixaria de conseguir
        // carrega-la, e uma tabela que o portao nao alcanca e uma tabela sem
        // portao. Foi exatamente o que aconteceu na primeira versao.
        RECEITAS.put(chave, new Receita(chave, quantidade, raio));
    }

    /**
     * A receita de um encontro, se existir.
     *
     * <p>Vazio NAO e erro: um encontro sem receita e um encontro que ainda nao
     * tem conteudo, e o controlador trata isso como episodio que nao iniciou --
     * que e melhor do que inventar um bicho qualquer.</p>
     */
    public static Optional<Receita> de(String definitionId) {
        ResourceLocation id = ResourceLocation.tryParse(
                Objects.requireNonNull(definitionId, "definitionId ausente"));
        return id == null ? Optional.empty() : Optional.ofNullable(RECEITAS.get(id));
    }

    /** Os ids com receita, para portao e para diagnostico. */
    public static Map<ResourceLocation, Receita> todas() { return Map.copyOf(RECEITAS); }
}
