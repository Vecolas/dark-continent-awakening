package com.darkcontinent.nenfoundation.nen.category;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A tabela de afinidade, como ela vem do datapack.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. NENHUM NUMERO MORA AQUI. Nem um default, nem uma constante de
 * emergencia. Os valores vivem em
 * {@code data/nenfoundation/nen_afinidade/matriz.json}, que o proprio mod
 * distribui como datapack embutido. Uma copia em Java "para o caso de o
 * arquivo faltar" seria a mesma verdade em duas fontes -- e a de Java
 * ganharia em silencio no dia em que o JSON quebrasse, deixando a sessao de
 * balanceamento girando um botao morto.
 *
 * <p>2. SPECIALIZATION NAO E UMA LINHA A MAIS. Ela tem
 * {@link RegraDeEspecializacao} proprio. O cânone e explicito em dizer que ela
 * "nao e normalmente aprendivel como categoria secundaria", e NAO fixa numeros
 * para um Specialist aprendendo as outras. Enfiar isso como uma sexta linha da
 * matriz obrigaria a inventar cinco numeros e apresenta-los com a mesma cara
 * dos que o cânone realmente da.
 *
 * <p>3. A VALIDACAO MORDE DOS DOIS LADOS. {@link #problemas()} reprova tanto
 * par FALTANDO quanto par SOBRANDO. So o primeiro lado deixaria passar uma
 * matriz com {@code "enhancment"} escrito errado: o par certo faltaria (pego)
 * e o errado sobraria (nao pego) -- e como o codec ja teria recusado a chave
 * desconhecida, o sintoma real e outro: uma matriz com Specialization dentro,
 * que parece completa e contradiz a regra separada.
 *
 * @param comuns        as CINCO categorias comuns, nos dois sentidos: 5x5
 * @param especializacao a regra propria da sexta
 * @param nota          texto livre do datapack; ver {@link #nota()}
 */
public record MatrizDeAfinidade(
        Map<NenCategory, Map<NenCategory, Afinidade>> comuns,
        RegraDeEspecializacao especializacao,
        Optional<String> nota) {

    /**
     * As cinco categorias que a matriz cobre: as reais menos Specialization.
     *
     * <p>Derivada de {@link NenCategory#REAIS}, nunca escrita a mao: uma lista
     * literal divergiria do enum no dia em que alguem acrescentasse um valor.
     */
    public static final List<NenCategory> COMUNS =
            NenCategory.REAIS.stream()
                    .filter(c -> c != NenCategory.SPECIALIZATION)
                    .toList();

    private static final Codec<Map<NenCategory, Afinidade>> LINHA =
            Codec.unboundedMap(NenCategory.CODEC, Afinidade.CODEC);

    public static final Codec<MatrizDeAfinidade> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    Codec.unboundedMap(NenCategory.CODEC, LINHA)
                            .fieldOf("comuns").forGetter(MatrizDeAfinidade::comuns),
                    RegraDeEspecializacao.CODEC
                            .fieldOf("especializacao")
                            .forGetter(MatrizDeAfinidade::especializacao),
                    Codec.STRING.optionalFieldOf("nota").forGetter(MatrizDeAfinidade::nota)
            ).apply(inst, MatrizDeAfinidade::new));

    public MatrizDeAfinidade {
        Map<NenCategory, Map<NenCategory, Afinidade>> copia = new LinkedHashMap<>();
        comuns.forEach((origem, linha) -> copia.put(origem, Map.copyOf(linha)));
        comuns = Map.copyOf(copia);
    }

    /**
     * A afinidade entre duas categorias, ou {@link Afinidade#NENHUMA}.
     *
     * <p>Devolve NENHUMA em vez de {@code null} ou excecao para qualquer par
     * que a matriz nao cubra, inclusive {@link NenCategory#UNDETERMINED} --
     * que e a AUSENCIA de categoria e, portanto, nao tem afinidade com nada.
     * Todo jogador do servidor e UNDETERMINED ate despertar: perguntar por ele
     * e legitimo, e nao pode derrubar o tick.
     *
     * <p>Specialization, nos dois sentidos, sai da regra propria e nunca da
     * matriz.
     */
    public Afinidade entre(NenCategory origem, NenCategory alvo) {
        if (origem == null || alvo == null || !origem.eReal() || !alvo.eReal()) {
            return Afinidade.NENHUMA;
        }
        if (origem == NenCategory.SPECIALIZATION || alvo == NenCategory.SPECIALIZATION) {
            return this.especializacao.entre(origem, alvo);
        }
        return this.comuns.getOrDefault(origem, Map.of())
                .getOrDefault(alvo, Afinidade.NENHUMA);
    }

    /**
     * Texto livre que o datapack carrega junto dos numeros.
     *
     * <p>Existe porque a incerteza de cânone precisa VIAJAR COM O DADO. Quem
     * abrir a matriz num pack de terceiros tem de encontrar ali mesmo que o
     * numero de "Specialist aprendendo as outras" e abstracao de gameplay, e
     * nao leitura do material -- e nao so num javadoc que essa pessoa nunca
     * vai ler.
     */
    @Override
    public Optional<String> nota() {
        return this.nota;
    }

    /**
     * Tudo que esta errado nesta matriz. Lista vazia significa valida.
     *
     * <p>Devolve a lista inteira, e nao para no primeiro: quem esta editando um
     * datapack quer os cinco erros de uma vez, e nao cinco recargas.
     */
    public List<String> problemas() {
        List<String> achados = new ArrayList<>();

        for (NenCategory origem : COMUNS) {
            Map<NenCategory, Afinidade> linha = this.comuns.get(origem);
            if (linha == null) {
                achados.add("falta a linha de " + origem.getSerializedName());
                continue;
            }
            for (NenCategory alvo : COMUNS) {
                if (!linha.containsKey(alvo)) {
                    achados.add("falta o par " + origem.getSerializedName()
                            + " -> " + alvo.getSerializedName());
                }
            }
            for (NenCategory alvo : linha.keySet()) {
                if (!COMUNS.contains(alvo)) {
                    achados.add("sobra o par " + origem.getSerializedName()
                            + " -> " + alvo.getSerializedName()
                            + (alvo == NenCategory.SPECIALIZATION
                                    ? " (Specialization tem regra propria; ela nao"
                                            + " entra na matriz)"
                                    : ""));
                }
            }
        }

        for (NenCategory origem : this.comuns.keySet()) {
            if (!COMUNS.contains(origem)) {
                achados.add("sobra a linha de " + origem.getSerializedName()
                        + (origem == NenCategory.SPECIALIZATION
                                ? " (Specialization tem regra propria; ela nao"
                                        + " entra na matriz)"
                                : ""));
            }
        }

        return List.copyOf(achados);
    }

    /**
     * A regra propria de Specialization, separada em tres casos com nome.
     *
     * <p>POR QUE TRES CAMPOS, e nao um numero: os tres casos tem status de
     * cânone DIFERENTES, e misturar isso apaga a diferenca.
     *
     * @param propria             Specialist na propria categoria. Cânone claro:
     *                            e a categoria natural da pessoa.
     * @param outrosAprendendo    qualquer outra categoria tentando
     *                            Specialization. O material diz que ela "nao e
     *                            normalmente aprendivel como categoria
     *                            secundaria" -- e uma afirmacao do cânone, e
     *                            nao uma escolha de balanceamento.
     * @param especialistaNasOutras Specialist nas cinco comuns. <b>O cânone NAO
     *                            fixa isto.</b> O numero que vier aqui e
     *                            abstracao de gameplay, e esta em datapack
     *                            exatamente para poder ser girado sem
     *                            recompilar. Nao o cite como se fosse cânone.
     */
    public record RegraDeEspecializacao(
            Afinidade propria,
            Afinidade outrosAprendendo,
            Afinidade especialistaNasOutras) {

        public static final Codec<RegraDeEspecializacao> CODEC =
                RecordCodecBuilder.create(inst -> inst.group(
                        Afinidade.CODEC.fieldOf("propria")
                                .forGetter(RegraDeEspecializacao::propria),
                        Afinidade.CODEC.fieldOf("outros_aprendendo")
                                .forGetter(RegraDeEspecializacao::outrosAprendendo),
                        Afinidade.CODEC.fieldOf("especialista_nas_outras")
                                .forGetter(RegraDeEspecializacao::especialistaNasOutras)
                ).apply(inst, RegraDeEspecializacao::new));

        /** Qual dos tres casos se aplica. Chamado so quando um dos lados e Specialization. */
        Afinidade entre(NenCategory origem, NenCategory alvo) {
            boolean origemEspecialista = origem == NenCategory.SPECIALIZATION;
            boolean alvoEspecializacao = alvo == NenCategory.SPECIALIZATION;

            if (origemEspecialista && alvoEspecializacao) {
                return this.propria;
            }
            if (alvoEspecializacao) {
                return this.outrosAprendendo;
            }
            return this.especialistaNasOutras;
        }
    }

    /** Vista somente-leitura da matriz comum, para diagnostico e teste. */
    public Map<NenCategory, Map<NenCategory, Afinidade>> comuns() {
        return this.comuns;
    }

    /** Todas as linhas presentes, em ordem estavel do enum. Diagnostico. */
    public Map<NenCategory, Map<NenCategory, Afinidade>> ordenada() {
        Map<NenCategory, Map<NenCategory, Afinidade>> saida = new EnumMap<>(NenCategory.class);
        saida.putAll(this.comuns);
        return saida;
    }
}
