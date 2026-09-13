package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.client.vfx.SobreposicaoDeVfx.AjusteDePerfil;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilVisual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A sobreposicao de vfx: o que o dev forcou, e o que ela NAO pode deixar
 * acontecer.
 *
 * <p>O que estes testes protegem nao e o caminho feliz -- e o conjunto de
 * falhas silenciosas que uma ferramenta de tuning consegue produzir: um slider
 * que faz o render lancar, um ajuste que sobrevive ao logout e vira a proxima
 * captura, e um fator que quebra a invariante do perfil sem ninguem ver.
 */
class SobreposicaoDeVfxTest {

    private static final AuraPerfilVisual BASE = new AuraPerfilVisual(
            0.055F, 0.20F, 0.035F, 3.4F, 2.7F, 2.0F, 0.12F, 4.0F, 0.9F);

    @BeforeEach
    @AfterEach
    void semEstadoEntreTestes() {
        SobreposicaoDeVfx.limpar();
    }

    @Test
    @DisplayName("sem sobreposicao, o perfil sai identico -- e e o MESMO objeto")
    void semSobreposicaoNaoTocaNoPerfil() {
        assertSame(BASE, SobreposicaoDeVfx.aplicarNoPerfil(BASE),
                "sem ajuste nenhum, copiar o record seria alocacao por quadro sem motivo");
        assertFalse(SobreposicaoDeVfx.ativa());
    }

    @Test
    @DisplayName("alpha fora da faixa e PRENSADO, e nao lancado")
    void alphaForaDaFaixaNaoLanca() {
        // O construtor de AuraPerfilVisual lanca para alpha > 1. Uma excecao
        // aqui aconteceria DENTRO do render, e derrubaria o desenho do mundo
        // inteiro por causa de um slider.
        SobreposicaoDeVfx.forcarNoPerfil(AjusteDePerfil.ALPHA_BORDA, 9.0F);
        AuraPerfilVisual saida = SobreposicaoDeVfx.aplicarNoPerfil(BASE);
        assertEquals(1.0F, saida.alphaBorda(), 1.0e-6F);
    }

    @Test
    @DisplayName("o fator de Fresnel preserva a ordem interna > borda > externa")
    void fresnelPreservaAOrdem() {
        SobreposicaoDeVfx.forcarNoPerfil(AjusteDePerfil.FRESNEL, 0.25F);
        AuraPerfilVisual saida = SobreposicaoDeVfx.aplicarNoPerfil(BASE);
        assertTrue(saida.fresnelInterno() > saida.fresnelBorda(),
                "a invariante do perfil e que o expoente DIMINUI da camada interna para a externa");
        assertTrue(saida.fresnelBorda() > saida.fresnelExterno());
        assertNotEquals(BASE.fresnelInterno(), saida.fresnelInterno(),
                "o fator precisa ter mexido em alguma coisa");
    }

    @Test
    @DisplayName("escala de ruido nunca chega a zero, que colapsaria a UV")
    void ruidoNuncaZera() {
        SobreposicaoDeVfx.forcarNoPerfil(AjusteDePerfil.RUIDO, 0.0F);
        AuraPerfilVisual saida = SobreposicaoDeVfx.aplicarNoPerfil(BASE);
        assertTrue(saida.escalaDeRuido() > 0.0F,
                "zero colapsa a UV do ruido, e o construtor do perfil recusa");
    }

    @Test
    @DisplayName("limpar apaga TODOS os campos -- um ajuste nao sobrevive ao logout")
    void limparApagaTudo() {
        SobreposicaoDeVfx.desligar(true);
        SobreposicaoDeVfx.congelar(true);
        SobreposicaoDeVfx.forcarModo(AuraVisualMode.REN);
        SobreposicaoDeVfx.forcarOutput(0.3F);
        SobreposicaoDeVfx.forcarDensidade(0.0F);
        SobreposicaoDeVfx.forcarLod(AuraRenderLod.FAR);
        SobreposicaoDeVfx.forcarRibbons(3);
        for (AjusteDePerfil ajuste : AjusteDePerfil.values()) {
            SobreposicaoDeVfx.forcarNoPerfil(ajuste, 0.5F);
        }
        assertTrue(SobreposicaoDeVfx.ativa());

        SobreposicaoDeVfx.limpar();

        assertFalse(SobreposicaoDeVfx.ativa(),
                "um campo esquecido aqui reaparece na proxima sessao como um numero"
                        + " que ninguem lembra de ter posto");
        assertSame(BASE, SobreposicaoDeVfx.aplicarNoPerfil(BASE));
    }

    @Test
    @DisplayName("o estado forcado vai com a transicao no fim")
    void estadoForcadoNaoFicaNoMeioDaTransicao() {
        AuraVisualState base = new AuraVisualState(AuraVisualMode.TEN, AuraVisualPreset.off(),
                0.4F, 0.3F, AuraDistribution.uniforme(), 0xFFFFFFFF, 0xFFFFFFFF);
        SobreposicaoDeVfx.forcarModo(AuraVisualMode.REN);

        AuraVisualState saida = SobreposicaoDeVfx.aplicarNoLocal(base);

        assertEquals(AuraVisualMode.REN, saida.mode());
        assertEquals(1.0F, saida.transitionProgress(), 1.0e-6F,
                "estado forcado nao esta indo para lugar nenhum; deixar o progresso pela"
                        + " metade faz a captura seguinte pegar um quadro de animacao");
    }

    @Test
    @DisplayName("o desligamento geral vale tambem para os outros jogadores")
    void desligarValeParaTerceiros() {
        AuraVisualState base = new AuraVisualState(AuraVisualMode.TEN, AuraVisualPreset.off(),
                1.0F, 1.0F, AuraDistribution.uniforme(), 0xFFFFFFFF, 0xFFFFFFFF);
        SobreposicaoDeVfx.desligar(true);
        assertFalse(SobreposicaoDeVfx.aplicarEmTerceiro(base).enabled());
        assertFalse(SobreposicaoDeVfx.aplicarNoLocal(base).enabled());
    }

    @Test
    @DisplayName("o estado dos OUTROS nao se forca -- so se desliga")
    void terceiroNaoAceitaEstadoForcado() {
        AuraVisualState base = new AuraVisualState(AuraVisualMode.TEN, AuraVisualPreset.off(),
                1.0F, 1.0F, AuraDistribution.uniforme(), 0xFFFFFFFF, 0xFFFFFFFF);
        SobreposicaoDeVfx.forcarModo(AuraVisualMode.REN);

        assertEquals(AuraVisualMode.TEN, SobreposicaoDeVfx.aplicarEmTerceiro(base).mode(),
                "desenhar em outra pessoa um estado que o servidor nunca mandou produz"
                        + " uma captura que nao prova nada");
    }

    @Test
    @DisplayName("modo forcado para OFF apaga, em vez de desenhar um preset vazio")
    void modoOffApaga() {
        AuraVisualState base = new AuraVisualState(AuraVisualMode.TEN, AuraVisualPreset.off(),
                1.0F, 1.0F, AuraDistribution.uniforme(), 0xFFFFFFFF, 0xFFFFFFFF);
        SobreposicaoDeVfx.forcarModo(AuraVisualMode.OFF);
        assertFalse(SobreposicaoDeVfx.aplicarNoLocal(base).enabled());
    }

    @Test
    @DisplayName("densidade forcada substitui a config; ausente devolve a config")
    void densidadeCaiParaAConfig() {
        assertEquals(1.0D, SobreposicaoDeVfx.aplicarNaDensidade(1.0D), 1.0e-6D);
        SobreposicaoDeVfx.forcarDensidade(0.0F);
        assertEquals(0.0D, SobreposicaoDeVfx.aplicarNaDensidade(1.0D), 1.0e-6D,
                "zero e o valor do criterio do ADR-015, e precisa sobreviver ao caminho inteiro");
        SobreposicaoDeVfx.forcarDensidade(-1.0F);
        assertEquals(1.0D, SobreposicaoDeVfx.aplicarNaDensidade(1.0D), 1.0e-6D);
    }

    @Test
    @DisplayName("ribbons: zero e valor legitimo, negativo devolve o controle")
    void ribbonsAceitamZero() {
        assertEquals(8, SobreposicaoDeVfx.aplicarNasRibbons(8));
        SobreposicaoDeVfx.forcarRibbons(0);
        assertEquals(0, SobreposicaoDeVfx.aplicarNasRibbons(8),
                "zero filamento e uma das perguntas do AV2, e nao um valor invalido");
        SobreposicaoDeVfx.forcarRibbons(-1);
        assertEquals(8, SobreposicaoDeVfx.aplicarNasRibbons(8));
    }

    @Test
    @DisplayName("valor nao finito nao entra: NaN atravessaria tudo em silencio")
    void naoAceitaNaN() {
        SobreposicaoDeVfx.forcarOutput(Float.NaN);
        SobreposicaoDeVfx.forcarDensidade(Float.NaN);
        SobreposicaoDeVfx.forcarNoPerfil(AjusteDePerfil.FLUXO, Float.NaN);
        assertFalse(SobreposicaoDeVfx.ativa(),
                "NaN tem de ser tratado como ausencia; toda comparacao com ele e falsa,"
                        + " e ele atravessaria ate o vertice sem nada acusar");
    }
}
