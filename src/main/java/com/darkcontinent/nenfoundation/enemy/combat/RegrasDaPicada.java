package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * Quando um atacante SOLITARIO de flanco compromete o golpe, sem mundo e sem
 * entidade.
 *
 * <p>Ela existe porque "aproxima-se pelo flanco" e uma frase de documento ate
 * virar um numero. Enquanto for frase, cada goal escreve o proprio {@code if}, e
 * o terceiro esquece de conferir o angulo -- o oficial passa a mergulhar de
 * frente e ninguem nota, porque um mob rapido atacando de frente parece
 * perfeitamente normal. Aqui a decisao e uma so, e ela responde COM MOTIVO.</p>
 *
 * <p><b>Onde termina {@code enemy.ai.RegrasDeFlanco} e onde comeca esta.</b>
 * Aquela e do BANDO: ela recebe a ordem do esquadrao, exige no minimo dois
 * membros vivos e reprova quem tentar flanquear sozinho -- porque um flanqueador
 * de matilha depende de outro segurando a frente do alvo. Esta e de um oficial
 * SOLITARIO, cujo perfil de spawn tem teto 1: nao ha esquadrao, nao ha ordem, e
 * o que segura a frente do alvo e a propria velocidade dela. Elas compartilham o
 * vocabulario (arco frontal, contorno, investida) de proposito, para que ninguem
 * escreva um terceiro; o que nao podem compartilhar e o corpo, porque a regra do
 * bando devolveria RECUAR para sempre a um bicho que nasce sozinho -- e um mob
 * que nunca ataca nao levanta erro nenhum.</p>
 *
 * <p><b>O cosseno e medido do lado do ALVO, e nao do atacante.</b> Esta e a
 * diferenca que faz a regra valer: o que importa nao e para onde o oficial olha
 * -- ele sempre olha para a vitima -- e sim se a VITIMA o tem no campo de visao.
 * Medir do lado errado nao levanta erro nenhum: a conta fica plausivel, o valor
 * fica sempre proximo de 1, e a regra passa a aprovar todo ataque.</p>
 *
 * <p><b>O raio do contorno e a distancia de investida se leem JUNTOS.</b> Ver o
 * construtor: com o contorno dentro da investida, reposicionar deixaria o oficial
 * parado exatamente onde ele ja podia picar, e ele oscilaria entre "encarada" e
 * "pica" no mesmo ponto -- o que o jogador le como um bicho travado, nunca como
 * uma tatica.</p>
 *
 * <p>Os numeros sao injetados por {@code MosquitoOfficerTuning}; este record nao
 * conhece nenhum deles.</p>
 *
 * @param cossenoDoArcoFrontal acima disto o alvo esta encarando e o ataque e recusado
 * @param raioDoContorno raio do anel onde ela circula enquanto o alvo encara
 * @param distanciaDeInvestida distancia de centro a centro em que a agulha decide picar
 */
public record RegrasDaPicada(double cossenoDoArcoFrontal, double raioDoContorno,
        double distanciaDeInvestida) {

    public RegrasDaPicada {
        if (!Double.isFinite(cossenoDoArcoFrontal)
                || cossenoDoArcoFrontal <= -1.0D || cossenoDoArcoFrontal >= 1.0D) {
            throw new IllegalArgumentException("cosseno do arco frontal fora de (-1, 1): "
                    + cossenoDoArcoFrontal + ". Em 1 ela ataca de qualquer angulo e o flanco"
                    + " deixa de existir; em -1 ela so ataca de costas exatas e nunca ataca."
                    + " Nenhum dos dois levanta erro: um da um mob comum, o outro da um mob"
                    + " que so fica dando voltas.");
        }
        if (!Double.isFinite(distanciaDeInvestida) || distanciaDeInvestida <= 0.0D) {
            throw new IllegalArgumentException("distancia de investida invalida: "
                    + distanciaDeInvestida + ". Zero ou negativa e um golpe que nunca encosta.");
        }
        if (!Double.isFinite(raioDoContorno) || raioDoContorno <= distanciaDeInvestida) {
            throw new IllegalArgumentException("o raio do contorno (" + raioDoContorno + ") nao e"
                    + " maior que a distancia de investida (" + distanciaDeInvestida + "):"
                    + " reposicionar deixaria o oficial parado onde ele ja podia picar, e ele"
                    + " oscilaria entre recuar e atacar no mesmo ponto -- que o jogador le como"
                    + " bicho travado");
        }
    }

    /**
     * A UNICA decisao de picar. Ela e pura: quem chama move e ataca, nao decide.
     *
     * <p>A ordem das perguntas e a ordem da HISTORIA: sem alvo nao ha nada a
     * decidir; quem recolheu a aura nao esta lutando por nenhum motivo; parede e
     * parede mesmo pelas costas; encarar vence a distancia -- e o ponto do bicho,
     * e um oficial que ataca de frente so porque chegou perto e um oficial sem
     * regra; e so entao a distancia decide.</p>
     *
     * @param temAlvo ha alvo vivo e lembrado
     * @param escondendoAAura ela decidiu recolher a aura neste instante; a decisao
     *        e do Nen Foundation, e aqui chega ja tomada, como POSTURA
     * @param distancia distancia de centro a centro ate o alvo
     * @param cossenoDoOlharDoAlvo 1 quando o alvo esta olhando bem para ela, -1
     *        quando ela esta exatamente pelas costas dele
     * @param caminhoLivre ha linha de visao ate o alvo
     */
    public DecisaoDaPicada decidir(boolean temAlvo, boolean escondendoAAura, double distancia,
            double cossenoDoOlharDoAlvo, boolean caminhoLivre) {
        if (!temAlvo) return DecisaoDaPicada.SEM_ALVO;
        if (escondendoAAura) return DecisaoDaPicada.ESCONDIDA;
        if (!Double.isFinite(distancia) || distancia < 0.0D
                || !Double.isFinite(cossenoDoOlharDoAlvo)) {
            throw new IllegalArgumentException("medidas de picada invalidas: distancia="
                    + distancia + " cosseno=" + cossenoDoOlharDoAlvo + ". Um valor nao-finito nao"
                    + " daria erro na comparacao -- daria um oficial que nunca ataca, e ninguem"
                    + " procuraria a causa numa distancia.");
        }
        if (!caminhoLivre) return DecisaoDaPicada.SEM_LINHA_DE_VISAO;
        if (cossenoDoOlharDoAlvo > cossenoDoArcoFrontal) return DecisaoDaPicada.ENCARADA;
        if (distancia > distanciaDeInvestida) return DecisaoDaPicada.LONGE;
        return DecisaoDaPicada.PICA;
    }
}
