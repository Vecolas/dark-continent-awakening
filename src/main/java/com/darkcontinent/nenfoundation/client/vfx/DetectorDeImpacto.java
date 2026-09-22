package com.darkcontinent.nenfoundation.client.vfx;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Detecta o IMPACTO nos jogadores a vista, sem gastar protocolo (#103).
 *
 * <p><b>POR QUE O CLIENTE DECIDE ISTO SOZINHO.</b> A trilha AV nao gasta
 * contrato de servidor nem protocolo -- esta escrito no {@code CLAUDE.md} --, e
 * um payload novo para dizer "levou pancada" exigiria subir a versao pelo
 * procedimento do ADR-011 para um efeito que nao muda dano, defesa nem alcance.
 * O ripple e acabamento: ele pertence ao lado que desenha.
 *
 * <p>E o cliente ja sabe o suficiente, porque a vanilla sincroniza a ANIMACAO de
 * dano: {@code hurtTime} vai a {@code hurtDuration} no tick da pancada e desce
 * sozinho. A borda de subida e o impacto.
 *
 * <p><b>O QUE O CLIENTE NAO SABE, e por isso o ripple nasce no TRONCO.</b> Em
 * 1.21.1 o {@code LivingEntity.animateHurt(float yaw)} <b>descarta o yaw</b> --
 * ele so escreve {@code hurtDuration} e {@code hurtTime} --, e nao ha campo de
 * direcao no lado do cliente. Altura do golpe tambem nao viaja. Escolher
 * esquerda ou direita a partir de dado que nao existe seria inventar
 * informacao, e o erro apareceria como um ripple no braco de quem levou uma
 * flechada no peito -- plausivel demais para alguem reportar.
 *
 * <p>TRONCO e a escolha do proprio servidor para essa incerteza:
 * {@code FaixaDoCorpo.TRONCO} e descrita la como <i>"o meio do corpo; e onde
 * quase tudo acerta"</i>, e e o que {@code porAltura} devolve quando a altura
 * chega invalida. O ripple por regiao exata fica para o dia em que o servidor
 * puder dizer a regiao -- e esse dia custa uma versao de protocolo.
 *
 * <p><b>A PODA E POR PRESENCA, e nao por evento.</b> Mesma decisao de
 * {@link DetectorDeAtivacaoDeTen}: quem sai do alcance, morre ou troca de
 * dimensao deixa de estar na lista, e a entrada morre junto. Um handler de
 * morte cobriria os eventos que alguem lembrou; a poda cobre todos.
 */
final class DetectorDeImpacto {

    /**
     * Piso de dano que vira ripple, como fracao da vida maxima.
     *
     * <p>LIMITE DE DESENHO, nao balanceamento. Sem ele, dano de fome, de veneno
     * e o arranhao de meio coracao acenderiam a aura inteira a cada poucos
     * segundos -- e um efeito que acende sempre deixa de comunicar qualquer
     * coisa.
     */
    private static final float PISO_DE_DANO = 0.02F;

    private final Map<Integer, Leitura> anteriores = new HashMap<>();

    /**
     * Registra a leitura deste tick e devolve o impacto, se houve borda.
     *
     * @param entidadeId id da entidade observada
     * @param hurtTime o {@code hurtTime} sincronizado pela vanilla
     * @param vida vida atual
     * @param vidaMaxima vida maxima; zero ou negativa desliga a medicao
     * @return o impacto recem-nascido, ou {@code null} quando nao houve borda
     */
    AuraImpactState registrar(int entidadeId, int hurtTime, float vida, float vidaMaxima) {
        Leitura anterior = this.anteriores.put(entidadeId,
                new Leitura(hurtTime, vida));

        // A PRIMEIRA OBSERVACAO ESTABELECE A BASE E NAO ACENDE NADA. Sem esta
        // regra, aproximar-se de alguem que esta piscando de dano seria lido
        // como uma pancada que nunca foi vista -- o mesmo contrato que o
        // detector de Ten faz, e pelo mesmo motivo.
        if (anterior == null) {
            return null;
        }
        if (hurtTime <= anterior.hurtTime()) {
            return null;
        }
        float forca = forcaDe(anterior.vida(), vida, vidaMaxima);
        return forca > 0.0F ? AuraImpactState.iniciar(AuraBodyRegion.TORSO, forca) : null;
    }

    /**
     * A forca do ripple: quanto da vida maxima saiu nesta pancada.
     *
     * <p>A QUEDA DE VIDA, e nao um valor fixo. Um ripple de mesma intensidade
     * para meio coracao e para um golpe que quase matou nao informa nada. E a
     * fracao e da vida MAXIMA, nao da atual: pela atual, o ultimo golpe de
     * alguem quase morto seria sempre o mais forte da luta.
     */
    private static float forcaDe(float vidaAnterior, float vidaAtual, float vidaMaxima) {
        if (!(vidaMaxima > 0.0F) || !Float.isFinite(vidaAnterior) || !Float.isFinite(vidaAtual)) {
            return 0.0F;
        }
        float perdida = vidaAnterior - vidaAtual;
        if (!(perdida > 0.0F)) {
            // Pancada sem perda de vida -- absorvida por escudo, por armadura ou
            // por invulnerabilidade. O corpo pisca, e a aura nao reage: nao houve
            // impacto NELA.
            return 0.0F;
        }
        float fracao = perdida / vidaMaxima;
        return fracao < PISO_DE_DANO ? 0.0F : Math.min(1.0F, fracao);
    }

    /** Esquece quem nao esta mais presente. Chamado todo tick, com quem esta a vista. */
    void reterSomente(Set<Integer> presentes) {
        this.anteriores.keySet().retainAll(presentes);
    }

    void limpar() {
        this.anteriores.clear();
    }

    /** O que foi visto de uma entidade no tick anterior. */
    private record Leitura(int hurtTime, float vida) { }
}
