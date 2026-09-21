package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.client.vfx.model.AuraGeometryLadder;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraGeometryProfile;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraShellPass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A escada de espessuras que o AV4 precisou construir.
 *
 * <p><b>O QUE ESTE ARQUIVO PROTEGE, e por que ele nao e cerimonia.</b> A
 * espessura da shell e geometria ASSADA -- ela entra na construcao da malha, e
 * nao num uniform. Ren precisa ser mais espesso que Ten e a shell precisa
 * CONTRAIR nos primeiros 100 ms da subida; as duas coisas passam por escolher um
 * degrau desta escada, por quadro.
 *
 * <p>Os modos de falha sao todos silenciosos:
 *
 * <ul>
 *   <li>Ten ou Ren caindo ENTRE degraus -- os dois presetes da direcao de arte
 *       desenhariam numa espessura que nao e a deles, e ninguem repara numa
 *       diferenca de meio por cento;</li>
 *   <li>degraus grossos demais na faixa de Ten a Ren -- a troca vira um SALTO
 *       visivel durante a subida, e o relato e "a aura pisca ao ativar Ren";</li>
 *   <li>o degrau mais alto passando do teto de design -- o construtor do perfil
 *       lanca, e lancar dentro do registro de camada derruba o cliente.</li>
 * </ul>
 */
class AuraGeometryLadderTest {

    @Test
    @DisplayName("Ten e Ren caem EXATAMENTE em degraus, e nao entre eles")
    void osDoisPresetesTemDegrauProprio() {
        AuraGeometryProfile ten = AuraGeometryProfile.ten();
        AuraGeometryProfile ren = AuraGeometryProfile.ren();

        for (AuraShellPass passe : AuraShellPass.values()) {
            assertEquals(ten.espessuraDe(passe),
                    AuraGeometryLadder.perfilDe(AuraGeometryLadder.DEGRAU_DE_TEN)
                            .espessuraDe(passe),
                    1.0e-6F, "o degrau de Ten precisa ser Ten exato em " + passe);
            assertEquals(ren.espessuraDe(passe),
                    AuraGeometryLadder.perfilDe(AuraGeometryLadder.DEGRAU_DE_REN)
                            .espessuraDe(passe),
                    1.0e-6F, "o degrau de Ren precisa ser Ren exato em " + passe);
        }
    }

    @Test
    @DisplayName("todo degrau produz um perfil VALIDO: crescente e dentro do teto")
    void todosOsDegrausSaoValidos() {
        for (int i = 0; i < AuraGeometryLadder.DEGRAUS; i++) {
            // O CONSTRUTOR DE AuraGeometryProfile RECUSA espessura nao crescente
            // e acima do teto. Construir os degraus todos aqui e o que transforma
            // essa recusa numa garantia sobre a ESCADA -- e lancar no registro de
            // camada derrubaria o cliente inteiro, sem uma tela para relatar.
            AuraGeometryProfile p = AuraGeometryLadder.perfilDe(i);
            assertTrue(p.espessuraExterna() <= AuraGeometryProfile.ESPESSURA_MAXIMA,
                    "degrau " + i + " passa do teto de design: " + p.espessuraExterna());
            assertTrue(p.espessuraInterna() > 0.0F, "degrau " + i);
        }
    }

    @Test
    @DisplayName("os degraus sobem, e nenhum repete o anterior")
    void degrausSaoEstritamenteCrescentes() {
        for (int i = 1; i < AuraGeometryLadder.DEGRAUS; i++) {
            assertTrue(AuraGeometryLadder.espessuraDaBordaDe(i)
                            > AuraGeometryLadder.espessuraDaBordaDe(i - 1),
                    "degrau " + i + " nao e mais grosso que " + (i - 1)
                            + "; um degrau repetido e uma malha assada para nada");
        }
    }

    @Test
    @DisplayName("na faixa de Ten a Ren, um degrau nao chega a um decimo de pixel de textura")
    void degrausInvisiveisNaFaixaDaTransicao() {
        // A TRANSICAO TEN->REN PERCORRE ESTA FAIXA EM MENOS DE UM SEGUNDO. Um
        // degrau grosso aqui aparece como salto -- e o relato seria "a aura
        // pisca ao ativar Ren", que ninguem associa a uma tabela de espessuras.
        //
        // A unidade de modelo e 1/16 de bloco; a textura da skin tem 1 pixel por
        // unidade. Um decimo de unidade e, literalmente, um decimo de pixel.
        for (int i = AuraGeometryLadder.DEGRAU_DE_TEN;
                i < AuraGeometryLadder.DEGRAU_DE_REN; i++) {
            float salto = (AuraGeometryLadder.espessuraDaBordaDe(i + 1)
                    - AuraGeometryLadder.espessuraDaBordaDe(i))
                    * AuraGeometryProfile.UNIDADES_POR_BLOCO;
            assertTrue(salto <= 0.10F,
                    "o degrau " + i + " salta " + salto + " unidade de modelo; acima de"
                            + " 0,1 a troca deixa de ser invisivel durante a subida");
        }
    }

    @Test
    @DisplayName("ha degrau ABAIXO de Ten, e ele serve a contracao de 3 a 5%")
    void existeDegrauDeContracao() {
        float ten = AuraGeometryLadder.espessuraDaBordaDe(AuraGeometryLadder.DEGRAU_DE_TEN);
        // A contracao pedida pela direcao de arte, aplicada a borda de Ten.
        float contraida = ten * 0.955F;
        int degrau = AuraGeometryLadder.degrauPara(contraida);

        assertTrue(degrau < AuraGeometryLadder.DEGRAU_DE_TEN,
                "SEM UM DEGRAU ABAIXO DE TEN a contracao nao existe: o degrau mais"
                        + " proximo seria o proprio Ten, e a shell nao recuaria nada");
        float real = AuraGeometryLadder.espessuraDaBordaDe(degrau) / ten;
        assertTrue(real >= 0.94F && real <= 0.97F,
                "a contracao real ficou em " + ((1.0F - real) * 100.0F)
                        + "%, fora da faixa de 3 a 5 pedida pela direcao de arte");
    }

    @Test
    @DisplayName("ha folga ACIMA de Ren, para a aura passar por fora da armadura")
    void existeFolgaParaArmadura() {
        float ren = AuraGeometryLadder.espessuraDaBordaDe(AuraGeometryLadder.DEGRAU_DE_REN);
        float maior = AuraGeometryLadder.espessuraDaBordaDe(AuraGeometryLadder.DEGRAUS - 1);
        assertTrue(maior > ren,
                "o AV7 precisa engrossar a borda para ela aparecer por fora da armadura"
                        + " padrao; sem degrau acima de Ren nao ha para onde ir");
        // A faixa de 0,06 a 0,09 e a que a issue #202 pede para a borda com
        // armadura. Ela precisa estar ALCANCAVEL pela escada.
        assertTrue(maior >= 0.09F,
                "a borda com armadura chega a 0,09 bloco; a escada para em " + maior);
    }

    @Test
    @DisplayName("o degrau mais proximo e mesmo o mais proximo")
    void degrauEscolhidoEOMaisProximo() {
        for (int i = 0; i < AuraGeometryLadder.DEGRAUS; i++) {
            float alvo = AuraGeometryLadder.espessuraDaBordaDe(i);
            assertEquals(i, AuraGeometryLadder.degrauPara(alvo),
                    "pedir a espessura exata do degrau " + i + " devolveu outro");
        }
        // No meio de dois degraus, qualquer um dos dois serve -- o que nao pode
        // e vir um terceiro.
        float meio = (AuraGeometryLadder.espessuraDaBordaDe(3)
                + AuraGeometryLadder.espessuraDaBordaDe(4)) * 0.5F;
        int escolhido = AuraGeometryLadder.degrauPara(meio);
        assertTrue(escolhido == 3 || escolhido == 4, "veio o degrau " + escolhido);
    }

    @Test
    @DisplayName("espessura absurda SATURA em vez de lancar")
    void saturaEmVezDeLancar() {
        // QUEM CHAMA E O CAMINHO DE DESENHO. Uma excecao no tick de render
        // derruba o desenho do mundo inteiro, e nao so a aura -- e um efeito
        // visual jamais pode fazer isso.
        assertEquals(AuraGeometryLadder.DEGRAUS - 1, AuraGeometryLadder.degrauPara(99.0F));
        assertEquals(0, AuraGeometryLadder.degrauPara(-1.0F));
        assertEquals(AuraGeometryLadder.DEGRAU_DE_TEN, AuraGeometryLadder.degrauPara(Float.NaN),
                "NaN precisa cair num degrau conhecido: toda comparacao com NaN e falsa,"
                        + " e sem esta guarda ele atravessaria o laco inteiro");
        assertEquals(0, AuraGeometryLadder.degrauPara(
                AuraGeometryLadder.espessuraDaBordaDe(0) - 1.0F));
    }

    @Test
    @DisplayName("perfilDe satura o indice, e nao estoura o vetor")
    void indiceSatura() {
        assertEquals(AuraGeometryLadder.perfilDe(0), AuraGeometryLadder.perfilDe(-50));
        assertEquals(AuraGeometryLadder.perfilDe(AuraGeometryLadder.DEGRAUS - 1),
                AuraGeometryLadder.perfilDe(9999));
    }
}
