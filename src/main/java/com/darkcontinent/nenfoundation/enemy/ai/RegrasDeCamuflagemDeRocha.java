package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * Regras da camuflagem de rocha, sem mundo e sem entidade.
 *
 * <p>Carrega a decisao central do Melanin Lizard: <b>enquanto ninguem olha para
 * ele de perto, ele nao existe.</b> Camuflado, o lagarto nao anda, nao persegue
 * e nao e alvo de IA nenhuma -- ele e paisagem. A pista que o jogador aprende a
 * ler e o contrario da do man-faced ape: ali quem desvia o olhar e alcancado,
 * aqui quem OLHA acorda a pedra.</p>
 *
 * <p><b>Por que nao e {@link DisguiseRules}.</b> O disfarce do macaco responde
 * outra pergunta: "posso avancar agora?". Ele CAMINHA disfarcado, e a regra dele
 * exige raio de bando e ticks de reveal -- dois numeros que este bicho nao tem, e
 * que teriam de ser inventados para caber. Pior, {@code podeAproximar} tem o
 * sinal invertido em relacao ao que este mob precisa: o macaco anda quando nao e
 * observado, o lagarto congela. Espremer os dois na mesma regra faria um dos dois
 * ficar errado em silencio -- e "errado em silencio" aqui e um bicho que se mexe
 * enquanto o servidor o declara impossivel de mirar.</p>
 *
 * <p><b>Ela nao consulta mundo.</b> Distancia, visibilidade e o cosseno do olhar
 * chegam ja medidos pelo servidor. E o que permite provar as quatro saidas --
 * quebra por olhar, quebra por dano, recusa e recamuflagem -- sem servidor de pe,
 * e o que impede qualquer caminho de cliente de alimenta-la.</p>
 *
 * @param distanciaDeQuebra a que distancia um olhar derruba a camuflagem
 * @param cossenoDeObservacao quao de frente o olhar do observador precisa estar
 *        para contar como "esta olhando para mim" (1 = em cheio, 0 = de lado)
 * @param ticksSemAlvoParaRecamuflar quanto tempo sem alvo antes de virar pedra
 *        de novo
 */
public record RegrasDeCamuflagemDeRocha(double distanciaDeQuebra, double cossenoDeObservacao,
        int ticksSemAlvoParaRecamuflar) {

    public RegrasDeCamuflagemDeRocha {
        if (!Double.isFinite(distanciaDeQuebra) || distanciaDeQuebra <= 0.0D) {
            throw new IllegalArgumentException("distancia de quebra invalida: " + distanciaDeQuebra);
        }
        if (!Double.isFinite(cossenoDeObservacao) || cossenoDeObservacao > 1.0D) {
            throw new IllegalArgumentException("cosseno de observacao invalido: " + cossenoDeObservacao);
        }
        // Cosseno zero ou negativo significa "olhar para o lado, ou de costas, ja
        // conta como olhar para mim". A camuflagem continuaria existindo no codigo
        // e nao esconderia NADA: o lagarto quebraria assim que alguem chegasse
        // perto, de qualquer angulo. Isso nao da erro -- da um mob que parece ter
        // uma mecanica e nao tem, e ninguem consegue reportar o que falta.
        if (cossenoDeObservacao <= 0.0D) {
            throw new IllegalArgumentException("cosseno de observacao " + cossenoDeObservacao
                    + ": de lado ou de costas passaria a contar como olhar, e a camuflagem viraria"
                    + " enfeite -- sem nunca esconder ninguem de ninguem.");
        }
        // Zero tick para recamuflar e uma pedra que pisca: perdeu o alvo num tick,
        // sumiu no seguinte, reapareceu no outro. O jogador nao le isso como
        // camuflagem, le como bug de renderizacao.
        if (ticksSemAlvoParaRecamuflar < 1) {
            throw new IllegalArgumentException("ticks para recamuflar invalidos: "
                    + ticksSemAlvoParaRecamuflar);
        }
    }

    /**
     * ALGUEM ESTA OLHANDO PARA O LAGARTO, de perto.
     *
     * <p>As tres condicoes valem juntas: tem de estar visivel, dentro da
     * distancia e com o olhar apontado para ca. Medida nao finita (observador em
     * cima do bicho, vetor degenerado) NAO observa: um NaN nao pode virar "esta
     * olhando" por acidente de comparacao, senao a camuflagem cai sozinha no caso
     * exato em que o jogador esta encostado na pedra sem ter percebido nada.</p>
     */
    public boolean observado(boolean visivel, double distancia, double cossenoDoOlharDoObservador) {
        if (!visivel) return false;
        if (!Double.isFinite(distancia) || !Double.isFinite(cossenoDoOlharDoObservador)) return false;
        return distancia <= distanciaDeQuebra && cossenoDoOlharDoObservador >= cossenoDeObservacao;
    }

    /**
     * A camuflagem cai por OLHAR ou por DANO -- e so faz sentido camuflado.
     *
     * <p>Dano quebra mesmo sem ninguem olhando de proposito: quem acertou o
     * lagarto de longe, por acidente ou de raspao, ja provou que ele nao e pedra.
     * Sem isso, um bicho atingido continuaria "invisivel" para a propria IA e
     * ficaria apanhando parado ate morrer -- que nao e camuflagem, e paralisia.</p>
     */
    public boolean quebra(boolean camuflado, boolean observado, boolean sofreuDano) {
        return camuflado && (observado || sofreuDano);
    }

    /**
     * Camuflado ele NAO se move. E a metade visivel da regra.
     *
     * <p>A outra metade, {@link #podeSerAlvo}, e invisivel -- e por isso as duas
     * moram juntas aqui. Deixar o mob parado sem tirar a mira, ou tirar a mira
     * sem deixar o mob parado, sao os dois estados incoerentes possiveis, e
     * nenhum dos dois da erro.</p>
     */
    public boolean podeMover(boolean camuflado) {
        return !camuflado;
    }

    /**
     * Camuflado ele nao e alvo de IA nenhuma -- nem do jogador, nem de outro mob.
     *
     * <p>"Nao e alvo" e diferente de "nao pode ser atingido": quem viu o lagarto
     * e acerta nele deve acertar, e o acerto e justamente o que quebra a
     * camuflagem. O que esta regra nega e a MIRA AUTOMATICA -- o mob que escolhe
     * inimigo por varredura nao encontra uma pedra.</p>
     */
    public boolean podeSerAlvo(boolean camuflado) {
        return !camuflado;
    }

    /**
     * Volta a ser pedra: revelado, sem alvo, sem ninguem preso, e depois de
     * esperar.
     *
     * <p>A condicao de nao estar agarrando e a que nao e obvia: um lagarto que
     * recamufla com alguem na boca some da mira de quem ia salvar a vitima, e a
     * vitima fica presa a uma pedra. Nao da erro nenhum e e o pior relato de bug
     * que este mob consegue produzir.</p>
     */
    public boolean recamufla(boolean camuflado, boolean temAlvo, boolean agarrando, int ticksSemAlvo) {
        if (camuflado || temAlvo || agarrando) return false;
        return ticksSemAlvo >= ticksSemAlvoParaRecamuflar;
    }
}
