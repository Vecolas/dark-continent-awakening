package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * O voo do batedor: ele NAO luta -- ele sobe, afasta, e so morde encurralado.
 *
 * <p><b>A ORDEM DAS DECISOES E A PERSONALIDADE DO BICHO</b>, e e por isso que ela
 * mora num lugar so. Cambalear vem antes de tudo; estar encurralado vem antes de
 * fugir; fugir vem antes de pairar. Inverter qualquer par nao produz erro: produz
 * um batedor que tenta fugir enquanto cai, ou que morde estando livre -- e um
 * batedor que morde estando livre e um mob de combate de 3 de dano, ou seja, nada.
 * A ficha inteira dele e o relatorio, e a resposta que ele ensina e calar o
 * mensageiro antes que ele saia do alcance.</p>
 *
 * <p><b>Por que altura e uma coisa so com o resto.</b> Ele nao alterna "subir" e
 * "afastar": ele afasta SUBINDO. Alternar faria o batedor cruzar a altura do
 * jogador no meio da fuga, entregando de graca o unico instante em que um arco
 * acerta um bicho de 16 de vida e armadura 0.</p>
 *
 * <p><b>O que ela NAO faz:</b> nao le mundo, nao conhece entidade e nao decide
 * alvo. Quem tem o servidor mede distancia, altura e o proprio encurralamento e
 * passa os fatos; e por isso que este comportamento roda em teste unitario, sem
 * servidor.</p>
 *
 * @param alturaPreferida quantos blocos ACIMA do alvo o batedor quer estar
 * @param distanciaDeFuga abaixo desta distancia horizontal ele abre espaco
 * @param alcanceDaMordida distancia horizontal em que a mordida de encurralado vale
 * @param recuoVertical impulso vertical ao levar dano, em blocos por tick
 */
public record RegrasDeVooDeBatedor(double alturaPreferida, double distanciaDeFuga,
        double alcanceDaMordida, double recuoVertical) {

    public RegrasDeVooDeBatedor {
        if (!positivo(alturaPreferida) || !positivo(distanciaDeFuga)
                || !positivo(alcanceDaMordida) || !positivo(recuoVertical)) {
            throw new IllegalArgumentException("numeros de voo invalidos: altura="
                    + alturaPreferida + " fuga=" + distanciaDeFuga + " mordida=" + alcanceDaMordida
                    + " recuo=" + recuoVertical + ". Zero em qualquer um deles nao quebra nada"
                    + " visivel -- apaga um dos quatro comportamentos e o batedor vira um mob"
                    + " comum, sem que nada reprove.");
        }
        if (alcanceDaMordida >= distanciaDeFuga) {
            throw new IllegalArgumentException("o alcance da mordida (" + alcanceDaMordida
                    + ") alcanca tao longe quanto a distancia de fuga (" + distanciaDeFuga
                    + "): o batedor morderia antes de tentar fugir sempre que estivesse"
                    + " encurralado a qualquer distancia, e a leitura de 'ele so ataca sem saida'"
                    + " deixaria de existir");
        }
    }

    private static boolean positivo(double valor) {
        return Double.isFinite(valor) && valor > 0.0D;
    }

    /**
     * A decisao deste tick.
     *
     * <p>Distancia que nao da para medir vira {@link MovimentoDeVoo#PAIRAR} e nao
     * excecao: isto roda dentro de um tick de servidor, e derrubar o tick por causa
     * de uma leitura estranha trocaria um tick de comportamento errado por um mob
     * quebrado. Pairar e a recusa segura -- ele nao foge para lugar nenhum e nao
     * morde ninguem.</p>
     *
     * @param distanciaHorizontal distancia no plano ate o alvo, em blocos
     * @param alturaAcimaDoAlvo quanto ele esta acima do alvo; NEGATIVO quando esta
     *        abaixo, e e nesse caso que o ganho de altura importa mais
     * @param encurralado nao ha para onde recuar, medido por quem tem o mundo
     * @param cambaleando o stagger disparou; quem cambaleia no ar CAI
     */
    public DecisaoDeVoo decidir(double distanciaHorizontal, double alturaAcimaDoAlvo,
            boolean encurralado, boolean cambaleando) {
        // Cambalear vem PRIMEIRO para que nenhuma regra abaixo possa, por descuido,
        // devolver o controle do voo a quem acabou de ser interrompido. E a unica
        // recompensa visivel de acertar um bicho que passa a vida fora de alcance.
        if (cambaleando) return DecisaoDeVoo.de(MovimentoDeVoo.CAIR);

        if (!Double.isFinite(distanciaHorizontal) || distanciaHorizontal < 0.0D
                || !Double.isFinite(alturaAcimaDoAlvo)) {
            return DecisaoDeVoo.de(MovimentoDeVoo.PAIRAR);
        }

        double ganho = Math.max(0.0D, alturaPreferida - alturaAcimaDoAlvo);

        // Encurralado, fugir deixa de ser opcao -- e so entao a mordida existe.
        // Fora deste ramo o batedor NUNCA ataca, e isso e o bicho.
        if (encurralado) {
            return distanciaHorizontal <= alcanceDaMordida
                    ? DecisaoDeVoo.de(MovimentoDeVoo.MORDER)
                    : new DecisaoDeVoo(MovimentoDeVoo.PAIRAR, ganho);
        }

        if (distanciaHorizontal < distanciaDeFuga) {
            return new DecisaoDeVoo(MovimentoDeVoo.AFASTAR, ganho);
        }
        // Longe o bastante: se ainda falta altura, ele SOBE em vez de so pairar --
        // altura e o que torna a proxima fuga possivel, e ganha-la longe do alvo e
        // barato. Pairar baixo a dez blocos do jogador seria pairar ao alcance de
        // um arco sem nenhum motivo.
        return ganho > 0.0D
                ? new DecisaoDeVoo(MovimentoDeVoo.SUBIR, ganho)
                : DecisaoDeVoo.de(MovimentoDeVoo.PAIRAR);
    }

    /**
     * O impulso vertical de quem acabou de levar dano.
     *
     * <p>E o reflexo que faz este bicho ler como voador: um mob de chao apanha e
     * recua no plano; um morcego apanha e SOBE. Sem isso o batedor leva o primeiro
     * golpe e continua na mesma altura, o segundo acerta igual, e o jogador aprende
     * que este bicho e so um mob fraco em vez de um alvo dificil.</p>
     *
     * @param vivo ele sobreviveu ao golpe; um cadaver que salta e um cadaver voando
     * @param cambaleando o stagger disparou; quem perdeu o voo nao ganha impulso --
     *        e essa e a diferenca entre "acertei" e "acertei o bastante"
     */
    public double recuoAoLevarDano(boolean vivo, boolean cambaleando) {
        if (!vivo || cambaleando) return 0.0D;
        return recuoVertical;
    }
}
