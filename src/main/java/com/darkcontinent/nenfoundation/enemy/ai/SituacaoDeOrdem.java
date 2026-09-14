package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * Os fatos que o servidor ja apurou antes de perguntar se sai uma ordem.
 *
 * <p><b>Nenhum campo aqui e uma entidade, e nenhum e um {@code Squad}.</b> Se a
 * regra recebesse o bando, ela poderia MEXER nele -- e mexer no bando dentro de
 * uma regra de decisao e como duas Goals acabam trocando o alvo no mesmo tick, a
 * ultima vencendo. Quem escreve no bando e o consumidor, num ponto so, depois de
 * ler a resposta.</p>
 *
 * @param lidera ela e a lider DESTE bando agora -- lido do {@code Squad}, nunca de
 *        um campo local, porque promocao troca o papel no meio da vida
 * @param temBando ha bando ao qual ela pertence
 * @param noOrcamento este e o tick de coordenacao de {@code SquadRules}
 * @param postura o que {@link RegrasDeComandoAereo} decidiu para este tick
 * @param alvoProprioInedito ela enxerga um alvo que o bando ainda nao tem
 * @param bandoEspalhado ha membro alem do raio de reforco do bando
 * @param membrosVivos quantos membros o bando tem AGORA
 */
public record SituacaoDeOrdem(boolean lidera, boolean temBando, boolean noOrcamento,
        PosturaDeComando postura, boolean alvoProprioInedito, boolean bandoEspalhado,
        int membrosVivos) {

    public SituacaoDeOrdem {
        if (postura == null) {
            throw new NullPointerException("situacao de ordem sem postura: nulo aqui viraria"
                    + " NullPointerException no meio do tick do servidor, com pilha que nao diz"
                    + " qual comandante deixou de responder");
        }
        if (membrosVivos < 0) {
            throw new IllegalArgumentException("membros vivos negativo: " + membrosVivos
                    + ". Negativo compararia menor que qualquer minimo e faria a comandante nunca"
                    + " mandar reagrupar, sem nada acusar");
        }
        if (!temBando && membrosVivos > 0) {
            throw new IllegalArgumentException("sem bando e com " + membrosVivos + " membros: os"
                    + " dois campos discordam, e a decisao acabaria mandando ordem para um bando"
                    + " que o resto do sistema diz nao existir");
        }
        if (lidera && !temBando) {
            throw new IllegalArgumentException("lider de bando nenhum: um indice de bando que"
                    + " sobreviveu ao bando e exatamente a entrada morta que SquadRegistry existe"
                    + " para nao deixar acontecer");
        }
    }
}
