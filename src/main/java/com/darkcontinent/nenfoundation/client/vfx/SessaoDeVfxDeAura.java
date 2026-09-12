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
    private boolean recebeuAlgumaVez;

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
     * @param passo       quanto a transicao avanca neste tick, de 0 a 1
     */
    public void aoTick(Set<ResourceLocation> ativas, float intensidade, int cor, float passo) {
        AuraVisualMode modo = ModoVisualDeTecnica.de(ativas);
        float alvo = modo == AuraVisualMode.OFF ? 0.0F : sanear(intensidade);

        if (precisaReenviar(modo, alvo, cor)) {
            this.controlador.receber(modo, alvo, AuraDistribution.uniforme(), cor, cor);
            this.ultimoModo = modo;
            this.ultimaIntensidade = alvo;
            this.ultimaCor = cor;
            this.recebeuAlgumaVez = true;
        }
        this.controlador.avancar(sanear(passo));
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
        this.recebeuAlgumaVez = false;
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
                || Math.abs(intensidade - this.ultimaIntensidade) > 0.01F;
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
