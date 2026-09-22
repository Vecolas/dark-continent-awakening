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
     * e o arranhao de meio coracao acenderiam a aura a cada poucos segundos --
     * e um efeito que acende sempre deixa de comunicar qualquer coisa.
     */
    private static final float PISO_DE_DANO = 0.02F;

    /**
     * A fracao da vida maxima que produz um ripple de forca TOTAL.
     *
     * <p>Um quarto da vida. Acima disso o efeito satura, e e proposital: a
     * diferenca entre "levei uma pancada seria" e "quase morri" nao precisa
     * caber no halo -- ela ja esta na barra de vida.
     */
    private static final float DANO_DE_FORCA_TOTAL = 0.25F;

    private final Map<Integer, Float> vidaAnterior = new HashMap<>();

    /**
     * Registra a vida deste tick e devolve o impacto, se ela CAIU.
     *
     * <p><b>O GATILHO E A QUEDA DE VIDA, e nao a borda de {@code hurtTime}.</b>
     * A primeira versao exigia as duas coisas NO MESMO TICK -- o relogio de dano
     * subindo e a vida caindo --, e elas chegam ao cliente em pacotes separados,
     * que nao tem ordem garantida. Quando a vida chegava um tick depois, a borda
     * ja tinha passado e o ripple nunca nascia. Foi um dos tres motivos de
     * <i>"nao acende nunca"</i>, e o mais dificil de ver em teste: os testes
     * entregavam as duas coisas juntas, porque quem os escreveu tambem escreveu
     * o bug.
     *
     * <p>A queda de vida sozinha e o sinal mais honesto: ela SO acontece por
     * dano. E pancada absorvida por escudo -- que nao tira vida -- continua nao
     * acendendo, que e o comportamento desejado.
     *
     * @param entidadeId id da entidade observada
     * @param vida vida atual
     * @param vidaMaxima vida maxima; zero ou negativa desliga a medicao
     * @return o impacto recem-nascido, ou {@code null} quando nao houve queda
     */
    AuraImpactState registrar(int entidadeId, float vida, float vidaMaxima) {
        Float anterior = this.vidaAnterior.put(entidadeId, vida);

        // A PRIMEIRA OBSERVACAO ESTABELECE A BASE E NAO ACENDE NADA. Sem isto,
        // aproximar-se de alguem que ja esta ferido seria lido como uma pancada
        // que nunca foi vista -- o mesmo contrato do detector de Ten.
        if (anterior == null) {
            return null;
        }
        float forca = forcaDe(anterior, vida, vidaMaxima);
        return forca > 0.0F ? AuraImpactState.iniciar(AuraBodyRegion.TORSO, forca) : null;
    }

    /**
     * A forca do ripple: quanto da vida maxima saiu, numa curva.
     *
     * <p>A CURVA EXISTE PORQUE A FRACAO CRUA E INVISIVEL. Um soco de mao vazia
     * tira 1 de 20 -- 5% --, e um realce de 0,05 no multiplicador de alpha nao
     * aparece na tela. Era o terceiro motivo de "nao acende nunca". Com a raiz
     * sobre {@link #DANO_DE_FORCA_TOTAL}, o mesmo soco vale 0,45: um flash que
     * se ve, sem igualar o golpe que quase mata.
     *
     * <p>E a fracao e da vida MAXIMA, nao da atual: pela atual, o ultimo golpe
     * de alguem quase morto seria sempre o mais forte da luta.
     */
    private static float forcaDe(float vidaAnterior, float vidaAtual, float vidaMaxima) {
        if (!(vidaMaxima > 0.0F) || !Float.isFinite(vidaAnterior) || !Float.isFinite(vidaAtual)) {
            return 0.0F;
        }
        float perdida = vidaAnterior - vidaAtual;
        if (!(perdida > 0.0F)) {
            // Sem perda de vida nao houve impacto NA AURA: escudo, invulnerabilidade
            // ou cura. O corpo pode ate piscar; a aura nao reage.
            return 0.0F;
        }
        float fracao = perdida / vidaMaxima;
        if (fracao < PISO_DE_DANO) {
            return 0.0F;
        }
        return (float) Math.min(1.0D, Math.sqrt(fracao / DANO_DE_FORCA_TOTAL));
    }

    /** Esquece quem nao esta mais presente. Chamado todo tick, com quem esta a vista. */
    void reterSomente(Set<Integer> presentes) {
        this.vidaAnterior.keySet().retainAll(presentes);
    }

    void limpar() {
        this.vidaAnterior.clear();
    }
}
