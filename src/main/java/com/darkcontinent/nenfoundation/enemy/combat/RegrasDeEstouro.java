package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * Regras do estouro de esporos, sem mundo e sem entidade.
 *
 * <p>Carrega as tres perguntas do Hyper Puffball numa fonte so: <em>quando o
 * fungo estoura</em>, <em>quem o estouro atinge</em> e <em>a partir de quando
 * ele avisa</em>. Espalhadas pela entidade, cada uma viraria um {@code if}
 * diferente, e a divergencia entre eles nao daria erro nenhum -- daria um bicho
 * que estoura em situacoes que ninguem consegue reproduzir.</p>
 *
 * <p><b>O ESTOURO NAO TEM ANGULO.</b> Nenhum metodo daqui recebe cosseno de
 * frente, ao contrario de um {@link WeakPointResolver}. O gatilho e o mesmo por
 * tras, por cima e de lado -- e e por isso que a textura do bicho marca os poros
 * nas quatro paredes e no topo, cobrada pela validacao
 * {@code valida_marca_do_gatilho_em_toda_volta} do gerador de arte.</p>
 *
 * <p><b>CADEIA E PROIBIDA, e a proibicao mora aqui.</b> Ver
 * {@link #atinge(boolean, boolean, double)}.</p>
 *
 * <p>Os numeros sao injetados por {@code HyperPuffballTuning}; este record nao
 * conhece nenhum deles.</p>
 *
 * @param fracaoDeVidaDoGatilho fracao da vida maxima em que ou abaixo da qual a casca cede
 * @param distanciaDoGatilho distancia maxima do ATACANTE para o toque contar
 * @param distanciaDoAviso distancia em que o fungo comeca a estufar como telegrafo
 * @param raio alcance do dano em area, medido do centro da entidade
 * @param dano dano do estouro -- NAO e o ATTACK_DAMAGE do perfil, que e zero
 */
public record RegrasDeEstouro(float fracaoDeVidaDoGatilho, double distanciaDoGatilho,
        double distanciaDoAviso, double raio, float dano) {

    public RegrasDeEstouro {
        if (!Float.isFinite(fracaoDeVidaDoGatilho) || fracaoDeVidaDoGatilho <= 0.0F
                || fracaoDeVidaDoGatilho > 1.0F
                || !Double.isFinite(distanciaDoGatilho) || distanciaDoGatilho <= 0.0D
                || !Double.isFinite(distanciaDoAviso)
                || !Double.isFinite(raio) || raio <= 0.0D
                || !Float.isFinite(dano) || dano <= 0.0F) {
            throw new IllegalArgumentException("regras de estouro invalidas");
        }
        // O AVISO TEM DE CHEGAR ANTES DO GATILHO. Com a distancia de aviso menor
        // que a do gatilho, o fungo so comecaria a estufar depois que o jogador ja
        // esta perto o bastante para armar o estouro -- um telegrafo que chega
        // tarde demais para servir. Isso nao daria erro nenhum: daria um bicho que
        // "explode do nada", que e exatamente o relato que ele existe para evitar.
        if (distanciaDoAviso < distanciaDoGatilho) {
            throw new IllegalArgumentException(
                    "a distancia de aviso (" + distanciaDoAviso + ") e menor que a do gatilho ("
                            + distanciaDoGatilho + "): o telegrafo comecaria depois do perigo");
        }
    }

    /**
     * A UNICA decisao de estourar. Ela e pura: quem chama aplica, nao decide.
     *
     * <p>A ordem das perguntas e a ordem da HISTORIA, e nao uma otimizacao: quem
     * ja estourou nao estoura de novo por nenhum motivo; sem atacante nao ha
     * distancia que se possa medir; longe demais e longe demais mesmo com a vida
     * no fim; e so entao a vida decide.</p>
     *
     * @param vidaDepoisDoDano vida APOS o dano ser aplicado -- ler a vida de antes
     *        adiaria o estouro por um golpe inteiro, sem que nada acusasse
     * @param vidaMaxima vida maxima da entidade
     * @param distanciaDoAtacante distancia ate quem CAUSOU o dano, ou NaN quando
     *        nao ha corpo (fogo, queda, veneno)
     * @param jaEstourou o marcador de uso unico da propria entidade
     */
    public DecisaoDeEstouro decidir(float vidaDepoisDoDano, float vidaMaxima,
            double distanciaDoAtacante, boolean jaEstourou) {
        if (!Float.isFinite(vidaMaxima) || vidaMaxima <= 0.0F || !Float.isFinite(vidaDepoisDoDano)) {
            throw new IllegalArgumentException("vida invalida para decidir estouro");
        }
        if (jaEstourou) return DecisaoDeEstouro.JA_ESTOUROU;
        if (Double.isNaN(distanciaDoAtacante)) return DecisaoDeEstouro.SEM_ATACANTE;
        if (distanciaDoAtacante > distanciaDoGatilho) return DecisaoDeEstouro.GATILHO_LONGE_DEMAIS;
        if (vidaDepoisDoDano > vidaMaxima * fracaoDeVidaDoGatilho) {
            return DecisaoDeEstouro.VIDA_ACIMA_DO_LIMIAR;
        }
        return DecisaoDeEstouro.ESTOURA;
    }

    /**
     * Quem o estouro atinge -- e a CADEIA MORRE AQUI.
     *
     * <p><b>A decisao: cadeia PROIBIDA.</b> Um estouro que machuca outro puffball
     * pode leva-lo abaixo do limiar dele, que estoura, que atinge um terceiro. A
     * cadeia terminaria -- cada fungo tem um unico estouro, entao o total e
     * limitado pelo numero de fungos no raio -- mas terminar nao e o mesmo que
     * prestar. Numa cadeia, o dano que mata o jogador vem de uma entidade que ja
     * estava morta quando o dano saiu, o relato vira "levei quatro estouros de um
     * bicho so", e a licao do bicho (matar de longe e seguro) deixa de valer,
     * porque o jogador que recuou ainda pode ser alcancado pelo segundo da fila.
     *
     * <p>Por isso a mesma especie NUNCA e vitima. O filtro e uma linha, mora
     * junto da regra e e provado por teste; a alternativa -- permitir e provar
     * que termina -- custaria uma prova que depende de um marcador de instancia
     * sobreviver a todo caminho de saida, e esse marcador e exatamente o tipo de
     * estado que este projeto ja sabe que esquece em um dos pontos de saida.
     *
     * @param mesmaEspecie a vitima candidata e outro Hyper Puffball
     * @param vivo a vitima candidata esta viva
     * @param distancia distancia do centro do fungo ate a vitima
     */
    public boolean atinge(boolean mesmaEspecie, boolean vivo, double distancia) {
        if (mesmaEspecie || !vivo) return false;
        // A comparacao contra o raio e feita AQUI e nao na caixa de busca do
        // mundo: um AABB inflado e um cubo, e um cubo alcanca mais nos cantos. Sem
        // esta linha, quem recuou na diagonal levaria dano que a nuvem desenhada
        // na tela nunca prometeu -- e a leitura de distancia, que e a unica defesa
        // contra este bicho, passaria a mentir em quatro direcoes.
        return Double.isFinite(distancia) && distancia >= 0.0D && distancia <= raio;
    }

    /**
     * Quando o fungo comeca a estufar.
     *
     * <p>O estufo NAO machuca ninguem: o dano do perfil e zero, e a janela ACTIVE
     * dele nunca consulta caixa de golpe. Ele existe para o jogador associar
     * "cheguei perto" a "aquilo reagiu" ANTES de bater.</p>
     *
     * @param distanciaDoAlvo distancia ate o alvo percebido, ou NaN se nao ha alvo
     */
    public boolean deveAvisar(double distanciaDoAlvo) {
        return Double.isFinite(distanciaDoAlvo) && distanciaDoAlvo >= 0.0D
                && distanciaDoAlvo <= distanciaDoAviso;
    }
}
