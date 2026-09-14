package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * Armadura que vale POR ANGULO: a placa de frente, a costura de lado, o ventre atras.
 *
 * <p><b>A decisao que este arquivo carrega.</b> Um bicho de armadura 8 e HP 60
 * nao e um quebra-cabeca: e um atraso. Bater de frente e a coisa mais natural do
 * mundo e e exatamente a que nao funciona, e sem uma forma de o jogador
 * descobrir isso a ficha inteira vira "peon gordo". A saida nao e baixar a
 * armadura -- baixar a armadura resolve o atraso e apaga o quebra-cabeca junto.
 * A saida e a armadura ter LADO.</p>
 *
 * <p><b>Por que nao um {@link WeakPointResolver}.</b> Ele foi feito para o caso
 * oposto e so sabe dizer "acima de tal altura E dentro de um cone frontal" --
 * os dois limiares dele sao PISOS. Para marcar as costas como vulneravel seria
 * preciso escrever {@code cossenoMinimo = -1}, que aprova o bicho inteiro: todo
 * golpe viraria critico sem que uma linha de log mudasse. Alem disso o resolver
 * devolve um id de regiao para MULTIPLICAR DANO, e multiplicar dano aqui seria a
 * segunda conta de dano no mesmo mob -- o erro que este projeto ja sabe que
 * comete, e cujo numero final fica plausivel demais para alguem notar sem medir.
 * Esta regra nao multiplica nada: ela diz quanto da PLACA cobre aquele angulo, e
 * quem paga o preco continua sendo a formula de armadura do proprio jogo.</p>
 *
 * <p><b>Tres faces, e nao duas.</b> O flanco existe porque ele e o degrau que
 * ensina: quem contorna pela metade sente a diferenca e continua contornando.
 * Sem degrau, o jogador que chegou ao lado leva a mesma recusa que levava de
 * frente, conclui que contornar nao serve e volta a bater na placa -- e a regra
 * continua funcionando, sem nunca ter sido descoberta.</p>
 *
 * <p><b>Logica pura.</b> Sem mundo, sem entidade, sem numero proprio: o cosseno
 * chega medido pelo servidor e as fracoes vem do perfil do bicho. E o que
 * permite provar a regra sem servidor de pe, e o que garante que nenhum caminho
 * de cliente consiga alimenta-la.</p>
 *
 * @param cossenoDaFrente a partir deste cosseno o golpe e frontal; 1 e bem de
 *        frente, -1 e bem pelas costas
 * @param cossenoDoFlanco abaixo deste cosseno o golpe ja e pelas costas
 * @param placaDeFrente fracao da armadura que vale de frente, 0..1
 * @param placaDeFlanco fracao que vale pelo lado
 * @param placaDeTras fracao que vale pelas costas
 */
public record RegrasDeCarapacaOrientada(double cossenoDaFrente, double cossenoDoFlanco,
        float placaDeFrente, float placaDeFlanco, float placaDeTras) {

    public RegrasDeCarapacaOrientada {
        if (!faixaDeCosseno(cossenoDaFrente) || !faixaDeCosseno(cossenoDoFlanco)) {
            throw new IllegalArgumentException("cosseno fora de [-1,1]: frente=" + cossenoDaFrente
                    + " flanco=" + cossenoDoFlanco + ". Fora da faixa um dos arcos fica vazio e a"
                    + " carapaca passa a valer igual em volta do bicho inteiro.");
        }
        if (cossenoDoFlanco >= cossenoDaFrente) {
            throw new IllegalArgumentException("o arco de flanco esta vazio: flanco="
                    + cossenoDoFlanco + " nao e menor que frente=" + cossenoDaFrente
                    + ". Sem degrau intermediario, quem contorna pela metade recebe a mesma"
                    + " recusa que recebia de frente, conclui que contornar nao serve e volta a"
                    + " bater na placa.");
        }
        if (!fracao(placaDeFrente) || !fracao(placaDeFlanco) || !fracao(placaDeTras)) {
            throw new IllegalArgumentException("fracao de placa fora de [0,1]: frente="
                    + placaDeFrente + " flanco=" + placaDeFlanco + " tras=" + placaDeTras
                    + ". Acima de 1 a carapaca inventaria armadura que o perfil nao declara, e o"
                    + " numero do perfil viraria um botao morto.");
        }
        if (placaDeTras >= placaDeFrente) {
            throw new IllegalArgumentException("a placa de tras (" + placaDeTras + ") nao e menor"
                    + " que a de frente (" + placaDeFrente + "): chegar pelas costas deixa de"
                    + " pagar, e o encontro inteiro -- que e sobre chegar pelas costas -- vira um"
                    + " saco de pancada com armadura. Nada disso levanta excecao em jogo.");
        }
        if (placaDeFlanco > placaDeFrente || placaDeFlanco < placaDeTras) {
            throw new IllegalArgumentException("o flanco (" + placaDeFlanco + ") tem de ficar"
                    + " ENTRE a frente (" + placaDeFrente + ") e as costas (" + placaDeTras
                    + "): fora dessa ordem o bicho ensina o contrario do que a regra cobra, e o"
                    + " jogador aprende um caminho que o servidor pune.");
        }
    }

    private static boolean faixaDeCosseno(double valor) {
        return Double.isFinite(valor) && valor >= -1.0D && valor <= 1.0D;
    }

    private static boolean fracao(float valor) {
        return Float.isFinite(valor) && valor >= 0.0F && valor <= 1.0F;
    }

    /**
     * Por onde o golpe entrou.
     *
     * @param cossenoAteOAtacante produto escalar entre a frente do CORPO do bicho
     *        e a direcao horizontal ate quem bateu: 1 de frente, -1 pelas costas
     * @throws IllegalArgumentException se o cosseno nao for finito -- um NaN aqui
     *         reprovaria toda comparacao e devolveria {@code VENTRE} em silencio,
     *         entregando o bicho a quem batesse de qualquer angulo
     */
    public FaceDaCarapaca faceAtingida(double cossenoAteOAtacante) {
        if (!Double.isFinite(cossenoAteOAtacante)) {
            throw new IllegalArgumentException("cosseno de impacto invalido: "
                    + cossenoAteOAtacante + ". NaN perde toda comparacao e cairia no ventre sem"
                    + " uma linha de log, entregando o bicho de qualquer angulo.");
        }
        if (cossenoAteOAtacante >= cossenoDaFrente) return FaceDaCarapaca.FRENTE;
        if (cossenoAteOAtacante >= cossenoDoFlanco) return FaceDaCarapaca.FLANCO;
        return FaceDaCarapaca.VENTRE;
    }

    /** A fracao da placa que cobre aquele angulo. */
    public float fracaoDaPlaca(FaceDaCarapaca face) {
        return switch (face) {
            case FRENTE -> placaDeFrente;
            case FLANCO -> placaDeFlanco;
            case VENTRE -> placaDeTras;
        };
    }

    /**
     * Quanta armadura o golpe encontra de fato.
     *
     * <p>Devolve ARMADURA, e nao dano. Quem transforma armadura em dano continua
     * sendo a formula do proprio jogo, chamada uma unica vez, no unico lugar em
     * que ela ja era chamada. Devolver dano daqui criaria a segunda conta de dano
     * do mesmo mob, e o numero final ficaria plausivel demais para alguem notar
     * sem medir.</p>
     *
     * @param placaTotal o {@code ARMOR} que o servidor mediu no atributo, agora
     * @param cossenoAteOAtacante o angulo medido pelo servidor
     */
    public float placaEfetiva(float placaTotal, double cossenoAteOAtacante) {
        if (!Float.isFinite(placaTotal) || placaTotal < 0.0F) {
            throw new IllegalArgumentException("armadura invalida: " + placaTotal);
        }
        return placaTotal * fracaoDaPlaca(faceAtingida(cossenoAteOAtacante));
    }
}
