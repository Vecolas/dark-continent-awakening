package com.darkcontinent.nenfoundation.client.hud.component;

import com.darkcontinent.nenfoundation.client.hud.AparenciaDeTecnica;
import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import com.darkcontinent.nenfoundation.client.screen.DesenhoDaRoda;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Quais tecnicas estao ligadas AGORA, visivel sem abrir nada.
 *
 * <p>ATE AQUI SO A RODA MOSTRAVA ISSO, e a roda so aparece enquanto a tecla
 * esta segurada. Quem ligou Ren e soltou a tecla nao tinha como saber que ela
 * continuava ligada -- e Ren drena aura depressa. O sintoma seria a barra
 * caindo sem motivo aparente, que e o relato de bug que nao diz onde procurar.
 *
 * <p>ELE LE O RUNTIME, e nao o que o cliente pediu. O conjunto vem do delta
 * S2C: se o servidor recusou a ativacao, o indicador nao acende. Desenhar o
 * pedido em vez do estado seria mutacao otimista com outro nome.
 *
 * <p>DESENHA ATE {@link #TETO_DE_INDICADORES} e depois conta o resto. O teto
 * nao e regra de jogo -- nada limita quantas tecnicas ficam ativas, e o limite
 * real e a aura. Ele e so o ponto em que a fila deixaria a moldura.
 */
public final class TecnicasAtivasRenderer {

    /**
     * Quantos indicadores cabem antes de a fila sair da moldura.
     *
     * <p>CONSTANTE NO CODIGO de proposito: e um limite de DESENHO, e nao um
     * botao de balanceamento. Numero que alguem giraria numa sessao de
     * balanceamento sai para config; este ninguem gira.
     */
    public static final int TETO_DE_INDICADORES = 6;

    private static final int DIAMETRO = 9;
    private static final int ESPACO = 3;
    private static final int COR_APAGADA = 0x60_00_00_00;

    /**
     * Onde cada indicador fica, dado o canto da fila.
     *
     * <p>PUBLICO E ESTATICO para poder ser conferido sem abrir o jogo: o teste
     * mede a fila sem instanciar renderer nenhum, que exigiria o cliente de pe.
     */
    public static int xDoIndicador(int xInicial, int indice) {
        return xInicial + indice * (DIAMETRO + ESPACO);
    }

    /** Quantos indicadores serao desenhados, dado quantas tecnicas estao ativas. */
    public static int quantosDesenhar(int ativas) {
        return Math.min(Math.max(0, ativas), TETO_DE_INDICADORES);
    }

    /** Quantas ficaram de fora e viram contagem. Zero quando todas coubaram. */
    public static int quantasSobram(int ativas) {
        return Math.max(0, ativas - TETO_DE_INDICADORES);
    }

    /**
     * Desenha a fila.
     *
     * <p>A ORDEM VEM PRONTA do chamador, e tem de ser ESTAVEL: uma fila que
     * reordena a cada tick pisca, e o jogador nao consegue olhar de relance.
     */
    public void desenhar(GuiGraphics g, NenHudLayout.Retangulo area,
            List<ResourceLocation> ativas) {
        int desenhar = quantosDesenhar(ativas.size());
        float cy = area.y() + DIAMETRO / 2.0F;

        for (int i = 0; i < desenhar; i++) {
            AparenciaDeTecnica.Aparencia aparencia = AparenciaDeTecnica.de(ativas.get(i));
            float cx = xDoIndicador(area.x(), i) + DIAMETRO / 2.0F;
            float raio = DIAMETRO / 2.0F;

            // O fundo escuro entra sempre: sobre neve ou sobre areia, a forma
            // sozinha some, e o indicador vira um borrao claro num fundo claro.
            DesenhoDaRoda.disco(g, cx, cy, raio, COR_APAGADA);
            desenharForma(g, cx, cy, raio, aparencia);
        }

        int sobram = quantasSobram(ativas.size());
        if (sobram > 0) {
            g.drawString(net.minecraft.client.Minecraft.getInstance().font,
                    "+" + sobram, xDoIndicador(area.x(), desenhar), area.y(),
                    0xFF_D8_F4_FF, true);
        }
    }

    /** Um quarto de volta, em radianos: o tamanho do setor de Gyo. */
    private static final double QUARTO = Math.PI / 2.0D;

    /**
     * A abertura da ponta de Shu.
     *
     * <p>NAO E MAIS ESTREITA QUE ISTO de proposito: o indicador tem nove pixels,
     * e um setor de poucos graus nesse raio nao chega a um pixel de largura --
     * ele some, e a forma vira a de Ko.
     */
    private static final double PONTA = Math.PI / 3.5D;

    private static void desenharForma(GuiGraphics g, float cx, float cy, float raio,
            AparenciaDeTecnica.Aparencia aparencia) {
        switch (aparencia.forma()) {
            // Anel fechado: a aura fica presa em volta do corpo.
            case ANEL -> DesenhoDaRoda.anel(g, cx, cy, raio - 2.5F, raio - 0.5F,
                    aparencia.cor());
            // Aureola: um miolo cheio e um halo solto em volta. Ren libera
            // volume, e a forma tem de parecer maior que a de Ten sem ocupar
            // mais espaco na fila.
            case AUREOLA -> {
                DesenhoDaRoda.disco(g, cx, cy, raio - 2.5F, aparencia.cor());
                DesenhoDaRoda.anel(g, cx, cy, raio - 1.0F, raio, aparencia.cor());
            }
            // Contorno vazado: os nos fechados, e nada saindo pelo meio.
            case CONTORNO -> DesenhoDaRoda.anel(g, cx, cy, raio - 1.0F, raio,
                    aparencia.cor());
            // Muralha: anel grosso, quase fechando o indicador. Ken cobre o
            // corpo inteiro, e a forma tem de parecer mais SOLIDA que a de Ren
            // sem parecer mais brilhante.
            case MURALHA -> DesenhoDaRoda.anel(g, cx, cy, raio - 3.0F, raio,
                    aparencia.cor());
            // Setor: um quarto do anel aceso. O apagado nao e sobra -- e a
            // parte do corpo que a concentracao descobriu.
            case SETOR -> DesenhoDaRoda.setorDeAnel(g, cx, cy, raio - 3.0F, raio,
                    -QUARTO / 2.0D, QUARTO / 2.0D, aparencia.cor());
            // Lanca: miolo pequeno e uma ponta ATE A BORDA. E a unica forma que
            // sai do circulo, porque Shu e a unica que manda aura para fora do
            // corpo.
            case LANCA -> {
                DesenhoDaRoda.disco(g, cx, cy, raio - 4.0F, aparencia.cor());
                DesenhoDaRoda.setorDeAnel(g, cx, cy, raio - 3.5F, raio,
                        -PONTA / 2.0D, PONTA / 2.0D, aparencia.cor());
            }
            // Ponto: so o miolo. O VAZIO EM VOLTA E O DESENHO -- com a aura toda
            // num lugar, o resto do corpo esta descoberto.
            case PONTO -> DesenhoDaRoda.disco(g, cx, cy, raio - 3.5F, aparencia.cor());
            // Neutra: um disco cheio. Nao tenta significar nada, porque este
            // arquivo nao sabe o que a tecnica faz.
            case NEUTRA -> DesenhoDaRoda.disco(g, cx, cy, raio - 1.5F, aparencia.cor());
        }
    }
}
