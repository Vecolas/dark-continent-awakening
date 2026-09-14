package com.darkcontinent.nenfoundation.enemy.combat;

import net.minecraft.world.phys.Vec3;

/**
 * Segurar SEM montar: onde a vitima fica, com que forca ela e puxada e quando escapa.
 *
 * <p><b>Por que ela existe ao lado do {@link GrabController}, e nao dentro dele.</b>
 * O controlador e o RELOGIO do agarrao -- quem esta preso, ha quanto tempo, por
 * que soltou -- e ele e o mesmo para todo predador do mod, de proposito. O que
 * muda de bicho para bicho e o JEITO de segurar. O Frog-In-Waiting engole e o
 * Melanin Lizard monta a vitima ({@code startRiding}); este aqui fecha uma pinca
 * e mantem a presa NO CHAO, na frente dele. Montar um jogador tira a camera dele
 * e transforma o agarrao numa cutscene; a pinca deixa o jogador olhando em volta
 * -- que e o que faz a briga continuar enquanto ele esta preso.</p>
 *
 * <p><b>Segurar sem montar cobra um preco que montar nao cobra:</b> ninguem
 * garante que a vitima continua ali. Montada, ela so sai por {@code stopRiding};
 * presa por posicao, ela sai por teleporte, por perola, por elytra, por outro
 * mod, por um comando. Sem uma regra de ruptura, o caranguejo ficaria segurando
 * quem esta a cem blocos -- relogio correndo, pulsos de dano saindo, e nenhuma
 * excecao em lugar nenhum. E disso que {@link #aindaPresa(double)} trata, e e
 * exatamente o parametro que o {@code GrabController} deixou para o mob medir.</p>
 *
 * <p><b>Logica pura.</b> Ela recebe posicoes e devolve posicoes; nao consulta
 * mundo, nao move entidade, nao aplica dano. Quem aplica e a entidade, no lado
 * autoritativo -- e e isso que permite provar a geometria do agarrao sem
 * servidor de pe.</p>
 *
 * @param distanciaDaPinca quantos blocos a frente do predador a vitima fica
 * @param raioDeRuptura a que distancia do ponto de pinca o agarrao se rompe
 * @param velocidadeMaxima o maior puxao aplicavel num tick, em blocos por tick
 */
public record RegrasDePinca(double distanciaDaPinca, double raioDeRuptura,
        double velocidadeMaxima) {

    public RegrasDePinca {
        if (!positivo(distanciaDaPinca) || !positivo(raioDeRuptura)
                || !positivo(velocidadeMaxima)) {
            throw new IllegalArgumentException("geometria de pinca invalida: distancia="
                    + distanciaDaPinca + " ruptura=" + raioDeRuptura + " velocidade="
                    + velocidadeMaxima);
        }
        if (raioDeRuptura <= velocidadeMaxima) {
            throw new IllegalArgumentException("o raio de ruptura (" + raioDeRuptura + ") nao e"
                    + " maior que o puxao de um tick (" + velocidadeMaxima + "): a vitima que"
                    + " atrasar um unico tick ja estara fora do raio e sera solta de graca. Isso"
                    + " nao da erro -- da um agarrao que 'as vezes nao pega', que e o pior relato"
                    + " de bug que existe.");
        }
        if (raioDeRuptura < 2.0D * distanciaDaPinca) {
            throw new IllegalArgumentException("o raio de ruptura (" + raioDeRuptura + ") e menor"
                    + " que o diametro do arco da pinca (2 x " + distanciaDaPinca + "): o ponto de"
                    + " pinca gira em volta do predador, e um predador que se vire perde a vitima"
                    + " sozinho, sem ninguem ter feito nada. O sintoma aparece so quando o bicho"
                    + " gira, que e justamente quando ele esta procurando o proximo alvo.");
        }
    }

    private static boolean positivo(double valor) {
        return Double.isFinite(valor) && valor > 0.0D;
    }

    /**
     * Onde a vitima e mantida: a frente do predador, na altura do CHAO dele.
     *
     * <p>Na altura do chao, e nao na altura da pinca desenhada. Erguer a vitima
     * ate a garra exigiria segurar contra a gravidade todo tick, e o resultado e
     * um jogador tremendo no ar -- alem de deixar a soltura acontecer no alto,
     * com queda. A pinca fecha na altura da cintura de quem esta em pe, e isso ja
     * e o que o modelo desenha.</p>
     *
     * @param posicaoDoPredador posicao (pes) do predador
     * @param frenteHorizontal para onde o CORPO do predador aponta; nao precisa
     *        vir normalizada, mas precisa ter comprimento
     * @throws IllegalArgumentException se a direcao for nula -- normalizar vetor
     *         nulo devolve NaN, e a vitima seria presa em lugar nenhum
     */
    public Vec3 pontoDaPinca(Vec3 posicaoDoPredador, Vec3 frenteHorizontal) {
        if (posicaoDoPredador == null || frenteHorizontal == null) {
            throw new IllegalArgumentException("posicao ou direcao de pinca ausente");
        }
        Vec3 plana = new Vec3(frenteHorizontal.x, 0.0D, frenteHorizontal.z);
        if (plana.lengthSqr() < 1.0E-6D) {
            throw new IllegalArgumentException("direcao de pinca sem comprimento horizontal:"
                    + " normalizar um vetor nulo devolve NaN, e a vitima seria presa em lugar"
                    + " nenhum -- com o relogio do agarrao correndo do mesmo jeito");
        }
        return posicaoDoPredador.add(plana.normalize().scale(distanciaDaPinca));
    }

    /**
     * A vitima ainda esta na pinca?
     *
     * <p>Resposta medida em DISTANCIA, e nao em "alguem chamou soltar". Quem sai
     * por fora do nosso ciclo -- teleporte, perola, outro mod -- nao avisa
     * ninguem, e sem esta pergunta o predador continuaria segurando um fantasma.
     * </p>
     */
    public boolean aindaPresa(double distanciaAoPonto) {
        if (!Double.isFinite(distanciaAoPonto) || distanciaAoPonto < 0.0D) {
            throw new IllegalArgumentException("distancia de pinca invalida: " + distanciaAoPonto
                    + ". Distancia nao finita perderia a comparacao e a vitima ficaria presa para"
                    + " sempre.");
        }
        return distanciaAoPonto <= raioDeRuptura;
    }

    /**
     * O puxao deste tick: o deslocamento a aplicar na vitima, ja limitado.
     *
     * <p><b>O limite e o ponto inteiro desta funcao.</b> Puxar a vitima direto
     * para o ponto seria mais simples e funcionaria em campo aberto; com a vitima
     * a tres blocos -- depois de um knockback, de um degrau, de um empurrao de
     * aliado -- ela seria ARREMESSADA para dentro do bicho a tres blocos por
     * tick, atravessando o que houvesse no caminho. Nao da erro: da um jogador
     * que aparece do outro lado da parede, ou que morre de queda no tranco
     * seguinte.</p>
     *
     * <p>Ja no ponto, o puxao e ZERO. Sem esse caso, o vetor normalizado de um
     * deslocamento quase nulo faria a vitima vibrar em torno da pinca todo tick,
     * e a vibracao e o tipo de defeito que ninguem consegue descrever.</p>
     *
     * @return o vetor a somar na posicao da vitima; nunca maior que
     *         {@link #velocidadeMaxima()}
     */
    public Vec3 puxao(Vec3 posicaoDaVitima, Vec3 pontoDaPinca) {
        if (posicaoDaVitima == null || pontoDaPinca == null) {
            throw new IllegalArgumentException("posicao de puxao ausente");
        }
        Vec3 ate = pontoDaPinca.subtract(posicaoDaVitima);
        double distancia = ate.length();
        if (!Double.isFinite(distancia)) {
            throw new IllegalArgumentException("puxao invalido: a distancia nao e finita");
        }
        if (distancia <= 1.0E-4D) return Vec3.ZERO;
        return distancia <= velocidadeMaxima ? ate : ate.scale(velocidadeMaxima / distancia);
    }
}
