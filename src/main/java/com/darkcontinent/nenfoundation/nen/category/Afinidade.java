package com.darkcontinent.nenfoundation.nen.category;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Quao bem alguem se da com uma categoria. TRES numeros, nunca um.
 *
 * <p>POR QUE TRES, e nao uma porcentagem so: aprender mais devagar, chegar
 * menos longe e render menos sao coisas diferentes. Colapsadas num numero,
 * "aprende devagar mas chega longe" deixa de ser representavel -- e ninguem
 * descobre isso ao escrever a formula, descobre ao tentar desenhar a terceira
 * habilidade e nao conseguir dizer o que queria.
 *
 * <p>O CANONE DA UM NUMERO SO. A tabela classica (100 / 80 / 60 / 40 por
 * distancia no diagrama) descreve compatibilidade, e o material diz que ela
 * afeta tanto o quanto se progride fora da categoria quanto o quanto a tecnica
 * rende. Por isso os dados que este mod distribui nascem com os TRES campos
 * iguais em cada celula. Isso nao e duplicacao por descuido: e o cânone
 * ocupando os tres eixos que ele nao separa, nos eixos que o jogo precisa
 * poder separar depois -- em datapack, sem tocar em codigo de habilidade.
 *
 * <p>E ELES NAO SAO A FORMULA DE BALANCEAMENTO. Descrevem compatibilidade.
 * Quem usar isto em dano ou custo no M4 precisa combinar com output, controle
 * e proficiencia; usar a porcentagem sozinha como multiplicador de dano seria
 * ler a tabela como coisa que ela nao e.
 *
 * @param learningRate    velocidade de ganho de proficiencia
 * @param maxProficiency  teto de proficiencia alcancavel
 * @param effectiveness   quanto a tecnica rende com a proficiencia que se tem
 */
public record Afinidade(double learningRate, double maxProficiency, double effectiveness) {

    /**
     * Nenhuma afinidade. A resposta para pergunta que nao tem resposta.
     *
     * <p>Existe para que consultar {@link NenCategory#UNDETERMINED} devolva um
     * valor DEFINIDO em vez de {@code null} ou excecao. Um {@code null} que
     * atravessa um tick de servidor vira NPE longe da causa; uma excecao
     * derruba o tick inteiro por causa de uma pergunta que e legitima -- todo
     * jogador do servidor e UNDETERMINED ate despertar.
     */
    public static final Afinidade NENHUMA = new Afinidade(0.0D, 0.0D, 0.0D);

    public static final Codec<Afinidade> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    Codec.DOUBLE.fieldOf("learning_rate").forGetter(Afinidade::learningRate),
                    Codec.DOUBLE.fieldOf("max_proficiency").forGetter(Afinidade::maxProficiency),
                    Codec.DOUBLE.fieldOf("effectiveness").forGetter(Afinidade::effectiveness)
            ).apply(inst, Afinidade::new));

    /**
     * Os tres campos sao OBRIGATORIOS no JSON, sem {@code optionalFieldOf}.
     *
     * <p>Um default silencioso aqui seria a pior armadilha possivel: quem
     * errasse o nome de um campo no datapack -- {@code learningrate},
     * {@code learning-rate} -- receberia o default sem nenhum erro, e a
     * afinidade ficaria errada num canto so da matriz. Faltando o campo, o
     * codec recusa e o carregador diz qual arquivo e qual campo.
     */
    public Afinidade {
        exigirFracaoValida("learning_rate", learningRate);
        exigirFracaoValida("max_proficiency", maxProficiency);
        exigirFracaoValida("effectiveness", effectiveness);
    }

    /**
     * Recusa NaN, infinito e negativo.
     *
     * <p>NaN e o caso que motiva isto: ele atravessa multiplicacao sem erro, e
     * o sintoma final e dano ou custo NaN numa habilidade tres marcos adiante,
     * com a causa num arquivo de datapack que ninguem vai reler.
     *
     * <p>NAO ha teto em 1.0 de proposito. Um datapack que queira uma categoria
     * acima do normal e uma decisao de quem monta o pack; o que nao pode e
     * numero que nao e numero.
     */
    private static void exigirFracaoValida(String campo, double valor) {
        if (!Double.isFinite(valor)) {
            throw new IllegalArgumentException(
                    campo + " precisa ser finito, e veio " + valor);
        }
        if (valor < 0.0D) {
            throw new IllegalArgumentException(
                    campo + " nao pode ser negativo, e veio " + valor);
        }
    }
}
