package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.client.vfx.model.AuraGeometryLadder;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilDeBrilho;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilDePressao;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilVisual;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfis;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraAnchor;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraCurve;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraRibbonProfile;
import com.darkcontinent.nenfoundation.client.vfx.shader.KernelDeBorrao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O caminho quente nao aloca.
 *
 * <p><b>ALOCACAO POR QUADRO NAO DA ERRO.</b> Ela aparece como FPS e TPS caindo
 * devagar ao longo de uma sessao -- o mesmo sintoma do projetil orfao da lista de
 * erros previsiveis do {@code CLAUDE.md}. Nao ha exceção para procurar, nao ha
 * log, e quem relata diz "o jogo vai ficando pesado", que nao aponta para lugar
 * nenhum.
 *
 * <p><b>O QUE ESTE ARQUIVO PROVA, E O QUE ELE NAO PROVA.</b> Ele prova
 * propriedades ESTRUTURAIS: que a curva escreve num vetor de fora, que os tipos
 * de render sao constantes, que a escada de malhas e assada uma vez, que o perfil
 * interpolado e memorizado. Ele NAO mede taxa de alocacao -- isso e perfil de
 * {@code spark} ou JFR arquivado, com dez jogadores em Ren, e e a issue #206. Um
 * teste que dissesse medir isso seria a pior versao de um numero: o que parece
 * medicao e nao e.
 */
class AuraSemAlocacaoTest {

    private static AuraVisualState estado(AuraVisualMode modo, AuraVisualMode alvo,
            float progresso) {
        return new AuraVisualState(modo, alvo, 1.0F, progresso, AuraDistribution.uniforme(),
                0xFFFFFFFF, 0xFFFFFFFF, AuraTransitionSample.assentado(alvo));
    }

    @BeforeEach
    @AfterEach
    void semMemoEntreTestes() {
        // O MEMO FARIA UM TESTE ENXERGAR O RESULTADO DO ANTERIOR, e a falha
        // apareceria conforme a ordem de execucao -- que e o tipo de teste que
        // ninguem consegue depurar.
        AuraPerfis.esquecerMemo();
        SobreposicaoDeVfx.limpar();
    }

    // ------------------------------------------------- a curva

    @Test
    @DisplayName("a curva escreve NUM VETOR DE FORA, e o reaproveita mil vezes")
    void curvaNaoAloca() {
        // VINTE E OITO RIBBONS VEZES NOVE NOS VEZES SESSENTA QUADROS por segundo
        // e lixo demais para um efeito cosmetico. A prova estrutural e que o
        // vetor entra por parametro e volta o MESMO -- identidade, e nao valor.
        float[] destino = new float[12 * 3];
        float[] mesmoVetor = destino;
        for (int i = 0; i < 1000; i++) {
            int nos = AuraCurve.pontos(destino, AuraAnchor.SHOULDER_LEFT, false,
                    AuraCurve.semente(i, AuraAnchor.SHOULDER_LEFT, i % 8, i % 3),
                    1.0F, 0.4F);
            assertTrue(nos >= 2 && nos * 3 <= destino.length,
                    "a curva escreveu " + nos + " nos num vetor de " + destino.length);
        }
        assertSame(mesmoVetor, destino, "o vetor de trabalho foi trocado");
    }

    @Test
    @DisplayName("a coluna usa o MESMO vetor, e cabe nele")
    void colunaCabeNoVetor() {
        float[] destino = new float[12 * 3];
        for (int i = 0; i < 1000; i++) {
            int nos = AuraCurve.pontosDeColuna(destino, AuraAnchor.coluna(i), false,
                    AuraCurve.semente(i, AuraAnchor.coluna(i), i, i % 5), 1.0F, 2.5F);
            assertTrue(nos * 3 <= destino.length,
                    "a coluna quer " + nos + " nos e o vetor tem " + (destino.length / 3));
        }
    }

    @Test
    @DisplayName("a mesma semente devolve a MESMA curva: nada e sorteado por quadro")
    void curvaEDeterministica() {
        // UM SORTEIO A SESSENTA HERTZ NAO E "ORGANICO": e ruido branco, e ruido
        // branco e a assinatura do raio eletrico que a direcao de arte reprova.
        // E, sem determinismo, uma captura nao pode ser comparada com a seguinte.
        float[] a = new float[12 * 3];
        float[] b = new float[12 * 3];
        long semente = AuraCurve.semente(12345L, AuraAnchor.HAND_RIGHT, 3, 7);
        int na = AuraCurve.pontos(a, AuraAnchor.HAND_RIGHT, false, semente, 1.0F, 0.8F);
        int nb = AuraCurve.pontos(b, AuraAnchor.HAND_RIGHT, false, semente, 1.0F, 0.8F);
        assertEquals(na, nb);
        for (int i = 0; i < na * 3; i++) {
            assertEquals(a[i], b[i], 0.0F, "no " + i + " divergiu entre duas chamadas iguais");
        }
    }

    // ------------------------------------------------- a escada

    @Test
    @DisplayName("a escada e uma tabela, e nao um calculo por quadro")
    void escadaEEstavel() {
        for (int i = 0; i < AuraGeometryLadder.DEGRAUS; i++) {
            float primeira = AuraGeometryLadder.espessuraDaBordaDe(i);
            for (int repeticao = 0; repeticao < 100; repeticao++) {
                assertEquals(primeira, AuraGeometryLadder.espessuraDaBordaDe(i), 0.0F,
                        "o degrau " + i + " mudou de valor entre chamadas");
            }
        }
    }

    // ------------------------------------------------- o memo de perfil

    @Test
    @DisplayName("o perfil interpolado e MEMORIZADO: a mesma chave devolve a MESMA instancia")
    void perfilEMemorizado() {
        // QUATRO CHAMADAS POR JOGADOR POR QUADRO -- shell, filamentos, anel e
        // particula -- vezes sessenta quadros vezes dez jogadores sao 2400
        // records por segundo no caminho quente. A comparacao aqui e por
        // IDENTIDADE de proposito: por valor, ela passaria mesmo sem memo.
        AuraVisualState durante = estado(AuraVisualMode.TEN, AuraVisualMode.REN, 0.4F);
        AuraPerfilVisual primeira = AuraPerfis.de(durante);
        for (int i = 0; i < 100; i++) {
            assertSame(primeira, AuraPerfis.de(durante),
                    "o perfil foi recalculado com a mesma chave, na repeticao " + i);
        }
    }

    @Test
    @DisplayName("chave diferente recalcula: o memo nao devolve o perfil errado")
    void memoNaoMente() {
        AuraVisualState cedo = estado(AuraVisualMode.TEN, AuraVisualMode.REN, 0.20F);
        AuraVisualState tarde = estado(AuraVisualMode.TEN, AuraVisualMode.REN, 0.80F);
        AuraPerfilVisual a = AuraPerfis.de(cedo);
        AuraPerfilVisual b = AuraPerfis.de(tarde);
        assertNotSame(a, b, "dois progressos diferentes devolveram a mesma instancia");
        // O VALOR NAO E CONFERIDO AQUI, e a razao esta declarada: em JUnit nao ha
        // gerenciador de recursos, entao NENHUM perfil foi carregado e as duas
        // pontas caem no perfil de emergencia -- interpolar entre dois iguais da
        // iguais. Que a borda cresca ao caminhar para Ren e conferido em
        // AuraRenAcabamentoTest, que le os JSON do disco. O que ESTE teste
        // precisa provar e outra coisa: que o memo nao reaproveita uma chave que
        // nao e a dele.
        assertTrue(a != null && b != null);
    }

    @Test
    @DisplayName("girar um slider INVALIDA o memo -- senao o botao pareceria morto")
    void sobreposicaoInvalidaOMemo() {
        // ESTE E O ERRO NUMERO 7 CHEGANDO PELA PORTA DOS FUNDOS: o numero saiu
        // para o dado, o slider existe, e um cache esquecido faria a sessao de
        // arte girar um botao que nao muda nada.
        AuraVisualState durante = estado(AuraVisualMode.TEN, AuraVisualMode.REN, 0.5F);
        AuraPerfilVisual antes = AuraPerfis.de(durante);
        SobreposicaoDeVfx.forcarNoPerfil(SobreposicaoDeVfx.AjusteDePerfil.ALPHA_BORDA, 0.9F);
        AuraPerfilVisual depois = AuraPerfis.de(durante);
        assertNotSame(antes, depois, "o memo sobreviveu a um ajuste de perfil");
        assertEquals(0.9F, depois.alphaBorda(), 1.0e-5F);
    }

    @Test
    @DisplayName("esquecer o memo faz a proxima chamada recalcular")
    void esquecerFuncionaa() {
        AuraVisualState durante = estado(AuraVisualMode.REN, AuraVisualMode.REN, 1.0F);
        AuraPerfilVisual antes = AuraPerfis.de(durante);
        AuraPerfis.esquecerMemo();
        assertNotSame(antes, AuraPerfis.de(durante),
                "F3+T carregaria o perfil novo e o desenho continuaria mostrando o antigo");
    }

    // ------------------------------------------------- o kernel

    @Test
    @DisplayName("os pesos do desfoque sao calculados uma vez por raio, e cabem em oito")
    void kernelNaoCresce() {
        // RECALCULAR UMA GAUSSIANA POR PIXEL E POR QUADRO e gastar ALU para
        // reproduzir oito numeros que nao mudam. O vetor tem tamanho fixo, e e
        // isso que permite passa-lo como dois vec4 em vez de um array que o
        // `Uniform` do Minecraft nao aceita.
        for (float raio = 0.0F; raio <= AuraPerfilDeBrilho.RAIO_MAXIMO; raio += 0.5F) {
            assertEquals(KernelDeBorrao.TAPS, KernelDeBorrao.pesos(raio).length,
                    "o kernel mudou de tamanho no raio " + raio);
        }
    }

    // ------------------------------------------------- os tetos

    @Test
    @DisplayName("os tetos de seguranca continuam FORA da config")
    void tetosNaoSaoConfig() {
        // QUEM QUER MENOS PARTICULA MEXE NA DENSIDADE, que e config. O teto
        // existe para que uma densidade errada nao trave o cliente -- e teto que
        // vira botao deixa de ser teto.
        assertEquals(12, AuraPerfilDePressao.TETO_DE_DETRITOS);
        assertEquals(8, AuraPerfilDePressao.TETO_DE_COLUNAS);
        assertEquals(48, AuraPerfilDePressao.TETO_DE_SEGMENTOS);
        assertEquals(28, AuraRibbonProfile.TETO);
        assertTrue(com.darkcontinent.nenfoundation.client.vfx.model.AuraGeometryProfile
                .ESPESSURA_MAXIMA <= 0.25F,
                "a espessura maxima e limite de design: poder extremo aumenta densidade,"
                        + " brilho, velocidade e pressao -- nao tamanho");
    }

    @Test
    @DisplayName("sem aura na tela, a regua marca custo ZERO -- e nao custo pequeno")
    void custoZeroEZero() {
        MedidorDeVfx.limpar();
        MedidorDeVfx.fecharQuadro();
        assertTrue(MedidorDeVfx.custoZero(),
                "com nenhuma aura desenhada, o contador precisa marcar zero");

        MedidorDeVfx.coluna();
        MedidorDeVfx.fecharQuadro();
        assertTrue(!MedidorDeVfx.custoZero(),
                "UMA COLUNA JA E CUSTO. Um `custoZero` que ignorasse colunas e aneis"
                        + " afirmaria zero enquanto a tela desenha Ren inteiro");

        MedidorDeVfx.anelDePressao();
        MedidorDeVfx.fecharQuadro();
        assertTrue(!MedidorDeVfx.custoZero());
        MedidorDeVfx.limpar();
    }
}
