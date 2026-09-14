package com.darkcontinent.nenfoundation.client.vfx;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * O dono do controlador visual do jogador local, e o unico lugar que o tica.
 *
 * <p>POR QUE ELE EXISTE. O pacote {@code client.vfx} nasceu completo em
 * contrato e <b>sem ninguem chamando</b>: build verde, testes verdes, e nada em
 * tela. Este arquivo e a ponte que faltava entre o delta que chega do servidor
 * e o controlador que ja sabia interpolar.
 *
 * <p>SEM TIPO DE MINECRAFT NA ASSINATURA, de proposito. Assim a regra de
 * "quando reenviar o comando" e "quanto avancar por tick" da para provar sem o
 * jogo de pe -- e o que sobra do lado de la e so desenhar.
 *
 * <p>SO REENVIA QUANDO MUDA. Chamar {@code receber} todo tick zeraria a
 * transicao a cada tick, e a aura nunca sairia do primeiro quadro da animacao.
 * O sintoma nao seria erro: seria uma transicao que "nao funciona".
 */
public final class SessaoDeVfxDeAura {

    private final AuraVisualController controlador = new AuraVisualController();

    private AuraVisualMode ultimoModo = AuraVisualMode.OFF;
    private float ultimaIntensidade;
    private int ultimaCor;
    private AuraDistribution ultimaDistribuicao;
    private AuraDistribution distribuicaoDesteTick = AuraDistribution.uniforme();
    private boolean recebeuAlgumaVez;
    private boolean ativacaoDeTenPendente;

    /** O estado interpolado deste tick. Nunca nulo. */
    public AuraVisualState estado() {
        return this.controlador.atual();
    }

    /**
     * Um tick de cliente.
     *
     * @param ativas      tecnicas que o SERVIDOR confirmou como ligadas
     * @param intensidade de 0 a 1; vem do output efetivo, e nao da aura atual
     * @param cor         a cor da tecnica dominante, decidida por quem chama
     * @param escala      multiplicador de duracao; 1.0 e o tempo de projeto
     */
    public void aoTick(Set<ResourceLocation> ativas, float intensidade, int cor, float escala) {
        aoTick(ativas, intensidade, cor, escala, AuraDistribution.uniforme());
    }

    /**
     * Idem, com a distribuicao que o SERVIDOR mandou.
     *
     * <p>Ela entra como argumento, e nao e buscada aqui: este objeto nao
     * conhece cache nem rede, e e por isso que a regra dele da para provar sem
     * subir o jogo.
     */
    public void aoTick(Set<ResourceLocation> ativas, float intensidade, int cor, float escala,
            AuraDistribution distribuicao) {
        this.distribuicaoDesteTick = distribuicao;
        AuraVisualMode modo = ModoVisualDeTecnica.de(ativas);
        float alvo = modo == AuraVisualMode.OFF ? 0.0F : sanear(intensidade);

        if (precisaReenviar(modo, alvo, cor)) {
            // O AUDIO LE A MESMA TROCA QUE O CONTROLADOR. Reconstituir a
            // borda OFF -> TEN em outro relogio faria o som adiantar ou
            // atrasar justamente quando a escala da transicao mudasse.
            if (AuraTransicao.de(this.ultimoModo, modo) == AuraTransicao.LIGAR) {
                this.ativacaoDeTenPendente = true;
            }
            this.controlador.receber(modo, alvo, distribuicao, cor, cor);
            this.ultimoModo = modo;
            this.ultimaIntensidade = alvo;
            this.ultimaCor = cor;
            this.ultimaDistribuicao = distribuicao;
            this.recebeuAlgumaVez = true;
        }
        this.controlador.avancar(escalaSanea(escala));
    }

    /**
     * Quem liga, desliga: usado ao sair do servidor.
     *
     * <p>Sem isto, a aura do mundo anterior continuaria desenhada no intervalo
     * entre sair de um servidor e entrar em outro -- estado de outro mundo na
     * tela, sem nada acusar. E o mesmo motivo pelo qual o cache do cliente e
     * limpo no logout.
     */
    public void limpar() {
        this.controlador.limpar();
        this.ultimoModo = AuraVisualMode.OFF;
        this.ultimaIntensidade = 0.0F;
        this.ultimaCor = 0;
        this.ultimaDistribuicao = null;
        this.distribuicaoDesteTick = AuraDistribution.uniforme();
        this.recebeuAlgumaVez = false;
        this.ativacaoDeTenPendente = false;
    }

    /**
     * Consome a borda {@code OFF -> TEN} observada pela propria sessao.
     *
     * <p>E UMA BORDA, NAO UM ESTADO. Enquanto Ten permanece ligado, chamadas
     * seguintes retornam {@code false}; assim uma queda de FPS ou uma mudanca
     * pequena de output nao empilha vinte copias do mesmo som.
     */
    public boolean consumirAtivacaoDeTen() {
        boolean pendente = this.ativacaoDeTenPendente;
        this.ativacaoDeTenPendente = false;
        return pendente;
    }

    /**
     * Reenviar so quando alguma coisa mudou de verdade.
     *
     * <p>A COMPARACAO DE INTENSIDADE TEM FOLGA porque o output chega como float
     * interpolado: sem ela, uma variacao de 1e-7 contaria como mudanca e a
     * transicao reiniciaria em todo tick.
     */
    private boolean precisaReenviar(AuraVisualMode modo, float intensidade, int cor) {
        if (!this.recebeuAlgumaVez) {
            return true;
        }
        return modo != this.ultimoModo
                || cor != this.ultimaCor
                || Math.abs(intensidade - this.ultimaIntensidade) > 0.01F
                // A DISTRIBUICAO TAMBEM CONTA COMO MUDANCA. Sem esta linha,
                // ligar Gyo com Ten ja ativo nao reenviaria nada -- o modo
                // continua TEN, a intensidade nao muda, e a aura ficaria
                // espalhada na tela enquanto o servidor a concentrou.
                || !distribuicaoIgual();
    }

    private boolean distribuicaoIgual() {
        return this.ultimaDistribuicao != null
                && this.ultimaDistribuicao.equals(this.distribuicaoDesteTick);
    }

    /**
     * A escala nunca pode ser zero nem negativa.
     *
     * <p>Escala zero congelaria a aura no primeiro quadro da animacao -- e o
     * sintoma seria "a transicao nao funciona", que e um dos piores relatos de
     * bug que existem, porque nao ha erro para procurar.
     */
    private static float escalaSanea(float valor) {
        if (!Float.isFinite(valor) || valor <= 0.0F) {
            return 1.0F;
        }
        return valor;
    }

    private static float sanear(float valor) {
        if (!Float.isFinite(valor)) {
            // NaN NAO PODE CHEGAR AO CONTROLADOR: ele valida e lanca, e uma
            // excecao no tick do cliente derruba o render inteiro. O output ja
            // atravessou o protocolo como NaN uma vez neste projeto.
            return 0.0F;
        }
        return Math.clamp(valor, 0.0F, 1.0F);
    }
}
