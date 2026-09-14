package com.darkcontinent.nenfoundation.enemy.ai;

import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import java.util.Objects;

/**
 * Quando uma ordem sai do alto, qual ela e, e por que ela foi recusada.
 *
 * <p><b>Onde termina o {@code Squad} e onde comeca este arquivo.</b> O
 * {@code SquadController} decide o que vale para o bando inteiro -- recuar,
 * reagrupar, qual o alvo. Aqui mora o que e da COMANDANTE: ela so pode mandar do
 * alto, ela so manda no orcamento, e a ordem que justifica o alcance 36 dela e
 * publicar o alvo que ninguem no chao viu.</p>
 *
 * <p>Escrever "so manda do alto" dentro do {@code SquadController} faria TODA
 * familia que usa squad herdar uma regra de voo; escrever o alvo compartilhado
 * aqui faria cada membro escolher o proprio. Os dois erros dao o mesmo sintoma:
 * um esquadrao que se comporta como uma multidao, sem uma linha de log.</p>
 *
 * <p><b>O orcamento nao e copiado, e recebido.</b> {@code ticksDeAtualizacao},
 * teto e raio saem inteiros de {@link SquadRules}. Redeclarados aqui, o numero
 * daqui venceria em metade dos caminhos e o de la na outra metade, e a sessao de
 * balanceamento giraria um botao morto -- o erro numero 7 da lista do
 * CLAUDE.md.</p>
 *
 * @param bando as regras do grupo, de onde sai o orcamento de coordenacao e o teto
 * @param membrosParaReagrupar quantos membros vivos fazem "reagrupar" significar
 *        alguma coisa
 */
public record RegrasDeOrdemDeEsquadrao(SquadRules bando, int membrosParaReagrupar) {

    public RegrasDeOrdemDeEsquadrao {
        Objects.requireNonNull(bando, "regras de bando ausentes");
        if (membrosParaReagrupar < 2) {
            throw new IllegalArgumentException("membrosParaReagrupar = " + membrosParaReagrupar
                    + ": reagrupar um bando de um e uma ordem que nao muda nada, e a comandante"
                    + " passaria a gastar o orcamento inteiro repetindo-a. Nada acusa -- so um"
                    + " bando de um bicho que recebe ordens para sempre");
        }
        if (membrosParaReagrupar > bando.maximoDeMembros()) {
            throw new IllegalArgumentException("membrosParaReagrupar = " + membrosParaReagrupar
                    + " e o teto do bando e " + bando.maximoDeMembros() + ": o bando nunca alcanca"
                    + " o numero que autoriza reagrupar, entao a ordem nunca sai. A comandante"
                    + " nasce, voa, comanda e passa em tudo -- o unico sinal e um esquadrao que"
                    + " nunca se reorganiza");
        }
    }

    /**
     * A ordem deste tick de coordenacao -- ou a recusa, com motivo.
     *
     * <p>A ordem das perguntas nao e arbitraria, e ela decide o que a recusa
     * ENSINA:</p>
     *
     * <ol>
     *   <li><b>ha bando?</b> Primeiro de todos, porque sem bando nenhuma das
     *       perguntas seguintes tem sujeito -- e porque {@code SituacaoDeOrdem}
     *       proibe "lider de bando nenhum": perguntar pela lideranca antes
     *       deixaria {@link OrdemDoComandante#SEM_BANDO} inalcancavel, ou seja, um
     *       motivo de recusa que nunca sai e uma recusa que nunca se testa;</li>
     *   <li><b>ela lidera?</b> Sem isso, uma comandante alistada como membro
     *       comum mandaria no bando de outra, e as duas ordens do mesmo tick se
     *       sobrescreveriam -- a ultima vence, e o bando parece indeciso sem causa
     *       visivel;</li>
     *   <li><b>ela esta no alto?</b> Esta pergunta vem ANTES do orcamento de
     *       proposito. As duas recusam, e a resposta e a mesma -- nenhuma ordem --,
     *       mas so uma delas e a licao do encontro. Se o orcamento viesse antes,
     *       uma comandante no chao relataria {@code FORA_DO_ORCAMENTO} em nove de
     *       cada dez ticks, e quem fosse diagnosticar "por que ela parou de
     *       comandar" leria o relogio em vez da altitude;</li>
     *   <li><b>e o tick de coordenacao?</b> Ordem por tick e coordenacao por tick,
     *       e coordenacao por tick e consulta por membro ao quadrado: o sintoma e
     *       TPS caindo devagar numa colonia, nunca um erro;</li>
     *   <li><b>ha alvo inedito?</b> Trocar o alvo do bando vale mais que
     *       reagrupar: e a unica coisa que ela faz e que nenhum membro do chao
     *       poderia fazer;</li>
     *   <li><b>o bando espalhou?</b> So entao reagrupar.</li>
     * </ol>
     */
    public OrdemDoComandante decidir(SituacaoDeOrdem s) {
        Objects.requireNonNull(s, "situacao de ordem ausente");
        if (!s.temBando() || s.membrosVivos() < 1) return OrdemDoComandante.SEM_BANDO;
        if (!s.lidera()) return OrdemDoComandante.NAO_LIDERA;
        if (!s.postura().comanda()) return OrdemDoComandante.BAIXA_DEMAIS;
        if (!s.noOrcamento()) return OrdemDoComandante.FORA_DO_ORCAMENTO;
        if (s.alvoProprioInedito()) return OrdemDoComandante.TROCAR_ALVO;
        if (s.bandoEspalhado() && s.membrosVivos() >= membrosParaReagrupar) {
            return OrdemDoComandante.REAGRUPAR;
        }
        return OrdemDoComandante.NADA_A_ORDENAR;
    }

    /** Este tick pertence ao orcamento de coordenacao do bando? */
    public boolean noOrcamento(int tickDoMundo, int desfasagem) {
        return bando.atualizaNesteTick(tickDoMundo, desfasagem);
    }
}
