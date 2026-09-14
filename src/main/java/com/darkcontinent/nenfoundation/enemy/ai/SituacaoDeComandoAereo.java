package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * Os fatos que o SERVIDOR ja mediu sobre o voo da comandante -- a regra nao
 * consulta mundo nenhum.
 *
 * <p><b>Por que tudo chega medido.</b> Uma regra que consultasse o nivel para
 * saber a altura passaria a custar uma varredura por chamada, e uma regra cara e
 * uma regra que alguem vai deixar de chamar todo tick "para economizar" -- e a
 * altitude passaria a ser conferida de dez em dez ticks, que e onde o mergulho
 * inteiro acontece. Recebendo numeros, a decisao e testavel sem servidor e barata
 * o bastante para rodar sempre.</p>
 *
 * <p><b>{@code alturaSobreOChao} e medida ate o CHAO, e nao ate o alvo.</b> A
 * ficha dela diz "ela e vulneravel no chao", e chao e o que o jogador consegue
 * manipular: ele a empurra para baixo, a encurrala sob um teto, a faz descer
 * atras de alguem. Medida ate o alvo, a mesma comandante estaria "alta" sobre um
 * jogador num buraco e "baixa" sobre um jogador numa torre -- e a tatica que o
 * encontro ensina deixaria de funcionar exatamente onde o jogador construiu.</p>
 *
 * @param alturaSobreOChao blocos entre os pes dela e o primeiro solido abaixo
 * @param distanciaAoAlvo centro a centro, horizontal, ja medida pelo servidor
 * @param temAlvo ha alvo hostil reconhecido AGORA
 * @param mergulhando ela ja esta comprometida com um mergulho iniciado antes
 * @param ticksDesdeOMergulho ticks desde o FIM do ultimo mergulho; grande quando
 *        nunca houve um
 * @param cambaleando o stagger disparou e o controle do voo se foi
 */
public record SituacaoDeComandoAereo(double alturaSobreOChao, double distanciaAoAlvo,
        boolean temAlvo, boolean mergulhando, int ticksDesdeOMergulho, boolean cambaleando) {

    public SituacaoDeComandoAereo {
        if (!Double.isFinite(alturaSobreOChao) || alturaSobreOChao < 0.0D) {
            throw new IllegalArgumentException("altura sobre o chao invalida: " + alturaSobreOChao
                    + ". NaN aqui nao vira erro: vira uma comandante que se acha no alto para"
                    + " sempre, comandando de dentro de uma caverna.");
        }
        if (!Double.isFinite(distanciaAoAlvo) || distanciaAoAlvo < 0.0D) {
            throw new IllegalArgumentException("distancia ao alvo invalida: " + distanciaAoAlvo
                    + ". O lado seguro de uma medida quebrada e a comandante NAO se comprometer"
                    + " com o mergulho.");
        }
        if (ticksDesdeOMergulho < 0) {
            throw new IllegalArgumentException("ticks desde o mergulho negativo: "
                    + ticksDesdeOMergulho + ". Negativo compararia menor que qualquer janela e"
                    + " prenderia a comandante em RECOLHER para sempre -- ela subiria e nunca mais"
                    + " daria uma ordem, sem uma linha de log.");
        }
        if (mergulhando && !temAlvo) {
            throw new IllegalArgumentException("mergulho em curso sem alvo: os dois campos"
                    + " discordam, e a decisao levaria a comandante a descer sobre um alvo que o"
                    + " resto do sistema diz nao existir");
        }
    }

    /** Situacao de quem esta no alto, inteira e sem ninguem por perto. */
    public static SituacaoDeComandoAereo emPatrulha(double alturaSobreOChao) {
        return new SituacaoDeComandoAereo(alturaSobreOChao, 0.0D, false, false,
                Integer.MAX_VALUE, false);
    }
}
