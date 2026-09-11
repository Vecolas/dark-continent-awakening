package com.darkcontinent.nenfoundation.nen.technique;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * As tecnicas que existem, e a conferencia de que as exclusoes fecham.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. A EXCLUSAO E DADO, e nao {@code if} dentro da tecnica. Zetsu nao
 * conhece Ten pelo nome no proprio corpo: ele declara com quem nao coexiste, e
 * quem resolve e a maquina de estados. Regra espalhada por condicional diverge
 * -- Ten sabe que Zetsu o cancela, Zetsu esquece que Ren tambem existe, e a
 * combinacao ilegal fica possivel sem nenhum erro no log.
 *
 * <p>2. A SIMETRIA E CONFERIDA NA CARGA, e nao na ativacao. Uma exclusao
 * declarada de um lado so nao produz erro: produz uma combinacao ilegal que
 * FUNCIONA, e que so aparece no dia em que alguem tentar o par na ordem que
 * ninguem testou. Conferir no registro transforma esse defeito silencioso numa
 * falha de inicializacao, alta e cedo.
 *
 * <p>3. O REGISTRO E IMUTAVEL DEPOIS DE SELADO. Tecnica acrescentada em runtime
 * escaparia da conferencia de simetria -- e a conferencia so vale se ninguem
 * puder entrar depois dela.
 *
 * <p>4. Ele nao conhece jogador, aura nem servidor. E uma tabela. Quem age e o
 * servico; separar os dois e o que permite exercitar a simetria sem um servidor
 * de pe.
 */
public final class RegistroDeTecnicas {

    private final Map<ResourceLocation, NenTechnique> porId;

    private RegistroDeTecnicas(Map<ResourceLocation, NenTechnique> porId) {
        this.porId = Map.copyOf(porId);
    }

    /**
     * Monta o registro e RECUSA se as exclusoes nao fecharem.
     *
     * @throws IllegalStateException listando todos os problemas de uma vez. A
     *     lista inteira, e nao o primeiro: quem esta acrescentando uma tecnica
     *     quer os tres erros juntos, e nao tres reinicializacoes.
     */
    public static RegistroDeTecnicas selar(Collection<NenTechnique> tecnicas) {
        Objects.requireNonNull(tecnicas, "tecnicas");

        Map<ResourceLocation, NenTechnique> porId = new LinkedHashMap<>();
        List<String> problemas = new ArrayList<>();

        for (NenTechnique tecnica : tecnicas) {
            Objects.requireNonNull(tecnica, "tecnica nula na lista");
            ResourceLocation id = Objects.requireNonNull(tecnica.id(), "tecnica com id nulo");
            NenTechnique anterior = porId.put(id, tecnica);
            if (anterior != null) {
                problemas.add("id repetido: " + id + " registrado duas vezes ("
                        + anterior.getClass().getSimpleName() + " e "
                        + tecnica.getClass().getSimpleName() + ")");
            }
        }

        problemas.addAll(problemasDeExclusao(porId));

        if (!problemas.isEmpty()) {
            throw new IllegalStateException(
                    "O registro de tecnicas nao fecha (" + problemas.size() + "):\n  "
                            + String.join("\n  ", problemas));
        }
        return new RegistroDeTecnicas(porId);
    }

    /**
     * Tudo que esta errado nas exclusoes. Lista vazia significa que fecham.
     *
     * <p>Separado e {@code static} para poder ser exercitado sem selar o
     * registro -- e para o teste poder montar o caso torto de proposito.
     */
    static List<String> problemasDeExclusao(Map<ResourceLocation, NenTechnique> porId) {
        List<String> problemas = new ArrayList<>();

        for (Map.Entry<ResourceLocation, NenTechnique> entrada : porId.entrySet()) {
            ResourceLocation id = entrada.getKey();
            Set<ResourceLocation> exclusoes =
                    Objects.requireNonNull(entrada.getValue().incompativeisCom(),
                            "incompativeisCom() devolveu null em " + id);

            for (ResourceLocation outro : exclusoes) {
                if (outro.equals(id)) {
                    problemas.add(id + " se declara incompativel consigo mesma");
                    continue;
                }
                NenTechnique parceira = porId.get(outro);
                if (parceira == null) {
                    // NAO e so um id errado: a exclusao aponta para o vazio, e
                    // a maquina de estados nunca teria com quem conferir.
                    problemas.add(id + " e incompativel com " + outro
                            + ", que nao esta registrada");
                    continue;
                }
                if (!parceira.incompativeisCom().contains(id)) {
                    problemas.add("exclusao pela metade: " + id + " recusa "
                            + outro + ", mas " + outro + " nao recusa " + id
                            + " -- ativar na ordem inversa deixaria as duas ligadas");
                }
            }
        }
        return List.copyOf(problemas);
    }

    /** A tecnica de um id, se ela existir. */
    public Optional<NenTechnique> porId(ResourceLocation id) {
        return Optional.ofNullable(this.porId.get(id));
    }

    /** Todos os ids registrados, em ordem estavel de registro. */
    public Set<ResourceLocation> ids() {
        return new LinkedHashSet<>(this.porId.keySet());
    }

    /** Todas as tecnicas, em ordem estavel de registro. */
    public Collection<NenTechnique> todas() {
        return List.copyOf(this.porId.values());
    }

    public int tamanho() {
        return this.porId.size();
    }

    /**
     * As tecnicas ATIVAS que impedem esta.
     *
     * <p>Devolve a lista, e nao um booleano: quem recusa precisa dizer com quem
     * o conflito e. "Nao da" sem nome manda o jogador adivinhar qual das quatro
     * desligar.
     */
    public Set<ResourceLocation> conflitosDe(
            ResourceLocation candidata, Set<ResourceLocation> ativas) {
        Objects.requireNonNull(candidata, "candidata");
        Objects.requireNonNull(ativas, "ativas");

        NenTechnique tecnica = this.porId.get(candidata);
        if (tecnica == null) {
            return Set.of();
        }
        Set<ResourceLocation> conflitos = new LinkedHashSet<>();
        for (ResourceLocation ativa : ativas) {
            if (ativa.equals(candidata)) {
                continue;
            }
            // Basta um lado, porque a simetria ja foi conferida no selamento.
            // Conferir os dois aqui esconderia uma assimetria que passou.
            if (tecnica.incompativeisCom().contains(ativa)) {
                conflitos.add(ativa);
            }
        }
        return Set.copyOf(conflitos);
    }
}
