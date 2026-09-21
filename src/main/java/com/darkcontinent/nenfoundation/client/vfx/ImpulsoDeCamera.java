package com.darkcontinent.nenfoundation.client.vfx;

import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * O empurrao minimo de camera no instante em que Ren e liberado.
 *
 * <p><b>SO NA ATIVACAO, E SO NO JOGADOR LOCAL.</b> As duas metades sao criterio
 * de aceite, e as duas falham de formas diferentes:
 *
 * <ul>
 *   <li><b>Tremor CONTINUO e irritante em minutos.</b> Ele parece bom nos
 *       primeiros dez segundos de um video e insuportavel numa sessao. A regua
 *       da issue e literal: ativar Ren vinte vezes seguidas sem que incomode.
 *       Por isso isto e um impulso que DECAI e termina, e nao um estado.</li>
 *   <li><b>Observador nao recebe nada.</b> Mexer na camera de quem esta olhando
 *       e tirar o controle da mao de alguem por causa do que outra pessoa fez --
 *       e, num servidor, fazer isso a distancia. O evento so e consultado para o
 *       jogador local porque {@link #disparar()} so e chamado pela sessao do
 *       proprio jogador.</li>
 * </ul>
 *
 * <p>O TAMANHO E DE UM QUINTO DE GRAU. A direcao de arte pede de 0,1 a 0,25
 * grau: o bastante para o corpo sentir, pouco demais para a mira se perder. Um
 * impulso de dois graus nao e "mais impacto" -- e uma arma disparando.
 *
 * <p>ESTADO ESTATICO, e pelo mesmo motivo de {@code AuraVisualSystem}: o
 * Minecraft carrega um mod de cliente por JVM, e ha exatamente uma camera. Nao e
 * o erro numero 2 do {@code CLAUDE.md} -- nao ha estado POR JOGADOR aqui, so o
 * do jogador local.
 */
public final class ImpulsoDeCamera {

    /**
     * O PICO do impulso, em graus. Limite de design, e nao botao de ajuste.
     *
     * <p>E o pico DE VERDADE, e nao uma amplitude de onde uma formula tira um
     * numero menor: a forma abaixo vale exatamente 1 no meio do caminho. Essa
     * distincao existe porque "0,1 a 0,25 grau" so e verificavel se o numero no
     * codigo for o numero que a camera recebe -- caso contrario o teste teria de
     * reimplementar a formula para saber onde esta o maximo.
     */
    public static final float GRAUS = 0.20F;

    /**
     * Quantos ticks o impulso leva para morrer.
     *
     * <p>SEIS TICKS -- 300 ms. Mais que isso deixa de ser um empurrao e vira
     * tremor, que e exatamente o que este arquivo existe para nao produzir.
     */
    public static final int DURACAO_EM_TICKS = 6;

    private static float restante;

    private ImpulsoDeCamera() {
    }

    /** Dispara o impulso. Chamado UMA vez, na borda de ativacao de Ren. */
    public static void disparar() {
        restante = DURACAO_EM_TICKS;
    }

    /** Avanca o decaimento um tick. */
    public static void aoTick() {
        if (restante > 0.0F) {
            restante -= 1.0F;
            if (restante < 0.0F) {
                restante = 0.0F;
            }
        }
    }

    /** Quem liga, desliga: o impulso nao sobrevive ao logout nem a morte. */
    public static void limpar() {
        restante = 0.0F;
    }

    /** Se ha impulso em curso. Regua do overlay de dev. */
    public static boolean ativo() {
        return restante > 0.0F;
    }

    /**
     * O deslocamento em graus neste instante.
     *
     * <p><b>UM EMPURRAO SO, E NAO UMA OSCILACAO QUE DECAI.</b> A versao com
     * oscilacao parece mais rica no papel e e pior em jogo por duas razoes. A
     * primeira e de leitura: duas ou tres idas e voltas em 300 ms sao TREMOR, e
     * tremor e exatamente o que o criterio de aceite reprova -- ativar Ren vinte
     * vezes seguidas sem que incomode. A segunda e de verificacao: com
     * oscilacao, o pico fica onde a formula o colocar, e "entre 0,1 e 0,25 grau"
     * vira uma propriedade que so da para conferir reimplementando a formula no
     * teste.
     *
     * <p>A forma e meio seno: SOBE de zero, chega ao pico no meio e VOLTA a
     * zero. Comecar e terminar em zero e o que faz a mira voltar exatamente para
     * onde estava -- um offset que some de uma vez le como "a tela escorregou".
     *
     * <p>PURA, e por isso provavel sem o jogo: o teste fixa o pico e o termino
     * exato em zero sem precisar de uma camera.
     */
    public static float deslocamentoEm(float restanteEmTicks, float parcial) {
        float t = restanteEmTicks - parcial;
        if (t <= 0.0F) {
            return 0.0F;
        }
        // `restante` CONTA PARA TRAS -- comeca cheio e chega a zero --, entao o
        // tempo DECORRIDO e o complemento. Trocar os dois nao lanca: so espelha
        // a curva, e um empurrao espelhado continua parecendo um empurrao.
        float decorrido = Math.clamp(1.0F - t / DURACAO_EM_TICKS, 0.0F, 1.0F);
        return GRAUS * (float) Math.sin(Math.PI * decorrido);
    }

    /**
     * Aplica o impulso a camera.
     *
     * <p>PITCH E ROLL, e nao yaw. Mexer no yaw gira a mira lateralmente, que e o
     * eixo em que o jogador esta mirando; um empurrao vertical com uma torcao
     * minima le como recuo sem tirar o alvo de baixo do centro.
     */
    public static void aoComputarAngulos(ViewportEvent.ComputeCameraAngles evento) {
        if (restante <= 0.0F) {
            return;
        }
        float d = deslocamentoEm(restante, (float) evento.getPartialTick());
        evento.setPitch(evento.getPitch() + d);
        evento.setRoll(evento.getRoll() + d * 0.4F);
    }
}
