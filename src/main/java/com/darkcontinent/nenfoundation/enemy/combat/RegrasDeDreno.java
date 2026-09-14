package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * A regra do DRENO DE VIDA, sem mundo e sem entidade.
 *
 * <p><b>Dreno de VIDA, e nao de aura.</b> O nucleo de Nen ja usa a palavra
 * "dreno" para outra coisa -- {@code AuraFormulas} e {@code Ken} chamam assim o
 * gasto de aura por tick de uma tecnica ligada. Esta classe nao tem nada a ver
 * com aquilo: ela devolve pontos de VIDA ao inimigo que acertou, nao mexe em
 * aura nenhuma e nao conhece o Nen Foundation. Os dois vivem em pacotes
 * diferentes de proposito; o aviso esta aqui para que ninguem procure regra de
 * aura neste arquivo nem regra de combate naquele.</p>
 *
 * <p>Ela carrega as tres perguntas do Mosquito Officer numa fonte so: <em>quanto
 * cada picada devolve</em>, <em>o que conta como acerto valido</em> e <em>onde o
 * dreno PARA</em>. Espalhadas pela entidade, cada uma viraria um {@code if}
 * diferente, e a divergencia entre eles nao daria erro nenhum -- daria um oficial
 * que cura em situacoes que ninguem consegue reproduzir.</p>
 *
 * <p><b>O TETO E O CORACAO DESTA CLASSE.</b> Este oficial fica mais forte com o
 * que tira; sem um limite, um combate longo o leva a vida praticamente infinita e
 * o encontro deixa de ter fim. Um mob de 55 de vida que nao morre nao levanta
 * excecao, nao aparece no log e nao reprova portao nenhum: ele so transforma a
 * luta numa espera. O teto e o numero que separa "tensa" de "impossivel", e por
 * isso ele mora aqui e nao num {@code if} solto no meio do golpe.</p>
 *
 * <p><b>Ela e PURA, e isso e o que a torna testavel.</b> Nao le vida do mundo,
 * nao aplica cura, nao conhece entidade. Quem chama ja mediu o dano que de fato
 * ENTROU na vitima -- e nao o dano que o ataque pretendia -- e e essa diferenca
 * que faz o acerto absorvido, bloqueado ou desferido contra alguem invulneravel
 * nao curar nada. O chamador aplica; ele nao decide.</p>
 *
 * <p><b>O acumulado do encontro e do CHAMADOR, de proposito.</b> Guardado aqui,
 * este record deixaria de ser imutavel e dois oficiais que compartilhassem a
 * mesma instancia dividiriam o mesmo teto -- a versao deste projeto do erro
 * classico de estado de jogador num campo de classe. Guardado na entidade, ele
 * morre com ela e e zerado no mesmo ponto em que o encontro termina.</p>
 *
 * <p>Os numeros sao injetados por {@code MosquitoOfficerTuning}; este record nao
 * conhece nenhum deles.</p>
 *
 * @param fracaoDoDano quanto do dano APLICADO volta como vida, de 0 a 1
 * @param tetoDoEncontro maximo de vida que um unico encontro pode devolver
 * @param curaMinima piso abaixo do qual a picada nao paga nada
 */
public record RegrasDeDreno(float fracaoDoDano, float tetoDoEncontro, float curaMinima) {

    public RegrasDeDreno {
        if (!Float.isFinite(fracaoDoDano) || fracaoDoDano <= 0.0F || fracaoDoDano > 1.0F) {
            throw new IllegalArgumentException("fracao de dreno fora de (0, 1]: " + fracaoDoDano
                    + ". Acima de 1 a picada devolveria mais vida do que tirou, e dois oficiais"
                    + " lutando entre si ganhariam vida do nada.");
        }
        if (!Float.isFinite(tetoDoEncontro) || tetoDoEncontro <= 0.0F) {
            throw new IllegalArgumentException("teto de dreno invalido: " + tetoDoEncontro
                    + ". Sem teto, o combate longo leva o oficial a vida infinita e o encontro"
                    + " deixa de ter fim -- sem erro nenhum.");
        }
        if (!Float.isFinite(curaMinima) || curaMinima < 0.0F) {
            throw new IllegalArgumentException("cura minima invalida: " + curaMinima);
        }
        if (curaMinima >= tetoDoEncontro) {
            throw new IllegalArgumentException("a cura minima (" + curaMinima + ") e maior ou igual"
                    + " ao teto do encontro (" + tetoDoEncontro + "): nenhuma picada jamais"
                    + " pagaria, e o bicho inteiro viraria um oficial fraco sem mecanica nenhuma"
                    + " -- e nada acusaria isso");
        }
    }

    /**
     * A UNICA decisao de drenar. Ela e pura: quem chama aplica, nao decide.
     *
     * <p>A ordem das perguntas e a ordem da HISTORIA, e nao uma otimizacao: golpe
     * de outro nao e dela por nenhum motivo; vitima sem sangue nao da sangue
     * mesmo que o golpe tenha sido perfeito; picada que nao tirou vida nao tirou
     * nada; e so entao a conta comeca.</p>
     *
     * <p>O teto e conferido ANTES da vida que falta porque ele e o limite do
     * ENCONTRO e ela e o limite do INSTANTE: um oficial cheio de vida com teto
     * gasto e um oficial que nao drena mais hoje, e nao um que nao precisa agora.
     * Trocar a ordem nao mudaria a cura de nenhum caso -- mudaria o motivo
     * relatado, e motivo errado manda a proxima pessoa atras do bug errado.</p>
     *
     * @param danoAplicado dano que de fato ENTROU na vitima, medido pelo servidor
     *        -- ler o dano pretendido faria escudo, absorcao e invulnerabilidade
     *        alimentarem o oficial, e o acerto que nao machucou passaria a curar
     * @param golpeProprio o dano veio da picada DESTE oficial
     * @param vitimaDrenavel ha o que sugar nesta vitima
     * @param vidaAtual vida do oficial agora
     * @param vidaMaxima vida maxima do oficial
     * @param jaDrenadoNesteEncontro quanto este encontro ja devolveu
     */
    public ResultadoDeDreno decidir(float danoAplicado, boolean golpeProprio,
            boolean vitimaDrenavel, float vidaAtual, float vidaMaxima,
            float jaDrenadoNesteEncontro) {
        if (!Float.isFinite(vidaMaxima) || vidaMaxima <= 0.0F || !Float.isFinite(vidaAtual)
                || !Float.isFinite(jaDrenadoNesteEncontro) || jaDrenadoNesteEncontro < 0.0F) {
            throw new IllegalArgumentException("estado invalido para decidir dreno: vida="
                    + vidaAtual + "/" + vidaMaxima + " ja drenado=" + jaDrenadoNesteEncontro);
        }
        if (!golpeProprio) return ResultadoDeDreno.recusa(DecisaoDeDreno.GOLPE_DE_OUTRO);
        if (!vitimaDrenavel) return ResultadoDeDreno.recusa(DecisaoDeDreno.VITIMA_NAO_DRENAVEL);
        if (!Float.isFinite(danoAplicado) || danoAplicado <= 0.0F) {
            return ResultadoDeDreno.recusa(DecisaoDeDreno.SEM_DANO);
        }

        float bruta = danoAplicado * fracaoDoDano;
        if (bruta < curaMinima) return ResultadoDeDreno.recusa(DecisaoDeDreno.CURA_PEQUENA_DEMAIS);

        float restanteDoTeto = tetoDoEncontro - jaDrenadoNesteEncontro;
        if (restanteDoTeto <= 0.0F) return ResultadoDeDreno.recusa(DecisaoDeDreno.TETO_ATINGIDO);

        float faltandoDeVida = vidaMaxima - vidaAtual;
        if (faltandoDeVida <= 0.0F) return ResultadoDeDreno.recusa(DecisaoDeDreno.JA_ESTA_CHEIA);

        // O menor dos tres limites ganha, e o teto entra na conta junto com a vida
        // que falta. Cortar so pela vida faria o excedente NAO ser contabilizado,
        // e o oficial drenaria para sempre desde que estivesse quase cheio.
        float cura = Math.min(bruta, Math.min(restanteDoTeto, faltandoDeVida));
        return new ResultadoDeDreno(DecisaoDeDreno.DRENA, cura);
    }
}
