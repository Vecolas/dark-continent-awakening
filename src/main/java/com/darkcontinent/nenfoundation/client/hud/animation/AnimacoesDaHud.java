package com.darkcontinent.nenfoundation.client.hud.animation;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * As tres transicoes da HUD, num lugar so.
 *
 * <p>O plano pede animacao CONTIDA: flash leve ao tomar dano, fade curto quando
 * o chip muda, e a supressao de Zetsu entrando por rampa em vez de estalo. As
 * tres compartilham a mesma forma -- uma borda detectada, um relogio, e um
 * valor que decai -- e escrever cada uma no seu renderer daria tres relogios
 * ligeiramente diferentes.
 *
 * <p>O RELOGIO ENTRA COMO ARGUMENTO, sempre. Nenhum metodo aqui consulta o
 * tempo por conta propria, e e isso que permite provar a curva inteira sem
 * esperar nem um tick -- a mesma decisao que {@link HudValueAnimator} ja
 * carregava.
 *
 * <p>O TEMPO ANDANDO PARA TRAS REINICIA TUDO. Trocar de mundo, voltar ao menu e
 * entrar de novo faz o {@code gameTime} recomecar; sem esta guarda, o flash de
 * dano de uma sessao ficaria "em andamento" por horas na sessao seguinte, e
 * ninguem ligaria um ao outro.
 */
public final class AnimacoesDaHud {

    /** Quanto dura o flash de dano. Curto: ele avisa, e nao chama atencao. */
    private static final float DURACAO_DO_FLASH = 6.0F;

    /** O fade do chip. Ainda mais curto: trocar de estado tem de parecer imediato. */
    private static final float DURACAO_DO_FADE = 4.0F;

    /**
     * A rampa da supressao de Zetsu.
     *
     * <p>A MAIS LONGA DAS TRES, e de proposito: Zetsu e o unico estado que muda
     * a cor da barra de forma sustentada, e uma troca de cor instantanea numa
     * barra permanente le como falha de renderizacao.
     */
    private static final float DURACAO_DA_SUPRESSAO = 10.0F;

    private boolean iniciado;
    private double ultimoTick;

    private float vidaAnterior;
    private double inicioDoFlash = Double.NEGATIVE_INFINITY;

    private ResourceLocation chipAnterior;
    private double inicioDoFade = Double.NEGATIVE_INFINITY;

    private float supressao;
    private double supressaoEm;
    private boolean suprimindo;

    /**
     * Le o estado do quadro e arma as bordas.
     *
     * <p>CHAMADO UMA VEZ POR QUADRO, antes de qualquer leitura. Chamar duas
     * vezes nao quebra nada -- as bordas sao detectadas por comparacao, e nao
     * por contagem.
     */
    public void observar(double tick, float vida, Optional<ResourceLocation> dominante) {
        if (!this.iniciado || tick < this.ultimoTick) {
            reiniciar(tick, vida, dominante);
            return;
        }
        this.ultimoTick = tick;

        // SO A QUEDA ACENDE. Curar tambem e mudanca de vida, e um flash na cura
        // faria a HUD piscar durante a regeneracao inteira.
        if (Float.isFinite(vida) && vida < this.vidaAnterior) {
            this.inicioDoFlash = tick;
        }
        if (Float.isFinite(vida)) {
            this.vidaAnterior = vida;
        }

        ResourceLocation agora = dominante.orElse(null);
        if (!Objects.equals(agora, this.chipAnterior)) {
            this.inicioDoFade = tick;
            this.chipAnterior = agora;
        }

        boolean deveSuprimir = dominante
                .map(com.darkcontinent.nenfoundation.nen.technique.Zetsu.ID::equals)
                .orElse(false);
        // A rampa e congelada no valor ATUAL antes de mudar de direcao; sem
        // isso, ligar e desligar Zetsu depressa faria a cor saltar do meio da
        // rampa para a ponta.
        if (deveSuprimir != this.suprimindo) {
            this.supressao = supressao(tick);
            this.supressaoEm = tick;
            this.suprimindo = deveSuprimir;
        }
    }

    /** O flash de dano, de 1 logo apos a pancada ate 0. */
    public float flashDeDano(double tick) {
        return decaimento(tick, this.inicioDoFlash, DURACAO_DO_FLASH);
    }

    /**
     * A opacidade do chip: 0 no instante da troca, 1 quando assentou.
     *
     * <p>E o INVERSO do decaimento -- o chip nasce apagado e acende. Um chip que
     * nasce cheio e apaga leria como se o estado estivesse terminando, que e a
     * informacao oposta.
     */
    public float fadeDoChip(double tick) {
        if (this.inicioDoFade == Double.NEGATIVE_INFINITY) {
            return 1.0F;
        }
        return 1.0F - decaimento(tick, this.inicioDoFade, DURACAO_DO_FADE);
    }

    /** Quanto da supressao de Zetsu ja entrou, de 0 a 1. */
    public float supressao(double tick) {
        if (!this.iniciado) {
            return 0.0F;
        }
        float alvo = this.suprimindo ? 1.0F : 0.0F;
        float andado = (float) Math.clamp(
                (tick - this.supressaoEm) / DURACAO_DA_SUPRESSAO, 0.0D, 1.0D);
        return this.supressao + (alvo - this.supressao) * andado;
    }

    /** Esquece tudo. Chamado no mesmo ponto de saida que limpa o resto do cliente. */
    public void limpar() {
        this.iniciado = false;
        this.inicioDoFlash = Double.NEGATIVE_INFINITY;
        this.inicioDoFade = Double.NEGATIVE_INFINITY;
        this.chipAnterior = null;
        this.supressao = 0.0F;
        this.suprimindo = false;
        this.vidaAnterior = 0.0F;
    }

    private void reiniciar(double tick, float vida, Optional<ResourceLocation> dominante) {
        this.iniciado = true;
        this.ultimoTick = tick;
        this.vidaAnterior = Float.isFinite(vida) ? vida : 0.0F;
        this.chipAnterior = dominante.orElse(null);
        // NASCE ASSENTADO, e nao em transicao: entrar no mundo nao e "o estado
        // acabou de mudar", e um fade na primeira renderizacao faria a HUD
        // aparecer piscando toda vez que alguem entra no servidor.
        this.inicioDoFlash = Double.NEGATIVE_INFINITY;
        this.inicioDoFade = Double.NEGATIVE_INFINITY;
        this.suprimindo = dominante
                .map(com.darkcontinent.nenfoundation.nen.technique.Zetsu.ID::equals)
                .orElse(false);
        this.supressao = this.suprimindo ? 1.0F : 0.0F;
        this.supressaoEm = tick;
    }

    private static float decaimento(double tick, double inicio, float duracao) {
        if (inicio == Double.NEGATIVE_INFINITY || tick < inicio) {
            return 0.0F;
        }
        double andado = (tick - inicio) / duracao;
        return (float) Math.clamp(1.0D - andado, 0.0D, 1.0D);
    }
}
