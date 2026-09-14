package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * A locomocao do Bubble Horse: IMPULSO com PAUSA, e nunca caminhada.
 *
 * <p>Carrega a decisao central do bicho: <b>ele foge, e a fuga dele tem
 * compasso.</b> O servidor nao empurra o cavalo um pouquinho a cada tick -- ele
 * espera {@code ticksDePausa}, da um impulso, e espera de novo. Essa pausa e a
 * unica coisa que torna o salto PREVISIVEL, e antecipar o salto e a resposta que
 * o encontro ensina: perseguir um bicho de velocidade 0.45 nao funciona.</p>
 *
 * <p><b>Por que a pausa e regra e nao consequencia da fisica.</b> Deixar a
 * locomocao a cargo de {@code PathNavigation} entregaria um cavalo que desliza
 * suavemente para longe -- rapido, correto, e sem um unico instante em que o
 * jogador possa ler "agora ele vai pular para la". Nao ha erro nisso: ha um mob
 * que ninguem aprende a pegar, e a conclusao que chega e "esse bicho e chato".</p>
 *
 * <p><b>O COICE MORA AQUI, e nao num arquivo de ataque.</b> A patada deste bicho
 * nao e um ataque procurado: e o que sobra quando fugir deixou de ser possivel.
 * Escrita junto da regra de fuga, ela e obrigada a conhecer a distancia de
 * conforto -- e o construtor cobra que o alcance do coice seja MENOR que ela. Num
 * arquivo separado, os dois numeros divergiriam e o cavalo passaria a coicear
 * antes de tentar fugir, virando um mob agressivo de dano 3 que ninguem projetou.</p>
 *
 * <p><b>Ela nao consulta mundo.</b> Chao, ar, distancia e exaustao chegam ja
 * medidos pelo servidor. E o que permite provar as oito saidas sem servidor de
 * pe, e o que impede qualquer caminho de cliente de alimenta-la.</p>
 *
 * @param ticksDePausa quantos ticks o bicho espera entre um impulso e o proximo
 * @param ticksDeArco quantos ticks o voo do salto ocupa; a soma com a pausa e o
 *        CICLO, copiado pelo gerador de animacao como duracao do clipe de salto
 * @param impulsoHorizontal blocos por tick somados ao afastamento no impulso
 * @param impulsoVertical blocos por tick somados para cima no impulso
 * @param distanciaDeConforto a que distancia a ameaca deixa de valer uma fuga
 * @param distanciaDoCoice a que distancia a ameaca esta perto o bastante para a
 *        patada -- obrigatoriamente MENOR que a distancia de conforto
 */
public record RegrasDeSaltoDeBolha(int ticksDePausa, int ticksDeArco,
        double impulsoHorizontal, double impulsoVertical,
        double distanciaDeConforto, double distanciaDoCoice) {

    public RegrasDeSaltoDeBolha {
        // Pausa zero e um impulso por tick: o cavalo desliza, o clipe de salto
        // nunca completa um ciclo, e o jogador perde o unico sinal que ele tem
        // para prever onde o bicho cai. Isso nao da erro -- da uma locomocao que
        // parece bug de fisica.
        if (ticksDePausa < 1) {
            throw new IllegalArgumentException("ticks de pausa invalidos: " + ticksDePausa
                    + ". Sem pausa nao ha salto: ha um empurrao continuo, e o bicho passa a"
                    + " deslizar em vez de saltar -- sem uma linha de log.");
        }
        if (ticksDeArco < 1) {
            throw new IllegalArgumentException("ticks de arco invalidos: " + ticksDeArco
                    + ". O arco e a metade do ciclo que o clipe de animacao representa; com zero,"
                    + " o gerador de arte copiaria um ciclo que e so pausa.");
        }
        if (!Double.isFinite(impulsoHorizontal) || impulsoHorizontal <= 0.0D) {
            throw new IllegalArgumentException("impulso horizontal invalido: " + impulsoHorizontal);
        }
        // Impulso vertical zero e uma caminhada com nome de salto: a animacao
        // continua mostrando o arco, o corpo nunca sai do chao, e as duas coisas
        // discordam em silencio.
        if (!Double.isFinite(impulsoVertical) || impulsoVertical <= 0.0D) {
            throw new IllegalArgumentException("impulso vertical invalido: " + impulsoVertical
                    + ". Sem subida nao ha salto, so um escorregao -- e o clipe de arco passaria a"
                    + " mostrar um voo que o corpo nao faz.");
        }
        if (!Double.isFinite(distanciaDeConforto) || distanciaDeConforto <= 0.0D) {
            throw new IllegalArgumentException("distancia de conforto invalida: " + distanciaDeConforto);
        }
        if (!Double.isFinite(distanciaDoCoice) || distanciaDoCoice <= 0.0D) {
            throw new IllegalArgumentException("distancia do coice invalida: " + distanciaDoCoice);
        }
        // A INVERSAO QUE ESTE PROJETO PRECISA IMPEDIR. Com o alcance do coice
        // maior ou igual a distancia de conforto, o cavalo decide golpear antes de
        // decidir fugir: a ficha diz "ele foge, e o dano 3 e o coice de quem foi
        // encurralado", e o que nasce e um mob agressivo de dano baixo. Ele
        // funciona, ataca, anima e passa em todo portao -- e ensina o oposto.
        if (distanciaDoCoice >= distanciaDeConforto) {
            throw new IllegalArgumentException("o coice alcanca " + distanciaDoCoice
                    + " e a distancia de conforto e " + distanciaDeConforto
                    + ": o bicho decidiria golpear antes de decidir fugir, e um mob que so deveria"
                    + " coicear encurralado viraria um perseguidor de dano 3 -- sem que nada"
                    + " reprovasse.");
        }
    }

    /** O ciclo inteiro, em ticks. O gerador de animacao copia exatamente isto. */
    public int cicloEmTicks() {
        return ticksDePausa + ticksDeArco;
    }

    /**
     * A UNICA decisao de saltar. Ela e pura: quem chama aplica, nao decide.
     *
     * <p>A ordem das perguntas e a ordem da HISTORIA, e nao uma otimizacao. O que
     * trava o corpo vem primeiro -- exausto, cambaleando, em golpe -- porque
     * nenhum deles admite impulso por nenhum motivo. Depois vem a fisica (ja no
     * ar), depois o compasso (na pausa) e so entao a ameaca.</p>
     *
     * @param exausto a janela de captura esta aberta; ele PAROU de fugir
     * @param cambaleando {@code StaggerState.cambaleando()}
     * @param emGolpe a patada esta em curso (fase diferente de IDLE/COMPLETE)
     * @param noChao {@code onGround()} medido pelo servidor
     * @param ticksDesdeOSalto ticks decorridos desde o ultimo impulso aplicado
     * @param distanciaDaAmeaca distancia ate o alvo, ou NaN quando nao ha alvo
     */
    public DecisaoDeSaltoDeBolha decidir(boolean exausto, boolean cambaleando, boolean emGolpe,
            boolean noChao, int ticksDesdeOSalto, double distanciaDaAmeaca) {
        if (ticksDesdeOSalto < 0) {
            throw new IllegalArgumentException("ticks desde o salto negativos: " + ticksDesdeOSalto);
        }
        if (exausto) return DecisaoDeSaltoDeBolha.EXAUSTO;
        if (cambaleando) return DecisaoDeSaltoDeBolha.CAMBALEANDO;
        if (emGolpe) return DecisaoDeSaltoDeBolha.EM_GOLPE;
        if (!noChao) return DecisaoDeSaltoDeBolha.NO_AR;
        if (ticksDesdeOSalto < ticksDePausa) return DecisaoDeSaltoDeBolha.NA_PAUSA;
        // NaN NAO e "perto". Uma medida degenerada -- alvo em cima do bicho, vetor
        // de comprimento zero -- nao pode virar "foge" por acidente de comparacao,
        // senao o cavalo dispara sozinho em campo vazio e o relato e "ele foge do
        // nada".
        if (!Double.isFinite(distanciaDaAmeaca) || distanciaDaAmeaca < 0.0D) {
            return DecisaoDeSaltoDeBolha.SEM_AMEACA;
        }
        if (distanciaDaAmeaca > distanciaDeConforto) return DecisaoDeSaltoDeBolha.AMEACA_LONGE;
        return DecisaoDeSaltoDeBolha.SALTA;
    }

    /**
     * Ele coiceia -- e SO quando fugir deixou de ser possivel.
     *
     * <p>As tres condicoes valem juntas, e a do meio e a que define o bicho:
     * {@code saidaBloqueada} e o servidor dizendo que a fuga bateu em parede. Sem
     * ela, a patada viraria um ataque de proximidade comum e o cavalo passaria a
     * trocar golpes -- que e exatamente o comportamento que a ficha proibe.</p>
     *
     * <p>Exausto ele NAO coiceia: a janela de captura e o momento em que o bicho
     * parou, e um cavalo que golpeia no meio dela devolveria ao jogador o motivo
     * para continuar batendo -- e continuar batendo e o que perde o card.</p>
     *
     * @param exausto a janela de captura esta aberta
     * @param distanciaDaAmeaca distancia ate o alvo, ou NaN quando nao ha alvo
     * @param saidaBloqueada o servidor mediu que a fuga nao esta saindo do lugar
     */
    public boolean coiceia(boolean exausto, double distanciaDaAmeaca, boolean saidaBloqueada) {
        if (exausto || !saidaBloqueada) return false;
        if (!Double.isFinite(distanciaDaAmeaca) || distanciaDaAmeaca < 0.0D) return false;
        return distanciaDaAmeaca <= distanciaDoCoice;
    }
}
